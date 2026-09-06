import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Staged scene review, not a claim of live input testing. */
public final class PreviewHud {
    private static final Path OUT=Path.of("docs/testing/hud-40");
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);GameAudio.setMuted(true);
        var panel=new B2BJ(false);panel.setSize(1280,720);save(panel,"prologue");
        panel.game().begin();panel.game().player().collectIchor(63);save(panel,"hud");
        panel.game().togglePause();save(panel,"pause");panel.game().togglePause();
        var visited=RuinedOutpostGame.class.getDeclaredField("visited");visited.setAccessible(true);
        var cleared=RuinedOutpostGame.class.getDeclaredField("cleared");cleared.setAccessible(true);
        for(int i=0;i<8;i++){((boolean[])visited.get(panel.game()))[i]=true;((boolean[])cleared.get(panel.game()))[i]=true;}
        var map=B2BJ.class.getDeclaredField("mapShown");map.setAccessible(true);map.set(panel,true);save(panel,"map");
        panel.game().pause();save(panel,"map-paused");panel.game().togglePause();
        java.util.Arrays.fill((boolean[])visited.get(panel.game()),true);save(panel,"map-all");map.set(panel,false);
        panel.game().player().collectIchor(100);save(panel,"ready");panel.game().player().transform();
        panel.game().player().hurt(3);save(panel,"blade");
        var player=new Player(900,550);player.hurt(2);
        var enemy=new Wisp(970,550,970,970,1);
        var world=new RuinedOutpostMap(5);
        var game=new RuinedOutpostGame(world,player,List.of(enemy),new Guardian(world.guardianX(),world.guardianY()));game.begin();
        var gameField=B2BJ.class.getDeclaredField("game");gameField.setAccessible(true);gameField.set(panel,game);
        enemy.hurt(1);var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class);hit.setAccessible(true);hit.invoke(game,enemy);
        save(panel,"remains");game.interact();for(int i=0;i<50;i++)panel.step(.01);save(panel,"absorbing");
        for(int i=0;i<38;i++)panel.step(.01);save(panel,"healed");
    }
    private static void save(B2BJ panel,String name)throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();ImageIO.write(image,"png",OUT.resolve(name+".png").toFile());
        System.out.println("Saved "+name);
    }
}
