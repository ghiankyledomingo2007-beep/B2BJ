import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Joins reviewed native frames; optional floor correction is limited to three pixels. */
public final class PackFrames {
    public static void main(String[] args) throws Exception {
        if(args.length<6)throw new IllegalArgumentException("width height columns floor(-1=unchanged) output frame...");
        int w=Integer.parseInt(args[0]),h=Integer.parseInt(args[1]),columns=Integer.parseInt(args[2]);
        int floor=Integer.parseInt(args[3]),count=args.length-5;
        int cropBottom=Integer.getInteger("b2bj.cropBottom",h);
        int maxShift=Integer.getInteger("b2bj.maxFloorShift",3);
        boolean allowEmptyTail=Boolean.getBoolean("b2bj.allowEmptyTail");
        if(w<=0||h<=0||columns<=0||floor>=h||floor< -1)throw new IllegalArgumentException("Invalid grid");
        if(cropBottom<=0||cropBottom>h)throw new IllegalArgumentException("Invalid crop bottom");
        if(maxShift<0||maxShift>16)throw new IllegalArgumentException("Floor shift must be 0..16");
        var sheet=new BufferedImage(w*columns,h*((count+columns-1)/columns),BufferedImage.TYPE_INT_ARGB);
        var g=sheet.createGraphics();
        for(int i=0;i<count;i++) {
            var frame=ImageIO.read(Path.of(args[i+5]).toFile());
            if(frame==null||frame.getWidth()!=w||frame.getHeight()!=h)throw new IllegalArgumentException("Invalid frame: "+args[i+5]);
            int bottom=-1,opaque=0;
            for(int y=0;y<cropBottom;y++)for(int x=0;x<w;x++)if((frame.getRGB(x,y)>>>24)>0){bottom=y;opaque++;}
            if(opaque==0&&allowEmptyTail&&columns>1&&i%columns==columns-1)continue;
            if(opaque==0||opaque>w*h*0.85)throw new IllegalArgumentException("Invalid transparency: "+args[i+5]);
            int shift=floor<0?0:floor-bottom;
            if(shift<0||shift>maxShift)throw new IllegalArgumentException("Excessive floor drift: "+args[i+5]);
            g.drawImage(frame.getSubimage(0,0,w,cropBottom),(i%columns)*w,(i/columns)*h+shift,null);
        }
        g.dispose();ImageIO.write(sheet,"png",Path.of(args[4]).toFile());
        System.out.printf("Packed %d reviewed frames: %dx%d%n",count,sheet.getWidth(),sheet.getHeight());
    }
}
