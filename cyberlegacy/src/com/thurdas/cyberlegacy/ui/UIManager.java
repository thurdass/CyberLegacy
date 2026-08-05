package com.thurdas.cyberlegacy.ui;

import com.thurdas.cyberlegacy.CyberLegacy;
import com.thurdas.cyberlegacy.managers.WaveManager;
import com.thurdas.cyberlegacy.ui.ClassSelector;
import com.thurdas.cyberlegacy.ui.FloatingText;

import java.awt.*;
import java.awt.image.BufferedImage;

public class UIManager {
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

    // --- HUD / menu rendering methods (copied from original file) ---

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

            // Fundo escuro com efeito scan lines
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, w, h);
            
            // Scan lines
            g.setColor(new Color(0, 255, 200, 20));
            for (int i = 0; i < h; i += 4) {
                g.drawLine(0, i, w, i);
            }

            if (secondsLeft > 0) {
                String numText = String.valueOf(secondsLeft);
                Font largeFont = new Font("Courier New", Font.BOLD, 250);
                g.setFont(largeFont);
                FontMetrics fm = g.getFontMetrics();
                int numX = (w - fm.stringWidth(numText)) / 2;
                int numY = h / 2 + 80;

                // Efeito glitch - renderizar múltiplas vezes com offsets aleatórios
                for (int j = 0; j < 3; j++) {
                    int offsetX = (int)((Math.random() - 0.5) * 15);
                    int offsetY = (int)((Math.random() - 0.5) * 15);
                    
                    if (j == 0) {
                        g.setColor(new Color(255, 0, 255, 150));  // Magenta
                    } else if (j == 1) {
                        g.setColor(new Color(0, 255, 255, 150));  // Cyan
                    } else {
                        g.setColor(new Color(255, 50, 150, 150)); // Rosa neon
                    }
                    g.drawString(numText, numX + offsetX, numY + offsetY);
                }

                // Número principal - amarelo neon
                g.setColor(new Color(255, 255, 0, 255));
                g.drawString(numText, numX, numY);
            } else {
                String fightText = "FIGHT!";
                Font fightFont = new Font("Courier New", Font.BOLD, 200);
                g.setFont(fightFont);
                FontMetrics fmFight = g.getFontMetrics();
                int fightX = (w - fmFight.stringWidth(fightText)) / 2;
                int fightY = h / 2 + 60;

                // Glitch effect para FIGHT
                for (int j = 0; j < 4; j++) {
                    int offsetX = (int)((Math.random() - 0.5) * 20);
                    int offsetY = (int)((Math.random() - 0.5) * 20);
                    
                    if (j == 0) {
                        g.setColor(new Color(255, 0, 255, 120));  // Magenta
                    } else if (j == 1) {
                        g.setColor(new Color(0, 255, 255, 120));  // Cyan
                    } else if (j == 2) {
                        g.setColor(new Color(255, 50, 150, 120)); // Rosa neon
                    } else {
                        g.setColor(new Color(255, 100, 0, 120));  // Laranja
                    }
                    g.drawString(fightText, fightX + offsetX, fightY + offsetY);
                }

                // Texto principal FIGHT - verde neon
                g.setColor(new Color(0, 255, 100, 255));
                g.drawString(fightText, fightX, fightY);
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

        if (game.creditsScreenTexture != null) {
            g.drawImage(game.creditsScreenTexture, 0, 0, w, h, null);


        } else {

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
        for (com.thurdas.cyberlegacy.entities.Enemy e : game.enemies) {
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
