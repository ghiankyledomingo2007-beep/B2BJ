import java.util.ArrayList;
import java.util.List;

/**
 * Every area's set pieces: what stands where, and which part of it is solid. One table per area feeds
 * the renderer, the collision map and the art baker (tools/art), so art, footprints and baked ground
 * shadows cannot drift apart. Outpost offsets keep each piece in the free sector between a landmark's
 * roads; the forest's trees are scattered deterministically clear of trails, clearings and spawns.
 */
public final class SetPieces {
    private SetPieces() { }

    /** Sprite metrics in native pixels (the game draws them at 2x); footprints in world pixels. */
    public enum Kind {
        TENT(128, 64, 96, 184, 52), CAMPFIRE(44, 22, 25, 60, 22), LAMP(40, 12, 97, 14, 12), LOG_SEAT(44, 22, 14, 0, 0),
        CART(120, 60, 72, 200, 50), SIGNPOST(48, 22, 81, 14, 12), BANNER(48, 12, 145, 14, 12), RACK(76, 38, 57, 132, 24),
        DUMMY(36, 18, 69, 26, 16), GATE_POST(40, 20, 147, 26, 18), BARRACKS(264, 132, 192, 480, 100),
        WATCHTOWER(100, 50, 256, 120, 56), STANDARD(148, 56, 87, 96, 34), STATUE(80, 40, 146, 104, 40),
        BRAZIER(36, 18, 53, 34, 18), COLUMN(40, 20, 145, 52, 22), EAST_GATE(272, 136, 240, 0, 0),
        SIGNAL_TOWER(104, 52, 265, 144, 60), INFIRMARY(200, 100, 123, 330, 60), SUPPLIES(56, 28, 68, 64, 32),
        // Corrupted Forest
        OAK("forest", 116, 58, 158, 44, 22), PINE("forest", 76, 38, 170, 34, 20), WITHERED("forest", 92, 46, 140, 34, 18),
        BUSH("forest", 52, 26, 32, 0, 0), LOG("forest", 104, 52, 26, 176, 28), BOULDER("forest", 56, 28, 36, 88, 30),
        STUMP("forest", 34, 17, 24, 44, 20), BOAT("forest", 172, 86, 54, 296, 58), CLOTH_POST("forest", 32, 10, 72, 14, 12),
        HOLLOW_TREE("forest", 240, 120, 270, 300, 72), MOONWELL("forest", 132, 66, 62, 216, 66),
        ORCHARD_TREE("forest", 84, 42, 104, 36, 18), BRAMBLE("forest", 80, 40, 36, 128, 30),
        ROOT_WALL("forest", 148, 74, 50, 248, 40), BRIAR_ROOT("forest", 148, 74, 54, 232, 40),
        STAIR("forest", 212, 106, 124, 0, 0), RIFT_STONE("forest", 46, 23, 88, 54, 22), LEAN_TO("forest", 132, 66, 86, 210, 52);
        public final int width, anchorX, anchorY, footWidth, footDepth;
        private final String folder;
        Kind(int width, int anchorX, int anchorY, int footWidth, int footDepth) {
            this("outpost", width, anchorX, anchorY, footWidth, footDepth);
        }
        Kind(String folder, int width, int anchorX, int anchorY, int footWidth, int footDepth) {
            this.folder = folder; this.width = width; this.anchorX = anchorX; this.anchorY = anchorY;
            this.footWidth = footWidth; this.footDepth = footDepth;
        }
        public String sheet() { return "assets/props/" + folder + "/set_" + name().toLowerCase(java.util.Locale.ROOT) + ".png"; }
        /** Trees and other tall growth the player can stand behind. */
        public boolean tree() { return this == OAK || this == PINE || this == WITHERED || this == ORCHARD_TREE || this == HOLLOW_TREE; }
    }

    /** One placed piece; (x, y) is the front-centre of its footprint in world pixels. */
    public record Piece(Kind kind, double x, double y, int variant) {
        public boolean solid() { return kind.footWidth > 0; }
        public RuinedOutpostMap.Obstacle footprint() {
            return new RuinedOutpostMap.Obstacle(x, y - kind.footDepth / 2.0, kind.footWidth, kind.footDepth);
        }
    }

    /** Source on the sprite and its projection onto the floor, in world pixels. */
    public record Light(double x, double y, double groundY, double radius, int colour, boolean fire) { }

    private static final List<Piece> PIECES = build();
    private static final List<Piece> FOREST = Forest.build();

