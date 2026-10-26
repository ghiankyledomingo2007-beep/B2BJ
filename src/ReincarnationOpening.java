import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** The opening uses the dialogue clock, so pause, skip and saved story flags share one owner. */
final class ReincarnationOpening {
    private static final BufferedImage ROAD=B2BJ.loadImage("assets/ui/opening/last-attempt.png");
    private static final String[] SKILLS={"ANALYSIS", "SLIME BODY", "ABSORPTION", "REGENERATION"};
    private static final String[] RESULTS={"ACTIVE", "STABLE", "AVAILABLE", "ACTIVE"};
    private ReincarnationOpening() { }

    static boolean skipHit(int x,int y,int width,int height) {
        double scale=TitleScreen.menuScale(width,height);
        int left=(int)((width-1280*scale)/2),top=(int)((height-720*scale)/2);
        return new Rectangle(left+(int)(1096*scale),top+(int)(24*scale),
                (int)(160*scale),(int)(44*scale)).contains(x,y);
    }

    static void draw(Graphics2D canvas,RuinedOutpostGame game,int width,int height,
            BufferedImage slime,BufferedImage human,boolean reduced) {
        int index=game.dialogueIndex();double elapsed=game.openingLineTime(),clock=elapsed;
        for(int i=0;i<index;i++)clock+=OutpostDialogue.openingSeconds(i);
        double motion=reduced?0:clock;
        if(index<10) {canvas.setColor(new Color(0x050910));canvas.fillRect(0,0,width,height);}
        else Ui.veil(canvas,width,height,(int)Math.max(0,185-(index-10)*35-elapsed*12));
        double scale=TitleScreen.menuScale(width,height);
        var g=(Graphics2D)canvas.create();Ui.crisp(g);
        g.translate((width-1280*scale)/2,(height-720*scale)/2);g.scale(scale,scale);
        if(index==0) {
            if(ROAD!=null)g.drawImage(ROAD,0,0,1280,720,null);
            double close=reduced?0:Math.max(0,(elapsed-3.2)*3);
            if(close>0&&ROAD!=null)g.drawImage(ROAD,-(int)close,-(int)close,1280+(int)(close*2),720+(int)(close*2),null);
            rain(g,motion,reduced);
            Ui.veil(g,1280,720,(int)Math.min(255,Math.max(0,(elapsed-4.6)*190)));
        } else if(index<5) {
            if(index>=2)seal(g,640,300,Math.min(1,(index-2+elapsed/4)*.65),motion,game,reduced);
        } else if(index<10) {
            motes(g,motion);
            Ui.textCentered(g,"RECONSTRUCTION",640,116,2,Ui.GOLD_LIGHT);
            g.setColor(new Color(0x6c5b31));g.fillRect(480,132,320,2);
            seal(g,430,320,2.6,motion,game,reduced);
            double forming=index>5?1:Math.min(1,elapsed/3.6);
            if(slime!=null) {
                var body=(Graphics2D)g.create();
                body.setComposite(AlphaComposite.SrcOver.derive(index>=8?.12f:(float)forming));
                int cell=SlimeAnimation.CELL_SIZE,frame=reduced?0:(int)(motion*5)%(slime.getWidth()/cell);
                int rise=(int)((1-forming)*70);
                body.drawImage(slime,334,224+rise,526,416+rise,frame*cell,0,(frame+1)*cell,cell,null);body.dispose();
            }
            for(int i=0;i<SKILLS.length;i++) {
                double reveal=index>5?1:Math.max(0,Math.min(1,(elapsed-i*.85)/.55));
                if(reveal<=0)continue;
                var row=(Graphics2D)g.create();row.setComposite(AlphaComposite.SrcOver.derive((float)reveal));
                int y=228+i*64;
                row.setColor(Ui.GOLD);row.fillRect(710,y-10,6,6);
                Ui.text(row,SKILLS[i],736,y,2,Ui.TEXT);
                Ui.text(row,RESULTS[i],736,y+23,1,Ui.CYAN);row.dispose();
            }
            if(index>=8) {
                if(human!=null) {
                    var memory=(Graphics2D)g.create();memory.setComposite(AlphaComposite.SrcOver.derive(.85f));
                    memory.drawImage(human,330,180,530,380,0,0,80,80,null);memory.dispose();
                }
                Ui.textCentered(g,"HUMAN FORM",430,474,2,Ui.TEXT);
                Ui.textCentered(g,"INSUFFICIENT ICHOR",430,502,1,Ui.GOLD);
            }
        } else {
            double p=index>10?1:Math.min(1,elapsed/2.2);
            seal(g,640,300+(76-300)*p,2.6+(1-2.6)*p,motion,game,reduced);
            if(index==14)Ui.textCentered(g,"WASD  /  ARROW KEYS TO MOVE",640,532,1,Ui.MUTED);
        }
        // A quiet subtitle field keeps voices readable over both the memory and the waking world.
        Ui.veil(g,1280,36,190);g.setColor(new Color(3,7,12,210));g.fillRect(0,570,1280,150);
        var line=game.dialogueLine();
        Color accent=line.speaker()==OutpostDialogue.Speaker.INNER_VOICE?Ui.GOLD_LIGHT:Ui.CYAN;
        Ui.textCentered(g,line.speaker().label(),640,600,1,accent);
        var lines=Ui.wrap(line.text(),1000,3);int y=638-(lines.size()-1)*17;
        for(String text:lines) {Ui.textCentered(g,text,640,y,3,Ui.TEXT);y+=34;}
        Ui.text(g,"ONE MORE CHANCE",32,36,1,Ui.MUTED);
        Ui.textRight(g,"X  SKIP",1232,48,2,Ui.MUTED);
        Ui.textCentered(g,"ENTER  NEXT     ESC  PAUSE",640,698,1,Ui.DIM);
        g.dispose();
    }

    private static void seal(Graphics2D g,double x,double y,double size,double time,
            RuinedOutpostGame game,boolean reduced) {
        if(size<=0)return;
        var symbol=(Graphics2D)g.create();symbol.translate(x,y);symbol.scale(size,size);symbol.translate(-640,-76);
        boolean speaking=game.dialogueLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
        InnerVoiceOverlay.drawPresence(symbol,time,GameAudio.dialogueLevel(),1280,reduced,speaking);symbol.dispose();
    }

    private static void rain(Graphics2D g,double time,boolean reduced) {
        g.setColor(new Color(168,197,211,70));
        for(int i=0;i<56;i++) {
            int x=Math.floorMod(i*139,1280),y=(int)(i*71+(reduced?0:time*290))%560;
            g.fillRect(x,y,2,12+i%4*2);
        }
    }

    private static void motes(Graphics2D g,double time) {
        for(int i=0;i<60;i++) {
            int x=Math.floorMod(i*127+41,1240)+20,y=(int)(Math.floorMod(i*97,500)-time*(5+i%3));
            y=Math.floorMod(y,500)+40;
            g.setColor(new Color(230,191,98,30+i%5*12));g.fillRect(x,y,2,2);
        }
    }
}
