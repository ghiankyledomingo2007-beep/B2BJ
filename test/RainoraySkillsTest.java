import java.util.List;

public final class RainoraySkillsTest {
    public static void main(String[] args) throws Exception {
        costsAndStates();
        projectileCollision();
        riposteWindow();
        System.out.println("RainoraySkillsTest passed");
    }
    private static RuinedOutpostGame game(RuinedOutpostMap map, double x, double y, Wisp... enemies) {
        var game=new RuinedOutpostGame(map,new Player(x,y),List.of(enemies),new Guardian(1300,700));
        game.begin();return game;
    }
    private static void blade(RuinedOutpostGame game) {
        game.player().collectIchor(100);assert game.transform();game.drainEvents();
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double remaining=seconds;remaining>1e-8;remaining-=.01)game.update(Math.min(.01,remaining),0,0);
    }
    private static void costsAndStates() {
        var game=game(new RuinedOutpostMap(),300,300);
        assert !game.ichorCrescent(1,0)&&!game.riposte() : "human skills unavailable in Blob";
        blade(game);assert !game.ichorCrescent(0,0);assert game.player().ichor()==100;
        assert game.ichorCrescent(1,0);
        assert close(game.player().ichor(),92)&&close(game.player().bladeSeconds(),11.04);
        assert !game.ichorCrescent(1,0);
        assert game.crescentCooldown()==5;
        game.pause();advance(game,2);assert game.crescentCooldown()==5;
        assert !game.riposte();game.togglePause();advance(game,.5);
        assert close(game.crescentCooldown(),4.5);
        assert game.riposte();assert !game.attack(1,0)&&!game.ichorCrescent(1,0);
        double guard=game.guardProgress();game.pause();advance(game,1);
        assert game.guardProgress()==guard;game.togglePause();
        assert game.dash(1,0)&&!game.guarding() : "dash cancels guard rather than stacking immunity";
        var low=game(new RuinedOutpostMap(),300,300);blade(low);advance(low,11.1);
        double before=low.player().ichor();assert before<8;
        assert !low.riposte()&&!low.ichorCrescent(1,0)&&low.player().ichor()==before;
        low.restart();assert low.crescentCooldown()==0&&low.riposteCooldown()==0;
        var dead=game(new RuinedOutpostMap(),300,300);blade(dead);dead.player().hurt(99);
        assert !dead.riposte()&&!dead.ichorCrescent(1,0);
    }
    private static void projectileCollision() {
        var enemy=new Wisp(430,300,430,430,20);
        var game=game(new RuinedOutpostMap(),300,300,enemy);blade(game);
        assert game.ichorCrescent(1,0);assert enemy.health()==20;
        advance(game,.04);assert enemy.health()==20 : "crescent must travel before hitting";
        advance(game,.4);assert enemy.health()==17 : "one finite crescent deals three damage once";
        assert game.crescents().isEmpty();
        assert game.drainEvents().stream().anyMatch(e->e.type()==RuinedOutpostGame.EventType.CRESCENT_IMPACT);
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double y=wall.centerY()-Player.COLLISION_Y_OFFSET;
        var target=new Wisp(wall.centerX()+100,y,wall.centerX()+100,wall.centerX()+100,20);
        var blocked=game(map,wall.centerX()-100,y,target);blade(blocked);
        assert blocked.ichorCrescent(1,0);advance(blocked,.3);
        assert target.health()==20&&blocked.crescents().isEmpty() : "solid terrain stops swept arc";
        var empty=game(new RuinedOutpostMap(),300,300);blade(empty);
        assert empty.ichorCrescent(1,0);advance(empty,1);assert empty.crescents().isEmpty();
    }
    private static void riposteWindow() throws Exception {
        var enemy=new Wisp(370,300,370,370,20);
        var game=game(new RuinedOutpostMap(),300,300,enemy);blade(game);
        assert game.riposte();assert game.guarding();
        hit(game,1,enemy.x(),enemy.y());
        assert game.player().healthValue()==5&&enemy.health()==17;
        assert !game.guarding()&&!game.riposte();
        hit(game,1,enemy.x(),enemy.y());
        assert game.player().healthValue()==4.25 : "only one hit is negated; no lingering immunity";
        assert game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.RIPOSTE_COUNTER).count()==1;
        var expired=game(new RuinedOutpostMap(),300,300);blade(expired);
        assert expired.riposte();advance(expired,.31);assert !expired.guarding();
        hit(expired,2,300,300);assert expired.player().healthValue()==3.5;
        var invalid=game(new RuinedOutpostMap(),300,300);blade(invalid);assert invalid.riposte();
        hit(invalid,0,300,300);assert invalid.guarding() : "non-damage cannot consume parry";
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double y=wall.centerY()-24;
        var shielded=new Wisp(wall.centerX()+35,y,wall.centerX()+35,wall.centerX()+35,20);
        var cover=game(map,wall.centerX()-35,y,shielded);blade(cover);assert cover.riposte();
        hit(cover,1,cover.player().x(),y);assert shielded.health()==20 : "counter cannot hit through terrain";
    }
    private static void hit(RuinedOutpostGame game,int damage,double x,double y) throws Exception {
        var method=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);
        method.setAccessible(true);method.invoke(game,damage,x,y);
    }
    private static boolean close(double a,double b){return Math.abs(a-b)<1e-6;}
}
