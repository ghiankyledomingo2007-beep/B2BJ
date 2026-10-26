/** A released mire bloom: its caster's fixed ground tell precedes this lingering danger. */
public final class ForestHazard {
    private final double x,y,radius,duration;
    private double age,hitCooldown;
    ForestHazard(double x,double y,double radius,double duration) {
        if(!Double.isFinite(x+y+radius+duration)||radius<=0||duration<=0)throw new IllegalArgumentException("Invalid forest hazard");
        this.x=x;this.y=y;this.radius=radius;this.duration=duration;
    }
    void update(double seconds) {
        if(!Double.isFinite(seconds)||seconds<=0)return;
        age=Math.min(duration,age+seconds);hitCooldown=Math.max(0,hitCooldown-seconds);
    }
    boolean touches(double px,double py) {return alive()&&Math.hypot(px-x,py-y)<=radius;}
    boolean hit(double px,double py) {
        if(hitCooldown>0||!touches(px,py))return false;
        hitCooldown=.9;return true;
    }
    public boolean alive(){return age<duration;}
    public double x(){return x;}
    public double y(){return y;}
    public double radius(){return radius;}
    public double age(){return age;}
    public double remaining(){return duration-age;}
}
