import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/** A short chapter scene between places. The host freezes the world while covering(). */
final class LoadingScreen {
    enum Kind { ENTER, REFORM }

    static final double IN = .45, HOLD = 2.1, MIN_HOLD = .8, OUT = .55;
    private static final int W = 1280, H = 720, S = Ui.S;

    private BufferedImage snapshot;
    private final BufferedImage stage = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage[] backdrops = new BufferedImage[CampaignWorld.AREA_COUNT];
    private final int[] drip = new int[W / S];
    private double t = -1, hold = HOLD;
    private Kind kind = Kind.ENTER;
    private int biome;
    private final TitleScreen outpostScene = new TitleScreen();

    LoadingScreen() {
        var rng = new java.util.Random(11);
        for (int x = 0; x < drip.length; x++) drip[x] = 0;
        for (int k = 0; k < 46; k++) {                                      // long drips with rounded flanks
            int cx = rng.nextInt(drip.length), len = 10 + rng.nextInt(34), half = 2 + rng.nextInt(4);
            for (int dx = -half; dx <= half; dx++) {
                int x = cx + dx;
                if (x < 0 || x >= drip.length) continue;
                double edge = Math.sqrt(1 - Math.pow(dx / (half + .5), 2));
                drip[x] = Math.max(drip[x], (int) Math.round(len * edge));
            }
        }
        for (int x = 0; x < drip.length; x++) drip[x] += rng.nextInt(3);
    }

    /** Begins a transition. `from` is the frame to flood over; null starts already covered. */
    void start(BufferedImage from, Kind kind, int biome) {
        this.snapshot = from;
        this.kind = kind;
        this.biome = Math.max(0, Math.min(CampaignWorld.AREA_COUNT - 1, biome));
        this.hold = kind == Kind.REFORM ? 1.5 : HOLD;
        outpostScene.enter();
        t = from == null ? IN : 0;
    }

    boolean active() { return t >= 0; }
    /** True while the screen hides the world: the host keeps the game still. */
    boolean covering() { return t >= 0 && t < IN + hold; }
    /** Seconds since the transition began (for tests and the host's music). */
    double time() { return t; }

    void update(double seconds) {
        if (t < 0 || !Double.isFinite(seconds) || seconds <= 0) return;
        t += seconds;
        if (t >= IN + hold + OUT) { t = -1; snapshot = null; }
    }

    /** Any key or click: straight to the drain once the stage has been seen for a moment. */
    void skip() {
        if (t >= IN + MIN_HOLD && t < IN + hold) hold = t - IN;
    }

    /** Draws over whatever the host painted (the live world, during the drain). */
    void draw(Graphics2D g, double clock, BufferedImage slimeIdle, BufferedImage morphIn, BufferedImage morphOut, boolean reduced) {
        if (t < 0) return;
        renderStage(clock, slimeIdle, morphIn, morphOut, reduced);
        Ui.crisp(g);
        double front;                                                       // goo depth in art px, before drips
        if (t < IN) {
            if (snapshot != null) g.drawImage(snapshot, 0, 0, W, H, null);
            double p = t / IN;
            front = p * p * (H / S + 48);
        } else if (t < IN + hold) {
            front = H / S + 48;
        } else {
            double p = (t - IN - hold) / OUT;
            front = (1 - p) * (1 - p) * (H / S + 48);
        }
        if (front >= H / S + 47) { g.drawImage(stage, 0, 0, null); return; }
        var goo = new Polygon();
        goo.addPoint(0, 0);
        for (int x = 0; x < drip.length; x++) {
            int y = (int) Math.round(front - 48 + drip[x]) * S;
            goo.addPoint(x * S, Math.max(0, y));
            goo.addPoint(x * S + S, Math.max(0, y));
        }
        goo.addPoint(W, 0);
        var clip = (Graphics2D) g.create();
        clip.clip(goo);
        clip.drawImage(stage, 0, 0, null);
        clip.dispose();
        Color[] rim = {new Color(0x7aeee6), new Color(0x2cc6c8), new Color(0x178a93), new Color(0x0f5560)};
        for (int x = 0; x < drip.length; x++) {
            int y = (int) Math.round(front - 48 + drip[x]);
            if (y <= 0) continue;
            for (int k = 0; k < rim.length; k++) {
                if (y - 1 - k < 0) break;
                g.setColor(rim[k]);
                g.fillRect(x * S, (y - 1 - k) * S, S, S);
            }
            if (drip[x] > 12 && (x == 0 || drip[x] >= drip[x - 1]) && (x == drip.length - 1 || drip[x] >= drip[x + 1])) {
                g.setColor(new Color(0xcffff9));                            // a glint on each drip's bead
                g.fillRect(x * S, (y - 3) * S, S, S);
            }
        }
    }

