import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class RemnantFacingTest {
    public static void main(String[] args)throws Exception {
        var panel=new B2BJ(false);
        var draw=B2BJ.class.getDeclaredMethod("drawWisp",Graphics2D.class,Wisp.class,int.class,int.class);draw.setAccessible(true);
        for(var role:new Wisp.Role[]{Wisp.Role.SCOUT,Wisp.Role.SPITTER})for(int direction:new int[]{-1,1}) {
            var enemy=new Wisp(200,200,200,200,3,role);enemy.animation().update(direction,0,0);
            var image=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            draw.invoke(panel,g,enemy,0,0);g.dispose();
            var sheet=B2BJ.loadImage("assets/characters/wisp/"+(role==Wisp.Role.SPITTER?"spitter":"remnant")+"_walk.png");
            // Both reviewed sheets contain canonical WEST sprites; east alone must mirror them.
            for(int y=0;y<48;y++)for(int x=0;x<48;x++) {
                int expected=sheet.getRGB(direction>0?47-x:x,48+y);
                int actual=image.getRGB(152+x*2,152+y*2);
                assert (expected>>>24)==0?(actual>>>24)==0:expected==actual
                        : role+" faces opposite to movement/shot direction "+direction;
            }
        }
        System.out.println("RemnantFacingTest passed");
    }
}
