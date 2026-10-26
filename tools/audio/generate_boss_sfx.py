#!/usr/bin/env python3
"""Outpost Warden and Briarheart attack sounds, synthesized from scratch: no samples, services or keys.

    python3 tools/audio/generate_boss_sfx.py           # write the 20 takes below into assets/audio
    python3 tools/audio/generate_boss_sfx.py --check   # re-render in memory, exit 1 if a file differs

Warden      warden_windup    armour plates grinding as the blade comes up, mail shifting, a held breath
            warden_sweep     a heavy blade dragging the air round, its edge ringing, grit thrown off
            warden_rush      plated bulk breaking into a run: clanking strides, rushing air, a gravel skid
            warden_fracture  the blade point driven in, stone splitting away down a line, rubble settling
            warden_slam      the existing stone impact with a short armored exertion
            warden_strain    phase-two windup: uneven breath and a rougher held grunt
Briarheart  briar_windup     living wood creaking under strain, a deep breath drawn through bark, leaves
            briar_roots      soil heaving, root fibres tearing up through it, dirt falling back
            briar_seeds      a dry pod cracking open, seeds scattering and ticking down
            briar_cage       branches bursting up, bending with a groan, snapping shut

B2BJ starts a windup BossFx.WINDUP_LEAD (0.62 s) before the release, so each windup peaks just ahead
of the blow; the release plays once, on the attack's GUARDIAN_SLAM event, hit or miss.

Reuses generate_forest_sfx's and generate_combat_sfx's building blocks and finish(): 22050 Hz mono
PCM16, high-passed (no DC), peak-normalised below full scale, faded edges. Seeded, so a rerun writes
identical bytes. Only the 20 files named in FAMILIES are written; no other asset is touched.
"""
import math
import random
import sys
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_forest_sfx import (OUT, RATE, bands, biquad, bump, count, env, finish, grains, mix, mul,  # noqa: E402
                                 noise, stick_slip, swell, thump, voice, wav_bytes)
from generate_combat_sfx import churn, crack, glint, guardian_slam, knock, whoosh  # noqa: E402
from warden_voice_fx import treatment  # noqa: E402


def add(x, y, gain=1.0):
    return [a + b * gain for a, b in zip(x, y)]


# ---------------------------------------------------------------- the Outpost Warden; v is the take

PLATES = ([(380, 18, 1.0), (1020, 22, 0.7), (2150, 24, 0.4), (3300, 26, 0.18)],
          [(340, 18, 1.0), (930, 22, 0.7), (1980, 24, 0.4), (3050, 26, 0.18)])
STONE = ([(620, 4, 1.0), (1280, 5, 0.8), (2250, 5, 0.45)], [(570, 4, 1.0), (1190, 5, 0.8), (2100, 5, 0.45)])


def warden_effort(rng, v, strained=False):
    """A short nonverbal exertion, using the spoken Warden's quiet armor/room treatment."""
    d = (0.25, 0.23)[v]
    n = count(d)
    folds = voice(rng, d, lambda t: (92, 81)[v] * (1.12 - .3 * t / d),
                  jitter=.12 if strained else .055, sub=.3 if strained else .12, breath=.6)
    throat = bands(folds, [(340, 2.5, 1), (760, 3, .65), (1550, 3.5, .25)])
    air = biquad(noise(rng, d), "band", 950, .65)
    dry = mul(mix(n, (throat, 1), (air, .28 if strained else .16)), bump(n, .01, d - .01))
    return treatment(dry)


def warden_windup(rng, v, strained=False):
    d = (0.60, 0.56)[v]
    n, top = count(d), d - 0.10
    tense = lambda t: min(1.0, t / top)
    build = swell(n, top, 0.045, 1.8)
    strain = mul(bands(stick_slip(rng, d, lambda t: 28 + 150 * tense(t) ** 1.5, 0.02, top + 0.03), PLATES[v]), build)
    mail = mul(biquad(grains(rng, d, lambda t: 30 + 260 * tense(t), 0.0012), "band", (2400, 2200)[v], 2.0), build)
    weight = mul(biquad(noise(rng, d), "low", 180), build)
    inhale = mul(bands(noise(rng, d), [(330, 1.5, 1), (1000, 1.5, .5)]), bump(n, .015, .30))
    if strained:
        inhale = [s * (.72 + .28 * math.sin(2 * math.pi * 9 * i / RATE)) for i, s in enumerate(inhale)]
    effort = warden_effort(rng, v, strained)
    scrape = mul(biquad(noise(rng, d), "band", lambda t: 900 + 1700 * tense(t) ** 2, 6), bump(n, top - 0.22, top + 0.04))
    return mix(n, (strain, 1.0), (mail, 0.35), (weight, 0.5), (inhale, .48 if strained else .28),
               (effort, .70 if strained else .20, .06), (scrape, 0.3))


