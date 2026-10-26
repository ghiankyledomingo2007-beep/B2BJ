import java.awt.image.BufferedImage;
import java.util.List;

public final class MobEffectsTest {
    private static BufferedImage draw(MobFx fx,boolean reduced) {
        var image=new BufferedImage(400,320,2);var g=image.createGraphics();
        fx.drawGround(g,0,0,reduced);fx.drawAbove(g,0,0,reduced);g.dispose();return image;
    }
    private static long pixels(BufferedImage image,int color) {
        return java.util.Arrays.stream(image.getRGB(0,0,400,320,null,0,400)).filter(p->p==color).count();
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var mage=new CampaignEnemy(CampaignEnemy.Kind.MIRE_SHAMAN,90,160);
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(260,160),List.of(mage.body()),new Guardian(1400,900));
        var fx=new MobFx();fx.update(.01,game);
        for(int i=0;i<60;i++){mage.update(.01,260,160,null);fx.update(.01,game);}
        assert pixels(draw(fx,false),0xff8fa961)>0 : "a winding-up shaman forms a visible spell";
        for(int i=0;i<82;i++){mage.update(.01,260,160,null);fx.update(.01,game);}
        var released=mage.releaseHazards();assert released.size()==1;
        var f=RuinedOutpostGame.class.getDeclaredField("forestHazards");f.setAccessible(true);
        ((List<ForestHazard>)f.get(game)).addAll(released);
        for(var h:released)h.update(.2);fx.update(.2,game);
        assert pixels(draw(fx,false),0xffabb47a)>10 : "released curse has raised thorn tips";
        assert pixels(draw(fx,true),0xffabb47a)>10 : "reduced effects preserve the actual threat";
        mage.body().hurt(10000);fx.update(.4,game);
        assert pixels(draw(fx,false),0xffabb47a)>10 : "killing the caster does not hide a live hazard";
        released.get(0).update(4);fx.update(.4,game);
        assert pixels(draw(fx,false),0xffabb47a)==0 : "thorns disappear when damage ends";
        fx.clear();assert java.util.Arrays.stream(draw(fx,false).getRGB(0,0,400,320,null,0,400)).allMatch(p->p==0);
        var wolf=new CampaignEnemy(CampaignEnemy.Kind.THORN_WOLF,90,160);
        game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(190,160),List.of(wolf.body()),new Guardian(1400,900));
        fx.update(.01,game);for(int i=0;i<83;i++){wolf.update(.01,190,160,null);fx.update(.01,game);}
        assert pixels(draw(fx,false),0xffd7d4ae)>30 : "a bite shows closing jaws instead of a generic shield arc";
        System.out.println("MobEffectsTest passed: caster charge, released thorns, reduced effects, expiry, clear and wolf bite");
    }
}
