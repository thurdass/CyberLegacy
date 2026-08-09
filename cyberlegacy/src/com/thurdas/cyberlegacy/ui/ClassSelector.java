package com.thurdas.cyberlegacy.ui;

import com.thurdas.cyberlegacy.CyberLegacy;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class ClassSelector {
    public enum PlayerClass { KATANA, SHOOTER, HACKER }

    private static final int MENU_CARD_WIDTH = 150;
    private static final int SWORDER_FRAME_COUNT = 18;
    private static final int SWORDER_MENU_SCALE = 2;

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
                int sw = sheet.getWidth() / SWORDER_FRAME_COUNT;
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
                int scale = (i == 0) ? SWORDER_MENU_SCALE : 3;
                int imgW = menuSprites[i].getWidth() * scale;
                int imgH = menuSprites[i].getHeight() * scale;
                int drawX = x + (MENU_CARD_WIDTH - imgW) / 2;
                int drawY = y + (i == 0 ? 10 : 25);

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
