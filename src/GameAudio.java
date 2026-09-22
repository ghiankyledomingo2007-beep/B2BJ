import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class GameAudio {
    /**
     * Every cue has a packaged recording, assets/audio/&lt;name&gt;.wav plus _2, _3... takes
     * (tools/audio/generate_sfx.py), and the synthesized chirp it had before as the fallback
     * when the file is missing. The numbers below only describe that fallback.
     */
    public enum Cue {
        DASH(0.12, 240, 90, 0.20, true),
        SLASH(0.16, 720, 180, 0.22, false),
        THRUST(0.14, 640, 260, 0.22, false),
        SLASH_HEAVY(0.22, 520, 120, 0.24, false, "ice_crack", true),
        CLEAVE(0.26, 480, 110, 0.26, true, "ice_crack", true),
        DASH_SLASH(0.18, 700, 210, 0.22, true),
        SPIN_CLEAVE(0.34, 500, 140, 0.26, true, "ice_crack", true),
        COUNTER(0.20, 900, 300, 0.24, true),
        GUARD(0.14, 1100, 900, 0.18, false),
        CAST(0.20, 660, 330, 0.22, false),
        CRESCENT_IMPACT(0.14, 820, 240, 0.20, true),
        CHARGE_READY(0.24, 880, 1320, 0.16, false, null, false),
        ENEMY_HIT(0.10, 210, 70, 0.30, true, "hit_thud", true),
        KILL(0.16, 260, 60, 0.26, true, "shatter", true),
        FOOTSTEP(0.05, 140, 60, 0.10, true),
        REVERT(0.44, 900, 160, 0.20, false, null, false),
        SPIT_SHOT(0.12, 380, 220, 0.14, true, null, true, true),
        SPIT_IMPACT(0.12, 300, 90, 0.16, true, null, true, true),
        ABSORB(0.20, 300, 480, 0.16, true, null, true, true),
        ABSORBED(0.18, 360, 200, 0.16, false, null, true, true),
        HEALED(0.30, 520, 780, 0.14, false, null, false),
        BLOB_ATTACK(0.18, 420, 160, 0.16, true, null, true, true),
        WATER_SPLASH(0.20, 260, 90, 0.16, true, null, true, true),
        HURT(0.28, 190, 70, 0.24, false, "hit_thud", true),
        PICKUP(0.22, 420, 980, 0.20, false),
        TRANSFORM(0.48, 180, 920, 0.22, false, null, false),
        GUARDIAN_SLAM(0.42, 92, 38, 0.32, true, "sub_boom", true),
        VICTORY(0.72, 330, 660, 0.22, false, null, false),
        TIDE_RELEASE(0.30, 130, 380, 0.23, true, null, true, true),
        TIDE_IMPACT(0.38, 220, 78, 0.24, true, "sub_boom", true, true),
        FINISHER_RELEASE(0.22, 560, 125, 0.18, true, null, true, true),
        FINISHER_IMPACT(0.24, 125, 48, 0.25, true, "sub_boom", true, true),
        MEMBRANE_BREAK(0.18, 310, 95, 0.18, true, null, true, true),
        TRAIT_GAINED(0.36, 330, 740, 0.17, false, null, false),
        GUARDIAN_AWAKENED(0.60, 60, 30, 0.30, true, null, false);

        private final double duration;
        private final double startFrequency;
        private final double endFrequency;
        private final double volume;
        private final boolean noise;
        /** A second recording family mixed under this cue's take (a thud under a cut, a sub boom under a slam). */
        final String layer;
        /** Whether each play may be detuned a little; chimes and fanfares must stay in tune. */
        final boolean pitched;
        /** The original synthesized chirp mixed under the recording: the slime form is magic, and the
         *  chirp is what made it sound like magic (owner, 2026-09-22). */
        final boolean chirp;

        Cue(double duration, double startFrequency, double endFrequency, double volume, boolean noise) {
            this(duration, startFrequency, endFrequency, volume, noise, null, true, false);
        }

        Cue(double duration, double startFrequency, double endFrequency,
                double volume, boolean noise, String layer, boolean pitched) {
            this(duration, startFrequency, endFrequency, volume, noise, layer, pitched, false);
        }

        Cue(double duration, double startFrequency, double endFrequency,
                double volume, boolean noise, String layer, boolean pitched, boolean chirp) {
            this.duration = duration;
            this.startFrequency = startFrequency;
            this.endFrequency = endFrequency;
            this.volume = volume;
            this.noise = noise;
            this.layer = layer;
            this.pitched = pitched;
            this.chirp = chirp;
        }
    }

    /** Sounds that run until stopped: the charge hum, the low-health heartbeat, the rain bed. */
    public enum Loop {
        CHARGE("charge_loop", 0.0), HEARTBEAT("heartbeat", 0.6), AMBIENCE("ambience_rain", 0.0);

        final String family;
        final double gapSeconds;          // silence between repeats (a heartbeat is not continuous)

        Loop(String family, double gapSeconds) { this.family = family; this.gapSeconds = gapSeconds; }
    }

    private static final int SAMPLE_RATE = 22_050;
    private static final AudioFormat FORMAT = new AudioFormat(
            SAMPLE_RATE, 16, 1, true, false);
    private static volatile boolean muted;
    private static volatile double masterVolume = 1, sfxVolume = 1, musicVolume = .25;
    private static volatile int musicBiome;
    private static volatile boolean musicBlade, musicActive;
    private static final double[][] MUSIC_NOTES = {
            {146.832, 220, 174.614, 196, 146.832, 174.614, 130.813, 164.814},
            {164.814, 246.942, 195.998, 293.665, 220, 195.998, 246.942, 164.814},
            {130.813, 155.563, 116.541, 195.998, 130.813, 174.614, 155.563, 116.541},
            {174.614, 246.942, 207.652, 349.228, 220, 293.665, 246.942, 195.998}
    };
    private GameAudio() { }

    public static void play(Cue cue) {
        play(cue, 0);
    }

    /** Queue a cue on the mixer, delaySeconds from now (sample-exact: a cut lands on its contact frame). */
    public static void play(Cue cue, double delaySeconds) {
        java.util.Objects.requireNonNull(cue, "cue");
        if (!Double.isFinite(delaySeconds) || delaySeconds < 0) throw new IllegalArgumentException("Invalid delay");
        if (muted || masterVolume == 0 || sfxVolume == 0) return;
        addVoice(new Voice(voicePcm(cue, take(cue), pitch(cue)), (int) Math.round(delaySeconds * SAMPLE_RATE), null, 0));
    }

    // ---------------------------------------------------------------- the mixer
    // One line, opened once and kept warm. Every sound is summed into it in small chunks, so a cue
    // starts within ~12 ms of being asked for. Opening a line per sound cost 20-200 ms here, and that
    // wandering delay is what made the attacks feel wrong however good the recordings were.

    private static final int CHUNK = 256;                 // samples per write: ~12 ms
    private static final int LINE_CHUNKS = 8;             // line buffer: ~93 ms of lead; 3 (35 ms) starved on frame hiccups and crackled
    private static volatile long underruns;               // times the line ran dry between writes (diagnostics)
    private static final int MAX_VOICES = 24;

    private static final class Voice {
        final byte[] pcm;
        final Loop loop;                                  // null for a one-shot
        final int gap;                                    // samples of silence between repeats of a loop
        int wait;                                         // samples still to wait before the first one
        int at;                                           // byte offset into pcm; past the end while in the gap

        Voice(byte[] pcm, int wait, Loop loop, int gap) {
            this.pcm = pcm;
            this.wait = wait;
            this.loop = loop;
            this.gap = gap;
        }

        /** The next sample at unit gain, advancing; 0 in the wait or the gap. */
        int next() {
            if (wait > 0) { wait--; return 0; }
            if (at < pcm.length) {
                int value = (short) ((pcm[at] & 0xff) | pcm[at + 1] << 8);
                at += 2;
                return value;
            }
            if (loop == null) return 0;
            at += 2;
            if (at >= pcm.length + gap * 2) at = 0;
            return 0;
        }

        boolean done() { return loop == null && wait == 0 && at >= pcm.length; }
    }

    private static final java.util.List<Voice> VOICES = new java.util.ArrayList<>();
    private static Thread mixer;
    private static long musicSample;

    private static void addVoice(Voice voice) {
        synchronized (VOICES) {
            if (VOICES.size() >= MAX_VOICES) VOICES.remove(0);
            VOICES.add(voice);
        }
    }

    /** Called once by the desktop launcher. Unit tests never open an audio device. */
    public static synchronized void startMusic() {
        if (mixer != null) return;
        mixer = new Thread(GameAudio::mixerLoop, "b2bj-mixer");
        mixer.setDaemon(true);
        mixer.setPriority(Thread.MAX_PRIORITY);
        mixer.start();
    }

    private static void mixerLoop() {
        SourceDataLine line = null;
        byte[] chunk = new byte[CHUNK * 2];
        int[] sum = new int[CHUNK];
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (line == null) {
                    try {
                        line = AudioSystem.getSourceDataLine(FORMAT);
                        line.open(FORMAT, CHUNK * 2 * LINE_CHUNKS);
                        line.start();
                    } catch (LineUnavailableException | IllegalArgumentException | SecurityException unavailable) {
                        line = null;
                        Thread.sleep(1000);                       // no output device: try again later, keep the game running
                        continue;
                    }
                }
                java.util.Arrays.fill(sum, 0);
                mixVoices(sum);
                mixMusic(sum);
                for (int index = 0; index < CHUNK; index++) put(chunk, index * 2, limited(sum[index]));
                if (line.available() == line.getBufferSize() && line.getFramePosition() > 0) {
                    underruns++;
                    if (underruns % 50 == 1) System.err.println("audio underruns: " + underruns);
                }
                line.write(chunk, 0, chunk.length);
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        } finally {
            if (line != null) line.close();
        }
    }

    static void mixVoices(int[] sum) {
        double session = muted ? 0 : masterVolume * sfxVolume;
        synchronized (VOICES) {
            for (java.util.Iterator<Voice> it = VOICES.iterator(); it.hasNext();) {
                Voice voice = it.next();
                double gain = session * (voice.loop == null ? 1 : LOOP_GAIN[voice.loop.ordinal()]);
                for (int index = 0; index < sum.length; index++) sum[index] += (int) Math.round(voice.next() * gain);
                if (voice.done()) it.remove();
            }
        }
    }

    private static void mixMusic(int[] sum) {
        if (!musicAudible()) return;
        byte[] music = renderMusicChunk(musicBiome, musicBlade, musicSample, sum.length, masterVolume * musicVolume * duckGain());
        musicSample += sum.length;
        for (int index = 0; index < sum.length; index++) sum[index] += sample(music, index * 2);
    }

    // ---------------------------------------------------------------- packaged recordings

    private static final double SAMPLE_GAIN = 0.8;      // recordings are peak-normalised by the generator
    private static final int MAX_TAKES = 6;
    private static final java.util.Map<String, byte[][]> SAMPLES = new java.util.HashMap<>();
    private static final int[] LAST_TAKE = new int[Cue.values().length];

    /** The cue's recorded takes, already in FORMAT; empty when none is packaged. Loaded once, on first use. */
    static byte[][] samples(Cue cue) {
        return samples(cue.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** A recording family by file name: a cue's own takes, a layer, or a loop. */
    static synchronized byte[][] samples(String family) {
        byte[][] takes = SAMPLES.get(family);
        if (takes != null) return takes;
        java.util.List<byte[]> loaded = new java.util.ArrayList<>();
        String base = "/assets/audio/" + family;
        for (int take = 1; take <= MAX_TAKES; take++) {
            byte[] pcm = readWav(base + (take == 1 ? "" : "_" + take) + ".wav");
            if (pcm == null) break;
            loaded.add(pcm);
        }
        takes = loaded.toArray(new byte[0][]);
        SAMPLES.put(family, takes);
        return takes;
    }

    private static byte[] readWav(String resource) {
        java.io.InputStream stream = GameAudio.class.getResourceAsStream(resource);
        if (stream == null) return null;
        try (javax.sound.sampled.AudioInputStream raw =
                     AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(stream));
             javax.sound.sampled.AudioInputStream pcm = raw.getFormat().matches(FORMAT)
                     ? raw : AudioSystem.getAudioInputStream(FORMAT, raw)) {
            return pcm.readAllBytes();
        } catch (java.io.IOException | javax.sound.sampled.UnsupportedAudioFileException
                 | IllegalArgumentException broken) {
            System.err.println("Unplayable recording " + resource + ": " + broken.getMessage());
            return null;
        }
    }

    /** Which take plays next: random, and never the one that just played, so repeated hits vary. */
    private static int take(Cue cue) {
        int count = samples(cue).length;
        if (count < 2) return 0;
        int last = LAST_TAKE[cue.ordinal()];
        int next = (last + 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(count - 1)) % count;
        LAST_TAKE[cue.ordinal()] = next;
        return next;
    }

    private static final double DETUNE = 0.05;        // +-5 %: the same take never sounds like a repeat

    private static double pitch(Cue cue) {
        return cue.pitched ? 1 + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 2 - 1) * DETUNE : 1;
    }

    /** The take, its layer mixed under it, detuned by pitch, at unit session volume (the mixer applies the
     *  sliders live); or the fallback chirp when nothing is recorded. */
    static byte[] voicePcm(Cue cue, int take, double pitch) {
        byte[][] takes = samples(cue);
        if (takes.length == 0) return synthesize(cue);
        byte[] pcm = takes[Math.floorMod(take, takes.length)];
        if (cue.layer != null) {
            byte[][] under = samples(cue.layer);
            if (under.length > 0) pcm = mixed(pcm, under[Math.floorMod(take, under.length)]);
        }
        if (cue.chirp) pcm = mixed(pcm, scaled(synthesize(cue), CHIRP_GAIN));
        return scaled(resampled(pcm, pitch), SAMPLE_GAIN);
    }

    /** What that voice sounds like at the session volume right now (the tests' view of a play). */
    static byte[] playbackSamples(Cue cue, int take, double pitch) {
        if (samples(cue).length == 0) return playbackSamples(cue);
        return scaled(voicePcm(cue, take, pitch), muted ? 0 : masterVolume * sfxVolume);
    }

    private static final double LAYER_GAIN = 0.6;      // the layer sits under the cue
    private static final double CHIRP_GAIN = 1.8;      // the chirps were written quiet (Cue.volume ~0.2); lift them to a layer
    private static final int HEADROOM = 32_000;

    /** Both sounds from t=0, the layer under the cue, brought back under full scale if the sum went over it;
     *  the longer one sets the length. Two peak-normalised takes summed and clamped is distortion. */
    static byte[] mixed(byte[] a, byte[] b) {
        int count = Math.max(a.length, b.length) / 2;
        int[] sum = new int[count];
        int peak = 1;
        for (int index = 0; index < count; index++) {
            sum[index] = (index * 2 + 1 < a.length ? sample(a, index * 2) : 0)
                    + (int) Math.round((index * 2 + 1 < b.length ? sample(b, index * 2) : 0) * LAYER_GAIN);
            peak = Math.max(peak, Math.abs(sum[index]));
        }
        double gain = Math.min(1.0, HEADROOM / (double) peak);
        byte[] output = new byte[count * 2];
        for (int index = 0; index < count; index++) put(output, index * 2, (int) Math.round(sum[index] * gain));
        return output;
    }

    private static final int KNEE = 26_000;             // above this the mix is squeezed instead of clipped

    /** A soft limiter for the summed chunk: overlapping hits bend, they do not crunch. */
    static int limited(int value) {
        int magnitude = Math.abs(value);
        if (magnitude <= KNEE) return value;
        double over = (magnitude - KNEE) / (double) (Short.MAX_VALUE - KNEE);
        int squeezed = KNEE + (int) Math.round((Short.MAX_VALUE - KNEE) * (1 - Math.exp(-over)));
        return value < 0 ? -squeezed : squeezed;
    }

    /** Play faster (pitch > 1, shorter) or slower by linear interpolation; pitch 1 returns the input itself. */
    static byte[] resampled(byte[] pcm, double pitch) {
        if (!Double.isFinite(pitch) || pitch <= 0) throw new IllegalArgumentException("Invalid pitch");
        if (pitch == 1) return pcm;
        int count = pcm.length / 2, outCount = Math.max(2, (int) (count / pitch));
        byte[] output = new byte[outCount * 2];
        for (int index = 0; index < outCount; index++) {
            double at = index * pitch;
            int whole = Math.min(count - 1, (int) at), next = Math.min(count - 1, whole + 1);
            double fraction = at - whole;
            put(output, index * 2, (int) Math.round(sample(pcm, whole * 2) * (1 - fraction) + sample(pcm, next * 2) * fraction));
        }
        return output;
    }

    private static int sample(byte[] pcm, int index) {
        return (short) ((pcm[index] & 0xff) | pcm[index + 1] << 8);
    }

    private static void put(byte[] pcm, int index, int value) {
        short clamped = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
        pcm[index] = (byte) clamped;
        pcm[index + 1] = (byte) (clamped >>> 8);
    }

    static byte[] scaled(byte[] pcm, double gain) {
        byte[] output = new byte[pcm.length];
        for (int index = 0; index + 1 < pcm.length; index += 2) {
            int sample = (short) ((pcm[index] & 0xff) | pcm[index + 1] << 8);
            short mixed = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            output[index] = (byte) mixed;
            output[index + 1] = (byte) (mixed >>> 8);
        }
        return output;
    }

    static byte[] synthesize(Cue cue) {
        return synthesize(cue, 1);
    }

    static byte[] playbackSamples(Cue cue) {
        return synthesize(cue, muted ? 0 : masterVolume * sfxVolume);
    }

    private static byte[] synthesize(Cue cue, double volume) {
        int sampleCount = Math.max(2, (int) (cue.duration * SAMPLE_RATE));
        byte[] output = new byte[sampleCount * 2];
        double phase = 0;
        int noiseState = 0x9e3779b9 ^ cue.ordinal();

        for (int index = 0; index < sampleCount; index++) {
            double progress = index / (double) (sampleCount - 1);
            double frequency = frequency(cue, progress);
            phase += Math.PI * 2 * frequency / SAMPLE_RATE;
            double wave = Math.sin(phase) * 0.78
                    + Math.signum(Math.sin(phase * 0.5)) * 0.12;
            if (cue.noise) {
                noiseState = noiseState * 1_664_525 + 1_013_904_223;
                wave += ((noiseState >>> 8 & 0xffff) / 32_767.5 - 1) * 0.35;
            }
            double envelope = Math.sin(Math.PI * progress);
            double mixed = Math.max(-1, Math.min(1,
                    wave * envelope * cue.volume * volume));
            short sample = (short) Math.round(mixed * Short.MAX_VALUE);
            output[index * 2] = (byte) sample;
            output[index * 2 + 1] = (byte) (sample >>> 8);
        }
        return output;
    }

    private static double frequency(Cue cue, double progress) {
        if (cue == Cue.VICTORY) {
            double[] notes = {330, 415, 494, 660};
            return notes[Math.min(notes.length - 1, (int) (progress * notes.length))];
        }
        if (cue == Cue.GUARDIAN_SLAM) {
            return cue.endFrequency + (cue.startFrequency - cue.endFrequency)
                    * (1 - progress) + Math.sin(progress * Math.PI * 8) * 8;
        }
        return cue.startFrequency
                + (cue.endFrequency - cue.startFrequency) * progress;
    }

    // ---------------------------------------------------------------- ducking

    private static volatile double duckDepth, duckSeconds;
    private static volatile long duckEnd;

    /** Pull the music down by depth for a moment (a kill, a hit, the big cleave), recovering linearly. */
    public static void duck(double depth, double seconds) {
        if (!Double.isFinite(depth) || depth < 0 || depth > 1 || !Double.isFinite(seconds) || seconds <= 0)
            throw new IllegalArgumentException("Invalid duck");
        duckDepth = depth;
        duckSeconds = seconds;
        duckEnd = System.nanoTime() + (long) (seconds * 1e9);
    }

    /** The music's multiplier right now: 1 outside a duck. */
    static double duckGain() {
        double remaining = (duckEnd - System.nanoTime()) / 1e9;
        if (remaining <= 0) return 1;
        return 1 - duckDepth * Math.min(1, remaining / duckSeconds);
    }

    // ---------------------------------------------------------------- loops

    private static final double[] LOOP_GAIN = new double[Loop.values().length];

    /** Start (or just retune) a loop; gain 0..1 is applied live by the mixer, so the charge hum can rise.
     *  Called every frame is fine: a running loop only takes the new gain. Silent sessions start nothing. */
    public static void startLoop(Loop loop, double gain) {
        validateVolume(gain);
        LOOP_GAIN[loop.ordinal()] = gain;
        if (loopRunning(loop) || muted || masterVolume == 0 || sfxVolume == 0) return;
        byte[][] takes = samples(loop.family);
        if (takes.length == 0) return;
        addVoice(new Voice(scaled(takes[0], SAMPLE_GAIN), 0, loop, (int) (loop.gapSeconds * SAMPLE_RATE)));
    }

    public static void stopLoop(Loop loop) {
        synchronized (VOICES) {
            VOICES.removeIf(voice -> voice.loop == loop);
        }
    }

    public static void stopLoops() {
        for (Loop loop : Loop.values()) stopLoop(loop);
    }

    static long underruns() { return underruns; }

    static boolean loopRunning(Loop loop) {
        synchronized (VOICES) {
            for (Voice voice : VOICES) if (voice.loop == loop) return true;
        }
        return false;
    }

    public static void setMuted(boolean value) {
        muted = value;
    }

    public static boolean muted() {
        return muted;
    }

    public static double masterVolume() { return masterVolume; }
    public static double sfxVolume() { return sfxVolume; }
    public static double musicVolume() { return musicVolume; }

    public static void setMasterVolume(double value) {
        validateVolume(value);
        masterVolume = value;
    }

    public static void setSfxVolume(double value) {
        validateVolume(value);
        sfxVolume = value;
    }

    public static void setMusicVolume(double value) {
        validateVolume(value);
        musicVolume = value;
    }

    private static void validateVolume(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Volume must be between 0 and 1");
    }

    /** Frame updates only change flags; the one mixer thread renders the music from them. */
    public static void setMusicState(int biome, boolean blade, boolean active) {
        if (biome < 0 || biome > 3) throw new IllegalArgumentException("Invalid music biome");
        musicBiome = biome;
        musicBlade = blade;
        musicActive = active;
    }

    private static boolean musicAudible() {
        return musicActive && !muted && masterVolume > 0 && musicVolume > 0;
    }

    /** Original quiet pad and eight-note motif; absolute sample time keeps chunks continuous. */
    static byte[] renderMusicChunk(int biome, boolean blade, long firstSample, int sampleCount, double volume) {
        validateVolume(volume);
        if (biome < 0 || biome > 3 || firstSample < 0 || sampleCount < 0 || sampleCount > SAMPLE_RATE)
            throw new IllegalArgumentException("Invalid music chunk");
        byte[] output = new byte[sampleCount * 2];
        double root = MUSIC_NOTES[biome][0];
        for (int index = 0; index < sampleCount; index++) {
            double time = (firstSample + index) / (double) SAMPLE_RATE;
            double beat = time / (blade ? 1.0 : 2.0);
            int note = (int) (beat % 8);
            double envelope = Math.pow(Math.sin(Math.PI * (beat % 1)), 2);
            double phase = Math.PI * 2 * time;
            double pad = Math.sin(phase * root / 2) * .035 + Math.sin(phase * root * 1.5) * .018;
            double motif = Math.sin(phase * MUSIC_NOTES[biome][note]) * envelope * (blade ? .05 : .03);
            double texture = blade ? Math.sin(phase * root * 2) * envelope * .012 : 0;
            short sample = (short) Math.round((pad + motif + texture) * volume * Short.MAX_VALUE);
            output[index * 2] = (byte) sample;
            output[index * 2 + 1] = (byte) (sample >>> 8);
        }
        return output;
    }
}
