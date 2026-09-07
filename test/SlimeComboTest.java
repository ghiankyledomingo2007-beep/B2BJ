import java.util.List;

/** Player-facing sequences, including same-step collisions that ordinary play rarely reproduces. */
public final class SlimeComboTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("combo")) comboAndReset();
        if (args.length == 0 || args[0].equals("counter")) backDashCounter();
        if (args.length == 0 || args[0].equals("priority")) impactBeforeRetaliation();
        if (args.length == 0 || args[0].equals("stagger")) staggerCannotBeRefreshedForever();
        if (args.length == 0 || args[0].equals("slam")) slamShovesOnce();
        System.out.println("SlimeComboTest passed");
    }

    private static void comboAndReset() {
        var game = fixture(List.of());
        assert game.attack(1, 0);
        game.update(.2, 0, 0);
        WaterProjectile first = latest(game);
        assert first.y() > game.player().y() : "first M1 must sweep to one side of aim";
        assert first.damage() == 1 && !first.heavy();
        game.update(.3, 0, 0);
        assert game.attack(1, 0);
        game.update(.2, 0, 0);
        WaterProjectile second = latest(game);
        assert second.y() < game.player().y() : "second M1 must reverse the water cut";
        game.update(.3, 0, 0);
        assert game.attack(1, 0);
        game.update(.2, 0, 0);
        WaterProjectile third = latest(game);
        assert third.damage() == 2 && third.impulse() > first.impulse()
                : "third M1 must finish with a stronger impact";
        assert !third.heavy() && game.tideCooldown() == 0 : "finisher is not a free Tide Wave";
        game.update(.5, 0, 0);
        game.update(.5, 0, 0);
        assert game.attack(1, 0);
        game.update(.2, 0, 0);
        assert latest(game).damage() == 1 && latest(game).y() > game.player().y()
                : "expired combo starts at first cut";
        game.update(.3, 0, 0);
        assert game.attack(1, 0);
        assert game.dash(0, 1) : "dash may cancel the second windup";
        game.update(.5, 0, 0);
        assert game.attack(1, 0);
        game.update(.2, 0, 0);
        assert latest(game).damage() == 1 : "cancelled cast must not bank a finisher";
    }

    private static void backDashCounter() {
        var back = fixture(List.of());
        assert back.dash(-1, 0);
        assert !back.attack(1, 0) : "counter cannot fire during dash invulnerability";
        back.update(.18, 0, 0);
        back.pause(); back.update(.5, 0, 0); back.togglePause();
        assert back.attack(1, 0);
        back.update(.06, 0, 0);
        assert back.projectiles().size() == 1 : "back-dash counter has a short release";
        WaterProjectile counter = latest(back);
        assert counter.damage() == 1 && counter.impulse() > 140 && !counter.heavy()
                : "counter rewards timing with control, not bonus damage";
        assert !back.attack(1, 0) : "counter still spends normal casting cooldown";
        back.update(.5, 0, 0);
        assert back.attack(1, 0);
        back.update(.06, 0, 0);
        assert back.waterCharge() >= 0 : "counter window is consumed once";

        for (int[] direction : new int[][]{{1, 0}, {0, 1}}) {
            var other = fixture(List.of());
            assert other.dash(direction[0], direction[1]);
            other.update(.18, 0, 0);
            assert other.attack(1, 0);
            other.update(.06, 0, 0);
            assert other.projectiles().isEmpty() : "forward and side dashes do not grant countershot";
        }
        var late = fixture(List.of());
        assert late.dash(-1, 0); late.update(.5, 0, 0);
        assert late.attack(1, 0); late.update(.06, 0, 0);
        assert late.projectiles().isEmpty() : "counter requires a timely follow-up";
    }

    @SuppressWarnings("unchecked")
    private static void impactBeforeRetaliation() throws Exception {
        for (Wisp.Role role : Wisp.Role.values()) {
            var enemy = new Wisp(350, 300, 350, 350, 4, role);
            var game = fixture(List.of(enemy));
            set(enemy, "state", Wisp.State.TELEGRAPH);
            set(enemy, "stateTime", enemy.telegraphDuration() - .005);
            var waves = RuinedOutpostGame.class.getDeclaredField("projectiles");
            waves.setAccessible(true);
            ((List<WaterProjectile>) waves.get(game)).add(new WaterProjectile(350, 300, 1, 0, true));
            game.update(.01, 0, 0);
            assert enemy.health() == 2 && enemy.state() == Wisp.State.HURT;
            assert game.player().health() == Player.MAX_HEALTH
                    : role + " dealt contact damage before the same-step stagger";
            assert game.hostileProjectiles().isEmpty() : "interrupted release must not leave a ghost volley";
        }
    }

    private static void staggerCannotBeRefreshedForever() {
        var enemy = new Wisp(350, 300, 350, 350, 20);
        assert enemy.hurt(1, true);
        enemy.update(.1);
        assert enemy.hurt(1, true);
        assert enemy.stateSeconds() >= .1 : "another impact must not reset active stun";
        enemy.update(.2);
        assert enemy.state() != Wisp.State.HURT;
        assert enemy.hurt(1, true);
        assert enemy.state() != Wisp.State.HURT : "brief stagger resistance lets enemy act again";
        enemy.update(1.2);
        assert enemy.hurt(1, true) && enemy.state() == Wisp.State.HURT;
        assert enemy.hurt(100, true) && !enemy.alive() : "stagger resistance is never damage immunity";
    }

    private static void slamShovesOnce() {
        var enemy = new Wisp(365, 300, 365, 365, 20);
        var game = fixture(List.of(enemy));
        assert game.dash(1, 0);
        for (int i = 0; i < 22; i++) game.update(.01, 0, 0);
        assert enemy.health() == 19 : "one damage per enemy per body slam";
        assert enemy.x() > 380 : "body slam must visibly push the enemy out of the slime";
        assert game.player().health() == Player.MAX_HEALTH : "staggered target cannot trade through slam";
    }

    private static void set(Wisp enemy, String name, Object value) throws Exception {
        var field = Wisp.class.getDeclaredField(name); field.setAccessible(true); field.set(enemy, value);
    }
    private static WaterProjectile latest(RuinedOutpostGame game) {
        return game.projectiles().get(game.projectiles().size() - 1);
    }
    private static RuinedOutpostGame fixture(List<Wisp> enemies) {
        var game = new RuinedOutpostGame(new RuinedOutpostMap(), new Player(300, 300),
                enemies, new Guardian(900, 300));
        game.begin(); return game;
    }
}
