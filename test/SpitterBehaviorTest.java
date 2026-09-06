public final class SpitterBehaviorTest {
    private static final double DT = 0.01;

    public static void main(String[] args) {
        approachesOnlyToFiringRange();
        retreatsWithoutCrossingCover();
        requiresLineOfSight();
        projectileFitsFiringLane();
        readableTellAndCommittedShot();
        releaseDoesNotLunge();
        recoveryBoundsCadence();
        interruptionCancelsPendingShot();
        escapedTargetCancelsPendingShot();
        meleeRolesKeepTheirTimings();
        System.out.println("SpitterBehaviorTest passed");
    }

    private static Wisp spitter(double x, double y) {
        return new Wisp(x, y, x, x, 3, Wisp.Role.SPITTER);
    }

    private static void approachesOnlyToFiringRange() {
        Wisp enemy = spitter(500, 500);
        for (int i = 0; i < 200 && enemy.state() != Wisp.State.TELEGRAPH; i++)
            enemy.update(DT, 890, 500);
        assert enemy.state() == Wisp.State.TELEGRAPH : "spitter closes to a ranged tell";
        assert 890 - enemy.x() <= 300 && 890 - enemy.x() >= 295;
        assert enemy.shotNumber() == 0 : "approach cannot fire";
        Wisp distant = spitter(500, 500);
        for (int i = 0; i < 1000; i++) distant.update(DT, 1800, 500);
        assert distant.state() == Wisp.State.PATROL && distant.shotNumber() == 0;
    }

    private static void retreatsWithoutCrossingCover() {
        Wisp enemy = spitter(500, 500);
        for (int i = 0; i < 20; i++) enemy.update(DT, 600, 500);
        assert enemy.x() < 480 && enemy.shotNumber() == 0 : "close target causes retreat, not a lunge";
        RuinedOutpostMap map = new RuinedOutpostMap(2);
        var cover = map.barriers().get(0);
        double x = cover.centerX() + cover.width()/2 + Wisp.COLLISION_RADIUS + 1;
        double y = cover.centerY() - Wisp.COLLISION_Y_OFFSET;
        Wisp cornered = spitter(x, y);
        for (int i = 0; i < 100; i++) {
            cornered.update(DT, x+100, y, map);
            assert !map.isBlocked(cornered.x(), cornered.y()+Wisp.COLLISION_Y_OFFSET,
                    Wisp.COLLISION_RADIUS) : "retreat respects terrain";
        }
        assert Math.abs(cornered.x()-x) < 0.01 : "blocked retreat does not slide through cover";
        assert cornered.shotNumber() == 1 : "cornered spitter can still commit a readable shot";
    }

    private static void requiresLineOfSight() {
        RuinedOutpostMap map = new RuinedOutpostMap(2);
        var cover = map.barriers().get(0);
        double x = cover.centerX()-100, y = cover.centerY()-Wisp.COLLISION_Y_OFFSET;
        Wisp enemy = spitter(x, y);
        enemy.update(DT, x+200, y, map);
        assert enemy.state() == Wisp.State.PURSUE && enemy.shotNumber() == 0
                : "cover blocks starting a shot";
        Wisp aiming = spitter(x, y);
        aiming.update(DT, x, y+200, map);
        assert aiming.state() == Wisp.State.TELEGRAPH;
        aiming.update(0.6, x, y+200, map);
        aiming.update(0.16, x+200, y, map);
        assert aiming.state() == Wisp.State.RECOVER && aiming.shotNumber() == 0
                : "moving behind cover during tell cancels release";
        map.setPassagesLocked(true);
        double roadY = map.spawnY()-Wisp.COLLISION_Y_OFFSET;
        Wisp atGate = spitter(200, roadY);
        atGate.update(DT, 20, roadY, map);
        assert atGate.state() != Wisp.State.TELEGRAPH && atGate.shotNumber() == 0
                : "locked passage gates also block ranged line of sight";
    }

    private static void readableTellAndCommittedShot() {
        Wisp enemy = spitter(500, 500);
        enemy.update(DT, 750, 500);
        enemy.update(0.2, 500, 750);
        assert enemy.intentY() > 0.99 && !enemy.directionLocked() : "early tell tracks";
        enemy.update(0.31, 750, 500);
        assert enemy.directionLocked() && enemy.intentX() > 0.99;
        enemy.update(0.2, 250, 500);
        assert enemy.state() == Wisp.State.TELEGRAPH && enemy.shotNumber() == 0;
        assert enemy.intentX() > 0.99 : "late tell cannot reverse";
        enemy.update(0.05, 250, 500);
        assert enemy.state() == Wisp.State.LUNGE && enemy.shotNumber() == 1;
        assert enemy.intentX() > 0.99 : "release follows committed aim, not current player";
    }
    private static void projectileFitsFiringLane() {
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double y=wall.centerY()-Wisp.COLLISION_Y_OFFSET,px=wall.centerX()-130;
        var enemy=spitter(wall.centerX()+130,y);
        for(int i=0;i<1000;i++) {
            int before=enemy.shotNumber();enemy.update(DT,px,y,map);
            if(before!=enemy.shotNumber())assert map.clearWaterLine(enemy.x(),enemy.y()+Wisp.COLLISION_Y_OFFSET,
                    px,y+Wisp.COLLISION_Y_OFFSET,EnemyProjectile.RADIUS) : "firing lane must fit projectile, not just a one-pixel ray";
        }
        assert enemy.shotNumber()>0 : "spitter must complete cover detour rather than retreating forever behind cover";
    }

    private static void releaseDoesNotLunge() {
        Wisp enemy = spitter(500, 500);
        enemy.update(DT, 750, 500);
        enemy.update(0.75, 750, 500);
        double x=enemy.x(), y=enemy.y();
        for (int i=0; i<30; i++) enemy.update(DT, 750, 500);
        assert enemy.x()==x && enemy.y()==y : "ranged release never translates like melee lunge";
        assert enemy.shotNumber()==1 && enemy.state()==Wisp.State.RECOVER;
    }

    private static void recoveryBoundsCadence() {
        Wisp enemy=spitter(500,500);
        int previous=0;
        double last=-100;
        for(int i=0;i<1000;i++) {
            enemy.update(DT,750,500);
            if(enemy.shotNumber()!=previous) {
                assert enemy.shotNumber()==previous+1 : "exactly one emission per release";
                assert i*DT-last>=2.3 : "tell and recovery prevent spam";
                last=i*DT;previous=enemy.shotNumber();
            }
        }
        assert previous>=3 && previous<=5 : "bounded firing cadence over ten seconds";
    }

    private static void interruptionCancelsPendingShot() {
        for(int damage:new int[]{1,3}) {
            Wisp enemy=spitter(500,500);
            enemy.update(DT,750,500);
            enemy.update(0.7,750,500);
            enemy.hurt(damage);
            enemy.update(0.1,750,500);
            assert enemy.shotNumber()==0 : "hurt/death cannot release an interrupted tell";
            if(damage==3) {
                for(int i=0;i<500;i++)enemy.update(DT,750,500);
                assert enemy.shotNumber()==0 && !enemy.alive();
            } else {
                for(int i=0;i<10;i++)enemy.update(DT,750,500);
                assert enemy.shotNumber()==0 : "recovering from hurt starts a fresh tell";
            }
        }
    }

    private static void escapedTargetCancelsPendingShot() {
        Wisp enemy=spitter(500,500);
        enemy.update(DT,750,500);
        enemy.update(0.76,1700,500);
        assert enemy.shotNumber()==0 && enemy.state()==Wisp.State.RECOVER;
    }

    private static void meleeRolesKeepTheirTimings() {
        Wisp scout=new Wisp(500,500,500,500,2);
        Wisp guard=new Wisp(500,500,500,500,4,Wisp.Role.GUARD);
        assert scout.telegraphDuration()==0.4 && scout.lungeDuration()==0.18
                && scout.recoverDuration()==0.45;
        assert guard.telegraphDuration()==0.6 && guard.lungeDuration()==0.35
                && guard.recoverDuration()==0.85;
        for(Wisp enemy:new Wisp[]{scout,guard}) {
            enemy.update(DT,600,500);
            enemy.update(enemy.telegraphDuration(),600,500);
            double x=enemy.x();enemy.update(0.05,600,500);
            assert enemy.x()>x && enemy.shotNumber()==0;
        }
    }
}
