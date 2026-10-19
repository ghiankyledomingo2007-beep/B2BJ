/**
 * Jittered staggered-grid Voronoi cells for cobbles and gravel. Distances are squashed vertically so
 * stones read as seen from the 3/4 camera: wider than tall.
 */
final class Pebbles {
    private Pebbles() { }
    private static final double SQUASH = 1.3;

    /** {cell id, 1 when the pixel is the gap between two stones}. */
    static int[] nearest(int x, int y, int cell, int seed) {
        double px = x + .5, py = y + .5;
        int cy0 = (int) Math.floor(py / cell);
        double best = 1e18, second = 1e18;
        int bestId = 0;
        for (int cy = cy0 - 1; cy <= cy0 + 1; cy++) {
            double off = (cy & 1) * cell * .5;
            int cx0 = (int) Math.floor((px - off) / cell);
            for (int cx = cx0 - 1; cx <= cx0 + 1; cx++) {
                int idv = id(cx, cy);
                double[] c = centre(idv, cell, seed);
                double dx = px - c[0], dy = (py - c[1]) * SQUASH, d = Math.sqrt(dx * dx + dy * dy);
                if (d < best) { second = best; best = d; bestId = idv; }
                else if (d < second) second = d;
            }
        }
        return new int[]{bestId, second - best < cell * .15 ? 1 : 0};
    }

    static int id(int cx, int cy) { return (cx + 64) * 4096 + (cy + 64); }

    static double[] centre(int idv, int cell, int seed) {
        int cx = idv / 4096 - 64, cy = idv % 4096 - 64;
        double off = (cy & 1) * cell * .5;
        return new double[]{cx * cell + off + cell / 2.0 + (Noise.rnd(cx, cy, seed) - .5) * cell * .55,
                cy * cell + cell / 2.0 + (Noise.rnd(cx, cy, seed + 1) - .5) * cell * .45};
    }
}
