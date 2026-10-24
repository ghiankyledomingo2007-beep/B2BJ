import java.util.ArrayDeque;

/** The Outpost's non-combat interactions: form-only paths, carrying, the cache, survivors and rest braziers. */
public final class FieldObjectsTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        placement();
        reachability();
        gatesByForm();
        carryUnlockAndDeliver();
        survivorsComeHome();
        bladeCutsAndLights();
        saveAcceptsRestCheckpoints();
        System.out.println("FieldObjectsTest passed");
    }

    /** Yards sit on open ground: their fences cross no existing obstacle and every placed thing stands clear. */
    private static void placement() {
        var plain = RuinedOutpostMap.campaign(0);
        plain.setFieldGates(java.util.List.of(), java.util.List.of());
        var area = CampaignWorld.area(0);
        for (var yard : FieldObjects.yards(0)) {
            for (var l : area.landmarks()) assert Math.hypot(l.x() - yard.x(), l.y() - yard.y()) > 330 : yard.name() + " crowds " + l.name();
            for (double y = yard.northY() - 40; y <= yard.southY() + 40; y += 16)
                for (double x = yard.x() - 340; x <= yard.x() + 340; x += 16) {
                    boolean fence = Math.abs(y - yard.northY()) < 20 || Math.abs(y - yard.southY()) < 20
                            || Math.abs(Math.abs(x - yard.x()) - FieldObjects.Yard.HALF_WIDTH - 16) < 20;
                    if (!fence) continue;
                    // remove the field walls: what is left is the map as it was
                    assert !blockedWithoutField(x, y) : yard.name() + " fence overlaps an obstacle at " + x + "," + y;
                }
        }
        for (var yard : FieldObjects.yards(0)) for (var enemy : area.enemies())
            assert Math.abs(enemy.x() - yard.x()) > FieldObjects.Yard.HALF_WIDTH + 80 || Math.abs(enemy.y() - yard.y()) > FieldObjects.Yard.HALF_DEPTH + 80
                    : "an enemy spawns in or against " + yard.name();
        for (var spot : FieldObjects.all(0)) {
            if (spot.kind() == FieldObjects.Kind.CRAWL || spot.kind() == FieldObjects.Kind.BRAMBLE) continue;
            assert !plain.isBlocked(spot.x(), spot.y() + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS, true)
                    : spot.id() + " stands inside something";
        }
        for (var rest : FieldObjects.rests(0)) assert !plain.isBlocked(rest.x(), rest.y() + 80, Player.COLLISION_RADIUS, false)
                : "nowhere to stand at a rest brazier";
    }
    private static final RuinedOutpostMap BARE = bare();
    private static RuinedOutpostMap bare() {
        // The map's own obstacles only: a probe at fence spots must not see the fences themselves.
        try {
            var map = RuinedOutpostMap.campaign(0);
            var field = RuinedOutpostMap.class.getDeclaredField("fieldWalls"); field.setAccessible(true);
            ((java.util.List<?>) field.get(map)).clear();
            return map;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static boolean blockedWithoutField(double x, double y) { return BARE.isBlocked(x, y, 1); }

    /** Flood fill from camp: the slime reaches every yard; the Blade reaches the thorn yard only once it is cut. */
    private static void reachability() {
        var map = RuinedOutpostMap.campaign(0);
        var crawl = new java.util.ArrayList<RuinedOutpostMap.Obstacle>();
        var thorns = new java.util.ArrayList<RuinedOutpostMap.Obstacle>();
        var slimeOnly = new java.util.ArrayList<RuinedOutpostMap.Obstacle>();
        for (var s : FieldObjects.all(0)) {
            if (s.kind() == FieldObjects.Kind.CRAWL) { crawl.addAll(FieldObjects.gateParts(s, false)); slimeOnly.addAll(FieldObjects.gateParts(s, true)); }
            if (s.kind() == FieldObjects.Kind.BRAMBLE) thorns.addAll(FieldObjects.gateParts(s, false));
        }
        var solid = new java.util.ArrayList<>(crawl); solid.addAll(thorns);
        map.setFieldGates(solid, slimeOnly);
        var hub = CampaignWorld.area(0).hub();
        boolean[][] slime = fill(map, hub, true), blade = fill(map, hub, false);
        for (var s : FieldObjects.all(0)) {
            if (s.kind() == FieldObjects.Kind.CRAWL || s.kind() == FieldObjects.Kind.BRAMBLE) continue;
            boolean thornYard = s.id().equals("survivor_1") || s.id().equals("cache_0");
            boolean inYard = !s.id().equals("tin_2");
            assert reached(slime, s.x(), s.y()) != thornYard : s.id() + " slime reach";
            assert !reached(blade, s.x(), s.y()) || !inYard : s.id() + " must sit behind its yard's gate";
        }
        map.setFieldGates(crawl, slimeOnly);   // thorns cut
        boolean[][] cut = fill(map, hub, false);
        assert reached(cut, FieldObjects.find(0, "cache_0").x(), FieldObjects.find(0, "cache_0").y()) : "cut thorns open the yard";
        assert !reached(cut, FieldObjects.find(0, "survivor_0").x(), FieldObjects.find(0, "survivor_0").y()) : "crawl yards stay slime-only";
        for (var rest : FieldObjects.rests(0)) assert reached(blade, rest.x(), rest.y() + 80) : "rest brazier reachable";
    }
    private static boolean[][] fill(RuinedOutpostMap map, CampaignWorld.Point from, boolean slime) {
        int step = 16, w = map.worldWidth() / step, h = map.worldHeight() / step;
        var seen = new boolean[w][h];
        var queue = new ArrayDeque<int[]>();
        queue.add(new int[]{(int) from.x() / step, (int) from.y() / step});
        seen[(int) from.x() / step][(int) from.y() / step] = true;
        while (!queue.isEmpty()) {
            var c = queue.poll();
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = c[0] + d[0], y = c[1] + d[1];
                if (x < 0 || y < 0 || x >= w || y >= h || seen[x][y]) continue;
                if (map.isBlocked(x * step + step / 2.0, y * step + step / 2.0 + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS, slime)) continue;
                seen[x][y] = true; queue.add(new int[]{x, y});
            }
        }
        return seen;
    }
    private static boolean reached(boolean[][] seen, double x, double y) {
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
            int gx = (int) (x / 16) + dx, gy = (int) (y / 16) + dy;
            if (gx >= 0 && gy >= 0 && gx < seen.length && gy < seen[0].length && seen[gx][gy]) return true;
        }
        return false;
    }

    private static RuinedOutpostGame game() {
        var game = RuinedOutpostGame.campaign(); game.begin(); game.skipDialogue();
        game.setDebugGodMode(true);
        return game;
    }

    private static void gatesByForm() {
        var game = game();
        var crawl = FieldObjects.find(0, "gate_0");
        double y = crawl.y() - 12 - Player.COLLISION_Y_OFFSET;
        assert !game.map().isBlocked(crawl.x(), y + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS, true) : "slime fits the gap";
        assert game.map().isBlocked(crawl.x(), y + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS, false) : "Blade and enemies do not";
        game.player().relocate(crawl.x(), y);
        game.player().collectIchor(100);
        assert !game.transform() && !game.player().bladeForm() : "no changing into the Blade inside the gap";
    }

    private static void carryUnlockAndDeliver() {
        var game = game();
        var key = FieldObjects.find(0, "key_0");
        game.player().relocate(key.x(), key.y());
        int shards = game.shards();
        assert game.interact() && game.carried() == key && game.fieldPosition(key) == null : "the slime swallows the key";
        game.player().collectIchor(100);
        assert game.transform();
        game.update(.01, 0, 0);
        assert game.carried() == null && game.fieldPosition(key) != null : "the Blade cannot hold it; it drops where he stands";
        game.player().setUnlimitedBlade(false);
        while (game.player().bladeForm()) { game.player().spendBladeIchor(100); game.update(.05, 0, 0); }
        var at = game.fieldPosition(key);
        game.player().relocate(at.x(), at.y() - 40);
        assert game.interact() && game.carried() == key;
        var cache = FieldObjects.find(0, "cache_0");
        game.player().relocate(cache.x(), cache.y() + 60);
        assert game.interact() && game.fieldDone(cache) && game.fieldDone(key) && game.carried() == null;
        assert game.shards() == shards + 3 && game.dialogue() == OutpostDialogue.LEDGER;
        while (game.dialogue() != null) game.interact();
        assert !game.interact() || game.dialogue() == null : "an opened cache gives nothing twice";

        var tin = FieldObjects.find(0, "tin_2");
        game.player().relocate(tin.x(), tin.y());
        assert game.interact() && game.carried() == tin;
        var hub = game.campaignArea().hub();
        game.player().relocate(hub.x(), hub.y());
        assert game.interactionPrompt().startsWith("E HAND OVER");
        shards = game.shards();
        assert game.interact() && game.tinsDelivered() == 1 && game.shards() == shards + 1 && game.carried() == null;
    }

    private static void survivorsComeHome() {
        var game = game();
        int shards = game.shards();
        for (int i = 0; i < FieldObjects.SURVIVORS.length; i++) {
            var s = FieldObjects.find(0, "survivor_" + i);
            game.player().relocate(s.x(), s.y() + 50);
            assert game.interact() && game.dialogue() == OutpostDialogue.survivor(i);
            while (game.dialogue() != null) game.interact();
            assert game.fieldDone(s) && game.interactionPrompt().indexOf(s.name()) < 0 : "a rescued survivor leaves for camp";
        }
        assert game.rescued() == 3 && game.shards() == shards + 6;
        var hub = game.campaignArea().hub();
        game.player().relocate(hub.x(), hub.y());
        while (!game.dialogueSeen(OutpostDialogue.CAMP_HOME)) {
            assert game.interact();
            while (game.dialogue() != null) game.interact();
        }
        assert game.shards() == shards + 10 : "Mara's thanks once everyone is home";
    }

    private static void bladeCutsAndLights() {
        var game = game();
        game.player().collectIchor(100); assert game.transform();
        game.setDebugUnlimitedBlade(true);
        var thorns = FieldObjects.find(0, "gate_1");
        assert game.map().isBlocked(thorns.x(), thorns.y(), Player.COLLISION_RADIUS, true) : "thorns stop the slime too";
        strike(game, thorns.x(), thorns.y() + 70);
        assert game.fieldDone(thorns) && !game.map().isBlocked(thorns.x(), thorns.y() - 4, Player.COLLISION_RADIUS, false) : "a Blade strike cuts the thorns";

        var rest = FieldObjects.rests(0).get(1);
        assert !game.restLit(1);
        strike(game, rest.x(), rest.y() + 70);
        assert game.restLit(1) : "a Blade strike lights the brazier";
        game.player().relocate(rest.x(), rest.y() + 70);
        for (var enemy : game.scouts()) enemy.hurt(10_000);
        assert game.saveCheckpoint() && game.campaignCheckpoint() == FieldObjects.REST_CHECKPOINT + 1 : "lit brazier is a checkpoint";
    }
    private static void strike(RuinedOutpostGame game, double x, double fromY) {
        game.player().relocate(x, fromY);
        for (int tries = 0; tries < 6; tries++) {
            game.requestBladeAttack(false, 0, -1);
            for (int i = 0; i < 40; i++) game.update(.02, 0, 0);
        }
    }

    private static void saveAcceptsRestCheckpoints() {
        new CampaignSave.Progress(0, FieldObjects.REST_CHECKPOINT + 2, java.util.Set.of(), java.util.Set.of(), java.util.Map.of(), 0);
        try {
            new CampaignSave.Progress(0, FieldObjects.REST_CHECKPOINT + 3, java.util.Set.of(), java.util.Set.of(), java.util.Map.of(), 0);
            throw new AssertionError("an unknown checkpoint must be rejected");
        } catch (IllegalArgumentException expected) { }
    }
}
