package com.thurdas.cyberlegacy.managers;

import com.thurdas.cyberlegacy.CyberLegacy;
import com.thurdas.cyberlegacy.entities.Boss;
import com.thurdas.cyberlegacy.entities.Enemy;
import com.thurdas.cyberlegacy.entities.Portal;

public class WaveManager {
    private CyberLegacy game;
    public int currentWave = 0;

    public boolean isCountingDown = false;
    public long countdownStartTime;

    public WaveManager(CyberLegacy game) {
        this.game = game;
    }

    public void reset() {
        currentWave = 0;
        isCountingDown = true;
        countdownStartTime = System.currentTimeMillis();
    }

    public void tick(double delta) {
        if (isCountingDown) {
            if (System.currentTimeMillis() - countdownStartTime >= 3000) {
                isCountingDown = false;
            } else {
                return;
            }
        }

        if (game.enemies.isEmpty() && game.portal == null) {
            currentWave++;
            game.waveNotificationTimer = 140;

            int expectedPhase = (currentWave / 6) + 1;

            if (expectedPhase > game.currentPhase) {
                spawnPortal(expectedPhase);
            } else {
                spawnEnemiesForWave();
            }
        }
    }

    private void spawnPortal(int nextPhase) {
        float portalX = game.player.x;
        float portalY = game.player.y - 100;
        game.portal = new Portal(portalX, portalY, nextPhase);
    }

    private void spawnEnemiesForWave() {
        boolean isBossWave = (currentWave % 5 == 0);

        if (isBossWave) {
            boolean bossSpawned = false;
            while (!bossSpawned) {
                float spawnX = (float) (game.player.x + (Math.random() * 800 - 400));
                float spawnY = (float) (game.player.y + (Math.random() * 800 - 400));

                if (!game.isSolid(spawnX, spawnY, 64, 64)) {
                    game.enemies.add(new Boss(spawnX, spawnY, currentWave));
                    bossSpawned = true;
                }
            }
        }

        int enemiesToSpawn = 5 + (currentWave * 2);

        if (isBossWave) {
            enemiesToSpawn /= 2;
        }

        for (int i = 0; i < enemiesToSpawn; i++) {
            float spawnX = (float) (game.player.x + (Math.random() * 600 - 300));
            float spawnY = (float) (game.player.y + (Math.random() * 600 - 300));

            if (!game.isSolid(spawnX, spawnY, 32, 32)) {
                game.enemies.add(new Enemy(spawnX, spawnY, currentWave));
            } else {
                i--;
            }
        }
    }
}
