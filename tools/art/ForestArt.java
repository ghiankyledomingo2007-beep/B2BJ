/**
 * Corrupted Forest set pieces, drawn to SetPieces.Kind metrics (row anchorY - 1 is the ground line).
 * Moonlight from the upper left like the Outpost; crowns are leaf clumps sorted back to front;
 * corruption reads as violet bark with thin gold veins (the "gold light between the roots").
 */
final class ForestArt {
    private ForestArt() { }

    static Canvas sheet(SetPieces.Kind kind) {
        return switch (kind) {
            case OAK -> Canvas.strip(oak(0), oak(1), oak(2));
            case PINE -> Canvas.strip(pine(0), pine(1));
            case WITHERED -> Canvas.strip(withered(0), withered(1));
            case BUSH -> Canvas.strip(bush(0), bush(1), bush(2));
            case LOG -> Canvas.strip(log(0), log(1));
            case BOULDER -> Canvas.strip(boulder(0), boulder(1));
            case STUMP -> stump();
            case BOAT -> boat();
            case CLOTH_POST -> Canvas.strip(clothPost(0), clothPost(1));
            case HOLLOW_TREE -> hollowTree();
            case MOONWELL -> moonwell();
            case ORCHARD_TREE -> Canvas.strip(orchardTree(0), orchardTree(1));
            case BRAMBLE -> Canvas.strip(bramble(0), bramble(1), bramble(2));
            case ROOT_WALL -> Canvas.strip(rootWall(0, false), rootWall(1, false));
            case BRIAR_ROOT -> Canvas.strip(rootWall(0, true), rootWall(1, true));
            case STAIR -> stair();
            case RIFT_STONE -> Canvas.strip(riftStone(0), riftStone(1));
            case LEAN_TO -> leanTo();
            default -> throw new IllegalArgumentException("not a forest piece: " + kind);
        };
    }

    // ------------------------------------------------------------------ shared drawing
    /** A leaf clump, moonlit from the upper left, broken into clusters; the lower rim falls to ink. */
    static void clump(Canvas c, double cx, double cy, double r, int seed, int[] ramp, int base) {
        for (int y = (int) (cy - r - 2); y <= cy + r + 2; y++) for (int x = (int) (cx - r - 2); x <= cx + r + 2; x++) {
            double dx = x + .5 - cx, dy = y + .5 - cy, d = Math.hypot(dx, dy * 1.08) / r;
            double rim = .84 + .3 * Noise.value(x, y, 2.4, seed);
            if (d > rim) continue;
            double lit = (-dx * .6 - dy * .8) / r + (Noise.value(x, y, 2.1, seed + 1) - .5) * .8;
            int i = base + (lit > .7 ? 2 : lit > .25 ? 1 : lit > -.3 ? 0 : -1);
            int colour = ramp[Math.max(0, Math.min(ramp.length - 1, i))];
            if (d > rim - .14 && dy > r * .2) colour = d > rim - .06 ? Pal.INK : ramp[Math.max(0, base - 2)];
            c.set(x, y, colour);
        }
    }

    /** A crown of clumps on an ellipse, drawn back (top) to front (bottom). */
    static void crown(Canvas c, double cx, double cy, double rx, double ry, int clumps, double clumpR, int seed, int[] ramp, int base) {
        java.util.List<double[]> list = new java.util.ArrayList<>();
        list.add(new double[]{cx, cy, Math.min(rx, ry) * .95});
        for (int i = 0; i < clumps; i++) {
            double a = Math.PI * 2 * i / clumps + Noise.rnd(i, seed, 501) * .7;
            double rr = .55 + Noise.rnd(i, seed, 502) * .4;
            list.add(new double[]{cx + Math.cos(a) * rx * rr, cy + Math.sin(a) * ry * rr, clumpR * (.75 + Noise.rnd(i, seed, 503) * .5)});
        }
        list.sort((p, q) -> Double.compare(p[1], q[1]));
        int k = 0;
        for (double[] p : list) clump(c, p[0], p[1], p[2], seed * 31 + k++, ramp, base);
    }

    /** Vertical trunk tapering upward; bark grooves, lit on the left. */
    static void trunk(Canvas c, int cx, int top, int ground, int baseW, int topW, int[] bark, int seed) {
        for (int y = top; y <= ground; y++) {
            double t = (double) (y - top) / Math.max(1, ground - top);
            int w = (int) Math.round(topW + (baseW - topW) * t * t);
            int l = cx - w / 2;
            for (int x = l; x < l + w; x++) {
                double u = (x + .5 - l) / w;
                int i = u < .25 ? 3 : u < .55 ? 2 : u < .85 ? 1 : 0;
                if (Noise.rnd(x, y / 4, seed) < .16) i = Math.max(0, i - 1);               // grooves
                if (Noise.rnd(x / 2, y / 3, seed + 7) < .04) i = Math.min(bark.length - 1, i + 1);
                c.set(x, y, bark[Math.min(bark.length - 1, i)]);
            }
        }
    }

