public final class GuardianScaleTest {
    public static void main(String[] args) {
        assert Guardian.RENDER_SIZE==Guardian.CELL_SIZE*3 : "Warden needs crisp 3x native scale";
        var map=new RuinedOutpostMap(9);
        assert map.touchesGuardian(map.guardianX()+52,map.guardianY()+32,1) : "wider rendered feet need matching collision";
        assert !map.touchesGuardian(map.guardianX()+64,map.guardianY()+32,1) : "collision must not include transparent sprite padding";
        assert Guardian.MAX_HEALTH==240&&Guardian.SLAM_DAMAGE==2&&Guardian.SLAM_RADIUS==112
                : "visual size request must not silently buff boss stats";
        System.out.println("GuardianScaleTest passed");
    }
}
