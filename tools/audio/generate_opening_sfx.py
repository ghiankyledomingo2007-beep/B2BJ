#!/usr/bin/env python3
"""Original opening foley and magic, using the game's existing DSP. Stdlib only; --check is deterministic."""
import math
import random
import sys
import wave

from generate_forest_sfx import OUT, RATE, biquad, bump, count, finish, noise, norm, thump, wav_bytes
from generate_combat_sfx import bubble, glint, knock, whoosh


def add(target, sound, gain, start=0):
    offset = count(start)
    for i, sample in enumerate(norm(sound, gain)[:max(0, len(target) - offset)]):
        target[offset + i] += sample


def tone(seconds, frequency):
    return [math.sin(2 * math.pi * frequency * i / RATE) for i in range(count(seconds))]


def office(rng):
    d = 10; out = [0.] * count(d)
    add(out, biquad(noise(rng, d), "low", 380), .035)
    add(out, tone(d, 150), .012)
    at = .15
    while at < 3.25:
        click = knock(count(.065), .001, [(rng.uniform(600, 1000), 2, 1), (1950, 2.5, .2)])
        add(out, click, rng.uniform(.12, .25), at)
        at += rng.uniform(.075, .23)
    add(out, knock(count(.08), .001, [(720, 2, 1), (1800, 2, .15)]), .11, 3.9)
    return [sample * min(1, max(0, (8.2 - i / RATE) / 1.2)) for i, sample in enumerate(out)]


def heartbeat(rng):
    out = [0.] * count(4)
    for at, strength in [(0.1, 1), (1.2, .72), (2.7, .32)]:
        add(out, thump(rng, .36, 150, 65, .09), strength, at)
        add(out, thump(rng, .24, 125, 80, .055), strength * .5, at + .17)
    return out


def collapse(rng):
    n = count(.8); out = [0.] * n
    add(out, knock(n, .06, [(135, 3, 1), (310, 3, .6), (720, 2, .2)]), .7)
    cloth = biquad(noise(rng, .8), "band", 650, .7)
    add(out, [v * e for v, e in zip(cloth, bump(n, .01, .42))], .25)
    add(out, glint(n, .14, [(1100, .025, 1), (1900, .018, .3)]), .045)
    return out


def awaken(rng):
    d = 3.8; n = count(d); out = [0.] * n
    shape = bump(n, .02, d - .04)
    for frequency, gain in [(146.83, .65), (220.12, .24), (293.66, .15)]:
        add(out, [v * e for v, e in zip(tone(d, frequency), shape)], gain)
    add(out, whoosh(rng, d, 1.8, 1.1, .9, 180, 1100, .7, .08), .32)
    add(out, glint(n, 1.55, [(587.33, .55, 1), (881.2, .38, .55), (1618, .22, .15)]), .32)
    return out


def form(rng):
    d = 3.5; n = count(d); out = [0.] * n
    add(out, whoosh(rng, d, 1.6, 1.1, .95, 250, 1300, .65, .06), .3)
    pearls = [0.] * n
    for i in range(34):
        bubble(pearls, .08 + i * .085, rng.uniform(280, 950), rng.uniform(.018, .04), rise=.35)
    add(out, pearls, .45)
    add(out, glint(n, 2.65, [(293.66, .3, 1), (440, .25, .4), (587.33, .2, .2)]), .2)
    return out


def chime(rng, notes, seconds=.95):
    n = count(seconds); out = [0.] * n; ring = seconds * .36
    hiss = biquad(noise(rng, seconds), "band", 4500, 1.4)
    for i, frequency in enumerate(notes):
        at = .025 + i * .19; level = .55 if i == 0 else .5
        body = [(frequency, ring, 1), (frequency + 1.3, ring * .8, .3),
                (frequency * 2, ring * .4, .15)]
        add(out, glint(n, at, body + [(frequency * 2.76, .07, .09),
                                      (frequency * 5.4, .025, .03)]), level)
        add(out, glint(n, at + .0409, body), level * .2)
        add(out, [v * e for v, e in zip(hiss, bump(n, at - .001, at + .006))], level * .12)
        add(out, [v * e for v, e in zip(hiss, bump(n, at + .004, at + .16))], level * .06)
    return out

def human(rng):
    d = 1.5; out = [0.] * count(d)
    add(out, whoosh(rng, d, .45, .35, .45, 500, 1900, .6, .06), .25)
    add(out, glint(len(out), .22, [(740, .3, 1), (880, .28, .45), (1480, .18, .1)]), .5)
    return out


def wake(rng):
    d = 1.2; out = [0.] * count(d); drops = [0.] * count(d)
    for i in range(7):
        bubble(drops, .06 + i * .035, rng.uniform(180, 600), .045, rise=.2)
    add(out, drops, .4)
    add(out, whoosh(rng, d, .25, .18, .4, 200, 1000, .7, .05), .3)
    return out


CUES = {
    "opening_office": (office, .35), "opening_heartbeat": (heartbeat, .45),
    "opening_collapse": (collapse, .55), "opening_awaken": (awaken, .7), "opening_form": (form, .65),
    "opening_skill_analysis": (lambda r: chime(r, (587.33, 880)), .6),
    "opening_skill_body": (lambda r: chime(r, (392, 587.33)), .6),
    "opening_skill_absorption": (lambda r: chime(r, (293.66, 220)), .6),
    "opening_skill_regeneration": (lambda r: chime(r, (440, 659.25), 1.05), .6),
    "opening_human": (human, .6), "opening_wake": (wake, .65),
}


def render():
    result = {}
    for name, (synth, level) in CUES.items():
        pcm = finish(synth(random.Random(name)), level)
        assert RATE * .7 < len(pcm) <= RATE * 10
        assert abs(pcm[0]) <= 1 and abs(pcm[-1]) <= 1
        assert .1 * 32767 < max(map(abs, pcm)) <= .71 * 32767
        result[name] = pcm.tobytes()
    return result


if __name__ == "__main__":
    for name, pcm in render().items():
        path = OUT / f"{name}.wav"
        if "--check" in sys.argv:
            assert path.exists() and wav_bytes(path) == pcm, f"stale opening sound: {path}"
        else:
            with wave.open(str(path), "wb") as target:
                target.setparams((1, 2, RATE, 0, "NONE", "not compressed"));target.writeframes(pcm)
        print(f"{name}: {len(pcm) / 2 / RATE:.2f}s verified")
