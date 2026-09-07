import java.util.ArrayList;
import java.util.List;

/** Authored military outpost. Room names/layout are proposals, not GCD quotes. */
public final class RuinedOutpostMap {
    /** Native-pixel ground anchors and footprints; all props render at exactly 2x. */
    public enum Prop {
        PALISADE("barricade",48,56,76,12),
        COT("cot",56,52,80,22),
        STANDARD("standard",22,90,20,12),
        WALL("wall",48,47,82,12),
        BARREL("supply-barrel",24,53,32,16),
        TREE("dead-tree",31,102,24,12),
        /** Muster-ground cover. Its 96x48 world footprint is load-bearing: many combat tests read room 2's first barrier. */
        WEAPON_RACK("weapon-rack",32,56,48,24);
        final String file;
        final int anchorX,anchorY,footprintWidth,footprintDepth;
        Prop(String file,int anchorX,int anchorY,int footprintWidth,int footprintDepth) {
            this.file=file;this.anchorX=anchorX;this.anchorY=anchorY;
            this.footprintWidth=footprintWidth;this.footprintDepth=footprintDepth;
        }
        String path() { return "assets/props/outpost/"+file+".png"; }
    }
    public record Obstacle(double centerX, double centerY, double width, double height, Prop prop) {
        public Obstacle(double centerX,double centerY,double width,double height) {
            this(centerX,centerY,width,height,null);
        }
    }
    public record Room(String name, int gridX, int gridY, int enemies, String lore) { }
    public record Door(int destination, double x, double y) { }
    /** Rendered at 2x and depth sorted by ground anchor; only upright bases are solid (feet 0 = walkable). */
    public enum Decoration {
        CART("broken-cart",40,70,false,80,24), SHIELD_CACHE("shield-cache",32,54,false,0,0),
        BRAZIER("brazier",32,56,false,48,16), RUBBLE("rubble",32,54,false,0,0),
        FALLEN_ARMS("fallen-arms",32,44,true,0,0), SPLINTERS("splinters",32,54,true,0,0),
        BRAZIER_UNLIT("brazier-unlit",32,56,false,48,16),
        SLATE_RUBBLE("rubble-slate",32,54,false,56,18),
        CART_MUD("cart-mud",40,70,false,80,24),
        WAYPOST("waypost",24,76,false,20,12),
        BEDROLL("bedroll",32,40,true,0,0),
        LEAN_TO("lean-to",48,72,false,0,0),
        MEDICAL_CHEST("medical-chest",24,40,false,40,16),
        BANDAGE("bandage",16,28,true,0,0),
        STRETCHER("stretcher",48,36,true,0,0),
        GATE_PIER("gate-pier",32,104,false,48,24),
        WATCH_PLATFORM("watch-platform",64,84,false,100,24),
        TOWER_FOOTING("tower-footing",64,112,false,110,40),
        SIGNAL_MAST("signal-mast",48,52,true,0,0),
        WEAPON_RACK("weapon-rack",32,56,false,52,14),
        REEDS("reeds",32,40,true,0,0),
        SACK("sack",24,26,true,0,0),
        WHEEL("wheel",16,28,true,0,0),
        CANVAS("canvas-debris",32,40,true,0,0),
        WALL_END("wall-end",24,47,false,40,24);
        final String file;
        final int anchorX,anchorY,feetWidth,feetDepth;
        private final boolean ground;
        Decoration(String file,int anchorX,int anchorY,boolean ground,int feetWidth,int feetDepth) {
            this.file=file;this.anchorX=anchorX;this.anchorY=anchorY;
            this.ground=ground;this.feetWidth=feetWidth;this.feetDepth=feetDepth;
        }
        String path() {return "assets/props/outpost/"+file+".png";}
        boolean ground() { return ground; }
    }
    public record Dressing(double x,double y,Decoration decoration) {
        /** World-pixel feet calibrated against the native images; carts stand on a raised bed above the anchor. */
        public Obstacle obstacle() {
            if(decoration.feetWidth==0)return null;
            boolean cart=decoration==Decoration.CART||decoration==Decoration.CART_MUD;
            return new Obstacle(x+(cart?6:0),y-(cart?20:0),decoration.feetWidth,decoration.feetDepth);
        }
    }
    public static final int TILE_SIZE = 64;
    public static final double GUARDIAN_COLLISION_Y_OFFSET = 32;
    public static final List<Room> ROOMS = List.of(
            new Room("RAIN DITCH", 0, 2, 0, "RAINORAY. THAT WAS YOUR NAME. BEFORE THE LAST SHIFT."),
            new Room("BROKEN PALISADE", 1, 2, 2, "THE KNIGHTS HELD THIS BREACH. NONE RETURNED."),
            new Room("MUSTER GROUND", 2, 2, 3, "GOLD IN THE MUD. WALK OVER FALLEN ICHOR TO COLLECT IT."),
            new Room("ABANDONED BARRACKS", 3, 2, 5, "EMPTY BUNKS. TEN NAMES SCRATCHED INTO A SHIELD."),
            new Room("STANDARD YARD", 3, 1, 0, "THE FALLEN LEFT YOU THEIR ICHOR. PRESS Q AT 100."),
            new Room("SUPPLY LANE", 2, 1, 3, "POWER RUNS OUT. CHOOSE WHEN THE BLADE IS WORTH IT."),
            new Room("NORTH WATCH", 1, 1, 4, "THE WATCH FACED INWARD. WHAT WERE THEY KEEPING IN?"),
            new Room("CAPTAIN CAMP", 0, 1, 0, "A DRY BEDROLL. REST HERE. YOU WILL REFORM AT THIS CAMP."),
            new Room("INNER COURT", 0, 0, 4, "THE LAST DEFENCE STANDS BETWEEN YOU AND THE FOREST."),
            new Room("EAST GATEHOUSE", 1, 0, 0, "BAIT THE MARK. DODGE. STRIKE DURING RECOVERY."),
            new Room("FIELD INFIRMARY", 2, 3, 0, "NO MEDIC CAME. ONE SEALED DRESSING REMAINS."),
            new Room("SIGNAL TOWER", 1, -1, 0, "NO ANSWER FROM THE FOREST. THE LAST SIGNAL NEVER SENT."));
    private static final int[][] LINKS = {{0,1},{1,2},{2,3},{3,4},{4,5},{5,6},
            {6,7},{7,8},{8,9},{2,10},{9,11}};
    private final int room;
    private final List<Obstacle> barriers = new ArrayList<>();
    private final List<Obstacle> banks = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<Dressing> dressing = new ArrayList<>();
    private final List<Obstacle> dressingObstacles = new ArrayList<>();
    private final int[][] bankMasks=new int[18][30];
    private final java.awt.geom.Area bankShape=new java.awt.geom.Area();
    private boolean gateOpen;
    private boolean guardianPresent;
    private double liveGuardianX,liveGuardianY;
    private boolean passagesLocked;
    private boolean tutorialLocked;

