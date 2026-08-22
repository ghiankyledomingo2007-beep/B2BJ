import java.util.HashSet;
import java.util.Set;

public final class RuinedOutpostMapTest {
    public static void main(String[] args) {
        worldIsLargerThanTheViewport();
        mapContainsEarthStoneAndTransitions();
        everyWangPatternHasOneUniqueSourceTile();
        System.out.println("RuinedOutpostMapTest passed");
    }

    private static void worldIsLargerThanTheViewport() {
        RuinedOutpostMap map = new RuinedOutpostMap();

        assert map.worldWidth() > 1280 : "map must scroll horizontally";
        assert map.worldHeight() > 720 : "map must scroll vertically";
    }

    private static void mapContainsEarthStoneAndTransitions() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        boolean hasEarth = false;
        boolean hasStone = false;
        boolean hasTransition = false;

        for (int y = 0; y < map.heightInTiles(); y++) {
            for (int x = 0; x < map.widthInTiles(); x++) {
                int mask = map.tileMask(x, y);
                hasEarth |= mask == 0;
                hasStone |= mask == 15;
                hasTransition |= mask > 0 && mask < 15;
            }
        }

        assert hasEarth : "map needs battlefield earth";
        assert hasStone : "map needs outpost paving";
        assert hasTransition : "map needs seamless earth-to-stone borders";
    }

    private static void everyWangPatternHasOneUniqueSourceTile() {
        Set<String> sourceTiles = new HashSet<>();

        for (int mask = 0; mask < 16; mask++) {
            int x = WangTileset.sourceX(mask);
            int y = WangTileset.sourceY(mask);
            assert x >= 0 && x <= 96 && x % 32 == 0 : "invalid source x";
            assert y >= 0 && y <= 96 && y % 32 == 0 : "invalid source y";
            sourceTiles.add(x + "," + y);
        }

        assert sourceTiles.size() == 16 : "all corner patterns need unique tiles";
    }
}
