import java.util.ArrayList;
import java.util.List;

/** Real skill simulation: earned charge, walls, bounded hits, vulnerability and one stored meal. */
public final class RainorayKitTest {
    static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception {
        var m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,args);
    }
    static void ready(RuinedOutpostGame game)throws Exception {call(game,"earnVoid",new Class<?>[]{double.class},1.0);}
    static void advance(RuinedOutpostGame g,double seconds){BladeCombatTest.advance(g,seconds);}
    static RuinedOutpostGame fight(Wisp... enemies) {
        var p=new Player(900,550);p.collectIchor(100);p.transform();p.setUnlimitedBlade(true);
        var g=new RuinedOutpostGame(new RuinedOutpostMap(5),p,List.of(enemies),new Guardian(1600,1000));
        g.setCriticalRollForTesting(()->1);g.begin();g.drainEvents();return g;
    }
    static void hurt(RuinedOutpostGame game)throws Exception {
        call(game,"hurtPlayer",new Class<?>[]{int.class,double.class,double.class},10,game.player().x()+60,game.player().y());
    }
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(true);
        earnedCharge();ultimate();wallsAndInterruption();empowerment();bossRules();
        System.out.println("RainorayKitTest passed: earned charge, two-hit limit, wall collision, aim lock, vulnerability, parry opening and single-use empowerment");
    }
    private static void earnedCharge()throws Exception {
        var g=fight();assert !g.voidSever(1,0)&&g.voidReadiness()==0;
        assert g.riposte();double ichor=g.player().ichor();hurt(g);
        assert g.player().health()==100&&g.voidReadiness()==.5&&g.player().ichor()==ichor;
        advance(g,6.1);assert g.riposte();hurt(g);assert g.voidReadiness()==1;
        advance(g,.4);assert !g.voidSever(0,0)&&g.voidReadiness()==1;
        assert g.requestBladeAttack(true,1,0)&&g.voidActive()&&g.crescents().isEmpty();
        assert g.voidReadiness()==0&&!g.dash(0,1)&&!g.riposte()&&!g.primaryPress(1,0);
        g.pause();g.togglePause();assert !g.voidActive()&&g.voidReadiness()==0;
        var target=new Wisp(980,550,980,980,500);g=fight(target);assert g.riposte();hurt(g);
        assert target.state()==Wisp.State.HURT;advance(g,.8);
        assert target.state()==Wisp.State.HURT : "parry must leave time for a held follow-up";
        // A missed chain must not farm readiness.
        g=fight();for(int i=0;i<4;i++){BladeCombatTest.click(g,1,0);advance(g,RuinedOutpostGame.strikeDuration(i)+.02);}
        assert g.voidReadiness()==0;
    }
    private static void ultimate()throws Exception {
        var targets=new Wisp[]{new Wisp(995,550,995,995,500),new Wisp(1095,550,1095,1095,500),new Wisp(1195,550,1195,1195,500)};
        for(var target:targets)target.openForPunish();
        var g=fight(targets);ready(g);assert g.voidSever(1,0);
        advance(g,.45);for(var target:targets)assert target.health()==500 : "wind-up deals no damage";
        assert g.player().x()==900;
        advance(g,1.1);assert !g.voidActive()&&g.player().x()<=1220.001;
        var events=g.drainEvents();assert events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.VOID_RUPTURE).count()==1;
        assert events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.DAMAGE_DEALT).count()==6 : events;
        for(var target:targets)assert target.health()==420 : "one advance and one rupture per target: "+target.health();
        int health=targets[0].health();advance(g,.5);assert targets[0].health()==health;
        var fedTarget=new Wisp(1095,550,1095,1095,500);fedTarget.openForPunish();
        g=fight(fedTarget);ready(g);set(g,"devoured",true);assert g.voidSever(1,0);
        assert !g.devoured()&&g.voidEmpowered();advance(g,1.4);
        assert fedTarget.health()==402&&fedTarget.state()==Wisp.State.HURT : "one empowered rupture with a bounded ordinary-enemy opening";
        g=fight();ready(g);g.voidSever(1,0);g.aimVoidSever(0,1);advance(g,.49);
        g.aimVoidSever(-1,0);assert g.bladeAimX()==0&&g.bladeAimY()==1 : "aim locks on release";
    }
    private static void wallsAndInterruption()throws Exception {
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double x=wall.centerX()-wall.width()/2-Player.COLLISION_RADIUS-2,y=wall.centerY()-Player.COLLISION_Y_OFFSET;
        var p=new Player(x,y);p.collectIchor(100);p.transform();p.setUnlimitedBlade(true);
        var target=new Wisp(wall.centerX()+wall.width()/2+50,y,wall.centerX()+wall.width()/2+50,wall.centerX()+wall.width()/2+50,500);
        target.openForPunish();var g=new RuinedOutpostGame(map,p,List.of(target),new Guardian(1600,1000));g.begin();ready(g);g.voidSever(1,0);advance(g,1.5);
        assert p.x()<=wall.centerX()-wall.width()/2-Player.COLLISION_RADIUS+.01;
        assert target.health()==500 : "ultimate cannot hit through a wall";
        for(double time:new double[]{.10,.52,.73}) {
            g=fight();ready(g);g.voidSever(1,0);advance(g,time);hurt(g);
            if(time==.52)assert g.player().health()==100&&g.voidActive() : "only the advance is protected";
            else assert g.player().health()<100&&!g.voidActive() : "wind-up/recovery must be punishable";
        }
    }
    @SuppressWarnings("unchecked")
    private static void empowerment()throws Exception {
        var g=fight();var corpses=(List<RuinedOutpostGame.Corpse>)field(g,"corpses");
        var ctor=RuinedOutpostGame.Corpse.class.getDeclaredConstructor(double.class,double.class,boolean.class,CampaignEnemy.Kind.class,int.class);ctor.setAccessible(true);
        for(int i=0;i<4;i++)corpses.add(ctor.newInstance(950.0+i*20,550.0,true,CampaignEnemy.Kind.OUTPOST_SCOUT,1));
        assert g.beelzebub(1,0);advance(g,1.2);
        assert corpses.isEmpty()&&g.devoured()&&g.voidReadiness()==1;
        assert g.primaryPress(1,0);advance(g,.85);g.primaryRelease(1,0);
        assert !g.devoured()&&g.cleaveEmpowered() : "consume exactly once on committed release";
        advance(g,1);g.primaryPress(1,0);advance(g,.85);g.primaryRelease(1,0);
        assert !g.cleaveEmpowered();
        g=fight();ready(g);g.player().setUnlimitedBlade(false);g.player().spendBladeIchor(100);advance(g,1.6);
        assert g.voidReadiness()==1 : "form changes preserve earned readiness";
        g.player().collectIchor(100);assert g.transform();assert g.voidReadiness()==1;
    }
    private static void bossRules()throws Exception {
        // Ground hazards remain a dodge decision even while the parry window is open.
        var g=fight();assert g.riposte();
        call(g,"hurtPlayer",new Class<?>[]{int.class,double.class,double.class,boolean.class},20,920.,550.,false);
        assert g.player().health()<100&&g.voidReadiness()==0&&!g.guarding();
        for(var attack:Guardian.Attack.values()) {
            var boss=new Guardian(1100,550);boss.activate(900,550);
            set(boss,"attack",attack);set(boss,"state",Guardian.State.SLAM);set(boss,"stateTime",0.);
            boss.parried();boolean catchable=attack==Guardian.Attack.TARGET||attack==Guardian.Attack.SWEEP;
            assert (boss.state()==Guardian.State.RECOVER)==catchable : attack;
            if(catchable) {
                for(int i=0;i<200&&boss.state()==Guardian.State.RECOVER;i++)boss.update(.02,900,550);
                assert boss.state()!=Guardian.State.RECOVER : "parry opening must end";
            }
        }
        for(boolean exposed:new boolean[]{false,true}) {
            var boss=new Guardian(1100,550);var player=new Player(900,550);
            player.collectIchor(100);player.transform();player.setUnlimitedBlade(true);
            g=new RuinedOutpostGame(new RuinedOutpostMap(9),player,List.of(),boss);g.begin();g.setCriticalRollForTesting(()->1);
            boss.activate(900,550);set(boss,"state",exposed?Guardian.State.RECOVER:Guardian.State.TELEGRAPH);
            int hp=boss.health();ready(g);assert g.voidSever(1,0);
            // Advance the real skill in isolation so boss AI cannot change its armor state mid-assertion.
            call(g,"updateVoidSever",new Class<?>[]{double.class},.81);
            int damage=hp-boss.health();
            assert exposed?damage>0&&damage<=80:damage==0 : "ultimate must respect armor: "+damage;
        }
    }
    static void set(Object o,String name,Object value)throws Exception {
        var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);
    }
    static Object field(Object o,String name)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
}
