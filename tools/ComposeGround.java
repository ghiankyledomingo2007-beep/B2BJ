import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Composes the two 128x128 Wang sheets the outpost renderer consumes (ground: mud/paving,
 * banks: raised earth/transparent) from 32x32 material swatches, either procedural or
 * reviewed PixelLab textures. Deterministic; no resampling; corner masks follow WangTileset.
 *
 * java tools/ComposeGround.java <out-dir> [mud.png] [slab.png] [bank.png]
 */
public class ComposeGround {
    static final int CELL=32;
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        BufferedImage mud=args.length>1?swatch(args[1]):proceduralMud();
        BufferedImage slab=args.length>2?swatch(args[2]):proceduralSlab();
        BufferedImage bank=args.length>3?swatch(args[3]):proceduralBank();
        BufferedImage ground=sheet(mud,slab,false),banks=sheet(mud,bank,true);
        ImageIO.write(ground,"png",out.resolve("ground.png").toFile());
        ImageIO.write(banks,"png",out.resolve("banks.png").toFile());
        ImageIO.write(scale(ground,4),"png",out.resolve("ground-4x.png").toFile());
        ImageIO.write(scale(banks,4),"png",out.resolve("banks-4x.png").toFile());
        ImageIO.write(scale(sample(ground),2),"png",out.resolve("ground-sample.png").toFile());
        System.out.println("wrote "+out);
    }
    static BufferedImage swatch(String path) throws Exception {
        BufferedImage image=ImageIO.read(Path.of(path).toFile());
        if(image.getWidth()<CELL||image.getHeight()<CELL)throw new IllegalArgumentException("swatch must be at least 32x32: "+path);
        return image.getSubimage(0,0,CELL,CELL);
    }
    /** Periodic hash noise: the same value pattern on every cell so shared edges match. */
    static int hash(int x,int y,int salt) {
        int h=(x*374761393)^(y*668265263)^(salt*1274126177);
        h=(h^(h>>>13))*1274126177;return (h^(h>>>16))&0x7fffffff;
    }
    static double smooth(double u,double v) {
        // Periodic low-frequency wobble for the paving edge; wraps at the cell border.
        return .5+.18*Math.sin(2*Math.PI*u+1.3)*Math.cos(2*Math.PI*v+.4)
                +.12*Math.sin(4*Math.PI*v+2.1)*Math.cos(2*Math.PI*u+.9);
    }
    static boolean upper(int mask,int x,int y) {
        double u=(x+.5)/CELL,v=(y+.5)/CELL;
        double blend=((mask&1)!=0?(1-u)*(1-v):0)+((mask&2)!=0?u*(1-v):0)
                +((mask&4)!=0?(1-u)*v:0)+((mask&8)!=0?u*v:0);
        double ragged=(hash(x,y,7)%100)/100.0*.16-.08;
        return blend+(smooth(u,v)-.5)*.5+ragged>=.5;
    }
    static BufferedImage sheet(BufferedImage lower,BufferedImage upper,boolean transparentLower) {
        BufferedImage sheet=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        for(int mask=0;mask<16;mask++) {
            int ox=WangTileset.sourceX(mask),oy=WangTileset.sourceY(mask);
            boolean[][] top=new boolean[CELL][CELL];
            for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++)top[y][x]=upper(mask,x,y);
            for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
                boolean edge=false;
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    int nx=Math.max(0,Math.min(CELL-1,x+d[0])),ny=Math.max(0,Math.min(CELL-1,y+d[1]));
                    edge|=top[ny][nx]!=top[y][x];
                }
                int pixel;
                if(top[y][x]) {
                    pixel=upper.getRGB(x,y);
                    if(edge)pixel=transparentLower?shade(pixel,.72):seam(pixel,lower.getRGB(x,y));
                } else {
                    pixel=transparentLower?0:lower.getRGB(x,y);
                    if(edge&&!transparentLower)pixel=shade(pixel,.86);
                }
                sheet.setRGB(ox+x,oy+y,pixel);
            }
        }
        return sheet;
    }
    static int seam(int stone,int mud) { return shade(mud,.8); }
    static int shade(int argb,double factor) {
        int a=argb>>>24,r=(int)((argb>>16&255)*factor),g=(int)((argb>>8&255)*factor),b=(int)((argb&255)*factor);
        return a<<24|r<<16|g<<8|b;
    }
    static BufferedImage proceduralMud() {
        int[] tones={0xff353b33,0xff3b4239,0xff30362f,0xff2b312b};
        BufferedImage image=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
            double low=.5+.5*Math.sin(2*Math.PI*x/CELL*1+.7)*Math.cos(2*Math.PI*y/CELL*1+2.2);
            int h=hash(x/2,y/2,3)%100;
            int tone=h<18?3:h<40?2:low>.55?1:0;
            int pixel=tones[tone];
            int pebble=hash(x,y,11)%97;
            if(pebble==0)pixel=0xff4d5149;
            if(pebble==1&&x<CELL-1)pixel=0xff41483f;
            if(hash(x/4,y/3,17)%29==0&&hash(x,y,19)%3==0)pixel=0xff3f4a52; // faint wet glint
            image.setRGB(x,y,pixel);
        }
        return image;
    }
    static BufferedImage proceduralSlab() {
        // Three jittered grid lines per axis (periodic at 32) carve large irregular slabs.
        int[] gx={0,11,21},gy={0,10,22};
        int[][] jx=new int[4][4],jy=new int[4][4];
        for(int i=0;i<4;i++)for(int j=0;j<4;j++) {jx[i][j]=hash(i%3,j%3,23)%5-2;jy[i][j]=hash(i%3,j%3,29)%5-2;}
        int[] tones={0xff4a545c,0xff434d55,0xff3d4750,0xff505a62};
        BufferedImage image=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
            int column=0,row=0;
            for(int i=1;i<3;i++)if(x>=gx[i]+jx[i][(y*3/CELL)%3])column=i;
            for(int j=1;j<3;j++)if(y>=gy[j]+jy[(x*3/CELL)%3][j])row=j;
            boolean seamX=false,seamY=false;
            for(int i=1;i<3;i++)seamX|=x==gx[i]+jx[i][(y*3/CELL)%3];
            for(int j=1;j<3;j++)seamY|=y==gy[j]+jy[(x*3/CELL)%3][j];
            boolean wrapSeam=x==0&&hash(y/6,0,31)%2==0||y==0&&hash(x/6,1,37)%2==0;
            int slab=hash(column,row,41);
            int pixel=tones[slab%4];
            if(hash(x,y,43)%23==0)pixel=shade(pixel,.9);
            if(hash(x,y,47)%41==0)pixel=0xff59636b;
            boolean crack=slab%5==0&&Math.abs((x-gx[column])-(y-gy[row])*(slab%2==0?1:-1)-(slab%7-3))<=0&&hash(x,y,53)%3>0;
            if(seamX||seamY||wrapSeam||crack)pixel=0xff262c31;
            image.setRGB(x,y,pixel);
        }
        return image;
    }
    static BufferedImage proceduralBank() {
        int[] tones={0xff3d3a31,0xff433f34,0xff36342c,0xff2f2d27};
        BufferedImage image=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
            int h=hash(x/2,y/2,61)%100;
            int pixel=tones[h<15?3:h<40?2:h<75?0:1];
            int stone=hash(x/3,y/3,67)%23;
            if(stone==0&&hash(x,y,71)%2==0)pixel=0xff5a5f5f;
            if(stone==1&&hash(x,y,73)%3==0)pixel=0xff4a4f4f;
            if(hash(x/5,y,79)%31==0)pixel=0xff4b3f30; // thin root
            image.setRGB(x,y,pixel);
        }
        return image;
    }
    static BufferedImage scale(BufferedImage source,int factor) {
        BufferedImage result=new BufferedImage(source.getWidth()*factor,source.getHeight()*factor,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();
        g.setColor(new java.awt.Color(25,30,42));g.fillRect(0,0,result.getWidth(),result.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,0,0,result.getWidth(),result.getHeight(),null);g.dispose();return result;
    }
    /** A 10x6 map sample with a paved blob and road, to judge seams and repetition. */
    static BufferedImage sample(BufferedImage sheet) {
        int w=10,h=6;boolean[][] stone=new boolean[h+1][w+1];
        for(int y=0;y<=h;y++)for(int x=0;x<=w;x++)stone[y][x]=(x-5)*(x-5)/9.0+(y-3)*(y-3)/3.0<1||(y==3&&x>=2)||(x==5&&y<=3);
        BufferedImage result=new BufferedImage(w*CELL,h*CELL,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int mask=(stone[y][x]?1:0)|(stone[y][x+1]?2:0)|(stone[y+1][x]?4:0)|(stone[y+1][x+1]?8:0);
            int sx=WangTileset.sourceX(mask),sy=WangTileset.sourceY(mask);
            g.drawImage(sheet,x*CELL,y*CELL,x*CELL+CELL,y*CELL+CELL,sx,sy,sx+CELL,sy+CELL,null);
        }
        g.dispose();return result;
    }
}
