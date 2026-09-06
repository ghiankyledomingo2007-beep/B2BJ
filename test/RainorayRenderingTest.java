public final class RainorayRenderingTest {
    public static void main(String[] args) throws Exception {
        assert BladeAnimation.CELL_WIDTH == 80 && BladeAnimation.CELL_HEIGHT == 80
                : "Rainoray must share one native canvas across all actions";
        var blade=new BladeAnimation();
        blade.update(0,0,false,.5);
        assert blade.frame()>0 : "masked human needs a breathing idle, not a frozen run frame";
        blade.face(1,0);blade.slash();blade.update(0,0,false,.29);
        assert blade.frame()==7 : "side slash must reach all eight authored poses";
        assert blade.sheetPath().endsWith("rainoray_slash.png");
        var transform=new TransformationAnimation();transform.start();
        transform.update(TransformationAnimation.DURATION/2);
        assert transform.frame()==8 : "transformation must use sixteen morph poses";
        transform.update(TransformationAnimation.DURATION/2);
        assert !transform.active();
        nativeTelegraphsKeepPhysicsExtent();
        humanSkillInputAndEffectsStaySeparate();
        combatInterruptsMorphReplacement();
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
        assert output.getRGB(136,136)!=0&&output.getRGB(503,503)!=0 : "184px radius must match physics, not fixed 2x art size";
        assert output.getRGB(135,320)==0&&output.getRGB(504,320)==0;
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
}
