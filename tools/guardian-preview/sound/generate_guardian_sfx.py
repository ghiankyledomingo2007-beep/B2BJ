#!/usr/bin/env python3
"""Guardian ("Warden") boss sound bank for Blob to Blade: 16 procedural cues plus one audition mix.

    python3 generate_guardian_sfx.py               # render output/*.wav, output/manifest.json, output/check_report.json
    python3 generate_guardian_sfx.py --check       # re-render in memory, verify the files on disk, rewrite the report,
                                                   #   exit 1 on any fault
    python3 generate_guardian_sfx.py --selftest    # prove --check rejects tampered, truncated, missing and stray files

A huge dark-violet armored golem with gold horns and a brilliant cyan core. Nothing here is rubble or a stock thump:
  * armor   = modal plate resonances (inharmonic, controlled decays) + a hollow comb-tuned shell cavity
  * weight  = a short pitch-falling body tone carried by its own 2nd/3rd harmonics so it survives laptop speakers
  * core    = coherent harmonic partial stacks (A minor colour: A1/E2/A2/C3) with slow chorus drift, cyan glass shimmer
  * pressure/void = reversed space swells that suck into the hit, falling resonant air bands, pre-impact drop-outs
  * tearing = noise only ever heard through banks of harmonic resonators, so it is pitched, never plain hiss
Standard library only (no numpy/scipy needed), seeded per cue, 22050 Hz mono PCM16, deterministic, no saturation,
no bit reduction. Every file is high-passed, faded at both edges, DC-corrected and kept below -1 dBFS.
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

RATE = 22050
TAU = 2.0 * math.pi
VERSION = "guardian-sfx-1.0"
CUE_CEILING = 0.891        # -1.0 dBFS: no cue sample may exceed this
MIX_CEILING = 0.891        # same ceiling for the audition and the in-game overlap simulations
EDGE_LSB = 3               # first/last sample must be within 3 LSB of zero


# ---------------------------------------------------------------- basic list-of-floats helpers

def count(seconds):
    return int(round(seconds * RATE))


def zeros(n):
    return [0.0] * n


def mix_into(dst, src, gain=1.0, offset=0):
    """dst[offset + i] += gain * src[i], clipped to dst."""
    n = len(dst)
    lo = max(0, -offset)
    hi = min(len(src), n - offset)
    for i in range(lo, hi):
        dst[offset + i] += gain * src[i]
    return dst


def mix(n, *layers):
    """Sum (signal, gain) pairs into a new list of length n."""
    out = [0.0] * n
    for sig, g in layers:
        mix_into(out, sig, g)
    return out


def mul(a, b):
    return [x * y for x, y in zip(a, b)]


def scale(x, g):
    return [v * g for v in x]


def place(n, at, seg):
    """Full-length list of n samples with seg starting at time `at` seconds."""
    out = [0.0] * n
    return mix_into(out, seg, 1.0, count(at))


def peak(x):
    return max((abs(v) for v in x), default=0.0)


def rms(x):
    return math.sqrt(sum(v * v for v in x) / len(x)) if x else 0.0


def db(v):
    return round(20.0 * math.log10(max(v, 1e-9)), 2)


def window(x, a, b):
    return x[max(0, count(a)):max(0, count(b))]


def unit(x, level=1.0):
    """Scale so the peak equals level (internal layer balancing only, never the final master)."""
    p = peak(x)
    return scale(x, level / p) if p > 1e-12 else list(x)


# ---------------------------------------------------------------- envelopes

def hit_env(n, start, attack, decay):
    """Silent before start, raised-cosine attack, then exponential decay with time constant `decay` seconds."""
    out = [0.0] * n
    s = count(start)
    a = max(1, count(attack))
    k = math.exp(-1.0 / (decay * RATE))
    e = 1.0
    for i in range(max(0, s), n):
        j = i - s
        if j < a:
            out[i] = 0.5 - 0.5 * math.cos(math.pi * j / a)
        else:
            e *= k
            out[i] = e
    return out


def shape(n, points):
    """Raised-cosine segments through sorted (time_s, level) points; holds the first/last level outside them."""
    out = [float(points[-1][1])] * n
    for i in range(min(n, count(points[0][0]))):
        out[i] = float(points[0][1])
    for (t0, v0), (t1, v1) in zip(points, points[1:]):
        i0, i1 = count(t0), count(t1)
        span = max(1, i1 - i0)
        for i in range(max(0, i0), min(n, i1)):
            u = (i - i0) / span
            out[i] = v0 + (v1 - v0) * (0.5 - 0.5 * math.cos(math.pi * u))
    return out


def ramp(t, t0, t1, p=1.0):
    """0 before t0, ((t - t0) / (t1 - t0)) ** p between, 1 after."""
    if t <= t0:
        return 0.0
    if t >= t1:
        return 1.0
    return ((t - t0) / (t1 - t0)) ** p


# ---------------------------------------------------------------- noise and filters

def noise(rng, n):
    r = rng.random
    return [2.0 * r() - 1.0 for _ in range(n)]


def smooth_random(rng, n, rate, depth, base=1.0):
    """Slowly wandering gain: cosine-interpolated random points `rate` times per second, base +/- depth."""
    step = max(2, int(RATE / rate))
    pts = [rng.uniform(-1.0, 1.0) for _ in range(n // step + 3)]
    out = [0.0] * n
    for i in range(n):
        k, fr = divmod(i, step)
        u = 0.5 - 0.5 * math.cos(math.pi * fr / step)
        out[i] = base + depth * (pts[k] + (pts[k + 1] - pts[k]) * u)
    return out


def _coef(kind, f, q, gain_db):
    f = min(max(f, 12.0), 0.45 * RATE)
    w = TAU * f / RATE
    cw, sw = math.cos(w), math.sin(w)
    al = sw / (2.0 * q)
    if kind == "low":
        b0, b1, b2 = (1 - cw) / 2, 1 - cw, (1 - cw) / 2
        a0, a1, a2 = 1 + al, -2 * cw, 1 - al
    elif kind == "high":
        b0, b1, b2 = (1 + cw) / 2, -(1 + cw), (1 + cw) / 2
        a0, a1, a2 = 1 + al, -2 * cw, 1 - al
    elif kind == "band":                     # constant 0 dB peak gain at f
        b0, b1, b2 = al, 0.0, -al
        a0, a1, a2 = 1 + al, -2 * cw, 1 - al
    elif kind == "peak":
        A = 10 ** (gain_db / 40)
        b0, b1, b2 = 1 + al * A, -2 * cw, 1 - al * A
        a0, a1, a2 = 1 + al / A, -2 * cw, 1 - al / A
    elif kind == "lowshelf":
        A = 10 ** (gain_db / 40)
        sa = 2 * math.sqrt(A) * al
        b0 = A * ((A + 1) - (A - 1) * cw + sa)
        b1 = 2 * A * ((A - 1) - (A + 1) * cw)
        b2 = A * ((A + 1) - (A - 1) * cw - sa)
        a0 = (A + 1) + (A - 1) * cw + sa
        a1 = -2 * ((A - 1) + (A + 1) * cw)
        a2 = (A + 1) + (A - 1) * cw - sa
    else:
        raise ValueError(kind)
    return b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0


def biquad(x, kind, f, q=0.7071, gain_db=0.0, step=16):
    """RBJ biquad, direct form I. f may be a callable f(t) -> Hz, re-evaluated every `step` samples."""
    out = [0.0] * len(x)
    x1 = x2 = y1 = y2 = 0.0
    if callable(f):
        b0 = b1 = b2 = a1 = a2 = 0.0
        for i, v in enumerate(x):
            if i % step == 0:
                b0, b1, b2, a1, a2 = _coef(kind, f(i / RATE), q, gain_db)
            y = b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2, x1, y2, y1 = x1, v, y1, y
            out[i] = y
    else:
        b0, b1, b2, a1, a2 = _coef(kind, f, q, gain_db)
        for i, v in enumerate(x):
            y = b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2, x1, y2, y1 = x1, v, y1, y
            out[i] = y
    return out


def lowpass(x, f, order=2):
    for _ in range(order // 2):
        x = biquad(x, "low", f)
    return x


def highpass(x, f, order=2):
    for _ in range(order // 2):
        x = biquad(x, "high", f)
    return x


def resonators(x, bank, q):
    """Parallel high-Q band-passes [(Hz, gain)]: noise in, pitched resonance out."""
    out = [0.0] * len(x)
    for f, g in bank:
        if f < 0.45 * RATE:
            mix_into(out, biquad(x, "band", f, q), g)
    return out


def comb(x, freq, fb, damp=0.35):
    """Feedback comb with a damped loop: the hollow shell cavity. freq may be callable (interpolated delay)."""
    n = len(x)
    out = [0.0] * n
    lp = 0.0
    fixed = not callable(freq)
    d = RATE / freq if fixed else RATE / freq(0.0)
    for i in range(n):
        if not fixed and i % 8 == 0:
            d = RATE / freq(i / RATE)
        p = i - d
        if p >= 1.0:
            k = int(p)
            fr = p - k
            dv = out[k] + fr * (out[k + 1] - out[k])
        else:
            dv = 0.0
        lp = dv + damp * (lp - dv)
        out[i] = x[i] + fb * lp
    return out


def space(x, rt=0.8, wet=0.3, damp=0.4, predelay=0.011, size=1.0):
    """Dark diffuse room: six damped combs in parallel, three allpasses in series; wet scaled to the dry RMS."""
    n = len(x)
    pd = min(n, count(predelay))
    src = [0.0] * pd + list(x[:n - pd])
    acc = [0.0] * n
    for ms in (29.7, 37.1, 41.1, 43.7, 47.3, 53.9):
        d = max(1, int(ms * size * RATE / 1000))
        g = 10 ** (-3.0 * (d / RATE) / rt)
        buf = [0.0] * n
        tap = [0.0] * n                      # comb output is the delayed loop, so the room starts after d
        lp = 0.0
        for i in range(n):
            dv = buf[i - d] if i >= d else 0.0
            lp = dv + damp * (lp - dv)
            buf[i] = src[i] + g * lp
            tap[i] = dv
        acc = [a + b for a, b in zip(acc, tap)]
    for ms, ag in ((5.1, 0.6), (1.7, 0.6), (11.3, 0.5)):
        d = max(1, int(ms * RATE / 1000))
        y = [0.0] * n
        for i in range(n):
            if i >= d:
                y[i] = -ag * acc[i] + acc[i - d] + ag * y[i - d]
            else:
                y[i] = -ag * acc[i]
        acc = y
    wr, dr = rms(acc), rms(x)
    g = wet * dr / wr if wr > 1e-12 else 0.0
    return [a + g * b for a, b in zip(x, acc)]


def tail_only(x, rt, damp=0.45, size=1.0):
    """Just the wet part of space(), at the dry RMS (used for reversed swells)."""
    w = space(x, rt=rt, wet=1.0, damp=damp, predelay=0.0, size=size)
    return [a - b for a, b in zip(w, x)]


# ---------------------------------------------------------------- tonal generators

def modes(n, start, partials):
    """Decaying sinusoids [(Hz, amp, decay_s)] from `start` s, sine phase (click-free start), complex recursion."""
    out = [0.0] * n
    s = count(start)
    for f, a, dec in partials:
        if f >= 0.45 * RATE or a == 0.0:
            continue
        r = math.exp(-1.0 / (dec * RATE))
        w = TAU * f / RATE
        rot = complex(r * math.cos(w), r * math.sin(w))
        z = complex(a, 0.0)
        end = min(n, s + int(dec * RATE * 9.3) + 1)   # stop at -80 dB
        for i in range(max(0, s), end):
            out[i] += z.imag
            z *= rot
    return out


def tone(n, f0, partials, env, glide=None, spread=0.0, drift=0.0, phase=0.0):
    """Additive partial stack [(ratio, gain)] at f0 * glide(t), shaped by env (list). spread > 0 doubles every
    partial into a +/- detuned pair (slow chorus beating); drift adds a slow per-partial pitch wobble."""
    out = [0.0] * n
    live = [i for i in range(n) if env[i] > 1e-6]
    if not live:
        return out
    i0, i1 = live[0], live[-1] + 1
    gl = [glide(i / RATE) for i in range(i0, i1)] if glide else None
    top = max(gl) if gl else 1.0
    sides = (-1.0, 1.0) if spread else (0.0,)
    for k, (ratio, g) in enumerate(partials):
        if f0 * ratio * top * (1 + spread + drift) >= 0.45 * RATE:
            continue
        for side in sides:
            ph = phase + 0.61 * k + 1.3 * side
            wob = 0.19 + 0.13 * k + 0.07 * side
            base = TAU * f0 * ratio * (1 + side * spread) / RATE
            for j, i in enumerate(range(i0, i1)):
                m = gl[j] if gl else 1.0
                if drift:
                    m *= 1 + drift * math.sin(TAU * wob * i / RATE + k)
                ph += base * m
                out[i] += g * env[i] * math.sin(ph)
    return out


def sweep_tone(n, start, f_from, f_to, fall, env_attack, env_decay, harmonics=((1, 1.0),)):
    """Exponentially gliding partial stack (body weight / lift): f = f_to + (f_from - f_to) * exp(-t / fall).
    Runs until its envelope is 80 dB down (or the cue ends)."""
    out = [0.0] * n
    e = hit_env(n, start, env_attack, env_decay)
    s = count(start)
    end = min(n, s + count(env_attack) + int(env_decay * RATE * 9.3) + 1)
    for ratio, g in harmonics:
        if max(f_from, f_to) * ratio >= 0.45 * RATE:
            continue
        ph = 0.0
        for i in range(s, end):
            f = (f_to + (f_from - f_to) * math.exp(-(i - s) / (fall * RATE))) * ratio
            ph += TAU * f / RATE
            out[i] += g * e[i] * math.sin(ph)
    return out


# ---------------------------------------------------------------- the Guardian palette

A1, E2, A2, C3, D3, E3, A3 = 55.0, 82.41, 110.0, 130.81, 146.83, 164.81, 220.0
# coherent harmonic core (cyan): plain harmonics, so every core sound locks to one pitch centre
CORE = [(1, 1.0), (2, 0.62), (3, 0.44), (4, 0.30), (5, 0.21), (6, 0.17), (8, 0.11), (10, 0.06), (12, 0.04)]
# the same core an octave down with a weak fundamental: felt low, but the pitch lives in the 110-660 Hz harmonics
LOW_CORE = [(1, 0.5), (2, 1.0), (3, 0.6), (4, 0.42), (5, 0.25), (6, 0.2), (8, 0.1)]
# violet colour: root, fifth, octave, minor tenth, twelfth -> dark A-minor body without being a melody
VIOLET = [(1, 1.0), (1.498, 0.45), (2, 0.58), (2.378, 0.27), (3, 0.33), (4, 0.2), (6, 0.1), (8, 0.06)]
# cyan glass shimmer, used at high roots and low level
GLASS = [(1, 1.0), (2, 0.42), (3, 0.26), (4, 0.15), (6, 0.07)]
# thick curved armor plate: inharmonic, dense in the 150 Hz - 1.2 kHz band laptops reproduce
PLATE = [1.0, 1.594, 2.136, 2.296, 2.653, 2.918, 3.501, 4.152, 4.965, 5.435, 6.208, 7.114]
# rim / edge modes: short and bright, they carry the transient on small speakers
EDGE = [9.87, 12.31, 14.6, 17.9]


# ---------------------------------------------------------------- layers (each returns a full-length list)

def contact(rng, n, at, bright=2400.0, knock=380.0):
    """First 10 ms of a hit: a band-passed tick (laptop clarity) plus a short tonal knock. Not a crack/gunshot:
    there is no broadband noise body, only two narrow, fast-decaying components."""
    m = count(0.03)
    burst = mul(noise(rng, m), hit_env(m, 0.0, 0.0003, 0.0022))
    tick = biquad(biquad(burst, "band", bright, 0.9), "band", bright * 0.62, 1.2)
    knock_t = modes(m, 0.0, [(knock, 1.0, 0.011), (knock * 1.53, 0.5, 0.007)])
    return place(n, at, mix(m, (unit(tick), 0.55), (unit(knock_t), 0.6)))


def plate(rng, n, at, base, decay=0.3, bright=1.0, edge=0.6, detune=0.006):
    """Armor plate: inharmonic modes, upper modes damped progressively faster (controlled, not a bell)."""
    parts = []
    for k, r in enumerate(PLATE):
        f = base * r * (1 + rng.uniform(-detune, detune))
        parts.append((f, (bright ** (k / 6.0)) / (1 + 0.32 * k), decay / (1 + 0.22 * k)))
    for k, r in enumerate(EDGE):
        parts.append((base * r * (1 + rng.uniform(-detune, detune)), 0.22 * edge / (1 + k), 0.02 + 0.008 * k))
    return unit(modes(n, at, parts))


def hollow(rng, n, at, freq, length=0.03, fb=0.86, damp=0.42):
    """Hollow shell cavity: a short dark noise puff ringing a comb tuned to freq (the armor is empty inside)."""
    m = n - count(at)
    if m <= 0:
        return zeros(n)
    puff = lowpass(mul(noise(rng, m), hit_env(m, 0.0, 0.001, length)), 1700)
    return place(n, at, unit(comb(puff, freq, fb, damp)))


def weight(n, at, f_from=96.0, f_to=46.0, fall=0.05, decay=0.16):
    """Body weight: a falling tone whose 2nd-4th harmonics carry it on laptop speakers (sub stays modest)."""
    return unit(sweep_tone(n, at, f_from, f_to, fall, 0.0015, decay, ((1, 1.0), (2, 0.7), (3, 0.38), (4, 0.18))))


def core(n, f0, partials, env, glide=None, spread=0.0035, drift=0.0015):
    """Harmonic core resonance with slow chorus beating (never a raw oscillator)."""
    return tone(n, f0, partials, env, glide=glide, spread=spread, drift=drift)


def shimmer(n, at, f0, attack, decay, spread=0.004):
    """Cyan glass partials above the core, low in the mix."""
    return tone(n, f0, GLASS, hit_env(n, at, attack, decay), spread=spread, drift=0.002)


def air(rng, n, at, attack, decay, f_from, f_to, fall, q=1.2):
    """Pressure displacement: noise through a resonant band gliding f_from -> f_to (time constant fall). Two
    passes make it a pitched gust instead of hiss."""
    m = n - count(at)
    if m <= 0:
        return zeros(n)

    def f(t):
        return f_to + (f_from - f_to) * math.exp(-t / fall)

    band = biquad(biquad(noise(rng, m), "band", f, q), "band", f, q * 0.8)
    return place(n, at, mul(unit(band), hit_env(m, 0.0, attack, decay)))


def suction(rng, n, end, length, f0, rt=0.55):
    """Void suction: a struck core chord plus resonant air through the room, reversed, so the reverb tail is
    drawn *into* `end`. The room's own ~30 ms pre-echo gap becomes the pre-impact silence before `end`."""
    m = count(length)
    e = hit_env(m, 0.0, 0.002, length / 5)
    seg = mix(m, (unit(tone(m, f0, CORE[:6], e, spread=0.004)), 0.8),
              (unit(mul(resonators(noise(rng, m), [(f0 * 4, 1.0), (f0 * 6, 0.7), (f0 * 9, 0.45)], 9.0), e)), 0.6))
    swell = unit(tail_only(seg, rt)[::-1])
    return mix_into(zeros(n), swell, 1.0, count(end) - m)


