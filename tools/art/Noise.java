/** Deterministic hashing, value noise and ordered dither for the art generators. */
final class Noise {
    private Noise() { }

    static int hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
    /** Uniform [0,1). */
    static double rnd(int x, int y, int seed) { return (hash(x, y, seed) & 0xffffff) / (double) 0x1000000; }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    /** Value noise in [0,1] with cells of the given size. */
    static double value(double x, double y, double cell, int seed) {
        double fx = x / cell, fy = y / cell;
        int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy);
        double tx = smooth(fx - ix), ty = smooth(fy - iy);
        double a = rnd(ix, iy, seed), b = rnd(ix + 1, iy, seed), c = rnd(ix, iy + 1, seed), d = rnd(ix + 1, iy + 1, seed);
        return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * ty;
    }

    /** Value noise that repeats every `period` cells, for seamless tiles. */
    static double periodic(double x, double y, double cell, int period, int seed) {
        double fx = x / cell, fy = y / cell;
        int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy);
        double tx = smooth(fx - ix), ty = smooth(fy - iy);
        int x0 = Math.floorMod(ix, period), x1 = Math.floorMod(ix + 1, period);
        int y0 = Math.floorMod(iy, period), y1 = Math.floorMod(iy + 1, period);
        double a = rnd(x0, y0, seed), b = rnd(x1, y0, seed), c = rnd(x0, y1, seed), d = rnd(x1, y1, seed);
        return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * ty;
    }

    static double fbm(double x, double y, double cell, int octaves, int seed) {
        double sum = 0, amp = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += value(x, y, cell, seed + o * 101) * amp;
            norm += amp; amp *= .5; cell /= 2;
        }
        return sum / norm;
    }

    private static final int[] BAYER4 = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    /** Ordered-dither threshold in (0,1). */
    static double bayer(int x, int y) { return (BAYER4[(y & 3) * 4 + (x & 3)] + .5) / 16; }
}
