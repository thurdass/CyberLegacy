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

public class CyberLegacy extends JPanel implements Runnable, KeyListener, MouseWheelListener {

    public final int TILE_SIZE = 32;

    // --- VARIÁVEIS DE TEXTURA ---
    private BufferedImage[] floorTextures = new BufferedImage[3];
    private BufferedImage[] wallTextures = new BufferedImage[3];
    private BufferedImage currentFloorTexture;
    private BufferedImage currentWallTexture;
    public int currentPhase = 1;

    public BufferedImage titleScreenTexture;
    public BufferedImage creditsScreenTexture; // Nova textura de fundo para os créditos

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

    public int[][] map;

    public Player player;
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<HealthOrbs> items = new ArrayList<>();
    public ArrayList<Particle> particles = new ArrayList<>();
    public ArrayList<FloatingText> floatingTexts = new ArrayList<>();

    public UIManager uiManager;
    public WaveManager waveManager;
    public ClassSelector classSelector;
    public AudioManager audioManager;

    public CyberLegacy(JFrame frame) {
        this.frame = frame;
        this.addKeyListener(this);
        this.addMouseWheelListener(this);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(1280, 720));

        uiManager = new UIManager(this);
        waveManager = new WaveManager(this);
        classSelector = new ClassSelector(this);
        audioManager = new AudioManager();

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
            if (e.health <= 0) {
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
            FloatingText ft = floatingTexts.get(i);
            ft.tick(delta);
            if (ft.life <= 0) floatingTexts.remove(i);
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

        for (HealthOrbs item : items) item.render(g2d);
        for (Enemy e : enemies) { e.renderShadow(g); e.render(g2d); }
        if (player != null) { player.render(g2d); }
        for (Projectile p : projectiles) p.render(g2d);
        for (Particle p : particles) p.render(g2d);
        for (FloatingText ft : floatingTexts) ft.render(g2d);

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

class AudioManager {
    private Clip musicClip;

    public void playMusic(String trackPath) {
        try {
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop();
                musicClip.close();
            }
            File audioFile = new File(trackPath);
            if (audioFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                musicClip = AudioSystem.getClip();
                musicClip.open(audioStream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip.start();
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar música: " + e.getMessage());
        }
    }

    public void playSound(String sfxPath) {
        try {
            File audioFile = new File(sfxPath);
            if (audioFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar efeito sonoro: " + e.getMessage());
        }
    }
}

class UIManager {
    private CyberLegacy game;

    public UIManager(CyberLegacy game) {
        this.game = game;
    }

    public void render(Graphics2D g) {
        int w = game.getWidth();
        int h = game.getHeight();

        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g.setColor(Color.WHITE);

        if (game.gameState == CyberLegacy.State.MENU) {
            drawProceduralMenu(g, w, h);
        } else if (game.gameState == CyberLegacy.State.CREDITS) {
            drawCredits(g, w, h);
        } else if (game.gameState == CyberLegacy.State.CLASS_SELECTION) {
            game.classSelector.render(g, w, h);
        } else if (game.gameState == CyberLegacy.State.PLAYING || game.gameState == CyberLegacy.State.PAUSE || game.gameState == CyberLegacy.State.GAME_OVER) {
            drawHUD(g, w, h);
        }
    }

    private void drawHUD(Graphics2D g, int w, int h) {
        int baseX = 120;
        int baseY = 30;

        float hpPercent = Math.max(0, game.player.health / (float) game.player.maxHealth);
        float staPercent = Math.max(0, game.player.stamina / (float) game.player.maxStamina);
        float xpPercent = Math.max(0, game.player.xp / (float) game.player.maxXp);

        drawSlantedBar(g, baseX, baseY, 300, 30, 20, new Color(0, 80, 0), new Color(0, 255, 0), hpPercent);
        drawOutlinedTextHUD(g, game.player.health + "/" + game.player.maxHealth, baseX + 15, baseY + 23, new Font("Impact", Font.ITALIC, 22));

        drawSlantedBar(g, baseX, baseY + 30, 220, 20, 15, new Color(0, 50, 100), new Color(0, 160, 255), staPercent);
        drawOutlinedTextHUD(g, (int)(staPercent * 100) + "%", baseX + 12, baseY + 46, new Font("Impact", Font.ITALIC, 16));

        drawSlantedBar(g, baseX, baseY + 50, 140, 16, 12, new Color(100, 80, 0), new Color(255, 230, 0), xpPercent);
        drawOutlinedTextHUD(g, "LVL " + game.player.level, baseX + 10, baseY + 63, new Font("Impact", Font.ITALIC, 14));

        g.setColor(Color.WHITE);
        g.fillPolygon(new int[]{20, 130, 120, 10}, new int[]{20, 25, 130, 125}, 4);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(new int[]{20, 130, 120, 10}, new int[]{20, 25, 130, 125}, 4);
        g.setStroke(new BasicStroke(1));

        g.setColor(new Color(15, 105, 95));
        g.fillPolygon(new int[]{25, 125, 115, 15}, new int[]{25, 30, 125, 120}, 4);

        if (game.player != null) {
            BufferedImage portrait = game.player.getPortrait();
            if (portrait != null) {
                g.drawImage(portrait, 35, 40, 70, 70, null);
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillOval(50, 50, 40, 40);
            }
        }

        if (game.waveManager.isCountingDown) {
            long remainingTime = 3000 - (System.currentTimeMillis() - game.waveManager.countdownStartTime);
            int secondsLeft = (int) Math.ceil(remainingTime / 1000.0);

            if (secondsLeft > 0) {
                drawOutlinedText(g, "WAVES START IN: " + secondsLeft, h / 3, new Font("Impact", Font.BOLD, 70), new Color(255, 255, 0), w);
            } else {
                drawOutlinedText(g, "FIGHT!", h / 3, new Font("Impact", Font.BOLD, 70), new Color(0, 255, 0), w);
            }
        }
        else if (game.waveNotificationTimer > 0) {
            g.setFont(new Font("Impact", Font.BOLD, 48));
            String waveText = "WAVE " + game.waveManager.currentWave;
            if (game.waveManager.currentWave % 5 == 0) waveText = "BOSS WAVE!";

            FontMetrics fm = g.getFontMetrics();
            int tx = (w - fm.stringWidth(waveText)) / 2;
            int ty = h / 4;

            g.setColor(new Color(255, 0, 150, 100));
            g.drawString(waveText, tx + 2, ty + 2);
            g.setColor(game.waveManager.currentWave % 5 == 0 ? new Color(255, 50, 50) : new Color(0, 255, 255));
            g.drawString(waveText, tx, ty);
        }

        drawMinimap(g, w, h);

        if (game.gameState == CyberLegacy.State.PAUSE) {
            g.setColor(new Color(10, 5, 20, 200));
            g.fillRect(0, 0, w, h);
            drawCenteredText(g, "SYSTEM PAUSED", h / 2, new Font("Segoe UI", Font.BOLD, 40), new Color(0, 255, 255), w);
        }

        if (game.gameState == CyberLegacy.State.GAME_OVER) {
            g.setColor(new Color(30, 0, 10, 220));
            g.fillRect(0, 0, w, h);

            drawOutlinedText(g, "SYSTEM COMPROMISED", h / 3, new Font("Impact", Font.BOLD, 70), new Color(255, 0, 50), w);

            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            drawCenteredText(g, "Viruses Liquidated: " + game.player.kills, h / 2 - 20, g.getFont(), Color.WHITE, w);
            drawCenteredText(g, "Max Level Achieved: " + game.player.level, h / 2 + 20, g.getFont(), Color.WHITE, w);

            if (System.currentTimeMillis() % 1000 < 500) {
                drawOutlinedText(g, "> PRESS ENTER TO OVERRIDE / RESTART <", h / 2 + 120, new Font("Impact", Font.PLAIN, 28), new Color(0, 255, 255), w);
            }
        }
    }

    private void drawSlantedBar(Graphics2D g, int x, int y, int width, int height, int slant, Color bg, Color fg, float percentage) {
        Polygon pBg = new Polygon(new int[]{x, x + width, x + width - slant, x}, new int[]{y, y, y + height, y + height}, 4);
        g.setColor(Color.BLACK);
        g.fillPolygon(pBg);
        g.setColor(bg);
        g.fillPolygon(new int[]{x+2, x + width-2, x + width - slant-2, x+2}, new int[]{y+2, y+2, y + height-2, y + height-2}, 4);

        Shape oldClip = g.getClip();
        g.clipRect(x, y, (int)(width * percentage), height);
        g.setColor(fg);
        g.fillPolygon(pBg);
        g.setClip(oldClip);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.drawPolygon(pBg);
        g.setStroke(new BasicStroke(1));
    }

    private void drawOutlinedTextHUD(Graphics2D g, String text, int x, int y, Font font) {
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y + 1);
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
    }

    private void drawProceduralMenu(Graphics2D g, int w, int h) {
        if (game.titleScreenTexture != null) {
            g.drawImage(game.titleScreenTexture, 0, 0, w, h, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
        }

        Font titleFont = new Font("Serif", Font.BOLD, 80);
        drawOutlinedText(g, "CYBER LEGACY", h / 4, titleFont, new Color(255, 240, 220), w);

        String[] options = {"PLAY", "Credits", "Quit"};
        Font optionFont = new Font("Serif", Font.PLAIN, 32);
        g.setFont(optionFont);
        FontMetrics fm = g.getFontMetrics();

        int startY = h / 2;
        int spacing = 80;

        for (int i = 0; i < options.length; i++) {
            String opt = options[i];
            int textWidth = fm.stringWidth(opt);
            int x = (w - textWidth) / 2;
            int y = startY + (i * spacing);

            if (i == game.selectedMenuOption) {
                int boxWidth = textWidth + 80;
                int boxHeight = fm.getHeight() + 10;

                g.setColor(Color.WHITE);
                g.fillRect((w - boxWidth) / 2, y - fm.getAscent() - 5, boxWidth, boxHeight);

                g.setColor(Color.BLACK);
                g.drawString(opt, x, y);
            } else {
                g.setColor(Color.WHITE);
                g.drawString(opt, x, y);
            }
        }
    }

    private void drawCredits(Graphics2D g, int w, int h) {
        // Verifica se a textura de fundo dos créditos foi carregada
        if (game.creditsScreenTexture != null) {
            g.drawImage(game.creditsScreenTexture, 0, 0, w, h, null);
            // Desenha um filtro semi-transparente escuro para o texto branco não sumir caso a imagem seja clara

        } else {
            // Fundo preto padrão se não tiver imagem
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
        }

        Font titleFont = new Font("Serif", Font.BOLD, 50);


        g.setFont(new Font("Serif", Font.PLAIN, 28));
        g.setColor(Color.WHITE);




        g.setFont(new Font("Serif", Font.ITALIC, 20));
        String backText = "Press ENTER or ESC to return";

        if (System.currentTimeMillis() % 1500 < 1000) {
            g.drawString(backText, (w - g.getFontMetrics().stringWidth(backText)) / 2, h - 80);
        }
    }

    private void drawOutlinedText(Graphics2D g, String text, int y, Font font, Color color, int w) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;

        g.setColor(new Color(10, 0, 20));
        int off = 2;
        g.drawString(text, x - off, y - off);
        g.drawString(text, x + off, y - off);
        g.drawString(text, x - off, y + off);
        g.drawString(text, x + off, y + off);

        g.setColor(color);
        g.drawString(text, x, y);
    }

    private void drawMinimap(Graphics2D g, int w, int h) {
        int mmSize = 150;
        int startX = w - mmSize - 20;
        int startY = 20;

        g.setColor(new Color(25, 15, 35, 160));
        g.fillRoundRect(startX, startY, mmSize, mmSize, 10, 10);

        float scale = 1.5f;

        g.setColor(new Color(60, 40, 80));
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                if (game.map[x][y] == 1) {
                    g.fillRect(startX + (int) (x * scale), startY + (int) (y * scale), 2, 2);
                }
            }
        }

        g.setColor(new Color(255, 0, 80));
        for (Enemy e : game.enemies) {
            int ex = (int) ((e.x / game.TILE_SIZE) * scale);
            int ey = (int) ((e.y / game.TILE_SIZE) * scale);
            if (ex >= 0 && ex < mmSize && ey >= 0 && ey < mmSize) {
                g.fillRect(startX + ex, startY + ey, 3, 3);
            }
        }

        g.setColor(new Color(0, 255, 255));
        int px = (int) ((game.player.x / game.TILE_SIZE) * scale);
        int py = (int) ((game.player.y / game.TILE_SIZE) * scale);
        if (px >= 0 && px < mmSize && py >= 0 && py < mmSize) {
            g.fillOval(startX + px - 2, startY + py - 2, 5, 5);
        }

        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(0, 255, 255, 60));
        g.drawRoundRect(startX, startY, mmSize, mmSize, 10, 10);
    }

