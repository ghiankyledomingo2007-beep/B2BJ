import java.util.Arrays;
import java.util.HashSet;

public final class CombatArtTest {
    public static void main(String[] args) {
        transparentTailValidation();
        check("assets/characters/guardian/guardian_walk.png",64,64,8,1,8);
        check("assets/characters/guardian/guardian_death.png",64,64,12,1,12);
        for(String action:new String[]{"sweep","charge","wave"})
            check("assets/characters/guardian/guardian_"+action+".png",64,64,8,1,8);
        check("assets/characters/wisp/remnant_walk.png",48,48,4,3,3);
        check("assets/characters/wisp/remnant_attack.png",48,48,8,3,8);
        check("assets/characters/slime/slime_cast.png",48,48,8,3,8);
        check("assets/characters/blade/blade_side.png",64,80,8,2,8);
        for(int combo=0;combo<3;combo++) {
            double windup=RuinedOutpostGame.strikeWindup(combo);
            assert B2BJ.sideAttackFrame(windup-0.001,combo)<3;
            assert B2BJ.sideAttackFrame(windup,combo)==3 : "contact pose must coincide with strike";
            assert B2BJ.sideAttackFrame(0.3,combo)==7;
        }
        System.out.println("CombatArtTest passed");
    }
    static void check(String path,int width,int height,int columns,int rows,int unique) {
        check(path,width,height,columns,rows,unique,false);
    }
    static void check(String path,int width,int height,int columns,int rows,int unique,boolean allowEmptyLast) {
        var image=B2BJ.loadImage(path);
        assert image!=null : "Missing reviewed combat asset: "+path;
        assert image.getWidth()==width*columns&&image.getHeight()==height*rows;
        for(int row=0;row<rows;row++) {
            var hashes=new HashSet<Integer>();
            for(int col=0;col<columns;col++) {
                int[] pixels=image.getRGB(col*width,row*height,width,height,null,0,width);
                hashes.add(Arrays.hashCode(pixels));
                long visible=Arrays.stream(pixels).filter(p->(p>>>24)>0).count();
                boolean terminalEmpty=allowEmptyLast&&columns>1&&col==columns-1&&visible==0;
                assert (visible>0||terminalEmpty)&&visible<width*height*0.8 : "blank frame or opaque rectangle: "+path;
            }
            assert hashes.size()>=unique : "repeated animation frames: "+path;
        }
    }
    private static void transparentTailValidation() {
        try {
            var path=java.nio.file.Files.createTempFile("b2bj-fx-tail-",".png");
            var image=new java.awt.image.BufferedImage(16,4,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int row=0;row<2;row++)for(int col=0;col<7;col++)image.setRGB(col*2,row*2,0xff000001+col);
            javax.imageio.ImageIO.write(image,"png",path.toFile());
            rejected(path.toString(),false,8,2,8);
            check(path.toString(),2,2,8,2,8,true);
            image.setRGB(12,0,image.getRGB(10,0));javax.imageio.ImageIO.write(image,"png",path.toFile());
            rejected(path.toString(),true,8,2,8);
            image.setRGB(12,0,0xff000007);
            for(int y=0;y<2;y++)for(int x=14;x<16;x++)image.setRGB(x,y,0xffffffff);
            javax.imageio.ImageIO.write(image,"png",path.toFile());rejected(path.toString(),true,8,2,8);
            for(int y=0;y<2;y++)for(int x=14;x<16;x++)image.setRGB(x,y,0);
            // An early blank in a later row remains invalid, even with the opt-in.
            image.setRGB(4,2,0);javax.imageio.ImageIO.write(image,"png",path.toFile());
            rejected(path.toString(),true,8,2,8);
            image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            javax.imageio.ImageIO.write(image,"png",path.toFile());rejected(path.toString(),true,1,1,1);
        } catch(java.io.IOException error) { throw new AssertionError("FX validation fixture failed",error); }
    }
    private static void rejected(String path,boolean allow,int columns,int rows,int unique) {
        boolean rejected=false;
        try { check(path,2,2,columns,rows,unique,allow); }
        catch(AssertionError expected) { rejected=true; }
        assert rejected : "blank first/middle/default frames must stay invalid";
    }
}
