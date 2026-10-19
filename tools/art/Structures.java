/**
 * Landmark set pieces at character scale (a person is ~64 native px tall). Each sheet's cell width
 * and anchor come from SetPieces.Kind, so art, renderer and collision share one set of numbers.
 * Convention: row `ground` = anchorY - 1 is the front edge of the footprint.
 */
final class Structures {
    private Structures() { }

    static Canvas sheet(SetPieces.Kind kind) {
        return switch (kind) {
            case TENT -> tent();
            case CAMPFIRE -> campfire();
            case LAMP -> Canvas.strip(lamp(0), lamp(1));
            case LOG_SEAT -> Canvas.strip(logSeat(0), logSeat(1));
            case CART -> Canvas.strip(cart(0), cart(1));
            case SIGNPOST -> signpost();
            case BANNER -> { Canvas[] f = new Canvas[6]; for (int i = 0; i < 6; i++) f[i] = banner(i, 6); yield Canvas.strip(f); }
            case RACK -> Canvas.strip(rack(0), rack(1));
            case DUMMY -> Canvas.strip(dummy(0), dummy(1));
            case GATE_POST -> Canvas.strip(gatePost(false), gatePost(true));
            case BARRACKS -> Buildings.barracks();
            case WATCHTOWER -> Buildings.watchtower();
            case STANDARD -> Buildings.standard();
            case STATUE -> Buildings.statue();
            case BRAZIER -> brazier();
            case COLUMN -> Canvas.strip(column(0), column(1), column(2));
            case EAST_GATE -> Canvas.strip(Buildings.eastGate(false), Buildings.eastGate(true));
            case SIGNAL_TOWER -> Buildings.signalTower();
            case INFIRMARY -> Buildings.infirmary();
            case SUPPLIES -> Canvas.strip(Props.supplies(0), Props.supplies(1), Props.supplies(2), Props.supplies(3));
            default -> ForestArt.sheet(kind);
        };
    }

    static Canvas canvas(SetPieces.Kind k, int extraBelow) { return new Canvas(k.width, k.anchorY + extraBelow); }

    /** Upright timber, `w` wide, lit from the left. */
    static void post(Canvas c, int x, int top, int bottom, int w) {
        int[] shade = w >= 6 ? new int[]{Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[5], Pal.WOOD[4], Pal.WOOD[3], Pal.WOOD[2], Pal.WOOD[1]}
                : new int[]{Pal.WOOD[4], Pal.WOOD[4], Pal.WOOD[3], Pal.WOOD[2], Pal.WOOD[1]};
        for (int y = top; y <= bottom; y++) for (int i = 0; i < w; i++) {
            int colour = shade[Math.min(shade.length - 1, i * shade.length / w)];
            if (Noise.rnd(x + i, y / 3, 707) < .05) colour = Pal.shift(colour, -1);
            c.set(x + i, y, colour);
        }
    }

