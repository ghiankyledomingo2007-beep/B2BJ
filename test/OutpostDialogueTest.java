import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.swing.SwingUtilities;

/** Real campaign interactions, temporary saves, and the same keyboard/mouse bindings used in play. */
public final class OutpostDialogueTest {
    public static void main(String[] args) throws Exception {
        storyAndPersistence();
        bossIntroduction();
        for(var scene:OutpostDialogue.values())for(var line:scene.lines()) {
            var lines=Ui.wrap(line.text(),1280-352,2);
            assert lines.size()<=3 : "dialogue would be clipped: "+line.text();
            assert lines.stream().allMatch(text->Ui.width(text,2)<=1280-352);
        }
        var art=B2BJ.loadImage("assets/characters/npcs/mara_sheet.png");
        assert art!=null&&art.getWidth()==art.getHeight()*2&&art.getColorModel().hasAlpha();
        for(String path:new String[]{"assets/characters/npcs/mara_idle.png","assets/characters/slime/rainoray_portrait.png"}) {
            var sprite=B2BJ.loadImage(path);
            assert sprite!=null&&sprite.getColorModel().hasAlpha();
            assert Math.abs(sprite.getWidth()-sprite.getHeight())<sprite.getHeight()*.1;
        }
        SwingUtilities.invokeAndWait(()->{
            try { inputAndRendering(args.length==0?null:Path.of(args[0])); }
            catch(Exception e) { throw new AssertionError(e); }
        });
        System.out.println("OutpostDialogueTest passed");
    }

    private static void storyAndPersistence() throws Exception {
        Path save=Files.createTempDirectory("b2bj-dialogue-").resolve("campaign.properties");
        var game=RuinedOutpostGame.campaign(save);game.begin();
        assert game.dialogue()==OutpostDialogue.WAKE&&game.blocked();
        game.player().collectIchor(100);game.player().transform();
        double x=game.player().x(),ichor=game.player().ichor(),enemyX=game.scouts().get(0).x();
        for(int i=0;i<30;i++)game.update(.5,1,0);
        assert game.player().x()==x&&game.player().ichor()==ichor&&game.scouts().get(0).x()==enemyX;
        assert !game.attack(1,0)&&!game.dash(1,0)&&!game.transform()&&!game.saveCheckpoint();
        game.pause();assert !game.advanceDialogue()&&!game.skipDialogue();
        game.togglePause();finish(game);
        assert game.dialogueSeen(OutpostDialogue.WAKE)&&!game.blocked();
        assert game.campaignObjective().equals("Find Mara at Captain Camp");
        var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
        assert game.primaryPress(1,0);
        assert game.interact()&&game.dialogue()==OutpostDialogue.CAMP&&!game.bladePreparing();
        finish(game);
        assert game.campaignObjective().contains("Warden");
        assert game.interact()&&game.dialogue()==OutpostDialogue.CAMP_WAIT;game.skipDialogue();
        var tower=game.campaignArea().landmarks().get(9);game.player().relocate(tower.x(),tower.y());
        int shards=game.shards();
        assert game.interact()&&game.dialogue()==OutpostDialogue.SIGNAL&&game.shards()==shards+1;
        game.skipDialogue();game.interact();game.skipDialogue();
        assert game.shards()==shards+1 : "re-reading cannot farm shards";
        for(int index=10;index<=11;index++) {
            var place=game.campaignArea().landmarks().get(index);game.player().relocate(place.x(),place.y());
            assert game.interact()&&game.dialogue()==(index==10?OutpostDialogue.INFIRMARY:OutpostDialogue.SUPPLIES);
            game.skipDialogue();
        }
        game.player().relocate(hub.x(),hub.y());
        assert game.interact()&&game.dialogue()==OutpostDialogue.REPORT;finish(game);
        assert game.saveCheckpoint();
        var restored=RuinedOutpostGame.campaign(save);assert restored.continueCampaign();
        assert restored.dialogue()==null&&restored.dialogueSeen(OutpostDialogue.REPORT);
        assert restored.shards()==shards+3&&restored.campaignCheckpoint()==1;
        assert restored.interact()&&restored.dialogue()==OutpostDialogue.CAMP_WAIT;
        restored.pause();assert restored.returnToTitle()&&restored.dialogue()==null;
        assert restored.continueCampaign()&&restored.dialogue()==null;

        // Existing version-one saves have no story flags and must keep their progression.
        var old=new CampaignSave.Progress(1,1,Set.of(0),Set.of("landmark_0_9"),java.util.Map.of("vitality",1),7);
        CampaignSave.save(save,old);
        var legacy=RuinedOutpostGame.campaign(save);assert legacy.continueCampaign();
        assert legacy.biome()==1&&legacy.shards()==7&&legacy.upgradeLevel("vitality")==1&&legacy.dialogue()==null;
    }

