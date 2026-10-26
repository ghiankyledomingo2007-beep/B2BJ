import java.awt.image.BufferedImage;

public final class SagePresenceTest {
    public static void main(String[] args) throws Exception {
        for(int biome=0;biome<2;biome++) {
            var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
            if(biome==1)assert game.debugWarp(1,false);
            for(var enemy:game.scouts())enemy.hurt(10000);
            var hub=game.campaignArea().hub();game.player().relocate(hub.x(),hub.y());
            var silent=draw(game);assert pixels(silent,600,15,80,82)>350 : "Sage is present even without dialogue in region "+biome;
            double clock=game.sageTime();game.update(.4,0,0);assert game.sageTime()>clock;
            assert difference(silent,draw(game),600,15,80,82)>100 : "idle Sage moves gently";
            game.pause();clock=game.sageTime();game.update(.5,0,0);assert game.sageTime()==clock;
            game.togglePause();
            if(biome==0) {
                assert game.interact();assert pixels(draw(game),600,15,80,82)>350 : "NPC conversation must not hide Sage";
                clock=game.sageTime();game.update(.3,0,0);assert game.sageTime()>clock;
                game.skipDialogue();
            }
            game.guardian().activate(game.player().x(),game.player().y());
            assert pixels(draw(game),600,123,80,82)>350 : "idle Sage stays beneath boss bar";
        }
        var quiet=presence(1,0,false,false);var speech=presence(1,1,false,true);
        assert difference(quiet,speech,625,43,30,30)>8 : "voice energy drives the speaking core";
        assert difference(quiet,presence(1,1,false,false),600,15,80,82)==0 : "other speakers must not pulse Sage";
        assert difference(presence(1,0,true,false),presence(5,1,true,false),600,15,80,82)==0;
        assert difference(presence(1,0,true,true),presence(5,1,true,true),600,15,80,82)==0;
        var forest=RuinedOutpostGame.campaign();forest.begin();forest.skipDialogue();forest.debugWarp(1,false);
        var panel=new B2BJ(false,forest);panel.setSize(1280,720);
        for(int i=0;i<100;i++)panel.step(.05);
        assert forest.interact();
        var screen=new BufferedImage(1280,720,2);var graphics=screen.createGraphics();panel.paint(graphics);graphics.dispose();
        int gold=0;for(int y=20;y<95;y++)for(int x=606;x<674;x++) {
            int rgb=screen.getRGB(x,y);int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;
            if(r>110&&g>75&&r>b*1.3&&g>b*1.2)gold++;
        }
        assert gold>75 : "actual conversation screen must render Sage, not only the HUD helper";
        System.out.println("SagePresenceTest passed: persistent in both regions, dialogue, boss, idle, speech, pause and reduced effects");
    }
    static BufferedImage draw(RuinedOutpostGame game) {
        var image=new BufferedImage(1280,720,2);var g=image.createGraphics();CampaignRenderer.drawHud(g,game,1280,720,false);g.dispose();return image;
    }
    static BufferedImage presence(double time,double level,boolean reduced,boolean speaking) {
        var image=new BufferedImage(1280,720,2);var g=image.createGraphics();InnerVoiceOverlay.drawPresence(g,time,level,1280,reduced,speaking);g.dispose();return image;
    }
    static int pixels(BufferedImage image,int x,int y,int width,int height) {
        int n=0;for(int j=y;j<y+height;j++)for(int i=x;i<x+width;i++)if(image.getRGB(i,j)>>>24!=0)n++;return n;
    }
    static int difference(BufferedImage a,BufferedImage b,int x,int y,int width,int height) {
        int n=0;for(int j=y;j<y+height;j++)for(int i=x;i<x+width;i++)if(a.getRGB(i,j)!=b.getRGB(i,j))n++;return n;
    }
}
