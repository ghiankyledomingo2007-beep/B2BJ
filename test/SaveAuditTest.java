import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class SaveAuditTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("b2bj-save-audit-");
        try {
            cappedWalletCanReceiveLandmarkAndQuestRewards(directory);
            System.out.println("SaveAuditTest passed");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
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
