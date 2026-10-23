import java.util.ArrayList;
import java.util.List;

/**
 * Paints the whole Ruined Outpost floor at native resolution (one pixel = two world pixels) from
 * the live map layout: roads follow the real routes, plazas sit on the real landmarks, the forest
 * canopy fills the real boundary banks. Re-run whenever CampaignWorld changes.
 */
final class GroundBake {
    enum Style { MUDFLAT, CAMP, GRAVEL, TRACK, COBBLE, FLAG, ARENA }
    /** Floor of each landmark's clearing, in CampaignWorld landmark order. */
    static final Style[] PLAZA = {Style.MUDFLAT, Style.CAMP, Style.GRAVEL, Style.TRACK, Style.COBBLE,
            Style.COBBLE, Style.COBBLE, Style.FLAG, Style.ARENA, Style.FLAG, Style.CAMP, Style.GRAVEL};

    static final byte SOIL = 1, GRASSY = 2, MUDDY = 3, PUDDLE = 4, ROAD = 5, RUT = 6, GRAVEL = 7, SLAB = 8,
            MORTAR = 9, BROKEN = 10, COBBLE = 11, GAP = 12, CAMPDIRT = 13, CANOPY = 14, INLAY = 15, MEADOW = 16;

    final CampaignWorld.Area area;
    final RuinedOutpostMap map;
    final int W, H;
    final byte[] label;
    final int[] id;
    final float[] edge;
    final byte[] zone;
    final float[] lat;
    final boolean[] canopy;
    final int[] out;
    private final List<double[]> roads = new ArrayList<>();

    GroundBake() {
        area = CampaignWorld.area(0);
        map = RuinedOutpostMap.campaign(0);
        W = area.width() / 2; H = area.height() / 2;
        label = new byte[W * H]; id = new int[W * H]; edge = new float[W * H]; zone = new byte[W * H];
        lat = new float[W * H]; canopy = new boolean[W * H]; out = new int[W * H];
        var lm = area.landmarks();
        for (int[] r : CampaignWorld.routes())
            roads.add(new double[]{lm.get(r[0]).x(), lm.get(r[0]).y(), lm.get(r[1]).x(), lm.get(r[1]).y()});
        roads.add(new double[]{area.boss().x(), area.boss().y(), area.exit().x(), area.exit().y()});
        for (var bank : map.banks()) {
            int x0 = (int) Math.floor((bank.centerX() - bank.width() / 2) / 2), x1 = (int) Math.ceil((bank.centerX() + bank.width() / 2) / 2);
            int y0 = (int) Math.floor((bank.centerY() - bank.height() / 2) / 2), y1 = (int) Math.ceil((bank.centerY() + bank.height() / 2) / 2);
            for (int y = Math.max(0, y0); y < Math.min(H, y1); y++) for (int x = Math.max(0, x0); x < Math.min(W, x1); x++) canopy[y * W + x] = true;
        }
    }

    Canvas bake() {
        layout();
        aprons();
        paint();
        scatter();
        shadows();
        setPieces();
        new Canopy(W, H, canopy, out).paint();
        Canvas c = new Canvas(W, H);
        System.arraycopy(out, 0, c.px, 0, out.length);
        return c;
    }

    byte at(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H ? 0 : label[y * W + x]; }

