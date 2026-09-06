/** Regression coverage for audit findings 3, 4 and 6: aggro, awareness, pursuit, cover routing, locked tells. */
public final class WispPursuitTest {
    private static final double DT = 0.01;
    private static final double LUNGE_REACH = 150;

    public static void main(String[] args) {
        damageEstablishesSustainedAggro();
        awarenessCoversWaterReach();
        pursuesInsteadOfLungingFromRange();
        routesAroundCoverFromNearerEnd();
        routesAroundTallCoverVertically();
        lungeDirectionLocksBeforeLaunch();
        blockedLungeEndsEarlyAndSeparationRespectsCover();
        aggroExpiresWhenTargetStaysAway();
        System.out.println("WispPursuitTest passed");
    }

    private static void damageEstablishesSustainedAggro() {
        Wisp calm = new Wisp(700, 500, 700, 700, 2);
        for (int i = 0; i < 100; i++) calm.update(DT, 1150, 500);
        assert calm.state() == Wisp.State.PATROL : "an untouched enemy far outside reach keeps patrolling";

        Wisp struck = new Wisp(700, 500, 700, 700, 2);
        assert struck.hurt(1, false) : "basic water hit lands without stagger";
        double start = distance(struck, 1150, 500);
        boolean attacked = false;
        for (int i = 0; i < 800 && !attacked; i++) {
            struck.update(DT, 1150, 500);
            attacked = struck.state() == Wisp.State.LUNGE;
        }
        assert distance(struck, 1150, 500) < start - 100 : "damage must wake the enemy and make it close distance";
        assert attacked : "damaged enemy must keep pursuing until it can attack";
    }

    private static void awarenessCoversWaterReach() {
        for (int range : new int[]{310, 340}) {
            Wisp enemy = new Wisp(300 + range, 260, 300 + range, 300 + range, 2);
            boolean lunged = false;
            for (int i = 0; i < 400 && !lunged; i++) {
                enemy.update(DT, 300, 260);
                lunged = enemy.state() == Wisp.State.LUNGE;
            }
            assert lunged : "enemy inside water reach (" + range + " px) must engage within four seconds";
        }
    }

    private static void pursuesInsteadOfLungingFromRange() {
        Wisp enemy = new Wisp(580, 260, 580, 580, 2);
        double first = -1;
        for (int i = 0; i < 400 && first < 0; i++) {
            Wisp.State before = enemy.state();
            enemy.update(DT, 300, 260);
            if (before != Wisp.State.LUNGE && enemy.state() == Wisp.State.LUNGE) first = distance(enemy, 300, 260);
        }
        assert first >= 0 : "enemy at 280 px must still reach an attack";
        assert first <= LUNGE_REACH : "lunge must launch from reachable range, not " + first + " px";
    }

    private static void routesAroundCoverFromNearerEnd() {
        for (int side : new int[]{-1, 1}) {
            RuinedOutpostMap map = new RuinedOutpostMap(2);
            RuinedOutpostMap.Obstacle cover = map.barriers().get(0);
            double feet = cover.centerY() + side * 10;
            double px = cover.centerX() - side * 100, py = feet - Wisp.COLLISION_Y_OFFSET;
            double ex = cover.centerX() + side * 100;
            Wisp enemy = new Wisp(ex, py, ex, ex, 2);
            int wallLunges = 0;
            double reachedAt = -1, nearestEnd = feet;
            for (int i = 0; i < 800 && reachedAt < 0; i++) {
                Wisp.State before = enemy.state();
                enemy.update(DT, px, py, map);
                double footY = enemy.y() + Wisp.COLLISION_Y_OFFSET;
                nearestEnd = side < 0 ? Math.min(nearestEnd, footY) : Math.max(nearestEnd, footY);
                boolean clear = map.clearLine(enemy.x(), footY, px, py + Wisp.COLLISION_Y_OFFSET);
                if (before != Wisp.State.LUNGE && enemy.state() == Wisp.State.LUNGE && !clear) wallLunges++;
                assert !map.isBlocked(enemy.x(), footY, Wisp.COLLISION_RADIUS) : "pursuit must never tunnel into cover";
                if (clear && distance(enemy, px, py) <= LUNGE_REACH) reachedAt = i * DT;
            }
            assert wallLunges == 0 : "enemy must not lunge into cover (" + wallLunges + " wall lunges)";
            assert reachedAt >= 0 : "enemy must route around cover within eight seconds";
            double clearance = cover.height() / 2 + Wisp.COLLISION_RADIUS;
            assert side * (nearestEnd - cover.centerY()) > clearance - 1
                    : "enemy must go around the end nearer to its target";
        }
    }

    private static void routesAroundTallCoverVertically() {
        RuinedOutpostMap map = new RuinedOutpostMap(7);
        RuinedOutpostMap.Obstacle cover = map.barriers().get(0);
        double px = cover.centerX() + 20, py = cover.centerY() - cover.height() / 2 - 70;
        double ey = cover.centerY() + cover.height() / 2 + 60 - Wisp.COLLISION_Y_OFFSET;
        Wisp enemy = new Wisp(cover.centerX(), ey, cover.centerX(), cover.centerX(), 2);
        int wallLunges = 0;
        double reachedAt = -1;
        for (int i = 0; i < 800 && reachedAt < 0; i++) {
            Wisp.State before = enemy.state();
            enemy.update(DT, px, py, map);
            double footY = enemy.y() + Wisp.COLLISION_Y_OFFSET;
            boolean clear = map.clearLine(enemy.x(), footY, px, py + Wisp.COLLISION_Y_OFFSET);
            if (before != Wisp.State.LUNGE && enemy.state() == Wisp.State.LUNGE && !clear) wallLunges++;
            assert !map.isBlocked(enemy.x(), footY, Wisp.COLLISION_RADIUS);
            if (clear && distance(enemy, px, py) <= LUNGE_REACH) reachedAt = i * DT;
        }
        assert wallLunges == 0 : "enemy must not lunge into wide cover";
        assert reachedAt >= 0 : "enemy must route around the side of wide cover";
    }

