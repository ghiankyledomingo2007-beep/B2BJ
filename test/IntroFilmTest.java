import java.awt.image.BufferedImage;

/** The opening film is packaged, seekable a second at a time, decoded in the right colours, and as long as its sound. */
public final class IntroFilmTest {
    public static void main(String[] args) throws Exception {
        var film = IntroFilm.load();
        assert film.frames == 1362 : "the approved story is 56.75 s at 24 fps, not " + film.frames + " frames";
        assert Math.abs(film.soundSeconds() - film.frames / (double) IntroFilm.FPS) < .05
                : "sound runs " + film.soundSeconds() + " s against " + film.frames / 24.0 + " s of picture";
        assert film.subtitleCount() == 12 : "seven spoken lines and Sage's five notices";
        int[] keys = film.keyframes();
        assert keys.length == (film.frames + IntroFilm.FPS - 1) / IntroFilm.FPS : keys.length + " keyframes";
        for (int i = 0; i < keys.length; i++)
            assert keys[i] == i * IntroFilm.FPS : "group " + i + " starts at frame " + keys[i] + ": a seek would land elsewhere";
        // Coarse brightness of two frames decoded after a seek, against ffmpeg's straight decode of the same frames.
        matches(film.frame(300), new int[]{18, 9, 8, 46, 10, 15, 25, 56, 41, 26, 68, 57, 38, 45, 58, 45}, 300);
        matches(film.frame(1000), new int[]{12, 24, 16, 14, 12, 43, 61, 37, 12, 15, 18, 14, 12, 12, 12, 12}, 1000);
        // While the film plays, the drawn lines 0-9 wait; when it ends the opening carries on at the waking (line 10).
        var game = RuinedOutpostGame.campaign();
        game.begin();
        assert game.opening() && game.dialogueIndex() == 0;
        game.holdOpening(true);
        for (int i = 0; i < 160; i++) game.update(.5, 0, 0);
        assert game.dialogueIndex() == 0 && !game.advanceDialogue() : "the hidden lines must not run under the film";
        game.finishOpeningFilm();
        assert game.opening() && game.dialogueIndex() == 10 && game.openingLineTime() == 0 : "the film hands over to line 10";
        System.out.println("IntroFilmTest passed: " + film.frames + " frames, " + keys.length + " keyframes");
    }

    private static void matches(BufferedImage frame, int[] expected, int index) {
        assert frame.getWidth() == IntroFilm.WIDTH && frame.getHeight() == IntroFilm.HEIGHT;
        int cellW = IntroFilm.WIDTH / 4, cellH = IntroFilm.HEIGHT / 4;
        for (int cell = 0; cell < 16; cell++) {
            long r = 0, g = 0, b = 0;
            for (int y = cell / 4 * cellH; y < (cell / 4 + 1) * cellH; y++)
                for (int x = cell % 4 * cellW; x < (cell % 4 + 1) * cellW; x++) {
                    int rgb = frame.getRGB(x, y);
                    r += rgb >> 16 & 255; g += rgb >> 8 & 255; b += rgb & 255;
                }
            double n = cellW * cellH, luma = (.299 * r + .587 * g + .114 * b) / n;
            assert Math.abs(luma - expected[cell]) <= 4 : "frame " + index + " cell " + cell + ": " + luma + " vs " + expected[cell];
        }
    }
}