def groan(rng, n, t0, t1, base, q=26.0, bend=0.04, grit=0.08):
    """Armor strain: irregular stick-slip friction pulses (9-26 per second) plus a little continuous friction,
    exciting high-Q plate resonances whose pitch sags under load. Creak and groan, never gravel."""
    m = count(t1 - t0)
    span = max(1e-3, t1 - t0)
    exc = scale(noise(rng, m), grit)
    i = 0
    while i < m:
        amp = rng.uniform(0.4, 1.0)
        width = max(2, count(rng.uniform(0.002, 0.006)))
        for j in range(width):
            if i + j < m:
                exc[i + j] += amp * math.sin(math.pi * j / width) * (2.0 * rng.random() - 1.0)
        i += max(1, count(1.0 / rng.uniform(9.0, 26.0)))
    out = zeros(m)
    for k, r in enumerate(PLATE[:7]):
        def f(t, r=r):
            return base * r * (1.0 - bend * math.sin(math.pi * min(1.0, t / span)))
        mix_into(out, biquad(exc, "band", f, q, step=32), 1.0 / (1 + 0.35 * k))
    return place(n, t0, unit(out))


def tear(rng, n, start, f0, q=22.0, top=14, rate=17.0, depth=0.45, tilt=0.8):
    """Magical tearing: noise through harmonic resonators k * f0 (k = 1..top), each with its own wandering gain,
    so the texture is a pitched, shifting chord-shred instead of white noise."""
    m = n - count(start)
    if m <= 0:
        return zeros(n)
    src = noise(rng, m)
    out = zeros(m)
    for k in range(1, top + 1):
        f = f0 * k
        if f >= 0.45 * RATE:
            break
        band = biquad(src, "band", f, q)
        wob = smooth_random(rng, m, rate * (0.7 + 0.08 * k), depth, 1.0)
        g = tilt ** (k - 1)
        out = [o + g * b * w for o, b, w in zip(out, band, wob)]
    return place(n, start, unit(out))


