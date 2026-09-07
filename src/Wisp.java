import java.util.List;

public final class Wisp {
    public enum Role { SCOUT, GUARD, SPITTER }
    public enum State {
        PATROL,
        PURSUE,
        TELEGRAPH,
        LUNGE,
        RECOVER,
        HURT,
        DEAD
    }

    public static final double SPEED = 70.0;
    public static final double PURSUE_SPEED = 130.0;
    public static final int MAX_HEALTH = 4;
    public static final double COLLISION_RADIUS = 20.0;
    public static final double COLLISION_Y_OFFSET = 22.0;
    public static final double TELEGRAPH_DURATION = 0.4;
    /** Final part of the tell during which the lunge direction is committed and readable. */
    public static final double LUNGE_LOCK_WINDOW = 0.15;
    public static final double LUNGE_DURATION = 0.18;
    public static final double DEATH_DURATION = 0.35;
    /** Basic water travels 340 px plus projectile and body radii; awareness must exceed that. */
    public static final double AWARENESS_RANGE = 400.0;
    /** Lunge travel (about 94 px) plus contact radius; lunges from farther away cannot connect. */
    public static final double ATTACK_RANGE = 150.0;
    /** How long a damaged or alerted enemy keeps hunting a target it can no longer see. */
    public static final double AGGRO_DURATION = 6.0;

    private static final double RECOVER_DURATION = 0.45;
    private static final double HURT_DURATION = 0.28;
    private static final double STAGGER_COOLDOWN = 1.2;
    private static final double LUNGE_SPEED = 520.0;
    private static final double PROBE_DISTANCE = 24.0;
    private static final double DETOUR_COMMIT = 0.35;
    private static final double SEPARATION_SPEED = 120.0;
    private static final double[] DETOUR_ANGLES = {60, 90, 120, 150};

    private final double minimumX;
    private final double maximumX;
    private final double laneY;
    private final int maximumHealth;
    private final Role role;
    private final WispAnimation animation = new WispAnimation();
    private double x;
    private double y;
    private int horizontal = 1;
    private int health;
    private State state = State.PATROL;
    private double stateTime;
    private double lungeX;
    private double lungeY;
    private double intentX, intentY = 1;
    private double impulseX, impulseY;
    private double aggroTime;
    private int detourSign;
    private double detourTime, detourX, detourY;
    private int shotNumber;
    private double staggerCooldown;

    public Wisp(double x, double y, double minimumX, double maximumX) {
        this(x, y, minimumX, maximumX, MAX_HEALTH);
    }

    public Wisp(double x, double y, double minimumX, double maximumX,
            int maximumHealth) {
        this(x,y,minimumX,maximumX,maximumHealth,Role.SCOUT);
    }

    public Wisp(double x, double y, double minimumX, double maximumX,
            int maximumHealth, Role role) {
        this.role=java.util.Objects.requireNonNull(role);
        this.x = Math.max(minimumX, Math.min(maximumX, x));
        this.y = y;
        laneY = y;
        this.minimumX = minimumX;
        this.maximumX = maximumX;
        this.maximumHealth = Math.max(1, maximumHealth);
        health = this.maximumHealth;
    }

    public void update(double seconds) {
        update(seconds, x, y, false, null);
    }

    public void update(double seconds, double targetX, double targetY) {
        update(seconds, targetX, targetY, true, null);
    }

    public void update(double seconds, double targetX, double targetY,
            RuinedOutpostMap map) {
        update(seconds, targetX, targetY, true, map);
    }

