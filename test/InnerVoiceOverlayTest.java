import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioSystem;

public final class InnerVoiceOverlayTest {
    public static void main(String[] args) throws Exception {
        sequence();interruptions();rendering();characterSubtitles();recordings();
        System.out.println("InnerVoiceOverlayTest passed: sequence, movement, pause, interruption, seal motion and packaged speech");
    }

    private static RuinedOutpostGame scene() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        for(var enemy:game.scouts())enemy.hurt(10000);
        var first=FieldObjects.find(0,"survivor_0");game.player().relocate(first.x(),first.y()-600);
        game.update(.01,0,0);assert game.bark()==OutpostBark.HEARD_SURVIVOR;
        return game;
    }

    private static void advance(RuinedOutpostGame game,double seconds) {
        for(int i=0;i<(int)Math.round(seconds/.01);i++)game.update(.01,0,0);
    }

    private static void sequence() {
        var game=scene();assert game.barkVoiceKey().equals("bark_heard_survivor");
        double x=game.player().x();game.update(.1,1,0);
        assert game.player().x()>x&&!game.blocked() : "conversation must not stop exploration";
        advance(game,3.6);
        assert game.barkLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
        assert game.barkVoiceKey().equals("bark_heard_survivor_2");
        double remaining=game.barkRemaining();game.pause();advance(game,10);
        assert game.barkRemaining()==remaining : "pause must freeze line and seal timing";
        game.togglePause();advance(game,3.6);
        assert game.barkLine().speaker()==OutpostDialogue.Speaker.RAINORAY;
        assert game.barkVoiceKey().equals("bark_heard_survivor_3");
        advance(game,20);assert game.bark()==null&&game.barkVoiceKey()==null : "exchange must finish without repeating";
    }

    private static void interruptions() {
        var game=scene();advance(game,3.7);
        game.guardian().activate(game.player().x(),game.player().y());game.update(.01,0,0);
        assert game.bark()==OutpostBark.HEARD_SURVIVOR : "combat does not cut off the exchange";
        game=scene();advance(game,3.7);var first=FieldObjects.find(0,"survivor_0");
        game.player().relocate(first.x(),first.y()+50);game.interact();
        assert game.dialogue()!=null&&game.barkLine()==null : "NPC dialogue replaces inner voice";
        game=scene();game.pause();game.returnToTitle();assert game.barkLine()==null;
    }

    private static BufferedImage render(double time,double level,boolean reduced) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        InnerVoiceOverlay.draw(g,"Confirmed. Someone is still alive.",time,1,level,1280,720,reduced);
        g.dispose();return image;
    }

    private static int difference(BufferedImage a,BufferedImage b) {
        int count=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)
            if(a.getRGB(x,y)!=b.getRGB(x,y))count++;
        return count;
    }

    private static void rendering() {
        var a=render(1,.2,false);
        assert difference(a,render(1.7,.2,false))>200 : "rune rings must rotate";
        assert difference(a.getSubimage(625,43,30,30),render(1,1,false).getSubimage(625,43,30,30))>8
                : "small seal core must respond to speech energy";
        assert difference(render(1,.2,true),render(1.7,1,true))==0 : "reduced effects keep a static seal";
        for(int y=250;y<425;y++)for(int x=450;x<850;x++)assert a.getRGB(x,y)==0 : "player view must stay clear";
        for(int y=0;y<160;y++)for(int x=0;x<340;x++)assert a.getRGB(x,y)==0 : "health stays clear";
        for(int y=500;y<720;y++)for(int x=1024;x<1280;x++)assert a.getRGB(x,y)==0 : "minimap stays clear";
        int occupied=0;
        for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if(a.getRGB(x,y)!=0) {
            occupied++;
            assert x>=400&&x<880&&y<200 : "entire overlay stays in the compact top-centre region";
        }
        assert occupied<48_000 : "overlay must leave the playfield visible";
    }

    private static void recordings() throws Exception {
        var bark=OutpostBark.HEARD_SURVIVOR;
        for(int i=0;i<bark.lines().size();i++)try(var source=Ui.open("assets/audio/dialogue/"+bark.voiceKey(i)+".wav")) {
            assert source!=null : "runtime voice must be packaged";
            try(var wav=AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(source))) {
                double seconds=wav.getFrameLength()/wav.getFormat().getFrameRate();
                assert seconds>1&&seconds<bark.lineSeconds() : "subtitle must outlast its recording";
            }
        }
    }

    private static void characterSubtitles() {
        for(var bark:OutpostBark.values())for(var line:bark.lines()) {
            if(line.speaker()==OutpostDialogue.Speaker.INNER_VOICE)continue;
            var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            assert g.getFontMetrics(Ui.heading(21)).stringWidth(line.text())<500 : "exploration subtitle must fit";
            DialogueRenderer.drawExploration(g,line,1,1,1280,720);g.dispose();
            int occupied=0;
            for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if(image.getRGB(x,y)!=0) {
                occupied++;
                assert x>=360&&x<920&&y>=510&&y<580 : "subtitles must avoid the player, map and abilities";
            }
            assert occupied>1000&&occupied<24_000 : "subtitle backing is too large: "+occupied;
        }
    }
}
