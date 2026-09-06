public final class GuardianPressureTest {
    public static void main(String[] args) {
        Guardian g=new Guardian(900,550);g.activate(500,550);
        boolean charge=false,ring=false,close=false;
        for(int i=0;i<8000;i++) {
            double px=g.x()+(i<4000?600:130),py=g.y();
            g.update(0.01,px,py);
            if(g.state()==Guardian.State.TELEGRAPH) {
                double tx=g.targetX(),ty=g.targetY();
                g.update(0.01,g.x()-600,g.y());
                assert tx==g.targetX()&&ty==g.targetY() : "tells must stay committed";
                if(g.state()==Guardian.State.TELEGRAPH)assert !g.hits(tx,ty) : "warning cannot damage";
            }
            if(g.state()==Guardian.State.SLAM) {
                if(g.attack()==Guardian.Attack.CHARGE) {charge=true;assert g.hits(g.x()+70,g.y());}
                if(g.attack()==Guardian.Attack.SHOCKWAVE) {
                    ring=true;
                    assert g.hits(g.targetX()+g.waveRadius(),g.targetY());
                    if(g.waveRadius()>80)assert !g.hits(g.targetX(),g.targetY()) : "wave leaves safe interior";
                    assert !g.hits(g.targetX()+g.waveRadius()+40,g.targetY());
                }
                if(g.attack()==Guardian.Attack.SWEEP) {close=true;assert g.hits(g.targetX()+120,g.targetY());}
            }
        }
        assert charge&&ring&&close : "range-dependent attack coverage";
        System.out.println("GuardianPressureTest passed");
    }
}
