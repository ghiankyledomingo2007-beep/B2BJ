"""Generate the game's sound effects with ElevenLabs and finish them for GameAudio.

  python3 tools/audio/generate_sfx.py [--force] [--backend elevenlabs|unlocksfx] [cue ...]
      (--force regenerates the named cues only; default backend elevenlabs)

Writes assets/audio/<cue>.wav, <cue>_2.wav, ... (22050 Hz, mono, 16-bit: GameAudio.FORMAT, no
conversion at runtime). GameAudio picks a take at random when a cue has several, so repeated
hits do not sound like one sample. Existing files are kept unless --force, so a rerun only costs
the cues that are missing. Key: ~/.config/elevenlabs/api-key (never copied anywhere).
Cost: ElevenLabs 40 credits per requested second; UnlockSFX 1 credit per sound (key at
~/.config/unlocksfx/api-key; its API is undocumented, shape read from their Unity plugin). Every take is trimmed, normalised to LEVEL and given
click-free edges here, so loudness balance between cues lives in this table, not in Java.
"""
import array
import json
import math
import struct
import sys
import time
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
OUT = REPO / "assets/audio"
KEY = Path.home() / ".config/elevenlabs/api-key"
RATE = 22_050
# The API honours the pcm_* rate but the stream is STEREO, interleaved int16 (measured 2026-09-22: a
# 0.5 s request is 0.5 x 22050 x 2 samples, and the two channels differ). Read as mono it plays at half
# speed an octave down, which is what "metallic, clunky, echoing" was.
API_RATE = 22_050
URL = "https://api.elevenlabs.io/v1/sound-generation?output_format=pcm_22050"
# Anime SFX reference (owner, 2026-09-23): 0.5-1.3 s long, 0.3-0.65 s ringing tail, bright shing/shimmer.
STYLE = ", anime style video game sound effect, stylized, clean, no music"
INFLUENCE = 0.6                          # 0.45 let the model ignore "no reverb"; 0.7 made whooshes literal hiss
FADE_IN, FADE_OUT = 0.003, 0.03          # seconds; GameAudioTest wants click-free edges
GATE = 0.06                              # fraction of the peak that counts as silence when trimming
# Loudness tiers: hits loudest, the player's own actions under them, then cues, then foley and beds.
TIERS = {"impact": 1.0, "action": .8, "ui": .6, "foley": .35, "bed": .2}
# Family -> longest tail kept after the peak, seconds. Pixel games read as toys when sounds ring.
TAILS = {"swing": 0.65, "hit": 0.5, "water": 0.6, "cue": 1.2, "big": 2.0, "loop": None}
# Family -> low-pass cutoff, Hz. Generated whooshes come back as near-white hiss; a sword through air is
# mostly air, so the swing family is softened. Everything else keeps its top end.
SOFTEN = {"water": 6000}                   # anime sword sounds keep their top end; water magic stays a touch softer
LOOP_CROSSFADE = 0.25                    # seconds blended from the end into the start so a loop has no seam

