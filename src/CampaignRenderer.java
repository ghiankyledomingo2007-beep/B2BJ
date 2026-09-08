import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Original Java2D placeholders. Biome geometry comes from the collision map. */
public final class CampaignRenderer {
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
        for (var bank : game.map().banks()) {
            int x = (int) (bank.centerX() - bank.width() / 2) - cx;
            int y = (int) (bank.centerY() - bank.height() / 2) - cy;
            canvas.setColor(INK);
            canvas.fillRect(x, y, (int) bank.width(), (int) bank.height());
        }
        for (var obstacle : game.map().barriers()) {
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
            canvas.drawRect(x - 10, y - 8, 20, 16);
            canvas.fillRect(x - 2, y - 18, 4, 8);
            if (Math.hypot(game.player().x() - landmark.x(), game.player().y() - landmark.y()) < 280)
                centered(canvas, landmark.name(), x, y - 28, 13, landmark.optional() ? GOLD : MUTED);
        }
        int hx = (int) area.hub().x() - cx, hy = (int) area.hub().y() - cy;
        canvas.setColor(new Color(51, 61, 60));
        canvas.fillOval(hx - 54, hy + 2, 108, 32);
        canvas.setColor(GOLD);
        canvas.drawOval(hx - 42, hy - 2, 84, 28);
        drawNpc(canvas, hx + 86, hy - 18, game.biome());
        centered(canvas, CampaignStory.npcName(game.biome()), hx + 86, hy - 94, 14, GOLD);
        centered(canvas, "CAMP", hx, hy + 72, 12, MUTED);
        portal(canvas, area.exit(), cx, cy, game.biome() == 3 ? "THE RIFT" : "NEXT REGION",
                !game.guardian().alive() ? GOLD : MUTED);
        if (game.biome() > 0) portal(canvas, area.returnPortal(), cx, cy, "RETURN", CYAN);
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
        centered(g, label, x, y - 100, 13, color);
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
        if (!body.visible()) return;
        Graphics2D g = (Graphics2D) canvas.create();
        int x = (int) Math.round(body.x()) - cx, y = (int) Math.round(body.y()) - cy + 22;
        g.translate(x, y);
        g.scale(body.renderScale(), body.renderScale());
        g.setColor(new Color(5, 8, 16, 140));
        g.fillOval(-28, -6, 56, 18);
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
        g.dispose();
        if (body.alive() && (body.aggro() || body.health() < body.maxHealth())) {
            bar(canvas, x - 24, y - 92, 48, 4, body.health() / (double) body.maxHealth(), DANGER);
            if (enemy.shielded()) centered(canvas, "SHIELD", x, y - 98, 10, GOLD);
        }
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
        Player p = game.player();
        int right = width - 354;
        panel(g, right, 18, 336, 136);
        text(g, (game.biome() + 1) + " / 4   " + game.campaignArea().name(), right + 14, 40, 14, GOLD);
        text(g, p.bladeForm() ? "BLADE FORM" : "SLIME FORM", right + 14, 61, 12, CYAN);
        wrapped(g, game.campaignStory().objective(game.biome(), !game.guardian().alive()),
                right + 14, 85, 306, 13, 19, 3, TEXT);
        text(g, "SHARDS " + game.shards(), right + 14, 141, 12, GOLD);
        if (game.bossActive() && game.guardian().alive()) {
            panel(g, width / 2 - 250, 90, 500, 57);
            centered(g, game.guardian().bossName(), width / 2, 113, 16, GOLD);
            bar(g, width / 2 - 232, 127, 464, 8,
                    game.guardian().health() / (double) game.guardian().maxHealth(), DANGER);
        }

