public final class SlimeAnimationTest {
    public static void main(String[] args) {
        usesDirectionalMovementRows();
        mirrorsOnlyRightFacingSideFrames();
        keepsFacingDirectionWhileIdle();
        advancesAcrossAllFourFrames();
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
    }

    private static void advancesAcrossAllFourFrames() {
        SlimeAnimation animation = new SlimeAnimation();

        animation.update(0, 1, 0.1);
        assert animation.frame() == 1 : "movement must animate at 10 FPS";

        animation.update(0, 1, 0.31);
        assert animation.frame() == 0 : "four-frame cycle must loop";
    }
}
