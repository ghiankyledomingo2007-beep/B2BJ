public final class RecoveryArtTest {
    public static void main(String[] args) {
        CombatArtTest.check("assets/characters/slime/slime_absorb.png",48,48,8,3,7);
        CombatArtTest.check("assets/effects/corpse_idle.png",48,48,8,1,7);
        CombatArtTest.check("assets/effects/corpse_consume.png",48,48,8,1,7);
        CombatArtTest.check("assets/effects/absorb_link.png",64,32,8,1,7);
        CombatArtTest.check("assets/effects/heal_motes.png",48,48,8,1,7);
        for(String name:new String[]{"vitality","blob","blade","slash","wave","dash","consume"})
            CombatArtTest.check("assets/ui/"+name+".png",32,32,1,1,1);
        for(String name:new String[]{"ichor_idle","ichor_pickup"}) {
            var sheet=B2BJ.loadImage("assets/effects/"+name+".png");
            for(int y=27;y<32;y++)for(int x=0;x<sheet.getWidth();x++)
                assert (sheet.getRGB(x,y)>>>24)==0 : "orb must not drag a baked ground shadow";
        }
        System.out.println("RecoveryArtTest passed");
    }
}
