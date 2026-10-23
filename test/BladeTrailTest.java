import java.awt.image.BufferedImage;

/** The blade trail is a separate layer: its sheets must line up cell for cell with the blade sheets. */
public final class BladeTrailTest {
    private static final int CELL=B2BJ.TRAIL_CELL;
    private static final int[] TONES={0xffecfaff,0xff78c8ff,0xff286ee6};

    public static void main(String[] args) {
        assert CELL==BladeAnimation.CELL_WIDTH*2 : "a trail cell is twice the body cell, centred on it";
        strike("slash",12,20);
        strike("cleave",12,8);
        for(String name:new String[]{"cast","counter"}) {
            var image=sheet(name,8,4);
            for(int row=0;row<4;row++) {
                assert pixels(image,7,row)==0 : name+": the flick has faded by the last frame";
                assert anyTrail(image,0,3,row) : name+" happens at once, so it trails from the first frames";
            }
        }
        System.out.println("BladeTrailTest passed");
    }

    /** A swing's trail belongs to the strike: nothing during the wind-up, nothing once he has recovered. */
    private static void strike(String name,int columns,int rows) {
        var image=sheet(name,columns,rows);
        for(int row=0;row<rows;row++) {
            for(int frame=0;frame<=3;frame++)assert pixels(image,frame,row)==0 : name+" row "+row+": a wind-up is not a cut";
            for(int frame=11;frame<columns;frame++)assert pixels(image,frame,row)==0 : name+" row "+row+": the trail is gone before he is home";
            assert anyTrail(image,4,6,row) : name+" row "+row+": the strike leaves a trail";
            if(name.equals("slash")||row<4)for(int frame=7;frame<columns;frame++)
                assert pixels(image,frame,row)==0 : name+": ordinary cuts must fade before the recovery swing";
        }
    }

    private static BufferedImage sheet(String name,int columns,int rows) {
        var image=B2BJ.loadImage("assets/effects/rainoray_"+name+"_trail.png");
        assert image!=null : "missing trail sheet for "+name;
        assert image.getWidth()==columns*CELL&&image.getHeight()==rows*CELL : name+" trail must match the blade sheet's grid";
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int pixel=image.getRGB(x,y);
            assert (pixel>>>24)==0||pixel==TONES[0]||pixel==TONES[1]||pixel==TONES[2] : name+" trail uses only its three flat tones";
        }
        return image;
    }

    private static boolean anyTrail(BufferedImage image,int first,int last,int row) {
        for(int frame=first;frame<=last;frame++)if(pixels(image,frame,row)>0)return true;
        return false;
    }

    private static int pixels(BufferedImage image,int frame,int row) {
        int count=0;
        for(int pixel:image.getRGB(frame*CELL,row*CELL,CELL,CELL,null,0,CELL))if((pixel>>>24)>0)count++;
        return count;
    }
}
