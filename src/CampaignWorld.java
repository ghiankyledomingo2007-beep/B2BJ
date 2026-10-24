import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/** Authored regions in campaign order: Ruined Outpost, then the Corrupted Forest beyond its east gate. */
public final class CampaignWorld {
    public static final int AREA_COUNT = 2;
    public record Point(double x, double y) { }
    public record Landmark(String name, double x, double y, String lore, boolean optional) { }
    public record EnemySpawn(double x, double y, CampaignEnemy.Kind kind) { }
    public record Area(int id, String name, int width, int height, Point spawn, Point hub, Point boss,
            Point exit, Point returnPortal, List<Landmark> landmarks,
            List<EnemySpawn> enemies, List<RuinedOutpostMap.Obstacle> obstacles) {
        public Area {
            landmarks = List.copyOf(landmarks); enemies = List.copyOf(enemies); obstacles = List.copyOf(obstacles);
        }
    }
    private static final int[][] ROUTES = {{0,1},{1,2},{2,3},{3,4},{4,5},{5,6},{6,3},
            {6,7},{7,8},{2,6},{1,9},{4,10},{7,11}};
    private static final String[] LANDMARK_NAMES = {"RAIN DITCH", "CAPTAIN CAMP", "MUSTER GROUND",
            "BROKEN PALISADE", "ABANDONED BARRACKS", "NORTH WATCH", "STANDARD YARD", "INNER COURT",
            "EAST GATEHOUSE", "SIGNAL TOWER", "FIELD INFIRMARY", "SUPPLY RESERVE"};
    private static final int[][] LOCATIONS = {{512,3072},{1216,2816},{2368,3008},{2944,2176},{2496,1216},
            {4032,1216},{4160,2688},{5120,2752},{5376,1664},{1088,960},{3264,640},{5376,3520}};
    private static final String[] LORE = {"Rainoray. Your last shift is over.",
            "Mara keeps a lamp lit for anyone returning.", "The expedition gathered here. Its banners never returned.",
            "A broad breach cuts through the abandoned defence.", "Beds remain made for soldiers still listed as missing.",
            "The watch faced the forest until its final night.", "The fallen standard still carries a trace of Ichor.",
            "Two roads meet before the Warden's post.", "The Outpost Warden holds a gate no captain can relieve.",
            "The east road stays open until the last stretcher passes.", "One supply chest survived the evacuation.",
            "Spare weapons wait for an army that will never collect them."};
    private static final Area OUTPOST = create();
    private CampaignWorld() { }
    public static Area area(int id) {
        return switch (id) { case 0 -> OUTPOST; case 1 -> FOREST; default -> throw new IllegalArgumentException("No region " + id); };
    }
    /** Landmark index pairs joined by a road; the baked ground draws its tracks along these. */
    static int[][] routes() { return java.util.Arrays.stream(ROUTES).map(int[]::clone).toArray(int[][]::new); }

    /** Rubble stays off the fenced yards (FieldObjects) and the open ground in front of their gates. */
    private static boolean clearOfYards(double x, double y) {
        for (var yard : FieldObjects.yards(0))
            if (Math.abs(x - yard.x()) < FieldObjects.Yard.HALF_WIDTH + 140
                    && y > yard.northY() - 140 && y < yard.southY() + 300) return false;
        return true;
    }

