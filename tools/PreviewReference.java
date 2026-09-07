import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Evidence-only headless renderer for the map-art audit gates: reference scene, native-2x scale sheet,
 * gameplay-viewport captures and Gatehouse clearance. Not runtime code; sends no input to any game.
 * Usage: java -Djava.awt.headless=true -cp build:tool PreviewReference /tmp/out
 */
public final class PreviewReference {
    private static final int W=1280,H=720;
    private static final double GUARDIAN_CLEARANCE=260,SLOT_CLEARANCE=90;
    /** Mirrors the add-spawn slots in RuinedOutpostGame.updateGuardian (authored coordinates). */
    private static final int[][] SUMMON_SLOTS={{400,544},{920,544},{400,240},{920,240},{640,544},{640,240}};
    private static final Font LABEL=new Font(Font.MONOSPACED,Font.BOLD,14);
    private static Method enterRoom,drawWorld,cameraPosition;
    private static Path out;

    public static void main(String[] args) throws Exception {
        out=Path.of(args.length>0?args[0]:"/tmp/preview-reference");Files.createDirectories(out);
        GameAudio.setMuted(true);
        enterRoom=RuinedOutpostGame.class.getDeclaredMethod("enterRoom",int.class,int.class);enterRoom.setAccessible(true);
        drawWorld=B2BJ.class.getDeclaredMethod("drawWorld",Graphics2D.class,int.class,int.class);drawWorld.setAccessible(true);
        cameraPosition=B2BJ.class.getDeclaredMethod("cameraPosition",double.class,int.class,int.class);cameraPosition.setAccessible(true);
        referenceScenes();
        scaleSheet();
        roomViewports();
        gatehouse();
    }

    /** Room 1 through the real paint path (HUD included): Blob and Blade on the road beside the breach, Blade occluded. */
    private static void referenceScenes() throws Exception {
        B2BJ panel=panel(1);RuinedOutpostGame game=panel.game();
        var fence=game.map().barriers().get(0);
        double roadY=game.map().spawnY()+24; // road centre line bends through the breach here
        place(game,fence.centerX()+fence.width()/2+160,roadY); // clear of the fallen-arms dressing
        save(paint(panel),"reference-blob");
        game.player().collectIchor(100);
        if(!game.player().transform())throw new IllegalStateException("Blade transform refused");
        save(paint(panel),"reference-blade");
        double fenceTop=fence.centerY()-fence.height()/2;
        place(game,fence.centerX(),fenceTop-Player.COLLISION_Y_OFFSET-Player.COLLISION_RADIUS);
        save(paint(panel),"reference-blade-behind");
    }

