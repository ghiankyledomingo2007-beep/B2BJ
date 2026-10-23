/**
 * Building blocks for 3/4-view structures. Convention: a solid stands on a footprint whose front
 * edge is the sprite row `ground`; its front face rises from there, its top face sits above that.
 * Moonlight comes from the upper left.
 */
final class Kit {
    private Kit() { }

    /** Coursed stone: front face of blocks plus a weathered top face of `depth` rows. */
    static void stoneBox(Canvas c, int x0, int ground, int w, int depth, int height, int seed, boolean mossy) {
        for (int x = x0; x < x0 + w; x++) {
            for (int y = ground - height + 1; y <= ground; y++) {
                int fromGround = ground - y, course = fromGround / 9, inCourse = fromGround % 9;
                int shiftX = x - x0 + (course % 2) * 7;
                int joint = Math.floorMod(shiftX, 14);
                int blockId = shiftX / 14 * 31 + course;
                int tone = Noise.rnd(blockId, seed, 1) < .3 ? 3 : 4;
                int colour = inCourse == 8 || joint == 0 ? Pal.STONE[1] : inCourse == 7 ? Pal.STONE[tone + 1]
                        : inCourse <= 1 ? Pal.STONE[tone - 1] : joint == 1 ? Pal.STONE[tone + 1] : joint == 13 ? Pal.STONE[tone - 1]
                        : Noise.value(x, y, 3, seed + 9) > .74 ? Pal.STONE[tone + 1] : Pal.STONE[tone];
                if (x == x0 && colour != Pal.STONE[1]) colour = Pal.shift(colour, 1);
                if (x == x0 + w - 1 && colour != Pal.STONE[1]) colour = Pal.shift(colour, -1);
                c.set(x, y, colour);
            }
            for (int r = 0; r < depth; r++) {
                int y = ground - height - depth + 1 + r;
                int colour = r == 0 ? Pal.STONE[6] : r == depth - 1 ? Pal.STONE[4] : Pal.STONE[5];
                if (mossy && r > 0 && Noise.value(x, y, 4, seed + 2) > .6) colour = Pal.GRASS[Noise.value(x, y, 2, seed + 3) > .5 ? 3 : 2];
                c.set(x, y, colour);
            }
        }
    }

    /** Vertical timber planks with a frame beam top and bottom. */
    static void plankWall(Canvas c, int x0, int top, int w, int bottom, int seed) {
        for (int x = x0; x < x0 + w; x++) {
            int plank = (x - x0) / 6, inPlank = (x - x0) % 6;
            int tone = Noise.rnd(plank, seed, 5) < .35 ? 2 : 3;
            for (int y = top; y <= bottom; y++) {
                int colour = inPlank == 0 ? Pal.WOOD[1] : inPlank == 1 ? Pal.WOOD[tone + 1] : inPlank == 5 ? Pal.WOOD[tone - 1] : Pal.WOOD[tone];
                if (Noise.rnd(x, y / 3, seed + 6) < .05) colour = Pal.shift(colour, -1);            // grain
                if (bottom - y < 10 && Noise.rnd(x, y, seed + 7) < (10 - (bottom - y)) / 25.0) colour = Pal.shift(colour, -1); // rot at the foot
                if (y <= top + 2) colour = y == top ? Pal.WOOD[4] : Pal.WOOD[2];
                if (y >= bottom - 2) colour = y == bottom - 2 ? Pal.WOOD[3] : Pal.WOOD[1];
                c.set(x, y, colour);
            }
        }
    }

    /**
     * Front roof slope of overlapping wooden shingles, eave at the bottom, ridge at the top. Each row
     * casts a shadow on the one below, seams show only where a shingle is exposed, and the slope
     * darkens toward the eave where the rain runs off.
     */
    static void shingleRoof(Canvas c, int x0, int w, int ridgeY, int eaveY, int seed) {
        int rowH = 6;
        for (int y = ridgeY; y <= eaveY; y++) {
            int row = (y - ridgeY) / rowH, inRow = (y - ridgeY) % rowH;
            double down = (y - ridgeY) / (double) Math.max(1, eaveY - ridgeY);
            int offset = (int) (Noise.rnd(row, 0, seed + 3) * 9);
            int shingle = 0, start = 0, next = 7 + (int) (Noise.rnd(row, 0, seed) * 5);
            for (int x = x0; x < x0 + w; x++) {
                int local = x - x0 + offset;
                while (local >= next) { shingle++; start = next; next += 7 + (int) (Noise.rnd(row, shingle, seed) * 5); }
                int tone = Noise.rnd(shingle, row, seed + 1) < .3 ? 2 : 3;
                if (Noise.rnd(shingle, row, seed + 2) < .07) tone = 1;
                if (down > .7 && tone > 2) tone--;
                int colour = inRow == 0 ? Pal.WOOD[0] : inRow == 1 ? Pal.WOOD[tone + 1] : inRow == rowH - 1 ? Pal.WOOD[tone - 1] : Pal.WOOD[tone];
                if (local == start && inRow >= 2) colour = Pal.WOOD[0];                       // seam where exposed
                if (local == start + 1 && inRow >= 2) colour = Pal.WOOD[Math.min(5, tone + 1)];
                double moss = Noise.value(x, y, 8, seed + 4);
                if (moss > .63 && inRow > 0) colour = inRow == 1 ? Pal.GRASS[moss > .73 ? 4 : 3] : Pal.GRASS[moss > .72 ? 2 : 1];
                if (y == ridgeY) colour = Pal.WOOD[5];
                if (y == ridgeY + 1) colour = Pal.WOOD[3];
                c.set(x, y, colour);
            }
        }
        for (int x = x0 - 1; x <= x0 + w; x++) { c.set(x, eaveY + 1, Pal.WOOD[1]); c.set(x, eaveY + 2, Pal.WOOD[0]); }
    }