    public RuinedOutpostMap() { this(0); }
    public RuinedOutpostMap(int room) {
        if (room < 0 || room >= ROOMS.size()) throw new IllegalArgumentException("room");
        this.room = room;
        guardianPresent = room == 9;
        liveGuardianX=guardianX();liveGuardianY=guardianY();
        for (int[] link : LINKS) {
            int other = link[0] == room ? link[1] : link[1] == room ? link[0] : -1;
            if (other < 0) continue;
            Room a = ROOMS.get(room), b = ROOMS.get(other);
            doors.add(new Door(other, b.gridX() > a.gridX() ? worldWidth()-64 : b.gridX() < a.gridX() ? 64 : spawnX(),
                    b.gridY() > a.gridY() ? worldHeight()-64 : b.gridY() < a.gridY() ? 64 : spawnY()));
        }
        // Uneven earth banks border the compound; roads cut broad, physical breaches.
        for(int x=32;x<worldWidth();x+=64) {
            if(!breachAt(x,64)) banks.add(new Obstacle(x,48,64,bankDepth(0,x/64)));
            if(!breachAt(x,worldHeight()-64)) banks.add(new Obstacle(x,worldHeight()-48,64,bankDepth(1,x/64)));
        }
        for(int y=32;y<worldHeight();y+=64) {
            if(!breachAt(64,y)) banks.add(new Obstacle(48,y,bankDepth(2,y/64),64));
            if(!breachAt(worldWidth()-64,y)) banks.add(new Obstacle(worldWidth()-48,y,bankDepth(3,y/64),64));
        }
        for(var bank:banks)bankShape.add(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(
                bank.centerX()-bank.width()/2,bank.centerY()-bank.height()/2,bank.width(),bank.height())));
        for(int y=0;y<heightInTiles();y++)for(int x=0;x<widthInTiles();x++)
            bankMasks[y][x]=(bankAt(x,y)?1:0)|(bankAt(x+1,y)?2:0)|(bankAt(x,y+1)?4:0)|(bankAt(x+1,y+1)?8:0);
        // Authored coordinates (x1.5). Solids stay 90px off every enemy slot; dressing stays 180px off spawn and doors.
        switch (room) {
            case 0 -> { // Rain Ditch: a drainage line of reeds and one wheel where the last cart left the road.
                prop(280,240,Prop.TREE); prop(920,480,Prop.TREE);
                dress(170,560,Decoration.REEDS);dress(250,500,Decoration.REEDS);
                dress(340,440,Decoration.REEDS);dress(470,340,Decoration.REEDS);
                dress(1010,455,Decoration.WHEEL);
            }
            case 1 -> { // Broken Palisade: reference scene, unchanged.
                prop(448,352,Prop.PALISADE); prop(832,352,Prop.PALISADE);
                dress(515,390,Decoration.SPLINTERS);dress(765,405,Decoration.SPLINTERS);
                dress(535,455,Decoration.FALLEN_ARMS);
            }
            case 2 -> { // Muster Ground: rack at the margin, dropped arms beside it, a waypost at the infirmary fork.
                prop(300,200,Prop.WEAPON_RACK);
                dress(360,250,Decoration.FALLEN_ARMS);dress(720,500,Decoration.WAYPOST);
            }
            case 3 -> { // Barracks: roofless foundation in the south-east corner, bunks inside the broken L.
                prop(1000,640,Prop.COT); prop(1132,640,Prop.COT); prop(980,566,Prop.WALL);
                dress(914,566,Decoration.WALL_END);dress(1054,566,Decoration.WALL_END);
                dress(914,606,Decoration.WALL_END);dress(914,646,Decoration.WALL_END);
                dress(1094,586,Decoration.WEAPON_RACK);dress(1140,520,Decoration.CANVAS);
                dress(820,470,Decoration.FALLEN_ARMS);dress(866,552,Decoration.BRAZIER_UNLIT);
            }
            case 4 -> { // Standard Yard: the standard, arms dropped facing it, a torn banner piece.
                prop(640,208,Prop.STANDARD);
                dress(566,262,Decoration.FALLEN_ARMS);dress(694,222,Decoration.CANVAS);
            }
            case 5 -> { // Supply Lane: staggered stock beside a stuck cart; last barrel is the group's north end.
                prop(668,538,Prop.BARREL); prop(734,502,Prop.BARREL);
                prop(806,526,Prop.BARREL); prop(726,580,Prop.BARREL);
                dress(886,574,Decoration.CART_MUD);dress(534,462,Decoration.SACK);dress(934,314,Decoration.WHEEL);
            }
            case 6 -> { // North Watch: collapsed platform at the boundary behind a parapet line with one breach.
                prop(374,200,Prop.WALL); prop(826,200,Prop.WALL);
                dress(900,200,Decoration.WALL_END);dress(640,166,Decoration.WATCH_PLATFORM);
                dress(746,134,Decoration.BRAZIER_UNLIT);
            }
            case 7 -> { // Captain Camp: lean-to against a bare tree, bedroll on the rest point, cold brazier.
                prop(374,254,Prop.TREE);
                dress(440,296,Decoration.LEAN_TO);dress(440,296,Decoration.BEDROLL);
                dress(520,240,Decoration.BRAZIER_UNLIT);
                dress(800,506,Decoration.FALLEN_ARMS);dress(842,532,Decoration.FALLEN_ARMS);
            }
            case 8 -> { // Inner Court: fallback wall line toward the gatehouse; arms and splinters face east.
                prop(1000,240,Prop.WALL);
                dress(1074,240,Decoration.WALL_END);
                dress(1000,506,Decoration.FALLEN_ARMS);dress(1046,478,Decoration.SPLINTERS);
            }
            case 9 -> { // East Gatehouse: ruined piers flank the gate outside its breach; the fight floor stays open.
                dress(1174,240,Decoration.GATE_PIER);dress(1174,528,Decoration.GATE_PIER);
                dress(200,600,Decoration.SLATE_RUBBLE);dress(1040,506,Decoration.FALLEN_ARMS);
            }
            case 10 -> { // Field Infirmary: cots on the shelter line, chest beside the dressing on the rest point.
                prop(280,296,Prop.COT); prop(600,296,Prop.COT);
                dress(440,296,Decoration.LEAN_TO);dress(440,296,Decoration.BANDAGE);
                dress(480,340,Decoration.MEDICAL_CHEST);dress(814,480,Decoration.STRETCHER);
            }
            case 11 -> { // Signal Tower: collapsed footing, fallen mast, dead beacon basket, slate spill.
                dress(640,200,Decoration.TOWER_FOOTING);dress(786,266,Decoration.SIGNAL_MAST);
                dress(534,220,Decoration.BRAZIER_UNLIT);dress(746,160,Decoration.SLATE_RUBBLE);
            }
            default -> { }
        }
    }
    private void dress(double x,double y,Decoration decoration) {
        var item=new Dressing(authored(x),authored(y),decoration);
        dressing.add(item);
        var feet=item.obstacle();
        if(feet!=null)dressingObstacles.add(feet);
    }
    private void prop(double x,double y,Prop prop) {
        barriers.add(new Obstacle(authored(x),authored(y),prop.footprintWidth*2,prop.footprintDepth*2,prop));
    }
    /** 80-144px in 8px steps from a deterministic hash, so the perimeter reads as uneven earth, not panels. */
    private int bankDepth(int side,int segment) {
        int hash=room*0x9E3779B1+side*0x85EBCA6B+segment*0xC2B2AE35;
        hash^=hash>>>15;hash*=0x2C1B3C6D;hash^=hash>>>12;
        return 80+8*Math.floorMod(hash,9);
    }
    public double authored(double coordinate) { return coordinate*1.5; }
    private boolean breachAt(double x,double y) {
        for(Door door:doors) if(Math.hypot(x-door.x(),y-door.y())<160) return true;
        return room==9 && Math.hypot(x-gateX(),y-gateY())<160;
    }
    public int room() { return room; }
    public Room description() { return ROOMS.get(room); }
    public List<Door> doors() { return List.copyOf(doors); }
    public List<Dressing> dressing() {return List.copyOf(dressing);}
    public int widthInTiles() { return 30; }
    public int heightInTiles() { return 18; }
    public int worldWidth() { return widthInTiles()*TILE_SIZE; }
    public int worldHeight() { return heightInTiles()*TILE_SIZE; }
    public double spawnX() { return worldWidth()/2.0; }
    public double spawnY() { return worldHeight()/2.0; }
    public double guardianX() { return authored(860); }
    public double guardianY() { return authored(352); }
    /** Shelter point in Camp and Infirmary; the bedroll/bandage dressing is centred here. */
    public double restX() { return authored(440); }
    public double restY() { return authored(296); }
    public double gateX() { return worldWidth()-64; }
    public double gateY() { return spawnY(); }
    public List<Obstacle> barriers() { return List.copyOf(barriers); }
    public List<Obstacle> banks() { return List.copyOf(banks); }
    int bankMask(int x,int y) { return bankMasks[y][x]; }
    java.awt.Shape bankShape() { return new java.awt.geom.Area(bankShape); }
    private boolean bankAt(int x,int y) {
        for(var bank:banks)if(intersects(x*TILE_SIZE,y*TILE_SIZE,1,bank))return true;
        return false;
    }
    public void setPassagesLocked(boolean locked) { passagesLocked=locked; }
    public void setTutorialLocked(boolean locked) { tutorialLocked=locked; }
    public boolean passageOpen(Door door) {
        return !passagesLocked && !(tutorialLocked && door.destination()==5);
    }
    public Obstacle passageBarrier(Door door) {
        boolean vertical=door.x()!=spawnX();
        return new Obstacle(door.x(),door.y(),vertical?48:320,vertical?320:48);
    }
    public void openGate() { gateOpen = true; }
    public boolean gateOpen() { return gateOpen; }
    public void clearGuardian() { guardianPresent = false; }
    public void setGuardianPosition(double x,double y) {
        if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("guardian position");
        liveGuardianX=x;liveGuardianY=y;
    }
    public boolean exitReached(double x, double y) {
        return room == 9 && gateOpen && Math.hypot(x-gateX(), y-gateY()) < 100;
    }
    public boolean isBlocked(double x, double y, double radius) {
        return touchesGuardian(x,y,radius) || waterBlocked(x,y,radius);
    }
    public boolean touchesGuardian(double x,double y,double radius) {
        return guardianPresent && intersects(x,y,radius,
                new Obstacle(liveGuardianX,liveGuardianY+GUARDIAN_COLLISION_Y_OFFSET,112,52));
    }
    public boolean waterBlocked(double x,double y,double radius) {
        if(x-radius<0 || y-radius<0 || x+radius>worldWidth() || y+radius>worldHeight()) return true;
        for (Obstacle obstacle : barriers) if (intersects(x,y,radius,obstacle)) return true;
        for (Obstacle obstacle : dressingObstacles) if (intersects(x,y,radius,obstacle)) return true;
        for (Obstacle bank : banks) if (intersects(x,y,radius,bank)) return true;
        for (Door door : doors) if (!passageOpen(door) && intersects(x,y,radius,passageBarrier(door))) return true;
        if(room==9&&!gateOpen&&intersects(x,y,radius,new Obstacle(gateX(),gateY(),48,320))) return true;
        return false;
    }
    public boolean clearLine(double ax, double ay, double bx, double by) {
        int steps = Math.max(1, (int)Math.ceil(Math.hypot(bx-ax,by-ay)/8));
        for (int i=1; i<steps; i++) {
            double x=ax+(bx-ax)*i/steps, y=ay+(by-ay)*i/steps;
            for (Obstacle obstacle : barriers) if (intersects(x,y,1,obstacle)) return false;
            for (Obstacle obstacle : dressingObstacles) if (intersects(x,y,1,obstacle)) return false;
        }
        return true;
    }
    public boolean clearWaterLine(double ax,double ay,double bx,double by,double radius) {
        if(!Double.isFinite(ax)||!Double.isFinite(ay)||!Double.isFinite(bx)||!Double.isFinite(by)
                ||!Double.isFinite(radius)||radius<=0)return false;
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(bx-ax,by-ay)/8));
        for(int i=0;i<=steps;i++)if(waterBlocked(ax+(bx-ax)*i/steps,ay+(by-ay)*i/steps,radius))return false;
        return true;
    }
    private static boolean intersects(double x, double y, double radius, Obstacle o) {
        double dx=x-Math.max(o.centerX()-o.width()/2,Math.min(o.centerX()+o.width()/2,x));
        double dy=y-Math.max(o.centerY()-o.height()/2,Math.min(o.centerY()+o.height()/2,y));
        return dx*dx+dy*dy < radius*radius;
    }
    public int tileMask(int x, int y) {
        return (stone(x,y)?1:0)|(stone(x+1,y)?2:0)|(stone(x,y+1)?4:0)|(stone(x+1,y+1)?8:0);
    }
    /** Two materials only; each room gets its own authored paving shape plus a road to every door mouth. */
    private boolean stone(int x, int y) {
        double wx=x*TILE_SIZE,wy=y*TILE_SIZE;
        // Broken paving opens into earth, not an uninterrupted tiled rectangle.
        boolean crater=(x-(8+room%4*3))*(x-(8+room%4*3))+(y-12)*(y-12)<4;
        boolean paving=switch(room) {
            case 0 -> wx>=900&&Math.abs(wy-spawnY()-Math.sin((wx-900)/300)*40)<70; // one worn track from the east exit
            case 1 -> Math.abs(wy-(spawnY()+Math.sin(wx/worldWidth()*Math.PI*2)*80))<112; // bending approach road
            case 2 -> rect(wx,wy,470,350,1450,800)&&ellipse(wx,wy,960,576,580,330); // rounded parade rectangle
            case 3 -> ellipse(wx,wy,960,576,300,190)||rect(wx,wy,1280,790,1840,1060); // yard plus barracks floor
            case 4 -> ellipse(wx,wy,960,360,350,250)&&!ellipse(wx,wy,960,330,110,70); // ring around the standard
            case 5 -> rect(wx,wy,900,560,1380,960); // pull-off beside the lane
            case 6 -> ellipse(wx,wy,960,576,280,200)||rect(wx,wy,840,130,1080,320); // watch footing at the boundary
            case 7 -> ellipse(wx,wy,660,444,230,150); // pad under the shelter
            case 8 -> Math.abs(wy-spawnY())<100+Math.max(0,wx-500)*0.36; // court widens toward the gatehouse
            case 9 -> ellipse(wx,wy,960,576,640,340); // open boss floor
            case 10 -> ellipse(wx,wy,690,470,340,190); // treatment shelter pad
            case 11 -> ellipse(wx,wy,960,340,330,210); // round tower footing
            default -> false;
        };
        if(room>0)paving|=roads(wx,wy);
        return paving&&!crater;
    }
    private boolean roads(double wx,double wy) {
        for(Door door:doors) {
            if(door.x()!=spawnX()) {
                if(Math.abs(wy-spawnY())<110&&(door.x()<spawnX()?wx<=spawnX():wx>=spawnX()))return true;
            } else if(Math.abs(wx-spawnX())<110&&(door.y()<spawnY()?wy<=spawnY():wy>=spawnY()))return true;
        }
        return room==9&&wx>=spawnX()&&Math.abs(wy-spawnY())<110;
    }
    private static boolean rect(double wx,double wy,double x1,double y1,double x2,double y2) {
        return wx>=x1&&wx<=x2&&wy>=y1&&wy<=y2;
    }
    private static boolean ellipse(double wx,double wy,double cx,double cy,double rx,double ry) {
        double dx=(wx-cx)/rx,dy=(wy-cy)/ry;
        return dx*dx+dy*dy<1;
    }
}
