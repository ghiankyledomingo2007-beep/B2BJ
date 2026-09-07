public final class BladeAnimation {
    public enum Action {
        IDLE,
        RUN,
        DASH,
        SLASH, CAST, GUARD, HURT
    }

    public static final int CELL_WIDTH = 80;
    public static final int CELL_HEIGHT = 80;
    public static final int FOOT_ROW = 72;
    public static final int RENDER_WIDTH = CELL_WIDTH * 2;
    public static final int RENDER_HEIGHT = CELL_HEIGHT * 2;

    private static final double RUN_FPS = 14.0;
    private static final double SLASH_DURATION = 0.3;
    private static final double SLASH_FPS = 8.0 / SLASH_DURATION;

    private Action action = Action.IDLE;
    private int facingX;
    private int facingY = 1;
    private double time;
    private double duration;

    public void update(int horizontal, int vertical, boolean dashing, double seconds) {
        boolean moving = horizontal != 0 || vertical != 0;
        if (moving && duration == 0) {
            facingX = horizontal;
            facingY = vertical;
        }

        if (duration > 0) {
            time += seconds;
            if (time < duration && !dashing) {
                return;
            }
            duration = 0;
        }

        Action next = dashing ? Action.DASH : moving ? Action.RUN : Action.IDLE;
        if (next != action) {
            action = next;
            time = 0;
        } else {
            time += seconds;
        }
    }

    public void slash() {
        play(Action.SLASH, SLASH_DURATION);
    }
    public void play(Action next,double seconds) { action=next;duration=seconds;time=0; }
    public double elapsed() { return time; }

    public void face(int x,int y) { facingX=x; facingY=y; }

    public Action action() {
        return action;
    }

    public int row() {
        if (Math.abs(facingX) > Math.abs(facingY)) {
            return 1;
        }
        return facingY < 0 ? 2 : 0;
    }

    public int frame() {
        return switch (action) {
            case IDLE -> (int)(time*6)%8;
            case RUN -> (int) (time * RUN_FPS) % 8;
            case DASH -> Math.min(7,(int)(time*8/Player.DASH_DURATION));
            case SLASH -> Math.min(7, (int) (time * SLASH_FPS));
            case CAST, GUARD, HURT -> Math.min(7,(int)(time*8/duration));
        };
    }

    public boolean flipHorizontal() {
        return Math.abs(facingX) > Math.abs(facingY) && facingX < 0;
    }

    /** Transparent weapon margin; body pixels still render at the shared 2x scale. */
    public int padding() {
        return 0;
    }

    public int facingHorizontal() {
        return facingX;
    }

    public int facingVertical() {
        return facingY;
    }

    public String sheetPath() {
        return "assets/characters/blade/rainoray_"+action.name().toLowerCase(java.util.Locale.ROOT)+".png";
    }
}
