package adventure;

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
    private static final String OBJECTIVES_FILE = "Objectives.txt";

    // Player starting values
    private static final int PLAYER_START_ROOM = 1;
    private static final int PLAYER_MAX_HEALTH = 20;
    private static final int PLAYER_BASE_DAMAGE = 2;

    public static void main(String[] args) {
        Map<Integer, Room> rooms = new HashMap<>();
        List<Item> allItems = new ArrayList<>();

        if (!GameDataLoader.loadRooms(ROOMS_FILE, rooms)) {
            GameView.printLine("ERROR: Could not load room data from " + ROOMS_FILE);
            return;
        }

        GameDataLoader.loadItems(ITEMS_FILE, rooms, allItems);
        Puzzle[] puzzles = GameDataLoader.loadPuzzles(PUZZLE_FILE);
        Monster[] monsters = GameDataLoader.loadMonsters(MONSTERS_FILE);
        String[] objectives = GameDataLoader.loadObjectives(OBJECTIVES_FILE);

        int startRoom = rooms.containsKey(PLAYER_START_ROOM)
                ? PLAYER_START_ROOM
                : rooms.keySet().stream().min(Integer::compareTo).orElseThrow();

        Player player = new Player(startRoom, PLAYER_MAX_HEALTH, PLAYER_BASE_DAMAGE);

        try (Scanner input = new Scanner(System.in)) {
            boolean started = false;
            while (!started) {
                GameView.printLine("");
                GameView.printLine("=== MAIN MENU ===");
                GameView.printLine("1. Start New Game");
                GameView.printLine("2. Load Saved Game");
                GameView.printLine("3. Quit");
                GameView.print("\nChoice: ");

                String startChoice = input.nextLine().trim();
                switch (startChoice) {
                    case "1" -> {
                        GameView.printWelcome();
                        input.nextLine();
                        GameView.printGoodLuck();
                        started = true;
                    }
                    case "2" -> {
                        if (!SaveManager.hasSaveFile()) {
                            GameView.printLine("\n=== ERROR: No save file found. ===");
                        } else {
                            if (SaveManager.loadGame(player, rooms, puzzles, monsters, allItems)) {
                                GameView.printLine("\n=== Saved game loaded. ===");
                                started = true;
                            } else {
                                GameView.printLine("\n=== ERROR: Could not load save file. ===");
                            }
                        }
                    }
                    case "3" -> {
                        GameView.printLine("\n=== Thanks for playing! ===");
                        return;
                    }
                    default -> GameView.printLine("Enter 1, 2, or 3.");
                }
            }

            boolean running = true;
            boolean justEnteredRoom = true;
            int previousRoom = -1;
            int currentObjective = 0;

            while (running) {
                int currentRoomNumber = player.getCurrentRoomNumber();
                Room current = rooms.get(currentRoomNumber);

                // When leaving a room, reset puzzle visit state for the room just left
                if (previousRoom != -1 && previousRoom != currentRoomNumber) {
                    Puzzle prev = Puzzle.findByRoomNumber(puzzles, previousRoom);
                    if (prev != null) {
                        prev.clearFailedForThisVisit();
                    }
                }
                previousRoom = currentRoomNumber;

                if (justEnteredRoom) {
                    GameView.displayRoomHeader(current);

                    if (!current.isVisited()) {
                        if (currentRoomNumber == 6) {
                            currentObjective = advanceObjectiveTo(1, currentObjective);
                        } else if (currentRoomNumber == 8) {
                            currentObjective = advanceObjectiveTo(3, currentObjective);
                        } else if (currentRoomNumber == 15) {
                            currentObjective = advanceObjectiveTo(5, currentObjective);
                        }
                    }

                    // Monster encounter on entry and reset
                    Monster m = Monster.findByRoomNumber(monsters, currentRoomNumber);
                    if (m != null && m.isIgnored()) {
                        m.reset();
                    }

                    Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                    if (monster != null) {
                        boolean survived = handleMonsterEncounter(monster, allItems, player, current, input);
                        if (!survived) {
                            GameView.showGameOverMenu();
                            int gameOverChoice = promptGameOverChoice(input, player, rooms, puzzles, monsters,
                                    allItems);

                            if (gameOverChoice == 2) {
                                running = false;
                            } else if (gameOverChoice == 1) {
                                GameView.printLine("\n=== Respawning... ===");
                                player.resetHealth();
                                running = true;
                                previousRoom = -1;
                            } else {
                                GameView.printLine("\n=== Saved game loaded. ===");
                                running = true;
                                previousRoom = -1;
                            }

                            justEnteredRoom = true;
                            continue;
                        }
                    }

                    // Puzzle encounter on entry
                    Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);
                    if (roomPuzzle != null && !roomPuzzle.isSolved()) {
                        boolean solvedPuzzle = handlePuzzleEncounter(roomPuzzle, player, allItems, current, input);

                        if (solvedPuzzle) {
                            if (currentRoomNumber == 6) {
                                currentObjective = advanceObjectiveTo(2, currentObjective);
                            } else if (currentRoomNumber == 12) {
                                currentObjective = advanceObjectiveTo(4, currentObjective);
                            } else if (currentRoomNumber == 18) {
                                currentObjective = advanceObjectiveTo(6, currentObjective);
                            }
                        }
                    }

                    if (currentRoomNumber == 20) {
                        boolean guiltyVerdict = promptFinalVerdict(input);
                        GameView.printVictorySequence(guiltyVerdict);
                        running = false;
                        continue;
                    }

                    GameView.displayRoomFooter(current);
                    current.markVisited();
                    justEnteredRoom = false;
                }

                GameView.print("\nChoice: ");
                String inputLine = input.nextLine().trim();

                if (inputLine.isEmpty()) {
                    continue;
                }

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
                                    GameView.printLine("\n=== You use the " + keyName + " to unlock the door. ===");
                                    player.removeItemByName(keyName);
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

                    case "SEARCH" -> {
                        if (!current.hasItems()) {
                            GameView.printLine("You search the room thoroughly. Nothing of use turns up.");
                        } else {
                            GameView.printLine("You search the room thoroughly. Something useful catches your eye:");
                            for (Item item : current.getItems()) {
                                GameView.printLine("  - " + item.getName());
                            }
                        }
                    }

                    case "LOOK" -> {
                        GameView.printLine(current.getDescription());
                    }

                    case "PICKUP", "TAKE", "GET" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify an item name. Example: TAKE Keycard");
                        } else {
                            Player.PickupResult result = player.pickupItem(current, argument);
                            GameView.printPickupResult(result, argument, true);
                        }
                    }

                    case "INSPECT", "EXAMINE" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine(current.getDescription());
                        } else {
                            handleInspectCommand(player, current, monsters, argument);
                        }
                    }

                    case "DROP" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify an item name. Example: DROP Keycard");
                        } else {
                            Player.DropResult result = player.dropItem(current, argument);
                            GameView.printDropResult(result, argument, current, true);
                        }
                    }

                    case "EQUIP" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify a weapon. Example: EQUIP Rusty Knife");
                        } else {
                            Player.EquipResult result = player.equipWeapon(argument);
                            GameView.printEquipResult(result, argument, true);
                        }
                    }

                    case "UNEQUIP" -> {
                        Player.UnequipResult result = player.unequipWeapon();
                        GameView.printUnequipResult(result, true);
                    }

                    case "HEAL", "CONSUME" -> {
                        if (argument.isEmpty()) {
                            GameView.printLine("Specify an item. Example: HEAL Med Kit");
                        } else {
                            Player.ConsumeResult result = player.consumeHealingItem(argument);
                            boolean banner = result.getStatus() != Player.ConsumeStatus.FULL_HEALTH;
                            GameView.printConsumeResult(result, argument, banner);
                        }
                    }

                    case "ATTACK", "FIGHT" -> {
                        Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                        if (monster == null) {
                            GameView.printLine("There is no monster here.");
                        } else {
                            boolean survived = handleCombat(monster, allItems, player, current, input);
                            if (!survived) {
                                GameView.showGameOverMenu();
                                int gameOverChoice = promptGameOverChoice(input, player, rooms, puzzles, monsters,
                                        allItems);

                                if (gameOverChoice == 2) {
                                    running = false;
                                } else if (gameOverChoice == 1) {
                                    GameView.printLine("\n=== Respawning... ===");
                                    player.resetHealth();
                                    running = true;
                                    previousRoom = -1;
                                } else {
                                    GameView.printLine("\n=== Saved game loaded. ===");
                                    running = true;
                                    previousRoom = -1;
                                }

                                justEnteredRoom = true;
                            }
                        }
                    }

                    case "IGNORE" -> {
                        Monster monster = Monster.findActiveByRoomNumber(monsters, currentRoomNumber);
                        Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);

                        if (monster != null) {
                            monster.setIgnored();
                            GameView.printLine("You back away carefully. The monster drifts back into the shadows.");
                        } else if (roomPuzzle != null && !roomPuzzle.isSolved()) {
                            GameView.printLine("You ignore the puzzle for now.");
                        } else {
                            GameView.printLine("There is nothing here to ignore.");
                        }
                    }

                    case "SOLVE" -> {
                        Puzzle roomPuzzle = Puzzle.findByRoomNumber(puzzles, currentRoomNumber);

                        if (roomPuzzle == null) {
                            GameView.printLine("There is no puzzle here.");
                        } else if (roomPuzzle.isSolved()) {
                            GameView.printLine("This puzzle has already been solved.");
                        } else {
                            boolean solvedPuzzle = handlePuzzleSolveMenu(roomPuzzle, player, allItems, current, input);

                            if (solvedPuzzle) {
                                if (currentRoomNumber == 6) {
                                    currentObjective = advanceObjectiveTo(2, currentObjective);
                                } else if (currentRoomNumber == 12) {
                                    currentObjective = advanceObjectiveTo(4, currentObjective);
                                } else if (currentRoomNumber == 18) {
                                    currentObjective = advanceObjectiveTo(6, currentObjective);
                                }
                            }
                        }
                    }

                    case "EXITS" -> GameView.displayRoomFooter(current);

                    case "INVENTORY", "INV", "I" -> GameView.displayInventory(player);

                    case "HEALTH", "HP" ->
                        GameView.printLine("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth());

                    case "HELP" -> GameView.printHelp();

                    case "OBJECTIVE", "OBJ" -> GameView.printObjective(currentObjective, objectives);

                    case "SAVE" -> {
                        if (SaveManager.saveGame(player, puzzles, monsters, allItems)) {
                            GameView.printLine("\n=== Game saved. ===");
                        } else {
                            GameView.printLine("Could not save game.");
                        }
                    }

                    case "LOAD" -> {
                        if (!SaveManager.hasSaveFile()) {
                            GameView.printLine("No save file found.");
                        } else if (SaveManager.loadGame(player, rooms, puzzles, monsters, allItems)) {
                            GameView.printLine("\n=== Saved game loaded. ===");
                            justEnteredRoom = true;
                            previousRoom = -1;
                        } else {
                            GameView.printLine("Could not load save file.");
                        }
                    }

                    case "Q", "QUIT", "EXIT" -> {
                        running = false;
                        GameView.printLine("\n=== Thanks for playing! ===");
                    }

                    default -> GameView.printLine("Unknown command. Type HELP for commands.");
                }
            }
        }
    }

    private static boolean handlePuzzleEncounter(Puzzle puzzle, Player player, List<Item> allItems, Room room,
            Scanner input) {

        GameView.printLine("");
        GameView.printPuzzleEncounterHeader(puzzle.getName(), puzzle.getDescription());

        while (true) {
            GameView.print("\nChoice: ");
            String choice = input.nextLine().trim().toUpperCase();

            if (choice.equals("SOLVE")) {
                return handlePuzzleSolveMenu(puzzle, player, allItems, room, input);
            }

            if (choice.equals("IGNORE")) {
                GameView.printLine("You ignore the puzzle for now.");
                return false;
            }

            if (choice.equals("LOOK")) {
                GameView.printLine(room.getDescription());
                GameView.printLine(puzzle.getDescription());
            } else {
                GameView.printLine("Unknown command. Type SOLVE to attempt or IGNORE to skip.");
            }
        }
    }

    private static boolean handlePuzzleSolveMenu(Puzzle puzzle, Player player, List<Item> allItems, Room room,
            Scanner input) {

        puzzle.startSolveSession();

        while (!puzzle.isSolved() && puzzle.hasAttemptsRemaining()) {
            GameView.printLine("");
            int attemptNumber = puzzle.getAllowedAttempts() - puzzle.getRemainingAttempts() + 1;
            GameView.print("[Attempt " + attemptNumber + "/" + puzzle.getAllowedAttempts() + "] Answer: ");

            String answer = input.nextLine().trim();

            if (answer.equalsIgnoreCase("IGNORE")) {
                GameView.printLine("You step away from the puzzle.");
                return false;
            }

            if (answer.equalsIgnoreCase("LOOK")) {
                GameView.printLine(room.getDescription());
                GameView.printLine(puzzle.getDescription());
                continue;
            }

            if (answer.equalsIgnoreCase("SOLVE")) {
                GameView.printLine("That's not correct.");
                continue;
            }

            if (answer.isEmpty()) {
                GameView.printLine("Please enter an answer or type IGNORE.");
                continue;
            }

            boolean solved = puzzle.attemptSolve(answer);

            if (solved) {
                GameView.printLine(puzzle.getSuccessMessage());

                Item reward = puzzle.grantReward(player, allItems);
                if (reward != null) {
                    GameView.printLine("\n=== You received: " + reward.getName() + " ===");
                }

                return true;
            } else {
                GameView.printLine("That's not correct.");
            }
        }

        if (!puzzle.isSolved() && !puzzle.hasAttemptsRemaining()) {
            GameView.printLine("");
            GameView.printLine("That's not right. No attempts remaining.");
            GameView.printLine("Come back to the room to try again.");
        }

        return false;
    }

    private static boolean promptFinalVerdict(Scanner input) {
        while (true) {
            GameView.printFinalVerdictPrompt();
            GameView.print("\nChoice: ");

            String choice = input.nextLine().trim().toUpperCase();
            if (choice.equals("GUILTY")) {
                return true;
            }

            if (choice.equals("NOT GUILTY")) {
                return false;
            }

            GameView.printLine("Please type GUILTY or NOT GUILTY.");
        }
    }

    private static boolean handleMonsterEncounter(Monster monster, List<Item> allItems, Player player, Room room,
            Scanner input) {

        GameView.printLine("");
        GameView.printMonsterEncounterHeader(monster.getName(), monster.getDescription());

        while (true) {
            GameView.print("\nChoice: ");
            String inputLine = input.nextLine().trim();

            if (inputLine.isEmpty()) {
                continue;
            }

            String[] parts = inputLine.split(" ", 2);
            String choice = parts[0].toUpperCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";

            if (choice.equals("FIGHT")) {
                return handleCombat(monster, allItems, player, room, input);
            }

            if (choice.equals("IGNORE")) {
                monster.setIgnored();
                GameView.printLine("You back away carefully. The monster drifts back into the shadows.");
                return true;
            }

            if (choice.equals("INVENTORY") || choice.equals("INV")) {
                GameView.displayInventory(player);
                continue;
            }

            if (choice.equals("EQUIP")) {
                if (arg.isEmpty()) {
                    GameView.printLine("Specify a weapon. Example: EQUIP Rusty Knife");
                } else {
                    Player.EquipResult result = player.equipWeapon(arg);
                    GameView.printEquipResult(result, arg, false);
                }
                continue;
            }

            if (choice.equals("HEAL")) {
                if (arg.isEmpty()) {
                    GameView.printLine("Specify an item. Example: HEAL Med Kit");
                } else {
                    Player.ConsumeResult result = player.consumeHealingItem(arg);
                    GameView.printConsumeResult(result, arg, false);
                }
                continue;
            }

            if (choice.equals("HELP") || choice.equals("OBJ") || choice.equals("OBJECTIVE")) {
                GameView.printLine("You must deal with the threat first. Type FIGHT to engage");
                GameView.printLine("or IGNORE to back away.");
            } else {
                GameView.printLine("Unknown command.");
            }
        }
    }

    private static boolean handleCombat(Monster monster, List<Item> allItems, Player player, Room room,
            Scanner input) {

        GameView.printLine("");
        GameView.printLine("Your HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());

        while (!monster.isDead() && !player.isDead()) {
            GameView.printCombatCommandsPrompt();

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

                    if (monster.isDead()) {
                        GameView.printLine("");
                        GameView.printLine("=== You strike true. The " + monster.getName() + " is defeated. ===");
                        GameView.printLine("Your HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                                + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());
                        GameView.printLine("");
                        dropMonsterLoot(monster, allItems, room);
                        return true;
                    }

                    int monsterDamage = monster.monsterAttack();
                    player.takeDamage(monsterDamage);

                    GameView.printLine("");
                    if (monsterDamage > monster.getAttackDamage()) {
                        GameView.printLine("The " + monster.getName() + " surges forward with unnatural speed!");
                    } else {
                        GameView.printLine("You lunge forward and strike. The " + monster.getName() + " recoils.");
                    }
                    GameView.printLine("Your HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                            + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());
                    GameView.printLine("");

                    if (player.isDead()) {
                        GameView.printLine(
                                "=== The " + monster.getName() + " overwhelms you. Everything goes dark. ===");
                        return false;
                    }
                }

                case "FLEE" -> {
                    monster.setIgnored();
                    GameView.printLine("\n=== You retreat into the shadows. The " + monster.getName()
                            + " does not follow. ===");
                    return true;
                }

                case "HEAL" -> {
                    Player.ConsumeResult result = player.consumeHealingItem(arg);
                    GameView.printConsumeResult(result, arg, true);

                    if (!player.isDead()) {
                        int monsterDamage = monster.monsterAttack();
                        player.takeDamage(monsterDamage);

                        GameView.printLine("");
                        if (monsterDamage > monster.getAttackDamage()) {
                            GameView.printLine("The " + monster.getName() + " surges forward with unnatural speed!");
                        } else {
                            GameView.printLine("You lunge forward and strike. The " + monster.getName() + " recoils.");
                        }
                        GameView.printLine("Your HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                                + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());
                        GameView.printLine("");

                        if (player.isDead()) {
                            GameView.printLine(
                                    "=== The " + monster.getName() + " overwhelms you. Everything goes dark. ===");
                            return false;
                        }
                    }
                }

                case "EQUIP" -> {
                    Player.EquipResult result = player.equipWeapon(arg);
                    GameView.printEquipResult(result, arg, true);
                }

                case "UNEQUIP" -> {
                    Player.UnequipResult result = player.unequipWeapon();
                    GameView.printUnequipResult(result, false);
                }

                case "INVENTORY", "INV" -> GameView.displayInventory(player);

                default -> GameView.printLine("Unknown combat command.");
            }
        }

        return !player.isDead();
    }

    private static int promptGameOverChoice(Scanner input, Player player, Map<Integer, Room> rooms,
            Puzzle[] puzzles, Monster[] monsters, List<Item> allItems) {
        while (true) {
            GameView.print("\nChoice: ");
            String choice = input.nextLine().trim();

            if (choice.equals("1")) {
                return 1;
            }

            if (choice.equals("2")) {
                return 2;
            }

            if (choice.equals("3")) {
                if (!SaveManager.hasSaveFile()) {
                    GameView.printLine("\n=== ERROR: No save file found. ===");
                    GameView.showGameOverMenu();
                    continue;
                }

                if (SaveManager.loadGame(player, rooms, puzzles, monsters, allItems)) {
                    GameView.printLine("\n=== Saved game loaded. ===");
                    return 3;
                }

                GameView.printLine("\n=== ERROR: Could not load save file. ===");
                GameView.showGameOverMenu();
                continue;
            }

            GameView.printLine("Enter 1, 2, or 3.");
        }
    }

    private static void dropMonsterLoot(Monster monster, List<Item> allItems, Room room) {
        if (!monster.hasDropItem()) {
            return;
        }

        for (Item item : allItems) {
            if (item.getName().equalsIgnoreCase(monster.getDropItemName())) {
                room.addItem(item);
                GameView.printMonsterDrop(monster.getName(), item.getName());
                return;
            }
        }
    }

    private static void handleInspectCommand(Player player, Room room, Monster[] monsters, String target) {
        Item roomItem = room.findItem(target);
        if (roomItem != null) {
            GameView.printLine(roomItem.getName() + ": " + roomItem.getDescription());
            return;
        }

        Item invItem = player.getItemByName(target);
        if (invItem != null) {
            GameView.printLine(invItem.getName() + ": " + invItem.getDescription());
            return;
        }

        Monster monster = Monster.findActiveByRoomNumber(monsters, room.getRoomNumber());
        if (monster != null && monster.getName().equalsIgnoreCase(target)) {
            GameView.printLine(monster.getName() + ": " + monster.getDescription());
            GameView.printLine("Attack: " + monster.getAttackDamage() + " | HP: " + monster.getCurrentHealth() + "/"
                    + monster.getMaxHealth());
            return;
        }

        GameView.printLine("There is nothing like that to inspect here.");
    }

    // =========================================================================
    // File loaders
    // =========================================================================

    private static int advanceObjectiveTo(int newObjective, int currentObjective) {
        if (newObjective > currentObjective) {
            return newObjective;
        }

        return currentObjective;
    }

}
