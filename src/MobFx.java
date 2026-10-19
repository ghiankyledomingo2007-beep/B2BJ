import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Visual-only effects for ordinary enemies, driven by their public state (they never change a fight):
 * an alert pop when one notices you, a glint as it winds up, dust and speed streaks as it lunges and
 * skids, an arc where a shield blow lands, charge motes and a muzzle flash on spitters, and a burst when
 * one dies. Every mark is drawn on the 2x pixel grid. Timings follow docs/design/attack-feel.md:
 * anticipation reads first, the strike lasts a few frames, nothing smears on the wind-up.
 */
final class MobFx {
    enum Kind { ALERT, GLINT, DUST, STREAK, ARC, MOTE, FLASH, SPARK }

    /** One mark; x, y in world pixels; vx, vy in world px/s; ax, ay its direction where it has one. */
    private static final class Bit {
        final Kind kind; double x, y, vx, vy, ax, ay, age; final double life; final int colour;
        Bit(Kind kind, double x, double y, double life, int colour) { this.kind = kind; this.x = x; this.y = y; this.life = life; this.colour = colour; }
    }

    private static final int LIMIT = 420;
    private static final Color[] DANGER = {new Color(0xf16458), new Color(0xff9c80), new Color(0xfff0bd)};
    private final List<Bit> bits = new ArrayList<>();
    private final Map<Wisp, Wisp.State> last = new WeakHashMap<>();
    private final Map<Wisp, Boolean> noticed = new WeakHashMap<>();
    private long seed = 0x6d6f62L;

