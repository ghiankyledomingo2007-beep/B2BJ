import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Isolated, read-only animation audit: drives real input/update/render code, never a campaign save. */
public final class AuditPlayerPlayback {
    private static final String[] DIRECTIONS={"down","right","up","left"};
    private static final int[][] AIM={{0,1},{1,0},{0,-1},{-1,0}};
    private static final Field BLADE=field("bladeAnimation"),SLIME=field("slimeAnimation"),MORPH=field("transformationAnimation");
    private static final Method DRAW=method(B2BJ.class,"drawPlayer",Graphics2D.class,int.class,int.class);
    private static final Method EFFECTS=method(B2BJ.class,"drawImpacts",Graphics2D.class,int.class,int.class,Boolean.class);
    private static final Method CORPSES=method(B2BJ.class,"drawCorpses",Graphics2D.class,int.class,int.class);
    private static final Method CRESCENT=method(B2BJ.class,"drawCrescent",Graphics2D.class,int.class,int.class,RuinedOutpostGame.Crescent.class);
    private static final Method WATER=method(B2BJ.class,"drawWater",Graphics2D.class,int.class,int.class,WaterProjectile.class);
    private static final Method HURT=method(RuinedOutpostGame.class,"hurtPlayer",int.class,double.class,double.class);
    private static final int W=180,H=160,COLUMNS=10;
    private static Field field(String name) {try {var f=B2BJ.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new IllegalStateException(e);}}
    private static Method method(Class<?> owner,String name,Class<?>...args) {try {var m=owner.getDeclaredMethod(name,args);m.setAccessible(true);return m;}catch(Exception e){throw new IllegalStateException(e);}}
    private final B2BJ panel;
    private final RuinedOutpostGame game;
    private final BladeAnimation blade;
    private final SlimeAnimation slime;
    private final int direction;
    private final List<BufferedImage> frames=new ArrayList<>();
    private final List<String> trace=new ArrayList<>();
    private int ticks;
    private double time;
    private String phase="ready";