    /** A root or branch as a thick line, shaded across its width. */
    static void limb(Canvas c, double x0, double y0, double x1, double y1, double w0, double w1, int[] bark) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) Math.ceil(len * 2);
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps, x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t, w = w0 + (w1 - w0) * t;
            for (int oy = (int) -Math.ceil(w); oy <= Math.ceil(w); oy++) for (int ox = (int) -Math.ceil(w); ox <= Math.ceil(w); ox++) {
                double d = Math.hypot(ox, oy) / Math.max(.5, w);
                if (d > 1) continue;
                int i = oy < -w * .3 ? 3 : oy < w * .3 ? 2 : 1;
                if (ox < -w * .4) i = Math.min(3, i + 1);
                c.set((int) Math.round(x + ox), (int) Math.round(y + oy), bark[Math.min(bark.length - 1, i)]);
            }
        }
    }

    static final int[] OAK_BARK = {Pal.WOOD[0], Pal.WOOD[1], Pal.WOOD[2], Pal.WOOD[3], Pal.WOOD[4]};
    static final int[] PALE_BARK = {Pal.STONE[1], Pal.STONE[2], Pal.STONE[3], Pal.STONE[4], Pal.STONE[5]};
    static final int[] ROT_BARK = {Pal.INK, Pal.VIOLET[0], Pal.VIOLET[1], Pal.VIOLET[2], Pal.VIOLET[3]};

    /** Thin glowing gold veins along a vertical span: the forest's corruption. */
    static void veins(Canvas c, int x0, int x1, int top, int bottom, int seed) {
        for (int v = 0; v < 3; v++) {
            double x = x0 + (x1 - x0) * Noise.rnd(v, seed, 511);
            for (int y = bottom; y > top + Noise.rnd(v, seed, 512) * (bottom - top) * .5; y--) {
                x += (Noise.rnd(v, y, seed) - .5) * 1.3;
                int ix = (int) Math.round(x);
                if (ix <= x0 || ix >= x1 || !c.opaque(ix, y)) continue;
                c.set(ix, y, Noise.rnd(ix, y, seed + 3) < .25 ? Pal.GOLD[5] : Pal.GOLD[3]);
            }
        }
    }

    // ------------------------------------------------------------------ trees
    static Canvas oak(int v) {
        var k = SetPieces.Kind.OAK;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 600 + v * 17;
        trunk(c, ax, 78, g, 18, 11, OAK_BARK, seed);
        for (int side : new int[]{-1, 1}) {                                  // root flare
            limb(c, ax + side * 5, g - 6, ax + side * (14 + v * 2), g + 1, 3.2, 1.2, OAK_BARK);
            limb(c, ax + side * 2, g - 3, ax + side * 7, g + 2, 2.4, 1, OAK_BARK);
        }
        limb(c, ax - 2, 98, ax - 22, 72, 3.5, 1.5, OAK_BARK);               // limbs into the crown
        limb(c, ax + 3, 92, ax + 24, 66, 3.2, 1.3, OAK_BARK);
        for (int y = 112; y < g - 8; y++) if (Noise.rnd(0, y / 3, seed + 9) < .5) c.set(ax - 7 + (y % 2), y, Pal.GRASS[2]);   // moss
        double rx = 50 - v * 3, ry = 44 - v * 2;
        crown(c, ax + (v - 1) * 2, 60 + v * 2, rx, ry, 9 + v, 21 - v, seed, Pal.GRASS, v == 2 ? 3 : 2);
        if (v == 2) for (int i = 0; i < 26; i++) {                           // a turning tree: a few rust leaves
            int x = (int) (ax - 40 + Noise.rnd(i, 1, seed) * 80), y = (int) (24 + Noise.rnd(i, 2, seed) * 70);
            if (c.opaque(x, y) && c.get(x, y) != Pal.INK) c.set(x, y, Noise.rnd(i, 3, seed) < .5 ? Pal.RUST[3] : Pal.RUST[4]);
        }
        return c;
    }

    static Canvas pine(int v) {
        var k = SetPieces.Kind.PINE;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 640 + v * 13;
        trunk(c, ax, g - 26, g, 9, 7, OAK_BARK, seed);
        limb(c, ax - 3, g - 4, ax - 9, g + 1, 2, 1, OAK_BARK);
        limb(c, ax + 3, g - 4, ax + 9, g + 1, 2, 1, OAK_BARK);
        int tiers = 6 + v;
        for (int t = 0; t < tiers; t++) {                                     // bottom tier first, upper tiers over it
            double f = t / (double) (tiers - 1);
            int base = (int) (g - 22 - t * (130.0 / tiers)), half = (int) Math.round(35 - f * 27), height = (int) (30 - f * 8);
            int lean = (int) Math.round((Noise.rnd(t, 1, seed) - .5) * 4);   // tiers sit a little off true
            for (int y = base - height; y <= base; y++) {
                double row = (y - (base - height)) / (double) height;
                int hl = (int) Math.round(half * Math.pow(row, .8) + (Noise.value(0, y, 3, seed + t) - .5) * 5);
                int hr = (int) Math.round(half * Math.pow(row, .8) + (Noise.value(1, y, 3, seed + t) - .5) * 5);
                int hw = Math.max(hl, hr);
                for (int x = ax + lean - hl; x <= ax + lean + hr; x++) {
                    if (y == base && Noise.rnd(x, t, seed) < .45) continue;            // ragged needle tips
                    double u = (x - ax - lean) / (double) Math.max(1, hw);
                    double lit = -u * .7 - row * .3 + (Noise.value(x, y, 2, seed + t) - .5) * .7;
                    int colour = lit > .35 ? Pal.GRASS[3] : lit > -.1 ? Pal.GRASS[2] : lit > -.5 ? Pal.GRASS[1] : Pal.GRASS[0];
                    if (row > .86) colour = Pal.GRASS[0];
                    c.set(x, y, colour);
                }
            }
        }
        c.set(ax, (int) (g - 22 - (tiers - 1) * (130.0 / tiers) - 24), Pal.GRASS[3]);
        return c;
    }

    static Canvas withered(int v) {
        var k = SetPieces.Kind.WITHERED;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 680 + v * 23;
        trunk(c, ax, 46, g, 16, 9, ROT_BARK, seed);
        for (int side : new int[]{-1, 1}) limb(c, ax + side * 5, g - 7, ax + side * 17, g + 1, 3.4, 1.2, ROT_BARK);
        branchOut(c, ax, 52, -Math.PI / 2 - .35, 34, 4, 4, seed);              // claw-like crown, no leaves
        branchOut(c, ax, 60, -Math.PI / 2 + .45, 32, 3.6, 4, seed + 5);
        branchOut(c, ax - 2, 84, Math.PI + .5, 26, 3, 3, seed + 9);
        branchOut(c, ax + 2, 78, -.35, 24, 3, 3, seed + 13);
        veins(c, ax - 7, ax + 7, 56, g - 4, seed);
        for (int i = 0; i < 9; i++) {                                         // a few dead leaves left
            int x = (int) (ax - 36 + Noise.rnd(i, 7, seed) * 72), y = (int) (10 + Noise.rnd(i, 8, seed) * 60);
            if (c.opaque(x, y + 1)) c.set(x, y, Pal.RUST[2]);
        }
        return c;
    }
    private static void branchOut(Canvas c, double x, double y, double angle, double len, double w, int depth, int seed) {
        double ex = x + Math.cos(angle) * len, ey = y + Math.sin(angle) * len;
        limb(c, x, y, ex, ey, w, Math.max(.6, w * .55), ROT_BARK);
        if (depth <= 1) return;
        double spread = .45 + Noise.rnd(depth, seed, 521) * .3;
        branchOut(c, ex, ey, angle - spread, len * .66, w * .6, depth - 1, seed * 3 + 1);
        branchOut(c, ex, ey, angle + spread * .8, len * .6, w * .55, depth - 1, seed * 3 + 2);
    }

    static Canvas orchardTree(int v) {
        var k = SetPieces.Kind.ORCHARD_TREE;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 720 + v * 11;
        trunk(c, ax, 58, g, 12, 8, OAK_BARK, seed);
        branchFruit(c, ax, 62, -Math.PI / 2 - .55, 30, 3, 3, seed);
        branchFruit(c, ax, 64, -Math.PI / 2 + .6, 28, 3, 3, seed + 4);
        branchFruit(c, ax, 72, -Math.PI / 2, 26, 2.6, 3, seed + 8);
        for (int i = 0; i < 40; i++) {                                         // black leaves in small clusters
            double a = Noise.rnd(i, 1, seed) * Math.PI * 2, r = 8 + Noise.rnd(i, 2, seed) * 30;
            int x = (int) (ax + Math.cos(a) * r * 1.2), y = (int) (40 + Math.sin(a) * r * .7);
            c.set(x, y, Pal.EARTH[0]); c.set(x + 1, y, Pal.INK); c.set(x, y + 1, Pal.EARTH[1]);
        }
        for (int i = 0; i < 4 + v; i++) {                                     // windfall under the tree
            int x = (int) (ax - 22 + Noise.rnd(i, 4, seed) * 44), y = g - 1 + (int) (Noise.rnd(i, 5, seed) * 3);
            c.set(x, y, Pal.RUST[2]); c.set(x + 1, y, Pal.CRIMSON[2]); c.set(x, y - 1, Pal.RUST[3]);
        }
        return c;
    }
    private static void branchFruit(Canvas c, double x, double y, double angle, double len, double w, int depth, int seed) {
        double ex = x + Math.cos(angle) * len, ey = y + Math.sin(angle) * len;
        limb(c, x, y, ex, ey, w, Math.max(.6, w * .6), OAK_BARK);
        if (depth > 1) {
            branchFruit(c, ex, ey, angle - .5, len * .62, w * .62, depth - 1, seed * 3 + 1);
            branchFruit(c, ex, ey, angle + .45, len * .58, w * .6, depth - 1, seed * 3 + 2);
        } else if (Noise.rnd((int) ex, (int) ey, seed) < .6) {
            c.set((int) ex, (int) ey + 1, Pal.CRIMSON[3]); c.set((int) ex, (int) ey + 2, Pal.CRIMSON[1]);   // a withered fruit
        }
    }

    static Canvas hollowTree() {
        var k = SetPieces.Kind.HOLLOW_TREE;
        Canvas c = Structures.canvas(k, 6);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 760;
        for (int i = 0; i < 7; i++) {                                         // great roots splay into the ground
            double a = Math.PI * (.08 + i * .14), len = 46 + Noise.rnd(i, 1, seed) * 34;
            double sx = ax + Math.cos(Math.PI - a) * 30, ex = ax + Math.cos(Math.PI - a) * (30 + len);
            limb(c, sx, g - 22, ex, g + 2 - Noise.rnd(i, 2, seed) * 6, 9, 2, OAK_BARK);
        }
        trunk(c, ax, 108, g - 4, 92, 58, OAK_BARK, seed);
        // the hollow: a tall dark mouth with a lip of lit bark and a faint gold glow deep inside
        for (int y = g - 64; y <= g - 6; y++) {
            double t = (y - (g - 64)) / 58.0;
            int hw = (int) Math.round(15 * Math.sqrt(Math.min(1, t * 1.6)));
            for (int x = ax - hw; x <= ax + hw; x++) {
                int colour = Pal.INK;
                if (t > .55 && Math.abs(x - ax) < hw - 4 && Noise.rnd(x, y, seed) < .12) colour = Pal.GOLD[0];
                c.set(x, y, colour);
            }
            c.set(ax - hw - 1, y, Pal.WOOD[4]);
            c.set(ax + hw + 1, y, Pal.WOOD[1]);
        }
        for (int y = 120; y < g - 30; y++) if (Noise.rnd(1, y / 3, seed + 3) < .55) { c.set(ax - 40 + y % 3, y, Pal.GRASS[2]); c.set(ax - 38, y, Pal.GRASS[3]); }
        limb(c, ax - 20, 128, ax - 74, 84, 9, 3, OAK_BARK);
        limb(c, ax + 22, 122, ax + 80, 82, 8, 3, OAK_BARK);
        limb(c, ax + 6, 112, ax + 14, 60, 7, 3, OAK_BARK);
        crown(c, ax, 72, 108, 66, 14, 34, seed, Pal.GRASS, 2);
        limb(c, ax - 70, 118, ax - 104, 96, 3, 1, OAK_BARK);                  // one bare limb out of the canopy
        return c;
    }

    // ------------------------------------------------------------------ undergrowth
    static Canvas bush(int v) {
        var k = SetPieces.Kind.BUSH;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 800 + v * 7;
        clump(c, ax - 11, g - 12, 12, seed, Pal.GRASS, 2);
        clump(c, ax + 11, g - 11, 12, seed + 1, Pal.GRASS, 2);
        clump(c, ax, g - 17, 14, seed + 2, Pal.GRASS, v == 2 ? 3 : 2);
        for (int i = 0; i < (v == 0 ? 0 : 9); i++) {                          // berries or pale blossoms
            int x = (int) (ax - 18 + Noise.rnd(i, 1, seed) * 36), y = (int) (g - 26 + Noise.rnd(i, 2, seed) * 18);
            if (!c.opaque(x, y) || c.get(x, y) == Pal.INK) continue;
            c.set(x, y, v == 1 ? Pal.CRIMSON[4] : Pal.BONE[3]);
            if (v == 1) c.set(x, y + 1, Pal.CRIMSON[1]);
        }
        return c;
    }

    static Canvas log(int v) {
        var k = SetPieces.Kind.LOG;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, seed = 820 + v;
        int left = 8, right = 98, cy = g - 10, r = 9;
        for (int x = left; x <= right; x++) for (int y = cy - r; y <= cy + r; y++) {
            double u = (y + .5 - cy) / r;
            if (x > right - 4 && Noise.rnd(x, y, seed) < (x - (right - 4)) / 4.0) continue;      // broken end
            int i = u < -.55 ? 4 : u < -.1 ? 3 : u < .45 ? 2 : u < .8 ? 1 : 0;
            if (Noise.rnd(x / 5, y, seed) < .18) i = Math.max(0, i - 1);
            c.set(x, y, Pal.WOOD[i]);
        }
        for (int y = cy - r; y <= cy + r; y++) for (int x = left - 5; x <= left + 4; x++) {        // cut end with rings
            double dx = (x + .5 - left) / 5.0, dy = (y + .5 - cy) / r, d = Math.hypot(dx, dy);
            if (d > 1) continue;
            int ring = (int) (d * 4);
            c.set(x, y, ring % 2 == 0 ? Pal.WOOD[4] : Pal.WOOD[3]);
            if (d < .2) c.set(x, y, Pal.WOOD[2]);
        }
        for (int x = left + 6; x < right - 8; x++) if (Noise.value(x, 0, 6, seed) > .45) {         // moss along the top
            c.set(x, cy - r, Pal.GRASS[3]); if (Noise.rnd(x, 1, seed) < .5) c.set(x, cy - r - 1, Pal.GRASS[2]);
        }
        if (v == 1) for (int m = 0; m < 3; m++) {                             // a cluster of pale caps
            int x = 44 + m * 7, y = cy - r - 2;
            c.set(x, y, Pal.BONE[3]); c.set(x + 1, y, Pal.BONE[2]); c.set(x - 1, y, Pal.BONE[3]); c.set(x, y + 1, Pal.BONE[1]);
        }
        return c;
    }

    static Canvas boulder(int v) {
        var k = SetPieces.Kind.BOULDER;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 840 + v;
        rock(c, ax + (v == 1 ? -8 : 0), g - 14, 22, 15, seed);
        if (v == 1) rock(c, ax + 15, g - 7, 11, 8, seed + 3);
        return c;
    }
    private static void rock(Canvas c, double cx, double cy, double rx, double ry, int seed) {
        for (int y = (int) (cy - ry - 2); y <= cy + ry + 1; y++) for (int x = (int) (cx - rx - 2); x <= cx + rx + 2; x++) {
            double dx = (x + .5 - cx) / rx, dy = (y + .5 - cy) / ry, d = Math.hypot(dx, dy);
            if (d > .92 + .16 * Noise.value(x, y, 3, seed)) continue;
            double lit = -dx * .6 - dy * .8 + (Noise.value(x, y, 2.5, seed + 1) - .5) * .5;
            int colour = lit > .55 ? Pal.STONE[5] : lit > .1 ? Pal.STONE[4] : lit > -.4 ? Pal.STONE[3] : Pal.STONE[2];
            if (dy < -.35 && Noise.value(x, y, 3, seed + 2) > .48) colour = lit > .3 ? Pal.GRASS[3] : Pal.GRASS[2];   // moss cap
            if (Noise.rnd(x, y, seed + 4) < .03) colour = Pal.STONE[1];
            c.set(x, y, colour);
        }
        c.selectiveOutline();
    }

    static Canvas stump() {
        var k = SetPieces.Kind.STUMP;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, ax = k.anchorX;
        for (int side : new int[]{-1, 1}) limb(c, ax + side * 6, g - 4, ax + side * 13, g + 1, 2.4, 1, OAK_BARK);
        trunk(c, ax, g - 14, g, 18, 18, OAK_BARK, 860);
        for (int y = g - 19; y <= g - 11; y++) for (int x = ax - 9; x <= ax + 9; x++) {   // the cut face, rings
            double dx = (x + .5 - ax) / 9.0, dy = (y + .5 - (g - 15)) / 4.0, d = Math.hypot(dx, dy);
            if (d > 1) continue;
            c.set(x, y, (int) (d * 4) % 2 == 0 ? Pal.WOOD[4] : Pal.WOOD[3]);
        }
        c.set(ax + 6, g - 8, Pal.BONE[3]); c.set(ax + 7, g - 8, Pal.BONE[2]);   // a pale cap on the bark
        return c;
    }

    static Canvas bramble(int v) {
        var k = SetPieces.Kind.BRAMBLE;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 880 + v * 5;
        for (int i = 0; i < 9; i++) clump(c, ax - 30 + i * 7.5, g - 10 - Noise.rnd(i, 0, seed) * 10, 8 + Noise.rnd(i, 1, seed) * 3, seed + i, Pal.GRASS, 1);
        for (int vine = 0; vine < 11; vine++) {                               // thorned canes arching over the leaves
            double x = ax - 34 + Noise.rnd(vine, 2, seed) * 68, y = g - 2, a = -Math.PI / 2 + (Noise.rnd(vine, 3, seed) - .5) * 1.8;
            for (int s = 0; s < 26; s++) {
                x += Math.cos(a); y += Math.sin(a) * .8; a += (Noise.rnd(vine, s, seed) - .4) * .25;
                int ix = (int) Math.round(x), iy = (int) Math.round(y);
                if (iy < 2) break;
                c.set(ix, iy, s % 2 == 0 ? Pal.WOOD[1] : Pal.VIOLET[1]);
                if (s % 4 == 2) c.set(ix + 1, iy - 1, Pal.BONE[2]);           // a thorn
            }
        }
        for (int b = 0; b < 3 + v; b++) {                                      // violet blooms
            int x = (int) (ax - 26 + Noise.rnd(b, 4, seed) * 52), y = (int) (g - 26 + Noise.rnd(b, 5, seed) * 14);
            c.set(x, y, Pal.VIOLET[4]); c.set(x + 1, y, Pal.VIOLET[5]); c.set(x, y + 1, Pal.VIOLET[3]);
        }
        return c;
    }

    // ------------------------------------------------------------------ landmarks
    /** Sedge's ferry: a long flat punt stranded on the dry bed, bow raised, pole and white cloth aboard. */
    static Canvas boat() {
        var k = SetPieces.Kind.BOAT;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 900;
        int xl = ax - 78, xr = ax + 78;
        double beam = 12.5, cy0 = g - 22;
        int[] top = new int[c.w], bot = new int[c.w];
        for (int x = xl; x <= xr; x++) {
            double t = (x - xl) / (double) (xr - xl);
            double bw = beam * (t < .68 ? .78 + .22 * Math.sin(t / .68 * Math.PI / 2) : Math.cos((t - .68) / .32 * Math.PI / 2));
            double cy = cy0 - t * 6;                                       // the bow rides up on the bank
            top[x] = (int) Math.round(cy - bw); bot[x] = (int) Math.round(cy + bw);
        }
        for (int x = xl; x <= xr; x++) {
            if (bot[x] <= top[x]) continue;
            // near side planks: from the near gunwale down to the ground the hull rests on
            int keel = bot[x] + 9 - (int) Math.round((x - xl) / (double) (xr - xl) * 3);
            for (int y = bot[x]; y <= keel; y++) {
                int strake = (y - bot[x]) / 3;
                int colour = strake == 0 ? Pal.WOOD[3] : strake == 1 ? Pal.WOOD[2] : Pal.WOOD[1];
                if ((y - bot[x]) % 3 == 2) colour = Pal.WOOD[0];
                if (Noise.rnd(x / 6, y, seed) < .05) colour = Pal.WOOD[0];
                c.set(x, y, colour);
            }
            // inside: the far wall catches the moon, the floor is in shade, ribs every 12 px
            for (int y = top[x]; y < bot[x]; y++) {
                int depth = y - top[x];
                int colour = depth < 4 ? Pal.WOOD[3] : Pal.WOOD[1];
                if (depth >= 4 && (x - xl) % 12 == 0) colour = Pal.WOOD[0];
                c.set(x, y, colour);
            }
            c.set(x, top[x], Pal.WOOD[5]);                                // lit gunwales
            c.set(x, bot[x], Pal.WOOD[4]);
        }
        for (int s2 = 0; s2 < 2; s2++) {                                   // two thwarts across the hull
            int x = xl + 46 + s2 * 44;
            for (int y = top[x] + 1; y < bot[x]; y++) { c.set(x, y, Pal.WOOD[4]); c.set(x + 1, y, Pal.WOOD[4]); c.set(x + 2, y, Pal.WOOD[2]); }
        }
        // a coil of rope in the stern, a bundle of white cloth amidships, the pole laid across the thwarts
        for (int r = 0; r < 3; r++) c.ellipse(xl + 20, cy0 - 1, 6.5 - r * 1.8, 3.4 - r, r % 2 == 0 ? Pal.CANVAS[4] : Pal.CANVAS[2]);
        c.ellipse(xl + 70, cy0 - 3, 12, 5, Pal.CANVAS[3]);
        c.ellipse(xl + 69, cy0 - 4, 10, 3.5, Pal.BONE[3]);
        for (int x = xl + 61; x < xl + 79; x += 4) c.set(x, (int) cy0 - 4, Pal.CANVAS[4]);
        c.line(xl - 6, (int) cy0 - 9, xr - 14, (int) cy0 - 20, Pal.WOOD[4]);
        c.line(xl - 6, (int) cy0 - 8, xr - 14, (int) cy0 - 19, Pal.WOOD[2]);
        c.selectiveOutline();
        return c;
    }

    static Canvas clothPost(int v) {
        var k = SetPieces.Kind.CLOTH_POST;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, x = k.anchorX;
        for (int y = 8; y <= g; y++) { c.set(x + (y < 30 ? 1 : 0), y, Pal.WOOD[3]); c.set(x + 1 + (y < 30 ? 1 : 0), y, Pal.WOOD[1]); }
        int dir = v == 0 ? 1 : -1, tie = 16;
        for (int i = 0; i < 18; i++) {                                         // the cloth streams off the stick
            int cx = x + 2 + dir * i, wave = (int) Math.round(Math.sin(i * .55 + v) * 1.6);
            int w = Math.max(2, 5 - i / 5);
            for (int j = 0; j < w; j++) c.set(cx, tie + wave + j, j == 0 ? Pal.BONE[3] : j == w - 1 ? Pal.CANVAS[3] : Pal.CANVAS[5]);
        }
        c.set(x, tie - 1, Pal.CANVAS[2]); c.set(x + 1, tie - 1, Pal.CANVAS[2]);
        return c;
    }

    static Canvas moonwell() {
        var k = SetPieces.Kind.MOONWELL;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 940, cy = g - 26;
        // stone ring: blocks lit from the upper left, mortar lines between them
        for (int y = cy - 26; y <= g; y++) for (int x = ax - 60; x <= ax + 60; x++) {
            double dx = (x + .5 - ax) / 58.0, dy = (y + .5 - cy) / 24.0, d = Math.hypot(dx, dy);
            double wall = y > cy ? (y - cy) / 24.0 : 0;
            if (d > 1 && !(Math.abs(dx) <= 1 && y > cy && y <= g && Math.hypot(dx, 0) <= 1 && (y - cy) <= 10 + 14 * Math.sqrt(1 - dx * dx))) continue;
            double a = Math.atan2(dy, dx);
            int block = (int) Math.floor((a + Math.PI) / (Math.PI * 2) * 22);
            boolean mortar = Math.floorMod((int) Math.round((a + Math.PI) / (Math.PI * 2) * 22 * 6), 6) == 0;
            int tone = Noise.rnd(block, 0, seed) < .5 ? 3 : 4;
            double lit = -dx * .6 - dy * .8;
            int colour = Pal.STONE[Math.max(1, Math.min(6, tone + (lit > .45 ? 1 : lit < -.4 ? -1 : 0)))];
            if (d > 1) colour = Pal.STONE[2 + (int) (wall * 0)];                      // the outer face below the rim
            if (mortar && d <= 1) colour = Pal.STONE[1];
            if (Noise.value(x, y, 4, seed + 1) > .62 && dy < .1) colour = Pal.GRASS[Noise.rnd(x, y, seed) < .5 ? 2 : 3];   // moss
            c.set(x, y, colour);
        }
        // the water: pale with the moon in it
        for (int y = cy - 16; y <= cy + 14; y++) for (int x = ax - 46; x <= ax + 46; x++) {
            double dx = (x + .5 - ax) / 45.0, dy = (y + .5 - cy) / 15.0, d = Math.hypot(dx, dy);
            if (d > 1) continue;
            int colour = d > .86 ? Pal.WATER[1] : Pal.WATER[3];
            if (dy < -.4 && d > .7) colour = Pal.WATER[0];                              // the rim's shadow on the water
            double moon = Math.hypot((x + .5 - (ax - 10)) / 9.0, (y + .5 - (cy - 2)) / 4.0);
            if (moon < 1) colour = moon < .55 ? Pal.CYAN[5] : Pal.WATER[5];
            else if (Noise.rnd(x / 3, y, seed + 2) < .05) colour = Pal.CYAN[4];
            c.set(x, y, colour);
        }
        for (int i = 0; i < 10; i++) {                                         // white flowers round the stones
            double a = Math.PI * (.15 + i * .07), x = ax + Math.cos(a) * 62 * (i % 2 == 0 ? 1 : -1), y = cy + Math.sin(a) * 26 + 3;
            c.set((int) x, (int) y, Pal.BONE[3]); c.set((int) x + 1, (int) y, Pal.GOLD[4]);
        }
        return c;
    }

    /** Roots arching out of the ground in a low wall; `rot` turns them into Briarheart's thorned, gold-veined roots. */
    static Canvas rootWall(int v, boolean rot) {
        var k = rot ? SetPieces.Kind.BRIAR_ROOT : SetPieces.Kind.ROOT_WALL;
        Canvas c = Structures.canvas(k, 3);
        int g = k.anchorY - 1, ax = k.anchorX, seed = (rot ? 980 : 960) + v * 9;
        int[] bark = rot ? ROT_BARK : OAK_BARK;
        for (int r = 0; r < 4; r++) {                                       // thick where it leaves the earth, thin where it dives back
            double x0 = ax - 70 + r * 32 + Noise.rnd(r, 1, seed) * 10, x1 = x0 + 40 + Noise.rnd(r, 2, seed) * 24;
            if ((r + v) % 2 == 1) { double t = x0; x0 = x1; x1 = t; }
            double h = 16 + Noise.rnd(r, 3, seed) * (rot ? 22 : 16), w0 = 7 + Noise.rnd(r, 4, seed) * 2.5;
            root(c, x0, x1, g, h, w0, 2.5, bark, seed + r * 11, rot);
            for (int m = -3; m <= 3; m++) {                                 // a little heaved earth at each end
                c.set((int) x0 + m, g + 1, Pal.MUD[1]); c.set((int) x1 + m / 2, g + 1, Pal.MUD[1]);
                if (Math.abs(m) < 2) { c.set((int) x0 + m, g, Pal.MUD[2]); }
            }
        }
        if (rot) veins(c, ax - 72, ax + 72, 4, g, seed);
        c.selectiveOutline();
        return c;
    }
    /** One arching root: bark grooves run along it, moonlight on its back, shade beneath. */
    private static void root(Canvas c, double x0, double x1, int ground, double h, double w0, double w1, int[] bark, int seed, boolean rot) {
        int steps = (int) Math.abs(x1 - x0) * 3;
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps, x = x0 + (x1 - x0) * t, y = ground - Math.sin(t * Math.PI) * h;
            double dx = (x1 - x0) / steps, dy = -Math.cos(t * Math.PI) * Math.PI * h / steps;
            double len = Math.hypot(dx, dy), nx = -dy / len, ny = dx / len;
            if (ny > 0) { nx = -nx; ny = -ny; }                             // the normal that points up the screen is the lit back
            double w = w0 + (w1 - w0) * t;
            for (double o = -w; o <= w; o += .5) {
                double u = o / w;                                             // 1 = top (lit), -1 = underside
                int i = u > .45 ? 4 : u > -.1 ? 3 : u > -.6 ? 2 : 1;
                if (Noise.rnd(s / 4, (int) Math.round((o + w) * 1.5), seed) < .2) i = Math.max(0, i - 2);   // grooves
                int px = (int) Math.round(x + nx * o), py = (int) Math.round(y + ny * o);
                if (py > ground) continue;
                c.set(px, py, bark[Math.min(bark.length - 1, i)]);
            }
            int topX = (int) Math.round(x + nx * w), topY = (int) Math.round(y + ny * w);
            if (!rot && Noise.value(s, 0, 9, seed) > .55 && topY < ground - 1) c.set(topX, topY - 1, Pal.GRASS[Noise.rnd(s, 1, seed) < .5 ? 2 : 3]);
            if (rot && s % 9 == 4 && topY < ground - 2) { c.set(topX, topY - 1, Pal.BONE[2]); c.set(topX, topY - 2, Pal.BONE[3]); }
        }
    }

    static Canvas stair() {
        var k = SetPieces.Kind.STAIR;
        Canvas c = Structures.canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 1000;
        int mouthTop = g - 92, hw = 58;
        // steps falling away into the dark; the deepest catch the gold light from below
        for (int s = 0; s < 9; s++) {
            int y0 = mouthTop + s * 10, y1 = y0 + 9, inset = s * 3;
            for (int y = y0; y <= y1; y++) for (int x = ax - hw + inset; x <= ax + hw - inset; x++) {
                int depth = 8 - s;
                int colour = y - y0 < 2 ? Pal.STONE[Math.max(1, 5 - depth / 2)] : Pal.STONE[Math.max(0, 3 - depth / 3)];
                if (depth >= 5) colour = y - y0 < 2 ? Pal.STONE[2] : Pal.INK;
                if (depth >= 6 && Noise.rnd(x, y, seed) < .1) colour = Pal.GOLD[depth == 8 ? 2 : 1];
                c.set(x, y, colour);
            }
        }
        // stone cheeks either side, then roots wrapping them
        for (int side : new int[]{-1, 1}) {
            int x0 = side < 0 ? ax - hw - 30 : ax + hw, x1 = side < 0 ? ax - hw : ax + hw + 30;
            Kit.stoneBox(c, x0, g, x1 - x0, 18, 86, seed + side, true);
        }
        for (int side : new int[]{-1, 1}) for (int r = 0; r < 3; r++) {
            double x = ax + side * (hw + 14), y = g - 10 - r * 26;
            double ex = ax + side * (hw - 18 + r * 4), ey = y - 22;
            limb(c, x + side * 24, y + 6, x, y, 4, 4, ROT_BARK);
            limb(c, x, y, ex, ey, 4, 2, ROT_BARK);
            c.set((int) ex, (int) ey - 3, Pal.BONE[3]);
        }
        veins(c, ax - hw - 30, ax + hw + 30, mouthTop - 20, g, seed);
        // a broken lintel of carved stone over the mouth
        Kit.stoneBox(c, ax - hw - 18, mouthTop + 4, (hw + 18) * 2, 10, 12, seed + 7, true);
        c.selectiveOutline();
        return c;
    }

    static Canvas riftStone(int v) {
        var k = SetPieces.Kind.RIFT_STONE;
        Canvas c = Structures.canvas(k, 2);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 1040 + v * 3;
        int top = v == 1 ? 28 : 8;
        for (int y = top; y <= g; y++) {
            double t = (y - top) / (double) (g - top);
            int hw = (int) Math.round(9 + t * 9 + (v == 1 && y < top + 6 ? -Noise.rnd(0, y, seed) * 6 : 0));
            for (int x = ax - hw; x <= ax + hw; x++) {
                double u = (x + .5 - ax) / hw;
                int colour = u < -.4 ? Pal.STONE[4] : u < .3 ? Pal.STONE[3] : u < .75 ? Pal.STONE[2] : Pal.STONE[1];
                if (Noise.rnd(x / 2, y / 3, seed) < .06) colour = Pal.STONE[1];
                if (y > g - 10 && Noise.value(x, y, 3, seed) > .45) colour = Pal.GRASS[2];
                c.set(x, y, colour);
            }
        }
        double x = ax + 1;                                                     // the rift crack, glowing violet
        for (int y = top + 6; y < g - 8; y++) {
            x += (Noise.rnd(1, y, seed) - .5) * 1.4;
            int ix = (int) Math.round(x);
            c.set(ix, y, Noise.rnd(ix, y, seed + 1) < .3 ? Pal.VIOLET[5] : Pal.VIOLET[4]);
            c.set(ix - 1, y, Pal.VIOLET[2]);
        }
        for (int r = 0; r < 4; r++) { int ry = top + 16 + r * 12; c.set(ax - 6, ry, Pal.VIOLET[3]); c.set(ax - 5, ry + 1, Pal.VIOLET[3]); c.set(ax - 7, ry + 1, Pal.VIOLET[3]); }
        c.selectiveOutline();
        return c;
    }

    /**
     * The wayfinder's lean-to, seen from its open front: a ridge pole on two forked posts, bark slabs
     * slanting back to the ground (their underside in shadow), side triangles, a bedroll and pack inside.
     */
    static Canvas leanTo() {
        var k = SetPieces.Kind.LEAN_TO;
        Canvas c = Structures.canvas(k, 3);
        int g = k.anchorY - 1, ax = k.anchorX, seed = 1060;
        int ridge = g - 60, back = g - 30, l = ax - 54, r = ax + 54;
        for (int side : new int[]{-1, 1}) {                                // side walls: triangles of stacked branches
            int x0 = side < 0 ? l - 6 : r + 6;
            for (int y = ridge; y <= g; y++) {
                double t = (y - ridge) / (double) (g - ridge);
                int reach = (int) Math.round(t * 10);
                for (int i = 0; i <= reach; i++) {
                    int x = x0 + side * i;
                    c.set(x, y, (y + i) % 4 == 0 ? Pal.WOOD[1] : Pal.WOOD[2]);
                }
            }
        }
        for (int x = l; x <= r; x++) for (int y = ridge; y <= back; y++) {  // the roof's underside, darker toward the back
            double t = (y - ridge) / (double) (back - ridge);
            int slab = (x - l) / 8;
            int colour = t < .25 ? Pal.WOOD[3] : t < .6 ? Pal.WOOD[2] : Pal.WOOD[1];
            if ((x - l) % 8 == 0) colour = Pal.WOOD[0];
            if (slab % 3 == 1 && t > .3) colour = Pal.shift(colour, -1);
            c.set(x, y, colour);
        }
        for (int x = l; x <= r; x++) for (int y = back + 1; y <= g; y++) c.set(x, y, Pal.EARTH[0]);   // the sheltered floor
        c.ellipse(ax - 8, g - 7, 30, 6, Pal.CRIMSON[1]);                    // bedroll, its top catching what light there is
        c.ellipse(ax - 8, g - 8, 28, 4, Pal.CRIMSON[2]);
        for (int x = ax - 34; x < ax + 18; x += 7) c.set(x, g - 9, Pal.CRIMSON[3]);
        c.ellipse(ax + 34, g - 8, 8, 7, Pal.CANVAS[2]);                     // pack
        c.ellipse(ax + 33, g - 10, 6, 4, Pal.CANVAS[3]);
        for (int x = l - 8; x <= r + 8; x++) {                              // the ridge pole, lit on top
            c.set(x, ridge - 2, Pal.WOOD[5]); c.set(x, ridge - 1, Pal.WOOD[4]); c.set(x, ridge, Pal.WOOD[2]);
        }
        for (int px : new int[]{l + 2, r - 2}) {                            // forked posts
            for (int y = ridge - 6; y <= g; y++) { c.set(px, y, Pal.WOOD[4]); c.set(px + 1, y, Pal.WOOD[2]); }
            c.set(px - 1, ridge - 6, Pal.WOOD[4]); c.set(px - 2, ridge - 7, Pal.WOOD[4]); c.set(px + 2, ridge - 6, Pal.WOOD[2]); c.set(px + 3, ridge - 7, Pal.WOOD[2]);
        }
        for (int i = 0; i < 13; i++) for (int j = 0; j < 3; j++)              // white cloth tied to the right post
            c.set(r + 1 + i, ridge + 4 + (int) Math.round(Math.sin(i * .6) * 1.4) + j, j == 0 ? Pal.BONE[3] : j == 2 ? Pal.CANVAS[3] : Pal.CANVAS[4]);
        c.selectiveOutline();
        return c;
    }

}
