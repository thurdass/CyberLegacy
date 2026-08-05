package com.thurdas.cyberlegacy.entities;

import java.awt.*;

public class Portal {
    public float x, y;
    public float radius = 32;
    private float rotation = 0;
    private float pulseAlpha = 1.0f;
    private int nextPhase;
    private static final float ROTATION_SPEED = 3.0f;

    public Portal(float x, float y, int nextPhase) {
        this.x = x;
        this.y = y;
        this.nextPhase = nextPhase;
    }

    public void tick(double delta) {
        rotation += ROTATION_SPEED;
        if (rotation >= 360) rotation -= 360;
        
        pulseAlpha = (float) (0.6f + 0.4f * Math.sin(System.currentTimeMillis() / 500.0));
    }

    public void render(Graphics2D g2d) {
        // Salvar transform
        var oldTransform = g2d.getTransform();
        
        // Transladar para o centro do portal
        g2d.translate(x + radius, y + radius);
        g2d.rotate(Math.toRadians(rotation));
        
        // Desenhar círculo externo
        g2d.setColor(new Color(0, 255, 255, (int)(100 * pulseAlpha)));
        g2d.fillOval(-(int)radius, -(int)radius, (int)(radius * 2), (int)(radius * 2));
        
        // Desenhar borda
        g2d.setColor(new Color(0, 255, 255, (int)(255 * pulseAlpha)));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(-(int)radius, -(int)radius, (int)(radius * 2), (int)(radius * 2));
        
        // Desenhar padrão interno
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 / 6) * i;
            int x1 = (int)(Math.cos(angle) * radius * 0.5);
            int y1 = (int)(Math.sin(angle) * radius * 0.5);
            int x2 = (int)(Math.cos(angle) * radius * 0.8);
            int y2 = (int)(Math.sin(angle) * radius * 0.8);
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Restaurar transform
        g2d.setTransform(oldTransform);
        
        // Desenhar texto "NEXT PHASE"
        g2d.setColor(new Color(0, 255, 255, (int)(200 * pulseAlpha)));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String text = "PHASE " + nextPhase;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)(x + radius - fm.stringWidth(text) / 2);
        int textY = (int)(y + radius + fm.getHeight() / 2);
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
