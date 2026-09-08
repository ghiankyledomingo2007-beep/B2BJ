import java.util.List;

public final class CombatAuditTest {
    public static void main(String[] args) {
        expiredSummonsDoNotAccumulateOrShiftAuthoredRewards();
        System.out.println("CombatAuditTest passed");
    }

    private static void expiredSummonsDoNotAccumulateOrShiftAuthoredRewards() {
        var game = RuinedOutpostGame.campaign();
        game.begin();
        List<Wisp> authored = List.copyOf(game.scouts());
        game.player().relocate(game.guardian().x() + 300, game.guardian().y());
        assert !game.map().isBlocked(game.player().x(), game.player().y() + Player.COLLISION_Y_OFFSET,
                Player.COLLISION_RADIUS);
        for (int cycle = 0; cycle < 3; cycle++) {
            Wisp summoned = null;
            for (int tick = 0; tick < 1_000 && summoned == null; tick++) {
                game.player().heal(); game.update(.01, 0, 0);
                summoned = game.scouts().stream().filter(Wisp::alive)
                        .filter(enemy -> !authored.contains(enemy)).findFirst().orElse(null);
            }
            assert summoned != null : "active boss must produce a real summoned enemy";
            for (int tick = 0; tick < 200 && summoned.alive(); tick++) {
                game.player().heal(); game.player().collectIchor(100); game.transform();
                positionForStrike(game, summoned);
                game.attack((int) Math.round((summoned.x() - game.player().x()) * 100),
                        (int) Math.round((summoned.y() - game.player().y()) * 100));
                game.update(.05, 0, 0);
            }
            assert !summoned.alive() : "summon must die through a real player strike";
            assert summoned.visible() && game.scouts().contains(summoned)
                    : "cleanup must preserve the short death pose";
            for (int tick = 0; tick < 100; tick++) { game.player().heal(); game.update(.01, 0, 0); }
            assert !summoned.visible();
            assert !game.scouts().contains(summoned) : "expired summoned body still occupies combat list";
            assert game.campaignEnemy(summoned) == null : "expired summon profile still retained";
            for (int index = 0; index < authored.size(); index++)
                assert game.scouts().get(index) == authored.get(index)
                        : "summon cleanup must preserve authored enemy indices used by elite rewards";
        }
    }

    private static void positionForStrike(RuinedOutpostGame game, Wisp enemy) {
        for (int i = 0; i < 16; i++) {
            double x = enemy.x() + Math.cos(i * Math.PI / 8) * 110;
            double y = enemy.y() + Math.sin(i * Math.PI / 8) * 110;
            if (!game.map().isBlocked(x, y + Player.COLLISION_Y_OFFSET, Player.COLLISION_RADIUS)
                    && game.map().clearLine(x, y, enemy.x(), enemy.y())) {
                game.player().relocate(x, y); return;
            }
        }
        throw new AssertionError("No legal strike position for summoned enemy");
    }
}
