import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

/** The pixel UI kit, the title menu's guard on saves, and the region card. Headless, temp saves only. */
public final class UiOverhaulTest {
    public static void main(String[] args) throws Exception {
        kitIsWhole();
        SwingUtilities.invokeAndWait(() -> {
            try {
                titleMenuGuardsTheSave();
                areaCardFloatsThenLeaves();
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
        System.out.println("UiOverhaulTest passed");
    }

    /** Fonts, atlas and title art load; every kit rectangle lies inside the atlas and has pixels in it. */
    private static void kitIsWhole() {
        assert Ui.fontsLoaded() : "pixel fonts must load from assets/ui/fonts";
        assert Ui.sans(2).getSize() == 20 && Ui.display(3).getSize() == 30;
        var kit = Ui.KIT;
        assert kit != null : "assets/ui/kit.png must load";
        for (var piece : Ui.Piece.values()) {
            assert piece.x >= 0 && piece.y >= 0 && piece.x + piece.w <= kit.getWidth() && piece.y + piece.h <= kit.getHeight()
                    : piece + " lies outside the atlas";
            assert piece.inset * 2 < Math.max(piece.w, piece.h) || piece.inset == 0 : piece + " has no middle to stretch";
            assert painted(kit, piece.x, piece.y, piece.w, piece.h) : piece + " is empty in kit.png";
        }
        for (var icon : Ui.Icon.values())
            assert painted(kit, icon.x(), icon.y(), Ui.Icon.CELL, Ui.Icon.CELL) : icon + " icon is empty";
        assert TitleScreen.available() : "title art (sky, far, fore, logo) must load";
        assert B2BJ.loadImage("assets/ui/splash.png") != null;
        // the title helper never rebuilds a cached title, and its outline frames the glyphs
        var a = Ui.title("PAUSED", Ui.GOLD_RAMP);
        assert a == Ui.title("PAUSED", Ui.GOLD_RAMP);
        assert (a.getRGB(0, 0) >>> 24) == 0 : "title images start transparent at the corner";
    }

    private static boolean painted(BufferedImage image, int x0, int y0, int w, int h) {
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) if ((image.getRGB(x, y) >>> 24) != 0) return true;
        return false;
    }

    /** Continue needs a save; with one, New Journey asks once before it replaces anything. */
    private static void titleMenuGuardsTheSave() throws Exception {
        Path save = Files.createTempFile("b2bj-title-", ".properties");
        Files.delete(save);
        try {
            var game = RuinedOutpostGame.campaign(save);
            var panel = new B2BJ(false, game);
            panel.setSize(1280, 720);
            panel.doLayout();
            panel.step(.01);
            assert !shown(panel, "Continue").isEnabled() : "nothing to continue on a first run";
            shown(panel, "New Journey").doClick();
            assert game.story().phase() != OutpostStory.Phase.PROLOGUE : "New Journey starts a first run at once";
            assert game.saveAvailable() : "a started run writes its save";
            game.pause();
            assert game.returnToTitle();
            panel.step(.01);
            assert shown(panel, "Continue").isEnabled() : "a saved run can be continued";
            var fresh = shown(panel, "New Journey");
            fresh.doClick();
            assert game.story().phase() == OutpostStory.Phase.PROLOGUE : "one click never replaces a save";
            assert fresh.getText().equals(TitleMenu.CONFIRM) : "the button asks before replacing the save";
            fresh.doClick();
            assert game.story().phase() != OutpostStory.Phase.PROLOGUE : "confirming starts the new journey";
            assert fresh.getText().equals(TitleMenu.NEW);
        } finally {
            Files.deleteIfExists(save);
        }
    }

    /** The region card paints over play right after a run starts and is gone a few seconds later. */
    private static void areaCardFloatsThenLeaves() throws Exception {
        Path save = Files.createTempFile("b2bj-card-", ".properties");
        Files.delete(save);
        try {
            var game = RuinedOutpostGame.campaign(save);
            var panel = new B2BJ(false, game);
            panel.setSize(1280, 720);
            panel.doLayout();
            panel.getActionMap().get("beginPressed").actionPerformed(new ActionEvent(panel, 0, "begin"));
            game.skipDialogue();
            for (int i = 0; i < 60; i++) panel.step(1 / 60.0);
            var during = band(panel);
            for (int i = 0; i < 60 * 4; i++) panel.step(1 / 60.0);
            var after = band(panel);
            int changed = 0;
            for (int i = 0; i < during.length; i++) if (during[i] != after[i]) changed++;
            assert changed > during.length / 20 : "the card must show over play, then leave (" + changed + " px changed)";
            assert !game.paused() && game.story().phase() != OutpostStory.Phase.PROLOGUE : "the card never stops the game";
        } finally {
            Files.deleteIfExists(save);
        }
    }

    /** The strip where the region name sits. */
    private static int[] band(B2BJ panel) {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image.getRGB(300, 190, 680, 60, null, 0, 680);
    }

    private static AbstractButton shown(B2BJ panel, String text) {
        return all(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(b -> b.getText().equals(text) && chain(b, panel)).findFirst()
                .orElseThrow(() -> new AssertionError("no visible " + text));
    }

    private static boolean chain(Component c, B2BJ root) {
        for (Component at = c; at != null && at != root; at = at.getParent()) if (!at.isVisible()) return false;
        return true;
    }

    private static List<Component> all(Container root) {
        var out = new ArrayList<Component>();
        for (Component c : root.getComponents()) { out.add(c); if (c instanceof Container k) out.addAll(all(k)); }
        return out;
    }
}
