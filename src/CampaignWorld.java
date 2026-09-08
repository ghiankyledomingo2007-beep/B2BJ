import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/** Four hand-authored, continuous biomes. Local names and routes are original campaign material. */
public final class CampaignWorld {
    public static final int AREA_COUNT=4;
    public record Point(double x,double y) { }
    public record Landmark(String name,double x,double y,String lore,boolean optional) { }
    public record EnemySpawn(double x,double y,CampaignEnemy.Kind kind) { }
    public record Area(int id,String name,int width,int height,Point spawn,Point hub,Point boss,
                       Point exit,Point returnPortal,List<Landmark> landmarks,
                       List<EnemySpawn> enemies,List<RuinedOutpostMap.Obstacle> obstacles) {
        public Area {
            landmarks=List.copyOf(landmarks);enemies=List.copyOf(enemies);obstacles=List.copyOf(obstacles);
        }
    }

    // The centre circuit has alternate approaches; optional spurs end at distinct discoveries.
    private static final int[][] ROUTES={{0,1},{1,2},{2,3},{3,4},{4,5},{5,6},{6,3},
            {6,7},{7,8},{2,6},{1,9},{4,10},{7,11}};
    private static final String[] NAMES={"RUINED OUTPOST","CORRUPTED FOREST","DEMON CATACOMBS","RIFT CITADEL"};
    private static final int[][][] LOCATIONS={
            {{512,3072},{1216,2816},{2368,3008},{2944,2176},{2496,1216},{4032,1216},
                    {4160,2688},{5120,2752},{5376,1664},{1088,960},{3264,640},{5376,3520}},
            {{576,3072},{1280,2624},{2112,3264},{2816,2368},{2176,1280},{3840,768},
                    {4224,2432},{5120,3008},{5376,1536},{896,960},{2816,512},{5056,3648}},
            {{576,3200},{1280,2880},{2240,3200},{2944,2240},{2304,1216},{3904,1024},
                    {4096,2560},{5184,2880},{5376,1408},{832,1216},{3072,576},{4992,3648}},
            {{512,3072},{1280,2688},{2304,3200},{2944,2240},{2240,1216},{3904,832},
                    {4160,2304},{4992,2944},{5312,1408},{960,960},{2880,512},{5440,3584}}};
    private static final String[][] LANDMARK_NAMES={
            {"RAIN DITCH","CAPTAIN CAMP","MUSTER GROUND","BROKEN PALISADE","ABANDONED BARRACKS",
                    "NORTH WATCH","STANDARD YARD","INNER COURT","EAST GATEHOUSE",
                    "SIGNAL TOWER","FIELD INFIRMARY","SUPPLY RESERVE"},
            {"ASHEN TRAIL","FERRY REFUGE","IMP CLEARING","ROOT CROSSING","HOLLOW GROVE",
                    "WHITE CLOTH RIDGE","BLACKWATER FORD","THORN APPROACH","BRIARHEART ROOTS",
                    "WAYFINDER'S REST","MOONWELL","ABANDONED ORCHARD"},
            {"DESCENT STAIR","KEEPER'S REFUGE","SILENT PROCESSION","OSSUARY CROSSROADS","KNIGHT'S VIGIL",
                    "CRYPT GALLERY","DROWNED VAULT","BELL APPROACH","OATHKEEPER'S VAULT",
                    "SEALED ARCHIVE","NAMELESS TOMB","RELIQUARY"},
            {"BREACHED RAMPART","MAINTENANCE RECESS","SHATTERED COURT","WARD CROSSING","SPLIT BASTION",
                    "CROWN WALK","ICHOR CONDUIT","LAST STAIR","RIFT HEART",
                    "OUTER WARD RECORDS","WATCHKEEPER'S GARDEN","BROKEN OBSERVATORY"}};
    private static final String[][] LORE={
            {"Rainoray. Your last shift is over.","Mara keeps a lamp lit for anyone returning.",
                    "The expedition gathered here. Its banners never returned.","A broad breach cuts through the abandoned defence.",
                    "Beds remain made for soldiers still listed as missing.","The watch faced the forest until its final night.",
                    "The fallen standard still carries a trace of Ichor.","Two roads meet before the Warden's post.",
                    "The Outpost Warden holds a gate no captain can relieve.",
                    "The east road stays open until the last stretcher passes.","One supply chest survived the evacuation.",
                    "Spare weapons wait for an army that will never collect them."},
            {"Gold light moves between the roots.","Sedge marks the way back with white cloth.",
                    "Rift Imps gather where the canopy opens.","Old roots divide the road into two ways forward.",
                    "The trees drank what the knights spilled.","The high road overlooks the ruined watercourse.",
                    "The ford has gone dry, but its stones still lead east.","Thorns grip the stair beneath the clearing.",
                    "Briarheart has rooted itself across the descent.",
                    "If the flags go dark, follow the white cloth. It was never a surrender.",
                    "A quiet spring reflects a sky the forest can no longer see.","The last harvest lies untouched beneath black leaves."},
            {"An old bell carries up through the stone.","Ilyra records names where others counted losses.",
                    "A lone knight still guards the first funeral passage.","The old burial routes join beneath the kingdom.",
                    "Shielded knights wait for relief that never came.","Empty niches outnumber the dead brought home.",
                    "Water marks show how high the dark once rose.","The bell grows louder at every turn.",
                    "The Oathkeeper still rings the order to hold.",
                    "Relief approved. Courier unavailable. Hold this order until the roads are secured.",
                    "Someone left a place for a soldier with no recovered name.","The sealed stores still shelter a little borrowed power."},
            {"The citadel shines with the kingdom's stolen light.","Orin keeps a tired watch over the failing seal.",
                    "The expedition's last camp lies beneath the broken wards.","Every conduit bends toward the tear.",
                    "Two surviving roads circle the ruined bastion.","The crown's walkway overlooks an empty kingdom.",
                    "Ichor flows inward against the stone's slope.","The final stair trembles beneath borrowed power.",
                    "The Ichor Golem feeds the rift it was built to contain.",
                    "Four keepers. Six hours each. The ward needs a watch, not a martyr.",
                    "A small garden grew wherever the keepers could rest.","The telescope points toward a sky hidden by the tear."}};
    private static final Area[] AREAS={create(0),create(1),create(2),create(3)};

