import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

/** The Corrupted Forest: floor baked from the live layout, pieces inside their culling boxes, nothing walls the player in. */
public final class ForestArtTest {
    public static void main(String[] args) throws Exception {
        var area = CampaignWorld.area(1);
        var map = RuinedOutpostMap.campaign(1);

        BufferedImage ground = B2BJ.loadImage("assets/tilesets/forest/forest_ground.png");
        assert ground != null && ground.getWidth() * 2 == area.width() && ground.getHeight() * 2 == area.height()
                : "baked forest floor must cover the whole region at native resolution";
        assert read("assets/tilesets/forest/forest_ground.layout").trim().equals(SetPieces.layoutFingerprint(1))
                : "forest layout changed since the floor was baked: run tools/art/build-art.sh forest";
        BufferedImage tiles = B2BJ.loadImage("assets/tilesets/forest/forest_tileset.png");
        assert tiles != null && tiles.getWidth() == 16 * 16 && tiles.getHeight() % 16 == 0 : "the forest tileset is 16x16 tiles";

        // Every authored variant must survive import; an empty animation frame must not become a variant.
        for (var kind : new SetPieces.Kind[]{SetPieces.Kind.OAK, SetPieces.Kind.BUSH, SetPieces.Kind.LOG, SetPieces.Kind.STUMP}) {
            var sheet = B2BJ.loadImage(kind.sheet());
            int variants = kind == SetPieces.Kind.OAK || kind == SetPieces.Kind.BUSH ? 3 : kind == SetPieces.Kind.LOG ? 2 : 1;
            assert sheet != null && sheet.getWidth() == variants * kind.width : kind + " sheet slicing";
            assert kind.anchorX < kind.width && kind.anchorY < sheet.getHeight() : kind + " ground anchor";
            for (int cell = 0; cell < variants; cell++) {
                int solid = 0, base = 0;
                for (int y = 0; y < sheet.getHeight(); y++) for (int x = 0; x < kind.width; x++) {
                    int alpha = sheet.getRGB(cell * kind.width + x, y) >>> 24;
                    assert alpha == 0 || alpha == 255 : kind + " blurred pixel edges";
                    if (alpha != 0) { solid++; if (Math.abs(y - kind.anchorY) <= 3) base++; }
                }
                assert solid > 100 && base > 0 : kind + " empty variant or detached ground anchor";
            }
        }

        for (String kind : new String[]{"rift_imp", "thorn_wolf", "mire_shaman"}) {     // the forest's own enemies, remnant format
            BufferedImage walk = B2BJ.loadImage("assets/characters/forest/" + kind + "_walk.png");
            BufferedImage attack = B2BJ.loadImage("assets/characters/forest/" + kind + "_attack.png");
            assert walk != null && walk.getWidth() == 4 * 48 && walk.getHeight() == 3 * 48 : kind + " walk sheet: 4 frames x 3 facings";
            assert attack != null && attack.getWidth() == 8 * 48 && attack.getHeight() == 3 * 48 : kind + " attack sheet: 8 frames x 3 facings";
            pixelSheet(walk,48,34);pixelSheet(attack,48,34);
        }

        BufferedImage briar = B2BJ.loadImage("assets/characters/guardian/briarheart_motion.png");
        assert briar != null && briar.getWidth() == 8 * 288 && briar.getHeight() == Guardian.Animation.values().length * 288
                : "Briarheart's motion sheet: 8 frames of every Guardian animation, 288 px cells (the 3x redraw)";
        pixelSheet(briar,288,27);
        wholeFrames(briar,288);
        pixelSheet(B2BJ.loadImage("assets/characters/npcs/sedge_idle.png"),80,20);
        for (String fx : new String[]{"debris", "dust", "crack", "sweep", "flame", "spike", "beam", "mote", "impact"})
            assert B2BJ.loadImage("assets/effects/briar_" + fx + ".png") != null : "missing Briarheart effect " + fx;

        var game = RuinedOutpostGame.campaign();
        for (var piece : SetPieces.pieces(1)) {
            var box = CampaignRenderer.bounds(piece, 0, 0);
            assert box != null : "missing forest sheet for " + piece.kind();
            int pad = 48, w = box.width + pad * 2;
            var view = new BufferedImage(w, box.height + pad * 2, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = ((java.awt.image.DataBufferInt) view.getRaster().getDataBuffer()).getData();
            for (long frame : new long[]{0, 20}) {
                java.util.Arrays.fill(pixels, 0);
                var g = view.createGraphics();
                CampaignRenderer.drawSetPiece(g, piece, game, box.x - pad, box.y - pad, frame);
                g.dispose();
                for (int i = 0; i < pixels.length; i++) {
                    int x = i % w, y = i / w;
                    assert pixels[i] >>> 24 == 0 || x >= pad && y >= pad && x < pad + box.width && y < pad + box.height
                            : piece.kind() + " paints outside its culling box";
                }
            }
        }

        for (var footprint : SetPieces.footprints(1)) {
            assertClear(footprint, area.spawn().x(), area.spawn().y(), 48, "Ashen Trail spawn");
            assertClear(footprint, area.hub().x(), area.hub().y(), 64, "Ferry Refuge camp");
            assertClear(footprint, area.boss().x(), area.boss().y(), 160, "Briarheart");
            assertClear(footprint, area.exit().x(), area.exit().y(), 40, "stair");
            assertClear(footprint, area.returnPortal().x(), area.returnPortal().y(), 48, "way back");
            for (var enemy : area.enemies()) assertClear(footprint, enemy.x(), enemy.y(), 40, "enemy spawn");
        }
        assert !map.isBlocked(area.spawn().x(), area.spawn().y() + 24, 24) : "blocked spawn";

        map.clearGuardian();
        map.openGate();
        boolean[][] seen = flood(map, area.spawn().x(), area.spawn().y());
        for (var landmark : area.landmarks()) assert reachable(seen, landmark.x(), landmark.y()) : "cannot reach " + landmark.name();
        assert reachable(seen, area.exit().x(), area.exit().y()) : "cannot reach the stair";
        assert reachable(seen, area.returnPortal().x(), area.returnPortal().y()) : "cannot reach the way back to the Outpost";
        for (var enemy : area.enemies()) assert reachable(seen, enemy.x(), enemy.y()) : "an enemy stands where no one can reach";
        System.out.println("ForestArtTest passed");
    }

    private static void assertClear(RuinedOutpostMap.Obstacle o, double x, double y, double radius, String what) {
        double dx = Math.max(0, Math.abs(x - o.centerX()) - o.width() / 2), dy = Math.max(0, Math.abs(y - o.centerY()) - o.height() / 2);
        assert Math.hypot(dx, dy) >= radius : "forest piece at " + o.centerX() + "," + o.centerY() + " crowds the " + what;
    }

    private static void pixelSheet(BufferedImage image,int size,int paletteLimit) {
        assert image!=null&&image.getWidth()%size==0&&image.getHeight()%size==0;
        var colors=new java.util.HashSet<Integer>();
        for(int cy=0;cy<image.getHeight();cy+=size)for(int cx=0;cx<image.getWidth();cx+=size) {
            int solid=0;
            for(int y=cy;y<cy+size;y++)for(int x=cx;x<cx+size;x++) {
                int rgb=image.getRGB(x,y),alpha=rgb>>>24;
                assert alpha==0||alpha==255 : "sprite has blurred edges";
                if(alpha==255){colors.add(rgb);solid++;}
            }
            assert solid>40 : "empty animation cell";
        }
        assert colors.size()<=paletteLimit : "uncontrolled sprite palette: "+colors.size();
    }

    /** 2026-10-29, the ground showed through Briarheart: every frame is one unbroken body, with no transparent seam
     *  across it (the breathing frames split at the chest) and no straight edge where a pose was cut at the grid of
     *  its generated source (the crouch's roots, the raised leaves, the wide claws). */
    private static void wholeFrames(BufferedImage sheet,int cell) {
        for(int top=0;top<sheet.getHeight();top+=cell)for(int left=0;left<sheet.getWidth();left+=cell) {
            String frame="Briarheart frame "+left/cell+" of row "+top/cell;
            int minX=cell,maxX=-1,maxY=-1;
            for(int y=0;y<cell;y++)for(int x=0;x<cell;x++)if(opaque(sheet,left+x,top+y)) {
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
            }
            for(int y=1;y<cell-2;y++) {
                int seam=0;
                for(int x=0;x<cell;x++)if(!opaque(sheet,left+x,top+y)&&opaque(sheet,left+x,top+y-1)
                        &&(opaque(sheet,left+x,top+y+1)||opaque(sheet,left+x,top+y+2)))seam++;
                assert seam<24 : frame+" splits across row "+y;
            }
            assert run(sheet,left,top+maxY,1,0,cell)<48 : frame+" ends in a straight cut along its base";
            assert run(sheet,left+minX,top,0,1,cell)<24&&run(sheet,left+maxX,top,0,1,cell)<24 : frame+" is cut straight at a side";
        }
    }
    /** The longest opaque run along a line of the sheet. */
    private static int run(BufferedImage sheet,int x,int y,int dx,int dy,int length) {
        int best=0,run=0;
        for(int i=0;i<length;i++) {run=opaque(sheet,x+i*dx,y+i*dy)?run+1:0;best=Math.max(best,run);}
        return best;
    }
    private static boolean opaque(BufferedImage image,int x,int y) { return y<image.getHeight()&&image.getRGB(x,y)>>>24>=128; }

    private static boolean[][] flood(RuinedOutpostMap map, double sx, double sy) {
        int w = map.worldWidth() / 16, h = map.worldHeight() / 16;
        boolean[][] seen = new boolean[h + 1][w + 1];
        var queue = new ArrayDeque<int[]>();
        int x0 = (int) (sx / 16), y0 = (int) (sy / 16);
        seen[y0][x0] = true; queue.add(new int[]{x0, y0});
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = c[0] + d[0], y = c[1] + d[1];
                if (x < 0 || y < 0 || x > w || y > h || seen[y][x] || map.isBlocked(x * 16, y * 16 + 24, 24)) continue;
                seen[y][x] = true; queue.add(new int[]{x, y});
            }
        }
        return seen;
    }

    private static boolean reachable(boolean[][] seen, double x, double y) {
        for (int gy = 0; gy < seen.length; gy++) for (int gx = 0; gx < seen[0].length; gx++)
            if (seen[gy][gx] && Math.hypot(gx * 16 - x, gy * 16 - y) < 120) return true;
        return false;
    }

    private static String read(String path) throws Exception {
        try (InputStream in = B2BJ.class.getResourceAsStream("/" + path)) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return java.nio.file.Files.readString(java.nio.file.Path.of(path));
    }
}
