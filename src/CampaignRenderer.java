import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Reviewed character art with Java2D placeholders for unfinished campaign assets. */
public final class CampaignRenderer {
    private static final java.awt.image.BufferedImage GUARD = B2BJ.loadImage("assets/characters/campaign/outpost_guard.png");
    private static final java.awt.image.BufferedImage KNIGHT = B2BJ.loadImage("assets/characters/campaign/fallen_knight.png");
    /** The whole Outpost floor at native resolution (1 px = 2 world px), baked by tools/art from this map's layout. */
    private static final java.awt.image.BufferedImage GROUND = opaque(B2BJ.loadImage("assets/tilesets/outpost/outpost_ground.png"));
    /** The Corrupted Forest floor, baked by tools/art from 16x16 tiles of the forest tileset. */
    private static final java.awt.image.BufferedImage FOREST_GROUND = opaque(B2BJ.loadImage("assets/tilesets/forest/forest_ground.png"));
    private static final java.awt.image.BufferedImage RUIN_WALL = B2BJ.loadImage("assets/props/outpost/ruin_wall.png");
    private static final java.awt.image.BufferedImage STAKE_WALL = B2BJ.loadImage("assets/props/outpost/stake_wall.png");
    private static final java.util.Map<SetPieces.Kind, java.awt.image.BufferedImage> SET_PIECES = new java.util.EnumMap<>(SetPieces.Kind.class);
    static { for (var kind : SetPieces.Kind.values()) { var sheet = B2BJ.loadImage(kind.sheet()); if (sheet != null) SET_PIECES.put(kind, sheet); } }
    private static final java.awt.image.BufferedImage SUPPLIES = SET_PIECES.get(SetPieces.Kind.SUPPLIES);
    private static final java.awt.image.BufferedImage FIRE_CAMP = B2BJ.loadImage("assets/effects/fire_camp.png");
    private static final java.awt.image.BufferedImage FIRE_BRAZIER = B2BJ.loadImage("assets/effects/fire_brazier.png");
    /** The baked floor replaces the placeholder blocks only when every prop and set piece drawn over it loads too. */
    private static final boolean OUTPOST_ART = GROUND != null && RUIN_WALL != null && STAKE_WALL != null && SUPPLIES != null
            && SET_PIECES.size() == SetPieces.Kind.values().length;
    private static final boolean FOREST_ART = FOREST_GROUND != null && SET_PIECES.size() == SetPieces.Kind.values().length;
    private static final Color INK = new Color(12, 15, 24);
    private static final Color PANEL = new Color(15, 20, 30, 238);
    private static final Color TEXT = new Color(219, 227, 223);
    private static final Color MUTED = new Color(144, 160, 165);
    private static final Color GOLD = new Color(232, 183, 58);
    private static final Color CYAN = new Color(79, 227, 216);
    private static final Color DANGER = new Color(241, 100, 88);
    private static final Color[][] PALETTES = {
        {new Color(31, 39, 39), new Color(56, 58, 53), new Color(42, 48, 45), new Color(87, 94, 89), new Color(149, 158, 123)},
        {new Color(22, 42, 38), new Color(43, 57, 47), new Color(33, 58, 45), new Color(64, 92, 69), new Color(137, 180, 136)},
        {new Color(29, 28, 40), new Color(56, 52, 64), new Color(42, 39, 53), new Color(90, 80, 108), new Color(173, 156, 195)},
        {new Color(30, 25, 43), new Color(55, 47, 70), new Color(46, 34, 62), new Color(90, 66, 118), new Color(185, 143, 207)}
    };

    private CampaignRenderer() { }

    public static void drawGround(Graphics2D canvas, RuinedOutpostGame game,
            int cx, int cy, int width, int height) {
        var map = game.map();
        if (bakedGround(game)) {
            canvas.setColor(INK);
            canvas.fillRect(0, 0, width, height);
            // The camera may look past the map edge; copy only the part that exists, 2x nearest-neighbour.
            int left = Math.max(0, cx), top = Math.max(0, cy);
            int right = Math.min(map.worldWidth(), cx + width), bottom = Math.min(map.worldHeight(), cy + height);
            if (right > left && bottom > top)
                canvas.drawImage(groundImage(game), left - cx, top - cy, right - cx, bottom - cy,
                        left / 2, top / 2, right / 2, bottom / 2, null);
            return;
        }
        Color[] colors = PALETTES[game.biome()];
        canvas.setColor(colors[0]);
        canvas.fillRect(0, 0, width, height);
        int firstX = Math.max(0, cx / 64), firstY = Math.max(0, cy / 64);
        for (int ty = firstY; ty <= Math.min(map.heightInTiles() - 1, (cy + height) / 64); ty++) {
            for (int tx = firstX; tx <= Math.min(map.widthInTiles() - 1, (cx + width) / 64); tx++) {
                int x = tx * 64 - cx, y = ty * 64 - cy, mask = map.tileMask(tx, ty);
                int seed = Math.floorMod(tx * 73 + ty * 109 + game.biome() * 31, 97);
                if (mask != 0) {
                    canvas.setColor(colors[1]);
                    if (mask == 15) canvas.fillRect(x, y, 64, 64);
                    else {
                        if ((mask & 1) != 0) canvas.fillRect(x, y, 40, 40);
                        if ((mask & 2) != 0) canvas.fillRect(x + 24, y, 40, 40);
                        if ((mask & 4) != 0) canvas.fillRect(x, y + 24, 40, 40);
                        if ((mask & 8) != 0) canvas.fillRect(x + 24, y + 24, 40, 40);
                    }
                }
                canvas.setColor(colors[2]);
                canvas.fillRect(x + seed % 26 * 2, y + seed % 23 * 2, 8, 2);
                canvas.fillRect(x + 10 + seed % 19 * 2, y + 12 + seed % 17 * 2, 2, 6);
                if (game.biome() >= 2 && mask != 0) {
                    canvas.setColor(colors[0]);
                    canvas.drawLine(x + 2, y + 62, x + 60, y + 62);
                    canvas.drawLine(x + (ty % 2) * 30, y + 2, x + (ty % 2) * 30, y + 60);
                }
            }
        }
    }

