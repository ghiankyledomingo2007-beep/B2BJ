import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public final class SlimeAttackRenderingTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMuted(true);
        for(String name:new String[]{"water_slash","tide_wave","water_splash","water_charge","water_wake","tide_impact"}) {
            var sheet=B2BJ.loadImage("assets/effects/"+name+".png");
            int cell=name.startsWith("tide")?48:32;
            assert sheet!=null && sheet.getWidth()==cell*8 && sheet.getHeight()==cell : "native eight-frame effect: "+name;
            var poses=new java.util.HashSet<Integer>();
            for(int f=0;f<8;f++) {
                int[] pixels=sheet.getRGB(f*cell,0,cell,cell,null,0,cell);
                poses.add(java.util.Arrays.hashCode(pixels));
                int visible=0;
                for(int y=0;y<cell;y++)for(int x=0;x<cell;x++)if((pixels[y*cell+x]>>>24)>0) {
                    visible++;
                    assert x>0&&y>0&&x<cell-1&&y<cell-1 : "clipped water frame: "+name;
                }
                assert visible>0&&visible<cell*cell*0.8 : "effect needs transparent gaps: "+name;
            }
            assert poses.size()>=6 : "effect must animate, not repeat a still: "+name;
        }
        var draw=B2BJ.class.getDeclaredMethod("drawSlime",Graphics2D.class,int.class,int.class);draw.setAccessible(true);
        var water=B2BJ.class.getDeclaredMethod("drawWater",Graphics2D.class,int.class,int.class,WaterProjectile.class);water.setAccessible(true);
        for(int[] direction:new int[][]{{1,0},{-1,0},{0,-1},{0,1}}) {
            B2BJ panel=new B2BJ(false);panel.setSize(panel.getPreferredSize());panel.game().begin();
            panel.game().player().relocate(300,300);
            int mx=300+direction[0]*160,my=300+direction[1]*160;
            mouse(panel,MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON1,mx,my);
            mouse(panel,MouseEvent.MOUSE_RELEASED,MouseEvent.BUTTON1,mx,my);
            panel.step(0.04);
            assert visiblePixels(render(panel,water))==0 : "windup precedes release";
            panel.step(0.16);
            BufferedImage body=render(panel,draw);
            for(int y=0;y<320;y++)for(int x=0;x<320;x++)
                if(Math.abs(x-160)>48||Math.abs(y-160)>48)
                    assert (body.getRGB(x,y)>>>24)==0 : "no attached fist or stretched melee appendage";
            BufferedImage wave=render(panel,water);
            assert visiblePixels(wave)>80 : "water must be a visible curved sprite in every direction";
            for(int y=0;y<320;y+=2)for(int x=0;x<320;x+=2) {
                int pixel=wave.getRGB(x,y);
                assert pixel==wave.getRGB(x+1,y)&&pixel==wave.getRGB(x,y+1)&&pixel==wave.getRGB(x+1,y+1)
                        : "rotated water keeps native 2x pixel grid";
            }
            panel.step(0.4);
            mouse(panel,MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON3,mx,my);
            assert panel.game().tideCooldown()>4 : "right click binds Tide Wave";
            panel.step(0.22);
            assert panel.game().projectiles().stream().anyMatch(WaterProjectile::heavy);
            assert visiblePixels(render(panel,water))>80;
        }
        comboFrames(water);
        GameAudio.setMuted(false);
        System.out.println("SlimeAttackRenderingTest passed");
    }
    private static void comboFrames(java.lang.reflect.Method draw) throws Exception {
        B2BJ panel=new B2BJ(false);panel.game().begin();
        panel.game().player().relocate(300,300);
        int[] hashes=new int[3];
        int[] visible=new int[3];
        BufferedImage firstFrame=null;
        for(int cut=0;cut<3;cut++) {
            assert panel.game().attack(1,0);
            panel.game().update(.2,0,0);
            var waves=panel.game().projectiles();
            var wave=waves.get(waves.size()-1);
            var frame=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);
            var g=frame.createGraphics();
            draw.invoke(panel,g,(int)wave.x()-160,(int)wave.y()-160,wave);g.dispose();
            hashes[cut]=java.util.Arrays.hashCode(frame.getRGB(0,0,320,320,null,0,320));
            visible[cut]=visiblePixels(frame);
            if(cut==0)firstFrame=frame;
            if(cut==1) {
                int changed=0,union=0;
                for(int y=0;y<320;y++)for(int x=0;x<320;x++) {
                    boolean a=(firstFrame.getRGB(x,y)>>>24)>0,b=(frame.getRGB(x,y)>>>24)>0;
                    if(a||b)union++;
                    if(a!=b)changed++;
                }
                assert changed>union*.25 : "return cut silhouette must change substantially, not just a few pixels";
            }
            panel.game().update(.3,0,0);
        }
        assert hashes[0]!=hashes[1] : "return cut needs a visibly different sprite pose";
        assert hashes[1]!=hashes[2]&&hashes[0]!=hashes[2] : "finisher must read differently from basic cuts";
        assert visible[2]>visible[0]*1.25 : "finisher's two cuts must not overlap into one basic slash";
    }
    private static void mouse(B2BJ panel,int event,int button,int x,int y) {
        panel.dispatchEvent(new MouseEvent(panel,event,0,0,x,y,1,false,button));
    }
    private static BufferedImage render(B2BJ panel,java.lang.reflect.Method draw) throws Exception {
        var image=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        if(draw.getParameterCount()==4)for(var wave:panel.game().projectiles())draw.invoke(panel,g,140,140,wave);
        else draw.invoke(panel,g,140,140);
        g.dispose();return image;
    }
    private static int visiblePixels(BufferedImage image) {
        int count=0;
        for(int y=0;y<320;y++)for(int x=0;x<320;x++)if((image.getRGB(x,y)>>>24)>0)count++;
        return count;
    }
}
