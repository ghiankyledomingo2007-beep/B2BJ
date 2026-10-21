public final class BladeAnimation {
    public enum Action {
        IDLE,
        RUN,
        DASH,
        SLASH, CAST, GUARD, HURT, START, STOP, TURN, CHARGE, HOLD, CLEAVE, COUNTER
    }

    public static final int CELL_WIDTH = 80;
    public static final int CELL_HEIGHT = 80;
    public static final int FOOT_ROW = 72;
    public static final int RENDER_WIDTH = CELL_WIDTH * 2;
    public static final int RENDER_HEIGHT = CELL_HEIGHT * 2;

    public static final int RUN_FRAMES = 8;

    /** Frames per row of an action's sheet. A sheet's cell size is its width divided by this. */
    public static int columns(Action action) {
        return switch (action) {
            case SLASH, CLEAVE -> 12;
            case START, STOP, TURN -> 4;
            case RUN -> RUN_FRAMES;
            default -> 8;
        };
    }
    // One authored two-step stride; tune cadence independently from movement speed.
    // 8 poses at 16 fps = a 0.5 s sprint stride; fewer, held poses read punchier than 12 at 20.
    private static final double RUN_FPS = 16.0;
    private static final double SLASH_DURATION = 0.3;
    private static final double SLASH_FPS = 8.0 / SLASH_DURATION;

    private Action action = Action.IDLE;
    private int facingX;
    private int facingY = 1;
    private double time;
    private double duration;

    public void update(int horizontal, int vertical, boolean dashing, double seconds) {
        boolean moving=horizontal!=0||vertical!=0;
        int oldDirection=directionRow();
        boolean transition=action==Action.START||action==Action.STOP||action==Action.TURN;
        if(duration>0&&!transition&&!dashing) {
            time+=seconds;
            if(time<duration)return;
            duration=0;
        }
        if(moving)face(horizontal,vertical);
        if(dashing) {
            if(action!=Action.DASH){action=Action.DASH;time=0;duration=0;}else time+=seconds;
            return;
        }
        if(moving) {
            if(action==Action.IDLE||action==Action.STOP)play(Action.START,.12);
            // A direction change keeps the stride clock; a standing shuffle would slide over the floor.
            else if(action==Action.RUN&&oldDirection!=directionRow())time+=seconds;
            else if(transition&&oldDirection!=directionRow())play(Action.START,.12);
            else if(action==Action.START||action==Action.TURN) {
                time+=seconds;
                if(time>=duration){action=Action.RUN;duration=0;time=0;}
            } else if(action!=Action.RUN){action=Action.RUN;duration=0;time=0;}
            else time+=seconds;
        } else {
            if(action==Action.RUN||action==Action.START||action==Action.TURN||action==Action.DASH)play(Action.STOP,.12);
            else if(action==Action.STOP) {
                time+=seconds;
                if(time>=duration){action=Action.IDLE;time=0;duration=0;}
            } else if(action!=Action.IDLE){action=Action.IDLE;time=0;duration=0;}
            else time+=seconds;
        }
    }

    public void slash() {
        play(Action.SLASH, SLASH_DURATION);
    }
    public void play(Action next,double seconds) { action=next;duration=seconds;time=0; }
    public double elapsed() { return time; }

    public void pose(Action next,double elapsed,double total) {action=next;time=elapsed;duration=total;}
    public boolean canAim() {return duration==0||action==Action.STOP||action==Action.TURN;}
    public int directionRow() {return Math.abs(facingX)>Math.abs(facingY)?facingX<0?3:1:facingY<0?2:0;}
    public void face(int x,int y) { if(x!=0||y!=0){facingX=x; facingY=y;} }

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
            case RUN -> (int) (time * RUN_FPS) % RUN_FRAMES;
            case DASH -> Math.min(7,(int)(time*8/Player.BLADE_DASH_DURATION));
            case SLASH -> Math.min(7, (int) (time * SLASH_FPS));
            case START, STOP, TURN -> Math.min(3,(int)(time*4/.12));
            case CHARGE -> Math.min(7,(int)(time*8/.35));
            case HOLD -> (int)(time*8)%8;
            case CLEAVE -> B2BJ.bladeAttackFrame(time,4);
            case CAST, GUARD, HURT, COUNTER -> Math.min(7,(int)(time*8/duration));
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
