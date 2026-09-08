import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Real campaign renderer and simulation; isolated in-memory test journey, never the owner's save. */
public final class PreviewSlimeDepth {
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]);Files.createDirectories(output);GameAudio.setMuted(true);
        for(var trait:Player.Trait.values()) {
            var game=RuinedOutpostGame.campaign();game.begin();
            var panel=new B2BJ(false,game);panel.setSize(1280,720);
            var landmark=game.campaignArea().landmarks().get(3);
            game.player().relocate(landmark.x()-180,landmark.y()+128);
            game.player().gainTrait(trait);panel.step(.08);
            capture(panel,output.resolve("trait-"+trait+".png"));
        }
        for(boolean tide:new boolean[]{false,true}) {
            var game=RuinedOutpostGame.campaign();game.begin();
            var panel=new B2BJ(false,game);panel.setSize(1280,720);
            var enemy=game.scouts().get(0);
            game.player().relocate(enemy.x()-180,enemy.y());
            if(!tide)for(int i=0;i<2;i++) {assert game.attack(0,1);panel.step(.5);}
            for(int frame=0;frame<40;frame++) {
                if(frame==0) {
                    panel.dispatchEvent(new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,
                            820,360,1,false,tide?3:1));
                    panel.dispatchEvent(new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_RELEASED,0,0,
                            820,360,1,false,tide?3:1));
                }
                panel.step(.02);
                if(frame%3==0)capture(panel,output.resolve((tide?"tide":"finisher")+"-"+String.format("%02d",frame)+".png"));
            }
            System.out.printf("%s: health=%.1f, enemyHP=%d, projectiles=%d%n",tide?"Tide":"Finisher",
                    game.player().healthValue(),enemy.health(),game.projectiles().size());
        }
        System.out.println("Slime depth screenshots: "+output.toAbsolutePath());
    }
    private static void capture(B2BJ panel,Path path) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        panel.paint(g);g.dispose();ImageIO.write(image,"png",path.toFile());
    }
}
