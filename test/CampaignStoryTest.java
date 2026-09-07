import java.util.Set;

public final class CampaignStoryTest {
    public static void main(String[] args) {
        completesEachQuestInEitherOrder();
        restoresProgressWithoutRepeatingRewards();
        rejectsInvalidStateWithoutLosingProgress();
        providesReadableCampaignAndEndingText();
        System.out.println("CampaignStoryTest passed");
    }

    private static void completesEachQuestInEitherOrder() {
        for (int biome = 0; biome < 4; biome++) {
            CampaignStory story = new CampaignStory();
            assert !story.dialogueOpen();
            assert story.objective(biome, false).contains(CampaignStory.npcName(biome));
            if (biome % 2 == 0) {
                assert !story.talk(biome) : "meeting alone must not award a quest";
                assert story.dialogueTitle().equals(CampaignStory.npcName(biome));
                checkPanel(story.dialogueLines());
                story.advanceDialogue();
            }
            assert story.discover(biome);
            assert story.dialogueOpen();
            checkPanel(story.dialogueLines());
            story.advanceDialogue();
            assert !story.discover(biome) : "memory discovery is idempotent";
            assert !story.dialogueOpen();
            assert story.questObjective(biome).contains(CampaignStory.npcName(biome));
            assert story.talk(biome) : "turn-in works even when clue was found first";
            assert story.questFlags().containsAll(Set.of("talked_" + biome,
                    "memory_" + biome, "quest_" + biome));
            checkPanel(story.dialogueLines());
            story.advanceDialogue();
            assert !story.dialogueOpen();
            assert story.dialogueLines().length == 0;
            assert !story.talk(biome) : "repeat conversations must not duplicate rewards";
            assert story.questObjective(biome).contains("COMPLETE");
            checkPanel(story.dialogueLines());
        }
    }

    private static void restoresProgressWithoutRepeatingRewards() {
        CampaignStory original = new CampaignStory();
        original.discover(0);
        original.talk(0);
        original.discover(1);
        CampaignStory restored = new CampaignStory();
        restored.restoreQuestFlags(original.questFlags());
        assert !restored.dialogueOpen() : "transient dialogue is not saved";
        assert !restored.talk(0);
        assert restored.talk(1) : "unfinished turn-in survives save/load";
        Set<String> snapshot = restored.questFlags();
        restored.talk(2);
        assert !snapshot.contains("talked_2") : "save snapshots must not change later";
        try {
            snapshot.add("memory_3");
            throw new AssertionError("quest flags must be immutable");
        } catch (UnsupportedOperationException expected) { }
        restored.restoreQuestFlags(Set.of("quest_3"));
        assert restored.questFlags().containsAll(Set.of("quest_3", "memory_3", "talked_3"));
        assert !restored.talk(3) : "completed quests restore their prerequisites";
    }

    private static void rejectsInvalidStateWithoutLosingProgress() {
        CampaignStory story = new CampaignStory();
        story.talk(0);
        Set<String> before = story.questFlags();
        for (int invalid : new int[] {-1, 4, Integer.MAX_VALUE}) {
            try {
                story.discover(invalid);
                throw new AssertionError("invalid biome accepted");
            } catch (IllegalArgumentException expected) { }
        }
        try {
            story.restoreQuestFlags(Set.of("quest_4"));
            throw new AssertionError("unknown quest flag accepted");
        } catch (IllegalArgumentException expected) { }
        assert story.questFlags().equals(before) : "failed restore must be atomic";
    }

    private static void providesReadableCampaignAndEndingText() {
        checkPanel(CampaignStory.openingLines());
        assert String.join(" ", CampaignStory.openingLines()).contains("RAINORAY");
        for (int biome = 0; biome < 4; biome++) {
            checkPanel(CampaignStory.transitionLines(biome));
            CampaignStory story = new CampaignStory();
            assert !CampaignStory.questName(biome).isBlank();
            assert !story.objective(biome, true).contains("TALK")
                    : "side quests must not gate a cleared biome";
            story.talk(biome);
            assert story.questObjective(biome).contains(CampaignStory.memoryName(biome))
                    : "quest and map must use the same clue name";
            String[] lines = story.dialogueLines();
            lines[0] = "MUTATED";
            assert !story.dialogueLines()[0].equals("MUTATED");
        }
        for (CampaignStory.Ending ending : CampaignStory.Ending.values()) {
            assert !CampaignStory.endingTitle(ending).isBlank();
            checkPanel(CampaignStory.endingLines(ending));
        }
        assert String.join(" ", CampaignStory.endingLines(CampaignStory.Ending.CLOSE_RIFT))
                .contains("SLIME");
        assert String.join(" ", CampaignStory.endingLines(CampaignStory.Ending.HUMAN_FORM))
                .contains("SEAL");
    }

    private static void checkPanel(String[] lines) {
        assert lines.length > 0 && lines.length <= 4 : "panel must fit four lines";
        for (String line : lines)
            assert !line.isBlank() && line.length() <= 72 : "panel line too wide: " + line;
    }
}
