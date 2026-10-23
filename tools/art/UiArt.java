/**
 * The pixel UI kit atlas (assets/ui/kit.png): window and panel frames, buttons, key caps, check boxes,
 * slider parts, boss-bar parts, emblems, cursor, trims and icons. Every rectangle comes from
 * {@code Ui.Piece} / {@code Ui.Icon}, so the game slices exactly what is drawn here.
 * Lighting is from the top left: upper and left bevels catch light, lower and right fall into shadow.
 */
final class UiArt {
    private UiArt() { }

    static final int INK = Pal.c(0x0b0a14), DEEP = Pal.c(0x06080d);
    static final int[] METAL = {Pal.c(0x100e1c), Pal.c(0x18172a), Pal.c(0x252037), Pal.c(0x3c3a56), Pal.c(0x635e7c), Pal.c(0x8f8bb0)};
    static final int[] NAVY = {Pal.c(0x070a11), Pal.c(0x0a0e17), Pal.c(0x0c111b), Pal.c(0x111827), Pal.c(0x172033)};
    static final int[] GEM = {Pal.c(0x0f2a2a), Pal.c(0x1f4a48), Pal.c(0x3a6b68), Pal.c(0x5fa39c), Pal.c(0x96cec6), Pal.c(0xdffffe)};
    static final int[] BRONZE = {Pal.c(0x2b1c18), Pal.c(0x503330), Pal.c(0x6d4941), Pal.c(0x996f5d), Pal.c(0xb48a67), Pal.c(0xcbaa6f), Pal.c(0xddc69a)};

    static Canvas kit() {
        Canvas c = new Canvas(256, 192);
        window(c, Ui.Piece.WINDOW);
        frame(c, Ui.Piece.PANEL, new int[]{INK, METAL[4], METAL[2]}, new int[]{INK, METAL[1], METAL[2]}, NAVY[2], 1);
        frame(c, Ui.Piece.CARD, new int[]{INK, BRONZE[5], BRONZE[3]}, new int[]{INK, BRONZE[2], BRONZE[3]}, NAVY[2], 1);
        frame(c, Ui.Piece.INSET, new int[]{INK, DEEP, NAVY[0]}, new int[]{METAL[3], METAL[2], NAVY[1]}, NAVY[1], 0);
        button(c, Ui.Piece.BUTTON, new int[]{INK, METAL[4], METAL[2]}, new int[]{INK, METAL[0], METAL[1]}, METAL[1], METAL[2]);
        button(c, Ui.Piece.BUTTON_HOT, new int[]{INK, BRONZE[6], METAL[3]}, new int[]{INK, BRONZE[3], METAL[2]}, METAL[2], METAL[3]);
        button(c, Ui.Piece.BUTTON_DOWN, new int[]{INK, METAL[0], METAL[0]}, new int[]{INK, METAL[3], METAL[1]}, Pal.c(0x120f20), Pal.c(0x120f20));
        button(c, Ui.Piece.BUTTON_OFF, new int[]{INK, METAL[2], METAL[1]}, new int[]{INK, METAL[1], METAL[1]}, Pal.c(0x121020), Pal.c(0x121020));
        frame(c, Ui.Piece.TAG, new int[]{INK, Pal.RUST[4]}, new int[]{INK, Pal.RUST[2]}, Pal.c(0x1a120c), 2);
        keycap(c, Ui.Piece.KEYCAP);
        check(c, Ui.Piece.CHECK, false, false);
        check(c, Ui.Piece.CHECK_ON, true, false);
        check(c, Ui.Piece.CHECK_HOT, false, true);
        check(c, Ui.Piece.CHECK_HOT_ON, true, true);
        track(c, Ui.Piece.TRACK, new int[]{INK, NAVY[0], NAVY[1], NAVY[1], METAL[2], INK});
        track(c, Ui.Piece.TRACK_FILL, new int[]{INK, Pal.CYAN[4], Pal.CYAN[3], Pal.CYAN[3], Pal.CYAN[2], INK});
        knob(c, Ui.Piece.KNOB, METAL[4], METAL[1]);
        knob(c, Ui.Piece.KNOB_HOT, BRONZE[6], BRONZE[3]);
        boss(c);
        UiIcons.put(c, Ui.Piece.EMBLEM_WARDEN.x, Ui.Piece.EMBLEM_WARDEN.y, "warden", UiIcons.WARDEN);
        UiIcons.put(c, Ui.Piece.EMBLEM_BRIAR.x, Ui.Piece.EMBLEM_BRIAR.y, "briar", UiIcons.BRIAR);
        UiIcons.put(c, Ui.Piece.CURSOR.x, Ui.Piece.CURSOR.y, "cursor", UiIcons.CURSOR);
        UiIcons.put(c, Ui.Piece.CURSOR_2.x, Ui.Piece.CURSOR_2.y, "cursor2", UiIcons.CURSOR_GLINT);
        UiIcons.put(c, Ui.Piece.CLASP.x, Ui.Piece.CLASP.y, "clasp", UiIcons.CLASP);
        UiIcons.put(c, Ui.Piece.CLASP_V.x, Ui.Piece.CLASP_V.y, "claspV", UiIcons.transpose(UiIcons.CLASP));
        UiIcons.put(c, Ui.Piece.DIAMOND.x, Ui.Piece.DIAMOND.y, "diamond", UiIcons.DIAMOND);
        ornament(c, Ui.Piece.ORNAMENT);
        frame(c, Ui.Piece.SCROLL_THUMB, new int[]{INK, METAL[4]}, new int[]{INK, METAL[2]}, METAL[3], 1);
        UiIcons.put(c, Ui.Piece.MOUSE_L.x, Ui.Piece.MOUSE_L.y, "mouseL", UiIcons.MOUSE_L);
        UiIcons.put(c, Ui.Piece.MOUSE_R.x, Ui.Piece.MOUSE_R.y, "mouseR", UiIcons.mirror(UiIcons.MOUSE_L));
        String[][] icons = UiIcons.icons();
        if (icons.length != Ui.Icon.values().length) throw new IllegalStateException("icon maps do not match Ui.Icon");
        for (var icon : Ui.Icon.values()) UiIcons.put(c, icon.x(), icon.y(), icon.name(), icons[icon.ordinal()]);
        flourish(c, Ui.Piece.FLOURISH);
        return c;
    }

