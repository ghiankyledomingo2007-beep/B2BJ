import java.util.List;

public final class SlimeCastingTest {
    public static void main(String[] args) {
        briefSlowdownAndCadence();
        pauseAndCancellation();
        movementDoesNotDependOnFrameSize();
        System.out.println("SlimeCastingTest passed: cadence, brief slowdown, pause, dash, transform, reset");
    }
    private static RuinedOutpostGame fixture() {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(),new Guardian(1500,900));
        game.begin();return game;
    }
    private static void briefSlowdownAndCadence() {
        for(boolean tide:new boolean[]{false,true}) {
            var game=fixture();
            assert tide?game.tideWave(1,0):game.attack(1,0);
            game.update(.1,1,0);
            close(game.player().x()-500,26*.65);
            game.update(.11,0,0);
            double x=game.player().x();game.update(.1,1,0);
            close(game.player().x()-x,26);
            game.update(.28,0,0);
            assert !game.attack(1,0) : "movement recovers before the shot cooldown";
            game.update(.02,0,0);
            assert game.attack(1,0) : "next cast available after 0.60 seconds";
        }
        var wave=new WaterProjectile(0,0,1,0,false);
        assert wave.damage()==18 : "basic water stays weaker than Blade";
    }
    private static void pauseAndCancellation() {
        var game=fixture();assert game.attack(1,0);
        game.pause();game.update(.5,1,0);close(game.player().x(),500);
        game.togglePause();game.update(.1,1,0);close(game.player().x()-500,26*.65);
        assert game.dash(1,0);double x=game.player().x();game.update(.16,1,0);
        close(game.player().x()-x,Player.DASH_SPEED*.16);
        x=game.player().x();game.update(.1,1,0);close(game.player().x()-x,26);

        game=fixture();assert game.attack(1,0);game.player().collectIchor(100);assert game.transform();
        game.update(.1,1,0);close(game.player().x()-500,26*Player.BLADE_SPEED_MULTIPLIER);
        assert game.projectiles().isEmpty() : "transform cancels the pending cast";
        for(boolean refill:new boolean[]{false,true}) {
            var player=new Player(500,400);assert player.startAttack();
            if(refill)player.refillForTesting();else player.relocate(500,400);
            player.move(1,0,.1,new RuinedOutpostMap());close(player.x()-500,26);
        }
    }
    private static void movementDoesNotDependOnFrameSize() {
        var map=new RuinedOutpostMap();var whole=new Player(500,400);var split=new Player(500,400);
        assert whole.startAttack()&&split.startAttack();
        whole.move(1,0,.3,map);
        for(int i=0;i<30;i++)split.move(1,0,.01,map);
        close(whole.x(),split.x());close(whole.x()-500,260*(.2*.65+.1));
    }
    private static void close(double actual,double expected) {
        assert Math.abs(actual-expected)<1e-7 : "expected "+expected+", got "+actual;
    }
}
