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

    // ========================
    // PLAYER ACTION MESSAGES
    // ========================

    public static void printPickupResult(Player.PickupResult result, String requestedItemName, boolean banner) {
        if (result != null && result.isSuccess() && result.getItem() != null) {
            printActionMessage(result.getItem().getName() + " added to inventory.", banner);
            return;
        }

        printActionMessage("There is no item called '" + requestedItemName + "' here.", banner);
    }

    public static void printDropResult(Player.DropResult result, String requestedItemName, Room room, boolean banner) {
        if (result != null && result.isSuccess() && result.getItem() != null && room != null) {
            printActionMessage(result.getItem().getName() + " dropped in " + room.getName() + ".", banner);
            return;
        }

        printActionMessage("You do not have '" + requestedItemName + "' in your inventory.", banner);
    }

    public static void printEquipResult(Player.EquipResult result, String requestedItemName, boolean banner) {
        if (result == null) {
            printActionMessage("You do not have '" + requestedItemName + "'.", banner);
            return;
        }

        switch (result.getStatus()) {
            case EQUIPPED ->
                printActionMessage(result.getItem().getName() + " equipped. Attack: " + result.getAttackDamageAfter(),
                        banner);
            case NOT_WEAPON ->
                printActionMessage(result.getItem().getName() + " cannot be equipped - it is not a weapon.", banner);
            case NOT_OWNED ->
                printActionMessage("You do not have '" + requestedItemName + "'.", banner);
        }
    }

    public static void printUnequipResult(Player.UnequipResult result, boolean banner) {
        if (result == null) {
            printActionMessage("Nothing is equipped.", banner);
            return;
        }

        switch (result.getStatus()) {
            case NOTHING_EQUIPPED -> printActionMessage("Nothing is equipped.", banner);
            case UNEQUIPPED -> printActionMessage(
                    result.getItem().getName() + " unequipped. Attack: " + result.getAttackDamageAfter(), banner);
        }
    }

    public static void printConsumeResult(Player.ConsumeResult result, String requestedItemName, boolean banner) {
        if (result == null) {
            printActionMessage("You do not have '" + requestedItemName + "'.", banner);
            return;
        }

        switch (result.getStatus()) {
            case FULL_HEALTH -> printActionMessage("You are already at full health.", banner);
            case NOT_OWNED -> printActionMessage("You do not have '" + requestedItemName + "'.", banner);
            case NOT_CONSUMABLE -> printActionMessage(result.getItem().getName() + " cannot be consumed.", banner);
            case CONSUMED -> printActionMessage(
                    "Used " + result.getItem().getName() + ". Restored "
                            + result.getRestoredAmount() + " HP. Health: "
                            + result.getHealthAfter() + "/" + result.getMaxHealth(),
                    banner);
        }
    }

    private static void printActionMessage(String message, boolean banner) {
        if (banner) {
            printLine("\n=== " + message + " ===");
        } else {
            printLine(message);
        }
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
        printLine("\n=== " + monsterName + " dropped: " + itemName + " ===");
        printLine("Use TAKE to pick it up.");
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

    public static void printFinalVerdictPrompt() {
        printLine("");
        printLine("Based on your investigation, do you find Mrs. Blackwood guilty?");
        printLine("");
        printLine("Type GUILTY to convict | Type NOT GUILTY to acquit");
    }

    public static void printVictorySequence(boolean guiltyVerdict) {
        printLine("");
        printLine(DIVIDER);
        printLine("");
        printLine("=== CASE CLOSED ===");
        printLine("");
        if (guiltyVerdict) {
            printLine("Mrs. Blackwood goes still.");
            printLine("You read the case record aloud, and the composure drains from her face.");
            printLine("The guests were never random victims. They were witnesses,");
            printLine("lured in and silenced. Every puzzle, every locked room, every");
            printLine("planted clue was a stage she built to bury the truth.");
            printLine("");
            printLine("You present the final sequence. The signed file.");
            printLine("The Conservatory doors unlock. The house goes quiet.");
            printLine("");
            printLine("By morning, the police have enough to reopen every closed report.");
            printLine("Eleanor Blackwood does not leave the estate a free woman.");
            printLine("");
            printLine("The portraits are just portraits now. The rooms are just rooms.");
            printLine("The Blackwood mystery is finally solved.");
        } else {
            printLine("Mrs. Blackwood goes still.");
            printLine("Your hesitation fills the Conservatory.");
            printLine("You read the case record aloud. She gives a deceptive smile.");
            printLine("");
            printLine("The doors unlock. The house exhales.");
            printLine("And then the disappearances continue.");
            printLine("");
            printLine("Another guest. Then another. The police search, and the trail");
            printLine("folds back into the same dark walls every time.");
            printLine("");
            printLine("Without a conviction, the mystery outlives your investigation.");
            printLine("Blackwood Mansion keeps its secrets.");
        }
        printLine("");
        if (guiltyVerdict) {
            printLine("You walk out as dawn breaks through the glass roof.");
            printLine("The Blackwood mystery is finally over.");
        } else {
            printLine("You walk out as dawn breaks through the glass roof.");
            printLine("The Blackwood mystery is never solved.");
        }
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
