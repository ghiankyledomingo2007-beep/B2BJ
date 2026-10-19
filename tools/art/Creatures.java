/**
 * Corrupted Forest enemies in the Outpost remnants' sheet format, so the game draws them the same way:
 * 48x48 cells, rows front / side (facing right; mirrored for left) / back; a walk sheet of 4 frames and
 * an attack sheet of 8 (3 wind-up, 3 strike, 2 settle, matching Wisp.attackFrame()). Shading is lit
 * from the upper left with a selective outline. Each pose reads before it hits: wings flare, a wolf
 * crouches and raises its thorns, a shaman's orb swells.
 */
final class Creatures {
    private Creatures() { }
    static final int C = 48;

    static Canvas sheet(String kind, boolean attack) {
        int frames = attack ? 8 : 4;
        Canvas sheet = new Canvas(frames * C, 3 * C);
        for (int row = 0; row < 3; row++) for (int f = 0; f < frames; f++) {
            Canvas cell = switch (kind) {
                case "imp" -> imp(row, f, attack);
                case "wolf" -> wolf(row, f, attack);
                default -> shaman(row, f, attack);
            };
            cell.selectiveOutline();
            sheet.blit(cell, f * C, row * C);
        }
        return sheet;
    }

    // ------------------------------------------------------------------ shared
    /** An ellipse shaded like a lit sphere: highlight up-left, core, shade, a dark rim below. */
    static void ball(Canvas c, double cx, double cy, double rx, double ry, int[] ramp, int base) {
        for (int y = (int) Math.floor(cy - ry); y <= cy + ry; y++) for (int x = (int) Math.floor(cx - rx); x <= cx + rx; x++) {
            double dx = (x + .5 - cx) / rx, dy = (y + .5 - cy) / ry;
            if (dx * dx + dy * dy > 1) continue;
            double lit = -dx * .55 - dy * .75;
            int i = base + (lit > .6 ? 2 : lit > .15 ? 1 : lit > -.45 ? 0 : -1);
            c.set(x, y, ramp[Math.max(0, Math.min(ramp.length - 1, i))]);
        }
    }
    static void px(Canvas c, int x, int y, int colour) { c.set(x, y, colour); }

