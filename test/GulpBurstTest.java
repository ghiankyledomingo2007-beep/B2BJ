import java.awt.image.BufferedImage;

/** Owner 2026-10-29: Beelzebub's finishing burst must read as the dark (void core, violet rim), never the orange counter. */
public final class GulpBurstTest {
    public static void main(String[] args) {
        for(String path:new String[]{"assets/effects/riposte_counter.png","assets/effects/ichor_pickup.png"})check(path);
    }
    private static void check(String path) {
        BufferedImage counter=B2BJ.loadImage(path);
        BufferedImage gulp=B2BJ.voidTint(counter);
        if(counter==null||gulp==null)throw new AssertionError(path+" missing");
        int shown=0,dark=0;
        for(int y=0;y<gulp.getHeight();y++)for(int x=0;x<gulp.getWidth();x++) {
            int a=counter.getRGB(x,y)>>>24,g=gulp.getRGB(x,y);
            if((g>>>24)!=a)throw new AssertionError("alpha changed at "+x+","+y);
            if(a==0)continue;
            shown++;
            int r=g>>16&255,gr=g>>8&255,b=g&255;
            if(r>gr+60&&b<r)throw new AssertionError("warm pixel left at "+x+","+y);  // orange or yellow
            if(b<r||b<gr)throw new AssertionError("not violet at "+x+","+y);
            if(r+gr+b<120)dark++;
        }
        if(dark*3<shown)throw new AssertionError("burst not dark enough: "+dark+"/"+shown);
        System.out.println("GulpBurstTest ok: "+path+" "+shown+" px, "+dark+" void");
    }
}
