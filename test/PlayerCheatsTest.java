public final class PlayerCheatsTest {
    public static void main(String[] args) throws Exception {
        Player player = new Player(640, 384);
        assert !player.godMode();
        player.collectIchor(100);
        assert player.transform();
        player.setGodMode(true);
        assert !player.invulnerable() : "god mode must not masquerade as dash/parry invulnerability";
        assert !player.hurt(10) && player.healthValue() == 5 && player.ichor() == 100;
        player.move(0, 0, 1, new RuinedOutpostMap(0));
        assert player.ichor() < 100 : "god mode does not freeze the form timer";
        assert player.startAttack() && player.dash(1, 0);
        player.refillForTesting();
        assert player.bladeForm() && player.ichor() == 100 && !player.dashing();
        assert player.dashCooldown() == 0 && player.startAttack();
        player.setGodMode(false);
        assert player.hurt(1) && player.healthValue() < 5 && player.ichor() == 95;
        player.refillForTesting();
        assert player.healthValue() == 5 && player.ichor() == 100;
        unlimitedBladeAndSaveProtection();
        System.out.println("PlayerCheatsTest passed");
    }

    private static void unlimitedBladeAndSaveProtection() throws Exception {
        var map = new RuinedOutpostMap(0);
        var player = new Player(640, 384);
        assert !player.unlimitedBlade();
        player.setUnlimitedBlade(true);
        assert player.ichor() == 100 && !player.bladeForm() : "Q still controls transformation";
        assert player.transform();
        player.move(0, 0, 60, map);
        assert player.bladeForm() && player.bladeSeconds() == player.bladeDuration();
        for (int i = 0; i < 30; i++) assert player.spendBladeIchor(10);
        assert player.hurt(1) && player.healthValue() < player.maxHealth() : "not invincibility";
        assert player.ichor() == 100 && !player.recovering() : "skills and hits cannot revert form";
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 101})
            assert !player.spendBladeIchor(invalid) : "cheat must preserve cost validation";
        player.setUnlimitedBlade(false);
        player.move(0, 0, 1, map);
        assert player.ichor() < 100 && player.bladeSeconds() < player.bladeDuration();
        player.setUnlimitedBlade(true);
        assert player.ichor() == 100 && player.bladeSeconds() == player.bladeDuration();
        player.setUnlimitedBlade(false);
        assert player.spendBladeIchor(10) && player.ichor() == 90;
        assert player.hurt(1) && player.ichor() == 85 : "damage costs resume";
        player.move(0, 0, 60, map);
        assert !player.bladeForm() && player.recovering() : "normal expiry resumes";

        var save = java.nio.file.Files.createTempDirectory("b2bj-unlimited-").resolve("campaign.properties");
        var game = RuinedOutpostGame.campaign(save);
        assert !game.setDebugUnlimitedBlade(true) && !game.debugSession() : "title cannot enable cheat";
        game.begin();
        byte[] normalSave = java.nio.file.Files.readAllBytes(save);
        assert game.setDebugUnlimitedBlade(false) && !game.debugSession() : "no-op remains normal";
        assert game.setDebugUnlimitedBlade(true) && game.debugSession();
        assert game.player().transform();
        assert game.debugWarp(0, false) && game.debugResetEncounter();
        assert game.debugUnlimitedBlade() && game.player().bladeForm();
        assert game.player().hurt(100);
        game.update(.01, 0, 0);game.restart();
        assert game.debugUnlimitedBlade() && game.player().alive() && game.player().ichor() == 100;
        assert game.setDebugUnlimitedBlade(false) && game.debugSession();
        game.pause();
        assert game.returnToTitle();
        assert java.util.Arrays.equals(normalSave, java.nio.file.Files.readAllBytes(save)) : "disk save protected";
        assert game.continueCampaign() && !game.debugSession() && !game.debugUnlimitedBlade();
        assert game.campaignCheckpoint() == 0 : "normal Continue snapshot protected from test travel";
        assert game.setDebugUnlimitedBlade(true);game.pause();assert game.returnToTitle();
        assert game.continueCampaign() && !game.debugUnlimitedBlade() : "Continue clears enabled cheat";
        assert game.setDebugUnlimitedBlade(true);game.pause();assert game.returnToTitle();game.newCampaign();
        assert !game.debugUnlimitedBlade() && !game.debugSession() : "New Game clears cheat";
    }
}
