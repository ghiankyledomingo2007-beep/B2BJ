public final class Guardian {
    public enum Profile {
        OUTPOST_WARDEN("Outpost Warden", 240),
        BRIARHEART("Briarheart", 260),
        OATHKEEPER("Oathkeeper", 280),
        ICHOR_GOLEM("Ichor Golem", 320);
        private final String name;
        private final int health;
        Profile(String name,int health) { this.name=name;this.health=health; }
    }
    public enum State { DORMANT, APPROACH, TELEGRAPH, SLAM, RECOVER, HURT, DEAD }
    public enum Attack { TARGET, SWEEP, CHARGE, SHOCKWAVE, FISSURE }
    /** Row order in warden_motion.png: eight native 64px frames per row. */
    public enum Animation { IDLE, APPROACH, WINDUP, STRIKE, RECOVER, CHARGE, DEATH }
    public static final int CELL_SIZE=64, RENDER_SIZE=CELL_SIZE*3, MAX_HEALTH=240, SLAM_DAMAGE=2;
    public static final int GROUND_Y_OFFSET=48, SPRITE_FOOT_ROW=56;
    public static final double SLAM_RADIUS=112, TELEGRAPH_DURATION=0.9, SLAM_DURATION=0.18;
    public static final double RECOVER_DURATION=1.5, DEATH_DURATION=0.5;
    private double x,y,targetX,targetY,stateTime,hurtFlash,chargeX,chargeY;
    private final Profile profile;
    private int health,impactNumber;
    private State state=State.DORMANT;
    private Attack attack=Attack.TARGET, previousAttack=Attack.TARGET;
    private boolean fissureHorizontal;
    private double idleAnimationTime;

    public Guardian(double x,double y) { this(x,y,Profile.OUTPOST_WARDEN); }
    public Guardian(double x,double y,Profile profile) {
        if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Invalid boss position");
        this.profile=java.util.Objects.requireNonNull(profile);
        this.x=x;this.y=y;health=profile.health;
    }
    public void activate(double playerX,double playerY) {
        if(state!=State.DORMANT||!Double.isFinite(playerX)||!Double.isFinite(playerY))return;
        if(profile!=Profile.OUTPOST_WARDEN) {beginAttack(playerX,playerY);return;}
        targetX=playerX;targetY=playerY;state=State.TELEGRAPH;stateTime=0;
    }
    public void update(double seconds,double playerX,double playerY) {
        update(seconds,playerX,playerY,null);
    }
    public void update(double seconds,double playerX,double playerY,RuinedOutpostMap map) {
        if(!Double.isFinite(seconds)||seconds<=0||!Double.isFinite(playerX)||!Double.isFinite(playerY))return;
        // Physics uses short steps even for direct callers, not just the game loop.
        for(double left=Math.min(seconds,5);left>0.0000001;) {
            double dt=Math.min(left,0.01);tick(dt,playerX,playerY,map);left-=dt;
        }
        if(map!=null&&alive())map.setGuardianPosition(x,y);
    }
    private void tick(double dt,double px,double py,RuinedOutpostMap map) {
        hurtFlash=Math.max(0,hurtFlash-dt);
        switch(state) {
            case DORMANT -> idleAnimationTime=(idleAnimationTime+dt)%(8.0/6);
            case APPROACH -> {
                double dx=px-x,dy=py-y,length=Math.hypot(dx,dy);
                if(length>175)move(dx/length*(enraged()?185:145)*dt,dy/length*(enraged()?185:145)*dt,map,px,py);
                stateTime+=dt;
                if(length<=175||stateTime>=1.25)beginAttack(px,py);
            }
            case TELEGRAPH -> {
                // Every target is committed for the entire visible tell. No late tracking.
                stateTime+=dt;
                if(stateTime+1e-8>=telegraphDuration()) {
                    state=State.SLAM;stateTime=0;impactNumber++;
                }
            }
            case SLAM -> {
                if(attack==Attack.CHARGE)move(chargeX*620*dt,chargeY*620*dt,map,px,py);
                stateTime+=dt;
                if(stateTime+1e-8>=activeDuration()) {state=State.RECOVER;stateTime=0;}
            }
            case RECOVER,HURT -> {
                stateTime+=dt;
                if(stateTime+1e-8>=(enraged()?1.25:RECOVER_DURATION)) {state=State.APPROACH;stateTime=0;}
            }
            case DEAD -> stateTime+=dt;
        }
    }
    private void beginAttack(double px,double py) {
        double distance=Math.hypot(px-x,py-y);
        if(profile==Profile.BRIARHEART) {
            attack=impactNumber%3==2?Attack.SHOCKWAVE:Attack.TARGET;
        }
        else if(profile==Profile.OATHKEEPER) {
            attack=impactNumber%3==2?Attack.FISSURE:distance>200?Attack.CHARGE:Attack.SWEEP;
            if(attack==Attack.FISSURE)fissureHorizontal=!fissureHorizontal;
        }
        else if(profile==Profile.ICHOR_GOLEM) {
            attack=impactNumber%3==2?Attack.FISSURE:impactNumber%3==1?Attack.TARGET:Attack.SHOCKWAVE;
            if(attack==Attack.FISSURE)fissureHorizontal=!fissureHorizontal;
        }
        else if(enraged()&&impactNumber%4==3) {
            attack=Attack.FISSURE;fissureHorizontal=!fissureHorizontal;
        }
        else if(previousAttack!=Attack.SHOCKWAVE&&impactNumber%3==2)attack=Attack.SHOCKWAVE;
        else if(distance>260&&previousAttack!=Attack.CHARGE)attack=Attack.CHARGE;
        else if(distance<190&&previousAttack!=Attack.SWEEP)attack=Attack.SWEEP;
        else attack=Attack.TARGET;
        previousAttack=attack;
        targetX=(attack==Attack.SWEEP||attack==Attack.SHOCKWAVE)?x:px;
        targetY=(attack==Attack.SWEEP||attack==Attack.SHOCKWAVE)?y:py;
        double length=Math.max(1,Math.hypot(px-x,py-y));
        chargeX=(px-x)/length;chargeY=(py-y)/length;
        state=State.TELEGRAPH;stateTime=0;
    }
    private void move(double dx,double dy,RuinedOutpostMap map,double px,double py) {
        // Conservative body footprint; never phase through terrain or push player into a wall.
        double nx=x+dx,ny=y+dy;
        if(Math.hypot(nx-px,ny+32-(py+Player.COLLISION_Y_OFFSET))<84)return;
        if(map==null||!map.waterBlocked(nx,ny+32,60)) {x=nx;y=ny;}
    }
    public boolean hits(double px,double py) {
        if(state!=State.SLAM)return false;
        if(attack==Attack.FISSURE) {
            var lane=fissureBounds();
            return Math.abs(px-lane.centerX())<=lane.width()/2
                    &&Math.abs(py-lane.centerY())<=lane.height()/2;
        }
        if(attack==Attack.CHARGE)return Math.hypot(px-x,py-y)<=92;
        double distance=Math.hypot(px-targetX,py-targetY);
        if(attack==Attack.SHOCKWAVE)return Math.abs(distance-waveRadius())<=28;
        return distance<=slamRadius();
    }
    public double waveRadius() {return 32+(slamRadius()-32)*Math.min(1,stateTime/activeDuration());}
    public double activeDuration() {return attack==Attack.FISSURE?0.32:attack==Attack.CHARGE?0.55:attack==Attack.SHOCKWAVE?0.75:SLAM_DURATION;}
    public double telegraphDuration() {
        if(attack==Attack.FISSURE)return 1.1;
        return profile==Profile.BRIARHEART?1.05:profile==Profile.OATHKEEPER?.8:TELEGRAPH_DURATION;
    }
    /** The exact committed damage rectangle, also used to render its complete warning. */
    public RuinedOutpostMap.Obstacle fissureBounds() {
        if(attack!=Attack.FISSURE||(state!=State.TELEGRAPH&&state!=State.SLAM))return null;
        return new RuinedOutpostMap.Obstacle(targetX,targetY,fissureHorizontal?1152:84,fissureHorizontal?84:1152);
    }
    public boolean hitFrom(double ax,double ay,int fx,int fy,double range,int damage) {
        double length=Math.hypot(fx,fy);
        if(state==State.DORMANT||!alive()||damage<=0||range<=0||length==0)return false;
        double dx=x-ax,dy=y-ay,forward=(dx*fx+dy*fy)/length,sideways=Math.abs(dx*fy-dy*fx)/length;
        return forward>=0&&forward<=range&&sideways<=range*0.75&&hurt(damage);
    }
    public boolean hurt(int damage) {
        if(state==State.DORMANT||!alive()||damage<=0)return false;
        health=Math.max(0,health-damage);hurtFlash=0.18;
        if(health==0){state=State.DEAD;stateTime=0;}
        return true;
    }
    public boolean enraged(){return health<=maxHealth()/2;}
    public Profile profile(){return profile;}
    public int maxHealth(){return profile.health;}
    public String bossName(){return profile.name;}
    public double auraRadius(){return profile==Profile.ICHOR_GOLEM&&alive()&&state!=State.DORMANT?(enraged()?340:280):0;}
    public double drainPerSecond(){return auraRadius()>0?(enraged()?5:3):0;}
    public Attack attack(){return attack;}
    public double directionX(){return chargeX;}
    public double directionY(){return chargeY;}
    public double stateSeconds(){return stateTime;}
    public Animation animation() {
        return switch(state) {
            case DORMANT -> Animation.IDLE;
            case APPROACH -> Animation.APPROACH;
            case TELEGRAPH -> Animation.WINDUP;
            case SLAM -> attack==Attack.CHARGE?Animation.CHARGE:Animation.STRIKE;
            case RECOVER,HURT -> Animation.RECOVER;
            case DEAD -> Animation.DEATH;
        };
    }
    public int animationFrame() {
        if(state==State.DORMANT)return (int)(idleAnimationTime*6)%8;
        if(state==State.APPROACH)return (int)(stateTime*8)%8;
        double duration=switch(state) {
            case TELEGRAPH -> telegraphDuration();
            case SLAM -> activeDuration();
            case RECOVER,HURT -> enraged()?1.25:RECOVER_DURATION;
            case DEAD -> DEATH_DURATION;
            default -> throw new IllegalStateException("Looping animation already handled");
        };
        return Math.min(7,(int)(stateTime*8/duration));
    }
    public int row(){return state==State.SLAM?1:state==State.RECOVER||state==State.HURT||state==State.DEAD?2:0;}
    public int frame(){
        double duration=switch(state) {
            case TELEGRAPH -> telegraphDuration();
            case SLAM -> activeDuration();
            case RECOVER,HURT -> enraged()?1.25:RECOVER_DURATION;
            case DEAD -> DEATH_DURATION;
            default -> 1;
        };
        return Math.min(3,(int)(stateTime*4/duration));
    }
    public boolean flashVisible(){return hurtFlash==0||(int)(hurtFlash*30)%2==0;}
    public double renderScale(){return state==State.DEAD?Math.max(0,1-stateTime/DEATH_DURATION):1;}
    public boolean alive(){return state!=State.DEAD;}
    public boolean visible(){return state!=State.DEAD||stateTime<DEATH_DURATION-1e-8;}
    public State state(){return state;}
    public int health(){return health;}
    public int impactNumber(){return impactNumber;}
    public double targetX(){return targetX;}
    public double targetY(){return targetY;}
    public double slamRadius(){return switch(attack){
        case SWEEP -> 165;
        case SHOCKWAVE -> profile==Profile.BRIARHEART?(enraged()?420:340):enraged()?440:360;
        default -> profile==Profile.BRIARHEART?140:SLAM_RADIUS;
    };}
    public double x(){return x;}
    public double y(){return y;}
}
