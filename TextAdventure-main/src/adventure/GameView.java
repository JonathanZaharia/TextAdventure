package adventure;

public class GameView {

    private static final int MAX_LINE_LENGTH = 60;
    private static final String DIVIDER = "============================================================";

    public static void printLine(String message) {
        if (message == null) {
            System.out.println();
            return;
        }

        String[] lines = message.split("\\r?\\n", -1);
        for (String line : lines) {
            printWrappedLine(line);
        }
    }

    public static void print(String message) {
        System.out.print(message);
    }

    public static void printWelcome() {
        System.out.println("============================================================");
        System.out.println("             THE BLACKWOOD MANSION MYSTERY");
        System.out.println("============================================================");
        System.out.println();
        System.out.println("The year is 1923. You are a private detective summoned to");
        System.out.println("Blackwood Mansion after the sudden disappearance of three");
        System.out.println("guests - and the silence of Mrs. Eleanor Blackwood.");
        System.out.println();
        System.out.println("The police closed the case. You didn't.");
        System.out.println();
        System.out.println("Your goal: reach the top floor, uncover the truth, and");
        System.out.println("confront whatever is waiting in the Conservatory.");
        System.out.println();
        System.out.println("During the game, you can:");
        System.out.println("- Type HELP for a list of possible commands.");
        System.out.println("- Type OBJ to view your current objective.");
        System.out.println();
        System.out.print("Press ENTER to step into the Blackwood Mansion:");
    }

    public static void printGoodLuck() {
        System.out.println();
        System.out.println("Good Luck!");
    }

    public static void displayRoomHeader(Room room) {
        printLine("");
        printLine(DIVIDER);
        printLine("");
        printLine("Room " + room.getRoomNumber() + ": " + room.getName());
        printLine(room.getDescription());
        if (room.isVisited())
            printLine("This room looks familiar");
    }

    public static void displayRoomFooter(Room room) {
        String exits = room.getExits().isEmpty()
                ? "None"
                : room.getExits().keySet().stream()
                        .sorted((a, b) -> {
                            int aPriority = getExitSortPriority(a);
                            int bPriority = getExitSortPriority(b);

                            if (aPriority != bPriority) {
                                return Integer.compare(aPriority, bPriority);
                            }

                            return a.compareToIgnoreCase(b);
                        })
                        .map(dir -> switch (dir.toUpperCase()) {
                            case "N" -> "North";
                            case "E" -> "East";
                            case "S" -> "South";
                            case "W" -> "West";
                            default -> dir;
                        })
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("None");

        printLine("\nAvailable Exits: " + exits);
        printLine("");
        printLine("=== COMMANDS ===");
        printLine("  N / E / S / W         OBJECTIVE / OBJ");
        printLine("  SEARCH                TAKE [item]");
        printLine("  INVENTORY / INV       HEALTH / HP");
        printLine("  HELP");
    }

    public static void printCombatCommandsPrompt() {
        printLine("=== COMMANDS ===");
        printLine("  ATTACK               FLEE");
        printLine("  HEAL [item]          EQUIP [item]");
        printLine("  INVENTORY / INV");
        printLine("");
        print("Choice: ");
    }

    private static int getExitSortPriority(String direction) {
        if (direction == null) {
            return Integer.MAX_VALUE;
        }

        return switch (direction.trim().toUpperCase()) {
            case "N" -> 0;
            case "E" -> 1;
            case "S" -> 2;
            case "W" -> 3;
            default -> Integer.MAX_VALUE;
        };
    }

