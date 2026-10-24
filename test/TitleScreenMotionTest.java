import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Title-only motion must preserve the stance and architecture, loop, and respect reduced effects. */
public final class TitleScreenMotionTest {
    public static void main(String[] args) {
        var art=B2BJ.loadImage("assets/ui/title/ruined-gate.png");
        var wind=new TitleWind(art,B2BJ.loadImage("assets/ui/title/ruined-gate-wind-clean.png"));
        assert wind.available() : "the animated title needs its clean background plate";
        for(double seconds:new double[]{0,1.6,3.2,4.8,6.4}) {
            var frame=windFrame(art,wind,seconds,0);
            int changed=0;
            for(int y=0;y<360;y++)for(int x=0;x<640;x++)if(frame.getRGB(x,y)!=art.getRGB(x,y)) {
                changed++;
                assert x>=450&&x<490&&y>=236&&y<295 : "wind moved the head, stance or background at "+x+","+y;
            }
            if(seconds==0||seconds==6.4)assert changed==0 : "the loop must return to the original pose without a seam";
            else assert changed>100 : "hair and coat must visibly move";
        }
        var ready=windFrame(art,wind,3.2,1);
        for(int y=296;y<318;y++)for(int x=465;x<489;x++)
            assert ready.getRGB(x,y)==art.getRGB(x,y) : "readying the sword must not slide the feet";

        var title=new TitleScreen();title.select(0);
        int[] still=titleFrame(title,1,true);
        assert Arrays.equals(still,titleFrame(title,8,true)) : "reduced effects must freeze all ambient movement";
        assert !Arrays.equals(titleFrame(title,1,false),titleFrame(title,3,false));
        title.select(3);
        assert !Arrays.equals(still,titleFrame(title,8,true)) : "selection can change lighting without requiring animation";

        var intro=new TitleScreen();
        int[] entering=titleFrame(intro,.12,false),settled=titleFrame(intro,3,false);
        int letters=0;
        for(int y=70;y<350;y++)for(int x=70;x<470;x++)if(entering[y*1280+x]!=settled[y*1280+x])letters++;
        assert letters>20_000 : "the wordmark needs a visible staged entrance";
        var reducedIntro=new TitleScreen();
        assert Arrays.equals(titleFrame(reducedIntro,0,true),titleFrame(reducedIntro,20,true))
                : "reduced effects shows the complete title immediately, without an idle shimmer";

        System.out.println("TitleScreenMotionTest passed: anchored wind, loop, ready pose, reduced effects and lighting");
    }

    private static BufferedImage windFrame(BufferedImage art,TitleWind wind,double time,double ready) {
        var frame=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
        var g=frame.createGraphics();g.drawImage(art,0,0,null);wind.draw(g,time,ready);g.dispose();return frame;
    }

    private static int[] titleFrame(TitleScreen title,double time,boolean reduced) {
        var frame=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
        var g=frame.createGraphics();title.draw(g,1280,720,time,null,reduced);g.dispose();
        return frame.getRGB(0,0,1280,720,null,0,1280);
    }
}