def rattle(rng, n, at, hits, span, base, level=0.35):
    """Plates settling after the contact: a few small, decreasing modal taps (armor, not debris)."""
    out = zeros(n)
    t = at
    for h in range(hits):
        t += rng.uniform(0.35, 1.0) * span / hits
        parts = [(base * r * rng.uniform(0.97, 1.03), (0.8 ** h) / (1 + 0.4 * k), 0.025 + 0.015 * rng.random())
                 for k, r in enumerate(PLATE[2:9])]
        mix_into(out, modes(n, t, parts), level)
    return out


def fade_in(x, seconds):
    m = min(len(x), max(1, count(seconds)))
    out = list(x)
    for i in range(m):
        out[i] *= 0.5 - 0.5 * math.cos(math.pi * i / m)
    return out


def fade_tail(x, seconds):
    """Raised-cosine taper over the last `seconds`; the final sample is exactly zero."""
    n = len(x)
    m = min(n, max(1, count(seconds)))
    out = list(x)
    for i in range(m):
        out[n - m + i] *= 0.5 + 0.5 * math.cos(math.pi * (i + 1) / m)
    return out


def gate(x, points):
    """Multiply by a shape() envelope (used for drop-outs and pre-impact silences)."""
    return mul(x, shape(len(x), points))


# ---------------------------------------------------------------- the 16 cues: (rng, take) -> list of floats

def warden_windup(rng, v):
    """0.86 s: the armor shifts at once (readable from the first frame), strain creaks, the low core hum and a void
    swell rise together, then everything drops out by 0.735 s so the slam lands after a beat of silence."""
    n = count(0.86)
    rise_glide = lambda t: 1.0 + 0.122 * ramp(t, 0.05, 0.80, 1.6)   # two semitones up as the arm loads
    shift = mix(n, (plate(rng, n, 0.006, 150.0, decay=0.12, bright=0.7, edge=0.4), 0.8),
                (hollow(rng, n, 0.006, 118.0, length=0.02, fb=0.8), 0.5))
    strain = mul(groan(rng, n, 0.03, 0.82, 140.0, q=24.0, bend=0.05),
                 shape(n, [(0.03, 0.25), (0.75, 1.0), (0.82, 0.0)]))
    rise = shape(n, [(0.0, 0.0), (0.09, 0.18), (0.72, 0.92), (0.80, 1.0), (0.835, 0.0)])
    hum = core(n, A1, LOW_CORE, rise, glide=rise_glide)
    fifth = core(n, E2, VIOLET[:5], rise, glide=rise_glide, spread=0.005)
    gust = air(rng, n, 0.02, 0.74, 0.04, 200.0, 1150.0, 0.55, q=2.2)
    pull = suction(rng, n, 0.80, 0.52, A3, rt=0.5)
    body = mix(n, (shift, 0.45), (strain, 0.42), (unit(hum), 0.55), (unit(fifth), 0.25), (gust, 0.22), (pull, 0.38))
    return gate(body, [(0.0, 1.0), (0.63, 1.0), (0.735, 0.0)])


def warden_slam(rng, v):
    """Heavy fist contact at 3 ms: tick + knock, two plate layers, hollow shell, body weight and settling plates;
    the violet core chord flares and cyan glass shimmers through a dark room. Take 2: same recipe, a semitone up,
    fifth-up glass, one less settling tap."""
    d, k = (1.05, 1.0)[v], (1.0, 1.059)[v]
    n, C = count(d), 0.003
    hit = mix(n,
              (contact(rng, n, C, bright=2300.0 * k, knock=360.0 * k), 0.55),
              (plate(rng, n, C, 128.0 * k, decay=0.34, bright=1.0, edge=0.7), 0.5),
              (plate(rng, n, C + 0.0015, 212.0 * k, decay=0.16, bright=0.8, edge=0.5), 0.3),
              (hollow(rng, n, C, 98.0 * k, length=0.03, fb=0.86), 0.42),
              (weight(n, C, 92.0 * k, 52.0, fall=0.045, decay=0.17), 0.55),
              (unit(rattle(rng, n, C + 0.06, (3, 2)[v], 0.22, 240.0 * k, 1.0)), 0.18))
    flare = core(n, A2 * k, VIOLET, hit_env(n, C + 0.004, 0.012, 0.42),
                 glide=lambda t: 1.0 - 0.015 * ramp(t, C, 0.6))
    glass = shimmer(n, C + 0.01, (880.0, 1320.0)[v] * k, 0.02, 0.3)
    push = air(rng, n, C + 0.004, 0.006, 0.12, 1300.0, 340.0, 0.08, q=1.0)
    body = mix(n, (hit, 1.0), (unit(flare), 0.34), (unit(glass), 0.11), (push, 0.22))
    return fade_tail(space(body, rt=0.9, wet=0.32, damp=0.45), 0.22)


def warden_sweep(rng, v):
    """Heavy hollow backhand, leading edge at 2 ms: a cyan shearing edge (gliding comb-pitched noise and a falling
    glass partial) over a glancing plate scrape, a hollow 'whoom' gust ringing the shell cavity, a falling core
    trail and a light body. Take 2 is a little lower and shorter."""
    d, k = (0.84, 0.80)[v], (1.0, 0.94)[v]
    n, C = count(d), 0.002
    m = n - count(C)
    edge_src = mul(noise(rng, m), hit_env(m, 0.0, 0.002, 0.13))
    edge = comb(edge_src, lambda t: (520.0 - 260.0 * min(1.0, t / 0.3)) * k, 0.72, 0.25)
    edge = biquad(edge, "band", lambda t: 900.0 + 2500.0 * k * math.exp(-t / 0.09), 1.4)
    fall_glass = tone(n, 1760.0 * k, GLASS[:3], hit_env(n, C, 0.003, 0.12),
                      glide=lambda t: 1.0 - 0.33 * ramp(t, C, C + 0.25, 0.7), spread=0.004)
    shear = mix(n, (place(n, C, unit(edge)), 1.0), (unit(fall_glass), 0.35))
    scrape_exc = mul(noise(rng, n), hit_env(n, C, 0.003, 0.06))
    scrape = unit(resonators(scrape_exc, [(190.0 * k * r, 1.0 / (1 + 0.3 * i)) for i, r in enumerate(PLATE[:9])], 30.0))
    whoom = unit(comb(air(rng, n, 0.0, 0.03, 0.2, 900.0 * k, 260.0 * k, 0.18, q=1.6), 150.0 * k, 0.7, 0.4))
    trail = core(n, A2 * k, CORE[:6], hit_env(n, C + 0.008, 0.03, 0.3), glide=lambda t: 1.0 - 0.06 * ramp(t, C, C + 0.5))
    body = mix(n, (contact(rng, n, C + 0.001, bright=3000.0 * k, knock=520.0 * k), 0.42),
               (shear, 0.42), (scrape, 0.4), (whoom, 0.45), (unit(trail), 0.26),
               (weight(n, C + 0.002, 110.0 * k, 58.0, fall=0.04, decay=0.09), 0.34))
    return fade_tail(space(body, rt=0.7, wet=0.3, damp=0.42), 0.2)


