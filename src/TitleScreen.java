import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Quiet outpost key art, a live wordmark and a handful of rain streaks at native pixel scale. */
final class TitleScreen {
    private static final BufferedImage ART=B2BJ.loadImage("assets/ui/title/ruined-gate.png");
    static boolean available() {return ART!=null;}

    void draw(Graphics2D g,int width,int height,double seconds,BufferedImage slimeIdle,boolean reduced) {
        Ui.crisp(g);
        if(ART!=null)g.drawImage(ART,0,0,width,height,null);
        Ui.fade(g,0,0,620,height,120,0);
        if(!reduced) {
            g.setColor(new Color(151,190,204,55));
            for(int i=0;i<22;i++) {
                int x=520+Math.floorMod(i*137,740),y=(int)(seconds*80+i*61)%height;
                g.fillRect(x/2*2,y/2*2,2,6+(i%3)*2);
            }
        }
        Ui.prose(g,"BLOB",76,210,Ui.heading(68),Ui.TEXT);
        Ui.prose(g,"TO",80,247,Ui.body(17),Ui.GOLD);
        g.setColor(new Color(153,127,83,150));g.fillRect(118,240,184,1);
        Ui.prose(g,"BLADE",74,318,Ui.heading(76),Ui.TEXT);
        g.setColor(Ui.TEAL);g.fillRect(80,342,34,2);
    }
}
