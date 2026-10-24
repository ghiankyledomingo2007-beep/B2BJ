import java.util.ArrayList;
import java.util.List;

/**
 * The Outpost's things to do besides fighting, placed once per region:
 * fenced yards that only one form can enter (the slime squeezes under a broken fence, the Blade cuts
 * a bramble wall), things the slime swallows and carries (a key, supply tins for the camp), a locked
 * cache, expedition members still missing in the ruins, and dormant braziers the Blade can light.
 *
 * Live state (what was cut, delivered, rescued or lit) belongs to {@link RuinedOutpostGame}; this class
 * only knows where everything is and what shape it has. Yards are probed clear of roads, landmarks and
 * obstacles by FieldObjectsTest.
 */
public final class FieldObjects {
    private FieldObjects() { }

    public enum Kind { CRAWL, BRAMBLE, KEY, TIN, CACHE, SURVIVOR }
    /** Things the slime can swallow; at most one rides in the jelly at a time. */
    public static boolean carriable(Kind kind) { return kind == Kind.KEY || kind == Kind.TIN; }

    /** One placed object; ids are save-file identifiers, so never rename one. */
    public record Spot(String id, Kind kind, double x, double y, int variant, String name) { }

    /** A fenced yard: (x, y) is its centre; its south wall's middle section is the way in. */
    public record Yard(String name, double x, double y, Kind gate) {
        static final double HALF_WIDTH = 288, HALF_DEPTH = 128, SECTION = 192, SIDE = 32, SIDE_SECTION = 128;
        double southY() { return y + HALF_DEPTH; }
        double northY() { return y - HALF_DEPTH; }
    }

    /** People listed missing on the evacuation dispatch. Order matches the survivor sheet's columns. */
    public static final String[] SURVIVORS = {"TOBIN", "ILSE", "VENN"};
    /** Column of each survivor in field_survivor.png: Tobin's hurt leg, Ilse's arm sling, old Venn's blanket. */
    public static final int[] SPRITE_COLUMN = {1, 0, 2};
    /** A rest brazier is checkpoint REST_CHECKPOINT + its index. */
    public static final int REST_CHECKPOINT = 10;
    /** Width of the squeeze-through gap in a crawl fence, in world pixels; the slime's body is 48 across. */
    public static final double CRAWL_GAP = 72;

    private static final List<Yard> YARDS = List.of(
            new Yard("STOREHOUSE YARD", 3304, 3604, Kind.CRAWL),
            new Yard("THORN YARD", 1704, 1288, Kind.BRAMBLE),
            new Yard("CULVERT YARD", 3404, 1868, Kind.CRAWL));
    /** Dormant braziers near the Muster Ground, the Barracks and the Standard Yard. */
    private static final List<CampaignWorld.Point> RESTS = List.of(
            new CampaignWorld.Point(2764, 2758), new CampaignWorld.Point(2272, 1482), new CampaignWorld.Point(4482, 2522));
    private static final List<Spot> SPOTS = spots();

    public static List<Yard> yards(int area) { return area == 0 ? YARDS : List.of(); }
    public static List<CampaignWorld.Point> rests(int area) { return area == 0 ? RESTS : List.of(); }
    public static List<Spot> all(int area) { return area == 0 ? SPOTS : List.of(); }

    public static Spot find(int area, String id) {
        for (var s : all(area)) if (s.id().equals(id)) return s;
        return null;
    }

