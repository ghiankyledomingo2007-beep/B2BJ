import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
    private double animationTime;

    public CampaignMenu(RuinedOutpostGame game, Runnable resume, Runnable title,
            Consumer<Boolean> effectsChanged, BooleanSupplier reducedEffects) {
        super(new BorderLayout(12, 12));
        this.game = Objects.requireNonNull(game, "game");
        this.resume = Objects.requireNonNull(resume, "resume");
        this.title = Objects.requireNonNull(title, "title");
        this.effectsChanged = Objects.requireNonNull(effectsChanged, "effectsChanged");
        this.reducedEffects = Objects.requireNonNull(reducedEffects, "reducedEffects");
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(118, 88, 128, 88));
        setPreferredSize(new Dimension(600, 680));
        getAccessibleContext().setAccessibleName("Game menu");
        home();
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g=(Graphics2D)graphics.create();
        StoneMenuArt.frame(g,getWidth(),getHeight(),animationTime,reducedEffects.getAsBoolean());
        g.dispose();
    }

    void animate(double seconds) {
        if(!Double.isFinite(seconds)||seconds<=0)return;
        animationTime+=Math.min(seconds,.1);
        for(var control:PixelSwing.controls(this)) {
            control.putClientProperty("menu.time",animationTime);
            control.putClientProperty("menu.reduced",reducedEffects.getAsBoolean());
        }
        repaint();
    }

    public void home() {
        removeAll();
        boolean ongoing = game.campaignMode() && game.player().alive() && !game.story().blocksGameplay();
        add(heading(ongoing && game.paused() ? "Paused" : "Menu"), BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        JLabel status = new JLabel(game.debugSession() ? "TEST SESSION — NORMAL SAVE PROTECTED"
                : game.paused() ? CampaignWorld.area(game.biome()).name() : "BLOB TO BLADE", SwingConstants.CENTER);
        content.add(status, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(5, 1, 0, 8));
        actions.add(PixelSwing.button("Resume", 'R', Ui.Icon.PLAY, resume, ongoing));
        actions.add(PixelSwing.button("Settings", 'S', Ui.Icon.GEAR, this::settings, true));
        actions.add(PixelSwing.button("Controls", 'C', Ui.Icon.KEYS, () -> page("Controls", new ControlsSheet()), true));
        actions.add(PixelSwing.button("Admin / Testing", 'A', Ui.Icon.FLASK, this::admin, game.debugAvailable()));
        actions.add(PixelSwing.button("Return to Title", 'T', Ui.Icon.DOOR, title, ongoing && game.paused()));
        for (Component action : actions.getComponents())
            ((JComponent)action).putClientProperty("menu.large", true);
        content.add(actions, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        styleMenu();
        status.setForeground(game.debugSession() ? Ui.AMBER : Ui.MUTED);
        revalidate();
        repaint();
    }

    /** Opens the Settings page (also the O key). */
    public void settings() { page("Settings", CampaignOptions.panel(effectsChanged, reducedEffects.getAsBoolean())); }

    /** Opens the Admin / Testing page (also F1); its controls stay disabled outside a live run. */
    public void admin() { page("Testing", DebugPanel.panel(game)); }

    /** Puts keyboard focus on the first enabled control, so arrows and Enter work at once. */
    public void focusFirst() {
        for (var c : PixelSwing.controls(this)) if (c.isEnabled() && c.isFocusable()
                && !(c instanceof JButton b && b.getText().equals("Close"))) { selectControl(c); c.requestFocusInWindow(); return; }
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
        styleMenu();
        revalidate();
        repaint();
        javax.swing.SwingUtilities.invokeLater(this::revalidate);   // second pass: wrapped text now knows its width
        focusFirst();
    }

    private JPanel heading(String text) {
        var label=new PixelSwing.Title(text.toUpperCase(java.util.Locale.ROOT),3,Ui.GOLD_RAMP);
        label.putClientProperty("menu.heading",true);
        var close=PixelSwing.button("Close",'X',null,resume,true);
        close.setPreferredSize(new Dimension(76,34));
        close.setToolTipText("Close menu (Esc)");
        var row=new JPanel(null) {
            @Override public void doLayout() {
                label.setBounds(0,0,getWidth(),52);
                close.setBounds(getWidth()-76,0,76,34);
            }
        };
        row.setPreferredSize(new Dimension(424,52));
        row.add(label);row.add(close);
        return row;
    }

    private void styleMenu() {
        PixelSwing.style(this);
        finishStyle(this);
        for (var control : PixelSwing.controls(this)) {
            control.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) { selectControl(control); }
            });
            if (control instanceof javax.swing.AbstractButton) control.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent event) {
                    if (control.isEnabled()) { selectControl(control); control.requestFocusInWindow(); }
                }
            });
        }
        focusFirst();
    }

    private void selectControl(JComponent selected) {
        for (var control : PixelSwing.controls(this)) if (control instanceof javax.swing.AbstractButton) {
            boolean was=Boolean.TRUE.equals(control.getClientProperty("menu.selected"));
            control.putClientProperty("menu.selected", control == selected);
            if(control==selected&&!was)control.putClientProperty("menu.selectedAt",animationTime);
            control.putClientProperty("menu.time",animationTime);
            control.putClientProperty("menu.reduced",reducedEffects.getAsBoolean());
            control.repaint();
        }
    }

    private static void finishStyle(Component component) {
        if (component instanceof PixelSwing.Button button) button.menuStyle = true;
        else if (component instanceof JButton button) button.putClientProperty("menu.row", true);
        if (component instanceof JComponent item) {
            boolean heading = Boolean.TRUE.equals(item.getClientProperty("menu.heading"));
            item.setFont(Ui.sans(2));
            if (heading) item.setForeground(new Color(0xe7d9b5));
        }
        if (component instanceof javax.swing.JComboBox || component instanceof javax.swing.JSlider) return;
        if (component instanceof Container parent)
            for (Component child : parent.getComponents()) finishStyle(child);
    }

    /** Every binding drawn as key caps beside what it does. Read-only. */
    static final class ControlsSheet extends JPanel {
        private static final String[][] ROWS = {
            {"W A S D", "Move (or arrow keys)"}, {"LMB", "Attack / hold to charge"},
            {"RMB", "Tide Wave / Ichor Crescent"}, {"SPACE", "Dash"}, {"Q", "Transform into the blade"},
            {"F", "Riposte"}, {"R", "Beelzebub / mass absorb"}, {"E", "Interact  ·  talk  ·  absorb"}, {"H", "Rest and save at camp"},
            {"U", "Open camp services"}, {"1 2 3 4", "Buy a camp upgrade"}, {"TAB", "Hold for the region map"}, {"ESC", "Pause menu"},
            {"ENTER", "Next line (or E / click)"}, {"X", "Skip conversation"},
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
                g.setColor(new Color(0x1b2b38));
                g.fillRect(0, y + 32, getWidth(), 1);
                Ui.text(g, row[1], 150, y + 19, 2, Ui.TEXT);
                y += 38;
            }
            g.dispose();
        }
    }
}
