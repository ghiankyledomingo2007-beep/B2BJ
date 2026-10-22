import java.awt.image.BufferedImage;

public final class PackagedAssetLoadingTest {
    public static void main(String[] args) throws Exception {
        loadsRuntimeAssetFromClasspath();
        RainorayArtTest.main(new String[0]);
        OutpostArtTest.main(new String[0]);     // floor, layout stamp, prop and set piece sheets, from the classpath only
        ForestArtTest.main(new String[0]);      // the forest floor, its stamp, tileset and every forest sheet, from the jar too
        WardenFxTest.main(new String[0]);       // cleaned spike sheets and natural boss cues work from packaged assets
        OutpostDialogueTest.main(new String[0]);
        CampArtTest.main(new String[0]);
        System.out.println("PackagedAssetLoadingTest passed");
    }

    private static void loadsRuntimeAssetFromClasspath() {
        BufferedImage image = B2BJ.loadImage("assets/characters/slime/slime_sprite_sheet.png");

        assert image != null : "packaged slime asset must load";
        assert Ui.fontsLoaded() : "the pixel fonts must load from the packaged classpath";
        assert image.getWidth() > 0 : "packaged slime asset must have pixels";
        for(String path:new String[]{
                "assets/characters/blade/blade_run.png",
                "assets/characters/blade/blade_dash.png",
                "assets/characters/blade/blade_slash.png",
                "assets/characters/blade/blade_cut.png",
                "assets/characters/guardian/guardian_actions.png",
                "assets/characters/wisp/wisp_walk.png",
                "assets/characters/wisp/wisp_attack.png",
                "assets/characters/slime/slime_effects.png",
                "assets/characters/slime/slime_idle.png",
                "assets/characters/npcs/mara_sheet.png",
                "assets/characters/npcs/mara_idle.png",
                "assets/characters/slime/rainoray_portrait.png",
                "assets/ui/pixel_font.png",
                "assets/ui/kit.png",
                "assets/ui/splash.png",
                "assets/ui/title/sky.png",
                "assets/ui/title/clouds.png",
                "assets/ui/title/moon.png",
                "assets/ui/title/far.png",
                "assets/ui/title/mist.png",
                "assets/ui/title/mid.png",
                "assets/ui/title/fore.png",
                "assets/ui/title/logo.png",
                "assets/ui/hud_frame.png",
                "assets/ui/skill_frame.png",
                "assets/ui/vitality.png",
                "assets/ui/blob.png",
                "assets/ui/blade.png",
                "assets/ui/slash.png",
                "assets/ui/wave.png",
                "assets/ui/dash.png",
                "assets/ui/consume.png",
                "assets/ui/camp.png",
                "assets/ui/heal.png",
                "assets/characters/slime/slime_absorb.png",
                "assets/effects/corpse_idle.png",
                "assets/effects/corpse_consume.png",
                "assets/effects/absorb_link.png",
                "assets/effects/heal_motes.png",
                "assets/tilesets/legacy/ruined_outpost/ruined_outpost_wang.png",
                "assets/tilesets/outpost/outpost_tileset.png",
                "assets/effects/blade_hit.png",
                "assets/effects/blob_hit.png",
                "assets/effects/water_slash.png",
                "assets/effects/tide_wave.png",
                "assets/effects/water_splash.png",
                "assets/effects/water_charge.png",
                "assets/effects/water_wake.png",
                "assets/effects/tide_impact.png",
                "assets/props/outpost/barricade.png",
                "assets/props/outpost/cot.png",
                "assets/props/outpost/standard.png",
                "assets/props/outpost/wall.png",
                "assets/props/outpost/supply-barrel.png",
                "assets/props/outpost/dead-tree.png",
                "assets/characters/guardian/warden_motion.png",
                "assets/effects/fire_camp.png",
                "assets/effects/fire_brazier.png",
                "assets/effects/warden_debris.png",
                "assets/effects/warden_dust.png",
                "assets/effects/warden_crack.png",
                "assets/effects/warden_sweep.png",
                "assets/effects/warden_flame.png",
                "assets/effects/warden_spike.png",
                "assets/effects/warden_beam.png",
                "assets/effects/warden_mote.png"}) {
            assert B2BJ.loadImage(path)!=null : "missing packaged runtime asset "+path;
        }
        for(var prop:RuinedOutpostMap.Prop.values()) {
            BufferedImage sprite=B2BJ.loadImage(prop.path());
            assert prop.anchorX>=prop.footprintWidth/2 && prop.anchorX+prop.footprintWidth/2<sprite.getWidth();
            assert prop.anchorY>=prop.footprintDepth && prop.anchorY<sprite.getHeight();
            int opaque=0;
            for(int y=0;y<sprite.getHeight();y++)for(int x=0;x<sprite.getWidth();x++)
                if((sprite.getRGB(x,y)>>>24)>0)opaque++;
            assert opaque>0 && opaque<sprite.getWidth()*sprite.getHeight()*0.85 : "prop needs transparent margins: "+prop;
        }
    }
}
