import java.util.ArrayList;
import java.util.List;

/** Directional crowd control: one momentum transfer per body, with solid cover retained. */
public final class TideCrowdTest {
    public static void main(String[] args) throws Exception {
        for (int[] aim : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}})
            crowd(aim[0],aim[1]);
        cover();
        System.out.println("TideCrowdTest passed: 8 headings, repeat contacts, focused M1, solid cover");
    }
    private static void crowd(int x,int y) throws Exception {
        double length=Math.hypot(x,y),dx=x/length,dy=y/length;
        List<Wisp> enemies=new ArrayList<>();
        for(double[] position:new double[][]{{100,-48},{100,48},{200,0},{100,125},{-110,0}}) {
            double ex=700+dx*position[0]-dy*position[1],ey=500+dy*position[0]+dx*position[1];
            enemies.add(new Wisp(ex,ey,ex,ex,20));
        }
        var game=fixture(new RuinedOutpostMap(),new Player(700,500),enemies);
        var wave=new WaterProjectile(700,500,x,y,WaterProjectile.Kind.TIDE);
        waves(game).add(wave);
        advanceWater(game,.7);
        for(int i=0;i<3;i++)assert enemies.get(i).health()==18
                : "Tide must reach each body once across its broad front: "+x+","+y+" target "+i;
        assert enemies.get(3).health()==20&&enemies.get(4).health()==20 : "outside front/behind caster stays safe";
        advanceWater(game,.7);
        for(int i=0;i<3;i++)assert enemies.get(i).health()==18 : "overlapping wave cannot repeatedly hurt/stagger";
        assert game.projectiles().isEmpty();
        long contacts=game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.TIDE_IMPACT).count();
        assert contacts==3 : "each struck body needs its own anchored impact";

        var first=new Wisp(800,500,800,800,20);var second=new Wisp(900,500,900,900,20);
        var focused=fixture(new RuinedOutpostMap(),new Player(700,500),List.of(first,second));
        waves(focused).add(new WaterProjectile(700,500,1,0,WaterProjectile.Kind.FINISHER));
        advanceWater(focused,1);
        assert first.health()==18&&second.health()==20 : "finisher remains focused, not a free crowd wave";
    }
    private static void cover() throws Exception {
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double y=wall.centerY()-Player.COLLISION_Y_OFFSET;
        var target=new Wisp(wall.centerX()+70,y,wall.centerX()+70,wall.centerX()+70,20);
        var game=fixture(map,new Player(wall.centerX()-120,y),List.of(target));
        waves(game).add(new WaterProjectile(game.player().x(),y,1,0,WaterProjectile.Kind.TIDE));
        advanceWater(game,1);
        assert target.health()==20&&game.projectiles().isEmpty() : "piercing bodies never means piercing terrain";
    }
    @SuppressWarnings("unchecked")
    private static List<WaterProjectile> waves(RuinedOutpostGame game) throws Exception {
        var field=RuinedOutpostGame.class.getDeclaredField("projectiles");field.setAccessible(true);
        return (List<WaterProjectile>)field.get(game);
    }
    private static void advanceWater(RuinedOutpostGame game,double duration) throws Exception {
        var method=RuinedOutpostGame.class.getDeclaredMethod("updateWater",double.class);method.setAccessible(true);
        for(double remaining=duration;remaining>1e-8;remaining-=.01)method.invoke(game,Math.min(.01,remaining));
    }
    private static RuinedOutpostGame fixture(RuinedOutpostMap map,Player player,List<Wisp> enemies) {
        var game=new RuinedOutpostGame(map,player,enemies,new Guardian(1500,900));game.begin();return game;
    }
}
