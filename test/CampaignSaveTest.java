import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CampaignSaveTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("b2bj-save-test-");
        try {
            missingSaveDoesNotCreateFiles(directory);
            progressSurvivesRestartAndReplacement(directory);
            corruptSavesRemainUntouched(directory);
            progressRejectsInvalidValuesAndCopiesCollections();
            campaignPrerequisitesAndBacktracking(directory);
            failedReplacementPreservesDestination(directory);
            System.out.println("CampaignSaveTest passed");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.delete(file);
            }
        }
    }

    private static CampaignSave.Progress progress() {
        return new CampaignSave.Progress(2, 1, Set.of(0, 1), Set.of("blade", "crescent"),
                Map.of("vitality", 2, "edge", 1),
                Set.of("talked_0", "memory_0", "quest_0"), Set.of(), 543);
    }

    private static void missingSaveDoesNotCreateFiles(Path directory) throws IOException {
        Path save = directory.resolve("missing/campaign.properties");
        assert CampaignSave.load(save).isEmpty();
        assert !Files.exists(save.getParent()) : "reading absent save must not create directories";
    }

    private static void progressSurvivesRestartAndReplacement(Path directory) throws IOException {
        Path save = directory.resolve("nested/campaign.properties");
        CampaignSave.save(save, progress());
        assert CampaignSave.load(save).orElseThrow().equals(progress())
                : "all campaign progress must survive disk roundtrip";
        var finished = new CampaignSave.Progress(3, 1, Set.of(0, 1, 2, 3),
                Set.of("blade", "crescent", "riposte"), Map.of("vitality", 3),
                Set.of("quest_0", "quest_1", "quest_2", "quest_3"),
                Set.of("CLOSE_RIFT", "HUMAN_FORM"), 100_000);
        CampaignSave.save(save, finished);
        assert CampaignSave.load(save).orElseThrow().equals(finished)
                : "replacement must persist latest campaign";
        try (var files = Files.list(save.getParent())) {
            assert files.count() == 1 : "successful save must leave no temporary files";
        }
        var fresh = new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of());
        CampaignSave.save(save, fresh);
        assert CampaignSave.load(save).orElseThrow().equals(fresh) : "empty collections must roundtrip";
    }

    private static void corruptSavesRemainUntouched(Path directory) throws IOException {
        Path save = directory.resolve("corrupt.properties");
        CampaignSave.save(save, progress());
        String valid = Files.readString(save);
        String[] malformed = {
                "not a campaign save", "", "version=999\n", "version=1\nbiome=2\n",
                valid.replace("biome=2", "biome=-1"),
                valid.replace("checkpoint=1", "checkpoint=bad"),
                valid.replace("shards=543", "shards=-1"),
                valid + "\nclearedBosses=0,4\n", valid + "\nclearedBosses=0,01,1\n",
                valid + "\nupgrades=vitality\n", valid + "\nupgrades=vitality:1,vitality:2\n",
                valid + "\nunlocks=blade,,crescent\n", valid + "\nendings=NONE\n",
                valid + "\nquestFlags=unknown_0\n",
                valid + "\nclearedBosses=0\n", valid + "\nclearedBosses=0,2\n",
                valid + "\nendings=CLOSE_RIFT\n",
                "version=1\n#" + "x".repeat(65_536), "version=1\nbiome=\\uNOPE"
        };
        for (String data : malformed) {
            Files.writeString(save, data);
            boolean rejected = false;
            try { CampaignSave.load(save); }
            catch (IOException expected) { rejected = true; }
            assert rejected : "malformed save must report IOException";
            assert Files.readString(save).equals(data) : "loading corrupt save must preserve original bytes";
        }
    }

    private static void campaignPrerequisitesAndBacktracking(Path directory) throws IOException {
        rejects(() -> new CampaignSave.Progress(3, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(2, 0, Set.of(0), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(0, 2), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of("CLOSE_RIFT")));
        var backtracked = new CampaignSave.Progress(0, 1, Set.of(0, 1, 2), Set.of("landmark_0_2"),
                Map.of(), Set.of("quest_0"), Set.of(), 40);
        assert backtracked.questFlags().equals(Set.of("talked_0", "memory_0", "quest_0"))
                : "completed quest prerequisites must normalize exactly as story restore";
        Path save = directory.resolve("backtracked.properties");
        CampaignSave.save(save, backtracked);
        assert CampaignSave.load(save).orElseThrow().equals(backtracked)
                : "backtracking to a previous biome must remain loadable";
    }

    private static void progressRejectsInvalidValuesAndCopiesCollections() {
        Set<Integer> bosses = new HashSet<>(Set.of(0));
        Set<String> unlocks = new HashSet<>(Set.of("blade"));
        Map<String, Integer> upgrades = new HashMap<>(Map.of("vitality", 1));
        Set<String> flags = new HashSet<>(Set.of("talked_0"));
        Set<String> endings = new HashSet<>();
        var copy = new CampaignSave.Progress(0, 0, bosses, unlocks, upgrades, flags, endings);
        bosses.clear(); unlocks.clear(); upgrades.clear(); flags.clear(); endings.add("HUMAN_FORM");
        assert copy.clearedBosses().equals(Set.of(0)) && copy.unlocks().equals(Set.of("blade"));
        assert copy.upgrades().equals(Map.of("vitality", 1));
        assert copy.questFlags().equals(Set.of("talked_0")) && copy.endings().isEmpty();
        boolean immutable = false;
        try { copy.questFlags().add("quest_0"); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        assert immutable : "save snapshot collections must be immutable";

        rejects(() -> new CampaignSave.Progress(-1, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(4, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, -1, Set.of(), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 2, Set.of(), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(-1), Set.of(), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of("bad,token"), Map.of(), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of("vitality", -1), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of("NONE")));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of(), -1));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(), Set.of(), Set.of(), 100_001));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of("vitality", 4), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of("edge", 2), Set.of(), Set.of()));
        rejects(() -> new CampaignSave.Progress(0, 0, Set.of(), Set.of(), Map.of(), Set.of("landmark_0"), Set.of()));
    }

    private static void failedReplacementPreservesDestination(Path directory) throws IOException {
        Path destination = Files.createDirectory(directory.resolve("blocked.properties"));
        Path marker = destination.resolve("keep.txt");
        Files.writeString(marker, "existing data");
        long before;
        try (var files = Files.list(directory)) { before = files.count(); }
        boolean rejected = false;
        try { CampaignSave.save(destination, progress()); }
        catch (IOException expected) { rejected = true; }
        assert rejected : "failed atomic replace must report error";
        assert Files.readString(marker).equals("existing data");
        try (var files = Files.list(directory)) {
            assert files.count() == before : "failed save must clean only its temporary file";
        }
    }

    private static void rejects(Runnable action) {
        boolean rejected = false;
        try { action.run(); }
        catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected : "invalid progress must fail at save boundary";
    }
}
