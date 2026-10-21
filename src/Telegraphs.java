import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * Attack warnings, painted pixel by pixel at native resolution (the game shows them at 2x).
 * Every hazard speaks one flat language so it reads as information, never as an object:
 * a 1 px outline whose outermost pixels are the inclusive hit boundary, a faint fill that
 * grows from the source and meets the outline when damage lands, and one flash on impact.
 * Projectile windups use short, soft direction marks instead of full-flight lines.
 * Overlapping warnings keep the strongest pixel instead of stacking into a solid blob.
 */
final class Telegraphs {
    /** HUD danger red for lines, a deeper red for fills, the fire ramp's white-hot for impact. */
    static final int EDGE = 0xf16458, FILL = 0xdc474e, FRONT = 0xff9c80, HOT = 0xfff0bd, AIM = 0xff7a5c;
    static final int FILL_ALPHA = 46, FRONT_ALPHA = 175;

    private final BufferedImage image;
    private final int[] px;
    private final int width, height;
    private int x0, y0, x1, y1;   // area painted since begin(); empty when x1 < x0

    Telegraphs(int width, int height) {
        this.width = width; this.height = height;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        px = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        x1 = -1; y1 = -1;
    }

    BufferedImage image() { return image; }

    /** Wipes last frame's marks; only the area that was painted is touched. */
    void begin() {
        for (int y = Math.max(0, y0); y <= y1; y++) Arrays.fill(px, y * width + x0, y * width + x1 + 1, 0);
        x0 = width; y0 = height; x1 = -1; y1 = -1;
    }

