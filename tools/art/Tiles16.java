/**
 * Each area's ground materials as a 16x16 tileset at the game's pixel density (shown at 2x like every
 * other sprite); every B2BJ tileset is 16x16. The forest floor is baked from these same tiles, and
 * the sheets serve map editors (Tiled, LDtk, Godot).
 *
 * Layout, 16 columns of 16 px cells:
 *   rows 0-3   fills, 8 variants each, in the set's order (Outpost: grass, meadow, dirt, flagstone,
 *              cobble, water, mud, gravel; Forest: leaf litter, moss, grass, dirt, blackwater, dry
 *              riverbed, blight, mud).
 *              Variants 0-3 are plain, 4-7 carry one small detail; every variant of a material joins
 *              every other, so scatter them at random (mostly 0-3).
 *   rows 4-    16-tile Wang corner sets, three rows per material pair (see Set), one per variant.
 *              Column = corner mask: 1 top-left, 2 top-right, 4 bottom-left, 8 bottom-right; a set bit
 *              is the second material. Give the three variants the same Wang id and let the editor
 *              pick at random, so a long border never repeats the same wiggle.
 * The outer two rows and columns of a tile depend only on the corners they touch (identical in all
 * variants), and the edge noise repeats every 16 px, so any two tiles that agree on their shared
 * corners join without a seam; only a tile's middle wanders differently per variant.
 * Higher ground (moss, meadow > grass, litter > stone > dirt, blight > mud, riverbed > water) gets a lit
 * top lip, a shaded front lip and drops blade tips or a shadow on whatever lies below it.
 */
final class Tiles16 {
    private Tiles16() { }
    static final int T = 16;
    /** How far past its own edge a pixel() ever looks (the water shadow reads two rows up); verifySeams proves that far. */
    static final int REACH = 2;
    enum Mat { GRASS, MEADOW, DIRT, FLAG, COBBLE, WATER, MUD, GRAVEL, LITTER, MOSS, BLACKWATER, RIVERBED, BLIGHT }

    /** One area's tileset: its eight fill materials and the pairs that get Wang corner sets. */
    enum Set {
        OUTPOST(new Mat[]{Mat.GRASS, Mat.MEADOW, Mat.DIRT, Mat.FLAG, Mat.COBBLE, Mat.WATER, Mat.MUD, Mat.GRAVEL},
                new Mat[][]{{Mat.GRASS, Mat.DIRT}, {Mat.GRASS, Mat.MEADOW}, {Mat.GRASS, Mat.WATER}, {Mat.GRASS, Mat.FLAG},
                        {Mat.GRASS, Mat.COBBLE}, {Mat.DIRT, Mat.FLAG}, {Mat.DIRT, Mat.WATER}, {Mat.GRASS, Mat.MUD}}),
        FOREST(new Mat[]{Mat.LITTER, Mat.MOSS, Mat.GRASS, Mat.DIRT, Mat.BLACKWATER, Mat.RIVERBED, Mat.BLIGHT, Mat.MUD},
                new Mat[][]{{Mat.LITTER, Mat.DIRT}, {Mat.LITTER, Mat.MOSS}, {Mat.LITTER, Mat.GRASS}, {Mat.LITTER, Mat.BLACKWATER},
                        {Mat.LITTER, Mat.RIVERBED}, {Mat.RIVERBED, Mat.BLACKWATER}, {Mat.LITTER, Mat.BLIGHT},
                        {Mat.GRASS, Mat.DIRT}, {Mat.DIRT, Mat.RIVERBED}, {Mat.LITTER, Mat.MUD}});
        final Mat[] fills;
        final Mat[][] pairs;
        Set(Mat[] fills, Mat[][] pairs) { this.fills = fills; this.pairs = pairs; }
        /** The Wang pair holding both materials, in sheet order, or null when the set has none. */
        Mat[] pair(Mat x, Mat y) {
            for (Mat[] p : pairs) if ((p[0] == x && p[1] == y) || (p[0] == y && p[1] == x)) return p;
            return null;
        }
    }

