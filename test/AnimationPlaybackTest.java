import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** Reproduces the input/render failures found in the recorded animation audit. */
public final class AnimationPlaybackTest {
    private static final int[][] AIM={{0,1},{1,0},{0,-1},{-1,0}};
    private static BladeAnimation blade(B2BJ panel) throws Exception {
        var field=B2BJ.class.getDeclaredField("bladeAnimation");field.setAccessible(true);
        return (BladeAnimation)field.get(panel);
    }
    private static B2BJ panel() {
        var panel=new B2BJ(false,BladeCombatTest.fixture());panel.setSize(1280,720);return panel;
    }
    private static void advance(B2BJ panel,double seconds) {
        for(double left=seconds;left>1e-8;left-=Math.min(left,1.0/60))panel.step(Math.min(left,1.0/60));
    }
    private static void move(B2BJ panel,int row,boolean down) {
        panel.getActionMap().get(new String[]{"down","right","up","left"}[row]+(down?"Pressed":"Released"))
                .actionPerformed(new ActionEvent(panel,0,""));
    }
    private static void cast(B2BJ panel,int row) {
        var event=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,
                640+AIM[row][0]*220,360+AIM[row][1]*220,1,false,MouseEvent.BUTTON3);
        for(var listener:panel.getMouseListeners())listener.mousePressed(event);
    }
    private static BufferedImage body(B2BJ panel) throws Exception {
        var image=new BufferedImage(360,320,BufferedImage.TYPE_INT_ARGB);
        var method=B2BJ.class.getDeclaredMethod("drawPlayer",java.awt.Graphics2D.class,int.class,int.class);
        method.setAccessible(true);var g=image.createGraphics();
        method.invoke(panel,g,(int)Math.round(panel.game().player().x())-180,(int)Math.round(panel.game().player().y())-160);
        g.dispose();return image;
    }
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        for(int row=0;row<4;row++) {
            var panel=panel();blade(panel).face(-AIM[row][0],-AIM[row][1]);cast(panel,row);panel.step(1.0/60);
            assert blade(panel).action()==BladeAnimation.Action.CAST&&blade(panel).directionRow()==row
                    : "Crescent click must aim the body as well as the projectile";
            var shot=panel.game().crescents().get(0);
            assert shot.directionX()==AIM[row][0]&&shot.directionY()==AIM[row][1];

            panel=panel();move(panel,row,true);advance(panel,.3);cast(panel,row);
            double x=panel.game().player().x(),y=panel.game().player().y();advance(panel,.3);
            assert panel.game().player().x()==x&&panel.game().player().y()==y : "cast feet must remain planted";
            advance(panel,.15);
            assert Math.hypot(panel.game().player().x()-x,panel.game().player().y()-y)>0 : "movement resumes after cast";

            panel=panel();move(panel,row,true);panel.game().primaryPress(AIM[row][0],AIM[row][1]);
            x=panel.game().player().x();y=panel.game().player().y();advance(panel,.8);
            assert panel.game().player().x()==x&&panel.game().player().y()==y : "charge feet remain planted";

            panel=panel();assert panel.game().dash(AIM[row][0],AIM[row][1]);advance(panel,.05);
            assert panel.game().primaryPress(AIM[row][0],AIM[row][1]);advance(panel,.06);
            assert panel.game().player().dashing()&&blade(panel).action()==BladeAnimation.Action.DASH
                    : "buffered M1 cannot replace an active dash pose";
            assert panel.game().player().invulnerable()&&!panel.game().player().damageFlashing();
            assert java.util.Arrays.stream(body(panel).getRGB(0,0,360,320,null,0,360)).anyMatch(p->(p>>>24)>0)
                    : "undamaged dash body stays visible";
            panel.game().primaryRelease(AIM[row][0],AIM[row][1]);advance(panel,.08);
            assert panel.game().bladeAttacking() : "dash buffer still releases one attack";
        }
        var early=panel();var late=panel();advance(late,.3);
        early.game().primaryPress(1,0);late.game().primaryPress(1,0);
        advance(early,.4);advance(late,.4);
        assert blade(early).action()==BladeAnimation.Action.HOLD&&blade(early).frame()==blade(late).frame()
                : "hold entry must not depend on world uptime";
        int holdFrame=blade(early).frame();advance(early,1.125);
        assert blade(early).frame()!=holdFrame : "full charge hold keeps breathing instead of freezing at the charge cap";

        var dead=panel();dead.game().player().setGodMode(false);dead.game().player().hurt(100);advance(dead,.4);
        assert !dead.game().player().alive()&&blade(dead).action()!=BladeAnimation.Action.IDLE;
        var pixels=body(dead).getRGB(0,0,360,320,null,0,360);advance(dead,.7);
        assert java.util.Arrays.equals(pixels,body(dead).getRGB(0,0,360,320,null,0,360)) : "dead body must not breathe";

        var player=new Player(900,550);player.collectIchor(100);player.setUnlimitedBlade(true);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(5),player,List.of(),new Guardian(1700,1000));game.begin();
        var morph=new B2BJ(false,game);morph.setSize(1280,720);
        morph.getActionMap().get("transformPressed").actionPerformed(new ActionEvent(morph,0,""));advance(morph,.69);
        assert player.bladeForm()&&blade(morph).action()==BladeAnimation.Action.IDLE&&blade(morph).frame()==0
                : "transformation must reveal the initial idle pose";
        System.out.println("AnimationPlaybackTest passed");
    }
}
