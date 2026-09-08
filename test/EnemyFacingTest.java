public final class EnemyFacingTest {
    public static void main(String[] args) {
        double[][] headings={{1,.15},{1,-.15},{-1,.15},{-1,-.15},{.15,1},{-.15,1},{.15,-1},{-.15,-1}};
        int cases=0;
        for(var role:Wisp.Role.values())for(var heading:headings) {
            double dx=heading[0],dy=heading[1];
            int row=Math.abs(dx)>Math.abs(dy)?(dx>0?1:3):(dy>0?0:2);
            var body=new Wisp(500,500,400,600,4,role);
            body.update(.01,500+dx*350,500+dy*350);
            assert body.state()==Wisp.State.PURSUE;
            assert body.animation().row()==row : role+" pursuit lost dominant facing "+dx+","+dy;
            double range=role==Wisp.Role.SPITTER?200:80;
            double tx=body.x()+dx*range,ty=body.y()+dy*range;
            for(int i=0;i<1000&&body.state()!=Wisp.State.TELEGRAPH;i++)body.update(.01,tx,ty);
            assert body.state()==Wisp.State.TELEGRAPH;
            assert body.animation().row()==row : "windup faces its real target";
            for(int i=0;i<1000&&!body.directionLocked();i++)body.update(.01,tx,ty);
            assert body.directionLocked();
            tx=body.x()-dx*range;ty=body.y()-dy*range;
            for(int i=0;i<1000&&body.state()!=Wisp.State.LUNGE;i++)body.update(.01,tx,ty);
            assert body.state()==Wisp.State.LUNGE;
            assert body.animation().row()==row : "locked tell cannot turn its sprite behind";
            body.update(.04,tx,ty);
            assert body.animation().row()==row : "release retains the committed dominant axis";
            cases++;
        }
        System.out.println("EnemyFacingTest passed: "+cases+" role/heading scenarios");
    }
}
