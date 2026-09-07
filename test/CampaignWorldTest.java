import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class CampaignWorldTest {
    public static void main(String[] args) {
        String[] names={"RUINED OUTPOST","CORRUPTED FOREST","DEMON CATACOMBS","RIFT CITADEL"};
        assert CampaignWorld.AREA_COUNT==4 : "all four GCD biomes required";
        for(int biome=0;biome<4;biome++) {
            var area=CampaignWorld.area(biome);
            var map=RuinedOutpostMap.campaign(biome);
            assert area.id()==biome && area.name().equals(names[biome]);
            assert map.campaign() && map.biome()==biome;
            assert area.width()==6144 && area.height()==4096 : "biomes must be large contiguous areas";
            assert map.worldWidth()==area.width() && map.worldHeight()==area.height();
            assert area.landmarks().size()==12;
            assert area.landmarks().stream().filter(CampaignWorld.Landmark::optional).count()==3;
            assert area.enemies().size()>=18 : "authored encounters must span each biome";
            assert map.barriers().size()>=12 : "large areas need terrain structure";
            assert map.doors().isEmpty() : "no room-by-room arena chain";
            assert !map.isBlocked(area.boss().x(),area.boss().y()+32,24) : "boss collision begins on activation";
            map.setGuardianPresent(true);
            assert map.isBlocked(area.boss().x(),area.boss().y()+32,24);
            map.clearGuardian();
            map.setPassagesLocked(true);
            map.setTutorialLocked(true);
            assert !map.exitReached(area.exit().x(),area.exit().y());
            map.openGate();
            assert map.exitReached(area.exit().x(),area.exit().y());
            assertReachable(area,map);
            for(var spawn:area.enemies()) {
                assert !map.waterBlocked(spawn.x(),spawn.y()+24,24) : "blocked enemy placement in "+area.name();
                assert Math.hypot(spawn.x()-area.hub().x(),spawn.y()-area.hub().y())>320 : "hub must be safe";
            }
            boolean earth=false,paving=false;
            for(int y=0;y<map.heightInTiles();y++)for(int x=0;x<map.widthInTiles();x++) {
                earth|=map.tileMask(x,y)==0;
                paving|=map.tileMask(x,y)==15;
                map.bankMask(x,y);
            }
            assert earth&&paving : "biome terrain must have readable roads and margins";
        }
        assert !new RuinedOutpostMap().campaign() : "legacy map constructor remains compatible";
        boolean rejected=false;
        try { CampaignWorld.area(4); } catch(IllegalArgumentException expected) { rejected=true; }
        assert rejected : "invalid biome rejected";
        System.out.println("CampaignWorldTest passed");
    }

    private static void assertReachable(CampaignWorld.Area area,RuinedOutpostMap map) {
        int step=32,width=map.worldWidth()/step,height=map.worldHeight()/step;
        var queue=new ArrayDeque<int[]>();
        var seen=new HashSet<Integer>();
        int sx=(int)Math.round(area.spawn().x()/step),sy=(int)Math.round(area.spawn().y()/step);
        queue.add(new int[]{sx,sy});seen.add(sy*width+sx);
        while(!queue.isEmpty()) {
            var cell=queue.remove();
            for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int x=cell[0]+d[0],y=cell[1]+d[1],key=y*width+x;
                if(x<0||y<0||x>=width||y>=height||seen.contains(key)||map.waterBlocked(x*step,y*step+24,24))continue;
                seen.add(key);queue.add(new int[]{x,y});
            }
        }
        for(var landmark:area.landmarks())
            assert nearReachable(seen,width,landmark.x(),landmark.y(),step) : "unreachable "+area.name()+" / "+landmark.name();
        for(var point:new CampaignWorld.Point[]{area.hub(),area.boss(),area.exit(),area.returnPortal()})
            assert nearReachable(seen,width,point.x(),point.y(),step) : "unreachable campaign interaction in "+area.name();
    }

    private static boolean nearReachable(Set<Integer> seen,int width,double px,double py,int step) {
        int x=(int)Math.round(px/step),y=(int)Math.round(py/step);
        for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)if(seen.contains((y+dy)*width+x+dx))return true;
        return false;
    }
}
