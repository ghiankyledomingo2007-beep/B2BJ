import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Room-by-room placement invariants from the map art audit: open fight floors, reachable exits, purposeful landmarks. */
public final class OutpostLayoutTest {
    /** Authored enemy spawn order from RuinedOutpostGame.enterRoom and the Warden's summon slots. */
    private static final double[][] SPAWNS={{440,384},{840,384},{640,240},{640,544},{960,384}};
    private static final double[][] SUMMONS={{400,544},{920,544},{400,240},{920,240},{640,544},{640,240}};
    private static final EnumSet<RuinedOutpostMap.Decoration> LANDMARKS=EnumSet.of(
            RuinedOutpostMap.Decoration.BRAZIER_UNLIT,RuinedOutpostMap.Decoration.SLATE_RUBBLE,
            RuinedOutpostMap.Decoration.CART_MUD,RuinedOutpostMap.Decoration.WAYPOST,
            RuinedOutpostMap.Decoration.BEDROLL,RuinedOutpostMap.Decoration.LEAN_TO,
            RuinedOutpostMap.Decoration.MEDICAL_CHEST,RuinedOutpostMap.Decoration.BANDAGE,
            RuinedOutpostMap.Decoration.STRETCHER,RuinedOutpostMap.Decoration.GATE_PIER,
            RuinedOutpostMap.Decoration.WATCH_PLATFORM,RuinedOutpostMap.Decoration.TOWER_FOOTING,
            RuinedOutpostMap.Decoration.SIGNAL_MAST,RuinedOutpostMap.Decoration.WEAPON_RACK,
            RuinedOutpostMap.Decoration.REEDS,RuinedOutpostMap.Decoration.SACK,
            RuinedOutpostMap.Decoration.WHEEL,RuinedOutpostMap.Decoration.CANVAS,
            RuinedOutpostMap.Decoration.WALL_END);

    public static void main(String[] args) {
        for(int room=0;room<RuinedOutpostMap.ROOMS.size();room++) {
            var map=new RuinedOutpostMap(room);
            List<RuinedOutpostMap.Obstacle> solids=new ArrayList<>(map.barriers());
            for(var item:map.dressing())if(item.obstacle()!=null)solids.add(item.obstacle());
            boolean[][] reach=floodFill(map);
            for(var door:map.doors())assert reached(reach,door.x(),door.y(),120) : "unreachable exit in room "+room;
            var slots=room==9?SUMMONS:java.util.Arrays.copyOf(SPAWNS,map.description().enemies());
            for(double[] slot:slots) {
                double sx=map.authored(slot[0]),sy=map.authored(slot[1]);
                assert !map.isBlocked(sx,sy+Wisp.COLLISION_Y_OFFSET,Wisp.COLLISION_RADIUS) : "blocked enemy slot in room "+room;
                // Broken Palisade is the approved reference scene; its fence line sits nearer the road than 90px.
                if(room==1)continue;
                for(var solid:solids)assert distance(sx,sy,solid)>=90
                        : "solid within 90px of an enemy slot in room "+room+" at "+sx+","+sy;
            }
            for(var item:map.dressing()) {
                assert Math.hypot(item.x()-map.spawnX(),item.y()-map.spawnY())>=180 : "dressing crowds spawn in room "+room;
                for(var door:map.doors())assert Math.hypot(item.x()-door.x(),item.y()-door.y())>160
                        : "dressing inside a door mouth in room "+room;
            }
            for(var barrier:map.barriers())for(var door:map.doors())
                assert distance(door.x(),door.y(),barrier)>=160 : "barrier inside a door mouth in room "+room;
            if(room==7||room==10) {
                assert !map.isBlocked(map.restX(),map.restY()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS)
                        : "rest point stands inside a solid in room "+room;
                assert reached(reach,map.restX(),map.restY(),100) : "rest point unreachable in room "+room;
            }
            if(room==9) {
                assert map.dressing().stream().noneMatch(d->d.decoration()==RuinedOutpostMap.Decoration.BRAZIER)
                        : "gatehouse must not burn decorative danger-coloured fire";
                for(var solid:solids)assert distance(map.guardianX(),map.guardianY(),solid)>=260
                        : "Warden's opening floor must stay free of solids";
            }
            boolean landmark=map.dressing().stream().anyMatch(d->LANDMARKS.contains(d.decoration()))
                    ||map.barriers().stream().anyMatch(b->b.prop()==RuinedOutpostMap.Prop.WEAPON_RACK);
            assert landmark||room==1 : "room "+room+" needs at least one purposeful landmark piece";
        }
        System.out.println("OutpostLayoutTest passed");
    }
    private static double distance(double x,double y,RuinedOutpostMap.Obstacle o) {
        double dx=x-Math.max(o.centerX()-o.width()/2,Math.min(o.centerX()+o.width()/2,x));
        double dy=y-Math.max(o.centerY()-o.height()/2,Math.min(o.centerY()+o.height()/2,y));
        return Math.hypot(dx,dy);
    }
    private static boolean[][] floodFill(RuinedOutpostMap map) {
        int width=map.worldWidth()/16,height=map.worldHeight()/16;
        boolean[][] seen=new boolean[height+1][width+1];
        ArrayDeque<int[]> queue=new ArrayDeque<>();
        queue.add(new int[]{width/2,height/2});seen[height/2][width/2]=true;
        while(!queue.isEmpty()) {
            int[] cell=queue.remove();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int x=cell[0]+d[0],y=cell[1]+d[1];
                if(x<0||x>width||y<0||y>height||seen[y][x]
                        ||map.isBlocked(x*16,y*16+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS)) continue;
                seen[y][x]=true;queue.add(new int[]{x,y});
            }
        }
        return seen;
    }
    private static boolean reached(boolean[][] seen,double x,double y,double within) {
        for(int cy=0;cy<seen.length;cy++)for(int cx=0;cx<seen[cy].length;cx++)
            if(seen[cy][cx]&&Math.hypot(cx*16-x,cy*16-y)<within)return true;
        return false;
    }
}
