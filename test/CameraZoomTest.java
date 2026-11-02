import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

/** The wider camera preserves player centering and mouse aim across window sizes. */
public final class CameraZoomTest {
    public static void main(String[] args) throws Exception {
        assert B2BJ.worldZoom(1280,720)==1&&B2BJ.worldZoom(1366,768)==1&&B2BJ.worldZoom(800,600)==1;
        assert B2BJ.worldZoom(1920,1080)==1.25&&B2BJ.worldZoom(1906,1071)==1.25 : "fullscreen and maximised views gain 20% more world width";
        assert B2BJ.worldZoom(2560,1440)==1.75&&B2BJ.worldZoom(2560,1080)==1.25;
        for(int w=640;w<=3840;w+=17)for(int h=360;h<=2160;h+=23)
            assert B2BJ.worldZoom(w,h)>=1&&B2BJ.worldZoom(w,h)*4%1==0 : "zoom stays in discrete quarter steps without shrinking below native size";
        SwingUtilities.invokeAndWait(()->{
            try {
                var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
                var panel=new B2BJ(false,game);game.player().relocate(3000,2000);
                var aim=B2BJ.class.getDeclaredMethod("aimDirection");aim.setAccessible(true);
                for(int[] size:new int[][]{{1280,720},{1906,1071},{1920,1080},{2560,1440}}) {
                    int w=size[0],h=size[1];double z=B2BJ.worldZoom(w,h);panel.setSize(w,h);panel.doLayout();
                    double sx=(game.player().x()-panel.worldCameraX())*z,sy=(game.player().y()-panel.worldCameraY())*z;
                    assert Math.abs(sx-w/2.)<=2*z&&Math.abs(sy-h/2.)<=2*z : "the player stays centred on screen at "+w+"x"+h;
                    for(int[] d:new int[][]{{120,0},{0,-120},{-90,90}}) {
                        int mx=(int)Math.round(sx+d[0]*z),my=(int)Math.round(sy+d[1]*z);
                        panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,mx,my,0,false));
                        int[] a=(int[])aim.invoke(panel);
                        double want=Math.atan2(d[1],d[0]),got=Math.atan2(a[1],a[0]);
                        assert Math.abs(Math.atan2(Math.sin(want-got),Math.cos(want-got)))<.05
                                : "the mouse aims through the zoom at "+w+"x"+h;
                    }
                }
            } catch(Exception e) {throw new RuntimeException(e);}
        });
        System.out.println("CameraZoomTest passed: wider view, centred player, mouse aim through the zoom");
    }
}
