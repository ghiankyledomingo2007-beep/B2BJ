import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Title-sized versions of the game's existing icons, in the gate scene's palette. */
final class TitleIcons {
    private static final Ui.Icon[] SYMBOLS = {Ui.Icon.GATE, Ui.Icon.MAP, Ui.Icon.GEAR, Ui.Icon.DOOR};
    private static final int[][] COLORS = {
        {0x263946, 0x7e94a2, 0xb9cbd1},
        {0x245b70, 0x59cddd, 0xd6ffff},
        {0x202b35, 0x44515e, 0x65717c}
    };
    private static final BufferedImage[][] IMAGES = makeIcons();

    private static BufferedImage[][] makeIcons() {
        var images = new BufferedImage[Ui.Icon.values().length][COLORS.length];
        for (int choice = 0; choice < Ui.Icon.values().length; choice++) {
            var symbol = Ui.Icon.values()[choice];
            for (int state = 0; state < COLORS.length; state++) {
                var image = new BufferedImage(Ui.Icon.CELL, Ui.Icon.CELL, BufferedImage.TYPE_INT_ARGB);
                if (Ui.KIT != null) for (int y = 0; y < Ui.Icon.CELL; y++) for (int x = 0; x < Ui.Icon.CELL; x++) {
                    int pixel = Ui.KIT.getRGB(symbol.x() + x, symbol.y() + y);
                    int light = (3 * ((pixel >> 16) & 255) + 6 * ((pixel >> 8) & 255) + (pixel & 255)) / 10;
                    image.setRGB(x, y, (pixel & 0xff000000) | COLORS[state][light < 65 ? 0 : light < 140 ? 1 : 2]);
                }
                images[choice][state] = image;
            }
        }
        return images;
    }

    static void draw(Graphics2D g, int choice, int x, int y, boolean selected, boolean enabled) {
        draw(g, SYMBOLS[choice], x, y, selected, enabled);
    }

    static void draw(Graphics2D g, Ui.Icon symbol, int x, int y, boolean selected, boolean enabled) {
        g.drawImage(IMAGES[symbol.ordinal()][!enabled ? 2 : selected ? 1 : 0], x, y, 24, 24, null);
    }
}
