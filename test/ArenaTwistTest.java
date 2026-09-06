public final class ArenaTwistTest {
    public static void main(String[] args) {
        Guardian healthy=new Guardian(1000,500);healthy.activate(800,500);
        for(int i=0;i<5000;i++) {
            healthy.update(.01,800,500);
            assert healthy.attack()!=Guardian.Attack.FISSURE : "phase one keeps its original move set";
        }
        Guardian boss=new Guardian(1000,500);boss.activate(800,500);boss.hurt(120);
        boolean lastHorizontal=false;int lanes=0;
        for(int i=0;i<12000&&lanes<4;i++) {
            boss.update(.01,800,500);
            if(boss.attack()!=Guardian.Attack.FISSURE||boss.state()!=Guardian.State.TELEGRAPH)continue;
            var lane=boss.fissureBounds();
            boolean horizontal=lane.width()>lane.height();
            if(lanes>0)assert horizontal!=lastHorizontal : "successive fissures alternate their escape direction";
            assert Math.min(lane.width(),lane.height())==84;
            assert Math.max(lane.width(),lane.height())==1152;
            assert boss.telegraphDuration()>=1.1 : "full warning before lane erupts";
            assert !boss.hits(lane.centerX(),lane.centerY()) : "warning cannot damage";
            double elapsed=boss.stateSeconds();
            boss.update(0,0,0);
            assert boss.stateSeconds()==elapsed : "no time means no warning advancement";
            boss.update(.4,1500,900);
            assert lane.equals(boss.fissureBounds()) : "lane must not track a dodging player";
            while(boss.state()==Guardian.State.TELEGRAPH)boss.update(.01,1500,900);
            int impact=boss.impactNumber();
            assert boss.hits(lane.centerX(),lane.centerY());
            double safeX=lane.centerX()+(horizontal?0:72),safeY=lane.centerY()+(horizontal?72:0);
            assert !boss.hits(safeX,safeY) : "one short perpendicular step clears the whole footprint";
            boss.update(.1,safeX,safeY);
            assert boss.impactNumber()==impact : "one impact ID for the whole active lane";
            boss.update(.4,safeX,safeY);
            assert boss.fissureBounds()==null&&!boss.hits(lane.centerX(),lane.centerY());
            lastHorizontal=horizontal;lanes++;
        }
        assert lanes==4 : "repeatable second-phase cadence";
        while(boss.fissureBounds()==null)boss.update(.01,800,500);
        boss.hurt(Guardian.MAX_HEALTH);
        assert boss.fissureBounds()==null : "death clears hazard immediately";
        verifyEscapeAtEdges();
        verifyDressing();
        System.out.println("ArenaTwistTest passed");
    }
    private static void verifyEscapeAtEdges() {
        RuinedOutpostMap map=new RuinedOutpostMap(9);map.clearGuardian();
        double[][] positions={{192,192},{1728,192},{192,960},{1728,960},{960,576},{64,576},{960,64},{1664,576}};
        for(double[] p:positions) {
            Guardian boss=new Guardian(map.guardianX(),map.guardianY());boss.activate(p[0],p[1]);boss.hurt(120);
            for(int turn=0;turn<2;turn++) {
                for(int i=0;i<4000;i++) {
                    boss.update(.01,p[0],p[1]);
                    if(boss.attack()==Guardian.Attack.FISSURE&&boss.state()==Guardian.State.TELEGRAPH)break;
                }
                var lane=boss.fissureBounds();assert lane!=null;
                boolean horizontal=lane.width()>lane.height(),escape=false;
                for(int sign:new int[]{-1,1}) {
                    double ex=p[0]+(horizontal?0:sign*96),ey=p[1]+(horizontal?sign*96:0);
                    if(map.clearWaterLine(p[0],p[1]+Player.COLLISION_Y_OFFSET,
                            ex,ey+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))escape=true;
                }
                assert escape : "a walking escape must stay open at "+p[0]+","+p[1];
                boss.update(1.5,p[0],p[1]);
            }
        }
        // Sweep all traversable 32px samples, not only the scripted player positions.
        map=new RuinedOutpostMap(9);
        for(int y=32;y<map.worldHeight();y+=32)for(int x=32;x<map.worldWidth();x+=32) {
            if(map.isBlocked(x,y+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))continue;
            for(boolean horizontal:new boolean[]{false,true}) {
                boolean escape=false;
                for(int sign:new int[]{-1,1}) {
                    double ex=x+(horizontal?0:sign*96),ey=y+(horizontal?sign*96:0);
                    boolean clear=true;
                    for(int step=0;step<=12;step++) {
                        if(map.isBlocked(x+(ex-x)*step/12,y+(ey-y)*step/12+Player.COLLISION_Y_OFFSET,
                                Player.COLLISION_RADIUS)){clear=false;break;}
                    }
                    escape|=clear;
                }
                assert escape||walkingEscape(map,x,y,horizontal)
                        : "no walking escape before impact at "+x+","+y+" horizontal="+horizontal;
            }
        }
    }
    private static boolean walkingEscape(RuinedOutpostMap map,int x,int y,boolean horizontal) {
        // Irregular banks sometimes require an L-shaped step. 192px takes <1s even in Blade form.
        java.util.ArrayDeque<int[]> queue=new java.util.ArrayDeque<>();
        boolean[][] seen=new boolean[25][25];queue.add(new int[]{0,0,0});seen[12][12]=true;
        while(!queue.isEmpty()) {
            int[] p=queue.remove();
            if(Math.abs(horizontal?p[1]:p[0])*16>=72)return true;
            if(p[2]==12)continue;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int dx=p[0]+d[0],dy=p[1]+d[1];
                if(seen[dy+12][dx+12])continue;
                seen[dy+12][dx+12]=true;
                if(!map.isBlocked(x+dx*16,y+dy*16+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))
                    queue.add(new int[]{dx,dy,p[2]+1});
            }
        }
        return false;
    }
    private static void verifyDressing() {
        java.util.Set<RuinedOutpostMap.Decoration> kinds=new java.util.HashSet<>();
        for(int room=0;room<RuinedOutpostMap.ROOMS.size();room++) {
            var map=new RuinedOutpostMap(room);
            assert map.dressing().size()>=2 : "authored dressing per room";
            for(var item:map.dressing()) {
                kinds.add(item.decoration());
                boolean solid=item.decoration()==RuinedOutpostMap.Decoration.CART
                        ||item.decoration()==RuinedOutpostMap.Decoration.BRAZIER;
                double cx=item.x()+(item.decoration()==RuinedOutpostMap.Decoration.CART?6:0);
                double cy=item.y()-(item.decoration()==RuinedOutpostMap.Decoration.CART?20:0);
                if(solid) {
                    assert map.isBlocked(cx,cy,1) : "upright dressing feet must block actors";
                    assert map.waterBlocked(cx,cy,1) : "upright dressing feet must block projectiles";
                    assert !map.clearLine(cx-100,cy,cx+100,cy) : "combat sight lines must respect solid feet";
                    assert !map.clearWaterLine(cx-100,cy,cx+100,cy,4);
                    assert !map.waterBlocked(cx,cy-40,1) : "upper sprite must not become a tall invisible wall";
                } else assert !map.waterBlocked(item.x(),item.y(),24) : "flat wreckage stays walkable";
                assert Math.hypot(item.x()-map.spawnX(),item.y()-map.spawnY())>180;
                for(var door:map.doors())assert Math.hypot(item.x()-door.x(),item.y()-door.y())>180;
            }
        }
        assert kinds.size()==4;
        assert new RuinedOutpostMap(9).barriers().isEmpty() : "arena remains open; no new full-width walls";
    }
}
