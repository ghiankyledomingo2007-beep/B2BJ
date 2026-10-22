import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Cached pixel portraits and the same window frame used by the rest of the game. */
public final class DialogueRenderer {
    private static final BufferedImage MARA_SHEET=B2BJ.loadImage("assets/characters/npcs/mara_sheet.png");
    private static final BufferedImage MARA=cell(MARA_SHEET,0,0,MARA_SHEET==null?0:MARA_SHEET.getHeight(),64);
    private static final BufferedImage MARA_BODY=cell(MARA_SHEET,MARA_SHEET==null?0:MARA_SHEET.getHeight(),0,
            MARA_SHEET==null?0:MARA_SHEET.getHeight(),64);
    private static final BufferedImage BLOB=B2BJ.loadImage("assets/ui/blob.png");
    private static final BufferedImage BLADE=cell(B2BJ.loadImage("assets/characters/blade/rainoray_idle.png"),20,0,40,64);
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
        if(portrait!=null)g.drawImage(portrait,92,y+36,128,128,null);
        String speaker=line.speaker()==OutpostDialogue.Speaker.WARDEN?"OUTPOST WARDEN":line.speaker().name();
        Ui.text(g,speaker,264,y+42,2,line.speaker()==OutpostDialogue.Speaker.MARA?Ui.GOLD:Ui.CYAN);
        Ui.textRight(g,scene.title(),width-204,y+42,1,Ui.MUTED);
        Ui.textRight(g,(game.dialogueIndex()+1)+" / "+scene.lines().size(),width-80,y+42,2,Ui.MUTED);
        Ui.wrapped(g,line.text(),264,y+82,width-352,2,30,3,Ui.TEXT);
        Ui.text(g,"ENTER / CLICK  "+(game.dialogueIndex()+1==scene.lines().size()?"CLOSE":"CONTINUE"),264,y+184,2,Ui.MUTED);
        Ui.textRight(g,"X  SKIP",width-80,y+184,2,Ui.GOLD);
    }

    public static void drawMara(Graphics2D g,RuinedOutpostGame game,int cameraX,int cameraY) {
        if(MARA_BODY==null)return;
        var hub=game.campaignArea().hub();
        int x=(int)hub.x()-112-cameraX,y=(int)hub.y()+64-cameraY;
        g.drawImage(MARA_BODY,x-64,y-120,128,128,null);
        if(game.nearCampaignHub()&&!game.blocked())Ui.outlinedCentered(g,"MARA",x,y-126,1,Ui.GOLD);
    }
}
