import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

public final class TelegraphGeometryTest {
    public static void main(String[] args) throws Exception {
        discOutlineIsTheInclusiveHitBoundary();
        chargingFillKeepsTerrainVisible();
        largeRingsStayOnePixelThin();
        everyFrameStartsClean();
        overlapsKeepTheStrongestPixel();
        chargeCapsuleMatchesDamage();
        fissureEdgesMatchDamage();
        spitterCueIsCompactAndFollowsVolley();
        System.out.println("TelegraphGeometryTest passed");
    }

    private static void discOutlineIsTheInclusiveHitBoundary() {
        for(int r:new int[]{20,56,83})for(double charge:new double[]{0,.5,1})for(double hit:new double[]{0,.6}) {
            var t=new Telegraphs(400,400);t.begin();t.disc(200,200,r,charge,hit,false);var image=t.image();
            assert alpha(image,200-r,200)>0&&alpha(image,200+r,200)>0&&alpha(image,200,200-r)>0&&alpha(image,200,200+r)>0
                    : "outline must sit on the inclusive damage boundary r="+r;
            assert alpha(image,199-r,200)==0&&alpha(image,201+r,200)==0&&alpha(image,200,199-r)==0&&alpha(image,200,201+r)==0
                    : "a warning must never mark ground beyond its damage r="+r+" charge="+charge+" hit="+hit;
        }
    }
    private static void chargingFillKeepsTerrainVisible() {
        int r=60;
        for(int step=0;step<=10;step++) {
            double charge=step/10.;
            var t=new Telegraphs(200,200);t.begin();t.disc(100,100,r,charge,0,false);var image=t.image();
            int strong=0;
            for(int y=100-r+1;y<100+r;y++)for(int x=100-r+1;x<100+r;x++) {
                if(Math.hypot(x-100,y-100)>r-1.5)continue;   // the outline itself
                int a=alpha(image,x,y);
                assert a<=Telegraphs.FRONT_ALPHA : "interior never turns opaque";
                if(a>Telegraphs.FILL_ALPHA)strong++;
                if(step==0)assert a==0 : "an uncharged warning leaves its whole interior clear";
            }
            // Only the 1 px charge front may be stronger than the faint fill.
            assert strong<=8*r*charge+8 : "charge front must stay a thin line, found "+strong;
        }
    }
    private static void largeRingsStayOnePixelThin() {
        var t=new Telegraphs(640,640);t.begin();t.disc(320,320,234,0,0,false);var image=t.image();
        int painted=0;
        for(int x=0;x<=320;x++)if(alpha(image,x,320)>0)painted++;
        assert painted==1&&alpha(image,320-234,320)>0 : "a whole-arena ring is one native pixel thick, found "+painted;
    }
    private static void everyFrameStartsClean() {
        var t=new Telegraphs(300,200);t.begin();t.disc(60,60,30,.5,0,false);
        t.begin();t.disc(220,120,30,.5,0,false);
        assert alpha(t.image(),60,60)==0&&alpha(t.image(),30,60)==0 : "last frame's warning must not linger";
        assert alpha(t.image(),190,120)>0;
    }
    private static void overlapsKeepTheStrongestPixel() {
        var t=new Telegraphs(300,200);t.begin();
        t.disc(130,100,50,1,0,false);t.disc(170,100,50,1,0,false);
        assert alpha(t.image(),150,100)==Telegraphs.FILL_ALPHA : "overlapping fills must not stack into a solid blob";
    }
    private static void chargeCapsuleMatchesDamage() throws Exception {
        Guardian guardian=new Guardian(300,360);guardian.activate(1000,360);
        for(int i=0;i<2000&&(guardian.attack()!=Guardian.Attack.CHARGE
                ||guardian.state()!=Guardian.State.TELEGRAPH);i++)guardian.update(.01,1000,360);
        assert guardian.attack()==Guardian.Attack.CHARGE;
        var panel=arena(guardian);
        var image=drawHazards(panel);
        int x=(int)Math.round(guardian.x()/2)*2,y=(int)Math.round(guardian.y()/2)*2;
        assert visible(image,x,y-92)&&visible(image,x,y+92) : "charge path must be as wide as its 92px hit radius";
        assert !visible(image,x,y-94)&&!visible(image,x,y+94);
        for(int py=0;py<720;py+=2)for(int px=0;px<1280;px+=2)
            assert image.getRGB(px,py)==image.getRGB(px+1,py+1) : "warnings are painted natively, then shown at 2x";
        guardian.update(guardian.telegraphDuration()-guardian.stateSeconds(),1000,360);
        assert guardian.hits(guardian.x(),guardian.y()+92)&&!guardian.hits(guardian.x(),guardian.y()+93);
        image=drawHazards(panel);
        x=(int)Math.round(guardian.x()/2)*2;y=(int)Math.round(guardian.y()/2)*2;
        assert visible(image,x,y-92)&&visible(image,x,y+92) : "a live charge keeps marking its true hit radius";
    }
    private static void fissureEdgesMatchDamage() throws Exception {
        Guardian guardian=new Guardian(500,360);guardian.activate(640,360);guardian.hurt(Guardian.MAX_HEALTH/2);
        var panel=arena(guardian);
        for(int turn=0;turn<2;turn++) {
            for(int i=0;i<3000&&guardian.fissureBounds()==null;i++)guardian.update(.01,640,360);
            var bounds=guardian.fissureBounds();assert bounds!=null;
            boolean horizontal=bounds.width()>bounds.height();
            int center=(int)(horizontal?bounds.centerY():bounds.centerX());
            int lo=Math.max(0,(int)((horizontal?bounds.centerX()-bounds.width()/2:bounds.centerY()-bounds.height()/2)));
            int hi=Math.min(horizontal?1278:718,(int)((horizontal?bounds.centerX()+bounds.width()/2:bounds.centerY()+bounds.height()/2)));
            for(int phase=0;phase<2;phase++) {
                var image=drawHazards(panel);
                for(int axis=lo;axis<=hi;axis+=2) {
                    assert visible(image,horizontal?axis:center-42,horizontal?center-42:axis)
                            &&visible(image,horizontal?axis:center+42,horizontal?center+42:axis)
                            : "both edges of the 84px fissure lane are marked along its whole length";
                    assert !visible(image,horizontal?axis:center-44,horizontal?center-44:axis)
                            &&!visible(image,horizontal?axis:center+44,horizontal?center+44:axis)
                            : "fissure warning stays inside its physical lane";
                }
                if(phase==0) {
                    guardian.update(guardian.telegraphDuration()-guardian.stateSeconds(),640,360);
                    assert guardian.state()==Guardian.State.SLAM;
                    double along=horizontal?bounds.centerX():bounds.centerY();
                    assert guardian.hits(horizontal?along:center-42,horizontal?center-42:along)
                            &&!guardian.hits(horizontal?along:center-43,horizontal?center-43:along);
                }
            }
            guardian.update(guardian.activeDuration()+.01,640,360);
        }
    }
    private static void spitterCueIsCompactAndFollowsVolley() throws Exception {
        var scout=new Wisp(320,360,320,320,2,Wisp.Role.SPITTER);
        for(int i=0;i<100&&scout.state()!=Wisp.State.TELEGRAPH;i++)scout.update(.01,560,360);
        assert scout.state()==Wisp.State.TELEGRAPH;
        var game=new RuinedOutpostGame(new RuinedOutpostMap(9),new Player(560,360),java.util.List.of(scout),new Guardian(1100,500));
        var volley=RuinedOutpostGame.class.getDeclaredMethod("emitSpit",Wisp.class);volley.setAccessible(true);volley.invoke(game,scout);
        assert game.hostileProjectiles().size()==3;
        var center=game.hostileProjectiles().get(1);
        assert center.x()==scout.x()&&center.y()==scout.y() : "warning origin must match zero-offset shot spawn";
        center.advance(EnemyProjectile.LIFETIME-.001);
        assert center.alive()&&Math.abs(center.x()-scout.x()-399.75)<.001;
        var panel=new B2BJ(false);set(panel,"game",game);
        int aimingAlpha=0;
        for(boolean locked:new boolean[]{false,true}) {
            while(locked&&!scout.directionLocked())scout.update(.01,560,360);
            var image=drawHazards(panel);
            int strongest=0;
            for(int y=0;y<720;y+=2)for(int x=0;x<1280;x+=2) {
                int a=alpha(image,x,y);strongest=Math.max(strongest,a);
                assert a<=190 : "projectile cue stays translucent, including its small charge spark";
                if(a>0) assert Math.hypot(x-scout.x(),y-scout.y())<=74
                        : "projectile warning stays near its caster, not across the arena";
                assert image.getRGB(x,y)==image.getRGB(x+1,y+1) : "soft cues keep native pixel edges";
            }
            assert strongest>20 : "windup must still be visible";
            if(locked)assert strongest>aimingAlpha : "locked windup brightens before release";
            else aimingAlpha=strongest;
            for(int l=0;l<3;l++) {
                var shot=game.hostileProjectiles().get(l);
                assert Math.abs(Math.atan2(shot.directionY(),shot.directionX())-(l-1)*.20)<.00001;
                double ux=shot.directionX(),uy=shot.directionY();
                double near=(B2BJ.AIM_START+B2BJ.SHOT_CUE_LENGTH/2)*2;
                assert markedWithin(image,scout.x()+ux*near,scout.y()+uy*near,4)
                        : "compact cue follows volley heading "+l;
                double far=EnemyProjectile.MAX_TRAVEL/2;
                assert !markedWithin(image,scout.x()+ux*far,scout.y()+uy*far,8)
                        : "long targeting lines must be gone";
            }
        }
        while(scout.state()==Wisp.State.TELEGRAPH)scout.update(.01,560,360);
        var released=drawHazards(panel);
        assert java.util.Arrays.stream(released.getRGB(0,0,1280,720,null,0,1280)).allMatch(p->(p>>>24)==0)
                : "windup cue clears when the projectile releases";
        assert EnemyProjectile.MAX_TRAVEL==400 : "warning follows original speed/lifetime without a balance change";
    }

    private static B2BJ arena(Guardian guardian) throws Exception {
        var panel=new B2BJ(false);
        set(panel,"game",new RuinedOutpostGame(new RuinedOutpostMap(9),new Player(640,360),java.util.List.of(),guardian));
        return panel;
    }
    private static BufferedImage drawHazards(B2BJ panel) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var draw=B2BJ.class.getDeclaredMethod("drawGuardianTelegraph",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();return image;
    }
    private static int alpha(BufferedImage image,int x,int y) {return image.getRGB(x,y)>>>24;}
    private static boolean marked(BufferedImage image,double x,double y) {
        for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++)if(visible(image,(int)Math.round(x)+dx,(int)Math.round(y)+dy))return true;
        return false;
    }
    private static boolean markedWithin(BufferedImage image,double x,double y,int r) {
        for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++)if(visible(image,(int)Math.round(x)+dx,(int)Math.round(y)+dy))return true;
        return false;
    }
    private static boolean visible(BufferedImage image,int x,int y) {return alpha(image,x,y)!=0;}
    private static void set(Object target,String name,Object value) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