    public static void drawScenery(Graphics2D canvas, RuinedOutpostGame game,
            int cx, int cy, int width, int height) {
        Color[] colors = PALETTES[game.biome()];
        boolean baked = bakedGround(game);
        if (!baked) for (var bank : game.map().banks()) {
            int x = (int) (bank.centerX() - bank.width() / 2) - cx;
            int y = (int) (bank.centerY() - bank.height() / 2) - cy;
            canvas.setColor(INK);
            canvas.fillRect(x, y, (int) bank.width(), (int) bank.height());
        }
        for (var obstacle : game.map().barriers()) {
            if (baked) continue; // props and set pieces are drawn depth-sorted with the actors
            int x = (int) (obstacle.centerX() - obstacle.width() / 2) - cx;
            int y = (int) (obstacle.centerY() - obstacle.height() / 2) - cy;
            int w = (int) obstacle.width(), h = (int) obstacle.height();
            if (x > width + 80 || y > height + 80 || x + w < -80 || y + h < -80) continue;
            if (game.biome() == 2 && w >= 260) {
                // Adjacent collision spans form one stone mass, without artificial floor-sized seams.
                canvas.setColor(colors[2]);
                canvas.fillRect(x, y, w, h);
                canvas.setColor(new Color(50, 46, 64));
                for (int mark = 24; mark < w; mark += 64)
                    canvas.fillRect(x + mark, y + h / 2, 12, 2);
                continue;
            }
            canvas.setColor(new Color(5, 10, 16, 110));
            canvas.fillRect(x + 10, y + 12, w + 8, h + 12);
            canvas.setColor(colors[2]);
            canvas.fillRect(x, y - 14, w, h + 14);
            canvas.setColor(colors[3]);
            canvas.fillRect(x, y - 18, w, h);
            canvas.setColor(colors[1]);
            canvas.fillRect(x + 4, y - 14, Math.max(0, w - 8), Math.max(4, h - 8));
            if (game.biome() == 1 && w < 260 && h < 260) {
                int trunk = x + w / 2;
                canvas.setColor(colors[3]);
                canvas.fillRect(trunk - 8, y - 42, 16, h + 36);
                canvas.fillPolygon(new int[]{x - 10, trunk, x + w + 10, trunk + 18},
                        new int[]{y, y - 76, y, y + 14}, 4);
                canvas.setColor(colors[2]);
                canvas.fillPolygon(new int[]{x, trunk - 8, trunk, x + w},
                        new int[]{y - 16, y - 60, y - 8, y - 10}, 4);
            } else if (game.biome() == 3 && w < 220 && h < 220) {
                canvas.setColor(colors[3]);
                canvas.fillPolygon(new int[]{x + w / 2, x + w, x + w / 2, x},
                        new int[]{y - 56, y + h / 2, y + h, y + h / 2}, 4);
                canvas.setColor(colors[4]);
                canvas.drawLine(x + w / 2, y - 48, x + w / 2, y + h - 8);
            } else {
                canvas.setColor(colors[3]);
                for (int seam = 48; seam < w; seam += 64)
                    canvas.drawLine(x + seam, y - 14, x + seam, y + h - 4);
            }
        }
        var area = game.campaignArea();
        for (var landmark : area.landmarks()) {
            if (Math.hypot(landmark.x() - area.hub().x(), landmark.y() - area.hub().y()) < 100) continue;
            int x = (int) landmark.x() - cx, y = (int) landmark.y() - cy;
            if (x < -120 || x > width + 120 || y < -120 || y > height + 120) continue;
            canvas.setColor(landmark.optional() ? GOLD : colors[4]);
            if (!baked) {
                canvas.drawRect(x - 10, y - 8, 20, 16);
                canvas.fillRect(x - 2, y - 18, 4, 8);
            }
            if (!game.bossActive() && Math.hypot(game.player().x() - landmark.x(), game.player().y() - landmark.y()) < 280)
                Ui.outlinedCentered(canvas, landmark.name(), x, y - 28, 2, landmark.optional() ? Ui.GOLD : Ui.MUTED);
        }
        int hx = (int) area.hub().x() - cx, hy = (int) area.hub().y() - cy;
        if (!baked) {
            canvas.setColor(new Color(51, 61, 60));
            canvas.fillOval(hx - 54, hy + 2, 108, 32);
            canvas.setColor(GOLD);
            canvas.drawOval(hx - 42, hy - 2, 84, 28);
        }
        Ui.outlinedCentered(canvas, "CAMP", hx, hy + 72, 2, Ui.MUTED);
        if (!baked) portal(canvas, area.exit(), cx, cy, "EAST GATE", !game.guardian().alive() ? GOLD : MUTED);
        else Ui.outlinedCentered(canvas, game.biome() == 0 ? "EAST GATE" : "ROOTED STAIR", (int) area.exit().x() - cx,
                (int) area.exit().y() - cy - 316, 2, !game.guardian().alive() ? Ui.GOLD : Ui.MUTED);
    }

    /** One landmark set piece; animated kinds take their cell from the clock, fires play on top. */
    static void drawSetPiece(Graphics2D g, SetPieces.Piece p, RuinedOutpostGame game, int cx, int cy, long frame) {
        var kind = p.kind();
        var sheet = SET_PIECES.get(kind);
        if (sheet == null) return;
        int cells = Math.max(1, sheet.getWidth() / kind.width);
        int phase = Math.floorMod((int) p.x() * 7 + (int) p.y() * 3, 64);
        int cell = switch (kind) {
            case BANNER -> (int) ((frame + phase) * 8 / 60 % cells);
            case LAMP -> ((frame + phase) / 7 * 1103515245L >>> 16 & 7) < 2 ? 1 : 0;
            case EAST_GATE -> game.guardian().alive() ? 0 : Math.min(1, cells - 1);
            default -> Math.min(p.variant(), cells - 1);
        };
        int left = (int) Math.round(p.x()) - cx - kind.anchorX * 2;
        int top = (int) Math.round(p.y()) - cy - kind.anchorY * 2;
        cell2x(g, sheet, left, top, cell * kind.width, 0, kind.width, sheet.getHeight());
        switch (kind) {
            case CAMPFIRE -> fire(g, FIRE_CAMP, 30, p.x(), p.y() - 16, cx, cy, frame + phase);
            case BRAZIER -> {
                boolean cold = SetPieces.arenaBrazier(p) && game.guardian().alive() && game.guardian().state() == Guardian.State.DORMANT;
                if (!cold) fire(g, FIRE_BRAZIER, 22, p.x(), p.y() - 60, cx, cy, frame + phase);
            }
            case WATCHTOWER -> fire(g, FIRE_BRAZIER, 22, p.x() + 4, p.y() - 376, cx, cy, frame + phase);
            default -> { }
        }
    }

    /** Screen box a set piece paints into, its fire included; null without its sheet. */
    static java.awt.Rectangle bounds(SetPieces.Piece p, int cx, int cy) {
        var kind = p.kind();
        var sheet = SET_PIECES.get(kind);
        if (sheet == null) return null;
        int flame = kind == SetPieces.Kind.CAMPFIRE || kind == SetPieces.Kind.BRAZIER ? 48 : 0;   // fire rising over the sheet
        int left = (int) Math.round(p.x()) - cx - kind.anchorX * 2, top = (int) Math.round(p.y()) - cy - kind.anchorY * 2;
        return new java.awt.Rectangle(left, top - flame, kind.width * 2, sheet.getHeight() * 2 + flame);
    }

    /** A looping fire sheet whose second-to-last row sits on (x, y), drawn at 2x. */
    private static void fire(Graphics2D g, java.awt.image.BufferedImage sheet, int cell, double x, double y,
            int cx, int cy, long frame) {
        if (sheet == null) return;
        int frames = sheet.getWidth() / cell, f = (int) (frame * 12 / 60 % frames);
        int left = (int) Math.round(x) - cx - cell, top = (int) Math.round(y) - cy - (sheet.getHeight() - 2) * 2;
        cell2x(g, sheet, left, top, f * cell, 0, cell, sheet.getHeight());
    }

    /** PNGs decode to a slow pixel layout; the floor is copied once into plain RGB so each frame is a fast blit. */
    private static java.awt.image.BufferedImage opaque(java.awt.image.BufferedImage source) {
        if (source == null) return null;
        var image = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return image;
    }

    private static java.awt.image.BufferedImage propSheet(RuinedOutpostMap.Prop prop) {
        if (prop == null) return null;
        return switch (prop) { case WALL -> RUIN_WALL; case PALISADE -> STAKE_WALL; case BARREL -> SUPPLIES; default -> null; };
    }

    /**
     * Outpost obstacle art over its unchanged collision footprint. Sprite anchor = the footprint's
     * front-centre in native pixels; the variant follows the position, so every visit looks the same.
     */
    static void drawProp(Graphics2D g, RuinedOutpostMap.Obstacle o, int cx, int cy) {
        var box = propBounds(o, cx, cy);
        if (box == null) return;
        var sheet = propSheet(o.prop());
        int cell = box.width / 2;
        int mix = (int) o.centerX() * 73856093 ^ (int) o.centerY() * 19349663;
        int variant = Math.floorMod(mix ^ mix >>> 13, sheet.getWidth() / cell);
        cell2x(g, sheet, box.x, box.y, variant * cell, 0, cell, sheet.getHeight());
    }

