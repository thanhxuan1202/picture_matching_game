package view;

import controller.GameController;
import model.LevelConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

public class MenuPanel extends JPanel {
    private GameController controller;
    private LevelConfig levelConfig;

    public MenuPanel(GameController controller, LevelConfig levelConfig) {
        this.controller = controller;
        this.levelConfig = levelConfig;
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Game Lật Hình", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startButton = new RoundedButton("Start", 10);
        startButton.setFont(new Font("Arial", Font.BOLD, 30));
        startButton.setPreferredSize(new Dimension(200, 60));
        startButton.setBackground(Color.GREEN);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> controller.startNewGame());

        JButton continueButton = null;
        File stateFile = new File("resources/Game_data/game_state.txt");
        if (stateFile.exists()) {
            continueButton = new RoundedButton("Continue", 10);
            continueButton.setFont(new Font("Arial", Font.BOLD, 30));
            continueButton.setPreferredSize(new Dimension(200, 60));
            continueButton.setBackground(Color.YELLOW);
            continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            continueButton.addActionListener(e -> controller.continueGame());
        }

        JButton exitButton = new RoundedButton("Exit", 10);
        exitButton.setFont(new Font("Arial", Font.BOLD, 30));
        exitButton.setPreferredSize(new Dimension(200, 60));
        exitButton.setBackground(Color.RED);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> controller.exitGame());

        buttonPanel.add(titleLabel);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(startButton);
        if (continueButton != null) {
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(continueButton);
        }
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(exitButton);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(buttonPanel, gbc);

        add(centerPanel, BorderLayout.CENTER);

        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            Image background = new ImageIcon(getClass().getResource("/Game_data/background.jpg")).getImage();
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        } catch (Exception e) {
            System.err.println("Lỗi: Không thể tải background.jpg");
            e.printStackTrace();
        }
    }
}

class RoundedButton extends JButton {
    private int radius;
    private Icon defaultIcon;

    RoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    RoundedButton(Icon icon, int radius) {
        super(icon);
        this.radius = radius;
        this.defaultIcon = icon;
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(getBackground());
        g2.fill(roundedRectangle);

        g2.setColor(getForeground());
        g2.draw(roundedRectangle);

        if (getIcon() != null) {
            getIcon().paintIcon(this, g2, (getWidth() - getIcon().getIconWidth()) / 2, (getHeight() - getIcon().getIconHeight()) / 2);
        } else {
            super.paintComponent(g2);
        }

        g2.dispose();
    }
}