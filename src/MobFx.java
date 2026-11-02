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
    enum Kind { ALERT, GLINT, DUST, STREAK, ARC, CLAW, BITE, CAST, MOTE, FLASH, SPARK, SHARD }

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
    private final Map<Wisp, Boolean> swiping = new WeakHashMap<>();
    private List<Wisp> enemies=List.of();
    private List<ForestHazard> hazards=List.of();
    private long seed = 0x6d6f62L;

    private double rnd() { seed = seed * 6364136223846793005L + 1442695040888963407L; return (seed >>> 11) / (double) (1L << 53); }
    private Bit add(Kind kind, double x, double y, double life, int colour) {
        if (bits.size() >= LIMIT) bits.remove(0);
        var b = new Bit(kind, x, y, life, colour);
        bits.add(b);
        return b;
    }

    void clear() { bits.clear(); last.clear(); noticed.clear(); swiping.clear(); enemies=List.of();hazards=List.of(); }

    // ------------------------------------------------------------------ simulation
    void update(double seconds, RuinedOutpostGame game) {
        enemies=game.scouts();hazards=game.forestHazards();
        for (Wisp w : game.scouts()) {
            var before = last.put(w, w.state());
            boolean aggro = w.alive() && w.aggro();
            Boolean was = noticed.put(w, aggro);
            if (aggro && Boolean.FALSE.equals(was)) alert(w);
            if (before != null && before != w.state()) transition(w, before);
            boolean claw=w.impSwipeActive();
            if(claw&&!Boolean.TRUE.equals(swiping.put(w,claw))) {
                double angle=Math.atan2(w.intentY(),w.intentX())+(w.stateSeconds()<.3?-.25:.25);
                var slash=add(Kind.CLAW,w.x(),w.y()+Player.COLLISION_Y_OFFSET,.20,0xe1c8ef);
                slash.ax=Math.cos(angle);slash.ay=Math.sin(angle);
                for(int i=0;i<5;i++) {
                    var chip=add(Kind.SPARK,w.x()+slash.ax*100,w.y()+24+slash.ay*100,.12+rnd()*.12,i%2==0?0xe1c8ef:0x9165b5);
                    chip.vx=slash.ax*90+(rnd()-.5)*140;chip.vy=slash.ay*90+(rnd()-.5)*120;
                }
            } else if(!claw)swiping.put(w,false);
            if (w.state() == Wisp.State.TELEGRAPH && w.ranged() && rnd() < .5) chargeMote(w);
            if (w.state() == Wisp.State.TELEGRAPH && w.remnantRush() && rnd() < .3) chargeMote(w);
            if (w.state() == Wisp.State.LUNGE && lunger(w)) streak(w);
        }
        for (int i = bits.size() - 1; i >= 0; i--) {
            var b = bits.get(i);
            b.age += seconds;
            if (b.age >= b.life) { bits.remove(i); continue; }
            b.x += b.vx * seconds; b.y += b.vy * seconds;
            double drag = b.kind == Kind.DUST ? 5 : b.kind == Kind.SPARK ? 3 : b.kind == Kind.SHARD ? 7 : 0;   // fast start, exponential slow
            b.vx *= Math.exp(-drag * seconds); b.vy *= Math.exp(-drag * seconds);
            if (b.kind == Kind.MOTE) {                                             // motes are drawn into the mouth
                b.vx += (b.ax - b.x) * 40 * seconds; b.vy += (b.ay - b.y) * 40 * seconds;
            }
        }
    }

    private static boolean lunger(Wisp w) { return !w.ranged() && !w.stationaryMelee(); }
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
                if(w.role()==Wisp.Role.RIFT_IMP)return;
                if(w.role()==Wisp.Role.MIRE_SHAMAN) {
                    var cast=add(Kind.CAST,w.x(),w.y()-38,.24,0xb7c77b);
                    add(Kind.FLASH,w.x(),w.y()-48,.08,0xd5dfa3);add(Kind.FLASH,w.x(),w.y()-48,.05,0xfffffe);   // the staff flares
                    cast.ax=w.markedX();cast.ay=w.markedY()+Player.COLLISION_Y_OFFSET;
                    return;
                }
                if(w.role()==Wisp.Role.THORN_WOLF&&!w.wolfLeap()) {
                    var bite=add(Kind.BITE,w.x()+ix*75,w.y()+Player.COLLISION_Y_OFFSET+iy*75,.26,0xd7d4ae);
                    bite.ax=ix;bite.ay=iy;
                    for(int i=0;i<4;i++)dust(w.x()+ix*70,w.y()+24+iy*70,ix*60+(rnd()-.5)*80,iy*60+(rnd()-.5)*80);
                    return;
                }
                if (lunger(w)) for (int i = 0; i < 6; i++)                          // push-off dust behind the rush
                    dust(w.x() - ix * 18 + (rnd() - .5) * 20, w.y() + 22, -ix * (90 + rnd() * 80), -iy * 40 + (rnd() - .5) * 30);
                else if (w.stationaryMelee()) {                                    // the blow's arc where it lands
                    int colour=w.role()==Wisp.Role.REMNANT?0xbf87c9:0x8baeb5;
                    var arc = add(Kind.ARC, w.x() + ix * 26, w.y() + 10 + iy * 26, .2, colour);
                    arc.ax = ix; arc.ay = iy;
                    for(int i=0;i<5;i++) {
                        var chip=add(Kind.SPARK,w.x()+ix*65,w.y()+24+iy*65,.18+rnd()*.12,colour);
                        chip.vx=ix*60+(rnd()-.5)*140;chip.vy=iy*40-30+(rnd()-.5)*90;
                    }
                    for (int i = 0; i < 4; i++) dust(w.x() + ix * 44 + (rnd() - .5) * 30, w.y() + 22 + iy * 30, ix * 60 + (rnd() - .5) * 60, iy * 30);
                } else {                                                           // a spitter's muzzle flash and recoil puff
                    var flash = add(Kind.FLASH, w.x() + ix * 20, w.y() - 24 + iy * 10, .1, 0xfff0bd);
                    flash.ax = ix; flash.ay = iy;
                    for(int i=0;i<6;i++) {
                        var drop=add(Kind.SPARK,w.x()+ix*20,w.y()-24+iy*10,.2+rnd()*.12,i%2==0?0xab68cf:0xe2b6f5);
                        drop.vx=ix*(80+rnd()*100)+(rnd()-.5)*50;drop.vy=iy*(80+rnd()*100)+(rnd()-.5)*50;
                    }
                    add(Kind.FLASH, w.x() + ix * 24, w.y() - 24 + iy * 10, .06, 0xfffffe).ax = ix;   // a white star, not a disc
                    for (int i = 0; i < 3; i++) {                                  // and a small smoke puff off the barrel
                        var puff = add(Kind.DUST, w.x() + ix * (26 + i * 6), w.y() - 26 + iy * 10, .2 + rnd() * .08, i % 2 == 0 ? 0x8a8f99 : 0x6e737d);
                        puff.vx = ix * (40 + rnd() * 30); puff.vy = -20 - rnd() * 20;
                    }
                    dust(w.x() - ix * 12, w.y() + 22, -ix * 60, -iy * 30);
                }
            }
            case RECOVER -> {
                if(from==Wisp.State.LUNGE&&w.wolfLeap()) {
                    for(int i=0;i<12;i++) {
                        double a=i*Math.PI/6;
                        var leaf=add(Kind.SPARK,w.x(),w.y()+24,.3+rnd()*.15,i%2==0?0x99a26a:0x655b45);
                        leaf.vx=Math.cos(a)*(90+rnd()*90);leaf.vy=Math.sin(a)*80-25;
                    }
                    var bite=add(Kind.BITE,w.x(),w.y()+24,.22,0x9b9973);bite.ax=ix;bite.ay=iy;
                }
                if (from == Wisp.State.LUNGE && w.role() == Wisp.Role.REMNANT) {   // the knight's cut where the rush ends
                    var cut = add(Kind.ARC, w.x() + ix * 26, w.y() + 10 + iy * 26, .2, 0xc9d3dd); cut.ax = ix; cut.ay = iy;
                }
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
        boolean knight = w.role() == Wisp.Role.REMNANT;                       // armour charges on foot: steel streaks, dust
        var s = add(Kind.STREAK, w.x() - ix / len * 14 + (rnd() - .5) * 24, w.y() - 16 + (rnd() - .5) * 24, .12, knight ? 0xc9d3dd : 0xe2b6f5);
        if (knight && rnd() < .5) dust(w.x() - ix / len * 16, w.y() + 22, -ix / len * 50, -iy / len * 20);
        s.ax = ix / len; s.ay = iy / len;
    }
    private void chargeMote(Wisp w) {
        double ix = w.intentX(), iy = w.intentY(), len = Math.max(1e-6, Math.hypot(ix, iy));
        double mx = w.x() + ix / len * 20, my = w.y() - 24, a = rnd() * Math.PI * 2, r = 26 + rnd() * 16;
        var m = add(Kind.MOTE, mx + Math.cos(a) * r, my + Math.sin(a) * r * .7, .24,
                w.role()==Wisp.Role.MIRE_SHAMAN?0xb7c77b:0xe2b6f5);
        m.ax = mx; m.ay = my;
    }

    void hit(double x,double y,boolean critical,boolean heavy,boolean water,boolean reduced) {
        int color=critical?0xffce62:water?0x8ceafa:0xeaf6ff;
        if(!reduced)add(Kind.FLASH,x,y-20,.07,0xffffff);
        if(critical)add(Kind.GLINT,x,y-28,.22,color);
        int count=reduced?3:critical?12:heavy?10:6;
        for(int i=0;i<count;i++) {
            double angle=(i+rnd()*.4)*Math.PI*2/count;
            double speed=(heavy||critical?180:110)+rnd()*80;
            var spark=add(Kind.SPARK,x,y-20,.18+rnd()*.16,color);
            spark.vx=Math.cos(angle)*speed;spark.vy=Math.sin(angle)*speed*.65;
        }
    }

    /** Steel on a raised shield: a hard glint, and sparks thrown back toward the striker (fx,fy points from
     *  the striker to the shield). */
    void block(double x,double y,double fx,double fy,boolean reduced) {
        double back=Math.hypot(fx,fy)<1e-6?Math.PI:Math.atan2(-fy,-fx);
        add(Kind.GLINT,x,y-30,.2,0xfff0bd);
        for(int i=0;i<(reduced?4:10);i++) {
            double a=back+(rnd()-.5)*1.7,speed=170+rnd()*170;
            var s=add(Kind.SHARD,x,y-28,.12+rnd()*.1,i%2==0?0xffce62:0xfff0bd);
            s.vx=Math.cos(a)*speed;s.vy=Math.sin(a)*speed*.7;
        }
    }

    /** An enemy dies: one white flash and a burst of sparks. Its body stays where it fell (the corpse). */
    void defeated(double x, double y, boolean reduced) {
        add(Kind.FLASH, x, y - 20, .08, 0xffffff).ax = 0;
        for (int i = 0; i < (reduced ? 6 : 12); i++) {
            double a = i * Math.PI * 2 / 12 + rnd() * .4, speed = 120 + rnd() * 140;
            var s = add(Kind.SPARK, x, y - 20, .35 + rnd() * .2, i % 3 == 0 ? 0xffdd80 : 0xeaf6ff);
            s.vx = Math.cos(a) * speed; s.vy = Math.sin(a) * speed * .7 - 40;
        }
        if (!reduced) for (int i = 0; i < 4; i++) dust(x + (rnd() - .5) * 36, y + 22, (rnd() - .5) * 90, -12 * rnd());   // it hits the ground
    }

    // ------------------------------------------------------------------ drawing (native pixels, shown 2x)
    /** Dust and arcs lie on the ground, under every actor. */
    void drawGround(Graphics2D g, int cx, int cy, boolean reduced) {
        for(var w:enemies)if(w.role()==Wisp.Role.MIRE_SHAMAN&&w.state()==Wisp.State.TELEGRAPH)
            drawMire(g,w.markedX(),w.markedY(),74,w.stateSeconds(),.15+.35*w.stateSeconds()/w.telegraphDuration(),1,cx,cy,false,reduced);
        for(var hazard:hazards)if(hazard.alive())drawMire(g,hazard,cx,cy,false,reduced);
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
                    // the swing's crescent: solid while fresh, white at its edge, thinning as it sweeps out
                    double reach = 34 + t * 30, base = Math.atan2(b.ay, b.ax);
                    for (int row = 0; row < (t < .5 ? 3 : 1); row++) {
                        g.setColor(new Color(row == 0 && t < .35 ? 0xffffff : t < .35 ? 0xe2e5cc : b.colour));
                        double r = reach - row * 3;
                        for (double a = -1; a <= 1; a += .035)
                            g.fillRect(snap(b.x - cx + Math.cos(base + a) * r), snap(b.y - cy + Math.sin(base + a) * r * .7), 2, 2);
                    }
                }
                default -> { }
            }
        }
    }

    /** Alerts, glints, streaks, flashes, motes and sparks read above the actors. */
    void drawAbove(Graphics2D g, int cx, int cy, boolean reduced) {
        for(var w:enemies)if(w.role()==Wisp.Role.MIRE_SHAMAN&&w.state()==Wisp.State.TELEGRAPH)
            drawMire(g,w.markedX(),w.markedY(),74,w.stateSeconds(),.15+.35*w.stateSeconds()/w.telegraphDuration(),1,cx,cy,true,reduced);
        for(var hazard:hazards)if(hazard.alive())drawMire(g,hazard,cx,cy,true,reduced);
        for(var w:enemies)if(w.alive()&&w.ranged()&&w.state()==Wisp.State.TELEGRAPH) {
            boolean shaman=w.role()==Wisp.Role.MIRE_SHAMAN;
            double charge=Math.min(1,w.stateSeconds()/w.telegraphDuration());
            int x=snap(w.x()-cx+(shaman?0:w.intentX()*20)),y=snap(w.y()-cy-(shaman?48:24)),r=2+(int)(charge*charge*(shaman?7:6));
            g.setColor(new Color(shaman?0x293e32:0x342c44));blob(g,x,y,r+2);
            g.setColor(new Color(shaman?0x8fa961:0xab68cf));blob(g,x,y,r);
            g.setColor(new Color(charge>.8?0xffffff:shaman?0xd5dfa3:0xe2b6f5));blob(g,x-2,y-2,Math.max(1,r/2));
            if(shaman&&!reduced)for(int i=0;i<3;i++) {
                double a=w.stateSeconds()*4+i*Math.PI*2/3;
                g.fillRect(snap(x+Math.cos(a)*20),snap(y+Math.sin(a)*10),4,2);
            }
        }
        for (var b : bits) {
            double t = b.age / b.life;
            int x = snap(b.x - cx), y = snap(b.y - cy);
            switch (b.kind) {
                case CLAW -> {
                    // Paint whole outlines first so neighbouring pixels cannot cover the bright core.
                    for(int layer=1;layer>=0;layer--) {
                        g.setColor(new Color(layer==1?0x704a8c:t<.55?0xffe8fa:b.colour));
                        for(int finger=-1;finger<=1;finger++)for(double u=Math.max(0,t-.35);u<=Math.min(1,.3+t*2);u+=.025) {
                            double along=42+100*u,side=finger*18+(u-.5)*36+Math.sin(u*Math.PI)*14;
                            int px=snap(b.x-cx+b.ax*along-b.ay*side),py=snap(b.y-cy+b.ay*along+b.ax*side);
                            int width=(reduced||t>.7?4:6)+layer*4;
                            g.fillRect(px-layer*2,py-layer*2,width,width);
                        }
                    }
                }
                case BITE -> {
                    // two curved rows of fangs at body height, twice the old size: they gape, then snap shut hard; a white
                    // star where they meet and a few shards, then gone
                    double shut=Math.min(1,Math.max(0,(t-.45)/.08));
                    int bx=x,by=y-22;
                    if(t<.7) {
                        double gap=4+30*(1-shut)*(t<.45?.6+.4*t/.45:1);
                        for(int layer=1;layer>=0;layer--) {
                            g.setColor(new Color(layer==1?0x343b32:b.colour));
                            for(int jaw:new int[]{-1,1})for(int i=-12;i<=12;i++) {
                                double u=i/12.,along=i*4,side=jaw*(2+gap*(1-u*u));
                                int px=snap(bx+b.ax*along-b.ay*side),py=snap(by+b.ay*along+b.ax*side);
                                int width=(reduced?4:6)+layer*4;
                                g.fillRect(px-layer*2,py-layer*2,width,width);
                                if(layer==0&&i%3==0&&Math.abs(i)<12)for(int tooth=1;tooth<=3;tooth++)g.fillRect(
                                        snap(px+b.ay*jaw*tooth*3),snap(py-b.ax*jaw*tooth*3),4,4);
                            }
                        }
                    }
                    if(shut>=1&&t<.75) {
                        double f=(t-.53)/.22;
                        g.setColor(new Color(0xffffff));
                        g.fillRect(bx-3,by-3,6,6);
                        for(int k=4;k<(f<.3?16:10);k+=2) {
                            g.fillRect(bx+k,by-1,2,2);g.fillRect(bx-k-2,by-1,2,2);g.fillRect(bx-1,by+k,2,2);g.fillRect(bx-1,by-k-2,2,2);
                        }
                        g.setColor(new Color(0xfff0bd));
                        for(int k=0;k<5;k++) {
                            double a=k*Math.PI*2/5+.4,r=12+f*40;
                            for(int step=0;step<3;step++)g.fillRect(snap(bx+Math.cos(a)*(r+step*3)),snap(by+Math.sin(a)*(r+step*3)*.7),2,2);
                        }
                    }
                }
                case CAST -> {
                    if(reduced)continue;
                    for(double u=Math.max(0,t-.4);u<=Math.min(1,t+.35);u+=.012) {
                        double bend=Math.sin(u*Math.PI)*20;
                        int px=snap(b.x-cx+(b.ax-b.x)*u),py=snap(b.y-cy+(b.ay-b.y)*u-bend);
                        g.setColor(new Color(0x42523a));g.fillRect(px-2,py-2,6,6);
                        g.setColor(new Color(b.colour));g.fillRect(px,py,2,2);
                    }
                }
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
                    g.setColor(new Color(t < .5 ? b.colour : b.colour==0xb7c77b?0x70864b:0xab68cf));
                    g.fillRect(x, y, 2, 2);
                }
                case SPARK -> {
                    g.setColor(new Color(b.colour));
                    g.fillRect(x, y, 2, 2);
                    if (t < .4) g.fillRect(snap(x - b.vx * .012), snap(y - b.vy * .012), 2, 2);   // a short tail while fast
                }
                case SHARD -> {
                    // as long as it is fast: a thrown line first, a speck once the drag has slowed it
                    g.setColor(new Color(t < .45 ? 0xffffff : b.colour));
                    int steps = (int) Math.min(5, Math.hypot(b.vx, b.vy) / 60);
                    for (int i = 0; i <= steps; i++) g.fillRect(snap(b.x - cx - b.vx * .008 * i), snap(b.y - cy - b.vy * .008 * i), 2, 2);
                }
                default -> { }
            }
        }
    }

    /** Released magic has height and substance; the quiet warning stays a separate ground layer. */
    private static void drawMire(Graphics2D g,ForestHazard hazard,int cx,int cy,boolean above,boolean reduced) {
        double age=hazard.age(),rise=Math.min(1,age/.18),sink=Math.min(1,hazard.remaining()/.4);
        drawMire(g,hazard.x(),hazard.y(),hazard.radius(),age,rise,sink,cx,cy,above,reduced);
    }
    private static void drawMire(Graphics2D g,double wx,double wy,double r,double age,double rise,double sink,
            int cx,int cy,boolean above,boolean reduced) {
        int x=snap(wx-cx),y=snap(wy+Player.COLLISION_Y_OFFSET-cy);
        if(!above) {
            for(int i=0;i<9;i++) {
                double a=i*2.4,spread=r*(.2+(i%3)*.22);
                int px=snap(x+Math.cos(a)*spread),py=snap(y+Math.sin(a)*spread);
                g.setColor(new Color(0x242a32));blob(g,px,py,5);
                g.setColor(new Color(0x424344));blob(g,px,py,3);
                if(!reduced) {
                    double bubble=(age*1.7+i*.31)%1;
                    g.setColor(new Color(0x929664));g.fillRect(px,snap(py-bubble*18),2,2);
                }
            }
            return;
        }
        for(int i=0;i<7;i++) {
            double a=i*2.4,spread=r*(i==0?.12:.48+(i%2)*.18);
            int bx=snap(x+Math.cos(a)*spread),by=snap(y+Math.sin(a)*spread);
            int height=(int)((20+(i%3)*10)*rise*sink),lean=(i%2==0?1:-1)*8;
            for(int h=0;h<height;h+=2) {
                int px=snap(bx+lean*(double)h/Math.max(1,height)),width=h<height*.35?8:h<height*.75?6:2;
                g.setColor(new Color(0x22282a));g.fillRect(px-width/2-2,by-h,width+4,2);
                g.setColor(new Color(h>height*.7?0xabb47a:0x666e50));g.fillRect(px-width/2,by-h,width,2);
                if(h<height*.7) {g.setColor(new Color(0x899161));g.fillRect(px-width/2,by-h,2,2);}
            }
            if(height>12) {
                g.setColor(new Color(0x899161));
                for(int h=0;h<8;h+=2)g.fillRect(snap(bx-lean*.35+h*(lean>0?-1:1)),by-height/2-h,4,2);
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
