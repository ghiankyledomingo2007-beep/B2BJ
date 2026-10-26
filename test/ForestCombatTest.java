import java.awt.image.BufferedImage;

public final class ForestCombatTest {
    public static void main(String[] args) throws Exception {
        feint();wolf();shaman();hazards();renderedGround();clawEffects();
        System.out.println("ForestCombatTest passed: planted Imp double swipe, two Wolf bites before pounce, locked landing, interruptible bloom and hazards");
    }
    static void step(CampaignEnemy enemy,double seconds,double x,double y) {
        for(double t=0;t<seconds-1e-8;t+=.01)enemy.update(.01,x,y,null);
    }
    static void feint() {
        var enemy=new CampaignEnemy(CampaignEnemy.Kind.RIFT_IMP,500,500);var body=enemy.body();
        step(enemy,.01,610,500);assert body.state()==Wisp.State.TELEGRAPH;
        step(enemy,.25,610,500);assert Math.abs(body.y()-500)>25 : "imp actually steps sideways";
        step(enemy,.35,610,500);double ix=body.intentX(),iy=body.intentY();
        assert body.directionLocked();step(enemy,.31,300,300);
        assert body.state()==Wisp.State.LUNGE&&Math.abs(body.intentX()-ix)<.001&&Math.abs(body.intentY()-iy)<.001;
        double x=body.x(),y=body.y(),tx=x+ix*90,ty=y+iy*90;
        assert body.stationaryMelee()&&body.lungeReach()==0;
        step(enemy,.04,300,300);assert enemy.hits(tx,ty)&&!enemy.hits(x-ix*90,y-iy*90);
        step(enemy,.18,300,300);assert !enemy.hits(tx,ty) : "gap between claw strokes";
        step(enemy,.18,300,300);assert enemy.hits(tx,ty) : "second planted claw stroke";
        step(enemy,.16,300,300);assert !enemy.hits(tx,ty);
        assert body.x()==x&&body.y()==y : "the combo never jumps or lunges forward";
        assert enemy.releaseShots().isEmpty();
    }
    static void wolf() {
        var close=new CampaignEnemy(CampaignEnemy.Kind.THORN_WOLF,500,500);step(close,.01,600,500);
        assert close.body().stationaryMelee()&&!close.body().wolfLeap();step(close,.81,600,500);
        assert close.hits(600,500)&&!close.hits(400,500)&&close.body().x()==500 : "bite is a planted forward cone";
        var approach=new CampaignEnemy(CampaignEnemy.Kind.THORN_WOLF,500,500);
        step(approach,.6,780,500);assert !approach.body().wolfLeap()&&approach.body().state()==Wisp.State.PURSUE
                &&approach.body().x()>550 : "first approach runs on the ground instead of jumping";
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            var wolf=new CampaignEnemy(CampaignEnemy.Kind.THORN_WOLF,500,500);var body=wolf.body();
            for(int bite=0;bite<2;bite++) {
                for(int i=0;i<200&&body.state()!=Wisp.State.LUNGE;i++)step(wolf,.01,600,500);
                assert body.state()==Wisp.State.LUNGE&&!body.wolfLeap();
                while(body.state()==Wisp.State.LUNGE)step(wolf,.01,600,500);
                while(body.state()==Wisp.State.RECOVER)step(wolf,.01,600,500);
            }
            double tx=500+d[0]*280,ty=500+d[1]*280;
            step(wolf,.61,tx,ty);assert body.wolfLeap()&&body.directionLocked();
            double mx=body.markedX(),my=body.markedY();
            step(wolf,.56,500-d[0]*100,500-d[1]*100);
            assert body.markedX()==mx&&body.markedY()==my : "landing commits before leap";
            assert !wolf.hits(body.x(),body.y()) : "airborne wolf does not hit along its entire route";
            step(wolf,.39,200,200);assert wolf.hits(mx,my)&&!wolf.hits(mx+100,my+100);
        }
    }
    static void shaman() {
        var mage=new CampaignEnemy(CampaignEnemy.Kind.MIRE_SHAMAN,500,500);var body=mage.body();
        step(mage,.01,760,500);assert body.state()==Wisp.State.TELEGRAPH;
        double mx=body.markedX(),my=body.markedY();step(mage,1.41,500,760);
        assert body.markedX()==mx&&body.markedY()==my;
        assert mage.releaseShots().isEmpty() : "shaman is not a reskinned spitter";
        var released=mage.releaseHazards();assert released.size()==1&&released.get(0).x()==mx&&released.get(0).y()==my;
        assert mage.releaseHazards().isEmpty();
        var interrupted=new CampaignEnemy(CampaignEnemy.Kind.MIRE_SHAMAN,500,500);
        step(interrupted,.9,760,500);interrupted.receiveDamage(1,true,true);step(interrupted,.2,760,500);
        assert interrupted.releaseHazards().isEmpty() : "interrupting its tell prevents the bloom";
        var dead=new CampaignEnemy(CampaignEnemy.Kind.MIRE_SHAMAN,500,500);step(dead,1.42,760,500);
        dead.receiveDamage(10000,true,true);assert dead.releaseHazards().isEmpty() : "outgoing damage resolves before pending release";
    }
    static void hazards() {
        var pool=new ForestHazard(100,100,74,3.6);
        assert pool.hit(100,100)&&!pool.hit(100,100);pool.update(.91);
        assert !pool.hit(175,100)&&pool.hit(174,100);
        pool.update(3);assert !pool.alive()&&!pool.hit(100,100);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,0}) {
            try {new ForestHazard(0,0,invalid,3);throw new AssertionError("invalid radius");}
            catch(IllegalArgumentException expected) { }
        }
    }
    static void renderedGround() {
        var pixels=new Telegraphs(640,360);pixels.begin();pixels.bloom(320,180,37,1,false);
        var image=pixels.image();int roots=0;
        for(int y=135;y<225;y++)for(int x=275;x<365;x++)if(image.getRGB(x,y)>>>24!=0) {
            assert Math.hypot(x-320,y-180)<39 : "telegraph cannot claim space beyond its real bloom boundary";
            roots++;
        }
        assert roots>400;pixels.begin();assert pixels.image().getRGB(320,180)==0;
    }
    static void clawEffects() {
        var imp=new CampaignEnemy(CampaignEnemy.Kind.RIFT_IMP,100,100);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(210,100),
                java.util.List.of(imp.body()),new Guardian(1500,900));
        var fx=new MobFx();boolean visible=false;int bursts=0;
        for(int frame=0;frame<160;frame++) {
            imp.update(.01,210,100,null);fx.update(.01,game);
            var image=new BufferedImage(320,240,2);var g=image.createGraphics();fx.drawAbove(g,0,0,false);g.dispose();
            boolean now=java.util.Arrays.stream(image.getRGB(0,0,320,240,null,0,320))
                    .anyMatch(rgb->rgb==0xffffe8fa||rgb==0xffe1c8ef);
            if(now&&!visible)bursts++;visible=now;
        }
        assert bursts==2 : "each planted claw stroke needs its own visible slash: "+bursts;
        fx.clear();var image=new BufferedImage(320,240,2);var g=image.createGraphics();fx.drawAbove(g,0,0,false);g.dispose();
        assert java.util.Arrays.stream(image.getRGB(0,0,320,240,null,0,320)).allMatch(rgb->rgb==0);
    }
}
