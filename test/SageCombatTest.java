import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Set;

public final class SageCombatTest {
    public static void main(String[] args) throws Exception {
        learning();healthAndHits();dialogueClearsHitReaction();visibleDuringCombat();conversationMusic();bossMusicHandoff();
        System.out.println("SageCombatTest passed: witnessed attacks, learned warnings, cadence, boss overlay and conversation music");
    }
    private static void set(Object object,String name,Object value) throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    private static void guardianTick(RuinedOutpostGame game) throws Exception {
        var method=RuinedOutpostGame.class.getDeclaredMethod("updateGuardian",double.class);method.setAccessible(true);method.invoke(game,.02);
    }
    private static void tell(RuinedOutpostGame game,double distance) throws Exception {
        var boss=game.guardian();game.player().relocate(boss.x()-distance,boss.y());
        set(boss,"state",Guardian.State.APPROACH);set(boss,"stateTime",1.24);
        set(boss,"previousAttack",Guardian.Attack.SHOCKWAVE);guardianTick(game);
    }
    private static void learning() throws Exception {
        var path=Files.createTempDirectory("b2bj-sage-").resolve("save.properties");
        CampaignSave.save(path,new CampaignSave.Progress(0,1,Set.of(),Set.of(),java.util.Map.of(),0));
        var game=RuinedOutpostGame.campaign(path);assert game.continueCampaign();
        var boss=game.guardian();game.player().relocate(boss.x()-220,boss.y());boss.activate(game.player().x(),game.player().y());
        tell(game,220);assert game.bark()==null : "Sage must observe before identifying attacks";
        for(int observed=1;observed<=2;observed++) {
            set(boss,"attack",Guardian.Attack.TARGET);set(boss,"state",Guardian.State.TELEGRAPH);
            set(boss,"stateTime",boss.telegraphDuration()-.01);guardianTick(game);
            boolean known=CampaignSave.load(path).orElseThrow().unlocks().contains("sage_warden_target");
            assert known==(observed==2) : "learn only after two witnessed releases";
        }
        set(boss,"state",Guardian.State.RECOVER);set(boss,"stateTime",0d);guardianTick(game);
        assert game.bark()==OutpostBark.WARDEN_READ : "Sage acknowledges the pattern between attacks";
        set(game,"bark",null);set(game,"bossBarkCooldown",0d);
        tell(game,220);assert game.bark()==OutpostBark.SAGE_TARGET : "identify the learned attack during its next tell";
        set(game,"bark",null);tell(game,220);assert game.bark()==null : "warnings must respect the shared cooldown";
        set(game,"bossBarkCooldown",0d);tell(game,400);assert game.bark()==null : "knowing a slam does not reveal a charge";
        tell(game,220);assert game.bark()==OutpostBark.SAGE_TARGET_AGAIN : "repeat patterns use a different natural warning";
        var restored=RuinedOutpostGame.campaign(path);assert restored.continueCampaign();
        tell(restored,220);assert restored.bark()==OutpostBark.SAGE_TARGET : "Sage retains learned patterns after loading";
    }
    private static void healthAndHits() throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var boss=game.guardian();game.player().relocate(boss.x()-220,boss.y());boss.activate(game.player().x(),game.player().y());
        var tick=RuinedOutpostGame.class.getDeclaredMethod("updateBark",double.class,double.class);tick.setAccessible(true);
        boss.hurt((int)(boss.maxHealth()*.70));set(boss,"state",Guardian.State.RECOVER);
        tick.invoke(game,.01,0d);assert game.bark()==null : "no nearly-down line at thirty percent";
        boss.hurt((int)(boss.maxHealth()*.10));set(boss,"state",Guardian.State.RECOVER);
        tick.invoke(game,.01,0d);assert game.bark()==OutpostBark.WARDEN_WEAK;
        set(game,"bark",null);set(game,"bossBarkCooldown",0d);tick.invoke(game,.01,0d);
        assert game.bark()==null : "health milestone is not repeated";
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        game.player().setGodMode(true);hurt.invoke(game,10,boss.x(),boss.y());tick.invoke(game,.01,0d);
        assert game.bark()==null : "blocked damage must not produce a hit reaction";
        game.player().setGodMode(false);hurt.invoke(game,10,boss.x(),boss.y());tick.invoke(game,.01,0d);
        assert game.bark()==null : "leave room for Rainoray's brief hurt reaction";tick.invoke(game,1.01,0d);
        assert game.bark()==OutpostBark.SAGE_HIT;
        set(game,"bark",null);tick.invoke(game,.1,0d);assert game.bark()==null : "one hit does not repeat";
        set(game,"hurtReactionTime",2d);tick.invoke(game,.1,0d);assert game.bark()==null : "hit chatter has its own cooldown";
        set(game,"hurtReactionTime",1.4d);set(game,"hurtReactionCooldown",0d);tick.invoke(game,.01,0d);
        assert game.bark()==OutpostBark.SAGE_HIT_AGAIN;
    }
    private static void dialogueClearsHitReaction() throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        hurt.invoke(game,10,hub.x()+100,hub.y());
        assert game.interact()&&game.dialogue()==OutpostDialogue.CAMP;
        for(int i=0;i<60;i++)game.update(.5,0,0);
        game.skipDialogue();game.update(.2,0,0);
        assert game.bark()==null : "an old hit reaction must not play after an NPC conversation";
    }
    private static void visibleDuringCombat() throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        game.guardian().activate(game.player().x(),game.player().y());
        set(game,"bark",OutpostBark.WARDEN_START);set(game,"barkTime",2.6);
        var with=draw(game);set(game,"bark",null);var without=draw(game);int changed=0;
        for(int y=120;y<270;y++)for(int x=400;x<880;x++)if(with.getRGB(x,y)!=without.getRGB(x,y))changed++;
        assert changed>1500 : "Sage must remain visible beneath the boss bar during combat";
        for(int y=0;y<110;y++)for(int x=340;x<940;x++)assert with.getRGB(x,y)==without.getRGB(x,y) : "Sage must not cover the boss bar";
    }
    private static BufferedImage draw(RuinedOutpostGame game) {
        var image=new BufferedImage(1280,720,2);var g=image.createGraphics();CampaignRenderer.drawHud(g,game,1280,720,true);g.dispose();return image;
    }
    private static void conversationMusic() throws Exception {
        GameAudio.setMuted(false);GameAudio.setMasterVolume(1);GameAudio.setMusicVolume(.25);
        var game=RuinedOutpostGame.campaign();game.begin();var panel=new B2BJ(false,game);
        for(int i=0;i<5;i++)game.advanceDialogue();
        panel.step(.1);assert GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC) : "reconstruction needs its music bed";
        game.pause();panel.step(.1);assert !GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC) : "pause respects music state";
        game.togglePause();panel.step(.1);assert GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC);
        game.skipDialogue();panel.step(.1);assert !GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC) : "conversation theme fades back to exploration";
        set(game,"dialogue",OutpostDialogue.WARDEN);set(game,"dialogueIndex",0);panel.step(.1);
        assert GameAudio.loopRunning(GameAudio.Loop.BOSS_MUSIC) : "Warden introduction needs its tense theme";
        assert !GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC);
        game.pause();panel.step(.1);assert !GameAudio.loopRunning(GameAudio.Loop.BOSS_MUSIC) : "pausing the Warden introduction stops its theme";
        GameAudio.stopLoops();GameAudio.setDialogue(null,false);
    }
    private static void bossMusicHandoff() throws Exception {
        GameAudio.stopLoops();GameAudio.mixVoices(new int[22050]);
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var panel=new B2BJ(false,game);var boss=game.guardian();
        game.player().relocate(boss.x()-300,boss.y());
        set(game,"dialogue",OutpostDialogue.WARDEN);set(game,"dialogueIndex",0);
        var sync=B2BJ.class.getDeclaredMethod("syncConversationMusic");sync.setAccessible(true);sync.invoke(panel);
        var gains=GameAudio.class.getDeclaredField("LOOP_GAIN");gains.setAccessible(true);
        double intro=((double[])gains.get(null))[GameAudio.Loop.BOSS_MUSIC.ordinal()];
        assert intro>=.7 : "the introduction must remain audible beneath speech ducking";
        GameAudio.mixVoices(new int[2205]);
        set(game,"dialogue",null);boss.activate(game.player().x(),game.player().y());
        var fight=B2BJ.class.getDeclaredMethod("bossMusic",double.class);fight.setAccessible(true);fight.invoke(panel,.016);
        double after=((double[])gains.get(null))[GameAudio.Loop.BOSS_MUSIC.ordinal()];
        assert after>=intro&&after-intro<.05 : "intro-to-fight transition must not drop the soundtrack back to silence";
        GameAudio.stopLoops();GameAudio.setDialogue(null,false);
    }
}