    private void drawCenteredText(Graphics g, String text, int y, Font font, Color color, int screenWidth) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int x = (screenWidth - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}

class FloatingText {
    float x, y;
    String text;
    Color color;
    int life = 60;

    public FloatingText(String text, float x, float y, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void tick(double delta) {
        y -= 1.5f * delta;
        life--;
    }

    public void render(Graphics2D g) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, life * 4)));
        g.setFont(new Font("Impact", Font.PLAIN, 20));
        g.drawString(text, (int) x, (int) y);
    }
}

class ClassSelector {
    public enum PlayerClass { KATANA, SHOOTER, HACKER }

    private CyberLegacy game;
    private int selectedIndex = 0;
    private long lastInputTimer = 0;

    private BufferedImage[] menuSprites = new BufferedImage[3];

    public ClassSelector(CyberLegacy game) {
        this.game = game;
        loadMenuSprites();
    }

    private void loadMenuSprites() {
        try {
            File fKatana = new File("cyberlegacy/assets/img/sworder.png");
            if (fKatana.exists()) {
                BufferedImage sheet = ImageIO.read(fKatana);
                int sw = sheet.getWidth() / 18;
                int sh = sheet.getHeight();
                menuSprites[0] = sheet.getSubimage(0, 0, sw, sh);
            }

            File fShooter = new File("cyberlegacy/assets/img/jogador.png");
            if (fShooter.exists()) {
                BufferedImage sheet = ImageIO.read(fShooter);
                int sw = sheet.getWidth() / 20;
                int sh = sheet.getHeight();
                menuSprites[1] = sheet.getSubimage(0, 0, sw, sh);
            }

            File fHacker = new File("cyberlegacy/assets/img/tux.png");
            if (fHacker.exists()) {
                BufferedImage sheet = ImageIO.read(fHacker);
                int sw = sheet.getWidth() / 8;
                int sh = sheet.getHeight() / 11;
                menuSprites[2] = sheet.getSubimage(0, 4 * sh, sw, sh);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar as sprites do menu: " + e.getMessage());
        }
    }

    public void tick() {
        if (System.currentTimeMillis() - game.lastStateChangeTime < 400) return;

        if (System.currentTimeMillis() - lastInputTimer > 180) {
            if (game.keys[KeyEvent.VK_A] || game.keys[KeyEvent.VK_LEFT]) {
                selectedIndex = (selectedIndex - 1 + 3) % 3;
                lastInputTimer = System.currentTimeMillis();
            }
            if (game.keys[KeyEvent.VK_D] || game.keys[KeyEvent.VK_RIGHT]) {
                selectedIndex = (selectedIndex + 1) % 3;
                lastInputTimer = System.currentTimeMillis();
            }
            if (game.keys[KeyEvent.VK_ENTER]) {
                game.keys[KeyEvent.VK_ENTER] = false;
                game.startNewGame(PlayerClass.values()[selectedIndex]);
                lastInputTimer = System.currentTimeMillis();
            }
        }
    }

    public void render(Graphics2D g, int w, int h) {

        g.setColor(new Color(10, 5, 25));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(0, 149, 255));
        g.setFont(new Font("Impact", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("CHOOSE YOUR CLASS", (w - fm.stringWidth("CHOOSE YOUR CLASS")) / 2, h / 4);

        String[] names = {"SWORDER", "SHOOTER", "HACKER"};
        Color[] colors = {new Color(0, 111, 255), new Color(119, 110, 5), new Color(94, 14, 134)};

        int totalWidth = (3 * 150) + (2 * 50);
        int startX = (w - totalWidth) / 2;

        for (int i = 0; i < 3; i++) {
            int x = startX + (i * 200);
            int y = h / 2 - 100;

            if (i == selectedIndex) {
                g.setColor(colors[i]);
                g.fillRoundRect(x - 5, y - 5, 160, 210, 10, 10);
            }

            g.setColor(new Color(25, 20, 35));
            g.fillRoundRect(x, y, 150, 200, 10, 10);

            if (menuSprites[i] != null) {
                int scale = 3;
                int imgW = menuSprites[i].getWidth() * scale;
                int imgH = menuSprites[i].getHeight() * scale;
                int drawX = x + (150 - imgW) / 2;
                int[] yOffsets = {35, 0, 0};
                int currentOffset = yOffsets[i];
                int drawY = y + 25 - currentOffset;

                g.drawImage(menuSprites[i], drawX, drawY, imgW, imgH, null);
            } else {
                int cx = x + 60;
                int cy = y + 45;

                g.setColor(Color.LIGHT_GRAY);
                g.fillOval(cx + 8, cy - 5, 16, 16);
                g.setColor(colors[i]);
                g.fillRoundRect(cx + 6, cy + 11, 20, 20, 4, 4);
                g.setColor(Color.LIGHT_GRAY);
                g.fillOval(cx, cy + 15, 8, 8);
                g.fillOval(cx + 24, cy + 15, 8, 8);
            }

            g.setColor(i == selectedIndex ? Color.BLACK : Color.WHITE);
            g.setFont(new Font("Impact", Font.PLAIN, 22));
            g.drawString(names[i], x + 35, y + 150);
        }
    }
}

class Player {
    private BufferedImage spriteSheet;
    private int animationFrame = 0;

    private double animationTimer = 0;
    private double animationSpeed = 0.20;

    private int spriteWidth = 32;
    private int spriteHeight = 32;
    private boolean isMoving = false;

    float x, y;
    int maxHealth, health;
    float maxStamina = 100, stamina = 100;
    int level = 1, xp = 0, maxXp = 50, kills = 0;
    float speed;
    int damage;
    long attackCooldown, lastAttack;

    boolean isDashing = false;
    long dashEndTime;

    ClassSelector.PlayerClass pClass;
    CyberLegacy.Direction facing = CyberLegacy.Direction.DOWN;
    boolean isAttacking = false;

    public Player(float x, float y, ClassSelector.PlayerClass pClass) {
        this.x = x;
        this.y = y;
        this.pClass = pClass;

        try {
            String spritePath = "";
            switch (pClass) {
                case KATANA:  spritePath = "cyberlegacy/assets/img/sworder.png"; break;
                case SHOOTER: spritePath = "cyberlegacy/assets/img/jogador.png"; break;
                case HACKER:  spritePath = "cyberlegacy/assets/img/tux.png"; break;
            }

            File spriteFile = new File(spritePath);
            if (spriteFile.exists()) {
                spriteSheet = ImageIO.read(spriteFile);

                if (pClass == ClassSelector.PlayerClass.HACKER) {
                    spriteWidth = spriteSheet.getWidth() / 8;
                    spriteHeight = spriteSheet.getHeight() / 11;
                } else if (pClass == ClassSelector.PlayerClass.KATANA) {
                    spriteWidth = 48;
                    spriteHeight = spriteSheet.getHeight();
                } else {
                    spriteWidth = spriteSheet.getWidth() / 20;
                    spriteHeight = spriteSheet.getHeight();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (pClass) {
            case KATANA:  maxHealth = 360; health = 200; speed = 220; damage = 80; attackCooldown = 450; break;
            case SHOOTER: maxHealth = 300;  health = 150;  speed = 250; damage = 40; attackCooldown = 150; break;
            case HACKER:  maxHealth = 230;  health = 100;  speed = 300; damage = 90; attackCooldown = 600; break;
        }
    }

    public void addXp(int amount) {
        xp += amount;
        if (xp >= maxXp) levelUp();
    }

    private void levelUp() {
        level++;
        xp -= maxXp;
        maxXp = (int) (maxXp * 1.5);
        maxHealth += 20;
        health = maxHealth;
        damage += 10;
    }

    public void tick(CyberLegacy game, double delta) {
        handleMovement(game, delta);
        handleCombat(game);
    }

    public BufferedImage getPortrait() {
        if (spriteSheet != null) {
            try {
                if (pClass == ClassSelector.PlayerClass.HACKER) {
                    return spriteSheet.getSubimage(0, 4 * spriteHeight, spriteWidth, spriteHeight);
                } else {
                    return spriteSheet.getSubimage(0, 0, spriteWidth, spriteHeight);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void handleMovement(CyberLegacy game, double delta) {
        float xMove = 0, yMove = 0;
        float currentSpeed = speed;

        CyberLegacy.Direction oldFacing = facing;

        if (!isDashing && stamina < maxStamina) {
            stamina += 7.5f * delta;
        }

        if (game.keys[KeyEvent.VK_SHIFT] && !isDashing && stamina >= 30) {
            isDashing = true;
            stamina -= 30;
            dashEndTime = System.currentTimeMillis() + 150;
        }

        if (isDashing) {
            currentSpeed *= 3.5f;
            Color dashColor = (pClass == ClassSelector.PlayerClass.KATANA) ? new Color(0, 255, 255) :
                    (pClass == ClassSelector.PlayerClass.SHOOTER) ? new Color(255, 255, 0) : new Color(150, 0, 255);
            game.particles.add(new Particle(x + 16, y + 16, dashColor));

            if (System.currentTimeMillis() > dashEndTime) isDashing = false;
        }

        if (game.keys[KeyEvent.VK_W] || game.keys[KeyEvent.VK_UP]) { yMove -= currentSpeed * delta; facing = CyberLegacy.Direction.UP; }
        if (game.keys[KeyEvent.VK_S] || game.keys[KeyEvent.VK_DOWN]) { yMove += currentSpeed * delta; facing = CyberLegacy.Direction.DOWN; }
        if (game.keys[KeyEvent.VK_A] || game.keys[KeyEvent.VK_LEFT]) { xMove -= currentSpeed * delta; facing = CyberLegacy.Direction.LEFT; }
        if (game.keys[KeyEvent.VK_D] || game.keys[KeyEvent.VK_RIGHT]) { xMove += currentSpeed * delta; facing = CyberLegacy.Direction.RIGHT; }

        if (xMove != 0 && yMove != 0) {
            xMove *= 0.7071f;
            yMove *= 0.7071f;
        }

        isMoving = (xMove != 0 || yMove != 0);

        if (isMoving) {
            if (oldFacing != facing) {
                animationFrame = 1;
                animationTimer = 0;
            } else {
                animationTimer += delta;
                if (animationTimer >= animationSpeed) {
                    animationTimer = 0;
                    animationFrame++;
                    if (animationFrame > 3) animationFrame = 0;
                }
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }

        int hitboxWidth = 16;
        int hitboxHeight = 16;
        float offsetX = 8;
        float offsetY = 16;

        if (!game.isSolid(x + xMove + offsetX, y + offsetY, hitboxWidth, hitboxHeight)) x += xMove;
        if (!game.isSolid(x + offsetX, y + yMove + offsetY, hitboxWidth, hitboxHeight)) y += yMove;
    }

    private void handleCombat(CyberLegacy game) {
        if (game.keys[KeyEvent.VK_SPACE] && System.currentTimeMillis() - lastAttack > attackCooldown) {
            lastAttack = System.currentTimeMillis();
            isAttacking = true;

            switch (pClass) {
                case SHOOTER:
                    game.projectiles.add(new Projectile(x, y, facing, true, damage, 600.0f, new Color(255, 255, 0), 6));
                    game.audioManager.playSound("cyberlegacy/assets/sfx/shoot.wav");
                    break;
                case HACKER:
                    game.projectiles.add(new Projectile(x, y, facing, true, damage, 300.0f, new Color(150, 0, 255), 20));
                    game.audioManager.playSound("cyberlegacy/assets/sfx/fireball.wav");
                    break;
                case KATANA:
                    game.audioManager.playSound("cyberlegacy/assets/sfx/daviddumaisaudio-sword-slash-and-swing-185432.wav");
                    break;
            }
        }

        if (isAttacking && System.currentTimeMillis() - lastAttack > 150) {
            isAttacking = false;
        }
    }

    public Rectangle getMeleeHitbox() {
        if (!isAttacking || pClass != ClassSelector.PlayerClass.KATANA) return null;
        int reach = 55;
        switch (facing) {
            case UP:    return new Rectangle((int) x - 15, (int) y - reach, 62, reach);
            case DOWN:  return new Rectangle((int) x - 15, (int) y + 32, 62, reach);
            case LEFT:  return new Rectangle((int) x - reach, (int) y - 15, reach, 62);
            case RIGHT: return new Rectangle((int) x + 32, (int) y - 15, reach, 62);
            default:    return null;
        }
    }

    public void renderShadow(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval((int) x + 4, (int) y + 24, 24, 12);
    }

    public void render(Graphics2D g2d) {
        int cx = (int) x;
        int cy = (int) y;

        if (spriteSheet != null) {
            int xClip = 0;
            int yClip = 0;
            boolean flip = false;

            if (pClass == ClassSelector.PlayerClass.HACKER) {
                int baseRow = 0;
                switch (facing) {
                    case DOWN:  baseRow = 4; break;
                    case LEFT:  baseRow = 1; flip = true; break;
                    case UP:    baseRow = 6; break;
                    case RIGHT: baseRow = 1; break;
                }

                if (baseRow * spriteHeight >= spriteSheet.getHeight()) baseRow = 0;

                xClip = (animationFrame % 4) * spriteWidth;
                if (xClip >= spriteSheet.getWidth()) xClip = 0;

                yClip = baseRow * spriteHeight;

            } else if (pClass == ClassSelector.PlayerClass.KATANA) {

                int frameIndex = 0;
                int animStep = animationFrame % 4;

                if (isAttacking) {

                    long elapsed = System.currentTimeMillis() - lastAttack;
                    int attackStep = (int)((elapsed / 150.0f) * 5);

                    if (attackStep > 4) attackStep = 4;

                    switch (facing) {
                        case RIGHT:
                            frameIndex = 6 + attackStep;
                            break;
                        case LEFT:
                            frameIndex = 6 + attackStep;
                            flip = true;
                            break;
                        case UP:
                            frameIndex = 12;
                            break;
                        case DOWN:
                            frameIndex = 1;
                            break;
                    }

                } else {

                    switch (facing) {
                        case DOWN:
                            int[] downFrames = {0,1,2,1};
                            frameIndex = downFrames[animStep];
                            break;
                        case UP:
                            int[] upFrames = {11,12,13,12};
                            frameIndex = upFrames[animStep];
                            break;
                        case RIGHT:
                            int[] rightFrames = {14,15,16,15};
                            frameIndex = rightFrames[animStep];
                            break;
                        case LEFT:
                            int[] leftFrames = {14,15,16,15};
                            frameIndex = leftFrames[animStep];
                            flip = true;
                            break;
                    }
                }

                xClip = frameIndex * spriteWidth;
                yClip = 0;
            }  else {
                int baseFrame = 0;
                switch (facing) {
                    case DOWN:  baseFrame = 0; break;
                    case LEFT:  baseFrame = 4; break;
                    case UP:    baseFrame = 12; break;
                    case RIGHT: baseFrame = 4; flip = true; break;
                }

                xClip = (baseFrame + (animationFrame % 4)) * spriteWidth;
                if (xClip + spriteWidth > spriteSheet.getWidth()) xClip = 0;
                yClip = 0;
            }

            try {
                BufferedImage frameAtual = spriteSheet.getSubimage(xClip, yClip, spriteWidth, spriteHeight);

                int drawX = cx + (16 - spriteWidth / 2);
                int drawY = cy + (32 - spriteHeight);

                if (flip) {
                    g2d.drawImage(frameAtual, drawX + spriteWidth, drawY, drawX, drawY + spriteHeight, 0, 0, spriteWidth, spriteHeight, null);
                } else {
                    g2d.drawImage(frameAtual, drawX, drawY, spriteWidth, spriteHeight, null);
                }
            } catch (Exception e) {
                drawFallbackShape(g2d, cx, cy);
            }
        } else {
            drawFallbackShape(g2d, cx, cy);
        }

        if (pClass == ClassSelector.PlayerClass.KATANA && isAttacking) {
            long elapsed = System.currentTimeMillis() - lastAttack;
            float progress = Math.min(1.0f, elapsed / 150.0f);
            int radius = 45;

            g2d.setColor(new Color(0, 255, 255, (int) (255 * (1.0f - progress))));
            g2d.setStroke(new BasicStroke(15f * (1.0f - progress), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int baseAngle = 0;
            switch (facing) {
                case RIGHT: baseAngle = 300; break;
                case UP:    baseAngle = 30; break;
                case LEFT:  baseAngle = 120; break;
                case DOWN:  baseAngle = 210; break;
            }

            int sweep = (int) (140 * progress);
            g2d.drawArc(cx + 16 - radius, cy + 16 - radius, radius * 2, radius * 2, baseAngle + 140 - sweep, sweep);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    private void drawFallbackShape(Graphics2D g2d, int cx, int cy) {
        Color clothColor = new Color(0, 255, 255);
        if (pClass == ClassSelector.PlayerClass.SHOOTER) clothColor = new Color(255, 255, 0);
        else if (pClass == ClassSelector.PlayerClass.HACKER) clothColor = new Color(150, 0, 255);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(cx + 8, cy + 2, 16, 16);

        g2d.setColor(new Color(20, 20, 20));
        g2d.fillRoundRect(cx + 6, cy + 18, 20, 14, 4, 4);

        g2d.setColor(clothColor);
        g2d.fillRect(cx + 10, cy + 20, 12, 4);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(cx + 2, cy + 22, 6, 6);
        g2d.fillOval(cx + 24, cy + 22, 6, 6);

        if (pClass == ClassSelector.PlayerClass.KATANA && !isAttacking) {
            g2d.setColor(new Color(0, 255, 255));
            switch (facing) {
                case RIGHT: g2d.fillRect(cx + 28, cy + 24, 16, 3); break;
                case LEFT:  g2d.fillRect(cx - 12, cy + 24, 16, 3); break;
                case UP:    g2d.fillRect(cx + 24, cy + 2, 3, 16); break;
                case DOWN:  g2d.fillRect(cx + 24, cy + 28, 3, 16); break;
            }
        }
    }
}

class Enemy {
    float x, y;
    int health, maxHealth, damage, xpReward;
    float speed;
    long lastHitTime;
    boolean isShocked = false;

    Color color = new Color(200, 0, 30);

    private static BufferedImage[] spriteSheets = new BufferedImage[4];
    private static int[] spriteWidths = new int[4];
    private static int[] spriteHeights = new int[4];
    private static boolean loaded = false;

    private int zombieType;

    private int animationFrame = 0;
    private double animationTimer = 0;
    private double animationSpeed = 0.15;
    private CyberLegacy.Direction facing = CyberLegacy.Direction.DOWN;

    public Enemy(float x, float y, int wave) {
        this.x = x;
        this.y = y;
        this.maxHealth = 40 + (wave * 15);
        this.health = maxHealth;
        this.speed = 90.0f + wave;
        this.damage = 15 + (wave * 2);
        this.xpReward = 15 + wave;

        if (!loaded) {
            loadSprites();
        }

        this.zombieType = (int) (Math.random() * 4);
    }

    private static void loadSprites() {
        loaded = true;
        String[] files = {
                "cyberlegacy/assets/img/zumbi2.png",
                "cyberlegacy/assets/img/zumbi2.png",
                "cyberlegacy/assets/img/zumbi3.png",
                "cyberlegacy/assets/img/zumbi4.png"
        };

        for (int i = 0; i < 4; i++) {
            try {
                File file = new File(files[i]);
                if (file.exists()) {
                    spriteSheets[i] = ImageIO.read(file);
                    spriteWidths[i] = spriteSheets[i].getWidth() / 16;
                    spriteHeights[i] = spriteSheets[i].getHeight();
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar sprite do inimigo " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    public void tick(CyberLegacy game, Player p, double delta) {
        if (isShocked) {
            if (System.currentTimeMillis() - lastHitTime > 150) isShocked = false;
            else return;
        }

        float xMove = 0, yMove = 0;
        if (x < p.x) xMove += speed * delta;
        if (x > p.x) xMove -= speed * delta;
        if (y < p.y) yMove += speed * delta;
        if (y > p.y) yMove -= speed * delta;

        boolean isMoving = false;

        if (Math.abs(xMove) > Math.abs(yMove)) {
            facing = (xMove > 0) ? CyberLegacy.Direction.RIGHT : CyberLegacy.Direction.LEFT;
            isMoving = true;
        } else if (yMove != 0) {
            facing = (yMove > 0) ? CyberLegacy.Direction.DOWN : CyberLegacy.Direction.UP;
            isMoving = true;
        }

        if (isMoving && !isShocked) {
            animationTimer += delta;
            if (animationTimer >= animationSpeed) {
                animationTimer = 0;
                animationFrame++;
                if (animationFrame > 3) animationFrame = 0;
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }

        if (!game.isSolid(x + xMove, y, 32, 32)) x += xMove;
        if (!game.isSolid(x, y + yMove, 32, 32)) y += yMove;

        Rectangle rEnemy = new Rectangle((int) x, (int) y, 32, 32);
        Rectangle rPlayer = new Rectangle((int) p.x, (int) p.y, 32, 32);

        if (rEnemy.intersects(rPlayer) && System.currentTimeMillis() - lastHitTime > 1000 && !p.isDashing) {
            p.health -= damage;
            lastHitTime = System.currentTimeMillis();
            game.triggerShake(10);
        }

        if (p.isAttacking && p.pClass == ClassSelector.PlayerClass.KATANA) {
            Rectangle hitBlade = p.getMeleeHitbox();
            if (hitBlade != null && hitBlade.intersects(rEnemy) && !isShocked) {
                takeDamage(p.damage, game);
            }
        }
    }

    public void takeDamage(int dmg, CyberLegacy game) {
        boolean isCrit = Math.random() < 0.2;
        int finalDmg = isCrit ? (int) (dmg * 1.5) : dmg;

        this.health -= finalDmg;
        this.isShocked = true;
        this.lastHitTime = System.currentTimeMillis();

        game.floatingTexts.add(new FloatingText("" + finalDmg, x, y - 10, isCrit ? new Color(255, 255, 0) : Color.WHITE));

        if (isCrit) game.triggerShake(8);
        for (int i = 0; i < 8; i++) {
            game.particles.add(new Particle(x + 16, y + 16, color));
        }
    }

    public void renderShadow(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval((int) x + 4, (int) y + 24, 24, 12);
    }

    public void render(Graphics2D g2d) {
        int cx = (int) x;
        int cy = (int) y;

        BufferedImage currentSheet = spriteSheets[zombieType];
        int sWidth = spriteWidths[zombieType];
        int sHeight = spriteHeights[zombieType];

        if (currentSheet != null && sWidth > 0) {

            int baseFrame = 0;
            switch (facing) {
                case DOWN:  baseFrame = 0; break;
                case LEFT:  baseFrame = 4; break;
                case RIGHT: baseFrame = 8; break;
                case UP:    baseFrame = 12; break;
            }

            int currentFrame = baseFrame + animationFrame;
            int xClip = currentFrame * sWidth;
            int yClip = 0;

            BufferedImage frame = currentSheet.getSubimage(xClip, yClip, sWidth, sHeight);

            int drawX = cx + (16 - sWidth / 2);
            int drawY = cy + (32 - sHeight);

            if (isShocked) {
                g2d.drawImage(frame, drawX, drawY, null);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect(drawX, drawY, sWidth, sHeight);
            } else {
                g2d.drawImage(frame, drawX, drawY, null);
            }

        } else {
            if (isShocked) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(cx, cy, 32, 32);
            } else {
                g2d.setColor(new Color(40, 40, 40));
                g2d.fillOval(cx + 8, cy + 2, 16, 16);
                g2d.setColor(new Color(255, 0, 50));
                g2d.fillRect(cx + 12, cy + 6, 3, 3);
                g2d.fillRect(cx + 18, cy + 6, 3, 3);
                g2d.setColor(new Color(30, 30, 30));
                g2d.fillRoundRect(cx + 6, cy + 18, 20, 14, 4, 4);
                g2d.setColor(new Color(40, 40, 40));
                g2d.fillOval(cx, cy + 10, 8, 8);
                g2d.fillOval(cx + 24, cy + 10, 8, 8);
            }
        }

        if (health < maxHealth && health > 0) {
            g2d.setColor(new Color(20, 0, 0));
            g2d.fillRect(cx - 4, cy - 8, 40, 5);
            g2d.setColor(new Color(255, 0, 50));
            g2d.fillRect(cx - 4, cy - 8, (int) ((health / (float) maxHealth) * 40), 5);
        }
    }
}

class Boss extends Enemy {
    private static BufferedImage bossSpriteSheet;
    private static int spriteWidth = -1;
    private static int spriteHeight = -1;
    private static boolean loadedBoss = false;

    private int animationFrame = 0;
    private double animationTimer = 0;
    private double animationSpeed = 0.15;
    private CyberLegacy.Direction facing = CyberLegacy.Direction.DOWN;

    private final int bossSize = 64;

    // --- Variáveis do Sistema de Dash ---
    private boolean isDashing = false;
    private long dashEndTime = 0;
    private long nextDashTime = 0;
    private float dashDx = 0;
    private float dashDy = 0;

    public Boss(float x, float y, int wave) {
        super(x, y, wave);

        this.maxHealth = 1000 + (wave * 60);
        this.health = maxHealth;
        this.damage = 30 + (wave * 5);
        this.speed = 65.0f + (wave * 0.5f);
        this.xpReward = 100 + (wave * 15);
        this.color = new Color(138, 43, 226);

        // O boss já nasce preparado para dar o primeiro dash depois de 2 segundos
        this.nextDashTime = System.currentTimeMillis() + 2000;

        if (!loadedBoss) {
            loadBossSprite();
        }
    }

    private static void loadBossSprite() {
        loadedBoss = true;
        try {
            File file = new File("cyberlegacy/assets/img/zumbi.png");
            if (file.exists()) {
                bossSpriteSheet = ImageIO.read(file);
                spriteWidth = bossSpriteSheet.getWidth() / 16;
                spriteHeight = bossSpriteSheet.getHeight();
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar sprite do boss: " + e.getMessage());
        }
    }

    @Override
    public void tick(CyberLegacy game, Player p, double delta) {
        if (isShocked) {
            if (System.currentTimeMillis() - lastHitTime > 150) isShocked = false;
            else return;
        }

        long currentTime = System.currentTimeMillis();
        float xMove = 0, yMove = 0;

        // Lógica de Movimento e Dash
        if (isDashing) {
            if (currentTime > dashEndTime) {
                // Finaliza o dash
                isDashing = false;
                // Define o próximo dash para 3 a 5 segundos no futuro
                nextDashTime = currentTime + 1000 + (long)(Math.random() * 1000);
            } else {
                // Movimento do dash com velocidade 6x maior
                float currentSpeed = speed * 6.0f;
                xMove = dashDx * currentSpeed * (float) delta;
                yMove = dashDy * currentSpeed * (float) delta;

                // Adiciona um rastro de partículas durante o dash
                game.particles.add(new Particle(x + bossSize / 2, y + bossSize / 2, color));
            }
        } else {
            // Movimento normal seguindo o player
            if (x < p.x) xMove += speed * delta;
            if (x > p.x) xMove -= speed * delta;
            if (y < p.y) yMove += speed * delta;
            if (y > p.y) yMove -= speed * delta;

            // Checa a distância entre o boss e o player
            float distToPlayer = (float) Math.hypot(p.x - x, p.y - y);

            // Inicia o dash se o tempo passou e o player está dentro de um raio de engajamento
            if (currentTime > nextDashTime && distToPlayer < 400) {
                isDashing = true;
                dashEndTime = currentTime + 400; // Duração do dash (400ms)

                // Trava a mira da direção (linha reta até o player)
                if (distToPlayer > 0) {
                    dashDx = (p.x - x) / distToPlayer;
                    dashDy = (p.y - y) / distToPlayer;
                }

                // Opcional: Tremer um pouquinho a tela na arrancada para dar impacto
                game.triggerShake(5);
            }
        }

        // --- Animação ---
        boolean isMoving = false;
        if (Math.abs(xMove) > Math.abs(yMove)) {
            facing = (xMove > 0) ? CyberLegacy.Direction.RIGHT : CyberLegacy.Direction.LEFT;
            isMoving = true;
        } else if (yMove != 0) {
            facing = (yMove > 0) ? CyberLegacy.Direction.DOWN : CyberLegacy.Direction.UP;
            isMoving = true;
        }

        if (isMoving && !isShocked) {
            animationTimer += delta;
            // Se estiver no dash, a perninha dele bate 2x mais rápido
            double currentAnimSpeed = isDashing ? animationSpeed / 2.0 : animationSpeed;

            if (animationTimer >= currentAnimSpeed) {
                animationTimer = 0;
                animationFrame++;
                if (animationFrame > 3) animationFrame = 0;
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }

        // Aplica a movimentação checando colisão
        if (!game.isSolid(x + xMove, y, bossSize, bossSize)) x += xMove;
        if (!game.isSolid(x, y + yMove, bossSize, bossSize)) y += yMove;

        // --- Verificação de Dano e Hitbox ---
        Rectangle rBoss = new Rectangle((int) x, (int) y, bossSize, bossSize);
        Rectangle rPlayer = new Rectangle((int) p.x, (int) p.y, 32, 32);

        // Se bater no player
        if (rBoss.intersects(rPlayer) && currentTime - lastHitTime > 1000 && !p.isDashing) {
            // Se bateu durante o dash, pode dar um empurrão ou dano extra. Aqui mantive padrão:
            p.health -= damage;
            lastHitTime = currentTime;
            game.triggerShake(18);
        }

        // Se o player (Katana) estiver atacando o boss
        if (p.isAttacking && p.pClass == ClassSelector.PlayerClass.KATANA) {
            Rectangle hitBlade = p.getMeleeHitbox();
            if (hitBlade != null && hitBlade.intersects(rBoss) && !isShocked) {
                takeDamage(p.damage, game);
            }
        }
    }

    @Override
    public void takeDamage(int dmg, CyberLegacy game) {
        boolean isCrit = Math.random() < 0.2;
        int finalDmg = isCrit ? (int) (dmg * 1.5) : dmg;

        this.health -= finalDmg;
        this.isShocked = true;
        this.lastHitTime = System.currentTimeMillis();

        game.floatingTexts.add(new FloatingText("" + finalDmg, x + 16, y - 10, isCrit ? new Color(255, 255, 0) : Color.WHITE));

        if (isCrit) game.triggerShake(10);
        for (int i = 0; i < 15; i++) {
            game.particles.add(new Particle(x + bossSize / 2, y + bossSize / 2, color));
        }
    }

    @Override
    public void renderShadow(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval((int) x + 8, (int) y + bossSize - 16, bossSize - 16, 20);
    }

    @Override
    public void render(Graphics2D g2d) {
        int cx = (int) x;
        int cy = (int) y;

        if (bossSpriteSheet != null && spriteWidth > 0) {
            int baseFrame = 0;
            switch (facing) {
                case DOWN:  baseFrame = 0; break;
                case LEFT:  baseFrame = 4; break;
                case RIGHT: baseFrame = 8; break;
                case UP:    baseFrame = 12; break;
            }

            int currentFrame = baseFrame + animationFrame;
            int xClip = currentFrame * spriteWidth;
            int yClip = 0;

            BufferedImage frame = bossSpriteSheet.getSubimage(xClip, yClip, spriteWidth, spriteHeight);

            if (isShocked) {
                g2d.drawImage(frame, cx, cy, bossSize, bossSize, null);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect(cx, cy, bossSize, bossSize);
            } else {
                g2d.drawImage(frame, cx, cy, bossSize, bossSize, null);
            }
        } else {
            if (isShocked) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(cx, cy, bossSize, bossSize);
            } else {
                g2d.setColor(new Color(150, 0, 255));
                g2d.fillRect(cx, cy, bossSize, bossSize);
            }
        }

        g2d.setFont(new Font("Impact", Font.PLAIN, 18));
        String bossName = "SYSTEM OVERLORD [BOSS]";
        FontMetrics fm = g2d.getFontMetrics();
        int nameWidth = fm.stringWidth(bossName);
        int nx = cx + (bossSize - nameWidth) / 2;
        int ny = cy - 18;

        g2d.setColor(Color.BLACK);
        g2d.drawString(bossName, nx - 1, ny - 1);
        g2d.drawString(bossName, nx + 1, ny - 1);
        g2d.drawString(bossName, nx - 1, ny + 1);
        g2d.drawString(bossName, nx + 1, ny + 1);

        g2d.setColor(new Color(255, 50, 50));
        g2d.drawString(bossName, nx, ny);

        if (health < maxHealth && health > 0) {
            g2d.setColor(new Color(20, 0, 0));
            g2d.fillRect(cx, cy - 12, bossSize, 8);
            g2d.setColor(new Color(255, 0, 50));
            g2d.fillRect(cx, cy - 12, (int) ((health / (float) maxHealth) * bossSize), 8);
        }
    }
}
class WaveManager {
    private CyberLegacy game;
    public int currentWave = 0;

    public boolean isCountingDown = false;
    public long countdownStartTime;

    public WaveManager(CyberLegacy game) {
        this.game = game;
    }

    public void reset() {
        currentWave = 0;
        isCountingDown = true;
        countdownStartTime = System.currentTimeMillis();
    }

    public void tick(double delta) {
        if (isCountingDown) {
            if (System.currentTimeMillis() - countdownStartTime >= 3000) {
                isCountingDown = false;
            } else {
                return;
            }
        }

        if (game.enemies.isEmpty()) {
            currentWave++;
            game.waveNotificationTimer = 140;

            int expectedPhase = (currentWave / 6) + 1;

            if (expectedPhase > game.currentPhase) {
                game.changePhase(expectedPhase);
            }

            spawnEnemiesForWave();
        }
    }

    private void spawnEnemiesForWave() {
        boolean isBossWave = (currentWave % 5 == 0);

        if (isBossWave) {
            boolean bossSpawned = false;
            while (!bossSpawned) {
                float spawnX = (float) (game.player.x + (Math.random() * 800 - 400));
                float spawnY = (float) (game.player.y + (Math.random() * 800 - 400));

                if (!game.isSolid(spawnX, spawnY, 64, 64)) {
                    game.enemies.add(new Boss(spawnX, spawnY, currentWave));
                    bossSpawned = true;
                }
            }
        }

        int enemiesToSpawn = 5 + (currentWave * 2);

        if (isBossWave) {
            enemiesToSpawn /= 2;
        }

        for (int i = 0; i < enemiesToSpawn; i++) {
            float spawnX = (float) (game.player.x + (Math.random() * 600 - 300));
            float spawnY = (float) (game.player.y + (Math.random() * 600 - 300));

            if (!game.isSolid(spawnX, spawnY, 32, 32)) {
                game.enemies.add(new Enemy(spawnX, spawnY, currentWave));
            } else {
                i--;
            }
        }
    }
}

class Projectile {
    float x, y, speed;
    CyberLegacy.Direction dir;
    boolean isPlayer;
    int damage, size;
    Color color;
    public boolean isDestroyed = false;

    public Projectile(float x, float y, CyberLegacy.Direction dir, boolean p, int d, float s, Color c, int sz) {
        this.x = x + 16 - (sz / 2.0f);
        this.y = y + 16 - (sz / 2.0f);
        this.dir = dir;
        this.isPlayer = p;
        this.damage = d;
        this.speed = s;
        this.color = c;
        this.size = sz;
    }

    public void tick(CyberLegacy game, double delta) {
        switch (dir) {
            case UP:    y -= speed * delta; break;
            case DOWN:  y += speed * delta; break;
            case LEFT:  x -= speed * delta; break;
            case RIGHT: x += speed * delta; break;
        }

        if (game.isSolid(x, y, size, size)) {
            isDestroyed = true;
            return;
        }

        Rectangle rProj = new Rectangle((int) x, (int) y, size, size);
        for (Enemy e : game.enemies) {
            if (new Rectangle((int) e.x, (int) e.y, 32, 32).intersects(rProj) && !e.isShocked) {
                e.takeDamage(damage, game);
                isDestroyed = true;
                return;
            }
        }

        game.particles.add(new Particle(x + size / 2, y + size / 2, color));
    }

    public void render(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int) x, (int) y, size, size);
    }
}

class Particle {
    float x, y, dx, dy;
    int life = 20;
    Color color;

    public Particle(float x, float y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.dx = (float) ((Math.random() - 0.5) * 6);
        this.dy = (float) ((Math.random() - 0.5) * 6);
    }

    public void tick(double delta) {
        x += dx;
        y += dy;
        life--;
    }

    public void render(Graphics2D g2d) {
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, life * 10)));
        g2d.fillRect((int) x, (int) y, 4, 4);
    }
}

class HealthOrbs {
    public enum Type { HEALTH }

    float x, y;
    Type type;

  
    private static BufferedImage healthImage;
    private static boolean loadedTextures = false;

    public HealthOrbs(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;

        if (!loadedTextures) {
            loadTextures();
        }
    }

    private static void loadTextures() {
        loadedTextures = true;
        try {

            File imgFile = new File("cyberlegacy/assets/img/orb.png");
            if (imgFile.exists()) {
                healthImage = ImageIO.read(imgFile);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar imagem do DropItem: " + e.getMessage());
        }
    }

    public boolean tick(Player p) {
        if (new Rectangle((int) x, (int) y, 16, 16).intersects(new Rectangle((int) p.x, (int) p.y, 32, 32))) {
            if (type == Type.HEALTH) {
                p.health = Math.min(p.health + 40, p.maxHealth);
            }
            return true;
        }
        return false;
    }

    public void render(Graphics2D g2d) {

        float floatOffset = (float) Math.sin(System.currentTimeMillis() / 200.0) * 3;
        int drawX = (int) x + 8;
        int drawY = (int) (y + 8 + floatOffset);

        if (type == Type.HEALTH && healthImage != null) {

            g2d.drawImage(healthImage, drawX, drawY, 16, 16, null);
        } else {

            g2d.setColor(new Color(0, 255, 100));
            g2d.fillOval(drawX, drawY, 16, 16);
        }
    }
}