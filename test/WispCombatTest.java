public final class WispCombatTest {
    public static void main(String[] args) {
        takesHitsOnlyInsideTheForwardAttackArc();
        telegraphsThenLungesTowardThePlayer();
        deathShrinksToNothing();
        scoutHealthCanBeBalancedPerEncounter();
        System.out.println("WispCombatTest passed");
    }

    private static void takesHitsOnlyInsideTheForwardAttackArc() {
        Wisp front = new Wisp(100, 100, 0, 200);
        Wisp behind = new Wisp(-50, 100, -100, 100);

        assert front.hitFrom(0, 100, 1, 0, 120, 1)
                : "Wisp in front and in range must take damage";
        assert front.health() == Wisp.MAX_HEALTH - 1;
        assert front.state() == Wisp.State.HURT;
        assert !behind.hitFrom(0, 100, 1, 0, 120, 1)
                : "Wisp behind player must not take damage";
    }

    private static void telegraphsThenLungesTowardThePlayer() {
        Wisp wisp = new Wisp(0, 0, -100, 100);

        wisp.update(0.01, 50, 0);
        assert wisp.state() == Wisp.State.TELEGRAPH;
        assert wisp.usesAttackAnimation();
        assert wisp.attackFrame() == 0;
        assert wisp.renderScale() == 1 : "attack art must keep its locked render size";

        wisp.update(Wisp.TELEGRAPH_DURATION / 2, 50, 0);
        assert wisp.attackFrame() == 1 : "telegraph must advance once, not loop";

        wisp.update(Wisp.TELEGRAPH_DURATION / 2, 50, 0);
        assert wisp.state() == Wisp.State.LUNGE;
        assert wisp.attackFrame() == 3;
        double startingX = wisp.x();

        wisp.update(Wisp.LUNGE_DURATION / 2, 50, 0);
        assert wisp.x() > startingX : "lunge must move toward player";
        assert wisp.attackFrame() == 4;
        assert wisp.renderScale() == 1 : "lunge art must not be code-stretched";

        wisp.update(Wisp.LUNGE_DURATION / 2, 50, 0);
        assert wisp.state() == Wisp.State.RECOVER;
        assert wisp.attackFrame() == 6;

        wisp.update(0.34, 50, 0);
        assert wisp.attackFrame() == 7 : "recovery must settle before returning to walk";

        wisp.update(0.11, 50, 0);
        assert wisp.state() == Wisp.State.PATROL;
        assert !wisp.usesAttackAnimation();
    }

    private static void deathShrinksToNothing() {
        Wisp wisp = new Wisp(100, 100, 0, 200);

        assert wisp.hitFrom(0, 100, 1, 0, 120, Player.BLADE_DAMAGE);
        assert !wisp.alive();
        assert wisp.visible() : "death pose must remain briefly visible";

        wisp.update(Wisp.DEATH_DURATION);
        assert !wisp.visible() : "death pose must finish";
        assert wisp.renderScale() == 0 : "death pose must shrink away";
    }

    private static void scoutHealthCanBeBalancedPerEncounter() {
        Wisp scout = new Wisp(100, 100, 0, 200, 1);

        assert scout.maxHealth() == 1;
        assert scout.hurt(Player.SLIME_DAMAGE);
        assert !scout.alive() : "one-health scout must die to a Slime body slam";
    }
}
