import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Visual-only spectacle round the Outpost Warden: the wake-up shock, slam cracks and flying rubble,
 * the spin-cleave smear, charge afterimages, spikes erupting along shockwaves and fissures, enrage
 * flames, a white hit flash, and a death of light that leaves the fallen Warden in the arena.
 * It only reads the Guardian's public state, so the fight itself is untouched.
 */
final class WardenFx {
    /**
     * Whose fight this is: its effect sheets (assets/effects/<prefix>_*.png), its name card, the colour its
     * rage burns, its motion atlas's cell size, and whether the Outpost's arena braziers answer it.
     */
    record Theme(String prefix, String name, String title, int glow, int cell, boolean braziers) {
        static final Theme WARDEN = new Theme("warden", "OUTPOST WARDEN", "KEEPER OF THE EAST GATE", 0x7aeee6, 64, true);
        static final Theme BRIARHEART = new Theme("briar", "BRIARHEART", "ROOT OF THE DESCENT", 0xffdd80, 96, false);
    }
    private final Theme theme;
    private final int cell, foot, renderSize;
    private final BufferedImage DEBRIS, DUST, CRACK, SWEEP, FLAME, SPIKE, BEAM, MOTE;
    private static final int LIMIT = 260;

    private enum Kind { DEBRIS, DUST, MOTE_CYAN, MOTE_GOLD, SPIKE, GHOST, BEAM, FIRE_MOTE }
    private static final class Bit {
        final Kind kind; double x, y, z, vx, vy, vz, age; final double life; final int variant;
        int ghostRow, ghostFrame;
        Bit(Kind kind, double x, double y, double life, int variant) { this.kind = kind; this.x = x; this.y = y; this.life = life; this.variant = variant; }
    }

    private final BufferedImage atlas, white, cyan;
    private final List<Bit> bits = new ArrayList<>();
    private final List<double[]> cracks = new ArrayList<>();          // x, y, age
    private Guardian.State last = Guardian.State.DORMANT;
    private Guardian.Attack lastAttack = Guardian.Attack.TARGET;
    private boolean wasEnraged, fallen;
    private double awakeAge = -1, sweepAge = -1, deathAge = -1, enrageAge = -1, ghostClock, waveDone, fallenX, fallenY;
    private long seed = 1;

    /** Flat white and cyan copies of an atlas, built once and shared by every restart; dropped with the atlas. */
    private static final java.util.Map<BufferedImage, BufferedImage[]> SILHOUETTES = new java.util.WeakHashMap<>();

    WardenFx(BufferedImage motionAtlas) { this(motionAtlas, Theme.WARDEN); }

    WardenFx(BufferedImage motionAtlas, Theme theme) {
        this.theme = theme;
        cell = theme.cell();
        renderSize=cell==96?192:Guardian.RENDER_SIZE;
        foot = cell * Guardian.SPRITE_FOOT_ROW / Guardian.CELL_SIZE;   // the foot row scales with the cell
        String base = "assets/effects/" + theme.prefix() + "_";
        DEBRIS = B2BJ.loadImage(base + "debris.png"); DUST = B2BJ.loadImage(base + "dust.png");
        CRACK = B2BJ.loadImage(base + "crack.png"); SWEEP = B2BJ.loadImage(base + "sweep.png");
        FLAME = B2BJ.loadImage(base + "flame.png"); SPIKE = B2BJ.loadImage(base + "spike.png");
        BEAM = B2BJ.loadImage(base + "beam.png"); MOTE = B2BJ.loadImage(base + "mote.png");
        atlas = motionAtlas;
        var tints = motionAtlas == null ? null
                : SILHOUETTES.computeIfAbsent(motionAtlas, a -> new BufferedImage[]{silhouette(a, 0xfff6ff), silhouette(a, theme.glow())});
        white = tints == null ? null : tints[0];
        cyan = tints == null ? null : tints[1];
    }

