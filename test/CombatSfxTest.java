import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/** The player's combat sounds and the hits they land (tools/audio/generate_combat_sfx.py). */
public final class CombatSfxTest {
    private static final int RATE = 22_050;
    /** cue -> packaged takes */
    private static final Map<GameAudio.Cue, Integer> FAMILIES = new LinkedHashMap<>();
    static {
        FAMILIES.put(GameAudio.Cue.BLOB_ATTACK, 3);
        FAMILIES.put(GameAudio.Cue.SLIME_DASH, 2);
        FAMILIES.put(GameAudio.Cue.WATER_SPLASH, 2);
        FAMILIES.put(GameAudio.Cue.SLIME_HURT, 2);
        FAMILIES.put(GameAudio.Cue.ENEMY_HIT, 3);
        FAMILIES.put(GameAudio.Cue.SLASH, 3);
        FAMILIES.put(GameAudio.Cue.THRUST, 2);
        FAMILIES.put(GameAudio.Cue.SLASH_HEAVY, 2);
        FAMILIES.put(GameAudio.Cue.CLEAVE, 2);
        FAMILIES.put(GameAudio.Cue.DASH_SLASH, 2);
        FAMILIES.put(GameAudio.Cue.SPIN_CLEAVE, 2);
        FAMILIES.put(GameAudio.Cue.TIDE_RELEASE, 2);
        FAMILIES.put(GameAudio.Cue.TIDE_IMPACT, 2);
        FAMILIES.put(GameAudio.Cue.FINISHER_RELEASE, 2);
        FAMILIES.put(GameAudio.Cue.FINISHER_IMPACT, 2);
        FAMILIES.put(GameAudio.Cue.GUARDIAN_SLAM, 2);
    }
    private static final Set<GameAudio.Cue> SLIME = Set.of(GameAudio.Cue.BLOB_ATTACK, GameAudio.Cue.SLIME_DASH,
            GameAudio.Cue.WATER_SPLASH, GameAudio.Cue.SLIME_HURT);
    private static final Set<GameAudio.Cue> LIGHT = Set.of(GameAudio.Cue.ENEMY_HIT, GameAudio.Cue.SLASH,
            GameAudio.Cue.THRUST, GameAudio.Cue.DASH_SLASH);
    private static final GameAudio.Cue[] SWINGS = {GameAudio.Cue.SLASH, GameAudio.Cue.THRUST, GameAudio.Cue.SLASH_HEAVY,
            GameAudio.Cue.CLEAVE, GameAudio.Cue.DASH_SLASH, GameAudio.Cue.SPIN_CLEAVE};
    private static final GameAudio.Cue[] IMPACTS = {GameAudio.Cue.ENEMY_HIT, GameAudio.Cue.WATER_SPLASH,
            GameAudio.Cue.SLIME_HURT, GameAudio.Cue.TIDE_IMPACT, GameAudio.Cue.FINISHER_IMPACT, GameAudio.Cue.GUARDIAN_SLAM};
    /** B2BJ.SWING_LEAD: a swing's cue starts this long before the blade lands. */
    private static final double SWING_LEAD = .08;

    public static void main(String[] args) throws Exception {
        everyTakeIsPackagedInMixerFormat();
        takesAreDistinctButKeepTheirFamilyShape();
        takesAreBoundedLoudAndClean();
        runtimePlaysOnlyTheDesignedTake();
        slidersActOnSoundsAlreadyPlaying();
        combatStandsAboveMovementWithoutLeaningOnSubBass();
        weightAndTimingFollowTheMoves();
        System.out.println("CombatSfxTest passed");
    }

    private static void everyTakeIsPackagedInMixerFormat() throws Exception {
        for (var family : FAMILIES.entrySet()) {
            String name = family.getKey().name().toLowerCase(java.util.Locale.ROOT);
            assert GameAudio.samples(family.getKey()).length == family.getValue()
                    : family.getKey() + " should load " + family.getValue() + " takes";
            for (int take = 1; take <= family.getValue(); take++) {
                var url = GameAudio.class.getResource("/assets/audio/" + name + (take == 1 ? "" : "_" + take) + ".wav");
                assert url != null : name + " take " + take + " is not packaged";
                AudioFormat format = AudioSystem.getAudioFileFormat(url).getFormat();
                assert format.getSampleRate() == RATE && format.getChannels() == 1 && format.getSampleSizeInBits() == 16
                        && format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && !format.isBigEndian()
                        : name + " take " + take + " must be 22050 Hz mono PCM16: " + format;
            }
        }
    }