    public static List<Piece> pieces(int area) {
        return switch (area) { case 0 -> PIECES; case 1 -> FOREST; default -> List.of(); };
    }

    /** The solid parts, as plain obstacles for the campaign collision map. */
    public static List<RuinedOutpostMap.Obstacle> footprints(int area) {
        var list = new ArrayList<RuinedOutpostMap.Obstacle>();
        for (var p : pieces(area)) if (p.solid()) list.add(p.footprint());
        if (area == 0) {
            // The East Gate's towers flank its doorway; the doorway itself is the map's gate barrier.
            var gate = PIECES.stream().filter(p -> p.kind() == Kind.EAST_GATE).findFirst().orElseThrow();
            for (int side : new int[]{-1, 1})
                list.add(new RuinedOutpostMap.Obstacle(gate.x() + side * 156, gate.y() - 22, 200, 44));
        }
        if (area == 1) {
            // The stair's mouth is the exit; its stone cheeks either side are solid.
            var stair = FOREST.stream().filter(p -> p.kind() == Kind.STAIR).findFirst().orElseThrow();
            for (int side : new int[]{-1, 1})
                list.add(new RuinedOutpostMap.Obstacle(stair.x() + side * 150, stair.y() - 30, 90, 60));
        }
        return List.copyOf(list);
    }

    /** Index of the rest brazier this piece is (FieldObjects.rests), or -1. */
    public static int restBrazier(Piece p) {
        if (p.kind() != Kind.BRAZIER) return -1;
        var rests = FieldObjects.rests(0);
        for (int i = 0; i < rests.size(); i++) if (rests.get(i).x() == p.x() && rests.get(i).y() == p.y()) return i;
        return -1;
    }

    /** The four braziers round the Warden's floor: cold until it wakes. */
    public static boolean arenaBrazier(Piece p) {
        var gatehouse = CampaignWorld.area(0).landmarks().get(8);
        return p.kind() == Kind.BRAZIER && Math.hypot(p.x() - gatehouse.x(), p.y() - gatehouse.y()) < 420;
    }

    private static final List<Light> LIGHTS = buildLights(PIECES), FOREST_LIGHTS = buildLights(FOREST);

    /** Flames and glows for lighting; fire=true pieces also carry an animated fire sprite. */
    public static List<Light> lights(int area) {
        return switch (area) { case 0 -> LIGHTS; case 1 -> FOREST_LIGHTS; default -> List.of(); };
    }

    private static List<Light> buildLights(List<Piece> pieces) {
        var list = new ArrayList<Light>();
        for (var p : pieces) switch (p.kind()) {
            case CAMPFIRE -> list.add(new Light(p.x(), p.y() - 50, p.y(), 300, 0xffa040, true));
            case BRAZIER -> list.add(new Light(p.x(), p.y() - 84, p.y(), 220, 0xff9a3c, true));
            case WATCHTOWER -> list.add(new Light(p.x() + 4, p.y() - 400, p.y(), 260, 0xff9a3c, true));
            case LAMP -> list.add(new Light(p.x() + 36, p.y() - 126, p.y(), 170, 0xffc45e, false));
            case STANDARD -> list.add(new Light(p.x() + 104, p.y() - 10, p.y(), 190, 0xeeb840, false));
            case MOONWELL -> list.add(new Light(p.x(), p.y() - 40, p.y(), 260, 0x9fe8ff, false));
            case RIFT_STONE -> list.add(new Light(p.x(), p.y() - 120, p.y(), 150, 0xab68cf, false));
            case BRIAR_ROOT -> list.add(new Light(p.x(), p.y() - 30, p.y(), 170, 0xeeb840, false));
            case STAIR -> list.add(new Light(p.x(), p.y() - 90, p.y(), 240, 0xffdd80, false));
            default -> { }
        }
        return List.copyOf(list);
    }

