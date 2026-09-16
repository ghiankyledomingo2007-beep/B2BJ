import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Deliberately staged render review; not a live-playthrough claim. Never opens a window. */
public final class PreviewRainoray {
    private static final Path OUT=Path.of(System.getProperty("b2bj.previewDir","docs/testing/rainoray-pass"));
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);GameAudio.setMuted(true);
        if(args.length>0&&args[0].equals("--engulf-only")) { engulfScenes();return; }
        var panel=scene(5,new Player(900,550),List.of());
        var player=panel.game().player();player.collectIchor(100);player.transform();
        var animation=(BladeAnimation)field(panel,"bladeAnimation");
        int[][] directions={{0,1},{1,0},{0,-1},{-1,0}};
        String[] names={"south","east","north","west"};
        for(int i=0;i<directions.length;i++) {
            animation.face(directions[i][0],directions[i][1]);animation.play(BladeAnimation.Action.IDLE,0);
            save(panel,"human-"+names[i]);
        }
        if(args.length>0&&args[0].equals("--idle-only")) {
            save(scene(5,new Player(900,550),List.of()),"blob-size-reference");
            panel=scene(1,new Player(900,550),List.of());player=panel.game().player();
            var fence=panel.game().map().barriers().get(0);
            player.collectIchor(100);player.transform();
            player.relocate(fence.centerX(),fence.centerY()-fence.height()/2-Player.COLLISION_Y_OFFSET-Player.COLLISION_RADIUS);
            save(panel,"human-behind-fence");
            player.relocate(fence.centerX(),fence.centerY()+fence.height()/2);save(panel,"human-before-fence");
            return;
        }
        animation.face(1,0);
        for(var action:BladeAnimation.Action.values()) {
            animation.play(action,.3);animation.update(0,0,false,.12);save(panel,"action-"+action.name().toLowerCase());
        }
        var morph=(TransformationAnimation)field(panel,"transformationAnimation");
        for(boolean reverse:new boolean[]{false,true})for(int phase=0;phase<3;phase++) {
            if(reverse&&player.bladeForm())player.spendBladeIchor(player.ichor());
            morph.start(reverse);morph.update(.08+phase*.24);save(panel,(reverse?"revert-":"transform-")+phase);
        }
        morph.cancel();
        panel=scene(5,new Player(900,550),List.of());player=panel.game().player();player.collectIchor(100);player.transform();
        ((BladeAnimation)field(panel,"bladeAnimation")).face(1,0);
        panel.game().ichorCrescent(1,0);panel.step(0);panel.step(.12);save(panel,"crescent");
        for(int i=0;i<30;i++)panel.step(.01);
        panel.game().riposte();panel.step(0);panel.step(.12);save(panel,"riposte");
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        hurt.invoke(panel.game(),1,player.x()+80,player.y());panel.step(0);panel.step(.12);save(panel,"riposte-counter");

        var remains=new Wisp(970,550,970,970,1);
        panel=scene(5,new Player(900,550),List.of(remains));
        remains.hurt(1);var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class);
        hit.setAccessible(true);hit.invoke(panel.game(),remains);
        panel.game().interact();
        for(int phase=0;phase<4;phase++) {for(int i=0;i<18;i++)panel.step(.01);save(panel,"engulf-"+phase);}
        for(int i=0;i<16;i++)panel.step(.01);save(panel,"engulf-finished");

        var scout=new Wisp(1030,550,1030,1030,4);
        var spitter=new Wisp(950,760,950,950,4,Wisp.Role.SPITTER);
        panel=scene(5,new Player(900,550),List.of(scout,spitter));
        scout.update(.01,900,550);spitter.update(.01,900,550);
        scout.update(.3,900,550);spitter.update(.3,900,550);save(panel,"enemy-warnings");

        for(var attack:Guardian.Attack.values()) {
            panel=scene(9,new Player(900,550),List.of());var boss=panel.game().guardian();
            boss.activate(900,550);boss.hurt(120);
            for(int tick=0;tick<20000;tick++) {
                if(boss.attack()==attack&&boss.state()==Guardian.State.TELEGRAPH)break;
                boss.update(.01,attack==Guardian.Attack.CHARGE?500:900,550);
            }
            if(boss.attack()!=attack||boss.state()!=Guardian.State.TELEGRAPH)throw new IllegalStateException("Missing fixture "+attack);
            boss.update(.2,900,550);save(panel,"boss-"+attack.name().toLowerCase()+"-warning");
            while(boss.state()==Guardian.State.TELEGRAPH)boss.update(.01,900,550);
            boss.update(Math.min(.1,boss.activeDuration()/2),900,550);save(panel,"boss-"+attack.name().toLowerCase()+"-active");
            if(attack==Guardian.Attack.FISSURE) {
                for(int tick=0;tick<20000;tick++) {
                    var lane=boss.fissureBounds();
                    if(boss.state()==Guardian.State.TELEGRAPH&&lane!=null&&lane.height()>lane.width())break;
                    boss.update(.01,900,550);
                }
                boss.update(.2,900,550);save(panel,"boss-fissure-vertical-warning");
                while(boss.state()==Guardian.State.TELEGRAPH)boss.update(.01,900,550);
                boss.update(.1,900,550);save(panel,"boss-fissure-vertical-active");
            }
        }
        panel=scene(9,new Player(900,550),List.of());
        var map=B2BJ.class.getDeclaredField("mapShown");map.setAccessible(true);map.set(panel,true);save(panel,"arena-map");
        var visited=RuinedOutpostGame.class.getDeclaredField("visited");visited.setAccessible(true);
        java.util.Arrays.fill((boolean[])visited.get(panel.game()),true);save(panel,"arena-map-all");
        panel=scene(9,new Player(700,420),List.of());
        var clock=B2BJ.class.getDeclaredField("frameCounter");clock.setAccessible(true);
        for(int frame:new int[]{0,2,5,7}) {clock.set(panel,(long)((frame*60+7)/8));save(panel,"brazier-frame-"+frame);}
        panel=scene(5,new Player(900,550),List.of());
        panel.dispatchEvent(new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_MOVED,0,0,740,360,0,false));
        save(panel,"aim-reticle");
    }
    private static B2BJ scene(int room,Player player,List<Wisp> enemies)throws Exception {
        var panel=new B2BJ(false);panel.setSize(1280,720);
        var world=new RuinedOutpostMap(room);var game=new RuinedOutpostGame(world,player,enemies,new Guardian(1100,550));
        game.begin();var field=B2BJ.class.getDeclaredField("game");field.setAccessible(true);field.set(panel,game);return panel;
    }
    private static void engulfScenes() throws Exception {
        int[][] directions={{0,1},{1,0},{0,-1},{-1,0}};
        String[] names={"south","east","north","west"};
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class);hit.setAccessible(true);
        for(int d=0;d<directions.length;d++) {
            double cx=900+70*directions[d][0],cy=550+70*directions[d][1];
            var remains=new Wisp(cx,cy,cx,cx,1);
            var panel=scene(5,new Player(900,550),List.of(remains));remains.hurt(1);hit.invoke(panel.game(),remains);
            save(panel,"engulf-"+names[d]+"-before");
            if(!panel.game().interact())throw new IllegalStateException("Cannot stage engulf "+names[d]);
            for(int pose:new int[]{3,6,8,11,14,15}) {
                for(int tick=0;tick<240&&panel.game().absorptionTarget()!=null
                        &&panel.game().absorptionProgress()<(pose+.1)/16;tick++)panel.step(.005);
                save(panel,"engulf-"+names[d]+"-"+pose);
            }
            for(int tick=0;tick<20;tick++)panel.step(.005);
            save(panel,"engulf-"+names[d]+"-after");
        }
    }
    private static Object field(Object object,String name)throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    private static void save(B2BJ panel,String name)throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();ImageIO.write(image,"png",OUT.resolve(name+".png").toFile());
        System.out.println("Saved "+name);
    }
}
