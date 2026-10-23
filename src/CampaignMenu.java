import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
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
        super(new BorderLayout(12, 14));
        this.game = Objects.requireNonNull(game, "game");
        this.resume = Objects.requireNonNull(resume, "resume");
        this.title = Objects.requireNonNull(title, "title");
        this.effectsChanged = Objects.requireNonNull(effectsChanged, "effectsChanged");
        this.reducedEffects = Objects.requireNonNull(reducedEffects, "reducedEffects");
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 40, 30, 40));
        setPreferredSize(new Dimension(540, 620));
        getAccessibleContext().setAccessibleName("Game menu");
        home();
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g = (Graphics2D) graphics.create();
        Ui.crisp(g);
        Ui.window(g, 0, 0, getWidth(), getHeight());
        g.dispose();
    }

    public void home() {
        removeAll();
        boolean ongoing = game.campaignMode() && game.player().alive() && !game.story().blocksGameplay();
        add(heading(ongoing && game.paused() ? "PAUSED" : "MENU"), BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 16));
        JLabel status = new JLabel(game.debugSession() ? "TEST SESSION — NORMAL SAVE PROTECTED"
                : game.paused() ? "CAMPAIGN PAUSED" : "SETTINGS AND GAME CONTROLS", SwingConstants.CENTER);
        content.add(status, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(5, 1, 0, 10));
        actions.add(PixelSwing.button("Resume", 'R', Ui.Icon.PLAY, resume, ongoing));
        actions.add(PixelSwing.button("Settings", 'S', Ui.Icon.GEAR, this::settings, true));
        actions.add(PixelSwing.button("Controls", 'C', Ui.Icon.KEYS, () -> page("CONTROLS", new ControlsSheet()), true));
        actions.add(PixelSwing.button("Admin / Testing", 'A', Ui.Icon.FLASK, this::admin, game.debugAvailable()));
        actions.add(PixelSwing.button("Return to Title", 'T', Ui.Icon.DOOR, title, ongoing && game.paused()));
        content.add(actions, BorderLayout.CENTER);
        JTextArea help = new JTextArea(game.debugSession()
                ? "Testing changes stay in this session. Return to title and Continue to restore the normal saved journey."
                : "Blade: click LMB for a four-hit combo. Hold LMB, then release for a charged cleave. Space cancels charging. Settings change audio and effects.");
        help.setEditable(false);
        help.setFocusable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setRows(3);
        help.getAccessibleContext().setAccessibleName("Game menu help");
        content.add(help, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        PixelSwing.style(this);
        status.setForeground(game.debugSession() ? Ui.AMBER : Ui.MUTED);
        help.setPreferredSize(new Dimension(0, help.getFontMetrics(help.getFont()).getHeight() * help.getRows()));
        revalidate();
        repaint();
    }

    /** Opens the Settings page (also the O key). */
    public void settings() { page("SETTINGS", CampaignOptions.panel(effectsChanged, reducedEffects.getAsBoolean())); }

    /** Opens the Admin / Testing page (also F1); its controls stay disabled outside a live run. */
    public void admin() { page("ADMIN / TESTING", DebugPanel.panel(game)); }

    /** Puts keyboard focus on the first enabled control, so arrows and Enter work at once. */
    public void focusFirst() {
        for (var c : PixelSwing.controls(this)) if (c.isEnabled() && c.isFocusable()) { c.requestFocusInWindow(); return; }
    }

    private void page(String title, JComponent content) {
        removeAll();
        add(heading(title), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(new PixelSwing.Fit(content), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getAccessibleContext().setAccessibleName(title + " controls");
        add(scroll, BorderLayout.CENTER);
        // from the title screen, Back returns to the title instead of an in-run menu home
        Runnable backAction = game.story().phase() == OutpostStory.Phase.PROLOGUE ? resume : this::home;
        JButton back = PixelSwing.button("Back", 'B', Ui.Icon.BACK, backAction, true);
        add(back, BorderLayout.SOUTH);
        PixelSwing.style(this);
        revalidate();
        repaint();
        javax.swing.SwingUtilities.invokeLater(this::revalidate);   // second pass: wrapped text now knows its width
        focusFirst();
    }

    private static JLabel heading(String title) {
        return new PixelSwing.Title(title, 3, Ui.GOLD_RAMP);
    }

    /** Every binding drawn as key caps beside what it does. Read-only. */
    static final class ControlsSheet extends JComponent {
        private static final String[][] ROWS = {
            {"W A S D", "Move  (arrow keys too)"}, {"LMB", "Attack  ·  blade: click combo, hold to charge"},
            {"RMB", "Tide Wave  ·  blade: Ichor Crescent"}, {"SPACE", "Dash"}, {"Q", "Transform into the blade"},
            {"F", "Riposte"}, {"E", "Interact  ·  talk  ·  absorb"}, {"H", "Rest and save at camp"},
            {"U", "Open camp services"}, {"1 2 3 4", "Buy upgrades while camp is open"}, {"TAB", "Hold for the region map"}, {"ESC", "Pause menu"},
            {"ENTER", "Next dialogue line (E / click too)"}, {"X", "Skip conversation"},
            {"M", "Mute"}, {"V", "Reduce flashes and shake"}};

        ControlsSheet() {
            setOpaque(false);
            setPreferredSize(new Dimension(440, ROWS.length * 38 + 8));
            getAccessibleContext().setAccessibleName("Controls");
            StringBuilder text = new StringBuilder();
            for (String[] row : ROWS) text.append(row[0]).append(": ").append(row[1]).append(". ");
            getAccessibleContext().setAccessibleDescription(text.toString());
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            int y = 4;
            for (String[] row : ROWS) {
                int x = 0;
                for (String key : row[0].split(" ")) {
                    if (key.equals("LMB") || key.equals("RMB")) {
                        Ui.piece(g, key.equals("LMB") ? Ui.Piece.MOUSE_L : Ui.Piece.MOUSE_R, x, y);
                        x += 26;
                    } else x += Ui.keycap(g, key, x, y) + 4;
                }
                Ui.text(g, row[1], 150, y + 19, 2, Ui.TEXT);
                y += 38;
            }
            g.dispose();
        }
    }
}
