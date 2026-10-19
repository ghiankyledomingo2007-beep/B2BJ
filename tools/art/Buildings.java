/** The large Outpost landmarks at character scale. Same conventions as Structures: ground row = anchorY - 1. */
final class Buildings {
    private Buildings() { }

    // ------------------------------------------------------------------ abandoned barracks
    static Canvas barracks() {
        var k = SetPieces.Kind.BARRACKS;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, x0 = k.anchorX - 120, w = 240;
        int wallTop = g - 96, eave = wallTop - 1, ridge = eave - 58;
        Kit.stoneBox(c, x0 + 26, eave - 30, 24, 6, 50, 801, true);                       // chimney
        for (int x = x0 + 26; x < x0 + 50; x++) for (int s = 0; s < (int) (Noise.rnd(x / 2, 1, 802) * 5); s++) c.clear(x, eave - 30 - 55 + s);
        Kit.shingleRoof(c, x0 - 6, w + 12, ridge, eave, 803);
        // the collapsed bay: rafters hang over the dark hall
        int hx0 = x0 + 148, hx1 = x0 + 222;
        for (int y = ridge + 8; y < eave - 5; y++) {
            int left = hx0 + (int) (Noise.rnd(y / 2, 2, 804) * 7) + (y - ridge) / 4;
            int right = hx1 - (int) (Noise.rnd(y / 2, 3, 805) * 7) - (eave - y) / 6;
            for (int x = left; x < right; x++) c.set(x, y, y > eave - 14 ? Pal.WOOD[0] : Pal.INK);
            c.set(left - 1, y, Pal.WOOD[5]); c.set(left - 2, y, Pal.WOOD[3]); c.set(right, y, Pal.WOOD[1]);
        }
        for (int r = 0; r < 5; r++) {
            int rx = hx0 + 8 + r * 13;
            boolean snapped = r == 2 || r == 4;
            int endY = snapped ? ridge + 20 + r * 3 : eave - 3;
            for (int t = 0; t < 3; t++) c.line(rx + t, ridge + 6, rx + 5 + t, endY, t == 0 ? Pal.WOOD[4] : t == 1 ? Pal.WOOD[3] : Pal.WOOD[1]);
            if (snapped) { c.set(rx + 5, endY + 1, Pal.WOOD[5]); c.set(rx + 6, endY, Pal.WOOD[5]); }
        }
        for (int t = 0; t < 3; t++) c.line(hx0 + 4, ridge + 20 + t, hx1 - 6, ridge + 16 + t, t == 0 ? Pal.WOOD[4] : t == 1 ? Pal.WOOD[3] : Pal.WOOD[1]);
        // front wall: stone footing, timber frame, plank infill
        Kit.plankWall(c, x0, wallTop, w, g - 10, 806);
        Kit.stoneBox(c, x0 - 2, g, w + 4, 0, 10, 807, false);
        for (int post = 0; post <= 5; post++) Structures.post(c, x0 + post * (w - 6) / 5, wallTop, g - 10, 6);
        for (int y = wallTop; y < wallTop + 4; y++) for (int x = x0; x < x0 + w; x++) c.set(x, y, y == wallTop ? Pal.WOOD[5] : y == wallTop + 3 ? Pal.WOOD[1] : Pal.WOOD[3]);
        for (int y = wallTop + 40; y < wallTop + 44; y++) for (int x = x0; x < x0 + w; x++) c.set(x, y, y == wallTop + 40 ? Pal.WOOD[5] : y == wallTop + 43 ? Pal.WOOD[1] : Pal.WOOD[3]);
        for (int win : new int[]{x0 + 16, x0 + 112, x0 + 196}) window(c, win, wallTop + 12, win == x0 + 196);
        // the door hangs open on one hinge; a small awning over it
        int dx = x0 + 58, dw = 36, dh = 74;
        c.rect(dx, g - 10 - dh, dw, dh, Pal.INK);
        c.rect(dx + 2, g - 10 - dh + 2, dw - 4, 6, Pal.WOOD[0]);
        for (int y = g - 10 - dh; y < g - 10; y++) { c.set(dx - 1, y, Pal.WOOD[5]); c.set(dx - 2, y, Pal.WOOD[3]); c.set(dx + dw, y, Pal.WOOD[2]); c.set(dx + dw + 1, y, Pal.WOOD[1]); }
        Kit.poly(c, Pal.WOOD[3], dx + dw, g - 8 - dh, dx + dw + 13, g - 4 - dh, dx + dw + 13, g - 13, dx + dw, g - 10);
        for (int x = dx + dw + 2; x < dx + dw + 13; x += 4) c.line(x, g - 7 - dh + (x - dx - dw) / 3, x, g - 12, Pal.WOOD[2]);
        c.set(dx + dw + 10, g - 50, Pal.IRON[4]); c.set(dx + dw + 10, g - 49, Pal.IRON[2]);
        for (int y = g - 13 - dh; y < g - 7 - dh; y++) for (int x = dx - 8; x < dx + dw + 8; x++) c.set(x, y, y == g - 13 - dh ? Pal.WOOD[5] : (x - dx) % 6 == 0 ? Pal.WOOD[1] : Pal.WOOD[3]);
        c.line(dx - 6, g - 7 - dh, dx - 2, g - 22 - dh + 18, Pal.WOOD[2]);
        c.rect(dx - 4, g - 9, dw + 8, 4, Pal.STONE[5]); c.hline(dx - 4, dx + dw + 3, g - 9, Pal.STONE[6]); c.hline(dx - 4, dx + dw + 3, g - 6, Pal.STONE[2]);
        // a torn banner nailed between the windows, a rain barrel by the door
        for (int y = 0; y < 30; y++) for (int x = 0; x < 14; x++) {
            if (y > 22 && Noise.rnd(x, y, 808) < .45) continue;
            c.set(x0 + 150 + x, wallTop + 8 + y, x <= 1 ? Pal.GOLD[1] : x < 6 ? Pal.CRIMSON[3] : Pal.CRIMSON[2]);
        }
        Props.barrel(c, dx - 30, g - 4);
        // ivy up the timbers, the fallen roof at the foot of the collapse
        for (int s = 0; s < 8; s++) {
            int ix = x0 + 4 + (int) (Noise.rnd(s, 4, 809) * (w - 8)), len = 14 + (int) (Noise.rnd(s, 5, 809) * 44);
            for (int y = 0; y < len; y++) c.tint(ix + (y / 5 % 2), g - 11 - y, y % 3 == 0 ? Pal.GRASS[3] : Pal.GRASS[2]);
        }
        c.blit(Decals.PLANK.art(), hx0 + 6, g - 1); c.blit(Decals.RUBBLE.art(), hx0 + 30, g);
        c.blit(Decals.PLANK.art().flipped(), hx0 + 44, g + 1); c.blit(Decals.TUFT_C.art(), x0 - 8, g - Decals.TUFT_C.art().h + 3);
        c.selectiveOutline();
        return c;
    }
    private static void window(Canvas c, int x, int y, boolean shutterGone) {
        int ww = 24, wh = 22;
        c.rect(x, y, ww, wh, Pal.INK);
        c.rect(x + 2, y + 2, ww - 4, wh - 4, Pal.WOOD[0]);
        c.vline(x + ww / 2, y, y + wh - 1, Pal.WOOD[2]); c.hline(x, x + ww - 1, y + wh / 2, Pal.WOOD[2]);
        c.hline(x - 2, x + ww + 1, y - 2, Pal.WOOD[5]); c.hline(x - 2, x + ww + 1, y - 1, Pal.WOOD[3]);
        c.hline(x - 2, x + ww + 1, y + wh, Pal.WOOD[3]); c.hline(x - 2, x + ww + 1, y + wh + 1, Pal.WOOD[1]);
        if (!shutterGone) for (int r = 0; r < wh; r++) for (int s = 0; s < 8; s++)
            c.set(x - 10 + s, y + r, s == 0 || s == 7 ? Pal.WOOD[1] : s % 3 == 1 ? Pal.WOOD[4] : Pal.WOOD[3]);
        else for (int t = 0; t < 3; t++) c.line(x + ww + 3 + t, y + 3, x + ww + 9 + t, y + wh + 4, t == 1 ? Pal.WOOD[3] : Pal.WOOD[1]);
    }

