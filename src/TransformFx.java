import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Slime into Rainoray (Q at 100 Ichor), after the owner's favourite older trailer test (2026-10-29: "a gooey swirl
 * going up"). The slime lights up and squats, then its own pixels stretch up (face and star with them) and smooth
 * into a soft goo teardrop while two thick goo strands swirl up round it and sink back in; the teardrop draws in to
 * his size and flashes white, the flash leaves a cyan jelly Rainoray (his idle frame as the mask), and the jelly
 * clears from the feet up to his real colours as the sword glints. Native pixels shown at 2x, nearest neighbour
 * only, and every mark is a function of the transformation's progress (game time), so 120 Hz substeps are true slow
 * motion.
 */
final class TransformFx {
    /** The buffer: Rainoray's 80 px cell sits at (8, 20), his feet on row 92; the slime's 48 px cell at (24, 44). */
    static final int W=96,H=100,HERO_X=8,HERO_Y=20,SLIME_X=24,SLIME_Y=44,FEET_X=48,FEET_Y=92;
    /** The teardrop flashes white at FLASH and leaves him in its place at SWAP (about three 120 Hz substeps). */
    static final double FLASH=.43,SWAP=.46;
    private static final int[] RAMP={0xff0c3440,0xff125a6c,0xff2092a4,0xff54d4de,0xffbefaf8};   // the slime's five cyans
    private static final int WHITE=0xfff4fffe;
    private static final int[] TONE={RAMP[0],RAMP[1],RAMP[2],RAMP[3],RAMP[4],WHITE};          // tone 5 is white
    /** The goo's tones: the slime's own ink, shade and body cyan (so it stretches without changing colour), the
     *  ramp's dark teal, pale cyan and white between them. After the flash he is jelly in the ramp's cyans. */
    private static final int[] GOO_TONE={0xff0a0c12,RAMP[1],0xff1f9e9b,0xff2ed1cd,RAMP[4],WHITE};
    private static final BufferedImage BUFFER=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
    private static final int[] PX=new int[W*H];
    /** This frame's goo: which pixels it covers, the slime pixel each is stretched from, each row's extent. */
    private static final boolean[] GOO=new boolean[W*H];
    private static final int[] FROM=new int[W*H];
    private static final double[] ROW_L=new double[H],ROW_R=new double[H],ROW_U=new double[H];
    private TransformFx() { }

