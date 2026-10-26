import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Chapter-one route guidance and the three separated introductory threats. */
public final class OutpostApproachTest {
    public static void main(String[] args) throws Exception {
        guidance(); encounters(); anticipation(); roads(); visibleTell();
        if(args.length>0) {
            Path out=Path.of(args[0]);Files.createDirectories(out);
            SwingUtilities.invokeAndWait(()->{
                try {capture(out);}catch(Exception e){throw new AssertionError(e);}
            });
        }
        System.out.println("OutpostApproachTest passed: optional guidance, separated threats, readable tells and roads");
    }

    private static RuinedOutpostGame start() {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();return game;
    }

    private static void visit(RuinedOutpostGame game,int index) {
        var place=game.campaignArea().landmarks().get(index);
        game.player().relocate(place.x(),place.y());
    }

    private static void guidance() {
        var game=start();
        assert game.outpostWaypoint()==1&&game.campaignObjectivePoint().equals(game.campaignArea().hub());
        visit(game,1);assert game.interact();game.skipDialogue();
        assert game.outpostWaypoint()==2&&game.campaignObjective().contains("Muster Ground");
        visit(game,2);assert game.outpostWaypoint()==6;
        visit(game,4);assert game.outpostWaypoint()==6 : "north route must not send explorers back to the first fight";
        visit(game,6);assert game.outpostWaypoint()==7;
        visit(game,7);assert game.outpostWaypoint()==8;
        var skip=start();visit(skip,8);skip.update(.01,0,0);
        assert skip.dialogue()==OutpostDialogue.WARDEN : "waypoints must never lock the boss behind visits or kills";
        skip.skipDialogue();skip.update(.01,0,0);
        assert skip.bossActive()&&skip.outpostWaypoint()==-1&&skip.campaignObjective().contains("Defeat");
        assert skip.campaignObjectivePoint().equals(skip.campaignArea().boss());
    }

    private static void encounters() {
        var game=start();
        assert game.scouts().size()==21 : "pace existing enemies instead of adding more";
        var kinds=new CampaignEnemy.Kind[]{CampaignEnemy.Kind.OUTPOST_SCOUT,
                CampaignEnemy.Kind.OUTPOST_GUARD,CampaignEnemy.Kind.OUTPOST_SPITTER};
        for(var enemy:game.scouts())assert !game.map().isBlocked(enemy.x(),enemy.y()+Wisp.COLLISION_Y_OFFSET,Wisp.COLLISION_RADIUS)
                : "authored enemy starts inside collision at "+enemy.x()+","+enemy.y();
        visit(game,1);game.update(.1,0,0);
        assert game.scouts().stream().noneMatch(Wisp::aggro) : "Mara's camp must remain quiet";
        for(int i=0;i<3;i++) {
            var enemy=game.scouts().get(i);
            assert game.campaignEnemy(enemy).kind()==kinds[i];
            game.player().relocate(enemy.x()-330,enemy.y());
            game.update(.05,0,0);
            assert enemy.aggro();
            assert game.scouts().stream().filter(Wisp::aggro).count()==1
                    : "east-road approach must introduce one threat at a time: "+kinds[i];
            enemy.hurt(enemy.maxHealth());
        }
    }

    private static void anticipation() {
        for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            var enemy=new CampaignEnemy(CampaignEnemy.Kind.OUTPOST_SCOUT,500,500);
            var body=enemy.body();int dx=direction[0],dy=direction[1];
            enemy.update(.01,500+dx*100,500+dy*100,null);
            assert body.state()==Wisp.State.TELEGRAPH;
            assert body.telegraphDuration()==.6&&body.recoverDuration()==.7;
            enemy.update(.32,500+dx*100,500+dy*100,null);
            assert body.directionLocked()&&!enemy.hits(500+dx*20,500+dy*20);
            enemy.update(.1,500-dx*100,500-dy*100,null);
            assert body.intentX()*dx+body.intentY()*dy>.99 : "late sidestep must not be tracked";
            enemy.update(.4,500-dx*100,500-dy*100,null);
            assert body.state()==Wisp.State.RECOVER&&!enemy.hits(body.x(),body.y());
        }
        var forest=new CampaignEnemy(CampaignEnemy.Kind.RIFT_IMP,500,500).body();
        assert forest.role()==Wisp.Role.RIFT_IMP&&forest.telegraphDuration()==.9
                : "forest imps need their own feint timing";
        var legacy=new Wisp(500,500,450,550);
        assert legacy.telegraphDuration()==.4&&legacy.recoverDuration()==.45 : "legacy scouts keep their timing";
    }

    private static void roads() {
        var game=start();var player=game.player();visit(game,1);
        for(int index:new int[]{2,6,7,8}) {
            var point=game.campaignArea().landmarks().get(index);
            int steps=0;
            while(Math.hypot(player.x()-point.x(),player.y()-point.y())>12&&steps++<2500) {
                double dx=point.x()-player.x(),dy=point.y()-player.y();
                player.move(Math.abs(dx)<6?0:dx>0?1:-1,Math.abs(dy)<6?0:dy>0?1:-1,.02,game.map());
            }
            assert steps<2500 : "main route blocked before "+point.name();
        }
    }

    private static void visibleTell() throws Exception {
        var game=start();var enemy=game.scouts().get(0);
        game.player().relocate(enemy.x()-100,enemy.y());game.update(.01,0,0);game.update(.32,0,0);
        assert enemy.directionLocked();
        var panel=new B2BJ(false,game);
        var method=B2BJ.class.getDeclaredMethod("drawGuardianTelegraph",java.awt.Graphics2D.class,int.class,int.class);
        method.setAccessible(true);
        var image=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        method.invoke(panel,g,(int)enemy.x()-160,(int)enemy.y()-160);g.dispose();
        int visible=0;
        for(int y=140;y<180;y++)for(int x=50;x<160-WispAnimation.RENDER_SIZE/2;x++)
            if((image.getRGB(x,y)>>>24)>0)visible++;
        assert visible>8 : "committed warning must extend beyond the sprite silhouette";
    }

    private static void capture(Path out) throws Exception {
        var game=start();visit(game,1);game.interact();game.skipDialogue();
        var panel=new B2BJ(false,game);panel.setSize(1280,720);panel.doLayout();
        for(int i=0;i<200;i++)panel.step(.02);
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();CampaignRenderer.drawMap(g,game,1280,720);g.dispose();
        ImageIO.write(image,"png",out.resolve("camp-route-map.png").toFile());
        game.player().relocate(1740,2944);
        for(int i=0;i<100&&game.scouts().get(0).state()!=Wisp.State.TELEGRAPH;i++)panel.step(.01);
        for(int i=0;i<34;i++)panel.step(.01);
        assert game.scouts().get(0).state()==Wisp.State.TELEGRAPH&&game.scouts().get(0).directionLocked()
                : "capture must show the readable committed windup";
        g=image.createGraphics();panel.paint(g);g.dispose();
        ImageIO.write(image,"png",out.resolve("first-remnant.png").toFile());
    }
}
