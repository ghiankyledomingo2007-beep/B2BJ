import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Staged native-scale render evidence, never a GUI or a claim of an unassisted playthrough. */
public final class PreviewMotionPolish {
    private static final Path OUT=Path.of("docs/testing/motion-polish");
    private static final int[][] DIRECTIONS={{0,1},{1,0},{0,-1},{-1,0},{1,-1},{-1,1}};
    private static final String[] NAMES={"south","east","north","west","north-east","south-west"};
    private static int screenshots;
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(true);Files.createDirectories(OUT);
        String part=args.length==0?"all":args[0];
        if(part.equals("all")||part.equals("run"))runScenes();
        if(part.equals("all")||part.equals("engulf"))engulfScenes();
        if(part.equals("all")||part.equals("boss"))bossScenes();
        if(part.equals("all")||part.equals("tide"))tideScenes();
        if(screenshots==0)throw new IllegalArgumentException("Expected all, run, engulf, boss or tide");
        System.out.println("Saved "+screenshots+" staged screenshots/contact sheets to "+OUT);
    }
    private static B2BJ scene(int room,Wisp... enemies)throws Exception {
        var panel=new B2BJ(false);panel.setSize(1280,720);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(room),new Player(960,576),
                List.of(enemies),new Guardian(1110,576));game.begin();
        set(panel,"game",game);return panel;
    }
    private static void runScenes()throws Exception {
        String[] keys={"down","right","up","left"};
        for(int d=0;d<4;d++) {
            var panel=scene(0);panel.game().player().collectIchor(100);
            key(panel,"transform",true);key(panel,"transform",false);advance(panel,.72);
            key(panel,keys[d],true);panel.step(.001);
            var animation=(BladeAnimation)get(panel,"bladeAnimation");
            var crops=new ArrayList<BufferedImage>();var labels=new ArrayList<String>();
            for(int pose=0;pose<8;pose++) {
                for(int tick=0;tick<100&&animation.frame()!=pose;tick++)panel.step(.005);
                if(animation.action()!=BladeAnimation.Action.RUN||animation.frame()!=pose)
                    throw new IllegalStateException("Missing real run pose "+NAMES[d]+" "+pose);
                var image=save(panel,"run-"+NAMES[d]+"-"+pose);
                crops.add(crop(image,screenX(panel, panel.game().player().x()),screenY(panel,panel.game().player().y()),320,240));
                labels.add(NAMES[d]+" run "+pose+" / row "+animation.row());
                if(pose<7)advance(panel,.075);
            }
            key(panel,keys[d],false);contact("run-"+NAMES[d]+"-contact",crops,labels,4);
        }
    }
    private static void engulfScenes()throws Exception {
        for(int d:new int[]{1,3}) {
            int dx=DIRECTIONS[d][0];var enemy=new Wisp(960+70*dx,576,960+70*dx,960+70*dx,1);
            var panel=scene(0,enemy);mouse(panel,MouseEvent.BUTTON1,dx,0);
            for(int tick=0;tick<300&&enemy.alive();tick++)panel.step(.005);
            if(enemy.alive()||panel.game().corpses().isEmpty())throw new IllegalStateException("Real kill failed "+NAMES[d]);
            advance(panel,.55);save(panel,"engulf-"+NAMES[d]+"-before");
            key(panel,"interact",true);key(panel,"interact",false);panel.step(0);
            if(panel.game().absorptionTarget()==null)throw new IllegalStateException("Real E failed "+NAMES[d]);
            double px=panel.game().player().x(),py=panel.game().player().y();
            var crops=new ArrayList<BufferedImage>();var labels=new ArrayList<String>();
            for(int pose=0;pose<16;pose++) {
                double target=(pose+.08)/16;
                for(int tick=0;tick<100&&panel.game().absorptionProgress()<target;tick++)panel.step(.002);
                if(panel.game().absorptionTarget()==null)throw new IllegalStateException("Engulf ended before pose "+pose);
                assert panel.game().player().x()==px&&panel.game().player().y()==py : "engulf collision body moved";
                var image=save(panel,"engulf-"+NAMES[d]+"-"+pose);
                crops.add(crop(image,screenX(panel,px+dx*32),screenY(panel,py),384,256));
                labels.add(NAMES[d]+" engulf "+pose+" / "+String.format(java.util.Locale.ROOT,"%.3f",panel.game().absorptionProgress()));
            }
            advance(panel,.2);assert panel.game().absorptionTarget()==null&&panel.game().corpses().isEmpty();
            save(panel,"engulf-"+NAMES[d]+"-after");contact("engulf-"+NAMES[d]+"-contact",crops,labels,4);
        }
    }
    private static void bossScenes()throws Exception {
        for(var motion:Guardian.Animation.values()) {
            var panel=scene(9);var boss=panel.game().guardian();
            Guardian.State state=switch(motion) {
                case IDLE -> Guardian.State.DORMANT;
                case APPROACH -> Guardian.State.APPROACH;
                case WINDUP -> Guardian.State.TELEGRAPH;
                case STRIKE,CHARGE -> Guardian.State.SLAM;
                case RECOVER -> Guardian.State.RECOVER;
                case DEATH -> Guardian.State.DEAD;
            };
            set(boss,"state",state);set(boss,"attack",motion==Guardian.Animation.CHARGE?Guardian.Attack.CHARGE:Guardian.Attack.TARGET);
            set(boss,"targetX",960.0);set(boss,"targetY",576.0);
            if(motion==Guardian.Animation.DEATH)set(boss,"health",0);
            double duration=switch(motion) {
                case IDLE -> 8.0/6;
                case APPROACH -> 1;
                case WINDUP -> boss.telegraphDuration();
                case STRIKE,CHARGE -> boss.activeDuration();
                case RECOVER -> Guardian.RECOVER_DURATION;
                case DEATH -> Guardian.DEATH_DURATION;
            };
            var crops=new ArrayList<BufferedImage>();var labels=new ArrayList<String>();
            for(int pose=0;pose<8;pose++) {
                double normalized=(pose+.2)/8;
                set(boss,"stateTime",normalized*duration);set(boss,"idleAnimationTime",normalized*duration);
                assert boss.animation()==motion&&boss.animationFrame()==pose;
                var image=save(panel,"boss-"+motion.name().toLowerCase(java.util.Locale.ROOT)+"-"+pose);
                crops.add(crop(image,screenX(panel,boss.x()),screenY(panel,boss.y()),320,256));
                labels.add(motion+" "+pose+" / "+String.format(java.util.Locale.ROOT,"%.3f",normalized));
            }
            contact("boss-"+motion.name().toLowerCase(java.util.Locale.ROOT)+"-contact",crops,labels,4);
        }
    }
    private static void tideScenes()throws Exception {
        for(int d=0;d<DIRECTIONS.length;d++)for(boolean collision:new boolean[]{false,true}) {
            int dx=DIRECTIONS[d][0],dy=DIRECTIONS[d][1];double length=Math.hypot(dx,dy);
            double tx=960+dx/length*260,ty=576+dy/length*260;
            var enemy=new Wisp(tx,ty,tx,tx,20);
            var panel=collision?scene(0,enemy):scene(0);
            mouse(panel,MouseEvent.BUTTON3,dx,dy);
            for(int tick=0;tick<100&&panel.game().projectiles().isEmpty();tick++)panel.step(.005);
            if(panel.game().projectiles().isEmpty()||!panel.game().projectiles().get(0).heavy())
                throw new IllegalStateException("RMB did not release Tide "+NAMES[d]);
            var wave=panel.game().projectiles().get(0);String prefix="tide-"+NAMES[d]+(collision?"-hit":"-miss");
            var crops=new ArrayList<BufferedImage>();var labels=new ArrayList<String>();
            advance(panel,.04);tideSample(panel,prefix,"release",wave,crops,labels);
            advance(panel,.16);tideSample(panel,prefix,"midflight",wave,crops,labels);
            for(int tick=0;tick<400&&!panel.game().projectiles().isEmpty();tick++)panel.step(.005);
            if(!panel.game().projectiles().isEmpty())throw new IllegalStateException("Tide never ended");
            if(collision&&enemy.health()!=18)throw new IllegalStateException("Tide collision fixture missed "+NAMES[d]+": HP="+enemy.health());
            advance(panel,.07);tideSample(panel,prefix,collision?"impact":"expiry",wave,crops,labels);
            advance(panel,.18);tideSample(panel,prefix,"foam-finish",wave,crops,labels);
            contact(prefix+"-contact",crops,labels,2);
        }
    }
    private static void tideSample(B2BJ panel,String prefix,String phase,WaterProjectile wave,
            List<BufferedImage> crops,List<String> labels)throws Exception {
        var image=save(panel,prefix+"-"+phase);
        double x=(panel.game().player().x()+wave.x())/2,y=(panel.game().player().y()+wave.y())/2;
        crops.add(crop(image,screenX(panel,x),screenY(panel,y),512,400));labels.add(prefix+" / "+phase);
    }
    private static void advance(B2BJ panel,double seconds) {
        for(double remaining=seconds;remaining>1e-9;remaining-=.005)panel.step(Math.min(.005,remaining));
    }
    private static void key(B2BJ panel,String name,boolean pressed) {
        var action=panel.getActionMap().get(name+(pressed?"Pressed":"Released"));
        if(action==null)throw new IllegalArgumentException("Missing input "+name);
        action.actionPerformed(new ActionEvent(panel,ActionEvent.ACTION_PERFORMED,name));
    }
    private static void mouse(B2BJ panel,int button,int dx,int dy)throws Exception {
        int x=screenX(panel,panel.game().player().x())+dx*180,y=screenY(panel,panel.game().player().y())+dy*180;
        for(int event:new int[]{MouseEvent.MOUSE_PRESSED,MouseEvent.MOUSE_RELEASED})
            panel.dispatchEvent(new MouseEvent(panel,event,0,0,x,y,1,false,button));
    }
    private static int screenX(B2BJ panel,double x)throws Exception {return (int)Math.round(x)-camera(panel,true);}
    private static int screenY(B2BJ panel,double y)throws Exception {return (int)Math.round(y)-camera(panel,false);}
    private static int camera(B2BJ panel,boolean horizontal)throws Exception {
        var method=B2BJ.class.getDeclaredMethod("cameraPosition",double.class,int.class,int.class);method.setAccessible(true);
        return (int)method.invoke(null,horizontal?panel.game().player().x():panel.game().player().y(),
                horizontal?1280:720,horizontal?panel.game().map().worldWidth():panel.game().map().worldHeight());
    }
    private static Object get(Object object,String name)throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    private static void set(Object object,String name,Object value)throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    private static BufferedImage save(B2BJ panel,String name)throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();write(image,name);return image;
    }
    private static BufferedImage crop(BufferedImage image,int x,int y,int width,int height) {
        var result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=result.createGraphics();
        g.drawImage(image,width/2-x,height/2-y,null);g.dispose();return result;
    }
    private static void contact(String name,List<BufferedImage> images,List<String> labels,int columns)throws Exception {
        int width=images.get(0).getWidth(),height=images.get(0).getHeight()+24;
        var sheet=new BufferedImage(width*columns,height*((images.size()+columns-1)/columns),BufferedImage.TYPE_INT_ARGB);
        var g=sheet.createGraphics();g.setColor(new Color(13,20,29));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        for(int i=0;i<images.size();i++) {
            int x=i%columns*width,y=i/columns*height;g.drawImage(images.get(i),x,y+24,null);
            g.setColor(Color.WHITE);g.drawString(labels.get(i),x+6,y+17);
        }
        g.dispose();write(sheet,name);
    }
    private static void write(BufferedImage image,String name)throws Exception {
        if(!ImageIO.write(image,"png",OUT.resolve(name+".png").toFile()))throw new IllegalStateException("PNG writer unavailable");
        screenshots++;System.out.println("Saved "+name);
    }
}
