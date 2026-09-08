import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AbsorbedTraitTest {
    public static void main(String[] args) throws Exception {
        protection();
        channelAndReplacement();
        jetAndExpiry();
        cancellationAndSummons();
        System.out.println("AbsorbedTraitTest passed: shield, jet, channel, pause, expiry, replacement, summons, death/form reset");
    }
    private static void protection() {
        var p=new Player(500,400);p.gainTrait(Player.Trait.MEMBRANE);
        p.setGodMode(true);assert !p.hurt(2)&&p.trait()==Player.Trait.MEMBRANE;
        p.setGodMode(false);assert p.hurt(2)&&p.healthValue()==3.5 : "membrane absorbs half a heart once";
        assert p.trait()==Player.Trait.NONE;
        p.gainTrait(Player.Trait.MEMBRANE);assert !p.hurt(1)&&p.trait()==Player.Trait.MEMBRANE
                : "invulnerable contacts never consume protection";
        p.move(0,0,.7,new RuinedOutpostMap());assert p.hurt(1)&&p.healthValue()==3;
        p.move(0,0,.7,new RuinedOutpostMap());assert p.hurt(1)&&p.healthValue()==2;
        p.gainTrait(Player.Trait.MEMBRANE);p.collectIchor(100);assert p.transform();
        assert p.trait()==Player.Trait.NONE : "slime traits cannot stack with Blade defense";
        p.gainTrait(Player.Trait.JET);assert p.trait()==Player.Trait.NONE;
        var dead=new Player(500,400);dead.gainTrait(Player.Trait.JET);dead.hurt(99);
        assert dead.trait()==Player.Trait.NONE;dead.gainTrait(Player.Trait.MEMBRANE);
        assert dead.trait()==Player.Trait.NONE;
    }
    private static void channelAndReplacement() throws Exception {
        var game=fixture();game.player().collectIchor(100);
        corpse(game,CampaignEnemy.Kind.OUTPOST_GUARD,false);
        assert game.prompt().contains("MEMBRANE") : "show the corpse reward before committing to eat";
        assert game.interact() : "useful new trait can be eaten at full health and Ichor";
        advance(game,.4);assert game.player().trait()==Player.Trait.NONE;
        game.pause();advance(game,2);assert game.player().trait()==Player.Trait.NONE;
        game.togglePause();advance(game,.41);assert game.player().trait()==Player.Trait.MEMBRANE;
        assert game.player().traitSeconds()>9.9&&game.corpses().isEmpty();
        corpse(game,CampaignEnemy.Kind.OUTPOST_GUARD,false);
        assert !game.interact() : "do not waste an identical fresh trait at full resources";
        corpse(game,CampaignEnemy.Kind.OUTPOST_SPITTER,false);
        assert game.interact();advance(game,.81);
        assert game.player().trait()==Player.Trait.JET : "new trait replaces, never stacks";
        assert game.player().hurt(1)&&game.player().healthValue()==4;
        game.restart();assert game.player().trait()==Player.Trait.NONE;
    }
    private static void jetAndExpiry() {
        var boosted=fixture();boosted.player().gainTrait(Player.Trait.JET);
        var plain=fixture();assert boosted.attack(1,0)&&plain.attack(1,0);
        advance(boosted,.12);advance(plain,.12);
        var a=boosted.projectiles().get(0);var b=plain.projectiles().get(0);
        assert Math.abs(a.speed()/b.speed()-1.3)<1e-8&&a.damage()==b.damage();
        assert a.jet()&&!b.jet();assert !boosted.attack(1,0) : "jet does not remove casting recovery";
        double remaining=boosted.player().traitSeconds();boosted.pause();advance(boosted,3);
        assert boosted.player().traitSeconds()==remaining;
        boosted.togglePause();advance(boosted,10.1);assert boosted.player().trait()==Player.Trait.NONE;
        assert boosted.attack(1,0);advance(boosted,.12);assert !boosted.projectiles().get(0).jet();
    }
    private static void cancellationAndSummons() throws Exception {
        for(CampaignEnemy.Kind kind:CampaignEnemy.Kind.values()) {
            var game=fixture();corpse(game,kind,true);assert game.interact();advance(game,.81);
            assert game.player().trait()==Player.Trait.NONE : "infinite summons cannot farm traits: "+kind;
        }
        var cancelled=fixture();corpse(cancelled,CampaignEnemy.Kind.OUTPOST_SPITTER,false);
        assert cancelled.interact();advance(cancelled,.4);cancelled.update(.01,1,0);advance(cancelled,.5);
        assert cancelled.player().trait()==Player.Trait.NONE&&cancelled.corpses().size()==1;
    }
    @SuppressWarnings("unchecked")
    private static void corpse(RuinedOutpostGame game,CampaignEnemy.Kind kind,boolean summon) throws Exception {
        var enemy=new CampaignEnemy(kind,530,400);enemy.body().hurt(100);
        var identities=RuinedOutpostGame.class.getDeclaredField("campaignEnemies");identities.setAccessible(true);
        ((Map<Wisp,CampaignEnemy>)identities.get(game)).put(enemy.body(),enemy);
        if(summon) {
            var adds=RuinedOutpostGame.class.getDeclaredField("bossAdds");adds.setAccessible(true);
            ((Set<Wisp>)adds.get(game)).add(enemy.body());
        }
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);
        hit.setAccessible(true);hit.invoke(game,enemy.body(),true);
        advance(game,.1);game.drainEvents();
    }
    private static RuinedOutpostGame fixture() {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(),new Guardian(1500,900));
        game.begin();return game;
    }
    private static void advance(RuinedOutpostGame game,double seconds) {
        for(double left=seconds;left>1e-8;left-=.01)game.update(Math.min(.01,left),0,0);
    }
}
