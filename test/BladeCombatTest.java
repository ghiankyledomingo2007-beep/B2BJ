import java.util.List;
import java.awt.event.MouseEvent;

/** Exercises actual input timing, interruption and aim rather than sprite internals. */
public final class BladeCombatTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        for(int move=0;move<=RuinedOutpostGame.SPIN_CLEAVE;move++) {
            double contact=RuinedOutpostGame.strikeWindup(move);
            assert B2BJ.bladeAttackFrame(contact-.001,move)<4;
            assert B2BJ.bladeAttackFrame(contact,move)==4 : "contact art and damage share one clock";
            assert B2BJ.bladeAttackFrame(RuinedOutpostGame.strikeDuration(move),move)==11;
        }
        var game=fixture();
        for(int step=0;step<4;step++) {
            click(game,1,0);assert game.bladeMove()==step : "one click advances one move";
            assert game.comboStep()==step+1;
            advance(game,RuinedOutpostGame.strikeDuration(step)+.02);
        }
        click(game,1,0);assert game.bladeMove()==0 : "finisher starts fresh next click";
        advance(game,1);click(game,1,0);assert game.bladeMove()==0 : "timeout resets chain";

        game=fixture();click(game,1,0);advance(game,.21);
        click(game,0,-1);click(game,-1,0);advance(game,.12);
        assert game.bladeMove()==1 : "single late click buffers, extra clicks never skip moves";
        assert game.bladeAimY()==-1 : "buffer keeps intentional direction";
        advance(game,.7);assert !game.bladeAttacking() : "buffer never repeats";

        game=fixture();assert game.primaryPress(1,0);advance(game,1);
        assert game.charging()&&!game.bladeAttacking() : "hold never fires normal attacks";
        long ready=game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.CHARGE_READY).count();
        assert ready==1 : "one full charge cue";
        double before=game.player().ichor();game.primaryRelease(1,0);
        assert game.bladeMove()==RuinedOutpostGame.SPIN_CLEAVE&&game.bladeAttacking() : "a full charge spins";
        assert Math.abs(before-game.player().ichor()-RuinedOutpostGame.CHARGE_COST)<1e-8;
        game.primaryRelease(1,0);assert Math.abs(before-game.player().ichor()-RuinedOutpostGame.CHARGE_COST)<1e-8;
        advance(game,.75);click(game,1,0);assert game.bladeMove()==0;
        game=fixture();game.primaryPress(1,0);advance(game,.4);before=game.player().ichor();game.primaryRelease(1,0);
        assert game.bladeMove()==4 : "short of the ready cue it is the plain cleave";
        assert Math.abs(before-game.player().ichor()-RuinedOutpostGame.CHARGE_COST)<1e-8 : "both cost the same";

        game=fixture();game.primaryPress(1,0);advance(game,.3);game.pause();game.togglePause();game.primaryRelease(1,0);
        assert !game.charging()&&!game.bladeAttacking() : "pause discards release";
        game.primaryPress(1,0);advance(game,.3);assert game.dash(0,1);game.primaryRelease(1,0);
        assert !game.charging()&&!game.bladeAttacking() : "dash cancels charge";
        game=fixture();click(game,1,0);advance(game,.12);assert game.dash(0,1);
        advance(game,.18);click(game,1,0);assert game.bladeMove()==1 : "one recovery dodge preserves chain";
        assert !game.dash(1,0) : "cannot cancel contact into free damage";
        advance(game,.6);assert game.dash(0,1);advance(game,.18);click(game,1,0);
        assert game.bladeMove()==RuinedOutpostGame.DASH_SLASH : "a click straight out of a dash is the dash slash";
        assert RuinedOutpostGame.strikeKind(RuinedOutpostGame.DASH_SLASH)==0 : "it hits wide, like the draw cut, not like the thrust";
        advance(game,RuinedOutpostGame.strikeDuration(RuinedOutpostGame.DASH_SLASH)+.02);click(game,1,0);
        assert game.bladeMove()==2 : "and the chain carries on from move 2, as the thrust opener did";

        game=fixture();game.primaryPress(1,0);advance(game,.3);
        game.player().spendBladeIchor(game.player().ichor());advance(game,.02);game.primaryRelease(1,0);
        assert !game.bladeAttacking()&&!game.charging() : "form loss clears stored charge";
        var panel=new B2BJ(false,fixture());panel.setSize(1280,720);
        for(int[] point:new int[][]{{1270,360,1},{640,10,2},{10,360,3},{640,710,0}}) {
            var e=new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,point[0],point[1],0,false);
            for(var listener:panel.getMouseMotionListeners())listener.mouseMoved(e);
            panel.step(.02);
            var field=B2BJ.class.getDeclaredField("bladeAnimation");field.setAccessible(true);
            assert ((BladeAnimation)field.get(panel)).directionRow()==point[2] : "idle tracks mouse in four directions";
        }
        var press=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,1200,360,1,false,MouseEvent.BUTTON1);
        for(var listener:panel.getMouseListeners())listener.mousePressed(press);
        panel.step(.04);
        assert !panel.game().bladeAttacking() : "press prepares; short release attacks";
        var release=new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,0,0,1200,360,1,false,MouseEvent.BUTTON1);
        for(var listener:panel.getMouseListeners())listener.mouseReleased(release);
        assert panel.game().bladeAttacking();
        damageGeometryAndInterruption();
        visualContactAndFocusCancel();
        idleKeepsLastMovementFacing();
        System.out.println("BladeCombatTest passed");
    }
    private static void idleKeepsLastMovementFacing() throws Exception {
        var panel=new B2BJ(false,fixture());panel.setSize(1280,720);
        var field=B2BJ.class.getDeclaredField("bladeAnimation");field.setAccessible(true);
        var animation=(BladeAnimation)field.get(panel);
        String[] directions={"down","left","up","right"};
        int[] rows={0,3,2,1};
        for(int i=0;i<directions.length;i++) {
            var mouse=new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,1270,360,0,false);
            for(var listener:panel.getMouseMotionListeners())listener.mouseMoved(mouse);
            panel.getActionMap().get(directions[i]+"Pressed").actionPerformed(new java.awt.event.ActionEvent(panel,0,""));
            panel.step(.13);panel.step(.13);
            assert animation.directionRow()==rows[i] : "movement faces travel direction";
            panel.getActionMap().get(directions[i]+"Released").actionPerformed(new java.awt.event.ActionEvent(panel,0,""));
            panel.step(.01);
            assert animation.directionRow()==rows[i] : "stop must not snap toward stale mouse position";
            panel.step(.13);panel.step(.5);
            assert animation.action()==BladeAnimation.Action.IDLE&&animation.directionRow()==rows[i]
                    : "idle must keep last movement facing in all four directions";
        }
        var mouse=new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,640,710,0,false);
        for(var listener:panel.getMouseMotionListeners())listener.mouseMoved(mouse);
        panel.step(.02);
        assert animation.directionRow()==0 : "fresh mouse motion still aims idle";
        var press=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,1270,360,1,false,MouseEvent.BUTTON1);
        for(var listener:panel.getMouseListeners())listener.mousePressed(press);
        var release=new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,0,0,1270,360,1,false,MouseEvent.BUTTON1);
        for(var listener:panel.getMouseListeners())listener.mouseReleased(release);
        panel.step(.02);
        assert animation.directionRow()==1&&panel.game().bladeAttacking() : "click still aims attack toward cursor";
    }
    private static void visualContactAndFocusCancel() throws Exception {
        var panel=new B2BJ(false,fixture());panel.setSize(1280,720);
        // The swing's effect is the blade trail layer: its own sheet, drawn over the body in step with its pose.
        var effects=B2BJ.class.getDeclaredMethod("drawBladeTrail",java.awt.Graphics2D.class,int.class,int.class);effects.setAccessible(true);
        var game=panel.game();click(game,1,0);panel.step(.001);      // a step poses him; it ignores a zero-length one
        var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();effects.invoke(panel,g,0,0);g.dispose();
        assert java.util.Arrays.stream(image.getRGB(0,0,1280,720,null,0,1280)).noneMatch(p->(p>>>24)>0) : "no contact trail before anticipation";
        advance(game,game.bladeContact()+.02);panel.step(.001);
        g=image.createGraphics();effects.invoke(panel,g,0,0);g.dispose();
        assert java.util.Arrays.stream(image.getRGB(0,0,1280,720,null,0,1280)).anyMatch(p->(p>>>24)>0) : "miss still shows blue slash";
        for(int y=0;y<720;y+=2)for(int x=0;x<1280;x+=2) {
            int pixel=image.getRGB(x,y);
            assert pixel==image.getRGB(x+1,y)&&pixel==image.getRGB(x,y+1)&&pixel==image.getRGB(x+1,y+1) : "slash uses native 2x pixels";
        }
        assert game.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.ENEMY_HIT) : "miss has no hit-only impact";
        advance(game,.5);game.primaryPress(1,0);advance(game,.4);
        for(var listener:panel.getFocusListeners())listener.focusLost(new java.awt.event.FocusEvent(panel,java.awt.event.FocusEvent.FOCUS_LOST));
        assert game.paused()&&!game.charging();game.togglePause();game.primaryRelease(1,0);
        assert !game.bladeAttacking() : "focus loss cannot store a charged release";
    }
    private static void damageGeometryAndInterruption() throws Exception {
        var a=new Wisp(1000,550,1000,1000,100);var b=new Wisp(1020,550,1020,1020,100);
        var player=new Player(900,550);player.collectIchor(100);player.transform();player.setGodMode(true);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(5),player,List.of(a,b),new Guardian(1600,1000));game.begin();
        // Resolve the thrust with two targets in one lane; the farther one must remain untouched.
        var combo=RuinedOutpostGame.class.getDeclaredField("combo");combo.setAccessible(true);combo.setInt(game,0);
        var timer=RuinedOutpostGame.class.getDeclaredField("comboTime");timer.setAccessible(true);timer.setDouble(game,.5);
        click(game,1,0);advance(game,.14);
        assert a.health()==96&&b.health()==100 : "thrust strikes one nearest target";
        int health=a.health();advance(game,.1);assert a.health()==health : "animation never repeats damage";
        game=fixture();game.player().setGodMode(false);click(game,1,0);
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        hurt.invoke(game,1,1000.0,550.0);assert !game.bladeAttacking() : "hurt cancels pending strike";
        game=fixture();game.primaryPress(1,0);advance(game,.3);game.player().spendBladeIchor(90);
        game.primaryRelease(1,0);assert !game.bladeAttacking() : "insufficient Ichor cannot release charge";
        game=fixture();var map=game.map();var wall=map.barriers().get(0);
        player=game.player();player.relocate(wall.centerX()-wall.width()/2-Player.COLLISION_RADIUS-1,wall.centerY()-Player.COLLISION_Y_OFFSET);
        assert !map.isBlocked(player.x(),player.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS);
        click(game,1,0);advance(game,.2);
        assert !map.isBlocked(player.x(),player.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS) : "attack step respects walls";

        // Every cut carries him forward by its own step, all of it before contact.
        game=fixture();
        for(int move=0;move<4;move++) {
            double before=game.player().x();click(game,1,0);advance(game,RuinedOutpostGame.strikeWindup(move)+.011);
            assert Math.abs(game.player().x()-before-RuinedOutpostGame.strikeStep(move))<.5 : "move "+move+" steps in with the cut";
            double landed=game.player().x();advance(game,RuinedOutpostGame.strikeDuration(move));
            assert game.player().x()==landed : "no sliding after contact";
        }
        // A real click is held for a few frames. That time counts as wind-up: contact lands at
        // press + wind-up, and the full step is still taken.
        game=fixture();double start=game.player().x();
        assert game.primaryPress(1,0);advance(game,.05);game.primaryRelease(1,0);
        assert game.bladeAttacking()&&Math.abs(game.bladeElapsed()-.05)<1e-6 : "held time is credited to the wind-up";
        advance(game,RuinedOutpostGame.strikeWindup(0)-.05+.011);
        assert game.bladeElapsed()>=game.bladeContact() : "contact at press + wind-up";
        assert Math.abs(game.player().x()-start-RuinedOutpostGame.strikeStep(0))<.5 : "credited click still takes the whole step";
        game=fixture();assert game.primaryPress(1,0);advance(game,.12);game.primaryRelease(1,0);
        assert game.bladeElapsed()<game.bladeContact() : "a slow click never skips contact";

        // A caught hit is answered: the counter cut faces the attacker, plants him, and ends by itself.
        game=fixture();game.player().setGodMode(false);assert game.riposte();
        hurt.invoke(game,1,game.player().x()-60,game.player().y());
        assert game.bladeCountering()&&game.bladeAimX()<0 : "he answers toward whoever hit him";
        double stood=game.player().x();game.update(.05,1,0);
        assert game.player().x()==stood : "the counter cut plants his feet";
        advance(game,RuinedOutpostGame.COUNTER_DURATION);
        assert !game.bladeCountering() : "and it ends by itself";
        game.update(.05,1,0);assert game.player().x()>stood : "then he is free to move";
        game=fixture();game.player().setGodMode(false);assert game.riposte();
        hurt.invoke(game,1,game.player().x()+60,game.player().y());assert game.dash(0,1);
        assert !game.bladeCountering() : "a dash cuts the counter short";
    }
    static RuinedOutpostGame fixture() {
        var player=new Player(900,550);player.collectIchor(100);player.transform();player.setGodMode(true);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(5),player,List.of(),new Guardian(1600,1000));
        game.begin();game.drainEvents();return game;
    }
    static void click(RuinedOutpostGame game,int x,int y) {game.primaryPress(x,y);game.primaryRelease(x,y);}
    static void advance(RuinedOutpostGame game,double seconds) {
        while(seconds>1e-8){double dt=Math.min(.01,seconds);game.update(dt,0,0);seconds-=dt;}
    }
}
