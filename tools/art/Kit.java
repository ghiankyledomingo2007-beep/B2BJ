/**
 * Building blocks for 3/4-view structures. Convention: a solid stands on a footprint whose front
 * edge is the sprite row `ground`; its front face rises from there, its top face sits above that.
 * Moonlight comes from the upper left.
 */
final class Kit {
    private Kit() { }

    /** Coursed stone: front face of blocks plus a weathered top face of `depth` rows. */
    static void stoneBox(Canvas c, int x0, int ground, int w, int depth, int height, int seed, boolean mossy) {
        for (int x = x0; x < x0 + w; x++) {
            for (int y = ground - height + 1; y <= ground; y++) {
                int fromGround = ground - y, course = fromGround / 9, inCourse = fromGround % 9;
                int shiftX = x - x0 + (course % 2) * 7;
                int joint = Math.floorMod(shiftX, 14);
                int blockId = shiftX / 14 * 31 + course;
                int tone = Noise.rnd(blockId, seed, 1) < .3 ? 3 : 4;
                int colour = inCourse == 8 || joint == 0 ? Pal.STONE[1] : inCourse == 7 ? Pal.STONE[tone + 1]
                        : inCourse <= 1 ? Pal.STONE[tone - 1] : joint == 1 ? Pal.STONE[tone + 1] : joint == 13 ? Pal.STONE[tone - 1]
                        : Noise.value(x, y, 3, seed + 9) > .74 ? Pal.STONE[tone + 1] : Pal.STONE[tone];
                if (x == x0 && colour != Pal.STONE[1]) colour = Pal.shift(colour, 1);
                if (x == x0 + w - 1 && colour != Pal.STONE[1]) colour = Pal.shift(colour, -1);
                c.set(x, y, colour);
            }
            for (int r = 0; r < depth; r++) {
                int y = ground - height - depth + 1 + r;
                int colour = r == 0 ? Pal.STONE[6] : r == depth - 1 ? Pal.STONE[4] : Pal.STONE[5];
                if (mossy && r > 0 && Noise.value(x, y, 4, seed + 2) > .6) colour = Pal.GRASS[Noise.value(x, y, 2, seed + 3) > .5 ? 3 : 2];
                c.set(x, y, colour);
            }
        }
    }

    /** Vertical timber planks with a frame beam top and bottom. */
    static void plankWall(Canvas c, int x0, int top, int w, int bottom, int seed) {
        for (int x = x0; x < x0 + w; x++) {
            int plank = (x - x0) / 6, inPlank = (x - x0) % 6;
            int tone = Noise.rnd(plank, seed, 5) < .35 ? 2 : 3;
            for (int y = top; y <= bottom; y++) {
                int colour = inPlank == 0 ? Pal.WOOD[1] : inPlank == 1 ? Pal.WOOD[tone + 1] : inPlank == 5 ? Pal.WOOD[tone - 1] : Pal.WOOD[tone];
                if (Noise.rnd(x, y / 3, seed + 6) < .05) colour = Pal.shift(colour, -1);            // grain
                if (bottom - y < 10 && Noise.rnd(x, y, seed + 7) < (10 - (bottom - y)) / 25.0) colour = Pal.shift(colour, -1); // rot at the foot
                if (y <= top + 2) colour = y == top ? Pal.WOOD[4] : Pal.WOOD[2];
                if (y >= bottom - 2) colour = y == bottom - 2 ? Pal.WOOD[3] : Pal.WOOD[1];
                c.set(x, y, colour);
            }
        }
    }

    /**
     * Front roof slope of overlapping wooden shingles, eave at the bottom, ridge at the top. Each row
     * casts a shadow on the one below, seams show only where a shingle is exposed, and the slope
     * darkens toward the eave where the rain runs off.
     */
    static void shingleRoof(Canvas c, int x0, int w, int ridgeY, int eaveY, int seed) {
        int rowH = 6;
        for (int y = ridgeY; y <= eaveY; y++) {
            int row = (y - ridgeY) / rowH, inRow = (y - ridgeY) % rowH;
            double down = (y - ridgeY) / (double) Math.max(1, eaveY - ridgeY);
            int offset = (int) (Noise.rnd(row, 0, seed + 3) * 9);
            int shingle = 0, start = 0, next = 7 + (int) (Noise.rnd(row, 0, seed) * 5);
            for (int x = x0; x < x0 + w; x++) {
                int local = x - x0 + offset;
                while (local >= next) { shingle++; start = next; next += 7 + (int) (Noise.rnd(row, shingle, seed) * 5); }
                int tone = Noise.rnd(shingle, row, seed + 1) < .3 ? 2 : 3;
                if (Noise.rnd(shingle, row, seed + 2) < .07) tone = 1;
                if (down > .7 && tone > 2) tone--;
                int colour = inRow == 0 ? Pal.WOOD[0] : inRow == 1 ? Pal.WOOD[tone + 1] : inRow == rowH - 1 ? Pal.WOOD[tone - 1] : Pal.WOOD[tone];
                if (local == start && inRow >= 2) colour = Pal.WOOD[0];                       // seam where exposed
                if (local == start + 1 && inRow >= 2) colour = Pal.WOOD[Math.min(5, tone + 1)];
                double moss = Noise.value(x, y, 8, seed + 4);
                if (moss > .63 && inRow > 0) colour = inRow == 1 ? Pal.GRASS[moss > .73 ? 4 : 3] : Pal.GRASS[moss > .72 ? 2 : 1];
                if (y == ridgeY) colour = Pal.WOOD[5];
                if (y == ridgeY + 1) colour = Pal.WOOD[3];
                c.set(x, y, colour);
            }
        }
        for (int x = x0 - 1; x <= x0 + w; x++) { c.set(x, eaveY + 1, Pal.WOOD[1]); c.set(x, eaveY + 2, Pal.WOOD[0]); }
    }

