import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Real keyboard/mouse smoke test. Uses only an isolated save and its own native window. */
public final class LiveCampaignSmoke {
    private static JFrame frame;
    private static B2BJ panel;
    private static RuinedOutpostGame game;
    private static Point origin;

    public static void main(String[] args) throws Exception {
        Path output=Files.createTempDirectory("b2bj-live-smoke-");
        Robot robot=new Robot();robot.setAutoDelay(35);
        int status=0;
        try {
            SwingUtilities.invokeAndWait(()->{
                game=RuinedOutpostGame.campaign(output.resolve("isolated-save.properties"));
                panel=new B2BJ(true,game);frame=new JFrame("B2BJ campaign smoke test");
                frame.add(panel);frame.pack();frame.setLocationRelativeTo(null);frame.setVisible(true);
                frame.toFront();panel.requestFocusInWindow();
                origin=panel.getLocationOnScreen();
            });
            robot.delay(350);robot.waitForIdle();
            // Some Linux window managers intentionally ignore Java's toFront().
            if(System.getProperty("os.name").equals("Linux")) {
                Process focus=new ProcessBuilder("rtk","proxy","xdotool","search","--name",
                        "^B2BJ campaign smoke test$","windowactivate","--sync").inheritIO().start();
                if(!focus.waitFor(3,java.util.concurrent.TimeUnit.SECONDS)) {
                    focus.destroy();throw new AssertionError("Window manager did not activate the test window");
                }
                if(focus.exitValue()!=0)throw new AssertionError("Could not activate test window");
            }
            SwingUtilities.invokeAndWait(()->{
                panel.requestFocusInWindow();origin=panel.getLocationOnScreen();
                System.out.println("Native window: "+frame.getBounds()+" / panel "+origin);
            });
            robot.delay(200);robot.waitForIdle();
            SwingUtilities.invokeAndWait(()->{assert frame.isFocused():"Window manager kept game unfocused; no physical input will be sent";});
            robot.mouseMove(origin.x+640,origin.y+360);
            capture(robot,output.resolve("title.png"));
            tap(robot,KeyEvent.VK_ENTER);robot.delay(150);
            SwingUtilities.invokeAndWait(()->{assert !game.blocked():"Enter starts campaign: "+game.story().phase()+" paused="+game.paused();});
            double[] start=new double[1];
            SwingUtilities.invokeAndWait(()->start[0]=game.player().x());
            robot.keyPress(KeyEvent.VK_D);robot.delay(180);robot.keyRelease(KeyEvent.VK_D);
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(()->{assert game.player().x()>start[0]+10:"real D key moves player";});
            robot.mouseMove(origin.x+950,origin.y+360);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);robot.delay(100);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);robot.delay(35);
            SwingUtilities.invokeAndWait(()->{assert !game.projectiles().isEmpty():"real M1 releases water blade";});
            capture(robot,output.resolve("attack.png"));
            GameAudio.startMusic();GameAudio.startMusic();
            robot.delay(150);
            long workers=Thread.getAllStackTraces().keySet().stream().filter(t->t.getName().equals("b2bj-music")).count();
            assert workers==1:"music must have one worker";
            tap(robot,KeyEvent.VK_ESCAPE);robot.delay(100);
            SwingUtilities.invokeAndWait(()->{assert game.paused():"Escape pauses";});
            tap(robot,KeyEvent.VK_O);robot.delay(150);
            SwingUtilities.invokeAndWait(()->{
                assert java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog&&w.isShowing()):"O opens native options";
            });
            capture(robot,output.resolve("options.png"));
            tap(robot,KeyEvent.VK_ESCAPE);robot.delay(120);
            SwingUtilities.invokeAndWait(()->{
                assert java.util.Arrays.stream(Window.getWindows()).noneMatch(w->w instanceof JDialog&&w.isShowing());
                assert game.paused():"closing options keeps game safely paused";
                frame.toFront();panel.requestFocusInWindow();
            });
            tap(robot,KeyEvent.VK_T);robot.delay(100);
            SwingUtilities.invokeAndWait(()->{assert game.story().phase()==OutpostStory.Phase.PROLOGUE;});
            tap(robot,KeyEvent.VK_C);robot.delay(100);
            SwingUtilities.invokeAndWait(()->{assert !game.blocked():"C restores checkpoint";});
            assert Files.isRegularFile(output.resolve("isolated-save.properties"));
            System.out.println("Native campaign input/options/continue smoke passed. Captures: "+output);
        } catch(Throwable failure) {
            status=1;failure.printStackTrace();
        } finally {
            GameAudio.setMusicState(0,false,false);
            SwingUtilities.invokeAndWait(()->{if(frame!=null)frame.dispose();});
        }
        System.exit(status); // Own test timer only; no other process/window is touched.
    }

    private static void tap(Robot robot,int key) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            assert java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w.isFocused()
                    &&(w==frame||w.getOwner()==frame)):"Game lost focus; refusing physical input";
        });
        robot.keyPress(key);robot.keyRelease(key);robot.waitForIdle();
    }
    private static void capture(Robot robot,Path path) throws Exception {
        ImageIO.write(robot.createScreenCapture(new Rectangle(origin.x,origin.y,1280,720)),"png",path.toFile());
    }
}
