import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pixel UI kit: the game's two pixel fonts, 9-slice frames, icons and text helpers. The art is
 * assets/ui/kit.png (drawn by tools/art/UiArt.java); one art pixel is {@link #S} screen pixels, the
 * same scale as the world, so menus and HUD share one pixel grid. Coordinates here are screen pixels.
 */
final class Ui {
    private Ui() { }

    /** Screen pixels per art pixel. */
    static final int S = 2;

    static final Color INK = new Color(0x0b0a14), PANEL = new Color(0x0c111b), TEXT = new Color(0xece2c8),
            MUTED = new Color(0x9a9cb4), DIM = new Color(0x5d5f78), GOLD = new Color(0xeeb840),
            GOLD_LIGHT = new Color(0xffdd80), CYAN = new Color(0x7aeee6), TEAL = new Color(0x2cc6c8),
            DANGER = new Color(0xf16458), AMBER = new Color(0xf08a32), SHADOW = new Color(5, 6, 12, 200);
    /** Display-title ramps, light to dark: top rows take the first colour. */
    static final int[] GOLD_RAMP = {0xfff3c8, 0xffdd80, 0xeeb840, 0xc48a26},
            STEEL_RAMP = {0xffffff, 0xcfd6e2, 0x9aa3b6, 0x6c7690},
            CYAN_RAMP = {0xcffff9, 0x7aeee6, 0x2cc6c8, 0x178a93},
            BLOOD_RAMP = {0xffc0a8, 0xf16458, 0xb8323a, 0x7a1c26},
            BONE_RAMP = {0xf4efe0, 0xd8d0ba, 0xb0a78f, 0x857d68};

    /** Rectangles in kit.png. `inset` is the 9-slice border in art pixels (0: drawn whole). */
    enum Piece {
        WINDOW(0, 0, 48, 48, 12), PANEL(48, 0, 24, 24, 6), INSET(72, 0, 16, 16, 4), CARD(88, 0, 24, 24, 6),
        BUTTON(0, 48, 24, 16, 5), BUTTON_HOT(24, 48, 24, 16, 5), BUTTON_DOWN(48, 48, 24, 16, 5), BUTTON_OFF(72, 48, 24, 16, 5),
        TAG(96, 48, 16, 12, 4), KEYCAP(112, 48, 12, 14, 4),
        CHECK(128, 48, 11, 11, 0), CHECK_ON(140, 48, 11, 11, 0), CHECK_HOT(152, 48, 11, 11, 0), CHECK_HOT_ON(164, 48, 11, 11, 0),
        TRACK(0, 64, 16, 6, 2), TRACK_FILL(16, 64, 16, 6, 2), KNOB(32, 64, 7, 11, 0), KNOB_HOT(40, 64, 7, 11, 0),
        BOSS_LEFT(0, 80, 26, 24, 0), BOSS_MID(26, 80, 8, 24, 0), BOSS_RIGHT(34, 80, 14, 24, 0),
        EMBLEM_WARDEN(48, 80, 16, 16, 0), EMBLEM_BRIAR(64, 80, 16, 16, 0),
        CURSOR(0, 104, 12, 9, 0), CURSOR_2(12, 104, 12, 9, 0), CLASP(24, 104, 12, 7, 0), CLASP_V(36, 104, 7, 12, 0),
        DIAMOND(44, 104, 7, 7, 0), ORNAMENT(52, 104, 26, 9, 0), SCROLL_THUMB(80, 104, 6, 12, 2), MOUSE_L(88, 104, 10, 14, 0),
        MOUSE_R(98, 104, 10, 14, 0), FLOURISH(0, 176, 160, 16, 0);

        final int x, y, w, h, inset;
        Piece(int x, int y, int w, int h, int inset) { this.x = x; this.y = y; this.w = w; this.h = h; this.inset = inset; }
    }

    /** 12x12 icons in one row of kit.png starting at y = 128 (and a second row at 140). */
    enum Icon {
        SHARD, OBJECTIVE, SKULL, CAMP, GATE, MEMORY, CACHE, PLAYER, MENU, GEAR, SOUND, EYE, FLASK, MAP, KEYS, DOOR,
        BACK, PLAY, NEW, HEART, CROWN, STAIR, TIP, SWORD;
        static final int CELL = 12, Y = 128, PER_ROW = 16;
        int x() { return ordinal() % PER_ROW * CELL; }
        int y() { return Y + ordinal() / PER_ROW * CELL; }
    }

    static final BufferedImage KIT = B2BJ.loadImage("assets/ui/kit.png");
    static final Font SANS = font("assets/ui/fonts/b2bj-sans.ttf"), DISPLAY = font("assets/ui/fonts/b2bj-display.ttf");
    private static final Font HEADING = font("assets/ui/fonts/DejaVuSerif.ttf"), BODY = font("assets/ui/fonts/DejaVuSans.ttf");
    static Font heading(float size) { return HEADING.deriveFont(size); }
    static Font body(float size) { return BODY.deriveFont(size); }
    static void prose(Graphics2D g, String text, int x, int y, Font font, Color color) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font); g.setColor(color); g.drawString(text,x,y);
    }

    private static final Font[] SANS_AT = new Font[7], DISPLAY_AT = new Font[7];

    private static Font font(String path) {
        try (InputStream in = open(path)) {
            if (in != null) return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception broken) {
            // a missing or damaged font falls back to the platform monospace below
        }
        return new Font(Font.MONOSPACED, Font.BOLD, 10);
    }

    static InputStream open(String path) throws java.io.IOException {
        InputStream in = Ui.class.getResourceAsStream("/" + path);
        if (in != null) return in;
        Path file = Path.of(path);
        return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
    }

    /** True when the packaged pixel fonts loaded (false only in a broken install). */
    static boolean fontsLoaded() { return SANS.getFamily().startsWith("B2BJ") && DISPLAY.getFamily().startsWith("B2BJ"); }

    /** Pixel sans with each font pixel `scale` screen pixels wide (the game uses 2). */
    static Font sans(int scale) {
        int s = Math.max(1, Math.min(6, scale));
        if (SANS_AT[s] == null) SANS_AT[s] = SANS.deriveFont(10f * s);
        return SANS_AT[s];
    }

    static Font display(int scale) {
        int s = Math.max(1, Math.min(6, scale));
        if (DISPLAY_AT[s] == null) DISPLAY_AT[s] = DISPLAY.deriveFont(10f * s);
        return DISPLAY_AT[s];
    }

    /** Hard pixel edges for everything drawn after this. */
    static void crisp(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    // ------------------------------------------------------------------ frames

    /** A kit piece stretched by 9-slice into the screen rectangle; sizes snap to the 2x grid. */
    static void nine(Graphics2D g, Piece p, int x, int y, int w, int h) {
        w = w / S * S; h = h / S * S;
        if (KIT == null) { fallbackFrame(g, x, y, w, h); return; }
        int i = p.inset, d = i * S;
        if (i == 0) { blit(g, p.x, p.y, p.w, p.h, x, y, w, h); return; }
        int mw = p.w - 2 * i, mh = p.h - 2 * i, cw = Math.max(0, w - 2 * d), ch = Math.max(0, h - 2 * d);
        blit(g, p.x, p.y, i, i, x, y, d, d);
        blit(g, p.x + i, p.y, mw, i, x + d, y, cw, d);
        blit(g, p.x + p.w - i, p.y, i, i, x + w - d, y, d, d);
        blit(g, p.x, p.y + i, i, mh, x, y + d, d, ch);
        blit(g, p.x + i, p.y + i, mw, mh, x + d, y + d, cw, ch);
        blit(g, p.x + p.w - i, p.y + i, i, mh, x + w - d, y + d, d, ch);
        blit(g, p.x, p.y + p.h - i, i, i, x, y + h - d, d, d);
        blit(g, p.x + i, p.y + p.h - i, mw, i, x + d, y + h - d, cw, d);
        blit(g, p.x + p.w - i, p.y + p.h - i, i, i, x + w - d, y + h - d, d, d);
    }

    /** The window frame plus its edge clasps: the menus' and minimap's frame. */
    static void window(Graphics2D g, int x, int y, int w, int h) {
        nine(g, Piece.WINDOW, x, y, w, h);
        if (w >= 200) {
            piece(g, Piece.CLASP, x + w / 2 - Piece.CLASP.w, y + S);
            piece(g, Piece.CLASP, x + w / 2 - Piece.CLASP.w, y + h - Piece.CLASP.h * S - S);
        }
        if (h >= 220) {
            piece(g, Piece.CLASP_V, x + S, y + h / 2 - Piece.CLASP_V.h);
            piece(g, Piece.CLASP_V, x + w - Piece.CLASP_V.w * S - S, y + h / 2 - Piece.CLASP_V.h);
        }
    }

    /** A kit piece at its natural size (2x). */
    static void piece(Graphics2D g, Piece p, int x, int y) {
        if (KIT == null) return;
        blit(g, p.x, p.y, p.w, p.h, x, y, p.w * S, p.h * S);
    }

    static void icon(Graphics2D g, Icon icon, int x, int y) {
        if (KIT == null) return;
        blit(g, icon.x(), icon.y(), Icon.CELL, Icon.CELL, x, y, Icon.CELL * S, Icon.CELL * S);
    }

    private static void blit(Graphics2D g, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) return;
        g.drawImage(KIT, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
    }

    private static void fallbackFrame(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(PANEL);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(0x3c3a56));
        g.drawRect(x, y, w - 1, h - 1);
    }

    // ------------------------------------------------------------------ text

    /** Width in screen pixels of `value` in pixel sans at `scale`. */
    static int width(String value, int scale) {
        return METRICS.getFontMetrics(sans(scale)).stringWidth(value);
    }

    static int displayWidth(String value, int scale) {
        return METRICS.getFontMetrics(display(scale)).stringWidth(value);
    }

    private static final Graphics2D METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

    /** Pixel sans with a one-art-pixel drop shadow; (x, y) is the left end of the baseline. */
    static void text(Graphics2D g, String value, int x, int y, int scale, Color color) {
        if (value == null || value.isEmpty()) return;
        crisp(g);
        g.setFont(sans(scale));
        g.setColor(SHADOW);
        g.drawString(value, x, y + scale);
        g.setColor(color);
        g.drawString(value, x, y);
    }

    static void textRight(Graphics2D g, String value, int right, int y, int scale, Color color) {
        text(g, value, right - width(value, scale), y, scale, color);
    }

    static void textCentered(Graphics2D g, String value, int center, int y, int scale, Color color) {
        text(g, value, center - width(value, scale) / 2 / S * S, y, scale, color);
    }

    /** Sans outlined on all four sides: for words floating over the world. */
    static void outlined(Graphics2D g, String value, int x, int y, int scale, Color color) {
        if (value == null || value.isEmpty()) return;
        crisp(g);
        g.setFont(sans(scale));
        g.setColor(INK);
        g.drawString(value, x - scale, y); g.drawString(value, x + scale, y);
        g.drawString(value, x, y - scale); g.drawString(value, x, y + scale); g.drawString(value, x + scale, y + scale);
        g.setColor(color);
        g.drawString(value, x, y);
    }

    static void outlinedCentered(Graphics2D g, String value, int center, int y, int scale, Color color) {
        outlined(g, value, center - width(value, scale) / 2 / S * S, y, scale, color);
    }

    /**
     * Words wrapped to `width`, one line every `spacing` pixels; returns the baseline after the last line.
     * Never splits a word; stops after `maxLines`, ending the last kept line with an ellipsis if cut.
     */
    static int wrapped(Graphics2D g, String value, int x, int y, int width, int scale, int spacing, int maxLines, Color color) {
        var lines = wrap(value, width, scale);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) line = line + "…";
            text(g, line, x, y, scale, color);
            y += spacing;
        }
        return y;
    }

    static java.util.List<String> wrap(String value, int width, int scale) {
        var lines = new java.util.ArrayList<String>();
        if (value == null || value.isBlank()) return lines;
        var fm = METRICS.getFontMetrics(sans(scale));
        StringBuilder line = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && fm.stringWidth(candidate) > width) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static final Map<String, BufferedImage> TITLES = new LinkedHashMap<>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) { return size() > 48; }
    };

    /**
     * A display-font title: banded gradient from `ramp`, one-pixel ink outline and a drop shadow, built once
     * at art resolution and scaled by `scale` (2, 3 or 4). (x, y) is the top-left of the outlined block.
     */
    static BufferedImage title(String value, int[] ramp) {
        String key = value + '\u0000' + System.identityHashCode(ramp);
        BufferedImage cached = TITLES.get(key);
        if (cached != null) return cached;
        Font font = display(1);
        var fm = METRICS.getFontMetrics(font);
        int w = Math.max(1, fm.stringWidth(value)) + 4, h = 11 + 2 + 4;
        var mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        var mg = mask.createGraphics();
        crisp(mg);
        mg.setFont(font);
        mg.setColor(Color.WHITE);
        mg.drawString(value, 2, 2 + 11);
        mg.dispose();
        var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int shadow = 0xff05060c, ink = 0xff0b0a14;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (lit(mask, x, y)) continue;
            if (lit(mask, x, y - 2) || lit(mask, x - 1, y - 2) || lit(mask, x + 1, y - 2)) out.setRGB(x, y, shadow);
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (lit(mask, x, y)) continue;
            if (lit(mask, x - 1, y) || lit(mask, x + 1, y) || lit(mask, x, y - 1) || lit(mask, x, y + 1)) out.setRGB(x, y, ink);
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (!lit(mask, x, y)) continue;
            int row = y - 2, band = row <= 1 ? 0 : row <= 4 ? 1 : row <= 7 ? 2 : 3;
            out.setRGB(x, y, 0xff000000 | ramp[Math.min(ramp.length - 1, band)]);
        }
        TITLES.put(key, out);
        return out;
    }

    private static boolean lit(BufferedImage mask, int x, int y) {
        return x >= 0 && y >= 0 && x < mask.getWidth() && y < mask.getHeight() && (mask.getRGB(x, y) >>> 24) > 0;
    }

    /** Draws a display title with its top-left at (x, y); returns its drawn width. */
    static int drawTitle(Graphics2D g, String value, int x, int y, int scale, int[] ramp) {
        var image = title(value, ramp);
        crisp(g);
        g.drawImage(image, x - 2 * scale, y - 2 * scale, image.getWidth() * scale, image.getHeight() * scale, null);
        return (image.getWidth() - 4) * scale;
    }

    static int drawTitleCentered(Graphics2D g, String value, int center, int y, int scale, int[] ramp) {
        int w = (title(value, ramp).getWidth() - 4) * scale;
        return drawTitle(g, value, center - w / 2 / scale * scale, y, scale, ramp);
    }

    static int titleWidth(String value, int scale) { return (title(value, GOLD_RAMP).getWidth() - 4) * scale; }

    // ------------------------------------------------------------------ widgets

    /** A key cap with its legend; returns its width. (x, y) is the top-left; height is 26. */
    static int keycap(Graphics2D g, String key, int x, int y) {
        int w = Math.max(26, width(key, 2) + 14);
        w = w / S * S;
        nine(g, Piece.KEYCAP, x, y, w, 28);
        crisp(g);
        g.setFont(sans(2));
        g.setColor(new Color(0x1a1622));
        g.drawString(key, x + (w - width(key, 2)) / 2 / S * S, y + 18);
        return w;
    }

    /** A gold hairline that fades at both ends, with a diamond at its centre. */
    static void divider(Graphics2D g, int center, int y, int half, Color color) {
        crisp(g);
        for (int dx = 0; dx <= half; dx += S) {
            double t = dx / (double) Math.max(1, half);
            if (t > .7 && ((dx / S) % 2 == 1 || t > .9 && (dx / S) % 4 != 0)) continue;
            g.setColor(color);
            g.fillRect(center + dx, y, S, S);
            g.fillRect(center - dx - S, y, S, S);
        }
        piece(g, Piece.DIAMOND, center - Piece.DIAMOND.w, y - Piece.DIAMOND.h + 1);
    }

    /** A bar on the 2x grid: dark trough, fill, one-pixel highlight along the fill's top. */
    static void bar(Graphics2D g, int x, int y, int w, int h, double fraction, Color fill, Color light) {
        g.setColor(INK);
        g.fillRect(x - S, y - S, w + 2 * S, h + 2 * S);
        g.setColor(new Color(0x1b1928));
        g.fillRect(x, y, w, h);
        int f = (int) Math.round(w * Math.max(0, Math.min(1, fraction))) / S * S;
        if (f <= 0) return;
        g.setColor(fill);
        g.fillRect(x, y, f, h);
        g.setColor(light);
        g.fillRect(x, y, f, S);
    }

    private static final Map<Long, BufferedImage> FADES = new java.util.HashMap<>();

    /**
     * A dithered dark plate fading horizontally from `from` (left) to `to` (right) opacity, 0..255:
     * pixel-art's answer to a soft gradient, so HUD text stays legible over bright ground.
     */
    static void fade(Graphics2D g, int x, int y, int w, int h, int from, int to) {
        int aw = Math.max(1, w / S), ah = Math.max(1, h / S);
        long key = ((long) aw << 40) | ((long) ah << 24) | ((long) from << 8) | to;
        BufferedImage plate = FADES.computeIfAbsent(key, k -> {
            var img = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_ARGB);
            int[] bayer = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
            for (int py = 0; py < ah; py++) for (int px = 0; px < aw; px++) {
                double t = aw <= 1 ? 0 : px / (double) (aw - 1);
                double a = from + (to - from) * t;
                int step = (int) (a / 64);                       // four opacity steps, dithered between
                double frac = a / 64 - step;
                if (frac * 16 > bayer[(py & 3) * 4 + (px & 3)]) step++;
                int alpha = Math.min(255, step * 64);
                if (alpha > 0) img.setRGB(px, py, (alpha << 24) | 0x05070d);
            }
            return img;
        });
        crisp(g);
        g.drawImage(plate, x, y, aw * S, ah * S, null);
    }

    /** Darkens the whole screen: the backdrop behind menus and cards. */
    static void veil(Graphics2D g, int w, int h, int alpha) {
        var c = (Graphics2D) g.create();
        c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha / 255f))));
        c.setColor(new Color(0x05070d));
        c.fillRect(0, 0, w, h);
        c.dispose();
    }

    /** Roman numeral for chapter cards (1-10). */
    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : Integer.toString(n);
    }
}
