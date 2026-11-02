#!/usr/bin/env python3
"""Rainoray skill sounds: a small bank of synthesized cues plus three pre-synced audition tracks.

    python3 generate_rainoray_skill_sfx.py --out output            # write cues/, mixes/, manifest.json
    python3 generate_rainoray_skill_sfx.py --check --out output    # re-render in memory, verify, exit 1 on any fault
    python3 generate_rainoray_skill_sfx.py --selftest --out output # prove --check rejects tampered/missing files

Void magic: detuned FM partial clouds that fold inward, ring-pitched breath formants, reverse swells, short
pressure drops, ring-modulated space-tears and a diffuse comb/allpass haze. Seeded, stdlib only, no samples. 22050 Hz mono PCM16, no DC, faded edges, peak <= 0.78.
No saturation anywhere.
"""
import argparse
import array
import hashlib
import io
import json
import math
import random
import shutil
import sys
import tempfile
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_forest_sfx import RATE, biquad, bump, count, env, mix, mul, noise, norm  # noqa: E402

FPS = 60
MIX_PEAK = 0.708          # -3 dBFS master target for the audition tracks
CUE_CEILING = 0.78        # hard limit for any file (-2.2 dBFS)


# ---------------------------------------------------------------- small helpers (lists of floats)

def times(n):
    return [i / RATE for i in range(n)]


def add(*xs):
    return [sum(v) for v in zip(*xs)]


def scale(x, g):
    return [v * g for v in x]


def charge_shape(n, top, gone, curve):
    """Tensing rise to top, then a fast cosine drop to silence by gone: the pre-release vacuum dip."""
    out = []
    for t in times(n):
        if t < top:
            out.append(math.sin(t / top * math.pi / 2) ** curve)
        elif t < gone:
            out.append(0.5 + 0.5 * math.cos(math.pi * (t - top) / (gone - top)))
        else:
            out.append(0.0)
    return out


def fade_tail(x, seconds):
    n, m = len(x), count(seconds)
    return [v * (1.0 if i < n - m else 0.5 + 0.5 * math.cos(math.pi * (i - (n - m)) / m)) for i, v in enumerate(x)]


# ---------------------------------------------------------------- void DSP: four building blocks

# partial sets (multiple of f0, gain): hollow/stretched for the void, near-harmonic glass for the seal
VOID = [(1, 1.0), (2.0, 0.55), (2.98, 0.42), (4.07, 0.3), (5.43, 0.22), (6.9, 0.14), (9.2, 0.08)]
SEAL = [(1, 1.0), (2.0, 0.5), (3.0, 0.32), (4.01, 0.22), (5.04, 0.14), (6.0, 0.08)]
SHINE = [(1, 1.0), (1.5, 0.6), (2.01, 0.45), (2.67, 0.3), (3.56, 0.18)]


def cloud(n, f0, partials, shape, squeeze=None, glide=None, fm=None, spread=0.0035):
    """Detuned partial cloud. Every partial is a slowly drifting pair f*(1 -/+ spread) so it beats like a chorus.
    squeeze(t) pulls the multiples toward f0 (overtones folding inward), glide(t) bends the whole cloud and fm(t)
    is a phase-modulation index against a sub-octave modulator (dark, unstable sidebands when high)."""
    out = [0.0] * n
    live = [i for i in range(n) if abs(shape[i]) > 1e-5]
    if not live:
        return out
    for k, (m, g) in enumerate(partials):
        for side in (-1, 1):
            ph = mph = 0.0
            drift = 0.21 + 0.17 * k + 0.05 * side
            for i in range(live[0], live[-1] + 1):
                t = i / RATE
                mm = 1 + (m - 1) * squeeze(t) if squeeze else m
                f = f0 * mm * (glide(t) if glide else 1.0) * (1 + side * spread + 0.003 * math.sin(2 * math.pi * drift * t + k))
                ph += 2 * math.pi * f / RATE
                mph += math.pi * f / RATE
                out[i] += g * shape[i] * math.sin(ph + (fm(t) * math.sin(mph) if fm else 0.0))
    return out


def breath(rng, d, formants, ring=None, depth=0.6):
    """Nonverbal 'oh/oo' resonance: noise through moving vowel formants [(Hz or f(t), Q, gain)]. ring partially
    ring-modulates it (phase-continuous) so the whisper takes a hollow pitch, like a choir heard through stone."""
    x = add(*(scale(biquad(noise(rng, d), "band", f, q), g) for f, q, g in formants))
    if ring is None:
        return x
    rf, ph, out = ring if callable(ring) else (lambda _: ring), 0.0, []
    for i, v in enumerate(x):
        ph += 2 * math.pi * rf(i / RATE) / RATE
        out.append(v * (1 - depth + depth * math.sin(ph)))
    return out