# cue -> (prompt, requested seconds, takes, tier, family)
# The blade is ichor and ice: glassy and cold, never steel. Slime: wet and elastic. Warden: stone and sub-bass.
# Layer families (hit_thud, shatter, ice_crack, sub_boom) are mixed under a cue by GameAudio.Cue.layer.
CUES = {
    # ---- blade form: the cut sound plays on the contact frame (GameAudio.play with a delay)
    "slash": ("anime sword slash, fast whoosh with a bright metallic shing and a short magical shimmer tail", 0.8, 3, "action", "swing"),
    "thrust": ("anime sword thrust, sharp fast whip forward with a bright shing", 0.7, 2, "action", "swing"),
    "slash_heavy": ("anime heavy overhead sword swing, big deep whoosh with a long bright shing and shimmer tail", 1.0, 2, "action", "swing"),
    "cleave": ("anime charged sword cleave, powerful wide whoosh with an icy energy shing and a magical shimmer tail", 1.0, 2, "action", "swing"),
    "dash_slash": ("anime dash slash, rushing wind whoosh with a bright blade shing and shimmer", 0.9, 2, "action", "swing"),
    "spin_cleave": ("anime spinning sword attack, two rapid wide whooshes with bright energy shing and a shimmer tail", 1.2, 2, "action", "cue"),
    "ice_crack": ("sharp ice cracking snap, bright brittle crackle, one hit, very short", 0.5, 2, "impact", "hit"),
    "counter": ("anime parry clang then an instant rising counter slash with a bright shing and shimmer", 0.9, 2, "action", "swing"),
    "guard": ("anime sword raised to guard, bright metallic shing with a short ring", 0.6, 2, "ui", "swing"),
    "cast": ("anime sword swing releasing an ice energy wave, whoosh with a bright crystalline magical release and shimmer tail", 1.0, 2, "action", "swing"),
    "crescent_impact": ("anime ice energy crescent bursting on impact, bright glassy shatter with a shimmer tail", 0.7, 2, "impact", "hit"),
    "charge_ready": ("short bright resonant glassy ping, energy fully charged, game notification", 0.6, 1, "ui", "cue"),
    "charge_loop": ("seamless loop, low cold glassy hum of gathering ice energy, steady, no start or end", 1.5, 1, "foley", "loop"),
    # one hit sound for every enemy in both forms: simple and satisfying (owner, 2026-09-22)
    "enemy_hit": ("sword slicing into a wet gelatinous creature, sharp wet cut squelch, short", 0.5, 3, "impact", "hit"),
    "hit_thud": ("dull close body thud, one short punchy impact, no ring", 0.5, 3, "impact", "hit"),
    "kill": ("small slimy creature bursting apart, wet squelchy pop with a splatter, short", 0.6, 2, "impact", "hit"),
    "shatter": ("short crystalline ice shatter, bright, one burst", 0.5, 2, "impact", "hit"),
    "footstep": ("single quick light footstep of a boot on packed dirt with a little stone grit, very short, dry", 0.5, 4, "foley", "hit"),
    "dash": ("anime quick dash, fast air whoosh with a light energy shimmer", 0.6, 2, "action", "swing"),
    "hurt": ("the hero taking a hit, dull body impact thud with a short wet slime squish", 0.5, 2, "impact", "hit"),
    "heartbeat": ("single slow low heartbeat, one double thump, deep, muffled", 0.7, 1, "foley", "cue"),
    "transform": ("slime morphing into an armored swordsman, wet goo stretching then hardening into glassy ice with a bright ring", 1.5, 1, "action", "big"),
    "revert": ("glassy armored form dissolving back into liquid slime, bright ring fading into a wet goo splash", 1.2, 1, "action", "big"),
    # ---- slime form
    "blob_attack": ("anime water magic blade thrown, liquid whoosh with a bubbly magical shimmer tail", 0.8, 3, "action", "water"),
    "water_splash": ("anime water magic dissolving, bubbly liquid burst with a soft shimmer", 0.7, 2, "action", "water"),
    "finisher_release": ("anime big water magic spell released, deep surging liquid whoosh with a rising magical shimmer", 1.0, 1, "action", "big"),
    "finisher_impact": ("heavy water magic blast impact, deep wet boom with a crystalline sparkle burst", 0.8, 1, "impact", "big"),
    "tide_release": ("anime tidal wave spell summoned, deep rushing water surge with a magical shimmer", 1.1, 1, "action", "big"),
    "tide_impact": ("enchanted wave crashing down, heavy magical water impact with sparkle spray", 1.0, 1, "impact", "big"),
    "membrane_break": ("water shield bursting, deep wet elastic pop with a bubbly splash, no chime, no glass, short", 0.6, 2, "impact", "water"),
    "spit_shot": ("small goo bolt launched, soft wet bubbly pop, no chime, short", 0.5, 2, "action", "water"),
    "spit_impact": ("goo bolt splatting, soft wet splat with a bubbly fizzle, no chime, short", 0.5, 2, "impact", "water"),
    "absorbed": ("slime absorbing a soul, wet gulp with a warm magical chime, short", 0.7, 2, "action", "cue"),
    "pickup": ("bright small collectible pickup, soft glassy pop with a sparkle, short", 0.5, 2, "ui", "cue"),
    "healed": ("gentle healing chime, warm soft glassy shimmer rising, short", 0.9, 1, "ui", "cue"),
    "trait_gained": ("trait unlocked, short triumphant glassy magical chord swell", 1.2, 1, "ui", "cue"),
    # ---- the moments that had no sound (owner's inventory, 2026-09-23)
    "slime_move": ("anime slime blob hopping along, tiny soft wet squelch, very short", 0.5, 4, "foley", "hit"),
    "slime_dash": ("anime slime dash, quick wet stretchy burst of goo with a light magical shimmer, short", 0.6, 2, "action", "water"),
    "death_blade": ("anime hero collapsing to one knee, armor and sword clatter on stone, low defeated tone", 1.2, 1, "impact", "big"),
    "death_slime": ("anime slime melting away, long wet gooey deflating splat with a sad low tone", 1.2, 1, "impact", "big"),
    "blade_fades": ("anime warning: magic armor beginning to fade, low pulsing crystalline tone with a falling shimmer", 1.0, 1, "ui", "cue"),
    "denied": ("anime UI denied, short dull low double blip, muted, no sparkle", 0.5, 1, "ui", "hit"),
    "checkpoint": ("anime checkpoint saved at a campfire, warm rising magical chime with a soft fire crackle", 1.2, 1, "ui", "cue"),
    "upgrade": ("anime upgrade purchased, bright ascending three-note magical chime", 1.0, 1, "ui", "cue"),
    "ui_open": ("anime menu open, soft short glassy swipe upward", 0.5, 1, "ui", "hit"),
    "ui_close": ("anime menu close, soft short glassy swipe downward", 0.5, 1, "ui", "hit"),
    "tide_dissipate": ("anime water wave dissolving into foam and sparkles, soft fizzle", 0.8, 1, "foley", "water"),
    # ---- the warden, the world and the end
    "sub_boom": ("deep sub-bass boom, 40 hertz thump, one hit, short, no high end", 0.6, 1, "ui", "hit"),
    "guardian_awakened": ("giant stone guardian awakening, deep grinding rumble of stone and a low monstrous groan", 2.0, 1, "impact", "big"),
    "guardian_slam": ("colossal stone fist slamming the ground, deep boom with falling rock debris, short", 0.9, 2, "impact", "big"),
    "ambience_rain": ("seamless loop, soft steady rain on stone ruins with distant wind and dripping water, calm, no start or end", 10.0, 1, "bed", "loop"),
    "victory": ("short victory fanfare, triumphant bright brass with a glassy magical shimmer", 2.0, 1, "ui", "big"),
}
LOOPS = {"charge_loop", "ambience_rain"}


