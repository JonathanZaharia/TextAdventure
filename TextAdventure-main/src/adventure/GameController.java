package adventure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GameController {

    private static final String ROOMS_FILE = "Rooms.txt";
    private static final String ITEMS_FILE = "Item.txt";
    private static final String PUZZLE_FILE = "Puzzle.txt";
    private static final String MONSTERS_FILE = "Monsters.txt";

    // Player starting values
    private static final int PLAYER_START_ROOM = 1;
    private static final int PLAYER_MAX_HEALTH = 20;
    private static final int PLAYER_BASE_DAMAGE = 2;

    public static void main(String[] args) {
        Map<Integer, Room> rooms = new HashMap<>();
        List<Item> allItems = new ArrayList<>();

        if (!loadRooms(ROOMS_FILE, rooms)) {
            GameView.printLine("ERROR: Could not load room data from " + ROOMS_FILE);
            return;
        }

        loadItems(ITEMS_FILE, rooms, allItems);
        Puzzle[] puzzles = loadPuzzles(PUZZLE_FILE);
        Monster[] monsters = loadMonsters(MONSTERS_FILE);

        int startRoom = rooms.containsKey(PLAYER_START_ROOM)
                ? PLAYER_START_ROOM
                : rooms.keySet().stream().min(Integer::compareTo).orElseThrow();

        Player player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);

        try (Scanner input = new Scanner(System.in)) {
            GameView.printWelcome();

            boolean running = true;
            boolean justEnteredRoom = true;
            int previousRoom = -1;

            while (running) {
                int currentRoomNumber = player.getCurrentRoomNumber();
                Room current = rooms.get(currentRoomNumber);

                // When leaving a room, reset puzzle visit state for the room just left
                if (previousRoom != -1 && previousRoom != currentRoomNumber) {
                    Puzzle prev = Puzzle.findByRoomNumber(puzzles, previousRoom);
                    if (prev != null)
                        prev.clearFailedForThisVisit();
                }
                previousRoom = currentRoomNumber;

                if (justEnteredRoom) {
                    // Monster encounter on entry
                    Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                    if (monster != null) {
                        boolean survived = handleMonsterEncounter(monster, allItems, player, current, input);
                        if (!survived) {
                            GameView.showGameOverMenu();
                            running = promptGameOverChoice(input);
                            if (running) {
                                // Restart — reset player and all game state
                                player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);
                                for (Puzzle p : puzzles)
                                    p.reset();
                                for (Room r : rooms.values())
                                    r.setVisited(false);
                            }
                            justEnteredRoom = true;
                            continue;
                        }
                    }

                    // Puzzle check on entry
                    Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);
                    if (roomPuzzle != null && !roomPuzzle.isSolved() && !roomPuzzle.isFailedForThisVisit()) {
                        boolean solved = roomPuzzle.attemptPuzzle(allItems, player, input);
                        if (!solved)
                            roomPuzzle.setFailedForThisVisit();
                    }

                    GameView.displayRoom(current, puzzles, monsters);
                    current.markVisited();
                    justEnteredRoom = false;
                }

                GameView.print("\nCommand: ");
                String inputLine = input.nextLine().trim();
                if (inputLine.isEmpty())
                    continue;

                String[] parts = inputLine.split(" ", 2);
                String cmd = parts[0].toUpperCase();
                String argument = parts.length > 1 ? parts[1].trim() : "";

                switch (cmd) {

                    case "N", "E", "S", "W", "NORTH", "SOUTH", "EAST", "WEST" -> {
                        String dir = cmd.substring(0, 1);
                        int exitDest = current.getExit(dir);

                        if (exitDest <= 0 || !rooms.containsKey(exitDest)) {
                            GameView.printLine("You cannot go that way.");
                        } else {
                            Room dest = rooms.get(exitDest);
                            if (dest.isLocked()) {
                                if (dest.canPlayerEnter(player)) {
                                    String keyName = dest.getRequiredItemName();
                                    dest.unlock();
                                    GameView.printLine("You use the " + keyName + " to unlock the door.");
                                    player.setCurrentRoomNumber(exitDest);
                                    justEnteredRoom = true;
                                } else {
                                    GameView.printLine("This room is locked. You need: " + dest.getRequiredItemName());
                                }
                            } else {
                                player.setCurrentRoomNumber(exitDest);
                                justEnteredRoom = true;
                            }
                        }
                    }

                    case "EXPLORE", "LOOK" -> {
                        if (!current.hasItems()) {
                            GameView.printLine("There are no items in this room.");
                        } else {
                            GameView.printLine("Items in this room:");
                            for (Item item : current.getItems()) {
                                GameView.printLine("  - " + item.getName());
                            }
                        }
                    }

                    case "PICKUP", "TAKE", "GET" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify an item name. Example: TAKE Keycard");
                        } else {
                            player.pickupItem(current, argument);
                        }
                    }

                    case "INSPECT", "EXAMINE" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine(current.getDescription());
                        } else {
                            GameView.handleInspect(player, current, monsters, argument);
                        }
                    }

                    case "DROP" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify an item name. Example: DROP Keycard");
                        } else {
                            player.dropItem(current, argument);
                        }
                    }

                    case "EQUIP" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify a weapon. Example: EQUIP Rusty Knife");
                        } else {
                            player.equipWeapon(argument);
                        }
                    }

                    case "UNEQUIP" -> player.unequipWeapon();

                    case "HEAL", "CONSUME" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify an item. Example: HEAL Med Kit");
                        } else {
                            player.consumeHealingItem(argument);
                        }
                    }

                    case "ATTACK" -> {
                        Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                        if (monster == null) {
                            GameView.printLine("There is no monster here.");
                        } else {
                            boolean survived = handleCombat(monster, allItems, player, current, input);
                            if (!survived) {
                                GameView.showGameOverMenu();
                                running = promptGameOverChoice(input);
                                if (running) {
                                    player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);
                                    for (Puzzle p : puzzles)
                                        p.reset();
                                    for (Room r : rooms.values())
                                        r.setVisited(false);
                                }
                                justEnteredRoom = true;
                            }
                        }
                    }

                    case "IGNORE" -> {
                        Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                        if (monster == null) {
                            GameView.printLine("There is no monster here.");
                        } else {
                            monster.setIgnored();
                            GameView.printLine("You back away. The " + monster.getName()
                                    + " lets you go and will not appear again.");
                        }
                    }

                    case "SOLVE" -> {
                        Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);
                        if (roomPuzzle == null) {
                            GameView.printLine("There is no puzzle here.");
                        } else if (roomPuzzle.isSolved()) {
                            GameView.printLine("This puzzle has already been solved.");
                        } else if (roomPuzzle.isFailedForThisVisit()) {
                            GameView.printLine("You have no attempts remaining for this puzzle right now.");
                        } else {
                            boolean solved = roomPuzzle.attemptPuzzle(allItems, player, input);
                            if (!solved)
                                roomPuzzle.setFailedForThisVisit();
                        }
                    }

                    case "INVENTORY", "INV", "I" -> GameView.displayInventory(player);

                    case "HEALTH", "HP" ->
                        GameView.printLine("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth());

                    case "HELP" -> GameView.printHelp();

                    case "Q", "QUIT", "EXIT" -> {
                        running = false;
                        GameView.printLine("Thanks for playing!");
                    }

                    default -> GameView.printLine("Unknown command. Type HELP for commands.");
                }
            }
        }
    }

    private static boolean handleMonsterEncounter(Monster monster, List<Item> allItems, Player player, Room room,
            Scanner input) {
        GameView.printLine("\n! A " + monster.getName() + " is here !");
        GameView.printLine(monster.getDescription());
        GameView.printLine("Type ATTACK to fight or IGNORE to back away.");

        while (true) {
            GameView.print("Choice: ");
            String choice = input.nextLine().trim().toUpperCase();

            if (choice.equals("ATTACK")) {
                return handleCombat(monster, allItems, player, room, input);
            }

            if (choice.equals("IGNORE")) {
                monster.setIgnored();
                GameView.printLine("You back away. The " + monster.getName() + " will not appear again.");
                return true;
            }

            GameView.printLine("Type ATTACK or IGNORE.");
        }
    }

    private static boolean handleCombat(Monster monster, List<Item> allItems, Player player, Room room,
            Scanner input) {
        GameView.printLine("\n--- COMBAT: " + monster.getName() + " ---");

        while (!monster.isDead() && !player.isDead()) {
            GameView.printLine("\nYour HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                    + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());
            GameView.print("Action (ATTACK / HEAL [item] / EQUIP [item] / UNEQUIP / INVENTORY): ");

            String line = input.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(" ", 2);
            String action = parts[0].toUpperCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";

            switch (action) {
                case "ATTACK" -> {
                    int damage = player.getAttackDamage();
                    monster.takeDamage(damage);
                    GameView.printLine("You deal " + damage + " damage. " + monster.getName() + " HP: "
                            + monster.getCurrentHealth());

                    if (monster.isDead()) {
                        GameView.printLine("You defeated the " + monster.getName() + "!");
                        monster.dropLoot(allItems, room);
                        return true;
                    }

                    monster.monsterAttack(player);
                }
                case "HEAL" -> {
                    player.consumeHealingItem(arg);
                    if (!player.isDead()) {
                        monster.monsterAttack(player);
                    }
                }
                case "EQUIP" -> player.equipWeapon(arg);
                case "UNEQUIP" -> player.unequipWeapon();
                case "INVENTORY", "INV" -> GameView.displayInventory(player);
                default -> GameView.printLine("Unknown combat command.");
            }
        }

        return !player.isDead();
    }

    private static boolean promptGameOverChoice(Scanner input) {
        while (true) {
            GameView.print("Choice: ");
            String choice = input.nextLine().trim();
            if (choice.equals("1"))
                return true;
            if (choice.equals("2"))
                return false;
            GameView.printLine("Enter 1 or 2.");
        }
    }

    // -------------------------------------------------------------------------
    // File loaders
    // -------------------------------------------------------------------------

    private static File resolveDataFile(String fileName) {
        File directFile = new File(fileName);
        if (directFile.exists())
            return directFile;

        File projectFile = new File("TextAdventure-main", fileName);
        if (projectFile.exists())
            return projectFile;

        File current = new File(System.getProperty("user.dir"));
        while (current != null) {
            File candidate = new File(current, fileName);
            if (candidate.exists())
                return candidate;

            File nestedProjectFile = new File(current, "TextAdventure-main" + File.separator + fileName);
            if (nestedProjectFile.exists())
                return nestedProjectFile;

            current = current.getParentFile();
        }

        return directFile;
    }

    // Format: roomNumber|name|description|north|east|south|west
    // Optional lock line: LOCK|roomNumber|requiredItemName
    private static boolean loadRooms(String fileName, Map<Integer, Room> rooms) {
        File f = resolveDataFile(fileName);
        if (!f.exists())
            return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\|", -1);

                if (parts[0].trim().equalsIgnoreCase("LOCK")) {
                    if (parts.length >= 3) {
                        try {
                            Room room = rooms.get(Integer.parseInt(parts[1].trim()));
                            if (room != null)
                                room.setLocked(parts[2].trim());
                        } catch (NumberFormatException e) {
                            // Ignore malformed lock rows.
                        }
                    }
                    continue;
                }

                if (parts.length < 7)
                    continue;

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

    // Format: name|description|type|attackBonus|healAmount|roomNumber
    private static void loadItems(String fileName, Map<Integer, Room> rooms, List<Item> allItems) {
        File f = resolveDataFile(fileName);
        if (!f.exists()) {
            GameView.printLine("Warning: " + fileName + " not found.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 6)
                    continue;

                try {
                    String name = parts[0].trim();
                    String description = parts[1].trim();
                    String type = parts[2].trim();
                    int attackBonus = Integer.parseInt(parts[3].trim());
                    int healAmount = Integer.parseInt(parts[4].trim());
                    int roomNumber = Integer.parseInt(parts[5].trim());

                    Item item = new Item(name, description, type, attackBonus, healAmount, roomNumber);
                    allItems.add(item);
                    if (roomNumber > 0 && rooms.containsKey(roomNumber))
                        rooms.get(roomNumber).addItem(item);
                } catch (NumberFormatException e) {
                    // Ignore malformed item rows.
                }
            }
        } catch (IOException e) {
            GameView.printLine("Warning: Could not load items from " + fileName);
        }
    }

    // Format:
    // roomNumber|name|description|correctAnswer|successMessage|rewardItemName|allowedAttempts
    private static Puzzle[] loadPuzzles(String fileName) {
        File f = resolveDataFile(fileName);
        if (!f.exists()) {
            GameView.printLine("Warning: " + fileName + " not found.");
            return new Puzzle[0];
        }

        Map<Integer, Puzzle> puzzleMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 7)
                    continue;

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
            GameView.printLine("Warning: Could not load puzzles from " + fileName);
            return new Puzzle[0];
        }

        return puzzleMap.values().toArray(Puzzle[]::new);
    }

    // Format:
    // roomNumber|name|description|health|attackDamage|threshold|dropItemName
    private static Monster[] loadMonsters(String fileName) {
        File f = resolveDataFile(fileName);
        if (!f.exists()) {
            GameView.printLine("Warning: " + fileName + " not found.");
            return new Monster[0];
        }

        List<Monster> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 7)
                    continue;

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
            GameView.printLine("Warning: Could not load monsters from " + fileName);
            return new Monster[0];
        }

        return list.toArray(Monster[]::new);
    }
}