    /** Screen box an Outpost prop paints into; null for obstacles without art. */
    static java.awt.Rectangle propBounds(RuinedOutpostMap.Obstacle o, int cx, int cy) {
        var sheet = propSheet(o.prop());
        if (sheet == null) return null;
        int cell = o.prop() == RuinedOutpostMap.Prop.BARREL ? 56 : 96;
        int anchorY = switch (o.prop()) { case WALL -> 76; case PALISADE -> 108; default -> 68; };
        return new java.awt.Rectangle((int) Math.round(o.centerX()) - cx - cell,
                (int) Math.round(o.centerY() + o.height() / 2) - cy - anchorY * 2, cell * 2, sheet.getHeight() * 2);
    }

    private static final java.util.Map<java.awt.image.BufferedImage, java.awt.image.BufferedImage> DOUBLED = new java.util.IdentityHashMap<>();
    /** A sheet enlarged 2x once (premultiplied), so each frame is a plain copy instead of a scaled draw. */
    static java.awt.image.BufferedImage doubled(java.awt.image.BufferedImage sheet) {
        return DOUBLED.computeIfAbsent(sheet, s -> {
            var big = new java.awt.image.BufferedImage(s.getWidth() * 2, s.getHeight() * 2, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            var g = big.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(s, 0, 0, big.getWidth(), big.getHeight(), null);
            g.dispose();
            return big;
        });
    }
    /** Copies native cell (sx, sy, w, h) of a sheet to (left, top) at 2x. */
    private static void cell2x(Graphics2D g, java.awt.image.BufferedImage sheet, int left, int top, int sx, int sy, int w, int h) {
        var big = doubled(sheet);
        g.drawImage(big, left, top, left + w * 2, top + h * 2, sx * 2, sy * 2, (sx + w) * 2, (sy + h) * 2, null);
    }

    private static java.awt.image.BufferedImage groundImage(RuinedOutpostGame game) {
        return switch (game.biome()) { case 0 -> OUTPOST_ART ? GROUND : null; case 1 -> FOREST_ART ? FOREST_GROUND : null; default -> null; };
    }

    /** The baked floor already carries the forest banks and is sized to this map. */
    static boolean bakedGround(RuinedOutpostGame game) {
        var ground = groundImage(game);
        return game.campaignMode() && ground != null && ground.getWidth() * 2 == game.map().worldWidth()
                && ground.getHeight() * 2 == game.map().worldHeight();
    }

    private static void portal(Graphics2D g, CampaignWorld.Point point, int cx, int cy,
            String label, Color color) {
        int x = (int) point.x() - cx, y = (int) point.y() - cy;
        g.setColor(INK);
        g.fillRect(x - 46, y - 76, 92, 108);
        g.setColor(color);
        g.fillRect(x - 52, y - 84, 12, 118);
        g.fillRect(x + 40, y - 84, 12, 118);
        g.fillRect(x - 52, y - 84, 104, 12);
        g.drawRect(x - 32, y - 62, 64, 84);
        Ui.outlinedCentered(g, label, x, y - 100, 2, color);
    }

    private static void drawNpc(Graphics2D g, int x, int y, int biome) {
        g.setColor(INK);
        g.fillRect(x - 18, y - 54, 36, 66);
        g.setColor(PALETTES[biome][4]);
        g.fillPolygon(new int[]{x - 10, x + 10, x + 24, x - 24},
                new int[]{y - 38, y - 38, y + 10, y + 10}, 4);
        g.setColor(new Color(184, 174, 149));
        g.fillRect(x - 9, y - 58, 18, 18);
        g.setColor(INK);
        g.fillRect(x - 8, y - 58, 16, 5);
        g.fillRect(x - 12, y + 8, 9, 10);
        g.fillRect(x + 3, y + 8, 9, 10);
        g.setColor(GOLD);
        g.fillRect(x + 28, y - 36, 4, 54);
        g.fillRect(x + 24, y - 42, 12, 8);
    }

    public static void drawEnemy(Graphics2D canvas, CampaignEnemy enemy, int cx, int cy) {
        Wisp body = enemy.body();
        var sheet=meleeSheet(enemy.kind());
        boolean knight=enemy.kind()==CampaignEnemy.Kind.FALLEN_KNIGHT;
        if (!body.visible() || (sheet!=null && !body.alive())) return; // The edible corpse owns the collapse.
        Graphics2D g = (Graphics2D) canvas.create();
        int x = (int) Math.round(body.x()) - cx, y = (int) Math.round(body.y()) - cy + 22;
        g.translate(x, y);
        g.scale(body.renderScale(), body.renderScale());
        g.setColor(new Color(5, 8, 16, 140));
        g.fillOval(-28, -6, 56, 18);
        if (sheet!=null) {
            int direction=body.animation().row();
            int row=body.usesAttackAnimation()?(knight?13+direction:switch(direction){case 1->10;case 2->12;case 3->14;default->9;})
                    :body.state()==Wisp.State.HURT?(knight?9:5)+direction:(knight?1:16)+direction;
            int frame=body.usesAttackAnimation()?(knight?body.attackFrame(9,3,4):body.attackFrame(8,4,4))
                    :body.state()==Wisp.State.HURT?body.hurtFrame(8):body.animation().frame();
            meleeFrame(g,B2BJ.hitTint(sheet,B2BJ.whiteHit(body)),knight,row,frame,0,0);
        } else {
        // Grounded anticipation/contact/recovery; shadow stays at the collision anchor.
        int lean = switch(body.state()) { case TELEGRAPH -> -5; case LUNGE -> 10; case RECOVER -> 2; default -> 0; };
        g.translate(Math.rint(body.intentX()*lean), Math.rint(body.intentY()*lean));
        if(body.intentX()<-.25)g.scale(-1,1);
        boolean hurt = body.state() == Wisp.State.HURT;
        Color skin = hurt ? TEXT : switch (enemy.kind()) {
            case RIFT_IMP -> new Color(151, 119, 182);
            case THORN_WOLF -> new Color(111, 151, 114);
            case MIRE_SHAMAN -> new Color(108, 157, 147);
            case FALLEN_KNIGHT -> new Color(151, 159, 173);
            case CINDER_HEXER -> new Color(179, 129, 179);
            case OUTPOST_SCOUT -> new Color(112, 142, 138);
            case OUTPOST_GUARD -> new Color(146, 143, 117);
            case OUTPOST_SPITTER -> new Color(138, 127, 161);
        };
        g.setColor(skin);
        switch (enemy.kind()) {
            case RIFT_IMP -> {
                int flap = body.animation().frame() % 2 * 8;
                g.fillPolygon(new int[]{-8, -42, -32, -14}, new int[]{-30, -48 + flap, -14, -12}, 4);
                g.fillPolygon(new int[]{8, 42, 32, 14}, new int[]{-30, -48 + flap, -14, -12}, 4);
                g.fillRect(-12, -38, 24, 30);
                g.fillRect(-14, -48, 6, 14);
                g.fillRect(8, -48, 6, 14);
                g.fillRect(-12, -10, 8, 12);
                g.fillRect(4, -10, 8, 12);
            }
            case THORN_WOLF -> {
                g.fillRect(-32, -28, 54, 22);
                g.fillPolygon(new int[]{12, 34, 36, 12}, new int[]{-34, -30, -10, -8}, 4);
                g.fillPolygon(new int[]{-32, -48, -26}, new int[]{-22, -38, -30}, 3);
                for (int leg : new int[]{-26, -10, 10, 24}) g.fillRect(leg, -10, 6, 15);
                for (int thorn = -24; thorn < 20; thorn += 12)
                    g.fillPolygon(new int[]{thorn, thorn + 5, thorn + 10}, new int[]{-26, -42, -26}, 3);
            }
            case FALLEN_KNIGHT, OUTPOST_GUARD -> {
                g.fillRect(-20, -42, 40, 36);
                g.fillRect(-13, -62, 26, 22);
                g.fillRect(-18, -8, 12, 14);
                g.fillRect(6, -8, 12, 14);
                g.setColor(INK);
                g.fillRect(-8, -53, 16, 6);
                g.fillRect(-4, -40, 8, 24);
                g.setColor(enemy.shielded() ? GOLD : PALETTES[2][3]);
                int shieldPush = enemy.kind()==CampaignEnemy.Kind.OUTPOST_GUARD
                        ? switch(body.state()) {case TELEGRAPH -> 8; case LUNGE -> 38; case RECOVER -> 16; default -> 0;} : 0;
                g.translate(shieldPush,0);
                g.fillPolygon(new int[]{-37, -14, -14, -26, -37}, new int[]{-40, -40, -10, 2, -10}, 5);
                g.translate(-shieldPush,0);
                g.setColor(TEXT);
                int lift = enemy.kind()==CampaignEnemy.Kind.FALLEN_KNIGHT && body.state()==Wisp.State.TELEGRAPH ? -30 : 0;
                g.fillRect(28, -54 + lift, 5, 52);
                g.fillRect(22, -12 + lift, 17, 5);
                if (enemy.kind() == CampaignEnemy.Kind.OUTPOST_GUARD) {
                    g.setColor(skin);
                    g.fillRect(-21, -69, 42, 8);
                }
            }
            case MIRE_SHAMAN, CINDER_HEXER, OUTPOST_SPITTER -> {
                g.fillPolygon(new int[]{0, 27, 17, -17, -27}, new int[]{-64, -20, 4, 4, -20}, 5);
                g.setColor(INK);
                g.fillRect(-11, -38, 22, 16);
                g.setColor(skin);
                g.fillRect(30, -58, 4, 63);
                if (enemy.kind() == CampaignEnemy.Kind.CINDER_HEXER) {
                    g.drawOval(-24, -72, 48, 24);
                    g.fillRect(-4, -80, 8, 10);
                } else if (enemy.kind() == CampaignEnemy.Kind.MIRE_SHAMAN) {
                    g.fillOval(22, -66, 20, 18);
                } else {
                    g.fillRect(-30, -48, 60, 10);
                    g.fillRect(24, -65, 16, 16);
                }
            }
            case OUTPOST_SCOUT -> {
                g.fillOval(-24, -40, 48, 42);
                g.fillRect(-18, -46, 36, 20);
                g.fillPolygon(new int[]{-24, -36, -8}, new int[]{-18, -40, -28}, 3);
                g.fillPolygon(new int[]{24, 36, 8}, new int[]{-18, -40, -28}, 3);
            }
        }
        g.setColor(body.state() == Wisp.State.TELEGRAPH ? DANGER : GOLD);
        g.fillRect(-7, -32, 4, 4);
        g.fillRect(4, -32, 4, 4);
        }
        g.dispose();
        if (body.alive() && (body.aggro() || body.health() < body.maxHealth())) {
            bar(canvas, x - 24, y - (sheet!=null?(knight?144:128):92), 48, 4, body.health() / (double) body.maxHealth(), DANGER);
            if (enemy.shielded()) Ui.outlinedCentered(canvas, "SHIELD", x, y - (sheet!=null?150:98), 2, Ui.GOLD);
        }
    }

    /** Collapse and edible remains are one sprite, so absorption cannot leave a second body behind. */
    public static boolean drawCorpse(Graphics2D g, RuinedOutpostGame.Corpse corpse, int x, int y) {
        var sheet=meleeSheet(corpse.kind());
        if (sheet==null) return false;
        boolean knight=corpse.kind()==CampaignEnemy.Kind.FALLEN_KNIGHT;
        meleeFrame(g,sheet,knight,(knight?5:1)+corpse.direction(),Math.min(11,(int)(corpse.age()*20)),x,y);
        return true;
    }

    private static java.awt.image.BufferedImage meleeSheet(CampaignEnemy.Kind kind) {
        return kind==CampaignEnemy.Kind.OUTPOST_GUARD?GUARD:kind==CampaignEnemy.Kind.FALLEN_KNIGHT?KNIGHT:null;
    }

    private static void meleeFrame(Graphics2D g,java.awt.image.BufferedImage sheet,boolean knight,int row,int frame,int x,int y) {
        // Export cells include pivot padding; never crop at the nominal 64px generation size.
        int cell=knight?92:88,foot=knight?76:72;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(sheet,x-cell,y-foot*2,x+cell,y+(cell-foot)*2,frame*cell,row*cell,(frame+1)*cell,(row+1)*cell,null);
    }

    public static void drawBoss(Graphics2D canvas, Guardian boss, int biome, int cx, int cy) {
        if (!boss.visible()) return;
        Graphics2D g = (Graphics2D) canvas.create();
        int x = (int) Math.round(boss.x()) - cx, y = (int) Math.round(boss.y()) - cy + 32;
        g.translate(x, y);
        g.scale(boss.renderScale(), boss.renderScale());
        g.setColor(new Color(5, 8, 15, 160));
        g.fillOval(-72, -8, 144, 38);
        Color color = boss.state() == Guardian.State.HURT ? TEXT : PALETTES[biome][4];
        g.setColor(color);
        if (biome == 1) {
            g.fillPolygon(new int[]{-38, -26, 22, 48, 30, -34},
                    new int[]{-14, -96, -106, -16, 12, 12}, 6);
            for (int side : new int[]{-1, 1}) {
                g.fillPolygon(new int[]{side * 18, side * 58, side * 88, side * 62},
                        new int[]{-60, -98, -92, -40}, 4);
                g.fillRect(side * 34 - 6, -120, 12, 44);
            }
            g.setColor(PALETTES[1][2]);
            g.fillRect(-10, -84, 16, 80);
        } else if (biome == 3) {
            g.fillPolygon(new int[]{0, 52, 40, -40, -52}, new int[]{-136, -80, -8, -8, -80}, 5);
            for (int side : new int[]{-1, 1}) {
                g.fillPolygon(new int[]{side * 42, side * 74, side * 88, side * 54},
                        new int[]{-86, -100, -34, -20}, 4);
                g.fillRect(side * 24 - 12, -14, 24, 34);
            }
            g.setColor(PALETTES[3][3]);
            g.fillPolygon(new int[]{0, 40, 0}, new int[]{-130, -74, -18}, 3);
            g.setColor(GOLD);
            g.fillPolygon(new int[]{0, 16, 0, -16}, new int[]{-90, -65, -38, -65}, 4);
        } else {
            g.fillRect(-42, -94, 84, 78);
            g.fillRect(-28, -130, 56, 40);
            g.fillRect(-38, -20, 26, 38);
            g.fillRect(12, -20, 26, 38);
            g.fillRect(-66, -96, 30, 58);
            g.fillRect(36, -96, 30, 58);
            g.setColor(PALETTES[biome][2]);
            g.fillRect(-28, -88, 56, 50);
            if (biome == 2) {
                g.setColor(PALETTES[2][3]);
                g.fillPolygon(new int[]{-80, -30, -30, -54, -80}, new int[]{-94, -94, -30, 8, -30}, 5);
                g.setColor(TEXT);
                g.fillRect(72, -142, 8, 128);
                g.fillRect(54, -30, 42, 8);
            } else {
                g.setColor(color);
                g.fillRect(-36, -142, 72, 12);
                g.fillRect(68, -82, 12, 110);
                g.fillRect(54, -88, 40, 30);
            }
        }
        g.setColor(boss.state() == Guardian.State.TELEGRAPH ? DANGER : GOLD);
        g.fillRect(-16, -112, 10, 6);
        g.fillRect(6, -112, 10, 6);
        g.dispose();
    }

    public static void drawHud(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        Ui.crisp(g);
        Player p = game.player();
        if (game.bossActive() && game.guardian().alive()) bossBar(g, game, width);
        tracker(g, game, width);
        drawMiniMap(g, game, width - 240 - 16, height - 196 - 16, 240, 196);
        String prompt = game.interactionPrompt();
        if (!prompt.isEmpty() && !game.nearCampaignHub()) prompt(g, game, prompt, 16, height - 16 - 60);
        boolean danger = game.bossActive() || game.scouts().stream().anyMatch(enemy -> enemy.alive() && enemy.aggro()
                && Math.hypot(enemy.x() - p.x(), enemy.y() - p.y()) < 700);
        if (!game.campaignNotice().isEmpty() && !danger) toast(g, game.campaignNotice(), width / 2, 108);
        if (game.nearCampaignHub()) campCard(g, game, 16, height - 16 - 214);
    }

    // ------------------------------------------------------------------ boss bar

    private static double ghost = 1, shown = 1;
    private static long ghostHoldUntil, lastFrame;
    private static Guardian ghostOwner;

    /** Ornate top-centre bar: emblem, name, health with a pale trail that drains after each hit, a 50% notch. */
    private static void bossBar(Graphics2D g, RuinedOutpostGame game, int width) {
        Guardian boss = game.guardian();
        double health = Math.max(0, boss.health() / (double) boss.maxHealth());
        long now = System.nanoTime();
        if (ghostOwner != boss) { ghostOwner = boss; ghost = shown = health; ghostHoldUntil = now; lastFrame = now; }
        double dt = Math.min(.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (health < shown) ghostHoldUntil = now + 450_000_000L;     // the trail waits, then drains
        shown = health;
        if (ghost < health) ghost = health;
        else if (now > ghostHoldUntil) ghost = Math.max(health, ghost - dt * .6);

        int w = 560, x0 = width / 2 - w / 2, y0 = 40;
        Ui.drawTitleCentered(g, boss.bossName().toUpperCase(java.util.Locale.ROOT), width / 2, 8, 2,
                boss.enraged() ? Ui.BLOOD_RAMP : Ui.GOLD_RAMP);
        int left = Ui.Piece.BOSS_LEFT.w * Ui.S, right = Ui.Piece.BOSS_RIGHT.w * Ui.S;
        for (int x = x0 + left; x < x0 + w - right; x += Ui.Piece.BOSS_MID.w * Ui.S) {
            int tile = Math.min(Ui.Piece.BOSS_MID.w * Ui.S, x0 + w - right - x);
            Ui.nine(g, Ui.Piece.BOSS_MID, x, y0, tile, Ui.Piece.BOSS_MID.h * Ui.S);
        }
        // trough between the rails
        int tx = x0 + 46, ty = y0 + 18, tw = w - 46 - right, th = 14;
        g.setColor(new Color(0x14101c));
        g.fillRect(tx, ty, tw, th);
        int ghostW = (int) Math.round(tw * ghost) / Ui.S * Ui.S, fillW = (int) Math.round(tw * health) / Ui.S * Ui.S;
        if (ghostW > fillW) { g.setColor(new Color(0xf4dcc0)); g.fillRect(tx + fillW, ty, ghostW - fillW, th); }
        boolean stagger = boss.state() == Guardian.State.RECOVER;
        Color base = stagger ? new Color(0x2cc6c8) : boss.enraged() ? new Color(0xcc5222) : new Color(0xb8323a);
        Color light = stagger ? new Color(0xcffff9) : boss.enraged() ? new Color(0xffc45e) : new Color(0xf16458);
        Color dark = stagger ? new Color(0x178a93) : boss.enraged() ? new Color(0x8c2c16) : new Color(0x7a1c26);
        if (fillW > 0) {
            g.setColor(base); g.fillRect(tx, ty, fillW, th);
            g.setColor(light); g.fillRect(tx, ty, fillW, 4);
            g.setColor(dark); g.fillRect(tx, ty + th - 4, fillW, 4);
        }
        int notch = tx + tw / 2 / Ui.S * Ui.S;
        g.setColor(Ui.INK);
        g.fillRect(notch, ty, Ui.S, th);
        Ui.piece(g, Ui.Piece.DIAMOND, notch - Ui.Piece.DIAMOND.w + 1, ty - 12);
        Ui.piece(g, Ui.Piece.BOSS_LEFT, x0, y0);
        Ui.piece(g, Ui.Piece.BOSS_RIGHT, x0 + w - right, y0);
        Ui.piece(g, game.biome() == 0 ? Ui.Piece.EMBLEM_WARDEN : Ui.Piece.EMBLEM_BRIAR, x0 + 8, y0 + 8);
    }

    // ------------------------------------------------------------------ tracker

    /** Top-right: chapter and region, the current goal, shards. No box: a soft dark plate and shadowed type. */
    private static void tracker(Graphics2D g, RuinedOutpostGame game, int width) {
        int right = width - 16;
        String goal = game.campaignObjective();
        var lines = Ui.wrap(goal, 300, 2);
        int plateH = 64 + lines.size() * 22 + 30;
        Ui.fade(g, width - 420, 60, 420, plateH, 0, 190);
        String area = Ui.roman(game.biome() + 1) + "  \u00b7  " + game.campaignArea().name();
        Ui.textRight(g, area, right, 86, 2, Ui.GOLD);
        for (int x = 0; x < 240; x += Ui.S) {
            if (x > 150 && (x / Ui.S) % 2 == 1) continue;
            g.setColor(new Color(0xc48a26));
            g.fillRect(right - Ui.S - x, 96, Ui.S, Ui.S);
        }
        int y = 122;
        for (int i = 0; i < lines.size(); i++) {
            Ui.textRight(g, lines.get(i), right, y, 2, Ui.TEXT);
            if (i == 0) Ui.icon(g, Ui.Icon.OBJECTIVE, right - Ui.width(lines.get(0), 2) - 32, y - 18);
            y += 22;
        }
        String shards = Integer.toString(game.shards());
        Ui.textRight(g, shards, right, y + 8, 2, Ui.GOLD);
        Ui.icon(g, Ui.Icon.SHARD, right - Ui.width(shards, 2) - 30, y - 10);
    }

    // ------------------------------------------------------------------ prompts, toasts, camp

    private static final java.util.regex.Pattern KEYED = java.util.regex.Pattern.compile("^([A-Z0-9])\\s+(.+)$");

    /** Bottom-left: a key cap and what it does; absorbing shows its progress under the words. */
    private static void prompt(Graphics2D g, RuinedOutpostGame game, String prompt, int x, int y) {
        var m = KEYED.matcher(prompt);
        String key = m.matches() ? m.group(1) : null, words = m.matches() ? m.group(2).trim() : prompt;
        int textW = Math.min(420, Ui.width(words, 2));
        int w = (key != null ? 44 : 0) + textW + 36, h = 52;
        Ui.nine(g, Ui.Piece.PANEL, x, y, w, h);
        int tx = x + 18;
        if (key != null) { Ui.keycap(g, key, tx, y + 12); tx += 44; }
        Ui.wrapped(g, words, tx, y + 32, 420, 2, 20, 1, Ui.GOLD_LIGHT);
        if (game.absorptionTarget() != null) Ui.bar(g, tx, y + 40, textW, 4, game.absorptionProgress(), Ui.GOLD, Ui.GOLD_LIGHT);
    }

    /** A short message centred near the top, in a panel sized to its words. */
    private static void toast(Graphics2D g, String message, int center, int y) {
        var lines = Ui.wrap(message, 560, 2);
        if (lines.size() > 3) lines = lines.subList(0, 3);
        int textW = 0;
        for (String line : lines) textW = Math.max(textW, Ui.width(line, 2));
        int w = textW + 48, h = 26 + lines.size() * 22;
        int x = center - w / 2 / Ui.S * Ui.S;
        Ui.nine(g, Ui.Piece.PANEL, x, y, w, h);
        for (int i = 0; i < lines.size(); i++) Ui.textCentered(g, lines.get(i), center, y + 30 + i * 22, 2, Ui.TEXT);
    }

    private static final String[] TRACKS = {"vitality", "capacity", "efficiency", "edge"};
    private static final String[][] TRACK_TEXT = {{"Vitality", "+20 HP"}, {"Capacity", "+10% ichor"},
            {"Efficiency", "slower drain"}, {"Edge", "stronger finisher"}};

    /** Camp upgrades: one row per track with its key, level pips and price. */
    private static void campCard(Graphics2D g, RuinedOutpostGame game, int x, int y) {
        int w = 470, h = 214;
        Ui.nine(g, Ui.Piece.CARD, x, y, w, h);
        Ui.drawTitle(g, "FIELD CAMP", x + 20, y + 14, 2, Ui.GOLD_RAMP);
        int hx = x + w - 20;
        hx -= Ui.width("Rest + save", 2);
        Ui.text(g, "Rest + save", hx, y + 34, 2, Ui.MUTED);
        hx -= Ui.keycap(g, "H", hx - 34, y + 16) + 12;
        hx -= Ui.width("Talk", 2);
        Ui.text(g, "Talk", hx, y + 34, 2, Ui.MUTED);
        Ui.keycap(g, "E", hx - 34, y + 16);
        for (int i = 0; i < TRACKS.length; i++) {
            int ry = y + 56 + i * 38;
            int level = game.upgradeLevel(TRACKS[i]), max = i == 3 ? 1 : 3;
            boolean affordable = level < max && game.shards() >= game.upgradeCost(TRACKS[i]);
            Ui.keycap(g, Integer.toString(i + 1), x + 20, ry);
            Ui.text(g, TRACK_TEXT[i][0], x + 60, ry + 19, 2, affordable ? Ui.TEXT : Ui.MUTED);
            Ui.text(g, TRACK_TEXT[i][1], x + 186, ry + 19, 2, Ui.DIM);
            for (int k = 0; k < max; k++) {
                g.setColor(Ui.INK);
                g.fillRect(x + 336 + k * 14, ry + 8, 10, 10);
                g.setColor(k < level ? Ui.TEAL : new Color(0x252037));
                g.fillRect(x + 338 + k * 14, ry + 10, 6, 6);
            }
            if (level >= max) Ui.textRight(g, "MAX", x + w - 20, ry + 19, 2, Ui.CYAN);
            else {
                String cost = Integer.toString(game.upgradeCost(TRACKS[i]));
                Ui.textRight(g, cost, x + w - 20, ry + 19, 2, affordable ? Ui.GOLD : Ui.DIM);
                Ui.icon(g, Ui.Icon.SHARD, x + w - 20 - Ui.width(cost, 2) - 28, ry + 2);
            }
        }
    }

    public static void drawDebugStatus(Graphics2D g, RuinedOutpostGame game) {
        if (!game.debugSession()) return;
        String label = "TEST SESSION";
        int w = Ui.width(label, 2) + 54;
        Ui.nine(g, Ui.Piece.TAG, 20, 150, w, 32);
        Ui.icon(g, Ui.Icon.FLASK, 26, 154);
        Ui.text(g, label, 54, 172, 2, Ui.AMBER);
    }

    // ------------------------------------------------------------------ minimap

    /** Shrunk floors by biome and size: the minimap and the full map each keep their own. */
    private static final java.util.Map<Long, java.awt.image.BufferedImage> THUMBS = new java.util.HashMap<>();

    /** The region's real baked floor, shrunk to the minimap's art size (progressive halving keeps it smooth). */
    static java.awt.image.BufferedImage thumbnail(int biome, int artW, int artH) {
        long key = ((long) biome << 40) | ((long) artW << 20) | artH;
        var cached = THUMBS.get(key);
        if (cached != null) return cached;
        var source = biome == 0 ? GROUND : FOREST_GROUND;
        if (source == null) return null;
        java.awt.image.BufferedImage step = source;
        int w = source.getWidth(), h = source.getHeight();
        while (w / 2 >= artW && h / 2 >= artH) {
            w /= 2; h /= 2;
            var half = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var hg = half.createGraphics();
            hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            hg.drawImage(step, 0, 0, w, h, null);
            hg.dispose();
            step = half;
        }
        var out = new java.awt.image.BufferedImage(artW, artH, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var og = out.createGraphics();
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        og.drawImage(step, 0, 0, artW, artH, null);
        og.dispose();
        // night floors are near black once shrunk; lift them so roads, water and clearings read
        for (int py = 0; py < artH; py++) for (int px = 0; px < artW; px++) {
            int rgb = out.getRGB(px, py), r = rgb >> 16 & 255, gr = rgb >> 8 & 255, b = rgb & 255;
            r = Math.min(255, (int) (r * 1.75 + 6)); gr = Math.min(255, (int) (gr * 1.75 + 6)); b = Math.min(255, (int) (b * 1.75 + 10));
            out.setRGB(px, py, r << 16 | gr << 8 | b);
        }
        THUMBS.put(key, out);
        return out;
    }

    /** Framed minimap: header, the real floor in miniature, walls, landmarks, camp, boss, passages and you. */
    static void drawMiniMap(Graphics2D g, RuinedOutpostGame game, int x, int y, int w, int h) {
        Ui.window(g, x, y, w, h);
        Ui.text(g, "REGION MAP", x + 16, y + 30, 2, Ui.MUTED);
        int capW = Ui.width("TAB", 2) + 14;
        Ui.keycap(g, "TAB", x + w - 16 - Math.max(26, capW), y + 12);
        int mx = x + 12, my = y + 44, mw = w - 24, mh = h - 44 - 12;
        mw = mw / Ui.S * Ui.S; mh = mh / Ui.S * Ui.S;
        mapArea(g, game, mx, my, mw, mh, false);
    }

    /** The map picture shared by the minimap and the full map. */
    private static void mapArea(Graphics2D g, RuinedOutpostGame game, int mx, int my, int mw, int mh, boolean large) {
        var area = game.campaignArea();
        var thumb = thumbnail(game.biome(), mw / Ui.S, mh / Ui.S);
        g.setColor(Ui.INK);
        g.fillRect(mx - Ui.S, my - Ui.S, mw + 2 * Ui.S, mh + 2 * Ui.S);
        if (thumb != null) g.drawImage(thumb, mx, my, mw, mh, null);
        else { g.setColor(PALETTES[game.biome()][0]); g.fillRect(mx, my, mw, mh); }
        double sx = mw / (double) area.width(), sy = mh / (double) area.height();
        g.setColor(new Color(0x2a2741));
        for (var obstacle : area.obstacles()) {
            int l = (int) Math.floor((obstacle.centerX() - obstacle.width() / 2) * sx / Ui.S) * Ui.S;
            int t = (int) Math.floor((obstacle.centerY() - obstacle.height() / 2) * sy / Ui.S) * Ui.S;
            int r = (int) Math.ceil((obstacle.centerX() + obstacle.width() / 2) * sx / Ui.S) * Ui.S;
            int b = (int) Math.ceil((obstacle.centerY() + obstacle.height() / 2) * sy / Ui.S) * Ui.S;
            g.fillRect(mx + l, my + t, Math.max(Ui.S, r - l), Math.max(Ui.S, b - t));
        }
        int dot = large ? 3 : 2;
        for (var landmark : area.landmarks()) {
            int lx = snap(mx + landmark.x() * sx), ly = snap(my + landmark.y() * sy);
            mark(g, lx, ly, dot, landmark.optional() ? Ui.GOLD : new Color(0xb8b09a));
        }
        for (var point : new CampaignWorld.Point[]{area.exit(), area.returnPortal()}) {
            int px = snap(mx + point.x() * sx), py = snap(my + point.y() * sy);
            mark(g, px, py, dot, new Color(0x7b8da4));
        }
        int hx = snap(mx + area.hub().x() * sx), hy = snap(my + area.hub().y() * sy);
        mark(g, hx, hy, dot + 1, new Color(0xf08a32));
        int bx = snap(mx + area.boss().x() * sx), by = snap(my + area.boss().y() * sy);
        diamond(g, bx, by, large ? 5 : 4, game.guardian().alive() ? Ui.DANGER : Ui.GOLD);
        int px = snap(mx + game.player().x() * sx), py = snap(my + game.player().y() * sy);
        boolean pulse = System.currentTimeMillis() / 400 % 2 == 0;
        if (pulse) { g.setColor(new Color(0x2cc6c8)); g.drawRect(px - 7, py - 7, 13, 13); g.drawRect(px - 6, py - 6, 11, 11); }
        mark(g, px, py, dot + 1, Ui.CYAN);
    }

    private static int snap(double v) { return (int) Math.floor(v / Ui.S) * Ui.S; }

    /** A square map mark `size` art pixels wide with an ink rim. */
    private static void mark(Graphics2D g, int cx, int cy, int size, Color color) {
        int half = size * Ui.S / 2 / Ui.S * Ui.S;
        g.setColor(Ui.INK);
        g.fillRect(cx - half - Ui.S, cy - half - Ui.S, size * Ui.S + 2 * Ui.S, size * Ui.S + 2 * Ui.S);
        g.setColor(color);
        g.fillRect(cx - half, cy - half, size * Ui.S, size * Ui.S);
    }

    private static void diamond(Graphics2D g, int cx, int cy, int r, Color color) {
        for (int pass = 0; pass < 2; pass++) {
            int rr = pass == 0 ? r + 1 : r;
            g.setColor(pass == 0 ? Ui.INK : color);
            for (int dy = -rr; dy <= rr; dy++) {
                int span = rr - Math.abs(dy);
                g.fillRect(cx - span * Ui.S, cy + dy * Ui.S, (2 * span + 1) * Ui.S, Ui.S);
            }
        }
    }

    /** TAB: the region map. The real floor framed on the left; regions, goal and a legend on the right. */
    public static void drawMap(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        Ui.crisp(g);
        Ui.veil(g, width, height, 238);
        var area = game.campaignArea();
        Ui.drawTitle(g, Ui.roman(game.biome() + 1) + "  " + area.name(), 36, 18, 2, Ui.GOLD_RAMP);
        int hx = width - 168;   // clear of the Menu button
        hx -= Ui.width(game.paused() ? "Paused" : "The world keeps moving", 2);
        Ui.text(g, game.paused() ? "Paused" : "The world keeps moving", hx, 42, 2, Ui.MUTED);
        hx -= 22 + Ui.width("Release", 2);
        Ui.text(g, "Release", hx, 42, 2, Ui.MUTED);
        Ui.keycap(g, "TAB", hx - 60, 24);
        int fx = 28, fy = 66, fw = 916, fh = 628;
        Ui.window(g, fx, fy, fw, fh);
        int mx = fx + 16, my = fy + 16, mw = (fw - 32) / Ui.S * Ui.S, mh = (fh - 32) / Ui.S * Ui.S;
        mapArea(g, game, mx, my, mw, mh, true);
        double sx = mw / (double) area.width(), sy = mh / (double) area.height();
        boolean firstOptional = true;
        for (var landmark : area.landmarks()) {
            int lx = snap(mx + landmark.x() * sx), ly = snap(my + landmark.y() * sy);
            Ui.outlinedCentered(g, landmark.name(), lx, ly - 12, 2, landmark.optional() ? Ui.GOLD : Ui.TEXT);
            if (landmark.optional()) Ui.outlinedCentered(g, firstOptional ? "memory" : "cache", lx, ly + 26, 2, Ui.MUTED);
            if (landmark.optional()) firstOptional = false;
        }
        int camp = snap(mx + area.hub().x() * sx), campY = snap(my + area.hub().y() * sy);
        Ui.outlinedCentered(g, "CAMP", camp, campY + 28, 2, new Color(0xf08a32));

        int cx = fx + fw + 20, cw = width - cx - 28;
        int y = fy;
        Ui.text(g, "REGIONS", cx, y + 16, 2, Ui.MUTED);
        y += 28;
        for (int biome = 0; biome < CampaignWorld.AREA_COUNT; biome++) {
            boolean here = biome == game.biome(), open = game.biomeUnlocked(biome);
            Ui.nine(g, here ? Ui.Piece.CARD : Ui.Piece.PANEL, cx, y, cw, 62);
            Ui.text(g, Ui.roman(biome + 1) + "  " + CampaignWorld.area(biome).name(), cx + 14, y + 26, 2,
                    here ? Ui.GOLD_LIGHT : open ? Ui.TEXT : Ui.DIM);
            Ui.text(g, here ? "You are here" : open ? "Open" : "Beyond the gate", cx + 14, y + 48, 2, here ? Ui.CYAN : Ui.MUTED);
            y += 72;
        }
        y += 8;
        Ui.text(g, "GOAL", cx, y + 16, 2, Ui.MUTED);
        String goal = game.campaignObjective();
        y = Ui.wrapped(g, goal, cx, y + 42, cw, 2, 22, 3, Ui.TEXT) + 10;
        Ui.text(g, "KEY", cx, y + 16, 2, Ui.MUTED);
        y += 30;
        Object[][] legend = {{Ui.CYAN, "You"}, {new Color(0xf08a32), "Camp"}, {Ui.DANGER, "Guardian"},
                {Ui.GOLD, "Memory, cache"}, {new Color(0xb8b09a), "Landmark"}, {new Color(0x7b8da4), "Passage"}};
        for (Object[] row : legend) {
            if (row[0] == Ui.DANGER) diamond(g, cx + 8, y + 8, 4, Ui.DANGER);
            else mark(g, cx + 8, y + 8, 3, (Color) row[0]);
            Ui.text(g, (String) row[1], cx + 30, y + 16, 2, Ui.TEXT);
            y += 28;
        }
    }

    private static final String[] TAGLINES = {"Where the last knights fell", "Where the roots drink ichor"};
    private static final int[][] AREA_RAMPS = {Ui.STEEL_RAMP, Ui.GOLD_RAMP};

    /**
     * The region title card: chapter, name, a flourish that unfurls, a tagline. It floats over play for about
     * three seconds and never stops the game (short loads want an overlay, not a loading screen).
     */
    public static void drawAreaCard(Graphics2D g, int biome, double t, int width, boolean reduced) {
        if (biome < 0 || biome >= CampaignWorld.AREA_COUNT || t >= 3.3) return;
        double alpha = Math.min(1, Math.min(t / .45, (3.3 - t) / .7));
        if (alpha <= 0) return;
        var card = (Graphics2D) g.create();
        card.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) alpha));
        Ui.crisp(card);
        int cx = width / 2, top = 168;
        for (int[] band : new int[][]{{top - 30, 12, 70}, {top - 18, 170, 180}, {top + 152, 12, 70}}) {   // soft top and bottom
            Ui.fade(card, 0, band[0], width / 2, band[1], 0, band[2]);
            Ui.fade(card, width / 2, band[0], width / 2, band[1], band[2], 0);
        }
        Ui.textCentered(card, "C H A P T E R   " + Ui.roman(biome + 1), cx, top + 6, 2, Ui.GOLD);
        Ui.drawTitleCentered(card, CampaignWorld.area(biome).name(), cx, top + 20, 4, AREA_RAMPS[Math.min(biome, AREA_RAMPS.length - 1)]);
        int open = reduced ? Ui.Piece.FLOURISH.w : (int) Math.min(Ui.Piece.FLOURISH.w, 12 + t * 200);
        var clip = (Graphics2D) card.create();
        clip.clipRect(cx - open, top + 88, open * 2, 36);
        Ui.piece(clip, Ui.Piece.FLOURISH, cx - Ui.Piece.FLOURISH.w, top + 90);
        clip.dispose();
        Ui.textCentered(card, TAGLINES[Math.min(biome, TAGLINES.length - 1)], cx, top + 140, 2, Ui.MUTED);
        card.dispose();
    }