        miniMap(g, game, width - 254, height - 196, 236, 170, false);
        String prompt = game.interactionPrompt();
        if (!prompt.isEmpty()) {
            panel(g, 18, height - 108, 398, 72);
            wrapped(g, prompt, 32, height - 84, 370, 14, 18, 3, GOLD);
            if(game.absorptionTarget()!=null)bar(g,32,height-47,370,4,game.absorptionProgress(),GOLD);
        }
        boolean danger = game.bossActive() || game.scouts().stream().anyMatch(enemy -> enemy.alive() && enemy.aggro()
                && Math.hypot(enemy.x() - p.x(), enemy.y() - p.y()) < 700);
        if (!game.campaignNotice().isEmpty() && !danger) {
            int noticeHeight = 20 + Math.min(4, 1 + game.campaignNotice().length() / 70) * 19;
            panel(g, 352, 94, Math.max(250, width - 726), noticeHeight);
            wrapped(g, game.campaignNotice(), 366, 117, Math.max(222, width - 754), 13, 19, 4, TEXT);
        }
        if (game.nearCampaignHub() && !game.campaignStory().dialogueOpen()) {
            panel(g, 18, height - 284, 440, 170);
            text(g, "CAMP  E TALK / H REST + SAVE", 32, height - 258, 14, GOLD);
            String[] keys = {"vitality", "capacity", "efficiency", "edge"};
            String[] labels = {"VITALITY  +1 HEALTH", "CAPACITY  +10% ICHOR", "EFFICIENCY  SLOWER DRAIN", "EDGE  FOURTH BLADE HIT"};
            for (int i = 0; i < keys.length; i++) {
                int level = game.upgradeLevel(keys[i]), max = i == 3 ? 1 : 3;
                String cost = level == max ? "MAX" : game.upgradeCost(keys[i]) + " SHARDS";
                text(g, (i + 1) + " " + labels[i], 32, height - 229 + i * 25, 12, TEXT);
                text(g, level + "/" + max + "  " + cost, 298, height - 229 + i * 25, 12,
                        level < max && game.shards() >= game.upgradeCost(keys[i]) ? GOLD : MUTED);
            }
        }
    }

    public static void drawDebugStatus(Graphics2D g, RuinedOutpostGame game) {
        if (!game.debugSession()) return;
        panel(g, 18, 152, 272, 47);
        text(g, "TEST SESSION", 32, 172, 13, GOLD);
        text(g, "NORMAL SAVE PROTECTED", 32, 190, 11, TEXT);
    }

    public static void drawMap(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        g.setColor(new Color(5, 8, 15, 245));
        g.fillRect(0, 0, width, height);
        text(g, "THE FALLEN KINGDOM", 40, 43, 24, GOLD);
        text(g, "RELEASE TAB TO CLOSE  /  " + (game.paused() ? "PAUSED" : "WORLD REMAINS LIVE"),
                width - 476, 40, 12, MUTED);
        int tabWidth = (width - 92) / 4;
        for (int biome = 0; biome < 4; biome++) {
            int x = 40 + biome * (tabWidth + 4);
            panel(g, x, 61, tabWidth, 53);
            text(g, CampaignWorld.area(biome).name(), x + 10, 82, 13,
                    biome == game.biome() ? CYAN : game.biomeUnlocked(biome) ? TEXT : MUTED);
            text(g, biome == game.biome() ? "YOU ARE HERE" : game.biomeUnlocked(biome) ? "OPEN" : "BEYOND THE GATE",
                    x + 10, 102, 11, biome == game.biome() ? CYAN : MUTED);
        }
        miniMap(g, game, 40, 128, width - 80, height - 200, true);
        text(g, "CYAN: YOU   GOLD: CAMP / MEMORY / CACHE   DIAMOND: BOSS   SQUARE: PASSAGE",
                44, height - 44, 12, MUTED);
        text(g, "OPTIONAL: " + game.campaignStory().questObjective(game.biome()), 44, height - 20, 13, GOLD);
    }

    private static void miniMap(Graphics2D g, RuinedOutpostGame game, int x, int y, int width, int height,
            boolean expanded) {
        panel(g, x, y, width, height);
        var area = game.campaignArea();
        int inset = expanded ? 38 : 10;
        int ox = x + inset, oy = y + (expanded ? 18 : 30);
        int mw = width - inset * 2, mh = height - (expanded ? 40 : 44);
        double sx = mw / (double) area.width(), sy = mh / (double) area.height();
        if (!expanded) text(g, "REGION MAP                  TAB", x + 10, y + 18, 10, MUTED);
        g.setColor(PALETTES[game.biome()][0]);
        g.fillRect(ox, oy, mw, mh);
        for (int ty = 0; ty < game.map().heightInTiles(); ty += 2) {
            for (int tx = 0; tx < game.map().widthInTiles(); tx += 2) {
                if (game.map().tileMask(tx, ty) == 0) continue;
                g.setColor(PALETTES[game.biome()][1]);
                g.fillRect(ox + (int) (tx * 64 * sx), oy + (int) (ty * 64 * sy),
                        Math.max(1, (int) Math.ceil(128 * sx)), Math.max(1, (int) Math.ceil(128 * sy)));
            }
        }
        g.setColor(PALETTES[game.biome()][3]);
        for (var obstacle : area.obstacles()) {
            int left = (int) Math.floor((obstacle.centerX() - obstacle.width() / 2) * sx);
            int top = (int) Math.floor((obstacle.centerY() - obstacle.height() / 2) * sy);
            int right = (int) Math.ceil((obstacle.centerX() + obstacle.width() / 2) * sx);
            int bottom = (int) Math.ceil((obstacle.centerY() + obstacle.height() / 2) * sy);
            g.fillRect(ox + left, oy + top, Math.max(1, right - left), Math.max(1, bottom - top));
        }
        boolean firstOptional = true;
        for (var landmark : area.landmarks()) {
            int lx = ox + (int) (landmark.x() * sx), ly = oy + (int) (landmark.y() * sy);
            g.setColor(landmark.optional() ? GOLD : MUTED);
            int size = expanded ? 5 : 2;
            g.fillRect(lx - size, ly - size, size * 2, size * 2);
            if (expanded) {
                centered(g, landmark.name(), lx, ly - 12, 12, landmark.optional() ? GOLD : TEXT);
                if (landmark.optional()) centered(g, firstOptional ? "MEMORY" : "CACHE", lx, ly + 20, 10, MUTED);
            }
            if (landmark.optional()) firstOptional = false;
        }
        int hx = ox + (int) (area.hub().x() * sx), hy = oy + (int) (area.hub().y() * sy);
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(2));
        g.drawOval(hx - 7, hy - 7, 14, 14);
        if (expanded) centered(g, CampaignStory.npcName(game.biome()) + " / CAMP", hx, hy + 33, 11, GOLD);
        int bx = ox + (int) (area.boss().x() * sx), by = oy + (int) (area.boss().y() * sy);
        g.setColor(game.guardian().alive() ? DANGER : GOLD);
        g.drawPolygon(new int[]{bx, bx + 8, bx, bx - 8}, new int[]{by - 8, by, by + 8, by}, 4);
        for (var point : new CampaignWorld.Point[]{area.exit(), area.returnPortal()}) {
            int px = ox + (int) (point.x() * sx), py = oy + (int) (point.y() * sy);
            g.setColor(MUTED);
            g.drawRect(px - 5, py - 5, 10, 10);
        }
        int px = ox + (int) (game.player().x() * sx), py = oy + (int) (game.player().y() * sy);
        g.setColor(INK);
        g.fillOval(px - 7, py - 7, 14, 14);
        g.setColor(CYAN);
        g.fillOval(px - 4, py - 4, 8, 8);
        g.setStroke(new BasicStroke(1));
    }

    public static void drawOverlay(Graphics2D g, RuinedOutpostGame game, int width, int height) {
        var story = game.campaignStory();
        if (story.dialogueOpen() && game.player().alive()) {
            g.setColor(new Color(4, 8, 14, 130));
            g.fillRect(0, 0, width, height);
            panel(g, 100, height - 246, width - 200, 214);
            text(g, story.dialogueTitle(), 126, height - 212, 19, GOLD);
            int lineY = height - 178;
            for (String line : story.dialogueLines()) {
                lineY = wrapped(g, line, 126, lineY, width - 252, 15, 23, 2, TEXT) + 4;
            }
            text(g, "E / ENTER  CONTINUE", width - 346, height - 52, 13, CYAN);
            return;
        }
        boolean title = game.story().phase() == OutpostStory.Phase.PROLOGUE;
        if (!title && game.player().alive() && !game.choosingEnding() && game.campaignEnding() == null && !game.paused())
            return;
        g.setColor(new Color(5, 9, 17, 228));
        g.fillRect(0, 0, width, height);
        panel(g, 120, 106, width - 240, height - 212);
        if (title) {
            centered(g, "BLOB TO BLADE", width / 2, 174, 38, CYAN);
            centered(g, "ABSORB. TRANSFORM. CONQUER.", width / 2, 211, 15, GOLD);
            centeredLines(g, CampaignStory.openingLines(), width, 270, 16, 29);
            centered(g, "ENTER  NEW JOURNEY", width / 2, height - 193, 18, CYAN);
            centered(g, game.saveAvailable() ? "C  CONTINUE FROM CAMP" : "NO SAVED JOURNEY", width / 2,
                    height - 158, 14, game.saveAvailable() ? GOLD : MUTED);
            if (!game.campaignNotice().isEmpty()) wrapped(g, game.campaignNotice(), 184, height - 264,
                    width - 368, 14, 24, 2, GOLD);
            centered(g, "WASD MOVE  /  O OPTIONS  /  M SOUND  /  V EFFECTS", width / 2,
                    height - 126, 12, MUTED);
        } else if (game.campaignEnding() != null) {
            centered(g, CampaignStory.endingTitle(game.campaignEnding()), width / 2, 177, 27, GOLD);
            centeredLines(g, CampaignStory.endingLines(game.campaignEnding()), width, 265, 16, 34);
            centered(g, "THE JOURNEY IS COMPLETE", width / 2, height - 176, 17, CYAN);
            centered(g, "R  RETURN TO TITLE", width / 2, height - 134, 14, GOLD);
        } else if (game.choosingEnding()) {
            centered(g, "THE LAST CHOICE", width / 2, 174, 30, GOLD);
            centered(g, "The rift is yours to close. The Ichor is yours to keep.", width / 2, 236, 16, TEXT);
            int cardWidth = (width - 324) / 2;
            panel(g, 146, 284, cardWidth, 200);
            panel(g, width / 2 + 16, 284, cardWidth, 200);
            centered(g, "1  CLOSE THE RIFT", 146 + cardWidth / 2, 326, 20, CYAN);
            centered(g, "2  KEEP THE CORE", width / 2 + 16 + cardWidth / 2, 326, 20, GOLD);
            wrapped(g, "Spend the Ichor. End the breach. Remain a slime.", 168, 374, cardWidth - 44, 16, 28, 3, TEXT);
            wrapped(g, "Reclaim human form. Leave the rift sealed, but still alive.", width / 2 + 38, 374,
                    cardWidth - 44, 16, 28, 3, TEXT);
        } else if (!game.player().alive()) {
            centered(g, "SCATTERED, NOT FINISHED", width / 2, 194, 29, DANGER);
            centered(g, game.campaignArea().name(), width / 2, 252, 17, GOLD);
            centered(g, "Reform at your last camp. Recovered strength stays with you.", width / 2, 333, 15, TEXT);
            centered(g, "R  RETRY", width / 2, height - 177, 22, CYAN);
        } else {
            centered(g, "PAUSED", width / 2, 166, 32, GOLD);
            String[] controls = {"WASD / ARROWS   MOVE", "LMB / RMB       ATTACK / FORM SKILL", "SPACE / Q       DASH / TRANSFORM",
                    "F / E           RIPOSTE / INTERACT", "H / 1-4         REST + SAVE / CAMP UPGRADES", "TAB             HOLD REGION MAP",
                    "M / V           SOUND / REDUCED EFFECTS"};
            for (int i = 0; i < controls.length; i++)
                text(g, controls[i], width / 2 - 258, 230 + i * 34, 16, TEXT);
            centered(g, "ESC RESUME / O OPTIONS / T TITLE / F1 TEST MENU", width / 2, height - 134, 14, CYAN);
        }
    }

    private static void centeredLines(Graphics2D g, String[] lines, int width, int y, int size, int spacing) {
        for (int i = 0; i < lines.length; i++) centered(g, lines[i], width / 2, y + i * spacing, size, TEXT);
    }

    private static int wrapped(Graphics2D g, String value, int x, int y, int width, int size,
            int spacing, int maxLines, Color color) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, size));
        StringBuilder line = new StringBuilder();
        int count = 0;
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && g.getFontMetrics().stringWidth(candidate) > width) {
                text(g, line.toString(), x, y, size, color);
                y += spacing;
                if (++count >= maxLines) return y;
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) {
            text(g, line.toString(), x, y, size, color);
            y += spacing;
        }
        return y;
    }

    private static void panel(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(PANEL);
        g.fillRect(x, y, width, height);
        g.setColor(new Color(70, 79, 84));
        g.drawRect(x, y, width - 1, height - 1);
    }

    private static void bar(Graphics2D g, int x, int y, int width, int height, double fraction, Color color) {
        g.setColor(INK);
        g.fillRect(x - 2, y - 2, width + 4, height + 4);
        g.setColor(new Color(58, 55, 61));
        g.fillRect(x, y, width, height);
        g.setColor(color);
        g.fillRect(x, y, (int) (width * Math.max(0, Math.min(1, fraction))), height);
    }

    private static void text(Graphics2D g, String value, int x, int y, int size, Color color) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, size));
        g.setColor(color);
        g.drawString(value, x, y);
    }

    private static void centered(Graphics2D g, String value, int x, int y, int size, Color color) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, size));
        text(g, value, x - g.getFontMetrics().stringWidth(value) / 2, y, size, color);
    }
}