    /**
     * A digest of everything the baked floor is painted from: map size, landmarks, roads, obstacles,
     * boundary banks and these set pieces. tools/art writes it next to the floor image; a mismatch
     * means the layout changed and the floor must be rebaked (tools/art/build-art.sh ground).
     */
    public static String layoutFingerprint(int areaId) {
        var area = CampaignWorld.area(areaId);
        var text = new StringBuilder().append(area.width()).append('x').append(area.height());
        for (var l : area.landmarks()) text.append('|').append(l.x()).append(',').append(l.y()).append(',').append(l.optional());
        for (int[] r : CampaignWorld.routes()) text.append("|r").append(r[0]).append('-').append(r[1]);
        for (var o : area.obstacles()) text.append("|o").append(o.centerX()).append(',').append(o.centerY()).append(',').append(o.width()).append(',').append(o.height()).append(o.prop());
        for (var b : RuinedOutpostMap.campaign(areaId).banks()) text.append("|b").append(b.centerX()).append(',').append(b.centerY()).append(',').append(b.width()).append(',').append(b.height());
        if (areaId == 1) for (var c : CampaignWorld.forestChannel()) text.append("|c").append(c.x()).append(',').append(c.y());
        for (var p : pieces(areaId)) {
            var k = p.kind();   // the baker sizes aprons and shadows from these metrics too
            text.append("|p").append(k).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.variant())
                    .append(',').append(k.width).append(',').append(k.anchorX).append(',').append(k.anchorY)
                    .append(',').append(k.footWidth).append(',').append(k.footDepth);
        }
        text.append("|boss").append(area.boss()).append("|exit").append(area.exit()).append("|hub").append(area.hub());
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var hex = new StringBuilder();
            for (int i = 0; i < 12; i++) hex.append(String.format("%02x", digest[i]));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static List<Piece> build() {
        var lm = CampaignWorld.area(0).landmarks();
        var list = new ArrayList<Piece>();
        // { landmark, kind ordinal, dx, dy, variant }
        Object[][] layout = {
            {0, Kind.CART, -120, -200, 0}, {0, Kind.SIGNPOST, 251, 47, 0},
            {1, Kind.TENT, 240, -220, 0}, {1, Kind.CAMPFIRE, 60, -70, 0}, {1, Kind.LAMP, -130, -110, 0},
            {1, Kind.LOG_SEAT, 0, -34, 0}, {1, Kind.LOG_SEAT, 122, -40, 1}, {1, Kind.SUPPLIES, 330, -120, 2},
            {2, Kind.BANNER, -150, 158, 0}, {2, Kind.BANNER, 0, 214, 0}, {2, Kind.BANNER, 150, 158, 0},
            {2, Kind.RACK, -240, -170, 0}, {2, Kind.DUMMY, 236, 196, 0}, {2, Kind.DUMMY, -64, -236, 1},
            {3, Kind.GATE_POST, 43, -241, 0}, {3, Kind.GATE_POST, -211, -123, 1},
            {4, Kind.BARRACKS, -260, -330, 0}, {4, Kind.SUPPLIES, -330, 40, 1}, {4, Kind.SUPPLIES, -280, 150, 0},
            {5, Kind.WATCHTOWER, 200, -210, 0}, {5, Kind.SUPPLIES, 260, 64, 3},
            {6, Kind.STANDARD, 0, 232, 0}, {6, Kind.BANNER, 190, -190, 0},
            {7, Kind.STATUE, 262, -10, 0}, {7, Kind.BRAZIER, -150, -196, 0}, {7, Kind.BRAZIER, 210, 152, 0},
            {8, Kind.BRAZIER, -225, -130, 0}, {8, Kind.BRAZIER, -225, 130, 0}, {8, Kind.BRAZIER, -23, -259, 0},
            {8, Kind.BRAZIER, 213, 149, 0}, {8, Kind.COLUMN, -330, 0, 0}, {8, Kind.COLUMN, -165, -286, 1},
            {8, Kind.COLUMN, 165, 286, 2}, {8, Kind.COLUMN, 165, -286, 1},
            {9, Kind.SIGNAL_TOWER, 0, -220, 0},
            {10, Kind.INFIRMARY, 40, -190, 0},
            {11, Kind.RACK, -230, 10, 1}, {11, Kind.RACK, 200, -80, 0}, {11, Kind.SUPPLIES, -126, 124, 1},
            {11, Kind.SUPPLIES, 220, 40, 2}, {11, Kind.CART, 0, 220, 1},
        };
        for (Object[] row : layout) {
            var l = lm.get((Integer) row[0]);
            list.add(new Piece((Kind) row[1], l.x() + (Integer) row[2], l.y() + (Integer) row[3], (Integer) row[4]));
        }
        var exit = CampaignWorld.area(0).exit();
        list.add(new Piece(Kind.EAST_GATE, exit.x(), exit.y(), 0));
        // Rest braziers (FieldObjects): cold until the Blade strikes them alight.
        for (var rest : FieldObjects.rests(0)) list.add(new Piece(Kind.BRAZIER, rest.x(), rest.y(), 0));
        return List.copyOf(list);
    }

