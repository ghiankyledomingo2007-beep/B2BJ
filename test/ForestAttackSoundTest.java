import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Real attack state transitions must reach the mixer, including misses and interrupted casts. */
public final class ForestAttackSoundTest {
    private record Fight(RuinedOutpostGame game, CampaignEnemy enemy) { }

    public static void main(String[] args) throws Exception {
        imp();wolf();shaman();dispatch();
        System.out.println("ForestAttackSoundTest passed: swipe timing, misses, pause, interruption, bite/pounce/landing, roots and audible event dispatch");
    }

    @SuppressWarnings("unchecked")
    private static Fight fight(CampaignEnemy.Kind kind) throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();game.debugWarp(1,false);game.skipDialogue();
        game.setDebugGodMode(true);
        var sf=RuinedOutpostGame.class.getDeclaredField("scouts");sf.setAccessible(true);
        var scouts=(List<Wisp>)sf.get(game);scouts.clear();
        var mf=RuinedOutpostGame.class.getDeclaredField("campaignEnemies");mf.setAccessible(true);
        var enemies=(Map<Wisp,CampaignEnemy>)mf.get(game);enemies.clear();
        var enemy=new CampaignEnemy(kind,2180,3264);scouts.add(enemy.body());enemies.put(enemy.body(),enemy);
        game.player().relocate(kind==CampaignEnemy.Kind.MIRE_SHAMAN?1920:2070,3264);
        game.drainEvents();return new Fight(game,enemy);
    }

    private static List<RuinedOutpostGame.Event> step(Fight fight,double seconds) {
        var events=new ArrayList<RuinedOutpostGame.Event>();
        for(double t=0;t<seconds-1e-8;t+=.01) {fight.game.update(.01,0,0);events.addAll(fight.game.drainEvents());}
        return events;
    }
    private static long count(List<RuinedOutpostGame.Event> events,String type) {
        return events.stream().filter(e->e.type().name().equals(type)).count();
    }

    private static void imp() throws Exception {
        var f=fight(CampaignEnemy.Kind.RIFT_IMP);
        var events=step(f,.70);assert count(events,"IMP_WINDUP")==1;
        f.game.pause();assert step(f,.4).isEmpty();f.game.togglePause();
        f.game.player().relocate(1800,3100); // Leave the locked attack: a miss still needs both swishes.
        events.addAll(step(f,1.1));assert count(events,"IMP_CLAW")==2 : events;
        assert count(events,"IMP_WINDUP")==1&&count(events,"PLAYER_HIT")==0;
        f=fight(CampaignEnemy.Kind.RIFT_IMP);step(f,.6);f.enemy.receiveDamage(10000,true,true);
        assert count(step(f,.9),"IMP_CLAW")==0 : "a killed windup cannot play a phantom strike";
        f=fight(CampaignEnemy.Kind.RIFT_IMP);step(f,.6);f.enemy.receiveDamage(1,true,true);
        assert count(step(f,.5),"IMP_CLAW")==0 : "stagger cancels the pending swipe";
    }

    private static void wolf() throws Exception {
        var f=fight(CampaignEnemy.Kind.THORN_WOLF);var body=f.enemy.body();
        var events=new ArrayList<RuinedOutpostGame.Event>();
        for(int attack=0;attack<2;attack++) {
            for(int i=0;i<400&&body.state()!=Wisp.State.LUNGE;i++)events.addAll(step(f,.01));
            assert body.state()==Wisp.State.LUNGE&&!body.wolfLeap();
            for(int i=0;i<400&&(body.state()==Wisp.State.LUNGE||body.state()==Wisp.State.RECOVER);i++)events.addAll(step(f,.01));
        }
        assert count(events,"WOLF_BITE")==2&&count(events,"WOLF_POUNCE")==0;
        f.game.player().relocate(1900,3264);
        for(int i=0;i<500&&count(events,"WOLF_LAND")==0;i++)events.addAll(step(f,.01));
        assert count(events,"WOLF_GROWL")==3 : events;
        assert count(events,"WOLF_POUNCE")==1&&count(events,"WOLF_LAND")==1 : events;
        assert count(step(f,.5),"WOLF_LAND")==0 : "landing cannot repeat throughout recovery";
    }

    private static void shaman() throws Exception {
        var f=fight(CampaignEnemy.Kind.MIRE_SHAMAN);var events=step(f,1.7);
        assert count(events,"MIRE_CHARGE")==1&&count(events,"MIRE_ERUPT")==1 : events;
        assert count(events,"SPIT_SHOT")==0;
        var eruption=events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.MIRE_ERUPT).findFirst().orElseThrow();
        assert eruption.x()==1920&&eruption.y()==3264 : "root sound belongs to its actual target";
        f=fight(CampaignEnemy.Kind.MIRE_SHAMAN);step(f,.6);f.enemy.receiveDamage(1,true,true);
        assert count(step(f,1),"MIRE_ERUPT")==0 : "interrupting the cast prevents its eruption sound";
    }

    private static void drain() {GameAudio.stopLoops();GameAudio.mixVoices(new int[22050*4]);}
    private static void dispatch() throws Exception {
        var f=fight(CampaignEnemy.Kind.RIFT_IMP);var panel=new B2BJ(false,f.game);
        var handle=B2BJ.class.getDeclaredMethod("handleEvents",List.class);handle.setAccessible(true);
        GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(1);GameAudio.setMuted(false);
        try {
            for(String name:List.of("IMP_WINDUP","IMP_CLAW","WOLF_GROWL","WOLF_BITE","WOLF_POUNCE","WOLF_LAND","MIRE_CHARGE","MIRE_ERUPT")) {
                var type=RuinedOutpostGame.EventType.valueOf(name);
                for(boolean near:new boolean[]{true,false}) {
                    drain();handle.invoke(panel,List.of(new RuinedOutpostGame.Event(type,near?2180:4000,3264)));
                    int[] mix=new int[4096];GameAudio.mixVoices(mix);
                    assert Arrays.stream(mix).anyMatch(v->v!=0)==near : name+" hearing range";
                }
                drain();GameAudio.setMuted(true);
                handle.invoke(panel,List.of(new RuinedOutpostGame.Event(type,2180,3264)));
                int[] mix=new int[4096];GameAudio.mixVoices(mix);
                assert Arrays.stream(mix).allMatch(v->v==0);GameAudio.setMuted(false);
            }
        } finally {drain();GameAudio.setMuted(false);}
    }
}
