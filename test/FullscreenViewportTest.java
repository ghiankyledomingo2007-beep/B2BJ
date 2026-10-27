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
                int[] originalSkills=null;
                for(var size:new int[][]{{1280,720},{1920,1080},{1600,900},{2560,1080},{1280,720}}) {
                    int w=size[0],h=size[1];panel.setSize(w,h);panel.doLayout();
                    assert Math.abs(game.player().x()-panel.worldCameraX()-w/2.)<=1;
                    assert Math.abs(game.player().y()-panel.worldCameraY()-h/2.)<=1;
                    var tells=(Telegraphs)field(panel,"tells");
                    assert tells.image().getWidth()==(w+1)/2&&tells.image().getHeight()==(h+1)/2;
                    panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,w/2+150,h/2,0,false));
                    var aim=B2BJ.class.getDeclaredMethod("aimDirection");aim.setAccessible(true);int[] direction=(int[])aim.invoke(panel);
                    assert direction[0]>0&&direction[1]==0 : "fullscreen mouse aim must use the displayed camera";
                    var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);
                    var button=(javax.swing.JButton)field(panel,"menuButton");
                    assert button.getX()==w-140&&button.getX()+button.getWidth()<=w;
                    button.doClick();panel.doLayout();
                    var menu=(javax.swing.JPanel)field(panel,"menu");
                    assert menu.isVisible()&&Math.abs(menu.getX()+menu.getWidth()/2-w/2)<=1;
                    assert Math.abs(menu.getY()+menu.getHeight()/2-h/2)<=1;
                    button.doClick();
                    var hud=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var hg=hud.createGraphics();
                    var drawHud=B2BJ.class.getDeclaredMethod("drawPlayerHud",java.awt.Graphics2D.class);drawHud.setAccessible(true);drawHud.invoke(panel,hg);hg.dispose();
                    int[] skills=hud.getRGB(478+(w-1280)/2,h-100,332,80,null,0,332);
                    if(originalSkills==null)originalSkills=skills;
                    else assert java.util.Arrays.equals(originalSkills,skills) : "ability bar must stay at bottom centre at native pixel size";
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
            var hud=new BufferedImage(width,300,BufferedImage.TYPE_INT_ARGB);var g=hud.createGraphics();draw.invoke(panel,g);g.dispose();
            var overlays=new BufferedImage(width,300,BufferedImage.TYPE_INT_ARGB);g=overlays.createGraphics();
            CampaignRenderer.drawSage(g,game,width,720,false);CampaignRenderer.drawDebugStatus(g,game);g.dispose();
            assert (hud.getRGB(44,200)>>>24)>0 : "Blade meter belongs below HP and Ichor";
            for(int y=0;y<300;y++)for(int x=0;x<width;x++)
                assert (hud.getRGB(x,y)>>>24)==0||(overlays.getRGB(x,y)>>>24)==0
                        : "Raphael and the debug label must not cover Blade resources at "+width+" / boss="+boss;
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
            assert title.getX()==Math.round(68*scale)&&title.getY()==Math.round(386*scale);
            assert title.getWidth()==Math.round(420*scale)&&title.getY()+title.getHeight()<h;
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
