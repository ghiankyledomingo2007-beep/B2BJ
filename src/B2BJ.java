import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings("serial")
public final class B2BJ extends JPanel {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int SPRITE_SIZE = 96;
    private static final int PILLAR_SIZE = 800;
    private static final Color BACKGROUND = new Color(24, 31, 44);

    private final RuinedOutpostMap map = new RuinedOutpostMap();
    private final Player player = new Player(map.spawnX(), map.spawnY());
    private final BufferedImage terrainSheet = loadImage(
            "assets/tilesets/ruined_outpost/ruined_outpost_wang.png");
    private final BufferedImage pillarImage = loadImage(
            "assets/props/ruined_outpost/ruined_stone_pillar.png");
    private final BufferedImage slimeSheet = loadSlimeSheet();
    private final SlimeAnimation slimeAnimation = new SlimeAnimation();
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private long previousFrame = System.nanoTime();

    private B2BJ() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BACKGROUND);
        bindMovementKeys();
        new Timer(16, this::updateGame).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("B2BJ");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.add(new B2BJ());
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    private void updateGame(ActionEvent event) {
        long now = System.nanoTime();
        double seconds = Math.min((now - previousFrame) / 1_000_000_000.0, 0.05);
        previousFrame = now;

        int horizontal = (right ? 1 : 0) - (left ? 1 : 0);
        int vertical = (down ? 1 : 0) - (up ? 1 : 0);
        player.move(horizontal, vertical, seconds, map);
        slimeAnimation.update(horizontal, vertical, seconds);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int cameraX = cameraPosition(player.x(), WIDTH, map.worldWidth());
        int cameraY = cameraPosition(player.y(), HEIGHT, map.worldHeight());
        drawTerrain(canvas, cameraX, cameraY);

        if (player.y() > map.pillarSortY()) {
            drawPillar(canvas, cameraX, cameraY);
        }
        drawPlayer(canvas, cameraX, cameraY);
        if (player.y() <= map.pillarSortY()) {
            drawPillar(canvas, cameraX, cameraY);
        }
        canvas.dispose();
    }

    private void drawPlayer(Graphics2D canvas, int cameraX, int cameraY) {
        int x = (int) Math.round(player.x()) - cameraX - SPRITE_SIZE / 2;
        int y = (int) Math.round(player.y()) - cameraY - SPRITE_SIZE / 2;
        if (slimeSheet != null) {
            int sourceX = slimeAnimation.frame() * SlimeAnimation.CELL_SIZE;
            int sourceY = slimeAnimation.row() * SlimeAnimation.CELL_SIZE;
            int left = slimeAnimation.flipHorizontal() ? x + SPRITE_SIZE : x;
            int right = slimeAnimation.flipHorizontal() ? x : x + SPRITE_SIZE;
            canvas.drawImage(slimeSheet, left, y, right, y + SPRITE_SIZE,
                    sourceX, sourceY,
                    sourceX + SlimeAnimation.CELL_SIZE,
                    sourceY + SlimeAnimation.CELL_SIZE, null);
        } else {
            canvas.setColor(new Color(79, 227, 216));
            canvas.fillOval(x, y + 16, SPRITE_SIZE, SPRITE_SIZE - 16);
        }
    }

    private void drawPillar(Graphics2D canvas, int cameraX, int cameraY) {
        if (pillarImage == null) {
            return;
        }
        int x = (int) Math.round(map.pillarX()) - cameraX - PILLAR_SIZE / 2;
        int y = (int) Math.round(map.pillarY()) - cameraY - PILLAR_SIZE / 2;
        canvas.drawImage(pillarImage, x, y, PILLAR_SIZE, PILLAR_SIZE, null);
    }

    private void drawTerrain(Graphics2D canvas, int cameraX, int cameraY) {
        if (terrainSheet == null) {
            return;
        }

        int firstColumn = cameraX / RuinedOutpostMap.TILE_SIZE;
        int firstRow = cameraY / RuinedOutpostMap.TILE_SIZE;
        int lastColumn = Math.min(map.widthInTiles() - 1,
                (cameraX + WIDTH) / RuinedOutpostMap.TILE_SIZE);
        int lastRow = Math.min(map.heightInTiles() - 1,
                (cameraY + HEIGHT) / RuinedOutpostMap.TILE_SIZE);

        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                int mask = map.tileMask(column, row);
                int sourceX = WangTileset.sourceX(mask);
                int sourceY = WangTileset.sourceY(mask);
                int x = column * RuinedOutpostMap.TILE_SIZE - cameraX;
                int y = row * RuinedOutpostMap.TILE_SIZE - cameraY;
                canvas.drawImage(terrainSheet,
                        x, y, x + RuinedOutpostMap.TILE_SIZE, y + RuinedOutpostMap.TILE_SIZE,
                        sourceX, sourceY, sourceX + 32, sourceY + 32, null);
            }
        }
    }

    private static int cameraPosition(double playerPosition, int viewportSize, int worldSize) {
        return (int) Math.round(Math.max(0,
                Math.min(worldSize - viewportSize, playerPosition - viewportSize / 2.0)));
    }

    private void bindMovementKeys() {
        bind(KeyEvent.VK_A, "left", value -> left = value);
        bind(KeyEvent.VK_LEFT, "leftArrow", value -> left = value);
        bind(KeyEvent.VK_D, "right", value -> right = value);
        bind(KeyEvent.VK_RIGHT, "rightArrow", value -> right = value);
        bind(KeyEvent.VK_W, "up", value -> up = value);
        bind(KeyEvent.VK_UP, "upArrow", value -> up = value);
        bind(KeyEvent.VK_S, "down", value -> down = value);
        bind(KeyEvent.VK_DOWN, "downArrow", value -> down = value);
    }

    private void bind(int keyCode, String name, KeyState keyState) {
        InputMap inputs = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getActionMap();
        inputs.put(KeyStroke.getKeyStroke(keyCode, 0, false), name + "Pressed");
        inputs.put(KeyStroke.getKeyStroke(keyCode, 0, true), name + "Released");
        actions.put(name + "Pressed", action(() -> keyState.set(true)));
        actions.put(name + "Released", action(() -> keyState.set(false)));
    }

    private static AbstractAction action(Runnable runnable) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        };
    }

    private static BufferedImage loadSlimeSheet() {
        return loadImage("assets/characters/slime/slime_sprite_sheet.png");
    }

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(Path.of(path).toFile());
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }

    @FunctionalInterface
    private interface KeyState {
        void set(boolean value);
    }
}
