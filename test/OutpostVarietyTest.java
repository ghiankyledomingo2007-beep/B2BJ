import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class OutpostVarietyTest {
    public static void main(String[] args) {
        int failures = 0;
        for (String check : new String[]{"roster", "guard", "poses"}) {
            try {
                switch (check) { case "roster" -> roster(); case "guard" -> guard(); default -> poses(); }
                System.out.println("Outpost variety " + check + " passed");
            } catch (AssertionError e) { failures++; System.err.println(check + ": " + e.getMessage()); }
        }
        assert failures == 0 : failures + " outpost regressions";
        System.out.println("OutpostVarietyTest passed");
    }
    private static void roster() {
        var area = CampaignWorld.area(0);
        assert area.enemies().size() == 21 : "variety must not inflate encounter density";
        for (var kind : new CampaignEnemy.Kind[]{CampaignEnemy.Kind.OUTPOST_SCOUT,
                CampaignEnemy.Kind.OUTPOST_GUARD, CampaignEnemy.Kind.OUTPOST_SPITTER}) {
            assert area.enemies().stream().anyMatch(e -> e.kind() == kind) : "missing authored " + kind;
        }
        var game = RuinedOutpostGame.campaign(); game.begin();
        assert game.scouts().stream().anyMatch(w -> w.role() == Wisp.Role.SPITTER) : "runtime must instantiate spitters";
    }
    private static CampaignEnemy enemy(CampaignEnemy.Kind kind) { return new CampaignEnemy(kind, 500, 500); }
    private static void until(CampaignEnemy enemy, Wisp.State state, double tx, double ty) {
        for (int i = 0; i < 1000 && enemy.body().state() != state; i++) enemy.update(.01, tx, ty, null);
        assert enemy.body().state() == state : "enemy cannot reach " + state;
    }
    private static void guard() {
        for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            var guard = enemy(CampaignEnemy.Kind.OUTPOST_GUARD);
            int ux = dir[0], uy = dir[1];
            until(guard, Wisp.State.TELEGRAPH, 500 + ux * 80, 500 + uy * 80);
            assert !guard.hits(500 + ux * 80, 500 + uy * 80) : "tell cannot hurt";
            until(guard, Wisp.State.LUNGE, 500 + ux * 80, 500 + uy * 80);
            double x = guard.body().x(), y = guard.body().y();
            assert guard.hits(x + ux * 90, y + uy * 90) : "shield bash must reach its forward lane";
            assert !guard.hits(x - ux * 20, y - uy * 20) : "behind guard stays safe";
            assert !guard.hits(x + ux * 110, y + uy * 110) : "bash has limited reach";
            assert !guard.hits(x - uy * 70, y + ux * 70) : "sidestep avoids bash";
            guard.update(.1, x - ux * 80, y - uy * 80, null);
            assert guard.body().x() == x && guard.body().y() == y : "guard must plant, not copy wolf charge";
            assert guard.hits(x + ux * 90, y + uy * 90) : "bash cannot turn after release";
            until(guard, Wisp.State.RECOVER, x - ux * 80, y - uy * 80);
            assert !guard.hits(x + ux * 20, y + uy * 20) && guard.hurt(1, true, true)
                    : "recovery is safe and punishable in slime form";
        }
        var interrupted = enemy(CampaignEnemy.Kind.OUTPOST_GUARD);
        until(interrupted, Wisp.State.LUNGE, 580, 500); interrupted.hurt(1, true, true);
        assert !interrupted.hits(580, 500) : "stagger cancels bash contact";
    }
    private static void poses() {
        for (var kind : new CampaignEnemy.Kind[]{CampaignEnemy.Kind.OUTPOST_SCOUT,
                CampaignEnemy.Kind.OUTPOST_GUARD, CampaignEnemy.Kind.THORN_WOLF, CampaignEnemy.Kind.FALLEN_KNIGHT}) {
            var enemy = enemy(kind);
            until(enemy, Wisp.State.TELEGRAPH, 580, 500); var tell = pixels(enemy);
            until(enemy, Wisp.State.LUNGE, 580, 500); var active = pixels(enemy);
            until(enemy, Wisp.State.RECOVER, 580, 500); var recovery = pixels(enemy);
            assert !Arrays.equals(tell, active) && !Arrays.equals(active, recovery)
                    : kind + " needs separate windup/contact/recovery poses";
            var reverse = enemy(kind); until(reverse, Wisp.State.LUNGE, 420, 500);
            assert !Arrays.equals(active, pixels(reverse)) : kind + " attack must face its committed direction";
        }
    }
    private static int[] pixels(CampaignEnemy enemy) {
        var image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        CampaignRenderer.drawEnemy(g, enemy, (int) enemy.body().x() - 160, (int) enemy.body().y() - 180); g.dispose();
        // Exclude warning-eye color, health bar and shadow: verify real body/weapon changes.
        return image.getRGB(100, 142, 120, 24, null, 0, 120);
    }
}
