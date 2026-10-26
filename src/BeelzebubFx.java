import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Native-pixel inward currents: the bodies show the range without a bright screen-wide warning ring. */
final class BeelzebubFx {
    static final BufferedImage ICON=icon();
    private static final Color RIM=new Color(147,99,193),LIGHT=new Color(219,171,237);
    private BeelzebubFx() { }

    static void draw(Graphics2D canvas,RuinedOutpostGame game,int cameraX,int cameraY,boolean reduced) {
        if(!game.beelzebubActive())return;
        var player=game.player();double p=game.absorptionProgress();
        var g=(Graphics2D)canvas.create();Ui.crisp(g);
        g.translate(Math.round(player.x()/2)*2-cameraX,Math.round(player.y()/2)*2-cameraY);g.scale(2,2);
        double length=Math.max(1,Math.hypot(game.bladeAimX(),game.bladeAimY()));
        int cx=(int)(game.bladeAimX()/length*26),cy=-16+(int)(game.bladeAimY()/length*12);
        double motion=reduced?0:p*8;
        for(var corpse:game.corpses())if(game.beelzebubTarget(corpse)) {
            double sx=(corpse.x()-player.x())/2,sy=(corpse.y()-player.y())/2+10;
            int bends=reduced?1:3;
            for(int lane=0;lane<bends;lane++) {
                int lastX=(int)sx,lastY=(int)sy;
                for(int segment=1;segment<=24;segment++) {
                    double t=segment/24.,curl=Math.sin(t*Math.PI)*(lane-1)*10;
                    int x=(int)Math.round(sx+(cx-sx)*t+curl),y=(int)Math.round(sy+(cy-sy)*t-curl*.6);
                    g.setColor(new Color(126+lane*22,85+lane*10,171+lane*20,reduced?65:35+segment*3));
                    g.drawLine(lastX,lastY,x,y);lastX=x;lastY=y;
                }
                if(!reduced)for(int mote=0;mote<5;mote++) {
                    double t=(p*2.2+mote*.2+lane*.07)%1;
                    int x=(int)(sx+(cx-sx)*t+Math.sin(t*Math.PI)*(lane-1)*10);
                    int y=(int)(sy+(cy-sy)*t-Math.sin(t*Math.PI)*(lane-1)*6);
                    g.setColor(mote%2==0?LIGHT:RIM);g.fillRect(x,y,2,1);
                }
            }
        }
        int radius=reduced?23:12+(int)(Math.sin(Math.PI*p)*14);
        g.setColor(new Color(53,26,78,55));g.fillOval(cx-radius-9,cy-radius-5,(radius+9)*2,(radius+5)*2);
        g.setColor(new Color(10,7,20,240));g.fillOval(cx-radius,cy-radius*3/4,radius*2,radius*3/2);
        for(int ring=0;ring<3;ring++) {
            int r=radius+ring*3;
            g.setColor(ring==0?LIGHT:ring==1?RIM:new Color(82,53,122,140));
            for(int arm=0;arm<3;arm++)g.drawArc(cx-r,cy-r*3/4,r*2,r*3/2,
                    (int)(motion*50)+arm*120+ring*28,70);
        }
        for(int arm=0;arm<(reduced?2:4);arm++) {
            int lastX=cx,lastY=cy;
            for(int step=0;step<20;step++) {
                double r=radius*step/20.,a=arm*Math.PI/2+motion-r*.15;
                int x=cx+(int)(Math.cos(a)*r),y=cy+(int)(Math.sin(a)*r*.7);
                g.setColor(step<8?new Color(48,27,71):RIM);g.drawLine(lastX,lastY,x,y);lastX=x;lastY=y;
            }
        }
        g.setColor(new Color(5,4,12));g.fillOval(cx-6,cy-4,12,8);
        g.dispose();
    }

    private static BufferedImage icon() {
        var source=B2BJ.loadImage("assets/ui/consume.png");
        var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        if(source==null)return image;
        for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
            int c=source.getRGB(x,y),brightness=Math.max(c>>16&255,Math.max(c>>8&255,c&255));
            if(c>>>24==0)continue;
            image.setRGB(x,y,new Color(Math.min(238,brightness),brightness*2/3,Math.min(255,brightness+22),c>>>24).getRGB());
        }
        return image;
    }
}
