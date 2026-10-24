import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Playable test copy: starts at the Outpost camp from a throwaway save, god mode on. The real save is never read or written. */
public final class PlayTest {
    public static void main(String[] args) throws Exception {
        var save = Files.createTempFile("b2bj-playtest-", ".properties");
        save.toFile().deleteOnExit();
        CampaignSave.save(save, new CampaignSave.Progress(0, 1, Set.<Integer>of(), Set.<String>of(), Map.<String,Integer>of(), 25));
        SwingUtilities.invokeLater(() -> {
            var game = RuinedOutpostGame.campaign(save);
            if (!game.continueCampaign()) throw new IllegalStateException("test save did not load");
            game.setDebugGodMode(true);
            game.player().collectIchor(100);
            var panel = new B2BJ(true, game);
            var window = new JFrame("B2BJ — Play test (temporary, no save)");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.add(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            GameAudio.startMusic();
            panel.requestFocusInWindow();
        });
    }
}