    private void update(double seconds, double targetX, double targetY,
            boolean canStartAttack, RuinedOutpostMap map) {
        staggerCooldown=Math.max(0,staggerCooldown-seconds);
        if(alive()) {
            move(impulseX*seconds,impulseY*seconds,map);
            double drag=Math.exp(-10*seconds);
            impulseX*=drag; impulseY*=drag;
        }
        switch (state) {
            case PATROL -> updatePatrol(seconds, targetX, targetY,
                    canStartAttack, map);
            case PURSUE -> updatePursue(seconds, targetX, targetY,
                    canStartAttack, map);
            case TELEGRAPH -> updateTelegraph(seconds, targetX, targetY, map);
            case LUNGE -> updateLunge(seconds, map);
            case RECOVER -> updateRecovery(seconds);
            case HURT -> updateHurt(seconds);
            case DEAD -> stateTime += seconds;
        }
    }

    private void updatePatrol(double seconds, double targetX, double targetY,
            boolean canStartAttack, RuinedOutpostMap map) {
        if (canStartAttack && (aggroTime > 0
                || Math.hypot(targetX - x, targetY - y) <= AWARENESS_RANGE)) {
            enter(State.PURSUE);
            updatePursue(seconds, targetX, targetY, true, map);
            return;
        }
        double previousX = x;
        if (x > maximumX) horizontal = -1;
        else if (x < minimumX) horizontal = 1;
        double distance = horizontal * SPEED * seconds;
        if (x >= minimumX && x <= maximumX)
            distance = clamp(x + distance, minimumX, maximumX) - x;
        double climb = clamp(laneY - y, -SPEED * seconds, SPEED * seconds);
        move(distance, climb, map);
        if (map != null && x == previousX) {
            horizontal = -horizontal;
        }
        if (x >= maximumX) {
            horizontal = -1;
        } else if (x <= minimumX) {
            horizontal = 1;
        }
        animation.update(horizontal, 0, seconds);
    }

