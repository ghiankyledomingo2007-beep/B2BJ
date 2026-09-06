import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Reproducible scene fixtures, not an unassisted playthrough. Compile against the built JAR. */
public final class PreviewOutpost {
    public static void main(String[] args) throws Exception {
        int target=Integer.parseInt(args[0]);
        if(target<0||target>9)throw new IllegalArgumentException("main-path room 0..9");
        GameAudio.setMuted(true);
        B2BJ panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());
        var game=panel.game();game.begin();
        while(game.map().room()<target) {
            for(Wisp enemy:game.scouts())enemy.hurt(enemy.health());
            game.update(0.01,0,0);
            if(game.map().room()==4)game.transform();
            int next=game.map().room()+1;
            var door=game.map().doors().stream().filter(d->d.destination()==next).findFirst().orElseThrow();
            game.player().relocate(door.x(),door.y());
            if(!game.interact())throw new AssertionError("Cannot enter "+next);
        }
        if(target==9) {
            game.player().relocate(game.guardian().x()+220,game.guardian().y()+60);
            game.guardian().update(0.8,game.player().x(),game.player().y());
        } else {
            var prop=game.map().barriers().get(0);
            game.player().relocate(prop.centerX()+150,prop.centerY()+prop.height()/2);
        }
        if(args.length>2 && args[2].equals("hit")) {
            Wisp enemy=game.scouts().get(0);
            game.player().relocate(enemy.x()-80,enemy.y());
            if(!game.attack(1,0))throw new AssertionError("Attack blocked");
            panel.step(0.2);
            if(enemy.health()!=1)throw new AssertionError("Blob attack did not resolve");
        }
        if(args.length>2 && (args[2].startsWith("blade-")||args[2].startsWith("blob-")||args[2].startsWith("tide-"))) {
            if(args[2].startsWith("blade-")&&!game.player().bladeForm()) {game.player().collectIchor(100);game.transform();}
            if(args[2].startsWith("blob-")&&game.player().bladeForm())throw new IllegalArgumentException("Use rooms 0..4 for Blob fixture");
            int[] direction=switch(args[2].substring(args[2].indexOf('-')+1)) {
                case "south" -> new int[]{0,1}; case "east" -> new int[]{1,0};
                case "north" -> new int[]{0,-1}; case "west" -> new int[]{-1,0};
                default -> throw new IllegalArgumentException("Unknown attack direction");
            };
            int cameraX=(int)Math.round(Math.max(0,Math.min(game.map().worldWidth()-1280,game.player().x()-640)));
            int cameraY=(int)Math.round(Math.max(0,Math.min(game.map().worldHeight()-720,game.player().y()-360)));
            int mx=(int)Math.round(game.player().x())-cameraX+direction[0]*150;
            int my=(int)Math.round(game.player().y())-cameraY+direction[1]*150;
            int button=args[2].startsWith("tide-")?java.awt.event.MouseEvent.BUTTON3:java.awt.event.MouseEvent.BUTTON1;
            panel.dispatchEvent(new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_PRESSED,
                    0,0,mx,my,1,false,button));
            panel.dispatchEvent(new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_RELEASED,
                    0,0,mx,my,1,false,button));
            panel.step(args.length>3?Double.parseDouble(args[3]):0.12);
        }
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();
        ImageIO.write(image,"png",Path.of(args[1]).toFile());
    }
}