def warden_sweep(rng, v):
    d = (0.62, 0.58)[v]
    n, peak = count(d), (0.13, 0.12)[v]
    whirl = [1 - 0.25 * (0.5 + 0.5 * math.cos(2 * math.pi * (11, 13)[v] * i / RATE)) for i in range(n)]
    air = mul(whoosh(rng, d, peak, 0.08, 0.13, (150, 170)[v], (820, 760)[v], 0.6, 0.22), whirl)
    drag = mul(biquad(noise(rng, d), "band", lambda t: 240 + 260 * math.exp(-max(0.0, t - peak) / 0.12), 0.8),
               env(n, 0.06, 0.13, start=0.05))                                  # the air pulled round after it
    edge = glint(n, peak - 0.01, [((1350, 1480)[v], 0.09, 1.0), ((2110, 2290)[v], 0.06, 0.55), ((3240, 3420)[v], 0.04, 0.25)])
    mass = mul(biquad(noise(rng, d), "low", 220), bump(n, peak - 0.09, peak + 0.16))
    grit = biquad(grains(rng, d, lambda t: 0 if t < peak else 900 * math.exp(-(t - peak) / 0.08), 0.002), "band", 1300, 0.9)
    return mix(n, (air, 1.0), (drag, 0.45), (edge, 0.22), (mass, 0.55), (grit, 0.35),
               (warden_effort(rng, v), .65))


def warden_rush(rng, v):
    d = (0.58, 0.54)[v]
    n = count(d)
    steps = ((0.0, 0.13, 0.25, 0.36), (0.0, 0.12, 0.23, 0.33))[v]
    strides, plates = [0.0] * n, [0.0] * n
    for k, at in enumerate(steps):
        strides = add(strides, thump(rng, d, (120, 110)[v] - 6 * k, 50, 0.05, start=at), 0.85 ** k)
        plates = add(plates, knock(n, at + 0.004, [(f * (1 + 0.04 * (k % 2)), q, g) for f, q, g in PLATES[v][:3]]), 0.8 ** k)
    hit = lambda t: sum(math.exp(-(t - s) / 0.03) for s in steps if t >= s)
    rattle = biquad(grains(rng, d, lambda t: 60 + 900 * hit(t), 0.0012), "band", (2300, 2100)[v], 1.8)
    air = churn(rng, mul(biquad(noise(rng, d), "band", lambda t: 320 + 800 * math.sin(math.pi * min(1.0, t / (d * 0.9))), 0.7),
                         swell(n, (0.30, 0.28)[v], 0.09, 1.4)), 20)
    effort = warden_effort(rng, v)
    end = steps[-1] + 0.03
    skid = mul(biquad(grains(rng, d, lambda t: 1600 if t > end else 0, 0.002), "band",
                      lambda t: 1500 - 800 * min(1.0, max(0.0, t - end) / 0.12), 1.0), bump(n, end, d - 0.01))
    return mix(n, (strides, 0.8), (plates, 0.45), (rattle, 0.3), (air, 1.0), (effort, 0.65), (skid, 0.4))


def warden_fracture(rng, v):
    d = (0.74, 0.70)[v]
    n = count(d)
    stab = knock(n, 0.002, [((760, 690)[v], 10, 1.0), ((1540, 1420)[v], 12, 0.6), ((2480, 2300)[v], 14, 0.3)])
    body = thump(rng, d, (105, 96)[v], 42, 0.09)
    split, at, amp = [0.0] * n, 0.03, 1.0                                       # the split running away down the line
    for k in range((9, 8)[v]):
        split = add(split, crack(rng, d, at, [(f * (1 - 0.03 * k), q, g) for f, q, g in STONE[v]], 2, 1200, 0.006), amp)
        at += 0.028 + 0.02 * rng.random()
        amp *= 0.82
    rip = mul(biquad(noise(rng, d), "band", lambda t: 180 + 260 * math.exp(-t / 0.15), 1.2), env(n, 0.02, 0.12, hold=0.08, start=0.02))
    rubble = biquad(grains(rng, d, lambda t: 0 if t < 0.08 else 220 * math.exp(-(t - 0.08) / 0.16), 0.004), "band", 1100, 0.8)
    pebbles = [0.0] * n
    for k in range((6, 5)[v]):
        pebbles = add(pebbles, knock(n, 0.15 + 0.38 * rng.random(), [(1400 + 1800 * rng.random(), 12, 1.0)]), 0.8 ** k)
    dust = mul(biquad(noise(rng, d), "band", 800, 0.6), env(n, 0.03, 0.15, start=0.05))
    return mix(n, (stab, 0.5), (body, 0.8), (split, 1.0), (rip, 0.5), (rubble, 0.5), (pebbles, 0.3), (dust, 0.25),
               (warden_effort(rng, v), .65))