    /**
     * Draws the change at progress t (0-1) with the buffer's top-left at screen (left, top). The slime is the cell at
     * (slimeX, slimeY) of its sheet and Rainoray the cell at (heroX, heroY) of his idle sheet: the frames shown just
     * before and just after, so the change starts and lands on them exactly.
     */
    static void draw(Graphics2D canvas,int left,int top,double t,BufferedImage slime,int slimeX,int slimeY,boolean slimeFlip,
            BufferedImage hero,int heroX,int heroY,int heroCell,boolean heroFlip,boolean reduced) {
        Arrays.fill(PX,0);Arrays.fill(GOO,false);
        int margin=(heroCell-80)/2;
        int[] heroPx=cell(hero,heroX,heroY,heroCell,heroFlip,HERO_X-margin,HERO_Y-margin);
        int[] slimePx=slime==null?new int[W*H]:cell(slime,slimeX,slimeY,48,slimeFlip,SLIME_X,SLIME_Y);
        double time=t*TransformationAnimation.DURATION;
        int[] sl=new int[H],sr=new int[H],hl=new int[H],hr=new int[H];
        int[] own=extent(slimePx,sl,sr),his=extent(heroPx,hl,hr);   // {top, bottom, widest, median width}
        boolean noSlime=own[0]<0;
        if(noSlime)own=new int[]{FEET_Y-30,FEET_Y-1,40,40};
        if(his[0]<0)his=new int[]{FEET_Y-62,FEET_Y,24,24};

        double glow=smooth(clamp(t/.1)),crouch=Math.sin(Math.PI*clamp((t-.02)/.1));
        double rise=smooth(clamp((t-.08)/.28)),settle=smooth(clamp((t-.33)/.1));
        double morph=noSlime?1:smooth(clamp((t-.24)/.12));               // the last third of the rise: its pixels smooth into goo
        double in=smooth(clamp((t-.3)/(FLASH-.3)));                       // the strands wind in and sink into it
        int bright=t>FLASH-.027?1:0;                                     // a step brighter the two substeps before the flash
        double clear=smooth(clamp((t-.5)/.38));

        // the goo: the slime's rows stretched up about its feet (squatting first), drawing in to his height and width
        double tallOwn=own[1]-own[0]+1,tallHis=his[1]-his[0]+1;
        double height=lerp(lerp(tallOwn*(1-.16*crouch),Math.min(tallHis+4,own[1]-1),rise),tallHis,settle);
        double sx=lerp(1+.08*crouch,.62,rise),half=own[2]/2.*sx;
        double[] bodyL=new double[H],bodyR=new double[H];
        body(hl,hr,his,bodyL,bodyR);
        int base=(int)Math.round(lerp(own[1],his[1],rise)),rows=t<SWAP?(int)Math.round(height):0;
        for(int k=0;k<rows&&base-k>=0;k++) {
            int y=base-k;
            double u=(k+.5)/rows;
            int ys=(int)Math.round(own[1]-k*(tallOwn-1)/Math.max(1,rows-1));
            boolean empty=sr[ys]<sl[ys];
            double ownL=empty?FEET_X:FEET_X+(sl[ys]-FEET_X)*sx,ownR=empty?FEET_X:FEET_X+(sr[ys]+1-FEET_X)*sx;
            double hw=half*profile(u)*(1+.07*Math.sin(u*9-time*14)+.04*Math.sin(u*17-time*23)),cx=FEET_X+sway(u,time)*morph;
            double l=lerp(lerp(ownL,cx-hw-drip(u,t,-1),morph),bodyL[y],settle),r=lerp(lerp(ownR,cx+hw+drip(u,t,1),morph),bodyR[y],settle);
            ROW_L[y]=l;ROW_R[y]=r;ROW_U[y]=u;
            int x0=(int)Math.ceil(l-.5),x1=(int)Math.floor(r-.5);
            for(int x=Math.max(0,x0);x<=Math.min(W-1,x1);x++) {
                GOO[y*W+x]=true;
                FROM[y*W+x]=empty?0:slimePx[ys*W+sl[ys]+(int)Math.round((x-x0)*(double)(sr[ys]-sl[ys])/Math.max(1,x1-x0))];
            }
        }
        int crown=base+1;
        for(int y=Math.max(0,base-rows+1);y<=base&&crown>base;y++)for(int x=0;x<W;x++)if(GOO[y*W+x]) {crown=y;break;}

        if(t<FLASH)puddle(base,own[2]/2.+1,in,smooth(clamp((t-.05)/.1)));    // the goo it rises out of
        int strands=reduced?1:2;
        if(t<FLASH)strands(strands,t,time,base,rows,in,false);
        for(int y=Math.max(0,crown);y<=base;y++)for(int x=0;x<W;x++) {
            int i=y*W+x;
            if(!GOO[i])continue;
            boolean rim=!goo(x-1,y)||!goo(x+1,y)||!goo(x,y-1)||!goo(x,y+1);
            if(t>=FLASH) {PX[i]=rim?RAMP[4]:WHITE;continue;}
            int c=FROM[i],goo=gooTone(x,y,crown,time);
            double lit=c==0?goo:ownTone(c,glow,rim&&(!goo(x-1,y)||!goo(x,y-1)));
            int tone=Math.max(0,Math.min(5,(int)Math.round(lerp(lit,goo,morph))+bright));
            PX[i]=c!=0&&tone==ramp(c)&&morph<.5?c:GOO_TONE[tone];         // its own colour until its tone moves
        }
        if(!reduced&&rows>0&&t>.1&&t<FLASH)bubbles(time,base,rows,morph);
        if(!reduced&&t>.14&&t<FLASH)flung(t,base,rows);
        if(t<FLASH)strands(strands,t,time,base,rows,in,true);
        if(!reduced&&t>.1&&t<FLASH)sparkles(t,base,rows);

        int heroTop=his[0];
        int lift=(int)Math.ceil(3*(1-clamp((t-SWAP)/.08)));                // the flash settling into jelly over six substeps
        if(t>=FLASH)for(int i=0;i<PX.length;i++) {                       // Rainoray: jelly, clearing from the feet up
            int c=heroPx[i];
            if(c==0)continue;
            int x=i%W,y=i/W;
            if(t<SWAP) {PX[i]=edge(heroPx,x,y)?RAMP[4]:WHITE;continue;}
            int line=(int)Math.round(FEET_Y+1-clear*(FEET_Y+2-heroTop));
            if(y>=line) {PX[i]=c;continue;}
            PX[i]=y==line-1&&clear>0?WHITE:TONE[Math.min(5,jelly(heroPx,x,y,c)+lift)];
        }
        if(!reduced&&clear>0&&clear<1)fizz(heroPx,t,heroTop);
        if(t>.86)glint(heroPx,(t-.86)/.14);
        BUFFER.setRGB(0,0,W,H,PX,0,W);
        var g=(Graphics2D)canvas.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(BUFFER,left,top,W*2,H*2,null);
        g.dispose();
    }

