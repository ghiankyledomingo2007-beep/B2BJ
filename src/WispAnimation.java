public final class WispAnimation {
    public static final int CELL_SIZE = 48;
    public static final int RENDER_SIZE = CELL_SIZE * 2;

    private static final int FRAMES = 8;
    private static final double WALK_FPS = 8.0;

    private int facingX;
    private int facingY = 1;
    private double time;

    public void update(int horizontal, int vertical, double seconds) {
        if (horizontal != 0 || vertical != 0) {
            facingX = horizontal;
            facingY = vertical;
        }
        time += seconds;
    }

    public int row() {
        if (Math.abs(facingX) > Math.abs(facingY)) {
            return facingX > 0 ? 1 : 3;
        }
        return facingY < 0 ? 2 : 0;
    }

    public int frame() {
        return (int) (time * WALK_FPS) % FRAMES;
    }
}