    private static List<Spot> spots() {
        var list = new ArrayList<Spot>();
        var storehouse = YARDS.get(0); var thorns = YARDS.get(1); var culvert = YARDS.get(2);
        for (int i = 0; i < YARDS.size(); i++) {
            var yard = YARDS.get(i);
            list.add(new Spot("gate_" + i, yard.gate(), yard.x(), yard.southY(), i, yard.name()));
        }
        // Storehouse: Tobin is pinned behind the fence; the cache key and a tin lie in the straw.
        list.add(new Spot("survivor_0", Kind.SURVIVOR, storehouse.x() - 160, storehouse.y() - 36, 0, SURVIVORS[0]));
        list.add(new Spot("key_0", Kind.KEY, storehouse.x() + 170, storehouse.y() + 30, 0, "CACHE KEY"));
        list.add(new Spot("tin_0", Kind.TIN, storehouse.x() + 40, storehouse.y() - 50, 0, "SUPPLY TIN"));
        // Thorn yard: Ilse kept the quartermaster's cache shut; its key is in the storehouse.
        list.add(new Spot("survivor_1", Kind.SURVIVOR, thorns.x() - 150, thorns.y() - 30, 1, SURVIVORS[1]));
        list.add(new Spot("cache_0", Kind.CACHE, thorns.x() + 150, thorns.y() - 40, 0, "LOCKED CACHE"));
        // Culvert: Venn, and the second tin.
        list.add(new Spot("survivor_2", Kind.SURVIVOR, culvert.x() + 140, culvert.y() - 34, 2, SURVIVORS[2]));
        list.add(new Spot("tin_1", Kind.TIN, culvert.x() - 150, culvert.y() + 20, 1, "SUPPLY TIN"));
        // The third tin fell off a cart in the open grass north of the watch.
        list.add(new Spot("tin_2", Kind.TIN, 4420, 900, 2, "SUPPLY TIN"));
        return List.copyOf(list);
    }

    public static int count(int area, Kind kind) {
        int n = 0;
        for (var s : all(area)) if (s.kind() == kind) n++;
        return n;
    }

    /** The yards' fences, always solid. The gate sections are separate: see {@link #gateParts}. */
    public static List<RuinedOutpostMap.Obstacle> walls(int area) {
        var list = new ArrayList<RuinedOutpostMap.Obstacle>();
        for (var yard : yards(area)) {
            double depth = 24;
            for (int i = -1; i <= 1; i++) {
                list.add(new RuinedOutpostMap.Obstacle(yard.x() + i * Yard.SECTION, yard.northY() - depth / 2, Yard.SECTION, depth));
                if (i != 0) list.add(new RuinedOutpostMap.Obstacle(yard.x() + i * Yard.SECTION, yard.southY() - depth / 2, Yard.SECTION, depth));
            }
            for (int side = -1; side <= 1; side += 2)
                list.add(new RuinedOutpostMap.Obstacle(yard.x() + side * (Yard.HALF_WIDTH + Yard.SIDE / 2), yard.y(),
                        Yard.SIDE, 2 * Yard.HALF_DEPTH));
        }
        return list;
    }

    /**
     * A gate section's blocking parts: a crawl fence is solid apart from its low gap, which stops everything
     * except the slime; a bramble wall stops everything until the Blade cuts it.
     */
    public static List<RuinedOutpostMap.Obstacle> gateParts(Spot gate, boolean slimeOnlyPart) {
        double depth = 24, y = gate.y() - depth / 2;
        if (gate.kind() == Kind.BRAMBLE) return slimeOnlyPart ? List.of() : List.of(new RuinedOutpostMap.Obstacle(gate.x(), y, Yard.SECTION, depth));
        double post = (Yard.SECTION - CRAWL_GAP) / 2;
        if (slimeOnlyPart) return List.of(new RuinedOutpostMap.Obstacle(gate.x(), y, CRAWL_GAP, depth));
        return List.of(new RuinedOutpostMap.Obstacle(gate.x() - CRAWL_GAP / 2 - post / 2, y, post, depth),
                new RuinedOutpostMap.Obstacle(gate.x() + CRAWL_GAP / 2 + post / 2, y, post, depth));
    }

    /** Where rescued people sit round Mara's fire and delivered tins stack, relative to the hub. */
    static CampaignWorld.Point campSeat(CampaignWorld.Point hub, int survivor) {
        int[][] seats = {{-40, -190}, {190, -150}, {230, -64}};
        return new CampaignWorld.Point(hub.x() + seats[survivor][0], hub.y() + seats[survivor][1]);
    }
    static CampaignWorld.Point campTins(CampaignWorld.Point hub) { return new CampaignWorld.Point(hub.x() + 300, hub.y() - 70); }
}
