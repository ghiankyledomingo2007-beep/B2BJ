import java.util.List;

public final class WaterCombatTest {
    public static void main(String[] args) {
        var enemy=new Wisp(510,300,510,510,4);
        var game=fixture(new Player(300,300),List.of(enemy));
        assert game.attack(1,0);
        game.update(0.12,0,0);
        assert enemy.health()==4 : "water must travel, never deal instant melee damage";
        assert game.projectiles().size()==1;
        double x=game.projectiles().get(0).x();
        game.pause(); game.update(0.3,0,0);
        assert game.projectiles().get(0).x()==x;
        assert !game.tideWave(1,0);
        game.togglePause(); game.update(0.3,0,0);
        assert enemy.health()==3 : "one projectile damages once";
        assert enemy.state()!=Wisp.State.HURT : "basic water must not repeatedly stun-lock enemies";
        assert game.projectiles().isEmpty();

        var heavy=fixture(new Player(300,260),List.of(new Wisp(430,260,430,430,4)));
        assert heavy.tideWave(1,0);
        assert !heavy.attack(1,0) && !heavy.tideWave(1,0) : "skills share casting recovery";
        heavy.update(0.5,0,0);
        assert heavy.scouts().get(0).health()==2;
        assert heavy.scouts().get(0).x()>430 : "heavy water transfers momentum";
        assert !heavy.tideWave(1,0) : "Tide Wave cannot be spammed";
        for(int i=0;i<10;i++)heavy.update(0.5,0,0);
        assert heavy.tideCooldown()==0;

        var wall=fixture(new Player(45,300),List.of());
        assert wall.attack(-1,0); wall.update(0.3,0,0);
        assert wall.projectiles().isEmpty() : "world edge absorbs water";
        assert wall.drainEvents().stream().anyMatch(e->e.type()==RuinedOutpostGame.EventType.WATER_IMPACT);

        var expiry=fixture(new Player(300,300),List.of());
        assert expiry.attack(1,0); expiry.update(0.1,0,0);
        var wave=expiry.projectiles().get(0);
        double speed=wave.speed(); expiry.update(0.1,0,0);
        assert wave.speed()<speed : "water loses momentum with travel";
        expiry.update(0.5,0,0); expiry.update(0.5,0,0);
        assert expiry.projectiles().isEmpty() : "finite range and lifetime";
        assert !expiry.attack(0,0) && !expiry.tideWave(0,0);
        assert expiry.attack(1,0); expiry.player().collectIchor(100);
        assert expiry.transform(); expiry.update(0.2,0,0);
        assert expiry.projectiles().isEmpty() : "transformation cancels pending water cast";
        assert !expiry.tideWave(1,0) : "Blade form keeps its own moveset";
        expiry.restart(); assert expiry.projectiles().isEmpty();
        solidCoverAndKnockback();
        bossArmorAndCastCancellation();
        System.out.println("WaterCombatTest passed");
    }
    private static void solidCoverAndKnockback() {
        var map=new RuinedOutpostMap(2);
        var cover=map.barriers().get(0);
        double y=cover.centerY()-Player.COLLISION_Y_OFFSET;
        var target=new Wisp(cover.centerX()+100,y,cover.centerX()+100,cover.centerX()+100,4);
        var game=new RuinedOutpostGame(map,new Player(cover.centerX()-100,y),List.of(target),new Guardian(900,300));
        game.begin(); assert game.attack(1,0); game.update(0.3,0,0);
        assert target.health()==4 && game.projectiles().isEmpty() : "cover blocks water damage";
        target.push(-2000,0); target.update(0.1,0,0,map);
        assert !map.isBlocked(target.x(),target.y()+Wisp.COLLISION_Y_OFFSET,Wisp.COLLISION_RADIUS)
                : "knockback cannot tunnel into cover";
        var gateMap=new RuinedOutpostMap(1); gateMap.setPassagesLocked(true);
        var gate=gateMap.doors().get(0);
        double sign=gate.x()<gateMap.spawnX()?-1:1;
        var gates=new RuinedOutpostGame(gateMap,new Player(gate.x()-sign*140,gate.y()),
                List.of(new Wisp(960,300,960,960,4)),new Guardian(900,300));
        gates.begin(); assert gates.attack((int)sign,0); gates.update(0.5,0,0);
        assert gates.map().room()==1 && gates.projectiles().isEmpty() : "closed breach stops water";
    }
    private static void bossArmorAndCastCancellation() {
        var map=new RuinedOutpostMap(9);
        var guardian=new Guardian(map.guardianX(),map.guardianY());
        var game=new RuinedOutpostGame(map,new Player(guardian.x()-140,guardian.y()),List.of(),guardian);
        game.begin(); guardian.activate(game.player().x(),game.player().y());
        assert game.attack(1,0); game.update(0.3,0,0);
        assert guardian.health()==Guardian.MAX_HEALTH : "water cannot bypass guarded boss armor";
        var cancel=fixture(new Player(300,300),List.of());
        assert cancel.tideWave(1,0); assert cancel.dash(1,0);
        cancel.update(0.25,0,0);
        assert cancel.projectiles().isEmpty() && cancel.waterCharge()<0 : "dash cancels pending cast";
        assert !cancel.tideWave(1,0) : "cancelled heavy still spends cooldown";
        var death=fixture(new Player(300,300),List.of());
        death.attack(1,0); death.update(0.12,0,0); death.player().hurt(100); death.update(0.01,0,0);
        assert death.projectiles().isEmpty();
    }
    private static RuinedOutpostGame fixture(Player player,List<Wisp> enemies) {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),player,enemies,new Guardian(900,300));
        game.begin(); return game;
    }
}
