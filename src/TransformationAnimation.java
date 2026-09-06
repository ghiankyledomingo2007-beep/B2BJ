public final class TransformationAnimation {
    public static final int CELL_SIZE = 48;
    public static final int RENDER_SIZE = CELL_SIZE * 2;
    public static final int ROW = 3;
    public static final double DURATION = 0.64;

    private static final int FRAMES = 8;

    private boolean active;
    private double time;

    public void start() {
        active = true;
        time = 0;
    }

    public void update(double seconds) {
        if (!active) {
            return;
        }
        time += seconds;
        if (time >= DURATION) {
            active = false;
        }
    }

    public boolean active() {
        return active;
    }

    public int frame() {
        return Math.min(FRAMES - 1, (int) (time * FRAMES / DURATION));
    }
}
