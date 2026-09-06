import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Input-driven diagnostics, not difficulty assertions. Only isolated fixtures inject starting state. */
public final class AuditImmersion {
    private static final double DT=.01;
    private static final List<String> results=new ArrayList<>();
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        GameAudio.setMuted(true);
        results.add("scenario\tobservation");
        freshRoute(false);
        freshRoute(true);
        for(String policy:List.of("stationary","strafe","reactive-dash")) isolated(policy);
        cover();
        magnet();
        Path output=Path.of(args.length==0?"docs/testing/immersion-40/scenario-results.tsv":args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output,results);
        System.out.println("Saved "+(results.size()-1)+" observations to "+output);
    }
    private static void result(String name,String format,Object...args) {
        String line=name+"\t"+String.format(format,args);
        results.add(line);System.out.println(line);
    }
    private static final class Events {
        int hits,shots,pickups,dashes;
        void collect(RuinedOutpostGame game) {
            for(var event:game.drainEvents())switch(event.type()) {
                case PLAYER_HIT -> hits++;
                case SPIT_SHOT -> shots++;
                case PICKUP -> pickups++;
                case DASH -> dashes++;
                default -> { }
            }
        }
    }
    private static void freshRoute(boolean adaptive) {
        var game=new RuinedOutpostGame();game.begin();var events=new Events();
        double elapsed=0;int room=0,casts=0,transforms=0;long spitters=0;
        String name="fresh-route-"+(adaptive?"two-skills-reactive-dodge":"basic");
        StringBuilder visits=new StringBuilder("0");
        // Same route/spacing policy as AuditOutpost. No fixture changes, resets or boss-fight claims.
        while(elapsed<300&&game.player().alive()&&game.map().room()!=9) {
            var p=game.player();
            if(game.map().room()>=4&&game.transform())transforms++;
            Wisp enemy=game.scouts().stream().filter(Wisp::alive)
                    .min(Comparator.comparingDouble(e->Math.hypot(e.x()-p.x(),e.y()-p.y()))).orElse(null);
            double tx=p.x(),ty=p.y();
            if(enemy!=null) {
                double dx=enemy.x()-p.x(),dy=enemy.y()-p.y(),distance=Math.max(1,Math.hypot(dx,dy));
                int[] aim=aim(dx,dy);
                if(distance<(p.bladeForm()?145:340)) {
                    if(adaptive&&distance<290&&game.tideWave(aim[0],aim[1]))casts++;
                    if(game.attack(aim[0],aim[1]))casts++;
                }
                if(distance>(p.bladeForm()?115:305)){tx=enemy.x();ty=enemy.y();}
                else if(distance<(p.bladeForm()?65:200)){tx=p.x()-dx;ty=p.y()-dy;}
            } else if(!game.drops().isEmpty()&&p.ichor()<100) {
                var drop=game.drops().stream().min(Comparator.comparingDouble(d->Math.hypot(d.x()-p.x(),d.y()-p.y()))).orElseThrow();
                tx=drop.x();ty=drop.y();
            } else {
                int next=game.map().room()+1;
                var door=game.map().doors().stream().filter(d->d.destination()==next).findFirst().orElseThrow();
                tx=door.x();ty=door.y();
            }
            int[] movement=steer(game,tx,ty);
            if(adaptive) {
                dodgeProjectiles(game);
                if(enemy!=null&&enemy.state()==Wisp.State.LUNGE
                        &&enemy.role()!=Wisp.Role.SPITTER&&Math.hypot(enemy.x()-p.x(),enemy.y()-p.y())<110)
                    game.dash(movement[0],movement[1]);
            }
            game.update(DT,movement[0],movement[1]);elapsed+=DT;events.collect(game);
            if(room!=game.map().room()) {
                room=game.map().room();visits.append(',').append(room);
                spitters+=game.scouts().stream().filter(e->e.role()==Wisp.Role.SPITTER).count();
                result(name+"-room-"+room,"t=%.2f; HP=%.2f; hits=%d; volleys=%d; kills=%d; Ichor=%.2f",
                        elapsed,p.healthValue(),events.hits,events.shots,game.story().defeatedScouts(),p.ichor());
            }
            if(enemy==null&&Math.hypot(tx-p.x(),ty-p.y())<4)game.interact();
        }
        result(name,"time=%.2f; rooms=%s; HP=%.2f; hits=%d; volleys=%d; authored spitters encountered=%d; casts=%d; dashes=%d; transformations=%d; kills=%d; reached boss=%s; phase=%s; position=(%.1f,%.1f); live enemies=%d; normal tutorial grant/camp healing only; no teleports/forced kills/resources",
                elapsed,visits,game.player().healthValue(),events.hits,events.shots,spitters,casts,events.dashes,transforms,
                game.story().defeatedScouts(),room==9,game.story().phase(),game.player().x(),game.player().y(),
                game.scouts().stream().filter(Wisp::alive).count());
    }
    private static RuinedOutpostGame fixture(RuinedOutpostMap map,Player p,List<Wisp> enemies) {
        var game=new RuinedOutpostGame(map,p,enemies,new Guardian(map.guardianX(),map.guardianY()));
        game.begin();return game;
    }
    private static void isolated(String policy) {
        var enemy=new Wisp(800,576,800,800,3,Wisp.Role.SPITTER);
        var game=fixture(new RuinedOutpostMap(),new Player(550,576),List.of(enemy));
        var events=new Events();double elapsed=0,firstHit=-1,minCadence=Double.POSITIVE_INFINITY,lastShot=-1;
        int maxLive=0;
        while(elapsed<25&&game.player().alive()) {
            var p=game.player();int[] movement={0,0};
            if(policy.equals("strafe")) {
                double dx=p.x()-enemy.x(),dy=p.y()-enemy.y(),r=Math.max(1,Math.hypot(dx,dy));
                movement=steer(game,enemy.x()+dx/r*250-dy/r*70,enemy.y()+dy/r*250+dx/r*70);
            } else if(policy.equals("reactive-dash"))dodgeProjectiles(game);
            int shots=events.shots,hits=events.hits;
            game.update(DT,movement[0],movement[1]);elapsed+=DT;events.collect(game);
            if(events.shots>shots){if(lastShot>=0)minCadence=Math.min(minCadence,elapsed-lastShot);lastShot=elapsed;}
            if(events.hits>hits&&firstHit<0)firstHit=elapsed;
            maxLive=Math.max(maxLive,game.hostileProjectiles().size());
        }
        result("isolated-spitter-"+policy,"time=%.2f; HP=%.2f; hits=%d; first hit=%.2f; volleys=%d; minimum volley interval=%.2f; dashes=%d; max live projectiles=%d; enemyHP=%d; no player attacks/healing",
                elapsed,game.player().healthValue(),events.hits,firstHit,events.shots,minCadence,events.dashes,maxLive,enemy.health());
    }
    private static void dodgeProjectiles(RuinedOutpostGame game) {
        var p=game.player();
        for(var shot:game.hostileProjectiles()) {
            double dx=p.x()-shot.x(),dy=p.y()-shot.y();
            if(Math.hypot(dx,dy)<100&&dx*shot.directionX()+dy*shot.directionY()>0) {
                int[] dodge=aim(-shot.directionY(),shot.directionX());
                game.dash(dodge[0],dodge[1]);return;
            }
        }
    }
    private static void cover() {
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double y=wall.centerY()-Wisp.COLLISION_Y_OFFSET;
        var enemy=new Wisp(wall.centerX()+130,y,wall.centerX()+130,wall.centerX()+130,3,Wisp.Role.SPITTER);
        var game=fixture(map,new Player(wall.centerX()-130,y),List.of(enemy));var events=new Events();
        double first=-1;int blockedReleases=0,wideBlockedReleases=0;
        for(int i=0;i<1000&&game.player().alive();i++) {
            int shot=enemy.shotNumber();game.update(DT,0,0);events.collect(game);
            if(enemy.shotNumber()!=shot) {
                if(first<0)first=(i+1)*DT;
                if(!map.clearWaterLine(enemy.x(),enemy.y()+Wisp.COLLISION_Y_OFFSET,
                        game.player().x(),game.player().y()+Wisp.COLLISION_Y_OFFSET,1))blockedReleases++;
                if(!map.clearWaterLine(enemy.x(),enemy.y()+Wisp.COLLISION_Y_OFFSET,
                        game.player().x(),game.player().y()+Wisp.COLLISION_Y_OFFSET,EnemyProjectile.RADIUS))wideBlockedReleases++;
            }
        }
        result("cover-spitter","10 s cap; first release=%.2f; volleys=%d; blocked-LOS releases=%d; projectile-radius-blocked releases=%d; playerHP=%.2f; enemy=(%.15f,%.15f); distance=%.15f; state=%s; final firing lane clear=%s; no player inputs",
                first,events.shots,blockedReleases,wideBlockedReleases,game.player().healthValue(),enemy.x(),enemy.y(),
                Math.hypot(enemy.x()-game.player().x(),enemy.y()-game.player().y()),enemy.state(),
                map.clearWaterLine(enemy.x(),enemy.y()+Wisp.COLLISION_Y_OFFSET,
                        game.player().x(),game.player().y()+Wisp.COLLISION_Y_OFFSET,EnemyProjectile.RADIUS));
    }
    @SuppressWarnings("unchecked")
    private static void addFixtureDrop(RuinedOutpostGame game,double x,double y,double amount) throws Exception {
        var field=RuinedOutpostGame.class.getDeclaredField("drops");field.setAccessible(true);
        ((List<RuinedOutpostGame.IchorDrop>)field.get(game)).add(new RuinedOutpostGame.IchorDrop(x,y,amount));
    }
    private static void magnet() throws Exception {
        var map=new RuinedOutpostMap(2);double y=map.spawnY()-Player.COLLISION_Y_OFFSET;
        var game=fixture(map,new Player(145,y),List.of(new Wisp(1400,800,1400,1400,4)));
        map.setPassagesLocked(true);addFixtureDrop(game,20,y,10);var drop=game.drops().get(0);
        for(int i=0;i<200;i++)game.update(DT,0,0);
        result("magnet-locked-gate","2 s; player-drop initial distance=125; drop displacement=%.2f; Ichor=%.2f; drop amount=%.2f; gate remains locked=%s",
                drop.x()-20,game.player().ichor(),drop.amount(),!map.passageOpen(map.doors().get(0)));
        var partial=fixture(new RuinedOutpostMap(),new Player(500,500),List.of());
        partial.player().collectIchor(95);addFixtureDrop(partial,500,500,10);var events=new Events();
        partial.update(DT,0,0);events.collect(partial);
        result("magnet-capacity-overflow","Ichor=%.2f; remainder=%.2f; pickup events=%d",
                partial.player().ichor(),partial.drops().get(0).amount(),events.pickups);
        partial.transform();for(int i=0;i<120;i++){partial.update(DT,0,0);events.collect(partial);}
        result("magnet-blade-topup","1.2 s after normal transform; Ichor=%.2f; drops=%d; total pickup events=%d; no resource loss from overflow and no per-tick event spam",
                partial.player().ichor(),partial.drops().size(),events.pickups);
    }
    // AuditOutpost's eight-way local input steering; not a replacement for player pathfinding.
    private static int[] steer(RuinedOutpostGame game,double tx,double ty) {
        var p=game.player();if(Math.hypot(tx-p.x(),ty-p.y())<4)return new int[]{0,0};
        double best=Double.POSITIVE_INFINITY;int[] chosen={0,0};
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0) {
            double length=Math.hypot(dx,dy),x=p.x()+dx/length*16,y=p.y()+dy/length*16;
            if(game.map().isBlocked(x,y+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))continue;
            double score=Math.hypot(tx-x,ty-y);
            if(score<best){best=score;chosen=new int[]{dx,dy};}
        }
        return chosen;
    }
    private static int[] aim(double dx,double dy) {
        double length=Math.max(.001,Math.hypot(dx,dy));
        return new int[]{(int)Math.round(dx/length*1000),(int)Math.round(dy/length*1000)};
    }
}
