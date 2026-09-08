import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Native testing controls. Opening the menu is read-only; controller commands protect the normal save. */
public final class DebugPanel {
    private DebugPanel() { }

    public static void show(Component parent, RuinedOutpostGame game, Runnable beforeOpen) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(beforeOpen, "beforeOpen");
        if (!game.debugAvailable()) return;
        beforeOpen.run();
        JOptionPane.showMessageDialog(parent, panel(game), "Blob to Blade / Testing", JOptionPane.PLAIN_MESSAGE);
    }

    static JPanel panel(RuinedOutpostGame game) {
        Objects.requireNonNull(game, "game");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel status = new JLabel();
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        Runnable refresh = () -> status.setText(game.debugSession()
                ? "TEST SESSION — NORMAL SAVE PROTECTED" : "NORMAL SESSION — NO CHEATS USED");
        refresh.run();
        panel.add(status);
        JTextArea help = new JTextArea("Using a cheat disables saving until you return to title and Continue.\n"
                + "Closing this menu leaves the game paused. Esc resumes play.\n"
                + "One-shot attacks still need to connect and cannot pass through walls.");
        help.setEditable(false);
        help.setOpaque(false);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        help.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        help.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        help.getAccessibleContext().setAccessibleName("Testing session and save protection help");
        panel.add(help);

        JCheckBox god = new JCheckBox("God mode", game.debugGodMode());
        god.setMnemonic('G');
        god.setEnabled(game.debugAvailable());
        god.addActionListener(event -> {
            game.setDebugGodMode(god.isSelected());
            god.setSelected(game.debugGodMode());
            refresh.run();
        });
        panel.add(god);
        JCheckBox oneShot = new JCheckBox("One-shot attacks", game.debugOneShot());
        oneShot.setMnemonic('O');
        oneShot.setEnabled(game.debugAvailable());
        oneShot.addActionListener(event -> {
            game.setDebugOneShot(oneShot.isSelected());
            oneShot.setSelected(game.debugOneShot());
            refresh.run();
        });
        panel.add(oneShot);

        JPanel supplies = row();
        supplies.add(button("Heal + refill / reset cooldowns", 'H', game, game::debugRefill, refresh));
        supplies.add(button("+25 shards", 'S', game, game::debugAddShards, refresh));
        panel.add(supplies);
        String[] names = new String[CampaignWorld.AREA_COUNT];
        for (int biome = 0; biome < names.length; biome++) names[biome] = CampaignWorld.area(biome).name();
        JComboBox<String> regions = new JComboBox<>(names);
        regions.setSelectedIndex(game.biome());
        regions.setEnabled(game.debugAvailable());
        regions.setAlignmentX(Component.LEFT_ALIGNMENT);
        regions.getAccessibleContext().setAccessibleName("Testing destination region");
        JLabel destination = new JLabel("Destination region");
        destination.setLabelFor(regions);
        destination.setDisplayedMnemonic('D');
        destination.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        panel.add(destination);
        panel.add(regions);
        JPanel travel = row();
        travel.add(button("Go to camp", 'C', game, () -> game.debugWarp(regions.getSelectedIndex(), false), refresh));
        travel.add(button("Go to boss", 'B', game, () -> game.debugWarp(regions.getSelectedIndex(), true), refresh));
        panel.add(travel);
        JPanel encounter = row();
        encounter.add(button("Reset current encounter", 'R', game, game::debugResetEncounter, refresh));
        panel.add(encounter);
        if (!game.debugAvailable()) panel.add(new JLabel("Start or resume a live campaign to use testing controls."));
        return panel;
    }

    private static JPanel row() {
        JPanel row = new JPanel(new GridLayout(1, 0, 8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JButton button(String text, char mnemonic, RuinedOutpostGame game,
            BooleanSupplier command, Runnable refresh) {
        JButton button = new JButton(text);
        button.setMnemonic(mnemonic);
        button.setEnabled(game.debugAvailable());
        button.addActionListener(event -> {
            command.getAsBoolean();
            refresh.run();
        });
        return button;
    }
}
