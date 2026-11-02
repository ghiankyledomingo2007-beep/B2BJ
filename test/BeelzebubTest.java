import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public final class BeelzebubTest {
    public static void main(String[] args) throws Exception {
        harvestAndCooldown();terrainAndEligibility();interruptions();eatsThroughTheFight();lifetimeAndReset();callouts();
        SwingUtilities.invokeAndWait(()->{
            try { inputAndPresentation(args.length==0?null:Path.of(args[0]));restartBinding(); }
            catch(Exception e) {throw new AssertionError(e);}
        });
        System.out.println("BeelzebubTest passed: batch rewards, radius, cover, summons, expiry, eating through the fight, hard ends, pause, reversion, R binding, effects, sound and HUD");
    }

    static RuinedOutpostGame fixture() {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(300,300),List.of(),new Guardian(1000,650));
        game.begin();game.player().collectIchor(100);assert game.transform();game.player().spendBladeIchor(30);
        return game;
    }

    private static void callouts() throws Exception {
        var game=fixture();
        var mode=RuinedOutpostGame.class.getDeclaredField("campaignMode");mode.setAccessible(true);mode.set(game,true);
        var cooldown=RuinedOutpostGame.class.getDeclaredField("beelzebubCooldown");cooldown.setAccessible(true);
        var bark=RuinedOutpostGame.class.getDeclaredField("bark");bark.setAccessible(true);
        var start=RuinedOutpostGame.class.getDeclaredMethod("startBark",OutpostBark.class);start.setAccessible(true);
        var cancel=RuinedOutpostGame.class.getDeclaredMethod("endAbsorption");cancel.setAccessible(true);
        game.player().setUnlimitedBlade(true);
        assert !game.beelzebub(1,0)&&game.bark()==null : "no shout without a valid target";
        corpse(game,500,300,false);
        var calls=List.of(OutpostBark.BEELZEBUB_CALL,OutpostBark.BEELZEBUB_CALL_AGAIN,OutpostBark.BEELZEBUB_CALL_TAKE);
        for(int i=0;i<4;i++) {
            game.player().move(0,0,.5,game.map());cooldown.set(game,0d);bark.set(game,null);
            assert game.beelzebub(1,0)&&game.bark()==calls.get(i%3) : "rotate three voiced calls";
            assert game.barkLine().speaker()==OutpostDialogue.Speaker.RAINORAY&&game.bark().combat();
            var key=game.barkVoiceKey();assert !game.beelzebub(1,0)&&key.equals(game.barkVoiceKey());
            cancel.invoke(game);
        }
        game.player().move(0,0,.5,game.map());cooldown.set(game,0d);start.invoke(game,OutpostBark.IDENTIFY_GUARD);
        assert game.beelzebub(1,0)&&game.bark()==OutpostBark.IDENTIFY_GUARD : "casting must not cut off Raphael or queue a late shout";
        cancel.invoke(game);bark.set(game,null);game.player().move(0,0,.5,game.map());
        assert !game.beelzebub(1,0)&&game.bark()==null : "cooldown denial stays silent";
    }

    private static void harvestAndCooldown() throws Exception {
        var game=fixture();game.player().hurt(40);
        corpse(game,500,300,false);corpse(game,640,300,false);corpse(game,770,300,false);settle(game);
        double hp=game.player().healthValue(),ichor=game.player().ichor();
        assert game.beelzebub(1,0)&&game.beelzebubActive();
        assert !game.beelzebub(1,0) : "held/repeated requests cannot restart a cast";
        assert game.beelzebubTarget(game.corpses().get(0))&&game.beelzebubTarget(game.corpses().get(1));
        assert !game.beelzebubTarget(game.corpses().get(2)) : "far bodies stay untouched";
        advance(game,.15);
        assert game.corpses().get(0).x()==500 : "the hand opens before the bodies move";
        advance(game,.35);
        assert game.corpses().get(0).x()<500&&game.corpses().get(1).x()<640;
        assert game.player().healthValue()==hp : "no healing until the cast finishes";
        double progress=game.absorptionProgress(),cooldown=game.beelzebubCooldown();
        game.pause();advance(game,2);
        assert game.absorptionProgress()==progress&&game.beelzebubCooldown()==cooldown;
        game.togglePause();advance(game,.66);
        assert !game.beelzebubActive()&&game.corpses().size()==1&&game.corpses().get(0).x()==770;
        assert close(game.player().healthValue(),hp+20);
        assert close(game.player().ichor(),ichor-100/12.*1.16+10) : "one normal +5 Ichor reward per body";
        assert game.player().trait()==Player.Trait.NONE : "human harvest cannot stack slime traits";
        assert game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.ABSORBED).count()==1;
        corpse(game,500,300,false);settle(game);assert !game.beelzebub(1,0);
        game.player().setUnlimitedBlade(true);advance(game,10);corpse(game,520,300,false);settle(game);
        assert game.beelzebub(1,0) : "cooldown eventually releases";
    }

    private static void terrainAndEligibility() throws Exception {
        var empty=fixture();assert !empty.beelzebub(1,0)&&empty.beelzebubCooldown()==0;
        var slime=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(300,300),List.of(),new Guardian(1000,650));
        slime.begin();corpse(slime,500,300,false);settle(slime);
        assert !slime.beelzebub(1,0)&&slime.beelzebubCooldown()==0 : "human form only";
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        var blocked=new RuinedOutpostGame(map,new Player(wall.centerX()-70,wall.centerY()-24),List.of(),new Guardian(1000,650));
        blocked.begin();blocked.player().collectIchor(100);blocked.transform();
        corpse(blocked,wall.centerX()+70,wall.centerY()-24,false);settle(blocked);
        assert !blocked.beelzebub(1,0)&&blocked.beelzebubCooldown()==0 : "cannot harvest through terrain";
        var adds=fixture();adds.player().hurt(40);corpse(adds,520,300,true);settle(adds);
        double hp=adds.player().healthValue();assert adds.beelzebub(1,0);advance(adds,1.16);
        assert adds.corpses().isEmpty()&&adds.player().healthValue()==hp : "summons cannot farm HP";
        var live=fixture();var enemies=RuinedOutpostGame.class.getDeclaredField("scouts");enemies.setAccessible(true);
        @SuppressWarnings("unchecked") var scouts=(List<Wisp>)enemies.get(live);
        var enemy=new Wisp(500,300,500,500,100);scouts.add(enemy);
        assert !live.beelzebub(1,0)&&enemy.health()==100 : "live enemies are not targets";
    }

    private static void interruptions() throws Exception {
        // Owner 2026-10-29: "it only draws the corpses closer, not absorb". Ordinary actions keep the meal going.
        for(String action:new String[]{"move","dash","attack","hold","crescent","riposte","hurt"}) {
            var game=fixture();corpse(game,520,300,false);settle(game);
            assert game.beelzebub(1,0);advance(game,.4);
            switch(action) {
                case "move" -> game.update(.01,1,0);
                case "dash" -> {assert game.dash(1,0);}
                case "attack" -> {assert game.attack(1,0);}
                case "hold" -> {assert game.primaryPress(1,0);}
                case "crescent" -> {assert game.ichorCrescent(1,0);}
                case "riposte" -> {assert game.riposte();}
                case "hurt" -> {assert game.player().hurt(20);game.update(.01,0,0);}
            }
            assert game.beelzebubActive() : "the meal survives: "+action;
            advance(game,1);
            assert !game.beelzebubActive()&&game.corpses().isEmpty() : "it finishes eating through: "+action;
        }
        // Only the hard ends drop it: the half-pulled body stays where it was.
        for(String action:new String[]{"death","revert"}) {
            var game=fixture();corpse(game,520,300,false);settle(game);
            assert game.beelzebub(1,0);advance(game,.4);
            if(action.equals("death")) {game.player().hurt(1000);game.update(.01,0,0);}
            else {game.player().spendBladeIchor(game.player().ichor());game.update(.01,0,0);}
            assert !game.beelzebubActive()&&game.absorptionTarget()==null : action;
            assert game.corpses().size()==1 : "cancelled corpses stay available: "+action;
            double x=game.corpses().get(0).x();advance(game,.2);
            assert game.corpses().get(0).x()==x : "cancelled body stays where it was pulled";
        }
        var campaign=RuinedOutpostGame.campaign();campaign.begin();
        assert !campaign.beelzebub(1,0) : "opening blocks skills";
        campaign.skipDialogue();campaign.player().collectIchor(100);campaign.transform();
        var hub=campaign.campaignArea().hub();campaign.player().relocate(hub.x(),hub.y());
        corpse(campaign,hub.x()+30,hub.y(),false);settle(campaign);
        assert campaign.beelzebub(1,0);assert campaign.interact();
        assert campaign.dialogue()!=null&&!campaign.beelzebubActive() : "NPC conversation cancels the cast";
    }

    /** The owner's case: cast on two bodies, keep moving, take a hit and dash; both are eaten, +5 Ichor each. */
    private static void eatsThroughTheFight() throws Exception {
        var game=fixture();                                                         // 70 Ichor: room for the reward
        corpse(game,520,300,false);corpse(game,560,330,false);settle(game);
        assert game.beelzebub(1,0)&&game.beelzebubActive();
        for(int i=0;i<25;i++)game.update(.01,1,1);                                  // held movement
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        hurt.invoke(game,10,game.player().x()+30,game.player().y());game.update(.01,0,0);
        assert game.beelzebubActive() : "a hit does not drop the meal";
        for(int i=0;i<40&&!game.dash(-1,0);i++)game.update(.01,0,0);              // dash as soon as it is allowed
        assert game.beelzebubActive()&&game.player().dashing() : "a dash does not drop the meal";
        double before=game.player().ichor();game.drainEvents();
        for(int i=0;i<200&&game.beelzebubActive();i++) {before=game.player().ichor();game.update(.01,1,0);}
        assert !game.beelzebubActive()&&game.corpses().isEmpty() : "every caught body is eaten";
        double gain=game.player().ichor()-before;                                  // less one 10 ms tick of blade drain
        assert Math.abs(gain-10)<.2 : "+5 Ichor per body, got "+gain;
        var gulp=game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.ABSORBED).toList();
        assert gulp.size()==1&&gulp.get(0).heavy()&&gulp.get(0).damage()==10 : "one gulp carrying the Ichor it gave";
    }

    private static void lifetimeAndReset() throws Exception {
        var game=fixture();game.player().setUnlimitedBlade(true);
        corpse(game,520,300,false);settle(game);advance(game,19.2);
        assert game.beelzebub(1,0);advance(game,1.16);
        assert game.corpses().isEmpty()&&!game.beelzebubActive();
        assert game.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.ABSORBED)
                : "expired corpses cannot grant rewards";
        game=fixture();corpse(game,520,300,false);settle(game);assert game.beelzebub(1,0);
        game.restart();assert game.corpses().isEmpty()&&!game.beelzebubActive()&&game.beelzebubCooldown()==0;
    }

    private static void inputAndPresentation(Path output) throws Exception {
        GameAudio.setMuted(false);
        var game=fixture();corpse(game,510,300,false);corpse(game,500,470,false);settle(game);
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        var before=fx(game,false);key(panel,false);panel.step(.35);
        assert game.beelzebubActive();double time=game.absorptionProgress();
        key(panel,false);assert game.absorptionProgress()==time;
        key(panel,true);
        assert GameAudio.loopRunning(GameAudio.Loop.DEVOUR);
        assert !GameAudio.loopRunning(GameAudio.Loop.CHARGE) : "devouring has its own sound";
        var after=fx(game,false);var reduced=fx(game,true);
        assert pixels(before)==0&&pixels(after)>6000&&pixels(after)<65000 : "substantial effect stays around bodies and hand";
        assert pixels(reduced)<pixels(after) : "reduced effects remove the busy currents";
        var animation=B2BJ.class.getDeclaredField("bladeAnimation");animation.setAccessible(true);
        assert ((BladeAnimation)animation.get(panel)).action()==BladeAnimation.Action.CAST;
        game.pause();panel.step(.4);
        assert !GameAudio.loopRunning(GameAudio.Loop.DEVOUR)&&game.absorptionProgress()==time;
        assert java.util.Arrays.equals(after.getRGB(0,0,1280,720,null,0,1280),fx(game,false).getRGB(0,0,1280,720,null,0,1280));
        game.togglePause();panel.step(.3);assert GameAudio.loopRunning(GameAudio.Loop.DEVOUR);
        for(int width:new int[]{1280,1920}) {
            panel.setSize(width,width*9/16);panel.doLayout();
            var image=new BufferedImage(width,width*9/16,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();
            if(output!=null) {Files.createDirectories(output);ImageIO.write(image,"png",output.resolve("beelzebub-"+width+".png").toFile());}
        }
        assert game.dash(1,0);panel.step(.01);
        assert game.beelzebubActive()&&GameAudio.loopRunning(GameAudio.Loop.DEVOUR) : "a dash keeps eating";
        for(int i=0;i<40&&game.beelzebubActive();i++)panel.step(.05);
        assert !game.beelzebubActive()&&!GameAudio.loopRunning(GameAudio.Loop.DEVOUR);
        assert pixels(fx(game,false))==0 : "a finished meal leaves no vortex";
        GameAudio.stopLoops();
    }

    private static BufferedImage fx(RuinedOutpostGame game,boolean reduced) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        BeelzebubFx.draw(g,game,0,0,reduced);g.dispose();return image;
    }
    private static void restartBinding() {
        for(boolean campaign:new boolean[]{false,true}) {
            var game=campaign?RuinedOutpostGame.campaign():fixture();
            if(campaign){game.begin();game.skipDialogue();}
            var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
            game.player().hurt(10000);game.update(.01,0,0);
            assert game.story().phase()==OutpostStory.Phase.DEAD;
            key(panel,false);key(panel,true);
            assert game.player().alive()&&game.story().phase()!=OutpostStory.Phase.DEAD
                    : "R must still restart after death";
            assert !game.beelzebubActive()&&game.beelzebubCooldown()==0;
        }
    }
    private static long pixels(BufferedImage image) {return java.util.Arrays.stream(image.getRGB(0,0,1280,720,null,0,1280)).filter(c->c>>>24!=0).count();}
    private static void key(B2BJ panel,boolean released) {
        var binding=panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_R,0,released));
        assert binding!=null : "R must have a real keyboard binding";
        panel.getActionMap().get(binding).actionPerformed(new ActionEvent(panel,0,"R"));
    }
    static void advance(RuinedOutpostGame game,double seconds) {for(double t=seconds;t>1e-8;t-=.01)game.update(Math.min(.01,t),0,0);}
    private static boolean close(double a,double b) {return Math.abs(a-b)<1e-6;}
    private static void settle(RuinedOutpostGame game) {advance(game,.1);game.drainEvents();}
    @SuppressWarnings("unchecked")
    static void corpse(RuinedOutpostGame game,double x,double y,boolean summon) throws Exception {
        var enemy=new Wisp(x,y,x,x,1);enemy.hurt(1);
        if(summon) {var field=RuinedOutpostGame.class.getDeclaredField("bossAdds");field.setAccessible(true);((Set<Wisp>)field.get(game)).add(enemy);}
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);hit.setAccessible(true);hit.invoke(game,enemy,true);
        var drops=RuinedOutpostGame.class.getDeclaredField("drops");drops.setAccessible(true);((List<?>)drops.get(game)).clear();
    }
}
