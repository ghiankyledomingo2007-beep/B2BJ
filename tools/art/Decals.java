import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ground details at character scale (a person is ~64 native px tall), lit from the upper left.
 * Hand-pixelled items are written as text, one character per pixel ('.' transparent); grass,
 * reeds and stones are grown by small rules so their many variants stay consistent.
 */
final class Decals {
    private Decals() { }

    record Decal(String name, Canvas art, boolean raised) { }

    static final Map<Character, Integer> LEGEND = new HashMap<>();
    static {
        put("qwerty", Pal.GRASS, 0);          // q dark .. y brightest grass
        put("asdfhjk", Pal.STONE, 1);         // a STONE[1] .. k STONE[7]
        put("zxcvb", Pal.WOOD, 1);            // z WOOD[1] .. b WOOD[5]
        put("nmNML", Pal.IRON, 1);            // n IRON[1] .. L IRON[5]
        put("opPO", Pal.BONE, 0);             // o BONE[0] .. O BONE[3]
        put("RTYU", Pal.CRIMSON, 1);          // R CRIMSON[1] .. U CRIMSON[4]
        put("DFGH", Pal.DRY, 1);              // D DRY[1] .. H DRY[4]
        put("CVBXZ", Pal.CYAN, 1);            // C CYAN[1] .. Z CYAN[5]
        put("uil", Pal.MUD, 1);               // u MUD[1] i MUD[2] l MUD[3]
        put("QWE", Pal.RUST, 1);              // Q RUST[1] .. E RUST[3]
        put("ASJI", Pal.GOLD, 1);             // A GOLD[1] .. I GOLD[4]
        LEGEND.put('K', Pal.INK);
    }
    private static void put(String keys, int[] ramp, int from) {
        for (int i = 0; i < keys.length(); i++) LEGEND.put(keys.charAt(i), ramp[from + i]);
    }

