import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class TitleMenuIconsTest {
    public static void main(String[] args) {
        int[][] previous = new int[4][];
        for (int choice = 0; choice < 4; choice++) {
            int[] normal = render(choice, false, true);
            int[] selected = render(choice, true, true);
            int[] disabled = render(choice, false, false);
            assert !Arrays.equals(normal, selected) : "selection must brighten the icon";
            assert !Arrays.equals(normal, disabled) : "unavailable Continue must be dimmed";
            assert Arrays.equals(disabled, render(choice, true, false)) : "disabled takes priority over selection";
            int painted = 0;
            var shades = new java.util.HashSet<Integer>();
            for (int i = 0; i < normal.length; i++) {
                int x = i % 32, y = i / 32;
                assert (normal[i] >>> 24) == (selected[i] >>> 24) : "selection must preserve the symbol";
                if ((normal[i] >>> 24) != 0) {
                    painted++;
                    shades.add(normal[i]);
                    assert x >= 4 && x < 28 && y >= 4 && y < 28 : "icon must fit its reserved slot";
                }
            }
            assert painted > 100 : "ivory silhouettes must stay substantial at the smallest menu size";
            assert shades.size() > 2 : "heraldic icons need visible accents and smooth edges";
            for (int earlier = 0; earlier < choice; earlier++)
                assert !Arrays.equals(previous[earlier], normal) : "each action needs a distinct icon";
            previous[choice] = normal;
        }
        int[] sound=volume("masterVolume"),voice=volume("dialogueVolume"),music=volume("musicVolume");
        assert !Arrays.equals(sound,voice)&&!Arrays.equals(voice,music)&&!Arrays.equals(sound,music)
                : "audio channels need recognizable, distinct symbols";
        System.out.println("TitleMenuIconsTest passed");
    }

    private static int[] volume(String name) {
        var icon=TitleIcons.volumeIcon(name);
        var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();icon.paintIcon(new javax.swing.JLabel(),g,0,0);g.dispose();
        int[] pixels=image.getRGB(0,0,32,32,null,0,32);
        assert Arrays.stream(pixels).filter(pixel->(pixel>>>24)>0).count()>100 : name+" must remain visible";
        return pixels;
    }

    private static int[] render(int choice, boolean selected, boolean enabled) {
        var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        Ui.crisp(g);
        TitleIcons.draw(g, choice, 4, 4, selected, enabled);
        g.dispose();
        return image.getRGB(0, 0, 32, 32, null, 0, 32);
    }
}
