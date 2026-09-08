public final class PlayerCheatsTest {
    public static void main(String[] args) {
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
        System.out.println("PlayerCheatsTest passed");
    }
}
