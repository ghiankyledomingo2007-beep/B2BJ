import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class TideLayerTest {
    public static void main(String[] args) throws Exception {
        boolean muted=GameAudio.muted();GameAudio.setMuted(true);
        try {
            var panel=new B2BJ(false);panel.game().begin();panel.game().player().relocate(640,360);
            set(panel,"tideReleaseSheet",solidSheet(0xffff00ff));
            set(panel,"tideBreakSheet",solidSheet(0xff1122dd));
            var baseline=draw(panel,"drawWorld");
            var body=draw(panel,"drawPlayer");
            emit(panel,RuinedOutpostGame.EventType.TIDE_RELEASE,640,360);
            assertBodyUnchanged(baseline,draw(panel,"drawWorld"),body);
            assert draw(panel,"drawImpacts").getRGB(640,360)==0xffff00ff
                    : "three-argument effect rendering still includes the full-opacity release";
            Method update=B2BJ.class.getDeclaredMethod("updateImpacts",double.class);update.setAccessible(true);update.invoke(panel,.2);
            assertBodyUnchanged(baseline,draw(panel,"drawWorld"),body);
            emit(panel,RuinedOutpostGame.EventType.TIDE_IMPACT,640,360);
            var collision=draw(panel,"drawWorld");
            int covered=0;
            for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if((body.getRGB(x,y)>>>24)==255) {
                assert collision.getRGB(x,y)!=baseline.getRGB(x,y) : "collision splash must retain foreground depth";
                covered++;
            }
            assert covered>100;
            panel.game().player().relocate(840,360);
            var effects=draw(panel,"drawImpacts");
            assert effects.getRGB(640,360)==0xff1122dd&&(effects.getRGB(840,360)>>>24)==0
                    : "moving player cannot drag either impact away from event anchor";
            for(int i=0;i<70;i++)emit(panel,RuinedOutpostGame.EventType.TIDE_RELEASE,640,360);
            Field field=B2BJ.class.getDeclaredField("impacts");field.setAccessible(true);
            assert ((List<?>)field.get(panel)).size()==64 : "ground rendering keeps the existing bounded queue";
            System.out.println("TideLayerTest passed");
        } finally {GameAudio.setMuted(muted);}
    }
    private static void assertBodyUnchanged(BufferedImage baseline,BufferedImage release,BufferedImage body) {
        int preserved=0,ground=0;
        for(int y=0;y<720;y++)for(int x=0;x<1280;x++) {
            if((body.getRGB(x,y)>>>24)==255) {
                assert release.getRGB(x,y)==baseline.getRGB(x,y)
                        : "opaque Tide release must paint behind the staged Blob, not cover it";
                preserved++;
            } else if(release.getRGB(x,y)!=baseline.getRGB(x,y))ground++;
        }
        assert preserved>100&&ground>100 : "fixture must show both actual body and surrounding burst";
    }
    private static BufferedImage solidSheet(int color) {
        var image=new BufferedImage(512,64,BufferedImage.TYPE_INT_ARGB);
        var ink=image.createGraphics();ink.setColor(new java.awt.Color(color,true));ink.fillRect(0,0,512,64);ink.dispose();return image;
    }
    private static void emit(B2BJ panel,RuinedOutpostGame.EventType type,double x,double y) throws Exception {
        Method method=B2BJ.class.getDeclaredMethod("handleEvents",List.class);method.setAccessible(true);
        method.invoke(panel,List.of(new RuinedOutpostGame.Event(type,x,y,true)));
    }
    private static BufferedImage draw(B2BJ panel,String method) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        Method draw=B2BJ.class.getDeclaredMethod(method,Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();return image;
    }
    private static void set(B2BJ panel,String name,Object value) throws Exception {
        Field field=B2BJ.class.getDeclaredField(name);field.setAccessible(true);field.set(panel,value);
    }
}
