import java.util.Arrays;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/** The Chapter Two forest enemy attack recordings (tools/audio/generate_forest_sfx.py). */
public final class ForestSfxTest {
    private static final int RATE = 22_050;
    private static final GameAudio.Cue[] FOREST = {
            GameAudio.Cue.IMP_WINDUP, GameAudio.Cue.IMP_CLAW, GameAudio.Cue.WOLF_GROWL, GameAudio.Cue.WOLF_BITE,
            GameAudio.Cue.WOLF_POUNCE, GameAudio.Cue.WOLF_LAND, GameAudio.Cue.MIRE_CHARGE, GameAudio.Cue.MIRE_ERUPT};
    private static final Set<GameAudio.Cue> WINDUPS = Set.of(GameAudio.Cue.IMP_WINDUP, GameAudio.Cue.WOLF_GROWL);

    public static void main(String[] args) throws Exception {
        filesAreNativeMixerFormat();
        twoCleanDistinctTakesWithHeadroom();
        audibleBesideTheBladeSlash();
        runtimePlaysTheRecordingThroughTheSliders();
        System.out.println("ForestSfxTest passed");
    }

    private static void filesAreNativeMixerFormat() throws Exception {
        for (GameAudio.Cue cue : FOREST) {
            String family = cue.name().toLowerCase(java.util.Locale.ROOT);
            for (String file : new String[] {family, family + "_2"}) {
                var url = GameAudio.class.getResource("/assets/audio/" + file + ".wav");
                assert url != null : file + ".wav is not packaged";
                AudioFormat format = AudioSystem.getAudioFileFormat(url).getFormat();
                assert format.getSampleRate() == RATE && format.getChannels() == 1 && format.getSampleSizeInBits() == 16
                        && format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && !format.isBigEndian()
                        : file + " must be 22050 Hz mono PCM16, no runtime conversion: " + format;
            }
        }
    }

    private static void twoCleanDistinctTakesWithHeadroom() {
        for (GameAudio.Cue cue : FOREST) {
            byte[][] takes = GameAudio.samples(cue);
            assert takes.length == 2 : cue + " needs exactly two takes";
            assert !Arrays.equals(takes[0], takes[1]) : cue + " takes must differ";
            double shortest = cue == GameAudio.Cue.MIRE_CHARGE || WINDUPS.contains(cue) ? .25 : .15;
            double longest = cue == GameAudio.Cue.MIRE_CHARGE ? .70 : WINDUPS.contains(cue) ? .50 : .40;
            for (byte[] take : takes) {
                double seconds = take.length / 2.0 / RATE;
                assert seconds >= shortest && seconds <= longest : cue + " lasts " + seconds + " s";
                int peak = peak(take, 0, take.length);
                assert peak >= 16_000 && peak <= 26_500 : cue + " peak " + peak + " must be loud but under full scale";
                assert Math.abs(sample(take, 0)) < 128 && Math.abs(sample(take, take.length - 2)) < 128
                        : cue + " must start and end without a click";
                assert peak(take, take.length - 2 * 220, take.length) < peak * .15
                        : cue + " must have died away by its last 10 ms, not be cut off or ring";
                long sum = 0;
                for (int index = 0; index < take.length; index += 2) sum += sample(take, index);
                assert Math.abs(sum / (take.length / 2.0)) < peak * .01 : cue + " has DC bias";
            }
        }
    }

    private static void audibleBesideTheBladeSlash() {
        double slash = 0;
        byte[][] slashes = GameAudio.samples(GameAudio.Cue.SLASH);
        for (byte[] take : slashes) slash += loudest100ms(take) / slashes.length;
        for (GameAudio.Cue cue : FOREST)
            for (byte[] take : GameAudio.samples(cue))
                assert loudest100ms(take) > slash * .4 : cue + " is too quiet beside the slash";
    }

    /** No layer and no chirp: the cue plays its own recording, and the mute and SFX sliders still govern it. */
    private static void runtimePlaysTheRecordingThroughTheSliders() {
        try {
            for (GameAudio.Cue cue : FOREST) {
                byte[][] takes = GameAudio.samples(cue);
                for (int take = 0; take < takes.length; take++) {
                    byte[] voice = GameAudio.voicePcm(cue, take, 1.0);
                    assert Arrays.equals(voice, GameAudio.scaled(takes[take], .8)) : cue + " must play its recording";
                    assert !Arrays.equals(voice, GameAudio.synthesize(cue)) : cue + " must not fall back to a beep";
                }
                byte[] full = GameAudio.playbackSamples(cue, 1, 1.0);
                GameAudio.setSfxVolume(.5);
                byte[] half = GameAudio.playbackSamples(cue, 1, 1.0);
                for (int index = 0; index < full.length; index += 2)
                    assert Math.abs(sample(half, index) - sample(full, index) * .5) <= 1 : cue + " ignores the SFX slider";
                GameAudio.setSfxVolume(1);
                GameAudio.setMuted(true);
                assert peak(GameAudio.playbackSamples(cue, 0, 1.0), 0, Integer.MAX_VALUE) == 0 : cue + " plays while muted";
                GameAudio.setMuted(false);
            }
        } finally {
            GameAudio.setMuted(false);
            GameAudio.setSfxVolume(1);
        }
    }

    /** RMS of the loudest 100 ms, in 10 ms steps: how loud a short attack reads, unlike a peak. */
    private static double loudest100ms(byte[] take) {
        int window = RATE / 10;
        double best = 0;
        for (int start = 0; start + window * 2 <= take.length; start += 440) {
            double energy = 0;
            for (int index = start; index < start + window * 2; index += 2) energy += Math.pow(sample(take, index), 2);
            best = Math.max(best, Math.sqrt(energy / window));
        }
        return best;
    }

    private static int peak(byte[] pcm, int from, int to) {
        int peak = 0;
        for (int index = Math.max(0, from); index + 1 < Math.min(pcm.length, to); index += 2)
            peak = Math.max(peak, Math.abs(sample(pcm, index)));
        return peak;
    }

    private static int sample(byte[] pcm, int index) {
        return (short) ((pcm[index] & 0xff) | pcm[index + 1] << 8);
    }
}
