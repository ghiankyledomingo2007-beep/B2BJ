import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reproducible recovery diagnostics. Actor/resource fixtures are labelled; no mid-fight edits. */
public final class AuditRecovery {
    private static final double DT=.01;
    private static final List<String> results=new ArrayList<>();
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        results.add("scenario\tobservation");
        passive();
        pressure(false,false);
        pressure(true,false);
        pressure(true,true);
        bossAdd();
        interrupts();
        lifetimeAndTransition();
        Path output=Path.of(args.length==0?"docs/testing/hud-40/recovery-scenarios.tsv":args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());Files.write(output,results);
        System.out.println("Saved "+(results.size()-1)+" observations to "+output);
    }
    private static void result(String name,String format,Object...args) {
        String line=name+"\t"+String.format(format,args);results.add(line);System.out.println(line);
    }
    private static RuinedOutpostGame fixture(List<Wisp> enemies,int damage) {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(550,576),enemies,new Guardian(1400,700));
        if(damage>0)game.player().hurt(damage);game.begin();return game;
    }
    private static void passive() {
        var game=fixture(List.of(),0);double elapsed=0;
        while(game.player().ichor()<100&&elapsed<180){game.update(DT,0,0);elapsed+=DT;}
        result("passive-empty-to-full","time=%.2f s; Ichor=%.3f; HP=%.2f; no attacks, pickups or other input",elapsed,game.player().ichor(),game.player().healthValue());
        game.transform();advance(game,12.2);double ichor=game.player().ichor();boolean recovery=game.player().recovering();
        advance(game,.5);result("passive-reversion","recovering at start=%s; Ichor before=%.3f; after 0.5 s=%.3f",recovery,ichor,game.player().ichor());
    }
    private static final class Counts {
        int hits,starts,absorbed,kills,shots;
        void collect(RuinedOutpostGame game) {
            for(var event:game.drainEvents())switch(event.type()) {
                case PLAYER_HIT -> hits++;
                case ABSORB_START -> starts++;
                case ABSORBED -> absorbed++;
                case ENEMY_DEFEATED -> kills++;
                case SPIT_SHOT -> shots++;
                default -> { }
            }
        }
    }
    private static Wisp meal() { return new Wisp(610,576,610,610,1); }
    private static void pressure(boolean eat,boolean late) {
        var meal=meal();var spitter=new Wisp(800,576,800,800,3,Wisp.Role.SPITTER);
        var game=fixture(List.of(meal,spitter),2);var events=new Counts();
        double elapsed=0,firstEat=-1,firstHit=-1,firstCancel=-1;
        while(game.player().alive()&&elapsed<20) {
            if(meal.alive())game.attack(1,0);
            boolean imminent=game.hostileProjectiles().stream().anyMatch(s->Math.hypot(s.x()-game.player().x(),s.y()-game.player().y())<125);
            if(eat&&!meal.alive()&&(!late||imminent||events.starts>0))game.interact();
            boolean channel=game.absorptionTarget()!=null;int before=events.hits,absorbed=events.absorbed;
            game.update(DT,0,0);elapsed+=DT;events.collect(game);
            if(events.hits>before&&firstHit<0)firstHit=elapsed;
            if(events.absorbed>absorbed&&firstEat<0)firstEat=elapsed;
            if(channel&&game.absorptionTarget()==null&&events.absorbed==absorbed&&firstCancel<0)firstCancel=elapsed;
        }
        result("pressure-"+(!eat?"no-consume":late?"late-consume":"early-consume"),
                "survived=%.2f s; HP=%.2f; hits=%d; normal kills=%d; absorption starts=%d; completions=%d; first completion=%.2f; first hit=%.2f; first interrupted channel=%.2f; volleys=%d; starting 3 HP/0 Ichor fixture; actual water kill and E input; no movement/dodge/extra attacks after meal dies",
                elapsed,game.player().healthValue(),events.hits,events.kills,events.starts,events.absorbed,firstEat,firstHit,firstCancel,events.shots);
    }
    @SuppressWarnings("unchecked")
    private static void bossAdd() throws Exception {
        var meal=meal();var game=fixture(List.of(meal),2);
        var field=RuinedOutpostGame.class.getDeclaredField("bossAdds");field.setAccessible(true);
        ((Set<Wisp>)field.get(game)).add(meal);
        game.attack(1,0);advance(game,.6);double health=game.player().healthValue(),ichor=game.player().ichor();
        boolean healing=game.corpses().get(0).healing();game.interact();advance(game,.81);
        result("boss-add-no-healing","health before=%.2f; after=%.2f; corpse healing=%s; Ichor before=%.3f; after=%.3f; remaining corpses=%d; boss-add membership is starting fixture, kill/absorption use gameplay input",
                health,game.player().healthValue(),healing,ichor,game.player().ichor(),game.corpses().size());
    }
    private static RuinedOutpostGame killedMeal() {
        var enemy=meal();var game=fixture(List.of(enemy),2);game.attack(1,0);advance(game,.6);return game;
    }
    private static void interrupts() {
        for(String action:List.of("move","dash","attack","tide","transform")) {
            var game=killedMeal();if(action.equals("transform"))game.player().collectIchor(100);
            boolean started=game.interact();advance(game,.3);double before=game.absorptionProgress();
            switch(action) {
                case "move" -> game.update(DT,1,0);
                case "dash" -> game.dash(1,0);
                case "attack" -> game.attack(1,0);
                case "tide" -> game.tideWave(1,0);
                case "transform" -> game.transform();
            }
            advance(game,.6);
            result("interrupt-"+action,"started=%s; progress before=%.3f; target after=%s; HP=%.2f; corpses=%d",started,before,game.absorptionTarget()!=null,game.player().healthValue(),game.corpses().size());
        }
        var game=killedMeal();game.interact();advance(game,.3);
        double before=game.absorptionProgress(),age=game.corpses().get(0).age(),ichor=game.player().ichor();
        game.pause();advance(game,5);
        result("pause-channel","5 s pause; progress before=%.3f; after=%.3f; corpse age delta=%.3f; Ichor delta=%.3f",before,game.absorptionProgress(),game.corpses().get(0).age()-age,game.player().ichor()-ichor);
        game.togglePause();advance(game,.51);result("resume-channel","remaining corpses=%d; HP=%.2f",game.corpses().size(),game.player().healthValue());
    }
    private static void lifetimeAndTransition() throws Exception {
        var game=killedMeal();double start=game.corpses().get(0).age();advance(game,20);
        result("corpse-expiry","initial age=%.2f; 20 s later count=%d",start,game.corpses().size());
        game=killedMeal();var corpse=game.corpses().get(0);var door=game.map().doors().get(0);double elapsed=0;
        while(game.map().room()==0&&elapsed<20) {
            int[] movement=steer(game,door.x(),door.y());game.update(DT,movement[0],movement[1]);elapsed+=DT;
        }
        result("room-transition-cleanup","walked %.2f s; room=%d; old corpse age=%.2f; remaining corpses=%d; target active=%s; normal movement, no relocation",elapsed,game.map().room(),corpse.age(),game.corpses().size(),game.absorptionTarget()!=null);
        var cap=fixture(List.of(),0);
        var afterHit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);afterHit.setAccessible(true);
        for(int i=0;i<25;i++){var enemy=meal();enemy.hurt(1);afterHit.invoke(cap,enemy,true);}
        result("corpse-storage-cap","25 completed-kill callbacks fixture; retained corpses=%d",cap.corpses().size());
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double remaining=seconds;remaining>1e-8;remaining-=DT)game.update(Math.min(DT,remaining),0,0);
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
}
