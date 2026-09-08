import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class DebugSafetyTest {
    public static void main(String[] args) throws Exception {
        legacyRejectsCheats();
        titleRejectsCheats();
        dialogueRejectsCheats();
        deathRejectsCheats();
        diskRemainsPristine();
        cachedContinueRemainsPristine();
        System.out.println("DebugSafetyTest passed");
    }

    static RuinedOutpostGame fresh() {
        var game = RuinedOutpostGame.campaign();
        game.begin();
        return game;
    }

    static void legacyRejectsCheats() {
        var game = new RuinedOutpostGame();
        game.begin();
        denied(game);
    }

    static void titleRejectsCheats() { denied(RuinedOutpostGame.campaign()); }

    static void dialogueRejectsCheats() {
        var game = fresh();
        var hub = game.campaignArea().hub();
        game.player().relocate(hub.x(), hub.y());
        assert game.interact() && game.campaignStory().dialogueOpen();
        denied(game);
    }

    static void deathRejectsCheats() {
        var game = fresh();
        assert game.player().hurt(1_000);
        game.update(.01, 0, 0);
        assert game.story().phase() == OutpostStory.Phase.DEAD;
        denied(game);
    }

    static void endingBoundary(boolean complete) throws Exception {
        Path directory = Files.createTempDirectory("b2bj-debug-boundary-");
        try {
            Path path = directory.resolve("save.properties");
            CampaignSave.save(path, new CampaignSave.Progress(3, 1, Set.of(0, 1, 2, 3),
                    Set.of(), Map.of(), Set.of(), Set.of()));
            var game = RuinedOutpostGame.campaign(path);
            assert game.continueCampaign();
            relocate(game, game.campaignArea().exit());
            assert game.interact() && game.choosingEnding();
            if (complete) assert game.chooseEnding(CampaignStory.Ending.CLOSE_RIFT);
            denied(game);
        } finally { deleteFixture(directory); }
    }

    static void activeAvailability() {
        var game = fresh();
        assert debug(game, "debugAvailable");
        assert !debug(game, "debugSession") : "reading availability cannot taint progress";
    }

    static void pausedAvailability() {
        var game = fresh();
        game.pause();
        assert debug(game, "debugAvailable") && debug(game, "setDebugGodMode", true);
        assert game.paused() && debug(game, "debugGodMode");
    }

    static void invalidWarps() {
        var game = fresh();
        double x = game.player().x(), y = game.player().y();
        assert !debug(game, "debugWarp", -1, false);
        assert !debug(game, "debugWarp", CampaignWorld.AREA_COUNT, true);
        assert !debug(game, "debugSession") && game.biome() == 0;
        assert game.player().x() == x && game.player().y() == y;
    }

    static void godHealth() {
        var game = fresh();
        assert game.player().hurt(1);
        double health = game.player().healthValue();
        assert debug(game, "setDebugGodMode", true);
        game.update(.5, 0, 0); game.update(.3, 0, 0);
        assert !game.player().hurt(1_000) && game.player().healthValue() == health;
    }

    static void godIchorPenalty() {
        var game = fresh();
        game.player().collectIchor(100); assert game.transform();
        assert debug(game, "setDebugGodMode", true);
        double ichor = game.player().ichor(), health = game.player().healthValue();
        assert !game.player().hurt(2);
        assert game.player().ichor() == ichor && game.player().healthValue() == health;
    }

    static void godNaturalDrain() {
        var game = fresh();
        assert debug(game, "setDebugGodMode", true);
        game.player().collectIchor(100); assert game.transform();
        game.update(.4, 0, 0);
        assert game.player().ichor() < 100 && game.player().bladeSeconds() < game.player().bladeDuration()
                : "god mode must not silently grant infinite transformation time";
    }

    static void godCanBeDisabled() {
        var game = fresh();
        assert debug(game, "setDebugGodMode", true) && !game.player().hurt(1);
        assert debug(game, "setDebugGodMode", false) && game.player().hurt(1);
    }

    static void oneShotCanBeDisabled() {
        var game = knightGame();
        Wisp enemy = knight(game); position(game, enemy.x(), enemy.y(), 110);
        int health = enemy.health();
        assert game.attack(1, 0); game.update(.1, 0, 0);
        assert !game.projectiles().isEmpty();
        assert debug(game, "setDebugOneShot", false);
        game.update(.25, 0, 0);
        assert enemy.health() == health : "disabled one-shot must restore the raised shield";
    }

    static void refillHealthAndIchor() {
        var game = fresh(); game.player().hurt(2); game.player().collectIchor(17);
        assert debug(game, "debugRefill");
        assert game.player().healthValue() == game.player().maxHealth() && game.player().ichor() == 100;
        assert debug(game, "debugSession");
    }

    static void refillCooldowns() {
        var game = fresh(); assert game.tideWave(1, 0); game.update(.5, 0, 0);
        game.player().collectIchor(100); assert game.transform();
        assert game.ichorCrescent(1, 0); game.update(.4, 0, 0);
        assert game.riposte(); assert game.dash(1, 0); game.pause();
        assert debug(game, "debugRefill");
        assert game.tideCooldown() == 0 && game.crescentCooldown() == 0 && game.riposteCooldown() == 0;
        assert game.player().dashCooldown() == 0 && !game.player().dashing() && !game.guarding();
        game.togglePause(); assert game.attack(1, 0) : "refill must clear player attack cooldown";
    }

    static void addsTwentyFiveShards() {
        var game = fresh(); int before = game.shards();
        assert debug(game, "debugAddShards") && game.shards() == before + 25;
    }

    static void stickyAfterTogglesOff() {
        var game = fresh();
        assert debug(game, "setDebugGodMode", true) && debug(game, "setDebugOneShot", true);
        assert debug(game, "setDebugGodMode", false) && debug(game, "setDebugOneShot", false);
        assert debug(game, "debugSession") && !debug(game, "debugGodMode") && !debug(game, "debugOneShot");
    }

    static void diskRemainsPristine() throws Exception {
        Path directory = Files.createTempDirectory("b2bj-debug-disk-");
        try {
            Path save = directory.resolve("save.properties");
            CampaignSave.save(save, normalProgress());
            byte[] pristine = Files.readAllBytes(save);
            var game = RuinedOutpostGame.campaign(save); assert game.continueCampaign();
            assert debug(game, "setDebugGodMode", true) && debug(game, "debugAddShards");
            assert game.buyUpgrade("efficiency");
            var memory = game.campaignArea().landmarks().stream().filter(CampaignWorld.Landmark::optional)
                    .findFirst().orElseThrow();
            game.player().relocate(memory.x(), memory.y()); assert game.interact(); closeDialogue(game);
            relocate(game, game.campaignArea().hub()); assert game.interact(); closeDialogue(game);
            assert debug(game, "setDebugGodMode", false);
            game.saveCheckpoint();
            assert Arrays.equals(pristine, Files.readAllBytes(save))
                    : "turning cheats off must not permit checkpoint/quest/upgrade save writes";
            assert debug(game, "setDebugGodMode", true) && debug(game, "setDebugOneShot", true);
            assert debug(game, "debugWarp", 3, true);
            killBoss(game, false);
            relocate(game, game.campaignArea().exit()); assert game.interact();
            assert game.chooseEnding(CampaignStory.Ending.HUMAN_FORM);
            assert Arrays.equals(pristine, Files.readAllBytes(save))
                    : "warped boss clears and debug endings cannot overwrite normal progress";
            game.restart(); assert game.continueCampaign();
            assertNormalProgress(game);
        } finally { deleteFixture(directory); }
    }

    static void cachedContinueRemainsPristine() {
        var game = fresh(); relocate(game, game.campaignArea().hub()); assert game.saveCheckpoint();
        assert debug(game, "debugAddShards") && debug(game, "debugWarp", 2, false);
        assert game.buyUpgrade("vitality"); game.saveCheckpoint(); game.pause();
        assert game.returnToTitle() && game.continueCampaign();
        assert game.biome() == 0 && game.shards() == 0 && game.upgradeLevel("vitality") == 0;
        assert game.campaignCheckpoint() == 1 && game.nearCampaignHub() && !game.biomeUnlocked(1);
        assert !debug(game, "debugSession") && !debug(game, "debugGodMode") && !debug(game, "debugOneShot");
    }

    static void warp(int biome, boolean boss) {
        var game = fresh(); game.pause();
        assert debug(game, "debugWarp", biome, boss) && game.paused();
        assert game.biome() == biome && debug(game, "debugSession");
        assert !game.map().isBlocked(game.player().x(), game.player().y() + Player.COLLISION_Y_OFFSET,
                Player.COLLISION_RADIUS) : "warp must use a valid foot position";
        for (int earlier = 0; earlier <= biome; earlier++) assert game.biomeUnlocked(earlier);
        if (biome < 3) assert !game.biomeUnlocked(biome + 1) : "warp cannot pre-clear its target boss";
        if (boss) {
            assert Math.hypot(game.player().x() - game.guardian().x(),
                    game.player().y() - game.guardian().y()) <= 600;
            game.togglePause(); game.update(.1, 0, 0); assert game.bossActive();
        } else assert game.nearCampaignHub();
    }

    static void resetEncounter() {
        var game = fresh(); assert debug(game, "setDebugOneShot", true);
        assert debug(game, "debugWarp", 1, true); killBoss(game, false);
        game.pause(); assert debug(game, "debugResetEncounter");
        assert game.paused() && game.biome() == 1 && game.guardian().alive();
        assert game.guardian().health() == game.guardian().maxHealth();
        assert game.scouts().stream().allMatch(w -> w.alive() && w.health() == w.maxHealth());
        assert debug(game, "debugOneShot") && debug(game, "debugSession");
    }

    static void travelInSandbox() {
        var game = fresh(); assert debug(game, "setDebugGodMode", true);
        assert debug(game, "setDebugOneShot", true) && debug(game, "debugWarp", 0, true);
        killBoss(game, false); relocate(game, game.campaignArea().exit()); assert game.interact();
        assert game.biome() == 1 && debug(game, "debugSession") && debug(game, "debugGodMode");
        assert !game.player().hurt(1_000);
    }

    enum Weapon { WATER, TIDE, DASH, BLADE, CRESCENT, RIPOSTE }

    static void knightWeapon(Weapon weapon) {
        var game = knightGame(); Wisp target = knight(game);
        position(game, target.x(), target.y(), weapon == Weapon.DASH ? 70 : 100);
        if (weapon == Weapon.BLADE || weapon == Weapon.CRESCENT || weapon == Weapon.RIPOSTE) {
            game.player().collectIchor(100); assert game.transform();
        }
        switch (weapon) {
            case WATER, BLADE -> { assert game.attack(1, 0); }
            case TIDE -> { assert game.tideWave(1, 0); }
            case DASH -> { assert game.dash(1, 0); }
            case CRESCENT -> { assert game.ichorCrescent(1, 0); }
            case RIPOSTE -> {
                assert debug(game, "setDebugGodMode", true);
                game.update(.01, 0, 0);
                for (int i = 0; i < 100 && (target.state() != Wisp.State.TELEGRAPH
                        || target.stateSeconds() < target.telegraphDuration() - .12); i++) game.update(.01, 0, 0);
                assert target.state() == Wisp.State.TELEGRAPH;
                assert game.riposte();
            }
        }
        game.update(.4, 0, 0);
        assert !target.alive() : weapon + " must deliver one real lethal hit through the knight shield";
        assert game.drainEvents().stream().filter(e -> e.type() == RuinedOutpostGame.EventType.ENEMY_DEFEATED
                && Math.hypot(e.x() - target.x(), e.y() - target.y()) < 1).count() == 1;
    }

    static void awakeBoss(boolean dash) {
        var game = fresh(); assert debug(game, "setDebugOneShot", true);
        assert debug(game, "debugWarp", 0, true); killBoss(game, dash);
    }

    static void killBoss(RuinedOutpostGame game, boolean dash) {
        var boss = game.guardian(); position(game, boss.x(), boss.y(), dash ? 130 : 200);
        game.update(.1, 0, 0);
        assert game.bossActive() && boss.state() == Guardian.State.TELEGRAPH;
        if (dash) assert game.dash(1, 0); else assert game.attack(1, 0);
        game.update(.5, 0, 0);
        assert !boss.alive() : "connected one-shot must defeat an awakened armored boss";
    }

    static void dormantBossUntouched() {
        var game = fresh(); assert debug(game, "setDebugOneShot", true);
        int health = game.guardian().health();
        assert game.attack(1, 0); game.update(.5, 0, 0); game.update(.5, 0, 0);
        assert game.guardian().state() == Guardian.State.DORMANT && game.guardian().health() == health;
    }

    static void wallBlocksShot() {
        var game = knightGame(); var enemy = knight(game);
        var wall = game.map().barriers().stream().filter(o -> o.width() < 300 && o.height() >= 128)
                .findFirst().orElseThrow();
        setPosition(enemy, wall.centerX() + wall.width() / 2 + 45, wall.centerY() - Wisp.COLLISION_Y_OFFSET);
        game.player().relocate(wall.centerX() - wall.width() / 2 - 45, enemy.y());
        assert !game.map().isBlocked(game.player().x(), game.player().y() + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS);
        assert !game.map().isBlocked(enemy.x(), enemy.y() + Wisp.COLLISION_Y_OFFSET, Wisp.COLLISION_RADIUS);
        int health = enemy.health(); assert game.attack(1, 0);
        game.update(.5, 0, 0); game.update(.5, 0, 0);
        assert enemy.health() == health : "one-shot must not bypass actual terrain";
    }

    static void wrongFacingCannotHit() {
        var game = knightGame(); var enemy = knight(game); position(game, enemy.x(), enemy.y(), 110);
        game.player().collectIchor(100); assert game.transform(); int health = enemy.health();
        assert game.attack(-1, 0); game.update(.15, 0, 0);
        assert enemy.health() == health;
    }

    static void beyondRangeCannotHit() {
        var game = knightGame(); var enemy = knight(game); position(game, enemy.x(), enemy.y(), 800);
        int health = enemy.health(); assert game.attack(1, 0);
        game.update(.5, 0, 0); game.update(.5, 0, 0);
        assert enemy.health() == health;
    }

    static void dashCancelsPendingStrike() {
        var game = knightGame(); var enemy = knight(game); position(game, enemy.x(), enemy.y(), 200);
        int health = enemy.health(); assert game.attack(1, 0); assert game.dash(-1, 0);
        game.update(.4, 0, 0);
        assert enemy.health() == health && game.projectiles().isEmpty();
    }

    static void pauseFreezesPendingStrike() {
        var game = knightGame(); var enemy = knight(game); position(game, enemy.x(), enemy.y(), 110);
        int health = enemy.health(); assert game.attack(1, 0); game.pause(); game.update(.5, 0, 0);
        assert enemy.health() == health && game.projectiles().isEmpty();
        game.togglePause(); game.update(.4, 0, 0); assert !enemy.alive();
    }

    static void reversionCancelsPendingBlade() {
        var game = knightGame(); var enemy = knight(game); position(game, enemy.x(), enemy.y(), 110);
        game.player().collectIchor(100); assert game.transform();
        game.player().spendBladeIchor(99.9); int health = enemy.health();
        assert game.attack(1, 0); game.update(.15, 0, 0);
        assert game.player().recovering() && enemy.health() == health;
    }

    static void deathCancelsPendingStrike() {
        var game = knightGame(); var enemy = knight(game); position(game, enemy.x(), enemy.y(), 110);
        int health = enemy.health(); assert game.attack(1, 0); assert game.player().hurt(1_000);
        game.update(.4, 0, 0);
        assert game.story().phase() == OutpostStory.Phase.DEAD && enemy.health() == health;
        assert game.projectiles().isEmpty() && game.crescents().isEmpty() && !game.guarding();
    }

    static void warpClearsTransientCombat() {
        var game = fresh(); assert game.attack(1, 0); game.update(.12, 0, 0);
        assert !game.projectiles().isEmpty(); assert debug(game, "debugWarp", 1, false);
        assert game.projectiles().isEmpty() && game.crescents().isEmpty() && game.hostileProjectiles().isEmpty();
        game.update(.4, 0, 0); assert game.projectiles().isEmpty();
    }

    static void focusClearsMovement() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            var game = fresh(); var panel = new B2BJ(false, game); panel.setSize(1280, 720);
            panel.getActionMap().get("rightPressed").actionPerformed(new java.awt.event.ActionEvent(panel, 0, ""));
            double x = game.player().x(); panel.step(.1); assert game.player().x() > x;
            for (var listener : panel.getFocusListeners()) listener.focusLost(
                    new java.awt.event.FocusEvent(panel, java.awt.event.FocusEvent.FOCUS_LOST));
            assert game.paused(); game.togglePause(); x = game.player().x(); panel.step(.1);
            assert game.player().x() == x : "focus loss cannot leave held movement behind";
        });
    }

    static void panelCallbacks() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            var game = fresh(); game.pause();
            try {
                var method = Class.forName("DebugPanel").getDeclaredMethod("panel", RuinedOutpostGame.class);
                method.setAccessible(true);
                var panel = (javax.swing.JPanel) method.invoke(null, game);
                assert !debug(game, "debugSession") : "opening menu must be read-only";
                var controls = descendants(panel);
                for (String name : new String[]{"God mode", "One-shot attacks"}) {
                    var box = controls.stream().filter(c -> c instanceof javax.swing.JCheckBox b && b.getText().equals(name))
                            .map(javax.swing.JCheckBox.class::cast).findFirst().orElseThrow();
                    box.doClick();
                }
                assert debug(game, "debugGodMode") && debug(game, "debugOneShot") && game.paused();
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        });
    }

    static void deathRetryStaysSandboxed() {
        var game = fresh(); assert debug(game, "setDebugOneShot", true);
        assert debug(game, "debugWarp", 2, false); assert game.player().hurt(1_000);
        game.update(.1, 0, 0); game.restart();
        assert game.player().alive() && game.biome() == 2 && debug(game, "debugSession")
                && debug(game, "debugOneShot");
        game.pause(); assert game.returnToTitle() && game.continueCampaign();
        assert game.biome() == 0 && !debug(game, "debugSession") && !debug(game, "debugOneShot");
    }

    private static RuinedOutpostGame knightGame() {
        var game = fresh(); assert debug(game, "setDebugOneShot", true);
        assert debug(game, "debugWarp", 2, false); return game;
    }

    private static Wisp knight(RuinedOutpostGame game) {
        return game.scouts().stream().filter(w -> game.campaignEnemy(w).kind() == CampaignEnemy.Kind.FALLEN_KNIGHT)
                .findFirst().orElseThrow();
    }

    private static void position(RuinedOutpostGame game, double x, double y, double distance) {
        game.player().relocate(x - distance, y);
        assert !game.map().isBlocked(game.player().x(), game.player().y() + Player.COLLISION_Y_OFFSET,
                Player.COLLISION_RADIUS) : "invalid attack fixture position";
    }

    private static void setPosition(Wisp enemy, double x, double y) {
        try {
            var xf = Wisp.class.getDeclaredField("x"); var yf = Wisp.class.getDeclaredField("y");
            xf.setAccessible(true); yf.setAccessible(true); xf.setDouble(enemy, x); yf.setDouble(enemy, y);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static CampaignSave.Progress normalProgress() {
        return new CampaignSave.Progress(1, 1, Set.of(0), Set.of(), Map.of("vitality", 1),
                Set.of("talked_0", "memory_0", "quest_0"), Set.of(), 7);
    }

    private static void assertNormalProgress(RuinedOutpostGame game) {
        assert game.biome() == 1 && game.shards() == 7 && game.upgradeLevel("vitality") == 1;
        assert game.upgradeLevel("efficiency") == 0 && !game.biomeUnlocked(2);
        assert game.campaignStory().questFlags().equals(normalProgress().questFlags());
        assert !debug(game, "debugSession") && !debug(game, "debugGodMode") && !debug(game, "debugOneShot");
    }

    private static void relocate(RuinedOutpostGame game, CampaignWorld.Point point) {
        game.player().relocate(point.x(), point.y());
    }

    private static void closeDialogue(RuinedOutpostGame game) {
        if (game.campaignStory().dialogueOpen()) assert game.interact();
    }

    private static java.util.List<java.awt.Component> descendants(java.awt.Container parent) {
        var result = new java.util.ArrayList<java.awt.Component>();
        for (var child : parent.getComponents()) {
            result.add(child);
            if (child instanceof java.awt.Container container) result.addAll(descendants(container));
        }
        return result;
    }

    private static void deleteFixture(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    static void denied(RuinedOutpostGame game) {
        double health = game.player().healthValue(), ichor = game.player().ichor();
        int biome = game.biome(), shards = game.shards();
        assert !debug(game, "debugAvailable");
        assert !debug(game, "setDebugGodMode", true);
        assert !debug(game, "setDebugOneShot", true);
        assert !debug(game, "debugRefill") && !debug(game, "debugAddShards");
        assert !debug(game, "debugWarp", 3, true) && !debug(game, "debugResetEncounter");
        assert !debug(game, "debugSession") && !debug(game, "debugGodMode")
                && !debug(game, "debugOneShot");
        assert game.player().healthValue() == health && game.player().ichor() == ichor;
        assert game.biome() == biome && game.shards() == shards;
    }

    static boolean debug(RuinedOutpostGame game, String method, Object... arguments) {
        Class<?>[] types = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++)
            types[i] = arguments[i] instanceof Boolean ? boolean.class : int.class;
        try { return (boolean) game.getClass().getMethod(method, types).invoke(game, arguments); }
        catch (InvocationTargetException failure) {
            throw new AssertionError("Debug operation threw: " + method, failure.getCause());
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("Debug API missing: " + method, missing);
        }
    }
}
