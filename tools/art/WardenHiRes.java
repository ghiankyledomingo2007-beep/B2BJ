/**
 * The Outpost Warden at the world's pixel size. The original atlas (64 px cells drawn at 4x) is kept untouched
 * in assets/source/characters/warden_motion_64.png; this doubles it to 128 px cells (drawn at 2x, so the same
 * size on screen) without inventing colours: every output pixel is a colour of the source cell.
 *  1. EPX/Scale2x per cell, deciding with "similar colour" instead of exact equality so the noisy rock still
 *     gets its silhouette and spike stair-steps rounded off.
 *  2. The near-black silhouette outline, now 2 px thick, is thinned back to 1 px (Briarheart's line weight):
 *     outline pixels that no longer touch the outside take the colour of the rock behind them.
 *  3. Lone highlights and dark cracks (pixels far brighter or darker than their neighbours) keep a 1 px line
 *     along their run instead of a 2x2 block: lit sparks keep the upper-left pixels, cracks the lower-right.
 * Same frame order, foot row 56 becomes 112. The fight's attack and death strips (guardian_*.png, one row of
 * 64 px cells, originals kept as assets/source/characters/guardian_*_64.png) get the same treatment.
 */
final class WardenHiRes {
    private WardenHiRes() { }
    static final int SRC = 64, CELL = 128, COLS = 8, ROWS = 7;
    static final String SOURCE = "assets/source/characters/warden_motion_64.png", OUT = "assets/characters/guardian/warden_motion.png";
    private static final int OUTLINE_LUM = 20, SPARK = 38, CRACK = 30, SIMILAR = 40;

    static Canvas build() { return build(SOURCE, COLS, ROWS); }

    /** The single-row attack and death strips drawn during the fight, same treatment, same frame count. */
    static final String[] STRIPS = {"charge", "sweep", "wave", "death"};
    static void strips() {
        for (String n : STRIPS) {
            String src = "assets/source/characters/guardian_" + n + "_64.png";
            build(src, Canvas.load(src).w / SRC, 1).save("assets/characters/guardian/guardian_" + n + ".png");
        }
    }

