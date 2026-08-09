package com.thurdas.cyberlegacy.entities;

import com.thurdas.cyberlegacy.CyberLegacy;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Projectile {
    public float x, y, speed;
    public com.thurdas.cyberlegacy.CyberLegacy.Direction dir;
    public boolean isPlayer;
    public int damage, size;
    public Color color;
    private BufferedImage sprite;
    public boolean isDestroyed = false;

    public Projectile(float x, float y, com.thurdas.cyberlegacy.CyberLegacy.Direction dir, boolean p, int d, float s, Color c, int sz) {
        this(x, y, dir, p, d, s, c, sz, 32, 32, null);
    }

    public Projectile(float x, float y, com.thurdas.cyberlegacy.CyberLegacy.Direction dir,
                      boolean p, int d, float s, Color c, int sz,
                      int originWidth, int originHeight) {
        this(x, y, dir, p, d, s, c, sz, originWidth, originHeight, null);
    }

    public Projectile(float x, float y, com.thurdas.cyberlegacy.CyberLegacy.Direction dir,
                      boolean p, int d, float s, Color c, int sz,
                      int originWidth, int originHeight, BufferedImage sprite) {
        this.x = x + (originWidth - sz) / 2.0f;
        this.y = y + (originHeight - sz) / 2.0f;
        this.dir = dir;
        this.isPlayer = p;
        this.damage = d;
        this.speed = s;
        this.color = c;
        this.size = sz;
        this.sprite = sprite;
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

        if (isPlayer) {
            for (com.thurdas.cyberlegacy.entities.Enemy e : game.enemies) {
                if (e.health > 0 && e.getCollisionBounds().intersects(rProj) && !e.isShocked) {
                    e.takeDamage(damage, game);
                    isDestroyed = true;
                    return;
                }
            }
        } else if (game.player != null
                && !game.player.isDashing
                && new Rectangle((int) game.player.x, (int) game.player.y, 32, 32).intersects(rProj)) {
            game.player.health -= damage;
            game.triggerShake(4);
            isDestroyed = true;
            return;
        }

        game.particles.add(new com.thurdas.cyberlegacy.entities.Particle(x + size / 2, y + size / 2, color));
    }

    public void render(Graphics2D g2d) {
        if (sprite != null) {
            renderSprite(g2d);
            return;
        }

        if (!isPlayer) {
            renderTankShell(g2d);
            return;
        }

        g2d.setColor(color);
        g2d.fillOval((int) x, (int) y, size, size);
    }

    private void renderSprite(Graphics2D g2d) {
        Graphics2D spriteGraphics = (Graphics2D) g2d.create();
        spriteGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        spriteGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;
        float drawWidth = 34.0f;
        float drawHeight = drawWidth * sprite.getHeight() / sprite.getWidth();
        double angle = 0.0;

        switch (dir) {
            case LEFT:  angle = Math.PI; break;
            case UP:    angle = -Math.PI / 2.0; break;
            case DOWN:  angle = Math.PI / 2.0; break;
            case RIGHT: break;
        }

        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(angle);
        transform.scale(drawWidth / sprite.getWidth(), drawHeight / sprite.getHeight());
        transform.translate(-sprite.getWidth() / 2.0, -sprite.getHeight() / 2.0);
        spriteGraphics.drawImage(sprite, transform, null);
        spriteGraphics.dispose();
    }

    /** Desenha o tiro do tanque como um projétil direcional, em vez de um círculo. */
    private void renderTankShell(Graphics2D g2d) {
        Graphics2D projectileGraphics = (Graphics2D) g2d.create();
        projectileGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;
        float directionX = 0;
        float directionY = 0;

        switch (dir) {
            case LEFT:  directionX = -1; break;
            case RIGHT: directionX = 1; break;
            case UP:    directionY = -1; break;
            case DOWN:  directionY = 1; break;
        }

        // Aura e rastro dão sensação de velocidade sem alterar a hitbox.
        projectileGraphics.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        projectileGraphics.setColor(new Color(255, 75, 15, 45));
        projectileGraphics.drawLine(
                Math.round(centerX - directionX * 10), Math.round(centerY - directionY * 10),
                Math.round(centerX - directionX * 2), Math.round(centerY - directionY * 2));

        projectileGraphics.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        projectileGraphics.setColor(new Color(135, 38, 12, 230));
        projectileGraphics.drawLine(
                Math.round(centerX - directionX * 7), Math.round(centerY - directionY * 7),
                Math.round(centerX + directionX * 7), Math.round(centerY + directionY * 7));

        projectileGraphics.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        projectileGraphics.setColor(new Color(255, 105, 25));
        projectileGraphics.drawLine(
                Math.round(centerX - directionX * 6), Math.round(centerY - directionY * 6),
                Math.round(centerX + directionX * 6), Math.round(centerY + directionY * 6));

        projectileGraphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        projectileGraphics.setColor(new Color(255, 235, 145));
        projectileGraphics.drawLine(
                Math.round(centerX - directionX * 3), Math.round(centerY - directionY * 3),
                Math.round(centerX + directionX * 5), Math.round(centerY + directionY * 5));

        // Ponta branca reforça a direção do disparo.
        projectileGraphics.setColor(Color.WHITE);
        projectileGraphics.fillOval(
                Math.round(centerX + directionX * 4 - 1.5f),
                Math.round(centerY + directionY * 4 - 1.5f), 3, 3);

        projectileGraphics.dispose();
    }
}
