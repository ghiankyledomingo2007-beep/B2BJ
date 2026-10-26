#!/usr/bin/env python3
"""The player's combat sounds and the hits they land, synthesized from scratch: no samples, services or keys.

    python3 tools/audio/generate_combat_sfx.py           # write the 35 takes below into assets/audio
    python3 tools/audio/generate_combat_sfx.py --check   # re-render in memory, exit 1 if a file differs

Slime     blob_attack (3)   a rounded liquid snap, then a short jet
          slime_dash        bubbly water displaced in a rush
          water_splash      a slap and plop, droplets scattering after it
          slime_hurt        a rubbery squash that wobbles back
Hits      enemy_hit (3)     a dry knock with a low body under it
Blade     slash (3)         light arc of air peaking on the contact frame
          thrust            narrow, rising, stopped dead
          slash_heavy       wide low swing, a chop and an ice crack where it lands
          cleave            the deepest swing, a body blow, the air dragged after it
          dash_slash        a traveling cut: the dash rush, then a pass-by falling in pitch
          spin_cleave       two sweeps with a rotating flutter
Water     tide_release      rushing water building up
          tide_impact       the wave breaking into a broad low splash
          finisher_release  pressure squeezing in, then a bolt of water leaving
          finisher_impact   a crisp heavy water impact
Warden    guardian_slam     stone cracking, a low body, a short spill of earth and pebbles

Blade cues start SWING_LEAD (0.08 s) before the blade lands (B2BJ.handleEvents), so every swing peaks
there. Each take already holds the thud, crack or boom GameAudio used to mix in from a separate family,
and the slime's magic is a soft bubble ring inside the take instead of the old synthesized chirp.

Reuses generate_forest_sfx's building blocks and finish(): 22050 Hz mono PCM16, high-passed (no DC),
peak-normalised below full scale, faded edges. Seeded, so a rerun writes identical bytes. Only the
files named in FAMILIES are written; earlier takes are kept in assets/source/audio/.
"""
import math
import random
import sys
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_forest_sfx import (OUT, RATE, bands, biquad, bump, count, env, finish, grains, mix, mul,  # noqa: E402
                                 noise, norm, stick_slip, swell, thump, wav_bytes)


# ---------------------------------------------------------------- water, air and ice

def bubble(out, at, f0, decay, amp=1.0, rise=0.1):
    """One bubble ringing as it forms or bursts (Minnaert): a sine rising in pitch, gone within a few decays."""
    start, phase = count(at), 0.0
    for i in range(min(len(out) - start, count(decay * 6))):
        t = i / RATE
        phase += 2 * math.pi * f0 * (1 + rise * t / decay) / RATE
        out[start + i] += amp * math.sin(phase) * math.exp(-t / decay) * min(1.0, i / 8)
    return out


def bubbles(rng, seconds, rate, low, high, ring=1.0):
    """A random cloud of bubbles, rate(t) per second, sizes between low and high Hz. Big ones are louder
    and ring longer; ring scales every decay (van den Doel's damping for a free bubble)."""
    out = [0.0] * count(seconds)
    for i in range(len(out)):
        if rng.random() < rate(i / RATE) / RATE:
            f0 = low * (high / low) ** rng.random()
            decay = ring / (0.13 * f0 + 0.0072 * f0 ** 1.5)
            bubble(out, i / RATE, f0, decay, (0.4 + 0.6 * rng.random()) * (low / f0) ** 0.5, 0.1 + 0.2 * rng.random())
    return out


def churn(rng, x, rate=40, depth=0.35):
    """Turbulence: the level of moving water wobbles at random."""
    wobble = norm(biquad(noise(rng, len(x) / RATE), "low", rate))
    return [v * (1 - depth + depth * w) for v, w in zip(x, wobble)]


def follow(shape):
    """A per-sample envelope as a function of time, for rates that track a level."""
    return lambda t: shape[min(len(shape) - 1, count(t))]


