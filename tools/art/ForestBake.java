import java.util.HashMap;
import java.util.Map;

/**
 * Paints the Corrupted Forest floor at native resolution (one pixel = two world pixels) from 16x16
 * Wang tiles of Tiles16.Set.FOREST. Every tile corner takes a material from the live layout (trails,
 * the dry channel and its pools, clearings, moss, the mire, Briarheart's blight); every tile is then
 * the sheet tile its four corners name. Decals, tree shade, light pools and the canopy on the boundary
 * banks follow. Re-run whenever CampaignWorld or SetPieces change (tools/art/build-art.sh forest).
 */
final class ForestBake {
    static final int T = Tiles16.T;
    final CampaignWorld.Area area = CampaignWorld.area(1);
    final int W = area.width() / 2, H = area.height() / 2, CW = W / T, CH = H / T;
    final Tiles16.Mat[][] corner = new Tiles16.Mat[CH + 1][CW + 1];
    final Tiles16.Mat[] ground;
    final boolean[] canopy;
    final int[] out;

    ForestBake() {
        ground = new Tiles16.Mat[W * H];
        canopy = new boolean[W * H];
        out = new int[W * H];
        for (var bank : RuinedOutpostMap.campaign(1).banks()) {
            int x0 = (int) Math.floor((bank.centerX() - bank.width() / 2) / 2), x1 = (int) Math.ceil((bank.centerX() + bank.width() / 2) / 2);
            int y0 = (int) Math.floor((bank.centerY() - bank.height() / 2) / 2), y1 = (int) Math.ceil((bank.centerY() + bank.height() / 2) / 2);
            for (int y = Math.max(0, y0); y < Math.min(H, y1); y++) for (int x = Math.max(0, x0); x < Math.min(W, x1); x++) canopy[y * W + x] = true;
        }
    }

    Canvas bake() {
        for (int j = 0; j <= CH; j++) for (int i = 0; i <= CW; i++) corner[j][i] = material(i * T * 2.0, j * T * 2.0);
        resolve();
        tiles();
        decals();
        fordStones();
        shade();
        new Canopy(W, H, canopy, out).paint();
        Canvas c = new Canvas(W, H);
        System.arraycopy(out, 0, c.px, 0, out.length);
        return c;
    }

    // ---------------------------------------------------------------- what each tile corner is
    Tiles16.Mat material(double wx, double wy) {
        var lm = area.landmarks();
        var boss = area.boss();
        double toBoss = Math.hypot(wx - boss.x(), wy - boss.y());
        // Briarheart's blight: the arena, the stair, and thin tendrils creeping out through the trees
        if (toBoss < 560 + (Noise.value(wx, wy, 120, 811) - .5) * 220) return Tiles16.Mat.BLIGHT;
        if (Math.hypot(wx - area.exit().x(), wy - area.exit().y()) < 220) return Tiles16.Mat.BLIGHT;
        if (toBoss < 1300 && Math.abs(Noise.value(wx, wy, 150, 812) - .5) < .045 * (1.3 - toBoss / 1300)) return Tiles16.Mat.BLIGHT;
        double channel = CampaignWorld.channelDistance(wx, wy) + (Noise.value(wx, wy, 60, 813) - .5) * 40;
        boolean trail = CampaignWorld.nearRoutes(wx, wy, lm, 46 + (Noise.value(wx, wy, 70, 814) - .5) * 22);
        if (channel < CampaignWorld.CHANNEL_HALF_WIDTH) {
            // the old watercourse is dry; black water lies only in its deeper bends, never on a crossing
            boolean pool = Noise.value(wx, wy, 300, 815) > .6 && channel < CampaignWorld.CHANNEL_HALF_WIDTH - 22
                    && !CampaignWorld.nearRoutes(wx, wy, lm, 150);
            return pool ? Tiles16.Mat.BLACKWATER : Tiles16.Mat.RIVERBED;
        }
        // the mire below the Thorn Approach, where the shamans keep their pools
        double mire = Math.hypot((wx - 4620) / 430, (wy - 3420) / 300) + (Noise.value(wx, wy, 90, 816) - .5) * .5;
        if (!trail && mire < .55 && Noise.value(wx, wy, 70, 817) > .42) return Tiles16.Mat.BLACKWATER;
        if (!trail && mire < 1) return Tiles16.Mat.MUD;
        if (trail) return Tiles16.Mat.DIRT;
        for (int i = 0; i < lm.size(); i++) {
            var l = lm.get(i);
            double clearing = switch (i) { case 2 -> 470; case 1 -> 330; case 7 -> 0; default -> l.optional() ? 230 : 280; };
            double d = Math.hypot(wx - l.x(), wy - l.y()) + (Noise.value(wx, wy, 50, 818 + i) - .5) * 90;
            if (d < clearing) return i == 4 || i == 10 ? Tiles16.Mat.MOSS : Tiles16.Mat.GRASS;
        }
        if (Noise.fbm(wx, wy, 260, 3, 819) > .64) return Tiles16.Mat.MOSS;
        return Tiles16.Mat.LITTER;
    }