    /** Over the animated title art: the tagline, any notice, control hints, and the veil under an open menu. */
    public static void drawTitleExtras(Graphics2D g, RuinedOutpostGame game, int width, int height, boolean menuOpen) {
        Ui.crisp(g);
        Ui.text(g, "ABSORB  \u00b7  TRANSFORM  \u00b7  CONQUER", 58, 276, 2, Ui.GOLD);
        if (!game.campaignNotice().isEmpty()) {
            var lines = Ui.wrap(game.campaignNotice(), 330, 2);
            Ui.nine(g, Ui.Piece.PANEL, 56, 524, 344, 24 + Math.min(3, lines.size()) * 22);
            for (int i = 0; i < lines.size() && i < 3; i++) Ui.text(g, lines.get(i), 74, 552 + i * 22, 2, Ui.GOLD_LIGHT);
        }
        int x = 58, y = height - 44;
        x += Ui.keycap(g, "\u2191", x, y) + 4;
        x += Ui.keycap(g, "\u2193", x, y) + 10;
        Ui.text(g, "Select", x, y + 19, 2, Ui.MUTED);
        x += Ui.width("Select", 2) + 24;
        x += Ui.keycap(g, "ENTER", x, y) + 10;
        Ui.text(g, "Confirm", x, y + 19, 2, Ui.MUTED);
        if (menuOpen) Ui.veil(g, width, height, 150);
    }