def warden_rush(rng, v):
    """Plated shoulder burst at 2 ms (no windup): three plate clanks as the armor drives forward, a low pressure
    whump with a pitched body, a resonant forward gust, a core surge rising four semitones and a short back-draft.
    Take 2 is a semitone up and shorter."""
    d, k = (0.76, 0.72)[v], (1.0, 1.06)[v]
    n, C = count(d), 0.002
    clatter = mix(n, *[(plate(rng, n, C + dt, b * k, decay=0.12, bright=0.85, edge=0.6), g)
                       for dt, b, g in ((0.001, 175.0, 0.5), (0.019, 205.0, 0.36), (0.043, 150.0, 0.28))])
    surge = core(n, E2 * k, CORE[:7], shape(n, [(0.0, 0.0), (0.02, 1.0), (0.25, 0.5), (0.62, 0.0)]),
                 glide=lambda t: 1.0 + 0.26 * ramp(t, 0.0, 0.15, 0.6))
    body = mix(n, (contact(rng, n, C, bright=1900.0 * k, knock=300.0 * k), 0.4), (clatter, 0.6),
               (air(rng, n, C, 0.004, 0.16, 420.0, 95.0, 0.06, q=0.9), 0.36),
               (weight(n, C, 84.0 * k, 54.0, fall=0.05, decay=0.14), 0.4),
               (air(rng, n, C + 0.01, 0.07, 0.22, 380.0 * k, 1300.0 * k, 0.15, q=2.0), 0.3),
               (unit(surge), 0.3),
               (air(rng, n, 0.18, 0.03, 0.12, 1600.0, 500.0, 0.1, q=1.8), 0.14),
               (hollow(rng, n, C, 132.0 * k, length=0.025, fb=0.8), 0.3))
    return fade_tail(space(body, rt=0.6, wet=0.26, damp=0.45), 0.18)


def warden_fists_launch(rng, v):
    """Two armor pieces unlatch 74 ms apart (left/right, a tone apart), each a bright latch tick, a small plate, a
    shell ping and a puff; the core tether lifts them on a rising glass fifth. Two mechanical releases, no shot."""
    n = count(0.44)
    parts = []
    for at, base, knock, cav in ((0.004, 300.0, 640.0, 196.0), (0.078, 337.0, 718.0, 220.0)):
        parts += [(contact(rng, n, at, bright=3200.0, knock=knock), 0.42),
                  (plate(rng, n, at, base, decay=0.09, bright=0.9, edge=0.9), 0.42),
                  (hollow(rng, n, at, cav, length=0.012, fb=0.75), 0.22),
                  (air(rng, n, at + 0.002, 0.004, 0.06, 2200.0, 700.0, 0.04, q=1.4), 0.16)]
    lift = core(n, A3, GLASS, shape(n, [(0.01, 0.0), (0.06, 1.0), (0.3, 0.5), (0.43, 0.0)]),
                glide=lambda t: 1.0 + 0.5 * ramp(t, 0.02, 0.3, 0.8), spread=0.005)
    body = mix(n, *parts, (unit(lift), 0.24))
    return fade_tail(space(body, rt=0.45, wet=0.22, damp=0.4), 0.1)


def warden_fists_impact(rng, v):
    """Focused fist contact at 3 ms: smaller and brighter than the slam with a short tail. Take 1 is centred on D3,
    take 2 on A3 (a fifth above), so the pair fired 0.48 s apart rings as one open fifth instead of mud."""
    d, root = (0.80, 0.78)[v], (D3, A3)[v]
    n, C = count(d), 0.003
    k = root / D3
    sparks = mul(resonators(noise(rng, n), [(root * h, 1.0 / h ** 0.5) for h in (4, 6, 8, 12)], 18.0),
                 hit_env(n, C, 0.002, 0.05))
    body = mix(n,
               (contact(rng, n, C, bright=2800.0, knock=440.0 * k ** 0.5), 0.6),
               (plate(rng, n, C, 236.0 * k ** 0.5, decay=0.17, bright=0.9, edge=0.8), 0.5),
               (hollow(rng, n, C, root, length=0.02, fb=0.8), 0.3),
               (weight(n, C, 120.0, 58.0, fall=0.035, decay=0.1), 0.42),
               (unit(core(n, root, CORE[:8], hit_env(n, C + 0.003, 0.006, 0.18))), 0.3),
               (unit(shimmer(n, C + 0.005, root * 6.0, 0.01, 0.16)), 0.12),
               (unit(sparks), 0.2))
    return fade_tail(space(body, rt=0.55, wet=0.26, damp=0.42), 0.2)


def warden_beam_charge(rng, v):
    """1.25 s coherent core gathering: core harmonics enter in three waves over a one-semitone rise, an
    accelerating throb (2.5 -> 10 Hz), two void suctions drawing inward and a pitched rising gust, then a 120 ms
    fall to silence at 1.14 s. No fake impact: the release is the separate warden_beam cue."""
    n = count(1.25)
    glide = lambda t: 1.0 + 0.06 * ramp(t, 0.0, 1.15, 1.8)
    gather = zeros(n)
    for parts, t0, g in ((CORE[:2], 0.0, 1.0), (CORE[2:5], 0.25, 0.8), (CORE[5:], 0.55, 0.7)):
        e = shape(n, [(t0, 0.0), (1.13, 1.0), (1.2, 0.0)])
        mix_into(gather, core(n, A2, parts, e, glide=glide, spread=0.003), g)
    throb = [0.78 + 0.22 * math.cos(TAU * (2.5 * t + 3.2 * t * t)) for t in (i / RATE for i in range(n))]
    gather = mul(gather, throb)
    low = core(n, A1, [(1, 0.6), (2, 1.0), (3, 0.4)], shape(n, [(0.0, 0.0), (0.9, 0.8), (1.13, 1.0), (1.2, 0.0)]),
               glide=glide)
    glass = core(n, 880.0, GLASS, shape(n, [(0.55, 0.0), (1.13, 1.0), (1.2, 0.0)]), glide=glide, spread=0.005)
    body = mix(n, (unit(gather), 0.55), (unit(low), 0.22), (unit(glass), 0.09),
               (air(rng, n, 0.05, 1.0, 0.07, 300.0, 1500.0, 0.8, q=3.0), 0.2),
               (suction(rng, n, 0.62, 0.45, A3, rt=0.5), 0.22), (suction(rng, n, 1.17, 0.6, 330.0, rt=0.6), 0.3))
    return gate(body, [(0.0, 1.0), (1.02, 1.0), (1.14, 0.0)])


def warden_beam(rng, v):
    """1.15 s: release front at 2 ms (pressure burst + knock), then a dense sustained core beam to 0.9 s - harmonic
    chord with chorus, violet fifth undertone, pitched tearing through 16 harmonic resonators, a resonant comb roar
    and a thin cyan glaze - dissipating through the room over the final 0.25 s."""
    n = count(1.15)
    sus = shape(n, [(0.0, 0.0), (0.006, 1.0), (0.07, 0.8), (0.5, 0.9), (0.9, 0.86), (1.0, 0.35), (1.1, 0.0)])
    flutter = [0.86 + 0.14 * math.sin(TAU * 7.3 * i / RATE) for i in range(n)]
    chord = core(n, A2, CORE, sus, spread=0.004)
    under = core(n, A1, [(1, 0.5), (1.5, 0.6), (2, 1.0), (3, 0.35)], sus, spread=0.005)
    shred = mul(tear(rng, n, 0.0, A2, q=24.0, top=16, rate=19.0, depth=0.5, tilt=0.84),
                mul(shape(n, [(0.0, 0.0), (0.004, 1.0), (0.9, 0.8), (1.05, 0.0)]), flutter))
    roar = mul(unit(comb(lowpass(noise(rng, n), 2500.0), A2, 0.93, 0.3)), sus)
    glaze = core(n, 1320.0, GLASS, mul(sus, smooth_random(rng, n, 5.0, 0.3, 0.7)), spread=0.006)
    front = mix(n, (air(rng, n, 0.002, 0.003, 0.09, 2600.0, 420.0, 0.06, q=0.9), 1.0),
                (contact(rng, n, 0.002, bright=2000.0, knock=330.0), 0.8))
    body = mix(n, (unit(chord), 0.42), (unit(under), 0.26), (shred, 0.4), (roar, 0.22), (unit(glaze), 0.07),
               (front, 0.34))
    body = space(body, rt=1.0, wet=0.28, damp=0.45)
    return fade_tail(gate(body, [(0.0, 1.0), (0.9, 1.0), (1.15, 0.0)]), 0.05)


def warden_core_open(rng, v):
    """Punish window: two soft unlatch taps, the shell exhales (a falling resonant breath) and the exposed core
    rings a clear, gentle fifth with a glass overtone. Quieter and softer-edged than any impact."""
    n = count(0.66)
    chime = core(n, A3, [(1, 1.0), (1.5, 0.55), (2, 0.5), (3, 0.3), (4, 0.18)], hit_env(n, 0.035, 0.02, 0.22),
                 spread=0.004)
    body = mix(n,
               (contact(rng, n, 0.004, bright=2600.0, knock=600.0), 0.22),
               (plate(rng, n, 0.004, 340.0, decay=0.05, bright=0.6, edge=0.5), 0.26),
               (plate(rng, n, 0.05, 380.0, decay=0.05, bright=0.6, edge=0.5), 0.2),
               (hollow(rng, n, 0.006, E3, length=0.02, fb=0.8), 0.18),
               (air(rng, n, 0.01, 0.02, 0.16, 1500.0, 420.0, 0.12, q=1.8), 0.28),
               (unit(chime), 0.42),
               (unit(shimmer(n, 0.05, 1320.0, 0.03, 0.18)), 0.12))
    return fade_tail(space(body, rt=0.6, wet=0.3, damp=0.42), 0.15)


