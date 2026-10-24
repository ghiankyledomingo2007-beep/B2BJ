import java.util.HashMap;
import java.util.Map;

/**
 * The field-camp rescue props: a north-south run of the log palisade, a crawl gap under it, a dropped
 * key, a medicine tin, a locked cache, three hurt expedition members and the bedrolls they rest on.
 * Same scale, palette, light (upper left) and selective outline as Props; people are ~64 px standing,
 * so seated they come out ~36 px tall. Small hand-pixelled things are character maps, one colour per letter.
 */
final class FieldArt {
    private FieldArt() { }

    // ------------------------------------------------------------------ character maps
    /** Pairs of (letters, ramp, first index): letter i of `keys` takes ramp[from + i]. */
    static Map<Character, Integer> legend(Object... spec) {
        Map<Character, Integer> m = new HashMap<>();
        for (int i = 0; i < spec.length; i += 3) {
            String keys = (String) spec[i];
            int[] ramp = (int[]) spec[i + 1];
            int from = (Integer) spec[i + 2];
            for (int k = 0; k < keys.length(); k++) m.put(keys.charAt(k), ramp[from + k]);
        }
        m.put('K', Pal.INK);
        return m;
    }
    static void paint(Canvas c, int x0, int y0, Map<Character, Integer> legend, String... rows) {
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            char ch = rows[y].charAt(x);
            if (ch == '.') continue;
            Integer colour = legend.get(ch);
            if (colour == null) throw new IllegalArgumentException("unknown pixel '" + ch + "' in row " + y + ": " + rows[y]);
            c.set(x0 + x, y0 + y, colour);
        }
    }

    // ------------------------------------------------------------------ palisade running north-south
    static final int SIDE_W = 16, SIDE_H = 64, STEP = 8, SLOTS = SIDE_H / STEP, SX = 5;
    /**
     * Seen from the 3/4 camera a north-south fence is a column of stakes, each a step nearer (lower on
     * screen) than the last, so every sharpened tip overlaps the body of the stake behind it. Odd stakes sit
     * one pixel right, as no row of hand-set stakes is straight. A stake shows until the next stake but one
     * (same column) is full width over it, so slots 6 and 7 run into the cell below: they are the same in every
     * variant and each cell redraws them above its top edge, which lets any variant stack under any other.
     * Offsets push a tip down a pixel or two, never up out of the cell.
     */
    private static int[] offsets(int v) {
        return switch (v) { case 0 -> new int[]{0, 1, 0, 2, 1, 0, 1, 0}; case 1 -> new int[]{0, 2, 1, 0, 2, 1, 1, 0}; default -> new int[]{0, 0, 2, 1, 0, 2, 1, 0}; };
    }
    private static int stakeSeed(int v, int k) { return k >= SLOTS - 2 ? 7700 + k : 7000 + v * 31 + k; }

    static Canvas sideCell(int v) {
        Canvas c = new Canvas(SIDE_W, SIDE_H);
        sideColumn(c, new int[]{v});
        return c;
    }
    /** Variants stacked top to bottom into `c`, exactly as the game stacks the cells. */
    private static void sideColumn(Canvas c, int[] vs) {
        rail(c);
        int[] fixed = offsets(0);
        for (int k = SLOTS - 2; k < SLOTS; k++) sideStake(c, k, k * STEP - SIDE_H + fixed[k], stakeSeed(0, k), false);   // from the cell above
        for (int i = 0; i < vs.length; i++) {
            int[] off = offsets(vs[i]);
            for (int k = 0; k < SLOTS; k++) sideStake(c, k, i * SIDE_H + k * STEP + off[k], stakeSeed(vs[i], k), vs[i] == 1 && k == 3);
        }
        for (int i = 0; i < vs.length; i++) sideTrim(c, vs[i], i * SIDE_H);
        c.selectiveOutline();
    }
    private static void sideStake(Canvas c, int k, int top, int seed, boolean broken) {
        stake(c, SX + (k & 1), top, 26, seed, broken, true);
    }
    /** The rails both run along the outer (west) face, so in this view they project onto one pole. */
    private static void rail(Canvas c) {
        for (int y = 0; y < c.h; y++) {
            int ly = Math.floorMod(y, SIDE_H);
            c.set(SX - 3, y, Noise.rnd(0, ly / 3, 7801) < .12 ? Pal.WOOD[3] : Pal.WOOD[4]);
            c.set(SX - 2, y, Noise.rnd(1, ly / 2, 7802) < .15 ? Pal.WOOD[2] : Pal.WOOD[3]);
            c.set(SX - 1, y, Pal.WOOD[1]);
        }
    }
    /** Rope lashings (same rows in every variant) and, on v2, spent arrows. */
    private static void sideTrim(Canvas c, int v, int y0) {
        for (int ly = 5; ly < SIDE_H; ly += 16) for (int s = 0; s < 4; s++) {
            c.set(SX - 3 + s, y0 + ly + s, s < 2 ? Pal.DRY[3] : Pal.DRY[2]);
            c.set(SX - 3 + s, y0 + ly + s + 1, Pal.DRY[1]);
        }
        if (v == 2) for (int[] a : new int[][]{{SX + 3, 22}, {SX + 4, 45}}) {
            Canvas arrow = Decals.ascii("O.O..", ".OO..", "..xc.", "...xc");
            c.blit(arrow, a[0] - 3, y0 + a[1] - 3);
        }
    }
    /**
     * One sharpened stake, 8 px wide, shaded as Props.log (private there). `stacked` stakes stand in front
     * of another stake, so their tip edges darken the wood behind them; only `len` rows are drawn.
     */
    private static void stake(Canvas c, int x, int top, int len, int seed, boolean broken, boolean stacked) {
        int[] shade = {Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[5], Pal.WOOD[4], Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[2], Pal.WOOD[1]};
        int drop = broken ? 7 : 0;
        for (int r = drop; r < len; r++) {
            int y = top + r;
            int half = broken ? 4 : stacked ? Math.min(4, (r + 1) / 2) : Math.min(4, (r + 1) * 4 / 9);
            for (int col = 0; col < 8; col++) {
                int dx = col - 4;
                if (dx < -half || dx >= Math.max(1, half)) continue;
                int colour = shade[col];
                if (Noise.rnd(col, r / 2, seed) < (stacked ? .04 : .07)) colour = Pal.shift(colour, -1);            // bark furrows
                if (col == 2 && Noise.rnd(0, r / 4, seed + 1) < .25) colour = Pal.WOOD[4];
                if (!stacked && len - r < 7 && Noise.rnd(col, r, seed + 2) < .3) colour = Pal.shift(colour, -1);  // wet foot
                c.set(x + col, y, colour);
            }
            if (stacked && !broken && half < 4) {             // the stake behind sinks into shadow around the tip
                c.tint(x + 3 - half, y, Pal.shift(c.get(x + 3 - half, y), -2));
                for (int col = 4 + Math.max(1, half); col < 9; col++) if (c.opaque(x + col, y)) c.set(x + col, y, Pal.shift(c.get(x + col, y), col == 4 + Math.max(1, half) ? -3 : -1));
                if (half > 0) c.set(x + 4 - half, y, Pal.WOOD[5]);                // moonlight along the tip's lit edge
            }
        }
        if (broken) for (int col = 0; col < 8; col++) {    // splintered top
            int jag = (int) (Noise.rnd(col, seed, 3) * 4);
            for (int s = 0; s < jag; s++) if (stacked) c.set(x + col, top + drop + s, Pal.WOOD[col < 4 ? 2 : 1]); else c.clear(x + col, top + drop + s);
            c.set(x + col, top + drop + jag, col < 4 ? Pal.WOOD[5] : Pal.WOOD[4]);
        } else { c.set(x + 4, top, Pal.WOOD[5]); c.set(x + 3, top + 1, Pal.WOOD[5]); }
        if (!broken || len > 30) {
            int knot = top + (stacked ? 11 + (int) (Noise.rnd(0, 0, seed + 3) * 4) : 14 + (int) (Noise.rnd(x, 0, seed + 3) * Math.max(1, len - 30)));
            c.set(x + 4, knot, Pal.WOOD[1]); c.set(x + 5, knot, Pal.WOOD[0]); c.set(x + 4, knot - 1, Pal.WOOD[2]);
        }
    }

    static Canvas side() { return Canvas.strip(sideCell(0), sideCell(1), sideCell(2)); }

    /**
     * Fails the build unless every variant stacks on every other: the two cells drawn one above the other
     * must equal the fence drawn as one continuous column (outline included).
     */
    static void verifySideSeams() {
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
            Canvas strip = new Canvas(SIDE_W, SIDE_H * 2);
            sideColumn(strip, new int[]{a, b});
            Canvas top = sideCell(a), bottom = sideCell(b);
            for (int y = 0; y < SIDE_H * 2; y++) for (int x = 0; x < SIDE_W; x++) {
                int want = strip.get(x, y), got = y < SIDE_H ? top.get(x, y) : bottom.get(x, y - SIDE_H);
                if (want != got) throw new IllegalStateException("field_palisade_side: variant " + a + " over " + b + " breaks at " + x + "," + y);
            }
        }
    }

    // ------------------------------------------------------------------ crawl gap under the front palisade
    /**
     * A stake_wall cell (same size, anchor, stakes, rails) with no stake missing, whose feet have rotted away
     * over a burrow: splintered ends hang above a dark hollow 30 x 14 at ground level, wet mud spills out.
     * The rail at 18 px still carries the hanging stakes, so only something low and soft gets under.
     */
    static Canvas crawl() { return crawl(false); }
    /** `low`: the knee-high run (LOW_H cell, one rail) with a 12-row hollow instead of 14. */
    static Canvas crawl(boolean low) {
        Canvas c = low ? new Canvas(Props.STAKE_W, LOW_H) : new Canvas(Props.STAKE_W, Props.STAKE_H);
        int g = low ? LOW_GROUND : Props.STAKE_GROUND, x0 = 8, logs = 10, seed = 7100, hh = low ? 12 : 14;
        int[] foot = new int[Props.STAKE_W];                                  // lowest wood row per column
        java.util.Arrays.fill(foot, g);
        for (int i = 0; i < logs; i++) {
            int lx = x0 + i * 8, base = g - (i % 2);
            int height = low ? 36 + (int) (Noise.rnd(i, 0, seed) * 9) : 80 + (int) (Noise.rnd(i, 0, seed) * 14);
            stake(c, lx, base - height, height + 1, seed + i, false, false);
            if (lx + 8 < 34 || lx > 62) continue;
            int snap = g - (hh - 4) - (int) Math.round(Math.sqrt(Math.max(0, 1 - Math.pow((lx + 4 - 48) / 16.0, 2))) * (hh - 10)) - (int) (Noise.rnd(i, 1, seed) * 2);
            boolean slantRight = Noise.rnd(i, 2, seed) < .5;
            int sliver = (int) (Noise.rnd(i, 3, seed) * 8);
            for (int col = 0; col < 8; col++) {                               // rotted off: each stake snaps at a slant of its own
                int x = lx + col;
                if (x < 33 || x > 63) continue;
                int end = snap + (slantRight ? col : 7 - col) / 3 + (col == sliver ? 3 : 0);   // one long sliver per stake
                foot[x] = end;
                for (int y = end + 1; y <= base; y++) c.clear(x, y);
                c.set(x, end, col == sliver ? Pal.DRY[3] : col < 4 ? Pal.WOOD[4] : Pal.WOOD[2]);
            }
        }
        for (int rail : low ? new int[]{18} : new int[]{18, 56}) {           // binding rails and rope lashings, as Props
            int y = g - rail;
            for (int x = x0 - 1; x < x0 + logs * 8 + 1; x++) {
                c.set(x, y - 2, Pal.WOOD[5]); c.set(x, y - 1, Pal.WOOD[4]); c.set(x, y, Pal.WOOD[3]); c.set(x, y + 1, Pal.WOOD[2]); c.set(x, y + 2, Pal.WOOD[0]);
                if (Math.floorMod(x - x0, 16) == 5) for (int s = -3; s <= 3; s++) c.set(x + (s & 1), y + s, s < 0 ? Pal.DRY[3] : Pal.DRY[2]);
            }
        }
        for (int x = 33; x <= 63; x++) {                                      // the hollow: black deep in, earth at the rim
            double u = (x - 48) / 15.5;
            int bank = (int) Math.round((1 - Math.sqrt(Math.max(0, 1 - u * u))) * (hh - 4));   // rounded earth shoulders
            int roof = g - hh + bank;
            for (int y = foot[x] + 1; y < roof; y++) c.set(x, y, Math.abs(u) > .85 ? Pal.MUD[2] : Pal.MUD[1]);   // earth behind the stumps
            for (int y = Math.max(foot[x] + 1, roof); y <= g; y++) {
                boolean rim = y == roof || Math.abs(u) > .93;
                int colour = rim ? Pal.MUD[1] : Pal.INK;
                if (y == g - 1 && Math.abs(u) < .7) colour = Pal.MUD[0];        // the floor running on under the fence
                if (y == g) colour = Math.abs(u) < .8 ? Pal.MUD[2] : Pal.MUD[3];
                c.set(x, y, colour);
            }
        }
        for (int x = 31; x <= 66; x++) {                                      // wet spoil fanned out of the burrow
            double u = (x - 48) / 18.0;
            int rows = (int) Math.round(Math.sqrt(Math.max(0, 1 - u * u)) * 2.6 + Noise.value(x, 0, 3, seed + 20) - .5);
            for (int r = 1; r <= rows; r++) {
                int colour = r == rows ? Pal.MUD[2] : Pal.MUD[3];
                if (r == 1 && Noise.rnd(x, 0, seed + 21) < .35) colour = Pal.MUD[4];    // wet sheen
                c.set(x, g + r, colour);
            }
        }
        int[][] trickle = {{50, g - 1}, {50, g}, {49, g + 1}, {49, g + 2}, {48, g + 3}, {47, g + 3}};
        for (int[] t : trickle) c.set(t[0], t[1], Pal.WATER[2]);
        c.set(51, g, Pal.WATER[1]); c.set(50, g + 1, Pal.WATER[1]);
        c.set(50, g - 1, Pal.WATER[4]); c.set(49, g + 2, Pal.WATER[5]); c.set(46, g + 3, Pal.WATER[3]);
        c.blit(Decals.TUFT_C.art(), 0, g - Decals.TUFT_C.art().h + 3);
        c.blit(Decals.TUFT_B.art(), 80, g - Decals.TUFT_B.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ knee-high front run
    /**
     * The south side of a yard, cut down so the camera sees over it into the yard (the 3/4 convention):
     * the same stakes and lower rail as stake_wall, tops 36-44 px above the ground, still sharpened or
     * splintered so it reads as a barrier. Cells are 96 x LOW_H with the ground row LOW_H - 5, as stake_wall.
     */
    static final int LOW_H = 56, LOW_GROUND = LOW_H - 5;
    static Canvas lowFence(int v) {
        Canvas c = new Canvas(Props.STAKE_W, LOW_H);
        int g = LOW_GROUND, x0 = 8, logs = 10, seed = 7500 + v * 13;
        for (int i = 0; i < logs; i++) {
            boolean broken = Noise.rnd(i, v, seed) > (v == 0 ? .92 : v == 1 ? .6 : .75);
            int height = broken ? 34 + (int) (Noise.rnd(i, v, seed + 1) * 5) : 36 + (int) (Noise.rnd(i, v, seed + 2) * 9);
            stake(c, x0 + i * 8, g - (i % 2) - height, height + 1, seed + i, broken, false);
        }
        int y = g - 18;                                                        // the lower rail and its lashings, as Props
        for (int x = x0 - 1; x < x0 + logs * 8 + 1; x++) {
            c.set(x, y - 2, Pal.WOOD[5]); c.set(x, y - 1, Pal.WOOD[4]); c.set(x, y, Pal.WOOD[3]); c.set(x, y + 1, Pal.WOOD[2]); c.set(x, y + 2, Pal.WOOD[0]);
            if (Math.floorMod(x - x0, 16) == 5) for (int s = -3; s <= 3; s++) c.set(x + (s & 1), y + s, s < 0 ? Pal.DRY[3] : Pal.DRY[2]);
        }
        if (v == 2) c.blit(Decals.ascii("O.O..", ".OO..", "..xc.", "...xc", "....x"), 58, g - 31);   // a spent arrow
        if (v == 1) c.blit(Decals.PLANK.art(), 30, g - 2);
        c.blit(Decals.TUFT_C.art(), 0, g - Decals.TUFT_C.art().h + 3);
        c.blit(Decals.TUFT_B.art(), 80, g - Decals.TUFT_B.art().h + 3);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ small finds
    private static final Map<Character, Integer> METAL = legend("012345", Pal.GOLD, 0, "ghijkl", Pal.IRON, 0,
            "abcdef", Pal.CANVAS, 0, "rs", Pal.RUST, 1, "vwxyz", Pal.WOOD, 1);

    /** An old brass key dropped on the ground; a glint runs from the bow to the bit (frame 0 has none). */
    static Canvas key(int frame) {
        Canvas c = new Canvas(12, 12);
        paint(c, 0, 0, METAL,
                "............",
                "..33........",
                ".3..2.......",
                ".3..2.......",
                "..2232......",
                ".....32.....",
                "......32....",
                ".......32...",
                "........32..",
                ".......2132.",
                "........1...",
                "............");
        int[][] glint = {null, {2, 1}, {6, 6}, {9, 8}};
        if (frame > 0) {
            int gx = glint[frame][0], gy = glint[frame][1];
            c.set(gx, gy, Pal.GOLD[5]);
            for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) c.tint(gx + d[0], gy + d[1], Pal.GOLD[4]);
        }
        c.selectiveOutline();
        return c;
    }

    /** The quartermaster's tin: dented lid, brass latch, a pale cloth band with a scrawled hand. No emblem. */
    static Canvas tin() {
        Canvas c = new Canvas(14, 12);
        paint(c, 1, 0, METAL,
                "............",
                ".kkkkkkkkj..",
                "kjjjjjjhljj.",
                "kjjjjjjjhlj.",
                "jjjjj32jjiii",
                "iiiii21iihhh",
                "feeeeeeeeddc",
                "fecceceeddcc",
                "jiiiiiiihhhg",
                "ihhshhhhhhgg",
                ".gggggggggg.");
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ the locked cache
    /** Iron-bound plank chest, 26 px wide: frame 0 shut under a brass padlock, frame 1 lid thrown back, empty. */
    static Canvas chest(boolean open) {
        Canvas c = new Canvas(32, 28);
        int x0 = 3, x1 = 28, g = 25, rim = 14, seed = 7300;
        for (int y = rim; y <= g; y++) for (int x = x0; x <= x1; x++) {     // front face: three horizontal planks
            int inPlank = (y - rim) % 4;
            int colour = inPlank == 0 ? Pal.WOOD[1] : inPlank == 1 ? Pal.WOOD[4] : Pal.WOOD[3];
            if (x == x0) colour = Pal.shift(colour, 1);
            if (x >= x1 - 1) colour = Pal.shift(colour, -1);
            if (y == g) colour = Pal.WOOD[1];
            if (inPlank > 1 && Noise.rnd(x / 3, y, seed) < .12) colour = Pal.WOOD[2];          // grain
            c.set(x, y, colour);
        }
        if (!open) {                                                     // domed lid: top in light, front in shade
            int[] rows = {Pal.WOOD[5], Pal.WOOD[5], Pal.WOOD[4], Pal.WOOD[4], Pal.WOOD[4], Pal.WOOD[3], Pal.WOOD[3], Pal.WOOD[2]};
            for (int r = 0; r < rows.length; r++) {
                int inset = r == 0 ? 2 : r == 1 ? 1 : 0, y = rim - rows.length + r;
                for (int x = x0 + inset; x <= x1 - inset; x++) {
                    int colour = rows[r];
                    if (Math.floorMod(x - x0, 6) == 5 && r < 5) colour = Pal.WOOD[2];          // lid slats
                    if (x >= x1 - 1) colour = Pal.shift(colour, -1);
                    c.set(x, y, colour);
                }
            }
            c.hline(x0, x1, rim - 1, Pal.IRON[2]);                       // iron edge of the lid
            c.hline(x0 + 1, x1 - 1, rim - 2, Pal.IRON[3]);
        } else {
            for (int y = 2; y <= 8; y++) {                                       // lid thrown back: its underside, slats
                int inset = y == 2 ? 2 : y == 3 ? 1 : 0;                         // the dome's rounded edge
                for (int x = x0 + 1 + inset; x <= x1 - 1 - inset; x++) {
                    int colour = Math.floorMod(x - x0, 6) == 5 ? Pal.WOOD[1] : x == x0 + 1 + inset ? Pal.WOOD[3] : Pal.WOOD[2];
                    if (y == 2) colour = Pal.IRON[3];
                    if (y == 8) colour = Pal.WOOD[1];                                 // hinge edge in shadow
                    if (x >= x1 - 2 && y > 2 && y < 8) colour = Pal.shift(colour, -1);
                    c.set(x, y, colour);
                }
            }
            for (int x = x0; x <= x1; x++) {                                     // looking in: back rim, back wall, bare floor boards
                boolean side = x <= x0 + 1 || x >= x1 - 1;
                c.set(x, 9, Pal.WOOD[4]);
                for (int y = 10; y < rim; y++) {
                    int colour = y == 10 ? Pal.WOOD[2] : y == 11 ? Pal.WOOD[1] : y == 12 ? (Math.floorMod(x - x0, 7) == 3 ? Pal.WOOD[1] : Pal.WOOD[3]) : Pal.WOOD[0];
                    if (side) colour = x == x0 || x == x1 ? Pal.WOOD[4] : x == x0 + 1 ? Pal.WOOD[1] : Pal.WOOD[3];   // walls: left in shade, right lit
                    c.set(x, y, colour);
                }
                c.set(x, rim, Pal.WOOD[5]);
            }
            c.rect(15, 0, 2, 3, Pal.IRON[4]); c.set(16, 1, Pal.IRON[2]);           // the hasp, now pointing up
        }
        int top = open ? 2 : rim - 8;
        for (int sx : new int[]{x0 + 3, x1 - 4}) {                       // straps over lid and body, rivets lit
            for (int y = top; y <= g; y++) {
                if (open && y > 8 && y < rim) continue;
                if (!c.opaque(sx, y)) continue;
                c.set(sx, y, Pal.IRON[4]); c.set(sx + 1, y, Pal.IRON[2]);
            }
            for (int y : new int[]{rim + 2, g - 2}) c.set(sx, y, Pal.IRON[5]);
        }
        for (int y = rim; y <= g; y += g - rim) { c.set(x0, y, Pal.IRON[3]); c.set(x1, y, Pal.IRON[2]); }   // corner caps
        c.rect(15, rim, 2, 2, Pal.IRON[3]); c.set(16, rim + 1, Pal.IRON[1]);         // staple on the front
        if (!open) {
            c.rect(15, rim - 4, 2, 4, Pal.IRON[4]); c.set(16, rim - 1, Pal.IRON[2]);   // hasp down over the staple
            paint(c, 13, rim + 1, METAL,                                 // brass padlock with its shackle through the staple
                    ".hkh.",
                    ".h.h.",
                    "34443",
                    "43322",
                    "32K21",
                    "32K21",
                    ".111.");
        }
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ bedrolls
    /**
     * A ground sheet seen from above with a wool blanket turned back to show its lining, and the pack rolled
     * for a pillow (its spiralled end faces the camera). The middle stays plain for a survivor to sit on.
     */
    static Canvas bedroll(int v) {
        Canvas c = new Canvas(40, 16);
        boolean left = v == 0;                                                // which end the pillow is
        int[] mat = left ? Pal.CANVAS : Pal.DRY, wool = left ? Pal.CRIMSON : Pal.STONE;
        int top = left ? 1 : 3;                                               // the brightest wool step
        int mx0 = 2, mx1 = 37, my0 = 3, my1 = 12, seed = 7400 + v;
        for (int y = my0; y <= my1 + 1; y++) for (int x = mx0; x <= mx1; x++) {          // ground sheet, its edge in shade
            if ((y == my0 || y == my1 + 1) && (x == mx0 || x == mx1)) continue;
            int colour = y == my1 + 1 ? mat[1] : y == my0 ? mat[4] : x == mx0 ? mat[4] : mat[3];
            if (y > my0 && y <= my1 && Noise.rnd(x / 3, y, seed) < .06) colour = mat[2];
            c.set(x, y, colour);
        }
        int bx0 = left ? 13 : 3, bx1 = left ? 36 : 26, wy0 = my0 + 1;
        for (int y = wy0; y <= my1 + 1; y++) for (int x = bx0; x <= bx1; x++) {        // blanket: lit top, rumpled middle
            c.set(x, y, y == my1 + 1 ? wool[top - 1] : y == wy0 ? wool[top + 2] : wool[top + 1]);
        }
        for (int x = bx0 + 1; x < bx1; x++) for (int y = wy0 + 1; y < my1; y++) {   // soft rumples, lit on their upper side
            double n = Noise.value(x, y * 1.6, 5, seed + 1);
            if (n > .7) c.set(x, y, wool[top + 2]);
            else if (n < .3 && Noise.value(x, (y - 1) * 1.6, 5, seed + 1) >= .3) c.set(x, y, wool[top]);
        }
        int fold = left ? bx0 : bx1, dir = left ? 1 : -1;                      // turned-back edge: lining, then its shadow
        for (int y = wy0; y <= my1 + 1; y++) {
            boolean front = y == my1 + 1;
            c.set(fold, y, front ? Pal.BONE[0] : Pal.BONE[2]);
            c.set(fold + dir, y, front ? Pal.BONE[0] : Pal.BONE[3]);
            c.set(fold + 2 * dir, y, front ? Pal.BONE[0] : Pal.BONE[1]);
            c.set(fold + 3 * dir, y, wool[top - 1]);
        }
        int px = left ? 3 : 29;                                                // pack rolled across the head end
        for (int y = 1; y <= 10; y++) for (int x = px; x < px + 8; x++) {
            int colour = x == px ? mat[3] : x < px + 3 ? mat[mat.length - 1] : x < px + 6 ? mat[3] : mat[2];
            if (y == 1 && (x == px || x == px + 7)) continue;
            if (y == 3 || y == 7) colour = x < px + 4 ? Pal.WOOD[4] : Pal.WOOD[2];      // straps
            c.set(x, y, colour);
        }
        c.ellipse(px + 4, 12.5, 4, 2.6, mat[3]);                               // its near end, rolled in a spiral
        c.hline(px + 1, px + 6, 11, mat[mat.length - 1]);
        c.hline(px + 2, px + 5, 13, mat[2]);
        c.set(px + 5, 12, mat[2]); c.set(px + 3, 12, mat[1]);
        c.selectiveOutline();
        return c;
    }

    // ------------------------------------------------------------------ survivors
    static final int SURV_W = 48, SURV_H = 40;
    /**
     * Three hurt expedition members, columns: arm in a sling, bandaged leg, older man under a blanket.
     * Row 0 slumped where they were found, row 1 sitting up at the camp fire. A standing adult is ~64 px, so
     * seated they are 30-34 px; the seat sits on the bottom rows, the body centred on x 24. Rows below are
     * "start column, pixels": skin 1-4 dark to light (n a ruddy nose), hair 5-7, coat a-d, trousers o-r,
     * boots x-z, bandage and sling u v w W, blanket k l L M, belt t with buckle G, eyes e, blood R.
     */
    static Canvas survivors() {
        Map<Character, Integer> sling = person(new int[]{Pal.RUST[1], Pal.RUST[2], Pal.RUST[3], Pal.RUST[4]},
                new int[]{Pal.WOOD[0], Pal.WOOD[1], Pal.WOOD[2]}, Pal.STONE, 2, new int[]{Pal.MUD[1], Pal.MUD[2], Pal.MUD[4], Pal.MUD[5]});
        Map<Character, Integer> leg = person(new int[]{Pal.WOOD[1], Pal.WOOD[3], Pal.WOOD[4], Pal.WOOD[5]},
                new int[]{Pal.STONE[0], Pal.STONE[1], Pal.STONE[2]}, Pal.DRY, 1, new int[]{Pal.STONE[1], Pal.STONE[2], Pal.STONE[3], Pal.STONE[4]});
        Map<Character, Integer> elder = person(new int[]{Pal.RUST[2], Pal.CANVAS[4], Pal.CANVAS[5], Pal.BONE[3]},
                new int[]{Pal.IRON[3], Pal.IRON[4], Pal.IRON[5]}, Pal.STONE, 2, new int[]{Pal.MUD[1], Pal.MUD[2], Pal.MUD[4], Pal.MUD[5]});
        elder.put('n', Pal.RUST[3]);
        Canvas sheet = new Canvas(SURV_W * 3, SURV_H * 2);
        Object[][] cells = {{sling, HURT1, CALM1}, {leg, HURT2, CALM2}, {elder, HURT3, CALM3}};
        for (int col = 0; col < 3; col++) for (int row = 0; row < 2; row++) {
            @SuppressWarnings("unchecked") Map<Character, Integer> legend = (Map<Character, Integer>) cells[col][0];
            sheet.blit(seated(legend, (String[]) cells[col][1 + row]), col * SURV_W, row * SURV_H);
        }
        return sheet;
    }
    private static Map<Character, Integer> person(int[] skin, int[] hair, int[] coat, int coatFrom, int[] trousers) {
        Map<Character, Integer> m = legend("1234", skin, 0, "567", hair, 0, "abcd", coat, coatFrom, "opqr", trousers, 0,
                "xyz", Pal.WOOD, 1, "uvwW", Pal.BONE, 0, "klLM", Pal.CRIMSON, 1, "t", Pal.WOOD, 1, "g", Pal.WOOD, 3,
                "G", Pal.IRON, 4, "R", Pal.CRIMSON, 2);
        m.put('e', Pal.INK);
        m.put('n', skin[0]);
        return m;
    }
    private static Canvas seated(Map<Character, Integer> legend, String[] rows) {
        Canvas c = new Canvas(SURV_W, SURV_H);
        for (int i = 0; i < rows.length; i++) {
            int space = rows[i].indexOf(' ');
            paint(c, 12 + Integer.parseInt(rows[i].substring(0, space)), SURV_H - 1 - rows.length + i, legend, rows[i].substring(space + 1));
        }
        c.selectiveOutline();
        return c;
    }
    private static final String[] HURT1 = {
            "11 565",
            "10 56775",
            "9 556655",
            "7 5666666665",
            "6 566777666655",
            "6 566666666665",
            "6 566666666665",
            "6 566444336655",
            "7 5433333225",
            "7 5311331125",
            "7 4333333225",
            "8 23313222",
            "9 222221",
            "5 dddcw2ccbbb",
            "4 dddcwvcccbbba",
            "3 dddbdcwvccbabb",
            "3 ddcbdccwvcbabb",
            "3 dccbdcccwWWWWwv",
            "3 dcbbdcc3wWWwwvv",
            "3 dcbbdcc34wwvvvu",
            "3 dcbbbccuuvvvuub",
            "3 dcbtttttGtttttb",
            "2 c43rrqqqqqqqqqqqp",
            "1 432rrqqqqqqoqqqqqqp",
            "3 rrqqqqqooooqqqqqqp",
            "2 rrqqqqpo....oqqqqqpp",
            "1 rrqqqqp......pqqqqqp",
            "0 yzzyyqp........pqqzzyx",
            "0 zzzyyx..........xzzyyx",
            "0 zzyyyx..........xyyyyx",
            "1 yyyx............xyyx"};
    private static final String[] CALM1 = {
            "10 565",
            "9 56775",
            "9 55665",
            "7 566666665",
            "6 56677766665",
            "6 56666666665",
            "6 56644433665",
            "6 54333333325",
            "6 543e333e325",
            "6 54333333225",
            "7 433313322",
            "8 2333322",
            "9 12221",
            "5 dddcw212ccbbb",
            "4 ddddcwvccccbbba",
            "3 dddbdccwvcccbabb",
            "3 ddcbdcccwvccbabb",
            "3 dccbdccccwvcbabb",
            "3 dcbbdcccwWWWWwwv",
            "3 dcbbdcc3wWWwwwvv",
            "3 dcbbdcc34wwwvvvu",
            "3 dcbbbccuuvvvuuub",
            "3 c43tttttGttttttb",
            "2 rrr32qqqqqqqqqqqqqp",
            "1 rrrrqqqqqqqqqpqqqqqqqqp",
            "0 rrrqqqqqqqqqqpqqqqqqqqqqp",
            "0 rqqrrrqqqqqqqoqqqqqqrrrqp",
            "0 pqqprrqqoooooooooorrqqpqp",
            "1 ppqprrqooooooooorrqpqpp",
            "3 pzzyrrqooooorrqyzzp",
            "5 xzzyyzzyyzzyyzx",
            "7 xxxxxxxxxxx"};
    private static final String[] HURT2 = {
            "9 55666655",
            "8 5667766665",
            "8 5666666665",
            "8 5644443335",
            "8 5433333325",
            "8 2431131122",
            "8 2433333322",
            "9 33111132",
            "9 23333322",
            "10 222222",
            "6 dddcc2cbbbb",
            "4 dddddccdbcccbbbb",
            "3 dddbddccdbcccbbabb",
            "2 dddbdddccdbcccbbabba",
            "2 ddcbddcccdbcccbbabba",
            "2 ddcbdccccdbccccbabba",
            "2 dccbdccccdbcccbbabba",
            "2 dcbbdccccdbcc43babba",
            "2 dcbbtttttGttt32tbab",
            "1 c43rrrqqqqqqqqqqqqp",
            "0 432.rrqqqqqqoqqqqqqqp",
            "3 rrqqqqqqooooqqqqqqqp",
            "2 rrqqqqqpo....oqqqqqqpp",
            "1 rrqqqqqp......pWWWwwvup",
            "0 yzzyyqqp........vWwwRwvux",
            "0 zzzyyyx..........uwwvvuyx",
            "0 zzzyyyx..........xzzzyyx",
            "1 yyyyx............xyyyyx"};
    private static final String[] CALM2 = {
            "8 55666655",
            "7 5667766665",
            "7 5666666665",
            "7 5644443335",
            "7 5433333325",
            "7 243e33e322",
            "7 2433333322",
            "8 33311332",
            "8 23333322",
            "9 222222",
            "6 dddc2112cbbb",
            "4 dddddccdbcccbbbb",
            "3 dddbddccdbcccbbabb",
            "2 dddbdddccdbcccbbabba",
            "2 ddcbddcccdbcccbbabba",
            "2 ddcbdccccdbccccbabba",
            "2 dccbdccccdbcccbbabba",
            "2 dcbbdccccdbcccbbabba",
            "2 dcbbdcccbdbccbbbabba",
            "2 c43ttttttGtttttttt34a",
            "1 rrrrqqqqqqqqqqqqqqqqp",
            "0 rrrqqqqqqqqqqpqqqqqqqqp",
            "0 rqqrrrqqqqqqqoprrqqqqqqp",
            "0 pqqprrqqooooooopprrqqqqqp",
            "1 ppqprrqoooo...ppWWWwwwvu",
            "3 pzzyrrqoo.......vWwwRwvu",
            "5 xzzyyzx.........uwwwvvuu",
            "7 xxxxx...........rqqqqqpp",
            "24 yzzzyyx",
            "23 yzzzzyyxx",
            "24 xxxxxxx"};
    private static final String[] HURT3 = {
            "9 34443",
            "8 3444432",
            "7 344444332",
            "7 534444325",
            "7 633333326",
            "7 677337726",
            "7 631131126",
            "7 637666736",
            "6 LLl6777776kk",
            "5 LLLll67776kkkk",
            "4 LLLlllkcbckkkkk",
            "3 LLLllllklbkkkkkkk",
            "3 LLllllllklkkkkkkk",
            "2 LLLlllllllkllkkkkk",
            "2 LLlllkll43lklkkkkk",
            "2 LLlllkl4432klkkkkk",
            "2 Lllllklllklkkkkkkk",
            "1 LLlllkllllklllkkkkkk",
            "1 Lllllklllkklllkkkkkk",
            "1 LLLLLlllkLLLLllkkkkk",
            "1 LllllllkkLlllllkkkkk",
            "1 Lllllllkklllllkkkkkk",
            "1 Lllllllkklllllkkkkkk",
            "1 lllllllkkllllkkkkkkk",
            "1 llllllkkkkllllkkkkkk",
            "2 kkkkkkk..kkkkkkkkk",
            "3 zzyyx......zzyyx",
            "3 xxxx.......xxxx"};
    private static final String[] CALM3 = {
            "9 34443",
            "8 3444432",
            "7 534444325",
            "7 633333326",
            "7 677337726",
            "7 63e33e226",
            "7 6433n3226",
            "7 637666736",
            "8 6777776",
            "9 67776",
            "6 Lll66655kk",
            "4 LLLllcbcbbkkkk",
            "3 LLLllllcbcbkkkkk",
            "3 LLlllllcbcbbkkkk",
            "2 LLLllll3443lkkkkk",
            "2 LLlllkl4332lkkkkkk",
            "2 LLlllkllkcklkkkkkk",
            "2 Lllllkllkcklkkkkkk",
            "1 LLlllklllkcbllkkkkkk",
            "1 Lllllklllkbbllkkkkkk",
            "1 Llllkllllkbbllkkkkkk",
            "1 llllklllllbbllkkkkkk",
            "0 rrrkkkkkkkkkkkkkkkkkkpp",
            "0 rrrqqqqqqqqqqpqqqqqqqqqqp",
            "0 rqqrrrqqqqqqqoqqqqqqrrrqp",
            "0 pqqprrqqoooooooooorrqqpqp",
            "1 ppqprrqooooooooorrqpqpp",
            "3 pzzyrrqooooorrqyzzp",
            "5 xzzyyzzyyzzyyzx",
            "7 xxxxxxxxxxx"};
}
