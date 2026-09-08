import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** Embedded game menu. The desktop host owns pausing, visibility, and return-to-title behavior. */
@SuppressWarnings("serial")
public final class CampaignMenu extends JPanel {
    private final RuinedOutpostGame game;
    private final Runnable resume;
    private final Runnable title;
    private final Consumer<Boolean> effectsChanged;
    private final BooleanSupplier reducedEffects;

    public CampaignMenu(RuinedOutpostGame game, Runnable resume, Runnable title,
            Consumer<Boolean> effectsChanged, BooleanSupplier reducedEffects) {
        super(new BorderLayout(12, 16));
        this.game = Objects.requireNonNull(game, "game");
        this.resume = Objects.requireNonNull(resume, "resume");
        this.title = Objects.requireNonNull(title, "title");
        this.effectsChanged = Objects.requireNonNull(effectsChanged, "effectsChanged");
        this.reducedEffects = Objects.requireNonNull(reducedEffects, "reducedEffects");
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        setPreferredSize(new Dimension(580, 630));
        getAccessibleContext().setAccessibleName("Game menu");
        home();
    }

    public void home() {
        removeAll();
        add(heading("GAME MENU"), BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        JLabel status = new JLabel(game.debugSession() ? "TEST SESSION — NORMAL SAVE PROTECTED"
                : game.paused() ? "CAMPAIGN PAUSED" : "SETTINGS AND GAME CONTROLS", SwingConstants.CENTER);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        content.add(status, BorderLayout.NORTH);
        boolean ongoing = game.campaignMode() && game.player().alive() && !game.story().blocksGameplay();
        JPanel actions = new JPanel(new GridLayout(4, 1, 0, 12));
        actions.add(button("Resume", 'R', resume, ongoing));
        actions.add(button("Settings", 'S', () -> page("SETTINGS",
                CampaignOptions.panel(effectsChanged, reducedEffects.getAsBoolean())), true));
        actions.add(button("Admin / Testing", 'A', () -> page("ADMIN / TESTING", DebugPanel.panel(game)),
                game.debugAvailable()));
        actions.add(button("Return to Title", 'T', title, ongoing && game.paused()));
        content.add(actions, BorderLayout.CENTER);
        JTextArea help = new JTextArea(game.debugSession()
                ? "Testing changes stay in this session. Return to title and Continue to restore the normal saved journey."
                : "Settings change audio and effects. Admin / Testing provides protected controls for trying combat, upgrades, and regions.");
        help.setEditable(false);
        help.setOpaque(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setRows(3);
        help.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        help.setPreferredSize(new Dimension(0, help.getFontMetrics(help.getFont()).getHeight() * help.getRows()));
        help.getAccessibleContext().setAccessibleName("Game menu help");
        content.add(help, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void page(String title, JPanel content) {
        removeAll();
        add(heading(title), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getAccessibleContext().setAccessibleName(title + " controls");
        add(scroll, BorderLayout.CENTER);
        add(button("Back", 'B', this::home, true), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private static JLabel heading(String title) {
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        return label;
    }

    private static JButton button(String text, char mnemonic, Runnable action, boolean enabled) {
        JButton button = new JButton(text);
        button.setMnemonic(mnemonic);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 16f));
        button.setEnabled(enabled);
        button.addActionListener(event -> action.run());
        return button;
    }
}
