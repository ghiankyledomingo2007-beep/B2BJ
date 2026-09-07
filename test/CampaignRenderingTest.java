import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;

public final class CampaignRenderingTest {
    public static void main(String[] args) {
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
        System.out.println("CampaignRenderingTest passed");
    }

    private static long opaquePixels(BufferedImage image) {
        return Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                .filter(pixel -> (pixel >>> 24) != 0).count();
    }
}
