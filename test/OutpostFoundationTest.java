import java.awt.*;
import java.awt.image.BufferedImage;

public final class OutpostFoundationTest {
    public static void main(String[] args) throws Exception {
        var map=new RuinedOutpostMap(1);
        var fences=map.barriers();
        assert fences.size()==2;
        assert fences.get(0).centerY()==fences.get(1).centerY()
                : "broken palisade should read as one interrupted defence line";
        assert fences.get(1).centerX()-fences.get(0).centerX()-fences.get(0).width()>200
                : "breach must leave a broad actor passage";
        for(var item:map.dressing()) {
            assert item.decoration()!=RuinedOutpostMap.Decoration.SHIELD_CACHE
                    &&item.decoration()!=RuinedOutpostMap.Decoration.RUBBLE
                    : "reference area must not retain bright treasure-like corner filler";
            assert item.obstacle()==null : "fallen debris must not add invisible solid obstacles";
        }
        var panel=new B2BJ(false);panel.game().begin();
        var field=B2BJ.class.getDeclaredField("bankTiles");field.setAccessible(true);
        var marker=new BufferedImage[16];
        for(int mask=0;mask<16;mask++) {
            marker[mask]=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
            var ink=marker[mask].createGraphics();ink.setColor(Color.MAGENTA);ink.fillRect(0,0,64,64);ink.dispose();
        }
        field.set(panel,marker);
        var draw=B2BJ.class.getDeclaredMethod("drawBanks",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        draw.invoke(panel,g,0,0);g.dispose();
        assert image.getRGB(32,32)==Color.MAGENTA.getRGB() : "boundary must consume authored bank art";
        assert image.getRGB(640,500)==0 : "boundary art must not cover the open floor";
        field.set(panel,null);g=image.createGraphics();draw.invoke(panel,g,0,0);g.dispose();
        System.out.println("OutpostFoundationTest passed");
    }
}
