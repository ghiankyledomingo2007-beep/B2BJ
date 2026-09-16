public final class RainorayArtTest {
    public static void main(String[] args) {
        try {
            engulfRuntimeReachesLastPose();
            crescentCoreMatchesDamageDiameter();
        } catch(Exception error) { throw new AssertionError("Render fixture failed",error); }
        if(args.length>0&&args[0].equals("--runtime-only"))return;
        for(String action:new String[]{"idle","dash","cast","guard","hurt"})
            CombatArtTest.check("assets/characters/blade/rainoray_"+action+".png",80,80,8,3,8);
        CombatArtTest.check("assets/characters/blade/rainoray_run.png",80,80,BladeAnimation.RUN_FRAMES,3,BladeAnimation.RUN_FRAMES);
        CombatArtTest.check("assets/characters/blade/rainoray_slash.png",80,80,8,9,8);
        CombatArtTest.check("assets/characters/slime/slime_engulf.png",80,80,16,3,16);
        for(String name:new String[]{"transform_in","transform_out"})
            CombatArtTest.check("assets/effects/"+name+".png",80,80,16,3,16);
        for(String name:new String[]{"telegraph_chevron","aim_reticle","fissure_tile"})
            CombatArtTest.check("assets/effects/"+name+".png",32,32,8,1,8);
        CombatArtTest.check("assets/effects/telegraph_ring.png",128,128,8,1,8);
        CombatArtTest.check("assets/effects/ichor_crescent.png",64,64,8,1,8);
        CombatArtTest.check("assets/effects/riposte_guard.png",80,80,8,1,8,true);
        CombatArtTest.check("assets/effects/riposte_counter.png",80,80,8,1,8);
        for(String name:new String[]{"crescent","riposte"})
            CombatArtTest.check("assets/ui/"+name+".png",32,32,1,1,1);
        for(var decoration:RuinedOutpostMap.Decoration.values()) {
            var image=B2BJ.loadImage(decoration.path());
            assert image!=null : "Missing reviewed dressing: "+decoration.path();
        }
        var ring=B2BJ.loadImage("assets/effects/telegraph_ring.png");
        for(int f=0;f<8;f++) {
            for(int y=42;y<86;y++)for(int x=42;x<86;x++)
                assert (ring.getRGB(f*128+x,y)>>>24)==0 : "danger ring must leave terrain visible";
        }
        System.out.println("RainorayArtTest passed");
    }
    private static void engulfRuntimeReachesLastPose() throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);var world=new RuinedOutpostMap(5);
        var game=new RuinedOutpostGame(world,new Player(900,550),java.util.List.of(),new Guardian(1300,700));
        game.begin();var field=B2BJ.class.getDeclaredField("game");field.setAccessible(true);field.set(panel,game);
        var corpse=new Wisp(970,550,970,970,1);corpse.hurt(1);
        var killed=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class);killed.setAccessible(true);killed.invoke(game,corpse);
        assert game.interact();
        for(int i=0;i<240&&game.absorptionTarget()!=null&&game.absorptionProgress()<.95;i++)panel.step(.005);
        assert game.absorptionProgress()>.9375&&game.absorptionProgress()<1;
        var sheet=new java.awt.image.BufferedImage(80*16,80*3,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=sheet.createGraphics();
        for(int frame=0;frame<16;frame++) {ink.setColor(new java.awt.Color(frame*16,0,0));ink.fillRect(frame*80,0,80,240);}ink.dispose();
        field=B2BJ.class.getDeclaredField("engulfSheet");field.setAccessible(true);field.set(panel,sheet);
        var draw=B2BJ.class.getDeclaredMethod("drawSlime",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var output=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ink=output.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
        assert output.getRGB(900,500)==0xfff00000 : "engulf must reach authored pose15 before channel completion";
        GameAudio.setMuted(false);
        System.out.println("Engulf runtime16-pose check passed");
    }
    private static void crescentCoreMatchesDamageDiameter() throws Exception {
        var panel=new B2BJ(false);panel.game().begin();var player=panel.game().player();
        player.collectIchor(100);assert player.transform();assert panel.game().ichorCrescent(1,0);
        var crescent=panel.game().crescents().get(0);
        var sheet=new java.awt.image.BufferedImage(64*8,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=sheet.createGraphics();ink.setColor(java.awt.Color.WHITE);
        for(int frame=0;frame<8;frame++)ink.fillRect(frame*64+11,11,42,42);ink.dispose();
        var field=B2BJ.class.getDeclaredField("crescentSheet");field.setAccessible(true);field.set(panel,sheet);
        var draw=B2BJ.class.getDeclaredMethod("drawCrescent",java.awt.Graphics2D.class,int.class,int.class,RuinedOutpostGame.Crescent.class);
        draw.setAccessible(true);
        var output=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ink=output.createGraphics();draw.invoke(panel,ink,0,0,crescent);ink.dispose();
        int minX=1280,minY=720,maxX=-1,maxY=-1;
        for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if((output.getRGB(x,y)>>>24)>0) {
            minX=Math.min(x,minX);minY=Math.min(y,minY);maxX=Math.max(x,maxX);maxY=Math.max(y,maxY);
        }
        assert maxX-minX+1==42&&maxY-minY+1==42 : "42px authored core must stay inside44px damage diameter, not double to84";
        assert crescent.radius()==22 : "visual correction must not change collision";
        System.out.println("Crescent native-size check passed");
    }
}