def haze(x, rt, wet, cut=3200):
    """Diffuse spectral tail: four combs (rt s to -60 dB) through two allpasses, darkened, mixed under x."""
    n, src, acc = len(x), biquad(x, "low", cut), [0.0] * len(x)
    for ms in (31.1, 37.7, 41.3, 47.9):
        dl, g, y = count(ms / 1000), 10 ** (-3 * ms / 1000 / rt), list(src)
        for i in range(dl, n):
            y[i] += g * y[i - dl]
        acc = add(acc, y)
    for ms in (5.3, 1.9):
        dl, y = count(ms / 1000), [0.0] * n
        for i in range(n):
            y[i] = -0.7 * acc[i] + (acc[i - dl] + 0.7 * y[i - dl] if i >= dl else 0.0)
        acc = y
    top = max(1e-9, max(abs(v) for v in acc))
    return add(x, scale(acc, wet * max(abs(v) for v in x) / top))


def tear(rng, d, at, hi, lo, fall, ring, decay):
    """The contact: a resonant noise band snapping from hi to lo Hz, ring-modulated at ring Hz so its partials are
    inharmonic and sourceless (no blade, stone or drum sounds like this), with a 0.4 ms front."""
    n = count(d)
    rip = biquad(noise(rng, d), "band", lambda t: lo + (hi - lo) * math.exp(-max(0.0, t - at) / fall), 2.2)
    return [v * e * math.sin(2 * math.pi * ring * i / RATE) for i, (v, e) in enumerate(zip(rip, env(n, 0.0004, decay, start=at)))]


def inhale(rng, n, end, length, f0, parts, rt=0.5):
    """Reverse swell: a struck cloud plus breath, hazed and played backwards so it finishes at `end` s. Air and
    overtones are drawn *into* the coming tear; the abrupt end is the pressure drop."""
    m = count(length)
    seg = mix(m, (cloud(m, f0, parts, env(m, 0.002, length / 5)), 1.0),
              (mul(breath(rng, length, [(700, 6, 1.0), (1500, 8, 0.6), (3100, 9, 0.35)]), env(m, 0.001, length / 6)), 0.8))
    out, s = [0.0] * n, count(end) - m
    for i, v in enumerate(haze(seg, rt, 0.9)[::-1]):
        if 0 <= s + i < n:
            out[s + i] = v
    return out


def since(c):
    return lambda t: max(0.0, t - c)


def choir(rng, d, n, start, f0, vowels, attack, decay):
    """Dark nonverbal resonance: two formants drifting from 'oh' toward 'oo', ring-pitched an octave above f0."""
    a = since(start)
    (f1, f2), (g1, g2) = vowels
    x = breath(rng, d, [(lambda t: f1 + (g1 - f1) * min(1.0, a(t) / 0.7), 9, 1.0),
                        (lambda t: f2 + (g2 - f2) * min(1.0, a(t) / 0.7), 10, 0.6), (2600, 12, 0.15)], ring=2 * f0)
    return mul(x, env(n, attack, decay, start=start))


# ---------------------------------------------------------------- the cues; each returns floats, sync point in CUES

def void_charge(rng, v):
    d, top, gone = 0.50, 0.44, 0.468
    n, rise = count(d), (lambda t: min(1.0, t / top))
    shape = charge_shape(n, top, gone, 2.2)
    hum = cloud(n, 73.4, VOID, shape, glide=lambda t: 1 + 0.06 * rise(t) ** 2, fm=lambda t: 1.8 * rise(t) ** 3)
    gasp = breath(rng, d, [(lambda t: 300 + 500 * rise(t), 7, 1.0), (lambda t: 900 + 1300 * rise(t), 9, 0.6),
                           (lambda t: 2400 + 1600 * rise(t), 10, 0.3)], ring=lambda t: 146.8 * (1 + 0.06 * rise(t) ** 2))
    pull = inhale(rng, n, top + 0.005, 0.42, 293.7, SHINE, 0.45)
    return mul(mix(n, (hum, 1.0), (gasp, 0.7), (pull, 0.55)), shape)


