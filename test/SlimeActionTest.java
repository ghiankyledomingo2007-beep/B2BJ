import java.util.List;

/** Close-range combo commitment, cancellation and animation use the actual simulation. */
public final class SlimeActionTest {
    public static void main(String[] args) throws Exception {
        lightReach();finisherCommitment();finisherRespectsCover();cancelledFinisher();poseTiming();
        System.out.println("SlimeActionTest passed: close range, finisher movement/pause/cover, cancel/hurt, pose timing");
    }
    private static RuinedOutpostGame fixture(Wisp... enemies) {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(enemies),new Guardian(1500,900));
        game.setCriticalRollForTesting(()->1);game.begin();return game;
    }
    private static void advance(RuinedOutpostGame game,double seconds,int horizontal) {
        while(seconds>1e-8){double dt=Math.min(.01,seconds);game.update(dt,horizontal,0);seconds-=dt;}
    }
    private static void finisher(RuinedOutpostGame game) {
        assert game.attack(1,0);advance(game,.41,0);
        assert game.attack(1,0);advance(game,.45,0);
        assert game.attack(1,0)&&game.waterKind()==WaterProjectile.Kind.FINISHER;
    }
    private static void lightReach() {
        Wisp near=new Wisp(630,400,630,630,100),far=new Wisp(850,400,850,850,100);
        var game=fixture(near,far);assert game.attack(1,0);advance(game,.4,0);
        assert near.health()<100 : "close lash must connect";
        assert far.health()==100 : "a light lash cannot snipe a distant enemy";
        var light=new WaterProjectile(500,400,1,0,WaterProjectile.Kind.CUT);
        var tide=new WaterProjectile(500,400,1,0,WaterProjectile.Kind.TIDE);
        light.advance(.4);tide.advance(.4);
        assert !light.alive()&&tide.alive() : "Tide retains a distinct range/control role";
        assert light.impulse()<tide.impulse()/4 : "light hits keep enemies within combo reach";
    }
    private static void finisherCommitment() {
        var game=fixture();finisher(game);double x=game.player().x();
        advance(game,.15,-1);
        assert Math.abs(game.player().x()-x)<1e-8 : "finisher windup cannot slide backward";
        assert game.projectiles().isEmpty() : "finisher must visibly prepare before release";
        double elapsed=game.slimeAttackElapsed();game.pause();advance(game,.5,-1);
        assert game.slimeAttackElapsed()==elapsed&&game.player().x()==x : "pause freezes the commitment";
        game.togglePause();advance(game,.16,-1);
        assert Math.abs(game.player().x()-x-24)<1e-7 : "release makes one controlled forward step";
        var hit=game.projectiles().get(0);assert hit.damage()==40&&hit.staggers();
        x=game.player().x();advance(game,.16,-1);
        assert game.player().x()<x : "movement returns during recovery";
        assert !game.attack(1,0) : "finisher recovery cannot be mashed away";
        advance(game,.22,0);assert game.slimeAttackElapsed()<0&&game.attack(1,0);
    }
    private static void cancelledFinisher() throws Exception {
        for(String cancellation:new String[]{"dash","hurt","transform"}) {
            var game=fixture();finisher(game);advance(game,.10,0);game.drainEvents();
            switch(cancellation) {
                case "dash" -> {assert game.dash(0,1);}
                case "hurt" -> {
                    var hurt=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);
                    hurt.setAccessible(true);hurt.invoke(game,1,480.,400.);
                }
                case "transform" -> {game.player().collectIchor(100);assert game.transform();}
            }
            advance(game,.5,0);
            assert game.projectiles().isEmpty() : cancellation+" cannot release a cancelled finisher";
            assert game.slimeAttackElapsed()<0 : cancellation+" cannot restart the cancelled body pose";
            assert game.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.FINISHER_RELEASE);
        }
    }
    private static void finisherRespectsCover() {
        var map=new RuinedOutpostMap(2);var cover=map.barriers().get(0);
        double x=cover.centerX()-cover.width()/2-Player.COLLISION_RADIUS-5;
        var player=new Player(x,cover.centerY()-Player.COLLISION_Y_OFFSET);
        var game=new RuinedOutpostGame(map,player,List.of(),new Guardian(1500,900));game.begin();
        assert !map.isBlocked(player.x(),player.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS);
        finisher(game);advance(game,.34,0);
        assert player.x()-x<24 : "the release step must stop at cover";
        assert !map.isBlocked(player.x(),player.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS)
                : "a finisher cannot force the player into terrain";
    }
    private static void poseTiming() {
        var pose=new SlimeAnimation();pose.face(1,0);
        pose.pose(.21,.68,.22);assert pose.castFrame()==2&&pose.attacking();
        pose.pose(.22,.68,.22);assert pose.castFrame()==3 : "impact pose begins at physical release";
        pose.pose(.66,.68,.22);assert pose.castFrame()==7;
        pose.pose(.68,.68,.22);assert !pose.attacking();
        boolean rejected=false;
        try {pose.pose(Double.NaN,.68,.22);}catch(IllegalArgumentException expected){rejected=true;}
        assert rejected : "invalid timing must never select invalid sprite cells";
    }
}
