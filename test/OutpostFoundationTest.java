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
        var union=new java.awt.geom.Area(panel.game().map().bankShape());
        var halo=new java.awt.geom.Area(new BasicStroke(16).createStrokedShape(union));halo.subtract(union);
        int shadowed=0;
        for(int y=4;y<720;y+=8)for(int x=4;x<1280;x+=8) {
            int alpha=image.getRGB(x,y)>>>24;
            if(union.contains(x,y))assert alpha==255 : "earth shape must be solid inside the collider union at "+x+","+y;
            else if(halo.contains(x,y)) {
                assert alpha>0&&alpha<255 : "floor shadow expected just outside the bank edge at "+x+","+y;shadowed++;
            } else assert alpha==0 : "bank art must not leak past the collider union at "+x+","+y;
        }
        assert shadowed>50 : "floor shadow band missing";
        // Column 3 of the top bank ends at y=96; four pixels inside that edge is slope, not raw texture.
        assert union.contains(200,92)&&!union.contains(200,100);
        assert image.getRGB(200,92)!=Color.MAGENTA.getRGB() : "slope band inside the edge must be shaded, not raw texture";
        field.set(panel,null);g=image.createGraphics();draw.invoke(panel,g,0,0);g.dispose();
        var bank=B2BJ.loadImage("assets/tilesets/ruined_outpost/earth_banks.png");
        // Candidate art failed visual review; missing art intentionally keeps the stable fallback.
        if(bank!=null)assert bank.getWidth()==128&&bank.getHeight()==128 : "native bank atlas dimensions";
        for(var item:map.dressing()) {
            var sprite=B2BJ.loadImage(item.decoration().path());
            assert sprite!=null : "reviewed breach debris required";
            assert sprite.getWidth()==64&&(sprite.getHeight()==48||sprite.getHeight()==64);
            int filled=0;
            for(int y=0;y<sprite.getHeight();y++)for(int x=0;x<sprite.getWidth();x++)if((sprite.getRGB(x,y)>>>24)>0)filled++;
            assert filled>20&&filled<sprite.getWidth()*sprite.getHeight()*.85 : "debris must have real transparent background";
        }
        // Rest marker follows availability: room 10's marker disappears once the field dressing is used.
        var enter=RuinedOutpostGame.class.getDeclaredMethod("enterRoom",int.class,int.class);enter.setAccessible(true);
        var world=B2BJ.class.getDeclaredMethod("drawWorld",Graphics2D.class,int.class,int.class);world.setAccessible(true);
        panel=new B2BJ(false);panel.game().begin();enter.invoke(panel.game(),10,-1);
        var infirmary=panel.game().map();
        // In reach of the dressing but standing clear of the marker itself.
        panel.game().player().relocate(infirmary.restX(),infirmary.restY()+100);
        assert markerVisible(panel,world,infirmary) : "available dressing shows its ground marker";
        assert panel.game().interact()&&panel.game().infirmaryUsed();
        assert !markerVisible(panel,world,infirmary) : "consumed dressing must not look available";
        System.out.println("OutpostFoundationTest passed");
    }
    private static boolean markerVisible(B2BJ panel,java.lang.reflect.Method world,RuinedOutpostMap map) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        world.invoke(panel,g,(int)map.restX()-640,(int)map.restY()-360);g.dispose();
        int marker=new Color(104,146,144).getRGB();
        for(int y=300;y<350;y++)for(int x=620;x<660;x++)if(image.getRGB(x,y)==marker)return true;
        return false;
    }
}
