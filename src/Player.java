public final class Player {
    public static final double SPEED = 260.0;
    public static final double RADIUS = 48.0;
    public static final double COLLISION_RADIUS = 24.0;
    public static final double COLLISION_Y_OFFSET = 24.0;
    public static final double DASH_SPEED = 850.0;
    public static final double DASH_DURATION = 0.16;
    public static final double DASH_COOLDOWN = 0.4;
    public static final double ATTACK_COOLDOWN = 0.35;
    public static final double WATER_COOLDOWN = 0.48;
    public static final double HURT_INVULNERABILITY = 0.65;
    public static final double MAX_ICHOR = 100.0;
    public static final double BLADE_DURATION = 12.0;
    public static final double BLADE_SPEED_MULTIPLIER = 0.8;
    public static final int SLIME_DAMAGE = 1;
    public static final int BLADE_DAMAGE = SLIME_DAMAGE * 4;
    public static final int MAX_HEALTH = 5;

    private double x;
    private double y;
    private double dashX;
    private double dashY;
    private double dashTime;
    private double dashCooldown;
    private double attackCooldown;
    private double ichor;
    private double bladeTime;
    private double hurtInvulnerability;
    private double health = MAX_HEALTH;
    private double recoveryTime;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void move(int horizontal, int vertical, double seconds, RuinedOutpostMap map) {
        if (!Double.isFinite(seconds) || seconds <= 0) return;
        recoveryTime = Math.max(0, recoveryTime - seconds);
        dashCooldown = Math.max(0, dashCooldown - seconds);
        attackCooldown = Math.max(0, attackCooldown - seconds);
        hurtInvulnerability = Math.max(0, hurtInvulnerability - seconds);
        if (bladeTime > 0) {
            bladeTime = Math.max(0, bladeTime - seconds);
            ichor = MAX_ICHOR * bladeTime / BLADE_DURATION;
            if (bladeTime == 0) beginRecovery();
        }
        if (recovering() || !alive()) return;
        if (dashTime > 0) {
            double activeSeconds = Math.min(seconds, dashTime);
            moveDash(DASH_SPEED * activeSeconds, map);
            dashTime -= activeSeconds;
            return;
        }

        double length = Math.hypot(horizontal, vertical);
        if (length > 0) {
            double speed = bladeForm() ? SPEED * BLADE_SPEED_MULTIPLIER : SPEED;
            double distance = speed * seconds;
            int steps = Math.max(1, (int) Math.ceil(distance / 8));
            for (int i = 0; i < steps; i++)
                moveStep(horizontal / length, vertical / length, distance / steps, map);
        }
    }

    public boolean startAttack() {
        if (attackCooldown > 0 || recovering() || dashing() || !alive()) {
            return false;
        }
        attackCooldown = bladeForm() ? ATTACK_COOLDOWN : WATER_COOLDOWN;
        return true;
    }

    public int attackDamage() {
        return bladeForm() ? BLADE_DAMAGE : SLIME_DAMAGE;
    }

    public boolean hurt(int damage) {
        if (damage <= 0 || !alive() || invulnerable()) {
            return false;
        }
        health = Math.max(0, health - damage * (bladeForm() ? 0.75 : 1));
        if (bladeForm()) {
            ichor = Math.max(0, ichor - 5);
            bladeTime = BLADE_DURATION * ichor / MAX_ICHOR;
            if (bladeTime == 0) beginRecovery();
        }
        hurtInvulnerability = HURT_INVULNERABILITY;
        return true;
    }

    public int health() {
        return (int) Math.ceil(health);
    }

    private void beginRecovery() {
        recoveryTime = 1.5;
        dashTime = 0;
    }

    public boolean alive() {
        return health > 0;
    }

    public boolean invulnerable() {
        return alive() && (hurtInvulnerability > 0 || dashing());
    }

    public void collectIchor(double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !alive()) return;
        boolean blade = bladeForm();
        ichor = Math.min(MAX_ICHOR, ichor + amount);
        if (blade) bladeTime = BLADE_DURATION * ichor / MAX_ICHOR;
    }

    public boolean transform() {
        if (bladeForm() || recovering() || !alive() || ichor < MAX_ICHOR) {
            return false;
        }
        bladeTime = BLADE_DURATION;
        return true;
    }

    public boolean bladeForm() {
        return bladeTime > 0;
    }

    public double ichor() {
        return ichor;
    }

    public boolean dash(int horizontal, int vertical) {
        double length = Math.hypot(horizontal, vertical);
        if (length == 0 || dashCooldown > 0 || recovering() || !alive()) {
            return false;
        }

        dashX = horizontal / length;
        dashY = vertical / length;
        dashTime = DASH_DURATION;
        dashCooldown = DASH_COOLDOWN;
        return true;
    }

    public boolean dashing() {
        return dashTime > 0;
    }

    public boolean recovering() { return recoveryTime > 0; }
    public double dashCooldown() { return dashCooldown; }
    public double healthValue() { return health; }
    public double bladeSeconds() { return bladeTime; }
    public void heal() { health = MAX_HEALTH; }
    public void heal(double amount) {
        if (Double.isFinite(amount) && amount > 0 && alive()) health = Math.min(MAX_HEALTH, health + amount);
    }
    public void relocate(double newX, double newY) {
        x = newX;
        y = newY;
        dashTime = 0;
    }

    private void moveDash(double distance, RuinedOutpostMap map) {
        int steps = Math.max(1, (int) Math.ceil(distance / 8.0));
        double stepDistance = distance / steps;
        for (int step = 0; step < steps; step++) {
            moveStep(dashX, dashY, stepDistance, map);
        }
    }

    private void moveStep(double directionX, double directionY, double distance,
            RuinedOutpostMap map) {
        double nextX = clamp(x + directionX * distance,
                RADIUS, map.worldWidth() - RADIUS);
        if (!map.isBlocked(nextX, y + COLLISION_Y_OFFSET, COLLISION_RADIUS)) {
            x = nextX;
        }

        double nextY = clamp(y + directionY * distance,
                RADIUS, map.worldHeight() - RADIUS);
        if (!map.isBlocked(x, nextY + COLLISION_Y_OFFSET, COLLISION_RADIUS)) {
            y = nextY;
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
