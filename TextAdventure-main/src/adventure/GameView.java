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
        System.out.println("             THE BLACKWOOD MANSION INVESTIGATION");
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
        System.out.println("Type HELP for commands. Type GOAL to view your objective.");
        System.out.println();
    }

    public static void displayRoomHeader(Room room) {
        printLine(DIVIDER + "\n");
        printLine("Room " + room.getRoomNumber() + ": " + room.getName());
        printLine("");
        printLine(room.getDescription());
        if (room.isVisited())
            printLine("[Visited]");

        if (room.hasItems()) {
            printLine(
                    "Items: " + room.getItems().stream().map(Item::getName).reduce((a, b) -> a + ", " + b).orElse(""));
        }
    }

    public static void displayRoomFooter(Room room) {
        String exits = room.getExits().isEmpty()
                ? "None"
                : room.getExits().keySet().stream()
                        .map(dir -> switch (dir.toUpperCase()) {
                            case "N" -> "North";
                            case "E" -> "East";
                            case "S" -> "South";
                            case "W" -> "West";
                            default -> dir;
                        })
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("None");
        printLine("Available Exits: " + exits);
        printLine("\nMove with N/E/S/W. Type HELP for more.");
    }

    public static void displayInventory(Player player) {
        printLine("");
        printLine("--- Inventory ---");
        if (player.isInventoryEmpty()) {
            printLine("  (empty)");
        } else {
            for (Item item : player.getInventory()) {
                String tag = (player.getEquippedItem() == item) ? " [EQUIPPED]" : "";
                printLine("  - " + item.getName() + " (" + item.getType() + ")" + tag);
            }
        }
        printLine("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                + "  |  Attack: " + player.getAttackDamage());
    }

    public static void printHelp() {
        printLine("");
        printLine("--- Commands ---");
        printLine("  Move: N/E/S/W");
        printLine("  Look: LOOK or EXPLORE");
        printLine("  Take: TAKE <item>");
        printLine("  Drop: DROP <item>");
        printLine("  Inspect: INSPECT <item>");
        printLine("  Equip: EQUIP <item>");
        printLine("  Unequip: UNEQUIP");
        printLine("  Heal: HEAL <item>");
        printLine("  Attack: ATTACK");
        printLine("  Ignore: IGNORE");
        printLine("  Solve: SOLVE");
        printLine("  Exits: EXITS");
        printLine("  Inventory: INVENTORY or INV");
        printLine("  Health: HEALTH or HP");
        printLine("  Help: HELP");
        printLine("  Quit: QUIT or Q");
    }

    public static void handleInspect(Player player, Room room, Monster[] monsters, String target) {
        // Check room items first
        Item roomItem = room.findItem(target);
        if (roomItem != null) {
            printLine(roomItem.getName() + ": " + roomItem.getDescription());
            return;
        }

        // Check inventory items
        Item invItem = player.getItemByName(target);
        if (invItem != null) {
            printLine(invItem.getName() + ": " + invItem.getDescription());
            return;
        }

        // Check for active monster in room
        Monster monster = Monster.findActiveByRoomNumber(monsters, room.getRoomNumber());
        if (monster != null && monster.getName().equalsIgnoreCase(target)) {
            printLine(monster.getName() + ": " + monster.getDescription());
            printLine("Attack: " + monster.getAttackDamage() + " | HP: " + monster.getCurrentHealth() + "/"
                    + monster.getMaxHealth());
            return;
        }

        printLine("There is nothing like that to inspect here.");
    }

    public static void showGameOverMenu() {
        printLine("");
        printLine("=== GAME OVER ===");
        printLine("1. Start new game   2. Exit");
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
