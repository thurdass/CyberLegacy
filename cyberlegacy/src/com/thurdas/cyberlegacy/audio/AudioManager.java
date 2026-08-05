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
                AudioInputStream convertedStream = convertAudioFormat(audioStream);
                musicClip = AudioSystem.getClip();
                musicClip.open(convertedStream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip.start();
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar música: " + e.getMessage());
        }
    }

    public void playSound(String sfxPath) {
        new Thread(() -> {
            try {
                File audioFile = new File(sfxPath);
                if (audioFile.exists()) {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    AudioInputStream convertedStream = convertAudioFormat(audioStream);
                    Clip clip = AudioSystem.getClip();
                    clip.open(convertedStream);
                    clip.start();
                }
            } catch (Exception e) {
                System.err.println("Erro ao tocar efeito sonoro: " + e.getMessage());
            }
        }).start();
    }

    private AudioInputStream convertAudioFormat(AudioInputStream audioStream) {
        AudioFormat originalFormat = audioStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            Math.min(originalFormat.getSampleRate(), 44100),
            16,
            Math.min(originalFormat.getChannels(), 2),
            Math.min(originalFormat.getChannels(), 2) * 2,
            Math.min(originalFormat.getSampleRate(), 44100),
            false
        );

        if (originalFormat.equals(targetFormat)) {
            return audioStream;
        }

        return AudioSystem.getAudioInputStream(targetFormat, audioStream);
    }
}
