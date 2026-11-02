import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/** A hand-held devouring vortex; black flame silhouettes carry the motion at native pixel scale. */
final class BeelzebubFx {
    static final BufferedImage ICON=icon();
    private static final Color VOID=new Color(8,5,17),RIM=new Color(105,40,174),LIGHT=new Color(190,103,237);
    private BeelzebubFx() { }

    /** The hand's vortex in native px from the player's position, along the aim. */
    static int[] hand(RuinedOutpostGame game) {
        double length=Math.max(1,Math.hypot(game.bladeAimX(),game.bladeAimY()));
        return new int[]{(int)(game.bladeAimX()/length*34),-10+(int)(game.bladeAimY()/length*12)};
    }

    /** The share of the cast after which the caught bodies leave the ground, and by which they are gone into the hand. */
    static final double SWALLOW=.82,GONE=.9;

    /**
     * Owner 2026-10-29, "they're floating": caught remains are dragged along the ground by the arms to the spot under
     * the hand, and only lift into it as they are swallowed. {dx, dy, rise, left}: the body's offset on the ground from
     * where the game holds it (world px), how far it is lifted off that ground, and how much of it is still there.
     */
    static double[] drag(RuinedOutpostGame game,RuinedOutpostGame.Corpse corpse) {
        double p=game.absorptionProgress(),pull=game.beelzebubPull();
        int[] hand=hand(game);
        double up=Math.max(0,Math.min(1,(p-SWALLOW)/(GONE-SWALLOW)));
        return new double[]{pull*hand[0]*2,pull*(68+hand[1]*2-ground(corpse)),Math.pow(up,2.5)*76,1-Math.max(0,Math.min(1,(p-GONE+.03)/.03))};
    }

    /** How far below a body's position its ground line is drawn (world px): armour falls at its feet, the rest lie
     *  on the floor line of their old 96 px cell, the Spitter's carcass puddle a little nearer. */
    static int ground(RuinedOutpostGame.Corpse corpse) {
        var kind=corpse.kind();
        return armoured(kind)?22:kind==CampaignEnemy.Kind.OUTPOST_SPITTER?52:48;
    }
    /** How high a lying body's middle sits above its ground line (world px): where the arm takes hold. */
    static int middle(RuinedOutpostGame.Corpse corpse) {
        var kind=corpse.kind();
        return armoured(kind)?16:kind==CampaignEnemy.Kind.OUTPOST_SPITTER?22:20;
    }
    private static boolean armoured(CampaignEnemy.Kind kind) {
        return kind!=null&&(CampaignRenderer.knightBody(kind)||kind==CampaignEnemy.Kind.OUTPOST_GUARD);
    }

