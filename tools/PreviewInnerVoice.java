import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Real game renderer and dialogue, staged in memory without reading or writing a player save. */
public final class PreviewInnerVoice {
    private static B2BJ scene() {
        return scene(null);
    }
    private static B2BJ scene(OutpostBark exchange) {
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        for(var enemy:game.scouts())enemy.hurt(10000);
        var first=FieldObjects.find(0,"survivor_0");game.player().relocate(first.x(),first.y()-600);
        game.update(.01,0,0);
        if(exchange!=null)try {
            var field=RuinedOutpostGame.class.getDeclaredField("bark");field.setAccessible(true);field.set(game,exchange);
        } catch(ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        var panel=new B2BJ(false,game);panel.setSize(1280,720);return panel;
    }

    public static void main(String[] args) throws Exception {
        boolean banter=args.length>0&&args[0].equals("--banter");
        if(args.length>0&&!banter) {
            Path output=Path.of(args[0]);Files.createDirectories(output);
            var panel=scene();
            for(int frame=0;frame<210;frame++) {
                panel.step(.05);GameAudio.mixVoices(new int[1102]);
                if(frame==35||frame==85||frame==110||frame==185) {
                    var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
                    panel.paint(g);g.dispose();
                    String speaker=frame==35||frame==185?"rainoray":"inner-voice";
                    ImageIO.write(image,"png",output.resolve(speaker+"-"+frame+".png").toFile());
                }
            }
            return;
        }
        SwingUtilities.invokeLater(()->{
            var window=new JFrame(banter?"B2BJ — Rainoray + Sage conversations (temporary session)":"B2BJ — Sage preview (temporary session)");
            var choices=java.util.Arrays.stream(OutpostBark.values()).filter(b->b.trigger()!=OutpostBark.Trigger.STORY).toList();
            int[] selected={0};
            B2BJ[] panel={scene(banter?choices.get(0):null)};
            var replay=new JButton("Replay conversation");replay.setFocusable(false);
            Runnable play=()->{
                GameAudio.setDialogue(null,false);window.remove(panel[0]);panel[0]=scene(banter?choices.get(selected[0]):null);
                window.add(panel[0],BorderLayout.CENTER);window.revalidate();panel[0].requestFocusInWindow();
            };
            replay.addActionListener(event->play.run());
            var buttons=new javax.swing.JPanel();buttons.add(replay);
            if(banter) {
                var next=new JButton("Next conversation");next.setFocusable(false);
                next.addActionListener(event->{selected[0]=(selected[0]+1)%choices.size();play.run();});buttons.add(next);
            }
            window.add(panel[0],BorderLayout.CENTER);window.add(buttons,BorderLayout.SOUTH);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);window.setResizable(false);window.pack();
            window.setLocationRelativeTo(null);window.setVisible(true);panel[0].requestFocusInWindow();
            GameAudio.startMusic();
            long[] last={System.nanoTime()};
            new Timer(16,event->{
                long now=System.nanoTime();double dt=Math.min(.05,(now-last[0])/1e9);last[0]=now;
                GameAudio.setMusicState(0,panel[0].game().player().bladeForm(),!panel[0].game().blocked());
                panel[0].step(dt);
            }).start();
        });
    }
}