    private CampaignWorld() { }
    public static Area area(int id) {
        if(id<0||id>=AREA_COUNT)throw new IllegalArgumentException("biome");
        return AREAS[id];
    }

    private static Area create(int id) {
        var landmarks=new ArrayList<Landmark>();
        for(int i=0;i<12;i++)landmarks.add(new Landmark(i==9?CampaignStory.memoryName(id):LANDMARK_NAMES[id][i],
                LOCATIONS[id][i][0],LOCATIONS[id][i][1],
                i==9?LANDMARK_NAMES[id][i]+": "+LORE[id][i]:LORE[id][i],i>=9));
        var obstacles=new ArrayList<RuinedOutpostMap.Obstacle>();
        if(id==2)catacombWalls(landmarks,obstacles);
        else outdoorObstacles(id,landmarks,obstacles);
        var enemies=new ArrayList<EnemySpawn>();
        CampaignEnemy.Kind[] kinds=switch(id) {
            case 0 -> new CampaignEnemy.Kind[]{CampaignEnemy.Kind.OUTPOST_SCOUT,CampaignEnemy.Kind.OUTPOST_SCOUT,
                    CampaignEnemy.Kind.OUTPOST_GUARD,CampaignEnemy.Kind.OUTPOST_SPITTER};
            case 1 -> new CampaignEnemy.Kind[]{CampaignEnemy.Kind.RIFT_IMP,CampaignEnemy.Kind.RIFT_IMP,
                    CampaignEnemy.Kind.THORN_WOLF,CampaignEnemy.Kind.MIRE_SHAMAN};
            case 2 -> new CampaignEnemy.Kind[]{CampaignEnemy.Kind.FALLEN_KNIGHT,CampaignEnemy.Kind.RIFT_IMP,
                    CampaignEnemy.Kind.MIRE_SHAMAN,CampaignEnemy.Kind.CINDER_HEXER};
            default -> new CampaignEnemy.Kind[]{CampaignEnemy.Kind.RIFT_IMP,CampaignEnemy.Kind.THORN_WOLF,
                    CampaignEnemy.Kind.FALLEN_KNIGHT,CampaignEnemy.Kind.MIRE_SHAMAN,CampaignEnemy.Kind.CINDER_HEXER};
        };
        int[][] offsets={{-96,64},{112,64},{0,-112},{128,-96},{-128,-112}};
        for(int location=2;location<8;location++) {
            int count=id==0?3:id==3?5:4;
            if(id==2&&location==2)count=1; // Introduce the shielded knight alone before mixed encounters.
            for(int i=0;i<count;i++) {
                var p=landmarks.get(location);
                int kindIndex=(i+(id==0?location-2:0))%kinds.length;
                enemies.add(new EnemySpawn(p.x()+offsets[i][0],p.y()+offsets[i][1],kinds[kindIndex]));
            }
        }
        for(int location=9;location<12;location++) {
            var p=landmarks.get(location);
            enemies.add(new EnemySpawn(p.x()+112,p.y()+96,kinds[(location-9)%kinds.length]));
        }
        Point spawn=point(landmarks.get(0)),hub=point(landmarks.get(1)),boss=point(landmarks.get(8));
        return new Area(id,NAMES[id],6144,4096,spawn,hub,boss,new Point(boss.x()+384,boss.y()-224),
                new Point(spawn.x()-160,spawn.y()),landmarks,enemies,obstacles);
    }

