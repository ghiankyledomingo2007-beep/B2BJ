import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class LoadingScreenTest {
    public static void main(String[] args) {
        var screen = new LoadingScreen();
        screen.start(null, LoadingScreen.Kind.ENTER, 0);
        assert screen.active() && screen.covering();
        screen.skip();
        assert screen.covering() : "early input must not skip the initial reveal";
        int[] still = render(screen, 0, true);
        assert Arrays.equals(still, render(screen, 8, true)) : "reduced effects must freeze the scene and indicator";
        assert !Arrays.equals(render(screen, 1, false), render(screen, 2, false)) : "wind and rain should animate";
        var backdrop = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        var g = backdrop.createGraphics();
        new TitleScreen().drawBackdrop(g, 1280, 720, 0, true);
        g.dispose();
        for (int y = 0; y < 720; y++) for (int x = 700; x < 1280; x++)
            assert still[y * 1280 + x] == backdrop.getRGB(x, y) : "chapter scene must preserve the gate and grounded character";
        screen.update(LoadingScreen.MIN_HOLD + .01);
        screen.skip();
        assert screen.active() && !screen.covering() : "skip must start the reveal without blocking gameplay";
        screen.update(LoadingScreen.OUT + .01);
        assert !screen.active();
        screen.start(null, LoadingScreen.Kind.REFORM, 1);
        assert Arrays.equals(render(screen, 0, true), render(screen, 5, true));
        screen.update(1.5 + LoadingScreen.OUT + .01);
        assert !screen.active() : "camp return timing must remain unchanged";
        System.out.println("LoadingScreenTest passed: scene, reduced motion, skip, entry and camp timing");
    }

    private static int[] render(LoadingScreen screen, double clock, boolean reduced) {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        screen.draw(g, clock, null, null, null, reduced);
        g.dispose();
        return image.getRGB(0, 0, 1280, 720, null, 0, 1280);
    }
}
