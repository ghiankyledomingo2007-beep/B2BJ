import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Journey Ribbon's screen-space layout and the shared Ivory Heraldry drawing primitives. */
final class JourneyHud {
    static final Color INK=new Color(5,15,23,242), IVORY=MenuTheme.IVORY, GOLD=new Color(0xe1ba75);
    static final Font LABEL=Ui.menuFont(18).deriveFont(Font.BOLD), SMALL=Ui.menuFont(15).deriveFont(Font.BOLD), BODY=Ui.heading(16);
    private static final java.awt.image.BufferedImage APPROVED=B2BJ.loadImage("assets/ui/journey-ribbon-approved.png");
    static final java.awt.image.BufferedImage SLASH=skillArt(590), WAVE=skillArt(728), DASH=skillArt(867), FORM=skillArt(1006);

    private static java.awt.image.BufferedImage skillArt(int x) {
        return APPROVED==null?null:APPROVED.getSubimage(x,810,78,78);
    }

    // Match the approved board in a 1600 x 900 logical HUD; widescreen adds space between anchored groups.
    static double scale(int width,int height) {return Math.min(width/1600.,height/900.);}
    static Rectangle ribbon(int width,int height) {return new Rectangle(width/2-300,height-204,600,72);}
    static Rectangle skill(int width,int height,boolean blade,int index) {
        int size=blade?80:92,gap=blade?20:40,count=blade?6:4,total=count*size+(count-1)*gap;
        return new Rectangle(width/2-total/2+index*(size+gap),height-132,size,size+24);
    }

    static void text(Graphics2D g,String text,int x,int baseline,Font font,Color color) {
        Ui.prose(g,text,x,baseline,font,color);
    }
    static void center(Graphics2D g,String text,int x,int baseline,Font font,Color color) {
        text(g,text,x-g.getFontMetrics(font).stringWidth(text)/2,baseline,font,color);
    }
    static void right(Graphics2D g,String text,int x,int baseline,Font font,Color color) {
        text(g,text,x-g.getFontMetrics(font).stringWidth(text),baseline,font,color);
    }

