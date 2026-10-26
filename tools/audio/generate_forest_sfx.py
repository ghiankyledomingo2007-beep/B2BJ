#!/usr/bin/env python3
"""Chapter Two forest enemy attack sounds, synthesized from scratch: no samples, services or keys.

    python3 tools/audio/generate_forest_sfx.py           # write assets/audio/<cue>.wav and <cue>_2.wav
    python3 tools/audio/generate_forest_sfx.py --check   # re-render in memory, exit 1 if a file differs

Rift Imp     imp_windup   small throat hissing as the claws draw back, claw tips chittering
             imp_claw     fast airy tear over a gritty rasp (the combo plays it once per swipe)
Thorn Wolf   wolf_growl   chesty, breathy growl with a rolling rasp
             wolf_bite    dry tooth clack over a jaw thud, the lunge's breath just before
             wolf_pounce  air and fur rushing past, an effort huff at the jump
             wolf_land    two paw thumps and kicked-up dirt
Mire Shaman  mire_charge  staff wood creaking under a low humming magical breath
             mire_erupt   ground heave, roots cracking out of the soil, dirt falling back

Only these 16 files are written; no other asset is touched. Python stdlib only and every take is
seeded, so a rerun writes identical bytes. Output is GameAudio's native format (22050 Hz mono PCM16),
high-passed (no DC), peak-normalised below full scale, with faded edges. Two takes per cue:
GameAudio never plays the same take twice in a row, so repeated attacks vary.
"""
import array
import math
import random
import sys
import wave
from pathlib import Path

RATE = 22_050
OUT = Path(__file__).resolve().parents[2] / "assets/audio"


# ---------------------------------------------------------------- building blocks (lists of floats)

def count(seconds):
    return int(seconds * RATE)


def noise(rng, seconds):
    return [rng.random() * 2 - 1 for _ in range(count(seconds))]


def biquad(x, kind, freq, q=0.707, step=16):
    """RBJ cookbook low/high/band-pass (band: 0 dB peak). freq is Hz or a function of time for sweeps."""
    at = freq if callable(freq) else (lambda _: freq)
    y = [0.0] * len(x)
    x1 = x2 = y1 = y2 = 0.0
    b0 = b1 = b2 = a1 = a2 = 0.0
    for i, v in enumerate(x):
        if i % step == 0:
            f = at(i / RATE)
            w = 2 * math.pi * min(max(f, 10.0), RATE * 0.45) / RATE
            cw, alpha = math.cos(w), math.sin(w) / (2 * q)
            if kind == "low":
                b0, b1, b2 = (1 - cw) / 2, 1 - cw, (1 - cw) / 2
            elif kind == "high":
                b0, b1, b2 = (1 + cw) / 2, -(1 + cw), (1 + cw) / 2
            else:
                b0, b1, b2 = alpha, 0.0, -alpha
            a0 = 1 + alpha
            b0, b1, b2, a1, a2 = b0 / a0, b1 / a0, b2 / a0, -2 * cw / a0, (1 - alpha) / a0
        out = b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, v, y1, out
        y[i] = out
    return y


def bands(x, spec):
    """Sum of band-passes [(Hz, Q, gain)]: formants of a throat, modes of a piece of wood."""
    out = [0.0] * len(x)
    for f, q, gain in spec:
        for i, v in enumerate(biquad(x, "band", f, q)):
            out[i] += v * gain
    return out


def norm(x, level=1.0):
    peak = max(1e-12, max(abs(v) for v in x))
    return [v * level / peak for v in x]


def mul(x, shape):
    return [a * b for a, b in zip(x, shape)]


def mix(n, *layers):
    """(sound, gain[, start seconds]) layers summed into n samples; each sound is peak-normalised first."""
    out = [0.0] * n
    for layer in layers:
        sound, gain, start = layer if len(layer) == 3 else (*layer, 0.0)
        at = count(start)
        for i, v in enumerate(norm(sound, gain)[: max(0, n - at)]):
            out[at + i] += v
    return out


def env(n, attack, decay, hold=0.0, start=0.0):
    """Silent until start, sine rise over attack, hold, then an e-fold every decay seconds."""
    out = []
    for i in range(n):
        t = i / RATE - start
        if t < 0:
            out.append(0.0)
        elif t < attack:
            out.append(math.sin(t / attack * math.pi / 2))
        elif t < attack + hold:
            out.append(1.0)
        else:
            out.append(math.exp(-(t - attack - hold) / decay))
    return out


