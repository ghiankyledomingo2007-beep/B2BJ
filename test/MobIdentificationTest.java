import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MobIdentificationTest {
    private static void set(Object target,String name,Object value) throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private static Object get(Object target,String name) throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private static void tick(RuinedOutpostGame game,double seconds) throws Exception {
        var m=RuinedOutpostGame.class.getDeclaredMethod("updateBark",double.class,double.class);
        m.setAccessible(true);m.invoke(game,seconds,0d);
    }
    @SuppressWarnings("unchecked")
    private static void only(RuinedOutpostGame game,CampaignEnemy... enemies) throws Exception {
        var bodies=(List<Wisp>)get(game,"scouts");bodies.clear();
        var identities=(Map<Wisp,CampaignEnemy>)get(game,"campaignEnemies");identities.clear();
        for(var e:enemies){bodies.add(e.body());identities.put(e.body(),e);}
    }
    public static void main(String[] args) throws Exception {
        for(var kind:CampaignEnemy.Kind.values()) {
            var bark=OutpostBark.identification(kind);if(bark==null)continue;
            var path=Files.createTempDirectory("b2bj-identify-").resolve("save.properties");
            CampaignSave.save(path,new CampaignSave.Progress(bark.biome(),1,bark.biome()==1?Set.of(0):Set.of(),Set.of(),Map.of(),0));
            var game=RuinedOutpostGame.campaign(path);assert game.continueCampaign();
            double x=game.player().x(),y=game.player().y();
            // A clear stretch around camp, independent of authored enemy placement.
            double dx=0,dy=0;
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4;
                if(game.map().clearLine(x,y+24,x+Math.cos(a)*140,y+Math.sin(a)*140+24)) {
                    dx=Math.cos(a)*140;dy=Math.sin(a)*140;break;
                }
            }
            assert dx!=0||dy!=0;
            var enemy=new CampaignEnemy(kind,x+dx,y+dy);only(game,enemy);
            game.player().relocate(x-1000,y);tick(game,.1);
            assert game.bark()==null : "offscreen enemy must not be identified";
            game.player().relocate(x,y);
            set(game,"dialogue",bark.biome()==1?OutpostDialogue.FOREST_SEDGE:OutpostDialogue.CAMP);
            tick(game,.1);assert game.bark()==null : "NPC dialogue takes priority";
            set(game,"dialogue",null);tick(game,.1);
            assert game.bark()==bark&&game.barkLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
            assert bark.combat()&&!game.blocked() : "identification stays visible without freezing combat";
            assert CampaignSave.load(path).orElseThrow().unlocks().contains(bark.key());
            double remaining=game.barkRemaining();game.pause();game.update(.5,0,0);
            assert game.barkRemaining()==remaining;game.togglePause();
            tick(game,4.5);assert game.bark()!=bark;
            tick(game,30);assert game.bark()!=bark : "same mob does not repeat";
            var restored=RuinedOutpostGame.campaign(path);assert restored.continueCampaign();
            restored.player().relocate(x,y);only(restored,new CampaignEnemy(kind,x+dx,y+dy));tick(restored,.1);
            assert restored.bark()!=bark : "knowledge survives loading";
        }
        cadenceAndOcclusion();
        System.out.println("MobIdentificationTest passed: seven types, first sight, dialogue priority, pause, cadence, walls and persistence");
    }
    private static void cadenceAndOcclusion() throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();game.debugWarp(1,false);
        game.player().relocate(1280,2624);
        var imp=new CampaignEnemy(CampaignEnemy.Kind.RIFT_IMP,1280,2624);
        var wolf=new CampaignEnemy(CampaignEnemy.Kind.THORN_WOLF,1280,2624);
        only(game,imp,wolf);tick(game,.1);var first=game.bark();assert first!=null;
        tick(game,4.5);assert game.bark()==null : "mixed groups do not become back-to-back roll calls";
        tick(game,3.6);assert game.bark()!=null&&game.bark()!=first;
        set(game,"bark",null);set(game,"identificationCooldown",0d);
        var shaman=new CampaignEnemy(CampaignEnemy.Kind.MIRE_SHAMAN,1280,2624);only(game,shaman);
        boolean blocked=false;
        for(var wall:game.map().barriers()) {
            if(wall.width()>400||wall.height()>400)continue;
            double x=wall.centerX(),y=wall.centerY(),d=wall.width()/2+30;
            if(d*2>=460||game.map().clearLine(x-d,y,x+d,y))continue;
            game.player().relocate(x-d,y-24);set(shaman.body(),"x",x+d);set(shaman.body(),"y",y-24);
            tick(game,.1);assert game.bark()!=OutpostBark.FOREST_IDENTIFY_SHAMAN;
            blocked=true;break;
        }
        assert blocked : "occlusion fixture must cross a real barrier";
        game.player().relocate(shaman.body().x(),shaman.body().y());tick(game,.1);
        assert game.bark()==OutpostBark.FOREST_IDENTIFY_SHAMAN : "an occluded sighting must not consume identification";
    }
}
