import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Lab 6's simple chase using B2BJ sprites; no campaign, combat or save state. */
@SuppressWarnings("serial")
public final class EnemyAiLab extends JPanel {
    static final int WIDTH = 1280, HEIGHT = 720, SPRITE_SIZE = 80;
    static final double ENEMY_DETECTION_RANGE = 300;
    static final double ENEMY_STOP_DISTANCE = 80;
    static final double ENEMY_SPEED = 80; // 0.08 pixels/ms; this game loop uses seconds.
    private static final double PLAYER_SPEED = 180;
    private static final double STOP_EPSILON = 1e-9; // Diagonal floating-point rounding must not keep the enemy chasing.
    private final BufferedImage slime = requiredImage("assets/characters/slime/slime_sprite_sheet.png");
    private final BufferedImage idle = requiredImage("assets/characters/slime/slime_idle.png");
    private final BufferedImage remnant = requiredImage("assets/characters/wisp/remnant_walk.png");
    private SlimeAnimation playerAnimation = new SlimeAnimation();
    private WispAnimation enemyAnimation = new WispAnimation();
    private final Set<Integer> keys = new HashSet<>();
    private final Timer timer;
    private final boolean startTimer;
    private long previousFrame;
    private double playerX, playerY, enemyX, enemyY, velocityX, velocityY;
    private boolean paused;

