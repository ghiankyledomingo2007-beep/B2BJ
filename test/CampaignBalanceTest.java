import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Deterministic controller measurements with test healing, not a human difficulty/playtest claim. */
public final class CampaignBalanceTest {
    private static final double DT = .05;
    private static Path directory;
    private static int fixtureNumber;

    public static void main(String[] args) throws Exception {
        directory = Files.createTempDirectory("b2bj-balance-");
        try {
            golemRecoveryProvidesARealOpening();
            knightShieldAndInterruptPaths();
            bossGuardRejectsEveryOutgoingWeapon();
            golemAuraDrainsOnlyInsideItsBoundary();
            for (int biome = 0; biome < 4; biome++) {
                if (args.length > 0 && args[0].equals("--measure")) {
                    fight(biome, 0, false);
                    fight(biome, 1, false);
                } else fight(biome, 3, true);
            }
            System.out.println("CampaignBalanceTest passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static RuinedOutpostGame fixture(int biome, boolean upgrades) throws Exception {
        return fixture(biome, upgrades ? 3 : 0);
    }

    private static RuinedOutpostGame fixture(int biome, int upgradeTier) throws Exception {
        var previous = new HashSet<Integer>();
        for (int id = 0; id < biome; id++) previous.add(id);
        Path save = directory.resolve("fixture-" + fixtureNumber++ + ".properties");
        CampaignSave.save(save, new CampaignSave.Progress(biome, 0, previous, Set.of(),
                upgradeTier > 0 ? Map.of("vitality", upgradeTier, "capacity", upgradeTier,
                        "efficiency", upgradeTier, "edge", 1) : Map.of(),
                Set.of(), Set.of(), 0));
        var game = RuinedOutpostGame.campaign(save);
        assert game.continueCampaign();
        return game;
    }

    private static void golemRecoveryProvidesARealOpening() {
        for (boolean enraged : new boolean[]{false, true}) {
            var boss = new Guardian(500, 500, Guardian.Profile.ICHOR_GOLEM);
            boss.activate(700, 500);
            if (enraged) boss.hurt(boss.maxHealth() / 2);
            while (boss.state() != Guardian.State.RECOVER) boss.update(.01, 700, 500);
            assert boss.recoverDuration() == (enraged ? 1.8 : 2.1);
            boss.update(1.5, 700, 500);
            assert boss.state() == Guardian.State.RECOVER : "golem needs time to approach and punish after its wave";
            boss.update(boss.recoverDuration() - boss.stateSeconds() + .001, 700, 500);
            assert boss.state() == Guardian.State.APPROACH : "longer opening must still end";
        }
    }

    private static Wisp firstKnight(RuinedOutpostGame game) {
        return game.scouts().stream().filter(body -> game.campaignEnemy(body).kind()
                == CampaignEnemy.Kind.FALLEN_KNIGHT).findFirst().orElseThrow();
    }

    private static void knightShieldAndInterruptPaths() throws Exception {
        for (String weapon : new String[]{"water", "tide", "dash", "blade", "crescent", "riposte"}) {
            var game = fixture(2, false);
            var knight = firstKnight(game);
            game.player().relocate(knight.x() - 100, knight.y());
            if (Set.of("blade", "crescent", "riposte").contains(weapon)) {
                game.player().collectIchor(100); assert game.transform();
            }
            game.update(.01, 0, 0);
            int health = knight.health();
            if (weapon.equals("riposte")) {
                while (knight.stateSeconds() < knight.telegraphDuration() - .12) game.update(.01, 0, 0);
            }
            double playerHealth = game.player().healthValue();
            assert use(game, weapon, 1, 0) : "weapon must start: " + weapon;
            game.update(.3, 0, 0);
            if (Set.of("water", "tide", "dash").contains(weapon)) {
                assert knight.health() == health : "raised shield must block " + weapon;
                assert knight.state() != Wisp.State.HURT : "blocked slime hit cannot interrupt shield";
            } else {
                assert knight.health() < health : "blade path must penetrate shield: " + weapon;
                assert game.player().healthValue() == playerHealth : "interrupted knight cannot trade: " + weapon;
            }
        }
        for (String weapon : new String[]{"water", "tide", "dash"}) {
            var game = fixture(2, false);
            var knight = firstKnight(game);
            game.player().relocate(knight.x() - 100, knight.y());
            while (knight.state() != Wisp.State.RECOVER) { game.player().heal(); game.update(.01, 0, 0); }
            game.player().relocate(knight.x() - 100, knight.y());
            int health = knight.health();
            assert use(game, weapon, 1, 0);
            game.update(.3, 0, 0);
            assert knight.health() < health : "recovery must expose knight to " + weapon;
        }
    }

    private static boolean use(RuinedOutpostGame game, String weapon, int x, int y) {
        return switch (weapon) {
            case "water", "blade" -> game.attack(x, y);
            case "tide" -> game.tideWave(x, y);
            case "dash" -> game.dash(x, y);
            case "crescent" -> game.ichorCrescent(x, y);
            case "riposte" -> game.riposte();
            default -> throw new AssertionError(weapon);
        };
    }

    private static void bossGuardRejectsEveryOutgoingWeapon() throws Exception {
        for (String weapon : new String[]{"water", "tide", "dash", "blade", "crescent", "riposte"}) {
            var game = fixture(2, false);
            var boss = game.guardian();
            game.player().relocate(boss.x() - 110, boss.y());
            if (Set.of("blade", "crescent", "riposte").contains(weapon)) {
                game.player().collectIchor(100); assert game.transform();
            }
            game.update(.01, 0, 0);
            if (weapon.equals("riposte")) {
                while (boss.stateSeconds() < boss.telegraphDuration() - .12) game.update(.01, 0, 0);
            }
            assert use(game, weapon, 1, 0);
            game.update(weapon.equals("riposte") ? .17 : .3, 0, 0);
            assert boss.health() == boss.maxHealth() : "boss guard rejects " + weapon + " outside recovery";
            if (weapon.equals("riposte")) assert game.drainEvents().stream()
                    .anyMatch(event -> event.type() == RuinedOutpostGame.EventType.RIPOSTE_COUNTER)
                    : "guard check must exercise an actual counter, not an expired stance";
        }
    }

    private static void golemAuraDrainsOnlyInsideItsBoundary() throws Exception {
        double[] ichor = new double[2];
        for (int i = 0; i < 2; i++) {
            var game = fixture(3, false);
            var boss = game.guardian();
            placeNearBoss(game, i == 0 ? 200 : 430);
            game.player().collectIchor(100); assert game.transform();
            game.update(.2, 0, 0);
            ichor[i] = game.player().ichor();
            assert game.player().healthValue() == Player.MAX_HEALTH : "aura measurement precedes first impact";
        }
        assert Math.abs((ichor[1] - ichor[0]) - .6) < 1e-6 : "only inside aura pays 3 extra Ichor/second";
    }

    private static void fight(int biome, int upgradeTier, boolean firstWindowOnly) throws Exception {
        var game = fixture(biome, upgradeTier);
        var boss = game.guardian();
        placeNearBoss(game, 430);
        game.player().collectIchor(100); // One earned full starting reservoir; never refill Ichor after this.
        var authored = Set.copyOf(game.scouts());
        Set<Wisp> countedDeaths = new HashSet<>();
        int transforms = 0, firstWindowDamage = -1, addsDefeated = 0;
        double elapsed = 0, bladeTime = 0, incomingDamage = 0;
        boolean previousBlade = false;
        for (int tick = 0; tick < 12_000 && boss.alive(); tick++) {
            incomingDamage += game.player().maxHealth() - game.player().healthValue();
            game.player().heal(); // Keeps this deterministic combat/economy measurement alive.
            if (!game.player().bladeForm() && previousBlade && firstWindowDamage < 0) {
                firstWindowDamage = boss.maxHealth() - boss.health();
                assert boss.alive() : "one transformation defeated " + boss.bossName();
                if (firstWindowOnly) break;
            }
            if (boss.state() == Guardian.State.RECOVER && game.transform()) transforms++;
            previousBlade = game.player().bladeForm();
            if (previousBlade) bladeTime += DT;
            act(game);
            game.update(DT, movementX, movementY);
            elapsed += DT;
            for (var event : game.drainEvents()) if (event.type() == RuinedOutpostGame.EventType.ENEMY_DEFEATED)
                for (var enemy : game.scouts()) if (!enemy.alive() && !countedDeaths.contains(enemy)
                        && Math.hypot(event.x() - enemy.x(), event.y() - enemy.y()) < 1) {
                    countedDeaths.add(enemy);
                    if (!authored.contains(enemy)) addsDefeated++;
                    break;
                }
            assert game.player().alive() && !game.blocked() : "measurement unexpectedly lost control";
        }
        System.out.printf(java.util.Locale.ROOT,
                "Balance %s%s: %.1fs, transforms=%d, first-window=%d/%d, blade=%.1fs, adds=%d, incoming=%.1f, remaining=%d%n",
                boss.bossName(), upgradeTier == 3 ? " [max upgrades]" : upgradeTier == 1 ? " [one upgrade/track]" : " [base]", elapsed, transforms,
                firstWindowDamage, boss.maxHealth(), bladeTime, addsDefeated, incomingDamage, boss.health());
        assert firstWindowDamage > 0 && firstWindowDamage < boss.maxHealth()
                : "window check needs actual damage while leaving the boss alive";
        if (!firstWindowOnly) assert !boss.alive() : "finite natural Ichor strategy did not finish " + boss.bossName();
    }

    private static int movementX, movementY;

    private static void placeNearBoss(RuinedOutpostGame game, double radius) {
        for (int i = 0; i < 16; i++) {
            double x = game.guardian().x() + Math.cos(i * Math.PI / 8) * radius;
            double y = game.guardian().y() + Math.sin(i * Math.PI / 8) * radius;
            if (!game.map().isBlocked(x, y + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS)) {
                game.player().relocate(x, y); return;
            }
        }
        throw new AssertionError("Boss approach has no collision-free fixture position");
    }

    private static void act(RuinedOutpostGame game) {
        var player = game.player(); var boss = game.guardian();
        double dx = player.x() - boss.x(), dy = player.y() - boss.y();
        double length = Math.max(1, Math.hypot(dx, dy));
        double retreat = boss.attack() == Guardian.Attack.SHOCKWAVE && boss.state() == Guardian.State.SLAM ? 470 : 270;
        double targetX = boss.x() + dx / length * retreat, targetY = boss.y() + dy / length * retreat;
        Wisp add = game.scouts().stream().filter(Wisp::alive)
                .filter(enemy -> Math.hypot(enemy.x() - player.x(), enemy.y() - player.y()) < 600)
                .min(Comparator.comparingDouble(enemy -> Math.hypot(enemy.x() - player.x(), enemy.y() - player.y())))
                .orElse(null);
        boolean hitBoss = boss.state() == Guardian.State.RECOVER && (player.bladeForm() || add == null);
        double attackX = hitBoss ? boss.x() : add == null ? targetX : add.x();
        double attackY = hitBoss ? boss.y() : add == null ? targetY : add.y();
        if (hitBoss || add != null) {
            double ax = player.x() - attackX, ay = player.y() - attackY;
            double distance = Math.max(1, Math.hypot(ax, ay));
            double reach = player.bladeForm() ? 112 : 180;
            targetX = attackX + ax / distance * reach; targetY = attackY + ay / distance * reach;
            if (distance < (player.bladeForm() ? 148 : 320)) {
                int aimX = (int) Math.round(-ax * 100), aimY = (int) Math.round(-ay * 100);
                if (!player.bladeForm() && add != null && game.tideCooldown() == 0) game.tideWave(aimX, aimY);
                else game.attack(aimX, aimY);
            }
        }
        if (!hitBoss && player.ichor() < 100 && !game.drops().isEmpty()) {
            var drop = game.drops().stream().min(Comparator.comparingDouble(value ->
                    Math.hypot(value.x() - player.x(), value.y() - player.y()))).orElseThrow();
            if (Math.hypot(drop.x() - player.x(), drop.y() - player.y()) < 650) {
                targetX = drop.x(); targetY = drop.y();
            }
        }
        movementX = (int) Math.round((targetX - player.x()) * 100);
        movementY = (int) Math.round((targetY - player.y()) * 100);
        if (Math.hypot(targetX - player.x(), targetY - player.y()) < 10) movementX = movementY = 0;
        if (boss.state() == Guardian.State.TELEGRAPH && boss.stateSeconds() > boss.telegraphDuration() - .18) {
            if (boss.attack() == Guardian.Attack.CHARGE)
                game.dash((int) Math.round(-boss.directionY() * 1000), (int) Math.round(boss.directionX() * 1000));
            else if (Math.hypot(player.x() - boss.targetX(), player.y() - boss.targetY()) < boss.slamRadius() + 35)
                game.dash(movementX == 0 && movementY == 0 ? 1 : movementX, movementY);
        }
    }
}
