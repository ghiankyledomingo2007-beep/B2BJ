import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** A small ARGB pixel canvas with the primitives the Outpost art is drawn from. Binary alpha only. */
final class Canvas {
    final int w, h;
    final int[] px;

    Canvas(int w, int h) { this.w = w; this.h = h; px = new int[w * h]; }

    boolean in(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h; }
    int get(int x, int y) { return in(x, y) ? px[y * w + x] : 0; }
    boolean opaque(int x, int y) { return (get(x, y) >>> 24) != 0; }
    void set(int x, int y, int c) { if (in(x, y)) px[y * w + x] = c; }
    /** Paints only over pixels that already hold colour: shading never grows a silhouette. */
    void tint(int x, int y, int c) { if (opaque(x, y)) px[y * w + x] = c; }
    void clear(int x, int y) { if (in(x, y)) px[y * w + x] = 0; }

    void fill(int c) { java.util.Arrays.fill(px, c); }
    void rect(int x, int y, int rw, int rh, int c) {
        for (int j = y; j < y + rh; j++) for (int i = x; i < x + rw; i++) set(i, j, c);
    }
    void hline(int x0, int x1, int y, int c) { for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) set(x, y, c); }
    void vline(int x, int y0, int y1, int c) { for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) set(x, y, c); }

    void line(int x0, int y0, int x1, int y1, int c) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            set(x0, y0, c);
            if (x0 == x1 && y0 == y1) return;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    /** Filled ellipse centred on a pixel grid point; radii may be fractional for odd widths. */
    void ellipse(double cx, double cy, double rx, double ry, int c) {
        for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++)
            for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++) {
                double dx = (x + .5 - cx) / rx, dy = (y + .5 - cy) / ry;
                if (dx * dx + dy * dy <= 1) set(x, y, c);
            }
    }

    /** Scanline polygon fill with pixel-centre sampling. */
    void poly(int[] xs, int[] ys, int c) {
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int y : ys) { minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        for (int y = minY; y <= maxY; y++) {
            double sy = y + .5;
            java.util.List<Double> cross = new java.util.ArrayList<>();
            for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
                double yi = ys[i], yj = ys[j];
                if ((yi <= sy && yj > sy) || (yj <= sy && yi > sy))
                    cross.add(xs[i] + (sy - yi) / (yj - yi) * (xs[j] - xs[i]));
            }
            java.util.Collections.sort(cross);
            for (int k = 0; k + 1 < cross.size(); k += 2)
                for (int x = (int) Math.ceil(cross.get(k) - .5); x <= (int) Math.floor(cross.get(k + 1) - .5); x++) set(x, y, c);
        }
    }

    /** Copies opaque pixels of src at (dx,dy). */
    void blit(Canvas src, int dx, int dy) { blit(src, dx, dy, false); }
    void blit(Canvas src, int dx, int dy, boolean flip) {
        for (int y = 0; y < src.h; y++) for (int x = 0; x < src.w; x++) {
            int c = src.px[y * src.w + (flip ? src.w - 1 - x : x)];
            if ((c >>> 24) != 0) set(dx + x, dy + y, c);
        }
    }

    /** One-pixel outline on transparent pixels that touch colour (4-neighbourhood). */
    void outline(int ink) {
        int[] copy = px.clone();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if ((copy[y * w + x] >>> 24) != 0) continue;
            boolean edge = false;
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] n : d) {
                int nx = x + n[0], ny = y + n[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && (copy[ny * w + nx] >>> 24) != 0) edge = true;
            }
            if (edge) px[y * w + x] = ink;
        }
    }

    /** Selective outline: the silhouette edge takes the darkest step of the colour it borders. */
    void selectiveOutline() {
        int[] copy = px.clone();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if ((copy[y * w + x] >>> 24) != 0) continue;
            int found = 0;
            int[][] d = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
            for (int[] n : d) {
                int nx = x + n[0], ny = y + n[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && (copy[ny * w + nx] >>> 24) != 0) { found = copy[ny * w + nx]; break; }
            }
            if (found != 0) px[y * w + x] = Pal.shift(found, -4) == found ? Pal.INK : darkest(found);
        }
    }
    private static int darkest(int c) {
        int d = Pal.shift(c, -9);
        return d;
    }

    Canvas copy() { Canvas c = new Canvas(w, h); System.arraycopy(px, 0, c.px, 0, px.length); return c; }
    Canvas flipped() { Canvas c = new Canvas(w, h); c.blit(this, 0, 0, true); return c; }

    BufferedImage image() {
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        return img;
    }
    /**
     * Writes a generated PNG, but never over a hand edit: tools/art/generated.sha records the digest
     * of each file as last generated, and a file on disk that no longer matches it has been retouched,
     * so it is kept (and reported) unless the build runs with --force.
     */
    void save(String path) {
        try {
            File f = new File(path);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            if (f.exists() && !Manifest.FORCE) {
                String recorded = Manifest.get(path);
                if (recorded != null && !recorded.equals(Manifest.digest(f.toPath()))) {
                    System.out.println("kept hand-edited " + path + " (rerun with --force to regenerate it)");
                    return;
                }
            }
            ImageIO.write(image(), "png", f);
            Manifest.put(path, Manifest.digest(f.toPath()));
        } catch (IOException e) { throw new IllegalStateException("cannot write " + path, e); }
    }

    /** Digests of generated files, so the build can tell its own output from a retouch. */
    static final class Manifest {
        static boolean FORCE;
        private static final java.nio.file.Path FILE = java.nio.file.Path.of("tools/art/generated.sha");
        private static final java.util.TreeMap<String, String> ENTRIES = new java.util.TreeMap<>();
        static {
            try {
                if (java.nio.file.Files.exists(FILE)) for (String line : java.nio.file.Files.readAllLines(FILE)) {
                    int space = line.indexOf(' ');
                    if (space > 0) ENTRIES.put(line.substring(space + 1), line.substring(0, space));
                }
            } catch (IOException e) { throw new IllegalStateException(e); }
        }
        static String get(String path) { return ENTRIES.get(path); }
        static void put(String path, String digest) {
            ENTRIES.put(path, digest);
            var text = new StringBuilder();
            for (var e : ENTRIES.entrySet()) text.append(e.getValue()).append(' ').append(e.getKey()).append('\n');
            try { java.nio.file.Files.writeString(FILE, text); } catch (IOException e) { throw new IllegalStateException(e); }
        }
        static String digest(java.nio.file.Path file) throws IOException {
            try {
                byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(file));
                var hex = new StringBuilder();
                for (byte b : d) hex.append(String.format("%02x", b));
                return hex.toString();
            } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        }
    }
    static Canvas load(String path) {
        try {
            var img = ImageIO.read(new File(path));
            Canvas c = new Canvas(img.getWidth(), img.getHeight());
            img.getRGB(0, 0, c.w, c.h, c.px, 0, c.w);
            return c;
        } catch (IOException e) { throw new IllegalStateException("cannot read " + path, e); }
    }

    /** Places several frames side by side into one horizontal sheet. */
    static Canvas strip(Canvas... frames) {
        int fw = frames[0].w, fh = frames[0].h;
        Canvas sheet = new Canvas(fw * frames.length, fh);
        for (int i = 0; i < frames.length; i++) sheet.blit(frames[i], i * fw, 0);
        return sheet;
    }
}
