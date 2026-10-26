import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Staged renderer evidence only; never reads or writes the player's save. */
public final class PreviewForest {
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args.length==0?"docs/art-review/area-two/renders":args[0]);Files.createDirectories(out);
        GameAudio.setMuted(true);
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();game.debugWarp(1,false);
        var panel=new B2BJ(false,game);panel.setSize(1280,720);
        for(int i=0;i<100;i++)panel.step(.05);
        save(panel,out,"ferry-refuge");game.interact();save(panel,out,"sedge-dialogue");game.skipDialogue();
        for(int landmark:new int[]{2,3,4,5,6,7,9,10,11}) {
            var place=game.campaignArea().landmarks().get(landmark);game.player().relocate(place.x()-170,place.y()+100);
            panel.step(.01);save(panel,out,"landmark-"+landmark);
        }
        game.debugWarp(1,true);game.player().setGodMode(true);
        for(var enemy:game.scouts())enemy.hurt(10000);
        var boss=game.guardian();
        for(int move=0;move<3;move++) {
            until(panel,Guardian.State.TELEGRAPH);for(int i=0;i<45;i++)panel.step(.01);
            String name=boss.forestAttack().name().toLowerCase(java.util.Locale.ROOT);save(panel,out,name+"-warning");
            until(panel,Guardian.State.SLAM);panel.step(.08);save(panel,out,name+"-active");
            until(panel,Guardian.State.RECOVER);if(boss.coreExposed())save(panel,out,"heart-exposed");
        }
        boss.hurt(boss.maxHealth()/2+1);until(panel,Guardian.State.ENRAGE);panel.step(.4);save(panel,out,"briarheart-enrage");
        until(panel,Guardian.State.TELEGRAPH);panel.step(.5);save(panel,out,"phase-two-warning");
        System.out.println("Forest staged renderer captures: "+out.toAbsolutePath());
    }
    private static void until(B2BJ panel,Guardian.State state) {
        for(int i=0;i<1500&&panel.game().guardian().state()!=state;i++)panel.step(.01);
        if(panel.game().guardian().state()!=state)throw new AssertionError("did not reach "+state);
    }
    private static void save(B2BJ panel,Path out,String name) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();panel.paint(g);g.dispose();
        ImageIO.write(image,"png",out.resolve(name+".png").toFile());
    }
}
