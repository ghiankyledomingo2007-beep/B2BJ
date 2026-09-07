import java.util.List;

public final class CampaignEnemyTest {
    public static void main(String[] args) {
        shieldRequiresBladeOrAnOpening();
        knightCommitsAStationaryOverhead();
        chargeHasMoreReachThanSwarmLunge();
        impWeavesThenCommitsToTheActualPlayer();
        castersHaveDifferentCommittedPatterns();
        interruptedReleaseCannotFire();
        invalidInputsCannotCorruptMovement();
        System.out.println("CampaignEnemyTest passed");
    }

    private static CampaignEnemy enemy(CampaignEnemy.Kind kind) {
        return new CampaignEnemy(kind, 500, 500);
    }

    private static void advanceTo(CampaignEnemy enemy, Wisp.State state) {
        for (int i = 0; i < 1000 && enemy.body().state() != state; i++)
            enemy.update(.01, 700, 500, null);
        assert enemy.body().state() == state : "enemy must reach " + state;
    }

    private static void shieldRequiresBladeOrAnOpening() {
        var knight = enemy(CampaignEnemy.Kind.FALLEN_KNIGHT);
        int health = knight.body().health();
        assert knight.shielded();
        assert !knight.hurt(3, true, true) : "shield blocks water and slime impacts";
        assert knight.body().health() == health;
        assert knight.hurt(4, false, true) : "blade penetrates shield stance";
        assert knight.body().health() == health - 4;

        var recovering = enemy(CampaignEnemy.Kind.FALLEN_KNIGHT);
        advanceTo(recovering, Wisp.State.RECOVER);
        assert !recovering.shielded();
        assert recovering.hurt(1, true, false) : "committed attack leaves a punishable opening";
    }

    private static void chargeHasMoreReachThanSwarmLunge() {
        var imp = enemy(CampaignEnemy.Kind.RIFT_IMP);
        var wolf = enemy(CampaignEnemy.Kind.THORN_WOLF);
        advanceTo(imp, Wisp.State.LUNGE);
        advanceTo(wolf, Wisp.State.LUNGE);
        double impX = imp.body().x(), wolfX = wolf.body().x();
        advanceTo(imp, Wisp.State.RECOVER);
        advanceTo(wolf, Wisp.State.RECOVER);
        assert wolf.body().x() - wolfX > (imp.body().x() - impX) * 1.7
                : "wolf charge covers substantially more ground than an imp swipe";
    }

    private static void knightCommitsAStationaryOverhead() {
        var knight = enemy(CampaignEnemy.Kind.FALLEN_KNIGHT);
        advanceTo(knight, Wisp.State.TELEGRAPH);
        assert !knight.hits(knight.body().x() + 100, knight.body().y());
        advanceTo(knight, Wisp.State.LUNGE);
        double x = knight.body().x(), y = knight.body().y();
        assert knight.hits(x + 120, y + 40) : "overhead strikes the warned lane";
        assert !knight.hits(x - 1, y) && !knight.hits(x + 136, y) && !knight.hits(x + 100, y + 51)
                : "behind, past and beside the warned lane stay safe";
        knight.update(.1, x - 200, y, null);
        assert knight.body().x() == x && knight.body().y() == y : "overhead cannot become a wolf charge";
        assert knight.hits(x + 100, y) && !knight.hits(x - 100, y)
                : "swing keeps its committed facing";
    }

    private static void castersHaveDifferentCommittedPatterns() {
        var shaman = enemy(CampaignEnemy.Kind.MIRE_SHAMAN);
        var hexer = enemy(CampaignEnemy.Kind.CINDER_HEXER);
        for (var caster : List.of(shaman, hexer)) {
            advanceTo(caster, Wisp.State.TELEGRAPH);
            assert caster.releaseShots().isEmpty() : "tell cannot damage";
            advanceTo(caster, Wisp.State.LUNGE);
        }
        List<EnemyProjectile> fan = shaman.releaseShots();
        List<EnemyProjectile> ring = hexer.releaseShots();
        assert fan.size() == 3 && ring.size() == 8;
        assert fan.stream().allMatch(shot -> shot.directionX() > .9)
                : "shaman fan follows committed aim";
        assert ring.stream().anyMatch(shot -> shot.directionX() < -.9)
                : "hexer ring attacks its rear as well as its aim direction";
        assert shaman.releaseShots().isEmpty() && hexer.releaseShots().isEmpty()
                : "one pattern per committed release";
        double x = ring.get(0).x();
        ring.get(0).advance(.1);
        assert ring.get(0).x() > x : "reuse real moving hostile projectiles";
    }

    private static void impWeavesThenCommitsToTheActualPlayer() {
        var imp = enemy(CampaignEnemy.Kind.RIFT_IMP);
        imp.update(.4, 810, 500, null);
        assert imp.body().x() > 500 && Math.abs(imp.body().y() - 500) > 4
                : "imp approach weaves without teleporting";
        for (int i = 0; i < 400 && imp.body().state() != Wisp.State.TELEGRAPH; i++)
            imp.update(.01, 810, 500, null);
        assert imp.body().state() == Wisp.State.TELEGRAPH;
        double dx = 810 - imp.body().x(), dy = 500 - imp.body().y();
        double distance = Math.hypot(dx, dy);
        assert Math.abs(imp.body().intentX() - dx / distance) < 1e-8
                && Math.abs(imp.body().intentY() - dy / distance) < 1e-8
                : "swipe tell aims at real player, never the weave offset";
    }

    private static void interruptedReleaseCannotFire() {
        for (int damage : new int[]{1, 100}) {
            var caster = enemy(CampaignEnemy.Kind.MIRE_SHAMAN);
            advanceTo(caster, Wisp.State.LUNGE);
            caster.hurt(damage, false, true);
            assert caster.releaseShots().isEmpty() : "outgoing strike cancels an unconsumed release";
            if (damage == 100) {
                caster.update(.5, 700, 500, null);
                assert caster.releaseShots().isEmpty() && !caster.body().alive();
            }
        }
    }

    private static void invalidInputsCannotCorruptMovement() {
        var imp = enemy(CampaignEnemy.Kind.RIFT_IMP);
        for (double dt : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, 0})
            imp.update(dt, 700, 500, null);
        imp.update(.1, Double.NaN, 500, null);
        assert imp.body().x() == 500 && imp.body().y() == 500;
        boolean rejected = false;
        try { new CampaignEnemy(CampaignEnemy.Kind.RIFT_IMP, Double.NaN, 0); }
        catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected : "invalid spawn coordinates are rejected";
    }
}
