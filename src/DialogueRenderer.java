import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Cached pixel portraits and the same window frame used by the rest of the game. */
public final class DialogueRenderer {
    private static final BufferedImage MARA=B2BJ.loadImage("assets/characters/npcs/mara_portrait.png");
    private static final BufferedImage MARA_BODY=B2BJ.loadImage("assets/characters/npcs/mara_idle.png");
    private static final BufferedImage BLOB=B2BJ.loadImage("assets/characters/slime/rainoray_portrait.png");
    private static final BufferedImage BLADE=cell(B2BJ.loadImage("assets/characters/blade/rainoray_idle.png"),20,0,40,40);
    private static final BufferedImage WARDEN=cell(B2BJ.loadImage("assets/characters/guardian/warden_motion.png"),0,0,64,64);

    private DialogueRenderer() { }

    private static BufferedImage cell(BufferedImage source,int x,int y,int size,int target) {
        if(source==null||size<=0||x+size>source.getWidth()||y+size>source.getHeight())return null;
        var image=new BufferedImage(target,target,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();Ui.crisp(g);
        g.drawImage(source,0,0,target,target,x,y,x+size,y+size,null);g.dispose();return image;
    }

    public static void draw(Graphics2D g,RuinedOutpostGame game,int width,int height) {
        var scene=game.dialogue();
        if(scene==null||!game.dialogueReady())return;
        var line=scene.lines().get(game.dialogueIndex());
        int y=height-188,right=width-128;
        Ui.veil(g,width,height,scene==OutpostDialogue.WARDEN?28:80);
        Ui.window(g,128,y,width-256,160);
        Ui.nine(g,Ui.Piece.PANEL,144,y+12,136,136);
        var portrait=switch(line.speaker()) {
            case MARA -> MARA;
            case WARDEN -> WARDEN;
            case RAINORAY -> game.player().bladeForm()?BLADE:BLOB;
        };
        int size=portrait==BLADE?120:128;
        if(portrait!=null)g.drawImage(portrait,212-size/2,y+80-size/2,size,size,null);
        String speaker=line.speaker()==OutpostDialogue.Speaker.WARDEN?"OUTPOST WARDEN":line.speaker().name();
        boolean warm=line.speaker()==OutpostDialogue.Speaker.MARA;
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

    public static void drawMara(Graphics2D g,RuinedOutpostGame game,int cameraX,int cameraY) {
        if(MARA_BODY==null)return;
        var hub=game.campaignArea().hub();
        int x=(int)Math.round((hub.x()-112)/2)*2-cameraX,y=(int)Math.round((hub.y()+64)/2)*2-cameraY;
        g.drawImage(MARA_BODY,x-80,y-148,160,160,null);
        if(game.nearCampaignHub()&&!game.blocked())Ui.outlinedCentered(g,"MARA",x,y-144,1,Ui.GOLD);
    }
}
