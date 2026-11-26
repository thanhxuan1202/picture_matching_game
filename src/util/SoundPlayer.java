package util;

import javax.sound.sampled.*;
import java.io.IOException;

public class SoundPlayer {
    private Clip matchClip;

    public SoundPlayer() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("/Game_data/click.wav"));
            matchClip = AudioSystem.getClip();
            matchClip.open(audioInputStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Lỗi: Không thể tải file âm thanh tại /Game_data/click.wav");
            e.printStackTrace();
            matchClip = null;
        }
    }

    public void playMatchSound() {
        if (matchClip != null) {
            matchClip.stop();
            matchClip.setFramePosition(0);
            matchClip.start();
        }
    }
}