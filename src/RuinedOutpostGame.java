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
        DENIED, CHECKPOINT, UPGRADE, DAMAGE_DEALT, DAMAGE_TAKEN, ARMOR_BLOCK
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
    public static final double CHARGE_THRESHOLD=.18, FULL_CHARGE=.8, CHARGE_COST=10, INPUT_BUFFER=.12;
    private boolean primaryHeld, charging, chargeReady, comboDodged;
    private double primaryTime, queuedTime, bladeTime, bladeDuration, releasedCharge, dashOpener, bladeCastTime;
    private int queuedAttack,queuedX,queuedY,bladeMove;

    private double passageCooldown;
    private double tideCooldown;
    private double counterWindow, dashX, dashY, waterWindup, counterTime;
    private double crescentCooldown,riposteCooldown,guardTime;
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
        bark=null;pendingBossBark=null;barkIndex=0;barkTime=barkDuration=barkCooldown=identificationCooldown=0;
        banterCooldown=12;quietTime=exploredDistance=pendingBanterTime=bossBarkCooldown=0;pendingBanter=null;lastDefeatedKind=null;
        java.util.Arrays.fill(observedBossAttacks,0);
        java.util.Arrays.fill(alternateBossWarnings,false);
        hurtReactionTime=hurtReactionCooldown=healingQuietTime=healingLastHealth=0;hurtReactions=0;passiveHealing=false;
        wardenRevealTime=wardenReturnTime=wardenReturnWeight=0;
        biome=destination;map=RuinedOutpostMap.campaign(biome);
        campaignCheckpoint=arrival==1||restIndex(arrival)>=0?arrival:0;
        carried=null;dropped.clear();syncFieldGates();
        scouts=new ArrayList<>();campaignEnemies.clear();forestAttackPhases.clear();
        drops.clear();corpses.clear();cancelAbsorption();events.clear();
        lungeHits.clear();dashHits.clear();bossAdds.clear();projectiles.clear();crescents.clear();hostileProjectiles.clear();forestHazards.clear();
        tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=attackDelay=comboTime=hitStop=addTimer=counterWindow=0;
        cancelBladeInput();combo=addSpawnIndex=0;guardianHitImpact=-1;passageCooldown=.35;
        armorNoticeTime=0;
        for(var spawn:campaignArea().enemies()) {
            CampaignEnemy enemy=new CampaignEnemy(spawn.kind(),spawn.x(),spawn.y());
            scouts.add(enemy.body());campaignEnemies.put(enemy.body(),enemy);
        }
        guardian=new Guardian(map.guardianX(),map.guardianY(),Guardian.Profile.values()[biome]);
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
        saveProgress();cancelAbsorption();dialogue=null;wardenReturnTime=0;story=new OutpostStory();paused=false;
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
                cancelAbsorption();enterBiome(biome+1,0);notice(campaignArea().name());saveProgress();return true;
            }
            // the campaign story never passes the gatehouse phases on its own; the last exit is where it ends
            story.enterGatehouse();story.guardianDefeated();story.escaped();
            cancelAbsorption();notice(campaignArea().name()+" CLEARED");saveProgress();
            emit(EventType.VICTORY,player.x(),player.y());return true;
        }
        if(biome>0&&near(campaignArea().returnPortal().x(),campaignArea().returnPortal().y(),120)) {
            story.resume();enterBiome(biome-1,2);notice("RETURNED TO "+campaignArea().name());saveProgress();return true;
        }
        return false;
    }

    private double openingLineTime;
    public boolean opening() { return dialogue==OutpostDialogue.WAKE; }
    public double openingLineTime() { return openingLineTime; }
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
        barkTime=Math.max(0,barkTime-seconds);
        if(bark!=null&&barkTime==0) {
            if(++barkIndex<bark.lines().size())barkTime=barkDuration=bark.lineSeconds();
            else {
                if(bark.banter()) {campaignUnlocks.add(bark.key());saveProgress();}
                bark=null;
            }
        }
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
            if(description==null||campaignUnlocks.contains(description.key())||!body.alive())continue;
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
        if(next.banter())banterPlayedAt.put(next,sageTime);else campaignUnlocks.add(next.key());
        exploredDistance=0;
        if(next.trigger()==OutpostBark.Trigger.STORY)banterCooldown=Math.max(banterCooldown,18);
        else if(next.banter()) {
            banterCooldown=24+Math.random()*10;
            pendingBanter=null;pendingBanterTime=0;
        }
        if(next.combat()&&next.trigger()!=OutpostBark.Trigger.IDENTIFY)bossBarkCooldown=15;
        if(next==OutpostBark.HEARD_SURVIVOR)campaignUnlocks.add("found_survivor_0");
        if(!next.banter())saveProgress();
    }
    private void bossBark(OutpostBark next) {
        if(campaignMode&&biome==next.biome()&&player.alive()&&dialogue==null&&!campaignUnlocks.contains(next.key())) {
            if(bark==null)startBark(next);else pendingBossBark=next;
        }
    }
    public boolean bossIntroduction() { return dialogue==OutpostDialogue.WARDEN||dialogue==OutpostDialogue.FOREST_BRIARHEART; }
    public boolean cutsceneActive() { return bossIntroduction()||wardenReturnTime>0; }
    public boolean dialogueReady() { return !bossIntroduction()||wardenRevealTime>=1.35; }
    public double storyCameraWeight() {
        double t=bossIntroduction()?Math.min(1,wardenRevealTime/1.1):wardenReturnTime/.6;
        return t*t*(3-2*t)*(bossIntroduction()?1:wardenReturnWeight);
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
        cancelAbsorption();cancelBladeInput();
        bark=null;pendingBossBark=null;barkTime=0;
        hurtReactionTime=0;
        pendingBanter=null;pendingBanterTime=quietTime=exploredDistance=0;
        banterCooldown=12;
        dialogue=scene;dialogueIndex=0;openingLineTime=0;
        if(bossIntroduction()) {
            wardenRevealTime=0;
            // Arrival movement and queued casts must not resume after the camera returns.
            player.relocate(player.x(),player.y());attackDelay=waterWindup=0;waterCast=false;
        }
    }

    public boolean advanceDialogue() {
        if(dialogue==null||paused||!dialogueReady())return false;
        openingLineTime=0;
        if(++dialogueIndex>=dialogue.lines().size())skipDialogue();
        return true;
    }

    public boolean skipDialogue() {
        if(dialogue==null||paused)return false;
        if(bossIntroduction()) {
            wardenReturnWeight=storyCameraWeight();wardenReturnTime=wardenReturnWeight>0?.6:0;
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
        debugSession=true;player.refillForTesting();cancelAbsorption();
        tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=attackDelay=comboTime=counterWindow=hitStop=0;
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
            tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=0;
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
        tideCooldown=crescentCooldown=riposteCooldown=beelzebubCooldown=guardTime=0;
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
        hostileProjectiles.clear();forestHazards.clear();
        attackDelay = comboTime = hitStop = addTimer = counterWindow = 0;
        cancelBladeInput();combo = 0;
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
            openingLineTime+=elapsed;
            if(openingLineTime>=OutpostDialogue.openingSeconds(dialogueIndex))advanceDialogue();
            return;
        }
        if(map.gateOpen()&&(gateTime>0||!campaignMode||biome!=0
                ||near(campaignArea().exit().x(),campaignArea().exit().y(),320)))gateTime=Math.min(1.4,gateTime+elapsed);
        if(cutsceneActive()) {
            if(bossIntroduction())wardenRevealTime+=elapsed;
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
        if(campaignMode&&noticeTime>0&&!campaignNotice.startsWith("SAVE FAILED")) {
            noticeTime=Math.max(0,noticeTime-seconds);
            if(noticeTime==0)campaignNotice="";
        }
        if (!player.alive()) { projectiles.clear(); crescents.clear(); guardTime=0; hostileProjectiles.clear();forestHazards.clear(); attackDelay=0; cancelBladeInput(); cancelAbsorption(); story.playerDied(); return; }
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
        boolean planted=player.bladeForm()&&(bladePreparing()||bladeTime>0||bladeCastTime>0||guarding()||counterTime>0);
        counterTime=player.bladeForm()?Math.max(0,counterTime-seconds):0;
        double previousX=player.x(),previousY=player.y();
        player.move(horizontal,vertical,seconds,map,planted?0:1);
        bladeCastTime=Math.max(0,bladeCastTime-seconds);
        if(wasBlade&&!player.bladeForm())cancelBladeInput();
        if(player.bladeForm())updateBladeInput(seconds);
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
        updateCrescents(seconds);
        if (wasDashing || player.dashing()) damageEnemiesFromDash();
        // Resolve outgoing impacts before incoming contact/release: interrupted attacks cannot trade.
        boolean dashProtected=wasDashing&&!player.recovering();
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
            if(!dashProtected)hurtPlayer(guardian.profile()==Guardian.Profile.BRIARHEART?22:Guardian.SLAM_DAMAGE,guardian.targetX(),guardian.targetY());
        }
        updateHostileProjectiles(seconds,dashProtected);
        for(var hazard:forestHazards) {
            hazard.update(seconds);
            if(!dashProtected&&hazard.hit(player.x(),player.y()))hurtPlayer(9,hazard.x(),hazard.y());
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
        if (!player.alive()) { projectiles.clear(); crescents.clear(); guardTime=0; hostileProjectiles.clear();forestHazards.clear(); attackDelay=0; cancelBladeInput(); cancelAbsorption(); story.playerDied(); }
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
        if(guardian.profile()==Guardian.Profile.BRIARHEART)return;
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
    private boolean near(double x,double y,double radius) {
        return Math.hypot(player.x()-x,player.y()-y)<=radius;
    }
    private void emit(EventType type,double x,double y) { events.add(new Event(type,x,y)); }
    private void hurtPlayer(int damage,double x,double y) {
        if(damage>0&&guarding()&&!player.invulnerable()) {
            guardTime=0;
            // He answers toward whoever hit him. The cut is only shown; the damage below is the riposte's own.
            aimX=(int)Math.round((x-player.x())*1000);aimY=(int)Math.round((y-player.y())*1000);
            if(aimX==0&&aimY==0)aimY=1;
            counterTime=COUNTER_DURATION;player.attackRecovery(COUNTER_DURATION*.6);
            emit(EventType.RIPOSTE_COUNTER,player.x(),player.y());
            for(Wisp scout:scouts)if(scout.alive()&&near(scout.x(),scout.y(),RIPOSTE_RANGE)
                    &&map.clearLine(player.x(),player.y(),scout.x(),scout.y())&&hurtScout(scout,36,false,true)) {
                double dx=scout.x()-player.x(),dy=scout.y()-player.y(),length=Math.max(1,Math.hypot(dx,dy));
                scout.push(dx/length*300,dy/length*300);afterScoutHit(scout);
            }
            if(bossActive()&&!bossDefeated
                    &&near(guardian.x(),guardian.y(),RIPOSTE_RANGE)
                    &&map.clearLine(player.x(),player.y(),guardian.x(),guardian.y())&&hurtGuardian(36,false))
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
            cancelAbsorption();cancelBladeInput();
            emit(EventType.PLAYER_HIT,x,y);
            hitStop=0.1;                         // getting hurt lands hardest: six frames of stillness
            if (blade&&!player.bladeForm()) emit(EventType.REVERT,player.x(),player.y());
        }
    }
    public boolean dash(int horizontal,int vertical) {
        if (blocked() || (player.bladeForm()&&attackDelay>0) || !player.dash(horizontal,vertical)) return false;
        boolean preserve=player.bladeForm()&&comboTime>0&&combo<3&&!comboDodged;
        clearBladeButtons();bladeTime=bladeCastTime=counterTime=0;
        if(player.bladeForm())player.attackRecovery(0);
        cancelAbsorption();
        guardTime=0;
        attackDelay=0;
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
        if (blocked() || guarding() || charging || (facingX==0&&facingY==0) || !player.startAttack()) return false;
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
            waterWindup=counter?.04:combo==2?.12:.08;
            if(counter) {combo=0;comboTime=0;}
        }
        counterWindow=0;
        int move=opener?DASH_SLASH:combo;
        attackDelay=waterCast?waterWindup:strikeWindup(move);
        if(!waterCast) {
            bladeMove=move;bladeDuration=bladeTime=strikeDuration(move);
            player.attackRecovery(bladeDuration);comboTime=bladeDuration+.5;
            dashOpener=0;releasedCharge=0;
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
    /** Every cut is made with a step: world pixels he advances during the wind-up, landing on contact. */
    static double strikeStep(int move) { return switch(move) {case 1->30;case 3->20;case 4,SPIN_CLEAVE->26;case DASH_SLASH->40;default->16;}; }
    /**
     * The move whose HIT RULES a variation uses (width, single target, knockback, hit pause). The dash slash
     * hits like the wide draw cut, not the narrow thrust it replaced; the spin is a cleave.
     */
    static int strikeKind(int move) { return move==DASH_SLASH?0:move==SPIN_CLEAVE?4:move; }
    public boolean primaryPress(int x,int y) {
        if(blocked()||!player.bladeForm()||!player.alive()||player.recovering()||guarding()||primaryHeld||queuedAttack!=0||(x==0&&y==0))return false;
        if(player.attackCooldown()>INPUT_BUFFER||player.dashSeconds()>INPUT_BUFFER)return false;
        cancelAbsorption();
        primaryHeld=true;primaryTime=0;chargeReady=false;return true;
    }
    public void primaryRelease(int x,int y) {
        if(!primaryHeld)return;
        boolean charged=charging;double held=primaryTime;
        primaryHeld=charging=chargeReady=false;primaryTime=0;
        if(blocked()||!player.bladeForm()||!player.alive())return;
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
    }
    public boolean requestBladeAttack(boolean crescent,int x,int y) {
        if(blocked()||!player.bladeForm()||guarding()||charging||(x==0&&y==0))return false;
        if(crescent?ichorCrescent(x,y):attack(x,y))return true;
        if(queuedAttack==0||crescent) {
            if(player.attackCooldown()<=INPUT_BUFFER+1e-9&&player.dashSeconds()<=INPUT_BUFFER+1e-9) {
                queuedAttack=crescent?2:1;queuedX=x;queuedY=y;queuedTime=INPUT_BUFFER+.01;
                return true;
            }
        }
        return false;
    }
    private void updateBladeInput(double seconds) {
        if(!player.alive()||player.recovering()){cancelBladeInput();return;}
        if(primaryHeld) {
            primaryTime+=seconds;
            if(primaryTime>=CHARGE_THRESHOLD&&!player.dashing()&&player.attackCooldown()==0&&!guarding())charging=true;
            if(charging&&primaryTime>=FULL_CHARGE&&!chargeReady) {
                chargeReady=true;emit(EventType.CHARGE_READY,player.x(),player.y());
            }
        }
        if(queuedAttack!=0) {
            boolean started=queuedAttack==2?ichorCrescent(queuedX,queuedY):attack(queuedX,queuedY);
            queuedTime-=seconds;
            if(started||queuedTime<=0)queuedAttack=0;
        }
    }
    private boolean chargedCleave(int x,int y,double power) {
        if((x==0&&y==0)||guarding()||player.ichor()<=CHARGE_COST||!player.startAttack())return false;
        player.spendBladeIchor(CHARGE_COST);cancelAbsorption();
        // Held to the charge-ready cue, the cut carries him right round; anything less is the plain cleave.
        aimX=x;aimY=y;releasedCharge=power;bladeMove=power>=1?SPIN_CLEAVE:4;counterTime=0;
        bladeDuration=bladeTime=strikeDuration(bladeMove);player.attackRecovery(bladeDuration);
        strikeDamage=player.attackDamage()+(int)Math.round(36*power);strikeRange=BLADE_ATTACK_RANGE;
        waterCast=heavyCast=false;attackDelay=strikeWindup(bladeMove);comboTime=dashOpener=0;combo=0;
        emit(EventType.ATTACK,player.x(),player.y());return true;
    }
    private void clearBladeButtons() {primaryHeld=charging=chargeReady=false;primaryTime=queuedTime=0;queuedAttack=0;}
    public void cancelBladeInput() {
        clearBladeButtons();bladeTime=bladeCastTime=dashOpener=counterTime=0;comboDodged=false;
        if(!waterCast||player.bladeForm()){comboTime=0;attackDelay=0;}
    }
    public boolean bladePreparing() {return primaryHeld&&!player.dashing()&&!guarding()&&player.attackCooldown()==0;}
    public boolean charging() {return charging;}
    public double chargeProgress() {return Math.min(1,primaryTime/FULL_CHARGE);}
    public double chargeSeconds() {return primaryTime;}
    public boolean bladeCasting() {return player.bladeForm()&&bladeCastTime>0;}
    public boolean bladeCountering() {return player.bladeForm()&&counterTime>0;}
    public double counterElapsed() {return COUNTER_DURATION-counterTime;}
    public double bladeCastElapsed() {return BLADE_CAST_DURATION-bladeCastTime;}
    public double bladeElapsed() {return bladeDuration-bladeTime;}
    public double bladeContact() {return strikeWindup(bladeMove);}
    public boolean bladeAttacking() {return bladeTime>0&&player.bladeForm();}
    public int bladeMove() {return bladeMove;}
    public int bladeAimX() {return aimX;}
    public int bladeAimY() {return aimY;}
    public int dashAimX() {return (int)Math.round(dashX*1000);}
    public int dashAimY() {return (int)Math.round(dashY*1000);}
    public double releasedCharge() {return releasedCharge;}
    public int comboStep() {return comboTime>0?combo+1:0;}

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
        if(blocked()||!player.bladeForm()||!player.alive()||player.recovering()||player.dashing()
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
        emit(EventType.BEELZEBUB_CAST,player.x(),player.y());return true;
    }
    public boolean riposte() {
        if(!blocked()&&player.bladeForm()&&(riposteCooldown>0||player.ichor()<=BLADE_SKILL_COST))deny();
        if(blocked()||!player.bladeForm()||riposteCooldown>0||player.ichor()<=BLADE_SKILL_COST
                ||!player.startAttack())return false;
        player.spendBladeIchor(BLADE_SKILL_COST);
        cancelAbsorption();cancelBladeInput();
        guardTime=RIPOSTE_DURATION;riposteCooldown=RIPOSTE_COOLDOWN;
        emit(EventType.RIPOSTE_START,player.x(),player.y());return true;
    }
    public boolean tideWave(int facingX,int facingY) {
        if(!blocked()&&!player.bladeForm()&&tideCooldown>0)deny();
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
                else if(waterKind==WaterProjectile.Kind.FINISHER)emit(EventType.FINISHER_RELEASE,player.x(),player.y());
            }
            return;
        }
        if(!player.bladeForm()) return;
        Wisp nearest=null;
        for(Wisp scout:scouts)if(scout.alive()&&bladeHits(scout.x(),scout.y())
                &&(nearest==null||Math.hypot(scout.x()-player.x(),scout.y()-player.y())
                    <Math.hypot(nearest.x()-player.x(),nearest.y()-player.y())))nearest=scout;
        boolean bossHit=bossActive()&&!bossDefeated&&bladeHits(guardian.x(),guardian.y());
        boolean bossFirst=bossHit&&(nearest==null||Math.hypot(guardian.x()-player.x(),guardian.y()-player.y())
                <Math.hypot(nearest.x()-player.x(),nearest.y()-player.y()));
        int kind=strikeKind(bladeMove);
        for(Wisp scout:scouts) {
            if(!scout.alive()||!bladeHits(scout.x(),scout.y())||(kind==1&&(scout!=nearest||bossFirst)))continue;
            if(hurtScout(scout,strikeDamage,false,true)) {
                double length=Math.max(1,Math.hypot(aimX,aimY));
                double force=kind==4?220+100*releasedCharge:kind==3?260:kind==1?110:55;
                scout.push(aimX/length*force,aimY/length*force);afterScoutHit(scout);
                if(kind>=3)hitStop=.1;
            }
        }
        if(bossHit&&(kind!=1||bossFirst)&&hurtGuardian(strikeDamage,false))afterGuardianHit(false);
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
        if (!guardian.alive()) {
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
                events.add(new Event(EventType.ENEMY_DEFEATED,guardian.x(),guardian.y(),water));
                bossBark(biome==1?OutpostBark.FOREST_BOSS_VICTORY:OutpostBark.WARDEN_VICTORY);
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
                            if(wave.kind()==WaterProjectile.Kind.FINISHER)hitStop=.1;
                            if(wave.heavy())emit(EventType.TIDE_IMPACT,scout.x(),scout.y());
                        }
                        // Bodies part around the wave; a raised shield or solid terrain stops it.
                        if(!wave.heavy()||!damaged) {wave.stop();break;}
                    }
                }
                if(!wave.alive()) events.add(new Event(wave.heavy()?EventType.TIDE_IMPACT:
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
        cancelAbsorption();
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
        return player.alive()&&!player.bladeForm()&&!player.recovering()&&!player.dashing()&&attackDelay==0;
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
    private void cancelAbsorption() { absorptionTarget=null;absorptionTime=0;beelzebubTargets.clear(); }
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
        player.collectIchor(5);
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
        if(!player.alive()||!player.bladeForm()||player.dashing()||player.recovering()
                ||Math.hypot(player.x()-absorptionX,player.y()-absorptionY)>.01
                ||player.healthValue()<absorptionHealth) { cancelAbsorption();return; }
        beelzebubTargets.removeIf(pull->!corpses.contains(pull.corpse())
                ||!corpseReachable(pull.corpse(),BEELZEBUB_RADIUS));
        if(beelzebubTargets.isEmpty()) { cancelAbsorption();return; }
        absorptionTarget=beelzebubTargets.get(0).corpse();
        absorptionTime+=seconds;
        double fraction=absorptionProgress(),pull=fraction*fraction*(3-2*fraction);
        for(var target:beelzebubTargets) {
            target.corpse().x=target.x()+(absorptionX-target.x())*pull;
            target.corpse().y=target.y()+(absorptionY-target.y())*pull;
        }
        if(absorptionTime+1e-9<BEELZEBUB_DURATION)return;
        double beforeHealth=player.healthValue();
        player.collectIchor(5*beelzebubTargets.size());
        for(var target:beelzebubTargets) {
            if(target.corpse().healing())player.heal(10);
            corpses.remove(target.corpse());
        }
        // Blade form takes the resources; temporary absorbed traits belong to slime form.
        if(player.healthValue()>beforeHealth)emit(EventType.HEALED,player.x(),player.y());
        cancelAbsorption();emit(EventType.ABSORBED,player.x(),player.y());
    }
    public boolean blocked() { return paused||story.blocksGameplay()||dialogue!=null||wardenReturnTime>0; }
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
    public double absorptionAnchorX() { return absorptionTarget==null?player.x():corpseStartX; }
    public double absorptionAnchorY() { return absorptionTarget==null?player.y():corpseStartY; }
}
