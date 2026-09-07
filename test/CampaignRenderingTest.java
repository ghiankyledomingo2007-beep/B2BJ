import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;

public final class CampaignRenderingTest {
    public static void main(String[] args) throws Exception {
        var silhouettes = new HashSet<Integer>();
        for (var kind : CampaignEnemy.Kind.values()) {
            var enemy = new CampaignEnemy(kind, 320, 260);
            var image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            var transform = g.getTransform();
            CampaignRenderer.drawEnemy(g, enemy, 0, 0);
            assert transform.equals(g.getTransform()) : "enemy rendering must not move subsequent actors";
            g.dispose();
            assert opaquePixels(image) > 300 : "every enemy needs a readable placeholder silhouette: " + kind;
            assert silhouettes.add(Arrays.hashCode(image.getRGB(0, 0, 640, 480, null, 0, 640)))
                    : "enemy kinds must be distinguishable: " + kind;
            assert (image.getRGB(0, 0) >>> 24) == 0 : "actor drawing must preserve surrounding terrain";
        }
        var bosses = new HashSet<Integer>();
        for (int biome = 0; biome < 4; biome++) {
            var image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
            var g = image.createGraphics();
            CampaignRenderer.drawBoss(g, new Guardian(320, 260), biome, 0, 0);
            g.dispose();
            assert opaquePixels(image) > 1_000 : "boss needs a larger silhouette than ordinary enemies";
            assert bosses.add(Arrays.hashCode(image.getRGB(0, 0, 640, 480, null, 0, 640)))
                    : "region bosses must have distinct silhouettes";
        }
        var game = RuinedOutpostGame.campaign();
        var title = snapshot(game, false);
        assert (title.getRGB(640, 200) >>> 24) == 255 : "campaign title must render over an opaque world";
        game.begin();
        var enter = RuinedOutpostGame.class.getDeclaredMethod("enterBiome", int.class, int.class);
        enter.setAccessible(true);
        var grounds = new HashSet<Integer>();
        for (int biome = 0; biome < 4; biome++) {
            enter.invoke(game, biome, 1);
            var player = game.player();
            double px = player.x(), py = player.y();
            var frame = snapshot(game, false);
            var map = snapshot(game, true);
            assert game.player() == player && px == player.x() && py == player.y()
                    : "rendering must not change game state";
            assert grounds.add(frame.getRGB(420, 330)) : "biome ground palettes must remain distinct";
            assert opaquePixels(frame) == 1280 * 720 : "campaign world must fill the viewport";
            assert !Arrays.equals(frame.getRGB(0, 0, 1280, 720, null, 0, 1280),
                    map.getRGB(0, 0, 1280, 720, null, 0, 1280)) : "expanded map needs its own readable view";
            if (args.length > 0) {
                var directory = java.nio.file.Path.of(args[0]);
                java.nio.file.Files.createDirectories(directory);
                javax.imageio.ImageIO.write(frame, "png", directory.resolve("biome-" + biome + ".png").toFile());
                javax.imageio.ImageIO.write(map, "png", directory.resolve("map-" + biome + ".png").toFile());
                javax.imageio.ImageIO.write(title, "png", directory.resolve("title.png").toFile());
            }
        }
        game.campaignStory().talk(3);
        assert !game.campaignStory().dialogueTitle().isEmpty();
        var dialogue = snapshot(game, false);
        assert game.campaignStory().dialogueOpen() : "drawing dialogue must not dismiss it";
        if (args.length > 0) javax.imageio.ImageIO.write(dialogue, "png",
                java.nio.file.Path.of(args[0], "dialogue.png").toFile());
        game.campaignStory().advanceDialogue();
        var choice = RuinedOutpostGame.class.getDeclaredField("choosingEnding");
        choice.setAccessible(true);
        choice.setBoolean(game, true);
        var choices = snapshot(game, false);
        assert game.choosingEnding() : "rendering must not make the final choice";
        if (args.length > 0) javax.imageio.ImageIO.write(choices, "png",
                java.nio.file.Path.of(args[0], "choice.png").toFile());
        var ending = RuinedOutpostGame.class.getDeclaredField("campaignEnding");
        ending.setAccessible(true);
        var endingFrames = new HashSet<Integer>();
        for (var result : CampaignStory.Ending.values()) {
            ending.set(game, result);
            var frame = snapshot(game, false);
            assert endingFrames.add(Arrays.hashCode(frame.getRGB(120, 106, 1040, 508, null, 0, 1040)))
                    : "the two endings need distinct consequence text";
            if (args.length > 0) javax.imageio.ImageIO.write(frame, "png",
                    java.nio.file.Path.of(args[0], "ending-" + result + ".png").toFile());
        }
        System.out.println("CampaignRenderingTest passed");
    }

    private static BufferedImage snapshot(RuinedOutpostGame game, boolean map) {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        int cx = Math.max(0, (int) game.player().x() - 640), cy = Math.max(0, (int) game.player().y() - 360);
        CampaignRenderer.drawGround(g, game, cx, cy, 1280, 720);
        CampaignRenderer.drawScenery(g, game, cx, cy, 1280, 720);
        for (var body : game.scouts()) CampaignRenderer.drawEnemy(g, game.campaignEnemy(body), cx, cy);
        CampaignRenderer.drawBoss(g, game.guardian(), game.biome(), cx, cy);
        CampaignRenderer.drawHud(g, game, 1280, 720);
        CampaignRenderer.drawOverlay(g, game, 1280, 720);
        if (map) CampaignRenderer.drawMap(g, game, 1280, 720);
        g.dispose();
        return image;
    }

    private static long opaquePixels(BufferedImage image) {
        return Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                .filter(pixel -> (pixel >>> 24) != 0).count();
    }
}
