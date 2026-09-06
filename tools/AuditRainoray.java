import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Deterministic diagnostic input policies, not a claim of human playtest coverage. */
public final class AuditRainoray {
    private static final double DT=.01, LIMIT=360;
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        var output=Path.of(args.length==0?"docs/testing/rainoray-rework/gameplay.tsv":args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        var rows=new ArrayList<String>();
        rows.add("policy\tinitial_ichor\toutcome\tseconds\tplayer_hp\tboss_hp\tphase2_at\thits\treversion_hits\ttransforms\tprimary_casts\ttides\tcrescents\tguards\tparries\tfissure_impacts\tlongest_damage_gap\tgate_open");
        for(boolean skills:new boolean[]{false,true})for(int ichor:new int[]{0,70,100}) {
            String row=fight(skills,ichor);rows.add(row);System.out.println(row);
        }
        Files.write(output,rows);
    }
    private static String fight(boolean skills,int ichor) {
        var map=new RuinedOutpostMap(9);
        var player=new Player(map.guardianX()-280,map.guardianY());player.collectIchor(ichor);
        var boss=new Guardian(map.guardianX(),map.guardianY());
        var game=new RuinedOutpostGame(map,player,List.of(),boss);
        game.begin();game.story().enterGatehouse();boss.activate(player.x(),player.y());
        int hits=0,reversionHits=0,transforms=0,casts=0,tides=0,crescents=0,guards=0,parries=0,fissures=0;
        double elapsed=0,phase2=-1,lastDamage=0,longestGap=0;
        while(elapsed<LIMIT&&player.alive()&&boss.alive()) {
            if(game.transform())transforms++;
            boolean prepareGuard=skills&&player.bladeForm()&&game.riposteCooldown()==0
                    &&player.ichor()>RuinedOutpostGame.BLADE_SKILL_COST&&guardThreat(game,.55);
            if(prepareGuard&&guardThreat(game,.16)&&game.riposte())guards++;
            int[] movement=movement(game);
            Wisp closest=game.scouts().stream().filter(Wisp::alive)
                    .min(Comparator.comparingDouble(e->Math.hypot(e.x()-player.x(),e.y()-player.y()))).orElse(null);
            boolean targetAdd=closest!=null&&Math.hypot(closest.x()-player.x(),closest.y()-player.y())<340;
            double dx=(targetAdd?closest.x():boss.x())-player.x(),dy=(targetAdd?closest.y():boss.y())-player.y();
            int[] aim=aim(dx,dy);
            if(!prepareGuard&&!game.guarding()) {
                double distance=Math.hypot(dx,dy);
                if(skills&&player.bladeForm()&&player.ichor()>24&&distance>145&&distance<330
                        &&(targetAdd||boss.state()==Guardian.State.RECOVER)&&game.ichorCrescent(aim[0],aim[1]))crescents++;
                if(targetAdd&&game.tideWave(aim[0],aim[1]))tides++;
                if(game.attack(aim[0],aim[1]))casts++;
            }
            int health=boss.health(),impact=boss.impactNumber();
            game.update(DT,movement[0],movement[1]);elapsed+=DT;
            assert game.map().room()==9 : "boss fixture unexpectedly left its arena";
            assert Double.isFinite(player.ichor())&&player.ichor()>=0&&player.ichor()<=100;
            assert player.healthValue()>=0&&player.healthValue()<=Player.MAX_HEALTH;
            if(boss.health()<health) {longestGap=Math.max(longestGap,elapsed-lastDamage);lastDamage=elapsed;}
            if(phase2<0&&boss.enraged())phase2=elapsed;
            if(impact!=boss.impactNumber()&&boss.attack()==Guardian.Attack.FISSURE)fissures++;
            for(var event:game.drainEvents()) {
                if(event.type()==RuinedOutpostGame.EventType.PLAYER_HIT) {
                    hits++;if(player.recovering())reversionHits++;
                }
                if(event.type()==RuinedOutpostGame.EventType.RIPOSTE_COUNTER)parries++;
            }
        }
        longestGap=Math.max(longestGap,elapsed-lastDamage);
        return String.format("%s\t%d\t%s\t%.2f\t%.2f\t%d\t%.2f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.2f\t%s",
                skills?"skills":"baseline",ichor,!player.alive()?"death":!boss.alive()?"win":"timeout",elapsed,
                player.healthValue(),boss.health(),phase2,hits,reversionHits,transforms,casts,tides,crescents,guards,parries,
                fissures,longestGap,map.gateOpen());
    }
    /** Reacts to visible committed attack tells; no knowledge of future random inputs or actor edits. */
    private static boolean guardThreat(RuinedOutpostGame game,double within) {
        var p=game.player();var b=game.guardian();
        if(b.state()==Guardian.State.TELEGRAPH&&b.telegraphDuration()-b.stateSeconds()<within) {
            double distance=Math.hypot(p.x()-b.targetX(),p.y()-b.targetY());
            if(b.attack()==Guardian.Attack.FISSURE) {
                var lane=b.fissureBounds();
                if(Math.abs(p.x()-lane.centerX())<lane.width()/2+20&&Math.abs(p.y()-lane.centerY())<lane.height()/2+20)return true;
            } else if((b.attack()==Guardian.Attack.TARGET||b.attack()==Guardian.Attack.SWEEP)&&distance<b.slamRadius()+25)return true;
        }
        if(b.state()==Guardian.State.SLAM&&b.hits(p.x(),p.y()))return true;
        for(var enemy:game.scouts())if(enemy.alive()&&Math.hypot(enemy.x()-p.x(),enemy.y()-p.y())<100
                &&((enemy.state()==Wisp.State.TELEGRAPH&&enemy.telegraphDuration()-enemy.stateSeconds()<within)
                ||enemy.state()==Wisp.State.LUNGE))return true;
        return false;
    }
    /** Same orbit/drop/telegraph policy as AuditOutpost; add the authored fissure escape. */
    private static int[] movement(RuinedOutpostGame game) {
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
                        dodge=active||(warning&&b.stateSeconds()>b.telegraphDuration()-.12);
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
                case FISSURE -> {
                    var lane=b.fissureBounds();boolean horizontal=lane.width()>lane.height();
                    double delta=horizontal?hy:hx;
                    if(Math.abs(delta)<85) {
                        double sign=delta<0?-1:1;
                        tx=p.x()+(horizontal?0:sign*180);ty=p.y()+(horizontal?sign*180:0);
                        dodge=active||(warning&&b.stateSeconds()>b.telegraphDuration()-.12);
                    }
                }
            }
        }
        int[] move=steer(game,tx,ty);
        if(dodge&&!game.guarding()) {
            int[] evasion=b.attack()==Guardian.Attack.SHOCKWAVE?direction(-dx,-dy):move;
            game.dash(evasion[0],evasion[1]);
        }
        for(var enemy:game.scouts())if(!game.guarding()&&enemy.alive()&&enemy.state()==Wisp.State.LUNGE
                &&Math.hypot(enemy.x()-p.x(),enemy.y()-p.y())<110)game.dash(move[0],move[1]);
        return move;
    }
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
    private static int[] direction(double dx,double dy) {
        double angle=Math.round(Math.atan2(dy,dx)/(Math.PI/4))*Math.PI/4;
        return new int[]{(int)Math.round(Math.cos(angle)),(int)Math.round(Math.sin(angle))};
    }
    private static int[] aim(double dx,double dy) {
        double length=Math.max(.001,Math.hypot(dx,dy));
        return new int[]{(int)Math.round(dx/length*1000),(int)Math.round(dy/length*1000)};
    }
}