def whoosh(rng, seconds, peak, rise, fall, low, high, q=0.9, tear=0.35):
    """A blade through air: band-passed noise whose centre and level follow the swing's speed, fastest at
    peak. A narrower band an octave up is the edge tearing the air."""
    speed = lambda t: math.exp(-((t - peak) / (rise if t < peak else fall)) ** 2)
    centre = lambda t: low + (high - low) * speed(t)
    air = biquad(noise(rng, seconds), "band", centre, q)
    edge = biquad(noise(rng, seconds), "band", lambda t: centre(t) * 2.2, q * 1.6)
    return [(a + tear * e) * speed(i / RATE) ** 1.4 for i, (a, e) in enumerate(zip(air, edge))]


def glint(n, at, partials):
    """The cold edge of the ichor blade: a few inharmonic partials [(Hz, decay s, gain)], short and quiet."""
    out = [0.0] * n
    for f, decay, gain in partials:
        for i in range(count(at), min(n, count(at + decay * 6))):
            t = i / RATE - at
            out[i] += gain * math.sin(2 * math.pi * f * t) * math.exp(-t / decay) * min(1.0, t / 0.002)
    return out


def crack(rng, seconds, at, modes, events, rate=1400, gap=0.012):
    """Splintering ice or stone: short dense bursts of friction through brittle resonances."""
    splinters, amp = [0.0] * count(seconds), 1.0
    for _ in range(events):
        burst = stick_slip(rng, seconds, lambda _: rate, at, at + 0.006, 0.5, amp)
        splinters = [a + b for a, b in zip(splinters, burst)]
        at += gap * (0.6 + 0.8 * rng.random())
        amp *= 0.72
    return bands(splinters, modes)


def knock(n, at, modes):
    """A struck body: one impulse through its resonances [(Hz, Q, gain)]."""
    hit = [0.0] * n
    hit[count(at)] = 1.0
    return bands(hit, modes)


# ---------------------------------------------------------------- slime form; v is the take

def blob_attack(rng, v):
    d = (0.24, 0.22, 0.25)[v]
    n = count(d)
    pop = bubble([0.0] * n, 0.004, (430, 380, 480)[v], 0.03, rise=0.5)          # the goo letting go
    squelch = mul(biquad(noise(rng, d), "band", (650, 560, 720)[v], 1.4), env(n, 0.002, 0.018))
    high, low = ((1500, 850), (1300, 800), (1650, 900))[v]
    shape = env(n, 0.03, 0.06, hold=0.03, start=0.012)
    jet = churn(rng, mul(biquad(biquad(noise(rng, d), "band", lambda t: low + (high - low) * math.exp(-t / 0.07), 0.9),
                                "low", 3800), shape))
    fizz = bubbles(rng, d, lambda t: 260 * follow(shape)(t), 600, 1600, 1.4)
    ring = bubble([0.0] * n, 0.02, (620, 560, 680)[v], 0.045, rise=0.35)     # the slime's soft magic
    return mix(n, (pop, 1.0), (squelch, 0.55), (jet, 0.8), (fizz, 0.35), (ring, 0.22))


def slime_dash(rng, v):
    d = (0.26, 0.24)[v]
    n = count(d)
    shape = swell(n, (0.07, 0.06)[v], 0.06, 1.3)
    centre = lambda t: (320, 360)[v] + 650 * math.sin(math.pi * min(1.0, t / (d * 0.85)))
    rush = churn(rng, mul(biquad(biquad(noise(rng, d), "band", centre, 0.7), "low", 2400), shape))
    mass = mul(biquad(noise(rng, d), "low", 180), shape)                       # the water pushed aside
    fizz = bubbles(rng, d, lambda t: 500 * follow(shape)(t), 300, 1100, 1.2)
    gulp = bubble([0.0] * n, 0.0, (240, 270)[v], 0.03, rise=0.4)
    return mix(n, (rush, 1.0), (mass, 0.55), (fizz, 0.45), (gulp, 0.4))


