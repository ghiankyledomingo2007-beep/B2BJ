public final class RuinedOutpostMap {
    public static final int TILE_SIZE = 64;

    private static final int WIDTH = 64;
    private static final int HEIGHT = 36;
    private static final double PILLAR_X = 35.0 * TILE_SIZE;
    private static final double PILLAR_Y = 16.0 * TILE_SIZE;
    private static final double PILLAR_HALF_WIDTH = 170.0;
    private static final double PILLAR_BASE_TOP = PILLAR_Y + 190.0;
    private static final double PILLAR_BASE_BOTTOM = PILLAR_Y + 340.0;

    private final boolean[][] stoneVertices = new boolean[HEIGHT + 1][WIDTH + 1];

    public RuinedOutpostMap() {
        for (int y = 0; y <= HEIGHT; y++) {
            for (int x = 0; x <= WIDTH; x++) {
                stoneVertices[y][x] = isOutpostStone(x, y);
            }
        }
    }

    public int tileMask(int x, int y) {
        int mask = stoneVertices[y][x] ? 1 : 0;
        mask |= stoneVertices[y][x + 1] ? 2 : 0;
        mask |= stoneVertices[y + 1][x] ? 4 : 0;
        mask |= stoneVertices[y + 1][x + 1] ? 8 : 0;
        return mask;
    }

    public int widthInTiles() {
        return WIDTH;
    }

    public int heightInTiles() {
        return HEIGHT;
    }

    public int worldWidth() {
        return WIDTH * TILE_SIZE;
    }

    public int worldHeight() {
        return HEIGHT * TILE_SIZE;
    }

    public double spawnX() {
        return 32.0 * TILE_SIZE;
    }

    public double spawnY() {
        return 18.0 * TILE_SIZE;
    }

    public double pillarX() {
        return PILLAR_X;
    }

    public double pillarY() {
        return PILLAR_Y;
    }

    public double pillarSortY() {
        return PILLAR_Y + 250.0;
    }

    public boolean isBlocked(double centerX, double centerY, double radius) {
        double closestX = Math.max(PILLAR_X - PILLAR_HALF_WIDTH,
                Math.min(PILLAR_X + PILLAR_HALF_WIDTH, centerX));
        double closestY = Math.max(PILLAR_BASE_TOP,
                Math.min(PILLAR_BASE_BOTTOM, centerY));
        double dx = centerX - closestX;
        double dy = centerY - closestY;
        return dx * dx + dy * dy < radius * radius;
    }

    private static boolean isOutpostStone(int x, int y) {
        double courtyardX = (x - 32.0) / 18.0;
        double courtyardY = (y - 18.0) / 12.0;
        boolean courtyard = courtyardX * courtyardX + courtyardY * courtyardY <= 1.0;
        boolean westApproach = x <= 18 && Math.abs(y - 19) <= 3;
        double eastRoadY = 18.0 + Math.sin((x - 46.0) * 0.35) * 2.0;
        boolean eastApproach = x >= 46 && Math.abs(y - eastRoadY) <= 3;
        boolean northWatch = x >= 27 && x <= 37 && y >= 3 && y <= 10;

        boolean craterOne = distanceSquared(x, y, 22, 13) <= 4;
        boolean craterTwo = distanceSquared(x, y, 42, 24) <= 6;
        boolean brokenGround = craterOne || craterTwo;
        return (courtyard || westApproach || eastApproach || northWatch) && !brokenGround;
    }

    private static int distanceSquared(int x, int y, int centerX, int centerY) {
        int dx = x - centerX;
        int dy = y - centerY;
        return dx * dx + dy * dy;
    }
}
