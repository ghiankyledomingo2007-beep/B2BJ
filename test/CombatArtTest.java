import java.util.Arrays;
import java.util.HashSet;

public final class CombatArtTest {
    public static void main(String[] args) {
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
        var image=B2BJ.loadImage(path);
        assert image!=null : "Missing reviewed combat asset: "+path;
        assert image.getWidth()==width*columns&&image.getHeight()==height*rows;
        for(int row=0;row<rows;row++) {
            var hashes=new HashSet<Integer>();
            for(int col=0;col<columns;col++) {
                int[] pixels=image.getRGB(col*width,row*height,width,height,null,0,width);
                hashes.add(Arrays.hashCode(pixels));
                long visible=Arrays.stream(pixels).filter(p->(p>>>24)>0).count();
                assert visible>0&&visible<width*height*0.8 : "blank frame or opaque rectangle: "+path;
            }
            assert hashes.size()>=unique : "repeated animation frames: "+path;
        }
    }
}
