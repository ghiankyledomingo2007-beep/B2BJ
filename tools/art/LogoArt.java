/**
 * The title logo (assets/ui/title/logo.png): BLOB in glossy dripping goo, TO in gold, BLADE in chiselled
 * steel resting on a sword. The word itself tells the game: the slime above becomes the blade below.
 */
final class LogoArt {
    private LogoArt() { }

    static final int W = 214, H = 108;
    private static final int MASK = 0xffffffff;

    static Canvas logo() {
        Canvas out = new Canvas(W, H);
        Canvas goo = new Canvas(W, H);
        int x = 8;
        x = gooB(goo, x, 6) + 5;
        x = gooL(goo, x, 6) + 5;
        x = gooO(goo, x, 6) + 5;
        gooB(goo, x, 6);
        drip(goo, 15, 34, 6); drip(goo, 47, 34, 4); drip(goo, 74, 34, 8); drip(goo, 104, 34, 5);
        shadeGoo(goo);
        goo.outline(UiArt.INK);
        out.blit(goo, 0, 0);

        Canvas steel = new Canvas(W, H);
        int bx = 8, by = 54;
        bx = steelB(steel, bx, by) + 4;
        bx = steelL(steel, bx, by) + 4;
        bx = steelA(steel, bx, by) + 4;
        bx = steelD(steel, bx, by) + 4;
        steelE(steel, bx, by);
        shadeSteel(steel, by, 26);
        steel.outline(UiArt.INK);
        Canvas sword = sword();
        out.blit(sword, 0, 0);
        out.blit(steel, 0, 0);
        to(out, 132, 36);
        return out;
    }

    // ------------------------------------------------------------------ goo letters (28 px tall)

