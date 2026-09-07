/** Read-only checks against Claude's compiled map classes. */
public final class ClaudeMapProbe {
    public static void main(String[] args) {
        var muster=new RuinedOutpostMap(2);
        var rack=muster.barriers().get(0);
        var barracks=new RuinedOutpostMap(3);
        var other=barracks.dressing().stream().filter(d->d.decoration()==RuinedOutpostMap.Decoration.WEAPON_RACK)
                .findFirst().orElseThrow();
        var feet=other.obstacle();
        assert rack.prop().path().equals(other.decoration().path());
        System.out.printf("Same weapon-rack sprite at 2x: muster %.0fx%.0f, barracks %.0fx%.0f collision%n",
                rack.width(),rack.height(),feet.width(),feet.height());
        for(int room:new int[]{7,10}) {
            var roof=new RuinedOutpostMap(room).dressing().stream()
                    .filter(d->d.decoration()==RuinedOutpostMap.Decoration.LEAN_TO).findFirst().orElseThrow();
            System.out.printf("Room %d shelter collider: %s%n",room,roof.obstacle());
        }
        var breach=new RuinedOutpostMap(1);int changed=0;
        for(int y=0;y<18;y++)for(int x=0;x<30;x++) {
            int old=(oldStone(x,y)?1:0)|(oldStone(x+1,y)?2:0)
                    |(oldStone(x,y+1)?4:0)|(oldStone(x+1,y+1)?8:0);
            if(old!=breach.tileMask(x,y))changed++;
        }
        System.out.println("Broken Palisade terrain masks changed versus reference: "+changed+" / 540");
    }
    private static boolean oldStone(int x,int y) {
        double centre=576+Math.sin(x*64.0/1920*Math.PI*2)*80;
        return Math.abs(y*64-centre)<112;
    }
}
