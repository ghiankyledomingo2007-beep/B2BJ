import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Approved stone-reliquary artwork, sampled once onto the game's native 2x grid. */
final class StoneMenuArt {
    private static final BufferedImage FRAME = trim(load("frame"));
    private static final BufferedImage BUTTON = trim(load("button"));
    private static final BufferedImage[] ICONS = icons();
    private static final Map<Integer, BufferedImage[]> ROWS = new HashMap<>();
    private static BufferedImage frame, light;

    private static BufferedImage load(String name) {
        var image=B2BJ.loadImage("assets/ui/stone-menu/"+name+".png");
        if(image==null)throw new IllegalStateException("Missing stone menu art: "+name);
        return image;
    }

    private static BufferedImage trim(BufferedImage image) {
        int x0=image.getWidth(),y0=image.getHeight(),x1=-1,y1=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>=128) {
            x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);
        }
        if(x1<x0)throw new IllegalStateException("Empty stone menu art");
        return image.getSubimage(x0,y0,x1-x0+1,y1-y0+1);
    }

    private static BufferedImage sample(BufferedImage image,int w,int h) {
        var result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int p=image.getRGB(Math.min(image.getWidth()-1,(int)((x+.5)*image.getWidth()/w)),
                    Math.min(image.getHeight()-1,(int)((y+.5)*image.getHeight()/h)));
            result.setRGB(x,y,(p>>>24)<128?0:0xff000000|(p&0xffffff));
        }
        return result;
    }

    private static BufferedImage[] icons() {
        var source=load("icons");
        var out=new BufferedImage[5];
        for(int i=0;i<out.length;i++) {
            int x=i*source.getWidth()/5,end=(i+1)*source.getWidth()/5;
            var icon=trim(source.getSubimage(x,0,end-x,source.getHeight()));
            double scale=20.0/Math.max(icon.getWidth(),icon.getHeight());
            var pixels=sample(icon,Math.max(1,(int)(icon.getWidth()*scale)),Math.max(1,(int)(icon.getHeight()*scale)));
            out[i]=new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
            var g=out[i].createGraphics();g.drawImage(pixels,(20-pixels.getWidth())/2,(20-pixels.getHeight())/2,null);g.dispose();
        }
        return out;
    }

    static void frame(Graphics2D graphics,int width,int height,double time,boolean reduced) {
        int w=Math.max(1,width/2),h=Math.max(1,height/2);
        if(frame==null||frame.getWidth()!=w||frame.getHeight()!=h) {
            frame=sample(FRAME,w,h);light=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int p=frame.getRGB(x,y),r=p>>16&255,green=p>>8&255,b=p&255;
                if((p>>>24)>0&&green>90&&b>80&&green>r*1.5&&b>r*1.5)light.setRGB(x,y,0xffa5fff0);
            }
        }
        var g=(Graphics2D)graphics.create();Ui.crisp(g);g.scale(2,2);g.drawImage(frame,0,0,null);
        if(!reduced) {
            g.setComposite(AlphaComposite.SrcOver.derive((float)(.09+.07*Math.sin(time*1.8))));
            g.drawImage(light,0,0,null);
            g.setComposite(AlphaComposite.SrcOver);
            for(int i=0;i<3;i++) {
                int x=w/2-12+i*12,y=(int)(h*.145)-(int)((time*5+i*9)%22);
                g.setColor(new Color(152,255,235,125));g.fillRect(x,y,1,1);
            }
        }
        g.dispose();
    }

    static void row(Graphics2D graphics,int width,int height,boolean selected,boolean pressed,
            double time,double selectedAt,boolean reduced) {
        int w=Math.max(1,width/2),h=Math.max(1,height/2),key=w<<16|h;
        var states=ROWS.computeIfAbsent(key,ignored->{
            var idle=sample(BUTTON,w,h);var active=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int p=idle.getRGB(x,y),r=p>>16&255,green=p>>8&255,b=p&255;
                if((p>>>24)>0&&b>r*1.2&&green>r*1.1) {
                    r=Math.min(255,r+3);green=Math.min(255,green+26);b=Math.min(255,b+25);
                    p=0xff000000|r<<16|green<<8|b;
                }
                active.setRGB(x,y,p);
            }
            return new BufferedImage[]{idle,active};
        });
        var base=states[selected?1:0];
        var g=(Graphics2D)graphics.create();Ui.crisp(g);g.scale(2,2);g.translate(0,pressed?1:0);
        g.drawImage(base,0,0,null);
        if(selected) {
            g.setColor(new Color(103,234,231,reduced?150:150+(int)(Math.sin(time*2)*30)));
            g.fillRect(15,3,Math.max(0,w-30),1);g.fillRect(15,h-5,Math.max(0,w-30),1);
            double age=time-selectedAt;
            if(!reduced&&age>=0&&age<.45) {
                int sweep=(int)(age/.45*(w+20))-10;
                g.setColor(new Color(189,255,244,45));
                for(int y=3;y<h-4;y++)for(int x=Math.max(0,sweep-y/2);x<Math.min(w,sweep-y/2+8);x++)
                    if((base.getRGB(x,y)>>>24)>0)g.fillRect(x,y,1,1);
            }
        }
        g.dispose();
    }

    static void icon(Graphics2D graphics,Ui.Icon symbol,int x,int y,boolean enabled) {
        int i=switch(symbol){case PLAY->0;case GEAR->1;case KEYS->2;case FLASK->3;case DOOR->4;default->-1;};
        if(i<0)throw new IllegalArgumentException("Unknown stone icon: "+symbol);
        var g=(Graphics2D)graphics.create();Ui.crisp(g);
        if(!enabled)g.setComposite(AlphaComposite.SrcOver.derive(.35f));
        g.drawImage(ICONS[i],x,y,40,40,null);g.dispose();
    }
}
