import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Deliberately staged art fixtures, never evidence of an unassisted playthrough. */
public final class PreviewImmersion {
    private static final Path OUT=Path.of("docs/testing/immersion-40");
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(true);Files.createDirectories(OUT);
        var spitter=new Wisp(1150,550,1150,1150,3,Wisp.Role.SPITTER);
        var panel=panel(5,new Player(900,550),List.of(spitter));
        for(int i=0;i<55;i++)panel.step(.01);save(panel,"spitter-tell");
        for(int i=0;i<50;i++)panel.step(.01);save(panel,"spitter-volley");
        for(boolean blade:new boolean[]{false,true}) {
            var p=new Player(900,550);if(blade){p.collectIchor(100);p.transform();}
            var hurt=panel(5,p,List.of());p.hurt(1);
            var handler=B2BJ.class.getDeclaredMethod("handleEvents",List.class);handler.setAccessible(true);
            handler.invoke(hurt,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.PLAYER_HIT,1150,550)));
            hurt.step(.05);save(hurt,blade?"blade-hurt":"slime-hurt");
        }
        var pickup=panel(5,new Player(900,550),List.of());
        var drops=RuinedOutpostGame.class.getDeclaredField("drops");drops.setAccessible(true);
        @SuppressWarnings("unchecked") var list=(List<RuinedOutpostGame.IchorDrop>)drops.get(pickup.game());
        list.add(new RuinedOutpostGame.IchorDrop(1040,550,10));
        save(pickup,"ichor-before");
        for(int i=0;i<25;i++)pickup.step(.01);save(pickup,"ichor-pulling");
        for(int i=0;i<20;i++)pickup.step(.01);save(pickup,"ichor-collected");
        var map=new RuinedOutpostMap(9);
        var bossPanel=panel(9,new Player(map.guardianX()+230,map.guardianY()+60),List.of());
        bossPanel.game().story().enterGatehouse();
        bossPanel.game().guardian().activate(bossPanel.game().player().x(),bossPanel.game().player().y());
        for(int i=0;i<55;i++)bossPanel.step(.01);save(bossPanel,"warden-larger");
    }
    private static B2BJ panel(int room,Player player,List<Wisp> enemies)throws Exception {
        var map=new RuinedOutpostMap(room);
        var game=new RuinedOutpostGame(map,player,enemies,new Guardian(map.guardianX(),map.guardianY()));game.begin();
        var panel=new B2BJ(false);panel.setSize(1280,720);
        var field=B2BJ.class.getDeclaredField("game");field.setAccessible(true);field.set(panel,game);
        return panel;
    }
    private static void save(B2BJ panel,String name)throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();panel.paint(g);g.dispose();ImageIO.write(image,"png",OUT.resolve(name+".png").toFile());
        System.out.println("Saved "+name);
    }
}
