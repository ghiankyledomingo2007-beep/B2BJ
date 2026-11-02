import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;

/** Animation draft only: current game art/audio, staged actors and proposed effects; never installs gameplay. */
public final class PreviewRainorayKit {
    private static final int FPS=60,W=900,H=500;
    private static final double X=1790,Y=2944;
    private static final Color VOID=new Color(12,8,28),VIOLET=new Color(113,65,189),LIGHT=new Color(196,155,250),WHITE=new Color(232,255,248);
    private static Object get(Object o,String name)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void set(Object o,String name,Object value)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
    private static double smooth(double v){v=clamp(v);return v*v*(3-2*v);}
    private static int px(double world,int camera){return (int)Math.round((world-camera)/2);}

    @SuppressWarnings("unchecked")
    private static void render(Path out,String mode,boolean reduced)throws Exception {
        Files.createDirectories(out);Path save=out.resolve("preview-only-save.properties");Files.deleteIfExists(save);
        var game=RuinedOutpostGame.campaign(save);game.begin();game.skipDialogue();game.setCriticalRollForTesting(()->1);
        game.player().relocate(X,Y);game.player().collectIchor(100);game.player().transform();game.player().setUnlimitedBlade(true);
        game.player().setGodMode(!mode.equals("parry"));
        var bodies=(java.util.List<Wisp>)get(game,"scouts");bodies.clear();
        var identities=(Map<Wisp,CampaignEnemy>)get(game,"campaignEnemies");identities.clear();
        int count=mode.equals("ultimate")?3:1;
        var targets=new ArrayList<CampaignEnemy>();
        for(int i=0;i<count;i++) {
            double x=mode.equals("absorb")?1950:mode.equals("parry")?X+82:X+95+i*100;
            var enemy=new CampaignEnemy(CampaignEnemy.Kind.OUTPOST_SCOUT,x,Y+(i%2==0?0:10));
            set(enemy.body(),"health",400);targets.add(enemy);bodies.add(enemy.body());identities.put(enemy.body(),enemy);
        }
        if(mode.equals("absorb")) {
            var ctor=RuinedOutpostGame.Corpse.class.getDeclaredConstructor(double.class,double.class,boolean.class,CampaignEnemy.Kind.class,int.class);ctor.setAccessible(true);
            var corpses=(java.util.List<RuinedOutpostGame.Corpse>)get(game,"corpses");corpses.clear();
            for(int i=0;i<3;i++)corpses.add(ctor.newInstance(X+65+i*40,Y-70+i*60,true,CampaignEnemy.Kind.OUTPOST_SCOUT,1));
        }
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();set(panel,"reducedEffects",reduced);
        if(mode.equals("parry"))set(panel,"riposteSheet",null);
        var animation=(BladeAnimation)get(panel,"bladeAnimation");animation.face(1,0);
        var world=B2BJ.class.getDeclaredMethod("drawWorld",Graphics2D.class,int.class,int.class);world.setAccessible(true);
        var player=B2BJ.class.getDeclaredMethod("drawPlayer",Graphics2D.class,int.class,int.class);player.setAccessible(true);
        var posed=B2BJ.class.getDeclaredMethod("posed",Graphics2D.class,Wisp.class,int.class,int.class,int.class);posed.setAccessible(true);
        var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);hurt.setAccessible(true);
        GameAudio.stopLoops();GameAudio.setMuted(true);for(int i=0;i<50;i++)GameAudio.mixVoices(new int[735]);
        GameAudio.setMuted(false);GameAudio.setMasterVolume(.9);GameAudio.setSfxVolume(1);GameAudio.setDialogueVolume(0);GameAudio.setMusicVolume(0);
        int frames=mode.equals("absorb")?282:mode.equals("parry")?210:252,peak=0,outputFrame=0,impactBeats=0;
        ByteArrayOutputStream pcm=new ByteArrayOutputStream();
        BufferedImage effects=new BufferedImage(W/2,H/2,BufferedImage.TYPE_INT_ARGB);
        for(int frame=0;frame<frames;frame++) {
            double t=frame/(double)FPS;
            int previousHealth=targets.stream().mapToInt(e->e.body().health()).sum();
            for(var target:targets)if(target.body().state()!=Wisp.State.HURT) {
                set(target.body(),"state",mode.equals("parry")&&frame>=24&&frame<48?Wisp.State.TELEGRAPH:Wisp.State.RECOVER);
                set(target.body(),"stateTime",mode.equals("parry")&&frame>=24&&frame<48?(frame-24)/60.:0.);
            }
            if(mode.equals("ultimate")) {
                if(frame==30)GameAudio.startLoop(GameAudio.Loop.CHARGE,.3);
                if(frame>30&&frame<59)GameAudio.startLoop(GameAudio.Loop.CHARGE,clamp((t-.5)/.48));
                if(frame==59){GameAudio.stopLoop(GameAudio.Loop.CHARGE);GameAudio.play(GameAudio.Cue.DASH_SLASH);}
                if(frame==78)GameAudio.play(GameAudio.Cue.FINISHER_IMPACT);
                for(int i=0;i<3;i++)if(frame==78+i*2){targets.get(i).body().hurt(80,true);targets.get(i).body().push(80,0);}
            } else if(mode.equals("parry")) {
                if(frame==39&&!game.riposte())throw new AssertionError("parry setup failed");
                if(frame==48){hurt.invoke(game,12,X+82,Y);if(!game.bladeCountering())throw new AssertionError("counter did not trigger");}
                if(frame>52&&frame<106){set(targets.get(0).body(),"state",Wisp.State.HURT);set(targets.get(0).body(),"stateTime",.16);}
                if(frame==72&&!game.primaryPress(1,0))throw new AssertionError("parry follow-up failed");
                if(frame==96)game.primaryRelease(1,0);
            } else {
                if(frame==30&&!game.beelzebub(1,0))throw new AssertionError("absorption setup failed");
                if(frame==116&&!game.primaryPress(1,0))throw new AssertionError("follow-up charge failed");
                if(frame==166)game.primaryRelease(1,0);
                if(frame==181){targets.get(0).body().hurt(18,true);GameAudio.play(GameAudio.Cue.CRESCENT_IMPACT);}
            }
            panel.step(1./FPS);
            // The proposal replaces the large guard cross and orange empowered-hit overlays.
            if((mode.equals("parry")&&frame<72)||(mode.equals("absorb")&&frame>=166))((java.util.List<?>)get(panel,"impacts")).clear();
            if(mode.equals("ultimate")) {
                double travel=smooth((t-.98)/.18);game.player().relocate(X+340*travel,Y);
                set(game,"aimX",1000);set(game,"aimY",0);set(game,"attackDelay",0.);set(game,"waterCast",false);
                if(t>=.5&&t<1.72) {
                    int move=t<1.16?RuinedOutpostGame.DASH_SLASH:RuinedOutpostGame.SPIN_CLEAVE;
                    double elapsed=t<.98?(t-.5)/.48*.098:t<1.16?.10+(t-.98)/.18*.14:
                            t<1.30?(t-1.16)/.14*.179:.18+(t-1.30)/.42*.519;
                    set(game,"bladeMove",move);set(game,"bladeDuration",RuinedOutpostGame.strikeDuration(move));
                    set(game,"bladeTime",RuinedOutpostGame.strikeDuration(move)-elapsed);
                    animation.face(1,0);animation.pose(B2BJ.bladeAction(move),elapsed,RuinedOutpostGame.strikeDuration(move));
                } else {set(game,"bladeTime",0.);animation.pose(BladeAnimation.Action.IDLE,t<.5?t:t-1.72,0);}
            }
            int cameraX=1490+(mode.equals("ultimate")&&!reduced?(int)(smooth((t-.9)/.65)*70):0),cameraY=2740;
            var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();Ui.crisp(g);
            double burstAge=mode.equals("ultimate")?t-1.30:mode.equals("parry")?t-.8:t-3.02;
            int strength=reduced?0:(int)(Math.max(0,1-burstAge/.20)*(burstAge>=0?5:0));
            int shakeX=strength==0?0:(frame%3-1)*strength,shakeY=strength==0?0:((frame+1)%3-1)*strength;
            g.translate(shakeX,shakeY);world.invoke(panel,g,cameraX,cameraY);
            if(mode.equals("ultimate")&&!reduced&&t>.65&&t<1.5) {
                float dim=(float)(.17*smooth((t-.65)/.28)*(1-smooth((t-1.30)/.2)));
                g.setColor(new Color(5,4,18,(int)(dim*255)));g.fillRect(-8,-8,W+16,H+16);
                if(t>=.98&&t<1.25)for(int i=3;i>=1;i--) {
                    var echo=(Graphics2D)g.create();echo.setComposite(AlphaComposite.SrcOver.derive(.18f/i));
                    player.invoke(panel,echo,cameraX+i*30,cameraY);echo.dispose();
                }
                player.invoke(panel,g,cameraX,cameraY);
            }
            var fx=effects.createGraphics();fx.setComposite(AlphaComposite.Clear);fx.fillRect(0,0,W/2,H/2);fx.setComposite(AlphaComposite.SrcOver);Ui.crisp(fx);
            if(mode.equals("ultimate"))ultimateFx(fx,t,cameraX,cameraY,reduced);
            else if(mode.equals("parry")) {
                if(t>=.65&&t<.8){fx.setColor(new Color(242,213,137));fx.drawArc(px(X+25,cameraX),px(Y-48,cameraY),12,30,-70,140);}
                burst(fx,px(X+45,cameraX),px(Y-22,cameraY),t-.8,1,true,reduced);
                if(t>.86&&t<1.20)cut(fx,px(X+25,cameraX),px(Y-20,cameraY),px(X+125,cameraX),px(Y-38,cameraY),1-(t-.86)/.34,false);
            } else {
                if(t>1.62&&t<2.98) {
                    int hx=px(game.player().x()+32,cameraX),hy=px(Y-19,cameraY);
                    double intensity=t<1.90?clamp((t-1.62)/.20):.6+.4*clamp((t-1.90)/.85);
                    gather(fx,hx,hy,t,intensity,reduced);
                }
                if(t>2.98&&t<3.40)cut(fx,px(X+30,cameraX),px(Y+15,cameraY),px(X+185,cameraX),px(Y-58,cameraY),1-(t-2.98)/.42,true);
                burst(fx,px(targets.get(0).body().x(),cameraX),px(Y-18,cameraY),t-3.02,1.2,false,reduced);
            }
            fx.dispose();g.drawImage(effects,0,0,W,H,null);g.dispose();
            boolean damaged=targets.stream().mapToInt(e->e.body().health()).sum()<previousHealth;
            int hold=mode.equals("ultimate")?(frame==63?3:frame==78?6:0):mode.equals("parry")?
                    (frame==48?2:damaged&&frame>96?4:0):damaged&&frame!=181?5:0;
            BufferedImage actors=null;
            if(hold>0) {
                impactBeats++;
                System.out.println("impact: simulation="+frame+", output="+outputFrame+", hold="+hold);
                if(!reduced) {
                    actors=new BufferedImage(W/2,H/2,BufferedImage.TYPE_INT_ARGB);
                    var silhouettes=actors.createGraphics();Ui.crisp(silhouettes);silhouettes.scale(.5,.5);silhouettes.translate(shakeX,shakeY);
                    for(var target:targets) {
                        if(mode.equals("ultimate")&&frame==63&&target!=targets.get(0))continue;
                        var body=(Graphics2D)posed.invoke(panel,silhouettes,target.body(),cameraX,cameraY,22);
                        CampaignRenderer.drawEnemy(body,target,cameraX,cameraY);body.dispose();
                    }
                    player.invoke(panel,silhouettes,cameraX,cameraY);silhouettes.dispose();
                    // Keep translucent ground shadows out of the contact silhouettes.
                    for(int ay=0;ay<actors.getHeight();ay++)for(int ax=0;ax<actors.getWidth();ax++)
                        if((actors.getRGB(ax,ay)>>>24)<240)actors.setRGB(ax,ay,0);
                }
            }
            // Freeze the contact pose; keep the mixer advancing so attack tails remain uninterrupted.
            for(int held=0;held<=hold;held++) {
                BufferedImage result=image;
                if(actors!=null&&held<2) {
                    result=impactFrame(image,actors,held,mode.equals("parry")&&frame==48,
                            px(mode.equals("ultimate")?X+(frame==63?95:200):mode.equals("parry")&&frame==48?X+45:targets.get(0).body().x(),cameraX),px(Y-24,cameraY),
                            mode.equals("ultimate")?(frame==63?55:100):45);
                }
                peak=Math.max(peak,writeFrame(result,out,outputFrame++,pcm));
            }
        }
        int expectedFrames=mode.equals("ultimate")?261:mode.equals("parry")?216:287;
        if(outputFrame!=expectedFrames||impactBeats!=(mode.equals("absorb")?1:2))throw new AssertionError("impact timing changed: "+outputFrame+" / "+impactBeats);
        if(mode.equals("ultimate")&&targets.stream().anyMatch(e->e.body().health()!=320))throw new AssertionError("staged ultimate hit count changed");
        if(mode.equals("parry")&&game.player().health()!=100)throw new AssertionError("guard failed");
        if(mode.equals("parry")&&targets.get(0).body().health()>=364)throw new AssertionError("parry follow-up missed");
        if(mode.equals("absorb")&&(!game.corpses().isEmpty()||targets.get(0).body().health()>=382))throw new AssertionError("absorb/charged follow-up did not complete");
        byte[] bytes=pcm.toByteArray();var format=new AudioFormat(22050,16,1,true,false);
        try(var audio=new AudioInputStream(new ByteArrayInputStream(bytes),format,bytes.length/2)){AudioSystem.write(audio,AudioFileFormat.Type.WAVE,out.resolve("sound.wav").toFile());}
        GameAudio.stopLoops();GameAudio.setMuted(true);Files.deleteIfExists(save);
        System.out.println(mode+(reduced?" reduced":"")+": "+outputFrame+" frames, impact beats="+impactBeats+", audio peak="+peak+", target HP="+targets.stream().map(e->e.body().health()).toList());
    }

    private static int writeFrame(BufferedImage image,Path out,int frame,ByteArrayOutputStream pcm)throws Exception {
        ImageIO.write(image,"png",out.resolve(String.format("%03d.png",frame)).toFile());
        int samples=(frame+1)*22050/FPS-frame*22050/FPS,peak=0;int[] mix=new int[samples];GameAudio.mixVoices(mix);
        for(int sample:mix){int limited=GameAudio.limited(sample);peak=Math.max(peak,Math.abs(limited));pcm.write(limited&255);pcm.write((limited>>8)&255);}
        return peak;
    }

    /** Two contact frames: ivory silhouettes, then ink silhouettes with a thin colored edge. */
    private static BufferedImage impactFrame(BufferedImage scene,BufferedImage actors,int phase,boolean gold,int x,int y,int reach) {
        var result=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);var g=result.createGraphics();Ui.crisp(g);
        g.drawImage(scene,0,0,null);g.setColor(new Color(5,5,18,phase==0?215:165));g.fillRect(0,0,W,H);
        var mask=new BufferedImage(W/2,H/2,BufferedImage.TYPE_INT_ARGB);var m=mask.createGraphics();
        m.drawImage(actors,0,0,null);m.setComposite(AlphaComposite.SrcIn);m.setColor(phase==0?WHITE:gold?new Color(245,211,125):LIGHT);m.fillRect(0,0,W/2,H/2);
        g.scale(2,2);
        if(phase==1)for(int[] offset:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})g.drawImage(mask,offset[0],offset[1],null);
        if(phase==1){m.setColor(VOID);m.fillRect(0,0,W/2,H/2);}
        m.dispose();g.drawImage(mask,0,0,null);
        Color edge=gold?new Color(225,175,75):VIOLET;
        g.setColor(edge);g.fillPolygon(new int[]{x-reach-12,x+4,x+reach+12,x-4},new int[]{y+18,y-8,y-18,y+8},4);
        for(int i=0;i<8;i++) {
            double angle=i*Math.PI/4+.15,wide=angle+.20;int radius=(i%2==0?36:21)+(phase==1?8:0);
            g.setColor(i%2==0?WHITE:edge);
            g.fillPolygon(new int[]{x+(int)(Math.cos(angle)*5),x+(int)(Math.cos(angle)*radius),x+(int)(Math.cos(wide)*7)},
                    new int[]{y+(int)(Math.sin(angle)*5),y+(int)(Math.sin(angle)*radius*.65),y+(int)(Math.sin(wide)*7)},3);
        }
        g.setColor(WHITE);g.drawLine(x-reach,y+16,x+reach,y-16);
        if(!gold){g.drawLine(x-12,y-22,x+12,y+22);g.fillRect(x-2,y-3,5,7);}
        g.dispose();return result;
    }

    private static void ultimateFx(Graphics2D g,double t,int cx,int cy,boolean reduced) {
        int x=px(X,cx),y=px(Y-20,cy),end=px(X+350,cx);
        if(t>.5&&t<.98)gather(g,x+15,y,t,clamp((t-.5)/.48),reduced);
        if(t>=.98&&t<1.72) {
            double travel=smooth((t-.98)/.18),power=t<1.30?.7:1-clamp((t-1.30)/.42);
            if(t<1.30)cut(g,x,y,x+(int)((end-x)*travel),y-5,power,true);
            else rift(g,x,y,end,y-5,clamp((t-1.30)/.42));
            if(t<1.15){g.setColor(WHITE);g.drawLine(x,y,x+(int)((end-x)*travel),y-5);}
        }
        for(int i=0;i<3;i++)burst(g,px(X+95+i*100,cx),y,t-(1.30+i/30.),1.45,false,reduced);
    }

    private static void rift(Graphics2D g,int x,int y,int ex,int ey,double p) {
        int n=16;int[] xx=new int[n*2],yy=new int[n*2];
        for(int layer=0;layer<2;layer++) {
            for(int i=0;i<n;i++) {
                double u=i/(double)(n-1),width=Math.sin(u*Math.PI)*(2+Math.sin(p*Math.PI)*13)*(i%3==0?1:.65);
                width=Math.max(0,width-layer*3);
                xx[i]=xx[n*2-i-1]=x+(int)((ex-x)*u);
                yy[i]=y+(int)((ey-y)*u-width);yy[n*2-i-1]=y+(int)((ey-y)*u+width);
            }
            g.setColor(layer==0?new Color(153,96,220,(int)((1-p)*235)):new Color(9,5,23,(int)((1-p)*255)));
            g.fillPolygon(xx,yy,n*2);
        }
        g.setColor(new Color(221,198,255,(int)((1-p)*255)));g.drawLine(x,y,ex,ey);
    }

    /** Tight native-pixel currents converge on the blade, not a screen-sized warning ring. */
    private static void gather(Graphics2D g,int x,int y,double t,double power,boolean reduced) {
        int r=2+(int)(power*5);g.setColor(VOID);g.fillOval(x-r-2,y-r-2,r*2+4,r*2+4);
        g.setColor(VIOLET);g.fillOval(x-r,y-r,r*2,r*2);g.setColor(LIGHT);g.fillRect(x-1,y-r+1,2,Math.max(2,r*2-2));
        if(reduced)return;
        for(int i=0;i<9;i++) {
            double p=(t*1.8+i/9.)%1,a=i*2.4+p*2,dist=5+(1-p)*25;
            int dx=(int)(Math.cos(a)*dist),dy=(int)(Math.sin(a)*dist);
            g.setColor(i%3==0?WHITE:VIOLET);g.drawLine(x+dx,y+dy,x+(int)(dx*.8),y+(int)(dy*.8));
        }
    }

    private static void cut(Graphics2D g,int x,int y,int ex,int ey,double power,boolean dark) {
        if(power<=0)return;int w=2+(int)(power*5),mx=(x+ex)/2,my=(y+ey)/2;
        g.setColor(dark?new Color(75,39,123,(int)(power*170)):new Color(33,123,161,(int)(power*170)));
        g.fillPolygon(new int[]{x,mx,ex,mx},new int[]{y,my-w-4,ey,my+w+4},4);
        g.setColor(dark?VIOLET:new Color(100,229,235));g.fillPolygon(new int[]{x,mx,ex,mx},new int[]{y,my-w,ey,my+w},4);
        g.setColor(dark?VOID:WHITE);g.fillPolygon(new int[]{x,mx,ex,mx},new int[]{y,my-w+2,ey,my+w-2},4);
        g.setColor(dark?LIGHT:WHITE);g.drawLine(x,y-1,ex,ey-1);
    }

    private static void burst(Graphics2D g,int x,int y,double age,double force,boolean gold,boolean reduced) {
        if(age<0||age>.42)return;
        double p=age/.42,alpha=1-p;
        Color edge=gold?new Color(225,175,75):VIOLET,shine=gold?new Color(255,242,179):LIGHT;
        int length=(int)((9+23*Math.sin(Math.min(1,p*1.5)*Math.PI/2))*force);
        g.setColor(new Color(edge.getRed(),edge.getGreen(),edge.getBlue(),(int)(alpha*150)));
        g.fillPolygon(new int[]{x-length,x+4,x+length,x-4},new int[]{y+length/2,y-8,y-length/2,y+8},4);
        g.setColor(new Color(shine.getRed(),shine.getGreen(),shine.getBlue(),(int)(alpha*255)));g.drawLine(x-length,y+length/2,x+length,y-length/2);
        if(age<.11){g.setColor(WHITE);g.fillPolygon(new int[]{x-3,x,x+3,x},new int[]{y,y-15,y,y+15},4);}
        if(reduced)return;
        for(int i=0;i<12;i++) {
            double a=i*2.4,dist=(6+p*34)*force;
            int sx=x+(int)(Math.cos(a)*dist),sy=y+(int)(Math.sin(a)*dist*.65+p*p*9);
            g.setColor(i%3==0?new Color(shine.getRed(),shine.getGreen(),shine.getBlue(),(int)(alpha*255)):
                    new Color(edge.getRed(),edge.getGreen(),edge.getBlue(),(int)(alpha*230)));
            g.drawLine(sx,sy,sx+(int)(Math.cos(a)*4),sy+(int)(Math.sin(a)*4));
        }
    }

    public static void main(String[] args)throws Exception {
        if(args.length<2||!java.util.List.of("ultimate","parry","absorb").contains(args[1]))throw new IllegalArgumentException("output-directory ultimate|parry|absorb [reduced]");
        CombatUi.cinematic=true;boolean reduced=args.length>2&&args[2].equals("reduced");
        SwingUtilities.invokeAndWait(()->{try{render(Path.of(args[0]),args[1],reduced);}catch(Exception e){throw new RuntimeException(e);}});
    }
}