    /** Importance when a tile's corners name a combination the sheet has no tiles for. */
    private static int importance(Tiles16.Mat m) {
        return switch (m) {
            case DIRT -> 7; case RIVERBED -> 6; case BLACKWATER -> 5; case BLIGHT -> 4; case GRASS -> 3; case MUD -> 2; case MOSS -> 1;
            default -> 0;
        };
    }

    /** Until every tile is a fill or a pair the forest set draws, the least important odd corner turns to litter. */
    void resolve() {
        for (int pass = 0; pass < 40; pass++) {
            boolean changed = false;
            for (int j = 0; j < CH; j++) for (int i = 0; i < CW; i++) {
                var mats = new java.util.LinkedHashSet<Tiles16.Mat>(java.util.List.of(corner[j][i], corner[j][i + 1], corner[j + 1][i], corner[j + 1][i + 1]));
                if (mats.size() == 1) continue;
                var arr = mats.toArray(new Tiles16.Mat[0]);
                if (arr.length == 2 && Tiles16.Set.FOREST.pair(arr[0], arr[1]) != null) continue;
                Tiles16.Mat drop = null;
                for (var m : mats) if (m != Tiles16.Mat.LITTER && (drop == null || importance(m) < importance(drop))) drop = m;
                if (drop == null) continue;
                for (int[] c : new int[][]{{j, i}, {j, i + 1}, {j + 1, i}, {j + 1, i + 1}})
                    if (corner[c[0]][c[1]] == drop) { corner[c[0]][c[1]] = Tiles16.Mat.LITTER; changed = true; }
            }
            if (!changed) return;
        }
        throw new IllegalStateException("forest corners did not settle");
    }

