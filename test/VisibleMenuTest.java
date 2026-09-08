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
            AbstractButton menu = button(panel, "Menu");
            assert menu.isVisible() && menu.getWidth() >= 90 && menu.getHeight() >= 30;
            menu.doClick();
            assert button(panel, "Settings").isVisible();
            assert !button(panel, "Admin / Testing").isEnabled();
            press(panel, "begin");
            var click = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, 500, 500, 1, false, MouseEvent.BUTTON1);
            for (var listener : panel.getMouseListeners()) listener.mousePressed(click);
            assert game.story().phase() == OutpostStory.Phase.PROLOGUE : "menu clicks and keys cannot start a hidden game";
            press(panel, "pause");
            press(panel, "begin");
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