UNLOCK_KEY = Path.home() / ".config/unlocksfx/api-key"
UNLOCK_URL = "https://www.unlocksfx.com/api/v1"
BACKEND = "elevenlabs"


def request(prompt, seconds, loop):
    if BACKEND == "unlocksfx":
        return request_unlocksfx(prompt, seconds, loop)
    return request_elevenlabs(prompt, seconds, loop)


def request_unlocksfx(prompt, seconds, loop):
    """POST /generate -> signed downloadUrl -> WAV bytes -> the stereo PCM shape finish() expects."""
    body = json.dumps({"originalPrompt": prompt + STYLE, "durationSeconds": seconds, "loopable": loop,
                       "outputFormat": "wav", "promptInfluence": INFLUENCE}).encode()
    headers = {"Authorization": "Bearer " + UNLOCK_KEY.read_text().strip(), "Content-Type": "application/json"}
    with urllib.request.urlopen(urllib.request.Request(UNLOCK_URL + "/generate", body, headers), timeout=180) as r:
        meta = json.load(r)
    print(f"  unlocksfx credits left: {meta.get('creditsRemaining')}", flush=True)
    with urllib.request.urlopen(meta["downloadUrl"], timeout=180) as r:
        wav = r.read()
    import subprocess
    done = subprocess.run(["ffmpeg", "-v", "error", "-i", "pipe:0", "-ac", "2", "-ar", str(API_RATE), "-f", "s16le", "-"],
                          input=wav, capture_output=True, check=True)
    return done.stdout


