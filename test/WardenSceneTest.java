import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class WardenSceneTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var boss=game.guardian();
        game.player().relocate(boss.x()-300,boss.y()+120);
        assert game.map().clearLine(game.player().x(),game.player().y(),boss.x(),boss.y());
        var panel=new B2BJ(false,game);panel.setSize(1280,720);
        assert game.dash(1,0);
        game.update(.01,0,0);
        assert game.cutsceneActive()&&!game.dialogueReady()&&!game.advanceDialogue();
        assert !game.player().dashing() : "arrival dash must end at the cutscene";
        double x=game.player().x(),y=game.player().y(),ichor=game.player().ichor();
        assert !game.attack(1,0)&&!game.dash(1,0)&&!game.transform();
        game.pause();game.update(.5,1,1);assert game.storyCameraWeight()==0;game.togglePause();
        for(int i=0;i<28;i++)panel.step(.05);
        assert game.dialogueReady()&&game.storyCameraWeight()==1;
        assert game.player().x()==x&&game.player().y()==y&&game.player().ichor()==ichor;
        assert boss.state()==Guardian.State.DORMANT&&game.projectiles().isEmpty();
        int bx=(int)boss.x()-panel.worldCameraX(),feet=(int)boss.y()+Guardian.GROUND_Y_OFFSET-panel.worldCameraY();
        assert bx>160&&bx<1120&&feet>256&&feet<480 : "Warden must be visible above dialogue";
        while(game.dialogue()!=null)assert game.advanceDialogue();
        assert game.cutsceneActive()&&game.blocked()&&!game.attack(1,0);
        game.update(.3,1,1);assert game.storyCameraWeight()>0&&game.storyCameraWeight()<1;
        double held=game.storyCameraWeight();game.pause();game.update(.5,0,0);
        assert game.storyCameraWeight()==held;game.togglePause();
        game.update(.3,0,0);assert !game.cutsceneActive()&&!game.blocked();
        assert boss.state()==Guardian.State.DORMANT;
        game.update(.01,0,0);assert game.bossActive();
        assert game.dialogueSeen(OutpostDialogue.WARDEN);
        occludedApproachAndEarlySkip();
        gateOpensOnce();
        System.out.println("WardenSceneTest passed: reveal, pause, input freeze, return camera and gate opening");
    }

    private static void occludedApproachAndEarlySkip() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        var boss=game.guardian();boolean found=false;
        for(int dx=-480;dx<=480&&!found;dx+=60)for(int dy=-480;dy<=480&&!found;dy+=60) {
            double x=boss.x()+dx,y=boss.y()+dy;
            if(Math.hypot(dx,dy)>500||game.map().isBlocked(x,y+24,24)
                    ||game.map().clearLine(x,y,boss.x(),boss.y()))continue;
            game.player().relocate(x,y);game.update(.01,0,0);
            assert game.dialogue()==null&&!game.bossActive() : "Warden must not speak through a wall";
            found=true;
        }
        assert found : "test needs an occluded approach near the gatehouse";
        game.player().relocate(boss.x()-300,boss.y()+120);game.update(.01,0,0);game.update(.3,0,0);
        double weight=game.storyCameraWeight();assert weight>0&&weight<1;
        assert game.skipDialogue()&&game.storyCameraWeight()==weight : "skip returns from current camera position";
        game.update(.5,0,0);game.update(.1,0,0);
        assert !game.cutsceneActive()&&!game.blocked();
    }

    private static void gateOpensOnce() throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        assert game.gateOpening()==0;
        game.map().openGate();
        game.update(.5,0,0);assert game.gateOpening()==0 : "opening waits until the player can see it";
        game.player().relocate(game.campaignArea().exit().x()-180,game.campaignArea().exit().y()+100);
        game.update(.5,0,0);assert game.gateOpening()>0&&game.gateOpening()<1;
        double held=game.gateOpening();game.pause();game.update(.5,0,0);
        assert game.gateOpening()==held;game.togglePause();
        game.update(.5,0,0);game.update(.5,0,0);assert game.gateOpening()==1;
        var sheet=B2BJ.loadImage(SetPieces.Kind.EAST_GATE.sheet());
        int width=SetPieces.Kind.EAST_GATE.width;
        assert sheet.getWidth()==width*8;
        var frames=new HashSet<Integer>();
        for(int f=0;f<8;f++)frames.add(Arrays.hashCode(sheet.getRGB(f*width,0,width,sheet.getHeight(),null,0,width)));
        assert frames.size()==8 : "gate needs distinct opening poses";
        var save=Files.createTempDirectory("b2bj-gate").resolve("campaign.properties");
        CampaignSave.save(save,new CampaignSave.Progress(0,0,Set.of(0),Set.of(),Map.of(),0));
        var restored=RuinedOutpostGame.campaign(save);assert restored.continueCampaign();
        assert restored.gateOpening()==1 : "Continue keeps cleared gates open";
    }
}