    public static void drawOverlay(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        drawOverlay(g, game, width, height, false);
    }

    public static void drawOverlay(Graphics2D g, RuinedOutpostGame game, int width, int height, boolean menuOpen) {
        boolean title = game.story().phase() == OutpostStory.Phase.PROLOGUE;
        boolean complete = game.story().phase() == OutpostStory.Phase.COMPLETE;
        if (!title && !complete && game.player().alive() && !game.paused())
            return;
        if (!title && !complete && game.player().alive()) {
            // paused: the world stays visible, dimmed, behind the menu (or this banner when focus was lost)
            Ui.veil(g, width, height, 170);
            if (!menuOpen) {
                Ui.nine(g, Ui.Piece.PANEL, width / 2 - 170, height / 2 - 40, 340, 84);
                Ui.drawTitleCentered(g, "PAUSED", width / 2, height / 2 - 26, 3, Ui.GOLD_RAMP);
                Ui.textCentered(g, "ESC TO RESUME", width / 2, height / 2 + 30, 2, Ui.MUTED);
            }
            return;
        }
        Ui.crisp(g);
        if (complete) { victory(g, game, width, height); return; }
        if (!title) { death(g, game, width, height); return; }
        // title fallback (only when the title art failed to load): the old text card, in pixel type
        Ui.veil(g, width, height, 228);
        Ui.window(g, 120, 106, width - 240, height - 212);
        Ui.drawTitleCentered(g, "BLOB TO BLADE", width / 2, 150, 4, Ui.CYAN_RAMP);
        Ui.textCentered(g, "ABSORB \u00b7 TRANSFORM \u00b7 CONQUER", width / 2, 250, 2, Ui.GOLD);
        String[] lines = OutpostStory.openingLines();
        for (int i = 0; i < lines.length; i++) Ui.textCentered(g, lines[i], width / 2, 300 + i * 26, 2, Ui.TEXT);
        Ui.textCentered(g, "ENTER  NEW JOURNEY", width / 2, height - 190, 2, Ui.CYAN);
        Ui.textCentered(g, game.saveAvailable() ? "C  CONTINUE FROM CAMP" : "NO SAVED JOURNEY", width / 2, height - 160, 2,
                game.saveAvailable() ? Ui.GOLD : Ui.MUTED);
        if (!game.campaignNotice().isEmpty()) Ui.textCentered(g, game.campaignNotice(), width / 2, height - 130, 2, Ui.GOLD_LIGHT);
    }

