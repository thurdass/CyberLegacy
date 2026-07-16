package com.thurdas.cyberlegacy.entities;

import com.thurdas.cyberlegacy.CyberLegacy;
import com.thurdas.cyberlegacy.ui.FloatingText;
import com.thurdas.cyberlegacy.ui.ClassSelector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Boss extends Enemy {
    private static BufferedImage bossSpriteSheet;
    private static int spriteWidth = -1;
    private static int spriteHeight = -1;
    private static boolean loadedBoss = false;

    private int animationFrame = 0;
    private double animationTimer = 0;
    private double animationSpeed = 0.15;
    private CyberLegacy.Direction facing = CyberLegacy.Direction.DOWN;

    private final int bossSize = 64;

    private boolean isDashing = false;
    private long dashEndTime = 0;
    private long nextDashTime = 0;
    private float dashDx = 0;
    private float dashDy = 0;

    public Boss(float x, float y, int wave) {
        super(x, y, wave);

        this.maxHealth = 1000 + (wave * 60);
        this.health = maxHealth;
        this.damage = 30 + (wave * 5);
        this.speed = 65.0f + (wave * 0.5f);
        this.xpReward = 100 + (wave * 15);
        this.color = new Color(138, 43, 226);

        this.nextDashTime = System.currentTimeMillis() + 2000;

        if (!loadedBoss) {
            loadBossSprite();
        }
    }

    private static void loadBossSprite() {
        loadedBoss = true;
        try {
            File file = new File("cyberlegacy/assets/img/zumbi.png");
            if (file.exists()) {
                bossSpriteSheet = ImageIO.read(file);
                spriteWidth = bossSpriteSheet.getWidth() / 16;
                spriteHeight = bossSpriteSheet.getHeight();
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar sprite do boss: " + e.getMessage());
        }
    }

    @Override
    public void tick(CyberLegacy game, Player p, double delta) {
        if (isShocked) {
            if (System.currentTimeMillis() - lastHitTime > 150) isShocked = false;
            else return;
        }

        long currentTime = System.currentTimeMillis();
        float xMove = 0, yMove = 0;

        if (isDashing) {
            if (currentTime > dashEndTime) {
                isDashing = false;
                nextDashTime = currentTime + 1000 + (long)(Math.random() * 1000);
            } else {
                float currentSpeed = speed * 6.0f;
                xMove = dashDx * currentSpeed * (float) delta;
                yMove = dashDy * currentSpeed * (float) delta;

                game.particles.add(new Particle(x + bossSize / 2, y + bossSize / 2, color));
            }
        } else {
            if (x < p.x) xMove += speed * delta;
            if (x > p.x) xMove -= speed * delta;
            if (y < p.y) yMove += speed * delta;
            if (y > p.y) yMove -= speed * delta;

            float distToPlayer = (float) Math.hypot(p.x - x, p.y - y);

            if (currentTime > nextDashTime && distToPlayer < 400) {
                isDashing = true;
                dashEndTime = currentTime + 400;

                if (distToPlayer > 0) {
                    dashDx = (p.x - x) / distToPlayer;
                    dashDy = (p.y - y) / distToPlayer;
                }

                game.triggerShake(5);
            }
        }

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

            double currentAnimSpeed = isDashing ? animationSpeed / 2.0 : animationSpeed;

            if (animationTimer >= currentAnimSpeed) {
                animationTimer = 0;
                animationFrame++;
                if (animationFrame > 3) animationFrame = 0;
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }

        if (!game.isSolid(x + xMove, y, bossSize, bossSize)) x += xMove;
        if (!game.isSolid(x, y + yMove, bossSize, bossSize)) y += yMove;

        Rectangle rBoss = new Rectangle((int) x, (int) y, bossSize, bossSize);
        Rectangle rPlayer = new Rectangle((int) p.x, (int) p.y, 32, 32);

        if (rBoss.intersects(rPlayer) && currentTime - lastHitTime > 1000 && !p.isDashing) {
            p.health -= damage;
            lastHitTime = currentTime;
            game.triggerShake(18);
        }

        if (p.isAttacking && p.pClass == ClassSelector.PlayerClass.KATANA) {
            java.awt.Rectangle hitBlade = p.getMeleeHitbox();
            if (hitBlade != null && hitBlade.intersects(rBoss) && !isShocked) {
                takeDamage(p.damage, game);
            }
        }
    }

    @Override
    public void takeDamage(int dmg, CyberLegacy game) {
        boolean isCrit = Math.random() < 0.2;
        int finalDmg = isCrit ? (int) (dmg * 1.5) : dmg;

        this.health -= finalDmg;
        this.isShocked = true;
        this.lastHitTime = System.currentTimeMillis();

        game.floatingTexts.add(new FloatingText("" + finalDmg, x + 16, y - 10, isCrit ? new Color(255, 255, 0) : Color.WHITE));

        if (isCrit) game.triggerShake(10);
        for (int i = 0; i < 15; i++) {
            game.particles.add(new Particle(x + bossSize / 2, y + bossSize / 2, color));
        }
    }

    @Override
    public void renderShadow(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval((int) x + 8, (int) y + bossSize - 16, bossSize - 16, 20);
    }

    @Override
    public void render(Graphics2D g2d) {
        int cx = (int) x;
        int cy = (int) y;

        if (bossSpriteSheet != null && spriteWidth > 0) {
            int baseFrame = 0;
            switch (facing) {
                case DOWN:  baseFrame = 0; break;
                case LEFT:  baseFrame = 4; break;
                case RIGHT: baseFrame = 8; break;
                case UP:    baseFrame = 12; break;
            }

            int currentFrame = baseFrame + animationFrame;
            int xClip = currentFrame * spriteWidth;
            int yClip = 0;

            BufferedImage frame = bossSpriteSheet.getSubimage(xClip, yClip, spriteWidth, spriteHeight);

            if (isShocked) {
                g2d.drawImage(frame, cx, cy, bossSize, bossSize, null);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect(cx, cy, bossSize, bossSize);
            } else {
                g2d.drawImage(frame, cx, cy, bossSize, bossSize, null);
            }
        } else {
            if (isShocked) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(cx, cy, bossSize, bossSize);
            } else {
                g2d.setColor(new Color(150, 0, 255));
                g2d.fillRect(cx, cy, bossSize, bossSize);
            }
        }

        g2d.setFont(new Font("Impact", Font.PLAIN, 18));
        String bossName = "SYSTEM OVERLORD [BOSS]";
        FontMetrics fm = g2d.getFontMetrics();
        int nameWidth = fm.stringWidth(bossName);
        int nx = cx + (bossSize - nameWidth) / 2;
        int ny = cy - 18;

        g2d.setColor(Color.BLACK);
        g2d.drawString(bossName, nx - 1, ny - 1);
        g2d.drawString(bossName, nx + 1, ny - 1);
        g2d.drawString(bossName, nx - 1, ny + 1);
        g2d.drawString(bossName, nx + 1, ny + 1);

        g2d.setColor(new Color(255, 50, 50));
        g2d.drawString(bossName, nx, ny);

        if (health < maxHealth && health > 0) {
            g2d.setColor(new Color(20, 0, 0));
            g2d.fillRect(cx, cy - 12, bossSize, 8);
            g2d.setColor(new Color(255, 0, 50));
            g2d.fillRect(cx, cy - 12, (int) ((health / (float) maxHealth) * bossSize), 8);
        }
    }
}
