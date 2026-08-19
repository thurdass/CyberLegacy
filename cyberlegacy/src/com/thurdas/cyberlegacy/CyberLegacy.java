package com.thurdas.cyberlegacy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import javax.sound.sampled.*;
import java.io.File;

import com.thurdas.cyberlegacy.audio.AudioManager;
import com.thurdas.cyberlegacy.ui.UIManager;
import com.thurdas.cyberlegacy.ui.ClassSelector;
import com.thurdas.cyberlegacy.managers.WaveManager;
import com.thurdas.cyberlegacy.database.DatabaseManager;
import com.thurdas.cyberlegacy.entities.*;

public class CyberLegacy extends JPanel implements Runnable, KeyListener, MouseListener, MouseWheelListener {

    public final int TILE_SIZE = 32;


    private BufferedImage[] floorTextures = new BufferedImage[3];
    private BufferedImage[] wallTextures = new BufferedImage[3];
    private BufferedImage currentFloorTexture;
    private BufferedImage currentWallTexture;
    public int currentPhase = 1;

    public BufferedImage titleScreenTexture;
    public BufferedImage creditsScreenTexture;

    public enum State { MENU, CREDITS, CLASS_SELECTION, PLAYING, PAUSE, GAME_OVER, VICTORY }
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public State gameState = State.MENU;
    private JFrame frame;
    private boolean isFullscreen = false;
    private Thread thread;
    private boolean running = false;
    public int fps = 0;

    public float cameraX = 0, cameraY = 0;
    public float targetZoom = 1.0f, currentZoom = 1.0f;
    public float shakeIntensity = 0;

    public long lastStateChangeTime = 0;
    public int waveNotificationTimer = 0;

    public int selectedMenuOption = 0;
    public long lastMenuInputTime = 0;

    public boolean[] keys = new boolean[256];
    public boolean mouseAttack = false;

    public int[][] map;

    public Player player;
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<HealthOrbs> items = new ArrayList<>();
    public ArrayList<Particle> particles = new ArrayList<>();
    public ArrayList<com.thurdas.cyberlegacy.ui.FloatingText> floatingTexts = new ArrayList<>();
    public Portal portal = null;

    public UIManager uiManager;
    public WaveManager waveManager;
    public ClassSelector classSelector;
    public AudioManager audioManager;
    public DatabaseManager databaseManager;

    public CyberLegacy(JFrame frame) {
        this.frame = frame;
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(1280, 720));

        uiManager = new UIManager(this);
        waveManager = new WaveManager(this);
        classSelector = new ClassSelector(this);
        audioManager = new AudioManager();
        databaseManager = new DatabaseManager();

        initMap(1);
        loadTextures();

