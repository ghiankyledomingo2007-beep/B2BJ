public final class RainorayArtTest {
    public static void main(String[] args) {
        for(String action:new String[]{"idle","run","dash","cast","guard","hurt"})
            CombatArtTest.check("assets/characters/blade/rainoray_"+action+".png",80,80,8,3,8);
        CombatArtTest.check("assets/characters/blade/rainoray_slash.png",80,80,8,9,8);
        CombatArtTest.check("assets/characters/slime/slime_engulf.png",80,80,8,3,8);
        for(String name:new String[]{"transform_in","transform_out"})
            CombatArtTest.check("assets/effects/"+name+".png",80,80,16,3,16);
        for(String name:new String[]{"telegraph_chevron","aim_reticle","fissure_tile"})
            CombatArtTest.check("assets/effects/"+name+".png",32,32,8,1,8);
        CombatArtTest.check("assets/effects/telegraph_ring.png",128,128,8,1,8);
        CombatArtTest.check("assets/effects/ichor_crescent.png",64,64,8,1,8);
        for(String name:new String[]{"riposte_guard","riposte_counter"})
            CombatArtTest.check("assets/effects/"+name+".png",80,80,8,1,8);
        for(String name:new String[]{"crescent","riposte"})
            CombatArtTest.check("assets/ui/"+name+".png",32,32,1,1,1);
        for(var decoration:RuinedOutpostMap.Decoration.values()) {
            var image=B2BJ.loadImage(decoration.path());
            assert image!=null : "Missing reviewed dressing: "+decoration.path();
        }
        var ring=B2BJ.loadImage("assets/effects/telegraph_ring.png");
        for(int f=0;f<8;f++) {
            for(int y=42;y<86;y++)for(int x=42;x<86;x++)
                assert (ring.getRGB(f*128+x,y)>>>24)==0 : "danger ring must leave terrain visible";
        }
        System.out.println("RainorayArtTest passed");
    }
}
