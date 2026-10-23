import java.awt.image.BufferedImage;

public final class CombatUiTest {
    public static void main(String[] args) {
        var ui=new CombatUi();var enemy=new Wisp(200,200,200,200,72);
        assert visible(draw(ui,enemy,false,0,false))==0 : "unhurt idle enemies do not carry bars";
        assert visible(draw(ui,enemy,true,0,false))>0 : "hover identifies a healthy enemy";
        enemy.hurt(24);
        var hit=draw(ui,enemy,true,.01,false);
        assert gold(hit)>0 : "damage leaves a short contrasting trail";
        var settled=draw(ui,enemy,false,1,false);
        assert gold(settled)==0 : "the damage trail drains instead of overstating health";
        enemy.hurt(12);
        assert gold(draw(ui,enemy,false,1.01,true))==0 : "reduced effects shows immediate health";
        for(int[] d:new int[][]{{1,1},{1,-1},{-1,-1},{-1,1}}) {
            var image=new BufferedImage(200,200,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            CombatUi.direction(g,100,100,d[0],d[1]);g.dispose();
            assert visible(image)>0;
            for(int y=0;y<200;y++)for(int x=0;x<200;x++)if((image.getRGB(x,y)>>>24)>0)
                assert (x-100)*d[0]>0&&(y-100)*d[1]>0 : "diagonal cue points into the correct quadrant";
        }
        System.out.println("CombatUiTest passed");
    }
    private static BufferedImage draw(CombatUi ui,Wisp enemy,boolean target,double time,boolean reduced) {
        var image=new BufferedImage(200,100,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        ui.health(g,enemy,100,50,target,time,reduced);g.dispose();return image;
    }
    private static int visible(BufferedImage image) {
        return (int)java.util.Arrays.stream(image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())).filter(p->(p>>>24)>0).count();
    }
    private static int gold(BufferedImage image) {
        return (int)java.util.Arrays.stream(image.getRGB(0,50,200,10,null,0,200)).filter(p->p==Ui.GOLD_LIGHT.getRGB()).count();
    }
}
