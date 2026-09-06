import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class B2BJIntegrationTest {
    public static void main(String[] args) {
        rendersStoryAndPlayableHudHeadlessly();
        controlsFreezeAndReleaseReliably();
        System.out.println("B2BJIntegrationTest passed");
    }

    private static void rendersStoryAndPlayableHudHeadlessly() {
        B2BJ panel = new B2BJ(false);
        panel.setSize(panel.getPreferredSize());

        BufferedImage prologue = render(panel);
        assert panel.game().story().phase() == OutpostStory.Phase.PROLOGUE;

        press(panel, "begin");
        BufferedImage playing = render(panel);
        assert panel.game().story().phase() == OutpostStory.Phase.HUNT;
        assert !Arrays.equals(pixels(prologue), pixels(playing))
                : "story overlay and gameplay HUD must render differently";

        panel.game().player().hurt(2);
        BufferedImage damaged = render(panel);
        assert !Arrays.equals(pixels(playing), pixels(damaged))
                : "player health HUD must visibly update after damage";

        press(panel, "mute");
        assert GameAudio.muted() : "M must mute sound";
        press(panel, "mute");
        assert !GameAudio.muted() : "M must restore sound";

        panel.game().update(0.4, 0, 0);
        panel.game().update(0.4, 0, 0);
        panel.game().player().hurt(Player.MAX_HEALTH);
        panel.game().update(0.01, 0, 0);
        BufferedImage death = render(panel);
        assert panel.game().story().phase() == OutpostStory.Phase.DEAD;
        assert !Arrays.equals(pixels(damaged), pixels(death))
                : "death overlay must replace active play";

        press(panel, "restart");
        assert panel.game().story().phase() == OutpostStory.Phase.PROLOGUE;
        assert panel.game().player().health() == Player.MAX_HEALTH;

        panel.game().story().begin();
        for (int scout = 0; scout < OutpostStory.SCOUT_TOTAL; scout++) {
            panel.game().story().scoutDefeated();
        }
        panel.game().story().enterGatehouse();
        BufferedImage boss = render(panel);
        assert panel.game().story().phase() == OutpostStory.Phase.GUARDIAN;
        panel.game().story().guardianDefeated();
        panel.game().story().escaped();
        BufferedImage complete = render(panel);
        assert panel.game().story().phase() == OutpostStory.Phase.COMPLETE;
        assert !Arrays.equals(pixels(boss), pixels(complete))
                : "victory overlay must replace the boss HUD";
    }

    private static BufferedImage render(B2BJ panel) {
        BufferedImage image = new BufferedImage(
                panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    private static void press(B2BJ panel, String name) {
        panel.getActionMap().get(name + "Pressed").actionPerformed(
                new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, name));
        panel.getActionMap().get(name + "Released").actionPerformed(
                new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, name));
    }

    private static void controlsFreezeAndReleaseReliably() {
        GameAudio.setMuted(true);
        B2BJ panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());press(panel,"begin");
        press(panel,"pause");
        double x=panel.game().player().x();
        panel.getActionMap().get("rightPressed").actionPerformed(new ActionEvent(panel,0,""));
        panel.step(0.2);
        assert panel.game().player().x()==x : "pause must freeze simulation";
        press(panel,"pause");
        panel.getActionMap().get("rightArrowPressed").actionPerformed(new ActionEvent(panel,0,""));
        panel.getActionMap().get("rightReleased").actionPerformed(new ActionEvent(panel,0,""));
        panel.step(0.1);
        assert panel.game().player().x()>x : "releasing D must not cancel a held right arrow";
        panel.getActionMap().get("mapPressed").actionPerformed(new ActionEvent(panel,0,""));
        x=panel.game().player().x();panel.step(0.1);
        assert panel.game().player().x()>x : "map overlay must not pause";
        for(var listener:panel.getFocusListeners()) listener.focusLost(
                new java.awt.event.FocusEvent(panel,java.awt.event.FocusEvent.FOCUS_LOST));
        assert panel.game().paused() : "focus loss must pause";
        press(panel,"pause");x=panel.game().player().x();panel.step(0.1);
        assert panel.game().player().x()==x : "focus loss must clear held keys";
        GameAudio.setMuted(false);
    }
}
