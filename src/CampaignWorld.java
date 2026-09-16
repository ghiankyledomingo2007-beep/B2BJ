import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/** The presentation build contains one authored region: Ruined Outpost. */
public final class CampaignWorld {
    public static final int AREA_COUNT = 1;
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
    public static Area area(int id) { if (id != 0) throw new IllegalArgumentException("Only Ruined Outpost is included"); return OUTPOST; }

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
            if (!nearRoutes(x, y, landmarks, 208) && clearOfLandmarks(x, y, landmarks, 400))
                obstacles.add(new RuinedOutpostMap.Obstacle(x, y, prop.footprintWidth * 2, prop.footprintDepth * 2, prop));
        }
        Point spawn = point(landmarks.get(0)), hub = point(landmarks.get(1)), boss = point(landmarks.get(8));
        return new Area(0, "RUINED OUTPOST", 6144, 4096, spawn, hub, boss,
                new Point(boss.x() + 384, boss.y() - 224), new Point(spawn.x() - 160, spawn.y()), landmarks, enemies, obstacles);
    }
    private static Point point(Landmark landmark) { return new Point(landmark.x(), landmark.y()); }
    private static boolean clearOfLandmarks(double x, double y, List<Landmark> landmarks, double radius) {
        for (var point : landmarks) if (Math.hypot(x - point.x(), y - point.y()) < radius) return false;
        return true;
    }
    private static boolean nearRoutes(double x, double y, List<Landmark> landmarks, double radius) {
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
