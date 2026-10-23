import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

/** The revamped Outpost art: baked floor in sync with the map, sheets present, set pieces never trap the player. */
public final class OutpostArtTest {
    public static void main(String[] args) throws Exception {
        var area = CampaignWorld.area(0);
        var map = RuinedOutpostMap.campaign(0);

        BufferedImage ground = B2BJ.loadImage("assets/tilesets/outpost/outpost_ground.png");
        assert ground != null && ground.getWidth() * 2 == area.width() && ground.getHeight() * 2 == area.height()
                : "baked floor must cover the whole Outpost at native resolution";
        String baked = read("assets/tilesets/outpost/outpost_ground.layout").trim();
        assert baked.equals(SetPieces.layoutFingerprint(0))
                : "map layout changed since the floor was baked: run tools/art/build-art.sh ground";

        for (var kind : SetPieces.Kind.values()) {
            BufferedImage sheet = B2BJ.loadImage(kind.sheet());
            assert sheet != null : "missing set piece sheet " + kind;
            assert sheet.getWidth() % kind.width == 0 && sheet.getHeight() >= kind.anchorY : "bad cells in " + kind;
        }
        for (String[] prop : new String[][]{{"ruin_wall", "96"}, {"stake_wall", "96"}, {"set_supplies", "56"}}) {
            BufferedImage sheet = B2BJ.loadImage("assets/props/outpost/" + prop[0] + ".png");
            assert sheet != null && sheet.getWidth() % Integer.parseInt(prop[1]) == 0 : "bad prop sheet " + prop[0];
        }

        // The renderer culls on CampaignRenderer.bounds: every pixel a piece paints, fire and animation included, lies inside it.
        var game = RuinedOutpostGame.campaign();
        for (var piece : SetPieces.pieces(0)) {
            var box = CampaignRenderer.bounds(piece, 0, 0);
            int pad = 64, w = box.width + pad * 2;
            var view = new BufferedImage(w, box.height + pad * 2, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = ((java.awt.image.DataBufferInt) view.getRaster().getDataBuffer()).getData();
            for (long frame = 0; frame <= 40; frame += 5) {
                java.util.Arrays.fill(pixels, 0);
                var g = view.createGraphics();
                CampaignRenderer.drawSetPiece(g, piece, game, box.x - pad, box.y - pad, frame);
                g.dispose();
                for (int i = 0; i < pixels.length; i++) {
                    int x = i % w, y = i / w;
                    assert pixels[i] >>> 24 == 0 || x >= pad && y >= pad && x < pad + box.width && y < pad + box.height
                            : piece.kind() + " paints outside its culling box at frame " + frame;
                }
            }
        }

        // Solid set pieces stay clear of every place the player or an enemy must stand.
        for (var footprint : SetPieces.footprints(0)) {
            assertClear(footprint, area.spawn().x(), area.spawn().y(), 48, "spawn");
            assertClear(footprint, area.hub().x(), area.hub().y(), 64, "camp");
            assertClear(footprint, area.boss().x(), area.boss().y(), 140, "Warden");
            for (var enemy : area.enemies()) assertClear(footprint, enemy.x(), enemy.y(), 40, "enemy spawn");
        }
        assert !map.isBlocked(area.spawn().x(), area.spawn().y() + 24, 24) : "blocked spawn";

        // Every landmark, the boss floor and (once open) the East Gate stay reachable from the spawn.
        map.clearGuardian();
        map.openGate();
        boolean[][] seen = flood(map, area.spawn().x(), area.spawn().y());
        for (var landmark : area.landmarks()) assert reachable(seen, landmark.x(), landmark.y()) : "cannot reach " + landmark.name();
        assert reachable(seen, area.exit().x(), area.exit().y()) : "cannot reach the East Gate";
        System.out.println("OutpostArtTest passed");
    }

    private static void assertClear(RuinedOutpostMap.Obstacle o, double x, double y, double radius, String what) {
        double dx = Math.max(0, Math.abs(x - o.centerX()) - o.width() / 2), dy = Math.max(0, Math.abs(y - o.centerY()) - o.height() / 2);
        assert Math.hypot(dx, dy) >= radius : "set piece at " + o.centerX() + "," + o.centerY() + " crowds the " + what;
    }

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
