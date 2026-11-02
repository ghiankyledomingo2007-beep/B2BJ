/**
 * How an enemy's body acts out what it is doing, with no new art: an offset and a squash around its feet.
 * Timing follows the owner's references: a held, easing wind-up (Dead Cells); the gunner's charge squash and firing
 * kick (Enter the Gungeon); a lean back, then a hard step into the swing (Hollow Knight's husks); a lunge that
 * stretches along its path (Hyper Light Drifter); and a short recoil away from the blow on a hit.
 * Offsets land on the 2 px art grid and a 96 px body only grows or shrinks in 4 px steps, so the pixels stay crisp.
 */
final class EnemyPose {
    private EnemyPose() {}

    /** {dx, dy, sx, sy} for an enemy; px, py is the player, whose blows push it away. */
    static double[] of(Wisp w,double px,double py) {
        return pose(w.role(),w.state(),w.stateSeconds(),w.telegraphDuration(),w.lungeDuration(),
                w.intentX(),w.intentY(),w.x()-px,w.y()-py,CampaignRenderer.travelled(w));
    }

    /** travelled: ground covered so far, which sets the walking step (the forest mobs' four walk frames barely move). */
    static double[] pose(Wisp.Role role,Wisp.State state,double t,double windup,double lunge,
            double ix,double iy,double awayX,double awayY,double travelled) {
        double length=Math.hypot(ix,iy);
        if(length<1e-6) {ix=1;iy=0;} else {ix/=length;iy/=length;}
        double dx=0,dy=0,sx=1,sy=1;
        switch(state) {
            case TELEGRAPH -> {
                double c=Math.min(1,t/Math.max(1e-6,windup)),e=c*c*(3-2*c);
                switch(role) {
                    case SPITTER -> {sx=1+.14*e;sy=1-.16*e;if(c>.55)dx=(int)(t*30)%2==0?2:-2;}   // inflates, trembles
                    case THORN_WOLF -> {dx=-ix*8*e;dy=-iy*4*e;sx=1+.08*e;sy=1-.14*e;}             // crouches back
                    case RIFT_IMP -> {dx=-ix*8*e;dy=-iy*4*e;sx=1-.06*e;sy=1+.08*e;}               // rears back
                    case MIRE_SHAMAN -> {sx=1-.06*e;sy=1+.1*e;}                                    // rises with the staff
                    default -> {dx=-ix*6*e;sx=1-.04*e;sy=1+.04*e;}          // leans back, feet on the ground
                }
            }
            case LUNGE -> {
                double k=Math.min(1,t/Math.max(1e-6,Math.min(.12,lunge)));                        // the contact frames
                switch(role) {
                    case SPITTER -> {
                        dx=-ix*12*(1-k);dy=-iy*6*(1-k);                                           // kicks back on firing
                        if(t<.05) {sx=1.16;sy=.84;} else if(t<.12) {sx=.92;sy=1.1;}
                    }
                    case MIRE_SHAMAN -> {dx=-ix*6*(1-k);if(t<.06) {sx=1.06;sy=.9;}}
                    case RIFT_IMP -> {dx=ix*10;dy=iy*5;sx=1.08;sy=.94;}                           // leans into the swipe
                    case THORN_WOLF -> {
                        double s=.18*(1-.5*k);dx=ix*6;dy=iy*3;
                        if(Math.abs(ix)>=Math.abs(iy)) {sx=1+s;sy=1-s*.5;} else {sy=1+s;sx=1-s*.5;}
                    }
                    default -> {dx=ix*12*Math.min(1,t/.06);sx=1.06;sy=.96;}    // a hard grounded step in
                }
            }
            case RECOVER -> {
                double r=Math.max(0,1-t/.2);
                if(role!=Wisp.Role.SPITTER&&role!=Wisp.Role.MIRE_SHAMAN)dx=ix*6*r;
            }
            case PATROL,PURSUE -> {
                // a step per stride of ground covered, so it is always in time with the real speed; leaning in to chase
                double lean=state==Wisp.State.PURSUE?1:0;
                switch(role) {
                    case THORN_WOLF -> {dy=-4*Math.abs(Math.sin(travelled/24*Math.PI));dx=ix*4*lean;}     // a trot
                    case RIFT_IMP -> {dy=-6*Math.abs(Math.sin(travelled/32*Math.PI));dx=ix*4*lean;}       // wingbeats
                    case MIRE_SHAMAN -> {dy=-2*Math.abs(Math.sin(travelled/20*Math.PI));dx=ix*2*lean;}    // a heavy plod
                    default -> { }
                }
            }
            case HURT -> {
                double h=Math.max(0,1-t/.18),away=Math.max(1,Math.hypot(awayX,awayY));
                dx=awayX/away*10*h;dy=awayY/away*10*h*.25;sx=1+.22*h;sy=1-.22*h;
            }
            default -> { }
        }
        return new double[]{2*Math.round(dx/2),2*Math.round(dy/2),Math.round(96*sx/4)*4/96.,Math.round(96*sy/4)*4/96.};
    }
}
