import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;

/** Native-size imports; generated masters stay outside the game's packaged asset folders. */
final class ImportedArt {
    private ImportedArt() { }

    static void portraits() {
        int[] mara={0x171a18,0x30251c,0x493525,0x70513a,0x9d7049,0xc48e5c,0xe3b583,
                0xeee0ba,0xc7bd97,0x9a9270,0x6c684a,0x3c4328,0x596034,0x7a804d,0x999d68};
        var sheet=Canvas.load("assets/source/characters/mara_sheet.generated.png").image();
        fit(sheet.getSubimage(0,0,sheet.getHeight(),sheet.getHeight()),64,64,mara)
                .save("assets/characters/npcs/mara_portrait.png");
        fit(Canvas.load("assets/source/characters/mara_idle.generated.png").image(),80,80,mara)
                .save("assets/characters/npcs/mara_idle.png");
        var idle=Canvas.load("assets/characters/slime/slime_idle.png");
        var colours=new LinkedHashSet<Integer>();
        for(int pixel:idle.px)if(pixel>>>24!=0)colours.add(pixel&0xffffff);
        fit(Canvas.load("assets/source/characters/rainoray_portrait.generated.png").image(),64,64,
                colours.stream().mapToInt(Integer::intValue).toArray())
                .save("assets/characters/slime/rainoray_portrait.png");
    }

    static Canvas tent() {
        int[] colours={0x17161a,0x292421,0x42382d,0x62533d,0x806e52,0xa58d68,0xc2aa80,0xddc293,
                0xefdab0,0x553133,0x803c3e,0xa34e50,0xc96d6a,0x6d472d,0x9c6c3e,0xc49355};
        return fit(Canvas.load("assets/source/outpost/tent.generated.png").image(),128,100,colours);
    }

    private static Canvas fit(BufferedImage source,int targetWidth,int targetHeight,int[] palette) {
        double scale=Math.min((double)targetWidth/source.getWidth(),(double)targetHeight/source.getHeight());
        int width=(int)Math.round(source.getWidth()*scale),height=(int)Math.round(source.getHeight()*scale);
        var image=new BufferedImage(targetWidth,targetHeight,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();Ui.crisp(g);
        g.drawImage(source,(targetWidth-width)/2,(targetHeight-height)/2,width,height,null);g.dispose();
        var result=new Canvas(targetWidth,targetHeight);
        for(int y=0;y<targetHeight;y++)for(int x=0;x<targetWidth;x++) {
            int pixel=image.getRGB(x,y);
            if(pixel>>>24<128)continue;
            int best=palette[0];double distance=Double.MAX_VALUE;
            for(int colour:palette) {
                int r=(pixel>>16&255)-(colour>>16&255),green=(pixel>>8&255)-(colour>>8&255),b=(pixel&255)-(colour&255);
                double d=.3*r*r+.59*green*green+.11*b*b;
                if(d<distance){distance=d;best=colour;}
            }
            result.set(x,y,Pal.c(best));
        }
        return result;
    }
}