    private static void lungeDirectionLocksBeforeLaunch() {
        Wisp enemy = new Wisp(700, 500, 700, 700, 2);
        enemy.update(DT, 800, 500);
        assert enemy.state() == Wisp.State.TELEGRAPH;
        enemy.update(0.1, 700, 400);
        assert !enemy.directionLocked() && enemy.intentY() < 0 : "early tell tracks the target";
        enemy.update(Wisp.TELEGRAPH_DURATION - DT - 0.1 - 0.03, 800, 500);
        assert enemy.directionLocked() && enemy.intentX() > 0 : "late tell commits to a direction";
        enemy.update(0.02, 600, 500);
        assert enemy.state() == Wisp.State.TELEGRAPH : "tell must still be visible before launch";
        assert enemy.intentX() > 0 : "locked direction ignores late repositioning";
        enemy.update(0.05, 600, 500);
        assert enemy.state() == Wisp.State.LUNGE && enemy.directionLocked();
        double x = enemy.x();
        enemy.update(0.05, 600, 500);
        assert enemy.state() == Wisp.State.LUNGE && enemy.intentX() > 0;
        assert enemy.x() > x : "target crossing behind in the final 20 ms must not flip the lunge";
    }

    private static void blockedLungeEndsEarlyAndSeparationRespectsCover() {
        RuinedOutpostMap map = new RuinedOutpostMap(2);
        RuinedOutpostMap.Obstacle cover = map.barriers().get(0);
        double left = cover.centerX() - cover.width() / 2;
        double y = cover.centerY() - Wisp.COLLISION_Y_OFFSET;
        // Lunge west into the world edge on the open road row: the line check ignores edges.
        double edgeY = map.spawnY() - Wisp.COLLISION_Y_OFFSET;
        Wisp enemy = new Wisp(60, edgeY, 60, 60, 2);
        enemy.update(DT, 20, edgeY, map);
        assert enemy.state() == Wisp.State.TELEGRAPH : "the target is within reach with a clear line";
        enemy.update(Wisp.TELEGRAPH_DURATION, 20, edgeY, map);
        assert enemy.state() == Wisp.State.LUNGE;
        double elapsed = 0;
        while (enemy.state() == Wisp.State.LUNGE && elapsed < Wisp.LUNGE_DURATION) {
            enemy.update(0.02, 20, edgeY, map);
            elapsed += 0.02;
        }
        assert enemy.state() == Wisp.State.RECOVER && elapsed < Wisp.LUNGE_DURATION - 0.02
                : "a lunge stopped by solid terrain recovers early instead of grinding";
        assert !map.isBlocked(enemy.x(), enemy.y() + Wisp.COLLISION_Y_OFFSET, Wisp.COLLISION_RADIUS);

        Wisp a = new Wisp(left - 25, y, left - 25, left - 25, 2);
        Wisp b = new Wisp(left - 25, y, left - 25, left - 25, 2);
        assert a.separateFrom(b, DT, map) | b.separateFrom(a, DT, map) : "overlapping enemies separate";
        for (int i = 0; i < 100; i++) {
            a.separateFrom(b, DT, map);
            b.separateFrom(a, DT, map);
        }
        assert Math.hypot(a.x() - b.x(), a.y() - b.y()) >= 2 * Wisp.COLLISION_RADIUS - 0.01
                : "separation must reach clear silhouettes";
        for (Wisp w : new Wisp[]{a, b}) {
            assert !map.isBlocked(w.x(), w.y() + Wisp.COLLISION_Y_OFFSET, Wisp.COLLISION_RADIUS)
                    : "separation must never push into cover";
        }
        assert !a.separateFrom(b, DT, map) : "separated enemies stay put";
        assert !a.separateFrom(a, DT, map) && !a.separateFrom(null, DT, map);
    }

    private static void aggroExpiresWhenTargetStaysAway() {
        // Target sits farther than awareness plus everything the hunt can cover before it lapses.
        double farY = 500 + Wisp.AWARENESS_RANGE + Wisp.PURSUE_SPEED * Wisp.AGGRO_DURATION + 200;
        Wisp enemy = new Wisp(700, 500, 640, 760, 2);
        assert enemy.hurt(1, false) && enemy.aggro();
        for (int i = 0; i < 50; i++) enemy.update(DT, 700, farY);
        assert enemy.state() == Wisp.State.PURSUE && enemy.y() > 500
                : "a struck enemy hunts even a target out of sight";
        for (int i = 0; i < (int) (Wisp.AGGRO_DURATION / DT) + 10; i++) enemy.update(DT, 700, farY);
        assert !enemy.aggro() && enemy.state() == Wisp.State.PATROL : "aggro is bounded, not eternal";
        for (int i = 0; i < 1500; i++) enemy.update(DT, 700, farY);
        assert Math.abs(enemy.y() - 500) < 0.01 && enemy.x() >= 640 && enemy.x() <= 760
                : "enemy returns to its patrol lane";
    }

    private static double distance(Wisp enemy, double x, double y) {
        return Math.hypot(enemy.x() - x, enemy.y() - y);
    }
}
