import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Reproducible integrated captures using isolated checkpoint files. No player save is read. */
public final class PreviewCampaign {
    public static void main(String[] args) throws Exception {
        Path output = args.length == 0 ? Files.createTempDirectory("b2bj-campaign-preview-") : Path.of(args[0]);
        Files.createDirectories(output);
        Path fixtures = Files.createTempDirectory("b2bj-campaign-fixtures-");
        var title = RuinedOutpostGame.campaign();
        capture(new B2BJ(false, title), output.resolve("title.png"));
        for (int biome = 0; biome < 4; biome++) {
            Set<Integer> cleared = new HashSet<>();
            for (int earlier = 0; earlier < biome; earlier++) cleared.add(earlier);
            Path save = fixtures.resolve("region-" + biome + ".properties");
            CampaignSave.save(save, new CampaignSave.Progress(biome, 1, cleared, Set.of(),
                    Map.of("vitality", 1, "capacity", 1, "efficiency", 1), Set.of("talked_" + biome), Set.of(), 8));
            var game = RuinedOutpostGame.campaign(save);
            if (biome == 0) {
                game.begin();
                capture(new B2BJ(false, game), output.resolve("title-confirm.png"));
            }
            assert game.continueCampaign();
            var panel = new B2BJ(false, game);
            capture(panel, output.resolve("biome-" + biome + "-hub.png"));
            var landmark = game.campaignArea().landmarks().get(4);
            game.player().relocate(landmark.x() - 280, landmark.y() + 120);
            capture(panel, output.resolve("biome-" + biome + "-exploration.png"));
            var enemy = game.scouts().get(0);
            game.player().relocate(enemy.x() - 120, enemy.y());
            panel.step(.20);
            capture(panel, output.resolve("biome-" + biome + "-combat.png"));
            var boss = game.guardian();
            game.player().relocate(boss.x() - 260, boss.y() + 90);
            game.player().collectIchor(100);
            game.transform();
            panel.step(.2);
            panel.step(.35);
            panel.step(.35);
            capture(panel, output.resolve("biome-" + biome + "-boss.png"));
        }
        Path finale = fixtures.resolve("finale.properties");
        CampaignSave.save(finale, new CampaignSave.Progress(3, 1, Set.of(0, 1, 2, 3), Set.of(),
                Map.of(), Set.of("talked_3"), Set.of(), 20));
        for (var ending : CampaignStory.Ending.values()) {
            var game = RuinedOutpostGame.campaign(finale);
            assert game.continueCampaign();
            var exit = game.campaignArea().exit();
            game.player().relocate(exit.x(), exit.y());
            assert game.interact() && game.choosingEnding();
            var panel = new B2BJ(false, game);
            capture(panel, output.resolve("choice.png"));
            assert game.chooseEnding(ending);
            capture(panel, output.resolve("ending-" + ending + ".png"));
        }
        System.out.println("Campaign screenshots: " + output.toAbsolutePath());
    }

    private static void capture(B2BJ panel, Path path) throws Exception {
        panel.setSize(1280, 720);
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }
}
