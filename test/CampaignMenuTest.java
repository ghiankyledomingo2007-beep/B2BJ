import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

public final class CampaignMenuTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(CampaignMenuTest::visibleNavigationReusesExistingPanels);
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
        assert menu.getPreferredSize().width <= 600 && menu.getPreferredSize().height <= 650;
        game.begin();
        game.pause();
        menu.home();
        assert button(menu, "Resume").isEnabled() && button(menu, "Return to Title").isEnabled();
        button(menu, "Settings").doClick();
        assert descendants(menu).stream().filter(JSlider.class::isInstance).count() == 3
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
        button(menu, "God mode").doClick();
        assert game.debugGodMode() && game.debugSession();
        button(menu, "Back").doClick();
        assert descendants(menu).stream().anyMatch(component -> component instanceof JLabel label
                && label.getText().contains("NORMAL SAVE PROTECTED")) : "home must preserve visible test-session status";
        assert game.paused() && callbacks[0] == 0 && callbacks[1] == 0;
        button(menu, "Resume").doClick();
        button(menu, "Return to Title").doClick();
        assert callbacks[0] == 1 && callbacks[1] == 1 : "only explicit menu actions invoke host callbacks";
        game.campaignStory().talk(0);
        menu.home();
        assert button(menu, "Resume").isEnabled() : "menu must close back into pending dialogue";
        assert !button(menu, "Admin / Testing").isEnabled() : "dialogue must not enable testing mutations";
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
}