def void_dash(rng, v):
    d, C = 0.42, 0.07
    n, a = count(d), since(C)
    sp = lambda t: math.exp(-((t - C) / (0.03 if t < C else 0.08)) ** 2)
    slip = mul(breath(rng, d, [(lambda t: 450 + 2200 * sp(t), 4, 1.0), (lambda t: 1200 + 3600 * sp(t), 6, 0.4)], ring=146.8),
               [sp(t) ** 1.4 for t in times(n)])
    rip = tear(rng, d, C, 5200, 1400, 0.025, 610, 0.03)
    ring = cloud(n, 146.8, SHINE, env(n, 0.002, 0.09, start=C), squeeze=lambda t: 1 - 0.25 * min(1.0, a(t) / 0.15),
                 fm=lambda t: 2.5 * math.exp(-a(t) / 0.03))
    return fade_tail(haze(mix(n, (slip, 0.6), (rip, 1.0), (ring, 0.6)), 0.35, 0.35), 0.1)


def void_rupture(rng, v):
    d, C = 2.15, 0.28
    n, a = count(d), since(C)
    draw = inhale(rng, n, C - 0.04, 0.26, 110, VOID, 0.6)                     # space breathes in, then a 40 ms drop
    sink = cloud(n, 41.2, VOID[:3], charge_shape(n, C - 0.06, C - 0.04, 3))   # the pressure that vanishes with it
    rip = tear(rng, d, C, 6000, 900, 0.05, 330, 0.06)
    hollow = cloud(n, 55, VOID, env(n, 0.004, 0.75, start=C), squeeze=lambda t: 1 - 0.35 * (1 - math.exp(-a(t) / 0.5)),
                   fm=lambda t: 2.2 * math.exp(-a(t) / 0.12))
    voices = choir(rng, d, n, C, 55, ((520, 900), (340, 650)), 0.03, 0.55)
    ghost = cloud(n, 440, SHINE, env(n, 0.01, 0.6, start=C + 0.02), squeeze=lambda t: 1 - 0.2 * min(1.0, a(t)), fm=lambda t: 0.6)
    opened = haze(mix(n, (rip, 1.0), (hollow, 0.9), (voices, 0.55), (ghost, 0.2)), 1.4, 0.45)
    return fade_tail(mix(n, (draw, 0.5), (sink, 0.3), (opened, 1.0)), 0.4)


def seam_fracture(rng, v):
    d, C = (0.9, 0.85, 1.15)[v], 0.012
    n, k, a = count(d), (1.0, 1.19, 0.75)[v], since(C)
    rip = tear(rng, d, C, 5200 * k, 1100 * k, 0.03, (470, 560, 290)[v], 0.035)
    ring = cloud(n, 98 * k, VOID[:5], env(n, 0.003, (0.22, 0.18, 0.35)[v], start=C),
                 squeeze=lambda t: 1 - 0.3 * min(1.0, a(t) / 0.4), fm=lambda t: 1.5 * math.exp(-a(t) / 0.05))
    layers = [(rip, 1.0), (ring, 0.7)]
    if v == 2:   # the dark take: a low hollow chord and a breathing undertone under the tear
        layers += [(choir(rng, d, n, C, 36.7, ((380, 640), (300, 560)), 0.04, 0.3), 0.5),
                   (cloud(n, 36.7, VOID[:4], env(n, 0.01, 0.35, start=C), squeeze=lambda t: 1 - 0.3 * min(1.0, a(t) / 0.5)), 0.5)]
    return fade_tail(haze(mix(n, *layers), 0.8, 0.4), 0.2)


def parry_clash(rng, v):
    d, C = 0.75, 0.15
    n, s, a = count(d), (1.0, 1.06)[v], since(C)
    gather = inhale(rng, n, C - 0.04, 0.12, 392 * s, SEAL, 0.3)               # the seal drawing energy in
    rip = tear(rng, d, C, 4800 * s, 2200 * s, 0.02, 880 * s, 0.018)
    seal = cloud(n, 392 * s, SEAL, env(n, 0.002, 0.16, start=C), glide=lambda t: 1 + 0.03 * min(1.0, a(t) / 0.3),
                 fm=lambda t: 1.2 * math.exp(-a(t) / 0.02))
    hum = cloud(n, 98 * s, VOID[:4], env(n, 0.003, 0.12, start=C), fm=lambda t: 0.8 * math.exp(-a(t) / 0.04))
    caught = haze(mix(n, (rip, 1.0), (seal, 0.9), (hum, 0.5)), 0.6, 0.4)
    return fade_tail(mix(n, (gather, 0.35), (caught, 1.0)), 0.2)