    /** The goo's half-width at height u (0 feet, 1 top) as a share of its base: a soft teardrop with a round tip. */
    private static double profile(double u) {
        if(u<0||u>1)return 0;
        return Math.sqrt(Math.max(0,1-Math.pow(u,3)))*(1-.25*u);
    }
    /** Drips sliding down its flanks as it is pulled up (side -1 left, 1 right): how far one bulges it out at u. */
    private static double drip(double u,double t,int side) {
        double out=0;
        for(int j=0;j<3;j++) {
            double age=(t-.2-j*.05-(side>0?.025:0))/.13;
            if(age<0||age>1)continue;
            double d=(u-(.8-.6*age))/.045;
            out+=Math.max(0,1-d*d)*1.6;
        }
        return out;
    }
    /** His body's outline row by row, the sword and reaching arm trimmed off and the edge smoothed: the shape the goo
     *  draws in to before it flashes. Rows he does not fill close to his middle. */
    private static void body(int[] hl,int[] hr,int[] his,double[] bodyL,double[] bodyR) {
        double[] mids=new double[H];int count=0;
        for(int y=his[0];y<=his[1];y++)if(hr[y]>=hl[y])mids[count++]=(hl[y]+hr[y]+1)/2.;
        Arrays.sort(mids,0,count);
        double middle=count==0?FEET_X:mids[count/2],reach=his[3]/2.*1.3;
        double[] l=new double[H],r=new double[H];
        for(int y=0;y<H;y++) {
            boolean filled=hr[y]>=hl[y];
            l[y]=filled?Math.max(hl[y],middle-reach):middle;
            r[y]=filled?Math.min(hr[y]+1,middle+reach):middle;
        }
        for(int y=0;y<H;y++) {
            double sl=0,sr=0;int n=0;
            for(int d=-2;d<=2;d++)if(y+d>=0&&y+d<H) {sl+=l[y+d];sr+=r[y+d];n++;}
            bodyL[y]=hr[y]>=hl[y]?sl/n:middle;bodyR[y]=hr[y]>=hl[y]?sr/n:middle;
        }
    }
    private static double sway(double u,double time) { return Math.sin(u*3.4-time*11)*Math.max(0,u)*2; }

    /** A pixel of the slime lit from within: on the cyan ramp by its brightness, its palest spots and star going
     *  white, a rim lighting up its upper left; its ink and body keep their tone. */
    private static double ownTone(int c,double glow,boolean litRim) {
        int k=ramp(c);
        return litRim?k+glow*Math.max(0,4-k):k==4?k+glow:k;
    }
    /** The goo's own shading at (x, y): a dark rim with a white top, a soft gloss oval up its left that rides with
     *  the bulges (white at its heart), a lit edge up its left and two shaded pixels down its right. */
    private static int gooTone(int x,int y,int crown,double time) {
        if(y<=crown+1)return 5;
        if(!goo(x-1,y)||!goo(x+1,y)||!goo(x,y-1)||!goo(x,y+1))return 1;
        double l=ROW_L[y],r=ROW_R[y],u=ROW_U[y],v=(x+.5-(l+r)/2)/Math.max(.5,(r-l)/2);
        double ride=.6+.05*Math.sin(time*9),gv=(v+.42)/.3,gu=(u-ride)/.2,gloss=gv*gv+gu*gu;
        if(gloss<1)return gloss<.22?5:4;
        if(!goo(x+2,y)||!goo(x+3,y)&&u<.8)return 2;
        return !goo(x-2,y)&&u>.45&&u<.9?4:3;
    }
    private static boolean goo(int x,int y) { return x>=0&&y>=0&&x<W&&y<H&&GOO[y*W+x]; }

