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
        if (what.equals("campfire")) campfire();
        if (what.equals("brazier")) brazier();
        if (what.equals("warden")) WardenArt.combat();
        if (what.equals("warden-hires")) { WardenHiRes.build().save(WardenHiRes.OUT); WardenHiRes.strips(); }
        if (what.equals("dummy")) ImportedArt.dummy().save(SetPieces.Kind.DUMMY.sheet());
        if (what.equals("gate")) Structures.sheet(SetPieces.Kind.EAST_GATE).save(SetPieces.Kind.EAST_GATE.sheet());
        if (what.equals("tent")) Structures.tent().save(SetPieces.Kind.TENT.sheet());
        if (what.equals("infirmary")) Structures.sheet(SetPieces.Kind.INFIRMARY).save(SetPieces.Kind.INFIRMARY.sheet());
        if (what.equals("all") || what.equals("portraits")) ImportedArt.portraits();
        if (what.equals("all") || what.equals("ground")) ground();
        if (what.equals("all") || what.equals("forest")) forest();
        if (what.equals("all") || what.equals("ui")) ui();
        if (what.equals("all") || what.equals("field")) field();
        System.out.printf("art built (%s) in %.1fs%n", what, (System.nanoTime() - t) / 1e9);
    }

    static void props() {
        Canvas.strip(Props.wall(0), Props.wall(1), Props.wall(2)).save("assets/props/outpost/ruin_wall.png");
        Canvas.strip(Props.palisade(0), Props.palisade(1), Props.palisade(2)).save("assets/props/outpost/stake_wall.png");
        Decals.sheet().save("assets/tilesets/outpost/outpost_decals.png");
        for (var kind : SetPieces.Kind.values()) Structures.sheet(kind).save(kind.sheet());
        campfire();
        brazier();
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
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("assets/source/shared"));
            java.nio.file.Files.writeString(java.nio.file.Path.of("assets/source/shared/outpost_night.gpl"), Pal.gpl());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    static void ui() {
        UiArt.kit().save("assets/ui/kit.png");
        TitleArt.build();
        UiArt.splash().save("assets/ui/splash.png");
    }

    /** Field-camp rescue props (FieldArt); the side palisade must stack under itself before it is saved. */
    static void field() {
        FieldArt.verifySideSeams();
        String dir = "assets/props/outpost/";
        FieldArt.side().save(dir + "field_palisade_side.png");
        FieldArt.crawl().save(dir + "field_crawl.png");
        Canvas.strip(FieldArt.lowFence(0), FieldArt.lowFence(1), FieldArt.lowFence(2)).save(dir + "field_fence_low.png");
        FieldArt.crawl(true).save(dir + "field_crawl_low.png");
        Canvas.strip(FieldArt.key(0), FieldArt.key(1), FieldArt.key(2), FieldArt.key(3)).save(dir + "field_key.png");
        FieldArt.tin().save(dir + "field_tin.png");
        Canvas.strip(FieldArt.chest(false), FieldArt.chest(true)).save(dir + "field_chest.png");
        FieldArt.survivors().save(dir + "field_survivor.png");
        Canvas.strip(FieldArt.bedroll(0), FieldArt.bedroll(1)).save(dir + "field_bedroll.png");
    }

    static void campfire() {
        Canvas[] frames=new Canvas[8];
        for(int f=0;f<frames.length;f++)frames[f]=Kit.campfire(f);
        Canvas.strip(frames).save("assets/effects/fire_camp.png");
    }

    static void brazier() {
        Canvas[] frames=new Canvas[8];
        for(int f=0;f<frames.length;f++)frames[f]=Kit.fire(22,30,f);
        Canvas.strip(frames).save("assets/effects/fire_brazier.png");
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
