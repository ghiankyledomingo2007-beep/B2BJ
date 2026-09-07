import java.nio.file.Files;
import java.nio.file.Path;

public final class FullCampaignTest {
    public static void main(String[] args) throws Exception {
        freshCampaignStartsAtTheOutpost();
        pausedTitleRoundtripPreservesProgress();
        completeRouteRetainsProgressAndOffersBothEndings();
        System.out.println("FullCampaignTest passed");
    }

    private static void pausedTitleRoundtripPreservesProgress() {
        var game = RuinedOutpostGame.campaign();
        game.begin();
        optionalQuestAndUpgrade(game);
        relocate(game, game.campaignArea().hub());
        assert game.saveCheckpoint();
        int shards = game.shards();
        var flags = game.campaignStory().questFlags();
        assert !game.returnToTitle() : "active play cannot accidentally leave for the title";
        game.pause();
        assert game.returnToTitle() : "pause menu must allow a safe return to title";
        assert game.story().phase() == OutpostStory.Phase.PROLOGUE && !game.paused();
        assert game.continueCampaign();
        assert !game.blocked() && game.nearCampaignHub() && game.campaignCheckpoint() == 1;
        assert game.shards() == shards && game.upgradeLevel("vitality") == 1;
        assert game.campaignStory().questFlags().equals(flags);
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

    private static void completeRouteRetainsProgressAndOffersBothEndings() throws Exception {
        Path directory = Files.createTempDirectory("b2bj-full-campaign-");
        try {
            malformedSaveRequiresExplicitNewCampaign(directory);
            Path save = directory.resolve("campaign.properties");
            var game = RuinedOutpostGame.campaign(save);
            assert !game.continueCampaign() : "missing save cannot invent progress";
            game.begin();
            for (int biome = 0; biome < CampaignWorld.AREA_COUNT; biome++) {
                assert game.biome() == biome : "main route skipped a biome";
                assert !game.chooseEnding(CampaignStory.Ending.HUMAN_FORM);
                var area = game.campaignArea();
                assert area.width() > 1280 && area.height() > 720;

                relocate(game, area.exit());
                game.interact();
                assert game.biome() == biome && !game.choosingEnding()
                        : "locked boss exit cannot skip its encounter";

                if (biome == 0) optionalQuestAndUpgrade(game);
                relocate(game, area.hub());
                assert game.saveCheckpoint() : "authored camp must save";
                assert game.campaignCheckpoint() == 1;
                assert Files.exists(save);
                game = reloadAndDieAtCheckpoint(save, game);
                if (biome == 2) shieldBlocksWaterInTheGameLoop(game);

                // Fixture supplies health/Ichor and positions actors. Every defeated enemy
                // still requires actual game attacks; this verifies progression, not difficulty.
                clearEnemies(game);
                defeatBoss(game);
                assert !game.guardian().alive();
                if (biome + 1 < CampaignWorld.AREA_COUNT)
                    assert game.biomeUnlocked(biome + 1) : "boss must unlock the next region";

                relocate(game, area.hub());
                assert game.saveCheckpoint();
                var saved = CampaignSave.load(save).orElseThrow();
                assert saved.clearedBosses().contains(biome)
                        : "real final strike must reach durable boss bookkeeping";

                if (biome + 1 < CampaignWorld.AREA_COUNT) {
                    relocate(game, area.exit());
                    assert game.interact() : "defeated boss exit must accept travel";
                    closeDialogue(game);
                    assert game.biome() == biome + 1;

                    // Returning through the arrival portal must preserve the prior boss kill.
                    relocate(game, game.campaignArea().returnPortal());
                    assert game.interact();
                    closeDialogue(game);
                    assert game.biome() == biome;
                    assert !game.guardian().alive() : "backtracking cannot resurrect a boss";
                    relocate(game, game.campaignArea().exit());
                    assert game.interact();
                    closeDialogue(game);
                    assert game.biome() == biome + 1;
                }
            }

            // Preserve the same completed route before either explicit ending choice.
            Path humanSave = directory.resolve("human.properties");
            Files.copy(save, humanSave);
            finish(game, CampaignStory.Ending.CLOSE_RIFT);
            var human = RuinedOutpostGame.campaign(humanSave);
            assert human.continueCampaign();
            finish(human, CampaignStory.Ending.HUMAN_FORM);
            assert CampaignSave.load(save).orElseThrow().endings().contains("CLOSE_RIFT");
            assert CampaignSave.load(humanSave).orElseThrow().endings().contains("HUMAN_FORM");

            human.restart();
            assert human.story().phase() == OutpostStory.Phase.PROLOGUE;
            human.begin();
            assert human.story().phase() == OutpostStory.Phase.PROLOGUE
                    : "casual title input must not overwrite an existing save";
            human.newCampaign();
            assert human.biome() == 0 && human.shards() == 0;
            assert human.upgradeLevel("vitality") == 0 && !human.biomeUnlocked(1)
                    : "new campaign must not inherit completed-run progress";
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.delete(path);
            }
        }
    }

