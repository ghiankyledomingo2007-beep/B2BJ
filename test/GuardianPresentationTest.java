import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class GuardianPresentationTest {
    public static void main(String[] args) throws Exception {
        deathIgnoresHitBlinkAndEndsCleanly();
        atlasSelectsEveryStateAndFrame();
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
        return panel;
    }
    private static BufferedImage draw(B2BJ panel,Guardian guardian) throws Exception {
        var image=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
        Method draw=B2BJ.class.getDeclaredMethod("drawGuardian",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,(int)Math.round(guardian.x())-320,(int)Math.round(guardian.y())-300);
        ink.dispose();return image;
    }
    private static void assertAtlasBody(Guardian guardian,BufferedImage image) {
        int expected=color(guardian.animation().ordinal(),guardian.animationFrame());
        assert image.getRGB(320,260)==expected : "selected boss atlas pose must be visible: "+guardian.animation()+" / "+guardian.animationFrame();
        int count=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)==expected) {
            assert x>=224&&x<=415&&y>=180&&y<=371 : "boss ground anchor must stay fixed";
            count++;
        }
        assert count==192*192 : "every atlas pose, including death, keeps its full native3x destination";
    }
    private static boolean empty(BufferedImage image) {
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)!=0)return false;
        return true;
    }
    private static int color(int row,int frame) {return 0xff000067|(32+row*24)<<16|(32+frame*24)<<8;}
}
