package controller;

import main.GameLatHinh;
import model.LevelConfig;
import model.GameState;
import view.GamePanel;
import view.MenuPanel;

import javax.swing.*;
import java.io.File;

public class GameController {
    private GameLatHinh game;
    private JFrame frame;
    private MenuPanel menuPanel;
    private GamePanel gamePanel;
    private LevelConfig levelConfig;

    public GameController(GameLatHinh game, JFrame frame) {
        this.game = game;
        this.frame = frame;
        this.levelConfig = new LevelConfig();
        showMenu();
    }

    public void startNewGame() {
        new File("resources/Game_data/game_state.txt").delete();
        gamePanel = new GamePanel(this, levelConfig, 0, 100); // Bắt đầu Level 1 với 100 điểm
        frame.getContentPane().removeAll();
        frame.add(gamePanel);
        frame.revalidate();
        frame.repaint();
    }

    public void nextGame(int level, int currentScore) {
        int newScore = (level == 0) ? 100 : currentScore + (gamePanel != null ? (gamePanel.getMaxTime() - gamePanel.getTime()) / 50 : 0);
        gamePanel = new GamePanel(this, levelConfig, level, newScore); // Sử dụng điểm hiện tại + thưởng thời gian
        frame.getContentPane().removeAll();
        frame.add(gamePanel);
        frame.revalidate();
        frame.repaint();
    }

    public void showMenu() {
        menuPanel = new MenuPanel(this, levelConfig);
        frame.getContentPane().removeAll();
        frame.add(menuPanel);
        frame.revalidate();
        frame.repaint();
    }

    public void continueGame() {
        GameState state = GameState.loadState();
        if (state != null) {
            gamePanel = new GamePanel(this, levelConfig, state.getLevel(), state.getScore());
            frame.getContentPane().removeAll();
            frame.add(gamePanel);
            frame.revalidate();
            frame.repaint();
        } else {
            JOptionPane.showMessageDialog(frame, "Không có trò chơi nào để tiếp tục!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            showMenu();
        }
    }

    public void exitGame() {
        frame.dispose();
        System.exit(0);
    }
}