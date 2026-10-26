import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.SwingUtilities;

public final class MaraIdleTest {
    public static void main(String[] args) throws Exception {
        var original=B2BJ.loadImage("assets/characters/npcs/mara_idle.png");
        assert original.getWidth()==80&&original.getHeight()==80 : "pose masks use Mara's native 80px cutout";
        var neutral=DialogueRenderer.maraFrame(0,false);
        assert same(original,neutral) : "neutral pose must preserve Mara's design";
        var palette=new HashSet<Integer>();
        for(int p:pixels(original))palette.add(p);
        int changed=0;
        for(int i=0;i<12;i++) {
            var frame=DialogueRenderer.maraFrame(i*.4+.01,false);
            if(!same(neutral,frame))changed++;
            for(int p:pixels(frame))assert palette.contains(p) : "animation must not introduce blur or new colours";
            for(int y=59;y<80;y++)for(int x=0;x<80;x++)
                assert frame.getRGB(x,y)==original.getRGB(x,y) : "legs and boots must stay planted";
            assert same(neutral,DialogueRenderer.maraFrame(i*.4,true)) : "reduced effects must stay still";
        }
        assert changed>=6 : "idle needs visibly different breathing and cloth poses";
        assert same(neutral,DialogueRenderer.maraFrame(4.8,false)) : "idle must loop without drift";
        assert same(neutral,DialogueRenderer.maraFrame(Double.NaN,false));
        SwingUtilities.invokeAndWait(MaraIdleTest::worldClock);
        System.out.println("MaraIdleTest passed: motion, planted feet, palette, loop, dialogue and pause");
    }

    private static void worldClock() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        game.player().relocate(game.campaignArea().hub().x(),game.campaignArea().hub().y());
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        try {
            var clock=B2BJ.class.getDeclaredField("weatherTime");clock.setAccessible(true);
            game.interact();assert game.dialogue()==OutpostDialogue.CAMP;
            for(int i=0;i<90;i++)panel.step(.02);
            double t=clock.getDouble(panel);
            assert !same(DialogueRenderer.maraFrame(0,false),DialogueRenderer.maraFrame(t,false))
                    : "Mara should keep breathing during conversation";
            game.pause();for(int i=0;i<90;i++)panel.step(.02);
            assert clock.getDouble(panel)==t : "pause must freeze the NPC clock";
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
    }
    private static boolean same(BufferedImage a,BufferedImage b) {return Arrays.equals(pixels(a),pixels(b));}
}
