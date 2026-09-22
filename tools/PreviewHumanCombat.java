import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual input/update/render evidence. Fixtures do not touch campaign saves. */
public final class PreviewHumanCombat {
    private static final Path OUT=Path.of(System.getProperty("b2bj.previewDir","docs/testing/human-motion"));
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(true);Files.createDirectories(OUT);
        for(String direction:new String[]{"south","east","north","west"}) {
            var panel=scene();
            String key=switch(direction){case "east"->"right";case "west"->"left";case "north"->"up";default->"down";};
            key(panel,key,true);advance(panel,.15);
            for(int i=0;i<48;i++){save(panel,"run-"+direction+"-"+String.format("%02d",i));panel.step(1.0/60);}
            key(panel,key,false);advance(panel,.12);save(panel,"stop-"+direction);

            int dx=direction.equals("east")?1:direction.equals("west")?-1:0;
            int dy=direction.equals("south")?1:direction.equals("north")?-1:0;
            for(int move=0;move<4;move++) {
                mouse(panel,MouseEvent.MOUSE_PRESSED,dx,dy);advance(panel,.035);mouse(panel,MouseEvent.MOUSE_RELEASED,dx,dy);
                advance(panel,RuinedOutpostGame.strikeWindup(move)+.02);save(panel,"combo-"+move+"-"+direction);
                advance(panel,RuinedOutpostGame.strikeDuration(move)-RuinedOutpostGame.strikeWindup(move));
            }
            mouse(panel,MouseEvent.MOUSE_PRESSED,dx,dy);advance(panel,.81);save(panel,"charge-"+direction);
            mouse(panel,MouseEvent.MOUSE_RELEASED,dx,dy);advance(panel,.2);save(panel,"cleave-"+direction);
        }
        System.out.println("Saved input-driven human review to "+OUT);
    }
    static B2BJ scene() {
        var player=new Player(900,550);player.collectIchor(100);player.transform();player.setGodMode(true);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(5),player,List.of(),new Guardian(1700,900));game.begin();
        var panel=new B2BJ(false,game);panel.setSize(1280,720);return panel;
    }
    static void key(B2BJ panel,String name,boolean pressed) {
        panel.getActionMap().get(name+(pressed?"Pressed":"Released")).actionPerformed(new ActionEvent(panel,ActionEvent.ACTION_PERFORMED,name));
    }
    static void mouse(B2BJ panel,int type,int dx,int dy) {
        panel.dispatchEvent(new MouseEvent(panel,type,0,0,640+dx*180,360+dy*180,1,false,MouseEvent.BUTTON1));
    }
    static void advance(B2BJ panel,double seconds) {
        while(seconds>1e-8){double dt=Math.min(.005,seconds);panel.step(dt);seconds-=dt;}
    }
    static void save(B2BJ panel,String name)throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();panel.paint(g);g.dispose();
        ImageIO.write(image,"png",OUT.resolve(name+".png").toFile());
    }
}
