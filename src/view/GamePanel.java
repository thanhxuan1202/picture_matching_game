/*package view;

import controller.GameController;
import model.LevelConfig;
import model.GameState;
import util.SoundPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener {
    private GameController controller;
    private LevelConfig levelConfig;
    private int level, score, count, id, preX, preY, X, Y, hit, time, maxTime, hintsUsed;
    private int m, n, maxXY = 100;
    private JButton[][] bt = new JButton[maxXY][maxXY];
    private boolean[][] tick = new boolean[maxXY][maxXY];
    private boolean[][] clicked = new boolean[maxXY][maxXY];
    private int[][] a = new int[maxXY][maxXY];
    private JProgressBar progressTime;
    private JButton scoreButton, hintButton, pauseButton;
    private Timer timer, timer2, hintTimer;
    private SoundPlayer soundPlayer;
    private JLayeredPane layeredPane;
    private JPanel pausePanel, mainPanel, gridPanel, loadingPanel;
    private final int MAX_HINTS = 3;
    private int cardSize;
    private HashMap<Integer, Image> scaledIconCache;
    private boolean isProcessing = false;

    public GamePanel(GameController controller, LevelConfig levelConfig, int level, int score) {
        this.controller = controller;
        this.levelConfig = levelConfig;
        this.level = level;
        this.hintsUsed = 0;
        this.soundPlayer = new SoundPlayer();

        GameState state = GameState.loadState();
        if (state != null && state.getLevel() == level) {
            this.hintsUsed = state.getHintsUsed();
            this.score = state.getScore();
            this.m = state.getM();
            this.n = state.getN();
            this.time = state.getTime();
            this.maxTime = state.getMaxTime();
            this.preX = state.getPreX();
            this.preY = state.getPreY();
            this.id = state.getId();
            this.count = state.getCount();
            this.hit = state.getHit();
            int[][] savedA = state.getMatrixA();
            boolean[][] savedTick = state.getTick();
            boolean[][] savedClicked = state.getClicked();
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    a[i][j] = savedA[i][j];
                    tick[i][j] = savedTick[i][j];
                    clicked[i][j] = (savedClicked != null && i < savedClicked.length && j < savedClicked[i].length) ? savedClicked[i][j] : false;
                }
            }
            if (hit == m * n / 2) {
                timer2 = new Timer(100, null);
                timer2.stop();
                if (level + 1 < levelConfig.getLevelCount()) {
                    controller.nextGame(level + 1, score + (maxTime - time) / 50);
                } else {
                    showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + score + "\nChơi lại?", "Thông báo");
                }
                return;
            }
        } else {
            this.score = score;
            this.m = levelConfig.getSizeX(level);
            this.n = levelConfig.getSizeY(level);
            this.maxTime = levelConfig.getTime(level) * 10;
            this.time = 0;
            this.hit = 0;
            this.count = 0;
            createMatrix();
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    clicked[i][j] = false;
                }
            }
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.8);
        int maxHeight = (int) (screenSize.height * 0.6);
        cardSize = Math.min(maxWidth / n, maxHeight / m);

        scaledIconCache = new HashMap<>();

        timer = new Timer(400, e -> {
            open();
            isProcessing = false;
            timer.stop();
        });
        timer2 = new Timer(100, e -> {
            time++;
            progressTime.setValue(maxTime - time);
            int currentScore = Integer.parseInt(scoreButton.getText());
            if (maxTime == time || currentScore <= 0) {
                timer2.stop();
                if (currentScore <= 0) {
                    showDialogGameOver("Thua!\nĐiểm: " + currentScore + "\nBạn có muốn chơi lại không?", "Thông báo");
                } else {
                    showDialogNewGame("Hết thời gian.\nĐiểm: " + currentScore + "\nBạn có muốn chơi lại không?", "Thông báo");
                }
            }
        });
        hintTimer = new Timer(1000, e -> {
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (tick[i][j] && !(count == 1 && i == preX && j == preY)) {
                        bt[i][j].setIcon(getIcon(0));
                    }
                }
            }
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (tick[i][j] && bt[i][j].getIcon() == getIcon(0) && !(count == 1 && i == preX && j == preY)) {
                        clicked[i][j] = false;
                    }
                }
            }
            hintTimer.stop();
        });

        layeredPane = new JLayeredPane();
        layeredPane.setVisible(false);

        setLayout(new BorderLayout());
        loadingPanel = new JPanel();
        loadingPanel.setBackground(Color.LIGHT_GRAY);
        JLabel loadingLabel = new JLabel("Đang tải...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 20));
        loadingPanel.add(loadingLabel);
        add(loadingPanel, BorderLayout.CENTER);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                long startTime = System.currentTimeMillis();
                if (a[0][0] == 0 && state == null) {
                    createMatrix();
                }
                preloadIcons();
                initComponents();
                System.out.println("Thời gian tải (ms): " + (System.currentTimeMillis() - startTime));
                return null;
            }

            @Override
            protected void done() {
                remove(loadingPanel);
                add(mainPanel, BorderLayout.CENTER);
                resetCardIcons();
                timer2.start();
                revalidate();
                repaint();
            }
        }.execute();
    }

    private void preloadIcons() {
        for (int i : new int[]{-1, 0}) {
            String fileName = "/Game_data/" + i + ".jpg";
            try {
                Image rawImage = new ImageIcon(getClass().getResource(fileName)).getImage();
                scaledIconCache.put(i, rawImage.getScaledInstance(cardSize, cardSize, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                System.err.println("Lỗi: Không thể tải hình ảnh tại " + fileName);
                e.printStackTrace();
                scaledIconCache.put(i, null);
            }
        }

        int maxPairs = Math.min(m * n / 2, 20);
        for (int i = 1; i <= maxPairs; i++) {
            if (getClass().getResource("/Game_data/" + i + ".jpg") == null) {
                System.err.println("Thiếu hình ảnh: /Game_data/" + i + ".jpg");
                break;
            }
            String fileName = "/Game_data/" + i + ".jpg";
            try {
                Image rawImage = new ImageIcon(getClass().getResource(fileName)).getImage();
                scaledIconCache.put(i, rawImage.getScaledInstance(cardSize, cardSize, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                System.err.println("Lỗi: Không thể tải hình ảnh tại " + fileName);
                e.printStackTrace();
                scaledIconCache.put(i, null);
            }
        }
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout());

        gridPanel = new JPanel(new GridLayout(m, n, 2, 2));
        gridPanel.setBackground(Color.WHITE);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (bt[i][j] == null) {
                    bt[i][j] = new RoundedButton(getIcon(0), 15);
                    bt[i][j].setActionCommand(i + " " + j);
                    bt[i][j].addActionListener(this);
                    bt[i][j].setBackground(Color.WHITE);
                    bt[i][j].setMargin(new Insets(0, 0, 0, 0));
                }
                if (tick[i][j]) {
                    bt[i][j].setIcon(getIcon(0));
                } else {
                    bt[i][j].setIcon(getIcon(-1));
                    bt[i][j].setBorder(null);
                }
                gridPanel.add(bt[i][j]);
            }
        }

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(Color.WHITE);
        JLabel scoreLabel = new JLabel("Score: ");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setForeground(Color.BLACK);
        if (scoreButton == null) {
            scoreButton = new RoundedButton(String.valueOf(score), 10);
            scoreButton.setFont(new Font("Arial", Font.BOLD, 20));
            scoreButton.setBackground(Color.WHITE);
        } else {
            scoreButton.setText(String.valueOf(score));
        }

        if (hintButton == null) {
            hintButton = new RoundedButton("Hint (" + (MAX_HINTS - hintsUsed) + ")", 10);
            hintButton.setFont(new Font("Arial", Font.BOLD, 20));
            hintButton.setBackground(Color.YELLOW);
            hintButton.addActionListener(e -> useHint());
        } else {
            hintButton.setText("Hint (" + (MAX_HINTS - hintsUsed) + ")");
        }

        if (pauseButton == null) {
            pauseButton = new RoundedButton("Pause", 10);
            pauseButton.setFont(new Font("Arial", Font.BOLD, 20));
            pauseButton.setBackground(Color.CYAN);
            pauseButton.addActionListener(e -> showPausePanel());
        }

        bottomPanel.add(scoreLabel);
        bottomPanel.add(scoreButton);
        bottomPanel.add(hintButton);
        bottomPanel.add(pauseButton);

        if (progressTime == null) {
            progressTime = new JProgressBar(0, maxTime);
            progressTime.setForeground(Color.ORANGE);
        }
        progressTime.setValue(maxTime - time);

        mainPanel.add(progressTime, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void resetCardIcons() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (tick[i][j]) {
                    if (count == 1 && i == preX && j == preY) {
                        bt[i][j].setIcon(getIcon(a[i][j]));
                    } else if (!clicked[i][j]) {
                        bt[i][j].setIcon(getIcon(0));
                    } else {
                        bt[i][j].setIcon(getIcon(0));
                    }
                } else {
                    bt[i][j].setIcon(getIcon(-1));
                    bt[i][j].setBorder(null);
                }
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void createMatrix() {
        int N = m * n;
        int[] b = new int[N];
        int[] c = new int[N];
        for (int i = 0; i < N; i++) {
            b[i] = i + 1;
            if (b[i] > N / 2) b[i] -= N / 2;
            c[i] = (int) (Math.random() * 1000000);
        }
        for (int i = 0; i < N - 1; i++) {
            for (int j = i + 1; j < N; j++) {
                if (c[i] > c[j]) {
                    int tmp = b[i];
                    b[i] = b[j];
                    b[j] = tmp;
                    tmp = c[i];
                    c[i] = c[j];
                    c[j] = tmp;
                }
            }
        }
        int index = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = b[index++];
                tick[i][j] = true;
                clicked[i][j] = false;
            }
        }
    }

    private Icon getIcon(int index) {
        Image scaledImage = scaledIconCache.get(index);
        if (scaledImage == null) {
            return null;
        }
        return new ImageIcon(scaledImage);
    }

    private void open() {
        if (id == a[X][Y]) {
            bt[preX][preY].setIcon(getIcon(-1));
            bt[X][Y].setIcon(getIcon(-1));
            a[X][Y] = a[preX][preY] = 0;
            tick[X][Y] = tick[preX][preY] = false;
            clicked[preX][preY] = true;
            clicked[X][Y] = true;
            scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) + 25));
            hit++;
            soundPlayer.playMatchSound();
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
            if (hit == m * n / 2) {
                timer.stop();
                timer2.stop();
                if (level + 1 < levelConfig.getLevelCount()) {
                    controller.nextGame(level + 1, Integer.parseInt(scoreButton.getText()) + (maxTime - time) / 50);
                } else {
                    showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + scoreButton.getText() + "\nChơi lại?", "Thông báo");
                }
            }
        } else {
            bt[preX][preY].setIcon(getIcon(0));
            bt[X][Y].setIcon(getIcon(0));
            clicked[preX][preY] = false;
            clicked[X][Y] = false;
            scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) - 10));
        }
    }

    private void useHint() {
        if (hintsUsed >= MAX_HINTS) {
            JOptionPane.showMessageDialog(this, "Bạn đã sử dụng hết số lần gợi ý!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (tick[i][j]) {
                    for (int k = 0; k < m; k++) {
                        for (int l = 0; l < n; l++) {
                            if (tick[k][l] && a[i][j] == a[k][l] && (i != k || j != l)) {
                                pairs.add(new int[]{i, j, k, l});
                            }
                        }
                    }
                }
            }
        }

        if (!pairs.isEmpty()) {
            int[] pair = pairs.get(0);
            boolean includesPreSelected = (count == 1 && ((pair[0] == preX && pair[1] == preY) || (pair[2] == preX && pair[3] == preY)));
            bt[pair[0]][pair[1]].setIcon(getIcon(a[pair[0]][pair[1]]));
            bt[pair[2]][pair[3]].setIcon(getIcon(a[pair[2]][pair[3]]));
            hintTimer = new Timer(1000, e -> {
                bt[pair[0]][pair[1]].setIcon(getIcon(-1));
                bt[pair[2]][pair[3]].setIcon(getIcon(-1));
                a[pair[0]][pair[1]] = 0;
                a[pair[2]][pair[3]] = 0;
                tick[pair[0]][pair[1]] = false;
                tick[pair[2]][pair[3]] = false;
                clicked[pair[0]][pair[1]] = true;
                clicked[pair[2]][pair[3]] = true;
                if (includesPreSelected) {
                    count = 0;
                }
                hit++;
                soundPlayer.playMatchSound();
                GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
                if (hit == m * n / 2) {
                    timer.stop();
                    timer2.stop();
                    if (level + 1 < levelConfig.getLevelCount()) {
                        controller.nextGame(level + 1, Integer.parseInt(scoreButton.getText()) + (maxTime - time) / 50);
                    } else {
                        showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + scoreButton.getText() + "\nChơi lại?", "Thông báo");
                    }
                }
                hintTimer.stop();
            });
            hintsUsed++;
            hintButton.setText("Hint (" + (MAX_HINTS - hintsUsed) + ")");
            scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) - 50));
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
            hintTimer.start();
            soundPlayer.playMatchSound();
        }
    }

    private void showPausePanel() {
        timer2.stop();

        GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);

        if (!layeredPane.isVisible()) {
            removeAll();
            setLayout(new BorderLayout());
            add(layeredPane, BorderLayout.CENTER);
            layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
            layeredPane.setVisible(true);
        }

        pausePanel = new JPanel() {
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
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        pausePanel.setLayout(new GridBagLayout());
        pausePanel.setBounds(0, 0, getWidth(), getHeight());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Paused", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton resumeButton = new RoundedButton("Resume", 10);
        resumeButton.setFont(new Font("Arial", Font.BOLD, 30));
        resumeButton.setPreferredSize(new Dimension(200, 60));
        resumeButton.setBackground(Color.GREEN);
        resumeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resumeButton.addActionListener(e -> {
            layeredPane.remove(pausePanel);
            layeredPane.setVisible(false);
            setLayout(new BorderLayout());
            add(mainPanel, BorderLayout.CENTER);
            timer2.start();
            revalidate();
            repaint();
        });

        JButton restartButton = new RoundedButton("Restart", 10);
        restartButton.setFont(new Font("Arial", Font.BOLD, 30));
        restartButton.setPreferredSize(new Dimension(200, 60));
        restartButton.setBackground(Color.ORANGE);
        restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        restartButton.addActionListener(e -> {
            GameState.clearState();
            controller.startNewGame();
        });

        JButton menuButton = new RoundedButton("Back", 10);
        menuButton.setFont(new Font("Arial", Font.BOLD, 30));
        menuButton.setPreferredSize(new Dimension(200, 60));
        menuButton.setBackground(Color.RED);
        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuButton.addActionListener(e -> {
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
            controller.showMenu();
        });

        buttonPanel.add(titleLabel);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(resumeButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(restartButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(menuButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pausePanel.add(buttonPanel, gbc);

        layeredPane.add(pausePanel, JLayeredPane.POPUP_LAYER);
        revalidate();
        repaint();
    }

    private void showDialogNewGame(String message, String title) {
        int select = JOptionPane.showOptionDialog(null, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (select == 0) {
            GameState.clearState();
            controller.startNewGame();
        } else {
            controller.exitGame();
        }
    }

    private void showDialogGameOver(String message, String title) {
        int select = JOptionPane.showOptionDialog(null, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (select == 0) {
            GameState.clearState();
            controller.startNewGame();
        } else {
            controller.exitGame();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        int k = s.indexOf(" ");
        int i = Integer.parseInt(s.substring(0, k));
        int j = Integer.parseInt(s.substring(k + 1));

        if (tick[i][j] && !clicked[i][j] && !isProcessing) {
            if (count == 1 && preX == i && preY == j) {
                return;
            }

            clicked[i][j] = true;
            bt[i][j].setIcon(getIcon(a[i][j]));
            if (count == 0) {
                id = a[i][j];
                preX = i;
                preY = j;
                count = 1;
            } else {
                X = i;
                Y = j;
                isProcessing = true;
                if (timer != null) timer.start();
                count = 0;
            }
        }
    }

    private static class RoundedButton extends JButton {
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

    public int getLevel() { return level; }
    public int getScore() { return Integer.parseInt(scoreButton.getText()); }
    public int getHintsUsed() { return hintsUsed; }
    public int[][] getMatrixA() { return a; }
    public boolean[][] getTick() { return tick; }
    public int getTime() { return time; }
    public int getMaxTime() { return maxTime; }
    public int getPreX() { return preX; }
    public int getPreY() { return preY; }
    public int getId() { return id; }
    public int getCount() { return count; }
    public int getHit() { return hit; }
    public int getM() { return m; }
    public int getN() { return n; }
    public boolean[][] getClicked() { return clicked; }
}*/
package view;

