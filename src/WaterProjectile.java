/** A finite packet of water momentum; simulation owns collisions, renderer owns art. */
public final class WaterProjectile {
    private double x, y, speed, age, distance;
    private final double dx, dy;
    private final boolean heavy;
    private boolean alive = true;

    WaterProjectile(double x,double y,int aimX,int aimY,boolean heavy) {
        double length=Math.hypot(aimX,aimY);
        if(!Double.isFinite(x)||!Double.isFinite(y)||length==0) throw new IllegalArgumentException("Invalid water cast");
        this.x=x; this.y=y; dx=aimX/length; dy=aimY/length; this.heavy=heavy;
        speed=heavy?440:620;
    }
    void advance(double dt) {
        double travel=speed*(-Math.expm1(-0.5*dt))/0.5;
        x+=dx*travel; y+=dy*travel; distance+=travel;
        speed*=Math.exp(-0.5*dt); age+=dt;
        if(distance>=(heavy?280:340)||age>=0.8) alive=false;
    }
    void stop() { alive=false; }
    public double x() { return x; }
    public double y() { return y; }
    public double speed() { return speed; }
    public double age() { return age; }
    public double opacity() { return Math.max(0,Math.min(1,((heavy?280:340)-distance)/60)); }
    public double directionX() { return dx; }
    public double directionY() { return dy; }
    public double radius() { return heavy?28:16; }
    public boolean heavy() { return heavy; }
    public boolean alive() { return alive; }
    public int damage() { return heavy?2:1; }
    public double impulse() { return heavy?420:140; }
}