def swell(n, peak_at, tail, curve=2.0):
    """A tensing build to peak_at, then an exponential release."""
    return [math.sin(min(1.0, i / RATE / peak_at) * math.pi / 2) ** curve if i / RATE < peak_at
            else math.exp(-(i / RATE - peak_at) / tail) for i in range(n)]


def bump(n, start, stop):
    """A smooth sin^2 window from start to stop, zero outside."""
    out = []
    for i in range(n):
        t = (i / RATE - start) / (stop - start)
        out.append(math.sin(math.pi * t) ** 2 if 0 < t < 1 else 0.0)
    return out


def grains(rng, seconds, rate, decay=0.0015, start=0.0):
    """Random tiny noise ticks, rate(t) per second: scraping grit, fur, kicked dirt."""
    out, level = [], 0.0
    keep = math.exp(-1 / (decay * RATE))
    for i in range(count(seconds)):
        t = i / RATE
        level *= keep
        if t >= start and rng.random() < rate(t) / RATE:
            level += 0.3 + 0.7 * rng.random()
        out.append(level * (rng.random() * 2 - 1))
    return out


def stick_slip(rng, seconds, rate, start, stop, jitter=0.25, amp=1.0):
    """Friction that sticks and slips: an impulse every 1/rate(t) s, jittered. Through narrow wood
    resonances that is a creak; very fast and short it is a splinter."""
    out = [0.0] * count(seconds)
    t = start
    while t < stop and count(t) < len(out):
        out[count(t)] += amp * (0.55 + 0.45 * rng.random())
        t += (1 + (rng.random() * 2 - 1) * jitter) / rate(t)
    return out


def voice(rng, seconds, pitch, jitter, sub, breath, soft=0.82):
    """Creature vocal folds: a soft pulse per cycle at pitch(t) Hz with jitter, every other pulse weaker
    (period doubling: the rasp of a growl), and breath noise puffing as the folds open."""
    out, phase, wobble, odd, glottis = [], 0.0, 1.0, False, 0.0
    for i in range(count(seconds)):
        phase += pitch(i / RATE) * wobble / RATE
        if phase >= 1:
            phase -= 1
            wobble = 1 + (rng.random() * 2 - 1) * jitter
            odd = not odd
            glottis += (1 - sub if odd else 1.0) * (0.75 + 0.5 * rng.random())
        glottis *= soft
        out.append(glottis + (rng.random() * 2 - 1) * breath * (1 - phase) ** 2)
    return out


def thump(rng, seconds, high, low, decay, start=0.0):
    """A body hitting the ground: a falling low tone inside a burst of muffled noise. The noise carries
    the thump on small speakers that cannot reproduce the tone."""
    out, phase = [], 0.0
    for i in range(count(seconds)):
        t = i / RATE - start
        if t < 0:
            out.append(0.0)
            continue
        phase += 2 * math.pi * (low + (high - low) * math.exp(-t / 0.03)) / RATE
        out.append(math.sin(phase) * math.exp(-t / decay) * min(1.0, t / 0.002))
    muffle = mul(biquad(noise(rng, seconds), "low", 700), env(len(out), 0.002, decay * 0.6, start=start))
    return [a + b * 0.6 for a, b in zip(norm(out), norm(muffle))]


# ---------------------------------------------------------------- the sounds; v is the take (0 or 1)

def imp_windup(rng, v):
    d = (0.36, 0.31)[v]
    n, top = count(d), d - 0.07
    tense = lambda t: min(1.0, t / top)
    base = (205, 240)[v]
    throat = voice(rng, d, lambda t: base * (1 + 0.4 * tense(t)), jitter=0.06, sub=0.45, breath=0.6, soft=0.7)
    snarl = bands(throat, [(820, 4, 1.0), (1650, 5, 0.6), (2900, 6, 0.22)])
    draw = biquad(biquad(noise(rng, d), "band", lambda t: 650 + 1400 * tense(t) ** 1.5, 1.3), "low", 4200)
    chitter = biquad(grains(rng, d, lambda t: 20 + 110 * tense(t), 0.0012), "band", (3000, 2600)[v], 3)
    return mul(mix(n, (snarl, 1.0), (draw, 0.55), (chitter, 0.35)), swell(n, top, 0.03, 1.6))


def imp_claw(rng, v):
    d = (0.22, 0.20)[v]
    n = count(d)
    high, low = ((3300, 950), (2600, 1250))[v]  # the backhand take sweeps less far
    sweep = lambda t: low + (high - low) * math.exp(-t / 0.05)
    tear = mul(biquad(biquad(noise(rng, d), "band", sweep, 1.1), "low", 5000), env(n, 0.006, 0.04, 0.02))
    rasp = mul(biquad(grains(rng, d, lambda t: 60 + 900 * math.exp(-t / 0.06), 0.0009), "band",
                      lambda t: sweep(t) * 0.75, 2.2), env(n, 0.004, 0.045, 0.03))
    push = mul(biquad(noise(rng, d), "low", 420), env(n, 0.012, 0.04))
    return mix(n, (tear, 1.0), (rasp, 0.75), (push, 0.45))


