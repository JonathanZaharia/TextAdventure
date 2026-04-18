package adventure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final String ROOMS_FILE    = "Rooms.txt";
    private static final String ITEMS_FILE    = "Items.txt";
    private static final String PUZZLE_FILE   = "Puzzle.txt";
    private static final String MONSTERS_FILE = "Monsters.txt";

    // Player starting values — change here, not in Player.java
    private static final int PLAYER_START_ROOM  = 1;
    private static final int PLAYER_MAX_HEALTH  = 20;
    private static final int PLAYER_BASE_DAMAGE = 2;

    private static final Random random = new Random();

    public static void main(String[] args) {
        Map<Integer, Room> rooms = new HashMap<>();
        List<Item> allItems = new ArrayList<>();

        if (!loadRooms(ROOMS_FILE, rooms)) {
            System.out.println("ERROR: Could not load room data from " + ROOMS_FILE);
            return;
        }

        loadItems(ITEMS_FILE, rooms, allItems);
        Puzzle[]  puzzles  = loadPuzzles(PUZZLE_FILE);
        Monster[] monsters = loadMonsters(MONSTERS_FILE);

        int startRoom = rooms.containsKey(PLAYER_START_ROOM)
                ? PLAYER_START_ROOM
                : rooms.keySet().stream().min(Integer::compareTo).orElseThrow();

        Player player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);

        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Welcome to Mrs. Blackwood's Mansion!");
            System.out.println("Type HELP for commands.\n");

            boolean running          = true;
            boolean justEnteredRoom  = true;
            int     previousRoom     = -1;

            while (running) {
                int  currentRoomNumber = player.getCurrentRoomNumber();
                Room current           = rooms.get(currentRoomNumber);

                // When leaving a room, reset puzzle visit state for the room just left
                if (previousRoom != -1 && previousRoom != currentRoomNumber) {
                    Puzzle prev = Puzzle.findByRoomNumber(puzzles, previousRoom);
                    if (prev != null) prev.clearFailedForThisVisit();
                }
                previousRoom = currentRoomNumber;

                if (justEnteredRoom) {
                    // Monster encounter on entry
                    Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                    if (monster != null) {
                        boolean survived = handleMonsterEncounter(monster, allItems, player, current, input);
                        if (!survived) {
                            running = handleGameOver(input);
                            if (running) {
                                // Restart — reset player and all game state
                                player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);
                                for (Puzzle p : puzzles) p.reset();
                                for (Room   r : rooms.values()) r.setVisited(false);
                            }
                            justEnteredRoom = true;
                            continue;
                        }
                    }

                    // Puzzle check on entry
                    Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);
                    if (roomPuzzle != null && !roomPuzzle.isSolved() && !roomPuzzle.isFailedForThisVisit()) {
                        boolean solved = handlePuzzle(roomPuzzle, allItems, player, input);
                        if (!solved) roomPuzzle.setFailedForThisVisit();
                    }

                    displayRoom(current, puzzles, monsters);
                    justEnteredRoom = false;
                }

                System.out.print("\nCommand: ");
                String inputLine = input.nextLine().trim();
                if (inputLine.isEmpty()) continue;

                String[] parts    = inputLine.split(" ", 2);
                String   cmd      = parts[0].toUpperCase();
                String   argument = parts.length > 1 ? parts[1].trim() : "";

                switch (cmd) {

                    case "N", "E", "S", "W", "NORTH", "SOUTH", "EAST", "WEST" -> {
                        String dir  = cmd.substring(0, 1);
                        int exitDest = current.getExit(dir);

                        if (exitDest <= 0 || !rooms.containsKey(exitDest)) {
                            System.out.println("You cannot go that way.");
                        } else {
                            Room dest = rooms.get(exitDest);
                            if (dest.isLocked()) {
                                if (dest.canPlayerEnter(player)) {
                                    String keyName = dest.getRequiredItemName();
                                    dest.unlock();
                                    System.out.println("You use the " + keyName + " to unlock the door.");
                                    player.setCurrentRoomNumber(exitDest);
                                    justEnteredRoom = true;
                                } else {
                                    System.out.println("This room is locked. You need: " + dest.getRequiredItemName());
                                }
                            } else {
                                player.setCurrentRoomNumber(exitDest);
                                justEnteredRoom = true;
                            }
                        }
                    }

                    case "EXPLORE", "LOOK" -> {
                        if (!current.hasItems()) {
                            System.out.println("There are no items in this room.");
                        } else {
                            System.out.println("Items in this room:");
                            for (Item item : current.getItems()) {
                                System.out.println("  - " + item.getName());
                            }
                        }
                    }

                    case "PICKUP", "TAKE", "GET" -> {
                        if (argument.isEmpty()) {
                            System.out.println("Specify an item name. Example: TAKE Keycard");
                        } else {
                            handlePickup(player, current, argument);
                        }
                    }

                    case "INSPECT", "EXAMINE" -> {
                        if (argument.isEmpty()) {
                            System.out.println(current.getDescription());
                        } else {
                            handleInspect(player, current, monsters, argument);
                        }
                    }

                    case "DROP" -> {
                        if (argument.isEmpty()) {
                            System.out.println("Specify an item name. Example: DROP Keycard");
                        } else {
                            handleDrop(player, current, argument);
                        }
                    }

                    case "EQUIP" -> {
                        if (argument.isEmpty()) {
                            System.out.println("Specify a weapon. Example: EQUIP Rusty Knife");
                        } else {
                            handleEquip(player, argument);
                        }
                    }

                    case "UNEQUIP" -> handleUnequip(player);

                    case "HEAL", "CONSUME" -> {
                        if (argument.isEmpty()) {
                            System.out.println("Specify an item. Example: HEAL Med Kit");
                        } else {
                            handleHeal(player, argument);
                        }
                    }

                    case "ATTACK" -> {
                        Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                        if (monster == null) {
                            System.out.println("There is no monster here.");
                        } else {
                            boolean survived = handleCombat(monster, allItems, player, current, input);
                            if (!survived) {
                                running = handleGameOver(input);
                                if (running) {
                                    player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);
                                    for (Puzzle p : puzzles) p.reset();
                                    for (Room   r : rooms.values()) r.setVisited(false);
                                }
                                justEnteredRoom = true;
                            }
                        }
                    }

                    case "IGNORE" -> {
                        Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                        if (monster == null) {
                            System.out.println("There is no monster here.");
                        } else {
                            monster.setIgnored();
                            System.out.println("You back away. The " + monster.getName() + " lets you go and will not appear again.");
                        }
                    }

                    case "SOLVE" -> {
                        Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);
                        if (roomPuzzle == null) {
                            System.out.println("There is no puzzle here.");
                        } else if (roomPuzzle.isSolved()) {
                            System.out.println("This puzzle has already been solved.");
                        } else if (roomPuzzle.isFailedForThisVisit()) {
                            System.out.println("You have no attempts remaining for this puzzle right now.");
                        } else {
                            boolean solved = handlePuzzle(roomPuzzle, allItems, player, input);
                            if (!solved) roomPuzzle.setFailedForThisVisit();
                        }
                    }

                    case "INVENTORY", "INV", "I" -> displayInventory(player);

                    case "HEALTH", "HP" -> System.out.println("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth());

                    case "HELP" -> printHelp();

                    case "Q", "QUIT", "EXIT" -> {
                        running = false;
                        System.out.println("Thanks for playing!");
                    }

                    default -> System.out.println("Unknown command. Type HELP for commands.");
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Command handlers
    // -------------------------------------------------------------------------

    private static void handlePickup(Player player, Room room, String itemName) {
        Item item = room.findItem(itemName);
        if (item == null) {
            System.out.println("There is no item called '" + itemName + "' here.");
            return;
        }
        player.addItem(item);
        room.removeItem(item);
        System.out.println(item.getName() + " added to inventory.");
    }

    private static void handleInspect(Player player, Room room, Monster[] monsters, String target) {
        // Check room items first, then inventory, then monster
        Item roomItem = room.findItem(target);
        if (roomItem != null) { System.out.println(roomItem.getName() + ": " + roomItem.getDescription()); return; }

        Item invItem = player.getItemByName(target);
        if (invItem != null) { System.out.println(invItem.getName() + ": " + invItem.getDescription()); return; }

        Monster monster = Monster.findActiveByRoomNumber(monsters, room.getRoomNumber());
        if (monster != null && monster.getName().equalsIgnoreCase(target)) {
            System.out.println(monster.getName() + ": " + monster.getDescription());
            System.out.println("Attack: " + monster.getAttackDamage() + " | HP: " + monster.getCurrentHealth() + "/" + monster.getMaxHealth());
            return;
        }

        System.out.println("There is nothing like that to inspect here.");
    }

    private static void handleDrop(Player player, Room room, String itemName) {
        Item item = player.removeItemByName(itemName);
        if (item == null) {
            System.out.println("You do not have '" + itemName + "' in your inventory.");
            return;
        }
        room.addItem(item);
        System.out.println(item.getName() + " dropped in " + room.getName() + ".");
    }

    private static void handleEquip(Player player, String itemName) {
        Item item = player.getItemByName(itemName);
        if (item == null) { System.out.println("You do not have '" + itemName + "'."); return; }
        if (!player.equipItem(item)) { System.out.println(item.getName() + " cannot be equipped — it is not a weapon."); return; }
        System.out.println(item.getName() + " equipped. Attack: " + player.getAttackDamage());
    }

    private static void handleUnequip(Player player) {
        Item prev = player.unequipItem();
        if (prev == null) { System.out.println("Nothing is equipped."); return; }
        System.out.println(prev.getName() + " unequipped. Attack: " + player.getAttackDamage());
    }

    private static void handleHeal(Player player, String itemName) {
        Item item = player.getItemByName(itemName);
        if (item == null) { System.out.println("You do not have '" + itemName + "'."); return; }
        if (!item.isConsumable()) { System.out.println(item.getName() + " cannot be consumed."); return; }
        int before = player.getCurrentHealth();
        player.heal(item.getHealAmount());
        player.removeItem(item);
        System.out.println("Used " + item.getName() + ". Restored " + (player.getCurrentHealth() - before) + " HP. Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth());
    }

    // -------------------------------------------------------------------------
    // Puzzle handler — same logic as original, updated for new Puzzle fields
    // -------------------------------------------------------------------------

    private static boolean handlePuzzle(Puzzle puzzle, List<Item> allItems, Player player, Scanner input) {
        System.out.println("\n*** PUZZLE: " + puzzle.getName() + " ***");
        System.out.println(puzzle.getDescription());

        while (puzzle.hasAttemptsRemaining()) {
            System.out.print("Your answer: ");
            String answer = input.nextLine().trim();

            if (puzzle.attemptAnswer(answer)) {
                String msg = puzzle.getSuccessMessage();
                System.out.println(msg.isEmpty() ? "Puzzle solved!" : msg);

                // Grant item reward if any
                if (puzzle.hasItemReward()) {
                    for (Item item : allItems) {
                        if (item.getName().equalsIgnoreCase(puzzle.getRewardItemName())) {
                            player.addItem(item);
                            System.out.println("Received: " + item.getName());
                            break;
                        }
                    }
                }
                return true;
            } else {
                if (puzzle.hasAttemptsRemaining()) {
                    System.out.println("Incorrect. " + puzzle.getRemainingAttempts() + " attempt(s) remaining.");
                } else {
                    System.out.println("Incorrect. No attempts remaining.");
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Monster handlers
    // -------------------------------------------------------------------------

    // Called on room entry — player chooses to attack or ignore
    private static boolean handleMonsterEncounter(Monster monster, List<Item> allItems, Player player, Room room, Scanner input) {
        System.out.println("\n! A " + monster.getName() + " is here !");
        System.out.println(monster.getDescription());
        System.out.println("Type ATTACK to fight or IGNORE to back away.");

        while (true) {
            System.out.print("Choice: ");
            String choice = input.nextLine().trim().toUpperCase();
            if (choice.equals("ATTACK")) return handleCombat(monster, allItems, player, room, input);
            if (choice.equals("IGNORE")) {
                monster.setIgnored();
                System.out.println("You back away. The " + monster.getName() + " will not appear again.");
                return true;
            }
            System.out.println("Type ATTACK or IGNORE.");
        }
    }

    // Turn-based combat loop; player can also EQUIP/HEAL during combat
    private static boolean handleCombat(Monster monster, List<Item> allItems, Player player, Room room, Scanner input) {
        System.out.println("\n--- COMBAT: " + monster.getName() + " ---");

        while (!monster.isDead() && !player.isDead()) {
            System.out.println("\nYour HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                    + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());
            System.out.print("Action (ATTACK / HEAL [item] / EQUIP [item] / UNEQUIP / INVENTORY): ");

            String line = input.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts  = line.split(" ", 2);
            String   action = parts[0].toUpperCase();
            String   arg    = parts.length > 1 ? parts[1].trim() : "";

            switch (action) {
                case "ATTACK" -> {
                    int damage = player.getAttackDamage();
                    monster.takeDamage(damage);
                    System.out.println("You deal " + damage + " damage. " + monster.getName() + " HP: " + monster.getCurrentHealth());
                    if (monster.isDead()) {
                        System.out.println("You defeated the " + monster.getName() + "!");
                        handleMonsterDrop(monster, allItems, room);
                        return true;
                    }
                    // Monster counter-attacks
                    monsterAttack(monster, player);
                }
                case "HEAL"    -> { handleHeal(player, arg);    if (!player.isDead()) monsterAttack(monster, player); }
                case "EQUIP"   -> handleEquip(player, arg);
                case "UNEQUIP" -> handleUnequip(player);
                case "INVENTORY", "INV" -> displayInventory(player);
                default -> System.out.println("Unknown combat command.");
            }
        }

        return !player.isDead();
    }

    private static void monsterAttack(Monster monster, Player player) {
        double roll   = random.nextDouble();
        int    damage = monster.calculateAttackDamage(roll);
        player.takeDamage(damage);
        System.out.println(monster.getName() + " attacks for " + damage
                + (damage > monster.getAttackDamage() ? " (critical!)" : "")
                + ". Your HP: " + player.getCurrentHealth());
        if (player.isDead()) System.out.println("You have been defeated...");
    }

    // Place monster's drop item in the room after defeat
    private static void handleMonsterDrop(Monster monster, List<Item> allItems, Room room) {
        if (!monster.hasDropItem()) return;
        for (Item item : allItems) {
            if (item.getName().equalsIgnoreCase(monster.getDropItemName())) {
                room.addItem(item);
                System.out.println(monster.getName() + " dropped: " + item.getName() + ". Use TAKE to pick it up.");
                return;
            }
        }
    }

    private static boolean handleGameOver(Scanner input) {
        System.out.println("\n=== GAME OVER ===");
        System.out.println("1. Start new game   2. Exit");
        while (true) {
            System.out.print("Choice: ");
            String c = input.nextLine().trim();
            if (c.equals("1")) return true;
            if (c.equals("2")) return false;
            System.out.println("Enter 1 or 2.");
        }
    }

    // -------------------------------------------------------------------------
    // Display helpers (all console output lives in Main, not in model classes)
    // -------------------------------------------------------------------------

    private static void displayRoom(Room room, Puzzle[] puzzles, Monster[] monsters) {
        System.out.println("\n--------------------------------------------------");
        System.out.println("Room " + room.getRoomNumber() + ": " + room.getName());
        System.out.println(room.getDescription());
        if (room.isVisited()) System.out.println("[Visited]");
        room.markVisited();

        if (room.hasItems()) {
            System.out.println("Items: " + room.getItems().stream().map(Item::getName).reduce((a, b) -> a + ", " + b).orElse(""));
        }

        Monster m = Monster.findActiveByRoomNumber(monsters, room.getRoomNumber());
        if (m != null) System.out.println("[WARNING: " + m.getName() + " is here!]");

        Puzzle p = Puzzle.findByRoomNumber(puzzles, room.getRoomNumber());
        if (p != null && !p.isSolved()) System.out.println("[There is a puzzle here. Type SOLVE to attempt it.]");

        System.out.print("Exits: ");
        System.out.println(room.getExits().isEmpty() ? "None" : String.join(", ", room.getExits().keySet()));
    }

    private static void displayInventory(Player player) {
        System.out.println("\n--- Inventory ---");
        if (player.isInventoryEmpty()) {
            System.out.println("  (empty)");
        } else {
            for (Item item : player.getInventory()) {
                String tag = (player.getEquippedItem() == item) ? " [EQUIPPED]" : "";
                System.out.println("  - " + item.getName() + " (" + item.getType() + ")" + tag);
            }
        }
        System.out.println("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                + "  |  Attack: " + player.getAttackDamage());
    }

    private static void printHelp() {
        System.out.println("\n--- Commands ---");
        System.out.println("  N/E/S/W           Move");
        System.out.println("  LOOK / EXPLORE    List items in room");
        System.out.println("  TAKE [item]       Pick up item");
        System.out.println("  DROP [item]       Drop item");
        System.out.println("  INSPECT [item]    Inspect item or monster");
        System.out.println("  EQUIP [item]      Equip a weapon");
        System.out.println("  UNEQUIP           Unequip weapon");
        System.out.println("  HEAL [item]       Use a consumable");
        System.out.println("  ATTACK            Attack monster in room");
        System.out.println("  IGNORE            Ignore monster in room");
        System.out.println("  SOLVE             Attempt room puzzle");
        System.out.println("  INVENTORY / INV   View inventory");
        System.out.println("  HEALTH / HP       View health");
        System.out.println("  HELP              Show commands");
        System.out.println("  QUIT / Q          Quit");
    }

    // -------------------------------------------------------------------------
    // File loaders
    // -------------------------------------------------------------------------

    // Format: roomNumber|name|description|north|east|south|west
    // Optional lock line: LOCK|roomNumber|requiredItemName
    private static boolean loadRooms(String fileName, Map<Integer, Room> rooms) {
        File f = new File(fileName);
        if (!f.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);

                if (parts[0].trim().equalsIgnoreCase("LOCK")) {
                    if (parts.length >= 3) {
                        Room room = rooms.get(Integer.parseInt(parts[1].trim()));
                        if (room != null) room.setLocked(parts[2].trim());
                    }
                    continue;
                }

                if (parts.length < 7) continue;

                int    number = Integer.parseInt(parts[0].trim());
                String name   = parts[1].trim();
                String desc   = parts[2].trim();

                Room room = new Room(number, name, desc);
                room.addExit("N", Integer.parseInt(parts[3].trim()));
                room.addExit("E", Integer.parseInt(parts[4].trim()));
                room.addExit("S", Integer.parseInt(parts[5].trim()));
                room.addExit("W", Integer.parseInt(parts[6].trim()));
                rooms.put(number, room);
            }
        } catch (IOException e) {
            return false;
        }

        return !rooms.isEmpty();
    }

    // Format: name|description|type|attackBonus|healAmount|roomNumber
    private static void loadItems(String fileName, Map<Integer, Room> rooms, List<Item> allItems) {
        File f = new File(fileName);
        if (!f.exists()) { System.out.println("Warning: " + fileName + " not found."); return; }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 6) continue;

                String name        = parts[0].trim();
                String description = parts[1].trim();
                String type        = parts[2].trim();
                int    attackBonus = Integer.parseInt(parts[3].trim());
                int    healAmount  = Integer.parseInt(parts[4].trim());
                int    roomNumber  = Integer.parseInt(parts[5].trim());

                Item item = new Item(name, description, type, attackBonus, healAmount, roomNumber);
                allItems.add(item);
                if (roomNumber > 0 && rooms.containsKey(roomNumber)) rooms.get(roomNumber).addItem(item);
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not load items from " + fileName);
        }
    }

    // Format: roomNumber|name|description|correctAnswer|successMessage|rewardItemName|allowedAttempts
    private static Puzzle[] loadPuzzles(String fileName) {
        File f = new File(fileName);
        if (!f.exists()) { System.out.println("Warning: " + fileName + " not found."); return new Puzzle[0]; }

        Map<Integer, Puzzle> puzzleMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;

                int    roomNumber  = Integer.parseInt(parts[0].trim());
                String name        = parts[1].trim();
                String description = parts[2].trim();
                String answer      = parts[3].trim();
                String successMsg  = parts[4].trim();
                String rewardItem  = parts[5].trim();
                int    attempts    = Integer.parseInt(parts[6].trim());

                puzzleMap.put(roomNumber, new Puzzle(name, description, answer, successMsg, rewardItem, attempts, roomNumber));
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not load puzzles from " + fileName);
            return new Puzzle[0];
        }

        return puzzleMap.values().toArray(Puzzle[]::new);
    }

    // Format: roomNumber|name|description|health|attackDamage|threshold|dropItemName
    private static Monster[] loadMonsters(String fileName) {
        File f = new File(fileName);
        if (!f.exists()) { System.out.println("Warning: " + fileName + " not found."); return new Monster[0]; }

        List<Monster> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;

                int    roomNumber = Integer.parseInt(parts[0].trim());
                String name       = parts[1].trim();
                String desc       = parts[2].trim();
                int    health     = Integer.parseInt(parts[3].trim());
                int    attack     = Integer.parseInt(parts[4].trim());
                double threshold  = Double.parseDouble(parts[5].trim());
                String dropItem   = parts[6].trim();

                list.add(new Monster(name, desc, health, attack, threshold, dropItem, roomNumber));
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not load monsters from " + fileName);
            return new Monster[0];
        }

        return list.toArray(Monster[]::new);
    }
}