    static void frame(Graphics2D graphics,int x,int y,int width,int height) {
        var g=(Graphics2D)graphics.create();g.translate(x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int cap=Math.min(10,height/4);
        var edge=new Path2D.Double();edge.moveTo(cap,0);edge.lineTo(width-cap,0);edge.lineTo(width,cap);
        edge.lineTo(width,height-cap);edge.lineTo(width-cap,height);edge.lineTo(cap,height);edge.lineTo(0,height-cap);edge.lineTo(0,cap);edge.closePath();
        g.setColor(INK);g.fill(edge);g.setColor(GOLD);g.setStroke(new BasicStroke(1.25f));g.draw(edge);
        if(height>=40)for(int cx:new int[]{0,width})for(int cy:new int[]{0,height}) {
            int dx=cx==0?1:-1,dy=cy==0?1:-1;
            corner(g,cx,cy,dx,dy);
        }
        if(height>=40){
            g.setColor(new Color(225,186,117,160));g.drawLine(18,4,width-18,4);g.drawLine(18,height-4,width-18,height-4);
            g.setColor(GOLD);MenuTheme.star(g,width/2.,0,5);MenuTheme.star(g,width/2.,height,5);
            if(width>height*3){diamond(g,0,height/2.,5);diamond(g,width,height/2.,5);}
        }g.dispose();
    }

    private static void corner(Graphics2D graphics,int x,int y,int dx,int dy) {
        var g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(dx,dy);
        g.setStroke(new BasicStroke(1.1f));g.setColor(GOLD);
        g.drawLine(1,20,1,1);g.drawLine(1,1,20,1);g.drawLine(5,16,5,5);g.drawLine(5,5,16,5);
        var engraving=new Path2D.Double();engraving.moveTo(2,17);engraving.lineTo(6,13);engraving.lineTo(10,13);
        engraving.lineTo(14,5);engraving.lineTo(18,2);g.draw(engraving);g.drawLine(5,10,10,5);
        MenuTheme.star(g,4,4,3);g.dispose();
    }

    static void skillFrame(Graphics2D graphics,int x,int y,int size) {
        var g=(Graphics2D)graphics.create();g.translate(x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(INK);g.fillRect(0,0,size,size);
        g.setColor(GOLD);g.setStroke(new BasicStroke(1.25f));g.drawRect(0,0,size,size);
        g.setColor(new Color(225,186,117,150));g.setStroke(new BasicStroke(.8f));g.drawRect(4,4,size-8,size-8);
        for(int cx:new int[]{0,size})for(int cy:new int[]{0,size})corner(g,cx,cy,cx==0?1:-1,cy==0?1:-1);
        g.dispose();
    }

    static void diamond(Graphics2D g,double x,double y,double radius) {
        var d=new Path2D.Double();d.moveTo(x,y-radius);d.lineTo(x+radius,y);d.lineTo(x,y+radius);d.lineTo(x-radius,y);d.closePath();
        g.setColor(INK);g.fill(d);g.setColor(GOLD);g.draw(d);
        double r=radius*.55;g.setColor(IVORY);g.fillPolygon(new int[]{(int)x,(int)(x+r),(int)x,(int)(x-r)},new int[]{(int)(y-r),(int)y,(int)(y+r),(int)y},4);
    }

    static void meter(Graphics2D graphics,int x,int y,int width,int height,double fraction,Color fill,Color light) {
        var g=(Graphics2D)graphics.create();g.translate(x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        double cap=height/2.;var edge=new Path2D.Double();edge.moveTo(cap,0);edge.lineTo(width-cap,0);edge.lineTo(width,cap);
        edge.lineTo(width-cap,height);edge.lineTo(cap,height);edge.lineTo(0,cap);edge.closePath();
        g.setColor(new Color(0x070e17));g.fill(edge);
        var inside=(Graphics2D)g.create();inside.clip(edge);
        inside.setPaint(new GradientPaint(0,2,light,0,height-2,fill));
        int amount=(int)Math.round((width-4)*Math.max(0,Math.min(1,fraction)));
        if(amount>0)inside.fillRect(2,2,amount,height-4);inside.dispose();
        g.setColor(GOLD);g.setStroke(new BasicStroke(1.2f));g.draw(edge);
        if(height>=12){diamond(g,cap,cap,cap-1);diamond(g,width-cap,cap,cap-1);}g.dispose();
    }

    static void heart(Graphics2D graphics,int x,int y,Color color) {
        var g=(Graphics2D)graphics.create();g.translate(x,y);g.setColor(color);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        var h=new Path2D.Double();h.moveTo(9,17);h.curveTo(-10,5,2,-5,9,3);h.curveTo(16,-5,28,5,9,17);g.fill(h);g.dispose();
    }

    static void crest(Graphics2D graphics,boolean blade) {
        var g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(INK);g.fillRect(55,39,180,32);g.setColor(GOLD);g.setStroke(new BasicStroke(1.25f));g.drawLine(55,39,235,39);g.drawLine(55,71,235,71);
        MenuTheme.star(g,235,55,7);
        g.setColor(INK);g.fillOval(20,24,70,70);g.setColor(GOLD);g.setStroke(new BasicStroke(1.5f));g.drawOval(20,24,70,70);g.drawOval(24,28,62,62);
        for(int[] point:new int[][]{{55,24},{55,94},{20,59},{90,59}})MenuTheme.star(g,point[0],point[1],5);
        g.translate(0,6);
        var slime=new Path2D.Double();slime.moveTo(32,62);slime.curveTo(30,50,37,42,45,40);
        slime.curveTo(47,37,44,35,49,34);slime.curveTo(52,33,52,30,52,29);slime.curveTo(59,30,62,35,61,40);
        slime.curveTo(73,40,82,49,80,62);slime.curveTo(77,75,39,76,32,62);g.setColor(IVORY);g.fill(slime);
        g.setColor(INK);g.fillOval(46,51,4,6);g.fillOval(64,51,4,6);g.drawArc(53,55,8,6,190,160);
        text(g,"Rainoray",103,56,LABEL,IVORY);
        if(blade)text(g,"Human form",103,82,SMALL,MenuTheme.MUTED);g.dispose();
    }

    static void location(Graphics2D graphics,String label,int x,int baseline,Color color) {
        var g=(Graphics2D)graphics.create();int half=g.getFontMetrics(LABEL).stringWidth(label)/2;
        Ui.fade(g,x-half-20,baseline-22,half+20,30,0,140);Ui.fade(g,x,baseline-22,half+20,30,140,0);
        center(g,label,x,baseline,LABEL,color);g.setColor(GOLD);g.setStroke(new BasicStroke(1));
        g.drawLine(x-half,baseline+8,x-9,baseline+8);g.drawLine(x+9,baseline+8,x+half,baseline+8);MenuTheme.star(g,x,baseline+8,3);g.dispose();
    }

    static List<String> wrap(Graphics2D g,String words,Font font,int width) {
        var lines=new ArrayList<String>();String line="";
        for(String word:words.split("\\s+")) {
            String next=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&g.getFontMetrics(font).stringWidth(next)>width){lines.add(line);line=word;}
            else line=next;
        }
        if(!line.isEmpty())lines.add(line);return lines;
    }
}
