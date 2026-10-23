import java.awt.event.ActionEvent;

public final class EightWayAnimationTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        int[][] dirs={{1,1,4},{1,-1,5},{-1,-1,6},{-1,1,7}};
        var field=B2BJ.class.getDeclaredField("bladeAnimation");field.setAccessible(true);
        for(var d:dirs) {
            var player=new Player(900,550);player.collectIchor(100);assert player.transform();
            var game=new RuinedOutpostGame(new RuinedOutpostMap(5),player,java.util.List.of(),new Guardian(1500,850));game.begin();
            var panel=new B2BJ(false,game);panel.setSize(1280,720);
            String horizontal=d[0]>0?"right":"left",vertical=d[1]>0?"down":"up";
            key(panel,horizontal+"Pressed");key(panel,vertical+"Pressed");panel.step(.02);
            var animation=(BladeAnimation)field.get(panel);
            assert animation.directionRow()==d[2] : "two movement keys select the actual diagonal row";
            double dx=player.x()-900,dy=player.y()-550;
            assert dx*d[0]>0&&dy*d[1]>0&&Math.abs(Math.abs(dx)-Math.abs(dy))<1e-8;
            key(panel,horizontal+"Released");key(panel,vertical+"Released");panel.step(.14);panel.step(.14);
            assert animation.directionRow()==d[2] : "idle retains the diagonal after release";
            key(panel,"dashPressed");panel.step(.02);
            assert animation.directionRow()==d[2]&&animation.action()==BladeAnimation.Action.DASH;
            assert game.dashAimX()*d[0]>0&&game.dashAimY()*d[1]>0 : "standing dash follows the retained diagonal";
            for(var action:BladeAnimation.Action.values()) {
                var sheet=B2BJ.loadImage("assets/characters/blade/rainoray_"+action.name().toLowerCase(java.util.Locale.ROOT)+".png");
                assert B2BJ.bladeFacings(sheet,action)==8 : action+" needs all eight views";
            }
        }
        for(int[] d:new int[][]{{0,1,0},{1,0,1},{0,-1,2},{-1,0,3}})
            assert BladeAnimation.directionRow(d[0],d[1])==d[2] : "cardinal row order stays compatible";
        System.out.println("EightWayAnimationTest passed");
    }
    private static void key(B2BJ panel,String name) {panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel,0,name));}
}
