import java.util.List;

public final class TideFeedbackTest {
    public static void main(String[] args) throws Exception {
        releaseUsesActualSpawn();
        naturalFinish(false);
        naturalFinish(true);
        collisionFinishesOnce();
        rejectedAndCancelledCastsStaySilent();
        slashHasNoTideFeedback();
        System.out.println("TideFeedbackTest passed");
    }
    private static RuinedOutpostGame fixture(boolean begin,Wisp... enemies) {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(enemies),new Guardian(1500,600));
        if(begin)game.begin();return game;
    }
    private static List<RuinedOutpostGame.Event> advance(RuinedOutpostGame game,double seconds) {
        for(double remaining=seconds;remaining>1e-8;remaining-=.01)game.update(Math.min(.01,remaining),0,0);
        return game.drainEvents();
    }
    private static long count(List<RuinedOutpostGame.Event> events,RuinedOutpostGame.EventType type) {
        return events.stream().filter(e->e.type()==type).count();
    }
    private static void noTide(List<RuinedOutpostGame.Event> events) {
        assert events.stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.TIDE_RELEASE
                ||e.type()==RuinedOutpostGame.EventType.TIDE_DISSIPATE||e.type()==RuinedOutpostGame.EventType.TIDE_IMPACT);
    }
    private static void releaseUsesActualSpawn() {
        var game=fixture(true);assert game.tideWave(1,0);
        assert count(game.drainEvents(),RuinedOutpostGame.EventType.TIDE_RELEASE)==0 : "no release at input acceptance";
        RuinedOutpostGame.Event release=null;
        for(int i=0;i<18;i++) {
            game.update(.01,1,0);
            for(var event:game.drainEvents())if(event.type()==RuinedOutpostGame.EventType.TIDE_RELEASE) {
                assert release==null&&i==17 : "one release after the existing 0.18-second windup";
                release=event;
                assert release.x()==game.player().x()&&release.y()==game.player().y() : "release is actual spawn, not input position";
            }
        }
        assert release!=null&&release.x()>500&&game.projectiles().size()==1;
        double fixedX=release.x();game.update(.1,1,0);
        assert release.x()==fixedX&&release.x()!=game.player().x() : "source event does not follow moving player";
        assert count(game.drainEvents(),RuinedOutpostGame.EventType.TIDE_RELEASE)==0;
        var wave=game.projectiles().get(0);
        assert wave.damage()==2&&wave.radius()==28&&wave.impulse()==420;
        assert Math.abs(game.tideCooldown()-4.22)<1e-6 : "feedback must not change cooldown";
    }
    private static void naturalFinish(boolean lifetime) throws Exception {
        var game=fixture(true);assert game.tideWave(1,0);advance(game,.18);
        var wave=game.projectiles().get(0);
        if(lifetime) {
            // Slow fixture isolates the lifetime branch; default heavy waves reach their range first.
            var speed=WaterProjectile.class.getDeclaredField("speed");speed.setAccessible(true);speed.setDouble(wave,100);
        }
        var events=advance(game,1.2);
        assert game.projectiles().isEmpty();
        assert count(events,RuinedOutpostGame.EventType.TIDE_DISSIPATE)==1 : "natural ending needs exactly one foam finish";
        assert count(events,RuinedOutpostGame.EventType.TIDE_IMPACT)==0 : "expiry is not a collision";
        var finish=events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.TIDE_DISSIPATE).findFirst().orElseThrow();
        assert finish.x()==wave.x()&&finish.y()==wave.y() : "finish uses actual last simulated position";
        assert lifetime?wave.age()>=.8:wave.age()<.8;
        noTide(advance(game,1));
    }
    private static void collisionFinishesOnce() {
        var enemy=new Wisp(610,400,610,610,20);
        var game=fixture(true,enemy);assert game.tideWave(1,0);
        var events=advance(game,1.4);
        assert count(events,RuinedOutpostGame.EventType.TIDE_RELEASE)==1;
        assert count(events,RuinedOutpostGame.EventType.TIDE_IMPACT)==1;
        assert count(events,RuinedOutpostGame.EventType.TIDE_DISSIPATE)==0;
        assert enemy.health()==18&&game.projectiles().isEmpty() : "existing damage and non-piercing stop preserved";
        noTide(advance(game,.2));
        var wall=fixture(true);wall.player().relocate(155,400);assert wall.tideWave(-1,0);
        var wallEvents=advance(wall,1.2);
        assert count(wallEvents,RuinedOutpostGame.EventType.TIDE_IMPACT)==1;
        assert count(wallEvents,RuinedOutpostGame.EventType.TIDE_DISSIPATE)==0;
    }
    private static void rejectedAndCancelledCastsStaySilent() {
        var intro=fixture(false);assert !intro.tideWave(1,0);noTide(advance(intro,1));
        var paused=fixture(true);paused.pause();assert !paused.tideWave(1,0);noTide(advance(paused,1));
        var zero=fixture(true);assert !zero.tideWave(0,0);noTide(advance(zero,1));
        for(String action:new String[]{"dash","transform","death"}) {
            var game=fixture(true);assert game.tideWave(1,0);advance(game,.07);
            switch(action) {
                case "dash" -> {assert game.dash(1,0);}
                case "transform" -> {game.player().collectIchor(100);assert game.transform();}
                case "death" -> game.player().hurt(99);
            }
            noTide(advance(game,1));assert game.projectiles().isEmpty();
        }
        var cooling=fixture(true);assert cooling.tideWave(1,0);advance(cooling,1.2);
        assert !cooling.tideWave(1,0);noTide(advance(cooling,.2));
        var blade=fixture(true);blade.player().collectIchor(100);assert blade.transform();
        assert !blade.tideWave(1,0);noTide(advance(blade,1));
    }
    private static void slashHasNoTideFeedback() {
        var empty=fixture(true);assert empty.attack(1,0);noTide(advance(empty,1.2));
        var target=fixture(true,new Wisp(610,400,610,610,20));assert target.attack(1,0);
        var events=advance(target,1.2);noTide(events);
        assert count(events,RuinedOutpostGame.EventType.WATER_IMPACT)==1;
    }
}