    private void updatePursue(double seconds, double targetX, double targetY,
            boolean canStartAttack, RuinedOutpostMap map) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        if (canStartAttack && distance <= AWARENESS_RANGE) aggroTime = AGGRO_DURATION;
        else aggroTime = Math.max(0, aggroTime - seconds);
        if (!canStartAttack || aggroTime <= 0) {
            enter(State.PATROL);
            return;
        }
        stateTime += seconds;
        if (role == Role.SPITTER && distance > 0 && distance < 160-1e-6
                && lineClear(targetX,targetY,map)
                && (map == null || probeFree(-dx/distance, -dy/distance, map))) {
            double retreat = Math.min(160-distance, PURSUE_SPEED*seconds);
            move(-dx/distance*retreat, -dy/distance*retreat, map);
            setIntent(dx, dy);
            face(targetX, targetY, seconds);
            return;
        }
        double attackRange = role==Role.SPITTER?300:role==Role.GUARD?220:ATTACK_RANGE;
        if (distance <= attackRange && lineClear(targetX, targetY, map)) {
            setIntent(dx, dy);
            enter(State.TELEGRAPH);
            face(targetX, targetY, 0);
            return;
        }
        if (distance < 1) {
            animation.update(0, 0, seconds);
            return;
        }
        double[] direction = steer(dx / distance, dy / distance, seconds, map);
        setIntent(direction[0], direction[1]);
        double speed=role==Role.GUARD?165:PURSUE_SPEED;
        move(direction[0] * speed * seconds,
                direction[1] * speed * seconds, map);
        animation.update((int) Math.signum(direction[0]),
                (int) Math.signum(direction[1]), seconds);
    }

    /** Local navigation: go straight when the next step is free, otherwise slip around the nearer end. */
    private double[] steer(double ux, double uy, double seconds, RuinedOutpostMap map) {
        if (map == null) return new double[]{ux, uy};
        boolean directFree = probeFree(ux, uy, map);
        if (detourTime > 0) {
            detourTime -= seconds;
            if (!directFree && probeFree(detourX, detourY, map)) {
                return new double[]{detourX, detourY};
            }
            detourTime = 0;
        }
        if (directFree) return new double[]{ux, uy};
        int side = detourSign != 0 ? detourSign : preferredSide(ux, uy, map);
        for (int attempt = 0; attempt < 2; attempt++, side = -side) {
            for (double degrees : DETOUR_ANGLES) {
                double radians = Math.toRadians(degrees * side);
                double cx = ux * Math.cos(radians) - uy * Math.sin(radians);
                double cy = ux * Math.sin(radians) + uy * Math.cos(radians);
                if (probeFree(cx, cy, map)) {
                    detourSign = side;
                    detourX = cx;
                    detourY = cy;
                    detourTime = DETOUR_COMMIT;
                    return new double[]{cx, cy};
                }
            }
        }
        return new double[]{0, 0};
    }

    private boolean probeFree(double ux, double uy, RuinedOutpostMap map) {
        return !map.isBlocked(x + ux * PROBE_DISTANCE,
                y + COLLISION_Y_OFFSET + uy * PROBE_DISTANCE, COLLISION_RADIUS);
    }

    /** +1 rotates the heading counter-clockwise on screen; picks the shorter way around the blocker. */
    private int preferredSide(double ux, double uy, RuinedOutpostMap map) {
        double probeX = x + ux * PROBE_DISTANCE;
        double probeY = y + COLLISION_Y_OFFSET + uy * PROBE_DISTANCE;
        RuinedOutpostMap.Obstacle blocker = null;
        for (List<RuinedOutpostMap.Obstacle> group : List.of(map.barriers(), map.banks())) {
            for (RuinedOutpostMap.Obstacle obstacle : group) {
                if (touches(probeX, probeY, obstacle)) {
                    blocker = obstacle;
                    break;
                }
            }
            if (blocker != null) break;
        }
        if (blocker == null) return 1;
        double px = -uy, py = ux;
        double feetY = y + COLLISION_Y_OFFSET;
        double positive = 0, negative = 0;
        for (int corner = 0; corner < 4; corner++) {
            double cornerX = blocker.centerX() + (corner % 2 == 0 ? -1 : 1) * blocker.width() / 2;
            double cornerY = blocker.centerY() + (corner < 2 ? -1 : 1) * blocker.height() / 2;
            double projection = (cornerX - x) * px + (cornerY - feetY) * py;
            positive = Math.max(positive, projection);
            negative = Math.max(negative, -projection);
        }
        return positive <= negative ? 1 : -1;
    }

    private static boolean touches(double px, double py, RuinedOutpostMap.Obstacle o) {
        double dx = px - clamp(px, o.centerX() - o.width() / 2, o.centerX() + o.width() / 2);
        double dy = py - clamp(py, o.centerY() - o.height() / 2, o.centerY() + o.height() / 2);
        return dx * dx + dy * dy < COLLISION_RADIUS * COLLISION_RADIUS;
    }

    private boolean lineClear(double targetX, double targetY, RuinedOutpostMap map) {
        if (role == Role.SPITTER && map != null) {
            return map.clearWaterLine(x,y+COLLISION_Y_OFFSET,targetX,targetY+COLLISION_Y_OFFSET,EnemyProjectile.RADIUS);
        }
        return map == null || map.clearLine(x, y + COLLISION_Y_OFFSET,
                targetX, targetY + COLLISION_Y_OFFSET);
    }

    private void updateTelegraph(double seconds, double targetX, double targetY,
            RuinedOutpostMap map) {
        boolean tracking = stateTime < telegraphDuration() - lockWindow();
        stateTime += seconds;
        if (tracking) {
            setIntent(targetX - x, targetY - y);
            face(targetX, targetY, seconds);
        } else {
            animation.update((int) Math.signum(intentX), (int) Math.signum(intentY), seconds);
        }
        if (stateTime >= telegraphDuration()) {
            if (role == Role.SPITTER) {
                if (Math.hypot(targetX-x, targetY-y) > AWARENESS_RANGE
                        || !lineClear(targetX, targetY, map)) {
                    enter(State.RECOVER);
                    return;
                }
                shotNumber++;
            }
            lungeX = intentX;
            lungeY = intentY;
            enter(State.LUNGE);
        }
    }

    private void updateLunge(double seconds, RuinedOutpostMap map) {
        if (role == Role.SPITTER) {
            // Reuse attack frames/state, but a ranged release has no melee propulsion.
            animation.update(0, 0, seconds);
            stateTime += seconds;
            if (stateTime >= lungeDuration()) enter(State.RECOVER);
            return;
        }
        double active = Math.min(seconds, lungeDuration() - stateTime);
        double beforeX = x, beforeY = y;
        move(lungeX * LUNGE_SPEED * active,
                lungeY * LUNGE_SPEED * active, map);
        double travelled = Math.hypot(x - beforeX, y - beforeY);
        animation.update((int) Math.signum(lungeX), (int) Math.signum(lungeY), seconds);
        stateTime += seconds;
        boolean blocked = map != null && active > 0
                && travelled < LUNGE_SPEED * active * 0.25;
        if (stateTime >= lungeDuration() || blocked) {
            enter(State.RECOVER);
        }
    }

    private void move(double distanceX, double distanceY, RuinedOutpostMap map) {
        if (map == null) {
            x += distanceX;
            y += distanceY;
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(
                Math.hypot(distanceX, distanceY) / 8.0));
        for (int step = 0; step < steps; step++) {
            double nextX = clamp(x + distanceX / steps,
                    COLLISION_RADIUS, map.worldWidth() - COLLISION_RADIUS);
            if (!map.isBlocked(nextX, y + COLLISION_Y_OFFSET, COLLISION_RADIUS)) {
                x = nextX;
            }
            double nextY = clamp(y + distanceY / steps,
                    COLLISION_RADIUS,
                    map.worldHeight() - COLLISION_RADIUS - COLLISION_Y_OFFSET);
            if (!map.isBlocked(x, nextY + COLLISION_Y_OFFSET, COLLISION_RADIUS)) {
                y = nextY;
            }
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void updateRecovery(double seconds) {
        animation.update(0, 0, seconds);
        stateTime += seconds;
        if (stateTime >= recoverDuration()) {
            enter(State.PATROL);
        }
    }

    private void updateHurt(double seconds) {
        animation.update(0, 0, seconds);
        stateTime += seconds;
        if (stateTime >= HURT_DURATION) {
            enter(aggroTime > 0 ? State.PURSUE : State.PATROL);
        }
    }

    private void enter(State next) {
        state = next;
        stateTime = 0;
        if (next != State.PURSUE) {
            detourSign = 0;
            detourTime = 0;
        }
    }

    private void setIntent(double dx, double dy) {
        double length = Math.hypot(dx, dy);
        if (length == 0) return;
        intentX = dx / length;
        intentY = dy / length;
    }

    private void face(double targetX, double targetY, double seconds) {
        animation.update((int) Math.signum(targetX - x),
                (int) Math.signum(targetY - y), seconds);
    }

    public boolean hitFrom(double attackerX, double attackerY,
            int facingX, int facingY, double range, int damage) {
        double facingLength = Math.hypot(facingX, facingY);
        if (!alive() || damage <= 0 || range <= 0 || facingLength == 0) {
            return false;
        }

        double dx = x - attackerX;
        double dy = y - attackerY;
        double forward = (dx * facingX + dy * facingY) / facingLength;
        double sideways = Math.abs(dx * facingY - dy * facingX) / facingLength;
        if (forward < 0 || forward > range || sideways > range * 0.65) {
            return false;
        }

        return hurt(damage);
    }

    public boolean hurt(int damage) {
        return hurt(damage,true);
    }

    public boolean hurt(int damage,boolean stagger) {
        if (!alive() || damage <= 0) {
            return false;
        }
        health = Math.max(0, health - damage);
        aggroTime = AGGRO_DURATION;
        if(health==0) enter(State.DEAD);
        else if(stagger&&staggerCooldown==0) {
            enter(State.HURT);
            staggerCooldown=STAGGER_COOLDOWN;
        }
        return true;
    }

    public void push(double velocityX,double velocityY) {
        if(!Double.isFinite(velocityX)||!Double.isFinite(velocityY)) return;
        impulseX=velocityX; impulseY=velocityY;
    }

    /**
     * Nudges this enemy away from an overlapping one, using map collision so it never
     * gets shoved into terrain. Only this enemy moves; a lunging enemy keeps its lane.
     * Returns whether any separation was needed.
     */
    public boolean separateFrom(Wisp other, double seconds, RuinedOutpostMap map) {
        if (other == null || other == this || !alive() || !other.alive()
                || state == State.LUNGE || !(seconds > 0)) {
            return false;
        }
        double dx = x - other.x;
        double dy = y - other.y;
        double distance = Math.hypot(dx, dy);
        double minimum = COLLISION_RADIUS * 2;
        if (distance >= minimum) return false;
        if (distance < 1e-6) {
            dx = System.identityHashCode(this) <= System.identityHashCode(other) ? -1 : 1;
            dy = 0;
            distance = 1;
        }
        double push = Math.min((minimum - distance) / 2, SEPARATION_SPEED * seconds);
        move(dx / distance * push, dy / distance * push, map);
        return true;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public WispAnimation animation() {
        return animation;
    }

    /** Unit direction of the current intention: pursuit heading, tracked or locked tell, or lunge. */
    public double intentX() {
        return state == State.LUNGE ? lungeX : intentX;
    }

    public double intentY() {
        return state == State.LUNGE ? lungeY : intentY;
    }

    /** True once the tell has committed to its direction, through the end of the lunge. */
    public boolean directionLocked() {
        return state == State.LUNGE
                || (state == State.TELEGRAPH
                        && stateTime >= telegraphDuration() - lockWindow());
    }

    public boolean aggro() {
        return alive() && aggroTime > 0;
    }

    public Role role(){return role;}
    /** Monotonic release counter; callers consume changes immediately after update, not during hurt/death. */
    public int shotNumber(){return shotNumber;}
    private double lockWindow(){return role==Role.SPITTER?0.25:LUNGE_LOCK_WINDOW;}
    public double telegraphDuration(){return role==Role.SPITTER?0.75:role==Role.GUARD?0.6:TELEGRAPH_DURATION;}
    public double lungeDuration(){return role==Role.GUARD?0.35:LUNGE_DURATION;}
    public double recoverDuration(){return role==Role.SPITTER?1.4:role==Role.GUARD?0.85:RECOVER_DURATION;}
    public double stateSeconds(){return stateTime;}

    public boolean usesAttackAnimation() {
        return state == State.TELEGRAPH
                || state == State.LUNGE
                || state == State.RECOVER;
    }

    public int attackFrame() {
        return switch (state) {
            case TELEGRAPH -> timedFrame(0, 3, telegraphDuration());
            case LUNGE -> timedFrame(3, 3, lungeDuration());
            case RECOVER -> timedFrame(6, 2, recoverDuration());
            default -> 0;
        };
    }

    private int timedFrame(int first, int count, double duration) {
        return first + Math.min(count - 1, (int) (stateTime / duration * count));
    }

    public int health() {
        return health;
    }

    public int maxHealth() {
        return maximumHealth;
    }

    public State state() {
        return state;
    }

    public boolean alive() {
        return state != State.DEAD;
    }

    public boolean visible() {
        return state != State.DEAD || stateTime < DEATH_DURATION;
    }

    public boolean flashVisible() {
        return state != State.HURT || (int) (stateTime * 30) % 2 == 0;
    }

    public double renderScale() {
        return state == State.DEAD
                ? Math.max(0, 1 - stateTime / DEATH_DURATION)
                : 1.0;
    }
}
