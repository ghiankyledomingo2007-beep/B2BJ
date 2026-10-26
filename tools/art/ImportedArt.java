import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;

/** Native-size imports; generated masters stay outside the game's packaged asset folders. */
final class ImportedArt {
    private ImportedArt() { }

    static void reincarnation() {
        var source=Canvas.load("assets/source/characters/npcs/tensura-cast.generated.png").image();
        int[][] palettes={
            {0x171820,0x34303b,0x655361,0x9b7c83,0xd2aca7,0xf2d8bd,0xffebd3,0x652f43,0x9e4454,0xcc6b7d,0xe8a1ad,0xf2c5ca,0x9f9caa,0xd5d3d7,0xf4efe6},
            {0x171820,0x302622,0x49362a,0x70513a,0x9d7049,0xc48e5c,0xe3b583,0xeee0ba,0x785d43,0xa18a69,0xc7b998,0x4b2d35,0x754346,0xa46356,0x5c6266},
            {0x171820,0x273344,0x3f5470,0x647998,0x9daabd,0xeee0ba,0xc7b998,0xe3b583,0xc48e5c,0x9d7049,0x70513a,0x49362a,0x9b8952,0xc5ae69,0xe8d194},
            {0x171820,0x302a22,0x4a3d2a,0x70563b,0x99734e,0xc6a077,0xe3c49e,0xeee0ba,0x344327,0x536039,0x7b854b,0xa5ad68,0xc5cb9e,0x7e8282,0xb6b7af},
            {0x141925,0x25334b,0x384b68,0x536c8a,0x8094ad,0xa4b2c4,0x4c4b57,0x777482,0xaaa4ac,0xd4cdd1,0xeee8de,0x70513a,0xb3835f,0xe3b583,0xffe1be}};
        int[] rows={0,source.getHeight()*40/100,source.getHeight()*75/100,source.getHeight()};
        var faces=new Canvas(192,64);var people=new Canvas(192,128);
        for(int col=0;col<5;col++) {
            int x=col*source.getWidth()/5,w=(col+1)*source.getWidth()/5-x;
            var closeup=trim(source.getSubimage(x,rows[2],w,rows[3]-rows[2]));
            int square=Math.min(closeup.getWidth(),closeup.getHeight());
            var portraits=fit(closeup.getSubimage((closeup.getWidth()-square)/2,0,square,square),64,64,palettes[col]);
            if(col==0)portraits.save("assets/characters/npcs/mara_portrait.png");
            else if(col==4)portraits.save("assets/characters/npcs/sedge_portrait.png");
            else faces.blit(portraits,(col-1)*64,0);
            var poses=new Canvas[2];
            for(int row=0;row<2;row++) {
                var crop=trim(source.getSubimage(x,rows[row],w,rows[row+1]-rows[row]));
                if(col==0||col==4) {
                    poses[row]=new Canvas(80,80);poses[row].blit(fit(crop,48,70,palettes[col]),16,5);
                } else {
                    var body=fit(crop,60,row==0?52:50,palettes[col]);int bottom=0;
                    for(int py=0;py<body.h;py++)for(int px=0;px<body.w;px++)if(body.opaque(px,py))bottom=Math.max(bottom,py);
                    people.blit(body,(col-1)*64+2,row*64+63-bottom);
                }
            }
            if(col==0)poses[0].save("assets/characters/npcs/mara_idle.png");
            if(col==4)Canvas.strip(poses).save("assets/characters/npcs/sedge_idle.png");
        }
        faces.save("assets/characters/npcs/survivor_portraits.png");people.save("assets/props/outpost/field_survivor.png");
        var road=Canvas.load("assets/source/ui/opening/last-attempt.generated.png").image();
        var nativeRoad=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);var g=nativeRoad.createGraphics();Ui.crisp(g);
        g.drawImage(road,0,0,640,360,null);g.dispose();var packed=new Canvas(640,360);nativeRoad.getRGB(0,0,640,360,packed.px,0,640);packed.save("assets/ui/opening/last-attempt.png");
    }

    static void sedge() {
        var source=Canvas.load("assets/source/characters/npcs/sedge.generated.png").image();
        if(source.getWidth()!=2172||source.getHeight()!=724)throw new IllegalStateException("Unexpected Sedge source");
        int[] palette={0x171a20,0x263031,0x3a4543,0x53645f,0x72867a,0x9ca995,
                0x30251c,0x493525,0x70513a,0x9d7049,0xc48e5c,0xe3b583,0xeee0ba,
                0x293b48,0x425b69,0x668895,0x9aacad,0xc7bd97,0x9a9270};
        var bodies=new Canvas[2];
        for(int i=0;i<3;i++) {
            var crop=trim(source.getSubimage(i*724,0,724,724));
            if(i==2)fit(crop,64,64,palette).save("assets/characters/npcs/sedge_portrait.png");
            else {bodies[i]=new Canvas(80,80);bodies[i].blit(fit(crop,48,70,palette),16,5);}
        }
        Canvas.strip(bodies).save("assets/characters/npcs/sedge_idle.png");
    }

    static void forestCreatures() {
        var source=Canvas.load("assets/source/characters/forest/forest-creatures-keyposes.generated.png").image();
        if(source.getWidth()!=1774||source.getHeight()!=887)throw new IllegalStateException("Unexpected forest creature source");
        int[] palette={0x141720,0x24242e,0x37323c,0x524852,0x716778,0x9a8c9d,
                0x302137,0x483052,0x67406e,0x895497,0xb17bc1,
                0x242f2a,0x3b4732,0x535d38,0x707c48,0x929b60,0xb2b27a,
                0x3c403c,0x5a6057,0x7d8372,0xa3aa91,0xc6c9ae,0xe7e2c5,
                0x30251c,0x493525,0x70513a,0x9d7049,0xc48e5c,0xe3b583,
                0x815227,0xc28531,0xe9b64d,0xffdf83};
        var parts=new BufferedImage[3][6];
        // Connected silhouettes can cross a nominal row gutter (the shaman's raised staff).
        for(int col=0;col<6;col++) {
            int ox=col*source.getWidth()/6,w=(col+1)*source.getWidth()/6-ox,h=source.getHeight();
            var masks=new BufferedImage[]{new BufferedImage(w,h,2),new BufferedImage(w,h,2),new BufferedImage(w,h,2)};
            boolean[] seen=new boolean[w*h];int[] queue=new int[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int first=y*w+x;if(seen[first]||source.getRGB(ox+x,y)>>>24<128)continue;
                int tail=1;queue[0]=first;seen[first]=true;long sumY=0;
                for(int head=0;head<tail;head++) {
                    int at=queue[head],ax=at%w,ay=at/w;sumY+=ay;
                    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                        int nx=ax+dx,ny=ay+dy;if(nx<0||nx>=w||ny<0||ny>=h)continue;
                        int n=ny*w+nx;if(seen[n]||source.getRGB(ox+nx,ny)>>>24<128)continue;
                        seen[n]=true;queue[tail++]=n;
                    }
                }
                if(tail<6)continue;
                int row=Math.min(2,(int)(sumY*3/tail/h));
                for(int i=0;i<tail;i++){int ax=queue[i]%w,ay=queue[i]/w;masks[row].setRGB(ax,ay,source.getRGB(ox+ax,ay));}
            }
            for(int row=0;row<3;row++)parts[row][col]=trim(masks[row]);
        }
        String[] names={"rift_imp","thorn_wolf","mire_shaman"};
        for(int kind=0;kind<3;kind++) {
            double scale=1;
            for(var part:parts[kind])scale=Math.min(scale,Math.min(42.0/part.getWidth(),42.0/part.getHeight()));
            var poses=new Canvas[6];
            for(int pose=0;pose<6;pose++) {
                int w=Math.max(1,(int)Math.round(parts[kind][pose].getWidth()*scale)),h=Math.max(1,(int)Math.round(parts[kind][pose].getHeight()*scale));
                poses[pose]=new Canvas(48,48);poses[pose].blit(fit(parts[kind][pose],w,h,palette),(48-w)/2,46-h);
            }
            Canvas.strip(poses).save("assets/characters/forest/"+names[kind]+"_poses.png");
            for(boolean attack:new boolean[]{false,true}) {
                int frames=attack?8:4;var sheet=new Canvas(frames*48,3*48);
                for(int direction=0;direction<3;direction++)for(int frame=0;frame<frames;frame++) {
                    var pose=poses[direction+(attack&&frame>=4&&frame<=6?3:0)];
                    for(int y=0;y<48;y++)for(int x=0;x<48;x++)if(pose.opaque(x,y)) {
                        int dy=attack?(frame==2||frame==3?1:0):(frame==1||frame==3?-1:0);
                        int dx=!attack&&y>=38?(x<24?1:-1)*(frame==1?1:frame==3?-1:0):0;
                        sheet.set(frame*48+x+dx,direction*48+y+(y<38?dy:0),pose.get(x,y));
                    }
                }
                sheet.save("assets/characters/forest/"+names[kind]+(attack?"_attack.png":"_walk.png"));
            }
        }
    }

    private static BufferedImage trim(BufferedImage source) {
        int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if(source.getRGB(x,y)>>>24>=128) {
            left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
        }
        if(right<left)throw new IllegalStateException("Empty source sprite");
        return source.getSubimage(left,top,right-left+1,bottom-top+1);
    }

    static Canvas briarheart() {
        var source=Canvas.load("assets/source/characters/forest/briarheart-keyposes.generated.png").image();
        if(source.getWidth()!=1774||source.getHeight()!=887)throw new IllegalStateException("Unexpected Briarheart source dimensions");
        int[] colours={0x101b20,0x1d2929,0x293b37,0x3d5147,0x56665a,0x7b8771,
                0x201f23,0x30272b,0x44332e,0x5c4536,0x77583f,0x957450,0xb49665,0xd0b77d,
                0x261f35,0x3b2d4a,0x573b61,0x785077,0xa07492,
                0x592d20,0x925023,0xc47929,0xe5a53a,0xf6cc62,0xffe694,0xfff3ca};
        var crops=new BufferedImage[8];double scale=1;
        for(int cell=0;cell<8;cell++) {
            int col=cell%4,row=cell/4,ox=col*source.getWidth()/4,oy=row*source.getHeight()/2;
            int cw=(col+1)*source.getWidth()/4-ox,ch=(row+1)*source.getHeight()/2-oy;
            int left=cw,top=ch,right=-1,bottom=-1;
            for(int y=0;y<ch;y++)for(int x=0;x<cw;x++)if(source.getRGB(ox+x,oy+y)>>>24>=128) {
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
            if(right<left)throw new IllegalStateException("Empty Briarheart key pose "+cell);
            crops[cell]=source.getSubimage(ox+left,oy+top,right-left+1,bottom-top+1);
            scale=Math.min(scale,Math.min(88.0/crops[cell].getWidth(),80.0/crops[cell].getHeight()));
        }
        var poses=new Canvas[8];
        for(int i=0;i<8;i++) {
            int w=Math.max(1,(int)Math.round(crops[i].getWidth()*scale)),h=Math.max(1,(int)Math.round(crops[i].getHeight()*scale));
            poses[i]=new Canvas(96,96);poses[i].blit(fit(crops[i],w,h,colours),(96-w)/2,85-h);
        }
        Canvas.strip(poses).save("assets/characters/guardian/briarheart_poses.png");
        int[][] sequence={{0,0,0,0,0,0,0,0},{0,0,1,1,1,1,0,0},{0,0,1,1,1,3,3,3},
                {3,3,2,2,2,1,1,0},{4,4,4,4,4,4,4,4},{1,3,3,6,6,2,2,0},{6,6,4,7,7,7,7,7}};
        var sheet=new Canvas(8*96,7*96);
        for(int row=0;row<7;row++)for(int frame=0;frame<8;frame++) {
            var pose=poses[sequence[row][frame]];
            int breath=(row==0||row==1||row==4)&&(frame==2||frame==3||frame==4)?-1:0;
            for(int y=0;y<96;y++)for(int x=0;x<96;x++)if(pose.opaque(x,y))
                sheet.set(frame*96+x,row*96+y+(y<65?breath:0),pose.get(x,y));
        }
        return sheet;
    }

    static Canvas survivors() {
        var source=Canvas.load("assets/source/characters/npcs/survivors-mara-style.generated.png").image();
        if(source.getWidth()!=1536||source.getHeight()!=1024)throw new IllegalStateException("Unexpected survivor source dimensions");
        int[] palette={0x171a18,0x30251c,0x493525,0x70513a,0x9d7049,0xc48e5c,0xe3b583,
                0xeee0ba,0xc7bd97,0x9a9270,0x6c684a,0x3c4328,0x596034,0x7a804d,0x999d68,
                0x293b48,0x425b69,0x668895,0x9aacad,0x4f2c2f,0x754346,0xa46356,0xc48a6e,
                0x3f4241,0x69736b,0xa1aaa1,0xd5d6bc};
        var sheet=new Canvas(192,128);
        for(int row=0;row<2;row++)for(int col=0;col<3;col++) {
            int left=1536,top=1024,right=-1,bottom=-1;
            // The generated top row ends at523; split in the transparent gap, not through its boots.
            for(int y=row==0?0:536;y<(row==0?536:1024);y++)for(int x=col*512;x<(col+1)*512;x++)
                if(source.getRGB(x,y)>>>24>=128) {
                    left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
                }
            if(right<left)throw new IllegalStateException("Empty survivor source cell "+col+","+row);
            var body=fit(source.getSubimage(left,top,right-left+1,bottom-top+1),52,50,palette);
            sheet.blit(body,col*64+6,row*64+14);
        }
        return sheet;
    }

    static void portraits() {
        int[] mara={0x171a18,0x30251c,0x493525,0x70513a,0x9d7049,0xc48e5c,0xe3b583,
                0xeee0ba,0xc7bd97,0x9a9270,0x6c684a,0x3c4328,0x596034,0x7a804d,0x999d68};
        var sheet=Canvas.load("assets/source/characters/mara_sheet.generated.png").image();
        fit(sheet.getSubimage(0,0,sheet.getHeight(),sheet.getHeight()),64,64,mara)
                .save("assets/characters/npcs/mara_portrait.png");
        fit(Canvas.load("assets/source/characters/mara_idle.generated.png").image(),80,80,mara)
                .save("assets/characters/npcs/mara_idle.png");
        var idle=Canvas.load("assets/characters/slime/slime_idle.png");
        var colours=new LinkedHashSet<Integer>();
        for(int pixel:idle.px)if(pixel>>>24!=0)colours.add(pixel&0xffffff);
        fit(Canvas.load("assets/source/characters/rainoray_portrait.generated.png").image(),64,64,
                colours.stream().mapToInt(Integer::intValue).toArray())
                .save("assets/characters/slime/rainoray_portrait.png");
    }

    static Canvas tent() {
        int[] colours={0x17161a,0x292421,0x42382d,0x62533d,0x806e52,0xa58d68,0xc2aa80,0xddc293,
                0xefdab0,0x553133,0x803c3e,0xa34e50,0xc96d6a,0x6d472d,0x9c6c3e,0xc49355};
        return fit(Canvas.load("assets/source/outpost/tent.generated.png").image(),128,100,colours);
    }

    static Canvas dummy() {
        var source=Canvas.load("assets/source/outpost/dummy.generated.png").image();
        int[] colours={0x191719,0x30251d,0x493523,0x654831,0x835b39,0xa67a49,0xc69a5a,0xe4b96e,
                0xf1d292,0x514735,0x6e6147,0x8b7b56,0xaa956a,0xc7b182,0xe4cba1,0xf2dfb5};
        Canvas[] frames=new Canvas[2];
        for(int f=0;f<2;f++) {
            int cell=source.getWidth()/2,left=cell,top=source.getHeight(),right=-1,bottom=-1;
            for(int y=0;y<source.getHeight();y++)for(int x=0;x<cell;x++)if(source.getRGB(f*cell+x,y)>>>24>=128) {
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
            if(right<left)throw new IllegalStateException("empty dummy source cell "+f);
            var body=fit(source.getSubimage(f*cell+left,top,right-left+1,bottom-top+1),32,64,colours);
            int foot=0;
            for(int y=0;y<body.h;y++)for(int x=0;x<body.w;x++)if(body.opaque(x,y))foot=y;
            frames[f]=new Canvas(36,72);frames[f].blit(body,2,69-foot);
        }
        return Canvas.strip(frames);
    }

    /** Preserve the group's authored pixels; export only the complete static log frame. */
    static Canvas forest(SetPieces.Kind kind) {
        String name = switch (kind) {
            case OAK -> "oak";
            case BUSH -> "bush";
            case LOG -> "log";
            case STUMP -> "stump";
            default -> throw new IllegalArgumentException("no groupmate source for " + kind);
        };
        var source = Canvas.load("assets/source/forest/groupmate-2026-10-25/" + name + ".png").image();
        int width = switch (kind) { case OAK -> 348; case BUSH -> 156; case LOG -> 208; default -> 64; };
        int height = switch (kind) { case OAK -> 162; case BUSH -> 34; case LOG -> 28; default -> 64; };
        if (source.getWidth() != width || source.getHeight() != height)
            throw new IllegalArgumentException("unexpected groupmate source dimensions: " + name);
        // Bright lime grass must stay foliage instead of snapping to pale bone highlights.
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = source.getRGB(x, y), green = pixel >> 8 & 255;
            if (pixel >>> 24 != 0 && green > (pixel >> 16 & 255) + 4 && green > (pixel & 255) + 4)
                source.setRGB(x, y, Pal.c(nearest(pixel, Pal.GRASS)));
        }
        int[] palette = java.util.stream.Stream.of(Pal.GRASS, Pal.WOOD, Pal.EARTH, Pal.CRIMSON, Pal.BONE, Pal.DRY)
                .flatMapToInt(java.util.Arrays::stream).toArray();
        return fit(source, kind == SetPieces.Kind.STUMP ? kind.width : width,
                kind == SetPieces.Kind.STUMP ? 48 : height, palette);
    }

    private static Canvas fit(BufferedImage source,int targetWidth,int targetHeight,int[] palette) {
        double scale=Math.min((double)targetWidth/source.getWidth(),(double)targetHeight/source.getHeight());
        int width=(int)Math.round(source.getWidth()*scale),height=(int)Math.round(source.getHeight()*scale);
        var image=new BufferedImage(targetWidth,targetHeight,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();Ui.crisp(g);
        g.drawImage(source,(targetWidth-width)/2,(targetHeight-height)/2,width,height,null);g.dispose();
        var result=new Canvas(targetWidth,targetHeight);
        for(int y=0;y<targetHeight;y++)for(int x=0;x<targetWidth;x++) {
            int pixel=image.getRGB(x,y);
            if(pixel>>>24<128)continue;
            result.set(x,y,Pal.c(nearest(pixel,palette)));
        }
        return result;
    }

    private static int nearest(int pixel,int[] palette) {
        int best=palette[0];double distance=Double.MAX_VALUE;
        for(int colour:palette) {
            int r=(pixel>>16&255)-(colour>>16&255),green=(pixel>>8&255)-(colour>>8&255),b=(pixel&255)-(colour&255);
            double d=.3*r*r+.59*green*green+.11*b*b;
            if(d<distance){distance=d;best=colour;}
        }
        return best;
    }
}
