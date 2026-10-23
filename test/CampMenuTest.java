import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

public final class CampMenuTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        SwingUtilities.invokeAndWait(()->{
            var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
            var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
            key(panel,"campPressed");key(panel,"campReleased");
            assert !camp(panel).isVisible() : "camp cannot open away from the hub";
            var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
            assert game.debugAddShards();
            panel.step(.01);
            key(panel,"upgrade0Pressed");key(panel,"upgrade0Released");
            assert game.upgradeLevel("vitality")==0 : "number keys cannot spend shards during exploration";
            key(panel,"campPressed");
            var focus=new java.awt.event.FocusEvent(panel,java.awt.event.FocusEvent.FOCUS_LOST,false,camp(panel).getComponent(0));
            for(var listener:panel.getFocusListeners())listener.focusLost(focus);
            key(panel,"campPressed");
            assert camp(panel).isVisible() : "held U cannot immediately close the camp after focus moves to a button";
            key(panel,"campReleased");
            assert camp(panel).isVisible();
            double x=game.player().x(),y=game.player().y();
            key(panel,"rightPressed");key(panel,"dashPressed");key(panel,"transformPressed");
            var click=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,900,400,1,false,MouseEvent.BUTTON1);
            for(var listener:panel.getMouseListeners())listener.mousePressed(click);
            for(int i=0;i<30;i++)panel.step(.05);
            assert game.player().x()==x&&game.player().y()==y&&!game.player().dashing()&&!game.player().bladeForm();
            assert game.projectiles().isEmpty() : "clicks through the camp cannot attack";
            int shards=game.shards();
            key(panel,"upgrade0Pressed");key(panel,"upgrade0Released");
            assert game.upgradeLevel("vitality")==1&&game.shards()==shards-2;
            for(int i=0;i<4;i++){key(panel,"upgrade0Pressed");key(panel,"upgrade0Released");}
            assert game.upgradeLevel("vitality")==3 : "completed upgrades stay capped";
            key(panel,"pausePressed");key(panel,"pauseReleased");
            assert !camp(panel).isVisible()&&!game.paused();
            panel.step(.05);assert game.player().x()==x&&game.player().y()==y : "menu-held movement never leaks into play";
            key(panel,"campPressed");key(panel,"campReleased");
            game.player().relocate(hub.x()+500,hub.y());panel.step(.01);
            assert !camp(panel).isVisible() : "a teleport invalidates the camp";
        });
        System.out.println("CampMenuTest passed");
    }
    private static CampMenu camp(B2BJ panel) {
        for(var child:panel.getComponents())if(child instanceof CampMenu camp)return camp;
        throw new AssertionError("camp component missing");
    }
    private static void key(B2BJ panel,String name) {panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel,0,name));}
}
