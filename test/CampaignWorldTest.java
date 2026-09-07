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
            var memory=area.landmarks().stream().filter(CampaignWorld.Landmark::optional).findFirst().orElseThrow();
            var narrative=new CampaignStory();narrative.discover(biome);
            assert memory.name().equals(narrative.dialogueTitle()) : "quest objective must match its map label";
            assert area.enemies().size()>=18 : "authored encounters must span each biome";
            assert map.barriers().size()>=12 : "large areas need terrain structure";
            assert map.doors().isEmpty() : "no room-by-room arena chain";
            assert !map.isBlocked(area.boss().x(),area.boss().y()+32,24) : "boss collision begins on activation";
            assert !map.waterBlocked(area.exit().x()-160,area.exit().y()+24,24) : "return arrival must be clear";
            assertBossTravel(area,map);
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
            var restored=RuinedOutpostMap.campaign(biome);
            assert restored.barriers().equals(map.barriers()) : "terrain must remain stable on return";
            for(int y=0;y<map.heightInTiles();y++)for(int x=0;x<map.widthInTiles();x++) {
                earth|=map.tileMask(x,y)==0;
                paving|=map.tileMask(x,y)==15;
                assert map.tileMask(x,y)==restored.tileMask(x,y)&&map.bankMask(x,y)==restored.bankMask(x,y);
            }
            assert earth&&paving : "biome terrain must have readable roads and margins";
        }
        assert !new RuinedOutpostMap().campaign() : "legacy map constructor remains compatible";
        boolean rejected=false;
        try { CampaignWorld.area(4); } catch(IllegalArgumentException expected) { rejected=true; }
        assert rejected : "invalid biome rejected";
        System.out.println("CampaignWorldTest passed");
    }

    private static void assertBossTravel(CampaignWorld.Area area,RuinedOutpostMap map) {
        var boss=new Guardian(area.boss().x(),area.boss().y(),Guardian.Profile.values()[area.id()]);
        boss.activate(area.boss().x()-400,area.boss().y());
        double travelled=0;
        for(int i=0;i<600;i++) {
            double x=boss.x(),y=boss.y(),angle=i/120.0;
            boss.update(.02,area.boss().x()+Math.cos(angle)*400,area.boss().y()+Math.sin(angle)*400,map);
            assert !map.waterBlocked(boss.x(),boss.y()+32,60) : "boss must remain outside terrain";
            travelled+=Math.hypot(x-boss.x(),y-boss.y());
        }
        assert travelled>100 : "boss must move through its battle ground";
        map.setGuardianPosition(area.boss().x(),area.boss().y());
    }

    private static void assertReachable(CampaignWorld.Area area,RuinedOutpostMap map) {
        int step=32,width=map.worldWidth()/step,height=map.worldHeight()/step;
        var queue=new ArrayDeque<int[]>();
        var seen=new HashSet<Integer>();
        var previous=new java.util.HashMap<Integer,Integer>();
        int sx=(int)Math.round(area.spawn().x()/step),sy=(int)Math.round(area.spawn().y()/step);
        queue.add(new int[]{sx,sy});seen.add(sy*width+sx);
        while(!queue.isEmpty()) {
            var cell=queue.remove();
            for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int x=cell[0]+d[0],y=cell[1]+d[1],key=y*width+x;
                if(x<0||y<0||x>=width||y>=height||seen.contains(key)||map.waterBlocked(x*step,y*step+24,24))continue;
                seen.add(key);previous.put(key,cell[1]*width+cell[0]);queue.add(new int[]{x,y});
            }
        }
        for(var landmark:area.landmarks())
            assert nearReachable(seen,width,landmark.x(),landmark.y(),step) : "unreachable "+area.name()+" / "+landmark.name();
        for(var point:new CampaignWorld.Point[]{area.hub(),area.boss(),area.exit(),area.returnPortal()})
            assert nearReachable(seen,width,point.x(),point.y(),step) : "unreachable campaign interaction in "+area.name();
        for(var landmark:area.landmarks())if(landmark.optional()) {
            int key=(int)Math.round(landmark.y()/step)*width+(int)Math.round(landmark.x()/step);
            var path=new java.util.ArrayList<Integer>();
            while(previous.containsKey(key)) {path.add(key);key=previous.get(key);}
            java.util.Collections.reverse(path);
            var player=new Player(area.spawn().x(),area.spawn().y());
            for(int cell:path) {
                double x=(cell%width)*step,y=(cell/width)*step;
                int dx=(int)Math.signum(x-player.x()),dy=(int)Math.signum(y-player.y());
                player.move(dx,dy,Math.hypot(x-player.x(),y-player.y())/Player.SPEED,map);
                assert Math.hypot(player.x()-x,player.y()-y)<.01 : "walking route clips terrain";
                var dash=new Player(x,y);
                assert dash.dash(dx==0?1:dx,dy);
                dash.move(0,0,Player.DASH_DURATION,map);
                assert !map.isBlocked(dash.x(),dash.y()+24,24) : "dash enters a branch wall";
            }
            assert Math.hypot(player.x()-landmark.x(),player.y()-landmark.y())<120 : "optional branch must be playable on foot";
        }
    }

    private static boolean nearReachable(Set<Integer> seen,int width,double px,double py,int step) {
        int x=(int)Math.round(px/step),y=(int)Math.round(py/step);
        for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)if(seen.contains((y+dy)*width+x+dx))return true;
        return false;
    }
}
