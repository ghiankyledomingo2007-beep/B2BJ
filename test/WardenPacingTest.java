import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class WardenPacingTest {
    public static void main(String[] args) throws Exception {
        thresholdPreservesAttacks(); phaseBeat(); recoveryWindows(); gameIntegration(); otherBosses();
        SwingUtilities.invokeAndWait(()->{try{impactFeedback();}catch(Exception e){throw new AssertionError(e);}});
        if(args.length>0) {
            var out=Path.of(args[0]);Files.createDirectories(out);
            SwingUtilities.invokeAndWait(()->{try{capture(out);}catch(Exception e){throw new AssertionError(e);}});
        }
        System.out.println("WardenPacingTest passed: committed attacks, phase beat, openings, pause and summon grace");
    }

    private static Guardian boss(Guardian.Attack attack) throws Exception {
        var boss=new Guardian(900,500);boss.activate(700,500);
        set(boss,"attack",attack);set(boss,"chargeX",-1.0);
        return boss;
    }

    private static void thresholdPreservesAttacks() throws Exception {
        for(var attack:Guardian.Attack.values())for(var state:new Guardian.State[]{Guardian.State.TELEGRAPH,Guardian.State.SLAM,Guardian.State.RECOVER}) {
            var boss=boss(attack);set(boss,"state",state);set(boss,"stateTime",.1);
            double radius=boss.slamRadius(),recover=boss.recoverDuration(),duration=boss.telegraphDuration();
            double x=boss.targetX(),y=boss.targetY();
            boss.hurt(600);
            assert !boss.enraged()&&boss.state()==state : "health crossing must not interrupt "+attack+" / "+state;
            assert boss.slamRadius()==radius&&boss.recoverDuration()==recover&&boss.telegraphDuration()==duration
                    : "health crossing must not resize damage or shorten the current opening";
            assert boss.targetX()==x&&boss.targetY()==y;
        }
    }

    private static void phaseBeat() throws Exception {
        var boss=boss(Guardian.Attack.CHARGE);set(boss,"state",Guardian.State.RECOVER);
        boss.hurt(600);boss.update(1.9,700,500);
        assert boss.state()==Guardian.State.RECOVER;
        boss.update(.1,700,500);
        assert boss.state()==Guardian.State.ENRAGE&&!boss.enraged();
        double x=boss.x(),y=boss.y();int impacts=boss.impactNumber();
        for(int i=0;i<179;i++) {
            assert !boss.hits(x,y)&&!boss.hits(700,500)&&boss.fissureBounds()==null;
            assert boss.animationFrame()>=0&&boss.animationFrame()<8&&boss.actionFrame()==-1;
            boss.update(.01,400+i,650);
            assert boss.state()==Guardian.State.ENRAGE&&boss.x()==x&&boss.y()==y;
        }
        boss.update(.01,400,650);
        assert boss.enraged()&&boss.state()==Guardian.State.TELEGRAPH&&boss.attack()==Guardian.Attack.FISSURE;
        assert boss.impactNumber()==impacts&&!boss.hits(400,650) : "phase change cannot count as an attack";
        assert boss.targetX()==400&&boss.targetY()==650&&boss.stateSeconds()<.001;
        boss.update(1.3,700,300);
        assert boss.state()==Guardian.State.TELEGRAPH&&boss.targetX()==400&&boss.targetY()==650
                : "first phase-two fissure gets its full, committed warning";
        for(int i=0;i<1500;i++) {
            boss.update(.01,700,300);
            assert boss.state()!=Guardian.State.ENRAGE : "phase transition runs once";
        }
        var dying=boss(Guardian.Attack.TARGET);set(dying,"state",Guardian.State.ENRAGE);
        dying.hurt(dying.health());dying.update(3,700,500);
        assert dying.state()==Guardian.State.DEAD&&!dying.visible() : "death cancels the transition";
    }

    private static void recoveryWindows() throws Exception {
        var target=boss(Guardian.Attack.TARGET);
        for(var attack:new Guardian.Attack[]{Guardian.Attack.CHARGE,Guardian.Attack.SHOCKWAVE,Guardian.Attack.FISSURE}) {
            var boss=boss(attack);set(boss,"state",Guardian.State.RECOVER);
            double opening=boss.recoverDuration();
            assert opening>target.recoverDuration() : "heavy attacks need a larger punish window";
            boss.update(opening-.01,700,500);
            assert boss.state()==Guardian.State.RECOVER&&!boss.hits(boss.x(),boss.y());
            boss.update(.01,700,500);assert boss.state()==Guardian.State.APPROACH;
        }
        target.update(target.telegraphDuration()*.8,100,100);int frame=target.animationFrame();
        target.update(target.telegraphDuration()*.15,100,100);
        assert frame==7&&target.animationFrame()==7&&target.state()==Guardian.State.TELEGRAPH
                : "target slam holds its readable release pose before impact";
    }

    private static void gameIntegration() throws Exception {
        var map=new RuinedOutpostMap(9);var boss=new Guardian(map.guardianX(),map.guardianY());
        var player=new Player(map.guardianX()-300,map.guardianY()+150);
        var game=new RuinedOutpostGame(map,player,List.of(),boss);game.begin();boss.activate(player.x(),player.y());
        set(boss,"state",Guardian.State.RECOVER);set(boss,"stateTime",boss.recoverDuration()-.01);
        boss.hurt(600);set(game,"addTimer",5.99);game.drainEvents();game.update(.01,0,0);
        assert boss.state()==Guardian.State.ENRAGE&&game.scouts().isEmpty();
        long cues=game.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.GUARDIAN_ENRAGE).count();
        assert cues==1 : "transition gets one audio cue";
        game.pause();game.update(.5,0,0);assert boss.stateSeconds()==0;
        game.togglePause();
        long releases=0;
        for(int i=0;i<40;i++) {
            game.update(.05,0,0);
            assert game.scouts().isEmpty() : "no fresh summons over the phase-change beat";
            var events=game.drainEvents();
            assert events.stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.GUARDIAN_ENRAGE);
            releases+=events.stream().filter(e->e.type()==RuinedOutpostGame.EventType.GUARDIAN_PHASE_TWO).count();
        }
        assert boss.enraged()&&releases==1 : "phase release gets exactly one impact cue";
    }

    private static void impactFeedback() throws Exception {
        GameAudio.setMuted(true);
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var panel=new B2BJ(false,game);
        for(int i=0;i<240;i++)panel.step(.02);
        var boss=game.guardian();game.player().relocate(boss.x()-270,boss.y()+100);
        game.update(.01,0,0);game.skipDialogue();game.update(.01,0,0);game.drainEvents();
        var member=B2BJ.class.getDeclaredField("feedback");member.setAccessible(true);
        var feedback=(CombatFeedback)member.get(panel);
        boss.hurt(600);set(boss,"state",Guardian.State.ENRAGE);set(boss,"stateTime",Guardian.ENRAGE_DURATION-.01);
        panel.step(.01);
        assert feedback.shakeStrength()==12&&feedback.ichorAlpha()>0 : "phase release must kick the camera and pulse cyan";
        member=B2BJ.class.getDeclaredField("wardenFx");member.setAccessible(true);
        assert ((WardenFx)member.get(panel)).particles()>=50 : "phase release restores the full mote/debris burst";
        feedback.update(2);
        assert feedback.shakeStrength()==0&&feedback.ichorAlpha()==0 : "release feedback expires";
        set(boss,"state",Guardian.State.TELEGRAPH);set(boss,"stateTime",boss.telegraphDuration()-.01);
        panel.step(.01);
        assert feedback.shakeStrength()==12 : "ordinary boss impacts keep their camera shake";
    }

    private static void otherBosses() {
        for(var profile:Guardian.Profile.values())if(profile!=Guardian.Profile.OUTPOST_WARDEN) {
            var boss=new Guardian(900,500,profile);boss.activate(700,500);boss.hurt(boss.maxHealth()/2);
            if(profile==Guardian.Profile.BRIARHEART)
                assert !boss.enraged()&&boss.state()==Guardian.State.TELEGRAPH : "forest phase waits for its committed pattern";
            else assert boss.enraged()&&boss.state()==Guardian.State.TELEGRAPH : "other profiles retain their current phase rules";
        }
    }

    private static void capture(Path out) throws Exception {
        GameAudio.setMuted(true);
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        for(int i=0;i<240;i++)panel.step(.02);
        var boss=game.guardian();game.player().relocate(boss.x()-270,boss.y()+100);
        game.update(.01,0,0);game.skipDialogue();game.update(.01,0,0);
        // Let the encounter title clear before inspecting the mid-fight transition.
        set(boss,"state",Guardian.State.RECOVER);
        for(int i=0;i<240;i++){set(boss,"stateTime",.2);set(game,"addTimer",0.0);panel.step(.02);}
        set(boss,"state",Guardian.State.RECOVER);set(boss,"stateTime",.2);boss.hurt(610);
        for(int i=0;i<30;i++)panel.step(.01);
        paint(panel,out.resolve("recovery.png"));
        while(boss.state()!=Guardian.State.ENRAGE)panel.step(.01);
        for(int i=0;i<115;i++)panel.step(.01);
        paint(panel,out.resolve("phase-transition.png"));
        while(boss.state()==Guardian.State.ENRAGE)panel.step(.01);
        for(int i=0;i<70;i++)panel.step(.01);
        paint(panel,out.resolve("phase-two-fissure.png"));
    }

    private static void paint(B2BJ panel,Path path) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        panel.paint(g);g.dispose();ImageIO.write(image,"png",path.toFile());
    }
    private static void set(Object object,String field,Object value) throws Exception {
        var member=object.getClass().getDeclaredField(field);member.setAccessible(true);member.set(object,value);
    }
}
