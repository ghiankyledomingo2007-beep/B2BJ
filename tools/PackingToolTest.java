import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Pixel-preserving layout checks; no image generation or network calls. */
public final class PackingToolTest {
    public static void main(String[] args)throws Exception {
        var dir=Files.createTempDirectory("b2bj-packing-check-");
        var source=new BufferedImage(80,80,BufferedImage.TYPE_INT_ARGB);
        source.setRGB(20,63,0xff23acdd);source.setRGB(21,62,0xffead090);
        var input=dir.resolve("input.png");ImageIO.write(source,"png",input.toFile());
        System.setProperty("b2bj.maxFloorShift","16");
        var grounded=dir.resolve("grounded.png");
        PackFrames.main(new String[]{"80","80","1","72",grounded.toString(),input.toString()});
        var result=ImageIO.read(grounded.toFile());
        for(int y=0;y<80;y++)for(int x=0;x<80;x++)
            assert result.getRGB(x,y)==(y>=9?source.getRGB(x,y-9):0) : "ground alignment repainted pixels";
        System.setProperty("b2bj.flip","true");var mirrored=dir.resolve("mirrored.png");
        CropSprite.main(new String[]{input.toString(),"0","0","80","80",mirrored.toString()});
        result=ImageIO.read(mirrored.toFile());
        for(int y=0;y<80;y++)for(int x=0;x<80;x++)
            assert result.getRGB(x,y)==source.getRGB(79-x,y) : "mirror changed native pixels";
        System.clearProperty("b2bj.flip");System.clearProperty("b2bj.maxFloorShift");
        try {
            PackFrames.main(new String[]{"80","80","1","72",grounded.toString(),input.toString()});
            throw new AssertionError("default three-pixel drift guard was removed");
        } catch(IllegalArgumentException expected) {
            assert expected.getMessage().contains("floor drift");
        }
        transparentTailIsExplicitAndTerminal(dir,input);
        System.out.println("\nPackingToolTest passed");
    }
    private static void transparentTailIsExplicitAndTerminal(java.nio.file.Path dir,java.nio.file.Path input)throws Exception {
        var empty=dir.resolve("empty.png");ImageIO.write(new BufferedImage(80,80,BufferedImage.TYPE_INT_ARGB),"png",empty.toFile());
        var output=dir.resolve("tail.png");
        String[] frames={"80","80","2","63",output.toString(),input.toString(),empty.toString(),input.toString(),empty.toString()};
        System.clearProperty("b2bj.allowEmptyTail");rejectBlank(frames);
        System.setProperty("b2bj.allowEmptyTail","true");
        try {
            PackFrames.main(frames);
            var packed=ImageIO.read(output.toFile());
            assert packed.getWidth()==160&&packed.getHeight()==160;
            assert packed.getRGB(20,63)==0xff23acdd&&packed.getRGB(20,143)==0xff23acdd;
            for(int y=0;y<160;y++)for(int x=80;x<160;x++)assert packed.getRGB(x,y)==0 : "FX tail must stay genuinely transparent";
            rejectBlank(new String[]{"80","80","2","-1",output.toString(),empty.toString(),input.toString()});
            rejectBlank(new String[]{"80","80","4","-1",output.toString(),input.toString(),empty.toString(),input.toString(),empty.toString()});
            rejectBlank(new String[]{"80","80","1","-1",output.toString(),empty.toString()});
            rejectBlank(new String[]{"80","80","2","-1",output.toString(),empty.toString(),empty.toString()});
        } finally { System.clearProperty("b2bj.allowEmptyTail"); }
        rejectBlank(frames);
    }
    private static void rejectBlank(String[] frames)throws Exception {
        try { PackFrames.main(frames);throw new AssertionError("empty non-terminal/default frame must remain rejected"); }
        catch(IllegalArgumentException expected) { assert expected.getMessage().contains("transparency"); }
    }
}
