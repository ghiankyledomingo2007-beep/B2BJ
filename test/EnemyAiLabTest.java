import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class EnemyAiLabTest {
    public static void main(String[] args) throws Exception {
        var lab = new EnemyAiLab(false);
        lab.setSize(lab.getPreferredSize());
        var original = lab.enemyBounds();
        lab.step(1);
        assert lab.enemyBounds().equals(original) : "outside 300px must stay still, not patrol";
        assert lab.state().equals("OUTSIDE RANGE");
        assert !lab.checkCollision();
        save(lab, args, "01-outside-range.png");

        lab.reset(2);
        double x = lab.enemyBounds().getX();
        lab.step(.5);
        assert close(lab.enemyBounds().getX() - x, 40) : "0.08px/ms equals 80px/s";
        assert close(lab.distance(), 200);
        assert close(lab.enemySpeed(), 80);
        save(lab, args, "02-chasing.png");
        lab.step(10);
        assert close(lab.distance(), 80) : "large frame must stop without overshoot";
        var stopped = lab.enemyBounds();
        lab.step(1);
        assert lab.enemyBounds().equals(stopped);
        assert lab.state().equals("STOPPED NEAR PLAYER");
        assert !lab.checkCollision() : "rectangles touching at 80px horizontally have no overlapping area";
        save(lab, args, "03-stopped-near-player.png");

        lab.reset(2);
        lab.press(KeyEvent.VK_SPACE);
        lab.press(KeyEvent.VK_SPACE);
        lab.press(KeyEvent.VK_D);
        var before = lab.playerBounds();
        lab.step(1);
        assert lab.playerBounds().equals(before) && lab.paused() : "pause and key repeat";
        lab.release(KeyEvent.VK_SPACE);
        lab.press(KeyEvent.VK_SPACE);
        lab.step(1);
        assert lab.state().equals("OUTSIDE RANGE") && lab.enemySpeed() == 0 : "escaping detection stops immediately";
        lab.release(KeyEvent.VK_D);

        lab.reset(3);
        lab.press(KeyEvent.VK_A);
        lab.step(80.0 / 180);
        lab.release(KeyEvent.VK_A);
        assert close(lab.distance(), 0) && lab.enemySpeed() == 0 : "coincident centers never divide by zero";
        assert lab.checkCollision() : "overlapping sprite rectangles show collision";
        lab.step(1);
        assert Double.isFinite(lab.enemyBounds().getX());

        // Move exactly to the detection boundary using the same controls as the desktop demo.
        lab.reset(1);
        lab.press(KeyEvent.VK_A);
        lab.step(120.0 / 180);
        lab.release(KeyEvent.VK_A);
        assert lab.distance() < 300 && lab.enemySpeed() > 0 : "exactly 300px begins chasing";

        lab.reset(2);
        lab.press(KeyEvent.VK_W);
        lab.step(.1);
        lab.release(KeyEvent.VK_W);
        double ex = lab.enemyBounds().getX(), ey = lab.enemyBounds().getY();
        lab.step(.25);
        assert close(Math.hypot(lab.enemyBounds().getX() - ex, lab.enemyBounds().getY() - ey), 20)
                : "diagonal motion has same speed";
        assert lab.enemyBounds().getY() < ey : "enemy follows changing player center in both axes";
        for (double angle : new double[]{0, .27, Math.PI / 4, Math.PI / 2, Math.PI, Math.PI * 1.5, Math.PI * 1.75}) {
            lab.reset(2);
            set(lab, "playerX", lab.enemyBounds().getX() + Math.cos(angle) * 180);
            set(lab, "playerY", lab.enemyBounds().getY() + Math.sin(angle) * 180);
            lab.step(.01);
            assert close(lab.enemySpeed(), 80);
            lab.step(10);
            assert close(lab.distance(), 80) && lab.enemySpeed() == 0 : "stop in every direction";
        }
        var whole = new EnemyAiLab(false);
        whole.reset(2); whole.step(1);
        var divided = new EnemyAiLab(false);
        divided.reset(2);
        for (int i = 0; i < 60; i++) divided.step(1.0 / 60);
        assert close(whole.distance(), divided.distance()) : "speed independent of frame rate";
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var bounds = lab.enemyBounds();
            lab.step(invalid);
            assert lab.enemyBounds().equals(bounds);
        }
        var snapshot = lab.playerBounds();
        snapshot.setRect(0, 0, 1, 1);
        assert lab.playerBounds().getWidth() == 80 : "bounds are defensive copies";

        for (var listener : lab.getFocusListeners())
            listener.focusLost(new FocusEvent(lab, FocusEvent.FOCUS_LOST));
        assert lab.paused() : "switching windows pauses the demonstration";
        lab.press(KeyEvent.VK_1);
        assert !lab.paused() && close(lab.distance(), 420);
        lab.press(KeyEvent.VK_2);
        assert close(lab.distance(), 240);
        lab.press(KeyEvent.VK_3);
        assert close(lab.distance(), 80);
        System.out.println("EnemyAiLabTest passed: thresholds, speed, direction, overshoot, pause, input, rendering");
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 1e-7; }

    private static void set(EnemyAiLab lab, String name, double value) throws Exception {
        var field = EnemyAiLab.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setDouble(lab, value);
    }

    private static void save(EnemyAiLab lab, String[] args, String name) throws Exception {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        lab.paint(g);
        g.dispose();
        assert image.getRGB((int) lab.playerBounds().getCenterX(),
                (int) lab.playerBounds().getCenterY()) != image.getRGB(0, 0) : "player sprite rendered";
        if (args.length == 0) return;
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        ImageIO.write(image, "png", output.resolve(name).toFile());
    }
}
