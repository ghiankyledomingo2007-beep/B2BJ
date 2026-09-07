import java.awt.Color;
import java.awt.image.BufferedImage;

public final class RainSpriteTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);panel.game().begin();panel.step(.4);
        var streak=B2BJ.class.getDeclaredField("rainStreakSheet");streak.setAccessible(true);
        var splash=B2BJ.class.getDeclaredField("rainSplashSheet");splash.setAccessible(true);
        streak.set(panel,marker(64,32));splash.set(panel,marker(96,32));
        assert magenta(RainRenderingTest.draw(panel,0,0,false))>0 : "falling rain uses generated streak atlas";
        assert magenta(RainRenderingTest.draw(panel,0,0,true))>0 : "ground rain uses generated splash atlas";
        streak.set(panel,null);splash.set(panel,null);
        RainRenderingTest.draw(panel,0,0,false);RainRenderingTest.draw(panel,0,0,true);
        var art=B2BJ.loadImage("assets/effects/rain_streaks.png");
        assert art!=null&&art.getWidth()==64&&art.getHeight()==32 : "two native32px streak canvases";
        CombatArtTest.check("assets/effects/rain_streaks.png",32,32,2,1,2);
        CombatArtTest.check("assets/effects/rain_splashes.png",16,16,6,2,6,true);
        System.out.println("RainSpriteTest passed");
    }
    private static BufferedImage marker(int w,int h) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();
        ink.setColor(Color.MAGENTA);ink.fillRect(0,0,w,h);ink.dispose();return image;
    }
    private static int magenta(BufferedImage image) {
        int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int p=image.getRGB(x,y);if((p>>>24)>0&&(p>>16&255)>180&&(p>>8&255)<30&&(p&255)>180)count++;
        }return count;
    }
}