def wolf_growl(rng, v):
    d = (0.46, 0.40)[v]
    n, f0 = count(d), (82, 72)[v]
    throat = voice(rng, d, lambda t: f0 * (1 + 0.16 * math.sin(math.pi * min(1.0, t / d))),
                   jitter=0.07, sub=0.4, breath=0.32)
    chest = bands(throat, [(170, 1.4, 0.9), (480, 3, 1.0), (1050, 4, 0.45), (2350, 5, 0.15)])
    rate, phase = (25, 29)[v], rng.random() * 6.28
    roll = [1 - 0.35 * (0.5 + 0.5 * math.sin(2 * math.pi * rate * i / RATE + phase)) for i in range(n)]
    breath = biquad(bands(noise(rng, d), [(900, 0.8, 1.0), (1800, 1.0, 0.5)]), "low", 2800)
    shape = [min(1.0, i / RATE / 0.07) * math.exp(-max(0.0, i / RATE - (d - 0.13)) / 0.05) for i in range(n)]
    return mul(mix(n, (mul(chest, roll), 1.0), (breath, 0.25)), shape)


def wolf_bite(rng, v):
    d = (0.18, 0.16)[v]
    n, snap = count(d), (0.035, 0.028)[v]
    chuff = mul(biquad(noise(rng, d), "band", (1000, 850)[v], 0.9), env(n, 0.004, 0.018))
    teeth = [0.0] * n
    teeth[count(snap)] = 1.0
    teeth[count(snap + 0.006)] = -0.45  # the lower canines land a beat after the upper ones
    modes = ([(2400, 32, 1.0), (3600, 34, 0.6), (1350, 24, 0.7)], [(2150, 32, 1.0), (3300, 34, 0.6), (1200, 24, 0.7)])[v]
    clack = bands(teeth, modes)
    click = mul(biquad(noise(rng, d), "band", 3000, 0.7), env(n, 0.0005, 0.0015, start=snap))
    snap_air = mul(biquad(noise(rng, d), "band", 1500, 1.5), env(n, 0.001, 0.008, start=snap))
    jaw = thump(rng, d, (190, 170)[v], 90, 0.04, start=snap)
    return mix(n, (clack, 1.0), (click, 0.55), (snap_air, 0.5), (jaw, 0.55), (chuff, 0.45))


def wolf_pounce(rng, v):
    d = (0.32, 0.28)[v]
    n, peak = count(d), (0.14, 0.12)[v]
    center = lambda t: 380 + 900 * math.sin(math.pi * min(1.0, t / (d * 0.8)))
    shape = swell(n, peak, 0.06, 1.5)
    rush = mul(biquad(biquad(noise(rng, d), "band", center, 0.8), "low", 3000), shape)
    fur = mul(biquad(grains(rng, d, lambda t: 120 + 500 * math.sin(math.pi * min(1.0, t / d)), 0.002),
                     "band", (2100, 1800)[v], 1.2), shape)
    weight = mul(biquad(noise(rng, d), "low", 250), shape)
    huff = mul(bands(noise(rng, d), [(650, 4, 1.0), (1250, 5, 0.6)]), env(n, 0.008, 0.03))
    return mix(n, (rush, 1.0), (fur, 0.45), (weight, 0.5), (huff, 0.55))


def wolf_land(rng, v):
    d = (0.26, 0.24)[v]
    n, gap = count(d), (0.038, 0.052)[v]
    front = thump(rng, d, 150, 60, 0.04)
    hind = thump(rng, d, (160, 140)[v], 64, 0.035, start=gap)
    dirt = biquad(grains(rng, d, lambda t: 2500 * math.exp(-t / 0.05), 0.0015, start=0.004), "band", (1700, 1500)[v], 0.9)
    scuff = mul(biquad(noise(rng, d), "band", 650, 1.1), env(n, 0.003, 0.05))
    return mix(n, (front, 1.0), (hind, 0.7), (dirt, 0.8), (scuff, 0.6))


