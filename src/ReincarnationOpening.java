import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Every shot follows the dialogue clock: pausing, advancing and skipping have one owner. */
final class ReincarnationOpening {
    private static final BufferedImage OFFICE=B2BJ.loadImage("assets/ui/opening/office-night-atlas.png");
    private static final String[] SKILLS={"ANALYSIS","SLIME BODY","ABSORPTION","REGENERATION"};
    private static final String[] RESULTS={"ACTIVE","STABLE","AVAILABLE","ACTIVE"};
    private ReincarnationOpening() { }

    static boolean skipHit(int x,int y,int width,int height) {
        double scale=TitleScreen.menuScale(width,height);
        int left=(int)((width-1280*scale)/2),top=(int)((height-720*scale)/2);
        return new Rectangle(left+(int)(1096*scale),top+(int)(24*scale),
                (int)(160*scale),(int)(44*scale)).contains(x,y);
    }

    private static double since(RuinedOutpostGame game,int first) {
        double time=game.openingLineTime();
        for(int i=first;i<game.dialogueIndex();i++)time+=OutpostDialogue.openingSeconds(i);
        return time;
    }

    /** Return to the exact gameplay transform before the final line ends. */
    static double worldReveal(RuinedOutpostGame game) {
        if(!game.opening())return 1;
        if(game.dialogueIndex()<10)return 0;
        double duration=0;
        for(int i=10;i<14;i++)duration+=OutpostDialogue.openingSeconds(i);
        return smooth(since(game,10)/(duration+OutpostDialogue.openingSeconds(14)*.55));
    }

    static void frameWorld(Graphics2D g,RuinedOutpostGame game,int width,int height,
            int cameraX,int cameraY,boolean reduced) {
        if(!game.opening()||game.dialogueIndex()<10||reduced)return;
        double weight=1-worldReveal(game),zoom=1+1.35*weight;
        double x=game.player().x()-cameraX,y=game.player().y()-cameraY;
        g.translate((width*.5-x)*weight,(height*.51-y)*weight);
        g.translate(x,y);g.scale(zoom,zoom);g.translate(-x,-y);
    }

    static void draw(Graphics2D canvas,RuinedOutpostGame game,int width,int height,
            BufferedImage slime,BufferedImage human,boolean reduced) {
        int index=game.dialogueIndex();double elapsed=game.openingLineTime();
        double motion=reduced?0:since(game,0);
        if(index<10) {canvas.setColor(new Color(0x050910));canvas.fillRect(0,0,width,height);}
        else Ui.veil(canvas,width,height,(int)((1-smooth(since(game,10)/1.8))*245));
        double scale=TitleScreen.menuScale(width,height);
        var g=(Graphics2D)canvas.create();Ui.crisp(g);
        g.translate((width-1280*scale)/2,(height-720*scale)/2);g.scale(scale,scale);
        if(index==0)memory(g,elapsed,motion,reduced);
        else if(index<5)awakening(g,game,motion,reduced);
        else if(index<10)reconstruction(g,game,slime,human,motion,reduced);
        else waking(g,game,motion,reduced);
        int top=(int)((height-720*scale)/2),bottom=top+(int)(590*scale);
        canvas.setColor(new Color(3,7,12));canvas.fillRect(0,0,width,top+(int)(62*scale));
        boolean world=index>=10;                          // awake in the world: the game's own dialogue window
        if(!world)canvas.fillRect(0,bottom,width,height-bottom);
        subtitles(g,game,world);g.dispose();
        if(world) {
            var window=(Graphics2D)canvas.create();
            window.setComposite(AlphaComposite.SrcOver.derive((float)smooth(since(game,10)/1.8)));
            DialogueRenderer.window(window,game,game.dialogueLine(),width,height,reduced,true);window.dispose();
        }
    }

    private static void memory(Graphics2D g,double elapsed,double time,boolean reduced) {
        var shot=(Graphics2D)g.create();double approach=reduced?0:smooth(elapsed/8.5);
        shot.translate(870,345);shot.scale(1.025+approach*.105,1.025+approach*.105);shot.translate(-870,-345);
        double pose=elapsed<3.2?0:elapsed<4.6?smooth((elapsed-3.2)/.55)
                :elapsed<5.8?1+smooth((elapsed-4.6)/.6):2+smooth((elapsed-5.8)/.7);
        int frame=Math.min(3,(int)pose);officeFrame(shot,frame,1);
        if(frame<3)officeFrame(shot,frame+1,pose-frame);
        double alive=1-smooth((elapsed-6.5)/1.5),flicker=reduced?.5:.5+.5*Math.sin(time*8.3);
        halo(shot,255,325,145,new Color(105,194,218,(int)(5+alive*flicker*10)));
        halo(shot,410,370,95,new Color(255,185,83,13));
        var window=(Graphics2D)shot.create();window.clipRect(340,64,240,235);rain(window,time,reduced);window.dispose();
        shot.dispose();vignette(g,60);
        Ui.veil(g,1280,720,(int)(255*smooth((elapsed-8)/1.75)));
    }