    // ------------------------------------------------------------------ captain's camp
    static Canvas tent() {
        var k = SetPieces.Kind.TENT;
        Canvas c = canvas(k, 4);
        int g = k.anchorY - 1, ax = k.anchorX;
        int l = ax - 46, r = ax + 46, apexY = g - 62, back = 26;
        Kit.poly(c, Pal.CANVAS[4], l, g, ax, apexY, ax, apexY - back, l, g - back);
        Kit.poly(c, Pal.CANVAS[2], r, g, ax, apexY, ax, apexY - back, r, g - back);
        for (int f = 1; f <= 6; f++) {          // folds run down each slope
            int fx = ax - f * 7;
            int yTop = apexY - back + (int) Math.round((ax - fx) * 62.0 / 46), yBot = yTop + back;
            c.line(fx, yTop, fx, Math.min(yBot, g - (int) Math.round((ax - fx) * 0.0)), Pal.CANVAS[3]);
            c.line(fx - 1, yTop + 1, fx - 1, Math.min(yBot, g), Pal.CANVAS[5]);
            int rx = ax + f * 7;
            c.line(rx, yTop, rx, Math.min(yBot, g), Pal.CANVAS[1]);
        }
        for (int y = apexY - back; y <= apexY; y++) { c.set(ax, y, Pal.CANVAS[5]); c.set(ax + 1, y, Pal.CANVAS[3]); }
        // front gable with seams converging on the pole, an open flap and the dark inside
        Kit.poly(c, Pal.CANVAS[3], l, g, ax, apexY, r, g);
        for (int s = -3; s <= 3; s++) if (s != 0) c.line(ax + s * 12, g, ax + s * 2, apexY + 10, Math.abs(s) == 3 ? Pal.CANVAS[2] : Pal.CANVAS[2]);
        Kit.poly(c, Pal.INK, ax - 15, g, ax, apexY + 24, ax + 15, g);
        Kit.poly(c, Pal.CANVAS[0], ax - 12, g, ax, apexY + 30, ax + 8, g);
        Kit.poly(c, Pal.CANVAS[5], ax, apexY + 24, ax + 15, g, ax + 25, g - 12);
        Kit.poly(c, Pal.CANVAS[4], ax + 15, g, ax + 25, g - 12, ax + 22, g - 1);
        c.line(ax, apexY + 24, ax + 25, g - 12, Pal.CANVAS[4]);
        c.set(ax + 24, g - 12, Pal.DRY[2]); c.set(ax + 25, g - 11, Pal.DRY[2]);          // tie-back cord
        // patches and stitches
        c.rect(l + 14, g - 20, 8, 6, Pal.CANVAS[2]); for (int x = l + 14; x < l + 22; x += 2) { c.set(x, g - 20, Pal.CANVAS[1]); c.set(x + 1, g - 15, Pal.CANVAS[1]); }
        c.rect(r - 24, g - 36, 7, 5, Pal.CANVAS[1]);
        for (int y = g - 4; y <= g; y++) for (int x = l; x <= r; x++) if (c.opaque(x, y) && Noise.rnd(x, y, 731) < (y - g + 5) / 7.0) c.set(x, y, Pal.shift(c.get(x, y), -1));
        // pole tip, pennant, guy ropes, stakes
        for (int y = apexY - 9; y < apexY; y++) { c.set(ax, y, Pal.WOOD[4]); c.set(ax + 1, y, Pal.WOOD[2]); }
        Kit.poly(c, Pal.CRIMSON[3], ax + 2, apexY - 9, ax + 14, apexY - 7, ax + 2, apexY - 4);
        c.line(ax + 2, apexY - 9, ax + 13, apexY - 7, Pal.CRIMSON[4]);
        c.line(l + 2, g - 8, l - 14, g + 2, Pal.DRY[2]); c.line(r - 2, g - 8, r + 14, g + 2, Pal.DRY[1]);
        c.line(ax - 30, g - 46, l - 16, g - 30, Pal.DRY[2]);
        for (int[] st : new int[][]{{l - 15, g}, {r + 14, g}}) { c.vline(st[0], st[1] - 1, st[1] + 3, Pal.WOOD[3]); c.set(st[0] + 1, st[1] + 1, Pal.WOOD[1]); }
        c.selectiveOutline();
        return c;
    }

