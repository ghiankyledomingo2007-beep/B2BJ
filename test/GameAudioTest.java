import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GameAudioTest {
    public static void main(String[] args) {
        everyCueProducesAUniqueClickFreeWaveform();
        muteStateCanBeControlled();
        sessionVolumesScalePlaybackWithoutChangingLegacySynthesis();
        ambientThemesAreDistinctAndVolumeAware();
        frameStateUpdatesNeverStartAudioWorkers();
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

    private static void sessionVolumesScalePlaybackWithoutChangingLegacySynthesis() {
        byte[] baseline = GameAudio.synthesize(GameAudio.Cue.TRANSFORM);
        try {
            invoke("setMasterVolume", new Class<?>[] {double.class}, .5);
            invoke("setSfxVolume", new Class<?>[] {double.class}, .25);
            assert invoke("masterVolume", new Class<?>[0]).equals(.5);
            assert invoke("sfxVolume", new Class<?>[0]).equals(.25);
            byte[] quiet = (byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class}, GameAudio.Cue.TRANSFORM);
            for (int index = 0; index < baseline.length; index += 2)
                assert Math.abs(sample(quiet, index) - sample(baseline, index) * .125) <= 1
                        : "master and SFX sliders must both scale actual playback";
            assert Arrays.equals(baseline, GameAudio.synthesize(GameAudio.Cue.TRANSFORM));
            for (String setter : new String[] {"setMasterVolume", "setSfxVolume", "setMusicVolume"}) {
                for (double invalid : new double[] {-1, 1.01, Double.NaN, Double.POSITIVE_INFINITY}) {
                    boolean rejected = false;
                    try { invoke(setter, new Class<?>[] {double.class}, invalid); }
                    catch (IllegalArgumentException expected) { rejected = true; }
                    assert rejected : "volume must reject nonfinite or out-of-range values";
                }
            }
            invoke("setSfxVolume", new Class<?>[] {double.class}, 0.0);
            assert silent((byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class}, GameAudio.Cue.HIT));
            invoke("setSfxVolume", new Class<?>[] {double.class}, 1.0);
            GameAudio.setMuted(true);
            assert silent((byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class}, GameAudio.Cue.HIT));
        } finally {
            GameAudio.setMuted(false);
            invoke("setMasterVolume", new Class<?>[] {double.class}, 1.0);
            invoke("setSfxVolume", new Class<?>[] {double.class}, 1.0);
            invoke("setMusicVolume", new Class<?>[] {double.class}, .25);
        }
    }

    private static void ambientThemesAreDistinctAndVolumeAware() {
        Set<Integer> themes = new HashSet<>();
        Class<?>[] signature = {int.class, boolean.class, long.class, int.class, double.class};
        for (int biome = 0; biome < 4; biome++) {
            byte[] full = (byte[]) invoke("renderMusicChunk", signature, biome, false, 0L, 4096, 1.0);
            byte[] repeat = (byte[]) invoke("renderMusicChunk", signature, biome, false, 0L, 4096, 1.0);
            byte[] quiet = (byte[]) invoke("renderMusicChunk", signature, biome, false, 0L, 4096, .25);
            byte[] blade = (byte[]) invoke("renderMusicChunk", signature, biome, true, 0L, 4096, 1.0);
            assert full.length == 8192 && Arrays.equals(full, repeat) : "music must synthesize deterministically";
            byte[] first = (byte[]) invoke("renderMusicChunk", signature, biome, false, 0L, 1024, 1.0);
            byte[] next = (byte[]) invoke("renderMusicChunk", signature, biome, false, 1024L, 3072, 1.0);
            assert Arrays.equals(first, Arrays.copyOfRange(full, 0, first.length));
            assert Arrays.equals(next, Arrays.copyOfRange(full, first.length, full.length))
                    : "music sample timeline must remain continuous between chunks";
            assert !Arrays.equals(full, blade) : "blade form should change music texture";
            assert !silent(full) : "each biome needs audible ambient music";
            for (int index = 0; index < full.length; index += 2)
                assert Math.abs(sample(quiet, index) - sample(full, index) * .25) <= 1;
            assert silent((byte[]) invoke("renderMusicChunk", signature, biome, false, 0L, 4096, 0.0));
            themes.add(Arrays.hashCode(full));
        }
        assert themes.size() == 4 : "all four biome themes must differ";
    }

    private static void frameStateUpdatesNeverStartAudioWorkers() {
        long before = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().equals("b2bj-music")).count();
        for (int frame = 0; frame < 100; frame++)
            invoke("setMusicState", new Class<?>[] {int.class, boolean.class, boolean.class}, frame % 4, frame % 2 == 0, true);
        invoke("setMusicState", new Class<?>[] {int.class, boolean.class, boolean.class}, 0, false, false);
        long after = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().equals("b2bj-music")).count();
        assert after == before : "frame state updates must never create audio threads";
    }

    private static boolean silent(byte[] samples) {
        for (byte value : samples) if (value != 0) return false;
        return true;
    }

    private static Object invoke(String name, Class<?>[] signature, Object... values) {
        try {
            var method = GameAudio.class.getDeclaredMethod(name, signature);
            return method.invoke(null, values);
        } catch (java.lang.reflect.InvocationTargetException failed) {
            if (failed.getCause() instanceof RuntimeException cause) throw cause;
            throw new AssertionError(failed.getCause());
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("Audio control missing: " + name, missing);
        }
    }
}
