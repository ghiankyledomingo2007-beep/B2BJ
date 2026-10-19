import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/**
 * The screen between places. Slime goo floods down over the last frame with a dripping cyan rim, a stage
 * plays while the world waits (the region's silhouette, rising ichor, a turning rune ring, Rainoray
 * shifting from blob to blade and back, a pulsing row of drops, the region's name and a tip), then the goo
 * drains up and play resumes underneath. Any key skips it once it has shown itself. Pure drawing and
 * timing: the host freezes the world while {@link #covering()}.
 */
final class LoadingScreen {
    enum Kind { ENTER, REFORM }

    static final double IN = .45, HOLD = 2.1, MIN_HOLD = .8, OUT = .55;
    private static final int W = 1280, H = 720, S = Ui.S;
    private static final String[] TIPS = {
        "Hold LMB in blade form, then release for a charged cleave.",
        "In blade form, F raises a riposte guard: time it as the blow lands.",
        "Absorb the fallen with E to borrow their traits for a while.",
        "A full ichor gauge lets you transform with Q. The blade fades as it drains.",
        "Rest at a camp with H to save. Shards buy upgrades there with 1-4.",
        "Hold TAB for the region map. The world keeps moving while you look.",
        "Only two enemies strike at once. The rest circle, waiting for an opening.",
        "Thin red lines show where shots will fly. Step off the line.",
        "A guardian's bar burns orange once it enrages at half health."};

    private BufferedImage snapshot;
    private final BufferedImage stage = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage[] backdrops = new BufferedImage[CampaignWorld.AREA_COUNT];
    private final int[] drip = new int[W / S];
    private double t = -1, hold = HOLD;
    private Kind kind = Kind.ENTER;
    private int biome, tip = -1;

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
        this.tip = (tip + 1 + (int) (Math.random() * (TIPS.length - 1))) % TIPS.length;
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
            if (snapshot != null) g.drawImage(snapshot, 0, 0, null);
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
        int drift = (int) Math.round(Math.sin(c * .22) * 22) / S * S;     // the backdrop breathes sideways
        g.drawImage(backdrop(biome), -32 + drift, 0, null);
        pool(g, W / 2, 420, c);
        motes(g, c);
        int cx = W / 2, feet = 420;
        ring(g, cx, feet, c);
        hero(g, cx, feet, c, slimeIdle, morphIn, morphOut);
        String heading = kind == Kind.REFORM ? "R E F O R M I N G   A T   C A M P" : "C H A P T E R   " + Ui.roman(biome + 1);
        Ui.textCentered(g, heading, cx, 104, 2, Ui.GOLD);
        Ui.drawTitleCentered(g, CampaignWorld.area(biome).name(), cx, 118, 3, biome == 0 ? Ui.STEEL_RAMP : Ui.GOLD_RAMP);
        Ui.piece(g, Ui.Piece.FLOURISH, cx - Ui.Piece.FLOURISH.w, 176);
        drops(g, cx, 540, c);
        String words = TIPS[Math.max(0, tip)];
        int w = Ui.width("TIP", 2) + 16 + Ui.width(words, 2);
        int x = cx - w / 2 / S * S;
        Ui.text(g, "TIP", x, 628, 2, Ui.GOLD);
        Ui.text(g, words, x + Ui.width("TIP", 2) + 16, 628, 2, Ui.TEXT);
        if (t >= IN + MIN_HOLD && covering() && (int) (clock * 1.6) % 2 == 0)
            Ui.textCentered(g, "Press any key", cx, 676, 2, Ui.DIM);
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
        var sky = B2BJ.loadImage("assets/ui/title/sky.png");
        var land = B2BJ.loadImage(which == 0 ? "assets/ui/title/far.png" : "assets/ui/title/mid.png");
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

    /** Nine drops; a pulse runs along them, left to right: the world is on its way. */
    private static void drops(Graphics2D g, int cx, int y, double c) {
        for (int i = 0; i < 9; i++) {
            double lit = Math.sin(c * 5 - i * .7) * .5 + .5;
            int x = cx + (i - 4) * 24;
            Color body = lit > .75 ? new Color(0x7aeee6) : lit > .4 ? new Color(0x2cc6c8) : new Color(0x0f5560);
            int bob = lit > .75 ? -S : 0;
            g.setColor(Ui.INK);
            g.fillRect(x - 3 * S, y - 2 * S + bob, 6 * S, 7 * S);
            g.setColor(body);
            g.fillRect(x - S, y - 2 * S + bob, 2 * S, S);
            g.fillRect(x - 2 * S, y - S + bob, 4 * S, 4 * S);
            g.fillRect(x - S, y + 3 * S + bob, 2 * S, S);
            if (lit > .75) { g.setColor(new Color(0xcffff9)); g.fillRect(x - S, y + bob, S, S); }
        }
    }
}
