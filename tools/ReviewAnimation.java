import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import javax.imageio.ImageIO;

/** Contact sheet and frame metrics only; source pixels are left untouched. */
public final class ReviewAnimation {
    public static void main(String[] args) throws Exception {
        Path directory=Path.of(args[0]);
        java.util.List<Path> paths;
        try(var files=Files.list(directory)) {
            paths=files.filter(p->p.getFileName().toString().matches("frame-[0-9]+\\.png"))
                    .sorted(java.util.Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().replace("frame-","").replace(".png","")))).toList();
        }
        if(paths.isEmpty())throw new IllegalArgumentException("No frames: "+directory);
        var first=ImageIO.read(paths.get(0).toFile());
        int w=first.getWidth(),h=first.getHeight(),scale=3;
        var sheet=new BufferedImage(w*scale*paths.size(),h*scale+20,BufferedImage.TYPE_INT_ARGB);
        var g=sheet.createGraphics();g.setColor(new Color(25,30,42));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        var hashes=new HashSet<Integer>();int failures=0;
        for(int i=0;i<paths.size();i++) {
            var frame=ImageIO.read(paths.get(i).toFile());
            if(frame.getWidth()!=w||frame.getHeight()!=h)throw new IllegalArgumentException("Frame dimensions changed");
            int count=0,left=w,right=-1,top=h,bottom=-1;var colors=new HashSet<Integer>();
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int pixel=frame.getRGB(x,y);if((pixel>>>24)==0)continue;
                count++;colors.add(pixel&0xffffff);left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
            boolean clean=count<w*h*0.85;
            if(!clean)failures++;
            hashes.add(Arrays.hashCode(frame.getRGB(0,0,w,h,null,0,w)));
            g.drawImage(frame,i*w*scale,20,w*scale,h*scale,null);g.setColor(clean?Color.WHITE:Color.RED);g.drawString(""+i,i*w*scale+4,14);
            System.out.printf("%s frame=%d pixels=%d colors=%d bounds=%d,%d..%d,%d alpha=%s%n",directory.getFileName(),i,count,colors.size(),left,top,right,bottom,count==0?"empty":clean?"pass":"FAIL");
        }
        g.dispose();ImageIO.write(sheet,"png",directory.resolve("contact.png").toFile());
        System.out.printf("frames=%d unique=%d alphaFailures=%d%n",paths.size(),hashes.size(),failures);
    }
}