    /**
     * A bevelled frame filling the piece: `light` bands (outside in) on the top and left, `shade` bands
     * on the bottom and right, `fill` inside. `round` clears that many pixels off each outer corner.
     */
    static void frame(Canvas c, Ui.Piece p, int[] light, int[] shade, int fill, int round) {
        int t = light.length;
        for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) {
            int top = y, left = x, bottom = p.h - 1 - y, right = p.w - 1 - x;
            int d = Math.min(Math.min(top, left), Math.min(bottom, right));
            int colour;
            if (d >= t) colour = fill;
            else if ((top == d && top <= right) || (left == d && left <= bottom)) colour = light[d];
            else colour = shade[d];
            c.set(p.x + x, p.y + y, colour);
        }
        for (int r = 0; r < round; r++) for (int k = 0; k <= r; k++) {
            int a = r - k;
            c.clear(p.x + a, p.y + k); c.clear(p.x + p.w - 1 - a, p.y + k);
            c.clear(p.x + a, p.y + p.h - 1 - k); c.clear(p.x + p.w - 1 - a, p.y + p.h - 1 - k);
        }
    }

    static void window(Canvas c, Ui.Piece p) {
        frame(c, p, new int[]{INK, METAL[4], METAL[3], METAL[2], METAL[0]},
                new int[]{INK, METAL[1], METAL[2], METAL[2], METAL[0]}, NAVY[2], 0);
        for (int x = 5; x < p.w - 5; x++) { c.set(p.x + x, p.y + 5, NAVY[0]); c.set(p.x + x, p.y + 6, NAVY[1]); }
        socket(c, p.x, p.y);
        socket(c, p.x + p.w - 12, p.y);
        socket(c, p.x, p.y + p.h - 12);
        socket(c, p.x + p.w - 12, p.y + p.h - 12);
    }

    /** A 12x12 corner plate holding a round teal gem. */
    static void socket(Canvas c, int ox, int oy) {
        for (int y = 0; y < 12; y++) for (int x = 0; x < 12; x++) {
            int colour = METAL[2];
            if (x == 0 || y == 0 || x == 11 || y == 11) colour = INK;
            else if (y == 1 || x == 1) colour = METAL[4];
            else if (y == 10 || x == 10) colour = METAL[1];
            c.set(ox + x, oy + y, colour);
        }
        for (int y = 2; y <= 9; y++) for (int x = 2; x <= 9; x++) {
            double dx = x - 5.5, dy = y - 5.5, r2 = dx * dx + dy * dy;
            int colour;
            if (r2 > 3.3 * 3.3) colour = (x == 2 || y == 2) ? METAL[0] : METAL[1];
            else if (r2 > 2.6 * 2.6) colour = dx + dy < 0 ? GEM[1] : INK;
            else if (dx + dy < -2.2) colour = GEM[4];
            else if (dx + dy > 1.6) colour = GEM[1];
            else colour = GEM[2];
            c.set(ox + x, oy + y, colour);
        }
        c.set(ox + 4, oy + 4, GEM[5]);
        c.set(ox + 5, oy + 4, GEM[4]);
        c.set(ox + 4, oy + 5, GEM[4]);
    }

    static void button(Canvas c, Ui.Piece p, int[] light, int[] shade, int fill, int lip) {
        frame(c, p, light, shade, fill, 1);
        for (int x = 3; x < p.w - 3; x++) c.set(p.x + x, p.y + 3, lip);
    }

    static void keycap(Canvas c, Ui.Piece p) {
        int hi = Pal.c(0xd5dadb), face = Pal.c(0xaab2b6), rim = Pal.c(0x8b9398), side = Pal.c(0x6c767e), sideDark = Pal.c(0x4a535d);
        for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) {
            int colour;
            boolean edge = x == 0 || x == p.w - 1;
            if (y == 0 || y == p.h - 1 || edge) colour = INK;
            else if (y == 1) colour = hi;
            else if (y <= 9) colour = x == 1 ? hi : x == p.w - 2 ? rim : face;
            else if (y <= 11) colour = side;
            else colour = sideDark;
            c.set(p.x + x, p.y + y, colour);
        }
        c.clear(p.x, p.y); c.clear(p.x + p.w - 1, p.y); c.clear(p.x, p.y + p.h - 1); c.clear(p.x + p.w - 1, p.y + p.h - 1);
    }

    static void check(Canvas c, Ui.Piece p, boolean on, boolean hot) {
        frame(c, p, new int[]{INK, hot ? BRONZE[6] : METAL[3], DEEP}, new int[]{INK, hot ? BRONZE[3] : METAL[1], NAVY[0]}, NAVY[0], 1);
        if (!on) return;
        int[][] mark = {{2, 5}, {3, 6}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}};
        for (int[] m : mark) {
            c.set(p.x + m[0], p.y + m[1], Pal.CYAN[4]);
            c.set(p.x + m[0], p.y + m[1] + 1, Pal.CYAN[2]);
        }
        c.set(p.x + 8, p.y + 3, Pal.CYAN[5]);
    }

    static void track(Canvas c, Ui.Piece p, int[] rows) {
        for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) {
            int colour = (x == 0 || x == p.w - 1) ? INK : rows[y];
            c.set(p.x + x, p.y + y, colour);
        }
        c.clear(p.x, p.y); c.clear(p.x + p.w - 1, p.y); c.clear(p.x, p.y + p.h - 1); c.clear(p.x + p.w - 1, p.y + p.h - 1);
    }

    static void knob(Canvas c, Ui.Piece p, int light, int shade) {
        frame(c, p, new int[]{INK, light}, new int[]{INK, shade}, METAL[3], 1);
        c.set(p.x + 3, p.y + 4, GEM[4]); c.set(p.x + 3, p.y + 5, GEM[2]); c.set(p.x + 3, p.y + 6, GEM[1]);
        c.set(p.x + 2, p.y + 5, GEM[3]); c.set(p.x + 4, p.y + 5, GEM[1]);
    }

    /** Boss bar: emblem socket on the left, rails around a trough the game fills, a finial on the right. */
    static void boss(Canvas c) {
        var mid = Ui.Piece.BOSS_MID;
        rails(c, mid.x, mid.y, mid.w);
        c.set(mid.x + 3, mid.y + 7, METAL[5]);
        var left = Ui.Piece.BOSS_LEFT;
        rails(c, left.x + 12, left.y, left.w - 12);
        for (int y = 9; y <= 15; y++) c.set(left.x + 22, left.y + y, INK);
        for (int y = 0; y < 24; y++) for (int x = 0; x < 24; x++) {
            double dx = x - 11.5, dy = y - 11.5, r = Math.sqrt(dx * dx + dy * dy);
            int colour;
            if (r > 11.6) continue;
            else if (r > 10.7) colour = INK;
            else if (r > 9.0) colour = dx + dy < -2 ? BRONZE[6] : dx + dy > 2 ? BRONZE[2] : BRONZE[4];
            else if (r > 8.2) colour = INK;
            else colour = r < 7 && dx + dy < -5 ? METAL[2] : METAL[1];
            c.set(left.x + x, left.y + y, colour);
        }
        var right = Ui.Piece.BOSS_RIGHT;
        rails(c, right.x, right.y, 4);
        for (int y = 9; y <= 15; y++) c.set(right.x, right.y + y, INK);
        for (int y = 4; y <= 20; y++) for (int x = 1; x <= 13; x++) {
            double t = Math.abs(y - 12) / 8.0;
            if (x - 1 > (1 - t) * 12) continue;
            boolean border = x - 1 > (1 - t) * 12 - 1.2 || y == 4 || y == 20 || x == 1;
            int colour = border ? INK : y < 12 ? (x < 5 ? BRONZE[6] : BRONZE[5]) : (x < 5 ? BRONZE[4] : BRONZE[3]);
            c.set(right.x + x, right.y + y, colour);
        }
        c.set(right.x + 4, right.y + 12, GEM[5]); c.set(right.x + 5, right.y + 12, GEM[3]); c.set(right.x + 4, right.y + 11, GEM[4]);
    }

    static void rails(Canvas c, int ox, int oy, int w) {
        for (int x = 0; x < w; x++) {
            c.set(ox + x, oy + 6, INK); c.set(ox + x, oy + 7, METAL[4]); c.set(ox + x, oy + 8, METAL[3]);
            c.set(ox + x, oy + 16, METAL[2]); c.set(ox + x, oy + 17, METAL[1]); c.set(ox + x, oy + 18, INK);
        }
    }

    static void ornament(Canvas c, Ui.Piece p) {
        int cx = p.x + p.w / 2, cy = p.y + p.h / 2;
        for (int x = 0; x < p.w; x++) if (Math.abs(x - p.w / 2) > 4 && (Math.abs(x - p.w / 2) < 9 || x % 2 == 0))
            c.set(p.x + x, cy, Pal.GOLD[2]);
        UiIcons.put(c, cx - 3, cy - 3, "ornament", UiIcons.DIAMOND);
    }

    /**
     * The boot splash (assets/ui/splash.png). The JVM shows it at its own size before the game loads, so
     * it is drawn at art size and doubled here: 640x360 screen pixels. The game paints the load bar into
     * the empty strip at the bottom (see Splash.java).
     */
    static Canvas splash() {
        int w = 320, h = 180;
        Canvas c = new Canvas(w, h);
        int[] ramp = {Pal.c(0x04060c),Pal.c(0x060912),Pal.c(0x080c19),Pal.c(0x0b1121),Pal.c(0x0f172c),
                Pal.c(0x141f39),Pal.c(0x1a2848),Pal.c(0x213258),Pal.c(0x293d66)};
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double d = Math.hypot((x - w / 2.0) / w, (y - h * .42) / h);
            double at = 3.2 - d * 5.5;
            at = Math.max(0, Math.min(ramp.length - 1, at));
            int base = (int) at;
            c.set(x, y, ramp[Math.min(ramp.length - 1, base + ((at - base) > Noise.bayer(x, y) ? 1 : 0))]);
        }
        for (int i = 0; i < 70; i++) {
            int x = Noise.hash(i, 1, 3) & 0xff, y = Noise.hash(i, 2, 3) % 120;
            if (x < w && y >= 0) c.set(x + 32, y, Noise.rnd(i, 3, 3) < .3 ? Pal.c(0xdfe8ff) : Pal.STONE[6]);
        }
        Canvas logo = LogoArt.logo();
        c.blit(logo, (w - LogoArt.W) / 2, 22);
        for (int x = 85; x < 235; x++) { c.set(x, 160, INK); c.set(x, 163, INK); }
        for (int y = 160; y <= 163; y++) { c.set(84, y, INK); c.set(235, y, INK); }
        for (int x = 85; x < 235; x++) for (int y = 161; y <= 162; y++) c.set(x, y, NAVY[1]);
        Canvas big = new Canvas(w * 2, h * 2);
        for (int y = 0; y < h * 2; y++) for (int x = 0; x < w * 2; x++) big.set(x, y, c.get(x / 2, y / 2));
        return big;
    }

    /** Area-card filigree: a medallion with gold lines that curl and fade toward both ends. */
    static void flourish(Canvas c, Ui.Piece p) {
        int cx = p.x + p.w / 2, cy = p.y + p.h / 2;
        int lineY = cy;
        for (int dx = 9; dx < p.w / 2; dx++) {
            double t = dx / (double) (p.w / 2);
            if (t > .75 && dx % 2 == 1) continue;
            if (t > .9 && dx % 4 != 0) continue;
            int colour = t < .45 ? Pal.GOLD[3] : t < .7 ? Pal.GOLD[2] : Pal.GOLD[1];
            c.set(cx + dx, lineY, colour); c.set(cx - dx - 1, lineY, colour);
            if (t < .45) { c.set(cx + dx, lineY + 1, Pal.GOLD[1]); c.set(cx - dx - 1, lineY + 1, Pal.GOLD[1]); }
        }
        for (int side = -1; side <= 1; side += 2) {
            int ox = cx + side * 30;
            for (double a = 0; a < Math.PI * 1.6; a += .08) {
                double r = 2 + a * 1.3;
                int x = ox + (int) Math.round(side * Math.cos(a + Math.PI / 2) * r), y = lineY - (int) Math.round(Math.sin(a + Math.PI / 2) * r * .8);
                if (y < lineY) c.set(x, y, Pal.GOLD[3]);
            }
            c.set(cx + side * 46, lineY - 2, Pal.GOLD[4]); c.set(cx + side * 46, lineY + 2, Pal.GOLD[2]);
            c.set(cx + side * 58, lineY - 1, Pal.GOLD[3]);
        }
        for (int y = -7; y <= 7; y++) for (int x = -8; x <= 8; x++) {
            double r = Math.sqrt(x * x + y * y);
            if (r > 7.5) continue;
            int colour;
            if (r > 6.6) colour = INK;
            else if (r > 5.4) colour = x + y < 0 ? Pal.GOLD[4] : Pal.GOLD[2];
            else if (r > 4.6) colour = INK;
            else colour = NAVY[3];
            c.set(cx + x, cy + y, colour);
        }
        UiIcons.put(c, cx - 3, cy - 3, "medallion", UiIcons.DIAMOND);
    }
}