    // ---------------------------------------------------------------- layout: what each pixel is
    private void layout() {
        var lms = area.landmarks();
        for (int ny = 0; ny < H; ny++) for (int nx = 0; nx < W; nx++) {
            int i = ny * W + nx;
            zone[i] = -2; lat[i] = Float.NaN;
            if (canopy[i]) { label[i] = CANOPY; continue; }
            double wx = (nx + .5) * 2, wy = (ny + .5) * 2;
            double best = 1e18, side = 0;
            for (double[] s : roads) {
                double dx = s[2] - s[0], dy = s[3] - s[1], len2 = dx * dx + dy * dy;
                double t = Math.max(0, Math.min(1, ((wx - s[0]) * dx + (wy - s[1]) * dy) / len2));
                double px = s[0] + dx * t - wx, py = s[1] + dy * t - wy, d = px * px + py * py;
                if (d < best) { best = d; side = ((wx - s[0]) * dy - (wy - s[1]) * dx) / Math.sqrt(len2); }
            }
            // roads meander and breathe: the centreline drifts and the width varies along the way
            double drift = (Noise.fbm(nx, ny, 70, 2, 12) - .5) * 30;
            double breathe = (Noise.fbm(nx, ny, 45, 2, 13) - .5) * 26;
            double roadD = Math.sqrt(best) - 112 - breathe;
            side += drift;
            double plazaD = 1e9; int near = -1;
            for (int k = 0; k < lms.size(); k++) {
                var l = lms.get(k);
                double d = Math.hypot(wx - l.x(), wy - l.y()) - (l.optional() ? 144 : 192);
                if (d < plazaD) { plazaD = d; near = k; }
            }
            plazaD += (Noise.fbm(nx, ny, 22, 2, 14) - .5) * 34;
            double wobble = (Noise.fbm(nx, ny, 7, 2, 11) - .5) * 12;
            if (plazaD < wobble) {
                zone[i] = (byte) near;
                edge[i] = (float) ((plazaD - wobble) / 2);
                plaza(nx, ny, i, PLAZA[near], lms.get(near));
            } else if (roadD < wobble) {
                zone[i] = -1;
                edge[i] = (float) ((roadD - wobble) / 2);
                lat[i] = (float) side;
                double rut = Math.abs(Math.abs(side) - 44);
                label[i] = rut < 8 ? RUT : ROAD;
            } else {
                edge[i] = (float) ((Math.min(roadD, plazaD) - wobble) / 2);
                wild(nx, ny, i);
            }
        }
    }

    private void wild(int x, int y, int i) {
        // crisp regions, no soft gradients: dark grass, lighter meadow, bare dirt, wet hollows
        double bare = Noise.fbm(x, y, 34, 3, 2) - Math.max(0, 1 - edge[i] / 8.0) * .12;
        double meadow = Noise.fbm(x, y, 64, 3, 1);
        double wet = Noise.fbm(x, y, 52, 2, 3);
        if (wet > .74) label[i] = wet > .78 ? PUDDLE : MUDDY;
        else if (bare < .245) label[i] = SOIL;
        else label[i] = meadow > .56 ? MEADOW : GRASSY;
    }

    private void plaza(int x, int y, int i, Style style, CampaignWorld.Landmark lm) {
        switch (style) {
            case MUDFLAT -> {
                double n = Noise.fbm(x, y, 15, 2, 31);
                label[i] = n > .55 ? PUDDLE : n > .42 ? MUDDY : SOIL;
            }
            case CAMP -> label[i] = CAMPDIRT;
            case GRAVEL -> label[i] = GRAVEL;
            case TRACK -> label[i] = ROAD;
            case COBBLE -> {
                int[] cell = Pebbles.nearest(x, y, 10, 41);
                id[i] = cell[0];
                boolean missing = Noise.rnd(cell[0], 3, 43) < brokenChance(edge[i]);
                label[i] = missing ? BROKEN : cell[1] == 1 ? GAP : COBBLE;
            }
            case FLAG, ARENA -> {
                int[] slab = Slabs.at(x, y);
                id[i] = slab[0];
                boolean missing = Noise.rnd(slab[0], 7, 47) < brokenChance(edge[i]) || Noise.rnd(slab[0], 9, 49) < .025;
                label[i] = missing ? BROKEN : slab[1] == 1 ? MORTAR : SLAB;
                if (style == Style.ARENA && label[i] != BROKEN) {
                    double r = Math.hypot((x + .5) * 2 - lm.x(), (y + .5) * 2 - lm.y());
                    if (Math.abs(r - 150) < 7 || Math.abs(r - 64) < 5) label[i] = INLAY;
                }
            }
        }
    }

    /** Near a clearing's edge whole stones are gone; deep inside they hold. */
    private static double brokenChance(float edgeNative) {
        return Math.max(0, Math.min(.92, (edgeNative + 11) / 11.0 * .92));
    }

