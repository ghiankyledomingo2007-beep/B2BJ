public final class RemnantRolesTest {
    public static void main(String[] args) {
        Wisp scout=new Wisp(600,500,500,700,2);
        Wisp guard=new Wisp(600,500,500,700,4,Wisp.Role.GUARD);
        assert guard.telegraphDuration()>scout.telegraphDuration() : "longer guard charge needs a longer tell";
        assert guard.lungeDuration()>scout.lungeDuration();
        for(int i=0;i<300&&guard.state()!=Wisp.State.TELEGRAPH;i++)guard.update(0.01,300,500);
        assert guard.state()==Wisp.State.TELEGRAPH;
        double x=guard.x();
        for(int i=0;i<300&&guard.state()!=Wisp.State.RECOVER;i++)guard.update(0.01,300,500);
        assert x-guard.x()>150 : "guard has a distinct committed long charge";
        assert guard.hurt(Player.BLADE_DAMAGE)&&!guard.alive() : "Blade remains decisive against guards";
        RuinedOutpostGame game=new RuinedOutpostGame();game.begin();
        for(int room=1;room<=3;room++) {
            for(Wisp w:game.scouts())w.hurt(100);
            game.update(0.01,0,0);
            final int next=room;
            var door=game.map().doors().stream().filter(d->d.destination()==next).findFirst().orElseThrow();
            game.player().relocate(door.x(),door.y());assert game.interact();
            if(room==1)assert game.scouts().stream().noneMatch(w->w.role()==Wisp.Role.GUARD);
            else assert game.scouts().stream().anyMatch(w->w.role()==Wisp.Role.GUARD) : "introduce guard after opening encounter";
        }
        System.out.println("RemnantRolesTest passed");
    }
}
