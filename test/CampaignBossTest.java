import java.util.EnumSet;

public final class CampaignBossTest {
    public static void main(String[] args) {
        profilesHaveDistinctAttackCycles();
        golemHasAReadableDrainAura();
        defaultGuardianRemainsCompatible();
        System.out.println("CampaignBossTest passed");
    }

    private static void profilesHaveDistinctAttackCycles() {
        var forest = attacks(Guardian.Profile.BRIARHEART);
        var catacombs = attacks(Guardian.Profile.OATHKEEPER);
        var citadel = attacks(Guardian.Profile.ICHOR_GOLEM);
        assert forest.contains(Guardian.Attack.SHOCKWAVE) && forest.contains(Guardian.Attack.TARGET);
        assert !forest.contains(Guardian.Attack.CHARGE) : "rooted forest boss controls clearings";
        assert catacombs.contains(Guardian.Attack.CHARGE) && catacombs.contains(Guardian.Attack.FISSURE);
        assert !catacombs.contains(Guardian.Attack.SHOCKWAVE) : "oathkeeper tests lanes and committed rushes";
        assert citadel.contains(Guardian.Attack.SHOCKWAVE) && citadel.contains(Guardian.Attack.FISSURE);
        for (var profile : Guardian.Profile.values()) {
            var boss = new Guardian(500, 500, profile);
            assert boss.maxHealth() > Player.BLADE_DAMAGE * Player.BLADE_DURATION / Player.ATTACK_COOLDOWN;
            assert boss.health() == boss.maxHealth() && !boss.bossName().isBlank();
        }
    }

    private static EnumSet<Guardian.Attack> attacks(Guardian.Profile profile) {
        var boss = new Guardian(500, 500, profile);
        var attacks = EnumSet.noneOf(Guardian.Attack.class);
        boss.activate(800, 500);
        boss.hurt(boss.maxHealth() / 2);
        for (int i = 0; i < 5000; i++) {
            boss.update(.01, boss.x() + 320, boss.y());
            if (boss.state() == Guardian.State.TELEGRAPH) {
                double x = boss.targetX(), y = boss.targetY();
                assert !boss.hits(x, y) : "warning cannot damage";
                boss.update(.01, boss.x() - 320, boss.y());
                assert boss.targetX() == x && boss.targetY() == y : "boss tells never track late";
            }
            if (boss.state() == Guardian.State.SLAM) attacks.add(boss.attack());
        }
        return attacks;
    }

    private static void golemHasAReadableDrainAura() {
        var golem = new Guardian(500, 500, Guardian.Profile.ICHOR_GOLEM);
        assert golem.auraRadius() == 0 && golem.drainPerSecond() == 0 : "dormant boss cannot drain";
        golem.activate(800, 500);
        double radius = golem.auraRadius(), drain = golem.drainPerSecond();
        assert radius >= 200 && radius <= 400 && drain > 0;
        golem.hurt(golem.maxHealth() / 2);
        assert golem.auraRadius() >= radius && golem.drainPerSecond() > drain;
        golem.hurt(golem.maxHealth());
        assert golem.auraRadius() == 0 && golem.drainPerSecond() == 0 : "death stops aura immediately";
        for (var profile : new Guardian.Profile[]{Guardian.Profile.OUTPOST_WARDEN,
                Guardian.Profile.BRIARHEART, Guardian.Profile.OATHKEEPER}) {
            var boss = new Guardian(500, 500, profile);
            boss.activate(800, 500);
            assert boss.auraRadius() == 0 && boss.drainPerSecond() == 0;
        }
    }

    private static void defaultGuardianRemainsCompatible() {
        var guardian = new Guardian(500, 500);
        assert guardian.profile() == Guardian.Profile.OUTPOST_WARDEN;
        assert guardian.maxHealth() == Guardian.MAX_HEALTH;
        guardian.activate(800, 500);
        assert guardian.attack() == Guardian.Attack.TARGET;
        assert guardian.telegraphDuration() == Guardian.TELEGRAPH_DURATION;
    }
}
