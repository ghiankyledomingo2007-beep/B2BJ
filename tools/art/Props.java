/**
 * Standing obstacles of the Outpost, drawn in the game's 3/4 view at character scale (a person is
 * about 64 native pixels tall): a front face rising from the collision footprint's front edge, a
 * top face above it, moonlight from the upper left. Footprints match RuinedOutpostMap.Prop, so
 * collision is unchanged; only the art grew to its true size.
 */
final class Props {
    private Props() { }
    static final int WALL_W = 96, WALL_H = 80, WALL_GROUND = 75;          // anchor (48, 76)
    static final int STAKE_W = 96, STAKE_H = 112, STAKE_GROUND = 107;     // anchor (48, 108)
    static final int SUPPLY_W = 56, SUPPLY_H = 72, SUPPLY_GROUND = 67;    // anchor (28, 68)

    // ------------------------------------------------------------------ ruined stone wall
    static Canvas wall(int v) {
        Canvas c = new Canvas(WALL_W, WALL_H);
        int ground = WALL_GROUND, depth = 11, x0 = 7, x1 = 88, seed = 500 + v * 17;
        int[] h = new int[WALL_W];
        for (int x = x0; x <= x1; x++) {
            int block = (x - x0) / 13;
            double ruin = Noise.rnd(block, v, seed);
            int top = 52 - (ruin < .25 ? 16 : ruin < .45 ? 8 : 0) - (int) (Noise.rnd(block, v, seed + 1) * 4);
            int fromEnd = Math.min(x - x0, x1 - x);
            if (fromEnd < 14) top = Math.min(top, 16 + fromEnd * 3 - (int) (Noise.rnd(x, v, seed + 2) * 4));
            h[x] = Math.max(8, top);
        }
        for (int x = x0; x <= x1; x++) {
            int topY = ground - h[x];
            for (int y = topY; y <= ground; y++) {
                int fromGround = ground - y, course = fromGround / 9, inCourse = fromGround % 9;
                int shiftX = x - x0 + (course % 2) * 7;
                int joint = Math.floorMod(shiftX, 14);
                int blockId = shiftX / 14 * 31 + course;
                int tone = Noise.rnd(blockId, v, seed + 3) < .3 ? 3 : 4;
                int colour;
                if (inCourse == 8 || joint == 0) colour = Pal.STONE[1];
                else if (inCourse == 7) colour = Pal.STONE[tone + 1];
                else if (inCourse <= 1) colour = Pal.STONE[tone - 1];
                else if (joint == 1) colour = Pal.STONE[tone + 1];
                else if (joint == 13) colour = Pal.STONE[tone - 1];
                else colour = Noise.value(x, y, 3, seed + 10) > .72 ? Pal.STONE[tone + 1] : Pal.STONE[tone];
                if (fromGround < 6 && Noise.rnd(x, y, seed + 11) < (6 - fromGround) / 12.0) colour = Pal.shift(colour, -1); // damp foot
                c.set(x, y, colour);
            }
            for (int y = topY - depth; y < topY; y++) {
                boolean lip = y == topY - depth;
                int colour = lip ? Pal.STONE[6] : y == topY - 1 ? Pal.STONE[4] : Pal.STONE[5];
                double moss = Noise.value(x, y, 5, seed + 4);
                if (!lip && moss > .56) colour = moss > .72 ? Pal.GRASS[3] : Pal.GRASS[2];
                if (lip && moss > .62) colour = Pal.GRASS[4];
                c.set(x, y, colour);
            }
        }
        for (int x = x0 + 1; x <= x1; x++) if (h[x] != h[x - 1]) {
            int low = h[x] < h[x - 1] ? x : x - 1;
            c.set(low, ground - h[low] - depth, Pal.STONE[7]);
        }
        for (int k = 0; k < 5; k++) {                                   // cracks
            int cx = x0 + 10 + (int) (Noise.rnd(k, v, seed + 5) * (x1 - x0 - 20)), cy = ground - h[cx] + 3;
            int len = 8 + (int) (Noise.rnd(k, v, seed + 6) * 18);
            for (int s = 0; s < len; s++) {
                cx += Noise.rnd(k, s, seed + 7) < .4 ? (Noise.rnd(k, s, seed + 8) < .5 ? 1 : -1) : 0;
                c.tint(cx, cy + s, Pal.STONE[0]); c.tint(cx + 1, cy + s, Pal.STONE[2]);
            }
        }
        for (int x = x0 + 2; x < x1 - 1; x++) {                         // hanging moss and ivy
            if (Noise.rnd(x, v, seed + 8) > .2) continue;
            int y0 = ground - h[x], len = 4 + (int) (Noise.rnd(x, v, seed + 9) * 16);
            for (int s = 0; s < len && y0 + s < ground - 3; s++)
                c.tint(x + (s / 5 % 2), y0 + s, s == len - 1 ? Pal.GRASS[1] : s % 4 == 0 ? Pal.GRASS[3] : Pal.GRASS[2]);
        }
        if (v == 1) arrowSlit(c, 44, ground - 36);
        if (v == 2) { bracket(c, 30, ground - 30); bracket(c, 66, ground - 26); }
        rubbleAt(c, x0, x1, ground, seed);
        c.selectiveOutline();
        return c;
    }
    private static void arrowSlit(Canvas c, int x, int y) {
        for (int s = 0; s < 12; s++) { c.tint(x, y + s, Pal.INK); c.tint(x + 1, y + s, Pal.INK); c.tint(x + 2, y + s, s < 2 ? Pal.INK : Pal.STONE[0]); }
        for (int dx = -1; dx <= 3; dx++) { c.tint(x + dx, y + 12, Pal.STONE[5]); c.tint(x + dx, y - 1, Pal.STONE[2]); }
    }
    private static void bracket(Canvas c, int x, int y) {
        Canvas b = Decals.ascii(
                ".EE.",
                "EWWQ",
                ".WQ.",
                ".WQ.",
                "EWWQ",
                ".QQ.");
        for (int yy = 0; yy < b.h; yy++) for (int xx = 0; xx < b.w; xx++) if (b.opaque(xx, yy)) c.tint(x + xx, y + yy, b.get(xx, yy));
        c.tint(x + 1, y + 6, Pal.RUST[0]); c.tint(x + 2, y + 7, Pal.RUST[0]);          // rust streak
    }
    private static void rubbleAt(Canvas c, int x0, int x1, int ground, int seed) {
        Decals.Decal[] bits = {Decals.RUBBLE, Decals.ROCK_S, Decals.CHIP, Decals.ROCK_M, Decals.CHIP};
        for (int k = 0; k < 6; k++) {
            Canvas a = bits[k % bits.length].art();
            int x = x0 - 4 + (int) (Noise.rnd(k, seed, 9) * (x1 - x0)), y = ground + 3 - a.h + (int) (Noise.rnd(k, seed, 10) * 3);
            c.blit(a, x, y);
        }
    }

