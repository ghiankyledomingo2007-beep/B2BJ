import java.nio.file.Files;

/** In the campaign no more than two enemies wind up or strike at once; the rest circle and still take their turns. */
public final class AttackSlotsTest {
    public static void main(String[] args) throws Exception {
        var save = Files.createTempDirectory("b2bj-slots-").resolve("campaign.properties");
        var game = RuinedOutpostGame.campaign(save);
        game.begin();
        game.skipDialogue();
        game.setDebugGodMode(true);
        var boss = game.guardian();
        boss.activate(game.player().x(), game.player().y());
        boss.hurt(boss.maxHealth());
        var defeat = RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit", boolean.class);
        defeat.setAccessible(true);
        defeat.invoke(game, false);
        var exit = game.campaignArea().exit();
        game.player().relocate(exit.x(), exit.y());
        assert game.interact()&&game.dialogue()==OutpostDialogue.GATE;
        game.skipDialogue();
        assert game.interact() && game.biome() == 1 : "into the forest";
        var clearing = game.campaignArea().landmarks().get(2);           // the Imp Clearing: four enemies round one spot
        game.player().relocate(clearing.x(), clearing.y());
        int most = 0, starts = 0;
        java.util.Map<Wisp, Boolean> was = new java.util.HashMap<>();
        java.util.Set<Wisp> tookTurn = new java.util.HashSet<>();
        for (int frame = 0; frame < 60 * 20; frame++) {
            game.update(1 / 60.0, 0, 0);
            int now = 0;
            for (var w : game.scouts()) {
                if (!w.alive()) continue;
                if (w.attacking()) now++;
                if (w.attacking() && !was.getOrDefault(w, false)) { starts++; tookTurn.add(w); }
                was.put(w, w.attacking());
            }
            most = Math.max(most, now);
        }
        assert most <= RuinedOutpostGame.MAX_ATTACKERS : "at most two attackers at once, saw " + most;
        assert most == RuinedOutpostGame.MAX_ATTACKERS : "the slots are used: pressure stays on";
        assert starts >= 8 : "attacks keep coming while the others wait: " + starts;
        assert tookTurn.size() >= 3 : "waiting enemies get their turn too: " + tookTurn.size();
        System.out.println("AttackSlotsTest passed: peak " + most + " attackers, " + starts + " attacks by " + tookTurn.size() + " enemies in 20 s");
    }
}
