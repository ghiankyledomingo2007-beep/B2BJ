import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioSystem;

public final class ChapterOneVoicesTest {
    public static void main(String[] args) throws Exception {
        bossMoments();recordings();
        System.out.println("ChapterOneVoicesTest passed: all 211 clips, story branches and Warden moments");
    }

    static void recordings() throws Exception {
        var clips=new HashMap<String,String>();
        for(var scene:OutpostDialogue.values())if(scene.biome()==0)for(int i=0;i<scene.lines().size();i++)
            for(boolean met:new boolean[]{false,true})for(int rescued:new int[]{0,1,3}) {
                String key=scene.voiceKey(i,met,rescued),text=scene.line(i,met,rescued).text();
                var previous=clips.putIfAbsent(key,text);
                assert previous==null||previous.equals(text) : "different scripts share a clip: "+key;
            }
        for(var bark:OutpostBark.values())if(bark.biome()==0)for(int i=0;i<bark.lines().size();i++) {
            assert clips.put(bark.voiceKey(i),bark.line(i).text())==null;
            assert bark.banter()?duration(bark.voiceKey(i))<5:duration(bark.voiceKey(i))+.2<bark.lineSeconds() : "bark cuts off speech: "+bark;
        }
        for(String key:clips.keySet())assert duration(key)>.5 : "empty recording: "+key;
        assert clips.size()==211 : "every Chapter One line and branch must be voiced";
    }

    static double duration(String key) throws Exception {
        try(var source=Ui.open("assets/audio/dialogue/"+key+".wav")) {
            assert source!=null : "missing voice: "+key;
            try(var audio=AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(source))) {
                var format=audio.getFormat();
                assert format.getSampleRate()==22050&&format.getChannels()==1&&format.getSampleSizeInBits()==16;
                assert format.getEncoding()==javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED&&!format.isBigEndian();
                byte[] data=audio.readAllBytes();assert data.length==audio.getFrameLength()*2;
                int peak=0;
                for(int i=0;i<data.length;i+=2)peak=Math.max(peak,Math.abs((short)((data[i]&255)|(data[i+1]<<8))));
                assert peak>1000&&peak<30000 : "silent or clipped voice: "+key;
                return audio.getFrameLength()/format.getFrameRate();
            }
        }
    }

    private static void bossMoments() throws Exception {
        var path=Files.createTempDirectory("b2bj-warden-voices-").resolve("campaign.properties");
        var heard=new HashSet<String>();heard.add(OutpostDialogue.WARDEN.key());
        for(var bark:OutpostBark.values())if(bark.trigger()==OutpostBark.Trigger.STORY)heard.add(bark.key());
        CampaignSave.save(path,new CampaignSave.Progress(0,1,Set.of(),heard,java.util.Map.of(),0));
        var game=RuinedOutpostGame.campaign(path);assert game.continueCampaign();
        for(var enemy:game.scouts())enemy.hurt(10000);
        game.player().setGodMode(true);game.player().relocate(game.guardian().x()-450,game.guardian().y());
        game.update(.01,0,0);assert game.bark()==OutpostBark.WARDEN_START&&game.bossActive();
        assert !game.blocked() : "boss conversation must not freeze combat";
        double left=game.barkRemaining();game.pause();advance(game,20);assert game.barkRemaining()==left;
        game.togglePause();advance(game,OutpostBark.WARDEN_START.lineSeconds()+.1);assert game.barkLine().speaker()==OutpostDialogue.Speaker.RAINORAY;
        game.player().setGodMode(false);assert game.player().hurt(90);game.player().setGodMode(true);
        advance(game,12);assert game.bark()==OutpostBark.WARDEN_CLOSE_CALL;
        advance(game,20);assert game.bark()!=OutpostBark.WARDEN_CLOSE_CALL : "low-health warning must not repeat";
        game.guardian().hurt(game.guardian().maxHealth()/2+1);
        for(int i=0;i<1500&&game.bark()!=OutpostBark.WARDEN_PHASE;i++)game.update(.01,0,0);
        assert game.bark()==OutpostBark.WARDEN_PHASE : "phase transition gets its own cue";
        advance(game,OutpostBark.WARDEN_PHASE.lineSeconds()+.1);assert game.barkLine().speaker()==OutpostDialogue.Speaker.RAINORAY;
        game.guardian().hurt(game.guardian().health());
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit",boolean.class);hit.setAccessible(true);hit.invoke(game,true);
        assert game.bark()==OutpostBark.WARDEN_PHASE : "victory waits for the current sentence";
        for(int i=0;i<800&&game.bark()!=OutpostBark.WARDEN_VICTORY;i++)game.update(.01,0,0);
        assert game.bark()==OutpostBark.WARDEN_VICTORY;
        advance(game,OutpostBark.WARDEN_VICTORY.lineSeconds()+.1);assert game.barkLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
        advance(game,OutpostBark.WARDEN_VICTORY.lineSeconds()+.1);assert game.bark()==null;
        var saved=CampaignSave.load(path).orElseThrow();
        for(var bark:java.util.List.of(OutpostBark.WARDEN_START,OutpostBark.WARDEN_PHASE,
                OutpostBark.WARDEN_CLOSE_CALL,OutpostBark.WARDEN_VICTORY))assert saved.unlocks().contains(bark.key());
        assert saved.clearedBosses().contains(0);
    }

    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double left=seconds;left>1e-8;left-=.1)game.update(Math.min(left,.1),0,0);
    }
}
