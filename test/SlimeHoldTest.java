import java.util.List;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/** The hold path uses real mouse input, simulation, terrain and the game renderer. */
public final class SlimeHoldTest {
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(true);
        tapsAndBuffer();holdAndRelease();cancellations();collision();chargeFormation();mouseAndEffects();
        GameAudio.stopLoops();
        System.out.println("SlimeHoldTest passed: tap/buffer, charge/release/aim, interruption, terrain, mouse and reduced effects");
    }
    static RuinedOutpostGame fixture() {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(),new Guardian(1500,900));
        game.setCriticalRollForTesting(()->1);game.begin();return game;
    }
    static void advance(RuinedOutpostGame g,double seconds) {
        while(seconds>1e-8){double dt=Math.min(.01,seconds);g.update(dt,0,0);seconds-=dt;}
    }
    static void tapsAndBuffer() {
        var g=fixture();assert g.primaryPress(1,0);advance(g,.06);
        assert g.projectiles().isEmpty() : "press must not fire while deciding between tap and hold";
        g.primaryRelease(1,0);advance(g,.06);
        assert g.projectiles().size()==1&&g.waterKind()==WaterProjectile.Kind.CUT;
        advance(g,.23);assert g.primaryPress(1,0);g.primaryRelease(1,0);advance(g,.10);
        assert g.waterKind()==WaterProjectile.Kind.RETURN_CUT : "one late tap buffers the next combo step";
        advance(g,.7);assert g.projectiles().isEmpty()&&!g.slimePreparing() : "buffer never auto-repeats";
    }
    static void holdAndRelease() {
        var g=fixture();assert g.primaryPress(1,0);
        double x=g.player().x();g.update(.1,1,0);
        assert Math.abs(g.player().x()-x-Player.SPEED*.1*.45)<1e-7 : "charging commits movement without freezing it";
        advance(g,.75);assert g.charging()&&g.chargeProgress()==1&&g.projectiles().isEmpty();
        assert !g.bladePreparing()&&g.slimePreparing();
        long ready=g.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.CHARGE_READY).count();
        advance(g,1);assert ready==1&&g.drainEvents().stream().noneMatch(e->e.type()==RuinedOutpostGame.EventType.CHARGE_READY);
        g.aimSlimeCharge(0,-1);assert g.bladeAimY()==-1;
        double y=g.player().y();g.primaryRelease(0,-1);assert !g.slimePreparing();
        advance(g,.06);var wave=g.projectiles().get(0);
        assert wave.kind()==WaterProjectile.Kind.CHARGED_JET&&wave.damage()==52&&wave.directionY()==-1;
        assert Math.abs(g.player().y()-y-18)<1e-7 : "release has a single collision-safe recoil";
        assert g.drainEvents().stream().filter(e->e.type()==RuinedOutpostGame.EventType.WATER_JET_RELEASE).count()==1;
        g.primaryRelease(1,0);assert g.projectiles().size()==1 : "one hold fires once";
        assert !g.attack(1,0)&&!g.primaryPress(1,0) : "jet spends recovery";
        var partial=fixture();partial.primaryPress(1,0);advance(partial,.4);partial.primaryRelease(1,0);advance(partial,.06);
        assert partial.projectiles().get(0).damage()==38 : "partial hold has less power";
    }
    static void cancellations()throws Exception {
        for(String action:new String[]{"pause","dash","hurt","transform","death","restart","tide"}) {
            var g=fixture();g.primaryPress(1,0);advance(g,.5);
            switch(action) {
                case "pause" -> {g.pause();g.togglePause();}
                case "dash" -> {assert g.dash(0,1);}
                case "hurt" -> {
                    var m=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);
                    m.setAccessible(true);m.invoke(g,1,480.,400.);
                }
                case "transform" -> {g.player().collectIchor(100);assert g.transform();}
                case "death" -> {g.player().hurt(1000);advance(g,.01);}
                case "restart" -> g.restart();
                case "tide" -> {assert g.tideWave(1,0);}
            }
            g.primaryRelease(1,0);advance(g,.12);
            assert !g.slimePreparing()&&!g.charging() : action;
            assert g.projectiles().stream().noneMatch(w->w.kind()==WaterProjectile.Kind.CHARGED_JET) : action+" cannot leave a charged ghost shot";
        }
    }
    static void collision()throws Exception {
        var map=new RuinedOutpostMap(2);var wall=map.barriers().get(0);
        double y=wall.centerY()-Player.COLLISION_Y_OFFSET;
        var enemy=new Wisp(wall.centerX()+70,y,wall.centerX()+70,wall.centerX()+70,100);
        var g=new RuinedOutpostGame(map,new Player(wall.centerX()-100,y),List.of(enemy),new Guardian(1500,900));g.begin();
        var field=RuinedOutpostGame.class.getDeclaredField("projectiles");field.setAccessible(true);
        @SuppressWarnings("unchecked") var shots=(List<WaterProjectile>)field.get(g);
        shots.add(new WaterProjectile(g.player().x(),y,1,0,WaterProjectile.Kind.CHARGED_JET));
        var update=RuinedOutpostGame.class.getDeclaredMethod("updateWater",double.class);update.setAccessible(true);update.invoke(g,.5);
        assert shots.isEmpty()&&enemy.health()==100 : "fast jet cannot tunnel through cover";
        var light=new WaterProjectile(500,400,1,0,WaterProjectile.Kind.CUT);
        var jet=new WaterProjectile(500,400,1,0,WaterProjectile.Kind.CHARGED_JET);
        light.advance(.4);jet.advance(.4);assert !light.alive()&&jet.alive();jet.advance(.5);assert !jet.alive();
        boolean invalid=false;try{new WaterProjectile(0,0,1,0,WaterProjectile.Kind.CHARGED_JET,false,Double.NaN);}catch(IllegalArgumentException expected){invalid=true;}
        assert invalid;
    }
    static void mouseAndEffects()throws Exception {
        GameAudio.setMuted(false);
        for(boolean reduced:new boolean[]{false,true}) {
            var panel=new B2BJ(false,fixture());panel.setSize(1280,720);
            var setting=B2BJ.class.getDeclaredField("reducedEffects");setting.setAccessible(true);setting.setBoolean(panel,reduced);
            mouse(panel,MouseEvent.MOUSE_PRESSED,900,400);
            for(int i=0;i<55;i++)panel.step(1./60);
            assert panel.game().projectiles().isEmpty()&&panel.game().slimePreparing() : "holding mouse must charge, not auto-fire";
            assert GameAudio.loopRunning(GameAudio.Loop.CHARGE) : "slime charge hum must survive the animation update";
            var draw=B2BJ.class.getDeclaredMethod("drawSlimeCharge",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
            var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();draw.invoke(panel,ink,0,0);ink.dispose();
            int count=0;for(int pixel:image.getRGB(0,0,1280,720,null,0,1280))if((pixel>>>24)>0)count++;
            assert count>150 : "charge remains readable with reduced effects";
            mouse(panel,MouseEvent.MOUSE_RELEASED,900,400);panel.step(.07);
            assert panel.game().projectiles().size()==1;
            assert !GameAudio.loopRunning(GameAudio.Loop.CHARGE) : "release stops the charge loop";
            var f=B2BJ.class.getDeclaredField("feedback");f.setAccessible(true);var feedback=(CombatFeedback)f.get(panel);
            assert feedback.shakeStrength()>0 : "jet release triggers a brief camera kick";
            assert feedback.shakeStrength()<=5 : "release must not use a boss-sized shake";
            // Reduced-effects paint suppresses all camera shake; charge and projectile stay visible.
            assert panel.game().waterKind()==WaterProjectile.Kind.CHARGED_JET;
            for(var listener:panel.getFocusListeners())listener.focusLost(new java.awt.event.FocusEvent(panel,java.awt.event.FocusEvent.FOCUS_LOST));
            assert panel.game().paused()&&!panel.game().slimePreparing();
        }
    }
    static void chargeFormation()throws Exception {
        var draw=B2BJ.class.getDeclaredMethod("drawSlimeCharge",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var time=B2BJ.class.getDeclaredField("weatherTime");time.setAccessible(true);
        var setting=B2BJ.class.getDeclaredField("reducedEffects");setting.setAccessible(true);
        for(boolean reduced:new boolean[]{false,true}) {
            var game=fixture();var panel=new B2BJ(false,game);setting.setBoolean(panel,reduced);
            game.primaryPress(1,0);advance(game,.04);
            int[] early=chargePixels(panel,draw);
            advance(game,.8);int[] full=chargePixels(panel,draw);
            long earlyInk=java.util.Arrays.stream(early).filter(p->(p>>>24)>0).count();
            long fullInk=java.util.Arrays.stream(full).filter(p->(p>>>24)>0).count();
            assert fullInk>earlyInk*1.5 : "sparse water must visibly gather into a solid orb";
            for(int[] aim:new int[][]{{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}}) {
                game.aimSlimeCharge(aim[0],aim[1]);int[] pixels=chargePixels(panel,draw);
                double sum=0;int bright=0;
                for(int i=0;i<pixels.length;i++)if((pixels[i]>>>24)>200&&(pixels[i]>>16&255)>100&&(pixels[i]>>8&255)>220) {
                    sum+=((i%420-200)*aim[0]+(i/420-212)*aim[1])/Math.hypot(aim[0],aim[1]);bright++;
                }
                assert bright>50&&sum/bright>60 : "charge core must clear the face in every aim direction";
            }
            int[] still=chargePixels(panel,draw);time.setDouble(panel,.27);
            boolean animated=!java.util.Arrays.equals(still,chargePixels(panel,draw));
            assert animated!=reduced : "full charge should flow; reduced effects should settle";
        }
    }
    static int[] chargePixels(B2BJ panel,java.lang.reflect.Method draw)throws Exception {
        var image=new BufferedImage(420,420,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();
        draw.invoke(panel,ink,300,200);ink.dispose();return image.getRGB(0,0,420,420,null,0,420);
    }
    static void mouse(B2BJ panel,int event,int x,int y) {
        panel.dispatchEvent(new MouseEvent(panel,event,0,0,x,y,1,false,MouseEvent.BUTTON1));
    }
}
