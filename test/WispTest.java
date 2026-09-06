public final class WispTest {
    public static void main(String[] args) {
        patrolsInsideItsLaneAndTurnsAround();
        lungeStopsAtVisibleMapCollision();
        System.out.println("WispTest passed");
    }

    private static void patrolsInsideItsLaneAndTurnsAround() {
        Wisp wisp = new Wisp(0, 20, 0, 100);

        wisp.update(1.0);
        assert wisp.x() == Wisp.SPEED : "Wisp must walk east at patrol speed";
        assert wisp.animation().row() == 1 : "east patrol must use east animation row";

        wisp.update(1.0);
        assert wisp.x() == 100 : "Wisp must stop at patrol boundary";
        assert wisp.animation().row() == 3 : "Wisp must face west after turning";

        wisp.update(0.5);
        assert wisp.x() == 100 - Wisp.SPEED * 0.5 : "Wisp must walk back west";
        assert wisp.y() == 20 : "horizontal patrol must keep its floor line";
    }

    private static void lungeStopsAtVisibleMapCollision() {
        RuinedOutpostMap map = new RuinedOutpostMap(1);
        RuinedOutpostMap.Obstacle barrier = map.barriers().get(0);
        double startX = barrier.centerX() - barrier.width() / 2 - 54;
        double startY = barrier.centerY() - Wisp.COLLISION_Y_OFFSET;
        Wisp wisp = new Wisp(startX, startY, startX, startX, 2);

        wisp.update(0.01, barrier.centerX() + 80, startY, map);
        wisp.update(Wisp.TELEGRAPH_DURATION,
                barrier.centerX() + 80, startY, map);
        wisp.update(Wisp.LUNGE_DURATION,
                barrier.centerX() + 80, startY, map);

        assert !map.isBlocked(wisp.x(), wisp.y() + Wisp.COLLISION_Y_OFFSET,
                Wisp.COLLISION_RADIUS)
                : "Wisp must never end a lunge inside visible collision";
        assert wisp.x() < barrier.centerX() - barrier.width() / 2
                : "solid barricade must stop a Wisp lunge";
    }
}