    private static void officeFrame(Graphics2D g,int frame,double opacity) {
        if(OFFICE==null||opacity<=0)return;
        var pose=(Graphics2D)g.create();pose.setComposite(AlphaComposite.SrcOver.derive((float)opacity));
        int col=frame%2,row=frame/2,w=OFFICE.getWidth(),h=OFFICE.getHeight();
        pose.drawImage(OFFICE,0,0,1280,720,col*w/2,row*h/2,(col+1)*w/2,(row+1)*h/2,null);pose.dispose();
    }

    private static void awakening(Graphics2D g,RuinedOutpostGame game,double time,boolean reduced) {
        int index=game.dialogueIndex();
        double presence=index<2?0:index>2?1:smooth(game.openingLineTime()/1.6);
        atmosphere(g,time,presence);
        if(index<2) {halo(g,640,300,90,new Color(100,205,220,12));return;}
        halo(g,640,290,280,new Color(236,173,62,(int)(26*presence)));
        var symbol=(Graphics2D)g.create();symbol.setComposite(AlphaComposite.SrcOver.derive((float)presence));
        OpeningSeal.draw(symbol,640,310,174,since(game,2),time,GameAudio.dialogueLevel(),reduced);symbol.dispose();
        floor(g,640,472,210,time,presence);
        if(index==4) {
            var name=(Graphics2D)g.create();name.setComposite(AlphaComposite.SrcOver.derive((float)smooth(game.openingLineTime()/.7)));
            JourneyHud.center(name,"Raphael",640,500,Ui.menuFont(26),JourneyHud.GOLD);name.dispose();
        }
    }

    private static void reconstruction(Graphics2D g,RuinedOutpostGame game,BufferedImage slime,
            BufferedImage human,double time,boolean reduced) {
        int index=game.dialogueIndex();double elapsed=game.openingLineTime();
        double forming=index>5?1:smooth(elapsed/3.5);
        atmosphere(g,time,1);halo(g,640,350,290,new Color(44,166,170,24));
        var backdrop=(Graphics2D)g.create();backdrop.setComposite(AlphaComposite.SrcOver.derive(.24f));
        OpeningSeal.draw(backdrop,640,302,198,since(game,2),time,GameAudio.dialogueLevel(),reduced);backdrop.dispose();floor(g,640,478,235,time,1);
        int bob=reduced?0:snap(Math.sin(time*1.6)*7);
        int frame=slime==null?0:reduced?0:(int)(time*5)%(slime.getWidth()/SlimeAnimation.CELL_SIZE);
        if(index<8) {
            if(forming<1) {
                halo(g,640,320,180,new Color(109,248,235,(int)((1-forming)*44)));
                g.setColor(new Color(116,243,232,(int)((1-forming)*24)));
                g.fillPolygon(new int[]{620,660,760,520},new int[]{130,130,480,480},4);
            }
            body(g,slime,frame,496,190+bob,6,forming,reduced);
            if(slime!=null&&forming>.7) {
                var reflection=(Graphics2D)g.create();reflection.setComposite(AlphaComposite.SrcOver.derive(.12f));
                int cell=SlimeAnimation.CELL_SIZE;
                reflection.drawImage(slime,496,535,784,477,frame*cell,0,(frame+1)*cell,cell,null);reflection.dispose();
            }
            skills(g,index==5?elapsed:5.5,time,reduced);
        } else {
            double reveal=index>8?1:smooth(elapsed/1.1);
            var memory=(Graphics2D)g.create();memory.setComposite(AlphaComposite.SrcOver.derive((float)(.7*reveal)));
            if(human!=null) {
                int cell=B2BJ.bladeCell(human,BladeAnimation.Action.IDLE),pose=reduced?0:(int)(time*4)%(human.getWidth()/cell);
                int margin=(cell-80)*2;
                memory.drawImage(human,480-margin,142+bob-margin,800+margin,462+bob+margin,pose*cell,0,(pose+1)*cell,cell,null);
                memory.setColor(new Color(5,9,16,65));
                for(int y=146;y<464;y+=6)memory.fillRect(480,y,320,2);
            }
            memory.dispose();JourneyHud.center(g,"Human form",640,505,Ui.menuFont(24),MenuTheme.IVORY);
            JourneyHud.center(g,"Requires Ichor",640,530,JourneyHud.SMALL,JourneyHud.GOLD);
            for(int side:new int[]{-1,1}) {
                g.setColor(new Color(117,226,225,90));int x=640+side*178;
                g.fillRect(x,206,2,174);g.fillRect(x-(side>0?12:0),206,14,2);
            }
        }
        vignette(g,44);
    }

