package com.thurdas.cyberlegacy.entities;

import java.awt.*;

public class Portal {
    public float x, y;
    public float radius = 45;
    private float rotation = 0;
    private int nextPhase;
    private static final float ROTATION_SPEED = 2.5f;

    public Portal(float x, float y, int nextPhase) {
        this.x = x;
        this.y = y;
        this.nextPhase = nextPhase;
    }

    public void tick(double delta) {
        rotation += ROTATION_SPEED;
        if (rotation >= 360) rotation -= 360;
    }

    public void render(Graphics2D g2d) {
        var oldTransform = g2d.getTransform();
        g2d.translate(x + radius, y + radius);
        g2d.rotate(Math.toRadians(rotation));

        // Camada 1: Núcleo escuro bem escuro (preto azulado)
        g2d.setColor(new Color(5, 15, 35, 255));
        g2d.fillOval(-(int)(radius * 0.4f), -(int)(radius * 0.4f), (int)(radius * 0.8f), (int)(radius * 0.8f));

        // Camada 2: Azul escuro profundo
        g2d.setColor(new Color(20, 60, 120, 200));
        g2d.setStroke(new BasicStroke(6));
        g2d.drawOval(-(int)(radius * 0.5f), -(int)(radius * 0.5f), (int)(radius * 1.0f), (int)(radius * 1.0f));

        // Camada 3: Roxo/Magenta escuro
        g2d.setColor(new Color(80, 20, 120, 180));
        g2d.setStroke(new BasicStroke(5));
        g2d.drawOval(-(int)(radius * 0.65f), -(int)(radius * 0.65f), (int)(radius * 1.3f), (int)(radius * 1.3f));

        // Camada 4: Azul médio
        g2d.setColor(new Color(50, 150, 220, 200));
        g2d.setStroke(new BasicStroke(7));
        g2d.drawOval(-(int)(radius * 0.8f), -(int)(radius * 0.8f), (int)(radius * 1.6f), (int)(radius * 1.6f));

        // Camada 5: Cyan/Azul claro (mais brilhante)
        g2d.setColor(new Color(100, 200, 255, 230));
        g2d.setStroke(new BasicStroke(8));
        g2d.drawOval(-(int)radius, -(int)radius, (int)(radius * 2), (int)(radius * 2));

        // Camada 6: Rosa/Magenta neon
        g2d.setColor(new Color(255, 100, 200, 150));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval(-(int)(radius * 1.1f), -(int)(radius * 1.1f), (int)(radius * 2.2f), (int)(radius * 2.2f));

        // Linhas radiais internas (energia)
        g2d.setColor(new Color(150, 220, 255, 180));
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i;
            int x1 = (int)(Math.cos(angle) * radius * 0.3f);
            int y1 = (int)(Math.sin(angle) * radius * 0.3f);
            int x2 = (int)(Math.cos(angle) * radius * 0.75f);
            int y2 = (int)(Math.sin(angle) * radius * 0.75f);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Glow/brilho no núcleo
        g2d.setColor(new Color(100, 200, 255, 80));
        g2d.fillOval(-(int)(radius * 0.25f), -(int)(radius * 0.25f), (int)(radius * 0.5f), (int)(radius * 0.5f));

        g2d.setTransform(oldTransform);

        // Texto "PHASE X" embaixo
        g2d.setColor(new Color(150, 220, 255, 255));
        g2d.setFont(new Font("Courier New", Font.BOLD, 14));
        String text = "PHASE " + nextPhase;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)(x + radius - fm.stringWidth(text) / 2);
        int textY = (int)(y + radius * 2 + 25);
        g2d.drawString(text, textX, textY);
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
