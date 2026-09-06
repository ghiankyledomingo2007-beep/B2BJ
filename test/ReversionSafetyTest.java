import java.util.List;

public final class ReversionSafetyTest {
    public static void main(String[] args) {
        RuinedOutpostMap map = new RuinedOutpostMap();
        for (double remaining : new double[]{0.01, 0.06, 0.15}) {
            Player p = new Player(600, 500);
            p.collectIchor(100);
            assert p.transform();
            p.move(0, 0, Player.BLADE_DURATION - remaining, map);
            assert p.dash(1, 0);
            p.move(0, 0, remaining + 0.001, map);
            assert p.recovering() && !p.bladeForm();
            assert !p.dashing() : "reversion must cancel stale dash";
            assert p.hurt(1) : "reversion cannot retain dash immunity";
            double x = p.x();
            p.move(0, 0, 1, map);
            assert !p.invulnerable() && !p.dash(1, 0);
            p.move(0, 0, 0.6, map);
            assert p.x() == x : "expired dash must not resume";
        }
        Player p = new Player(600, 500);
        p.collectIchor(100); p.transform();
        p.move(0, 0, 11.9, map);
        assert p.hurt(1) && p.recovering();
        p.move(0, 0, 0.7, map);
        assert p.recovering() && !p.invulnerable() : "damage reversion retains only normal hurt i-frames";
        p = new Player(600, 500);
        p.collectIchor(100); p.transform();
        p.move(0, 0, 11.999, map);
        Wisp enemy = new Wisp(625, 500, 625, 625, 4);
        RuinedOutpostGame game = new RuinedOutpostGame(map, p, List.of(enemy), new Guardian(1500, 800));
        game.begin();
        assert game.dash(1, 0);
        game.update(0.01, 0, 0);
        assert p.recovering();
        assert enemy.health() == 4 : "canceled Blade dash must not become a Slime contact hit";
        System.out.println("ReversionSafetyTest passed");
    }
}