    /** A round brush stroke: every pixel within r of the segment. */
    static void stroke(Canvas c, double x0, double y0, double x1, double y1, double r) {
        int steps = (int) Math.ceil(Math.hypot(x1 - x0, y1 - y0) * 2) + 1;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            c.ellipse(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r, r, MASK);
        }
    }

    static void arc(Canvas c, double cx, double cy, double rx, double ry, double from, double to, double r) {
        int steps = (int) Math.ceil(Math.abs(to - from) * Math.max(rx, ry) * 2) + 1;
        for (int i = 0; i <= steps; i++) {
            double a = from + (to - from) * i / steps;
            c.ellipse(cx + Math.cos(a) * rx, cy + Math.sin(a) * ry, r, r, MASK);
        }
    }

    static int gooB(Canvas c, int x, int y) {
        double r = 3.6;
        stroke(c, x + 3.5, y + 3.5, x + 3.5, y + 24.5, r);
        stroke(c, x + 3.5, y + 3.5, x + 12, y + 3.5, r);
        arc(c, x + 12, y + 8.5, 6.5, 5, -Math.PI / 2, Math.PI / 2, r);
        stroke(c, x + 3.5, y + 13.5, x + 13, y + 13.5, r);
        arc(c, x + 13, y + 19, 7.5, 5.5, -Math.PI / 2, Math.PI / 2, r);
        stroke(c, x + 3.5, y + 24.5, x + 13, y + 24.5, r);
        return x + 24;
    }

    static int gooL(Canvas c, int x, int y) {
        double r = 3.6;
        stroke(c, x + 3.5, y + 3.5, x + 3.5, y + 24.5, r);
        stroke(c, x + 3.5, y + 24.5, x + 17, y + 24.5, r);
        return x + 20;
    }

    static int gooO(Canvas c, int x, int y) {
        arc(c, x + 12, y + 14, 8.5, 10.5, 0, Math.PI * 2, 3.6);
        return x + 24;
    }

    /** A drop hanging from the letters' bottom edge: a neck and a round bead. */
    static void drip(Canvas c, int x, int y, int length) {
        stroke(c, x, y - 2, x, y + length - 2, 1.2);
        c.ellipse(x, y + length - 1, 2.2, 2.6, MASK);
    }

    /** Glossy goo: lit from the top left, a cool core, a dark lower rim, white specular glints. */
    static void shadeGoo(Canvas c) {
        int[] cyan = Pal.CYAN;
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            if (!c.opaque(x, y)) continue;
            boolean up = !c.opaque(x, y - 1), up2 = !c.opaque(x, y - 2), left = !c.opaque(x - 1, y);
            boolean down = !c.opaque(x, y + 1), down2 = !c.opaque(x, y + 2), right = !c.opaque(x + 1, y);
            int colour = cyan[3];
            if (down) colour = cyan[1];
            else if (down2 || right) colour = cyan[2];
            else if (up) colour = cyan[5];
            else if (up2 || left) colour = cyan[4];
            c.set(x, y, colour);
        }
        // glints: one bright pixel pair a little inside each top-left curve
        for (int y = 1; y < c.h - 1; y++) for (int x = 1; x < c.w - 1; x++) {
            if (c.get(x, y) != cyan[4]) continue;
            if (!c.opaque(x - 1, y - 2) && !c.opaque(x - 2, y - 1) && (x * 7 + y * 3) % 11 == 0) {
                c.set(x, y, 0xffffffff);
                c.set(x + 1, y, cyan[5]);
            }
        }
    }

    // ------------------------------------------------------------------ steel letters (26 px tall)

    static void box(Canvas c, int x0, int y0, int x1, int y1) { c.rect(x0, y0, x1 - x0 + 1, y1 - y0 + 1, MASK); }

    /** Clears a triangle off a corner: chamfers that make the letters cut, not printed. */
    static void chamfer(Canvas c, int cx, int cy, int size, int dx, int dy) {
        for (int i = 0; i < size; i++) for (int j = 0; j < size - i; j++) c.clear(cx + dx * i, cy + dy * j);
    }

    static int steelB(Canvas c, int x, int y) {
        box(c, x, y, x + 5, y + 25);
        box(c, x, y, x + 15, y + 4);
        box(c, x, y + 11, x + 15, y + 14);
        box(c, x, y + 21, x + 17, y + 25);
        box(c, x + 12, y, x + 16, y + 13);
        box(c, x + 13, y + 12, x + 18, y + 25);
        chamfer(c, x + 16, y, 4, -1, 1);
        chamfer(c, x + 16, y + 13, 3, -1, -1);
        chamfer(c, x + 18, y + 25, 5, -1, -1);
        chamfer(c, x + 18, y + 12, 3, -1, 1);
        box(c, x + 6, y + 5, x + 11, y + 10); clearBox(c, x + 6, y + 5, x + 11, y + 10);
        clearBox(c, x + 6, y + 15, x + 12, y + 20);
        return x + 19;
    }

    static void clearBox(Canvas c, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) c.clear(x, y);
    }

    static int steelL(Canvas c, int x, int y) {
        box(c, x, y, x + 5, y + 25);
        box(c, x, y + 21, x + 14, y + 25);
        for (int i = 0; i < 5; i++) for (int j = 0; j <= i; j++) c.set(x + 15 + j, y + 21 + i, MASK);   // the foot ends in a point
        for (int i = 0; i < 3; i++) c.set(x + 21 - i, y + 25 - 2 + i, 0);
        chamfer(c, x, y, 3, 1, 1);
        return x + 20;
    }

    static int steelA(Canvas c, int x, int y) {
        int w = 22;
        for (int row = 0; row <= 25; row++) {
            double half = 2.5 + row * (w / 2.0 - 2.5) / 25.0;
            int l = (int) Math.round(x + w / 2.0 - half), r = (int) Math.round(x + w / 2.0 + half) - 1;
            double inner = half - 6.0;
            for (int px = l; px <= r; px++) {
                boolean hollow = row > 5 && inner > .6 && Math.abs(px + .5 - (x + w / 2.0)) < inner && !(row >= 15 && row <= 18);
                if (!hollow) c.set(px, y + row, MASK);
            }
        }
        return x + w;
    }

    static int steelD(Canvas c, int x, int y) {
        box(c, x, y, x + 5, y + 25);
        box(c, x, y, x + 13, y + 4);
        box(c, x, y + 21, x + 13, y + 25);
        box(c, x + 13, y + 3, x + 18, y + 22);
        box(c, x + 10, y + 1, x + 15, y + 7);
        box(c, x + 10, y + 18, x + 15, y + 24);
        chamfer(c, x + 18, y + 3, 4, -1, 1);
        chamfer(c, x + 18, y + 22, 4, -1, -1);
        chamfer(c, x + 15, y, 3, -1, 1);
        chamfer(c, x + 15, y + 25, 3, -1, -1);
        clearBox(c, x + 6, y + 5, x + 12, y + 20);
        return x + 19;
    }

    static int steelE(Canvas c, int x, int y) {
        box(c, x, y, x + 5, y + 25);
        int[][] arms = {{0, 4, 16}, {11, 14, 12}, {21, 25, 16}};
        for (int[] a : arms) {
            box(c, x, y + a[0], x + a[2], y + a[1]);
            int h = a[1] - a[0] + 1;
            for (int i = 0; i < h; i++) for (int j = 0; j < Math.min(i, h - 1 - i) + 1; j++) c.set(x + a[2] + 1 + j, y + a[0] + i, MASK);
        }
        return x + 20;
    }

    /** Bevelled steel: banded from bright top to dark base, a white edge where light catches, gold rim. */
    static void shadeSteel(Canvas c, int top, int height) {
        int[] ramp = {0xffffffff, Pal.c(0xcfd6e2), Pal.IRON[5], Pal.IRON[4], Pal.IRON[3], Pal.IRON[2]};
        Canvas copy = c.copy();
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            if (!copy.opaque(x, y)) continue;
            double t = (y - top) / (double) height;
            int band = t < .12 ? 1 : t < .42 ? 2 : t < .5 ? 0 : t < .75 ? 3 : t < .9 ? 4 : 5;
            int colour = ramp[band];
            boolean edgeTop = !copy.opaque(x, y - 1), edgeLeft = !copy.opaque(x - 1, y);
            boolean edgeBottom = !copy.opaque(x, y + 1), edgeRight = !copy.opaque(x + 1, y);
            if (edgeTop || edgeLeft) colour = Pal.GOLD[4];
            else if (edgeBottom || edgeRight) colour = Pal.GOLD[1];
            c.set(x, y, colour);
        }
    }

    // ------------------------------------------------------------------ sword and TO

    /** The sword the letters rest on: gold pommel, wrapped grip, cross-guard, a long blade with ichor runes. */
    static Canvas sword() {
        Canvas c = new Canvas(W, H);
        int y = 86;
        for (int x = 26; x < W - 8; x++) {
            int half = x > W - 26 ? Math.max(0, (W - 8 - x) * 3 / 18) : 3;
            for (int dy = -half; dy <= half; dy++) {
                int colour = dy < -1 ? Pal.c(0xcfd6e2) : dy == -1 ? 0xffffffff : dy == 0 ? Pal.IRON[5] : dy == 1 ? Pal.IRON[4] : Pal.IRON[3];
                c.set(x, y + dy, colour);
            }
        }
        for (int x = 40; x < W - 40; x += 13) { c.set(x, y, Pal.CYAN[4]); c.set(x + 1, y, Pal.CYAN[3]); }
        for (int dy = -7; dy <= 7; dy++) for (int dx = 0; dx < 4; dx++)
            c.set(20 + dx, y + dy, Math.abs(dy) == 7 ? Pal.GOLD[2] : dx == 0 ? Pal.GOLD[4] : dx == 3 ? Pal.GOLD[1] : Pal.GOLD[3]);
        c.set(21, y, Pal.CYAN[4]); c.set(22, y, Pal.CYAN[3]);
        for (int x = 8; x < 20; x++) for (int dy = -2; dy <= 2; dy++)
            c.set(x, y + dy, ((x + dy) & 1) == 0 ? Pal.c(0x6d4941) : Pal.c(0x996f5d));
        c.ellipse(5.5, y, 3.2, 3.2, Pal.GOLD[3]);
        c.set(4, y - 1, Pal.GOLD[5]); c.set(6, y + 1, Pal.GOLD[1]);
        c.outline(UiArt.INK);
        return c;
    }

    static void to(Canvas c, int x, int y) {
        String[] t = {
            "#######..#####.",
            "#######.#######",
            "..##....##...##",
            "..##....##...##",
            "..##....##...##",
            "..##....##...##",
            "..##....##...##",
            "..##....#######",
            "..##.....#####."};
        for (int row = 0; row < t.length; row++) for (int col = 0; col < t[row].length(); col++)
            if (t[row].charAt(col) == '#') c.set(x + col, y + row, row < 3 ? Pal.GOLD[4] : row < 6 ? Pal.GOLD[3] : Pal.GOLD[2]);
        for (int side = -1; side <= 1; side += 2) {
            int cx = side < 0 ? x - 8 : x + t[0].length() + 7;
            for (int d = 0; d <= 2; d++) { c.set(cx - d, y + 4, Pal.GOLD[2]); c.set(cx + d, y + 4, Pal.GOLD[2]); }
            c.set(cx, y + 3, Pal.GOLD[4]); c.set(cx, y + 5, Pal.GOLD[1]);
            for (int d = 4; d < 16; d += 2) c.set(side < 0 ? cx - d : cx + d, y + 4, Pal.GOLD[1]);
        }
        Canvas mark = new Canvas(c.w, c.h);
        for (int yy = y - 1; yy <= y + t.length; yy++) for (int xx = x - 26; xx <= x + t[0].length() + 25; xx++)
            if (c.opaque(xx, yy)) mark.set(xx, yy, c.get(xx, yy));
        mark.outline(UiArt.INK);
        c.blit(mark, 0, 0);
    }
}
