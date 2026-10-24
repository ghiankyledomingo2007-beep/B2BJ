import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Camp services open on demand; purchases still go through the game's save and price guards.
 * Drawn entirely from the pixel kit: each upgrade is a card with its icon, rank pips and a shard cost
 * that turns red when it cannot be paid; the actions along the bottom are key prompts.
 */
@SuppressWarnings("serial")
final class CampMenu extends JPanel {
    static final String[] TRACKS = {"vitality", "capacity", "efficiency", "edge"};
    private static final String[] NAMES = {"Vitality", "Capacity", "Efficiency", "Edge"};
    private static final String[] DETAILS = {"20 more maximum HP", "10% more Ichor", "Slower Ichor drain", "A stronger combo finisher"};
    private static final Ui.Icon[] ICONS = {Ui.Icon.HEART, Ui.Icon.FLASK, Ui.Icon.HOURGLASS, Ui.Icon.SWORD};
    private static final int ROW_Y = 128, ROW_H = 64, ROW_GAP = 10, SIDE = 28;
    private final RuinedOutpostGame game;
    private final Slot[] upgrades = new Slot[4];
    private final Slot leave, rest, talk;
    private final Runnable close;
    private String status="A moment to recover.";

    CampMenu(RuinedOutpostGame game, Runnable close, Runnable converse) {
        super(null);
        this.game=game;this.close=close;
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Field camp upgrades");
        for(int i=0;i<upgrades.length;i++) {
            final int index=i;
            upgrades[i]=slot(()->buy(index));
        }
        rest=slot(this::rest);
        talk=slot(converse);
        leave=slot(close);
        rest.getAccessibleContext().setAccessibleName("Rest and save, H");
        talk.getAccessibleContext().setAccessibleName("Talk to Mara, E");
        leave.getAccessibleContext().setAccessibleName("Leave camp, U");
    }

