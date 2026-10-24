import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Outpost key art with restrained movement; all scene effects follow its native 640 x 360 grid. */
final class TitleScreen {
    private static final BufferedImage ART=B2BJ.loadImage("assets/ui/title/ruined-gate.png");
    private static final BufferedImage WATER=water();
    private static final TitleWind WIND=new TitleWind(ART,B2BJ.loadImage("assets/ui/title/ruined-gate-wind-clean.png"));
    private static final BufferedImage[] BLOB=word("BLOB",new int[]{0xc4e5df,0xa6ceca,0x79aaa9,0x507e86});
    private static final BufferedImage[] BLADE=word("BLADE",new int[]{0xf2f1e6,0xdce1de,0xbdc8cc,0x8e9da8});
    private final double[] mood={1,0,0,0};
    private final BufferedImage sceneBuffer=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
    private int selected;
    private double previous=-1;
    static boolean available() {return ART!=null;}
    void select(int choice) {selected=Math.max(0,Math.min(3,choice));}
    void enter() {previous=-1;}

    static double entrance(double time,double delay,double duration,boolean reduced) {
        if(reduced)return 1;
        double p=Math.max(0,Math.min(1,(time-delay)/duration));
        return 1-Math.pow(1-p,3);
    }

    void draw(Graphics2D g,int width,int height,double seconds,BufferedImage slimeIdle,boolean reduced) {
        Ui.crisp(g);
        double blend=reduced||previous<0?1:1-Math.exp(-Math.min(.1,Math.max(0,seconds-previous))*8);
        for(int i=0;i<mood.length;i++)mood[i]+=((i==selected?1:0)-mood[i])*blend;
        previous=seconds;
        if(ART!=null) {
            var layer=sceneBuffer.createGraphics();Ui.crisp(layer);
            layer.drawImage(ART,0,0,null);scene(layer,640,360,seconds,reduced);layer.dispose();
            g.drawImage(sceneBuffer,0,0,width,height,null);
        }
        Ui.fade(g,0,0,620,height,120,0);
        if(!reduced) {
            g.setColor(new Color(151,190,204,(int)(55*(1-.65*mood[2]))));
            for(int i=0;i<22;i++) {
                int x=520+Math.floorMod(i*137,740),y=(int)(seconds*80+i*61)%height;
                g.fillRect(x/2*2,y/2*2,2,6+(i%3)*2);
            }
        }
        word(g,BLOB,76,80,seconds,0,reduced,-28);
        double link=entrance(seconds,.28,.6,reduced);
        var connector=(Graphics2D)g.create();
        connector.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)link));
        Ui.text(connector,"TO",80,225,3,Ui.GOLD);
        connector.setColor(new Color(153,127,83,150));connector.fillRect(140,214,(int)(240*link),2);connector.dispose();
        word(g,BLADE,74,240,seconds,.35,reduced,32);
        g.setColor(Ui.TEAL);g.fillRect(80,366,(int)(50*entrance(seconds,1.05,.4,reduced)),2);
    }

    private static BufferedImage[] word(String value,int[] ramp) {
        var original=Ui.title(value,ramp);
        var face=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB);
        var shade=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB);
        // The kit's one-pixel outline becomes nine pixels at logo scale; keep only its letter faces.
        for(int y=0;y<face.getHeight();y++)for(int x=0;x<face.getWidth();x++) {
            int pixel=original.getRGB(x,y);
            for(int color:ramp)if(pixel==(0xff000000|color)) {
                face.setRGB(x,y,pixel);shade.setRGB(x,y,0xb005101b);break;
            }
        }
        return new BufferedImage[]{shade,face};
    }

    private static void word(Graphics2D graphics,BufferedImage[] word,int x,int y,double time,
            double delay,boolean reduced,int travel) {
        double reveal=entrance(time,delay,.85,reduced);
        if(reveal<=0)return;
        int w=word[0].getWidth()*9,h=word[0].getHeight()*9;
        var g=(Graphics2D)graphics.create();
        g.translate((int)(travel*(1-reveal)),(int)(10*(1-reveal)));
        glow(g,x+w*.45,y+48,w*.65,70,new Color(78,208,229,(int)(34*(1-reveal))));
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)reveal));
        g.clipRect(x-20,y-20,(int)((w+12)*reveal),h+16);
        g.drawImage(word[0],x-18+4,y-18+6,w,h,null);
        double sweep=-1;
        if(!reduced) {
            if(time<delay+1.2)sweep=(time-delay-.18)/.9;
            else if(time>4.2+delay) {
                double repeat=(time-4.2-delay)%9;
                if(repeat<1.1)sweep=repeat/1.1;
            }
        }
        g.drawImage(sheen(word[1],sweep),x-18,y-18,w,h,null);
        g.dispose();
    }

    private static BufferedImage sheen(BufferedImage face,double sweep) {
        if(sweep<0||sweep>1)return face;
        var bright=new BufferedImage(face.getWidth(),face.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<face.getHeight();y++)for(int x=0;x<face.getWidth();x++) {
            int pixel=face.getRGB(x,y);
            if((pixel>>>24)==0)continue;
            double position=(x+y*.45)/(face.getWidth()+face.getHeight()*.45);
            double shine=Math.max(0,1-Math.abs(position-sweep)/.11)*.6;
            int r=pixel>>16&255,green=pixel>>8&255,b=pixel&255;
            r+=(int)((220-r)*shine);green+=(int)((255-green)*shine);b+=(int)((255-b)*shine);
            bright.setRGB(x,y,0xff000000|r<<16|green<<8|b);
        }
        return bright;
    }

    private static BufferedImage water() {
        if(ART==null)return null;
        var out=new BufferedImage(ART.getWidth(),ART.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=298;y<350;y++)for(int x=395;x<588;x++) {
            int rgb=ART.getRGB(x,y),r=rgb>>16&255,green=rgb>>8&255,b=rgb&255;
            if(green>70&&b>r*1.2)out.setRGB(x,y,0x58000000|(rgb&0xffffff));
        }
        return out;
    }

    private void scene(Graphics2D graphics,int width,int height,double seconds,boolean reduced) {
        var g=(Graphics2D)graphics.create();
        g.scale(width/640.0,height/360.0);
        if(!reduced)WIND.draw(g,seconds,mood[1]);
        double flicker=reduced?0:(Math.sin(seconds*3.4)*.6+Math.sin(seconds*7.9)*.4)*(1-.75*mood[2]);
        glow(g,564,183,43,56,new Color(255,171,65,18+(int)(flicker*5)));
        glow(g,564,181,8,13,new Color(255,214,133,25+(int)(flicker*8)));
        glow(g,494,171,42,71,new Color(98,222,241,(int)(mood[0]*16+mood[1]*29+mood[2]*5)));
        glow(g,477,302,31,10,new Color(80,219,238,(int)(mood[0]*5+mood[1]*17)));
        glow(g,463,232,123,110,new Color(112,152,210,(int)(mood[2]*12)));
        if(!reduced) {
            glow(g,376+Math.sin(seconds*.08)*22,282,146,17,new Color(131,172,184,12));
            glow(g,503+Math.sin(seconds*.11+1)*18,327,128,13,new Color(153,182,190,10));
            for(int y=298;y<350;y+=2) {
                int drift=(int)Math.round(Math.sin(seconds*1.3+y*.45));
                g.drawImage(WATER,395+drift,y,588+drift,y+2,395,y,588,y+2,null);
            }
            g.setColor(new Color(130,240,248,(int)(mood[0]*22+mood[1]*80)));
            for(int i=0;i<5;i++) {
                int x=458+i*10+(int)(Math.sin(seconds*.8+i)*4),y=285-(int)((seconds*9+i*13)%60);
                g.fillRect(x,y,1,2);
            }
        }
        if(mood[3]>.04) {g.setColor(new Color(2,5,12,(int)(mood[3]*28)));g.fillRect(0,0,640,360);}
        g.dispose();
    }

    private static void glow(Graphics2D graphics,double x,double y,double rx,double ry,Color color) {
        if(color.getAlpha()==0)return;
        var g=(Graphics2D)graphics.create();
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(color.getAlpha()/255f));
        g.drawImage(OutpostLights.glow(64,color.getRGB(),255),(int)(x-rx),(int)(y-ry*1.25),
                (int)(rx*2),(int)(ry*2.5),null);g.dispose();
    }
}
