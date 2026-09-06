/** A committed, non-homing hostile glob. Game simulation resolves swept collisions. */
public final class EnemyProjectile {
    public static final double SPEED=250, RADIUS=10, LIFETIME=1.6;
    public static final double MAX_TRAVEL=SPEED*LIFETIME;
    private final double dx,dy;
    private double x,y,age;
    private boolean alive=true;
    EnemyProjectile(double x,double y,double aimX,double aimY) {
        double length=Math.hypot(aimX,aimY);
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(length)||length==0)
            throw new IllegalArgumentException("Invalid hostile shot");
        this.x=x;this.y=y;dx=aimX/length;dy=aimY/length;
    }
    void advance(double dt) {
        if(!alive||!Double.isFinite(dt)||dt<=0)return;
        double active=Math.min(dt,LIFETIME-age);
        x+=dx*SPEED*active;y+=dy*SPEED*active;age+=active;
        if(age>=LIFETIME-1e-8)alive=false;
    }
    void stop(){alive=false;}
    public double x(){return x;}
    public double y(){return y;}
    public double directionX(){return dx;}
    public double directionY(){return dy;}
    public double age(){return age;}
    public boolean alive(){return alive;}
}
