import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Staged rendering fixtures. Not player-completion evidence. */
public final class PreviewCombatRepair {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        Path out=Path.of("docs/testing/combat-repair-2026-09-06");Files.createDirectories(out);
        for(Guardian.Attack attack:Guardian.Attack.values()) {
            B2BJ panel=new B2BJ(false);panel.setSize(1280,720);
            var game=panel.game();game.begin();
            while(game.map().room()<9) {
                for(Wisp w:game.scouts())w.hurt(100);
                game.update(0.01,0,0);if(game.map().room()==4)game.transform();
                int next=game.map().room()+1;
                var door=game.map().doors().stream().filter(d->d.destination()==next).findFirst().orElseThrow();
                game.player().relocate(door.x(),door.y());game.interact();
            }
            var boss=game.guardian();
            for(int i=0;i<6000;i++) {
                double radius=attack==Guardian.Attack.SWEEP?130:400;
                game.player().relocate(boss.x()-radius,boss.y()+30);game.player().heal();
                panel.step(0.01);
                if(boss.attack()==attack&&boss.state()==Guardian.State.TELEGRAPH&&boss.stateSeconds()>0.55)break;
            }
            save(panel,out.resolve("boss-"+attack.name().toLowerCase()+"-tell.png"));
        }
    }
    private static void save(B2BJ panel,Path path)throws Exception {
        BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();ImageIO.write(image,"png",path.toFile());
    }
}
