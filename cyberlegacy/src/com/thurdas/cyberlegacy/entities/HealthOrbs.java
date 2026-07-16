package com.thurdas.cyberlegacy.entities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class HealthOrbs {
    public enum Type { HEALTH }

    public float x, y;
    public Type type;

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

    public boolean tick(com.thurdas.cyberlegacy.entities.Player p) {
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
