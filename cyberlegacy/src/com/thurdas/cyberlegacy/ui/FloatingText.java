package com.thurdas.cyberlegacy.ui;

import java.awt.*;

public class FloatingText {
    public float x, y;
    public String text;
    public Color color;
    public int life = 60;

    public FloatingText(String text, float x, float y, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void tick(double delta) {
        y -= 1.5f * delta;
        life--;
    }

    public void render(Graphics2D g) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, life * 4)));
        g.setFont(new Font("Impact", Font.PLAIN, 20));
        g.drawString(text, (int) x, (int) y);
    }
}