    // ------------------------------------------------------------------ Rift Imp: a small violet flier
    static Canvas imp(int row, int f, boolean attack) {
        Canvas c = new Canvas(C, C);
        int[] body = Pal.VIOLET;
        double bob = attack ? 0 : Math.sin(f * Math.PI / 2) * 1.5;
        double lean = 0, stretch = 1;
        int wing = attack ? (f < 3 ? 3 : f < 6 ? -1 : 1) : (f % 2 == 0 ? (f == 0 ? 2 : -2) : 0);   // flap: up, mid, down, mid
        boolean flare = attack && f < 3, dart = attack && f >= 3 && f < 6;
        if (flare) lean = -2 - f;                                            // rears back as it winds up
        if (dart) { lean = 5; stretch = 1.35; }
        double cx = 24 + (row == 1 ? lean : 0), cy = 22 + bob + (row != 1 ? -lean * .3 : 0);
        // wings first (behind the body), membranes lighter than the bones
        for (int side : row == 1 ? new int[]{-1} : new int[]{-1, 1}) {
            double span = dart ? 6 : 13, lift = wing * 2.5;
            int bx = (int) Math.round(cx + side * 4), by = (int) Math.round(cy - 2);
            int tx = (int) Math.round(cx + side * (4 + span)), ty = (int) Math.round(cy - 6 - lift);
            int ex = (int) Math.round(cx + side * (2 + span * .7)), ey = (int) Math.round(cy + 5 - lift * .3);
            Kit.poly(c, body[3], bx, by, tx, ty, ex, ey);
            Kit.poly(c, body[2], bx, by + 1, ex, ey, (bx + ex) / 2, ey + 2);
            c.line(bx, by, tx, ty, body[1]);
            c.line(tx, ty, ex, ey, body[4]);
        }
        // tail with a spade tip trailing behind
        int tailDir = row == 1 ? -1 : 1;
        double tx0 = cx + (row == 1 ? -6 : 3), ty0 = cy + 8;
        for (int i = 0; i < 9; i++) {
            int x = (int) Math.round(tx0 + tailDir * i * (row == 1 ? 1 : .4)), y = (int) Math.round(ty0 + i * (row == 1 ? .3 : .9) + Math.sin(i * .7 + f) * 1.2);
            px(c, x, y, body[1]);
            if (i == 8) { px(c, x - 1, y, body[3]); px(c, x + 1, y, body[3]); px(c, x, y + 1, body[3]); }
        }
        ball(c, cx, cy, 7.5 * stretch, 7.5 / Math.sqrt(stretch), body, 2);   // body
        ball(c, cx + (row == 1 ? 2 : 0), cy - 8, 6, 5.5, body, 3);          // head
        for (int side : row == 1 ? new int[]{1} : new int[]{-1, 1}) {        // horns
            int hx = (int) Math.round(cx + side * 4 + (row == 1 ? 1 : 0)), hy = (int) Math.round(cy - 12);
            px(c, hx, hy, Pal.BONE[2]); px(c, hx + side, hy - 1, Pal.BONE[3]); px(c, hx + side, hy - 2, Pal.BONE[3]);
        }
        if (row != 2) {                                                     // eyes: gold, flaring white on the wind-up
            int eye = flare ? Pal.GOLD[5] : Pal.GOLD[4];
            if (row == 0) { px(c, (int) cx - 2, (int) (cy - 8), eye); px(c, (int) cx + 2, (int) (cy - 8), eye); }
            else { px(c, (int) cx + 4, (int) (cy - 8), eye); px(c, (int) cx + 5, (int) (cy - 8), Pal.GOLD[3]); }
            if (flare) for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}}) px(c, (int) cx + (row == 1 ? 4 : 2) + o[0], (int) (cy - 8) + o[1], Pal.GOLD[3]);
        }
        if (row == 0 && !dart) { px(c, (int) cx - 1, (int) (cy - 5), Pal.INK); px(c, (int) cx, (int) (cy - 5), Pal.INK); px(c, (int) cx + 1, (int) (cy - 5), Pal.INK); }
        return c;
    }

    // ------------------------------------------------------------------ Thorn Wolf: bramble on four legs
    static Canvas wolf(int row, int f, boolean attack) {
        Canvas c = new Canvas(C, C);
        int[] fur = {Pal.INK, Pal.WOOD[1], Pal.WOOD[2], Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[5]};   // mid-value: reads on the dark floor
        boolean crouch = attack && f < 3, pounce = attack && f >= 3 && f < 6, land = attack && f >= 6;
        double phase = attack ? 0 : f * Math.PI / 2, lower = crouch ? 2 + f : land ? 1 : 0, lift = pounce ? -4 : 0;
        int ground = 40;
        if (row == 1) {                                                     // side view: the clearest silhouette
            double bodyX = 23 + (pounce ? 3 : crouch ? -2 : 0), bodyY = 27 + lower + lift;
            for (int n = 0; n < 4; n++) {                                  // legs: far pair darker, swinging in opposite phases
                boolean far = n % 2 == 1, front = n >= 2;
                double hip = bodyX + (front ? 8 : -8) + (far ? 1 : 0);
                double swing = pounce ? (front ? 7 : -7) : crouch ? (front ? 2 : -2) : Math.sin(phase + (front ? 0 : Math.PI) + (far ? Math.PI : 0)) * 5;
                int footY = pounce ? (int) (bodyY + 8) : ground - (int) Math.max(0, Math.cos(phase + (front ? 0 : Math.PI) + (far ? Math.PI : 0)) * 2);
                int[] leg = far ? new int[]{Pal.INK, fur[1], fur[1], fur[2], fur[2]} : new int[]{Pal.INK, fur[2], fur[3], fur[3], fur[4]};
                ForestArt.limb(c, hip, bodyY + 3, hip + swing * .6, (bodyY + 3 + footY) / 2.0 + 1, 2.2, 1.6, leg);    // upper leg
                ForestArt.limb(c, hip + swing * .6, (bodyY + 3 + footY) / 2.0 + 1, hip + swing, footY, 1.6, 1.3, leg);  // lower leg
                px(c, (int) Math.round(hip + swing) + 1, footY, leg[3]); px(c, (int) Math.round(hip + swing) + 2, footY, leg[2]);  // paw
            }
            ball(c, bodyX, bodyY, 12, 6, fur, 3);                            // body
            for (int m = 0; m < 9; m++) {                                       // bramble moss caught in the coat
                int mx = (int) (bodyX - 9 + Noise.rnd(m, 1, 1131) * 18), my = (int) (bodyY - 4 + Noise.rnd(m, 2, 1131) * 5);
                px(c, mx, my, Pal.GRASS[Noise.rnd(m, 3, 1131) < .5 ? 2 : 3]);
            }
            ball(c, bodyX - 13, bodyY - 1 - (crouch ? 0 : 1), 5, 3, fur, 3); // brushy tail
            double headX = bodyX + 13 + (pounce ? 2 : 0), headY = bodyY - 4 + (crouch ? 3 : 0);
            ball(c, headX, headY, 5.5, 4.5, fur, 3);
            Kit.poly(c, fur[3], (int) headX + 3, (int) headY - 1, (int) headX + 10, (int) headY + 1, (int) headX + 3, (int) headY + 3);   // muzzle
            px(c, (int) headX + 10, (int) headY + 1, Pal.INK);
            Kit.poly(c, fur[2], (int) headX - 2, (int) headY - 3, (int) headX, (int) headY - 9, (int) headX + 2, (int) headY - 3);       // ear
            if (pounce) { c.line((int) headX + 4, (int) headY + 3, (int) headX + 9, (int) headY + 4, Pal.CRIMSON[3]); px(c, (int) headX + 6, (int) headY + 3, Pal.BONE[3]); }
            px(c, (int) headX + 2, (int) headY - 1, crouch || pounce ? Pal.GOLD[5] : Pal.GOLD[4]);
            thorns(c, bodyX - 10, bodyX + 9, bodyY - 6, crouch ? 4 : 2);
        } else {                                                            // front and back: chest or rump, two legs showing
            double bodyX = 24, bodyY = 27 + lower + lift;
            for (int side : new int[]{-1, 1}) {
                double step = attack ? 0 : Math.sin(phase + (side > 0 ? Math.PI : 0)) * 1.5;
                ForestArt.limb(c, bodyX + side * 5, bodyY + 3, bodyX + side * 6, ground - step, 2, 1.4, new int[]{Pal.INK, fur[1], fur[2], fur[3], fur[3]});
            }
            ball(c, bodyX, bodyY, 9, 7.5, fur, 3);
            if (row == 0) {
                ball(c, bodyX, bodyY - 8 + (crouch ? 3 : 0), 6, 5, fur, 3);
                for (int side : new int[]{-1, 1}) Kit.poly(c, fur[2], (int) bodyX + side * 3, (int) bodyY - 12, (int) bodyX + side * 6, (int) bodyY - 18, (int) bodyX + side * 7, (int) bodyY - 11);
                int eye = crouch || pounce ? Pal.GOLD[5] : Pal.GOLD[4];
                px(c, (int) bodyX - 3, (int) bodyY - 9 + (crouch ? 3 : 0), eye); px(c, (int) bodyX + 2, (int) bodyY - 9 + (crouch ? 3 : 0), eye);
                ball(c, bodyX, bodyY - 5 + (crouch ? 3 : 0), 3, 2, fur, 4);
                if (pounce) px(c, (int) bodyX, (int) bodyY - 4, Pal.CRIMSON[3]);
            } else ball(c, bodyX, bodyY - 6, 4, 4, fur, 2);                    // the tail seen from behind
            thorns(c, bodyX - 7, bodyX + 7, bodyY - 7, crouch ? 4 : 2);
        }
        return c;
    }
    /** Pale thorns along a back; raised higher as a wolf gathers to spring. */
    private static void thorns(Canvas c, double x0, double x1, double y, int height) {
        for (double x = x0; x <= x1; x += 3.5) {
            int ix = (int) Math.round(x), iy = (int) Math.round(y);
            for (int h = 0; h < height; h++) px(c, ix + (h > 1 ? 1 : 0), iy - h, h == height - 1 ? Pal.BONE[3] : Pal.WOOD[2]);
        }
    }

    // ------------------------------------------------------------------ Mire Shaman: a hooded caster
    static Canvas shaman(int row, int f, boolean attack) {
        Canvas c = new Canvas(C, C);
        int[] robe = {Pal.INK, Pal.MUD[1], Pal.MUD[2], Pal.MUD[3], Pal.MUD[4], Pal.MUD[5]};
        boolean raise = attack && f < 3, cast = attack && f >= 3 && f < 6;
        double sway = attack ? 0 : Math.sin(f * Math.PI / 2) * 1;
        int ground = 41;
        double cx = 23 + sway * .5;
        // robe: a bell of cloth, mossy rags at the hem
        for (int y = 16; y <= ground; y++) {
            double t = (y - 16) / (double) (ground - 16);
            int hw = (int) Math.round(5 + t * 8 + (y == ground ? Math.sin(f + y) : 0));
            for (int x = (int) cx - hw; x <= cx + hw; x++) {
                double u = (x - cx) / Math.max(1, hw);
                int i = u < -.45 ? 4 : u < .1 ? 3 : u < .6 ? 2 : 1;
                if (y > ground - 3 && ((x + f) % 3 == 0)) i = 1;
                c.set(x, y, robe[i]);
                if (y > ground - 5 && Noise.rnd(x, y, 1101) < .2) c.set(x, y, Pal.GRASS[2]);
            }
        }
        // hood and the dark face inside it, two eyes glowing violet; moss hangs from the brim
        ball(c, cx, 15, 7.5, 7, robe, 3);
        for (int strand = -2; strand <= 2; strand++) {
            int sx0 = (int) Math.round(cx + strand * 3);
            for (int k = 0; k < 3 + Math.abs(strand) % 2; k++) px(c, sx0 + (k > 1 ? (strand > 0 ? 1 : -1) : 0), 21 + k, k == 0 ? Pal.GRASS[3] : Pal.GRASS[2]);
        }
        for (int bead = 0; bead < 3; bead++) px(c, (int) cx - 3 + bead * 3, 27 + bead % 2, Pal.BONE[3]);   // a string of bone charms
        if (row != 2) {
            int fx = (int) Math.round(cx + (row == 1 ? 3 : 0));
            for (int y = 13; y <= 18; y++) for (int x = fx - 3; x <= fx + 3; x++) if (Math.abs(x - fx) + (y - 13) / 2 < 5) c.set(x, y, Pal.INK);
            int eye = raise || cast ? Pal.VIOLET[5] : Pal.VIOLET[4];
            if (row == 0) { px(c, fx - 2, 15, eye); px(c, fx + 2, 15, eye); } else { px(c, fx + 1, 15, eye); px(c, fx + 2, 15, Pal.VIOLET[3]); }
        }
        // the staff: held beside, raised overhead to charge, thrust forward to cast
        double sx, sy, ex, ey;
        int dir = row == 1 ? 1 : 1;
        if (raise) { sx = cx + 9; sy = 34; ex = cx + 7 - f; ey = 4 + f; }
        else if (cast) { sx = cx + 6; sy = 30; ex = cx + 18 * dir; ey = 16; }
        else { sx = cx + 10; sy = ground; ex = cx + 11; ey = 9 + sway; }
        c.line((int) sx, (int) sy, (int) ex, (int) ey, Pal.WOOD[3]);
        c.line((int) sx + 1, (int) sy, (int) ex + 1, (int) ey, Pal.WOOD[1]);
        double orb = raise ? 2.5 + f * .8 : cast ? 4.5 : 2.5;
        int orbBase = raise || cast ? 4 : 3;
        ball(c, ex, ey - 2, orb, orb, Pal.VIOLET, orbBase);                // mire-magic: violet, the enemy's colour, never the player's cyan
        if (cast) for (int[] o : new int[][]{{-6, 0}, {6, 0}, {0, -6}, {0, 5}}) px(c, (int) ex + o[0], (int) ey - 2 + o[1], Pal.VIOLET[5]);
        // a webbed hand on the staff
        px(c, (int) Math.round((sx + ex) / 2) - 1, (int) Math.round((sy + ey) / 2), Pal.GRASS[3]);
        px(c, (int) Math.round((sx + ex) / 2), (int) Math.round((sy + ey) / 2), Pal.GRASS[4]);
        return c;
    }
}