def mire_charge(rng, v):
    d = (0.62, 0.56)[v]
    n = count(d)
    if v == 0:   # the staff bends twice
        creak = [a + b for a, b in zip(stick_slip(rng, d, lambda t: 35 + 140 * t, 0.03, 0.27),
                                       stick_slip(rng, d, lambda t: 45 + 120 * t, 0.31, 0.50, amp=0.8))]
        gate = [a + b for a, b in zip(bump(n, 0.02, 0.28), bump(n, 0.30, 0.51))]
    else:        # one long groan of wood
        creak = stick_slip(rng, d, lambda t: 30 + 110 * t, 0.04, 0.46)
        gate = bump(n, 0.03, 0.47)
    wood = mul(bands(creak, [(240, 10, 1.0), (590, 14, 0.8), (1150, 12, 0.5), (1980, 9, 0.25)]), gate)
    breath = mul(biquad(bands(noise(rng, d), [(300, 3, 1.0), (720, 4, 0.6), (1100, 5, 0.2)]), "low", 1600),
                 swell(n, d * 0.75, 0.05))
    hum = mul(bands(voice(rng, d, lambda t: (58, 52)[v] * (1 + 0.04 * t), jitter=0.01, sub=0.0, breath=0.2),
                    [(240, 6, 1.0), (480, 8, 0.5)]), swell(n, d * 0.8, 0.04))
    return mix(n, (wood, 0.9), (breath, 1.0), (hum, 0.45))


def mire_erupt(rng, v):
    d = (0.38, 0.34)[v]
    n = count(d)
    heave = thump(rng, d, (115, 100)[v], 45, 0.08)
    soil = mul(biquad(noise(rng, d), "low", 220), env(n, 0.01, 0.08))
    splinters, t, amp = [0.0] * n, 0.004, 1.0
    for _ in range((7, 6)[v]):
        splinters = [a + b for a, b in zip(splinters, stick_slip(rng, d, lambda _: 900, t, t + 0.008, 0.5, amp))]
        t += 0.012 + 0.023 * rng.random()
        amp *= 0.8
    modes = ([(780, 8, 1.0), (1450, 10, 0.7), (2300, 9, 0.4)], [(690, 8, 1.0), (1300, 10, 0.7), (2550, 9, 0.35)])[v]
    cracks = bands(splinters, modes)
    pull = mul(bands(stick_slip(rng, d, lambda t: 80 + 600 * t, 0.02, 0.11), [(420, 9, 1.0), (900, 10, 0.6)]),
               bump(n, 0.015, 0.12))
    grit = biquad(grains(rng, d, lambda t: 80 + 1200 * math.exp(-t / 0.07), 0.0025), "band", 1300, 0.8)
    fall = mul(biquad(noise(rng, d), "low", 900), env(n, 0.02, 0.1, start=0.03))
    return mix(n, (heave, 0.7), (soil, 0.4), (cracks, 1.0), (pull, 0.5), (grit, 0.7), (fall, 0.35))


# cue -> (sound, peak level of full scale). Strikes sit with the slash/hit takes (~0.78), windups under them.
CUES = {
    "imp_windup": (imp_windup, 0.62),
    "imp_claw": (imp_claw, 0.72),
    "wolf_growl": (wolf_growl, 0.66),
    "wolf_bite": (wolf_bite, 0.76),
    "wolf_pounce": (wolf_pounce, 0.66),
    "wolf_land": (wolf_land, 0.72),
    "mire_charge": (mire_charge, 0.62),
    "mire_erupt": (mire_erupt, 0.78),
}
FADE_IN, FADE_OUT = 0.002, 0.02   # seconds, raised cosine: click-free edges


def finish(x, level):
    """No DC or rumble, no fizz above 7 kHz, a little soft saturation for body, faded edges, peak = level."""
    x = biquad(biquad(x, "high", 40), "low", 7000)
    x = [math.tanh(1.5 * v) for v in norm(x)]
    n, fi, fo = len(x), count(FADE_IN), count(FADE_OUT)
    for i in range(fi):
        x[i] *= 0.5 - 0.5 * math.cos(math.pi * i / fi)
    for i in range(fo):
        x[n - 1 - i] *= 0.5 - 0.5 * math.cos(math.pi * i / fo)
    return array.array("h", (round(v) for v in norm(x, level * 32767)))


def render():
    """name -> PCM16 bytes, for every take."""
    takes = {}
    for cue, (sound, level) in CUES.items():
        for v in (0, 1):
            name = cue if v == 0 else f"{cue}_2"
            takes[name] = finish(sound(random.Random(name), v), level).tobytes()
    return takes


def wav_bytes(path):
    with wave.open(str(path)) as w:
        if (w.getframerate(), w.getnchannels(), w.getsampwidth()) != (RATE, 1, 2):
            return None
        return w.readframes(w.getnframes())


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
        print("forest sfx match the generator")


if __name__ == "__main__":
    main()
