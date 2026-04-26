package adventure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDataLoader {

    private GameDataLoader() {
        // Utility class
    }

    public static boolean loadRooms(String fileName, Map<Integer, Room> rooms) {
        File f = resolveDataFile(fileName);

        if (!f.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);

                if (parts[0].trim().equalsIgnoreCase("LOCK")) {
                    if (parts.length >= 3) {
                        try {
                            Room room = rooms.get(Integer.parseInt(parts[1].trim()));

                            if (room != null) {
                                room.setLocked(parts[2].trim());
                            }
                        } catch (NumberFormatException e) {
                            // Ignore malformed lock rows.
                        }
                    }

                    continue;
                }

                if (parts.length < 7) {
                    continue;
                }

                try {
                    int number = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String desc = parts[2].trim();

                    Room room = new Room(number, name, desc);
                    room.addExit("N", Integer.parseInt(parts[3].trim()));
                    room.addExit("E", Integer.parseInt(parts[4].trim()));
                    room.addExit("S", Integer.parseInt(parts[5].trim()));
                    room.addExit("W", Integer.parseInt(parts[6].trim()));

                    rooms.put(number, room);
                } catch (NumberFormatException e) {
                    // Ignore malformed room rows.
                }
            }
        } catch (IOException e) {
            return false;
        }

        return !rooms.isEmpty();
    }

    public static void loadItems(String fileName, Map<Integer, Room> rooms, List<Item> allItems) {
        File f = resolveDataFile(fileName);

        if (!f.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);

                if (parts.length < 6) {
                    continue;
                }

                try {
                    String name = parts[0].trim();
                    String description = parts[1].trim();
                    String type = parts[2].trim();
                    int attackBonus = Integer.parseInt(parts[3].trim());
                    int healAmount = Integer.parseInt(parts[4].trim());
                    int roomNumber = Integer.parseInt(parts[5].trim());

                    Item item = new Item(name, description, type, attackBonus, healAmount, roomNumber);
                    allItems.add(item);

                    if (roomNumber > 0 && rooms.containsKey(roomNumber)) {
                        rooms.get(roomNumber).addItem(item);
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed item rows.
                }
            }
        } catch (IOException e) {
            // Keep startup resilient by ignoring malformed item file reads.
        }
    }

    public static Puzzle[] loadPuzzles(String fileName) {
        File f = resolveDataFile(fileName);

        if (!f.exists()) {
            return new Puzzle[0];
        }

        Map<Integer, Puzzle> puzzleMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);

                if (parts.length < 7) {
                    continue;
                }

                try {
                    int roomNumber = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String description = parts[2].trim();
                    String answer = parts[3].trim();
                    String successMsg = parts[4].trim();
                    String rewardItem = parts[5].trim();
                    int attempts = Integer.parseInt(parts[6].trim());

                    puzzleMap.put(roomNumber,
                            new Puzzle(name, description, answer, successMsg, rewardItem, attempts, roomNumber));
                } catch (NumberFormatException e) {
                    // Ignore malformed puzzle rows.
                }
            }
        } catch (IOException e) {
            return new Puzzle[0];
        }

        return puzzleMap.values().toArray(Puzzle[]::new);
    }

    public static Monster[] loadMonsters(String fileName) {
        File f = resolveDataFile(fileName);

        if (!f.exists()) {
            return new Monster[0];
        }

        List<Monster> list = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);

                if (parts.length < 7) {
                    continue;
                }

                try {
                    int roomNumber = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String desc = parts[2].trim();
                    int health = Integer.parseInt(parts[3].trim());
                    int attack = Integer.parseInt(parts[4].trim());
                    double threshold = Double.parseDouble(parts[5].trim());
                    String dropItem = parts[6].trim();

                    list.add(new Monster(name, desc, health, attack, threshold, dropItem, roomNumber));
                } catch (NumberFormatException e) {
                    // Ignore malformed monster rows.
                }
            }
        } catch (IOException e) {
            return new Monster[0];
        }

        return list.toArray(Monster[]::new);
    }

    public static String[] loadObjectives(String fileName) {
        File f = resolveDataFile(fileName);

        if (!f.exists()) {
            return new String[0];
        }

        List<String> objectives = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                objectives.add(line);
            }
        } catch (IOException e) {
            return new String[0];
        }

        return objectives.toArray(String[]::new);
    }

    private static File resolveDataFile(String fileName) {
        File directFile = new File(fileName);

        if (directFile.exists()) {
            return directFile;
        }

        File projectFile = new File("TextAdventure-main", fileName);

        if (projectFile.exists()) {
            return projectFile;
        }

        File current = new File(System.getProperty("user.dir"));

        while (current != null) {
            File candidate = new File(current, fileName);

            if (candidate.exists()) {
                return candidate;
            }

            File nestedProjectFile = new File(current, "TextAdventure-main" + File.separator + fileName);

            if (nestedProjectFile.exists()) {
                return nestedProjectFile;
            }

            current = current.getParentFile();
        }

        return directFile;
    }
}
