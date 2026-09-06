import java.util.List;

public final class SpitterCombatTest {
    public static void main(String[] args) {
        var game=fixture(300,300,550,300);
        game.update(.5,0,0);
        assert game.hostileProjectiles().isEmpty() : "tell must precede projectiles";
        for(int i=0;i<30;i++)game.update(.01,0,0);
        assert game.hostileProjectiles().size()==3 : "one committed fan per release";
        var shot=game.hostileProjectiles().get(0);double x=shot.x();
        game.pause();game.update(.5,0,0);assert shot.x()==x;
        game.togglePause();
        for(int i=0;i<100;i++)game.update(.01,0,0);
        assert game.player().healthValue()==4 : "single volley cannot shotgun three simultaneous damage hits";
        game.restart();assert game.hostileProjectiles().isEmpty();

        var dodge=fixture(300,300,550,300);
        for(int i=0;i<145;i++)dodge.update(.01,0,0);
        assert dodge.dash(0,1);
        for(int i=0;i<25;i++)dodge.update(.01,0,0);
        assert dodge.player().healthValue()==5 : "telegraphed volley has a dodge window";
        var dead=fixture(300,300,550,300);dead.scouts().get(0).hurt(99);
        dead.update(.5,0,0);dead.update(.5,0,0);
        assert dead.hostileProjectiles().isEmpty();

        var wallMap=new RuinedOutpostMap(2);var wall=wallMap.barriers().get(0);
        var wallGame=new RuinedOutpostGame(wallMap,new Player(wall.centerX()-130,wall.centerY()-24),
                List.of(new Wisp(wall.centerX()+130,wall.centerY()-24,wall.centerX()+130,wall.centerX()+130,3,Wisp.Role.SPITTER)),new Guardian(900,300));
        wallGame.begin();for(int i=0;i<100;i++)wallGame.update(.01,0,0);
        assert wallGame.hostileProjectiles().isEmpty()&&wallGame.player().healthValue()==5;
        System.out.println("SpitterCombatTest passed");
    }
    private static RuinedOutpostGame fixture(double px,double py,double x,double y) {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(px,py),
                List.of(new Wisp(x,y,x,x,3,Wisp.Role.SPITTER)),new Guardian(900,300));game.begin();return game;
    }
}
