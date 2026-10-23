import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;
import javax.swing.JPanel;

/** Camp services open on demand; purchases still go through the game's save and price guards. */
@SuppressWarnings("serial")
final class CampMenu extends JPanel {
    static final String[] TRACKS = {"vitality", "capacity", "efficiency", "edge"};
    private static final String[] NAMES = {"Vitality", "Capacity", "Efficiency", "Edge"};
    private static final String[] DETAILS = {"20 more maximum HP", "10% more ichor", "Slower ichor drain", "A stronger combo finisher"};
    private final RuinedOutpostGame game;
    private final JButton[] upgrades = new JButton[4];
    private final JButton leave, rest, talk;
    private final Runnable close;
    private String status="A moment to recover.";

    CampMenu(RuinedOutpostGame game, Runnable close, Runnable converse) {
        super(null);
        this.game=game;this.close=close;
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Field camp upgrades");
        for(int i=0;i<upgrades.length;i++) {
            final int index=i;
            upgrades[i]=PixelSwing.textButton("",(char)('1'+i),()->buy(index));
            add(upgrades[i]);
        }
        rest=PixelSwing.textButton("Rest & save",'H',this::rest);
        talk=PixelSwing.textButton("Talk to Mara",'E',converse);
        leave=PixelSwing.textButton("Leave",'U',close);
        add(rest);add(talk);add(leave);
    }

    void rest() {
        if(game.saveCheckpoint())close.run();
        else {status=game.campaignNotice();repaint();}
    }

    void buy(int index) {
        if(index>=0&&index<TRACKS.length&&upgrades[index].isEnabled())game.buyUpgrade(TRACKS[index]);
        refresh();
    }

    void refresh() {
        for(int i=0;i<upgrades.length;i++) {
            int level=game.upgradeLevel(TRACKS[i]), max=i==3?1:3;
            int cost=game.upgradeCost(TRACKS[i]);
            upgrades[i].setText(level==max?"Mastered":cost+" shards");
            upgrades[i].setEnabled(level<max&&game.shards()>=cost&&!game.blocked());
            upgrades[i].getAccessibleContext().setAccessibleName(NAMES[i]+", "+DETAILS[i]+", level "+level+" of "+max+", "+upgrades[i].getText());
        }
        talk.setVisible(game.biome()==0);
        repaint();
    }

    void focusFirst() {
        for(var b:upgrades)if(b.isEnabled()){b.requestFocusInWindow();return;}
        rest.requestFocusInWindow();
    }

    @Override public void doLayout() {
        for(int i=0;i<upgrades.length;i++)upgrades[i].setBounds(getWidth()-168,116+i*78,136,44);
        rest.setBounds(24,getHeight()-70,182,44);
        talk.setBounds(206,getHeight()-70,188,44);
        leave.setBounds(getWidth()-140,getHeight()-70,112,44);
    }

    @Override protected void paintComponent(Graphics graphics) {
        var g=(Graphics2D)graphics.create();
        Ui.window(g,0,0,getWidth(),getHeight());
        Ui.prose(g,"Field Camp",32,53,Ui.heading(30),Ui.TEXT);
        Ui.prose(g,status,32,82,Ui.body(16),Ui.MUTED);
        Ui.icon(g,Ui.Icon.SHARD,getWidth()-148,35);
        Ui.prose(g,Integer.toString(game.shards()),getWidth()-116,55,Ui.body(21),Ui.GOLD_LIGHT);
        for(int i=0;i<upgrades.length;i++) {
            int y=116+i*78, level=game.upgradeLevel(TRACKS[i]);
            g.setColor(new java.awt.Color(0x2e3441));g.fillRect(32,y-13,getWidth()-64,1);
            Ui.prose(g,Integer.toString(i+1),34,y+22,Ui.body(14),Ui.DIM);
            Ui.prose(g,NAMES[i],64,y+20,Ui.heading(22),Ui.TEXT);
            Ui.prose(g,DETAILS[i],64,y+45,Ui.body(15),Ui.MUTED);
            for(int n=0;n<(i==3?1:3);n++) {
                g.setColor(n<level?Ui.GOLD:Ui.DIM);g.fillRect(300+n*12,y+9,6,6);
            }
        }
        g.dispose();
    }
}
