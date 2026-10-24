import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Every runtime sprite shares one pixel size (1 native pixel = 2 screen pixels): no art pre-doubled inside
 * its file, no checkerboard dither, no soft alpha, and the bosses' atlases match their 2x render sizes.
 */
public final class PixelConsistencyTest {
    /** Known exceptions: the JVM splash is shown unscaled, so it is doubled on purpose; the loading layers are pending a redraw. */
    private static final Set<String> EXEMPT = Set.of("assets/ui/splash.png", "assets/ui/loading/outpost.png", "assets/ui/loading/sky.png",
            "assets/ui/title/ruined-gate.png");

    public static void main(String[] args) throws Exception {
        int checked = 0;
        try (var files = Files.walk(Path.of("assets"))) {
            for (var path : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".png") && !p.toString().contains("/source/"))::iterator) {
                String name = path.toString().replace('\\', '/');
                if (EXEMPT.contains(name)) continue;
                var image = B2BJ.loadImage(name);
                assert image != null : "unreadable " + name;
                check(name, image);
                checked++;
            }
        }
        var warden = B2BJ.loadImage("assets/characters/guardian/warden_motion.png");
        assert warden.getWidth() / 8 * 2 == Guardian.RENDER_SIZE : "the Warden is drawn at exactly 2x";
        var briar = B2BJ.loadImage("assets/characters/guardian/briarheart_motion.png");
        assert briar.getWidth() / 8 * 2 == 192 : "Briarheart is drawn at exactly 2x";
        assert checked > 150 : "asset walk found too little: " + checked;
        System.out.println("PixelConsistencyTest passed (" + checked + " sheets)");
    }

    private static void check(String name, BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight(), visible = 0, soft = 0, blocks = 0, uniform = 0, inner = 0, checker = 0;
        var colours = new java.util.HashSet<Integer>();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = image.getRGB(x, y), a = c >>> 24;
            if (a == 0) continue;
            visible++; colours.add(c);
            if (a < 255) soft++;
        }
        if (visible == 0) return;
        for (int y = 0; y + 1 < h; y += 2) for (int x = 0; x + 1 < w; x += 2) {
            int a = image.getRGB(x, y), b = image.getRGB(x + 1, y), c = image.getRGB(x, y + 1), d = image.getRGB(x + 1, y + 1);
            if ((a | b | c | d) >>> 24 == 0) continue;
            blocks++;
            if (a == b && b == c && c == d) uniform++;
        }
        for (int y = 1; y + 1 < h; y++) for (int x = 1; x + 1 < w; x++) {
            int c = image.getRGB(x, y);
            if (c >>> 24 == 0) continue;
            int l = image.getRGB(x - 1, y), r = image.getRGB(x + 1, y), u = image.getRGB(x, y - 1), d = image.getRGB(x, y + 1);
            if ((l >>> 24) == 0 || (r >>> 24) == 0 || (u >>> 24) == 0 || (d >>> 24) == 0) continue;
            inner++;
            if (l != c && r != c && u != c && d != c && image.getRGB(x - 1, y - 1) == c && image.getRGB(x + 1, y + 1) == c
                    && image.getRGB(x + 1, y - 1) == c && image.getRGB(x - 1, y + 1) == c) checker++;
        }
        assert soft == 0 : name + " has semi-transparent pixels";
        // Flat art is naturally blocky; doubled art is uniform in every block while still having detail.
        assert !(colours.size() > 6 && visible > 800 && uniform == blocks) : name + " is pre-doubled: its pixels show at 4x";
        assert inner < 200 || checker * 100 < inner * 2 : name + " uses checkerboard dither (" + checker + " of " + inner + ")";
    }
}
