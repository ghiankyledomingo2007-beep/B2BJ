import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class CampaignMenuTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(CampaignMenuTest::visibleNavigationReusesExistingPanels);
        if (args.length > 0) {
            Path output = Path.of(args[0]);
            Files.createDirectories(output);
            SwingUtilities.invokeAndWait(() -> captureIntegratedMenu(output));
        }
        System.out.println("CampaignMenuTest passed");
    }

    private static void visibleNavigationReusesExistingPanels() {
        var game = RuinedOutpostGame.campaign();
        int[] callbacks = {0, 0};
        boolean[] reduced = {false};
        var menu = new CampaignMenu(game, () -> callbacks[0]++, () -> callbacks[1]++,
                value -> reduced[0] = value, () -> reduced[0]);
        assert !button(menu, "Resume").isEnabled() && !button(menu, "Return to Title").isEnabled();
        assert !button(menu, "Admin / Testing").isEnabled() : "title screen cannot enable cheats";
        assert button(menu, "Settings").isEnabled();
        var controlsButton = button(menu, "Controls");
        var hover = new java.awt.event.MouseEvent(controlsButton, java.awt.event.MouseEvent.MOUSE_ENTERED,
                0, 0, 4, 4, 0, false);
        for (var listener : controlsButton.getMouseListeners()) listener.mouseEntered(hover);
        assert Boolean.TRUE.equals(controlsButton.getClientProperty("menu.selected"));
        var settingsButton = button(menu, "Settings");
        var focus = new java.awt.event.FocusEvent(settingsButton, java.awt.event.FocusEvent.FOCUS_GAINED);
        for (var listener : settingsButton.getFocusListeners()) listener.focusGained(focus);
        assert Boolean.TRUE.equals(settingsButton.getClientProperty("menu.selected"))
                && Boolean.FALSE.equals(controlsButton.getClientProperty("menu.selected"))
                : "keyboard focus must replace the hovered selection, never light two rows";
        assert button(menu, "Close").isEnabled() : "every menu page needs an internal close action";
        assert descendants(menu).stream().noneMatch(JTextArea.class::isInstance)
                : "pause home should leave instructions in Controls";
        assert menu.getPreferredSize().width <= 600 && menu.getPreferredSize().height <= 688;
        menu.setSize(menu.getPreferredSize());
        layout(menu);
        var frame = new BufferedImage(menu.getWidth(), menu.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var frameGraphics = frame.createGraphics();
        menu.paintComponent(frameGraphics);
        frameGraphics.dispose();
        var colors = new java.util.HashSet<Integer>();
        for (int y = 0; y < frame.getHeight(); y += 2) for (int x = 0; x < frame.getWidth(); x += 2) {
            int pixel = frame.getRGB(x, y);
            colors.add(pixel);
            assert pixel == frame.getRGB(x + 1, y) && pixel == frame.getRGB(x, y + 1)
                    && pixel == frame.getRGB(x + 1, y + 1) : "panel art must stay on the native 2x pixel grid";
        }
        assert (frame.getRGB(0,0)>>>24)==0 : "carved frame silhouette must leave the world visible";
        assert (frame.getRGB(frame.getWidth()/2,frame.getHeight()/2)>>>24)==255 : "content well must stay readable";
        assert button(menu, "Close").getWidth() == 76 : "Close must keep its compact explicit width";
        for (String name : List.of("Resume", "Settings", "Admin / Testing", "Return to Title")) {
            assert button(menu, name).getHeight() >= 32
                    : "first layout must leave every Home button visible: " + name + " " + button(menu, name).getBounds()
                    + " / help " + descendants(menu).stream().filter(javax.swing.JTextArea.class::isInstance)
                            .map(component -> component.getBounds() + " preferred=" + component.getPreferredSize()).toList();
            var button = button(menu, name);
            assert Ui.width(button.getText(), 3) + 64 <= button.getWidth() - 36
                    : "larger labels must leave room for their icon and selection crystal";
            var bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), menu);
            assert new java.awt.Rectangle(0, 0, menu.getWidth(), menu.getHeight()).contains(bounds)
                    : "Home button must stay inside the visible menu: " + name;
        }
        game.begin();
        game.pause();
        menu.home();
        assert button(menu, "Resume").isEnabled() && button(menu, "Return to Title").isEnabled();
        button(menu, "Controls").doClick();
        assert descendants(menu).stream().filter(CampaignMenu.ControlsSheet.class::isInstance)
                .anyMatch(c -> c.getAccessibleContext().getAccessibleDescription().contains("Riposte"))
                : "Controls must open and expose its bindings to screen readers";
        button(menu, "Back").doClick();
        button(menu, "Settings").doClick();
        assert descendants(menu).stream().filter(JSlider.class::isInstance).count() == 4
                : "Settings must embed the existing audio options";
        JCheckBox effects = descendants(menu).stream().filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
                .filter(box -> box.getText().startsWith("Reduce flashes")).findFirst().orElseThrow();
        effects.doClick();
        assert reduced[0];
        button(menu, "Back").doClick();
        button(menu, "Settings").doClick();
        assert descendants(menu).stream().filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
                .anyMatch(box -> box.getText().startsWith("Reduce flashes") && box.isSelected())
                : "reopened settings must use the current effects state";
        button(menu, "Back").doClick();
        button(menu, "Admin / Testing").doClick();
        assert !game.debugSession() : "navigation into Admin must remain read-only";
        assert !button(menu, "Unlimited Blade form").isSelected();
        button(menu, "Unlimited Blade form").doClick();
        assert game.debugUnlimitedBlade() && game.debugSession() && game.player().ichor() == 100;
        button(menu, "Back").doClick();
        button(menu, "Admin / Testing").doClick();
        assert button(menu, "Unlimited Blade form").isSelected() : "reopened panel reflects active cheat";
        button(menu, "Unlimited Blade form").doClick();
        assert !game.debugUnlimitedBlade() && game.debugSession() : "disabling never restores saving";
        button(menu, "God mode").doClick();
        assert game.debugGodMode() && game.debugSession();
        button(menu, "Back").doClick();
        assert descendants(menu).stream().anyMatch(component -> component instanceof JLabel label
                && label.getText().contains("NORMAL SAVE PROTECTED")) : "home must preserve visible test-session status";
        assert game.paused() && callbacks[0] == 0 && callbacks[1] == 0;
        button(menu, "Resume").doClick();
        button(menu, "Return to Title").doClick();
        assert callbacks[0] == 1 && callbacks[1] == 1 : "only explicit menu actions invoke host callbacks";
    }

    private static AbstractButton button(Container root, String text) {
        return descendants(root).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }

    private static List<Component> descendants(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container container) result.addAll(descendants(container));
        }
        return result;
    }

    private static void captureIntegratedMenu(Path output) {
        var game = RuinedOutpostGame.campaign();
        game.begin();
        game.skipDialogue();
        var desktop = new B2BJ(false, game);
        desktop.setSize(1280, 720);
        layout(desktop);
        capture(desktop, output.resolve("menu-button.png"));
        button(desktop, "Menu").doClick();
        desktop.step(.01);
        assert !button(desktop, "Menu").isVisible() : "internal Close replaces the duplicate corner control";
        button(desktop, "Resume").getModel().setRollover(true);
        capture(desktop, output.resolve("menu-home.png"));
        var resumeModel = button(desktop, "Resume").getModel();
        resumeModel.setArmed(true);
        resumeModel.setPressed(true);
        capture(desktop, output.resolve("menu-pressed.png"));
        resumeModel.setArmed(false);
        resumeModel.setPressed(false);
        assert game.paused() : "cancelling a press must not activate Resume";
        button(desktop, "Controls").doClick();
        capture(desktop, output.resolve("menu-controls.png"));
        button(desktop, "Back").doClick();
        button(desktop, "Settings").doClick();
        capture(desktop, output.resolve("menu-settings.png"));
        button(desktop, "Back").doClick();
        button(desktop, "Admin / Testing").doClick();
        button(desktop, "God mode").doClick();
        button(desktop, "Unlimited Blade form").doClick();
        capture(desktop, output.resolve("menu-admin.png"));
        button(desktop, "Back").doClick();
        capture(desktop, output.resolve("menu-protected-home.png"));
    }

    private static void capture(B2BJ desktop, Path output) {
        layout(desktop);
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        desktop.paint(graphics);
        graphics.dispose();
        try { javax.imageio.ImageIO.write(image, "png", output.toFile()); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
    }
}