    /** Upright cylinder lit from the left: tower, post or drum. */
    static void cylinder(Canvas c, int cx, int ground, int radius, int height, int[] ramp, int lightIdx, int seed) {
        for (int x = cx - radius; x < cx + radius; x++) {
            double u = (x + .5 - cx) / radius;                 // -1 left .. 1 right
            int idx = lightIdx + (u < -.55 ? 0 : u < -.1 ? 1 : u < .45 ? 0 : u < .8 ? -1 : -2);
            idx = Math.max(0, Math.min(ramp.length - 1, idx));
            int bulge = (int) Math.round(Math.sqrt(Math.max(0, 1 - u * u)) * 2);
            for (int y = ground - height + 1; y <= ground + bulge; y++) c.set(x, y, ramp[idx]);
        }
    }

    /** Filled polygon in one colour. */
    static void poly(Canvas c, int colour, int... xy) {
        int n = xy.length / 2;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) { xs[i] = xy[i * 2]; ys[i] = xy[i * 2 + 1]; }
        c.poly(xs, ys, colour);
    }

    /** A rope or thin line, one pixel. */
    static void rope(Canvas c, int x0, int y0, int x1, int y1, int colour) { c.line(x0, y0, x1, y1, colour); }

    // ------------------------------------------------------------------ fire
    /**
     * One frame of a looping fire, `frames` frames per loop. Tongues sway and breathe out of phase;
     * the core burns white-yellow, the rim red; embers rise and wink out above.
     */
    static Canvas fire(int w, int h, int frame, int frames, int tongues, int seed) {
        Canvas c = new Canvas(w, h);
        double t = frame * Math.PI * 2 / frames;
        int base = h - 2;
        for (int k = 0; k < tongues; k++) {
            double bx = w / 2.0 + (k - (tongues - 1) / 2.0) * (w * .55 / Math.max(1, tongues - 1)) * (tongues > 1 ? 1 : 0);
            double phase = Noise.rnd(k, seed, 1) * 6.28, phase2 = Noise.rnd(k, seed, 2) * 6.28;
            double centre = 1 - Math.abs(k - (tongues - 1) / 2.0) / Math.max(1, tongues / 2.0);
            double height = (h - 6) * (.45 + .35 * centre) * (.82 + .18 * Math.sin(phase + t));
            double width = w * (.13 + .09 * centre);
            double sway = 1.6 * Math.sin(phase2 + t * (k % 2 == 0 ? 1 : 2));
            for (int y = 0; y < height; y++) {
                double v = y / height;
                double half = width * Math.pow(1 - v, .75) + .5;
                double xc = bx + sway * Math.pow(v, 1.4);
                for (int x = (int) (xc - half); x <= (int) (xc + half); x++) {
                    double heat = (1 - Math.abs(x + .5 - xc) / half) * (1 - .62 * v) * (.75 + .25 * centre);
                    int colour = heat > .62 ? Pal.FIRE[5] : heat > .44 ? Pal.FIRE[4] : heat > .27 ? Pal.FIRE[3] : heat > .1 ? Pal.FIRE[2] : Pal.FIRE[1];
                    int py = base - y;
                    int old = c.get(x, py);
                    if ((old >>> 24) == 0 || Pal.index(colour) > Pal.index(old)) c.set(x, py, colour);
                }
            }
        }
        for (int e = 0; e < 4; e++) {      // embers ride up through the loop
            double life = ((frame + e * frames / 4.0) % frames) / frames;
            int ex = (int) Math.round(w / 2.0 + (Noise.rnd(e, seed, 3) - .5) * w * .6 + Math.sin(life * 6 + e) * 1.5);
            int ey = (int) Math.round(base - (h * .45) - life * h * .5);
            if (life < .85) c.set(ex, ey, life < .4 ? Pal.FIRE[4] : Pal.FIRE[3]);
        }
        return c;
    }
}
