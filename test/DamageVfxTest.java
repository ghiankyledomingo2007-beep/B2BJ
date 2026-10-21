import java.awt.image.BufferedImage;
import java.util.List;

public final class DamageVfxTest {
    public static void main(String[] args) throws Exception {
        var numbers=new DamageNumbers();
        numbers.add(100,100,0,false,false);numbers.add(Double.NaN,100,20,false,false);
        assert numbers.active().isEmpty();
        for(int i=0;i<100;i++)numbers.add(100,100,20,i%2==0,false);
        assert numbers.active().size()==DamageNumbers.LIMIT;
        numbers.update(Double.NaN);assert numbers.active().get(0).age==0;
        numbers.update(DamageNumbers.LIFE);assert numbers.active().isEmpty();
        var panel=new B2BJ(false);panel.setSize(1280,720);
        var handle=B2BJ.class.getDeclaredMethod("handleEvents",List.class);handle.setAccessible(true);
        var draw=B2BJ.class.getDeclaredMethod("drawDamageNumbers",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        handle.invoke(panel,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.DAMAGE_DEALT,300,300,false,36,true,false)));
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
        long gold=java.util.Arrays.stream(image.getRGB(0,0,1280,720,null,0,1280)).filter(p->p==0xffffd35c).count();
        assert gold>30 : "critical label uses readable gold pixel text";
        var full=new MobFx();var reduced=new MobFx();
        full.hit(100,100,true,true,false,false);reduced.hit(100,100,true,true,false,true);
        var bits=MobFx.class.getDeclaredField("bits");bits.setAccessible(true);
        assert ((List<?>)bits.get(reduced)).size()<((List<?>)bits.get(full)).size();
        for(int i=0;i<100;i++)full.hit(100,100,true,true,false,false);
        assert ((List<?>)bits.get(full)).size()<=420;
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(300,300),List.of(),new Guardian(1500,900));
        full.update(1,game);assert ((List<?>)bits.get(full)).isEmpty();
        var reset=B2BJ.class.getDeclaredMethod("resetVisualState");reset.setAccessible(true);reset.invoke(panel);
        var field=B2BJ.class.getDeclaredField("damageNumbers");field.setAccessible(true);
        assert ((DamageNumbers)field.get(panel)).active().isEmpty();
        System.out.println("DamageVfxTest passed: gold labels, bounded effects, reduced effects, expiry, reset");
    }
}
