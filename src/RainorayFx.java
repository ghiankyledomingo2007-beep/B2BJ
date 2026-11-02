import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** Native-pixel effects lifted from the approved skill audition, driven by live combat events. */
public final class RainorayFx {
    private static final Color VOID=new Color(12,8,28),VIOLET=new Color(113,65,189),LIGHT=new Color(196,155,250),WHITE=new Color(232,255,248);
    private static final class Mark {
        final double x,y,force;final boolean gold;double age;
        Mark(double x,double y,double force,boolean gold){this.x=x;this.y=y;this.force=force;this.gold=gold;}
    }
    public static final BufferedImage ICON=icon();
    private static BufferedImage icon() {
        var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();Ui.crisp(g);
        g.setColor(VOID);g.fillOval(7,7,50,50);g.rotate(-Math.PI/4,32,32);
        rift(g,8,32,56,32,.35);g.setColor(WHITE);g.fillRect(31,8,3,38);g.setColor(LIGHT);g.fillRect(23,42,19,3);
        g.setColor(VIOLET);g.fillRect(31,45,3,11);g.dispose();return image;
    }
    private final ArrayList<Mark> marks=new ArrayList<>();
    double impactAge=1,impactX,impactY;boolean impactGold;
    private BufferedImage layer;
    public void clear(){marks.clear();impactAge=1;}
    public void update(double dt){impactAge+=dt;for(var m:marks)m.age+=dt;marks.removeIf(m->m.age>.42);}
    public void hit(double x,double y,double force,boolean gold,boolean impact) {
        if(marks.size()==12)marks.remove(0);marks.add(new Mark(x,y,force,gold));
        if(impact){impactAge=0;impactX=x;impactY=y;impactGold=gold;}
    }
    private static double clamp(double x){return Math.max(0,Math.min(1,x));}
    private static int px(double world,int camera){return (int)Math.round((world-camera)/2);}
    public void draw(Graphics2D canvas,RuinedOutpostGame game,int cx,int cy,int width,int height,boolean reduced) {
        int w=(width+1)/2,h=(height+1)/2;
        if(layer==null||layer.getWidth()!=w||layer.getHeight()!=h)layer=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var g=layer.createGraphics();g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,w,h);g.setComposite(AlphaComposite.SrcOver);Ui.crisp(g);
        var p=game.player();double length=Math.max(1,Math.hypot(game.bladeAimX(),game.bladeAimY()));
        int hx=px(p.x()+32*game.bladeAimX()/length,cx),hy=px(p.y()-20+24*game.bladeAimY()/length,cy);
        if(p.bladeForm()&&game.devoured())gather(g,hx,hy,game.chargeSeconds(),game.bladePreparing()?.6+.4*game.chargeProgress():.35,reduced);
        if(game.guarding()) {g.setColor(new Color(242,213,137));g.drawArc(hx-4,hy-14,12,30,-70,140);}
        if(game.voidActive()) {
            double t=game.voidElapsed();
            if(t<.48)gather(g,hx,hy,t,clamp(t/.48),reduced);
            else {
                int x=px(game.voidStartX(),cx),y=px(game.voidStartY()-20,cy),ex=px(game.voidEndX(),cx),ey=px(game.voidEndY()-20,cy);
                double distance=Math.hypot(ex-x,ey-y);
                var lane=(Graphics2D)g.create();lane.translate(x,y);lane.rotate(Math.atan2(ey-y,ex-x));
                if(t<.8)cut(lane,0,0,(int)distance,0,.7,true);
                else rift(lane,0,0,(int)distance,0,clamp((t-.8)/.42));
                lane.dispose();
            }
        }
        for(var m:marks)burst(g,px(m.x,cx),px(m.y-20,cy),m.age,m.force,m.gold,reduced);
        g.dispose();canvas.drawImage(layer,0,0,w*2,h*2,null);
    }
    private static void rift(Graphics2D g,int x,int y,int ex,int ey,double p) {
        int n=16;int[] xx=new int[n*2],yy=new int[n*2];
        for(int layer=0;layer<2;layer++) {
            for(int i=0;i<n;i++) {
                double u=i/(double)(n-1),width=Math.sin(u*Math.PI)*(2+Math.sin(p*Math.PI)*13)*(i%3==0?1:.65);
                width=Math.max(0,width-layer*3);
                xx[i]=xx[n*2-i-1]=x+(int)((ex-x)*u);
                yy[i]=y+(int)((ey-y)*u-width);yy[n*2-i-1]=y+(int)((ey-y)*u+width);
            }
            g.setColor(layer==0?new Color(153,96,220,(int)((1-p)*235)):new Color(9,5,23,(int)((1-p)*255)));
            g.fillPolygon(xx,yy,n*2);
        }
        g.setColor(new Color(221,198,255,(int)((1-p)*255)));g.drawLine(x,y,ex,ey);
    }

    /** Tight native-pixel currents converge on the blade, not a screen-sized warning ring. */
    private static void gather(Graphics2D g,int x,int y,double t,double power,boolean reduced) {
        int r=2+(int)(power*5);g.setColor(VOID);g.fillOval(x-r-2,y-r-2,r*2+4,r*2+4);
        g.setColor(VIOLET);g.fillOval(x-r,y-r,r*2,r*2);g.setColor(LIGHT);g.fillRect(x-1,y-r+1,2,Math.max(2,r*2-2));
        if(reduced)return;
        for(int i=0;i<9;i++) {
            double p=(t*1.8+i/9.)%1,a=i*2.4+p*2,dist=5+(1-p)*25;
            int dx=(int)(Math.cos(a)*dist),dy=(int)(Math.sin(a)*dist);
            g.setColor(i%3==0?WHITE:VIOLET);g.drawLine(x+dx,y+dy,x+(int)(dx*.8),y+(int)(dy*.8));
        }
    }

    private static void cut(Graphics2D g,int x,int y,int ex,int ey,double power,boolean dark) {
        if(power<=0)return;int w=2+(int)(power*5),mx=(x+ex)/2,my=(y+ey)/2;
        g.setColor(dark?new Color(75,39,123,(int)(power*170)):new Color(33,123,161,(int)(power*170)));
        g.fillPolygon(new int[]{x,mx,ex,mx},new int[]{y,my-w-4,ey,my+w+4},4);
        g.setColor(dark?VIOLET:new Color(100,229,235));g.fillPolygon(new int[]{x,mx,ex,mx},new int[]{y,my-w,ey,my+w},4);
        g.setColor(dark?VOID:WHITE);g.fillPolygon(new int[]{x,mx,ex,mx},new int[]{y,my-w+2,ey,my+w-2},4);
        g.setColor(dark?LIGHT:WHITE);g.drawLine(x,y-1,ex,ey-1);
    }

    private static void burst(Graphics2D g,int x,int y,double age,double force,boolean gold,boolean reduced) {
        if(age<0||age>.42)return;
        double p=age/.42,alpha=1-p;
        Color edge=gold?new Color(225,175,75):VIOLET,shine=gold?new Color(255,242,179):LIGHT;
        int length=(int)((9+23*Math.sin(Math.min(1,p*1.5)*Math.PI/2))*force);
        g.setColor(new Color(edge.getRed(),edge.getGreen(),edge.getBlue(),(int)(alpha*150)));
        g.fillPolygon(new int[]{x-length,x+4,x+length,x-4},new int[]{y+length/2,y-8,y-length/2,y+8},4);
        g.setColor(new Color(shine.getRed(),shine.getGreen(),shine.getBlue(),(int)(alpha*255)));g.drawLine(x-length,y+length/2,x+length,y-length/2);
        if(age<.11){g.setColor(WHITE);g.fillPolygon(new int[]{x-3,x,x+3,x},new int[]{y,y-15,y,y+15},4);}
        if(reduced)return;
        for(int i=0;i<12;i++) {
            double a=i*2.4,dist=(6+p*34)*force;
            int sx=x+(int)(Math.cos(a)*dist),sy=y+(int)(Math.sin(a)*dist*.65+p*p*9);
            g.setColor(i%3==0?new Color(shine.getRed(),shine.getGreen(),shine.getBlue(),(int)(alpha*255)):
                    new Color(edge.getRed(),edge.getGreen(),edge.getBlue(),(int)(alpha*230)));
            g.drawLine(sx,sy,sx+(int)(Math.cos(a)*4),sy+(int)(Math.sin(a)*4));
        }
    }

}
