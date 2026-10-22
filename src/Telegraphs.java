import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Small direction glints for enemy windups, painted at native resolution and shown at 2x. */
final class Telegraphs {
    static final int FRONT = 0xff9c80, AIM = 0xff7a5c;

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

    private static double clamp(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
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

}
