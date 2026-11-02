import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/** The compact HUD action must keep its icon and complete label inside the ornate border. */
public final class MenuButtonLayoutTest {
    public static void main(String[] args) throws Exception {
        var dir=Files.createTempDirectory("b2bj-menu-spacing-");
        try {
            SwingUtilities.invokeAndWait(()->{
                var panel=new B2BJ(false,RuinedOutpostGame.campaign(dir.resolve("save.properties")));
                var button=java.util.Arrays.stream(panel.getComponents())
                        .filter(c->c instanceof JButton b&&b.getText().equals("Menu"))
                        .map(c->(PixelSwing.Button)c).findFirst().orElseThrow();
                for(int[] viewport:new int[][]{{1280,720},{1920,1080},{2560,1440},{2560,1080}}) {
                    panel.setSize(viewport[0],viewport[1]);panel.doLayout();
                    for(boolean hot:new boolean[]{false,true}) {
                        button.getModel().setRollover(hot);
                        var image=new BufferedImage(button.getWidth(),button.getHeight(),BufferedImage.TYPE_INT_ARGB);
                        var g=image.createGraphics();button.paint(g);g.dispose();
                        int left=image.getWidth(),right=-1,count=0;
                        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                            int pixel=image.getRGB(x,y),r=pixel>>16&255,green=pixel>>8&255,b=pixel&255;
                            if((pixel>>>24)>220&&r>225&&green>210&&b>175&&r>=green) {
                                left=Math.min(left,x);right=Math.max(right,x);count++;
                            }
                        }
                        assert count>40 : "Menu label and icon must be visible";
                        double padding=12*button.titleScale;
                        assert left>=padding&&right<image.getWidth()-padding
                                : "Menu label/icon touch the frame at "+viewport[0]+": "+left+".."+right;
                    }
                }
            });
        } finally {Files.deleteIfExists(dir.resolve("save.properties"));Files.delete(dir);}
        System.out.println("MenuButtonLayoutTest passed: icon/label padding at four viewports, idle and hover");
    }
}
