public final class GuardianTest {
    public static void main(String[] args) {
        Guardian g=new Guardian(300,200);
        assert !g.hurt(1);
        g.activate(120,140);
        g.update(Guardian.TELEGRAPH_DURATION,999,999);
        assert g.state()==Guardian.State.SLAM;
        assert g.targetX()==120&&g.targetY()==140 : "slam must not follow until impact";
        assert g.impactNumber()==1;
        assert g.hurt(4);
        assert g.state()==Guardian.State.SLAM : "damage must not cancel boss attacks";
        assert g.health()==Guardian.MAX_HEALTH-4;
        g.update(Guardian.SLAM_DURATION,0,0);
        assert g.state()==Guardian.State.RECOVER;
        g.update(Guardian.RECOVER_DURATION,220,240);
        assert g.state()==Guardian.State.APPROACH : "recovery must lead into visible repositioning";
        java.util.Set<Guardian.Attack> attacks=new java.util.HashSet<>();
        for(int i=0;i<2000;i++) { g.update(0.01,g.x()+600,g.y()); attacks.add(g.attack()); }
        assert attacks.contains(Guardian.Attack.CHARGE)&&attacks.contains(Guardian.Attack.SHOCKWAVE)
                : "boss needs different answers to ranged spacing";
        g.hurt(Guardian.MAX_HEALTH/2);
        assert g.enraged() : "half health activates second phase";
        assert Guardian.MAX_HEALTH > Player.BLADE_DAMAGE*Player.BLADE_DURATION/Player.ATTACK_COOLDOWN
                : "boss must exceed one uninterrupted base transformation";
        g.hurt(Guardian.MAX_HEALTH);
        assert !g.alive();
        g.update(Guardian.DEATH_DURATION,0,0);
        assert !g.visible();
        System.out.println("GuardianTest passed");
    }
}
