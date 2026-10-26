import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Cached pixel portraits and the same window frame used by the rest of the game. */
public final class DialogueRenderer {
    private static final BufferedImage MARA=B2BJ.loadImage("assets/characters/npcs/mara_portrait.png");
    private static final BufferedImage MARA_BODY=B2BJ.loadImage("assets/characters/npcs/mara_idle.png");
    private static final BufferedImage SEDGE=B2BJ.loadImage("assets/characters/npcs/sedge_portrait.png");
    private static final BufferedImage SEDGE_BODY=B2BJ.loadImage("assets/characters/npcs/sedge_idle.png");
    private static final BufferedImage[] MARA_IDLE=maraFrames();
    private static final BufferedImage BLOB=B2BJ.loadImage("assets/characters/slime/rainoray_portrait.png");
    // Every portrait is native art drawn at the game's 2x: 64 native pixels fill the 128 frame.
    private static final BufferedImage BLADE=cell(B2BJ.loadImage("assets/characters/blade/rainoray_idle.png"),8,0,64,64);
    private static final BufferedImage WARDEN=cell(B2BJ.loadImage("assets/characters/guardian/warden_motion.png"),0,0,128,64);
    private static final BufferedImage SURVIVORS=B2BJ.loadImage("assets/characters/npcs/survivor_portraits.png");
    private static final BufferedImage[] SURVIVOR_FACES={face(0),face(1),face(2)};
    private static BufferedImage face(int column) {
        if(SURVIVORS==null)return null;
        int size=Math.min(SURVIVORS.getWidth()/3,SURVIVORS.getHeight());
        return cell(SURVIVORS,column*SURVIVORS.getWidth()/3,0,size,64);
    }

    private DialogueRenderer() { }

