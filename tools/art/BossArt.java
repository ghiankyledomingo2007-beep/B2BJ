/**
 * Briarheart, the Corrupted Forest's guardian, in the Warden's motion-sheet layout: 8 frames per row,
 * rows in Guardian.Animation order (idle, approach, windup, strike, recover, charge, death). Cells are
 * 96 px drawn at 2x, so its pixels match the forest's; the foot row is 84 (the Warden's 56 of 64, scaled).
 * A hunched giant of violet-black roots with a gold heart burning behind its ribs; long thorned root-arms
 * rise overhead on the windup and crash into the ground on the strike; in death it withers and the heart
 * goes out.
 */
final class BossArt {
    private BossArt() { }
    static final int CELL = 96, FOOT = 84;

    static Canvas briarheart() {
        Canvas sheet = new Canvas(8 * CELL, 7 * CELL);
        for (int row = 0; row < 7; row++) for (int f = 0; f < 8; f++) {
            Canvas cell = pose(row, f);
            rim(cell, row == 6 && f >= 4);
            cell.selectiveOutline();
            sheet.blit(cell, f * CELL, row * CELL);
        }
        return sheet;
    }

    /**
     * One frame. arm: 0 hangs, 1 overhead, -1 slammed into the ground; glow: the heart's heat 0..1;
     * wither: 0 alive .. 1 a dead heap.
     */
    static Canvas pose(int row, int f) {
        double t = f / 7.0, breathe = Math.sin(f / 8.0 * Math.PI * 2);
        double arm = 0, glow = .55 + .15 * breathe, lean = 0, sink = 0, shift = 0, wither = 0;
        int step = 0;
        switch (row) {
            case 0 -> { sink = breathe; arm = .05 * breathe; }
            case 1 -> { shift = Math.sin(f / 8.0 * Math.PI * 2) * 2; step = f < 4 ? 1 : -1; sink = Math.abs(Math.sin(f / 8.0 * Math.PI * 2)); }
            case 2 -> { arm = ease(t); glow = .6 + .4 * t; lean = -3 * t; }
            case 3, 5 -> { arm = f < 2 ? 1 - f * 1.0 : -1; glow = f < 3 ? 1 : .85 - (f - 3) * .06; lean = f < 2 ? 0 : 4; sink = f < 2 ? 0 : 2; }
            case 4 -> { arm = -1 + ease(t); glow = .45 + .1 * t; lean = 4 * (1 - t); sink = 2 * (1 - t); }
            default -> { wither = t; arm = -.3 * t; glow = Math.max(0, .9 - t * 1.3); sink = t * 16; lean = 2 * t; }
        }
        Canvas c = new Canvas(CELL, CELL);
        int[] bark = wither > .5 ? new int[]{Pal.INK, Pal.STONE[0], Pal.STONE[1], Pal.STONE[2], Pal.STONE[3]} : ForestArt.ROT_BARK;
        double cx = 48 + shift, top = 30 + sink, heartY = 47 + sink * .8;
        // far arm first (darker), then the root legs, the body, the near arm over everything
        armLimb(c, cx, top, -1, arm, bark, true, wither);
        for (int r = 0; r < 4; r++) {                                       // root legs splay into the ground
            int side = r < 2 ? -1 : 1, inner = r % 2;
            double hipX = cx + side * (inner == 0 ? 11 : 5), footX = cx + side * (inner == 0 ? 27 : 12) + (side == step ? 2 : 0);
            double footY = FOOT + (inner == 0 ? 0 : 1) - (side == step && r % 2 == 0 ? 2 : 0);
            ForestArt.limb(c, hipX, Math.min(FOOT - 6, 64 + sink), footX, footY, inner == 0 ? 5 : 4, 1.5, bark);
        }
        trunk(c, cx + lean * .5, top, 68 + Math.min(10, sink * .5), bark, wither);
        heart(c, cx + lean * .3, heartY, glow, wither);
        crown(c, cx + lean, top - 4, glow, wither, bark);
        armLimb(c, cx, top, 1, arm, bark, false, wither);
        return c;
    }