    private static void bossIntroduction() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        game.player().relocate(game.guardian().x()+300,game.guardian().y());
        game.update(.01,0,0);
        assert game.dialogue()==OutpostDialogue.WARDEN&&game.guardian().state()==Guardian.State.DORMANT;
        game.update(.5,0,0);
        assert game.guardian().state()==Guardian.State.DORMANT : "boss waits for the conversation";
        game.skipDialogue();game.update(.01,0,0);
        assert game.bossActive()&&game.dialogue()==null;
        assert game.debugWarp(0,false)&&game.dialogue()==null;
        game.player().relocate(game.guardian().x()+300,game.guardian().y());game.update(.01,0,0);
        assert game.dialogue()==null : "Warden introduction plays once";
    }

    private static void inputAndRendering(Path output) throws Exception {
        var game=RuinedOutpostGame.campaign();
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        press(panel,"begin");
        // The opening must not prevent the loading screen's clock from finishing.
        var tick=B2BJ.class.getDeclaredMethod("tick");tick.setAccessible(true);
        var clock=B2BJ.class.getDeclaredField("previousFrame");clock.setAccessible(true);
        for(int i=0;i<100;i++) {clock.setLong(panel,System.nanoTime()-50_000_000);tick.invoke(panel);}
        press(panel,"begin");assert game.dialogueIndex()==0 : "held title key cannot advance the opening";
        release(panel,"begin");
        assert game.dialogue()==OutpostDialogue.WAKE;
        press(panel,"begin");assert game.dialogueIndex()==1;
        press(panel,"begin");assert game.dialogueIndex()==1 : "held Enter advances only once";
        release(panel,"begin");
        press(panel,"right");
        click(panel);assert game.dialogueIndex()==2&&game.projectiles().isEmpty();
        press(panel,"skipDialogue");assert game.dialogue()==null;
        release(panel,"skipDialogue");
        double x=game.player().x();panel.step(.05);
        assert game.player().x()==x : "movement pressed in dialogue must not leak into combat";
        release(panel,"right");
        var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
        press(panel,"interact");assert game.dialogue()==OutpostDialogue.CAMP&&game.dialogueIndex()==0;
        panel.step(.05);press(panel,"interact");assert game.dialogueIndex()==0 : "held E cannot open and advance";
        release(panel,"interact");
        var rendered=paint(panel);
        assert panel.getAccessibleContext().getAccessibleDescription().contains("Easy.");
        if(output!=null) {Files.createDirectories(output);javax.imageio.ImageIO.write(rendered,"png",output.resolve("mara-conversation.png").toFile());}
        game.advanceDialogue();
        if(output!=null)javax.imageio.ImageIO.write(paint(panel),"png",output.resolve("slime-conversation.png").toFile());
        while(game.dialogueIndex()<game.dialogue().lines().size()-1)game.advanceDialogue();
        click(panel);assert game.dialogue()==null&&game.projectiles().isEmpty();
        panel.step(.1);assert game.projectiles().isEmpty() : "closing click must not fire";
        game.player().collectIchor(100);assert game.transform();
        game.interact();game.advanceDialogue();
        var human=paint(panel);
        if(output!=null)javax.imageio.ImageIO.write(human,"png",output.resolve("rainoray-conversation.png").toFile());
        game.skipDialogue();
        for(int i=0;i<120;i++)panel.step(.05);
        if(output!=null)javax.imageio.ImageIO.write(paint(panel),"png",output.resolve("camp-human-scale.png").toFile());
        game.player().relocate(game.guardian().x()+300,game.guardian().y());game.update(.01,0,0);
        assert game.dialogue()==OutpostDialogue.WARDEN;
        if(output!=null)javax.imageio.ImageIO.write(paint(panel),"png",output.resolve("warden-conversation.png").toFile());
        click(panel,1190,516);
        assert game.dialogue()==null&&game.projectiles().isEmpty() : "the quiet Skip target must skip, not attack";
    }

    private static void finish(RuinedOutpostGame game) {while(game.dialogue()!=null)assert game.advanceDialogue();}
    private static void press(B2BJ panel,String key) {panel.getActionMap().get(key+"Pressed").actionPerformed(new ActionEvent(panel,0,key));}
    private static void release(B2BJ panel,String key) {panel.getActionMap().get(key+"Released").actionPerformed(new ActionEvent(panel,0,key));}
    private static void click(B2BJ panel) {
        click(panel,600,600);
    }
    private static void click(B2BJ panel,int x,int y) {
        var click=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,x,y,1,false,MouseEvent.BUTTON1);
        for(var listener:panel.getMouseListeners())listener.mousePressed(click);
    }
    private static BufferedImage paint(B2BJ panel) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();return image;
    }
}