    private static void malformedSaveRequiresExplicitNewCampaign(Path directory) throws Exception {
        Path save = directory.resolve("malformed.properties");
        String original = "broken campaign save\n";
        Files.writeString(save, original);
        var game = RuinedOutpostGame.campaign(save);
        assert !game.saveAvailable() && !game.continueCampaign();
        game.begin();
        assert game.story().phase() == OutpostStory.Phase.PROLOGUE;
        assert Files.readString(save).equals(original) : "unreadable save must remain untouched";
        game.newCampaign();
        assert !game.story().blocksGameplay();
        assert CampaignSave.load(save).orElseThrow().biome() == 0
                : "explicit New Game may replace the selected corrupt save";
    }

    private static void shieldBlocksWaterInTheGameLoop(RuinedOutpostGame game) {
        var knight = game.scouts().stream()
                .filter(body -> game.campaignEnemy(body).kind() == CampaignEnemy.Kind.FALLEN_KNIGHT)
                .findFirst().orElseThrow();
        assert !game.player().bladeForm();
        positionForAttack(game, knight.x(), knight.y());
        int health = knight.health();
        attackAt(game, knight.x(), knight.y());
        game.update(.4, 0, 0);
        assert knight.health() == health : "game water path must honor the raised knight shield";
    }

    private static void optionalQuestAndUpgrade(RuinedOutpostGame game) {
        var memory = game.campaignArea().landmarks().stream()
                .filter(CampaignWorld.Landmark::optional).findFirst().orElseThrow();
        game.player().relocate(memory.x(), memory.y());
        assert game.interact() : "optional memory must be discoverable before meeting its NPC";
        closeDialogue(game);
        relocate(game, game.campaignArea().hub());
        assert game.interact();
        assert game.campaignStory().dialogueOpen();
        double x = game.player().x(), y = game.player().y();
        game.update(.3, 1, 1);
        assert game.player().x() == x && game.player().y() == y
                : "story conversation must pause combat movement";
        assert !game.attack(1, 0) && !game.dash(1, 0);
        closeDialogue(game);
        assert game.shards() >= 3 : "memory and quest turn-in grant their one-time rewards";
        int shards = game.shards();
        assert game.interact();
        closeDialogue(game);
        assert game.shards() == shards : "repeat NPC dialogue cannot farm quest currency";
        assert !game.buyUpgrade(null) : "missing shop key must be rejected without throwing";
        assert !game.buyUpgrade("unknown") : "unknown shop key must be rejected";
        int cost = game.upgradeCost("vitality");
        assert game.buyUpgrade("vitality");
        assert game.upgradeLevel("vitality") == 1 && game.shards() == shards - cost;
        assert !game.buyUpgrade("edge") : "unaffordable upgrade cannot grant itself";
        relocate(game, game.campaignArea().spawn());
        assert !game.buyUpgrade("vitality") : "upgrades require the actual shop location";
    }

    private static RuinedOutpostGame reloadAndDieAtCheckpoint(Path save,
            RuinedOutpostGame previous) {
        int biome = previous.biome(), shards = previous.shards();
        var game = RuinedOutpostGame.campaign(save);
        assert game.continueCampaign();
        assert game.biome() == biome && game.campaignCheckpoint() == 1;
        assert game.shards() == shards && game.upgradeLevel("vitality") == 1;
        assert game.nearCampaignHub() && game.player().alive();
        assert game.player().hurt(1_000);
        game.update(.01, 0, 0);
        assert game.story().phase() == OutpostStory.Phase.DEAD;
        game.restart();
        assert game.player().alive() && !game.player().bladeForm() && !game.player().recovering();
        assert game.biome() == biome && game.campaignCheckpoint() == 1 && game.nearCampaignHub();
        assert game.shards() == shards && game.upgradeLevel("vitality") == 1;
        for (int prior = 0; prior < biome; prior++)
            assert game.biomeUnlocked(prior + 1) : "death must retain earlier boss unlocks";
        return game;
    }

