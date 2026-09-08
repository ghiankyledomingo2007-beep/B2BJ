import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class SlimeDepthRenderingTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        if(args.length>0&&args[0].equals("labels")) {readableTraits();System.out.println("Trait labels passed");return;}
        releaseFeedback();
        assetCoverage();
        directionalFinishers();
        projectilePixels();
        readableTraits();
        System.out.println("SlimeDepthRenderingTest passed: release timing, directional art, pixel grid, trait face/HUD preservation");
    }
    private static void releaseFeedback() {
        var game=fixture();
        for(int i=0;i<2;i++) {assert game.attack(1,0);game.update(.5,0,0);game.drainEvents();}
        assert game.attack(1,0);
        assert game.drainEvents().stream().noneMatch(e->e.type().name().equals("FINISHER_RELEASE"));
        game.update(.12,0,0);
        assert game.drainEvents().stream().filter(e->e.type().name().equals("FINISHER_RELEASE")).count()==1
                : "finisher needs a distinct cue at physical release, not input acceptance";
        var cancel=fixture();
        for(int i=0;i<2;i++) {assert cancel.attack(1,0);cancel.update(.5,0,0);}
        cancel.drainEvents();assert cancel.attack(1,0)&&cancel.dash(0,1);cancel.update(.5,0,0);
        assert cancel.drainEvents().stream().noneMatch(e->e.type().name().equals("FINISHER_RELEASE"));
    }
    private static void assetCoverage() {
        for(String name:new String[]{"tide_front","water_finisher","water_finisher_impact","guard_membrane","spitter_current","../characters/slime/slime_finisher"}) {
            int cell=name.startsWith("..")?64:name.contains("finisher")?48:64;
            String path=name.startsWith("..")?"assets/characters/slime/slime_finisher.png":"assets/effects/"+name+".png";
            var sheet=B2BJ.loadImage(path);
            assert sheet!=null&&sheet.getWidth()==cell*8&&sheet.getHeight()==(name.startsWith("..")?192:cell)
                    : "missing native eight-frame art: "+path;
            for(int row=0;row<sheet.getHeight()/cell;row++) {
                var poses=new HashSet<Integer>();
                for(int f=0;f<8;f++) {
                    int[] pixels=sheet.getRGB(f*cell,row*cell,cell,cell,null,0,cell);
                    poses.add(Arrays.hashCode(pixels));int visible=0;
                    for(int pixel:pixels)if((pixel>>>24)>0)visible++;
                    assert visible>4&&visible<cell*cell*.85 : "transparent readable VFX: "+path;
                }
                assert poses.size()>=6 : "not a repeated still: "+path+" row "+row;
            }
            if(name.startsWith("..")) {
                var reference=B2BJ.loadImage("assets/characters/slime/slime_cast.png");
                for(int row=0;row<3;row++)for(int f=0;f<8;f++) {
                    int bottom=-1,visible=0;
                    for(int y=0;y<64;y++)for(int x=0;x<64;x++)if((sheet.getRGB(f*64+x,row*64+y)>>>24)>0) {
                        visible++;bottom=Math.max(bottom,y);
                        assert x>0&&x<63&&y>0&&y<63 : "finisher motion must have unclipped transparent margin";
                    }
                    assert bottom==55 : "every finisher pose stays planted at the original ground anchor";
                    assert visible>950&&visible<1750 : "extra canvas must not enlarge or hollow out the slime";
                }
                for(int row=0;row<3;row++)for(int y=0;y<48;y++)for(int x=0;x<48;x++)
                    assert sheet.getRGB(7*64+x+8,row*64+y+8)==reference.getRGB(x,row*48+y)
                            : "settle pose must return to exact native character pixels";
            }
        }
    }
    private static void projectilePixels() throws Exception {
        var panel=new B2BJ(false,fixture());
        var method=B2BJ.class.getDeclaredMethod("drawWater",Graphics2D.class,int.class,int.class,WaterProjectile.class);
        method.setAccessible(true);
        for(var kind:WaterProjectile.Kind.values())for(int[] aim:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}) {
            var wave=new WaterProjectile(700,500,aim[0],aim[1],kind,true);
            var frame=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=frame.createGraphics();
            method.invoke(panel,ink,0,0,wave);ink.dispose();int visible=0;
            for(int y=380;y<620;y+=2)for(int x=580;x<820;x+=2) {
                int pixel=frame.getRGB(x,y);
                if((pixel>>>24)>0)visible++;
                assert pixel==frame.getRGB(x+1,y)&&pixel==frame.getRGB(x,y+1)&&pixel==frame.getRGB(x+1,y+1)
                        : "new water art preserves native 2x pixels after rotation";
            }
            assert visible>20 : "every attack heading has visible art: "+kind;
        }
    }
    private static void directionalFinishers() throws Exception {
        var panel=new B2BJ(false,fixture());
        var marked=new BufferedImage(512,192,BufferedImage.TYPE_INT_ARGB);
        int[] colors={0xffff00ff,0xff11dd55,0xff2255ff};
        var ink=marked.createGraphics();
        for(int row=0;row<3;row++){ink.setColor(new java.awt.Color(colors[row],true));ink.fillRect(0,row*64,512,64);}ink.dispose();
        set(panel,"slimeFinisherSheet",marked);
        set(panel.game(),"waterKind",WaterProjectile.Kind.FINISHER);
        var field=B2BJ.class.getDeclaredField("slimeAnimation");field.setAccessible(true);
        var animation=(SlimeAnimation)field.get(panel);
        for(int[] aim:new int[][]{{0,1,0},{-1,0,1},{1,0,1},{0,-1,2}}) {
            animation.face(aim[0],aim[1]);animation.attack();
            assert draw(panel,"drawSlime").getRGB(500,400)==colors[aim[2]] : "finisher uses correct directional row";
        }
    }
    private static void readableTraits() throws Exception {
        var panel=new B2BJ(false,fixture());var bare=draw(panel,"drawPlayer");var hud=draw(panel,"drawPlayerHud");
        for(var trait:new Player.Trait[]{Player.Trait.MEMBRANE,Player.Trait.JET}) {
            panel.game().player().gainTrait(trait);var effect=draw(panel,"drawPlayer");int added=0;
            for(int y=0;y<720;y++)for(int x=0;x<1280;x++) {
                if((bare.getRGB(x,y)>>>24)==255)assert effect.getRGB(x,y)==bare.getRGB(x,y) : "trait cannot cover slime face/body";
                else if(effect.getRGB(x,y)!=bare.getRGB(x,y))added++;
            }
            assert added>40 : "trait has a visible world cue";
            var activeHud=draw(panel,"drawPlayerHud");int changed=0;
            for(int y=538;y<592;y++)for(int x=478;x<810;x++)if(hud.getRGB(x,y)!=activeHud.getRGB(x,y))changed++;
            assert changed>100 : "trait name and remaining time need a HUD slot";
            if(trait==Player.Trait.JET) {
                var expected=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=expected.createGraphics();
                var text=B2BJ.class.getDeclaredMethod("pixelText",Graphics2D.class,String.class,int.class,int.class,int.class,java.awt.Color.class);
                text.setAccessible(true);text.invoke(panel,ink,"SHOT SPEED 1.3X",526,571,1,new java.awt.Color(191,203,209));ink.dispose();
                for(int y=571;y<578;y++)for(int x=526;x<616;x++)if((expected.getRGB(x,y)>>>24)>0)
                    assert activeHud.getRGB(x,y)==expected.getRGB(x,y) : "Jet description needs supported, unambiguous pixel-font glyphs";
            }
            for(int y=16;y<144;y++)for(int x=16;x<336;x++)assert activeHud.getRGB(x,y)==hud.getRGB(x,y)
                    : "trait UI must preserve health/Ichor";
        }
    }
    private static BufferedImage draw(B2BJ panel,String name) throws Exception {
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var ink=image.createGraphics();
        var method=name.equals("drawPlayerHud")?B2BJ.class.getDeclaredMethod(name,Graphics2D.class)
                :B2BJ.class.getDeclaredMethod(name,Graphics2D.class,int.class,int.class);
        method.setAccessible(true);
        if(name.equals("drawPlayerHud"))method.invoke(panel,ink);else method.invoke(panel,ink,0,0);
        ink.dispose();return image;
    }
    private static void set(Object object,String name,Object value) throws Exception {
        var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    private static RuinedOutpostGame fixture() {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(500,400),List.of(),new Guardian(1500,900));
        game.begin();return game;
    }
}
