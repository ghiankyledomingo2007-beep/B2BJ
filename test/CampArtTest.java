import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;

public final class CampArtTest {
    public static void main(String[] args) throws Exception {
        var fire=B2BJ.loadImage("assets/effects/fire_camp.png");
        assert fire!=null&&fire.getWidth()==240&&fire.getHeight()==40;
        var colours=new HashSet<Integer>();var frames=new HashSet<Integer>();
        for(int frame=0;frame<8;frame++) {
            int[] pixels=fire.getRGB(frame*30,0,30,40,null,0,30);int opaque=0;
            for(int pixel:pixels) {
                int alpha=pixel>>>24;
                assert alpha==0||alpha==255 : "camp flames need hard pixel edges";
                if(alpha!=0) {colours.add(pixel);opaque++;}
            }
            assert opaque>150&&opaque<650 : "each frame needs a complete flame and transparent margin";
            frames.add(Arrays.hashCode(pixels));
        }
        assert colours.size()<=6&&frames.size()>=6 : "flat palette, real animated silhouettes";
        var field=DialogueRenderer.class.getDeclaredField("MARA_BODY");field.setAccessible(true);
        var mara=(BufferedImage)field.get(null);
        var hero=B2BJ.loadImage("assets/characters/blade/rainoray_idle.png").getSubimage(0,0,80,80);
        assert mara.getWidth()==80&&mara.getHeight()==80 : "humans share the 80-pixel frame at 2x";
        int humanHeight=visibleHeight(hero),maraHeight=visibleHeight(mara);
        assert Math.abs(humanHeight-maraHeight)<=humanHeight*.15 : "Mara must fit the player's human scale";
        System.out.println("CampArtTest passed: flat animated flames; human heights "+humanHeight+" / "+maraHeight);
    }

    private static int visibleHeight(BufferedImage image) {
        int top=image.getHeight(),bottom=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
            if(image.getRGB(x,y)>>>24>=128) {top=Math.min(top,y);bottom=Math.max(bottom,y);}
        return bottom-top+1;
    }
}
