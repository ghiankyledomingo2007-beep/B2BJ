public final class PlayerMovementTest {
    private static final double EPSILON = 0.001;

    public static void main(String[] args) {
        movesAtConstantSpeed();
        normalizesDiagonalMovement();
        cannotWalkThroughPillar();
        staysStillWithoutInput();
        staysInsideTheWorld();
        System.out.println("PlayerMovementTest passed");
    }

    private static void movesAtConstantSpeed() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(100, 100);

        player.move(1, 0, 1, map);

        assertClose(360, player.x());
        assertClose(100, player.y());
    }

    private static void normalizesDiagonalMovement() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(100, 100);

        player.move(1, 1, 1, map);

        assertClose(Player.SPEED, Math.hypot(player.x() - 100, player.y() - 100));
    }

    private static void staysInsideTheWorld() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(50, 50);

        player.move(-1, -1, 1, map);

        assertClose(Player.RADIUS, player.x());
        assertClose(Player.RADIUS, player.y());
    }

    private static void staysStillWithoutInput() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(100, 100);

        player.move(0, 0, 1, map);

        assertClose(100, player.x());
        assertClose(100, player.y());
    }

    private static void cannotWalkThroughPillar() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(
                map.pillarX() - Player.RADIUS - 180,
                map.pillarY() + 250);
        double startingX = player.x();

        player.move(1, 0, 0.05, map);

        assertClose(startingX, player.x());
    }

    private static void assertClose(double expected, double actual) {
        assert Math.abs(expected - actual) < EPSILON
                : "expected " + expected + ", got " + actual;
    }
}