        audioManager.playMusic("cyberlegacy/assets/music/emmraan-attack-254128.wav");
        lastStateChangeTime = System.currentTimeMillis();
    }

    public void changePhase(int newPhase) {
        this.currentPhase = newPhase;
        int textureIndex = Math.min(newPhase - 1, 2);
        currentFloorTexture = floorTextures[textureIndex];
        currentWallTexture = wallTextures[textureIndex];

        initMap(newPhase);

        if (player != null) {
            player.x = 50 * TILE_SIZE;
            player.y = 50 * TILE_SIZE;
        }

        projectiles.clear();
        items.clear();
        particles.clear();
        portal = null;

        triggerShake(20);
    }

    private void loadTextures() {
        try {
            for (int i = 0; i < 3; i++) {
                File floorFile = new File("cyberlegacy/assets/img/floor" + (i + 1) + ".png");
                if (floorFile.exists()) floorTextures[i] = ImageIO.read(floorFile);

                File wallFile = new File("cyberlegacy/assets/img/parede" + (i + 1) + ".png");
                if (wallFile.exists()) wallTextures[i] = ImageIO.read(wallFile);
            }

            File titleFile = new File("cyberlegacy/assets/img/titlescreen.png");
            if (titleFile.exists()) titleScreenTexture = ImageIO.read(titleFile);


            File creditsFile = new File("cyberlegacy/assets/img/creditsscreen.png");
            if (creditsFile.exists()) creditsScreenTexture = ImageIO.read(creditsFile);

            currentFloorTexture = floorTextures[0];
            currentWallTexture = wallTextures[0];

        } catch (Exception e) {
            System.err.println("Erro ao carregar texturas do mapa: " + e.getMessage());
        }
    }

    private void initMap(int phase) {
        map = new int[100][100];
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                if (x == 0 || y == 0 || x == 99 || y == 99) {
                    map[x][y] = 1;
                } else {
                    map[x][y] = 0;
                }
            }
        }
        for (int x = 30; x <= 70; x++) {
            map[x][30] = 1;
            map[x][70] = 1;
        }
        for (int y = 30; y <= 70; y++) {
            map[30][y] = 1;
            map[70][y] = 1;
        }
        for (int i = 45; i <= 55; i++) {
            map[i][30] = 0; map[i][70] = 0;
            map[30][i] = 0; map[70][i] = 0;
        }

        int[][] bunkers = {{15, 15}, {85, 15}, {15, 85}, {85, 85}};
        for (int[] b : bunkers) {
            for (int i = -4; i <= 4; i++) {
                for (int j = -4; j <= 4; j++) {
                    if (Math.abs(i) == 4 || Math.abs(j) == 4) {
                        map[b[0] + i][b[1] + j] = 1;
                    }
                }
            }
            map[b[0]][b[1] + 4] = 0;
            map[b[0]][b[1] - 4] = 0;
        }
    }

    public void startNewGame(ClassSelector.PlayerClass selectedClass) {
        player = new Player(50 * TILE_SIZE, 50 * TILE_SIZE, selectedClass);
        enemies.clear();
        projectiles.clear();
        items.clear();
        particles.clear();
        floatingTexts.clear();

        waveManager.reset();
        audioManager.playMusic("cyberlegacy/assets/music/playmusic.wav");

        gameState = State.PLAYING;
        lastStateChangeTime = System.currentTimeMillis();
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;

            if (delta >= 1) {
                tick(delta / amountOfTicks);
                repaint();
                frames++;
                delta--;
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                fps = frames;
                frames = 0;
            }
        }
    }

    private void tick(double delta) {
        if (keys[KeyEvent.VK_F11]) {
            keys[KeyEvent.VK_F11] = false;
            toggleFullscreen();
        }

        if (waveNotificationTimer > 0) waveNotificationTimer--;

        switch (gameState) {
            case MENU:
                if (System.currentTimeMillis() - lastMenuInputTime > 150) {
                    if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
                        selectedMenuOption--;
                        if (selectedMenuOption < 0) selectedMenuOption = 2;
                        lastMenuInputTime = System.currentTimeMillis();
                    }
                    if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
                        selectedMenuOption++;
                        if (selectedMenuOption > 2) selectedMenuOption = 0;
                        lastMenuInputTime = System.currentTimeMillis();
                    }
                    if (keys[KeyEvent.VK_ENTER] && System.currentTimeMillis() - lastStateChangeTime > 400) {
                        keys[KeyEvent.VK_ENTER] = false;
                        if (selectedMenuOption == 0) {
                            gameState = State.CLASS_SELECTION;
                        } else if (selectedMenuOption == 1) {
                            gameState = State.CREDITS;
                        } else if (selectedMenuOption == 2) {
                            System.exit(0);
                        }
                        lastStateChangeTime = System.currentTimeMillis();
                    }
                }
                break;

            case CREDITS:
                if ((keys[KeyEvent.VK_ENTER] || keys[KeyEvent.VK_ESCAPE]) && System.currentTimeMillis() - lastStateChangeTime > 400) {
                    keys[KeyEvent.VK_ENTER] = false;
                    keys[KeyEvent.VK_ESCAPE] = false;
                    gameState = State.MENU;
                    lastStateChangeTime = System.currentTimeMillis();
                }
                break;

            case VICTORY:
            case GAME_OVER:
                if (keys[KeyEvent.VK_ENTER] && System.currentTimeMillis() - lastStateChangeTime > 1000) {
                    keys[KeyEvent.VK_ENTER] = false;
                    gameState = State.CLASS_SELECTION;
                    lastStateChangeTime = System.currentTimeMillis();
                }
                break;

            case CLASS_SELECTION:
                classSelector.tick();
                break;

            case PLAYING:
                handlePlayingState(delta);
                break;

            case PAUSE:
                if (keys[KeyEvent.VK_ESCAPE]) {
                    keys[KeyEvent.VK_ESCAPE] = false;
                    gameState = State.PLAYING;
                    lastStateChangeTime = System.currentTimeMillis();
                }
                break;
        }
    }

    private void handlePlayingState(double delta) {
        if (keys[KeyEvent.VK_ESCAPE]) {
            keys[KeyEvent.VK_ESCAPE] = false;
            gameState = State.PAUSE;
            lastStateChangeTime = System.currentTimeMillis();
            return;
        }

        if (player.health <= 0) {
            gameState = State.GAME_OVER;
            lastStateChangeTime = System.currentTimeMillis();
            return;
        }

        player.tick(this, delta);
        waveManager.tick(delta);

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.tick(this, player, delta);
            if (e.isReadyToRemove()) {
                player.addXp(e.xpReward);
                player.kills++;
                if (Math.random() < 0.3) items.add(new HealthOrbs(e.x, e.y, HealthOrbs.Type.HEALTH));
                enemies.remove(i);
            }
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.tick(this, delta);
            if (p.isDestroyed) projectiles.remove(i);
        }

        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).tick(player)) items.remove(i);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.tick(delta);
            if (p.life <= 0) particles.remove(i);
        }

        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            com.thurdas.cyberlegacy.ui.FloatingText ft = floatingTexts.get(i);
            ft.tick(delta);
            if (ft.life <= 0) floatingTexts.remove(i);
        }

        if (portal != null) {
            portal.tick(delta);
            if (portal.collidsWith(player.x, player.y, 32, 32)) {
                changePhase(portal.getNextPhase());
            }
        }

        currentZoom += (targetZoom - currentZoom) * 0.1f;
        float screenCenterX = (getWidth() / 2f) / currentZoom;
        float screenCenterY = (getHeight() / 2f) / currentZoom;

        cameraX += ((player.x - screenCenterX + 16) - cameraX) * 0.1f;
        cameraY += ((player.y - screenCenterY + 16) - cameraY) * 0.1f;

        if (shakeIntensity > 0) {
            cameraX += (Math.random() - 0.5) * shakeIntensity;
            cameraY += (Math.random() - 0.5) * shakeIntensity;
            shakeIntensity *= 0.9f;
            if (shakeIntensity < 0.5f) shakeIntensity = 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (gameState == State.PLAYING || gameState == State.PAUSE || gameState == State.GAME_OVER) {
            var oldTransform = g2d.getTransform();

            g2d.scale(currentZoom, currentZoom);
            g2d.translate(-cameraX, -cameraY);

            renderWorld(g2d, g);

            g2d.setTransform(oldTransform);
        }

        uiManager.render(g2d);
    }

    private void renderWorld(Graphics2D g2d, Graphics g) {
        int startX = Math.max(0, (int) (cameraX / TILE_SIZE));
        int startY = Math.max(0, (int) (cameraY / TILE_SIZE));
        int endX = Math.min(100, (int) ((cameraX + getWidth() / currentZoom) / TILE_SIZE) + 1);
        int endY = Math.min(100, (int) ((cameraY + getHeight() / currentZoom) / TILE_SIZE) + 1);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                if (map[x][y] == 1) {
                    if (currentWallTexture != null) {
                        g2d.drawImage(currentWallTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                    } else {
                        g.setColor(new Color(25, 20, 35));
                        g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        g.setColor(new Color(35, 30, 50));
                        g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                } else {
                    if (currentFloorTexture != null) {
                        g2d.drawImage(currentFloorTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                    } else {
                        g.setColor(new Color(13, 8, 20));
                        g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        for (HealthOrbs item : new ArrayList<>(items)) item.render(g2d);
        for (Enemy e : new ArrayList<>(enemies)) { e.renderShadow(g); e.render(g2d); }
        if (player != null) { player.render(g2d); }
        if (portal != null) { portal.render(g2d); }
        for (Projectile p : new ArrayList<>(projectiles)) p.render(g2d);
        for (Particle p : new ArrayList<>(particles)) p.render(g2d);
        for (com.thurdas.cyberlegacy.ui.FloatingText ft : new ArrayList<>(floatingTexts)) ft.render(g2d);

        if (player != null) drawLighting(g2d);
    }

    private void drawLighting(Graphics2D g2d) {
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(0, 0, 0, 0), new Color(10, 0, 20, 230)};
        Point2D center = new Point2D.Float(player.x + 16, player.y + 16);
        RadialGradientPaint p = new RadialGradientPaint(center, 400.0f, dist, colors);
        g2d.setPaint(p);
        g2d.fillRect((int) cameraX - 100, (int) cameraY - 100, (int) (getWidth() / currentZoom) + 200, (int) (getHeight() / currentZoom) + 200);
    }

    public void triggerShake(float intensity) { this.shakeIntensity = intensity; }

    private void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        isFullscreen = !isFullscreen;
        frame.dispose();
        frame.setUndecorated(isFullscreen);

        if (isFullscreen) {
            gd.setFullScreenWindow(frame);
        } else {
            gd.setFullScreenWindow(null);
            frame.pack();
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        this.requestFocus();
    }

    public boolean isSolid(float x, float y, int width, int height) {
        int tX1 = (int) (x / TILE_SIZE), tY1 = (int) (y / TILE_SIZE);
        int tX2 = (int) ((x + width) / TILE_SIZE), tY2 = (int) ((y + height) / TILE_SIZE);

        if (tX1 < 0 || tY1 < 0 || tX2 >= 100 || tY2 >= 100) return true;

        return map[tX1][tY1] == 1 || map[tX2][tY1] == 1 || map[tX1][tY2] == 1 || map[tX2][tY2] == 1;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256) keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256) keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) mouseAttack = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) mouseAttack = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        targetZoom += (e.getWheelRotation() < 0) ? 0.1f : -0.1f;
        targetZoom = Math.max(0.8f, Math.min(targetZoom, 2.5f));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Cyber Legacy");
        CyberLegacy game = new CyberLegacy(frame);
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.start();
    }
}