def warden_strain(rng, v):
    """1.4 s phase transition: the armor strains (two creaking groans sagging in pitch, a tightening tremble and a
    rising core pressure), a plate gives at 1.10 s, everything is gated to near-silence 1.165-1.20 s (room tail
    included), then the core opens at 1.20 s in a harmonic bloom - a chord, not a melody."""
    n, B = count(1.4), 1.2
    load = shape(n, [(0.0, 0.2), (1.0, 1.0), (1.1, 1.0), (1.15, 0.0)])
    creak = mix(n, (groan(rng, n, 0.0, 1.13, 120.0, q=30.0, bend=0.06), 0.6),
                (groan(rng, n, 0.3, 1.13, 205.0, q=28.0, bend=0.08), 0.4))
    trem = [0.7 + 0.3 * math.sin(TAU * (6.0 * t + 3.5 * t * t)) for t in (i / RATE for i in range(n))]
    press = core(n, A2, CORE[:6], mul(load, trem), glide=lambda t: 1.0 + 0.12 * ramp(t, 0.0, 1.1, 1.5))
    low = core(n, A1, [(1, 0.5), (2, 1.0), (3, 0.5)], load)
    give = mix(n, (plate(rng, n, 1.10, 180.0, decay=0.12, bright=0.9, edge=0.6), 0.7),
               (hollow(rng, n, 1.10, 98.0, length=0.02, fb=0.8), 0.3))
    pre = mix(n, (mul(creak, load), 0.5), (unit(press), 0.3), (unit(low), 0.16), (give, 0.32))
    pre = gate(space(pre, rt=0.7, wet=0.25, damp=0.45), [(0.0, 1.0), (1.13, 1.0), (1.165, 0.0)])
    pull = suction(rng, n, B - 0.01, 0.35, A3, rt=0.5)
    bloom_env = hit_env(n, B, 0.025, 0.5)
    bloom = mix(n, (unit(core(n, A2, CORE, bloom_env, spread=0.005)), 0.5),
                (unit(core(n, E3, VIOLET[:4], bloom_env, spread=0.005)), 0.26),
                (unit(shimmer(n, B + 0.01, 880.0, 0.03, 0.4)), 0.12),
                (unit(shimmer(n, B + 0.02, 1320.0, 0.03, 0.35)), 0.08))
    bloom = space(bloom, rt=1.0, wet=0.35, damp=0.4)
    return fade_tail(mix(n, (pre, 1.0), (pull, 0.2), (bloom, 0.9)), 0.1)


def warden_step(rng, v):
    """Compact plated footfall at 3 ms: tick, small plate clank, short body, shell ping, two settling taps.
    High-passed at 70 Hz in mastering so walking never clutters the bass under attacks."""
    d, k = (0.32, 0.30)[v], (1.0, 1.08)[v]
    n, C = count(d), 0.003
    body = mix(n,
               (contact(rng, n, C, bright=2100.0 * k, knock=300.0 * k), 0.5),
               (plate(rng, n, C, 190.0 * k, decay=0.07, bright=0.7, edge=0.6), 0.45),
               (weight(n, C, 100.0, 62.0, fall=0.025, decay=0.05), 0.34),
               (hollow(rng, n, C, 120.0 * k, length=0.01, fb=0.7), 0.2),
               (unit(rattle(rng, n, C + 0.02, 2, 0.08, 260.0 * k, 1.0)), 0.2))
    return fade_tail(space(body, rt=0.35, wet=0.15, damp=0.5), 0.08)


# ---------------------------------------------------------------- cue table
# target  = laptop-weighted short-term loudness goal in dB (see loudness()); the peak ceiling may hold a cue below it
# gain    = recommended in-game playback gain (used by the audition and the overlap simulations)
# sync    = seconds into the file where the gameplay moment (contact / release / core opening) sits
# onset   = the file must reach 30 % of its peak within this many seconds (trigger-aligned cues)

def spec(fn, take, dur, target, gain, sync, trigger, purpose, onset=None, hp=38.0, sub_max=0.30):
    return {"fn": fn, "take": take, "dur": dur, "target": target, "gain": gain, "sync": sync, "trigger": trigger,
            "purpose": purpose, "onset": onset, "hp": hp, "sub_max": sub_max}


CUES = {
    "warden_windup": spec(warden_windup, 0, (0.86, 0.86), -18.0, 0.8, 0.0,
                          "slam telegraph start, ~0.90 s before the slam contact frame",
                          "low armor/pressure rise; readable at once; drops to silence, no impact at the end"),
    "warden_slam": spec(warden_slam, 0, (0.85, 1.15), -12.0, 1.0, 0.003, "slam fist contact frame",
                        "heavy plated fist contact with violet/cyan core flare", onset=0.010),
    "warden_slam_2": spec(warden_slam, 1, (0.85, 1.15), -12.0, 1.0, 0.003, "slam fist contact frame (alternate)",
                          "same slam identity, a semitone up, fifth-up glass", onset=0.010),
    "warden_sweep": spec(warden_sweep, 0, (0.70, 0.95), -14.0, 0.9, 0.002, "backhand release frame",
                         "heavy hollow backhand with a cyan shearing leading edge", onset=0.015),
    "warden_sweep_2": spec(warden_sweep, 1, (0.70, 0.95), -14.0, 0.9, 0.002, "backhand release frame (alternate)",
                           "same backhand, lower and shorter", onset=0.015),
    "warden_rush": spec(warden_rush, 0, (0.65, 0.85), -14.0, 0.9, 0.002, "rush launch frame",
                        "plated shoulder burst and pressure displacement, no windup", onset=0.015),
    "warden_rush_2": spec(warden_rush, 1, (0.65, 0.85), -14.0, 0.9, 0.002, "rush launch frame (alternate)",
                          "same rush, a semitone up and shorter", onset=0.015),
    "warden_fists_launch": spec(warden_fists_launch, 0, (0.36, 0.50), -16.0, 0.85, 0.004,
                                "fists release frame (fires once for both fists)",
                                "two resonant armor pieces unlatch and lift on the core tether", onset=0.015),
    "warden_fists_impact": spec(warden_fists_impact, 0, (0.70, 0.90), -14.0, 0.9, 0.003, "first fist lands",
                                "focused magical fist contact centred on D3", onset=0.015),
    "warden_fists_impact_2": spec(warden_fists_impact, 1, (0.70, 0.90), -14.0, 0.9, 0.003,
                                  "second fist lands, 0.48 s after the first",
                                  "focused magical fist contact centred on A3 (open fifth over take 1)", onset=0.015),
    "warden_beam_charge": spec(warden_beam_charge, 0, (1.25, 1.25), -17.0, 0.85, 0.0,
                               "beam charge start, exactly 1.25 s before the release frame",
                               "coherent core pressure gathering; falls silent before release, no fake impact"),
    "warden_beam": spec(warden_beam, 0, (1.15, 1.15), -13.0, 1.0, 0.002, "beam release frame",
                        "beam release at 0, sustained tearing core resonance to 0.9 s, 0.25 s dissipating tail",
                        onset=0.020),
    "warden_core_open": spec(warden_core_open, 0, (0.55, 0.75), -20.5, 0.8, 0.004,
                             "core exposed / punish window opens",
                             "short resonant armor release and gentle core chime, subtler than any impact"),
    "warden_strain": spec(warden_strain, 0, (1.40, 1.40), -15.0, 0.9, 1.2,
                          "phase I -> II transition start; the core opens 1.2 s in",
                          "armor strains, pre-bloom silence, core opens at 1.2 s in a harmonic bloom"),
    "warden_step": spec(warden_step, 0, (0.25, 0.40), -23.0, 0.55, 0.003, "footfall contact frame",
                        "compact heavy plated footfall, quiet, no bass clutter", onset=0.015, hp=70.0, sub_max=0.12),
    "warden_step_2": spec(warden_step, 1, (0.25, 0.40), -23.0, 0.55, 0.003, "footfall contact frame (alternate)",
                          "alternate footfall, slightly higher plates", onset=0.015, hp=70.0, sub_max=0.12),
}

# audition: (group, [(start_s, cue)]); cue gains are the CUES game gains, groups are separated by >= 0.6 s of silence
AUDITION_LENGTH = 20.9
AUDITION = [
    ("phase1_walk_and_slam", [(0.40, "warden_step"), (0.95, "warden_step_2"), (1.45, "warden_windup"),
                              (2.35, "warden_slam")]),
    ("phase1_slam_take2", [(4.10, "warden_windup"), (5.00, "warden_slam_2")]),
    ("phase1_sweeps", [(6.80, "warden_sweep"), (7.70, "warden_sweep_2")]),
    ("phase1_rushes", [(9.30, "warden_rush"), (10.10, "warden_rush_2")]),
    ("phase_transition", [(11.60, "warden_strain"), (13.05, "warden_core_open")]),
    ("phase2_fists", [(14.50, "warden_fists_launch"), (15.00, "warden_fists_impact"), (15.48, "warden_fists_impact_2")]),
    ("phase2_beam", [(17.10, "warden_beam_charge"), (18.35, "warden_beam"), (19.62, "warden_core_open")]),
]


def family(name):
    return name[:-2] if name.endswith("_2") else name


def seed(name):
    return f"guardian:{name}"


# ---------------------------------------------------------------- measurement shared by mastering and checks

