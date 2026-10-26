import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public final class StoneMenuArtTest {
    public static void main(String[] args) throws Exception {
        var still = frame(0, true);
        assert same(still, frame(8, true)) : "reduced effects must freeze crest, runes and motes";
        var animated = frame(.8, false);
        assert !same(frame(0, false), animated) : "crest and runes must animate";
        nativeGrid(animated);
        assert (animated.getRGB(0, 0) >>> 24) == 0 : "exterior must remain transparent";
        assert same(row(false, false, 0, 0, false), row(false, false, 4, 0, false))
                : "unselected plates must remain still";
        assert same(row(true, false, 0, 0, true), row(true, false, 4, 3.8, true))
                : "reduced effects must suppress both pulse and selection sweep";
        assert !same(row(true, false, 2, 1.8, false), row(true, false, 2, 0, false))
                : "new selection gets a short sweep";
        assert same(row(true, false, 2, 1.5, false), row(true, false, 2, 0, false))
                : "sweep must finish after 450ms";
        assert !same(row(true, false, 2, 0, true), row(true, true, 2, 0, true))
                : "press must move the plate down";
        nativeGrid(row(true, true, 2, 1.8, false));
        SwingUtilities.invokeAndWait(StoneMenuArtTest::pauseKeepsWorldFrozen);
        System.out.println("StoneMenuArtTest passed: motion, reduced effects, pixel grid and paused world");
    }

    private static void pauseKeepsWorldFrozen() {
        var game = RuinedOutpostGame.campaign();
        game.begin(); game.skipDialogue();
        var desktop = new B2BJ(false, game);
        desktop.setSize(1280, 720); desktop.doLayout();
        desktop.getActionMap().get("pausePressed").actionPerformed(new java.awt.event.ActionEvent(desktop, 0, ""));
        desktop.getActionMap().get("pauseReleased").actionPerformed(new java.awt.event.ActionEvent(desktop, 0, ""));
        var menu = Arrays.stream(desktop.getComponents()).filter(CampaignMenu.class::isInstance)
                .map(CampaignMenu.class::cast).findFirst().orElseThrow();
        var before = render(600, 680, g -> menu.paintComponent(g));
        double x = game.player().x(), y = game.player().y(), hp = game.player().healthValue();
        for (int i = 0; i < 60; i++) desktop.step(1.0 / 60);
        assert game.paused() && game.player().x() == x && game.player().y() == y && game.player().healthValue() == hp
                : "UI animation must not resume movement or combat";
        assert !same(before, render(600, 680, g -> menu.paintComponent(g)))
                : "menu clock must advance while gameplay is paused";
    }

    private static BufferedImage frame(double time, boolean reduced) {
        return render(600, 680, g -> StoneMenuArt.frame(g, 600, 680, time, reduced));
    }

    private static BufferedImage row(boolean selected, boolean pressed, double time, double selectedAt, boolean reduced) {
        return render(424, 60, g -> StoneMenuArt.row(g, 424, 60, selected, pressed, time, selectedAt, reduced));
    }

    private static BufferedImage render(int width, int height, Consumer<Graphics2D> paint) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); paint.accept(g); g.dispose();
        return image;
    }

    private static boolean same(BufferedImage a, BufferedImage b) {
        return Arrays.equals(a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth()),
                b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth()));
    }

    private static void nativeGrid(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 2) for (int x = 0; x < image.getWidth(); x += 2) {
            int pixel = image.getRGB(x, y);
            assert pixel == image.getRGB(x + 1, y) && pixel == image.getRGB(x, y + 1)
                    && pixel == image.getRGB(x + 1, y + 1) : "effects must respect native 2x pixels";
        }
    }
}
