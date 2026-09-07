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
        TREE("dead-tree",31,102,24,12);
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
    /** Rendered at 2x and depth sorted by ground anchor; only upright bases are solid. */
    public enum Decoration {
        CART("broken-cart",40,70), SHIELD_CACHE("shield-cache",32,54),
        BRAZIER("brazier",32,56), RUBBLE("rubble",32,54),
        FALLEN_ARMS("fallen-arms",32,44), SPLINTERS("splinters",32,54);
        final String file;
        final int anchorX,anchorY;
        Decoration(String file,int anchorX,int anchorY) {
            this.file=file;this.anchorX=anchorX;this.anchorY=anchorY;
        }
        String path() {return "assets/props/outpost/"+file+".png";}
        boolean ground() { return this==FALLEN_ARMS||this==SPLINTERS; }
    }
    public record Dressing(double x,double y,Decoration decoration) {
        /** World-pixel feet calibrated against the selected native cart/brazier images. */
        public Obstacle obstacle() {
            return switch(decoration) {
                case CART -> new Obstacle(x+6,y-20,80,24);
                case BRAZIER -> new Obstacle(x,y,48,16);
                default -> null;
            };
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
            if(!breachAt(x,64)) banks.add(new Obstacle(x,48,64,96+(x/64%3)*16));
            if(!breachAt(x,worldHeight()-64)) banks.add(new Obstacle(x,worldHeight()-48,64,96+(x/64%4)*16));
        }
        for(int y=32;y<worldHeight();y+=64) {
            if(!breachAt(64,y)) banks.add(new Obstacle(48,y,96+(y/64%4)*16,64));
            if(!breachAt(worldWidth()-64,y)) banks.add(new Obstacle(worldWidth()-48,y,96+(y/64%3)*16,64));
        }
        for(var bank:banks)bankShape.add(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(
                bank.centerX()-bank.width()/2,bank.centerY()-bank.height()/2,bank.width(),bank.height())));
        for(int y=0;y<heightInTiles();y++)for(int x=0;x<widthInTiles();x++)
            bankMasks[y][x]=(bankAt(x,y)?1:0)|(bankAt(x+1,y)?2:0)|(bankAt(x,y+1)?4:0)|(bankAt(x+1,y+1)?8:0);
        switch (room) {
            case 0 -> { prop(280,240,Prop.TREE); prop(920,480,Prop.TREE); }
            case 1 -> { prop(448,352,Prop.PALISADE); prop(832,352,Prop.PALISADE); }
            case 2 -> { rubble(352,256,96,48); rubble(928,512,96,48); }
            case 3 -> { prop(320,240,Prop.COT); prop(960,528,Prop.COT); }
            case 4 -> prop(640,208,Prop.STANDARD);
            case 5 -> { prop(480,240,Prop.BARREL); prop(480,292,Prop.BARREL);
                prop(800,480,Prop.BARREL); prop(800,532,Prop.BARREL); }
            case 6 -> { prop(352,240,Prop.WALL); prop(928,528,Prop.WALL); }
            case 7 -> rubble(640,240,128,64);
            case 8 -> { prop(416,240,Prop.WALL); prop(864,528,Prop.WALL); }
            case 10 -> { prop(384,256,Prop.COT); prop(896,256,Prop.COT); }
            case 11 -> { rubble(352,256,64,192); rubble(928,256,64,192); }
            default -> { }
        }
        // These silhouettes fill the margins, never the spawn, main roads or door mouths.
        switch(room) {
            case 0 -> {dress(240,176,Decoration.RUBBLE);dress(1060,640,Decoration.CART);}
            case 1 -> {dress(515,390,Decoration.SPLINTERS);dress(765,405,Decoration.SPLINTERS);
                dress(535,455,Decoration.FALLEN_ARMS);}
            case 2 -> {dress(180,520,Decoration.CART);dress(1040,180,Decoration.SHIELD_CACHE);}
            case 3 -> {dress(230,620,Decoration.SHIELD_CACHE);dress(1050,170,Decoration.BRAZIER);}
            case 4 -> {dress(320,560,Decoration.BRAZIER);dress(990,220,Decoration.SHIELD_CACHE);}
            case 5 -> {dress(240,600,Decoration.CART);dress(1020,180,Decoration.RUBBLE);}
            case 6 -> {dress(200,570,Decoration.RUBBLE);dress(1040,190,Decoration.BRAZIER);}
            case 7 -> {dress(330,560,Decoration.BRAZIER);dress(990,200,Decoration.SHIELD_CACHE);}
            case 8 -> {dress(240,610,Decoration.SHIELD_CACHE);dress(1060,200,Decoration.CART);}
            case 9 -> {
                dress(250,220,Decoration.BRAZIER);dress(1060,220,Decoration.BRAZIER);
                dress(350,600,Decoration.SHIELD_CACHE);dress(1030,610,Decoration.RUBBLE);
            }
            case 10 -> {dress(220,560,Decoration.CART);dress(1040,570,Decoration.SHIELD_CACHE);}
            case 11 -> {dress(460,230,Decoration.RUBBLE);dress(820,230,Decoration.BRAZIER);}
            default -> { }
        }
    }
    private void dress(double x,double y,Decoration decoration) {
        var item=new Dressing(authored(x),authored(y),decoration);
        dressing.add(item);
        var feet=item.obstacle();
        if(feet!=null)dressingObstacles.add(feet);
    }
    private void rubble(double x, double y, double width, double height) {
        barriers.add(new Obstacle(authored(x),authored(y),width,height));
    }
    private void prop(double x,double y,Prop prop) {
        barriers.add(new Obstacle(authored(x),authored(y),prop.footprintWidth*2,prop.footprintDepth*2,prop));
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
    public double restX() { return spawnX(); }
    public double restY() { return authored(320); }
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
    private boolean stone(int x, int y) {
        double wx=x*TILE_SIZE,wy=y*TILE_SIZE;
        if(room==1) {
            // One broad approach bends through the broken defence, not a round arena stamp.
            double centre=spawnY()+Math.sin(wx/worldWidth()*Math.PI*2)*80;
            return Math.abs(wy-centre)<112;
        }
        double dx=(wx-spawnX())/380,dy=(wy-spawnY())/260;
        boolean paving=room!=0 && dx*dx+dy*dy<1;
        for(Door door:doors) {
            if(door.x()!=spawnX()) paving|=Math.abs(wy-spawnY())<110
                    && (door.x()<spawnX()?wx<=spawnX():wx>=spawnX());
            else paving|=Math.abs(wx-spawnX())<110
                    && (door.y()<spawnY()?wy<=spawnY():wy>=spawnY());
        }
        if(room==9) paving|=wx>=spawnX()&&Math.abs(wy-spawnY())<110;
        // Broken paving opens into earth, not an uninterrupted tiled rectangle.
        boolean crater=(x-(8+room%4*3))*(x-(8+room%4*3))+(y-12)*(y-12)<4;
        return paving&&!crater;
    }
}
