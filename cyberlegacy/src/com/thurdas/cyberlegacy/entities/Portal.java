package com.thurdas.cyberlegacy.entities;

import java.awt.*;
import java.util.Random;

public class Portal {
    public float x, y;
    public float radius = 45;
    private float rotation = 0;
    private int nextPhase;
    private static final float ROTATION_SPEED = 1.5f;
    private static final int STAR_COUNT = 60;
    private float[] stars;
    private Random random;

    public Portal(float x, float y, int nextPhase) {
        this.x = x;
        this.y = y;
        this.nextPhase = nextPhase;
        this.random = new Random();
        this.stars = new float[STAR_COUNT * 3];
        
        // Gerar posições das estrelas (x, y, brightness)
        for (int i = 0; i < STAR_COUNT; i++) {
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float distance = radius * 0.5f + random.nextFloat() * radius * 1.2f;
            stars[i * 3] = (float)(Math.cos(angle) * distance);
            stars[i * 3 + 1] = (float)(Math.sin(angle) * distance);
            stars[i * 3 + 2] = 50 + random.nextFloat() * 200;
        }
    }

    public void tick(double delta) {
        rotation += ROTATION_SPEED;
        if (rotation >= 360) rotation -= 360;
    }

    public void render(Graphics2D g2d) {
        var oldTransform = g2d.getTransform();
        g2d.translate(x + radius, y + radius);
        g2d.rotate(Math.toRadians(rotation));

        // Fundo preto profundo (espaço)
        g2d.setColor(new Color(0, 0, 5, 100));
        g2d.fillOval(-(int)(radius * 1.3f), -(int)(radius * 1.3f), (int)(radius * 2.6f), (int)(radius * 2.6f));

        // Braços espirais da galáxia (3 camadas de braços)
        drawSpiralArm(g2d, 0, new Color(180, 100, 200, 120), radius * 1.0f);
        drawSpiralArm(g2d, 120, new Color(150, 150, 255, 100), radius * 1.0f);
        drawSpiralArm(g2d, 240, new Color(100, 200, 255, 100), radius * 1.0f);

        // Disco da galáxia com gradiente
        for (int layer = 8; layer > 0; layer--) {
            float layerRadius = radius * (layer / 8.0f);
            int alpha = (int)(200 * (1.0f - (layer / 8.0f)));
            
            if (layer % 2 == 0) {
                g2d.setColor(new Color(150, 80, 200, alpha)); // Roxo/Magenta
            } else {
                g2d.setColor(new Color(100, 150, 255, alpha)); // Azul/Cyan
            }
            g2d.fillOval(-(int)layerRadius, -(int)layerRadius, (int)(layerRadius * 2), (int)(layerRadius * 2));
        }

        // Estrelas
        for (int i = 0; i < STAR_COUNT; i++) {
            float sx = stars[i * 3];
            float sy = stars[i * 3 + 1];
            float brightness = stars[i * 3 + 2];
            
            // Pulsação leve das estrelas
            brightness = Math.min(255, brightness + (float)Math.sin(rotation * 0.05f + i) * 50);
            
            g2d.setColor(new Color(255, 255, 255, (int)brightness));
            int starSize = brightness > 150 ? 2 : 1;
            g2d.fillOval((int)sx - starSize, (int)sy - starSize, starSize * 2, starSize * 2);
        }

        // Núcleo brilhante (buraco negro/centro galáctico)
        for (int glow = 3; glow > 0; glow--) {
            g2d.setColor(new Color(255, 200, 100, (int)(100 - glow * 30)));
            g2d.fillOval(-(glow * 2), -(glow * 2), glow * 4, glow * 4);
        }
        
        g2d.setColor(new Color(255, 220, 150, 255));
        g2d.fillOval(-2, -2, 4, 4);

        g2d.setTransform(oldTransform);

        // Texto "PHASE X"
        g2d.setColor(new Color(150, 220, 255, 255));
        g2d.setFont(new Font("Courier New", Font.BOLD, 14));
        String text = "PHASE " + nextPhase;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)(x + radius - fm.stringWidth(text) / 2);
        int textY = (int)(y + radius * 2 + 25);
        g2d.drawString(text, textX, textY);
    }

    private void drawSpiralArm(Graphics2D g2d, float startAngle, Color color, float maxRadius) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        float prevX = 0, prevY = 0;
        for (int i = 0; i <= 30; i++) {
            float t = i / 30.0f;
            float angle = startAngle + t * (float)(Math.PI * 4);
            float dist = maxRadius * t;
            
            float newX = (float)(Math.cos(angle) * dist);
            float newY = (float)(Math.sin(angle) * dist);
            
            if (i > 0) {
                g2d.drawLine((int)prevX, (int)prevY, (int)newX, (int)newY);
            }
            prevX = newX;
            prevY = newY;
        }
    }

    public boolean collidsWith(float px, float py, int width, int height) {
        float px_center = px + width / 2.0f;
        float py_center = py + height / 2.0f;
        float portalCenterX = x + radius;
        float portalCenterY = y + radius;
        
        float dx = px_center - portalCenterX;
        float dy = py_center - portalCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance < (radius + width / 2.0f);
    }

    public int getNextPhase() {
        return nextPhase;
    }
}
