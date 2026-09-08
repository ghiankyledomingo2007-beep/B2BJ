import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Checks the actual renderer against reviewed native pixels, not just distinct silhouettes. */
public final class CampaignGuardArtTest {
    private static BufferedImage atlas;
    public static void main(String[] args) throws Exception {
        atlas = B2BJ.loadImage("assets/characters/campaign/outpost_guard.png");
        assert atlas != null && atlas.getWidth() == 1408 && atlas.getHeight() == 1760;
        for (int[] spec : new int[][]{{1,4,12},{5,4,8},{9,7,16},{16,4,8}})
            for (int row=spec[0]; row<spec[0]+spec[1]; row++) for (int frame=0; frame<spec[2]; frame++) {
                int count=0;
                for(int p:atlas.getRGB(frame*88,row*88,88,88,null,0,88)) if((p>>>24)>0)count++;
                assert count>500 && count<88*88*.8 : "empty or opaque guard frame";
            }
        for (int direction=0; direction<4; direction++) {
            int dx=direction==1?1:direction==3?-1:0, dy=direction==0?1:direction==2?-1:0;
            var enemy=new CampaignEnemy(CampaignEnemy.Kind.OUTPOST_GUARD,500,500);
            enemy.body().animation().update(dx,dy,0);
            assertPose(render(enemy),16+direction,0);
            until(enemy,Wisp.State.TELEGRAPH,500+dx*80,500+dy*80);
            int row=new int[]{9,10,12,14}[direction];
            assertPose(render(enemy),row,0);
            until(enemy,Wisp.State.LUNGE,500+dx*80,500+dy*80);
            assertPose(render(enemy),row,8);
            // Re-aim behind the guard after release: sprite and damage must stay committed.
            enemy.update(.08,500-dx*80,500-dy*80,null);
            assertPose(render(enemy),row,9);
            until(enemy,Wisp.State.RECOVER,500-dx*80,500-dy*80);
            assertPose(render(enemy),row,12);
            enemy.hurt(1,true,true);
            assertPose(render(enemy),5+direction,0);
        }
        corpseKeepsIdentity();
        System.out.println("CampaignGuardArtTest passed");
    }
    private static void corpseKeepsIdentity() throws Exception {
        var game=RuinedOutpostGame.campaign(); game.begin();
        Wisp body=game.scouts().stream().filter(w->w.role()==Wisp.Role.SHIELD_GUARD).findFirst().orElseThrow();
        body.animation().update(1,0,0); body.hurt(99);
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);
        hit.setAccessible(true); hit.invoke(game,body,true);
        var panel=new B2BJ(false,game);
        var draw=B2BJ.class.getDeclaredMethod("drawCorpses",java.awt.Graphics2D.class,int.class,int.class);
        draw.setAccessible(true);
        var first=corpseFrame(panel,draw,body);
        assertPose(first,2,0);
        var dead=render(game.campaignEnemy(body));
        assert Arrays.stream(dead.getRGB(0,0,320,320,null,0,320)).allMatch(p->p==0)
                : "corpse animation owns the dead guard; never draw two bodies";
        var age=RuinedOutpostGame.Corpse.class.getDeclaredField("age"); age.setAccessible(true);
        age.setDouble(game.corpses().get(0),.7);
        assertPose(corpseFrame(panel,draw,body),2,11);
        age.setDouble(game.corpses().get(0),2);
        assert Arrays.equals(corpseFrame(panel,draw,body).getRGB(0,0,320,320,null,0,320),
                corpseAt(panel,draw,body,age,game,3).getRGB(0,0,320,320,null,0,320))
                : "collapse must hold its last frame, never stand up again";
    }
    private static BufferedImage corpseAt(B2BJ panel,java.lang.reflect.Method draw,Wisp body,
            java.lang.reflect.Field age,RuinedOutpostGame game,double seconds) throws Exception {
        age.setDouble(game.corpses().get(0),seconds); return corpseFrame(panel,draw,body);
    }
    private static BufferedImage corpseFrame(B2BJ panel,java.lang.reflect.Method draw,Wisp body) throws Exception {
        var image=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        draw.invoke(panel,g,(int)body.x()-160,(int)body.y()-160);g.dispose();return image;
    }
    private static void until(CampaignEnemy enemy,Wisp.State state,double x,double y) {
        for(int i=0;i<1000&&enemy.body().state()!=state;i++)enemy.update(.01,x,y,null);
        assert enemy.body().state()==state;
    }
    private static BufferedImage render(CampaignEnemy enemy) {
        var image=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        CampaignRenderer.drawEnemy(g,enemy,(int)enemy.body().x()-160,(int)enemy.body().y()-160);
        g.dispose();return image;
    }
    private static void assertPose(BufferedImage image,int row,int frame) {
        int checked=0;
        for(int y=0;y<66;y++)for(int x=0;x<88;x++) {
            int pixel=atlas.getRGB(frame*88+x,row*88+y);
            if((pixel>>>24)!=255)continue;
            for(int sy=0;sy<2;sy++)for(int sx=0;sx<2;sx++)
                assert image.getRGB(72+x*2+sx,38+y*2+sy)==pixel
                        : "guard must render reviewed row "+row+" frame "+frame+" at native 2x / fixed feet";
            checked++;
        }
        assert checked>200;
    }
}