def band_energy(x):
    """Energy fractions: sub (< 80 Hz), mid (250 Hz - 4 kHz, the band laptop speakers reproduce well)."""
    tot = sum(v * v for v in x)
    if tot <= 0.0:
        return {"sub": 0.0, "mid": 0.0}
    sub = sum(v * v for v in lowpass(x, 80.0, 4))
    mid = sum(v * v for v in lowpass(highpass(x, 250.0, 4), 4000.0, 4))
    return {"sub": round(sub / tot, 4), "mid": round(mid / tot, 4)}


def loudness(x, win=0.1, hop=0.005):
    """Laptop-weighted short-term loudness: 4th-order 150 Hz high-pass (small speakers), loudest 100 ms RMS, dB."""
    y = highpass(x, 150.0, 4)
    w, h = count(win), max(1, count(hop))
    cs = [0.0]
    acc = 0.0
    for v in y:
        acc += v * v
        cs.append(acc)
    if len(y) <= w:
        return db(math.sqrt(acc / max(1, len(y))))
    best = max(cs[i + w] - cs[i] for i in range(0, len(y) - w + 1, h))
    return db(math.sqrt(max(best, 0.0) / w))


def env_rms(x, a, b):
    return rms(window(x, a, b))


# ---------------------------------------------------------------- mastering and PCM

def dc_trim(x):
    """Remove residual DC with a full-length Hann-shaped correction, so the faded edges stay exactly zero."""
    n = len(x)
    if n < 3:
        return list(x)
    w = [0.5 - 0.5 * math.cos(TAU * i / (n - 1)) for i in range(n)]
    c = sum(x) / sum(w)
    return [v - c * wi for v, wi in zip(x, w)]


def master(x, s):
    """Per-cue master: 4th-order high-pass (DC/rumble), fixed laptop-translation EQ, a bounded corrective EQ (at most
    four 2-3 dB moves, all reported), edge tapers, DC trim, then one gain to the loudness target capped by the peak
    ceiling. No limiter, no saturation, no bit reduction."""
    x = highpass(x, s["hp"], 4)
    x = biquad(x, "lowshelf", 95.0, 0.7071, -2.5)
    x = biquad(x, "peak", 2400.0, 0.9, 1.5)
    x = lowpass(x, 9000.0, 2)
    eq = []
    for _ in range(4):
        be = band_energy(x)
        if be["sub"] > s["sub_max"]:
            x = biquad(x, "lowshelf", 110.0, 0.7071, -3.0)
            eq.append("lowshelf 110 Hz -3 dB")
        elif be["mid"] < 0.30:
            x = biquad(x, "peak", 1400.0, 0.7, 2.0)
            eq.append("peak 1.4 kHz +2 dB")
        else:
            break
    x = dc_trim(fade_tail(fade_in(x, 0.001), 0.012))
    loud, p = loudness(x), peak(x)
    want = 10 ** ((s["target"] - loud) / 20.0)
    cap = 0.97 * CUE_CEILING / p if p > 0 else want
    g = min(want, cap)
    return scale(x, g), {"translation_eq": eq, "master_gain_db": db(g), "peak_capped": cap < want}


def pcm(x):
    return array.array("h", (max(-32767, min(32767, int(round(v * 32767.0)))) for v in x))


def floats(a):
    return [v / 32767.0 for v in a]


def wav_bytes(samples):
    data = array.array("h", samples)
    if sys.byteorder == "big":
        data.byteswap()
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())
    return buf.getvalue()


def read_wav(path):
    """Returns (format dict, array('h')) or raises wave.Error / EOFError / OSError."""
    with wave.open(str(path), "rb") as w:
        fmt = {"channels": w.getnchannels(), "sampwidth": w.getsampwidth(), "rate": w.getframerate(),
               "comptype": w.getcomptype(), "frames": w.getnframes()}
        raw = w.readframes(w.getnframes())
    a = array.array("h")
    a.frombytes(raw[:len(raw) - len(raw) % 2])
    if sys.byteorder == "big":
        a.byteswap()
    return fmt, a


def stats(x):
    p, r = peak(x), rms(x)
    return {"samples": len(x), "duration_s": round(len(x) / RATE, 6), "peak": round(p, 5), "peak_dbfs": db(p),
            "rms_dbfs": db(r), "crest_db": round(db(p) - db(r), 2), "dc": round(sum(x) / len(x), 7) if x else 0.0,
            "loudness_db": loudness(x)}


# ---------------------------------------------------------------- build

def render_cue(name):
    s = CUES[name]
    raw = s["fn"](random.Random(seed(name)), s["take"])
    mastered, info = master(raw, s)
    return pcm(mastered), info


def mix_events(events, cue_pcm, length, ceiling=MIX_CEILING):
    """Sum cues at their game gains (times in seconds). Only ever scaled *down*, and only if the sum would pass the
    ceiling, so the audition reflects real in-game gain. Returns (floats, scale)."""
    out = zeros(count(length))
    for at, name in events:
        mix_into(out, floats(cue_pcm[name]), CUES[name]["gain"], count(at))
    p = peak(out)
    sc = min(1.0, 0.97 * ceiling / p) if p > 0 else 1.0
    return (scale(out, sc) if sc < 1.0 else out), sc


def audition_events():
    return [(at, name) for _, evs in AUDITION for at, name in evs]


def build():
    cue_pcm, info = {}, {}
    for name in CUES:
        cue_pcm[name], info[name] = render_cue(name)
    aud, aud_scale = mix_events(audition_events(), cue_pcm, AUDITION_LENGTH)
    aud = fade_tail(fade_in(aud, 0.005), 0.05)
    aud_pcm = pcm(aud)
    files = {f"{name}.wav": wav_bytes(a) for name, a in cue_pcm.items()}
    files["audition.wav"] = wav_bytes(aud_pcm)
    man = {"generator": "generate_guardian_sfx.py", "version": VERSION,
           "format": {"container": "RIFF WAVE", "encoding": "PCM signed 16-bit little-endian", "channels": 1,
                      "sample_rate": RATE},
           "cue_ceiling": CUE_CEILING, "mix_ceiling": MIX_CEILING,
           "loudness_measure": "laptop-weighted short-term: 4th-order 150 Hz high-pass, loudest 100 ms RMS (dB)",
           "cues": {}, "audition": {}}
    for name, s in CUES.items():
        x = floats(cue_pcm[name])
        man["cues"][name] = {
            "file": f"{name}.wav", "family": family(name), "take": s["take"], "purpose": s["purpose"],
            "trigger": s["trigger"], "sync_s": s["sync"], "game_gain": s["gain"], "seed": seed(name),
            "function": s["fn"].__name__, "layers": " ".join(s["fn"].__doc__.split()),
            "duration_rule_s": list(s["dur"]), "loudness_target_db": s["target"], "highpass_hz": s["hp"],
            "sha256": hashlib.sha256(files[f"{name}.wav"]).hexdigest(), **info[name], **stats(x), **band_energy(x)}
    ax = floats(aud_pcm)
    man["audition"] = {
        "file": "audition.wav", "length_s": AUDITION_LENGTH, "mix_scale": round(aud_scale, 6),
        "groups": [{"group": g, "start_s": evs[0][0], "end_s": round(max(at + len(cue_pcm[c]) / RATE for at, c in evs), 3),
                    "events": [{"time_s": at, "cue": c, "gain": CUES[c]["gain"]} for at, c in evs]}
                   for g, evs in AUDITION],
        "sha256": hashlib.sha256(files["audition.wav"]).hexdigest(), **stats(ax)}
    return files, json.dumps(man, indent=2, sort_keys=True) + "\n", cue_pcm


# ---------------------------------------------------------------- checks (all run on the decoded files on disk)

EXPECTED = [f"{name}.wav" for name in CUES] + ["audition.wav"]
SIDE_FILES = {"manifest.json", "check_report.json"}


class Report:
    def __init__(self):
        self.items = []

    def add(self, check, target, ok, measured=None, limit=None):
        self.items.append({"check": check, "target": target, "passed": bool(ok), "measured": measured, "limit": limit})
        return bool(ok)

    def failed(self):
        return [i for i in self.items if not i["passed"]]

    def to_json(self, mode):
        bad = self.failed()
        return json.dumps({"generator": "generate_guardian_sfx.py", "version": VERSION, "mode": mode,
                           "passed": not bad, "total": len(self.items), "failed": len(bad),
                           "failures": [f"{i['check']} [{i['target']}]" for i in bad], "checks": self.items},
                          indent=2, sort_keys=True) + "\n"


def bandpass(x, lo, hi):
    return lowpass(highpass(x, lo, 4), hi, 4)


def energy(x):
    return sum(v * v for v in x)


