import java.awt.Window;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Native startup check; stays on the title and never changes the player's save. */
public final class WindowLaunchSmoke {
    public static void main(String[] args) throws Exception {
        int status = 0;
        try {
            B2BJ.main(args);
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = Arrays.stream(Window.getWindows())
                        .filter(window -> window instanceof JFrame && window.isShowing())
                        .map(JFrame.class::cast).findFirst().orElseThrow();
                boolean borderless = Arrays.asList(args).contains("--borderless");
                assert frame.isUndecorated() == borderless : "--borderless must bypass native decorations";
                assert frame.getContentPane().getComponent(0).getPreferredSize().equals(new java.awt.Dimension(1280, 720));
                // Actual decorated geometry is the desktop failure this fallback bypasses.
                if (borderless) {
                    assert frame.getContentPane().getWidth() == 1280;
                    assert frame.getContentPane().getHeight() == 720;
                }
                assert !frame.isResizable();
                assert frame.getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE;
            });
            System.out.println("WindowLaunchSmoke passed: " + Arrays.toString(args));
        } catch (Throwable failure) {
            status = 1;
            failure.printStackTrace();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) window.dispose();
            });
        }
        System.exit(status); // Stops only this test process's game timer.
    }
}
