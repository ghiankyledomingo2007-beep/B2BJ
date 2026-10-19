/**
 * Effect sheets for the Outpost Warden fight, in the Warden's own colours: cyan visor-fire,
 * gold from its spikes, violet-black armour and grey dust. All frames run left to right.
 */
final class WardenArt {
    private WardenArt() { }

    static void build() {
        debris().save("assets/effects/warden_debris.png");
        Canvas[] dust = new Canvas[6], crack = new Canvas[4], sweep = new Canvas[4], flame = new Canvas[8],
                spike = new Canvas[6], beam = new Canvas[4], mote = new Canvas[8];
        for (int f = 0; f < 6; f++) dust[f] = dust(f);
        for (int f = 0; f < 4; f++) crack[f] = crack(f);
        for (int f = 0; f < 4; f++) sweep[f] = sweep(f);
        for (int f = 0; f < 8; f++) flame[f] = flame(f, 8);
        for (int f = 0; f < 6; f++) spike[f] = spike(f);
        for (int f = 0; f < 4; f++) beam[f] = beam(f);
        for (int f = 0; f < 8; f++) mote[f] = mote(f % 4, f < 4 ? Pal.CYAN : Pal.GOLD);
        Canvas.strip(dust).save("assets/effects/warden_dust.png");
        Canvas.strip(crack).save("assets/effects/warden_crack.png");
        Canvas.strip(sweep).save("assets/effects/warden_sweep.png");
        Canvas.strip(flame).save("assets/effects/warden_flame.png");
        Canvas.strip(spike).save("assets/effects/warden_spike.png");
        Canvas.strip(beam).save("assets/effects/warden_beam.png");
        Canvas.strip(mote).save("assets/effects/warden_mote.png");
        // Briarheart's copies: the Warden's cyan fire becomes gold light through violet rot
        for (String n : new String[]{"debris", "dust", "crack", "sweep", "flame", "spike", "beam", "mote"})
            briar(Canvas.load("assets/effects/warden_" + n + ".png")).save("assets/effects/briar_" + n + ".png");
        briar(Canvas.load("assets/effects/warden_impact.png")).save("assets/effects/briar_impact.png");
    }

