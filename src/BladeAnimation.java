public final class BladeAnimation {
    public enum Action {
        IDLE,
        RUN,
        DASH,
        SLASH
    }

    public static final int CELL_WIDTH = 48;
    public static final int CELL_HEIGHT = 64;
    public static final int RENDER_WIDTH = CELL_WIDTH * 2;
    public static final int RENDER_HEIGHT = CELL_HEIGHT * 2;

    private static final double RUN_FPS = 10.0;
    private static final double DASH_FPS = 12.0;
    private static final double SLASH_DURATION = 0.3;
    private static final double SLASH_FPS = 8.0 / SLASH_DURATION;

    private Action action = Action.IDLE;
    private int facingX;
    private int facingY = 1;
    private double time;

    public void update(int horizontal, int vertical, boolean dashing, double seconds) {
        boolean moving = horizontal != 0 || vertical != 0;
        if (moving && action != Action.SLASH) {
            facingX = horizontal;
            facingY = vertical;
        }

        if (action == Action.SLASH) {
            time += seconds;
            if (time < SLASH_DURATION) {
                return;
            }
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
        action = Action.SLASH;
        time = 0;
    }

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
            case IDLE -> 0;
            case RUN -> (int) (time * RUN_FPS) % 4;
            case DASH -> (int) (time * DASH_FPS) % 3;
            // ponytail: retain the legacy side grid until a replacement passes native-scale art review.
            case SLASH -> row() == 1 ? Math.min(2, (int) (time * 10.0))
                    : Math.min(7, (int) (time * SLASH_FPS));
        };
    }

    public boolean flipHorizontal() {
        return Math.abs(facingX) > Math.abs(facingY) && facingX < 0;
    }

    /** Transparent weapon margin; body pixels still render at the shared 2x scale. */
    public int padding() {
        return action == Action.SLASH && row() != 1 ? 8 : 0;
    }

    public int facingHorizontal() {
        return facingX;
    }

    public int facingVertical() {
        return facingY;
    }

    public String sheetPath() {
        return switch (action) {
            case IDLE, RUN -> "assets/characters/blade/blade_run.png";
            case DASH -> "assets/characters/blade/blade_dash.png";
            case SLASH -> row() == 1 ? "assets/characters/blade/blade_slash.png"
                    : "assets/characters/blade/blade_cut.png";
        };
    }
}
