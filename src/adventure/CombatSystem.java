package adventure;

import java.util.List;
import java.util.Scanner;

public final class CombatSystem {

    private CombatSystem() {
        // Utility class
    }

    // Called on room entry - player chooses to attack or ignore.
    public static boolean handleMonsterEncounter(Monster monster, List<Item> allItems, Player player, Room room,
            Scanner input) {
        System.out.println("\n! A " + monster.getName() + " is here !");
        System.out.println(monster.getDescription());
        System.out.println("Type ATTACK to fight or IGNORE to back away.");

        while (true) {
            System.out.print("Choice: ");
            String choice = input.nextLine().trim().toUpperCase();

            if (choice.equals("ATTACK")) {
                return handleCombat(monster, allItems, player, room, input);
            }

            if (choice.equals("IGNORE")) {
                monster.setIgnored();
                System.out.println("You back away. The " + monster.getName() + " will not appear again.");
                return true;
            }

            System.out.println("Type ATTACK or IGNORE.");
        }
    }

    // Turn-based combat loop; player can also equip/heal during combat.
    public static boolean handleCombat(Monster monster, List<Item> allItems, Player player, Room room, Scanner input) {
        System.out.println("\n--- COMBAT: " + monster.getName() + " ---");

        while (!monster.isDead() && !player.isDead()) {
            System.out.println("\nYour HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                    + "  |  " + monster.getName() + " HP: " + monster.getCurrentHealth());
            System.out.print("Action (ATTACK / HEAL [item] / EQUIP [item] / UNEQUIP / INVENTORY): ");

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
                    System.out.println("You deal " + damage + " damage. " + monster.getName() + " HP: "
                            + monster.getCurrentHealth());

                    if (monster.isDead()) {
                        System.out.println("You defeated the " + monster.getName() + "!");
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
                case "INVENTORY", "INV" -> {
                    if (player.isInventoryEmpty()) {
                        System.out.println("\n--- Inventory ---\n  (empty)");
                    } else {
                        System.out.println("\n--- Inventory ---");
                        for (Item item : player.getInventory()) {
                            String tag = (player.getEquippedItem() == item) ? " [EQUIPPED]" : "";
                            System.out.println("  - " + item.getName() + " (" + item.getType() + ")" + tag);
                        }
                    }
                    System.out.println("Health: " + player.getCurrentHealth() + "/" + player.getMaxHealth()
                            + "  |  Attack: " + player.getAttackDamage());
                }
                default -> System.out.println("Unknown combat command.");
            }
        }

        return !player.isDead();
    }

    // Retains the extracted monster attack hook from Main's combat flow.
    public static void monsterAttack(Monster monster, Player player) {
        monster.monsterAttack(player);
    }
}
