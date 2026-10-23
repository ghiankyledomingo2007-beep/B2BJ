import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class OutpostLightsTest {
    public static void main(String[] args) {
        var glow=OutpostLights.glow(80,0xff9a3c,104);
        int previous=255;
        var levels=new java.util.HashSet<Integer>();
        for(int x=80;x<160;x++) {
            int alpha=glow.getRGB(x,80)>>>24;
            assert alpha<=previous : "falloff must not have ordered-dither bright spots";
            levels.add(alpha);previous=alpha;
        }
        assert levels.size()>40 && previous==0 : "light must fade smoothly to transparent";

        var light=new SetPieces.Light(100,20,100,150,0xff9a3c,true);
        var mask=new BufferedImage(300,200,BufferedImage.TYPE_INT_ARGB);
        var g=mask.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,300,200);
        g.setComposite(AlphaComposite.DstOut);
        OutpostLights.shadow(g,light,140,100,32,16,80);g.dispose();
        assert (mask.getRGB(150,100)>>>24)<120 : "caster blocks light on its far side";
        assert (mask.getRGB(120,100)>>>24)==255 : "lit side must stay lit";
        assert (mask.getRGB(150,50)>>>24)==255 : "shadow must not cover unrelated ground";

        var game=RuinedOutpostGame.campaign();
        var piece=SetPieces.pieces(0).stream().filter(p -> p.kind()==SetPieces.Kind.BRAZIER&&!SetPieces.arenaBrazier(p)).findFirst().orElseThrow();
        int cx=(int)piece.x()-640,cy=(int)piece.y()-360;
        game.player().relocate(100,100);
        var empty=ground(game,cx,cy,0,true);
        game.player().collectIchor(100);game.player().transform();
        game.player().relocate(piece.x()+70,piece.y()-48);
        var blocked=ground(game,cx,cy,0,true);
        assert (blocked.getRGB(730,360)>>>24)<(empty.getRGB(730,360)>>>24) : "moving player casts a shadow";
        assert Arrays.equals(pixels(blocked),pixels(ground(game,cx,cy,900,true))) : "reduced effects freezes light flicker";
        game.player().relocate(100,100);
        assert Arrays.equals(pixels(empty),pixels(ground(game,cx,cy,0,true))) : "moving away must clear the old shadow";

        var arena=SetPieces.pieces(0).stream().filter(SetPieces::arenaBrazier).findFirst().orElseThrow();
        cx=(int)arena.x()-640;cy=(int)arena.y()-360;
        int cold=ground(game,cx,cy,0,true).getRGB(640,360)>>>24;
        game.guardian().activate(game.player().x(),game.player().y());
        int warm=ground(game,cx,cy,0,true).getRGB(640,360)>>>24;
        assert cold==0 && warm>50 : "dormant arena fires must not light the floor";
        System.out.println("OutpostLightsTest passed");
    }

    private static BufferedImage ground(RuinedOutpostGame game,int cx,int cy,long frame,boolean reduced) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();OutpostLights.drawGround(g,game,cx,cy,frame,reduced);g.dispose();
        return image;
    }
    private static int[] pixels(BufferedImage image) {
        return ((java.awt.image.DataBufferInt)image.getRaster().getDataBuffer()).getData();
    }
}