    private static double ease(double t) { return t * t * (3 - 2 * t); }

    /**
     * Moonlight catches its upper-left edges: a pale violet rim (gold near the heart's glow) that lifts the
     * silhouette off the violet blight it stands on. A dead heap keeps only a faint grey rim.
     */
    private static void rim(Canvas c, boolean dead) {
        Canvas src = c.copy();
        for (int y = 1; y < CELL; y++) for (int x = 1; x < CELL; x++) {
            if (!src.opaque(x, y) || src.opaque(x - 1, y) && src.opaque(x, y - 1)) continue;
            int p = src.get(x, y);
            boolean gold = p == Pal.GOLD[1] || p == Pal.GOLD[2] || p == Pal.GOLD[3];
            c.set(x, y, dead ? Pal.STONE[3] : gold ? Pal.GOLD[4] : Pal.VIOLET[4]);
        }
    }

    /** The woven body: vertical grooves of root, darker toward the edges. */
    private static void trunk(Canvas c, double cx, double top, double bottom, int[] bark, double wither) {
        for (int y = (int) top; y <= bottom; y++) {
            double v = (y - top) / Math.max(1, bottom - top);
            int hw = (int) Math.round(8 + Math.sin(v * Math.PI) * 5 - v * 1.5);
            for (int x = (int) cx - hw; x <= cx + hw; x++) {
                double u = (x - cx) / hw;
                int i = u < -.5 ? 3 : u < .1 ? 2 : u < .6 ? 1 : 0;
                if (Math.floorMod(x * 3 + (int) (v * 5), 7) == 0) i = Math.max(0, i - 1);   // woven grooves
                c.set(x, y, bark[Math.max(0, Math.min(bark.length - 1, i + 1))]);
            }
        }
    }

    /** The gold heart behind three root ribs; its heat drives the size of the glow. */
    private static void heart(Canvas c, double cx, double cy, double glow, double wither) {
        double r = 4 + glow * 3;
        for (int y = (int) (cy - r - 3); y <= cy + r + 3; y++) for (int x = (int) (cx - r - 3); x <= cx + r + 3; x++) {
            double d = Math.hypot(x + .5 - cx, (y + .5 - cy) * 1.1);
            if (d > r + 2.5) continue;
            int colour;
            if (wither >= .85) colour = d < r * .6 ? Pal.STONE[1] : Pal.INK;               // a dead coal
            else if (d < r * .4) colour = glow > .8 ? Pal.GOLD[5] : Pal.GOLD[4];
            else if (d < r * .8) colour = Pal.GOLD[3];
            else if (d < r) colour = Pal.GOLD[2];
            else colour = c.opaque(x, y) ? Pal.GOLD[1] : 0;                                // glow spilling on the bark
            if (colour != 0) c.set(x, y, colour);
        }
        for (int side : new int[]{-1, 1}) {                                  // two roots cross over the heart like a cage
            for (double k = -r - 2; k <= r + 2; k += .5) {
                int x = (int) Math.round(cx + k * side), y = (int) Math.round(cy + k * .9 + Math.sin(k * .5) * .8);
                c.set(x, y, Pal.VIOLET[1]);
                c.set(x, y - 1, Math.abs(k) < r * .5 ? Pal.VIOLET[2] : Pal.VIOLET[1]);
            }
        }
        if (wither < .7) for (int v = 0; v < 6; v++) {                       // short veins of light running out from it
            double a = v * Math.PI / 3 + .3, len = r + 4 + (v % 2) * 4;
            for (double k = r + 1; k < len; k += .7) {
                int x = (int) Math.round(cx + Math.cos(a) * k), y = (int) Math.round(cy + Math.sin(a) * k * 1.1);
                if (c.opaque(x, y)) c.set(x, y, k < r + 3 ? Pal.GOLD[3] : Pal.GOLD[1]);
            }
        }
    }