    private double rnd() { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed >>> 11) / (double) (1L << 53); }
    private Bit add(Kind kind, double x, double y, double life, int colour) {
        if (bits.size() >= LIMIT) bits.remove(0);
        var b = new Bit(kind, x, y, life, colour);
        bits.add(b);
        return b;
    }

    void clear() { bits.clear(); last.clear(); noticed.clear(); }

    // ------------------------------------------------------------------ simulation
    void update(double seconds, RuinedOutpostGame game) {
        for (Wisp w : game.scouts()) {
            var before = last.put(w, w.state());
            boolean aggro = w.alive() && w.aggro();
            Boolean was = noticed.put(w, aggro);
            if (aggro && Boolean.FALSE.equals(was)) alert(w);
            if (before != null && before != w.state()) transition(w, before);
            if (w.state() == Wisp.State.TELEGRAPH && w.role() == Wisp.Role.SPITTER && rnd() < .5) chargeMote(w);
            if (w.state() == Wisp.State.LUNGE && lunger(w)) streak(w);
        }
        for (int i = bits.size() - 1; i >= 0; i--) {
            var b = bits.get(i);
            b.age += seconds;
            if (b.age >= b.life) { bits.remove(i); continue; }
            b.x += b.vx * seconds; b.y += b.vy * seconds;
            double drag = b.kind == Kind.DUST ? 5 : b.kind == Kind.SPARK ? 3 : 0;   // fast start, exponential slow
            b.vx *= Math.exp(-drag * seconds); b.vy *= Math.exp(-drag * seconds);
            if (b.kind == Kind.MOTE) {                                             // motes are drawn into the mouth
                b.vx += (b.ax - b.x) * 40 * seconds; b.vy += (b.ay - b.y) * 40 * seconds;
            }
        }
    }

    private static boolean lunger(Wisp w) { return w.role() != Wisp.Role.SPITTER && !w.stationaryMelee(); }
    private static double headY(Wisp w) { return w.y() - 44; }

    private void transition(Wisp w, Wisp.State from) {
        double ix = w.intentX(), iy = w.intentY(), len = Math.max(1e-6, Math.hypot(ix, iy));
        ix /= len; iy /= len;
        switch (w.state()) {
            case TELEGRAPH -> {                                                    // the wind-up glint, on the eye or the shield
                double gx = w.x() + (w.stationaryMelee() ? ix * 18 : ix * 6), gy = headY(w) + (w.stationaryMelee() ? 12 : 0);
                add(Kind.GLINT, gx, gy, .26, 0xfff0bd);
                if (lunger(w)) for (int i = 0; i < 3; i++) dust(w.x() - ix * 10, w.y() + 22, -ix * 50 + (rnd() - .5) * 40, -iy * 20);
            }
            case LUNGE -> {
                if (lunger(w)) for (int i = 0; i < 6; i++)                          // push-off dust behind the rush
                    dust(w.x() - ix * 18 + (rnd() - .5) * 20, w.y() + 22, -ix * (90 + rnd() * 80), -iy * 40 + (rnd() - .5) * 30);
                else if (w.stationaryMelee()) {                                    // the blow's arc where it lands
                    var arc = add(Kind.ARC, w.x() + ix * 26, w.y() + 10 + iy * 26, .2, 0xf16458);
                    arc.ax = ix; arc.ay = iy;
                    for (int i = 0; i < 4; i++) dust(w.x() + ix * 44 + (rnd() - .5) * 30, w.y() + 22 + iy * 30, ix * 60 + (rnd() - .5) * 60, iy * 30);
                } else {                                                           // a spitter's muzzle flash and recoil puff
                    var flash = add(Kind.FLASH, w.x() + ix * 20, w.y() - 24 + iy * 10, .1, 0xfff0bd);
                    flash.ax = ix; flash.ay = iy;
                    dust(w.x() - ix * 12, w.y() + 22, -ix * 60, -iy * 30);
                }
            }
            case RECOVER -> {
                if (from == Wisp.State.LUNGE && lunger(w))                        // the skid at the end of the rush
                    for (int i = 0; i < 3; i++) dust(w.x() + ix * 10 + (rnd() - .5) * 16, w.y() + 22, ix * 40, (rnd() - .5) * 20);
            }
            default -> { }
        }
    }

    private void alert(Wisp w) { add(Kind.ALERT, w.x(), headY(w) - 24, .7, 0xffc45e); }
    private void dust(double x, double y, double vx, double vy) {
        var d = add(Kind.DUST, x, y, .42 + rnd() * .2, rnd() < .5 ? 0x7d725d : 0x615849);
        d.vx = vx; d.vy = vy;
    }
    private void streak(Wisp w) {
        double ix = w.intentX(), iy = w.intentY(), len = Math.max(1e-6, Math.hypot(ix, iy));
        var s = add(Kind.STREAK, w.x() - ix / len * 14 + (rnd() - .5) * 24, w.y() - 16 + (rnd() - .5) * 24, .12, 0xe2b6f5);
        s.ax = ix / len; s.ay = iy / len;
    }
    private void chargeMote(Wisp w) {
        double ix = w.intentX(), iy = w.intentY(), len = Math.max(1e-6, Math.hypot(ix, iy));
        double mx = w.x() + ix / len * 20, my = w.y() - 24, a = rnd() * Math.PI * 2, r = 26 + rnd() * 16;
        var m = add(Kind.MOTE, mx + Math.cos(a) * r, my + Math.sin(a) * r * .7, .24, 0xe2b6f5);
        m.ax = mx; m.ay = my;
    }

    /** An enemy dies: one white flash, then its ichor bursts outward and settles. */
    void defeated(double x, double y, boolean reduced) {
        add(Kind.FLASH, x, y - 20, .08, 0xffffff).ax = 0;
        for (int i = 0; i < (reduced ? 6 : 12); i++) {
            double a = i * Math.PI * 2 / 12 + rnd() * .4, speed = 120 + rnd() * 140;
            var s = add(Kind.SPARK, x, y - 20, .35 + rnd() * .2, i % 3 == 0 ? 0xffdd80 : i % 3 == 1 ? 0xab68cf : 0xe2b6f5);
            s.vx = Math.cos(a) * speed; s.vy = Math.sin(a) * speed * .7 - 40;
        }
    }

    // ------------------------------------------------------------------ drawing (native pixels, shown 2x)
    /** Dust and arcs lie on the ground, under every actor. */
    void drawGround(Graphics2D g, int cx, int cy, boolean reduced) {
        for (var b : bits) {
            double t = b.age / b.life;
            int x = snap(b.x - cx), y = snap(b.y - cy);
            switch (b.kind) {
                case DUST -> {
                    int r = (int) Math.round(2 + t * 3);                          // dense first, then billows and thins
                    if (t > .7 && ((int) (b.age * 60) & 1) == 1) continue;
                    g.setColor(new Color(b.colour));
                    blob(g, x, y, r);
                }
                case ARC -> {
                    double reach = 34 + t * 26;
                    g.setColor(t < .35 ? DANGER[2] : t < .7 ? DANGER[1] : DANGER[0]);
                    double base = Math.atan2(b.ay, b.ax);
                    for (double a = -.9; a <= .9; a += .07) {
                        int px = snap(b.x - cx + Math.cos(base + a) * reach), py = snap(b.y - cy + Math.sin(base + a) * reach * .7);
                        g.fillRect(px, py, 2, 2);
                        if (t < .5) g.fillRect(snap(b.x - cx + Math.cos(base + a) * (reach - 4)), snap(b.y - cy + Math.sin(base + a) * (reach - 4) * .7), 2, 2);
                    }
                }
                default -> { }
            }
        }
    }

    /** Alerts, glints, streaks, flashes, motes and sparks read above the actors. */
    void drawAbove(Graphics2D g, int cx, int cy, boolean reduced) {
        for (var b : bits) {
            double t = b.age / b.life;
            int x = snap(b.x - cx), y = snap(b.y - cy);
            switch (b.kind) {
                case ALERT -> {
                    double pop = t < .12 ? t / .12 * 1.3 : t < .2 ? 1.3 - (t - .12) / .08 * .3 : 1;
                    if (t > .82 && ((int) (b.age * 30) & 1) == 1) continue;
                    exclaim(g, x, y - (int) Math.round(pop * 4) * 2, pop);
                }
                case GLINT -> {
                    int arm = t < .35 ? 1 + (int) (t / .35 * 4) : (int) Math.round((1 - t) / .65 * 5);
                    if (arm <= 0) continue;
                    g.setColor(new Color(0xfff0bd));
                    for (int i = -arm; i <= arm; i++) { g.fillRect(x + i * 2, y, 2, 2); g.fillRect(x, y + i * 2, 2, 2); }
                    g.setColor(DANGER[1]);
                    g.fillRect(x - 2, y - 2, 2, 2); g.fillRect(x + 2, y - 2, 2, 2); g.fillRect(x - 2, y + 2, 2, 2); g.fillRect(x + 2, y + 2, 2, 2);
                }
                case STREAK -> {
                    g.setColor(new Color(b.colour));
                    int len = (int) Math.round((1 - t) * 6);
                    for (int i = 0; i < len; i++) g.fillRect(snap(b.x - cx - b.ax * i * 4), snap(b.y - cy - b.ay * i * 4), 2, 2);
                }
                case FLASH -> {
                    if (reduced && b.colour == 0xffffff) continue;
                    int r = b.colour == 0xffffff ? 18 : 6 + (int) ((1 - t) * 4);
                    g.setColor(new Color(b.colour));
                    blob(g, x, y, r / 2);
                    if (b.colour != 0xffffff) for (int k = 0; k < 4; k++) {          // four short rays
                        double a = k * Math.PI / 2 + Math.atan2(b.ay, b.ax) + Math.PI / 4;
                        for (int s = r / 2; s < r; s += 2) g.fillRect(snap(x + Math.cos(a) * s), snap(y + Math.sin(a) * s), 2, 2);
                    }
                }
                case MOTE -> {
                    g.setColor(new Color(t < .5 ? b.colour : 0xab68cf));
                    g.fillRect(x, y, 2, 2);
                }
                case SPARK -> {
                    g.setColor(new Color(b.colour));
                    g.fillRect(x, y, 2, 2);
                    if (t < .4) g.fillRect(snap(x - b.vx * .012), snap(y - b.vy * .012), 2, 2);   // a short tail while fast
                }
                default -> { }
            }
        }
    }

    private static int snap(double v) { return (int) Math.floor(v / 2) * 2; }
    private static void blob(Graphics2D g, int x, int y, int r) {
        for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++)
            if (dx * dx + dy * dy <= r * r + r) g.fillRect(x + dx * 2, y + dy * 2, 2, 2);
    }
    /** A pixel "!" with an ink outline, scaled by its pop. */
    private static void exclaim(Graphics2D g, int x, int y, double pop) {
        int[][] mark = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 5}};
        int s = pop > 1.15 ? 4 : 2;
        g.setColor(new Color(0x090c11));
        for (int[] p : mark) for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++)
            g.fillRect(x + (p[0] + ox) * s - s / 2, y + (p[1] + oy) * s - 6 * s, s, s);
        g.setColor(new Color(0xffc45e));
        for (int[] p : mark) g.fillRect(x + p[0] * s - s / 2, y + p[1] * s - 6 * s, s, s);
        g.setColor(new Color(0xfff0bd));
        g.fillRect(x - s / 2, y - 6 * s, s, s);
    }
}