    private static Area create() {
        var landmarks = new ArrayList<Landmark>();
        for (int i = 0; i < LANDMARK_NAMES.length; i++) landmarks.add(new Landmark(
                LANDMARK_NAMES[i], LOCATIONS[i][0], LOCATIONS[i][1], LORE[i], i >= 9));
        var enemies = new ArrayList<EnemySpawn>();
        CampaignEnemy.Kind[] kinds = {CampaignEnemy.Kind.OUTPOST_SCOUT, CampaignEnemy.Kind.OUTPOST_SCOUT,
                CampaignEnemy.Kind.OUTPOST_GUARD, CampaignEnemy.Kind.OUTPOST_SPITTER};
        int[][] offsets = {{-96,64},{112,64},{0,-112}};
        for (int location = 2; location < 8; location++) for (int i = 0; i < offsets.length; i++) {
            var point = landmarks.get(location);
            enemies.add(new EnemySpawn(point.x() + offsets[i][0], point.y() + offsets[i][1],
                    kinds[(i + location - 2) % kinds.length]));
        }
        for (int location = 9; location < 12; location++) {
            var point = landmarks.get(location);
            enemies.add(new EnemySpawn(point.x() + 112, point.y() + 96, kinds[location - 9]));
        }
        var obstacles = new ArrayList<RuinedOutpostMap.Obstacle>();
        int[][] clusters = {{512,640},{1792,640},{4480,512},{5504,576},{1664,1664},{3392,1472},
                {768,2048},{1856,2240},{3520,3072},{4480,3456},{2560,3776},{5760,2432},
                {448,3776},{1472,3648},{3456,2624},{4736,1792},{5760,3904}};
        for (int[] cluster : clusters) for (int i = 0; i < 6; i++) {
            double x = cluster[0] + (i % 3 - 1) * 160, y = cluster[1] + (i / 3 - .5) * 176;
            var prop = i % 3 == 0 ? RuinedOutpostMap.Prop.WALL
                    : i % 3 == 1 ? RuinedOutpostMap.Prop.PALISADE : RuinedOutpostMap.Prop.BARREL;
            if (!nearRoutes(x, y, landmarks, 208) && clearOfLandmarks(x, y, landmarks, 400) && clearOfYards(x, y))
                obstacles.add(new RuinedOutpostMap.Obstacle(x, y, prop.footprintWidth * 2, prop.footprintDepth * 2, prop));
        }
        Point spawn = point(landmarks.get(0)), hub = point(landmarks.get(1)), boss = point(landmarks.get(8));
        return new Area(0, "RUINED OUTPOST", 6144, 4096, spawn, hub, boss,
                new Point(boss.x() + 384, boss.y() - 224), new Point(spawn.x() - 160, spawn.y()), landmarks, enemies, obstacles);
    }
    // ------------------------------------------------------------------ Corrupted Forest
    private static final String[] FOREST_NAMES = {"ASHEN TRAIL", "FERRY REFUGE", "IMP CLEARING", "ROOT CROSSING",
            "HOLLOW GROVE", "WHITE CLOTH RIDGE", "BLACKWATER FORD", "THORN APPROACH", "BRIARHEART ROOTS",
            "WAYFINDER'S REST", "MOONWELL", "ABANDONED ORCHARD"};
    private static final int[][] FOREST_LOCATIONS = {{576,3072},{1280,2624},{2112,3264},{2816,2368},{2176,1280},
            {3840,768},{4224,2432},{5120,3008},{5376,1536},{896,960},{2816,512},{5056,3648}};
    private static final String[] FOREST_LORE = {"Gold light moves between the roots.",
            "Sedge marks the way back with white cloth.", "Rift Imps gather where the canopy opens.",
            "Old roots divide the road into two ways forward.", "The trees drank what the knights spilled.",
            "The high road overlooks the ruined watercourse.", "The ford has gone dry, but its stones still lead east.",
            "Thorns grip the stair beneath the clearing.", "Briarheart has rooted itself across the descent.",
            "If the flags go dark, follow the white cloth. It was never a surrender.",
            "A quiet spring reflects a sky the forest can no longer see.", "The last harvest lies untouched beneath black leaves."};
    /** The dry watercourse, east to west: past the ridge, through the ford, under Sedge's ferry, out of the woods. */
    private static final int[][] CHANNEL = {{4560,-40},{4380,520},{4420,1200},{4300,1850},{4224,2432},{4000,2850},
            {3450,3020},{2900,2950},{2450,2800},{1950,2650},{1450,2470},{1000,2330},{520,2260},{-40,2180}};
    /** Half the dry channel's width in world pixels. */
    static final double CHANNEL_HALF_WIDTH = 104;
    // Declared after the tables it reads: static fields initialise in source order.
    private static final Area FOREST = createForest();

    static List<Point> forestChannel() {
        var list = new ArrayList<Point>();
        for (int[] p : CHANNEL) list.add(new Point(p[0], p[1]));
        return List.copyOf(list);
    }
    /** Distance from (x, y) to the dry channel's centre line. */
    static double channelDistance(double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < CHANNEL.length; i++)
            best = Math.min(best, Line2D.ptSegDist(CHANNEL[i][0], CHANNEL[i][1], CHANNEL[i + 1][0], CHANNEL[i + 1][1], x, y));
        return best;
    }

    private static Area createForest() {
        var landmarks = new ArrayList<Landmark>();
        for (int i = 0; i < FOREST_NAMES.length; i++) landmarks.add(new Landmark(
                FOREST_NAMES[i], FOREST_LOCATIONS[i][0], FOREST_LOCATIONS[i][1], FOREST_LORE[i], i >= 9));
        var enemies = new ArrayList<EnemySpawn>();
        CampaignEnemy.Kind[] kinds = {CampaignEnemy.Kind.RIFT_IMP, CampaignEnemy.Kind.RIFT_IMP,
                CampaignEnemy.Kind.THORN_WOLF, CampaignEnemy.Kind.MIRE_SHAMAN};
        int[][] offsets = {{-96,64},{112,64},{0,-112},{128,-96}};
        for (int location = 2; location < 8; location++) for (int i = 0; i < offsets.length; i++) {
            var point = landmarks.get(location);
            enemies.add(new EnemySpawn(point.x() + offsets[i][0], point.y() + offsets[i][1], kinds[i]));
        }
        for (int location = 9; location < 12; location++) {
            var point = landmarks.get(location);
            enemies.add(new EnemySpawn(point.x() + 112, point.y() + 96, kinds[location - 9]));
        }
        Point spawn = point(landmarks.get(0)), hub = point(landmarks.get(1)), boss = point(landmarks.get(8));
        return new Area(1, "CORRUPTED FOREST", 6144, 4096, spawn, hub, boss,
                new Point(boss.x() + 384, boss.y() - 224), new Point(spawn.x() - 160, spawn.y()), landmarks, enemies, List.of());
    }

    private static Point point(Landmark landmark) { return new Point(landmark.x(), landmark.y()); }
    private static boolean clearOfLandmarks(double x, double y, List<Landmark> landmarks, double radius) {
        for (var point : landmarks) if (Math.hypot(x - point.x(), y - point.y()) < radius) return false;
        return true;
    }
    static boolean nearRoutes(double x, double y, List<Landmark> landmarks, double radius) {
        for (int[] route : ROUTES) { var a = landmarks.get(route[0]); var b = landmarks.get(route[1]);
            if (Line2D.ptSegDist(a.x(), a.y(), b.x(), b.y(), x, y) < radius) return true; }
        return false;
    }
    static boolean paved(Area area, double x, double y) {
        if (nearRoutes(x, y, area.landmarks(), 112)) return true;
        for (var point : area.landmarks()) if (Math.hypot(x - point.x(), y - point.y()) < (point.optional() ? 144 : 192)) return true;
        return Line2D.ptSegDist(area.boss().x(), area.boss().y(), area.exit().x(), area.exit().y(), x, y) < 112;
    }
}
