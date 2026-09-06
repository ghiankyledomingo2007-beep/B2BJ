import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import javax.imageio.ImageIO;

public class InspectProp {
    public static void main(String[] args) throws Exception {
        BufferedImage image=ImageIO.read(Path.of(args[0]).toFile());
        int left=image.getWidth(),top=image.getHeight(),right=-1,bottom=-1,count=0;
        var colors=new HashSet<Integer>();
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int pixel=image.getRGB(x,y);
            if((pixel>>>24)==0)continue;
            left=Math.min(left,x);right=Math.max(right,x);
            top=Math.min(top,y);bottom=Math.max(bottom,y);
            count++;colors.add(pixel&0xffffff);
        }
        System.out.printf("%dx%d opaque=%d colors=%d bounds=[%d,%d..%d,%d]%n",
                image.getWidth(),image.getHeight(),count,colors.size(),left,top,right,bottom);
        if(count==0||count==image.getWidth()*image.getHeight())throw new IllegalArgumentException("Invalid transparency");
        if(args.length>1) {
            BufferedImage preview=new BufferedImage(image.getWidth()*4,image.getHeight()*4,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=preview.createGraphics();
            g.setColor(new java.awt.Color(25,30,42));g.fillRect(0,0,preview.getWidth(),preview.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(image,0,0,preview.getWidth(),preview.getHeight(),null);g.dispose();
            ImageIO.write(preview,"png",Path.of(args[1]).toFile());
        }
    }
}