    // ------------------------------------------------------------------ north watch
    static Canvas watchtower() {
        var k = SetPieces.Kind.WATCHTOWER;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, cx = k.anchorX, deck = g - 160;
        for (int lx : new int[]{cx - 22, cx + 16}) Structures.post(c, lx, deck - 10, g - 18, 6);   // back legs
        for (int level = 0; level < 4; level++) {                                                   // bracing
            int y0 = g - 8 - level * 38, y1 = y0 - 36;
            for (int t = 0; t < 2; t++) {
                c.line(cx - 22, y0 + t, cx + 21, y1 + t, t == 0 ? Pal.WOOD[3] : Pal.WOOD[1]);
                c.line(cx + 21, y0 + t, cx - 22, y1 + t, t == 0 ? Pal.WOOD[3] : Pal.WOOD[1]);
            }
            for (int y = y1; y < y1 + 3; y++) for (int x = cx - 30; x <= cx + 30; x++) c.set(x, y, y == y1 ? Pal.WOOD[5] : y == y1 + 2 ? Pal.WOOD[1] : Pal.WOOD[3]);
        }
        for (int lx : new int[]{cx - 30, cx + 24}) Structures.post(c, lx, deck, g, 7);               // front legs
        for (int y = deck; y <= g; y++) { c.set(cx - 6, y, Pal.WOOD[4]); c.set(cx - 5, y, Pal.WOOD[2]); c.set(cx + 5, y, Pal.WOOD[3]); c.set(cx + 6, y, Pal.WOOD[1]); }
        for (int y = deck + 4; y < g; y += 7) { c.hline(cx - 4, cx + 4, y, Pal.WOOD[4]); c.hline(cx - 4, cx + 4, y + 1, Pal.WOOD[1]); }
        // deck: planked top, fascia, railing
        for (int y = deck - 18; y < deck; y++) for (int x = cx - 42; x < cx + 42; x++)
            c.set(x, y, y == deck - 18 ? Pal.WOOD[5] : Math.floorMod(x - cx, 8) == 0 ? Pal.WOOD[2] : Pal.WOOD[4]);
        for (int y = deck; y < deck + 7; y++) for (int x = cx - 42; x < cx + 42; x++) c.set(x, y, y == deck ? Pal.WOOD[4] : y == deck + 6 ? Pal.WOOD[0] : Pal.WOOD[2]);
        // beacon brazier on the deck (the game draws its flame) and a pennant pole
        int bowl = deck - 26;
        c.line(cx - 8, deck - 10, cx - 1, bowl + 5, Pal.IRON[3]); c.line(cx + 12, deck - 10, cx + 5, bowl + 5, Pal.IRON[1]);
        for (int y = 0; y < 7; y++) { int half = 11 - y * y / 4; for (int x = -half; x <= half; x++) c.set(cx + 2 + x, bowl + y, y == 0 ? Pal.IRON[5] : x < 0 ? Pal.IRON[3] : Pal.IRON[1]); }
        c.hline(cx - 8, cx + 12, bowl - 1, Pal.FIRE[2]);
        for (int px = cx - 42; px <= cx + 36; px += 13) Structures.post(c, px, deck - 40, deck - 18, 4);
        for (int y = deck - 42; y < deck - 38; y++) for (int x = cx - 42; x < cx + 42; x++) c.set(x, y, y == deck - 42 ? Pal.WOOD[5] : y == deck - 39 ? Pal.WOOD[1] : Pal.WOOD[3]);
        c.hline(cx - 42, cx + 41, deck - 29, Pal.WOOD[3]); c.hline(cx - 42, cx + 41, deck - 28, Pal.WOOD[1]);
        Structures.post(c, cx - 40, deck - 100, deck - 40, 3);
        for (int y = 0; y < 14; y++) for (int x = 0; x < 22 - y * 3 / 2; x++)
            c.set(cx - 37 + x, deck - 98 + y, x <= 1 ? Pal.GOLD[2] : y < 5 ? Pal.CRIMSON[4] : Pal.CRIMSON[3]);
        c.blit(Decals.TUFT_C.art(), cx - 44, g - Decals.TUFT_C.art().h + 3);
        c.blit(Decals.ROCK_M.art(), cx + 28, g - Decals.ROCK_M.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ standard yard
    static Canvas standard() {
        var k = SetPieces.Kind.STANDARD;
        Canvas c = new Canvas(k.width, k.anchorY + 6);
        int g = k.anchorY - 1, ax = k.anchorX;
        for (int y = g - 14; y <= g + 4; y++) for (int x = ax + 14; x < ax + 92; x++) {   // Ichor bleeding into the mud
            double d = Math.hypot((x - (ax + 52)) / 36.0, (y - (g - 4)) / 8.5) + (Noise.value(x, y, 4, 811) - .5) * .5;
            if (d > 1) continue;
            c.set(x, y, d < .3 ? Pal.GOLD[5] : d < .5 ? Pal.GOLD[4] : d < .72 ? Pal.GOLD[3] : d < .88 ? Pal.GOLD[2] : Pal.GOLD[1]);
        }
        Kit.stoneBox(c, ax - 24, g, 48, 10, 18, 812, true);
        c.rect(ax - 8, g - 14, 16, 9, Pal.STONE[2]);                                     // carved crest, gold inlay
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) if (Math.abs(dx) + Math.abs(dy) <= 3 && (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy)))
            c.set(ax + dx, g - 10 + dy, Math.abs(dx) + Math.abs(dy) <= 1 ? Pal.GOLD[4] : Pal.GOLD[2]);
        Structures.post(c, ax - 3, g - 58, g - 27, 6);                                    // the snapped stump
        for (int x = ax - 3; x < ax + 3; x++) for (int s = 0; s < (int) (Noise.rnd(x, 1, 813) * 5); s++) c.clear(x, g - 58 + s);
        c.set(ax - 2, g - 54, Pal.WOOD[5]); c.set(ax + 1, g - 56, Pal.WOOD[5]);
        for (int t = 0; t < 4; t++) c.line(ax + 10, g - 22 + t, ax + 88, g - 4 + t, t == 0 ? Pal.WOOD[4] : t < 3 ? Pal.WOOD[3] : Pal.WOOD[1]);
        c.rect(ax + 86, g - 8, 5, 5, Pal.GOLD[2]); c.set(ax + 86, g - 8, Pal.GOLD[4]); c.set(ax + 87, g - 8, Pal.GOLD[3]);
        for (int y = 0; y < 22; y++) for (int x = 0; x < 64; x++) {                       // the banner drapes into the Ichor
            int bx = ax + 16 + x, by = g - 20 + y + x * 16 / 70;
            if (y > 16 && Noise.rnd(x, y, 814) < .45) continue;
            double fold = Math.sin(x * .32 + y * .18);
            int colour = fold > .5 ? Pal.CRIMSON[4] : fold > -.3 ? Pal.CRIMSON[3] : Pal.CRIMSON[2];
            if (y <= 1) colour = Pal.GOLD[2];
            int sx = x - 32, sy = y - 9;
            double d = Math.hypot(sx, sy * 1.3);
            if (d <= 7 && (sx == 0 || sy == 0 || Math.abs(sx) == Math.abs(sy) || d <= 3)) colour = d <= 2 ? Pal.GOLD[5] : Pal.GOLD[3];
            c.set(bx, by, colour);
        }
        c.blit(Decals.ARROW.art(), ax - 36, g - Decals.ARROW.art().h + 2);
        c.blit(Decals.ARROWS.art(), ax + 60, g - 30);
        c.blit(Decals.SHIELD.art(), ax - 52, g - Decals.SHIELD.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ inner court
    static Canvas statue() {
        var k = SetPieces.Kind.STATUE;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, cx = k.anchorX;
        Kit.stoneBox(c, cx - 26, g, 52, 10, 22, 821, true);
        c.rect(cx - 13, g - 16, 26, 10, Pal.STONE[2]);                                    // plaque, names worn smooth
        for (int x = cx - 10; x < cx + 10; x += 3) { c.set(x, g - 13, Pal.STONE[1]); c.set(x + 1, g - 10, Pal.STONE[1]); }
        int feet = g - 27;
        Kit.poly(c, Pal.STONE[2], cx - 20, feet - 8, cx - 14, feet - 74, cx + 14, feet - 74, cx + 20, feet - 8);   // cloak
        Kit.poly(c, Pal.STONE[1], cx + 8, feet - 74, cx + 14, feet - 74, cx + 20, feet - 8, cx + 12, feet - 8);
        c.rect(cx - 12, feet - 24, 9, 24, Pal.STONE[5]); c.rect(cx + 3, feet - 24, 9, 24, Pal.STONE[4]);           // greaves
        c.vline(cx - 12, feet - 24, feet - 1, Pal.STONE[6]); c.hline(cx - 13, cx - 2, feet - 1, Pal.STONE[3]); c.hline(cx + 2, cx + 13, feet - 1, Pal.STONE[3]);
        Kit.poly(c, Pal.STONE[4], cx - 15, feet - 20, cx - 12, feet - 48, cx + 12, feet - 48, cx + 15, feet - 20);  // tabard
        Kit.poly(c, Pal.STONE[5], cx - 15, feet - 20, cx - 12, feet - 48, cx - 1, feet - 48, cx - 2, feet - 20);
        c.rect(cx - 13, feet - 72, 26, 25, Pal.STONE[4]); c.rect(cx - 13, feet - 72, 11, 24, Pal.STONE[5]);         // breastplate
        c.vline(cx - 1, feet - 70, feet - 50, Pal.STONE[3]);
        c.hline(cx - 13, cx + 12, feet - 48, Pal.STONE[2]); c.hline(cx - 13, cx + 12, feet - 47, Pal.STONE[1]);       // belt
        c.ellipse(cx - 15, feet - 68, 7, 6, Pal.STONE[5]); c.ellipse(cx + 15, feet - 68, 7, 6, Pal.STONE[3]);       // pauldrons
        c.ellipse(cx - 17, feet - 70, 3, 2, Pal.STONE[6]);
        c.rect(cx - 20, feet - 64, 6, 20, Pal.STONE[5]); c.rect(cx + 14, feet - 64, 6, 20, Pal.STONE[3]);           // arms
        c.rect(cx - 8, feet - 50, 16, 6, Pal.STONE[4]); c.hline(cx - 8, cx + 7, feet - 50, Pal.STONE[5]);           // hands on the pommel
        c.rect(cx - 2, feet - 42, 4, 42, Pal.STONE[6]); c.vline(cx + 1, feet - 42, feet, Pal.STONE[3]);             // sword, point down
        c.hline(cx - 9, cx + 9, feet - 43, Pal.STONE[5]); c.hline(cx - 9, cx + 9, feet - 42, Pal.STONE[2]);
        c.rect(cx - 2, feet - 53, 4, 4, Pal.STONE[4]); c.rect(cx - 2, feet - 56, 4, 3, Pal.STONE[6]);
        c.rect(cx - 9, feet - 92, 18, 20, Pal.STONE[4]); c.rect(cx - 9, feet - 92, 8, 19, Pal.STONE[5]);             // great helm
        c.hline(cx - 7, cx + 6, feet - 83, Pal.STONE[0]); c.hline(cx - 7, cx + 6, feet - 82, Pal.STONE[1]);
        c.vline(cx - 1, feet - 81, feet - 76, Pal.STONE[0]);
        for (int x = cx - 4; x <= cx + 4; x += 2) c.set(x, feet - 78, Pal.STONE[1]);
        c.rect(cx - 2, feet - 99, 4, 7, Pal.STONE[5]); c.set(cx - 2, feet - 100, Pal.STONE[6]); c.set(cx + 1, feet - 101, Pal.STONE[6]);  // broken crest
        for (int x = cx - 24; x < cx + 24; x++) for (int y = feet - 102; y < feet - 50; y++)                       // moss where rain sits
            if (c.opaque(x, y) && !c.opaque(x, y - 1) && Noise.rnd(x, y, 822) < .6) { c.set(x, y, Pal.GRASS[3]); if (Noise.rnd(x, y, 823) < .5) c.tint(x, y + 1, Pal.GRASS[2]); }
        for (int t = 0; t < 2; t++) c.line(cx + 6 + t, feet - 70, cx + 3 + t, feet - 52, t == 0 ? Pal.STONE[1] : Pal.STONE[3]);
        c.blit(Decals.TUFT_A.art(), cx - 34, g - Decals.TUFT_A.art().h + 3); c.blit(Decals.TUFT_B.art(), cx + 24, g - Decals.TUFT_B.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ east gate
    static Canvas eastGate(boolean open) {
        var k = SetPieces.Kind.EAST_GATE;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, cx = k.anchorX;
        int towerW = 68, towerH = 180, wallH = 150, halfWall = 60;
        Kit.stoneBox(c, cx - halfWall, g, halfWall * 2, 18, wallH, 831, true);
        merlons(c, cx - halfWall, cx + halfWall, g - wallH - 18, 832);
        for (int side : new int[]{-1, 1}) {
            int tx = side < 0 ? cx - halfWall - towerW : cx + halfWall;
            Kit.stoneBox(c, tx, g, towerW, 22, towerH, 833 + side, true);
            merlons(c, tx, tx + towerW, g - towerH - 22, 835 + side);
            for (int s = 0; s < 2; s++) {                                     // arrow slits
                int sx = tx + towerW / 2 - 1, sy = g - 150 + s * 56;
                for (int y = 0; y < 16; y++) { c.set(sx, sy + y, Pal.INK); c.set(sx + 1, sy + y, Pal.INK); c.set(sx + 2, sy + y, Pal.STONE[0]); }
                c.hline(sx - 2, sx + 4, sy + 16, Pal.STONE[6]);
            }
            int bx = tx + (side < 0 ? 10 : towerW - 30), by = g - towerH + 14;       // a long banner on each tower
            for (int y = 0; y < 76; y++) for (int x = 0; x < 20; x++) {
                if (y > 66 && (x + y) % 3 == 0) continue;
                int colour = x <= 1 || x >= 18 ? Pal.GOLD[1] : x < 8 ? Pal.CRIMSON[3] : Pal.CRIMSON[2];
                int sxs = x - 10, sys = y - 30;
                double d = Math.hypot(sxs, sys);
                if (d <= 6 && (sxs == 0 || sys == 0 || Math.abs(sxs) == Math.abs(sys) || d <= 2.5)) colour = d <= 1.6 ? Pal.GOLD[4] : Pal.GOLD[2];
                c.set(bx + x, by + y, colour);
            }
            for (int x = bx - 2; x < bx + 22; x++) { c.set(x, by - 2, Pal.IRON[4]); c.set(x, by - 1, Pal.IRON[2]); }
            sconce(c, side < 0 ? cx - 44 : cx + 40, g - 96);
            for (int s = 0; s < 3; s++) {                                      // ivy up the tower foot
                int ix = tx + 6 + s * 9 + (side > 0 ? 30 : 0), len = 30 + s * 24;
                for (int y = 0; y < len; y++) c.tint(ix + (y / 5 % 2), g - 2 - y, y % 3 == 0 ? Pal.GRASS[3] : Pal.GRASS[2]);
            }
        }
        // the arch: a ring of wedge stones round the opening
        int archW = 28, spring = g - 56, archTop = spring - archW;
        for (int y = archTop - 7; y <= g; y++) for (int x = cx - archW - 7; x <= cx + archW + 7; x++) {
            double r = y < spring ? Math.hypot(x + .5 - cx, y + .5 - spring) : Math.abs(x + .5 - cx);
            if (r <= archW) c.set(x, y, open ? 0 : Pal.INK);
            else if (r <= archW + 7) {
                double a = Math.atan2(y + .5 - spring, x + .5 - cx);
                int wedge = (int) Math.floor((a + Math.PI) / (Math.PI / 11));
                c.set(x, y, r > archW + 6 ? Pal.STONE[1] : r < archW + 1 ? Pal.STONE[2] : wedge % 2 == 0 ? Pal.STONE[5] : Pal.STONE[4]);
            }
        }
        c.rect(cx - 5, archTop - 9, 10, 11, Pal.STONE[6]); c.rect(cx - 3, archTop - 6, 6, 5, Pal.CYAN[1]);
        c.set(cx, archTop - 4, Pal.CYAN[open ? 1 : 4]);
        for (int y = archTop; y <= g; y++) for (int x = cx - archW; x <= cx + archW; x++) {
            double r = y < spring ? Math.hypot(x + .5 - cx, y + .5 - spring) : Math.abs(x + .5 - cx);
            if (r > archW) continue;
            if (!open) {                                   // sealed oak doors, iron bands, studs
                int plank = Math.floorMod(x - cx, 7);
                int colour = plank == 0 ? Pal.WOOD[1] : plank == 1 ? Pal.WOOD[4] : plank == 6 ? Pal.WOOD[2] : Pal.WOOD[3];
                if (x == cx || x == cx - 1) colour = Pal.WOOD[0];
                int band = g - y;
                boolean iron = band >= 10 && band <= 12 || band >= 36 && band <= 38 || band >= 62 && band <= 64;
                if (iron) colour = band % 13 == 10 || band == 36 || band == 62 ? Pal.IRON[4] : x < cx ? Pal.IRON[3] : Pal.IRON[2];
                if (iron && Math.floorMod(x - cx, 8) == 4) colour = Pal.IRON[5];
                c.set(x, y, colour);
            } else {                                       // swung open: golden light spills down the road beyond
                double fromAxis = Math.abs(x + .5 - cx) / archW, up = (g - y) / 84.0;
                double light = 1 - fromAxis * .75 - up * .45 + (Noise.bayer(x, y) - .5) * .22;
                int colour = light > .8 ? Pal.GOLD[5] : light > .62 ? Pal.GOLD[4] : light > .44 ? Pal.GOLD[3] : light > .26 ? Pal.GOLD[2] : Pal.GOLD[1];
                if (y > g - 14 && Math.abs(x - cx) < 12 + (g - y) / 2) colour = (g - y + x) % 4 == 0 ? Pal.MUD[5] : Pal.MUD[4];
                if (Math.abs(x - cx) >= archW - 7) colour = Math.abs(x - cx) >= archW - 2 ? Pal.WOOD[1] : x < cx ? Pal.WOOD[3] : Pal.WOOD[2];
                c.set(x, y, colour);
            }
        }
        if (open) for (int x = cx - archW + 3; x <= cx + archW - 3; x += 5)     // the raised portcullis' teeth
            for (int y = archTop + 2; y < archTop + 10 + (Math.abs(x - cx) < 10 ? 4 : 0); y++) { c.set(x, y, Pal.IRON[2]); c.set(x + 1, y, Pal.IRON[1]); }
        if (!open) {                                       // the Warden's ward: a diamond eye across the seam
            int sy = g - 44;
            for (int y = -11; y <= 11; y++) for (int x = -14; x <= 14; x++) {
                int d = Math.abs(x) + Math.abs(y) * 14 / 11;
                if (d == 14 || d == 13) c.set(cx + x, sy + y, Pal.CYAN[4]);
                else if (d == 12) c.set(cx + x, sy + y, Pal.CYAN[2]);
                else if (d <= 5 && d >= 4) c.set(cx + x, sy + y, Pal.CYAN[3]);
            }
            c.rect(cx - 1, sy - 1, 2, 3, Pal.CYAN[5]);
        }
        c.blit(Decals.RUBBLE.art(), cx - 100, g - Decals.RUBBLE.art().h + 3);
        c.blit(Decals.TUFT_C.art(), cx + 74, g - Decals.TUFT_C.art().h + 3);
        c.blit(Decals.BOULDER.art(), cx + 104, g - Decals.BOULDER.art().h + 3);
        c.selectiveOutline();
        return c;
    }
    private static void merlons(Canvas c, int x0, int x1, int topY, int seed) {
        for (int x = x0; x < x1; x += 14) {
            if (Noise.rnd(x, 1, seed) < .2) continue;
            for (int y = topY - 11; y < topY; y++) for (int dx = 0; dx < 9 && x + dx < x1; dx++)
                c.set(x + dx, y, y == topY - 11 ? Pal.STONE[6] : y == topY - 10 ? Pal.STONE[5] : dx == 0 ? Pal.STONE[5] : dx == 8 ? Pal.STONE[3] : Pal.STONE[4]);
        }
    }
    private static void sconce(Canvas c, int x, int y) {
        Canvas s = Decals.ascii("..E..", ".EWQ.", "QWWWQ", ".QWQ.", "..Q..", "..Q..");
        c.blit(s, x - 2, y);
        c.set(x, y - 1, Pal.FIRE[3]); c.set(x - 1, y - 2, Pal.FIRE[3]); c.set(x, y - 2, Pal.FIRE[4]); c.set(x + 1, y - 2, Pal.FIRE[2]);
        c.set(x, y - 3, Pal.FIRE[5]); c.set(x, y - 4, Pal.FIRE[4]); c.set(x - 1, y - 5, Pal.FIRE[3]);
    }

    // ------------------------------------------------------------------ signal tower
    static Canvas signalTower() {
        var k = SetPieces.Kind.SIGNAL_TOWER;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, cx = k.anchorX, r = 34, top = g - 200;
        for (int x = cx - r; x < cx + r; x++) {
            double u = (x + .5 - cx) / r;
            int light = u < -.5 ? 5 : u < -.05 ? 5 : u < .4 ? 4 : u < .75 ? 3 : 2;
            int bulge = (int) Math.round(Math.sqrt(Math.max(0, 1 - u * u)) * 5);
            for (int y = top; y <= g + bulge - 5; y++) {
                int fromGround = g - y, course = fromGround / 10, inCourse = fromGround % 10;
                double ang = Math.asin(Math.max(-1, Math.min(1, u)));
                int joint = (int) Math.floor(ang / Math.PI * 9 + (course % 2) * .5);
                int prev = (int) Math.floor(Math.asin(Math.max(-1, Math.min(1, (x - .5 - cx) / r))) / Math.PI * 9 + (course % 2) * .5);
                int tone = Noise.rnd(joint * 7 + course * 13, course, 842) < .25 ? Math.max(2, light - 1) : light;
                int colour = inCourse == 9 || (joint != prev && Math.abs(u) < .94) ? Pal.STONE[1] : inCourse == 8 ? Pal.STONE[Math.min(7, tone + 1)]
                        : inCourse <= 1 ? Pal.STONE[tone - 1] : Pal.STONE[tone];
                c.set(x, y, colour);
            }
        }
        for (int y = top - 14; y < top; y++) for (int x = cx - r - 5; x < cx + r + 5; x++)       // parapet ring
            c.set(x, y, y == top - 14 ? Pal.STONE[6] : x < cx - 12 ? Pal.STONE[5] : x < cx + 16 ? Pal.STONE[4] : Pal.STONE[3]);
        c.hline(cx - r - 5, cx + r + 4, top, Pal.STONE[1]); c.hline(cx - r - 5, cx + r + 4, top + 1, Pal.STONE[2]);
        merlons(c, cx - r - 5, cx + r + 5, top - 14, 841);
        for (int y = top - 52; y < top - 14; y++) { c.set(cx, y, Pal.IRON[3]); c.set(cx + 1, y, Pal.IRON[2]); c.set(cx + 2, y, Pal.IRON[1]); }
        for (int y = 0; y < 18; y++) {                                                            // the unlit beacon cage
            int half = 11 - Math.abs(y - 9) / 2;
            int yy = top - 70 + y;
            c.set(cx - half, yy, Pal.IRON[4]); c.set(cx + half + 2, yy, Pal.IRON[1]);
            if (y % 4 == 0) c.hline(cx - half, cx + half + 2, yy, Pal.IRON[2]);
            for (int x = cx - half + 1; x <= cx + half + 1; x++) if (y > 4 && (x + y) % 4 != 0) c.set(x, yy, y < 11 ? Pal.WOOD[3] : Pal.WOOD[2]);
        }
        for (int y = g - 40; y <= g - 4; y++) for (int x = cx - 11; x <= cx + 11; x++) {          // door
            double rr = y < g - 29 ? Math.hypot(x + .5 - cx, y + .5 - (g - 29)) : Math.abs(x + .5 - cx);
            if (rr > 11) continue;
            c.set(x, y, rr > 9.5 ? Pal.STONE[6] : x < cx + 3 ? Pal.WOOD[x % 4 == 0 ? 1 : 2] : Pal.INK);
        }
        for (int s = 0; s < 3; s++) for (int y = 0; y < 13; y++) { c.set(cx - 12 + s * 12, g - 90 - s * 40 + y, Pal.INK); c.set(cx - 11 + s * 12, g - 90 - s * 40 + y, Pal.STONE[0]); }
        for (int s = 0; s < 4; s++) {
            int ix = cx - r + 3 + s * 4, len = 60 + s * 30;
            for (int y = 0; y < len; y++) c.tint(ix + (y / 5 % 2), g - 6 - y, y % 3 == 0 ? Pal.GRASS[3] : Pal.GRASS[2]);
        }
        c.blit(Decals.RUBBLE.art(), cx + 22, g - Decals.RUBBLE.art().h + 3); c.blit(Decals.TUFT_A.art(), cx - 42, g - Decals.TUFT_A.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ field infirmary
    static Canvas infirmary() {
        var k = SetPieces.Kind.INFIRMARY;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, x0 = k.anchorX - 82, w = 164, front = g - 72, back = front - 28;
        c.rect(x0 + 2, back + 2, w - 4, g - back - 2, Pal.CANVAS[0]);                       // shadowed interior
        for (int y = back + 2; y < g; y++) for (int x = x0 + 2; x < x0 + w - 2; x++) if (Noise.bayer(x, y) < (y - back) / 140.0) c.set(x, y, Pal.MUD[1]);
        for (int cot = 0; cot < 3; cot++) {
            int bx = x0 + 12 + cot * 52;
            c.rect(bx, g - 20, 40, 9, Pal.CANVAS[4]); c.hline(bx, bx + 39, g - 20, Pal.CANVAS[5]);
            c.rect(bx + 14, g - 19, 26, 8, cot == 1 ? Pal.CANVAS[3] : Pal.CRIMSON[3]);
            c.hline(bx + 14, bx + 39, g - 19, cot == 1 ? Pal.CANVAS[4] : Pal.CRIMSON[4]);
            for (int x = bx + 16; x < bx + 40; x += 6) c.set(x, g - 15, cot == 1 ? Pal.CANVAS[2] : Pal.CRIMSON[2]);
            c.rect(bx + 2, g - 19, 9, 5, Pal.CANVAS[5]); c.hline(bx + 2, bx + 10, g - 15, Pal.CANVAS[3]);
            c.hline(bx, bx + 39, g - 11, Pal.WOOD[3]); c.hline(bx, bx + 39, g - 10, Pal.WOOD[1]);
            for (int lx : new int[]{bx, bx + 38}) { c.vline(lx, g - 11, g - 3, Pal.WOOD[3]); c.vline(lx + 1, g - 11, g - 3, Pal.WOOD[1]); }
        }
        Kit.poly(c, Pal.CANVAS[3], x0 - 7, front, x0 + 6, back, x0 + w - 6, back, x0 + w + 7, front);   // canvas roof
        for (int x = x0 - 6; x < x0 + w + 6; x++) {
            if (Math.floorMod(x - x0, 20) < 10) c.line(x, front, x + 6, back + 1, Pal.CANVAS[4]);
            c.set(x, back + 1, Pal.CANVAS[5]);
        }
        for (int x = x0 - 7; x < x0 + w + 7; x++) {                                             // scalloped valance
            int m = Math.floorMod(x - x0, 16), drop = 7 + (m < 8 ? m / 2 : (15 - m) / 2);
            for (int y = front; y < front + drop; y++) c.set(x, y, y == front ? Pal.CANVAS[5] : y == front + drop - 1 ? Pal.CANVAS[1] : Math.floorMod(x - x0, 32) < 16 ? Pal.CANVAS[4] : Pal.CRIMSON[3]);
        }
        for (int px : new int[]{x0 - 4, x0 + 52, x0 + 108, x0 + w}) Structures.post(c, px, front + 6, g, 5);
        for (int y = 0; y < 30; y++) for (int x = 0; x < 10; x++)                             // the expedition's pale sash as a sign
            c.set(x0 + 76 + x, front + 10 + y, x <= 1 ? Pal.CANVAS[5] : y % 9 == 4 ? Pal.CRIMSON[3] : Pal.CANVAS[4]);
        Canvas crate = Decals.ascii(
                "vvvvvvvvvvvv",
                "cbbbbbbbbbbx",
                "cvOOOOOOOOvx",
                "cvOOOUUOOOvx",
                "cvOOUUUUOOvx",
                "cvOOOUUOOOvx",
                "xxxxxxxxxxxz");
        c.blit(crate, x0 + w + 8, g - crate.h + 1);
        c.line(x0 - 6, front + 1, x0 - 20, g + 2, Pal.DRY[2]); c.line(x0 + w + 6, front + 1, x0 + w + 20, g - 9, Pal.DRY[1]);
        c.blit(Decals.TUFT_C.art(), x0 - 26, g - Decals.TUFT_C.art().h + 3);
        c.selectiveOutline();
        return c;
    }
}
