public final class FullCampaignTest {
    public static void main(String[] args) throws Exception {
        freshCampaignStartsAtTheOutpost();
        System.out.println("FullCampaignTest passed");
    }

    private static void freshCampaignStartsAtTheOutpost() {
        var game = RuinedOutpostGame.campaign();
        assert game.campaignMode();
        assert game.biome() == 0 && game.campaignArea().id() == 0;
        assert game.story().phase() == OutpostStory.Phase.PROLOGUE;
        assert !game.chooseEnding(CampaignStory.Ending.CLOSE_RIFT)
                : "ending cannot bypass the complete campaign";
        assert game.biomeUnlocked(0) && !game.biomeUnlocked(1);
        game.begin();
        assert !game.story().blocksGameplay();
        assert !game.map().isBlocked(game.player().x(),
                game.player().y() + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS);
    }
}