    /** Native sprite pixels converge independently, then hand back to the ordinary idle sheet. */
    private static void body(Graphics2D g,BufferedImage sheet,int frame,int x,int y,int scale,double progress,boolean reduced) {
        if(sheet==null)return;
        int cell=SlimeAnimation.CELL_SIZE;
        if(progress>=1||reduced) {
            g.setComposite(AlphaComposite.SrcOver.derive((float)progress));
            g.drawImage(sheet,x,y,x+cell*scale,y+cell*scale,frame*cell,0,(frame+1)*cell,cell,null);
            g.setComposite(AlphaComposite.SrcOver);return;
        }
        for(int sy=0;sy<cell;sy++)for(int sx=0;sx<cell;sx++) {
            int pixel=sheet.getRGB(frame*cell+sx,sy),alpha=pixel>>>24;if(alpha==0)continue;
            int seed=Math.floorMod(sx*97+sy*131,101);
            double local=smooth((progress-seed/101.*.3)/.7),away=1-local;
            double angle=Math.atan2(sy-cell*.5,sx-cell*.5)+away*.7;
            int px=x+sx*scale+snap(Math.cos(angle)*(100+seed*1.8)*away);
            int py=y+sy*scale+snap(Math.sin(angle)*(70+seed)*away);
            int r=pixel>>16&255,green=pixel>>8&255,b=pixel&255;
            g.setColor(new Color((int)(r+(174-r)*away),(int)(green+(255-green)*away),
                    (int)(b+(244-b)*away),(int)(alpha*(.08+.92*progress))));
            g.fillRect(px,py,scale,scale);
        }
    }

    private static void skills(Graphics2D g,double elapsed,double time,boolean reduced) {
        for(int i=0;i<SKILLS.length;i++) {
            double reveal=smooth((elapsed-.6-i*.85)/.65);if(reveal<=0)continue;
            boolean left=i%2==0;int x=left?270:1010,y=264+(i/2)*126;
            var label=(Graphics2D)g.create();label.setComposite(AlphaComposite.SrcOver.derive((float)reveal));
            label.setColor(new Color(219,179,93,70));label.drawLine(left?x+90:x-90,y-8,left?474:806,y-8);
            JourneyHud.center(label,SKILLS[i],x,y,Ui.menuFont(22),MenuTheme.IVORY);
            JourneyHud.center(label,RESULTS[i],x,y+25,Ui.menuFont(13),Ui.CYAN);
            int pulse=reduced?0:snap(Math.sin(time*2+i)*2);
            label.setColor(Ui.GOLD_LIGHT);diamond(label,x,y-34,3+pulse);label.dispose();
        }
    }

    private static void waking(Graphics2D g,RuinedOutpostGame game,double time,boolean reduced) {
        double age=since(game,10),eyes=smooth(age/1.8);
        if(!reduced&&eyes<1) {
            int lid=(int)((1-eyes)*290);g.setColor(new Color(3,7,12));
            g.fillRect(0,0,1280,lid);g.fillRect(0,590-lid,1280,lid+130);   // no bottom bar here: close to the edge
        }
        double settle=smooth(age/2.2);
        if(settle<1) {                                    // the big seal hands over to the one in the dialogue window
            var fading=(Graphics2D)g.create();fading.setComposite(AlphaComposite.SrcOver.derive((float)(1-settle)));
            seal(fading,640,285-153*settle,3.35-2.25*settle,time,game,reduced);fading.dispose();
        }
        if(game.dialogueIndex()>=12) {
            double reveal=smooth(since(game,12)/1.2);
            var title=(Graphics2D)g.create();title.setComposite(AlphaComposite.SrcOver.derive((float)reveal));
            JourneyHud.text(title,"Chapter I",88,144,JourneyHud.SMALL,JourneyHud.GOLD);
            JourneyHud.text(title,"Ruined Outpost",88,181,Ui.menuFont(28),MenuTheme.IVORY);
            title.setColor(Ui.GOLD);title.fillRect(88,194,(int)(112*reveal),2);title.dispose();
        }
        if(game.dialogueIndex()==14)JourneyHud.center(g,"WASD / Arrow keys to move",640,500,JourneyHud.SMALL,MenuTheme.IVORY);
        vignette(g,40);
    }