    /** Upright cylinder lit from the left: tower, post or drum. */
    static void cylinder(Canvas c, int cx, int ground, int radius, int height, int[] ramp, int lightIdx, int seed) {
        for (int x = cx - radius; x < cx + radius; x++) {
            double u = (x + .5 - cx) / radius;                 // -1 left .. 1 right
            int idx = lightIdx + (u < -.55 ? 0 : u < -.1 ? 1 : u < .45 ? 0 : u < .8 ? -1 : -2);
            idx = Math.max(0, Math.min(ramp.length - 1, idx));
            int bulge = (int) Math.round(Math.sqrt(Math.max(0, 1 - u * u)) * 2);
            for (int y = ground - height + 1; y <= ground + bulge; y++) c.set(x, y, ramp[idx]);
        }
    }

    /** Filled polygon in one colour. */
    static void poly(Canvas c, int colour, int... xy) {
        int n = xy.length / 2;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) { xs[i] = xy[i * 2]; ys[i] = xy[i * 2 + 1]; }
        c.poly(xs, ys, colour);
    }

    /** A rope or thin line, one pixel. */
    static void rope(Canvas c, int x0, int y0, int x1, int y1, int colour) { c.line(x0, y0, x1, y1, colour); }

    // ------------------------------------------------------------------ fire
    /** Flat colour clusters keep the camp flame readable at the world's native pixel scale. */
    static Canvas campfire(int frame) {
        Canvas c=new Canvas(30,40);
        int f=Math.floorMod(frame,8);
        int lean=new int[]{-1,-1,0,1,2,1,0,-1}[f];
        int rise=new int[]{0,1,2,1,0,-1,-2,-1}[f];
        poly(c,Pal.FIRE[1],3,37,3,31,6,27,5,21-rise,10,26,12,19,11+lean,11,
                16+lean,6+rise,17+lean,15,21,21,22,27,25,22+rise,25,30,27,34,26,38);
        poly(c,Pal.FIRE[2],5,36,6,30,10,33,9,27,13,22,15+lean,13+rise,
                19,23,21,31,23,28,25,34,24,37);
        poly(c,Pal.FIRE[3],8,36,8,32,12,27,13,30,16+lean,22+rise,18,28,20,32,22,35,21,37);
        poly(c,Pal.FIRE[4],11,37,11,33,14+lean,27+rise,16,32,18,30,20,36,19,38);
        poly(c,Pal.FIRE[5],13,37,13,34,15,31+rise,17,34,18,37);
        if(f<3)c.rect(22,9-f*3,2,2,Pal.FIRE[3]);
        if(f>=4&&f<7)c.rect(7,12-(f-4)*3,2,2,Pal.FIRE[4]);
        return c;
    }

    /** Broad flat tongues, a curling tip and detached embers; no heat-map dithering. */
    static Canvas fire(int w,int h,int frame) {
        int f=Math.floorMod(frame,8),lean=new int[]{-1,-1,0,1,2,1,0,-1}[f];
        int rise=new int[]{0,1,2,1,0,-1,-2,-1}[f];
        Canvas source=new Canvas(14,24);
        poly(source,Pal.FIRE[1],2,22,1,17,3,12,4,16,6,11,6+lean,5+rise,
                9+lean,8+rise,9,13,12,15,13,20,11,23,4,23);
        poly(source,Pal.FIRE[2],3,21,3,17,5,14,6,17,8+lean,9+rise,9,15,11,18,11,22);
        poly(source,Pal.FIRE[3],4,22,4,19,6,16,7,18,9,14+rise,10,19,10,22);
        poly(source,Pal.FIRE[4],5,22,5,20,7,17+rise,8,20,9,22);
        source.rect(6,21,2,2,Pal.FIRE[5]);
        if(f<3)source.rect(10,5-f*2,2,2,Pal.FIRE[3]);
        if(f>=4&&f<7)source.rect(3,7-(f-4)*2,2,2,Pal.FIRE[4]);
        Canvas result=new Canvas(w,h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)result.set(x,y,source.get(x*14/w,y*24/h));
        return result;
    }
}