    // ------------------------------------------------------------------ the stage

    private void renderStage(double clock, BufferedImage slimeIdle, BufferedImage morphIn, BufferedImage morphOut, boolean reduced) {
        double c = reduced ? 0 : clock;
        var g = stage.createGraphics();
        Ui.crisp(g);
        if (biome == 0 && TitleScreen.available()) {
            outpostScene.drawBackdrop(g, W, H, c, reduced);
        } else {
            g.drawImage(backdrop(biome), -32, 0, null);
            pool(g, 940, 560, c);
            motes(g, c);
            ring(g, 940, 560, c);
            hero(g, 940, 560, c, slimeIdle, morphIn, morphOut);
        }
        Ui.fade(g, 0, 0, 680, H, 130, 0);
        String heading = kind == Kind.REFORM ? "RETURNING TO CAMP" : "CHAPTER " + Ui.roman(biome + 1);
        Ui.text(g, heading, 80, 238, 2, Ui.GOLD);
        String[] name = CampaignWorld.area(biome).name().split(" ", 2);
        Ui.drawTitle(g, name[0], 76, 262, 4, Ui.STEEL_RAMP);
        if (name.length > 1) Ui.drawTitle(g, name[1], 76, 322, 4, Ui.STEEL_RAMP);
        g.setColor(Ui.TEAL);
        g.fillRect(80, 396, 48, 2);
        Ui.text(g, kind == Kind.REFORM ? "Reforming" : "Entering", 80, 448, 2, Ui.MUTED);
        int end = 80 + Ui.width(kind == Kind.REFORM ? "Reforming" : "Entering", 2) + 12;
        for (int i = 0; i < 3; i++) {
            g.setColor(reduced || (int)(c * 3) % 3 == i ? Ui.TEAL : Ui.DIM);
            g.fillRect(end + i * 10, 442, 4, 4);
        }
        g.dispose();
    }

