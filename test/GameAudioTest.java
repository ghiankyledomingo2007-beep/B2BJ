import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GameAudioTest {
    public static void main(String[] args) {
        everyCueProducesAUniqueClickFreeWaveform();
        muteStateCanBeControlled();
        System.out.println("GameAudioTest passed");
    }

    private static void everyCueProducesAUniqueClickFreeWaveform() {
        Set<Integer> waveforms = new HashSet<>();

        for (GameAudio.Cue cue : GameAudio.Cue.values()) {
            byte[] samples = GameAudio.synthesize(cue);
            assert samples.length >= 2_000 : cue + " must be audible";
            assert samples.length % 2 == 0 : cue + " must contain 16-bit samples";
            assert Math.abs(sample(samples, 0)) < 128 : cue + " must fade in without a click";
            assert Math.abs(sample(samples, samples.length - 2)) < 128
                    : cue + " must fade out without a click";
            waveforms.add(Arrays.hashCode(samples));
        }

        assert waveforms.size() == GameAudio.Cue.values().length
                : "every game event needs a distinct sound";
    }

    private static void muteStateCanBeControlled() {
        GameAudio.setMuted(true);
        assert GameAudio.muted();
        GameAudio.setMuted(false);
        assert !GameAudio.muted();
    }

    private static int sample(byte[] samples, int index) {
        return (short) ((samples[index] & 0xff) | samples[index + 1] << 8);
    }
}
