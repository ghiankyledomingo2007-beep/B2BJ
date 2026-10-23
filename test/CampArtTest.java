import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;

public final class CampArtTest {
    public static void main(String[] args) throws Exception {
        flame("fire_camp",30,40);flame("fire_brazier",22,30);
        flame("warden_flame",14,24);flame("briar_flame",14,24);
        for(String[] asset:new String[][]{{"characters/npcs/mara_idle", "80", "80", "15"},
                {"characters/npcs/mara_portrait", "64", "64", "15"},
                {"characters/slime/rainoray_portrait", "64", "64", "12"},
                {"props/outpost/set_tent", "128", "100", "16"},
                {"props/outpost/set_dummy", "72", "72", "16"}}) {
            var sprite=B2BJ.loadImage("assets/"+asset[0]+".png");
            assert sprite!=null&&sprite.getWidth()==Integer.parseInt(asset[1])&&sprite.getHeight()==Integer.parseInt(asset[2]);
            var palette=new HashSet<Integer>();
            for(int y=0;y<sprite.getHeight();y++)for(int x=0;x<sprite.getWidth();x++) {
                int pixel=sprite.getRGB(x,y),alpha=pixel>>>24;
                assert alpha==0||alpha==255 : "soft sprite edge: "+asset[0];
                if(alpha!=0)palette.add(pixel);
            }
            assert palette.size()>4&&palette.size()<=Integer.parseInt(asset[3]) : "native pixel palette: "+asset[0];
        }
        var field=DialogueRenderer.class.getDeclaredField("MARA_BODY");field.setAccessible(true);
        var mara=(BufferedImage)field.get(null);
        var hero=B2BJ.loadImage("assets/characters/blade/rainoray_idle.png").getSubimage(0,0,80,80);
        assert mara.getWidth()==80&&mara.getHeight()==80 : "humans share the 80-pixel frame at 2x";
        int humanHeight=visibleHeight(hero),maraHeight=visibleHeight(mara);
        assert Math.abs(humanHeight-maraHeight)<=humanHeight*.15 : "Mara must fit the player's human scale";
        System.out.println("CampArtTest passed: flat animated flames; human heights "+humanHeight+" / "+maraHeight);
    }

    private static void flame(String name,int width,int height) {
        var sheet=B2BJ.loadImage("assets/effects/"+name+".png");
        assert sheet!=null&&sheet.getWidth()==width*8&&sheet.getHeight()==height;
        var colours=new HashSet<Integer>();var frames=new HashSet<Integer>();
        for(int frame=0;frame<8;frame++) {
            int[] pixels=sheet.getRGB(frame*width,0,width,height,null,0,width);int opaque=0;
            for(int pixel:pixels) {
                int alpha=pixel>>>24;
                assert alpha==0||alpha==255 : name+" needs hard pixel edges";
                if(alpha!=0){colours.add(pixel);opaque++;}
            }
            assert opaque>width*height/8&&opaque<width*height*3/4 : "flame and transparent margin";
            frames.add(Arrays.hashCode(pixels));
        }
        assert colours.size()<=6&&frames.size()>=6 : "flat palette, distinct flame silhouettes: "+name;
    }

    private static int visibleHeight(BufferedImage image) {
        int top=image.getHeight(),bottom=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
            if(image.getRGB(x,y)>>>24>=128) {top=Math.min(top,y);bottom=Math.max(bottom,y);}
        return bottom-top+1;
    }
}