    private static void takesAreDistinctButKeepTheirFamilyShape() {
        Set<Integer> all = new HashSet<>();
        int total = 0;
        for (GameAudio.Cue cue : FAMILIES.keySet()) {
            byte[][] takes = GameAudio.samples(cue);
            for (byte[] take : takes) {
                all.add(Arrays.hashCode(take));
                total++;
                assert Math.abs(take.length - takes[0].length) <= takes[0].length * .2
                        : cue + " alternates must keep the family's length";
            }
        }
        assert all.size() == total : "every take of every family must be its own recording";
    }

    private static void takesAreBoundedLoudAndClean() {
        for (GameAudio.Cue cue : FAMILIES.keySet()) {
            double longest = cue == GameAudio.Cue.GUARDIAN_SLAM ? .85 : SLIME.contains(cue) ? .30 : LIGHT.contains(cue) ? .45 : .70;
            for (byte[] take : GameAudio.samples(cue)) {
                double seconds = take.length / 2.0 / RATE;
                assert seconds >= .15 && seconds <= longest : cue + " lasts " + seconds + " s";
                int peak = peak(take, 0, take.length);
                assert peak >= .65 * 32_767 && peak <= .80 * 32_767 + 1 : cue + " peak " + peak + " outside .65-.80 of full scale";
                assert Math.abs(sample(take, 0)) < 128 && Math.abs(sample(take, take.length - 2)) < 128 : cue + " clicks at an edge";
                assert peak(take, take.length - 2 * 220, take.length) < peak * .15 : cue + " is cut off or still ringing at its end";
                long sum = 0;
                for (int index = 0; index < take.length; index += 2) sum += sample(take, index);
                assert Math.abs(sum / (take.length / 2.0)) < peak * .01 : cue + " has DC bias";
            }
        }
    }

    /** The thud, crack, boom and liquid ring are inside each take now: no old layer, no chirp, still detuned. */
    private static void runtimePlaysOnlyTheDesignedTake() {
        for (GameAudio.Cue cue : FAMILIES.keySet()) {
            assert cue.layer == null && !cue.chirp : cue + " would double-layer or beep over its designed take";
            assert cue.pitched : cue + " should keep its subtle per-play detune";
            byte[][] takes = GameAudio.samples(cue);
            for (int take = 0; take < takes.length; take++)
                assert Arrays.equals(GameAudio.voicePcm(cue, take, 1.0), GameAudio.scaled(takes[take], .8))
                        : cue + " take " + take + " must play exactly its recording";
            byte[] detuned = GameAudio.voicePcm(cue, 0, 1.05);
            assert detuned.length < takes[0].length && Math.abs(sample(detuned, detuned.length - 2)) < 128
                    : cue + " detune must shorten the take and keep its edge clean";
        }
    }

    private static void slidersActOnSoundsAlreadyPlaying() {
        int[] sum = new int[256];
        try {
            drain(sum);
            GameAudio.play(GameAudio.Cue.CLEAVE);
            assert mixed(sum) : "a cleave plays";
            GameAudio.setSfxVolume(0);
            assert !mixed(sum) : "the SFX slider silences a swing already playing";
            GameAudio.setSfxVolume(.5);
            assert mixed(sum) : "raising the slider brings it back";
            GameAudio.setSfxVolume(1);
            GameAudio.setMuted(true);
            assert !mixed(sum) : "mute silences a swing already playing";
            GameAudio.setMuted(false);
            drain(sum);
            GameAudio.setMuted(true);
            GameAudio.play(GameAudio.Cue.GUARDIAN_SLAM);
            GameAudio.setMuted(false);
            assert !mixed(sum) : "a slam played while muted never queues";
            for (GameAudio.Cue cue : FAMILIES.keySet()) {
                byte[] full = GameAudio.playbackSamples(cue, 0, 1.0);
                GameAudio.setMasterVolume(.5);
                byte[] half = GameAudio.playbackSamples(cue, 0, 1.0);
                GameAudio.setMasterVolume(1);
                for (int index = 0; index < full.length; index += 2)
                    assert Math.abs(sample(half, index) - sample(full, index) * .5) <= 1 : cue + " ignores the master slider";
            }
        } finally {
            GameAudio.setMuted(false);
            GameAudio.setSfxVolume(1);
            GameAudio.setMasterVolume(1);
        }
    }

    private static void combatStandsAboveMovementWithoutLeaningOnSubBass() {
        double movement = 0;
        for (GameAudio.Cue foley : new GameAudio.Cue[] {GameAudio.Cue.SLIME_MOVE, GameAudio.Cue.FOOTSTEP})
            for (byte[] take : GameAudio.samples(foley)) movement = Math.max(movement, loudest(take, .1));
        for (GameAudio.Cue cue : FAMILIES.keySet())
            for (byte[] take : GameAudio.samples(cue)) {
                assert loudest(take, .1) > movement * 2.5 : cue + " is barely louder than walking";
                assert aboveLowBody(take) > .25 : cue + " lives in the sub-bass; laptop speakers would lose it";
            }
    }