    private static void clearEnemies(RuinedOutpostGame game) {
        for (Wisp enemy : java.util.List.copyOf(game.scouts())) {
            for (int ticks = 0; enemy.alive() && ticks < 2_000; ticks++) {
                supplyFixture(game);
                positionForAttack(game, enemy.x(), enemy.y());
                attackAt(game, enemy.x(), enemy.y());
                game.update(.05, 0, 0);
            }
            assert !enemy.alive() : "campaign enemy resisted all valid attacks in biome " + game.biome();
        }
    }

    private static void defeatBoss(RuinedOutpostGame game) {
        var boss = game.guardian();
        positionForAttack(game, boss.x(), boss.y());
        game.update(.1, 0, 0); // Allow the last ordinary enemy's hit-stop to expire.
        assert game.bossActive() : "approaching the authored boss must activate it";
        int initialHealth = boss.health();
        for (int ticks = 0; boss.alive() && ticks < 20_000; ticks++) {
            supplyFixture(game);
            positionForAttack(game, boss.x(), boss.y());
            attackAt(game, boss.x(), boss.y());
            game.update(.05, 0, 0);
            assert game.player().alive() : "combat fixture unexpectedly died";
        }
        assert initialHealth > 0 && !boss.alive()
                : "valid combat never defeated boss in biome " + game.biome();
        assert !game.bossActive();
        int impactNumber = boss.impactNumber();
        game.update(.4, 0, 0);
        game.update(.4, 0, 0);
        assert !boss.visible() : "campaign death animation must retire after the real boss kill";
        assert boss.impactNumber() == impactNumber
                : "advancing boss death cannot restart combat";
    }

    private static void supplyFixture(RuinedOutpostGame game) {
        game.player().heal();
        game.player().collectIchor(Player.MAX_ICHOR);
        if (!game.player().bladeForm() && !game.player().recovering()) game.transform();
    }

    private static void positionForAttack(RuinedOutpostGame game, double tx, double ty) {
        for (int angle = 0; angle < 16; angle++) {
            double x = tx + Math.cos(angle * Math.PI / 8) * 110;
            double y = ty + Math.sin(angle * Math.PI / 8) * 110;
            if (!game.map().isBlocked(x, y + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS)
                    && game.map().clearLine(x, y, tx, ty)) {
                game.player().relocate(x, y);
                return;
            }
        }
        throw new AssertionError("No collision-free attack position for target " + tx + "," + ty);
    }

    private static void attackAt(RuinedOutpostGame game, double tx, double ty) {
        game.attack((int) Math.round((tx - game.player().x()) * 100),
                (int) Math.round((ty - game.player().y()) * 100));
    }

    private static void finish(RuinedOutpostGame game, CampaignStory.Ending ending) {
        assert game.biome() == CampaignWorld.AREA_COUNT - 1;
        assert !game.guardian().alive();
        if (game.player().bladeForm()) game.player().spendBladeIchor(game.player().ichor());
        assert game.player().ichor() == 0 : "ending fixture uses no remaining combat Ichor";
        relocate(game, game.campaignArea().exit());
        assert game.interact();
        closeDialogue(game);
        assert game.choosingEnding() : "final rift must offer an explicit choice";
        assert game.chooseEnding(ending);
        assert game.campaignEnding() == ending;
        assert game.story().phase() == OutpostStory.Phase.COMPLETE;
        assert !game.attack(1, 0) && !game.dash(1, 0);
        assert !game.chooseEnding(ending) : "ending cannot commit twice";
    }

    private static void relocate(RuinedOutpostGame game, CampaignWorld.Point point) {
        game.player().relocate(point.x(), point.y());
    }

    private static void closeDialogue(RuinedOutpostGame game) {
        for (int lines = 0; game.campaignStory().dialogueOpen() && lines < 30; lines++)
            assert game.interact() : "E must advance every open dialogue line";
        assert !game.campaignStory().dialogueOpen() : "dialogue never returned control";
    }
}