    public static void displayInventory(Player player) {
        printLine("");
        printLine("=== Inventory ===");
        if (player.isInventoryEmpty()) {
            printLine("  (empty)");
        } else {
            for (Item item : player.getInventory()) {
                String tag = (player.getEquippedItem() == item) ? " [EQUIPPED]" : "";
                printLine("  - " + item.getName() + " (" + item.getType() + ")" + tag);
            }
        }
        printLine("");
        printLine("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                + "  |  Attack: " + player.getAttackDamage());
    }

    public static void printHelp() {
        printLine("");
        printLine("=== LIST OF POSSIBLE COMMANDS ===");
        printLine("  Navigation:");
        printLine("  N / E / S / W         Move between rooms");
        printLine("  EXITS                 List available exits");
        printLine("");
        printLine("  Exploration:");
        printLine("  SEARCH                Search the room for items");
        printLine("  LOOK                  Reprint the room description");
        printLine("  INSPECT [item]        Examine an item or monster");
        printLine("");
        printLine("  Items:");
        printLine("  TAKE [item]           Pick up an item");
        printLine("  DROP [item]           Drop an item from inventory");
        printLine("  INVENTORY / INV       View your inventory");
        printLine("  EQUIP [item]          Equip a weapon");
        printLine("  UNEQUIP               Unequip current weapon");
        printLine("  HEAL [item]           Use a consumable item");
        printLine("");
        printLine("  Combat:");
        printLine("  FIGHT                 Engage an encountered monster");
        printLine("  IGNORE                Back away from a monster");
        printLine("");
        printLine("  Puzzle:");
        printLine("  SOLVE                 Attempt to solve a puzzle");
        printLine("");
        printLine("  Status:");
        printLine("  HEALTH / HP           View current health");
        printLine("  OBJECTIVE / OBJ       View current objective");
        printLine("");
        printLine("  Game:");
        printLine("  SAVE                  Save your progress");
        printLine("  LOAD                  Load your last save");
        printLine("  HELP                  Show this list");
        printLine("  QUIT / Q              Quit the game");
        printLine("");
        printLine("  Note: During monster encounters only FIGHT and IGNORE");
        printLine("  are accepted. During combat only ATTACK, FLEE, HEAL,");
        printLine("  EQUIP, UNEQUIP, and INV are accepted. During puzzles");
        printLine("  only SOLVE and IGNORE are accepted — any other input");
        printLine("  will count as a puzzle answer attempt.");
    }

    public static void printObjective(int currentObjective, String[] objectives) {
        printLine("");
        if (objectives == null || objectives.length == 0) {
            printLine("Current Objective: No objective data available.");
            return;
        }

        int index = Math.max(0, Math.min(currentObjective, objectives.length - 1));
        String objective = objectives[index];
        if (objective == null || objective.isBlank()) {
            printLine("Current Objective: No objective data available.");
        } else {
            printLine("Current Objective: " + objective.trim());
        }
    }

    public static void showGameOverMenu() {
        printLine("");
        printLine(DIVIDER);
        printLine("");
        printLine("=== GAME OVER ===");
        printLine("");
        printLine("1. Respawn   2. Quit Game   3. Load Save");
        printLine("");
        printLine(DIVIDER);
    }

    public static void printMonsterDrop(String monsterName, String itemName) {
        printLine(monsterName + " dropped: " + itemName + ". Use TAKE to pick it up.");
    }

    public static void printMonsterEncounterHeader(String monsterName, String monsterDescription) {
        printLine("=== ENCOUNTER: " + monsterName + " ===");
        printLine(monsterDescription);
        printLine("");
        printLine("Type FIGHT to engage  |  Type IGNORE to back away");
    }

    public static void printPuzzleEncounterHeader(String puzzleName, String puzzleDescription) {
        printLine("=== PUZZLE: " + puzzleName + " ===");
        printLine(puzzleDescription);
        printLine("");
        printLine("Type SOLVE to attempt  |  Type IGNORE to skip");
    }

    public static void printVictorySequence() {
        printLine("");
        printLine(DIVIDER);
        printLine("");
        printLine("=== CASE CLOSED ===");
        printLine("");
        printLine("Mrs. Blackwood turns slowly, still and composed.");
        printLine("Her smile fades when you read the case record aloud:");
        printLine("the missing guests were never random victims, but");
        printLine("witnesses she lured into the mansion's staged riddles.");
        printLine("");
        printLine("Each puzzle, each locked room, and each planted clue");
        printLine("pointed to one truth: Blackwood engineered a theater of");
        printLine("fear to erase anyone who uncovered her crimes.");
        printLine("");
        printLine("When you present the final sequence and the signed file,");
        printLine("the Conservatory doors unlock and the house falls silent.");
        printLine("By dawn, the police return with enough evidence to reopen");
        printLine("every closed report and arrest Eleanor Blackwood.");
        printLine("");
        printLine("You leave the mansion as the first light breaks over the");
        printLine("glass roof. The Blackwood mystery is finally solved.");
        printLine("");
        printLine(DIVIDER);
    }

    private static void printWrappedLine(String line) {
        if (line == null || line.isEmpty()) {
            System.out.println();
            return;
        }

        String remaining = line;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= MAX_LINE_LENGTH) {
                System.out.println(remaining);
                return;
            }

            int breakPoint = findWrapPoint(remaining, MAX_LINE_LENGTH);
            if (breakPoint <= 0) {
                System.out.println(remaining.substring(0, MAX_LINE_LENGTH));
                remaining = remaining.substring(MAX_LINE_LENGTH);
            } else {
                System.out.println(trimTrailingWhitespace(remaining.substring(0, breakPoint)));
                remaining = trimLeadingWhitespace(remaining.substring(breakPoint));
            }
        }
    }

    private static int findWrapPoint(String text, int maxLength) {
        int limit = Math.min(text.length(), maxLength);
        for (int i = limit - 1; i >= 0; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String trimLeadingWhitespace(String text) {
        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return text.substring(index);
    }

    private static String trimTrailingWhitespace(String text) {
        int index = text.length();
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return text.substring(0, index);
    }
}
