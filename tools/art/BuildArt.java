/**
 * Builds every generated Outpost art asset into assets/. Run tools/art/build-art.sh from the project
 * root. Deterministic: the same code and map layout always produce the same pixels.
 */
public final class BuildArt {
    public static void main(String[] args) {
        Canvas.Manifest.FORCE = java.util.Arrays.asList(args).contains("--force");
        String what = args.length > 0 && !args[0].startsWith("--") ? args[0] : "all";
        long t = System.nanoTime();
        if (what.equals("all") || what.equals("props")) props();
        if (what.equals("all") || what.equals("ground")) ground();
        if (what.equals("all") || what.equals("forest")) forest();
        if (what.equals("all") || what.equals("ui")) ui();
        System.out.printf("art built (%s) in %.1fs%n", what, (System.nanoTime() - t) / 1e9);
    }

    static void props() {
        Canvas.strip(Props.wall(0), Props.wall(1), Props.wall(2)).save("assets/props/outpost/ruin_wall.png");
        Canvas.strip(Props.palisade(0), Props.palisade(1), Props.palisade(2)).save("assets/props/outpost/stake_wall.png");
        Canvas.strip(Props.supplies(0), Props.supplies(1), Props.supplies(2), Props.supplies(3)).save("assets/props/outpost/supplies.png");
        Decals.sheet().save("assets/tilesets/outpost/outpost_decals.png");
        for (var kind : SetPieces.Kind.values()) Structures.sheet(kind).save(kind.sheet());
        Canvas[] camp = new Canvas[8], brazier = new Canvas[8];
        for (int f = 0; f < 8; f++) { camp[f] = Kit.fire(30, 40, f, 8, 5, 77); brazier[f] = Kit.fire(22, 30, f, 8, 4, 91); }
        Canvas.strip(camp).save("assets/effects/fire_camp.png");
        Canvas.strip(brazier).save("assets/effects/fire_brazier.png");
        WardenArt.build();
        BossArt.briarheart().save("assets/characters/guardian/briarheart_motion.png");
        for (String[] c : new String[][]{{"imp", "rift_imp"}, {"wolf", "thorn_wolf"}, {"shaman", "mire_shaman"}}) {
            Creatures.sheet(c[0], false).save("assets/characters/forest/" + c[1] + "_walk.png");
            Creatures.sheet(c[0], true).save("assets/characters/forest/" + c[1] + "_attack.png");
        }
        for (var set : Tiles16.Set.values()) {
            Tiles16.verifySeams(set);
            String area = set.name().toLowerCase(java.util.Locale.ROOT);
            Tiles16.sheet(set).save("assets/tilesets/" + area + "/" + area + "_tileset.png");
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("assets/source/outpost"));
            java.nio.file.Files.writeString(java.nio.file.Path.of("assets/source/outpost/outpost_night.gpl"), Pal.gpl());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    static void ui() {
        UiArt.kit().save("assets/ui/kit.png");
        LogoArt.logo().save("assets/ui/title/logo.png");
        TitleArt.build();
        UiArt.splash().save("assets/ui/splash.png");
    }

    static void forest() {
        new ForestBake().bake().save("assets/tilesets/forest/forest_ground.png");
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("assets/tilesets/forest/forest_ground.layout"),
                    SetPieces.layoutFingerprint(1) + "\n");
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    static void ground() {
        Canvas ground = new GroundBake().bake();
        // Hand retouches of the floor live in their own layer, painted over every fresh bake.
        var retouch = new java.io.File("assets/source/outpost/outpost_ground_retouch.png");
        if (retouch.exists()) ground.blit(Canvas.load(retouch.getPath()), 0, 0);
        ground.save("assets/tilesets/outpost/outpost_ground.png");
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("assets/tilesets/outpost/outpost_ground.layout"),
                    SetPieces.layoutFingerprint(0) + "\n");
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
