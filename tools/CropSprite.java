import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;

/** Extracts an unchanged native frame for PixelLab; never resizes or repaints it. */
public final class CropSprite {
    public static void main(String[] args) throws Exception {
        var source=ImageIO.read(Path.of(args[0]).toFile());
        var frame=source.getSubimage(Integer.parseInt(args[1]),Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),Integer.parseInt(args[4]));
        if(args.length>6) {
            int padding=Integer.parseInt(args[6]);
            if(padding<0||padding>32)throw new IllegalArgumentException("padding 0..32");
            var padded=new java.awt.image.BufferedImage(frame.getWidth()+padding*2,frame.getHeight()+padding*2,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var g=padded.createGraphics();g.drawImage(frame,padding,padding,null);g.dispose();frame=padded;
        }
        if(args.length>5)ImageIO.write(frame,"png",Path.of(args[5]).toFile());
        var bytes=new ByteArrayOutputStream();ImageIO.write(frame,"png",bytes);
        System.out.print(Base64.getEncoder().encodeToString(bytes.toByteArray()));
    }
}
