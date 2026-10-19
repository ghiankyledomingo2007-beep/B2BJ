/** Running-bond flagstone courses: rows of varied-width slabs, each row offset from the last. */
final class Slabs {
    private Slabs() { }
    static final int ROW = 12;
    private static final java.util.Map<Integer, int[]> ROWS = new java.util.HashMap<>();

    private static int[] row(int r) {
        return ROWS.computeIfAbsent(r, key -> {
            int[] b = new int[400];
            int x = 0;
            for (int k = 0; k < b.length; k++) { b[k] = x; x += 16 + Math.floorMod(Noise.hash(key, k, 91), 12); }
            return b;
        });
    }
    private static int shift(int r) { return (int) (Noise.rnd(r, 0, 93) * 28); }

    /** {slab id, 1 when the pixel is mortar}. */
    static int[] at(int x, int y) {
        int r = Math.floorDiv(y, ROW);
        int[] b = row(r);
        int sx = x + shift(r);
        int k = java.util.Arrays.binarySearch(b, sx);
        if (k < 0) k = -k - 2;
        k = Math.max(0, k);
        boolean mortar = sx == b[k] || y == r * ROW;
        return new int[]{(r + 16) * 1024 + k, mortar ? 1 : 0};
    }

    /** {left, top, width, height} of a slab in map pixels. */
    static int[] box(int idv) {
        int r = idv / 1024 - 16, k = idv % 1024;
        int[] b = row(r);
        return new int[]{b[k] - shift(r), r * ROW, b[k + 1] - b[k], ROW};
    }

    /** About one slab in six carries a single hairline crack from its top edge to its bottom edge. */
    static boolean cracked(int idv, int lx, int ly, int w, int h) {
        if (Noise.rnd(idv, 6, 95) >= .16 || ly < 1 || ly > h - 2 || w < 6) return false;
        double a = 2 + Noise.rnd(idv, 7, 95) * (w - 4), b = 2 + Noise.rnd(idv, 8, 95) * (w - 4);
        return lx == (int) Math.round(a + (b - a) * (ly - 1) / Math.max(1, h - 3));
    }
}
