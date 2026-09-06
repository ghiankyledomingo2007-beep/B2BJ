import java.util.List;

public final class IchorMagnetTest {
    public static void main(String[] args) throws Exception {
        var game=fixture(new RuinedOutpostMap(),300,300,420,300,10);
        var drop=game.drops().get(0);
        game.update(.1,0,0);
        assert drop.x()<420&&drop.x()>330 : "nearby Ichor must accelerate toward player, not teleport";
        assert Math.abs(game.player().ichor()-.06)<1e-8 && drop.amount()==10
                : "only passive regeneration applies before pickup arrival";
        double x=drop.x();game.pause();game.update(.5,0,0);
        assert drop.x()==x : "pause freezes attraction";
        game.togglePause();for(int i=0;i<100;i++)game.update(.01,0,0);
        assert game.drops().isEmpty()&&Math.abs(game.player().ichor()-10.66)<1e-8;
        assert game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.PICKUP).count()==1;

        var far=fixture(new RuinedOutpostMap(),300,300,600,300,10);
        far.update(.5,0,0);assert far.drops().get(0).x()==600;
        var full=fixture(new RuinedOutpostMap(),300,300,300,300,10);
        full.player().collectIchor(100);full.update(.1,0,0);
        assert full.drops().size()==1 : "full meter cannot erase drops";
        var partial=fixture(new RuinedOutpostMap(),300,300,300,300,10);
        partial.player().collectIchor(95);partial.update(.1,0,0);
        assert partial.player().ichor()==100&&partial.drops().size()==1;
        assert partial.drops().get(0).amount()==5 : "capacity overflow must remain available";
        assert partial.transform();for(int i=0;i<100;i++)partial.update(.01,0,0);
        assert partial.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.PICKUP).count()<=2
                : "fractional Blade top-ups must not spam pickup sound/effects every physics tick";

        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        assert !map.clearWaterLine(wall.centerX()-60,wall.centerY(),wall.centerX()+60,wall.centerY(),4);
        double cy=wall.centerY()-Player.COLLISION_Y_OFFSET;
        var blocked=fixture(map,wall.centerX()-60,cy,wall.centerX()+60,cy,10);
        blocked.update(.5,0,0);
        assert blocked.drops().get(0).x()==wall.centerX()+60&&blocked.drops().get(0).amount()==10
                &&Math.abs(blocked.player().ichor()-.3)<1e-8
                : "magnet and collection cannot cross cover";
        var dead=fixture(new RuinedOutpostMap(),300,300,300,300,10);
        dead.player().hurt(99);dead.update(.1,0,0);
        assert dead.player().ichor()==0;
        System.out.println("IchorMagnetTest passed");
    }
    @SuppressWarnings("unchecked")
    private static RuinedOutpostGame fixture(RuinedOutpostMap map,double px,double py,double x,double y,double amount) throws Exception {
        var game=new RuinedOutpostGame(map,new Player(px,py),List.of(),new Guardian(900,300));game.begin();
        var field=RuinedOutpostGame.class.getDeclaredField("drops");field.setAccessible(true);
        ((List<RuinedOutpostGame.IchorDrop>)field.get(game)).add(new RuinedOutpostGame.IchorDrop(x,y,amount));
        return game;
    }
}
