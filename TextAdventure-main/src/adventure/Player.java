package adventure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Player {

    private int currentRoomNumber;
    private int maxHealth;
    private int currentHealth;
    private int baseDamage;
    private final ArrayList<Item> inventory;
    private Item equippedItem; // currently equipped weapon, null if none

    public Player(int startingRoomNumber, int maxHealth, int baseDamage) {
        this.currentRoomNumber = startingRoomNumber;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.baseDamage = baseDamage;
        this.inventory = new ArrayList<>();
        this.equippedItem = null;
    }

    public int getCurrentRoomNumber() {
        return currentRoomNumber;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public Item getEquippedItem() {
        return equippedItem;
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public boolean hasItemEquipped() {
        return equippedItem != null;
    }

    public void setCurrentRoomNumber(int roomNumber) {
        this.currentRoomNumber = roomNumber;
    }

    // Move in a direction; returns new room number (unchanged if move is invalid)
    public int move(String direction, Map<Integer, Room> rooms) {
        Room current = rooms.get(currentRoomNumber);
        if (current == null)
            return currentRoomNumber;

        int destination = current.getExit(direction);
        if (destination <= 0 || !rooms.containsKey(destination))
            return currentRoomNumber;

        currentRoomNumber = destination;
        return currentRoomNumber;
    }

    // --- Health ---

    public void takeDamage(int amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    public void resetHealth() {
        currentHealth = maxHealth;
    }

    // --- Combat ---

    // Returns base damage + weapon bonus if equipped
    public int getAttackDamage() {
        if (equippedItem != null && equippedItem.isWeapon()) {
            return baseDamage + equippedItem.getAttackBonus();
        }
        return baseDamage;
    }

    // Equip a weapon from inventory; returns false if item is not a weapon
    public boolean equipItem(Item item) {
        if (item == null || !item.isWeapon() || !inventory.contains(item))
            return false;
        equippedItem = item;
        return true;
    }

    // Unequip current weapon; returns the item that was unequipped (or null)
    public Item unequipItem() {
        Item previous = equippedItem;
        equippedItem = null;
        return previous;
    }

    // --- Inventory ---

    public void addItem(Item item) {
        if (item != null && !inventory.contains(item))
            inventory.add(item);
    }

    public void removeItem(Item item) {
        if (equippedItem == item)
            equippedItem = null;
        inventory.remove(item);
    }

    public boolean hasItem(Item item) {
        return inventory.contains(item);
    }

    public Item getItemByName(String itemName) {
        if (itemName == null)
            return null;
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName))
                return item;
        }
        return null;
    }

    public Item removeItemByName(String itemName) {
        if (itemName == null)
            return null;
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                if (equippedItem == item)
                    equippedItem = null;
                inventory.remove(item);
                return item;
            }
        }
        return null;
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public boolean isInventoryEmpty() {
        return inventory.isEmpty();
    }

    public void clearInventory() {
        inventory.clear();
        equippedItem = null;
    }

    public void pickupItem(Room room, String itemName) {
        Item item = room.findItem(itemName);
        if (item == null) {
            System.out.println("There is no item called '" + itemName + "' here.");
            return;
        }
        addItem(item);
        room.removeItem(item);
        System.out.println(item.getName() + " added to inventory.");
    }

    public void dropItem(Room room, String itemName) {
        Item item = removeItemByName(itemName);
        if (item == null) {
            System.out.println("You do not have '" + itemName + "' in your inventory.");
            return;
        }
        room.addItem(item);
        System.out.println(item.getName() + " dropped in " + room.getName() + ".");
    }

    public void equipWeapon(String itemName) {
        Item item = getItemByName(itemName);
        if (item == null) {
            System.out.println("You do not have '" + itemName + "'.");
            return;
        }
        if (!equipItem(item)) {
            System.out.println(item.getName() + " cannot be equipped - it is not a weapon.");
            return;
        }
        System.out.println(item.getName() + " equipped. Attack: " + getAttackDamage());
    }

    public void unequipWeapon() {
        Item previous = unequipItem();
        if (previous == null) {
            System.out.println("Nothing is equipped.");
            return;
        }
        System.out.println(previous.getName() + " unequipped. Attack: " + getAttackDamage());
    }

    public void consumeHealingItem(String itemName) {
        Item item = getItemByName(itemName);
        if (item == null) {
            System.out.println("You do not have '" + itemName + "'.");
            return;
        }
        if (!item.isConsumable()) {
            System.out.println(item.getName() + " cannot be consumed.");
            return;
        }
        int before = getCurrentHealth();
        heal(item.getHealAmount());
        removeItem(item);
        System.out.println("Used " + item.getName() + ". Restored "
                + (getCurrentHealth() - before) + " HP. Health: "
                + getCurrentHealth() + "/" + getMaxHealth());
    }
}