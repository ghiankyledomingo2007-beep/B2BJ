import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

public final class RainRenderingTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var smooth=new B2BJ(false);var slow=new B2BJ(false);
        for(int i=0;i<60;i++)smooth.step(1.0/60);
        for(int i=0;i<30;i++)slow.step(1.0/30);
        var normal=draw(smooth,0,0,false);
        assert Arrays.equals(pixels(normal),pixels(draw(slow,0,0,false)))
                : "rain must move by elapsed time, not frame count";
        smooth.game().pause();smooth.step(.4);
        assert Arrays.equals(pixels(normal),pixels(draw(smooth,0,0,false))) : "pause freezes weather";
        var reduced=B2BJ.class.getDeclaredField("reducedEffects");reduced.setAccessible(true);reduced.set(slow,true);
        assert count(draw(slow,0,0,false))<count(normal)/2 : "reduced effects reduces rain clutter";
        assert count(normal)>100 : "visible rain";
        var colors=new HashSet<Integer>();
        for(int y=0;y<720;y+=2)for(int x=0;x<1280;x+=2) {
            int p=normal.getRGB(x,y);if((p>>>24)>0)colors.add(p);
            assert p==normal.getRGB(x+1,y)&&p==normal.getRGB(x,y+1)&&p==normal.getRGB(x+1,y+1)
                    : "rain uses native 2x pixels";
        }
        assert colors.size()>=3 : "rain has depth and fading tails, not identical bars";
        var shifted=draw(smooth,20,12,false);
        for(int y=0;y<700;y++)for(int x=0;x<1240;x++)
            assert normal.getRGB(x+20,y+12)==shifted.getRGB(x,y) : "rain is world anchored";
        var splashes=draw(smooth,0,0,true);
        assert count(splashes)>0 : "drops have brief ground contact";
        assert count(draw(slow,0,0,true))==0 : "reduced effects omits ground splashes";
        System.out.println("RainRenderingTest passed");
    }
    static BufferedImage draw(B2BJ panel,int cameraX,int cameraY,boolean ground) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        Method method=Arrays.stream(B2BJ.class.getDeclaredMethods()).filter(m->m.getName().equals("drawWeather")).findFirst().orElseThrow();
        method.setAccessible(true);Graphics2D ink=image.createGraphics();
        if(method.getParameterCount()==1)method.invoke(panel,ink);
        else method.invoke(panel,ink,cameraX,cameraY,ground);
        ink.dispose();return image;
    }
    private static int[] pixels(BufferedImage image) {return image.getRGB(0,0,1280,720,null,0,1280);}
    private static int count(BufferedImage image) {int n=0;for(int p:pixels(image))if((p>>>24)>0)n++;return n;}
}