    static Canvas campfire() {
        var k = SetPieces.Kind.CAMPFIRE;
        Canvas c = canvas(k, 3);
        int cx = k.anchorX, cy = k.anchorY - 8;
        c.ellipse(cx, cy, 14, 5.5, Pal.STONE[1]);
        c.ellipse(cx, cy, 11, 4, Pal.MUD[0]);
        for (int a = 0; a < 12; a++) {            // ring stones, lit on their upper left
            double t = a * Math.PI * 2 / 12 + .15;
            int sx = (int) Math.round(cx + Math.cos(t) * 16), sy = (int) Math.round(cy + Math.sin(t) * 6.5);
            int tone = sy < cy ? 4 : 3;
            c.rect(sx - 2, sy - 1, 4, 3, Pal.STONE[tone]);
            c.set(sx - 2, sy - 1, Pal.STONE[tone + 1]); c.set(sx - 1, sy - 1, Pal.STONE[tone + 1]);
            c.set(sx + 1, sy + 1, Pal.STONE[tone - 2]); c.set(sx, sy + 1, Pal.STONE[tone - 1]);
        }
        for (int[] log : new int[][]{{-9, 3, 8, -3}, {-8, -3, 9, 3}, {-1, 4, 2, -4}}) {   // crossed logs
            c.line(cx + log[0], cy + log[1], cx + log[2], cy + log[3], Pal.WOOD[1]);
            c.line(cx + log[0], cy + log[1] - 1, cx + log[2], cy + log[3] - 1, Pal.WOOD[3]);
            c.line(cx + log[0], cy + log[1] - 2, cx + log[2], cy + log[3] - 2, Pal.WOOD[4]);
        }
        for (int e = 0; e < 7; e++) c.set(cx - 5 + e * 2, cy + (e % 2), e % 3 == 0 ? Pal.FIRE[3] : Pal.FIRE[1]);
        c.selectiveOutline();
        return c;
    }

    static Canvas lamp(int frame) {
        var k = SetPieces.Kind.LAMP;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, px = k.anchorX - 2;
        post(c, px, 14, g, 4);
        for (int y = 14; y < 18; y++) for (int x = px; x <= px + 24; x++) c.set(x, y, y == 14 ? Pal.WOOD[5] : y == 17 ? Pal.WOOD[1] : Pal.WOOD[3]);
        c.line(px + 4, 34, px + 14, 18, Pal.WOOD[3]); c.line(px + 4, 35, px + 15, 18, Pal.WOOD[1]);
        c.vline(px + 20, 18, 22, Pal.IRON[3]);
        int lx = px + 14, ly = 23;                    // lantern: cap, iron frame, warm glass
        Kit.poly(c, Pal.IRON[3], lx - 1, ly + 4, lx + 6, ly - 1, lx + 13, ly + 4);
        c.hline(lx + 2, lx + 10, ly + 2, Pal.IRON[4]);
        c.rect(lx, ly + 4, 13, 15, Pal.IRON[1]);
        c.rect(lx + 1, ly + 5, 11, 13, Pal.FIRE[2]);
        c.rect(lx + 2, ly + 6, 9, 11, Pal.FIRE[3]);
        c.rect(lx + 4, ly + 8, 5, 7, Pal.FIRE[4]);
        c.rect(lx + 5, ly + 10, 3, 3, frame == 0 ? Pal.FIRE[5] : Pal.FIRE[4]);
        c.vline(lx + 6, ly + 4, ly + 18, Pal.IRON[2]); c.hline(lx, lx + 12, ly + 11, Pal.IRON[2]);
        c.vline(lx, ly + 4, ly + 18, Pal.IRON[3]);
        c.hline(lx - 1, lx + 13, ly + 19, Pal.IRON[3]); c.hline(lx, lx + 12, ly + 20, Pal.IRON[1]);
        // a strip of crimson cloth tied round the post: someone is still expected home
        c.rect(px - 1, 52, 6, 4, Pal.CRIMSON[3]); c.hline(px - 1, px + 4, 52, Pal.CRIMSON[4]);
        c.line(px - 1, 56, px - 4, 63, Pal.CRIMSON[2]); c.line(px, 56, px - 2, 64, Pal.CRIMSON[3]);
        Canvas stones = Decals.RUBBLE.art();
        c.blit(stones, px - 6, g - stones.h + 2);
        c.blit(Decals.TUFT_B.art(), px + 3, g - Decals.TUFT_B.art().h + 2);
        c.selectiveOutline();
        return c;
    }