def counter_cut(rng, v):
    d, C = 0.45, 0.06
    n, s, a = count(d), (1.0, 1.08)[v], since(C)
    sp = lambda t: math.exp(-((t - C) / (0.025 if t < C else 0.07)) ** 2)
    slip = mul(breath(rng, d, [(lambda t: (600 + 2600 * sp(t)) * s, 4, 1.0), (lambda t: (1400 + 3800 * sp(t)) * s, 6, 0.4)],
                      ring=196 * s, depth=0.5), [sp(t) ** 1.3 for t in times(n)])
    rip = tear(rng, d, C, 6200 * s, 2600 * s, 0.015, 740 * s, 0.01)
    spark = cloud(n, 587 * s, SHINE, env(n, 0.002, 0.07, start=C), glide=lambda t: 1 + 0.12 * min(1.0, a(t) / 0.1))
    return fade_tail(haze(mix(n, (slip, 0.7), (rip, 0.8), (spark, 0.35)), 0.4, 0.3), 0.15)


def empower_charge(rng, v):
    d = (0.42, 1.10)[v]
    n, top, gone = count(d), d - 0.07, d - 0.04
    rise = lambda t: min(1.0, t / top)
    shape = charge_shape(n, top, gone, 1.6)
    if v:   # held empowerment throbs, quickening
        shape = [e * (0.72 + 0.28 * math.cos(2 * math.pi * (2.2 * t + 1.8 * t * t))) for e, t in zip(shape, times(n))]
    f0 = (110, 82.4)[v]
    core = cloud(n, f0, VOID[:6], shape, glide=lambda t: 1 + 0.12 * rise(t) ** 2, fm=lambda t: 0.3 + 1.8 * rise(t) ** 2)
    whisper = breath(rng, d, [(lambda t: 450 + 900 * rise(t), 8, 1.0), (lambda t: 1100 + 2200 * rise(t), 10, 0.5)],
                     ring=lambda t: 2 * f0 * (1 + 0.12 * rise(t) ** 2))
    return mix(n, (core, 1.0), (mul(whisper, shape), 0.6))


def heavy_cleave(rng, v):
    d, C = (1.05, 1.45)[v], 0.165
    n, big, a, f0 = count(d), v == 1, since(C), (73.4, 55)[v]
    up = lambda t: min(1.0, t / (C - 0.06)) ** 2
    swing = mul(breath(rng, d, [(lambda t: 300 + 1500 * up(t), 3, 1.0), (lambda t: 900 + 2600 * up(t), 6, 0.4)], ring=2 * f0, depth=0.5),
                charge_shape(n, C - 0.06, C - 0.04, 2))                      # ghost swing, then the pre-impact drop
    draw = inhale(rng, n, C - 0.04, 0.15, 3 * f0, VOID, 0.4)
    rip = tear(rng, d, C, (5600, 4800)[v], (1000, 700)[v], 0.04, (390, 290)[v], (0.04, 0.05)[v])
    hollow = cloud(n, f0, VOID, env(n, 0.003, (0.35, 0.55)[v], start=C), squeeze=lambda t: 1 - 0.3 * min(1.0, a(t) / 0.5),
                   fm=lambda t: (1.8, 2.6)[v] * math.exp(-a(t) / 0.08))
    voices = choir(rng, d, n, C, f0, ((480, 820), (330, 620)), 0.02, (0.25, 0.45)[v])
    hit = haze(mix(n, (rip, 1.0), (hollow, 0.85), (voices, 0.6 if big else 0.3)), (0.7, 1.0)[v], 0.4)
    return fade_tail(mix(n, (swing, 0.4), (draw, 0.35), (hit, 1.0)), 0.3)


def devour_cast(rng, v):
    d, S, O = 1.45, 1.15, 0.012
    n = count(d)
    prog = lambda t: min(1.0, t / S)
    shape = [min(1.0, t / 0.12) ** 1.5 * (0.55 + 0.45 * prog(t)) *
             (1.0 if t < S - 0.05 else max(0.0, 0.5 + 0.5 * math.cos(math.pi * min(1.0, (t - S + 0.05) / 0.08)))) for t in times(n)]
    throat = breath(rng, d, [(lambda t: 260 + 60 * math.sin(2 * math.pi * 0.9 * t), 8, 1.0),
                             (lambda t: 560 + 90 * math.sin(2 * math.pi * 1.3 * t + 1), 9, 0.6), (lambda t: 1500 - 600 * prog(t), 6, 0.35)],
                    ring=lambda t: 82.4 * (1 - 0.25 * prog(t)), depth=0.65)
    fold = cloud(n, 82.4, VOID, shape, squeeze=lambda t: 1 - 0.7 * prog(t) ** 1.5, fm=lambda t: 0.8 + 1.2 * prog(t))  # overtones collapse
    opening = tear(rng, d, O, 3000, 700, 0.04, 210, 0.04)
    shut = haze(cloud(n, 164.8, SEAL[:3], env(n, 0.003, 0.07, start=S), glide=lambda t: 1 - 0.3 * min(1.0, since(S)(t) / 0.1)), 0.4, 0.4)
    return fade_tail(mix(n, (mul(throat, shape), 0.9), (fold, 0.6), (opening, 0.4), (shut, 0.5)), 0.15)


