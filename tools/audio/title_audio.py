"""Render the original title score and menu cues; no service calls or game runtime dependencies.

Run: python3 tools/audio/title_audio.py
Music rendering uses the locally installed libfluidsynth and TimGM6mb.sf2. The score is
16 bars at 80 BPM: harp, quiet strings, flute and cello. Only four named WAVs are written.
"""
import array
import ctypes as c
import ctypes.util
import math
from pathlib import Path
import sys
import wave

RATE = 22_050
OUT = Path(__file__).resolve().parents[2] / "assets/audio"
SOUNDFONT = Path("/usr/share/sounds/sf2/TimGM6mb.sf2")
BEAT = .75
BARS = 16


def write(name, samples, peak, edges=True):
    samples = list(samples)
    high = max(abs(n) for n in samples)
    assert high > 0 and all(math.isfinite(n) for n in samples)
    gain = peak * 32767 / high
    fade = int(.008 * RATE)
    pcm = array.array("h", (round(n * gain * (min(1, i / fade, (len(samples)-1-i) / fade)
                          if edges else 1)) for i, n in enumerate(samples)))
    assert max(abs(n) for n in pcm) < 32767
    if edges:
        assert pcm[0] == pcm[-1] == 0
    rms = math.sqrt(sum(n*n for n in pcm) / len(pcm)) / 32768
    if sys.byteorder != "little":
        pcm.byteswap()
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / (name + ".wav")), "wb") as output:
        output.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        output.writeframes(pcm.tobytes())
    print(f"{name}: {len(samples)/RATE:.2f}s, peak {peak:.2f}, RMS {rms:.3f}")


def score():
    events = []

    def note(beat, length, channel, pitch, velocity):
        events.append((round(beat * BEAT * RATE), 1, channel, pitch, velocity))
        events.append((round((beat+length) * BEAT * RATE), 0, channel, pitch, 0))

    # Dm(add9), Bbmaj7, Fmaj7, Csus2, Gm7, Bbmaj7, A7sus4, A7; the second phrase answers the first.
    chords = [(50, 57, 60, 65), (46, 53, 57, 62), (41, 48, 52, 57), (48, 55, 62, 64),
              (43, 50, 53, 58), (46, 53, 57, 62), (45, 52, 55, 62), (45, 52, 55, 61)]
    melody = [(0, 74, 1.6), (2, 69, 1.2), (4, 77, 2.5), (7, 76, .75),
              (8, 74, 2.5), (11, 72, .75), (12, 69, 2.8),
              (16, 70, 1.5), (18, 69, 1.5), (20, 65, 2.5), (23, 67, .75),
              (24, 69, 3), (28, 73, 1.5), (30, 76, 1.25),
              (32, 77, 2.5), (35, 76, .75), (36, 74, 2.5), (39, 69, .75),
              (40, 72, 1.5), (42, 69, 1.5), (44, 67, 2.8),
              (48, 70, 1.5), (50, 74, 1.5), (52, 77, 2.5), (55, 76, .75),
              (56, 74, 3), (60, 73, 2.5)]
    for bar in range(BARS):
        chord = chords[bar % len(chords)]
        for pitch in chord:
            note(bar*4, 3.85, 1, pitch, 37)
        note(bar*4, 3.5, 3, chord[0]-12, 42)
        for i, tone in enumerate((0, 2, 1, 3, 2, 1)):
            note(bar*4 + i*.5, 1.25, 0, chord[tone]+12, 55 if i == 0 else 43+(i%3)*4)
    for beat, pitch, length in melody:
        note(beat, length, 2, pitch, 51)
    return sorted(events)


