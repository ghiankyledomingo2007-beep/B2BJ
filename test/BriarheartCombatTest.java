import java.nio.file.Files;

public final class BriarheartCombatTest {
    public static void main(String[] args) throws Exception {
        committedPatternsAndOpening();
        phaseChangesBetweenPatterns();
        gameHazardsAndCleanup();
        System.out.println("BriarheartCombatTest passed: rooted patterns, shared geometry, escape gap, exposure, phase, hazards and cleanup");
    }
    private static void committedPatternsAndOpening() {
        var boss=new Guardian(900,500,Guardian.Profile.BRIARHEART);boss.activate(500,500);
        for(int round=0;round<2;round++)for(var attack:Guardian.ForestAttack.values()) {
            assert boss.state()==Guardian.State.TELEGRAPH&&boss.forestAttack()==attack;
            var roots=boss.forestRoots();var seeds=boss.forestSeeds();
            double tx=boss.targetX(),ty=boss.targetY();
            assert !boss.hits(tx,ty)&&!boss.coreExposed();
            boss.update(.5,1000,900);
            assert roots.equals(boss.forestRoots())&&seeds.equals(boss.forestSeeds());
            assert boss.targetX()==tx&&boss.targetY()==ty : "all tells stay committed";
            advance(boss,Guardian.State.SLAM);
            assert boss.x()==900&&boss.y()==500 : "Briarheart stays rooted";
            for(var root:roots) {
                assert boss.hits(root.centerX(),root.centerY());
                boolean across=root.width()>root.height();
                double safeX=root.centerX()+(across?0:root.width()/2+8);
                double safeY=root.centerY()+(across?root.height()/2+8:0);
                if(attack==Guardian.ForestAttack.ROOT_WEAVE)assert !boss.hits(safeX,safeY);
            }
            if(attack==Guardian.ForestAttack.SEED_BLOOM) {
                assert seeds.size()==3&&roots.isEmpty();
                assert !boss.hits(tx,ty) : "seed damage belongs only to released hazards";
            }
            if(attack==Guardian.ForestAttack.BRAMBLE_CAGE) {
                assert roots.size()==3;
                int side=boss.forestGap();double dx=side==1?1:side==3?-1:0,dy=side==0?-1:side==2?1:0;
                assert !boss.hits(tx,ty) : "central eruption has an additional warning";
                boss.update(.91,tx,ty);assert boss.hits(tx,ty);
                for(int d=132;d<=260;d+=4)assert !boss.hits(tx+dx*d,ty+dy*d) : "gap leads completely outside the cage";
            }
            advance(boss,Guardian.State.RECOVER);
            assert boss.forestRoots().isEmpty()&&boss.forestSeeds().isEmpty();
            assert boss.coreExposed()==(attack==Guardian.ForestAttack.BRAMBLE_CAGE);
            if(boss.coreExposed())assert boss.recoverDuration()>=3.5 : "time to reach and punish the heart";
            advance(boss,Guardian.State.TELEGRAPH);
        }
        boss.hurt(boss.maxHealth());
        assert !boss.hits(boss.targetX(),boss.targetY())&&boss.forestRoots().isEmpty()&&boss.forestSeeds().isEmpty();
        var tells=new Telegraphs(400,250);tells.begin();tells.root(200,125,325,35,1,true);
        var image=tells.image();
        for(int y=0;y<250;y++)for(int x=0;x<400;x++)if(image.getRGB(x,y)>>>24!=0)
            assert Math.abs(x-200)<=162.5&&Math.abs(y-125)<=17.5 : "root pixels never claim a larger damage area";
    }
    private static void phaseChangesBetweenPatterns() {
        var boss=new Guardian(900,500,Guardian.Profile.BRIARHEART);boss.activate(500,500);
        var committed=boss.forestRoots();boss.hurt(boss.maxHealth()/2);
        assert !boss.enraged()&&committed.equals(boss.forestRoots()) : "HP change cannot expand a committed tell";
        advance(boss,Guardian.State.ENRAGE);
        assert !boss.hits(500,500)&&!boss.enraged();
        boss.update(1.7,500,500);assert boss.state()==Guardian.State.ENRAGE;
        advance(boss,Guardian.State.TELEGRAPH);
        assert boss.enraged()&&boss.forestAttack()==Guardian.ForestAttack.SEED_BLOOM&&boss.forestSeeds().size()==5;
        advance(boss,Guardian.State.RECOVER);advance(boss,Guardian.State.TELEGRAPH);
        advance(boss,Guardian.State.RECOVER);advance(boss,Guardian.State.TELEGRAPH);
        assert boss.forestAttack()==Guardian.ForestAttack.ROOT_WEAVE&&boss.forestRoots().size()==4;
    }
    private static void gameHazardsAndCleanup() throws Exception {
        var save=Files.createTempDirectory("b2bj-briar-").resolve("save.properties");
        var game=RuinedOutpostGame.campaign(save);game.begin();game.skipDialogue();assert game.debugWarp(1,true);
        for(var enemy:game.scouts())enemy.hurt(10000);
        game.skipDialogue();int population=game.scouts().size();
        var boss=game.guardian();boss.activate(game.player().x(),game.player().y());
        for(int i=0;i<1600&&game.forestHazards().isEmpty();i++) {game.player().heal();game.update(.01,0,0);}
        assert game.forestHazards().size()==3 : "actual boss releases all committed seeds";
        var hazard=game.forestHazards().get(0);double age=hazard.age();
        game.pause();game.update(.5,0,0);assert hazard.age()==age;game.togglePause();
        game.player().relocate(hazard.x(),hazard.y());game.player().heal();int before=game.player().health();
        for(int i=0;i<200;i++)game.update(.01,0,0);
        assert game.player().health()<before : "standing in a released seed hurts";
        for(int i=0;i<1600;i++){game.player().heal();game.update(.01,0,0);}
        assert game.scouts().size()==population : "Briarheart does not recycle Warden minion waves";
        boss.hurt(boss.maxHealth());var defeat=RuinedOutpostGame.class.getDeclaredMethod("afterGuardianHit",boolean.class);
        defeat.setAccessible(true);defeat.invoke(game,false);
        assert game.forestHazards().isEmpty() : "victory clears lingering danger";
        assert game.debugWarp(0,false)&&game.forestHazards().isEmpty();
    }
    private static void advance(Guardian boss,Guardian.State target) {
        for(int i=0;i<1500&&boss.state()!=target;i++)boss.update(.01,500,500);
        assert boss.state()==target : "failed to reach "+target;
    }
}