def water_splash(rng, v):
    d = (0.28, 0.26)[v]
    n = count(d)
    slap = mul(bands(noise(rng, d), [(500, 0.8, 1.0), (1400, 0.9, 0.7), (2600, 1.2, 0.3)]), env(n, 0.002, 0.035))
    plop = bubble([0.0] * n, 0.006, (280, 320)[v], 0.035, rise=0.45)
    drops = bubbles(rng, d, lambda t: 0 if t < 0.02 else 380 * math.exp(-(t - 0.02) / 0.07), 1100, 2600, 2.0)
    spray = mul(biquad(grains(rng, d, lambda t: 900 * math.exp(-t / 0.05), 0.002), "band", 2200, 0.9), env(n, 0.004, 0.06))
    ring = bubble([0.0] * n, 0.03, (560, 610)[v], 0.05, rise=0.3)
    return mix(n, (slap, 1.0), (plop, 0.7), (drops, 0.55), (spray, 0.35), (ring, 0.2))


def slime_hurt(rng, v):
    d = (0.20, 0.19)[v]
    n = count(d)
    body, phase = [], 0.0                       # squashed jelly: a low ring falling as it is pressed flat,
    for i in range(n):                          # wobbling as it springs back
        t = i / RATE
        phase += 2 * math.pi * (210 + (260, 230)[v] * math.exp(-t / 0.035)) / RATE
        wobble = 1 + 0.5 * math.exp(-t / 0.06) * math.sin(2 * math.pi * (19, 23)[v] * t)
        body.append(math.tanh(1.3 * math.sin(phase)) * wobble * min(1.0, t / 0.003) * math.exp(-t / 0.05))
    body = biquad(body, "low", 1400)
    slap = mul(biquad(noise(rng, d), "band", (950, 850)[v], 1.0), env(n, 0.001, 0.014))
    squish = mul(biquad(noise(rng, d), "band", lambda t: 600 - 300 * min(1.0, t / 0.08), 2.0), env(n, 0.004, 0.04, start=0.01))
    blips = bubbles(rng, d, lambda t: 120 if 0.02 < t < 0.12 else 0, 350, 900, 1.5)
    return mix(n, (body, 1.0), (slap, 0.75), (squish, 0.55), (blips, 0.3))


# ---------------------------------------------------------------- hits

def enemy_hit(rng, v):
    d = (0.22, 0.20, 0.24)[v]
    n = count(d)
    click = mul(biquad(noise(rng, d), "band", (2600, 2300, 2900)[v], 0.8), env(n, 0.0005, 0.0025))
    wood = knock(n, 0.001, [((210, 190, 235)[v], 12, 1.0), ((430, 390, 470)[v], 12, 0.8), ((820, 760, 900)[v], 12, 0.45)])
    thwack = mul(biquad(noise(rng, d), "band", (1000, 900, 1150)[v], 1.2), env(n, 0.001, 0.04))
    crunch = mul(biquad(grains(rng, d, lambda _: 2500, 0.001), "band", (1700, 1550, 1850)[v], 1.0), env(n, 0.001, 0.03))
    body = thump(rng, d, (170, 155, 185)[v], 90, (0.045, 0.042, 0.05)[v])
    hit = mix(n, (click, 0.35), (wood, 1.0), (thwack, 0.9), (crunch, 0.5), (body, 0.75))
    return [math.tanh(1.8 * x) for x in norm(hit)]                            # squeezed: a punch, not a tick


# ---------------------------------------------------------------- blade form: every swing peaks at ~0.08 s

ICE = ([(1900, 7, 1.0), (2900, 8, 0.6), (4100, 9, 0.3)], [(1750, 7, 1.0), (2650, 8, 0.6), (3800, 9, 0.3)])


def slash(rng, v):
    d = (0.32, 0.30, 0.33)[v]
    n, peak = count(d), (0.08, 0.075, 0.085)[v]
    low, high = ((420, 1900), (480, 2100), (380, 1750))[v]
    air = whoosh(rng, d, peak, 0.045, 0.06, low, high)
    edge = glint(n, peak, [((2350, 2500, 2200)[v], 0.035, 1.0), ((3550, 3800, 3300)[v], 0.025, 0.6)])
    return mix(n, (air, 1.0), (edge, 0.18))


