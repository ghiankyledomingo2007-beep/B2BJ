import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class RemnantAttackTest {
    public static void main(String[] args) throws Exception {
        for(int heading=0;heading<8;heading++)for(boolean rush:new boolean[]{false,true}) {
            double angle=heading*Math.PI/4,dx=Math.cos(angle),dy=Math.sin(angle),range=rush?200:100;
            var enemy=new CampaignEnemy(CampaignEnemy.Kind.OUTPOST_SCOUT,500,500);
            var body=enemy.body();double tx=500+dx*range,ty=500+dy*range;
            enemy.update(.01,tx,ty,null);
            assert body.state()==Wisp.State.TELEGRAPH&&body.remnantRush()==rush;
            assert !enemy.hits(500,500) : "windup cannot hurt";
            enemy.update(rush?.47:.32,tx,ty,null);
            assert body.directionLocked();
            double reach=body.lungeReach();
            while(body.state()==Wisp.State.TELEGRAPH)enemy.update(.01,500-dx*100,500-dy*100,null);
            assert body.remnantRush()==rush : "moving closer must not change an announced attack";
            assert body.intentX()*dx+body.intentY()*dy>.99 : "committed attacks cannot follow a sidestep";
            double x=body.x(),y=body.y();
            while(body.state()==Wisp.State.LUNGE)enemy.update(.01,500-dx*100,500-dy*100,null);
            assert Math.abs(Math.hypot(body.x()-x,body.y()-y)-reach)<.01 : "travel must match the advertised reach";
            assert !enemy.hits(body.x(),body.y())&&body.state()==Wisp.State.RECOVER;
            assert body.recoverDuration()==(rush?1.0:.7);
            for(double t=0;t<body.recoverDuration()-.025;t+=.01)enemy.update(.01,body.x()+20,body.y(),null);
            assert body.state()==Wisp.State.RECOVER : "leave the full punish window";
        }
        var waiting=new Wisp(500,500,450,550,64,Wisp.Role.REMNANT);waiting.waitForTurn(true);
        waiting.update(.01,700,500);assert !waiting.attacking() : "rushes must respect shared attack slots";
        waiting.waitForTurn(false);waiting.update(.01,700,500);
        assert waiting.remnantRush();waiting.hurt(1,true);assert waiting.state()==Wisp.State.HURT;
        for(var kind:new CampaignEnemy.Kind[]{CampaignEnemy.Kind.RIFT_IMP,CampaignEnemy.Kind.OUTPOST_GUARD}) {
            var other=new CampaignEnemy(kind,500,500);other.update(.01,700,500,null);
            assert !other.body().remnantRush() : "only outpost slimes gain this move";
        }
        silhouettes();
        System.out.println("RemnantAttackTest passed: two moves, eight directions, lock, reach, recovery, interruption and distinct silhouettes");
    }

    private static void silhouettes() throws Exception {
        var panel=new B2BJ(false);
        var draw=B2BJ.class.getDeclaredMethod("drawWisp",Graphics2D.class,Wisp.class,int.class,int.class);draw.setAccessible(true);
        int[][] pixels=new int[2][];
        for(int move=0;move<2;move++) {
            var enemy=new CampaignEnemy(CampaignEnemy.Kind.OUTPOST_SCOUT,200,200);
            enemy.update(.01,move==0?300:400,200,null);
            for(int i=0;i<2;i++)enemy.update(enemy.body().telegraphDuration()*.4,move==0?300:400,200,null);
            var image=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            draw.invoke(panel,g,enemy.body(),0,0);g.dispose();
            pixels[move]=image.getRGB(0,0,400,400,null,0,400);
        }
        assert !Arrays.equals(pixels[0],pixels[1]) : "pounce and rush need different visible windups";
    }
}
