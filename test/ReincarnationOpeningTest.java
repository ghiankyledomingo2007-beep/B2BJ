import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class ReincarnationOpeningTest {
    public static void main(String[] args) throws Exception {
        var naturalPath=Files.createTempDirectory("b2bj-opening-").resolve("save.properties");
        var skippedPath=Files.createTempDirectory("b2bj-opening-skip-").resolve("save.properties");
        var natural=RuinedOutpostGame.campaign(naturalPath);natural.begin();
        var skipped=RuinedOutpostGame.campaign(skippedPath);skipped.begin();skipped.skipDialogue();
        double x=natural.player().x(),health=natural.player().healthValue();
        natural.update(.25,1,1);double time=natural.openingLineTime();
        natural.pause();natural.update(.5,1,1);
        assert natural.openingLineTime()==time&&!natural.skipDialogue()&&!natural.advanceDialogue();
        natural.togglePause();natural.update(Double.NaN,0,0);assert natural.openingLineTime()==time;
        double total=0;
        for(int i=0;i<OutpostDialogue.WAKE.lines().size();i++) {
            double length=OutpostDialogue.openingSeconds(i);total+=length;
            assert ChapterOneVoicesTest.duration(OutpostDialogue.WAKE.voiceKey(i,false,0))+.2<length : "opening cuts off line "+i;
        }
        assert total>=60&&total<=75;
        for(int i=0;i<1500&&natural.opening();i++)natural.update(.05,1,1);
        assert !natural.opening()&&!natural.blocked()&&natural.dialogueSeen(OutpostDialogue.WAKE);
        assert natural.player().x()==x&&natural.player().healthValue()==health : "film freezes gameplay";
        assert CampaignSave.load(naturalPath).equals(CampaignSave.load(skippedPath)) : "skip and full playback have identical progress";
        var resumed=RuinedOutpostGame.campaign(naturalPath);assert resumed.continueCampaign()&&!resumed.opening();
        assert OutpostDialogue.Speaker.INNER_VOICE.label().equals("RAPHAEL");
        assert OutpostDialogue.Speaker.MARA.label().equals("SHUNA");
        assert java.util.Arrays.equals(FieldObjects.SURVIVORS,new String[]{"KAIJIN","EREN","RIGURD"});
        for(var scene:OutpostDialogue.values())for(var line:scene.lines())
            assert !line.text().matches(".*\\b(Mara|Tobin|Ilse|Venn|Sedge|Quartermaster|quartermaster)\\b.*") : line;
        awakeningMotion();cameraHandoff();
        SwingUtilities.invokeAndWait(()->{
            try { keyboardAndReducedEffects();controlsAndFrames(args.length==0?null:Path.of(args[0])); }
            catch(Exception e) {throw new AssertionError(e);}
        });
        System.out.println("ReincarnationOpeningTest passed: voice slots, automatic ending, skip equivalence, pause, input, viewport, labels and Continue");
    }

    private static void awakeningMotion() {
        var game=RuinedOutpostGame.campaign();game.begin();game.advanceDialogue();game.advanceDialogue();
        var ignition=openingFrame(game,false);
        for(int i=0;i<16;i++)game.update(.25,0,0);
        var formed=openingFrame(game,false);
        assert !java.util.Arrays.equals(ignition,formed) : "Raphael's awakening must visibly build over time";
        assert java.util.Arrays.equals(formed,openingFrame(game,false)) : "effects use deterministic scene time";
        game.pause();game.update(.5,0,0);
        assert java.util.Arrays.equals(formed,openingFrame(game,false)) : "pause freezes awakening particles and geometry";
        game.togglePause();game.advanceDialogue();
        var reduced=openingFrame(game,true);game.update(.3,0,0);
        assert java.util.Arrays.equals(reduced,openingFrame(game,true)) : "settled awakening respects reduced effects";
    }

    private static void cameraHandoff() {
        var game=RuinedOutpostGame.campaign();game.begin();
        while(game.dialogueIndex()<10)game.advanceDialogue();
        var frame=new BufferedImage(1920,1080,BufferedImage.TYPE_INT_RGB);var g=frame.createGraphics();
        ReincarnationOpening.frameWorld(g,game,1920,1080,0,0,false);
        var subject=g.getTransform().transform(new java.awt.geom.Point2D.Double(game.player().x(),game.player().y()),null);
        assert Math.abs(subject.getX()-960)<.001&&Math.abs(subject.getY()-550.8)<.001 : "wake shot centers on Rainoray";
        var frozen=g.getTransform();game.pause();game.update(.4,0,0);g.setTransform(new java.awt.geom.AffineTransform());
        ReincarnationOpening.frameWorld(g,game,1920,1080,0,0,false);
        assert frozen.equals(g.getTransform()) : "pause freezes the cinematic camera";
        g.setTransform(new java.awt.geom.AffineTransform());
        ReincarnationOpening.frameWorld(g,game,1920,1080,0,0,true);
        assert g.getTransform().isIdentity() : "reduced effects disable zoom and pan";
        game.togglePause();while(game.dialogueIndex()<14)game.advanceDialogue();
        double end=OutpostDialogue.openingSeconds(14)*.7;
        for(double t=0;t<end;t+=.05)game.update(.05,0,0);
        ReincarnationOpening.frameWorld(g,game,1920,1080,0,0,false);
        assert g.getTransform().isIdentity()&&ReincarnationOpening.worldReveal(game)==1 : "camera returns before gameplay";
        g.dispose();
    }

    private static void keyboardAndReducedEffects() {
        var game=RuinedOutpostGame.campaign();game.begin();
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();panel.step(.1);
        double before=game.openingLineTime();
        key(panel,"pause");panel.step(.5);
        assert game.paused()&&game.openingLineTime()==before : "Escape freezes the opening";
        key(panel,"skipDialogue");assert game.opening() : "skip must not leak through pause";
        key(panel,"pause");assert !game.paused();
        key(panel,"begin");assert game.dialogueIndex()==1;
        while(game.dialogueIndex()<7)game.advanceDialogue();
        var still=openingFrame(game,true);var moving=openingFrame(game,false);
        game.update(.35,0,0);
        assert java.util.Arrays.equals(still,openingFrame(game,true)) : "reduced effects stop seal, motes and idle motion";
        assert !java.util.Arrays.equals(moving,openingFrame(game,false)) : "normal reconstruction stays animated";
        key(panel,"skipDialogue");assert !game.opening();
    }

    private static int[] openingFrame(RuinedOutpostGame game,boolean reduced) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        ReincarnationOpening.draw(g,game,1280,720,B2BJ.loadImage("assets/characters/slime/slime_idle.png"),
                B2BJ.loadImage("assets/characters/blade/rainoray_idle.png"),reduced);
        g.dispose();return image.getRGB(0,0,1280,720,null,0,1280);
    }

    private static void key(B2BJ panel,String name) {
        for(String state:new String[]{"Pressed","Released"})panel.getActionMap().get(name+state)
                .actionPerformed(new java.awt.event.ActionEvent(panel,0,name));
    }

    private static void controlsAndFrames(Path output) throws Exception {
        if(output!=null)Files.createDirectories(output);
        for(int[] size:new int[][]{{1280,720},{1920,1080},{2560,1080}}) {
            var game=RuinedOutpostGame.campaign();game.begin();
            var panel=new B2BJ(false,game);panel.setSize(size[0],size[1]);panel.doLayout();
            panel.step(.1);assert !GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC) : "memory starts quietly";
            for(int beat=0;beat<OutpostDialogue.WAKE.lines().size();beat++) {
                if(beat>0)game.advanceDialogue();
                panel.step(.25);
                var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();
                if(output!=null&&size[0]==1920&&(beat==0||beat==5||beat==8||beat==11))
                    ImageIO.write(image,"png",output.resolve("opening-"+beat+".png").toFile());
                if(beat==5)assert GameAudio.loopRunning(GameAudio.Loop.STORY_MUSIC);
            }
            double scale=TitleScreen.menuScale(size[0],size[1]);
            int px=(int)((size[0]-1280*scale)/2+1190*scale),py=(int)((size[1]-720*scale)/2+44*scale);
            assert ReincarnationOpening.skipHit(px,py,size[0],size[1]);
            assert !ReincarnationOpening.skipHit(size[0]/2,size[1]/2,size[0],size[1]);
            panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,px,py,1,false,MouseEvent.BUTTON1));
            assert !game.opening()&&!game.blocked();
            panel.step(.01);
            var key=GameAudio.class.getDeclaredField("dialogueKey");key.setAccessible(true);
            assert key.get(null)==null : "skipping stops the spoken line";
        }
    }
}
