import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Diagnostic scenarios against production logic. Never changes production tuning.
 * Fixture positions/resources are set before a scenario, never during a fight.
 * Findings are observations, not assertions that the current defects are desirable.
 */
public final class AuditOutpost {
    private static final double DT=0.01;
    private static final List<String> results=new ArrayList<>();
    private static Path output;
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        GameAudio.setMuted(true);
        output=Path.of(args.length==0?"docs/testing/outpost-audit-2026-09-06":args[0]);
        Files.createDirectories(output);
        results.add("scenario\tobservation");
        if(args.length>1&&args[1].equals("adaptive")) {
            for(int ichor:new int[]{0,70,100}) {
                var map=new RuinedOutpostMap(9);
                var player=new Player(map.guardianX()-280,map.guardianY());player.collectIchor(ichor);
                runBossFight(fixture(9,player,List.of()),"boss-adaptive-ichor-"+ichor,220,true,true,360,true);
            }
            Files.write(output.resolve("scenario-results.tsv"),results);
            return;
        }
        bossMovementAndPattern();
        reversionDash();
        for(int range:new int[]{120,180,240,280,310,340,380}) {
            stationaryEnemy(range,false);
            stationaryEnemy(range,true);
        }
        blockedEnemy();
        trackingTell();
        stockpiling();
        spawnSafety();
        tutorialRoute(false);
        tutorialRoute(true);
        for(int radius:new int[]{220,280,330}) bossFight(radius,false,true,360);
        bossFight(280,true,true,360);
        bossFight(330,false,false,60);
        renderCost();
        Files.write(output.resolve("scenario-results.tsv"),results);
        System.out.println("Saved "+(results.size()-1)+" observations to "+output);
    }
    private static void result(String name,String format,Object...args) {
        String line=name+"\t"+String.format(format,args);
        results.add(line);System.out.println(line);
    }
    private static RuinedOutpostGame fixture(int room,Player p,List<Wisp> enemies) {
        var map=new RuinedOutpostMap(room);
        var boss=new Guardian(map.guardianX(),map.guardianY());
        var game=new RuinedOutpostGame(map,p,enemies,boss);game.begin();
        if(room==9) {game.story().enterGatehouse();boss.activate(p.x(),p.y());}
        return game;
    }
    private static void bossMovementAndPattern() {
        for(int health:new int[]{240,60}) {
            var boss=new Guardian(1290,528);boss.activate(1100,500);boss.hurt(240-health);
            StringBuilder pattern=new StringBuilder();double travel=0;int count=0;
            for(int i=0;i<6000;i++) {
                double x=boss.x(),y=boss.y();int before=boss.impactNumber();
                boss.update(DT,1100+180*Math.cos(i*DT),500+180*Math.sin(i*DT));
                travel+=Math.hypot(boss.x()-x,boss.y()-y);
                if(before!=boss.impactNumber()) {count++;if(count<=12){if(count>1)pattern.append(',');pattern.append(boss.attack());}}
            }
            result("boss-motion-hp-"+health,"60 s; travel=%.1f px; impacts=%d; first 12=%s",travel,count,pattern);
        }
    }
    private static void reversionDash() {
        for(boolean dash:new boolean[]{false,true}) {
            Player p=new Player(300,260);var map=new RuinedOutpostMap();p.collectIchor(100);p.transform();
            for(int i=0;i<1194;i++)p.move(0,0,DT,map);
            if(dash)assert p.dash(1,0);
            for(int i=0;i<8;i++)p.move(0,0,DT,map);
            boolean vulnerable=p.hurt(1);double x=p.x();
            for(int i=0;i<100;i++)p.move(0,0,DT,map);
            result("reversion-"+(dash?"during-dash":"control"),
                    "recovery=%s; damage accepted=%s; invulnerable after 1 s=%s; dash still active=%s; recovery travel=%.1f",
                    p.recovering(),vulnerable,p.invulnerable(),p.dashing(),p.x()-x);
        }
    }
    private static void stationaryEnemy(int distance,boolean heavy) {
        var enemy=new Wisp(300+distance,260,300+distance,300+distance,2);
        var game=fixture(0,new Player(300,260),List.of(enemy));
        int casts=0,lunges=0,hits=0;double elapsed=0;
        while(elapsed<10&&enemy.alive()&&game.player().alive()) {
            int[] aim=aim(enemy.x()-game.player().x(),enemy.y()-game.player().y());
            if(heavy?game.tideWave(aim[0],aim[1]):game.attack(aim[0],aim[1]))casts++;
            Wisp.State before=enemy.state();game.update(DT,0,0);elapsed+=DT;
            if(before!=Wisp.State.LUNGE&&enemy.state()==Wisp.State.LUNGE)lunges++;
            for(var event:game.drainEvents())if(event.type()==RuinedOutpostGame.EventType.PLAYER_HIT)hits++;
        }
        result((heavy?"tide":"slash")+"-stationary-"+distance,
                "time=%.2f s; enemyHP=%d; playerHP=%.2f; casts=%d; enemy lunges=%d; player hits=%d",
                elapsed,enemy.health(),game.player().healthValue(),casts,lunges,hits);
    }
    private static void blockedEnemy() throws Exception {
        var map=new RuinedOutpostMap(2);var cover=map.barriers().get(0);
        double x=cover.centerX()+100,y=cover.centerY()-22;
        var enemy=new Wisp(x,y,x,x,2);
        var game=fixture(2,new Player(cover.centerX()-100,y),List.of(enemy));
        int lunges=0;double minX=x;
        for(int i=0;i<3000;i++) {
            var before=enemy.state();game.update(DT,0,0);
            if(before!=Wisp.State.LUNGE&&enemy.state()==Wisp.State.LUNGE)lunges++;
            minX=Math.min(minX,enemy.x());game.drainEvents();
        }
        result("enemy-obstacle-routing","30 s; repeated lunges=%d; closest advance=%.1f px; final=(%.1f,%.1f); playerHP=%.1f",
                lunges,x-minX,enemy.x(),enemy.y(),game.player().healthValue());
        snapshot(game,"enemy-stuck-at-cover.png");
    }
    private static void trackingTell() {
        Wisp enemy=new Wisp(700,500,700,700,2);enemy.update(DT,800,500);
        enemy.update(0.39,800,500);enemy.update(0.02,600,500);
        double x=enemy.x();enemy.update(0.05,600,500);
        result("last-moment-lunge-retarget","player crosses behind in last 20 ms; enemy state=%s; lunge displacement=%.1f px (negative=180-degree retarget)",enemy.state(),enemy.x()-x);
    }
    private static void stockpiling() {
        var p=new Player(300,260);p.collectIchor(100);
        var enemy=new Wisp(420,260,420,420,2);var game=fixture(0,p,List.of(enemy));
        game.tideWave(1,0);for(int i=0;i<100;i++)game.update(DT,0,0);
        int before=game.drops().size();
        // Move normally over the drop while full, then leave it for 30 seconds.
        for(int i=0;i<46;i++)game.update(DT,1,0);
        for(int i=0;i<3000;i++)game.update(DT,0,0);
        result("full-meter-drop-bank","drops after kill=%d; after walking over while full + 30 s=%d; ichor=%.1f",before,game.drops().size(),p.ichor());
        game.transform();for(int i=0;i<10;i++)game.update(DT,0,0);
        result("full-meter-bank-consumption","after transform + 0.1 s: drops=%d; ichor=%.3f; blade seconds=%.3f",game.drops().size(),p.ichor(),p.bladeSeconds());
    }
    private static void bossFight(int radius,boolean allowBlade,boolean attack,double seconds) throws Exception {
        var map=new RuinedOutpostMap(9);
        var p=new Player(map.guardianX()+radius,map.guardianY());
        if(allowBlade)p.collectIchor(100);
        var game=fixture(9,p,List.of());
        String name="boss-orbit-"+radius+(allowBlade?"-blade":"-blob")+(attack?"-attack":"-no-attack");
        runBossFight(game,name,radius,allowBlade,attack,seconds);
    }
    private static double runBossFight(RuinedOutpostGame game,String name,int radius,boolean allowBlade,boolean attack,double seconds) throws Exception {
        return runBossFight(game,name,radius,allowBlade,attack,seconds,false);
    }
    private static double runBossFight(RuinedOutpostGame game,String name,int radius,boolean allowBlade,boolean attack,double seconds,boolean adaptive) throws Exception {
        var p=game.player();var boss=game.guardian();
        int hits=0,bossHits=0,casts=0,transforms=0,damage=0,maxAdds=0;double elapsed=0,minSeparation=Double.POSITIVE_INFINITY;
        snapshot(game,name+"-start.png");
        while(elapsed<seconds&&p.alive()&&boss.alive()) {
            if(allowBlade&&game.transform())transforms++;
            double dx=p.x()-boss.x(),dy=p.y()-boss.y(),r=Math.max(1,Math.hypot(dx,dy));
            double wanted=p.bladeForm()?120:radius;
            if(boss.state()==Guardian.State.TELEGRAPH&&boss.slamRadius()>112)wanted=Math.max(wanted,240);
            int[] movement=direction(-dy/r-dx/r*(r-wanted)/60,dx/r-dy/r*(r-wanted)/60);
            if(adaptive)movement=adaptiveMovement(game);
            if(attack) {
                Wisp closest=game.scouts().stream().filter(Wisp::alive)
                        .min(Comparator.comparingDouble(e->Math.hypot(e.x()-p.x(),e.y()-p.y()))).orElse(null);
                boolean targetAdd=allowBlade&&closest!=null&&Math.hypot(closest.x()-p.x(),closest.y()-p.y())<(adaptive?340:220);
                int[] aim=aim((targetAdd?closest.x():boss.x())-p.x(),(targetAdd?closest.y():boss.y())-p.y());
                if(targetAdd&&game.tideWave(aim[0],aim[1]))casts++;
                if(game.attack(aim[0],aim[1]))casts++;
            }
            int health=boss.health();game.update(DT,movement[0],movement[1]);elapsed+=DT;damage+=health-boss.health();
            var adds=game.scouts().stream().filter(Wisp::alive).toList();maxAdds=Math.max(maxAdds,adds.size());
            for(int i=0;i<adds.size();i++)for(int j=i+1;j<adds.size();j++)
                minSeparation=Math.min(minSeparation,Math.hypot(adds.get(i).x()-adds.get(j).x(),adds.get(i).y()-adds.get(j).y()));
            for(var event:game.drainEvents())if(event.type()==RuinedOutpostGame.EventType.PLAYER_HIT) {
                hits++;
                if(event.x()==boss.targetX()&&event.y()==boss.targetY())bossHits++;
                if(adaptive)result(name+"-hit-"+hits,"t=%.2f; source=%s; bossAttack=%s; state=%s; phaseTime=%.2f; player=(%.1f,%.1f); boss=(%.1f,%.1f); target=(%.1f,%.1f); wave=%.1f; dashing=%s; recovering=%s",
                        elapsed,event.x()==boss.targetX()&&event.y()==boss.targetY()?"boss":"add",boss.attack(),boss.state(),boss.stateSeconds(),p.x(),p.y(),boss.x(),boss.y(),boss.targetX(),boss.targetY(),boss.waveRadius(),p.dashing(),p.recovering());
            }
            if(Math.abs(elapsed-30)<DT/2)snapshot(game,name+"-30s.png");
        }
        snapshot(game,name+"-end.png");
        result(name,"time=%.2f s; bossHP=%d; damage=%d; playerHP=%.2f; hits=%d (boss=%d); transformations=%d; casts=%d; max live adds=%d; minimum add spacing=%.2f px; gateOpen=%s",
                elapsed,boss.health(),damage,p.healthValue(),hits,bossHits,transforms,casts,maxAdds,minSeparation,game.map().gateOpen());
        return elapsed;
    }
    /** Diagnostic input policy only: reads visible attack state, never edits actors mid-fight. */
    private static int[] adaptiveMovement(RuinedOutpostGame game) {
        var p=game.player();var b=game.guardian();
        double dx=p.x()-b.x(),dy=p.y()-b.y(),r=Math.max(1,Math.hypot(dx,dy));
        double wanted=p.bladeForm()&&p.bladeSeconds()>2?115:220;
        double tx=b.x()+dx/r*wanted-dy/r*60,ty=b.y()+dy/r*wanted+dx/r*60;
        if(!game.drops().isEmpty()&&p.ichor()<100) {
            var drop=game.drops().stream().min(Comparator.comparingDouble(d->Math.hypot(d.x()-p.x(),d.y()-p.y()))).orElseThrow();
            if(Math.hypot(drop.x()-p.x(),drop.y()-p.y())<350){tx=drop.x();ty=drop.y();}
        }
        boolean warning=b.state()==Guardian.State.TELEGRAPH,active=b.state()==Guardian.State.SLAM,dodge=false;
        if(warning||active) {
            double hx=p.x()-b.targetX(),hy=p.y()-b.targetY(),distance=Math.max(1,Math.hypot(hx,hy));
            switch(b.attack()) {
                case TARGET,SWEEP -> {
                    if(distance<b.slamRadius()+40) {
                        if(distance<2){hx=dx/r;hy=dy/r;distance=1;}
                        tx=p.x()+hx/distance*180;ty=p.y()+hy/distance*180;
                        dodge=active||(warning&&b.stateSeconds()>.78);
                    }
                }
                case CHARGE -> {
                    double side=dx*-b.directionY()+dy*b.directionX();
                    if(Math.abs(side)<130) {
                        double sign=side<0?-1:1;
                        tx=p.x()-b.directionY()*180*sign;ty=p.y()+b.directionX()*180*sign;
                        dodge=active&&r<200;
                    }
                }
                case SHOCKWAVE -> {
                    tx=b.x()+dx/r*110;ty=b.y()+dy/r*110;
                    dodge=active&&Math.abs(distance-b.waveRadius())<90;
                }
            }
        }
        int[] move=steer(game,tx,ty);
        if(dodge) {
            int[] evasion=b.attack()==Guardian.Attack.SHOCKWAVE?direction(-dx,-dy):move;
            game.dash(evasion[0],evasion[1]);
        }
        for(var enemy:game.scouts())if(enemy.alive()&&enemy.state()==Wisp.State.LUNGE&&Math.hypot(enemy.x()-p.x(),enemy.y()-p.y())<110)
            game.dash(move[0],move[1]);
        return move;
    }
    private static void spawnSafety() {
        int[][] positions={{440,384},{840,384},{640,240},{640,544},{960,384}};
        int checked=0,blocked=0;
        for(int room=0;room<12;room++) {
            var map=new RuinedOutpostMap(room);
            for(int i=0;i<map.description().enemies();i++) {
                checked++;
                if(map.isBlocked(map.authored(positions[i][0]),map.authored(positions[i][1])+Wisp.COLLISION_Y_OFFSET,Wisp.COLLISION_RADIUS))blocked++;
            }
        }
        result("authored-enemy-spawn-safety","checked=%d; initially embedded in solid collision=%d",checked,blocked);
    }
    private static void tutorialRoute(boolean useTide) throws Exception {
        var game=new RuinedOutpostGame();game.begin();double elapsed=0,tutorialTime=-1;int hits=0,casts=0,previousRoom=0,transforms=0;
        StringBuilder visits=new StringBuilder("0");
        // Actual fresh run: no relocation, forced kills, healing or starting Ichor.
        while(elapsed<300&&game.player().alive()&&game.map().room()!=9) {
            var p=game.player();
            if(game.map().room()==4&&tutorialTime<0) {
                tutorialTime=elapsed;
                result("fresh-tutorial-route-"+(useTide?"two-skills":"slash-only"),
                        "time=%.2f s; playerHP=%.1f; hits=%d; kills=%d; no teleport/dash/transform",elapsed,p.healthValue(),hits,game.story().defeatedScouts());
            }
            if(game.map().room()>=4&&game.transform())transforms++;
            Wisp enemy=game.scouts().stream().filter(Wisp::alive)
                    .min(Comparator.comparingDouble(e->Math.hypot(e.x()-p.x(),e.y()-p.y()))).orElse(null);
            double tx=p.x(),ty=p.y();boolean combat=false;
            if(enemy!=null) {
                double dx=enemy.x()-p.x(),dy=enemy.y()-p.y(),distance=Math.max(1,Math.hypot(dx,dy));
                int[] a=aim(dx,dy);
                if(distance<(p.bladeForm()?145:340)) {
                    if(useTide&&distance<290&&game.tideWave(a[0],a[1]))casts++;
                    if(game.attack(a[0],a[1]))casts++;
                }
                // Keep ranged spacing; drift backwards if approached. No dash used.
                if(distance>(p.bladeForm()?115:305)) {tx=enemy.x();ty=enemy.y();}
                else if(distance<(p.bladeForm()?65:200)) {tx=p.x()-dx;ty=p.y()-dy;}
                combat=true;
            } else if(!game.drops().isEmpty()&&p.ichor()<100) {
                var drop=game.drops().stream().min(Comparator.comparingDouble(d->Math.hypot(d.x()-p.x(),d.y()-p.y()))).orElseThrow();
                tx=drop.x();ty=drop.y();
            } else {
                int next=game.map().room()+1;
                var door=game.map().doors().stream().filter(d->d.destination()==next).findFirst().orElseThrow();
                tx=door.x();ty=door.y();
            }
            int[] movement=steer(game,tx,ty);
            game.update(DT,movement[0],movement[1]);elapsed+=DT;
            for(var event:game.drainEvents())if(event.type()==RuinedOutpostGame.EventType.PLAYER_HIT)hits++;
            if(previousRoom!=game.map().room()) {previousRoom=game.map().room();visits.append(",").append(previousRoom);}
            if(!combat&&Math.hypot(tx-p.x(),ty-p.y())<4)game.interact();
        }
        result("fresh-outpost-route-"+(useTide?"two-skills":"slash-only"),
                "time=%.2f s; rooms=%s; playerHP=%.1f; hits=%d; casts=%d; ichor=%.1f; kills=%d; reached boss=%s; transformations=%d; no teleport/dash/forced kills",
                elapsed,visits,game.player().healthValue(),hits,casts,game.player().ichor(),game.story().defeatedScouts(),game.map().room()==9,transforms);
        snapshot(game,"fresh-route-"+(useTide?"two-skills":"slash-only")+".png");
        if(!useTide&&game.map().room()==9) {
            elapsed+=runBossFight(game,"fresh-run-boss-continuation",330,false,true,360);
            double exitTime=0;
            while(game.player().alive()&&!game.guardian().alive()&&exitTime<30&&game.story().phase()!=OutpostStory.Phase.COMPLETE) {
                int[] move=steer(game,game.map().gateX(),game.map().gateY());
                game.update(DT,move[0],move[1]);game.interact();exitTime+=DT;
            }
            result("fresh-run-completion","total=%.2f s; phase=%s; playerHP=%.1f; route transformations=%d; boss transformations=0; no teleport/dash/heal/forced kills",
                    elapsed+exitTime,game.story().phase(),game.player().healthValue(),transforms);
            snapshot(game,"fresh-run-completion.png");
        }
    }
    private static int[] steer(RuinedOutpostGame game,double tx,double ty) {
        Player p=game.player();if(Math.hypot(tx-p.x(),ty-p.y())<4)return new int[]{0,0};
        double best=Double.POSITIVE_INFINITY;int[] chosen={0,0};
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0) {
            double length=Math.hypot(dx,dy),x=p.x()+dx/length*16,y=p.y()+dy/length*16;
            if(game.map().isBlocked(x,y+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))continue;
            double score=Math.hypot(tx-x,ty-y);
            if(score<best) {best=score;chosen=new int[]{dx,dy};}
        }
        return chosen;
    }
    private static int[] direction(double dx,double dy) {
        if(dx==0&&dy==0)return new int[]{0,0};
        double angle=Math.round(Math.atan2(dy,dx)/(Math.PI/4))*Math.PI/4;
        return new int[]{(int)Math.round(Math.cos(angle)),(int)Math.round(Math.sin(angle))};
    }
    private static int[] aim(double dx,double dy) {
        double length=Math.max(0.001,Math.hypot(dx,dy));
        return new int[]{(int)Math.round(dx/length*1000),(int)Math.round(dy/length*1000)};
    }
    private static B2BJ panel(RuinedOutpostGame game) throws Exception {
        B2BJ panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());
        var field=B2BJ.class.getDeclaredField("game");field.setAccessible(true);field.set(panel,game);
        return panel;
    }
    private static void snapshot(RuinedOutpostGame game,String name) throws Exception {
        var panel=panel(game);var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();ImageIO.write(image,"png",output.resolve(name).toFile());
    }
    private static void renderCost() throws Exception {
        var game=fixture(9,new Player(1050,528),List.of());var panel=panel(game);
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        long[] times=new long[240];
        for(int i=0;i<300;i++) {
            var g=image.createGraphics();long start=System.nanoTime();panel.paint(g);long elapsed=System.nanoTime()-start;g.dispose();
            if(i>=60)times[i-60]=elapsed;
        }
        java.util.Arrays.sort(times);
        result("headless-render-cost","240 warmed 1280x720 frames; p50=%.2f ms; p95=%.2f ms; p99=%.2f ms (not on-screen frame pacing)",
                times[120]/1e6,times[228]/1e6,times[237]/1e6);
    }
}
