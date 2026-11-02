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
        static final Theme WARDEN = new Theme("warden", "OUTPOST WARDEN", "KEEPER OF THE EAST GATE", 0x7aeee6, 128, true);
        static final Theme BRIARHEART = new Theme("briar", "BRIARHEART", "ROOT OF THE DESCENT", 0xffdd80, 288, false);
    }
    private final Theme theme;
    private final int cell, foot, renderSize;
    private final BufferedImage DEBRIS, DUST, CRACK, SWEEP, FLAME, SPIKE, BEAM, MOTE;
    private static final int LIMIT = 260;

    private enum Kind { DEBRIS, DUST, MOTE_CYAN, MOTE_GOLD, SPIKE, GHOST, BEAM, FIRE_MOTE }
    private static final class Bit {
        final Kind kind; double x, y, z, vx, vy, vz, age; final double life; final int variant;
        int ghostRow, ghostFrame;
        Guardian.Attack ghostAttack;
        boolean ghostFlip;
        Bit(Kind kind, double x, double y, double life, int variant) { this.kind = kind; this.x = x; this.y = y; this.life = life; this.variant = variant; }
    }

    private final BufferedImage atlas, white, cyan;
    private final java.util.Map<Guardian.Attack,BufferedImage[]> actionTints=new java.util.EnumMap<>(Guardian.Attack.class);
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
        cell = motionAtlas != null && motionAtlas.getWidth() >= 8 ? motionAtlas.getWidth() / 8 : theme.cell();
        renderSize=cell*2;                                                // every boss atlas is drawn at exactly 2x
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
        if(theme==Theme.WARDEN)for(var attack:new Guardian.Attack[]{Guardian.Attack.SWEEP,Guardian.Attack.CHARGE,Guardian.Attack.SHOCKWAVE}) {
            String name=attack==Guardian.Attack.SHOCKWAVE?"wave":attack.name().toLowerCase(java.util.Locale.ROOT);
            var source=B2BJ.loadImage("assets/characters/guardian/guardian_"+name+".png");
            if(source!=null)actionTints.put(attack,SILHOUETTES.computeIfAbsent(source,
                    a->new BufferedImage[]{silhouette(a,0xfff6ff),silhouette(a,theme.glow())}));
        }
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
        if(g.profile()==Guardian.Profile.BRIARHEART) {
            for(var root:g.forestRoots()) {
                boolean across=root.width()>root.height();double length=Math.max(root.width(),root.height());
                for(double s=-length/2+20;s<length/2;s+=64)
                    dust(root.centerX()+(across?s:0),root.centerY()+24+(across?0:s),0,-24);
            }
            for(var seed:g.forestSeeds())burstRing(seed.x(),seed.y()+24,12,Kind.MOTE_GOLD,100);
            return;
        }
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
        if ((state == Guardian.State.TELEGRAPH||state==Guardian.State.ENRAGE) && g.alive()) {
            double focusX=g.x(),focusY=g.y()-70;
            if(theme==Theme.WARDEN&&state!=Guardian.State.ENRAGE)switch(g.attack()) {
                case TARGET -> focusY=g.y()-144;
                case SWEEP -> {focusX+=g.directionX()<0?64:-64;focusY=g.y()-66;}
                case CHARGE -> {focusX+=g.directionX()*28;focusY=g.y()-112+g.directionY()*16;}
                case SHOCKWAVE,FISSURE -> focusY=g.y()+24;
            }
            double rate = (state==Guardian.State.ENRAGE?18:34) * dt;
            while (rnd() < rate--) {
                double a = rnd() * Math.PI * 2, r = 70 + rnd() * 50;
                var m = add(Kind.MOTE_CYAN, focusX + Math.cos(a) * r, focusY + Math.sin(a) * r * .6, .42, (int) (rnd() * 4));
                m.vx = -Math.cos(a) * r / .42; m.vy = -Math.sin(a) * r * .6 / .42;
            }
        }
        if (state == Guardian.State.SLAM && g.attack() == Guardian.Attack.CHARGE) {
            ghostClock -= dt;
            if (ghostClock <= 0) {
                ghostClock = .05;
                var ghost = add(Kind.GHOST, g.x(), g.y(), .26, 0);
                ghost.ghostRow = g.animation().ordinal(); ghost.ghostFrame = g.animationFrame();
                if(g.actionFrame()>=0&&actionTints.containsKey(g.attack())) {
                    ghost.ghostAttack=g.attack();ghost.ghostRow=0;ghost.ghostFrame=g.actionFrame();
                    ghost.ghostFlip=g.directionX()<0;
                }
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
        if (state != Guardian.State.DORMANT && state != Guardian.State.DEAD) fought = true;
        if (state == Guardian.State.DEAD && last != Guardian.State.DEAD) {
            deathAge = 0; fallen = true; fallenX = g.x(); fallenY = g.y();
            if (performs()) {
                prepareDeath();
                if (!fought) deathAge = STATUE;             // back in a cleared arena: the statue is already there
            } else release(g.x(), g.y());
        }
        if (poses != null && deathAge >= 0) performDeath(dt);
        if (poses == null && deathAge >= 0 && deathAge < 2.6 && rnd() < 60 * dt) {   // the stolen Ichor rises free
            var m = add(Kind.MOTE_GOLD, fallenX + (rnd() - .5) * 120, fallenY + (rnd() - .5) * 40, 1.6, (int) (rnd() * 4));
            m.vx = (rnd() - .5) * 30; m.vy = -50 - rnd() * 60;
        }
        last = state; lastAttack = g.attack(); wasEnraged = g.enraged();
    }

    boolean fallen() { return fallen; }
    int particles() { return bits.size(); }

    /** The release of light and rubble when a boss without the performed death falls. */
    private void release(double x, double y) {
        burstRing(x, y - 50, 48, Kind.MOTE_CYAN, 520);
        for (int i = 0; i < 18; i++) {
            double a = rnd() * Math.PI * 2, speed = 120 + rnd() * 220;
            var d = add(Kind.DEBRIS, x, y + 10, 2 + rnd(), 2 + (int) (rnd() * 4));
            d.vx = Math.cos(a) * speed; d.vy = Math.sin(a) * speed * .6; d.vz = 260 + rnd() * 260;
        }
        for (int i = 0; i < 5; i++) add(Kind.BEAM, x + (rnd() - .5) * 110, y + 20 + (rnd() - .5) * 30, 1.5 + rnd() * .4, i % 4).age = -i * .07;
    }

    // ------------------------------------------------------------------ the Warden's death, performed
    // Owner 2026-10-29: "better death animation"; a shatter read as "fragmented", so it ends whole. Drawn from the
    // Warden's own atlas, no new art: it staggers with its visor guttering, light leaks through cracks in its plates,
    // it drops to one knee, shudders, slumps forward (head dips, fists to the ground), its core gives one flat pulse
    // and the visor dies, then the whole body cools from purple crystal to grey stone and stays as a kneeling statue.
    // RuinedOutpostGame.updateWardenVictory uses the same beats for its events and camera.
    static final double STAGGER = .3, SEAMS_FROM = .98, KNEEL = 1.2, KNELT = 1.55, SLUMP = 1.8, SETTLE = 2.2,
            PULSE = 2.25, COOL = 2.3, COOLED = 2.9, STATUE = 4;
    private static final int SEAMS = 7, HEAD_DIP = 6;
    // Weathered carved stone, lighter than the arena floor: three body tones and a moonlit top-left edge, horns in
    // dull bronze-stone, the dead visor a dark recessed band, one dark outline round it (owner 2026-10-29).
    private static final int[] STONE = {0xff4e5056, 0xff6c6c70, 0xff8e8c8a, 0xffb8b4aa}, BRONZE = {0xff4e3c2a, 0xff725a3a, 0xff98804f};
    private static final int OUTLINE = 0xff121216, RECESS = 0xff1a1a20;
    private static final Color HOT = new Color(150, 236, 236), WHITE_HOT = new Color(232, 255, 252),
            GLOW = new Color(70, 190, 205), HAIRLINE = new Color(26, 26, 32), PULSE_LIGHT = new Color(244, 255, 252);
    private BufferedImage[] poses, darkPoses, kneels, darkKneels, slumps, darkSlumps;  // poses 0-4; 6 steps each after
    private BufferedImage stone;
    private int[][] seams;                                            // art x,y pairs over pose 3's body
    private int[] visorPixels = new int[0];                           // x,y pairs of the dead visor, slumped
    private int coreX = 64, coreY = 48, waist, feet, left, right, high, headBottom, armWidth, dropLeft, dropRight;
    private boolean fought;

    private boolean performs() { return theme == Theme.WARDEN && atlas != null && atlas.getHeight() >= 7 * cell; }

    /** The Warden is drawn here from its death on, statue included: drawGuardian leaves it to this. */
    boolean performing() { return poses != null && deathAge >= 0; }

    private void prepareDeath() {
        int row = Guardian.Animation.DEATH.ordinal() * cell;
        poses = new BufferedImage[5]; darkPoses = new BufferedImage[5];
        for (int i = 0; i < 5; i++) { poses[i] = atlas.getSubimage(i * cell, row, cell, cell); darkPoses[i] = visorDead(poses[i]); }
        long sx = 0, sy = 0, n = 0;                                    // the visor's centre is the core
        for (int y = 0; y < cell; y++) for (int x = 0; x < cell; x++) if (visor(poses[0].getRGB(x, y))) { sx += x; sy += y; n++; }
        if (n > 0) { coreX = (int) (sx / n); coreY = (int) (sy / n); }
        int top = cell; feet = 0;
        for (int y = 0; y < cell; y++) for (int x = 0; x < cell; x++)
            if ((poses[3].getRGB(x, y) >>> 24) > 0) { top = Math.min(top, y); feet = Math.max(feet, y); }
        waist = top + (feet - top) * 2 / 3;                           // the legs are the bottom third
        kneels = new BufferedImage[6]; darkKneels = new BufferedImage[6];
        for (int k = 0; k < 6; k++) { kneels[k] = kneel(poses[3], k / 5.0); darkKneels[k] = kneel(darkPoses[3], k / 5.0); }
        var knelt = kneels[5];                                        // the knelt body's arms, head and how far the fists hang
        left = cell; right = -1; high = cell;
        for (int y = 0; y < cell; y++) for (int x = 0; x < cell; x++)
            if ((knelt.getRGB(x, y) >>> 24) > 0) { left = Math.min(left, x); right = Math.max(right, x); high = Math.min(high, y); }
        armWidth = Math.max(1, (int) Math.round((right - left + 1) * .26));
        headBottom = high + (int) Math.round((waist + kneelShift(1) - high) * .38);   // the head and crown
        int lowLeft = 0, lowRight = 0;
        for (int y = 0; y < cell; y++) for (int x = 0; x < cell; x++) if ((knelt.getRGB(x, y) >>> 24) > 0) {
            if (arm(x) < 0) lowLeft = Math.max(lowLeft, y);
            if (arm(x) > 0) lowRight = Math.max(lowRight, y);
        }
        dropLeft = Math.max(0, Math.min(10, feet - lowLeft)); dropRight = Math.max(0, Math.min(10, feet - lowRight));
        slumps = new BufferedImage[6]; darkSlumps = new BufferedImage[6];
        for (int k = 0; k < 6; k++) { slumps[k] = slump(knelt, k / 5.0); darkSlumps[k] = slump(darkKneels[5], k / 5.0); }
        stone = stoneOf(darkSlumps[5]);
        var visorAt = new ArrayList<Integer>();
        for (int y = 0; y < cell; y++) for (int x = 0; x < cell; x++) {
            int p = darkSlumps[5].getRGB(x, y);
            if (p == 0xff2c2742 || p == 0xff1b1828) { visorAt.add(x); visorAt.add(y); }
        }
        visorPixels = visorAt.stream().mapToInt(Integer::intValue).toArray();
        seams = new int[SEAMS][];
        for (int i = 0; i < SEAMS; i++) seams[i] = seam(poses[3], i);
    }

    private static boolean visor(int argb) {
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        return (argb >>> 24) > 0 && g > r + 50 && b > r + 50;
    }

    /** The same pose with its visor dead: cyan becomes the dark of the plates, two tones by brightness. */
    private static BufferedImage visorDead(BufferedImage pose) {
        var out = new BufferedImage(pose.getWidth(), pose.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pose.getHeight(); y++) for (int x = 0; x < pose.getWidth(); x++) {
            int p = pose.getRGB(x, y);
            out.setRGB(x, y, !visor(p) ? p : (p & 255) > 200 ? 0xff2c2742 : 0xff1b1828);
        }
        return out;
    }

    private static boolean solid(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight() && (image.getRGB(x, y) >>> 24) > 0;
    }

    /** Dormant carved stone in flat tones: brightness picks a body tone, the top-left edge catches the moon, the far
     *  edge falls into shade; gold horns become bronze-stone, the dead visor a recess; a dark outline closes it. */
    private static BufferedImage stoneOf(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = source.getRGB(x, y);
            if ((p >>> 24) == 0) continue;
            int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
            double l = (.299 * r + .587 * g + .114 * b) / 255;
            boolean lit = !solid(source, x - 1, y) || !solid(source, x, y - 1), shade = !solid(source, x + 1, y) || !solid(source, x, y + 1);
            int tone;
            if (p == 0xff2c2742 || p == 0xff1b1828) tone = RECESS;
            else if (r > 140 && g > 95 && r > b + 60 && g > b + 25) tone = BRONZE[lit ? 2 : Math.min(2, (int) (l * 3))];
            else {
                int base = l < .12 ? 0 : l < .22 ? 1 : 2;
                tone = STONE[lit ? Math.min(3, base + 1) : shade ? Math.max(0, base - 1) : base];
            }
            out.setRGB(x, y, tone);
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            if (!solid(source, x, y) && (solid(source, x - 1, y) || solid(source, x + 1, y) || solid(source, x, y - 1) || solid(source, x, y + 1)))
                out.setRGB(x, y, OUTLINE);
        return out;
    }

    private int kneelShift(double k) {
        int legs = feet - waist + 1;
        return legs - Math.max(1, (int) Math.round(legs * (1 - .32 * k)));
    }

    /** Pose 4 sinking onto one knee: leg rows drop out evenly, the body above sits lower by as much, the feet stay put. */
    private BufferedImage kneel(BufferedImage pose, double k) {
        int legs = feet - waist + 1, shift = kneelShift(k), kept = legs - shift;
        var out = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < waist; y++) if (y + shift < cell) for (int x = 0; x < cell; x++) out.setRGB(x, y + shift, pose.getRGB(x, y));
        for (int i = 0; i < kept; i++) {
            int source = waist + (int) ((long) i * legs / kept);
            for (int x = 0; x < cell; x++) { int p = pose.getRGB(x, source); if ((p >>> 24) > 0) out.setRGB(x, waist + shift + i, p); }
        }
        return out;
    }

    /** -1 left arm, 1 right arm, 0 the body between, in the knelt pose's columns. */
    private int arm(int x) { return x < left + armWidth ? -1 : x > right - armWidth ? 1 : 0; }
    private boolean head(int x, int y) { return arm(x) == 0 && y <= headBottom; }

    /** How far a knelt-pose pixel moves as it slumps: the arms hang until the fists rest on the ground, the head bows.
     *  The torso keeps its height, so the kneel stays readable. Whole pixels only: the art keeps its one pixel size. */
    private int slumped(int x, int y, double s) {
        int side = arm(x);
        return (int) Math.round((side < 0 ? dropLeft : side > 0 ? dropRight : head(x, y) ? HEAD_DIP : 0) * s);
    }

    /** The knelt body slumping forward, moved by whole pixels: the torso stays, the arms drop, the bowed head over it. */
    private BufferedImage slump(BufferedImage knelt, double s) {
        var out = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
        for (int pass = 0; pass < 3; pass++)                          // torso, then arms, then the head in front
            for (int y = 0; y < cell; y++) for (int x = 0; x < cell; x++) {
                int p = knelt.getRGB(x, y);
                if ((p >>> 24) == 0 || (pass == 0 ? arm(x) != 0 || head(x, y) : pass == 1 ? arm(x) == 0 : !head(x, y))) continue;
                int ty = y + slumped(x, y, s);
                if (ty < cell) out.setRGB(x, ty, p);
            }
        return out;
    }

    /** One crack in pose 4: a wandering line from the core outward, over the body above the legs only. */
    private int[] seam(BufferedImage pose, int index) {
        double angle = index * Math.PI * 2 / SEAMS + rnd() * .6, x = coreX + Math.cos(angle) * 3, y = coreY + Math.sin(angle) * 2;
        int length = 10 + (int) (rnd() * 17), n = 0;
        var points = new int[length * 2];
        for (int i = 0; i < length; i++) {
            angle += (rnd() - .5) * .9; x += Math.cos(angle); y += Math.sin(angle);
            int px = (int) Math.round(x), py = (int) Math.round(y);
            if (px < 0 || py < 0 || px >= cell || py >= waist || (pose.getRGB(px, py) >>> 24) == 0) break;
            if (n == 0 || points[n - 2] != px || points[n - 1] != py) { points[n++] = px; points[n++] = py; }
        }
        return java.util.Arrays.copyOf(points, n);
    }

    private double floorY() { return fallenY + Guardian.GROUND_Y_OFFSET + (cell - foot) * renderSize / cell; }

    private void performDeath(double dt) {
        double before = deathAge - dt, scale = renderSize / (double) cell, x0 = fallenX - renderSize / 2.0;
        if (before < KNELT && deathAge >= KNELT)                      // the knee meets the stone
            for (int i = 0; i < 10; i++) dust(fallenX + (rnd() - .5) * 120, fallenY + 30 + (rnd() - .5) * 24, (rnd() - .5) * 140, (rnd() - .5) * 40);
        if (before < SETTLE && deathAge >= SETTLE)                    // its fists come to rest on the ground
            for (int side = -1; side <= 1; side += 2) {
                double fist = x0 + (side < 0 ? left + armWidth / 2.0 : right - armWidth / 2.0) * scale;
                for (int i = 0; i < 4; i++) dust(fist + (rnd() - .5) * 30, fallenY + 34, side * (40 + rnd() * 60), (rnd() - .5) * 30);
            }
        if (before < COOLED && deathAge >= COOLED)                    // one last breath of dust
            for (int i = 0; i < 3; i++) dust(fallenX + (rnd() - .5) * 60, fallenY + 34, (rnd() - .5) * 50, -10 - rnd() * 20);
    }

    private static boolean visorLit(double t) {
        double[] dark = {.42, .50, .66, .72, .83, .95, 1.05, 1.12, 1.70, 1.78};   // from, to: the visor gutters, then dies
        if (t >= 1.86) return false;
        for (int i = 0; i < dark.length; i += 2) if (t >= dark[i] && t < dark[i + 1]) return false;
        return true;
    }

    /** Screen pixels of sideways shake, always an even number so the art stays on its 2x grid; one shudder after the kneel. */
    private static int tremble(double t) {
        double amp = t < STAGGER ? 1 : t < KNEEL ? 2.5 - (t - STAGGER) * 1.4 : t > 1.62 && t < 1.74 ? 2 : 0;
        return amp <= 0 ? 0 : ((int) (t * 30) % 2 == 0 ? 2 : -2) * (int) Math.round(amp);
    }

    private static Color mix(Color a, Color b, double f) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f), (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    private void performBody(Graphics2D g, int cx, int cy, boolean reduced) {
        double t = deathAge;
        int size = renderSize, top = (int) Math.round(floorY()) - cy - size;
        int x = (int) Math.round(fallenX) - cx - size / 2 + (reduced ? 0 : tremble(t));
        boolean lit = reduced ? t < 1.86 : visorLit(t);
        BufferedImage pose; int shift = 0; double slump = -1;
        if (t < KNEEL) {
            int f = t < STAGGER ? 0 : Math.min(3, 1 + (int) ((t - STAGGER) / .3));
            pose = lit ? poses[f] : darkPoses[f];
        } else if (t < SLUMP) {
            int step = (int) Math.round(eased((t - KNEEL) / (KNELT - KNEEL)) * 5);
            pose = lit ? kneels[step] : darkKneels[step]; shift = kneelShift(step / 5.0);
        } else {
            int step = (int) Math.round(eased((t - SLUMP) / (SETTLE - SLUMP)) * 5);
            pose = lit ? slumps[step] : darkSlumps[step]; shift = kneelShift(1); slump = step / 5.0;
        }
        double cool = eased((t - COOL) / (COOLED - COOL));
        if (cool > 0) g.drawImage(stone, x, top, x + size, top + size, 0, 0, cell, cell, null);   // cools to dormant stone
        if (cool < 1) {
            var warm = (Graphics2D) g.create();
            if (cool > 0) warm.setComposite(AlphaComposite.SrcOver.derive((float) (1 - cool)));
            warm.drawImage(pose, x, top, x + size, top + size, 0, 0, cell, cell, null);
            warm.dispose();
        }
        if (t < .06 && !reduced && white != null) {                    // the killing blow lands
            int row = Guardian.Animation.DEATH.ordinal() * cell;
            g.drawImage(white, x, top, x + size, top + size, 0, row, cell, row + cell, null);
        }
        if (t >= PULSE && t < PULSE + .2 && !reduced) {                // one flat pulse from the core, then dark for good
            var pulse = (Graphics2D) g.create();
            pulse.setComposite(AlphaComposite.SrcOver.derive((float) (1 - (t - PULSE) / .2)));
            pulse.setColor(PULSE_LIGHT);
            for (int i = 0; i < visorPixels.length; i += 2) pulse.fillRect(x + visorPixels[i] * 2, top + visorPixels[i + 1] * 2, 2, 2);
            pulse.dispose();
        }
        if (t < SEAMS_FROM) return;
        double heat = t < KNELT ? .55 : Math.min(1, .55 + (t - KNELT) / (SETTLE - KNELT) * .45);
        var crack = mix(heat > .8 ? WHITE_HOT : HOT, HAIRLINE, cool);      // light in the cracks, then dark hairlines
        boolean glowing = heat > .7 && cool < .35;
        for (int i = 0; i < SEAMS; i++) {
            double appear = SEAMS_FROM + i * .12;
            if (t < appear) continue;
            int[] seam = seams[i];
            int shown = (int) (seam.length / 2 * Math.min(1, (t - appear) / .15));
            for (int p = 0; p < shown; p++) {
                int ax = seam[p * 2], ay = seam[p * 2 + 1] + shift;
                if (slump >= 0) ay += slumped(ax, ay, slump);
                if (ay >= cell || (pose.getRGB(ax, ay) >>> 24) == 0) continue;
                if (glowing) for (int side = -1; side <= 1; side += 2)
                    if (ax + side >= 0 && ax + side < cell && (pose.getRGB(ax + side, ay) >>> 24) > 0) {
                        g.setColor(GLOW); g.fillRect(x + (ax + side) * 2, top + ay * 2, 2, 2);
                    }
                g.setColor(crack); g.fillRect(x + ax * 2, top + ay * 2, 2, 2);
            }
        }
    }

    private static double eased(double t) { double p = Math.max(0, Math.min(1, t)); return p * p * (3 - 2 * p); }

    // ------------------------------------------------------------------ drawing
    /** Below the actors: cracks, dust, the fallen Warden's remains. */
    void drawGround(Graphics2D g, Guardian guardian, int cx, int cy) {
        if(guardian.state()==Guardian.State.ENRAGE&&DUST!=null) {
            int f=(int)(guardian.stateSeconds()*8)%3;
            for(int i=0;i<6;i++) {
                double angle=i*Math.PI/3;
                cell(g,DUST,28,f,guardian.x()+Math.cos(angle)*88,guardian.y()+30+Math.sin(angle)*46,cx,cy,14,18);
            }
        }
        if (guardian.profile()!=Guardian.Profile.BRIARHEART&&guardian.state() == Guardian.State.TELEGRAPH && DUST != null) {
            int f = (int) (guardian.stateSeconds() * 10) % 3;
            if (guardian.attack() == Guardian.Attack.TARGET)
                for (int side = -1; side <= 1; side++)
                    cell(g, DUST, 28, f, guardian.targetX() + side * 40, guardian.targetY() + (side == 0 ? 40 : 10), cx, cy, 14, 18);
            if(guardian.attack()==Guardian.Attack.CHARGE)
                for(int step=1;step<=3;step++)cell(g,DUST,28,f,
                        guardian.x()-guardian.directionX()*step*30,guardian.y()+36-guardian.directionY()*step*30,cx,cy,14,18);
            if(guardian.attack()==Guardian.Attack.SHOCKWAVE&&SPIKE!=null)
                for(int i=0;i<8;i++) {
                    double a=i*Math.PI/4;
                    cell(g,SPIKE,20,0,guardian.x()+Math.cos(a)*104,guardian.y()+Math.sin(a)*72,cx,cy,10,28);
                }
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
        if (fallen && !guardian.visible() && atlas != null && poses == null) {   // the performed death leaves its statue instead
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
        if (performing()) sink.accept(fallenY + Guardian.GROUND_Y_OFFSET + 1e-3, () -> performBody(g, cx, cy, reduced));
        else if (guardian.visible()) sink.accept(guardian.y() + Guardian.GROUND_Y_OFFSET + 1e-3, () -> body(g, guardian, cx, cy, frame, reduced));
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
        if(guardian.state()==Guardian.State.ENRAGE&&cyan!=null) {
            float charge=(float)Math.min(1,guardian.stateSeconds()/Guardian.ENRAGE_DURATION);
            frameOf(g,cyan,guardian,cx,cy,reduced?.16f:.12f+charge*.36f);
        }
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
        if (enrageAge >= 0 && enrageAge < .3) {
            if(theme==Theme.WARDEN)frameOf(g,enrageAge<.06?white:cyan,guardian,cx,cy,(float)(.85*(1-enrageAge/.3)));
            else frameOf(g,((int)(enrageAge*30))%2==0?white:cyan,guardian,cx,cy,.85f);
        }
    }

    /** Above the actors: one complete cleave, motes and the death beams. */
    void drawAbove(Graphics2D g, Guardian guardian, int cx, int cy, long frame) {
    }

    private void ghost(Graphics2D g, Bit b, int cx, int cy) {
        if (cyan == null) return;
        var faded = (Graphics2D) g.create();
        faded.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, .42 * (1 - b.age / b.life))));
        int size = renderSize, x = (int) Math.round(b.x) - cx - size / 2;
        int floorY = (int) Math.round(b.y) - cy + Guardian.GROUND_Y_OFFSET
                + (cell - foot) * renderSize / cell;
        int sx = b.ghostFrame * cell, sy = b.ghostRow * cell;
        var sheet=b.ghostAttack==null?cyan:actionTints.get(b.ghostAttack)[1];
        faded.drawImage(sheet,b.ghostFlip?x+size:x,floorY-size,b.ghostFlip?x:x+size,floorY,sx,sy,sx+cell,sy+cell,null);
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
        boolean action=guardian.actionFrame()>=0&&actionTints.containsKey(guardian.attack());
        if(action) {sheet=actionTints.get(guardian.attack())[sheet==white?0:1];sx=guardian.actionFrame()*cell;sy=0;}
        boolean flip=action&&guardian.attack()!=Guardian.Attack.SHOCKWAVE&&guardian.directionX()<0;
        layer.drawImage(sheet,flip?x+size:x,y,flip?x:x+size,y+size,sx,sy,sx+cell,sy+cell,null);
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
        int cy=height-(int)Math.ceil(204*JourneyHud.scale(width,height))-140;
        for (int[] band : new int[][]{{cy - 30, 12, 80}, {cy - 18, 136, 200}, {cy + 118, 12, 80}}) {   // soft top and bottom
            Ui.fade(card, 0, band[0], width / 2, band[1], 0, band[2]);
            Ui.fade(card, width / 2, band[0], width / 2, band[1], band[2], 0);
        }
        String name = theme.name().toUpperCase(java.util.Locale.ROOT);
        JourneyHud.center(card,name,width/2,cy+34,Ui.menuFont(42),MenuTheme.IVORY);
        int open=(int)Math.min(140,20+t*150);
        card.setColor(JourneyHud.GOLD);card.drawLine(width/2-open,cy+60,width/2-12,cy+60);
        card.drawLine(width/2+12,cy+60,width/2+open,cy+60);MenuTheme.star(card,width/2,cy+60,5);
        JourneyHud.center(card,theme.title(),width/2,cy+94,Ui.heading(18),new Color(theme.glow()));
        card.dispose();
    }
}
