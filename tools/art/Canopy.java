import java.util.ArrayList;
import java.util.List;

/**
 * The forest pressing on a map's boundary banks: overlapping crowns lit from the upper left (moon),
 * a few dead trees, and ambient-occlusion shade where the canopy meets open ground. Paints into any
 * floor given its size, its canopy mask and its pixels.
 */
final class Canopy {
    private final int W, H;
    private final boolean[] canopy;
    private final int[] out;
    private final int[] dist;
    private static final int REACH = 12;

    Canopy(int w, int h, boolean[] canopy, int[] out) { W = w; H = h; this.canopy = canopy; this.out = out; dist = distanceToCanopy(); }

    void paint() {
        for (int i = 0; i < W * H; i++) if (canopy[i])
            out[i] = Noise.rnd(i % W / 2, i / W / 2, 201) < .5 ? Pal.INK : Pal.EARTH[0];
        // occlusion first, so overhanging crowns paint over their own shade
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = y * W + x, d = dist[i];
            if (canopy[i] || d > 9) continue;
            int steps = d <= 2 ? 2 : Noise.bayer(x, y) < 1 - (d - 2) / 7.0 ? 1 : 0;
            if (steps > 0) out[i] = Pal.shift(out[i], -steps);
        }
        List<double[]> crowns = new ArrayList<>();
        int cell = 12;
        for (int cy = -1; cy <= H / cell + 1; cy++) for (int cx = -1; cx <= W / cell + 1; cx++) {
            double x = cx * cell + Noise.rnd(cx, cy, 211) * cell, y = cy * cell + Noise.rnd(cx, cy, 212) * cell;
            int ix = (int) Math.max(0, Math.min(W - 1, x)), iy = (int) Math.max(0, Math.min(H - 1, y));
            if (!canopy[iy * W + ix]) continue;
            double r = 7 + Noise.rnd(cx, cy, 213) * 6;
            boolean dead = Noise.rnd(cx, cy, 214) < .12;
            crowns.add(new double[]{x, y, r, dead ? 1 : 0, Noise.hash(cx, cy, 215)});
        }
        crowns.sort((a, b) -> Double.compare(a[1], b[1]));
        for (double[] c : crowns) {
            if (c[3] == 1) deadTree(c); else crown(c);
        }
    }

    private boolean paintable(int x, int y) {
        if (x < 0 || y < 0 || x >= W || y >= H) return false;
        int i = y * W + x;
        return canopy[i] || dist[i] <= 3;
    }

    private void crown(double[] c) {
        double cx = c[0], cy = c[1], r = c[2];
        int seed = (int) c[4];
        for (int y = (int) (cy - r - 2); y <= cy + r + 2; y++) for (int x = (int) (cx - r - 2); x <= cx + r + 2; x++) {
            if (!paintable(x, y)) continue;
            double dx = x + .5 - cx, dy = y + .5 - cy, d = Math.hypot(dx, dy) / r;
            double rim = .86 + .26 * Noise.value(x, y, 2.6, seed);
            if (d > rim) continue;
            // moonlight from the upper left; leaf clumps break the gradient into clusters
            double lit = (-dx * .6 - dy * .8) / r + (Noise.value(x, y, 2.2, seed + 1) - .5) * .7;
            int colour = lit > .62 ? Pal.GRASS[3] : lit > .22 ? Pal.GRASS[2] : lit > -.35 ? Pal.GRASS[1] : Pal.GRASS[0];
            if (d > rim - .13 && dy > 0) colour = Pal.INK;
            out[y * W + x] = colour;
        }
    }

    private void deadTree(double[] c) {
        int cx = (int) c[0], cy = (int) c[1], seed = (int) c[4];
        int branches = 5 + Math.floorMod(seed, 3);
        for (int b = 0; b < branches; b++) {
            double a = b * Math.PI * 2 / branches + Noise.rnd(b, seed, 221) * .8;
            double len = c[2] * (.7 + Noise.rnd(b, seed, 222) * .4);
            int ex = (int) Math.round(cx + Math.cos(a) * len), ey = (int) Math.round(cy + Math.sin(a) * len * .8);
            line(cx, cy, ex, ey, Pal.WOOD[2]);
            double a2 = a + (Noise.rnd(b, seed, 223) < .5 ? .6 : -.6);
            int mx = (cx + ex) / 2, my = (cy + ey) / 2;
            line(mx, my, (int) Math.round(mx + Math.cos(a2) * len * .45), (int) Math.round(my + Math.sin(a2) * len * .36), Pal.WOOD[1]);
        }
        set(cx, cy, Pal.WOOD[3]); set(cx - 1, cy, Pal.WOOD[3]); set(cx, cy - 1, Pal.WOOD[4]);
    }

    private void line(int x0, int y0, int x1, int y1, int colour) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            set(x0, y0, colour);
            if (x0 == x1 && y0 == y1) return;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }
    private void set(int x, int y, int colour) { if (paintable(x, y)) out[y * W + x] = colour; }

    /** City-block distance (pixels) from each pixel to the nearest canopy pixel, capped at REACH. */
    private int[] distanceToCanopy() {
        int[] d = new int[W * H];
        java.util.Arrays.fill(d, REACH + 1);
        var queue = new java.util.ArrayDeque<Integer>();
        for (int i = 0; i < W * H; i++) if (canopy[i]) { d[i] = 0; queue.add(i); }
        while (!queue.isEmpty()) {
            int i = queue.poll(), x = i % W, y = i / W;
            if (d[i] >= REACH) continue;
            int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] o : n) {
                int nx = x + o[0], ny = y + o[1];
                if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
                int j = ny * W + nx;
                if (d[j] > d[i] + 1) { d[j] = d[i] + 1; queue.add(j); }
            }
        }
        return d;
    }
}
