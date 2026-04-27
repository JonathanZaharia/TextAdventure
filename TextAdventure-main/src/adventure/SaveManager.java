package adventure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveManager {

    private static final String SAVE_FILE = "SaveData.txt";

    public static boolean hasSaveFile() {
        return new File(SAVE_FILE).exists();
    }

    public static boolean saveGame(Player player, Puzzle[] puzzles, Monster[] monsters, List<Item> allItems) {
        try (PrintWriter out = new PrintWriter(new FileWriter(SAVE_FILE))) {
            out.println("ROOM=" + player.getCurrentRoomNumber());
            out.println("HP=" + player.getCurrentHealth());

            if (player.getEquippedItem() != null) {
                out.println("EQUIPPED=" + player.getEquippedItem().getName());
            } else {
                out.println("EQUIPPED=");
            }

            for (Item item : allItems) {
                out.println("ITEM=" + item.getName() + "|" + item.getCurrentLocation());
            }

            for (Puzzle puzzle : puzzles) {
                out.println("PUZZLE=" + puzzle.getRoomNumber() + "|" + puzzle.isSolved());
            }

            for (Monster monster : monsters) {
                if (monster.isDead()) {
                    out.println("MONSTER=" + monster.getRoomNumber() + "|DEAD");
                } else if (monster.isIgnored()) {
                    out.println("MONSTER=" + monster.getRoomNumber() + "|IGNORED");
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean loadGame(Player player, Map<Integer, Room> rooms,
            Puzzle[] puzzles, Monster[] monsters, List<Item> allItems) {

        File saveFile = new File(SAVE_FILE);
        if (!saveFile.exists()) {
            return false;
        }

        int savedRoom = player.getCurrentRoomNumber();
        int savedHp = player.getMaxHealth();
        String equippedName = "";

        Map<String, Integer> itemLocations = new HashMap<>();
        Map<Integer, Boolean> solvedPuzzles = new HashMap<>();
        Map<Integer, String> monsterStates = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("ROOM=")) {
                    savedRoom = Integer.parseInt(line.substring(5).trim());
                } else if (line.startsWith("HP=")) {
                    savedHp = Integer.parseInt(line.substring(3).trim());
                } else if (line.startsWith("EQUIPPED=")) {
                    equippedName = line.substring(9).trim();
                } else if (line.startsWith("ITEM=")) {
                    String[] parts = line.substring(5).split("\\|");
                    if (parts.length == 2) {
                        itemLocations.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    }
                } else if (line.startsWith("PUZZLE=")) {
                    String[] parts = line.substring(7).split("\\|");
                    if (parts.length == 2) {
                        solvedPuzzles.put(Integer.parseInt(parts[0].trim()), Boolean.parseBoolean(parts[1].trim()));
                    }
                } else if (line.startsWith("MONSTER=")) {
                    String[] parts = line.substring(8).split("\\|");
                    if (parts.length == 2) {
                        monsterStates.put(Integer.parseInt(parts[0].trim()), parts[1].trim());
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }

        if (rooms.containsKey(savedRoom)) {
            player.setCurrentRoomNumber(savedRoom);
        }

        int clampedHp = Math.max(0, Math.min(player.getMaxHealth(), savedHp));
        player.resetHealth();
        if (clampedHp < player.getMaxHealth()) {
            player.takeDamage(player.getMaxHealth() - clampedHp);
        }

        for (Puzzle puzzle : puzzles) {
            puzzle.reset();
            Boolean solved = solvedPuzzles.get(puzzle.getRoomNumber());
            if (Boolean.TRUE.equals(solved)) {
                puzzle.attemptSolve(puzzle.getCorrectAnswer());
            }
        }

        for (Monster monster : monsters) {
            monster.reset();
            String state = monsterStates.get(monster.getRoomNumber());
            if (state == null) {
                continue;
            }

            if (state.equalsIgnoreCase("DEAD")) {
                monster.takeDamage(monster.getCurrentHealth());
            } else if (state.equalsIgnoreCase("IGNORED")) {
                monster.setIgnored();
            }
        }

        for (Item item : allItems) {
            Integer location = itemLocations.get(item.getName());
            if (location != null) {
                item.setCurrentLocation(location);
            }
        }

        rebuildItems(player, rooms, allItems);

        if (!equippedName.isEmpty()) {
            Item item = player.getItemByName(equippedName);
            if (item != null) {
                player.equipItem(item);
            }
        }

        return true;
    }

    private static void rebuildItems(Player player, Map<Integer, Room> rooms, List<Item> allItems) {
        player.clearInventory();

        for (Room room : rooms.values()) {
            room.clearItems();
        }

        for (Item item : allItems) {
            int location = item.getCurrentLocation();
            if (location == 0) {
                player.addItem(item);
            } else {
                Room room = rooms.get(location);
                if (room != null) {
                    room.addItem(item);
                }
            }
        }
    }

    public static void deleteSave() {
        File saveFile = new File(SAVE_FILE);
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }
}