package com.thurdas.cyberlegacy.entities;

import java.awt.*;

public class Particle {
    public float x, y, dx, dy;
    public int life = 20;
    public Color color;

    public Particle(float x, float y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.dx = (float) ((Math.random() - 0.5) * 6);
        this.dy = (float) ((Math.random() - 0.5) * 6);
    }

    public void tick(double delta) {
        x += dx;
        y += dy;
        life--;
    }

    public void render(Graphics2D g2d) {
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, life * 10)));
        g2d.fillRect((int) x, (int) y, 4, 4);
    }
}
