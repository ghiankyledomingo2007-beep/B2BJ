public final class Guardian {
    public enum Profile {
        OUTPOST_WARDEN("Outpost Warden", 1200),
        BRIARHEART("Briarheart", 1300),
        OATHKEEPER("Oathkeeper", 1400),
        ICHOR_GOLEM("Ichor Golem", 1600);
        private final String name;
        private final int health;
        Profile(String name,int health) { this.name=name;this.health=health; }
    }
    public enum State { DORMANT, APPROACH, TELEGRAPH, SLAM, RECOVER, HURT, ENRAGE, DEAD }
    public enum Attack { TARGET, SWEEP, CHARGE, SHOCKWAVE, FISSURE }
    public enum ForestAttack { ROOT_WEAVE, SEED_BLOOM, BRAMBLE_CAGE }
    public record Bloom(double x,double y,double radius) {}
    /** Row order in warden_motion.png: eight 128 px frames per row (drawn at 2x). */
    public enum Animation { IDLE, APPROACH, WINDUP, STRIKE, RECOVER, CHARGE, DEATH }
    public static final int CELL_SIZE=64, RENDER_SIZE=CELL_SIZE*4, MAX_HEALTH=1200, SLAM_DAMAGE=35;
    public static final int GROUND_Y_OFFSET=48, SPRITE_FOOT_ROW=56;
    public static final double SLAM_RADIUS=112, TELEGRAPH_DURATION=0.9, SLAM_DURATION=0.18;
    public static final double RECOVER_DURATION=1.5, DEATH_DURATION=0.5;
    public static final double ENRAGE_DURATION=1.8;
    /** Shared with the attack warnings, so a balance change can never make a warning lie. */
    public static final double CHARGE_SPEED=620, CHARGE_DURATION=0.55, CHARGE_RADIUS=92, WAVE_START=32, WAVE_HALF_WIDTH=28;
    private double x,y,targetX,targetY,stateTime,hurtFlash,chargeX,chargeY;
    private final Profile profile;
    private int health,impactNumber;
    private State state=State.DORMANT;
    private Attack attack=Attack.TARGET, previousAttack=Attack.TARGET;
    private boolean fissureHorizontal;
    private boolean phaseTwo;
    private ForestAttack forestAttack=ForestAttack.ROOT_WEAVE;
    private java.util.List<RuinedOutpostMap.Obstacle> roots=java.util.List.of();
    private java.util.List<Bloom> seeds=java.util.List.of();
    private int forestGap;
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
                if(profile==Profile.BRIARHEART) {beginAttack(px,py,map);break;}
                double dx=px-x,dy=py-y,length=Math.hypot(dx,dy);
                if(length>175)move(dx/length*(enraged()?185:145)*dt,dy/length*(enraged()?185:145)*dt,map,px,py);
                stateTime+=dt;
                if(length<=175||stateTime>=1.25)beginAttack(px,py,map);
            }
            case TELEGRAPH -> {
                // Every target is committed for the entire visible tell. No late tracking.
                stateTime+=dt;
                if(stateTime+1e-8>=telegraphDuration()) {
                    state=State.SLAM;stateTime=0;impactNumber++;
                }
            }
            case SLAM -> {
                if(profile!=Profile.BRIARHEART&&attack==Attack.CHARGE)move(chargeX*CHARGE_SPEED*dt,chargeY*CHARGE_SPEED*dt,map,px,py);
                stateTime+=dt;
                if(stateTime+1e-8>=activeDuration()) {state=State.RECOVER;stateTime=0;}
            }
            case RECOVER,HURT -> {
                stateTime+=dt;
                if(stateTime+1e-8>=recoverDuration()) {
                    state=(profile==Profile.OUTPOST_WARDEN||profile==Profile.BRIARHEART)&&!phaseTwo&&health<=maxHealth()/2?State.ENRAGE:State.APPROACH;
                    stateTime=0;
                }
            }
            case ENRAGE -> {
                stateTime+=dt;
                if(stateTime+1e-8>=ENRAGE_DURATION) {phaseTwo=true;beginAttack(px,py,map);}
            }
            case DEAD -> stateTime+=dt;
        }
    }
    private void beginAttack(double px,double py) {
        beginAttack(px,py,null);
    }
    private void beginAttack(double px,double py,RuinedOutpostMap map) {
        double distance=Math.hypot(px-x,py-y);
        if(profile==Profile.BRIARHEART) {
            beginForestAttack(px,py,map);return;
        }
        else if(profile==Profile.OATHKEEPER) {
            attack=impactNumber%3==2?Attack.FISSURE:distance>200?Attack.CHARGE:Attack.SWEEP;
            if(attack==Attack.FISSURE)fissureHorizontal=!fissureHorizontal;
        }
        else if(profile==Profile.ICHOR_GOLEM) {
            attack=impactNumber%3==2?Attack.FISSURE:impactNumber%3==1?Attack.TARGET:Attack.SHOCKWAVE;
            if(attack==Attack.FISSURE)fissureHorizontal=!fissureHorizontal;
        }
        else if(enraged()&&(state==State.ENRAGE||impactNumber%4==3)) {
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
    private void beginForestAttack(double px,double py,RuinedOutpostMap map) {
        forestAttack=ForestAttack.values()[impactNumber%3];
        targetX=px;targetY=py;
        var lanes=new java.util.ArrayList<RuinedOutpostMap.Obstacle>();
        var blooms=new java.util.ArrayList<Bloom>();
        if(forestAttack==ForestAttack.BRAMBLE_CAGE) {
            // Keep the opening on a walkable route, including when the player hugs the bank.
            double best=Double.POSITIVE_INFINITY;boolean found=false;
            for(int side=0;side<4;side++) {
                double ex=px+gapX(side)*230,ey=py+gapY(side)*230;
                if(map!=null&&!map.clearWaterLine(px,py+Player.COLLISION_Y_OFFSET,
                        ex,ey+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))continue;
                double distance=Math.hypot(ex-x,ey-y);
                if(distance<best) {best=distance;forestGap=side;found=true;}
            }
            if(!found)forestAttack=ForestAttack.ROOT_WEAVE;
            else for(int side=0;side<4;side++)if(side!=forestGap)
                lanes.add(new RuinedOutpostMap.Obstacle(px+gapX(side)*160,py+gapY(side)*160,
                        side%2==0?370:50,side%2==0?50:370));
        }
        if(forestAttack==ForestAttack.ROOT_WEAVE) {
            boolean horizontal=(impactNumber/3)%2==0;
            int count=phaseTwo?4:3;
            for(int i=0;i<count;i++) {
                double offset=(i-(count-1)/2.)*160;
                lanes.add(new RuinedOutpostMap.Obstacle(px+(horizontal?0:offset),py+(horizontal?offset:0),
                        horizontal?650:70,horizontal?70:650));
            }
        } else if(forestAttack==ForestAttack.SEED_BLOOM) {
            blooms.add(new Bloom(px,py,72));
            int count=phaseTwo?4:2;
            for(int i=0;i<count;i++) {
                double angle=i*2*Math.PI/count+(impactNumber/3)*Math.PI/2;
                blooms.add(new Bloom(px+Math.cos(angle)*190,py+Math.sin(angle)*190,72));
            }
        }
        roots=java.util.List.copyOf(lanes);seeds=java.util.List.copyOf(blooms);
        state=State.TELEGRAPH;stateTime=0;
    }
    private static int gapX(int side){return side==1?1:side==3?-1:0;}
    private static int gapY(int side){return side==0?-1:side==2?1:0;}
    public ForestAttack forestAttack(){return forestAttack;}
    public int forestGap(){return forestGap;}
    public java.util.List<RuinedOutpostMap.Obstacle> forestRoots(){
        return profile==Profile.BRIARHEART&&(state==State.TELEGRAPH||state==State.SLAM)?roots:java.util.List.of();
    }
    public java.util.List<Bloom> forestSeeds(){
        return profile==Profile.BRIARHEART&&(state==State.TELEGRAPH||state==State.SLAM)?seeds:java.util.List.of();
    }
    public boolean coreExposed(){
        return state==State.RECOVER&&(profile!=Profile.BRIARHEART||impactNumber%3==0);
    }
    private void move(double dx,double dy,RuinedOutpostMap map,double px,double py) {
        // Conservative body footprint; never phase through terrain or push player into a wall.
        double nx=x+dx,ny=y+dy;
        if(Math.hypot(nx-px,ny+32-(py+Player.COLLISION_Y_OFFSET))<84)return;
        if(map==null||!map.waterBlocked(nx,ny+32,60)) {x=nx;y=ny;}
    }
    public boolean hits(double px,double py) {
        if(state!=State.SLAM)return false;
        if(profile==Profile.BRIARHEART) {
            for(var root:roots)if(Math.abs(px-root.centerX())<=root.width()/2
                    &&Math.abs(py-root.centerY())<=root.height()/2)return true;
            return forestAttack==ForestAttack.BRAMBLE_CAGE&&stateTime>=.9&&Math.hypot(px-targetX,py-targetY)<=130;
        }
        if(attack==Attack.FISSURE) {
            var lane=fissureBounds();
            return Math.abs(px-lane.centerX())<=lane.width()/2
                    &&Math.abs(py-lane.centerY())<=lane.height()/2;
        }
        if(attack==Attack.CHARGE)return Math.hypot(px-x,py-y)<=CHARGE_RADIUS;
        double distance=Math.hypot(px-targetX,py-targetY);
        if(attack==Attack.SHOCKWAVE)return Math.abs(distance-waveRadius())<=WAVE_HALF_WIDTH;
        return distance<=slamRadius();
    }
    public double waveRadius() {return WAVE_START+(slamRadius()-WAVE_START)*Math.min(1,stateTime/activeDuration());}
    public double activeDuration() {
        if(profile==Profile.BRIARHEART)return forestAttack==ForestAttack.BRAMBLE_CAGE?1.6:forestAttack==ForestAttack.ROOT_WEAVE?.7:.24;
        return attack==Attack.FISSURE?0.32:attack==Attack.CHARGE?CHARGE_DURATION:attack==Attack.SHOCKWAVE?0.75:SLAM_DURATION;
    }
    public double telegraphDuration() {
        if(profile==Profile.BRIARHEART)return forestAttack==ForestAttack.BRAMBLE_CAGE?1.85:1.65;
        if(profile==Profile.OUTPOST_WARDEN)return switch(attack) {
            case TARGET -> 1.05;
            case SWEEP -> 1.0;
            case CHARGE -> 1.2;
            case SHOCKWAVE -> 1.3;
            case FISSURE -> 1.4;
        };
        if(attack==Attack.FISSURE)return 1.1;
        return profile==Profile.BRIARHEART?1.05:profile==Profile.OATHKEEPER?.8:TELEGRAPH_DURATION;
    }
    public double recoverDuration() {
        if(profile==Profile.BRIARHEART)return impactNumber%3==0?3.8:1.2;
        if(profile==Profile.OUTPOST_WARDEN) {
            double opening=switch(attack) {
                case CHARGE -> 2.0;
                case SHOCKWAVE -> 1.85;
                case FISSURE -> 1.9;
                default -> RECOVER_DURATION;
            };
            return opening-(phaseTwo?.2:0);
        }
        return profile==Profile.ICHOR_GOLEM?(enraged()?1.8:2.1):enraged()?1.25:RECOVER_DURATION;
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
    /** Only direct, grounded strikes can be caught. The normal recovery caps the opening. */
    public void parried() {
        if(alive()&&state==State.SLAM&&profile!=Profile.BRIARHEART&&(attack==Attack.TARGET||attack==Attack.SWEEP)) {
            state=State.RECOVER;stateTime=0;
        }
    }
    public boolean hurt(int damage) {
        if(state==State.DORMANT||!alive()||damage<=0)return false;
        health=Math.max(0,health-damage);hurtFlash=0.18;
        if(health==0){state=State.DEAD;stateTime=0;}
        return true;
    }
    public boolean enraged(){return profile==Profile.OUTPOST_WARDEN||profile==Profile.BRIARHEART?phaseTwo:health<=maxHealth()/2;}
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
            case TELEGRAPH -> profile==Profile.OUTPOST_WARDEN&&attack==Attack.FISSURE?Animation.RECOVER:Animation.WINDUP;
            case SLAM -> profile==Profile.BRIARHEART&&forestAttack==ForestAttack.SEED_BLOOM||attack==Attack.CHARGE?Animation.CHARGE:Animation.STRIKE;
            case RECOVER,HURT -> profile==Profile.BRIARHEART&&!coreExposed()?Animation.IDLE:Animation.RECOVER;
            case ENRAGE -> Animation.WINDUP;
            case DEAD -> Animation.DEATH;
        };
    }
    public int animationFrame() {
        if(state==State.DORMANT)return (int)(idleAnimationTime*6)%8;
        if(state==State.APPROACH)return (int)(stateTime*8)%8;
        if(state==State.ENRAGE)return Math.min(7,(int)(stateTime*8/(ENRAGE_DURATION*.72)));
        if(profile==Profile.OUTPOST_WARDEN&&state==State.TELEGRAPH&&attack==Attack.TARGET)
            return Math.min(7,(int)(stateTime*8/(telegraphDuration()*.72)));
        if(profile==Profile.OUTPOST_WARDEN&&attack==Attack.FISSURE) {
            // Reverse the rising recovery poses into a planted crouch before the ground splits.
            if(state==State.TELEGRAPH)return 7-Math.min(7,(int)(stateTime*8/(telegraphDuration()*.75)));
            if(state==State.SLAM)return 7;
        }
        double duration=switch(state) {
            case TELEGRAPH -> telegraphDuration();
            case SLAM -> activeDuration();
            case RECOVER,HURT -> recoverDuration();
            case ENRAGE -> ENRAGE_DURATION;
            case DEAD -> DEATH_DURATION;
            default -> throw new IllegalStateException("Looping animation already handled");
        };
        return Math.min(7,(int)(stateTime*8/duration));
    }
    /** Authored attack strips: four preparation poses, two strikes, two recovery poses. */
    public int actionFrame() {
        if(profile!=Profile.OUTPOST_WARDEN||(attack!=Attack.SWEEP&&attack!=Attack.CHARGE&&attack!=Attack.SHOCKWAVE))return -1;
        return switch(state) {
            case TELEGRAPH -> Math.min(3,(int)(stateTime*4/(telegraphDuration()*.72)));
            case SLAM -> 4+Math.min(1,(int)(stateTime*2/activeDuration()));
            case RECOVER,HURT -> 6+Math.min(1,(int)(stateTime*2/recoverDuration()));
            default -> -1;
        };
    }
    public int row(){return state==State.SLAM?1:state==State.RECOVER||state==State.HURT||state==State.DEAD?2:0;}
    public int frame(){
        double duration=switch(state) {
            case TELEGRAPH -> telegraphDuration();
            case SLAM -> activeDuration();
            case RECOVER,HURT -> recoverDuration();
            case ENRAGE -> ENRAGE_DURATION;
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