    private static final boolean[] STRAND=new boolean[W*H];
    /** Two thick goo strands swirling up round the goo out of its puddle, beads riding up them, lit along their tops
     *  and shaded under; they wind in tighter and sink into it before it flashes. The halves behind it (front false)
     *  are drawn first and darker. */
    private static void strands(int count,double t,double time,int base,int rows,double in,boolean front) {
        if(rows<4)return;
        Arrays.fill(STRAND,false);
        boolean any=false;
        for(int k=0;k<count;k++) {
            double head=.8*clamp((t-.09-k*.03)/.22),tail=lerp(Math.max(0,head-.7),head,in);
            if(head-tail<.03)continue;
            double step=.4/rows,run=0,lastX=Double.NaN,lastY=0;
            for(double s=tail;s<=head+1e-9;s+=step) {
                double y=base+.5-s*rows;
                int row=Math.max(0,Math.min(H-1,(int)Math.floor(y)));
                double hw=Math.max(0,(ROW_R[row]-ROW_L[row])/2),cx=(ROW_L[row]+ROW_R[row])/2;
                // a screw turned by its own climb, heads ending round the back so the last of each sinks in out of sight
                double a=Math.PI*(1+(k==0?-.2:.2))+Math.PI*2*1.15*(s-head)-time*7*(1-in),along=(s-tail)/(head-tail);
                double x=cx+(hw+(1+3*(1-in))*(1-along*along))*Math.sin(a);   // out round it, in onto it at the head
                if(!Double.isNaN(lastX))run+=Math.hypot(x-lastX,y-lastY);
                lastX=x;lastY=y;
                if(Math.cos(a)>0!=front)continue;
                double radius=lerp(.7,1.8,Math.min(1,along*3))*(1-.35*in);
                if((run+time*60)%9<.6)radius+=.9;                           // a bead riding up it
                if(s+step>head)radius+=.6;                                     // a drop at its head
                stamp(x,y,radius);any=true;
            }
        }
        if(!any)return;
        for(int i=0;i<STRAND.length;i++) {
            if(!STRAND[i])continue;
            boolean top=i<W||!STRAND[i-W],under=i>=W*(H-1)||!STRAND[i+W];
            PX[i]=GOO_TONE[(top?4:under?2:3)-(front?0:1)];
        }
    }
    private static void stamp(double cx,double cy,double radius) {
        for(int y=(int)Math.floor(cy-radius);y<=(int)Math.ceil(cy+radius);y++)for(int x=(int)Math.floor(cx-radius);x<=(int)Math.ceil(cx+radius);x++) {
            if(x<0||y<0||x>=W||y>=H)continue;
            double dx=x+.5-cx,dy=y+.5-cy;
            if(dx*dx+dy*dy<=radius*radius)STRAND[y*W+x]=true;
        }
    }

    /** The goo it rises out of: a puddle round its foot, swelling in as it squats, taken up into it before the flash. */
    private static void puddle(int base,double reach,double in,double swell) {
        double rx=reach*(1-in),ry=Math.min(3.2,rx*.3)*swell;
        if(rx<2||ry<.8)return;
        double cy=base+.5-ry*.35;
        for(int y=(int)Math.floor(cy-ry);y<=(int)Math.ceil(cy+ry);y++)for(int x=(int)Math.floor(FEET_X-rx);x<=(int)Math.ceil(FEET_X+rx);x++) {
            if(!inside(x,y,rx,ry,cy))continue;
            boolean edge=!inside(x-1,y,rx,ry,cy)||!inside(x+1,y,rx,ry,cy)||!inside(x,y-1,rx,ry,cy)||!inside(x,y+1,rx,ry,cy);
            double v=(y+.5-cy)/ry;
            put(x,y,GOO_TONE[edge?(v<0?3:1):v<0?3:2]);
        }
    }
    private static boolean inside(int x,int y,double rx,double ry,double cy) {
        double u=(x+.5-FEET_X)/rx,v=(y+.5-cy)/ry;
        return u*u+v*v<=1;
    }

