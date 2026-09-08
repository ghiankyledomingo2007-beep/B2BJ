import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public final class DebugPanelTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(DebugPanelTest::controlsAffectOnlyAnExplicitTestSession);
        System.out.println("DebugPanelTest passed");
    }

    private static void controlsAffectOnlyAnExplicitTestSession() {
        var game = RuinedOutpostGame.campaign();
        var titlePanel = DebugPanel.panel(game);
        assert descendants(titlePanel).stream().filter(AbstractButton.class::isInstance)
                .map(AbstractButton.class::cast).noneMatch(AbstractButton::isEnabled)
                : "cheats must be unavailable on the title screen";
        boolean[] opened = {false};
        DebugPanel.show(null, game, () -> opened[0] = true);
        assert !opened[0] : "unavailable debug menu must not open or alter input state";
        game.begin();
        game.pause();
        var panel = DebugPanel.panel(game);
        assert !game.debugSession() : "opening controls must not enable cheats or affect saving";
        JCheckBox god = (JCheckBox) button(panel, "God mode");
        JCheckBox oneShot = (JCheckBox) button(panel, "One-shot attacks");
        god.doClick();
        assert game.debugGodMode() && game.debugSession();
        god.doClick();
        assert !game.debugGodMode() && game.debugSession() : "turning cheats off must not resume normal saving";
        oneShot.doClick();
        assert game.debugOneShot();
        oneShot.doClick();
        assert !game.debugOneShot();
        game.player().hurt(1);
        button(panel, "Heal + refill / reset cooldowns").doClick();
        assert game.player().healthValue() == game.player().maxHealth() && game.player().ichor() == 100;
        int shards = game.shards();
        button(panel, "+25 shards").doClick();
        assert game.shards() == shards + 25;
        JComboBox<?> regions = descendants(panel).stream().filter(JComboBox.class::isInstance)
                .map(JComboBox.class::cast).findFirst().orElseThrow();
        assert regions.getItemCount() == 4;
        assert descendants(panel).stream().anyMatch(component -> component instanceof JLabel label
                && label.getLabelFor() == regions) : "region selector needs an accessible label";
        regions.setSelectedIndex(2);
        button(panel, "Go to camp").doClick();
        assert game.biome() == 2 && game.nearCampaignHub();
        button(panel, "Go to boss").doClick();
        assert Math.hypot(game.player().x() - game.guardian().x(), game.player().y() - game.guardian().y()) < 650;
        button(panel, "Reset current encounter").doClick();
        assert game.guardian().alive() && game.paused() : "testing actions must leave the simulation paused";
        assert descendants(panel).stream().anyMatch(component -> component instanceof JLabel label
                && label.getText().contains("TEST SESSION") && label.getText().contains("NORMAL SAVE PROTECTED"))
                : "active test session must remain visible even after toggles are switched off";
        var desktop = new B2BJ(false, game);
        Object binding = desktop.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, false));
        assert binding != null && desktop.getActionMap().get(binding) != null : "F1 must open testing controls";
    }

    private static AbstractButton button(Container parent, String name) {
        return descendants(parent).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(button -> button.getText().equals(name)).findFirst().orElseThrow();
    }

    private static List<Component> descendants(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            result.add(child);
            if (child instanceof Container container) result.addAll(descendants(container));
        }
        return result;
    }
}
