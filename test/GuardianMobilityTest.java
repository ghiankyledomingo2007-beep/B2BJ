public final class GuardianMobilityTest {
    public static void main(String[] args) {
        RuinedOutpostMap map = new RuinedOutpostMap(9);
        Guardian boss = new Guardian(map.guardianX(), map.guardianY());
        double startX=boss.x(), startY=boss.y();
        boss.activate(650, 650);
        for(int i=0;i<600;i++) boss.update(0.01,650,650);
        assert Math.hypot(boss.x()-startX,boss.y()-startY)>80 : "boss must close distance between attacks";
        // Actual game must move the collider together with the boss, not leave spawn blocked.
        Player p=new Player(650,650);
        boss=new Guardian(map.guardianX(),map.guardianY());
        RuinedOutpostGame game=new RuinedOutpostGame(map,p,java.util.List.of(),boss);
        game.begin(); boss.activate(p.x(),p.y());
        for(int i=0;i<600&&p.alive();i++) { game.update(0.01,0,1); p.heal(); }
        assert Math.hypot(boss.x()-startX,boss.y()-startY)>80;
        assert map.touchesGuardian(boss.x(),boss.y()+32,1) : "live boss position must collide";
        assert !map.touchesGuardian(startX,startY+32,1) : "no ghost collider at old spawn";
        assert !map.waterBlocked(boss.x(),boss.y()+32,52) : "boss may not move through terrain";
        System.out.println("GuardianMobilityTest passed");
    }
}
