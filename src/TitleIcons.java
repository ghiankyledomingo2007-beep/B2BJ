import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Solid ivory heraldry from the selected menu concept; the world atlas is independent. */
final class TitleIcons {
    private static final Ui.Icon[] SYMBOLS={Ui.Icon.GATE,Ui.Icon.MAP,Ui.Icon.GEAR,Ui.Icon.DOOR};
    private static final int DIALOGUE=Ui.Icon.values().length, MUSIC=DIALOGUE+1;
    private static final BufferedImage[][] IMAGES=makeIcons();

    private static BufferedImage[][] makeIcons() {
        var images=new BufferedImage[MUSIC+1][3];
        for(int index=0;index<images.length;index++)for(int state=0;state<3;state++) {
            var image=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);
            var g=image.createGraphics();g.scale(2,2);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(2.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            Color paper=new Color(state==2?0x64707a:state==1?0xfff6de:0xefdfb9);
            Color cyan=new Color(state==2?0x647e86:state==1?0x8ff5ef:0x68dce4);
            g.setColor(paper);
            if(index==DIALOGUE) {
                g.fillRoundRect(4,7,40,29,24,24);fill(g,12,30,10,44,25,32);
                cut(g);for(int x:new int[]{15,24,33})g.fillOval(x-2,18,4,5);
            } else if(index==MUSIC) {
                fill(g,17,8,39,3,39,32,34,32,34,12,22,15,22,37,17,37);
                g.fillOval(7,31,15,12);g.fillOval(24,27,15,12);
            } else switch(Ui.Icon.values()[index]) {
                case GATE -> {
                    for(int y:new int[]{26,33,40}){g.fillRect(6,y,9,6);g.fillRect(33,y,9,6);}
                    fill(g,6,24,8,16,17,19,15,25);fill(g,9,14,15,7,21,14,17,18);
                    fill(g,17,6,24,1,29,8,23,13);fill(g,30,9,37,14,32,21,26,15);
                    fill(g,38,16,42,24,33,25,33,22);
                    g.setColor(cyan);MenuTheme.star(g,24,26,8);fill(g,21,26,27,26,27,43,21,43);
                }
                case MAP, NEW -> {
                    fill(g,4,12,15,7,15,40,4,44);fill(g,17,7,30,12,30,45,17,40);
                    fill(g,32,12,44,6,44,39,32,45);
                    cut(g);MenuTheme.star(g,35,25,8);g.fillOval(9,20,3,3);g.fillOval(24,33,3,3);
                    ink(g,paper);MenuTheme.star(g,24,4,4);
                }
                case GEAR -> {
                    var gear=new Path2D.Double();
                    for(int i=0;i<32;i++) {
                        double angle=i*Math.PI/16-Math.PI/2,r=(i%4==0||i%4==3)?21:16;
                        double x=24+Math.cos(angle)*r,y=24+Math.sin(angle)*r;
                        if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);
                    }
                    gear.closePath();g.fill(gear);cut(g);g.fillOval(11,11,26,26);
                    ink(g,state==1?cyan:paper);MenuTheme.star(g,24,24,12);
                }
                case DOOR -> {
                    fill(g,5,10,22,2,33,8,33,17,28,17,28,11,22,8,10,14,10,38,22,42,28,40,28,32,33,32,33,44,22,47,5,42);
                    fill(g,14,16,24,11,24,39,14,36);cut(g);g.fillOval(19,24,2,3);
                    ink(g,cyan);fill(g,25,21,38,21,38,15,47,24,38,33,38,27,25,27);
                }
                case PLAY -> {
                    g.setStroke(new BasicStroke(3));g.drawOval(7,7,34,34);
                    for(int i=0;i<4;i++) {var c=(Graphics2D)g.create();c.rotate(i*Math.PI/2,24,24);fill(c,24,0,29,7,24,5,19,7);c.dispose();}
                    g.setColor(cyan);fill(g,18,13,36,24,18,35);
                }
                case KEYS, MEMORY -> {
                    fill(g,3,8,13,6,23,11,23,42,13,37,3,39);fill(g,25,11,35,6,45,8,45,39,35,37,25,42);
                    cut(g);g.setStroke(new BasicStroke(1.7f));g.drawLine(7,12,7,33);g.drawLine(41,12,41,33);
                    MenuTheme.star(g,34,24,7);ink(g,paper);g.drawLine(3,43,13,41);g.drawLine(13,41,24,46);g.drawLine(24,46,35,41);g.drawLine(35,41,45,43);
                }
                case FLASK -> {
                    fill(g,17,3,31,3,31,8,28,8,28,18,40,37,39,44,9,44,8,37,20,18,20,8,17,8);
                    cut(g);fill(g,23,9,25,9,25,20,35,35,13,35,23,20);
                    ink(g,cyan);g.fillOval(19,36,5,4);g.fillOval(29,38,3,3);
                }
                case BACK -> {
                    var arrow=new Path2D.Double();arrow.moveTo(20,3);arrow.lineTo(3,19);arrow.lineTo(20,33);arrow.lineTo(20,24);
                    arrow.curveTo(37,23,40,33,27,44);arrow.curveTo(51,37,48,13,20,13);arrow.closePath();g.fill(arrow);
                    g.setColor(cyan);MenuTheme.star(g,30,38,5);
                }
                case MENU -> {for(int y:new int[]{10,22,34})fill(g,5,y,43,y,39,y+5,9,y+5);}
                case HEART -> {
                    var heart=new Path2D.Double();heart.moveTo(24,43);heart.curveTo(-13,18,10,-4,24,12);
                    heart.curveTo(38,-4,61,18,24,43);g.fill(heart);
                }
                case HOURGLASS -> {
                    g.fillRect(10,3,28,5);g.fillRect(10,40,28,5);
                    fill(g,13,8,35,8,35,15,27,24,35,33,35,40,13,40,13,33,21,24,13,15);
                    cut(g);fill(g,17,9,31,9,31,14,24,21,17,14);
                    ink(g,cyan);fill(g,17,37,24,29,31,37);
                }
                case SWORD -> {
                    fill(g,36,3,44,4,45,12,22,35,13,26);g.fillRect(5,33,10,10);
                    g.setStroke(new BasicStroke(5));g.drawLine(7,23,26,42);
                    g.setColor(cyan);g.setStroke(new BasicStroke(2));g.drawLine(21,27,38,10);
                }
                case SHARD -> {fill(g,24,2,36,16,30,40,18,46,12,25);g.setColor(cyan);fill(g,24,7,29,19,22,37,18,25);}
                case CROWN -> {
                    fill(g,4,12,15,22,24,5,33,22,44,12,39,36,9,36);g.fillRect(9,40,30,5);
                    cut(g);MenuTheme.star(g,24,27,5);
                }
                case SOUND -> {
                    fill(g,3,17,12,17,24,7,24,41,12,31,3,31);
                    g.setColor(cyan);g.setStroke(new BasicStroke(3.5f));g.drawArc(20,13,16,22,-64,128);g.drawArc(19,5,27,38,-63,126);
                }
                case EYE -> {
                    fill(g,2,24,11,13,24,8,37,13,46,24,37,35,24,40,11,35);
                    cut(g);g.fillOval(14,14,20,20);ink(g,cyan);g.fillOval(20,20,8,8);
                }
                default -> MenuTheme.star(g,24,24,18);
            }
            g.dispose();images[index][state]=image;
        }
        return images;
    }

    private static void cut(Graphics2D g) {g.setComposite(AlphaComposite.Clear);}
    private static void ink(Graphics2D g,Color color) {g.setComposite(AlphaComposite.SrcOver);g.setColor(color);}
    private static void fill(Graphics2D g,double... points) {
        var path=new Path2D.Double();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);
        path.closePath();g.fill(path);
    }

    static javax.swing.Icon volumeIcon(String name) {
        int index=switch(name){case "dialogueVolume"->DIALOGUE;case "musicVolume"->MUSIC;default->Ui.Icon.SOUND.ordinal();};
        return new javax.swing.Icon() {
            public int getIconWidth(){return 32;}
            public int getIconHeight(){return 32;}
            public void paintIcon(java.awt.Component c,java.awt.Graphics g,int x,int y){drawImage((Graphics2D)g,index,x,y,32,false,c.isEnabled());}
        };
    }
    static void draw(Graphics2D g,int choice,int x,int y,boolean selected,boolean enabled) {draw(g,choice,x,y,24,selected,enabled);}
    static void draw(Graphics2D g,int choice,int x,int y,int size,boolean selected,boolean enabled) {draw(g,SYMBOLS[choice],x,y,size,selected,enabled);}
    static void draw(Graphics2D g,Ui.Icon icon,int x,int y,boolean selected,boolean enabled) {draw(g,icon,x,y,24,selected,enabled);}
    static void draw(Graphics2D g,Ui.Icon icon,int x,int y,int size,boolean selected,boolean enabled) {drawImage(g,icon.ordinal(),x,y,size,selected,enabled);}
    private static void drawImage(Graphics2D graphics,int index,int x,int y,int size,boolean selected,boolean enabled) {
        var g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(IMAGES[index][!enabled?2:selected?1:0],x,y,size,size,null);g.dispose();
    }
}