    /** Bubbles rising inside the goo, white as they near the top. */
    private static void bubbles(double time,int base,int rows,double morph) {
        for(int j=0;j<6;j++) {
            double b=(time*1.4+j/6.)%1;
            int y=(int)Math.round(base-b*rows*.9);
            if(y<0||y>=H||y>base-2)continue;
            double hw=(ROW_R[y]-ROW_L[y])/2;
            if(hw<3)continue;
            int x=(int)Math.floor((ROW_L[y]+ROW_R[y])/2+Math.sin(j*2.3+time*3)*.45*hw);
            if(!goo(x-1,y)||!goo(x+1,y)||!goo(x,y-1)||!goo(x,y+1))continue;
            put(x,y,b>.6?WHITE:RAMP[4]);
            if(morph>.5&&j%2==0&&goo(x+1,y-1))put(x+1,y,RAMP[4]);
        }
    }

    /** Goo flung off its flanks as it stretches, arcing out and falling back. */
    private static void flung(double t,int base,int rows) {
        for(int j=0;j<6;j++) {
            double born=.15+j*.035,age=(t-born)*TransformationAnimation.DURATION;
            if(age<0||age>.15)continue;
            int row=Math.max(0,Math.min(H-1,(int)Math.round(base-(.22+.1*(j%4))*rows)));
            double side=(j&1)==0?-1:1,edge=side<0?ROW_L[row]:ROW_R[row];
            int x=(int)Math.round(edge+side*(1+age*60)),y=(int)Math.round(row-age*55+age*age*900);
            put(x,y,RAMP[4]);put(x,y+1,RAMP[3]);put(x+(side<0?1:-1),y+1,RAMP[2]);
        }
    }

    /** Soft sparkles round the goo: a pixel that swells to a small cross and fades, drifting up. */
    private static void sparkles(double t,int base,int rows) {
        for(int j=0;j<9;j++) {
            double born=.1+j*.034,age=(t-born)/.09;
            if(age<0||age>1)continue;
            int row=Math.max(0,Math.min(H-1,(int)Math.round(base-(.15+.09*((j*5)%9))*rows)));
            double side=(j%2==0?-1:1),edge=side<0?ROW_L[row]:ROW_R[row];
            int x=(int)Math.round(edge+side*(3+(j%3)*2)),y=(int)Math.round(row-age*8);
            boolean peak=age>.3&&age<.65;
            put(x,y,age<.75?WHITE:RAMP[4]);
            if(peak) {put(x-1,y,RAMP[4]);put(x+1,y,RAMP[4]);put(x,y-1,RAMP[4]);put(x,y+1,RAMP[4]);}
        }
    }

    /** Light fizzing off the line as the jelly clears: a few pixels rising from it and dimming. */
    private static void fizz(int[] heroPx,double t,int heroTop) {
        for(int i=0;i<16;i++) {
            double born=.5+i*.024,age=(t-born)*TransformationAnimation.DURATION;
            if(age<0||age>.12)continue;
            int line=(int)Math.round(FEET_Y+1-smooth(clamp((born-.5)/.38))*(FEET_Y+2-heroTop));
            int count=0;
            for(int x=0;x<W;x++)if(line>=0&&line<H&&heroPx[line*W+x]!=0)count++;
            if(count==0)continue;
            int pick=Math.floorMod(i*7919+13,count),x=-1;
            for(int c=0;c<W&&x<0;c++)if(heroPx[line*W+c]!=0&&pick--==0)x=c;
            int y=(int)Math.round(line-age*70);
            put(x+(i%3-1)*((int)(age*30)),y,age<.04?WHITE:age<.08?RAMP[4]:RAMP[3]);
        }
    }

