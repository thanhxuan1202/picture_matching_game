package model;

import java.io.*;

public class GameState {
    private int level;
    private int score;
    private int hintsUsed;
    private int[][] a;
    private boolean[][] tick;
    private boolean[][] clicked;
    private int time;
    private int maxTime;
    private int preX, preY, id, count, hit;
    private int m, n;
    private final int consecutiveMatches;

    public GameState(int level, int score, int hintsUsed, int[][] a, boolean[][] tick, boolean[][] clicked, int time, int maxTime, int preX, int preY, int id, int count, int hit, int m, int n, int consecutiveMatches) {
        this.level = level;
        this.score = score;
        this.hintsUsed = hintsUsed;
        this.a = a;
        this.tick = tick;
        this.clicked = clicked;
        this.time = time;
        this.maxTime = maxTime;
        this.preX = preX;
        this.preY = preY;
        this.id = id;
        this.count = count;
        this.hit = hit;
        this.m = m;
        this.n = n;
        this.consecutiveMatches = consecutiveMatches;
    }

    // Các getter
    public int getLevel() { return level; }
    public int getScore() { return score; }
    public int getHintsUsed() { return hintsUsed; }
    public int[][] getMatrixA() { return a; }
    public boolean[][] getTick() { return tick; }
    public boolean[][] getClicked() { return clicked; }
    public int getTime() { return time; }
    public int getMaxTime() { return maxTime; }
    public int getPreX() { return preX; }
    public int getPreY() { return preY; }
    public int getId() { return id; }
    public int getCount() { return count; }
    public int getHit() { return hit; }
    public int getM() { return m; }
    public int getN() { return n; }
    public int getConsecutiveMatches() { return consecutiveMatches; }

    // Lưu trạng thái trò chơi vào tệp
    public static void saveState(int level, int score, int hintsUsed, int[][] a, boolean[][] tick, boolean[][] clicked, int time, int maxTime, int preX, int preY, int id, int count, int hit, int m, int n, int consecutiveMatches) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resources/Game_data/game_state.txt"))) {
            writer.write(String.valueOf(level));
            writer.newLine();
            writer.write(String.valueOf(score));
            writer.newLine();
            writer.write(String.valueOf(hintsUsed));
            writer.newLine();
            writer.write(m + " " + n);
            writer.newLine();
            writer.write(String.valueOf(time));
            writer.newLine();
            writer.write(String.valueOf(maxTime));
            writer.newLine();
            writer.write(preX + " " + preY + " " + id + " " + count + " " + hit);
            writer.newLine();
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    writer.write(a[i][j] + " ");
                }
                writer.newLine();
            }
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    writer.write((tick[i][j] ? "1" : "0") + " ");
                }
                writer.newLine();
            }
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    writer.write((clicked[i][j] ? "1" : "0") + " ");
                }
                writer.newLine();
            }
            writer.write(String.valueOf(consecutiveMatches));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Tải trạng thái trò chơi từ tệp
    public static GameState loadState() {
        File file = new File("resources/Game_data/game_state.txt");
        if (!file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int level = Integer.parseInt(reader.readLine());
            int score = Integer.parseInt(reader.readLine());
            int hintsUsed = Integer.parseInt(reader.readLine());
            String[] dimensions = reader.readLine().split(" ");
            int m = Integer.parseInt(dimensions[0]);
            int n = Integer.parseInt(dimensions[1]);
            int time = Integer.parseInt(reader.readLine());
            int maxTime = Integer.parseInt(reader.readLine());
            String[] params = reader.readLine().split(" ");
            int preX = Integer.parseInt(params[0]);
            int preY = Integer.parseInt(params[1]);
            int id = Integer.parseInt(params[2]);
            int count = Integer.parseInt(params[3]);
            int hit = Integer.parseInt(params[4]);

            int[][] a = new int[m][n];
            for (int i = 0; i < m; i++) {
                String[] row = reader.readLine().split(" ");
                for (int j = 0; j < n; j++) {
                    a[i][j] = Integer.parseInt(row[j]);
                }
            }

            boolean[][] tick = new boolean[m][n];
            for (int i = 0; i < m; i++) {
                String[] row = reader.readLine().split(" ");
                for (int j = 0; j < n; j++) {
                    tick[i][j] = row[j].equals("1");
                }
            }

            boolean[][] clicked = new boolean[m][n];
            for (int i = 0; i < m; i++) {
                String[] row = reader.readLine().split(" ");
                for (int j = 0; j < n; j++) {
                    clicked[i][j] = row[j].equals("1");
                }
            }

            int consecutiveMatches = Integer.parseInt(reader.readLine());

            return new GameState(level, score, hintsUsed, a, tick, clicked, time, maxTime, preX, preY, id, count, hit, m, n, consecutiveMatches);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Lỗi khi tải trạng thái: " + e.getMessage());
            return null;
        }
    }

    // Xóa trạng thái trò chơi
    public static void clearState() {
        File file = new File("resources/Game_data/game_state.txt");
        if (file.exists()) {
            file.delete();
        }
    }
}