    /** The head: a knot of roots, two slit eyes, a crown of thorned branches. */
    private static void crown(Canvas c, double cx, double top, double glow, double wither, int[] bark) {
        Creatures.ball(c, cx, top + 2, 9, 7, bark, 2);
        int eye = wither > .6 ? Pal.STONE[1] : glow > .85 ? Pal.GOLD[5] : Pal.GOLD[4];
        for (int side : new int[]{-1, 1}) {
            c.set((int) Math.round(cx + side * 3), (int) Math.round(top + 2), eye);
            c.set((int) Math.round(cx + side * 4), (int) Math.round(top + 2), eye);
            c.set((int) Math.round(cx + side * 3), (int) Math.round(top + 1), Pal.GOLD[2]);
        }
        double spread = 1 + glow * .4 - wither * .6;
        for (int b = 0; b < 5; b++) {                                       // antler-like thorned branches
            double a = -Math.PI / 2 + (b - 2) * .5 * spread, len = (b == 2 ? 14 : 11) * (1 - wither * .5);
            double ex = cx + Math.cos(a) * len, ey = top - 3 + Math.sin(a) * len;
            ForestArt.limb(c, cx + (b - 2) * 2, top - 3, ex, ey, 2, .8, bark);
            c.set((int) Math.round(ex), (int) Math.round(ey) - 1, Pal.BONE[3]);
            if (b != 2) { double mx = (cx + ex) / 2, my = (top - 3 + ey) / 2; c.set((int) Math.round(mx + (b < 2 ? -2 : 2)), (int) Math.round(my - 1), Pal.BONE[2]); }
        }
        for (int l = 0; l < 6 && wither < .5; l++) {                        // a few dead leaves caught in the crown
            int lx = (int) (cx - 10 + Noise.rnd(l, 1, 1210) * 20), ly = (int) (top - 12 + Noise.rnd(l, 2, 1210) * 10);
            if (c.opaque(lx, ly + 1)) c.set(lx, ly, l % 2 == 0 ? Pal.RUST[2] : Pal.VIOLET[3]);
        }
    }

    /** One root-arm from the shoulder: hanging, raised overhead, or slammed into the ground in front. */
    private static void armLimb(Canvas c, double cx, double top, int side, double arm, int[] bark, boolean far, double wither) {
        double sx = cx + side * 11, sy = top + 5;
        double hx, hy, ex, ey;                                              // elbow, hand
        if (arm >= 0) {                                                     // hanging clear of the body, or raised overhead
            hx = cx + side * (21 + 5 * arm); hy = sy + 13 - 28 * arm;
            ex = cx + side * (27 - 13 * arm); ey = sy + 34 - 60 * arm;
        } else {                                                            // the slam: both arms forward, hands driven into the ground in front
            double s = -arm;
            hx = cx + side * (21 - 1 * s); hy = sy + 13 + 9 * s;
            ex = cx + side * (27 - 13 * s); ey = sy + 34 + (FOOT + 6 - sy - 34) * s;
        }
        if (wither > .3) { ey = Math.min(FOOT, ey + wither * 10); hy += wither * 6; }
        int[] tone = far ? new int[]{Pal.INK, bark[0], bark[1], bark[1], bark[2]} : bark;
        ForestArt.limb(c, sx, sy, hx, hy, 4.2, 3.4, tone);
        ForestArt.limb(c, hx, hy, ex, ey, 3.4, 1.6, tone);
        for (int k = 1; k <= 3; k++) {                                      // thorns along the forearm
            double t = k / 4.0, tx = hx + (ex - hx) * t, ty = hy + (ey - hy) * t;
            c.set((int) Math.round(tx + side * 3), (int) Math.round(ty - 1), Pal.BONE[far ? 1 : 2]);
            c.set((int) Math.round(tx + side * 4), (int) Math.round(ty - 2), Pal.BONE[far ? 2 : 3]);
        }
        for (int finger = -1; finger <= 1; finger++)                        // root-fingers at the end
            ForestArt.limb(c, ex, ey, ex + side * 3 + finger * 3, ey + 4 - Math.abs(finger), 1.4, .6, tone);
    }
}