    static void draw(Graphics2D canvas,RuinedOutpostGame game,int cameraX,int cameraY,boolean reduced) {
        if(!game.beelzebubActive())return;
        var player=game.player();double p=game.absorptionProgress();
        var g=(Graphics2D)canvas.create();Ui.crisp(g);
        g.translate(Math.round(player.x()/2)*2-cameraX,Math.round(player.y()/2)*2-cameraY);g.scale(2,2);
        int[] hand=hand(game);
        int cx=hand[0],cy=hand[1];
        double growth=Math.min(1,p/.18),collapse=Math.min(1,(1-p)/.18);
        double strength=growth*collapse,motion=reduced?0:p*9;
        for(var corpse:game.corpses())if(game.beelzebubTarget(corpse)) {
            // the arm holds the body where it is drawn: on the ground, then rising into the hand at the very end
            double[] drag=drag(game,corpse);
            if(drag[3]<=0)continue;
            double sx=(corpse.x()+drag[0]-player.x())/2,sy=(corpse.y()+ground(corpse)+drag[1]-middle(corpse)-drag[2]-player.y())/2;
            double reach=Math.min(1,p/.27);
            sx=cx+(sx-cx)*reach;sy=cy+(sy-cy)*reach;
            double width=(reduced?8:15)*strength;
            ribbon(g,cx,cy,sx,sy,width+3,motion,new Color(72,26,117,90));
            ribbon(g,cx,cy,sx,sy,width,motion,new Color(68,29,106));
            ribbon(g,cx,cy,sx,sy,Math.max(1,width-3),motion,VOID);
            if(!reduced)ribbon(g,cx,cy,sx,sy,width*.4,motion+.6,new Color(30,13,49));
            if(!reduced) {
                for(int mote=0;mote<9;mote++) {
                    double t=(p*2+mote/9.)%1,a=motion+mote*2.4;
                    double spread=Math.sin(t*Math.PI)*17;
                    int x=(int)(sx+(cx-sx)*t+Math.cos(a)*spread);
                    int y=(int)(sy+(cy-sy)*t+Math.sin(a)*spread);
                    g.setColor(mote%3==0?LIGHT:RIM);g.fillRect(x,y,3,2);
                }
                int radius=(int)((7+6*(1-p))*strength);          // the grip: clutching the body, not hiding it
                for(int puff=0;puff<6;puff++) {
                    double a=puff*2.4+motion*.5;
                    int r=(int)(radius*(.4+.08*(puff%3)));
                    int x=(int)(sx+Math.cos(a)*radius*.5),y=(int)(sy+Math.sin(a)*radius*.4);
                    g.setColor(new Color(53,22,81));g.fillOval(x-r,y-r,r*2,r*2);
                    g.setColor(VOID);g.fillOval(x-r+2,y-r+1,Math.max(1,r*2-3),Math.max(1,r*2-3));
                }
            }
        }
        int radius=(int)((reduced?29:38)*strength);
        if(radius>0) {
            if(!reduced) {
                g.setColor(new Color(103,38,172,28));g.fillOval(cx-radius-12,cy-radius-8,(radius+12)*2,(radius+8)*2);
                g.setColor(new Color(134,43,201,38));g.fillOval(cx-radius-6,cy-radius-3,(radius+6)*2,(radius+3)*2);
            }
            flame(g,cx,cy,radius,motion);
            g.setColor(VOID);g.fillOval(cx-radius/3,cy-radius/4,radius*2/3,radius/2);
        }
        g.dispose();
    }

    private static void ribbon(Graphics2D g,double cx,double cy,double sx,double sy,double width,double motion,Color color) {
        double dx=sx-cx,dy=sy-cy,length=Math.max(1,Math.hypot(dx,dy)),nx=-dy/length,ny=dx/length;
        var shape=new Polygon();
        for(int side:new int[]{1,-1})for(int step=0;step<=28;step++) {
            double t=(side==1?step:28-step)/28.;
            double curl=Math.sin(t*Math.PI)*Math.sin(t*7-motion)*Math.min(18,length*.14);
            double edge=width*(.45+.55*Math.sin(t*Math.PI/2))*(1+.16*Math.sin(t*31-motion*2));
            shape.addPoint((int)Math.round(cx+dx*t+nx*(curl+side*edge)),
                    (int)Math.round(cy+dy*t+ny*(curl+side*edge)));
        }
        g.setColor(color);g.fillPolygon(shape);
    }

    private static void flame(Graphics2D g,int cx,int cy,int radius,double motion) {
        if(radius<1)return;
        var body=new Polygon();
        for(int step=0;step<48;step++) {
            double a=step*Math.PI/24,r=radius*(.78+.14*Math.sin(a*5-motion)+.08*Math.cos(a*9+motion));
            body.addPoint(cx+(int)(Math.cos(a)*r),cy+(int)(Math.sin(a)*r*.8));
        }
        g.setColor(VOID);g.fillPolygon(body);
        for(int arm=0;arm<4;arm++)for(int layer=0;layer<3;layer++) {
            var petal=new Polygon();
            for(int side:new int[]{1,-1})for(int step=0;step<=24;step++) {
                double t=(side==1?step:24-step)/24.;
                if(layer==2)t=.25+t*.5;
                double a=arm*1.67+motion*.7-t*3.2;
                double r=radius*(1-.82*t)*(1-.10*(arm%2));
                double thickness=radius*Math.sin(t*Math.PI)*(.23-layer*.085);
                petal.addPoint(cx+(int)(Math.cos(a)*(r+side*thickness)),
                        cy+(int)(Math.sin(a)*(r+side*thickness)*.8));
            }
            g.setColor(layer==0?new Color(45,14,75):layer==1?RIM:new Color(154,72,213));g.fillPolygon(petal);
        }
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
