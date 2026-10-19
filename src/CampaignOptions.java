import java.awt.Component;
import java.awt.Font;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;

/** Native, keyboard-accessible options. Settings last for this game session only. */
public final class CampaignOptions {
    private CampaignOptions() {}

    public static void show(Component parent, Runnable beforeOpen,
            Consumer<Boolean> reducedEffectsChanged, boolean reducedEffects) {
        JPanel content = panel(reducedEffectsChanged, reducedEffects);
        Objects.requireNonNull(beforeOpen, "beforeOpen").run();
        JOptionPane.showMessageDialog(parent, content, "Blob to Blade / Options", JOptionPane.PLAIN_MESSAGE);
    }

    static JPanel panel(Consumer<Boolean> reducedEffectsChanged, boolean reducedEffects) {
        Objects.requireNonNull(reducedEffectsChanged, "reducedEffectsChanged");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        volume(panel, "Master volume", 'M', "masterVolume", GameAudio.masterVolume(), GameAudio::setMasterVolume);
        volume(panel, "Sound effects", 'S', "sfxVolume", GameAudio.sfxVolume(), GameAudio::setSfxVolume);
        volume(panel, "Ambient music", 'U', "musicVolume", GameAudio.musicVolume(), GameAudio::setMusicVolume);
        JCheckBox effects = new JCheckBox("Reduce flashes, weather, shake", reducedEffects);
        effects.setMnemonic('R');
        effects.addActionListener(event -> reducedEffectsChanged.accept(effects.isSelected()));
        panel.add(effects);
        JCheckBox mute = new JCheckBox("Mute all audio", GameAudio.muted());
        mute.setMnemonic('A');
        mute.addActionListener(event -> GameAudio.setMuted(mute.isSelected()));
        panel.add(mute);
        JTextArea help = new JTextArea("WASD / arrows move, Space dashes; Menu > Controls lists every key. "
                + "Music at 0 turns ambience off. Changes apply now and last for this session.");
        help.setEditable(false);
        help.setOpaque(false);
        help.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setColumns(36);
        help.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        help.getAccessibleContext().setAccessibleName("Game controls and settings help");
        panel.add(help);
        return panel;
    }

    private static void volume(JPanel panel, String text, char mnemonic, String name,
            double initial, DoubleConsumer changed) {
        JSlider slider = new JSlider(0, 100, (int) Math.round(initial * 100));
        slider.setName(name);
        slider.setMajorTickSpacing(25);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.addChangeListener(event -> changed.accept(slider.getValue() / 100.0));
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(slider);
        label.putClientProperty("pixel.color", new java.awt.Color(0xece2c8));
        JLabel value = new JLabel(slider.getValue() + "%");
        value.putClientProperty("pixel.color", new java.awt.Color(0xeeb840));
        slider.addChangeListener(event -> value.setText(slider.getValue() + "%"));
        slider.getAccessibleContext().setAccessibleName(text);
        JPanel head = new JPanel(new java.awt.BorderLayout());
        head.add(label, java.awt.BorderLayout.WEST);
        head.add(value, java.awt.BorderLayout.EAST);
        head.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        panel.add(head);
        panel.add(slider);
    }
}
