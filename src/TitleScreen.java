import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Outpost key art with restrained movement; all scene effects follow its native 640 x 360 grid. */
final class TitleScreen {
    private static final BufferedImage ART=B2BJ.loadImage("assets/ui/title/ruined-gate.png");
    private static final BufferedImage WATER=water();
    private static final TitleWind WIND=new TitleWind(ART,B2BJ.loadImage("assets/ui/title/ruined-gate-wind-clean.png"));
    private static final BufferedImage LOGO=B2BJ.loadImage("assets/ui/title/moonlit-gate-logo.png");
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
        var backdrop=(Graphics2D)g.create();
        double drift=reduced?1:1+.008*(1-Math.cos(seconds*Math.PI/24));
        backdrop.translate(width*(1-drift)*.8,height*(1-drift)*.6);
        backdrop.scale(drift,drift);
        drawBackdrop(backdrop,width,height,seconds,reduced);backdrop.dispose();
        MenuTheme.frame(g,width,height,seconds,reduced,false);
        g=(Graphics2D)g.create();double scale=menuScale(width,height);g.scale(scale,scale);
        double reveal=entrance(seconds,0,1.2,reduced);
        var brand=(Graphics2D)g.create();
        brand.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)reveal));
        brand.translate(0,(int)(12*(1-reveal)));
        if(LOGO!=null) {
            brand.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            brand.drawImage(LOGO,90,86,550,Math.round(550f*LOGO.getHeight()/LOGO.getWidth()),null);
        } else JourneyHud.text(brand,"Blob to Blade",90,220,Ui.menuFont(46),MenuTheme.IVORY);
        brand.dispose();
        var chapter=Ui.menuFont(11);
        Ui.prose(g,"CHAPTER I",(int)(width/scale)-76-g.getFontMetrics(chapter).stringWidth("CHAPTER I"),686,chapter,MenuTheme.MUTED);
        g.dispose();
    }

    static double menuScale(int width,int height) { return Math.min(width/1280.,height/720.); }

    /** Shared scene without the title logo or menu, also used when entering the outpost. */
    void drawBackdrop(Graphics2D g,int width,int height,double seconds,boolean reduced) {
        Ui.crisp(g);
        double blend=reduced||previous<0?1:1-Math.exp(-Math.min(.1,Math.max(0,seconds-previous))*8);
        for(int i=0;i<mood.length;i++)mood[i]+=((i==selected?1:0)-mood[i])*blend;
        previous=seconds;
        if(ART!=null) {
            var layer=sceneBuffer.createGraphics();Ui.crisp(layer);
            layer.drawImage(ART,0,0,null);scene(layer,640,360,seconds,reduced);layer.dispose();
            g.drawImage(sceneBuffer,0,0,width,height,null);
        }
        Ui.fade(g,0,0,(int)(620*menuScale(width,height)),height,120,0);
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
        double pulse=reduced?1:1+Math.sin(seconds*1.25)*.14;
        glow(g,494,171,42,71,new Color(98,222,241,(int)((mood[0]*16+mood[1]*29+mood[2]*5)*pulse)));
        glow(g,477,302,31,10,new Color(80,219,238,(int)(mood[0]*5+mood[1]*17)));
        glow(g,463,232,123,110,new Color(112,152,210,(int)(mood[2]*12)));
        if(!reduced) {
            g.setColor(new Color(151,190,204,(int)(42*(1-.65*mood[2]))));
            for(int i=0;i<24;i++) {
                int x=Math.floorMod(i*137,650),y=(int)(seconds*90+i*61)%400-20;
                g.drawLine(x,y,x-2,y+8+i%3*2);
            }
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
