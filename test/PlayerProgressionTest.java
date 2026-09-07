public final class PlayerProgressionTest {
    public static void main(String[] args) {
        Player player=new Player(400,400);
        player.configureUpgrades(2,2,2,1);
        assert player.maxHealth()==7;
        player.heal();
        assert player.health()==7;
        assert player.bladeComboLength()==4;
        assert Math.abs(player.dropMultiplier()-1.2)<1e-9;
        player.collectIchor(100);
        assert player.transform();
        assert player.bladeDuration()>Player.BLADE_DURATION;
        player.move(0,0,1,new RuinedOutpostMap(0));
        assert Math.abs(player.ichor()-(100-(100.0/12-1)))<1e-6;
        assert player.spendBladeIchor(player.ichor());
        assert player.recovering()&&!player.bladeForm();
        boolean rejected=false;
        try { player.configureUpgrades(-1,0,0,0); } catch(IllegalArgumentException expected) { rejected=true; }
        assert rejected;
        Player basic=new Player(400,400);
        assert basic.maxHealth()==Player.MAX_HEALTH;
        assert basic.bladeDuration()==Player.BLADE_DURATION;
        assert basic.bladeComboLength()==3;
        System.out.println("Player progression tests passed");
    }
}
