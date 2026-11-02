#!/usr/bin/env python3
"""Original 1.15s devouring cast: opening tear, inward rush, low collapse. No anime recording."""
import math
import random
import sys

from generate_forest_sfx import OUT, RATE, bands, biquad, count, finish, grains, mix, noise, norm, thump, voice, wav_bytes


def render():
    rng = random.Random(281028)
    duration = 1.15
    n = count(duration)
    air = norm(biquad(noise(rng, duration), "band", lambda t: 280 + 1800 * (t / duration) ** 1.7, 1.1))
    body = norm(bands(voice(rng, duration, lambda t: 95 - 30 * t / duration,
                           jitter=.035, sub=.3, breath=.25), [(180, 1.3, 1), (520, 2, .45)]))
    grit = norm(biquad(grains(rng, duration, lambda t: 40 + 260 * t, .002), "band", 1350, .9))
    rush = []
    for i in range(n):
        t = i / RATE
        swell = min(1, t / .18) * max(0, min(1, (1.04 - t) / .10))
        rotation = .8 + .2 * math.sin(2 * math.pi * (5 * t + 5 * t * t))
        rush.append((air[i] * .65 * rotation + body[i] * .45 + grit[i] * .13) * swell)
    tear = norm(biquad(noise(rng, duration), "band", 1900, .7))
    tear = [v * min(1, i / (RATE * .008)) * math.exp(-i / (RATE * .055)) for i, v in enumerate(tear)]
    opening = thump(rng, duration, 240, 80, .045)
    low = thump(rng, duration, 230, 54, .06, start=.98)
    snap = norm(biquad(noise(rng, duration), "band", 1100, .8))
    snap = [v * (min(1, max(0, (i / RATE - .98) / .004))
                 * math.exp(-max(0, i / RATE - .98) / .025)) for i, v in enumerate(snap)]
    return finish(mix(n, (rush, 1), (tear, .35), (opening, .5), (low, .9), (snap, .3)), .72).tobytes()


if __name__ == "__main__":
    import struct
    import wave

    data = render()
    assert abs(len(data) / 2 / RATE - 1.15) < .001
    pcm = struct.unpack("<" + "h" * (len(data) // 2), data)
    assert max(abs(x) for x in pcm) < 30000
    assert abs(pcm[0]) < 128 and abs(pcm[-1]) < 128
    path = OUT / "beelzebub.wav"
    if "--check" in sys.argv:
        assert wav_bytes(path) == data, "Beelzebub sound differs from deterministic source"
    else:
        with wave.open(str(path), "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(RATE)
            wav.writeframes(data)
    print("Beelzebub sound: 1.15s, mono PCM16/22050Hz, faded edges; verified")
