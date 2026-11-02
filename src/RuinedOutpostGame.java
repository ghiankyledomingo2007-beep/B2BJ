import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class RuinedOutpostGame {
    public enum EventType {
        DASH, ATTACK, WATER_IMPACT, FINISHER_RELEASE, FINISHER_IMPACT, TIDE_IMPACT, TIDE_RELEASE, TIDE_DISSIPATE, ENEMY_HIT, ENEMY_DEFEATED, PLAYER_HIT, PICKUP,
        TRANSFORM, REVERT, GUARDIAN_AWAKENED, GUARDIAN_SLAM, GUARDIAN_ENRAGE, GUARDIAN_PHASE_TWO, VICTORY, ROOM_ENTERED, SPIT_SHOT, SPIT_IMPACT,
        ABSORB_START, BEELZEBUB_CAST, ABSORBED, HEALED, TRAIT_GAINED, MEMBRANE_BREAK, CRESCENT_CAST, CRESCENT_IMPACT, RIPOSTE_START, RIPOSTE_COUNTER, CHARGE_READY,
        IMP_WINDUP, IMP_CLAW, WOLF_GROWL, WOLF_BITE, WOLF_POUNCE, WOLF_LAND, MIRE_CHARGE, MIRE_ERUPT,
        DENIED, CHECKPOINT, UPGRADE, DAMAGE_DEALT, DAMAGE_TAKEN, ARMOR_BLOCK,
        INTRO_WINDUP, INTRO_SLAM, INTRO_LAND, WARDEN_KNEEL, WARDEN_FALL, GATE_RELEASE, BRIARLING_SPROUT, BRIARLING_EMERGE, WATER_JET_RELEASE, WATER_JET_IMPACT, VOID_START, VOID_RELEASE, VOID_CONTACT, VOID_RUPTURE, EMPOWERED_HIT
    }
    public record Event(EventType type, double x, double y, boolean water, int damage, boolean critical, boolean heavy) {
        public Event(EventType type,double x,double y,boolean water) { this(type,x,y,water,0,false,false); }
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
    private static final double HEALING_DELAY = 5;
    public static final double ABSORB_RADIUS = 84, ABSORB_DURATION = 0.8, CORPSE_LIFETIME = 20;
    public static final double BEELZEBUB_RADIUS = 420, BEELZEBUB_DURATION = 1.15, BEELZEBUB_COOLDOWN = 10;
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
    /** Enemies allowed to wind up or strike at the same moment. */
    public static final int MAX_ATTACKERS = 2;
    public static final double TIDE_COOLDOWN = 4.5;
    public static final double CRESCENT_COOLDOWN = 5, RIPOSTE_COOLDOWN = 6, RIPOSTE_DURATION = .3;
    public static final double BLADE_SKILL_COST = 8, RIPOSTE_RANGE = 120;
    public static final double BLADE_CAST_DURATION = .35;
    /** Blade moves beyond the four-click combo (0-3) and the charged cleave (4). */
    public static final int DASH_SLASH = 5, SPIN_CLEAVE = 6;
    /** How long the answering cut of a successful riposte plays. Its damage is dealt by the riposte itself. */
    public static final double COUNTER_DURATION = .32;
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
    private record AbsorptionPull(Corpse corpse,double x,double y) { }
    private final List<AbsorptionPull> beelzebubTargets = new ArrayList<>();
    private double beelzebubCooldown;
    private int beelzebubCalls;
    private final List<Event> events = new ArrayList<>();
    private final List<WaterProjectile> projectiles = new ArrayList<>();
    private final List<Crescent> crescents = new ArrayList<>();
    private final List<EnemyProjectile> hostileProjectiles=new ArrayList<>();
    private final List<ForestHazard> forestHazards=new ArrayList<>();
    private final java.util.Map<Wisp,Integer> forestAttackPhases=new java.util.WeakHashMap<>();
    private CampaignEnemy.Kind lastDefeatedKind;
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
    /** A Briarling about to break the ground: where, and the seconds of warning left (owner 2026-10-29). */
    public record Sprout(double x,double y,double time) { }
    static final double SPROUT_WARNING=.7;
    private final List<Sprout> sprouts=new ArrayList<>();
    private double sproutTimer;
    private int sproutThresholds;
    private double addTimer;
    private double armorNoticeTime;
    private OutpostBark bark;
    private int barkIndex;
    private double barkTime,barkDuration,barkCooldown,identificationCooldown;
    private double banterCooldown,quietTime,exploredDistance,pendingBanterTime,bossBarkCooldown;
    private OutpostBark.Trigger pendingBanter;
    private OutpostBark pendingBossBark;
    private final java.util.Map<OutpostBark,Double> banterPlayedAt=new java.util.EnumMap<>(OutpostBark.class);
    private final int[] observedBossAttacks=new int[Guardian.Attack.values().length];
    private final boolean[] alternateBossWarnings=new boolean[Guardian.Attack.values().length];
    private double hurtReactionTime,hurtReactionCooldown,healingQuietTime,healingLastHealth;
    private int hurtReactions;
    private boolean passiveHealing;
    private double attackDelay;
    private int aimX, aimY;
    private int strikeDamage;
    private java.util.function.DoubleSupplier criticalRoll = Math::random;
    void setCriticalRollForTesting(java.util.function.DoubleSupplier roll) { criticalRoll=java.util.Objects.requireNonNull(roll); }
    private double strikeRange;
    private int combo;
    private double comboTime;
    private double hitStop;
    public static final double CHARGE_THRESHOLD=.18, FULL_CHARGE=.8, CHARGE_COST=0, INPUT_BUFFER=.12;
    private boolean primaryHeld, charging, chargeReady, comboDodged, bladeLinked;
    private double primaryTime, queuedTime, bladeTime, bladeDuration, releasedCharge, dashOpener, bladeCastTime;
    private int queuedAttack,queuedX,queuedY,bladeMove;

    private double passageCooldown;
    private double tideCooldown;
    private double counterWindow, dashX, dashY, waterWindup, counterTime;
    private double crescentCooldown,riposteCooldown,guardTime;
    public static final double VOID_WINDUP=.48, VOID_ADVANCE=.18, VOID_RUPTURE_TIME=.80, VOID_DURATION=1.22;
    private double voidTime=-1, voidReadiness, voidStartX, voidStartY, voidEndX, voidEndY;
    private boolean devoured, voidEmpowered, cleaveEmpowered, finisherEarned, voidBossHit;
    private final Set<Wisp> voidHits=Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Wisp> empoweredHits=new ArrayList<>();
    private double empoweredDelay;
    private boolean empoweredBoss;

    private boolean waterCast, heavyCast;
    private WaterProjectile.Kind waterKind=WaterProjectile.Kind.CUT;
    private boolean campaignMode, saveBlocked;
    private boolean debugSession, debugOneShot;
    private int biome, shards, campaignCheckpoint;
    private java.nio.file.Path savePath;
    private CampaignSave.Progress savedProgress;
    private String campaignNotice="";
    private OutpostDialogue dialogue;
    private int dialogueIndex;
    private double wardenRevealTime,wardenReturnTime,wardenReturnWeight,gateTime;
    private double wardenFocus=1,wardenFocusFrom=1,wardenFocusTime;
    private double wardenStepTime=-1,wardenWalkTime,wardenWalkDistance,wardenStepX,wardenStepY;
    private static final double INTRO_WINDUP=.9, INTRO_CONTACT=1.0, INTRO_FLIGHT=.55, INTRO_RECOVER=1.85;
    private double wardenVictoryTime=-1,wardenCameraX,wardenCameraY;
    private boolean wardenVictorySpoken;
    private double noticeTime,sageTime;
    private void notice(String text) { campaignNotice=text;noticeTime=8; }
    private final Set<Integer> clearedBosses=new java.util.HashSet<>();
    private final Set<String> campaignUnlocks=new java.util.HashSet<>();
    private final java.util.Map<String,Integer> upgrades=new java.util.HashMap<>();
    /** What rides inside the slime, and carriables put down away from their authored spot this visit. */
    private FieldObjects.Spot carried;
    private final java.util.Map<String,CampaignWorld.Point> dropped=new java.util.HashMap<>();
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
        clearedBosses.clear();campaignUnlocks.clear();upgrades.clear();
        shards=campaignCheckpoint=0;paused=false;sageTime=0;banterPlayedAt.clear();
        debugSession=debugOneShot=false;
        story=new OutpostStory();player=new Player(640,384);
        enterBiome(0,0);events.clear();
    }

    private void enterBiome(int destination,int arrival) {
        dialogue=null;dialogueIndex=0;openingLineTime=0;
        bark=null;pendingBossBark=null;barkIndex=beelzebubCalls=0;barkTime=barkDuration=barkCooldown=identificationCooldown=0;
        banterCooldown=12;quietTime=exploredDistance=pendingBanterTime=bossBarkCooldown=0;pendingBanter=null;lastDefeatedKind=null;
        java.util.Arrays.fill(observedBossAttacks,0);
        java.util.Arrays.fill(alternateBossWarnings,false);
        hurtReactionTime=hurtReactionCooldown=healingQuietTime=healingLastHealth=0;hurtReactions=0;passiveHealing=false;
        wardenRevealTime=wardenReturnTime=wardenReturnWeight=0;
        wardenVictoryTime=-1;
        biome=destination;map=RuinedOutpostMap.campaign(biome);
        campaignCheckpoint=arrival==1||restIndex(arrival)>=0?arrival:0;
        carried=null;dropped.clear();syncFieldGates();
        scouts=new ArrayList<>();campaignEnemies.clear();forestAttackPhases.clear();
        drops.clear();corpses.clear();endAbsorption();events.clear();
        lungeHits.clear();dashHits.clear();bossAdds.clear();sprouts.clear();sproutTimer=sproutThresholds=0;projectiles.clear();crescents.clear();hostileProjectiles.clear();forestHazards.clear();
        resetKit();tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=attackDelay=comboTime=hitStop=addTimer=counterWindow=0;
        cancelBladeInput();combo=addSpawnIndex=0;guardianHitImpact=-1;passageCooldown=.35;
        armorNoticeTime=0;
        for(var spawn:campaignArea().enemies()) {
            CampaignEnemy enemy=new CampaignEnemy(spawn.kind(),spawn.x(),spawn.y());
            scouts.add(enemy.body());campaignEnemies.put(enemy.body(),enemy);
        }
        guardian=new Guardian(map.guardianX(),map.guardianY(),Guardian.Profile.values()[biome]);
        boolean giant=guardian.profile()==Guardian.Profile.BRIARHEART;           // the 3x redraw (owner 2026-10-29)
        map.setGuardianFootprint(giant?BRIAR_FOOTPRINT_WIDTH:112,giant?BRIAR_FOOTPRINT_DEPTH:52);
        bossDefeated=clearedBosses.contains(biome);
        gateTime=bossDefeated?1.4:0;
        if(bossDefeated) {
            guardian.activate(map.spawnX(),map.spawnY());guardian.hurt(guardian.maxHealth());
            map.clearGuardian();map.openGate();
        }
        var point=arrival==1?campaignArea().hub():arrival==2?campaignArea().exit():campaignArea().spawn();
        if(restIndex(arrival)>=0) {var rest=FieldObjects.rests(biome).get(restIndex(arrival));point=new CampaignWorld.Point(rest.x(),rest.y()+72);}
        player.relocate(point.x(),point.y());
        if(arrival==2)player.relocate(point.x()-160,point.y());
        if(biome==0&&(arrival==1||restIndex(arrival)>=0))player.collectIchor(Player.MAX_ICHOR);
        emit(EventType.ROOM_ENTERED,player.x(),player.y());
        if(destination==1&&arrival==0&&!dialogueSeen(OutpostDialogue.FOREST_ARRIVAL))openDialogue(OutpostDialogue.FOREST_ARRIVAL);
    }

    private void applyUpgrades() {
        player.configureUpgrades(upgradeLevel("vitality"),upgradeLevel("capacity"),
                upgradeLevel("efficiency"),upgradeLevel("edge"));
    }

    public boolean continueCampaign() {
        if(!campaignMode||savedProgress==null||story.phase()!=OutpostStory.Phase.PROLOGUE)return false;
        var progress=savedProgress;
        clearedBosses.clear();clearedBosses.addAll(progress.clearedBosses());
        campaignUnlocks.clear();campaignUnlocks.addAll(progress.unlocks());
        upgrades.clear();upgrades.putAll(progress.upgrades());
        shards=progress.shards();
        player=new Player(640,384);applyUpgrades();player.heal();
        debugSession=debugOneShot=false;
        story.resume();paused=false;
        enterBiome(progress.biome(),progress.checkpoint());
        carried=FieldObjects.find(biome,progress.carried());dropped.putAll(progress.dropped());
        notice("CAMPAIGN RESTORED");return true;
    }

    private void saveProgress() {
        // A test session never replaces either the disk save or the in-memory Continue snapshot.
        if(saveBlocked||debugSession)return;
        var progress=new CampaignSave.Progress(biome,campaignCheckpoint,clearedBosses,campaignUnlocks,upgrades,shards,
                carried==null?"":carried.id(),dropped);
        savedProgress=progress;
        if(savePath!=null)try { CampaignSave.save(savePath,progress); }
        catch(java.io.IOException failure) { campaignNotice="SAVE FAILED. PROGRESS STILL IN MEMORY.";return; }
    }

    public boolean saveCheckpoint() {
        int rest=nearCampaignHub()?-1:nearLitRest();
        if(!campaignMode||blocked()||(!nearCampaignHub()&&rest<0))return false;
        if(scouts.stream().anyMatch(w->w.alive()&&near(w.x(),w.y(),400))
                ||(bossActive()&&near(guardian.x(),guardian.y(),600))) {
            notice(rest<0?"CAMP IS NOT SAFE YET":"NOT SAFE TO REST YET");deny();return false;
        }
        campaignCheckpoint=rest<0?1:FieldObjects.REST_CHECKPOINT+rest;player.heal();emit(EventType.CHECKPOINT,player.x(),player.y());
        if(biome==0)player.collectIchor(Player.MAX_ICHOR);
        String where=rest<0?"RESTED AT CAMP":"RESTED AT THE BRAZIER";
        notice(debugSession?where+" / TEST SESSION NOT SAVED":where+" / "+(biome==0?"HP + ICHOR / ":"")+"CHECKPOINT SAVED");
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
        if(level>=(name.equals("edge")?1:3)||shards<cost) {notice("NOT ENOUGH SHARDS OR TRACK COMPLETE");deny();return false;}
        shards-=cost;upgrades.put(name,level+1);applyUpgrades();emit(EventType.UPGRADE,player.x(),player.y());
        if(name.equals("vitality"))player.heal(20);
        notice(name.toUpperCase(java.util.Locale.ROOT)+" UPGRADED");saveProgress();return true;
    }

    public boolean returnToTitle() {
        if(!campaignMode||!paused||!player.alive()||story.blocksGameplay())return false;
        saveProgress();endAbsorption();dialogue=null;wardenReturnTime=0;wardenVictoryTime=-1;story=new OutpostStory();paused=false;
        campaignNotice="";noticeTime=0;bark=null;pendingBossBark=null;barkTime=0;return true;
    }

    private boolean campaignInteract() {
        if(blocked())return false;
        if(fieldInteract())return true;
        if(biome==0&&nearCampaignHub()) {
            if(!dialogueSeen(OutpostDialogue.CAMP))openDialogue(bossDefeated?OutpostDialogue.CAMP_LATE:OutpostDialogue.CAMP);
            else if(campaignUnlocks.contains("landmark_0_9")&&!dialogueSeen(OutpostDialogue.REPORT))openDialogue(OutpostDialogue.REPORT);
            else if(rescued()==FieldObjects.SURVIVORS.length&&!dialogueSeen(OutpostDialogue.CAMP_HOME)) {
                openDialogue(OutpostDialogue.CAMP_HOME);
                if(campaignUnlocks.add("outpost_home_reward"))grantShards(4);
                saveProgress();
            }
            else if(rescued()>0&&rescued()<FieldObjects.SURVIVORS.length&&!dialogueSeen(OutpostDialogue.CAMP_RESCUED))openDialogue(OutpostDialogue.CAMP_RESCUED);
            else openDialogue(bossDefeated?OutpostDialogue.CAMP_AFTER:OutpostDialogue.CAMP_WAIT);
            return true;
        }
        if(biome==1&&nearCampaignHub()) {
            openDialogue(!dialogueSeen(OutpostDialogue.FOREST_SEDGE)
                    ?(bossDefeated?OutpostDialogue.FOREST_SEDGE_LATE:OutpostDialogue.FOREST_SEDGE)
                    :(bossDefeated?OutpostDialogue.FOREST_SEDGE_AFTER:OutpostDialogue.FOREST_SEDGE_WAIT));
            return true;
        }
        for(var landmark:campaignArea().landmarks())if(landmark.optional()&&near(landmark.x(),landmark.y(),120)) {
            int index=campaignArea().landmarks().indexOf(landmark);
            String key="landmark_"+biome+"_"+index;
            if(campaignUnlocks.add(key))grantShards(1);
            if(biome==0&&index>=9&&index<=11)openDialogue(switch(index) {
                case 9 -> OutpostDialogue.SIGNAL;
                case 10 -> OutpostDialogue.INFIRMARY;
                default -> OutpostDialogue.SUPPLIES;
            });
            else if(biome==1&&index>=9&&index<=11)openDialogue(switch(index) {
                case 9 -> OutpostDialogue.FOREST_WAYFINDER;
                case 10 -> OutpostDialogue.FOREST_MOONWELL;
                default -> OutpostDialogue.FOREST_ORCHARD;
            });
            else notice(landmark.name()+" / "+landmark.lore());
            saveProgress();return true;
        }
        if(near(campaignArea().exit().x(),campaignArea().exit().y(),140)) {
            if(!bossDefeated) {notice("DEFEAT "+guardian.bossName().toUpperCase(java.util.Locale.ROOT)+" TO OPEN THIS ROUTE");return false;}
            if(biome==0&&!dialogueSeen(OutpostDialogue.GATE)) {openDialogue(OutpostDialogue.GATE);return true;}
            if(biome==1&&!dialogueSeen(OutpostDialogue.FOREST_END)) {openDialogue(OutpostDialogue.FOREST_END);return true;}
            if(biome+1<CampaignWorld.AREA_COUNT) {
                // the open gate leads on into the next region
                endAbsorption();enterBiome(biome+1,0);notice(campaignArea().name());saveProgress();return true;
            }
            // the campaign story never passes the gatehouse phases on its own; the last exit is where it ends
            story.enterGatehouse();story.guardianDefeated();story.escaped();
            endAbsorption();notice(campaignArea().name()+" CLEARED");saveProgress();
            emit(EventType.VICTORY,player.x(),player.y());return true;
        }
        if(biome>0&&near(campaignArea().returnPortal().x(),campaignArea().returnPortal().y(),120)) {
            story.resume();enterBiome(biome-1,2);notice("RETURNED TO "+campaignArea().name());saveProgress();return true;
        }
        return false;
    }

    private double openingLineTime;
    private boolean openingHeld;           // the opening film is playing WAKE lines 0-9
    public boolean opening() { return dialogue==OutpostDialogue.WAKE; }
    public double openingLineTime() { return openingLineTime; }
    /** While the opening film plays, the drawn lines 0-9 neither time out nor advance. */
    public void holdOpening(boolean held) { openingHeld=held; }
    /** The film is over: carry on at line 10, where the slime wakes in the outpost. */
    public void finishOpeningFilm() {
        openingHeld=false;
        if(opening()&&dialogueIndex<10) {dialogueIndex=10;openingLineTime=0;}
    }
    public boolean campaignMode() { return campaignMode; }
    public OutpostDialogue dialogue() { return dialogue; }
    public int dialogueIndex() { return dialogueIndex; }
    public OutpostDialogue.Line dialogueLine() {
        if(dialogue==null)return null;
        return dialogue.line(dialogueIndex,dialogueSeen(OutpostDialogue.CAMP),rescued());
    }
    public String dialogueVoiceKey() {
        return dialogue==null?null:dialogue.voiceKey(dialogueIndex,dialogueSeen(OutpostDialogue.CAMP),rescued());
    }
    public OutpostBark bark() { return bark; }
    public OutpostDialogue.Line barkLine() { return bark==null?null:bark.line(barkIndex); }
    public String barkVoiceKey() { return bark==null?null:bark.voiceKey(barkIndex); }
    public double barkElapsed() { return bark==null?0:(barkDuration>0?barkDuration:bark.lineSeconds())-barkTime; }
    public double barkRemaining() { return bark==null?0:barkTime; }
    public void setBarkVoiceDuration(double seconds) {
        if(bark==null||paused||!Double.isFinite(seconds)||seconds<=0)return;
        double duration=Math.max(1.2,seconds+.30);
        barkTime=Math.max(0,barkTime+duration-(barkDuration>0?barkDuration:bark.lineSeconds()));
        barkDuration=duration;
    }
    public double sageTime() { return sageTime; }
    private boolean combatNearby() {
        return bossActive()||scouts.stream().anyMatch(enemy->enemy.alive()&&enemy.aggro()&&near(enemy.x(),enemy.y(),700))
                ||hostileProjectiles.stream().anyMatch(shot->shot.alive()&&near(shot.x(),shot.y(),700))
                ||forestHazards.stream().anyMatch(zone->zone.alive()&&near(zone.x(),zone.y(),700));
    }
    public boolean passiveHealing() { return passiveHealing&&player.alive()&&!blocked(); }
    public double healingDelay() {
        return !campaignMode||!player.alive()||player.healthValue()>=player.maxHealth()?0:Math.max(0,HEALING_DELAY-healingQuietTime);
    }
    private void updatePassiveHealing(double seconds) {
        if(!campaignMode)return;
        if(!player.alive()||player.healthValue()>=player.maxHealth()||player.healthValue()<healingLastHealth) {
            healingQuietTime=0;passiveHealing=false;
        } else {
            healingQuietTime+=seconds;
            if(healingQuietTime>=HEALING_DELAY) {
                player.heal(2*seconds);
                if(!passiveHealing)emit(EventType.HEALED,player.x(),player.y());
                passiveHealing=true;
            }
        }
        healingLastHealth=player.healthValue();
    }
    private void updateBark(double seconds,double travelled) {
        if(!campaignMode)return;
        boolean danger=combatNearby();
        barkCooldown=Math.max(0,barkCooldown-seconds);
        identificationCooldown=Math.max(0,identificationCooldown-seconds);
        bossBarkCooldown=Math.max(0,bossBarkCooldown-seconds);
        hurtReactionTime=Math.max(0,hurtReactionTime-seconds);
        hurtReactionCooldown=Math.max(0,hurtReactionCooldown-seconds);
        banterCooldown=Math.max(0,banterCooldown-seconds);
        pendingBanterTime=Math.max(0,pendingBanterTime-seconds);
        if(pendingBanterTime==0)pendingBanter=null;
        quietTime=danger?0:quietTime+seconds;
        if(danger)exploredDistance=0;
        else if(bark==null)exploredDistance+=travelled;
        advanceBark(seconds);
        if(bark==null&&pendingBossBark!=null) {
            var next=pendingBossBark;pendingBossBark=null;bossBark(next);
        }
        if(bossActive()&&bark==null&&bossBarkCooldown==0&&player.health()<=player.maxHealth()*.35)
            bossBark(biome==1?OutpostBark.FOREST_BOSS_CLOSE_CALL:OutpostBark.WARDEN_CLOSE_CALL);
        if(hurtReactionTime>0&&hurtReactionTime<=1.5&&hurtReactionCooldown==0&&bark==null&&player.alive()) {
            startBark(OutpostBark.hitReaction(hurtReactions++));
            hurtReactionTime=0;hurtReactionCooldown=15;
        }
        if(bossActive()&&guardian.health()<=guardian.maxHealth()*.25&&guardian.state()==Guardian.State.RECOVER
                &&bark==null&&bossBarkCooldown==0)bossBark(biome==1?OutpostBark.FOREST_BOSS_WEAK:OutpostBark.WARDEN_WEAK);
        if(bark==null&&identificationCooldown==0&&player.alive()&&dialogue==null)identifyNearbyEnemy();
        if(danger||bark!=null||barkCooldown>0)return;
        var hub=campaignArea().hub();var first=FieldObjects.find(0,"survivor_0");
        OutpostBark next=null;
        if(biome==1) {
            OutpostBark[] discoveries={OutpostBark.FOREST_CROSSING,OutpostBark.FOREST_GROVE,OutpostBark.FOREST_RIDGE,OutpostBark.FOREST_FORD};
            for(int i=0;i<discoveries.length;i++) {
                var place=campaignArea().landmarks().get(i+3);
                if(near(place.x(),place.y(),260)&&!campaignUnlocks.contains(discoveries[i].key())){next=discoveries[i];break;}
            }
        }
        else if(dialogueSeen(OutpostDialogue.CAMP)&&!near(hub.x(),hub.y(),210)&&near(hub.x(),hub.y(),430)
                &&!campaignUnlocks.contains(OutpostBark.LEAVING_CAMP.key()))next=OutpostBark.LEAVING_CAMP;
        else if(!fieldDone(first)&&near(first.x(),first.y()-600,400)
                &&!campaignUnlocks.contains(OutpostBark.HEARD_SURVIVOR.key()))next=OutpostBark.HEARD_SURVIVOR;
        else if(rescued()>0&&rescued()<FieldObjects.SURVIVORS.length&&!campaignUnlocks.contains(OutpostBark.AFTER_RESCUE.key())
                &&FieldObjects.all(0).stream().filter(s->s.kind()==FieldObjects.Kind.SURVIVOR&&fieldDone(s))
                        .noneMatch(s->near(s.x(),s.y(),220)))next=OutpostBark.AFTER_RESCUE;
        if(next==null&&banterCooldown==0&&quietTime>=3) {
            OutpostBark.Trigger trigger=pendingBanter;
            if(trigger==OutpostBark.Trigger.CAMP&&near(hub.x(),hub.y(),260))trigger=null;
            if(trigger==OutpostBark.Trigger.FIGHT&&player.health()<=player.maxHealth()*.35)
                trigger=OutpostBark.Trigger.CLOSE_CALL;
            if(trigger!=null)next=nextBanter(trigger);
            if(next==null&&quietTime>=8&&exploredDistance>=300&&player.health()>player.maxHealth()*.35)
                next=nextBanter(OutpostBark.Trigger.EXPLORING);
        }
        if(next!=null)startBark(next);
    }
    private void advanceBark(double seconds) {
        barkTime=Math.max(0,barkTime-seconds);
        if(bark!=null&&barkTime==0) {
            if(wardenVictory()&&bark!=OutpostBark.WARDEN_VICTORY) {bark=null;return;}
            if(++barkIndex<bark.lines().size())barkTime=barkDuration=bark.lineSeconds();
            else {
                if(bark.banter()) {campaignUnlocks.add(bark.key());saveProgress();}
                else if(bark.trigger()==OutpostBark.Trigger.IDENTIFY) {
                    campaignUnlocks.add(bark.key()+"_exchange");identificationCooldown=4;saveProgress();
                }
                bark=null;
            }
        }
    }
    private OutpostBark nextBanter(OutpostBark.Trigger trigger) {
        var recent=new java.util.HashSet<String>();
        banterPlayedAt.forEach((line,time)->{if(sageTime-time<180)recent.add(line.key());});
        var heard=new java.util.HashSet<>(campaignUnlocks);heard.addAll(recent);
        var next=OutpostBark.unheard(trigger,heard,biome,lastDefeatedKind);
        return next!=null?next:OutpostBark.unheard(trigger,recent,biome,lastDefeatedKind);
    }
    private void identifyNearbyEnemy() {
        OutpostBark next=null;double closest=460;
        for(var enemy:campaignEnemies.values()) {
            Wisp body=enemy.body();var description=OutpostBark.identification(enemy.kind());
            if(description==null||campaignUnlocks.contains(description.key()+"_exchange")||!body.alive())continue;
            double distance=Math.hypot(body.x()-player.x(),body.y()-player.y());
            if(distance>=closest||Math.abs(body.y()-player.y())>260
                    ||!map.clearLine(player.x(),player.y()+Player.COLLISION_Y_OFFSET,
                            body.x(),body.y()+Wisp.COLLISION_Y_OFFSET))continue;
            closest=distance;next=description;
        }
        if(next!=null) {startBark(next);identificationCooldown=8;}
    }
    private void startBark(OutpostBark next) {
        bark=next;barkIndex=0;barkTime=barkDuration=next.lineSeconds();barkCooldown=next.combat()?6:10;
        if(next.banter())banterPlayedAt.put(next,sageTime);
        else if(next.trigger()!=OutpostBark.Trigger.SKILL)campaignUnlocks.add(next.key());
        exploredDistance=0;
        if(next.trigger()==OutpostBark.Trigger.STORY)banterCooldown=Math.max(banterCooldown,18);
        else if(next.banter()) {
            banterCooldown=24+Math.random()*10;
            pendingBanter=null;pendingBanterTime=0;
        }
        if(next.trigger()==OutpostBark.Trigger.BOSS||next.trigger()==OutpostBark.Trigger.WARNING)bossBarkCooldown=15;
        if(next==OutpostBark.HEARD_SURVIVOR)campaignUnlocks.add("found_survivor_0");
        if(!next.banter()&&next.trigger()!=OutpostBark.Trigger.SKILL)saveProgress();
    }
    private void bossBark(OutpostBark next) {
        if(campaignMode&&biome==next.biome()&&player.alive()&&dialogue==null&&!campaignUnlocks.contains(next.key())) {
            if(bark==null)startBark(next);else pendingBossBark=next;
        }
    }
    public boolean bossIntroduction() { return dialogue==OutpostDialogue.WARDEN||dialogue==OutpostDialogue.FOREST_BRIARHEART; }
    public boolean cutsceneActive() { return bossIntroduction()||wardenVictory()||wardenReturnTime>0; }
    public boolean dialogueReady() { return (!bossIntroduction()||wardenRevealTime>=1.35)&&!wardenStepActive(); }
    public double storyCameraWeight() {
        if(giantFraming())return giantCamera;
        double t=bossIntroduction()?Math.min(1,wardenRevealTime/1.1):wardenVictory()?Math.min(1,wardenVictoryTime/.8):wardenReturnTime/.6;
        return t*t*(3-2*t)*(bossIntroduction()||wardenVictory()?1:wardenReturnWeight);
    }
    public double storyCameraFocus() { return wardenFocus; }
    public double storyCameraX() {
        if(giantFraming())return player.x()+(guardian.x()-player.x())*.35;
        return bossIntroduction()?player.x()+(guardian.x()-player.x())*wardenFocus:wardenCameraX;
    }
    public double storyCameraY() {
        // Lean toward its heart, but never push the player more than 170 px below the centre (onto the skill bar).
        if(giantFraming())return Math.max(guardian.y()-190,player.y()-170/giantCamera);
        double lift=guardian.profile()==Guardian.Profile.BRIARHEART?248*wardenFocus:0;   // frame the giant's crown too
        return bossIntroduction()?player.y()+(guardian.y()-player.y())*wardenFocus+88-lift:wardenCameraY;
    }
    /** The 3x Briarheart fills the screen, so while the player fights near it the camera leans up until its heart and
     *  the player both stay in view (owner 2026-10-29). It eases in and out with distance; cutscenes keep their camera,
     *  and it waits out the 3 s name card at the bottom of the screen so the player is never framed behind it. */
    private double giantCamera,giantFightTime;
    private boolean giantFraming() {
        return giantCamera>0&&!bossIntroduction()&&!wardenVictory()&&wardenReturnTime<=0;
    }
    private void updateGiantCamera(double seconds) {
        double target=0;
        if(guardian.profile()==Guardian.Profile.BRIARHEART&&bossActive()&&!bossDefeated&&guardian.alive()
                &&!bossIntroduction()&&!wardenVictory()&&wardenReturnTime<=0) {
            giantFightTime+=seconds;
            double t=Math.max(0,Math.min(1,(760-Math.hypot(player.x()-guardian.x(),player.y()-guardian.y()))/260));
            if(giantFightTime>=3)target=.5*t*t*(3-2*t);
        } else giantFightTime=0;
        giantCamera+=(target-giantCamera)*Math.min(1,seconds*2.5);
        if(giantCamera<1e-4)giantCamera=0;
    }
    public boolean wardenVictory() { return wardenVictoryTime>=0; }
    public double wardenVictorySeconds() { return Math.max(0,wardenVictoryTime); }
    private static double eased(double time) { double t=Math.max(0,Math.min(1,time));return t*t*(3-2*t); }
    static final double GATE_RELEASE_AT=4.8,LAST_WORDS_AT=1.6;
    private void updateWardenVictory(double seconds) {
        // Retire in-flight attacks after their iterators finish; the aftermath cannot hurt the player.
        projectiles.clear();crescents.clear();hostileProjectiles.clear();forestHazards.clear();
        double before=wardenVictoryTime;wardenVictoryTime+=seconds;
        guardian.update(seconds*.25,player.x(),player.y());
        advanceBark(seconds);
        // Beats shared with WardenFx's performed death: it drops to a knee (1.55), breaks apart (2.2) and the
        // camera stays on the rubble until it settles before panning to the gate (owner 2026-10-29).
        if(before<WardenFx.KNELT&&wardenVictoryTime>=WardenFx.KNELT)emit(EventType.WARDEN_KNEEL,guardian.x(),guardian.y());
        // Its last words land just after the knee: started directly, so they play however quiet the barks are set.
        if(before<LAST_WORDS_AT&&wardenVictoryTime>=LAST_WORDS_AT)startBark(OutpostBark.WARDEN_LAST);
        if(before<WardenFx.SETTLE&&wardenVictoryTime>=WardenFx.SETTLE)emit(EventType.WARDEN_FALL,guardian.x(),guardian.y());
        if(before<GATE_RELEASE_AT&&wardenVictoryTime>=GATE_RELEASE_AT)emit(EventType.GATE_RELEASE,campaignArea().exit().x(),campaignArea().exit().y());
        gateTime=Math.max(0,Math.min(1.4,wardenVictoryTime-GATE_RELEASE_AT));
        double gate=eased((wardenVictoryTime-3.4)/1.2),returning=eased((wardenVictoryTime-6.4)/.9);
        double x=guardian.x()+(campaignArea().exit().x()-guardian.x())*gate;
        double y=guardian.y()+24+(campaignArea().exit().y()-160-guardian.y()-24)*gate;
        wardenCameraX=x+(player.x()-x)*returning;
        wardenCameraY=y+(player.y()+88-y)*returning;
        if(wardenVictoryTime>=6.6&&bark==null) {
            if(!wardenVictorySpoken) {wardenVictorySpoken=true;startBark(OutpostBark.WARDEN_VICTORY);}
            else finishWardenVictory();
        }
    }
    private void finishWardenVictory() {
        projectiles.clear();crescents.clear();hostileProjectiles.clear();forestHazards.clear();
        wardenReturnWeight=storyCameraWeight();wardenReturnTime=wardenReturnWeight>0?.6:0;
        wardenVictoryTime=-1;gateTime=1.4;bark=null;barkTime=0;pendingBossBark=null;
        campaignUnlocks.add(OutpostBark.WARDEN_VICTORY.key());
        events.removeIf(e->e.type()==EventType.WARDEN_FALL||e.type()==EventType.GATE_RELEASE);
        campaignNotice="";noticeTime=0;banterCooldown=12;saveProgress();
    }
    public boolean wardenStepActive() { return dialogue==OutpostDialogue.WARDEN&&wardenStepTime>=0; }
    public boolean wardenRecoil() {
        double time=wardenStepTime-wardenWalkTime-INTRO_CONTACT;
        return wardenStepActive()&&time>=0&&time<INTRO_FLIGHT;
    }
    public double wardenPlayerLift() {
        return wardenRecoil()?28*Math.sin(Math.PI*(wardenStepTime-wardenWalkTime-INTRO_CONTACT)/INTRO_FLIGHT):0;
    }
    public Guardian.Animation wardenIntroPose() {
        double time=wardenStepTime-wardenWalkTime;
        return time<0?Guardian.Animation.IDLE:time<INTRO_WINDUP?Guardian.Animation.WINDUP
                :time<INTRO_WINDUP+Guardian.SLAM_DURATION?Guardian.Animation.STRIKE:Guardian.Animation.RECOVER;
    }
    public int wardenIntroFrame() {
        double time=wardenStepTime-wardenWalkTime;
        return Math.max(0,Math.min(7,(int)(switch(wardenIntroPose()) {
            case IDLE -> (wardenStepTime*6)%8;
            case WINDUP -> time*8/(INTRO_WINDUP*.72);
            case STRIKE -> (time-INTRO_WINDUP)*8/Guardian.SLAM_DURATION;
            default -> (time-INTRO_WINDUP-Guardian.SLAM_DURATION)*8/(INTRO_RECOVER-INTRO_WINDUP-Guardian.SLAM_DURATION);
        })));
    }
    private void updateWardenStep(double seconds) {
        // Borrow the attack poses without waking the combat AI or charging the player health.
        double before=wardenStepTime,next=before+seconds;
        double walk=Math.min(next,wardenWalkTime)-Math.min(before,wardenWalkTime);
        player.attackStep(wardenStepX,wardenStepY,walk*wardenWalkDistance/wardenWalkTime,map);
        if(before<wardenWalkTime&&next>=wardenWalkTime)
            events.add(new Event(EventType.INTRO_WINDUP,guardian.x(),guardian.y()));
        double contact=wardenWalkTime+INTRO_CONTACT,land=contact+INTRO_FLIGHT;
        if(before<contact&&next>=contact)
            events.add(new Event(EventType.INTRO_SLAM,player.x(),player.y()));
        double a=Math.max(0,Math.min(1,(before-contact)/INTRO_FLIGHT));
        double b=Math.max(0,Math.min(1,(next-contact)/INTRO_FLIGHT));
        player.attackStep(-wardenStepX,-wardenStepY,84*((2*b-b*b)-(2*a-a*a)),map);
        if(before<land&&next>=land)events.add(new Event(EventType.INTRO_LAND,player.x(),player.y()));
        wardenStepTime=next;
        if(next>=wardenWalkTime+INTRO_RECOVER) {
            wardenStepTime=-1;wardenFocusFrom=wardenFocus;wardenFocusTime=0;
        }
    }
    public double gateOpening() { return map.gateOpen()?Math.min(1,gateTime/1.4):0; }
    public boolean dialogueSeen(OutpostDialogue scene) { return campaignUnlocks.contains(scene.key()); }

    /** A road suggestion, never a required visit or kill gate; northern exploration stays valid. */
    int outpostWaypoint() {
        if(biome!=0||guardian.state()!=Guardian.State.DORMANT)return -1;
        if(!dialogueSeen(OutpostDialogue.CAMP)&&!dialogueSeen(OutpostDialogue.WARDEN))return 1;
        var places=campaignArea().landmarks();
        int nearest=0;double distance=Double.POSITIVE_INFINITY;
        for(int i=0;i<places.size();i++) {
            var place=places.get(i);
            double candidate=Math.hypot(player.x()-place.x(),player.y()-place.y());
            if(candidate<distance){distance=candidate;nearest=i;}
        }
        return switch(nearest) {
            case 0,1,9 -> 2;
            case 2 -> player.x()<places.get(2).x()-160?2:6;
            case 3,4,5,10 -> 6;
            case 6 -> player.x()<places.get(6).x()-160?6:7;
            default -> 8;
        };
    }

    public CampaignWorld.Point campaignObjectivePoint() {
        if(campaignMode&&biome==1&&!dialogueSeen(OutpostDialogue.FOREST_SEDGE)&&guardian.alive())return campaignArea().hub();
        int waypoint=outpostWaypoint();
        if(waypoint>=0) {
            var place=campaignArea().landmarks().get(waypoint);
            return new CampaignWorld.Point(place.x(),place.y());
        }
        return guardian.alive()?campaignArea().boss():campaignArea().exit();
    }

    public String campaignObjective() {
        if(campaignMode&&biome==1&&!dialogueSeen(OutpostDialogue.FOREST_SEDGE)&&guardian.alive())return "Find Souei at Ferry Refuge";
        switch(outpostWaypoint()) {
            case 1: return "Find Shuna at Captain Camp";
            case 2: return "Follow banners to Muster Ground";
            case 6: return "Follow the road to Standard Yard";
            case 7: return "Reach Inner Court";
            case 8: return "Reach the East Gatehouse";
            default: break;
        }
        return guardian.alive()?"Defeat the "+guardian.bossName()
                :biome==0?"Cross the Eastern Gate":"Reach the survivors beneath the stair";
    }

    private void openDialogue(OutpostDialogue scene) {
        endAbsorption();cancelBladeInput();
        bark=null;pendingBossBark=null;barkTime=0;
        hurtReactionTime=0;
        pendingBanter=null;pendingBanterTime=quietTime=exploredDistance=0;
        banterCooldown=12;
        dialogue=scene;dialogueIndex=0;openingLineTime=0;openingHeld=false;
        if(bossIntroduction()) {
            wardenRevealTime=0;
            wardenStepTime=-1;
            wardenFocus=wardenFocusFrom=1;wardenFocusTime=-1.35;
            // Arrival movement and queued casts must not resume after the camera returns.
            player.relocate(player.x(),player.y());attackDelay=waterWindup=0;waterCast=false;
        }
    }

    public boolean advanceDialogue() {
        if(dialogue==null||paused||!dialogueReady()||openingHeld&&opening()&&dialogueIndex<10)return false;
        if(bossIntroduction()) {wardenFocusFrom=wardenFocus;wardenFocusTime=0;}
        if(dialogue==OutpostDialogue.WARDEN&&dialogueIndex==1) {
            double dx=guardian.x()-player.x(),dy=guardian.y()-player.y(),distance=Math.hypot(dx,dy);
            wardenStepX=distance>0?dx/distance:0;wardenStepY=distance>0?dy/distance:-1;
            wardenWalkDistance=Math.min(420,Math.max(0,distance-112));
            wardenWalkTime=Math.max(.75,wardenWalkDistance/145);
            wardenStepTime=0;
        }
        openingLineTime=0;
        if(++dialogueIndex>=dialogue.lines().size())skipDialogue();
        return true;
    }

    public boolean skipDialogue() {
        if(paused)return false;
        if(wardenVictory()) {finishWardenVictory();return true;}
        if(dialogue==null)return false;
        if(bossIntroduction()) {
            wardenCameraX=storyCameraX();wardenCameraY=storyCameraY();
            wardenReturnWeight=storyCameraWeight();wardenReturnTime=wardenReturnWeight>0?.6:0;
            wardenStepTime=-1;
            events.removeIf(e->e.type()==EventType.INTRO_WINDUP||e.type()==EventType.INTRO_SLAM||e.type()==EventType.INTRO_LAND);
        }
        campaignUnlocks.add(dialogue.key());
        if(dialogue==OutpostDialogue.CAMP_LATE)campaignUnlocks.add(OutpostDialogue.CAMP.key());
        if(dialogue==OutpostDialogue.FOREST_SEDGE_LATE)campaignUnlocks.add(OutpostDialogue.FOREST_SEDGE.key());
        if(dialogue.lines().stream().anyMatch(line->line.speaker()==OutpostDialogue.Speaker.MARA||line.speaker()==OutpostDialogue.Speaker.SEDGE)) {
            pendingBanter=OutpostBark.Trigger.CAMP;pendingBanterTime=60;
        }
        dialogue=null;dialogueIndex=0;openingLineTime=0;
        saveProgress();return true;
    }
    public int biome() { return biome; }
    public CampaignWorld.Area campaignArea() { return CampaignWorld.area(biome); }
    public CampaignEnemy campaignEnemy(Wisp body) { return campaignEnemies.get(body); }
    public boolean bossActive() { return campaignMode?!bossDefeated&&guardian.state()!=Guardian.State.DORMANT:map.room()==9; }
    public int shards() { return shards; }
    public int upgradeLevel(String key) { return upgrades.getOrDefault(key,0); }
    public String campaignNotice() { return campaignNotice; }
    public boolean saveAvailable() { return savedProgress!=null; }
    public boolean saveNeedsConfirmation() { return savedProgress!=null||saveBlocked; }
    public String savedAreaName() { return savedProgress==null?"":CampaignWorld.area(savedProgress.biome()).name(); }
    public int campaignCheckpoint() { return campaignCheckpoint; }
    public boolean biomeUnlocked(int id) { return id>=0&&id<CampaignWorld.AREA_COUNT&&id==0; }

    private void grantShards(int amount) { shards=(int)Math.min(100_000L,(long)shards+amount); }

    public boolean debugAvailable() {
        return campaignMode&&player.alive()&&!story.blocksGameplay()
                &&story.phase()!=OutpostStory.Phase.COMPLETE;
    }
    public boolean debugSession() { return debugSession; }
    public boolean debugGodMode() { return player.godMode(); }
    public boolean debugUnlimitedBlade() { return player.unlimitedBlade(); }
    public boolean debugOneShot() { return debugOneShot; }
    public boolean setDebugGodMode(boolean enabled) {
        if(!debugAvailable())return false;
        if(player.godMode()!=enabled)debugSession=true;
        player.setGodMode(enabled);return true;
    }
    public boolean setDebugUnlimitedBlade(boolean enabled) {
        if(!debugAvailable())return false;
        if(player.unlimitedBlade()!=enabled)debugSession=true;
        player.setUnlimitedBlade(enabled);return true;
    }
    public boolean setDebugOneShot(boolean enabled) {
        if(!debugAvailable())return false;
        if(debugOneShot!=enabled)debugSession=true;
        debugOneShot=enabled;return true;
    }
    public boolean debugRefill() {
        if(!debugAvailable())return false;
        debugSession=true;player.refillForTesting();endAbsorption();
        resetKit();tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=attackDelay=comboTime=counterWindow=hitStop=0;
        cancelBladeInput();combo=0;dashHits.clear();
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
            if(story.phase()!=OutpostStory.Phase.PROLOGUE)return;
            // Existing saves are not replaced by a casual title-screen click.
            if(savedProgress!=null||saveBlocked) {
                notice("C CONTINUE / N CONFIRM NEW GAME (REPLACES SAVE)");return;
            }
        }
        story.begin();if(campaignMode) {openDialogue(OutpostDialogue.WAKE);saveProgress();}
    }
    public void newCampaign() {
        if(!campaignMode||story.phase()!=OutpostStory.Phase.PROLOGUE)return;
        resetCampaign();saveBlocked=false;story.begin();openDialogue(OutpostDialogue.WAKE);saveProgress();
    }
    public void restart() {
        if(campaignMode) {
            if(story.phase()==OutpostStory.Phase.DEAD) {
                var held=carried;var positions=java.util.Map.copyOf(dropped);
                boolean god=player.godMode(),unlimited=player.unlimitedBlade();
                player=new Player(640,384);applyUpgrades();player.heal();story.resume();paused=false;
                player.setGodMode(god);
                player.setUnlimitedBlade(unlimited);
                enterBiome(biome,campaignCheckpoint);carried=held;dropped.putAll(positions);
                notice("REFORMED AT YOUR CHECKPOINT");
            } else if(story.phase()==OutpostStory.Phase.COMPLETE) {
                story=new OutpostStory();paused=false;
            }
            return;
        }
        if (story.phase() == OutpostStory.Phase.DEAD && checkpoint == 7) {
            player = new Player(640,384);
            resetKit();tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=0;
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
        resetKit();tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=0;
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
        endAbsorption();
        events.clear();
        lungeHits.clear();
        dashHits.clear();
        bossAdds.clear();
        projectiles.clear();
        crescents.clear();guardTime=0;
        hostileProjectiles.clear();forestHazards.clear();
        attackDelay = comboTime = hitStop = addTimer = counterWindow = 0;
        cancelBladeInput();combo = 0;
        guardianHitImpact=-1;
        addSpawnIndex=0;
        guardian = new Guardian(map.guardianX(),map.guardianY());map.setGuardianFootprint(112,52);
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
                scouts.add(new Wisp(sx,sy,sx-56,sx+56,spitter?72:guard?80:64,
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
        if (!Double.isFinite(seconds) || seconds <= 0 || paused||story.blocksGameplay()) return;
        double elapsed=Math.min(seconds,.5);
        if(campaignMode)sageTime+=elapsed;
        if(opening()) {
            if(openingHeld&&dialogueIndex<10)return;
            openingLineTime+=elapsed;
            if(openingLineTime>=OutpostDialogue.openingSeconds(dialogueIndex))advanceDialogue();
            return;
        }
        if(wardenVictory()) {updateWardenVictory(elapsed);return;}
        if(map.gateOpen()&&(gateTime>0||!campaignMode||biome!=0
                ||near(campaignArea().exit().x(),campaignArea().exit().y(),320)))gateTime=Math.min(1.4,gateTime+elapsed);
        if(cutsceneActive()) {
            if(bossIntroduction()) {
                wardenRevealTime+=elapsed;wardenFocusTime+=elapsed;
                double target=wardenStepActive()?.5:switch(dialogueLine().speaker()) {
                    case RAINORAY -> .12;
                    case INNER_VOICE -> .9;
                    default -> 1;
                };
                double t=Math.max(0,Math.min(1,wardenFocusTime/.85));
                wardenFocus=wardenFocusFrom+(target-wardenFocusFrom)*t*t*(3-2*t);
                if(wardenStepActive())updateWardenStep(elapsed);
            }
            else wardenReturnTime=Math.max(0,wardenReturnTime-elapsed);
            if(guardian.state()==Guardian.State.DORMANT)guardian.update(elapsed,player.x(),player.y());
            return;
        }
        if(blocked())return;
        if(carried!=null&&player.bladeForm())dropCarried();
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
        updateGiantCamera(seconds);
        if(campaignMode&&noticeTime>0&&!campaignNotice.startsWith("SAVE FAILED")) {
            noticeTime=Math.max(0,noticeTime-seconds);
            if(noticeTime==0)campaignNotice="";
        }
        if (!player.alive()) { resetKit();projectiles.clear(); crescents.clear(); guardTime=0; hostileProjectiles.clear();forestHazards.clear(); attackDelay=0; cancelBladeInput(); endAbsorption(); story.playerDied(); return; }
        if (hitStop > 0) { hitStop=Math.max(0,hitStop-seconds); return; }
        passageCooldown=Math.max(0,passageCooldown-seconds);
        tideCooldown=Math.max(0,tideCooldown-seconds);denyTime=Math.max(0,denyTime-seconds);
        armorNoticeTime=Math.max(0,armorNoticeTime-seconds);
        if(campaignMode&&biome==0) {
            boolean found=false;
            for(var spot:FieldObjects.all(0))if(spot.kind()==FieldObjects.Kind.SURVIVOR&&!fieldDone(spot)
                    &&near(spot.x(),spot.y(),520))found|=campaignUnlocks.add("found_"+spot.id());
            if(found)saveProgress();
        }
        crescentCooldown=Math.max(0,crescentCooldown-seconds);
        riposteCooldown=Math.max(0,riposteCooldown-seconds);
        beelzebubCooldown=Math.max(0,beelzebubCooldown-seconds);
        guardTime=Math.max(0,guardTime-seconds);
        counterWindow=Math.max(0,counterWindow-seconds);
        boolean wasBlade=player.bladeForm(), wasDashing=player.dashing(), wasRecovering=player.recovering();
        // These full-body poses plant the feet. Strikes retain their authored, collision-safe step below.
        double slimeElapsed=slimeAttackElapsed();
        boolean finisher=slimeElapsed>=0&&waterKind==WaterProjectile.Kind.FINISHER;
        boolean planted=player.bladeForm()&&(voidActive()||bladePreparing()||bladeTime>0||bladeCastTime>0||guarding()||counterTime>0)
                ||finisher&&slimeElapsed<.34;
        counterTime=player.bladeForm()?Math.max(0,counterTime-seconds):0;
        double previousX=player.x(),previousY=player.y();
        player.move(horizontal,vertical,seconds,map,planted?0:slimePreparing()?.45:1);
        // The third lash commits forward through the release; collision still owns the step.
        if(finisher&&slimeElapsed<.30) {
            double length=Math.max(1,Math.hypot(aimX,aimY));
            double travel=24*Math.max(0,Math.min(slimeElapsed+seconds,.30)-Math.max(slimeElapsed,.16))/.14;
            player.attackStep(aimX/length,aimY/length,travel,map);
        }
        bladeCastTime=Math.max(0,bladeCastTime-seconds);
        if(wasBlade&&!player.bladeForm())cancelBladeInput();
        updatePrimaryInput(seconds);
        updateVoidSever(seconds);
        updateEmpoweredHits(seconds);
        if(!player.bladeForm())guardTime=0;
        if (wasBlade && !player.bladeForm()) emit(EventType.REVERT,player.x(),player.y());
        comboTime=Math.max(0,comboTime-seconds);
        dashOpener=Math.max(0,dashOpener-seconds);
        if(bladeTime>0) {
            double elapsed=bladeDuration-bladeTime;
            if(elapsed<bladeContact()&&!player.dashing()) {
                double length=Math.max(1,Math.hypot(aimX,aimY));
                player.attackStep(aimX/length,aimY/length,strikeStep(bladeMove)*Math.min(seconds,bladeContact()-elapsed)/bladeContact(),map);
            }
            bladeTime=Math.max(0,bladeTime-seconds);
        }
        if (attackDelay > 0) {
            attackDelay=Math.max(0,attackDelay-seconds);
            if (attackDelay <= 1e-9) {attackDelay=0;resolveStrike();}
        }
        if(wardenVictory())return;
        if(campaignMode&&!bossDefeated&&guardian.state()==Guardian.State.DORMANT
                &&near(guardian.x(),guardian.y(),600)) {
            var introduction=biome==1?OutpostDialogue.FOREST_BRIARHEART:OutpostDialogue.WARDEN;
            if(!dialogueSeen(introduction)) {
                if(near(guardian.x(),guardian.y(),520)&&map.clearLine(player.x(),player.y(),guardian.x(),guardian.y())) {
                    openDialogue(introduction);return;
                }
            } else {
                map.setGuardianPresent(true);guardian.activate(player.x(),player.y());
                emit(EventType.GUARDIAN_AWAKENED,guardian.x(),guardian.y());
                bossBark(biome==1?OutpostBark.FOREST_BOSS_START:OutpostBark.WARDEN_START);
            }
        }
        // Forest attacks take turns so body cues remain readable without overlapping ground markers.
        int attackLimit=biome==1?1:MAX_ATTACKERS;
        int attackers=0;
        for(Wisp scout:scouts)if(scout.alive()&&scout.attacking())attackers++;
        for (Wisp scout : scouts) {
            boolean was=scout.attacking();
            scout.waitForTurn(campaignMode&&!was&&attackers>=attackLimit);
            CampaignEnemy enemy=campaignEnemies.get(scout);
            if(enemy==null)scout.update(seconds,player.x(),player.y(),map);
            else enemy.update(seconds,player.x(),player.y(),map);
            if(!was&&scout.attacking())attackers++;
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
        if(wardenVictory())return;
        updateCrescents(seconds);
        if(wardenVictory())return;
        if (wasDashing || player.dashing()) damageEnemiesFromDash();
        if(wardenVictory())return;
        // Resolve outgoing impacts before incoming contact/release: interrupted attacks cannot trade.
        boolean dashProtected=wasDashing&&!player.recovering()||voidAdvancing();
        for(Wisp scout:scouts) {
            updateForestAttackCue(scout);
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
                    if(!dashProtected)hurtPlayer(campaignEnemies.containsKey(scout)?campaignEnemies.get(scout).damage():20,scout.x(),scout.y());
                }
            }
        }
        if(campaignMode)for(CampaignEnemy enemy:campaignEnemies.values()) {
            var shots=enemy.releaseShots();
            if(hostileProjectiles.size()+shots.size()<=48)hostileProjectiles.addAll(shots);
            if(!shots.isEmpty())emit(EventType.SPIT_SHOT,enemy.body().x(),enemy.body().y());
            var blooms=enemy.releaseHazards();
            if(forestHazards.size()+blooms.size()<=24) {
                forestHazards.addAll(blooms);
                for(var bloom:blooms)emit(EventType.MIRE_ERUPT,bloom.x(),bloom.y());
            }
        }
        updateBark(seconds,Math.hypot(player.x()-previousX,player.y()-previousY));
        if(bossActive()&&guardian.alive()&&guardianHitImpact!=guardian.impactNumber()
                &&guardian.hits(player.x(),player.y())) {
            guardianHitImpact=guardian.impactNumber();
            if(!dashProtected) {
                boolean parryable=guardian.profile()!=Guardian.Profile.BRIARHEART
                        &&(guardian.attack()==Guardian.Attack.TARGET||guardian.attack()==Guardian.Attack.SWEEP);
                if(parryable&&guarding()&&!player.invulnerable())guardian.parried();
                hurtPlayer(guardian.profile()==Guardian.Profile.BRIARHEART?22:Guardian.SLAM_DAMAGE,guardian.targetX(),guardian.targetY(),parryable);
            }
        }
        updateHostileProjectiles(seconds,dashProtected);
        for(var hazard:forestHazards) {
            hazard.update(seconds);
            if(!dashProtected&&hazard.hit(player.x(),player.y()))hurtPlayer(9,hazard.x(),hazard.y(),false);
        }
        forestHazards.removeIf(hazard->!hazard.alive());
        if(campaignMode&&bossActive()&&player.bladeForm()&&near(guardian.x(),guardian.y(),guardian.auraRadius())) {
            player.spendBladeIchor(Math.min(player.ichor(),guardian.drainPerSecond()*seconds));
            if(!player.bladeForm()){cancelBladeInput();emit(EventType.REVERT,player.x(),player.y());}
        }
        collectDrops(seconds);
        if(player.alive()&&!wasBlade&&!wasRecovering&&!player.bladeForm()&&!player.recovering())
            player.collectIchor(PASSIVE_ICHOR_PER_SECOND*seconds);
        updateAbsorption(seconds);
        updatePassiveHealing(seconds);
        if (!player.alive()) { resetKit();projectiles.clear(); crescents.clear(); guardTime=0; hostileProjectiles.clear();forestHazards.clear(); attackDelay=0; cancelBladeInput(); endAbsorption(); story.playerDied(); }
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
        var stateBefore=guardian.state();
        guardian.update(seconds,player.x(),player.y(),map);
        if (!guardian.alive()) return;
        if(campaignMode&&near(guardian.x(),guardian.y(),1000)) {
            var attack=guardian.attack();int index=biome==1?guardian.forestAttack().ordinal():attack.ordinal();
            String learned=biome==1?"sage_briar_"+guardian.forestAttack().name().toLowerCase(java.util.Locale.ROOT)
                    :"sage_warden_"+attack.name().toLowerCase(java.util.Locale.ROOT);
            var read=biome==1?OutpostBark.FOREST_BOSS_READ:OutpostBark.WARDEN_READ;
            if(guardian.impactNumber()!=impactBefore&&observedBossAttacks[index]<2
                    &&++observedBossAttacks[index]==2&&campaignUnlocks.add(learned))saveProgress();
            if(biome==1&&guardian.coreExposed()&&bark==null)bossBark(OutpostBark.FOREST_BOSS_OPEN);
            if(campaignUnlocks.contains(learned)&&guardian.state()==Guardian.State.RECOVER
                    &&bark==null&&bossBarkCooldown==0)bossBark(read);
            if(stateBefore!=Guardian.State.TELEGRAPH&&guardian.state()==Guardian.State.TELEGRAPH
                    &&campaignUnlocks.contains(learned)&&campaignUnlocks.contains(read.key())
                    &&bark==null&&bossBarkCooldown==0) {
                boolean alternate=alternateBossWarnings[index];
                startBark(biome==1?switch(guardian.forestAttack()) {
                    case ROOT_WEAVE -> alternate?OutpostBark.FOREST_ROOT_AGAIN:OutpostBark.FOREST_ROOT_WARNING;
                    case SEED_BLOOM -> alternate?OutpostBark.FOREST_SEED_AGAIN:OutpostBark.FOREST_SEED_WARNING;
                    case BRAMBLE_CAGE -> alternate?OutpostBark.FOREST_CAGE_AGAIN:OutpostBark.FOREST_CAGE_WARNING;
                }:switch(attack) {
                    case TARGET -> alternate?OutpostBark.SAGE_TARGET_AGAIN:OutpostBark.SAGE_TARGET;
                    case SWEEP -> alternate?OutpostBark.SAGE_SWEEP_AGAIN:OutpostBark.SAGE_SWEEP;
                    case CHARGE -> alternate?OutpostBark.SAGE_CHARGE_AGAIN:OutpostBark.SAGE_CHARGE;
                    case SHOCKWAVE -> alternate?OutpostBark.SAGE_SHOCKWAVE_AGAIN:OutpostBark.SAGE_SHOCKWAVE;
                    case FISSURE -> alternate?OutpostBark.SAGE_FISSURE_AGAIN:OutpostBark.SAGE_FISSURE;
                });
                alternateBossWarnings[index]=!alternate;
            }
        }
        if(stateBefore==Guardian.State.ENRAGE&&guardian.enraged())
            emit(EventType.GUARDIAN_PHASE_TWO,guardian.x(),guardian.y());
        if(guardian.state()==Guardian.State.ENRAGE) {
            if(stateBefore!=Guardian.State.ENRAGE) {
                emit(EventType.GUARDIAN_ENRAGE,guardian.x(),guardian.y());
                bossBark(biome==1?OutpostBark.FOREST_BOSS_PHASE:OutpostBark.WARDEN_PHASE);
            }
            addTimer=0;return;
        }
        if (guardian.impactNumber()!=impactBefore) {
            if(biome==0&&campaignMode&&bark==null&&bossBarkCooldown==0&&near(guardian.x(),guardian.y(),1000)
                    &&campaignUnlocks.contains(OutpostBark.WARDEN_READ.key()))
                bossBark(guardian.health()<guardian.maxHealth()*.45?OutpostBark.WARDEN_VOICE_ORDER:switch(guardian.attack()) {
                    case CHARGE -> OutpostBark.WARDEN_VOICE_STAND;
                    case SWEEP -> OutpostBark.WARDEN_VOICE_LINE;
                    default -> OutpostBark.WARDEN_VOICE_GATE;
                });
            emit(EventType.GUARDIAN_SLAM,guardian.targetX(),guardian.targetY());
            for(var seed:guardian.forestSeeds())forestHazards.add(new ForestHazard(seed.x(),seed.y(),seed.radius(),3.6));
        }
        if(guardian.profile()==Guardian.Profile.BRIARHEART) {updateBriarlings(seconds);return;}
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
                    var enemy=new CampaignEnemy(biome==0?CampaignEnemy.Kind.OUTPOST_GUARD:CampaignEnemy.Kind.RIFT_IMP,x,y);
                    add=enemy.body();campaignEnemies.put(add,enemy);
                } else add=new Wisp(x,y,x-60,x+60,64);
                scouts.add(add);bossAdds.add(add);addTimer=0;break;
            }
        }
    }
    static final double BRIAR_FOOTPRINT_WIDTH=260,BRIAR_FOOTPRINT_DEPTH=84;
    /** Where a strike meets the boss: Briarheart's trunk can be struck anywhere along its width; the Warden at its centre. */
    private double bossNearX() {
        double half=guardian.profile()==Guardian.Profile.BRIARHEART?map.guardianHalfWidth():0;
        return Math.max(guardian.x()-half,Math.min(guardian.x()+half,player.x()));
    }
    /** Briarheart raises Briarlings: one on a timer and a pair at each quarter of its health, never more than two
     *  (three once enraged) awake or sprouting. Each gives a short flat warning before it breaks the ground. */
    private void updateBriarlings(double seconds) {
        if(!campaignMode||!guardian.alive())return;
        for(var it=sprouts.listIterator();it.hasNext();) {
            var sprout=it.next();
            if(sprout.time()>seconds) {it.set(new Sprout(sprout.x(),sprout.y(),sprout.time()-seconds));continue;}
            it.remove();
            var enemy=new CampaignEnemy(CampaignEnemy.Kind.BRIARLING,sprout.x(),sprout.y());
            var add=enemy.body();campaignEnemies.put(add,enemy);scouts.add(add);bossAdds.add(add);
            emit(EventType.BRIARLING_EMERGE,sprout.x(),sprout.y());
        }
        long awake=bossAdds.stream().filter(Wisp::alive).count()+sprouts.size();
        double health=guardian.health()/(double)guardian.maxHealth();
        int crossed=health<=.25?3:health<=.5?2:health<=.75?1:0,wanted=0;
        if(crossed>sproutThresholds) {sproutThresholds=crossed;wanted=2;}
        sproutTimer+=seconds;
        if(sproutTimer>=(guardian.enraged()?7:9)) {sproutTimer=0;wanted=Math.max(wanted,1);}
        for(int i=0;i<wanted&&awake<(guardian.enraged()?3:2);i++)if(sprout())awake++;
    }
    /** A clear patch on the arena side of its roots, away from the player and anything else standing there. */
    private boolean sprout() {
        for(int attempt=0;attempt<10;attempt++) {
            int turn=addSpawnIndex++;
            double angle=Math.PI*(.18+.64*((turn*37)%100)/100.0),reach=300+(turn*53)%90;
            double x=guardian.x()+Math.cos(angle)*reach*(turn%2==0?1:-1),y=guardian.y()+Math.sin(angle)*reach*.7+40;
            if(near(x,y,140)||map.isBlocked(x,y+Wisp.COLLISION_Y_OFFSET,Wisp.COLLISION_RADIUS)
                    ||sprouts.stream().anyMatch(s->Math.hypot(s.x()-x,s.y()-y)<90)
                    ||scouts.stream().anyMatch(w->w.alive()&&Math.hypot(x-w.x(),y-w.y())<90))continue;
            sprouts.add(new Sprout(x,y,SPROUT_WARNING));emit(EventType.BRIARLING_SPROUT,x,y);return true;
        }
        return false;
    }
    public List<Sprout> sprouts() { return Collections.unmodifiableList(sprouts); }
    private boolean near(double x,double y,double radius) {
        return Math.hypot(player.x()-x,player.y()-y)<=radius;
    }
    private void emit(EventType type,double x,double y) { events.add(new Event(type,x,y)); }
    private void hurtPlayer(int damage,double x,double y) { hurtPlayer(damage,x,y,true); }
    private void hurtPlayer(int damage,double x,double y,boolean parryable) {
        if(voidAdvancing())return;
        if(damage>0&&parryable&&guarding()&&!player.invulnerable()) {
            earnVoid(.5);hitStop=Math.max(hitStop,.04);
            guardTime=0;
            // He answers toward whoever hit him. The cut is only shown; the damage below is the riposte's own.
            aimX=(int)Math.round((x-player.x())*1000);aimY=(int)Math.round((y-player.y())*1000);
            if(aimX==0&&aimY==0)aimY=1;
            counterTime=COUNTER_DURATION;player.attackRecovery(COUNTER_DURATION*.6);
            emit(EventType.RIPOSTE_COUNTER,player.x(),player.y());
            for(Wisp scout:scouts)if(scout.alive()&&near(scout.x(),scout.y(),RIPOSTE_RANGE)
                    &&map.clearLine(player.x(),player.y(),scout.x(),scout.y())&&hurtScout(scout,36,false,true)) {
                double dx=scout.x()-player.x(),dy=scout.y()-player.y(),length=Math.max(1,Math.hypot(dx,dy));
                scout.push(dx/length*160,dy/length*160);scout.openForPunish();afterScoutHit(scout);
            }
            if(bossActive()&&!bossDefeated
                    &&near(bossNearX(),guardian.y(),RIPOSTE_RANGE)
                    &&map.clearLine(player.x(),player.y(),bossNearX(),guardian.y())&&hurtGuardian(36,false))
                afterGuardianHit(false);
            return;
        }
        boolean blade=player.bladeForm();
        boolean membrane=player.trait()==Player.Trait.MEMBRANE;
        int before=player.health();
        if (player.hurt(damage)) {
            healingQuietTime=0;passiveHealing=false;
            if(campaignMode&&player.alive()&&hurtReactionCooldown==0)hurtReactionTime=2.5;
            events.add(new Event(EventType.DAMAGE_TAKEN,player.x(),player.y(),false,before-player.health(),false,false));
            if(membrane)emit(EventType.MEMBRANE_BREAK,player.x(),player.y());
            cancelAbsorption();cancelBladeInput();guardTime=0;
            if(!player.bladeForm()) {waterCast=false;attackDelay=comboTime=0;}
            emit(EventType.PLAYER_HIT,x,y);
            hitStop=0.1;                         // getting hurt lands hardest: six frames of stillness
            if (blade&&!player.bladeForm()) emit(EventType.REVERT,player.x(),player.y());
        }
    }
    public boolean dash(int horizontal,int vertical) {
        if(voidActive())return false;
        if (blocked() || (player.bladeForm()&&attackDelay>0) || !player.dash(horizontal,vertical)) return false;
        boolean preserve=player.bladeForm()&&comboTime>0&&combo<3&&!comboDodged;
        clearBladeButtons();bladeTime=bladeCastTime=counterTime=0;
        if(player.bladeForm())player.attackRecovery(0);
        cancelAbsorption();
        guardTime=0;
        attackDelay=0;
        waterCast=false;
        dashHits.clear();
        comboTime=preserve?player.dashSeconds()+.5:0;
        comboDodged=preserve;
        dashOpener=player.bladeForm()&&!preserve?player.dashSeconds()+.3:0;
        double length=Math.hypot(horizontal,vertical);
        dashX=horizontal/length;dashY=vertical/length;
        counterWindow=player.bladeForm()?0:player.dashSeconds()+.3;
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
        if(voidActive())return false;
        if (blocked() || guarding() || charging || !player.alive() || player.recovering()
                || player.dashing() || (facingX==0&&facingY==0)) return false;
        boolean linked=bladeComboActive()&&attackDelay==0&&bladeElapsed()+1e-9>=strikeLinkTime(bladeMove);
        if(linked)player.attackRecovery(0);
        if(!player.startAttack())return false;
        cancelAbsorption();counterTime=0;
        // A click straight out of a dash opens with the dash slash; the chain then carries on from move 2.
        boolean opener=player.bladeForm()&&dashOpener>0&&comboTime==0;
        combo=comboTime>0?(combo+1)%(player.bladeForm()?player.bladeComboLength():3):0;
        if(opener)combo=1;
        if(player.bladeForm()&&combo==0)comboDodged=false;
        comboTime=0.9;
        aimX=facingX; aimY=facingY;
        strikeDamage=player.attackDamage();
        if(player.bladeForm()&&combo==3)strikeDamage+=12+player.bladeFinisherBonus();
        strikeRange=BLADE_ATTACK_RANGE;
        waterCast=!player.bladeForm(); heavyCast=false;
        if(waterCast) {
            boolean counter=counterWindow>0&&(dashX*facingX+dashY*facingY)/Math.hypot(facingX,facingY)<-.6;
            waterKind=counter?WaterProjectile.Kind.COUNTER:combo==2?WaterProjectile.Kind.FINISHER
                    :combo==1?WaterProjectile.Kind.RETURN_CUT:WaterProjectile.Kind.CUT;
            waterWindup=counter?.04:combo==2?.22:combo==1?.10:.08;
            player.attackRecovery(slimeAttackDuration());
            if(counter) {combo=0;comboTime=0;}
        }
        counterWindow=0;
        int move=opener?DASH_SLASH:combo;
        attackDelay=waterCast?waterWindup:strikeWindup(move);
        if(!waterCast) {
            bladeLinked=linked;
            bladeMove=move;bladeDuration=bladeTime=strikeDuration(move);
            player.attackRecovery(bladeDuration);comboTime=bladeDuration+.5;
            dashOpener=0;releasedCharge=0;cleaveEmpowered=false;finisherEarned=false;
        }
        emit(EventType.ATTACK,player.x(),player.y());
        return true;
    }
    /*
     * Weighted, not frantic (2026-10-23): the old 0.30-0.54 s moves played 12 poses at ~40 fps, so the
     * four-pose draw lasted 90 ms and nothing read. Light cuts now sit at 0.40-0.48 s with a 0.13-0.17 s
     * draw (Hades / Dead Cells range); the finisher is the longest move. BladeCombatTest guards the floor.
     */
    static double strikeWindup(int move) { return switch(move) {case 1->.14;case 2->.17;case 3->.24;case 4,SPIN_CLEAVE->.18;case DASH_SLASH->.10;default->.13;}; }
    static double strikeDuration(int move) { return switch(move) {case 1->.42;case 2->.48;case 3->.70;case 4->.58;case DASH_SLASH->.42;case SPIN_CLEAVE->.70;default->.40;}; }
    /** Link after the cut follows through, before the last return-to-guard poses. */
    static double strikeLinkTime(int move) {return strikeWindup(move)+(strikeDuration(move)-strikeWindup(move))*.68;}
    private boolean bladeComboActive() {return bladeAttacking()&&comboTime>0&&combo<player.bladeComboLength()-1
            &&(bladeMove<3||bladeMove==DASH_SLASH);}
    private double primaryInputWait() {return bladeComboActive()?Math.max(0,strikeLinkTime(bladeMove)-bladeElapsed()):player.attackCooldown();}
    private double primaryBuffer() {return bladeComboActive()?.24:INPUT_BUFFER;}
    /** Every cut is made with a step: world pixels he advances during the wind-up, landing on contact. */
    static double strikeStep(int move) { return switch(move) {case 1->30;case 3->20;case 4,SPIN_CLEAVE->26;case DASH_SLASH->40;default->16;}; }
    /**
     * The move whose HIT RULES a variation uses (width, single target, knockback, hit pause). The dash slash
     * hits like the wide draw cut, not the narrow thrust it replaced; the spin is a cleave.
     */
    static int strikeKind(int move) { return move==DASH_SLASH?0:move==SPIN_CLEAVE?4:move; }
    public boolean primaryPress(int x,int y) {
        if(blocked()||voidActive()||!player.alive()||player.recovering()||guarding()||primaryHeld||queuedAttack!=0||(x==0&&y==0))return false;
        if(primaryInputWait()>primaryBuffer()+1e-9||player.dashSeconds()>INPUT_BUFFER)return false;
        cancelAbsorption();
        primaryHeld=true;primaryTime=0;chargeReady=false;
        aimSlimeCharge(x,y);return true;
    }
    public void primaryRelease(int x,int y) {
        if(!primaryHeld)return;
        boolean charged=charging;double held=primaryTime;
        primaryHeld=charging=chargeReady=false;primaryTime=0;
        if(blocked()||!player.alive())return;
        if(!player.bladeForm()) {
            if(charged)chargedJet(x,y,Math.min(1,held/FULL_CHARGE));
            else if(held<CHARGE_THRESHOLD&&(x!=0||y!=0)) {
                if(attack(x,y)) {
                    double credit=Math.min(held,waterWindup*.7);
                    attackDelay-=credit;player.attackRecovery(slimeAttackDuration()-credit);
                } else if(player.attackCooldown()<=INPUT_BUFFER&&player.dashSeconds()<=INPUT_BUFFER) {
                    queuedAttack=1;queuedX=x;queuedY=y;queuedTime=INPUT_BUFFER+.01;
                }
            }
            return;
        }
        if(charged)chargedCleave(x,y,Math.min(1,held/FULL_CHARGE));
        else if(held<CHARGE_THRESHOLD) {
            boolean busy=bladeTime>0;
            if(requestBladeAttack(false,x,y)&&!busy&&bladeTime>0)creditPress(held);
        }
    }
    /**
     * A click only becomes an attack on release, because a hold is a charge. The press time is already
     * spent wind-up: the draw began on screen when the button went down. Counting it keeps contact at
     * press + wind-up instead of release + wind-up, which is what made clicks feel late.
     */
    private void creditPress(double held) {
        double credit=Math.min(held,bladeContact()*.7);
        if(credit<=0)return;
        double length=Math.max(1,Math.hypot(aimX,aimY));
        player.attackStep(aimX/length,aimY/length,strikeStep(bladeMove)*credit/bladeContact(),map);
        attackDelay=Math.max(0,attackDelay-credit);bladeTime-=credit;
        player.attackRecovery(bladeTime);
    }
    public boolean requestBladeAttack(boolean crescent,int x,int y) {
        if(blocked()||voidActive()||!player.bladeForm()||guarding()||charging||(x==0&&y==0))return false;
        if(crescent?voidSever(x,y):attack(x,y))return true;
        if(crescent&&voidReadiness<1)return false;
        if(queuedAttack==0||crescent) {
            double wait=crescent?player.attackCooldown():primaryInputWait(),buffer=crescent?INPUT_BUFFER:primaryBuffer();
            if(wait<=buffer+1e-9&&player.dashSeconds()<=INPUT_BUFFER+1e-9) {
                queuedAttack=crescent?2:1;queuedX=x;queuedY=y;queuedTime=buffer+.01;
                return true;
            }
        }
        return false;
    }
    private void updatePrimaryInput(double seconds) {
        if(!player.alive()||player.recovering()){cancelBladeInput();return;}
        if(primaryHeld) {
            primaryTime+=seconds;
            if(primaryTime>=CHARGE_THRESHOLD&&!player.dashing()&&player.attackCooldown()==0&&!guarding())charging=true;
            if(charging&&primaryTime>=FULL_CHARGE&&!chargeReady) {
                chargeReady=true;emit(EventType.CHARGE_READY,player.x(),player.y());
            }
        }
        if(queuedAttack!=0) {
            boolean started=queuedAttack==2&&player.bladeForm()?voidSever(queuedX,queuedY):attack(queuedX,queuedY);
            queuedTime-=seconds;
            if(started||queuedTime<=0)queuedAttack=0;
        }
    }
    private boolean chargedJet(int x,int y,double power) {
        if((x==0&&y==0)||!player.startAttack())return false;
        cancelAbsorption();aimX=x;aimY=y;releasedCharge=power;
        waterCast=true;heavyCast=false;waterKind=WaterProjectile.Kind.CHARGED_JET;
        attackDelay=waterWindup=.04;player.attackRecovery(slimeAttackDuration());
        comboTime=counterWindow=0;combo=0;return true;
    }
    private boolean chargedCleave(int x,int y,double power) {
        if((x==0&&y==0)||voidActive()||guarding()||!player.startAttack())return false;
        cancelAbsorption();cleaveEmpowered=devoured;devoured=false;
        // Held to the charge-ready cue, the cut carries him right round; anything less is the plain cleave.
        aimX=x;aimY=y;releasedCharge=power;bladeMove=power>=1?SPIN_CLEAVE:4;counterTime=0;bladeLinked=false;
        bladeDuration=bladeTime=strikeDuration(bladeMove);player.attackRecovery(bladeDuration);
        strikeDamage=player.attackDamage()+(int)Math.round(36*power);strikeRange=BLADE_ATTACK_RANGE;
        waterCast=heavyCast=false;attackDelay=strikeWindup(bladeMove);comboTime=dashOpener=0;combo=0;
        emit(EventType.ATTACK,player.x(),player.y());return true;
    }
    private void clearBladeButtons() {primaryHeld=charging=chargeReady=false;primaryTime=queuedTime=0;queuedAttack=0;}
    public void cancelBladeInput() {
        voidTime=-1;voidHits.clear();voidBossHit=false;empoweredDelay=0;empoweredHits.clear();empoweredBoss=false;cleaveEmpowered=false;
        clearBladeButtons();bladeTime=bladeCastTime=dashOpener=counterTime=0;comboDodged=bladeLinked=false;
        if(!waterCast||player.bladeForm()){comboTime=0;attackDelay=0;}
    }
    public boolean bladePreparing() {return player.bladeForm()&&primaryHeld&&!player.dashing()&&!guarding()&&player.attackCooldown()==0;}
    public boolean slimePreparing() {return !player.bladeForm()&&primaryHeld&&player.alive()&&!player.dashing()&&player.attackCooldown()==0;}
    public void aimSlimeCharge(int x,int y) {if(slimePreparing()&&(x!=0||y!=0)){aimX=x;aimY=y;}}
    public boolean charging() {return charging;}
    public double chargeProgress() {return Math.min(1,primaryTime/FULL_CHARGE);}
    public double chargeSeconds() {return primaryTime;}
    public boolean bladeCasting() {return player.bladeForm()&&bladeCastTime>0;}
    public boolean bladeCountering() {return player.bladeForm()&&counterTime>0;}
    public double counterElapsed() {return COUNTER_DURATION-counterTime;}
    public double bladeCastElapsed() {return BLADE_CAST_DURATION-bladeCastTime;}
    public double bladeElapsed() {
        if(!voidActive())return bladeDuration-bladeTime;
        if(voidTime<VOID_WINDUP)return voidTime/VOID_WINDUP*.098;
        if(voidTime<.66)return .10+(voidTime-.48)/.18*.14;
        return voidTime<.80?(voidTime-.66)/.14*.179:.18+(voidTime-.80)/.42*.519;
    }
    public double bladeContact() {return strikeWindup(bladeMove);}
    public boolean bladeAttacking() {return (voidActive()||bladeTime>0)&&player.bladeForm();}
    public int bladeMove() {return voidActive()?(voidTime<.66?DASH_SLASH:SPIN_CLEAVE):bladeMove;}
    public boolean bladeLinked() {return !voidActive()&&bladeAttacking()&&bladeLinked;}
    public int bladeAimX() {return aimX;}
    public int bladeAimY() {return aimY;}
    public int dashAimX() {return (int)Math.round(dashX*1000);}
    public int dashAimY() {return (int)Math.round(dashY*1000);}
    public double releasedCharge() {return releasedCharge;}
    public int comboStep() {return comboTime>0?combo+1:0;}

    public double slimeAttackDuration() {
        return switch(waterKind) {case CUT->.40;case RETURN_CUT->.44;case FINISHER->.68;case COUNTER->.36;case CHARGED_JET->.58;case TIDE->Player.WATER_COOLDOWN;};
    }
    public double slimeAttackElapsed() {
        return waterCast&&!player.bladeForm()&&player.alive()&&!player.dashing()&&player.attackCooldown()>0
                ?Math.max(0,slimeAttackDuration()-player.attackCooldown()):-1;
    }
    public double slimeReleaseTime() {return waterWindup;}

    public boolean voidActive(){return voidTime>=0;}
    public boolean voidAdvancing(){return voidTime>=VOID_WINDUP&&voidTime<VOID_WINDUP+VOID_ADVANCE;}
    public double voidElapsed(){return voidTime;}
    public double voidReadiness(){return voidReadiness;}
    public boolean devoured(){return devoured;}
    public boolean voidEmpowered(){return voidEmpowered;}
    public boolean cleaveEmpowered(){return cleaveEmpowered;}
    public double voidStartX(){return voidStartX;}
    public double voidStartY(){return voidStartY;}
    public double voidEndX(){return voidEndX;}
    public double voidEndY(){return voidEndY;}
    private void earnVoid(double amount){voidReadiness=Math.min(1,voidReadiness+amount);}
    private void resetKit(){voidReadiness=0;devoured=false;cancelBladeInput();}
    public void aimVoidSever(int x,int y) {
        if(voidActive()&&voidTime<VOID_WINDUP&&(x!=0||y!=0)){aimX=x;aimY=y;}
    }
    public boolean voidSever(int x,int y) {
        if(blocked()||voidActive()||!player.bladeForm()||guarding()||charging()||(x==0&&y==0))return false;
        if(voidReadiness<1){notice("VOID SEVER / EARN THROUGH FINISHERS, PARRIES AND ABSORPTION");deny();return false;}
        if(!player.startAttack())return false;
        cancelAbsorption();cancelBladeInput();voidReadiness=0;
        voidEmpowered=devoured;devoured=false;voidTime=0;aimX=x;aimY=y;
        voidStartX=voidEndX=player.x();voidStartY=voidEndY=player.y();
        player.attackRecovery(VOID_DURATION);emit(EventType.VOID_START,player.x(),player.y());return true;
    }
    private void updateVoidSever(double seconds) {
        if(!voidActive())return;
        if(!player.alive()||!player.bladeForm()){cancelBladeInput();return;}
        double before=voidTime;voidTime+=seconds;
        if(before<VOID_WINDUP&&voidTime>=VOID_WINDUP)emit(EventType.VOID_RELEASE,player.x(),player.y());
        double travel=Math.max(0,Math.min(voidTime,VOID_WINDUP+VOID_ADVANCE)-Math.max(before,VOID_WINDUP));
        if(travel>0) {
            double length=Math.max(1,Math.hypot(aimX,aimY)),distance=320*travel/VOID_ADVANCE;
            int steps=Math.max(1,(int)Math.ceil(distance/4));
            for(int i=0;i<steps;i++) {
                double x=player.x(),y=player.y();
                // Stop the lane at walls rather than sliding the attack around corners.
                double nx=x+aimX/length*distance/steps,ny=y+aimY/length*distance/steps;
                if(map.isBlocked(nx,ny+Player.COLLISION_Y_OFFSET,Player.COLLISION_RADIUS,false))break;
                player.attackStep(aimX/length,aimY/length,distance/steps,map);
                voidEndX=player.x();voidEndY=player.y();
                for(Wisp body:scouts)if(body.alive()&&!voidHits.contains(body)&&near(body.x(),body.y(),70)
                        &&map.clearLine(player.x(),player.y(),body.x(),body.y())) {
                    voidHits.add(body);
                    if(hurtScout(body,32,false,true)) {
                        afterScoutHit(body);
                        if(voidHits.size()==1){emit(EventType.VOID_CONTACT,body.x(),body.y());hitStop=Math.max(hitStop,.05);}
                    }
                }
                if(!voidBossHit&&bossActive()&&guardian.alive()&&near(bossNearX(),guardian.y(),100)
                        &&map.clearLine(player.x(),player.y(),bossNearX(),guardian.y())) {
                    voidBossHit=true;if(hurtGuardian(32,false))afterGuardianHit(false);
                }
            }
        }
        if(before<VOID_RUPTURE_TIME&&voidTime>=VOID_RUPTURE_TIME) {
            emit(EventType.VOID_RUPTURE,(voidStartX+voidEndX)/2,(voidStartY+voidEndY)/2);
            for(Wisp body:new ArrayList<>(voidHits))if(body.alive()&&Math.hypot(body.x()-player.x(),body.y()-player.y())<=480
                    &&map.clearLine(player.x(),player.y(),body.x(),body.y())&&hurtScout(body,voidEmpowered?66:48,false,true)) {
                afterScoutHit(body);body.push(aimX/Math.max(1,Math.hypot(aimX,aimY))*180,aimY/Math.max(1,Math.hypot(aimX,aimY))*180);
                if(voidEmpowered)body.openForPunish();
            }
            if(voidBossHit&&guardian.alive()&&bossActive()&&near(bossNearX(),guardian.y(),150)
                    &&map.clearLine(player.x(),player.y(),bossNearX(),guardian.y())&&hurtGuardian(voidEmpowered?66:48,false))afterGuardianHit(false);
            hitStop=Math.max(hitStop,.10);
        }
        if(voidTime>=VOID_DURATION){voidTime=-1;voidHits.clear();voidBossHit=false;}
    }
    private void updateEmpoweredHits(double seconds) {
        if(empoweredDelay<=0)return;
        empoweredDelay=Math.max(0,empoweredDelay-seconds);if(empoweredDelay>0)return;
        for(Wisp body:new ArrayList<>(empoweredHits))if(body.alive()&&near(body.x(),body.y(),240)
                &&map.clearLine(player.x(),player.y(),body.x(),body.y())&&hurtScout(body,18,false,true)) {
            afterScoutHit(body);emit(EventType.VOID_CONTACT,body.x(),body.y());
        }
        if(empoweredBoss&&guardian.alive()&&bossActive()&&near(bossNearX(),guardian.y(),200)
                &&map.clearLine(player.x(),player.y(),bossNearX(),guardian.y())&&hurtGuardian(18,false))afterGuardianHit(false);
        empoweredHits.clear();empoweredBoss=false;
    }

    public boolean ichorCrescent(int facingX,int facingY) {
        if(!blocked()&&player.bladeForm()&&(crescentCooldown>0||player.ichor()<=BLADE_SKILL_COST))deny();
        if(blocked()||guarding()||!player.bladeForm()||crescentCooldown>0
                ||player.ichor()<=BLADE_SKILL_COST||(facingX==0&&facingY==0)||!player.startAttack())return false;
        player.spendBladeIchor(BLADE_SKILL_COST);
        cancelAbsorption();cancelBladeInput();
        aimX=facingX;aimY=facingY;bladeCastTime=BLADE_CAST_DURATION;
        crescentCooldown=CRESCENT_COOLDOWN;
        crescents.add(new Crescent(player.x(),player.y(),facingX,facingY));
        emit(EventType.CRESCENT_CAST,player.x(),player.y());return true;
    }
    public boolean beelzebub(int facingX,int facingY) {
        if(blocked()||voidActive()||!player.bladeForm()||!player.alive()||player.recovering()||player.dashing()
                ||beelzebubActive()||guarding()||bladeAttacking()||bladeCasting()||primaryHeld||counterTime>0
                ||(facingX==0&&facingY==0))return false;
        if(beelzebubCooldown>0) { deny();return false; }
        var targets=new ArrayList<AbsorptionPull>();
        for(var corpse:corpses)if(corpseReachable(corpse,BEELZEBUB_RADIUS))
            targets.add(new AbsorptionPull(corpse,corpse.x(),corpse.y()));
        if(targets.isEmpty()) { notice("NO REMAINS IN REACH");deny();return false; }
        if(!player.startAttack())return false;
        cancelAbsorption();cancelBladeInput();
        beelzebubTargets.addAll(targets);absorptionTarget=targets.get(0).corpse();
        absorptionX=player.x();absorptionY=player.y();absorptionHealth=player.healthValue();
        aimX=facingX;aimY=facingY;beelzebubCooldown=BEELZEBUB_COOLDOWN;
        if(campaignMode&&bark==null)startBark(switch(beelzebubCalls++%3) {
            case 0 -> OutpostBark.BEELZEBUB_CALL;
            case 1 -> OutpostBark.BEELZEBUB_CALL_AGAIN;
            default -> OutpostBark.BEELZEBUB_CALL_TAKE;
        });
        emit(EventType.BEELZEBUB_CAST,player.x(),player.y());return true;
    }
    public boolean riposte() {
        if(!blocked()&&player.bladeForm()&&riposteCooldown>0)deny();
        if(blocked()||voidActive()||!player.bladeForm()||riposteCooldown>0||!player.startAttack())return false;
        cancelAbsorption();cancelBladeInput();
        guardTime=RIPOSTE_DURATION;riposteCooldown=RIPOSTE_COOLDOWN;
        emit(EventType.RIPOSTE_START,player.x(),player.y());return true;
    }
    public boolean tideWave(int facingX,int facingY) {
        if(!blocked()&&!player.bladeForm()&&tideCooldown>0)deny();
        if(blocked()||player.bladeForm()||tideCooldown>0||(facingX==0&&facingY==0)
                ||!player.startAttack()) return false;
        cancelAbsorption();clearBladeButtons();
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
                projectiles.add(new WaterProjectile(player.x(),player.y(),aimX,aimY,waterKind,player.trait()==Player.Trait.JET,releasedCharge));
                if(heavyCast)events.add(new Event(EventType.TIDE_RELEASE,player.x(),player.y(),true));
                else if(waterKind==WaterProjectile.Kind.FINISHER)emit(EventType.FINISHER_RELEASE,player.x(),player.y());
                else if(waterKind==WaterProjectile.Kind.CHARGED_JET) {
                    emit(EventType.WATER_JET_RELEASE,player.x(),player.y());
                    double length=Math.max(1,Math.hypot(aimX,aimY));
                    player.attackStep(-aimX/length,-aimY/length,10+8*releasedCharge,map);
                }
            }
            return;
        }
        if(!player.bladeForm()) return;
        Wisp nearest=null;
        for(Wisp scout:scouts)if(scout.alive()&&bladeHits(scout.x(),scout.y())
                &&(nearest==null||Math.hypot(scout.x()-player.x(),scout.y()-player.y())
                    <Math.hypot(nearest.x()-player.x(),nearest.y()-player.y())))nearest=scout;
        boolean bossHit=bossActive()&&!bossDefeated&&bladeHits(bossNearX(),guardian.y());
        boolean bossFirst=bossHit&&(nearest==null||Math.hypot(guardian.x()-player.x(),guardian.y()-player.y())
                <Math.hypot(nearest.x()-player.x(),nearest.y()-player.y()));
        int kind=strikeKind(bladeMove);boolean landed=false;
        for(Wisp scout:scouts) {
            if(!scout.alive()||!bladeHits(scout.x(),scout.y())||(kind==1&&(scout!=nearest||bossFirst)))continue;
            if(hurtScout(scout,strikeDamage,false,true)) {
                landed=true;
                double length=Math.max(1,Math.hypot(aimX,aimY));
                double force=kind==4?220+100*releasedCharge:kind==3?260:kind==1?110:55;
                scout.push(aimX/length*force,aimY/length*force);afterScoutHit(scout);
                if(kind==3&&!finisherEarned){earnVoid(.34);finisherEarned=true;}
                if(kind==4&&cleaveEmpowered&&!empoweredHits.contains(scout))empoweredHits.add(scout);
                if(kind>=3)hitStop=.1;
            }
        }
        if(bossHit&&(kind!=1||bossFirst)&&hurtGuardian(strikeDamage,false)) {
            landed=true;afterGuardianHit(false);
            if(kind==3&&!finisherEarned){earnVoid(.34);finisherEarned=true;}
            if(kind==4&&cleaveEmpowered)empoweredBoss=true;
        }
        if(kind==4&&landed) {
            events.add(new Event(EventType.EMPOWERED_HIT,player.x(),player.y(),false,0,false,cleaveEmpowered));
            if(cleaveEmpowered)empoweredDelay=.16;
        }
        fieldStrike();
    }
    private boolean bladeHits(double x,double y) {
        double length=Math.max(1,Math.hypot(aimX,aimY)),dx=x-player.x(),dy=y-player.y();
        double forward=(dx*aimX+dy*aimY)/length,side=Math.abs(dx*aimY-dy*aimX)/length;
        int kind=strikeKind(bladeMove);
        double width=kind==1?24:kind==3?48:strikeRange*.65;
        return forward>=0&&forward<=strikeRange&&side<=width&&map.clearLine(player.x(),player.y(),x,y);
    }
    private void afterGuardianHit(boolean water) {
        events.add(new Event(EventType.ENEMY_HIT,guardian.x(),guardian.y(),water));
        hitStop=0.06;
        if (!guardian.alive()&&!bossDefeated) {
                bossDefeated=true;
                gateTime=0;
                if(campaignMode) {
                    if(clearedBosses.add(biome))grantShards(4+biome*2);
                    notice(guardian.bossName().toUpperCase(java.util.Locale.ROOT)+" DEFEATED / ROUTE OPEN");
                    saveProgress();
                } else {cleared[9]=true;story.guardianDefeated();}
                map.clearGuardian();
                map.openGate();
                forestHazards.clear();
                for (Wisp add:scouts) if(add.alive()&&(!campaignMode||bossAdds.contains(add))) add.hurt(add.health());
                sprouts.clear();                                    // nothing else breaks the ground once it falls
                events.add(new Event(EventType.ENEMY_DEFEATED,guardian.x(),guardian.y(),water));
                if(campaignMode&&biome==0&&player.alive()) {
                    cancelAbsorption();cancelBladeInput();
                    player.relocate(player.x(),player.y());attackDelay=waterWindup=bladeTime=bladeCastTime=guardTime=counterTime=hitStop=0;waterCast=false;
                    pendingBossBark=null;pendingBanter=null;hurtReactionTime=0;
                    wardenVictoryTime=0;wardenVictorySpoken=false;
                    wardenCameraX=guardian.x();wardenCameraY=guardian.y()+24;
                } else bossBark(biome==1?OutpostBark.FOREST_BOSS_VICTORY:OutpostBark.WARDEN_VICTORY);
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
                    if(hurtGuardian(wave.damage(),true)) afterGuardianHit(true);
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
                            if(wave.kind()==WaterProjectile.Kind.FINISHER||wave.kind()==WaterProjectile.Kind.CHARGED_JET)hitStop=.1;
                            if(wave.heavy())emit(EventType.TIDE_IMPACT,scout.x(),scout.y());
                        }
                        // Bodies part around the wave; a raised shield or solid terrain stops it.
                        if(!wave.heavy()||!damaged) {wave.stop();break;}
                    }
                }
                if(!wave.alive()) events.add(new Event(wave.heavy()?EventType.TIDE_IMPACT:
                        wave.kind()==WaterProjectile.Kind.CHARGED_JET?EventType.WATER_JET_IMPACT:
                        wave.kind()==WaterProjectile.Kind.FINISHER?EventType.FINISHER_IMPACT:EventType.WATER_IMPACT,wave.x(),wave.y(),true));
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
                    if(hurtGuardian(36,false))afterGuardianHit(false);
                    arc.alive=false;
                } else for(Wisp scout:scouts)if(scout.alive()
                        &&Math.hypot(scout.x()-arc.x,scout.y()-arc.y)<=Wisp.COLLISION_RADIUS+arc.radius()) {
                    if(hurtScout(scout,36,false,true)) {
                        scout.push(arc.dx*260,arc.dy*260);afterScoutHit(scout);
                    }
                    arc.alive=false;break;
                }
                if(!arc.alive)emit(EventType.CRESCENT_IMPACT,arc.x,arc.y);
            }
        }
        crescents.removeIf(arc->!arc.alive);
    }
    private void updateForestAttackCue(Wisp body) {
        if(body.role()!=Wisp.Role.RIFT_IMP&&body.role()!=Wisp.Role.THORN_WOLF&&body.role()!=Wisp.Role.MIRE_SHAMAN)return;
        int phase=body.state()==Wisp.State.TELEGRAPH?1:0;
        if(body.state()==Wisp.State.LUNGE) {
            if(body.role()==Wisp.Role.RIFT_IMP)phase=body.stateSeconds()>=.40?3:body.stateSeconds()>=.04?2:0;
            else if(body.role()==Wisp.Role.THORN_WOLF)phase=2;
        } else if(body.state()==Wisp.State.RECOVER&&body.wolfLeap())phase=3;
        Integer before=forestAttackPhases.put(body,phase);
        if(phase==0||before!=null&&before==phase)return;
        EventType type=switch(body.role()) {
            case RIFT_IMP -> phase==1?EventType.IMP_WINDUP:EventType.IMP_CLAW;
            case THORN_WOLF -> phase==1?EventType.WOLF_GROWL:phase==3?EventType.WOLF_LAND
                    :body.wolfLeap()?EventType.WOLF_POUNCE:EventType.WOLF_BITE;
            default -> EventType.MIRE_CHARGE;
        };
        emit(type,body.x(),body.y());
    }

    private void afterScoutHit(Wisp scout) {
        afterScoutHit(scout,false);
    }
    private boolean hurtScout(Wisp scout,int damage,boolean slime,boolean stagger) {
        var enemy=campaignEnemies.get(scout);
        if (!scout.alive() || damage<=0 || (!debugOneShot && slime && enemy!=null && enemy.shielded())) return false;
        boolean critical=!debugOneShot && criticalRoll.getAsDouble()<CombatStats.CRITICAL_CHANCE;
        int amount=debugOneShot?scout.health():CombatStats.damage(damage,scout.defense(),critical);
        int before=scout.health();
        boolean hit=debugOneShot || enemy==null ? scout.hurt(amount,stagger) : enemy.receiveDamage(amount,slime,stagger);
        if(hit) damageEvent(scout.x(),scout.y(),before-scout.health(),critical,damage>Player.BLADE_DAMAGE,slime);
        return hit;
    }
    private boolean bossVulnerable() { return debugOneShot||guardian.coreExposed(); }
    private boolean hurtGuardian(int attack,boolean water) {
        if(!guardian.alive() || attack<=0)return false;
        if(!bossVulnerable()) {
            if(campaignMode&&armorNoticeTime==0) {
                emit(EventType.ARMOR_BLOCK,guardian.x(),guardian.y());
                notice(biome==1?"ROOTS SHIELD ITS HEART / WAIT FOR THE OPENING":"ARMOR HELD / STRIKE AFTER ITS ATTACK");armorNoticeTime=.8;
            }
            return false;
        }
        boolean critical=!debugOneShot && criticalRoll.getAsDouble()<CombatStats.CRITICAL_CHANCE;
        int before=guardian.health();
        boolean hit=guardian.hurt(debugOneShot?before:CombatStats.damage(attack,25,critical));
        if(hit)damageEvent(guardian.x(),guardian.y(),before-guardian.health(),critical,attack>Player.BLADE_DAMAGE,water);
        return hit;
    }
    private void damageEvent(double x,double y,int damage,boolean critical,boolean heavy,boolean water) {
        events.add(new Event(EventType.DAMAGE_DEALT,x,y,water,damage,critical,heavy));
    }
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
                    if(!dashProtected)hurtPlayer(18,shot.x(),shot.y());shot.stop();
                }
                if(!shot.alive())emit(EventType.SPIT_IMPACT,shot.x(),shot.y());
            }
        }
        hostileProjectiles.removeIf(shot->!shot.alive());
    }
    private void afterScoutHit(Wisp scout,boolean water) {
        events.add(new Event(EventType.ENEMY_HIT,scout.x(),scout.y(),water));
        hitStop=Math.max(hitStop,0.05);          // light hits hold three frames, kills five (docs/design/attack-feel.md)
        if (scout.alive()) return;
        if(campaignMode&&!bossActive()&&!bossAdds.contains(scout)) {
            pendingBanter=OutpostBark.Trigger.FIGHT;pendingBanterTime=45;quietTime=0;
            banterCooldown=Math.min(banterCooldown,12);
            var fallen=campaignEnemies.get(scout);lastDefeatedKind=fallen==null?null:fallen.kind();
        }
        hitStop=Math.max(hitStop,0.085);
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
        if(blocked())return false;
        double feet=player.y()+Player.COLLISION_Y_OFFSET;
        // Only a slime-only gap traps a Blade; anywhere else keeps the old rules.
        if(!player.bladeForm()&&map.isBlocked(player.x(),feet,Player.COLLISION_RADIUS,false)
                &&!map.isBlocked(player.x(),feet,Player.COLLISION_RADIUS,true)) {
            notice("NO ROOM TO CHANGE HERE");deny();return false;
        }
        if(!player.transform()) return false;
        waterCast=false;attackDelay=0;
        endAbsorption();
        cancelBladeInput();
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
        if(beelzebubActive())return "BEELZEBUB / "+beelzebubTargets.size()+" REMAINS";
        if (blocked()) return "";
        if(campaignMode) {
            String field=fieldPrompt();
            if(!field.isEmpty())return field;
            if(nearCampaignHub())return (biome==0?"E TALK TO SHUNA / ":"E TALK TO SOUEI / ")+"H REST + SAVE / U CAMP";
            for(var landmark:campaignArea().landmarks())if(landmark.optional()&&near(landmark.x(),landmark.y(),120))return "E EXAMINE "+landmark.name();
            if(near(campaignArea().exit().x(),campaignArea().exit().y(),140))return !bossDefeated?"DEFEAT "+guardian.bossName()
                    :biome+1<CampaignWorld.AREA_COUNT?"E ENTER THE "+CampaignWorld.area(biome+1).name():"E DESCEND THE STAIR";
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
    // ------------------------------------------------------------------ field objects (FieldObjects)

    private boolean fieldDone(String id) { return campaignUnlocks.contains("field_"+id); }
    private void markField(String id) { campaignUnlocks.add("field_"+id); }
    public boolean fieldDone(FieldObjects.Spot spot) { return fieldDone(spot.id()); }
    public boolean restLit(int index) { return campaignMode&&biome==0
            &&(index==FieldObjects.GATE_REST||fieldDone("rest_"+index)); }
    public FieldObjects.Spot carried() { return carried; }
    public int rescued() { int n=0; for(int i=0;i<FieldObjects.SURVIVORS.length;i++)if(fieldDone("survivor_"+i))n++; return n; }
    public int tinsDelivered() { int n=0; for(var s:FieldObjects.all(0))if(s.kind()==FieldObjects.Kind.TIN&&fieldDone(s))n++; return n; }
    public boolean survivorKnown(FieldObjects.Spot spot) {
        return spot.kind()==FieldObjects.Kind.SURVIVOR&&(fieldDone(spot)||campaignUnlocks.contains("found_"+spot.id())
                ||spot.variant()==0&&dialogueSeen(OutpostDialogue.CAMP));
    }

    private static int restIndex(int checkpoint) {
        return checkpoint>=FieldObjects.REST_CHECKPOINT&&checkpoint<FieldObjects.REST_CHECKPOINT+FieldObjects.rests(0).size()
                ?checkpoint-FieldObjects.REST_CHECKPOINT:-1;
    }

    /** Where a carriable lies now, or null while it rides in the slime or is used up. */
    public CampaignWorld.Point fieldPosition(FieldObjects.Spot spot) {
        if(!campaignMode||spot==carried||fieldDone(spot))return null;
        return dropped.getOrDefault(spot.id(),new CampaignWorld.Point(spot.x(),spot.y()));
    }

    /** Thorns stop everyone until cut; a crawl fence's gap stops all but the slime. */
    private void syncFieldGates() {
        var solid=new ArrayList<RuinedOutpostMap.Obstacle>();var slimeOnly=new ArrayList<RuinedOutpostMap.Obstacle>();
        if(campaignMode)for(var spot:FieldObjects.all(biome)) {
            if(spot.kind()!=FieldObjects.Kind.CRAWL&&spot.kind()!=FieldObjects.Kind.BRAMBLE)continue;
            if(spot.kind()==FieldObjects.Kind.BRAMBLE&&fieldDone(spot))continue;
            solid.addAll(FieldObjects.gateParts(spot,false));slimeOnly.addAll(FieldObjects.gateParts(spot,true));
        }
        map.setFieldGates(solid,slimeOnly);
    }

    private int nearLitRest() {
        var rests=FieldObjects.rests(biome);
        for(int i=0;i<rests.size();i++)if(restLit(i)&&near(rests.get(i).x(),rests.get(i).y(),150))return i;
        return -1;
    }

    private FieldObjects.Spot nearbyField(FieldObjects.Kind kind,double reach) {
        if(!campaignMode)return null;
        for(var spot:FieldObjects.all(biome)) {
            if(spot.kind()!=kind)continue;
            var at=FieldObjects.carriable(kind)?fieldPosition(spot):fieldDone(spot)?null:new CampaignWorld.Point(spot.x(),spot.y());
            if(at!=null&&near(at.x(),at.y(),reach))return spot;
        }
        return null;
    }

    private void dropCarried() {
        if(carried==null)return;
        dropped.put(carried.id(),new CampaignWorld.Point(player.x(),Math.min(campaignArea().height()-1,player.y()+40)));
        notice(carried.name()+" SLIPS OUT");carried=null;
        saveProgress();
    }

    /** E near the field objects: deliver, rescue, unlock, swallow or put down. False when none applies. */
    private boolean fieldInteract() {
        if(!campaignMode)return false;
        boolean slime=!player.bladeForm();
        if(carried!=null&&carried.kind()==FieldObjects.Kind.TIN&&biome==0&&(nearCampaignHub()||nearLitRest()>=0)) {
            markField(carried.id());carried=null;grantShards(1);player.heal(25);emit(EventType.HEALED,player.x(),player.y());
            notice("SUPPLIES SECURED / "+tinsDelivered()+" OF "+FieldObjects.count(0,FieldObjects.Kind.TIN)+" / +25 HP");
            saveProgress();return true;
        }
        var survivor=nearbyField(FieldObjects.Kind.SURVIVOR,110);
        if(survivor!=null) {
            markField(survivor.id());grantShards(2);emit(EventType.UPGRADE,player.x(),player.y());
            openDialogue(OutpostDialogue.survivor(survivor.variant()));saveProgress();return true;
        }
        var cache=nearbyField(FieldObjects.Kind.CACHE,110);
        if(cache!=null) {
            if(carried==null||carried.kind()!=FieldObjects.Kind.KEY) {notice("LOCKED / ITS KEY IS SOMEWHERE IN THE OUTPOST");deny();return true;}
            markField(carried.id());markField(cache.id());carried=null;grantShards(3);emit(EventType.UPGRADE,player.x(),player.y());
            openDialogue(OutpostDialogue.LEDGER);saveProgress();return true;
        }
        for(var kind:new FieldObjects.Kind[]{FieldObjects.Kind.KEY,FieldObjects.Kind.TIN}) {
            var item=nearbyField(kind,90);
            if(item==null)continue;
            if(!slime) {notice("NO POCKETS IN THIS FORM / THE SLIME COULD CARRY IT");deny();return true;}
            if(carried!=null)break;
            carried=item;dropped.remove(item.id());emit(EventType.PICKUP,player.x(),player.y());saveProgress();
            notice(item.name()+" SWALLOWED / CARRY IT INSIDE");return true;
        }
        if(carried!=null&&nearbyCorpse()==null&&!nearCampaignHub()) {dropCarried();return true;}
        return false;
    }

    /** A Blade strike that reaches thorns cuts them; one that reaches a cold brazier strikes it alight. */
    private void fieldStrike() {
        if(!campaignMode||!player.bladeForm())return;
        for(var spot:FieldObjects.all(biome)) {
            if(spot.kind()!=FieldObjects.Kind.BRAMBLE||fieldDone(spot)||!fieldHit(spot.x(),spot.y()-12,40))continue;
            markField(spot.id());syncFieldGates();events.add(new Event(EventType.ENEMY_HIT,spot.x(),spot.y()-20,false));
            hitStop=.06;notice("THE THORNS FALL / "+spot.name()+" IS OPEN");saveProgress();
        }
        var rests=FieldObjects.rests(biome);
        for(int i=0;i<rests.size();i++) {
            if(restLit(i)||!fieldHit(rests.get(i).x(),rests.get(i).y()-30,30))continue;
            markField("rest_"+i);emit(EventType.CHECKPOINT,rests.get(i).x(),rests.get(i).y());
            notice("BRAZIER LIT / H TO REST HERE");saveProgress();
        }
    }
    /** The strike's reach, ignoring the target's own footprint (a brazier or a wall is solid). */
    private boolean fieldHit(double x,double y,double size) {
        double length=Math.max(1,Math.hypot(aimX,aimY)),dx=x-player.x(),dy=y-player.y();
        double forward=(dx*aimX+dy*aimY)/length,side=Math.abs(dx*aimY-dy*aimX)/length;
        return forward>=-size&&forward<=strikeRange+size&&side<=strikeRange*.65+size;
    }

    /** Hints and actions for the field objects in reach; empty when none. */
    private String fieldPrompt() {
        boolean slime=!player.bladeForm();
        if(carried!=null&&carried.kind()==FieldObjects.Kind.TIN&&biome==0&&(nearCampaignHub()||nearLitRest()>=0))
            return nearCampaignHub()?"E HAND OVER THE SUPPLY TIN":"E SECURE SUPPLIES HERE / +25 HP";
        var survivor=nearbyField(FieldObjects.Kind.SURVIVOR,110);
        if(survivor!=null)return "E TALK TO "+survivor.name();
        var cache=nearbyField(FieldObjects.Kind.CACHE,110);
        if(cache!=null)return carried!=null&&carried.kind()==FieldObjects.Kind.KEY?"E UNLOCK THE CACHE":"LOCKED CACHE / FIND ITS KEY";
        for(var kind:new FieldObjects.Kind[]{FieldObjects.Kind.KEY,FieldObjects.Kind.TIN}) {
            var item=nearbyField(kind,90);
            if(item==null)continue;
            if(!slime)return item.name()+" / ONLY THE SLIME CAN CARRY IT";
            if(carried==null)return "E SWALLOW THE "+item.name();
        }
        for(var spot:FieldObjects.all(biome)) {
            if(!near(spot.x(),spot.y(),170))continue;
            if(spot.kind()==FieldObjects.Kind.CRAWL&&!slime)return "TOO LOW TO PASS / THE SLIME COULD SQUEEZE UNDER";
            if(spot.kind()==FieldObjects.Kind.BRAMBLE&&!fieldDone(spot))return slime?"THORNS / THE BLADE COULD CUT THROUGH":"STRIKE TO CUT THE THORNS";
        }
        var rests=FieldObjects.rests(biome);
        for(int i=0;i<rests.size();i++)if(near(rests.get(i).x(),rests.get(i).y(),150))
            return restLit(i)?"H REST + SAVE":slime?"COLD BRAZIER / THE BLADE COULD LIGHT IT":"STRIKE TO LIGHT THE BRAZIER";
        if(carried!=null&&nearbyCorpse()==null&&!nearCampaignHub())return "CARRYING THE "+carried.name()+" / E PUT IT DOWN";
        return "";
    }

    public boolean interact() {
        if(dialogue!=null)return advanceDialogue();
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
        return player.alive()&&!player.bladeForm()&&!player.recovering()&&!player.dashing()&&!primaryHeld&&attackDelay==0;
    }
    private String absorbPrompt() {
        Corpse corpse=nearbyCorpse();
        return corpse==null?"":"E ABSORB"+(corpse.trait()==Player.Trait.NONE?"":" / "+corpse.trait().label());
    }
    private boolean corpseReachable(Corpse corpse) {
        return corpseReachable(corpse,ABSORB_RADIUS);
    }
    private boolean corpseReachable(Corpse corpse,double radius) {
        return near(corpse.x(),corpse.y(),radius)
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
    /** Ordinary actions (moving, dashing, a hit, guarding, attacks, charging, Crescent, Riposte, interacting) stop the
     *  slime's own swallow, but a cast Beelzebub keeps eating until it finishes (owner 2026-10-29: "it only draws the
     *  corpses closer, not absorb"). */
    private void cancelAbsorption() { if(!beelzebubActive())endAbsorption(); }
    /** The hard stop for both: death, reverting to slime, changing room or area, dialogue, the title, reset and debug. */
    private void endAbsorption() { absorptionTarget=null;absorptionTime=0;beelzebubTargets.clear(); }
    private void updateAbsorption(double seconds) {
        for(Corpse corpse:corpses)corpse.age+=seconds;
        corpses.removeIf(corpse->corpse.age>=CORPSE_LIFETIME);
        if(absorptionTarget==null)return;
        if(beelzebubActive()) { updateBeelzebub(seconds);return; }
        if(!canAbsorb()||!corpses.contains(absorptionTarget)||!corpseReachable(absorptionTarget)
                ||Math.hypot(player.x()-absorptionX,player.y()-absorptionY)>0.01
                ||player.healthValue()<absorptionHealth) { cancelAbsorption();return; }
        absorptionTime+=seconds;
        double pull=Math.pow(absorptionProgress(),2);
        absorptionTarget.x=corpseStartX+(player.x()-corpseStartX)*pull;
        absorptionTarget.y=corpseStartY+(player.y()-corpseStartY)*pull;
        if(absorptionTime+1e-9<ABSORB_DURATION)return;
        player.collectIchor(5);earnVoid(.25);
        double beforeHealth=player.healthValue();
        if(absorptionTarget.healing())player.heal(10);
        if(player.healthValue()>beforeHealth)emit(EventType.HEALED,player.x(),player.y());
        if(absorptionTarget.trait()!=Player.Trait.NONE) {
            player.gainTrait(absorptionTarget.trait());
            emit(EventType.TRAIT_GAINED,player.x(),player.y());
        }
        corpses.remove(absorptionTarget);cancelAbsorption();
        emit(EventType.ABSORBED,player.x(),player.y());
    }
    private void updateBeelzebub(double seconds) {
        // Owner 2026-10-29: "it only draws the corpses closer, not absorb". Once cast, the devour finishes:
        // the hand moves with him, a hit or a dash doesn't drop the meal; only death or reverting ends it.
        if(!player.alive()||!player.bladeForm()) { endAbsorption();return; }
        absorptionX=player.x();absorptionY=player.y();
        beelzebubTargets.removeIf(pull->!corpses.contains(pull.corpse()));   // caught at the cast, held to the end
        if(beelzebubTargets.isEmpty()) { endAbsorption();return; }
        absorptionTarget=beelzebubTargets.get(0).corpse();
        absorptionTime+=seconds;
        double pull=beelzebubPull();
        for(var target:beelzebubTargets) {
            target.corpse().x=target.x()+(absorptionX-target.x())*pull;
            target.corpse().y=target.y()+(absorptionY-target.y())*pull;
        }
        if(absorptionTime+1e-9<BEELZEBUB_DURATION)return;
        double beforeHealth=player.healthValue(),beforeIchor=player.ichor();
        player.collectIchor(5*beelzebubTargets.size());
        devoured=true;earnVoid(.25*beelzebubTargets.size());
        for(var target:beelzebubTargets) {
            if(target.corpse().healing())player.heal(10);
            corpses.remove(target.corpse());
        }
        // Blade form takes the resources; temporary absorbed traits belong to slime form.
        if(player.healthValue()>beforeHealth)emit(EventType.HEALED,player.x(),player.y());
        // The gulp lands at the hand's vortex (BeelzebubFx: 34 px along the aim), carrying the Ichor it gave.
        double length=Math.max(1,Math.hypot(aimX,aimY));
        int gained=(int)Math.round(player.ichor()-beforeIchor);
        endAbsorption();
        events.add(new Event(EventType.ABSORBED,player.x()+aimX/length*68,player.y()-20+aimY/length*24,false,gained,false,true));
    }
    public boolean blocked() { return paused||story.blocksGameplay()||dialogue!=null||wardenVictory()||wardenReturnTime>0; }
    public boolean paused() { return paused; }
    public void togglePause() { if(!story.blocksGameplay()) {paused=!paused;if(paused)cancelBladeInput();} }
    public void pause() { if(!story.blocksGameplay()) {paused=true;cancelBladeInput();} }
    public int combo() { return combo; }
    public WaterProjectile.Kind waterKind() { return waterKind; }
    public double tideCooldown() { return tideCooldown; }
    public double crescentCooldown() { return crescentCooldown; }
    public double riposteCooldown() { return riposteCooldown; }
    public boolean guarding() { return guardTime>0&&player.alive()&&player.bladeForm(); }
    public double guardProgress() { return guarding()?1-guardTime/RIPOSTE_DURATION:0; }
    public List<Crescent> crescents() { return Collections.unmodifiableList(crescents); }
    public double waterCharge() { return attackDelay>0&&waterCast ? 1-attackDelay/waterWindup : -1; }
    public List<WaterProjectile> projectiles() { return Collections.unmodifiableList(projectiles); }
    public List<EnemyProjectile> hostileProjectiles(){return Collections.unmodifiableList(hostileProjectiles);}
    public List<ForestHazard> forestHazards(){return Collections.unmodifiableList(forestHazards);}
    public boolean visited(int room) { return visited[room]; }
    public boolean cleared(int room) { return cleared[room]; }
    /** A skill or purchase refused for a reason the player can hear: cooldown, cost, unsafe camp. */
    private void deny() {
        if(denyTime>0)return;
        denyTime=DENY_REPEAT;emit(EventType.DENIED,player.x(),player.y());
    }
    private static final double DENY_REPEAT=.3;       // one denied cue per press, not one per queued retry
    private double denyTime;
    public List<Event> drainEvents() { List<Event> copy=List.copyOf(events); events.clear(); return copy; }
    public Player player() { return player; }
    public RuinedOutpostMap map() { return map; }
    public List<Wisp> scouts() { return Collections.unmodifiableList(scouts); }
    public Guardian guardian() { return guardian; }
    public OutpostStory story() { return story; }
    public List<IchorDrop> drops() { return Collections.unmodifiableList(drops); }
    public List<Corpse> corpses() { return Collections.unmodifiableList(corpses); }
    public Corpse absorptionTarget() { return absorptionTarget; }
    public double absorptionProgress() { return Math.min(1,absorptionTime/(beelzebubActive()?BEELZEBUB_DURATION:ABSORB_DURATION)); }
    public boolean beelzebubActive() { return !beelzebubTargets.isEmpty(); }
    public double beelzebubCooldown() { return beelzebubCooldown; }
    public boolean beelzebubTarget(Corpse corpse) { return beelzebubTargets.stream().anyMatch(pull->pull.corpse()==corpse); }
    /** How far the caught remains have been drawn in (0-1): the hand vortex opens before they accelerate into it. */
    public double beelzebubPull() { return beelzebubPull(absorptionProgress()); }
    public static double beelzebubPull(double progress) {
        double fraction=Math.max(0,Math.min(1,(progress-.18)/.7));
        return fraction*fraction*(3-2*fraction);
    }
    /** Where a body caught by Beelzebub lay when the cast began (its drag is drawn from there); null if not caught. */
    public double[] beelzebubOrigin(Corpse corpse) {
        for(var pull:beelzebubTargets)if(pull.corpse()==corpse)return new double[]{pull.x(),pull.y()};
        return null;
    }
    public double absorptionAnchorX() { return absorptionTarget==null?player.x():corpseStartX; }
    public double absorptionAnchorY() { return absorptionTarget==null?player.y():corpseStartY; }
}
