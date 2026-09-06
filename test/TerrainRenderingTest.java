import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class TerrainRenderingTest {
    public static void main(String[] args) {
        rendersSixteenReusableTiles();
        outdoorTerrainKeepsIntegerPixels();
        System.out.println("TerrainRenderingTest passed");
    }

    private static void rendersSixteenReusableTiles() {
        BufferedImage sheet = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = sheet.createGraphics();
        canvas.setColor(Color.MAGENTA);
        canvas.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        canvas.dispose();

        BufferedImage[] tiles = B2BJ.renderTiles(sheet);

        assert tiles.length == 16 : "Wang tileset must cache every corner mask";
        for (BufferedImage tile : tiles) {
            assert tile.getWidth() == RuinedOutpostMap.TILE_SIZE : "tile width must match map";
            assert tile.getHeight() == RuinedOutpostMap.TILE_SIZE : "tile height must match map";
            assert tile.getRGB(0, 0) == Color.MAGENTA.getRGB() : "tile must contain source art";
        }
    }

    private static void outdoorTerrainKeepsIntegerPixels() {
        BufferedImage source=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=source.createGraphics();g.setColor(Color.MAGENTA);g.fillRect(0,0,128,128);g.dispose();
        BufferedImage[] tiles=B2BJ.outdoorTiles(B2BJ.renderTiles(source));
        assert tiles[0].getRGB(32,32)!=tiles[15].getRGB(32,32);
        for(BufferedImage tile:tiles)for(int y=0;y<64;y+=2)for(int x=0;x<64;x+=2) {
            int pixel=tile.getRGB(x,y);
            assert pixel==tile.getRGB(x+1,y)&&pixel==tile.getRGB(x,y+1)&&pixel==tile.getRGB(x+1,y+1)
                    : "terrain must use exact 2x native pixels";
        }
    }
}
