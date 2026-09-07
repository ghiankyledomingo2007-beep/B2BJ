import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public final class MotionPolishRenderingTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        String check=args.length==0?"all":args[0];
        if(check.equals("all")||check.equals("cadence")) {
            var run=new BladeAnimation();run.update(1,0,false,0);run.update(1,0,false,.5);
            assert run.frame()==7 : "running cadence must reach pose7 in half a second";
            run.update(0,0,false,.01);assert run.action()==BladeAnimation.Action.IDLE&&run.frame()==0;
        }
        if(check.equals("all")||check.equals("engulf")) {
            var sheet=B2BJ.loadImage("assets/characters/slime/slime_engulf.png");
            int broad=0;
            for(int frame=5;frame<=11;frame++) {
                var box=bounds(sheet.getSubimage(frame*80,80,80,80));
                if(box[2]-box[0]>=46&&(box[2]-box[0])>1.65*(box[3]-box[1]))broad++;
            }
            assert broad>=3 : "side engulf needs sustained broad low body coverage, not an upright neck";
        }
        if(check.equals("all")||check.equals("carry")) {
            var run=B2BJ.loadImage("assets/characters/blade/rainoray_run.png");
            for(int row=0;row<3;row++) {
                int low=80,high=-1;
                for(int frame=0;frame<8;frame++) {
                    int top=80;
                    for(int y=0;y<80;y++)for(int x=0;x<80;x++) {
                        int p=run.getRGB(frame*80+x,row*80+y);
                        if((p>>>24)>0&&(p>>16&255)>185&&(p>>8&255)>185&&(p&255)>185)top=Math.min(top,y);
                    }
                    assert top<80 : "mask and blade highlights remain visible";
                    low=Math.min(low,top);high=Math.max(high,top);
                }
                assert high-low<=5 : "run must carry the blade steadily, not redraw it every loop: row "+row;
            }
        }
        if(check.equals("all")||check.equals("effects")) {
            for(String name:new String[]{"tide_crest","tide_release","tide_break","tide_foam","warden_impact"}) {
                int cell=name.equals("tide_crest")?48:name.equals("tide_foam")?32:64;
                var sheet=B2BJ.loadImage("assets/effects/"+name+".png");
                assert sheet!=null&&sheet.getWidth()==cell*8&&sheet.getHeight()==cell : "new native effect: "+name;
                CombatArtTest.check("assets/effects/"+name+".png",cell,cell,8,1,8,true);
            }
            var boss=B2BJ.loadImage("assets/characters/guardian/warden_motion.png");
            assert boss!=null&&boss.getWidth()==512&&boss.getHeight()==448 : "seven boss action rows";
            CombatArtTest.check("assets/characters/guardian/warden_motion.png",64,64,8,7,8);
            B2BJ panel=new B2BJ(false);panel.game().begin();panel.game().player().relocate(400,300);
            var events=B2BJ.class.getDeclaredMethod("handleEvents",List.class);events.setAccessible(true);
            var draw=B2BJ.class.getDeclaredMethod("drawImpacts",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
            events.invoke(panel,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.TIDE_RELEASE,200,180,true)));
            var burst=new BufferedImage(640,360,BufferedImage.TYPE_INT_ARGB);var g=burst.createGraphics();draw.invoke(panel,g,0,0);g.dispose();
            var box=bounds(burst);
            assert box[2]>box[0]&&box[0]>=136&&box[2]<=264&&box[1]>=116&&box[3]<=244 : "release burst anchored to event, not moving player";
            panel.step(.6);
            var expired=new BufferedImage(640,360,BufferedImage.TYPE_INT_ARGB);g=expired.createGraphics();draw.invoke(panel,g,0,0);g.dispose();
            assert bounds(expired)[2]<0 : "source burst expires";
            var water=B2BJ.class.getDeclaredMethod("drawWater",Graphics2D.class,int.class,int.class,WaterProjectile.class);water.setAccessible(true);
            for(int[] direction:new int[][]{{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}}) {
                var wave=new WaterProjectile(160,160,direction[0],direction[1],true);wave.advance(.1);
                var image=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);g=image.createGraphics();water.invoke(panel,g,0,0,wave);g.dispose();
                assert bounds(image)[2]>=0 : "visible Tide in every aim direction";
                for(int y=0;y<320;y+=2)for(int x=0;x<320;x+=2) {
                    int p=image.getRGB(x,y);
                    assert p==image.getRGB(x+1,y)&&p==image.getRGB(x,y+1)&&p==image.getRGB(x+1,y+1) : "native 2x water pixels";
                }
            }
            // A right-authored leading lip must not inherit the legacy diagonal correction.
            var crestField=B2BJ.class.getDeclaredField("tideCrestSheet");crestField.setAccessible(true);
            var marker=new BufferedImage(48*8,48,BufferedImage.TYPE_INT_ARGB);
            for(int frame=0;frame<8;frame++)marker.setRGB(frame*48+36,24,0xffff00ff);
            crestField.set(panel,marker);
            var straight=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);g=straight.createGraphics();
            water.invoke(panel,g,0,0,new WaterProjectile(160,160,1,0,true));g.dispose();
            assert straight.getRGB(184,160)==0xffff00ff : "Tide leading lip faces travel, not a legacy diagonal";
            var impacts=B2BJ.class.getDeclaredField("impacts");impacts.setAccessible(true);
            for(String type:new String[]{"TIDE_RELEASE","TIDE_IMPACT","TIDE_DISSIPATE"}) {
                ((List<?>)impacts.get(panel)).clear();
                events.invoke(panel,List.of(new RuinedOutpostGame.Event(RuinedOutpostGame.EventType.valueOf(type),200,180,true)));
                assert ((List<?>)impacts.get(panel)).size()==1 : "one feedback effect per Tide event";
                var effect=((List<?>)impacts.get(panel)).get(0);
                var kind=effect.getClass().getDeclaredField("kind");kind.setAccessible(true);
                String expected=type.equals("TIDE_IMPACT")?"TIDE":type.equals("TIDE_DISSIPATE")?"TIDE_FOAM":"TIDE_RELEASE";
                assert kind.get(effect).toString().equals(expected) : "release, collision and expiry use distinct feedback";
            }
            assert GameAudio.synthesize(GameAudio.Cue.valueOf("TIDE_RELEASE")).length>GameAudio.synthesize(GameAudio.Cue.BLOB_ATTACK).length : "distinct heavier cast cue";
        }
        System.out.println("MotionPolishRenderingTest "+check+" passed");
    }
    private static int[] bounds(BufferedImage image) {
        int[] box={image.getWidth(),image.getHeight(),-1,-1};
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>0) {
            box[0]=Math.min(box[0],x);box[1]=Math.min(box[1],y);box[2]=Math.max(box[2],x);box[3]=Math.max(box[3],y);
        }
        return box;
    }
}
