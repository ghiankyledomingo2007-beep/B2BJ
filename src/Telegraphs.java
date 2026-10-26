import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Ground warnings at native resolution, shown at 2x. */
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

    /** Soil splits into branching roots; no circular rim or expanding countdown ring. */
    void bloom(double cx,double cy,double radius,double charge,boolean active) {
        int left=clampX(cx-radius-2),right=clampX(cx+radius+2),top=clampY(cy-radius-2),bottom=clampY(cy+radius+2);
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++) {
            double distance=Math.hypot(x-cx,y-cy);
            if(distance>radius)continue;
            double angle=Math.atan2(y-cy,x-cx);
            double vein=Math.abs(Math.sin(angle*5+distance*.09))*distance;
            boolean grown=distance<radius*(.3+.7*clamp(charge));
            if(vein<(active?3:1.5))
                put(x,y,active?vein<1?0xb1ba7e:0x596349:grown?0x9ba47c:0x3e4939,active?220:grown?155:85);
            else if(((x*7+y*11)&15)<3&&distance<radius*.93)
                put(x,y,active?0x30382f:0x423e32,active?135:75);
        }
    }

    /** Bark veins occupy the committed lane without drawing a rectangular warning outline. */
    void root(double cx,double cy,double w,double h,double charge,boolean active) {
        int left=clampX(cx-w/2),right=clampX(cx+w/2),top=clampY(cy-h/2),bottom=clampY(cy+h/2);
        boolean across=w>h;
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++) {
            double dx=Math.abs(x-cx),dy=Math.abs(y-cy);
            if(dx>w/2||dy>h/2)continue;
            double along=across?x-cx:y-cy,side=across?y-cy:x-cx;
            double breadth=(across?h:w)/2;
            double bend=Math.sin(along*.045)*breadth*.19+Math.sin(along*.12)*1.5;
            double fork=Math.sin(along*.025+1.4)*breadth*.52;
            double distance=Math.min(Math.abs(side-bend),Math.abs(side-bend-fork));
            double thickness=active?3+2*Math.sin(along*.018+2):1;
            boolean grown=Math.abs(along)/((across?w:h)/2)<.15+.85*clamp(charge);
            if(distance<thickness)put(x,y,active?distance<1?0xb49665:side<bend?0x796349:0x30272b:grown?0xb4aaa1:0x796349,active?245:grown?85:40);
            else if(((x*7+y*11)&15)<3)put(x,y,active?0x70553b:0x423e32,active?110:65);
        }
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
