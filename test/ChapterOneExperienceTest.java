import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

public final class ChapterOneExperienceTest {
    public static void main(String[] args) throws Exception {
        dialogueBranches();carrySurvives();checkpointAndSupplies();saveValidation();armorFeedback();explorationLines();completedRescueStaysComplete();
        System.out.println("ChapterOneExperienceTest passed: story, carry persistence, retry, supplies and armor");
    }
    private static RuinedOutpostGame start() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();return game;
    }
    private static void dialogueBranches() {
        for(boolean met:new boolean[]{false,true})for(int rescued=0;rescued<=3;rescued++) {
            var line=OutpostDialogue.WARDEN.line(1,met,rescued);
            if(!met)assert !line.text().contains("Shuna");
            if(rescued>0)assert !line.text().contains("only one");
            assert Ui.wrap(line.text(),800,3).size()<=2;
            String key=OutpostDialogue.WARDEN.voiceKey(1,met,rescued);
            assert key.endsWith(rescued>0?"_rescued":met?"02":"_stranger");
        }
        var game=start();var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
        game.interact();game.skipDialogue();
        assert game.survivorKnown(FieldObjects.find(0,"survivor_0"));
        assert !game.survivorKnown(FieldObjects.find(0,"survivor_1"));
        for(int i=0;i<3;i++) {
            var person=FieldObjects.find(0,"survivor_"+i);game.player().relocate(person.x(),person.y()+50);
            assert game.interact();game.skipDialogue();
        }
        game.player().relocate(hub.x(),hub.y());game.interact();int reward=game.shards();
        assert game.dialogue()==OutpostDialogue.CAMP_HOME;
        game.pause();assert game.returnToTitle()&&game.continueCampaign();
        game.player().relocate(hub.x(),hub.y());game.interact();
        assert game.shards()==reward : "leaving mid-conversation cannot duplicate the rescue reward";
    }
    private static void carrySurvives() throws Exception {
        var path=Files.createTempDirectory("b2bj-carry-").resolve("save.properties");
        var game=RuinedOutpostGame.campaign(path);game.begin();game.skipDialogue();
        var key=FieldObjects.find(0,"key_0");game.player().relocate(key.x(),key.y());
        assert game.interact()&&game.carried()==key;
        var restored=RuinedOutpostGame.campaign(path);assert restored.continueCampaign();
        assert restored.carried()==key&&restored.fieldPosition(key)==null;
        restored.player().hurt(10000);restored.update(.01,0,0);restored.restart();
        assert restored.carried()==key : "death preserves carried quest progress";
        restored.player().relocate(2000,3300);assert restored.interact();
        var position=restored.fieldPosition(key);
        var again=RuinedOutpostGame.campaign(path);assert again.continueCampaign();
        assert again.carried()==null&&again.fieldPosition(key).equals(position);
        again.player().relocate(position.x(),position.y());assert again.interact();
        var cache=FieldObjects.find(0,"cache_0");again.player().relocate(cache.x(),cache.y()+50);
        assert again.interact()&&again.fieldDone(cache)&&again.carried()==null;
        var used=CampaignSave.load(path).orElseThrow();
        assert used.carried().isEmpty()&&!used.dropped().containsKey(key.id());
    }
    private static void checkpointAndSupplies() {
        var game=start();var rest=FieldObjects.rests(0).get(FieldObjects.GATE_REST);
        assert game.restLit(FieldObjects.GATE_REST);
        game.player().relocate(rest.x(),rest.y()+72);
        assert !game.map().isBlocked(game.player().x(),game.player().y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS,true);
        assert game.saveCheckpoint()&&game.player().ichor()==100;
        assert game.campaignCheckpoint()==FieldObjects.REST_CHECKPOINT+FieldObjects.GATE_REST;
        game.player().hurt(10000);game.update(.01,0,0);game.restart();
        assert game.player().ichor()==100&&game.player().health()==game.player().maxHealth();
        for(int i=0;i<60;i++)game.update(.5,0,0);
        assert game.dialogue()==null&&!game.bossActive();
        assert game.player().health()==game.player().maxHealth() : "patrols must not drift into the retry point";
        assert game.scouts().stream().noneMatch(Wisp::aggro) : "retry must begin outside ordinary enemy aggro";
        var tin=FieldObjects.find(0,"tin_2");game.player().relocate(tin.x(),tin.y());assert game.interact();
        game.player().hurt(50);double hp=game.player().healthValue();
        game.player().relocate(rest.x(),rest.y()+72);int shards=game.shards();
        assert game.interact()&&game.carried()==null&&game.tinsDelivered()==1;
        assert game.player().healthValue()==Math.min(game.player().maxHealth(),hp+25)&&game.shards()==shards+1;
        assert !game.interact()&&game.shards()==shards+1;
    }
    private static void saveValidation() throws Exception {
        var old=new CampaignSave.Progress(0,1,Set.of(),Set.of(),Map.of(),4);
        var path=Files.createTempDirectory("b2bj-old-save-").resolve("save.properties");CampaignSave.save(path,old);
        String text=Files.readString(path).replaceAll("(?m)^(carried|dropped)=.*\\R", "");Files.writeString(path,text);
        assert CampaignSave.load(path).orElseThrow().equals(old) : "old saves default to empty carry state";
        for(String id:new String[]{"../key_0","survivor_0","unknown"}) {
            try {new CampaignSave.Progress(0,0,Set.of(),Set.of(),Map.of(),0,id,Map.of());throw new AssertionError(id);}
            catch(IllegalArgumentException expected) { }
        }
        for(double x:new double[]{Double.NaN,-1,6144}) {
            try {new CampaignSave.Progress(0,0,Set.of(),Set.of(),Map.of(),0,"",Map.of("key_0",new CampaignWorld.Point(x,100)));throw new AssertionError();}
            catch(IllegalArgumentException expected) { }
        }
    }
    private static void armorFeedback() throws Exception {
        var game=start();var boss=game.guardian();boss.activate(game.player().x(),game.player().y());
        var hit=RuinedOutpostGame.class.getDeclaredMethod("hurtGuardian",int.class,boolean.class);hit.setAccessible(true);
        assert !(boolean)hit.invoke(game,50,false)&&boss.health()==boss.maxHealth();
        assert game.drainEvents().stream().anyMatch(e->e.type()==RuinedOutpostGame.EventType.ARMOR_BLOCK);
        hit.invoke(game,50,false);
        assert game.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.ARMOR_BLOCK);
        while(boss.state()!=Guardian.State.RECOVER)boss.update(.01,game.player().x(),game.player().y());
        assert (boolean)hit.invoke(game,50,false)&&boss.health()<boss.maxHealth();
    }
    private static void completedRescueStaysComplete() {
        var game=start();
        for(var enemy:game.scouts())enemy.hurt(10000);
        for(int i=0;i<FieldObjects.SURVIVORS.length;i++) {
            var person=FieldObjects.find(0,"survivor_"+i);
            game.player().relocate(person.x(),person.y()+50);
            assert game.interact();game.skipDialogue();
        }
        var spawn=game.campaignArea().spawn();game.player().relocate(spawn.x(),spawn.y());
        for(int i=0;i<40;i++) {
            game.update(.5,0,0);
            assert game.bark()!=OutpostBark.AFTER_RESCUE : "Sage must not say keep looking after all three survivors are home";
        }
    }
    private static void explorationLines() {
        var game=start();
        // This fixture exercises story hints without first-encounter enemy commentary taking the channel.
        for(var enemy:game.scouts())enemy.hurt(10000);
        var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
        game.interact();game.skipDialogue();game.player().relocate(hub.x()+260,hub.y());game.update(.01,0,0);
        assert game.bark()==OutpostBark.LEAVING_CAMP&&!game.blocked();
        game.pause();for(int i=0;i<20;i++)game.update(.5,0,0);
        assert game.bark()==OutpostBark.LEAVING_CAMP : "pause freezes exploration line";
        game.togglePause();for(int i=0;i<40;i++)game.update(.5,0,0);
        assert game.bark()==null : "farewell must not repeat on every frame";
        var first=FieldObjects.find(0,"survivor_0");
        for(var enemy:game.scouts())enemy.hurt(10000);
        game.player().relocate(first.x(),first.y()-600);game.update(.01,0,0);
        assert game.bark()==OutpostBark.HEARD_SURVIVOR&&game.survivorKnown(first);
        game.player().relocate(first.x(),first.y()+50);game.interact();
        assert game.bark()==null&&game.dialogue()!=null : "full conversation replaces the exploration line";
        game.skipDialogue();game.player().relocate(first.x()-400,first.y());
        for(int i=0;i<37;i++)game.update(.5,0,0);
        assert game.bark()==OutpostBark.AFTER_RESCUE;
        game.pause();game.returnToTitle();assert game.bark()==null;
    }
}
