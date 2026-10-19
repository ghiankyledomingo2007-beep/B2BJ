import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.InputEvent;
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
import javax.swing.JButton;
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

    private final RuinedOutpostGame game;
    private final JButton menuButton = new PixelSwing.Button("Menu", Ui.Icon.MENU);
    private CampaignMenu menu;
    private TitleMenu titleMenu;
    private final TitleScreen titleScreen = new TitleScreen();
    private double titleTime;
    private int cardBiome = -1;
    private double cardTime = 99;
    private OutpostStory.Phase lastPhase;
    private final LoadingScreen loading = new LoadingScreen();
    private double loadingClock;
    private int seenBiome = -1;
    private final BufferedImage[] terrainTiles = loadEnvironmentTiles();
    private final BufferedImage[] bankTiles=renderTiles(loadImage("assets/tilesets/ruined_outpost/earth_banks.png"));
    private final BufferedImage slimeSheet = loadSlimeSheet();
    private final BufferedImage slimeIdleSheet = loadImage("assets/characters/slime/slime_idle.png");
    private final BufferedImage slimeCastSheet=loadImage("assets/characters/slime/slime_cast.png");
    private final BufferedImage slimeFinisherSheet=loadImage("assets/characters/slime/slime_finisher.png");
    private final BufferedImage membraneSheet=loadImage("assets/effects/guard_membrane.png");
    private final BufferedImage jetSheet=loadImage("assets/effects/spitter_current.png");
    private final java.util.Map<BladeAnimation.Action,BufferedImage> rainoraySheets=new java.util.EnumMap<>(BladeAnimation.Action.class);
    /** The sword's swoosh, a layer of its own over the body: cell for cell with the blade sheet, twice the size. */
    private final java.util.Map<BladeAnimation.Action,BufferedImage> bladeTrailSheets=new java.util.EnumMap<>(BladeAnimation.Action.class);
    static final int TRAIL_CELL=BladeAnimation.CELL_WIDTH*2;
    private final BufferedImage transformInSheet=loadImage("assets/effects/transform_in.png");
    private final BufferedImage transformOutSheet=loadImage("assets/effects/transform_out.png");
    private final BufferedImage engulfSheet=loadImage("assets/characters/slime/slime_engulf.png");
    private final BufferedImage aimReticle=loadImage("assets/effects/aim_reticle.png");
    private final BufferedImage crescentSheet=loadImage("assets/effects/ichor_crescent.png");
    private final BufferedImage riposteSheet=loadImage("assets/effects/riposte_guard.png");
    private final BufferedImage counterSheet=loadImage("assets/effects/riposte_counter.png");
    private final BufferedImage crescentIcon=loadImage("assets/ui/crescent.png");
    private final BufferedImage riposteIcon=loadImage("assets/ui/riposte.png");
    private final Telegraphs tells=new Telegraphs(WIDTH/2,HEIGHT/2);
    private final BufferedImage waterBuffer = new BufferedImage(72,72,BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage tideBuffer = new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage tideFrontSheet=loadImage("assets/effects/tide_front.png");
    private final BufferedImage waterFinisherSheet=loadImage("assets/effects/water_finisher.png");
    private final BufferedImage finisherImpactSheet=loadImage("assets/effects/water_finisher_impact.png");
    private final BufferedImage waterSlashSheet = loadImage("assets/effects/water_slash.png");
    private final BufferedImage tideWaveSheet = loadImage("assets/effects/tide_wave.png");
    private final BufferedImage waterSplashSheet = loadImage("assets/effects/water_splash.png");
    private final BufferedImage waterChargeSheet = loadImage("assets/effects/water_charge.png");
    private final BufferedImage waterWakeSheet = loadImage("assets/effects/water_wake.png");
    private final BufferedImage tideImpactSheet = loadImage("assets/effects/tide_impact.png");
    private final BufferedImage tideCrestSheet=loadImage("assets/effects/tide_crest.png");
    private final BufferedImage tideReleaseSheet=loadImage("assets/effects/tide_release.png");
    private final BufferedImage tideBreakSheet=loadImage("assets/effects/tide_break.png");
    private final BufferedImage tideFoamSheet=loadImage("assets/effects/tide_foam.png");
    private final BufferedImage wardenImpactSheet=loadImage("assets/effects/warden_impact.png");
    private final BufferedImage briarImpactSheet=loadImage("assets/effects/briar_impact.png");
    private final BufferedImage remnantSheet = tintRemnant(slimeSheet);
    private final BufferedImage wispWalkSheet = loadImage(
            "assets/characters/wisp/wisp_walk.png");
    private final BufferedImage remnantWalkSheet=loadImage("assets/characters/wisp/remnant_walk.png");
    private final BufferedImage remnantAttackSheet=loadImage("assets/characters/wisp/remnant_attack.png");
    /** Forest enemies in the remnants' sheet format (tools/art/Creatures.java): {walk, attack} per kind. */
    private final java.util.Map<CampaignEnemy.Kind,BufferedImage[]> kindSheets=forestSheets();
    private static java.util.Map<CampaignEnemy.Kind,BufferedImage[]> forestSheets() {
        var sheets=new java.util.EnumMap<CampaignEnemy.Kind,BufferedImage[]>(CampaignEnemy.Kind.class);
        for(var kind:new CampaignEnemy.Kind[]{CampaignEnemy.Kind.RIFT_IMP,CampaignEnemy.Kind.THORN_WOLF,CampaignEnemy.Kind.MIRE_SHAMAN}) {
            String base="assets/characters/forest/"+kind.name().toLowerCase(java.util.Locale.ROOT);
            var walk=loadImage(base+"_walk.png");var attack=loadImage(base+"_attack.png");
            if(walk!=null&&attack!=null)sheets.put(kind,new BufferedImage[]{walk,attack});
        }
        return sheets;
    }
    private final BufferedImage wispAttackSheet = loadImage(
            "assets/characters/wisp/wisp_attack.png");
    private final BufferedImage guardianSheet = loadImage(
            "assets/characters/guardian/guardian_actions.png");
    private final BufferedImage wardenMotionSheet=loadImage("assets/characters/guardian/warden_motion.png");
    private final BufferedImage briarheartSheet=loadImage("assets/characters/guardian/briarheart_motion.png");
    private WardenFx wardenFx=new WardenFx(wardenMotionSheet);
    /** The boss effects for the area being played: the Warden's cyan, or Briarheart's gold and violet. */
    private WardenFx bossFx(){return game.campaignMode()&&game.biome()==1?new WardenFx(briarheartSheet,WardenFx.Theme.BRIARHEART):new WardenFx(wardenMotionSheet);}
    private final MobFx mobFx=new MobFx();
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
    private final java.util.Map<RuinedOutpostMap.Decoration,BufferedImage> dressingSprites=
            new java.util.EnumMap<>(RuinedOutpostMap.Decoration.class);
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
    private String bannerText = "";
    private double bannerTime;
    private long frameCounter;
    private double weatherTime;
    private final BufferedImage rainStreakSheet=loadImage("assets/effects/rain_streaks.png");
    private final BufferedImage rainSplashSheet=loadImage("assets/effects/rain_splashes.png");
    private static final Color[] RAIN_INK={new Color(122,155,175,26),new Color(133,169,187,42),new Color(158,190,204,62)};
    private long previousFrame = System.nanoTime();

    B2BJ(boolean startTimer) {
        this(startTimer,new RuinedOutpostGame());
    }

    B2BJ(boolean startTimer,RuinedOutpostGame game) {
        this.game=java.util.Objects.requireNonNull(game);
        for(var action:BladeAnimation.Action.values())rainoraySheets.put(action,loadImage(
                "assets/characters/blade/rainoray_"+action.name().toLowerCase(java.util.Locale.ROOT)+".png"));
        for(var action:new BladeAnimation.Action[]{BladeAnimation.Action.SLASH,BladeAnimation.Action.CLEAVE,
                BladeAnimation.Action.CAST,BladeAnimation.Action.COUNTER})
            bladeTrailSheets.put(action,loadImage("assets/effects/rainoray_"+action.name().toLowerCase(java.util.Locale.ROOT)+"_trail.png"));
        for(var prop:RuinedOutpostMap.Prop.values())propSprites.put(prop,loadImage(prop.path()));
        for(var decoration:RuinedOutpostMap.Decoration.values())dressingSprites.put(decoration,loadImage(decoration.path()));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BACKGROUND);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        if(game.campaignMode()) {
            setLayout(null);
            menu = new CampaignMenu(game,this::closeMenu,()->{
                if(game.returnToTitle()) {resetVisualState();closeMenu();}
            },enabled->reducedEffects=enabled,()->reducedEffects);
            menu.setVisible(false);
            menuButton.setMnemonic(KeyEvent.VK_G);
            menuButton.setToolTipText("Open Settings, Admin / Testing, or return to title");
            menuButton.getAccessibleContext().setAccessibleName("Game menu");
            menuButton.addActionListener(event->{if(menuShown())closeMenu();else openMenu();});
            add(menu);add(menuButton);
            titleMenu = new TitleMenu(game::saveAvailable,
                    () -> { var from=snapshot(); if(game.continueCampaign()) { resetVisualState(); startLoading(from,LoadingScreen.Kind.ENTER); } },
                    () -> {
                        var from=snapshot();
                        if(game.saveAvailable())game.newCampaign(); else game.begin();
                        resetVisualState();
                        if(game.story().phase()!=OutpostStory.Phase.PROLOGUE)startLoading(from,LoadingScreen.Kind.ENTER);
                    },
                    () -> { openMenu(); menu.settings(); },
                    () -> {
                        var window=SwingUtilities.getWindowAncestor(this);
                        if(window!=null)window.dispatchEvent(new java.awt.event.WindowEvent(window,java.awt.event.WindowEvent.WINDOW_CLOSING));
                    });
            titleMenu.setVisible(false);
            add(titleMenu);
        }
        bindKeys();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if(loading.covering()) { loading.skip(); return; }
                if(menuShown())return;
                if (event.getButton() != MouseEvent.BUTTON1 && event.getButton() != MouseEvent.BUTTON3) {
                    return;
                }
                if (game.story().phase() == OutpostStory.Phase.PROLOGUE) {
                    if(!titleArt())game.begin();   // with the title art, its buttons start the game, not stray clicks
                } else if (!game.blocked()) {
                    mouseX=event.getX(); mouseY=event.getY(); mouseAimed=true;
                    if(event.getButton()==MouseEvent.BUTTON1) {
                        attackHeld=true;
                        if(game.player().bladeForm()) {int[] aim=aimDirection();game.primaryPress(aim[0],aim[1]);}
                        else attack();
                    }
                    else attack(true);
                }
            }
            @Override public void mouseReleased(MouseEvent event) {
                if (event.getButton()==MouseEvent.BUTTON1) {
                    if(attackHeld&&game.player().bladeForm()) {
                        mouseX=event.getX();mouseY=event.getY();int[] aim=aimDirection();game.primaryRelease(aim[0],aim[1]);
                    }
                    attackHeld=false;
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                if(menuShown())return;
                mouseX=event.getX(); mouseY=event.getY(); mouseAimed=true;
                // Only fresh mouse input changes idle facing; a parked cursor must not undo movement.
                if(game.player().bladeForm()&&!game.blocked()&&horizontal()==0&&vertical()==0
                        &&!game.player().dashing()&&!game.bladeAttacking()&&bladeAnimation.canAim()) {
                    int[] aim=aimDirection();bladeAnimation.face(aim[0],aim[1]);
                }
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
            B2BJ panel = new B2BJ(true,RuinedOutpostGame.campaign(CampaignSave.defaultPath()));
            JFrame window = new JFrame("B2BJ — Blob to Blade");
            window.setUndecorated(java.util.Arrays.asList(args).contains("--borderless"));
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.add(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            GameAudio.startMusic();
            panel.requestFocusInWindow();
        });
    }

    RuinedOutpostGame game() {
        return game;
    }

    @Override public void doLayout() {
        super.doLayout();
        if(menu==null)return;
        menuButton.setBounds(Math.max(8,getWidth()-140),16,124,40);
        if(titleMenu!=null) { titleMenu.setBounds(56,300,330,206); titleMenu.doLayout(); }
        int width=Math.min(menu.getPreferredSize().width,Math.max(1,getWidth()-32));
        int height=Math.min(menu.getPreferredSize().height,Math.max(1,getHeight()-32));
        menu.setBounds((getWidth()-width)/2,(getHeight()-height)/2,width,height);
        menu.doLayout();
    }

    private boolean menuShown() { return menu!=null&&menu.isVisible(); }

    /** The current frame as an image: what the loading goo floods over. */
    private BufferedImage snapshot() {
        var image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();
        paint(g);
        g.dispose();
        return image;
    }

    /** Starts a loading screen; the region card is not repeated after it (the screen names the region). */
    private void startLoading(BufferedImage from,LoadingScreen.Kind kind) {
        if(!game.campaignMode())return;
        clearInput();
        loading.start(from,kind,game.biome());
        loadingClock=0;
        cardBiome=game.biome();cardTime=99;lastPhase=game.story().phase();seenBiome=game.biome();
        syncTitle();
    }

    /** A region change from anywhere (gates, the testing menu) gets a loading screen, already covered. */
    private void watchRegion() {
        if(!game.campaignMode())return;
        if(game.story().phase()==OutpostStory.Phase.PROLOGUE) { seenBiome=-1; return; }
        if(seenBiome>=0&&game.biome()!=seenBiome&&!loading.active())startLoading(null,LoadingScreen.Kind.ENTER);
        seenBiome=game.biome();
    }

    /** True when Rainoray stands where E could take him to another region (so a snapshot is worth taking). */
    private boolean nearPassage() {
        if(!game.campaignMode())return false;
        var area=game.campaignArea();
        double x=game.player().x(),y=game.player().y();
        return Math.hypot(area.exit().x()-x,area.exit().y()-y)<260||Math.hypot(area.returnPortal().x()-x,area.returnPortal().y()-y)<260;
    }

    /** A region's title card plays when a run starts and whenever Rainoray enters another region. */
    private void areaCard(double seconds) {
        if(!game.campaignMode())return;
        var phase=game.story().phase();
        if(phase!=OutpostStory.Phase.PROLOGUE&&(game.biome()!=cardBiome||lastPhase==OutpostStory.Phase.PROLOGUE)) {
            cardBiome=game.biome();cardTime=0;
        }
        lastPhase=phase;
        cardTime+=seconds;
    }
    private boolean titleArt() { return game.campaignMode()&&TitleScreen.available(); }
    private boolean titleShown() { return titleMenu!=null&&titleMenu.isVisible(); }

    /** Title menu shows on the title screen while the game menu is closed; the corner Menu button does not. */
    private void syncTitle() {
        if(titleMenu==null)return;
        boolean title=titleArt()&&game.story().phase()==OutpostStory.Phase.PROLOGUE;
        boolean show=title&&!menuShown();
        if(titleMenu.isVisible()!=show) {
            titleMenu.setVisible(show);
            if(show) { titleMenu.refresh(); titleMenu.focusDefault(); titleTime=0; }
        }
        boolean corner=!title&&!loading.active();
        if(menuButton.isVisible()!=corner)menuButton.setVisible(corner);
        if(show)titleMenu.refresh();
    }
    private void openMenu() {
        clearInput();game.pause();
        GameAudio.setMusicState(game.biome(),game.player().bladeForm(),false);
        GameAudio.play(GameAudio.Cue.UI_OPEN);
        menu.home();menu.setVisible(true);menuButton.setText("Close");
        doLayout();revalidate();repaint();menu.focusFirst();
    }
    private void closeMenu() {
        clearInput();menu.setVisible(false);menuButton.setText("Menu");
        GameAudio.play(GameAudio.Cue.UI_CLOSE);
        if(game.paused())game.togglePause();
        requestFocusInWindow();repaint();
    }

    private void updateGame(ActionEvent event) {
        // A javax.swing.Timer that throws once never fires again: the game would freeze for good.
        try { tick(); }
        catch (RuntimeException failure) { failure.printStackTrace(); }
    }

    private void tick() {
        long now = System.nanoTime();
        double seconds = Math.min((now - previousFrame) / 1_000_000_000.0, 0.05);
        previousFrame = now;

        if(game.campaignMode())GameAudio.setMusicState(game.biome(),game.player().bladeForm(),!game.blocked());
        if (game.paused()||!game.campaignMode()||game.blocked()) { GameAudio.stopLoops(); repaint(); return; }
        step(seconds);
    }

    void step(double seconds) {
        if (Double.isFinite(seconds) && seconds > 0) titleTime += seconds;
        syncTitle();
        if (loading.active() && Double.isFinite(seconds) && seconds > 0) {
            loadingClock += seconds;
            loading.update(seconds);
            if (loading.covering()) return;                         // the world waits behind the screen
        }
        watchRegion();
        if (!Double.isFinite(seconds) || seconds <= 0 || game.paused() || menuShown()) return;
        areaCard(seconds);
        weatherTime+=seconds;
        int horizontal = horizontal();
        int vertical = vertical();
        int[] aim=aimDirection();
        if (attackHeld&&!game.player().bladeForm()) attack();
        game.update(seconds, horizontal, vertical);

        int animationX = game.blocked() ? 0 : horizontal;
        int animationY = game.blocked() ? 0 : vertical;
        Player player = game.player();
        if (player.recovering()) { animationX=0; animationY=0; }
        if (player.dashing()&&(!reducedEffects||frameCounter%3==0))trails.add(new double[]{player.x(),player.y(),0});
        for (double[] trail:trails) trail[2]+=seconds;
        trails.removeIf(trail->trail[2]>0.14);
        if(!player.alive()||horizontal!=0||vertical!=0)transformationAnimation.cancel();
        boolean morphing=transformationAnimation.active();
        transformationAnimation.update(seconds);
        if (!player.alive()) {
            if(player.bladeForm())bladeAnimation.pose(BladeAnimation.Action.HURT,.20,.32);
        } else if(morphing) {
            // Start the revealed pose at its authored endpoint, not an invisible running clock.
            if(!transformationAnimation.active()) {
                bladeAnimation.play(BladeAnimation.Action.IDLE,0);
                slimeAnimation = new SlimeAnimation();
                slimeAnimation.face(bladeAnimation.facingHorizontal(),bladeAnimation.facingVertical());
            }
        } else if (player.bladeForm()) {
            if(game.bladeAttacking()) {
                bladeAnimation.face(game.bladeAimX(),game.bladeAimY());
                bladeAnimation.pose(bladeAction(game.bladeMove()),
                        game.bladeElapsed(),RuinedOutpostGame.strikeDuration(game.bladeMove()));
            } else if(game.bladeCountering()) {
                bladeAnimation.face(game.bladeAimX(),game.bladeAimY());
                bladeAnimation.pose(BladeAnimation.Action.COUNTER,game.counterElapsed(),RuinedOutpostGame.COUNTER_DURATION);
            } else if(game.bladeCasting()) {
                bladeAnimation.face(game.bladeAimX(),game.bladeAimY());
                bladeAnimation.pose(BladeAnimation.Action.CAST,game.bladeCastElapsed(),RuinedOutpostGame.BLADE_CAST_DURATION);
            } else if(game.bladePreparing()) {
                bladeAnimation.face(aim[0],aim[1]);
                double held=game.chargeSeconds();
                bladeAnimation.pose(held<.35?BladeAnimation.Action.CHARGE:BladeAnimation.Action.HOLD,held<.35?held:held-.35,0);
            } else {
                bladeAnimation.update(player.dashing()?game.dashAimX():animationX,
                        player.dashing()?game.dashAimY():animationY,player.dashing(),seconds);
                footsteps();
            }
            if(game.bladePreparing())GameAudio.startLoop(GameAudio.Loop.CHARGE,Math.min(1,game.chargeSeconds()/RuinedOutpostGame.FULL_CHARGE));
            else GameAudio.stopLoop(GameAudio.Loop.CHARGE);
        } else {
            slimeAnimation.update(animationX, animationY, player.dashing(), seconds);
            slimeHops();
        }
        deathAndFadeCues(player);
        if(!player.bladeForm())GameAudio.stopLoop(GameAudio.Loop.CHARGE);
        if(player.alive()&&player.health()<=LOW_HEALTH)GameAudio.startLoop(GameAudio.Loop.HEARTBEAT,1);
        else GameAudio.stopLoop(GameAudio.Loop.HEARTBEAT);
        GameAudio.startLoop(GameAudio.Loop.AMBIENCE,1);
        bossMusic(seconds);
        feedback.update(seconds);
        updateImpacts(seconds);
        if(wardenArea())wardenFx.update(seconds,game.guardian());
        mobFx.update(seconds,game);
        bannerTime = Math.max(0, bannerTime - seconds);
        handleEvents(game.drainEvents());
        if(eating&&game.absorptionProgress()==0) {             // moved, hit or transformed mid-eat
            GameAudio.stopLoop(GameAudio.Loop.EATING);
            eating=false;
        }
        frameCounter++;
        repaint();
    }


    /** The recording each blade move is heard with; the moves are RuinedOutpostGame's numbering. */
    static GameAudio.Cue bladeCue(int move) {
        return switch(move) {
            case 1 -> GameAudio.Cue.THRUST;
            case 3 -> GameAudio.Cue.SLASH_HEAVY;
            case 4 -> GameAudio.Cue.CLEAVE;
            case RuinedOutpostGame.DASH_SLASH -> GameAudio.Cue.DASH_SLASH;
            case RuinedOutpostGame.SPIN_CLEAVE -> GameAudio.Cue.SPIN_CLEAVE;
            default -> GameAudio.Cue.SLASH;
        };
    }

    private static final double SWING_LEAD = .08;                // the whoosh starts this long before the blade lands
    private static final double BLADE_FADE_WARNING = 3.0;        // seconds of Blade left when the warning plays
    private boolean wasAlive = true, bladeWarned;
    private int lastHopFrame = -1;

    /** One squelch per hop while the blob travels under its own power. */
    private void slimeHops() {
        int frame = slimeAnimation.hopping() ? slimeAnimation.frame() : -1;
        if(frame != lastHopFrame && frame == 0) GameAudio.play(GameAudio.Cue.SLIME_MOVE);
        lastHopFrame = frame;
    }

    /** The two cues that come from state rather than an event: dying, and the Blade about to fade. */
    private void deathAndFadeCues(Player player) {
        boolean alive = player.alive();
        if(wasAlive && !alive) {
            GameAudio.stopLoops();
            GameAudio.play(player.bladeForm() ? GameAudio.Cue.DEATH_BLADE : GameAudio.Cue.DEATH_SLIME);
        }
        wasAlive = alive;
        if(!player.bladeForm()) bladeWarned = false;
        else if(!bladeWarned && alive && player.bladeSeconds() > 0 && player.bladeSeconds() < BLADE_FADE_WARNING) {
            bladeWarned = true;
            GameAudio.play(GameAudio.Cue.BLADE_FADES);
        }
    }
    private static final int LOW_HEALTH = 1;
    private static final double BOSS_MUSIC_RANGE = 900;          // px from the Warden; he wakes at 600
    private static final double BOSS_FADE_IN = 1.0, BOSS_FADE_OUT = 1.5;   // seconds
    private double bossMusicLevel;
    private boolean eating;

    /** The Warden's track: fades in while the fight is on and near, out when he dies, you die or run off. */
    private void bossMusic(double seconds) {
        Guardian warden=game.guardian();
        Player player=game.player();
        boolean fighting=game.bossActive()&&warden.alive()&&player.alive()
                &&Math.hypot(warden.x()-player.x(),warden.y()-player.y())<=BOSS_MUSIC_RANGE;
        // something else stopped every loop (death, victory, pause): start again from silence, not full blast
        if(bossMusicLevel>0&&!GameAudio.loopRunning(GameAudio.Loop.BOSS_MUSIC))bossMusicLevel=0;
        bossMusicLevel=fighting?Math.min(1,bossMusicLevel+seconds/BOSS_FADE_IN)
                :Math.max(0,bossMusicLevel-seconds/BOSS_FADE_OUT);
        if(bossMusicLevel>0)GameAudio.startLoop(GameAudio.Loop.BOSS_MUSIC,bossMusicLevel);
        else GameAudio.stopLoop(GameAudio.Loop.BOSS_MUSIC);
    }                     // hearts left when the heartbeat starts
    private static final int[] RUN_CONTACT_FRAMES = {1, 5};      // of the 8-frame run: each foot lands once
    private int lastRunFrame = -1;

    /** A footstep the first time the run shows a foot landing; nothing while he stands or turns. */
    private void footsteps() {
        int frame = bladeAnimation.action()==BladeAnimation.Action.RUN ? bladeAnimation.frame() : -1;
        if(frame!=lastRunFrame && frame>=0 && java.util.Arrays.stream(RUN_CONTACT_FRAMES).anyMatch(f->f==frame))
            GameAudio.play(GameAudio.Cue.FOOTSTEP);
        lastRunFrame = frame;
    }

    private void handleEvents(List<RuinedOutpostGame.Event> events) {
        for (RuinedOutpostGame.Event event : events) {
            switch(event.type()) {
                case ATTACK,DASH,PLAYER_HIT,CRESCENT_CAST,RIPOSTE_START -> transformationAnimation.cancel();
                default -> { }
            }
            switch (event.type()) {
                case DASH -> GameAudio.play(GameAudio.Cue.DASH);
                case ATTACK -> {
                    if(game.player().bladeForm()) {
                        bladeAnimation.face(game.bladeAimX(),game.bladeAimY());
                        bladeAnimation.pose(bladeAction(game.bladeMove()),
                                game.bladeElapsed(),RuinedOutpostGame.strikeDuration(game.bladeMove()));
                    }
                    if(game.player().bladeForm()) {
                        GameAudio.play(bladeCue(game.bladeMove()),Math.max(0,RuinedOutpostGame.strikeWindup(game.bladeMove())-SWING_LEAD));
                        if(RuinedOutpostGame.strikeKind(game.bladeMove())>=4)GameAudio.duck(.4,.4);
                    } else GameAudio.play(GameAudio.Cue.BLOB_ATTACK);
                }
                case CHARGE_READY -> GameAudio.play(GameAudio.Cue.CHARGE_READY);
                case WATER_IMPACT -> {
                    addImpact(event.x(),event.y(),Fx.WATER);
                    GameAudio.play(GameAudio.Cue.WATER_SPLASH);
                }
                case FINISHER_RELEASE -> GameAudio.play(GameAudio.Cue.FINISHER_RELEASE);
                case FINISHER_IMPACT -> {
                    addImpact(event.x(),event.y(),Fx.FINISHER);
                    GameAudio.play(GameAudio.Cue.FINISHER_IMPACT);
                    GameAudio.duck(.4,.4);
                }
                case TRAIT_GAINED -> GameAudio.play(GameAudio.Cue.TRAIT_GAINED);
                case MEMBRANE_BREAK -> {
                    addImpact(event.x(),event.y(),Fx.BLADE);
                    GameAudio.play(GameAudio.Cue.MEMBRANE_BREAK);
                }
                case TIDE_RELEASE -> {
                    addImpact(event.x(),event.y(),Fx.TIDE_RELEASE);
                    GameAudio.play(GameAudio.Cue.TIDE_RELEASE);
                }
                case TIDE_IMPACT -> {
                    addImpact(event.x(),event.y(),Fx.TIDE);
                    GameAudio.play(GameAudio.Cue.TIDE_IMPACT);
                }
                case TIDE_DISSIPATE -> {
                    addImpact(event.x(),event.y(),Fx.TIDE_FOAM);
                    GameAudio.play(GameAudio.Cue.TIDE_DISSIPATE);
                }
                case DENIED -> GameAudio.play(GameAudio.Cue.DENIED);
                case CHECKPOINT -> GameAudio.play(GameAudio.Cue.CHECKPOINT);
                case UPGRADE -> GameAudio.play(GameAudio.Cue.UPGRADE);
                case ENEMY_HIT -> {
                    feedback.enemyHit(false);
                    if(!event.water()) {
                        addImpact(event.x(),event.y(),game.player().bladeForm()?Fx.BLADE:Fx.WATER);
                        GameAudio.play(GameAudio.Cue.ENEMY_HIT);
                    }
                }
                case ENEMY_DEFEATED -> {
                    feedback.enemyHit(true);
                    mobFx.defeated(event.x(),event.y(),reducedEffects);
                    impacts.removeIf(impact->impact.x==event.x()&&impact.y==event.y()&&impact.time==0);
                    if(!event.water())addImpact(event.x(),event.y(),game.player().bladeForm()?Fx.BLADE:Fx.WATER);
                    GameAudio.play(GameAudio.Cue.KILL);
                    GameAudio.duck(.3,.25);
                }
                case PLAYER_HIT -> {
                    if(game.player().bladeForm())bladeAnimation.play(BladeAnimation.Action.HURT,.32);
                    // the killing blow shows the collapsed pose (frame 5), as step() does for the dead body
                    if(game.player().bladeForm()&&!game.player().alive())bladeAnimation.pose(BladeAnimation.Action.HURT,.20,.32);
                    feedback.playerHit();
                    addImpact(game.player().x(),game.player().y(),game.player().bladeForm()?Fx.BLADE_HURT:Fx.SLIME_HURT);
                    GameAudio.play(GameAudio.Cue.HURT);
                    GameAudio.duck(.5,.35);
                }
                case PICKUP -> {
                    addImpact(event.x(),event.y(),Fx.PICKUP);
                    GameAudio.play(GameAudio.Cue.PICKUP);
                }
                case ABSORB_START -> {
                    int dx=(int)Math.round(event.x()-game.player().x()),dy=(int)Math.round(event.y()-game.player().y());
                    slimeAnimation.face(dx,dy);
                    GameAudio.stopLoop(GameAudio.Loop.EATING);
                    GameAudio.startLoop(GameAudio.Loop.EATING,1);
                    eating=true;
                }
                case ABSORBED -> {
                    addImpact(event.x(),event.y(),Fx.PICKUP);
                    GameAudio.play(GameAudio.Cue.ABSORBED);
                    eating=false;                                // a finished eat lets the munching play out
                }
                case HEALED -> {
                    addImpact(event.x(),event.y(),Fx.HEAL);
                    GameAudio.play(GameAudio.Cue.HEALED);
                }
                case CRESCENT_CAST -> {
                    bladeAnimation.face(game.bladeAimX(),game.bladeAimY());
                    bladeAnimation.pose(BladeAnimation.Action.CAST,game.bladeCastElapsed(),RuinedOutpostGame.BLADE_CAST_DURATION);
                    GameAudio.play(GameAudio.Cue.CAST);
                }
                case CRESCENT_IMPACT -> {
                    addImpact(event.x(),event.y(),Fx.BLADE);
                    GameAudio.play(GameAudio.Cue.CRESCENT_IMPACT);
                }
                case RIPOSTE_START -> {
                    bladeAnimation.play(BladeAnimation.Action.GUARD,RuinedOutpostGame.RIPOSTE_DURATION);
                    GameAudio.play(GameAudio.Cue.GUARD);
                }
                case RIPOSTE_COUNTER -> {
                    addImpact(event.x(),event.y(),Fx.COUNTER);feedback.enemyHit(true);
                    GameAudio.play(GameAudio.Cue.COUNTER);
                }
                case TRANSFORM -> {
                    bladeAnimation = new BladeAnimation();
                    bladeAnimation.face(slimeAnimation.facingHorizontal(), slimeAnimation.facingVertical());
                    transformationAnimation.start(false);
                    feedback.transformed();
                    GameAudio.play(GameAudio.Cue.TRANSFORM);
                }
                case REVERT -> {
                    attackHeld=false;
                    slimeAnimation = new SlimeAnimation();
                    slimeAnimation.face(bladeAnimation.facingHorizontal(), bladeAnimation.facingVertical());
                    transformationAnimation.start(true);
                    feedback.transformed();
                    banner("BLADE FADES / RECOVERING", 1.5);
                    GameAudio.play(GameAudio.Cue.REVERT);
                }
                case GUARDIAN_AWAKENED -> {
                    banner("THE OUTPOST WARDEN WAKES", 2.6);
                    if(wardenArea())wardenFx.awaken(game.guardian());
                    GameAudio.play(GameAudio.Cue.GUARDIAN_AWAKENED);
                }
                case GUARDIAN_SLAM -> {
                    feedback.guardianSlam();
                    Guardian guardian=game.guardian();
                    boolean charge=guardian.attack()==Guardian.Attack.CHARGE;
                    addImpact(charge?guardian.x():event.x(),charge?guardian.y():event.y(),Fx.WARDEN);
                    if(wardenArea())wardenFx.slam(guardian,event.x(),event.y());
                    GameAudio.play(GameAudio.Cue.GUARDIAN_SLAM);
                }
                case SPIT_SHOT -> GameAudio.play(GameAudio.Cue.SPIT_SHOT);
                case SPIT_IMPACT -> {
                    addImpact(event.x(),event.y(),Fx.SPIT);
                    GameAudio.play(GameAudio.Cue.SPIT_IMPACT);
                }
                case ROOM_ENTERED -> {
                    trails.clear(); impacts.clear(); attackHeld=false;bladeAnimation=new BladeAnimation();
                    wardenFx=bossFx();mobFx.clear();
                }
                case VICTORY -> {
                    feedback.victory();
                    GameAudio.stopLoops();
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

        syncTitle();
        if(titleArt()&&game.story().phase()==OutpostStory.Phase.PROLOGUE) {
            titleScreen.draw(canvas,WIDTH,HEIGHT,titleTime,slimeIdleSheet,reducedEffects);
            CampaignRenderer.drawTitleExtras(canvas,game,WIDTH,HEIGHT,menuShown());
            canvas.dispose();
            return;
        }
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
        if(game.campaignMode()) {
            drawPlayerHud(canvas);
            CampaignRenderer.drawHud(canvas,game,WIDTH,HEIGHT);
            CampaignRenderer.drawAreaCard(canvas,cardBiome,cardTime,WIDTH,reducedEffects);
            wardenFx.drawTitle(canvas,WIDTH,HEIGHT);
            if(mapShown&&!game.story().blocksGameplay())CampaignRenderer.drawMap(canvas,game,WIDTH,HEIGHT);
            CampaignRenderer.drawOverlay(canvas,game,WIDTH,HEIGHT,menuShown());
            CampaignRenderer.drawDebugStatus(canvas,game);
            loading.draw(canvas,loadingClock,slimeIdleSheet,transformInSheet,transformOutSheet,reducedEffects);
        } else {
            drawHud(canvas);drawBanner(canvas);drawStoryOverlay(canvas);
            if(game.paused())drawPause(canvas);
            if(mapShown&&!game.story().blocksGameplay())drawMap(canvas,true);
        }
        canvas.dispose();
    }

    /** box: the screen area a wall or set piece paints, so Rainoray can show through it. */
    private record DepthDraw(double groundY, Runnable paint, java.awt.Rectangle box) {
        DepthDraw(double groundY, Runnable paint) { this(groundY, paint, null); }
    }

    private void drawWorld(Graphics2D canvas, int cameraX, int cameraY) {
        boolean outpostArt=CampaignRenderer.bakedGround(game);
        if(game.campaignMode()) {
            CampaignRenderer.drawGround(canvas,game,cameraX,cameraY,WIDTH,HEIGHT);
            CampaignRenderer.drawScenery(canvas,game,cameraX,cameraY,WIDTH,HEIGHT);
        } else {
            drawTerrain(canvas, cameraX, cameraY);
            canvas.setColor(new Color(10,16,27,105)); canvas.fillRect(0,0,WIDTH,HEIGHT);
            drawBanks(canvas, cameraX, cameraY);drawFieldRemains(canvas,cameraX,cameraY);
        }
        if(!game.campaignMode()||game.biome()==0)drawWeather(canvas,cameraX,cameraY,true);
        drawCorpses(canvas,cameraX,cameraY);
        drawIchor(canvas, cameraX, cameraY);
        drawGuardianTelegraph(canvas, cameraX, cameraY);
        drawImpacts(canvas,cameraX,cameraY,true);
        if(wardenArea())wardenFx.drawGround(canvas,game.guardian(),cameraX,cameraY);
        mobFx.drawGround(canvas,cameraX,cameraY,reducedEffects);

        // Painter's order uses ground contact, never sprite centre or asset loading order.
        List<DepthDraw> draws=new ArrayList<>();
        if(!game.campaignMode())for(var barrier:game.map().barriers())
            draws.add(new DepthDraw(barrier.centerY()+barrier.height()/2,
                    ()->drawBarrier(canvas,barrier,cameraX,cameraY)));
        // Cull on each sprite's whole painted box; the 16 px margin covers screen shake.
        else if(outpostArt)for(var barrier:game.map().barriers()) {
            var box=CampaignRenderer.propBounds(barrier,cameraX,cameraY);
            if(box!=null&&box.intersects(-16,-16,WIDTH+32,HEIGHT+32))draws.add(new DepthDraw(barrier.centerY()+barrier.height()/2,
                    ()->CampaignRenderer.drawProp(canvas,barrier,cameraX,cameraY),box));
        }
        if(outpostArt)for(var piece:SetPieces.pieces(game.biome())) {
            var box=CampaignRenderer.bounds(piece,cameraX,cameraY);
            if(box!=null&&box.intersects(-16,-16,WIDTH+32,HEIGHT+32))draws.add(new DepthDraw(piece.y(),
                    ()->CampaignRenderer.drawSetPiece(canvas,piece,game,cameraX,cameraY,frameCounter),box));
        }
        for(var dressing:game.map().dressing()) {
            Runnable paint=()->{
            var decoration=dressing.decoration();var sprite=dressingSprites.get(decoration);
            if(sprite!=null) {
                int left=(int)Math.round(dressing.x())-cameraX-decoration.anchorX*2;
                int top=(int)Math.round(dressing.y())-cameraY-decoration.anchorY*2;
                if(decoration==RuinedOutpostMap.Decoration.BRAZIER) {
                    int frame=(int)(frameCounter*8/60)%(sprite.getWidth()/64);
                    canvas.drawImage(sprite,left,top,left+128,top+128,frame*64,0,frame*64+64,64,null);
                } else canvas.drawImage(sprite,left,top,sprite.getWidth()*2,sprite.getHeight()*2,null);
            }
            };
            if(dressing.decoration().ground())paint.run();else draws.add(new DepthDraw(dressing.y(),paint));
        }
        if(!game.campaignMode())queueDoors(draws,canvas,cameraX,cameraY);
        Player player=game.player();
        for(Wisp scout:game.scouts()) if(scout.visible())
            draws.add(new DepthDraw(scout.y()+WispAnimation.RENDER_SIZE/2,
                    ()->{
                        var enemy=game.campaignEnemy(scout);
                        if(enemy!=null && enemy.kind()!=CampaignEnemy.Kind.OUTPOST_SCOUT
                                && enemy.kind()!=CampaignEnemy.Kind.OUTPOST_SPITTER && !kindSheets.containsKey(enemy.kind()))
                            CampaignRenderer.drawEnemy(canvas,enemy,cameraX,cameraY);
                        else drawWisp(canvas,scout,cameraX,cameraY);
                    }));
        if((game.campaignMode()||game.map().room()==9)&&game.guardian().visible())
            draws.add(new DepthDraw(game.guardian().y()+Guardian.GROUND_Y_OFFSET,
                    ()->{
                        if(game.campaignMode()&&game.biome()!=0&&!(game.biome()==1&&briarheartSheet!=null))CampaignRenderer.drawBoss(canvas,game.guardian(),game.biome(),cameraX,cameraY);
                        else drawGuardian(canvas,cameraX,cameraY);
                    }));
        for(double[] trail:trails) {
            draws.add(new DepthDraw(trail[1]+SlimeAnimation.RENDER_SIZE/2,()->{
                Graphics2D ghost=(Graphics2D)canvas.create();
                ghost.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        (float)(0.25*(1-trail[2]/0.14))));
                int gx=cameraX+(int)(player.x()-trail[0]),gy=cameraY+(int)(player.y()-trail[1]);
                if(player.bladeForm()) {
                    drawBlade(ghost,gx,gy);
                    ghost.setColor(new Color(89,223,245));
                    int tx=(int)Math.round(trail[0]-cameraX),ty=(int)Math.round(trail[1]-cameraY)+40;
                    ghost.fillOval(tx-18,ty-4,36,8);
                } else drawSlime(ghost,gx,gy);
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
        for(var crescent:game.crescents())
            draws.add(new DepthDraw(crescent.y()+Player.COLLISION_Y_OFFSET,
                    ()->drawCrescent(canvas,cameraX,cameraY,crescent)));
        if(wardenArea())wardenFx.queue((y,paint)->draws.add(new DepthDraw(y,paint)),canvas,game.guardian(),
                cameraX,cameraY,frameCounter,reducedEffects);
        draws.sort(java.util.Comparator.comparingDouble(DepthDraw::groundY));
        for(DepthDraw draw:draws)draw.paint().run();
        // Walls and towers stand taller than Rainoray: where one covers it, a faint copy shows through.
        // Clipped to those sprites, so actors in front stay clean; it blinks with the hurt flash.
        if(outpostArt&&player.alive()&&!transformationAnimation.active()&&(!player.damageFlashing()||frameCounter/3%2==0)) {
            double feet=player.y()+SlimeAnimation.RENDER_SIZE/2;
            var body=new java.awt.Rectangle((int)Math.round(player.x())-cameraX-80,(int)Math.round(feet)-cameraY-150,160,166);
            var cover=new java.awt.geom.Area();
            for(DepthDraw draw:draws)if(draw.box()!=null&&draw.groundY()>feet&&draw.box().intersects(body))
                cover.add(new java.awt.geom.Area(draw.box()));
            if(!cover.isEmpty()) {
                var ghost=(Graphics2D)canvas.create();
                ghost.clip(cover);
                ghost.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.3f));
                if(player.bladeForm())drawBlade(ghost,cameraX,cameraY);else drawSlime(ghost,cameraX,cameraY);
                ghost.dispose();
            }
        }

        drawImpacts(canvas,cameraX,cameraY,false);
        if(wardenArea())wardenFx.drawAbove(canvas,game.guardian(),cameraX,cameraY,frameCounter);
        mobFx.drawAbove(canvas,cameraX,cameraY,reducedEffects);
        if(outpostArt)OutpostLights.draw(canvas,game,cameraX,cameraY,frameCounter,reducedEffects);
        if(!game.campaignMode()||game.biome()==0)drawWeather(canvas,cameraX,cameraY,false);
        if(mouseAimed&&!game.blocked()&&aimReticle!=null) {
            int frame=(int)(frameCounter/6)%(aimReticle.getWidth()/32);
            int x=Math.round(mouseX/2f)*2,y=Math.round(mouseY/2f)*2;
            canvas.drawImage(aimReticle,x-32,y-32,x+32,y+32,frame*32,0,frame*32+32,32,null);
        }
    }

    private void drawPlayer(Graphics2D canvas, int cameraX, int cameraY) {
        Player player = game.player();
        if(!player.bladeForm()&&player.trait()!=Player.Trait.NONE) {
            BufferedImage shell=player.trait()==Player.Trait.MEMBRANE?membraneSheet:jetSheet;
            if(shell!=null) {
                int frame=(int)(frameCounter/6)%(shell.getWidth()/64);
                int x=(int)Math.round((player.x()-cameraX)/2)*2-64,y=(int)Math.round((player.y()-cameraY)/2)*2-64;
                var aura=(Graphics2D)canvas.create();
                float alpha=(float)Math.min(reducedEffects?.55:.85,player.traitSeconds()/1.5);
                aura.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,alpha));
                aura.drawImage(shell,x,y,x+128,y+128,frame*64,0,frame*64+64,64,null);aura.dispose();
            }
        }
        BufferedImage morph=transformationAnimation.reverting()?transformOutSheet:transformInSheet;
        if(transformationAnimation.active()&&morph!=null) {
            drawTransformation(canvas,cameraX,cameraY);return;
        }
        if (!player.damageFlashing() || frameCounter / 3 % 2 == 0) {
            if (player.bladeForm()) {
                drawBlade(canvas, cameraX, cameraY);
                drawBladeTrail(canvas, cameraX, cameraY);
            } else {
                drawSlime(canvas, cameraX, cameraY);
            }
        }
        drawTransformation(canvas, cameraX, cameraY);
        if(player.bladeForm())drawChargeGlow(canvas,cameraX,cameraY);
        if(game.guarding()&&riposteSheet!=null) {
            int frame=Math.min(7,(int)(game.guardProgress()*8));
            int x=(int)Math.round(player.x())-cameraX-80,y=(int)Math.round(player.y())-cameraY+48-144;
            canvas.drawImage(riposteSheet,x,y,x+160,y+160,frame*80,0,frame*80+80,80,null);
        }
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
        if(game.absorptionTarget()!=null&&engulfSheet!=null) {
            int row=slimeAnimation.row()==3?1:slimeAnimation.row()==4?2:0;
            int frame=Math.min(15,(int)(game.absorptionProgress()*16));
            boolean flip=slimeAnimation.flipHorizontal();
            // Peak spread covers the actual remains; collision stays at the player.
            double spread=Math.sin(Math.PI*game.absorptionProgress());
            var corpse=game.absorptionTarget();
            int left=(int)Math.round((player.x()+(corpse.x()-player.x())*spread)/2)*2-cameraX-80;
            int top=(int)Math.round((player.y()+(corpse.y()-player.y())*spread)/2)*2-cameraY+48-144;
            canvas.drawImage(engulfSheet,flip?left+160:left,top,flip?left:left+160,top+160,
                    frame*80,row*80,frame*80+80,row*80+80,null);
            canvas.setColor(INK);canvas.fillRect(x+16,y+100,64,6);
            canvas.setColor(GOLD);canvas.fillRect(x+16,y+100,(int)(64*game.absorptionProgress()),4);
            return;
        }
        boolean reviewedIdle=slimeAnimation.row()==0&&slimeIdleSheet!=null;
        boolean reviewedCast=slimeAnimation.attacking()&&slimeCastSheet!=null;
        boolean finisher=reviewedCast&&game.waterKind()==WaterProjectile.Kind.FINISHER&&slimeFinisherSheet!=null;
        BufferedImage sheet=reviewedCast?(finisher?slimeFinisherSheet:slimeCastSheet):reviewedIdle?slimeIdleSheet:slimeSheet;
        if (sheet != null) {
            int cell=finisher?64:SlimeAnimation.CELL_SIZE;
            // Extra finisher canvas is transparent motion margin, never a larger character.
            if(finisher) {x-=16;y-=16;}
            int sourceX = (reviewedCast?slimeAnimation.castFrame():reviewedIdle?slimeAnimation.idleFrame():slimeAnimation.frame()) * cell;
            int castRow=slimeAnimation.row()==3?1:slimeAnimation.row()==4?2:0;
            int sourceY = (reviewedCast?castRow:reviewedIdle?0:slimeAnimation.row()) * cell;
            int imageLeft = slimeAnimation.flipHorizontal()
                    ? x + cell*2 : x;
            int imageRight = slimeAnimation.flipHorizontal()
                    ? x : x + cell*2;
            canvas.drawImage(sheet, imageLeft, y,
                    imageRight, y + cell*2,
                    sourceX, sourceY,
                    sourceX + cell,
                    sourceY + cell, null);
            return;
        } else {
            canvas.setColor(TEAL);
            canvas.fillOval(x, y + 16, SlimeAnimation.RENDER_SIZE,
                    SlimeAnimation.RENDER_SIZE - 16);
        }
    }


    private void drawBlade(Graphics2D canvas, int cameraX, int cameraY) {
        Player player = game.player();
        int cellWidth = BladeAnimation.CELL_WIDTH,cellHeight = BladeAnimation.CELL_HEIGHT;
        int renderWidth=cellWidth*2,renderHeight=cellHeight*2;
        int x = (int)Math.round(player.x())-cameraX-renderWidth/2;
        int floorY = (int) Math.round(player.y()) - cameraY
                + SlimeAnimation.RENDER_SIZE / 2;
        int y = floorY-BladeAnimation.FOOT_ROW*2;
        BufferedImage sheet=rainoraySheets.get(bladeAnimation.action());
        boolean slash=bladeAnimation.action()==BladeAnimation.Action.SLASH;
        // A sheet may carry cells wider than the 80 px body canvas: the body stays in the middle 80 px and
        // the margin is room to lunge, leap and swing. Four facing rows per block of a modern sheet.
        if(sheet!=null)cellWidth=cellHeight=bladeCell(sheet,bladeAnimation.action());
        boolean modern=sheet!=null&&sheet.getHeight()%(cellHeight*4)==0;
        int frame=bladeAnimation.frame();
        int sourceRow=modern?bladeAnimation.directionRow():bladeAnimation.row();
        if(slash||bladeAnimation.action()==BladeAnimation.Action.CLEAVE) {
            frame=modern?bladeAttackFrame(game.bladeElapsed(),game.bladeMove()):sideAttackFrame(bladeAnimation.elapsed(),Math.min(2,game.combo()));
            sourceRow+=modern?Math.min(bladeBlock(game.bladeMove()),sheet.getHeight()/(cellHeight*4)-1)*4:slash?Math.min(2,game.combo())*3:0;
        }
        if(sheet==null) {
            sheet=rainoraySheets.get(BladeAnimation.Action.IDLE);frame=0;modern=sheet!=null&&sheet.getHeight()==320;sourceRow=modern?bladeAnimation.directionRow():bladeAnimation.row();
            if(sheet==null) {
                // Error-only silhouette: a damaged installation must not hide the player.
                canvas.setColor(INK);canvas.fillRect(x+52,y+32,56,112);
                canvas.setColor(TEAL);canvas.fillRect(x+60,y+62,40,66);
                canvas.setColor(new Color(225,236,215));canvas.fillRect(x+70,y+36,20,24);
                canvas.fillRect(x+106,y+76,6,48);
                canvas.setColor(new Color(91,71,52));canvas.fillRect(x+60,y+128,16,16);canvas.fillRect(x+84,y+128,16,16);
                return;
            }
        }
        frame=Math.min(frame,sheet.getWidth()/cellWidth-1);
        int sourceX=frame*cellWidth;
        int sourceY = sourceRow * cellHeight;
        int margin=cellWidth-BladeAnimation.CELL_WIDTH;            // screen px each side at the shared 2x scale
        x-=margin;y-=margin;renderWidth=cellWidth*2;renderHeight=cellHeight*2;
        int imageLeft = !modern&&bladeAnimation.flipHorizontal()
                ? x + renderWidth : x;
        int imageRight = !modern&&bladeAnimation.flipHorizontal()
                ? x : x + renderWidth;
        canvas.drawImage(sheet, imageLeft, y,
                imageRight, y + renderHeight,
                sourceX, sourceY,
                sourceX + cellWidth,
                sourceY + cellHeight, null);
    }

    /**
     * The blade's trail is photographed from the same puppet pose as the body cell, so it shares that cell's
     * frame and row and is drawn centred on it. It already has the body cut out of it where he is in front.
     */
    private void drawBladeTrail(Graphics2D canvas,int cameraX,int cameraY) {
        BufferedImage sheet=bladeTrailSheets.get(bladeAnimation.action());
        if(sheet==null)return;
        int frame=bladeAnimation.frame(),row=bladeAnimation.directionRow();
        if(bladeAnimation.action()==BladeAnimation.Action.SLASH||bladeAnimation.action()==BladeAnimation.Action.CLEAVE) {
            frame=bladeAttackFrame(game.bladeElapsed(),game.bladeMove());row+=bladeBlock(game.bladeMove())*4;
        }
        if(frame>=sheet.getWidth()/TRAIL_CELL||row>=sheet.getHeight()/TRAIL_CELL)return;
        Player player=game.player();
        int grow=TRAIL_CELL-BladeAnimation.CELL_WIDTH;
        int x=(int)Math.round(player.x())-cameraX-BladeAnimation.RENDER_WIDTH/2-grow;
        int y=(int)Math.round(player.y())-cameraY+SlimeAnimation.RENDER_SIZE/2-BladeAnimation.FOOT_ROW*2-grow;
        var layer=(Graphics2D)canvas.create();
        if(reducedEffects)layer.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.6f));
        layer.drawImage(sheet,x,y,x+TRAIL_CELL*2,y+TRAIL_CELL*2,
                frame*TRAIL_CELL,row*TRAIL_CELL,(frame+1)*TRAIL_CELL,(row+1)*TRAIL_CELL,null);
        layer.dispose();
    }

    /** Which sheet a blade move is drawn from... */
    static BladeAnimation.Action bladeAction(int move) {
        return RuinedOutpostGame.strikeKind(move)==4?BladeAnimation.Action.CLEAVE:BladeAnimation.Action.SLASH;
    }
    /** ...and which block of four facing rows inside it: combo moves 0-3 then the dash slash; cleave then the spin. */
    static int bladeBlock(int move) {
        return move==RuinedOutpostGame.DASH_SLASH?4:move==RuinedOutpostGame.SPIN_CLEAVE?1:move<4?move:0;
    }
    /** A blade sheet's cell size: its width over the action's frames per row (80, or wider with margin). */
    static int bladeCell(BufferedImage sheet,BladeAnimation.Action action) {
        return Math.max(BladeAnimation.CELL_WIDTH,sheet.getWidth()/BladeAnimation.columns(action));
    }

    static int bladeAttackFrame(double elapsed,int move) {
        double contact=RuinedOutpostGame.strikeWindup(move),duration=RuinedOutpostGame.strikeDuration(move);
        return elapsed<contact?Math.max(0,Math.min(3,(int)(elapsed/contact*4)))
                :4+Math.min(7,(int)((elapsed-contact)/(duration-contact)*8));
    }

    /**
     * The glow of a held charge. The swing itself is no longer drawn here: the blade's trail is a layer of its
     * own, photographed from the same puppet pose as the body (drawBladeTrail), so it always sits on the sword.
     */
    private void drawChargeGlow(Graphics2D canvas,int cameraX,int cameraY) {
        if(!game.bladePreparing())return;
        int px=(int)Math.round((game.player().x()-cameraX)/2)*2;
        int py=(int)Math.round((game.player().y()-cameraY)/2)*2;
        double charge=game.chargeProgress();
        var glow=(Graphics2D)canvas.create();
        glow.setColor(new Color(80,190,255,80+(int)(charge*120)));
        int width=(int)(36*charge)*2;
        glow.fillRect(px-36,py-80,width,4);
        glow.setColor(charge>=1?new Color(215,255,255):new Color(77,211,250));
        for(int i=0;i<4;i++)if(charge>(i+1)*.2) {
            int offset=(int)(Math.sin(weatherTime*5+i)*8)*2;
            glow.fillRect(px-24+i*16,py-30-offset,2,4);
        }
        glow.dispose();
    }

    static int sideAttackFrame(double elapsed,int combo) {
        double windup=RuinedOutpostGame.strikeWindup(combo);
        return elapsed<windup?Math.max(0,Math.min(2,(int)(elapsed/windup*3)))
                :3+Math.min(4,(int)((elapsed-windup)/(0.3-windup)*5));
    }

    private void drawTransformation(Graphics2D canvas, int cameraX, int cameraY) {
        BufferedImage sheet=transformationAnimation.reverting()?transformOutSheet:transformInSheet;
        if (!transformationAnimation.active() || sheet==null)return;
        Player player = game.player();
        int x = (int) Math.round(player.x()) - cameraX
                - TransformationAnimation.RENDER_SIZE / 2;
        int y = (int)Math.round(player.y())-cameraY+48-BladeAnimation.FOOT_ROW*2;
        int sourceX = transformationAnimation.frame()
                * TransformationAnimation.CELL_SIZE;
        int sourceY=Math.min(sheet.getHeight()==320?bladeAnimation.directionRow():bladeAnimation.row(),sheet.getHeight()/80-1)*80;
        boolean flip=sheet.getHeight()!=320&&bladeAnimation.flipHorizontal();
        canvas.drawImage(sheet, flip?x+160:x, y,
                flip?x:x+160,
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
        boolean whiteHit=whiteHit(scout);
        int size = (int) Math.round(WispAnimation.RENDER_SIZE
                * scout.renderScale());
        if (size <= 0) {
            return;
        }
        int x = (int) Math.round(scout.x()) - cameraX - size / 2;
        int floorY = (int) Math.round(scout.y()) - cameraY
                + WispAnimation.RENDER_SIZE / 2;
        int y = floorY - size;
        int spriteY = y;
        BufferedImage sheet = scout.usesAttackAnimation() && wispAttackSheet != null
                ? wispAttackSheet : wispWalkSheet;
        var enemy=game.campaignEnemy(scout);
        var own=enemy==null?null:kindSheets.get(enemy.kind());
        if(own!=null) {
            int direction=scout.animation().row(),row=direction==2?2:direction==1||direction==3?1:0;
            int frame=scout.usesAttackAnimation()?scout.attackFrame():scout.animation().frame()%4;
            sheet = scout.usesAttackAnimation()?own[1]:own[0];
            canvas.drawImage(hitTint(sheet,whiteHit),direction==1?x+size:x,y,direction==1?x:x+size,y+size,
                    frame*48,row*48,frame*48+48,row*48+48,null);
        } else if(scout.role()==Wisp.Role.SPITTER&&spitterWalkSheet!=null&&spitterAttackSheet!=null) {
            int direction=scout.animation().row(),row=direction==2?2:direction==1||direction==3?1:0;
            int frame=scout.usesAttackAnimation()?scout.attackFrame():scout.animation().frame()%4;
            int groundCorrection=row==2?(int)Math.round(4*size/48.0):0;
            sheet = scout.usesAttackAnimation()?spitterAttackSheet:spitterWalkSheet;
            spriteY += groundCorrection;
            canvas.drawImage(hitTint(sheet,whiteHit),direction==1?x+size:x,spriteY,direction==1?x:x+size,spriteY+size,
                    frame*48,row*48,frame*48+48,row*48+48,null);
        } else if(game.map().room()!=9&&remnantWalkSheet!=null&&remnantAttackSheet!=null) {
            int direction=scout.animation().row();
            int row=direction==2?2:direction==1||direction==3?1:0;
            int frame=scout.usesAttackAnimation()?scout.attackFrame():scout.animation().frame()%4;
            sheet = scout.usesAttackAnimation()?remnantAttackSheet:remnantWalkSheet;
            canvas.drawImage(hitTint(sheet,whiteHit),direction==1?x+size:x,y,direction==1?x:x+size,y+size,
                    frame*48,row*48,frame*48+48,row*48+48,null);
        } else if (game.map().room()!=9 && remnantSheet!=null) {
            int direction=scout.animation().row();
            boolean side=direction==1||direction==3;
            boolean attacking=scout.usesAttackAnimation();
            int row=attacking?(side?10:direction==2?11:5):(side?7:direction==2?8:6);
            int frame=attacking?Math.min(3,scout.attackFrame()/2):scout.animation().frame()%4;
            sheet = remnantSheet;
            canvas.drawImage(hitTint(sheet,whiteHit),direction==1?x+size:x,y,direction==1?x:x+size,y+size,
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
            canvas.drawImage(hitTint(sheet,whiteHit), x, y, x + size, y + size,
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
        if (!guardian.visible() || (guardian.alive() && !guardian.flashVisible())) {
            return;
        }
        // Briarheart's atlas uses 96 px cells drawn at 2x; the Warden's, 64 px at 3x. Both fill the same box.
        BufferedImage motionSheet=game.campaignMode()&&game.biome()==1&&briarheartSheet!=null?briarheartSheet:wardenMotionSheet;
        int cell=motionSheet==briarheartSheet?96:Guardian.CELL_SIZE,foot=cell*Guardian.SPRITE_FOOT_ROW/Guardian.CELL_SIZE;
        boolean motion=motionSheet!=null&&motionSheet.getWidth()>=8*cell&&motionSheet.getHeight()>=7*cell;
        int size = motion||guardian.state()==Guardian.State.DEAD&&guardianDeathSheet!=null?Guardian.RENDER_SIZE
                :(int) Math.round(Guardian.RENDER_SIZE * guardian.renderScale());
        if (size <= 0) {
            return;
        }
        int x = (int) Math.round(guardian.x()) - cameraX - size / 2;
        int floorY = (int) Math.round(guardian.y()) - cameraY
                + Guardian.GROUND_Y_OFFSET+(cell-foot)*Guardian.RENDER_SIZE/cell;
        int y = floorY - size;
        BufferedImage actionSheet=switch(guardian.attack()) {
            case CHARGE -> guardianChargeSheet; case SWEEP -> guardianSweepSheet;
            case SHOCKWAVE -> guardianWaveSheet; default -> null;
        };
        if(motion) {
            int sourceX=guardian.animationFrame()*cell;
            int sourceY=guardian.animation().ordinal()*cell;
            canvas.drawImage(motionSheet,x,y,x+size,y+size,sourceX,sourceY,
                    sourceX+cell,sourceY+cell,null);
        } else if(guardian.state()==Guardian.State.DEAD&&guardianDeathSheet!=null) {
            int frame=Math.min(11,(int)(guardian.stateSeconds()/Guardian.DEATH_DURATION*12));
            canvas.drawImage(guardianDeathSheet,x,y,x+size,y+size,frame*64,0,frame*64+64,64,null);
        } else if(guardian.state()==Guardian.State.APPROACH&&guardianWalkSheet!=null) {
            int frame=(int)(guardian.stateSeconds()*8)%8;
            canvas.drawImage(guardianWalkSheet,x,y,x+size,y+size,frame*64,0,frame*64+64,64,null);
        } else if(actionSheet!=null&&guardian.state()!=Guardian.State.DORMANT) {
            int frame=switch(guardian.state()) {
                case TELEGRAPH -> Math.min(3,(int)(guardian.stateSeconds()/guardian.telegraphDuration()*4));
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

    /** The Warden's own effects belong to its arena; other bosses do not borrow them. */
    private boolean wardenArea(){return game.campaignMode()&&(game.biome()==0||game.biome()==1&&briarheartSheet!=null);}

    /** Native px from an enemy's centre where its aim line starts (clear of its body), and the hexer's spoke length. */
    static final double AIM_START=9, SPOKE=16;

    /** Every live attack warning, painted at native resolution by {@link Telegraphs}. */
    private void drawGuardianTelegraph(Graphics2D canvas,int cameraX,int cameraY) {
        tells.begin();
        for(var scout:game.scouts()) {
            boolean telling=scout.state()==Wisp.State.TELEGRAPH;
            boolean striking=scout.state()==Wisp.State.LUNGE&&scout.stationaryMelee();
            if(!telling&&!striking)continue;
            double x=(scout.x()-cameraX)/2.,y=(scout.y()-cameraY)/2.;
            double angle=Math.atan2(scout.intentY(),scout.intentX());
            double charge=telling?scout.stateSeconds()/scout.telegraphDuration():1;
            boolean locked=scout.directionLocked();
            if(scout.stationaryMelee()) {
                double reach=scout.meleeReach()/2;
                double hit=striking?Math.max(.05,1-scout.stateSeconds()/scout.lungeDuration()):0;
                tells.box(x+Math.cos(angle)*reach/2,y+Math.sin(angle)*reach/2,angle,reach,scout.meleeHalfWidth()/2,
                        charge,hit,locked,false,reducedEffects);
            } else if(scout.role()==Wisp.Role.SPITTER) {
                // each shot's flight as a thin aim line; the hexer's ring of eight as short spokes
                var profile=game.campaignEnemy(scout);
                boolean radial=profile!=null&&profile.kind()==CampaignEnemy.Kind.CINDER_HEXER;
                double reach=radial?AIM_START+SPOKE:EnemyProjectile.MAX_TRAVEL/2;
                for(int side=0;side<(radial?8:3);side++) {
                    double direction=angle+(radial?side*Math.PI/4:(side-1)*.20);
                    tells.aim(x,y,x+Math.cos(direction)*reach,y+Math.sin(direction)*reach,AIM_START,charge,locked);
                }
            } else {
                // a lunge: the line of the rush and an arrowhead as wide as the body it sweeps
                double reach=scout.lungeReach()/2;
                tells.lunge(x,y,x+Math.cos(angle)*reach,y+Math.sin(angle)*reach,RuinedOutpostGame.WISP_CONTACT_RADIUS/2,
                        AIM_START,charge,locked);
            }
        }
        Guardian guardian=game.guardian();
        if(game.campaignMode()&&guardian.auraRadius()>0)
            tells.aura((guardian.x()-cameraX)/2.,(guardian.y()-cameraY)/2.,guardian.auraRadius()/2);
        boolean warning=guardian.state()==Guardian.State.TELEGRAPH;
        if(game.bossActive()&&(warning||guardian.state()==Guardian.State.SLAM)) {
            double charge=warning?guardian.stateSeconds()/guardian.telegraphDuration():1;
            double hit=warning?0:Math.max(.05,1-guardian.stateSeconds()/guardian.activeDuration());
            double x=(guardian.targetX()-cameraX)/2.,y=(guardian.targetY()-cameraY)/2.;
            switch(guardian.attack()) {
                case FISSURE -> {
                    var lane=guardian.fissureBounds();
                    if(lane!=null) {
                        boolean across=lane.width()>lane.height();
                        tells.box((lane.centerX()-cameraX)/2.,(lane.centerY()-cameraY)/2.,across?0:Math.PI/2,
                                Math.max(lane.width(),lane.height())/2,Math.min(lane.width(),lane.height())/4,
                                charge,hit,true,true,reducedEffects);
                    }
                }
                case CHARGE -> {
                    double bx=(guardian.x()-cameraX)/2.,by=(guardian.y()-cameraY)/2.,reach=Guardian.CHARGE_SPEED*Guardian.CHARGE_DURATION/2;
                    double radius=Guardian.CHARGE_RADIUS/2;
                    if(warning)tells.capsule(bx,by,bx+guardian.directionX()*reach,by+guardian.directionY()*reach,radius,charge,true);
                    else tells.disc(bx,by,radius,1,hit,reducedEffects);
                }
                case SHOCKWAVE -> {
                    double band=Guardian.WAVE_HALF_WIDTH,reach=(guardian.slamRadius()+band)/2;
                    if(warning)tells.wave(x,y,(Guardian.WAVE_START+band)/2,reach,charge,reducedEffects);
                    else tells.band(x,y,(guardian.waveRadius()-band)/2,(guardian.waveRadius()+band)/2,reach,hit,reducedEffects);
                }
                default -> tells.disc(x,y,guardian.slamRadius()/2,charge,hit,reducedEffects);
            }
        }
        tells.drawTo(canvas,2);
    }

    /** A struck enemy shows pure white for its first two frames, then blinks: one hit, one clear frame of contact. */
    static boolean whiteHit(Wisp body){return body.state()==Wisp.State.HURT&&body.stateSeconds()<1/30.;}
    private static final java.util.Map<BufferedImage,BufferedImage> WHITE=new java.util.WeakHashMap<>();
    /** The sheet itself, or its white silhouette (made once per sheet) while a hit flashes. */
    static BufferedImage hitTint(BufferedImage sheet,boolean white) {
        if(!white||sheet==null)return sheet;
        return WHITE.computeIfAbsent(sheet,s->{
            var out=new BufferedImage(s.getWidth(),s.getHeight(),BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<s.getHeight();y++)for(int x=0;x<s.getWidth();x++)
                if((s.getRGB(x,y)>>>24)>0x40)out.setRGB(x,y,0xffffffff);
            return out;
        });
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
        if(bankTiles!=null) {
            var map=game.map();var ink=(Graphics2D)canvas.create();
            ink.translate(-cameraX,-cameraY);ink.clip(map.bankShape());
            for(int y=Math.max(0,cameraY/64);y<Math.min(map.heightInTiles(),(cameraY+HEIGHT)/64+1);y++)
                for(int x=Math.max(0,cameraX/64);x<Math.min(map.widthInTiles(),(cameraX+WIDTH)/64+1);x++)
                    ink.drawImage(bankTiles[map.bankMask(x,y)],x*64,y*64,null);
            ink.dispose();return;
        }
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
        if(room==1)return; // The reference breach uses authored fallen equipment instead of repeated stamps.
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

    private void drawWeather(Graphics2D canvas,int cameraX,int cameraY,boolean ground) {
        if(ground&&reducedEffects)return;
        var map=game.map();
        double clock=Math.rint(weatherTime*1_000_000)/1_000_000;
        // Fixed-size analytic particles: no per-frame particle allocations or growing queues.
        for(int i=0;i<240;i+=reducedEffects?4:1) {
            int layer=i%3;
            double life=.72+Math.floorMod(rainHash(i),57)*.01;
            double cycle=(clock+i*.173)/life;
            int generation=(int)Math.floor(cycle);
            double phase=cycle-generation;
            if(ground!=(phase>=.84))continue;
            int seed=rainHash(i*7919+generation*104729+map.room()*313);
            int landingX=Math.floorMod(seed,map.worldWidth()/2)*2;
            int landingY=Math.floorMod(rainHash(seed),map.worldHeight()/2)*2;
            int x=(landingX-cameraX)/2*2,y=(landingY-cameraY)/2*2;
            if(ground) {
                if(x< -12||x>WIDTH+12||y< -8||y>HEIGHT+8||map.waterBlocked(landingX,landingY,2))continue;
                if(rainSplashSheet!=null&&rainSplashSheet.getWidth()>=96&&rainSplashSheet.getHeight()>=32) {
                    int frame=Math.min(5,(int)((phase-.84)/.16*6)),row=i%2;
                    var composite=canvas.getComposite();
                    canvas.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,(float)(.36*(1-(phase-.84)/.16))));
                    canvas.drawImage(rainSplashSheet,x-16,y-16,x+16,y+16,frame*16,row*16,frame*16+16,row*16+16,null);
                    canvas.setComposite(composite);continue;
                }
                int spread=1+(int)((phase-.84)/.16*3);
                canvas.setColor(RAIN_INK[phase<.91?1:0]);
                canvas.fillRect(x-spread*2,y,2,2);canvas.fillRect(x+spread*2,y,2,2);
                if(spread<3){canvas.fillRect(x-2,y-2,4,2);canvas.fillRect(x,y+2,2,2);}
            } else {
                double height=(180+layer*65)*(1-phase/.84);
                x=(int)Math.floor((landingX-height*.18-cameraX)/2)*2;
                y=(int)Math.floor((landingY-height-cameraY)/2)*2;
                if(x< -64||x>WIDTH+64||y< -64||y>HEIGHT+64)continue;
                if(rainStreakSheet!=null&&rainStreakSheet.getWidth()>=64&&rainStreakSheet.getHeight()>=32) {
                    int column=layer==2?1:0;
                    var composite=canvas.getComposite();
                    canvas.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.16f+layer*.08f));
                    canvas.drawImage(rainStreakSheet,x-32,y-62,x+32,y+2,column*32,0,column*32+32,32,null);
                    canvas.setComposite(composite);continue;
                }
                for(int tail=0;tail<3+layer*2;tail++) {
                    canvas.setColor(RAIN_INK[tail<2?layer:0]);
                    canvas.fillRect(x-(tail/3)*2,y-tail*2,2,2);
                }
            }
        }
    }

    private static int rainHash(int seed) {
        seed=(seed^(seed>>>16))*0x45d9f3b;
        seed=(seed^(seed>>>16))*0x45d9f3b;
        return seed^(seed>>>16);
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
        for(var corpse:game.corpses()) {
            boolean consuming=corpse==game.absorptionTarget();
            double bodyX=corpse.x(),bodyY=corpse.y();
            int x=(int)Math.round(bodyX/2)*2-cameraX,y=(int)Math.round(bodyY/2)*2-cameraY+20;
            var remains=(Graphics2D)canvas.create();
            float alpha=(float)Math.min(1,(RuinedOutpostGame.CORPSE_LIFETIME-corpse.age())/2);
            // The landing splash encloses the remains; they stay inside while the body reforms.
            if(consuming)alpha*=(float)Math.max(0,Math.min(1,(.5-game.absorptionProgress())/.15));
            remains.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,Math.max(0,alpha)));
            if(!CampaignRenderer.drawCorpse(remains,corpse,x,y+2)) {
                BufferedImage sheet=consuming&&corpseConsumeSheet!=null?corpseConsumeSheet:corpseIdleSheet;
                if(sheet!=null) {
                    int columns=sheet.getWidth()/48;
                    int frame=consuming?Math.min(columns-1,(int)(game.absorptionProgress()*columns)):(int)(corpse.age()*6)%columns;
                    remains.drawImage(sheet,x-48,y-48,x+48,y+48,frame*48,0,frame*48+48,48,null);
                }
            }
            remains.dispose();
        }
    }

    private void drawCrescent(Graphics2D canvas,int cameraX,int cameraY,RuinedOutpostGame.Crescent crescent) {
        if(crescentSheet==null)return;
        var pixels=waterBuffer.createGraphics();
        pixels.setComposite(java.awt.AlphaComposite.Clear);pixels.fillRect(0,0,72,72);
        pixels.setComposite(java.awt.AlphaComposite.SrcOver);
        pixels.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        pixels.translate(36,36);pixels.rotate(Math.atan2(crescent.directionY(),crescent.directionX()));
        int frame=(int)(crescent.age()*16)%(crescentSheet.getWidth()/64);
        pixels.drawImage(crescentSheet,-32,-32,32,32,frame*64,0,frame*64+64,64,null);pixels.dispose();
        int x=(int)Math.round(crescent.x()/2)*2-cameraX,y=(int)Math.round(crescent.y()/2)*2-cameraY;
        var glow=(Graphics2D)canvas.create();
        glow.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,(float)crescent.opacity()));
        // This 64px sheet has a 42px core: native 1x matches the 44px damage diameter.
        glow.drawImage(waterBuffer,x-36,y-36,72,72,null);glow.dispose();
    }

    private void drawWater(Graphics2D canvas,int cameraX,int cameraY,WaterProjectile wave) {
            boolean broad=wave.heavy()&&tideFrontSheet!=null;
            boolean finisher=wave.kind()==WaterProjectile.Kind.FINISHER&&waterFinisherSheet!=null;
            boolean crest=wave.heavy()&&tideCrestSheet!=null;
            BufferedImage sheet=broad?tideFrontSheet:finisher?waterFinisherSheet:wave.heavy()?(crest?tideCrestSheet:tideWaveSheet):waterSlashSheet;
            if(sheet==null)return;
            int cell=broad?64:finisher||wave.heavy()?48:32;
            int frame=(int)(wave.age()*18)%(sheet.getWidth()/cell);
            BufferedImage buffer=broad?tideBuffer:waterBuffer;int size=buffer.getWidth();
            Graphics2D nativePixels=buffer.createGraphics();
            nativePixels.setComposite(java.awt.AlphaComposite.Clear);nativePixels.fillRect(0,0,size,size);
            nativePixels.setComposite(java.awt.AlphaComposite.SrcOver);
            nativePixels.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            nativePixels.translate(size/2,size/2);
            double angle=Math.atan2(wave.directionY(),wave.directionX());
            nativePixels.rotate(angle);
            BufferedImage wake=wave.heavy()&&tideFoamSheet!=null?tideFoamSheet:waterWakeSheet;
            if(wake!=null) {
                int wakeFrame=(int)(wave.age()*20)%(wake.getWidth()/32);
                int wakeLeft=wave.heavy()&&tideFoamSheet!=null?-32:-34;
                nativePixels.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,wave.jet()?.8f:.45f));
                nativePixels.drawImage(wake,wakeLeft,-16,wakeLeft+32,16,wakeFrame*32,0,wakeFrame*32+32,32,null);
                nativePixels.setComposite(java.awt.AlphaComposite.SrcOver);
            }
            // The new Tide crest faces right; only legacy diagonal art needs correction.
            if(!crest&&!broad&&!finisher)nativePixels.rotate(wave.heavy()?0.6:-0.6);
            if(!finisher)nativePixels.rotate(switch(wave.kind()) {
                case CUT->-.35;case RETURN_CUT->.35;case FINISHER->-.55;default->0;
            });
            if(wave.kind()==WaterProjectile.Kind.FINISHER&&!finisher) {
                // Two opposed native-size cuts, not an enlarged basic projectile.
                var crossing=(Graphics2D)nativePixels.create();
                crossing.rotate(1.1);crossing.translate(0,3);
                crossing.drawImage(sheet,-cell/2,-cell/2,cell/2,cell/2,frame*cell,0,(frame+1)*cell,cell,null);
                crossing.dispose();
                nativePixels.translate(0,-3);
            } else if(wave.kind()==WaterProjectile.Kind.COUNTER) {
                var echo=(Graphics2D)nativePixels.create();
                echo.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.3f));
                echo.drawImage(sheet,-cell/2-9,-cell/2,cell/2-9,cell/2,frame*cell,0,(frame+1)*cell,cell,null);
                echo.dispose();
            }
            nativePixels.drawImage(sheet,-cell/2,-cell/2,cell/2,cell/2,frame*cell,0,(frame+1)*cell,cell,null);
            nativePixels.dispose();
            if(broad) {
                // Clip the broad spray against the same terrain used by its swept collision lane.
                for(int y=0;y<size;y++)for(int x=0;x<size;x++)if((buffer.getRGB(x,y)>>>24)!=0
                        &&game.map().waterBlocked(wave.x()+(x-size/2)*2,
                                wave.y()+(y-size/2)*2+Player.COLLISION_Y_OFFSET,1))buffer.setRGB(x,y,0);
            }
            int px=(int)Math.round((wave.x()-cameraX)/2)*2,py=(int)Math.round((wave.y()-cameraY)/2)*2;
            Graphics2D effect=(Graphics2D)canvas.create();
            float alpha=(float)wave.opacity();
            effect.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,Math.max(0,alpha)));
            effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            effect.drawImage(buffer,px-size,py-size,size*2,size*2,null);
            effect.dispose();
    }

    private void drawImpacts(Graphics2D canvas, int cameraX, int cameraY) {
        drawImpacts(canvas,cameraX,cameraY,null);
    }

    /** Null includes both layers for standalone previews; world painting separates the source burst. */
    private void drawImpacts(Graphics2D canvas,int cameraX,int cameraY,Boolean groundLayer) {
        for (Impact impact : impacts) {
            if(groundLayer!=null&&(impact.kind==Fx.TIDE_RELEASE)!=groundLayer)continue;
            BufferedImage sheet=switch(impact.kind) {
                case WATER -> waterSplashSheet;
                case FINISHER -> finisherImpactSheet;
                case TIDE -> tideBreakSheet;
                case TIDE_RELEASE -> tideReleaseSheet;
                case TIDE_FOAM -> tideFoamSheet;
                case WARDEN -> game.campaignMode()&&game.biome()==1&&briarImpactSheet!=null?briarImpactSheet:wardenImpactSheet;
                case BLADE,BLADE_HURT -> armorSparksSheet;
                case SLIME_HURT -> slimeHurtSheet;
                case PICKUP -> ichorPickupSheet;
                case STONE -> stoneChipsSheet;
                case SPIT -> spitImpactSheet;
                case HEAL -> healSheet;
                case COUNTER -> counterSheet;
            };
            int cell=impact.kind.cell;
            if(sheet==null) {
                switch(impact.kind) {
                    case TIDE -> {sheet=tideImpactSheet;cell=48;}
                    case TIDE_RELEASE -> {sheet=waterChargeSheet;cell=32;}
                    case TIDE_FOAM -> {sheet=waterWakeSheet;cell=32;}
                    case WARDEN -> {sheet=stoneChipsSheet;cell=32;}
                    case FINISHER -> {sheet=waterSplashSheet;cell=32;}
                    default -> { }
                }
            }
            if(sheet==null||sheet.getWidth()<cell||sheet.getHeight()<cell)continue;
            int columns=sheet.getWidth()/cell;
            int frame=Math.min(columns-1,(int)(impact.time/impact.kind.duration*columns));
            int row=Math.floorMod(impact.variant,sheet.getHeight()/cell);
            int x=(int)Math.round((impact.x-cameraX)/2)*2-cell,y=(int)Math.round((impact.y-cameraY)/2)*2-cell;
            var effect=(Graphics2D)canvas.create();
            float alpha=(float)Math.max(0,Math.min(1,(impact.kind.duration-impact.time)/.12));
            if(impact.kind==Fx.TIDE_FOAM)alpha*=.55f;
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

    private void drawPlayerHud(Graphics2D canvas) {
        Player p=game.player();
        if(hudFrame!=null) {
            canvas.setColor(new Color(9,15,23,240));canvas.fillRect(32,30,288,98);
            canvas.drawImage(hudFrame,16,16,320,128,null);
        }
        else uiPanel(canvas,16,16,320,128);
        pixelText(canvas,"RAINORAY",38,32,2,new Color(191,203,209));
        int healthSize=p.maxHealth()>5?32:64, healthStep=p.maxHealth()>5?34:50;
        int healthY=p.maxHealth()>5?54:42;
        for(int i=0;i<p.maxHealth();i++) {
            int x=32+i*healthStep;
            double part=Math.max(0,Math.min(1,p.healthValue()-i));
            var pip=(Graphics2D)canvas.create();
            pip.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,.18f));
            icon(pip,vitalityIcon,x,healthY,healthSize);pip.dispose();
            pip=(Graphics2D)canvas.create();
            if(part<1)pip.clipRect(x,healthY,(int)(healthSize*part),healthSize);
            icon(pip,vitalityIcon,x,healthY,healthSize);pip.dispose();
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
        if(!game.campaignMode()) {
            uiPanel(canvas,1100,20,160,66);
            icon(canvas,p.bladeForm()?bladeIcon:blobIcon,1106,21,64);
            pixelText(canvas,p.bladeForm()?"BLADE":"BLOB",1170,38,2,p.bladeForm()?GOLD:TEAL);
            pixelText(canvas,GameAudio.muted()?"MUTED":"",1170,64,1,new Color(139,156,170));
        }
        if(p.bladeForm()) {
            Color color=p.bladeSeconds()<=3?RED:TEAL;
            uiPanel(canvas,482,20,316,54);
            pixelText(canvas,"BLADE",500,32,2,color);
            if(p.unlimitedBlade())pixelText(canvas,"UNLIMITED",650,32,2,color);
            else pixelText(canvas,String.format(java.util.Locale.ROOT,"%.1f S",p.bladeSeconds()),702,32,2,color);
            canvas.setColor(new Color(45,48,58));canvas.fillRect(500,58,280,6);
            canvas.setColor(color);canvas.fillRect(500,58,(int)(280*p.bladeSeconds()/p.bladeDuration()),6);
        } else if(p.recovering()) pixelCentered(canvas,"REFORMING",640,34,2,RED);
        int first=p.bladeForm()?436:478;
        skill(canvas,first,p.bladeForm()?bladeIcon:slashIcon,"LMB",1,!p.recovering());
        skill(canvas,first+84,p.bladeForm()?crescentIcon:waveIcon,"RMB",p.bladeForm()
                ?1-game.crescentCooldown()/RuinedOutpostGame.CRESCENT_COOLDOWN
                :1-game.tideCooldown()/RuinedOutpostGame.TIDE_COOLDOWN,
                !p.recovering()&&(!p.bladeForm()||p.ichor()>RuinedOutpostGame.BLADE_SKILL_COST));
        skill(canvas,first+168,dashIcon,"SPACE",1-p.dashCooldown()/Player.DASH_COOLDOWN,!p.recovering());
        skill(canvas,first+252,bladeIcon,"Q",p.ichor()/100,!p.bladeForm()&&!p.recovering());
        if(p.bladeForm()) {
            for(int i=0;i<4;i++) {
                canvas.setColor(i<game.comboStep()?TEAL:new Color(62,77,90));
                canvas.fillRect(first+8+i*15,594,11,4);
            }
            pixelCentered(canvas,game.bladePreparing()?(game.chargeProgress()>=1?"CHARGED / RELEASE":"CHARGING"):
                    "CLICK COMBO / HOLD CHARGE",640,578,1,TEAL);
        }
        if(p.bladeForm())skill(canvas,first+336,riposteIcon,"F",1-game.riposteCooldown()/RuinedOutpostGame.RIPOSTE_COOLDOWN,
                p.ichor()>RuinedOutpostGame.BLADE_SKILL_COST);
        if(p.ichor()>=100&&!p.bladeForm())pixelCentered(canvas,"TRANSFORM",770,601,1,GOLD);
        if(p.trait()!=Player.Trait.NONE) {
            boolean armor=p.trait()==Player.Trait.MEMBRANE;Color color=armor?GOLD:TEAL;
            uiPanel(canvas,478,538,332,54);
            icon(canvas,consumeIcon,486,545,32);
            pixelText(canvas,p.trait().label(),526,548,2,color);
            pixelText(canvas,armor?"BLOCK 0.5 HP / ONE HIT":"SHOT SPEED 1.3X",526,571,1,new Color(191,203,209));
            pixelText(canvas,String.format(java.util.Locale.ROOT,"%.1f",p.traitSeconds()),774,571,1,color);
            canvas.setColor(new Color(39,48,57));canvas.fillRect(488,584,312,3);
            canvas.setColor(color);canvas.fillRect(488,584,(int)(312*p.traitSeconds()/Player.TRAIT_DURATION),3);
        }
    }

    private void drawHud(Graphics2D canvas) {
        drawPlayerHud(canvas);
        if(game.map().room()==9 && game.guardian().alive()) {
            Guardian boss=game.guardian();
            pixelCentered(canvas,boss.enraged()?"OUTPOST WARDEN / ENRAGED":"OUTPOST WARDEN",640,94,2,GOLD);
            drawHealthBar(canvas,640,122,380,boss.health(),Guardian.MAX_HEALTH,
                    boss.state()==Guardian.State.RECOVER?TEAL:RED);
        }
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
                if(i==9) {
                    if(guardianSheet!=null)canvas.drawImage(guardianSheet,x-16,y-16,x+16,y+16,0,0,64,64,null);
                    else pixelCentered(canvas,"W",x,y-6,2,RED);
                } else if(i==7||i==10)icon(canvas,i==10?healIcon:campIcon,x-16,y-16,32);
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
        String[][] controls={{"WASD / ARROWS","MOVE"},{"LMB / RMB",game.player().bladeForm()?"CLICK COMBO / RMB ARC":"SLASH / TIDE"},{"SPACE / F","DASH / RIPOSTE"},
                {"Q","TRANSFORM AT 100"},{"E","ABSORB / INTERACT"},{"TAB","HOLD MAP"},
                {"M",GameAudio.muted()?"SOUND OFF":"SOUND ON"},{"V",reducedEffects?"EFFECTS REDUCED":"EFFECTS FULL"}};
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
        // Native Wang transitions already contain the reviewed earth and paving edges.
        return source;
    }

    private static BufferedImage[] loadEnvironmentTiles() {
        var approved=loadImage("assets/tilesets/ruined_outpost/outpost_ground_reviewed.png");
        if(approved!=null)return outdoorTiles(renderTiles(approved));
        // ponytail: retain the stable blockout until a replacement passes in-scene art review.
        var source=renderTiles(loadImage("assets/tilesets/ruined_outpost/ruined_outpost_wang.png"));
        if(source==null)return null;
        var tiles=new BufferedImage[16];
        for(int mask=0;mask<16;mask++) {
            var tile=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
                double u=x/31.0,v=y/31.0;
                double blend=((mask&1)!=0?(1-u)*(1-v):0)+((mask&2)!=0?u*(1-v):0)
                        +((mask&4)!=0?(1-u)*v:0)+((mask&8)!=0?u*v:0);
                int hash=(x/3*31+y/2*17)%29;
                int earth=hash<2?0xff4a4b3e:hash<6?0xff373d35:0xff303630;
                int pixel=blend>=.5?source[15].getRGB(x*2,y*2):earth;
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
        bind(KeyEvent.VK_E,"interact",value->{
            if(!value)return;
            int before=game.biome();
            var from=nearPassage()?snapshot():null;
            game.interact();
            if(game.biome()!=before&&from!=null)startLoading(from,LoadingScreen.Kind.ENTER);
        });
        // clearInput() empties pressedKeys; put ESC back so a held key does not toggle pause on every repeat
        bind(KeyEvent.VK_ESCAPE,"pause",value->{
            if(!value)return;
            clearInput();pressedKeys.add(KeyEvent.VK_ESCAPE);
            if(menu!=null&&!game.paused()&&!game.story().blocksGameplay()&&game.player().alive())openMenu();
            else game.togglePause();
        });
        bind(KeyEvent.VK_TAB,"map",value->mapShown=value);
        bind(KeyEvent.VK_M,"mute",value->{if(value) GameAudio.setMuted(!GameAudio.muted());});
        bind(KeyEvent.VK_V,"effects",value->{if(value) reducedEffects=!reducedEffects;});
        bind(KeyEvent.VK_F,"riposte",value->{if(value)game.riposte();});
        bind(KeyEvent.VK_C,"continue",value->{if(value&&game.continueCampaign())resetVisualState();});
        bind(KeyEvent.VK_N,"newCampaign",value->{
            if(value&&game.campaignMode()&&game.story().phase()==OutpostStory.Phase.PROLOGUE) {
                game.newCampaign();resetVisualState();
            }
        });
        bind(KeyEvent.VK_H,"saveCamp",value->{if(value)game.saveCheckpoint();});
        bind(KeyEvent.VK_T,"title",value->{if(value&&game.returnToTitle()){resetVisualState();if(menuShown())closeMenu();}});
        bind(KeyEvent.VK_O,"options",value->{
            if(value&&menu!=null){if(!menuShown())openMenu();menu.settings();}
            else if(value)CampaignOptions.show(this,()->{
                clearInput();game.pause();
                GameAudio.setMusicState(game.biome(),game.player().bladeForm(),false);
            },enabled->reducedEffects=enabled,reducedEffects);
        });
        bind(KeyEvent.VK_F1,"debug",value->{
            if(value&&menu!=null){if(!menuShown())openMenu();menu.admin();}
            else if(value)DebugPanel.show(this,game,()->{
                clearInput();game.pause();
                GameAudio.setMusicState(game.biome(),game.player().bladeForm(),false);
            });
        });
        String[] tracks={"vitality","capacity","efficiency","edge"};
        for(int i=0;i<tracks.length;i++) {
            String track=tracks[i];
            bind(KeyEvent.VK_1+i,"upgrade"+i,value->{
                if(!value)return;
                game.buyUpgrade(track);
            });
        }
        bind(KeyEvent.VK_R,"restart",value->{
            if(!value)return;
            if(game.story().phase()==OutpostStory.Phase.DEAD&&game.campaignMode()) {
                var from=snapshot();
                restartGame();
                startLoading(from,LoadingScreen.Kind.REFORM);
            } else if(game.story().phase()==OutpostStory.Phase.DEAD||game.story().phase()==OutpostStory.Phase.COMPLETE) restartGame();
        });
    }
    private boolean held(int a,int b) { return pressedKeys.contains(a)||pressedKeys.contains(b); }
    private int horizontal() {
        return (held(KeyEvent.VK_D,KeyEvent.VK_RIGHT)?1:0)-(held(KeyEvent.VK_A,KeyEvent.VK_LEFT)?1:0);
    }
    private int vertical() {
        return (held(KeyEvent.VK_S,KeyEvent.VK_DOWN)?1:0)-(held(KeyEvent.VK_W,KeyEvent.VK_UP)?1:0);
    }
    private void clearInput() { pressedKeys.clear();attackHeld=false;mapShown=false;game.cancelBladeInput(); }

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
        Player player=game.player();int[] aim=aimDirection();int facingX=aim[0],facingY=aim[1];
        if(player.bladeForm()) {
            if(heavy)game.requestBladeAttack(true,facingX,facingY);
            else game.requestBladeAttack(false,facingX,facingY);
            return;
        }
        if(!(heavy?game.tideWave(facingX,facingY):game.attack(facingX,facingY)))return;
        slimeAnimation.face(facingX,facingY);slimeAnimation.attack();
    }

    private int[] aimDirection() {
        Player player=game.player();
        int x=player.bladeForm()?bladeAnimation.facingHorizontal():slimeAnimation.facingHorizontal();
        int y=player.bladeForm()?bladeAnimation.facingVertical():slimeAnimation.facingVertical();
        if(mouseAimed) {
            double dx=mouseX+cameraPosition(player.x(),WIDTH,game.map().worldWidth())-player.x();
            double dy=mouseY+cameraPosition(player.y(),HEIGHT,game.map().worldHeight())-player.y();
            if(Math.hypot(dx,dy)>4) {
                double angle=Math.atan2(dy,dx);x=(int)Math.round(Math.cos(angle)*1000);y=(int)Math.round(Math.sin(angle)*1000);
            }
        }
        return new int[]{x,y};
    }

    private void transform() {
        game.transform();
    }

    private void restartGame() {
        boolean reforming=game.story().phase()==OutpostStory.Phase.DEAD;
        game.restart();
        resetVisualState();
        if(reforming)GameAudio.play(GameAudio.Cue.RESPAWN);
    }

    private void resetVisualState() {
        clearInput(); trails.clear();
        mouseAimed=false;
        slimeAnimation = new SlimeAnimation();
        bladeAnimation = new BladeAnimation();
        transformationAnimation = new TransformationAnimation();
        feedback = new CombatFeedback();
        impacts.clear();
        wardenFx = bossFx();mobFx.clear();
        bannerText = "";
        bannerTime = 0;
    }

    private void banner(String text, double seconds) {
        bannerText = text;
        bannerTime = seconds;
    }

    private static final int[] RELEASE_MODIFIERS = {0, InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK, InputEvent.META_DOWN_MASK};

    /** Arrow keys / W S walk the menu's controls; Enter presses the focused one. True when used. */
    private boolean menuKey(int keyCode) { return walk(menu,keyCode,true); }

    /** The same on the title menu; Enter with nothing focused falls through to the classic start key. */
    private boolean titleKey(int keyCode) { return walk(titleMenu,keyCode,false); }

    private static boolean walk(java.awt.Container root,int keyCode,boolean enterAlways) {
        var controls=PixelSwing.controls(root).stream().filter(c->c.isEnabled()&&c.isShowing()).toList();
        var owner=java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        int at=controls.indexOf(owner);
        switch(keyCode) {
            case KeyEvent.VK_UP,KeyEvent.VK_W,KeyEvent.VK_DOWN,KeyEvent.VK_S -> {
                if(controls.isEmpty())return true;
                int step=keyCode==KeyEvent.VK_UP||keyCode==KeyEvent.VK_W?-1:1;
                int next=at<0?(step>0?0:controls.size()-1):Math.floorMod(at+step,controls.size());
                controls.get(next).requestFocusInWindow();
                return true;
            }
            case KeyEvent.VK_ENTER -> {
                if(owner instanceof javax.swing.AbstractButton button&&button.isEnabled()&&at>=0) { button.doClick(); return true; }
                return enterAlways;
            }
            default -> {return false;}
        }
    }

    private void bind(int keyCode, String name, KeyState keyState) {
        InputMap inputs = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getActionMap();
        inputs.put(KeyStroke.getKeyStroke(keyCode, 0, false), name + "Pressed");
        inputs.put(KeyStroke.getKeyStroke(keyCode, InputEvent.SHIFT_DOWN_MASK, false), name + "Pressed");
        // a key let go while a modifier is held must still be released, or he keeps walking
        for (int modifiers : RELEASE_MODIFIERS)
            inputs.put(KeyStroke.getKeyStroke(keyCode, modifiers, true), name + "Released");
        actions.put(name+"Pressed",action(()->{
            if(loading.covering()) { loading.skip(); return; }      // any key hurries the screen along
            if(!menuShown()&&titleShown()&&titleKey(keyCode))return;
            if(menuShown()) {
                if(keyCode==KeyEvent.VK_ESCAPE&&pressedKeys.add(keyCode)) {
                    closeMenu();pressedKeys.add(keyCode);
                }
                if(menuKey(keyCode))return;
                // title, mute, effects and the menu's own page keys keep working over it; gameplay keys wait
                if(keyCode!=KeyEvent.VK_T&&keyCode!=KeyEvent.VK_M&&keyCode!=KeyEvent.VK_V
                        &&keyCode!=KeyEvent.VK_O&&keyCode!=KeyEvent.VK_F1)return;
            }
            if(pressedKeys.add(keyCode)) keyState.set(true);
        }));
        actions.put(name+"Released",action(()->{
            pressedKeys.remove(keyCode);if(!menuShown())keyState.set(false);
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
        Splash.step();
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
        WATER(32,.33),FINISHER(48,.42),TIDE(64,.4),BLADE(48,.32),SLIME_HURT(48,.38),BLADE_HURT(48,.38),
        PICKUP(32,.32),STONE(32,.45),SPIT(48,.3),HEAL(48,.65),COUNTER(80,.35),
        TIDE_RELEASE(64,.46),TIDE_FOAM(32,.36),WARDEN(64,.5);
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
