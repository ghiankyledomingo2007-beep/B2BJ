import java.util.List;

public final class SummonSafetyTest {
    public static void main(String[] args) {
        var map=new RuinedOutpostMap(9);
        var p=new Player(500,600);
        var boss=new Guardian(map.guardianX(),map.guardianY());
        var game=new RuinedOutpostGame(map,p,List.of(),boss);
        game.begin();boss.activate(p.x(),p.y());
        int previous=0;
        for(int i=0;i<3000;i++) {
            game.update(0.01,0,0);p.heal();
            int count=game.scouts().size();
            if(count>previous) {
                Wisp add=game.scouts().get(count-1);
                assert Math.hypot(add.x()-p.x(),add.y()-p.y())>=180 : "summon cannot appear on player";
                for(int j=0;j<count-1;j++)if(game.scouts().get(j).alive())
                    assert Math.hypot(add.x()-game.scouts().get(j).x(),add.y()-game.scouts().get(j).y())>=80
                            : "new summons must have separate spawn positions";
                previous=count;
            }
            assert count<=2 : "phase one summon pressure must stay bounded";
        }
        assert previous==2;
        var a=new Wisp(600,500,500,800,2);var b=new Wisp(600,500,500,800,2);
        game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(1200,500),List.of(a,b),new Guardian(1600,800));
        game.begin();
        for(int i=0;i<100;i++)game.update(0.01,0,0);
        assert Math.hypot(a.x()-b.x(),a.y()-b.y())>35 : "game loop must separate idle overlaps";
        System.out.println("SummonSafetyTest passed");
    }
}