def music():
    library = ctypes.util.find_library("fluidsynth")
    if not library or not SOUNDFONT.exists():
        raise SystemExit("Title rendering needs installed libfluidsynth and TimGM6mb.sf2; existing WAVs are untouched.")
    lib = c.CDLL(library)

    def api(name, result, *args):
        function = getattr(lib, name)
        function.restype, function.argtypes = result, args
        return function

    settings = api("new_fluid_settings", c.c_void_p)()
    setnum = api("fluid_settings_setnum", c.c_int, c.c_void_p, c.c_char_p, c.c_double)
    setnum(settings, b"synth.sample-rate", RATE)
    setnum(settings, b"synth.gain", .6)
    setnum(settings, b"synth.reverb.room-size", .65)
    setnum(settings, b"synth.reverb.damp", .4)
    setnum(settings, b"synth.reverb.level", .28)
    synth = api("new_fluid_synth", c.c_void_p, c.c_void_p)(settings)
    load = api("fluid_synth_sfload", c.c_int, c.c_void_p, c.c_char_p, c.c_int)
    program = api("fluid_synth_program_change", c.c_int, c.c_void_p, c.c_int, c.c_int)
    on = api("fluid_synth_noteon", c.c_int, c.c_void_p, c.c_int, c.c_int, c.c_int)
    off = api("fluid_synth_noteoff", c.c_int, c.c_void_p, c.c_int, c.c_int)
    render = api("fluid_synth_write_s16", c.c_int, c.c_void_p, c.c_int,
                 c.c_void_p, c.c_int, c.c_int, c.c_void_p, c.c_int, c.c_int)
    try:
        if load(synth, str(SOUNDFONT).encode(), 1) < 0:
            raise RuntimeError("Could not load the title instruments")
        for channel, instrument in enumerate((46, 48, 73, 42)):
            program(synth, channel, instrument)
        period = round(BARS * 4 * BEAT * RATE)
        events = [(at+cycle*period, active, channel, pitch, velocity)
                  for cycle in range(3) for at, active, channel, pitch, velocity in score()]
        events.append((3*period, 0, 0, 0, 0))
        samples = array.array("h")
        at = 0
        for when, active, channel, pitch, velocity in sorted(events):
            count = when - at
            if count:
                left, right = (c.c_short * count)(), (c.c_short * count)()
                if render(synth, count, left, 0, 1, right, 0, 1) < 0:
                    raise RuntimeError("Title music rendering failed")
                samples.extend(round((l+r)/2) for l, r in zip(left, right))
            if active:
                on(synth, channel, pitch, velocity)
            elif pitch:
                off(synth, channel, pitch)
            at = when
        result = list(samples[period:2*period])
        # Match the ending to the lead-in of this settled cycle, preserving the 48-second meter.
        seam = round(.06 * RATE)
        for i in range(seam):
            blend = (i+1)/seam
            result[-seam+i] = result[-seam+i]*(1-blend) + samples[period-seam+i]*blend
        write("title_music", result, .78, edges=False)
    finally:
        api("delete_fluid_synth", None, c.c_void_p)(synth)
        api("delete_fluid_settings", None, c.c_void_p)(settings)


def chime(name, notes, duration, decay, peak):
    samples = [0.0] * round(duration * RATE)
    for pitch, start, level in notes:
        frequency = 440 * 2 ** ((pitch-69)/12)
        for i in range(round(start*RATE), len(samples)):
            t = i/RATE-start
            envelope = (1-math.exp(-t/.004)) * math.exp(-t/decay)
            ring = (math.sin(2*math.pi*frequency*t) + .3*math.sin(2*math.pi*frequency*2.01*t)
                    + .08*math.sin(2*math.pi*frequency*3.98*t))
            samples[i] += ring * envelope * level
    write(name, samples, peak)


if __name__ == "__main__":
    music()
    chime("ui_move", [(81, 0, 1)], .10, .022, .16)
    chime("ui_confirm", [(74, 0, 1), (81, .06, .6)], .32, .065, .24)
    chime("journey_start", [(62, 0, .6), (69, .08, .7), (74, .18, 1), (77, .29, .65), (81, .4, .4)],
          1.65, .27, .44)
