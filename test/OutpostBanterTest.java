import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioSystem;

public final class OutpostBanterTest {
    public static void main(String[] args) throws Exception {
        selection();fightAndCooldown();closeCallAndInterruption();walkingAndCamp();persistence();recordings();
        System.out.println("OutpostBanterTest passed: 76 voices, context, quiet time, cooldown, interruption and saved variety");
    }

    private static void selection() {
        var heard=new HashSet<String>();int total=0;
        for(var trigger:java.util.List.of(OutpostBark.Trigger.FIGHT,OutpostBark.Trigger.CLOSE_CALL,OutpostBark.Trigger.EXPLORING,OutpostBark.Trigger.CAMP)) {
            OutpostBark next;
            while((next=OutpostBark.unheard(trigger,heard))!=null) {
                assert next.trigger()==trigger&&heard.add(next.key());
                assert next.lines().size()==2;
                assert Set.of(next.line(0).speaker(),next.line(1).speaker()).equals(Set.of(OutpostDialogue.Speaker.RAINORAY,OutpostDialogue.Speaker.INNER_VOICE));
                total++;
            }
            assert OutpostBark.unheard(trigger,heard)==null : "exhausted conversations stay quiet";
        }
        assert total==38;
    }

    private static RuinedOutpostGame scene() throws Exception {
        var path=Files.createTempDirectory("b2bj-banter-").resolve("campaign.properties");
        var heard=new HashSet<String>();
        for(var bark:OutpostBark.values())if(bark.trigger()==OutpostBark.Trigger.STORY)heard.add(bark.key());
        CampaignSave.save(path,new CampaignSave.Progress(0,1,Set.of(),heard,java.util.Map.of(),0));
        var game=RuinedOutpostGame.campaign(path);assert game.continueCampaign();
        for(var enemy:game.scouts())enemy.hurt(10000);
        return game;
    }

    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double left=seconds;left>1e-8;left-=.1)game.update(Math.min(left,.1),0,0);
    }

    private static void defeated(RuinedOutpostGame game,boolean summon) throws Exception {
        var body=new CampaignEnemy(CampaignEnemy.Kind.OUTPOST_GUARD,game.player().x()+120,game.player().y()).body();body.hurt(10000);
        if(summon) {
            var field=RuinedOutpostGame.class.getDeclaredField("bossAdds");field.setAccessible(true);
            @SuppressWarnings("unchecked") var adds=(Set<Wisp>)field.get(game);adds.add(body);
        }
        var method=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);
        method.setAccessible(true);method.invoke(game,body,true);
    }

    private static void fightAndCooldown() throws Exception {
        var game=scene();advance(game,56);assert game.bark()==null : "standing still is not exploration";
        defeated(game,false);advance(game,2.9);assert game.bark()==null : "wait until the fight has settled";
        advance(game,.3);assert game.bark()!=null&&game.bark().trigger()==OutpostBark.Trigger.FIGHT;
        var first=game.bark();double left=game.barkRemaining();game.pause();advance(game,100);
        assert game.bark()==first&&game.barkRemaining()==left : "pause freezes banter";
        game.togglePause();advance(game,3.6);assert game.barkLine().speaker()==first.line(1).speaker();
        advance(game,4);assert game.bark()==null;
        defeated(game,false);advance(game,12.2);assert game.bark()!=null&&game.bark()!=first : "a second fight gets a fresh, timely exchange";
        advance(game,40);assert game.bark()==null : "expired fight events must not play much later";
        defeated(game,true);advance(game,6);assert game.bark()==null : "summons cannot queue post-fight banter";
        defeated(game,false);advance(game,5.2);assert game.bark()!=null&&game.bark()!=first;
    }

    private static void closeCallAndInterruption() throws Exception {
        var game=scene();advance(game,56);game.player().hurt(70);defeated(game,false);advance(game,5.2);
        assert game.bark()!=null&&game.bark().trigger()==OutpostBark.Trigger.CLOSE_CALL;
        assert game.interact()&&game.dialogue()!=null&&game.bark()==null : "NPC speech takes priority";
        game.skipDialogue();advance(game,5);assert game.bark()==null;
        game=scene();advance(game,56);defeated(game,false);advance(game,5.2);
        var current=game.bark();game.player().setGodMode(true);
        game.guardian().activate(game.player().x(),game.player().y());game.update(.01,0,0);
        assert game.bark()==current : "renewed combat must not cut off a sentence";
        game=scene();advance(game,56);defeated(game,false);
        game.guardian().activate(game.player().x(),game.player().y());advance(game,6);
        assert game.bark()==null : "no post-fight speech while a boss is active";
    }

    private static void walkingAndCamp() throws Exception {
        var game=scene();advance(game,56);
        var spawn=game.campaignArea().spawn();game.player().relocate(spawn.x(),spawn.y());advance(game,.1);
        assert game.bark()==null : "a teleport is not walking";
        for(int cycle=0;cycle<80&&game.bark()==null;cycle++)game.update(.1,cycle%20<10?1:-1,0);
        assert game.bark()!=null&&game.bark().trigger()==OutpostBark.Trigger.EXPLORING : "travel can start a quiet exchange";
        double x=game.player().x();game.update(.1,1,0);assert game.player().x()!=x&&!game.blocked();
        game=scene();advance(game,56);assert game.interact();game.skipDialogue();
        var hub=game.campaignArea().hub();advance(game,36);assert game.bark()==null : "let Mara finish before leaving camp";
        game.player().relocate(hub.x()-450,hub.y());advance(game,.1);
        assert game.bark()!=null&&game.bark().trigger()==OutpostBark.Trigger.CAMP;
    }

    private static void persistence() throws Exception {
        var game=scene();advance(game,56);defeated(game,false);advance(game,5.2);var first=game.bark();
        var field=RuinedOutpostGame.class.getDeclaredField("savePath");field.setAccessible(true);var path=(Path)field.get(game);
        assert !CampaignSave.load(path).orElseThrow().unlocks().contains(first.key()) : "starting is not finishing";
        advance(game,8);var saved=CampaignSave.load(path).orElseThrow();assert saved.unlocks().contains(first.key());
        var restored=RuinedOutpostGame.campaign(path);assert restored.continueCampaign();
        for(var enemy:restored.scouts())enemy.hurt(10000);
        advance(restored,56);defeated(restored,false);advance(restored,5.2);
        assert restored.bark()!=null&&restored.bark()!=first : "reload retains heard exchanges";
        restored.player().hurt(10000);restored.update(.01,0,0);restored.restart();
        assert restored.bark()==null : "retry clears pending conversations";
    }

    static void recordings() throws Exception {
        var g=new java.awt.image.BufferedImage(1,1,2).createGraphics();int count=0;
        for(var bark:OutpostBark.values())if(bark.biome()==0&&bark.trigger()!=OutpostBark.Trigger.STORY&&!bark.combat())for(int i=0;i<2;i++) {
            var line=bark.line(i);boolean sage=line.speaker()==OutpostDialogue.Speaker.INNER_VOICE;
            assert g.getFontMetrics(Ui.heading(sage?20:21)).stringWidth(line.text())<(sage?440:500) : "subtitle must fit: "+line.text();
            try(var source=Ui.open("assets/audio/dialogue/"+bark.voiceKey(i)+".wav")) {
                assert source!=null : "missing recording: "+bark.voiceKey(i);
                try(var wav=AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(source))) {
                    var format=wav.getFormat();double seconds=wav.getFrameLength()/format.getFrameRate();
                    assert format.getSampleRate()==22050&&format.getSampleSizeInBits()==16&&format.getChannels()==1;
                    assert seconds>.5&&seconds<5 : "recording must finish before the next speaker: "+bark;
                }
            }
            count++;
        }
        g.dispose();assert count==76;
    }
}