    static Canvas build(String source, int cols, int rows) {
        Canvas src = Canvas.load(source);
        if (src.w != SRC * cols || src.h != SRC * rows) throw new IllegalStateException(source + " is not " + cols + "x" + rows + " cells of 64");
        Canvas out = new Canvas(src.w * 2, src.h * 2);
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            Canvas cell = new Canvas(SRC, SRC);
            for (int y = 0; y < SRC; y++) for (int x = 0; x < SRC; x++) {
                int p = src.get(c * SRC + x, r * SRC + y);
                cell.set(x, y, (p >>> 24) == 0 ? 0 : p | 0xff000000);       // hard alpha, one transparent value
            }
            out.blit(refine(cell), c * CELL, r * CELL);
        }
        return out;
    }

    static Canvas refine(Canvas s) {
        Canvas b = epx(s);
        thinDetail(s, b);
        thinOutline(s, b);
        return b;
    }

    // ------------------------------------------------------------------ colour tests
    static int lum(int p) { return (((p >> 16) & 255) * 299 + ((p >> 8) & 255) * 587 + (p & 255) * 114) / 1000; }
    static boolean similar(int a, int b) {
        boolean ta = (a >>> 24) == 0, tb = (b >>> 24) == 0;
        if (ta || tb) return ta && tb;
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) + Math.abs((a & 255) - (b & 255)) < SIMILAR;
    }
    private static boolean outline(Canvas s, int x, int y) {
        if (!s.opaque(x, y) || lum(s.get(x, y)) > OUTLINE_LUM) return false;
        return !s.opaque(x + 1, y) || !s.opaque(x - 1, y) || !s.opaque(x, y + 1) || !s.opaque(x, y - 1);
    }

    // ------------------------------------------------------------------ 1. EPX with similarity
    static Canvas epx(Canvas s) {
        Canvas b = new Canvas(s.w * 2, s.h * 2);
        for (int y = 0; y < s.h; y++) for (int x = 0; x < s.w; x++) {
            int p = s.get(x, y), a = s.get(x, y - 1), r = s.get(x + 1, y), l = s.get(x - 1, y), d = s.get(x, y + 1);
            int tl = p, tr = p, bl = p, br = p;
            if (similar(l, a) && !similar(l, d) && !similar(a, r)) tl = a;
            if (similar(a, r) && !similar(a, l) && !similar(r, d)) tr = r;
            if (similar(d, l) && !similar(d, r) && !similar(l, a)) bl = l;
            if (similar(r, d) && !similar(r, a) && !similar(d, l)) br = d;
            b.px[(2 * y) * b.w + 2 * x] = tl; b.px[(2 * y) * b.w + 2 * x + 1] = tr;
            b.px[(2 * y + 1) * b.w + 2 * x] = bl; b.px[(2 * y + 1) * b.w + 2 * x + 1] = br;
        }
        return b;
    }

    // ------------------------------------------------------------------ 2. one-pixel outline
    private static void thinOutline(Canvas s, Canvas b) {
        int[] copy = b.px.clone();
        for (int y = 0; y < b.h; y++) for (int x = 0; x < b.w; x++) {
            int p = copy[y * b.w + x];
            if ((p >>> 24) == 0 || lum(p) > OUTLINE_LUM || !outline(s, x / 2, y / 2)) continue;
            boolean outside = false;
            for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + n[0], ny = y + n[1];
                if (nx < 0 || ny < 0 || nx >= b.w || ny >= b.h || (copy[ny * b.w + nx] >>> 24) == 0) outside = true;
            }
            if (outside) continue;
            int sx = x / 2, sy = y / 2, dx = x % 2 == 0 ? -1 : 1, dy = y % 2 == 0 ? -1 : 1;
            int fill = rock(s, sx + dx, sy), alt = rock(s, sx, sy + dy);
            if (fill == 0) fill = alt;
            if (fill == 0) for (int[] n : new int[][]{{-dx, 0}, {0, -dy}}) if (fill == 0) fill = rock(s, sx + n[0], sy + n[1]);
            if (fill != 0) b.px[y * b.w + x] = fill;
        }
    }
    /** The source colour at (x,y) if it is body rather than outline or air, else 0. */
    private static int rock(Canvas s, int x, int y) {
        return s.opaque(x, y) && !outline(s, x, y) && lum(s.get(x, y)) > OUTLINE_LUM ? s.get(x, y) : 0;
    }

    // ------------------------------------------------------------------ 3. one-pixel sparks and cracks
    private static final int[][] N8 = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
    private static void thinDetail(Canvas s, Canvas b) {
        for (int y = 0; y < s.h; y++) for (int x = 0; x < s.w; x++) {
            int p = s.get(x, y);
            if ((p >>> 24) == 0 || outline(s, x, y)) continue;
            int[] around = new int[8];
            int n = 0;
            for (int[] d : N8) if (s.opaque(x + d[0], y + d[1])) around[n++] = lum(s.get(x + d[0], y + d[1]));
            if (n < 6) continue;                                                // edges belong to the outline pass
            java.util.Arrays.sort(around, 0, n);
            int median = around[n / 2], lp = lum(p);
            int kind = lp - median > SPARK ? 1 : median - lp > CRACK ? -1 : 0;
            if (kind == 0) continue;
            boolean[] same = new boolean[8];                                    // neighbours continuing the spark or crack
            for (int i = 0; i < 8; i++) {
                int q = s.get(x + N8[i][0], y + N8[i][1]);
                same[i] = (q >>> 24) != 0 && (similar(q, p) || (kind > 0 ? lum(q) - median > SPARK : median - lum(q) > CRACK));
            }
            boolean h = same[1] || same[3], v = same[0] || same[2];
            boolean[] keep = new boolean[4];                                    // TL TR BL BR
            if (h && v) continue;                                               // a junction or a broad patch: leave it whole
            else if (h) { if (kind > 0) { keep[0] = keep[1] = true; } else { keep[2] = keep[3] = true; } }
            else if (v) { if (kind > 0) { keep[0] = keep[2] = true; } else { keep[1] = keep[3] = true; } }
            else if (same[4] || same[6]) { keep[0] = keep[3] = true; }
            else if (same[5] || same[7]) { keep[1] = keep[2] = true; }
            else keep[kind > 0 ? 0 : 3] = true;                                 // a lone fleck: one pixel on its lit or shaded corner
            for (int k = 0; k < 4; k++) {
                int bx = 2 * x + (k & 1), by = 2 * y + (k >> 1);
                if (keep[k] || b.get(bx, by) != p) continue;
                int side = rockNear(s, x, y, (k & 1) == 0 ? -1 : 1, (k >> 1) == 0 ? -1 : 1, median);
                if (side != 0) b.set(bx, by, side);
            }
        }
    }
    /** The neighbour colour that fills a thinned sub-pixel: the side it faces, else the one nearest the median. */
    private static int rockNear(Canvas s, int x, int y, int dx, int dy, int median) {
        int best = 0, bestD = Integer.MAX_VALUE;
        for (int[] d : new int[][]{{dx, 0}, {0, dy}, {-dx, 0}, {0, -dy}}) {
            int q = s.get(x + d[0], y + d[1]);
            if ((q >>> 24) == 0 || outline(s, x + d[0], y + d[1])) continue;
            int dist = Math.abs(lum(q) - median) + (d[0] == dx && d[1] == 0 || d[1] == dy && d[0] == 0 ? 0 : 12);
            if (dist < bestD) { bestD = dist; best = q; }
        }
        return best;
    }
}
