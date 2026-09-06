public final class CombatLoopTest {
    private static final double EPSILON = 0.001;

    public static void main(String[] args) {
        ichorUnlocksTimedBladeForm();
        bladeMovesAtEightyPercentSlimeSpeed();
        attacksRespectCooldownAndFormDamage();
        damageUsesInvulnerabilityAndCanKill();
        System.out.println("CombatLoopTest passed");
    }

    private static void ichorUnlocksTimedBladeForm() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(500, 500);

        player.collectIchor(60);
        assert !player.transform() : "partial Ichor must not transform";
        player.collectIchor(60);
        assertClose(Player.MAX_ICHOR, player.ichor());
        assert player.transform() : "full Ichor must transform";
        assert player.bladeForm();

        player.move(0, 0, Player.BLADE_DURATION / 2, map);
        assert player.bladeForm() : "Blade must remain active at half duration";
        assertClose(Player.MAX_ICHOR / 2, player.ichor());

        player.move(0, 0, Player.BLADE_DURATION / 2, map);
        assert !player.bladeForm() : "Blade must revert when Ichor drains";
        assertClose(0, player.ichor());
    }

    private static void bladeMovesAtEightyPercentSlimeSpeed() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player slime = new Player(500, 500);
        Player blade = new Player(500, 500);
        blade.collectIchor(Player.MAX_ICHOR);
        blade.transform();

        slime.move(1, 0, 0.1, map);
        blade.move(1, 0, 0.1, map);

        assertClose(Player.SPEED * 0.1, slime.x() - 500);
        assertClose(Player.SPEED * Player.BLADE_SPEED_MULTIPLIER * 0.1,
                blade.x() - 500);
    }

    private static void attacksRespectCooldownAndFormDamage() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(500, 500);

        assert player.startAttack() : "first attack must start";
        assert player.attackDamage() == Player.SLIME_DAMAGE;
        assert !player.startAttack() : "attack must respect cooldown";

        player.move(0, 0, Player.WATER_COOLDOWN, map);
        assert player.startAttack() : "attack must recover after cooldown";

        player.collectIchor(Player.MAX_ICHOR);
        player.transform();
        assert player.attackDamage() == Player.BLADE_DAMAGE
                : "Blade attack must deal four times Slime damage";
    }

    private static void damageUsesInvulnerabilityAndCanKill() {
        RuinedOutpostMap map = new RuinedOutpostMap();
        Player player = new Player(500, 500);

        assert player.health() == Player.MAX_HEALTH;
        assert player.hurt(2) : "first enemy hit must deal damage";
        assert player.health() == Player.MAX_HEALTH - 2;
        assert player.invulnerable();
        assert !player.hurt(2) : "invulnerability must reject repeated contact damage";

        player.move(0, 0, Player.HURT_INVULNERABILITY, map);
        assert player.hurt(Player.MAX_HEALTH) : "damage must resume after invulnerability";
        assert !player.alive();
        assert player.health() == 0;
        assert !player.hurt(1) : "dead player must ignore further damage";
    }

    private static void assertClose(double expected, double actual) {
        assert Math.abs(expected - actual) < EPSILON
                : "expected " + expected + ", got " + actual;
    }
}