    /** Death: the world stays, darkened; a blood-red title, where you fell, and the one key that matters. */
    private static void death(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        Ui.veil(g, width, height, 205);
        int cx = width / 2;
        Ui.drawTitleCentered(g, "SCATTERED", cx, 186, 5, Ui.BLOOD_RAMP);
        Ui.drawTitleCentered(g, "NOT FINISHED", cx, 268, 2, Ui.BONE_RAMP);
        Ui.piece(g, Ui.Piece.FLOURISH, cx - Ui.Piece.FLOURISH.w, 316);
        Ui.textCentered(g, Ui.roman(game.biome() + 1) + "  \u00b7  " + game.campaignArea().name(), cx, 376, 2, Ui.GOLD);
        Ui.textCentered(g, "Your ichor returns to the stones. Recovered strength stays with you.", cx, 408, 2, Ui.TEXT);
        boolean bright = System.currentTimeMillis() / 600 % 2 == 0;
        String words = "Reform at your last camp";
        int w = 26 + 12 + Ui.width(words, 2), x = cx - w / 2 / Ui.S * Ui.S;
        Ui.keycap(g, "R", x, 470);
        Ui.text(g, words, x + 38, 489, 2, bright ? Ui.GOLD_LIGHT : Ui.GOLD);
    }

