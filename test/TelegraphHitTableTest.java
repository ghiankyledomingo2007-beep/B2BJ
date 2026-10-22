import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

/** Boss overlays stay absent; compact enemy cues follow their real attack heading at odd camera offsets. */
public final class TelegraphHitTableTest {
    interface Zone { boolean at(double wx, double wy); }
    static final int CAM_X = 301, CAM_Y = 217, W = 640, H = 360, SLACK = 2;
    static int cases;

    public static void main(String[] args) throws Exception {
        bosses();
        enemies();
        System.out.println("TelegraphHitTableTest passed: " + cases + " boss-overlay and enemy-heading cases");
    }

    private static void bosses() throws Exception {
        double gx = 701.3, gy = 399.6;
        for (var profile : Guardian.Profile.values()) for (boolean enraged : new boolean[]{false, true})
            for (var attack : Guardian.Attack.values()) for (boolean warning : new boolean[]{true, false}) {
                var g = new Guardian(gx, gy, profile);
                g.activate(gx, gy);
                if (enraged) g.hurt(g.health() - 1);
                boolean centred = attack == Guardian.Attack.SWEEP || attack == Guardian.Attack.SHOCKWAVE;
                double tx = centred ? gx : gx + 120.4, ty = centred ? gy : gy + 33.3, dx = Math.cos(.5), dy = Math.sin(.5);
                double[] times = warning ? new double[]{.5} : attack == Guardian.Attack.SHOCKWAVE ? new double[]{0, .3, .6} : new double[]{0};
                for (double t : times) {
                    set(g, "state", warning ? Guardian.State.TELEGRAPH : Guardian.State.SLAM);
                    set(g, "attack", attack);
                    set(g, "stateTime", warning ? t * g.telegraphDuration() : t);
                    set(g, "targetX", tx); set(g, "targetY", ty); set(g, "chargeX", dx); set(g, "chargeY", dy);
                    set(g, "fissureHorizontal", enraged);
                    var game = new RuinedOutpostGame(new RuinedOutpostMap(9), new Player(640, 360), List.of(), g);
                    checkEmpty(profile + " " + attack + (warning ? " warning" : " live"), game);
                }
            }
    }

    private static void enemies() throws Exception {
        for (var kind : new CampaignEnemy.Kind[]{CampaignEnemy.Kind.OUTPOST_SCOUT, CampaignEnemy.Kind.THORN_WOLF, CampaignEnemy.Kind.RIFT_IMP,
                CampaignEnemy.Kind.OUTPOST_SPITTER, CampaignEnemy.Kind.CINDER_HEXER, CampaignEnemy.Kind.OUTPOST_GUARD,
                CampaignEnemy.Kind.FALLEN_KNIGHT}) for (int phase = 0; phase < 3; phase++) {
            double ex = 701.3, ey = 399.6, px = ex + 84.2, py = ey + 41.7;
            var e = new CampaignEnemy(kind, ex, ey);
            var b = e.body();
            for (int i = 0; i < 800 && b.state() != Wisp.State.TELEGRAPH; i++) e.update(.01, px, py, null);
            assert b.state() == Wisp.State.TELEGRAPH : "no warning from " + kind;
            if (phase >= 1) for (int i = 0; i < 800 && !b.directionLocked(); i++) e.update(.005, px, py, null);
            if (phase == 2) {
                if (!b.stationaryMelee()) continue;                         // only a standing blow is drawn while it lands
                for (int i = 0; i < 800 && b.state() == Wisp.State.TELEGRAPH; i++) e.update(.005, px, py, null);
                assert b.state() == Wisp.State.LUNGE;
            }
            var game = new RuinedOutpostGame(new RuinedOutpostMap(9), new Player(640, 360), List.of(b), new Guardian(1100, 500));
            Field enemies = RuinedOutpostGame.class.getDeclaredField("campaignEnemies");
            enemies.setAccessible(true);
            @SuppressWarnings("unchecked") var map = (java.util.Map<Wisp, CampaignEnemy>) enemies.get(game);
            map.put(b, e);
            double bx = b.x(), by = b.y(), ix = b.intentX(), iy = b.intentY(), angle = Math.atan2(iy, ix);
            Zone zone;
            if (b.stationaryMelee()) zone = (x, y) -> {
                double fx = x - bx, fy = y - by, forward = fx * ix + fy * iy, side = Math.abs(fx * iy - fy * ix);
                return forward >= 0 && forward <= b.meleeReach() && side <= b.meleeHalfWidth();
            };
            else if (b.role() == Wisp.Role.SPITTER) {
                boolean radial = kind == CampaignEnemy.Kind.CINDER_HEXER;
                double lane = Player.COLLISION_RADIUS + EnemyProjectile.RADIUS;
                zone = (x, y) -> {
                    for (int s = 0; s < (radial ? 8 : 3); s++) {
                        double d = angle + (radial ? s * Math.PI / 4 : (s - 1) * .20);
                        if (segment(x, y, bx, by, bx + Math.cos(d) * EnemyProjectile.MAX_TRAVEL, by + Math.sin(d) * EnemyProjectile.MAX_TRAVEL) <= lane) return true;
                    }
                    return false;
                };
            } else zone = (x, y) -> segment(x, y, bx, by, bx + ix * b.lungeReach(), by + iy * b.lungeReach()) <= RuinedOutpostGame.WISP_CONTACT_RADIUS;
            String name = kind + (phase == 0 ? " aiming" : phase == 1 ? " locked" : " striking");
            if (phase == 2) { checkEmpty(name, game); continue; }
            // The paths cues must follow, in world pixels from the body's centre.
            double from = B2BJ.AIM_START * 2 + 4;
            java.util.List<double[]> paths = new java.util.ArrayList<>();
            if (b.role() == Wisp.Role.SPITTER) {
                boolean radial = kind == CampaignEnemy.Kind.CINDER_HEXER;
                double reach = (B2BJ.AIM_START + (radial ? B2BJ.SPOKE : B2BJ.SHOT_CUE_LENGTH)) * 2;
                for (int s = 0; s < (radial ? 8 : 3); s++) {
                    double d = angle + (radial ? s * Math.PI / 4 : (s - 1) * .20);
                    paths.add(new double[]{Math.cos(d), Math.sin(d), reach});
                }
            } else paths.add(new double[]{ix, iy, Math.min((B2BJ.AIM_START+B2BJ.SPOKE)*2,
                    b.stationaryMelee()?b.meleeReach():b.lungeReach())});
            checkLines(name, game, zone, bx, by, from, paths);
        }
    }

