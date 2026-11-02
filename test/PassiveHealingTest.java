import java.awt.image.BufferedImage;

public final class PassiveHealingTest {
    public static void main(String[] args) throws Exception {
        var game=safe();game.player().hurt(40);
        assert game.healingDelay()==5;
        advance(game,4.9);assert game.player().healthValue()==60&&!game.passiveHealing();
        assert game.healingDelay()>0&&game.healingDelay()<.2 : "HUD countdown follows the remaining delay";
        game.drainEvents();advance(game,2.2);
        assert Math.abs(game.player().healthValue()-64.2)<.1&&game.passiveHealing() : "two HP per second after five damage-free seconds";
        assert game.healingDelay()==0;
        assert game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.HEALED).count()==1 : "one healing cue, not one every tick";
        var panel=new B2BJ(false,game);var recovering=hud(panel);
        double hp=game.player().healthValue();game.pause();advance(game,5);
        assert game.player().healthValue()==hp&&!game.passiveHealing() : "pause freezes healing";
        game.togglePause();
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        hurt.invoke(game,10,game.player().x()+60,game.player().y());
        assert !game.passiveHealing()&&game.healingDelay()==5;var damaged=hud(panel);
        assert recovering.getRGB(432,594)!=damaged.getRGB(432,594) : "regeneration changes the HP bar color";
        var events=B2BJ.class.getDeclaredMethod("handleEvents",java.util.List.class);events.setAccessible(true);events.invoke(panel,game.drainEvents());
        assert hud(panel).getRGB(432,590)!=damaged.getRGB(432,590) : "a real hit flashes the HP bar edge";
        var reduced=B2BJ.class.getDeclaredField("reducedEffects");reduced.setAccessible(true);reduced.set(panel,true);
        assert hud(panel).getRGB(432,590)==damaged.getRGB(432,590) : "Reduced Effects disables the new hit flash";
        hp=game.player().healthValue();advance(game,4.9);assert game.player().healthValue()==hp : "new hit restarts the damage-free wait";
        advance(game,1);assert game.player().healthValue()>hp;
        hp=game.player().healthValue();game.guardian().activate(game.player().x(),game.player().y());game.player().setGodMode(true);
        advance(game,10);assert game.passiveHealing()&&Math.abs(game.player().healthValue()-hp-20)<.1 : "an active boss must not lock regeneration";
        game.player().setGodMode(false);hurt.invoke(game,10,game.guardian().x(),game.guardian().y());game.player().setGodMode(true);
        hp=game.player().healthValue();advance(game,4.9);assert game.player().healthValue()==hp&&!game.passiveHealing();
        advance(game,1);assert game.player().healthValue()>hp&&game.passiveHealing() : "recover again after a boss hit without ending the encounter";
        game=safe();game.player().hurt(1);advance(game,12);assert game.player().healthValue()==100&&!game.passiveHealing()&&game.healingDelay()==0;
        game=safe();game.player().hurt(1000);advance(game,20);assert !game.player().alive()&&!game.passiveHealing();
        game=safe();game.player().hurt(40);assert game.interact()&&game.dialogue()!=null;advance(game,20);
        assert game.player().healthValue()==60 : "dialogue cannot be used to wait out recovery";
        System.out.println("PassiveHealingTest passed: wait, rate, reset, combat, pause, dialogue, death, cap and visible cue");
    }
    private static RuinedOutpostGame safe() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        for(var enemy:game.scouts())enemy.hurt(10000);
        var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());return game;
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double left=seconds;left>1e-8;left-=.1)game.update(Math.min(left,.1),0,0);
    }
    private static BufferedImage hud(B2BJ panel) throws Exception {
        var image=new BufferedImage(1280,720,2);var g=image.createGraphics();
        var draw=B2BJ.class.getDeclaredMethod("drawPlayerHud",java.awt.Graphics2D.class);draw.setAccessible(true);draw.invoke(panel,g);g.dispose();return image;
    }
}
