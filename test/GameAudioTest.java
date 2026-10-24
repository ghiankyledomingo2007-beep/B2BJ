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
        packagedRecordingsPlayInSessionFormat();
        detuneLayersAndDucking();
        loopsRunUntilStopped();
        mixerSumsVoicesOnTime();
        onceLoopsEndAndLoopsSurviveABusyMix();
        titleThemeFollowsMusicVolumeAndFades();
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
            assert silent((byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class}, GameAudio.Cue.ENEMY_HIT));
            invoke("setSfxVolume", new Class<?>[] {double.class}, 1.0);
            GameAudio.setMuted(true);
            assert silent((byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class}, GameAudio.Cue.ENEMY_HIT));
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
        long before = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().equals("b2bj-mixer")).count();
        for (int frame = 0; frame < 100; frame++)
            invoke("setMusicState", new Class<?>[] {int.class, boolean.class, boolean.class}, frame % 4, frame % 2 == 0, true);
        invoke("setMusicState", new Class<?>[] {int.class, boolean.class, boolean.class}, 0, false, false);
        long after = Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().equals("b2bj-mixer")).count();
        assert after == before : "frame state updates must never create audio threads";
    }

    /** Every cue ships a recording (tools/audio/generate_sfx.py); the chirp is only the fallback. */
    private static void packagedRecordingsPlayInSessionFormat() {
        int recorded = 0;
        for (GameAudio.Cue cue : GameAudio.Cue.values()) {
            byte[][] takes = (byte[][]) invoke("samples", new Class<?>[] {GameAudio.Cue.class}, cue);
            assert takes.length > 0 : cue + " has no recording in assets/audio";
            for (byte[] take : takes) {
                assert take.length >= 2_000 && take.length % 2 == 0 : cue + " recording must be audible 16-bit mono";
                assert Math.abs(sample(take, 0)) < 128 && Math.abs(sample(take, take.length - 2)) < 128
                        : cue + " recording must have click-free edges";
                assert !silent(take);
            }
            recorded++;
        }
        assert recorded == GameAudio.Cue.values().length;
        for (GameAudio.Cue cue : GameAudio.Cue.values()) {
            byte[][] takes = (byte[][]) invoke("samples", new Class<?>[] {GameAudio.Cue.class}, cue);
            for (byte[] take : takes)
                assert take.length <= 2.3 * 22_050 * 2 : cue + " rings too long for a pixel game";
            if (cue.layer != null)
                assert ((byte[][]) invoke("samples", new Class<?>[] {String.class}, cue.layer)).length > 0
                        : cue + " names a layer family with no recording: " + cue.layer;
        }
        for (GameAudio.Loop loop : GameAudio.Loop.values()) {
            byte[][] takes = (byte[][]) invoke("samples", new Class<?>[] {String.class}, loop.family);
            assert takes.length > 0 : loop + " needs a recording";
            assert loop.gapSeconds > 0 || takes[0].length >= 2 * 22_050
                    : loop + " repeats back to back, so it needs at least a second";
        }
        byte[] full = (byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class, int.class, double.class}, GameAudio.Cue.ENEMY_HIT, 0, 1.0);
        byte[] again = (byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class, int.class, double.class}, GameAudio.Cue.ENEMY_HIT, 0, 1.0);
        assert Arrays.equals(full, again) : "a take is deterministic";
        try {
            invoke("setSfxVolume", new Class<?>[] {double.class}, .5);
            byte[] half = (byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class, int.class, double.class}, GameAudio.Cue.ENEMY_HIT, 0, 1.0);
            for (int index = 0; index < full.length; index += 2)
                assert Math.abs(sample(half, index) - sample(full, index) * .5) <= 1 : "sliders scale recordings too";
            GameAudio.setMuted(true);
            assert silent((byte[]) invoke("playbackSamples", new Class<?>[] {GameAudio.Cue.class, int.class, double.class}, GameAudio.Cue.ENEMY_HIT, 0, 1.0));
        } finally {
            GameAudio.setMuted(false);
            invoke("setSfxVolume", new Class<?>[] {double.class}, 1.0);
        }
        boolean rejected = false;
        try { GameAudio.play(GameAudio.Cue.SLASH, -1); } catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected : "a negative delay is a bug";
    }

    private static void detuneLayersAndDucking() {
        Class<?>[] signature = {GameAudio.Cue.class, int.class, double.class};
        byte[] plain = (byte[]) invoke("playbackSamples", signature, GameAudio.Cue.ENEMY_HIT, 0, 1.0);
        byte[] fast = (byte[]) invoke("playbackSamples", signature, GameAudio.Cue.ENEMY_HIT, 0, 1.05);
        byte[] slow = (byte[]) invoke("playbackSamples", signature, GameAudio.Cue.ENEMY_HIT, 0, 0.95);
        assert fast.length < plain.length && plain.length < slow.length : "a higher pitch is a shorter sound";
        assert Math.abs(sample(fast, 0)) < 128 && Math.abs(sample(slow, slow.length - 2)) < 128 : "detuning keeps the edges clean";
        byte[][] bare = (byte[][]) invoke("samples", new Class<?>[] {GameAudio.Cue.class}, GameAudio.Cue.ENEMY_HIT);
        byte[] scaledBare = (byte[]) invoke("scaled", new Class<?>[] {byte[].class, double.class}, bare[0], .8);
        assert !Arrays.equals(plain, scaledBare) : "a cue with a layer plays more than its bare take";
        boolean rejected = false;
        try { invoke("resampled", new Class<?>[] {byte[].class, double.class}, plain, 0.0); }
        catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected;
        rejected = false;
        try { GameAudio.duck(1.5, .1); } catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected : "duck depth must stay within 0..1";
        GameAudio.duck(.5, .05);
        double during = (double) invoke("duckGain", new Class<?>[0]);
        assert during < .6 : "music must dip at once: " + during;
        try { Thread.sleep(90); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        assert (double) invoke("duckGain", new Class<?>[0]) == 1.0 : "music must recover after the duck window";
    }

    private static void loopsRunUntilStopped() {
        GameAudio.setMuted(true);
        try {
            GameAudio.startLoop(GameAudio.Loop.CHARGE, .5);
            assert !(boolean) invoke("loopRunning", new Class<?>[] {GameAudio.Loop.class}, GameAudio.Loop.CHARGE)
                    : "a muted session starts no loop thread";
        } finally { GameAudio.setMuted(false); }
        GameAudio.startLoop(GameAudio.Loop.CHARGE, .5);
        GameAudio.startLoop(GameAudio.Loop.CHARGE, .9);
        long threads = Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().startsWith("b2bj-")).count();
        assert threads == 0 : "loops are mixer voices, never threads of their own";
        GameAudio.stopLoop(GameAudio.Loop.CHARGE);
        assert !(boolean) invoke("loopRunning", new Class<?>[] {GameAudio.Loop.class}, GameAudio.Loop.CHARGE);
        boolean rejected = false;
        try { GameAudio.startLoop(GameAudio.Loop.HEARTBEAT, 2); } catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected : "loop gain is a volume";
        GameAudio.stopLoops();
    }

    /** A delayed cue is silent for exactly its delay, then audible, then gone; a loop never ends. */
    private static void mixerSumsVoicesOnTime() {
        GameAudio.stopLoops();
        int chunk = 256;
        int[] sum = new int[chunk];
        for (int i = 0; i < 400; i++) { Arrays.fill(sum, 0); invoke("mixVoices", new Class<?>[] {int[].class}, (Object) sum); }  // stopped loops fade out, earlier one-shots finish
        GameAudio.play(GameAudio.Cue.ENEMY_HIT, chunk * 2 / 22_050.0);          // two chunks of delay
        invoke("mixVoices", new Class<?>[] {int[].class}, (Object) sum);
        assert Arrays.stream(sum).allMatch(v -> v == 0) : "nothing before the delay";
        invoke("mixVoices", new Class<?>[] {int[].class}, (Object) sum);
        assert Arrays.stream(sum).allMatch(v -> v == 0);
        Arrays.fill(sum, 0);
        invoke("mixVoices", new Class<?>[] {int[].class}, (Object) sum);
        assert Arrays.stream(sum).anyMatch(v -> v != 0) : "the cue plays right after its delay";
        for (int i = 0; i < 200; i++) { Arrays.fill(sum, 0); invoke("mixVoices", new Class<?>[] {int[].class}, (Object) sum); }
        assert Arrays.stream(sum).allMatch(v -> v == 0) : "a one-shot ends";
        GameAudio.startLoop(GameAudio.Loop.AMBIENCE, 1);
        boolean heard = false;
        for (int i = 0; i < 2000 && !heard; i++) {
            Arrays.fill(sum, 0); invoke("mixVoices", new Class<?>[] {int[].class}, (Object) sum);
            heard = Arrays.stream(sum).anyMatch(v -> v != 0);
        }
        assert heard && (boolean) invoke("loopRunning", new Class<?>[] {GameAudio.Loop.class}, GameAudio.Loop.AMBIENCE)
                : "a loop keeps playing";
        GameAudio.stopLoops();
        assert !(boolean) invoke("loopRunning", new Class<?>[] {GameAudio.Loop.class}, GameAudio.Loop.AMBIENCE);
        try {
            invoke("setMusicVolume", new Class<?>[] {double.class}, 0.0);
            GameAudio.startLoop(GameAudio.Loop.BOSS_MUSIC, 1);
            assert !(boolean) invoke("loopRunning", new Class<?>[] {GameAudio.Loop.class}, GameAudio.Loop.BOSS_MUSIC)
                    : "boss music follows the music slider";
            invoke("setMusicVolume", new Class<?>[] {double.class}, .25);
            invoke("setSfxVolume", new Class<?>[] {double.class}, 0.0);
            GameAudio.startLoop(GameAudio.Loop.BOSS_MUSIC, 1);
            assert (boolean) invoke("loopRunning", new Class<?>[] {GameAudio.Loop.class}, GameAudio.Loop.BOSS_MUSIC)
                    : "muting effects does not mute the boss music";
        } finally {
            GameAudio.stopLoops();
            invoke("setSfxVolume", new Class<?>[] {double.class}, 1.0);
            invoke("setMusicVolume", new Class<?>[] {double.class}, .25);
        }
    }

    /** The eating sound is a stoppable one-shot that must not come back; a crowd of one-shots never evicts a loop. */
    private static void onceLoopsEndAndLoopsSurviveABusyMix() {
        GameAudio.stopLoops();
        int[] sum = new int[256];
        Class<?>[] mix = {int[].class};
        Class<?>[] running = {GameAudio.Loop.class};
        for (int i = 0; i < 20; i++) { java.util.Arrays.fill(sum, 0); invoke("mixVoices", mix, (Object) sum); }  // drain fades
        GameAudio.startLoop(GameAudio.Loop.EATING, 1);
        assert (boolean) invoke("loopRunning", running, GameAudio.Loop.EATING);
        for (int i = 0; i < 200; i++) { java.util.Arrays.fill(sum, 0); invoke("mixVoices", mix, (Object) sum); }  // > 2 s
        assert !(boolean) invoke("loopRunning", running, GameAudio.Loop.EATING) : "eating plays once and ends";
        GameAudio.startLoop(GameAudio.Loop.AMBIENCE, 1);
        for (int i = 0; i < 40; i++) GameAudio.play(GameAudio.Cue.DENIED);
        assert (boolean) invoke("loopRunning", running, GameAudio.Loop.AMBIENCE) : "one-shots never push a loop out";
        GameAudio.stopLoops();
    }

    private static boolean silent(byte[] samples) {
        for (byte value : samples) if (value != 0) return false;
        return true;
    }

    private static void titleThemeFollowsMusicVolumeAndFades() {
        GameAudio.stopLoops();
        int[] sum=new int[256];
        for(int i=0;i<500;i++) {Arrays.fill(sum,0);GameAudio.mixVoices(sum);}
        byte[] theme=GameAudio.samples("title_music")[0];
        assert theme.length==48*22_050*2 : "the title needs a complete musical phrase";
        assert Math.abs(sample(theme,0)-sample(theme,theme.length-2))<512 : "the loop seam must not click";
        try {
            GameAudio.setMusicVolume(0);
            GameAudio.startLoop(GameAudio.Loop.TITLE_MUSIC,1);
            assert !GameAudio.loopRunning(GameAudio.Loop.TITLE_MUSIC);
            GameAudio.setMusicVolume(.25);GameAudio.setSfxVolume(0);
            GameAudio.startLoop(GameAudio.Loop.TITLE_MUSIC,1);
            for(int i=0;i<100;i++) {Arrays.fill(sum,0);GameAudio.mixVoices(sum);}
            assert Arrays.stream(sum).anyMatch(v->v!=0) : "muting effects leaves title music audible";
            GameAudio.setMusicVolume(0);
            Arrays.fill(sum,0);GameAudio.mixVoices(sum);
            assert Arrays.stream(sum).allMatch(v->v==0) : "the music slider acts on a running theme";
            GameAudio.setMusicVolume(.25);GameAudio.stopLoop(GameAudio.Loop.TITLE_MUSIC);
            Arrays.fill(sum,0);GameAudio.mixVoices(sum);
            assert Arrays.stream(sum).anyMatch(v->v!=0) : "entering fades music instead of cutting it off";
            for(int i=0;i<100;i++) {Arrays.fill(sum,0);GameAudio.mixVoices(sum);}
            assert Arrays.stream(sum).allMatch(v->v==0) : "the title theme finishes fading";
        } finally {
            GameAudio.stopLoops();GameAudio.setSfxVolume(1);GameAudio.setMusicVolume(.25);
        }
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
