import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Separate game window, authentic player kit, and an in-memory campaign only. */
public final class PreviewGuardianCombat {
    static RuinedOutpostGame scene(boolean human,boolean secondPhase){
        var game=RuinedOutpostGame.campaign();game.begin();game.skipDialogue();
        if(!game.debugWarp(0,true))throw new IllegalStateException("Could not enter isolated boss practice");
        game.player().collectIchor(100);
        if(human&&!game.player().transform())throw new IllegalStateException("Human practice form unavailable");
        game.player().setUnlimitedBlade(true);
        if(!(game.guardian() instanceof GuardianDraft boss))throw new IllegalStateException("Preview boss was not installed in this separate build");
        if(secondPhase)boss.secondPhase();
        return game;
    }
    static B2BJ panel(boolean human,boolean secondPhase){var p=new B2BJ(false,scene(human,secondPhase));p.setPreferredSize(new Dimension(1280,720));return p;}
    public static void main(String[] args)throws Exception{
        if(args.length==2&&args[0].equals("--capture")){capture(Path.of(args[1]));return;}
        SwingUtilities.invokeLater(()->{
            var window=new JFrame("Blob to Blade — Guardian preview · original player combat · temporary session");
            B2BJ[] current={panel(true,false)};
            current[0].game().pause();
            var bar=new JPanel(new FlowLayout(FlowLayout.LEFT,9,5));bar.setBackground(new Color(12,18,26));
            var label=new JLabel("Esc resume · WASD move · LMB tap / hold · RMB skill · Space dash · F parry · R Beelzebub · Q transform");label.setForeground(new Color(238,232,210));
            java.util.function.BiConsumer<Boolean,Boolean> reset=(human,phase)->{
                GameAudio.stopLoops();GameAudio.setDialogue(null,false);GameAudio.setOpeningSound(null,false);
                window.remove(current[0]);current[0]=panel(human,phase);window.add(current[0],BorderLayout.CENTER);window.revalidate();current[0].requestFocusInWindow();
            };
            for(String text:new String[]{"Human · I","Human · II","Slime · I","Slime · II"}){
                var button=new JButton(text);button.setFocusable(false);button.addActionListener(e->reset.accept(text.startsWith("Human"),text.endsWith("II")));bar.add(button);
            }
            var refill=new JButton("Refill");refill.setFocusable(false);refill.addActionListener(e->{current[0].game().debugRefill();current[0].requestFocusInWindow();});bar.add(refill);
            var pause=new JButton("Pause / resume");pause.setFocusable(false);pause.addActionListener(e->{current[0].game().togglePause();current[0].requestFocusInWindow();});bar.add(pause);
            var south=new JPanel(new BorderLayout());south.setBackground(bar.getBackground());south.add(label,BorderLayout.NORTH);south.add(bar,BorderLayout.SOUTH);
            window.add(current[0],BorderLayout.CENTER);window.add(south,BorderLayout.SOUTH);window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);window.pack();window.setLocationRelativeTo(null);window.setVisible(true);
            current[0].requestFocusInWindow();GameAudio.startMusic();GameAudio.setMusicVolume(.18);
            long[] last={System.nanoTime()};new Timer(16,e->{long now=System.nanoTime();double dt=Math.min(.05,(now-last[0])/1e9);last[0]=now;current[0].step(dt);}).start();
            window.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){if(!current[0].game().paused())current[0].game().pause();}});
        });
    }
    private static void capture(Path out)throws Exception{
        Files.createDirectories(out);SwingUtilities.invokeAndWait(()->{
            try{
                GameAudio.setMuted(true);
                for(boolean human:new boolean[]{false,true})for(GuardianDraft.Move move:GuardianDraft.Move.values()){
                    var p=panel(human,move==GuardianDraft.Move.DETACHED_FISTS||move==GuardianDraft.Move.CORE_BEAM);p.setSize(1280,720);p.doLayout();
                    var game=p.game();var boss=(GuardianDraft)game.guardian();game.player().setGodMode(true);
                    game.player().relocate(boss.x()-225,boss.y()+145);
                    boss.forceMove(move,game.player().x(),game.player().y());
                    for(int i=0;i<(int)((boss.telegraphDuration()+.12)*60);i++)p.step(1./60);
                    game.primaryPress(1,0);game.primaryRelease(1,0);
                    for(int i=0;i<10;i++)p.step(1./60);
                    var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();p.paint(g);g.dispose();
                    ImageIO.write(image,"png",out.resolve((human?"human-":"slime-")+move.name().toLowerCase()+".png").toFile());
                }
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
