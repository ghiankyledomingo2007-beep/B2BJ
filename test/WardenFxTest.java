import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;

/** Warden effects stay capped, run without art, and sit in depth order so actors in front cover the Warden's flash. */
public final class WardenFxTest {
    public static void main(String[] args) {
        BufferedImage atlas = B2BJ.loadImage("assets/characters/guardian/warden_motion.png");
        var view = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) view.getRaster().getDataBuffer()).getData();

        for (BufferedImage sheet : new BufferedImage[]{null, atlas}) {
            var warden = new Guardian(320, 300);
            warden.activate(200, 300);
            var fx = new WardenFx(sheet);
            fx.awaken(warden);
            var g = view.createGraphics();
            for (int i = 0; i < 600; i++) {                 // ten seconds with a slam every frame
                warden.update(1 / 60.0, 200, 300);
                fx.slam(warden, 200, 300);
                fx.update(1 / 60.0, warden);
                fx.drawGround(g, warden, 0, 0);
                fx.queue((y, paint) -> paint.run(), g, warden, 0, 0, i, false);
                fx.drawAbove(g, warden, 0, 0, i);
                assert fx.particles() <= 260 : "Warden effects must stay capped";
            }
            warden.hurt(warden.health());
            for (int i = 0; i < 120; i++) fx.update(1 / 60.0, warden);
            assert fx.fallen() : "the fallen Warden stays in the arena";
            g.dispose();
        }

        // Mid-blink, the white hit flash belongs to the Warden's own depth slot, never the layer above every actor.
        var warden = new Guardian(320, 300);
        warden.activate(200, 300);
        warden.hurt(1);
        assert !warden.flashVisible() : "test needs the Warden mid-blink";
        var fx = new WardenFx(atlas);
        var g = view.createGraphics();
        Arrays.fill(pixels, 0);
        fx.drawAbove(g, warden, 0, 0, 0);
        for (int p : pixels) assert p == 0 : "the hit flash must not paint over actors in front of the Warden";
        var depths = new ArrayList<Double>();
        var paints = new ArrayList<Runnable>();
        fx.queue((y, paint) -> { depths.add(y); paints.add(paint); }, g, warden, 0, 0, 0, false);
        double after = depths.get(0) - (warden.y() + Guardian.GROUND_Y_OFFSET);
        assert depths.size() == 1 && after > 0 && after < 1 : "the flash sorts just after the Warden, before anything in front";
        paints.get(0).run();
        int white = 0;
        for (int p : pixels) if (p == 0xfffff6ff) white++;
        assert white > 500 : "a hurt Warden flashes white";
        Arrays.fill(pixels, 0);
        fx.queue((y, paint) -> paint.run(), g, warden, 0, 0, 0, true);
        for (int p : pixels) assert p == 0 : "reduced effects keep the plain blink, without the white flash";
        g.dispose();
        System.out.println("WardenFxTest passed");
    }
}