    EnemyAiLab(boolean startTimer) {
        this.startTimer = startTimer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("B2BJ Laboratory 6 Enemy AI demonstration");
        reset(1);
        timer = new Timer(16, event -> {
            long now = System.nanoTime();
            if (!paused) {
                // Only the moving sprite area changes; avoid copying the full window every frame.
                var dirty = playerBounds().getBounds().union(enemyBounds().getBounds());
                boolean wasColliding = checkCollision();
                step(Math.min((now - previousFrame) / 1_000_000_000.0, .05));
                dirty.add(playerBounds().getBounds());
                dirty.add(enemyBounds().getBounds());
                dirty.grow(3, 3);
                repaint(dirty);
                if (wasColliding != checkCollision()) repaint(0, 0, 180, 32);
            }
            previousFrame = now;
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) { press(event.getKeyCode()); }
            @Override public void keyReleased(KeyEvent event) { release(event.getKeyCode()); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) {
                keys.clear();
                paused = true;
                repaint();
            }
        });
    }

    @Override public void addNotify() {
        super.addNotify();
        previousFrame = System.nanoTime();
        if (startTimer) timer.start();
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    void press(int key) {
        if (!keys.add(key)) return;
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_3) reset(key - KeyEvent.VK_0);
        else if (key == KeyEvent.VK_R) reset(1);
        else if (key == KeyEvent.VK_SPACE) paused = !paused;
        repaint();
    }

    void release(int key) { keys.remove(key); }

    void reset(int scene) {
        if (scene < 1 || scene > 3) throw new IllegalArgumentException("Scene must be 1, 2 or 3");
        keys.clear();
        enemyX = 400;
        enemyY = playerY = 340;
        playerX = enemyX + switch (scene) { case 1 -> 420; case 2 -> 240; default -> 80; };
        playerAnimation = new SlimeAnimation();
        enemyAnimation = new WispAnimation();
        paused = false;
        updateEnemyAI();
    }

    Rectangle2D playerBounds() { return new Rectangle2D.Double(playerX, playerY, SPRITE_SIZE, SPRITE_SIZE); }
    Rectangle2D enemyBounds() { return new Rectangle2D.Double(enemyX, enemyY, SPRITE_SIZE, SPRITE_SIZE); }
    boolean checkCollision() { return playerBounds().intersects(enemyBounds()); }
    double distance() {
        return Math.hypot(playerBounds().getCenterX() - enemyBounds().getCenterX(),
                playerBounds().getCenterY() - enemyBounds().getCenterY());
    }
    double enemySpeed() { return Math.hypot(velocityX, velocityY); }
    boolean paused() { return paused; }
    String state() {
        if (distance() > ENEMY_DETECTION_RANGE) return "OUTSIDE RANGE";
        return distance() <= ENEMY_STOP_DISTANCE + STOP_EPSILON ? "STOPPED NEAR PLAYER" : "CHASING";
    }

    private void updateEnemyAI() {
        double playerCenterX = playerBounds().getCenterX();
        double playerCenterY = playerBounds().getCenterY();
        double enemyCenterX = enemyBounds().getCenterX();
        double enemyCenterY = enemyBounds().getCenterY();
        double differenceX = playerCenterX - enemyCenterX;
        double differenceY = playerCenterY - enemyCenterY;
        double distance = Math.hypot(differenceX, differenceY);
        velocityX = velocityY = 0;
        if (distance > ENEMY_DETECTION_RANGE || distance <= ENEMY_STOP_DISTANCE + STOP_EPSILON) return;
        velocityX = differenceX / distance * ENEMY_SPEED;
        velocityY = differenceY / distance * ENEMY_SPEED;
    }

    void step(double seconds) {
        if (paused || !Double.isFinite(seconds) || seconds <= 0) return;
        int horizontal = held(KeyEvent.VK_D, KeyEvent.VK_RIGHT) - held(KeyEvent.VK_A, KeyEvent.VK_LEFT);
        int vertical = held(KeyEvent.VK_S, KeyEvent.VK_DOWN) - held(KeyEvent.VK_W, KeyEvent.VK_UP);
        double length = Math.max(1, Math.hypot(horizontal, vertical));
        playerX = Math.max(24, Math.min(WIDTH - SPRITE_SIZE - 24,
                playerX + horizontal / length * PLAYER_SPEED * seconds));
        playerY = Math.max(24, Math.min(HEIGHT - SPRITE_SIZE - 24,
                playerY + vertical / length * PLAYER_SPEED * seconds));
        playerAnimation.update(horizontal, vertical, seconds);
        updateEnemyAI();
        double speed = enemySpeed();
        if (speed > 0) {
            // Cap the last step so a slow frame cannot carry the enemy inside the 80px stop distance.
            double travel = Math.min(speed * seconds, distance() - ENEMY_STOP_DISTANCE);
            enemyX += velocityX / speed * travel;
            enemyY += velocityY / speed * travel;
            enemyAnimation.update(velocityX, velocityY, seconds);
        }
        updateEnemyAI();
    }

    private int held(int first, int second) { return keys.contains(first) || keys.contains(second) ? 1 : 0; }
    private static BufferedImage requiredImage(String path) {
        return Objects.requireNonNull(B2BJ.loadImage(path), "Missing lab sprite: " + path);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(16, 24, 34));
        g.fillRect(0, 0, getWidth(), getHeight());
        boolean moving = playerAnimation.hopping();
        drawFrame(g, moving ? slime : idle, moving ? playerAnimation.frame() : playerAnimation.idleFrame(),
                moving ? playerAnimation.row() : 0, playerX, playerY, moving && playerAnimation.flipHorizontal());
        int direction = enemyAnimation.row(), row = direction == 2 ? 2 : direction == 1 || direction == 3 ? 1 : 0;
        drawFrame(g, remnant, enemyAnimation.frame() % 4, row, enemyX, enemyY, direction == 1);
        g.setStroke(new BasicStroke(2));
        g.setColor(Color.RED);
        g.draw(playerBounds());
        g.setColor(Color.BLUE);
        g.draw(enemyBounds());
        g.setColor(checkCollision() ? Color.RED : Color.WHITE);
        label(g, checkCollision() ? "COLLISION!" : "No Collision", 12, 22, 13);
        g.setColor(new Color(177, 188, 198));
        if (paused) label(g, "Paused - press SPACE to resume", 12, 48, 15);
        g.dispose();
    }

    private static void drawFrame(Graphics2D g, BufferedImage sheet, int frame, int row,
            double x, double y, boolean flip) {
        int left = (int) Math.round(x), top = (int) Math.round(y), cell = 48;
        g.drawImage(sheet, flip ? left + SPRITE_SIZE : left, top, flip ? left : left + SPRITE_SIZE,
                top + SPRITE_SIZE, frame * cell, row * cell, (frame + 1) * cell, (row + 1) * cell, null);
    }

    private static void label(Graphics2D g, String text, int x, int y, int size) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        g.drawString(text, x, y);
    }
}
