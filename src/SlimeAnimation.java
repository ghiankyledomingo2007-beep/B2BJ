public final class SlimeAnimation {
    public static final int CELL_SIZE = 48;

    private static final int COLUMNS = 4;
    private static final double IDLE_FPS = 4.0;
    private static final double MOVE_FPS = 10.0;

    private int facingX;
    private int facingY = 1;
    private boolean moving;
    private double time;

    public void update(int horizontal, int vertical, double seconds) {
        moving = horizontal != 0 || vertical != 0;
        if (moving) {
            facingX = horizontal;
            facingY = vertical;
        }
        time += seconds;
    }

    public int row() {
        if (Math.abs(facingX) > Math.abs(facingY)) {
            return moving ? 7 : 3;
        }
        if (facingY < 0) {
            return moving ? 8 : 4;
        }
        return moving ? 6 : 0;
    }

    public int frame() {
        return (int) (time * (moving ? MOVE_FPS : IDLE_FPS)) % COLUMNS;
    }

    public boolean flipHorizontal() {
        return Math.abs(facingX) > Math.abs(facingY) && facingX > 0;
    }
}