    private static BufferedImage silhouette(BufferedImage source, int rgb) {
        var out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++)
            if ((source.getRGB(x, y) >>> 24) > 0) out.setRGB(x, y, 0xff000000 | rgb);
        return out;
    }

    private double rnd() { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed >>> 11) / (double) (1L << 53); }

    private Bit add(Kind kind, double x, double y, double life, int variant) {
        if (bits.size() >= LIMIT) bits.remove(0);
        var b = new Bit(kind, x, y, life, variant);
        bits.add(b);
        return b;
    }

    // ------------------------------------------------------------------ triggers
    void awaken(Guardian g) {
        awakeAge = 0;
        burstRing(g.x(), g.y() + 20, 36, Kind.MOTE_CYAN, 340);
        for (int i = 0; i < 12; i++) dust(g.x() + (rnd() - .5) * 150, g.y() + 30 + (rnd() - .5) * 40, (rnd() - .5) * 160, (rnd() - .5) * 60);
        if (theme.braziers()) for (var p : SetPieces.pieces(0)) if (SetPieces.arenaBrazier(p))      // the arena braziers flare alight
            for (int i = 0; i < 10; i++) {
                var m = add(Kind.FIRE_MOTE, p.x() + (rnd() - .5) * 16, p.y() - 60, .55 + rnd() * .3, i % 4);
                m.vx = (rnd() - .5) * 90; m.vy = -60 - rnd() * 120;
            }
    }

    void slam(Guardian g, double x, double y) {
        switch (g.attack()) {
            case TARGET -> impact(x, y, 12, true);
            case SHOCKWAVE -> { impact(g.x(), g.y() + 30, 8, true); waveDone = 0; }
            case SWEEP -> { sweepAge = 0; for (int i = 0; i < 10; i++) dust(g.x() + (rnd() - .5) * 220, g.y() + 30 + (rnd() - .5) * 90, 0, 0); }
            case CHARGE -> { for (int i = 0; i < 8; i++) dust(g.x() - g.directionX() * 30 + (rnd() - .5) * 60, g.y() + 40, -g.directionX() * 140, -g.directionY() * 60); ghostClock = 0; }
            case FISSURE -> {
                var lane = g.fissureBounds();
                if (lane != null) {
                    boolean across = lane.width() > lane.height();
                    double length = Math.max(lane.width(), lane.height());
                    for (double s = -length / 2 + 24; s < length / 2; s += 46) {
                        double px = across ? lane.centerX() + s : lane.centerX() + (rnd() - .5) * 30;
                        double py = across ? lane.centerY() + (rnd() - .5) * 30 : lane.centerY() + s;
                        add(Kind.SPIKE, px, py, .5, 0).age = -Math.abs(s) / length * .25;
                        if (rnd() < .45) cracks.add(new double[]{px, py, 0});
                    }
                }
            }
        }
    }

    private void impact(double x, double y, int chunks, boolean crack) {
        if (crack) { cracks.add(new double[]{x, y, 0}); if (cracks.size() > 14) cracks.remove(0); }
        for (int i = 0; i < chunks; i++) {
            double a = rnd() * Math.PI * 2, speed = 110 + rnd() * 170;
            var d = add(Kind.DEBRIS, x, y, 1.3 + rnd() * .5, (int) (rnd() * 6));
            d.vx = Math.cos(a) * speed; d.vy = Math.sin(a) * speed * .6; d.vz = 170 + rnd() * 230;
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + rnd() * .3;
            dust(x + Math.cos(a) * 26, y + Math.sin(a) * 14, Math.cos(a) * 120, Math.sin(a) * 60);
        }
    }

    private void dust(double x, double y, double vx, double vy) {
        var d = add(Kind.DUST, x, y, .55 + rnd() * .2, 0);
        d.vx = vx; d.vy = vy;
    }

    private void burstRing(double x, double y, int count, Kind kind, double speed) {
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            var m = add(kind, x, y, .6, i % 4);
            m.vx = Math.cos(a) * speed; m.vy = Math.sin(a) * speed * .6;
        }
    }

    // ------------------------------------------------------------------ simulation
    void update(double dt, Guardian g) {
        for (Iterator<Bit> it = bits.iterator(); it.hasNext(); ) {
            var b = it.next();
            b.age += dt;
            if (b.age >= b.life) { it.remove(); continue; }
            if (b.age < 0) continue;
            switch (b.kind) {
                case DEBRIS -> {
                    b.vz -= 980 * dt; b.z += b.vz * dt;
                    if (b.z < 0) { b.z = 0; b.vz = -b.vz * .3; b.vx *= .45; b.vy *= .45; }
                    b.x += b.vx * dt; b.y += b.vy * dt;
                }
                case DUST -> { b.x += b.vx * dt; b.y += b.vy * dt; b.vx *= Math.pow(.05, dt); b.vy *= Math.pow(.05, dt); }
                case MOTE_CYAN, MOTE_GOLD, FIRE_MOTE -> {
                    b.x += b.vx * dt; b.y += b.vy * dt;
                    b.vx *= Math.pow(.2, dt); b.vy = b.vy * Math.pow(.2, dt) - (b.kind == Kind.MOTE_GOLD ? 40 * dt : 0);
                }
                default -> { }
            }
        }
        for (Iterator<double[]> it = cracks.iterator(); it.hasNext(); ) { var c = it.next(); c[2] += dt; if (c[2] > 4.5) it.remove(); }
        if (awakeAge >= 0) awakeAge += dt;
        if (sweepAge >= 0) { sweepAge += dt; if (sweepAge > .42) sweepAge = -1; }
        if (enrageAge >= 0) { enrageAge += dt; if (enrageAge > 1.2) enrageAge = -1; }
        if (deathAge >= 0) deathAge += dt;
        var state = g.state();
        if (state == Guardian.State.TELEGRAPH && g.alive()) {               // power gathering into the visor
            double rate = 34 * dt;
            while (rnd() < rate--) {
                double a = rnd() * Math.PI * 2, r = 70 + rnd() * 50;
                var m = add(Kind.MOTE_CYAN, g.x() + Math.cos(a) * r, g.y() - 70 + Math.sin(a) * r * .6, .42, (int) (rnd() * 4));
                m.vx = -Math.cos(a) * r / .42; m.vy = -Math.sin(a) * r * .6 / .42;
            }
        }
        if (state == Guardian.State.SLAM && g.attack() == Guardian.Attack.CHARGE) {
            ghostClock -= dt;
            if (ghostClock <= 0) {
                ghostClock = .05;
                var ghost = add(Kind.GHOST, g.x(), g.y(), .26, 0);
                ghost.ghostRow = g.animation().ordinal(); ghost.ghostFrame = g.animationFrame();
                dust(g.x() + (rnd() - .5) * 40, g.y() + 44, -g.directionX() * 70, -g.directionY() * 30);
            }
        }
        if (state == Guardian.State.SLAM && g.attack() == Guardian.Attack.SHOCKWAVE) {   // spikes ride the wave front
            double radius = g.waveRadius();
            while (waveDone + 58 <= radius) {
                waveDone += 58;
                int count = Math.max(8, (int) (waveDone * Math.PI * 2 / 78));
                double turn = rnd();
                for (int i = 0; i < count; i++) {
                    double a = (i + turn) * Math.PI * 2 / count;
                    add(Kind.SPIKE, g.targetX() + Math.cos(a) * waveDone, g.targetY() + Math.sin(a) * waveDone, .42, 0);
                }
            }
        }
        if (last == Guardian.State.SLAM && state != Guardian.State.SLAM && lastAttack == Guardian.Attack.CHARGE)
            for (int i = 0; i < 7; i++) dust(g.x() + (rnd() - .5) * 70, g.y() + 40, g.directionX() * 90, g.directionY() * 40);
        if (g.alive() && state != Guardian.State.DORMANT && g.enraged() && !wasEnraged) {   // the rage breaks loose
            enrageAge = 0;
            burstRing(g.x(), g.y() - 40, 40, Kind.MOTE_CYAN, 420);
            impact(g.x(), g.y() + 30, 10, true);
        }
        if (state == Guardian.State.DEAD && last != Guardian.State.DEAD) {
            deathAge = 0; fallen = true; fallenX = g.x(); fallenY = g.y();
            burstRing(g.x(), g.y() - 50, 48, Kind.MOTE_CYAN, 520);
            for (int i = 0; i < 18; i++) {
                double a = rnd() * Math.PI * 2, speed = 120 + rnd() * 220;
                var d = add(Kind.DEBRIS, g.x(), g.y() + 10, 2 + rnd(), 2 + (int) (rnd() * 4));
                d.vx = Math.cos(a) * speed; d.vy = Math.sin(a) * speed * .6; d.vz = 260 + rnd() * 260;
            }
            for (int i = 0; i < 5; i++) add(Kind.BEAM, g.x() + (rnd() - .5) * 110, g.y() + 20 + (rnd() - .5) * 30, 1.5 + rnd() * .4, i % 4).age = -i * .07;
        }
        if (deathAge >= 0 && deathAge < 2.6 && rnd() < 60 * dt) {          // the stolen Ichor rises free
            var m = add(Kind.MOTE_GOLD, fallenX + (rnd() - .5) * 120, fallenY + (rnd() - .5) * 40, 1.6, (int) (rnd() * 4));
            m.vx = (rnd() - .5) * 30; m.vy = -50 - rnd() * 60;
        }
        last = state; lastAttack = g.attack(); wasEnraged = g.enraged();
    }

    boolean fallen() { return fallen; }
    int particles() { return bits.size(); }

    // ------------------------------------------------------------------ drawing
    /** Below the actors: cracks, dust, the fallen Warden's remains. */
    void drawGround(Graphics2D g, Guardian guardian, int cx, int cy) {
        if (guardian.state() == Guardian.State.TELEGRAPH && DUST != null) {
            int f = (int) (guardian.stateSeconds() * 10) % 3;
            if (guardian.attack() == Guardian.Attack.TARGET)
                for (int side = -1; side <= 1; side++)
                    cell(g, DUST, 28, f, guardian.targetX() + side * 40, guardian.targetY() + (side == 0 ? 40 : 10), cx, cy, 14, 18);
            var lane = guardian.fissureBounds();
            if (lane != null) {
                boolean across = lane.width() > lane.height();
                double length = Math.max(lane.width(), lane.height());
                for (double s = -length / 2 + 24; s < length / 2; s += 46)
                    cell(g, DUST, 28, f, lane.centerX() + (across ? s : 0), lane.centerY() + (across ? 0 : s), cx, cy, 14, 18);
            }
        }
        if (CRACK != null) for (double[] c : cracks) {
            int f = c[2] < .12 ? 0 : c[2] < .4 ? 1 : c[2] < 1.1 ? 2 : 3;
            if (c[2] > 3.8 && ((int) (c[2] * 20)) % 2 == 0) continue;
            cell(g, CRACK, 80, f, c[0], c[1] + 30, cx, cy, 40, 20);
        }
        if (fallen && !guardian.visible() && atlas != null) {
            int size = renderSize;
            int x = (int) Math.round(fallenX) - cx - size / 2;
            int floorY = (int) Math.round(fallenY) - cy + Guardian.GROUND_Y_OFFSET
                    + (cell - foot) * renderSize / cell;
            int sx = 7 * cell, sy = Guardian.Animation.DEATH.ordinal() * cell;
            g.drawImage(atlas, x, floorY - size, x + size, floorY, sx, sy, sx + cell, sy + cell, null);
        }
        for (var b : bits) if (b.kind == Kind.DUST && b.age >= 0 && DUST != null)
            cell(g, DUST, 28, Math.min(5, (int) (b.age / b.life * 6)), b.x, b.y, cx, cy, 14, 18);
    }

    /**
     * Things standing on the floor go into the world's depth order, so actors pass in front and behind.
     * Flames and flashes cling to the Warden's body: they sort just after it, so whatever stands in
     * front of the Warden still covers them.
     */
    void queue(java.util.function.BiConsumer<Double, Runnable> sink, Graphics2D g, Guardian guardian, int cx, int cy, long frame, boolean reduced) {
        if (guardian.visible()) sink.accept(guardian.y() + Guardian.GROUND_Y_OFFSET + 1e-3, () -> body(g, guardian, cx, cy, frame, reduced));
        for (var b : bits) if (b.age >= 0) switch (b.kind) {
            case GHOST -> sink.accept(b.y + Guardian.GROUND_Y_OFFSET, () -> ghost(g, b, cx, cy));
            case SPIKE -> { if (SPIKE != null) { int f = Math.min(5, (int) (b.age / b.life * 6)); sink.accept(b.y, () -> cell(g, SPIKE, 20, f, b.x, b.y, cx, cy, 10, 28)); } }
            case DEBRIS -> {
                if (DEBRIS == null || (b.age > b.life - .35 && ((int) (b.age * 24)) % 2 == 0)) break;
                sink.accept(b.y, () -> {
                    int sx = (int) Math.round(b.x) - cx - 10, sy = (int) Math.round(b.y - b.z) - cy - 10;
                    g.drawImage(DEBRIS, sx, sy, sx + 20, sy + 20, b.variant * 10, 0, b.variant * 10 + 10, 10, null);
                });
            }
            default -> { }
        }
    }

    /** Enrage flames, the hit flash (plain blink when effects are reduced) and the enrage flash, on the Warden's pose. */
    private void body(Graphics2D g, Guardian guardian, int cx, int cy, long frame, boolean reduced) {
        if (guardian.alive() && guardian.state() != Guardian.State.DORMANT && guardian.enraged() && FLAME != null) {
            int[][] spots = {{13, 32}, {51, 32}};
            int left = (int) Math.round(guardian.x()) - cx - renderSize / 2;
            int top = topOf(guardian, cy);
            for (int i = 0; i < spots.length; i++) {
                int f = (int) ((frame / 4 + i * 3) % 8);
                int fx=left+spots[i][0]*renderSize/64,fy=top+spots[i][1]*renderSize/64;
                g.drawImage(FLAME,fx-14,fy-36,fx+14,fy+12,f*14,0,f*14+14,24,null);
            }
        }
        if (reduced || white == null) return;
        if (guardian.alive() && !guardian.flashVisible()) frameOf(g, white, guardian, cx, cy, 1f);
        if (enrageAge >= 0 && enrageAge < .3) frameOf(g, ((int) (enrageAge * 30)) % 2 == 0 ? white : cyan, guardian, cx, cy, .85f);
    }

    /** Above the actors: one complete cleave, motes and the death beams. */
    void drawAbove(Graphics2D g, Guardian guardian, int cx, int cy, long frame) {
        if (sweepAge >= 0 && SWEEP != null && guardian.visible())
            cell(g, SWEEP, 184, Math.min(3,(int)(sweepAge/.105)), guardian.x(), guardian.y() + 30, cx, cy, 92, 58);
        for (var b : bits) if (b.age >= 0) switch (b.kind) {
            case MOTE_CYAN, MOTE_GOLD, FIRE_MOTE -> {
                if (MOTE == null) break;
                int row = b.kind == Kind.MOTE_CYAN ? 0 : 4;
                int f = row + (int) ((b.age * 12 + b.variant) % 4);
                int sx = (int) Math.round(b.x / 2) * 2 - cx - 6, sy = (int) Math.round(b.y / 2) * 2 - cy - 6;
                g.drawImage(MOTE, sx, sy, sx + 12, sy + 12, f * 6, 0, f * 6 + 6, 6, null);
            }
            case BEAM -> {
                if (BEAM == null) break;
                double grow = Math.min(1, b.age / .35), fade = b.age > b.life - .4 ? (b.life - b.age) / .4 : 1;
                int h = (int) (280 * grow), w = (int) (56 * Math.max(.2, fade));
                int sx = (int) Math.round(b.x) - cx - w / 2, sy = (int) Math.round(b.y) - cy - h;
                int f = (int) (frame / 3 % 4);
                g.drawImage(BEAM, sx, sy, sx + w, sy + h, f * 28, 0, f * 28 + 28, 140, null);
            }
            default -> { }
        }
    }

    private void ghost(Graphics2D g, Bit b, int cx, int cy) {
        if (cyan == null) return;
        var faded = (Graphics2D) g.create();
        faded.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, .42 * (1 - b.age / b.life))));
        int size = renderSize, x = (int) Math.round(b.x) - cx - size / 2;
        int floorY = (int) Math.round(b.y) - cy + Guardian.GROUND_Y_OFFSET
                + (cell - foot) * renderSize / cell;
        int sx = b.ghostFrame * cell, sy = b.ghostRow * cell;
        faded.drawImage(cyan, x, floorY - size, x + size, floorY, sx, sy, sx + cell, sy + cell, null);
        faded.dispose();
    }

    private int topOf(Guardian guardian, int cy) {
        int floorY = (int) Math.round(guardian.y()) - cy + Guardian.GROUND_Y_OFFSET
                + (cell - foot) * renderSize / cell;
        return floorY - renderSize;
    }

    /** The Warden's current atlas pose drawn from a silhouette sheet, in the same place drawGuardian puts it. */
    private void frameOf(Graphics2D g, BufferedImage sheet, Guardian guardian, int cx, int cy, float alpha) {
        if (alpha <= 0) return;
        var layer = (Graphics2D) g.create();
        if (alpha < 1) layer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        int size = renderSize, x = (int) Math.round(guardian.x()) - cx - size / 2, y = topOf(guardian, cy);
        int sx = guardian.animationFrame() * cell, sy = guardian.animation().ordinal() * cell;
        layer.drawImage(sheet, x, y, x + size, y + size, sx, sy, sx + cell, sy + cell, null);
        layer.dispose();
    }

    private static void cell(Graphics2D g, BufferedImage sheet, int cellW, int f, double x, double y, int cx, int cy, int anchorX, int anchorY) {
        int h = sheet.getHeight();
        int sx = (int) Math.round(x / 2) * 2 - cx - anchorX * 2, sy = (int) Math.round(y / 2) * 2 - cy - anchorY * 2;
        g.drawImage(sheet, sx, sy, sx + cellW * 2, sy + h * 2, f * cellW, 0, (f + 1) * cellW, h, null);
    }

    /** The boss's name card, shown for a few seconds as it wakes: a dark band, the name, a flourish that opens. */
    void drawTitle(Graphics2D g, int width, int height) {
        if (awakeAge < .3 || awakeAge > 3.0) return;
        double t = awakeAge - .3;
        float alpha = (float) Math.min(1, Math.min(t / .3, (2.7 - t) / .45));
        if (alpha <= 0) return;
        var card = (Graphics2D) g.create();
        card.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        Ui.crisp(card);
        int cy = height - 214;
        for (int[] band : new int[][]{{cy - 30, 12, 80}, {cy - 18, 136, 200}, {cy + 118, 12, 80}}) {   // soft top and bottom
            Ui.fade(card, 0, band[0], width / 2, band[1], 0, band[2]);
            Ui.fade(card, width / 2, band[0], width / 2, band[1], band[2], 0);
        }
        String name = theme.name().toUpperCase(java.util.Locale.ROOT);
        Ui.drawTitleCentered(card, name, width / 2, cy, 4, Ui.GOLD_RAMP);
        int open = (int) Math.min(Ui.Piece.FLOURISH.w, 20 + t * 220);         // the flourish unfurls from its centre
        var clip = (Graphics2D) card.create();
        clip.clipRect(width / 2 - open, cy + 70, open * 2, 40);
        Ui.piece(clip, Ui.Piece.FLOURISH, width / 2 - Ui.Piece.FLOURISH.w, cy + 72);
        clip.dispose();
        Ui.textCentered(card, theme.title(), width / 2, cy + 124, 2, new Color(theme.glow()));
        card.dispose();
    }
}
