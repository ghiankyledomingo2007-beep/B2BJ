import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

/** Headless EDT checks exercise the same action and mouse callbacks used by the desktop. */
public final class UiAuditTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            boolean muted = GameAudio.muted();
            GameAudio.setMuted(true);
            try {
                List<String> failures = new ArrayList<>();
                check("new journey must discard prior transformation", UiAuditTest::newJourneyClearsVisualState, failures);
                check("invalid frame duration must not corrupt rendering", UiAuditTest::invalidFrameDoesNotCorruptRendering, failures);
                check("dying enemies must remain visible until fade completes", UiAuditTest::enemyDeathRemainsVisible, failures);
                check("transformation must not resume an old facing", UiAuditTest::transformationUsesCurrentFacing, failures);
                assert failures.isEmpty() : String.join("; ", failures);
            } finally {
                GameAudio.setMuted(muted);
            }
        });
        System.out.println("UiAuditTest passed");
    }

    private static void newJourneyClearsVisualState() {
        B2BJ panel = startedPanel();
        panel.game().player().collectIchor(100);
        press(panel, "transform");
        panel.step(.01);
        assert transformation(panel).active() : "fixture must start the transformation effect";
        press(panel, "pause");
        press(panel, "title");
        assert panel.game().story().phase() == OutpostStory.Phase.PROLOGUE;
        press(panel, "newCampaign");
        panel.step(.01);
        assert !panel.game().player().bladeForm();
        assert !transformation(panel).active()
                : "new slime retained the previous journey's transformation overlay";
    }

    private static void invalidFrameDoesNotCorruptRendering() {
        B2BJ panel = startedPanel();
        for (double seconds : new double[] {-2, Double.NaN, Double.POSITIVE_INFINITY})
            panel.step(seconds);
        panel.step(.01);
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        try {
            var field = B2BJ.class.getDeclaredField("bannerTime");
            field.setAccessible(true);
            assert Double.isFinite(field.getDouble(panel)) : "invalid duration poisoned the banner clock";
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void enemyDeathRemainsVisible() {
        Wisp enemy = new Wisp(800, 384, 744, 856);
        var game = new RuinedOutpostGame(new RuinedOutpostMap(), new Player(600, 384),
                List.of(enemy), new Guardian(1_000, 384));
        B2BJ panel = new B2BJ(false, game);
        panel.setSize(1280, 720);
        press(panel, "begin");
        assert enemy.hurt(enemy.maxHealth());
        assert !enemy.alive() && enemy.visible();
        int[] dying = pixels(panel);
        enemy.update(Wisp.DEATH_DURATION + .01);
        assert !enemy.visible();
        assert !Arrays.equals(dying, pixels(panel))
                : "drawWorld removed the enemy before its model's death fade could render";
    }

    private static int[] pixels(B2BJ panel) {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, 1280, 720, null, 0, 1280);
    }

    private static void transformationUsesCurrentFacing() {
        B2BJ panel = startedPanel();
        Player player = panel.game().player();
        player.collectIchor(100);
        press(panel, "transform");
        for (int tick = 0; tick < 17; tick++) panel.step(.05);
        player.spendBladeIchor(player.ichor() - .1);
        MouseEvent click = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0,
                900, 360, 1, false, MouseEvent.BUTTON1);
        for (var listener : panel.getMouseListeners()) listener.mousePressed(click);
        MouseEvent release = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0,
                900, 360, 1, false, MouseEvent.BUTTON1);
        for (var listener : panel.getMouseListeners()) listener.mouseReleased(release);
        panel.step(.05);
        assert !player.bladeForm() && player.recovering() : "slash must be interrupted by drain";
        for (int tick = 0; tick < 32; tick++) panel.step(.05);
        panel.getActionMap().get("leftPressed").actionPerformed(new ActionEvent(panel, 0, "left"));
        panel.step(.05);
        panel.getActionMap().get("leftReleased").actionPerformed(new ActionEvent(panel, 0, "left"));
        player.collectIchor(100);
        press(panel, "transform");
        panel.step(.01);
        assert player.bladeForm();
        double before = player.x();
        press(panel, "dash");
        panel.step(.02);
        assert player.x() < before
                : "left-facing slime transformed then dashed right using a prior blade slash's facing";
    }

    private static B2BJ startedPanel() {
        B2BJ panel = new B2BJ(false, RuinedOutpostGame.campaign());
        panel.setSize(1280, 720);
        press(panel, "begin");
        return panel;
    }

    private static TransformationAnimation transformation(B2BJ panel) {
        try {
            var field = B2BJ.class.getDeclaredField("transformationAnimation");
            field.setAccessible(true);
            return (TransformationAnimation) field.get(panel);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void press(B2BJ panel, String name) {
        for (String suffix : new String[] {"Pressed", "Released"})
            panel.getActionMap().get(name + suffix).actionPerformed(new ActionEvent(panel, 0, name));
    }

    private static void check(String name, Runnable scenario, List<String> failures) {
        try {
            scenario.run();
        } catch (AssertionError | RuntimeException failure) {
            failures.add(name + ": " + failure.getMessage());
        }
    }
}
