import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

public final class VisibleMenuTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var game = RuinedOutpostGame.campaign();
            var panel = new B2BJ(false, game);
            panel.setSize(1280, 720); panel.doLayout();
            panel.step(.01);                                     // the title screen takes over with its own menu
            AbstractButton menu = button(panel, "Menu");
            assert !menu.isVisible() : "the title screen has its own menu, not the corner button";
            AbstractButton settings = shown(panel, "Settings");
            assert settings.getWidth() >= 90 && settings.getHeight() >= 30 : "title Settings must be a real mouse target";
            settings.doClick();
            assert children(panel).stream().anyMatch(c -> c instanceof javax.swing.JSlider && c.isVisible())
                    : "Settings must open the real audio controls from the title";
            press(panel, "debug");
            assert !button(panel, "God mode").isEnabled() : "the title screen cannot enable cheats";
            press(panel, "begin");
            var click = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, 500, 500, 1, false, MouseEvent.BUTTON1);
            for (var listener : panel.getMouseListeners()) listener.mousePressed(click);
            assert game.story().phase() == OutpostStory.Phase.PROLOGUE : "menu clicks and keys cannot start a hidden game";
            press(panel, "pause");                               // Esc closes the menu, back to the title
            panel.step(.01);
            assert shown(panel, "New Journey").isVisible() && !menu.isVisible();
            press(panel, "begin");
            assert game.dialogue()==OutpostDialogue.WAKE;
            game.skipDialogue();
            assert !game.blocked();
            press(panel, "right"); panel.step(.1);
            menu.doClick();
            assert game.paused() && !game.debugSession();
            assert button(panel, "Admin / Testing").isEnabled();
            button(panel, "Settings").doClick();
            assert children(panel).stream().anyMatch(c -> c instanceof javax.swing.JSlider)
                    : "Settings must open the real audio controls";
            button(panel, "Back").doClick();
            button(panel, "Admin / Testing").doClick();
            button(panel, "God mode").doClick();
            assert game.debugGodMode() && game.debugSession() && game.paused();
            button(panel, "Back").doClick();
            button(panel, "Resume").doClick();
            double x = game.player().x(); panel.step(.1);
            assert !game.paused() && game.player().x() == x : "menu clears held movement before resuming";
            assert game.projectiles().isEmpty() : "menu clicks cannot become gameplay attacks";
            menu.doClick(); button(panel, "Return to Title").doClick();
            assert game.story().phase() == OutpostStory.Phase.PROLOGUE;
            assert game.continueCampaign() && !game.debugSession() : "menu title flow must preserve normal save";
        });
        System.out.println("VisibleMenuTest passed");
    }

    /** The button with this text whose whole chain of parents is visible (titles and menus reuse labels). */
    private static AbstractButton shown(B2BJ panel, String text) {
        return children(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(button -> button.getText().equals(text) && visibleChain(button, panel)).findFirst()
                .orElseThrow(() -> new AssertionError("No visible control: " + text));
    }

    private static boolean visibleChain(Component c, B2BJ root) {
        for (Component at = c; at != null && at != root; at = at.getParent()) if (!at.isVisible()) return false;
        return true;
    }

    private static AbstractButton button(Container parent, String text) {
        return children(parent).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(button -> button.getText().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing visible menu control: " + text));
    }

    private static List<Component> children(Container parent) {
        var result = new ArrayList<Component>();
        for (Component child : parent.getComponents()) {
            result.add(child);
            if (child instanceof Container container) result.addAll(children(container));
        }
        return result;
    }

    private static void press(B2BJ panel, String action) {
        panel.getActionMap().get(action + "Pressed").actionPerformed(new ActionEvent(panel, 0, ""));
        if (!action.equals("right")) panel.getActionMap().get(action + "Released")
                .actionPerformed(new ActionEvent(panel, 0, ""));
    }
}
