import java.util.ArrayList;
import java.util.List;

public final class WardenIntroSlamTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        sequence(0,470,false,.01);sequence(-300,120,true,.05);sequence(300,0,false,.5);
        for(double seconds:new double[]{.2,2.3,3.2,3.6,4.1})skipAndPause(seconds);
        presentation();
        System.out.println("WardenIntroSlamTest passed: approach, slam, recoil, collision, pause/skip, audio timing and untouched combat state");
    }
    private static RuinedOutpostGame scene(double dx,double dy,boolean blade) {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        if(blade) {game.player().collectIchor(100);assert game.player().transform();}
        game.player().relocate(game.guardian().x()+dx,game.guardian().y()+dy);game.update(.01,0,0);
        assert game.dialogue()==OutpostDialogue.WARDEN;
        advance(game,1.4);assert game.advanceDialogue();assert game.advanceDialogue();
        assert game.wardenStepActive()&&!game.dialogueReady()&&game.dialogueIndex()==2;
        game.drainEvents();return game;
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        while(seconds>1e-8) {double dt=Math.min(.01,seconds);game.update(dt,1,1);seconds-=dt;}
    }
    private static double distance(RuinedOutpostGame game) {
        return Math.hypot(game.player().x()-game.guardian().x(),game.player().y()-game.guardian().y());
    }
    private static void sequence(double dx,double dy,boolean blade,double dt) {
        var game=scene(dx,dy,blade);var player=game.player();var boss=game.guardian();
        double health=player.health(),ichor=player.ichor(),formTime=player.bladeSeconds(),start=distance(game),nearest=start,highest=0;
        double enemyX=game.scouts().get(0).x();
        List<RuinedOutpostGame.EventType> events=new ArrayList<>();
        for(int i=0;i<600&&game.wardenStepActive();i++) {
            assert !game.advanceDialogue()&&!game.attack(1,0)&&!game.dash(1,0);
            assert game.wardenIntroFrame()>=0&&game.wardenIntroFrame()<8;
            game.update(dt,1,1);nearest=Math.min(nearest,distance(game));highest=Math.max(highest,game.wardenPlayerLift());
            game.drainEvents().forEach(e->events.add(e.type()));
            assert !game.map().isBlocked(player.x(),player.y()+24,24) : "script must respect walls";
            assert player.health()==health&&player.ichor()==ichor&&player.bladeSeconds()==formTime : "cinematic cannot cost health or resources";
            assert boss.state()==Guardian.State.DORMANT&&boss.impactNumber()==0&&game.scouts().get(0).x()==enemyX;
        }
        assert !game.wardenStepActive()&&game.dialogueReady()&&game.dialogueIndex()==2;
        assert nearest<start-30&&distance(game)>nearest+30&&distance(game)<=nearest+85 : "step closer, then a modest knockback";
        assert highest>8&&game.wardenPlayerLift()==0;
        assert events.equals(List.of(RuinedOutpostGame.EventType.INTRO_WINDUP,RuinedOutpostGame.EventType.INTRO_SLAM,RuinedOutpostGame.EventType.INTRO_LAND)) : events;
        game.skipDialogue();advance(game,.6);game.update(.01,0,0);
        assert game.bossActive()&&boss.state()==Guardian.State.TELEGRAPH&&boss.impactNumber()==0;
        assert game.dialogueSeen(OutpostDialogue.WARDEN);
    }
    private static void skipAndPause(double seconds) {
        var game=scene(0,470,false);advance(game,seconds);game.drainEvents();
        double x=game.player().x(),y=game.player().y(),lift=game.wardenPlayerLift(),focus=game.storyCameraFocus();
        int frame=game.wardenIntroFrame();game.pause();advance(game,1);
        assert !game.skipDialogue()&&game.player().x()==x&&game.player().y()==y;
        assert game.wardenPlayerLift()==lift&&game.storyCameraFocus()==focus&&game.wardenIntroFrame()==frame;
        assert game.drainEvents().isEmpty();game.togglePause();
        assert game.skipDialogue()&&!game.wardenStepActive()&&game.wardenPlayerLift()==0;
        advance(game,.6);assert game.player().x()==x&&game.player().y()==y;
        assert game.drainEvents().isEmpty() : "skip cannot leave delayed impact cues";
        game.update(.01,0,0);assert game.bossActive()&&game.dialogue()==null;
    }
    private static void presentation() throws Exception {
        GameAudio.setMuted(false);
        var game=scene(0,470,false);var panel=new B2BJ(false,game);panel.setSize(1280,720);
        var reduced=B2BJ.class.getDeclaredField("reducedEffects");reduced.setAccessible(true);reduced.setBoolean(panel,true);
        int impulses=0;boolean lifted=false;
        for(int i=0;i<160&&game.wardenStepActive();i++) {
            panel.step(.033);
            assert GameAudio.dialogueDuration()==0 : "next Warden line must wait until Rainoray lands";
            if(game.wardenPlayerLift()>10)lifted=true;
            assert game.drainEvents().isEmpty() : "presentation must consume cinematic cues immediately";
            var feedback=B2BJ.class.getDeclaredField("feedback");feedback.setAccessible(true);
            if(((CombatFeedback)feedback.get(panel)).shakeStrength()>0)impulses++;
            if(game.wardenStepActive()) {
                var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics=image.createGraphics();CampaignRenderer.drawSage(graphics,game,1280,720,true);graphics.dispose();
                assert java.util.Arrays.stream(image.getRGB(560,0,160,180,null,0,160)).allMatch(pixel->pixel==0)
                        : "Raphael's idle seal must not cover the Warden during his strike";
            }
        }
        assert lifted&&impulses>0;
        panel.step(.033);assert GameAudio.dialogueDuration()>0 : "dialogue resumes after the action";
        GameAudio.setDialogue(null,false);GameAudio.stopLoops();GameAudio.setMuted(true);
    }
}
