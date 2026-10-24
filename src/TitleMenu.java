import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The title screen's menu: Continue, New Journey, Settings, Quit, as real buttons (mouse, keyboard,
 * screen readers). Starting over on top of a save asks once more before it replaces anything.
 */
@SuppressWarnings("serial")
final class TitleMenu extends JPanel {
    static final String NEW = "New Journey", CONFIRM = "Replace save?";
    private final PixelSwing.Button resume, fresh, settings, quit;
    private final RuinedOutpostGame game;
    private final Runnable start;
    private final Timer cancel;
    private PixelSwing.Button selected;

    TitleMenu(RuinedOutpostGame game, Runnable resumeAction, Runnable start, Runnable settingsAction, Runnable quitAction) {
        super(null);
        this.game = game;
        this.start = start;
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Title menu");
        resume = PixelSwing.textButton("Continue", 'C', 3, resumeAction);
        fresh = PixelSwing.textButton(NEW, 'N', 3, this::freshPressed);
        settings = PixelSwing.textButton("Settings", 'S', 2, settingsAction);
        quit = PixelSwing.textButton("Quit", 'Q', 2, ()->{GameAudio.play(GameAudio.Cue.UI_CONFIRM);quitAction.run();});
        int choice=0;
        for(var b:new PixelSwing.Button[]{resume,fresh,settings,quit}) {
            b.titleChoice=choice++;
            add(b);
            b.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent event) { select(b,true); }
            });
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) {
                    if(b.isEnabled()) {select(b,true);b.requestFocusInWindow();}
                }
            });
        }
        cancel = new Timer(4000, event -> fresh.setText(NEW));
        cancel.setRepeats(false);
        refresh();
    }

    /** Continue only exists when there is a journey to continue. */
    void refresh() {
        resume.setEnabled(game.saveAvailable());
        String caption=resume.isEnabled()?game.savedAreaName():game.saveNeedsConfirmation()?"Unreadable save":"No saved journey";
        if(!caption.equals(resume.subtitle)) {
            resume.subtitle=caption;
            resume.getAccessibleContext().setAccessibleDescription(caption);
            resume.repaint();
        }
        if (!game.saveNeedsConfirmation() && fresh.getText().equals(CONFIRM)) fresh.setText(NEW);
        if(selected==null||!selected.isEnabled())select(resume.isEnabled()?resume:fresh,false);
    }

    @Override public void doLayout() {
        int y=0;
        var buttons=new PixelSwing.Button[]{resume,fresh,settings,quit};
        int[] heights={72,66,48,48};
        for(int i=0;i<buttons.length;i++) {buttons[i].setBounds(0,y,getWidth(),heights[i]);y+=heights[i]+4;}
    }

    void focusDefault() {
        refresh();select(resume.isEnabled()?resume:fresh,false);selected.requestFocusInWindow();
    }

    int selectedChoice() {return selected==null?0:selected.titleChoice;}

    void animate(double time,boolean reduced) {
        for(var button:new PixelSwing.Button[]{resume,fresh,settings,quit}) {
            double blend=reduced?1:1-Math.exp(-Math.min(.1,Math.max(0,time-button.titleTime))*12);
            button.titleFocus+=((button.titleSelected?1:0)-button.titleFocus)*blend;
            button.titleTime=time;button.reducedTitleMotion=reduced;
        }
    }

    private void select(PixelSwing.Button button,boolean sound) {
        if(!button.isEnabled()||selected==button)return;
        if(selected!=null) {selected.titleSelected=false;selected.repaint();}
        selected=button;selected.titleSelected=true;selected.repaint();
        if(sound)GameAudio.play(GameAudio.Cue.UI_MOVE);
    }

    /** Shortcuts use the save guard even before Swing grants focus. */
    boolean press(int key) {
        PixelSwing.Button button=switch(key) {
            case KeyEvent.VK_C -> resume;
            case KeyEvent.VK_N -> fresh;
            case KeyEvent.VK_ENTER -> selected;
            default -> null;
        };
        if(button==null)return false;
        if(button.isEnabled()) {select(button,false);button.requestFocusInWindow();button.doClick(0);}
        return true;
    }

    private void freshPressed() {
        if (!game.saveNeedsConfirmation() || fresh.getText().equals(CONFIRM)) {
            cancel.stop();
            fresh.setText(NEW);
            start.run();
            return;
        }
        GameAudio.play(GameAudio.Cue.UI_CONFIRM);
        fresh.setText(CONFIRM);
        cancel.restart();
    }
}
