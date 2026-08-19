package com.thurdas.cyberlegacy.entities;

import com.thurdas.cyberlegacy.CyberLegacy;
import com.thurdas.cyberlegacy.ui.ClassSelector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.InputStream;

public class VehicleEnemy extends Enemy {
    public static final int COLLISION_WIDTH = 56;
    public static final int COLLISION_HEIGHT = 36;

    private static final int CELL_WIDTH = 72;
    private static final int CELL_HEIGHT = 48;

    private static final int ROW_IDLE = 0;
    private static final int ROW_SHOOT = 1;
    private static final int ROW_DRIVE = 2;
    private static final int ROW_STOP_LEFT = 3;
    private static final int ROW_START_LEFT = 4;
    private static final int ROW_STOP_RIGHT = 5;
    private static final int ROW_START_RIGHT = 6;
    private static final int ROW_DAMAGE = 7;
    private static final int ROW_DESTROY = 8;

    private static final int IDLE_FRAMES = 2;
    private static final int SHOOT_FRAMES = 4;
    private static final int DRIVE_FRAMES = 10;
    private static final int STOP_LEFT_FRAMES = 4;
    private static final int STOP_RIGHT_FRAMES = 5;
    private static final int START_FRAMES = 4;
    private static final int DAMAGE_FRAMES = 2;
    private static final int DESTROY_FRAMES = 18;

    private static final double IDLE_FRAME_TIME = 0.30;
    private static final double DRIVE_FRAME_TIME = 0.085;
    private static final double TRANSITION_FRAME_TIME = 0.085;
    private static final double SHOOT_FRAME_TIME = 0.10;
    private static final double DAMAGE_FRAME_TIME = 0.08;
    private static final double DESTROY_FRAME_TIME = 0.085;

    private static final float DETECTION_RANGE = 360.0f;
    private static final float ATTACK_DISTANCE = 190.0f;
    private static final long SHOT_COOLDOWN = 900L;
    private static final long DAMAGE_ANIMATION_TIME = 180L;
    private static final int PROJECTILE_SIZE = 8;

    private static BufferedImage spriteSheet;
    private static BufferedImage projectileSprite;
    private static boolean spritesLoaded = false;

    private enum AnimationState { IDLE, START, DRIVE, STOP, SHOOT, DAMAGE, DESTROY }

    private AnimationState animationState = AnimationState.IDLE;
    private int animationFrame = 0;
    private double animationTimer = 0.0;
    private boolean wasMoving = false;
    private boolean destructionFinished = false;
    private boolean destructionEffectTriggered = false;

    private CyberLegacy.Direction facing = CyberLegacy.Direction.RIGHT;
    private long lastShotAt = 0L;
    private long attackAnimationEnd = 0L;
    private long damageAnimationEnd = 0L;
    private long lastContactDamageAt = 0L;

    public VehicleEnemy(float x, float y, int wave) {
        super(x, y, wave);

        this.maxHealth = 280 + (wave * 40);
        this.health = maxHealth;
        this.speed = 78.0f + (wave * 0.8f);
        this.damage = 22 + (wave * 3);
        this.xpReward = 50 + (wave * 5);
        this.color = new Color(25, 150, 125);

        if (!spritesLoaded) loadSprites();
    }