    /** A clickable, focusable region; the menu paints it so every state shares one look. */
    private Slot slot(Runnable action) {
        var slot=new Slot();
        slot.addActionListener(event->action.run());
        slot.getModel().addChangeListener(event->repaint());
        slot.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });
        add(slot);
        return slot;
    }

    private static final class Slot extends JButton {
        Slot() { setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); }
        boolean hot() { return isEnabled() && (getModel().isRollover() || isFocusOwner()); }
        @Override protected void paintComponent(Graphics g) { }
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
            int level=game.upgradeLevel(TRACKS[i]), max=max(i);
            int cost=game.upgradeCost(TRACKS[i]);
            upgrades[i].setEnabled(level<max&&game.shards()>=cost&&!game.blocked());
            upgrades[i].getAccessibleContext().setAccessibleName(NAMES[i]+", "+DETAILS[i]+", level "+level+" of "+max+", "
                    +(level==max?"mastered":cost+" shards"));
        }
        talk.setVisible(game.biome()==0);
        if(game.biome()==0)status="Missing found "+game.rescued()+" of "+FieldObjects.SURVIVORS.length
                +"  ·  Supply tins "+game.tinsDelivered()+" of "+FieldObjects.count(0,FieldObjects.Kind.TIN);
        doLayout();
        repaint();
    }

    private static int max(int track) { return track==3?1:3; }

    void focusFirst() {
        for(var b:upgrades)if(b.isEnabled()){b.requestFocusInWindow();return;}
        rest.requestFocusInWindow();
    }

    @Override public void doLayout() {
        for(int i=0;i<upgrades.length;i++)upgrades[i].setBounds(SIDE,ROW_Y+i*(ROW_H+ROW_GAP),getWidth()-2*SIDE,ROW_H);
        int y=getHeight()-66;
        rest.setBounds(SIDE,y,promptWidth("H","REST & SAVE"),36);
        talk.setBounds((getWidth()-promptWidth("E","TALK TO MARA"))/2/Ui.S*Ui.S,y,promptWidth("E","TALK TO MARA"),36);
        leave.setBounds(getWidth()-SIDE-promptWidth("U","LEAVE"),y,promptWidth("U","LEAVE"),36);
    }

    private static int promptWidth(String key, String label) { return Ui.keycapWidth(key)+10+Ui.width(label,2); }

    @Override protected void paintComponent(Graphics graphics) {
        var g=(Graphics2D)graphics.create();
        Ui.crisp(g);
        int w=getWidth(),h=getHeight();
        Ui.window(g,0,0,w,h);
        Ui.drawTitleCentered(g,"FIELD CAMP",w/2,30,3,Ui.GOLD_RAMP);
        Ui.divider(g,w/2,80,180,Ui.GOLD);
        Ui.textCentered(g,status,w/2,106,2,Ui.MUTED);
        // Shards on hand, top right.
        String shards=Integer.toString(game.shards());
        int sx=w-SIDE-8-Ui.width(shards,2);
        Ui.icon(g,Ui.Icon.SHARD,sx-30,30);
        Ui.text(g,shards,sx,50,2,Ui.GOLD_LIGHT);
        for(int i=0;i<upgrades.length;i++)row(g,i);
        if(rest.isVisible())prompt(g,rest,"H","REST & SAVE");
        if(talk.isVisible())prompt(g,talk,"E","TALK TO MARA");
        prompt(g,leave,"U","LEAVE");
        g.dispose();
    }

    private void row(Graphics2D g,int i) {
        var slot=upgrades[i];
        int x=slot.getX(),y=slot.getY(),w=slot.getWidth();
        int level=game.upgradeLevel(TRACKS[i]),max=max(i),cost=game.upgradeCost(TRACKS[i]);
        boolean mastered=level>=max,affordable=game.shards()>=cost;
        Ui.nine(g,slot.hot()?Ui.Piece.BUTTON_HOT:Ui.Piece.PANEL,x,y,w,ROW_H);
        Ui.keycap(g,Integer.toString(i+1),x+14,y+18);
        Ui.nine(g,Ui.Piece.INSET,x+52,y+12,40,40);
        Ui.icon(g,ICONS[i],x+60,y+20);
        Color name=mastered?Ui.MUTED:Ui.TEXT;
        Ui.text(g,NAMES[i].toUpperCase(java.util.Locale.ROOT),x+106,y+28,2,name);
        Ui.text(g,DETAILS[i],x+106,y+50,2,Ui.MUTED);
        // Rank pips beside the name: filled gold for bought ranks, dim sockets for the rest.
        int px=x+106+Ui.width(NAMES[i].toUpperCase(java.util.Locale.ROOT),2)+14;
        for(int n=0;n<max;n++)pip(g,px+n*16,y+14,n<level);
        int right=x+w-18;
        if(mastered) {
            Ui.textRight(g,"MASTERED",right,y+40,2,Ui.TEAL);
        } else {
            String price=Integer.toString(cost);
            int tx=right-Ui.width(price,2);
            Ui.icon(g,Ui.Icon.SHARD,tx-30,y+20);
            Ui.text(g,price,tx,y+40,2,affordable?Ui.GOLD_LIGHT:Ui.DANGER);
        }
    }

    /** A 10 px diamond on the 2x grid. */
    private static void pip(Graphics2D g,int x,int y,boolean filled) {
        int[][] rows={{4,2},{2,6},{0,10},{2,6},{4,2}};
        for(int r=0;r<rows.length;r++) {
            g.setColor(Ui.INK);g.fillRect(x+rows[r][0]-2,y+r*2,rows[r][1]+4,2);
        }
        for(int r=0;r<rows.length;r++) {
            g.setColor(filled?(r<2?Ui.GOLD_LIGHT:Ui.GOLD):new Color(0x2a2a3c));
            g.fillRect(x+rows[r][0],y+r*2,rows[r][1],2);
        }
    }

    private static void prompt(Graphics2D g,Slot slot,String key,String label) {
        int x=slot.getX(),y=slot.getY();
        int kw=Ui.keycap(g,key,x,y+4);
        boolean hot=slot.hot();
        Ui.text(g,label,x+kw+10,y+24,2,hot?Ui.GOLD_LIGHT:Ui.TEXT);
        if(hot){g.setColor(Ui.GOLD);g.fillRect(x+kw+10,y+32,Ui.width(label,2),2);}
    }
}
