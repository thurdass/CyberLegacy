package com.thurdas.cyberlegacy.entities;

import com.thurdas.cyberlegacy.CyberLegacy;

import java.awt.*;

public class Projectile {
    public float x, y, speed;
    public com.thurdas.cyberlegacy.CyberLegacy.Direction dir;
    public boolean isPlayer;
    public int damage, size;
    public Color color;
    public boolean isDestroyed = false;

    public Projectile(float x, float y, com.thurdas.cyberlegacy.CyberLegacy.Direction dir, boolean p, int d, float s, Color c, int sz) {
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
        for (com.thurdas.cyberlegacy.entities.Enemy e : game.enemies) {
            if (new Rectangle((int) e.x, (int) e.y, 32, 32).intersects(rProj) && !e.isShocked) {
                e.takeDamage(damage, game);
                isDestroyed = true;
                return;
            }
        }

        game.particles.add(new com.thurdas.cyberlegacy.entities.Particle(x + size / 2, y + size / 2, color));
    }

    public void render(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int) x, (int) y, size, size);
    }
}
