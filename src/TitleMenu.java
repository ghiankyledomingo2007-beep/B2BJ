import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The title screen's menu: Continue, New Journey, Settings, Quit, as real buttons (mouse, keyboard,
 * screen readers). Starting over on top of a save asks once more before it replaces anything.
 */
@SuppressWarnings("serial")
final class TitleMenu extends JPanel {
    static final String NEW = "New Journey", CONFIRM = "Replace save?";
    private final JButton resume, fresh, settings, quit;
    private final java.util.function.BooleanSupplier saved;
    private final Runnable start;
    private final Timer cancel;

    TitleMenu(java.util.function.BooleanSupplier saved, Runnable resumeAction, Runnable start, Runnable settingsAction, Runnable quitAction) {
        super(new GridLayout(4, 1, 0, 4));
        this.saved = saved;
        this.start = start;
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Title menu");
        resume = PixelSwing.textButton("Continue", 'C', resumeAction);
        fresh = PixelSwing.textButton(NEW, 'N', this::freshPressed);
        settings = PixelSwing.textButton("Settings", 'S', settingsAction);
        quit = PixelSwing.textButton("Quit", 'Q', quitAction);
        for (var b : new JButton[]{resume, fresh, settings, quit}) add(b);
        cancel = new Timer(4000, event -> fresh.setText(NEW));
        cancel.setRepeats(false);
        refresh();
    }

    /** Continue only exists when there is a journey to continue. */
    void refresh() {
        resume.setEnabled(saved.getAsBoolean());
        if (!saved.getAsBoolean() && fresh.getText().equals(CONFIRM)) fresh.setText(NEW);
    }

    /** Keyboard focus to the natural first choice: Continue with a save, New Journey without. */
    void focusDefault() { (resume.isEnabled() ? resume : fresh).requestFocusInWindow(); }

    private void freshPressed() {
        if (!saved.getAsBoolean() || fresh.getText().equals(CONFIRM)) {
            cancel.stop();
            fresh.setText(NEW);
            start.run();
            return;
        }
        fresh.setText(CONFIRM);
        cancel.restart();
    }
}
