import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;

/** Warden effects stay capped, run without art, and sit in depth order so actors in front cover the Warden's flash. */
public final class WardenFxTest {
    public static void main(String[] args) throws Exception {
        spikeArtHasNoBars();
        sweepsUseSolidClusters();
        groundAttacksKeepNaturalWindups();
        waveSpikesFollowTheDamageCircle();
        flashesFollowAttackPoses();
        BufferedImage atlas = B2BJ.loadImage("assets/characters/guardian/warden_motion.png");
        var view = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) view.getRaster().getDataBuffer()).getData();

        for (BufferedImage sheet : new BufferedImage[]{null, atlas}) {
            var warden = new Guardian(320, 300);
            warden.activate(200, 300);
            var fx = new WardenFx(sheet);
            fx.awaken(warden);
            var g = view.createGraphics();
            for (int i = 0; i < 600; i++) {                 // ten seconds with a slam every frame
                warden.update(1 / 60.0, 200, 300);
                fx.slam(warden, 200, 300);
                fx.update(1 / 60.0, warden);
                fx.drawGround(g, warden, 0, 0);
                fx.queue((y, paint) -> paint.run(), g, warden, 0, 0, i, false);
                fx.drawAbove(g, warden, 0, 0, i);
                assert fx.particles() <= 260 : "Warden effects must stay capped";
            }
            warden.hurt(warden.health());
            for (int i = 0; i < 120; i++) fx.update(1 / 60.0, warden);
            assert fx.fallen() : "the fallen Warden stays in the arena";
            g.dispose();
        }

        // Mid-blink, the white hit flash belongs to the Warden's own depth slot, never the layer above every actor.
        var warden = new Guardian(320, 300);
        warden.activate(200, 300);
        warden.hurt(1);
        assert !warden.flashVisible() : "test needs the Warden mid-blink";
        var fx = new WardenFx(atlas);
        var g = view.createGraphics();
        Arrays.fill(pixels, 0);
        fx.drawAbove(g, warden, 0, 0, 0);
        for (int p : pixels) assert p == 0 : "the hit flash must not paint over actors in front of the Warden";
        var depths = new ArrayList<Double>();
        var paints = new ArrayList<Runnable>();
        fx.queue((y, paint) -> { depths.add(y); paints.add(paint); }, g, warden, 0, 0, 0, false);
        double after = depths.get(0) - (warden.y() + Guardian.GROUND_Y_OFFSET);
        assert depths.size() == 1 && after > 0 && after < 1 : "the flash sorts just after the Warden, before anything in front";
        paints.get(0).run();
        int white = 0;
        for (int p : pixels) if (p == 0xfffff6ff) white++;
        assert white > 500 : "a hurt Warden flashes white";
        Arrays.fill(pixels, 0);
        fx.queue((y, paint) -> paint.run(), g, warden, 0, 0, 0, true);
        for (int p : pixels) assert p == 0 : "reduced effects keep the plain blink, without the white flash";
        g.dispose();
        System.out.println("WardenFxTest passed");
    }

    private static void spikeArtHasNoBars() {
        for(String prefix:new String[]{"warden","briar"}) {
            var sheet=B2BJ.loadImage("assets/effects/"+prefix+"_spike.png");
            assert sheet!=null&&sheet.getWidth()==120&&sheet.getHeight()==30;
            for(int f=0;f<6;f++)for(int y=0;y<30;y++) {
                int run=0;
                for(int x=0;x<20;x++) {
                    int p=sheet.getRGB(f*20+x,y),r=p>>16&255,g=p>>8&255,b=p&255;
                    boolean glow=prefix.equals("warden")?g>r*1.4&&b>r*1.3:r>g&&g>b*1.4;
                    run=(p>>>24)>0&&glow?run+1:0;
                    assert run<=3 : prefix+" spike has a horizontal glow bar in frame "+f;
                }
            }
            int height=0;
            for(int y=0;y<27;y++)if((sheet.getRGB(2*20+10,y)>>>24)>0)height++;
            assert height>10 : "spikes retain their rising stone silhouette";
        }
    }

    private static void sweepsUseSolidClusters() {
        for(String prefix:new String[]{"warden","briar"}) {
            var sheet=B2BJ.loadImage("assets/effects/"+prefix+"_sweep.png");
            assert sheet!=null&&sheet.getWidth()==184*4&&sheet.getHeight()==112;
            var frames=new java.util.HashSet<Integer>();
            for(int frame=0;frame<4;frame++) {
                int visible=0;
                for(int y=0;y<112;y+=2)for(int x=0;x<184;x+=2) {
                    int p=sheet.getRGB(frame*184+x,y);
                    assert (p>>>24)==0||(p>>>24)==255;
                    for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
                        assert sheet.getRGB(frame*184+x+dx,y+dy)==p : "sweep must not regress to noisy dither";
                    if(p!=0)visible++;
                }
                assert visible>200&&visible<1500;
                frames.add(Arrays.hashCode(sheet.getRGB(frame*184,0,184,112,null,0,184)));
            }
            assert frames.size()==4 : "cleave needs a complete turn";
        }
    }

    private static void groundAttacksKeepNaturalWindups() throws Exception {
        for(var theme:new WardenFx.Theme[]{WardenFx.Theme.WARDEN,WardenFx.Theme.BRIARHEART})
            for(var attack:new Guardian.Attack[]{Guardian.Attack.TARGET,Guardian.Attack.FISSURE,Guardian.Attack.CHARGE,Guardian.Attack.SHOCKWAVE}) {
                var boss=new Guardian(320,300);boss.activate(400,300);
                set(boss,"state",Guardian.State.TELEGRAPH);set(boss,"attack",attack);
                set(boss,"targetX",400.);set(boss,"targetY",300.);set(boss,"stateTime",.4);
                var image=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);
                var g=image.createGraphics();new WardenFx(null,theme).drawGround(g,boss,0,0);g.dispose();
                assert Arrays.stream(((DataBufferInt)image.getRaster().getDataBuffer()).getData()).anyMatch(p->p!=0)
                        : attack+" needs a visible dust windup before it damages the player";
                if(attack==Guardian.Attack.TARGET) {
                    int left=0,right=0;
                    for(int y=260;y<380;y++)for(int x=320;x<480;x++)if(image.getRGB(x,y)!=0) {
                        if(x<352)left++;
                        if(x>448)right++;
                    }
                    assert left>0&&right>0 : "slam dust must show beyond the player's 96px-wide body";
                }
            }
    }

    private static void waveSpikesFollowTheDamageCircle() throws Exception {
        var boss=new Guardian(320,300);boss.activate(320,300);
        set(boss,"state",Guardian.State.SLAM);set(boss,"attack",Guardian.Attack.SHOCKWAVE);
        set(boss,"targetX",320.);set(boss,"targetY",300.);set(boss,"stateTime",.4);
        var fx=new WardenFx(null);fx.slam(boss,320,300);fx.update(.01,boss);
        var field=WardenFx.class.getDeclaredField("bits");field.setAccessible(true);
        int count=0;
        for(var bit:(java.util.List<?>)field.get(fx)) {
            var kind=bit.getClass().getDeclaredField("kind");kind.setAccessible(true);
            if(!kind.get(bit).toString().equals("SPIKE"))continue;
            var x=bit.getClass().getDeclaredField("x");x.setAccessible(true);
            var y=bit.getClass().getDeclaredField("y");y.setAccessible(true);
            double radius=Math.hypot(x.getDouble(bit)-320,y.getDouble(bit)-300);
            assert Math.abs(radius/58-Math.rint(radius/58))<.000001
                    : "spike feet must follow the damage circle, not a squashed/offset ellipse";
            count++;
        }
        assert count>=8;
    }

    private static void flashesFollowAttackPoses() throws Exception {
        var game=RuinedOutpostGame.campaign();var boss=game.guardian();
        boss.activate(boss.x()-180,boss.y()+80);
        var panel=new B2BJ(false,game);
        var fx=new WardenFx(B2BJ.loadImage("assets/characters/guardian/warden_motion.png"));
        var white=WardenFx.class.getDeclaredField("white");white.setAccessible(true);
        var draw=B2BJ.class.getDeclaredMethod("drawGuardian",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var flash=WardenFx.class.getDeclaredMethod("frameOf",java.awt.Graphics2D.class,BufferedImage.class,Guardian.class,int.class,int.class,float.class);
        flash.setAccessible(true);
        for(var attack:Guardian.Attack.values())for(int direction:new int[]{-1,1}) {
            set(boss,"attack",attack);set(boss,"chargeX",(double)direction);set(boss,"stateTime",boss.telegraphDuration()*.8);
            var body=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
            var tint=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
            int cx=(int)boss.x()-320,cy=(int)boss.y()-300;
            var g=body.createGraphics();draw.invoke(panel,g,cx,cy);g.dispose();
            g=tint.createGraphics();flash.invoke(fx,g,white.get(fx),boss,cx,cy,1f);g.dispose();
            for(int y=124;y<380;y++)for(int x=192;x<448;x++)
                assert (body.getRGB(x,y)>>>24)==(tint.getRGB(x,y)>>>24)
                        : "flash must match the visible "+attack+" pose and facing";
        }
    }

    private static void set(Object target,String name,Object value) throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
