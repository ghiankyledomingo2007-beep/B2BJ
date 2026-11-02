import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;

/** Opening-only, deterministic assembly; never changes the compact gameplay seal. */
final class OpeningSeal {
    private OpeningSeal() { }
    static void draw(Graphics2D canvas,double x,double y,double radius,double age,double time,double voice,boolean reduced) {
        var g=(Graphics2D)canvas.create();Ui.crisp(g);g.translate(x,y);
        double t=reduced?0:time,build=reduced?1:ease(age/3.8),pulse=reduced?0:Math.max(0,Math.min(1,voice));
        double breathe=reduced?1:1+.012*Math.sin(t*1.7),ignition=reduced?1:ease(age/.75);
        // Resonance: every 2.6s the core beats and a light front eases outward, lifting each layer it crosses.
        double phase=age/2.6-Math.floor(age/2.6),front=radius*(.2+.92*(1-(1-phase)*(1-phase)));
        double wave=reduced?0:ignition*Math.sin(Math.PI*phase)*(.7+.3*pulse),beat=reduced?0:ignition*Math.pow(Math.cos(Math.PI*phase),8);
        g.scale(breathe,breathe);
        glow(g,0,0,(int)(radius*1.45),new Color(249,183,57,(int)(16+build*15+pulse*12+wave*8)));
        // Tilted rings pass behind and in front of the plate on separate half-orbits.
        orbit(g,radius*1.16,t*.19,build,false);
        if(wave>0)for(int step=0,f=at(front);step<3;step++) {
            g.setStroke(new BasicStroke(20-step*8));ink(g,249+step*3,183+step*26,57+step*60,(int)(wave*(16+step*14)));g.drawOval(-f,-f,f*2,f*2);
        }
        for(int i=0;i<12;i++) {
            double lock=reduced?1:ease((age-.32-i*.075)/1.45);
            if(lock<=0)continue;
            double angle=i*Math.PI/6+t*.065+(1-lock)*.55;
            double r=radius+(1-lock)*38,flare=lift(r,front,radius,wave);
            var segment=(Graphics2D)g.create();segment.rotate(angle);
            int ri=(int)r;
            for(int step=0;step<2;step++) {ink(segment,249,183,57,(int)((14+58*flare)*lock));segment.setStroke(new BasicStroke(26-step*8));segment.drawArc(-ri,-ri,ri*2,ri*2,5,22);}
            ink(segment,38,28,15,(int)(210*lock));segment.setStroke(new BasicStroke(12));segment.drawArc(-ri,-ri,ri*2,ri*2,5,22);
            ink(segment,170+(int)(72*flare),117+(int)(84*flare),43+(int)(70*flare),(int)(220*lock));segment.setStroke(new BasicStroke(7));segment.drawArc(-ri,-ri,ri*2,ri*2,5,22);
            ink(segment,251,213+(int)(40*flare),115+(int)(105*flare),(int)(245*lock));segment.setStroke(new BasicStroke(2));segment.drawArc(-ri,-ri,ri*2,ri*2,5,22);
            segment.dispose();
        }
        for(int track=0;track<3;track++) {
            double show=reduced?1:ease((age-.7-track*.35)/1.4);
            int r=(int)(radius*(.86-track*.14));double flare=lift(r,front,radius,wave);
            g.setStroke(new BasicStroke(track==1?2:3));ink(g,225+(int)(30*flare),170+(int)(56*flare),72+(int)(96*flare),(int)((110+130*flare)*show));g.drawOval(-r,-r,r*2,r*2);
            double head=t*(track%2==0?42:-29)+track*110;
            for(int tail=9;tail>=0;tail--) {
                int start=(int)(head-tail*5);g.setStroke(new BasicStroke(tail==0?4:2));
                ink(g,255,226-tail*4,153-tail*8,(int)((1-tail/10.)*230*show));g.drawArc(-r,-r,r*2,r*2,start,5);
            }
        }
        double marks=reduced?1:ease((age-1.2)/1.5),ring=lift(radius*.93,front,radius,wave);
        for(int i=0;i<48;i++) {
            double a=i*Math.PI/24-t*.11;
            double scan=.5+.5*Math.cos(a+t*.9);
            int r=(int)(radius*.93),length=i%4==0?10:4;
            ink(g,255,214+(int)(30*ring),116+(int)(84*ring),(int)(marks*(60+140*scan+140*ring)));g.setStroke(new BasicStroke(i%4==0?3:2));
            g.drawLine(at(Math.cos(a)*r),at(Math.sin(a)*r),at(Math.cos(a)*(r-length)),at(Math.sin(a)*(r-length)));
        }
        double geometry=reduced?1:ease((age-1.45)/1.7);
        var geometryLayer=(Graphics2D)g.create();geometryLayer.rotate(-t*.12);
        for(int triangle=0;triangle<2;triangle++)for(int edge=0;edge<3;edge++) {
            double a=(edge*2+triangle)*Math.PI/3-Math.PI/2,b=a+2*Math.PI/3;
            double r=radius*.61;
            int x1=at(Math.cos(a)*r),y1=at(Math.sin(a)*r),x2=at(Math.cos(b)*r),y2=at(Math.sin(b)*r);
            double trace=Math.max(0,Math.min(1,geometry*3-edge));
            gline(geometryLayer,x1,y1,at(x1+(x2-x1)*trace),at(y1+(y2-y1)*trace),geometry*(1+lift(r*.75,front,radius,wave)));
            if(trace>0&&trace<1)glint(geometryLayer,at(x1+(x2-x1)*trace),at(y1+(y2-y1)*trace),3,1);
            double spark=geometry*lift(r,front,radius,wave);if(spark>.03)glint(geometryLayer,x1,y1,3,spark);
        }
        geometryLayer.dispose();
        if(!reduced)for(int i=0;i<52;i++) {
            double cycle=(t*.16+i*.61803398875)%1;
            double r=radius*(1.5-.9*cycle),a=i*2.39996+t*.16+cycle*.8;
            double strength=Math.sin(cycle*Math.PI)*build*(1+1.5*lift(r,front,radius,wave));
            int px=at(Math.cos(a)*r),py=at(Math.sin(a)*r*.83);
            ink(g,221,165,63,(int)(90*strength));g.fillRect(px-2,py-2,6,6);
            ink(g,255,231,165,(int)(220*strength));g.fillRect(px,py,2,2);
            if(i%9==0)glint(g,px,py,3,strength*.6);
        }
        double core=ignition*(1+pulse*.22);
        glow(g,0,0,(int)(54+pulse*18+beat*14),new Color(255,210,101,(int)(70*ignition+pulse*35+beat*26)));
        ink(g,247,190,79,(int)(70*ignition));g.fillOval(-28,-28,56,56);
        ink(g,255,226,137,(int)(245*ignition));diamond(g,0,0,(int)(12*core),(int)(30*core));
        ink(g,255,253,226,(int)(255*ignition));diamond(g,0,0,(int)(5*core),(int)(23*core));
        orbit(g,radius*1.16,t*.19,build,true);
        g.dispose();
    }
    private static void orbit(Graphics2D g,double radius,double angle,double opacity,boolean front) {
        var orbit=(Graphics2D)g.create();orbit.rotate(-.22+.09*Math.sin(angle));
        int r=(int)radius;orbit.setStroke(new BasicStroke(2));
        ink(orbit,240,193,93,(int)(opacity*(front?100:38)));orbit.drawArc(-r,-r/3,r*2,r*2/3,front?180:0,180);
        for(int i=0;i<3;i++) {
            double a=angle+i*Math.PI*2/3;
            if((Math.sin(a)>0)!=front)continue;
            int x=at(Math.cos(a)*r),y=at(Math.sin(a)*r/3.);
            glow(orbit,x,y,15,new Color(255,210,107,(int)(80*opacity)));glint(orbit,x,y,4,opacity);
        }
        orbit.dispose();
    }
    private static void gline(Graphics2D g,int x,int y,int x2,int y2,double opacity) {
        g.setStroke(new BasicStroke(7));ink(g,190,134,46,(int)(35*opacity));g.drawLine(x,y,x2,y2);
        g.setStroke(new BasicStroke(2));ink(g,255,217,122,(int)(190*opacity));g.drawLine(x,y,x2,y2);
    }
    private static void glint(Graphics2D g,int x,int y,int size,double opacity) {
        ink(g,255,226,148,(int)(150*opacity));g.fillRect(x-size*2,y-1,size*4+2,2);g.fillRect(x-1,y-size*2,2,size*4+2);
        ink(g,255,255,220,(int)(255*opacity));g.fillRect(x-1,y-1,2,2);
    }
    private static void glow(Graphics2D g,int x,int y,int r,Color c) {
        g.setPaint(new RadialGradientPaint(x,y,r,new float[]{0,.4f,1},new Color[]{c,new Color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/3),new Color(c.getRed(),c.getGreen(),c.getBlue(),0)}));
        g.fillOval(x-r,y-r,r*2,r*2);
    }
    private static void diamond(Graphics2D g,int x,int y,int w,int h) {g.fillPolygon(new int[]{x,x+w,x,x-w},new int[]{y-h,y,y+h,y},4);}
    private static void ink(Graphics2D g,int r,int green,int b,int a) {g.setColor(new Color(Math.min(255,r),Math.min(255,green),Math.min(255,b),Math.max(0,Math.min(255,a))));}
    private static double lift(double r,double front,double radius,double wave) {double d=(r-front)/(radius*.12);return wave*Math.exp(-d*d);}
    private static int at(double n) {return (int)Math.round(n/2)*2;}
    private static double ease(double n) {double p=Math.max(0,Math.min(1,n));return p*p*(3-2*p);}
}
