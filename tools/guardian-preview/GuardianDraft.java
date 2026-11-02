/** Preview-only Warden. Player combat stays in the installed game classes. */
final class GuardianDraft extends Guardian {
    enum Move { FIST, BACKHAND, RUSH, DETACHED_FISTS, CORE_BEAM }
    private double x,y,time,flash,lastX,lastY,vx,vy,angle,turn=1,tx,ty,secondX,secondY;
    private final double homeX,homeY;
    private int hp=1200,impact,count,spell;
    private State state=State.DORMANT;
    private Move move=Move.FIST,previous=Move.FIST;
    private boolean phaseTwo,locked,sampled,wall,secondReleased;
    private RuinedOutpostMap terrain;

    GuardianDraft(double x,double y){super(x,y);this.x=homeX=x;this.y=homeY=y;}
    Move move(){return move;}
    boolean committed(){return locked;}
    double angle(){return angle;}
    double beamAngle(){return angle+turn*(.45-Math.min(1,time/.9)*.9);}
    double beamReach(){
        if(terrain==null||beamClear(470))return 470;
        double low=0,high=470;
        for(int i=0;i<9;i++){double middle=(low+high)/2;if(beamClear(middle))low=middle;else high=middle;}
        return low;
    }
    private boolean beamClear(double length){return terrain.clearLine(x,y+32,x+Math.cos(beamAngle())*length,y+Math.sin(beamAngle())*length+Player.COLLISION_Y_OFFSET);}
    double secondX(){return secondX;}
    double secondY(){return secondY;}
    double lockTime(){return switch(move){case FIST->.56;case BACKHAND->.68;case RUSH->.78;case DETACHED_FISTS->.72;case CORE_BEAM->.91;};}
    int damage(){return switch(move){case FIST->24;case BACKHAND->22;case RUSH->28;case DETACHED_FISTS->20;case CORE_BEAM->24;};}
    void secondPhase(){hp=Math.min(hp,600);phaseTwo=true;state=State.APPROACH;time=0;count=0;}
    void forceMove(Move selected,double px,double py){begin(selected,px,py);}

