import java.io.*;
import java.util.*;

// let me know if anything confused you team!

public class SaveManager {

    private static final String SAVE_FILE = "SaveData.txt";

    public static void saveGame(Player player, Puzzle[] puzzles, Monster[] monsters, List<Item> allItems) {
        try (PrintWriter out = new PrintWriter(new FileWriter(SAVE_FILE))) { //  Saves the game into saveData.txt


            out.println("ROOM=" + player.getCurrentRoomNumber()); // Player save data
            out.println("HP=" + player.getCurrentHealth());

            if (player.getEquippedItem() != null) {
                out.println("EQUIPPED=" + player.getEquippedItem().getName());
            } else {
                out.println("EQUIPPED=");
            }

            // Item save data

            for (Item item : allItems) {
                out.println("ITEM=" + item.getName() + "|" + item.getCurrentLocation());    // 0 = inventory anything above 0 = room number 
            }

            // Puzzle save data
            for (Puzzle puzzle : puzzles) {
                out.println("PUZZLE=" + puzzle.getRoomNumber() + "|" + puzzle.isSolved());
            }

            // Monster save data

            for (Monster monster : monsters) { // saves monster alive or dead status
                if (monster.isDead()) {
                    out.println("MONSTER=" + monster.getRoomNumber() + "|DEAD");
                } else if (monster.isIgnored()) {
                    out.println("MONSTER=" + monster.getRoomNumber() + "|IGNORED");
                }
            }

        } catch (IOException e) {
            System.out.println("Could not save game.");
        }
    }

    // LOADS GAME HERE!
    public static void loadGame(Player player, Map<Integer, Room> rooms,
                                Puzzle[] puzzles, Monster[] monsters, List<Item> allItems) {

        File saveFile = new File(SAVE_FILE);

        if (!saveFile.exists()) {         // If no save exist just start normally

            return;
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
                }
                else if (line.startsWith("HP=")) {
                    savedHp = Integer.parseInt(line.substring(3).trim());
                }
                else if (line.startsWith("EQUIPPED=")) {
                    equippedName = line.substring(9).trim();
                }
                else if (line.startsWith("ITEM=")) {
                    String data = line.substring(5);
                    String[] parts = data.split("\\|");

                    if (parts.length == 2) {
                        String itemName = parts[0].trim();
                        int location = Integer.parseInt(parts[1].trim());
                        itemLocations.put(itemName, location);
                    }
                }
                else if (line.startsWith("PUZZLE=")) {
                    String data = line.substring(7);
                    String[] parts = data.split("\\|");

                    if (parts.length == 2) {
                        int roomNumber = Integer.parseInt(parts[0].trim());
                        boolean solved = Boolean.parseBoolean(parts[1].trim());
                        solvedPuzzles.put(roomNumber, solved);
                    }
                }
                else if (line.startsWith("MONSTER=")) {
                    String data = line.substring(8);
                    String[] parts = data.split("\\|");

                    if (parts.length == 2) {
                        int roomNumber = Integer.parseInt(parts[0].trim());
                        String state = parts[1].trim();
                        monsterStates.put(roomNumber, state);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Could not load save file.");
            return;
        }


        player.setCurrentRoomNumber(savedRoom); // add player data
        player.setCurrentHealth(savedHp);


        for (Puzzle puzzle : puzzles) { // Apply puzzle data
            Boolean solved = solvedPuzzles.get(puzzle.getRoomNumber());

            if (solved != null && solved) {
                puzzle.markSolvedFromSave();
            }
        }


        for (Monster monster : monsters) {  // Apply monster data
            String state = monsterStates.get(monster.getRoomNumber());

            if (state != null) {
                if (state.equalsIgnoreCase("DEAD")) {
                    monster.killFromSave();
                } else if (state.equalsIgnoreCase("IGNORED")) {
                    monster.setIgnored(true);
                }
            }
        }


        for (Item item : allItems) { // Apply item locations for drops and all in case I forget
            Integer location = itemLocations.get(item.getName());

            if (location != null) {
                item.setCurrentLocation(location);
            }
        }


        rebuildItems(player, rooms, allItems); // clears items for saved game

        if (!equippedName.isEmpty()) {         // item in hand if saved weapon is still in inventory

            Item item = player.getItemByName(equippedName);

            if (item != null) {
                player.equipItem(item);
            }
        }

        System.out.println("Saved game loaded.");
    }


    private static void rebuildItems(Player player, Map<Integer, Room> rooms, List<Item> allItems) {
        // Puts items back into inventory/rooms after loading

        player.clearInventory();     // Clear inventory first fix if i forgot!


        for (Room room : rooms.values()) { // Clear all room item lists
            room.getItems().clear();
        }


        for (Item item : allItems) {      // Put items to currentLocation 
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


    public static void deleteSave() { // Deletes SaveData.txt basically restarts player on death!!
        File saveFile = new File(SAVE_FILE);

        if (saveFile.exists()) {
            saveFile.delete();
        }
    }
}