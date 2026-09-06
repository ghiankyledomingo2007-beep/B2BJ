import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CharacterAssetQualityTest {
    private static final int MAX_PALETTE_COLORS = 26;

    public static void main(String[] args) {
        slimeSheetHasCleanTransparency();
        slimeEffectsMatchTheirFrameGrid();
        bladeSheetsMatchTheirFrameGrid();
        wispSheetsMatchTheirFrameGridAndPalette();
        ruinedOutpostAssetsMatchTheirRuntimeGrids();
        reviewedBladeImpactHasDistinctTransparentFrames();
        reviewedSlimeIdleKeepsItsFloor();
        guardianWindupRaisesArmsBeforeImpact();
        BufferedImage blobHit=requireImage("assets/effects/blob_hit.png",288,32);
        Set<Integer> hitFrames=new HashSet<>();int previous=1024;
        for(int i=0;i<9;i++) {
            BufferedImage frame=blobHit.getSubimage(i*32,0,32,32);
            int visible=1024-transparentPixels(frame);
            assert visible>0 && visible<=previous : "Blob hit must dissipate without background growth";
            assert opaqueColors(frame)<=12 : "Blob hit palette drift";
            hitFrames.add(Arrays.hashCode(frame.getRGB(0,0,32,32,null,0,32)));previous=visible;
        }
        assert hitFrames.size()==9;
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

    private static void reviewedBladeImpactHasDistinctTransparentFrames() {
        BufferedImage sheet=requireImage("assets/effects/blade_hit.png",96,32);
        Set<Integer> hashes=new HashSet<>();
        int firstVisible=0,lastVisible=0;
        for(int frame=0;frame<3;frame++) {
            int[] pixels=sheet.getRGB(frame*32,0,32,32,null,0,32);
            hashes.add(Arrays.hashCode(pixels));
            long transparent=Arrays.stream(pixels).filter(pixel->(pixel>>>24)==0).count();
            assert transparent>512 : "impact must not have an opaque background";
            if(frame==0)firstVisible=(int)(1024-transparent);
            if(frame==2)lastVisible=(int)(1024-transparent);
        }
        assert hashes.size()==3 : "impact must not repeat one static frame";
        assert lastVisible<firstVisible : "impact must dissipate";
    }

    private static void bladeSheetsMatchTheirFrameGrid() {
        requireImage("assets/characters/blade/blade_run.png", 192, 192);
        requireImage("assets/characters/blade/blade_dash.png", 144, 192);
        requireImage("assets/characters/blade/blade_slash.png",144,192);
        BufferedImage attack=requireImage("assets/characters/blade/blade_cut.png",512,160);
        BufferedImage run=requireImage("assets/characters/blade/blade_run.png",192,192);
        for(int row=0;row<2;row++) {
            Set<Integer> poses=new HashSet<>();
            for(int y=0;y<64;y++)for(int x=0;x<48;x++) {
                assert attack.getRGB(x+8,row*80+y+8)==run.getRGB(x,row*128+y)
                        : "attack start must retain the exact native body and correct facing";
            }
            for(int i=0;i<8;i++) {
                BufferedImage frame=attack.getSubimage(i*64,row*80,64,80);
                poses.add(Arrays.hashCode(frame.getRGB(0,0,64,80,null,0,64)));
                assert opaqueColors(frame)<=32 : "attack palette drift";
                assert transparentPixels(frame)>3500 : "attack must not contain giant opaque effects";
                for(int y=0;y<80;y++)for(int x=0;x<64;x++) {
                    if(x<2||x>=62||y<2||y>=72)
                        assert (frame.getRGB(x,y)>>>24)==0 : "weapon clipping or ground drift";
                }
                boolean grounded=false;
                for(int x=0;x<64;x++)grounded|=(frame.getRGB(x,71)>>>24)>0;
                assert grounded : "attack boots must retain the shared ground line";
            }
            assert poses.size()>=6 : "attack needs distinct motion, not repeated placeholders";
        }
    }

    private static void slimeEffectsMatchTheirFrameGrid() {
        BufferedImage sheet = requireImage(
                "assets/characters/slime/slime_effects.png", 384, 192);

        assert opaqueColors(sheet) <= MAX_PALETTE_COLORS
                : "Slime effects must use the locked project palette";
        assert transparentPixels(sheet) > 0 : "Slime effects must preserve transparency";
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

    private static void ruinedOutpostAssetsMatchTheirRuntimeGrids() {
        String[][] assets = {
                {"assets/characters/guardian/guardian_actions.png", "256", "192"},
                {"assets/environment/ruined_outpost/gate.png", "128", "80"},
                {"assets/environment/ruined_outpost/torch.png", "72", "32"},
                {"assets/environment/ruined_outpost/altar_mask.png", "48", "64"},
                {"assets/environment/ruined_outpost/ichor_pool.png", "48", "32"},
                {"assets/environment/ruined_outpost/ooze.png", "48", "24"},
                {"assets/ui/ui_icons.png", "35", "7"}
        };
        for (String[] asset : assets) {
            BufferedImage image = requireImage(asset[0],
                    Integer.parseInt(asset[1]), Integer.parseInt(asset[2]));
            assert transparentPixels(image) > 0 : asset[0] + " must preserve transparency";
        }
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
