/** A finite packet of water momentum; simulation owns collisions, renderer owns art. */
public final class WaterProjectile {
    public enum Kind { CUT, RETURN_CUT, FINISHER, COUNTER, TIDE }
    private double x, y, speed, age, distance;
    private final double dx, dy;
    private final Kind kind;
    private final boolean jet;
    private final java.util.Set<Wisp> hitBodies = new java.util.HashSet<>();
    private boolean alive = true;

    WaterProjectile(double x,double y,int aimX,int aimY,boolean heavy) {
        this(x,y,aimX,aimY,heavy?Kind.TIDE:Kind.CUT);
    }
    WaterProjectile(double x,double y,int aimX,int aimY,Kind kind) {
        this(x,y,aimX,aimY,kind,false);
    }
    WaterProjectile(double x,double y,int aimX,int aimY,Kind kind,boolean jet) {
        double length=Math.hypot(aimX,aimY);
        if(!Double.isFinite(x)||!Double.isFinite(y)||length==0) throw new IllegalArgumentException("Invalid water cast");
        this.x=x; this.y=y; dx=aimX/length; dy=aimY/length; this.kind=java.util.Objects.requireNonNull(kind);this.jet=jet;
        speed=switch(kind) { case TIDE->440; case FINISHER->540; case COUNTER->780; default->620; };
        if(jet)speed*=1.3;
    }
    void advance(double dt) {
        if(!Double.isFinite(dt)||dt<=0||!alive)return;
        double travel=speed*(-Math.expm1(-0.5*dt))/0.5;
        // Analytic side sweep stays frame-rate independent and returns to the aimed centre line.
        double bend=(kind==Kind.CUT?6:kind==Kind.RETURN_CUT?-6:0)
                *(Math.sin(Math.PI*(distance+travel)/range())-Math.sin(Math.PI*distance/range()));
        x+=dx*travel-dy*bend; y+=dy*travel+dx*bend; distance+=travel;
        speed*=Math.exp(-0.5*dt); age+=dt;
        if(distance>=range()||age>=0.8) alive=false;
    }
    void stop() { alive=false; }
    public double x() { return x; }
    public double y() { return y; }
    public double speed() { return speed; }
    public double age() { return age; }
    public double opacity() { return Math.max(0,Math.min(1,(range()-distance)/60)); }
    public double directionX() { return dx; }
    public double directionY() { return dy; }
    public double radius() { return heavy()?28:kind==Kind.FINISHER?22:16; }
    public double halfWidth() { return heavy()?56:radius(); }
    boolean touches(double targetX,double targetY,double targetRadius) {
        double forward=(targetX-x)*dx+(targetY-y)*dy;
        double side=(targetX-x)*dy-(targetY-y)*dx;
        return Math.pow(forward/(radius()+targetRadius),2)+Math.pow(side/(halfWidth()+targetRadius),2)<=1;
    }
    boolean firstContact(Wisp target) { return hitBodies.add(target); }
    private double range() { return switch(kind) { case TIDE->280; case FINISHER->270; case COUNTER->300; default->340; }; }
    public Kind kind() { return kind; }
    public boolean jet() { return jet; }
    public boolean heavy() { return kind==Kind.TIDE; }
    public boolean staggers() { return heavy()||kind==Kind.FINISHER||kind==Kind.COUNTER; }
    public boolean alive() { return alive; }
    public int damage() { return heavy()||kind==Kind.FINISHER?2:1; }
    public double impulse() { return switch(kind) { case TIDE->420; case FINISHER->300; case COUNTER->220; default->140; }; }
}
