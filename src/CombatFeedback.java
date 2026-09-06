public final class CombatFeedback {
    private static final double HIT_DURATION = 0.12;
    private static final double DAMAGE_DURATION = 0.32;
    private static final double ICHOR_DURATION = 0.42;
    private static final double VICTORY_DURATION = 0.8;
    private static final double MAX_SHAKE_DURATION = 0.35;

    private double hitTime;
    private double damageTime;
    private double ichorTime;
    private double victoryTime;
    private double shakeTime;

    public void enemyHit(boolean defeated) {
        hitTime = HIT_DURATION;
        shakeTime = Math.max(shakeTime, defeated ? 0.22 : 0.12);
    }

    public void playerHit() {
        damageTime = DAMAGE_DURATION;
        shakeTime = Math.max(shakeTime, 0.28);
    }

    public void transformed() {
        ichorTime = ICHOR_DURATION;
        shakeTime = Math.max(shakeTime, 0.18);
    }

    public void guardianSlam() {
        shakeTime = MAX_SHAKE_DURATION;
    }

    public void victory() {
        victoryTime = VICTORY_DURATION;
        shakeTime = Math.max(shakeTime, 0.2);
    }

    public void update(double seconds) {
        hitTime = decay(hitTime, seconds);
        damageTime = decay(damageTime, seconds);
        ichorTime = decay(ichorTime, seconds);
        victoryTime = decay(victoryTime, seconds);
        shakeTime = decay(shakeTime, seconds);
    }

    public int hitAlpha() {
        return alpha(hitTime, HIT_DURATION, 22);
    }

    public int damageAlpha() {
        return alpha(damageTime, DAMAGE_DURATION, 180);
    }

    public int ichorAlpha() {
        return alpha(ichorTime, ICHOR_DURATION, 40);
    }

    public int victoryAlpha() {
        return alpha(victoryTime, VICTORY_DURATION, 170);
    }

    public int shakeStrength() {
        return (int) Math.ceil(12 * shakeTime / MAX_SHAKE_DURATION);
    }

    public int shakeX(long frame) {
        return offset(frame * 1_103_515_245L + 12_345);
    }

    public int shakeY(long frame) {
        return offset(frame * 2_147_483_647L + 54_321);
    }

    private int offset(long value) {
        int strength = shakeStrength();
        return strength == 0 ? 0
                : Math.floorMod((int) (value >>> 16), strength * 2 + 1) - strength;
    }

    private static int alpha(double time, double duration, int maximum) {
        return (int) Math.round(maximum * time / duration);
    }

    private static double decay(double time, double seconds) {
        return Math.max(0, time - seconds);
    }
}