    /**
     * The Corrupted Forest: each landmark's pieces in the free sectors between its trails, white
     * cloth along every trail (Sedge's markings), then trees and undergrowth scattered on a jittered
     * grid wherever they keep clear of trails, clearings, the dry channel, spawns and other pieces.
     */
    private static final class Forest {
        private Forest() { }
        static List<Piece> build() {
            var area = CampaignWorld.area(1);
            var lm = area.landmarks();
            var list = new ArrayList<Piece>();
            // { landmark, kind, dx, dy, variant }
            Object[][] layout = {
                {0, Kind.LOG, -40, -240, 0}, {0, Kind.CLOTH_POST, 120, 90, 0},
                {1, Kind.BOAT, 200, -190, 0}, {1, Kind.CAMPFIRE, 20, 110, 0}, {1, Kind.LOG_SEAT, -70, 150, 0},
                {1, Kind.LOG_SEAT, 110, 160, 1}, {1, Kind.SUPPLIES, -220, -90, 1},
                {2, Kind.RIFT_STONE, 244, 89, 0}, {2, Kind.RIFT_STONE, 89, 244, 1}, {2, Kind.RIFT_STONE, -130, 225, 0},
                {2, Kind.RIFT_STONE, -256, 45, 1},
                {3, Kind.ROOT_WALL, 150, -280, 0}, {3, Kind.ROOT_WALL, -300, 20, 1},
                {4, Kind.HOLLOW_TREE, -103, -282, 0}, {4, Kind.BOULDER, -217, 125, 0},
                {5, Kind.CLOTH_POST, 90, -156, 0}, {5, Kind.CLOTH_POST, -90, -156, 1}, {5, Kind.BOULDER, 244, 89, 1},
                {5, Kind.BOULDER, -216, -125, 0},
                {6, Kind.STUMP, 192, -161, 0}, {6, Kind.LOG, 70, 261, 1}, {6, Kind.CLOTH_POST, -153, -129, 0},
                {7, Kind.BRAMBLE, 256, 45, 0}, {7, Kind.BRAMBLE, 130, 225, 1}, {7, Kind.BRAMBLE, -217, 125, 2},
                {7, Kind.BRAMBLE, -106, -227, 0},
                {8, Kind.BRIAR_ROOT, -395, 144, 0}, {8, Kind.BRIAR_ROOT, -322, -270, 1}, {8, Kind.BRIAR_ROOT, 0, -420, 0},
                {8, Kind.BRIAR_ROOT, 297, 297, 1},
                {9, Kind.LEAN_TO, 0, -200, 0}, {9, Kind.LAMP, -150, 0, 0},
                {10, Kind.MOONWELL, 0, -150, 0},
                {11, Kind.ORCHARD_TREE, -260, -120, 0}, {11, Kind.ORCHARD_TREE, -380, 40, 1}, {11, Kind.ORCHARD_TREE, -250, 180, 0},
                {11, Kind.ORCHARD_TREE, 250, -80, 1}, {11, Kind.ORCHARD_TREE, 370, 90, 0}, {11, Kind.ORCHARD_TREE, 250, 250, 1},
            };
            for (Object[] row : layout) {
                var l = lm.get((Integer) row[0]);
                list.add(new Piece((Kind) row[1], l.x() + (Integer) row[2], l.y() + (Integer) row[3], (Integer) row[4]));
            }
            var exit = area.exit();
            list.add(new Piece(Kind.STAIR, exit.x(), exit.y() - 30, 0));
            clothAlongTrails(lm, list);
            var rng = new java.util.Random(0x5EEDF0L);
            scatter(area, list, rng);
            return List.copyOf(list);
        }

        private static void clothAlongTrails(List<CampaignWorld.Landmark> lm, List<Piece> list) {
            int n = 0;
            for (int[] r : CampaignWorld.routes()) {
                var a = lm.get(r[0]); var b = lm.get(r[1]);
                double length = Math.hypot(b.x() - a.x(), b.y() - a.y()), ux = (b.x() - a.x()) / length, uy = (b.y() - a.y()) / length;
                for (double t = 330; t < length - 330; t += 560, n++) {
                    double side = n % 2 == 0 ? 74 : -74;
                    list.add(new Piece(Kind.CLOTH_POST, a.x() + ux * t - uy * side, a.y() + uy * t + ux * side, n % 2));
                }
            }
        }