    /** The game's warnings, rendered; true where a native pixel is painted. */
    private static boolean[][] render(RuinedOutpostGame game) throws Exception {
        var panel = new B2BJ(false);
        set(panel, "game", game);
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var draw = B2BJ.class.getDeclaredMethod("drawGuardianTelegraph", Graphics2D.class, int.class, int.class);
        draw.setAccessible(true);
        var ink = image.createGraphics(); draw.invoke(panel, ink, CAM_X, CAM_Y); ink.dispose();
        boolean[][] painted = new boolean[H][W];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) painted[y][x] = (image.getRGB(x * 2, y * 2) >>> 24) != 0;
        return painted;
    }

    /** A line warning: never on safe ground, and present all along each path out to its reach. */
    private static void checkLines(String name, RuinedOutpostGame game, Zone zone, double bx, double by, double from,
            java.util.List<double[]> paths) throws Exception {
        boolean[][] painted = render(game);
        int count = 0;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (!painted[y][x]) continue;
            count++;
            assert Math.hypot(CAM_X+x*2-bx,CAM_Y+y*2-by)<=76 : name+": long attack lines must stay gone";
            boolean safe = true;
            for (int dy = -SLACK; dy <= SLACK && safe; dy++) for (int dx = -SLACK; dx <= SLACK && safe; dx++)
                if (zone.at(CAM_X + (x + dx) * 2 + 1, CAM_Y + (y + dy) * 2 + 1)) safe = false;
            assert !safe : name + ": marks safe ground at native " + x + "," + y;
        }
        assert count > 0 : name + ": nothing drawn";
        for (double[] path : paths) for (double d = from; d <= path[2] - 2; d += 4) {
            int nx = (int) Math.floor((bx + path[0] * d - CAM_X) / 2), ny = (int) Math.floor((by + path[1] * d - CAM_Y) / 2);
            if (nx < SLACK || ny < SLACK || nx >= W - SLACK || ny >= H - SLACK) continue;
            assert near(painted, nx, ny) : name + ": path left unmarked " + Math.round(d) + " px out";
        }
        cases++;
    }

    private static void checkEmpty(String name, RuinedOutpostGame game) throws Exception {
        var painted=render(game);
        for(var row:painted)for(boolean pixel:row)assert !pixel : name+": unexpected attack overlay";
        cases++;
    }

    private static boolean near(boolean[][] mask, int x, int y) {
        for (int dy = -SLACK; dy <= SLACK; dy++) for (int dx = -SLACK; dx <= SLACK; dx++) {
            int nx = x + dx, ny = y + dy;
            if (nx >= 0 && ny >= 0 && nx < W && ny < H && mask[ny][nx]) return true;
        }
        return false;
    }

    private static double segment(double px, double py, double ax, double ay, double bx, double by) {
        return java.awt.geom.Line2D.ptSegDist(ax, ay, bx, by, px, py);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
