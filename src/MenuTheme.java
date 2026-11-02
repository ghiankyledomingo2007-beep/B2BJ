import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Ivory Heraldry: quiet gold engraving, an arched crescent and cyan selection trim. */
final class MenuTheme {
    static final Color IVORY=new Color(0xf5e7c9), GOLD=new Color(0xc6a371), MUTED=new Color(0xb6b9b7);

    static void frame(Graphics2D graphics,int width,int height,double time,boolean reduced,boolean panel) {
        var g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int m=panel?9:20,right=width-m-1,bottom=height-m-1,top=panel?80:m;
        var shape=new Path2D.Double();shape.moveTo(m,top);
        if(panel){shape.lineTo(m+32,top);shape.quadTo(width/2.0,-49,right-32,top);}
        shape.lineTo(right,top);shape.lineTo(right,bottom);shape.lineTo(m,bottom);shape.closePath();
        if(panel){g.setPaint(new GradientPaint(0,top,new Color(0x0b1823),width,height,new Color(0x070e17)));g.fill(shape);}
        g.setStroke(new BasicStroke(panel?1.8f:1.2f));g.setColor(new Color(198,163,113,panel?230:160));g.draw(shape);
        if(panel) {
            var inner=new Path2D.Double();inner.moveTo(m+6,top+6);inner.lineTo(m+36,top+6);
            inner.quadTo(width/2.0,-34,right-36,top+6);inner.lineTo(right-6,top+6);inner.lineTo(right-6,bottom-6);inner.lineTo(m+6,bottom-6);inner.closePath();
            g.setStroke(new BasicStroke(.8f));g.setColor(new Color(198,163,113,120));g.draw(inner);
            int alpha=reduced?230:215+(int)(25*Math.sin(time*1.8));g.setColor(new Color(113,231,235,alpha));
            var moon=new Area(new Ellipse2D.Double(width/2.0-15,29,30,30));moon.subtract(new Area(new Ellipse2D.Double(width/2.0-6,24,29,29)));g.fill(moon);
            g.setColor(GOLD);star(g,width/2.0,15,5);star(g,width/2.0,70,3);
        }
        for(int x:new int[]{m,right})for(int y:new int[]{top,bottom}) {
            int dx=x==m?1:-1,dy=y==top?1:-1;
            var c=(Graphics2D)g.create();c.translate(x,y);c.scale(dx,dy);c.setColor(GOLD);c.setStroke(new BasicStroke(1));
            c.drawLine(5,5,22,5);c.drawLine(5,5,5,22);
            var curl=new Path2D.Double();curl.moveTo(5,18);curl.curveTo(18,18,20,8,11,8);curl.curveTo(7,8,8,13,12,12);c.draw(curl);
            star(c,3,3,2.5);c.dispose();
        }
        if(panel){g.setColor(GOLD);star(g,width/2.0,bottom-3,4);}
        g.dispose();
    }

    static void row(Graphics2D graphics,int width,int height,boolean hot,boolean down) {
        var g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int l=9,r=width-10,t=4,b=height-5,cy=height/2;
        var edge=new Path2D.Double();edge.moveTo(l+12,t);edge.lineTo(r-12,t);edge.quadTo(r,t,r,t+12);
        edge.lineTo(r,cy-7);edge.lineTo(r+6,cy);edge.lineTo(r,cy+7);edge.lineTo(r,b-12);edge.quadTo(r,b,r-12,b);
        edge.lineTo(l+12,b);edge.quadTo(l,b,l,b-12);edge.lineTo(l,cy+7);edge.lineTo(l-6,cy);edge.lineTo(l,cy-7);edge.lineTo(l,t+12);edge.quadTo(l,t,l+12,t);edge.closePath();
        g.setPaint(new GradientPaint(0,0,new Color(down?0x193640:hot?0x102c37:0x0b1823),width,0,new Color(0x08121b)));g.fill(edge);
        if(hot){g.setColor(new Color(105,229,237,25));g.setStroke(new BasicStroke(6));g.draw(edge);}
        g.setColor(hot?new Color(0x8be5e7):GOLD);g.setStroke(new BasicStroke(1.3f));g.draw(edge);
        g.setStroke(new BasicStroke(.7f));g.drawLine(l+14,t+4,r-14,t+4);g.drawLine(l+14,b-4,r-14,b-4);
        for(int x:new int[]{l,r})for(int y:new int[]{t,b}) {
            int dx=x==l?1:-1,dy=y==t?1:-1;
            g.drawLine(x,y,x+8*dx,y);g.drawLine(x,y,x,y+8*dy);g.drawLine(x,y+6*dy,x+6*dx,y);
        }
        star(g,l,cy,3);star(g,r,cy,3);g.dispose();
    }

    static void star(Graphics2D g,double x,double y,double radius) {
        var star=new Path2D.Double();
        for(int i=0;i<8;i++){double a=i*Math.PI/4-Math.PI/2,r=i%2==0?radius:radius*.30;
            double px=x+Math.cos(a)*r,py=y+Math.sin(a)*r;if(i==0)star.moveTo(px,py);else star.lineTo(px,py);}
        star.closePath();g.fill(star);
    }
}
