import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Briarheart raises Briarlings (owner 2026-10-29): warned, capped, killable, leaving a body; none after it falls. */
public final class BriarlingTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var save = Files.createTempFile("b2bj-briarling-", ".properties");
        Set<String> seen = new HashSet<>();
        for (var scene : OutpostDialogue.values()) seen.add(scene.key());
        CampaignSave.save(save, new CampaignSave.Progress(1, 1, Set.of(0), seen, Map.of()));
        var game = RuinedOutpostGame.campaign(save);
        assert game.continueCampaign() && game.biome() == 1;
        var boss = game.guardian();
        assert boss.profile() == Guardian.Profile.BRIARHEART;
        game.player().setGodMode(true);
        for (var enemy : game.scouts()) enemy.hurt(10000);
        game.player().relocate(boss.x(), boss.y() + 460);
        boss.activate(game.player().x(), game.player().y());
        game.drainEvents();

        int warned = 0, emerged = 0, most = 0;
        for (int i = 0; i < 1200; i++) {                                   // 12 s of the fight, from far enough away
            game.update(.01, 0, 0);
            game.player().relocate(boss.x(), boss.y() + 460);
            for (var e : game.drainEvents()) {
                if (e.type() == RuinedOutpostGame.EventType.BRIARLING_SPROUT) warned++;
                if (e.type() == RuinedOutpostGame.EventType.BRIARLING_EMERGE) emerged++;
            }
            most = Math.max(most, (int) (briarlings(game) + game.sprouts().size()));
            if (i == 100) assert game.storyCameraWeight() == 0 : "the giant camera waits out the 3 s name card";
        }
        // The camera then leans toward its heart, but never frames the player more than 170 px below the centre.
        double lean = game.storyCameraWeight(), below = lean * (game.player().y() - game.storyCameraY());
        assert lean > .4 && below <= 170.001 : "giant camera lean " + lean + ", player " + below + " px below centre";
        assert warned >= 1 && emerged == warned : "every Briarling is warned before it breaks the ground: " + warned + "/" + emerged;
        assert most <= 2 : "at most two awake or sprouting before the enrage: " + most;
        for (var s : game.sprouts()) assert Math.hypot(s.x() - game.player().x(), s.y() - game.player().y()) >= 140;

        int before = (int) briarlings(game);
        boss.hurt(boss.health() - boss.maxHealth() * 70 / 100);            // past three quarters of its health
        for (int i = 0; i < 100; i++) game.update(.01, 0, 0);
        assert briarlings(game) + game.sprouts().size() >= Math.min(2, before + 1) : "a quarter of its health calls more";

        var briarling = game.scouts().stream().filter(w -> w.alive() && game.campaignEnemy(w) != null
                && game.campaignEnemy(w).kind() == CampaignEnemy.Kind.BRIARLING).findFirst().orElseThrow();
        assert briarling.health() <= 3 * Player.BLADE_DAMAGE : "light: three Blade hits clear it";
        double x = briarling.x(), y = briarling.y();
        briarling.hurt(briarling.health());
        var hit = RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit", Wisp.class, boolean.class);
        hit.setAccessible(true); hit.invoke(game, briarling, true);
        assert game.corpses().stream().anyMatch(c -> Math.hypot(c.x() - x, c.y() - y) < 1) : "a Briarling leaves its body";
        game.drainEvents();
        game.player().relocate(x, y);                                       // and the slime can swallow it
        assert game.interact() : "a Briarling's body can be absorbed";
        for (int i = 0; i < 90; i++) game.update(.01, 0, 0);
        assert game.drainEvents().stream().anyMatch(e -> e.type() == RuinedOutpostGame.EventType.ABSORBED) : "absorbed";

        boss.hurt(boss.health());
        var fell = RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit", boolean.class);
        fell.setAccessible(true); fell.invoke(game, false);
        for (int i = 0; i < 1500; i++) game.update(.01, 0, 0);
        assert briarlings(game) == 0 && game.sprouts().isEmpty() : "none remain or sprout once Briarheart falls";
        System.out.println("BriarlingTest passed: warned sprouts, cap, health quarters, light absorbable bodies, nothing after the fall");
    }

    private static long briarlings(RuinedOutpostGame game) {
        return game.scouts().stream().filter(w -> w.alive() && game.campaignEnemy(w) != null
                && game.campaignEnemy(w).kind() == CampaignEnemy.Kind.BRIARLING).count();
    }
}
