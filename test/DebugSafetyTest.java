import java.lang.reflect.InvocationTargetException;

public final class DebugSafetyTest {
    public static void main(String[] args) {
        legacyRejectsCheats();
        titleRejectsCheats();
        dialogueRejectsCheats();
        deathRejectsCheats();
        System.out.println("DebugSafetyTest passed");
    }

    static RuinedOutpostGame fresh() {
        var game = RuinedOutpostGame.campaign();
        game.begin();
        return game;
    }

    static void legacyRejectsCheats() {
        var game = new RuinedOutpostGame();
        game.begin();
        denied(game);
    }

    static void titleRejectsCheats() { denied(RuinedOutpostGame.campaign()); }

    static void dialogueRejectsCheats() {
        var game = fresh();
        var hub = game.campaignArea().hub();
        game.player().relocate(hub.x(), hub.y());
        assert game.interact() && game.campaignStory().dialogueOpen();
        denied(game);
    }

    static void deathRejectsCheats() {
        var game = fresh();
        assert game.player().hurt(1_000);
        game.update(.01, 0, 0);
        assert game.story().phase() == OutpostStory.Phase.DEAD;
        denied(game);
    }

    static void denied(RuinedOutpostGame game) {
        double health = game.player().healthValue(), ichor = game.player().ichor();
        int biome = game.biome(), shards = game.shards();
        assert !debug(game, "debugAvailable");
        assert !debug(game, "setDebugGodMode", true);
        assert !debug(game, "setDebugOneShot", true);
        assert !debug(game, "debugRefill") && !debug(game, "debugAddShards");
        assert !debug(game, "debugWarp", 3, true) && !debug(game, "debugResetEncounter");
        assert !debug(game, "debugSession") && !debug(game, "debugGodMode")
                && !debug(game, "debugOneShot");
        assert game.player().healthValue() == health && game.player().ichor() == ichor;
        assert game.biome() == biome && game.shards() == shards;
    }

    static boolean debug(RuinedOutpostGame game, String method, Object... arguments) {
        Class<?>[] types = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++)
            types[i] = arguments[i] instanceof Boolean ? boolean.class : int.class;
        try { return (boolean) game.getClass().getMethod(method, types).invoke(game, arguments); }
        catch (InvocationTargetException failure) {
            throw new AssertionError("Debug operation threw: " + method, failure.getCause());
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("Debug API missing: " + method, missing);
        }
    }
}
