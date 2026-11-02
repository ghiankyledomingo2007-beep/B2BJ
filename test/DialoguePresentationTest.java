import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class DialoguePresentationTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        for(int[] size:new int[][]{{1280,720},{1920,1080},{2560,1080}}) {
            speakerCamera(size[0],size[1]);floatingRaphael(size[0],size[1]);
        }
        reducedCamera();forestReveal();
        System.out.println("DialoguePresentationTest passed: speaker pans, framing, pause/skip, seal inside the frame, reduced effects and forest reveal");
    }
    private static RuinedOutpostGame scene(int biome) throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        if(biome==1)game.debugWarp(1,false);
        var boss=game.guardian();game.player().relocate(boss.x(),boss.y()+470);
        var open=RuinedOutpostGame.class.getDeclaredMethod("openDialogue",OutpostDialogue.class);open.setAccessible(true);
        open.invoke(game,biome==0?OutpostDialogue.WARDEN:OutpostDialogue.FOREST_BRIARHEART);
        return game;
    }
    private static B2BJ panel(RuinedOutpostGame game,int width,int height) {
        var panel=new B2BJ(false,game);panel.setSize(width,height);panel.doLayout();return panel;
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        while(seconds>1e-8){double dt=Math.min(.01,seconds);game.update(dt,0,0);seconds-=dt;}
    }
    private static void speakerCamera(int width,int height) throws Exception {
        var game=scene(0);var panel=panel(game,width,height);advance(game,1.4);
        int reveal=panel.worldCameraY();assert game.advanceDialogue();
        assert panel.worldCameraY()==reveal : "new speaker must not snap the camera";
        advance(game,.4);int middle=panel.worldCameraY();
        assert middle>reveal+50 : "camera must leave the Warden when Rainoray speaks";
        game.pause();advance(game,.5);assert panel.worldCameraY()==middle;game.togglePause();
        advance(game,.5);int reply=panel.worldCameraY();
        assert reply>middle&&game.player().y()-reply<height-218 : "Rainoray must be visible above his dialogue";
        assert game.advanceDialogue();advance(game,.25);
        int returning=panel.worldCameraY();assert returning<reply;
        assert game.skipDialogue()&&panel.worldCameraY()==returning : "skip continues from the current shot";
        advance(game,.6);assert !game.cutsceneActive();
        assert Math.abs(game.player().y()-panel.worldCameraY()-height/2./B2BJ.worldZoom(width,height))<=1 : "restore the exact gameplay camera";
    }
    private static void reducedCamera() throws Exception {
        var game=scene(0);var panel=panel(game,1280,720);
        var field=B2BJ.class.getDeclaredField("reducedEffects");field.setAccessible(true);field.setBoolean(panel,true);
        advance(game,1.4);int before=panel.worldCameraY();game.advanceDialogue();advance(game,1);
        assert panel.worldCameraY()==before : "reduced effects holds a stable conversation frame";
    }
    private static void forestReveal() throws Exception {
        var game=scene(1);var panel=panel(game,1280,720);advance(game,1.1);
        int boss=panel.worldCameraY();assert !game.dialogueReady();
        advance(game,.25);assert Math.abs(panel.worldCameraY()-boss)<=2 : "show the forest boss before moving to the first speaker";
        advance(game,.9);assert panel.worldCameraY()>boss+200;
    }
    private static void floatingRaphael(int width,int height) throws Exception {
        var game=scene(0);advance(game,1.4);for(int i=0;i<3;i++) {
            assert game.advanceDialogue();advance(game,5);
        }
        assert game.dialogueLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
        var first=draw(game,width,height,false);int top=height-188;
        // Owner 2026-10-28: first "out of the portrait box", then "it's out of the frame" -> inside the frame, no box.
        assert gold(first,142,top-138,140,138)<20 : "Raphael's seal must not float above the dialogue frame";
        assert gold(first,148,top+16,128,128)>1000 : "Raphael's seal sits in the frame's left slot";
        var duplicate=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=duplicate.createGraphics();
        CampaignRenderer.drawSage(g,game,width,height,false);g.dispose();
        assert Arrays.stream(duplicate.getRGB(0,0,width,height,null,0,width)).allMatch(v->v==0) : "one Raphael seal per conversation";
        advance(game,.4);assert !same(first,draw(game,width,height,false)) : "the seal stays animated";
        var reduced=draw(game,width,height,true);advance(game,.4);
        assert same(reduced,draw(game,width,height,true)) : "reduced effects freezes the seal";
        game.pause();var paused=draw(game,width,height,false);advance(game,.5);
        assert same(paused,draw(game,width,height,false)) : "pause freezes the seal";
        assert DialogueRenderer.skipHit(width-180,height-158,width,height) : "Skip remains in its original hit target";
    }
    private static BufferedImage draw(RuinedOutpostGame game,int width,int height,boolean reduced) {
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        DialogueRenderer.draw(g,game,width,height,reduced);g.dispose();return image;
    }
    private static boolean same(BufferedImage a,BufferedImage b) {
        return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));
    }
    private static int gold(BufferedImage image,int x,int y,int width,int height) {
        int count=0;
        for(int j=y;j<y+height;j++)for(int i=x;i<x+width;i++) {
            int c=image.getRGB(i,j),r=c>>16&255,g=c>>8&255,b=c&255;
            if(r>100&&g>65&&r>b*1.4&&g>b*1.2)count++;
        }
        return count;
    }
}
