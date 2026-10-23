import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CharacterAssetQualityTest {
    private static final int MAX_PALETTE_COLORS = 26;

    public static void main(String[] args) {
        slimeSheetHasCleanTransparency();
        wispSheetsMatchTheirFrameGridAndPalette();
        reviewedSlimeIdleKeepsItsFloor();
        guardianWindupRaisesArmsBeforeImpact();
        System.out.println("CharacterAssetQualityTest passed");
    }

    private static void slimeSheetHasCleanTransparency() {
        BufferedImage sheet = requireImage(
                "assets/characters/slime/slime_sprite_sheet.png", 192, 576);

        assert greenScreenPixels(sheet) == 0 : "slime sheet must not contain green screen pixels";
        assert transparentPixels(sheet) > 0 : "slime sheet must preserve transparent backgrounds";
    }
    private static void reviewedSlimeIdleKeepsItsFloor() {
        BufferedImage sheet=requireImage("assets/characters/slime/slime_idle.png",384,48);
        Set<Integer> frames=new HashSet<>();
        for(int i=0;i<8;i++) {
            BufferedImage frame=sheet.getSubimage(i*48,0,48,48);
            frames.add(Arrays.hashCode(frame.getRGB(0,0,48,48,null,0,48)));
            assert opaqueColors(frame)<=12 : "idle palette drift";
            assert transparentPixels(frame)>700 : "opaque idle background";
            boolean grounded=false;
            for(int x=0;x<48;x++)grounded|=(frame.getRGB(x,47)>>>24)>0;
            assert grounded : "idle ground line drift in frame "+i;
        }
        assert frames.size()==8 : "idle must have eight unique poses";
    }
    private static void guardianWindupRaisesArmsBeforeImpact() {
        BufferedImage sheet=requireImage("assets/characters/guardian/guardian_actions.png",256,192);
        int first=0,raised=0;
        for(int y=0;y<24;y++)for(int x=0;x<64;x++)if(x<16||x>47) {
            if((sheet.getRGB(x,y)>>>24)>0)first++;
            if((sheet.getRGB(192+x,y)>>>24)>0)raised++;
        }
        assert raised>first+30 : "Warden telegraph must visibly raise its arms before damage: "+first+" / "+raised;
    }

    private static void wispSheetsMatchTheirFrameGridAndPalette() {
        for (String path : new String[] {
                "assets/characters/wisp/wisp_walk.png",
                "assets/characters/wisp/wisp_attack.png"
        }) {
            BufferedImage sheet = requireImage(path, 384, 192);
            assert opaqueColors(sheet) <= MAX_PALETTE_COLORS
                    : path + " must use the locked 26-color project palette";
            assert transparentPixels(sheet) > 0
                    : path + " must preserve transparent backgrounds";
            if (path.endsWith("wisp_attack.png")) {
                for (int row = 0; row < 4; row++) {
                    assert distinctFrames(sheet, row) >= 6
                            : "wisp attack row " + row + " is too repetitive";
                }
            }
        }
    }

    private static int distinctFrames(BufferedImage sheet, int row) {
        Set<Integer> hashes = new HashSet<>();
        for (int x = 0; x < sheet.getWidth(); x += WispAnimation.CELL_SIZE) {
            int[] pixels = sheet.getRGB(x, row * WispAnimation.CELL_SIZE,
                    WispAnimation.CELL_SIZE, WispAnimation.CELL_SIZE,
                    null, 0, WispAnimation.CELL_SIZE);
            hashes.add(Arrays.hashCode(pixels));
        }
        return hashes.size();
    }

    private static BufferedImage requireImage(String path, int width, int height) {
        BufferedImage image = B2BJ.loadImage(path);
        assert image != null : path + " must load";
        assert image.getWidth() == width : path + " has wrong width";
        assert image.getHeight() == height : path + " has wrong height";
        return image;
    }

    private static int greenScreenPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if (pixel >>> 24 == 0) continue;
                int red = pixel >> 16 & 0xff;
                int green = pixel >> 8 & 0xff;
                int blue = pixel & 0xff;
                if (green >= 200 && green > red * 2 && green > blue * 2) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int transparentPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int opaqueColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if (pixel >>> 24 > 0) {
                    colors.add(pixel & 0x00ffffff);
                }
            }
        }
        return colors.size();
    }
}
