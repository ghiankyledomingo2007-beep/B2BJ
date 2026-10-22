import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Cached pixel portraits and the same window frame used by the rest of the game. */
public final class DialogueRenderer {
    private static final BufferedImage MARA_SHEET=B2BJ.loadImage("assets/characters/npcs/mara_sheet.png");
    private static final BufferedImage MARA=cell(MARA_SHEET,0,0,MARA_SHEET==null?0:MARA_SHEET.getHeight(),64);
    private static final BufferedImage MARA_BODY=fit("assets/characters/npcs/mara_idle.png",80);
    private static final BufferedImage BLOB=fit("assets/characters/slime/rainoray_portrait.png",64);
    private static final BufferedImage BLADE=cell(B2BJ.loadImage("assets/characters/blade/rainoray_idle.png"),20,0,40,40);
    private static final BufferedImage WARDEN=cell(B2BJ.loadImage("assets/characters/guardian/warden_motion.png"),0,0,64,64);

    private DialogueRenderer() { }

    private static BufferedImage fit(String path,int target) {
        var source=B2BJ.loadImage(path);
        if(source==null)return null;
        double scale=(double)target/Math.max(source.getWidth(),source.getHeight());
        int width=(int)Math.round(source.getWidth()*scale),height=(int)Math.round(source.getHeight()*scale);
        var image=new BufferedImage(target,target,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();Ui.crisp(g);
        g.drawImage(source,(target-width)/2,(target-height)/2,width,height,null);g.dispose();return image;
    }

    private static BufferedImage cell(BufferedImage source,int x,int y,int size,int target) {
        if(source==null||size<=0||x+size>source.getWidth()||y+size>source.getHeight())return null;
        var image=new BufferedImage(target,target,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();Ui.crisp(g);
        g.drawImage(source,0,0,target,target,x,y,x+size,y+size,null);g.dispose();return image;
    }

    public static void draw(Graphics2D g,RuinedOutpostGame game,int width,int height) {
        var scene=game.dialogue();
        if(scene==null)return;
        var line=scene.lines().get(game.dialogueIndex());
        int y=height-240;
        Ui.veil(g,width,height,80);
        Ui.window(g,48,y,width-96,208);
        Ui.nine(g,Ui.Piece.PANEL,76,y+20,160,160);
        var portrait=switch(line.speaker()) {
            case MARA -> MARA;
            case WARDEN -> WARDEN;
            case RAINORAY -> game.player().bladeForm()?BLADE:BLOB;
        };
        int size=portrait==BLADE?120:128;
        if(portrait!=null)g.drawImage(portrait,156-size/2,y+100-size/2,size,size,null);
        String speaker=line.speaker()==OutpostDialogue.Speaker.WARDEN?"OUTPOST WARDEN":line.speaker().name();
        Ui.text(g,speaker,264,y+42,2,line.speaker()==OutpostDialogue.Speaker.MARA?Ui.GOLD:Ui.CYAN);
        Ui.textRight(g,"Skip",width-80,y+42,2,Ui.MUTED);
        Ui.wrapped(g,line.text(),264,y+82,width-352,2,30,3,Ui.TEXT);
        g.setColor(Ui.GOLD);
        for(int step=0;step<4;step++)g.fillRect(width-88+step*2,y+172+step*2,2,14-step*4);
    }

    static boolean skipHit(int x,int y,int width,int height) {
        return new java.awt.Rectangle(width-152,height-226,88,44).contains(x,y);
    }

    public static void drawMara(Graphics2D g,RuinedOutpostGame game,int cameraX,int cameraY) {
        if(MARA_BODY==null)return;
        var hub=game.campaignArea().hub();
        int x=(int)Math.round((hub.x()-112)/2)*2-cameraX,y=(int)Math.round((hub.y()+64)/2)*2-cameraY;
        g.drawImage(MARA_BODY,x-80,y-148,160,160,null);
        if(game.nearCampaignHub()&&!game.blocked())Ui.outlinedCentered(g,"MARA",x,y-144,1,Ui.GOLD);
    }
}
