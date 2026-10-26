import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** Compact top-centre gold seal and subtitle; the battlefield stays visible. */
final class InnerVoiceOverlay {
    private InnerVoiceOverlay() { }

    static void draw(Graphics2D canvas,String text,double elapsed,double remaining,double level,
            int width,int height,boolean reduced) {
        draw(canvas,text,elapsed,remaining,level,width,height,reduced,elapsed);
    }

    static void draw(Graphics2D canvas,String text,double elapsed,double remaining,double level,
            int width,int height,boolean reduced,double clock) {
        if(elapsed<0||remaining<=0||!Double.isFinite(elapsed+remaining+level))return;
        float fade=(float)Math.max(0,Math.min(1,Math.min(elapsed/.24,remaining/.38)));
        drawPresence(canvas,clock,level,width,reduced,true);
        if(fade==0)return;
        var g=(Graphics2D)canvas.create();Ui.crisp(g);
        g.setComposite(AlphaComposite.SrcOver.derive(fade));
        int cx=width/2;
        Ui.fade(g,cx-240,134,240,60,0,192);
        Ui.fade(g,cx,134,240,60,192,0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        var label=Ui.heading(17);var body=Ui.heading(20);
        String speaker=OutpostDialogue.Speaker.INNER_VOICE.label();
        int labelHalf=g.getFontMetrics(label).stringWidth(speaker)/2;
        g.setStroke(new BasicStroke(1));gold(g,210);
        g.drawLine(cx-156,147,cx-labelHalf-14,147);
        g.drawLine(cx+labelHalf+14,147,cx+156,147);
        g.drawLine(cx-154,194,cx-8,194);g.drawLine(cx+8,194,cx+154,194);
        for(int x:new int[]{cx-156,cx+156})
            g.fillPolygon(new int[]{x-3,x,x+3,x},new int[]{147,144,147,150},4);
        g.drawPolygon(new int[]{cx-3,cx,cx+3,cx},new int[]{194,191,194,197},4);
        Ui.prose(g,speaker,cx-labelHalf,153,label,Ui.SHADOW);
        Ui.prose(g,speaker,cx-labelHalf,152,label,Ui.GOLD_LIGHT);
        int textLeft=cx-g.getFontMetrics(body).stringWidth(text)/2;
        Ui.prose(g,text,textLeft,179,body,Ui.SHADOW);
        Ui.prose(g,text,textLeft,178,body,Ui.TEXT);
        g.dispose();
    }

    static void drawPresence(Graphics2D canvas,double clock,double level,int width,boolean reduced,boolean speaking) {
        if(!Double.isFinite(clock+level)||clock<0)return;
        double time=reduced?0:clock,pulse=reduced||!speaking?0:Math.max(0,Math.min(1,level));
        int cx=width/2,cy=76;
        // The halo stays around the compact seal, clear of subtitles and the boss bar.
        var seal=(Graphics2D)canvas.create();Ui.crisp(seal);
        seal.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        seal.setComposite(AlphaComposite.SrcOver.derive((float)(speaking?1:.78+.05*Math.sin(time*1.6))));seal.translate(cx,cy);seal.scale(.85,.85);
        seal.setColor(new Color(12,15,19,110));seal.fillOval(-52,-52,104,104);
        seal.setPaint(new RadialGradientPaint(0,0,(float)(76+pulse*6),new float[]{0,.42f,.72f,1},new Color[]{
                new Color(255,219,123,65+(int)(pulse*50)),new Color(255,193,65,45+(int)(pulse*40)),
                new Color(255,173,42,20+(int)(pulse*20)),new Color(255,164,32,0)}));
        seal.fillOval(-82,-82,164,164);
        for(int pass=0;pass<3;pass++) {
            seal.setStroke(new BasicStroke(pass==0?12:pass==1?6:1.8f));
            gold(seal,pass==0?40+(int)(pulse*20):pass==1?85+(int)(pulse*35):240);
            seal.drawOval(-52,-52,104,104);seal.drawOval(-43,-43,86,86);
            for(int i=0;i<3;i++)seal.drawArc(-58,-58,116,116,(int)(time*13)+i*120,76);
        }
        seal.setStroke(new BasicStroke(1.3f));
        var base=seal.getTransform();
        seal.rotate(-time*.15);
        for(int i=0;i<32;i++) {
            var turn=seal.getTransform();seal.rotate(i*Math.PI/16);
            gold(seal,i%4==0?235:140);
            seal.drawLine(0,-54,0,i%4==0?-58:-56);
            if(i%2==0) {
                seal.drawLine(-1,-48,1,-46);seal.drawLine(1,-46,-1,-44);
                if(i%4==0)seal.drawLine(-2,-46,2,-46);
            }
            seal.setTransform(turn);
        }
        seal.setTransform(base);seal.rotate(time*.22);
        gold(seal,125);polygon(seal,6,36,0);
        gold(seal,185);polygon(seal,3,35,0);polygon(seal,3,35,Math.PI);
        seal.setTransform(base);
        gold(seal,120);seal.drawOval(-29,-29,58,58);
        gold(seal,200);seal.drawOval(-20,-20,40,40);
        for(int i=0;i<4;i++) {
            var turn=seal.getTransform();seal.rotate(i*Math.PI/2);
            gold(seal,180);seal.drawLine(0,-64,0,-60);
            seal.drawLine(-3,-61,0,-65);seal.drawLine(3,-61,0,-65);
            seal.setTransform(turn);
        }
        for(int i=0;i<16;i++) {
            var turn=seal.getTransform();seal.rotate(i*Math.PI/8);
            int reach=10+(int)Math.round(pulse*(4+3*Math.sin(i*1.7)));
            gold(seal,95+(int)(pulse*120));seal.drawLine(0,-reach,0,-18);
            seal.setTransform(turn);
        }
        int core=5+(int)Math.round(pulse*2);
        gold(seal,75+(int)(pulse*80));seal.fillOval(-15,-15,30,30);
        seal.setColor(new Color(255,244,192));
        seal.fillPolygon(new int[]{0,core,0,-core},new int[]{-10,0,10,0},4);
        seal.setColor(new Color(255,255,235));seal.fillRect(-1,-5,2,10);
        seal.dispose();

    }

    private static void gold(Graphics2D g,int alpha) { g.setColor(new Color(255,207,96,alpha)); }

    private static void polygon(Graphics2D g,int sides,int radius,double angle) {
        var path=new Path2D.Double();
        for(int i=0;i<sides;i++) {
            double a=angle+i*Math.PI*2/sides;
            int x=(int)Math.round(Math.sin(a)*radius),y=(int)Math.round(Math.cos(a)*radius);
            if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
        }
        path.closePath();g.draw(path);
    }
}