def warden_slam(rng, v):
    impact = guardian_slam(rng, v)
    return mix(len(impact), (impact, 1), (warden_effort(rng, v), .50))


# ---------------------------------------------------------------- Briarheart

TRUNK = ([(170, 9, 1.0), (430, 12, 0.8), (910, 11, 0.5), (1600, 9, 0.25)],
         [(150, 9, 1.0), (390, 12, 0.8), (840, 11, 0.5), (1480, 9, 0.25)])
TWIG = ([(900, 9, 1.0), (1700, 10, 0.7), (2800, 9, 0.35)], [(820, 9, 1.0), (1580, 10, 0.7), (2600, 9, 0.35)])


def briar_windup(rng, v):
    d = (0.62, 0.58)[v]
    n, top = count(d), d - 0.10
    tense = lambda t: min(1.0, t / top)
    if v == 0:   # the trunk bends twice, the second time further
        creak = add(stick_slip(rng, d, lambda t: 22 + 90 * t, 0.02, 0.24),
                    stick_slip(rng, d, lambda t: 30 + 140 * t, 0.27, top + 0.03))
        gate = add(bump(n, 0.01, 0.25), bump(n, 0.26, top + 0.05))
    else:        # one long groan
        creak = stick_slip(rng, d, lambda t: 20 + 120 * tense(t) ** 1.3, 0.02, top + 0.03)
        gate = bump(n, 0.01, top + 0.05)
    wood = mul(bands(creak, TRUNK[v]), gate)
    inhale = mul(biquad(bands(noise(rng, d), [(380, 2.5, 1.0), (820, 3.5, 0.6), (1500, 4, 0.25)]), "low", 2000),
                 swell(n, top, 0.04, 1.6))
    groan = mul(bands(voice(rng, d, lambda t: (46, 52)[v] * (1 + 0.12 * tense(t)), jitter=0.04, sub=0.3, breath=0.35),
                      [(210, 4, 1.0), (470, 5, 0.5)]), swell(n, top, 0.05, 2.0))
    rustle = mul(biquad(grains(rng, d, lambda t: 40 + 300 * tense(t), 0.002), "band", 2100, 1.2), swell(n, top, 0.04))
    return mix(n, (wood, 1.0), (inhale, 0.7), (groan, 0.35), (rustle, 0.25))


def briar_roots(rng, v):
    d = (0.60, 0.56)[v]
    n = count(d)
    heave = thump(rng, d, (110, 100)[v], 45, 0.08)
    tears, fibres = [0.0] * n, [0.0] * n
    for k in range((7, 6)[v]):
        at, length = 0.02 + 0.34 * k / 6 + 0.03 * rng.random(), 0.06 + 0.03 * rng.random()
        flutter, phase = 30 + 30 * rng.random(), rng.random() * 6.28
        lift = lambda t, at=at, length=length: 500 + 1000 * min(1.0, max(0.0, t - at) / length)
        tear = biquad(noise(rng, d), "band", lift, 1.3, 8)
        shape = bump(n, at, at + length)
        tears = add(tears, [s * w * (0.6 + 0.4 * math.sin(2 * math.pi * flutter * i / RATE + phase))
                            for i, (s, w) in enumerate(zip(tear, shape))], 0.88 ** k)
        fibres = add(fibres, stick_slip(rng, d, lambda _: 1800, at, at + length * 0.7, 0.5, 0.88 ** k))
    fibres = bands(fibres, [((650, 600)[v], 5, 1.0), ((1300, 1200)[v], 6, 0.6), (2100, 6, 0.3)])
    pull = mul(bands(stick_slip(rng, d, lambda t: 60 + 900 * t, 0.02, 0.3), [(300, 8, 1.0), (700, 9, 0.6)]), bump(n, 0.01, 0.32))
    dirt = biquad(grains(rng, d, lambda t: 0 if t < 0.03 else 700 * math.exp(-(t - 0.03) / 0.14), 0.002), "band", 1200, 0.8)
    fall = mul(biquad(noise(rng, d), "low", 800), env(n, 0.03, 0.12, start=0.06))
    return mix(n, (heave, 0.7), (tears, 1.0), (fibres, 0.6), (pull, 0.45), (dirt, 0.5), (fall, 0.3))


