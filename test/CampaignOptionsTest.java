import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class CampaignOptionsTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(CampaignOptionsTest::controlsApplyWithoutPreferencesOrAudioHardware);
        System.out.println("CampaignOptionsTest passed");
    }

    private static void controlsApplyWithoutPreferencesOrAudioHardware() {
        boolean[] reduced = {false};
        JPanel panel;
        try {
            var options = Class.forName("CampaignOptions");
            var method = options.getDeclaredMethod("panel", java.util.function.Consumer.class, boolean.class);
            java.util.function.Consumer<Boolean> update = value -> reduced[0] = value;
            panel = (JPanel) method.invoke(null, update, true);
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("Campaign options panel missing", missing);
        }
        List<Component> controls = descendants(panel);
        List<JSlider> sliders = controls.stream().filter(JSlider.class::isInstance).map(JSlider.class::cast).toList();
        assert sliders.size() == 3 : "master, sound effects and music need native volume sliders";
        for (JSlider slider : sliders) {
            assert slider.getMinimum() == 0 && slider.getMaximum() == 100;
            assert controls.stream().anyMatch(component -> component instanceof JLabel label && label.getLabelFor() == slider)
                    : "each slider needs accessible label association";
            slider.setValue(0);
        }
        assert volume("masterVolume") == 0 && volume("sfxVolume") == 0 && volume("musicVolume") == 0;
        JCheckBox effects = controls.stream().filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast).findFirst().orElseThrow();
        assert effects.isSelected() : "panel must reflect existing reduced-effects setting";
        effects.doClick();
        assert !reduced[0];
        effects.doClick();
        assert reduced[0] : "checkbox must update game effects setting";
        String help = controls.stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                .map(JTextArea::getText).reduce("", (a, b) -> a + b);
        assert help.contains("WASD") && help.contains("Space") && help.contains("session")
                : "options must explain controls and session-only settings";
        for (JSlider slider : sliders) slider.setValue(slider.getName().equals("musicVolume") ? 25 : 100);
    }

    private static List<Component> descendants(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            result.add(child);
            if (child instanceof Container container) result.addAll(descendants(container));
        }
        return result;
    }

    private static double volume(String getter) {
        try { return (double) GameAudio.class.getMethod(getter).invoke(null); }
        catch (ReflectiveOperationException failed) { throw new AssertionError(failed); }
    }
}
