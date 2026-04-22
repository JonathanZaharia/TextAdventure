package adventure;

public class GameView {

    public static void printLine(String message) {
        System.out.println(message);
    }

    public static void print(String message) {
        System.out.print(message);
    }

    public static void printWelcome() {
        System.out.println("Welcome to Mrs. Blackwood's Mansion!");
        System.out.println("Type HELP for commands.\n");
    }

    public static void displayRoom(Room room, Puzzle[] puzzles, Monster[] monsters) {
        System.out.println("\n--------------------------------------------------");
        System.out.println("Room " + room.getRoomNumber() + ": " + room.getName());
        System.out.println(room.getDescription());
        if (room.isVisited())
            System.out.println("[Visited]");

        if (room.hasItems()) {
            System.out.println(
                    "Items: " + room.getItems().stream().map(Item::getName).reduce((a, b) -> a + ", " + b).orElse(""));
        }

        Monster m = Monster.findActiveByRoomNumber(monsters, room.getRoomNumber());
        if (m != null)
            System.out.println("[WARNING: " + m.getName() + " is here!]");

        Puzzle p = Puzzle.findByRoomNumber(puzzles, room.getRoomNumber());
        if (p != null && !p.isSolved())
            System.out.println("[There is a puzzle here. Type SOLVE to attempt it.]");

        System.out.print("Exits: ");
        System.out.println(room.getExits().isEmpty() ? "None" : String.join(", ", room.getExits().keySet()));
    }

    public static void displayInventory(Player player) {
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

    public static void printHelp() {
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

    public static void handleInspect(Player player, Room room, Monster[] monsters, String target) {
        // Check room items first
        Item roomItem = room.findItem(target);
        if (roomItem != null) {
            System.out.println(roomItem.getName() + ": " + roomItem.getDescription());
            return;
        }

        // Check inventory items
        Item invItem = player.getItemByName(target);
        if (invItem != null) {
            System.out.println(invItem.getName() + ": " + invItem.getDescription());
            return;
        }

        // Check for active monster in room
        Monster monster = Monster.findActiveByRoomNumber(monsters, room.getRoomNumber());
        if (monster != null && monster.getName().equalsIgnoreCase(target)) {
            System.out.println(monster.getName() + ": " + monster.getDescription());
            System.out.println("Attack: " + monster.getAttackDamage() + " | HP: " + monster.getCurrentHealth() + "/"
                    + monster.getMaxHealth());
            return;
        }

        System.out.println("There is nothing like that to inspect here.");
    }

    public static void showGameOverMenu() {
        System.out.println("\n=== GAME OVER ===");
        System.out.println("1. Start new game   2. Exit");
    }
}