    /** Maps the cyan ramp onto Briarheart's colours: its dark steps go violet, its bright steps gold. */
    static Canvas briar(Canvas c) {
        int[] to = {Pal.VIOLET[1], Pal.VIOLET[3], Pal.GOLD[2], Pal.GOLD[3], Pal.GOLD[4], Pal.GOLD[5]};
        Canvas out = c.copy();
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            int p = c.get(x, y);
            for (int i = 0; i < Pal.CYAN.length; i++) if ((p | 0xff000000) == Pal.CYAN[i] && (p >>> 24) != 0) out.set(x, y, (p & 0xff000000) | (to[i] & 0xffffff));
        }
        return out;
    }

    /** Six 10x10 chunks: two stones, two glowing crystal shards, two violet armour plates. */
    static Canvas debris() {
        Canvas sheet = new Canvas(60, 10);
        sheet.blit(Decals.stone(7, 6, false, 901), 1, 2);
        sheet.blit(Decals.stone(9, 7, false, 902), 10, 2);
        sheet.blit(Decals.ascii("..ZX.", ".ZXB.", "XXBV.", ".BVC.", "..C.."), 21, 3);
        sheet.blit(Decals.ascii(".XB..", "ZXBB.", "XBBVC", ".VVC."), 31, 4);
        sheet.blit(Decals.ascii("..SJ...", ".VVVJ..", "VVVVVC.", ".CVVVC.", "..CCC.."), 41, 3);
        sheet.blit(Decals.ascii(".J...", "VVJ..", "VVVVC", ".CCC."), 53, 4);
        return sheet;
    }

    /** A dust puff that swells and thins out to nothing over six frames. */
    static Canvas dust(int f) {
        Canvas c = new Canvas(28, 22);
        double grow = 5 + f * 1.6, thin = f / 6.5;
        double[][] lobes = {{14, 13, 1}, {9, 14, .8}, {19, 14, .85}, {12, 10, .7}, {17, 10, .65}};
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            double best = 9;
            double lx = 0, ly = 0;
            for (double[] l : lobes) {
                double cy = l[1] - f * .8;
                double d = Math.hypot(x + .5 - l[0], (y + .5 - cy) * 1.2) / (grow * l[2]);
                if (d < best) { best = d; lx = (x + .5 - l[0]); ly = (y + .5 - cy); }
            }
            if (best > 1) continue;
            if (Noise.bayer(x, y) < thin + (best > .7 ? .25 : 0)) continue;
            double lit = (-lx - ly) / grow;
            c.set(x, y, lit > .4 ? Pal.STONE[5] : lit > -.2 ? Pal.STONE[4] : lit > -.6 ? Pal.STONE[3] : Pal.STONE[2]);
        }
        return c;
    }

    /** Radial cracks in the paving; the cyan glow in them cools over four frames. */
    static Canvas crack(int f) {
        Canvas c = new Canvas(80, 40);
        int cx = 40, cy = 20;
        int[] glow = {f == 0 ? Pal.CYAN[5] : f == 1 ? Pal.CYAN[4] : f == 2 ? Pal.CYAN[2] : Pal.STONE[0],
                f == 0 ? Pal.CYAN[3] : f == 1 ? Pal.CYAN[2] : f == 2 ? Pal.CYAN[1] : Pal.INK};
        for (int arm = 0; arm < 7; arm++) {
            double a = arm * Math.PI * 2 / 7 + Noise.rnd(arm, 1, 911) * .5;
            double len = 18 + Noise.rnd(arm, 2, 911) * 20;
            double x = cx, y = cy;
            for (int s = 0; s < len; s++) {
                a += (Noise.rnd(arm, s, 912) - .5) * .5;
                x += Math.cos(a); y += Math.sin(a) * .55;
                int px = (int) Math.round(x), py = (int) Math.round(y);
                c.set(px, py, s < len * .7 ? glow[0] : glow[1]);
                c.set(px, py + 1, Pal.STONE[0]);
                if (s == (int) (len * .5) && Noise.rnd(arm, 3, 913) < .7) {         // a branch
                    double b = a + (Noise.rnd(arm, 4, 913) < .5 ? .7 : -.7), bx = x, by = y;
                    for (int t = 0; t < len * .4; t++) { bx += Math.cos(b); by += Math.sin(b) * .55; c.set((int) Math.round(bx), (int) Math.round(by), glow[1]); }
                }
            }
        }
        c.ellipse(cx, cy, 6, 3.5, Pal.STONE[0]);
        c.ellipse(cx, cy, 4, 2.2, glow[0]);
        if (f < 2) c.ellipse(cx, cy, 2, 1.2, Pal.CYAN[5]);
        for (int k = 0; k < 6; k++) {                                                  // kicked-up chips
            int px = cx - 26 + (int) (Noise.rnd(k, 5, 914) * 52), py = cy - 10 + (int) (Noise.rnd(k, 6, 914) * 20);
            c.set(px, py, Pal.STONE[5]); c.set(px + 1, py, Pal.STONE[4]); c.set(px, py + 1, Pal.STONE[2]);
        }
        return c;
    }

    /** The spin cleave: a 240-degree smear of cyan light whirling round the Warden. */
    static Canvas sweep(int f) {
        Canvas c = new Canvas(184, 112);
        double cx = 92, cy = 58, rx = 84, ry = 50;
        double head = f * Math.PI / 2;
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            double dx = (x + .5 - cx) / rx, dy = (y + .5 - cy) / ry, r = Math.hypot(dx, dy);
            if (r < .78 || r > 1.04) continue;
            double a = Math.atan2(dy, dx);
            double behind = Math.floorMod((long) Math.round((head - a) * 1000), (long) Math.round(Math.PI * 2000)) / 1000.0;
            if (behind > Math.PI * 4 / 3) continue;
            double fade = 1 - behind / (Math.PI * 4 / 3);
            double band = 1 - Math.abs(r - .92) / .13;
            double v = fade * band + (Noise.bayer(x, y) - .5) * .3;
            if (v < .12) continue;
            c.set(x, y, v > .78 ? Pal.CYAN[5] : v > .55 ? Pal.CYAN[4] : v > .32 ? Pal.CYAN[3] : Pal.CYAN[2]);
        }
        return c;
    }

    /** A looping cyan flame wisp. */
    static Canvas flame(int f, int frames) {
        Canvas fire = Kit.fire(14, 24, f, frames, 2, 931);
        Canvas c = new Canvas(14, 24);
        int[] map = {Pal.CYAN[1], Pal.CYAN[2], Pal.CYAN[3], Pal.CYAN[4], Pal.CYAN[5], Pal.CYAN[5]};
        for (int y = 0; y < 24; y++) for (int x = 0; x < 14; x++) {
            int v = fire.get(x, y);
            if ((v >>> 24) == 0) continue;
            int idx = Pal.index(v);
            c.set(x, y, map[Math.max(0, Math.min(5, idx))]);
        }
        return c;
    }

    /** A rock spike bursting from the floor on a cyan crack, then crumbling. */
    static Canvas spike(int f) {
        Canvas c = new Canvas(20, 30);
        int base = 27, cx = 10;
        int[] heights = {0, 8, 19, 20, 12, 0};
        int h = heights[f];
        if (f <= 4) for (int x = cx - 7; x <= cx + 7; x++) c.set(x, base, f == 0 ? Pal.CYAN[4] : Pal.CYAN[2]);
        if (f == 0) { c.set(cx, base, Pal.CYAN[5]); c.set(cx - 1, base, Pal.CYAN[5]); }
        if (h > 0) for (int y = 0; y < h; y++) {
            // a split, leaning shard: two facets, a lit left face and a dark right face, jagged edges
            double t = y / (double) h;
            int lean = (int) Math.round(t * t * 3);
            int left = (int) Math.round(-(1 - t) * 8.5 - (Noise.rnd(y / 3, 1, 947) - .5) * 2) + lean;
            int right = (int) Math.round((1 - t) * 7 + (Noise.rnd(y / 3, 2, 947) - .5) * 2) + lean;
            int ridge = lean + (int) Math.round((1 - t) * -.5);
            for (int x = left; x <= Math.max(left, right); x++) {
                int colour = x < ridge - 1 ? Pal.STONE[5] : x <= ridge ? Pal.STONE[6] : x < right ? Pal.STONE[3] : Pal.STONE[2];
                if (x == left) colour = Pal.STONE[4];
                if (y >= h - 2) colour = x <= ridge ? Pal.STONE[7] : Pal.STONE[4];
                if (y < 3) colour = Pal.CYAN[y == 0 ? 3 : 2];                         // glow where it tore out
                if (y >= 3 && y < 6 && x == ridge) colour = Pal.CYAN[1];               // light bleeding up the crack
                if (f == 4 && Noise.rnd(x, y, 941) < .3) continue;
                c.set(cx + x, base - 1 - y, colour);
            }
        }
        if (f >= 3) {                                                                   // shed rubble
            c.blit(Decals.stone(5, 4, false, 942 + f), cx - 9, base - 3);
            c.blit(Decals.stone(4, 3, false, 944 + f), cx + 5, base - 2);
        }
        if (f == 5) c.blit(Decals.stone(7, 5, false, 946), cx - 3, base - 4);
        return c;
    }

    /** A column of cyan light for the Warden's death. */
    static Canvas beam(int f) {
        Canvas c = new Canvas(28, 140);
        double width = 6 + (f % 2 == 0 ? 2 : 0) + (f == 3 ? -3 : 0);
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            double d = Math.abs(x + .5 - 14) / width;
            double top = y / 140.0;
            double v = (1 - d) * (.35 + .65 * top) + (Noise.bayer(x, y) - .5) * .35;
            if (v < .1 || d > 1.6) continue;
            c.set(x, y, v > .72 ? Pal.CYAN[5] : v > .5 ? Pal.CYAN[4] : v > .3 ? Pal.CYAN[3] : Pal.CYAN[2]);
        }
        return c;
    }

    static Canvas mote(int f, int[] ramp) {
        Canvas c = new Canvas(6, 6);
        int core = f == 1 ? ramp[5] : ramp[4];
        c.set(2, 2, core); c.set(3, 2, f == 2 ? ramp[3] : core); c.set(2, 3, ramp[3]); c.set(3, 3, ramp[3]);
        if (f == 1) { c.set(2, 0, ramp[3]); c.set(2, 5, ramp[2]); c.set(0, 2, ramp[3]); c.set(5, 3, ramp[2]); }
        if (f == 0 || f == 2) { c.set(2, 1, ramp[2]); c.set(1, 2, ramp[2]); c.set(4, 3, ramp[2]); c.set(3, 4, ramp[2]); }
        return c;
    }
}
