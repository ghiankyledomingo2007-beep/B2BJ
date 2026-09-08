import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class CampaignHudRestorationTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int biome = 0; biome < 4; biome++) {
                var game = RuinedOutpostGame.campaign(); game.begin();
                assert game.debugWarp(biome, false);
                var panel = new B2BJ(false, game); panel.setSize(1280, 720); panel.doLayout();
                var ready = paint(panel);
                assertIcon(ready, "vitality", 32, 42, 2);
                assertIcon(ready, "slash", 486, 623, 2);
                assert game.tideWave(1, 0);
                assert changed(ready, paint(panel), 572, 630, 50, 45) : "campaign RMB slot must show Tide cooldown";
                game.debugRefill(); game.player().configureUpgrades(3, 0, 3, 0); game.player().heal();
                var upgraded = paint(panel);
                assertIcon(upgraded, "vitality", 32 + 7 * 34, 54, 1);
                assert game.player().hurt(1);
                assert changed(upgraded, paint(panel), 32 + 7 * 34, 54, 32, 32) : "eighth health icon must track damage";
                game.debugRefill(); assert game.player().transform();
                var blade = paint(panel);
                assertIcon(blade, "riposte", 780, 623, 2);
                assert game.riposte();
                assert changed(blade, paint(panel), 782, 630, 50, 45) : "human Riposte slot must show cooldown";
                game.debugRefill(); assert game.ichorCrescent(1, 0);
                assert changed(blade, paint(panel), 530, 630, 50, 45) : "Crescent cooldown must be visible";
                game.debugRefill(); assert game.player().dash(1, 0);
                assert changed(blade, paint(panel), 614, 630, 50, 45) : "dash cooldown must be visible";
                game.debugRefill();
                assert game.player().spendBladeIchor(game.player().ichor());
                var recovering = paint(panel);
                assert changed(ready, recovering, 486, 630, 50, 45) : "reforming must dim unavailable skills";
                if (args.length > 0) {
                    try {
                        Path dir = Path.of(args[0]); Files.createDirectories(dir);
                        ImageIO.write(ready, "png", dir.resolve("hud-" + biome + ".png").toFile());
                        ImageIO.write(upgraded, "png", dir.resolve("health-eight-" + biome + ".png").toFile());
                        ImageIO.write(blade, "png", dir.resolve("blade-" + biome + ".png").toFile());
                    } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
                }
            }
        });
        System.out.println("CampaignHudRestorationTest passed");
    }

    private static void assertIcon(BufferedImage frame, String name, int x, int y, int scale) {
        var icon = B2BJ.loadImage("assets/ui/" + name + ".png");
        int checked = 0;
        // Left edge avoids cooldown labels and adjacent slots; compare actual approved asset pixels.
        for (int sy = 0; sy < icon.getHeight(); sy++) for (int sx = 0; sx < 5; sx++) {
            if ((icon.getRGB(sx, sy) >>> 24) != 255) continue;
            assert frame.getRGB(x + sx * scale, y + sy * scale) == icon.getRGB(sx, sy)
                    : "campaign must render existing " + name + " icon";
            checked++;
        }
        assert checked > 0 : "fixture must sample visible icon pixels";
    }
    private static BufferedImage paint(B2BJ panel) {
        var result = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var g = result.createGraphics(); panel.paint(g); g.dispose(); return result;
    }
    private static boolean changed(BufferedImage a, BufferedImage b, int x, int y, int w, int h) {
        return !Arrays.equals(a.getRGB(x, y, w, h, null, 0, w), b.getRGB(x, y, w, h, null, 0, w));
    }
}
