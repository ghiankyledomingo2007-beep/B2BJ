import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorldAuditTest {
    public static void main(String[] args) throws Exception {
        assert RuinedOutpostMap.campaign(2).waterBlocked(5800,3520,24)
                : "catacomb wall rows must retain their trailing solid spans";
        knightCannotTrackAnAttackThroughACatacombCorner();
        System.out.println("WorldAuditTest passed");
    }

    @SuppressWarnings("unchecked")
    private static void knightCannotTrackAnAttackThroughACatacombCorner() throws Exception {
        var directory=Files.createTempDirectory("b2bj-world-audit-");
        var save=directory.resolve("campaign.properties");
        try {
            CampaignSave.save(save,new CampaignSave.Progress(2,0,Set.of(0,1),Set.of(),
                    Map.of(),Set.of(),Set.of(),0));
            var game=RuinedOutpostGame.campaign(save);
            assert game.continueCampaign();
            var knight=new CampaignEnemy(CampaignEnemy.Kind.FALLEN_KNIGHT,2965,194);
            // Place one enemy beside an actual authored wall; the real game resolves its attack.
            var scouts=RuinedOutpostGame.class.getDeclaredField("scouts");scouts.setAccessible(true);
            ((List<Wisp>)scouts.get(game)).add(knight.body());
            var identities=RuinedOutpostGame.class.getDeclaredField("campaignEnemies");identities.setAccessible(true);
            ((Map<Wisp,CampaignEnemy>)identities.get(game)).put(knight.body(),knight);
            assert !game.map().isBlocked(2965,216,20);
            game.player().relocate(3014,257);
            assert game.map().clearLine(2965,216,3014,281);
            game.update(.01,0,0);
            assert knight.body().state()==Wisp.State.TELEGRAPH;
            game.player().relocate(2904,257);
            assert !game.map().isBlocked(2904,281,24);
            assert !game.map().clearLine(2965,216,2904,281) : "authored wall separates combatants";
            double health=game.player().healthValue();
            for(int tick=0;tick<90;tick++)game.update(.01,0,0);
            assert knight.body().state()==Wisp.State.LUNGE;
            assert game.player().healthValue()==health : "knight attack tracked through solid catacomb corner";
        } finally {
            Files.deleteIfExists(save);
            Files.delete(directory);
        }
    }
}