def thrust(rng, v):
    d = (0.22, 0.20)[v]
    n, peak = count(d), (0.075, 0.07)[v]
    air = whoosh(rng, d, peak, 0.05, 0.025, (700, 800)[v], (2600, 2400)[v], 2.0, 0.2)
    pierce = mul(biquad(noise(rng, d), "band", lambda t: 1200 + 1800 * min(1.0, t / peak), 3.0), bump(n, peak - 0.03, peak + 0.015))
    edge = glint(n, peak, [((3000, 2800)[v], 0.02, 1.0)])
    return mix(n, (air, 1.0), (pierce, 0.5), (edge, 0.15))


def slash_heavy(rng, v):
    d = (0.44, 0.42)[v]
    n, peak = count(d), (0.09, 0.095)[v]
    air = whoosh(rng, d, peak, 0.06, 0.09, (230, 260)[v], (1200, 1100)[v], 0.7, 0.25)
    weight = mul(biquad(noise(rng, d), "low", 320), bump(n, peak - 0.07, peak + 0.12))
    chop = thump(rng, d, (150, 140)[v], 70, 0.05, start=peak)
    ice = crack(rng, d, peak + 0.004, ICE[v], (3, 4)[v])
    edge = glint(n, peak, [((1800, 1950)[v], 0.05, 1.0), ((2700, 2900)[v], 0.035, 0.5)])
    return mix(n, (air, 1.0), (weight, 0.55), (chop, 0.45), (ice, 0.5), (edge, 0.15))


def cleave(rng, v):
    d = (0.56, 0.52)[v]
    n, peak = count(d), (0.085, 0.09)[v]
    air = whoosh(rng, d, peak, 0.065, 0.11, (170, 200)[v], (950, 850)[v], 0.6, 0.2)
    drag = mul(biquad(noise(rng, d), "band", lambda t: 260 + 240 * math.exp(-max(0.0, t - peak) / 0.15), 0.8),
               env(n, 0.05, 0.14, start=0.04))                                 # the air pulled after the blade
    body = thump(rng, d, (125, 115)[v], 52, 0.09, start=peak)
    ice = crack(rng, d, peak + 0.004, [(f * 0.8, q, g) for f, q, g in ICE[v]], 5, gap=0.016)
    return mix(n, (air, 1.0), (drag, 0.5), (body, 0.6), (ice, 0.55))


def dash_slash(rng, v):
    d = (0.36, 0.34)[v]
    n, peak = count(d), (0.08, 0.075)[v]
    rush = mul(biquad(noise(rng, d), "band", lambda t: 500 + 900 * min(1.0, t / peak), 0.8), env(n, 0.03, 0.02, hold=0.02))
    speed = [math.exp(-((i / RATE - peak) / (0.04 if i / RATE < peak else 0.1)) ** 2) ** 1.2 for i in range(n)]
    passing = mul(biquad(noise(rng, d), "band", lambda t: 420 + (2200, 2000)[v] * math.exp(-t / 0.09), 1.1), speed)
    edge = glint(n, peak, [((2600, 2450)[v], 0.03, 1.0)])
    return mix(n, (passing, 1.0), (rush, 0.55), (edge, 0.15))


def spin_cleave(rng, v):
    d = (0.62, 0.58)[v]
    n, (first, second) = count(d), ((0.08, 0.31), (0.08, 0.29))[v]
    whirl = [1 - 0.3 * (0.5 + 0.5 * math.cos(2 * math.pi * (15, 13)[v] * i / RATE)) for i in range(n)]
    a = mul(whoosh(rng, d, first, 0.05, 0.08, (260, 280)[v], (1300, 1200)[v], 0.8, 0.25), whirl)
    b = mul(whoosh(rng, d, second, 0.07, 0.1, (220, 240)[v], (1100, 1000)[v], 0.7, 0.2), whirl)
    body = thump(rng, d, 130, 55, 0.07, start=second)
    ice = crack(rng, d, first + 0.004, ICE[v], 3)
    return mix(n, (a, 1.0), (b, 0.9), (body, 0.45), (ice, 0.45))


# ---------------------------------------------------------------- water abilities

