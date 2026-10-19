import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Every attack warning against the hit test it describes, for every boss profile (calm and enraged),
 * every boss attack (warning and live), and every enemy archetype (aiming, locked, striking), at an odd
 * camera and fractional positions. No painted pixel ever lies more than 2 native pixels (4 world px) from
 * real damage. Boss attacks and standing blows outline their whole damage area: every edge pixel has a
 * painted pixel within 2 native pixels. A regular enemy's shot or rush is drawn as a thin line instead
 * (lanes for every shot buried the screen): that line must trace the attack's true path to its full reach.
 */
public final class TelegraphHitTableTest {
    interface Zone { boolean at(double wx, double wy); }
    static final int CAM_X = 301, CAM_Y = 217, W = 640, H = 360, SLACK = 2;
    static int cases;

    public static void main(String[] args) throws Exception {
        bosses();
        enemies();
        System.out.println("TelegraphHitTableTest passed: " + cases + " warnings matched to their hit tests");
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
                    Zone zone;
                    if (!warning && attack == Guardian.Attack.SHOCKWAVE) {
                        // a live wave also shows its reach: everything it will still sweep before it fades
                        double from = g.waveRadius() - Guardian.WAVE_HALF_WIDTH, to = g.slamRadius() + Guardian.WAVE_HALF_WIDTH;
                        zone = (x, y) -> { double d = Math.hypot(x - tx, y - ty); return g.hits(x, y) || d >= from && d <= to; };
                    } else if (!warning) zone = g::hits;
                    else zone = switch (attack) {
                        case CHARGE -> (x, y) -> segment(x, y, gx, gy, gx + dx * Guardian.CHARGE_SPEED * Guardian.CHARGE_DURATION,
                                gy + dy * Guardian.CHARGE_SPEED * Guardian.CHARGE_DURATION) <= Guardian.CHARGE_RADIUS;
                        case SHOCKWAVE -> (x, y) -> Math.hypot(x - tx, y - ty) <= g.slamRadius() + Guardian.WAVE_HALF_WIDTH;
                        case FISSURE -> (x, y) -> { var r = g.fissureBounds(); return Math.abs(x - r.centerX()) <= r.width() / 2 && Math.abs(y - r.centerY()) <= r.height() / 2; };
                        default -> (x, y) -> Math.hypot(x - tx, y - ty) <= g.slamRadius();
                    };
                    check(profile + (enraged ? " enraged " : " ") + attack + (warning ? " warning" : " live t=" + t), game, zone);
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
            if (b.stationaryMelee()) { check(name, game, zone); continue; }
            // the paths the lines must trace, in world px from the body's centre
            double from = B2BJ.AIM_START * 2 + 4;
            java.util.List<double[]> paths = new java.util.ArrayList<>();
            if (b.role() == Wisp.Role.SPITTER) {
                boolean radial = kind == CampaignEnemy.Kind.CINDER_HEXER;
                double reach = radial ? (B2BJ.AIM_START + B2BJ.SPOKE) * 2 : EnemyProjectile.MAX_TRAVEL;
                for (int s = 0; s < (radial ? 8 : 3); s++) {
                    double d = angle + (radial ? s * Math.PI / 4 : (s - 1) * .20);
                    paths.add(new double[]{Math.cos(d), Math.sin(d), reach});
                }
            } else paths.add(new double[]{ix, iy, b.lungeReach()});
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

    /** Renders the game's warnings and compares them, native pixel by native pixel, with the damage zone. */
    private static void check(String name, RuinedOutpostGame game, Zone zone) throws Exception {
        boolean[][] painted = render(game), hit = new boolean[H][W];
        int paintedCount = 0;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            hit[y][x] = zone.at(CAM_X + x * 2 + 1, CAM_Y + y * 2 + 1);
            if (painted[y][x]) paintedCount++;
        }
        assert paintedCount > 0 : name + ": nothing drawn";
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (painted[y][x]) assert near(hit, x, y) : name + ": marks safe ground at native " + x + "," + y;
            boolean edge = hit[y][x] && x > 0 && y > 0 && x < W - 1 && y < H - 1
                    && (!hit[y][x - 1] || !hit[y][x + 1] || !hit[y - 1][x] || !hit[y + 1][x]);
            if (edge) assert near(painted, x, y) : name + ": damage edge left unmarked at native " + x + "," + y;
        }
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