    static Canvas logSeat(int v) {
        var k = SetPieces.Kind.LOG_SEAT;
        Canvas c = canvas(k, 2);
        int g = k.anchorY - 1, len = v == 0 ? 36 : 28, x0 = (k.width - len) / 2;
        int[] rows = {Pal.WOOD[5], Pal.WOOD[4], Pal.WOOD[4], Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[2], Pal.WOOD[2], Pal.WOOD[1], Pal.WOOD[0]};
        for (int x = x0; x < x0 + len; x++) for (int r = 0; r < rows.length; r++) {
            int colour = rows[r];
            if (Noise.rnd(x / 2, r, 701 + v) < .12) colour = Pal.shift(colour, -1);
            c.set(x, g - rows.length + 1 + r, colour);
        }
        c.ellipse(x0 + len - 1, g - 4.5, 3.4, 4.6, Pal.WOOD[5]);           // cut end and its rings
        c.ellipse(x0 + len - 1, g - 4.5, 1.8, 2.6, Pal.WOOD[4]);
        c.set(x0 + len - 1, g - 5, Pal.WOOD[3]);
        if (v == 1) c.blit(Decals.TUFT_D.art(), x0 + 4, g - 9 - Decals.TUFT_D.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ road furniture
    static Canvas cart(int v) {
        var k = SetPieces.Kind.CART;
        Canvas c = canvas(k, 4);
        int g = k.anchorY - 1;
        if (v == 0) {     // overturned: the bed faces us, a wheel in the air, the other smashed beneath
            Kit.plankWall(c, 14, g - 34, 92, g, 711);
            for (int x = 14; x < 106; x += 23) for (int y = g - 34; y <= g; y++) { c.set(x, y, Pal.IRON[2]); c.set(x + 1, y, Pal.IRON[1]); }
            for (int x = 14; x < 106; x += 23) { c.set(x, g - 30, Pal.IRON[4]); c.set(x, g - 6, Pal.IRON[4]); }
            wheel(c, 84, g - 50, 16, true);
            c.line(84, g - 50, 84, g - 35, Pal.WOOD[2]);                    // axle stub
            for (int s = 0; s < 10; s++) c.set(18 + s * 3, g - 35 - (s % 2), Pal.WOOD[5]);
            c.blit(Decals.PLANK.art(), 2, g - 2);
            c.blit(Decals.PLANK.art().flipped(), 88, g);
            Canvas sack = Decals.ascii("...lll....", "..lHHHGl..", ".lHHHGGGl.", "lHHGGGFFFl", "lGGGFFFFFD", ".DGFFFFFD.", "..DDDDDD..");
            c.blit(sack, 98, g - sack.h + 2);
            c.blit(sack, 106, g - sack.h + 4);
        } else {          // upright supply cart, crates and a barrel lashed on
            for (int y = g - 40; y < g - 30; y++) for (int x = 14; x < 106; x++)
                c.set(x, y, y == g - 40 ? Pal.WOOD[5] : x % 8 == 0 ? Pal.WOOD[2] : Pal.WOOD[4]);
            Kit.plankWall(c, 14, g - 30, 92, g - 12, 712);
            wheel(c, 34, g - 13, 13, false); wheel(c, 86, g - 13, 13, false);
            c.line(0, g - 18, 14, g - 24, Pal.WOOD[3]); c.line(0, g - 17, 14, g - 23, Pal.WOOD[1]);
            c.line(2, g - 13, 14, g - 20, Pal.WOOD[3]);
            Props.crate(c, 22, g - 41, 24, 17); Props.crate(c, 48, g - 41, 22, 15);
            Props.barrel(c, 74, g - 41);
            c.line(22, g - 54, 94, g - 50, Pal.DRY[2]);
        }
        c.selectiveOutline();
        return c;
    }
    private static void wheel(Canvas c, int cx, int cy, int r, boolean broken) {
        for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++) {
            double d = Math.hypot(x + .5 - cx, y + .5 - cy);
            if (d > r) continue;
            if (d > r - 3) c.set(x, y, x < cx && y < cy ? Pal.WOOD[4] : Pal.WOOD[2]);
            if (Math.abs(d - (r - .8)) < .7) c.set(x, y, x < cx && y < cy ? Pal.IRON[4] : Pal.IRON[2]);   // iron tyre
        }
        for (int s = 0; s < 8; s++) {
            if (broken && (s == 2 || s == 3)) continue;
            double a = s * Math.PI / 4;
            int ex = (int) Math.round(cx + Math.cos(a) * (r - 3)), ey = (int) Math.round(cy + Math.sin(a) * (r - 3));
            c.line(cx, cy, ex, ey, Pal.WOOD[3]);
            c.line(cx + 1, cy, ex + 1, ey, s < 4 ? Pal.WOOD[1] : Pal.WOOD[4]);
        }
        c.ellipse(cx, cy, 3, 3, Pal.IRON[2]); c.set(cx - 1, cy - 1, Pal.IRON[4]);
        if (broken) for (int y = cy + 1; y <= cy + r; y++) for (int x = cx - 2; x <= cx + r; x++) if (Noise.rnd(x, y, 721) < .75) c.clear(x, y);
    }

    static Canvas signpost() {
        var k = SetPieces.Kind.SIGNPOST;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, px = k.anchorX - 2;
        post(c, px, 4, g, 4);
        c.set(px + 1, 3, Pal.WOOD[5]); c.set(px + 2, 3, Pal.WOOD[3]);
        board(c, px + 3, 10, 30, true); board(c, px - 22, 26, 24, false);
        Canvas tilt = Decals.ascii("vvvvvv.", "ccccccv", "xxxxxcc", ".zzzzx.");
        c.blit(tilt, px - 9, g - 6);
        c.blit(Decals.ROCK_S.art(), px - 8, g - 4); c.blit(Decals.TUFT_A.art(), px + 3, g - Decals.TUFT_A.art().h + 2);
        c.selectiveOutline();
        return c;
    }
    private static void board(Canvas c, int x, int y, int w, boolean right) {
        int h = 10;
        for (int i = 0; i < w; i++) {
            int tip = right ? Math.max(0, i - (w - 6)) : Math.max(0, 5 - i);
            for (int r = tip; r < h - tip; r++) c.set(x + i, y + r, r == tip ? Pal.WOOD[5] : r >= h - 2 - tip ? Pal.WOOD[2] : (r + i / 7) % 4 == 0 ? Pal.WOOD[3] : Pal.WOOD[4]);
        }
        for (int i = 6; i < w - 7; i += 4) { c.set(x + i, y + 4, Pal.WOOD[1]); c.set(x + i + 1, y + 5, Pal.WOOD[2]); }   // worn letters
        c.set(x + (right ? 2 : w - 3), y + 4, Pal.IRON[4]);
    }

    // ------------------------------------------------------------------ muster ground
    /** A tattered crimson banner with gold trim and sigil; frames ripple a wave down the cloth. */
    static Canvas banner(int frame, int frames) {
        var k = SetPieces.Kind.BANNER;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, px = k.anchorX - 2;
        post(c, px, 8, g, 4);
        c.rect(px - 1, 4, 6, 4, Pal.IRON[3]); c.set(px + 1, 2, Pal.IRON[4]); c.set(px + 2, 3, Pal.IRON[4]); c.set(px + 1, 3, Pal.IRON[3]);
        for (int y = 12; y < 15; y++) for (int x = px; x <= px + 32; x++) c.set(x, y, y == 12 ? Pal.WOOD[5] : y == 14 ? Pal.WOOD[1] : Pal.WOOD[3]);
        double t = frame * Math.PI * 2 / frames;
        int top = 15, len = 66, width = 26;
        for (int y = 0; y < len; y++) {
            double wave = Math.sin(t - y * .2) * (1.4 + y * .05);
            int sx = px + 5 + (int) Math.round(wave * .7);
            int tatter = y > len - 12 ? (int) (Noise.rnd(y, 3, 731) * 11) : 0;
            for (int x = 0; x < width - tatter; x++) {
                if (y > len - 9 && Noise.rnd(x, y, 732) < .3) continue;
                double shade = Math.cos(t - y * .2 + x * .24);
                int colour = shade > .55 ? Pal.CRIMSON[4] : shade > -.15 ? Pal.CRIMSON[3] : shade > -.7 ? Pal.CRIMSON[2] : Pal.CRIMSON[1];
                if (x <= 1 || x >= width - 2 - tatter) colour = Pal.GOLD[shade > 0 ? 2 : 1];
                c.set(sx + x, top + y, colour);
            }
            int mid = sx + width / 2;                  // the expedition's sigil: a gold sunburst
            int dy = y - 22;
            if (Math.abs(dy) <= 6) for (int dx = -6; dx <= 6; dx++) {
                double d = Math.hypot(dx, dy);
                boolean ray = (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy)) && d <= 6;
                if (d <= 2.5 || ray) c.set(mid + dx, top + y, d <= 1.5 ? Pal.GOLD[4] : Pal.GOLD[3]);
            }
        }
        c.blit(Decals.TUFT_B.art(), px - 3, g - Decals.TUFT_B.art().h + 2);
        c.selectiveOutline();
        return c;
    }