    private static Point point(Landmark landmark) { return new Point(landmark.x(),landmark.y()); }

    private static void outdoorObstacles(int id,List<Landmark> landmarks,List<RuinedOutpostMap.Obstacle> obstacles) {
        int[][] clusters={{512,640},{1792,640},{4480,512},{5504,576},{1664,1664},{3392,1472},
                {768,2048},{1856,2240},{3520,3072},{4480,3456},{2560,3776},{5760,2432},
                {448,3776},{1472,3648},{3456,2624},{4736,1792},{5760,3904}};
        for(int[] cluster:clusters)for(int i=0;i<6;i++) {
            double x=cluster[0]+(i%3-1)*160,y=cluster[1]+(i/3-.5)*176;
            RuinedOutpostMap.Prop prop=id==1?RuinedOutpostMap.Prop.TREE:id==0
                    ?(i%3==0?RuinedOutpostMap.Prop.WALL:i%3==1?RuinedOutpostMap.Prop.PALISADE:RuinedOutpostMap.Prop.BARREL):null;
            double width=prop!=null?prop.footprintWidth*2:224,height=prop!=null?prop.footprintDepth*2:128;
            if(!nearRoutes(x,y,landmarks,208)&&clearOfLandmarks(x,y,landmarks,400))
                obstacles.add(new RuinedOutpostMap.Obstacle(x,y,width,height,prop));
        }
        // Broad broken ramparts shape the citadel's courts without sealing their approaches.
        if(id==3)for(int[] wall:new int[][]{{1664,960,128,768},{3392,1728,640,128},
                {3520,3520,896,128},{4608,576,128,640},{576,1920,512,128}})
            obstacles.add(new RuinedOutpostMap.Obstacle(wall[0],wall[1],wall[2],wall[3]));
    }

    private static void catacombWalls(List<Landmark> landmarks,List<RuinedOutpostMap.Obstacle> obstacles) {
        // Authored routes carve connected chambers; merge each row's solid cells into one wall span.
        for(int y=192;y<4096-128;y+=128) {
            int start=-1;
            for(int x=192;x<6144;x+=128) {
                boolean wall=x<6144-128 && !nearRoutes(x,y,landmarks,224)
                        && clearOfLandmarks(x,y,landmarks,400)
                        && Math.hypot(x-landmarks.get(8).x(),y-landmarks.get(8).y())>752;
                if(wall&&start<0)start=x;
                if(!wall&&start>=0) {
                    obstacles.add(new RuinedOutpostMap.Obstacle((start+x-128)/2.0,y,x-start,128));
                    start=-1;
                }
            }
        }
    }

    private static boolean clearOfLandmarks(double x,double y,List<Landmark> landmarks,double radius) {
        for(var p:landmarks)if(Math.hypot(x-p.x(),y-p.y())<radius)return false;
        return true;
    }

    private static boolean nearRoutes(double x,double y,List<Landmark> landmarks,double radius) {
        for(int[] route:ROUTES) {
            var a=landmarks.get(route[0]);var b=landmarks.get(route[1]);
            if(Line2D.ptSegDist(a.x(),a.y(),b.x(),b.y(),x,y)<radius)return true;
        }
        return false;
    }

    static boolean paved(Area area,double x,double y) {
        if(nearRoutes(x,y,area.landmarks(),area.id()==2?148:112))return true;
        for(var p:area.landmarks()) {
            double radius=p.optional()?144:area.id()==2?240:192;
            if(Math.hypot(x-p.x(),y-p.y())<radius)return true;
        }
        return Line2D.ptSegDist(area.boss().x(),area.boss().y(),area.exit().x(),area.exit().y(),x,y)<112;
    }
}
