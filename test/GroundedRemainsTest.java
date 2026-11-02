import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Owner 2026-10-29: remains must lie ON the ground ("they're floating": armour's shadow sat 25 px under it) at rest and
 * while Beelzebub drags them, the Spitter leaves its purple goo carcass and lies still, and the transformation is drawn
 * from game time on the 2x grid: the slime's own pixels stretching up with no shape popping in, his whole jelly
 * silhouette after the flash (no slivers), landing on Rainoray's own pixels.
 */
public final class GroundedRemainsTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        for(var kind:new CampaignEnemy.Kind[]{CampaignEnemy.Kind.OUTPOST_SCOUT,CampaignEnemy.Kind.OUTPOST_GUARD,CampaignEnemy.Kind.OUTPOST_SPITTER})
            liesOnItsShadow(kind);
        carcassSettlesStill();
        transformationOnTheGrid();
        System.out.println("GroundedRemainsTest passed: remains on their shadows at rest and while dragged, goo carcass, 2x transformation");
    }

    private static void liesOnItsShadow(CampaignEnemy.Kind kind) throws Exception {
        var game=killed(kind);var panel=panel(game);
        advance(game,.8);
        grounded(remains(panel,game),kind+" at rest");
        game.player().collectIchor(100);assert game.transform();game.player().setUnlimitedBlade(true);
        assert game.beelzebub(1,0);
        while(game.absorptionProgress()<.5)game.update(.01,0,0);
        grounded(remains(panel,game),kind+" while dragged");
        while(game.absorptionProgress()<BeelzebubFx.GONE)game.update(.01,0,0);
        assert pixels(remains(panel,game),0)==0 : kind+": swallowed by the time the hand closes";
    }

    /** The lowest pixels of a body sit on its shadow: the shadow shows just under them, not a gap below. */
    private static void grounded(BufferedImage image,String what) {
        int body=-1,shadow=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int a=image.getRGB(x,y)>>>24;
            if(a==255)body=y;
            if(a>0)shadow=y;
        }
        assert body>0&&shadow>body&&shadow-body<=8 : what+": body ends at "+body+", its shadow at "+shadow;
    }

    private static void carcassSettlesStill() throws Exception {
        var game=killed(CampaignEnemy.Kind.OUTPOST_SPITTER);var panel=panel(game);
        advance(game,.4);var settled=remains(panel,game);
        advance(game,.8);var later=remains(panel,game);
        assert java.util.Arrays.equals(settled.getRGB(0,0,1280,720,null,0,1280),later.getRGB(0,0,1280,720,null,0,1280))
                : "after its slump the carcass lies still";
        assert pixels(later,0xff583daa)>0 : "the Spitter leaves the purple goo carcass";
        assert pixels(later,0xffac6631)==0 : "not its knocked-over barrel";
    }

    private static void transformationOnTheGrid() {
        var slime=B2BJ.loadImage("assets/characters/slime/slime_idle.png");
        var hero=B2BJ.loadImage("assets/characters/blade/rainoray_idle.png");
        int row=BladeAnimation.directionRow(1,0);
        for(double t:new double[]{.05,.15,.3,.45,.6,.75}) {
            var image=transform(t,slime,hero,row);
            for(int y=0;y<image.getHeight();y+=2)for(int x=0;x<image.getWidth();x+=2) {
                int c=image.getRGB(x,y);
                assert c==image.getRGB(x+1,y)&&c==image.getRGB(x,y+1)&&c==image.getRGB(x+1,y+1) : "every pixel 2x2 at "+t;
            }
            assert java.util.Arrays.equals(raw(image),raw(transform(t,slime,hero,row))) : "a function of progress alone";
        }
        double substep=1/120./TransformationAnimation.DURATION;
        assert !java.util.Arrays.equals(raw(transform(.3,slime,hero,row)),raw(transform(.3+substep,slime,hero,row)))
                : "the goo moves between 120 Hz substeps (true slow motion)";
        int above=0;var rising=transform(.3,slime,hero,row);
        for(int y=0;y<100;y++)for(int x=0;x<rising.getWidth();x++)if(rising.getRGB(x,y)>>>24!=0)above++;
        assert above>40 : "the goo column rises far above the slime";
        var real=cellAt2x(hero,0,row*80,80,TransformFx.HERO_X,TransformFx.HERO_Y);
        assert differ(transform(0,slime,hero,row),cellAt2x(slime,0,0,48,TransformFx.SLIME_X,TransformFx.SLIME_Y))==0
                : "it starts on the slime's own pixels";
        // judged 2026-10-29: the slime's own pixels stretch up, its star going up with it (no bleached dome, no new
        // shape popping out of it in one substep) ...
        var stretched=transform(.2,slime,hero,row);
        assert pixels(stretched,0xffdba741)>=4*8&&top(stretched)<=top(transform(0,slime,hero,row))-2*8
                : "its own pixels, gold star and all, stretch up";
        boolean[] last=mask(transform(0,slime,hero,row));
        for(double t=substep;t<TransformFx.FLASH;t+=substep) {
            boolean[] now=mask(transform(t,slime,hero,row));
            int changed=0,union=0;
            for(int i=0;i<now.length;i++) {if(now[i]!=last[i])changed++;if(now[i]||last[i])union++;}
            assert changed<=union*.3 : "the goo grows a little each substep, no new shape at once: "+changed+" of "+union+" at "+t;
            last=now;
        }
        // ... and after the white flash his whole silhouette is jelly at once (no slivers), landing exactly on him
        for(double t=TransformFx.SWAP+substep/2;t<.5;t+=substep)
            assert java.util.Arrays.equals(mask(transform(t,slime,hero,row)),mask(real)) : "his whole jelly silhouette at "+t;
        assert differ(transform(.95,slime,hero,row),real)<=4*40 : "it is him but for the sword's glint";
        assert differ(transform(.999,slime,hero,row),real)==0 : "it lands on his real idle pixels";
    }

    private static BufferedImage cellAt2x(BufferedImage sheet,int sx,int sy,int size,int x,int y) {
        var image=new BufferedImage(TransformFx.W*2,TransformFx.H*2,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        g.drawImage(sheet,x*2,y*2,x*2+size*2,y*2+size*2,sx,sy,sx+size,sy+size,null);
        g.dispose();
        return image;
    }
    private static int differ(BufferedImage a,BufferedImage b) {
        int[] pa=raw(a),pb=raw(b);int count=0;
        for(int i=0;i<pa.length;i++)if((pa[i]>>>24==0?0:pa[i])!=(pb[i]>>>24==0?0:pb[i]))count++;
        return count;
    }
    private static int top(BufferedImage image) {
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)>>>24!=0)return y;
        return image.getHeight();
    }
    private static boolean[] mask(BufferedImage image) {
        int[] px=raw(image);var out=new boolean[px.length];
        for(int i=0;i<px.length;i++)out[i]=px[i]>>>24!=0;
        return out;
    }

    private static BufferedImage transform(double t,BufferedImage slime,BufferedImage hero,int row) {
        var image=new BufferedImage(TransformFx.W*2,TransformFx.H*2,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        TransformFx.draw(g,0,0,t,slime,0,0,false,hero,0,row*80,80,false,false);
        g.dispose();
        return image;
    }
    private static int[] raw(BufferedImage image) { return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()); }

    @SuppressWarnings("unchecked")
    private static RuinedOutpostGame killed(CampaignEnemy.Kind kind) throws Exception {
        var game=new RuinedOutpostGame(new RuinedOutpostMap(),new Player(300,300),List.of(),new Guardian(1000,650));
        game.begin();
        var enemy=new CampaignEnemy(kind,520,300);
        ((List<Wisp>)field(game,"scouts")).add(enemy.body());
        ((Map<Wisp,CampaignEnemy>)field(game,"campaignEnemies")).put(enemy.body(),enemy);
        enemy.body().hurt(enemy.body().health());
        var hit=RuinedOutpostGame.class.getDeclaredMethod("afterScoutHit",Wisp.class,boolean.class);
        hit.setAccessible(true);hit.invoke(game,enemy.body(),true);
        ((List<?>)field(game,"drops")).clear();
        assert game.corpses().size()==1&&game.corpses().get(0).kind()==kind;
        return game;
    }
    private static B2BJ panel(RuinedOutpostGame game) throws Exception {
        var panel=new B2BJ(false,game);
        var reduced=B2BJ.class.getDeclaredField("reducedEffects");reduced.setAccessible(true);reduced.set(panel,true);   // no drag dust
        return panel;
    }
    /** The remains alone on a clear canvas, the body centred. */
    private static BufferedImage remains(B2BJ panel,RuinedOutpostGame game) throws Exception {
        var draw=B2BJ.class.getDeclaredMethod("drawCorpses",java.awt.Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        draw.invoke(panel,g,240,0);g.dispose();
        return image;
    }
    private static long pixels(BufferedImage image,int colour) {
        return java.util.Arrays.stream(raw(image)).filter(c->colour==0?c>>>24!=0:c==colour).count();
    }
    private static Object field(Object owner,String name) throws Exception {
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);
    }
    private static void advance(RuinedOutpostGame game,double seconds) { for(double t=seconds;t>1e-8;t-=.01)game.update(Math.min(.01,t),0,0); }
}
