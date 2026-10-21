/** Shared HP damage rule; attacks and defense are stats, not heart counts. */
public final class CombatStats {
    public static final double CRITICAL_CHANCE = .12;
    public static final double CRITICAL_MULTIPLIER = 1.5;
    private CombatStats() { }
    public static int damage(int attack, int defense, boolean critical) {
        if (attack <= 0) return 0;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                Math.round(attack * (critical ? CRITICAL_MULTIPLIER : 1) * 100.0 / (100.0 + Math.max(0, defense)))));
    }
}
