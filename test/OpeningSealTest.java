import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class OpeningSealTest {
    public static void main(String[] args) {
        var ignition=render(.2,1,0,false,1);
        var assembled=render(4,5,0,false,1);
        assert !Arrays.equals(ignition,assembled) : "seal builds from ignition to assembled rings";
        assert Arrays.equals(assembled,render(4,5,0,false,1)) : "same scene time produces identical particles";
        assert !Arrays.equals(assembled,render(4,6,0,false,1)) : "settled rings, lights and orbits keep moving";
        assert !Arrays.equals(assembled,render(4,5,1,false,1)) : "core responds to spoken energy";
        assert Arrays.equals(render(1,1,0,true,1),render(9,300,1,true,1))
                : "reduced effects freeze assembly, particles, orbit and voice pulsing";
        var hidden=render(4,5,1,false,0);
        for(int pixel:hidden)assert pixel==0 : "caller can fully fade the seal";
        assert !Arrays.equals(assembled,render(4,5,0,false,.24f)) : "background version honors incoming opacity";
        System.out.println("OpeningSealTest passed: assembly, deterministic motion, voice response, reduced effects and caller opacity");
    }

    private static int[] render(double age,double time,double voice,boolean reduced,float opacity) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        var composite=AlphaComposite.SrcOver.derive(opacity);g.setComposite(composite);
        var transform=g.getTransform();
        OpeningSeal.draw(g,640,310,174,age,time,voice,reduced);
        assert g.getTransform().equals(transform)&&g.getComposite().equals(composite)
                : "seal must restore caller graphics state";
        g.dispose();return image.getRGB(0,0,1280,720,null,0,1280);
    }
}