# name -> (sound, take, peak level, sync seconds into the cue, purpose)
CUES = {
    "void_charge": (void_charge, 0, 0.70, 0.0, "ultimate charge: rising detuned void chord + inhaled whisper + reverse shimmer swell; drops to silence before release"),
    "void_dash": (void_dash, 0, 0.74, 0.07, "ultimate release: ghostly breath slip into a small ring-modulated tear, overtones folding inward"),
    "void_rupture": (void_rupture, 0, 0.78, 0.28, "ultimate finisher: reverse inhale, 40 ms pressure drop, resonant space-tear, FM void chord folding inward, dark choir breath, spectral haze tail"),
    "seam_fracture": (seam_fracture, 0, 0.74, 0.012, "delayed seam: small space-tear with an inward-folding hollow ring"),
    "seam_fracture_2": (seam_fracture, 1, 0.74, 0.012, "second seam take (higher, tighter) so repeats differ"),
    "seam_fracture_3": (seam_fracture, 2, 0.76, 0.012, "dark seam take with low hollow chord and breathing undertone (absorb aftershock)"),
    "parry_clash": (parry_clash, 0, 0.78, 0.15, "magic seal draws energy in (reverse swell) then catches it: glassy near-harmonic ring in haze"),
    "parry_clash_2": (parry_clash, 1, 0.78, 0.15, "alternate seal take (pitched up, different seed)"),
    "counter_cut": (counter_cut, 0, 0.74, 0.06, "seal releases: bright breath slip, light tear, rising shimmer"),
    "counter_cut_2": (counter_cut, 1, 0.74, 0.06, "alternate seal release take"),
    "empower_charge": (empower_charge, 0, 0.68, 0.0, "short rising void chord + whisper charge (parry punish wind-up)"),
    "empower_charge_2": (empower_charge, 1, 0.68, 0.0, "held, throbbing void empowerment (absorb)"),
    "heavy_cleave": (heavy_cleave, 0, 0.78, 0.165, "charged punish: ghost swing, reverse inhale, drop, space-tear, hollow chord, haze"),
    "heavy_cleave_2": (heavy_cleave, 1, 0.78, 0.165, "dark empowered cleave: deeper tear, longer FM void chord and choir breath"),
    "devour_cast": (devour_cast, 0, 0.66, 0.012,"hollow ring-pitched throat; overtones collapse to one pitch, closing glassy blink"),
}

# timeline: (anchor frame, cue, gain, role); a cue's sync point lands on its anchor frame.
MIXES = {
    "ultimate": {"frames": 261, "quiet_by": 3.4,
                 "events": [(30, "void_charge", 0.55, "charge starts; vacuum dips out at release f59"),
                            (63, "void_dash", 0.62, "breath slip from release f59 into advancing cut contact"),
                            (81, "void_rupture", 1.0, "strongest finishing rupture (major silhouette)"),
                            (89, "seam_fracture", 0.58, "delayed seam fracture"),
                            (91, "seam_fracture_2", 0.5, "second delayed seam fracture")],
                 "ducks": [(63, 0.6), (81, 0.3), (89, 0.82), (91, 0.88)],
                 "contacts": [(63, "minor"), (81, "major"), (89, "minor"), (91, "minor")]},
    "parry": {"frames": 216, "quiet_by": 3.1,
              "events": [(48, "parry_clash", 0.7, "guard draw at f39, seal catch (impact silhouette)"),
                         (54, "counter_cut", 0.6, "counter slice"),
                         (74, "empower_charge", 0.45, "charged follow-up winds up, dips out at release f98"),
                         (108, "heavy_cleave", 1.0, "heavy charged punish contact (impact silhouette)")],
              "ducks": [(48, 0.55), (54, 0.7), (108, 0.3)],
              "contacts": [(48, "major"), (54, "minor"), (108, "major")]},
    "absorb": {"frames": 287, "quiet_by": 4.35,
               "events": [(30, "devour_cast", 0.45, "devour opens; suction to ~1.65 s"),
                          (102, "empower_charge_2", 0.5, "blade empowerment from ~1.70 s, held f116-f166, dips at release"),
                          (176, "heavy_cleave_2", 1.0, "charged cleave impact (impact silhouette)"),
                          (186, "seam_fracture_3", 0.42, "delayed dark rupture")],
               "ducks": [(176, 0.25), (186, 0.7)],
               "contacts": [(176, "major"), (186, "minor")]},
}