    /** Every prop/decoration sprite at native 2x from its anchor, beside the Blob and Blade idle frames. */
    private static void scaleSheet() throws Exception {
        record Row(String name,BufferedImage sprite,int anchorX,int anchorY,RuinedOutpostMap.Obstacle feet) { }
        List<Row> rows=new ArrayList<>();
        for(var prop:RuinedOutpostMap.Prop.values()) {
            BufferedImage sprite=B2BJ.loadImage(prop.path());
            if(sprite==null) {System.out.println("note: missing sprite "+prop.path());continue;}
            rows.add(new Row(prop.name(),sprite,prop.anchorX,prop.anchorY,
                    new RuinedOutpostMap.Obstacle(0,-prop.footprintDepth,prop.footprintWidth*2,prop.footprintDepth*2)));
        }
        for(var decoration:RuinedOutpostMap.Decoration.values()) {
            BufferedImage sprite=B2BJ.loadImage(decoration.path());
            if(sprite==null) {System.out.println("note: missing sprite "+decoration.path());continue;}
            if(decoration==RuinedOutpostMap.Decoration.BRAZIER)sprite=sprite.getSubimage(0,0,64,64); // first flame frame
            rows.add(new Row(decoration.name(),sprite,decoration.anchorX,decoration.anchorY,
                    new RuinedOutpostMap.Dressing(0,0,decoration).obstacle()));
        }
        BufferedImage slime=B2BJ.loadImage("assets/characters/slime/slime_idle.png");
        BufferedImage blade=B2BJ.loadImage("assets/characters/blade/rainoray_idle.png");
        if(slime==null)System.out.println("note: missing slime idle sheet");
        if(blade==null)System.out.println("note: missing rainoray idle sheet");
        B2BJ panel=panel(1);
        var tilesField=B2BJ.class.getDeclaredField("terrainTiles");tilesField.setAccessible(true);
        BufferedImage[] tiles=(BufferedImage[])tilesField.get(panel);
        int rowHeight=256,groundOffset=216,sheetWidth=1000;
        BufferedImage sheet=new BufferedImage(sheetWidth,40+rows.size()*rowHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=pixelGraphics(sheet);
        g.setColor(new Color(24,31,44));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        g.setFont(LABEL);g.setColor(Color.WHITE);
        g.drawString("Native 2x scale sheet: prop anchor on the in-game ground band, Blob and Blade idle beside it",16,26);
        for(int i=0;i<rows.size();i++) {
            Row row=rows.get(i);int top=40+i*rowHeight,ground=top+groundOffset;
            // Same earth tile and dark overlay the world receives before props are drawn.
            for(int x=0;x<sheetWidth;x+=64) {
                if(tiles!=null)g.drawImage(tiles[0],x,ground-40,null);
                else {g.setColor(new Color(48,54,48));g.fillRect(x,ground-40,64,64);}
            }
            g.setColor(new Color(10,16,27,105));g.fillRect(0,ground-40,sheetWidth,64);
            g.setColor(new Color(255,255,255,70));g.drawLine(0,ground,sheetWidth,ground);
            int anchorX=420;
            g.drawImage(row.sprite(),anchorX-row.anchorX()*2,ground-row.anchorY()*2,
                    row.sprite().getWidth()*2,row.sprite().getHeight()*2,null);
            String feet="walkable";
            if(row.feet()!=null) {
                var o=row.feet();
                g.setColor(new Color(220,71,78,90));
                g.fillRect((int)(anchorX+o.centerX()-o.width()/2),(int)(ground+o.centerY()-o.height()/2),(int)o.width(),(int)o.height());
                g.setColor(new Color(220,71,78));
                g.drawRect((int)(anchorX+o.centerX()-o.width()/2),(int)(ground+o.centerY()-o.height()/2),(int)o.width(),(int)o.height());
                feet=String.format("footprint %dx%d world px",(int)o.width(),(int)o.height());
            }
            if(slime!=null)g.drawImage(slime,640-48,ground-96,640+48,ground,0,0,48,48,null);
            if(blade!=null)g.drawImage(blade,800-80,ground-144,800+80,ground+16,0,0,80,80,null);
            g.setColor(Color.WHITE);g.drawString(row.name(),16,top+80);
            g.setColor(new Color(191,203,209));
            g.drawString(String.format("native %dx%d px",row.sprite().getWidth(),row.sprite().getHeight()),16,top+100);
            g.drawString(feet,16,top+120);
            System.out.printf("scale %-12s native %3dx%-3d anchor %3d,%-3d %s%n",row.name(),
                    row.sprite().getWidth(),row.sprite().getHeight(),row.anchorX(),row.anchorY(),feet);
        }
        g.dispose();save(sheet,"scale-sheet");
    }

    /** Two gameplay-camera captures per room: player at spawn, and 400 px toward the first door. */
    private static void roomViewports() throws Exception {
        for(int room=0;room<RuinedOutpostMap.ROOMS.size();room++) {
            B2BJ panel=panel(room);RuinedOutpostMap map=panel.game().map();
            String name=String.format("room-%02d-viewport",room);
            save(viewport(panel),name+"-spawn");
            var door=map.doors().get(0);
            double dx=door.x()-map.spawnX(),dy=door.y()-map.spawnY(),length=Math.hypot(dx,dy);
            place(panel.game(),map.spawnX()+dx/length*400,map.spawnY()+dy/length*400);
            save(viewport(panel),name+"-door");
            System.out.printf("room %02d %-18s barriers=%d dressing=%d doors=%d%n",room,map.description().name(),
                    map.barriers().size(),map.dressing().size(),map.doors().size());
        }
    }

    /** Room 9 with the Warden mid-telegraph through paint, plus an overview with clearance circles over all solids. */
    private static void gatehouse() throws Exception {
        B2BJ panel=panel(9);RuinedOutpostGame game=panel.game();RuinedOutpostMap map=game.map();
        game.update(0.45,0,0); // simulation only: the committed tell is visible, no player input
        save(paint(panel),"gatehouse-warden");
        BufferedImage overview=new BufferedImage(map.worldWidth(),map.worldHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=pixelGraphics(overview);
        for(int cy:new int[]{0,432})for(int cx:new int[]{0,640})g.drawImage(world(panel,cx,cy),cx,cy,null);
        List<String> names=new ArrayList<>();List<RuinedOutpostMap.Obstacle> solids=new ArrayList<>();
        for(var barrier:map.barriers()) {names.add(barrier.prop()==null?"blockout":barrier.prop().name());solids.add(barrier);}
        for(var dressing:map.dressing())if(dressing.obstacle()!=null) {names.add(dressing.decoration().name());solids.add(dressing.obstacle());}
        for(var bank:map.banks()) {names.add("bank");solids.add(bank);}
        for(var door:map.doors())if(!map.passageOpen(door)) {names.add("locked passage to "+door.destination());solids.add(map.passageBarrier(door));}
        if(!map.gateOpen()) {names.add("closed gate");solids.add(new RuinedOutpostMap.Obstacle(map.gateX(),map.gateY(),48,320));}
        g.setColor(new Color(255,90,90));g.setStroke(new BasicStroke(2));
        for(var o:solids)g.drawRect((int)(o.centerX()-o.width()/2),(int)(o.centerY()-o.height()/2),(int)o.width(),(int)o.height());
        circle(g,map.guardianX(),map.guardianY(),GUARDIAN_CLEARANCE,new Color(220,71,78,70));
        report("guardian start",map.guardianX(),map.guardianY(),GUARDIAN_CLEARANCE,names,solids);
        for(int[] slot:SUMMON_SLOTS) {
            double x=map.authored(slot[0]),y=map.authored(slot[1]);
            circle(g,x,y,SLOT_CLEARANCE,new Color(230,188,92,80));
            report("summon slot",x,y,SLOT_CLEARANCE,names,solids);
        }
        g.dispose();save(overview,"gatehouse-clearance");
    }

    private static void report(String what,double x,double y,double radius,List<String> names,List<RuinedOutpostMap.Obstacle> solids) {
        List<String> hits=new ArrayList<>();
        for(int i=0;i<solids.size();i++)if(intersects(x,y,radius,solids.get(i)))hits.add(names.get(i));
        System.out.printf("room 09 %s (%.0f,%.0f) r%.0f: %s%n",what,x,y,radius,hits.isEmpty()?"clear":"intersects "+String.join(", ",hits));
    }
    private static boolean intersects(double x,double y,double radius,RuinedOutpostMap.Obstacle o) {
        double dx=x-Math.max(o.centerX()-o.width()/2,Math.min(o.centerX()+o.width()/2,x));
        double dy=y-Math.max(o.centerY()-o.height()/2,Math.min(o.centerY()+o.height()/2,y));
        return dx*dx+dy*dy<radius*radius;
    }
    private static void circle(Graphics2D g,double x,double y,double radius,Color fill) {
        g.setColor(fill);g.fillOval((int)(x-radius),(int)(y-radius),(int)(radius*2),(int)(radius*2));
        g.setColor(new Color(fill.getRed(),fill.getGreen(),fill.getBlue()));
        g.drawOval((int)(x-radius),(int)(y-radius),(int)(radius*2),(int)(radius*2));
        g.drawLine((int)x-6,(int)y,(int)x+6,(int)y);g.drawLine((int)x,(int)y-6,(int)x,(int)y+6);
    }

    private static B2BJ panel(int room) throws Exception {
        B2BJ panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());
        panel.game().begin();enterRoom.invoke(panel.game(),room,-1);
        return panel;
    }
    private static void place(RuinedOutpostGame game,double x,double y) {
        game.player().relocate(x,y);
        if(game.map().isBlocked(x,y+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))
            System.out.printf("note: room %02d player at %.0f,%.0f overlaps a solid%n",game.map().room(),x,y);
    }
    private static BufferedImage viewport(B2BJ panel) throws Exception {
        Player player=panel.game().player();RuinedOutpostMap map=panel.game().map();
        int cx=(int)cameraPosition.invoke(null,player.x(),W,map.worldWidth());
        int cy=(int)cameraPosition.invoke(null,player.y(),H,map.worldHeight());
        return world(panel,cx,cy);
    }
    private static BufferedImage world(B2BJ panel,int cameraX,int cameraY) throws Exception {
        BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=pixelGraphics(image);drawWorld.invoke(panel,g,cameraX,cameraY);g.dispose();
        return image;
    }
    private static BufferedImage paint(B2BJ panel) {
        BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
        return image;
    }
    private static Graphics2D pixelGraphics(BufferedImage image) {
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        return g;
    }
    private static void save(BufferedImage image,String name) throws Exception {
        ImageIO.write(image,"png",out.resolve(name+".png").toFile());
    }
}