    static Canvas sheet(Set set) {
        Canvas sheet = new Canvas(16 * T, (4 + set.pairs.length * 3) * T);
        for (int m = 0; m < set.fills.length; m++) for (int v = 0; v < 8; v++) {
            int i = m * 8 + v;
            sheet.blit(tile(set.fills[m], set.fills[m], 0, v), i % 16 * T, i / 16 * T);
        }
        for (int p = 0; p < set.pairs.length; p++) for (int v = 0; v < 3; v++) for (int mask = 0; mask < 16; mask++)
            sheet.blit(tile(set.pairs[p][0], set.pairs[p][1], mask, v), mask * T, (4 + p * 3 + v) * T);
        return sheet;
    }

    /**
     * Proves the sheet is seamless: for every pair, every two masks that may sit side by side or
     * stacked (they agree on the shared corners) and every mix of variants, each tile's view of the
     * two pixels beyond its edge equals what the neighbour actually draws there. Throws on a seam.
     */
    static void verifySeams(Set set) {
        for (Mat[] pair : set.pairs) for (int ma = 0; ma < 16; ma++) for (int mb = 0; mb < 16; mb++)
            for (int va = 0; va < 3; va++) for (int vb = 0; vb < 3; vb++) {
                Mat a = pair[0], b = pair[1];
                // ma left of mb: ma's right corners (2, 8) are mb's left corners (1, 4)
                if (((ma >> 1) & 1) == (mb & 1) && ((ma >> 3) & 1) == ((mb >> 2) & 1))
                    for (int y = 0; y < T; y++) for (int d = 0; d < REACH; d++)
                        if (at(a, b, ma, T + d, y, va) != at(a, b, mb, d, y, vb) || at(a, b, mb, -1 - d, y, vb) != at(a, b, ma, T - 1 - d, y, va))
                            throw new IllegalStateException("horizontal seam " + a + "/" + b + " masks " + ma + "|" + mb);
                // ma above mb: ma's bottom corners (4, 8) are mb's top corners (1, 2)
                if (((ma >> 2) & 1) == (mb & 1) && ((ma >> 3) & 1) == ((mb >> 1) & 1))
                    for (int x = 0; x < T; x++) for (int d = 0; d < REACH; d++)
                        if (at(a, b, ma, x, T + d, va) != at(a, b, mb, x, d, vb) || at(a, b, mb, x, -1 - d, vb) != at(a, b, ma, x, T - 1 - d, va))
                            throw new IllegalStateException("vertical seam " + a + "/" + b + " masks " + ma + "/" + mb);
            }
    }

