import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Beating the Warden opens the Outpost's gate into the Corrupted Forest; beating Briarheart and taking
 * the stair at the forest's end finishes the campaign.
 */
public final class CampaignVictoryTest {
    public static void main(String[] args) throws Exception {
        Path save = Files.createTempDirectory("b2bj-victory-").resolve("campaign.properties");
        RuinedOutpostGame game = RuinedOutpostGame.campaign(save);
        game.begin();
        var exit = game.campaignArea().exit();
        game.player().relocate(exit.x(), exit.y());
        assert !game.interact() || game.biome() == 0 : "the gate stays shut while the Warden lives";

        defeatBoss(game);
        game.drainEvents();
        assert game.interact() : "the gate opens once the Warden is dead";
        assert game.biome() == 1 : "the open gate leads into the Corrupted Forest";
        assert game.story().phase() != OutpostStory.Phase.COMPLETE : "the campaign goes on past the Outpost";
        assert game.drainEvents().stream().noneMatch(e -> e.type() == RuinedOutpostGame.EventType.VICTORY);
        var forestSpawn = game.campaignArea().spawn();
        assert Math.hypot(game.player().x() - forestSpawn.x(), game.player().y() - forestSpawn.y()) < 1 : "arrive on the Ashen Trail";

        var stair = game.campaignArea().exit();
        game.player().relocate(stair.x(), stair.y());
        assert !game.interact() || game.story().phase() != OutpostStory.Phase.COMPLETE
                : "the stair stays wrapped while Briarheart lives";
        assert game.guardian().profile() == Guardian.Profile.BRIARHEART : "Briarheart guards the forest";
        defeatBoss(game);
        game.drainEvents();
        assert game.interact() : "the stair opens once Briarheart falls";
        List<RuinedOutpostGame.Event> events = game.drainEvents();
        assert events.stream().anyMatch(e -> e.type() == RuinedOutpostGame.EventType.VICTORY) : "descending is the victory";
        assert game.story().phase() == OutpostStory.Phase.COMPLETE : "the story ends";

        game.restart();
        assert game.story().phase() == OutpostStory.Phase.PROLOGUE : "R after the ending goes back to the title";
        System.out.println("CampaignVictoryTest passed");
    }

    /** Kills the area's boss through the game's own defeat path (clears it in the save, opens the route). */
    private static void defeatBoss(RuinedOutpostGame game) throws Exception {
        var boss = game.guardian();
        boss.activate(game.player().x(), game.player().y());
        boss.hurt(boss.maxHealth());
        var defeat = RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit", boolean.class);
        defeat.setAccessible(true);
        defeat.invoke(game, false);
    }
}
