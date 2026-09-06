import java.util.List;

public final class HurtFeedbackPositionTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);panel.game().player().relocate(300,280);
        var method=B2BJ.class.getDeclaredMethod("handleEvents",List.class);method.setAccessible(true);
        method.invoke(panel,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.PLAYER_HIT,900,650)));
        var field=B2BJ.class.getDeclaredField("impacts");field.setAccessible(true);
        var impacts=(List<?>)field.get(panel);var impact=impacts.get(0);
        var x=impact.getClass().getDeclaredField("x");var y=impact.getClass().getDeclaredField("y");
        x.setAccessible(true);y.setAccessible(true);
        assert x.getDouble(impact)==300&&y.getDouble(impact)==280 : "hurt effect must appear on player, not distant attacker";
        System.out.println("HurtFeedbackPositionTest passed");
    }
}
