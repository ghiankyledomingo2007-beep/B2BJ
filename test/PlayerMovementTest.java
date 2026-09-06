public final class PlayerMovementTest {
    public static void main(String[] args) {
        RuinedOutpostMap map=new RuinedOutpostMap();
        Player p=new Player(300,300);
        p.move(1,0,0.1,map);
        assert Math.abs(p.x()-326)<0.001;
        p=new Player(300,300);
        p.move(1,1,0.1,map);
        assert Math.abs(Math.hypot(p.x()-300,p.y()-300)-26)<0.001;
        p=new Player(200,200);
        p.move(-1,-1,1,map);
        assert !map.isBlocked(p.x(),p.y()+24,24) : "walking penetrates perimeter";
        map=new RuinedOutpostMap(1);
        var fence=map.barriers().get(0);
        double contactX=fence.centerX()-fence.width()/2-Player.COLLISION_RADIUS;
        p=new Player(490,360);
        p.move(1,0,1,map);
        assert p.x()<=contactX && p.x()>contactX-8 : "feet should meet barricade without a large gap";
        p=new Player(460,360);
        assert p.dash(1,0);
        assert p.invulnerable() && !p.hurt(1) : "dash must evade damage";
        p.move(0,0,Player.DASH_DURATION,map);
        assert p.x()<=contactX && !map.isBlocked(p.x(),p.y()+24,24) : "dash tunnels into wall";
        assert !p.dash(1,0);
        p.move(0,0,Player.DASH_COOLDOWN,map);
        assert p.dash(1,0);
        double x=p.x();
        p.move(1,0,Double.NaN,map);
        assert p.x()==x;
        for(int room:new int[]{0,1,3,4,5,6}) {
            map=new RuinedOutpostMap(room);
            for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                // Approach the outside barrel of a cluster, not through its neighbour.
                var prop=map.barriers().get(room==5&&direction[1]<0?map.barriers().size()-1:0);
                int dx=direction[0],dy=direction[1];
                double contact=(dx==0?prop.height():prop.width())/2+Player.COLLISION_RADIUS;
                p=new Player(prop.centerX()-dx*(contact+80),prop.centerY()-Player.COLLISION_Y_OFFSET-dy*(contact+80));
                p.move(dx,dy,0.5,map);
                double distance=dx==0?Math.abs(p.y()+Player.COLLISION_Y_OFFSET-prop.centerY()):Math.abs(p.x()-prop.centerX());
                assert distance>=contact && distance<contact+8 : "incorrect prop contact room="+room+" direction="+dx+","+dy;
                assert !map.isBlocked(p.x(),p.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS);
            }
        }
        System.out.println("PlayerMovementTest passed");
    }
}