    private static void subtitles(Graphics2D g,RuinedOutpostGame game,boolean window) {
        JourneyHud.text(g,"One more chance",32,38,JourneyHud.SMALL,MenuTheme.MUTED);
        JourneyHud.right(g,"X · Skip",1232,48,JourneyHud.LABEL,MenuTheme.MUTED);
        if(window)return;                                 // the dialogue window carries the line
        var line=game.dialogueLine();Color accent=line.speaker()==OutpostDialogue.Speaker.INNER_VOICE?Ui.GOLD_LIGHT:Ui.CYAN;
        JourneyHud.center(g,line.speaker().label(),640,618,JourneyHud.SMALL,accent);
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),90));
        g.fillRect(442,612,70,2);g.fillRect(768,612,70,2);
        var lines=JourneyHud.wrap(g,line.text(),Ui.heading(24),1060);int y=655-(lines.size()-1)*14;
        for(String text:lines) {JourneyHud.center(g,text,640,y,Ui.heading(24),MenuTheme.IVORY);y+=28;}
        JourneyHud.center(g,"Enter · Next     Esc · Pause",640,702,Ui.heading(13),MenuTheme.MUTED);
    }

    private static void atmosphere(Graphics2D g,double time,double light) {
        halo(g,640,330,390,new Color(31,64,75,65));halo(g,640,460,250,new Color(154,116,58,(int)(20*light)));
        for(int i=0;i<84;i++) {
            double depth=.35+(i%5)*.17;
            int x=Math.floorMod(i*127+41,1240)+20,y=Math.floorMod((int)(i*97-time*depth*9),480)+66;
            g.setColor(new Color(195,211,204,18+i%5*12));g.fillRect(x/2*2,y/2*2,i%5==0?4:2,2);
        }
    }

    private static void floor(Graphics2D g,int cx,int cy,int radius,double time,double opacity) {
        var floor=(Graphics2D)g.create();floor.setStroke(new BasicStroke(2));
        for(int i=0;i<4;i++) {
            double p=(time*.16+i*.25)%1;int r=(int)(radius*(.48+p*.52));
            floor.setColor(new Color(101,224,214,(int)((1-p)*50*opacity)));floor.drawOval(cx-r,cy-r/7,r*2,r*2/7);
        }
        floor.setColor(new Color(229,190,102,(int)(52*opacity)));floor.drawOval(cx-radius,cy-29,radius*2,58);
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12+time*.05;
            floor.fillRect(cx+snap(Math.cos(a)*radius),cy+snap(Math.sin(a)*29),4,2);
        }
        floor.dispose();
    }

    private static void seal(Graphics2D g,double x,double y,double size,double time,RuinedOutpostGame game,boolean reduced) {
        var symbol=(Graphics2D)g.create();symbol.translate(x,y);symbol.scale(size,size);symbol.translate(-640,-76);
        boolean speaking=game.dialogueLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
        InnerVoiceOverlay.drawPresence(symbol,time,GameAudio.dialogueLevel(),1280,reduced,speaking);symbol.dispose();
    }

    private static void rain(Graphics2D g,double time,boolean reduced) {
        for(int i=0;i<88;i++) {
            int depth=1+i%3,x=Math.floorMod(i*139+snap(time*depth*8),1280);
            int y=Math.floorMod((int)(i*71+time*(140+depth*75)),720);
            g.setColor(new Color(164,200,218,28+depth*16));g.fillRect(x/2*2,y/2*2,2,8+depth*7);
            if(i%3==0) {
                double p=(time*.8+i*.17)%1;int sx=Math.floorMod(i*107,1280),sy=440+i%11*12;
                g.setColor(new Color(160,210,222,reduced?30:(int)((1-p)*45)));g.drawOval(sx-snap(p*12),sy,snap(p*24)+4,2);
            }
        }
    }

    private static void halo(Graphics2D g,int x,int y,int radius,Color color) {
        g.setPaint(new RadialGradientPaint(x,y,radius,new float[]{0,.45f,1},new Color[]{color,
                new Color(color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha()/2),
                new Color(color.getRed(),color.getGreen(),color.getBlue(),0)}));g.fillOval(x-radius,y-radius,radius*2,radius*2);
    }
    private static void vignette(Graphics2D g,int strength) {
        Ui.fade(g,0,62,160,528,strength,0);Ui.fade(g,1120,62,160,528,0,strength);
    }
    private static void diamond(Graphics2D g,int x,int y,int r) {
        g.fillPolygon(new int[]{x,x+r,x,x-r},new int[]{y-r,y,y+r,y},4);
    }
    private static int snap(double n) {return (int)Math.round(n/2)*2;}
    private static double smooth(double n) {double p=Math.max(0,Math.min(1,n));return p*p*(3-2*p);}
}
