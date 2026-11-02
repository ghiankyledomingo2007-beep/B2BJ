public final class SlimeAnimation {
    public static final int CELL_SIZE = 48;
    public static final int RENDER_SIZE = CELL_SIZE * 2;

    private static final int COLUMNS = 4;
    private static final double IDLE_FPS = 4.0;
    private static final double MOVE_FPS = 10.0;
    private static final double DASH_FPS = 25.0;
    private static final double ATTACK_DURATION = 0.32;

    private int facingX;
    private int facingY = 1;
    private boolean moving;
    private boolean dashing;
    private boolean attacking;
    private double time;
    private double castDuration=ATTACK_DURATION,releaseTime=.12;

    public void update(int horizontal, int vertical, double seconds) {
        update(horizontal, vertical, false, seconds);
    }

    public void update(int horizontal, int vertical, boolean dashing, double seconds) {
        moving = horizontal != 0 || vertical != 0;
        if (moving && !attacking) {
            facingX = horizontal;
            facingY = vertical;
        }
        if (dashing) {
            attacking = false;
        }
        if (attacking) {
            time += seconds;
            if (time >= castDuration) {
                attacking = false;
                time = 0;
            }
        } else if (this.dashing != dashing) {
            time = 0;
        } else {
            time += seconds;
        }
        this.dashing = dashing;
    }

    public int row() {
        if (Math.abs(facingX) > Math.abs(facingY)) {
            return dashing ? 10 : attacking ? 3 : moving ? 7 : 3;
        }
        if (facingY < 0) {
            return dashing ? 11 : attacking ? 4 : moving ? 8 : 4;
        }
        return dashing ? 5 : attacking ? 0 : moving ? 6 : 0;
    }

    /** Hopping along under its own power: the moment a footfall sound belongs to. */
    public boolean hopping() { return moving && !attacking && !dashing; }

    public int frame() {
        if (attacking) return (int)(time*8)%COLUMNS;
        double framesPerSecond = dashing ? DASH_FPS
                : moving ? MOVE_FPS : IDLE_FPS;
        return (int) (time * framesPerSecond) % COLUMNS;
    }
    public int idleFrame() { return (int)(time*6)%8; }
    public int castFrame() {
        return Math.min(7,(int)(time<releaseTime?time/releaseTime*3:3+(time-releaseTime)/(castDuration-releaseTime)*5));
    }

    /** Follow simulation time so release, hit pause and recovery cannot drift apart. */
    public void pose(double elapsed,double duration,double release) {
        if(!Double.isFinite(elapsed+duration+release)||duration<=release||release<=0)
            throw new IllegalArgumentException("Invalid slime pose timing");
        attacking=elapsed>=0&&elapsed<duration;
        castDuration=duration;releaseTime=release;time=attacking?elapsed:0;
    }

    public void cancelAttack() {if(attacking){attacking=false;time=0;}}

    public void attack() {
        attacking = true;
        castDuration=ATTACK_DURATION;releaseTime=.12;
        time = 0;
    }

    public void face(int x,int y) { facingX=x; facingY=y; }

    public boolean attacking() {
        return attacking;
    }

    public boolean flipHorizontal() {
        return Math.abs(facingX) > Math.abs(facingY) && facingX > 0;
    }

    public int facingHorizontal() {
        return facingX;
    }

    public int facingVertical() {
        return facingY;
    }
}
