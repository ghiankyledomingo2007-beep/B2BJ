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
            if (!CombatUi.cinematic && !game.opening() && !game.cutsceneActive() && !game.bossActive() && Math.hypot(game.player().x() - landmark.x(), game.player().y() - landmark.y()) < 280)
                JourneyHud.location(canvas, landmark.name(), x, y - 48, landmark.optional() ? Ui.GOLD : MenuTheme.IVORY);
        }
        int hx = (int) area.hub().x() - cx, hy = (int) area.hub().y() - cy;
        if (!baked) {
            canvas.setColor(new Color(51, 61, 60));
            canvas.fillOval(hx - 54, hy + 2, 108, 32);
            canvas.setColor(GOLD);
            canvas.drawOval(hx - 42, hy - 2, 84, 28);
        }
        if(!game.opening()&&!CombatUi.cinematic)JourneyHud.location(canvas, "Camp", hx, hy + 72, MenuTheme.IVORY);
        if (!baked) portal(canvas, area.exit(), cx, cy, "EAST GATE", !game.guardian().alive() ? GOLD : MUTED);
        else if (!CombatUi.cinematic) JourneyHud.location(canvas, game.biome() == 0 ? "East Gate" : "Rooted Stair", (int) area.exit().x() - cx,
                (int) area.exit().y() - cy - 316, !game.guardian().alive() ? Ui.GOLD : MenuTheme.IVORY);
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
            case EAST_GATE -> Math.min(cells-1,(int)(game.gateOpening()*cells));
            default -> Math.min(p.variant(), cells - 1);
        };
        int left = (int) Math.round(p.x()) - cx - kind.anchorX * 2;
        int top = (int) Math.round(p.y()) - cy - kind.anchorY * 2;
        cell2x(g, sheet, left, top, cell * kind.width, 0, kind.width, sheet.getHeight());
        switch (kind) {
            case CAMPFIRE -> fire(g, FIRE_CAMP, 30, p.x(), p.y() - 16, cx, cy, frame + phase);
            case BRAZIER -> {
                int rest = game.biome() == 0 ? SetPieces.restBrazier(p) : -1;
                boolean cold = SetPieces.arenaBrazier(p) && game.guardian().alive() && game.guardian().state() == Guardian.State.DORMANT
                        || rest >= 0 && !game.restLit(rest);
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
    static void cell2x(Graphics2D g, java.awt.image.BufferedImage sheet, int left, int top, int sx, int sy, int w, int h) {
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
        if (!CombatUi.cinematic) Ui.outlinedCentered(g, label, x, y - 100, 2, color);
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
        boolean knight=knightBody(enemy.kind());
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
                    :body.state()==Wisp.State.HURT?body.hurtFrame(8):marchFrame(body);
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
            case BRIARLING -> new Color(149, 116, 80);
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

    }

    /** Collapse and edible remains are one sprite, so absorption cannot leave a second body behind. */
    public static boolean drawCorpse(Graphics2D g, RuinedOutpostGame.Corpse corpse, int x, int y) {
        var sheet=meleeSheet(corpse.kind());
        if (sheet==null) return false;
        boolean knight=knightBody(corpse.kind());
        meleeFrame(g,sheet,knight,(knight?5:1)+corpse.direction(),Math.min(11,(int)(corpse.age()*20)),x,y);
        return true;
    }

    private static final java.util.Map<String,int[]> CORPSE_SPANS=new java.util.HashMap<>();
    /** The current death frame's painted extent from the body's x and feet (screen px): {left, right, bottom}.
     *  Its contact shadow lies along it. Null without a sheet. */
    static int[] corpseSpan(RuinedOutpostGame.Corpse corpse) {
        var sheet=meleeSheet(corpse.kind());
        if (sheet==null) return null;
        boolean knight=knightBody(corpse.kind());
        int cell=knight?92:88,foot=knight?76:72,row=(knight?5:1)+corpse.direction(),frame=Math.min(11,(int)(corpse.age()*20));
        return CORPSE_SPANS.computeIfAbsent(knight+":"+row+":"+frame,key->{
            int left=cell,right=-1,bottom=-1;
            for(int y=0;y<cell;y++)for(int x=0;x<cell;x++)if(sheet.getRGB(frame*cell+x,row*cell+y)>>>24!=0) {
                left=Math.min(left,x);right=Math.max(right,x);bottom=Math.max(bottom,y);
            }
            return right<0?new int[]{0,0,0}:new int[]{(left-cell/2)*2,(right+1-cell/2)*2,(bottom+1-foot)*2};
        });
    }

    private static final java.util.Map<Wisp,double[]> STRIDES=new java.util.WeakHashMap<>();
    /** Armoured walkers step with the ground they cover (one frame per 16 px, a full cycle per two strides), so the
     *  feet never slide or march on the spot; standing still holds the last step. */
    static int marchFrame(Wisp body) {return (int)(travelled(body)/16)%8;}
    /** Ground a body has covered since it was first drawn; asking twice in one frame adds nothing. */
    static double travelled(Wisp body) {
        double[] s=STRIDES.computeIfAbsent(body,b->new double[]{b.x(),b.y(),0});
        s[2]+=Math.min(40,Math.hypot(body.x()-s[0],body.y()-s[1]));s[0]=body.x();s[1]=body.y();
        return s[2];
    }

    private static java.awt.image.BufferedImage meleeSheet(CampaignEnemy.Kind kind) {
        return kind==CampaignEnemy.Kind.OUTPOST_GUARD?GUARD:knightBody(kind)?KNIGHT:null;
    }
    /** Outpost Remnants wear the Fallen Knight's body; their rules (64 HP, slime can hit them, Ichor) are their own. */
    static boolean knightBody(CampaignEnemy.Kind kind) {
        return kind==CampaignEnemy.Kind.FALLEN_KNIGHT||kind==CampaignEnemy.Kind.OUTPOST_SCOUT;
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

    public static void drawHud(Graphics2D g, RuinedOutpostGame game, int width, int height, boolean reducedEffects) {
        Ui.crisp(g);
        var hud=(Graphics2D)g.create();double scale=JourneyHud.scale(width,height);hud.scale(scale,scale);
        int hw=(int)Math.round(width/scale),hh=(int)Math.round(height/scale);
        Player p = game.player();
        if (game.bossActive() && game.guardian().alive()) bossBar(hud, game, hw);
        tracker(hud, game, hw);
        drawMiniMap(hud, game, hw - 240 - 16, hh - 196 - 16, 240, 196);
        String prompt = game.interactionPrompt();
        if (!prompt.isEmpty() && !game.nearCampaignHub()) prompt(hud, game, prompt, 16, hh - 16 - 60);
        boolean danger = game.bossActive() || game.scouts().stream().anyMatch(enemy -> enemy.alive() && enemy.aggro()
                && Math.hypot(enemy.x() - p.x(), enemy.y() - p.y()) < 700);
        if (!game.campaignNotice().isEmpty() && !danger) toast(hud, game.campaignNotice(), hw / 2, 184);
        hud.dispose();
        drawSage(g,game,width,height,reducedEffects);
    }

    /** Also painted during conversations, when the gameplay HUD is hidden. */
    static void drawSage(Graphics2D g,RuinedOutpostGame game,int width,int height,boolean reducedEffects) {
        if(game.wardenStepActive())return;
        if(game.wardenVictory()&&game.bark()!=OutpostBark.WARDEN_VICTORY) {
            if(game.bark()!=null)DialogueRenderer.drawExploration(g,game.barkLine(),game.barkElapsed(),game.barkRemaining(),width,height);
            return;
        }
        // Full dialogue owns Raphael's floating seal; don't draw a second one at the top.
        if(game.dialogue()!=null&&game.dialogueReady()&&game.dialogueLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE)return;
        Player p=game.player();
        boolean danger=game.bossActive()||game.scouts().stream().anyMatch(enemy->enemy.alive()&&enemy.aggro()
                &&Math.hypot(enemy.x()-p.x(),enemy.y()-p.y())<700);
        boolean speaking=game.bark()!=null&&(!danger||game.bark().combat())&&game.dialogue()==null&&p.alive();
        boolean sageSpeaking=speaking&&game.barkLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE;
        if(game.campaignMode()&&!game.story().blocksGameplay()&&p.alive()) {
            var voiceCanvas=(Graphics2D)g.create();
            if(game.bossActive()&&!game.bossIntroduction())voiceCanvas.translate(0,108);
            if(sageSpeaking)InnerVoiceOverlay.draw(voiceCanvas,game.barkLine().text(),game.barkElapsed(),game.barkRemaining(),
                    GameAudio.dialogueLevel(),width,height,reducedEffects,game.sageTime());
            else InnerVoiceOverlay.drawPresence(voiceCanvas,game.sageTime(),GameAudio.dialogueLevel(),width,reducedEffects,
                    game.dialogue()!=null&&game.dialogueLine().speaker()==OutpostDialogue.Speaker.INNER_VOICE);
            voiceCanvas.dispose();
        }
        if(speaking&&!sageSpeaking)DialogueRenderer.drawExploration(g,game.barkLine(),game.barkElapsed(),game.barkRemaining(),width,height);
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

        int w=560,x=width/2-w/2,y=48;
        boolean stagger=boss.coreExposed();
        Color base=stagger?Ui.TEAL:boss.enraged()?new Color(0xcc5222):new Color(0xb8323a);
        JourneyHud.center(g,boss.bossName(),width/2,32,Ui.menuFont(23).deriveFont(java.awt.Font.BOLD),MenuTheme.IVORY);
        JourneyHud.frame(g,x,y,w,30);
        JourneyHud.meter(g,x+16,y+8,w-32,14,ghost,new Color(0x9e8872),new Color(0xf4dcc0));
        var current=(Graphics2D)g.create();current.clipRect(x+16,y+8,(int)Math.ceil((w-32)*health),14);
        JourneyHud.meter(current,x+16,y+8,w-32,14,health,base,stagger?new Color(0xcffff9):new Color(0xf16458));current.dispose();
        g.setColor(MenuTheme.GOLD);MenuTheme.star(g,width/2.,y+1,4);
        JourneyHud.center(g,stagger?(game.biome()==1?"Heart exposed":"Core exposed"):(game.biome()==1?"Roots woven":"Armor closed"),width/2,y+48,JourneyHud.SMALL,stagger?Ui.CYAN:MenuTheme.MUTED);
    }

    // ------------------------------------------------------------------ tracker

    /** Shared ivory/gold mission plaque with wrapped live objectives and currency. */
    private static void tracker(Graphics2D graphics, RuinedOutpostGame game, int width) {
        var g=(Graphics2D)graphics.create();int x=width-292,y=66,w=276;
        var lines=JourneyHud.wrap(g,game.campaignObjective(),JourneyHud.BODY,244);
        int h=80+lines.size()*21;
        g.setColor(new Color(5,15,23,195));g.fillRect(x+22,y,w-22,h);
        g.setColor(JourneyHud.GOLD);g.setStroke(new java.awt.BasicStroke(1.1f));
        g.drawLine(x+20,y,x+w,y);g.drawLine(x+20,y+h,x+w,y+h);
        MenuTheme.star(g,x+w,y,4);MenuTheme.star(g,x+w,y+h,4);
        var badge=new java.awt.Polygon(new int[]{x+12,x+31,x+12,x-7},new int[]{y-1,y+18,y+37,y+18},4);
        g.setColor(JourneyHud.INK);g.fill(badge);g.setColor(JourneyHud.GOLD);g.draw(badge);
        JourneyHud.center(g,"!",x+12,y+25,Ui.heading(23),JourneyHud.GOLD);
        String area=Ui.roman(game.biome()+1)+" · "+game.campaignArea().name();
        JourneyHud.text(g,area,x+42,y+24,JourneyHud.SMALL,JourneyHud.GOLD);
        g.setColor(Ui.CYAN);g.fillPolygon(new int[]{x+13,x+20,x+13,x+6},new int[]{y+46,y+53,y+60,y+53},4);
        for(int i=0;i<lines.size();i++)JourneyHud.text(g,lines.get(i),x+28,y+59+i*21,JourneyHud.BODY,MenuTheme.IVORY);
        String shards=Integer.toString(game.shards());int bottom=y+65+lines.size()*21;
        JourneyHud.right(g,shards,x+w-20,bottom,JourneyHud.BODY,Ui.GOLD_LIGHT);
        int coin=x+w-32-g.getFontMetrics(JourneyHud.BODY).stringWidth(shards);
        g.setColor(Ui.GOLD_LIGHT);g.fillPolygon(new int[]{coin-4,coin+1,coin+5,coin+5,coin,coin-4},new int[]{bottom-12,bottom-15,bottom-12,bottom-3,bottom,bottom-3},6);
        g.dispose();
    }

    // ------------------------------------------------------------------ prompts, toasts, camp

    private static final java.util.regex.Pattern KEYED = java.util.regex.Pattern.compile("^([A-Z0-9])\\s+(.+)$");

    /** Bottom-left: a key cap and what it does; absorbing shows its progress under the words. */
    private static void prompt(Graphics2D graphics, RuinedOutpostGame game, String prompt, int x, int y) {
        var g=(Graphics2D)graphics.create();var m=KEYED.matcher(prompt);
        String key=m.matches()?m.group(1):null,words=m.matches()?m.group(2).trim():prompt;
        var lines=JourneyHud.wrap(g,words,JourneyHud.BODY,key==null?260:218);
        int h=30+lines.size()*21;y+=60-h;
        JourneyHud.frame(g,x,y,300,h);int tx=x+18;
        if(key!=null){JourneyHud.frame(g,tx,y+14,28,28);JourneyHud.center(g,key,tx+14,y+34,JourneyHud.LABEL,MenuTheme.IVORY);tx+=40;}
        for(int i=0;i<lines.size();i++)JourneyHud.text(g,lines.get(i),tx,y+28+i*21,JourneyHud.BODY,MenuTheme.IVORY);
        if(game.absorptionTarget()!=null)JourneyHud.meter(g,tx,y+h-10,282-(tx-x),4,game.absorptionProgress(),Ui.TEAL,Ui.CYAN);
        g.dispose();
    }

    private static void toast(Graphics2D graphics,String message,int center,int y) {
        var g=(Graphics2D)graphics.create();var lines=JourneyHud.wrap(g,message,JourneyHud.BODY,520);
        int w=0;for(String line:lines)w=Math.max(w,g.getFontMetrics(JourneyHud.BODY).stringWidth(line));w+=40;
        JourneyHud.frame(g,center-w/2,y,w,22+lines.size()*22);
        for(int i=0;i<lines.size();i++)JourneyHud.center(g,lines.get(i),center,y+27+i*22,JourneyHud.BODY,MenuTheme.IVORY);
        g.dispose();
    }

    public static void drawDebugStatus(Graphics2D graphics,RuinedOutpostGame game,int width,int height) {
        if(!game.debugSession())return;
        var g=(Graphics2D)graphics.create();double scale=JourneyHud.scale(width,height);g.scale(scale,scale);
        JourneyHud.frame(g,20,262,190,32);TitleIcons.draw(g,Ui.Icon.FLASK,28,266,24,false,true);
        JourneyHud.text(g,"Test session",60,284,JourneyHud.SMALL,MenuTheme.MUTED);
        g.dispose();
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
        JourneyHud.frame(g,x,y,w,h);
        JourneyHud.text(g,"Region map",x+14,y+27,JourneyHud.SMALL,MenuTheme.IVORY);
        JourneyHud.frame(g,x+w-54,y+9,42,24);
        JourneyHud.center(g,"TAB",x+w-33,y+26,JourneyHud.SMALL,MenuTheme.IVORY);
        g.setColor(JourneyHud.GOLD);g.drawLine(x+12,y+38,x+w-12,y+38);
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
        // Lit rest braziers glow like the camp; cold ones are not shown until found.
        var rests = FieldObjects.rests(game.biome());
        for (int i = 0; i < rests.size(); i++) if (game.restLit(i))
            mark(g, snap(mx + rests.get(i).x() * sx), snap(my + rests.get(i).y() * sy), dot, new Color(0xf08a32));
        for(var spot:FieldObjects.all(game.biome()))if(game.survivorKnown(spot)&&!game.fieldDone(spot)) {
            int fx=snap(mx+spot.x()*sx),fy=snap(my+spot.y()*sy);
            diamond(g,fx,fy,large?4:3,Ui.CYAN);
            if(large)Ui.outlinedCentered(g,"HELP",fx,fy+24,1,Ui.CYAN);
        }
        if(game.carried()!=null) {
            var target=game.carried().kind()==FieldObjects.Kind.KEY?FieldObjects.find(game.biome(),"cache_0"):null;
            if(target!=null) {
                int fx=snap(mx+target.x()*sx),fy=snap(my+target.y()*sy);
                diamond(g,fx,fy,large?4:3,Ui.GOLD);
                if(large)Ui.outlinedCentered(g,"CACHE",fx,fy+24,1,Ui.GOLD);
            }
        }
        int hx = snap(mx + area.hub().x() * sx), hy = snap(my + area.hub().y() * sy);
        mark(g, hx, hy, dot + 1, new Color(0xf08a32));
        int bx = snap(mx + area.boss().x() * sx), by = snap(my + area.boss().y() * sy);
        diamond(g, bx, by, large ? 5 : 4, game.guardian().alive() ? Ui.DANGER : Ui.GOLD);
        if(game.biome()==0) {
            var goal=game.campaignObjectivePoint();
            int gx=snap(mx+goal.x()*sx),gy=snap(my+goal.y()*sy),r=large?12:8;
            objectiveMark(g,gx,gy,r);
        }
        int px = snap(mx + game.player().x() * sx), py = snap(my + game.player().y() * sy);
        boolean pulse = System.currentTimeMillis() / 400 % 2 == 0;
        if (pulse) { g.setColor(new Color(0x2cc6c8)); g.drawRect(px - 7, py - 7, 13, 13); g.drawRect(px - 6, py - 6, 11, 11); }
        mark(g, px, py, dot + 1, Ui.CYAN);
    }

    private static int snap(double v) { return (int) Math.floor(v / Ui.S) * Ui.S; }

    private static void objectiveMark(Graphics2D g,int x,int y,int radius) {
        g.setColor(Ui.TEXT);
        g.fillRect(x-radius,y-radius,radius*2,Ui.S);
        g.fillRect(x-radius,y+radius-Ui.S,radius*2,Ui.S);
        g.fillRect(x-radius,y-radius,Ui.S,radius*2);
        g.fillRect(x+radius-Ui.S,y-radius,Ui.S,radius*2);
    }

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
        g=(Graphics2D)g.create();
        double scale=TitleScreen.menuScale(width,height);g.scale(scale,scale);
        width=(int)Math.round(width/scale);height=(int)Math.round(height/scale);
        Ui.crisp(g);
        Ui.veil(g, width, height, 238);
        var area = game.campaignArea();
        JourneyHud.text(g,Ui.roman(game.biome()+1)+"  "+area.name(),36,42,Ui.menuFont(26),MenuTheme.IVORY);
        JourneyHud.right(g,game.paused()?"Release Tab to close · Paused":"Release Tab to close · World stays live",
                width-168,42,Ui.heading(15),MenuTheme.MUTED);
        int fx = 28, fy = 66, fw = width-368, fh = height-92;
        JourneyHud.frame(g, fx, fy, fw, fh);
        int mx = fx + 16, my = fy + 16, mw = (fw - 32) / Ui.S * Ui.S, mh = (fh - 32) / Ui.S * Ui.S;
        mapArea(g, game, mx, my, mw, mh, true);
        double sx = mw / (double) area.width(), sy = mh / (double) area.height();
        boolean firstOptional = true;
        for (var landmark : area.landmarks()) {
            int lx = snap(mx + landmark.x() * sx), ly = snap(my + landmark.y() * sy);
            JourneyHud.center(g,landmark.name(),lx+1,ly-11,JourneyHud.SMALL,Ui.INK);
            JourneyHud.center(g,landmark.name(),lx,ly-12,JourneyHud.SMALL,landmark.optional()?JourneyHud.GOLD:MenuTheme.IVORY);
            if (landmark.optional()) JourneyHud.center(g,firstOptional?"memory":"cache",lx,ly+26,Ui.heading(14),MenuTheme.MUTED);
            if (landmark.optional()) firstOptional = false;
        }
        int cx = fx + fw + 20, cw = width - cx - 28;
        int y = fy;
        JourneyHud.text(g,"Regions",cx,y+16,JourneyHud.LABEL,JourneyHud.GOLD);
        y += 28;
        for (int biome = 0; biome < CampaignWorld.AREA_COUNT; biome++) {
            boolean here = biome == game.biome(), open = game.biomeUnlocked(biome);
            JourneyHud.frame(g,cx,y,cw,62);
            JourneyHud.text(g,Ui.roman(biome+1)+"  "+CampaignWorld.area(biome).name(),cx+14,y+26,JourneyHud.SMALL,
                    here?JourneyHud.GOLD:open?MenuTheme.IVORY:MenuTheme.MUTED);
            JourneyHud.text(g,here?"You are here":open?"Open":"Beyond the gate",cx+14,y+48,JourneyHud.BODY,here?Ui.CYAN:MenuTheme.MUTED);
            y += 72;
        }
        y += 8;
        JourneyHud.text(g,"Goal",cx,y+16,JourneyHud.LABEL,JourneyHud.GOLD);
        String goal = game.campaignObjective();
        y+=42;
        for(String line:JourneyHud.wrap(g,goal,JourneyHud.BODY,cw)) {
            JourneyHud.text(g,line,cx,y,JourneyHud.BODY,MenuTheme.IVORY);y+=22;
        }
        y+=10;
        JourneyHud.text(g,"Key",cx,y+16,JourneyHud.LABEL,JourneyHud.GOLD);
        y += 30;
        Object[][] legend = {{Ui.CYAN, "You"}, {new Color(0xf08a32), "Camp"}, {Ui.DANGER, "Guardian"},
                {Ui.GOLD, "Memory, cache"}, {new Color(0xb8b09a), "Landmark"}, {new Color(0x7b8da4), "Passage"}};
        for (Object[] row : legend) {
            if (row[0] == Ui.DANGER) diamond(g, cx + 8, y + 8, 4, Ui.DANGER);
            else mark(g, cx + 8, y + 8, 3, (Color) row[0]);
            JourneyHud.text(g,(String)row[1],cx+30,y+16,JourneyHud.BODY,MenuTheme.IVORY);
            y += 28;
        }
        if(game.biome()==0) {
            objectiveMark(g,cx+8,y+8,8);
            JourneyHud.text(g,"Next stop",cx+30,y+16,JourneyHud.BODY,MenuTheme.IVORY);
        }
        g.dispose();
    }

    private static final String[] TAGLINES = {"Where the last knights fell", "Where the roots drink ichor"};

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
        JourneyHud.center(card,"Chapter "+Ui.roman(biome+1),cx,top+6,JourneyHud.LABEL,JourneyHud.GOLD);
        JourneyHud.center(card,CampaignWorld.area(biome).name(),cx,top+65,Ui.menuFont(44),MenuTheme.IVORY);
        int open=reduced?140:(int)Math.min(140,12+t*150);
        card.setColor(JourneyHud.GOLD);card.drawLine(cx-open,top+95,cx-12,top+95);card.drawLine(cx+12,top+95,cx+open,top+95);
        MenuTheme.star(card,cx,top+95,5);
        JourneyHud.center(card,TAGLINES[Math.min(biome,TAGLINES.length-1)],cx,top+130,Ui.heading(18),MenuTheme.MUTED);
        card.dispose();
    }

    /** Save notices and the veil under an open title menu. */
    public static void drawTitleExtras(Graphics2D g, RuinedOutpostGame game, int width, int height, boolean menuOpen) {
        Ui.crisp(g);
        if (!game.campaignNotice().isEmpty()) {
            var notice=(Graphics2D)g.create();double scale=TitleScreen.menuScale(width,height);notice.scale(scale,scale);
            var lines = JourneyHud.wrap(notice,game.campaignNotice(),JourneyHud.BODY,312);
            JourneyHud.frame(notice,68,604,344,24+Math.min(3,lines.size())*22);
            for(int i=0;i<lines.size()&&i<3;i++)JourneyHud.text(notice,lines.get(i),84,632+i*22,JourneyHud.BODY,JourneyHud.GOLD);
            notice.dispose();
        }
        if (menuOpen) Ui.veil(g, width, height, 150);
    }

    public static void drawOverlay(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        drawOverlay(g, game, width, height, false);
    }

    public static void drawOverlay(Graphics2D g, RuinedOutpostGame game, int width, int height, boolean menuOpen) {
        g=(Graphics2D)g.create();
        double scale=TitleScreen.menuScale(width,height);g.scale(scale,scale);
        width=(int)Math.round(width/scale);height=(int)Math.round(height/scale);
        try {
        boolean title = game.story().phase() == OutpostStory.Phase.PROLOGUE;
        boolean complete = game.story().phase() == OutpostStory.Phase.COMPLETE;
        if (!title && !complete && game.player().alive() && !game.paused())
            return;
        if (!title && !complete && game.player().alive()) {
            // paused: the world stays visible, dimmed, behind the menu (or this banner when focus was lost)
            Ui.veil(g, width, height, 170);
            if (!menuOpen) {
                JourneyHud.frame(g,width/2-180,height/2-60,360,120);
                JourneyHud.center(g,"Paused",width/2,height/2-3,Ui.menuFont(34),MenuTheme.IVORY);
                JourneyHud.center(g,"Esc to resume",width/2,height/2+32,Ui.heading(17),MenuTheme.MUTED);
            }
            return;
        }
        Ui.crisp(g);
        if (complete) { victory(g, game, width, height); return; }
        if (!title) { death(g, game, width, height); return; }
        // The fallback keeps the same identity even when the title art is missing.
        Ui.veil(g, width, height, 228);
        JourneyHud.frame(g,120,106,width-240,height-212);
        JourneyHud.center(g,"Blob to Blade",width/2,210,Ui.menuFont(46),MenuTheme.IVORY);
        String[] lines = OutpostStory.openingLines();
        for(int i=0;i<lines.length;i++)JourneyHud.center(g,lines[i],width/2,300+i*26,Ui.heading(18),MenuTheme.IVORY);
        JourneyHud.center(g,"Enter · New journey",width/2,height-190,JourneyHud.LABEL,Ui.CYAN);
        JourneyHud.center(g,game.saveAvailable()?"C · Continue from camp":"No saved journey",width/2,height-160,JourneyHud.BODY,MenuTheme.MUTED);
        if(!game.campaignNotice().isEmpty())JourneyHud.center(g,game.campaignNotice(),width/2,height-130,JourneyHud.BODY,JourneyHud.GOLD);
        } finally {g.dispose();}
    }

    /** Death: the world stays, darkened; a blood-red title, where you fell, and the one key that matters. */
    private static void death(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        Ui.veil(g, width, height, 205);
        int cx=width/2,cy=height/2;
        JourneyHud.center(g,"Scattered",cx,cy-90,Ui.menuFont(54),new Color(0xd7aaa3));
        JourneyHud.center(g,"Not finished",cx,cy-44,Ui.menuFont(23),MenuTheme.IVORY);
        g.setColor(JourneyHud.GOLD);MenuTheme.star(g,cx,cy-6,6);
        JourneyHud.center(g,Ui.roman(game.biome()+1)+" · "+game.campaignArea().name(),cx,cy+38,JourneyHud.LABEL,JourneyHud.GOLD);
        JourneyHud.center(g,"Your ichor returns to the stones. Recovered strength stays with you.",cx,cy+74,Ui.heading(18),MenuTheme.MUTED);
        JourneyHud.center(g,"R · Reform at your last camp",cx,cy+148,Ui.menuFont(20),MenuTheme.IVORY);
    }

    /** Victory: the crown, the cleared region, what comes next, and how to go again. */
    private static void victory(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        Ui.veil(g, width, height, 210);
        int cx=width/2,cy=height/2;
        TitleIcons.draw(g,Ui.Icon.CROWN,cx-32,cy-224,64,true,true);
        JourneyHud.center(g,game.story().overlayTitle(),cx,cy-106,Ui.menuFont(40),MenuTheme.IVORY);
        g.setColor(JourneyHud.GOLD);MenuTheme.star(g,cx,cy-64,6);
        String[] lines = game.story().overlayLines();
        int y=cy-8;
        for (String line : lines) {
            if (line.startsWith("PRESS")) continue;
            JourneyHud.center(g,line,cx,y,Ui.heading(18),MenuTheme.IVORY);
            y += 28;
        }
        JourneyHud.center(g,"R · Run again",cx,y+70,Ui.menuFont(20),JourneyHud.GOLD);
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