# ---------------------------------------------------------------- rendering

def seed(name):
    return f"rainoray:{name}"


def master(x, level, fade_in=0.003, fade_out=0.025):
    """High-pass (no DC/rumble), gentle top cut, raised-cosine edges, peak = level. No saturation here."""
    x = biquad(biquad(x, "high", 32), "low", 8500)
    n, fi, fo = len(x), count(fade_in), count(fade_out)
    for i in range(fi):
        x[i] *= 0.5 - 0.5 * math.cos(math.pi * i / fi)
    for i in range(fo):
        x[n - 1 - i] *= 0.5 - 0.5 * math.cos(math.pi * i / fo)
    return norm(x, level)


def pcm(x):
    return array.array("h", (max(-32767, min(32767, round(v * 32767))) for v in x))


def samples_for(frames):
    return frames * RATE // FPS


def duck(t, c, depth, pre=0.045, rec=0.2):
    if t < c - pre:
        return 1.0
    if t < c:
        return 1 - (1 - depth) * (0.5 - 0.5 * math.cos(math.pi * (t - c + pre) / pre))
    return 1 - (1 - depth) * math.exp(-(t - c) / rec)


def placements(spec):
    for frame, cue, gain, role in spec["events"]:
        at = frame / FPS
        yield frame, cue, gain, role, at, round((at - CUES[cue][3]) * RATE)


def render_mix(spec, cues):
    n = samples_for(spec["frames"])
    out = [0.0] * n
    ducks = [(f / FPS, depth) for f, depth in spec["ducks"]]
    for frame, cue, gain, role, at, start in placements(spec):
        assert start >= 0, cue
        mine = [(c, dp) for c, dp in ducks if start / RATE < c - 0.002 and abs(c - at) > 1e-9]   # duck earlier tails only
        for i, s in enumerate(cues[cue]):
            j = start + i
            if j >= n:
                break
            g, t = gain, j / RATE
            for c, dp in mine:
                g *= duck(t, c, dp)
            out[j] += g * s / 32767
    return pcm(master(out, MIX_PEAK, 0.005, 0.03))


def to_wav(samples):
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(samples.tobytes())
    return buf.getvalue()


# ---------------------------------------------------------------- measurement and assertions

def win(s, a, b):
    return s[max(0, count(a)):max(0, count(b))]


def peak(s):
    return max((abs(v) for v in s), default=0.0)


def rms(s):
    return math.sqrt(sum(v * v for v in s) / len(s)) if s else 0.0


def db(v):
    return round(20 * math.log10(max(v, 1e-9)), 2)


def stats(s):
    edge = count(0.002)
    return {"samples": len(s), "duration_s": round(len(s) / RATE, 6), "peak": round(peak(s), 5), "peak_dbfs": db(peak(s)),
            "rms_dbfs": db(rms(s)), "dc": round(sum(s) / len(s), 7), "first_2ms_peak": round(peak(s[:edge]), 5),
            "last_2ms_peak": round(peak(s[-edge:]), 5)}


def contact(s, t):
    hit = win(s, t - 0.004, t + 0.05)
    return {"peak": round(peak(hit), 4), "rms_dbfs": db(rms(hit)), "pre_rms_dbfs": db(rms(win(s, t - 0.035, t - 0.006)))}


