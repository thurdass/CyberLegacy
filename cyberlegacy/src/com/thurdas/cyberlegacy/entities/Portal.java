package com.thurdas.cyberlegacy.entities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.InputStream;

public class Portal {
    public float x, y;
    public float radius = 45;
    private int nextPhase;

    private static final int FRAME_WIDTH = 80;
    private static final int FRAME_HEIGHT = 80;
    private static final int FRAME_COUNT = 37;
    private static final double FRAME_DURATION = 0.08;

    private static BufferedImage spriteSheet;
    private static boolean spritesLoaded = false;

    private int animationFrame = 0;
    private double animationTimer = 0.0;

    public Portal(float x, float y, int nextPhase) {
        this.x = x;
        this.y = y;
        this.nextPhase = nextPhase;

        if (!spritesLoaded) loadSprites();
    }

    private static void loadSprites() {
        spritesLoaded = true;
        try {
            File file = new File("cyberlegacy/assets/img/portal.png");
            if (file.exists()) spriteSheet = ImageIO.read(file);

            if (spriteSheet == null) {
                try (InputStream resource = Portal.class.getClassLoader()
                        .getResourceAsStream("cyberlegacy/assets/img/portal.png")) {
                    if (resource != null) spriteSheet = ImageIO.read(resource);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar sprite do portal: " + e.getMessage());
        }
    }

    public void tick(double delta) {
        animationTimer += delta;
        while (animationTimer >= FRAME_DURATION) {
            animationTimer -= FRAME_DURATION;
            animationFrame = (animationFrame + 1) % FRAME_COUNT;
        }
    }

    public void render(Graphics2D g2d) {
        Graphics2D pixelGraphics = (Graphics2D) g2d.create();
        pixelGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        pixelGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        pixelGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        int drawX = Math.round(x + radius - FRAME_WIDTH / 2.0f);
        int drawY = Math.round(y + radius - FRAME_HEIGHT / 2.0f);

        if (spriteSheet != null) {
            try {
                BufferedImage frame = spriteSheet.getSubimage(
                        animationFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
                pixelGraphics.drawImage(frame, drawX, drawY, null);
            } catch (RasterFormatException e) {
                renderFallback(pixelGraphics, drawX, drawY);
            }
        } else {
            renderFallback(pixelGraphics, drawX, drawY);
        }

        pixelGraphics.dispose();

        g2d.setColor(new Color(150, 220, 255, 255));
        g2d.setFont(new Font("Courier New", Font.BOLD, 14));
        String text = "PHASE " + nextPhase;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int) (x + radius - fm.stringWidth(text) / 2.0f);
        int textY = (int) (y + radius * 2 + 25);
        g2d.drawString(text, textX, textY);
    }

    private void renderFallback(Graphics2D g2d, int drawX, int drawY) {
        g2d.setColor(new Color(40, 0, 120, 220));
        g2d.fillOval(drawX, drawY, FRAME_WIDTH, FRAME_HEIGHT);
        g2d.setColor(new Color(192, 64, 248, 230));
        g2d.fillOval(drawX + 12, drawY + 12, FRAME_WIDTH - 24, FRAME_HEIGHT - 24);
    }

    public boolean collidsWith(float px, float py, int width, int height) {
        float pxCenter = px + width / 2.0f;
        float pyCenter = py + height / 2.0f;
        float portalCenterX = x + radius;
        float portalCenterY = y + radius;

        float dx = pxCenter - portalCenterX;
        float dy = pyCenter - portalCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        return distance < (radius + width / 2.0f);
    }

    public int getNextPhase() {
        return nextPhase;
    }
}
