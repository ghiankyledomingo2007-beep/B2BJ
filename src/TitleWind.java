import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/** Hair and coat move over a clean plate; the head, hands, stance and architecture stay anchored. */
final class TitleWind {
    private static final int X=450,Y=228,W=64,H=86,FRAMES=64;
    private static final double PERIOD=6.4;
    private static final Polygon HAIR=polygon(474,229,479,228,482,230,483,233,483,241,480,246,
            479,250,477,253,474,255,472,257,470,257,469,259,466,257,464,260,462,259,
            462,255,459,256,457,255,458,252,461,250,462,248,464,247,465,244,468,240,471,238,472,233);
    private static final Polygon COAT=polygon(467,267,473,268,479,268,485,267,486,271,487,279,
            488,284,489,291,486,292,483,291,480,292,477,292,475,293,470,292,468,290,
            465,291,460,292,461,287,463,282,465,277);
    private static final Polygon SWORD=polygon(489,277,492,280,496,286,501,294,506,301,510,308,
            507,309,502,303,497,296,493,289,489,282);
    private final BufferedImage clear=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage swordClear=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage sword=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage[] frames=new BufferedImage[FRAMES];
    private final boolean available;

    TitleWind(BufferedImage art,BufferedImage plate) {
        available=art!=null&&plate!=null&&art.getWidth()==640&&art.getHeight()==360
                &&plate.getWidth()==640&&plate.getHeight()==360;
        if(!available)return;
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            int px=x+X,py=y+Y;
            if(HAIR.contains(px+.5,py+.5)||COAT.contains(px+.5,py+.5))clear.setRGB(x,y,plate.getRGB(px,py));
            if(SWORD.contains(px+.5,py+.5)) {
                swordClear.setRGB(x,y,plate.getRGB(px,py));sword.setRGB(x,y,art.getRGB(px,py));
            }
        }
        for(int frame=0;frame<FRAMES;frame++) {
            var image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
            double phase=frame*Math.PI*2/FRAMES;
            warp(image,art,COAT,phase,false);
            warp(image,art,HAIR,phase,true);
            frames[frame]=image;
        }
    }

    boolean available() {return available;}

    void draw(Graphics2D g,double seconds,double ready) {
        if(!available)return;
        int frame=(int)(Math.max(0,seconds)%PERIOD/PERIOD*FRAMES);
        g.drawImage(clear,X,Y,null);g.drawImage(frames[frame],X,Y,null);
        if(ready>.01) {
            g.drawImage(swordClear,X,Y,null);
            var blade=(Graphics2D)g.create();
            blade.rotate(-.09*ready,490,278);blade.drawImage(sword,X,Y,null);blade.dispose();
        }
    }

    private static void warp(BufferedImage image,BufferedImage art,Polygon mask,double phase,boolean hair) {
        double gust=(1-Math.cos(phase))*.5;
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            double px=x+X,py=y+Y,sx=px,sy=py;
            for(int iteration=0;iteration<3;iteration++) {
                double weight=hair?clamp((sy-236)/20)*clamp((478-sx)/18):Math.pow(clamp((sy-270)/22),1.4);
                sx=px+(hair?3.8:2.4)*gust*weight;
                sy=py-(hair?.7:.45)*Math.sin(phase)*weight;
            }
            int ix=(int)Math.round(sx),iy=(int)Math.round(sy);
            if(mask.contains(ix+.5,iy+.5))image.setRGB(x,y,art.getRGB(ix,iy));
        }
    }

    private static double clamp(double n) {return Math.max(0,Math.min(1,n));}
    private static Polygon polygon(int... points) {
        var out=new Polygon();
        for(int i=0;i<points.length;i+=2)out.addPoint(points[i],points[i+1]);
        return out;
    }
}
