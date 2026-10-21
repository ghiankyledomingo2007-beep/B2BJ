import java.util.List;
import java.util.Map;

public final class CombatStatsTest {
    public static void main(String[] args) throws Exception {
        assert CombatStats.damage(24,0,false)==24;
        assert CombatStats.damage(24,20,false)==20;
        assert CombatStats.damage(24,20,true)==30;
        assert CombatStats.damage(1,Integer.MAX_VALUE,false)==1;
        assert CombatStats.damage(0,0,true)==0;
        assert CombatStats.damage(Integer.MAX_VALUE,0,true)==Integer.MAX_VALUE;
        regularEnemiesTakeThreeToFiveHits();
        criticalAndOverkillEvents();
        protectionAndIncomingNumbers();
        System.out.println("CombatStatsTest passed: balance, defense, crits, overkill, shields, immunity");
    }
    private static RuinedOutpostGame game() {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(),new Guardian(1500,900));
        game.setCriticalRollForTesting(()->1);game.begin();game.drainEvents();return game;
    }
    private static boolean hit(RuinedOutpostGame game,Wisp body,int attack,boolean slime) throws Exception {
        var hit=RuinedOutpostGame.class.getDeclaredMethod("hurtScout",Wisp.class,int.class,boolean.class,boolean.class);
        hit.setAccessible(true);return (boolean)hit.invoke(game,body,attack,slime,true);
    }
    @SuppressWarnings("unchecked")
    private static void register(RuinedOutpostGame game,CampaignEnemy enemy) throws Exception {
        var field=RuinedOutpostGame.class.getDeclaredField("campaignEnemies");field.setAccessible(true);
        ((Map<Wisp,CampaignEnemy>)field.get(game)).put(enemy.body(),enemy);
    }
    private static void regularEnemiesTakeThreeToFiveHits() throws Exception {
        for(var kind:CampaignEnemy.Kind.values()) {
            if(kind==CampaignEnemy.Kind.FALLEN_KNIGHT)continue;
            for(boolean slime:new boolean[]{true,false}) {
                var game=game();var enemy=new CampaignEnemy(kind,600,400);register(game,enemy);
                int[] combo=slime?new int[]{Player.SLIME_DAMAGE,Player.SLIME_DAMAGE,
                        new WaterProjectile(0,0,1,0,WaterProjectile.Kind.FINISHER).damage()}
                        :new int[]{Player.BLADE_DAMAGE,Player.BLADE_DAMAGE,Player.BLADE_DAMAGE,Player.BLADE_DAMAGE+12};
                int hits=0;
                while(enemy.body().alive()&&hits<10) { assert hit(game,enemy.body(),combo[hits%combo.length],slime);hits++; }
                assert hits>=3&&hits<=5 : kind+" took "+hits+" connected "+(slime?"Slime":"Blade")+" combo hits";
            }
        }
    }
    private static void criticalAndOverkillEvents() throws Exception {
        var game=game();game.setCriticalRollForTesting(()->0);
        var body=new Wisp(600,400,600,600,80,Wisp.Role.GUARD);
        assert hit(game,body,24,false)&&body.health()==50;
        var event=game.drainEvents().get(0);
        assert event.type()==RuinedOutpostGame.EventType.DAMAGE_DEALT&&event.damage()==30&&event.critical();
        body.hurt(49);assert hit(game,body,24,false);
        assert game.drainEvents().get(0).damage()==1 : "display HP actually lost, not overkill";
        assert !hit(game,body,24,false)&&game.drainEvents().isEmpty();
        game.setCriticalRollForTesting(()->CombatStats.CRITICAL_CHANCE);
        body=new Wisp(600,400,600,600,80);assert hit(game,body,20,true);
        event=game.drainEvents().get(0);assert !event.critical()&&event.damage()==20&&event.water();
        assert hit(game,body,60,false);assert game.drainEvents().get(0).heavy();
    }
    private static void protectionAndIncomingNumbers() throws Exception {
        var game=game();var knight=new CampaignEnemy(CampaignEnemy.Kind.FALLEN_KNIGHT,600,400);register(game,knight);
        assert !hit(game,knight.body(),20,true)&&game.drainEvents().isEmpty();
        assert hit(game,knight.body(),24,false);game.drainEvents();
        var bossHit=RuinedOutpostGame.class.getDeclaredMethod("hurtGuardian",int.class,boolean.class);bossHit.setAccessible(true);
        assert !(boolean)bossHit.invoke(game,24,false)&&game.drainEvents().isEmpty();
        game.guardian().activate(500,400);
        for(int i=0;i<300&&game.guardian().state()!=Guardian.State.RECOVER;i++)game.guardian().update(.01,500,400);
        assert (boolean)bossHit.invoke(game,24,false);
        assert game.drainEvents().get(0).damage()==19;
        game.player().collectIchor(100);game.player().transform();
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        hurt.invoke(game,20,900,900);
        var events=game.drainEvents();var number=events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.DAMAGE_TAKEN).findFirst().orElseThrow();
        assert number.damage()==15&&number.x()==500&&number.y()==400&&game.player().health()==85;
        hurt.invoke(game,20,900,900);assert game.drainEvents().isEmpty() : "invulnerable contact has no damage popup";
    }
}
