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
        private boolean quiet;
        boolean titleSelected;
        boolean menuStyle;
        String subtitle = "";
        int titleChoice;
        double titleTime, titleFocus;
        double titleScale=1;
        boolean reducedTitleMotion;

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
            if (isPreferredSizeSet()) return super.getPreferredSize();
            int w = Ui.width(getText().toUpperCase(java.util.Locale.ROOT), 2) + (icon != null ? 92 : 64);
            return new Dimension(Math.max(160, w), 44);
        }

        boolean hot() { return isEnabled() && (getModel().isRollover() || isFocusOwner()); }

        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            if (menuStyle) {
                boolean down = getModel().isArmed() && getModel().isPressed();
                paintMenuRow(g, this, icon, down);
                g.dispose(); return;
            }
            if (quiet) {
                g.scale(titleScale,titleScale);
                double reveal=TitleScreen.entrance(titleTime,.45+titleChoice*.09,.8,reducedTitleMotion);
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)reveal));
                g.translate(0,(int)(9*(1-reveal)));
                boolean down = getModel().isArmed() && getModel().isPressed();
                boolean selected = isEnabled() && titleSelected;
                int height=(int)(getHeight()/titleScale);
                int width=(int)(getWidth()/titleScale);
                int baseline = subtitle.isEmpty() ? height/2 + 12 : 38;
                if(titleFocus>.01&&isEnabled()) {
                    var frame=(Graphics2D)g.create();
                    frame.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)(reveal*titleFocus)));
                    MenuTheme.row(frame,width,height,true,down);frame.dispose();
                }
                TitleIcons.draw(g,titleChoice,32+(down?1:0),height/2-22,44,selected,isEnabled());
                Ui.prose(g,getText(),96+(down?1:0),baseline,getFont(),
                        !isEnabled()?Ui.DIM:selected?new Color(0xfff6de):MenuTheme.IVORY);
                if(!subtitle.isEmpty())Ui.prose(g,subtitle,96,63,Ui.menuFont(13),isEnabled()?MenuTheme.MUTED:Ui.DIM);
                g.dispose(); return;
            }
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

    /** Shared heraldic rows preserve the Swing button's focus and enabled state. */
    private static void paintMenuRow(Graphics2D g, AbstractButton button, Ui.Icon icon, boolean down) {
        boolean hot = button.isEnabled() && (button.getClientProperty("menu.selected") instanceof Boolean selected
                ? selected : button.getModel().isRollover() || button.isFocusOwner());
        boolean large = Boolean.TRUE.equals(button.getClientProperty("menu.large"));
        double scale=button instanceof Button b?b.titleScale:1;
        g.scale(scale,scale);
        int width=(int)Math.round(button.getWidth()/scale),height=(int)Math.round(button.getHeight()/scale);
        MenuTheme.row(g,width,height,hot,down);
        int centerY=height/2+(down?1:0),size=large?36:20;
        int fontSize=large?24:button.getText().equals("Back")?22:button.getText().equals("Close")?13:17;
        var font=Ui.menuFont(fontSize).deriveFont(java.awt.Font.BOLD);
        var metrics=g.getFontMetrics(font);
        int gap=large?14:10;
        int x=large?30:(width-metrics.stringWidth(button.getText())-(icon==null?0:size+gap))/2;
        if(icon!=null) {
            TitleIcons.draw(g,icon,x,centerY-size/2,size,hot,button.isEnabled());
            x+=size+gap;
        }
        Ui.prose(g,button.getText(),x,centerY+(metrics.getAscent()-metrics.getDescent())/2,font,
                !button.isEnabled()?Ui.DIM:MenuTheme.IVORY);

    }

    static JButton button(String text, char mnemonic, Ui.Icon icon, Runnable action, boolean enabled) {
        var button = new Button(text, icon);
        button.setMnemonic(mnemonic);
        button.setEnabled(enabled);
        button.addActionListener(event -> action.run());
        return button;
    }

    static Button textButton(String text, char mnemonic, int scale, Runnable action) {
        var button = new Button(text,null);
        button.quiet = true;
        button.setFont(Ui.menuFont(11f*scale).deriveFont(java.awt.Font.BOLD));
        button.setMnemonic(mnemonic);
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
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            g.setColor(new Color(0x101f29));g.fillRect(x+2,y+2,18,18);
            g.setColor(hot?Ui.CYAN:MenuTheme.GOLD);g.drawRoundRect(x+2,y+2,18,18,4,4);
            if(on) {
                g.setColor(Ui.CYAN);
                g.fillPolygon(new int[]{x+11,x+16,x+11,x+6},new int[]{y+5,y+11,y+17,y+11},4);
            }
            g.dispose();
        }
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
    }

    /** Slider: an inset track, a cyan fill up to the knob, the value in pixel type at the right. */
    private static final class SliderUi extends BasicSliderUI {
        SliderUi(JSlider slider) { super(slider); }

        @Override protected Dimension getThumbSize() { return new Dimension(24, 26); }

        @Override public void paintTrack(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            Rectangle t = trackRect;
            int y=t.y+t.height/2,x=t.x,right=t.x+t.width;
            var edge=new java.awt.geom.Path2D.Double();edge.moveTo(x,y);edge.lineTo(x+6,y-6);
            edge.lineTo(right-6,y-6);edge.lineTo(right,y);edge.lineTo(right-6,y+6);edge.lineTo(x+6,y+6);edge.closePath();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x08111a));g.fill(edge);g.setColor(MenuTheme.GOLD);g.draw(edge);
            int fill=thumbRect.x+thumbRect.width/2-x-6;
            if(fill>0){g.setColor(new Color(0x51cfe0));g.fillRect(x+6,y-3,fill,6);
                g.setColor(new Color(0xb3f5ed));g.fillRect(x+6,y-3,fill,1);}
            g.dispose();
        }

        @Override public void paintThumb(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            boolean hot = slider.isFocusOwner() || isDragging();
            int x=thumbRect.x+thumbRect.width/2,y=thumbRect.y+thumbRect.height/2;
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(MenuTheme.GOLD);g.fillPolygon(new int[]{x,x+11,x,x-11},new int[]{y-12,y,y+12,y},4);
            g.setColor(new Color(0x101823));g.fillPolygon(new int[]{x,x+8,x,x-8},new int[]{y-9,y,y+9,y},4);
            g.setColor(hot?Ui.CYAN:MenuTheme.IVORY);g.fillPolygon(new int[]{x,x+5,x,x-5},new int[]{y-6,y,y+6,y},4);
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
            g.setColor(new Color(0x13252f));g.fillRect(r.x+4,r.y,Math.max(4,r.width-8),r.height);
            g.dispose();
        }
        @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            var g = (Graphics2D) graphics.create();
            Ui.crisp(g);
            g.setColor(MenuTheme.GOLD);g.fillRect(r.x+4,r.y,Math.max(4,r.width-8),r.height);
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
            MenuTheme.row(g,c.getWidth(),c.getHeight(),comboBox.isFocusOwner(),false);
            g.dispose();
            super.paint(graphics, c);
        }
    }

    /** List rows for combo boxes: pixel type on the panel colour, gold when selected. */
    private static final class Cell extends JLabel implements ListCellRenderer<Object> {
        private static final long serialVersionUID = 1L;
        Cell() { setOpaque(true); setFont(Ui.body(16)); setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12)); }
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            setText(String.valueOf(value));
            setBackground(selected && index >= 0 ? new Color(0x18343e) : index < 0 ? new Color(0, 0, 0, 0) : new Color(0x0b1721));
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
            if (Boolean.TRUE.equals(b.getClientProperty("menu.row"))) {
                paintMenuRow(g, b, null, down); g.dispose(); return;
            }
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
