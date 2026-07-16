package com.thurdas.cyberlegacy.audio;

import javax.sound.sampled.*;
import java.io.File;

public class AudioManager {
    private Clip musicClip;

    public void playMusic(String trackPath) {
        try {
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop();
                musicClip.close();
            }
            File audioFile = new File(trackPath);
            if (audioFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                musicClip = AudioSystem.getClip();
                musicClip.open(audioStream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip.start();
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar música: " + e.getMessage());
        }
    }

    public void playSound(String sfxPath) {
        try {
            File audioFile = new File(sfxPath);
            if (audioFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar efeito sonoro: " + e.getMessage());
        }
    }
}
