import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class RuinedOutpostMapTest {
    public static void main(String[] args) {
        Set<Integer> graph=new HashSet<>();
        visit(0,graph);
        assert graph.size()==12 : "all twelve rooms must connect";
        long optional=graph.stream().filter(i->i>0&&new RuinedOutpostMap(i).doors().size()==1).count();
        assert optional==2 : "two optional branches";
        Set<String> tiles=new HashSet<>();
        for(int mask=0;mask<16;mask++)
            tiles.add(WangTileset.sourceX(mask)+","+WangTileset.sourceY(mask));
        assert tiles.size()==16;
        var palisade=new RuinedOutpostMap(1).barriers().get(0);
        assert palisade.width()==152 && palisade.height()==24 : "palisade footprint must fit the wooden posts, not its canvas";
        var cot=new RuinedOutpostMap(3).barriers().get(0);
        assert cot.width()==160 && cot.height()==44 : "cot footprint must fit its four legs";
        var standard=new RuinedOutpostMap(4).barriers().get(0);
        assert standard.width()==40 && standard.height()==24 : "flag fabric must not become a giant collider";
        var supplies=new RuinedOutpostMap(5).barriers();
        assert supplies.size()==4 && supplies.stream().allMatch(o->o.prop()!=null)
                : "supply lane needs individually grounded barrels, not long placeholder walls";
        assert new RuinedOutpostMap(0).barriers().size()==2 : "rain ditch should have two grounded bare trees";
        // Absorption, Ichor, Spitter, Water and Wisp tests all read this cover; keep its 96x48 feet and clear surround.
        var cover=new RuinedOutpostMap(2).barriers().get(0);
        assert cover.prop()==RuinedOutpostMap.Prop.WEAPON_RACK && cover.width()==96 && cover.height()==48
                : "muster ground keeps one 96x48 cover as its first barrier";
        assert new RuinedOutpostMap(2).barriers().size()==1 : "muster ground has no other blockouts";
        assert new RuinedOutpostMap(7).barriers().get(0).prop()==RuinedOutpostMap.Prop.TREE
                : "camp cover is the tree the lean-to rests against, not a brick blockout";
        for(int room:new int[]{7,10}) {
            RuinedOutpostMap map=new RuinedOutpostMap(room);
            assert !map.isBlocked(map.restX(),map.restY()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS)
                    : "rest point must stay standable in room "+room;
            assert map.dressing().stream().anyMatch(d->d.x()==map.restX()&&d.y()==map.restY()
                    &&d.decoration()==(room==7?RuinedOutpostMap.Decoration.BEDROLL:RuinedOutpostMap.Decoration.BANDAGE))
                    : "rest interaction art must sit exactly on the rest point";
        }
        for(int room=0;room<12;room++) {
            RuinedOutpostMap map=new RuinedOutpostMap(room);
            assert map.worldWidth()>1280&&map.worldHeight()>720 : "outdoor grounds must scroll";
            assert !map.isBlocked(map.spawnX(),map.spawnY()+24,24) : "blocked spawn "+room;
            boolean earth=false,stone=false,transition=false;
            for(int y=0;y<map.heightInTiles();y++) for(int x=0;x<map.widthInTiles();x++) {
                int mask=map.tileMask(x,y);
                earth|=mask==0;stone|=mask==15;transition|=mask>0&&mask<15;
            }
            assert earth&&stone&&transition : "terrain mix "+room;
            for(var o:map.barriers()) assert map.isBlocked(o.centerX(),o.centerY(),24);
            assertDoorsReachable(map);
        }
        RuinedOutpostMap boss=new RuinedOutpostMap(9);
        assert !boss.exitReached(boss.gateX(),boss.gateY());
        assert boss.isBlocked(boss.guardianX(),boss.guardianY()+32,24);
        boss.clearGuardian(); boss.openGate();
        assert !boss.isBlocked(boss.guardianX(),boss.guardianY()+32,24);
        assert boss.exitReached(boss.gateX(),boss.gateY());
        System.out.println("RuinedOutpostMapTest passed");
    }
    private static void visit(int room,Set<Integer> seen) {
        if(!seen.add(room)) return;
        for(var door:new RuinedOutpostMap(room).doors()) visit(door.destination(),seen);
    }
    private static void assertDoorsReachable(RuinedOutpostMap map) {
        int width=map.worldWidth()/16,height=map.worldHeight()/16;
        boolean[][] seen=new boolean[height+1][width+1];
        ArrayDeque<int[]> queue=new ArrayDeque<>();
        queue.add(new int[]{width/2,height/2});seen[height/2][width/2]=true;
        while(!queue.isEmpty()) {
            int[] cell=queue.remove();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int x=cell[0]+d[0],y=cell[1]+d[1];
                if(x<0||x>width||y<0||y>height||seen[y][x]||map.isBlocked(x*16,y*16+24,24)) continue;
                seen[y][x]=true;queue.add(new int[]{x,y});
            }
        }
        for(var door:map.doors()) {
            boolean reachable=false;
            for(int y=0;y<=height;y++) for(int x=0;x<=width;x++)
                if(seen[y][x]&&Math.hypot(x*16-door.x(),y*16-door.y())<120) reachable=true;
            assert reachable : "unreachable exit in room "+map.room()+" to "+door.destination();
        }
    }
}
