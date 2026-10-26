import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/** The Outpost Warden's and Briarheart's attack recordings (tools/audio/generate_boss_sfx.py). */
public final class BossSfxTest {
    private static final int RATE = 22_050;
    static final GameAudio.Cue[] BOSS = {
            GameAudio.Cue.WARDEN_WINDUP, GameAudio.Cue.WARDEN_SWEEP, GameAudio.Cue.WARDEN_RUSH, GameAudio.Cue.WARDEN_FRACTURE,
            GameAudio.Cue.BRIAR_WINDUP, GameAudio.Cue.BRIAR_ROOTS, GameAudio.Cue.BRIAR_SEEDS, GameAudio.Cue.BRIAR_CAGE};

    public static void main(String[] args) throws Exception {
        filesAreNativeMixerFormat();
        twoCleanDistinctTakesWithHeadroom();
        audibleBesideTheCombatSounds();
        everyAttackHasItsOwnSound();
        runtimePlaysTheRecordingThroughTheSliders();
        System.out.println("BossSfxTest passed: 8 boss cues x 2 takes, native format, clean edges, levels beside slash and slam, sliders and mute");
    }

    private static void filesAreNativeMixerFormat() throws Exception {
        for (GameAudio.Cue cue : BOSS) {
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
        for (GameAudio.Cue cue : BOSS) {
            byte[][] takes = GameAudio.samples(cue);
            assert takes.length == 2 : cue + " needs exactly two takes";
            assert !Arrays.equals(takes[0], takes[1]) : cue + " takes must differ";
            double longest = cue == GameAudio.Cue.WARDEN_FRACTURE ? .85 : .70;   // only the falling rubble runs long
            for (byte[] take : takes) {
                double seconds = take.length / 2.0 / RATE;
                assert seconds >= .25 && seconds <= longest : cue + " lasts " + seconds + " s";
                int peak = peak(take, 0, take.length);
                assert peak >= .62 * 32767 && peak <= .82 * 32767 : cue + " peak " + peak + " must sit at ~0.65-0.80 of full scale";
                assert Math.abs(sample(take, 0)) < 128 && Math.abs(sample(take, take.length - 2)) < 128
                        : cue + " must start and end without a click";
                assert peak(take, take.length - 2 * 220, take.length) < peak * .15
                        : cue + " must have died away by its last 10 ms, not be cut off or rumble on";
                long sum = 0;
                for (int index = 0; index < take.length; index += 2) sum += sample(take, index);
                assert Math.abs(sum / (take.length / 2.0)) < peak * .01 : cue + " has DC bias";
            }
        }
    }

    /** Heard in the same mix as the blade and the Warden's slam: never buried, never far above them. */
    private static void audibleBesideTheCombatSounds() {
        double slash = average(GameAudio.Cue.SLASH), slam = average(GameAudio.Cue.GUARDIAN_SLAM);
        for (GameAudio.Cue cue : BOSS)
            for (byte[] take : GameAudio.samples(cue)) {
                double level = loudest100ms(take);
                assert level > slash * .6 : cue + " is too quiet beside the slash: " + level;
                assert level < slam * 1.3 : cue + " is far louder than the Warden's slam: " + level;
            }
    }

    private static void everyAttackHasItsOwnSound() {
        Set<Integer> recordings = new HashSet<>(), fallbacks = new HashSet<>();
        for (GameAudio.Cue cue : GameAudio.Cue.values()) {
            for (byte[] take : GameAudio.samples(cue)) assert recordings.add(Arrays.hashCode(take)) || !isBoss(cue)
                    : cue + " reuses another cue's recording";
            assert fallbacks.add(Arrays.hashCode(GameAudio.synthesize(cue))) : cue + " fallback must be distinct";
        }
    }

    /** No layer and no chirp: the cue plays its own recording, and the mute and SFX sliders still govern it. */
    private static void runtimePlaysTheRecordingThroughTheSliders() {
        try {
            for (GameAudio.Cue cue : BOSS) {
                assert cue.layer == null && !cue.chirp : cue + " carries its body itself; nothing mixed under it";
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

    private static boolean isBoss(GameAudio.Cue cue) { return Arrays.asList(BOSS).contains(cue); }

    private static double average(GameAudio.Cue cue) {
        double sum = 0;
        byte[][] takes = GameAudio.samples(cue);
        for (byte[] take : takes) sum += loudest100ms(take) / takes.length;
        return sum;
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
