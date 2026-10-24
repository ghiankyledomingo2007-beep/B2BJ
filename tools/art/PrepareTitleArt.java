import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Imports the generated clean plate onto the game's pixel grid; keeps the source master intact. */
public final class PrepareTitleArt {
    public static void main(String[] args) throws Exception {
        var root=Path.of("assets/source/ui/title");
        var plate=ImageIO.read(root.resolve("ruined-gate-wind-clean-master.png").toFile());
        var small=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
        var g=small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(plate,0,0,640,360,null);g.dispose();
        ImageIO.write(small,"png",Path.of("assets/ui/title/ruined-gate-wind-clean.png").toFile());
        System.out.println("Prepared 640x360 title clean plate.");
    }
}
