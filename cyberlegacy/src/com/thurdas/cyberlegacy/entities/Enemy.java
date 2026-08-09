package com.thurdas.cyberlegacy.entities;

import com.thurdas.cyberlegacy.CyberLegacy;
import com.thurdas.cyberlegacy.entities.Player;
import com.thurdas.cyberlegacy.ui.FloatingText;
import com.thurdas.cyberlegacy.ui.ClassSelector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Enemy {
    public float x, y;
    public int health, maxHealth, damage, xpReward;
    public float speed;
    public long lastHitTime;
    public boolean isShocked = false;

    public Color color = new Color(200, 0, 30);

    private static BufferedImage[] spriteSheets = new BufferedImage[4];
    private static int[] spriteWidths = new int[4];
    private static int[] spriteHeights = new int[4];
    private static boolean loaded = false;

    private int zombieType;

    private int animationFrame = 0;
    private double animationTimer = 0;
    private double animationSpeed = 0.15;
    private CyberLegacy.Direction facing = CyberLegacy.Direction.DOWN;

    public Enemy(float x, float y, int wave) {
        this.x = x;
        this.y = y;
        this.maxHealth = 40 + (wave * 15);
        this.health = maxHealth;
        this.speed = 90.0f + wave;
        this.damage = 15 + (wave * 2);
        this.xpReward = 15 + wave;

        if (!loaded) {
            loadSprites();
        }

        this.zombieType = (int) (Math.random() * 4);
    }

    public Rectangle getCollisionBounds() {
        return new Rectangle((int) x, (int) y, 32, 32);
    }

    public boolean isReadyToRemove() {
        return health <= 0;
    }

    private static void loadSprites() {
        loaded = true;
        String[] files = {
                "cyberlegacy/assets/img/zumbi2.png",
                "cyberlegacy/assets/img/zumbi2.png",
                "cyberlegacy/assets/img/zumbi3.png",
                "cyberlegacy/assets/img/zumbi4.png"
        };

        for (int i = 0; i < 4; i++) {
            try {
                File file = new File(files[i]);
                if (file.exists()) {
                    spriteSheets[i] = ImageIO.read(file);
                    spriteWidths[i] = spriteSheets[i].getWidth() / 16;
                    spriteHeights[i] = spriteSheets[i].getHeight();
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar sprite do inimigo " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    public void tick(CyberLegacy game, Player p, double delta) {
        if (health <= 0) return;

        if (isShocked) {
            if (System.currentTimeMillis() - lastHitTime > 150) isShocked = false;
            else return;
        }

        float xMove = 0, yMove = 0;
        if (x < p.x) xMove += speed * delta;
        if (x > p.x) xMove -= speed * delta;
        if (y < p.y) yMove += speed * delta;
        if (y > p.y) yMove -= speed * delta;

        boolean isMoving = false;

        if (Math.abs(xMove) > Math.abs(yMove)) {
            facing = (xMove > 0) ? CyberLegacy.Direction.RIGHT : CyberLegacy.Direction.LEFT;
            isMoving = true;
        } else if (yMove != 0) {
            facing = (yMove > 0) ? CyberLegacy.Direction.DOWN : CyberLegacy.Direction.UP;
            isMoving = true;
        }

        if (isMoving && !isShocked) {
            animationTimer += delta;
            if (animationTimer >= animationSpeed) {
                animationTimer = 0;
                animationFrame++;
                if (animationFrame > 3) animationFrame = 0;
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }

        if (!game.isSolid(x + xMove, y, 32, 32)) x += xMove;
        if (!game.isSolid(x, y + yMove, 32, 32)) y += yMove;

        Rectangle rEnemy = getCollisionBounds();
        Rectangle rPlayer = new Rectangle((int) p.x, (int) p.y, 32, 32);

        if (rEnemy.intersects(rPlayer) && System.currentTimeMillis() - lastHitTime > 1000 && !p.isDashing) {
            p.health -= damage;
            lastHitTime = System.currentTimeMillis();
            game.triggerShake(10);
        }

        if (p.isAttacking && p.pClass == ClassSelector.PlayerClass.KATANA) {
            java.awt.Rectangle hitBlade = p.getMeleeHitbox();
            if (hitBlade != null && hitBlade.intersects(rEnemy) && !isShocked) {
                takeDamage(p.damage, game);
            }
        }
    }

    public void takeDamage(int dmg, CyberLegacy game) {
        if (health <= 0) return;

        boolean isCrit = Math.random() < 0.2;
        int finalDmg = isCrit ? (int) (dmg * 1.5) : dmg;

        this.health -= finalDmg;
        this.isShocked = true;
        this.lastHitTime = System.currentTimeMillis();

        game.floatingTexts.add(new FloatingText("" + finalDmg, x, y - 10, isCrit ? new Color(255, 255, 0) : Color.WHITE));

        if (isCrit) game.triggerShake(8);
        for (int i = 0; i < 8; i++) {
            game.particles.add(new Particle(x + 16, y + 16, color));
        }
    }

    public void renderShadow(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval((int) x + 4, (int) y + 24, 24, 12);
    }

    public void render(Graphics2D g2d) {
        int cx = (int) x;
        int cy = (int) y;

        BufferedImage currentSheet = spriteSheets[zombieType];
        int sWidth = spriteWidths[zombieType];
        int sHeight = spriteHeights[zombieType];

        if (currentSheet != null && sWidth > 0) {

            int baseFrame = 0;
            switch (facing) {
                case DOWN:  baseFrame = 0; break;
                case LEFT:  baseFrame = 4; break;
                case RIGHT: baseFrame = 8; break;
                case UP:    baseFrame = 12; break;
            }

            int currentFrame = baseFrame + animationFrame;
            int xClip = currentFrame * sWidth;
            int yClip = 0;

            BufferedImage frame = currentSheet.getSubimage(xClip, yClip, sWidth, sHeight);

            int drawX = cx + (16 - sWidth / 2);
            int drawY = cy + (32 - sHeight);

            if (isShocked) {
                g2d.drawImage(frame, drawX, drawY, null);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect(drawX, drawY, sWidth, sHeight);
            } else {
                g2d.drawImage(frame, drawX, drawY, null);
            }

        } else {
            if (isShocked) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(cx, cy, 32, 32);
            } else {
                g2d.setColor(new Color(40, 40, 40));
                g2d.fillOval(cx + 8, cy + 2, 16, 16);
                g2d.setColor(new Color(255, 0, 50));
                g2d.fillRect(cx + 12, cy + 6, 3, 3);
                g2d.fillRect(cx + 18, cy + 6, 3, 3);
                g2d.setColor(new Color(30, 30, 30));
                g2d.fillRoundRect(cx + 6, cy + 18, 20, 14, 4, 4);
                g2d.setColor(new Color(40, 40, 40));
                g2d.fillOval(cx, cy + 10, 8, 8);
                g2d.fillOval(cx + 24, cy + 10, 8, 8);
            }
        }

        if (health < maxHealth && health > 0) {
            g2d.setColor(new Color(20, 0, 0));
            g2d.fillRect(cx - 4, cy - 8, 40, 5);
            g2d.setColor(new Color(255, 0, 50));
            g2d.fillRect(cx - 4, cy - 8, (int) ((health / (float) maxHealth) * 40), 5);
        }
    }
}