def faults(name, s):
    """Audio assertions shared by generation, --check and --selftest."""
    bad = [f"{name}: non-finite sample"] if not all(math.isfinite(v) for v in s) else []
    st = stats(s)
    if st["peak"] > CUE_CEILING:
        bad.append(f"{name}: peak {st['peak']} above {CUE_CEILING}")
    if sum(1 for v in s if abs(v) >= 32766 / 32767) > 0:
        bad.append(f"{name}: hard-clipped samples")
    if abs(st["dc"]) > 0.002:
        bad.append(f"{name}: DC offset {st['dc']}")
    if st["first_2ms_peak"] > 0.01 or st["last_2ms_peak"] > 0.01:
        bad.append(f"{name}: edges not faded ({st['first_2ms_peak']}, {st['last_2ms_peak']})")
    if name in MIXES:
        spec = MIXES[name]
        if len(s) != samples_for(spec["frames"]):
            bad.append(f"{name}: {len(s)} samples, expected {samples_for(spec['frames'])}")
        if peak(win(s, 0, 0.4)) > 0.002 or peak(s[-count(0.25):]) > 0.004:
            bad.append(f"{name}: first/last stretch not silent")
        if peak(win(s, spec["quiet_by"], 99)) > 0.03:
            bad.append(f"{name}: tail still loud after {spec['quiet_by']} s ({peak(win(s, spec['quiet_by'], 99)):.4f})")
        for frame, kind in spec["contacts"]:
            c = contact(s, frame / FPS)
            hit_rms = 10 ** (c["rms_dbfs"] / 20)
            if kind == "major" and (c["peak"] < 0.45 or c["rms_dbfs"] < -24 or 10 ** (c["pre_rms_dbfs"] / 20) > 0.6 * hit_rms):
                bad.append(f"{name}: major contact f{frame} weak or not contrasted {c}")
            if kind == "minor" and (c["peak"] < 0.12 or c["rms_dbfs"] < -34):
                bad.append(f"{name}: contact f{frame} inaudible {c}")
        if name == "ultimate" and peak(win(s, samples_for(261) / RATE - 0.8, 99)) > 0.006:
            bad.append("ultimate: last 0.8 s is not at rest")
        if name == "parry" and rms(win(s, 1.796, 1.85)) <= rms(win(s, 0.796, 0.85)):
            bad.append("parry: punish is not heavier than the catch")
        if name == "absorb":
            devour = max(rms(win(s, a / 100, a / 100 + 0.05)) for a in range(50, 165, 5))
            if devour >= 0.5 * rms(win(s, 2.929, 2.983)):
                bad.append("absorb: devour is not clearly quieter than the heavy hit")
    return bad


def floats(raw):
    return [v / 32767 for v in array.array("h", raw)]


# ---------------------------------------------------------------- build, write, verify

def build():
    cues = {name: pcm(master(fn(random.Random(seed(name)), take), level)) for name, (fn, take, level, _, _) in CUES.items()}
    mixes = {name: render_mix(spec, cues) for name, spec in MIXES.items()}
    files = {f"cues/{k}.wav": to_wav(v) for k, v in cues.items()}
    files.update({f"mixes/{k}.wav": to_wav(v) for k, v in mixes.items()})
    man = {"generator": "generate_rainoray_skill_sfx.py", "format": "WAV PCM16 mono 22050 Hz", "fps": FPS,
           "mix_peak_target": MIX_PEAK, "cue_ceiling": CUE_CEILING, "cues": {}, "mixes": {}}
    for name, (fn, take, level, sync, purpose) in CUES.items():
        f = f"cues/{name}.wav"
        man["cues"][name] = {"file": f, "purpose": purpose, "seed": seed(name), "function": fn.__name__, "take": take,
                             "level": level, "sync_s": sync, "sha256": hashlib.sha256(files[f]).hexdigest(),
                             **stats(floats(cues[name].tobytes()))}
    for name, spec in MIXES.items():
        f, s = f"mixes/{name}.wav", floats(mixes[name].tobytes())
        man["mixes"][name] = {
            "file": f, "frames": spec["frames"], "expected_samples": samples_for(spec["frames"]), "quiet_by_s": spec["quiet_by"],
            "events": [{"frame": fr, "time_s": round(at, 6), "cue": cue, "gain": g, "sync_s": CUES[cue][3],
                        "onset_s": round(st / RATE, 6), "onset_sample": st, "role": role}
                       for fr, cue, g, role, at, st in placements(spec)],
            "ducks": [{"frame": fr, "time_s": round(fr / FPS, 6), "depth": dp} for fr, dp in spec["ducks"]],
            "contacts": [{"frame": fr, "time_s": round(fr / FPS, 6), "kind": k, **contact(s, fr / FPS)} for fr, k in spec["contacts"]],
            "tail_energy_rms_dbfs": db(aftermath(s, name)), "sha256": hashlib.sha256(files[f]).hexdigest(), **stats(s)}
    return files, json.dumps(man, indent=2, sort_keys=True) + "\n"


MAIN_HIT = {"ultimate": 81, "parry": 108, "absorb": 176}


