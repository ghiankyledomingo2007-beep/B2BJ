import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class SaveAuditTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("b2bj-save-audit-");
        try {
            returningToTitleClosesNpcDialogue();
            cappedWalletCanReceiveLandmarkAndQuestRewards(directory);
            cappedWalletDoesNotInterruptBossVictory(directory);
            System.out.println("SaveAuditTest passed");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void cappedWalletDoesNotInterruptBossVictory(Path directory) throws Exception {
        Path save = directory.resolve("boss.properties");
        CampaignSave.save(save, new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(),
                Set.of(), Set.of(), 100_000));
        var game = RuinedOutpostGame.campaign(save);
        assert game.continueCampaign();
        var boss = game.guardian();
        game.player().relocate(boss.x() - 110, boss.y());
        boss.activate(game.player().x(), game.player().y());
        assert boss.hurt(boss.maxHealth() - 1); // Final-strike fixture; controller must perform the actual kill.
        boss.update(1.1, game.player().x(), game.player().y(), game.map());
        assert boss.state() == Guardian.State.RECOVER;
        game.player().collectIchor(100);
        assert game.transform() && game.attack(1, 0);
        game.update(.12, 0, 0);
        assert !boss.alive() && game.map().gateOpen()
                : "a full wallet must not interrupt boss victory or opening the Outpost gate";
        var progress = CampaignSave.load(save).orElseThrow();
        assert progress.shards() == 100_000 && progress.clearedBosses().equals(Set.of(0));
    }

    private static void returningToTitleClosesNpcDialogue() {
        var game = RuinedOutpostGame.campaign();
        game.begin();
        var hub = game.campaignArea().hub();
        game.player().relocate(hub.x(), hub.y());
        assert game.interact() && game.campaignStory().dialogueOpen();
        game.pause();
        assert game.returnToTitle();
        assert game.story().phase() == OutpostStory.Phase.PROLOGUE;
        assert !game.campaignStory().dialogueOpen()
                : "returning to title must close dialogue that would hide the title overlay";
        assert game.continueCampaign() && !game.campaignStory().dialogueOpen();
    }

    private static void cappedWalletCanReceiveLandmarkAndQuestRewards(Path directory) throws Exception {
        for (boolean quest : new boolean[] {false, true}) {
            Path save = directory.resolve(quest ? "quest.properties" : "landmark.properties");
            CampaignSave.save(save, new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(),
                    quest ? Set.of("memory_0") : Set.of(), Set.of(), 100_000));
            var game = RuinedOutpostGame.campaign(save);
            assert game.continueCampaign() : "maximum currency is a valid save state";
            if (quest) {
                var hub = game.campaignArea().hub();
                game.player().relocate(hub.x(), hub.y());
            } else {
                var landmark = game.campaignArea().landmarks().stream()
                        .filter(CampaignWorld.Landmark::optional).findFirst().orElseThrow();
                game.player().relocate(landmark.x(), landmark.y());
            }
            assert game.interact() : "wallet capacity must not block story progress";
            assert game.shards() == 100_000 : "earned shards must saturate at the save format limit";
            var progress = CampaignSave.load(save).orElseThrow();
            assert progress.shards() == 100_000;
            assert progress.questFlags().contains(quest ? "quest_0" : "memory_0")
                    : "reward overflow must not interrupt durable quest progress";
        }
    }
}
