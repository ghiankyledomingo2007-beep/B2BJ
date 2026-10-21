import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public final class DashVariantsTest {
    public static void main(String[] args) throws Exception {
        distanceCooldownAndVulnerability();
        bothFormsStopAtWalls();
        distinctEffectsAndFacing();
        System.out.println("DashVariantsTest passed: two distances, cooldown, vulnerable gap, walls, four-way effects, reduced effects");
    }
    private static void distanceCooldownAndVulnerability() {
        for(boolean blade:new boolean[]{false,true})for(int[] aim:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1}}) {
            var p=new Player(600,400);var map=new RuinedOutpostMap();
            if(blade){p.collectIchor(100);assert p.transform();}
            assert p.dash(aim[0],aim[1]);
            double duration=blade?.12:.16;
            assert p.invulnerable()&&!p.hurt(1);
            p.move(0,0,duration,map);
            close(Math.hypot(p.x()-600,p.y()-400),blade?120:144);
            assert !p.dashing()&&!p.invulnerable() : "dodge immunity ends before cooldown";
            assert !p.dash(aim[0],aim[1])&&p.hurt(1) : "there is a hittable gap between dodges";
            p.move(0,0,.319-duration,map);assert !p.dash(aim[0],aim[1]);
            p.move(0,0,.002,map);assert p.dash(aim[0],aim[1]) : "dash ready at 0.32 seconds";
        }
        var p=new Player(600,400);var map=new RuinedOutpostMap();
        assert p.dash(1,0);p.move(0,0,.08,map);p.collectIchor(100);assert p.transform();p.move(0,0,.08,map);
        close(p.x()-600,144); // Transforming cannot change the speed of an already-started burst.
    }
    private static void bothFormsStopAtWalls() {
        var map=new RuinedOutpostMap(1);var fence=map.barriers().get(0);
        double contact=fence.centerX()-fence.width()/2-Player.COLLISION_RADIUS;
        for(boolean blade:new boolean[]{false,true}) {
            var p=new Player(contact-80,fence.centerY()-Player.COLLISION_Y_OFFSET);
            if(blade){p.collectIchor(100);assert p.transform();}
            assert p.dash(1,0);p.move(0,0,.2,map);
            assert p.x()<=contact&&p.x()>contact-8 : "faster dodge still uses swept wall collision";
        }
    }
    private static void distinctEffectsAndFacing() throws Exception {
        GameAudio.setMuted(true);
        for(boolean blade:new boolean[]{false,true})for(boolean reduced:new boolean[]{false,true}) {
            var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(600,400),List.of(),new Guardian(1500,900));
            game.begin();game.drainEvents();
            if(blade){game.player().collectIchor(100);game.player().transform();}
            var panel=new B2BJ(false,game);set(panel,"reducedEffects",reduced);
            int[][] directions={{0,1},{1,0},{0,-1},{-1,0}};
            for(int row=0;row<directions.length;row++) {
                panel.step(.5);game.player().relocate(600,400);
                assert game.dash(directions[row][0],directions[row][1]);
                for(int i=0;i<6;i++)panel.step(.01);
                var trails=(List<?>)field(panel,"trails");
                var impacts=(List<?>)field(panel,"impacts");
                if(blade) {
                    assert trails.size()<7&&trails.isEmpty()==reduced;
                    assert impacts.isEmpty() : "human quickstep does not leave slime foam";
                    for(var trail:trails)assert (int)field(trail,"row")==row : "afterimage snapshots the actual dodge facing";
                    if(!reduced) {
                        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();
                        var draw=java.util.Arrays.stream(B2BJ.class.getDeclaredMethods()).filter(m->m.getName().equals("drawDashEcho")).findFirst().orElseThrow();
                        draw.setAccessible(true);for(var trail:trails)draw.invoke(panel,ink,0,0,trail);ink.dispose();
                        assert visible(image)>100 : "human dodge has a visible blue echo";
                    }
                } else {
                    assert trails.isEmpty()&&!impacts.isEmpty() : "slime leaves water, not a humanoid afterimage";
                    if(reduced)assert impacts.size()==1 : "reduced effects keeps only the starting ripple";
                    var animation=(SlimeAnimation)field(panel,"slimeAnimation");
                    assert animation.row()==(row==0?5:row==2?11:10) : "dash faces its direction even with no movement key held";
                    for(var impact:impacts)assert field(impact,"kind").toString().equals("DASH_WAKE");
                    var draw=B2BJ.class.getDeclaredMethod("drawImpacts",Graphics2D.class,int.class,int.class,Boolean.class);draw.setAccessible(true);
                    var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();
                    draw.invoke(panel,ink,0,0,true);ink.dispose();assert visible(image)>20 : "water wake stays below bodies";
                    image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);ink=image.createGraphics();
                    draw.invoke(panel,ink,0,0,false);ink.dispose();assert visible(image)==0 : "wake does not obscure the body";
                }
            }
        }
    }
    private static long visible(BufferedImage image) {
        return java.util.Arrays.stream(image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())).filter(p->(p>>>24)>0).count();
    }
    private static Object field(Object target,String name) throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private static void set(Object target,String name,Object value) throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private static void close(double actual,double expected) {
        assert Math.abs(actual-expected)<1e-7 : "expected "+expected+", got "+actual;
    }
}
