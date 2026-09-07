import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Headless weather scene samples, not a live frame-pacing playtest. */
public final class PreviewRain {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());panel.game().begin();
        Path out=Path.of("docs/testing/rain-art");Files.createDirectories(out);
        for(int frame=0;frame<4;frame++) {
            for(int tick=0;tick<15;tick++)panel.step(1.0/60);
            save(panel,out.resolve("rain-"+frame+".png"));
        }
        var reduced=B2BJ.class.getDeclaredField("reducedEffects");reduced.setAccessible(true);reduced.set(panel,true);
        save(panel,out.resolve("rain-reduced.png"));
        System.out.println("PreviewRain: four elapsed-time scene samples and reduced-effects sample saved");
    }
    private static void save(B2BJ panel,Path path) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var ink=image.createGraphics();panel.paint(ink);ink.dispose();ImageIO.write(image,"png",path.toFile());
    }
}
