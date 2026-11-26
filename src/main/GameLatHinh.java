package main;

import controller.GameController;

import javax.swing.*;

public class GameLatHinh {
    private JFrame frame;
    private GameController controller;

    public GameLatHinh() {
        frame = new JFrame("Game Lật Hình");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);

        controller = new GameController(this, frame);

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameLatHinh::new);
    }
}