    static Canvas rack(int v) {
        var k = SetPieces.Kind.RACK;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, x0 = 6, w = 64;
        for (int leg : new int[]{x0, x0 + w - 5}) post(c, leg, g - 36, g, 5);
        for (int[] rail : new int[][]{{g - 36, 3}, {g - 12, 2}})
            for (int y = rail[0]; y < rail[0] + rail[1] + 1; y++) for (int x = x0; x < x0 + w; x++)
                c.set(x, y, y == rail[0] ? Pal.WOOD[5] : y == rail[0] + rail[1] ? Pal.WOOD[1] : Pal.WOOD[3]);
        for (int s = 0; s < 8; s++) {
            int x = x0 + 7 + s * 7;
            if (v == 0) {                                        // spears, heads up
                if (s == 3 || s == 6) continue;
                c.vline(x, g - 48, g - 2, Pal.WOOD[4]); c.vline(x + 1, g - 48, g - 2, Pal.WOOD[2]);
                Canvas head = Decals.ascii(".L.", "LMn", "LMn", ".n.", ".z.");
                c.blit(head, x - 1, g - 54);
            } else if (s % 2 == 0) {                             // swords, points down
                c.vline(x, g - 34, g - 3, Pal.IRON[5]); c.vline(x + 1, g - 34, g - 3, Pal.IRON[3]);
                c.hline(x - 2, x + 3, g - 35, Pal.GOLD[2]); c.hline(x - 2, x + 3, g - 34, Pal.GOLD[1]);
                c.vline(x, g - 41, g - 36, Pal.WOOD[3]); c.vline(x + 1, g - 41, g - 36, Pal.WOOD[1]); c.set(x, g - 42, Pal.GOLD[3]);
            }
        }
        if (v == 1) { c.blit(Decals.SHIELD.art(), x0 + 44, g - Decals.SHIELD.art().h + 1); c.blit(Decals.HELMET.art(), x0 - 4, g - 46); }
        c.selectiveOutline();
        return c;
    }

