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
            int blotch=hash(x/8,y/8,5)%100,grain=hash(x,y,3)%100;
            int tone=grain<14?3:grain<34?2:blotch<45&&grain>72?1:0;
            int pixel=tones[tone];
            int pebble=hash(x,y,11)%97;
            if(pebble==0)pixel=0xff4d5149;
            if(pebble==1)pixel=0xff41483f;
            if(hash(x,y,19)%131==0)pixel=0xff3c464d; // rare single wet glint, no fixed cluster
            image.setRGB(x,y,pixel);
        }
        return image;
    }
    static BufferedImage proceduralSlab() {
        // Six seeds in a periodic 32px domain: irregular flagstones, not a brick grid.
        // Stratified seeds in a periodic 32px domain: large uneven flagstones, low-contrast seams.
        int[][] seed={{6,7},{23,4},{27,19},{11,21},{2,29}};
        double[] weight={1,1.1,.95,1.15,1.05};
        int seeds=seed.length;
        int[][] owner=new int[CELL][CELL];
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
            int best=-1;double bestDistance=Double.MAX_VALUE;
            for(int i=0;i<seeds;i++)for(int dx=-CELL;dx<=CELL;dx+=CELL)for(int dy=-CELL;dy<=CELL;dy+=CELL) {
                double ex=Math.abs(x-seed[i][0]-dx),ey=Math.abs(y-seed[i][1]-dy);
                // Squarish slabs with softened corners: mostly Chebyshev, a little Euclidean.
                double distance=(Math.max(ex,ey*1.15)*.7+Math.sqrt(ex*ex+ey*ey)*.3)*weight[i]+(hash(x,y,100+i)%3)*.5;
                if(distance<bestDistance){bestDistance=distance;best=i;}
            }
            owner[y][x]=best;
        }
        // Mostly the two darker slate tones; the two lighter ones only on two slabs.
        int[] tones={0xff394249,0xff3b444b,0xff414a52,0xff384148,0xff434c54};
        BufferedImage image=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
            int slab=owner[y][x];
            boolean seam=false;
            for(int[] d:new int[][]{{1,0},{0,1}})seam|=owner[(y+d[1])%CELL][(x+d[0])%CELL]!=slab;
            int pixel=tones[slab%tones.length];
            int sx=seed[slab][0],sy=seed[slab][1];
            // One small highlight cluster on the upper-left rim of each larger slab.
            int rx=((x-sx)%CELL+CELL)%CELL,ry=((y-sy)%CELL+CELL)%CELL;
            if(slab%5==2&&rx>=CELL-4&&rx<=CELL-3&&ry==CELL-3)pixel=shade(pixel,1.07);
            if(hash(x,y,47)%29==0)pixel=shade(pixel,.94);
            // A wet puddle cluster in the low slab.
            if(slab==3&&rx>=2&&rx<=4&&ry>=2&&ry<=3)pixel=0xff30373d;
            boolean crack=slab==1&&(x-sx)==(y-sy)&&Math.abs(x-sx)<3;
            // Seams are earth between sunk stones, not navy grout.
            if(seam||crack)pixel=hash(x,y,53)%3==0?0xff2c322c:0xff303631;
            image.setRGB(x,y,pixel);
        }
        return image;
    }
    static BufferedImage proceduralBank() {
        int[] tones={0xff433f35,0xff4a453b,0xff3d3a31,0xff35332c};
        BufferedImage image=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++) {
            int h=hash(x/2,y/2,61)%100;
            int pixel=tones[h<15?3:h<40?2:h<75?0:1];
            int stone=hash(x/3,y/3,67)%23;
            if(stone==0&&hash(x,y,71)%2==0)pixel=0xff676c6a;
            if(stone==1&&hash(x,y,73)%3==0)pixel=0xff585d5c;
            if(hash(x/5,y,79)%31==0)pixel=0xff5a4a36; // thin root
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
