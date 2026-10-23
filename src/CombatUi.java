import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.WeakHashMap;

/** Small world-space cues; they describe aim and damage without changing either. */
final class CombatUi {
    private static final Color BLOOD=new Color(0xc34c50), LIGHT=new Color(0xed8b77), TRACK=new Color(0x352c3c);
    private static final Cursor CURSOR=makeCursor();
    private final WeakHashMap<Wisp,Health> health=new WeakHashMap<>();
    private static final class Health {double value=1,trail=1,hitAt,last;}

    static Cursor cursor() {return CURSOR;}
    private static Cursor makeCursor() {
        if(GraphicsEnvironment.isHeadless())return Cursor.getDefaultCursor();
        var image=new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        g.setColor(Ui.INK);g.fillPolygon(new int[]{12,19,12,5},new int[]{3,12,21,12},4);
        g.setColor(Ui.TEXT);g.drawPolygon(new int[]{12,17,12,7},new int[]{6,12,18,12},4);
        g.fillRect(11,11,2,2);g.dispose();
        return Toolkit.getDefaultToolkit().createCustomCursor(image,new Point(12,12),"Aim");
    }

    void health(Graphics2D g,Wisp enemy,int x,int y,boolean targeted,double time,boolean reduced) {
        if(!enemy.alive()||!enemy.visible()||(!targeted&&!enemy.aggro()&&enemy.health()==enemy.maxHealth()))return;
        var state=health.computeIfAbsent(enemy,key->new Health());
        double value=Math.max(0,Math.min(1,enemy.health()/(double)enemy.maxHealth()));
        if(value<state.value)state.hitAt=time;
        if(reduced||value>state.trail)state.trail=value;
        else if(time-state.hitAt>.28)state.trail=Math.max(value,state.trail-Math.max(0,time-state.last)*1.2);
        state.value=value;state.last=time;
        int width=targeted?64:52,left=x-width/2;
        g.setColor(Ui.INK);g.fillPolygon(new int[]{left-4,left,left+width,left+width+4,left+width,left},
                new int[]{y+4,y-2,y-2,y+4,y+10,y+10},6);
        g.setColor(TRACK);g.fillRect(left,y,width,8);
        g.setColor(Ui.GOLD_LIGHT);g.fillRect(left,y,(int)(width*state.trail),8);
        int fill=(int)(width*value);
        g.setColor(BLOOD);g.fillRect(left,y,fill,8);
        g.setColor(LIGHT);g.fillRect(left,y,fill,2);
        if(targeted) {
            g.setColor(Ui.INK);g.fillPolygon(new int[]{x-9,x+9,x},new int[]{y-21,y-21,y-11},3);
            g.setColor(Ui.GOLD_LIGHT);g.fillPolygon(new int[]{x-6,x+6,x},new int[]{y-19,y-19,y-13},3);
        }
    }

    static void direction(Graphics2D g,int x,int y,int dx,int dy) {
        double length=Math.hypot(dx,dy);
        if(length==0)return;
        double nx=dx/length,ny=dy/length;
        int cx=(int)Math.round((x+nx*62)/2)*2,cy=(int)Math.round((y+ny*62)/2)*2;
        int[] xs=new int[4],ys=new int[4];
        double[] forward={7,-5,-1,-5},side={0,5,0,-5};
        for(int i=0;i<4;i++) {
            xs[i]=(int)Math.round((cx+nx*forward[i]-ny*side[i])/2)*2;
            ys[i]=(int)Math.round((cy+ny*forward[i]+nx*side[i])/2)*2;
        }
        g.setColor(Ui.INK);g.setStroke(new java.awt.BasicStroke(2));g.drawPolygon(xs,ys,4);
        g.setColor(new Color(233,221,174,220));g.fillPolygon(xs,ys,4);
    }
}
