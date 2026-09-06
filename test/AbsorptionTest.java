import java.util.List;
import java.util.Set;

public final class AbsorptionTest {
    public static void main(String[] args) throws Exception {
        passiveRegeneration();
        dashCooldownTracksSimulation();
        realKillLeavesEdibleRemains();
        consumeOnceAndCancel();
        limitsAndTerrain();
        System.out.println("AbsorptionTest passed");
    }
    private static void dashCooldownTracksSimulation() {
        var game=empty(new RuinedOutpostMap(),300,300);game.begin();
        assert game.player().dashCooldown()==0;
        assert game.dash(1,0);assert game.player().dashCooldown()==Player.DASH_COOLDOWN;
        advance(game,.1);assert close(game.player().dashCooldown(),.3);
        game.pause();advance(game,2);assert close(game.player().dashCooldown(),.3);
        game.togglePause();advance(game,.1);
        assert !game.player().dashing()&&close(game.player().dashCooldown(),.2)
                : "HUD must retain cooldown after dash movement ends";
        advance(game,.2);assert game.player().dashCooldown()==0;
    }
    private static void realKillLeavesEdibleRemains() {
        var enemy=new Wisp(375,300,375,375,1);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(300,300),
                List.of(enemy),new Guardian(900,300));game.begin();
        assert game.attack(1,0);advance(game,.6);
        assert !enemy.alive()&&game.corpses().size()==1 : "real killing strike must create remains";
        assert game.player().ichor()>=10 : "normal kill pickup is retained";
        assert game.interact();advance(game,.81);
        assert game.corpses().isEmpty()&&game.player().ichor()>=15;
    }
    private static void passiveRegeneration() {
        var game=empty(new RuinedOutpostMap(),300,300);
        game.update(.5,0,0);
        assert game.player().ichor()==0 : "story overlay must freeze regeneration";
        game.begin();advance(game,5);
        assert close(game.player().ichor(),3) : "Blob regenerates 0.6 Ichor per second";
        game.pause();advance(game,2);
        assert close(game.player().ichor(),3);
        game.togglePause();game.player().collectIchor(100);assert game.transform();
        advance(game,1);
        assert close(game.player().ichor(),100-100.0/12) : "Blade drain must not gain passive Ichor";
        advance(game,11.1);assert game.player().recovering();
        assert game.player().ichor()==0;
        advance(game,.5);assert game.player().ichor()==0 : "reversion blocks regen";
        game.player().hurt(99);advance(game,2);assert game.player().ichor()==0;
        var full=empty(new RuinedOutpostMap(),300,300);full.begin();full.player().collectIchor(100);
        advance(full,2);assert full.player().ichor()==100;
    }
    private static void consumeOnceAndCancel() throws Exception {
        var game=corpse(false);var player=game.player();player.hurt(1);
        assert game.corpses().size()==1 && game.corpses().get(0).healing();
        assert game.interact();assert game.absorptionTarget()!=null;
        double ichor=player.ichor();advance(game,.4);
        assert game.corpses().get(0).x()<330 : "absorbed remains must actually draw toward the player";
        assert close(player.healthValue(),4) : "no healing before channel completion";
        assert game.absorptionProgress()>.4 && game.absorptionProgress()<.6;
        assert !game.interact() : "repeat input cannot restart channel";
        game.pause();double progress=game.absorptionProgress();advance(game,1);
        assert game.absorptionProgress()==progress;
        game.togglePause();advance(game,.41);
        assert close(player.healthValue(),4.5) : "ordinary corpse heals half a heart";
        assert player.ichor()>ichor+5 && player.ichor()<ichor+5.5;
        assert game.corpses().isEmpty() && game.absorptionTarget()==null;
        assert !game.interact() : "corpse cannot be consumed twice";
        var events=game.drainEvents();
        assert events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.ABSORBED).count()==1;
        assert events.stream().filter(e->e.type().name().equals("HEALED")).count()==1 : "healing needs distinct feedback";

        for(String action:new String[]{"move","dash","attack","tide","transform","hurt"}) {
            game=corpse(false);player=game.player();player.hurt(1);
            assert game.interact();advance(game,.3);
            switch(action) {
                case "move" -> game.update(.01,1,0);
                case "dash" -> { assert game.dash(1,0); }
                case "attack" -> { assert game.attack(1,0); }
                case "tide" -> { assert game.tideWave(1,0); }
                case "transform" -> { player.collectIchor(100);assert game.transform(); }
                case "hurt" -> { player.move(0,0,.7,game.map());assert player.hurt(1);game.update(.01,0,0); }
            }
            assert game.absorptionTarget()==null : action+" must cancel absorption";
            double corpseX=game.corpses().get(0).x(),corpseY=game.corpses().get(0).y();
            advance(game,.6);assert game.corpses().size()==1 : "cancelled corpse remains available";
            assert game.corpses().get(0).x()==corpseX&&game.corpses().get(0).y()==corpseY
                    : "cancelled remains must not snap back";
            assert player.healthValue()<=4;
        }
    }
    private static void limitsAndTerrain() throws Exception {
        var add=corpse(true);add.player().hurt(1);assert !add.corpses().get(0).healing();
        assert add.interact();advance(add,.81);
        assert add.player().healthValue()==4 : "infinite boss adds cannot farm health";
        assert add.drainEvents().stream().noneMatch(e->e.type().name().equals("HEALED"));
        var full=corpse(false);full.player().collectIchor(100);
        assert !full.interact() && full.corpses().size()==1 : "do not waste corpse at full health/energy";
        advance(full,21);assert full.corpses().isEmpty();

        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        var blocked=empty(map,wall.centerX()-35,wall.centerY()-24);blocked.begin();
        addCorpse(blocked,wall.centerX()+35,wall.centerY()-24,false);
        assert !blocked.interact() : "cannot eat through solid terrain";
        double wallX=wall.centerX()-wall.width()/2-Player.COLLISION_RADIUS;
        var pressed=empty(map,wallX,wall.centerY()-24);pressed.begin();
        addCorpse(pressed,wallX-30,wall.centerY()-24,false);advance(pressed,.1);
        assert pressed.interact();advance(pressed,.2);pressed.update(.01,1,0);
        assert pressed.player().x()==wallX && pressed.absorptionTarget()==null
                : "movement intent cancels absorption even when wall prevents displacement";
        var far=corpse(false);far.player().relocate(700,300);assert !far.interact();
        var dead=corpse(false);dead.player().hurt(99);assert !dead.interact();
        var cap=empty(new RuinedOutpostMap(),300,300);cap.begin();
        for(int i=0;i<25;i++)addCorpse(cap,330,300,false);
        assert cap.corpses().size()<=16 : "corpse collection must remain bounded";
        cap.restart();assert cap.corpses().isEmpty() && cap.absorptionTarget()==null;
        Player p=new Player(0,0);p.hurt(1);p.heal(Double.NaN);p.heal(-1);
        assert p.healthValue()==4;p.heal(99);assert p.healthValue()==5;
        p=new Player(0,0);p.hurt(99);p.heal(1);assert !p.alive() : "partial healing cannot revive";
    }
    private static RuinedOutpostGame corpse(boolean add) throws Exception {
        var game=empty(new RuinedOutpostMap(),300,300);game.begin();
        addCorpse(game,330,300,add);advance(game,.6);
        return game;
    }
    @SuppressWarnings("unchecked")
    private static void addCorpse(RuinedOutpostGame game,double x,double y,boolean add) throws Exception {
        var enemy=new Wisp(x,y,x,x,1);
        if(add) {
            var field=RuinedOutpostGame.class.getDeclaredField("bossAdds");field.setAccessible(true);
            ((Set<Wisp>)field.get(game)).add(enemy);
        }
        enemy.hurt(1);
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);
        hit.setAccessible(true);hit.invoke(game,enemy,true);
    }
    private static RuinedOutpostGame empty(RuinedOutpostMap map,double x,double y) {
        return new RuinedOutpostGame(map,new Player(x,y),List.of(),new Guardian(900,300));
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double remaining=seconds;remaining>1e-8;remaining-=.01)game.update(Math.min(.01,remaining),0,0);
    }
    private static boolean close(double a,double b) { return Math.abs(a-b)<1e-6; }
}
