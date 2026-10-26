import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CompanionCadenceTest {
    public static void main(String[] args) throws Exception {
        for(int biome=0;biome<2;biome++) { cadence(biome,false);cadence(biome,true); }
        recordingClock();mixerCompletion();identificationPreservesContext();hitVariety();bossVoice();
        System.out.println("CompanionCadenceTest passed: both chapters, saved rotation, full recordings, context and reactions");
    }
    private static RuinedOutpostGame scene(int biome,boolean allHeard) throws Exception {
        var heard=new HashSet<String>();
        for(var bark:OutpostBark.values())if(allHeard||bark.trigger()==OutpostBark.Trigger.STORY)heard.add(bark.key());
        var save=Files.createTempDirectory("companion-cadence-").resolve("save.properties");
        CampaignSave.save(save,new CampaignSave.Progress(biome,1,biome==0?Set.of():Set.of(0),heard,Map.of(),0));
        var game=RuinedOutpostGame.campaign(save);assert game.continueCampaign();
        for(var enemy:game.scouts())enemy.hurt(10000);
        game.player().setGodMode(true);
        return game;
    }
    private static void sync(RuinedOutpostGame game) {
        GameAudio.setDialogue(game.barkVoiceKey(),game.paused());
        game.setBarkVoiceDuration(GameAudio.dialogueDuration());
    }
    private static void cadence(int biome,boolean allHeard) throws Exception {
        var game=scene(biome,allHeard);var starts=new HashMap<OutpostBark,Double>();
        OutpostBark previous=null;int count=0;
        for(int tick=0;tick<2400;tick++) {
            sync(game);game.update(.1,tick%20<10?1:-1,0);
            var now=game.bark();
            if(now!=null&&now!=previous&&now.banter()) {
                if(count==0)assert game.sageTime()<16 : "first exchange must not wait a minute";
                Double last=starts.put(now,game.sageTime());
                assert last==null||game.sageTime()-last>=180 : "no recent pair repeated";
                count++;
            }
            previous=now;
        }
        assert count>=6 : "companions went quiet in biome "+biome+": "+count;
        assert starts.size()>=6 : "use the available variety before repeating";
        GameAudio.setDialogue(null,false);
    }
    private static void set(Object owner,String name,Object value) throws Exception {
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);
    }
    private static Object get(Object owner,String name) throws Exception {
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);
    }
    private static void start(RuinedOutpostGame game,OutpostBark bark) throws Exception {
        var method=RuinedOutpostGame.class.getDeclaredMethod("startBark",OutpostBark.class);
        method.setAccessible(true);method.invoke(game,bark);
    }
    private static void recordingClock() throws Exception {
        var game=scene(0,false);start(game,OutpostBark.WALK_BIRDS);sync(game);
        double duration=GameAudio.dialogueDuration();assert duration>0;
        double remaining=game.barkRemaining();sync(game);sync(game);
        assert game.barkRemaining()==remaining : "repeated UI sync must not restart a recording";
        game.pause();game.update(.5,0,0);sync(game);assert game.barkRemaining()==remaining;
        game.togglePause();
        for(double t=0;t<duration-.1;t+=.01) {sync(game);game.update(.01,0,0);}
        assert game.barkVoiceKey().equals("bark_walk_birds") : "keep the whole first recording";
        for(int t=0;t<60&&game.barkVoiceKey().equals("bark_walk_birds");t++) {sync(game);game.update(.01,0,0);}
        assert game.barkVoiceKey().equals("bark_walk_birds_2") : "reply follows without fixed dead air";
        game.guardian().activate(game.player().x(),game.player().y());
        double time=0;
        while(game.bark()==OutpostBark.WALK_BIRDS&&time<6) {sync(game);game.update(.01,0,0);time+=.01;}
        assert time>1&&time<5 : "aggro neither cuts off nor stalls the reply";
        assert GameAudio.dialogueDuration()>0 : "duration survives mixer completion";
        GameAudio.setDialogue(null,false);assert GameAudio.dialogueDuration()==0;
        start(game,OutpostBark.WARDEN_VOICE_STAND);sync(game);
        assert game.barkRemaining()<2 : "short boss replies must not hold a fixed 3.6 second slot";
    }
    private static void mixerCompletion() throws Exception {
        var game=scene(0,false);String previous=null;int finished=0;
        var voice=GameAudio.class.getDeclaredField("dialogueVoice");voice.setAccessible(true);
        GameAudio.setDialogue(null,false);
        for(int tick=0;tick<6000;tick++) {
            String key=game.barkVoiceKey();
            if(previous!=null&&!previous.equals(key)) {
                assert voice.get(null)==null : "runtime advances before the recording finishes: "+previous;
                finished++;
            }
            sync(game);game.update(.02,tick%100<50?1:-1,0);
            GameAudio.mixVoices(new int[441]);previous=key;
        }
        assert finished>=6 : "exercise several complete spoken exchanges through the mixer";
        GameAudio.setDialogue(null,false);
    }
    private static void identificationPreservesContext() throws Exception {
        var game=scene(0,false);set(game,"banterCooldown",4d);
        set(game,"pendingBanter",OutpostBark.Trigger.FIGHT);set(game,"pendingBanterTime",35d);
        start(game,OutpostBark.IDENTIFY_GUARD);
        assert (double)get(game,"banterCooldown")==4;
        assert get(game,"pendingBanter")==OutpostBark.Trigger.FIGHT;
        for(int t=0;t<90&&(game.bark()==null||game.bark().trigger()!=OutpostBark.Trigger.FIGHT);t++)game.update(.1,0,0);
        assert game.bark()!=null&&game.bark().trigger()==OutpostBark.Trigger.FIGHT;
    }
    private static void hitVariety() throws Exception {
        var game=scene(0,false);var seen=new HashSet<OutpostBark>();
        var tick=RuinedOutpostGame.class.getDeclaredMethod("updateBark",double.class,double.class);tick.setAccessible(true);
        for(int i=0;i<8;i++) {
            set(game,"bark",null);set(game,"hurtReactionTime",1.4d);set(game,"hurtReactionCooldown",0d);
            tick.invoke(game,.01,0d);assert seen.add(game.bark()) : "repeat before exhausting hit reactions";
        }
        assert seen.size()==8;
    }
    private static void bossVoice() throws Exception {
        var game=scene(0,false);var seen=new HashSet<OutpostBark>();
        @SuppressWarnings("unchecked") var heard=(Set<String>)get(game,"campaignUnlocks");
        heard.add(OutpostDialogue.WARDEN.key());
        var boss=game.guardian();game.player().relocate(boss.x()-220,boss.y());
        for(int t=0;t<3600;t++) {
            if(t==1800)boss.hurt(boss.maxHealth()*2/3);
            game.update(.1,0,0);
            if(game.bark()!=null&&game.barkLine().speaker()==OutpostDialogue.Speaker.WARDEN)seen.add(game.bark());
        }
        assert seen.size()>=2 : "boss must speak through different fight moments: "+seen;
        assert seen.contains(OutpostBark.WARDEN_VOICE_ORDER) : "low health changes his reaction";
    }
}
