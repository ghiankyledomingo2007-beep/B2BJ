import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ForestChapterTest {
    public static void main(String[] args) throws Exception {
        var path=Files.createTempDirectory("b2bj-forest-story-").resolve("save.properties");
        CampaignSave.save(path,new CampaignSave.Progress(1,1,Set.of(0),Set.of(),Map.of("vitality",1),7));
        var game=RuinedOutpostGame.campaign(path);assert game.continueCampaign();
        assert game.dialogue()==null&&game.shards()==7&&game.upgradeLevel("vitality")==1;
        assert game.campaignObjective().contains("Souei")&&game.campaignObjectivePoint().equals(game.campaignArea().hub());
        for(var enemy:game.scouts())enemy.hurt(10000);
        assert game.interact()&&game.dialogue()==OutpostDialogue.FOREST_SEDGE;
        double clock=game.sageTime(),x=game.player().x();game.update(.5,1,0);
        assert game.sageTime()>clock&&game.player().x()==x;
        game.pause();assert !game.skipDialogue();game.togglePause();game.skipDialogue();
        assert game.interact()&&game.dialogue()==OutpostDialogue.FOREST_SEDGE_WAIT;game.skipDialogue();
        assert game.saveCheckpoint();
        game.player().hurt(game.player().maxHealth());game.update(.01,0,0);
        assert game.story().phase()==OutpostStory.Phase.DEAD;game.restart();
        assert game.biome()==1&&game.player().alive()&&game.dialogueSeen(OutpostDialogue.FOREST_SEDGE);
        assert game.nearCampaignHub()&&game.forestHazards().isEmpty();
        assert game.upgradeLevel("vitality")==1&&game.shards()==7;
        for(var enemy:game.scouts())enemy.hurt(10000);
        int shards=game.shards();
        OutpostDialogue[] discoveries={OutpostDialogue.FOREST_WAYFINDER,OutpostDialogue.FOREST_MOONWELL,OutpostDialogue.FOREST_ORCHARD};
        for(int i=0;i<3;i++) {
            var at=game.campaignArea().landmarks().get(i+9);game.player().relocate(at.x(),at.y());
            assert game.interact()&&game.dialogue()==discoveries[i];game.skipDialogue();
            assert game.interact();game.skipDialogue();
        }
        assert game.shards()==shards+3 : "optional discoveries only reward once";
        var boss=game.guardian();game.player().relocate(boss.x()-350,boss.y());game.update(.01,0,0);
        assert game.dialogue()==OutpostDialogue.FOREST_BRIARHEART&&boss.state()==Guardian.State.DORMANT;
        assert !game.attack(1,0);game.update(.5,0,0);game.skipDialogue();
        game.player().setGodMode(true);
        var seen=new HashSet<OutpostBark>();
        for(int i=0;i<18000;i++) {
            game.update(.01,0,0);
            if(game.bark()!=null)seen.add(game.bark());
            if(boss.impactNumber()<4)assert game.bark()==null||game.bark().trigger()!=OutpostBark.Trigger.WARNING : "Sage cannot predict an unobserved pattern";
        }
        assert seen.contains(OutpostBark.FOREST_BOSS_START)&&seen.contains(OutpostBark.FOREST_BOSS_OPEN)&&seen.contains(OutpostBark.FOREST_BOSS_READ);
        assert seen.stream().anyMatch(b->b.biome()==1&&b.trigger()==OutpostBark.Trigger.WARNING);
        var saved=CampaignSave.load(path).orElseThrow();
        for(var attack:Guardian.ForestAttack.values())assert saved.unlocks().contains("sage_briar_"+attack.name().toLowerCase(java.util.Locale.ROOT));
        assert saved.unlocks().stream().noneMatch(s->s.startsWith("sage_warden_")) : "knowledge belongs to this boss";
        boss.hurt(boss.maxHealth()/2+1);
        for(int i=0;i<1500&&game.bark()!=OutpostBark.FOREST_BOSS_PHASE;i++)game.update(.01,0,0);
        assert game.bark()==OutpostBark.FOREST_BOSS_PHASE;
        boss.hurt(boss.health());var defeat=RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit",boolean.class);
        defeat.setAccessible(true);defeat.invoke(game,false);
        assert game.bark()==OutpostBark.FOREST_BOSS_PHASE : "finish current speech before the victory exchange";
        for(int i=0;i<900&&game.bark()!=OutpostBark.FOREST_BOSS_VICTORY;i++)game.update(.01,0,0);
        assert game.bark()==OutpostBark.FOREST_BOSS_VICTORY;
        var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
        assert game.interact()&&game.dialogue()==OutpostDialogue.FOREST_SEDGE_AFTER;game.skipDialogue();
        assert game.saveCheckpoint();
        var reload=RuinedOutpostGame.campaign(path);assert reload.continueCampaign();
        assert !reload.guardian().alive()&&reload.dialogueSeen(OutpostDialogue.FOREST_SEDGE);
        assert reload.dialogueSeen(OutpostDialogue.FOREST_MOONWELL);
        var exit=reload.campaignArea().exit();reload.player().relocate(exit.x(),exit.y());
        assert reload.interact()&&reload.dialogue()==OutpostDialogue.FOREST_END;reload.skipDialogue();
        assert reload.interact()&&reload.story().phase()==OutpostStory.Phase.COMPLETE;
        assert CampaignWorld.area(1).enemies().size()==20;
        assert !OutpostBark.FOREST_FIGHT_WOLVES.fits(CampaignEnemy.Kind.RIFT_IMP);
        assert OutpostBark.FOREST_FIGHT_WOLVES.fits(CampaignEnemy.Kind.THORN_WOLF);
        var heard=new HashSet<String>();OutpostBark next;int n=0;
        while((next=OutpostBark.unheard(OutpostBark.Trigger.EXPLORING,heard,1))!=null) {
            assert next.biome()==1&&heard.add(next.key());n++;
        }
        assert n==10 : "forest exploration has its own non-repeating pool";
        System.out.println("ForestChapterTest passed: Sedge, discoveries, camera, boss learning, phases, ending, save and separate banter");
    }
}
