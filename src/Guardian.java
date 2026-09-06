public final class Guardian {
    public enum State { DORMANT, APPROACH, TELEGRAPH, SLAM, RECOVER, HURT, DEAD }
    public enum Attack { TARGET, SWEEP, CHARGE, SHOCKWAVE }
    public static final int CELL_SIZE=64, RENDER_SIZE=CELL_SIZE*3, MAX_HEALTH=240, SLAM_DAMAGE=2;
    public static final int GROUND_Y_OFFSET=48, SPRITE_FOOT_ROW=56;
    public static final double SLAM_RADIUS=112, TELEGRAPH_DURATION=0.9, SLAM_DURATION=0.18;
    public static final double RECOVER_DURATION=1.5, DEATH_DURATION=0.5;
    private double x,y,targetX,targetY,stateTime,hurtFlash,chargeX,chargeY;
    private int health=MAX_HEALTH,impactNumber;
    private State state=State.DORMANT;
    private Attack attack=Attack.TARGET, previousAttack=Attack.TARGET;

    public Guardian(double x,double y) { this.x=x;this.y=y; }
    public void activate(double playerX,double playerY) {
        if(state!=State.DORMANT)return;
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
            case DORMANT -> { }
            case APPROACH -> {
                double dx=px-x,dy=py-y,length=Math.hypot(dx,dy);
                if(length>175)move(dx/length*(enraged()?185:145)*dt,dy/length*(enraged()?185:145)*dt,map,px,py);
                stateTime+=dt;
                if(length<=175||stateTime>=1.25)beginAttack(px,py);
            }
            case TELEGRAPH -> {
                // Every target is committed for the entire visible tell. No late tracking.
                stateTime+=dt;
                if(stateTime+1e-8>=TELEGRAPH_DURATION) {
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
        if(previousAttack!=Attack.SHOCKWAVE&&impactNumber%3==2)attack=Attack.SHOCKWAVE;
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
        if(attack==Attack.CHARGE)return Math.hypot(px-x,py-y)<=92;
        double distance=Math.hypot(px-targetX,py-targetY);
        if(attack==Attack.SHOCKWAVE)return Math.abs(distance-waveRadius())<=28;
        return distance<=slamRadius();
    }
    public double waveRadius() {return 32+(slamRadius()-32)*Math.min(1,stateTime/activeDuration());}
    public double activeDuration() {return attack==Attack.CHARGE?0.55:attack==Attack.SHOCKWAVE?0.75:SLAM_DURATION;}
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
    public boolean enraged(){return health<=MAX_HEALTH/2;}
    public Attack attack(){return attack;}
    public double directionX(){return chargeX;}
    public double directionY(){return chargeY;}
    public double stateSeconds(){return stateTime;}
    public int row(){return state==State.SLAM?1:state==State.RECOVER||state==State.HURT||state==State.DEAD?2:0;}
    public int frame(){
        double duration=switch(state) {
            case TELEGRAPH -> TELEGRAPH_DURATION;
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
    public double slamRadius(){return switch(attack){case SWEEP -> 165;case SHOCKWAVE -> enraged()?440:360;default -> SLAM_RADIUS;};}
    public double x(){return x;}
    public double y(){return y;}
}
