public final class EnemyProjectileTest {
    public static void main(String[] args) {
        var shot=new EnemyProjectile(300,300,1,0);
        shot.advance(.2);
        assert shot.x()>300&&shot.x()<370&&shot.y()==300;
        double x=shot.x();shot.advance(Double.NaN);assert shot.x()==x;
        for(int i=0;i<300;i++)shot.advance(.01);
        assert !shot.alive() : "hostile shot must expire";
        boolean invalid=false;
        try{new EnemyProjectile(0,0,0,0);}catch(IllegalArgumentException expected){invalid=true;}
        assert invalid;
        System.out.println("EnemyProjectileTest passed");
    }
}
