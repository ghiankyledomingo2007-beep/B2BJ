/**
 * Title-screen key art, in layers the game animates (assets/ui/title/*.png, 640x360 art pixels):
 * a dithered night sky with a big moon, cloud and mist bands that scroll, far mountains and the ruined
 * outpost on its hill, a forest line, and a moonlit cliff where a sword stands in the stone.
 * The left third stays dark and quiet: the logo and menu live there.
 */
final class TitleArt {
    private TitleArt() { }

    static final int W = 640, H = 360, MOON_X = 506, MOON_Y = 78, MOON_R = 27;
    static final int SWORD_X = 534, SLIME_X = 452, LEDGE_Y = 298;
    static final int[] SKY = {Pal.c(0x04060c), Pal.c(0x060912), Pal.c(0x080c19), Pal.c(0x0b1121), Pal.c(0x0f172c),
            Pal.c(0x141f39), Pal.c(0x1a2848), Pal.c(0x213258), Pal.c(0x293d66)};
    static final int FAR = Pal.c(0x111a2e), FAR_RIM = Pal.c(0x1d2a46), NEAR = Pal.c(0x0c1324), NEAR_RIM = Pal.c(0x172238),
            HILL = Pal.c(0x0a1020), KEEP = Pal.c(0x090e1b), KEEP_RIM = Pal.c(0x26345a), TREE = Pal.c(0x060a13), TREE_RIM = Pal.c(0x101a2b),
            GROUND = Pal.c(0x05080e);