def aftermath(s, name):
    """Mean energy over the 1.5 s from just before the main hit. Reported only: energy, not perceived quality."""
    return rms(win(s, MAIN_HIT[name] / FPS - 0.05, MAIN_HIT[name] / FPS + 1.45))


def verify(out, files, manifest):
    errs = []
    for rel, data in files.items():
        p = out / rel
        if not p.is_file():
            errs.append(f"missing {rel}")
            continue
        try:
            with wave.open(str(p)) as w:
                fmt, raw = (w.getnchannels(), w.getsampwidth(), w.getframerate()), w.readframes(w.getnframes())
        except (wave.Error, EOFError, OSError) as e:
            errs.append(f"{rel}: unreadable WAV ({e})")
            continue
        if fmt != (1, 2, RATE):
            errs.append(f"{rel}: format {fmt}, expected mono/16-bit/{RATE}")
            continue
        if p.read_bytes() != data:
            errs.append(f"{rel}: differs from the deterministic render")
        s = floats(raw)
        errs += faults(Path(rel).stem if rel.startswith("mixes/") else rel, s)
    for sub in ("cues", "mixes"):
        for p in sorted((out / sub).glob("*.wav")) if (out / sub).is_dir() else []:
            if f"{sub}/{p.name}" not in files:
                errs.append(f"unexpected file {sub}/{p.name}")
    m = out / "manifest.json"
    if not m.is_file():
        errs.append("missing manifest.json")
    elif m.read_text() != manifest:
        errs.append("manifest.json differs from the deterministic render")
    return errs


def write(out, files, manifest):
    for rel, data in files.items():
        (out / rel).parent.mkdir(parents=True, exist_ok=True)
        (out / rel).write_bytes(data)
    (out / "manifest.json").write_text(manifest)


def selftest(files, manifest):
    """Tampered, truncated, missing and stray files must all fail the check."""
    lab = Path(tempfile.mkdtemp(prefix="b2bj-rainoray-sfx-"))
    results = []
    try:
        write(lab, files, manifest)
        results.append(("clean copy passes", not verify(lab, files, manifest)))
        p = lab / "mixes/ultimate.wav"
        good = p.read_bytes()
        bent = bytearray(good)
        mid = 44 + 2 * count(1.36)                       # inside the rupture, so the edit is real audio
        bent[mid:mid + 400] = bytes(400)
        assert bytes(bent) != good
        p.write_bytes(bytes(bent))
        results.append(("altered mix fails", bool(verify(lab, files, manifest))))
        p.write_bytes(good[:-2000])
        results.append(("truncated mix fails", bool(verify(lab, files, manifest))))
        p.write_bytes(good)
        (lab / "cues/void_dash.wav").unlink()
        results.append(("missing cue fails", bool(verify(lab, files, manifest))))
        write(lab, files, manifest)
        (lab / "cues/stray.wav").write_bytes(files["cues/void_dash.wav"])
        results.append(("stray file fails", bool(verify(lab, files, manifest))))
        (lab / "cues/stray.wav").unlink()
        (lab / "manifest.json").write_text(manifest.replace('"frames": 261', '"frames": 260'))
        results.append(("edited manifest fails", bool(verify(lab, files, manifest))))
    finally:
        shutil.rmtree(lab, ignore_errors=True)
    for label, ok in results:
        print(f"selftest {'ok  ' if ok else 'FAIL'} {label}")
    return all(ok for _, ok in results)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--out", type=Path, default=Path(__file__).resolve().parent / "output")
    ap.add_argument("--check", action="store_true", help="verify existing files against a fresh render")
    ap.add_argument("--selftest", action="store_true", help="prove the check rejects bad output")
    args = ap.parse_args()
    files, manifest = build()
    if args.selftest:
        sys.exit(0 if selftest(files, manifest) else 1)
    if not args.check:
        write(args.out, files, manifest)
    errs = verify(args.out, files, manifest)
    if errs:
        sys.exit("rainoray sfx check FAILED:\n  " + "\n  ".join(errs))
    m = json.loads(manifest)
    for name, d in m["mixes"].items():
        hits = ", ".join(f"f{c['frame']} {c['peak']:.2f}/{c['rms_dbfs']}dB (pre {c['pre_rms_dbfs']})" for c in d["contacts"])
        print(f"{name}: {d['samples']} samples, peak {d['peak_dbfs']} dBFS, rms {d['rms_dbfs']} dBFS | {hits}")
    print(f"{len(files)} files {'match' if args.check else 'written to'} {args.out}; all assertions pass")


if __name__ == "__main__":
    main()
