public final class CharacterAnimationTest {
    public static void main(String[] args) {
        bladeUsesDirectionalActionSheets();
        bladeSlashPlaysOnceThenReturnsToIdle();
        wispUsesEveryDirectionalRow();
        wispWalkLoopsAcrossEightFrames();
        characterRenderSizesUseOneSharedPixelScale();
        transformationEffectPlaysOnce();
        System.out.println("CharacterAnimationTest passed");
    }

    private static void bladeUsesDirectionalActionSheets() {
        BladeAnimation animation = new BladeAnimation();

        animation.update(0, 1, false, 0);
        assert animation.action() == BladeAnimation.Action.RUN;
        assert animation.row() == 0 : "down movement must use Blade front row";

        animation.update(-1, 0, false, 0);
        assert animation.row() == 1 : "side movement must use Blade side row";
        assert animation.flipHorizontal() : "right-facing source must mirror for west movement";
        animation.update(0, 0, false, 0);
        assert animation.facingHorizontal() == -1;
        assert animation.facingVertical() == 0;

        animation.update(1, 0, false, 0);
        assert !animation.flipHorizontal() : "right-facing source must stay unmirrored for east movement";
        animation.slash();
        assert !animation.flipHorizontal() : "east slash must keep its source orientation";
        assert animation.padding()==0 : "unreplaced side attack retains its legacy grid";
        assert animation.sheetPath().endsWith("blade_slash.png");
        animation.face(-1,0);
        assert animation.flipHorizontal() : "west slash must mirror its source orientation";
        animation.update(0,0,false,0.21);
        assert animation.frame()==2 : "legacy side attack must stay within its three-cell grid";
        animation.update(0,0,false,0.10);

        animation.update(0, -1, true, 0);
        assert animation.action() == BladeAnimation.Action.DASH;
        assert animation.row() == 2 : "up movement must use Blade back row";
        assert animation.sheetPath().endsWith("blade_dash.png");
        animation.update(1,0,true,0);
        assert !animation.flipHorizontal() : "east dash must keep its source orientation";
        animation.update(-1,0,true,0);
        assert animation.flipHorizontal() : "west dash must mirror its source orientation";
    }

    private static void bladeSlashPlaysOnceThenReturnsToIdle() {
        BladeAnimation animation = new BladeAnimation();

        animation.slash();
        animation.update(0, 0, false, 0.21);
        assert animation.action() == BladeAnimation.Action.SLASH;
        assert animation.frame() == 5 : "eight-pose attack must progress through recovery";
        assert animation.padding() == 8 : "attack margin is transparent weapon clearance, not body scaling";
        assert animation.sheetPath().endsWith("blade_cut.png");

        animation.update(0, 0, false, 0.08);
        assert animation.frame() == 7 : "attack must reach its final recovery pose";
        animation.update(0, 0, false, 0.02);
        assert animation.action() == BladeAnimation.Action.IDLE
                : "completed slash must return to idle";
        assert animation.frame() == 0 : "idle Blade must use planted run frame";
        assert animation.padding() == 0 : "run and idle retain their existing native cells";
    }

    private static void wispUsesEveryDirectionalRow() {
        WispAnimation animation = new WispAnimation();

        animation.update(0, 1, 0);
        assert animation.row() == 0 : "south must use row zero";
        animation.update(1, 0, 0);
        assert animation.row() == 1 : "east must use row one";
        animation.update(0, -1, 0);
        assert animation.row() == 2 : "north must use row two";
        animation.update(-1, 0, 0);
        assert animation.row() == 3 : "west must use row three";
    }

    private static void wispWalkLoopsAcrossEightFrames() {
        WispAnimation animation = new WispAnimation();

        animation.update(1, 0, 0.125);
        assert animation.frame() == 1 : "Wisp walk must animate at 8 FPS";

        animation.update(1, 0, 0.875);
        assert animation.frame() == 0 : "eight-frame Wisp walk must loop";
    }

    private static void characterRenderSizesUseOneSharedPixelScale() {
        assert SlimeAnimation.RENDER_SIZE == 96 : "Slime must render at 2x its 48px cell";
        assert WispAnimation.RENDER_SIZE == 96 : "Wisp must render at 2x its 48px cell";
        assert BladeAnimation.RENDER_WIDTH == 96 : "Blade width must use the same 2x scale";
        assert BladeAnimation.RENDER_HEIGHT == 128 : "Blade keeps its taller 48x64 frame";
    }

    private static void transformationEffectPlaysOnce() {
        TransformationAnimation animation = new TransformationAnimation();

        assert !animation.active();
        animation.start();
        animation.update(TransformationAnimation.DURATION / 2);
        assert animation.active();
        assert animation.frame() == 4 : "transformation must reach its middle burst";

        animation.update(TransformationAnimation.DURATION / 2);
        assert !animation.active() : "transformation effect must end after one cycle";
    }
}
