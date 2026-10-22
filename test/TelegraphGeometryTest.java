import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

public final class TelegraphGeometryTest {
    public static void main(String[] args) throws Exception {
        everyFrameStartsClean();
        overlapsKeepTheStrongestPixel();
        chargeAndFissureDamageStaysIntact();
        spitterCueIsCompactAndFollowsVolley();
        System.out.println("TelegraphGeometryTest passed");
    }

    private static void everyFrameStartsClean() {
        var t=new Telegraphs(300,200);t.begin();t.aim(20,60,60,60,9,.5,true);
        assert alpha(t.image(),40,60)>0;
        t.begin();t.aim(200,120,240,120,9,.5,true);
        assert alpha(t.image(),40,60)==0 : "last frame's glint must not linger";
        assert alpha(t.image(),220,120)>0;
        t.begin();
        assert java.util.Arrays.stream(t.image().getRGB(0,0,300,200,null,0,300)).allMatch(pixel->pixel==0);
    }
    private static void overlapsKeepTheStrongestPixel() {
        var t=new Telegraphs(100,100);t.begin();
        t.aim(20,50,60,50,9,1,true);
        var once=t.image().getRGB(0,0,100,100,null,0,100);
        t.aim(20,50,60,50,9,1,true);
        assert java.util.Arrays.equals(once,t.image().getRGB(0,0,100,100,null,0,100))
                : "overlapping glints must not stack into an opaque mark";
    }
    private static void chargeAndFissureDamageStaysIntact() throws Exception {
        Guardian guardian=new Guardian(300,360);guardian.activate(1000,360);
        for(int i=0;i<2000&&(guardian.attack()!=Guardian.Attack.CHARGE
                ||guardian.state()!=Guardian.State.TELEGRAPH);i++)guardian.update(.01,1000,360);
        assert guardian.attack()==Guardian.Attack.CHARGE;
        assertNoOverlay(arena(guardian));
        guardian.update(guardian.telegraphDuration()-guardian.stateSeconds(),1000,360);
        assert guardian.hits(guardian.x(),guardian.y()+92)&&!guardian.hits(guardian.x(),guardian.y()+93);
        assertNoOverlay(arena(guardian));

        guardian=new Guardian(500,360);guardian.activate(640,360);guardian.hurt(Guardian.MAX_HEALTH/2);
        for(int turn=0;turn<2;turn++) {
            for(int i=0;i<3000&&guardian.fissureBounds()==null;i++)guardian.update(.01,640,360);
            var bounds=guardian.fissureBounds();assert bounds!=null;
            assertNoOverlay(arena(guardian));
            guardian.update(guardian.telegraphDuration()-guardian.stateSeconds(),640,360);
            assert guardian.state()==Guardian.State.SLAM;
            boolean horizontal=bounds.width()>bounds.height();
            double x=bounds.centerX(),y=bounds.centerY();
            assert guardian.hits(x+(horizontal?0:42),y+(horizontal?42:0));
            assert !guardian.hits(x+(horizontal?0:43),y+(horizontal?43:0));
            assertNoOverlay(arena(guardian));
            guardian.update(guardian.activeDuration()+.01,640,360);
        }
    }
    private static void assertNoOverlay(B2BJ panel) throws Exception {
        var image=drawHazards(panel);
        assert java.util.Arrays.stream(image.getRGB(0,0,1280,720,null,0,1280)).allMatch(pixel->pixel==0)
                : "boss attacks use physical effects, without geometric overlays";
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
    private static boolean markedWithin(BufferedImage image,double x,double y,int r) {
        for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++)if(visible(image,(int)Math.round(x)+dx,(int)Math.round(y)+dy))return true;
        return false;
    }
    private static boolean visible(BufferedImage image,int x,int y) {return alpha(image,x,y)!=0;}
    private static void set(Object target,String name,Object value) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