    static Canvas dummy(int v) {
        var k = SetPieces.Kind.DUMMY;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, cx = k.anchorX;
        post(c, cx - 2, 18, g, 4);
        for (int y = 28; y < 31; y++) for (int x = cx - 15; x <= cx + 15; x++) c.set(x, y, y == 28 ? Pal.WOOD[5] : y == 30 ? Pal.WOOD[1] : Pal.WOOD[3]);
        for (int y = 24; y < 52; y++) for (int x = cx - 10; x <= cx + 10; x++) {     // straw body bound with rope
            double u = (x + .5 - cx) / 10.5, vv = (y - 38) / 14.0;
            if (u * u + vv * vv * .6 > 1) continue;
            int colour = u < -.45 ? Pal.DRY[3] : u < .25 ? Pal.DRY[2] : Pal.DRY[1];
            if ((y * 3 + x) % 5 == 0) colour = Pal.shift(colour, u < 0 ? 1 : -1);
            if (y == 33 || y == 34 || y == 45) colour = y == 33 ? Pal.WOOD[3] : Pal.WOOD[2];
            c.set(x, y, colour);
        }
        c.ellipse(cx, 15, 7.5, 7, Pal.CANVAS[3]);                                     // sack head, stitched face
        c.ellipse(cx - 2, 13, 3.5, 3, Pal.CANVAS[4]);
        c.set(cx - 3, 15, Pal.INK); c.set(cx + 2, 15, Pal.INK);
        for (int x = cx - 3; x <= cx + 3; x++) c.set(x, 19, x % 2 == 0 ? Pal.CANVAS[1] : Pal.CANVAS[2]);
        c.set(cx, 8, Pal.DRY[3]); c.set(cx + 1, 7, Pal.DRY[2]); c.set(cx - 2, 8, Pal.DRY[3]);  // straw poking out
        if (v == 1) for (int[] a : new int[][]{{cx + 5, 36}, {cx - 6, 42}, {cx + 1, 47}}) {
            c.set(a[0], a[1], Pal.WOOD[4]); c.set(a[0] + 1, a[1] - 1, Pal.WOOD[3]); c.set(a[0] + 2, a[1] - 2, Pal.BONE[3]); c.set(a[0] + 3, a[1] - 2, Pal.BONE[2]);
        }
        for (int s = 0; s < 5; s++) c.set(cx - 8 + s * 4, g - (s % 2), Pal.DRY[2]);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ broken palisade
    static Canvas gatePost(boolean mirror) {
        var k = SetPieces.Kind.GATE_POST;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, x0 = k.anchorX - 6;
        post(c, x0, 10, g, 12);
        for (int col = 0; col < 12; col++) c.set(x0 + col, 10 + (int) (Noise.rnd(col, 1, 742) * 4), Pal.WOOD[5]);
        for (int s = 0; s < 16; s++) for (int r = 0; r < 7; r++) {   // stub of the fallen lintel, still pegged
            int x = mirror ? x0 - 1 - s : x0 + 12 + s, y = 20 + r + s / 3;
            c.set(x, y, r == 0 ? Pal.WOOD[5] : r == 6 ? Pal.WOOD[0] : r < 3 ? Pal.WOOD[4] : Pal.WOOD[3]);
        }
        int endX = mirror ? x0 - 17 : x0 + 28;
        for (int r = 0; r < 7; r++) c.set(endX, 25 + r, Noise.rnd(r, 2, 743) < .5 ? Pal.WOOD[5] : 0);
        for (int y : new int[]{40, 96}) { c.hline(x0, x0 + 11, y, Pal.IRON[2]); c.hline(x0, x0 + 11, y + 1, Pal.IRON[1]); c.set(x0 + 2, y, Pal.IRON[4]); c.set(x0 + 9, y, Pal.IRON[3]); }
        c.line(x0 + (mirror ? 0 : 11), 64, x0 + (mirror ? -8 : 19), 82, Pal.DRY[2]);
        c.line(x0 + (mirror ? 0 : 11), 65, x0 + (mirror ? -7 : 18), 82, Pal.DRY[1]);
        c.blit(Decals.RUBBLE.art(), x0 - 6, g - Decals.RUBBLE.art().h + 3);
        c.blit(Decals.TUFT_A.art(), x0 + 6, g - Decals.TUFT_A.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ arena furniture
    static Canvas brazier() {
        var k = SetPieces.Kind.BRAZIER;
        Canvas c = canvas(k, 3);
        int g = k.anchorY - 1, cx = k.anchorX, bowl = g - 28;
        c.line(cx - 13, g, cx - 3, bowl + 6, Pal.IRON[3]); c.line(cx - 12, g, cx - 2, bowl + 6, Pal.IRON[2]);
        c.line(cx + 13, g, cx + 3, bowl + 6, Pal.IRON[1]); c.line(cx + 12, g, cx + 2, bowl + 6, Pal.IRON[2]);
        c.vline(cx, bowl + 6, g - 1, Pal.IRON[2]); c.vline(cx + 1, bowl + 6, g - 1, Pal.IRON[1]);
        c.hline(cx - 8, cx + 8, g - 12, Pal.IRON[3]); c.hline(cx - 8, cx + 8, g - 11, Pal.IRON[1]);
        for (int y = 0; y < 8; y++) {
            int half = 14 - y * y / 5;
            for (int x = -half; x <= half; x++) {
                int colour = y == 0 ? (x < 0 ? Pal.IRON[5] : Pal.IRON[4]) : y == 1 ? Pal.IRON[3]
                        : x < -half / 2 ? Pal.IRON[4] : x < half / 3 ? Pal.IRON[2] : Pal.IRON[1];
                if (y == 3 && Math.floorMod(x, 5) == 0) colour = Pal.IRON[4];                  // rivets
                c.set(cx + x, bowl + y, colour);
            }
        }
        c.hline(cx - 13, cx + 13, bowl - 1, Pal.FIRE[1]);
        for (int x = -12; x <= 12; x += 3) c.set(cx + x, bowl - 1, Noise.rnd(x, 1, 761) < .5 ? Pal.FIRE[3] : Pal.FIRE[2]);
        c.selectiveOutline();
        return c;
    }

    static Canvas column(int v) {
        var k = SetPieces.Kind.COLUMN;
        Canvas c = canvas(k, 4);
        int g = k.anchorY - 1, cx = k.anchorX;
        Kit.stoneBox(c, cx - 15, g, 30, 6, 12, 751 + v, true);
        int height = v == 0 ? 128 : v == 1 ? 84 : 46;
        Kit.cylinder(c, cx, g - 12, 11, height - 12, Pal.STONE, 4, 752);
        for (int y = g - height; y < g - 12; y += 16) { c.hline(cx - 11, cx + 10, y, Pal.STONE[1]); c.hline(cx - 11, cx + 10, y + 1, Pal.STONE[5]); }
        for (int x = cx - 8; x <= cx + 7; x += 4) for (int y = g - height + 3; y < g - 13; y++) if ((y - g) % 16 != 0) c.tint(x, y, Pal.shift(c.get(x, y), -1));
        if (v == 0) Kit.stoneBox(c, cx - 15, g - height + 1, 30, 6, 8, 753, true);
        else {
            for (int x = cx - 11; x < cx + 11; x++) {                  // broken, jagged top
                int jag = (int) (Noise.rnd(x / 2, v, 754) * 7);
                for (int s = 0; s < jag; s++) c.clear(x, g - height + 1 + s);
                c.tint(x, g - height + 1 + jag, Pal.STONE[6]);
                c.tint(x, g - height + 2 + jag, Pal.STONE[5]);
            }
            Canvas drum = Decals.stone(16, 9, true, 755 + v);
            c.blit(drum, cx + 6, g - drum.h + 3);
        }
        for (int s = 0; s < 4; s++) {                                    // ivy
            int x = cx - 9 + s * 5, len = 10 + s * 9;
            for (int y = 0; y < len; y++) c.tint(x + (y / 4 % 2), g - height + 8 + y, y % 3 == 0 ? Pal.GRASS[3] : Pal.GRASS[2]);
        }
        c.selectiveOutline();
        return c;
    }
}
