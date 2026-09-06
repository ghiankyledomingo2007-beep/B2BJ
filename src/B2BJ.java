import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings("serial")
public final class B2BJ extends JPanel {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final Color BACKGROUND = new Color(24, 31, 44);
    private static final Color INK = new Color(14, 17, 24);
    private static final Color PANEL = new Color(20, 27, 39, 232);
    private static final Color TEAL = new Color(46, 209, 205);
    private static final Color PURPLE = new Color(123, 63, 134);
    private static final Color RED = new Color(220, 71, 78);
    private static final Color GOLD = new Color(230, 188, 92);
    private static final Font SMALL_FONT = new Font(Font.MONOSPACED, Font.BOLD, 14);

    private final RuinedOutpostGame game = new RuinedOutpostGame();
    private final BufferedImage[] terrainTiles = outdoorTiles(renderTiles(
            loadImage("assets/tilesets/ruined_outpost/ruined_outpost_wang.png")));
    private final BufferedImage slimeSheet = loadSlimeSheet();
    private final BufferedImage slimeIdleSheet = loadImage("assets/characters/slime/slime_idle.png");
    private final BufferedImage slimeCastSheet=loadImage("assets/characters/slime/slime_cast.png");
    private final BufferedImage bladeSideSheet=loadImage("assets/characters/blade/blade_side.png");
    private final BufferedImage waterBuffer = new BufferedImage(72,72,BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage waterSlashSheet = loadImage("assets/effects/water_slash.png");
    private final BufferedImage tideWaveSheet = loadImage("assets/effects/tide_wave.png");
    private final BufferedImage waterSplashSheet = loadImage("assets/effects/water_splash.png");
    private final BufferedImage waterChargeSheet = loadImage("assets/effects/water_charge.png");
    private final BufferedImage waterWakeSheet = loadImage("assets/effects/water_wake.png");
    private final BufferedImage tideImpactSheet = loadImage("assets/effects/tide_impact.png");
    private final BufferedImage remnantSheet = tintRemnant(slimeSheet);
    private final BufferedImage slimeEffectsSheet = loadImage(
            "assets/characters/slime/slime_effects.png");
    private final BufferedImage bladeRunSheet = loadImage(
            "assets/characters/blade/blade_run.png");
    private final BufferedImage bladeDashSheet = loadImage(
            "assets/characters/blade/blade_dash.png");
    private final BufferedImage bladeSlashSheet = loadImage(
            "assets/characters/blade/blade_slash.png");
    private final BufferedImage bladeCutSheet = loadImage(
            "assets/characters/blade/blade_cut.png");
    private final BufferedImage wispWalkSheet = loadImage(
            "assets/characters/wisp/wisp_walk.png");
    private final BufferedImage remnantWalkSheet=loadImage("assets/characters/wisp/remnant_walk.png");
    private final BufferedImage remnantAttackSheet=loadImage("assets/characters/wisp/remnant_attack.png");
    private final BufferedImage wispAttackSheet = loadImage(
            "assets/characters/wisp/wisp_attack.png");
    private final BufferedImage guardianSheet = loadImage(
            "assets/characters/guardian/guardian_actions.png");
    private final BufferedImage guardianWalkSheet=loadImage("assets/characters/guardian/guardian_walk.png");
    private final BufferedImage guardianDeathSheet=loadImage("assets/characters/guardian/guardian_death.png");
    private final BufferedImage guardianSweepSheet=loadImage("assets/characters/guardian/guardian_sweep.png");
    private final BufferedImage guardianChargeSheet=loadImage("assets/characters/guardian/guardian_charge.png");
    private final BufferedImage guardianWaveSheet=loadImage("assets/characters/guardian/guardian_wave.png");
    private final BufferedImage pixelFont = loadImage("assets/ui/pixel_font.png");
    private final BufferedImage vitalityIcon=loadImage("assets/ui/vitality.png");
    private final BufferedImage blobIcon=loadImage("assets/ui/blob.png");
    private final BufferedImage bladeIcon=loadImage("assets/ui/blade.png");
    private final BufferedImage slashIcon=loadImage("assets/ui/slash.png");
    private final BufferedImage waveIcon=loadImage("assets/ui/wave.png");
    private final BufferedImage dashIcon=loadImage("assets/ui/dash.png");
    private final BufferedImage consumeIcon=loadImage("assets/ui/consume.png");
    private final BufferedImage hudFrame=loadImage("assets/ui/hud_frame.png");
    private final BufferedImage skillFrame=loadImage("assets/ui/skill_frame.png");
    private final BufferedImage corpseIdleSheet=loadImage("assets/effects/corpse_idle.png");
    private final BufferedImage corpseConsumeSheet=loadImage("assets/effects/corpse_consume.png");
    private final BufferedImage absorbLinkSheet=loadImage("assets/effects/absorb_link.png");
    private final BufferedImage slimeAbsorbSheet=loadImage("assets/characters/slime/slime_absorb.png");
    private final BufferedImage healSheet=loadImage("assets/effects/heal_motes.png");
    private final BufferedImage campIcon=loadImage("assets/ui/camp.png");
    private final BufferedImage healIcon=loadImage("assets/ui/heal.png");
    private static final List<RuinedOutpostMap> MAP_ROOMS=java.util.stream.IntStream.range(0,
            RuinedOutpostMap.ROOMS.size()).mapToObj(RuinedOutpostMap::new).toList();
    private final BufferedImage ichorIdleSheet=loadImage("assets/effects/ichor_idle.png");
    private final BufferedImage ichorPickupSheet=loadImage("assets/effects/ichor_pickup.png");
    private final BufferedImage slimeHurtSheet=loadImage("assets/effects/slime_hurt.png");
    private final BufferedImage armorSparksSheet=loadImage("assets/effects/armor_sparks.png");
    private final BufferedImage spitOrbSheet=loadImage("assets/effects/spit_orb.png");
    private final BufferedImage spitImpactSheet=loadImage("assets/effects/spit_impact.png");
    private final BufferedImage stoneChipsSheet=loadImage("assets/effects/stone_chips.png");
    private final BufferedImage spitterWalkSheet=loadImage("assets/characters/wisp/spitter_walk.png");
    private final BufferedImage spitterAttackSheet=loadImage("assets/characters/wisp/spitter_attack.png");
    private final java.util.Map<RuinedOutpostMap.Prop,BufferedImage> propSprites =
            new java.util.EnumMap<>(RuinedOutpostMap.Prop.class);
    private static final String GLYPHS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:./-!";
    private final List<Impact> impacts = new ArrayList<>();
    private int effectSequence;
    private final java.util.Set<Integer> pressedKeys = new java.util.HashSet<>();
    private boolean attackHeld, mouseAimed, mapShown;
    private boolean reducedEffects;
    private int mouseX = 640, mouseY = 360;
    private final List<double[]> trails = new ArrayList<>();

    private SlimeAnimation slimeAnimation = new SlimeAnimation();
    private BladeAnimation bladeAnimation = new BladeAnimation();
    private TransformationAnimation transformationAnimation =
            new TransformationAnimation();
    private CombatFeedback feedback = new CombatFeedback();
    private double attackEffectTime;
    private int attackFacingX;
    private int attackFacingY = 1;
    private boolean attackWasBlade;
    private String bannerText = "";
    private double bannerTime;
    private long frameCounter;
    private long previousFrame = System.nanoTime();

    B2BJ(boolean startTimer) {
        for(var prop:RuinedOutpostMap.Prop.values())propSprites.put(prop,loadImage(prop.path()));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BACKGROUND);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        bindKeys();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1 && event.getButton() != MouseEvent.BUTTON3) {
                    return;
                }
                if (game.story().phase() == OutpostStory.Phase.PROLOGUE) {
                    game.begin();
                } else {
                    mouseX=event.getX(); mouseY=event.getY(); mouseAimed=true;
                    if(event.getButton()==MouseEvent.BUTTON1) { attackHeld=true; attack(); }
                    else attack(true);
                }
            }
            @Override public void mouseReleased(MouseEvent event) {
                if (event.getButton()==MouseEvent.BUTTON1) attackHeld=false;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                mouseX=event.getX(); mouseY=event.getY(); mouseAimed=true;
            }
            @Override public void mouseDragged(MouseEvent event) { mouseMoved(event); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) {
                clearInput(); game.pause(); repaint();
            }
        });
        if (startTimer) {
            Timer timer = new Timer(16, this::updateGame);
            timer.setCoalesce(true);
            timer.start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            B2BJ panel = new B2BJ(true);
            JFrame window = new JFrame("B2BJ — Ruined Outpost");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.add(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            panel.requestFocusInWindow();
        });
    }

    RuinedOutpostGame game() {
        return game;
    }

    private void updateGame(ActionEvent event) {
        long now = System.nanoTime();
        double seconds = Math.min((now - previousFrame) / 1_000_000_000.0, 0.05);
        previousFrame = now;

        if (game.paused()) { repaint(); return; }
        step(seconds);
    }

    void step(double seconds) {
        if (game.paused()) return;
        int horizontal = horizontal();
        int vertical = vertical();
        if (attackHeld) attack();
        game.update(seconds, horizontal, vertical);

        int animationX = game.story().blocksGameplay() ? 0 : horizontal;
        int animationY = game.story().blocksGameplay() ? 0 : vertical;
        Player player = game.player();
        if (player.recovering()) { animationX=0; animationY=0; }
        if (player.dashing()) trails.add(new double[]{player.x(),player.y(),0});
        for (double[] trail:trails) trail[2]+=seconds;
        trails.removeIf(trail->trail[2]>0.14);
        if (player.bladeForm()) {
            bladeAnimation.update(animationX, animationY, player.dashing(), seconds);
        } else {
            slimeAnimation.update(animationX, animationY, player.dashing(), seconds);
        }
        transformationAnimation.update(seconds);
        feedback.update(seconds);
        updateImpacts(seconds);
        attackEffectTime = Math.max(0, attackEffectTime - seconds);
        bannerTime = Math.max(0, bannerTime - seconds);
        handleEvents(game.drainEvents());
        frameCounter++;
        repaint();
    }

    private void handleEvents(List<RuinedOutpostGame.Event> events) {
        for (RuinedOutpostGame.Event event : events) {
            switch (event.type()) {
                case DASH -> GameAudio.play(GameAudio.Cue.DASH);
                case ATTACK -> GameAudio.play(game.player().bladeForm()
                        ? GameAudio.Cue.ATTACK : GameAudio.Cue.BLOB_ATTACK);
                case WATER_IMPACT, TIDE_IMPACT -> {
                    addImpact(event.x(),event.y(),event.type()==RuinedOutpostGame.EventType.TIDE_IMPACT?Fx.TIDE:Fx.WATER);
                    GameAudio.play(GameAudio.Cue.WATER_SPLASH);
                }
                case ENEMY_HIT -> {
                    feedback.enemyHit(false);
                    if(!event.water()) {
                        addImpact(event.x(),event.y(),game.player().bladeForm()?Fx.BLADE:Fx.WATER);
                        GameAudio.play(GameAudio.Cue.HIT);
                    }
                }
                case ENEMY_DEFEATED -> {
                    feedback.enemyHit(true);
                    impacts.removeIf(impact->impact.x==event.x()&&impact.y==event.y()&&impact.time==0);
                    if(!event.water())addImpact(event.x(),event.y(),game.player().bladeForm()?Fx.BLADE:Fx.WATER);
                }
                case PLAYER_HIT -> {
                    feedback.playerHit();
                    addImpact(game.player().x(),game.player().y(),game.player().bladeForm()?Fx.BLADE_HURT:Fx.SLIME_HURT);
                    GameAudio.play(GameAudio.Cue.HURT);
                }
                case PICKUP -> {
                    addImpact(event.x(),event.y(),Fx.PICKUP);
                    GameAudio.play(GameAudio.Cue.PICKUP);
                }
                case ABSORB_START -> {
                    int dx=(int)Math.round(event.x()-game.player().x()),dy=(int)Math.round(event.y()-game.player().y());
                    slimeAnimation.face(dx,dy);
                    GameAudio.play(GameAudio.Cue.BLOB_ATTACK);
                }
                case ABSORBED -> {
                    addImpact(event.x(),event.y(),Fx.PICKUP);
                    GameAudio.play(GameAudio.Cue.PICKUP);
                }
                case HEALED -> addImpact(event.x(),event.y(),Fx.HEAL);
                case TRANSFORM -> {
                    bladeAnimation.update(slimeAnimation.facingHorizontal(),
                            slimeAnimation.facingVertical(), false, 0);
                    transformationAnimation.start();
                    feedback.transformed();
                    GameAudio.play(GameAudio.Cue.TRANSFORM);
                }
                case REVERT -> {
                    slimeAnimation.update(bladeAnimation.facingHorizontal(),
                            bladeAnimation.facingVertical(), false, 0);
                    transformationAnimation.start();
                    feedback.transformed();
                    banner("BLADE FADES / RECOVERING", 1.5);
                    GameAudio.play(GameAudio.Cue.TRANSFORM);
                }
                case GUARDIAN_AWAKENED -> {
                    banner("THE OUTPOST WARDEN WAKES", 2.6);
                    GameAudio.play(GameAudio.Cue.GUARDIAN_SLAM);
                }
                case GUARDIAN_SLAM -> {
                    feedback.guardianSlam();
                    addImpact(event.x(),event.y(),Fx.STONE);
                    GameAudio.play(GameAudio.Cue.GUARDIAN_SLAM);
                }
                case SPIT_SHOT -> GameAudio.play(GameAudio.Cue.BLOB_ATTACK);
                case SPIT_IMPACT -> addImpact(event.x(),event.y(),Fx.SPIT);
                case ROOM_ENTERED -> {
                    trails.clear(); impacts.clear(); attackHeld=false;
                }
                case VICTORY -> {
                    feedback.victory();
                    GameAudio.play(GameAudio.Cue.VICTORY);
                }
            }
        }
    }

    private void updateImpacts(double seconds) {
        Iterator<Impact> iterator = impacts.iterator();
        while (iterator.hasNext()) {
            Impact impact = iterator.next();
            impact.time += seconds;
            if (impact.time >= impact.kind.duration) {
                iterator.remove();
            }
        }
    }
    private void addImpact(double x,double y,Fx kind) {
        if(impacts.size()>=64)impacts.remove(0);
        impacts.add(new Impact(x,y,kind,effectSequence++));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        Player player = game.player();
        RuinedOutpostMap map = game.map();
        int cameraX = cameraPosition(player.x(), WIDTH, map.worldWidth());
        int cameraY = cameraPosition(player.y(), HEIGHT, map.worldHeight());
        int shakeX = reducedEffects?0:feedback.shakeX(frameCounter)/2*2;
        int shakeY = reducedEffects?0:feedback.shakeY(frameCounter)/2*2;
        canvas.translate(shakeX, shakeY);
        drawWorld(canvas, cameraX, cameraY);
        canvas.translate(-shakeX, -shakeY);
        drawScreenEffects(canvas);
        drawHud(canvas);
        drawBanner(canvas);
        drawStoryOverlay(canvas);
        if (game.paused()) drawPause(canvas);
        if (mapShown&&!game.story().blocksGameplay()) drawMap(canvas, true);
        canvas.dispose();
    }

    private record DepthDraw(double groundY, Runnable paint) { }

    private void drawWorld(Graphics2D canvas, int cameraX, int cameraY) {
        drawTerrain(canvas, cameraX, cameraY);
        canvas.setColor(new Color(10,16,27,105)); canvas.fillRect(0,0,WIDTH,HEIGHT);
        drawBanks(canvas, cameraX, cameraY);
        drawFieldRemains(canvas,cameraX,cameraY);
        drawCorpses(canvas,cameraX,cameraY);
        drawIchor(canvas, cameraX, cameraY);
        drawGuardianTelegraph(canvas, cameraX, cameraY);

        // Painter's order uses ground contact, never sprite centre or asset loading order.
        List<DepthDraw> draws=new ArrayList<>();
        for(var barrier:game.map().barriers())
            draws.add(new DepthDraw(barrier.centerY()+barrier.height()/2,
                    ()->drawBarrier(canvas,barrier,cameraX,cameraY)));
        queueDoors(draws,canvas,cameraX,cameraY);
        Player player=game.player();
        for(Wisp scout:game.scouts()) if(scout.alive())
            draws.add(new DepthDraw(scout.y()+WispAnimation.RENDER_SIZE/2,
                    ()->drawWisp(canvas,scout,cameraX,cameraY)));
        if(game.map().room()==9&&game.guardian().visible())
            draws.add(new DepthDraw(game.guardian().y()+Guardian.GROUND_Y_OFFSET,
                    ()->drawGuardian(canvas,cameraX,cameraY)));
        for(double[] trail:trails) {
            draws.add(new DepthDraw(trail[1]+SlimeAnimation.RENDER_SIZE/2,()->{
                Graphics2D ghost=(Graphics2D)canvas.create();
                ghost.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        (float)(0.25*(1-trail[2]/0.14))));
                int gx=cameraX+(int)(player.x()-trail[0]),gy=cameraY+(int)(player.y()-trail[1]);
                if(player.bladeForm())drawBlade(ghost,gx,gy);else drawSlime(ghost,gx,gy);
                ghost.dispose();
            }));
        }
        draws.add(new DepthDraw(player.y()+SlimeAnimation.RENDER_SIZE/2,
                ()->drawPlayer(canvas,cameraX,cameraY)));
        for(WaterProjectile wave:game.projectiles())
            draws.add(new DepthDraw(wave.y()+Player.COLLISION_Y_OFFSET,
                    ()->drawWater(canvas,cameraX,cameraY,wave)));
        for(var shot:game.hostileProjectiles())
            draws.add(new DepthDraw(shot.y()+Wisp.COLLISION_Y_OFFSET,
                    ()->drawHostileShot(canvas,cameraX,cameraY,shot)));
        draws.sort(java.util.Comparator.comparingDouble(DepthDraw::groundY));
        for(DepthDraw draw:draws)draw.paint().run();

        drawAttackEffect(canvas,cameraX,cameraY);
        drawImpacts(canvas,cameraX,cameraY);
        drawWeather(canvas);
    }

    private void drawPlayer(Graphics2D canvas, int cameraX, int cameraY) {
        Player player = game.player();
        if (!player.invulnerable() || frameCounter / 3 % 2 == 0) {
            if (player.bladeForm()) {
                drawBlade(canvas, cameraX, cameraY);
            } else {
                drawSlime(canvas, cameraX, cameraY);
            }
        }
        drawTransformation(canvas, cameraX, cameraY);
    }

    private void drawSlime(Graphics2D canvas, int cameraX, int cameraY) {
        Player player = game.player();
        double charge=game.waterCharge();
        if(charge>=0&&waterChargeSheet!=null) {
            int frame=Math.min(waterChargeSheet.getWidth()/32-1,(int)(charge*8));
            int cx=(int)Math.round(player.x()/2)*2-cameraX,cy=(int)Math.round(player.y()/2)*2-cameraY+24;
            canvas.drawImage(waterChargeSheet,cx-32,cy-32,cx+32,cy+32,frame*32,0,frame*32+32,32,null);
        }
        int x = (int) Math.round(player.x()) - cameraX
                - SlimeAnimation.RENDER_SIZE / 2;
        int y = (int) Math.round(player.y()) - cameraY
                - SlimeAnimation.RENDER_SIZE / 2;
        if(game.absorptionTarget()!=null&&slimeAbsorbSheet!=null) {
            int row=slimeAnimation.row()==3?1:slimeAnimation.row()==4?2:0;
            int frame=Math.min(7,(int)(game.absorptionProgress()*8));
            boolean flip=slimeAnimation.flipHorizontal();
            canvas.drawImage(slimeAbsorbSheet,flip?x+96:x,y,flip?x:x+96,y+96,
                    frame*48,row*48,frame*48+48,row*48+48,null);
            canvas.setColor(INK);canvas.fillRect(x+16,y+100,64,6);
            canvas.setColor(GOLD);canvas.fillRect(x+16,y+100,(int)(64*game.absorptionProgress()),4);
            return;
        }
        boolean reviewedIdle=slimeAnimation.row()==0&&slimeIdleSheet!=null;
        boolean reviewedCast=slimeAnimation.attacking()&&slimeCastSheet!=null;
        BufferedImage sheet=reviewedCast?slimeCastSheet:reviewedIdle?slimeIdleSheet:slimeSheet;
        if (sheet != null) {
            int sourceX = (reviewedCast?slimeAnimation.castFrame():reviewedIdle?slimeAnimation.idleFrame():slimeAnimation.frame()) * SlimeAnimation.CELL_SIZE;
            int castRow=slimeAnimation.row()==3?1:slimeAnimation.row()==4?2:0;
            int sourceY = (reviewedCast?castRow:reviewedIdle?0:slimeAnimation.row()) * SlimeAnimation.CELL_SIZE;
            int imageLeft = slimeAnimation.flipHorizontal()
                    ? x + SlimeAnimation.RENDER_SIZE : x;
            int imageRight = slimeAnimation.flipHorizontal()
                    ? x : x + SlimeAnimation.RENDER_SIZE;
            canvas.drawImage(sheet, imageLeft, y,
                    imageRight, y + SlimeAnimation.RENDER_SIZE,
                    sourceX, sourceY,
                    sourceX + SlimeAnimation.CELL_SIZE,
                    sourceY + SlimeAnimation.CELL_SIZE, null);
        } else {
            canvas.setColor(TEAL);
            canvas.fillOval(x, y + 16, SlimeAnimation.RENDER_SIZE,
                    SlimeAnimation.RENDER_SIZE - 16);
        }
    }


    private void drawBlade(Graphics2D canvas, int cameraX, int cameraY) {
        Player player = game.player();
        if(bladeAnimation.action()==BladeAnimation.Action.SLASH&&bladeAnimation.row()==1&&bladeSideSheet!=null) {
            int frame=sideAttackFrame(0.3-attackEffectTime,game.combo());
            int left=(int)Math.round(player.x())-cameraX-64;
            int top=(int)Math.round(player.y())-cameraY+SlimeAnimation.RENDER_SIZE/2-144;
            int row=game.combo()==1?1:0;
            boolean flip=bladeAnimation.flipHorizontal();
            canvas.drawImage(bladeSideSheet,flip?left+128:left,top,flip?left:left+128,top+160,
                    frame*64,row*80,frame*64+64,row*80+80,null);
            return;
        }
        int padding = bladeAnimation.padding();
        int cellWidth = BladeAnimation.CELL_WIDTH + padding * 2;
        int cellHeight = BladeAnimation.CELL_HEIGHT + padding * 2;
        int renderWidth = cellWidth * 2;
        int renderHeight = cellHeight * 2;
        int x = (int) Math.round(player.x()) - cameraX
                - renderWidth / 2;
        int floorY = (int) Math.round(player.y()) - cameraY
                + SlimeAnimation.RENDER_SIZE / 2;
        int y = floorY - (BladeAnimation.CELL_HEIGHT + padding) * 2;
        BufferedImage sheet = switch (bladeAnimation.action()) {
            case IDLE, RUN -> bladeRunSheet;
            case DASH -> bladeDashSheet;
            case SLASH -> bladeAnimation.row() == 1 ? bladeSlashSheet : bladeCutSheet;
        };
        if (sheet == null) {
            canvas.setColor(new Color(38, 66, 94));
            canvas.fillRect(x + padding * 2 + 28, y + padding * 2 + 16, 40,
                    BladeAnimation.RENDER_HEIGHT - 16);
            return;
        }
        int sourceX = bladeAnimation.frame() * cellWidth;
        int sourceRow = padding > 0 && bladeAnimation.row() == 2 ? 1 : bladeAnimation.row();
        int sourceY = sourceRow * cellHeight;
        int imageLeft = bladeAnimation.flipHorizontal()
                ? x + renderWidth : x;
        int imageRight = bladeAnimation.flipHorizontal()
                ? x : x + renderWidth;
        canvas.drawImage(sheet, imageLeft, y,
                imageRight, y + renderHeight,
                sourceX, sourceY,
                sourceX + cellWidth,
                sourceY + cellHeight, null);
    }

    static int sideAttackFrame(double elapsed,int combo) {
        double windup=RuinedOutpostGame.strikeWindup(combo);
        return elapsed<windup?Math.max(0,Math.min(2,(int)(elapsed/windup*3)))
                :3+Math.min(4,(int)((elapsed-windup)/(0.3-windup)*5));
    }

    private void drawTransformation(Graphics2D canvas, int cameraX, int cameraY) {
        if (!transformationAnimation.active() || slimeEffectsSheet == null) {
            return;
        }
        Player player = game.player();
        int x = (int) Math.round(player.x()) - cameraX
                - TransformationAnimation.RENDER_SIZE / 2;
        int y = (int) Math.round(player.y()) - cameraY
                - TransformationAnimation.RENDER_SIZE / 2;
        int sourceX = transformationAnimation.frame()
                * TransformationAnimation.CELL_SIZE;
        int sourceY = TransformationAnimation.ROW
                * TransformationAnimation.CELL_SIZE;
        canvas.drawImage(slimeEffectsSheet, x, y,
                x + TransformationAnimation.RENDER_SIZE,
                y + TransformationAnimation.RENDER_SIZE,
                sourceX, sourceY,
                sourceX + TransformationAnimation.CELL_SIZE,
                sourceY + TransformationAnimation.CELL_SIZE, null);
    }

    private void drawWisp(Graphics2D canvas, Wisp scout,
            int cameraX, int cameraY) {
        if (!scout.flashVisible()) {
            return;
        }
        int size = (int) Math.round(WispAnimation.RENDER_SIZE
                * scout.renderScale());
        if (size <= 0) {
            return;
        }
        int x = (int) Math.round(scout.x()) - cameraX - size / 2;
        int floorY = (int) Math.round(scout.y()) - cameraY
                + WispAnimation.RENDER_SIZE / 2;
        int y = floorY - size;
        if (scout.state() == Wisp.State.TELEGRAPH) {
            canvas.setColor(RED);
            canvas.setStroke(new BasicStroke(3));
            canvas.drawOval(x - 8, floorY - 20, size + 16, 24);
            int cx=(int)scout.x()-cameraX,cy=(int)scout.y()-cameraY;
            double reach=scout.role()==Wisp.Role.SPITTER?300:520*scout.lungeDuration();
            int ex=cx+(int)(scout.intentX()*reach),ey=cy+(int)(scout.intentY()*reach);
            canvas.setColor(scout.directionLocked()?new Color(255,191,116):RED);
            canvas.drawLine(cx,cy,ex,ey);
            int ox=(int)(-scout.intentY()*8),oy=(int)(scout.intentX()*8);
            canvas.drawLine(ex,ey,ex-(int)(scout.intentX()*16)+ox,ey-(int)(scout.intentY()*16)+oy);
            canvas.drawLine(ex,ey,ex-(int)(scout.intentX()*16)-ox,ey-(int)(scout.intentY()*16)-oy);
            if(scout.role()==Wisp.Role.SPITTER) {
                double angle=Math.atan2(scout.intentY(),scout.intentX());
                for(int side:new int[]{-1,1})canvas.drawLine(cx,cy,cx+(int)(Math.cos(angle+side*.20)*reach),cy+(int)(Math.sin(angle+side*.20)*reach));
            }
        }
        BufferedImage sheet = scout.usesAttackAnimation() && wispAttackSheet != null
                ? wispAttackSheet : wispWalkSheet;
        if(scout.role()==Wisp.Role.SPITTER&&spitterWalkSheet!=null&&spitterAttackSheet!=null) {
            int direction=scout.animation().row(),row=direction==2?2:direction==1||direction==3?1:0;
            int frame=scout.usesAttackAnimation()?scout.attackFrame():scout.animation().frame()%4;
            int groundCorrection=row==2?(int)Math.round(4*size/48.0):0;
            canvas.drawImage(scout.usesAttackAnimation()?spitterAttackSheet:spitterWalkSheet,direction==1?x+size:x,y+groundCorrection,direction==1?x:x+size,y+size+groundCorrection,
                    frame*48,row*48,frame*48+48,row*48+48,null);
        } else if(game.map().room()!=9&&remnantWalkSheet!=null&&remnantAttackSheet!=null) {
            int direction=scout.animation().row();
            int row=direction==2?2:direction==1||direction==3?1:0;
            int frame=scout.usesAttackAnimation()?scout.attackFrame():scout.animation().frame()%4;
            canvas.drawImage(scout.usesAttackAnimation()?remnantAttackSheet:remnantWalkSheet,direction==1?x+size:x,y,direction==1?x:x+size,y+size,
                    frame*48,row*48,frame*48+48,row*48+48,null);
        } else if (game.map().room()!=9 && remnantSheet!=null) {
            int direction=scout.animation().row();
            boolean side=direction==1||direction==3;
            boolean attacking=scout.usesAttackAnimation();
            int row=attacking?(side?10:direction==2?11:5):(side?7:direction==2?8:6);
            int frame=attacking?Math.min(3,scout.attackFrame()/2):scout.animation().frame()%4;
            canvas.drawImage(remnantSheet,direction==1?x+size:x,y,direction==1?x:x+size,y+size,
                    frame*48,row*48,frame*48+48,row*48+48,null);
        } else if (sheet == null) {
            canvas.setColor(PURPLE);
            canvas.fillOval(x + size / 4, y + size / 6,
                    size / 2, size * 3 / 4);
        } else {
            int sourceX = (scout.usesAttackAnimation()
                    ? scout.attackFrame() : scout.animation().frame())
                    * WispAnimation.CELL_SIZE;
            int sourceY = scout.animation().row() * WispAnimation.CELL_SIZE;
            canvas.drawImage(sheet, x, y, x + size, y + size,
                    sourceX, sourceY,
                    sourceX + WispAnimation.CELL_SIZE,
                    sourceY + WispAnimation.CELL_SIZE, null);
        }
        if (scout.alive()) {
            if(scout.role()==Wisp.Role.GUARD) {
                canvas.setColor(new Color(222,181,112));
                canvas.fillRect((int)scout.x()-cameraX-5,y-22,10,5);
            }
            if(scout.role()==Wisp.Role.SPITTER) {
                canvas.setColor(new Color(199,143,236));
                for(int mark=-1;mark<=1;mark++)canvas.fillRect((int)scout.x()-cameraX+mark*8-2,y-22,4,4);
            }
            drawHealthBar(canvas, (int) Math.round(scout.x()) - cameraX,
                    y - 9, 52, scout.health(), scout.maxHealth(), PURPLE);
        }
    }

    private void drawGuardian(Graphics2D canvas, int cameraX, int cameraY) {
        Guardian guardian = game.guardian();
        if (!guardian.flashVisible()) {
            return;
        }
        int size = guardian.state()==Guardian.State.DEAD&&guardianDeathSheet!=null?Guardian.RENDER_SIZE
                :(int) Math.round(Guardian.RENDER_SIZE * guardian.renderScale());
        if (size <= 0) {
            return;
        }
        int x = (int) Math.round(guardian.x()) - cameraX - size / 2;
        int floorY = (int) Math.round(guardian.y()) - cameraY
                + Guardian.GROUND_Y_OFFSET+(Guardian.CELL_SIZE-Guardian.SPRITE_FOOT_ROW)*Guardian.RENDER_SIZE/Guardian.CELL_SIZE;
        int y = floorY - size;
        BufferedImage actionSheet=switch(guardian.attack()) {
            case CHARGE -> guardianChargeSheet; case SWEEP -> guardianSweepSheet;
            case SHOCKWAVE -> guardianWaveSheet; default -> null;
        };
        if(guardian.state()==Guardian.State.DEAD&&guardianDeathSheet!=null) {
            int frame=Math.min(11,(int)(guardian.stateSeconds()/Guardian.DEATH_DURATION*12));
            canvas.drawImage(guardianDeathSheet,x,y,x+size,y+size,frame*64,0,frame*64+64,64,null);
        } else if(guardian.state()==Guardian.State.APPROACH&&guardianWalkSheet!=null) {
            int frame=(int)(guardian.stateSeconds()*8)%8;
            canvas.drawImage(guardianWalkSheet,x,y,x+size,y+size,frame*64,0,frame*64+64,64,null);
        } else if(actionSheet!=null&&guardian.state()!=Guardian.State.DORMANT) {
            int frame=switch(guardian.state()) {
                case TELEGRAPH -> Math.min(3,(int)(guardian.stateSeconds()/Guardian.TELEGRAPH_DURATION*4));
                case SLAM -> 4+Math.min(1,(int)(guardian.stateSeconds()/guardian.activeDuration()*2));
                default -> 6+Math.min(1,(int)(guardian.stateSeconds()/Guardian.RECOVER_DURATION*2));
            };
            boolean flip=guardian.attack()==Guardian.Attack.SWEEP&&guardian.directionX()<0;
            canvas.drawImage(actionSheet,flip?x+size:x,y,flip?x:x+size,y+size,frame*64,0,frame*64+64,64,null);
        } else if (guardianSheet == null) {
            canvas.setColor(new Color(73, 81, 97));
            canvas.fillRect(x + size / 5, y + size / 6,
                    size * 3 / 5, size * 5 / 6);
        } else {
            int sourceX = guardian.frame() * Guardian.CELL_SIZE;
            int sourceY = guardian.row() * Guardian.CELL_SIZE;
            canvas.drawImage(guardianSheet, x, y, x + size, y + size,
                    sourceX, sourceY,
                    sourceX + Guardian.CELL_SIZE,
                    sourceY + Guardian.CELL_SIZE, null);
        }
        if (guardian.alive() && guardian.state() != Guardian.State.DORMANT) {
            drawHealthBar(canvas, (int) Math.round(guardian.x()) - cameraX,
                    y - 12, 88, guardian.health(), Guardian.MAX_HEALTH, RED);
        }
    }

    private void drawGuardianTelegraph(Graphics2D canvas,
            int cameraX, int cameraY) {
        Guardian guardian = game.guardian();
        boolean warning=guardian.state()==Guardian.State.TELEGRAPH;
        if (game.map().room()!=9 || (!warning&&guardian.state()!=Guardian.State.SLAM)) {
            return;
        }
        int centerX = (int) Math.round(guardian.targetX()) - cameraX;
        int centerY = (int) Math.round(guardian.targetY()) - cameraY;
        int radius = (int) guardian.slamRadius();
        if(guardian.attack()==Guardian.Attack.SHOCKWAVE&&!warning)radius=(int)guardian.waveRadius();
        if(guardian.attack()==Guardian.Attack.CHARGE) {
            int bx=(int)guardian.x()-cameraX,by=(int)guardian.y()-cameraY;
            if(warning) {
                int ex=bx+(int)(guardian.directionX()*620*0.55),ey=by+(int)(guardian.directionY()*620*0.55);
                int ox=(int)(-guardian.directionY()*92),oy=(int)(guardian.directionX()*92);
                canvas.setColor(new Color(220,71,78,45));
                canvas.fillPolygon(new int[]{bx+ox,ex+ox,ex-ox,bx-ox},new int[]{by+oy,ey+oy,ey-oy,by-oy},4);
                canvas.setColor(RED);canvas.setStroke(new BasicStroke(3));
                canvas.drawLine(bx+ox,by+oy,ex+ox,ey+oy);canvas.drawLine(bx-ox,by-oy,ex-ox,ey-oy);
                canvas.drawOval(ex-92,ey-92,184,184);
            } else {canvas.setColor(RED);canvas.setStroke(new BasicStroke(3));canvas.drawOval(bx-92,by-92,184,184);}
            return;
        }
        canvas.setColor(new Color(220, 71, 78, 190));
        canvas.setStroke(new BasicStroke(4));
        canvas.drawOval(centerX - radius, centerY - radius,
                radius * 2, radius * 2);
        if(guardian.attack()==Guardian.Attack.SHOCKWAVE) {
            int inside=warning?32:Math.max(0,radius-28),outside=warning?radius:radius+28;
            canvas.setColor(new Color(238,166,87,180));
            canvas.drawOval(centerX-inside,centerY-inside,inside*2,inside*2);
            if(!warning)canvas.drawOval(centerX-outside,centerY-outside,outside*2,outside*2);
        }
        canvas.setColor(new Color(220, 71, 78, 120));
        canvas.drawLine(centerX - 14, centerY, centerX + 14, centerY);
        canvas.drawLine(centerX, centerY - 14, centerX, centerY + 14);
    }

    private void drawBarrier(Graphics2D canvas, RuinedOutpostMap.Obstacle barrier, int cameraX, int cameraY) {
            var prop=barrier.prop();
            BufferedImage sprite=propSprites.get(prop);
            if(sprite!=null) {
                int left=(int)Math.round(barrier.centerX())-cameraX-prop.anchorX*2;
                int top=(int)Math.round(barrier.centerY()+barrier.height()/2)-cameraY-prop.anchorY*2;
                canvas.drawImage(sprite,left,top,sprite.getWidth()*2,sprite.getHeight()*2,null);
                return;
            }
            int w=(int)barrier.width(), h=(int)barrier.height();
            int x=(int)(barrier.centerX()-w/2)-cameraX, y=(int)(barrier.centerY()-h/2)-cameraY;
            canvas.setColor(new Color(13,18,28)); canvas.fillRect(x+8,y+8,w,h+8);
            canvas.setColor(new Color(43,51,63)); canvas.fillRect(x,y-16,w,h+16);
            canvas.setColor(new Color(70,79,88)); canvas.fillRect(x,y-16,w,4);
            canvas.setColor(new Color(26,32,43));
            for(int row=0;row<h+16;row+=24) {
                canvas.fillRect(x,y-16+row,w,2);
                for(int col=(row/24%2)*24;col<w;col+=48) canvas.fillRect(x+col,y-16+row,2,24);
            }
            canvas.setColor(new Color(91,97,99));
            canvas.fillRect(x+8,y-12,Math.min(w-16,32),2);
            if (w<1000 && h<300) drawMilitaryProp(canvas,x,y,w,h);
    }

    private void drawMilitaryProp(Graphics2D canvas,int x,int y,int w,int h) {
        int room=game.map().room();
        if(room==7) {
            canvas.setColor(new Color(70,57,44));canvas.fillRect(x,y-12,w,h+12);
            canvas.setColor(new Color(137,131,110));canvas.fillRect(x+12,y-8,w-24,h-8);
            canvas.setColor(new Color(72,81,78));
            for(int i=0;i<4;i++)canvas.fillRect(x+24+i*20,y+4,2,h-24);
        }
    }

    private void queueDoors(List<DepthDraw> draws,Graphics2D canvas,int cameraX,int cameraY) {
        for(var door:game.map().doors()) {
            int x=(int)door.x()-cameraX,y=(int)door.y()-cameraY;
            boolean open=game.map().passageOpen(door);
            boolean vertical=door.x()!=game.map().spawnX();
            if(!open)queueGate(draws,canvas,x,y,door.y(),vertical);
            int signX=x+(vertical?0:112),signY=y+(vertical?112:0);
            draws.add(new DepthDraw(signY+cameraY+24,()->{
                canvas.setColor(new Color(98,87,69));canvas.fillRect(signX-4,signY-16,8,40);
                canvas.setColor(new Color(43,46,49));canvas.fillRect(signX-40,signY-32,80,24);
                pixelCentered(canvas,open?"PASS":"HOLD",signX,signY-26,2,
                        open?new Color(162,183,179):RED);
            }));
        }
        if(game.map().room()==9) {
            int x=(int)game.map().gateX()-cameraX,y=(int)game.map().gateY()-cameraY;
            if(!game.map().gateOpen())queueGate(draws,canvas,x,y,game.map().gateY(),true);
            else pixelCentered(canvas,"E / FOREST",x-50,y-150,2,TEAL);
        }
        if(game.map().room()==7||game.map().room()==10) {
            int x=(int)game.map().restX()-cameraX,y=(int)game.map().restY()-cameraY;
            // Bedroll/dressing are ground items; they must stay below actors.
            canvas.setColor(new Color(42,61,69));canvas.fillRect(x-26,y-10,52,32);
            canvas.setColor(GOLD);canvas.fillRect(x-20,y-6,40,4);
            pixelCentered(canvas,"E",x,y-32,2,GOLD);
        }
    }

    private void queueGate(List<DepthDraw> draws,Graphics2D canvas,int x,int y,double worldY,boolean vertical) {
        if(!vertical) {
            draws.add(new DepthDraw(worldY+20,()->drawFieldGate(canvas,x,y,false)));
            return;
        }
        // A long north/south fence needs local depth, not one depth for its whole length.
        for(int offset=-160;offset<160;offset+=16) {
            int py=y+offset;
            draws.add(new DepthDraw(worldY+offset+10,()->{
                canvas.setColor(new Color(54,47,43));canvas.fillRect(x-20,py,40,10);
                canvas.setColor(new Color(108,91,69));canvas.fillRect(x-20,py,36,4);
                canvas.setColor(new Color(171,77,70));canvas.fillRect(x-4,py,6,16);
            }));
        }
    }

    private void drawFieldGate(Graphics2D canvas,int x,int y,boolean vertical) {
        for(int offset=-160;offset<160;offset+=16) {
            int px=vertical?x-20:x+offset,py=vertical?y+offset:y-20;
            canvas.setColor(new Color(54,47,43));canvas.fillRect(px,py,vertical?40:10,vertical?10:40);
            canvas.setColor(new Color(108,91,69));canvas.fillRect(px,py,vertical?36:4,vertical?4:36);
        }
        canvas.setColor(new Color(171,77,70));
        if(vertical)canvas.fillRect(x-4,y-150,6,300);else canvas.fillRect(x-150,y-4,300,6);
    }

    private void drawBanks(Graphics2D canvas,int cameraX,int cameraY) {
        for(var bank:game.map().banks()) {
            int x=(int)(bank.centerX()-bank.width()/2)-cameraX;
            int y=(int)(bank.centerY()-bank.height()/2)-cameraY;
            int w=(int)bank.width(),h=(int)bank.height();
            canvas.setColor(new Color(18,25,28));canvas.fillRect(x,y,w,h);
            canvas.setColor(new Color(43,49,43));canvas.fillRect(x+4,y+4,w-8,h-8);
            canvas.setColor(new Color(62,65,53));canvas.fillRect(x+8,y+8,w-16,8);
            canvas.setColor(new Color(34,41,37));canvas.fillRect(x+12,y+30,w-24,16);
            canvas.setColor(new Color(25,33,31));canvas.fillRect(x+22,y+4,10,18);
        }
    }

    private void drawFieldRemains(Graphics2D canvas,int cameraX,int cameraY) {
        // Fallen equipment is ground dressing; standing objects use the map's collision rectangles.
        int room=game.map().room();
        int[][] spots={{480,600},{1020,210},{360,400}};
        for(int i=0;i<spots.length;i++) {
            int x=(int)game.map().authored(spots[i][0])-cameraX;
            int y=(int)game.map().authored(spots[i][1])-cameraY;
            if(room==0&&i>0)continue;
            canvas.setColor(new Color(39,44,45));canvas.fillRect(x-24,y-10,48,22);
            canvas.setColor(new Color(93,95,86));canvas.fillRect(x-18,y-14,32,18);
            canvas.setColor(new Color(53,67,78));canvas.fillRect(x-14,y-12,24,14);
            canvas.setColor(new Color(105,104,87));canvas.fillRect(x-4,y-10,4,16);
            canvas.setColor(new Color(73,65,54));canvas.fillRect(x+20,y-22,4,52);
            canvas.setColor(new Color(105,112,112));canvas.fillRect(x+18,y-30,8,12);
        }
    }

    private void drawWeather(Graphics2D canvas) {
        canvas.setColor(new Color(125,158,177,45));
        for(int i=0;i<35;i++) {
            int x=(int)((i*173L+frameCounter*2)%WIDTH);
            int y=(int)((i*97L+frameCounter*8)%HEIGHT);
            canvas.fillRect(x,y,2,10);
        }
    }

    private void drawIchor(Graphics2D canvas, int cameraX, int cameraY) {
        if(ichorIdleSheet==null)return;
        for (RuinedOutpostGame.IchorDrop drop : game.drops()) {
            int x = (int) Math.round(drop.x()) - cameraX;
            int y = (int) Math.round(drop.y()) - cameraY;
            int frame=(int)(drop.age()*10)%(ichorIdleSheet.getWidth()/32);
            canvas.drawImage(ichorIdleSheet,x-32,y-32,x+32,y+32,frame*32,0,frame*32+32,32,null);
        }
    }

    private void drawCorpses(Graphics2D canvas,int cameraX,int cameraY) {
        if(corpseIdleSheet==null)return;
        for(var corpse:game.corpses()) {
            boolean consuming=corpse==game.absorptionTarget();
            BufferedImage sheet=consuming&&corpseConsumeSheet!=null?corpseConsumeSheet:corpseIdleSheet;
            int columns=sheet.getWidth()/48;
            int frame=consuming?Math.min(columns-1,(int)(game.absorptionProgress()*columns)):(int)(corpse.age()*6)%columns;
            double bodyX=corpse.x(),bodyY=corpse.y();
            int x=(int)Math.round(bodyX/2)*2-cameraX,y=(int)Math.round(bodyY/2)*2-cameraY+20;
            var remains=(Graphics2D)canvas.create();
            float alpha=(float)Math.min(1,(RuinedOutpostGame.CORPSE_LIFETIME-corpse.age())/2);
            if(consuming)alpha*=(float)Math.min(1,(1-game.absorptionProgress())*4);
            remains.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,Math.max(0,alpha)));
            remains.drawImage(sheet,x-48,y-48,x+48,y+48,frame*48,0,frame*48+48,48,null);remains.dispose();
            if(consuming&&absorbLinkSheet!=null&&Math.hypot(bodyX-game.player().x(),bodyY-game.player().y())>8) {
                Player player=game.player();
                var pixels=waterBuffer.createGraphics();pixels.setComposite(java.awt.AlphaComposite.Clear);
                pixels.fillRect(0,0,72,72);pixels.setComposite(java.awt.AlphaComposite.SrcOver);
                pixels.translate(36,36);pixels.rotate(Math.atan2(bodyY-player.y(),bodyX-player.x()));
                int span=Math.max(1,(int)(Math.hypot(bodyX-player.x(),bodyY-player.y())/2));
                pixels.clipRect(-span/2,-16,span,32);
                int linkFrame=(int)(game.absorptionProgress()*16)%(absorbLinkSheet.getWidth()/64);
                pixels.drawImage(absorbLinkSheet,-32,-16,32,16,linkFrame*64,0,linkFrame*64+64,32,null);pixels.dispose();
                int mx=(int)((bodyX+player.x())/4)*2-cameraX,my=(int)((bodyY+player.y())/4)*2-cameraY+20;
                canvas.drawImage(waterBuffer,mx-72,my-72,144,144,null);
            }
        }
    }

    private void drawAttackEffect(Graphics2D canvas,int cameraX,int cameraY) {
        if(attackEffectTime<=0 || !attackWasBlade) return;
        Player p=game.player();
        int range=(int)RuinedOutpostGame.BLADE_ATTACK_RANGE;
        double angle=Math.atan2(attackFacingY,attackFacingX);
        double progress=1-attackEffectTime/0.3;
        if(progress<0.25) return; // Windup precedes the actual hit.
        canvas.setColor(attackWasBlade?GOLD:TEAL);
        int px=(int)p.x()-cameraX,py=(int)p.y()-cameraY;
        if(game.combo()==2) {
            int dx=(int)(Math.cos(angle)*range),dy=(int)(Math.sin(angle)*range);
            canvas.setStroke(new BasicStroke(6)); canvas.drawLine(px,py,px+dx,py+dy);
        } else {
            double sweep=game.combo()==0?progress:1-progress;
            for(int i=0;i<7;i++) {
                double a=angle-0.7+sweep*1.4-i*0.08;
                int x=px+(int)(Math.cos(a)*range),y=py+(int)(Math.sin(a)*range);
                canvas.fillRect(x/2*2,y/2*2,6,6);
            }
        }
    }

    private void drawWater(Graphics2D canvas,int cameraX,int cameraY,WaterProjectile wave) {
            BufferedImage sheet=wave.heavy()?tideWaveSheet:waterSlashSheet;
            if(sheet==null)return;
            int cell=wave.heavy()?48:32;
            int frame=(int)(wave.age()*18)%(sheet.getWidth()/cell);
            Graphics2D nativePixels=waterBuffer.createGraphics();
            nativePixels.setComposite(java.awt.AlphaComposite.Clear);nativePixels.fillRect(0,0,72,72);
            nativePixels.setComposite(java.awt.AlphaComposite.SrcOver);
            nativePixels.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            nativePixels.translate(36,36);
            double angle=Math.atan2(wave.directionY(),wave.directionX());
            nativePixels.rotate(angle);
            if(waterWakeSheet!=null) {
                int wakeFrame=(int)(wave.age()*20)%(waterWakeSheet.getWidth()/32);
                nativePixels.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,0.45f));
                nativePixels.drawImage(waterWakeSheet,-34,-16,-2,16,wakeFrame*32,0,wakeFrame*32+32,32,null);
                nativePixels.setComposite(java.awt.AlphaComposite.SrcOver);
            }
            // Reviewed crescents are diagonally authored; align their convex edge with travel.
            nativePixels.rotate(wave.heavy()?0.6:-0.6);
            nativePixels.drawImage(sheet,-cell/2,-cell/2,cell/2,cell/2,frame*cell,0,(frame+1)*cell,cell,null);
            nativePixels.dispose();
            int px=(int)Math.round((wave.x()-cameraX)/2)*2,py=(int)Math.round((wave.y()-cameraY)/2)*2;
            Graphics2D effect=(Graphics2D)canvas.create();
            float alpha=(float)wave.opacity();
            effect.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,Math.max(0,alpha)));
            effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            effect.drawImage(waterBuffer,px-72,py-72,144,144,null);
            effect.dispose();
    }

    private void drawImpacts(Graphics2D canvas, int cameraX, int cameraY) {
        for (Impact impact : impacts) {
            BufferedImage sheet=switch(impact.kind) {
                case WATER -> waterSplashSheet;
                case TIDE -> tideImpactSheet;
                case BLADE,BLADE_HURT -> armorSparksSheet;
                case SLIME_HURT -> slimeHurtSheet;
                case PICKUP -> ichorPickupSheet;
                case STONE -> stoneChipsSheet;
                case SPIT -> spitImpactSheet;
                case HEAL -> healSheet;
            };
            if(sheet==null)continue;
            int cell=impact.kind.cell,columns=sheet.getWidth()/cell;
            int frame=Math.min(columns-1,(int)(impact.time/impact.kind.duration*columns));
            int row=Math.floorMod(impact.variant,sheet.getHeight()/cell);
            int x=(int)Math.round((impact.x-cameraX)/2)*2-cell,y=(int)Math.round((impact.y-cameraY)/2)*2-cell;
            var effect=(Graphics2D)canvas.create();
            float alpha=(float)Math.max(0,Math.min(1,(impact.kind.duration-impact.time)/.12));
            if(reducedEffects&&impact.kind!=Fx.SLIME_HURT&&impact.kind!=Fx.BLADE_HURT)alpha*=.65f;
            effect.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,alpha));
            boolean flip=(impact.variant&2)!=0;
            effect.drawImage(sheet,flip?x+cell*2:x,y,flip?x:x+cell*2,y+cell*2,
                    frame*cell,row*cell,(frame+1)*cell,(row+1)*cell,null);
            effect.dispose();
        }
    }
    private void drawHostileShot(Graphics2D canvas,int cameraX,int cameraY,EnemyProjectile shot) {
        if(spitOrbSheet==null)return;
        int frame=(int)(shot.age()*16)%(spitOrbSheet.getWidth()/32);
        var pixels=waterBuffer.createGraphics();
        pixels.setComposite(java.awt.AlphaComposite.Clear);pixels.fillRect(0,0,72,72);
        pixels.setComposite(java.awt.AlphaComposite.SrcOver);
        pixels.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        pixels.translate(36,36);pixels.rotate(Math.atan2(shot.directionY(),shot.directionX()));
        pixels.drawImage(spitOrbSheet,-16,-16,16,16,frame*32,0,frame*32+32,32,null);pixels.dispose();
        int x=(int)Math.round((shot.x()-cameraX)/2)*2,y=(int)Math.round((shot.y()-cameraY)/2)*2;
        canvas.drawImage(waterBuffer,x-72,y-72,144,144,null);
    }

    private void drawHud(Graphics2D canvas) {
        Player p=game.player();
        if(hudFrame!=null) {
            canvas.setColor(new Color(9,15,23,240));canvas.fillRect(32,30,288,98);
            canvas.drawImage(hudFrame,16,16,320,128,null);
        }
        else uiPanel(canvas,16,16,320,128);
        pixelText(canvas,"RAINORAY",38,32,2,new Color(191,203,209));
        for(int i=0;i<Player.MAX_HEALTH;i++) {
            int x=32+i*50;
            double part=Math.max(0,Math.min(1,p.healthValue()-i));
            var pip=(Graphics2D)canvas.create();
            pip.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.18f));
            icon(pip,vitalityIcon,x,42,64);pip.dispose();
            pip=(Graphics2D)canvas.create();
            if(part<1)pip.clipRect(x+6,42,(int)(52*part),64);
            icon(pip,vitalityIcon,x,42,64);pip.dispose();
        }
        for(int i=0;i<10;i++) {
            int x=38+i*26,fill=(int)(Math.max(0,Math.min(1,(p.ichor()-i*10)/10))*11)*2;
            canvas.setColor(new Color(55,44,33));canvas.fillRect(x,100,22,10);
            canvas.setColor(p.ichor()>=100&&!reducedEffects&&frameCounter/24%2==0?new Color(255,228,147):GOLD);
            canvas.fillRect(x,100,fill,10);
            canvas.setColor(new Color(255,231,164));canvas.fillRect(x,100,fill,2);
        }
        pixelText(canvas,"ICHOR",38,119,1,GOLD);
        pixelText(canvas,String.format(java.util.Locale.ROOT,"%03d / 100",(int)p.ichor()),236,119,1,GOLD);
        uiPanel(canvas,1100,20,160,66);
        icon(canvas,p.bladeForm()?bladeIcon:blobIcon,1106,21,64);
        pixelText(canvas,p.bladeForm()?"BLADE":"BLOB",1170,38,2,p.bladeForm()?GOLD:TEAL);
        pixelText(canvas,GameAudio.muted()?"MUTED":"",1170,64,1,new Color(139,156,170));
        if(p.bladeForm()) {
            Color color=p.bladeSeconds()<=3?RED:TEAL;
            uiPanel(canvas,482,20,316,54);
            pixelText(canvas,"BLADE",500,32,2,color);
            pixelText(canvas,String.format(java.util.Locale.ROOT,"%.1f S",p.bladeSeconds()),702,32,2,color);
            canvas.setColor(new Color(45,48,58));canvas.fillRect(500,58,280,6);
            canvas.setColor(color);canvas.fillRect(500,58,(int)(280*p.bladeSeconds()/Player.BLADE_DURATION),6);
        } else if(p.recovering()) pixelCentered(canvas,"REFORMING",640,34,2,RED);
        if(game.map().room()==9 && game.guardian().alive()) {
            Guardian boss=game.guardian();
            pixelCentered(canvas,boss.enraged()?"OUTPOST WARDEN / ENRAGED":"OUTPOST WARDEN",640,94,2,GOLD);
            drawHealthBar(canvas,640,122,380,boss.health(),Guardian.MAX_HEALTH,
                    boss.state()==Guardian.State.RECOVER?TEAL:RED);
        }
        skill(canvas,478,p.bladeForm()?bladeIcon:slashIcon,"LMB",1,true);
        skill(canvas,562,waveIcon,"RMB",1-game.tideCooldown()/RuinedOutpostGame.TIDE_COOLDOWN,!p.bladeForm());
        skill(canvas,646,dashIcon,"SPACE",1-p.dashCooldown()/Player.DASH_COOLDOWN,!p.recovering());
        skill(canvas,730,bladeIcon,"Q",p.ichor()/100,!p.bladeForm()&&!p.recovering());
        if(p.ichor()>=100&&!p.bladeForm())pixelCentered(canvas,"TRANSFORM",770,601,1,GOLD);
        String prompt=compactPrompt();
        if(!prompt.isEmpty()) {
            boolean absorb=prompt.contains("ABSORB");
            int width=Math.min(460,prompt.length()*12+32+(absorb?40:0));
            uiPanel(canvas,24,652,width,42);
            if(absorb)icon(canvas,consumeIcon,32,656,32);
            pixelText(canvas,prompt,absorb?80:40,666,2,GOLD);
            if(game.absorptionTarget()!=null) {
                canvas.setColor(GOLD);canvas.fillRect(80,686,(int)((width-72)*game.absorptionProgress()),2);
            }
        }
        drawMap(canvas,false);
    }

    private String compactPrompt() {
        String prompt=game.interactionPrompt();
        if(prompt.startsWith("WALK THROUGH"))return "";
        if(prompt.startsWith("CLEAR THE ROOM"))return "PATH SEALED";
        if(prompt.contains("TRY THE BLADE"))return "Q  TRANSFORM";
        if(prompt.contains("FIELD DRESSING"))return "E  HEAL";
        if(prompt.contains("REST AT CAMP"))return "E  REST";
        if(prompt.contains("FOREST BOUNDARY"))return "E  LEAVE OUTPOST";
        return prompt;
    }

    private void skill(Graphics2D canvas,int x,BufferedImage image,String key,double ready,boolean enabled) {
        if(skillFrame!=null)canvas.drawImage(skillFrame,x,620,80,80,null);
        else uiPanel(canvas,x,620,80,80);
        var slot=(Graphics2D)canvas.create();
        if(!enabled)slot.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.3f));
        icon(slot,image,x+8,623,64);slot.dispose();
        ready=Math.max(0,Math.min(1,ready));
        if(enabled&&ready<1) {
            canvas.setColor(new Color(7,10,18,190));canvas.fillRect(x+10,628,60,(int)(52*(1-ready)));
            canvas.setColor(GOLD);canvas.fillRect(x+12,680,(int)(56*ready),2);
        }
        canvas.setColor(new Color(9,14,21,235));canvas.fillRect(x+8,684,64,16);
        pixelCentered(canvas,key,x+40,685,2,enabled?new Color(232,219,186):new Color(116,123,129));
    }

    private void icon(Graphics2D canvas,BufferedImage image,int x,int y,int size) {
        if(image!=null)canvas.drawImage(image,x,y,size,size,null);
        else {canvas.setColor(TEAL);canvas.fillRect(x+size/4,y+size/4,size/2,size/2);}
    }

    private void uiPanel(Graphics2D canvas,int x,int y,int w,int h) {
        canvas.setColor(new Color(8,13,21,238));canvas.fillRect(x,y,w,h);
        canvas.setColor(new Color(91,77,55));canvas.drawRect(x+1,y+1,w-3,h-3);
        canvas.setColor(new Color(181,151,94));
        for(int cx:new int[]{x,x+w-8})for(int cy:new int[]{y,y+h-8}) {
            canvas.fillRect(cx,cy,8,2);canvas.fillRect(cx,cy,2,8);
        }
    }

    private void drawMap(Graphics2D canvas,boolean expanded) {
        if(!expanded) { drawLocalMap(canvas);return; }
        int sx=212,sy=100,ox=322,oy=143;
        canvas.setColor(new Color(5,9,16,240));canvas.fillRect(0,0,WIDTH,HEIGHT);
        uiPanel(canvas,112,40,1056,640);
        pixelText(canvas,"RUINED OUTPOST",148,70,3,GOLD);
        pixelText(canvas,game.paused()?"RELEASE TAB TO CLOSE / PAUSED":"RELEASE TAB TO CLOSE / WORLD REMAINS LIVE",
                650,78,1,new Color(171,180,183));
        canvas.setColor(new Color(69,67,59));canvas.fillRect(148,106,984,2);
        pixelText(canvas,"CURRENT",154,646,2,TEAL);
        pixelText(canvas,"CLEARED",314,646,2,GOLD);
        pixelText(canvas,"UNEXPLORED",474,646,2,new Color(116,128,137));
        for(int i=0;i<MAP_ROOMS.size();i++) {
            if(!game.visited(i))continue;
            var room=RuinedOutpostMap.ROOMS.get(i);
            int x=ox+room.gridX()*sx,y=oy+(room.gridY()+1)*sy;
            for(var door:MAP_ROOMS.get(i).doors()) {
                if(game.visited(door.destination())&&door.destination()<i)continue;
                var next=RuinedOutpostMap.ROOMS.get(door.destination());
                canvas.setColor(new Color(55,62,66));canvas.setStroke(new BasicStroke(6));
                int nx=ox+next.gridX()*sx,ny=oy+(next.gridY()+1)*sy;
                canvas.drawLine(x,y,nx,ny);
                canvas.setColor(game.visited(door.destination())?new Color(144,121,75):new Color(78,86,88));
                canvas.setStroke(new BasicStroke(2));canvas.drawLine(x,y,nx,ny);
            }
        }
        canvas.setStroke(new BasicStroke(1));
        for(int i=0;i<MAP_ROOMS.size();i++) {
            boolean known=game.visited(i),adjacent=false;
            for(var door:MAP_ROOMS.get(i).doors())adjacent|=game.visited(door.destination());
            if(!known&&!adjacent)continue;
            var room=RuinedOutpostMap.ROOMS.get(i);
            int x=ox+room.gridX()*sx,y=oy+(room.gridY()+1)*sy,w=120,h=72;
            Color color=i==game.map().room()?TEAL:game.cleared(i)?GOLD:new Color(115,128,138);
            canvas.setColor(new Color(17,26,35));canvas.fillRect(x-w/2,y-h/2,w,h);
            canvas.setColor(known?color:new Color(61,74,85));canvas.drawRect(x-w/2,y-h/2,w,h);
            if(!known) {pixelCentered(canvas,"...",x,y-4,1,new Color(116,128,137));continue;}
            if(game.cleared(i)) {canvas.setColor(new Color(53,49,35));canvas.fillRect(x-w/2+2,y-h/2+2,w-3,h-3);}
            var area=MAP_ROOMS.get(i);
                for(int ty=1;ty<17;ty++)for(int tx=1;tx<29;tx++) {
                    canvas.setColor(area.tileMask(tx,ty)>0?new Color(93,100,104):new Color(30,42,44));
                    canvas.fillRect(x-60+tx*4,y-36+ty*4,4,4);
                }
                canvas.setColor(new Color(175,151,99));
                for(var obstacle:area.barriers())canvas.fillRect(x-60+(int)(obstacle.centerX()*120/area.worldWidth())-2,
                        y-36+(int)(obstacle.centerY()*72/area.worldHeight())-2,4,4);
                if(i==7||i==10||i==9)icon(canvas,i==9?bladeIcon:i==10?healIcon:campIcon,x-16,y-16,32);
                String name=room.name();int split=name.lastIndexOf(' ');
                if(name.length()>16&&split>0) {
                    pixelCentered(canvas,name.substring(0,split),x,y+42,2,color);
                    pixelCentered(canvas,name.substring(split+1),x,y+60,2,color);
                } else pixelCentered(canvas,name,x,y+42,2,color);
            if(i==game.map().room()) {
                canvas.setColor(TEAL);canvas.drawRect(x-w/2-4,y-h/2-4,w+8,h+8);
                canvas.setColor(Color.WHITE);
                int px=x-60+(int)(120*game.player().x()/game.map().worldWidth());
                int py=y-36+(int)(72*game.player().y()/game.map().worldHeight());
                canvas.fillRect(px-2,py-2,4,4);
            }
        }
    }

    private void drawLocalMap(Graphics2D canvas) {
        uiPanel(canvas,1038,536,222,166);
        pixelText(canvas,game.map().description().name(),1054,550,1,GOLD);
        pixelText(canvas,"TAB",1224,550,1,new Color(177,187,190));
        int ox=1058,oy=574;
        var area=game.map();
        for(int y=0;y<18;y++)for(int x=0;x<30;x++) {
            canvas.setColor(area.tileMask(x,y)>0?new Color(73,86,91):new Color(23,35,38));
            canvas.fillRect(ox+x*6,oy+y*6,6,6);
        }
        canvas.setColor(new Color(130,119,86));
        for(var obstacle:area.barriers())canvas.fillRect(ox+(int)((obstacle.centerX()-obstacle.width()/2)*180/area.worldWidth()),
                oy+(int)((obstacle.centerY()-obstacle.height()/2)*108/area.worldHeight()),
                Math.max(2,(int)(obstacle.width()*180/area.worldWidth())),Math.max(2,(int)(obstacle.height()*108/area.worldHeight())));
        for(var door:area.doors()) {
            int x=ox+(int)(door.x()*180/area.worldWidth()),y=oy+(int)(door.y()*108/area.worldHeight());
            canvas.setColor(area.passageOpen(door)?GOLD:RED);canvas.fillRect(x-3,y-3,6,6);
        }
        if(area.room()==9) {
            canvas.setColor(area.gateOpen()?GOLD:RED);
            canvas.fillRect(ox+(int)(area.gateX()*180/area.worldWidth())-3,oy+(int)(area.gateY()*108/area.worldHeight())-3,6,6);
            if(game.guardian().alive()) {
                canvas.setColor(RED);canvas.drawRect(ox+(int)(game.guardian().x()*180/area.worldWidth())-3,
                        oy+(int)(game.guardian().y()*108/area.worldHeight())-3,6,6);
            }
        }
        for(var enemy:game.scouts())if(enemy.alive()&&Math.hypot(enemy.x()-game.player().x(),enemy.y()-game.player().y())<360) {
            canvas.setColor(RED);canvas.fillRect(ox+(int)(enemy.x()*180/area.worldWidth())-1,
                    oy+(int)(enemy.y()*108/area.worldHeight())-1,3,3);
        }
        canvas.setColor(TEAL);int px=ox+(int)(game.player().x()*180/area.worldWidth()),py=oy+(int)(game.player().y()*108/area.worldHeight());
        canvas.fillRect(px-3,py-3,6,6);canvas.setColor(Color.WHITE);canvas.fillRect(px-1,py-1,2,2);
        pixelText(canvas,"N",1244,574,1,new Color(136,159,170));
    }

    private void drawPause(Graphics2D canvas) {
        canvas.setColor(new Color(4,9,15,225));canvas.fillRect(0,0,WIDTH,HEIGHT);
        uiPanel(canvas,300,96,680,532);
        pixelCentered(canvas,"PAUSED",640,136,4,GOLD);
        pixelCentered(canvas,game.map().description().name(),640,190,2,new Color(176,191,195));
        String[][] controls={{"WASD / ARROWS","MOVE"},{"LMB / RMB","SLASH / TIDE"},{"SPACE","DASH"},
                {"Q","TRANSFORM AT 100"},{"E","ABSORB / INTERACT"},{"TAB","HOLD MAP"},
                {"M",GameAudio.muted()?"SOUND OFF":"SOUND ON"},{"F",reducedEffects?"EFFECTS REDUCED":"EFFECTS FULL"}};
        for(int i=0;i<controls.length;i++) {
            pixelText(canvas,controls[i][0],348,244+i*32,2,GOLD);
            pixelText(canvas,controls[i][1],560,244+i*32,2,new Color(211,220,220));
        }
        pixelCentered(canvas,"ESC  RESUME",640,566,2,TEAL);
    }

    private void drawHealthBar(Graphics2D canvas, int centerX, int y,
            int width, int health, int maximum, Color color) {
        int x = centerX - width / 2;
        canvas.setColor(INK);
        canvas.fillRect(x - 2, y - 2, width + 4, 8);
        canvas.setColor(new Color(57, 61, 72));
        canvas.fillRect(x, y, width, 4);
        canvas.setColor(color);
        canvas.fillRect(x, y, width * health / maximum, 4);
    }

    private void drawScreenEffects(Graphics2D canvas) {
        if(reducedEffects) return;
        if (feedback.hitAlpha() > 0) {
            canvas.setColor(new Color(255, 255, 255, feedback.hitAlpha()));
            canvas.fillRect(0, 0, WIDTH, HEIGHT);
        }
        if (feedback.ichorAlpha() > 0) {
            canvas.setColor(new Color(46, 209, 205, feedback.ichorAlpha()));
            canvas.fillRect(0, 0, WIDTH, HEIGHT);
        }
        if (feedback.victoryAlpha() > 0) {
            canvas.setColor(new Color(230, 188, 92, feedback.victoryAlpha()));
            canvas.fillRect(0, 0, WIDTH, HEIGHT);
        }
        int damageAlpha = feedback.damageAlpha();
        if (game.player().health() == 1 && game.player().alive()) {
            damageAlpha = Math.max(damageAlpha,
                    35 + (int) (Math.abs(Math.sin(frameCounter * 0.08)) * 35));
        }
        if (damageAlpha > 0) {
            canvas.setColor(new Color(172, 29, 39, Math.min(210, damageAlpha)));
            canvas.fillRect(0, 0, WIDTH, 28);
            canvas.fillRect(0, HEIGHT - 28, WIDTH, 28);
            canvas.fillRect(0, 28, 28, HEIGHT - 56);
            canvas.fillRect(WIDTH - 28, 28, 28, HEIGHT - 56);
        }
    }

    private void drawBanner(Graphics2D canvas) {
        if(bannerTime<=0||bannerText.isEmpty()) return;
        canvas.setColor(PANEL);canvas.fillRect(250,156,780,44);
        pixelCentered(canvas,bannerText,640,170,2,GOLD);
    }
    private void drawStoryOverlay(Graphics2D canvas) {
        OutpostStory story=game.story();
        if(!story.blocksGameplay()) return;
        Color accent=story.phase()==OutpostStory.Phase.DEAD?RED:TEAL;
        canvas.setColor(new Color(7,10,16,220));canvas.fillRect(0,0,WIDTH,HEIGHT);
        uiPanel(canvas,160,152,960,420);
        pixelCentered(canvas,story.overlayTitle(),640,208,4,accent);
        String[] lines=story.overlayLines();
        for(int i=0;i<lines.length;i++)
            pixelCentered(canvas,lines[i],640,310+i*52,2,i==lines.length-1?GOLD:Color.WHITE);
    }
    private void pixelText(Graphics2D canvas,String text,int x,int y,int scale,Color color) {
        canvas.setColor(color);
        if(pixelFont==null) { canvas.setFont(SMALL_FONT);canvas.drawString(text,x,y+14);return; }
        for(int i=0;i<text.length();i++) {
            int glyph=GLYPHS.indexOf(Character.toUpperCase(text.charAt(i)));
            if(glyph<0) glyph=0;
            for(int row=0;row<7;row++) for(int col=0;col<5;col++)
                if((pixelFont.getRGB(glyph*5+col,row)>>>24)>0)
                    canvas.fillRect(x+i*6*scale+col*scale,y+row*scale,scale,scale);
        }
    }
    private void pixelCentered(Graphics2D canvas,String text,int center,int y,int scale,Color color) {
        pixelText(canvas,text,center-text.length()*6*scale/2,y,scale,color);
    }

    private void drawTerrain(Graphics2D canvas, int cameraX, int cameraY) {
        if (terrainTiles == null) {
            return;
        }
        RuinedOutpostMap map = game.map();
        int firstColumn = Math.max(0,cameraX / RuinedOutpostMap.TILE_SIZE);
        int firstRow = Math.max(0,cameraY / RuinedOutpostMap.TILE_SIZE);
        int lastColumn = Math.min(map.widthInTiles() - 1,
                (cameraX + WIDTH) / RuinedOutpostMap.TILE_SIZE);
        int lastRow = Math.min(map.heightInTiles() - 1,
                (cameraY + HEIGHT) / RuinedOutpostMap.TILE_SIZE);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                int x = column * RuinedOutpostMap.TILE_SIZE - cameraX;
                int y = row * RuinedOutpostMap.TILE_SIZE - cameraY;
                canvas.drawImage(terrainTiles[map.tileMask(column,row)],x,y,null);
            }
        }
    }

    static BufferedImage[] renderTiles(BufferedImage terrainSheet) {
        if (terrainSheet == null) {
            return null;
        }
        BufferedImage[] tiles = new BufferedImage[16];
        for (int mask = 0; mask < tiles.length; mask++) {
            tiles[mask] = new BufferedImage(RuinedOutpostMap.TILE_SIZE,
                    RuinedOutpostMap.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = tiles[mask].createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int sourceX = WangTileset.sourceX(mask);
            int sourceY = WangTileset.sourceY(mask);
            canvas.drawImage(terrainSheet,
                    0, 0, RuinedOutpostMap.TILE_SIZE, RuinedOutpostMap.TILE_SIZE,
                    sourceX, sourceY, sourceX + 32, sourceY + 32, null);
            canvas.dispose();
        }
        return tiles;
    }

    private static BufferedImage tintRemnant(BufferedImage source) {
        if(source==null) return null;
        BufferedImage result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int pixel=source.getRGB(x,y),a=pixel>>>24,r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
            if(a==0) continue;
            int light=(r+g+b)/3;
            Color shade=light>205?new Color(198,166,209):light>120?new Color(127,79,145)
                    :light>70?new Color(79,51,97):new Color(31,24,45);
            result.setRGB(x,y,(a<<24)|(shade.getRGB()&0xffffff));
        }
        return result;
    }

    private static int cameraPosition(double playerPosition,int viewportSize,int worldSize) {
        int margin=viewportSize==HEIGHT?112:0;
        return (int)Math.round(Math.max(-margin,Math.min(worldSize-viewportSize+margin,
                playerPosition-viewportSize/2.0))/2)*2;
    }

    static BufferedImage[] outdoorTiles(BufferedImage[] source) {
        if(source==null) return null;
        BufferedImage[] tiles=new BufferedImage[16];
        for(int mask=0;mask<16;mask++) {
            BufferedImage tile=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
                double u=x/31.0,v=y/31.0;
                double blend=((mask&1)!=0?(1-u)*(1-v):0)+((mask&2)!=0?u*(1-v):0)
                        +((mask&4)!=0?(1-u)*v:0)+((mask&8)!=0?u*v:0);
                int hash=(x/3*31+y/2*17)%29;
                int earth=hash<2?0xff4a4b3e:hash<6?0xff373d35:0xff303630;
                int pixel=blend>=0.5?source[15].getRGB(x*2,y*2):earth;
                for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)tile.setRGB(x*2+dx,y*2+dy,pixel);
            }
            tiles[mask]=tile;
        }
        return tiles;
    }

    private void bindKeys() {
        bind(KeyEvent.VK_A,"left",value->{});
        bind(KeyEvent.VK_LEFT,"leftArrow",value->{});
        bind(KeyEvent.VK_D,"right",value->{});
        bind(KeyEvent.VK_RIGHT,"rightArrow",value->{});
        bind(KeyEvent.VK_W,"up",value->{});
        bind(KeyEvent.VK_UP,"upArrow",value->{});
        bind(KeyEvent.VK_S,"down",value->{});
        bind(KeyEvent.VK_DOWN,"downArrow",value->{});
        bind(KeyEvent.VK_SPACE,"dash",value->{if(value) dash();});
        bind(KeyEvent.VK_Q,"transform",value->{if(value) transform();});
        bind(KeyEvent.VK_ENTER,"begin",value->{if(value) game.begin();});
        bind(KeyEvent.VK_E,"interact",value->{if(value) game.interact();});
        bind(KeyEvent.VK_ESCAPE,"pause",value->{if(value){game.togglePause();attackHeld=false;}});
        bind(KeyEvent.VK_TAB,"map",value->mapShown=value);
        bind(KeyEvent.VK_M,"mute",value->{if(value) GameAudio.setMuted(!GameAudio.muted());});
        bind(KeyEvent.VK_F,"effects",value->{if(value) reducedEffects=!reducedEffects;});
        bind(KeyEvent.VK_R,"restart",value->{
            if(value&&(game.story().phase()==OutpostStory.Phase.DEAD
                    ||game.story().phase()==OutpostStory.Phase.COMPLETE)) restartGame();
        });
    }
    private boolean held(int a,int b) { return pressedKeys.contains(a)||pressedKeys.contains(b); }
    private int horizontal() {
        return (held(KeyEvent.VK_D,KeyEvent.VK_RIGHT)?1:0)-(held(KeyEvent.VK_A,KeyEvent.VK_LEFT)?1:0);
    }
    private int vertical() {
        return (held(KeyEvent.VK_S,KeyEvent.VK_DOWN)?1:0)-(held(KeyEvent.VK_W,KeyEvent.VK_UP)?1:0);
    }
    private void clearInput() { pressedKeys.clear();attackHeld=false;mapShown=false; }

    private void dash() {
        int horizontal = horizontal();
        int vertical = vertical();
        if (horizontal == 0 && vertical == 0) {
            horizontal = game.player().bladeForm()
                    ? bladeAnimation.facingHorizontal()
                    : slimeAnimation.facingHorizontal();
            vertical = game.player().bladeForm()
                    ? bladeAnimation.facingVertical()
                    : slimeAnimation.facingVertical();
        }
        game.dash(horizontal, vertical);
    }

    private void attack() {
        attack(false);
    }
    private void attack(boolean heavy) {
        Player player = game.player();
        int facingX = player.bladeForm()
                ? bladeAnimation.facingHorizontal() : slimeAnimation.facingHorizontal();
        int facingY = player.bladeForm()
                ? bladeAnimation.facingVertical() : slimeAnimation.facingVertical();
        if(mouseAimed) {
            double dx=mouseX+cameraPosition(player.x(),WIDTH,game.map().worldWidth())-player.x();
            double dy=mouseY+cameraPosition(player.y(),HEIGHT,game.map().worldHeight())-player.y();
            double angle=Math.atan2(dy,dx);
            facingX=(int)Math.round(Math.cos(angle)*1000);
            facingY=(int)Math.round(Math.sin(angle)*1000);
        }
        if (!(heavy?game.tideWave(facingX,facingY):game.attack(facingX, facingY))) {
            return;
        }
        if (player.bladeForm()) {
            bladeAnimation.face(facingX,facingY);
            bladeAnimation.slash();
        } else {
            slimeAnimation.face(facingX,facingY);
            slimeAnimation.attack();
        }
        attackFacingX = facingX;
        attackFacingY = facingY;
        attackWasBlade = player.bladeForm();
        attackEffectTime = 0.3;
    }

    private void transform() {
        game.transform();
    }

    private void restartGame() {
        game.restart();
        clearInput(); trails.clear();
        slimeAnimation = new SlimeAnimation();
        bladeAnimation = new BladeAnimation();
        transformationAnimation = new TransformationAnimation();
        feedback = new CombatFeedback();
        impacts.clear();
        attackEffectTime = 0;
        bannerText = "";
        bannerTime = 0;
    }

    private void banner(String text, double seconds) {
        bannerText = text;
        bannerTime = seconds;
    }

    private void bind(int keyCode, String name, KeyState keyState) {
        InputMap inputs = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getActionMap();
        inputs.put(KeyStroke.getKeyStroke(keyCode, 0, false), name + "Pressed");
        inputs.put(KeyStroke.getKeyStroke(keyCode, 0, true), name + "Released");
        actions.put(name+"Pressed",action(()->{
            if(pressedKeys.add(keyCode)) keyState.set(true);
        }));
        actions.put(name+"Released",action(()->{
            pressedKeys.remove(keyCode);keyState.set(false);
        }));
    }

    private static AbstractAction action(Runnable runnable) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        };
    }

    private static BufferedImage loadSlimeSheet() {
        return loadImage("assets/characters/slime/slime_sprite_sheet.png");
    }

    static BufferedImage loadImage(String path) {
        try (InputStream resource = B2BJ.class.getResourceAsStream("/" + path)) {
            if (resource != null) {
                return ImageIO.read(resource);
            }
        } catch (IOException error) {
            return null;
        }
        try {
            return ImageIO.read(Path.of(path).toFile());
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }

    @FunctionalInterface
    private interface KeyState {
        void set(boolean value);
    }

    private enum Fx {
        WATER(32,.33),TIDE(48,.33),BLADE(48,.32),SLIME_HURT(48,.38),BLADE_HURT(48,.38),
        PICKUP(32,.32),STONE(32,.45),SPIT(48,.3),HEAL(48,.65);
        final int cell;final double duration;
        Fx(int cell,double duration){this.cell=cell;this.duration=duration;}
    }
    private static final class Impact {
        private final double x;
        private final double y;
        private final Fx kind;
        private final int variant;
        private double time;
        private Impact(double x,double y,Fx kind,int variant) {
            this.x = x;
            this.y = y;
            this.kind=kind;this.variant=variant;
        }
    }
}
