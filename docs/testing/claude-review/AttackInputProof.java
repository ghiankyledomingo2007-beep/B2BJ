import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.imageio.ImageIO;

/** Diagnostic only: exercises the packaged game's real mouse/key handlers, no production edits. */
public final class AttackInputProof {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        Path out=Path.of(args[0]);Files.createDirectories(out);
        var panel=panel();
        Set<WaterProjectile> captured=Collections.newSetFromMap(new IdentityHashMap<>());
        var strip=new BufferedImage(1200,270,BufferedImage.TYPE_INT_RGB);
        var ink=strip.createGraphics();ink.setColor(new Color(15,23,32));ink.fillRect(0,0,1200,270);
        mouse(panel,MouseEvent.MOUSE_PRESSED);
        int shots=0;
        WaterProjectile.Kind[] expected={WaterProjectile.Kind.CUT,WaterProjectile.Kind.RETURN_CUT,WaterProjectile.Kind.FINISHER};
        for(int frame=0;frame<200&&shots<3;frame++) {
            panel.step(.01);
            for(var wave:panel.game().projectiles())if(wave.age()>=.13&&captured.add(wave)) {
                assert wave.kind()==expected[shots] : "mouse hold did not advance combo";
                assert wave.damage()==(shots==2?2:1);
                var image=paint(panel);
                ImageIO.write(image,"png",out.resolve("mouse-hit-"+(shots+1)+".png").toFile());
                ink.drawImage(image,shots*400,0,shots*400+400,230,550,245,950,475,null);
                ink.setColor(Color.WHITE);ink.drawString("M1 "+(shots+1)+" / "+wave.kind()+" / damage "+wave.damage(),shots*400+12,255);
                System.out.println("mouse hold: "+wave.kind()+", damage="+wave.damage()+", frame="+frame);
                shots++;
            }
        }
        mouse(panel,MouseEvent.MOUSE_RELEASED);
        assert shots==3;
        ink.dispose();ImageIO.write(strip,"png",out.resolve("mouse-combo-proof.png").toFile());
        panel=panel();key(panel,"leftPressed");key(panel,"dashPressed");key(panel,"dashReleased");
        for(int i=0;i<20;i++)panel.step(.01);
        key(panel,"leftReleased");mouse(panel,MouseEvent.MOUSE_PRESSED);
        for(int i=0;i<5;i++)panel.step(.01);
        assert panel.game().projectiles().size()==1;
        assert panel.game().projectiles().get(0).kind()==WaterProjectile.Kind.COUNTER;
        mouse(panel,MouseEvent.MOUSE_RELEASED);
        System.out.println("keyboard back-dash + mouse: COUNTER, damage=1");
        System.out.println("Loaded B2BJ from "+B2BJ.class.getProtectionDomain().getCodeSource().getLocation());
    }
    private static B2BJ panel() {
        var panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());key(panel,"beginPressed");return panel;
    }
    private static void mouse(B2BJ panel,int event) {
        panel.dispatchEvent(new MouseEvent(panel,event,0,0,950,360,1,false,MouseEvent.BUTTON1));
    }
    private static void key(B2BJ panel,String name) {
        panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel,0,name));
    }
    private static BufferedImage paint(B2BJ panel) {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();return image;
    }
}