def request_elevenlabs(prompt, seconds, loop):
    fields = {"text": prompt + STYLE, "duration_seconds": seconds, "prompt_influence": INFLUENCE}
    if loop:
        fields["loop"] = True
    body = json.dumps(fields).encode()
    req = urllib.request.Request(URL, body, {"xi-api-key": KEY.read_text().strip(), "Content-Type": "application/json"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=180) as r:
                pcm = r.read()
        except urllib.error.HTTPError as e:
            if e.code == 429 or e.code >= 500:
                time.sleep(5 * (attempt + 1))
                continue
            detail = e.read()[:300]
            if loop and b"loop" in detail:               # older API: no loop field, loop it in Java instead
                return request(prompt, seconds, False)
            raise SystemExit(f"{prompt[:40]}: HTTP {e.code} {detail}")
        got = len(pcm) / 4 / API_RATE                  # 4 bytes per stereo frame
        if abs(got - seconds) > 0.1 * seconds + 0.05:    # the rate trap: check this before trusting any format change
            print(f"  warning: asked {seconds}s, got {got:.2f}s of stereo at {API_RATE} Hz", flush=True)
        return pcm
    raise SystemExit("ElevenLabs kept refusing: " + prompt[:40])


def finish(pcm, level, tail, family):
    """Trim to the sound, cap the tail, normalise to the tier, give it click-free edges.
    A loop (tail None) keeps its full length and gets a short equal fade at both ends so it can repeat."""
    a = array.array("h")
    a.frombytes(pcm[: len(pcm) // 2 * 2])
    a = mono(a)                         # interleaved stereo -> mono at the same rate
    peak = max(1, max(abs(x) for x in a))
    if tail is not None:
        loud = [i for i, x in enumerate(a) if abs(x) > peak * GATE]
        lead = max(0, loud[0] - int(0.005 * RATE))
        top = max(range(len(a)), key=lambda i: abs(a[i]))
        end = min(loud[-1] + int(FADE_OUT * RATE), top + int(tail * RATE))
        a = a[lead: max(end, lead + int(0.05 * RATE))]
    if family in SOFTEN:
        a = lowpass(a, SOFTEN[family])
        peak = max(1, max(abs(x) for x in a))
    if tail is None and len(a) > 2 * int(LOOP_CROSSFADE * RATE):
        a = seamless(a)
    gain = level * 32_000 / peak
    n = len(a)
    fi, fo = int(FADE_IN * RATE), int(FADE_OUT * RATE)
    loop = tail is None                 # a loop must meet itself: no edge fades at all
    out = array.array("h")
    for i, x in enumerate(a):
        env = 1.0 if loop else min(1.0, i / fi, (n - 1 - i) / fo)
        out.append(int(max(-32_767, min(32_767, x * gain * max(env, 0.0)))))
    return out


def mono(a):
    """Average each L/R pair into one sample."""
    return array.array("h", [(a[i] + a[i + 1]) // 2 for i in range(0, len(a) - 1, 2)])


def lowpass(a, cutoff):
    """One-pole low-pass, run forwards then backwards so it adds no delay."""
    k = 1 - math.exp(-2 * math.pi * cutoff / RATE)
    out = array.array("h", a)
    for indices in (range(len(a)), range(len(a) - 1, -1, -1)):
        y = 0.0
        for i in indices:
            y += k * (out[i] - y)
            out[i] = int(y)
    return out


def seamless(a):
    """Blend the tail into the head so the loop point is inaudible; the head's fade-in is then unnecessary."""
    n = int(LOOP_CROSSFADE * RATE)
    body = a[: len(a) - n]
    for i in range(n):
        t = i / n
        body[i] = int(body[i] * t + a[len(a) - n + i] * (1 - t))
    return body


def write_wav(path, samples):
    data = samples.tobytes()
    header = b"RIFF" + struct.pack("<I", 36 + len(data)) + b"WAVEfmt " + struct.pack(
        "<IHHIIHH", 16, 1, 1, RATE, RATE * 2, 2, 16) + b"data" + struct.pack("<I", len(data))
    path.write_bytes(header + data)


def main():
    global BACKEND
    force = "--force" in sys.argv
    if "--backend" in sys.argv:
        BACKEND = sys.argv[sys.argv.index("--backend") + 1]
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if BACKEND in args:
        args.remove(BACKEND)
    wanted = args or list(CUES)
    OUT.mkdir(parents=True, exist_ok=True)
    spent = 0.0
    for cue in wanted:
        prompt, seconds, takes, tier, family = CUES[cue]
        if force:
            for stale in OUT.glob(f"{cue}.wav"):
                stale.unlink()
            for stale in OUT.glob(f"{cue}_[0-9].wav"):
                stale.unlink()
        for take in range(1, takes + 1):
            path = OUT / (f"{cue}.wav" if take == 1 else f"{cue}_{take}.wav")
            if path.exists():
                continue
            samples = finish(request(prompt, seconds, cue in LOOPS), TIERS[tier], TAILS[family], family)
            write_wav(path, samples)
            spent += seconds
            print(f"{path.name}: {len(samples) / RATE:.2f}s", flush=True)
    print(f"done, ~{spent * 40:.0f} credits")


if __name__ == "__main__":
    main()
