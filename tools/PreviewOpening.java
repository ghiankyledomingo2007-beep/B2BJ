import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Replay the real opening without reading or writing the player's campaign save. */
public final class PreviewOpening {
    private static B2BJ scene(boolean awakening) {
        var game=RuinedOutpostGame.campaign();game.begin();
        if(awakening) {game.advanceDialogue();game.advanceDialogue();}
        assert game.opening();
        assert game.dialogueIndex()==(awakening?2:0);
        return new B2BJ(false,game);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            var window=new JFrame("B2BJ — New opening (temporary session)");
            boolean fullscreen=java.util.Arrays.asList(args).contains("--fullscreen");
            boolean awakening=java.util.Arrays.asList(args).contains("--raphael");
            window.setUndecorated(fullscreen);
            B2BJ[] panel={scene(awakening)};
            var replay=new JButton(awakening?"Replay Raphael awakening":"Replay opening");replay.setFocusable(false);
            replay.addActionListener(event->{
                GameAudio.setDialogue(null,false);GameAudio.setOpeningSound(null,false);GameAudio.stopLoops();
                window.remove(panel[0]);panel[0]=scene(awakening);window.add(panel[0],BorderLayout.CENTER);
                window.revalidate();panel[0].requestFocusInWindow();
            });
            window.add(panel[0],BorderLayout.CENTER);window.add(replay,BorderLayout.SOUTH);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);window.pack();window.setLocationRelativeTo(null);
            if(fullscreen)window.getGraphicsConfiguration().getDevice().setFullScreenWindow(window);
            else window.setVisible(true);
            panel[0].requestFocusInWindow();GameAudio.startMusic();
            long[] last={System.nanoTime()};
            new Timer(16,event->{
                long now=System.nanoTime();double dt=Math.min(.05,(now-last[0])/1e9);last[0]=now;
                panel[0].step(dt);
            }).start();
        });
    }
}
