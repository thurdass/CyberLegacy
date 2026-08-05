package com.thurdas.cyberlegacy.entities;

import java.awt.*;
import java.util.ArrayList;

public class Portal {
    public float x, y;
    public float radius = 40;
    private float rotation = 0;
    private float pulseAlpha = 1.0f;
    private int nextPhase;
    private ArrayList<PortalParticle> portalParticles;
    private static final float ROTATION_SPEED = 4.0f;

    public Portal(float x, float y, int nextPhase) {
        this.x = x;
        this.y = y;
        this.nextPhase = nextPhase;
        this.portalParticles = new ArrayList<>();
        spawnInitialParticles();
    }

    private void spawnInitialParticles() {
        for (int i = 0; i < 20; i++) {
            portalParticles.add(new PortalParticle(x + radius, y + radius));
        }
    }

    public void tick(double delta) {
        rotation += ROTATION_SPEED;
        if (rotation >= 360) rotation -= 360;
        
        pulseAlpha = (float) (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 400.0));

        for (int i = portalParticles.size() - 1; i >= 0; i--) {
            PortalParticle p = portalParticles.get(i);
            p.tick(delta);
            if (p.life <= 0) {
                portalParticles.remove(i);
            }
        }

        if (Math.random() < 0.3) {
            portalParticles.add(new PortalParticle(x + radius, y + radius));
        }
    }

    public void render(Graphics2D g2d) {
        // Renderizar partículas do portal
        for (PortalParticle p : portalParticles) {
            p.render(g2d);
        }

        // Salvar transform
        var oldTransform = g2d.getTransform();
        
        // Transladar para o centro do portal
        g2d.translate(x + radius, y + radius);
        g2d.rotate(Math.toRadians(rotation));
        
        // Camada 1: Núcleo interno (roxo/azul)
        GradientPaint coreGradient = new GradientPaint(
            -(int)radius, -(int)radius, new Color(100, 0, 200, (int)(150 * pulseAlpha)),
            (int)radius, (int)radius, new Color(0, 100, 255, (int)(80 * pulseAlpha))
        );
        g2d.setPaint(coreGradient);
        g2d.fillOval(-(int)(radius * 0.7f), -(int)(radius * 0.7f), (int)(radius * 1.4f), (int)(radius * 1.4f));

        // Camada 2: Anel externo (cyan pulsante)
        g2d.setColor(new Color(0, 255, 200, (int)(200 * pulseAlpha)));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval(-(int)radius, -(int)radius, (int)(radius * 2), (int)(radius * 2));

        // Camada 3: Segunda borda (roxo)
        g2d.setColor(new Color(150, 50, 255, (int)(150 * pulseAlpha)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(-(int)(radius * 0.85f), -(int)(radius * 0.85f), (int)(radius * 1.7f), (int)(radius * 1.7f));
        
        // Padrão radial interno
        g2d.setColor(new Color(100, 200, 255, (int)(100 * pulseAlpha)));
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i;
            int x1 = (int)(Math.cos(angle) * radius * 0.3f);
            int y1 = (int)(Math.sin(angle) * radius * 0.3f);
            int x2 = (int)(Math.cos(angle) * radius * 0.9f);
            int y2 = (int)(Math.sin(angle) * radius * 0.9f);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Restaurar transform
        g2d.setTransform(oldTransform);
        
        // Desenhar glow ao redor
        for (int i = 3; i >= 1; i--) {
            g2d.setColor(new Color(0, 255, 200, (int)(30 * pulseAlpha / i)));
            g2d.setStroke(new BasicStroke(i * 2));
            g2d.drawOval((int)(x + radius - radius - i * 5), (int)(y + radius - radius - i * 5), 
                        (int)(radius * 2 + i * 10), (int)(radius * 2 + i * 10));
        }
        
        // Desenhar texto "NEXT PHASE"
        g2d.setColor(new Color(0, 255, 200, (int)(255 * pulseAlpha)));
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String text = "PHASE " + nextPhase;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)(x + radius - fm.stringWidth(text) / 2);
        int textY = (int)(y + radius + fm.getHeight() / 2);
        
        // Sombra do texto
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(text, textX + 2, textY + 2);
        
        g2d.setColor(new Color(0, 255, 200, (int)(255 * pulseAlpha)));
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

    // Classe interna para partículas do portal
    private static class PortalParticle {
        float x, y;
        float vx, vy;
        float life = 1.0f;
        float size;
        Color color;

        PortalParticle(float x, float y) {
            this.x = x;
            this.y = y;
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * 2 + 1);
            this.vx = (float)Math.cos(angle) * speed;
            this.vy = (float)Math.sin(angle) * speed;
            this.size = (float)(Math.random() * 4 + 2);
            
            if (Math.random() < 0.5) {
                this.color = new Color(0, 255, 200);
            } else {
                this.color = new Color(100, 0, 200);
            }
        }

        void tick(double delta) {
            x += vx;
            y += vy;
            vx *= 0.98f;
            vy *= 0.98f;
            life -= 0.02f;
        }

        void render(Graphics2D g2d) {
            if (life > 0) {
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * life * 0.7f)));
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
            }
        }
    }
}