    private AuditPlayerPlayback(int direction,boolean human) throws Exception {
        this.direction=direction;
        var player=new Player(900,550);player.setGodMode(true);player.setUnlimitedBlade(true);
        if(human)player.transform();
        game=new RuinedOutpostGame(new RuinedOutpostMap(5),player,List.of(),new Guardian(1700,1000));
        game.begin();game.drainEvents();
        panel=new B2BJ(false,game);panel.setSize(1280,720);
        blade=(BladeAnimation)BLADE.get(panel);slime=(SlimeAnimation)SLIME.get(panel);
        blade.face(AIM[direction][0],AIM[direction][1]);slime.face(AIM[direction][0],AIM[direction][1]);
    }
    private void key(String name,boolean down) {
        panel.getActionMap().get(name+(down?"Pressed":"Released")).actionPerformed(new ActionEvent(panel,0,name));
    }
    private void tap(String name) {key(name,true);key(name,false);}
    private void mouse(int type,int button) {
        var e=new MouseEvent(panel,type,0,0,640+AIM[direction][0]*220,360+AIM[direction][1]*220,1,false,button);
        for(var listener:panel.getMouseListeners()) {
            if(type==MouseEvent.MOUSE_PRESSED)listener.mousePressed(e);else listener.mouseReleased(e);
        }
    }
    private void hold() {mouse(MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON1);}
    private void release() {mouse(MouseEvent.MOUSE_RELEASED,MouseEvent.BUTTON1);}
    private void advance(String phase,double seconds) throws Exception {
        this.phase=phase;
        for(double left=seconds;left>1e-8;) {
            double dt=Math.min(1.0/60,left);panel.step(dt);time+=dt;left-=dt;
            if(ticks++%2==0)capture();
        }
    }
    private void capture() throws Exception {
        var player=game.player();var current=(BladeAnimation)BLADE.get(panel);var transformation=(TransformationAnimation)MORPH.get(panel);
        int frame=current.action()==BladeAnimation.Action.SLASH?B2BJ.bladeAttackFrame(game.bladeElapsed(),game.bladeMove()):current.frame();
        String action=current.action().name();int row=current.directionRow();
        if(transformation.active()) {action=transformation.reverting()?"TRANSFORM_OUT":"TRANSFORM_IN";frame=transformation.frame();}
        else if(!player.bladeForm()) {
            var s=(SlimeAnimation)SLIME.get(panel);
            row=Math.abs(s.facingHorizontal())>Math.abs(s.facingVertical())?s.facingHorizontal()>0?1:3:s.facingVertical()<0?2:0;
            action=s.attacking()?"SLIME_CAST":player.dashing()?"SLIME_DASH":s.row()>=6?"SLIME_MOVE":"SLIME_IDLE";
            frame=s.attacking()?s.castFrame():s.row()==0?s.idleFrame():s.frame();
            if(game.absorptionTarget()!=null){action="SLIME_ABSORB";frame=Math.min(15,(int)(game.absorptionProgress()*16));}
        }
        double cx=game.crescents().isEmpty()?0:game.crescents().get(0).directionX();
        double cy=game.crescents().isEmpty()?0:game.crescents().get(0).directionY();
        trace.add(String.format(Locale.ROOT,"{\"t\":%.4f,\"phase\":\"%s\",\"action\":\"%s\",\"row\":%d,\"frame\":%d,\"x\":%.3f,\"y\":%.3f,\"dash\":%s,\"human\":%s,\"morph\":%s,\"morphFrame\":%d,\"alive\":%s,\"crescentX\":%.3f,\"crescentY\":%.3f}",
                time,phase,action,row,frame,player.x(),player.y(),player.dashing(),player.bladeForm(),transformation.active(),transformation.frame(),player.alive(),cx,cy));
        var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.scale(.5,.5);
        g.setColor(new Color(33,41,41));g.fillRect(0,0,W*2,H*2);
        int cameraX=(int)Math.round(player.x())-180,cameraY=(int)Math.round(player.y())-160;
        g.setColor(new Color(43,54,54));
        for(int x=-Math.floorMod(cameraX,40);x<W*2;x+=40)g.drawLine(x,0,x,H*2);
        for(int y=-Math.floorMod(cameraY,40);y<H*2;y+=40)g.drawLine(0,y,W*2,y);
        g.setColor(new Color(81,105,99));g.drawLine(170,208,190,208);
        EFFECTS.invoke(panel,g,cameraX,cameraY,Boolean.TRUE);CORPSES.invoke(panel,g,cameraX,cameraY);
        DRAW.invoke(panel,g,cameraX,cameraY);
        for(var shot:game.crescents())CRESCENT.invoke(panel,g,cameraX,cameraY,shot);
        for(var shot:game.projectiles())WATER.invoke(panel,g,cameraX,cameraY,shot);
        EFFECTS.invoke(panel,g,cameraX,cameraY,Boolean.FALSE);g.dispose();frames.add(image);
    }
    private void run(String scenario) throws Exception {
        switch(scenario) {
            case "movement" -> {advance("idle",.4);key(DIRECTIONS[direction],true);advance("run",1.4);key(DIRECTIONS[direction],false);advance("stop_idle",.8);}
            case "turn" -> {key(DIRECTIONS[direction],true);advance("run",.7);key(DIRECTIONS[direction],false);key(DIRECTIONS[(direction+1)%4],true);advance("turn_run",.7);key(DIRECTIONS[(direction+1)%4],false);advance("stop",.4);}
            case "dash" -> {advance("idle",.2);tap("dash");advance("dash_stop_idle",.8);}
            case "combo" -> {for(int move=0;move<4;move++){hold();advance("click",.033333333);release();advance("combo_"+move,RuinedOutpostGame.strikeDuration(move)+.06);}advance("recover",.5);}
            case "charge", "charge_late" -> {if(scenario.equals("charge_late"))advance("wait",.3);hold();advance("charge_hold",1.1);release();advance("release_recover",.9);}
            case "cast" -> {blade.face(-AIM[direction][0],-AIM[direction][1]);mouse(MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON3);advance("cast_opposite_aim",.7);}
            case "guard" -> {tap("riposte");advance("guard_idle",.7);}
            case "dash_slash" -> {advance("idle",.15);tap("dash");advance("dash",Player.BLADE_DASH_DURATION+.04);hold();advance("click",.033333333);release();advance("dash_slash",RuinedOutpostGame.strikeDuration(RuinedOutpostGame.DASH_SLASH)+.25);}
            case "riposte_counter" -> {game.player().setGodMode(false);tap("riposte");advance("guard",.08);HURT.invoke(game,1,game.player().x()+AIM[direction][0]*90,game.player().y()+AIM[direction][1]*90);advance("counter_idle",.7);}
            case "hurt", "slime_hurt" -> {game.player().setGodMode(false);HURT.invoke(game,1,game.player().x()+100,game.player().y());advance("hurt_idle",.9);}
            case "death" -> {game.player().setGodMode(false);HURT.invoke(game,100,game.player().x()+100,game.player().y());advance("dead",1.3);}
            case "transform" -> {tap("transform");advance("transform_in",.95);game.player().setUnlimitedBlade(false);game.player().spendBladeIchor(game.player().ichor()-.2);advance("transform_out_recovery",1.7);}
            case "moving_charge" -> {key(DIRECTIONS[direction],true);advance("run",.3);hold();advance("moving_charge",.9);release();advance("moving_release",.7);key(DIRECTIONS[direction],false);advance("stop",.3);}
            case "moving_cast" -> {key(DIRECTIONS[direction],true);advance("run",.3);mouse(MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON3);advance("moving_cast",.5);key(DIRECTIONS[direction],false);advance("stop",.3);}
            case "dash_buffer" -> {tap("dash");advance("dash",.06);hold();advance("press_during_dash",.13);release();advance("buffer_release",.5);}
            case "slime_movement" -> {advance("idle",.3);key(DIRECTIONS[direction],true);advance("move",.8);tap("dash");advance("dash_move",.35);key(DIRECTIONS[direction],false);advance("stop",.5);}
            case "slime_combo" -> {hold();advance("held_combo",1.65);release();advance("recover",.4);}
            case "slime_tide" -> {mouse(MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON3);advance("tide_recover",.8);}
            case "slime_absorb" -> {
                double x=game.player().x()+AIM[direction][0]*60,y=game.player().y()+AIM[direction][1]*60;
                var enemy=new Wisp(x,y,x,x,1);enemy.hurt(1);
                method(RuinedOutpostGame.class,"afterScoutHit",Wisp.class,boolean.class).invoke(game,enemy,true);
                game.player().setGodMode(false);game.player().hurt(1);game.player().setGodMode(true);
                advance("corpse_ready",.75);tap("interact");
                if(game.absorptionTarget()==null)throw new AssertionError("Absorb fixture did not start");
                advance("absorb_recover",1.2);
            }
            default -> throw new IllegalArgumentException(scenario);
        }
    }
    private void save(Path out,String scenario) throws Exception {
        String id=scenario+"-"+DIRECTIONS[direction];
        var atlas=new BufferedImage(W*COLUMNS,H*((frames.size()+COLUMNS-1)/COLUMNS),BufferedImage.TYPE_INT_RGB);var g=atlas.createGraphics();
        for(int i=0;i<frames.size();i++)g.drawImage(frames.get(i),(i%COLUMNS)*W,(i/COLUMNS)*H,null);
        g.dispose();ImageIO.write(atlas,"png",out.resolve(id+".png").toFile());
        Files.writeString(out.resolve(id+".json"),"{\"id\":\""+id+"\",\"scenario\":\""+scenario+"\",\"direction\":\""+DIRECTIONS[direction]+"\",\"width\":"+W+",\"height\":"+H+",\"columns\":"+COLUMNS+",\"frames\":["+String.join(",",trace)+"]}\n");
    }
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);Path out=Path.of(args[0]);Files.createDirectories(out);int count=0;
        for(String scenario:new String[]{"movement","turn","dash","dash_slash","combo","charge","charge_late","cast","guard","riposte_counter","hurt","death","transform","moving_charge","moving_cast","dash_buffer","slime_movement","slime_combo","slime_tide","slime_hurt","slime_absorb"})
            for(int direction=0;direction<4;direction++) {
                if(args.length>1&&!args[1].equals(scenario))continue;
                var audit=new AuditPlayerPlayback(direction,!scenario.startsWith("slime_")&&!scenario.equals("transform"));
                audit.run(scenario);audit.save(out,scenario);count++;
            }
        System.out.println("Captured "+count+" real-input player playback scenarios at 30fps from 60Hz updates: "+out);
    }
}
