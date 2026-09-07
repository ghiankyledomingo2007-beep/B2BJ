import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Actual renderer at native 2x; inspection fixtures, not a human playthrough. */
public final class ReviewSlimeCombo {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);
        var draw=B2BJ.class.getDeclaredMethod("drawWater",Graphics2D.class,int.class,int.class,WaterProjectile.class);
        draw.setAccessible(true);
        Path out=Path.of(args[0]);Files.createDirectories(out);
        int[][] directions={{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};
        for(double age:new double[]{.08,.20,.36}) {
            var sheet=new BufferedImage(1152,820,BufferedImage.TYPE_INT_RGB);
            var g=sheet.createGraphics();g.setColor(new Color(22,29,39));g.fillRect(0,0,1152,820);
            for(var kind:WaterProjectile.Kind.values())for(int d=0;d<directions.length;d++) {
                var wave=new WaterProjectile(300,300,directions[d][0],directions[d][1],kind);
                wave.advance(age);
                assert wave.alive();
                int cx=d*144+72,cy=kind.ordinal()*164+90;
                draw.invoke(panel,g,(int)Math.round(wave.x())-cx,(int)Math.round(wave.y())-cy,wave);
                g.setColor(new Color(160,175,190));g.drawString(kind+" / "+d,d*144+5,kind.ordinal()*164+18);
            }
            g.dispose();ImageIO.write(sheet,"png",out.resolve("cuts-"+Math.round(age*100)+".png").toFile());
        }
    }
}
