import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

public final class TelegraphGeometryTest {
    public static void main(String[] args) throws Exception {
        transparentMarginMatchesRadius();
        missingRingAndLaneRemainVisible();
        chargeCapsuleMatchesDamage();
        missingFissureRemainsVisible();
        spitterTellReachesActualVolley();
        largeRingRimStaysThin();
        fissureStrandsFollowLaneAxis();
        System.out.println("TelegraphGeometryTest passed");
    }
    private static BufferedImage paddedRing() {
        var image=new BufferedImage(128*8,128,BufferedImage.TYPE_INT_ARGB);
        var ink=image.createGraphics();ink.setColor(java.awt.Color.WHITE);ink.setStroke(new java.awt.BasicStroke(4));
        for(int frame=0;frame<8;frame++) {
            int margin=8+frame*2;
            ink.drawOval(frame*128+margin,margin,128-margin*2,128-margin*2);
        }
        ink.dispose();return image;
    }
    private static void transparentMarginMatchesRadius() throws Exception {
        var panel=new B2BJ(false);set(panel,"tellRing",paddedRing());
        Object cached=null;
        for(int frame=0;frame<8;frame++) {
            set(panel,"frameCounter",frame*5L);
            var image=drawRing(panel,160,160,40);
            assert visible(image,120,160)&&visible(image,200,160)
                    : "transparent ring margins must not shrink the inclusive damage boundary";
            assert visible(image,160,120)&&visible(image,160,200);
            assert !visible(image,119,160)&&!visible(image,201,160) : "warning must not invent a larger radius";
            assert !visible(image,160,160) : "ring center stays clear";
            Field field=B2BJ.class.getDeclaredField("telegraphFrameBounds");field.setAccessible(true);
            var cache=(java.util.Map<?,?>)field.get(panel);
            assert cache.size()==1 : "one cached alpha-bound set per loaded sheet";
            var current=cache.values().iterator().next();
            if(cached!=null)assert current==cached : "frame drawing must reuse precomputed bounds";
            cached=current;
        }
    }
    private static void missingRingAndLaneRemainVisible() throws Exception {
        var panel=new B2BJ(false);set(panel,"tellRing",null);set(panel,"tellChevron",null);
        var image=drawRing(panel,160,160,40);
        assert visible(image,120,160)&&visible(image,200,160) : "missing ring cannot hide damage boundary";
        var draw=B2BJ.class.getDeclaredMethod("drawTellLane",Graphics2D.class,double.class,double.class,
                double.class,double.class,int.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,40.,40.,0.,100.,16);ink.dispose();
        assert visible(image,40,40)&&visible(image,140,40) : "missing arrows need a visible complete direction lane";
        set(panel,"tellRing",new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB));
        assert visible(drawRing(panel,160,160,40),200,160) : "malformed sheet also needs a safe fallback";
        set(panel,"tellRing",new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB));
        assert visible(drawRing(panel,160,160,40),200,160) : "blank ring frame also needs a safe fallback";
    }
    private static void chargeCapsuleMatchesDamage() throws Exception {
        Guardian guardian=new Guardian(300,360);guardian.activate(1000,360);
        for(int i=0;i<2000&&(guardian.attack()!=Guardian.Attack.CHARGE
                ||guardian.state()!=Guardian.State.TELEGRAPH);i++)guardian.update(.01,1000,360);
        assert guardian.attack()==Guardian.Attack.CHARGE;
        var panel=arena(guardian);set(panel,"tellRing",paddedRing());set(panel,"tellChevron",null);
        var image=drawHazards(panel);
        int x=(int)Math.round(guardian.x()/2)*2,y=(int)Math.round(guardian.y()/2)*2;
        assert visible(image,x,y-92)&&visible(image,x,y+92) : "charge start cap must include radius92";
        guardian.update(guardian.telegraphDuration()-guardian.stateSeconds(),1000,360);
        assert guardian.hits(guardian.x(),guardian.y()+92)&&!guardian.hits(guardian.x(),guardian.y()+93);
        image=drawHazards(panel);
        assert visible(image,x,y-92)&&visible(image,x,y+92) : "active cap must match the same hit radius";
    }
    private static void missingFissureRemainsVisible() throws Exception {
        Guardian guardian=new Guardian(500,360);guardian.activate(640,360);guardian.hurt(120);
        var panel=arena(guardian);set(panel,"fissureSheet",null);
        for(int turn=0;turn<2;turn++) {
            for(int i=0;i<3000&&guardian.fissureBounds()==null;i++)guardian.update(.01,640,360);
            var bounds=guardian.fissureBounds();assert bounds!=null;
            boolean horizontal=bounds.width()>bounds.height();
            int ax=(int)bounds.centerX()-(horizontal?0:42),ay=(int)bounds.centerY()-(horizontal?42:0);
            int bx=(int)bounds.centerX()+(horizontal?0:42),by=(int)bounds.centerY()+(horizontal?42:0);
            var image=drawHazards(panel);
            assert visible(image,ax,ay)&&visible(image,bx,by) : "missing fissure must still mark its full width";
            guardian.update(guardian.telegraphDuration()-guardian.stateSeconds(),640,360);
            assert guardian.hits(ax,ay)&&guardian.hits(bx,by);
            assert !guardian.hits(bx+(horizontal?0:1),by+(horizontal?1:0));
            image=drawHazards(panel);
            assert visible(image,ax,ay)&&visible(image,bx,by) : "active fissure cannot become invisible";
            guardian.update(.5,640,360);
        }
    }
    private static B2BJ arena(Guardian guardian) throws Exception {
        var panel=new B2BJ(false);
        set(panel,"game",new RuinedOutpostGame(new RuinedOutpostMap(9),new Player(640,360),java.util.List.of(),guardian));
        return panel;
    }
    private static void fissureStrandsFollowLaneAxis() throws Exception {
        var strip=new BufferedImage(256,32,BufferedImage.TYPE_INT_ARGB);
        var ink=strip.createGraphics();ink.setColor(java.awt.Color.WHITE);
        // Transparent top/bottom margins must not split consecutive longitudinal segments.
        for(int frame=0;frame<8;frame++)ink.fillRect(frame*32+14,2,5,28);
        ink.dispose();
        Guardian guardian=new Guardian(500,360);guardian.activate(640,360);guardian.hurt(120);
        var panel=arena(guardian);set(panel,"fissureSheet",strip);
        for(int turn=0;turn<2;turn++) {
            for(int i=0;i<3000&&guardian.fissureBounds()==null;i++)guardian.update(.01,640,360);
            var bounds=guardian.fissureBounds();assert bounds!=null;
            boolean horizontal=bounds.width()>bounds.height();
            var image=drawHazards(panel);
            int lo=horizontal?(int)(bounds.centerX()-bounds.width()/2):Math.max(0,(int)(bounds.centerY()-bounds.height()/2));
            int hi=horizontal?(int)(bounds.centerX()+bounds.width()/2):Math.min(718,(int)(bounds.centerY()+bounds.height()/2));
            int center=(int)(horizontal?bounds.centerY():bounds.centerX());
            for(int axis=lo;axis<=hi;axis+=2) {
                assert visible(image,horizontal?axis:center,horizontal?center:axis)
                        : "fissure must form an uninterrupted strand along its lane axis";
                assert visible(image,horizontal?axis:center-42,horizontal?center-42:axis)
                        &&visible(image,horizontal?axis:center+42,horizontal?center+42:axis)
                        : "two authored edge strands must mark the full84px lane";
                assert !visible(image,horizontal?axis:center-44,horizontal?center-44:axis)
                        &&!visible(image,horizontal?axis:center+44,horizontal?center+44:axis)
                        : "fissure artwork must remain inside its physical lane";
            }
            guardian.update(guardian.telegraphDuration()+guardian.activeDuration(),640,360);
        }
    }
    private static void largeRingRimStaysThin() throws Exception {
        var ring=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        var ink=ring.createGraphics();ink.setColor(java.awt.Color.WHITE);ink.fillOval(8,8,112,112);
        ink.setComposite(java.awt.AlphaComposite.Clear);ink.fillOval(26,26,76,76);ink.dispose();
        var panel=new B2BJ(false);set(panel,"tellRing",ring);
        var image=drawRing(panel,260,260,234);
        int thickness=0;
        for(int x=26;x<260;x++)if(visible(image,x,260))thickness++;
        assert thickness>0&&thickness<=4 : "large danger rings must cap rim at4 native pixels, not scale into a wall";
        assert visible(image,26,260)&&visible(image,494,260) : "thin rim must retain true outer radius";
        assert !visible(image,25,260)&&!visible(image,495,260);
    }
    private static void spitterTellReachesActualVolley() throws Exception {
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
        var panel=new B2BJ(false);set(panel,"game",game);set(panel,"tellChevron",null);set(panel,"tellRing",paddedRing());
        var image=drawHazards(panel);
        assert visible(image,(int)Math.round(center.x()),(int)Math.round(center.y()))
                : "Spitter warning must reach where first-volley shots can actually travel";
        assert EnemyProjectile.MAX_TRAVEL==400 : "warning follows original speed/lifetime without a balance change";
        for(int lane=0;lane<3;lane++) {
            var shot=game.hostileProjectiles().get(lane);
            assert Math.abs(Math.atan2(shot.directionY(),shot.directionX())-(lane-1)*.20)<.00001;
            int edgeX=(int)Math.round(scout.x()+shot.directionX()*EnemyProjectile.MAX_TRAVEL
                    +Player.COLLISION_RADIUS+EnemyProjectile.RADIUS);
            int edgeY=(int)Math.round(scout.y()+shot.directionY()*EnemyProjectile.MAX_TRAVEL);
            boolean marked=false;
            for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)marked|=visible(image,edgeX+dx,edgeY+dy);
            assert marked : "volley lane "+lane+" needs radius34 terminal marker near "+edgeX+","+edgeY;
        }
    }
    private static BufferedImage drawHazards(B2BJ panel) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var draw=B2BJ.class.getDeclaredMethod("drawGuardianTelegraph",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();return image;
    }
    private static BufferedImage drawRing(B2BJ panel,double x,double y,double radius) throws Exception {
        var image=new BufferedImage(640,640,BufferedImage.TYPE_INT_ARGB);
        var draw=B2BJ.class.getDeclaredMethod("drawTellRing",Graphics2D.class,double.class,double.class,double.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,x,y,radius);ink.dispose();return image;
    }
    private static boolean visible(BufferedImage image,int x,int y) {return (image.getRGB(x,y)>>>24)!=0;}
    private static void set(Object target,String name,Object value) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