def features(x):
    """Coarse identity fingerprint: five spectral band shares, four temporal quarter shares, duration / 1.4 s."""
    bands = [energy(lowpass(x, 150.0, 4)), energy(bandpass(x, 150.0, 500.0)), energy(bandpass(x, 500.0, 1500.0)),
             energy(bandpass(x, 1500.0, 4000.0)), energy(highpass(x, 4000.0, 4))]
    tb = sum(bands) or 1e-18
    q = max(1, len(x) // 4)
    quarters = [energy(x[i * q:(i + 1) * q]) for i in range(4)]
    tq = sum(quarters) or 1e-18
    return [b / tb for b in bands] + [v / tq for v in quarters] + [len(x) / RATE / 1.4]


def distance(a, b):
    return math.sqrt(sum((u - v) ** 2 for u, v in zip(a, b)))


def short_rms_max(x, a, b, win=0.02):
    """Loudest 20 ms RMS inside [a, b) seconds."""
    seg = window(x, a, b)
    w = count(win)
    if len(seg) <= w:
        return rms(seg)
    return max(rms(seg[i:i + w]) for i in range(0, len(seg) - w + 1, max(1, w // 2)))


def onset_time(x, frac=0.3):
    p = peak(x)
    for i, v in enumerate(x):
        if abs(v) >= frac * p:
            return i / RATE
    return len(x) / RATE


def ratio(a, b, nd=4):
    return round(a / b, nd) if b > 0 else None


def run_checks(out, files, manifest, cue_pcm, full=True):
    """File-level checks always; with full=True also in-process determinism and every audio assertion."""
    rep = Report()
    present = sorted(p.name for p in out.iterdir() if p.is_file()) if out.is_dir() else []
    missing = [f for f in EXPECTED if f not in present]
    stray = [f for f in present if f not in EXPECTED and f not in SIDE_FILES]
    rep.add("files.exact_cue_set", str(out), not missing and not stray and len(CUES) == 16,
            {"missing": missing, "unexpected": stray, "cue_count": len(CUES)},
            "exactly the 16 cues + audition.wav (+ manifest.json, check_report.json)")
    disk = {}
    for f in EXPECTED:
        p = out / f
        if not p.is_file():
            continue
        data = p.read_bytes()
        rep.add("determinism.matches_fresh_render", f, data == files[f], hashlib.sha256(data).hexdigest(),
                hashlib.sha256(files[f]).hexdigest())
        try:
            fmt, a = read_wav(p)
        except (wave.Error, EOFError, OSError) as e:
            rep.add("format.readable_pcm_wav", f, False, str(e), "RIFF WAVE PCM")
            continue
        got = {k: fmt[k] for k in ("channels", "sampwidth", "rate", "comptype")}
        want = {"channels": 1, "sampwidth": 2, "rate": RATE, "comptype": "NONE"}
        ok = got == want and len(a) == fmt["frames"]
        rep.add("format.pcm16_mono_22050", f, ok, {**got, "frames": fmt["frames"], "decoded": len(a)}, want)
        if ok:
            disk[f[:-4]] = a
    m = out / "manifest.json"
    rep.add("determinism.manifest", "manifest.json", m.is_file() and m.read_text() == manifest,
            "present" if m.is_file() else "missing", "byte-identical to a fresh render")
    if full:
        for name in ("warden_slam", "warden_beam"):
            again, _ = render_cue(name)
            rep.add("determinism.in_process_rerender", name, again == cue_pcm[name], None, "identical samples")
        audio_checks(rep, disk)
    return rep


def audio_checks(rep, disk):
    x = {name: floats(a) for name, a in disk.items()}
    st = {}
    for name, s in CUES.items():
        if name not in disk:
            rep.add("audio.available", name, False, "missing or unreadable", "decoded PCM")
            continue
        a, v = disk[name], x[name]
        lo, hi = s["dur"]
        exact = lo == hi
        ok = len(a) == count(lo) if exact else count(lo) <= len(a) <= count(hi)
        rep.add("timing.duration", name, ok, round(len(a) / RATE, 6), f"exactly {lo} s" if exact else f"{lo}-{hi} s")
        sts, be = stats(v), band_energy(v)
        st[name] = {**sts, **be}
        rep.add("level.non_silent", name, sts["rms_dbfs"] >= -42.0 and sts["loudness_db"] >= -40.0,
                {"rms_dbfs": sts["rms_dbfs"], "loudness_db": sts["loudness_db"]}, "rms >= -42 dBFS, loudness >= -40 dB")
        rep.add("level.dc_bounded", name, abs(sts["dc"]) <= 0.002, sts["dc"], "|mean| <= 0.002 FS")
        full_scale = sum(1 for q in a if q >= 32767 or q <= -32767)
        rep.add("level.no_clipping", name, sts["peak"] <= CUE_CEILING and full_scale == 0,
                {"peak": sts["peak"], "full_scale_samples": full_scale}, f"peak <= {CUE_CEILING}, no full-scale samples")
        tail5, head = peak(v[-count(0.005):]), peak(v[:count(0.0005)])
        rep.add("edges.tapered", name,
                abs(a[0]) <= EDGE_LSB and abs(a[-1]) <= EDGE_LSB and tail5 <= 0.025 and head <= 0.05,
                {"first_lsb": a[0], "last_lsb": a[-1], "last_5ms_peak": round(tail5, 5), "first_0.5ms_peak": round(head, 5)},
                f"first/last sample within {EDGE_LSB} LSB, last 5 ms <= 0.025, first 0.5 ms <= 0.05")
        mid_min, sub_max = (0.30, 0.15) if family(name) == "warden_step" else (0.22, 0.40)
        rep.add("spectrum.audible_midrange", name, be["mid"] >= mid_min and be["sub"] <= sub_max, be,
                f"energy share mid(250 Hz-4 kHz) >= {mid_min}, sub(< 80 Hz) <= {sub_max}")
        rep.add("level.peak_rms_sensible", name,
                5.0 <= sts["crest_db"] <= 30.0 and s["target"] - 12.0 <= sts["loudness_db"] <= s["target"] + 1.0,
                {"crest_db": sts["crest_db"], "loudness_db": sts["loudness_db"], "peak_dbfs": sts["peak_dbfs"]},
                f"crest 5-30 dB and loudness in [{s['target'] - 12.0}, {s['target'] + 1.0}] dB")
        if s["onset"] is not None:
            t = onset_time(v)
            rep.add("timing.onset_at_trigger", name, t <= s["onset"], round(t, 5), f"30 % of peak within {s['onset']} s")
    semantic_checks(rep, x, st)
    mix_checks(rep, disk, x)


def semantic_checks(rep, x, st):
    for name in ("warden_windup", "warden_beam_charge"):
        if name in x:
            v = x[name]
            d = len(v) / RATE
            body, end = short_rms_max(v, 0.0, d - 0.15), short_rms_max(v, d - 0.15, d)
            last = peak(window(v, d - 0.06, d))
            rep.add("shape.no_impact_at_end", name, end <= 0.5 * body and last <= 0.25 * peak(v),
                    {"end_vs_body": ratio(end, body), "last_60ms_vs_peak": ratio(last, peak(v))},
                    "loudest 20 ms in the last 150 ms <= 0.5 x before; last 60 ms peak <= 0.25 x peak")
    if "warden_windup" in x:
        v = x["warden_windup"]
        rep.add("shape.readable_at_once", "warden_windup", peak(window(v, 0.0, 0.12)) >= 0.15 * peak(v),
                ratio(peak(window(v, 0.0, 0.12)), peak(v)), "first 120 ms peak >= 0.15 x peak")
    if "warden_beam_charge" in x:
        v = x["warden_beam_charge"]
        a, b = env_rms(v, 0.05, 0.35), env_rms(v, 0.8, 1.1)
        rep.add("shape.charge_gathers", "warden_beam_charge", b >= 1.5 * a, ratio(b, a), "rms 0.8-1.1 s >= 1.5 x 0.05-0.35 s")
    if "warden_beam" in x:
        v = x["warden_beam"]
        top, front = short_rms_max(v, 0.0, 1.15), short_rms_max(v, 0.0, 0.05)
        wins = [env_rms(v, 0.1 + 0.08 * i, 0.15 + 0.08 * i) for i in range(10)]
        tail, body = env_rms(v, 1.08, 1.15), env_rms(v, 0.4, 0.8)
        rep.add("shape.beam_release_at_zero", "warden_beam", front >= 0.35 * top, ratio(front, top),
                "loudest 20 ms in the first 50 ms >= 0.35 x loudest overall")
        rep.add("shape.beam_sustained_to_0.9s", "warden_beam", min(wins) >= 0.3 * max(wins), ratio(min(wins), max(wins)),
                "ten 50 ms windows over 0.10-0.87 s within 10.5 dB of each other")
        rep.add("shape.beam_dissipating_tail", "warden_beam", tail <= 0.1 * body, ratio(tail, body, 5),
                "rms 1.08-1.15 s at least 20 dB under 0.4-0.8 s")
    if "warden_strain" in x:
        v = x["warden_strain"]
        dip, bloom, pre = env_rms(v, 1.17, 1.195), env_rms(v, 1.21, 1.3), env_rms(v, 0.9, 1.1)
        rep.add("shape.strain_silence_then_bloom", "warden_strain", dip <= 0.25 * bloom and bloom >= 0.4 * pre,
                {"dip_vs_bloom": ratio(dip, bloom), "bloom_vs_strain": ratio(bloom, pre)},
                "1.17-1.195 s at least 12 dB under the 1.21-1.30 s bloom; bloom >= 0.4 x the strain")
    if "warden_fists_launch" in x:
        v = x["warden_fists_launch"]
        p1, p2 = peak(window(v, 0.0, 0.03)), peak(window(v, 0.076, 0.1))
        gap, burst = env_rms(v, 0.06, 0.075), env_rms(v, 0.079, 0.095)
        rep.add("shape.two_detach_events", "warden_fists_launch", p2 >= 0.45 * p1 and burst >= 1.25 * gap,
                {"second_vs_first_peak": ratio(p2, p1), "second_burst_vs_gap": ratio(burst, gap)},
                "second release peak >= 0.45 x first and >= 1.25 x the gap before it")
    loud = {n: st[n]["loudness_db"] for n in st}
    for quiet, big, margin in (("warden_step", "warden_slam", 5.0), ("warden_step_2", "warden_slam", 5.0),
                               ("warden_core_open", "warden_fists_impact", 2.0), ("warden_core_open", "warden_slam", 4.0)):
        if quiet in loud and big in loud:
            rep.add("level.relative_loudness", f"{quiet} < {big}", loud[quiet] <= loud[big] - margin,
                    {"quiet_db": loud[quiet], "loud_db": loud[big]}, f"at least {margin} dB quieter")
    for name in CUES:
        if name.endswith("_2") and name in x and family(name) in x:
            a, b = x[family(name)], x[name]
            diff = max(abs(u - w) for u, w in zip(a, b))
            dl = abs(len(a) - len(b)) / RATE
            rep.add("identity.variation_differs", name, diff > 0.05 and dl <= 0.1,
                    {"max_abs_diff": round(diff, 4), "length_delta_s": round(dl, 4)}, "not a copy; length within 0.1 s")
    feats = {n: features(x[n]) for n in CUES if n in x}
    names = sorted(feats)
    worst = None
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if family(a) != family(b):
                dd = distance(feats[a], feats[b])
                if worst is None or dd < worst[0]:
                    worst = (dd, a, b)
    if worst is not None:
        rep.add("identity.actions_distinct", "every cross-action pair", worst[0] >= 0.05,
                {"closest_pair": [worst[1], worst[2]], "distance": round(worst[0], 4)},
                "spectral/temporal fingerprint distance >= 0.05")


def mix_checks(rep, disk, x):
    def sim(events, length):
        if not all(nm in disk for _, nm in events):
            return None
        return mix_events(events, disk, length, ceiling=1e9)[0]      # raw in-game sum, never rescaled

    two = sim([(0.0, "warden_fists_impact"), (0.48, "warden_fists_impact_2")], 1.3)
    if two is not None:
        single = max(CUES["warden_fists_impact"]["gain"] * peak(x["warden_fists_impact"]),
                     CUES["warden_fists_impact_2"]["gain"] * peak(x["warden_fists_impact_2"]))
        p = peak(two)
        before, after = env_rms(two, 0.43, 0.477), env_rms(two, 0.483, 0.53)
        rep.add("mix.two_fist_overlap_gain_safe", "impact + impact_2 at +0.48 s", p <= MIX_CEILING and p <= 1.3 * single,
                {"peak": round(p, 4), "vs_loudest_single": ratio(p, single, 3)},
                f"peak <= {MIX_CEILING} and <= 1.3 x the louder single fist")
        rep.add("mix.two_fist_second_contact_reads", "impact + impact_2 at +0.48 s", after >= 2.0 * before,
                ratio(after, before, 3), "second contact >= +6 dB over the first fist's tail")
    beam = sim([(0.0, "warden_beam_charge"), (1.25, "warden_beam"), (2.40, "warden_core_open")], 3.2)
    if beam is not None:
        pre, post = env_rms(beam, 1.215, 1.248), env_rms(beam, 1.252, 1.30)
        rep.add("mix.beam_sequence_gain_safe", "charge -> beam at 1.25 s -> core_open", peak(beam) <= MIX_CEILING,
                round(peak(beam), 4), f"peak <= {MIX_CEILING}")
        rep.add("mix.beam_release_contrast", "charge -> beam at 1.25 s", post >= 4.0 * pre, ratio(post, pre, 3),
                "release >= +12 dB over the pre-release silence")
    phase = sim([(0.0, "warden_strain"), (1.2, "warden_core_open")], 2.0)
    if phase is not None:
        rep.add("mix.phase_transition_gain_safe", "strain + core_open at the 1.2 s bloom", peak(phase) <= MIX_CEILING,
                round(peak(phase), 4), f"peak <= {MIX_CEILING}")
    if "audition" not in disk:
        return
    a, v = disk["audition"], x["audition"]
    full_scale = sum(1 for q in a if q >= 32767 or q <= -32767)
    rep.add("audition.length", "audition.wav", len(a) == count(AUDITION_LENGTH), round(len(a) / RATE, 4),
            f"{AUDITION_LENGTH} s")
    rep.add("audition.no_clipping", "audition.wav", peak(v) <= MIX_CEILING and full_scale == 0,
            {"peak": round(peak(v), 5), "full_scale_samples": full_scale}, f"peak <= {MIX_CEILING}")
    rep.add("audition.edges", "audition.wav", abs(a[0]) <= EDGE_LSB and abs(a[-1]) <= EDGE_LSB and
            peak(window(v, 0.0, 0.35)) <= 0.002, {"first_lsb": a[0], "last_lsb": a[-1]}, "silent lead-in, zero edges")
    if all(c in disk for _, evs in AUDITION for _, c in evs):
        sc = mix_events(audition_events(), disk, AUDITION_LENGTH)[1]
        rep.add("audition.realistic_gain", "audition.wav", sc >= 0.9, round(sc, 4),
                "cues summed at game gains; at most a 0.9 dB safety trim")
        spans = []
        for g, evs in AUDITION:
            start, end = evs[0][0], max(at + len(disk[c]) / RATE for at, c in evs)
            rep.add("audition.group_audible", g, db(env_rms(v, start, end)) >= -40.0, db(env_rms(v, start, end)),
                    ">= -40 dBFS rms")
            spans.append((g, start, end))
        for (g1, _, e1), (g2, s2, _) in zip(spans, spans[1:]):
            gap = s2 - e1
            quiet = peak(window(v, e1 + 0.02, s2 - 0.02)) if gap > 0.05 else 1.0
            rep.add("audition.clear_gap", f"{g1} -> {g2}", gap >= 0.6 and quiet <= 0.003,
                    {"gap_s": round(gap, 3), "gap_peak": round(quiet, 5)}, ">= 0.6 s of silence (peak <= 0.003)")


# ---------------------------------------------------------------- write, selftest, main

def write(out, files, manifest):
    out.mkdir(parents=True, exist_ok=True)
    for rel, data in files.items():
        (out / rel).write_bytes(data)
    (out / "manifest.json").write_text(manifest)


def selftest(files, manifest, cue_pcm):
    """A clean render must pass everything; tampered, truncated, missing, stray, edited and clipped output must fail."""
    lab = Path(tempfile.mkdtemp(prefix="b2bj-guardian-sfx-"))
    results = []
    try:
        write(lab, files, manifest)
        results.append(("clean render passes every check", not run_checks(lab, files, manifest, cue_pcm).failed()))
        p = lab / "warden_slam.wav"
        good = p.read_bytes()
        bent = bytearray(good)
        mid = 44 + 2 * count(0.3)
        bent[mid:mid + 400] = bytes(400)
        p.write_bytes(bytes(bent))
        results.append(("altered cue fails", bool(run_checks(lab, files, manifest, cue_pcm, full=False).failed())))
        p.write_bytes(good[:-2000])
        results.append(("truncated cue fails", bool(run_checks(lab, files, manifest, cue_pcm, full=False).failed())))
        p.write_bytes(good)
        (lab / "warden_beam.wav").unlink()
        results.append(("missing cue fails", bool(run_checks(lab, files, manifest, cue_pcm, full=False).failed())))
        write(lab, files, manifest)
        (lab / "warden_extra.wav").write_bytes(files["warden_step.wav"])
        results.append(("stray file fails", bool(run_checks(lab, files, manifest, cue_pcm, full=False).failed())))
        (lab / "warden_extra.wav").unlink()
        (lab / "manifest.json").write_text(manifest.replace('"version"', '"versi0n"', 1))
        results.append(("edited manifest fails", bool(run_checks(lab, files, manifest, cue_pcm, full=False).failed())))
        write(lab, files, manifest)
        hot = array.array("h", cue_pcm["warden_step"])
        hot[count(0.1)] = 32767
        (lab / "warden_step.wav").write_bytes(wav_bytes(hot))
        caught = [i["check"] for i in run_checks(lab, files, manifest, cue_pcm).failed()
                  if i["target"] == "warden_step"]
        results.append(("clipped sample caught by the audio assertions", "level.no_clipping" in caught))
    finally:
        shutil.rmtree(lab, ignore_errors=True)
    for label, ok in results:
        print(f"selftest {'ok  ' if ok else 'FAIL'} {label}")
    return all(ok for _, ok in results)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--out", type=Path, default=Path(__file__).resolve().parent / "output")
    ap.add_argument("--check", action="store_true", help="verify existing output against a fresh in-memory render")
    ap.add_argument("--selftest", action="store_true", help="prove the check rejects bad output (temp dir only)")
    args = ap.parse_args()
    files, manifest, cue_pcm = build()
    if args.selftest:
        sys.exit(0 if selftest(files, manifest, cue_pcm) else 1)
    if not args.check:
        write(args.out, files, manifest)
    rep = run_checks(args.out, files, manifest, cue_pcm)
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "check_report.json").write_text(rep.to_json("check" if args.check else "generate"))
    man = json.loads(manifest)
    for name, c in man["cues"].items():
        print(f"{name:24s} {c['duration_s']:.3f}s  peak {c['peak_dbfs']:6.2f} dBFS  loud {c['loudness_db']:6.2f} dB"
              f"  crest {c['crest_db']:5.2f}  mid {c['mid']:.2f}  sub {c['sub']:.2f}  eq {len(c['translation_eq'])}")
    a = man["audition"]
    print(f"{'audition':24s} {a['duration_s']:.3f}s  peak {a['peak_dbfs']:6.2f} dBFS  mix scale {a['mix_scale']}")
    bad = rep.failed()
    print(f"{len(rep.items)} checks, {len(bad)} failed -> {args.out / 'check_report.json'}")
    for i in bad:
        print(f"  FAIL {i['check']} [{i['target']}] measured={i['measured']} limit={i['limit']}")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
