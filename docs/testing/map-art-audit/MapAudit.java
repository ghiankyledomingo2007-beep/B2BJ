import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Read-only renderer fixtures; no input to the running game. */
public class MapAudit {
  public static void main(String[] args) throws Exception {
    Path out=Path.of(args[0]);Files.createDirectories(out);GameAudio.setMuted(true);
    var enter=RuinedOutpostGame.class.getDeclaredMethod("enterRoom",int.class,int.class);enter.setAccessible(true);
    var draw=B2BJ.class.getDeclaredMethod("drawWorld",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
    for(int room=0;room<RuinedOutpostMap.ROOMS.size();room++) {
      var panel=new B2BJ(false);panel.game().begin();enter.invoke(panel.game(),room,-1);
      var map=panel.game().map();
      var world=new BufferedImage(map.worldWidth(),map.worldHeight(),BufferedImage.TYPE_INT_ARGB);
      var ink=world.createGraphics();
      for(int cy:new int[]{0,432})for(int cx:new int[]{0,640}) {
        var tile=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=tile.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        draw.invoke(panel,g,cx,cy);g.dispose();ink.drawImage(tile,cx,cy,null);
      }
      ink.dispose();String name=String.format("%02d-%s",room,map.description().name().toLowerCase().replace(' ','-'));
      ImageIO.write(world,"png",out.resolve(name+".png").toFile());
      var overview=new BufferedImage(960,602,BufferedImage.TYPE_INT_RGB);var g=overview.createGraphics();
      g.setColor(Color.WHITE);g.drawString(name,12,18);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(world,0,26,960,576,null);g.dispose();ImageIO.write(overview,"png",out.resolve(name+"-overview.png").toFile());
      System.out.printf("%02d %s: %d props/blockouts, %d dressing, %d exits%n",room,map.description().name(),map.barriers().size(),map.dressing().size(),map.doors().size());
    }
  }
}