    static Canvas tile(Mat a, Mat b, int mask, int variant) {
        Canvas c = new Canvas(T, T);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) c.set(x, y, pixel(a, b, mask, x, y, variant));
        if (a == b && variant >= 4) Detail.stamp(c, a, variant);
        return c;
    }

    /** Material at (x, y); also valid two pixels outside the tile, where it equals the neighbour's pixel. */
    static Mat at(Mat a, Mat b, int mask, int x, int y, int variant) {
        if (a == b || mask == 0) return a;
        if (mask == 15) return b;
        double u = Math.max(0, Math.min(1, (x + .5 - 2) / (T - 4))), v = Math.max(0, Math.min(1, (y + .5 - 2) / (T - 4)));
        double f = ((mask & 1) != 0 ? (1 - u) * (1 - v) : 0) + ((mask & 2) != 0 ? u * (1 - v) : 0)
                + ((mask & 4) != 0 ? (1 - u) * v : 0) + ((mask & 8) != 0 ? u * v : 0);
        // Two octaves of 16 px periodic noise; it may wander more in the middle of a tile than near
        // an edge, where both neighbours see the same distance and so the same amount.
        double edge = Math.min(Math.min(x + .5, T - .5 - x), Math.min(y + .5, T - .5 - y));
        double amp = .5 + .4 * Math.max(0, Math.min(1, (edge - 1.5) / 4));
        // Within two pixels of an edge every variant uses the same noise; toward the middle its own.
        double fixed = Math.max(0, Math.min(1, (4.5 - edge) / 3));
        double n = noise(x, y, 0) * fixed + noise(x, y, variant % 3) * (1 - fixed);
        return f + n * amp > .5 ? b : a;
    }
    private static double noise(int x, int y, int variant) {
        int seed = 77 + variant * 1000;
        return (Noise.periodic(x, y, 4, 4, seed) - .5) * .7 + (Noise.periodic(x, y, 2, 8, seed + 1) - .5) * .3;
    }

    static int height(Mat m) {
        return switch (m) {
            case WATER, BLACKWATER -> 0; case MUD, RIVERBED -> 1; case DIRT, GRAVEL, BLIGHT -> 2; case FLAG, COBBLE -> 3;
            case GRASS, LITTER -> 4; case MEADOW, MOSS -> 5;
        };
    }
    static boolean wet(Mat m) { return m == Mat.WATER || m == Mat.BLACKWATER; }

    static int pixel(Mat a, Mat b, int mask, int x, int y, int variant) {
        Mat m = at(a, b, mask, x, y, variant), up = at(a, b, mask, x, y - 1, variant), down = at(a, b, mask, x, y + 1, variant);
        int base = colour(m, x, y, variant);
        if (wet(m)) {
            boolean black = m == Mat.BLACKWATER;
            if (!wet(up)) return black ? Pal.INK : Pal.WATER[0];                  // the bank's shadow
            if (!wet(at(a, b, mask, x, y - REACH, variant))) return black ? Pal.WATER[0] : Pal.WATER[1];
            if (!wet(down)) return black ? Pal.WATER[2] : Pal.WATER[3];          // lit shallows at the near shore
            return base;
        }
        if (height(up) < height(m)) return lip(m, true);
        if (height(down) < height(m)) return lip(m, false);
        if (height(up) > height(m)) {                                             // overhang from the ground above
            boolean grassy = up == Mat.GRASS || up == Mat.MEADOW || up == Mat.MOSS;
            if (grassy && Noise.rnd(Math.floorMod(x, T), 0, 311) < .5) return Pal.GRASS[up == Mat.GRASS ? 1 : 2];
            if (up == Mat.LITTER && Noise.rnd(Math.floorMod(x, T), 0, 312) < .35) return Pal.RUST[1];    // a curled leaf over the edge
            return Pal.shift(base, -1);
        }
        return base;
    }

    private static int lip(Mat m, boolean top) {
        return switch (m) {
            case GRASS -> top ? Pal.GRASS[2] : Pal.GRASS[0];
            case MEADOW -> top ? Pal.GRASS[3] : Pal.GRASS[1];
            case FLAG, COBBLE -> top ? Pal.STONE[5] : Pal.STONE[1];
            case DIRT, GRAVEL -> top ? Pal.MUD[3] : Pal.MUD[1];
            case MUD -> top ? Pal.MUD[3] : Pal.MUD[0];
            case WATER, BLACKWATER -> Pal.WATER[2];
            case LITTER -> top ? Pal.EARTH[4] : Pal.EARTH[1];
            case MOSS -> top ? Pal.GRASS[4] : Pal.GRASS[1];
            case RIVERBED -> top ? Pal.CANVAS[3] : Pal.CANVAS[0];
            case BLIGHT -> top ? Pal.VIOLET[3] : Pal.VIOLET[0];
        };
    }

    /** The material's own texture; repeats every 16 px and varies only in what cannot meet an edge. */
    static int colour(Mat m, int x, int y, int variant) {
        int px = Math.floorMod(x, T), py = Math.floorMod(y, T), seed = (variant & 3) * 131;
        return switch (m) {
            case GRASS, MEADOW -> grass(px, py, m == Mat.MEADOW ? 2 : 1, seed);
            case DIRT -> {
                if (pebble(px, py, seed)) yield Noise.rnd(px, py, 15) < .35 ? Pal.STONE[4] : Pal.STONE[3];
                if (pebble(px, Math.floorMod(py - 1, T), seed)) yield Pal.MUD[1];
                if (clod(px, py, seed)) yield Pal.MUD[3];                               // a lit clod and its shadow
                if (clod(px, Math.floorMod(py - 1, T), seed)) yield Pal.MUD[1];
                if (Noise.rnd(px, py, 332 + seed) < .04) yield Pal.MUD[1];
                yield Noise.rnd(px, py, 333 + seed) < .03 ? Pal.MUD[3] : Pal.MUD[2];
            }
            case GRAVEL -> {
                double r = Noise.rnd(px, py, 54 + seed);
                if (r < .14) yield Noise.rnd(px, py, 55) < .3 ? Pal.STONE[4] : Pal.STONE[3];
                if (Noise.rnd(px, Math.floorMod(py - 1, T), 54 + seed) < .14) yield Pal.MUD[1];
                yield Noise.rnd(px, py, 56) < .12 ? Pal.STONE[2] : Pal.MUD[2];
            }
            case MUD -> {                                                       // short wet furrows with a lit lower lip
                if (furrow(px, py, seed)) yield Pal.MUD[1];
                if (furrow(px, Math.floorMod(py - 1, T), seed)) yield Pal.MUD[3];
                yield Noise.rnd(px, py, 342 + seed) < .03 ? Pal.WATER[2] : Pal.MUD[2];
            }
            case WATER -> {
                int cx = px / 4, cy = py / 4, rx = cx * 4 + (int) (Noise.rnd(cx, cy, 81 + seed) * 2), ry = cy * 4 + 1 + (int) (Noise.rnd(cx, cy, 82 + seed) * 2);
                if (Noise.rnd(cx, cy, 83 + seed) < .38 && py == ry && px >= rx && px < rx + 3) yield Pal.WATER[3];
                yield Noise.rnd(px, py, 84 + seed) < .006 ? Pal.WATER[4] : Pal.WATER[2];
            }
            case FLAG -> flag(px, py, variant);
            case COBBLE -> cobble(px, py, variant);
            case LITTER -> litter(px, py, seed);
            case MOSS -> {                                                      // soft cushions: mottled, a few lit tips
                double n = Noise.periodic(px, py, 4, 4, 401);
                if (Noise.rnd(px, py, 402 + seed) < .05) yield Pal.GRASS[4];
                if (n < .3 && Noise.rnd(px, py, 403) < .55) yield Pal.GRASS[2];
                yield n > .72 && Noise.rnd(px, py, 404) < .5 ? Pal.GRASS[4] : Pal.GRASS[3];
            }
            case BLACKWATER -> {
                int cx = px / 4, cy = py / 4, rx = cx * 4 + (int) (Noise.rnd(cx, cy, 85 + seed) * 2), ry = cy * 4 + 1 + (int) (Noise.rnd(cx, cy, 86 + seed) * 2);
                if (Noise.rnd(cx, cy, 87 + seed) < .3 && py == ry && px >= rx && px < rx + 2) yield Pal.WATER[1];
                yield Noise.rnd(px, py, 88 + seed) < .006 ? Pal.GOLD[1] : Pal.WATER[0];      // a rare drowned glint of Ichor
            }
            case RIVERBED -> {                                                  // dry sand, softly mottled, pebbles with shadows
                if (Noise.rnd(px, py, 61 + seed) < .04) yield Noise.rnd(px, py, 62) < .4 ? Pal.STONE[5] : Pal.STONE[4];
                if (Noise.rnd(px, Math.floorMod(py - 1, T), 61 + seed) < .04) yield Pal.CANVAS[0];
                if (Noise.rnd(px, py, 63 + seed) < .045) yield Pal.CANVAS[3];
                double n = Noise.periodic(px, py, 4, 4, 64);
                yield n < .3 && Noise.bayer(px, py) < .5 ? Pal.CANVAS[1] : Pal.CANVAS[2];
            }
            case BLIGHT -> {                                                    // violet rot, fine-grained; its glowing roots are map decals
                if (Noise.rnd(px, py, 71 + seed) < .004) yield Pal.GOLD[3];
                if (Noise.rnd(px, py, 72 + seed) < .045) yield Pal.VIOLET[2];
                double n = Noise.periodic(px, py, 2, 8, 73);
                yield n > .74 && Noise.bayer(px, py) < .6 ? Pal.VIOLET[0] : Pal.VIOLET[1];
            }
        };
    }

    /** Fallen leaves on dark earth: each leaf a lit tip over its body, a dark speck of shadow below. */
    private static int litter(int px, int py, int seed) {
        int cx = px / 4, cy = py / 4;
        double kind = Noise.rnd(cx, cy, 451 + seed);
        int lx = cx * 4 + (int) (Noise.rnd(cx, cy, 452 + seed) * 3), ly = cy * 4 + (int) (Noise.rnd(cx, cy, 453 + seed) * 2);
        if (kind < .42) {
            int body = kind < .14 ? Pal.RUST[2] : kind < .26 ? Pal.DRY[2] : kind < .34 ? Pal.RUST[1] : Pal.EARTH[4];
            if (py == ly && (px == lx || px == lx + 1)) return px == lx ? Pal.shift(body, 1) : body;
            if (py == ly + 1 && px == lx + 1) return Pal.EARTH[0];
        } else if (kind > .9 && py == ly + 1 && px >= lx && px < lx + 3) return Pal.WOOD[3];   // a twig
        if (Noise.rnd(px, py, 454 + seed) < .035) return Pal.EARTH[3];
        return Noise.periodic(px, py, 4, 4, 455) > .7 ? Pal.EARTH[1] : Pal.EARTH[2];
    }
    private static boolean pebble(int px, int py, int seed) { return Noise.rnd(px, py, 331 + seed) < .02; }
    private static boolean furrow(int px, int py, int seed) { return py % 3 == 1 && Noise.rnd(px / 2, py, 341 + seed) < .17; }
    private static boolean clod(int px, int py, int seed) { return Noise.rnd(px / 2, py, 334 + seed) < .05 && px % 2 == 0; }

    /** Tuft marks on a 4x4 grid: a lit tip over a mid stem, a small chevron, or a dark blade. */
    private static int grass(int px, int py, int base, int seed) {
        int cx = px / 4, cy = py / 4;
        double kind = Noise.rnd(cx, cy, 303 + seed);
        int bx = cx * 4 + (int) (Noise.rnd(cx, cy, 301 + seed) * 3), by = cy * 4 + (int) (Noise.rnd(cx, cy, 302 + seed) * 2);
        if (kind < .4) {
            if (px == bx && py == by) return Pal.GRASS[base + 2 - (kind < .26 ? 0 : 1)];
            if (px == bx && py == by + 1) return Pal.GRASS[base + 1];
        } else if (kind < .55) {
            if (py == by + 1 && (px == bx - 1 || px == bx + 1)) return Pal.GRASS[base + 1];
            if (py == by && px == bx) return Pal.GRASS[base + 1];
        } else if (kind > .88 && px == bx && py == by + 1) return Pal.GRASS[base - 1];
        return Pal.GRASS[base];
    }

    /**
     * Each tile is paving cut on an 8 px half grid: one slab, two, or four, with the joint on its top
     * row and left column so neighbours complete the grid. Variants change the cut and the stone.
     */
    private static int flag(int px, int py, int variant) {
        if (px == 0 || py == 0) return Pal.STONE[1];
        int v = variant & 7;
        boolean splitH = v == 2 || v == 4, splitV = v == 3 || v == 4;
        if ((splitH && py == 8) || (splitV && px == 8)) return Pal.STONE[1];
        int sx = splitV && px > 8 ? 1 : 0, sy = splitH && py > 8 ? 1 : 0;
        int x0 = sx == 1 ? 9 : 1, y0 = sy == 1 ? 9 : 1, x1 = splitV && sx == 0 ? 7 : 15, y1 = splitH && sy == 0 ? 7 : 15;
        int tone = Noise.rnd(v * 4 + sy * 2 + sx, 0, 51) < .55 ? 3 : 4;
        if (v == 1) tone = 4;
        if (v == 6 && (px == 1 || py == 1) && Noise.rnd(px, py, 63) < .55) return Pal.GRASS[Noise.rnd(px, py, 64) < .5 ? 1 : 2];
        if (v == 5 && crack(px, py)) return Pal.STONE[1];
        if (v == 5 && crack(px, py - 1)) return Pal.STONE[tone + 1];
        if (v == 7 && px + py > 21) {                                             // a broken corner, dirt beneath
            if (px + py == 22) return Pal.STONE[1];
            return Noise.rnd(px, py, 65) < .2 ? Pal.MUD[1] : Pal.MUD[2];
        }
        if (px == x0 || py == y0) return Pal.STONE[tone + 1];
        if (px == x1 || py == y1) return Pal.STONE[tone - 1];
        return Noise.rnd(px, py, 66 + v) < .04 ? Pal.STONE[tone - 1] : Pal.STONE[tone];
    }
    private static boolean crack(int px, int py) {
        int y = 4 + (px * 5) / 9 + (px > 9 ? -2 : 0);
        return px >= 3 && px <= 13 && py == y;
    }

    /**
     * Setts laid in three staggered courses (5, 5 and 6 px) that repeat every 16 px. Each course has
     * one stone crossing the tile edge; those keep fixed tones so any variant meets any other, and the
     * stones wholly inside the tile change tone per variant. Corners are cut for a rounded stone.
     */
    private static final int[][] COURSES = {{0, 5, 2, 7, 13}, {5, 5, 4, 10, 15}, {10, 6, 1, 6, 12}};
    private static int cobble(int px, int py, int variant) {
        int[] course = COURSES[py < 5 ? 0 : py < 10 ? 1 : 2];
        int top = course[0], height = course[1], ly = py - top;
        int stone = 2, start = course[4] - 16;                                   // the stone that wraps the edge
        for (int k = 2; k < 5; k++) if (px >= course[k]) { stone = k; start = course[k]; }
        if (px < course[2]) start = course[4] - 16;
        int end = stone == 4 || px < course[2] ? course[2] + 16 : course[stone + 1];
        if (px < course[2]) end = course[2];
        int lx = px - start, width = end - start;
        boolean wraps = px < course[2] || stone == 4;
        boolean gap = lx == 0 || ly == 0;
        boolean corner = (lx == 1 || lx == width - 1) && (ly == 1 || ly == height - 1);
        if (gap || corner) return variant >= 6 && Noise.rnd(px, py, 47) < .35 ? Pal.GRASS[1] : Pal.MUD[1];
        int id = (py < 5 ? 0 : py < 10 ? 3 : 6) + (wraps ? 0 : stone - 1);
        int tone = Noise.rnd(id, wraps ? 0 : variant, 43) < .6 ? 3 : 4;
        if (ly == 1 || lx == 1) return Pal.STONE[tone + 1];
        if (ly == height - 1 || lx == width - 1) return Pal.STONE[tone - 1];
        return Noise.rnd(px, py, 44 + variant) < .05 ? Pal.STONE[tone - 1] : Pal.STONE[tone];
    }

    /** Small details for fill variants 4-7, kept two pixels clear of every edge. */
    static final class Detail {
        private Detail() { }
        static void stamp(Canvas c, Mat m, int variant) {
            int ox = 3 + (int) (Noise.rnd(variant, m.ordinal(), 91) * 6), oy = 3 + (int) (Noise.rnd(variant, m.ordinal(), 92) * 5);
            String[] art = switch (m) {
                case GRASS -> switch (variant) {
                    case 4 -> new String[]{".h.h.", ".mhm.", "..m..", ".s.s."};
                    case 5 -> new String[]{".w...", "wyw.w", ".w.wy", "...w."};
                    case 6 -> new String[]{"..p..", ".pPp.", "..s.."};
                    default -> new String[]{".lo..", "loOl.", ".ll.."};
                };
                case MEADOW -> switch (variant) {
                    case 4 -> new String[]{"h.h.h", "mhmhm", ".m.m."};
                    case 5 -> new String[]{".v..", "vyv.", ".v.v", "...y"};
                    case 6 -> new String[]{".w.", "wyw", ".s."};
                    default -> new String[]{"h..h", "m.hm", "mmm."};
                };
                case DIRT -> switch (variant) {
                    case 4 -> new String[]{".oO.", "oOOo", ".dd."};
                    case 5 -> new String[]{"r....", ".rr..", "...rr", "....."};
                    case 6 -> new String[]{"d....", ".d.d.", "..d.d"};
                    default -> new String[]{".bb.", "b..b"};
                };
                case FLAG -> new String[0];
                case COBBLE -> variant == 7 ? new String[]{".s.", "sms"} : new String[0];
                case WATER -> switch (variant) {
                    case 4 -> new String[]{".mm.", "mlLm", ".mm."};
                    case 5 -> new String[]{"..k..", ".k.k."};
                    case 6 -> new String[]{"k.k", ".K."};
                    default -> new String[]{".ll", "lLl"};
                };
                case MUD -> switch (variant) {
                    case 4 -> new String[]{".kk.", "kKKk", ".kk."};
                    case 5 -> new String[]{"d.d", ".d.", "d.d"};
                    default -> new String[]{".oO", "oOd"};
                };
                case GRAVEL -> new String[]{".oO.", "oOOo", ".dd."};
                case LITTER -> switch (variant) {
                    case 4 -> new String[]{"f.f.f", ".fFf.", "..f..", "..s.."};     // a fern
                    case 5 -> new String[]{".cC.", "cCCc", ".ww."};                  // a glowing cap
                    case 6 -> new String[]{"rr...", "..rr.", "....r"};               // a fallen twig
                    default -> new String[]{".a.", "aAa", ".d."};                   // an acorn cup
                };
                case MOSS -> switch (variant) {
                    case 4 -> new String[]{".w.", "wyw", ".w."};
                    case 5 -> new String[]{".oO.", "oOOo", ".dd."};
                    case 6 -> new String[]{"h.h", ".m."};
                    default -> new String[]{".w..w", "w.w.."};
                };
                case BLACKWATER -> switch (variant) {
                    case 4 -> new String[]{".mm.", "mlLm", ".mm."};
                    case 5 -> new String[]{"..g..", ".g.g."};
                    case 6 -> new String[]{"k.k", ".K."};
                    default -> new String[]{".rr", "rr."};
                };
                case RIVERBED -> switch (variant) {
                    case 4 -> new String[]{".oO..", "oOOo.", ".ddoO", "...dd"};
                    case 5 -> new String[]{".b.", "bbb", ".b."};
                    case 6 -> new String[]{"t...", ".tt.", "...t"};
                    default -> new String[]{".oO.", "oOOo", ".dd."};
                };
                case BLIGHT -> switch (variant) {
                    case 4 -> new String[]{".v.", "vVv", ".V.", ".d."};              // a violet shard
                    case 5 -> new String[]{"g.g", ".G.", "g.g"};
                    case 6 -> new String[]{".bb.", "b..b", ".bb."};
                    default -> new String[]{"..g", ".G.", "g.."};
                };
            };
            for (int y = 0; y < art.length; y++) for (int x = 0; x < art[y].length(); x++) {
                int colour = ink(art[y].charAt(x));
                if (colour != 0) c.set(ox + x, oy + y, colour);
            }
        }
        private static int ink(char k) {
            return switch (k) {
                case 'h' -> Pal.GRASS[4]; case 'm' -> Pal.GRASS[3]; case 's' -> Pal.GRASS[0];
                case 'l' -> Pal.GRASS[3]; case 'L' -> Pal.GRASS[4];
                case 'w' -> Pal.BONE[3]; case 'y' -> Pal.GOLD[4]; case 'v' -> Pal.VIOLET[4];
                case 'p' -> Pal.CRIMSON[4]; case 'P' -> Pal.FIRE[4];
                case 'o' -> Pal.STONE[3]; case 'O' -> Pal.STONE[5]; case 'd' -> Pal.MUD[0];
                case 'r' -> Pal.WOOD[4]; case 'b' -> Pal.BONE[2];
                case 'k' -> Pal.WATER[4]; case 'K' -> Pal.WATER[5];
                case 'f' -> Pal.GRASS[3]; case 'F' -> Pal.GRASS[4];
                case 'c' -> Pal.CYAN[2]; case 'C' -> Pal.CYAN[4];
                case 'a' -> Pal.WOOD[3]; case 'A' -> Pal.WOOD[5];
                case 'g' -> Pal.GOLD[2]; case 'G' -> Pal.GOLD[4];
                case 'V' -> Pal.VIOLET[5]; case 't' -> Pal.CANVAS[0];
                default -> 0;
            };
        }
    }
}
