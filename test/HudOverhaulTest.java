import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class HudOverhaulTest {
    public static void main(String[] args) throws Exception {
        var panel=new B2BJ(false);panel.setSize(1280,720);panel.game().begin();
        var hud=layer(panel,"drawHud");
        assert (hud.getRGB(30,645)>>>24)==0 : "persistent tutorial/lore panel must leave the playfield";
        var heart=B2BJ.loadImage("assets/ui/vitality.png");
        for(int y=0;y<32;y++)for(int x=0;x<5;x++)if((heart.getRGB(x,y)>>>24)==255)
            assert hud.getRGB(32+x*2,42+y*2)==heart.getRGB(x,y) : "full vitality icons must not clip their edges";
        var events=B2BJ.class.getDeclaredMethod("handleEvents",java.util.List.class);events.setAccessible(true);
        events.invoke(panel,java.util.List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.ABSORB_START,
                panel.game().player().x()+80,panel.game().player().y()-1)));
        var animation=B2BJ.class.getDeclaredField("slimeAnimation");animation.setAccessible(true);
        assert ((SlimeAnimation)animation.get(panel)).row()==3 : "near-horizontal absorption must retain side facing";
        panel.game().player().collectIchor(100);panel.game().player().transform();
        panel.game().player().hurt(1);
        assert !Arrays.equals(pixels(hud,16,16,340,110),pixels(layer(panel,"drawHud"),16,16,340,110))
                : "fractional health remains readable";
        var map=B2BJ.class.getDeclaredMethod("drawMap",java.awt.Graphics2D.class,boolean.class);
        map.setAccessible(true);
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();map.invoke(panel,g,true);g.dispose();
        assert (image.getRGB(640,360)>>>24)>0 : "expanded map renders";
        assert !panel.game().paused() : "map preserves the live simulation contract";
        var pause=layer(panel,"drawPause");
        assert (pause.getRGB(640,360)>>>24)>0 : "controls remain available on pause screen";
        panel.game().pause();
        var paused=paint(panel);
        var shown=B2BJ.class.getDeclaredField("mapShown");shown.setAccessible(true);shown.set(panel,true);
        assert !Arrays.equals(pixels(paused,112,40,1056,640),pixels(paint(panel),112,40,1056,640))
                : "map must remain accessible while paused";
        assert paint(panel).getRGB(112,40)==image.getRGB(112,40) : "pause must not obscure the map";
        assert panel.game().paused() : "viewing paused map must not resume danger";
        System.out.println("HudOverhaulTest passed");
    }
    private static BufferedImage layer(B2BJ panel,String name)throws Exception {
        Method method=B2BJ.class.getDeclaredMethod(name,java.awt.Graphics2D.class);method.setAccessible(true);
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();method.invoke(panel,g);g.dispose();return image;
    }
    private static BufferedImage paint(B2BJ panel) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();return image;
    }
    private static int[] pixels(BufferedImage image,int x,int y,int w,int h){return image.getRGB(x,y,w,h,null,0,w);}
}
