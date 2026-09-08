import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Campaign identity and combat rules around the existing terrain-aware enemy body. */
public final class CampaignEnemy {
    public enum Kind {
        OUTPOST_SCOUT("Outpost Remnant", Wisp.Role.SCOUT, 2, 10, 1),
        OUTPOST_GUARD("Outpost Guard", Wisp.Role.SHIELD_GUARD, 4, 10, 1),
        OUTPOST_SPITTER("Ichor Spitter", Wisp.Role.SPITTER, 3, 10, 1),
        RIFT_IMP("Rift Imp", Wisp.Role.SCOUT, 3, 10, 1),
        THORN_WOLF("Thorn Wolf", Wisp.Role.GUARD, 6, 15, 2),
        MIRE_SHAMAN("Mire Shaman", Wisp.Role.SPITTER, 5, 15, 1),
        FALLEN_KNIGHT("Fallen Knight", Wisp.Role.KNIGHT, 12, 25, 2),
        CINDER_HEXER("Cinder Hexer", Wisp.Role.SPITTER, 6, 20, 1);

        private final String name;
        private final Wisp.Role role;
        private final int health, ichor, damage;

        Kind(String name, Wisp.Role role, int health, int ichor, int damage) {
            this.name = name; this.role = role; this.health = health;
            this.ichor = ichor; this.damage = damage;
        }

        public String displayName() { return name; }
    }

    private final Kind kind;
    private final Wisp body;
    private int consumedShot;
    private double approachTime;

    public CampaignEnemy(Kind kind, double x, double y) {
        this.kind = Objects.requireNonNull(kind);
        if (!Double.isFinite(x) || !Double.isFinite(y))
            throw new IllegalArgumentException("Invalid campaign enemy position");
        body = new Wisp(x, y, x - 56, x + 56, kind.health, kind.role);
    }

    public void update(double seconds, double targetX, double targetY, RuinedOutpostMap map) {
        if (!Double.isFinite(seconds) || seconds <= 0
                || !Double.isFinite(targetX) || !Double.isFinite(targetY)) return;
        for (double left = Math.min(seconds, .5); left > 1e-8;) {
            double dt = Math.min(left, .01);
            approachTime += dt;
            double x = targetX, y = targetY;
            double dx = targetX - body.x(), dy = targetY - body.y(), distance = Math.hypot(dx, dy);
            if (kind == Kind.RIFT_IMP && (body.state() == Wisp.State.PATROL || body.state() == Wisp.State.PURSUE)
                    && distance > 200 && distance < Wisp.AWARENESS_RANGE - 70) {
                double weave = Math.sin(approachTime * 6) * 65;
                x -= dy / distance * weave;
                y += dx / distance * weave;
            }
            body.update(dt, x, y, map);
            left -= dt;
        }
    }

    /** All slime damage, including dash contact, must pass through the same shield rule. */
    public boolean receiveDamage(int damage, boolean slime, boolean stagger) {
        return !(slime && shielded()) && body.hurt(damage, stagger);
    }

    public boolean hurt(int damage, boolean slime, boolean stagger) {
        return receiveDamage(damage, slime, stagger);
    }

    public boolean shielded() {
        return kind == Kind.FALLEN_KNIGHT && body.alive()
                && body.state() != Wisp.State.RECOVER && body.state() != Wisp.State.HURT;
    }

    public boolean hits(double x, double y) {
        if (body.state() != Wisp.State.LUNGE || body.role() == Wisp.Role.SPITTER) return false;
        double dx = x - body.x(), dy = y - body.y();
        if (!body.stationaryMelee()) return Math.hypot(dx, dy) <= RuinedOutpostGame.WISP_CONTACT_RADIUS;
        double forward = dx * body.intentX() + dy * body.intentY();
        double sideways = Math.abs(dx * body.intentY() - dy * body.intentX());
        return forward >= 0 && forward <= body.meleeReach() && sideways <= body.meleeHalfWidth();
    }

    /** Consume after outgoing player damage: a staggered/dead caster cannot trade a pending shot. */
    public List<EnemyProjectile> releaseShots() {
        if (body.role() != Wisp.Role.SPITTER || body.shotNumber() == consumedShot)
            return List.of();
        consumedShot = body.shotNumber();
        if (!body.alive() || body.state() != Wisp.State.LUNGE) return List.of();
        double angle = Math.atan2(body.intentY(), body.intentX());
        int count = kind == Kind.CINDER_HEXER ? 8 : 3;
        List<EnemyProjectile> shots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double direction = angle + (kind == Kind.CINDER_HEXER ? i * Math.PI / 4 : (i - 1) * .2);
            shots.add(new EnemyProjectile(body.x(), body.y(), Math.cos(direction), Math.sin(direction)));
        }
        return shots;
    }

    public Wisp body() { return body; }
    public Kind kind() { return kind; }
    public String name() { return kind.name; }
    public int damage() { return kind.damage; }
    public int ichor() { return kind.ichor; }
}
