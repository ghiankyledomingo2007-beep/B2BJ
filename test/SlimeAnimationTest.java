public final class SlimeAnimationTest {
    public static void main(String[] args) {
        usesDirectionalMovementRows();
        mirrorsOnlyRightFacingSideFrames();
        keepsFacingDirectionWhileIdle();
        advancesAcrossAllFourFrames();
        usesDirectionalDashRows();
        playsDashSquashAndStretchCycle();
        castingKeepsNaturalBody();
        SlimeAnimation idle=new SlimeAnimation();
        assert idle.idleFrame()==0;
        idle.update(0,0,1.19);
        assert idle.idleFrame()==7 : "reviewed idle must reach its eighth distinct pose";
        idle.update(0,0,0.15);
        assert idle.idleFrame()==0 : "reviewed idle must loop without a duplicate endpoint";
        System.out.println("SlimeAnimationTest passed");
    }

    private static void usesDirectionalMovementRows() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(0, 1, 0);
        assert animation.row() == 6 : "down movement must use front hop row";

        animation.update(-1, 0, 0);
        assert animation.row() == 7 : "side movement must use side hop row";

        animation.update(0, -1, 0);
        assert animation.row() == 8 : "up movement must use back hop row";
    }

    private static void mirrorsOnlyRightFacingSideFrames() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(-1, 0, 0);
        assert !animation.flipHorizontal() : "sheet already faces left";

        animation.update(1, 0, 0);
        assert animation.flipHorizontal() : "right movement must mirror side row";
    }

    private static void keepsFacingDirectionWhileIdle() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(0, -1, 0);
        animation.update(0, 0, 0);

        assert animation.row() == 4 : "idle must keep last back-facing direction";
        assert animation.facingHorizontal() == 0 : "idle must retain horizontal direction";
        assert animation.facingVertical() == -1 : "idle must retain vertical direction";
    }

    private static void advancesAcrossAllFourFrames() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(0, 1, 0.1);
        assert animation.frame() == 1 : "movement must animate at 10 FPS";

        animation.update(0, 1, 0.31);
        assert animation.frame() == 0 : "four-frame cycle must loop";
    }

    private static void usesDirectionalDashRows() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(0, 1, true, 0);
        assert animation.row() == 5 : "down dash must use front squash row";

        animation.update(-1, 0, true, 0);
        assert animation.row() == 10 : "side dash must use side stretch row";

        animation.update(0, -1, true, 0);
        assert animation.row() == 11 : "up dash must use back squash row";
    }

    private static void playsDashSquashAndStretchCycle() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(1, 0, true, 0);
        animation.update(1, 0, true, 0.12);

        assert animation.frame() == 3 : "dash must reach recovery frame before movement ends";
    }

    private static void castingKeepsNaturalBody() {
        SlimeAnimation animation=new SlimeAnimation();
        animation.face(1,0); animation.attack();
        animation.update(0,0,false,0.12);
        assert animation.attacking() && animation.row()==3 : "cast uses side idle, never a stretched punch";
        animation.update(0,0,false,0.24);
        assert !animation.attacking();
        animation.face(0,1); animation.attack();
        assert animation.row()==0 : "front cast preserves normal silhouette";
        animation.update(1,0,true,0.01);
        assert !animation.attacking() : "dash cancels casting pose";
    }
}
