import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class DepthRenderingTest {
    public static void main(String[] args) throws Exception {
        bladeActionsShareNativeFootAnchor();
        waterBehindFenceIsOccluded();
        B2BJ panel=new B2BJ(false);
        panel.setSize(panel.getPreferredSize());
        panel.game().begin();
        var exit=panel.game().map().doors().get(0);
        panel.game().player().relocate(exit.x(),exit.y());
        assert panel.game().interact();
        var fence=panel.game().map().barriers().get(0);
        Player player=panel.game().player();
        double top=fence.centerY()-fence.height()/2;
        player.relocate(fence.centerX(),top-Player.COLLISION_Y_OFFSET-Player.COLLISION_RADIUS);
        assert !panel.game().map().isBlocked(player.x(),player.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS);
        BufferedImage behind=render(panel);
        // Camera centres the actor here. This point is inside both its feet and a fence post.
        int pixel=behind.getRGB(640,396)&0xffffff;
        assert !isCyan(pixel)
                : "fence must occlude the rear actor's feet, got "+Integer.toHexString(pixel);
        int rearFencePixel=pixel;
        player.relocate(fence.centerX(),fence.centerY()+fence.height()/2);
        BufferedImage front=render(panel);
        pixel=front.getRGB(640,350)&0xffffff;
        assert isCyan(pixel) : "front actor must occlude the fence";
        player.collectIchor(100);
        assert player.transform();
        player.relocate(fence.centerX(),top-Player.COLLISION_Y_OFFSET-Player.COLLISION_RADIUS);
        assert (render(panel).getRGB(640,396)&0xffffff)==rearFencePixel
                : "same fence must occlude Blade feet at the same ground position";
        player.relocate(fence.centerX(),fence.centerY()+fence.height()/2);
        int frontBlade=render(panel).getRGB(640,350)&0xffffff;
        var prop=fence.prop();
        // Scenery is value-shaded at load; compare against what the fence actually renders as.
        int frontFence=B2BJ.environmentShade(B2BJ.loadImage(prop.path())).getRGB(prop.anchorX,prop.anchorY-5)&0xffffff;
        assert frontBlade!=frontFence : "front Blade must occlude the fence";
        System.out.println("DepthRenderingTest passed");
    }
    private static void bladeActionsShareNativeFootAnchor() throws Exception {
        B2BJ panel=new B2BJ(false);
        panel.game().player().relocate(128,128);
        var field=B2BJ.class.getDeclaredField("bladeAnimation");field.setAccessible(true);
        BladeAnimation animation=(BladeAnimation)field.get(panel);
        var draw=B2BJ.class.getDeclaredMethod("drawBlade",Graphics2D.class,int.class,int.class);
        draw.setAccessible(true);
        for(int[] facing:new int[][]{{0,1},{1,0},{-1,0},{0,-1}})for(var action:BladeAnimation.Action.values()) {
            animation.face(facing[0],facing[1]);animation.play(action,.3);
            var actual=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=actual.createGraphics();draw.invoke(panel,g,0,0);g.dispose();
            var sheet=B2BJ.loadImage(animation.sheetPath());
            assert sheet!=null : "reviewed Rainoray asset required: "+animation.sheetPath();
            int row=animation.row();
            var expected=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
            g=expected.createGraphics();
            g.drawImage(sheet,animation.flipHorizontal()?208:48,32,animation.flipHorizontal()?48:208,192,
                    0,row*80,80,row*80+80,null);g.dispose();
            assert java.util.Arrays.equals(actual.getRGB(0,0,256,256,null,0,256),
                    expected.getRGB(0,0,256,256,null,0,256))
                    : "every action must share exact 2x pixels and foot row72";
        }
    }
    private static boolean isCyan(int pixel) {
        int r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
        return g-r>50&&b-r>50;
    }
    private static void waterBehindFenceIsOccluded() throws Exception {
        B2BJ panel=new B2BJ(false);panel.game().begin();
        var exit=panel.game().map().doors().get(0);
        panel.game().player().relocate(exit.x(),exit.y());assert panel.game().interact();
        var fence=panel.game().map().barriers().get(0);
        panel.game().player().relocate(fence.centerX()-55,fence.centerY()-fence.height()/2-60);
        assert panel.game().attack(1,0);panel.game().update(0.12,0,0);
        assert panel.game().projectiles().size()==1;
        var world=B2BJ.class.getDeclaredMethod("drawWorld",Graphics2D.class,int.class,int.class);world.setAccessible(true);
        var water=B2BJ.class.getDeclaredMethod("drawWater",Graphics2D.class,int.class,int.class,WaterProjectile.class);water.setAccessible(true);
        var prop=B2BJ.class.getDeclaredMethod("drawBarrier",Graphics2D.class,RuinedOutpostMap.Obstacle.class,int.class,int.class);prop.setAccessible(true);
        var withWater=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var withoutWater=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var waveMask=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var propMask=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=withWater.createGraphics();world.invoke(panel,g,0,0);g.dispose();
        g=waveMask.createGraphics();water.invoke(panel,g,0,0,panel.game().projectiles().get(0));g.dispose();
        g=propMask.createGraphics();prop.invoke(panel,g,fence,0,0);g.dispose();
        var field=RuinedOutpostGame.class.getDeclaredField("projectiles");field.setAccessible(true);
        ((java.util.List<?>)field.get(panel.game())).clear();
        g=withoutWater.createGraphics();world.invoke(panel,g,0,0);g.dispose();
        int overlap=0;
        for(int y=0;y<720;y++)for(int x=0;x<1280;x++)
            if((waveMask.getRGB(x,y)>>>24)>0&&(propMask.getRGB(x,y)>>>24)==255) {
                overlap++;
                assert withWater.getRGB(x,y)==withoutWater.getRGB(x,y) : "rear water must not paint over opaque fence";
            }
        assert overlap>20 : "fixture must overlap projectile art and fence art";
    }
    private static BufferedImage render(B2BJ panel) {
        BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();return image;
    }
}