import controller.GameController;
import model.LevelConfig;
import model.GameState;
import util.SoundPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private GameController controller;
    private LevelConfig levelConfig;
    private int level, score, count, id, preX, preY, X, Y, hit, time, maxTime, hintsUsed;
    private int m, n, maxXY = 100;
    private ArrayList<ArrayList<JButton>> bt = new ArrayList<>();
    private boolean[][] tick = new boolean[maxXY][maxXY];
    private boolean[][] clicked = new boolean[maxXY][maxXY];
    private int[][] a = new int[maxXY][maxXY];
    private JProgressBar progressTime;
    private JButton scoreButton, hintButton, pauseButton;
    private int consecutiveMatches; // Biến đếm số lần ghép đúng liên tiếp
    private Timer timer, timer2, hintTimer;
    private SoundPlayer soundPlayer;
    private JLayeredPane layeredPane;
    private JPanel pausePanel, mainPanel, gridPanel, loadingPanel;
    private final int MAX_HINTS = 3;
    private int cardSize;
    private HashMap<Integer, Image> scaledIconCache;
    private boolean isProcessing = false;

    public GamePanel(GameController controller, LevelConfig levelConfig, int level, int score) {
        this.controller = controller;
        this.levelConfig = levelConfig;
        this.level = level;
        this.hintsUsed = 0;
        this.soundPlayer = new SoundPlayer();

        GameState state = GameState.loadState();
        if (state != null && state.getLevel() == level) {
            this.hintsUsed = state.getHintsUsed();
            this.score = state.getScore();
            this.m = state.getM();
            this.n = state.getN();
            this.time = state.getTime();
            this.maxTime = state.getMaxTime();
            this.preX = state.getPreX();
            this.consecutiveMatches = state.getConsecutiveMatches();
            this.preY = state.getPreY();
            this.id = state.getId();
            this.count = state.getCount();
            this.hit = state.getHit();
            int[][] savedA = state.getMatrixA();
            boolean[][] savedTick = state.getTick();
            boolean[][] savedClicked = state.getClicked();
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    a[i][j] = savedA[i][j];
                    tick[i][j] = savedTick[i][j];
                    clicked[i][j] = (savedClicked != null && i < savedClicked.length && j < savedClicked[i].length) ? savedClicked[i][j] : false;
                }
            }
            if (hit == m * n / 2) {
                timer2 = new Timer(100, null);
                timer2.stop();
                if (level + 1 < levelConfig.getLevelCount()) {
                    controller.nextGame(level + 1, score + (maxTime - time) / 50);
                } else {
                    showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + score + "\nChơi lại?", "Thông báo");
                }
                return;
            }
        } else {
            this.score = score;
            this.m = levelConfig.getSizeX(level);
            this.n = levelConfig.getSizeY(level);
            this.maxTime = levelConfig.getTime(level) * 10;
            this.time = 0;
            this.hit = 0;
            this.count = 0;
            this.consecutiveMatches = 0;
            createMatrix();
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    clicked[i][j] = false;
                }
            }
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.8);
        int maxHeight = (int) (screenSize.height * 0.6);
        cardSize = Math.min(maxWidth / n, maxHeight / m);

        scaledIconCache = new HashMap<>();

        timer = new Timer(400, e -> {
            open();
            isProcessing = false;
            timer.stop();
        });
        timer2 = new Timer(100, e -> {
            time++;
            progressTime.setValue(maxTime - time);
            int currentScore = Integer.parseInt(scoreButton.getText());
            if (maxTime == time || currentScore <= 0) {
                timer2.stop();
                if (currentScore <= 0) {
                    showDialogGameOver("Thua!\nĐiểm: " + currentScore + "\nBạn có muốn chơi lại không?", "Thông báo");
                } else {
                    showDialogNewGame("Hết thời gian.\nĐiểm: " + currentScore + "\nBạn có muốn chơi lại không?", "Thông báo");
                }
            }
        });
        hintTimer = new Timer(1000, e -> {
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (tick[i][j] && !(count == 1 && i == preX && j == preY)) {
                        bt.get(i).get(j).setIcon(getIcon(0));
                    }
                }
            }
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (tick[i][j] && bt.get(i).get(j).getIcon() == getIcon(0) && !(count == 1 && i == preX && j == preY)) {
                        clicked[i][j] = false;
                    }
                }
            }
            hintTimer.stop();
        });

        layeredPane = new JLayeredPane();
        layeredPane.setVisible(false);

        setLayout(new BorderLayout());
        loadingPanel = new JPanel();
        loadingPanel.setBackground(Color.LIGHT_GRAY);
        JLabel loadingLabel = new JLabel("Đang tải...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 20));
        loadingPanel.add(loadingLabel);
        add(loadingPanel, BorderLayout.CENTER);

        // Thêm KeyListener
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                long startTime = System.currentTimeMillis();
                if (a[0][0] == 0 && state == null) {
                    createMatrix();
                }
                preloadIcons();
                initComponents();
                System.out.println("Thời gian tải (ms): " + (System.currentTimeMillis() - startTime));
                return null;
            }

            @Override
            protected void done() {
                remove(loadingPanel);
                add(mainPanel, BorderLayout.CENTER);
                resetCardIcons();
                timer2.start();
                revalidate();
                repaint();
            }
        }.execute();
    }

    private void preloadIcons() {
        for (int i : new int[]{-1, 0}) {
            String fileName = "/Game_data/" + i + ".jpg";
            try {
                Image rawImage = new ImageIcon(getClass().getResource(fileName)).getImage();
                scaledIconCache.put(i, rawImage.getScaledInstance(cardSize, cardSize, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                System.err.println("Lỗi: Không thể tải hình ảnh tại " + fileName);
                e.printStackTrace();
                scaledIconCache.put(i, null);
            }
        }
        int maxPairs = Math.min(m * n / 2, 20);
        for (int i = 1; i <= maxPairs; i++) {
            if (getClass().getResource("/Game_data/" + i + ".jpg") == null) {
                System.err.println("Thiếu hình ảnh: /Game_data/" + i + ".jpg");
                break;
            }
            String fileName = "/Game_data/" + i + ".jpg";
            try {
                Image rawImage = new ImageIcon(getClass().getResource(fileName)).getImage();
                scaledIconCache.put(i, rawImage.getScaledInstance(cardSize, cardSize, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                System.err.println("Lỗi: Không thể tải hình ảnh tại " + fileName);
                e.printStackTrace();
                scaledIconCache.put(i, null);
            }
        }
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout());

        gridPanel = new JPanel(new GridLayout(m, n, 2, 2));
        gridPanel.setBackground(Color.WHITE);
        for (int i = 0; i < m; i++) {
            bt.add(new ArrayList<>());
            for (int j = 0; j < n; j++) {
                if (bt.get(i).size() <= j) {
                    bt.get(i).add(new RoundedButton(getIcon(0), 15));
                    bt.get(i).get(j).setActionCommand(i + " " + j);
                    bt.get(i).get(j).addActionListener(this);
                    bt.get(i).get(j).setBackground(Color.WHITE);
                    bt.get(i).get(j).setMargin(new Insets(0, 0, 0, 0));
                }
                if (tick[i][j]) {
                    bt.get(i).get(j).setIcon(getIcon(0));
                } else {
                    bt.get(i).get(j).setIcon(getIcon(-1));
                    bt.get(i).get(j).setBorder(null);
                }
                gridPanel.add(bt.get(i).get(j));
            }
        }

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(Color.WHITE);
        JLabel scoreLabel = new JLabel("Score: ");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setForeground(Color.BLACK);
        if (scoreButton == null) {
            scoreButton = new RoundedButton(String.valueOf(score), 10);
            scoreButton.setFont(new Font("Arial", Font.BOLD, 20));
            scoreButton.setBackground(Color.WHITE);
        } else {
            scoreButton.setText(String.valueOf(score));
        }

        if (hintButton == null) {
            hintButton = new RoundedButton("Hint (" + (MAX_HINTS - hintsUsed) + ")", 10);
            hintButton.setFont(new Font("Arial", Font.BOLD, 20));
            hintButton.setBackground(Color.YELLOW);
            hintButton.addActionListener(e -> useHint());
        } else {
            hintButton.setText("Hint (" + (MAX_HINTS - hintsUsed) + ")");
        }

        if (pauseButton == null) {
            pauseButton = new RoundedButton("Pause", 10);
            pauseButton.setFont(new Font("Arial", Font.BOLD, 20));
            pauseButton.setBackground(Color.CYAN);
            pauseButton.addActionListener(e -> showPausePanel());
        }

        bottomPanel.add(scoreLabel);
        bottomPanel.add(scoreButton);
        bottomPanel.add(hintButton);
        bottomPanel.add(pauseButton);

        if (progressTime == null) {
            progressTime = new JProgressBar(0, maxTime);
            progressTime.setForeground(Color.ORANGE);
        }
        progressTime.setValue(maxTime - time);

        mainPanel.add(progressTime, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void resetCardIcons() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (tick[i][j]) {
                    if (count == 1 && i == preX && j == preY) {
                        bt.get(i).get(j).setIcon(getIcon(a[i][j]));
                    } else if (!clicked[i][j]) {
                        bt.get(i).get(j).setIcon(getIcon(0));
                    } else {
                        bt.get(i).get(j).setIcon(getIcon(0));
                    }
                } else {
                    bt.get(i).get(j).setIcon(getIcon(-1));
                    bt.get(i).get(j).setBorder(null);
                }
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void createMatrix() {
        int N = m * n;
        int[] b = new int[N];
        int[] c = new int[N];
        for (int i = 0; i < N; i++) {
            b[i] = i + 1;
            if (b[i] > N / 2) b[i] -= N / 2;
            c[i] = (int) (Math.random() * 1000000);
        }
        for (int i = 0; i < N - 1; i++) {
            for (int j = i + 1; j < N; j++) {
                if (c[i] > c[j]) {
                    int tmp = b[i];
                    b[i] = b[j];
                    b[j] = tmp;
                    tmp = c[i];
                    c[i] = c[j];
                    c[j] = tmp;
                }
            }
        }
        int index = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = b[index++];
                tick[i][j] = true;
                clicked[i][j] = false;
            }
        }
    }

    private Icon getIcon(int index) {
        Image scaledImage = scaledIconCache.get(index);
        if (scaledImage == null) {
            return null;
        }
        return new ImageIcon(scaledImage);
    }

  /*  private void open() {
        if (id == a[X][Y]) {
            bt.get(preX).get(preY).setIcon(getIcon(-1));
            bt.get(X).get(Y).setIcon(getIcon(-1));
            a[X][Y] = a[preX][preY] = 0;
            tick[X][Y] = tick[preX][preY] = false;
            clicked[preX][preY] = true;
            clicked[X][Y] = true;
            scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) + 25));
            hit++;
            soundPlayer.playMatchSound();
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
            if (hit == m * n / 2) {
                timer.stop();
                timer2.stop();
                if (level + 1 < levelConfig.getLevelCount()) {
                    controller.nextGame(level + 1, Integer.parseInt(scoreButton.getText()) + (maxTime - time) / 50);
                } else {
                    showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + scoreButton.getText() + "\nChơi lại?", "Thông báo");
                }
            }
        } else {
            bt.get(preX).get(preY).setIcon(getIcon(0));
            bt.get(X).get(Y).setIcon(getIcon(0));
            clicked[preX][preY] = false;
            clicked[X][Y] = false;
            scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) - 10));
        }
    }*/
    private void open() {
    if (id == a[X][Y]) {
        bt.get(preX).get(preY).setIcon(getIcon(-1));
        bt.get(X).get(Y).setIcon(getIcon(-1));
        a[X][Y] = a[preX][preY] = 0;
        tick[X][Y] = tick[preX][preY] = false;
        clicked[preX][preY] = true;
        clicked[X][Y] = true;
        scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) + 25));
        hit++;
        consecutiveMatches++; // Tăng số lần ghép đúng liên tiếp
        if (consecutiveMatches == 2) {
            int remainingTime = maxTime - time;
            int newRemainingTime = remainingTime * 2;
            maxTime = time + newRemainingTime;
            progressTime.setMaximum(maxTime);
            progressTime.setValue(maxTime - time);
            consecutiveMatches = 0; // Đặt lại sau khi nhân đôi
            JOptionPane.showMessageDialog(this, "Ghép đúng 2 lần liên tiếp! Thời gian còn lại được nhân đôi.", "Thưởng", JOptionPane.INFORMATION_MESSAGE);
        }
        soundPlayer.playMatchSound();
        GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
        if (hit == m * n / 2) {
            timer.stop();
            timer2.stop();
            if (level + 1 < levelConfig.getLevelCount()) {
                controller.nextGame(level + 1, Integer.parseInt(scoreButton.getText()) + (maxTime - time) / 50);
            } else {
                showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + scoreButton.getText() + "\nChơi lại?", "Thông báo");
            }
        }
    } else {
        bt.get(preX).get(preY).setIcon(getIcon(0));
        bt.get(X).get(Y).setIcon(getIcon(0));
        clicked[preX][preY] = false;
        clicked[X][Y] = false;
        scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) - 10));
        consecutiveMatches = 0; // Đặt lại nếu ghép sai
        GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
    }
}

   /* private void useHint() {
        if (hintsUsed >= MAX_HINTS) {
            JOptionPane.showMessageDialog(this, "Bạn đã sử dụng hết số lần gợi ý!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (tick[i][j]) {
                    for (int k = 0; k < m; k++) {
                        for (int l = 0; l < n; l++) {
                            if (tick[k][l] && a[i][j] == a[k][l] && (i != k || j != l)) {
                                pairs.add(new int[]{i, j, k, l});
                            }
                        }
                    }
                }
            }
        }

        if (!pairs.isEmpty()) {
            int[] pair = pairs.get(0);
            boolean includesPreSelected = (count == 1 && ((pair[0] == preX && pair[1] == preY) || (pair[2] == preX && pair[3] == preY)));
            bt.get(pair[0]).get(pair[1]).setIcon(getIcon(a[pair[0]][pair[1]]));
            bt.get(pair[2]).get(pair[3]).setIcon(getIcon(a[pair[2]][pair[3]]));
            hintTimer = new Timer(1000, e -> {
                bt.get(pair[0]).get(pair[1]).setIcon(getIcon(-1));
                bt.get(pair[2]).get(pair[3]).setIcon(getIcon(-1));
                a[pair[0]][pair[1]] = 0;
                a[pair[2]][pair[3]] = 0;
                tick[pair[0]][pair[1]] = false;
                tick[pair[2]][pair[3]] = false;
                clicked[pair[0]][pair[1]] = true;
                clicked[pair[2]][pair[3]] = true;
                if (includesPreSelected) {
                    count = 0;
                }
                hit++;
                soundPlayer.playMatchSound();
                GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
                if (hit == m * n / 2) {
                    timer.stop();
                    timer2.stop();
                    if (level + 1 < levelConfig.getLevelCount()) {
                        controller.nextGame(level + 1, Integer.parseInt(scoreButton.getText()) + (maxTime - time) / 50);
                    } else {
                        showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + scoreButton.getText() + "\nChơi lại?", "Thông báo");
                    }
                }
                hintTimer.stop();
            });
            hintsUsed++;
            hintButton.setText("Hint (" + (MAX_HINTS - hintsUsed) + ")");
            scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) - 50));
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n);
            hintTimer.start();
            soundPlayer.playMatchSound();
        }
    }*/
    private void useHint() {
    if (hintsUsed >= MAX_HINTS) {
        JOptionPane.showMessageDialog(this, "Bạn đã sử dụng hết số lần gợi ý!", "Thông báo", JOptionPane.WARNING_MESSAGE);
        return;
    }

    List<int[]> pairs = new ArrayList<>();
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (tick[i][j]) {
                for (int k = 0; k < m; k++) {
                    for (int l = 0; l < n; l++) {
                        if (tick[k][l] && a[i][j] == a[k][l] && (i != k || j != l)) {
                            pairs.add(new int[]{i, j, k, l});
                        }
                    }
                }
            }
        }
    }

    if (!pairs.isEmpty()) {
        int[] pair = pairs.get(0);
        boolean includesPreSelected = (count == 1 && ((pair[0] == preX && pair[1] == preY) || (pair[2] == preX && pair[3] == preY)));
        bt.get(pair[0]).get(pair[1]).setIcon(getIcon(a[pair[0]][pair[1]]));
        bt.get(pair[2]).get(pair[3]).setIcon(getIcon(a[pair[2]][pair[3]]));
        hintTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bt.get(pair[0]).get(pair[1]).setIcon(getIcon(-1));
                bt.get(pair[2]).get(pair[3]).setIcon(getIcon(-1));
                a[pair[0]][pair[1]] = 0;
                a[pair[2]][pair[3]] = 0;
                tick[pair[0]][pair[1]] = false;
                tick[pair[2]][pair[3]] = false;
                clicked[pair[0]][pair[1]] = true;
                clicked[pair[2]][pair[3]] = true;
                if (includesPreSelected) {
                    count = 0;
                }
                hit++;
                consecutiveMatches++; // Tăng số lần ghép đúng liên tiếp
                if (consecutiveMatches >= 2) {
                    int remainingTime = maxTime - time;
                    int newRemainingTime = remainingTime * 2;
                    maxTime = time + newRemainingTime;
                    progressTime.setMaximum(maxTime);
                    progressTime.setValue(maxTime - time);
                    consecutiveMatches = 0; // Đặt lại sau khi nhân đôi
                    JOptionPane.showMessageDialog(GamePanel.this, "Ghép đúng 2 lần liên tiếp! Thời gian còn lại được nhân đôi.", "Thưởng", JOptionPane.INFORMATION_MESSAGE);
                }
                soundPlayer.playMatchSound();
                GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
                if (hit >= m * n / 2) {
                    timer.stop();
                    timer2.stop();
                    if (level + 1 < levelConfig.getLevelCount()) {
                        controller.nextGame(level + 1, Integer.parseInt(scoreButton.getText()) + (maxTime - time) / 50);
                    } else {
                        showDialogNewGame("Chúc mừng! Bạn đã hoàn thành tất cả cấp độ.\nĐiểm: " + scoreButton.getText() + "\nChơi lại?", "Thông báo");
                    }
                }
                ((Timer)e.getSource()).stop(); // Dừng timer sau khi thực thi
            }
        });
        hintsUsed++;
        hintButton.setText("Hint (" + (MAX_HINTS - hintsUsed) + ")");
        scoreButton.setText(String.valueOf(Integer.parseInt(scoreButton.getText()) - 50));
        GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
        hintTimer.setRepeats(false); // Đảm bảo timer chỉ chạy một lần
        hintTimer.start();
        soundPlayer.playMatchSound();
    }
}

    private void showPausePanel() {
    	timer2.stop();

        GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);

        if (!layeredPane.isVisible()) {
            removeAll();
            setLayout(new BorderLayout());
            add(layeredPane, BorderLayout.CENTER);
            layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
            layeredPane.setVisible(true);
        }

        pausePanel = new JPanel() {
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
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        pausePanel.setLayout(new GridBagLayout());
        pausePanel.setBounds(0, 0, getWidth(), getHeight());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Paused", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton resumeButton = new RoundedButton("Resume", 10);
        resumeButton.setFont(new Font("Arial", Font.BOLD, 30));
        resumeButton.setPreferredSize(new Dimension(200, 60));
        resumeButton.setBackground(Color.GREEN);
        resumeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resumeButton.addActionListener(e -> {
            layeredPane.remove(pausePanel);
            layeredPane.setVisible(false);
            setLayout(new BorderLayout());
            add(mainPanel, BorderLayout.CENTER);
            timer2.start();
            revalidate();
            repaint();
        });

        JButton restartButton = new RoundedButton("Restart", 10);
        restartButton.setFont(new Font("Arial", Font.BOLD, 30));
        restartButton.setPreferredSize(new Dimension(200, 60));
        restartButton.setBackground(Color.ORANGE);
        restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        restartButton.addActionListener(e -> {
            GameState.clearState();
            controller.startNewGame();
        });

        JButton quitLevelButton = new RoundedButton("Quit Level", 10);
        quitLevelButton.setFont(new Font("Arial", Font.BOLD, 30));
        quitLevelButton.setPreferredSize(new Dimension(200, 60));
        quitLevelButton.setBackground(Color.GRAY);
        quitLevelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        quitLevelButton.addActionListener(e -> {
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
            controller.showMenu();
        });

        JButton menuButton = new RoundedButton("Back", 10);
        menuButton.setFont(new Font("Arial", Font.BOLD, 30));
        menuButton.setPreferredSize(new Dimension(200, 60));
        menuButton.setBackground(Color.RED);
        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuButton.addActionListener(e -> {
            GameState.saveState(level, Integer.parseInt(scoreButton.getText()), hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
            controller.showMenu();
        });

        buttonPanel.add(titleLabel);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(resumeButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(restartButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(quitLevelButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(menuButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pausePanel.add(buttonPanel, gbc);

        layeredPane.add(pausePanel, JLayeredPane.POPUP_LAYER);
        revalidate();
        repaint();
    }

    private void showDialogNewGame(String message, String title) {
        int select = JOptionPane.showOptionDialog(null, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (select == 0) {
            GameState.clearState();
            controller.startNewGame();
        } else {
            controller.exitGame();
        }
    }

    private void showDialogGameOver(String message, String title) {
        int select = JOptionPane.showOptionDialog(null, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (select == 0) {
            GameState.clearState();
            controller.startNewGame();
        } else {
            controller.exitGame();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        int k = s.indexOf(" ");
        int i = Integer.parseInt(s.substring(0, k));
        int j = Integer.parseInt(s.substring(k + 1));

        if (tick[i][j] && !clicked[i][j] && !isProcessing) {
            if (count == 1 && preX == i && preY == j) {
                return;
            }

            clicked[i][j] = true;
            bt.get(i).get(j).setIcon(getIcon(a[i][j]));
            if (count == 0) {
                id = a[i][j];
                preX = i;
                preY = j;
                count = 1;
            } else {
                X = i;
                Y = j;
                isProcessing = true;
                if (timer != null) timer.start();
                count = 0;
            }
        }
    }

    // Xử lý sự kiện phím
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !isProcessing) {
            useHint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Không cần xử lý
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Không cần xử lý
    }

    private static class RoundedButton extends JButton {
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

    public int getLevel() { return level; }
    public int getScore() { return Integer.parseInt(scoreButton.getText()); }
    public int getHintsUsed() { return hintsUsed; }
    public int[][] getMatrixA() { return a; }
    public boolean[][] getTick() { return tick; }
    public int getTime() { return time; }
    public int getMaxTime() { return maxTime; }
    public int getPreX() { return preX; }
    public int getPreY() { return preY; }
    public int getId() { return id; }
    public int getCount() { return count; }
    public int getHit() { return hit; }
    public int getM() { return m; }
    public int getN() { return n; }
    public boolean[][] getClicked() { return clicked; }
}