    // ---------------------------------------------------------------- paint: colour by material
    private void paint() {
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) out[y * W + x] = colour(x, y);
    }


    private int colour(int x, int y) {
        int i = y * W + x;
        byte l = label[i], up = at(x, y - 1), down = at(x, y + 1), left = at(x - 1, y);
        return switch (l) {
            case SOIL -> soil(x, y, up);
            case GRASSY, MEADOW -> grass(x, y, l, up, down);
            case MUDDY -> {
                boolean wetUp = up == MUDDY || up == PUDDLE, wetDown = down == MUDDY || down == PUDDLE;
                yield !wetUp ? Pal.MUD[1] : !wetDown ? Pal.MUD[3] : Noise.rnd(x, y, 341) < .02 ? Pal.MUD[4] : Pal.MUD[2];
            }
            case PUDDLE -> water(x, y, up, down);
            case ROAD -> road(x, y, up);
            case RUT -> {
                if (Noise.value(x, y, 14, 17) > .64) yield water(x, y, up == RUT ? PUDDLE : up, down == RUT ? PUDDLE : down);
                if (up != RUT) yield Pal.MUD[0];
                if (down != RUT) yield Pal.MUD[3];
                if (left != RUT) yield Pal.MUD[0];
                if (at(x + 1, y) != RUT) yield Pal.MUD[2];
                yield Pal.MUD[1];
            }
            case GRAVEL -> {
                // packed yard: dirt with dense small stones, each with a lit top and a shadow under it
                if (gravel(x, y)) yield Noise.rnd(x, y, 55) < .25 ? Pal.STONE[4] : Pal.STONE[3];
                if (gravel(x, y - 1)) yield Pal.MUD[1];
                yield Noise.rnd(x, y, 56) < .1 ? Pal.STONE[2] : Pal.MUD[2];
            }
            case SLAB -> slab(x, y, i);
            case MORTAR -> Noise.value(x, y, 5, 61) > .62 ? Pal.GRASS[1] : Pal.STONE[1];
            case BROKEN -> {
                if (up == SLAB || up == COBBLE || up == INLAY) yield Pal.MUD[1];
                yield Noise.rnd(x, y, 63) < .07 ? Pal.STONE[3] : Pal.MUD[2];
            }
            case COBBLE -> cobble(x, y, i);
            case GAP -> Noise.value(x, y, 4, 67) > .6 ? Pal.GRASS[1] : Pal.MUD[1];
            case CAMPDIRT -> {
                if (Noise.rnd(x / 3, y, 73) < .045) yield Pal.DRY[2];
                if (Noise.rnd(x, y, 74) < .015) yield Pal.DRY[3];
                yield speck(x, y) ? Pal.MUD[2] : Pal.MUD[3];
            }
            case INLAY -> {
                // a carved groove in the paving, dim gold runes along it: decoration, never a warning colour
                int c = Pal.STONE[2];
                if (Noise.rnd(x / 3, y / 2, 79) < .2) c = Pal.GOLD[0];
                if (up != INLAY) c = Pal.STONE[1];
                else if (down != INLAY) c = Pal.STONE[4];
                yield c;
            }
            default -> Pal.EARTH[0];
        };
    }

    private int water(int x, int y, byte up, byte down) {
        if (up != PUDDLE) return Pal.WATER[1];
        if (down != PUDDLE) return Pal.WATER[3];
        if (Noise.rnd(x / 3, y, 81) < .045) return Pal.WATER[4];
        if (Noise.rnd(x, y, 82) < .004) return Pal.WATER[5];
        return Pal.WATER[2];
    }

    /** Grass edge fringe: blade tips hang one pixel over whatever lies below a meadow. */
    private static boolean bladeTip(int x, int y) { return Noise.rnd(x, y, 311) < .5; }

    private static boolean grassy(byte l) { return l == GRASSY || l == MEADOW; }

    private int soil(int x, int y, byte up) {
        if (grassy(up)) return bladeTip(x, y) ? (up == MEADOW ? Pal.GRASS[2] : Pal.GRASS[1]) : Pal.MUD[1];
        if (pebble(x, y)) return Noise.rnd(x, y, 15) < .35 ? Pal.STONE[4] : Pal.STONE[3];
        if (pebble(x, y - 1)) return Pal.MUD[0];
        if (speck(x, y)) return Pal.MUD[1];
        return Noise.rnd(x, y, 333) < .03 ? Pal.MUD[3] : Pal.MUD[2];
    }
    private static boolean gravel(int x, int y) { return Noise.rnd(x, y, 54) < .075; }
    private static boolean pebble(int x, int y) { return Noise.rnd(x, y, 331) < .011; }
    private static boolean speck(int x, int y) { return Noise.rnd(x, y, 332) < .05 && Noise.rnd(x, y - 1, 332) >= .05; }
    /** Large soft tonal shifts are ordered-dithered, never blotched. */
    private static int dither(int x, int y, double field, double threshold, int above, int below) {
        return field + (Noise.bayer(x, y) - .5) * .1 > threshold ? above : below;
    }

    private int grass(int x, int y, byte l, byte up, byte down) {
        boolean light = l == MEADOW;
        int base = light ? 2 : 1;
        if (!grassy(up) && up != 0) return Pal.GRASS[base + 1];                 // lit upper lip
        if (light && up == GRASSY) return Pal.GRASS[3];
        if (!grassy(down) && down != 0) return Pal.GRASS[base - 1];             // shaded front lip
        if (l == GRASSY && up == MEADOW && bladeTip(x, y)) return Pal.GRASS[2];  // meadow blades hang over
        // tuft marks on a jittered 4x3 grid: a lit tip over a mid stem, or a small chevron
        int cx = Math.floorDiv(x, 4), cy = Math.floorDiv(y, 3);
        double kind = Noise.rnd(cx, cy, 303);
        int bx = cx * 4 + (int) (Noise.rnd(cx, cy, 301) * 3), by = cy * 3 + (int) (Noise.rnd(cx, cy, 302) * 2);
        if (kind < .42) {
            if (x == bx && y == by) return Pal.GRASS[base + 2 - (kind < .3 ? 0 : 1)];
            if (x == bx && y == by + 1) return Pal.GRASS[base + 1];
        } else if (kind < .56) {
            if (y == by + 1 && (x == bx - 1 || x == bx + 1)) return Pal.GRASS[base + 1];
            if (y == by && x == bx) return Pal.GRASS[base + 1];
        } else if (kind > .9 && x == bx && y == by + 1) return Pal.GRASS[base - 1];
        return Pal.GRASS[base];
    }

    private int road(int x, int y, byte up) {
        if (grassy(up)) return bladeTip(x, y) ? (up == MEADOW ? Pal.GRASS[2] : Pal.GRASS[1]) : Pal.MUD[1];
        if (pebble(x, y)) return Noise.rnd(x, y, 15) < .35 ? Pal.STONE[4] : Pal.STONE[3];
        if (pebble(x, y - 1)) return Pal.MUD[0];
        if (Noise.rnd(x, y, 16) < .025) return Pal.STONE[2];
        if (speck(x, y)) return Pal.MUD[1];
        return Noise.rnd(x, y, 334) < .03 ? Pal.MUD[3] : Pal.MUD[2];
    }

    private int pebble(int x, int y, int size, int seed, int[] colours) {
        int[] cell = Pebbles.nearest(x, y, size, seed);
        if (cell[1] == 1) return Pal.MUD[1];
        int c = colours[(int) (Noise.rnd(cell[0], 1, seed) * colours.length)];
        int[] above = Pebbles.nearest(x, y - 1, size, seed);
        if (above[0] != cell[0] || above[1] == 1) return Pal.shift(c, 1);
        int[] below = Pebbles.nearest(x, y + 1, size, seed);
        if (below[0] != cell[0] || below[1] == 1) return Pal.shift(c, -1);
        return c;
    }

    private int cobble(int x, int y, int i) {
        int[] cell = Pebbles.nearest(x, y, 10, 41);
        int base = Noise.rnd(cell[0], 2, 41) < .65 ? 3 : 4;
        double[] centre = Pebbles.centre(cell[0], 10, 41);
        double dx = (x + .5 - centre[0]) / 5, dy = (y + .5 - centre[1]) / 4;
        double light = -(dx + dy) * .75;
        int idx = base + (light > .55 ? 1 : light < -.6 ? -1 : 0);
        if (light > 1.0 && Noise.rnd(cell[0], 4, 41) < .5) idx++;
        if (Noise.rnd(cell[0], 5, 41) < .08) return light > .4 ? Pal.GRASS[2] : Pal.GRASS[1];
        return Pal.STONE[Math.max(1, Math.min(6, idx))];
    }

    private int slab(int x, int y, int i) {
        int s = id[i];
        int[] box = Slabs.box(s);
        int base = Noise.rnd(s, 1, 51) < .58 ? 3 : 4;
        if (Noise.rnd(s, 2, 51) < .08) base = 2;
        int lx = x - box[0], ly = y - box[1], rw = box[2], rh = box[3];
        if (Slabs.cracked(s, lx, ly, rw, rh)) return Pal.STONE[1];
        if (Noise.rnd(s, 3, 51) < .07 && Noise.value(x, y, 3, 52) > .5) return Pal.GRASS[ly <= 1 ? 2 : 1];
        if (ly == 1 || lx == 1) return Pal.STONE[base + (Noise.rnd(s, 4, 51) < .3 ? 2 : 1)];
        if (ly == rh - 1 || lx == rw - 1) return Pal.STONE[base - 1];
        return Pal.STONE[base];
    }

    // ---------------------------------------------------------------- set pieces: worn ground, scorch, shadows
    /** People walked to every door: trampled dirt spreads in front of the larger set pieces. */
    private void aprons() {
        for (var p : SetPieces.pieces(0)) {
            var k = p.kind();
            boolean cloth = k == SetPieces.Kind.TENT || k == SetPieces.Kind.INFIRMARY;
            boolean building = cloth || k == SetPieces.Kind.BARRACKS || k == SetPieces.Kind.WATCHTOWER
                    || k == SetPieces.Kind.SIGNAL_TOWER || k == SetPieces.Kind.EAST_GATE || k == SetPieces.Kind.STATUE;
            if (!building) continue;
            double cx = p.x() / 2, cy = p.y() / 2 + 2;
            double rx = Math.max(k.footWidth, 120) / 4.0 + 10, ry = 16;
            for (int y = (int) (cy - ry - 4); y <= cy + ry + 4; y++) for (int x = (int) (cx - rx - 4); x <= cx + rx + 4; x++) {
                if (x < 0 || y < 0 || x >= W || y >= H) continue;
                int i = y * W + x;
                byte l = label[i];
                if (l != SOIL && l != GRASSY && l != MEADOW && l != MUDDY && l != PUDDLE && l != ROAD) continue;
                double d = Math.hypot((x - cx) / rx, (y - cy) / ry) + (Noise.value(x, y, 4, 961) - .5) * .35;
                if (d < 1) { label[i] = cloth ? CAMPDIRT : ROAD; lat[i] = Float.NaN; }
            }
        }
    }

    /** Tall pieces throw longer shadows; the camp fire leaves a scorched ring. */
    private void setPieces() {
        for (var p : SetPieces.pieces(0)) {
            var k = p.kind();
            if (k == SetPieces.Kind.CAMPFIRE) {
                int fx = (int) Math.round(p.x() / 2), fy = (int) Math.round(p.y() / 2) - 6;
                for (int y = fy - 9; y <= fy + 9; y++) for (int x = fx - 18; x <= fx + 18; x++) {
                    if (x < 0 || y < 0 || x >= W || y >= H) continue;
                    double d = Math.hypot((x - fx) / 17.0, (y - fy) / 8.0);
                    if (d < 1 && Noise.bayer(x, y) < 1.2 - d) out[y * W + x] = Pal.shift(out[y * W + x], d < .6 ? -2 : -1);
                }
            }
            if (!p.solid()) continue;
            var o = p.footprint();
            castShadow(o, Math.min(16, k.anchorY / 7));
        }
        var camp = area.landmarks().get(1);
        stamp(Decals.BEDROLL, (int) Math.round((camp.x() + 132) / 2), (int) Math.round((camp.y() - 80) / 2), false);
    }

    private void castShadow(RuinedOutpostMap.Obstacle o, int length) {
        int left = (int) Math.floor((o.centerX() - o.width() / 2) / 2), right = (int) Math.ceil((o.centerX() + o.width() / 2) / 2);
        int bottom = (int) Math.round((o.centerY() + o.height() / 2) / 2);
        for (int y = bottom - 1; y <= bottom + length; y++) for (int x = left + 2; x <= right + length; x++) {
            if (x < 0 || y < 0 || x >= W || y >= H || label[y * W + x] == CANOPY) continue;
            double fade = Math.max((y - bottom) / (double) (length + 1), Math.max(0, x - right) / (double) (length + 1));
            if (Noise.bayer(x, y) < 1 - fade) out[y * W + x] = Pal.shift(out[y * W + x], y - bottom < 2 ? -2 : -1);
        }
    }

    // ---------------------------------------------------------------- cast shadows of standing props
    /** Moonlight from the upper left: each obstacle darkens a band of ground below and right of its base. */
    private void shadows() {
        for (var o : area.obstacles()) castShadow(o, 4);
    }

    // ---------------------------------------------------------------- details: hand-pixelled decals
    /** Landmarks where the expedition fought: debris thickens toward them. */
    private static final int[] BATTLES = {0, 2, 3, 6, 8};
    private static final Decals.Decal[] DEBRIS = {Decals.SWORD, Decals.ARROW, Decals.ARROWS, Decals.SHIELD,
            Decals.HELMET, Decals.SKULL, Decals.BONES, Decals.RIBS, Decals.SPEAR, Decals.BANNER_SCRAP,
            Decals.ICHOR_STAIN, Decals.PLANK, Decals.ARROW, Decals.SHIELD, Decals.SKULL};

    private void scatter() {
        int cell = 11;
        for (int cy = 0; cy <= H / cell; cy++) for (int cx = 0; cx <= W / cell; cx++) {
            int x = cx * cell + (int) (Noise.rnd(cx, cy, 401) * cell), y = cy * cell + (int) (Noise.rnd(cx, cy, 402) * cell);
            if (x >= W || y >= H) continue;
            byte l = label[y * W + x];
            if (l == CANOPY || l == 0) continue;
            Decals.Decal d = choose(l, x, y, Noise.rnd(cx, cy, 403), Noise.rnd(cx, cy, 404));
            if (d == null) continue;
            boolean flip = Noise.rnd(cx, cy, 405) < .5;
            stamp(d, x, y, flip);
            // a fallen soldier's kit lies together: shield, blade, helm within a stride
            if (java.util.Arrays.asList(DEBRIS).contains(d) && Noise.rnd(cx, cy, 407) < .55) {
                Decals.Decal[] kit = {Decals.SWORD, Decals.HELMET, Decals.SHIELD, Decals.SKULL, Decals.ARROW};
                for (int k = 0; k < 2; k++) {
                    var extra = kit[(int) (Noise.rnd(cx + k, cy, 408) * kit.length)];
                    if (extra == d) continue;
                    int ex = x + (int) ((Noise.rnd(cx, cy + k, 409) - .5) * 26), ey = y + (int) ((Noise.rnd(cx + k, cy + k, 410) - .3) * 14);
                    if (ex > 0 && ey > 0 && ex < W && ey < H && label[ey * W + ex] != CANOPY && label[ey * W + ex] != PUDDLE) stamp(extra, ex, ey, !flip);
                }
            }
        }
    }

    private double battle(int x, int y) {
        double best = .06;
        for (int k : BATTLES) {
            var l = area.landmarks().get(k);
            best = Math.max(best, 1 - Math.hypot((x + .5) * 2 - l.x(), (y + .5) * 2 - l.y()) / 620);
        }
        return best;
    }

    private boolean near(int x, int y, int r, byte want) {
        for (int dy = -r; dy <= r; dy += 2) for (int dx = -r; dx <= r; dx += 2) if (at(x + dx, y + dy) == want) return true;
        return false;
    }

    private Decals.Decal choose(byte l, int x, int y, double roll, double pick) {
        if (l == PUDDLE || l == RUT || l == INLAY) return null;
        boolean walkable = l != MUDDY;
        if (walkable && roll < battle(x, y) * .05) return DEBRIS[(int) (pick * DEBRIS.length)];
        roll = Noise.rnd(x, y, 406);
        boolean water = near(x, y, 5, PUDDLE);
        switch (l) {
            case GRASSY, MEADOW -> {
                if (water && roll < .35) return pick < .5 ? Decals.REEDS : Decals.REEDS_SMALL;
                boolean rim = !grassy(at(x, y + 2)) || !grassy(at(x, y - 2)) || !grassy(at(x + 2, y)) || !grassy(at(x - 2, y));
                if (roll < (rim ? .45 : .13)) return pick < .3 ? Decals.TUFT_A : pick < .55 ? Decals.TUFT_B : pick < .8 ? Decals.TUFT_C : Decals.TUFT_D;
                if (roll < (rim ? .45 : .13) + (water ? .03 : .008)) return pick < .5 ? Decals.SHROOMS : Decals.SHROOM;
                if (roll > .975) return pick < .45 ? Decals.ROCK_S : pick < .75 ? Decals.ROCK_M : pick < .87 ? Decals.BOULDER : Decals.BRANCH;
            }
            case SOIL -> {
                if (roll < .06) return Decals.DRY_TUFT;
                if (roll < .11) return Decals.DRY_SPRIG;
                if (roll < .16) return pick < .7 ? Decals.ROCK_S : Decals.RUBBLE;
                if (roll < .175) return Decals.BRANCH;
            }
            case ROAD -> {
                float side = lat[y * W + x];
                if (!Float.isNaN(side) && Math.abs(side) < 28 && roll < .14) return pick < .55 ? Decals.DRY_TUFT : Decals.DRY_SPRIG;
                if (roll > .975) return pick < .6 ? Decals.CHIP : Decals.ROCK_S;
            }
            case MUDDY -> { if (roll < .1) return Decals.REEDS_SMALL; }
            case SLAB, MORTAR, BROKEN, COBBLE, GAP -> {
                if (edge[y * W + x] > -7 && roll < .12) return pick < .6 ? Decals.CHIP : Decals.RUBBLE;
                if ((l == BROKEN || l == GAP || l == MORTAR) && roll > .94) return Decals.TUFT_D;
                if (roll < .03) return Decals.CHIP;
            }
            case GRAVEL -> { if (roll < .03) return Decals.CHIP; if (roll > .98) return Decals.DRY_SPRIG; }
            case CAMPDIRT -> {
                if (roll < .04) return Decals.DRY_SPRIG;
                if (roll > .992) return pick < .5 ? Decals.PLANK : Decals.ROPE;
            }
            default -> { }
        }
        return null;
    }

    /** Anchored at the bottom centre; raised objects darken the ground right under them. */
    private void stamp(Decals.Decal d, int ax, int ay, boolean flip) {
        Canvas art = flip && d != Decals.SPEAR ? d.art().flipped() : d.art();
        int left = ax - art.w / 2, top = ay - art.h + 1;
        for (int y = 0; y < art.h; y++) for (int x = 0; x < art.w; x++) {
            int c = art.get(x, y), gx = left + x, gy = top + y;
            if ((c >>> 24) == 0 || gx < 0 || gy < 0 || gx >= W || gy >= H || label[gy * W + gx] == CANOPY) continue;
            out[gy * W + gx] = c;
        }
        if (!d.raised()) return;
        for (int x = 0; x < art.w; x++) {
            int bottom = -1;
            for (int y = art.h - 1; y >= 0; y--) if (art.opaque(x, y)) { bottom = y; break; }
            int gx = left + x, gy = top + bottom + 1;
            if (bottom < 0 || gx < 0 || gx >= W || gy >= H || art.opaque(x, bottom + 1)) continue;
            out[gy * W + gx] = Pal.shift(out[gy * W + gx], -1);
        }
    }

    public static void main(String[] args) {
        long t = System.nanoTime();
        var bake = new GroundBake();
        var canvas = bake.bake();
        canvas.save(args.length > 0 ? args[0] : "outpost_ground.png");
        System.out.printf("ground %dx%d in %.1fs%n", canvas.w, canvas.h, (System.nanoTime() - t) / 1e9);
    }
}
