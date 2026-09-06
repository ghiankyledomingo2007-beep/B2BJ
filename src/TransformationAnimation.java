public final class TransformationAnimation {
    public static final int CELL_SIZE = 80;
    public static final int RENDER_SIZE = CELL_SIZE * 2;
    public static final int ROW = 0;
    public static final double DURATION = 0.64;

    private static final int FRAMES = 16;

    private boolean active;
    private double time;
    private boolean reverting;

    public void start() {
        start(false);
    }
    public void start(boolean reverse) {
        active = true;
        reverting = reverse;
        time = 0;
    }
    public boolean reverting() { return reverting; }
    public void cancel() { active=false; }

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
