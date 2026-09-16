public final class RainorayRenderingTest {
    public static void main(String[] args) throws Exception {
        assert BladeAnimation.CELL_WIDTH == 80 && BladeAnimation.CELL_HEIGHT == 80
                : "Rainoray must share one native canvas across all actions";
        var blade=new BladeAnimation();
        blade.update(0,0,false,.5);
        assert blade.frame()>0 : "human form needs a breathing idle, not a frozen run frame";
        blade.face(1,0);blade.slash();blade.update(0,0,false,.29);
        assert blade.frame()==7 : "side slash must reach all eight authored poses";
        assert blade.sheetPath().endsWith("rainoray_slash.png");
        blade.update(1,0,true,0);blade.update(1,0,true,Player.DASH_DURATION-.001);
        assert blade.frame()==7 : "dash must finish all poses within the actual dash duration";
        var transform=new TransformationAnimation();transform.start();
        transform.update(TransformationAnimation.DURATION/2);
        assert transform.frame()==8 : "transformation must use sixteen morph poses";
        transform.update(TransformationAnimation.DURATION/2);
        assert !transform.active();
        nativeTelegraphsKeepPhysicsExtent();
        humanSkillInputAndEffectsStaySeparate();
        combatInterruptsMorphReplacement();
        engulfBodyCoversCurrentCorpseAndReturns();
        missingActionArtNeverMakesHumanInvisible();
        brazierAnimationKeepsItsFootprint();
        wardenMapMarkerNeverUsesPlayerPortrait();
        System.out.println("RainorayRenderingTest passed");
    }
    private static void nativeTelegraphsKeepPhysicsExtent() throws Exception {
        var panel=new B2BJ(false);
        var ring=new java.awt.image.BufferedImage(128*8,128,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=ring.createGraphics();ink.setColor(java.awt.Color.RED);
        for(int f=0;f<8;f++){ink.fillRect(f*128,0,128,1);ink.fillRect(f*128,127,128,1);
            ink.fillRect(f*128,0,1,128);ink.fillRect(f*128+127,0,1,128);}ink.dispose();
        var field=B2BJ.class.getDeclaredField("tellRing");field.setAccessible(true);field.set(panel,ring);
        var draw=B2BJ.class.getDeclaredMethod("drawTellRing",java.awt.Graphics2D.class,double.class,double.class,double.class);
        draw.setAccessible(true);
        var nativeLayer=new java.awt.image.BufferedImage(320,320,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g=nativeLayer.createGraphics();draw.invoke(panel,g,160.,160.,92.);g.dispose();
        var output=new java.awt.image.BufferedImage(640,640,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        g=output.createGraphics();g.drawImage(nativeLayer,0,0,640,640,null);g.dispose();
        assert output.getRGB(136,136)!=0&&output.getRGB(505,505)!=0 : "inclusive 184px radius must match physics, not fixed 2x art size";
        assert output.getRGB(135,320)==0&&output.getRGB(506,320)==0;
        for(int y=0;y<640;y+=2)for(int x=0;x<640;x+=2)assert output.getRGB(x,y)==output.getRGB(x+1,y+1)
                : "telegraph scaling must happen before native2x upscale";
    }
    private static void humanSkillInputAndEffectsStaySeparate() {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);panel.game().begin();var player=panel.game().player();
        player.collectIchor(100);assert player.transform();
        panel.getActionMap().get("ripostePressed").actionPerformed(null);
        assert panel.game().guarding() : "F must activate human riposte";
        panel.getActionMap().get("riposteReleased").actionPerformed(null);
        double energy=player.ichor();
        panel.getActionMap().get("effectsPressed").actionPerformed(null);
        assert player.ichor()==energy : "V toggles effects without spending skill energy";
        GameAudio.setMuted(false);
    }
    private static void combatInterruptsMorphReplacement() throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);
        var field=B2BJ.class.getDeclaredField("transformationAnimation");field.setAccessible(true);
        var morph=(TransformationAnimation)field.get(panel);
        var events=B2BJ.class.getDeclaredMethod("handleEvents",java.util.List.class);events.setAccessible(true);
        for(var action:new RuinedOutpostGame.EventType[]{RuinedOutpostGame.EventType.ATTACK,
                RuinedOutpostGame.EventType.DASH,RuinedOutpostGame.EventType.PLAYER_HIT,
                RuinedOutpostGame.EventType.CRESCENT_CAST,RuinedOutpostGame.EventType.RIPOSTE_START}) {
            morph.start();events.invoke(panel,java.util.List.of(new RuinedOutpostGame.Event(action,300,300)));
            assert !morph.active() : "combat response cannot remain hidden inside transformation: "+action;
        }
        GameAudio.setMuted(false);
    }
    private static void engulfBodyCoversCurrentCorpseAndReturns() throws Exception {
        GameAudio.setMuted(true);
        var panel=new B2BJ(false);var world=new RuinedOutpostMap(5);
        var game=new RuinedOutpostGame(world,new Player(900,550),java.util.List.of(),new Guardian(1300,700));game.begin();
        var field=B2BJ.class.getDeclaredField("game");field.setAccessible(true);field.set(panel,game);
        var corpse=new Wisp(970,550,970,970,1);corpse.hurt(1);
        var killed=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class);killed.setAccessible(true);killed.invoke(game,corpse);
        assert game.interact();
        var sheet=new java.awt.image.BufferedImage(1280,240,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=sheet.createGraphics();ink.setColor(new java.awt.Color(0x1248f0));ink.fillRect(0,0,1280,240);ink.dispose();
        field=B2BJ.class.getDeclaredField("engulfSheet");field.setAccessible(true);field.set(panel,sheet);
        for(int i=0;i<200&&game.absorptionProgress()<.5;i++)panel.step(.005);
        int[] bounds=engulfBounds(panel);
        assert Math.abs((bounds[0]+bounds[2]+1)/2.-game.absorptionTarget().x())<=2
                : "peak spread must centre whole body over current corpse, not beside it";
        assert bounds[0]%2==0&&bounds[1]%2==0 : "engulf keeps native2x placement grid";
        assert corpsePixels(panel)==0 : "swallowed remains must not protrude again as body reforms";
        for(int i=0;i<100&&game.absorptionProgress()<.96;i++)panel.step(.005);
        bounds=engulfBounds(panel);
        assert Math.abs((bounds[0]+bounds[2]+1)/2.-game.player().x())<=2
                : "body returns to stationary collision position before channel completes";
        game.update(.01,1,0);assert game.absorptionTarget()==null;
        assert engulfBounds(panel)[2]==-1 : "interruption must immediately remove engulf art";
        assert corpsePixels(panel)>0 : "interrupted absorption restores uneaten remains";
        GameAudio.setMuted(false);
    }
    private static int[] engulfBounds(B2BJ panel) throws Exception {
        var draw=B2BJ.class.getDeclaredMethod("drawSlime",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
        int[] bounds={1280,720,-1,-1};
        for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if(image.getRGB(x,y)==0xff1248f0) {
            bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.min(bounds[1],y);
            bounds[2]=Math.max(bounds[2],x);bounds[3]=Math.max(bounds[3],y);
        }
        return bounds;
    }
    private static int corpsePixels(B2BJ panel) throws Exception {
        var draw=B2BJ.class.getDeclaredMethod("drawCorpses",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
        int count=0;for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if((image.getRGB(x,y)>>>24)>0)count++;
        return count;
    }
    @SuppressWarnings("unchecked")
    private static void missingActionArtNeverMakesHumanInvisible() throws Exception {
        var panel=new B2BJ(false);panel.game().player().relocate(300,300);
        var field=B2BJ.class.getDeclaredField("rainoraySheets");field.setAccessible(true);
        var sheets=(java.util.Map<BladeAnimation.Action,java.awt.image.BufferedImage>)field.get(panel);
        var idle=new java.awt.image.BufferedImage(640,240,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=idle.createGraphics();
        for(int row=0;row<3;row++)for(int frame=0;frame<8;frame++) {
            ink.setColor(new java.awt.Color(frame*20,60+row*40,0));ink.fillRect(frame*80,row*80,80,80);
        }
        ink.dispose();sheets.clear();sheets.put(BladeAnimation.Action.IDLE,idle);
        field=B2BJ.class.getDeclaredField("bladeAnimation");field.setAccessible(true);var animation=(BladeAnimation)field.get(panel);
        field=RuinedOutpostGame.class.getDeclaredField("combo");field.setAccessible(true);field.set(panel.game(),2);
        var draw=B2BJ.class.getDeclaredMethod("drawBlade",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        for(var action:new BladeAnimation.Action[]{BladeAnimation.Action.CAST,BladeAnimation.Action.SLASH,BladeAnimation.Action.HURT}) {
            animation.face(1,0);animation.play(action,.3);animation.update(0,0,false,.16);
            var image=new java.awt.image.BufferedImage(640,640,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
            assert image.getRGB(300,280)==0xff006400 : "missing action must use idle frame0 with same facing, not become invisible";
        }
        sheets.clear();var image=new java.awt.image.BufferedImage(640,640,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
        int visible=0;for(int y=0;y<640;y++)for(int x=0;x<640;x++)if((image.getRGB(x,y)>>>24)>0)visible++;
        assert visible>200 : "missing idle must still leave visible player feedback";
    }
    @SuppressWarnings("unchecked")
    private static void brazierAnimationKeepsItsFootprint() throws Exception {
        var panel=new B2BJ(false);var map=new RuinedOutpostMap(9);var player=new Player(900,550);
        var game=new RuinedOutpostGame(map,player,java.util.List.of(),new Guardian(1100,550));game.begin();
        var field=B2BJ.class.getDeclaredField("game");field.setAccessible(true);field.set(panel,game);
        field=B2BJ.class.getDeclaredField("dressingSprites");field.setAccessible(true);
        var sprites=(java.util.Map<RuinedOutpostMap.Decoration,java.awt.image.BufferedImage>)field.get(panel);
        var sheet=new java.awt.image.BufferedImage(512,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=sheet.createGraphics();for(int f=0;f<8;f++) {
            ink.setColor(new java.awt.Color(17,f*20,239));ink.fillRect(f*64,0,64,64);
        }ink.dispose();sprites.put(RuinedOutpostMap.Decoration.BRAZIER,sheet);
        var brazier=map.dressing().stream().filter(d->d.decoration()==RuinedOutpostMap.Decoration.BRAZIER).findFirst().orElseThrow();
        var image=worldImage(panel);int minX=1280,minY=720,maxX=-1,maxY=-1;
        for(int y=0;y<720;y++)for(int x=0;x<1280;x++) {
            int pixel=image.getRGB(x,y);
            if((pixel&0xff00ff)==0x1100ef&&((pixel>>8)&255)%20==0) {
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
        }
        assert maxX-minX+1==128&&maxY-minY+1==128 : "animated brazier must crop one64px cell, never draw whole strip";
        assert minX==(int)brazier.x()-64&&minY==(int)brazier.y()-112 : "animated prop retains its ground anchor";
        field=B2BJ.class.getDeclaredField("frameCounter");field.setAccessible(true);field.set(panel,15L);
        image=worldImage(panel);
        assert image.getRGB(minX+8,minY+8)==0xff1128ef : "brazier advances two poses in15 nominal60Hz ticks";
        player.relocate(brazier.x(),brazier.y()-70);image=worldImage(panel);
        assert image.getRGB((int)brazier.x(),(int)brazier.y()-60)==0xff1128ef : "brazier occludes a rear actor";
        player.relocate(brazier.x(),brazier.y()+20);image=worldImage(panel);
        int pixel=image.getRGB((int)brazier.x(),(int)brazier.y()+4),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
        assert g-r>50&&b-r>50 : "front actor must occlude animated brazier";
    }
    private static java.awt.image.BufferedImage worldImage(B2BJ panel)throws Exception {
        var draw=B2BJ.class.getDeclaredMethod("drawWorld",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();return image;
    }
    private static void wardenMapMarkerNeverUsesPlayerPortrait() throws Exception {
        var panel=new B2BJ(false);
        var field=RuinedOutpostGame.class.getDeclaredField("visited");field.setAccessible(true);((boolean[])field.get(panel.game()))[9]=true;
        var portrait=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ink=portrait.createGraphics();ink.setColor(java.awt.Color.GREEN);ink.fillRect(0,0,32,32);ink.dispose();
        field=B2BJ.class.getDeclaredField("bladeIcon");field.setAccessible(true);field.set(panel,portrait);
        var guardian=new java.awt.image.BufferedImage(256,192,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ink=guardian.createGraphics();ink.setColor(java.awt.Color.ORANGE);ink.fillRect(0,0,256,192);
        ink.setColor(new java.awt.Color(0x6b1fad));ink.fillRect(0,0,64,64);ink.dispose();
        field=B2BJ.class.getDeclaredField("guardianSheet");field.setAccessible(true);field.set(panel,guardian);
        var draw=B2BJ.class.getDeclaredMethod("drawMap",java.awt.Graphics2D.class,boolean.class);draw.setAccessible(true);
        var image=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ink=image.createGraphics();draw.invoke(panel,ink,true);ink.dispose();
        assert image.getRGB(526,235)==0xff6b1fad : "Warden node must use its first64px sprite, not Rainoray portrait or whole atlas";
    }
}
