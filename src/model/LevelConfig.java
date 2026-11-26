package model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LevelConfig {
    private List<Integer> sizeX = new ArrayList<>();
    private List<Integer> sizeY = new ArrayList<>();
    private List<Integer> time = new ArrayList<>();

    public LevelConfig() {
        loadLevels();
    }

    private void loadLevels() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/Game_data/levels.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    sizeX.add(Integer.parseInt(parts[0]));
                    sizeY.add(Integer.parseInt(parts[1]));
                    time.add(Integer.parseInt(parts[2]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getSizeX(int level) {
        return level < sizeX.size() ? sizeX.get(level) : 2;
    }

    public int getSizeY(int level) {
        return level < sizeY.size() ? sizeY.get(level) : 3;
    }

    public int getTime(int level) {
        return level < time.size() ? time.get(level) : 10;
    }

    public int getLevelCount() {
        return sizeX.size();
    }
}