    /** Victory: the crown, the cleared region, what comes next, and how to go again. */
    private static void victory(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        Ui.veil(g, width, height, 210);
        int cx = width / 2;
        var crown = (Graphics2D) g.create();
        crown.drawImage(Ui.KIT, cx - 36, 112, cx + 36, 184, Ui.Icon.CROWN.x(), Ui.Icon.CROWN.y(),
                Ui.Icon.CROWN.x() + Ui.Icon.CELL, Ui.Icon.CROWN.y() + Ui.Icon.CELL, null);
        crown.dispose();
        Ui.drawTitleCentered(g, game.story().overlayTitle(), cx, 206, 4, Ui.GOLD_RAMP);
        Ui.piece(g, Ui.Piece.FLOURISH, cx - Ui.Piece.FLOURISH.w, 282);
        String[] lines = game.story().overlayLines();
        int y = 350;
        for (String line : lines) {
            if (line.startsWith("PRESS")) continue;
            Ui.textCentered(g, line, cx, y, 2, Ui.TEXT);
            y += 28;
        }
        boolean bright = System.currentTimeMillis() / 600 % 2 == 0;
        String words = "Run again";
        int w = 26 + 12 + Ui.width(words, 2), x = cx - w / 2 / Ui.S * Ui.S;
        Ui.keycap(g, "R", x, y + 40);
        Ui.text(g, words, x + 38, y + 59, 2, bright ? Ui.GOLD_LIGHT : Ui.GOLD);
    }

    private static void bar(Graphics2D g, int x, int y, int width, int height, double fraction, Color color) {
        g.setColor(INK);
        g.fillRect(x - 2, y - 2, width + 4, height + 4);
        g.setColor(new Color(58, 55, 61));
        g.fillRect(x, y, width, height);
        g.setColor(color);
        g.fillRect(x, y, (int) (width * Math.max(0, Math.min(1, fraction))), height);
    }


}
