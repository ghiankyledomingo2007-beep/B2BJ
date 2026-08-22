public final class Player {
    public static final double SPEED = 260.0;
    public static final double RADIUS = 48.0;

    private double x;
    private double y;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void move(int horizontal, int vertical, double seconds, RuinedOutpostMap map) {
        double length = Math.hypot(horizontal, vertical);
        if (length > 0) {
            double nextX = clamp(x + horizontal / length * SPEED * seconds,
                    RADIUS, map.worldWidth() - RADIUS);
            if (!map.isBlocked(nextX, y, RADIUS)) {
                x = nextX;
            }

            double nextY = clamp(y + vertical / length * SPEED * seconds,
                    RADIUS, map.worldHeight() - RADIUS);
            if (!map.isBlocked(x, nextY, RADIUS)) {
                y = nextY;
            }
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
