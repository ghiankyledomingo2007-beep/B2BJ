import java.util.List;

public final class GuardianDeathLoopTest {
    public static void main(String[] args) {
        var guardian=new Guardian(900,550);
        var player=new Player(780,550);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(9),player,List.of(),guardian);
        game.begin();game.story().enterGatehouse();guardian.activate(player.x(),player.y());
        for(int i=0;i<150&&guardian.state()!=Guardian.State.RECOVER;i++)game.update(.01,0,0);
        assert guardian.state()==Guardian.State.RECOVER;
        assert guardian.hurt(Guardian.MAX_HEALTH-1);
        assert game.attack(1,0);
        for(int i=0;i<80&&guardian.alive();i++)game.update(.01,0,0);
        assert !guardian.alive()&&game.cleared(9) : "real projectile kill must reach victory bookkeeping";
        assert game.story().phase()==OutpostStory.Phase.ESCAPE&&!game.blocked()
                : "boss victory leaves gameplay running for the escape";
        assert game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.ENEMY_DEFEATED).count()==1;

        game.pause();game.update(.5,0,0);
        assert guardian.stateSeconds()==0&&guardian.visible() : "pause freezes the death pose";
        game.togglePause();
        int seen=1<<guardian.animationFrame(), impacts=guardian.impactNumber();
        double health=player.healthValue(),x=guardian.x(),y=guardian.y();
        for(int i=0;i<70;i++) {
            double before=guardian.stateSeconds();
            game.update(.01,0,0);
            if(before>0)assert Math.abs(guardian.stateSeconds()-before-.01)<1e-8
                    : "death clock advances once per physics step";
            if(guardian.visible())seen|=1<<guardian.animationFrame();
        }
        assert seen==255 : "normal game.update must play all eight death poses, not freeze after bossDefeated";
        assert !guardian.visible() : "normal game.update must retire the dead boss";
        for(int i=0;i<700;i++)game.update(.01,0,0);
        assert guardian.impactNumber()==impacts&&guardian.x()==x&&guardian.y()==y;
        assert player.healthValue()==health&&game.scouts().stream().noneMatch(Wisp::alive)
                : "death animation cannot restart combat or summon adds";
        assert game.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.GUARDIAN_SLAM
                ||e.type()==RuinedOutpostGame.EventType.ENEMY_DEFEATED);
        System.out.println("GuardianDeathLoopTest passed");
    }
}