        /** Trees on a jittered grid, then undergrowth lining the trails and filling gaps. */
        private static void scatter(CampaignWorld.Area area, List<Piece> list, java.util.Random rng) {
            var boss = area.boss();
            var thorns = area.landmarks().get(7);
            for (int gy = 0; gy * 132 < area.height(); gy++) for (int gx = 0; gx * 132 < area.width(); gx++) {
                double x = gx * 132 + 66 + (rng.nextDouble() - .5) * 110, y = gy * 132 + 66 + (rng.nextDouble() - .5) * 110;
                double pick = rng.nextDouble(), variant = rng.nextDouble();
                if (!clear(area, list, x, y, 36)) continue;
                double corrupt = Math.min(Math.hypot(x - boss.x(), y - boss.y()) / 950, Math.hypot(x - thorns.x(), y - thorns.y()) / 560);
                Kind kind = corrupt < 1 && pick < 1.1 - corrupt ? Kind.WITHERED
                        : pick < .04 ? Kind.WITHERED : (y < 1500 ? pick < .7 : pick < .22) ? Kind.PINE : Kind.OAK;
                int variants = kind == Kind.OAK ? 3 : 2;
                list.add(new Piece(kind, x, y, (int) (variant * variants)));
            }
            for (int i = 0; i < 2600; i++) {
                double x = 160 + rng.nextDouble() * (area.width() - 320), y = 200 + rng.nextDouble() * (area.height() - 360);
                double roll = rng.nextDouble(), v = rng.nextDouble();
                // bushes crowd the trail edges; rocks, logs and stumps sit back in the trees
                boolean edge = CampaignWorld.nearRoutes(x, y, area.landmarks(), 150) && !CampaignWorld.nearRoutes(x, y, area.landmarks(), 96);
                if (edge && roll < .5 && clear(area, list, x, y, -1)) list.add(new Piece(Kind.BUSH, x, y, (int) (v * 3)));
                else if (!edge && roll < .05 && clear(area, list, x, y, 30)) list.add(new Piece(Kind.BOULDER, x, y, (int) (v * 2)));
                else if (!edge && roll < .08 && clear(area, list, x, y, 50)) list.add(new Piece(Kind.LOG, x, y, (int) (v * 2)));
                else if (!edge && roll < .11 && clear(area, list, x, y, 20)) list.add(new Piece(Kind.STUMP, x, y, 0));
                else if (!edge && roll < .2 && clear(area, list, x, y, -1)) list.add(new Piece(Kind.BUSH, x, y, (int) (v * 3)));
            }
        }

        /**
         * Whether a piece of the given clearance radius may stand at (x, y); -1 marks a walk-through
         * bush, which may sit closer to trails but never on one or on a spawn.
         */
        private static boolean clear(CampaignWorld.Area area, List<Piece> list, double x, double y, double radius) {
            boolean soft = radius < 0;
            double r = Math.max(0, radius);
            if (x < 150 || y < 180 || x > area.width() - 150 || y > area.height() - 130) return false;
            if (CampaignWorld.nearRoutes(x, y, area.landmarks(), soft ? 96 : 150 + r * .5)) return false;
            if (CampaignWorld.channelDistance(x, y) < CampaignWorld.CHANNEL_HALF_WIDTH + (soft ? 10 : 60 + r * .5)) return false;
            for (int i = 0; i < area.landmarks().size(); i++) {
                var l = area.landmarks().get(i);
                double clearing = switch (i) { case 2 -> 520; case 8 -> 700; case 1 -> 380; default -> l.optional() ? 290 : 340; };
                if (Math.hypot(x - l.x(), y - l.y()) < clearing + r) return false;
            }
            for (var p : new CampaignWorld.Point[]{area.exit(), area.returnPortal(), area.spawn()})
                if (Math.hypot(x - p.x(), y - p.y()) < 300 + r) return false;
            for (var e : area.enemies()) if (Math.hypot(x - e.x(), y - e.y()) < 130 + r) return false;
            for (var p : list) {
                double room = soft ? (p.kind() == Kind.BUSH ? 70 : p.kind().footWidth / 2.0 + 20)
                        : p.kind().tree() && radius <= 36 ? 112 : Math.max(100, p.kind().footWidth / 2.0 + 60) + r;
                if (Math.abs(p.x() - x) < room && Math.abs(p.y() - y) < room) return false;
            }
            return true;
        }
    }
}
