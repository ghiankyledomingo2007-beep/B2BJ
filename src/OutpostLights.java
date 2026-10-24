import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Soft floor light with projected shadows, plus a small bloom over each emissive sprite. */
final class OutpostLights {
    private OutpostLights() { }

    private static final Map<Long, BufferedImage> CACHE = new HashMap<>();
    private record FloorLight(BufferedImage base, BufferedImage live) { }
    private static final Map<SetPieces.Light, FloorLight> FLOORS = new HashMap<>();

    /** Glow disc of `radius` native pixels; slightly squashed vertically for the 3/4 view. */
    static BufferedImage glow(int radius, int rgb, int peakAlpha) {
        long key = ((long) radius << 40) | ((rgb & 0xffffffL) << 8) | (peakAlpha & 0xff);
        return CACHE.computeIfAbsent(key, k -> {
            int size = radius * 2;
            var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                double d = Math.hypot(x + .5 - radius, (y + .5 - radius) * 1.25) / radius;
                if (d >= 1) continue;
                double strength = (1 - d) * (1 - d);
                int alpha = (int) Math.round(peakAlpha * strength);
                image.setRGB(x, y, alpha << 24 | (rgb & 0xffffff));
            }
            return image;
        });
    }

    static void drawGround(Graphics2D g, RuinedOutpostGame game, int cx, int cy, long frame, boolean reduced) {
        for (var light : SetPieces.lights(game.biome())) {
            if (!active(light, game) || game.biome() == 1 && (light.colour() == 0xeeb840 || light.colour() == 0xffdd80)) continue;
            double sx=light.x()-cx, sy=light.groundY()-cy, r=light.radius();
            if (sx < -r || sy < -r || sx > 1280+r || sy > 720+r) continue;
            var floor=FLOORS.computeIfAbsent(light, key -> floorLight(key, game));
            var layer=floor.live().createGraphics();
            layer.setComposite(AlphaComposite.Src);
            layer.drawImage(floor.base(), 0, 0, null);
            floorCoordinates(layer, light);
            var p=game.player();
            if (p.alive()) shadow(layer,light,p.x(),p.y()+48,p.bladeForm()?34:64,18,p.bladeForm()?116:52);
            for (var enemy:game.scouts()) if (enemy.alive() && enemy.visible())
                shadow(layer,light,enemy.x(),enemy.y()+WispAnimation.RENDER_SIZE/2,42,18,76);
            var boss=game.guardian();
            if (boss.alive() && boss.visible()) shadow(layer,light,boss.x(),boss.y()+Guardian.GROUND_Y_OFFSET,100,32,160);
            if (game.biome()==0) {
                var hub=game.campaignArea().hub();
                shadow(layer,light,hub.x()-112,hub.y()+64,32,16,112);
            }
            layer.dispose();
            var lit=(Graphics2D)g.create();
            float strength=reduced || !light.fire()?1f:(float)(.94+.035*Math.sin(frame*.13+light.x())+.025*Math.sin(frame*.29));
            lit.setComposite(AlphaComposite.SrcOver.derive(strength));
            int size=floor.live().getWidth()*2;
            lit.drawImage(floor.live(),(int)Math.round(sx-r),(int)Math.round(sy-r),size,size,null);
            lit.dispose();
        }
    }

    private static FloorLight floorLight(SetPieces.Light light, RuinedOutpostGame game) {
        int radius=(int)Math.round(light.radius()/2);
        var base=new BufferedImage(radius*2,radius*2,BufferedImage.TYPE_INT_ARGB_PRE);
        var g=base.createGraphics();
        g.drawImage(glow(radius,light.colour(),light.fire()?104:76),0,0,null);
        floorCoordinates(g,light);
        for (var piece:SetPieces.pieces(game.biome())) {
            var k=piece.kind();
            if (k==SetPieces.Kind.BRAZIER && Math.hypot(piece.x()-light.x(),piece.y()-light.groundY())<8) {
                // Three legs break up the pool immediately beneath the bowl.
                shadow(g,light,piece.x()-20,piece.y()-2,5,5,50);
                shadow(g,light,piece.x()+20,piece.y()-2,5,5,50);
                shadow(g,light,piece.x(),piece.y()+8,5,5,50);
            } else if (piece.solid()) {
                shadow(g,light,piece.x(),piece.y()-k.footDepth/2.0,k.footWidth,k.footDepth,k.anchorY*2);
            }
        }
        for (var wall:game.campaignArea().obstacles())
            shadow(g,light,wall.centerX(),wall.centerY(),wall.width(),wall.height(),120);
        g.dispose();
        return new FloorLight(base,new BufferedImage(radius*2,radius*2,BufferedImage.TYPE_INT_ARGB_PRE));
    }

    private static void floorCoordinates(Graphics2D g, SetPieces.Light light) {
        g.scale(.5,.5);
        g.translate(light.radius()-light.x(),light.radius()-light.groundY());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.DstOut);
    }

    // shortcut: project footprints, not sprite silhouettes; use silhouettes if limb-shaped shadows become necessary.
    static void shadow(Graphics2D g, SetPieces.Light light, double x, double y, double width, double depth, double height) {
        double dx=x-light.x(),dy=y-light.groundY(),distance=Math.hypot(dx,dy);
        if (distance<Math.max(width,depth)*.5 || distance>=light.radius()) return;
        double ux=dx/distance,uy=dy/distance;
        double half=(Math.abs(uy)*width+Math.abs(ux)*depth)*.5;
        double length=Math.min(light.radius()-distance,Math.max(18,distance*height/Math.max(24,light.groundY()-light.y()-height)));
        double endX=x+ux*length,endY=y+uy*length,spread=1+length/Math.max(80,distance)*.45;
        var path=new Path2D.Double();
        path.moveTo(x-uy*half,y+ux*half);
        path.lineTo(x+uy*half,y-ux*half);
        path.lineTo(endX+uy*half*spread,endY-ux*half*spread);
        path.quadTo(endX+ux*half,endY+uy*half,endX-uy*half*spread,endY+ux*half*spread);
        path.closePath();
        g.setPaint(new GradientPaint((float)x,(float)y,new Color(0,0,0,225),(float)endX,(float)endY,new Color(0,0,0,0)));
        g.fill(path);
    }

    private static boolean active(SetPieces.Light light, RuinedOutpostGame game) {
        if (game.biome()==0) {
            var rests=FieldObjects.rests(0);
            for (int i=0;i<rests.size();i++)
                if (light.x()==rests.get(i).x() && light.groundY()==rests.get(i).y() && !game.restLit(i)) return false;
        }
        var boss=game.guardian();
        var gatehouse=game.campaignArea().landmarks().get(8);
        return !(game.biome()==0 && boss.alive() && boss.state()==Guardian.State.DORMANT && light.fire()
                && Math.hypot(light.x()-gatehouse.x(),light.groundY()-gatehouse.y())<420);
    }

    static void draw(Graphics2D g, RuinedOutpostGame game, int cx, int cy, long frame, boolean reduced) {
        int index = 0;
        for (var light : SetPieces.lights(game.biome())) {
            index++;
            if (!active(light,game)) continue;
            double sx = light.x() - cx, sy = light.y() - cy, r = light.radius()*.48;
            if (sx < -r || sy < -r || sx > 1280 + r || sy > 720 + r) continue;
            // fires breathe between three sizes; lamps and Ichor hold steady
            int step = light.fire() && !reduced ? Math.floorMod((frame / 5 + index * 7) * 2654435761L >>> 7, 3) : 1;
            int radius = (int) Math.round(r / 2 * (.96 + step * .04));
            disc(g, glow(radius, light.colour(), light.fire()?42:32), sx, sy, radius);
        }
        var player = game.player();
        if (player.alive()) disc(g, glow(46, 0x2cc6c8, 34), player.x() - cx, player.y() - cy, 46);
        var warden = game.guardian();
        if (warden.alive() && warden.visible() && warden.state() != Guardian.State.DORMANT) {
            boolean rage = warden.enraged();
            int pulse = reduced ? 0 : (int) (frame / 6 % 3);
            int radius = rage ? 92 + pulse * 3 : 78;
            int colour = game.biome() == 1 ? (rage ? 0xffdd80 : 0xeeb840) : rage ? 0x7aeee6 : 0x2cc6c8;   // Briarheart burns gold
            disc(g, glow(radius, colour, rage ? 64 : 40), warden.x() - cx, warden.y() - cy - 40, radius);
        }
    }

    private static void disc(Graphics2D g, BufferedImage image, double sx, double sy, int radius) {
        int left = (int) Math.round(sx / 2) * 2 - radius * 2, top = (int) Math.round(sy / 2) * 2 - radius * 2;
        g.drawImage(CampaignRenderer.doubled(image), left, top, null);
    }
}
