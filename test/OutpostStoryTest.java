public final class OutpostStoryTest {
    public static void main(String[] args) {
        tellsACompleteRuinedOutpostStory();
        deathCanRestartTheRun();
        System.out.println("OutpostStoryTest passed");
    }

    private static void tellsACompleteRuinedOutpostStory() {
        OutpostStory story = new OutpostStory();

        assert story.phase() == OutpostStory.Phase.PROLOGUE;
        assert story.blocksGameplay();
        assert story.overlayTitle().equals("RUINED OUTPOST");
        assert String.join(" ", story.overlayLines()).contains("RAINORAY")
                : "prologue must establish the GCD protagonist";

        story.begin();
        assert story.phase() == OutpostStory.Phase.HUNT;
        assert !story.blocksGameplay();
        assert story.objective().contains("GATEHOUSE");

        story.guardianDefeated();
        assert story.phase() == OutpostStory.Phase.HUNT
                : "guardian cannot be skipped";
        for (int defeated = 1; defeated <= OutpostStory.SCOUT_TOTAL; defeated++) {
            story.scoutDefeated();
            assert story.defeatedScouts() == defeated;
        }
        assert story.phase() == OutpostStory.Phase.HUNT;
        story.enterGatehouse();
        assert story.phase() == OutpostStory.Phase.GUARDIAN;
        assert story.objective().contains("WARDEN");

        story.guardianDefeated();
        assert story.phase() == OutpostStory.Phase.ESCAPE;
        assert story.objective().contains("GATE");

        story.escaped();
        assert story.phase() == OutpostStory.Phase.COMPLETE;
        assert story.blocksGameplay();
        assert String.join(" ", story.overlayLines()).contains("FOREST")
                : "ending must point toward the next biome";
    }

    private static void deathCanRestartTheRun() {
        OutpostStory story = new OutpostStory();
        story.begin();
        story.scoutDefeated();
        story.playerDied();

        assert story.phase() == OutpostStory.Phase.DEAD;
        assert story.blocksGameplay();

        story.restart();

        assert story.phase() == OutpostStory.Phase.PROLOGUE;
        assert story.defeatedScouts() == 0;
    }
}