    static Canvas ascii(String... rows) {
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        Canvas c = new Canvas(w, rows.length);
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            char ch = rows[y].charAt(x);
            if (ch == '.') continue;
            Integer colour = LEGEND.get(ch);
            if (colour == null) throw new IllegalArgumentException("unknown pixel '" + ch + "' in " + String.join("/", rows));
            c.set(x, y, colour);
        }
        return c;
    }

    // ------------------------------------------------------------------ grown: grass, reeds, stones
    /** A clump of blades fanning from one root, dark at the base, lit at the tips. */
    static Canvas tuft(int blades, int spread, int height, int[] ramp, int seed) {
        int w = spread * 2 + 7, h = height + 2;
        Canvas c = new Canvas(w, h);
        int base = h - 1, cx = w / 2;
        for (int b = 0; b < blades; b++) {
            double t = blades == 1 ? .5 : b / (blades - 1.0);
            int bx = cx + (int) Math.round((t - .5) * spread * 1.2);
            int len = (int) Math.round(height * (.55 + .45 * Math.sin(t * Math.PI)) * (.85 + Noise.rnd(b, seed, 1) * .3));
            int tipX = bx + (int) Math.round((t - .5) * spread * 1.8 + (Noise.rnd(b, seed, 2) - .5) * 3);
            int tipY = base - len;
            int steps = Math.max(Math.abs(tipX - bx), base - tipY);
            for (int s = 0; s <= steps; s++) {
                double u = s / (double) Math.max(1, steps);
                int x = (int) Math.round(bx + (tipX - bx) * u * u), y = (int) Math.round(base - (base - tipY) * u);
                int colour = u > .86 ? ramp[Math.min(ramp.length - 1, 4)] : u > .6 ? ramp[3] : u > .3 ? ramp[2] : ramp[1];
                c.set(x, y, colour);
            }
        }
        for (int x = cx - spread / 2 - 1; x <= cx + spread / 2 + 1; x++) c.set(x, base, ramp[0]);
        return c;
    }

    static Canvas reeds(int stalks, int height, int seed) {
        Canvas c = tuft(stalks + 2, 3, height * 2 / 3, Pal.GRASS, seed);
        Canvas out = new Canvas(c.w + 2, height + 2);
        out.blit(c, 1, out.h - c.h);
        for (int s = 0; s < 2; s++) {                   // two cattails above the leaves
            int x = 3 + s * (out.w - 7), top = 1 + s * 3;
            for (int y = top + 5; y < out.h - 2; y++) out.set(x + 1, y, Pal.GRASS[2]);
            for (int y = top; y < top + 6; y++) { out.set(x, y, y == top ? Pal.WOOD[4] : Pal.WOOD[3]); out.set(x + 1, y, y == top ? Pal.WOOD[3] : Pal.WOOD[2]); }
            out.set(x, top - 1, Pal.GRASS[3]);
        }
        return out;
    }

    /** A stone lit from the upper left; mossy ones carry a cap of moss. */
    static Canvas stone(int w, int h, boolean mossy, int seed) {
        Canvas c = new Canvas(w, h);
        double cx = w / 2.0, cy = h / 2.0 + .5;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x + .5 - cx) / (w / 2.0), dy = (y + .5 - cy) / (h / 2.0);
            double r = Math.hypot(dx, dy) + (Noise.value(x, y, 2.2, seed) - .5) * .35;
            if (r > 1 || (dy > .55 && Math.abs(dx) > .8)) continue;
            double lit = -dx * .55 - dy * .85 + (Noise.value(x, y, 1.6, seed + 1) - .5) * .4;
            int idx = lit > .75 ? 6 : lit > .35 ? 5 : lit > -.15 ? 4 : lit > -.55 ? 3 : 2;
            int colour = Pal.STONE[idx];
            if (mossy && dy < -.25 && Noise.value(x, y, 2, seed + 2) > .35) colour = lit > .5 ? Pal.GRASS[3] : Pal.GRASS[2];
            if (r > .82 && dy > .2) colour = Pal.STONE[1];
            c.set(x, y, colour);
        }
        return c;
    }

    // ------------------------------------------------------------------ vegetation
    static final Decal TUFT_A = new Decal("tuft_a", tuft(7, 4, 9, Pal.GRASS, 11), false);
    static final Decal TUFT_B = new Decal("tuft_b", tuft(5, 3, 7, Pal.GRASS, 12), false);
    static final Decal TUFT_C = new Decal("tuft_c", tuft(9, 6, 8, Pal.GRASS, 13), false);
    static final Decal TUFT_D = new Decal("tuft_d", tuft(3, 2, 5, Pal.GRASS, 14), false);
    static final Decal DRY_TUFT = new Decal("dry_tuft", tuft(7, 4, 9, new int[]{Pal.DRY[0], Pal.DRY[1], Pal.DRY[2], Pal.DRY[3], Pal.DRY[4]}, 15), false);
    static final Decal DRY_SPRIG = new Decal("dry_sprig", tuft(4, 2, 5, new int[]{Pal.DRY[0], Pal.DRY[1], Pal.DRY[2], Pal.DRY[3], Pal.DRY[4]}, 16), false);
    static final Decal REEDS = new Decal("reeds", reeds(5, 18, 17), false);
    static final Decal REEDS_SMALL = new Decal("reeds_small", tuft(5, 2, 11, Pal.GRASS, 18), false);
    static final Decal SHROOMS = new Decal("shrooms", ascii(
            "..BXX......",
            ".BXZZB..BX.",
            "BXXZXXB.XZB",
            ".CVVVC.BXXB",
            "..VVC...VC.",
            "..VC....VC.",
            ".qqww..qwq."), false);
    static final Decal SHROOM = new Decal("shroom", ascii(
            ".BXX.",
            "BXZXB",
            ".VVC.",
            "..VC.",
            ".qwq."), false);
    static final Decal BRANCH = new Decal("branch", ascii(
            "..............b.",
            "...........b.vc.",
            "......v...cxcx..",
            "b....vcx.vcx....",
            ".vcxxcx.xcx.....",
            "..xcxxcxxx......",
            "...zx..zz......."), false);

    // ------------------------------------------------------------------ stone
    static final Decal ROCK_S = new Decal("rock_s", stone(8, 6, false, 21), true);
    static final Decal ROCK_M = new Decal("rock_m", stone(13, 10, true, 22), true);
    static final Decal BOULDER = new Decal("boulder", stone(21, 15, true, 23), true);
    static final Decal RUBBLE = new Decal("rubble", rubble(), true);
    static final Decal CHIP = new Decal("chip", ascii(
            "hff.",
            "fdda",
            ".aa."), true);
    private static Canvas rubble() {
        Canvas c = new Canvas(13, 8);
        c.blit(stone(6, 5, false, 24), 0, 3); c.blit(stone(7, 6, false, 25), 5, 2); c.blit(stone(4, 3, false, 26), 4, 0);
        return c;
    }

    // ------------------------------------------------------------------ the fallen expedition
    static final Decal SWORD = new Decal("sword", ascii(
            "...nMn...",
            "...MLM...",
            "....n....",
            "....x....",
            "....c....",
            "....x....",
            "....c....",
            "nmMMLMMmn",
            ".nmNLMmn.",
            "....LM...",
            "....LN...",
            "....LN...",
            "....LN...",
            "....MN...",
            "....MN...",
            "....Mm...",
            "....Nm...",
            "...uNmu..",
            "..uiiiuu.",
            "...uuu..."), true);
    static final Decal ARROW = new Decal("arrow", ascii(
            "O.O..",
            "POP..",
            ".v...",
            ".c...",
            ".x...",
            ".c...",
            ".x...",
            "uxu..",
            ".u..."), true);
    static final Decal ARROWS = new Decal("arrows", ascii(
            "O.O.....O.O",
            "POP.O.O.POP",
            ".v..POP..v.",
            ".c...v...c.",
            ".x...c...x.",
            ".c..x....c.",
            ".x.x.....x.",
            "uxux....uxu",
            ".uu......u."), true);
    static final Decal SHIELD = new Decal("shield", ascii(
            "....nmmmmmn....",
            "..nmUUUUUYYmn..",
            ".nYUUUUYYYYTmn.",
            "nmUUUYYYYYTTTmn",
            "mYUUYYnMMnTTTRm",
            "mYYYYTMLLMRTRRm",
            "mYYYTTnMMnRRRRm",
            "nmTTTTTRRRRRRmn",
            ".nmTTRRRRRRRmn.",
            "..nmRRRRRRmn...",
            "....nnnnnn....."), true);
    static final Decal HELMET = new Decal("helmet", ascii(
            "...nMMMMn...",
            "..nMLLLMNn..",
            ".nMLLMMMNNn.",
            ".mMMKKKKNNm.",
            ".mNMMKMNNmm.",
            ".mNNMKNNmmn.",
            "..nmmmmmmn..",
            "..uiiiiiiu.."), true);
    static final Decal SKULL = new Decal("skull", ascii(
            "..OOOP..",
            ".OOOOPP.",
            "OPKKPKKP",
            "OPKKPKKo",
            ".PPoPPo.",
            "..p.pp..",
            "..pppp.."), true);
    static final Decal BONES = new Decal("bones", ascii(
            ".OO.......OP.",
            "OOPPPPPPPPPPo",
            ".pPppppppppPp",
            "..p.......pp."), true);
    static final Decal RIBS = new Decal("ribs", ascii(
            "..O.O.O.O...",
            ".OP.OP.OP.O.",
            "OP.OP.OP.OP.",
            "P..P..P..P..",
            "P..P..P..p..",
            "p..p..p..p..",
            ".pPPPPPPPPo.",
            "..oooooooo.."), true);
    static final Decal SPEAR = new Decal("spear", spear(), true);
    static final Decal PLANK = new Decal("plank", ascii(
            "bvvvvvvvvvvb.vb...",
            "vccccccccccccvcvc.",
            "cxcxxxxxcxxxxcxxx.",
            ".zzzzzzzzzzzz.zz.."), true);
    static final Decal BANNER_SCRAP = new Decal("banner_scrap", ascii(
            "..ASA...........",
            ".UYYYYU.........",
            "UYYTTYYYU...U...",
            "YTTRRTTYYYUYY...",
            ".TRRRRRTTYYTTY..",
            "..RRRRRRRTTRRTY.",
            "...R..RRRRRRRRT.",
            ".......R..R.R..."), false);
    static final Decal ICHOR_STAIN = new Decal("ichor_stain", ascii(
            "....AAA.......",
            "..AASSSAA..A..",
            ".ASSJJJSSA.AA.",
            "ASJJIIJJSSA...",
            ".ASSJJJSSAA.A.",
            "..AASSAAA.....",
            "....AA........"), false);
    static final Decal ROPE = new Decal("rope", ascii(
            "...GGGGG...",
            ".GGFFFFFGG.",
            "GFF.GGG.FFG",
            "GF.GF.FG.FD",
            "FFG.FFF.GFD",
            ".DFFDDDFFD.",
            "...DDDDD..."), true);
    static final Decal BEDROLL = new Decal("bedroll", ascii(
            "...lPPPl......................",
            "..lOOOOPl.TTTTTTTTTTTTTTTTTT..",
            ".lOOOOPPPlYUUUUYYYYYYYYYYYYTR.",
            "lOOOPPPPPlUUUYYYYYYYYYYYYYYTRR",
            "lOPPPPPPolYYYYTYYYYYYTYYYYTTRR",
            "lPPPPPPoolYYYTYYYYYYTYYYYTTTRR",
            ".lpppppoolTTTTTTTTTTTTTTTTRRR.",
            "..lllll..RRRRRRRRRRRRRRRRRRR.."), false);

    private static Canvas spear() {
        Canvas c = new Canvas(30, 11);
        for (int s = 0; s <= 22; s++) {
            int x = 1 + s, y = 9 - s * 8 / 22;
            c.set(x, y, Pal.WOOD[4]); c.set(x, y + 1, Pal.WOOD[2]);
        }
        Canvas head = ascii("...LM", ".LLMn", "LMMn.", ".nn..");
        c.blit(head, 23, 0);
        c.set(22, 2, Pal.IRON[2]); c.set(21, 3, Pal.CRIMSON[3]); c.set(20, 3, Pal.CRIMSON[2]); c.set(21, 4, Pal.CRIMSON[2]);
        return c;
    }

    static final List<Decal> ALL = List.of(BEDROLL, TUFT_A, TUFT_B, TUFT_C, TUFT_D, DRY_TUFT, DRY_SPRIG, REEDS, REEDS_SMALL,
            SHROOMS, SHROOM, BRANCH, ROCK_S, ROCK_M, BOULDER, RUBBLE, CHIP, SWORD, ARROW, ARROWS, SHIELD, HELMET,
            SKULL, BONES, RIBS, SPEAR, PLANK, BANNER_SCRAP, ICHOR_STAIN, ROPE);

    /** Every decal on one sheet (for review and for the shipped tileset). */
    static Canvas sheet() {
        int cell = 32, cols = 8, rows = (ALL.size() + cols - 1) / cols;
        Canvas sheet = new Canvas(cols * cell, rows * cell);
        for (int i = 0; i < ALL.size(); i++) {
            Canvas a = ALL.get(i).art();
            sheet.blit(a, i % cols * cell + (cell - a.w) / 2, i / cols * cell + cell - 1 - a.h);
        }
        return sheet;
    }
}
