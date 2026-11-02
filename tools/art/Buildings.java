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
    /**
     * The gatehouse at the Outpost's exit. Two towers stand forward of a recessed curtain wall, so the
     * left tower shades it and both show their inner sides. Frame 0 is sealed by the Warden's ward;
     * frames 1..7 part the doors, then lift the portcullis off a dark passage.
     */
    static Canvas eastGate(int frame) {
        double opening = Math.max(0, Math.min(7, frame)) / 7.0;
        boolean open = frame > 0;
        var k = SetPieces.Kind.EAST_GATE;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, cx = k.anchorX;
        int towerW = 68, recess = 8, foot = g - recess, wallTop = foot - 149, walk = wallTop - 18;
        int x0 = cx - 128 + towerW + 4, x1 = cx + 128 - towerW - 5;           // curtain face between the towers' side faces
        int R = 28, spring = foot - 56, archTop = spring - R;

        // curtain wall: face, plinth, wall-walk and its back parapet
        gateAshlar(c, x0, x1, wallTop, foot - 10, 831, -1);
        gateAshlar(c, x0, x1, foot - 8, foot, 838, -1);
        c.hline(x0, x1, foot - 9, Pal.STONE[5]);
        gateFlags(c, x0, x1, walk, wallTop - 3, 832);
        c.hline(x0, x1, wallTop - 2, Pal.STONE[6]); c.hline(x0, x1, wallTop - 1, Pal.STONE[4]);   // coping lip
        gateBattlement(c, x0, x1, walk, 833);
        for (int x = x0; x <= x1; x++) for (int d = 0; d < 4; d++) gateShade(c, x, wallTop + d, d < 2 ? -2 : -1);
        gateArch(c, cx, spring, foot, R);

        for (int side : new int[]{-1, 1}) {
            int tx = side < 0 ? cx - 128 : cx + 128 - towerW;
            boolean[] joints = gateTower(c, tx, towerW, g, spring - 3, side, 834 + side);
            gateSideFace(c, side < 0 ? tx + towerW : tx - 1, g, spring - 3, joints, side);
        }

        // weather: rain runs off the lip, grime climbs from the ground, moss holds the joints
        for (int s = 0; s < 16; s++) {
            int sx = x0 + 3 + (int) (Noise.rnd(s, 1, 839) * (x1 - x0 - 6));
            if (Math.abs(sx - cx) < R + 12 && Noise.rnd(s, 2, 839) < .6) continue;
            gateStreak(c, sx, wallTop + 2, 10 + (int) (Noise.rnd(s, 3, 839) * 46));
        }
        gateGrime(c, x0, x1, foot, 840);
        gateLedgeMoss(c, x0, x1, foot - 9, 841);
        gateLedgeMoss(c, x0, x1, wallTop - 2, 842);
        gateJointMoss(c, x0, x1, foot - 26, foot, 843);
        for (int[] v : new int[][]{{x0 + 7, 22}, {x0 + 19, 38}, {cx + 43, 30}, {x1 - 6, 16}}) gateVine(c, v[0], wallTop, v[1], v[0]);
        for (int y = walk - 12; y <= foot; y++) for (int x = x0; x < x0 + 8; x++) gateShade(c, x, y, x < x0 + 4 ? -2 : -1);   // left tower's shadow
        for (int y = walk; y <= foot; y++) gateShade(c, x1, y, -1);

        // torches either side of the arch, their light stepped into the stone
        for (int side : new int[]{-1, 1}) {
            int tx = cx + side * 44, ty = spring - 26;
            gateWarmth(c, tx, ty - 8, x0 - 4, x1 + 4);
            gateTorch(c, tx, ty);
        }

        // the opening: doors, portcullis, and the passage behind them
        int doorWidth = (int) Math.round(R * (1 - opening * .88));
        int lift = (int) Math.round(Math.max(0, (opening - .15) / .85) * 80);
        int bottom = foot - lift;
        for (int y = archTop; y <= foot; y++) for (int x = cx - R; x <= cx + R; x++) {
            double r = y < spring ? Math.hypot(x + .5 - cx, y + .5 - spring) : Math.abs(x + .5 - cx);
            if (r > R) continue;
            int depth = foot - y, side = Math.abs(x - cx);
            int colour = Pal.INK;
            if (side >= R - 3 && depth < 64) colour = depth % 9 == 8 ? Pal.INK : depth < 24 ? Pal.STONE[1] : Pal.STONE[0];   // passage walls
            if (depth < 40 && side < 24 - depth / 2) {                                         // the road runs on into the dark
                colour = depth < 12 ? Pal.MUD[3] : depth < 26 ? Pal.MUD[2] : Pal.MUD[1];
                if (depth == 12 || depth == 26) colour = Pal.MUD[1];
            }
            if (depth <= 1) colour = depth == 1 ? Pal.STONE[5] : Pal.STONE[3];                 // threshold
            if (side > R - doorWidth) {
                int u = (R - side) * R / Math.max(1, doorWidth), plank = u % 7;
                colour = plank == 0 ? Pal.WOOD[0] : plank == 1 ? Pal.WOOD[4] : plank == 6 ? Pal.WOOD[1] : u / 7 % 2 == 0 ? Pal.WOOD[3] : Pal.WOOD[2];
                if (Noise.rnd(x, y / 3, 851) < .07) colour = Pal.shift(colour, -1);
                for (int band : new int[]{10, 36, 62}) if (depth >= band && depth <= band + 2) {
                    colour = depth == band + 2 ? Pal.IRON[3] : depth == band + 1 ? Pal.IRON[2] : Pal.IRON[1];
                    if (depth == band + 1 && Math.floorMod(R - side, 5) == 2) colour = Pal.IRON[5];
                }
                if (open) colour = Pal.shift(colour, -1);                                     // leaves turned from the light
            }
            if (y <= bottom) {
                int bar = Math.floorMod(x - cx, 7);
                boolean cross = y < bottom - 3 && Math.floorMod(y + lift - archTop, 17) < 2;
                if (bar < 2 && (y < bottom - 1 || bar == 0)) colour = y >= bottom - 1 ? Pal.IRON[3] : bar == 0 ? Pal.IRON[4] : Pal.IRON[1];
                if (cross) colour = bar == 0 ? Pal.IRON[5] : Math.floorMod(y + lift - archTop, 17) == 0 ? Pal.IRON[3] : Pal.IRON[1];
            }
            c.set(x, y, colour);
        }
        gateKeystone(c, cx, archTop, open);
        if (!open) {                                       // the Warden's ward: a diamond eye across the seam
            int sy = foot - 44;
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
        c.blit(Decals.ROCK_S.art(), x0 + 2, foot - Decals.ROCK_S.art().h + 4);
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
    private static boolean gateStone(int colour) {
        for (int s : Pal.STONE) if (s == colour) return true;
        return false;
    }
    /** Darkens (or lightens) a stone pixel by whole ramp steps; anything else is left alone. */
    private static void gateShade(Canvas c, int x, int y, int steps) {
        int colour = c.get(x, y);
        if (gateStone(colour)) c.set(x, y, Pal.shift(colour, steps));
    }

    /**
     * Gate-only ashlar: courses of uneven height, blocks of uneven length (some big), each bevelled
     * toward the moonlight, with chipped arrises, weathered patches and a few hairline cracks.
     * Returns which rows are course joints, so quoins and side faces can follow the coursing.
     */
    private static boolean[] gateAshlar(Canvas c, int x0, int x1, int top, int bottom, int seed, int bias) {
        boolean[] joints = new boolean[c.h + 1];
        int y = bottom;
        for (int course = 0; y >= top; course++) {
            int ch = 8 + (int) (Noise.rnd(course, 0, seed) * 4), courseTop = y - ch + 1;
            if (y >= 0 && y < joints.length) joints[y] = true;
            int x = x0 - (int) (Noise.rnd(course, 1, seed) * 14);
            for (int block = 0; x <= x1; block++) {
                int bw = 9 + (int) (Noise.rnd(course, block + 2, seed) * 14);
                if (Noise.rnd(course, block + 40, seed) < .12) bw += 8;                     // the odd big stone
                double r = Noise.rnd(course * 31 + block, 3, seed);
                int t = Math.max(2, Math.min(5, (r < .22 ? 3 : r > .9 ? 5 : 4) + bias));
                boolean chipTop = Noise.rnd(course, block, seed + 1) < .3, chipFoot = Noise.rnd(course, block, seed + 2) < .2;
                for (int py = Math.max(top, courseTop); py <= y; py++) for (int px = Math.max(x, x0); px < Math.min(x + bw, x1 + 1); px++) {
                    int lx = px - x, ly = py - courseTop, tone;
                    if (py == y || lx == 0) tone = 1;
                    else if (chipTop && lx >= bw - 3 && ly <= 1 && lx + ly >= bw - 2) tone = 1;   // a bitten corner
                    else if (chipFoot && lx <= 3 && py == y - 1) tone = t - 2;
                    else if (ly == 0 || lx == 1) tone = t + 1;
                    else if (py == y - 1 || lx == bw - 1) tone = t - 1;
                    else {
                        double n = Noise.value(px, py, 4, seed + 9);
                        tone = n > .76 ? t + 1 : n < .2 ? t - 1 : t;
                        if (Noise.rnd(px, py, seed + 5) < .006) tone = t - 1;              // pitting
                    }
                    c.set(px, py, Pal.STONE[Math.max(1, Math.min(7, tone))]);
                }
                x += bw;
            }
            y -= ch;
        }
        for (int s = 0; s < (x1 - x0) / 30 + 1; s++) {                                   // hairline cracks
            int cx = x0 + 4 + (int) (Noise.rnd(s, 7, seed) * Math.max(1, x1 - x0 - 8));
            int cy = top + 6 + (int) (Noise.rnd(s, 8, seed) * Math.max(1, bottom - top - 30));
            int len = 8 + (int) (Noise.rnd(s, 9, seed) * 16);
            for (int i = 0; i < len && cy <= bottom - 2; i++) {
                if (!gateStone(c.get(cx, cy))) break;
                c.set(cx, cy, Pal.STONE[1]);
                int lip = c.get(cx + 1, cy);
                if (gateStone(lip) && lip != Pal.STONE[1]) c.set(cx + 1, cy, Pal.shift(lip, 1));
                double step = Noise.rnd(s * 50 + i, 10, seed);
                if (step < .3) cx--; else if (step > .72) cx++;
                cy++;
            }
        }
        return joints;
    }

    /** A top face seen from above: flagstones in rows, joints holding moss that thickens at the back. */
    private static void gateFlags(Canvas c, int x0, int x1, int y0, int y1, int seed) {
        for (int y = y0; y <= y1; y++) {
            int row = (y - y0) / 5, inRow = (y - y0) % 5;
            int x = x0 - (int) (Noise.rnd(row, 0, seed) * 12);
            for (int slab = 0; x <= x1; slab++) {
                int sw = 8 + (int) (Noise.rnd(row, slab + 1, seed) * 9);
                int tone = Noise.rnd(row * 17 + slab, 2, seed) < .3 ? 4 : Noise.rnd(row * 17 + slab, 3, seed) < .15 ? 6 : 5;
                for (int px = Math.max(x, x0); px < Math.min(x + sw, x1 + 1); px++) {
                    int lx = px - x;
                    int colour = inRow == 4 || lx == 0 ? Pal.STONE[3] : inRow == 0 || lx == 1 ? Pal.STONE[Math.min(6, tone + 1)] : Pal.STONE[tone];
                    c.set(px, y, colour);
                }
                x += sw;
            }
        }
        boolean[] grow = new boolean[(x1 - x0 + 1) * (y1 - y0 + 1)];
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            boolean joint = c.get(x, y) == Pal.STONE[3];
            double n = Noise.fbm(x, y * 1.7, 6, 2, seed + 4), need = y - y0 < 3 ? .45 : joint ? .56 : .7;
            grow[(y - y0) * (x1 - x0 + 1) + (x - x0)] = n > need;
        }
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {                   // moss cushions: lit crowns, dark undersides
            int w = x1 - x0 + 1, i = (y - y0) * w + (x - x0);
            if (!grow[i]) continue;
            boolean above = y > y0 && grow[i - w], below = y < y1 && grow[i + w];
            c.set(x, y, !above ? Pal.GRASS[4] : !below ? Pal.GRASS[2] : Pal.GRASS[3]);
        }
    }

    /** Back parapet of a wall-walk: a low wall, merlons with lit caps, each throwing its shadow forward. */
    private static void gateBattlement(Canvas c, int x0, int x1, int floorY, int seed) {
        for (int x = x0; x <= x1; x++) {
            c.set(x, floorY - 3, Pal.STONE[6]); c.set(x, floorY - 2, Pal.STONE[4]); c.set(x, floorY - 1, Pal.STONE[3]);
            gateShade(c, x, floorY, -1);
        }
        for (int x = x0 + 2, m = 0; x + 8 <= x1; x += 14, m++) {
            boolean broken = Noise.rnd(m, 1, seed) < .18;
            for (int dx = 0; dx < 9; dx++) {
                int top = floorY - 12 + (broken && dx > 4 ? 3 + (dx - 4) / 2 : 0);
                for (int y = top; y < floorY - 3; y++) {
                    int ly = y - top;
                    int colour = ly == 0 ? Pal.STONE[6] : ly == 1 ? Pal.STONE[5] : dx == 0 ? Pal.STONE[5] : dx == 8 ? Pal.STONE[2]
                            : y == floorY - 7 ? Pal.STONE[2] : Pal.STONE[4];
                    c.set(x + dx, y, colour);
                }
            }
            for (int d = 0; d < 3; d++) for (int dx = 2 + d; dx < 11 + d; dx++) gateShade(c, x + dx, floorY + d, -1);
        }
    }

    /** Voussoir ring round a deep reveal whose inner faces sit in shadow, on block-and-start jambs and moulded imposts. */
    private static void gateArch(Canvas c, int cx, int spring, int foot, int R) {
        int ring = R + 11;
        for (int y = spring - ring - 1; y <= foot; y++) for (int x = cx - R - 16; x <= cx + R + 16; x++) {
            double dx = x + .5 - cx, dy = y + .5 - spring;
            boolean above = y < spring;
            double r = above ? Math.hypot(dx, dy) : Math.abs(dx);
            if (r <= R) continue;
            double u = (Math.atan2(dy, dx) + Math.PI) / (Math.PI / 15), f = u - Math.floor(u);
            boolean radial = above && f * Math.PI / 15 * r < 1;
            int colour;
            if (r <= R + 4) {                                                       // the reveal: the wall's thickness
                if (above) colour = r <= R + 1 || radial ? Pal.STONE[0] : dx > R * .45 ? Pal.STONE[2] : Pal.STONE[1];
                else {
                    boolean joint = (y - spring) % 9 == 8;
                    colour = dx < 0 ? (joint || r <= R + 1 ? Pal.STONE[0] : Pal.STONE[1]) : joint ? Pal.STONE[1] : r <= R + 1 ? Pal.STONE[2] : Pal.STONE[3];
                }
            } else if (above && r <= ring) {                                        // voussoirs, each bevelled to the light
                if (radial || r > ring - 1) colour = Pal.STONE[1];
                else {
                    int t = Noise.rnd((int) Math.floor(u), 0, 861) < .35 ? 5 : 4;
                    if (f * Math.PI / 15 * r < 2 || r <= R + 5) t++;
                    else if ((1 - f) * Math.PI / 15 * r < 2 || r > ring - 2) t--;
                    colour = Pal.STONE[t];
                }
            } else if (!above && r <= ring + ((y - spring) / 9 % 2 == 0 ? 4 : 0)) {  // jambs: long and short stones
                int inCourse = (y - spring) % 9, outer = ring + ((y - spring) / 9 % 2 == 0 ? 4 : 0);
                if (inCourse == 8 || r > outer - 1) colour = Pal.STONE[1];
                else {
                    int t = (y - spring) / 9 % 2 == 0 ? 5 : 4;
                    if (inCourse == 0 || r <= R + 5) t++;
                    else if (inCourse == 7 || r > outer - 2) t--;
                    colour = Pal.STONE[t];
                }
            } else continue;
            c.set(x, y, colour);
        }
        for (int s : new int[]{-1, 1}) for (int i = R + 3; i <= ring + 5; i++) {    // imposts at the springing
            int x = s < 0 ? cx - 1 - i : cx + i;
            c.set(x, spring - 3, Pal.STONE[6]); c.set(x, spring - 2, Pal.STONE[5]); c.set(x, spring - 1, Pal.STONE[2]);
            gateShade(c, x, spring, -1);
        }
    }

    /** The keystone stands proud of the ring and holds the ward's gem, lit while the gate is sealed. */
    private static void gateKeystone(Canvas c, int cx, int archTop, boolean open) {
        int top = archTop - 17, bottom = archTop - 1;
        for (int y = top - 1; y <= bottom; y++) {
            int half = 8 - Math.max(0, y - top) * 4 / (bottom - top);
            for (int x = cx - half - 1; x <= cx + half; x++) {
                int colour = y == top - 1 || x == cx - half - 1 || x == cx + half ? Pal.STONE[1]
                        : y == top ? Pal.STONE[7] : y == bottom ? Pal.STONE[2] : x == cx - half ? Pal.STONE[6] : x == cx + half - 1 ? Pal.STONE[3] : Pal.STONE[5];
                c.set(x, y, colour);
            }
        }
        int gy = archTop - 8;
        for (int y = gy - 3; y <= gy + 3; y++) for (int x = cx - 4; x <= cx + 3; x++) c.set(x, y, y == gy - 3 ? Pal.STONE[2] : Pal.STONE[3]);
        for (int y = gy - 2; y <= gy + 2; y++) for (int x = cx - 3; x <= cx + 2; x++) {
            double d = Math.abs(x + .5 - cx) + Math.abs(y - gy);
            if (d > 2.5) continue;
            c.set(x, y, d >= 2 ? Pal.CYAN[open ? 0 : 2] : d >= 1 ? Pal.CYAN[open ? 1 : 3] : Pal.CYAN[open ? 1 : 5]);
        }
    }

    /** A flanking tower: battered foot, quoined corners, string course, banner and slits under a machicolated crown. */
    private static boolean[] gateTower(Canvas c, int tx, int tw, int g, int stringY, int side, int seed) {
        int t1 = tx + tw - 1, crown = g - 179;                                       // crown: first row of the corbel band
        boolean[] joints = gateAshlar(c, tx, t1, crown + 6, g - 15, seed, 0);
        boolean[] talus = gateAshlar(c, tx - 3, t1 + 3, g - 13, g, seed + 3, 1);    // battered foot flares outward
        for (int y = g - 13; y <= g; y++) {
            int flare = (y - (g - 13)) / 5;
            for (int x = tx - 3; x <= t1 + 3; x++) if (side < 0 ? x < tx - flare || x > t1 : x > t1 + flare || x < tx) c.clear(x, y);
            joints[y] = talus[y];
        }
        for (int x = tx - 1; x <= t1 + 1; x++) { c.set(x, g - 14, Pal.STONE[6]); gateShade(c, x, g - 13, 1); }
        java.util.List<Integer> rows = new java.util.ArrayList<>();                   // quoins follow the coursing
        for (int y = g - 15; y >= crown + 6; y--) if (joints[y]) rows.add(y);
        rows.add(crown + 5);
        for (int q = 0; q + 1 < rows.size(); q++) {
            int jb = rows.get(q), jt = rows.get(q + 1);
            for (int corner = 0; corner < 2; corner++) {
                int len = (q + corner) % 2 == 0 ? 13 : 7, qx0 = corner == 0 ? tx : t1 - len + 1, qx1 = corner == 0 ? tx + len - 1 : t1;
                for (int y = jt + 1; y < jb; y++) for (int x = qx0; x <= qx1; x++) {
                    int colour = (corner == 0 ? x == qx1 : x == qx0) ? Pal.STONE[1] : y == jt + 1 ? Pal.STONE[6] : y == jb - 1 ? Pal.STONE[3]
                            : (corner == 0 ? x == qx0 : x == qx0 + 1) ? Pal.STONE[6] : x == qx1 && corner == 1 ? Pal.STONE[4]
                            : Noise.value(x, y, 3, seed + 6) > .74 ? Pal.STONE[6] : Pal.STONE[5];
                    c.set(x, y, colour);
                }
            }
        }
        for (int x = tx - 1; x <= t1 + 1; x++) {                                       // string course at the springing line
            c.set(x, stringY, Pal.STONE[6]); c.set(x, stringY + 1, Pal.STONE[5]); c.set(x, stringY + 2, Pal.STONE[2]);
            gateShade(c, x, stringY + 3, -1);
        }
        // crown: corbels carry a projecting parapet; behind it the roof, merlons at the back
        for (int x = tx; x <= t1; x++) for (int y = crown; y < crown + 6; y++) c.set(x, y, y < crown + 3 ? Pal.INK : Pal.STONE[0]);
        String[] corbel = {"66553", "54442", "54332", ".543.", ".432.", ".321."};
        for (int x = tx + 1; x + 4 <= t1; x += 8) for (int r = 0; r < 6; r++) for (int i = 0; i < 5; i++)
            if (corbel[r].charAt(i) != '.') c.set(x + i, crown + r, Pal.STONE[corbel[r].charAt(i) - '0']);
        for (int x = tx; x <= t1; x++) for (int d = 0; d < 3; d++) gateShade(c, x, crown + 6 + d, d < 2 ? -2 : -1);
        for (int x = tx - 2, b = 0; x <= t1 + 2; b++) {                              // parapet: one course of big blocks
            int bw = 11 + (int) (Noise.rnd(b, 1, seed + 4) * 6), t = Noise.rnd(b, 2, seed + 4) < .3 ? 3 : 4;
            for (int px = x; px < Math.min(x + bw, t1 + 3); px++) for (int y = crown - 10; y < crown; y++) {
                int colour = y == crown - 1 || px == x ? Pal.STONE[1] : y == crown - 10 || px == x + 1 ? Pal.STONE[t + 1] : y == crown - 2 || px == x + bw - 1 ? Pal.STONE[t - 1] : Pal.STONE[t];
                c.set(px, y, colour);
            }
            x += bw;
        }
        for (int x = tx - 2; x <= t1 + 2; x++) { c.set(x, crown - 13, Pal.STONE[6]); c.set(x, crown - 12, Pal.STONE[5]); c.set(x, crown - 11, Pal.STONE[4]); }
        gateFlags(c, tx + 1, t1 - 1, crown - 22, crown - 14, seed + 6);
        for (int y = crown - 25; y <= crown - 14; y++) {
            c.set(tx - 2, y, Pal.STONE[5]); c.set(tx - 1, y, Pal.STONE[6]); c.set(tx, y, Pal.STONE[3]);
            c.set(t1, y, Pal.STONE[5]); c.set(t1 + 1, y, Pal.STONE[6]); c.set(t1 + 2, y, Pal.STONE[4]);
            if (y >= crown - 22) { gateShade(c, tx + 1, y, -2); gateShade(c, tx + 2, y, -1); }
        }
        gateBattlement(c, tx - 2, t1 + 2, crown - 22, seed + 7);
        gateLedgeMoss(c, tx - 2, t1 + 2, crown - 13, seed + 8);

        int mid = tx + tw / 2 - 1, bannerX = tx + (side < 0 ? 10 : tw - 30);
        for (int s = 0; s < 2; s++) gateSlit(c, mid, g - 149 + s * 56);
        for (int s = 0; s < 6; s++) {                                                  // rain off the machicolations
            int sx = tx + 6 + (int) (Noise.rnd(s, 1, seed + 9) * (tw - 12));
            if (sx >= bannerX - 2 && sx < bannerX + 22) continue;
            gateStreak(c, sx, crown + 6, 12 + (int) (Noise.rnd(s, 2, seed + 9) * 60));
        }
        gateGrime(c, tx - 3, t1 + 3, g, seed + 10);
        gateLedgeMoss(c, tx - 1, t1 + 1, stringY, seed + 11);
        gateLedgeMoss(c, tx - 1, t1 + 1, g - 14, seed + 12);
        gateJointMoss(c, tx - 3, t1 + 3, g - 34, g, seed + 13);
        gateBanner(c, bannerX, crown + 14);
        for (int[] v : side < 0 ? new int[][]{{tx + 4, 46}, {t1 - 7, 24}} : new int[][]{{tx + 6, 30}, {t1 - 3, 58}}) gateVine(c, v[0], crown + 4, v[1], v[0] + seed);
        gateIvy(c, side < 0 ? tx + 12 : t1 - 22, g, seed + 14);
        return joints;
    }

    /** The tower's inner side face, seen past its corner: four columns stepping back to the recessed wall. */
    private static void gateSideFace(Canvas c, int edgeX, int g, int stringY, boolean[] joints, int side) {
        boolean lit = side > 0;                                                         // the right tower's side faces the moon
        int crown = g - 179;
        for (int i = 0; i < 4; i++) {
            int x = edgeX - side * i, rise = 2 * (i + 1), bottom = g - rise;
            for (int y = crown; y <= bottom; y++) {
                int sy = y + rise, colour;
                if (y < crown + 6) colour = Pal.STONE[0];
                else if (sy >= stringY && sy <= stringY + 2) colour = Pal.STONE[(lit ? 6 : 3) - (sy - stringY)];
                else if (sy < joints.length && joints[sy]) colour = Pal.STONE[lit ? 3 : 1];
                else colour = Pal.STONE[(lit ? 5 : 2) + (i == 0 ? 1 : 0) + (sy > g - 14 ? 1 : 0)];
                if (sy > g - 18 && y > crown + 6) colour = Pal.shift(colour, -1);
                c.set(x, y, colour);
            }
            for (int d = 0; d < 3; d++) gateShade(c, x, crown + 6 + d, -1);
        }
    }

    /** A cross-loop in a dressed surround; its sill sheds a rain streak down the tower. */
    private static void gateSlit(Canvas c, int x, int y) {
        for (int dy = -1; dy <= 16; dy++) { c.set(x - 1, y + dy, Pal.STONE[3]); c.set(x + 3, y + dy, Pal.STONE[5]); }
        c.hline(x - 1, x + 3, y - 2, Pal.STONE[5]);
        for (int dy = 0; dy < 16; dy++) { c.set(x, y + dy, Pal.INK); c.set(x + 1, y + dy, Pal.INK); c.set(x + 2, y + dy, Pal.STONE[0]); }
        for (int dx = -3; dx <= 5; dx++) if (dx < 0 || dx > 2) c.set(x + dx, y + 5, dx > 2 ? Pal.STONE[0] : Pal.INK);
        c.hline(x - 2, x + 4, y + 16, Pal.STONE[6]); c.hline(x - 2, x + 4, y + 17, Pal.STONE[3]);
        gateStreak(c, x + 1, y + 18, 16);
    }

    /** A long banner on an iron rod: soft folds catch the moon, the sun device bends with them, and it shades the wall. */
    private static void gateBanner(Canvas c, int bx, int by) {
        int w = 20, h = 76;
        int[] cloth = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double fold = Math.cos((x + .5 - 3) * 2 * Math.PI / (6.2 + y * .02) + .7 * Math.sin(x * .33) - y * .012) + (w / 2.0 - x) * .05;
            int hem = h - 1 - (int) Math.max(0, 8 - Math.abs(x + .5 - w / 2.0) * 1.5) + (fold > .5 ? 1 : 0);   // swallowtail
            if (y > hem) continue;
            int shade = fold > .55 ? 4 : fold > -.2 ? 3 : fold > -.75 ? 2 : 1;
            int colour = x <= 1 || x >= w - 2 || y <= 2 || y >= hem - 2 ? Pal.GOLD[shade - 1] : Pal.CRIMSON[shade];
            int sx = x - 10, sy = y - 30;
            double d = Math.hypot(sx, sy);
            if (d <= 6 && (sx == 0 || sy == 0 || Math.abs(sx) == Math.abs(sy) || d <= 2.5)) colour = d <= 1.6 ? Pal.GOLD[4] : Pal.GOLD[Math.max(1, shade - 1)];
            cloth[y * w + x] = colour;
        }
        for (int y = 0; y < h + 2; y++) for (int x = 0; x < w + 2; x++) {             // its shadow, down and right
            boolean over = x < w && y < h && cloth[y * w + x] != 0, caster = x >= 2 && y >= 2 && x - 2 < w && y - 2 < h && cloth[(y - 2) * w + x - 2] != 0;
            if (caster && !over) gateShade(c, bx + x, by + y, -2);
        }
        for (int i = 0; i < cloth.length; i++) if (cloth[i] != 0) c.set(bx + i % w, by + i / w, cloth[i]);
        for (int x = bx - 2; x < bx + w + 2; x++) { c.set(x, by - 2, Pal.IRON[4]); c.set(x, by - 1, Pal.IRON[2]); }
        c.set(bx - 3, by - 2, Pal.IRON[5]); c.set(bx - 3, by - 1, Pal.IRON[3]); c.set(bx + w + 2, by - 2, Pal.IRON[4]); c.set(bx + w + 2, by - 1, Pal.IRON[1]);
    }

    /** Rain runs down from a ledge: a dark wet line that breaks up toward its end. */
    private static void gateStreak(Canvas c, int x, int y, int len) {
        for (int i = 0; i < len; i++) if (i < len * 2 / 3 || i % 3 != 2) gateShade(c, x, y + i, -1);
    }

    /** Splash-back grime: a ragged dark band up from the ground, darkest at the foot. */
    private static void gateGrime(Canvas c, int x0, int x1, int foot, int seed) {
        for (int x = x0; x <= x1; x++) {
            int h = 6 + (int) (Noise.value(x, 0, 6, seed) * 12);
            for (int y = foot - h; y <= foot; y++) gateShade(c, x, y, y > foot - 3 ? -2 : -1);
        }
    }

    /** Moss cushions along a ledge where rain sits, a few strands dripping over its edge. */
    private static void gateLedgeMoss(Canvas c, int x0, int x1, int y, int seed) {
        for (int x = x0; x <= x1; x++) {
            double n = Noise.fbm(x, y, 5, 2, seed);
            if (n < .52 || !c.opaque(x, y)) continue;
            c.set(x, y, n > .62 ? Pal.GRASS[4] : Pal.GRASS[3]);
            if (n > .58) c.tint(x, y + 1, Pal.GRASS[2]);
            if (n > .66 && Noise.rnd(x, y, seed) < .6) for (int d = 2; d < 2 + (int) (Noise.rnd(x, 1, seed) * 5); d++) c.tint(x, y + d, Pal.GRASS[1]);
            if (n > .7) c.tint(x, y - 1, Pal.GRASS[3]);
        }
    }

    /** Moss filling the mortar near the ground, creeping onto the stone above. */
    private static void gateJointMoss(Canvas c, int x0, int x1, int y0, int y1, int seed) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            int colour = c.get(x, y);
            if (colour != Pal.STONE[1] && colour != Pal.STONE[0]) continue;
            double n = Noise.fbm(x, y, 6, 2, seed) + (y - y0) / (double) Math.max(1, y1 - y0) * .18;
            if (n > .62) { c.set(x, y, Pal.GRASS[1]); if (n > .7) c.tint(x, y - 1, Pal.GRASS[2]); }
        }
    }

    /** A vine hanging from the battlements, leaves in pairs that alternate sides. */
    private static void gateVine(Canvas c, int x, int y, int len, int seed) {
        for (int i = 0; i < len; i++) {
            int vx = x + (int) Math.round(Math.sin(i * .17 + seed) * 1.4), vy = y + i;
            c.tint(vx, vy, Pal.GRASS[1]);
            if (i % 4 == 1 && i < len - 1) {
                int dir = i / 4 % 2 == 0 ? -1 : 1;
                c.tint(vx + dir, vy, Pal.GRASS[3]); c.tint(vx + dir * 2, vy, Pal.GRASS[2]);
                c.tint(vx + dir, vy - 1, Pal.GRASS[4]); c.tint(vx + dir, vy + 1, Pal.GRASS[2]);
            }
        }
    }

    /** Ivy climbing from the ground: three stems, leaves thinning as they rise. */
    private static void gateIvy(Canvas c, int x, int g, int seed) {
        for (int s = 0; s < 3; s++) {
            int len = 16 + (int) (Noise.rnd(s, 1, seed) * 34), sx = x + s * 6;
            for (int i = 0; i < len; i++) {
                int vx = sx + (int) Math.round(Math.sin(i * .21 + s * 2 + seed) * 1.6), vy = g - 1 - i;
                c.tint(vx, vy, Pal.GRASS[1]);
                if (i % 3 == 0 && (i < len / 2 || i % 6 == 0)) {
                    int dir = (i / 3 + s) % 2 == 0 ? -1 : 1;
                    c.tint(vx + dir, vy, Pal.GRASS[3]); c.tint(vx + dir * 2, vy, Pal.GRASS[2]); c.tint(vx + dir, vy - 1, Pal.GRASS[4]);
                }
            }
        }
    }

    /** Torchlight on stone, stepped through warm ramps by distance: no blur, every pixel stays in the palette. */
    private static final int[][] GATE_WARM = {
            {Pal.WOOD[0], Pal.WOOD[1], Pal.RUST[1], Pal.RUST[2], Pal.RUST[3], Pal.GOLD[1], Pal.GOLD[2], Pal.GOLD[3]},
            {Pal.WOOD[0], Pal.WOOD[1], Pal.WOOD[2], Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[5], Pal.CANVAS[4], Pal.BONE[2]},
            {Pal.MUD[0], Pal.MUD[1], Pal.CANVAS[0], Pal.CANVAS[1], Pal.CANVAS[2], Pal.CANVAS[3], Pal.CANVAS[4], Pal.CANVAS[5]}};
    private static void gateWarmth(Canvas c, int fx, int fy, int xMin, int xMax) {
        for (int y = fy - 24; y <= fy + 24; y++) for (int x = Math.max(xMin, fx - 24); x <= Math.min(xMax, fx + 24); x++) {
            int colour = c.get(x, y);
            if (!gateStone(colour)) continue;
            double d = Math.hypot(x + .5 - fx, (y + .5 - fy) * 1.15) + (Noise.value(x, y, 3, 871) - .5) * 2;
            int i = Pal.index(colour), ring = d < 9 ? 0 : d < 14 ? 1 : d < 20 ? 2 : -1;
            if (ring < 0 || ring == 2 && i < 3) continue;                              // the faint edge never reaches into the joints
            if (i <= 1 && ring < 2) ring++;
            c.set(x, y, GATE_WARM[ring][i]);
        }
    }

    /** A wall torch: tall flame, pitch-soaked wrap, iron cup on a bracket plate. */
    private static void gateTorch(Canvas c, int x, int y) {
        String[] flame = {"...1...", "...12..", "..121..", "..132..", ".1232..", ".13431.", "1234421", "1345431", "1345531", ".24542.", "..343.."};
        for (int r = 0; r < flame.length; r++) for (int i = 0; i < 7; i++)
            if (flame[r].charAt(i) != '.') c.set(x - 3 + i, y - 13 + r, Pal.FIRE[flame[r].charAt(i) - '0']);
        c.set(x - 1, y - 2, Pal.DRY[2]); c.set(x, y - 2, Pal.DRY[3]); c.set(x + 1, y - 2, Pal.DRY[1]);
        c.vline(x, y - 1, y + 3, Pal.WOOD[3]); c.vline(x + 1, y - 1, y + 3, Pal.WOOD[1]);
        c.hline(x - 1, x + 2, y + 1, Pal.IRON[4]); c.hline(x - 1, x + 2, y + 2, Pal.IRON[2]);
        c.line(x - 1, y + 3, x - 3, y + 6, Pal.IRON[3]); c.line(x + 2, y + 3, x + 2, y + 6, Pal.IRON[1]);
        c.rect(x - 4, y + 6, 8, 3, Pal.IRON[2]); c.hline(x - 4, x + 3, y + 6, Pal.IRON[4]); c.set(x - 3, y + 7, Pal.IRON[5]); c.set(x + 2, y + 7, Pal.IRON[5]);
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
    /** Open-fronted hospital tent: sagging patched canvas on four poles, a lantern-lit ward of rumpled cots. */
    static Canvas infirmary() {
        var k = SetPieces.Kind.INFIRMARY;
        Canvas c = new Canvas(k.width, k.anchorY + 4);
        int g = k.anchorY - 1, x0 = k.anchorX - 82, w = 164, front = g - 72, back = front - 28;
        int[] poles = {x0 - 4, x0 + 52, x0 + 108, x0 + w};
        int[] eave = new int[c.w];                                                              // front edge sags between poles
        for (int x = 0; x < c.w; x++) {
            int p = 0; while (p < 2 && x > poles[p + 1]) p++;
            double t = Math.max(0, Math.min(1, (x - poles[p]) / (double) (poles[p + 1] - poles[p])));
            eave[x] = front + (int) Math.round(4 * Math.sin(Math.PI * t));
        }
        // ward interior: back wall, trampled floor
        int top = front + 6;
        c.rect(x0 + 2, top, w - 4, g - top, Pal.CANVAS[1]);
        for (int x = x0 + 9; x < x0 + w - 4; x += 17) {                                         // hanging folds of the back wall
            c.vline(x, top, g - 27, Pal.CANVAS[0]); c.vline(x + 1, top + 3, g - 27, Pal.CANVAS[2]);
        }
        c.rect(x0 + 2, g - 26, w - 4, 26, Pal.MUD[1]);
        c.hline(x0 + 2, x0 + w - 3, g - 26, Pal.CANVAS[0]);
        for (int y = g - 25; y < g; y++) for (int x = x0 + 3; x < x0 + w - 3; x++) {          // straw and boot-churned mud
            double r = Noise.rnd(x, y, 41);
            if (r < .018) { c.set(x, y, Pal.DRY[2]); if (r < .007) c.set(x + 1, y, Pal.DRY[3]); }
            else if (Noise.fbm(x, y * 2, 9, 2, 43) > .64) c.set(x, y, Pal.MUD[2]);
        }
        String[] blanket = {"crimson", "folded", "stone"};
        for (int bay = 0; bay < 3; bay++) cot(c, poles[bay] + 9, g - 7, blanket[bay]);
        for (int i = 0; i < 4; i++) {                                                           // dressings drying on a line in the first bay
            int bx = poles[0] + 12 + i * 9, len = 7 + (i * 5) % 4;
            c.vline(bx, top + 9, top + 9 + len, Pal.BONE[2]); c.vline(bx + 1, top + 9, top + 8 + len, Pal.BONE[1]);
        }
        c.line(poles[0] + 6, top + 8, poles[1] - 4, top + 10, Pal.DRY[1]);
        Canvas basin = Decals.ascii(
                ".NNNNNN.",
                "NddddddN",
                ".mmmmmm.",
                "..nnnn..");
        c.blit(basin, poles[2] - 4, g - 6);
        // warm lantern light, stepped up through each ramp so the ward reads lit from within
        int lx = poles[1] + 28, ly = top + 9;
        for (int y = top; y < g; y++) for (int x = x0 + 2; x < x0 + w - 2; x++) {
            double d = Math.hypot((x - lx) * .8, (y - ly) * 1.1);
            int step = d < 12 ? 2 : d < 32 ? 1 : 0;
            if (step > 0 && c.opaque(x, y)) c.set(x, y, Pal.shift(c.get(x, y), step));
        }
        c.vline(lx, top - 2, ly - 4, Pal.IRON[2]);
        Canvas lantern = Decals.ascii(
                ".nn.",
                "nJJn",
                "JIIJ",
                "JIIJ",
                "nJJn");
        c.blit(lantern, lx - 1, ly - 3);
        // the roof: canvas panels pulled taut at the poles, sagging and creased between them
        for (int y = back; y <= front + 4; y++) {
            double t = Math.min(1, (y + .5 - back) / (front - back));
            int left = (int) Math.round(x0 + 6 - 13 * t), right = (int) Math.round(x0 + w - 6 + 13 * t);
            for (int x = left; x < right; x++) {
                if (y > eave[x]) continue;
                int p = 0; while (p < 2 && x > poles[p + 1]) p++;
                double u = (x - poles[p]) / (double) (poles[p + 1] - poles[p]);
                int colour = u < .28 ? Pal.CANVAS[4] : u > .8 ? Pal.CANVAS[2] : Pal.CANVAS[3];
                if (Noise.fbm(x, y * 1.6, 14, 3, 7) > .66) colour = Pal.shift(colour, -1);    // rain stains in hard blotches
                if (y >= eave[x] - 2) colour = Pal.shift(colour, -1);
                if (y == back) colour = Pal.WOOD[2]; else if (y == back + 1) colour = Pal.WOOD[4];
                c.set(x, y, colour);
            }
        }
        for (int p = 0; p < 3; p++) {                                                           // tension creases from the sag to the ridge
            int mid = (poles[p] + poles[p + 1]) / 2, span = poles[p + 1] - poles[p];
            for (int side : new int[]{-1, 1}) {
                int tx = mid + side * span / 6, ty = back + (front - back) / 2;
                c.line(mid + side * 2, eave[mid] - 3, tx, ty, Pal.CANVAS[2]);
                c.line(mid + side * 2 + 1, eave[mid] - 4, tx + 1, ty + 4, Pal.CANVAS[4]);
            }
            c.line(poles[p + 1] - 1, back + 2, poles[p + 1] - 1, front, Pal.CANVAS[2]);       // the pole's ridge under the canvas
            c.line(poles[p + 1], back + 2, poles[p + 1], front - 1, Pal.CANVAS[5]);
        }
        patch(c, x0 + 22, back + 7, 14, 9, Pal.DRY[3]);
        patch(c, x0 + 118, back + 12, 11, 8, Pal.CANVAS[2]);
        for (int i = 0; i < 6; i++) { int tx = x0 + 96 + i, ty = back + 6 + i / 2; c.set(tx, ty, Pal.INK); c.set(tx, ty + 1, Pal.CANVAS[0]); }   // a slit tear
        for (int x = x0 - 6; x < x0 + w + 6; x++)                                               // mould creeping up from the eave
            if (Noise.fbm(x, 0, 6, 2, 19) > .58) for (int y = eave[x] - 1 - (int) (Noise.rnd(x, 3, 19) * 3); y < eave[x]; y++) c.tint(x, y, Pal.GRASS[1]);
        c.hline(x0 + 4, x0 + w - 5, back, Pal.WOOD[2]);
        c.set(x0 + 3, back, Pal.WOOD[3]); c.set(x0 + w - 4, back, Pal.WOOD[3]);
        // hem: a crimson band, frayed along the bottom
        for (int x = x0 - 7; x < x0 + w + 7; x++) {
            int y0 = eave[Math.max(0, Math.min(c.w - 1, x))] + 1;
            c.set(x, y0, Pal.CANVAS[4]); c.set(x, y0 + 1, Pal.CRIMSON[3]); c.set(x, y0 + 2, Pal.CRIMSON[2]);
            int fray = 1 + (Noise.hash(x, 0, 23) & 3) / 2;
            for (int y = y0 + 3; y < y0 + 3 + fray; y++) c.set(x, y, Pal.CANVAS[2]);
            if ((Noise.hash(x, 1, 23) & 31) == 0) for (int y = y0 + 3; y < y0 + 8; y++) c.set(x, y, Pal.CANVAS[2]);
        }
        for (int px : poles) Structures.post(c, px, front + 6, g, 5);
        flap(c, poles[0] + 5, eave[poles[0] + 5] + 4, g, 1);
        flap(c, poles[3] - 1, eave[poles[3] - 1] + 4, g, -1);
        // guy ropes to pegs, supplies outside
        c.line(x0 - 6, front + 1, x0 - 18, g + 1, Pal.DRY[2]); c.line(x0 + w + 6, front + 1, x0 + w + 16, g - 1, Pal.DRY[2]);
        c.rect(x0 - 19, g - 1, 2, 4, Pal.WOOD[3]); c.rect(x0 + w + 15, g - 3, 2, 4, Pal.WOOD[3]);
        Canvas crate = Decals.ascii(
                "..OOPp..OOP.",
                ".OOOPPpOOOPp",
                "vvvvvvvvvvvv",
                "cbbbbbbbbbbx",
                "cvccccccccvx",
                "cGGGGGGGGGGx",
                "cvccccccccvx",
                "xxxxxxxxxxxz");
        c.blit(crate, x0 + w + 6, g - crate.h + 1);
        c.blit(Decals.TUFT_C.art(), x0 - 26, g - Decals.TUFT_C.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    /** Wooden cot with a straw pillow and a blanket left the way its last patient left it. */
    private static void cot(Canvas c, int x, int floor, String blanket) {
        int top = floor - 12;
        for (int lx : new int[]{x + 1, x + 36}) { c.vline(lx, top + 6, floor, Pal.WOOD[3]); c.vline(lx + 1, top + 6, floor, Pal.WOOD[1]); }
        c.hline(x, x + 38, top + 6, Pal.WOOD[4]); c.hline(x, x + 38, top + 7, Pal.WOOD[2]);
        c.rect(x + 1, top + 2, 37, 4, Pal.CANVAS[3]); c.hline(x + 1, x + 37, top + 2, Pal.CANVAS[4]);
        c.rect(x + 2, top, 9, 4, Pal.BONE[2]); c.hline(x + 3, x + 9, top, Pal.BONE[3]); c.hline(x + 2, x + 10, top + 3, Pal.BONE[1]);
        int[] ramp = blanket.equals("crimson") ? Pal.CRIMSON : blanket.equals("stone") ? Pal.STONE : Pal.CANVAS;
        if (blanket.equals("folded")) {                                                          // squared away at the foot, never slept in
            c.rect(x + 26, top - 1, 11, 5, ramp[4]); c.hline(x + 26, x + 36, top - 1, ramp[5]); c.hline(x + 26, x + 36, top + 2, ramp[2]);
            return;
        }
        for (int bx = x + 12; bx <= x + 37; bx++) {                                             // thrown back, lumped where someone lay
            double ph = ramp == Pal.STONE ? 2 : 0; int lift = (int) Math.round(1.8 + 1.3 * Math.sin((bx - x) * .3 + ph) + .7 * Math.sin((bx - x) * .85 + ph));
            for (int y = top + 1 - lift; y <= top + 6; y++)
                c.set(bx, y, y == top + 1 - lift ? ramp[4] : y == top + 6 ? ramp[1] : (bx - x) % 7 == 3 ? ramp[2] : ramp[3]);
        }
        c.vline(x + 37, top + 6, top + 9, ramp[2]);                                             // a corner trailing over the frame
    }

    /** A sewn-on square of other cloth, its stitches showing. */
    private static void patch(Canvas c, int x, int y, int w, int h, int colour) {
        for (int py = y; py < y + h; py++) for (int px = x; px < x + w; px++) if (c.opaque(px, py)) c.set(px, py, colour);
        for (int px = x; px < x + w; px += 3) { c.tint(px, y, Pal.CANVAS[0]); c.tint(px + 1, y + h - 1, Pal.CANVAS[0]); }
        for (int py = y + 1; py < y + h; py += 3) { c.tint(x, py, Pal.CANVAS[0]); c.tint(x + w - 1, py, Pal.CANVAS[0]); }
        c.hline(x + 1, x + w - 2, y + 1, Pal.shift(colour, 1));
    }

    /** Front flap rolled up against a pole and tied off, its tail hanging. */
    private static void flap(Canvas c, int x, int y, int floor, int dir) {
        for (int i = 0; i < 26; i++) {
            int width = Math.max(2, 9 - i / 3);
            for (int j = 0; j < width; j++) {
                int col = j == 0 ? Pal.CANVAS[2] : j == width - 1 ? Pal.CANVAS[5] : (i + j) % 6 == 0 ? Pal.CANVAS[2] : Pal.CANVAS[4];
                c.set(x + dir * j, y + i, col);
            }
        }
        c.hline(x - 1, x + dir * 4, y + 26, Pal.DRY[3]); c.hline(x - 1, x + dir * 4, y + 27, Pal.DRY[1]);
        for (int i = 28; i < 40 && y + i < floor - 4; i++) { c.set(x + dir * (1 + i % 3 / 2), y + i, Pal.CANVAS[4]); c.set(x + dir * (2 + i % 3 / 2), y + i, Pal.CANVAS[2]); }
    }
}
