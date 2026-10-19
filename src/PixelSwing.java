import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Pixel-art skins for the Swing controls the menus are built from. The controls stay real Swing
 * components (keyboard focus, mnemonics, screen-reader names, tests) and only their painting changes.
 */
final class PixelSwing {
    private PixelSwing() { }

    /** A menu button: kit frame, pixel label, a sword cursor while focused or hovered. */
    static final class Button extends JButton {
        private static final long serialVersionUID = 1L;
        private final Ui.Icon icon;

        Button(String text, Ui.Icon icon) {
            super(text);
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Ui.sans(2));
            setForeground(Ui.TEXT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override public Dimension getPreferredSize() {
            int w = Ui.width(getText().toUpperCase(java.util.Locale.ROOT), 2) + (icon != null ? 92 : 64);
            return new Dimension(Math.max(160, w), 44);
        }

        boolean hot() { return isEnabled() && (getModel().isRollover() || isFocusOwner()); }

        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            ButtonModel m = getModel();
            boolean down = m.isArmed() && m.isPressed();
            Ui.Piece frame = !isEnabled() ? Ui.Piece.BUTTON_OFF : down ? Ui.Piece.BUTTON_DOWN
                    : hot() ? Ui.Piece.BUTTON_HOT : Ui.Piece.BUTTON;
            Ui.nine(g, frame, 0, 0, getWidth(), getHeight());
            int nudge = down ? Ui.S : 0;
            String label = getText().toUpperCase(java.util.Locale.ROOT);
            int textW = Ui.width(label, 2);
            int iconW = icon != null ? 32 : 0;
            int x = (getWidth() - textW - iconW) / 2 / Ui.S * Ui.S;
            int baseline = (getHeight() / 2 + 7) / Ui.S * Ui.S + nudge;
            if (icon != null) {
                var c = (Graphics2D) g.create();
                if (!isEnabled()) c.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, .35f));
                Ui.icon(c, icon, x, (getHeight() - 24) / 2 / Ui.S * Ui.S + nudge);
                c.dispose();
                x += iconW;
            }
            Color color = !isEnabled() ? Ui.DIM : hot() ? Ui.GOLD_LIGHT : Ui.TEXT;
            Ui.text(g, label, x, baseline, 2, color);
            if (hot()) {
                boolean glint = System.currentTimeMillis() / 450 % 2 == 0;
                int bob = System.currentTimeMillis() / 300 % 2 == 0 ? 0 : Ui.S;
                Ui.piece(g, glint ? Ui.Piece.CURSOR : Ui.Piece.CURSOR_2, 10 + bob, (getHeight() - 18) / 2 / Ui.S * Ui.S);
            }
            g.dispose();
        }
    }

    static JButton button(String text, char mnemonic, Ui.Icon icon, Runnable action, boolean enabled) {
        var button = new Button(text, icon);
        button.setMnemonic(mnemonic);
        button.setEnabled(enabled);
        button.addActionListener(event -> action.run());
        return button;
    }

    /** A title label painted in the display font with a gold ramp. */
    static final class Title extends JLabel {
        private static final long serialVersionUID = 1L;
        private final int scale;
        private final int[] ramp;

        Title(String text, int scale, int[] ramp) {
            super(text, CENTER);
            this.scale = scale;
            this.ramp = ramp;
            setOpaque(false);
            setFont(Ui.display(scale));
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(Ui.titleWidth(getText(), scale) + 8, (11 + 6) * scale + 10);
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.drawTitleCentered(g, getText(), getWidth() / 2, 4, scale, ramp);
            g.dispose();
        }
    }

    /** The check box glyph from the kit, following the box's state. */
    private static final class CheckIcon implements Icon {
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            var box = (AbstractButton) c;
            boolean hot = box.getModel().isRollover() || box.isFocusOwner();
            boolean on = box.isSelected();
            Ui.Piece piece = on ? (hot ? Ui.Piece.CHECK_HOT_ON : Ui.Piece.CHECK_ON) : (hot ? Ui.Piece.CHECK_HOT : Ui.Piece.CHECK);
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Ui.piece(g, piece, x, y);
            g.dispose();
        }
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
    }

    /** Slider: an inset track, a cyan fill up to the knob, the value in pixel type at the right. */
    private static final class SliderUi extends BasicSliderUI {
        SliderUi(JSlider slider) { super(slider); }

        @Override protected Dimension getThumbSize() { return new Dimension(14, 22); }

        @Override public void paintTrack(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Rectangle t = trackRect;
            int y = t.y + t.height / 2 - 6;
            Ui.nine(g, Ui.Piece.TRACK, t.x, y, t.width, 12);
            int fill = thumbRect.x + thumbRect.width / 2 - t.x;
            if (fill > 4) Ui.nine(g, Ui.Piece.TRACK_FILL, t.x, y, fill, 12);
            g.dispose();
        }

        @Override public void paintThumb(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            boolean hot = slider.isFocusOwner() || isDragging();
            Ui.piece(g, hot ? Ui.Piece.KNOB_HOT : Ui.Piece.KNOB, thumbRect.x, thumbRect.y + (thumbRect.height - 22) / 2);
            g.dispose();
        }

        @Override public void paintFocus(Graphics graphics) { }
        @Override public void paintTicks(Graphics graphics) { }
        @Override public void paintLabels(Graphics graphics) { }
    }

    private static final class ScrollUi extends BasicScrollBarUI {
        @Override protected void paintTrack(Graphics graphics, JComponent c, Rectangle r) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Ui.nine(g, Ui.Piece.INSET, r.x + 2, r.y, Math.max(8, r.width - 4), r.height);
            g.dispose();
        }
        @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Ui.nine(g, Ui.Piece.SCROLL_THUMB, r.x + 2, r.y, Math.max(8, r.width - 4), r.height);
            g.dispose();
        }
        @Override protected JButton createDecreaseButton(int orientation) { return zero(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zero(); }
        private static JButton zero() {
            var b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
    }

    private static final class ComboUi extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            var arrow = new JButton() {
                private static final long serialVersionUID = 1L;
                @Override protected void paintComponent(Graphics graphics) {
                    var g = (Graphics2D) graphics.create();
                    Ui.crisp(g);
                    int cx = getWidth() / 2 / Ui.S * Ui.S, cy = getHeight() / 2 / Ui.S * Ui.S - 2;
                    g.setColor(Ui.GOLD);
                    for (int i = 0; i < 4; i++) g.fillRect(cx - (3 - i) * Ui.S, cy + i * Ui.S, (7 - 2 * i) * Ui.S, Ui.S);
                    g.dispose();
                }
            };
            arrow.setContentAreaFilled(false);
            arrow.setBorderPainted(false);
            arrow.setFocusable(false);
            arrow.setOpaque(false);
            return arrow;
        }
        @Override public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean focus) { }
        @Override public void paint(Graphics graphics, JComponent c) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Ui.nine(g, comboBox.isFocusOwner() ? Ui.Piece.BUTTON_HOT : Ui.Piece.BUTTON, 0, 0, c.getWidth(), c.getHeight());
            g.dispose();
            super.paint(graphics, c);
        }
    }

    /** List rows for combo boxes: pixel type on the panel colour, gold when selected. */
    private static final class Cell extends JLabel implements ListCellRenderer<Object> {
        private static final long serialVersionUID = 1L;
        Cell() { setOpaque(true); setFont(Ui.sans(2)); setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12)); }
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            setText(String.valueOf(value));
            setBackground(selected && index >= 0 ? new Color(0x252037) : index < 0 ? new Color(0, 0, 0, 0) : Ui.PANEL);
            setOpaque(index >= 0);
            setForeground(selected ? Ui.GOLD_LIGHT : Ui.TEXT);
            return this;
        }
    }

    /** The focusable controls under `root`, in layout order. */
    static java.util.List<JComponent> controls(Container root) {
        var out = new java.util.ArrayList<JComponent>();
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton || c instanceof JSlider || c instanceof JComboBox) out.add((JComponent) c);
            else if (c instanceof Container k) out.addAll(controls(k));
        }
        return out;
    }

    /** Paints any plain JButton (the admin page's actions) as a kit button. */
    private static final class ButtonSkin extends javax.swing.plaf.basic.BasicButtonUI {
        @Override public void paint(Graphics graphics, JComponent c) {
            var b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            boolean down = m.isArmed() && m.isPressed(), hot = b.isEnabled() && (m.isRollover() || b.isFocusOwner());
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Ui.nine(g, !b.isEnabled() ? Ui.Piece.BUTTON_OFF : down ? Ui.Piece.BUTTON_DOWN : hot ? Ui.Piece.BUTTON_HOT : Ui.Piece.BUTTON,
                    0, 0, c.getWidth(), c.getHeight());
            String label = b.getText();
            Ui.text(g, label, (c.getWidth() - Ui.width(label, 2)) / 2 / Ui.S * Ui.S, (c.getHeight() / 2 + 7) / Ui.S * Ui.S + (down ? Ui.S : 0),
                    2, !b.isEnabled() ? Ui.DIM : hot ? Ui.GOLD_LIGHT : Ui.TEXT);
            g.dispose();
        }
        @Override public Dimension getPreferredSize(JComponent c) {
            return new Dimension(Ui.width(((AbstractButton) c).getText(), 2) + 40, 40);
        }
    }

    /** Lets a scroll pane's content wrap to the pane's width instead of running off its right edge. */
    static final class Fit extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;
        Fit(JComponent content) { super(new java.awt.BorderLayout()); setOpaque(false); add(content); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(20, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        /** Wrapped text areas only know their height once they know their width: give it to them first. */
        @Override public Dimension getPreferredSize() {
            if (getParent() instanceof javax.swing.JViewport view && view.getWidth() > 0) prime(this, view.getWidth());
            return super.getPreferredSize();
        }

        private void prime(Container c, int width) {
            var in = c.getInsets();
            int inner = width - in.left - in.right;
            for (Component k : c.getComponents()) {
                if (k instanceof JTextArea area && area.getLineWrap()) {
                    int w = Math.max(60, inner);
                    if (area.getWidth() == w) continue;
                    area.setSize(new Dimension(w, Short.MAX_VALUE));
                    // layouts cache child sizes; clear every cache between this text and the scroll view
                    for (Container up = area.getParent(); up != null && up != getParent(); up = up.getParent()) up.invalidate();
                } else if (k instanceof Container kc) prime(kc, inner);
            }
        }
    }

    /** Set on a label or control to keep a colour through {@link #style}. */
    static final String COLOR = "pixel.color";

    /** Re-skins every control under `root`; safe to call again after a page rebuild. */
    static void style(Component root) {
        if (root instanceof Button || root instanceof Title) return;
        if (root instanceof JComponent j) {
            j.setOpaque(false);
            j.setFont(Ui.sans(2));
            j.setForeground(Ui.TEXT);
            if (j.getParent() instanceof JComponent parent && parent.getLayout() instanceof javax.swing.BoxLayout)
                j.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        if (root instanceof AbstractButton b) b.setDisplayedMnemonicIndex(-1);
        if (root instanceof JCheckBox box) {
            var icon = new CheckIcon();
            box.setIcon(icon);
            box.setSelectedIcon(icon);
            box.setRolloverIcon(icon);
            box.setRolloverSelectedIcon(icon);
            box.setPressedIcon(icon);
            box.setIconTextGap(12);
            box.setFocusPainted(false);
            box.setRolloverEnabled(true);
            box.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        } else if (root instanceof JSlider slider) {
            slider.setUI(new SliderUi(slider));
            slider.setPaintTicks(false);
            slider.setPaintLabels(false);
            slider.setPreferredSize(new Dimension(320, 30));
            slider.setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 0));
        } else if (root instanceof JButton button) {
            button.setUI(new ButtonSkin());
            button.setRolloverEnabled(true);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        } else if (root instanceof JComboBox<?> combo) {
            combo.setUI(new ComboUi());
            @SuppressWarnings("unchecked") var raw = (JComboBox<Object>) combo;
            raw.setRenderer(new Cell());
            combo.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            combo.setPreferredSize(new Dimension(320, 40));
            combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        } else if (root instanceof JScrollPane scroll) {
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setOpaque(false);
            scroll.setViewportBorder(null);
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setUI(new ScrollUi());
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(14, 0));
        } else if (root instanceof JTextArea area) {
            area.setForeground(Ui.MUTED);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(BorderFactory.createEmptyBorder(6, 0, 10, 0));
        } else if (root instanceof JLabel label) {
            label.setForeground(Ui.MUTED);
            label.setDisplayedMnemonicIndex(-1);
        }
        if (root instanceof JComponent j && j.getClientProperty(COLOR) instanceof Color keep) j.setForeground(keep);
        if (root instanceof JPanel panel) panel.setBackground(new Color(0, 0, 0, 0));
        if (root instanceof JComboBox || root instanceof JScrollBar || root instanceof JSlider) return;
        if (root instanceof Container c) for (Component child : c.getComponents()) style(child);
    }
}
