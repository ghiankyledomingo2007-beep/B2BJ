import java.util.ArrayList;
import java.util.List;

/** Short-lived world-space labels, capped even during a crowded cleave. */
final class DamageNumbers {
    static final int LIMIT=48;
    static final double LIFE=.9;
    static final class Number {
        final double x,y;
        final int damage;
        final boolean critical,incoming,gain;
        double age;
        Number(double x,double y,int damage,boolean critical,boolean incoming) { this(x,y,damage,critical,incoming,false); }
        Number(double x,double y,int damage,boolean critical,boolean incoming,boolean gain) {
            this.x=x;this.y=y;this.damage=damage;this.critical=critical;this.incoming=incoming;this.gain=gain;
        }
    }
    private final List<Number> numbers=new ArrayList<>();
    private int lane;
    void add(double x,double y,int damage,boolean critical,boolean incoming) {
        if(damage<=0||!Double.isFinite(x)||!Double.isFinite(y))return;
        if(numbers.size()>=LIMIT)numbers.remove(0);
        numbers.add(new Number(x+(lane++%3-1)*18,y-52,damage,critical,incoming));
    }
    /** Ichor taken in, shown as +N above the gulp (Beelzebub's devour). */
    void addGain(double x,double y,int amount) {
        if(amount<=0||!Double.isFinite(x)||!Double.isFinite(y))return;
        if(numbers.size()>=LIMIT)numbers.remove(0);
        numbers.add(new Number(x,y-36,amount,false,false,true));
    }
    void update(double seconds) {
        if(!Double.isFinite(seconds)||seconds<=0)return;
        numbers.removeIf(n->{n.age+=seconds;return n.age>=LIFE;});
    }
    List<Number> active() { return java.util.Collections.unmodifiableList(numbers); }
    void clear() { numbers.clear();lane=0; }
}