    private static void weightAndTimingFollowTheMoves() {
        for (GameAudio.Cue swing : SWINGS)
            for (byte[] take : GameAudio.samples(swing)) {
                double at = loudestAt(take, 0, take.length);
                assert at > SWING_LEAD - .04 && at < SWING_LEAD + .08 : swing + " peaks at " + at + " s, not on contact";
            }
        for (GameAudio.Cue impact : IMPACTS)
            for (byte[] take : GameAudio.samples(impact))
                assert loudestAt(take, 0, take.length) < .05 : impact + " must hit at once";
        for (byte[] take : GameAudio.samples(GameAudio.Cue.TIDE_RELEASE))
            assert loudestAt(take, 0, take.length) > .25 : "the tide builds before it peaks";
        for (byte[] take : GameAudio.samples(GameAudio.Cue.FINISHER_RELEASE))
            assert loudestAt(take, 0, take.length) > .05 : "the finisher gathers before it is released";
        assert meanSeconds(GameAudio.Cue.THRUST) < meanSeconds(GameAudio.Cue.SLASH)
                && meanSeconds(GameAudio.Cue.SLASH) < meanSeconds(GameAudio.Cue.SLASH_HEAVY)
                && meanSeconds(GameAudio.Cue.SLASH_HEAVY) < meanSeconds(GameAudio.Cue.CLEAVE)
                : "heavier swings carry longer";
        for (byte[] take : GameAudio.samples(GameAudio.Cue.SPIN_CLEAVE)) {
            int first = loudestIndex(take, 0, seconds(.16));
            int second = loudestIndex(take, first + seconds(.15), take.length);
            double dip = Double.MAX_VALUE;
            for (int index = first; index < second; index += 44) dip = Math.min(dip, rms(take, index, seconds(.01)));
            double a = rms(take, first, seconds(.01)), b = rms(take, second, seconds(.01));
            assert b > a * .4 && dip < Math.min(a, b) * .6 : "the spin must be heard as two sweeps";
        }
    }

    private static boolean mixed(int[] sum) {
        Arrays.fill(sum, 0);
        GameAudio.mixVoices(sum);
        return Arrays.stream(sum).anyMatch(value -> value != 0);
    }

    private static void drain(int[] sum) {
        for (int chunk = 0; chunk < 200; chunk++) mixed(sum);
    }

    private static double meanSeconds(GameAudio.Cue cue) {
        double total = 0;
        byte[][] takes = GameAudio.samples(cue);
        for (byte[] take : takes) total += take.length / 2.0 / RATE / takes.length;
        return total;
    }

    /** Share of the energy above ~300 Hz (two one-pole high-passes): what small speakers can reproduce. */
    private static double aboveLowBody(byte[] take) {
        double keep = Math.exp(-2 * Math.PI * 300 / RATE), low1 = 0, low2 = 0, all = 0, high = 0;
        for (int index = 0; index < take.length; index += 2) {
            double value = sample(take, index);
            low1 = low1 * keep + value * (1 - keep);
            double once = value - low1;
            low2 = low2 * keep + once * (1 - keep);
            double twice = once - low2;
            all += value * value;
            high += twice * twice;
        }
        return high / Math.max(1, all);
    }

    private static int seconds(double seconds) {
        return 2 * (int) (seconds * RATE);
    }

    private static double rms(byte[] take, int from, int bytes) {
        double energy = 0;
        int to = Math.min(take.length, from + bytes);
        for (int index = from; index + 1 < to; index += 2) energy += Math.pow(sample(take, index), 2);
        return Math.sqrt(energy / Math.max(1, (to - from) / 2));
    }

    private static double loudest(byte[] take, double window) {
        double best = 0;
        for (int from = 0; from + seconds(window) <= take.length; from += 440) best = Math.max(best, rms(take, from, seconds(window)));
        return best;
    }

    /** Byte offset of the loudest 10 ms window starting within [from, to). */
    private static int loudestIndex(byte[] take, int from, int to) {
        int best = from;
        for (int index = from; index < Math.min(to, take.length - seconds(.01)); index += 44)
            if (rms(take, index, seconds(.01)) > rms(take, best, seconds(.01))) best = index;
        return best;
    }

    /** Centre, in seconds, of the loudest 10 ms window. */
    private static double loudestAt(byte[] take, int from, int to) {
        return (loudestIndex(take, from, to) + seconds(.005)) / 2.0 / RATE;
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
