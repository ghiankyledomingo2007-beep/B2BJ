import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Physical attack effects for the Outpost Warden and Briarheart, and when each attack is heard. Driven only by
 * the Guardian's public state and the existing GUARDIAN_SLAM event, so the fight itself is untouched: one windup
 * cue per committed attack, WINDUP_LEAD before it lands (pause, death and resets cancel it), and one release cue.
 * Warden blows leave a blade arc, a fan of stone chips and dust, a rush wake or a split in the ground where they
 * really land; Briarheart's roots bow out of their real lanes, its seeds burst from their real blooms and its cage
 * branches bend shut. Every mark stays inside the damage the model deals, sits on the 2x pixel grid and is finite.
 * Reduced effects thin the decoration; the live roots, cage, sweep and split still show.
 */
final class BossFx {
    /** Seconds before release that the windup starts: tools/audio/generate_boss_sfx.py peaks each one just ahead of the blow. */
    static final double WINDUP_LEAD = .62;
    static final int LIMIT = 220;
    private static final double GRAVITY = 900, FEET = 44;

    private enum Kind { CHIP, SOIL, SEED, SPLINTER, FIBER, LEAF, DUST, RIDE, WAKE, SPLIT, ARC, SWEEP, GLINT }
    /** One mark in world pixels; z is height above its ground point, ax/ay a direction, span/origin a size. */
    private static final class Bit {
        final Kind kind; final double life; final int colour;
        double x, y, z, vx, vy, vz, ax, ay, age, span, origin;
        Bit(Kind kind, double x, double y, double life, int colour) { this.kind = kind; this.x = x; this.y = y; this.life = life; this.colour = colour; }
    }

    private static final int[] STONE = {0x6f6a5f, 0x8a8578, 0xa39b88};
    private static final int STONE_DARK = 0x2e2b2a, STONE_LIGHT = 0xd3ccb5, DUST = 0x7d725d, DUST_DARK = 0x615849;
    private static final int STEEL = 0xeaf6ff, GLOW = 0x7aeee6, GLOW_DARK = 0x3f8f95, INK = 0x1b2a30;
    private static final int CRACK = 0x17151a, CRACK_RIM = 0x8a8170;
    private static final int BARK_DARK = 0x30272b, BARK = 0x796349, BARK_LIGHT = 0xb49665, FIBRE = 0xd5c9a8;
    private static final int[] LEAVES = {0x99a26a, 0x655b45, 0x8fa961};
    private static final int SOIL = 0x423e32, SOIL_LIGHT = 0x596349, HUSK = 0xb49665, SEED_GOLD = 0xffdd80;

    private final List<Bit> bits = new ArrayList<>();
    private Guardian boss;
    private Guardian.State last;
    private int heard = -1, glinted = -1, snapped = -1;
    private double wakeClock;
    private long seed = 0x626f7373L;

