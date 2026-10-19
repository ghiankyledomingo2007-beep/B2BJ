import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The animated title screen: the layered key art from tools/art/TitleArt.java, the logo, clouds and mist
 * drifting at different speeds, twinkling stars, ichor motes rising from the sword in the stone, fireflies
 * over the forest and Rainoray breathing on the cliff. Pure drawing: it never touches the game.
 */
final class TitleScreen {
    private static BufferedImage layer(String name) { return B2BJ.loadImage("assets/ui/title/" + name + ".png"); }

    private static final BufferedImage SKY = layer("sky"), CLOUDS = layer("clouds"), MOON = layer("moon"), FAR = layer("far"),
            MIST = layer("mist"), MID = layer("mid"), FORE = layer("fore"), LOGO = layer("logo");
    // positions shared with tools/art/TitleArt.java (art pixels)
    static final int SWORD_X = 534, SLIME_X = 452, LEDGE_Y = 298, MIST_Y = 238;
    private static final int S = Ui.S;

    private final int[][] twinkles = new int[18][];
    private final double[][] motes = new double[26][];
    private final double[][] flies = new double[9][];

    TitleScreen() {
        var rng = new java.util.Random(7);
        int found = 0;
        if (SKY != null) for (int tries = 0; tries < 20000 && found < twinkles.length; tries++) {
            int x = 220 + rng.nextInt(400), y = 8 + rng.nextInt(200);
            if (((SKY.getRGB(x, y) >> 16) & 255) > 150) twinkles[found++] = new int[]{x, y, rng.nextInt(1000)};
        }
        for (int i = 0; i < motes.length; i++)
            motes[i] = new double[]{SWORD_X - 30 + rng.nextDouble() * 70, 6 + rng.nextDouble() * 10, rng.nextDouble() * 100, rng.nextDouble() * 6.28};
        for (int i = 0; i < flies.length; i++)
            flies[i] = new double[]{250 + rng.nextDouble() * 370, 258 + rng.nextDouble() * 34, rng.nextDouble() * 6.28, .6 + rng.nextDouble()};
    }

    static boolean available() { return SKY != null && FAR != null && FORE != null && LOGO != null; }

    /** The scene is composed here at art size, then doubled onto the screen in one draw. */
    private final BufferedImage frame = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);

    /** One frame at `seconds` since the title appeared. `slimeIdle` is the 8-frame 48 px idle sheet. */
    void draw(Graphics2D screen, int width, int height, double seconds, BufferedImage slimeIdle, boolean reduced) {
        double t = reduced ? 0 : seconds;
        var g = frame.createGraphics();
        art(g, SKY, 0, 0);
        for (int[] star : twinkles) {
            if (star == null) continue;
            double b = Math.sin(t * 1.7 + star[2]) * .5 + .5;
            if (b < .55) continue;
            g.setColor(b > .85 ? Color.WHITE : new Color(0xbcd0ff));
            g.fillRect(star[0], star[1], 1, 1);
            if (b > .93) {
                g.setColor(new Color(0x6f86b8));
                g.fillRect(star[0] - 1, star[1], 1, 1); g.fillRect(star[0] + 1, star[1], 1, 1);
                g.fillRect(star[0], star[1] - 1, 1, 1); g.fillRect(star[0], star[1] + 1, 1, 1);
            }
        }
        scroll(g, CLOUDS, 0, t * 2.2);
        art(g, MOON, 0, 0);
        art(g, FAR, 0, 0);
        scroll(g, MIST, MIST_Y, t * 5.0);
        art(g, MID, 0, 0);
        g.setColor(new Color(0xffdd80));
        for (double[] f : flies) {                                          // fireflies over the trees
            double phase = t * f[3] + f[2];
            if (Math.sin(phase * 2.3) < .2) continue;
            g.fillRect((int) (f[0] + Math.sin(phase) * 9), (int) (f[1] + Math.cos(phase * .7) * 5), 1, 1);
        }
        swordGlow(g, t);
        art(g, FORE, 0, 0);
        if (slimeIdle != null) {
            int cell = Math.max(1, slimeIdle.getWidth() / 48), frameNo = (int) (t * 8) % cell;
            g.drawImage(slimeIdle, SLIME_X - 24, LEDGE_Y - 44, SLIME_X + 24, LEDGE_Y + 4, frameNo * 48, 0, frameNo * 48 + 48, 48, null);
        }
        for (double[] m : motes) {                                         // ichor rising from the blade
            double life = (t * (10 + m[2] * .08) + m[2]) % 60 / 60;
            if (life > .85 && ((int) (t * 10 + m[2]) & 1) == 0) continue;
            g.setColor(life < .5 ? new Color(0x7aeee6) : new Color(0x2cc6c8));
            g.fillRect((int) Math.round(m[0] + Math.sin(t * 1.3 + m[3]) * 2.5), (int) Math.round(LEDGE_Y - 20 - life * 90 - m[1]), 1, 1);
        }
        g.dispose();
        Ui.crisp(screen);
        screen.drawImage(frame, 0, 0, width, height, null);
        Ui.fade(screen, 0, 0, 800, height, 225, 0);
        if (LOGO != null) screen.drawImage(LOGO, 40, 34, LOGO.getWidth() * S, LOGO.getHeight() * S, null);
    }

    /** The blade's cyan halo, breathing: dithered rings so it stays pixel art. */
    private void swordGlow(Graphics2D g, double t) {
        double pulse = .5 + .5 * Math.sin(t * 2.2);
        int cx = SWORD_X, cy = LEDGE_Y - 34;
        Color strong = new Color(0x178a93), faint = new Color(0x0f3f4a);
        for (int y = -30; y <= 30; y++) for (int x = -14; x <= 14; x++) {
            double d = Math.sqrt(x * x * 2.2 + y * y * .45) / 20.0;
            double a = (1 - d) * (.35 + .25 * pulse);
            if (a <= 0) continue;
            double threshold = BAYER[(y & 3) * 4 + (x & 3)] / 16.0;
            if (a > threshold + .18) { g.setColor(strong); g.fillRect(cx + x, cy + y, 1, 1); }
            else if (a > threshold) { g.setColor(faint); g.fillRect(cx + x, cy + y, 1, 1); }
        }
    }

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private static void art(Graphics2D g, BufferedImage image, int x, int y) {
        if (image != null) g.drawImage(image, x, y, null);
    }

    /** A horizontally tiling layer drifting left by `offset` art pixels. */
    private static void scroll(Graphics2D g, BufferedImage image, int y, double offset) {
        if (image == null) return;
        int w = image.getWidth(), o = (int) Math.floor(offset) % w;
        art(g, image, -o, y);
        art(g, image, w - o, y);
    }
}
