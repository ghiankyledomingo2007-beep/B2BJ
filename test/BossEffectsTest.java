import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Boss attack effects and sounds follow the real attack: one windup and one release each, hit or miss; nothing
 * for a dead attack; frozen by pause; cleared by resets; capped; thinner but still truthful with reduced effects;
 * and never drawn beyond the damage the model deals. `java BossEffectsTest <dir>` also writes review frames.
 */
public final class BossEffectsTest {
    private static final double FRAME = 1 / 60.0;

    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        cuesFollowTheAttack();
        oneWindupPerCommittedAttack(Guardian.Profile.OUTPOST_WARDEN);
        oneWindupPerCommittedAttack(Guardian.Profile.BRIARHEART);
        deadAttacksLandNothing();
        marksStayInsideTheDamage();
        reducedEffectsKeepTheThreat();
        effectsAreFiniteAndCapped();
        mixerHearsEachAttackOnce();
        pauseFreezesAndResetsClear();
        if (args.length > 0) renders(Path.of(args[0]));
        System.out.println("BossEffectsTest passed: cue per attack, one windup per tell, death, damage bounds, reduced effects, cap, mixer dispatch, pause and resets");
    }

    // ------------------------------------------------------------------ model-level

    private static void cuesFollowTheAttack() throws Exception {
        for (var attack : Guardian.Attack.values()) {
            var boss = warden(attack);
            var expected = switch (attack) {
                case SWEEP -> GameAudio.Cue.WARDEN_SWEEP;
                case CHARGE -> GameAudio.Cue.WARDEN_RUSH;
                case FISSURE -> GameAudio.Cue.WARDEN_FRACTURE;
                case TARGET, SHOCKWAVE -> GameAudio.Cue.GUARDIAN_SLAM;
            };
            assert BossFx.releaseCue(boss) == expected : attack + " lands with " + expected;
            assert BossFx.windupCue(boss) == GameAudio.Cue.WARDEN_WINDUP;
        }
        for (var attack : Guardian.ForestAttack.values()) {
            var boss = briar(attack);
            var expected = switch (attack) {
                case ROOT_WEAVE -> GameAudio.Cue.BRIAR_ROOTS;
                case SEED_BLOOM -> GameAudio.Cue.BRIAR_SEEDS;
                case BRAMBLE_CAGE -> GameAudio.Cue.BRIAR_CAGE;
            };
            assert BossFx.releaseCue(boss) == expected : attack + " lands with " + expected;
            assert BossFx.windupCue(boss) == GameAudio.Cue.BRIAR_WINDUP;
        }
        for (var profile : new Guardian.Profile[]{Guardian.Profile.OATHKEEPER, Guardian.Profile.ICHOR_GOLEM}) {
            var boss = new Guardian(900, 500, profile);
            boss.activate(700, 560);
            var fx = new BossFx();
            assert BossFx.windupCue(boss) == null && BossFx.releaseCue(boss) == GameAudio.Cue.GUARDIAN_SLAM;
            for (int i = 0; i < 300; i++) { boss.update(FRAME, 700, 560); assert fx.update(FRAME, boss, false) == null; fx.release(boss, false); }
            assert fx.particles() == 0 : profile + " keeps its own presentation";
        }
    }

    private static void oneWindupPerCommittedAttack(Guardian.Profile profile) {
        var boss = new Guardian(900, 500, profile);
        var fx = new BossFx();
        boss.activate(700, 560);
        int windups = 0, heardFor = -1;
        var heard = new ArrayList<Object>();
        double px = 700, py = 560;
        for (int frame = 0; frame < 60 * 30; frame++) {
            if (frame % 240 == 0) { boolean near = frame % 480 == 0; px = near ? 760 : 1260; py = near ? 560 : 380; }   // sweeps, rushes and slams
            boss.update(FRAME, px, py);
            var cue = fx.update(FRAME, boss, false);
            if (cue == null) continue;
            windups++;
            double left = boss.telegraphDuration() - boss.stateSeconds();
            assert cue == BossFx.windupCue(boss);
            assert boss.state() == Guardian.State.TELEGRAPH : "windups belong to a committed tell";
            assert left <= BossFx.WINDUP_LEAD + 1e-6 && left > BossFx.WINDUP_LEAD - FRAME - 1e-6 : "heard WINDUP_LEAD before release, once: " + left;
            assert heardFor != boss.impactNumber() : "one windup per attack";
            heardFor = boss.impactNumber();
            heard.add(profile == Guardian.Profile.BRIARHEART ? boss.forestAttack() : boss.attack());
        }
        boolean pending = boss.state() == Guardian.State.TELEGRAPH && heardFor == boss.impactNumber();
        assert windups == boss.impactNumber() + (pending ? 1 : 0) : "every released attack was wound up exactly once: " + windups + " vs " + boss.impactNumber();
        assert boss.impactNumber() >= 6 : "test needs a real run of attacks: " + boss.impactNumber();
        assert new java.util.HashSet<>(heard).size() >= 3 : "several attack kinds were heard: " + heard;
    }

    private static void deadAttacksLandNothing() throws Exception {
        for (var boss : new Guardian[]{warden(Guardian.Attack.TARGET), briar(Guardian.ForestAttack.ROOT_WEAVE)}) {
            // Killed before the windup: no windup, and no impact effects for the attack that never landed.
            var fx = new BossFx();
            while (boss.telegraphDuration() - boss.stateSeconds() > BossFx.WINDUP_LEAD + .1) { boss.update(FRAME, 700, 560); assert fx.update(FRAME, boss, false) == null; }
            boss.hurt(boss.health());
            for (int i = 0; i < 120; i++) { boss.update(FRAME, 700, 560); assert fx.update(FRAME, boss, false) == null : "a dead boss winds nothing up"; fx.release(boss, false); }
            assert fx.particles() == 0 : "a dead attack lands nothing";
        }
        for (var boss : new Guardian[]{warden(Guardian.Attack.SWEEP), briar(Guardian.ForestAttack.SEED_BLOOM)}) {
            // Killed after its windup was heard: the anticipation goes with it, and nothing lands.
            var fx = new BossFx();
            boolean wound = false;
            for (int i = 0; i < 200 && !wound; i++) { boss.update(FRAME, 700, 560); wound = fx.update(FRAME, boss, false) != null; }
            assert wound;
            boss.hurt(boss.health());
            fx.update(FRAME, boss, false);
            fx.release(boss, false);
            assert fx.particles() == 0 : "death clears the tell and lands no blow";
        }
    }

    private static void marksStayInsideTheDamage() throws Exception {
        var target = landed(warden(Guardian.Attack.TARGET), false);
        follow(target, (fx, boss) -> {
            for (var b : bits(fx, "CHIP", "DUST"))
                assert Math.hypot(b[0] - boss.targetX(), b[1] - boss.targetY() - 24) <= boss.slamRadius() : "slam debris stays inside the slam";
        });
        var wave = landed(warden(Guardian.Attack.SHOCKWAVE), false);
        follow(wave, (fx, boss) -> {
            if (boss.state() != Guardian.State.SLAM) return;
            for (var b : bits(fx, "RIDE"))
                assert Math.abs(Math.hypot(b[0] - boss.targetX(), b[1] - boss.targetY()) - boss.waveRadius()) <= 12 : "wave dust rides the damage front";
        });
        var sweep = landed(warden(Guardian.Attack.SWEEP), false);
        follow(sweep, (fx, boss) -> {
            for (var b : bits(fx, "SWEEP")) assert b[3] <= boss.slamRadius() - 10 : "the blade arc stays inside the sweep";
            for (var b : bits(fx, "CHIP")) assert Math.hypot(b[0] - boss.x(), b[1] - boss.y() - 30) <= boss.slamRadius() : "sweep grit stays inside the sweep";
        });
        var rush = landed(warden(Guardian.Attack.CHARGE), false);
        follow(rush, (fx, boss) -> {
            if (boss.state() != Guardian.State.SLAM) return;
            for (var b : bits(fx, "WAKE")) if (b[2] <= FRAME + 1e-9)
                assert Math.hypot(b[0] - boss.x(), b[1] - boss.y()) <= Guardian.CHARGE_RADIUS : "the wake starts under the charging body";
        });
        var fissure = landed(warden(Guardian.Attack.FISSURE), false);
        var lane = fissure.boss.fissureBounds();
        advance(fissure, 15, false);
        var image = draw(fissure, false);
        int painted = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) if (image.getRGB(x, y) == 0xff17151a) {
            painted++;
            assert Math.abs(x - lane.centerX()) <= lane.width() / 2 + 2 && Math.abs(y - lane.centerY()) <= lane.height() / 2 + 2
                    : "the ground split never leaves the fissure lane";
        }
        assert painted > 500 : "the fissure leaves a visible split: " + painted;
        follow(fissure, (fx, boss) -> {
            for (var b : bits(fx, "CHIP"))
                assert Math.abs(b[0] - lane.centerX()) <= lane.width() / 2 && Math.abs(b[1] - lane.centerY()) <= lane.height() / 2 : "split fragments stay in the lane";
        });

        var seeds = landed(briar(Guardian.ForestAttack.SEED_BLOOM), false);
        var blooms = seeds.boss.forestSeeds();
        follow(seeds, (fx, boss) -> {
            for (var b : bits(fx, "SEED", "SPLINTER")) {
                double nearest = Double.MAX_VALUE;
                for (var bloom : blooms) nearest = Math.min(nearest, Math.hypot(b[0] - bloom.x(), b[1] - bloom.y() - Player.COLLISION_Y_OFFSET));
                assert nearest <= blooms.get(0).radius() : "seed husks land inside their bloom";
            }
        });
        var roots = landed(briar(Guardian.ForestAttack.ROOT_WEAVE), false);
        var lanes = roots.boss.forestRoots();
        follow(roots, (fx, boss) -> {
            for (var b : bits(fx, "SOIL", "FIBER")) {
                boolean inside = false;
                for (var root : lanes) inside |= Math.abs(b[0] - root.centerX()) <= root.width() / 2
                        && Math.abs(b[1] - Player.COLLISION_Y_OFFSET - root.centerY()) <= root.height() / 2;
                assert inside : "soil and fibres burst only from real root lanes";
            }
        });
    }

    private static void reducedEffectsKeepTheThreat() throws Exception {
        for (var attack : Guardian.Attack.values()) {
            var full = landed(warden(attack), false);
            var reduced = landed(warden(attack), true);
            assert reduced.fx.particles() > 0 && reduced.fx.particles() < full.fx.particles() : attack + " reduced thins, never removes: " + reduced.fx.particles() + " vs " + full.fx.particles();
        }
        for (var attack : Guardian.ForestAttack.values()) {
            var full = landed(briar(attack), false);
            var reduced = landed(briar(attack), true);
            assert reduced.fx.particles() > 0 && reduced.fx.particles() < full.fx.particles() : attack + " reduced thins, never removes";
        }
        for (var check : new Object[][]{{Guardian.Attack.SWEEP, 0xffeaf6ff, "the sweep's blade"},
                {Guardian.Attack.FISSURE, 0xff17151a, "the split"}, {Guardian.Attack.TARGET, 0xffeaf6ff, "the slam's arc"}}) {
            var reduced = landed(warden((Guardian.Attack) check[0]), true);
            advance(reduced, 3, true);
            assert colour(draw(reduced, true), (int) check[1]) > 40 : check[2] + " still shows with reduced effects";
        }
        for (var attack : new Guardian.ForestAttack[]{Guardian.ForestAttack.ROOT_WEAVE, Guardian.ForestAttack.BRAMBLE_CAGE}) {
            var live = landed(briar(attack), true);
            advance(live, 18, true);
            assert live.boss.state() == Guardian.State.SLAM;
            assert colour(draw(live, true), 0xff796349) > 100 : attack + " keeps its live roots with reduced effects";
        }
        // The tell's decoration (glint, leaves, grit) is dropped entirely; the release still lands.
        for (var boss : new Guardian[]{warden(Guardian.Attack.TARGET), briar(Guardian.ForestAttack.ROOT_WEAVE)}) {
            var fx = new BossFx();
            while (boss.state() == Guardian.State.TELEGRAPH) { boss.update(FRAME, 700, 560); fx.update(FRAME, boss, true); }
            assert bits(fx, "GLINT", "LEAF").isEmpty() : "reduced effects drop the windup decoration";
        }
    }

    private static void effectsAreFiniteAndCapped() throws Exception {
        for (var landed : new Landed[]{landed(warden(Guardian.Attack.FISSURE), false), landed(briar(Guardian.ForestAttack.ROOT_WEAVE), false)}) {
            var boss = landed.boss;
            var fx = landed.fx;
            for (int i = 0; i < 600; i++) {             // ten seconds with a release every frame, cycling the Warden's moves
                if (boss.profile() == Guardian.Profile.OUTPOST_WARDEN) set(boss, "attack", Guardian.Attack.values()[i % 5]);
                set(boss, "state", Guardian.State.SLAM);
                set(boss, "stateTime", 0.0);
                fx.update(FRAME, boss, false);
                fx.release(boss, false);
                assert fx.particles() <= BossFx.LIMIT : "boss effects stay capped";
            }
            set(boss, "state", Guardian.State.RECOVER);
            for (int i = 0; i < 180; i++) fx.update(FRAME, boss, false);
            assert fx.particles() == 0 : "every mark is finite";
        }
    }

    // ------------------------------------------------------------------ the running game

    /** The real event path through B2BJ: a windup and the attack's own release, once each, gated by distance and mute. */
    private static void mixerHearsEachAttackOnce() throws Exception {
        GameAudio.setMuted(false);GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(1);
        try {
            var panel = wardenFight();
            var game = panel.game();
            var boss = game.guardian();
            for (var attack : Guardian.Attack.values()) {
                game.player().relocate(boss.x() - 480, boss.y() + 100);       // outside every forced attack, inside earshot
                poise(game, attack, BossFx.WINDUP_LEAD + .05, false);
                drain();
                for (int i = 0; i < 200 && boss.state() == Guardian.State.TELEGRAPH; i++) { set(game, "addTimer", 0.0); panel.step(.02); }
                panel.step(.02);
                var voices = oneShots();
                assert voices.size() == 2 : attack + " is heard as one windup and one release, got " + voices.size();
                assert voices.stream().filter(v -> heardAs(v, GameAudio.Cue.WARDEN_WINDUP)).count() == 1 : attack + " windup";
                var release = BossFx.releaseCue(boss);
                assert voices.stream().filter(v -> heardAs(v, release)).count() == 1 : attack + " lands with " + release;
                if (release != GameAudio.Cue.GUARDIAN_SLAM)
                    assert voices.stream().noneMatch(v -> heardAs(v, GameAudio.Cue.GUARDIAN_SLAM)) : attack + " must not stack the generic slam";
                for (int i = 0; i < 40; i++) { set(game, "addTimer", 0.0); panel.step(.02); }
                assert oneShots().size() == 2 : attack + " plays nothing per frame afterwards";
            }
            // A blow out of earshot, or while muted, reaches no mixer; a boss killed after its windup releases nothing.
            var handle = B2BJ.class.getDeclaredMethod("handleEvents", List.class);
            handle.setAccessible(true);
            poise(game, Guardian.Attack.SWEEP, .3, false);
            set(boss, "state", Guardian.State.SLAM);
            drain();
            handle.invoke(panel, List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.GUARDIAN_SLAM, boss.x() + 2000, boss.y())));
            assert oneShots().isEmpty() : "out of earshot";
            GameAudio.setMuted(true);
            handle.invoke(panel, List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.GUARDIAN_SLAM, boss.x(), boss.y())));
            assert oneShots().isEmpty() : "muted";
            GameAudio.setMuted(false);
            poise(game, Guardian.Attack.TARGET, BossFx.WINDUP_LEAD + .05, false);
            drain();
            for (int i = 0; i < 6; i++) panel.step(.02);
            assert oneShots().size() == 1 && heardAs(oneShots().get(0), GameAudio.Cue.WARDEN_WINDUP) : "windup heard";
            boss.hurt(boss.health());
            for (int i = 0; i < 60; i++) panel.step(.02);
            assert oneShots().stream().filter(v -> heardAs(v, GameAudio.Cue.WARDEN_WINDUP)).count() == 1
                    && oneShots().stream().noneMatch(v -> heardAs(v, GameAudio.Cue.GUARDIAN_SLAM)) : "a boss killed mid-tell plays no release";
            assert ((BossFx) read(panel, "bossAttackFx")).particles() == 0;

            var forest = briarFight();
            game = forest.game();
            boss = game.guardian();
            for (int attack = 0; attack < 3; attack++) {
                for (int i = 0; i < 2000 && boss.state() != Guardian.State.TELEGRAPH; i++) forest.step(.01);
                set(boss, "stateTime", boss.telegraphDuration() - BossFx.WINDUP_LEAD - .05);
                var commit = new double[]{boss.targetX(), boss.targetY()};
                game.player().relocate(commit[0], commit[1] + 300);          // clear of the lanes, blooms and cage
                drain();
                for (int i = 0; i < 100 && boss.state() == Guardian.State.TELEGRAPH; i++) forest.step(.02);
                forest.step(.02);
                var voices = oneShots();
                var release = BossFx.releaseCue(boss);
                assert voices.size() == 2 : boss.forestAttack() + " is heard as one windup and one release, got " + voices.size();
                assert voices.stream().filter(v -> heardAs(v, GameAudio.Cue.BRIAR_WINDUP)).count() == 1 : boss.forestAttack() + " windup";
                assert voices.stream().filter(v -> heardAs(v, release)).count() == 1 : boss.forestAttack() + " lands with " + release;
                assert voices.stream().noneMatch(v -> heardAs(v, GameAudio.Cue.GUARDIAN_SLAM)) : "no generic slam under Briarheart";
                game.player().relocate(commit[0], commit[1]);
                for (int i = 0; i < 2000 && boss.state() != Guardian.State.RECOVER && boss.state() != Guardian.State.HURT; i++) forest.step(.01);
            }
        } finally {
            drain();
            GameAudio.setMuted(true);
        }
    }

    private static void pauseFreezesAndResetsClear() throws Exception {
        var panel = wardenFight();
        var game = panel.game();
        var fx = (BossFx) read(panel, "bossAttackFx");
        poise(game, Guardian.Attack.FISSURE, .05, true);
        for (int i = 0; i < 6; i++) panel.step(.02);
        assert game.guardian().state() == Guardian.State.SLAM && fx.particles() > 0;
        String before = ages(fx);
        game.pause();
        for (int i = 0; i < 10; i++) panel.step(.05);
        assert ages(fx).equals(before) : "pause freezes boss effects";
        game.togglePause();
        panel.step(.02);
        assert !ages(fx).equals(before) : "they resume with the game";
        var handle = B2BJ.class.getDeclaredMethod("handleEvents", List.class);
        handle.setAccessible(true);
        handle.invoke(panel, List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.ROOM_ENTERED, 0, 0)));
        assert fx.particles() == 0 : "an area change clears boss effects";
        poise(game, Guardian.Attack.TARGET, .05, true);
        for (int i = 0; i < 6; i++) panel.step(.02);
        assert fx.particles() > 0;
        var reset = B2BJ.class.getDeclaredMethod("resetVisualState");
        reset.setAccessible(true);
        reset.invoke(panel);
        assert fx.particles() == 0 : "a new session starts clean";
        var other = landed(warden(Guardian.Attack.TARGET), false);
        assert other.fx.particles() > 0;
        other.fx.update(FRAME, new Guardian(100, 100), false);
        assert other.fx.particles() == 0 : "a different boss never inherits effects";
    }

    // ------------------------------------------------------------------ review frames (optional)

    private static void renders(Path out) throws Exception {
        Files.createDirectories(out);
        GameAudio.setMuted(true);
        var panel = wardenFight();
        var game = panel.game();
        var boss = game.guardian();
        game.player().relocate(boss.x() - 270, boss.y() + 100);
        for (var attack : Guardian.Attack.values()) {
            String name = "warden-" + attack.name().toLowerCase(java.util.Locale.ROOT);
            poise(game, attack, .3, true);
            panel.step(.01);
            save(panel, out, name + "-1-windup");
            for (int i = 0; i < 100 && boss.state() == Guardian.State.TELEGRAPH; i++) { set(game, "addTimer", 0.0); panel.step(.01); }
            panel.step(.04);
            save(panel, out, name + "-2-impact");
            for (int i = 0; i < (attack == Guardian.Attack.CHARGE || attack == Guardian.Attack.SHOCKWAVE ? 25 : 14); i++) panel.step(.01);
            save(panel, out, name + "-3-after");
            if (attack == Guardian.Attack.FISSURE || attack == Guardian.Attack.SWEEP) {
                poise(game, attack, .02, true);
                set(panel, "reducedEffects", true);
                for (int i = 0; i < 6; i++) panel.step(.01);
                save(panel, out, name + "-reduced");
                set(panel, "reducedEffects", false);
            }
            for (int i = 0; i < 40; i++) { set(game, "addTimer", 0.0); panel.step(.02); }
        }
        var forest = briarFight();
        boss = forest.game().guardian();
        for (int move = 0; move < 3; move++) {
            for (int i = 0; i < 2000 && boss.state() != Guardian.State.TELEGRAPH; i++) forest.step(.01);
            String name = "briarheart-" + boss.forestAttack().name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
            set(boss, "stateTime", boss.telegraphDuration() - .3);
            forest.step(.01);
            save(forest, out, name + "-1-windup");
            for (int i = 0; i < 100 && boss.state() == Guardian.State.TELEGRAPH; i++) forest.step(.01);
            forest.step(.06);
            save(forest, out, name + "-2-impact");
            forest.step(.25);
            save(forest, out, name + "-3-after");
            if (boss.forestAttack() == Guardian.ForestAttack.BRAMBLE_CAGE) {
                for (int i = 0; i < 100 && boss.stateSeconds() < .95 && boss.state() == Guardian.State.SLAM; i++) forest.step(.01);
                save(forest, out, name + "-4-shut");
                set(forest, "reducedEffects", true);
                save(forest, out, name + "-reduced");
                set(forest, "reducedEffects", false);
            }
            for (int i = 0; i < 2000 && boss.state() != Guardian.State.RECOVER && boss.state() != Guardian.State.HURT; i++) forest.step(.01);
        }
        System.out.println("Boss effect review frames: " + out.toAbsolutePath());
    }

    // ------------------------------------------------------------------ helpers

    private record Landed(Guardian boss, BossFx fx) { }
    private interface Check { void at(BossFx fx, Guardian boss) throws Exception; }

    private static Guardian warden(Guardian.Attack attack) throws Exception {
        var boss = new Guardian(900, 500);
        boss.activate(700, 560);
        set(boss, "attack", attack);
        boolean centred = attack == Guardian.Attack.SWEEP || attack == Guardian.Attack.SHOCKWAVE;
        set(boss, "targetX", centred ? 900.0 : 700.0);
        set(boss, "targetY", centred ? 500.0 : 560.0);
        double length = Math.hypot(-200, 60);
        set(boss, "chargeX", -200 / length);
        set(boss, "chargeY", 60 / length);
        return boss;
    }

    private static Guardian briar(Guardian.ForestAttack attack) throws Exception {
        var boss = new Guardian(900, 500, Guardian.Profile.BRIARHEART);
        set(boss, "impactNumber", attack.ordinal());
        boss.activate(700, 560);
        assert boss.forestAttack() == attack;
        return boss;
    }

    /** Runs the real tell to its release (without effects, so only the release's marks exist) and lands it. */
    private static Landed landed(Guardian boss, boolean reduced) {
        for (int i = 0; i < 400 && boss.state() != Guardian.State.SLAM; i++) boss.update(.01, 700, 560);
        assert boss.state() == Guardian.State.SLAM;
        var fx = new BossFx();
        fx.release(boss, reduced);
        return new Landed(boss, fx);
    }

    private static void advance(Landed landed, int frames, boolean reduced) {
        for (int i = 0; i < frames; i++) { landed.boss.update(FRAME, 700, 560); landed.fx.update(FRAME, landed.boss, reduced); }
    }

    private static void follow(Landed landed, Check check) throws Exception {
        for (int i = 0; i < 90; i++) {
            landed.boss.update(FRAME, 700, 560);
            landed.fx.update(FRAME, landed.boss, false);
            check.at(landed.fx, landed.boss);
        }
    }

    /** x, y, age, span of every live mark of these kinds. */
    private static List<double[]> bits(BossFx fx, String... kinds) throws Exception {
        var out = new ArrayList<double[]>();
        for (Object bit : (List<?>) read(fx, "bits")) {
            String kind = read(bit, "kind").toString();
            if (List.of(kinds).contains(kind) && (double) read(bit, "age") >= 0)
                out.add(new double[]{(double) read(bit, "x"), (double) read(bit, "y"), (double) read(bit, "age"), (double) read(bit, "span")});
        }
        return out;
    }

    private static String ages(BossFx fx) throws Exception {
        var out = new StringBuilder();
        for (Object bit : (List<?>) read(fx, "bits")) out.append(read(bit, "age")).append(',');
        return out.toString();
    }

    private static BufferedImage draw(Landed landed, boolean reduced) {
        var image = new BufferedImage(1800, 1200, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        landed.fx.drawGround(g, 0, 0);
        landed.fx.queue((y, paint) -> paint.run(), g, landed.boss, 0, 0, reduced);
        landed.fx.drawAbove(g, 0, 0, reduced);
        g.dispose();
        return image;
    }

    private static long colour(BufferedImage image, int argb) {
        return java.util.Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())).filter(p -> p == argb).count();
    }

    private static B2BJ wardenFight() throws Exception {
        var game = RuinedOutpostGame.campaign();
        game.begin();game.skipDialogue();game.setDebugGodMode(true);
        var panel = new B2BJ(false, game);
        panel.setSize(1280, 720);panel.doLayout();
        for (int i = 0; i < 240; i++) panel.step(.02);
        var boss = game.guardian();
        game.player().relocate(boss.x() - 270, boss.y() + 100);
        for (int i = 0; i < 1500 && (boss.state() == Guardian.State.DORMANT || game.blocked()); i++) {
            if (game.dialogue() != null) game.skipDialogue();
            game.update(.01, 0, 0);
        }
        assert boss.state() != Guardian.State.DORMANT && !game.blocked() : "test needs the Warden fight running";
        game.drainEvents();
        for (int i = 0; i < 120; i++) { set(game, "addTimer", 0.0); panel.step(.02); }   // let the name card pass
        return panel;
    }

    /** The same route as tools/PreviewForest: arrive in Area Two, then warp to Briarheart's arena. */
    private static B2BJ briarFight() throws Exception {
        var game = RuinedOutpostGame.campaign();
        game.begin();game.skipDialogue();game.debugWarp(1, false);
        var panel = new B2BJ(false, game);
        panel.setSize(1280, 720);panel.doLayout();
        for (int i = 0; i < 100; i++) panel.step(.05);
        game.debugWarp(1, true);game.player().setGodMode(true);
        for (var enemy : game.scouts()) enemy.hurt(10000);
        for (int i = 0; i < 1500 && game.guardian().state() == Guardian.State.DORMANT; i++) {
            if (game.dialogue() != null) game.skipDialogue();
            panel.step(.01);
        }
        assert game.guardian().profile() == Guardian.Profile.BRIARHEART && game.guardian().state() != Guardian.State.DORMANT : "test needs Briarheart awake";
        return panel;
    }

    /** Forces one committed Warden attack, `before` seconds from release. Aimed off the player unless `onPlayer`. */
    private static void poise(RuinedOutpostGame game, Guardian.Attack attack, double before, boolean onPlayer) throws Exception {
        var boss = game.guardian();
        var player = game.player();
        set(boss, "attack", attack);
        set(boss, "state", Guardian.State.TELEGRAPH);
        boolean centred = attack == Guardian.Attack.SWEEP || attack == Guardian.Attack.SHOCKWAVE;
        double tx = onPlayer ? player.x() : boss.x() + 100, ty = onPlayer ? player.y() : boss.y() - (attack == Guardian.Attack.FISSURE ? 220 : 40);
        set(boss, "targetX", centred ? boss.x() : tx);
        set(boss, "targetY", centred ? boss.y() : ty);
        set(boss, "fissureHorizontal", true);
        double dx = onPlayer ? player.x() - boss.x() : 1, dy = onPlayer ? player.y() - boss.y() : 0, length = Math.max(1e-9, Math.hypot(dx, dy));
        set(boss, "chargeX", dx / length);
        set(boss, "chargeY", dy / length);
        set(boss, "stateTime", boss.telegraphDuration() - before);
    }

    private static void drain() { GameAudio.stopLoops(); GameAudio.mixVoices(new int[22_050 * 4]); }

    /** The one-shot voices waiting in the mixer (loops excluded). */
    private static List<byte[]> oneShots() throws Exception {
        var out = new ArrayList<byte[]>();
        var voices = (List<?>) readStatic(GameAudio.class, "VOICES");
        synchronized (voices) { for (Object voice : voices) if (read(voice, "loop") == null) out.add((byte[]) read(voice, "pcm")); }
        return out;
    }

    /** Whether a queued voice is one of the cue's takes, at whatever detune it was given. */
    private static boolean heardAs(byte[] voice, GameAudio.Cue cue) {
        byte[][] takes = GameAudio.samples(cue);
        for (int take = 0; take < takes.length; take++) {
            double pitch = takes[take].length / (double) voice.length;
            if (pitch < .94 || pitch > 1.06) continue;
            byte[] expected = GameAudio.voicePcm(cue, take, pitch);
            double dot = 0, a = 0, b = 0;
            for (int i = 0; i + 1 < Math.min(voice.length, expected.length); i += 2) {
                double x = (short) ((voice[i] & 0xff) | voice[i + 1] << 8), y = (short) ((expected[i] & 0xff) | expected[i + 1] << 8);
                dot += x * y; a += x * x; b += y * y;
            }
            if (dot / Math.sqrt(Math.max(1, a * b)) > .95) return true;
        }
        return false;
    }

    private static void save(B2BJ panel, Path out, String name) throws Exception {
        var image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        ImageIO.write(image, "png", out.resolve(name + ".png").toFile());
    }

    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> at = type; at != null; at = at.getSuperclass())
            try { var f = at.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException next) { }
        throw new NoSuchFieldException(name);
    }
    private static Object read(Object target, String name) throws Exception { return field(target.getClass(), name).get(target); }
    private static Object readStatic(Class<?> type, String name) throws Exception { return field(type, name).get(null); }
    private static void set(Object target, String name, Object value) throws Exception { field(target.getClass(), name).set(target, value); }
}
