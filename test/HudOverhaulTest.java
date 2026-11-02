import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class HudOverhaulTest {
    public static void main(String[] args) throws Exception {
        for(var art:new BufferedImage[]{JourneyHud.SLASH,JourneyHud.WAVE,JourneyHud.DASH,JourneyHud.FORM})
            assert art!=null&&art.getWidth()==78&&art.getHeight()==78 : "approved skill illustrations must be available";
        var panel=new B2BJ(false);panel.setSize(1280,720);panel.game().begin();
        var hud=layer(panel,"drawHud");
        assert (hud.getRGB(30,645)>>>24)==0 : "persistent tutorial/lore panel must leave the playfield";
        assert (hud.getRGB(300,40)>>>24)==0 : "the old upper-left resource block is gone";
        assert (hud.getRGB(412,585)>>>24)>0 : "bottom ribbon has a legible backplate";
        assert hud.getRGB(432,594)==hud.getRGB(608,594) : "full HP fills the meter";
        assert hud.getRGB(419,589)!=hud.getRGB(426,589) : "meter has a tapered cap";
        assert hud.getRGB(448,591)!=hud.getRGB(448,597) : "HP fill has visible depth";
        panel.game().player().hurt(50);
        var half=layer(panel,"drawHud");
        assert half.getRGB(432,594)!=half.getRGB(608,594) : "lost HP empties the right side";
        assert !Arrays.equals(pixels(hud,488,562,136,24),pixels(half,488,562,136,24)) : "numeric HP changes too";
        panel.game().player().collectIchor(50);
        var halfIchor=layer(panel,"drawHud");
        assert halfIchor.getRGB(672,594)!=halfIchor.getRGB(848,594) : "Ichor meter shows partial charge";
        assert hud.getRGB(672,594)!=halfIchor.getRGB(672,594) : "empty Ichor must have no gold fill";
        assert !Arrays.equals(pixels(hud,760,562,104,24),pixels(halfIchor,760,562,104,24)) : "Ichor count follows charge";
        panel.game().player().collectIchor(50);
        var ready=layer(panel,"drawHud");
        assert ready.getRGB(672,594)==ready.getRGB(848,594) : "full Ichor fills one continuous meter";
        assert !Arrays.equals(pixels(halfIchor,500,530,280,24),pixels(ready,500,530,280,24)) : "full Ichor retains the transform cue";
        var events=B2BJ.class.getDeclaredMethod("handleEvents",java.util.List.class);events.setAccessible(true);
        events.invoke(panel,java.util.List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.ABSORB_START,
                panel.game().player().x()+80,panel.game().player().y()-1)));
        var animation=B2BJ.class.getDeclaredField("slimeAnimation");animation.setAccessible(true);
        assert ((SlimeAnimation)animation.get(panel)).row()==3 : "near-horizontal absorption must retain side facing";
        panel.game().player().collectIchor(100);panel.game().player().transform();
        panel.game().player().hurt(1);
        assert !Arrays.equals(pixels(hud,400,556,480,58),pixels(layer(panel,"drawHud"),400,556,480,58))
                : "HP and form changes remain readable";
        assert !Arrays.equals(pixels(ready,16,84,191,50),pixels(layer(panel,"drawHud"),16,84,191,50))
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
        var measure=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB).createGraphics();
        for(String goal:new String[]{"Find Shuna at Captain Camp","Return to Captain Camp and speak with Shuna about the evacuation order"}) {
            var lines=JourneyHud.wrap(measure,goal,JourneyHud.BODY,218);
            assert String.join(" ",lines).equals(goal) : "objective wrapping must preserve every word";
            for(String line:lines)assert measure.getFontMetrics(JourneyHud.BODY).stringWidth(line)<=218 : "objective text must fit the plaque";
        }
        measure.dispose();
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