def tide_release(rng, v):
    d = (0.62, 0.58)[v]
    n, top = count(d), d - 0.12
    build = swell(n, top, 0.05, 1.8)
    surge = churn(rng, mul(biquad(biquad(noise(rng, d), "band", lambda t: 260 + 900 * min(1.0, t / top), 0.7), "low", 3000), build), 30)
    depth = mul(biquad(noise(rng, d), "low", 200), build)
    fizz = bubbles(rng, d, lambda t: 700 * follow(build)(t), 300, 1300, 1.3)
    rings = [0.0] * n
    for k, at in enumerate((0.12, 0.28, 0.42)):                               # the spell's soft liquid shimmer
        bubble(rings, at * d / 0.62, (330, 300)[v] * 1.18 ** k, 0.06, 0.7 + 0.15 * k, 0.3)
    return mix(n, (surge, 1.0), (depth, 0.6), (fizz, 0.45), (rings, 0.22))


def tide_impact(rng, v):
    d = (0.64, 0.60)[v]
    n = count(d)
    crash = mul(bands(noise(rng, d), [(320, 0.7, 1.0), (900, 0.7, 0.8), (2000, 0.9, 0.4)]), env(n, 0.004, 0.11))
    body = thump(rng, d, (110, 100)[v], 46, 0.10)
    wash = churn(rng, mul(biquad(biquad(noise(rng, d), "band", lambda t: 700 - 300 * min(1.0, t / 0.4), 0.6), "low", 2500),
                          env(n, 0.03, 0.18, start=0.02)), 25)
    drops = bubbles(rng, d, lambda t: 0 if t < 0.04 else 300 * math.exp(-(t - 0.04) / 0.15), 900, 2400, 2.0)
    gulps = bubbles(rng, d, lambda t: 90 * math.exp(-t / 0.2), 180, 450, 1.5)
    return mix(n, (crash, 1.0), (body, 0.8), (wash, 0.7), (drops, 0.4), (gulps, 0.35))


def finisher_release(rng, v):
    d = (0.46, 0.42)[v]
    n, burst = count(d), (0.09, 0.08)[v]
    squeeze = mul(biquad(noise(rng, d), "band", lambda t: 300 + 700 * min(1.0, t / burst) ** 2, 1.5), swell(n, burst, 0.015, 2.5))
    tone, phase = [], 0.0                                                      # pressure gathering, low and rising
    for i in range(n):
        t = i / RATE
        phase += 2 * math.pi * (150 + 250 * min(1.0, t / burst)) / RATE
        tone.append(math.sin(phase) * (math.sin(min(1.0, t / burst) * math.pi / 2) ** 2 if t < burst
                                       else math.exp(-(t - burst) / 0.03)))
    launch = churn(rng, mul(biquad(noise(rng, d), "band", lambda t: 500 + 800 * math.exp(-max(0.0, t - burst) / 0.08), 0.9),
                            env(n, 0.004, 0.09, hold=0.02, start=burst)))
    fizz = bubbles(rng, d, lambda t: 500 * math.exp(-(t - burst) / 0.1) if t > burst else 0, 500, 1500, 1.4)
    return mix(n, (squeeze, 0.7), (tone, 0.35), (launch, 1.0), (fizz, 0.4))


def finisher_impact(rng, v):
    d = (0.44, 0.42)[v]
    n = count(d)
    front = mul(biquad(noise(rng, d), "band", (3000, 2700)[v], 0.9), env(n, 0.0005, 0.004))
    smack = mul(bands(noise(rng, d), [(700, 1.0, 1.0), (1600, 1.0, 0.7)]), env(n, 0.002, 0.03))
    body = thump(rng, d, (140, 125)[v], 52, 0.06)
    splash = mul(biquad(noise(rng, d), "band", lambda t: 1500 - 700 * min(1.0, t / 0.2), 0.7), env(n, 0.01, 0.08, start=0.012))
    drops = bubbles(rng, d, lambda t: 0 if t < 0.03 else 420 * math.exp(-(t - 0.03) / 0.07), 1000, 2600, 2.0)
    plop = bubble([0.0] * n, 0.008, (230, 250)[v], 0.04, rise=0.4)
    return mix(n, (front, 0.6), (smack, 1.0), (body, 0.7), (splash, 0.7), (drops, 0.45), (plop, 0.4))


