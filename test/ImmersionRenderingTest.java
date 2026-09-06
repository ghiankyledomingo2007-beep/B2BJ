import java.util.List;

public final class ImmersionRenderingTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);
        var method=B2BJ.class.getDeclaredMethod("handleEvents",List.class);method.setAccessible(true);
        method.invoke(panel,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.PLAYER_HIT,900,600),
                new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.PICKUP,500,500)));
        var field=B2BJ.class.getDeclaredField("impacts");field.setAccessible(true);
        var impacts=(List<?>)field.get(panel);
        var kind=impacts.get(0).getClass().getDeclaredField("kind");kind.setAccessible(true);
        assert kind.get(impacts.get(0)).toString().equals("SLIME_HURT");
        assert kind.get(impacts.get(1)).toString().equals("PICKUP") : "gold pickup must not reuse blue water splash";
        panel.game().player().collectIchor(100);panel.game().player().transform();
        method.invoke(panel,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.PLAYER_HIT,900,600)));
        assert kind.get(impacts.get(2)).toString().equals("BLADE_HURT");
        for(String path:new String[]{"ichor_idle","ichor_pickup","spit_orb","stone_chips"})
            CombatArtTest.check("assets/effects/"+path+".png",32,32,8,1,8);
        for(String path:new String[]{"slime_hurt","armor_sparks"})
            CombatArtTest.check("assets/effects/"+path+".png",48,48,8,2,8);
        CombatArtTest.check("assets/effects/spit_impact.png",48,48,8,1,8);
        CombatArtTest.check("assets/characters/wisp/spitter_attack.png",48,48,8,3,7);
        CombatArtTest.check("assets/characters/wisp/spitter_walk.png",48,48,4,3,3);
        System.out.println("ImmersionRenderingTest passed");
    }
}
