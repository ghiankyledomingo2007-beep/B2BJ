import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public final class ForestFeelTest {
    public static void main(String[] args) throws Exception {
        dashCadence();bodiesAndWindups();
        System.out.println("ForestFeelTest passed: dash spacing, invulnerability, pause, readable bodies, native pixels and organic spell preparation");
    }
    private static void dashCadence() {
        for(boolean blade:new boolean[]{false,true}) {
            var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(400,400),List.of(),new Guardian(1100,600));
            game.begin();var p=game.player();if(blade){p.collectIchor(100);p.transform();}
            assert game.dash(1,0);game.update(.1,0,0);assert p.invulnerable();
            for(int i=0;i<8;i++)game.update(.1,0,0);
            assert !game.dash(1,0)&&!p.invulnerable() : "dash cannot be chained or protect the whole cooldown";
            double cooldown=p.dashCooldown();game.pause();game.update(.4,0,0);
            assert p.dashCooldown()==cooldown;game.togglePause();
            for(int i=0;i<4;i++)game.update(.1,0,0);
            assert game.dash(-1,0) : "the dodge becomes available again after a meaningful gap";
        }
    }
    @SuppressWarnings("unchecked")
    private static void bodiesAndWindups() throws Exception {
        for(var kind:new CampaignEnemy.Kind[]{CampaignEnemy.Kind.RIFT_IMP,CampaignEnemy.Kind.THORN_WOLF,CampaignEnemy.Kind.MIRE_SHAMAN}) {
            var enemy=new CampaignEnemy(kind,240,220);var body=enemy.body();
            var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(350,220),List.of(body),new Guardian(1200,700));game.begin();
            var field=RuinedOutpostGame.class.getDeclaredField("campaignEnemies");field.setAccessible(true);
            ((Map<Wisp,CampaignEnemy>)field.get(game)).put(body,enemy);
            var panel=new B2BJ(false,game);panel.setSize(1280,720);
            var draw=B2BJ.class.getDeclaredMethod("drawWisp",java.awt.Graphics2D.class,Wisp.class,int.class,int.class);draw.setAccessible(true);
            var image=new BufferedImage(480,360,2);var g=image.createGraphics();Ui.crisp(g);draw.invoke(panel,g,body,0,0);g.dispose();
            int minX=480,maxX=0,minY=360,maxY=0;
            for(int y=0;y<360;y++)for(int x=0;x<480;x++)if(image.getRGB(x,y)>>>24!=0) {
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
            var old=B2BJ.loadImage("assets/characters/forest/"+kind.name().toLowerCase(java.util.Locale.ROOT)+"_walk.png");
            int oldMinX=48,oldMaxX=0,oldMinY=48,oldMaxY=0;
            for(int y=0;y<48;y++)for(int x=0;x<48;x++)if(old.getRGB(x,y)>>>24!=0) {
                oldMinX=Math.min(oldMinX,x);oldMaxX=Math.max(oldMaxX,x);oldMinY=Math.min(oldMinY,y);oldMaxY=Math.max(oldMaxY,y);
            }
            assert maxX-minX+1>(oldMaxX-oldMinX+1)*2 : kind+" must visibly grow";
            assert maxY-minY+1>(oldMaxY-oldMinY+1)*2;
            var bar=B2BJ.class.getDeclaredMethod("enemyBarY",Wisp.class);bar.setAccessible(true);
            int gap=minY-(int)bar.invoke(panel,body);
            assert gap>=8&&gap<=26 : "health bars follow the visible head, not the transparent cell padding";
            for(int y=0;y<360;y+=2)for(int x=0;x<480;x+=2) {
                int pixel=image.getRGB(x,y);
                assert pixel==image.getRGB(x+1,y)&&pixel==image.getRGB(x,y+1)&&pixel==image.getRGB(x+1,y+1)
                        : "native 2x2 pixels must stay intact";
            }
            assert enemy.damage()<20&&body.recoverDuration()>=1&&body.telegraphDuration()>=.8;
            var fx=new MobFx();fx.update(.01,game);
            for(int i=0;i<60;i++){enemy.update(.01,350,220,null);fx.update(.01,game);}
            if(kind==CampaignEnemy.Kind.MIRE_SHAMAN)for(boolean reduced:new boolean[]{false,true}) {
                var warning=new BufferedImage(480,360,2);var wg=warning.createGraphics();
                fx.drawGround(wg,0,0,reduced);fx.drawAbove(wg,0,0,reduced);wg.dispose();int pixels=0;
                for(int y=180;y<290;y++)for(int x=310;x<420;x++)if(warning.getRGB(x,y)>>>24!=0)pixels++;
                assert pixels>150 : "roots must be visible at the fixed target before damage, including Reduced Effects";
            }
        }
    }
}
