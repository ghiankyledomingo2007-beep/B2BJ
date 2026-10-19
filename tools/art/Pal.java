/**
 * Outpost night palette. Every ramp runs dark to light and hue-shifts: shadows lean blue,
 * highlights lean warm. All generated Outpost art draws from these colours only.
 */
final class Pal {
    private Pal() { }
    static int c(int rgb) { return 0xff000000 | rgb; }
    private static int[] ramp(int... rgb) {
        int[] out = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) out[i] = c(rgb[i]);
        return out;
    }

    static final int INK = c(0x090c11);
    static final int[] EARTH = ramp(0x12171a, 0x192022, 0x20292a, 0x283331, 0x323f3a, 0x3e4d44);
    static final int[] GRASS = ramp(0x13221f, 0x1a2e29, 0x223d33, 0x2e4f3e, 0x3e6449, 0x557d55);
    static final int[] DRY = ramp(0x2a2a1f, 0x3a3826, 0x4d4a30, 0x64603d, 0x7d774d);
    static final int[] MUD = ramp(0x17130f, 0x211b16, 0x2b241d, 0x362d24, 0x43392d, 0x554837);
    static final int[] STONE = ramp(0x0e121a, 0x161c27, 0x1f2735, 0x283344, 0x344155, 0x435269, 0x5a6b83, 0x7b8da4);
    static final int[] WATER = ramp(0x0c1420, 0x121d2e, 0x192940, 0x223654, 0x36507a, 0x6f8fb8);
    static final int[] WOOD = ramp(0x19120e, 0x261b14, 0x35261b, 0x463223, 0x5b412c, 0x735437);
    static final int[] RUST = ramp(0x24130f, 0x3e1f15, 0x5e301c, 0x804525, 0xa5602f);
    static final int[] IRON = ramp(0x12151b, 0x1f242c, 0x313841, 0x4a535d, 0x6c767e, 0x9aa3a6);
    static final int[] CRIMSON = ramp(0x240d14, 0x3f141c, 0x601c24, 0x84282c, 0xa8403a);
    static final int[] CANVAS = ramp(0x24211d, 0x353029, 0x4a4338, 0x615849, 0x7d725d, 0x9b8f75);
    static final int[] BONE = ramp(0x4a463d, 0x6c6758, 0x918a76, 0xb8b09a);
    static final int[] FIRE = ramp(0x4a160f, 0x8c2c16, 0xcc5222, 0xf08a32, 0xffc45e, 0xfff0bd);
    static final int[] GOLD = ramp(0x4a3210, 0x85591a, 0xc48a26, 0xeeb840, 0xffdd80, 0xfff3c8);
    static final int[] CYAN = ramp(0x0a2e35, 0x0f5560, 0x178a93, 0x2cc6c8, 0x7aeee6, 0xcffff9);
    static final int[] VIOLET = ramp(0x170c22, 0x2b1440, 0x4a2366, 0x763a98, 0xab68cf, 0xe2b6f5);

    static final int[][] RAMPS = {EARTH, GRASS, DRY, MUD, STONE, WATER, WOOD, RUST, IRON, CRIMSON,
            CANVAS, BONE, FIRE, GOLD, CYAN, VIOLET};

    /** GIMP palette of every Outpost colour; Pixie Dust and Aseprite load it directly. */
    static String gpl() {
        var text = new StringBuilder("GIMP Palette\nName: Outpost Night\nColumns: 8\n#\n");
        for (int c : all()) text.append(String.format("%3d %3d %3d\t#%06x%n", c >> 16 & 255, c >> 8 & 255, c & 255, c & 0xffffff));
        return text.toString();
    }

    /** Every colour, for the exported .gpl palette and palette checks. */
    static java.util.List<Integer> all() {
        var list = new java.util.ArrayList<Integer>();
        list.add(INK);
        for (int[] r : RAMPS) for (int v : r) if (!list.contains(v)) list.add(v);
        return list;
    }

    private static final java.util.Map<Integer, int[]> WHERE = new java.util.HashMap<>();
    static {
        for (int r = 0; r < RAMPS.length; r++) for (int i = 0; i < RAMPS[r].length; i++)
            WHERE.putIfAbsent(RAMPS[r][i], new int[]{r, i});
    }

    /** Same-ramp neighbour: positive steps brighten, negative darken, clamped to the ramp. */
    static int shift(int colour, int steps) {
        int[] at = WHERE.get(colour);
        if (at == null) return colour == INK && steps > 0 ? STONE[0] : colour;
        int[] r = RAMPS[at[0]];
        return r[Math.max(0, Math.min(r.length - 1, at[1] + steps))];
    }
    private static final java.util.Map<Long, Integer> LIT = new java.util.HashMap<>();
    private static final int[] ALL = all().stream().mapToInt(Integer::intValue).toArray();
    /**
     * The palette colour a surface takes under a coloured light: blend toward the light by `strength`,
     * then snap to the nearest palette entry, so lit ground stays inside the palette.
     */
    static int lit(int colour, int light, double strength) {
        long key = ((long) colour << 32) | ((light & 0xffffffL) << 8) | (long) (strength * 100);
        return LIT.computeIfAbsent(key, k -> {
            int r = (int) Math.round(((colour >> 16) & 255) * (1 - strength) + ((light >> 16) & 255) * strength);
            int g = (int) Math.round(((colour >> 8) & 255) * (1 - strength) + ((light >> 8) & 255) * strength);
            int b = (int) Math.round((colour & 255) * (1 - strength) + (light & 255) * strength);
            int best = colour; double bestD = Double.MAX_VALUE;
            for (int p : ALL) {
                double dr = ((p >> 16) & 255) - r, dg = ((p >> 8) & 255) - g, db = (p & 255) - b;
                double d = dr * dr * .3 + dg * dg * .59 + db * db * .11;
                if (d < bestD) { bestD = d; best = p; }
            }
            return best;
        });
    }

    /** Ramp position of a palette colour, or -1. */
    static int index(int colour) { int[] at = WHERE.get(colour); return at == null ? -1 : at[1]; }
}