    // ---------------------------------------------------------------- tiles
    void tiles() {
        Map<Long, Canvas> cache = new HashMap<>();
        for (int j = 0; j < CH; j++) for (int i = 0; i < CW; i++) {
            Tiles16.Mat tl = corner[j][i], tr = corner[j][i + 1], bl = corner[j + 1][i], br = corner[j + 1][i + 1];
            Tiles16.Mat a = tl, b = tl;
            for (var m : new Tiles16.Mat[]{tr, bl, br}) if (m != a) b = m;
            int mask = 0, variant;
            if (a != b) {
                var pair = Tiles16.Set.FOREST.pair(a, b);
                a = pair[0]; b = pair[1];
                mask = (tl == b ? 1 : 0) | (tr == b ? 2 : 0) | (bl == b ? 4 : 0) | (br == b ? 8 : 0);
                variant = (int) (Noise.rnd(i, j, 821) * 3);
            } else {
                double r = Noise.rnd(i, j, 822);
                variant = r < .84 ? (int) (Noise.rnd(i, j, 823) * 4) : 4 + (int) (Noise.rnd(i, j, 824) * 4);
            }
            long key = ((long) a.ordinal() << 24) | ((long) b.ordinal() << 16) | (mask << 8) | variant;
            final Tiles16.Mat fa = a, fb = b;
            final int fm = mask, fv = variant;
            Canvas tile = cache.computeIfAbsent(key, k -> Tiles16.tile(fa, fb, fm, fv));
            for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
                int px = i * T + x, py = j * T + y;
                out[py * W + px] = tile.get(x, y);
                ground[py * W + px] = Tiles16.at(a, b, mask, x, y, variant);
            }
        }
    }

    Tiles16.Mat at(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H ? null : ground[y * W + x]; }

    // ---------------------------------------------------------------- decals
    void decals() {
        int cell = 10;
        for (int cy = 0; cy <= H / cell; cy++) for (int cx = 0; cx <= W / cell; cx++) {
            int x = cx * cell + (int) (Noise.rnd(cx, cy, 831) * cell), y = cy * cell + (int) (Noise.rnd(cx, cy, 832) * cell);
            if (x >= W || y >= H || canopy[y * W + x]) continue;
            var m = ground[y * W + x];
            double roll = Noise.rnd(cx, cy, 833), pick = Noise.rnd(cx, cy, 834);
            Decals.Decal d = switch (m) {
                case LITTER -> roll < .045 ? FERN : roll < .048 ? Decals.SHROOMS : roll < .062 ? Decals.BRANCH : roll < .08 ? LEAVES
                        : roll < .088 ? (pick < .5 ? Decals.ROCK_S : Decals.ROCK_M) : null;
                case MOSS -> roll < .06 ? (pick < .5 ? BLOSSOM_WHITE : FERN) : roll < .068 ? Decals.SHROOMS : null;
                case GRASS -> roll < .08 ? (pick < .35 ? Decals.TUFT_A : pick < .6 ? Decals.TUFT_B : pick < .8 ? BLOSSOM_WHITE : BLOSSOM_GOLD)
                        : roll < .1 ? BLOSSOM_VIOLET : null;
                case RIVERBED -> roll < .03 ? Decals.ROCK_S : roll < .036 ? Decals.BRANCH : roll < .04 ? Decals.BONES : null;
                case DIRT -> roll < .012 ? Decals.ROCK_S : null;
                case BLIGHT -> roll < .012 ? (pick < .45 ? Decals.BONES : pick < .62 ? Decals.SKULL : CRYSTAL) : null;
                case MUD -> roll < .04 ? Decals.REEDS_SMALL : null;
                default -> null;
            };
            if (d != null) stamp(d, x, y, Noise.rnd(cx, cy, 835) < .5);
            // reeds stand where the black water meets the bank
            if (m != Tiles16.Mat.BLACKWATER && (at(x, y + 2) == Tiles16.Mat.BLACKWATER || at(x, y - 2) == Tiles16.Mat.BLACKWATER) && roll < .55)
                stamp(roll < .25 ? Decals.REEDS : Decals.REEDS_SMALL, x, y, pick < .5);
        }
        goldRoots();
    }

    void stamp(Decals.Decal d, int x, int y, boolean flip) {
        Canvas a = d.art();
        int left = x - a.w / 2, top = y - a.h + 1;
        for (int j = 0; j < a.h; j++) for (int i = 0; i < a.w; i++) {
            int c = a.get(flip ? a.w - 1 - i : i, j);
            int px = left + i, py = top + j;
            if ((c >>> 24) == 0 || px < 0 || py < 0 || px >= W || py >= H || canopy[py * W + px]) continue;
            out[py * W + px] = c;
        }
    }

    /** Glowing roots crawl out from Briarheart's arena: the gold light between the roots. */
    void goldRoots() {
        var boss = area.boss();
        int bx = (int) Math.round(boss.x() / 2), by = (int) Math.round(boss.y() / 2);
        for (int r = 0; r < 12; r++) {
            double a = (r + Noise.rnd(r, 0, 841) * .6) * Math.PI * 2 / 12, len = 130 + Noise.rnd(r, 1, 841) * 170;
            double x = bx + Math.cos(a) * 46, y = by + Math.sin(a) * 30, turn = (Noise.rnd(r, 2, 841) - .5) * .02;
            for (int s = 0; s < len; s++) {
                a += turn + (Noise.value(s, r * 9, 14, 842) - .5) * .06;
                x += Math.cos(a); y += Math.sin(a) * .72;
                int ix = (int) Math.round(x), iy = (int) Math.round(y);
                if (ix < 2 || iy < 2 || ix >= W - 2 || iy >= H - 2 || canopy[iy * W + ix]) break;
                var m = ground[iy * W + ix];
                if (m == Tiles16.Mat.RIVERBED || m == Tiles16.Mat.BLACKWATER) break;
                double fade = s / len;
                int width = fade < .45 ? 2 : 1;
                // a lit back, a glowing gold core, a violet shadow side: a root, not a scribble
                out[(iy - 1) * W + ix] = fade < .8 ? Pal.VIOLET[3] : Pal.VIOLET[2];
                for (int w = 0; w < width; w++) out[(iy + w) * W + ix] = fade < .7 ? (Noise.rnd(ix, iy + w, 843) < .35 ? Pal.GOLD[4] : Pal.GOLD[3]) : Pal.GOLD[1];
                out[(iy + width) * W + ix] = Pal.VIOLET[0];
            }
        }
    }

    /** Flat stepping stones carry each trail across the dry channel. */
    void fordStones() {
        var lm = area.landmarks();
        for (int[] r : CampaignWorld.routes()) {
            var a = lm.get(r[0]); var b = lm.get(r[1]);
            double len = Math.hypot(b.x() - a.x(), b.y() - a.y());
            for (double t = 0; t < len; t += 26) {
                double wx = a.x() + (b.x() - a.x()) * t / len, wy = a.y() + (b.y() - a.y()) * t / len;
                if (CampaignWorld.channelDistance(wx, wy) > CampaignWorld.CHANNEL_HALF_WIDTH - 12) continue;
                int cx = (int) Math.round(wx / 2 + (Noise.rnd((int) t, r[0], 851) - .5) * 10), cy = (int) Math.round(wy / 2 + (Noise.rnd((int) t, r[1], 852) - .5) * 6);
                for (int y = cy - 4; y <= cy + 4; y++) for (int x = cx - 7; x <= cx + 7; x++) {
                    double d = Math.hypot((x + .5 - cx) / 6.5, (y + .5 - cy) / 3.6);
                    if (d > 1 || x < 0 || y < 0 || x >= W || y >= H) continue;
                    double lit = -(x - cx) / 6.5 * .6 - (y - cy) / 3.6 * .8;
                    out[y * W + x] = d > .85 && y > cy ? Pal.STONE[1] : lit > .4 ? Pal.STONE[5] : lit > -.2 ? Pal.STONE[4] : Pal.STONE[3];
                }
                for (int x = cx - 5; x <= cx + 5; x++) if (cy + 5 < H && x >= 0 && x < W) out[(cy + 5) * W + x] = Pal.CANVAS[0];
            }
        }
    }

    // ---------------------------------------------------------------- shade and light
    /** Crowns shade the ground below them, offset away from the moon; solid pieces cast short shadows. */
    void shade() {
        for (var p : SetPieces.pieces(1)) {
            var k = p.kind();
            if (k.tree()) {
                double cx = p.x() / 2 + k.width * .1, cy = p.y() / 2 - 2, rx = k.width * .46, ry = k.width * .2;
                for (int y = (int) (cy - ry - 1); y <= cy + ry + 1; y++) for (int x = (int) (cx - rx - 1); x <= cx + rx + 1; x++) {
                    if (x < 0 || y < 0 || x >= W || y >= H || canopy[y * W + x]) continue;
                    double d = Math.hypot((x + .5 - cx) / rx, (y + .5 - cy) / ry);
                    if (d >= 1) continue;
                    if (Noise.bayer(x, y) < 1.15 - d) out[y * W + x] = Pal.shift(out[y * W + x], d < .55 ? -2 : -1);
                }
            } else if (p.solid()) {
                var o = p.footprint();
                int left = (int) Math.floor((o.centerX() - o.width() / 2) / 2), right = (int) Math.ceil((o.centerX() + o.width() / 2) / 2);
                int bottom = (int) Math.round((o.centerY() + o.height() / 2) / 2), length = Math.min(12, k.anchorY / 8);
                for (int y = bottom - 1; y <= bottom + length; y++) for (int x = left + 2; x <= right + length; x++) {
                    if (x < 0 || y < 0 || x >= W || y >= H || canopy[y * W + x]) continue;
                    double fade = Math.max((y - bottom) / (double) (length + 1), Math.max(0, x - right) / (double) (length + 1));
                    if (Noise.bayer(x, y) < 1 - fade) out[y * W + x] = Pal.shift(out[y * W + x], -1);
                }
            }
        }
    }

    // ---------------------------------------------------------------- forest decals
    /** Pixel art from rows of characters, in the forest's own little legend. */
    static Canvas art(String... rows) {
        java.util.Map<Character, Integer> ink = java.util.Map.ofEntries(
                java.util.Map.entry('m', Pal.GRASS[3]), java.util.Map.entry('H', Pal.GRASS[4]), java.util.Map.entry('s', Pal.GRASS[1]),
                java.util.Map.entry('r', Pal.RUST[2]), java.util.Map.entry('R', Pal.RUST[3]), java.util.Map.entry('q', Pal.DRY[2]),
                java.util.Map.entry('Q', Pal.DRY[3]), java.util.Map.entry('w', Pal.BONE[3]), java.util.Map.entry('y', Pal.GOLD[4]),
                java.util.Map.entry('Y', Pal.GOLD[5]), java.util.Map.entry('v', Pal.VIOLET[4]), java.util.Map.entry('V', Pal.VIOLET[5]),
                java.util.Map.entry('d', Pal.VIOLET[1]));
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        Canvas c = new Canvas(w, rows.length);
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            char ch = rows[y].charAt(x);
            if (ch != '.') c.set(x, y, ink.get(ch));
        }
        return c;
    }
    static final Decals.Decal FERN = new Decals.Decal("fern", art(
            "...m.......",
            ".m.mm..m...",
            "..mHm.mm.m.",
            "m.mmmHmm.m.",
            ".mmHmmmHm..",
            "..mmsHmm...",
            "....ss....."), false);
    static final Decals.Decal LEAVES = new Decals.Decal("leaves", art(
            ".r..q.",
            "rRr.qQ",
            ".r..q.",
            "..rR.."), false);
    static final Decals.Decal BLOSSOM_WHITE = new Decals.Decal("blossom_white", art(
            ".w...w",
            "wyw.wyw",
            ".w.s.w.",
            "...s..."), false);
    static final Decals.Decal BLOSSOM_GOLD = new Decals.Decal("blossom_gold", art(
            ".y..",
            "yYy.",
            ".y.y",
            ".s.s"), false);
    static final Decals.Decal BLOSSOM_VIOLET = new Decals.Decal("blossom_violet", art(
            ".v.v.",
            "vVvVv",
            ".s.s."), false);
    static final Decals.Decal CRYSTAL = new Decals.Decal("crystal", art(
            "..V.",
            ".vVv",
            ".vV.",
            "dvvd"), false);
}
