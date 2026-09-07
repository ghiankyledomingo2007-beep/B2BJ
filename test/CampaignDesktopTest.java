import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public final class CampaignDesktopTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            RuinedOutpostGame game=RuinedOutpostGame.campaign();
            B2BJ panel=new B2BJ(false,game);
            panel.setSize(1280,720);
            var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
            var canvas=image.createGraphics();panel.paint(canvas);
            assert image.getRGB(640,360)!=0;
            panel.getActionMap().get("beginPressed").actionPerformed(new ActionEvent(panel,0,""));
            assert !game.blocked();
            assert game.map().worldWidth()>5000;
            game.player().relocate(game.campaignArea().hub().x(),game.campaignArea().hub().y());
            game.interact();assert game.campaignStory().dialogueOpen();
            panel.paint(canvas);
            assert game.blocked();
            game.interact();panel.paint(canvas);
            assert game.saveCheckpoint();
            assert !game.campaignNotice().isEmpty();
            for(int i=0;i<20;i++)game.update(.5,0,0);
            assert game.campaignNotice().isEmpty() : "routine notices must not permanently cover exploration";
            canvas.dispose();
        });
        System.out.println("Campaign desktop integration tests passed");
    }
}