def briar_seeds(rng, v):
    d = (0.42, 0.38)[v]
    n = count(d)
    pod = knock(n, 0.004, [((820, 760)[v], 14, 1.0), ((1650, 1520)[v], 16, 0.6), ((2700, 2500)[v], 12, 0.25)])
    click = mul(biquad(noise(rng, d), "band", 2800, 0.8), env(n, 0.0005, 0.002, start=0.004))
    split = crack(rng, d, 0.006, TWIG[v], 2, 1500, 0.008)
    puff = mul(biquad(noise(rng, d), "band", 900, 1.0), env(n, 0.003, 0.03, start=0.006))
    ticks = [0.0] * n
    for _ in range((18, 15)[v]):                                                # seeds land, densest just after the pop
        at = 0.03 + 0.3 * rng.random() ** 1.6
        ticks = add(ticks, knock(n, at, [(1700 + 1700 * rng.random(), 18, 1.0)]), (0.4 + 0.6 * rng.random()) * (1 - at))
    rattle = biquad(grains(rng, d, lambda t: 0 if t < 0.02 else 500 * math.exp(-(t - 0.02) / 0.1), 0.0015), "band", 2600, 2.0)
    body = thump(rng, d, (150, 135)[v], 70, 0.04)
    return mix(n, (pod, 1.0), (click, 0.4), (split, 0.6), (puff, 0.4), (ticks, 0.8), (rattle, 0.25), (body, 0.35))


def briar_cage(rng, v):
    d = (0.70, 0.66)[v]
    n, shut = count(d), (0.50, 0.47)[v]
    burst = thump(rng, d, (115, 105)[v], 48, 0.07)
    soil = biquad(grains(rng, d, lambda t: 1400 * math.exp(-t / 0.06), 0.002), "band", 1100, 0.8)
    emerge = crack(rng, d, 0.01, TWIG[v], 3, 1300, 0.014)
    bend = mul(bands(stick_slip(rng, d, lambda t: 25 + 220 * t, 0.08, shut - 0.02), TRUNK[v][:3]), bump(n, 0.07, shut + 0.01))
    snap = crack(rng, d, shut, [(f * 1.1, q, g) for f, q, g in TWIG[v]], 4, 1500, 0.02)
    knocks = [0.0] * n
    for k in range(3):
        knocks = add(knocks, knock(n, shut + 0.02 * k + 0.008 * rng.random(), [((420, 380)[v], 10, 1.0), (980, 12, 0.5)]), 0.8 ** k)
    rustle = mul(biquad(grains(rng, d, lambda t: 0 if t < shut else 500 * math.exp(-(t - shut) / 0.06), 0.002), "band", 2000, 1.2),
                 env(n, 0.01, 0.06, start=shut))
    return mix(n, (burst, 0.6), (soil, 0.35), (emerge, 0.6), (bend, 0.8), (snap, 1.0), (knocks, 0.45), (rustle, 0.2))


# family -> (sound, peak level of full scale). Windups sit under the blows they lead into.
FAMILIES = {
    "warden_windup": (warden_windup, 0.68),
    "warden_sweep": (warden_sweep, 0.78),
    "warden_rush": (warden_rush, 0.76),
    "warden_fracture": (warden_fracture, 0.76),
    "briar_windup": (briar_windup, 0.66),
    "briar_roots": (briar_roots, 0.78),
    "briar_seeds": (briar_seeds, 0.74),
    "briar_cage": (briar_cage, 0.78),
    "warden_strain": (lambda rng, v: warden_windup(rng, v, strained=True), 0.68),
    "warden_slam": (warden_slam, 0.78),
}


def render():
    """file name -> PCM16 bytes, two takes per family."""
    takes = {}
    for family, (sound, level) in FAMILIES.items():
        for v in (0, 1):
            name = family if v == 0 else f"{family}_2"
            takes[name] = finish(sound(random.Random("boss:" + name), v), level).tobytes()
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
        print("boss sfx match the generator")


if __name__ == "__main__":
    main()
