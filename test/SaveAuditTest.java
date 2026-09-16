import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class SaveAuditTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("b2bj-save-audit-");
        Path save = directory.resolve("campaign.properties");
        CampaignSave.Progress expected = new CampaignSave.Progress(0, 1, Set.of(), Set.of("landmark_0_9"),
                Map.of("vitality", 1), 12);
        CampaignSave.save(save, expected);
        assert CampaignSave.load(save).orElseThrow().equals(expected) : "checkpoint must survive round trip";
        System.out.println("SaveAuditTest passed");
    }
}
