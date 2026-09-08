import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class CampaignLegacyArtTest {
    public static void main(String[] args) throws Exception {
        var game=RuinedOutpostGame.campaign();game.begin();
        var panel=new B2BJ(false,game);
        for(var role:new Wisp.Role[]{Wisp.Role.SCOUT,Wisp.Role.SPITTER}) {
            String prefix=role==Wisp.Role.SPITTER?"spitter":"remnant";
            int walk=role==Wisp.Role.SPITTER?0xff10e03c:0xffd810c8;
            int attack=role==Wisp.Role.SPITTER?0xff09b4f2:0xfff6a708;
            replace(panel,prefix+"WalkSheet",sheet(192,144,walk));
            replace(panel,prefix+"AttackSheet",sheet(384,144,attack));
            var body=game.scouts().stream().filter(w->w.role()==role).findFirst().orElseThrow();
            game.player().relocate(body.x()-400,body.y()-200);
            assert count(world(panel,body.x(),body.y()),walk)>6500 : "campaign bypasses approved "+prefix+" walk art";
            for(int i=0;i<1000&&body.state()!=Wisp.State.TELEGRAPH;i++)body.update(.01,body.x()+200,body.y());
            assert body.state()==Wisp.State.TELEGRAPH;
            assert count(world(panel,body.x(),body.y()),attack)>6500 : "campaign bypasses approved "+prefix+" attack art";
        }
        int warden=0xff31ad79;
        replace(panel,"wardenMotionSheet",sheet(512,448,warden));
        game.player().relocate(game.guardian().x()-400,game.guardian().y()-200);
        assert count(world(panel,game.guardian().x(),game.guardian().y()),warden)>28000
                : "campaign bypasses reviewed Warden motion atlas";
        assert game.debugWarp(1,true);
        assert count(world(panel,game.guardian().x(),game.guardian().y()),warden)==0
                : "restoring Outpost art must not turn other biome bosses into the Warden";
        System.out.println("CampaignLegacyArtTest passed");
    }
    private static BufferedImage world(B2BJ panel,double x,double y) throws Exception {
        var draw=B2BJ.class.getDeclaredMethod("drawWorld",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var image=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        draw.invoke(panel,g,(int)x-320,(int)y-300);g.dispose();return image;
    }
    private static void replace(B2BJ panel,String name,BufferedImage image) throws Exception {
        var field=B2BJ.class.getDeclaredField(name);field.setAccessible(true);field.set(panel,image);
    }
    private static BufferedImage sheet(int width,int height,int color) {
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setColor(new Color(color,true));g.fillRect(0,0,width,height);g.dispose();return image;
    }
    private static long count(BufferedImage image,int color) {
        return java.util.Arrays.stream(image.getRGB(0,0,640,480,null,0,640)).filter(p->p==color).count();
    }
}
