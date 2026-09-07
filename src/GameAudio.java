import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class GameAudio {
    public enum Cue {
        DASH(0.12, 240, 90, 0.20, true),
        ATTACK(0.16, 720, 180, 0.22, false),
        BLOB_ATTACK(0.18, 420, 160, 0.16, true),
        WATER_SPLASH(0.20, 260, 90, 0.16, true),
        HIT(0.10, 150, 55, 0.30, true),
        HURT(0.28, 190, 70, 0.24, false),
        PICKUP(0.22, 420, 980, 0.20, false),
        TRANSFORM(0.48, 180, 920, 0.22, false),
        GUARDIAN_SLAM(0.42, 92, 38, 0.32, true),
        VICTORY(0.72, 330, 660, 0.22, false),
        TIDE_RELEASE(0.30, 130, 380, 0.23, true),
        TIDE_IMPACT(0.38, 220, 78, 0.24, true);

        private final double duration;
        private final double startFrequency;
        private final double endFrequency;
        private final double volume;
        private final boolean noise;

        Cue(double duration, double startFrequency, double endFrequency,
                double volume, boolean noise) {
            this.duration = duration;
            this.startFrequency = startFrequency;
            this.endFrequency = endFrequency;
            this.volume = volume;
            this.noise = noise;
        }
    }

    private static final int SAMPLE_RATE = 22_050;
    private static final AudioFormat FORMAT = new AudioFormat(
            SAMPLE_RATE, 16, 1, true, false);
    private static volatile boolean muted;
    private static volatile double masterVolume = 1, sfxVolume = 1, musicVolume = .25;
    private static volatile int musicBiome;
    private static volatile boolean musicBlade, musicActive;
    private static volatile long musicRevision;
    private static final Object MUSIC_LOCK = new Object();
    private static Thread musicWorker;
    private static final double[][] MUSIC_NOTES = {
            {146.832, 220, 174.614, 196, 146.832, 174.614, 130.813, 164.814},
            {164.814, 246.942, 195.998, 293.665, 220, 195.998, 246.942, 164.814},
            {130.813, 155.563, 116.541, 195.998, 130.813, 174.614, 155.563, 116.541},
            {174.614, 246.942, 207.652, 349.228, 220, 293.665, 246.942, 195.998}
    };
    private static final java.util.concurrent.Semaphore VOICES = new java.util.concurrent.Semaphore(6);

    private GameAudio() { }

    public static void play(Cue cue) {
        java.util.Objects.requireNonNull(cue, "cue");
        if (muted || masterVolume == 0 || sfxVolume == 0 || !VOICES.tryAcquire()) {
            return;
        }
        byte[] samples = playbackSamples(cue);
        Thread sound = new Thread(() -> {
            try { if (!muted) play(samples); }
            finally { VOICES.release(); }
        }, "b2bj-audio");
        sound.setDaemon(true);
        sound.start();
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

    private static void play(byte[] samples) {
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, samples.length);
            line.start();
            line.write(samples, 0, samples.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException
                | SecurityException ignored) {
            // Audio loss must never stop gameplay on machines without an output line.
        } finally {
            if (line != null) {
                line.close();
            }
        }
    }

    public static void setMuted(boolean value) {
        if (muted == value) return;
        muted = value;
        wakeMusic();
    }

    public static boolean muted() {
        return muted;
    }

    public static double masterVolume() { return masterVolume; }
    public static double sfxVolume() { return sfxVolume; }
    public static double musicVolume() { return musicVolume; }

    public static void setMasterVolume(double value) {
        validateVolume(value);
        if (masterVolume == value) return;
        masterVolume = value;
        wakeMusic();
    }

    public static void setSfxVolume(double value) {
        validateVolume(value);
        sfxVolume = value;
    }

    public static void setMusicVolume(double value) {
        validateVolume(value);
        if (musicVolume == value) return;
        musicVolume = value;
        wakeMusic();
    }

    private static void validateVolume(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Volume must be between 0 and 1");
    }

    /** Called once by the desktop launcher. Unit tests never open an audio device. */
    public static void startMusic() {
        synchronized (MUSIC_LOCK) {
            if (musicWorker != null) return;
            musicWorker = new Thread(GameAudio::musicLoop, "b2bj-music");
            musicWorker.setDaemon(true);
            musicWorker.start();
        }
    }

    /** Frame updates only change flags; one existing worker handles all music playback. */
    public static void setMusicState(int biome, boolean blade, boolean active) {
        if (biome < 0 || biome > 3) throw new IllegalArgumentException("Invalid music biome");
        if (musicBiome == biome && musicBlade == blade && musicActive == active) return;
        musicBiome = biome;
        musicBlade = blade;
        musicActive = active;
        wakeMusic();
    }

    private static void wakeMusic() {
        synchronized (MUSIC_LOCK) {
            musicRevision++;
            MUSIC_LOCK.notifyAll();
        }
    }

    private static boolean musicAudible() {
        return musicActive && !muted && masterVolume > 0 && musicVolume > 0;
    }

    private static void musicLoop() {
        SourceDataLine line = null;
        long firstSample = 0, failedRevision = -1;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (!musicAudible() || failedRevision == musicRevision) {
                    if (line != null) { line.close(); line = null; }
                    synchronized (MUSIC_LOCK) {
                        while (!musicAudible() || failedRevision == musicRevision) MUSIC_LOCK.wait();
                    }
                }
                long revision = musicRevision;
                try {
                    if (line == null) {
                        line = AudioSystem.getSourceDataLine(FORMAT);
                        line.open(FORMAT, 8192);
                        line.start();
                    }
                    byte[] samples = renderMusicChunk(musicBiome, musicBlade, firstSample, 2048,
                            masterVolume * musicVolume);
                    line.write(samples, 0, samples.length);
                    firstSample += samples.length / 2;
                } catch (LineUnavailableException | IllegalArgumentException | SecurityException unavailable) {
                    failedRevision = revision;
                    if (line != null) { line.close(); line = null; }
                }
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        } finally {
            if (line != null) line.close();
        }
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
