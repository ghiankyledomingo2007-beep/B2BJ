import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.SplashScreen;

/**
 * Fills the boot splash's load bar while the game's art loads. The JVM shows assets/ui/splash.png (named
 * in the jar manifest) the moment the game starts; it closes by itself when the window opens.
 */
final class Splash {
    private Splash() { }

    /** Images the game loads before its first frame (measured 2026-10-16); the bar fills against it. */
    static final int EXPECTED = 171;
    static final int BAR_X = 256, BAR_Y = 276, BAR_WIDTH = 128, BAR_HEIGHT = 2;
    private static int loaded;
    private static SplashScreen screen;
    private static boolean looked;

    static int loaded() { return loaded; }

    /** One more image is in. Cheap when there is no splash (tests, IDE runs). */
    static void step() {
        loaded++;
        if (loaded % 3 != 0) return;
        SplashScreen splash = screen();
        if (splash == null) return;
        try {
            if (!splash.isVisible()) { screen = null; return; }
            Graphics2D g = splash.createGraphics();
            int fill = (int) Math.round(BAR_WIDTH * Math.min(1, loaded / (double) EXPECTED)) / 2 * 2;
            g.setColor(new Color(0x8ccfca));
            g.fillRect(BAR_X, BAR_Y, fill, BAR_HEIGHT);
            g.dispose();
            splash.update();
        } catch (IllegalStateException closed) {
            screen = null;
        }
    }

    private static SplashScreen screen() {
        if (!looked) {
            looked = true;
            try {
                if (!GraphicsEnvironment.isHeadless()) screen = SplashScreen.getSplashScreen();
            } catch (RuntimeException unsupported) {
                screen = null;
            }
        }
        return screen;
    }
}
