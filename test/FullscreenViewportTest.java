import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public final class FullscreenViewportTest {
    private static Object field(Object object,String name) throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            try {
                titleScaling();
                bladeHudClearsSageAndDebugLabel();
                var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
                var panel=new B2BJ(false,game);game.player().relocate(3000,2000);
                int originalSkillWidth=0;
                for(var size:new int[][]{{1280,720},{1920,1080},{1600,900},{2560,1080},{1280,720}}) {
                    int w=size[0],h=size[1];panel.setSize(w,h);panel.doLayout();
                    double z=B2BJ.worldZoom(w,h);              // the world is zoomed on big screens
                    assert Math.abs(game.player().x()-panel.worldCameraX()-w/2./z)<=1;
                    assert Math.abs(game.player().y()-panel.worldCameraY()-h/2./z)<=1;
                    var tells=(Telegraphs)field(panel,"tells");
                    assert tells.image().getWidth()==(w+1)/2&&tells.image().getHeight()==(h+1)/2;
                    panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,w/2+150,h/2,0,false));
                    var aim=B2BJ.class.getDeclaredMethod("aimDirection");aim.setAccessible(true);int[] direction=(int[])aim.invoke(panel);
                    assert direction[0]>0&&direction[1]==0 : "fullscreen mouse aim must use the displayed camera";
                    var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);
                    var button=(javax.swing.JButton)field(panel,"menuButton");
                    double hudScale=JourneyHud.scale(w,h);
                    assert button.getX()==Math.round(w-140*hudScale)&&button.getX()+button.getWidth()<=w;
                    assert SwingUtilities.getDeepestComponentAt(panel,button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2)==button
                            : "scaled Menu button must retain its hit target";
                    button.doClick();panel.doLayout();
                    var menu=(javax.swing.JPanel)field(panel,"menu");
                    assert menu.isVisible()&&Math.abs(menu.getX()+menu.getWidth()/2-w/2)<=1;
                    assert Math.abs(menu.getY()+menu.getHeight()/2-h/2)<=1;
                    button.doClick();
                    var hud=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var hg=hud.createGraphics();
                    var drawHud=B2BJ.class.getDeclaredMethod("drawPlayerHud",java.awt.Graphics2D.class);drawHud.setAccessible(true);drawHud.invoke(panel,hg);hg.dispose();
                    int hw=(int)Math.round(w/hudScale),hh=(int)Math.round(h/hudScale);
                    var first=JourneyHud.skill(hw,hh,false,0);var last=JourneyHud.skill(hw,hh,false,3);
                    assert Math.abs((first.x+last.x+last.width)*hudScale/2-w/2.)<=1 : "abilities remain centered";
                    int skillWidth=(int)Math.round(first.width*hudScale);
                    if(originalSkillWidth==0)originalSkillWidth=skillWidth;
                    if(h==1080)assert skillWidth>originalSkillWidth*1.45 : "fullscreen skills must grow with the approved composition";
                    for(int i=0;i<4;i++) {
                        var slot=JourneyHud.skill(hw,hh,false,i);int sx=(int)Math.round(slot.x*hudScale),sy=(int)Math.round(slot.y*hudScale);
                        assert (hud.getRGB(sx+2,sy+skillWidth/2)>>>24)>0 : "square skill frame left edge is painted";
                        assert (hud.getRGB(sx+skillWidth/2,sy+skillWidth-2)>>>24)>0 : "square skill frame bottom edge is painted";
                        assert sy+(slot.height+2)*hudScale<h : "key tabs stay inside the screen";
                    }
                    var ribbon=JourneyHud.ribbon(hw,hh);double fraction=ribbon.width*hudScale/Math.min(w,h*16./9);
                    assert fraction>=.36&&fraction<=.39 : "resource ribbon must retain the approved roughly 37% width";
                    var loading=(LoadingScreen)field(panel,"loading");loading.start(null,LoadingScreen.Kind.ENTER,1);
                    panel.paint(g);g.dispose();var stage=(BufferedImage)field(loading,"stage");
                    assert image.getRGB(w-1,h-1)==stage.getRGB(1279,719) : "loading screen must cover the fullscreen corner";
                    loading.update(10);
                }
            } catch(Exception e) {throw new RuntimeException(e);}
        });
        System.out.println("FullscreenViewportTest passed: title scaling/hit targets, resize, camera, mouse aim, telegraphs, HUD, menus and loading coverage");
    }

    private static void bladeHudClearsSageAndDebugLabel() throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();game.setDebugGodMode(true);
        game.player().collectIchor(100);assert game.transform();
        var panel=new B2BJ(false,game);
        var draw=B2BJ.class.getDeclaredMethod("drawPlayerHud",java.awt.Graphics2D.class);draw.setAccessible(true);
        for(boolean boss:new boolean[]{false,true})for(int width:new int[]{1280,1920,2560}) {
            if(boss)game.guardian().activate(game.player().x(),game.player().y());
            panel.setSize(width,720);panel.doLayout();
            var hud=new BufferedImage(width,720,BufferedImage.TYPE_INT_ARGB);var g=hud.createGraphics();draw.invoke(panel,g);g.dispose();
            var overlays=new BufferedImage(width,720,BufferedImage.TYPE_INT_ARGB);g=overlays.createGraphics();
            CampaignRenderer.drawHud(g,game,width,720,false);CampaignRenderer.drawDebugStatus(g,game,width,720);g.dispose();
            assert (hud.getRGB(35,116)>>>24)>0 : "Blade duration remains below the character crest";
            for(int y=0;y<720;y++)for(int x=0;x<width;x++)
                assert (hud.getRGB(x,y)>>>24)==0||(overlays.getRGB(x,y)>>>24)==0
                        : "Mission/map/Raphael/debug overlays must not cover Blade resources or skills at "+width+" / boss="+boss;
        }
    }

    private static void titleScaling() throws Exception {
        var game=RuinedOutpostGame.campaign();var panel=new B2BJ(false,game);
        long basePixels=0;
        for(var size:new int[][]{{1280,720},{1920,1080},{2560,1080}}) {
            int w=size[0],h=size[1];double scale=TitleScreen.menuScale(w,h);
            panel.setSize(w,h);panel.doLayout();
            var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();
            var title=(TitleMenu)field(panel,"titleMenu");title.animate(3,true);
            assert title.getX()==Math.round(112*scale)&&title.getY()==Math.round(338*scale);
            assert title.getWidth()==Math.round(448*scale)&&title.getY()+title.getHeight()<h;
            for(var action:title.getComponents())assert action.getY()+action.getHeight()<=title.getHeight()
                    : "every menu hit target must fit the panel after scaling";
            var settings=(javax.swing.JButton)title.getComponent(2);
            int x=title.getX()+settings.getX()+settings.getWidth()/2,y=title.getY()+settings.getY()+settings.getHeight()/2;
            assert SwingUtilities.getDeepestComponentAt(panel,x,y)==settings : "scaled text must retain matching physical hit targets";
            var button=new BufferedImage(settings.getWidth(),settings.getHeight(),BufferedImage.TYPE_INT_ARGB);g=button.createGraphics();settings.paint(g);g.dispose();
            long pixels=java.util.Arrays.stream(button.getRGB(0,0,button.getWidth(),button.getHeight(),null,0,button.getWidth())).filter(p->p>>>24!=0).count();
            if(basePixels==0)basePixels=pixels;
            else assert pixels>basePixels*1.8 : "fullscreen menu type and icons must enlarge along with the buttons";
            settings.doClick(0);assert ((javax.swing.JPanel)field(panel,"menu")).isVisible();
            var close=B2BJ.class.getDeclaredMethod("closeMenu");close.setAccessible(true);close.invoke(panel);
        }
    }
}
