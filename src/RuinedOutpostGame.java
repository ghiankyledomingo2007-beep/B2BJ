import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class RuinedOutpostGame {
    public enum EventType {
        DASH, ATTACK, WATER_IMPACT, TIDE_IMPACT, TIDE_RELEASE, TIDE_DISSIPATE, ENEMY_HIT, ENEMY_DEFEATED, PLAYER_HIT, PICKUP,
        TRANSFORM, REVERT, GUARDIAN_AWAKENED, GUARDIAN_SLAM, VICTORY, ROOM_ENTERED, SPIT_SHOT, SPIT_IMPACT,
        ABSORB_START, ABSORBED, HEALED, TRAIT_GAINED, MEMBRANE_BREAK, CRESCENT_CAST, CRESCENT_IMPACT, RIPOSTE_START, RIPOSTE_COUNTER
    }
    public record Event(EventType type, double x, double y, boolean water) {
        public Event(EventType type,double x,double y) { this(type,x,y,false); }
    }
    public static final class IchorDrop {
        private double x,y,amount,age,pullSpeed;
        public IchorDrop(double x,double y,double amount) {
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(amount)||amount<=0)
                throw new IllegalArgumentException("Invalid Ichor drop");
            this.x=x;this.y=y;this.amount=amount;
        }
        public double x(){return x;}
        public double y(){return y;}
        public double amount(){return amount;}
        public double age(){return age;}
        public boolean attracted(){return pullSpeed>0;}
    }
    public static final double PICKUP_RADIUS = 24, MAGNET_RADIUS=160;
    public static final double PASSIVE_ICHOR_PER_SECOND = 0.6;
    public static final double ABSORB_RADIUS = 84, ABSORB_DURATION = 0.8, CORPSE_LIFETIME = 20;
    public static final class Corpse {
        private double x,y;
        private final boolean healing;
        private final CampaignEnemy.Kind kind;
        private final int direction;
        private double age;
        private Corpse(double x,double y,boolean healing,CampaignEnemy.Kind kind,int direction) {
            this.x=x;this.y=y;this.healing=healing;this.kind=kind;this.direction=direction;
        }
        public double x(){return x;}
        public double y(){return y;}
        public double age(){return age;}
        public boolean healing(){return healing;}
        public CampaignEnemy.Kind kind(){return kind;}
        public int direction(){return direction;}
        public Player.Trait trait() {
            if(!healing||kind==null)return Player.Trait.NONE;
            return switch(kind) {
                case OUTPOST_GUARD,FALLEN_KNIGHT -> Player.Trait.MEMBRANE;
                case OUTPOST_SPITTER,MIRE_SHAMAN,CINDER_HEXER -> Player.Trait.JET;
                default -> Player.Trait.NONE;
            };
        }
    }
    public static final double WISP_CONTACT_RADIUS = 58;
    public static final double TIDE_COOLDOWN = 4.5;
    public static final double CRESCENT_COOLDOWN = 5, RIPOSTE_COOLDOWN = 6, RIPOSTE_DURATION = .3;
    public static final double BLADE_SKILL_COST = 8, RIPOSTE_RANGE = 120;
    /** A single non-piercing arc; collision and lifetime use the same native-size footprint. */
    public static final class Crescent {
        private double x,y,age;
        private final double dx,dy;
        private boolean alive=true;
        private Crescent(double x,double y,int aimX,int aimY) {
            this.x=x;this.y=y;double length=Math.hypot(aimX,aimY);dx=aimX/length;dy=aimY/length;
        }
        public double x(){return x;}
        public double y(){return y;}
        public double age(){return age;}
        public double directionX(){return dx;}
        public double directionY(){return dy;}
        public double radius(){return 22;}
        public double opacity(){return Math.min(1,Math.max(0,(.65-age)/.12));}
    }
    public static final double BLADE_ATTACK_RANGE = 150;
    public static final double SCOUT_ICHOR = 10;
    private RuinedOutpostMap map;
    private Player player;
    private List<Wisp> scouts;
    private Guardian guardian;
    private OutpostStory story;
    private final List<IchorDrop> drops = new ArrayList<>();
    private final List<Corpse> corpses = new ArrayList<>();
    private Corpse absorptionTarget;
    private double absorptionTime, absorptionX, absorptionY, absorptionHealth, corpseStartX, corpseStartY;
    private final List<Event> events = new ArrayList<>();
    private final List<WaterProjectile> projectiles = new ArrayList<>();
    private final List<Crescent> crescents = new ArrayList<>();
    private final List<EnemyProjectile> hostileProjectiles=new ArrayList<>();
    private final Set<Wisp> lungeHits = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Wisp> dashHits = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Wisp> bossAdds = Collections.newSetFromMap(new IdentityHashMap<>());
    private final boolean[] cleared = new boolean[12];
    private final boolean[] visited = new boolean[12];
    private boolean paused;
    private boolean tutorialTransformed;
    private boolean infirmaryUsed;
    private boolean bossDefeated;
    private int checkpoint;
    private int guardianHitImpact=-1;
    private int addSpawnIndex;
    private double addTimer;
    private double attackDelay;
    private int aimX, aimY;
    private int strikeDamage;
    private double strikeRange;
    private int combo;
    private double comboTime;
    private double hitStop;
    private double passageCooldown;
    private double tideCooldown;
    private double counterWindow, dashX, dashY, waterWindup;
    private double crescentCooldown,riposteCooldown,guardTime;
    private boolean waterCast, heavyCast;
    private WaterProjectile.Kind waterKind=WaterProjectile.Kind.CUT;
    private boolean campaignMode, choosingEnding, saveBlocked;
    private boolean debugSession, debugOneShot;
    private int biome, shards, campaignCheckpoint;
    private CampaignStory campaignStory;
    private CampaignStory.Ending campaignEnding;
    private java.nio.file.Path savePath;
    private CampaignSave.Progress savedProgress;
    private String campaignNotice="";
    private double noticeTime;
    private void notice(String text) { campaignNotice=text;noticeTime=8; }
    private final Set<Integer> clearedBosses=new java.util.HashSet<>();
    private final Set<String> campaignUnlocks=new java.util.HashSet<>(), endings=new java.util.HashSet<>();
    private final java.util.Map<String,Integer> upgrades=new java.util.HashMap<>();
    private final java.util.Map<Wisp,CampaignEnemy> campaignEnemies=new IdentityHashMap<>();

    public static RuinedOutpostGame campaign() { return campaign(null); }
    public static RuinedOutpostGame campaign(java.nio.file.Path path) {
        RuinedOutpostGame game=new RuinedOutpostGame();
        game.campaignMode=true;game.savePath=path;game.resetCampaign();
        if(path!=null)try { game.savedProgress=CampaignSave.load(path).orElse(null); }
        catch(java.io.IOException failure) {
            game.saveBlocked=true;
            game.campaignNotice="SAVE COULD NOT BE READ. ORIGINAL PRESERVED. NEW GAME REQUIRES CONFIRMATION.";
        }
        return game;
    }

    private void resetCampaign() {
        clearedBosses.clear();campaignUnlocks.clear();endings.clear();upgrades.clear();
        shards=campaignCheckpoint=0;campaignEnding=null;choosingEnding=paused=false;
        debugSession=debugOneShot=false;
        campaignStory=new CampaignStory();story=new OutpostStory();player=new Player(640,384);
        enterBiome(0,0);events.clear();
    }

    private void enterBiome(int destination,int arrival) {
        biome=destination;map=RuinedOutpostMap.campaign(biome);
        campaignCheckpoint=arrival==1?1:0;
        scouts=new ArrayList<>();campaignEnemies.clear();
        drops.clear();corpses.clear();cancelAbsorption();events.clear();
        lungeHits.clear();dashHits.clear();bossAdds.clear();projectiles.clear();crescents.clear();hostileProjectiles.clear();
        tideCooldown=crescentCooldown=riposteCooldown=guardTime=attackDelay=comboTime=hitStop=addTimer=counterWindow=0;
        combo=addSpawnIndex=0;guardianHitImpact=-1;passageCooldown=.35;
        for(var spawn:campaignArea().enemies()) {
            CampaignEnemy enemy=new CampaignEnemy(spawn.kind(),spawn.x(),spawn.y());
            scouts.add(enemy.body());campaignEnemies.put(enemy.body(),enemy);
        }
        guardian=new Guardian(map.guardianX(),map.guardianY(),Guardian.Profile.values()[biome]);
        bossDefeated=clearedBosses.contains(biome);
        if(bossDefeated) {
            guardian.activate(map.spawnX(),map.spawnY());guardian.hurt(guardian.maxHealth());
            map.clearGuardian();map.openGate();
        }
        var point=arrival==1?campaignArea().hub():arrival==2?campaignArea().exit():campaignArea().spawn();
        player.relocate(point.x(),point.y());
        if(arrival==2)player.relocate(point.x()-160,point.y());
        emit(EventType.ROOM_ENTERED,player.x(),player.y());
    }

    private void applyUpgrades() {
        player.configureUpgrades(upgradeLevel("vitality"),upgradeLevel("capacity"),
                upgradeLevel("efficiency"),upgradeLevel("edge"));
    }

    public boolean continueCampaign() {
        if(!campaignMode||savedProgress==null||story.phase()!=OutpostStory.Phase.PROLOGUE)return false;
        var progress=savedProgress;
        CampaignStory restoredStory=new CampaignStory();restoredStory.restoreQuestFlags(progress.questFlags());
        clearedBosses.clear();clearedBosses.addAll(progress.clearedBosses());
        campaignUnlocks.clear();campaignUnlocks.addAll(progress.unlocks());
        upgrades.clear();upgrades.putAll(progress.upgrades());endings.clear();endings.addAll(progress.endings());
        shards=progress.shards();campaignStory=restoredStory;
        player=new Player(640,384);applyUpgrades();player.heal();
        debugSession=debugOneShot=false;
        story.resume();campaignEnding=null;choosingEnding=paused=false;
        enterBiome(progress.biome(),progress.checkpoint());
        notice("CAMPAIGN RESTORED");return true;
    }

    private void saveProgress() {
        // A test session never replaces either the disk save or the in-memory Continue snapshot.
        if(saveBlocked||debugSession)return;
        var progress=new CampaignSave.Progress(biome,campaignCheckpoint,clearedBosses,campaignUnlocks,
                upgrades,campaignStory.questFlags(),endings,shards);
        savedProgress=progress;
        if(savePath!=null)try { CampaignSave.save(savePath,progress); }
        catch(java.io.IOException failure) { campaignNotice="SAVE FAILED. PROGRESS STILL IN MEMORY.";return; }
    }

    public boolean saveCheckpoint() {
        if(!campaignMode||blocked()||!nearCampaignHub())return false;
        if(scouts.stream().anyMatch(w->w.alive()&&near(w.x(),w.y(),400))
                ||(bossActive()&&near(guardian.x(),guardian.y(),600))) {
            notice("CAMP IS NOT SAFE YET");return false;
        }
        campaignCheckpoint=1;player.heal();
        notice(debugSession?"RESTED AT CAMP / TEST SESSION NOT SAVED":"RESTED AT CAMP / CHECKPOINT SAVED");
        saveProgress();return true;
    }

    public boolean nearCampaignHub() {
        return campaignMode&&near(campaignArea().hub().x(),campaignArea().hub().y(),160);
    }
    public int upgradeCost(String name) {
        return "edge".equals(name)?4:2*(upgradeLevel(name)+1);
    }
    public boolean buyUpgrade(String name) {
        if(!campaignMode||blocked()||!nearCampaignHub()||name==null||!Set.of("vitality","capacity","efficiency","edge").contains(name))return false;
        int level=upgradeLevel(name),cost=upgradeCost(name);
        if(level>=(name.equals("edge")?1:3)||shards<cost) {notice("NOT ENOUGH SHARDS OR TRACK COMPLETE");return false;}
        shards-=cost;upgrades.put(name,level+1);applyUpgrades();
        if(name.equals("vitality"))player.heal(1);
        notice(name.toUpperCase(java.util.Locale.ROOT)+" UPGRADED");saveProgress();return true;
    }

    public boolean chooseEnding(CampaignStory.Ending ending) {
        if(!campaignMode||!choosingEnding||ending==null||clearedBosses.size()!=4)return false;
        campaignEnding=ending;choosingEnding=false;endings.add(ending.name());
        story.resume();story.enterGatehouse();story.guardianDefeated();story.escaped();
        cancelAbsorption();projectiles.clear();crescents.clear();hostileProjectiles.clear();
        emit(EventType.VICTORY,player.x(),player.y());saveProgress();return true;
    }

    public boolean returnToTitle() {
        if(!campaignMode||!paused||!player.alive()||story.blocksGameplay())return false;
        saveProgress();cancelAbsorption();campaignStory.advanceDialogue();story=new OutpostStory();paused=false;
        choosingEnding=false;campaignEnding=null;return true;
    }

    private boolean campaignInteract() {
        if(campaignStory.dialogueOpen()) {campaignStory.advanceDialogue();return true;}
        if(blocked())return false;
        if(nearCampaignHub()) {
            boolean first=!campaignStory.questFlags().contains("talked_"+biome);
            if(campaignStory.talk(biome))grantShards(2);
            if(first&&biome==0)player.collectIchor(100);
            saveProgress();return true;
        }
        for(var landmark:campaignArea().landmarks())if(landmark.optional()&&near(landmark.x(),landmark.y(),120)) {
            String key="landmark_"+biome+"_"+campaignArea().landmarks().indexOf(landmark);
            if(campaignUnlocks.add(key))grantShards(1);
            if(landmark==campaignArea().landmarks().stream().filter(CampaignWorld.Landmark::optional).findFirst().orElse(null))
                campaignStory.discover(biome);
            notice(landmark.name()+" / "+landmark.lore());saveProgress();return true;
        }
        if(near(campaignArea().exit().x(),campaignArea().exit().y(),140)) {
            if(!bossDefeated) {notice("DEFEAT "+guardian.bossName().toUpperCase(java.util.Locale.ROOT)+" TO OPEN THIS ROUTE");return false;}
            if(biome==3) {choosingEnding=true;return true;}
            story.resume();enterBiome(biome+1,0);notice(String.join(" ",CampaignStory.transitionLines(biome)));saveProgress();return true;
        }
        if(biome>0&&near(campaignArea().returnPortal().x(),campaignArea().returnPortal().y(),120)) {
            story.resume();enterBiome(biome-1,2);notice("RETURNED TO "+campaignArea().name());saveProgress();return true;
        }
        return false;
    }

    public boolean campaignMode() { return campaignMode; }
    public int biome() { return biome; }
    public CampaignWorld.Area campaignArea() { return CampaignWorld.area(biome); }
    public CampaignStory campaignStory() { return campaignStory; }
    public CampaignEnemy campaignEnemy(Wisp body) { return campaignEnemies.get(body); }
    public boolean bossActive() { return campaignMode?!bossDefeated&&guardian.state()!=Guardian.State.DORMANT:map.room()==9; }
    public int shards() { return shards; }
    public int upgradeLevel(String key) { return upgrades.getOrDefault(key,0); }
    public String campaignNotice() { return campaignNotice; }
    public CampaignStory.Ending campaignEnding() { return campaignEnding; }
    public boolean choosingEnding() { return choosingEnding; }
    public boolean saveAvailable() { return savedProgress!=null; }
    public int campaignCheckpoint() { return campaignCheckpoint; }
    public boolean biomeUnlocked(int id) { return id>=0&&id<4&&(id==0||clearedBosses.contains(id-1)); }

    private void grantShards(int amount) { shards=(int)Math.min(100_000L,(long)shards+amount); }

    public boolean debugAvailable() {
        return campaignMode&&player.alive()&&!story.blocksGameplay()&&!choosingEnding
                &&campaignEnding==null&&!campaignStory.dialogueOpen();
    }
    public boolean debugSession() { return debugSession; }
    public boolean debugGodMode() { return player.godMode(); }
    public boolean debugOneShot() { return debugOneShot; }
    public boolean setDebugGodMode(boolean enabled) {
        if(!debugAvailable())return false;
        if(player.godMode()!=enabled)debugSession=true;
        player.setGodMode(enabled);return true;
    }
    public boolean setDebugOneShot(boolean enabled) {
        if(!debugAvailable())return false;
        if(debugOneShot!=enabled)debugSession=true;
        debugOneShot=enabled;return true;
    }
    public boolean debugRefill() {
        if(!debugAvailable())return false;
        debugSession=true;player.refillForTesting();cancelAbsorption();
        tideCooldown=crescentCooldown=riposteCooldown=guardTime=attackDelay=comboTime=counterWindow=hitStop=0;
        combo=0;dashHits.clear();
        notice("TEST REFILL / HEALTH, ICHOR AND COOLDOWNS RESET");return true;
    }
    public boolean debugAddShards() {
        if(!debugAvailable())return false;
        debugSession=true;grantShards(25);notice("TEST WALLET / +25 SHARDS");return true;
    }
    public boolean debugWarp(int destination,boolean boss) {
        if(!debugAvailable()||destination<0||destination>=CampaignWorld.AREA_COUNT)return false;
        debugSession=true;
        for(int previous=0;previous<destination;previous++)clearedBosses.add(previous);
        if(boss)clearedBosses.removeIf(id->id>=destination);
        enterBiome(destination,1);
        if(boss)debugApproachBoss();
        notice("TEST TRAVEL / "+campaignArea().name());return true;
    }
    public boolean debugResetEncounter() {
        if(!debugAvailable())return false;
        boolean atBoss=near(guardian.x(),guardian.y(),650);
        debugSession=true;clearedBosses.removeIf(id->id>=biome);
        enterBiome(biome,1);
        if(atBoss)debugApproachBoss();
        notice("TEST RESET / CURRENT REGION RESPAWNED");return true;
    }
    private void debugApproachBoss() {
        for(int slot=0;slot<16;slot++) {
            double angle=Math.PI+slot*Math.PI/8;
            double x=guardian.x()+Math.cos(angle)*350,y=guardian.y()+Math.sin(angle)*350;
            if(map.isBlocked(x,y+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS))continue;
            player.relocate(x,y);map.setGuardianPresent(true);guardian.activate(x,y);return;
        }
        // Keep the camp spawn if terrain offers no safe approach point.
    }

    public RuinedOutpostGame() { resetDefaultState(); }
    RuinedOutpostGame(RuinedOutpostMap map, Player player, List<Wisp> scouts, Guardian guardian) {
        this.map = map;
        this.player = player;
        this.scouts = new ArrayList<>(scouts);
        this.guardian = guardian;
        story = new OutpostStory();
        visited[map.room()] = true;
    }
    public void begin() {
        if(campaignMode) {
            if(campaignStory.dialogueOpen()) {campaignStory.advanceDialogue();return;}
            if(story.phase()!=OutpostStory.Phase.PROLOGUE)return;
            // Existing saves are not replaced by a casual title-screen click.
            if(savedProgress!=null||saveBlocked) {
                notice("C CONTINUE / N CONFIRM NEW GAME (REPLACES SAVE)");return;
            }
        }
        story.begin();if(campaignMode)saveProgress();
    }
    public void newCampaign() {
        if(!campaignMode||story.phase()!=OutpostStory.Phase.PROLOGUE)return;
        resetCampaign();saveBlocked=false;story.begin();notice("FIND MARA AT THE FIELD CAMP");saveProgress();
    }
    public void restart() {
        if(campaignMode) {
            if(story.phase()==OutpostStory.Phase.DEAD) {
                boolean god=player.godMode();
                player=new Player(640,384);applyUpgrades();player.heal();story.resume();paused=false;
                player.setGodMode(god);
                enterBiome(biome,campaignCheckpoint);notice("REFORMED AT YOUR CHECKPOINT");
            } else if(story.phase()==OutpostStory.Phase.COMPLETE) {
                story=new OutpostStory();campaignEnding=null;choosingEnding=paused=false;
            }
            return;
        }
        if (story.phase() == OutpostStory.Phase.DEAD && checkpoint == 7) {
            player = new Player(640,384);
            tideCooldown=crescentCooldown=riposteCooldown=guardTime=0;
            story.resume();
            paused = false;
            enterRoom(7,-1);
        } else resetDefaultState();
    }
    private void resetDefaultState() {
        java.util.Arrays.fill(cleared,false);
        java.util.Arrays.fill(visited,false);
        checkpoint = 0;
        tutorialTransformed = infirmaryUsed = bossDefeated = paused = false;
        tideCooldown=crescentCooldown=riposteCooldown=guardTime=0;
        story = new OutpostStory();
        player = new Player(640,384);
        enterRoom(0,-1);
        events.clear();
    }
    private void enterRoom(int room, int from) {
        map = new RuinedOutpostMap(room);
        scouts = new ArrayList<>();
        drops.clear();
        corpses.clear();
        cancelAbsorption();
        events.clear();
        lungeHits.clear();
        dashHits.clear();
        bossAdds.clear();
        projectiles.clear();
        crescents.clear();guardTime=0;
        hostileProjectiles.clear();
        attackDelay = comboTime = hitStop = addTimer = counterWindow = 0;
        combo = 0;
        guardianHitImpact=-1;
        addSpawnIndex=0;
        guardian = new Guardian(map.guardianX(),map.guardianY());
        double x = map.spawnX(), y = map.spawnY();
        for (RuinedOutpostMap.Door door : map.doors()) {
            if (door.destination() == from) {
                x = door.x() == 64 ? 224 : door.x() == map.worldWidth()-64 ? map.worldWidth()-224 : door.x();
                y = door.y() == 64 ? 224 : door.y() == map.worldHeight()-64 ? map.worldHeight()-224 : door.y();
            }
        }
        player.relocate(x,y);
        if (!cleared[room]) {
            int[][] positions = {{440,384},{840,384},{640,240},{640,544},{960,384}};
            for (int i=0; i<map.description().enemies(); i++) {
                double sx=map.authored(positions[i][0]), sy=map.authored(positions[i][1]);
                boolean guard=room>=2&&(i==0||(room>=3&&i==3));
                boolean spitter=(room==5||room==6||room==8||room==11)&&i==1;
                scouts.add(new Wisp(sx,sy,sx-56,sx+56,spitter?3:guard?4:2,
                        spitter?Wisp.Role.SPITTER:guard?Wisp.Role.GUARD:Wisp.Role.SCOUT));
            }
        }
        visited[room] = true;
        passageCooldown=0.35;
        if (room == 4 && !tutorialTransformed) player.collectIchor(100);
        if (room == 7) {
            checkpoint = 7;
            player.heal();
        }
        if (room == 9) {
            if (bossDefeated) {
                story.enterGatehouse();
                story.guardianDefeated();
                guardian.activate(x,y);
                guardian.hurt(Guardian.MAX_HEALTH);
                map.clearGuardian();
                map.openGate();
            } else {
                story.enterGatehouse();
                guardian.activate(x,y);
                events.add(new Event(EventType.GUARDIAN_AWAKENED,guardian.x(),guardian.y()));
            }
        }
        refreshPassages();
        events.add(new Event(EventType.ROOM_ENTERED,x,y));
    }
    public void update(double seconds, int horizontal, int vertical) {
        if (!Double.isFinite(seconds) || seconds <= 0 || blocked()) return;
        if(horizontal!=0||vertical!=0)cancelAbsorption();
        // Fixed maximum physics step prevents fast dashes skipping enemies or thin walls.
        for (double remaining=Math.min(seconds,0.5); remaining>0.000001; ) {
            double dt=Math.min(remaining,0.01);
            tick(dt,horizontal,vertical);
            remaining-=dt;
            if (blocked()) break;
        }
    }
    private void tick(double seconds, int horizontal, int vertical) {
        if(campaignMode&&noticeTime>0&&!campaignNotice.startsWith("SAVE FAILED")) {
            noticeTime=Math.max(0,noticeTime-seconds);
            if(noticeTime==0)campaignNotice="";
        }
        if (!player.alive()) { projectiles.clear(); crescents.clear(); guardTime=0; hostileProjectiles.clear(); attackDelay=0; cancelAbsorption(); story.playerDied(); return; }
        if (hitStop > 0) { hitStop=Math.max(0,hitStop-seconds); return; }
        passageCooldown=Math.max(0,passageCooldown-seconds);
        tideCooldown=Math.max(0,tideCooldown-seconds);
        crescentCooldown=Math.max(0,crescentCooldown-seconds);
        riposteCooldown=Math.max(0,riposteCooldown-seconds);
        guardTime=Math.max(0,guardTime-seconds);
        counterWindow=Math.max(0,counterWindow-seconds);
        boolean wasBlade=player.bladeForm(), wasDashing=player.dashing(), wasRecovering=player.recovering();
        player.move(horizontal,vertical,seconds,map);
        if(!player.bladeForm())guardTime=0;
        if (wasBlade && !player.bladeForm()) emit(EventType.REVERT,player.x(),player.y());
        comboTime=Math.max(0,comboTime-seconds);
        if (attackDelay > 0) {
            attackDelay=Math.max(0,attackDelay-seconds);
            if (attackDelay == 0) resolveStrike();
        }
        if(campaignMode&&!bossDefeated&&guardian.state()==Guardian.State.DORMANT
                &&near(guardian.x(),guardian.y(),600)) {
            map.setGuardianPresent(true);guardian.activate(player.x(),player.y());
            emit(EventType.GUARDIAN_AWAKENED,guardian.x(),guardian.y());
        }
        for (Wisp scout : scouts) {
            CampaignEnemy enemy=campaignEnemies.get(scout);
            if(enemy==null)scout.update(seconds,player.x(),player.y(),map);
            else enemy.update(seconds,player.x(),player.y(),map);
        }
        // Keep authored indices stable (elite rewards), but retire expired summons after their death pose.
        scouts.removeIf(scout->{
            if(!bossAdds.contains(scout)||scout.visible())return false;
            bossAdds.remove(scout);campaignEnemies.remove(scout);lungeHits.remove(scout);dashHits.remove(scout);
            return true;
        });
        if (bossActive()||(campaignMode&&guardian.state()==Guardian.State.DEAD)) updateGuardian(seconds);
        // ponytail: authored populations stay below 48; spatial bins only if profiling needs them.
        for(int i=0;i<scouts.size();i++)for(int j=i+1;j<scouts.size();j++) {
            scouts.get(i).separateFrom(scouts.get(j),seconds,map);
            scouts.get(j).separateFrom(scouts.get(i),seconds,map);
        }
        updateWater(seconds);
        updateCrescents(seconds);
        if (wasDashing || player.dashing()) damageEnemiesFromDash();
        // Resolve outgoing impacts before incoming contact/release: interrupted attacks cannot trade.
        boolean dashProtected=wasDashing&&!player.recovering();
        for(Wisp scout:scouts) {
            if(scout.state()!=Wisp.State.LUNGE) lungeHits.remove(scout);
            else if(!lungeHits.contains(scout)) {
                if(scout.role()==Wisp.Role.SPITTER) {
                    lungeHits.add(scout);
                    if(!campaignMode)emitSpit(scout);
                }
                else if((campaignEnemies.containsKey(scout)?campaignEnemies.get(scout).hits(player.x(),player.y()):near(scout.x(),scout.y(),WISP_CONTACT_RADIUS))
                        &&map.clearLine(scout.x(),scout.y()+Wisp.COLLISION_Y_OFFSET,
                                player.x(),player.y()+Player.COLLISION_Y_OFFSET)) {
                    lungeHits.add(scout);
                    if(!dashProtected)hurtPlayer(campaignEnemies.containsKey(scout)?campaignEnemies.get(scout).damage():1,scout.x(),scout.y());
                }
            }
        }
        if(campaignMode)for(CampaignEnemy enemy:campaignEnemies.values()) {
            var shots=enemy.releaseShots();
            if(hostileProjectiles.size()+shots.size()<=48)hostileProjectiles.addAll(shots);
            if(!shots.isEmpty())emit(EventType.SPIT_SHOT,enemy.body().x(),enemy.body().y());
        }
        if(bossActive()&&guardian.alive()&&guardianHitImpact!=guardian.impactNumber()
                &&guardian.hits(player.x(),player.y())) {
            guardianHitImpact=guardian.impactNumber();
            if(!dashProtected)hurtPlayer(Guardian.SLAM_DAMAGE,guardian.targetX(),guardian.targetY());
        }
        updateHostileProjectiles(seconds,dashProtected);
        if(campaignMode&&bossActive()&&player.bladeForm()&&near(guardian.x(),guardian.y(),guardian.auraRadius())) {
            player.spendBladeIchor(Math.min(player.ichor(),guardian.drainPerSecond()*seconds));
            if(!player.bladeForm())emit(EventType.REVERT,player.x(),player.y());
        }
        collectDrops(seconds);
        if(player.alive()&&!wasBlade&&!wasRecovering&&!player.bladeForm()&&!player.recovering())
            player.collectIchor(PASSIVE_ICHOR_PER_SECOND*seconds);
        updateAbsorption(seconds);
        if (!player.alive()) { projectiles.clear(); crescents.clear(); guardTime=0; hostileProjectiles.clear(); attackDelay=0; cancelAbsorption(); story.playerDied(); }
        if (!campaignMode&&scouts.stream().noneMatch(Wisp::alive) && map.room()!=9) cleared[map.room()]=true;
        refreshPassages();
        if(player.alive()&&passageCooldown==0) {
            for(var door:map.doors()) if(map.passageOpen(door)&&near(door.x(),door.y(),70)) {
                enterRoom(door.destination(),map.room());
                break;
            }
        }
    }
    private void updateGuardian(double seconds) {
        int impactBefore=guardian.impactNumber();
        guardian.update(seconds,player.x(),player.y(),map);
        if (!guardian.alive()) return;
        if (guardian.impactNumber()!=impactBefore) {
            emit(EventType.GUARDIAN_SLAM,guardian.targetX(),guardian.targetY());
        }
        addTimer+=seconds;
        long nearbyAdds=campaignMode?bossAdds.stream().filter(Wisp::alive).count():scouts.stream().filter(Wisp::alive).count();
        if (addTimer>=(guardian.enraged()?5:6) && nearbyAdds<(guardian.enraged()?3:2)) {
            int[][] slots={{400,544},{920,544},{400,240},{920,240},{640,544},{640,240}};
            for(int attempt=0;attempt<slots.length;attempt++) {
                int[] slot=slots[addSpawnIndex++%slots.length];
                double angle=(addSpawnIndex%6)*Math.PI/3;
                double x=campaignMode?guardian.x()+Math.cos(angle)*320:map.authored(slot[0]);
                double y=campaignMode?guardian.y()+Math.sin(angle)*320:map.authored(slot[1]);
                if(near(x,y,200)||map.isBlocked(x,y+Wisp.COLLISION_Y_OFFSET,Wisp.COLLISION_RADIUS)
                        ||Math.hypot(x-guardian.x(),y-guardian.y())<160
                        ||scouts.stream().anyMatch(w->w.alive()&&Math.hypot(x-w.x(),y-w.y())<90))continue;
                Wisp add;
                if(campaignMode) {
                    var enemy=new CampaignEnemy(biome==0?CampaignEnemy.Kind.OUTPOST_SCOUT:CampaignEnemy.Kind.RIFT_IMP,x,y);
                    add=enemy.body();campaignEnemies.put(add,enemy);
                } else add=new Wisp(x,y,x-60,x+60,2);
                scouts.add(add);bossAdds.add(add);addTimer=0;break;
            }
        }
    }
    private boolean near(double x,double y,double radius) {
        return Math.hypot(player.x()-x,player.y()-y)<=radius;
    }
    private void emit(EventType type,double x,double y) { events.add(new Event(type,x,y)); }
    private void hurtPlayer(int damage,double x,double y) {
        if(damage>0&&guarding()&&!player.invulnerable()) {
            guardTime=0;
            emit(EventType.RIPOSTE_COUNTER,player.x(),player.y());
            for(Wisp scout:scouts)if(scout.alive()&&near(scout.x(),scout.y(),RIPOSTE_RANGE)
                    &&map.clearLine(player.x(),player.y(),scout.x(),scout.y())&&hurtScout(scout,3,false,true)) {
                double dx=scout.x()-player.x(),dy=scout.y()-player.y(),length=Math.max(1,Math.hypot(dx,dy));
                scout.push(dx/length*300,dy/length*300);afterScoutHit(scout);
            }
            if(bossActive()&&!bossDefeated&&bossVulnerable()
                    &&near(guardian.x(),guardian.y(),RIPOSTE_RANGE)
                    &&map.clearLine(player.x(),player.y(),guardian.x(),guardian.y())&&guardian.hurt(bossDamage(3)))
                afterGuardianHit(false);
            return;
        }
        boolean blade=player.bladeForm();
        boolean membrane=player.trait()==Player.Trait.MEMBRANE;
        if (player.hurt(damage)) {
            if(membrane)emit(EventType.MEMBRANE_BREAK,player.x(),player.y());
            cancelAbsorption();
            emit(EventType.PLAYER_HIT,x,y);
            hitStop=0.045;
            if (blade&&!player.bladeForm()) emit(EventType.REVERT,player.x(),player.y());
        }
    }
    public boolean dash(int horizontal,int vertical) {
        if (blocked() || !player.dash(horizontal,vertical)) return false;
        cancelAbsorption();
        guardTime=0;
        attackDelay=0;
        dashHits.clear();
        comboTime=0;
        double length=Math.hypot(horizontal,vertical);
        dashX=horizontal/length;dashY=vertical/length;
        counterWindow=player.bladeForm()?0:Player.DASH_DURATION+.3;
        emit(EventType.DASH,player.x(),player.y());
        return true;
    }
    private void damageEnemiesFromDash() {
        if (player.bladeForm() || player.recovering() || !player.alive()) return;
        for (Wisp scout:scouts) {
            if (scout.alive()&&!dashHits.contains(scout)&&near(scout.x(),scout.y(),WISP_CONTACT_RADIUS)
                    &&map.clearLine(player.x(),player.y(),scout.x(),scout.y())) {
                dashHits.add(scout);
                if (hurtScout(scout,Player.SLIME_DAMAGE,true,true)) {
                    scout.push(dashX*300,dashY*300);
                    afterScoutHit(scout);
                }
            }
        }
        if(debugOneShot&&bossActive()&&!bossDefeated
                &&map.touchesGuardian(player.x(),player.y()+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS+8)
                &&map.clearLine(player.x(),player.y(),guardian.x(),guardian.y())
                &&guardian.hurt(guardian.health()))afterGuardianHit(true);
    }
    public boolean attack(int facingX,int facingY) {
        if (blocked() || guarding() || (facingX==0&&facingY==0) || !player.startAttack()) return false;
        cancelAbsorption();
        combo=comboTime>0?(combo+1)%(player.bladeForm()?player.bladeComboLength():3):0;
        comboTime=0.9;
        aimX=facingX; aimY=facingY;
        strikeDamage=player.attackDamage();
        if(player.bladeForm()&&combo==3)strikeDamage+=2;
        strikeRange=BLADE_ATTACK_RANGE;
        waterCast=!player.bladeForm(); heavyCast=false;
        if(waterCast) {
            boolean counter=counterWindow>0&&(dashX*facingX+dashY*facingY)/Math.hypot(facingX,facingY)<-.6;
            waterKind=counter?WaterProjectile.Kind.COUNTER:combo==2?WaterProjectile.Kind.FINISHER
                    :combo==1?WaterProjectile.Kind.RETURN_CUT:WaterProjectile.Kind.CUT;
            waterWindup=counter?.04:combo==2?.12:.08;
            if(counter) {combo=0;comboTime=0;}
        }
        counterWindow=0;
        attackDelay=waterCast?waterWindup:strikeWindup(combo);
        emit(EventType.ATTACK,player.x(),player.y());
        return true;
    }
    static double strikeWindup(int combo) { return combo==2?0.12:0.08; }
    public boolean ichorCrescent(int facingX,int facingY) {
        if(blocked()||guarding()||!player.bladeForm()||crescentCooldown>0
                ||player.ichor()<=BLADE_SKILL_COST||(facingX==0&&facingY==0)||!player.startAttack())return false;
        player.spendBladeIchor(BLADE_SKILL_COST);
        cancelAbsorption();attackDelay=0;comboTime=0;
        crescentCooldown=CRESCENT_COOLDOWN;
        crescents.add(new Crescent(player.x(),player.y(),facingX,facingY));
        emit(EventType.CRESCENT_CAST,player.x(),player.y());return true;
    }
    public boolean riposte() {
        if(blocked()||!player.bladeForm()||riposteCooldown>0||player.ichor()<=BLADE_SKILL_COST
                ||!player.startAttack())return false;
        player.spendBladeIchor(BLADE_SKILL_COST);
        cancelAbsorption();attackDelay=0;comboTime=0;
        guardTime=RIPOSTE_DURATION;riposteCooldown=RIPOSTE_COOLDOWN;
        emit(EventType.RIPOSTE_START,player.x(),player.y());return true;
    }
    public boolean tideWave(int facingX,int facingY) {
        if(blocked()||player.bladeForm()||tideCooldown>0||(facingX==0&&facingY==0)
                ||!player.startAttack()) return false;
        cancelAbsorption();
        aimX=facingX; aimY=facingY; waterCast=heavyCast=true;
        waterKind=WaterProjectile.Kind.TIDE;
        attackDelay=waterWindup=0.18; tideCooldown=TIDE_COOLDOWN;
        comboTime=counterWindow=0;
        emit(EventType.ATTACK,player.x(),player.y());
        return true;
    }
    private void resolveStrike() {
        if(waterCast) {
            if(!player.bladeForm()) {
                projectiles.add(new WaterProjectile(player.x(),player.y(),aimX,aimY,waterKind,player.trait()==Player.Trait.JET));
                if(heavyCast)events.add(new Event(EventType.TIDE_RELEASE,player.x(),player.y(),true));
            }
            return;
        }
        if(!player.bladeForm()) return;
        for (Wisp scout:scouts) {
            if (map.clearLine(player.x(),player.y(),scout.x(),scout.y())
                    &&scout.hitFrom(player.x(),player.y(),aimX,aimY,strikeRange,debugOneShot?scout.health():strikeDamage))
                afterScoutHit(scout);
        }
        if (bossActive()&&!bossDefeated&&bossVulnerable()
                &&map.clearLine(player.x(),player.y(),guardian.x(),guardian.y())
                &&guardian.hitFrom(player.x(),player.y(),aimX,aimY,strikeRange,bossDamage(strikeDamage))) {
            afterGuardianHit(false);
        }
    }
    private void afterGuardianHit(boolean water) {
        events.add(new Event(EventType.ENEMY_HIT,guardian.x(),guardian.y(),water));
        hitStop=0.04;
        if (!guardian.alive()) {
                bossDefeated=true;
                if(campaignMode) {
                    if(clearedBosses.add(biome))grantShards(4+biome*2);
                    notice(guardian.bossName().toUpperCase(java.util.Locale.ROOT)+" DEFEATED / ROUTE OPEN");
                    saveProgress();
                } else {cleared[9]=true;story.guardianDefeated();}
                map.clearGuardian();
                map.openGate();
                for (Wisp add:scouts) if(add.alive()&&(!campaignMode||bossAdds.contains(add))) add.hurt(add.health());
                events.add(new Event(EventType.ENEMY_DEFEATED,guardian.x(),guardian.y(),water));
        }
    }
    private void updateWater(double seconds) {
        for(WaterProjectile wave:projectiles) {
            // Small swept steps prevent tunnelling through a thin gate or fast target.
            int steps=Math.max(1,(int)Math.ceil(wave.speed()*seconds/4));
            for(int i=0;i<steps&&wave.alive();i++) {
                wave.advance(seconds/steps);
                if(!wave.alive()) {
                    if(wave.heavy())events.add(new Event(EventType.TIDE_DISSIPATE,wave.x(),wave.y(),true));
                    break;
                }
                if(map.waterBlocked(wave.x(),wave.y()+Player.COLLISION_Y_OFFSET,wave.radius())) {
                    wave.stop();
                } else if(bossActive()&&guardian.alive()
                        &&map.touchesGuardian(wave.x(),wave.y()+Player.COLLISION_Y_OFFSET,wave.radius())) {
                    if(bossVulnerable()&&guardian.hurt(bossDamage(wave.damage()))) afterGuardianHit(true);
                    wave.stop();
                } else {
                    for(Wisp scout:scouts) if(scout.alive()
                            &&wave.touches(scout.x(),scout.y(),Wisp.COLLISION_RADIUS)
                            &&map.clearWaterLine(wave.x(),wave.y()+Player.COLLISION_Y_OFFSET,
                                    scout.x(),scout.y()+Wisp.COLLISION_Y_OFFSET,4)
                            &&wave.firstContact(scout)) {
                        boolean damaged=hurtScout(scout,wave.damage(),true,wave.staggers());
                        if(damaged) {
                            scout.push(wave.directionX()*wave.impulse(),wave.directionY()*wave.impulse());
                            afterScoutHit(scout,true);
                            if(wave.heavy())emit(EventType.TIDE_IMPACT,scout.x(),scout.y());
                        }
                        // Bodies part around the wave; a raised shield or solid terrain stops it.
                        if(!wave.heavy()||!damaged) {wave.stop();break;}
                    }
                }
                if(!wave.alive()) events.add(new Event(wave.heavy()?EventType.TIDE_IMPACT:EventType.WATER_IMPACT,wave.x(),wave.y(),true));
            }
        }
        projectiles.removeIf(wave->!wave.alive());
    }
    private void updateCrescents(double seconds) {
        for(Crescent arc:crescents) {
            int steps=Math.max(1,(int)Math.ceil(560*seconds/4));
            for(int i=0;i<steps&&arc.alive;i++) {
                double dt=seconds/steps;
                arc.x+=arc.dx*560*dt;arc.y+=arc.dy*560*dt;arc.age+=dt;
                if(arc.age>=.65) { arc.alive=false;break; }
                if(map.waterBlocked(arc.x,arc.y+Player.COLLISION_Y_OFFSET,arc.radius()))arc.alive=false;
                else if(bossActive()&&guardian.alive()
                        &&map.touchesGuardian(arc.x,arc.y+Player.COLLISION_Y_OFFSET,arc.radius())) {
                    if(bossVulnerable()&&guardian.hurt(bossDamage(3)))afterGuardianHit(false);
                    arc.alive=false;
                } else for(Wisp scout:scouts)if(scout.alive()
                        &&Math.hypot(scout.x()-arc.x,scout.y()-arc.y)<=Wisp.COLLISION_RADIUS+arc.radius()) {
                    if(hurtScout(scout,3,false,true)) {
                        scout.push(arc.dx*260,arc.dy*260);afterScoutHit(scout);
                    }
                    arc.alive=false;break;
                }
                if(!arc.alive)emit(EventType.CRESCENT_IMPACT,arc.x,arc.y);
            }
        }
        crescents.removeIf(arc->!arc.alive);
    }
    private void afterScoutHit(Wisp scout) {
        afterScoutHit(scout,false);
    }
    private boolean hurtScout(Wisp scout,int damage,boolean slime,boolean stagger) {
        if(debugOneShot)return scout.hurt(scout.health(),stagger);
        var enemy=campaignEnemies.get(scout);
        return enemy==null?scout.hurt(damage,stagger):enemy.receiveDamage(damage,slime,stagger);
    }
    private boolean bossVulnerable() { return debugOneShot||guardian.state()==Guardian.State.RECOVER; }
    private int bossDamage(int normal) { return debugOneShot?guardian.health():normal; }
    private void emitSpit(Wisp scout) {
        // ponytail: 24 active shots covers authored encounters; use pooling only if profiled necessary.
        if(hostileProjectiles.size()>21)return;
        double angle=Math.atan2(scout.intentY(),scout.intentX());
        for(int i=-1;i<=1;i++)hostileProjectiles.add(new EnemyProjectile(scout.x(),scout.y(),
                Math.cos(angle+i*.20),Math.sin(angle+i*.20)));
        emit(EventType.SPIT_SHOT,scout.x(),scout.y());
    }
    private void updateHostileProjectiles(double seconds,boolean dashProtected) {
        for(var shot:hostileProjectiles) {
            int steps=Math.max(1,(int)Math.ceil(EnemyProjectile.SPEED*seconds/4));
            for(int i=0;i<steps&&shot.alive();i++) {
                shot.advance(seconds/steps);
                if(!shot.alive())break;
                if(map.isBlocked(shot.x(),shot.y()+Wisp.COLLISION_Y_OFFSET,EnemyProjectile.RADIUS))shot.stop();
                else if(Math.hypot(shot.x()-player.x(),shot.y()-player.y())<=Player.COLLISION_RADIUS+EnemyProjectile.RADIUS) {
                    if(!dashProtected)hurtPlayer(1,shot.x(),shot.y());shot.stop();
                }
                if(!shot.alive())emit(EventType.SPIT_IMPACT,shot.x(),shot.y());
            }
        }
        hostileProjectiles.removeIf(shot->!shot.alive());
    }
    private void afterScoutHit(Wisp scout,boolean water) {
        events.add(new Event(EventType.ENEMY_HIT,scout.x(),scout.y(),water));
        hitStop=0.025;
        if (scout.alive()) return;
        var enemy=campaignEnemies.get(scout);
        double ichor=bossAdds.contains(scout)?15:enemy==null?SCOUT_ICHOR:enemy.ichor();
        drops.add(new IchorDrop(scout.x(),scout.y(),ichor*player.dropMultiplier()));
        if(campaignMode&&!bossAdds.contains(scout)&&enemy!=null&&enemy.kind()==CampaignEnemy.Kind.FALLEN_KNIGHT) {
            // Each authored elite rewards once, including across checkpoint respawns.
            int index=scouts.indexOf(scout);
            if(campaignUnlocks.add("elite_"+biome+"_"+index))grantShards(1);
        }
        // ponytail: keep only 16 recent remains; pool only if authored encounters outgrow this cap.
        if(corpses.size()==16)corpses.remove(0);
        corpses.add(new Corpse(scout.x(),scout.y(),!bossAdds.contains(scout),
                enemy==null?null:enemy.kind(),scout.animation().row()));
        events.add(new Event(EventType.ENEMY_DEFEATED,scout.x(),scout.y(),water));
        if (!campaignMode&&!bossAdds.contains(scout)) story.scoutDefeated();
    }
    private void collectDrops(double seconds) {
        if(!player.alive())return;
        Iterator<IchorDrop> iterator=drops.iterator();
        while(iterator.hasNext()) {
            IchorDrop drop=iterator.next();
            drop.age+=seconds;
            double dx=player.x()-drop.x,dy=player.y()-drop.y,distance=Math.hypot(dx,dy);
            boolean reachable=distance<=MAGNET_RADIUS&&player.ichor()<Player.MAX_ICHOR
                    &&map.clearWaterLine(drop.x,drop.y+Player.COLLISION_Y_OFFSET,
                            player.x(),player.y()+Player.COLLISION_Y_OFFSET,4);
            if(reachable&&distance>PICKUP_RADIUS) {
                drop.pullSpeed=Math.min(420,drop.pullSpeed+1200*seconds);
                double travel=Math.min(distance,drop.pullSpeed*seconds);
                drop.x+=dx/distance*travel;drop.y+=dy/distance*travel;
            } else drop.pullSpeed=0;
            if (reachable&&near(drop.x(),drop.y(),PICKUP_RADIUS)) {
                double collected=Math.min(drop.amount(),100-player.ichor());
                player.collectIchor(collected);
                drop.amount-=collected;
                if(drop.amount<1e-8)iterator.remove();
                else drop.pullSpeed=0;
                if(collected>=1||drop.amount<1e-8)emit(EventType.PICKUP,player.x(),player.y());
            }
        }
    }
    public boolean transform() {
        if(blocked()||!player.transform()) return false;
        cancelAbsorption();
        attackDelay=0;
        comboTime=counterWindow=0;
        if(!campaignMode&&map.room()==4) tutorialTransformed=true;
        refreshPassages();
        emit(EventType.TRANSFORM,player.x(),player.y());
        return true;
    }
    public boolean roomClear() {
        if(campaignMode)return bossDefeated;
        return scouts.stream().noneMatch(Wisp::alive) && (map.room()!=9||bossDefeated);
    }

    private void refreshPassages() {
        if(campaignMode)return;
        map.setPassagesLocked(!roomClear());
        map.setTutorialLocked(map.room()==4&&!tutorialTransformed);
    }
    public String interactionPrompt() {
        if (blocked()) return "";
        if(campaignMode) {
            if(nearCampaignHub())return "E TALK / H REST + SAVE / 1-4 UPGRADES";
            for(var landmark:campaignArea().landmarks())if(landmark.optional()&&near(landmark.x(),landmark.y(),120))return "E EXAMINE "+landmark.name();
            if(near(campaignArea().exit().x(),campaignArea().exit().y(),140))return bossDefeated?(biome==3?"E APPROACH THE RIFT":"E ENTER NEXT REGION"):"DEFEAT "+guardian.bossName();
            if(biome>0&&near(campaignArea().returnPortal().x(),campaignArea().returnPortal().y(),120))return "E RETURN TO PREVIOUS REGION";
            return absorptionTarget!=null?"ABSORBING":absorbPrompt();
        }
        if (map.room()==10&&!infirmaryUsed&&near(map.restX(),map.restY(),120)) return "E  USE FIELD DRESSING";
        if (map.room()==7&&near(map.restX(),map.restY(),120)) return "E  REST AT CAMP";
        if (map.exitReached(player.x(),player.y())) return "E  ENTER FOREST BOUNDARY";
        if(absorptionTarget!=null)return "ABSORBING";
        if(nearbyCorpse()!=null)return absorbPrompt();
        for (RuinedOutpostMap.Door door:map.doors()) if (near(door.x(),door.y(),120)) {
            if (!roomClear()) return "CLEAR THE ROOM TO UNLOCK";
            if (map.room()==4&&!tutorialTransformed&&door.destination()==5) return "Q  TRY THE BLADE IN SAFETY";
            return "WALK THROUGH / "+RuinedOutpostMap.ROOMS.get(door.destination()).name();
        }
        return "";
    }
    public boolean interact() {
        if(campaignMode&&campaignInteract())return true;
        if (blocked()) return false;
        if (!campaignMode&&map.room()==10&&!infirmaryUsed&&near(map.restX(),map.restY(),120)) {
            infirmaryUsed=true; player.heal(); return true;
        }
        if (!campaignMode&&map.room()==7&&near(map.restX(),map.restY(),120)) { player.heal(); return true; }
        if (!campaignMode&&map.exitReached(player.x(),player.y())) {
            cancelAbsorption();
            story.escaped(); emit(EventType.VICTORY,player.x(),player.y()); return true;
        }
        if(absorptionTarget!=null)return false;
        Corpse corpse=nearbyCorpse();
        if(corpse!=null) {
            absorptionTarget=corpse;absorptionTime=0;
            absorptionX=player.x();absorptionY=player.y();absorptionHealth=player.healthValue();
            corpseStartX=corpse.x();corpseStartY=corpse.y();
            emit(EventType.ABSORB_START,corpse.x(),corpse.y());return true;
        }
        if (!roomClear()) return false;
        for (RuinedOutpostMap.Door door:map.doors()) {
            if (near(door.x(),door.y(),120)) {
                if(map.room()==4&&!tutorialTransformed&&door.destination()==5) return false;
                enterRoom(door.destination(),map.room());
                return true;
            }
        }
        return false;
    }
    private boolean canAbsorb() {
        return player.alive()&&!player.bladeForm()&&!player.recovering()&&!player.dashing()&&attackDelay==0;
    }
    private String absorbPrompt() {
        Corpse corpse=nearbyCorpse();
        return corpse==null?"":"E ABSORB"+(corpse.trait()==Player.Trait.NONE?"":" / "+corpse.trait().label());
    }
    private boolean corpseReachable(Corpse corpse) {
        return near(corpse.x(),corpse.y(),ABSORB_RADIUS)
                &&map.clearWaterLine(player.x(),player.y()+Player.COLLISION_Y_OFFSET,
                        corpse.x(),corpse.y()+Player.COLLISION_Y_OFFSET,4);
    }
    private Corpse nearbyCorpse() {
        if(!canAbsorb())return null;
        Corpse nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(Corpse corpse:corpses) {
            double candidate=Math.hypot(player.x()-corpse.x(),player.y()-corpse.y());
            if(candidate<distance&&corpseReachable(corpse)
                    &&(player.ichor()<Player.MAX_ICHOR||(corpse.healing()&&player.healthValue()<player.maxHealth())
                            ||player.benefitsFrom(corpse.trait()))) {
                nearest=corpse;distance=candidate;
            }
        }
        return nearest;
    }
    private void cancelAbsorption() { absorptionTarget=null;absorptionTime=0; }
    private void updateAbsorption(double seconds) {
        for(Corpse corpse:corpses)corpse.age+=seconds;
        corpses.removeIf(corpse->corpse.age>=CORPSE_LIFETIME);
        if(absorptionTarget==null)return;
        if(!canAbsorb()||!corpses.contains(absorptionTarget)||!corpseReachable(absorptionTarget)
                ||Math.hypot(player.x()-absorptionX,player.y()-absorptionY)>0.01
                ||player.healthValue()<absorptionHealth) { cancelAbsorption();return; }
        absorptionTime+=seconds;
        double pull=Math.pow(absorptionProgress(),2);
        absorptionTarget.x=corpseStartX+(player.x()-corpseStartX)*pull;
        absorptionTarget.y=corpseStartY+(player.y()-corpseStartY)*pull;
        if(absorptionTime+1e-9<ABSORB_DURATION)return;
        player.collectIchor(5);
        double beforeHealth=player.healthValue();
        if(absorptionTarget.healing())player.heal(.5);
        if(player.healthValue()>beforeHealth)emit(EventType.HEALED,player.x(),player.y());
        if(absorptionTarget.trait()!=Player.Trait.NONE) {
            player.gainTrait(absorptionTarget.trait());
            emit(EventType.TRAIT_GAINED,player.x(),player.y());
        }
        corpses.remove(absorptionTarget);cancelAbsorption();
        emit(EventType.ABSORBED,player.x(),player.y());
    }
    public boolean blocked() { return paused||story.blocksGameplay()||(campaignMode&&(choosingEnding||campaignStory.dialogueOpen())); }
    public boolean paused() { return paused; }
    public void togglePause() { if(!story.blocksGameplay()) paused=!paused; }
    public void pause() { if(!story.blocksGameplay()) paused=true; }
    public int combo() { return combo; }
    public double tideCooldown() { return tideCooldown; }
    public double crescentCooldown() { return crescentCooldown; }
    public double riposteCooldown() { return riposteCooldown; }
    public boolean guarding() { return guardTime>0&&player.alive()&&player.bladeForm(); }
    public double guardProgress() { return guarding()?1-guardTime/RIPOSTE_DURATION:0; }
    public List<Crescent> crescents() { return Collections.unmodifiableList(crescents); }
    public double waterCharge() { return attackDelay>0&&waterCast ? 1-attackDelay/waterWindup : -1; }
    public List<WaterProjectile> projectiles() { return Collections.unmodifiableList(projectiles); }
    public List<EnemyProjectile> hostileProjectiles(){return Collections.unmodifiableList(hostileProjectiles);}
    public boolean visited(int room) { return visited[room]; }
    public boolean cleared(int room) { return cleared[room]; }
    public List<Event> drainEvents() { List<Event> copy=List.copyOf(events); events.clear(); return copy; }
    public Player player() { return player; }
    public RuinedOutpostMap map() { return map; }
    public List<Wisp> scouts() { return Collections.unmodifiableList(scouts); }
    public Guardian guardian() { return guardian; }
    public OutpostStory story() { return story; }
    public List<IchorDrop> drops() { return Collections.unmodifiableList(drops); }
    public List<Corpse> corpses() { return Collections.unmodifiableList(corpses); }
    public Corpse absorptionTarget() { return absorptionTarget; }
    public double absorptionProgress() { return Math.min(1,absorptionTime/ABSORB_DURATION); }
    public double absorptionAnchorX() { return absorptionTarget==null?player.x():corpseStartX; }
    public double absorptionAnchorY() { return absorptionTarget==null?player.y():corpseStartY; }
}
