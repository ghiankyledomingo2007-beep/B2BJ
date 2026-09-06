import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

/** Packs reviewed PixelLab frames without resampling or changing their pixels. */
public final class PackEffect {
    public static void main(String[] args) throws Exception {
        if(args.length!=2) throw new IllegalArgumentException("source-directory output-png");
        // Last two generated frames have opaque backgrounds; only these three passed review.
        BufferedImage strip=new BufferedImage(96,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas=strip.createGraphics();
        Set<Integer> hashes=new HashSet<>();
        for(int i=0;i<3;i++) {
            BufferedImage frame=ImageIO.read(Path.of(args[0],"frame-"+i+".png").toFile());
            if(frame==null||frame.getWidth()!=32||frame.getHeight()!=32)
                throw new IllegalArgumentException("Expected 32x32 frame "+i);
            int opaque=0,transparent=0;
            Set<Integer> colors=new HashSet<>();
            for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
                int pixel=frame.getRGB(x,y);
                if((pixel>>>24)==0) transparent++;
                else { opaque++;colors.add(pixel&0xffffff); }
            }
            if(transparent<512) throw new IllegalArgumentException("Opaque background in frame "+i);
            hashes.add(java.util.Arrays.hashCode(frame.getRGB(0,0,32,32,null,0,32)));
            System.out.println("frame "+i+": visible="+opaque+", colors="+colors.size());
            canvas.drawImage(frame,i*32,0,null);
        }
        canvas.dispose();
        if(hashes.size()<3) throw new IllegalArgumentException("Repetitive effect frames");
        ImageIO.write(strip,"png",Path.of(args[1]).toFile());
    }
}
