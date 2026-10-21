import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class HudOverhaulTest {
    public static void main(String[] args) throws Exception {
        var panel=new B2BJ(false);panel.setSize(1280,720);panel.game().begin();
        var hud=layer(panel,"drawHud");
        assert (hud.getRGB(30,645)>>>24)==0 : "persistent tutorial/lore panel must leave the playfield";
        assert hud.getRGB(28,28)==B2BJ.loadImage("assets/ui/hud_frame.png").getRGB(6,6)
                : "original frame ornament must remain intact";
        assert (hud.getRGB(30,152)>>>24)==0 : "vitals stay inside the original frame";
        assert hud.getRGB(52,66)==hud.getRGB(300,66) : "full HP fills the meter";
        assert hud.getRGB(40,56)!=hud.getRGB(46,56) : "meter has a stepped cap";
        assert hud.getRGB(80,58)!=hud.getRGB(80,66) && hud.getRGB(80,66)!=hud.getRGB(80,70)
                : "meter has a highlight and lower bevel";
        panel.game().player().hurt(50);
        var half=layer(panel,"drawHud");
        assert half.getRGB(52,66)!=half.getRGB(300,66) : "lost HP empties the right side";
        assert !Arrays.equals(pixels(hud,200,34,112,14),pixels(half,200,34,112,14)) : "numeric HP changes too";
        panel.game().player().collectIchor(50);
        var halfIchor=layer(panel,"drawHud");
        assert halfIchor.getRGB(52,114)!=halfIchor.getRGB(300,114) : "Ichor meter shows partial charge";
        assert hud.getRGB(52,114)!=halfIchor.getRGB(52,114) : "empty Ichor must have no gold fill";
        assert !Arrays.equals(pixels(hud,200,84,112,14),pixels(halfIchor,200,84,112,14))
                : "readable Ichor count follows charge";
        panel.game().player().collectIchor(50);
        var ready=layer(panel,"drawHud");
        assert ready.getRGB(52,114)==ready.getRGB(300,114) : "full Ichor fills one continuous meter";
        assert !Arrays.equals(pixels(halfIchor,740,600,90,12),pixels(ready,740,600,90,12))
                : "full Ichor retains the transform cue";
        var events=B2BJ.class.getDeclaredMethod("handleEvents",java.util.List.class);events.setAccessible(true);
        events.invoke(panel,java.util.List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.ABSORB_START,
                panel.game().player().x()+80,panel.game().player().y()-1)));
        var animation=B2BJ.class.getDeclaredField("slimeAnimation");animation.setAccessible(true);
        assert ((SlimeAnimation)animation.get(panel)).row()==3 : "near-horizontal absorption must retain side facing";
        panel.game().player().collectIchor(100);panel.game().player().transform();
        panel.game().player().hurt(1);
        assert !Arrays.equals(pixels(hud,32,30,288,98),pixels(layer(panel,"drawHud"),32,30,288,98))
                : "HP and form changes remain readable";
        assert !Arrays.equals(pixels(ready,482,20,316,54),pixels(layer(panel,"drawHud"),482,20,316,54))
                : "Blade duration remains visible after transforming";
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
