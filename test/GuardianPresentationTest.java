import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class GuardianPresentationTest {
    public static void main(String[] args) throws Exception {
        deathIgnoresHitBlinkAndEndsCleanly();
        atlasSelectsEveryStateAndFrame();
        attacksHaveTimeAndDistinctPoses();
        System.out.println("GuardianPresentationTest passed");
    }
    private static void deathIgnoresHitBlinkAndEndsCleanly() throws Exception {
        Guardian guardian=new Guardian(900,500);guardian.activate(400,500);
        var panel=panel(guardian);
        guardian.hurt(Guardian.MAX_HEALTH);
        assert !guardian.flashVisible() : "real killing hit must reproduce the positive hurt-flash window";
        int seen=0;
        for(int tick=0;tick<50;tick++) {
            assert guardian.visible();
            assertAtlasBody(guardian,draw(panel,guardian));
            seen|=1<<guardian.animationFrame();guardian.update(.01,400,500);
        }
        assert seen==255 : "all death poses remain visible, including during the old hit blink";
        assert !guardian.visible();
        assert empty(draw(panel,guardian)) : "expired death must not render even when draw is called directly";
    }
    private static void atlasSelectsEveryStateAndFrame() throws Exception {
        Guardian guardian=new Guardian(900,500);
        var panel=panel(guardian);
        int[] seen=new int[Guardian.Animation.values().length];
        for(int tick=0;tick<300;tick++) {
            captureNewPose(panel,guardian,seen);guardian.update(.005,400,500);
        }
        guardian.activate(400,500);
        for(int tick=0;tick<12000;tick++) {
            captureNewPose(panel,guardian,seen);
            boolean complete=true;
            for(int row=0;row<Guardian.Animation.DEATH.ordinal();row++)complete&=seen[row]==255;
            if(complete)break;
            guardian.update(.005,guardian.x()+600,500);
        }
        for(int row=0;row<Guardian.Animation.DEATH.ordinal();row++)
            assert seen[row]==255 : "atlas did not display all eight poses for "+Guardian.Animation.values()[row];
        guardian.hurt(1);
        assert !guardian.flashVisible()&&empty(draw(panel,guardian)) : "living boss retains its damage blink";
    }
    private static void captureNewPose(B2BJ panel,Guardian guardian,int[] seen) throws Exception {
        int row=guardian.animation().ordinal(),bit=1<<guardian.animationFrame();
        if((seen[row]&bit)!=0)return;
        assertAtlasBody(guardian,draw(panel,guardian));seen[row]|=bit;
    }
    private static B2BJ panel(Guardian guardian) throws Exception {
        var panel=new B2BJ(false);
        Field game=B2BJ.class.getDeclaredField("game");game.setAccessible(true);
        game.set(panel,new RuinedOutpostGame(new RuinedOutpostMap(9),new Player(400,500),java.util.List.of(),guardian));
        var atlas=new BufferedImage(512,448,BufferedImage.TYPE_INT_ARGB);
        var ink=atlas.createGraphics();
        for(int row=0;row<7;row++)for(int frame=0;frame<8;frame++) {
            ink.setColor(new java.awt.Color(color(row,frame),true));ink.fillRect(frame*64,row*64,64,64);
        }
        ink.dispose();
        Field sheet=B2BJ.class.getDeclaredField("wardenMotionSheet");sheet.setAccessible(true);sheet.set(panel,atlas);
        int row=7;
        for(String name:new String[]{"guardianSweepSheet","guardianChargeSheet","guardianWaveSheet"}) {
            var action=new BufferedImage(512,64,BufferedImage.TYPE_INT_ARGB);var g=action.createGraphics();
            for(int frame=0;frame<8;frame++) {g.setColor(new java.awt.Color(color(row,frame),true));g.fillRect(frame*64,0,64,64);}
            g.dispose();sheet=B2BJ.class.getDeclaredField(name);sheet.setAccessible(true);sheet.set(panel,action);row++;
        }
        return panel;
    }
    private static BufferedImage draw(B2BJ panel,Guardian guardian) throws Exception {
        var image=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
        Method draw=B2BJ.class.getDeclaredMethod("drawGuardian",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,(int)Math.round(guardian.x())-320,(int)Math.round(guardian.y())-300);
        ink.dispose();return image;
    }
    private static void assertAtlasBody(Guardian guardian,BufferedImage image) {
        int row=guardian.animation().ordinal(),frame=guardian.animationFrame();
        if(guardian.actionFrame()>=0) {
            row=switch(guardian.attack()){case SWEEP->7;case CHARGE->8;case SHOCKWAVE->9;default->throw new AssertionError();};
            frame=guardian.actionFrame();
        }
        int expected=color(row,frame);
        int size=Guardian.RENDER_SIZE,left=320-size/2;
        int top=300+Guardian.GROUND_Y_OFFSET-Guardian.SPRITE_FOOT_ROW*4;
        assert image.getRGB(320,260)==expected : "selected boss atlas pose must be visible: "+guardian.animation()+" / "+guardian.animationFrame();
        int count=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)==expected) {
            assert x>=left&&x<left+size&&y>=top&&y<top+size : "boss ground anchor must stay fixed";
            count++;
        }
        assert count==size*size : "every atlas pose, including death, keeps its full native4x destination";
    }
    private static boolean empty(BufferedImage image) {
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)!=0)return false;
        return true;
    }
    private static void attacksHaveTimeAndDistinctPoses() throws Exception {
        var poses=new java.util.HashSet<Integer>();
        for(var attack:Guardian.Attack.values()) {
            Guardian guardian=new Guardian(900,500);guardian.activate(400,500);
            set(guardian,"attack",attack);set(guardian,"chargeX",-1.0);
            var panel=panel(guardian);double duration=guardian.telegraphDuration();
            assert duration>=1&&duration<=1.4 : "every attack needs a readable preparation";
            double tx=guardian.targetX(),ty=guardian.targetY(),dx=guardian.directionX();
            guardian.update(duration*.7,1400,100);
            assert guardian.state()==Guardian.State.TELEGRAPH&&!guardian.hits(tx,ty);
            assert guardian.targetX()==tx&&guardian.targetY()==ty&&guardian.directionX()==dx : "no late retargeting";
            var rendered=draw(panel,guardian);assertAtlasBody(guardian,rendered);
            poses.add(rendered.getRGB(320,260));
            guardian.update(duration*.3,1400,100);
            assert guardian.state()==Guardian.State.SLAM&&guardian.impactNumber()==1;
            assertAtlasBody(guardian,draw(panel,guardian));
        }
        assert poses.size()==5 : "five attacks must not silently share the same preparation pose";
        var briar=new Guardian(0,0,Guardian.Profile.BRIARHEART);briar.activate(100,0);
        assert briar.telegraphDuration()==1.05&&briar.actionFrame()==-1 : "other boss art and timing stay separate";
    }
    private static void set(Object object,String name,Object value) throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    private static int color(int row,int frame) {return 0xff000067|(32+row*24)<<16|(32+frame*24)<<8;}
}