    private static void loadSprites() {
        spritesLoaded = true;
        try {
            File file = new File("cyberlegacy/assets/img/vehicle.png");
            if (file.exists()) spriteSheet = ImageIO.read(file);


            if (spriteSheet == null) {
                try (InputStream resource = VehicleEnemy.class.getClassLoader()
                        .getResourceAsStream("cyberlegacy/assets/img/vehicle.png")) {
                    if (resource != null) spriteSheet = ImageIO.read(resource);
                }
            }

            File projectileFile = new File("cyberlegacy/assets/img/vehicle-projectile.png");
            if (projectileFile.exists()) projectileSprite = ImageIO.read(projectileFile);

            if (projectileSprite == null) {
                try (InputStream resource = VehicleEnemy.class.getClassLoader()
                        .getResourceAsStream("cyberlegacy/assets/img/vehicle-projectile.png")) {
                    if (resource != null) projectileSprite = ImageIO.read(resource);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar sprite do veículo: " + e.getMessage());
        }
    }

    @Override
    public Rectangle getCollisionBounds() {
        return new Rectangle((int) x, (int) y, COLLISION_WIDTH, COLLISION_HEIGHT);
    }

    @Override
    public void tick(CyberLegacy game, Player p, double delta) {
        long now = System.currentTimeMillis();

        if (health <= 0) {
            updateDestructionAnimation(game, delta);
            return;
        }

        if (p == null) return;

        if (isShocked && now - lastHitTime > 150L) isShocked = false;

        if (now < damageAnimationEnd) {
            setAnimationState(AnimationState.DAMAGE);
            advanceAnimation(delta);
            return;
        }

        if (isShocked) return;

        if (animationState == AnimationState.SHOOT) {
            if (now < attackAnimationEnd) {
                advanceAnimation(delta);
                return;
            }
            setAnimationState(AnimationState.IDLE);
        }

        float vehicleCenterX = x + COLLISION_WIDTH / 2.0f;
        float vehicleCenterY = y + COLLISION_HEIGHT / 2.0f;
        float playerCenterX = p.x + 16.0f;
        float playerCenterY = p.y + 16.0f;
        float dx = playerCenterX - vehicleCenterX;
        float dy = playerCenterY - vehicleCenterY;
        float distance = (float) Math.hypot(dx, dy);

        if (Math.abs(dx) > 2.0f) {
            facing = dx < 0 ? CyberLegacy.Direction.LEFT : CyberLegacy.Direction.RIGHT;
        }

        boolean targetInRange = distance <= DETECTION_RANGE;
        boolean shouldMove = !targetInRange || Math.abs(dx) > ATTACK_DISTANCE;
        float movementScale = targetInRange ? 0.35f : 1.0f;
        float xMove = 0.0f;

        if (shouldMove && Math.abs(dx) > 2.0f) {
            xMove = Math.signum(dx) * speed * movementScale * (float) delta;
        }

        boolean canShoot = targetInRange
                && !shouldMove
                && now - lastShotAt >= SHOT_COOLDOWN;

        if (canShoot && (wasMoving
                || animationState == AnimationState.START
                || animationState == AnimationState.DRIVE)) {
            setAnimationState(AnimationState.STOP);
            wasMoving = false;
            advanceAnimation(delta);
            return;
        }

        if (canShoot && animationState == AnimationState.STOP) {
            advanceAnimation(delta);
            return;
        }

        if (canShoot) {
            fireAtPlayer(game, p, dx, dy, now);
            wasMoving = false;
            return;
        }

        boolean moved = false;
        if (xMove != 0.0f && !game.isSolid(x + xMove, y, COLLISION_WIDTH, COLLISION_HEIGHT)) {
            x += xMove;
            moved = true;
        }

        Rectangle vehicleBounds = getCollisionBounds();
        Rectangle playerBounds = new Rectangle((int) p.x, (int) p.y, 32, 32);

        if (vehicleBounds.intersects(playerBounds)
                && now - lastContactDamageAt > 1000L
                && !p.isDashing) {
            p.health -= damage;
            lastContactDamageAt = now;
            game.triggerShake(10);
        }

        if (p.isAttacking && p.pClass == ClassSelector.PlayerClass.KATANA && !isShocked) {
            Rectangle meleeHitbox = p.getMeleeHitbox();
            if (meleeHitbox != null && meleeHitbox.intersects(vehicleBounds)) {
                takeDamage(p.damage, game);
            }
        }

        if (health <= 0) {
            updateDestructionAnimation(game, delta);
            return;
        }

        if (moved) {
            if (!wasMoving) {
                setAnimationState(AnimationState.START);
            } else if (animationState != AnimationState.START && animationState != AnimationState.DRIVE) {
                setAnimationState(AnimationState.DRIVE);
            }
        } else {
            if (wasMoving) {
                setAnimationState(AnimationState.STOP);
            } else if (animationState != AnimationState.STOP && animationState != AnimationState.IDLE) {
                setAnimationState(AnimationState.IDLE);
            }
        }

        wasMoving = moved;
        advanceAnimation(delta);
    }

    private void fireAtPlayer(CyberLegacy game, Player p, float dx, float dy, long now) {
        CyberLegacy.Direction projectileDirection;
        if (Math.abs(dx) >= Math.abs(dy)) {
            projectileDirection = dx < 0 ? CyberLegacy.Direction.LEFT : CyberLegacy.Direction.RIGHT;
        } else {
            projectileDirection = dy < 0 ? CyberLegacy.Direction.UP : CyberLegacy.Direction.DOWN;
        }

        int projectileDamage = 18 + (damage / 2);
        game.projectiles.add(new Projectile(
                x, y, projectileDirection, false, projectileDamage, 280.0f,
                new Color(255, 120, 40), PROJECTILE_SIZE,
                COLLISION_WIDTH, COLLISION_HEIGHT, projectileSprite));
        game.audioManager.playSound("cyberlegacy/assets/sfx/shoot.wav");

        lastShotAt = now;
        attackAnimationEnd = now + (long) (SHOOT_FRAMES * SHOOT_FRAME_TIME * 1000.0);
        if (projectileDirection == CyberLegacy.Direction.LEFT
                || projectileDirection == CyberLegacy.Direction.RIGHT) {
            facing = projectileDirection;
        }
        setAnimationState(AnimationState.SHOOT);
    }

    @Override
    public void takeDamage(int dmg, CyberLegacy game) {
        if (health <= 0) return;

        int armoredDamage = Math.max(1, (int) Math.ceil(dmg * 0.75f));
        super.takeDamage(armoredDamage, game);

        long now = System.currentTimeMillis();
        if (health <= 0) {
            health = 0;
            isShocked = false;
            wasMoving = false;
            beginDestruction(game);
        } else {
            damageAnimationEnd = now + DAMAGE_ANIMATION_TIME;
            setAnimationState(AnimationState.DAMAGE);
        }
    }

    private void beginDestruction(CyberLegacy game) {
        destructionFinished = false;
        setAnimationState(AnimationState.DESTROY);

        triggerDestructionEffect(game);
    }

    private void triggerDestructionEffect(CyberLegacy game) {
        if (destructionEffectTriggered) return;

        destructionEffectTriggered = true;
        game.triggerShake(14);
        for (int i = 0; i < 18; i++) {
            game.particles.add(new Particle(
                    x + COLLISION_WIDTH / 2.0f,
                    y + COLLISION_HEIGHT / 2.0f,
                    i % 2 == 0 ? new Color(255, 150, 35) : new Color(255, 70, 15)));
        }
    }

    private void updateDestructionAnimation(CyberLegacy game, double delta) {
        if (animationState != AnimationState.DESTROY) {
            destructionFinished = false;
            setAnimationState(AnimationState.DESTROY);
        }
        triggerDestructionEffect(game);
        advanceAnimation(delta);
    }

    private void setAnimationState(AnimationState newState) {
        if (animationState == newState) return;
        animationState = newState;
        animationFrame = 0;
        animationTimer = 0.0;
    }

    private void advanceAnimation(double delta) {
        int frameCount = getFrameCount();
        if (frameCount <= 1) return;

        animationTimer += delta;
        double frameTime = getFrameTime();

        while (animationTimer >= frameTime) {
            animationTimer -= frameTime;

            if (animationState == AnimationState.DESTROY) {
                if (animationFrame < frameCount - 1) {
                    animationFrame++;
                } else {
                    destructionFinished = true;
                    animationTimer = 0.0;
                }
                continue;
            }

            if (animationState == AnimationState.START) {
                if (animationFrame < frameCount - 1) animationFrame++;
                else {
                    setAnimationState(AnimationState.DRIVE);
                    frameCount = getFrameCount();
                }
                continue;
            }

            if (animationState == AnimationState.STOP) {
                if (animationFrame < frameCount - 1) animationFrame++;
                else {
                    setAnimationState(AnimationState.IDLE);
                    frameCount = getFrameCount();
                }
                continue;
            }

            if (animationState == AnimationState.SHOOT) {
                if (animationFrame < frameCount - 1) animationFrame++;
                else animationTimer = 0.0;
                continue;
            }

            animationFrame = (animationFrame + 1) % frameCount;
        }
    }

    private int getFrameCount() {
        switch (animationState) {
            case IDLE: return IDLE_FRAMES;
            case SHOOT: return SHOOT_FRAMES;
            case DRIVE: return DRIVE_FRAMES;
            case START: return START_FRAMES;
            case DAMAGE: return DAMAGE_FRAMES;
            case DESTROY: return DESTROY_FRAMES;
            case STOP:
                return facing == CyberLegacy.Direction.LEFT ? STOP_LEFT_FRAMES : STOP_RIGHT_FRAMES;
            default: return 1;
        }
    }

    private double getFrameTime() {
        switch (animationState) {
            case IDLE: return IDLE_FRAME_TIME;
            case DRIVE: return DRIVE_FRAME_TIME;
            case START:
            case STOP: return TRANSITION_FRAME_TIME;
            case SHOOT: return SHOOT_FRAME_TIME;
            case DAMAGE: return DAMAGE_FRAME_TIME;
            case DESTROY: return DESTROY_FRAME_TIME;
            default: return IDLE_FRAME_TIME;
        }
    }

    private int getAnimationRow() {
        switch (animationState) {
            case IDLE: return ROW_IDLE;
            case SHOOT: return ROW_SHOOT;
            case DRIVE: return ROW_DRIVE;
            case DAMAGE: return ROW_DAMAGE;
            case DESTROY: return ROW_DESTROY;
            case START:
                return facing == CyberLegacy.Direction.LEFT ? ROW_START_LEFT : ROW_START_RIGHT;
            case STOP:
                return facing == CyberLegacy.Direction.LEFT ? ROW_STOP_LEFT : ROW_STOP_RIGHT;
            default: return ROW_IDLE;
        }
    }

    private BufferedImage getCurrentFrame() {
        if (spriteSheet == null) return null;

        int row = getAnimationRow();
        int maxFrame = Math.max(0, getFrameCount() - 1);
        int frame = Math.min(animationFrame, maxFrame);
        int xClip = frame * CELL_WIDTH;
        int yClip = row * CELL_HEIGHT;

        try {
            return spriteSheet.getSubimage(xClip, yClip, CELL_WIDTH, CELL_HEIGHT);
        } catch (RasterFormatException e) {
            return null;
        }
    }

    @Override
    public void renderShadow(Graphics g) {
        Graphics2D shadow = (Graphics2D) g.create();
        shadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        shadow.setColor(new Color(0, 0, 0, 150));
        shadow.fillOval((int) x + 3, (int) y + COLLISION_HEIGHT - 3, COLLISION_WIDTH - 2, 12);
        shadow.dispose();
    }

    @Override
    public void render(Graphics2D g2d) {
        Graphics2D pixelGraphics = (Graphics2D) g2d.create();
        pixelGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        pixelGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        pixelGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        BufferedImage frame = getCurrentFrame();
        int drawX = Math.round(x + COLLISION_WIDTH / 2.0f - CELL_WIDTH / 2.0f);
        int drawY = Math.round(y + COLLISION_HEIGHT - CELL_HEIGHT + 7.0f);

        if (frame != null) {
            if (facing == CyberLegacy.Direction.LEFT) {
                pixelGraphics.drawImage(frame, drawX + CELL_WIDTH, drawY,
                        drawX, drawY + CELL_HEIGHT, 0, 0, CELL_WIDTH, CELL_HEIGHT, null);
            } else {
                pixelGraphics.drawImage(frame, drawX, drawY, null);
            }

            if (isShocked && health > 0) {
                pixelGraphics.setColor(new Color(255, 255, 255, 120));
                pixelGraphics.fillRect(drawX, drawY, CELL_WIDTH, CELL_HEIGHT);
            }
        } else if (animationState == AnimationState.DESTROY) {
            renderExplosionFallback(g2d, drawX, drawY);
        } else {
            renderFallback(pixelGraphics, drawX, (int) y);
        }

        if (health < maxHealth && health > 0) {
            int barWidth = 60;
            int barX = Math.round(x + COLLISION_WIDTH / 2.0f - barWidth / 2.0f);
            int barY = (int) y - 10;
            pixelGraphics.setColor(new Color(20, 0, 0));
            pixelGraphics.fillRect(barX, barY, barWidth, 5);
            pixelGraphics.setColor(new Color(0, 220, 150));
            pixelGraphics.fillRect(barX, barY,
                    (int) ((health / (float) maxHealth) * barWidth), 5);
        }

        pixelGraphics.dispose();
    }

    private void renderExplosionFallback(Graphics2D g2d, int drawX, int drawY) {
        float progress = animationFrame / (float) Math.max(1, DESTROY_FRAMES - 1);
        int centerX = drawX + CELL_WIDTH / 2;
        int centerY = drawY + CELL_HEIGHT / 2;
        int radius = Math.round(8.0f + progress * 29.0f);

        g2d.setColor(new Color(255, 65, 10, 80));
        g2d.fillOval(centerX - radius - 8, centerY - radius - 8,
                (radius + 8) * 2, (radius + 8) * 2);
        g2d.setColor(new Color(255, 150, 20, 220));
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g2d.setColor(new Color(255, 245, 150, 245));
        int coreRadius = Math.max(3, radius / 2);
        g2d.fillOval(centerX - coreRadius, centerY - coreRadius,
                coreRadius * 2, coreRadius * 2);
    }

    private void renderFallback(Graphics2D g2d, int drawX, int drawY) {
        g2d.setColor(new Color(25, 70, 65));
        g2d.fillRect(drawX + 8, drawY + 10, 52, 22);
        g2d.setColor(new Color(25, 150, 125));
        g2d.fillRect(drawX + 17, drawY + 5, 28, 8);
        g2d.setColor(new Color(12, 25, 25));
        g2d.fillOval(drawX + 10, drawY + 27, 12, 12);
        g2d.fillOval(drawX + 46, drawY + 27, 12, 12);
    }

    @Override
    public boolean isReadyToRemove() {
        return health <= 0 && destructionFinished;
    }
}
