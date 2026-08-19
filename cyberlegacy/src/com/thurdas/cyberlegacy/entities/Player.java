package com.thurdas.cyberlegacy.entities;

import com.thurdas.cyberlegacy.CyberLegacy;
import com.thurdas.cyberlegacy.ui.ClassSelector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class Player {
    private static BufferedImage vehicleProjectileSprite;
    private static boolean vehicleProjectileLoaded = false;

    private BufferedImage spriteSheet;
    private int animationFrame = 0;

    private double animationTimer = 0;
    private double animationSpeed = 0.20;

    private int spriteWidth = 32;
    private int spriteHeight = 32;
    private boolean isMoving = false;

    public float x, y;
    public int maxHealth, health;
    public float maxStamina = 100, stamina = 100;
    public int level = 1, xp = 0, maxXp = 50, kills = 0;
    public float speed;
    public int damage;
    public long attackCooldown, lastAttack;

    public boolean isDashing = false;
    public long dashEndTime;

    public ClassSelector.PlayerClass pClass;
    public CyberLegacy.Direction facing = CyberLegacy.Direction.DOWN;
    public boolean isAttacking = false;

    public Player(float x, float y, ClassSelector.PlayerClass pClass) {
        this.x = x;
        this.y = y;
        this.pClass = pClass;

        try {
            String spritePath = "";
            switch (pClass) {
                case KATANA:  spritePath = "cyberlegacy/assets/img/sworder.png"; break;
                case SHOOTER: spritePath = "cyberlegacy/assets/img/jogador.png"; break;
                case HACKER:  spritePath = "cyberlegacy/assets/img/tux.png"; break;
            }

            File spriteFile = new File(spritePath);
            if (spriteFile.exists()) {
                spriteSheet = ImageIO.read(spriteFile);

                if (pClass == ClassSelector.PlayerClass.HACKER) {
                    spriteWidth = spriteSheet.getWidth() / 8;
                    spriteHeight = spriteSheet.getHeight() / 11;
                } else if (pClass == ClassSelector.PlayerClass.KATANA) {
                    spriteWidth = 48;
                    spriteHeight = spriteSheet.getHeight();
                } else {
                    spriteWidth = spriteSheet.getWidth() / 20;
                    spriteHeight = spriteSheet.getHeight();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pClass == ClassSelector.PlayerClass.SHOOTER) {
            loadVehicleProjectileSprite();
        }

        switch (pClass) {
            case KATANA:  maxHealth = 360; health = 200; speed = 220; damage = 80; attackCooldown = 450; break;
            case SHOOTER: maxHealth = 300;  health = 150;  speed = 250; damage = 40; attackCooldown = 150; break;
            case HACKER:  maxHealth = 230;  health = 100;  speed = 300; damage = 90; attackCooldown = 600; break;
        }
    }

    private static void loadVehicleProjectileSprite() {
        if (vehicleProjectileLoaded) return;
        vehicleProjectileLoaded = true;

        try {
            String[] filePaths = {
                    "cyberlegacy/assets/img/vehicle-projectile.png",
                    "assets/img/vehicle-projectile.png",
                    "../cyberlegacy/assets/img/vehicle-projectile.png"
            };
            for (String filePath : filePaths) {
                File projectileFile = new File(filePath);
                if (projectileFile.exists()) {
                    vehicleProjectileSprite = ImageIO.read(projectileFile);
                    break;
                }
            }

            if (vehicleProjectileSprite == null) {
                String[] resourcePaths = {
                        "cyberlegacy/assets/img/vehicle-projectile.png",
                        "assets/img/vehicle-projectile.png"
                };
                for (String resourcePath : resourcePaths) {
                    try (InputStream resource = Player.class.getClassLoader()
                            .getResourceAsStream(resourcePath)) {
                        if (resource != null) {
                            vehicleProjectileSprite = ImageIO.read(resource);
                            break;
                        }
                    }
                }
            }

            if (vehicleProjectileSprite == null) {
                System.err.println("Sprite vehicle-projectile.png não encontrado.");
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar sprite do tiro do shooter: " + e.getMessage());
        }
    }

    public void addXp(int amount) {
        xp += amount;
        if (xp >= maxXp) levelUp();
    }

    private void levelUp() {
        level++;
        xp -= maxXp;
        maxXp = (int) (maxXp * 1.5);
        maxHealth += 20;
        health = maxHealth;
        damage += 10;
    }

    public void tick(CyberLegacy game, double delta) {
        handleMovement(game, delta);
        handleCombat(game);
    }

    public BufferedImage getPortrait() {
        if (spriteSheet != null) {
            try {
                if (pClass == ClassSelector.PlayerClass.HACKER) {
                    return spriteSheet.getSubimage(0, 4 * spriteHeight, spriteWidth, spriteHeight);
                } else {
                    return spriteSheet.getSubimage(0, 0, spriteWidth, spriteHeight);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void handleMovement(CyberLegacy game, double delta) {
        float xMove = 0, yMove = 0;
        float currentSpeed = speed;

        CyberLegacy.Direction oldFacing = facing;

        if (!isDashing && stamina < maxStamina) {
            stamina += 7.5f * delta;
        }

        if (game.keys[KeyEvent.VK_SHIFT] && !isDashing && stamina >= 30) {
            isDashing = true;
            stamina -= 30;
            dashEndTime = System.currentTimeMillis() + 150;
        }

        if (isDashing) {
            currentSpeed *= 3.5f;
            Color dashColor = (pClass == ClassSelector.PlayerClass.KATANA) ? new Color(0, 255, 255) :
                    (pClass == ClassSelector.PlayerClass.SHOOTER) ? new Color(255, 255, 0) : new Color(150, 0, 255);
            game.particles.add(new Particle(x + 16, y + 16, dashColor));

            if (System.currentTimeMillis() > dashEndTime) isDashing = false;
        }

        if (game.keys[KeyEvent.VK_W] || game.keys[KeyEvent.VK_UP]) { yMove -= currentSpeed * delta; facing = CyberLegacy.Direction.UP; }
        if (game.keys[KeyEvent.VK_S] || game.keys[KeyEvent.VK_DOWN]) { yMove += currentSpeed * delta; facing = CyberLegacy.Direction.DOWN; }
        if (game.keys[KeyEvent.VK_A] || game.keys[KeyEvent.VK_LEFT]) { xMove -= currentSpeed * delta; facing = CyberLegacy.Direction.LEFT; }
        if (game.keys[KeyEvent.VK_D] || game.keys[KeyEvent.VK_RIGHT]) { xMove += currentSpeed * delta; facing = CyberLegacy.Direction.RIGHT; }

        if (xMove != 0 && yMove != 0) {
            xMove *= 0.7071f;
            yMove *= 0.7071f;
        }

        isMoving = (xMove != 0 || yMove != 0);

        if (isMoving) {
            if (oldFacing != facing) {
                animationFrame = 1;
                animationTimer = 0;
            } else {
                animationTimer += delta;
                if (animationTimer >= animationSpeed) {
                    animationTimer = 0;
                    animationFrame++;
                    if (animationFrame > 3) animationFrame = 0;
                }
            }
        } else {
            animationFrame = 0;
            animationTimer = 0;
        }

        int hitboxWidth = 16;
        int hitboxHeight = 16;
        float offsetX = 8;
        float offsetY = 16;

        if (!game.isSolid(x + xMove + offsetX, y + offsetY, hitboxWidth, hitboxHeight)) x += xMove;
        if (!game.isSolid(x + offsetX, y + yMove + offsetY, hitboxWidth, hitboxHeight)) y += yMove;
    }

    private void handleCombat(CyberLegacy game) {
        boolean attackInput = game.keys[KeyEvent.VK_SPACE] || game.mouseAttack;
        if (attackInput && System.currentTimeMillis() - lastAttack > attackCooldown) {
            lastAttack = System.currentTimeMillis();
            isAttacking = true;

            switch (pClass) {
                case SHOOTER:
                    game.projectiles.add(new Projectile(
                            x, y, facing, true, damage, 600.0f,
                            new Color(255, 255, 0), 6, 32, 32,
                            vehicleProjectileSprite));
                    game.audioManager.playSound("cyberlegacy/assets/sfx/shoot.wav");
                    break;
                case HACKER:
                    game.projectiles.add(new Projectile(x, y, facing, true, damage, 300.0f, new Color(150, 0, 255), 20));
                    game.audioManager.playSound("cyberlegacy/assets/sfx/fireball.wav");
                    break;
                case KATANA:
                    game.audioManager.playSound("cyberlegacy/assets/sfx/daviddumaisaudio-sword-slash-and-swing-185432.wav");
                    break;
            }
        }

        if (isAttacking && System.currentTimeMillis() - lastAttack > 150) {
            isAttacking = false;
        }
    }

    public Rectangle getMeleeHitbox() {
        if (!isAttacking || pClass != ClassSelector.PlayerClass.KATANA) return null;
        int reach = 55;
        switch (facing) {
            case UP:    return new Rectangle((int) x - 15, (int) y - reach, 62, reach);
            case DOWN:  return new Rectangle((int) x - 15, (int) y + 32, 62, reach);
            case LEFT:  return new Rectangle((int) x - reach, (int) y - 15, reach, 62);
            case RIGHT: return new Rectangle((int) x + 32, (int) y - 15, reach, 62);
            default:    return null;
        }
    }

    public void renderShadow(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval((int) x + 4, (int) y + 24, 24, 12);
    }

    public void render(Graphics2D g2d) {
        int cx = (int) x;
        int cy = (int) y;

        if (spriteSheet != null) {
            int xClip = 0;
            int yClip = 0;
            boolean flip = false;

            if (pClass == ClassSelector.PlayerClass.HACKER) {
                int baseRow = 0;
                switch (facing) {
                    case DOWN:  baseRow = 4; break;
                    case LEFT:  baseRow = 1; flip = true; break;
                    case UP:    baseRow = 6; break;
                    case RIGHT: baseRow = 1; break;
                }

                if (baseRow * spriteHeight >= spriteSheet.getHeight()) baseRow = 0;

                xClip = (animationFrame % 4) * spriteWidth;
                if (xClip >= spriteSheet.getWidth()) xClip = 0;

                yClip = baseRow * spriteHeight;

            } else if (pClass == ClassSelector.PlayerClass.KATANA) {

                int frameIndex = 0;
                int animStep = animationFrame % 4;

                if (isAttacking) {

                    long elapsed = System.currentTimeMillis() - lastAttack;
                    int attackStep = (int)((elapsed / 150.0f) * 7);

                    if (attackStep > 6) attackStep = 6;

                    switch (facing) {
                        case RIGHT:
                            frameIndex = 3 + attackStep;
                            flip = true;
                            break;
                        case LEFT:
                            frameIndex = 3 + attackStep;
                            break;
                        case UP:
                            frameIndex = 13;
                            break;
                        case DOWN:
                            frameIndex = 10;
                            break;
                    }

                } else {

                    int[] sideFrames = {15,16,15,16};

                    switch (facing) {
                        case DOWN:
                            int[] downFrames = {0,1,2,1};
                            frameIndex = downFrames[animStep];
                            break;
                        case UP:
                            int[] upFrames = {11,12,13,12};
                            frameIndex = upFrames[animStep];
                            break;
                        case RIGHT:
                            frameIndex = sideFrames[animStep];
                            flip = true;
                            break;
                        case LEFT:
                            frameIndex = sideFrames[animStep];
                            break;
                    }
                }

                xClip = frameIndex * spriteWidth;
                yClip = 0;
            }  else {
                int baseFrame = 0;
                switch (facing) {
                    case DOWN:  baseFrame = 0; break;
                    case LEFT:  baseFrame = 4; break;
                    case UP:    baseFrame = 12; break;
                    case RIGHT: baseFrame = 4; flip = true; break;
                }

                xClip = (baseFrame + (animationFrame % 4)) * spriteWidth;
                if (xClip + spriteWidth > spriteSheet.getWidth()) xClip = 0;
                yClip = 0;
            }

            try {
                BufferedImage frameAtual = spriteSheet.getSubimage(xClip, yClip, spriteWidth, spriteHeight);

                int drawX = cx + (16 - spriteWidth / 2);
                int drawY = cy + (32 - spriteHeight);

                if (flip) {
                    g2d.drawImage(frameAtual, drawX + spriteWidth, drawY, drawX, drawY + spriteHeight, 0, 0, spriteWidth, spriteHeight, null);
                } else {
                    g2d.drawImage(frameAtual, drawX, drawY, spriteWidth, spriteHeight, null);
                }
            } catch (Exception e) {
                drawFallbackShape(g2d, cx, cy);
            }
        } else {
            drawFallbackShape(g2d, cx, cy);
        }

        if (pClass == ClassSelector.PlayerClass.KATANA && isAttacking) {
            long elapsed = System.currentTimeMillis() - lastAttack;
            float progress = Math.min(1.0f, elapsed / 150.0f);
            int radius = 45;

            g2d.setColor(new Color(0, 255, 255, (int) (255 * (1.0f - progress))));
            g2d.setStroke(new BasicStroke(15f * (1.0f - progress), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int baseAngle = 0;
            switch (facing) {
                case RIGHT: baseAngle = 300; break;
                case UP:    baseAngle = 30; break;
                case LEFT:  baseAngle = 120; break;
                case DOWN:  baseAngle = 210; break;
            }

            int sweep = (int) (140 * progress);
            g2d.drawArc(cx + 16 - radius, cy + 16 - radius, radius * 2, radius * 2, baseAngle + 140 - sweep, sweep);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    private void drawFallbackShape(Graphics2D g2d, int cx, int cy) {
        Color clothColor = new Color(0, 255, 255);
        if (pClass == ClassSelector.PlayerClass.SHOOTER) clothColor = new Color(255, 255, 0);
        else if (pClass == ClassSelector.PlayerClass.HACKER) clothColor = new Color(150, 0, 255);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(cx + 8, cy + 2, 16, 16);

        g2d.setColor(new Color(20, 20, 20));
        g2d.fillRoundRect(cx + 6, cy + 18, 20, 14, 4, 4);

        g2d.setColor(clothColor);
        g2d.fillRect(cx + 10, cy + 20, 12, 4);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(cx + 2, cy + 22, 6, 6);
        g2d.fillOval(cx + 24, cy + 22, 6, 6);

        if (pClass == ClassSelector.PlayerClass.KATANA && !isAttacking) {
            g2d.setColor(new Color(0, 255, 255));
            switch (facing) {
                case RIGHT: g2d.fillRect(cx + 28, cy + 24, 16, 3); break;
                case LEFT:  g2d.fillRect(cx - 12, cy + 24, 16, 3); break;
                case UP:    g2d.fillRect(cx + 24, cy + 2, 3, 16); break;
                case DOWN:  g2d.fillRect(cx + 24, cy + 28, 3, 16); break;
            }
        }
    }
}
