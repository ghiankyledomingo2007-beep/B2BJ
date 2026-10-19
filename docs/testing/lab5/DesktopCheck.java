import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class DesktopCheck {
    private static B2BJ panel;
    private static JFrame window;
    private static Rectangle bounds;

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        try {
            SwingUtilities.invokeAndWait(() -> {
                panel = B2BJ.lab5(false);
                window = new JFrame("B2BJ Lab 5 verification");
                window.setUndecorated(true);
                window.add(panel); window.pack(); window.setLocationRelativeTo(null);
                window.setVisible(true); window.toFront(); panel.requestFocusInWindow();
            });
            Robot robot = new Robot(); robot.setAutoDelay(100); robot.delay(700); robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                assert panel.isFocusOwner() : "test window must own keyboard focus";
                bounds = new Rectangle(panel.getLocationOnScreen(), panel.getSize());
                assert !panel.checkCollision();
            });
            var before = robot.createScreenCapture(bounds);
            assert before.getRGB(592, 312) == Color.RED.getRGB();
            ImageIO.write(before, "png", output.resolve("04-desktop-no-collision.png").toFile());
            key(robot, KeyEvent.VK_F2);
            assert robot.createScreenCapture(bounds).getRGB(592, 312) != Color.RED.getRGB() : "physical F2 hides overlay";
            key(robot, KeyEvent.VK_F2);
            assert robot.createScreenCapture(bounds).getRGB(592, 312) == Color.RED.getRGB() : "physical F2 restores overlay";
            double[] start = new double[1];
            SwingUtilities.invokeAndWait(() -> start[0] = panel.game().player().x());
            robot.keyPress(KeyEvent.VK_D); robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                panel.step(.1);
                assert panel.game().player().x() > start[0] : "physical D moves the player";
            });
            robot.keyRelease(KeyEvent.VK_D); robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                panel = B2BJ.lab5(false);
                window.setContentPane(panel); window.validate();
                var enemy = panel.game().scouts().get(0);
                panel.game().player().relocate(enemy.x() - 60, enemy.y()); panel.repaint();
            });
            robot.delay(400); robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> { assert panel.checkCollision(); });
            ImageIO.write(robot.createScreenCapture(bounds), "png", output.resolve("05-desktop-collision.png").toFile());
            System.out.println("Desktop check passed: physical F2, physical D, both collision states, native captures");
        } finally {
            if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
        }
        System.exit(0);
    }

    private static void key(Robot robot, int key) throws Exception {
        robot.keyPress(key); robot.keyRelease(key); robot.waitForIdle();
        SwingUtilities.invokeAndWait(() -> panel.repaint());
        robot.delay(150); robot.waitForIdle();
    }
}
