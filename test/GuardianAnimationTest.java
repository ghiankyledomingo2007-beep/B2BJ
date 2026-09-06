public final class GuardianAnimationTest {
    public static void main(String[] args) {
        dormantIdleLoopsWithoutActivating();
        stateSequenceUsesSeparateEightFrameRows();
        specialAttacksKeepTheirOwnDurations();
        assert Guardian.CELL_SIZE==64&&Guardian.RENDER_SIZE==192&&Guardian.SPRITE_FOOT_ROW==56;
        assert Guardian.MAX_HEALTH==240&&Guardian.SLAM_DAMAGE==2&&Guardian.SLAM_RADIUS==112;
        System.out.println("GuardianAnimationTest passed");
    }
    private static void dormantIdleLoopsWithoutActivating() {
        Guardian guardian=new Guardian(400,300);
        int seen=0;
        for(int step=0;step<160;step++) {
            assert guardian.animation()==Guardian.Animation.IDLE;
            seen|=1<<guardian.animationFrame();guardian.update(.01,100,100);
        }
        assert seen==255 : "dormant idle must visit all eight breathing poses";
        assert guardian.animationFrame()<4 : "idle loops instead of holding its final pose";
        assert guardian.state()==Guardian.State.DORMANT&&guardian.stateSeconds()==0;
        assert guardian.x()==400&&guardian.y()==300&&guardian.health()==240&&guardian.impactNumber()==0;
    }
    private static void stateSequenceUsesSeparateEightFrameRows() {
        Guardian guardian=new Guardian(300,200);guardian.activate(120,140);
        oneShot(guardian,Guardian.Animation.WINDUP,.9,999,999);
        assert guardian.targetX()==120&&guardian.targetY()==140 : "animation cannot retarget a committed warning";
        assert guardian.impactNumber()==1&&guardian.hits(120,140);
        oneShot(guardian,Guardian.Animation.STRIKE,.18,999,999);
        oneShot(guardian,Guardian.Animation.RECOVER,1.5,999,999);
        assert guardian.animation()==Guardian.Animation.APPROACH;
        guardian.update(.6,999,999);int first=guardian.animationFrame();
        guardian.update(.5,999,999);
        assert guardian.animation()==Guardian.Animation.APPROACH&&guardian.animationFrame()<first
                : "approach loops its walking poses while movement continues";
        assert guardian.x()>300&&guardian.y()>200;
        guardian.hurt(Guardian.MAX_HEALTH);
        oneShot(guardian,Guardian.Animation.DEATH,Guardian.DEATH_DURATION,999,999);
        guardian.update(1,999,999);
        assert guardian.animationFrame()==7&&!guardian.visible() : "death holds final pose, never loops";
    }
    private static void specialAttacksKeepTheirOwnDurations() {
        Guardian guardian=new Guardian(800,400);guardian.activate(1400,400);
        seek(guardian,Guardian.Animation.CHARGE,Guardian.Attack.CHARGE);
        double x=guardian.x();
        oneShot(guardian,Guardian.Animation.CHARGE,.55,x+600,400);
        assert Math.abs(guardian.x()-x-620*.55)<.001 : "charge animation must not change displacement";
        guardian.hurt(120);
        oneShot(guardian,Guardian.Animation.RECOVER,1.25,guardian.x()+600,400);
        seek(guardian,Guardian.Animation.WINDUP,Guardian.Attack.FISSURE);
        var lane=guardian.fissureBounds();
        oneShot(guardian,Guardian.Animation.WINDUP,1.1,100,100);
        assert lane.equals(guardian.fissureBounds());
        oneShot(guardian,Guardian.Animation.STRIKE,.32,100,100);
        seek(guardian,Guardian.Animation.STRIKE,Guardian.Attack.SHOCKWAVE);
        oneShot(guardian,Guardian.Animation.STRIKE,.75,guardian.x()+600,400);
    }
    private static void seek(Guardian guardian,Guardian.Animation animation,Guardian.Attack attack) {
        for(int step=0;step<20000;step++) {
            if(guardian.animation()==animation&&guardian.attack()==attack)return;
            guardian.update(.005,guardian.x()+600,400);
        }
        throw new AssertionError("animation state never reached: "+animation+" / "+attack);
    }
    private static void oneShot(Guardian guardian,Guardian.Animation animation,double duration,double px,double py) {
        assert guardian.animation()==animation : "wrong row at start of "+animation;
        assert guardian.animationFrame()==0 : "one-shot must start at authored pose zero";
        int seen=0,previous=-1;
        for(double elapsed=0;elapsed<duration-1e-8;) {
            int frame=guardian.animationFrame();
            assert guardian.animation()==animation&&frame>=previous&&frame<8
                    : "one-shot must advance monotonically without crossing its row";
            seen|=1<<frame;previous=frame;
            double step=Math.min(.005,duration-elapsed);
            guardian.update(step,px,py);elapsed+=step;
        }
        assert seen==255 : animation+" must use all eight authored poses";
    }
}