    @Override public void activate(double px,double py){if(state==State.DORMANT&&Double.isFinite(px)&&Double.isFinite(py)){state=State.APPROACH;time=0;lastX=px;lastY=py;sampled=true;}}
    @Override public void update(double dt,double px,double py){update(dt,px,py,null);}
    @Override public void update(double seconds,double px,double py,RuinedOutpostMap map){
        if(!Double.isFinite(seconds)||seconds<=0||!Double.isFinite(px)||!Double.isFinite(py))return;
        terrain=map;
        if(sampled){double rawX=(px-lastX)/seconds,rawY=(py-lastY)/seconds,length=Math.hypot(rawX,rawY),scale=Math.min(1,320/Math.max(1,length));vx=rawX*scale;vy=rawY*scale;}
        lastX=px;lastY=py;sampled=true;
        for(double left=Math.min(seconds,5);left>1e-8;){double dt=Math.min(.01,left);tickDraft(dt,px,py);left-=dt;}
        if(map!=null&&alive())map.setGuardianPosition(x,y);
    }
    private double[] lead(double px,double py,double horizon,double cap){double length=Math.hypot(vx,vy),scale=Math.min(horizon,cap/Math.max(1,length));return new double[]{px+vx*scale,py+vy*scale};}
    private void aim(double px,double py){double[] p=lead(px,py,move==Move.DETACHED_FISTS?.45:.22,move==Move.DETACHED_FISTS?96:48);tx=p[0];ty=p[1];angle=Math.atan2(ty-y,tx-x);turn=Math.signum((px-x)*vy-(py-y)*vx);if(turn==0)turn=1;}
    private void begin(Move selected,double px,double py){move=selected;previous=selected;count++;state=State.TELEGRAPH;time=0;locked=wall=secondReleased=false;aim(px,py);}
    private void choose(double px,double py){double d=Math.hypot(px-x,py-y),away=((px-x)*vx+(py-y)*vy)/Math.max(1,d);
        if(phaseTwo&&count%2==1){begin(spell++%2==0?Move.DETACHED_FISTS:Move.CORE_BEAM,px,py);return;}
        begin((d>164||away>70&&d>120)&&previous!=Move.RUSH?Move.RUSH:d<184&&previous!=Move.BACKHAND?Move.BACKHAND:Move.FIST,px,py);
    }
    private boolean travel(double dx,double dy,double px,double py){double nx=x+dx,ny=y+dy;
        if(Math.hypot(nx-px,ny+32-(py+Player.COLLISION_Y_OFFSET))<84)return false;
        if(Math.hypot(nx-homeX,ny-homeY)>900||terrain!=null&&terrain.guardianDraftBlocked(nx,ny+32,60))return false;
        x=nx;y=ny;return true;
    }
    private void tickDraft(double dt,double px,double py){
        flash=Math.max(0,flash-dt);time+=dt;
        switch(state){
            case DORMANT,DEAD -> {}
            case APPROACH -> {
                double[] target=lead(px,py,.22,48);double dx=target[0]-x,dy=target[1]-y,d=Math.hypot(dx,dy),speed=phaseTwo?192:174;
                if(Math.hypot(px-x,py-y)>118&&d>1){angle=Math.atan2(dy,dx);if(!travel(dx/d*speed*dt,dy/d*speed*dt,px,py)){if(!travel(dx/d*speed*dt,0,px,py))travel(0,dy/d*speed*dt,px,py);}}
                if(time>.55&&(Math.hypot(px-x,py-y)<172||time>(phaseTwo?.95:1.15)))choose(px,py);
            }
            case TELEGRAPH -> {
                if(!locked){aim(px,py);if(move==Move.FIST&&Math.hypot(px-x,py-y)>120)travel(Math.cos(angle)*96*dt,Math.sin(angle)*96*dt,px,py);if(time>=lockTime())locked=true;}
                if(time>=telegraphDuration()){state=State.SLAM;time=0;impact++;
                    if(move==Move.DETACHED_FISTS){double[] p=lead(px,py,.55,120);secondX=p[0];secondY=p[1];}
                    if(move==Move.FIST){tx=x+Math.cos(angle)*96;ty=y+Math.sin(angle)*96;}
                }
            }
            case SLAM -> {
                if(move==Move.RUSH&&!travel(Math.cos(angle)*560*dt,Math.sin(angle)*560*dt,px,py)){
                    // Contact with the player remains an active hit; terrain collision grants recovery.
                    double nx=x+Math.cos(angle)*560*dt,ny=y+Math.sin(angle)*560*dt;
                    if(Math.hypot(nx-homeX,ny-homeY)>900||terrain!=null&&terrain.guardianDraftBlocked(nx,ny+32,60)){wall=true;state=State.RECOVER;time=0;}
                }
                if(move==Move.DETACHED_FISTS&&time>=.48&&!secondReleased){secondReleased=true;impact++;}
                if(state==State.SLAM&&time>=activeDuration()){state=State.RECOVER;time=0;}
            }
            case RECOVER,HURT -> {if(time>=recoverDuration()){state=!phaseTwo&&hp<=600?State.ENRAGE:State.APPROACH;time=0;}}
            case ENRAGE -> {if(time>=1.4){phaseTwo=true;count=0;state=State.APPROACH;time=0;}}
        }
    }
    @Override public boolean hits(double px,double py){
        if(state!=State.SLAM)return false;
        double d=Math.hypot(px-x,py-y),a=Math.atan2(Math.sin(Math.atan2(py-y,px-x)-angle),Math.cos(Math.atan2(py-y,px-x)-angle));
        boolean hit=switch(move){
            case FIST -> Math.hypot(px-tx,py-ty)<76;
            case RUSH -> d<108;
            case BACKHAND -> {double edge=-Math.PI/2+Math.min(1,time/.3)*Math.PI;yield d<182&&a>=edge-.7&&a<=edge+.12;}
            case DETACHED_FISTS -> {double age=secondReleased?time-.48:time;yield age>=.2&&age<.35&&Math.hypot(px-(secondReleased?secondX:tx),py-(secondReleased?secondY:ty))<62;}
            case CORE_BEAM -> d>86&&lineDistance(px,py,x,y,x+Math.cos(beamAngle())*470,y+Math.sin(beamAngle())*470)<24;
        };
        return hit&&(terrain==null||terrain.clearLine(x,y+32,px,py+Player.COLLISION_Y_OFFSET));
    }
    static double lineDistance(double px,double py,double ax,double ay,double bx,double by){double dx=bx-ax,dy=by-ay,q=Math.max(0,Math.min(1,((px-ax)*dx+(py-ay)*dy)/Math.max(1,dx*dx+dy*dy)));return Math.hypot(px-ax-dx*q,py-ay-dy*q);}
    @Override public boolean hurt(int damage){if(state==State.DORMANT||!alive()||damage<=0)return false;hp=Math.max(0,hp-damage);flash=.18;if(hp==0){state=State.DEAD;time=0;}return true;}
    @Override public boolean hitFrom(double ax,double ay,int fx,int fy,double range,int damage){double n=Math.hypot(fx,fy);if(n==0||range<=0||damage<=0)return false;double dx=x-ax,dy=y-ay;return(dx*fx+dy*fy)/n>=0&&(dx*fx+dy*fy)/n<=range&&Math.abs(dx*fy-dy*fx)/n<=range*.75&&hurt(damage);}
    @Override public void parried(){if(alive()&&state==State.SLAM&&(move==Move.FIST||move==Move.BACKHAND)){state=State.RECOVER;time=0;}}
    @Override public Attack attack(){return switch(move){case FIST->Attack.TARGET;case BACKHAND->Attack.SWEEP;case RUSH->Attack.CHARGE;case DETACHED_FISTS->Attack.FISSURE;case CORE_BEAM->Attack.SHOCKWAVE;};}
    @Override public double telegraphDuration(){return switch(move){case FIST->.86;case BACKHAND->1.;case RUSH->1.12;case DETACHED_FISTS->1.1;case CORE_BEAM->1.25;};}
    @Override public double activeDuration(){return switch(move){case FIST->.18;case BACKHAND->.3;case RUSH->.55;case DETACHED_FISTS->.96;case CORE_BEAM->.9;};}
    @Override public double recoverDuration(){return(switch(move){case FIST->1.7;case BACKHAND->1.75;case RUSH->2.;case DETACHED_FISTS->2.;case CORE_BEAM->2.1;})-(phaseTwo?.15:0)+(wall?.4:0);}
    @Override public Animation animation(){return switch(state){case DORMANT->Animation.IDLE;case APPROACH->Animation.APPROACH;case TELEGRAPH,ENRAGE->move==Move.CORE_BEAM||move==Move.DETACHED_FISTS?Animation.IDLE:Animation.WINDUP;case SLAM->move==Move.CORE_BEAM||move==Move.DETACHED_FISTS?Animation.IDLE:move==Move.RUSH?Animation.CHARGE:Animation.STRIKE;case RECOVER,HURT->Animation.RECOVER;case DEAD->Animation.DEATH;};}
    @Override public int animationFrame(){if(state==State.DORMANT||state==State.APPROACH||((move==Move.CORE_BEAM||move==Move.DETACHED_FISTS)&&(state==State.SLAM||state==State.TELEGRAPH)))return(int)(time*8)%8;double duration=switch(state){case TELEGRAPH->telegraphDuration();case SLAM->activeDuration();case ENRAGE->1.4;case DEAD->DEATH_DURATION;default->recoverDuration();};return Math.min(7,(int)(time/duration*8));}
    @Override public int actionFrame(){if(move!=Move.BACKHAND&&move!=Move.RUSH)return-1;return switch(state){case TELEGRAPH->Math.min(3,(int)(time/telegraphDuration()*4));case SLAM->4+Math.min(1,(int)(time/activeDuration()*2));case RECOVER,HURT->6+Math.min(1,(int)(time/recoverDuration()*2));default->-1;};}
    @Override public int row(){return state==State.SLAM?1:state==State.RECOVER||state==State.HURT||state==State.DEAD?2:0;}
    @Override public int frame(){return Math.min(3,animationFrame()/2);}
    @Override public boolean alive(){return state!=State.DEAD;}
    @Override public boolean visible(){return alive()||time<DEATH_DURATION-.02;}
    @Override public boolean coreExposed(){return state==State.RECOVER;}
    @Override public boolean flashVisible(){return flash==0||(int)(flash*30)%2==0;}
    @Override public boolean enraged(){return phaseTwo;}
    @Override public double renderScale(){return state==State.DEAD?Math.max(0,1-time/DEATH_DURATION):1;}
    @Override public int health(){return hp;}
    @Override public int impactNumber(){return impact;}
    @Override public State state(){return state;}
    @Override public double stateSeconds(){return time;}
    @Override public double x(){return x;}
    @Override public double y(){return y;}
    @Override public double targetX(){return move==Move.DETACHED_FISTS&&secondReleased?secondX:tx;}
    @Override public double targetY(){return move==Move.DETACHED_FISTS&&secondReleased?secondY:ty;}
    double firstX(){return tx;}
    double firstY(){return ty;}
    @Override public double directionX(){return Math.cos(angle);}
    @Override public double directionY(){return Math.sin(angle);}
    @Override public double slamRadius(){return move==Move.BACKHAND?182:76;}
    @Override public double waveRadius(){return 0;}
    @Override public RuinedOutpostMap.Obstacle fissureBounds(){return null;}
}
