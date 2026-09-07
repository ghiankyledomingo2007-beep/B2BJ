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
    private static final java.util.concurrent.Semaphore VOICES = new java.util.concurrent.Semaphore(6);

    private GameAudio() { }

    public static void play(Cue cue) {
        if (muted || !VOICES.tryAcquire()) {
            return;
        }
        byte[] samples = synthesize(cue);
        Thread sound = new Thread(() -> {
            try { if (!muted) play(samples); }
            finally { VOICES.release(); }
        }, "b2bj-audio");
        sound.setDaemon(true);
        sound.start();
    }

    static byte[] synthesize(Cue cue) {
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
                    wave * envelope * cue.volume));
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
        muted = value;
    }

    public static boolean muted() {
        return muted;
    }
}
