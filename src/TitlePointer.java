import java.awt.Color;
import java.awt.Graphics2D;

/** A hollow ice chevron: one quiet marker shared by every title option. */
final class TitlePointer {
    static void draw(Graphics2D graphics,int baseline,double time,boolean reduced) {
        var g=(Graphics2D)graphics.create();
        int drift=reduced?0:(int)Math.round(Math.sin(time*1.8));
        g.translate(10+drift,baseline-10);g.scale(2,2);
        int pulse=reduced?20:20+(int)(Math.sin(time*2.2)*7);
        g.drawImage(OutpostLights.glow(12,0x45cee4,pulse),-5,-7,24,14,null);
        g.setColor(new Color(0x123849));
        g.fillPolygon(new int[]{1,7,11,7,1,4},new int[]{-6,-3,0,3,6,0},6);
        g.setColor(new Color(0x2b97b9));
        g.fillPolygon(new int[]{2,7,10,7,2,5},new int[]{-5,-2,0,2,5,0},6);
        g.setColor(new Color(0x7de9f2));
        g.fillPolygon(new int[]{2,7,10,7,5},new int[]{-5,-2,0,1,-1},5);
        g.setColor(new Color(0xdcffff));g.fillRect(8,-1,2,1);
        g.setColor(new Color(0x3c829d));g.fillRect(-2,-1,2,2);
        g.dispose();
    }
}