    /** Quiet character speech uses the sage's typography without its magical seal. */
    static void drawExploration(Graphics2D canvas,OutpostDialogue.Line line,double elapsed,double remaining,
            int width,int height) {
        if(elapsed<0||remaining<=0||!Double.isFinite(elapsed+remaining))return;
        float fade=(float)Math.max(0,Math.min(1,Math.min(elapsed/.20,remaining/.30)));
        var g=(Graphics2D)canvas.create();
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(fade));
        var label=Ui.heading(16);var body=Ui.heading(21);
        int cx=width/2,baseline=height-158;
        int textWidth=g.getFontMetrics(body).stringWidth(line.text());
        int labelHalf=g.getFontMetrics(label).stringWidth(line.speaker().label())/2;
        int half=Math.max(160,(textWidth+47)/4*2);
        Ui.fade(g,cx-half,baseline-46,half,54,0,160);
        Ui.fade(g,cx,baseline-46,half,54,160,0);
        java.awt.Color accent=line.speaker()==OutpostDialogue.Speaker.RAINORAY?Ui.CYAN:Ui.GOLD_LIGHT;
        g.setStroke(new java.awt.BasicStroke(1));g.setColor(accent);
        g.drawLine(cx-labelHalf-46,baseline-32,cx-labelHalf-14,baseline-32);
        g.drawLine(cx+labelHalf+14,baseline-32,cx+labelHalf+46,baseline-32);
        Ui.prose(g,line.speaker().label(),cx-labelHalf,baseline-26,label,Ui.SHADOW);
        Ui.prose(g,line.speaker().label(),cx-labelHalf,baseline-27,label,accent);
        Ui.prose(g,line.text(),cx-textWidth/2,baseline+1,body,Ui.SHADOW);
        Ui.prose(g,line.text(),cx-textWidth/2,baseline,body,Ui.TEXT);
        g.dispose();
    }

    private static BufferedImage cell(BufferedImage source,int x,int y,int size,int target) {
        if(source==null||size<=0||x+size>source.getWidth()||y+size>source.getHeight())return null;
        var image=new BufferedImage(target,target,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();Ui.crisp(g);
        g.drawImage(source,0,0,target,target,x,y,x+size,y+size,null);g.dispose();return image;
    }

    public static void draw(Graphics2D g,RuinedOutpostGame game,int width,int height) {
        var scene=game.dialogue();
        if(scene==null||!game.dialogueReady())return;
        var line=game.dialogueLine();
        int y=height-188,right=width-128;
        Ui.veil(g,width,height,game.bossIntroduction()?28:80);
        Ui.window(g,128,y,width-256,160);
        Ui.nine(g,Ui.Piece.PANEL,144,y+12,136,136);
        var portrait=switch(line.speaker()) {
            case MARA -> MARA;
            case WARDEN -> WARDEN;
            case RAINORAY -> game.player().bladeForm()?BLADE:BLOB;
            case TOBIN -> SURVIVOR_FACES[0];
            case ILSE -> SURVIVOR_FACES[1];
            case VENN -> SURVIVOR_FACES[2];
            case INNER_VOICE -> null;
            case SEDGE -> SEDGE;
        };
        int size=128;
        if(portrait!=null)g.drawImage(portrait,212-size/2,y+80-size/2,size,size,null);
        else if(line.speaker()==OutpostDialogue.Speaker.INNER_VOICE) {
            var seal=(Graphics2D)g.create();seal.translate(148,y+22);
            InnerVoiceOverlay.drawPresence(seal,0,0,128,true,true);seal.dispose();
        }
        String speaker=line.speaker()==OutpostDialogue.Speaker.WARDEN?"OUTPOST WARDEN":line.speaker().label();
        boolean warm=line.speaker()!=OutpostDialogue.Speaker.RAINORAY&&line.speaker()!=OutpostDialogue.Speaker.WARDEN;
        Ui.drawTitle(g,speaker,304,y+16,2,warm?Ui.GOLD_RAMP:Ui.CYAN_RAMP);
        Ui.textRight(g,"Skip",right-32,y+34,2,Ui.MUTED);
        g.setColor(Ui.DIM);g.fillRect(304,y+50,width-480,2);
        g.setColor(warm?Ui.GOLD:Ui.TEAL);g.fillRect(304,y+50,48,2);
        var lines=Ui.wrap(line.text(),width-480,3);
        int baseline=y+94+(2-lines.size())*12;
        for(String text:lines) {Ui.text(g,text,304,baseline,3,Ui.TEXT);baseline+=36;}
        g.setColor(Ui.GOLD);
        for(int step=0;step<4;step++)g.fillRect(right-32+step*2,y+130+step*2,2,14-step*4);
    }

    static boolean skipHit(int x,int y,int width,int height) {
        return new java.awt.Rectangle(width-224,height-180,80,40).contains(x,y);
    }

    public static void drawSedge(Graphics2D g,RuinedOutpostGame game,int cameraX,int cameraY,double seconds,boolean reduced) {
        if(SEDGE_BODY==null)return;
        var hub=game.campaignArea().hub();int x=(int)Math.round(hub.x()-112)-cameraX,y=(int)Math.round(hub.y()+64)-cameraY;
        int pose=reduced?0:(int)(seconds/.7)%6==3?1:0;
        g.drawImage(SEDGE_BODY,x-80,y-148,x+80,y+12,pose*80,0,pose*80+80,80,null);
        if(game.nearCampaignHub()&&!game.blocked())Ui.outlinedCentered(g,"SOUEI",x,y-144,1,Ui.GOLD);
    }

    /** Small native-pixel poses from the existing cutout; boots never move and no colours are blended. */
    private static BufferedImage[] maraFrames() {
        if(MARA_BODY==null)return null;
        int[] breath={0,0,0,1,1,1,1,1,0,0,0,0};
        int[] weight={0,0,0,0,1,1,1,1,1,0,0,0};
        int[] cloth={0,1,1,0,0,-1,-1,0,1,1,0,0};
        var frames=new BufferedImage[breath.length];
        int width=MARA_BODY.getWidth(),height=MARA_BODY.getHeight();
        for(int frame=0;frame<frames.length;frame++) {
            var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int sy=y+(y<48?breath[frame]:0);
                int shift=y<48?weight[frame]:0;
                // Cloak edges and relaxed sleeves catch the breeze; the centre and boots stay anchored.
                if(y>=36&&y<59&&(x<33||x>46))shift+=cloth[frame];
                int sx=x-shift;
                if(sx>=0&&sx<width&&sy<height)image.setRGB(x,y,MARA_BODY.getRGB(sx,sy));
            }
            frames[frame]=image;
        }
        return frames;
    }

    static BufferedImage maraFrame(double seconds,boolean reduced) {
        if(MARA_IDLE==null)return null;
        int frame=reduced||!Double.isFinite(seconds)||seconds<0?0:(int)((seconds%4.8)/.4);
        return MARA_IDLE[frame];
    }

    public static void drawMara(Graphics2D g,RuinedOutpostGame game,int cameraX,int cameraY,
            double seconds,boolean reduced) {
        if(MARA_BODY==null)return;
        var hub=game.campaignArea().hub();
        int x=(int)Math.round((hub.x()-112)/2)*2-cameraX,y=(int)Math.round((hub.y()+64)/2)*2-cameraY;
        g.drawImage(maraFrame(seconds,reduced),x-80,y-148,160,160,null);
        if(game.nearCampaignHub()&&!game.blocked())Ui.outlinedCentered(g,"SHUNA",x,y-144,1,Ui.GOLD);
    }
}
