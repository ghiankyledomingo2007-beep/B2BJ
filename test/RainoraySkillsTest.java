import java.util.List;

public final class RainoraySkillsTest {
    public static void main(String[] args) throws Exception {
        costsAndStates();
        projectileCollision();
        riposteWindow();
        realDamageGateways();
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
        var resource=new Player(300,300);
        assert !resource.spendBladeIchor(8);resource.collectIchor(100);assert resource.transform();
        for(double amount:new double[]{Double.NaN,Double.POSITIVE_INFINITY,0,-1,101})
            assert !resource.spendBladeIchor(amount)&&resource.ichor()==100;
        assert resource.spendBladeIchor(100)&&resource.ichor()==0&&resource.recovering();
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
        advance(invalid,.4);assert !invalid.guarding()&&!invalid.riposte();
        advance(invalid,5.61);assert invalid.riposte() : "guard becomes reusable only after six seconds";
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
    @SuppressWarnings("unchecked")
    private static void realDamageGateways() throws Exception {
        var lunging=new Wisp(350,300,350,350,20);
        var meleeGame=game(new RuinedOutpostMap(),300,300,lunging);blade(meleeGame);
        for(int i=0;i<100&&!(lunging.state()==Wisp.State.TELEGRAPH&&lunging.stateSeconds()>.22);i++)
            advance(meleeGame,.01);
        assert lunging.state()==Wisp.State.TELEGRAPH;
        assert meleeGame.riposte();advance(meleeGame,.25);
        assert meleeGame.player().healthValue()==5&&lunging.health()==17&&!meleeGame.guarding()
                : "real enemy lunge is negated and countered once";
        var shotGame=game(new RuinedOutpostMap(),300,300);blade(shotGame);assert shotGame.riposte();
        var shots=RuinedOutpostGame.class.getDeclaredField("hostileProjectiles");shots.setAccessible(true);
        ((List<EnemyProjectile>)shots.get(shotGame)).add(new EnemyProjectile(345,300,-1,0));
        advance(shotGame,.08);
        assert shotGame.player().healthValue()==5&&!shotGame.guarding()&&shotGame.hostileProjectiles().isEmpty()
                : "real hostile projectile must consume guard, stop and deal no damage";
        var map=new RuinedOutpostMap(9);
        var bossGame=game(map,map.spawnX(),map.spawnY());blade(bossGame);
        bossGame.guardian().activate(bossGame.player().x(),bossGame.player().y());
        advance(bossGame,.72);assert bossGame.riposte();advance(bossGame,.25);
        assert bossGame.guardian().impactNumber()==1;
        assert bossGame.player().healthValue()==5&&!bossGame.guarding()
                : "real Guardian slam must pass through the same one-shot guard";
        assert bossGame.drainEvents().stream().anyMatch(e->e.type()==RuinedOutpostGame.EventType.RIPOSTE_COUNTER);
        var anchor=game(new RuinedOutpostMap(),300,300);
        var enemy=new Wisp(365,300,365,365,1);enemy.hurt(1);
        var defeated=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class);
        defeated.setAccessible(true);defeated.invoke(anchor,enemy);advance(anchor,.1);
        assert anchor.interact();advance(anchor,.4);
        assert anchor.absorptionAnchorX()==365&&anchor.absorptionAnchorY()==300;
        assert anchor.player().x()==300&&anchor.player().y()==300;
        assert anchor.absorptionTarget().x()<365 : "engulf anchor stays fixed while remains pull under body";
    }
    private static boolean close(double a,double b){return Math.abs(a-b)<1e-6;}
}