    private static double clamp(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    /** Index into a ramp with ordered dithering between neighbours: smooth light without banding. */
    private static int dithered(int[] ramp, double at, int x, int y) {
        at = Math.max(0, Math.min(ramp.length - 1, at));
        int base = (int) Math.floor(at);
        double frac = at - base;
        return ramp[Math.min(ramp.length - 1, base + (frac > Noise.bayer(x, y) ? 1 : 0))];
    }

    static Canvas sky() {
        Canvas c = new Canvas(W, H);
        int horizon = 262;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            double at = clamp(y / (double) horizon) * (SKY.length - 1.4);
            double d = Math.hypot(x - MOON_X, (y - MOON_Y) * 1.1);
            if (d < 120) at += 2.2 * Math.pow(1 - d / 120, 1.6);           // moonlit haze
            at -= clamp((200 - x) / 200.0) * .9;                           // the menu side stays darker
            c.set(x, y, dithered(SKY, at, x, y));
        }
        for (int y = 0; y < horizon - 30; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - MOON_X, y - MOON_Y);
            if (d < MOON_R + 18) continue;
            double r = Noise.rnd(x, y, 71), density = .0045 * (1.3 - y / (double) horizon);
            if (r < density) c.set(x, y, r < density * .25 ? Pal.c(0xdfe8ff) : r < density * .6 ? Pal.STONE[7] : Pal.STONE[6]);
        }
        int[][] big = {{332, 36}, {404, 120}, {590, 160}, {286, 98}, {612, 34}, {240, 30}, {448, 22}, {372, 64}};
        for (int[] s : big) {
            c.set(s[0], s[1], 0xffffffff);
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) c.set(s[0] + o[0], s[1] + o[1], Pal.STONE[7]);
        }
        return c;
    }

    /** The moon on its own layer, drawn over the clouds so no band ever stripes across it. */
    static Canvas moonLayer() {
        Canvas c = new Canvas(W, H);
        moon(c);
        return c;
    }

    static void moon(Canvas c) {
        int light = Pal.c(0xeef2f4), mid = Pal.c(0xd2dae2), shade = Pal.c(0xa9b6c4), rim = Pal.c(0x7f8ea3);
        for (int y = -MOON_R; y <= MOON_R; y++) for (int x = -MOON_R; x <= MOON_R; x++) {
            double d = Math.sqrt(x * x + y * y);
            if (d > MOON_R + .3) continue;
            double lit = (-x - y) / (double) MOON_R;
            int colour = d > MOON_R - 1.3 && x + y > 4 ? rim : lit > .55 ? light : lit > -.45 ? mid : shade;
            c.set(MOON_X + x, MOON_Y + y, colour);
        }
        int[][] craters = {{-8, -6, 5}, {9, 4, 4}, {-2, 12, 3}, {12, -11, 2}, {-14, 7, 3}, {3, -15, 2}};
        for (int[] k : craters) for (int y = -k[2]; y <= k[2]; y++) for (int x = -k[2]; x <= k[2]; x++) {
            double d = Math.sqrt(x * x + y * y);
            if (d > k[2]) continue;
            int px = MOON_X + k[0] + x, py = MOON_Y + k[1] + y;
            c.set(px, py, d > k[2] - 1 && x + y < 0 ? shade : x + y > 0 ? Pal.c(0xbcc7d3) : Pal.c(0xc3cdd8));
        }
    }

    /** Scrolling cloud bands; tiles horizontally (periodic noise), transparent between the dithered wisps. */
    static Canvas clouds() {
        Canvas c = new Canvas(W, 170);
        for (int y = 0; y < c.h; y++) for (int x = 0; x < W; x++) {
            double band = Math.exp(-Math.pow((y - 64) / 20.0, 2)) * .85 + Math.exp(-Math.pow((y - 126) / 12.0, 2)) * .55;
            double n = Noise.periodic(x * 1.0, y * 2.2, 52, W, 5) * .55 + Noise.periodic(x * 1.0, y * 2.2, 19, W, 9) * .3
                    + Noise.periodic(x * 1.0, y * 2.2, 7, W, 11) * .15;
            double v = band * n - .36;
            if (v <= Noise.bayer(x, y) * .22) continue;
            boolean top = v > .1 && band * Noise.periodic(x * 1.0, (y - 2) * 2.2, 52, W, 5) - .36 < .05;
            double d = Math.hypot(x - MOON_X, y - MOON_Y);
            c.set(x, y, top && d < 150 ? Pal.c(0x2f4170) : v > .16 ? Pal.c(0x1c2746) : Pal.c(0x161f38));
        }
        return c;
    }

    static Canvas far() {
        Canvas c = new Canvas(W, H);
        ridge(c, 214, 46, 150, 3, FAR, FAR_RIM);
        ridge(c, 238, 30, 80, 8, NEAR, NEAR_RIM);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {           // the outpost's hill
            double dx = (x - 462) / 170.0, top = 252 + dx * dx * 34;
            if (y >= top) c.set(x, y, y < top + 1 ? NEAR_RIM : HILL);
        }
        outpost(c);
        return c;
    }

    private static void ridge(Canvas c, int base, int lift, double cell, int seed, int fill, int rim) {
        for (int x = 0; x < W; x++) {
            double top = base - lift * Noise.fbm(x, 0, cell, 3, seed) - 8 * Math.sin(x / 37.0 + seed);
            for (int y = (int) top; y < H; y++) c.set(x, y, y == (int) top && x > 230 ? rim : fill);
        }
    }

    /** The Ruined Outpost in silhouette: broken keep, towers, crenellated wall, the sealed East Gate. */
    static void outpost(Canvas c) {
        int base = 246;
        rect(c, 364, 216, 196, base - 216);                                 // curtain wall
        for (int x = 364; x < 560; x += 7) rect(c, x, 212, 4, 4);
        rect(c, 452, 146, 30, base - 146);                                   // keep
        for (int x = 452; x < 482; x += 6) rect(c, x, 141, 4, 5);
        for (int i = 0; i < 9; i++) c.clear(474 + i, 141 + i / 2);          // its broken crown
        rect(c, 374, 184, 20, base - 184);                                   // west tower and its roof
        for (int i = 0; i < 12; i++) rect(c, 374 + i, 184 - i * 2 / 3, 20 - 2 * i, 1);
        rect(c, 530, 170, 22, base - 170);                                   // east tower, split
        for (int x = 530; x < 552; x += 6) rect(c, x, 166, 4, 4);
        for (int i = 0; i < 6; i++) c.clear(540 + i, 166 + i);
        rect(c, 482, 112, 2, 30);                                            // banner pole and torn banner
        for (int y = 0; y < 12; y++) for (int x = 0; x < 11 - (y % 4); x++) c.set(484 + x, 114 + y + x / 4, Pal.c(0x2a1520));
        for (int y = 212; y < 228; y++) for (int x = 498; x < 512; x++)     // a breach in the curtain wall
            if (y < 212 + Math.abs(x - 505) * 1.6 + 2) c.clear(x, y);
        for (int x = 403; x < 450; x += 9) rect(c, x, 210, 3, 6);             // wall buttresses
        int[][] slits = {{466, 160}, {458, 186}, {476, 214}, {382, 196}, {386, 218}, {540, 186}, {544, 204}};
        for (int[] k : slits) for (int y = 0; y < 4; y++) c.set(k[0], k[1] + y, Pal.c(0x03050a));
        for (int[] row : new int[][]{{452, 168, 30}, {452, 198, 30}, {374, 208, 20}, {530, 196, 22}})
            for (int x = row[0]; x < row[0] + row[2]; x++) if ((x & 3) != 1) c.set(x, row[1], Pal.c(0x0d1426));
        for (int y = 0; y < H; y++) for (int x = 360; x < 562; x++) {      // rim light from the moon (upper right)
            if (c.get(x, y) != KEEP) continue;
            if (!isKeep(c, x, y - 1) || !isKeep(c, x + 1, y)) c.set(x, y, KEEP_RIM);
        }
        for (int y = 228; y < base; y++) for (int x = 416; x < 434; x++) {   // East Gate arch, sealed in cyan
            double dx = (x - 424.5) / 9.0, top = 228 + dx * dx * 6;
            if (y >= top) c.set(x, y, y > top + 2 && Math.abs(dx) < .7 ? Pal.CYAN[1] : Pal.c(0x050810));
        }
        c.set(424, 236, Pal.CYAN[4]); c.set(425, 236, Pal.CYAN[3]); c.set(424, 237, Pal.CYAN[3]);
        int[][] windows = {{458, 172, 1}, {471, 190, 1}, {462, 208, 0}, {380, 202, 1}, {538, 192, 0}, {546, 216, 1}};
        for (int[] w : windows) {
            int colour = w[2] == 1 ? Pal.FIRE[4] : Pal.CYAN[3];
            c.set(w[0], w[1], colour); c.set(w[0] + 1, w[1], w[2] == 1 ? Pal.FIRE[3] : Pal.CYAN[2]);
            c.set(w[0], w[1] + 1, w[2] == 1 ? Pal.FIRE[3] : Pal.CYAN[2]); c.set(w[0] + 1, w[1] + 1, w[2] == 1 ? Pal.FIRE[2] : Pal.CYAN[1]);
        }
        for (int x : new int[]{396, 446, 520}) {                            // wall torches
            c.set(x, 207, Pal.FIRE[4]); c.set(x, 208, Pal.FIRE[3]); c.set(x - 1, 209, Pal.FIRE[2]); c.set(x, 209, Pal.FIRE[3]); c.set(x, 210, Pal.c(0x3a2214));
        }
    }

    private static boolean isKeep(Canvas c, int x, int y) { int v = c.get(x, y); return v == KEEP || v == KEEP_RIM; }
    private static void rect(Canvas c, int x, int y, int w, int h) { c.rect(x, y, w, h, KEEP); }

    /** A band of pines and dead trees, moon-rimmed on their right sides. */
    static Canvas mid() {
        Canvas c = new Canvas(W, H);
        int bx = -4, bi = 0;                                                 // a fainter back row first: depth
        while (bx < W + 10) {
            int h = 14 + (int) (Noise.rnd(bi, 13, 53) * 20), baseY = 280 + (int) (Noise.rnd(bi, 15, 57) * 6);
            pine(c, bx, baseY, h, Pal.c(0x0b1222), Pal.c(0x141e33));
            bx += 4 + (int) (Noise.rnd(bi, 17, 59) * 7);
            bi++;
        }
        int x = -8, i = 0;
        while (x < W + 10) {
            double r = Noise.rnd(i, 3, 41);
            int h = 22 + (int) (r * 34), baseY = 286 + (int) (Noise.rnd(i, 5, 43) * 10);
            if (x < 220) h = h * 3 / 4;
            if (Noise.rnd(i, 7, 47) < .14) deadTree(c, x, baseY, h);
            else pine(c, x, baseY, h, TREE, TREE_RIM);
            x += 6 + (int) (Noise.rnd(i, 9, 49) * 11);
            i++;
        }
        for (int y = 0; y < H; y++) for (int xx = 0; xx < W; xx++) if (y > 294 + (int) (4 * Math.sin(xx / 23.0))) c.set(xx, y, TREE);
        return c;
    }

    private static void pine(Canvas c, int cx, int base, int h, int fill, int rim) {
        int tiers = 3 + (h > 40 ? 2 : h > 26 ? 1 : 0);
        for (int y = 0; y < h; y++) {
            double t = y / (double) h;
            int tier = (int) (t * tiers);
            double within = t * tiers - tier;
            double ragged = Noise.rnd(cx, y, 61) < .3 ? 1 : 0;
            int half = (int) Math.round(1 + h * .17 * (tier + 1) / (double) tiers * (.4 + .6 * within) - ragged);
            int left = half + (Noise.rnd(cx + 1, y, 63) < .25 ? 1 : 0);
            for (int dx = -left; dx <= half; dx++) c.set(cx + dx, base - h + y, dx == half && t > .08 ? rim : fill);
        }
        c.rect(cx, base - 2, 1, 3, fill);
    }

    private static void deadTree(Canvas c, int cx, int base, int h) {
        c.rect(cx, base - h, 2, h, TREE);
        for (int k = 0; k < 4; k++) {
            int y = base - h + 4 + k * h / 5, len = 4 + k;
            int dir = (k & 1) == 0 ? 1 : -1;
            for (int s = 0; s < len; s++) c.set(cx + (dir > 0 ? 2 : -1) + dir * s, y - s / 2, s == len - 1 ? TREE_RIM : TREE);
        }
    }

    /** The cliff the slime stands on, the dark rocks under the menu, and the sword in the stone. */
    static Canvas fore() {
        Canvas c = new Canvas(W, H);
        int rock = Pal.c(0x0d121c), rockLit = Pal.c(0x1d2537), rockTop = Pal.c(0x2c3651), grass = Pal.c(0x0c1712);
        for (int x = 300; x < W; x++) {
            double top = LEDGE_Y + 6 * Math.sin(x / 31.0) + 3 * Noise.fbm(x, 1, 18, 2, 21) - (x - 300) * .02;
            if (x < 336) top += (336 - x) * 1.5;
            for (int y = (int) top; y < H; y++) {
                int depth = y - (int) top;
                int colour = depth == 0 ? rockTop : depth < 3 ? rockLit : rock;
                if (depth > 4) {
                    double strata = Noise.fbm(x * .5, y * 2.0, 22, 2, 25);
                    if (strata > .7) colour = Pal.c(0x10151f);
                    if (Noise.rnd(x, y, 23) < .025) colour = Pal.c(0x080b12);
                }
                if (depth == 1 && Noise.rnd(x, y, 27) < .18) colour = rockTop;
                c.set(x, y, colour);
            }
            if (Noise.rnd(x, 6, 35) < .05) { c.set(x, (int) top - 1, Pal.STONE[4]); c.set(x + 1, (int) top - 1, Pal.STONE[3]); }
            if (Noise.rnd(x, 2, 29) < .38) {                                  // grass tufts on the lip
                int tuft = 1 + (int) (Noise.rnd(x, 4, 31) * (Math.abs(x - SWORD_X) < 40 ? 6 : 3));
                for (int k = 1; k <= tuft; k++) c.set(x + (k > 3 ? (x & 1) : 0), (int) top - k, k == tuft ? Pal.c(0x183024) : grass);
            }
        }
        for (int x = 0; x < 230; x++) {                                       // rocks under the menu: nearly black
            double top = 330 - 22 * Math.sin(Math.min(Math.PI, x / 70.0)) + 4 * Noise.fbm(x, 2, 14, 2, 33);
            for (int y = (int) top; y < H; y++) c.set(x, y, y == (int) top ? Pal.c(0x121826) : GROUND);
        }
        stone(c);
        sword(c);
        return c;
    }

    private static void stone(Canvas c) {
        int cx = SWORD_X, cy = LEDGE_Y - 4;
        for (int y = -11; y <= 9; y++) for (int x = -21; x <= 21; x++) {
            double d = Math.pow(x / 21.0, 2) + Math.pow(y / (y < 0 ? 11.0 : 9.0), 2);
            if (d > 1) continue;
            double lit = (x - y * 1.4) / 21.0;
            int colour = d > .86 && y > 0 ? Pal.STONE[1] : lit > .55 ? Pal.STONE[6] : lit > .1 ? Pal.STONE[5] : lit > -.4 ? Pal.STONE[4] : Pal.STONE[3];
            c.set(cx + x, cy + y, colour);
        }
        int[][] cracks = {{-9, -3}, {-8, -2}, {-7, -2}, {-6, -1}, {5, 1}, {6, 2}, {7, 2}, {13, -4}, {14, -3}};
        for (int[] k : cracks) c.set(cx + k[0], cy + k[1], Pal.STONE[2]);
    }

    /** A greatsword driven into the stone: gold hilt, wrapped grip, ichor runes along the blade. */
    private static void sword(Canvas c) {
        int x = SWORD_X, tipY = LEDGE_Y - 12, guardY = LEDGE_Y - 56;
        for (int y = guardY + 3; y <= tipY; y++) {
            c.set(x - 2, y, Pal.IRON[3]); c.set(x - 1, y, Pal.IRON[4]); c.set(x, y, Pal.IRON[5]);
            c.set(x + 1, y, Pal.c(0xcfd6e2)); c.set(x + 2, y, 0xffffffff);
        }
        for (int y = guardY + 8; y < tipY - 4; y += 7) { c.set(x, y, Pal.CYAN[4]); c.set(x, y + 1, Pal.CYAN[3]); }
        for (int dx = -10; dx <= 10; dx++) for (int dy = 0; dy < 3; dy++)
            c.set(x + dx, guardY + dy, dy == 0 ? Pal.GOLD[4] : dy == 1 ? Pal.GOLD[3] : Pal.GOLD[1]);
        c.set(x - 11, guardY + 1, Pal.GOLD[2]); c.set(x + 11, guardY + 1, Pal.GOLD[2]);
        for (int y = guardY - 12; y < guardY; y++) for (int dx = -1; dx <= 1; dx++)
            c.set(x + dx, y, ((y + dx) & 1) == 0 ? Pal.c(0x6d4941) : Pal.c(0x996f5d));
        for (int y = -3; y <= 3; y++) for (int dx = -3; dx <= 3; dx++) {
            if (dx * dx + y * y > 10) continue;
            c.set(x + dx, guardY - 16 + y, dx + y < -1 ? Pal.GOLD[4] : dx + y > 2 ? Pal.GOLD[1] : Pal.GOLD[3]);
        }
        c.set(x, guardY - 16, Pal.CYAN[4]); c.set(x - 1, guardY - 17, Pal.CYAN[5]);
    }

    /** Low mist that drifts across the hill's foot; tiles horizontally. */
    static Canvas mist() {
        Canvas c = new Canvas(W, 48);
        for (int y = 0; y < c.h; y++) for (int x = 0; x < W; x++) {
            double profile = Math.exp(-Math.pow((y - 24) / 11.0, 2));
            double n = Noise.periodic(x, y * 4.0, 38, W, 13);
            double v = profile * (.35 + .65 * n) - .32;
            if (v > Noise.bayer(x, y) * .6) c.set(x, y, v > .34 ? Pal.c(0x223052) : Pal.c(0x18223e));
        }
        return c;
    }

    static void build() {
        sky().save("assets/ui/title/sky.png");
        clouds().save("assets/ui/title/clouds.png");
        far().save("assets/ui/title/far.png");
        mist().save("assets/ui/title/mist.png");
        moonLayer().save("assets/ui/title/moon.png");
        mid().save("assets/ui/title/mid.png");
        fore().save("assets/ui/title/fore.png");
    }
}