    private double rnd() { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed >>> 11) / (double) (1L << 53); }
    private Bit add(Kind kind, double x, double y, double life, int colour) {
        if (bits.size() >= LIMIT) bits.remove(0);
        var b = new Bit(kind, x, y, life, colour);
        bits.add(b);
        return b;
    }

    void clear() { bits.clear(); boss = null; last = null; heard = glinted = snapped = -1; wakeClock = 0; }
    int particles() { return bits.size(); }

    /** A different boss (new area, restart) starts clean; only the Warden and Briarheart have these effects. */
    private boolean follow(Guardian g) {
        if (g != boss) { clear(); boss = g; }
        return g.profile() == Guardian.Profile.OUTPOST_WARDEN || g.profile() == Guardian.Profile.BRIARHEART;
    }

    static GameAudio.Cue windupCue(Guardian g) {
        return switch (g.profile()) {
            case OUTPOST_WARDEN -> GameAudio.Cue.WARDEN_WINDUP;
            case BRIARHEART -> GameAudio.Cue.BRIAR_WINDUP;
            default -> null;
        };
    }

    /** The one sound an attack lands with, hit or miss; the overhead slam and shockwave keep the redesigned slam. */
    static GameAudio.Cue releaseCue(Guardian g) {
        if (g.profile() == Guardian.Profile.BRIARHEART) return switch (g.forestAttack()) {
            case ROOT_WEAVE -> GameAudio.Cue.BRIAR_ROOTS;
            case SEED_BLOOM -> GameAudio.Cue.BRIAR_SEEDS;
            case BRAMBLE_CAGE -> GameAudio.Cue.BRIAR_CAGE;
        };
        if (g.profile() != Guardian.Profile.OUTPOST_WARDEN) return GameAudio.Cue.GUARDIAN_SLAM;
        return switch (g.attack()) {
            case SWEEP -> GameAudio.Cue.WARDEN_SWEEP;
            case CHARGE -> GameAudio.Cue.WARDEN_RUSH;
            case FISSURE -> GameAudio.Cue.WARDEN_FRACTURE;
            case TARGET, SHOCKWAVE -> GameAudio.Cue.GUARDIAN_SLAM;
        };
    }

    // ------------------------------------------------------------------ simulation
    /** One frame of game time; returns the windup cue to play now, at most once per committed attack. */
    GameAudio.Cue update(double dt, Guardian g, boolean reduced) {
        if (!follow(g) || !Double.isFinite(dt) || dt <= 0) return null;
        var state = g.state();
        if (!g.alive()) {                                                     // a fallen boss leaves no attack behind
            if (last != Guardian.State.DEAD) bits.clear();
            last = state;
            return null;
        }
        GameAudio.Cue windup = null;
        if (state == Guardian.State.TELEGRAPH) {
            double left = g.telegraphDuration() - g.stateSeconds();
            if (heard != g.impactNumber() && left <= WINDUP_LEAD) { heard = g.impactNumber(); windup = windupCue(g); }
            if (!reduced) anticipate(dt, g, left);
        }
        if (state == Guardian.State.SLAM) active(dt, g, reduced);
        else if (last == Guardian.State.SLAM && g.profile() == Guardian.Profile.OUTPOST_WARDEN && g.attack() == Guardian.Attack.CHARGE)
            skid(g, reduced);
        for (int i = bits.size() - 1; i >= 0; i--) {
            var b = bits.get(i);
            b.age += dt;
            if (b.age >= b.life) { bits.remove(i); continue; }
            if (b.age < 0) continue;
            switch (b.kind) {
                case CHIP, SOIL, SEED, SPLINTER, FIBER -> {
                    b.vz -= GRAVITY * dt; b.z += b.vz * dt;
                    if (b.z <= 0) {                                           // bounce once or twice, then grind to a stop
                        b.z = 0;
                        if (b.vz < -60) { b.vz = -b.vz * .3; b.vx *= .5; b.vy *= .5; }
                        else { b.vz = 0; b.vx *= Math.exp(-12 * dt); b.vy *= Math.exp(-12 * dt); }
                    }
                    b.x += b.vx * dt; b.y += b.vy * dt;
                }
                case LEAF -> { if (b.z > 0) { b.z = Math.max(0, b.z - 45 * dt); b.x += (b.vx + Math.sin(b.age * 6 + b.origin) * 40) * dt; } }
                case DUST -> { b.x += b.vx * dt; b.y += b.vy * dt; b.vx *= Math.exp(-5 * dt); b.vy *= Math.exp(-5 * dt); }
                case RIDE -> { b.x += b.vx * dt; b.y += b.vy * dt; }
                default -> { }
            }
        }
        last = state;
        return windup;
    }

    /** Body and material anticipation through the tell: ground grit under the Warden, leaves and stirring soil for Briarheart. */
    private void anticipate(double dt, Guardian g, double left) {
        double p = Math.min(1, g.stateSeconds() / g.telegraphDuration());
        if (g.profile() == Guardian.Profile.OUTPOST_WARDEN) {
            if (rnd() < 10 * p * dt) {
                var grit = add(Kind.CHIP, g.x() + (rnd() - .5) * 110, g.y() + FEET + (rnd() - .5) * 16, .4, stone());
                grit.vz = 50 + rnd() * 60; grit.vx = (rnd() - .5) * 20;
            }
            if (glinted != g.impactNumber() && left <= .3 && g.attack() != Guardian.Attack.SHOCKWAVE && g.attack() != Guardian.Attack.FISSURE) {
                glinted = g.impactNumber();                                   // the edge catches the light as it locks
                double gx = g.x(), gy = g.y() - 144;
                if (g.attack() == Guardian.Attack.SWEEP) { gx += g.directionX() < 0 ? 64 : -64; gy = g.y() - 66; }
                if (g.attack() == Guardian.Attack.CHARGE) { gx += g.directionX() * 28; gy = g.y() - 112 + g.directionY() * 16; }
                add(Kind.GLINT, gx, gy, .3, STEEL);
            }
            return;
        }
        if (rnd() < 6 * p * dt) {
            var leaf = add(Kind.LEAF, g.x() + (rnd() - .5) * 120, g.y() + FEET + (rnd() - .5) * 20, 1.4, LEAVES[(int) (rnd() * 3)]);
            leaf.z = 70 + rnd() * 60; leaf.vx = (rnd() - .5) * 20; leaf.origin = rnd() * 6;
        }
        if (rnd() < 16 * p * dt) {                                             // the soil stirs where roots will break out
            var roots = g.forestRoots();
            var blooms = g.forestSeeds();
            if (roots.isEmpty() && blooms.isEmpty()) return;
            int pick = (int) (rnd() * (roots.size() + blooms.size()));
            double sx, sy;
            if (pick < roots.size()) {
                var lane = roots.get(pick);
                sx = lane.centerX() + (rnd() - .5) * lane.width(); sy = lane.centerY() + (rnd() - .5) * lane.height();
            } else {
                var bloom = blooms.get(pick - roots.size());
                double a = rnd() * Math.PI * 2, r = Math.sqrt(rnd()) * bloom.radius() * .8;
                sx = bloom.x() + Math.cos(a) * r; sy = bloom.y() + Math.sin(a) * r;
            }
            var speck = add(Kind.SOIL, sx, sy + Player.COLLISION_Y_OFFSET, .35, SOIL);
            speck.vz = 40 + rnd() * 50;
        }
    }

    /** Effects that follow the live attack: the rush wake behind the charging body, and the cage locking shut. */
    private void active(double dt, Guardian g, boolean reduced) {
        if (g.profile() == Guardian.Profile.OUTPOST_WARDEN && g.attack() == Guardian.Attack.CHARGE && (wakeClock -= dt) <= 0) {
            wakeClock = reduced ? .07 : .035;
            double ux = g.directionX(), uy = g.directionY(), feet = g.y() + FEET;
            for (int side = -1; side <= 1; side += 2) {
                var furrow = add(Kind.WAKE, g.x() - uy * side * 22 - ux * 16, feet + ux * side * 10 - uy * 16, .34, DUST_DARK);
                furrow.ax = ux; furrow.ay = uy;
                if (reduced) continue;
                var kicked = add(Kind.CHIP, g.x() - uy * side * 30, feet + ux * side * 12, .5, stone());
                kicked.vx = -uy * side * (60 + rnd() * 60) - ux * 30; kicked.vy = ux * side * (30 + rnd() * 30) - uy * 20;
                kicked.vz = 80 + rnd() * 80;
            }
        }
        if (g.profile() == Guardian.Profile.BRIARHEART && g.forestAttack() == Guardian.ForestAttack.BRAMBLE_CAGE
                && snapped != g.impactNumber() && g.stateSeconds() >= .9) {
            snapped = g.impactNumber();                                       // the branches lock: splinters spring off inward
            for (var wall : g.forestRoots()) {
                boolean across = wall.width() > wall.height();
                double length = Math.max(wall.width(), wall.height());
                double tx = g.targetX() - wall.centerX(), ty = g.targetY() - wall.centerY(), len = Math.max(1, Math.hypot(tx, ty));
                for (double s = -length / 2 + 30; s < length / 2; s += reduced ? 150 : 50) {
                    double bx = wall.centerX() + (across ? s : 0), by = wall.centerY() + (across ? 0 : s) + Player.COLLISION_Y_OFFSET;
                    var splinter = add(Kind.SPLINTER, bx, by, .5, BARK_LIGHT);
                    splinter.z = 34; splinter.vz = 60 + rnd() * 60;
                    splinter.vx = tx / len * (50 + rnd() * 50); splinter.vy = ty / len * (50 + rnd() * 50);
                    if (!reduced) {
                        var leaf = add(Kind.LEAF, bx + (rnd() - .5) * 20, by, .9, LEAVES[(int) (rnd() * 3)]);
                        leaf.z = 30 + rnd() * 10; leaf.vx = tx / len * 30; leaf.origin = rnd() * 6;
                    }
                }
            }
        }
    }

    /** The rush digs in and stops: grit sprays ahead of the feet. */
    private void skid(Guardian g, boolean reduced) {
        double ux = g.directionX(), uy = g.directionY();
        for (int i = 0; i < (reduced ? 2 : 6); i++) {
            var chip = add(Kind.CHIP, g.x() + ux * 30 + (rnd() - .5) * 40, g.y() + FEET + uy * 20, .55, stone());
            chip.vx = ux * (70 + rnd() * 70) + (rnd() - .5) * 50; chip.vy = uy * (40 + rnd() * 40); chip.vz = 90 + rnd() * 90;
        }
        for (int i = 0; i < (reduced ? 1 : 3); i++) dust(g.x() + ux * 24 + (rnd() - .5) * 40, g.y() + FEET, ux * 60, uy * 30);
    }

    // ------------------------------------------------------------------ the release (GUARDIAN_SLAM)
    /** The moment the committed attack lands, hit or miss. A cancelled or dead attack lands nothing. */
    void release(Guardian g, boolean reduced) {
        if (!follow(g) || !g.alive() || g.state() != Guardian.State.SLAM) return;
        if (g.profile() == Guardian.Profile.BRIARHEART) { briar(g, reduced); return; }
        switch (g.attack()) {
            case TARGET -> {
                double dx = g.targetX() - g.x(), dy = g.targetY() - g.y(), len = Math.max(1, Math.hypot(dx, dy));
                var arc = add(Kind.ARC, g.targetX(), g.targetY() + 24, .24, STEEL);
                arc.ax = dx / len; arc.ay = dy / len;
                fan(g.targetX(), g.targetY() + 24, g.slamRadius(), reduced ? 4 : 12);
                for (int i = 0; i < (reduced ? 3 : 8); i++) {
                    double a = i * Math.PI * 2 / (reduced ? 3 : 8) + rnd() * .4;
                    dust(g.targetX() + Math.cos(a) * 24, g.targetY() + 24 + Math.sin(a) * 14, Math.cos(a) * 90, Math.sin(a) * 50);
                }
            }
            case SHOCKWAVE -> {
                fan(g.x(), g.y() + FEET, 70, reduced ? 3 : 8);
                // Dust riding the wave front: it starts and travels exactly as Guardian.waveRadius() does.
                int count = reduced ? 8 : 18;
                double speed = (g.slamRadius() - Guardian.WAVE_START) / g.activeDuration();
                for (int i = 0; i < count; i++) {
                    double a = (i + rnd() * .5) * Math.PI * 2 / count;
                    var ride = add(Kind.RIDE, g.targetX() + Math.cos(a) * Guardian.WAVE_START, g.targetY() + Math.sin(a) * Guardian.WAVE_START,
                            g.activeDuration(), i % 2 == 0 ? DUST : DUST_DARK);
                    ride.vx = Math.cos(a) * speed; ride.vy = Math.sin(a) * speed;
                }
            }
            case SWEEP -> {
                var sweep = add(Kind.SWEEP, g.x(), g.y() + 30, .26, STEEL);
                sweep.ax = g.directionX() < 0 ? -1 : 1;
                sweep.span = Math.min(150, g.slamRadius() - 15);
                for (int i = 0; i < (reduced ? 3 : 10); i++) {                // grit thrown off the blade's path
                    double a = rnd() * Math.PI * 2, r = sweep.span * (.6 + rnd() * .3), turn = sweep.ax;
                    var grit = add(Kind.CHIP, g.x() + Math.cos(a) * r, g.y() + 30 + Math.sin(a) * r * .55, .5, stone());
                    grit.vx = -Math.sin(a) * turn * (40 + rnd() * 50); grit.vy = Math.cos(a) * turn * (20 + rnd() * 30); grit.vz = 60 + rnd() * 80;
                }
            }
            case CHARGE -> {
                double ux = g.directionX(), uy = g.directionY();
                for (int i = 0; i < (reduced ? 2 : 6); i++) {                  // the push-off
                    var chip = add(Kind.CHIP, g.x() - ux * 24 + (rnd() - .5) * 40, g.y() + FEET, .5, stone());
                    chip.vx = -ux * (60 + rnd() * 60) + (rnd() - .5) * 40; chip.vy = -uy * (30 + rnd() * 30); chip.vz = 90 + rnd() * 80;
                }
                for (int i = 0; i < (reduced ? 1 : 4); i++) dust(g.x() - ux * 30 + (rnd() - .5) * 50, g.y() + FEET, -ux * 80, -uy * 40);
                wakeClock = 0;
            }
            case FISSURE -> {
                var lane = g.fissureBounds();
                if (lane == null) return;
                boolean across = lane.width() > lane.height();
                double length = Math.max(lane.width(), lane.height()), half = Math.min(lane.width(), lane.height()) / 2;
                var split = add(Kind.SPLIT, lane.centerX(), lane.centerY(), 1.5, CRACK);
                split.ax = across ? 1 : 0; split.ay = across ? 0 : 1; split.span = length;
                split.origin = Math.max(-length / 2, Math.min(length / 2, across ? g.x() - lane.centerX() : g.y() - lane.centerY()));
                split.vx = (int) (rnd() * 1000);                              // the split's own jag pattern
                for (double s = -length / 2 + 20; s < length / 2; s += reduced ? 92 : 46) {
                    double side = (rnd() - .5) * half;                        // fragments stay inside the lane
                    var chip = add(Kind.CHIP, lane.centerX() + (across ? s : side), lane.centerY() + (across ? side : s), .8, stone());
                    chip.age = -Math.abs(s - split.origin) / length * .25;
                    chip.vz = 160 + rnd() * 140;
                    chip.vx = across ? (rnd() - .5) * 30 : (rnd() - .5) * 20; chip.vy = across ? (rnd() - .5) * 20 : (rnd() - .5) * 30;
                }
            }
        }
    }

    private void briar(Guardian g, boolean reduced) {
        double foot = Player.COLLISION_Y_OFFSET;
        if (g.forestAttack() == Guardian.ForestAttack.SEED_BLOOM) {
            for (var bloom : g.forestSeeds()) {
                int count = reduced ? 4 : 10;
                for (int i = 0; i < count; i++) {                             // husks land well inside the bloom
                    double a = (i + rnd() * .6) * Math.PI * 2 / count, speed = (.3 + .5 * rnd()) * bloom.radius();
                    var husk = add(Kind.SEED, bloom.x(), bloom.y() + foot, .7, i % 3 == 0 ? SEED_GOLD : HUSK);
                    husk.z = 6; husk.vx = Math.cos(a) * speed; husk.vy = Math.sin(a) * speed * .7; husk.vz = 110 + rnd() * 90;
                }
                if (reduced) continue;
                for (int i = 0; i < 4; i++) {                                 // the split pod's shell
                    double a = i * Math.PI / 2 + rnd() * .5;
                    var shell = add(Kind.SPLINTER, bloom.x(), bloom.y() + foot, .45, BARK);
                    shell.z = 4; shell.vx = Math.cos(a) * 50; shell.vy = Math.sin(a) * 35; shell.vz = 140 + rnd() * 40;
                }
                var leaf = add(Kind.LEAF, bloom.x(), bloom.y() + foot, 1.0, LEAVES[0]);
                leaf.z = 24; leaf.origin = rnd() * 6;
            }
            return;
        }
        for (var lane : g.forestRoots()) {                                      // the soil bursts where the roots come up
            boolean across = lane.width() > lane.height();
            double length = Math.max(lane.width(), lane.height()), half = Math.min(lane.width(), lane.height()) / 2;
            for (double s = -length / 2 + 24; s < length / 2; s += reduced ? 128 : 64) {
                double side = (rnd() - .5) * half;
                double px = lane.centerX() + (across ? s : side), py = lane.centerY() + (across ? side : s) + foot;
                var clod = add(Kind.SOIL, px, py, .6, rnd() < .5 ? SOIL : SOIL_LIGHT);
                clod.vz = 100 + rnd() * 100; clod.vx = (rnd() - .5) * 30; clod.vy = (rnd() - .5) * 20;
                if (reduced) continue;
                var fibre = add(Kind.FIBER, px, py, .55, FIBRE);
                fibre.vz = 140 + rnd() * 80; fibre.vx = (rnd() - .5) * 50; fibre.vy = (rnd() - .5) * 20;
            }
        }
    }

    private int stone() { return STONE[(int) (rnd() * STONE.length)]; }
    private void fan(double x, double y, double reach, int count) {
        for (int i = 0; i < count; i++) {
            double a = (i + rnd() * .7) * Math.PI * 2 / count, speed = (.35 + .5 * rnd()) * reach * .9;
            var chip = add(Kind.CHIP, x, y, .9 + rnd() * .3, stone());
            chip.vx = Math.cos(a) * speed; chip.vy = Math.sin(a) * speed * .6; chip.vz = 120 + rnd() * 120;
        }
    }
    private void dust(double x, double y, double vx, double vy) {
        var d = add(Kind.DUST, x, y, .45 + rnd() * .2, rnd() < .5 ? DUST : DUST_DARK);
        d.vx = vx; d.vy = vy;
    }

    // ------------------------------------------------------------------ drawing (native pixels, shown 2x)
    /** Flat marks under every actor: the ground split, the rush furrows, dust. */
    void drawGround(Graphics2D g, int cx, int cy) {
        for (var b : bits) {
            if (b.age < 0) continue;
            double t = b.age / b.life;
            switch (b.kind) {
                case DUST, RIDE -> {
                    if (t > .7 && ((int) (b.age * 60) & 1) == 1) continue;
                    g.setColor(new Color(b.colour));
                    blob(g, snap(b.x - cx), snap(b.y - cy), b.kind == Kind.RIDE ? 3 : (int) Math.round(2 + t * 3));
                }
                case WAKE -> {                                                  // a gouged furrow with its dirt thrown up beside it
                    int length = (int) Math.round((1 - t) * 12);
                    for (int i = 0; i < length; i++) {
                        int x = snap(b.x - cx - b.ax * i * 2), y = snap(b.y - cy - b.ay * i * 2);
                        g.setColor(new Color(STONE_DARK)); g.fillRect(x, y, 4, 2);
                        g.setColor(new Color(i % 2 == 0 ? STONE[2] : DUST)); g.fillRect(x, y - 2, 4, 2);
                    }
                }
                case SPLIT -> split(g, b, cx, cy);
                default -> { }
            }
        }
    }

    /** A jagged crack opening both ways from where the blow went in; it never leaves the fissure's lane. */
    private static void split(Graphics2D g, Bit b, int cx, int cy) {
        if (b.age > b.life - .4 && ((int) (b.age * 20) & 1) == 1) return;
        double reach = Math.min(1, b.age / .22) * b.span + 8;
        int pattern = (int) b.vx;
        for (int layer = 0; layer < 2; layer++) {
            g.setColor(new Color(layer == 0 ? CRACK_RIM : b.colour));
            for (double s = -b.span / 2 + 4; s < b.span / 2 - 4; s += 2) {
                if (Math.abs(s - b.origin) > reach) continue;
                double jag = jag(s, pattern);
                int x = snap(b.x - cx + b.ax * s - b.ay * jag), y = snap(b.y - cy + b.ay * s + b.ax * jag);
                if (layer == 0) g.fillRect(x - 2, y - 2, 6, 6); else g.fillRect(x, y, 2, 2);
                int knot = (int) Math.floor(s / 24);
                if (layer == 1 && Math.floorMod(knot * 31 + pattern, 3) == 0 && Math.abs(s - knot * 24) < 9) {
                    double side = Math.floorMod(knot + pattern, 2) == 0 ? 1 : -1, out = Math.abs(s - knot * 24);
                    g.fillRect(snap(b.x - cx + b.ax * s - b.ay * (jag + side * out)), snap(b.y - cy + b.ay * s + b.ax * (jag + side * out)), 2, 2);
                }
            }
        }
    }
    /** Zigzag offset in world px, within +-10: knots every 24 px with a hashed offset, straight between them. */
    private static double jag(double s, int pattern) {
        int knot = (int) Math.floor(s / 24);
        double f = s / 24 - knot;
        return knotOffset(knot, pattern) * (1 - f) + knotOffset(knot + 1, pattern) * f;
    }
    private static int knotOffset(int knot, int pattern) { return (Math.floorMod(knot * 73856093 ^ pattern * 19349663, 11) - 5) * 2; }

    /** Things with height join the world's depth order: thrown chips, seeds and fibres, the live roots and cage. */
    void queue(BiConsumer<Double, Runnable> sink, Graphics2D g, Guardian boss, int cx, int cy, boolean reduced) {
        for (var b : bits) if (b.age >= 0) switch (b.kind) {
            case CHIP, SOIL, SEED, SPLINTER, FIBER -> sink.accept(b.y, () -> thrown(g, b, cx, cy));
            default -> { }
        }
        if (boss.profile() != Guardian.Profile.BRIARHEART || !boss.alive() || boss.state() != Guardian.State.SLAM) return;
        double t = boss.stateSeconds(), foot = Player.COLLISION_Y_OFFSET;
        if (boss.forestAttack() == Guardian.ForestAttack.ROOT_WEAVE) {
            double rise = Math.min(1, t / .1) * Math.min(1, Math.max(0, boss.activeDuration() - t) / .18);
            for (var lane : boss.forestRoots()) {
                boolean across = lane.width() > lane.height();
                double length = Math.max(lane.width(), lane.height()), half = Math.min(lane.width(), lane.height()) / 2;
                int k = 0;
                for (double s = -length / 2 + 32; s < length / 2 - 16; s += reduced ? 96 : 64, k++) {
                    double side = (k % 2 == 0 ? .3 : -.3) * half;
                    double bx = lane.centerX() + (across ? s : side), by = lane.centerY() + (across ? side : s) + foot;
                    double height = (18 + (k * 7 % 3) * 5) * rise;
                    int phase = k;
                    sink.accept(by, () -> arch(g, bx - cx, by - cy, across ? 44 : 30, height, phase));
                }
            }
        } else if (boss.forestAttack() == Guardian.ForestAttack.BRAMBLE_CAGE) {
            double rise = Math.min(1, t / .25) * Math.min(1, Math.max(0, boss.activeDuration() - t) / .2), close = Math.min(1, t / .9);
            for (var wall : boss.forestRoots()) {
                boolean across = wall.width() > wall.height();
                double length = Math.max(wall.width(), wall.height());
                int k = 0;
                for (double s = -length / 2 + 18; s < length / 2; s += reduced ? 72 : 36, k++) {
                    double bx = wall.centerX() + (across ? s : 0), by = wall.centerY() + (across ? 0 : s) + foot;
                    double tx = boss.targetX() - wall.centerX(), ty = boss.targetY() - wall.centerY(), len = Math.max(1, Math.hypot(tx, ty));
                    double height = (30 + (k % 3) * 6) * rise, lean = (6 + 14 * close) * rise;
                    boolean shut = t >= .9;
                    sink.accept(by, () -> stalk(g, bx - cx, by - cy, height, tx / len * lean, ty / len * lean * .6, shut));
                }
            }
        }
    }

    /** A root bowing up out of the soil: dark underside, bark body, a lit crown, torn fibres hanging off it. */
    private static void arch(Graphics2D g, double bx, double by, double span, double height, int phase) {
        if (height < 2) return;
        for (int layer = 0; layer < 3; layer++) {
            g.setColor(new Color(layer == 0 ? BARK_DARK : layer == 1 ? BARK : BARK_LIGHT));
            for (int i = 0; i <= 12; i++) {
                double u = i / 12.0, x = bx + (u - .5) * span, y = by - Math.sin(Math.PI * u) * height;
                if (layer == 0) g.fillRect(snap(x) - 2, snap(y) - 2, 6, 6);
                else if (layer == 1) g.fillRect(snap(x) - 2, snap(y) - 2, 4, 4);
                else if (u > .2 && u < .8) g.fillRect(snap(x) - 2, snap(y) - 2, 2, 2);
            }
        }
        g.setColor(new Color(FIBRE));
        for (int i = 0; i < 3; i++) {
            double u = .3 + i * .2 + (phase % 2) * .05, x = bx + (u - .5) * span, y = by - Math.sin(Math.PI * u) * height;
            for (int k = 1; k <= 2 + (phase + i) % 2; k++) g.fillRect(snap(x + (i - 1) * k), snap(y + 2 + k * 2), 2, 2);
        }
    }

    /** A branch up out of the cage wall, its tip bending toward the trapped centre; thorned once it locks. */
    private static void stalk(Graphics2D g, double bx, double by, double height, double leanX, double leanY, boolean shut) {
        if (height < 2) return;
        int steps = (int) Math.max(2, height / 4);
        for (int layer = 0; layer < 2; layer++) {
            g.setColor(new Color(layer == 0 ? BARK_DARK : BARK));
            for (int i = 0; i <= steps; i++) {
                double u = i / (double) steps, bend = u * u;
                int x = snap(bx + leanX * bend), y = snap(by - height * u + leanY * bend);
                int w = u < .5 ? 4 : 2;
                if (layer == 0) g.fillRect(x - 2, y - 2, w + 4, 4); else g.fillRect(x, y - 2, w, 2);
            }
        }
        int tipX = snap(bx + leanX), tipY = snap(by - height + leanY);
        g.setColor(new Color(shut ? BARK_LIGHT : LEAVES[1]));
        g.fillRect(tipX, tipY - 2, 2, 2);
        if (shut) { g.fillRect(tipX - 2, tipY + 4, 2, 2); g.fillRect(tipX + 2, tipY + 8, 2, 2); }
    }

    private static void thrown(Graphics2D g, Bit b, int cx, int cy) {
        double t = b.age / b.life;
        if (t > .8 && ((int) (b.age * 24) & 1) == 1) return;
        int x = snap(b.x - cx), y = snap(b.y - b.z - cy);
        switch (b.kind) {
            case CHIP -> {                                                      // inked, so it reads on grass and flagstones
                int h = b.colour == STONE[2] ? 4 : 2;
                g.setColor(new Color(STONE_DARK)); g.fillRect(x - 2, y - 2, 8, h + 4);
                g.setColor(new Color(b.colour)); g.fillRect(x, y, 4, h);
                g.setColor(new Color(STONE_LIGHT)); g.fillRect(x, y, 2, 2);
            }
            case SOIL -> {
                g.setColor(new Color(b.colour)); g.fillRect(x, y, 4, 2);
                g.setColor(new Color(BARK_DARK)); g.fillRect(x + 2, y + 2, 2, 2);
            }
            case SEED -> {
                boolean flat = Math.abs(b.vx) > Math.abs(b.vy);
                g.setColor(new Color(BARK_DARK)); g.fillRect(x, y + 2, flat ? 4 : 2, 2);
                g.setColor(new Color(b.colour)); g.fillRect(x, y, flat ? 4 : 2, flat ? 2 : 4);
            }
            case SPLINTER -> {
                boolean flat = Math.abs(b.vx) > Math.abs(b.vy);
                g.setColor(new Color(b.colour)); g.fillRect(x, y, flat ? 6 : 2, flat ? 2 : 6);
                g.setColor(new Color(BARK_DARK)); g.fillRect(x, y, 2, 2);
            }
            case FIBER -> {
                double len = Math.max(1, Math.hypot(b.vx, b.vz));
                g.setColor(new Color(b.colour));
                for (int i = 0; i < 3; i++) g.fillRect(snap(x - b.vx / len * i * 2), snap(y + b.vz / len * i * 2), 2, 2);
            }
            default -> { }
        }
    }

    /** Above the actors: the blade's air arc and sweep, the locking glint, falling leaves. */
    void drawAbove(Graphics2D g, int cx, int cy, boolean reduced) {
        for (var b : bits) {
            if (b.age < 0) continue;
            double t = b.age / b.life;
            int x = snap(b.x - cx), y = snap(b.y - cy);
            switch (b.kind) {
                case ARC -> {                                                   // the blow's air, cut short on the ground
                    if (!reduced && t < .35) {
                        g.setColor(new Color(STEEL));
                        double drop = Math.min(1, t / .2);
                        for (int i = 0; i < 6; i++) g.fillRect(x, snap(y - 70 + drop * 64 - i * 6), 2, 4);
                    }
                    double reach = 22 + t * 24, base = Math.atan2(b.ay, b.ax);
                    for (int layer = 0; layer < 2; layer++) {
                        g.setColor(new Color(layer == 0 ? INK : t < .4 ? STEEL : GLOW));
                        for (double a = -1.1; a <= 1.1; a += .08) {
                            int px = snap(x + Math.cos(base + a) * reach), py = snap(y + Math.sin(base + a) * reach * .6);
                            if (layer == 0) g.fillRect(px - 2, py - 2, 6, 6); else g.fillRect(px, py, t < .5 ? 4 : 2, 2);
                        }
                    }
                }
                case SWEEP -> sweep(g, b, x, y, t, reduced);
                case GLINT -> {
                    int arm = t < .35 ? 1 + (int) (t / .35 * 4) : (int) Math.round((1 - t) / .65 * 5);
                    if (arm <= 0) continue;
                    g.setColor(new Color(STEEL));
                    for (int i = -arm; i <= arm; i++) { g.fillRect(x + i * 2, y, 2, 2); g.fillRect(x, y + i * 2, 2, 2); }
                    g.setColor(new Color(GLOW));
                    g.fillRect(x - 2, y - 2, 2, 2); g.fillRect(x + 2, y - 2, 2, 2); g.fillRect(x - 2, y + 2, 2, 2); g.fillRect(x + 2, y + 2, 2, 2);
                }
                case LEAF -> {
                    if (t > .75 && ((int) (b.age * 24) & 1) == 1) continue;
                    int ly = snap(b.y - b.z - cy);
                    boolean turned = Math.sin(b.age * 6 + b.origin) > 0;
                    g.setColor(new Color(b.colour)); g.fillRect(x, ly, turned ? 4 : 2, 2);
                    g.setColor(new Color(LEAVES[1])); g.fillRect(x + (turned ? 2 : 0), ly + 2, 2, 2);
                }
                default -> { }
            }
        }
    }

    /** The blade's path round the Warden, inside its sweep radius: a bright head, a cooling trail behind it. */
    private static void sweep(Graphics2D g, Bit b, int x, int y, double t, boolean reduced) {
        double turn = b.ax, head = (turn < 0 ? 0 : Math.PI) + turn * Math.PI * 2 * Math.min(1, t / .8);
        double trail = (reduced ? .8 : 1.6) * (1 - Math.max(0, t - .7) / .3);
        for (int layer = 0; layer < 2; layer++) for (double k = 0; k <= 1; k += .01) {
            if (layer == 0 && k > .4) break;
            double angle = head - turn * k * trail, r = b.span * (1 - .08 * k);
            int px = snap(x + Math.cos(angle) * r), py = snap(y + Math.sin(angle) * r * .55);
            int qx = snap(x + Math.cos(angle) * (r - 8)), qy = snap(y + Math.sin(angle) * (r - 8) * .55);
            if (layer == 0) { g.setColor(new Color(INK)); g.fillRect(px - 2, py - 2, 6, 6); g.fillRect(qx - 2, qy - 2, 6, 6); continue; }
            g.setColor(new Color(k < .25 ? STEEL : k < .6 ? GLOW : GLOW_DARK));
            g.fillRect(px, py, 2, 2);
            if (k < .5) { g.setColor(new Color(k < .15 ? STEEL : GLOW_DARK)); g.fillRect(qx, qy, 2, 2); }   // the blade's breadth
        }
    }

    private static int snap(double v) { return (int) Math.floor(v / 2) * 2; }
    private static void blob(Graphics2D g, int x, int y, int r) {
        for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++)
            if (dx * dx + dy * dy <= r * r + r) g.fillRect(x + dx * 2, y + dy * 2, 2, 2);
    }
}
