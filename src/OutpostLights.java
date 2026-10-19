import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Night lighting for the Outpost: soft pools of fire, lamp, Ichor and Warden light drawn over the
 * scene. Each glow is a native-resolution disc whose alpha falls off in four bands with ordered
 * dither between them, drawn at 2x, so the light keeps the pixel grid. The ground under static
 * lights is already warmed in the baked floor; these glows add the flicker and light the actors.
 */
final class OutpostLights {
    private OutpostLights() { }

    private static final Map<Long, BufferedImage> CACHE = new HashMap<>();
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    /** Glow disc of `radius` native pixels; slightly squashed vertically for the 3/4 view. */
    static BufferedImage glow(int radius, int rgb, int peakAlpha) {
        long key = ((long) radius << 40) | ((rgb & 0xffffffL) << 8) | (peakAlpha & 0xff);
        return CACHE.computeIfAbsent(key, k -> {
            int size = radius * 2;
            var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                double d = Math.hypot(x + .5 - radius, (y + .5 - radius) * 1.25) / radius;
                if (d >= 1) continue;
                double strength = (1 - d) * (1 - d);
                double level = strength * 4 + (BAYER[(y & 3) * 4 + (x & 3)] + .5) / 16 - .5;
                int band = Math.max(0, Math.min(4, (int) Math.floor(level + .5)));
                if (band == 0) continue;
                image.setRGB(x, y, (peakAlpha * band / 4) << 24 | (rgb & 0xffffff));
            }
            return image;
        });
    }

    static void draw(Graphics2D g, RuinedOutpostGame game, int cx, int cy, long frame, boolean reduced) {
        int index = 0;
        boolean arenaCold = game.guardian().alive() && game.guardian().state() == Guardian.State.DORMANT;
        var gatehouse = game.campaignArea().landmarks().get(8);
        for (var light : SetPieces.lights(game.biome())) {
            index++;
            if (arenaCold && light.fire() && Math.hypot(light.x() - gatehouse.x(), light.y() - gatehouse.y()) < 470) continue;
            double sx = light.x() - cx, sy = light.y() - cy, r = light.radius();
            if (sx < -r || sy < -r || sx > 1280 + r || sy > 720 + r) continue;
            // fires breathe between three sizes; lamps and Ichor hold steady
            int step = light.fire() && !reduced ? Math.floorMod((frame / 5 + index * 7) * 2654435761L >>> 7, 3) : 1;
            int radius = (int) Math.round(r / 2 * (.96 + step * .04));
            disc(g, glow(radius, light.colour(), light.fire() ? 64 : 52), sx, sy, radius);
        }
        var player = game.player();
        if (player.alive()) disc(g, glow(46, 0x2cc6c8, 34), player.x() - cx, player.y() - cy, 46);
        var warden = game.guardian();
        if (warden.alive() && warden.visible() && warden.state() != Guardian.State.DORMANT) {
            boolean rage = warden.enraged();
            int pulse = reduced ? 0 : (int) (frame / 6 % 3);
            int radius = rage ? 92 + pulse * 3 : 78;
            int colour = game.biome() == 1 ? (rage ? 0xffdd80 : 0xeeb840) : rage ? 0x7aeee6 : 0x2cc6c8;   // Briarheart burns gold
            disc(g, glow(radius, colour, rage ? 64 : 40), warden.x() - cx, warden.y() - cy - 40, radius);
        }
    }

    private static void disc(Graphics2D g, BufferedImage image, double sx, double sy, int radius) {
        int left = (int) Math.round(sx / 2) * 2 - radius * 2, top = (int) Math.round(sy / 2) * 2 - radius * 2;
        g.drawImage(CampaignRenderer.doubled(image), left, top, null);
    }
}