# ---------------------------------------------------------------- the warden

def guardian_slam(rng, v):
    d = (0.80, 0.76)[v]
    n = count(d)
    stone = [a + b for a, b in zip(
        crack(rng, d, 0.004, [(650, 4, 1.0), (1350, 5, 0.8), (2400, 5, 0.45), (3600, 6, 0.2)], 3, 1100, 0.008),
        [x * 0.7 for x in crack(rng, d, (0.035, 0.05)[v], [(560, 4, 1.0), (1200, 5, 0.8), (2100, 5, 0.4)], 2, 1000)])]
    body = thump(rng, d, (95, 88)[v], 40, 0.11)
    mass = knock(n, 0.003, [(180, 6, 1.0), (310, 7, 0.6)])                     # what small speakers hear of the body
    rumble = mul(biquad(noise(rng, d), "low", 260), env(n, 0.006, 0.11))
    crumble = mul(biquad(grains(rng, d, lambda _: 1800, 0.002), "band", (1000, 880)[v], 0.8), env(n, 0.003, 0.07, start=0.006))
    debris = biquad(grains(rng, d, lambda t: 0 if t < 0.03 else 140 * math.exp(-(t - 0.03) / 0.16), 0.004), "band", 1400, 0.7)
    pebbles = [0.0] * n
    for k in range((6, 7)[v]):
        at = 0.06 + 0.38 * rng.random()
        hit = knock(n, at, [(1500 + 1500 * rng.random(), 12, 1.0)])
        pebbles = [a + b * 0.8 ** k for a, b in zip(pebbles, hit)]
    dust = mul(biquad(noise(rng, d), "band", 900, 0.6), env(n, 0.03, 0.16, start=0.02))
    return mix(n, (stone, 1.0), (body, 0.8), (mass, 0.8), (rumble, 0.45), (crumble, 0.6), (debris, 0.6),
               (pebbles, 0.35), (dust, 0.3))


# family -> (sound, takes, peak level of full scale)
FAMILIES = {
    "blob_attack": (blob_attack, 3, 0.74),
    "slime_dash": (slime_dash, 2, 0.70),
    "water_splash": (water_splash, 2, 0.72),
    "slime_hurt": (slime_hurt, 2, 0.72),
    "enemy_hit": (enemy_hit, 3, 0.78),
    "slash": (slash, 3, 0.76),
    "thrust": (thrust, 2, 0.74),
    "slash_heavy": (slash_heavy, 2, 0.78),
    "cleave": (cleave, 2, 0.80),
    "dash_slash": (dash_slash, 2, 0.76),
    "spin_cleave": (spin_cleave, 2, 0.78),
    "tide_release": (tide_release, 2, 0.72),
    "tide_impact": (tide_impact, 2, 0.80),
    "finisher_release": (finisher_release, 2, 0.74),
    "finisher_impact": (finisher_impact, 2, 0.80),
    "guardian_slam": (guardian_slam, 2, 0.80),
}


def render():
    """file name -> PCM16 bytes, for every take of every family."""
    takes = {}
    for family, (sound, count_, level) in FAMILIES.items():
        for v in range(count_):
            name = family if v == 0 else f"{family}_{v + 1}"
            takes[name] = finish(sound(random.Random("combat:" + name), v), level).tobytes()
    return takes


def main():
    check = "--check" in sys.argv
    stale = []
    for name, pcm in render().items():
        path = OUT / f"{name}.wav"
        if check:
            if not path.exists() or wav_bytes(path) != pcm:
                stale.append(path.name)
            continue
        with wave.open(str(path), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm)
        print(f"{path.name}: {len(pcm) / 2 / RATE:.2f}s")
    if stale:
        sys.exit("differs from the generator: " + ", ".join(stale))
    if check:
        print("combat sfx match the generator")


if __name__ == "__main__":
    main()