    private static final java.util.Map<Integer,int[]> TIPS=new java.util.HashMap<>();
    /** A four-point glint where the sword ends (the bright pixel below the shoulders farthest out from his middle):
     *  a two-pixel cross with arms tapering to single pixels, up in two substeps, held, then gone before he stands. */
    private static void glint(int[] heroPx,double u) {
        int[] tip=TIPS.computeIfAbsent(Arrays.hashCode(heroPx),key->{
            int best=-1,bx=0,by=0;
            for(int y=HERO_Y+34;y<=FEET_Y;y++)for(int x=0;x<W;x++) {
                int c=heroPx[y*W+x];
                if(c==0||lum(c)<165)continue;
                int out=Math.abs(x-FEET_X);
                if(out>best||out==best&&y>by) {best=out;bx=x;by=y;}
            }
            return best<12?null:new int[]{bx,by};
        });
        if(tip==null)return;
        int arm=(int)Math.round(5*Math.min(1,Math.min(clamp(u)/.15,(1-clamp(u))/.25)));
        if(arm<=0)return;
        int x=tip[0],y=tip[1];
        for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)put(x+dx,y+dy,WHITE);
        for(int s=1;s<=arm;s++) {
            int colour=s<=arm/2+1?WHITE:RAMP[4];
            boolean thick=s<=arm-2;
            put(x-s,y,colour);put(x+1+s,y,colour);put(x,y-s,colour);put(x,y+1+s,colour);
            if(thick) {put(x-s,y+1,colour);put(x+1+s,y+1,colour);put(x+1,y-s,colour);put(x+1,y+1+s,colour);}
        }
    }

    /** His pixel as cyan jelly: its brightness on the slime's ramp a step up, lit on the upper left, deep on the lower right. */
    private static int jelly(int[] heroPx,int x,int y,int c) {
        boolean openLeft=x==0||heroPx[y*W+x-1]==0,openUp=y==0||heroPx[(y-1)*W+x]==0;
        boolean openRight=x==W-1||heroPx[y*W+x+1]==0,openDown=y==H-1||heroPx[(y+1)*W+x]==0;
        if(openLeft||openUp)return 4;
        if(openRight||openDown)return 1;
        return Math.min(4,ramp(c)+1);
    }
    /** Each row's first and last filled pixel (left[y] > right[y] where empty); {top, bottom, widest, median width},
     *  top -1 when it is all empty. */
    private static int[] extent(int[] px,int[] left,int[] right) {
        int top=-1,bottom=-1,widest=0;
        int[] widths=new int[H];int count=0;
        for(int y=0;y<H;y++) {
            left[y]=W;right[y]=-1;
            for(int x=0;x<W;x++)if(px[y*W+x]!=0) {left[y]=Math.min(left[y],x);right[y]=x;}
            if(right[y]<0)continue;
            if(top<0)top=y;
            bottom=y;
            widest=Math.max(widest,right[y]-left[y]+1);
            widths[count++]=right[y]-left[y]+1;
        }
        if(top<0)return new int[]{-1,-1,0,0};
        Arrays.sort(widths,0,count);
        return new int[]{top,bottom,widest,widths[count/2]};
    }
    private static int ramp(int c) { int l=lum(c);return l<40?0:l<80?1:l<135?2:l<200?3:4; }
    private static int lum(int c) { return (int)((c>>16&255)*.30+(c>>8&255)*.59+(c&255)*.11); }
    private static boolean edge(int[] px,int x,int y) {
        return x==0||x==W-1||y==0||y==H-1||px[y*W+x-1]==0||px[y*W+x+1]==0||px[(y-1)*W+x]==0||px[(y+1)*W+x]==0;
    }

    /** A sheet cell copied into buffer space at (ox, oy), mirrored if asked; 0 where it is empty. */
    private static int[] cell(BufferedImage sheet,int sx,int sy,int size,boolean flip,int ox,int oy) {
        int[] out=new int[W*H];
        if(sheet==null)return out;
        for(int y=0;y<size;y++)for(int x=0;x<size;x++) {
            int bx=ox+x,by=oy+y;
            if(bx<0||by<0||bx>=W||by>=H||sx+x>=sheet.getWidth()||sy+y>=sheet.getHeight())continue;
            int c=sheet.getRGB(flip?sx+size-1-x:sx+x,sy+y);
            if(c>>>24!=0)out[by*W+bx]=c|0xff000000;
        }
        return out;
    }
    private static void put(int x,int y,int colour) { if(x>=0&&y>=0&&x<W&&y<H)PX[y*W+x]=colour; }
    private static double clamp(double v) { return Math.max(0,Math.min(1,v)); }
    private static double smooth(double v) { return v*v*(3-2*v); }
    private static double lerp(double a,double b,double t) { return a+(b-a)*t; }
}
