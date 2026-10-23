/** Import the approved illustration at the game's native art size, preserving manual retouches. */
final class TitleArt {
    private TitleArt() { }
    static void build() {
        var source=Canvas.load("assets/source/ui/title/ruined-gate-master.png");
        var art=new Canvas(640,360);
        for(int y=0;y<360;y++)for(int x=0;x<640;x++)
            art.set(x,y,source.get((2*x+1)*source.w/1280,(2*y+1)*source.h/720));
        art.save("assets/ui/title/ruined-gate.png");
    }
}