    /** The region in silhouette behind the stage: the title's night sky and the outpost or the forest line. */
    private BufferedImage backdrop(int which) {
        if (backdrops[which] != null) return backdrops[which];
        int wide = W + 64;
        var img = new BufferedImage(wide, H, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        Ui.crisp(g);
        g.setColor(new Color(0x070a14));
        g.fillRect(0, 0, wide, H);
        var sky = B2BJ.loadImage("assets/ui/loading/sky.png");
        var land = B2BJ.loadImage(which == 0 ? "assets/ui/loading/outpost.png" : "assets/ui/loading/forest.png");
        if (sky != null) { g.drawImage(sky, 0, 0, W, H, null); g.drawImage(sky, W, 0, W, H, null); }
        if (land != null) g.drawImage(land, 32, which == 0 ? 56 : 24, W, H, null);
        Ui.veil(g, wide, H, 105);
        Ui.fade(g, 0, 0, wide / 2, H, 140, 0);
        Ui.fade(g, wide / 2, 0, wide / 2, H, 0, 140);
        g.dispose();
        backdrops[which] = img;
        return img;
    }

    /** A wide cyan light pool on the ground round the ring: the hero stands in his own ichor glow. */
    private static void pool(Graphics2D g, int cx, int feet, double c) {
        int rx = 120, ry = 34;
        Color far = new Color(0x0a1f2a), near = new Color(0x0d2c38);
        for (int y = -ry; y <= ry; y++) for (int x = -rx; x <= rx; x++) {
            double d = Math.pow(x / (double) rx, 2) + Math.pow(y / (double) ry, 2);
            if (d > 1) continue;
            double a = (1 - d) * (.75 + .1 * Math.sin(c * 2));
            double threshold = BAYER[(y & 3) * 4 + (x & 3)] / 16.0;
            if (a > threshold + .35) { g.setColor(near); g.fillRect(cx + x * S, feet + y * S, S, S); }
            else if (a > threshold) { g.setColor(far); g.fillRect(cx + x * S, feet + y * S, S, S); }
        }
    }

    private static void motes(Graphics2D g, double c) {
        for (int i = 0; i < 46; i++) {
            double speed = 8 + (i % 7) * 4, phase = i * 37.3;
            int x = (int) ((i * 83.7 + Math.sin(c * .7 + i) * 6) % (W / S));
            int y = (int) ((H / S) - (c * speed + phase) % (H / S + 20));
            boolean near = i % 3 == 0;
            g.setColor(near ? new Color(0x7aeee6) : new Color(0x178a93));
            g.fillRect(x * S, y * S, S, S);
            if (near && i % 6 == 0) { g.setColor(new Color(0x0f5560)); g.fillRect(x * S, (y + 1) * S, S, S); }
        }
    }

    /** A rune ring under the hero: dashes marching round an ellipse, with a breathing glow inside. */
    private static void ring(Graphics2D g, int cx, int feet, double c) {
        int rx = 46, ry = 12;
        for (int y = -ry; y <= ry; y++) for (int x = -rx; x <= rx; x++) {
            double d = Math.pow(x / (double) rx, 2) + Math.pow(y / (double) ry, 2);
            if (d > 1) continue;
            double glow = (1 - d) * (.45 + .2 * Math.sin(c * 3));
            if (glow > BAYER[(y & 3) * 4 + (x & 3)] / 16.0 + .1) {
                g.setColor(new Color(0x0f3f4a));
                g.fillRect(cx + x * S, feet + y * S, S, S);
            }
        }
        for (int i = 0; i < 64; i++) {
            double a = i / 64.0 * Math.PI * 2 + c * .9;
            if (i % 4 == 3) continue;
            int x = (int) Math.round(Math.cos(a) * rx), y = (int) Math.round(Math.sin(a) * ry);
            g.setColor(i % 8 < 2 ? new Color(0x7aeee6) : new Color(0x2cc6c8));
            g.fillRect(cx + x * S, feet + y * S, S, S);
        }
    }

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    /** Blob, then blade, then blob again: the game's whole idea, looping while the world waits. */
    private static void hero(Graphics2D g, int cx, int feet, double c, BufferedImage slimeIdle, BufferedImage morphIn, BufferedImage morphOut) {
        double loop = c % 3.4;
        if (loop < 1.0 || morphIn == null || morphOut == null) {
            if (slimeIdle == null) return;
            int frame = (int) (c * 8) % Math.max(1, slimeIdle.getWidth() / 48);
            g.drawImage(slimeIdle, cx - 48, feet - 84, cx + 48, feet + 12, frame * 48, 0, frame * 48 + 48, 48, null);
            return;
        }
        BufferedImage sheet;
        int frame;
        if (loop < 1.64) { sheet = morphIn; frame = (int) ((loop - 1.0) / .64 * 16); }
        else if (loop < 2.7) { sheet = morphIn; frame = 15; }
        else { sheet = morphOut; frame = (int) ((loop - 2.7) / .64 * 16); }
        frame = Math.max(0, Math.min(15, frame));
        g.drawImage(sheet, cx - 80, feet - 136, cx + 80, feet + 24, frame * 80, 0, frame * 80 + 80, 80, null);
    }

}
