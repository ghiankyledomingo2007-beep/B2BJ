import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class WardenVictorySceneTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        natural(false);natural(true);
        for(double time:new double[]{0,.5,2.7,4,6,10})pauseAndSkip(time);
        presentation();skipInput(false);skipInput(true);
        System.out.println("WardenVictorySceneTest passed: collapse, gate reveal, voice order, pause/skip, rewards, save/Continue, music and camera return");
    }
    private static RuinedOutpostGame ready(java.nio.file.Path save) throws Exception {
        var game=RuinedOutpostGame.campaign(save);game.begin();game.skipDialogue();
        var boss=game.guardian();game.player().relocate(boss.x(),boss.y()+240);
        for(var enemy:game.scouts())enemy.hurt(10000);
        boss.activate(game.player().x(),game.player().y());return game;
    }
    private static void defeat(RuinedOutpostGame game) throws Exception {
        game.guardian().hurt(game.guardian().health());
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit",boolean.class);hit.setAccessible(true);hit.invoke(game,true);
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double left=seconds;left>1e-8;) {double dt=Math.min(.01,left);game.update(dt,1,1);left-=dt;}
    }
    private static void natural(boolean blade) throws Exception {
        var save=Files.createTempDirectory("b2bj-warden-fall-").resolve("campaign.properties");
        var game=ready(save);var boss=game.guardian();var player=game.player();
        if(blade) {player.collectIchor(100);assert player.transform();}
        int shards=game.shards();defeat(game);game.drainEvents();
        assert game.wardenVictory()&&game.blocked()&&!game.bossActive()&&game.map().gateOpen();
        assert game.shards()==shards+4&&CampaignSave.load(save).orElseThrow().clearedBosses().contains(0);
        assert !game.attack(1,0)&&!game.dash(1,0)&&!game.interact();
        double health=player.healthValue(),x=player.x(),y=player.y(),ichor=player.ichor(),form=player.bladeSeconds();
        int poses=0;var cues=new ArrayList<RuinedOutpostGame.EventType>();var lines=new ArrayList<String>();
        for(int i=0;i<2000&&game.cutsceneActive();i++) {
            game.update(.01,1,1);
            if(boss.visible())poses|=1<<boss.animationFrame();
            game.drainEvents().forEach(e->cues.add(e.type()));
            if(game.bark()!=null&&(lines.isEmpty()||!lines.get(lines.size()-1).equals(game.barkVoiceKey())))lines.add(game.barkVoiceKey());
            if(game.wardenVictory()&&game.wardenVictorySeconds()<RuinedOutpostGame.GATE_RELEASE_AT)assert game.gateOpening()==0;
            if(game.wardenVictorySeconds()>RuinedOutpostGame.GATE_RELEASE_AT+1.41)assert game.gateOpening()==1;
            assert player.healthValue()==health&&player.ichor()==ichor&&player.bladeSeconds()==form&&player.x()==x&&player.y()==y;
        }
        assert !game.cutsceneActive()&&!game.blocked()&&game.gateOpening()==1;
        assert poses==255&&!boss.visible() : "all eight death poses must play before the rubble remains";
        assert cues.equals(List.of(RuinedOutpostGame.EventType.WARDEN_KNEEL,RuinedOutpostGame.EventType.WARDEN_FALL,
                RuinedOutpostGame.EventType.GATE_RELEASE)) : "kneel, break, then the gate: "+cues;
        assert lines.equals(List.of("bark_warden_last","bark_warden_victory","bark_warden_victory_2")) : "dying words, then the victory: "+lines;
        assert game.bark()==null;
        defeat(game);assert !game.wardenVictory()&&game.shards()==shards+4 : "duplicate impacts cannot replay or reward the victory";
        var restored=RuinedOutpostGame.campaign(save);assert restored.continueCampaign();
        assert restored.map().gateOpen()&&restored.gateOpening()==1&&!restored.wardenVictory()&&restored.shards()==game.shards();
        var gate=game.campaignArea().exit();game.player().relocate(gate.x(),gate.y());
        assert game.interact()&&game.dialogue()==OutpostDialogue.GATE;game.skipDialogue();
        assert game.interact()&&game.biome()==1&&game.dialogue()==OutpostDialogue.FOREST_ARRIVAL;
    }
    private static void pauseAndSkip(double seconds) throws Exception {
        var game=ready(null);defeat(game);advance(game,seconds);game.drainEvents();
        double clock=game.wardenVictorySeconds(),death=game.guardian().stateSeconds(),gate=game.gateOpening(),camera=game.storyCameraY();
        game.pause();advance(game,1);assert !game.skipDialogue();
        assert game.wardenVictorySeconds()==clock&&game.guardian().stateSeconds()==death&&game.gateOpening()==gate&&game.storyCameraY()==camera;
        assert game.drainEvents().isEmpty();game.togglePause();
        assert game.skipDialogue()&&!game.wardenVictory()&&game.gateOpening()==1;
        if(game.cutsceneActive())assert game.storyCameraY()==camera : "skip must not snap camera target";
        advance(game,.6);assert !game.blocked()&&game.bark()==null;
        assert game.projectiles().isEmpty();
        assert game.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.WARDEN_FALL||e.type()==RuinedOutpostGame.EventType.GATE_RELEASE);
    }
    private static void presentation() throws Exception {
        GameAudio.setMuted(false);var game=ready(null);var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        game.player().setGodMode(true);for(int i=0;i<210;i++)panel.step(1./30);
        defeat(game);int startX=panel.worldCameraX(),startY=panel.worldCameraY();
        for(int i=0;i<165;i++)panel.step(1./30);   // the camera holds on the breaking Warden, then pans
        assert panel.worldCameraX()>=startX+100&&panel.worldCameraY()<startY-250 : "camera must reveal the eastern gate within world bounds";
        assert GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC)&&!GameAudio.loopRunning(GameAudio.Loop.BOSS_MUSIC);
        String current=null;double elapsed=0,duration=0;int clips=0;
        var key=GameAudio.class.getDeclaredField("dialogueKey");key.setAccessible(true);
        for(int i=0;i<500&&game.cutsceneActive();i++) {
            panel.step(1./30);
            String next=(String)key.get(null);
            if(!java.util.Objects.equals(current,next)) {
                if(current!=null)assert elapsed+.04>=duration : "the complete recording must finish";
                current=next;elapsed=0;if(next!=null)clips++;
            }
            if(current!=null) {elapsed+=1./30;duration=GameAudio.dialogueDuration();}
        }
        assert clips==2&&!game.cutsceneActive();     // its dying words finished before the gate reveal; natural() checks them
        assert Math.abs(game.player().x()-panel.worldCameraX()-640)<=1;
        assert Math.abs(game.player().y()-panel.worldCameraY()-360)<=1;
        GameAudio.setDialogue(null,false);GameAudio.stopLoops();GameAudio.setMuted(true);
    }
    private static void skipInput(boolean mouse) throws Exception {
        var game=ready(null);game.player().setGodMode(true);
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        var reduced=B2BJ.class.getDeclaredField("reducedEffects");reduced.setAccessible(true);reduced.setBoolean(panel,true);
        for(int i=0;i<210;i++)panel.step(1./30);defeat(game);
        for(int i=0;i<36;i++)panel.step(1./30);
        int x=panel.worldCameraX(),y=panel.worldCameraY();
        for(int i=0;i<84;i++)panel.step(1./30);
        assert panel.worldCameraX()==x&&panel.worldCameraY()==y : "Reduced Effects omits the gate pan";
        if(mouse) {
            var event=new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,1220,20,1,false,java.awt.event.MouseEvent.BUTTON1);
            for(var listener:panel.getMouseListeners())listener.mousePressed(event);
        } else panel.getActionMap().get("skipDialoguePressed").actionPerformed(new java.awt.event.ActionEvent(panel,0,"skipDialoguePressed"));
        assert !game.wardenVictory()&&game.map().gateOpen();
        for(int i=0;i<20;i++)panel.step(1./30);assert !game.blocked();
    }
}