    /** Blits the painted area at `scale` with hard pixel edges. */
    void drawTo(Graphics2D canvas, int scale) {
        if (x1 < x0) return;
        var g = (Graphics2D) canvas.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, x0 * scale, y0 * scale, (x1 + 1) * scale, (y1 + 1) * scale, x0, y0, x1 + 1, y1 + 1, null);
        g.dispose();
    }

    // ------------------------------------------------------------------ hazards

    /**
     * Damage inside radius `r` of (cx, cy). `charge` 0..1 is how far the tell has run; `hit`
     * 0..1 is the impact flash while damage is live (0 during the tell).
     */
    void disc(double cx, double cy, double r, double charge, double hit, boolean calm) {
        int ox = (int) Math.floor(cx), oy = (int) Math.floor(cy), radius = (int) Math.floor(r);
        if (radius < 1) return;
        if (hit > 0) {
            fillDisc(ox, oy, radius, FILL, impactFill(hit, calm));
            ring(ox, oy, radius, impactEdge(hit, calm), 255);
            return;
        }
        int grown = (int) Math.round((radius - 1) * clamp(charge));
        if (grown >= 1) {
            fillDisc(ox, oy, grown, FILL, FILL_ALPHA);
            ring(ox, oy, grown, FRONT, FRONT_ALPHA);
        }
        ring(ox, oy, radius, EDGE, edgeAlpha(charge));
    }

    /** A persistent zone (the Ichor Golem's drain aura): present, but quiet. */
    void aura(double cx, double cy, double r) {
        int ox = (int) Math.floor(cx), oy = (int) Math.floor(cy), radius = (int) Math.floor(r);
        if (radius < 1) return;
        fillDisc(ox, oy, radius, FILL, 16);
        ring(ox, oy, radius, EDGE, 90);
    }

    /**
     * A shockwave's tell: a dashed ring at the farthest reach of its band whose dashes close into
     * a solid line as it is about to fire, around the charge building at its centre.
     */
    void wave(double cx, double cy, double start, double reach, double charge, boolean calm) {
        int ox = (int) Math.floor(cx), oy = (int) Math.floor(cy);
        disc(ox, oy, start, charge, 0, calm);
        ring(ox, oy, (int) Math.floor(reach), EDGE, edgeAlpha(charge), 1 + (int) (3 * clamp(charge)));
    }

    /** A shockwave's travelling band of damage between two radii, and its fading reach. */
    void band(double cx, double cy, double inner, double outer, double reach, double hit, boolean calm) {
        int ox = (int) Math.floor(cx), oy = (int) Math.floor(cy);
        int ri = (int) Math.ceil(inner), ro = (int) Math.floor(outer);
        ring(ox, oy, (int) Math.floor(reach), EDGE, 110, 2);
        if (ro < 1) return;
        int fill = impactFill(Math.max(hit, .35), calm), colour = FILL;
        int[] outerSpan = spans(ro), innerSpan = ri >= 1 ? spans(ri) : null;
        for (int dy = -ro; dy <= ro; dy++) {
            int to = outerSpan[Math.abs(dy)] - 1;
            int from = innerSpan != null && Math.abs(dy) <= ri ? outerExtent(ri, Math.abs(dy)) + 1 : 0;
            for (int dx = from; dx <= to; dx++) { put(ox + dx, oy + dy, colour, fill); put(ox - dx, oy + dy, colour, fill); }
        }
        ring(ox, oy, ro, calm ? EDGE : HOT, 255);
        if (ri >= 1) ring(ox, oy, ri, EDGE, 210);
    }

    /**
     * Everything within `r` of the segment start→end: a charge, a lunge, a shot's lane. While the aim
     * still follows its target (`locked` false) only a faint outline shows; once locked, the fill runs
     * from the start along the path. With `hit` > 0 use {@link #disc} on the moving body instead.
     */
    void capsule(double ax, double ay, double bx, double by, double r, double charge, boolean locked) {
        ax = Math.floor(ax); ay = Math.floor(ay); bx = Math.floor(bx); by = Math.floor(by); r = Math.floor(r);
        double length = Math.hypot(bx - ax, by - ay);
        double ux = length > 0 ? (bx - ax) / length : 1, uy = length > 0 ? (by - ay) / length : 0;
        double total = length + 2 * r, front = -r + total * clamp(charge);
        int radius = (int) Math.round(r);
        int left = clampX(Math.min(ax, bx) - r - 1), right = clampX(Math.max(ax, bx) + r + 1);
        int top = clampY(Math.min(ay, by) - r - 1), bottom = clampY(Math.max(ay, by) + r + 1);
        double inside = (r - .5) * (r - .5), frontHalf = Math.max(Math.abs(ux), Math.abs(uy)) / 2;
        if (locked) for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
            double rx = x - ax, ry = y - ay, t = rx * ux + ry * uy;
            double along = Math.max(0, Math.min(length, t)), qx = rx - ux * along, qy = ry - uy * along;
            if (qx * qx + qy * qy > inside || t > front + frontHalf) continue;
            put(x, y, Math.abs(t - front) <= frontHalf ? FRONT : FILL, Math.abs(t - front) <= frontHalf ? FRONT_ALPHA : FILL_ALPHA);
        }
        int alpha = locked ? edgeAlpha(charge) : 110;
        double nx = -uy * radius, ny = ux * radius;
        line(ax + nx, ay + ny, bx + nx, by + ny, EDGE, alpha);
        line(ax - nx, ay - ny, bx - nx, by - ny, EDGE, alpha);
        cap(ax, ay, radius, -ux, -uy, alpha);
        cap(bx, by, radius, ux, uy, alpha);
    }

    /** A soft ground glint along the shot's heading, brightening as the caster commits. */
    void aim(double ax, double ay, double bx, double by, double start, double charge, boolean locked) {
        double length = Math.hypot(bx - ax, by - ay);
        if (length <= start + 1) return;
        double ux = (bx - ax) / length, uy = (by - ay) / length;
        double sx = ax + ux * start, sy = ay + uy * start;
        int left = clampX(Math.min(sx, bx) - 5), right = clampX(Math.max(sx, bx) + 5);
        int top = clampY(Math.min(sy, by) - 5), bottom = clampY(Math.max(sy, by) + 5);
        double brightness = (60 + 60 * clamp(charge)) * (locked ? 1 : .75);
        for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
            double along = (x - ax) * ux + (y - ay) * uy;
            if (along <= start || along >= length) continue;
            double t = (along - start) / (length - start), envelope = 4 * t * (1 - t);
            double half = 1 + 4 * envelope, side = Math.abs((x - ax) * uy - (y - ay) * ux);
            if (side < half) put(x, y, AIM, (int) (brightness * envelope * (1 - side / half)));
        }
        int tipX = (int) Math.round(bx - ux * 5), tipY = (int) Math.round(by - uy * 5);
        int spark = (int) (100 + 90 * clamp(charge));
        put(tipX, tipY, FRONT, spark);
        put(tipX - 1, tipY, AIM, spark / 2); put(tipX + 1, tipY, AIM, spark / 2);
        put(tipX, tipY - 1, AIM, spark / 2); put(tipX, tipY + 1, AIM, spark / 2);
    }

    /**
     * A lunge: the line of the rush and an arrowhead at its end, as wide as the body that sweeps the path
     * (`r`), so direction, reach and width all read with almost no ink.
     */
    void lunge(double ax, double ay, double bx, double by, double r, double start, double charge, boolean locked) {
        double length = Math.hypot(bx - ax, by - ay);
        if (length <= start + 1) return;
        double ux = (bx - ax) / length, uy = (by - ay) / length, vx = -uy, vy = ux;
        double head = Math.min(r * .8, length * .3);
        ray(ax + ux * start, ay + uy * start, bx - ux, by - uy, charge, locked, EDGE, 190, 120);
        int alpha = locked ? 235 : 150;
        line(bx - ux * head + vx * head, by - uy * head + vy * head, bx, by, locked ? FRONT : EDGE, alpha);
        line(bx - ux * head - vx * head, by - uy * head - vy * head, bx, by, locked ? FRONT : EDGE, alpha);
    }

    /** A fading, optionally dashed one-pixel ray; the first `charge` of it is drawn hot. */
    private void ray(double ax, double ay, double bx, double by, double charge, boolean locked, int rgb, int near, int far) {
        int x = (int) Math.round(ax), y = (int) Math.round(ay), tx = (int) Math.round(bx), ty = (int) Math.round(by);
        int dx = Math.abs(tx - x), dy = -Math.abs(ty - y), sx = x < tx ? 1 : -1, sy = y < ty ? 1 : -1, err = dx + dy;
        int steps = Math.max(dx, -dy), hot = (int) Math.round(steps * clamp(charge));
        for (int i = 0; ; i++) {
            boolean gap = !locked && (i / 3) % 2 == 1;
            if (!gap) {
                double t = steps == 0 ? 0 : i / (double) steps;
                int alpha = (int) (near + (far - near) * t);
                if (!locked) alpha = alpha * 7 / 8;
                put(x, y, i <= hot && locked ? FRONT : rgb, i <= hot ? Math.min(255, alpha + 40) : alpha);
            }
            if (x == tx && y == ty) return;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }
    }

    /**
     * A rectangle `length` long and `half` wide either side of its axis, centred on (cx, cy)
     * along `angle`. `fromMiddle` grows the fill outward both ways (a fissure that splits from
     * its centre); otherwise it grows from the back edge forward (a blow struck from there).
     */
    void box(double cx, double cy, double angle, double length, double half, double charge, double hit,
             boolean locked, boolean fromMiddle, boolean calm) {
        double ux = Math.cos(angle), uy = Math.sin(angle), vx = -uy, vy = ux;
        cx = Math.floor(cx); cy = Math.floor(cy);
        double reach = Math.hypot(length / 2, half) + 1;
        int left = clampX(cx - reach), right = clampX(cx + reach), top = clampY(cy - reach), bottom = clampY(cy + reach);
        double grown = hit > 0 ? 1 : locked ? clamp(charge) : 0, frontHalf = Math.max(Math.abs(ux), Math.abs(uy)) / 2;
        int fill = hit > 0 ? impactFill(hit, calm) : FILL_ALPHA, colour = FILL;
        double ox = cx, oy = cy;
        for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
            double u = (x - ox) * ux + (y - oy) * uy, v = (x - ox) * vx + (y - oy) * vy;
            if (Math.abs(u) > length / 2 - .5 || Math.abs(v) > half - .5) continue;
            double depth = fromMiddle ? Math.abs(u) : u + length / 2, limit = (fromMiddle ? length / 2 : length) * grown;
            if (grown <= 0 || depth > limit + frontHalf) continue;
            boolean front = hit <= 0 && grown < 1 && Math.abs(depth - limit) <= frontHalf;
            put(x, y, front ? FRONT : colour, front ? FRONT_ALPHA : fill);
        }
        int edge = hit > 0 ? impactEdge(hit, calm) : EDGE, alpha = hit > 0 ? 255 : locked ? edgeAlpha(charge) : 110;
        double ax = ux * length / 2, ay = uy * length / 2, bx = vx * half, by = vy * half;
        line(cx - ax - bx, cy - ay - by, cx + ax - bx, cy + ay - by, edge, alpha);
        line(cx - ax + bx, cy - ay + by, cx + ax + bx, cy + ay + by, edge, alpha);
        line(cx - ax - bx, cy - ay - by, cx - ax + bx, cy - ay + by, edge, alpha);
        line(cx + ax - bx, cy + ay - by, cx + ax + bx, cy + ay + by, edge, alpha);
    }

    // ------------------------------------------------------------------ pixels

    private static double clamp(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    /** Outline firms up as the hit nears. */
    private static int edgeAlpha(double charge) { return (int) (150 + 95 * clamp(charge)); }
    /** While damage is live the area stays faint; the white-hot rim carries the impact. */
    private static int impactFill(double hit, boolean calm) { return (int) (calm ? 44 : 34 + 34 * clamp(hit)); }
    private static int impactEdge(double hit, boolean calm) { return !calm && hit > .55 ? HOT : EDGE; }
    private int clampX(double x) { return (int) Math.max(0, Math.min(width - 1, Math.floor(x))); }
    private int clampY(double y) { return (int) Math.max(0, Math.min(height - 1, Math.floor(y))); }

    private void put(int x, int y, int rgb, int alpha) {
        if (alpha <= 0 || x < 0 || y < 0 || x >= width || y >= height) return;
        int i = y * width + x;
        if ((px[i] >>> 24) >= alpha) return;
        px[i] = Math.min(255, alpha) << 24 | rgb;
        if (x < x0) x0 = x;
        if (x > x1) x1 = x;
        if (y < y0) y0 = y;
        if (y > y1) y1 = y;
    }

    /** Midpoint circle: clean 1 px steps, outermost pixels exactly `r` from the centre. */
    private void ring(int ox, int oy, int r, int rgb, int alpha) { ring(ox, oy, r, rgb, alpha, 4); }
    /** `dash` of every 4 px of arc length are drawn: 4 is solid, 2 is an even dash. */
    private void ring(int ox, int oy, int r, int rgb, int alpha, int dash) {
        if (r <= 0) { put(ox, oy, rgb, alpha); return; }
        for (int x = r, y = 0, err = 1 - r; x >= y; ) {
            for (int o = 0; o < 8; o++) {
                int dx = (o & 1) == 0 ? x : y, dy = (o & 1) == 0 ? y : x;
                if ((o & 2) != 0) dx = -dx;
                if ((o & 4) != 0) dy = -dy;
                if (dash >= 4 || Math.floorMod((int) Math.round((Math.atan2(dy, dx) + Math.PI) * r), 4) < dash)
                    put(ox + dx, oy + dy, rgb, alpha);
            }
            y++;
            if (err < 0) err += 2 * y + 1;
            else { x--; err += 2 * (y - x) + 1; }
        }
    }

    /** Half of a midpoint circle: the side facing (dirX, dirY). Closes a capsule's end. */
    private void cap(double cx, double cy, int r, double dirX, double dirY, int alpha) {
        int ox = (int) Math.round(cx), oy = (int) Math.round(cy);
        for (int x = r, y = 0, err = 1 - r; x >= y; ) {
            for (int o = 0; o < 8; o++) {
                int dx = (o & 1) == 0 ? x : y, dy = (o & 1) == 0 ? y : x;
                if ((o & 2) != 0) dx = -dx;
                if ((o & 4) != 0) dy = -dy;
                if (dx * dirX + dy * dirY >= -.5) put(ox + dx, oy + dy, EDGE, alpha);
            }
            y++;
            if (err < 0) err += 2 * y + 1;
            else { x--; err += 2 * (y - x) + 1; }
        }
    }

    /** For each row offset of a midpoint circle: the outline's innermost |x| on that row. */
    private static int[] spans(int r) {
        int[] inner = new int[r + 1];
        Arrays.fill(inner, Integer.MAX_VALUE);
        for (int x = r, y = 0, err = 1 - r; x >= y; ) {
            inner[y] = Math.min(inner[y], x);
            inner[x] = Math.min(inner[x], y);
            y++;
            if (err < 0) err += 2 * y + 1;
            else { x--; err += 2 * (y - x) + 1; }
        }
        return inner;
    }
    /** The outermost |x| a midpoint circle of radius r reaches on row dy. */
    private static int outerExtent(int r, int dy) {
        int best = -1;
        for (int x = r, y = 0, err = 1 - r; x >= y; ) {
            if (y == dy) best = Math.max(best, x);
            if (x == dy) best = Math.max(best, y);
            y++;
            if (err < 0) err += 2 * y + 1;
            else { x--; err += 2 * (y - x) + 1; }
        }
        return best;
    }

    private void fillDisc(int ox, int oy, int r, int rgb, int alpha) {
        int[] inner = spans(r);
        for (int dy = -r; dy <= r; dy++) {
            int span = inner[Math.abs(dy)] - 1;
            for (int dx = -span; dx <= span; dx++) put(ox + dx, oy + dy, rgb, alpha);
        }
    }

    /** Bresenham: one pixel per step, no doubled corners. */
    private void line(double ax, double ay, double bx, double by, int rgb, int alpha) {
        int x = (int) Math.round(ax), y = (int) Math.round(ay), tx = (int) Math.round(bx), ty = (int) Math.round(by);
        int dx = Math.abs(tx - x), dy = -Math.abs(ty - y), sx = x < tx ? 1 : -1, sy = y < ty ? 1 : -1, err = dx + dy;
        for (int step = 0; ; step++) {
            put(x, y, rgb, alpha);
            if (x == tx && y == ty) return;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }
    }
}
