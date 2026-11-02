import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.Picture;

/**
 * The opening film: the trailer's story for WAKE lines 0-9, 1280x720 H.264 with a keyframe every second.
 * Worker threads decode whole one-second groups in parallel a little ahead of the picture (one thread
 * decodes only about 35 frames a second); the film's own sound line is the clock, so picture and sound
 * cannot drift. Anything missing or undecodable makes {@link #open()} return null, and the drawn
 * opening plays instead.
 */
final class IntroFilm {
    static final int WIDTH = 1280, HEIGHT = 720, FPS = 24;
    static final String VIDEO = "assets/ui/opening/intro_film.mp4", SOUND = "assets/audio/opening/intro_film.wav",
            SUBTITLES = "assets/ui/opening/intro_film_subs.txt";
    private static final int AHEAD = 2;      // groups decoded past the one on screen: 3 x 24 x 1.4 MB of YUV at most
    private static final AudioFormat FORMAT = new AudioFormat(44_100, 16, 2, true, false);

    private record Subtitle(double start, double end, boolean sage, String text) { }

    /** One second of decoded frames as YUV planes; `decoded` is published after each frame. */
    private static final class Group {
        final byte[][][] frames = new byte[FPS][][];
        volatile int decoded;
    }

    private final ByteBuffer video;
    private final byte[] pcm;
    private final List<Subtitle> subtitles;
    final int frames;
    private final ConcurrentHashMap<Integer, Group> groups = new ConcurrentHashMap<>();
    private final Object window = new Object();
    private final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    private volatile int showing;             // group on screen: workers stay within AHEAD of it
    private volatile boolean closed, failed, paused, started;
    private volatile SourceDataLine line;     // null without a sound device: the wall clock keeps time instead
    private long wallStart, wallPaused;
    private int converted = -1;

    private IntroFilm(ByteBuffer video, byte[] pcm, List<Subtitle> subtitles) throws Exception {
        this.video = video;
        this.pcm = pcm;
        this.subtitles = subtitles;
        frames = grab().getVideoTrack().getMeta().getTotalFrames();
        if (frames <= 0) throw new IllegalStateException("empty film");
    }

    /** The packaged film with decoding already started; null when it cannot play. */
    static IntroFilm open() {
        try {
            var film = load();
            film.start();
            return film;
        } catch (Exception | LinkageError broken) {   // LinkageError: a build without the decoder classes
            System.err.println("Opening film unavailable, drawing the opening instead: " + broken);
            return null;
        }
    }

    /** The packaged pieces, not yet playing (tests read them this way). */
    static IntroFilm load() throws Exception {
        try (InputStream picture = Ui.open(VIDEO); InputStream sound = Ui.open(SOUND); InputStream text = Ui.open(SUBTITLES)) {
            if (picture == null || sound == null || text == null) throw new IllegalStateException("opening film not packaged");
            return new IntroFilm(ByteBuffer.wrap(picture.readAllBytes()), pcm(sound),
                    subtitles(new String(text.readAllBytes(), StandardCharsets.UTF_8)));
        }
    }

    private FrameGrab grab() throws Exception {
        return FrameGrab.createFrameGrab(ByteBufferSeekableByteChannel.readFromByteBuffer(video.duplicate()));
    }

    private static byte[] pcm(InputStream sound) throws Exception {
        try (var raw = AudioSystem.getAudioInputStream(new BufferedInputStream(sound));
             var pcm = raw.getFormat().matches(FORMAT) ? raw : AudioSystem.getAudioInputStream(FORMAT, raw)) {
            return pcm.readAllBytes();
        }
    }

    private static List<Subtitle> subtitles(String text) {
        var list = new ArrayList<Subtitle>();
        for (String row : text.split("\n")) {
            var cells = row.split("\t");
            if (cells.length == 4)
                list.add(new Subtitle(Double.parseDouble(cells[0]), Double.parseDouble(cells[1]), cells[2].equals("s"), cells[3]));
        }
        return List.copyOf(list);
    }

