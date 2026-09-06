import java.util.List;

public final class RuinedOutpostGameTest {
    public static void main(String[] args) {
        roomsAndCheckpoint();
        delayedHitAndPause();
        armorDrainAndRecovery();
        oneDamageEventPerLunge();
        walkThroughOpenBreach();
        System.out.println("RuinedOutpostGameTest passed");
    }
    private static void roomsAndCheckpoint() {
        RuinedOutpostGame game=new RuinedOutpostGame();
        assert !game.attack(1,0);
        game.begin();
        travel(game,1);
        assert game.scouts().size()==2;
        game.player().relocate(game.map().worldWidth()-180,game.map().spawnY());
        assert !game.interact() : "combat must lock exits";
        for(int room=1;room<=8;room++) {
            assert game.map().room()==room;
            clearWithAttacks(game);
            if(room==2) {
                travel(game,10);
                assert game.visited(10);
                travel(game,2);
                assert game.scouts().isEmpty() : "cleared rooms must stay cleared";
            }
            if(room==4) {
                assert game.scouts().isEmpty() && game.player().ichor()==100;
                assert game.transform() : "first transformation has a safe tutorial room";
            }
            if(room==7) {
                game.player().hurt(100);
                game.update(0.02,0,0);
                assert game.story().phase()==OutpostStory.Phase.DEAD;
                game.restart();
                assert game.map().room()==7 && game.player().health()==5;
                assert game.visited(10)&&game.cleared(3) : "checkpoint retains progress";
            }
            travel(game,room+1);
        }
        assert game.map().room()==9;
        assert game.story().phase()==OutpostStory.Phase.GUARDIAN;
        assert !game.map().gateOpen();
        // End-of-fight fixture. The real final strike must release collision and exits.
        game.guardian().hurt(Guardian.MAX_HEALTH-1);
        while(game.guardian().state()!=Guardian.State.RECOVER) {
            game.player().relocate(640,600);
            game.update(0.05,0,0);
        }
        game.player().relocate(game.guardian().x()-100,game.guardian().y());
        assert game.attack(1,0);
        game.update(0.2,0,0);
        assert !game.guardian().alive();
        assert game.map().gateOpen();
        travel(game,11);
        travel(game,9);
        assert !game.guardian().alive() : "boss cannot respawn on backtracking";
        game.player().relocate(game.map().gateX()-40,game.map().gateY());
        assert game.interact();
        assert game.story().phase()==OutpostStory.Phase.COMPLETE;
        assert game.drainEvents().stream().anyMatch(e->e.type()==RuinedOutpostGame.EventType.VICTORY);
    }
    private static void clearWithAttacks(RuinedOutpostGame game) {
        for(Wisp enemy:game.scouts()) {
            for(int attempt=0;enemy.alive()&&attempt<20;attempt++) {
                game.player().relocate(enemy.x()-(game.player().bladeForm()?75:200),enemy.y());
                game.attack(1,0);
                game.update(0.5,0,0);
                game.update(0.06,0,0);
                assert game.player().alive() : "scripted combat died in room "+game.map().room();
            }
            assert !enemy.alive() : "target never became hittable";
            game.player().relocate(enemy.x(),enemy.y());
            game.update(0.05,0,0);
        }
        assert game.roomClear();
    }
    private static void travel(RuinedOutpostGame game,int destination) {
        var door=game.map().doors().stream().filter(d->d.destination()==destination).findFirst().orElseThrow();
        game.player().relocate(door.x(),door.y());
        assert game.interact() : "cannot enter "+destination+" from "+game.map().room();
    }
    private static void delayedHitAndPause() {
        Player player=new Player(300,300);
        Wisp enemy=new Wisp(375,300,375,375,2);
        RuinedOutpostGame game=new RuinedOutpostGame(new RuinedOutpostMap(),player,
                List.of(enemy),new Guardian(900,300));
        game.begin();
        assert game.attack(1,0);
        assert enemy.health()==2 : "damage must wait for windup";
        game.togglePause();
        game.update(0.4,1,0);
        assert player.x()==300&&enemy.health()==2;
        assert !game.attack(1,0)&&!game.dash(1,0)&&!game.transform();
        game.togglePause();
        game.update(0.18,0,0);
        assert enemy.health()==1 : "active strike must damage exactly once";
        game.update(0.2,0,0);
        assert enemy.health()==1;
    }
    private static void armorDrainAndRecovery() {
        Player p=new Player(300,300);
        RuinedOutpostMap map=new RuinedOutpostMap();
        p.collectIchor(100);assert p.transform();
        assert p.hurt(2);
        assert p.healthValue()==3.5 : "Blade takes 75 percent damage";
        assert p.ichor()==95 : "Blade hit loses five additional Ichor";
        p.move(0,0,6,map);
        double before=p.bladeSeconds();
        p.collectIchor(10);
        assert Math.abs(p.bladeSeconds()-before-1.2)<0.0001 : "Blade can collect and extend";
        p.move(0,0,12,map);
        assert p.recovering()&&!p.bladeForm();
        assert !p.startAttack()&&!p.dash(1,0);
        double x=p.x();p.move(1,0,1,map);
        assert p.x()==x : "reversion recovery must prevent movement";
        p.move(0,0,0.5,map);
        assert !p.recovering();
    }
    private static void oneDamageEventPerLunge() {
        Player p=new Player(300,300);
        Wisp enemy=new Wisp(340,300,340,340,2);
        RuinedOutpostGame game=new RuinedOutpostGame(new RuinedOutpostMap(),p,
                List.of(enemy),new Guardian(900,300));
        game.begin();
        game.update(0.5,0,0);
        game.update(0.15,0,0);
        assert game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.PLAYER_HIT).count()==1;
        assert p.health()==4;
    }
    private static void walkThroughOpenBreach() {
        RuinedOutpostGame game=new RuinedOutpostGame();game.begin();
        for(int i=0;i<60&&game.map().room()==0;i++)game.update(0.1,1,0);
        assert game.map().room()==1 : "open road must transition without E";
        assert game.player().x()>180&&game.player().x()<320 : "arrival must be safely inside breach";
        var east=game.map().doors().stream().filter(d->d.destination()==2).findFirst().orElseThrow();
        assert !game.map().passageOpen(east);
        game.player().relocate(east.x()-160,east.y());
        game.update(0.5,1,0);
        assert game.map().room()==1 : "combat gate must prevent walking past uncleared encounter";
        assert !game.map().isBlocked(game.player().x(),game.player().y()+24,24);
    }
}