    // ------------------------------------------------------------------ log palisade
    static Canvas palisade(int v) {
        Canvas c = new Canvas(STAKE_W, STAKE_H);
        int ground = STAKE_GROUND, x0 = 8, seed = 600 + v * 13, logs = 10, logW = 8;
        for (int i = 0; i < logs; i++) {
            double r = Noise.rnd(i, v, seed);
            boolean missing = r < (v == 2 ? .2 : .06) && i > 0 && i < logs - 1;
            if (missing) continue;
            boolean broken = r > (v == 1 ? .7 : .86);
            int height = broken ? 22 + (int) (Noise.rnd(i, v, seed + 1) * 22) : 80 + (int) (Noise.rnd(i, v, seed + 2) * 14);
            log(c, x0 + i * logW, ground - (i % 2), height, broken, seed + i);
        }
        for (int rail : new int[]{18, 56}) {                     // binding rails and rope lashings
            int y = ground - rail;
            for (int x = x0 - 1; x < x0 + logs * logW + 1; x++) {
                if (!c.opaque(x, y) && !c.opaque(x, y - 2)) continue;
                c.set(x, y - 2, Pal.WOOD[5]); c.set(x, y - 1, Pal.WOOD[4]); c.set(x, y, Pal.WOOD[3]); c.set(x, y + 1, Pal.WOOD[2]); c.set(x, y + 2, Pal.WOOD[0]);
                if (Math.floorMod(x - x0, 16) == 5) for (int s = -3; s <= 3; s++) c.set(x + (s & 1), y + s, s < 0 ? Pal.DRY[3] : Pal.DRY[2]);
            }
        }
        if (v != 1) for (int k = 0; k < 4; k++) {                   // spent arrows in the timber
            int x = x0 + 8 + (int) (Noise.rnd(k, v, seed + 40) * 64), y = ground - 26 - (int) (Noise.rnd(k, v, seed + 41) * 40);
            if (!c.opaque(x, y)) continue;
            Canvas arrow = Decals.ascii("O.O..", ".OO..", "..xc.", "...xc", "....x");
            c.blit(arrow, x - 4, y - 4);
        }
        if (v == 2) c.blit(Decals.PLANK.art(), 52, ground - 2);
        c.blit(Decals.TUFT_C.art(), 0, ground - Decals.TUFT_C.art().h + 3);
        c.blit(Decals.TUFT_B.art(), 80, ground - Decals.TUFT_B.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    /** One sharpened stake, 8 px wide, lit from the left; bark streaks and knots break up the shading. */
    private static void log(Canvas c, int x, int base, int height, boolean broken, int seed) {
        int[] shade = {Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[5], Pal.WOOD[4], Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[2], Pal.WOOD[1]};
        int top = base - height;
        for (int y = top; y <= base; y++) {
            int fromTop = y - top;
            int halfWidth = broken ? 4 : Math.min(4, (fromTop + 1) * 4 / 9);
            for (int col = 0; col < 8; col++) {
                int dx = col - 4;
                if (dx < -halfWidth || dx >= Math.max(1, halfWidth)) continue;
                int colour = shade[col];
                if (Noise.rnd(x + col, y / 2, seed) < .07) colour = Pal.shift(colour, -1);            // bark furrows
                if (col == 2 && Noise.rnd(x, y / 4, seed + 1) < .25) colour = Pal.WOOD[4];
                if (base - y < 7 && Noise.rnd(x + col, y, seed + 2) < .3) colour = Pal.shift(colour, -1);  // wet foot
                c.set(x + col, y, colour);
            }
        }
        int knot = top + 14 + (int) (Noise.rnd(x, 0, seed + 3) * Math.max(1, height - 30));
        if (!broken || height > 30) { c.set(x + 4, knot, Pal.WOOD[1]); c.set(x + 5, knot, Pal.WOOD[0]); c.set(x + 4, knot - 1, Pal.WOOD[2]); }
        if (broken) {                                              // splintered top
            for (int col = 0; col < 8; col++) {
                int jag = (int) (Noise.rnd(col, seed, 3) * 5);
                for (int s = 0; s < jag; s++) c.clear(x + col, top + s);
                c.set(x + col, top + jag, col < 4 ? Pal.WOOD[5] : Pal.WOOD[4]);
            }
        } else { c.set(x + 4, top, Pal.WOOD[5]); c.set(x + 3, top + 1, Pal.WOOD[5]); }
    }

    // ------------------------------------------------------------------ supplies
    /** Footprint 32x16 centred on x 28; barrels are knee high, crates a bit lower. */
    static Canvas supplies(int v) {
        Canvas c = new Canvas(SUPPLY_W, SUPPLY_H);
        int ground = SUPPLY_GROUND;
        switch (v) {
            case 0 -> { barrel(c, 8, ground - 5); barrel(c, 26, ground); }
            case 1 -> { crate(c, 7, ground - 3, 21, 16); crate(c, 28, ground, 21, 16); crate(c, 17, ground - 23, 21, 16); }
            case 2 -> { barrel(c, 6, ground - 4); crate(c, 27, ground, 22, 17); sack(c, 36, ground - 22); }
            default -> { barrel(c, 25, ground - 5); spilled(c, 2, ground); }
        }
        c.selectiveOutline();
        return c;
    }

    static void barrel(Canvas c, int x, int base) {
        int w = 21, h = 28;
        int[] shade = {Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[4], Pal.WOOD[5], Pal.WOOD[5], Pal.WOOD[4], Pal.WOOD[4], Pal.WOOD[4],
                Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[2], Pal.WOOD[2], Pal.WOOD[2], Pal.WOOD[2],
                Pal.WOOD[2], Pal.WOOD[1], Pal.WOOD[1], Pal.WOOD[1], Pal.WOOD[0]};
        for (int y = base - h; y <= base; y++) {
            int fromBottom = base - y;
            int inset = fromBottom < 4 || fromBottom > h - 4 ? 1 : 0;
            for (int col = inset; col < w - inset; col++) {
                int colour = shade[col];
                if (col % 5 == 3) colour = Pal.shift(colour, -1);
                boolean hoop = fromBottom == 5 || fromBottom == 6 || fromBottom == h - 6 || fromBottom == h - 5;
                if (hoop) colour = col < 6 ? Pal.IRON[4] : col < 13 ? Pal.IRON[3] : col < 18 ? Pal.IRON[2] : Pal.IRON[1];
                if ((fromBottom == 6 || fromBottom == h - 5) && col == 3) colour = Pal.IRON[5];
                if (hoop && fromBottom == 5 && col > 14) colour = Pal.RUST[2];
                c.set(x + col, y, colour);
            }
        }
        int ly = base - h - 4;                                 // lid seen from above
        for (int r = 0; r < 6; r++) for (int col = 1; col < w - 1; col++) {
            double e = Math.pow((col - (w - 1) / 2.0) / (w / 2.0), 2) + Math.pow((r - 2.5) / 3.0, 2);
            if (e > 1) continue;
            int colour = r == 0 ? Pal.WOOD[5] : r >= 4 ? Pal.WOOD[2] : col % 6 == 0 ? Pal.WOOD[2] : Pal.WOOD[4];
            c.set(x + col, ly + r, colour);
        }
        c.set(x + 9, ly + 2, Pal.IRON[3]); c.set(x + 10, ly + 2, Pal.IRON[2]);
    }

    static void crate(Canvas c, int x, int base, int w, int h) {
        int topDepth = 7;
        for (int y = base - h; y <= base; y++) for (int col = 0; col < w; col++) {
            int fromBottom = base - y;
            boolean rim = fromBottom <= 1 || fromBottom >= h - 1 || col <= 1 || col >= w - 2;
            int colour = rim ? (col <= 1 || fromBottom >= h - 1 ? Pal.WOOD[4] : Pal.WOOD[1]) : fromBottom % 5 == 0 ? Pal.WOOD[1] : Pal.WOOD[3];
            int along = (int) Math.round((double) (col - 1) / (w - 3) * (h - 2)) + 1;
            if (!rim && Math.abs(fromBottom - along) <= 1) colour = fromBottom - along == 1 ? Pal.WOOD[5] : Pal.WOOD[4];
            if (!rim && fromBottom - along == -2) colour = Pal.WOOD[1];
            c.set(x + col, y, colour);
        }
        for (int y = base - h - topDepth; y < base - h; y++) for (int col = 0; col < w; col++) {
            int r = y - (base - h - topDepth);
            int colour = r == 0 ? Pal.WOOD[5] : r == topDepth - 1 ? Pal.WOOD[3] : col % 7 == 0 ? Pal.WOOD[2] : Pal.WOOD[4];
            c.set(x + col, y, colour);
        }
        for (int[] n : new int[][]{{2, h - 2}, {w - 3, h - 2}, {2, 2}, {w - 3, 2}}) c.set(x + n[0], base - n[1], Pal.IRON[4]);
    }

    private static void sack(Canvas c, int x, int base) {
        Canvas s = Decals.ascii(
                "....TT.....",
                "...lllT....",
                "..lHHHGl...",
                ".lHHHHGGl..",
                "lHHHHGGGGl.",
                "lHHGGGGFFFl",
                "lGGGGFFFFFD",
                ".DGFFFFFFD.",
                "..DDDDDDD..");
        c.blit(s, x - 5, base - s.h + 1);
    }

    private static void spilled(Canvas c, int x, int base) {
        Canvas s = Decals.ascii(
                ".....zxxxcxxxz........",
                "...zxcvvvcvvvcxmn.....",
                "..xcvbbvcvvbvvcmNn....",
                "..xcvvvcvvvcvvcmNn....",
                "..xcvvvcvvvcvvcmNn....",
                "...zxccxccxccxcmn..AS.",
                "....zxxzxxzxxzxASJIJSA",
                "........AASJJIIJISJSAA",
                ".........AASSJJSSAAA..",
                "............AAAA......");
        c.blit(s, x, base - s.h + 1);
    }
}