    private void start() {
        int workers = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() - 1));
        for (int first = 0; first < workers; first++) {
            int from = first;
            daemon("b2bj-film-" + first, () -> decode(from, workers));
        }
        daemon("b2bj-film-sound", this::playSound);
    }

    private static void daemon(String name, Runnable body) {
        var thread = new Thread(body, name);
        thread.setDaemon(true);
        thread.start();
    }

    /** Groups first, first + stride, ...: seek to the group's keyframe, then decode its 24 frames in order. */
    private void decode(int first, int stride) {
        try {
            var grab = grab();
            for (int group = first; group * FPS < frames && !closed; group += stride) {
                synchronized (window) {
                    while (!closed && group > showing + AHEAD) window.wait();
                }
                if (closed) return;
                var decoded = new Group();
                groups.put(group, decoded);
                grab.seekToFrameSloppy(group * FPS);
                for (int i = 0; i < FPS && group * FPS + i < frames && !closed; i++) {
                    Picture picture = grab.getNativeFrame();
                    if (picture == null) break;
                    decoded.frames[i] = new byte[][]{picture.getPlaneData(0).clone(),
                            picture.getPlaneData(1).clone(), picture.getPlaneData(2).clone()};
                    decoded.decoded = i + 1;
                }
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        } catch (Exception | LinkageError | OutOfMemoryError broken) {
            System.err.println("Opening film stopped: " + broken);
            failed = true;
        }
    }

    /** The sound plays on its own line; that line's frame position is the film's clock. */
    private void playSound() {
        SourceDataLine out;
        try {
            out = AudioSystem.getSourceDataLine(FORMAT);
            out.open(FORMAT, 44_100 / 5 * 4);                  // 200 ms
        } catch (Exception unavailable) {
            out = null;
        }
        synchronized (this) {
            if (closed) {if (out != null) out.close(); return;}
            line = out;
            wallStart = wallPaused = System.nanoTime();
            started = true;
            if (out == null) return;
            if (!paused) out.start();
        }
        byte[] chunk = new byte[4096];
        for (int at = 0; at < pcm.length && !closed; ) {
            int n = Math.min(chunk.length, pcm.length - at);
            double gain = GameAudio.muted() ? 0 : GameAudio.masterVolume();
            for (int i = 0; i + 1 < n; i += 2) {
                int sample = (int) Math.round((short) ((pcm[at + i] & 0xff) | pcm[at + i + 1] << 8) * gain);
                chunk[i] = (byte) sample;
                chunk[i + 1] = (byte) (sample >> 8);
            }
            out.write(chunk, 0, n);                             // blocks while paused (the line is stopped)
            at += n;
        }
    }

    /** Seconds of the film played. */
    synchronized double seconds() {
        if (!started) return 0;
        if (line != null) return line.getLongFramePosition() / (double) FORMAT.getFrameRate();
        long now = paused ? wallPaused : System.nanoTime();
        return Math.max(0, now - wallStart) / 1e9;
    }

    /** Pause with the game: the sound line stops where it is, and so does the picture. */
    synchronized void setPaused(boolean pause) {
        if (pause == paused || closed) return;
        paused = pause;
        if (line != null) {
            if (pause) line.stop(); else line.start();
        } else if (started) {
            if (pause) wallPaused = System.nanoTime(); else wallStart += System.nanoTime() - wallPaused;
        }
    }

    boolean finished() {
        return failed || seconds() >= frames / (double) FPS;
    }

    void close() {
        synchronized (this) {
            closed = true;
            if (line != null) {line.stop(); line.flush(); line.close();}
        }
        synchronized (window) { window.notifyAll(); }
        groups.clear();
    }

    void draw(Graphics2D canvas, int width, int height) { drawAt(canvas, width, height, seconds()); }

    /** The frame for film time t, letterboxed to the 1280x720 stage like every other screen, then the subtitles. */
    void drawAt(Graphics2D canvas, int width, int height, double t) {
        canvas.setColor(Color.BLACK);
        canvas.fillRect(0, 0, width, height);
        int frame = Math.min(frames - 1, (int) (t * FPS));
        int group = frame / FPS;
        if (group != showing) {
            showing = group;
            groups.keySet().removeIf(old -> old < group);
            synchronized (window) { window.notifyAll(); }
        }
        var decoded = groups.get(group);
        if (decoded != null && frame != converted && decoded.decoded > frame % FPS) {
            convert(decoded.frames[frame % FPS], pixels);
            converted = frame;
        }
        double scale = TitleScreen.menuScale(width, height);
        var g = (Graphics2D) canvas.create();
        Ui.crisp(g);
        g.translate((width - WIDTH * scale) / 2, (height - HEIGHT * scale) / 2);
        g.scale(scale, scale);
        if (converted >= 0) g.drawImage(image, 0, 0, null);
        for (var subtitle : subtitles) {
            if (t < subtitle.start || t >= subtitle.end) continue;
            var line = (Graphics2D) g.create();
            float alpha = (float) Math.max(0, Math.min(1, Math.min(t - subtitle.start, subtitle.end - t) / .15));
            line.setComposite(AlphaComposite.SrcOver.derive(alpha));
            Ui.outlinedCentered(line, subtitle.text, WIDTH / 2, subtitle.sage ? 640 : 676, 2,
                    subtitle.sage ? Ui.GOLD_LIGHT : Ui.TEXT);
            line.dispose();
        }
        Ui.textRight(g, "X  SKIP", 1232, 48, 2, Ui.MUTED);
        g.dispose();
    }

    /** YUV 4:2:0 (JCodec stores samples minus 128) to RGB, BT.601 limited range like the encoder's input. */
    static void convert(byte[][] planes, int[] out) {
        byte[] luma = planes[0], blue = planes[1], red = planes[2];
        for (int y = 0; y < HEIGHT; y++) {
            int row = y * WIDTH, chroma = (y >> 1) * (WIDTH / 2);
            for (int x = 0; x < WIDTH; x++) {
                int l = (luma[row + x] + 128 - 16) * 298, u = blue[chroma + (x >> 1)], v = red[chroma + (x >> 1)];
                int r = (l + 409 * v + 128) >> 8, gr = (l - 100 * u - 208 * v + 128) >> 8, b = (l + 516 * u + 128) >> 8;
                out[row + x] = clamp(r) << 16 | clamp(gr) << 8 | clamp(b);
            }
        }
    }

    private static int clamp(int value) { return value < 0 ? 0 : Math.min(255, value); }

    /** Frame `index` as the game shows it: decoded after a seek to its group's keyframe (tests). */
    BufferedImage frame(int index) throws Exception {
        var grab = grab();
        grab.seekToFrameSloppy(index / FPS * FPS);
        Picture picture = null;
        for (int i = 0; i <= index % FPS; i++) picture = grab.getNativeFrame();
        var still = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        convert(new byte[][]{picture.getPlaneData(0), picture.getPlaneData(1), picture.getPlaneData(2)},
                ((DataBufferInt) still.getRaster().getDataBuffer()).getData());
        return still;
    }

    /** Put frame `index` on screen without the decoder threads (renders of the intro for checking). */
    void show(int index) throws Exception {
        frame(index).getRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
        converted = index;
    }

    int[] keyframes() throws Exception { return grab().getVideoTrack().getMeta().getSeekFrames(); }

    double soundSeconds() { return pcm.length / (double) FORMAT.getFrameSize() / FORMAT.getFrameRate(); }

    int subtitleCount() { return subtitles.size(); }
}
