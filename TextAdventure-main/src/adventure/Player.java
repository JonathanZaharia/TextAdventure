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

    // === Health ===

    public void takeDamage(int amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    public void resetHealth() {
        currentHealth = maxHealth;
    }

    // === Combat ===

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

    // === Inventory ===

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
        return List.copyOf(inventory);
    }

    public boolean isInventoryEmpty() {
        return inventory.isEmpty();
    }

    public void clearInventory() {
        inventory.clear();
        equippedItem = null;
    }

    // ========================
    // PLAYER ACTION RESULTS
    // ========================

    public enum PickupStatus {
        PICKED_UP,
        ITEM_NOT_FOUND
    }

    public static final class PickupResult {
        private final PickupStatus status;
        private final Item item;

        private PickupResult(PickupStatus status, Item item) {
            this.status = status;
            this.item = item;
        }

        public static PickupResult pickedUp(Item item) {
            return new PickupResult(PickupStatus.PICKED_UP, item);
        }

        public static PickupResult itemNotFound() {
            return new PickupResult(PickupStatus.ITEM_NOT_FOUND, null);
        }

        public PickupStatus getStatus() {
            return status;
        }

        public Item getItem() {
            return item;
        }

        public boolean isSuccess() {
            return status == PickupStatus.PICKED_UP;
        }
    }

    public enum DropStatus {
        DROPPED,
        NOT_OWNED
    }

    public static final class DropResult {
        private final DropStatus status;
        private final Item item;

        private DropResult(DropStatus status, Item item) {
            this.status = status;
            this.item = item;
        }

        public static DropResult dropped(Item item) {
            return new DropResult(DropStatus.DROPPED, item);
        }

        public static DropResult notOwned() {
            return new DropResult(DropStatus.NOT_OWNED, null);
        }

        public DropStatus getStatus() {
            return status;
        }

        public Item getItem() {
            return item;
        }

        public boolean isSuccess() {
            return status == DropStatus.DROPPED;
        }
    }

    public enum EquipStatus {
        EQUIPPED,
        NOT_OWNED,
        NOT_WEAPON
    }

    public static final class EquipResult {
        private final EquipStatus status;
        private final Item item;
        private final int attackDamageAfter;

        private EquipResult(EquipStatus status, Item item, int attackDamageAfter) {
            this.status = status;
            this.item = item;
            this.attackDamageAfter = attackDamageAfter;
        }

        public static EquipResult equipped(Item item, int attackDamageAfter) {
            return new EquipResult(EquipStatus.EQUIPPED, item, attackDamageAfter);
        }

        public static EquipResult notOwned() {
            return new EquipResult(EquipStatus.NOT_OWNED, null, -1);
        }

        public static EquipResult notWeapon(Item item) {
            return new EquipResult(EquipStatus.NOT_WEAPON, item, -1);
        }

        public EquipStatus getStatus() {
            return status;
        }

        public Item getItem() {
            return item;
        }

        public int getAttackDamageAfter() {
            return attackDamageAfter;
        }

        public boolean isSuccess() {
            return status == EquipStatus.EQUIPPED;
        }
    }

    public enum UnequipStatus {
        UNEQUIPPED,
        NOTHING_EQUIPPED
    }

    public static final class UnequipResult {
        private final UnequipStatus status;
        private final Item item;
        private final int attackDamageAfter;

        private UnequipResult(UnequipStatus status, Item item, int attackDamageAfter) {
            this.status = status;
            this.item = item;
            this.attackDamageAfter = attackDamageAfter;
        }

        public static UnequipResult unequipped(Item item, int attackDamageAfter) {
            return new UnequipResult(UnequipStatus.UNEQUIPPED, item, attackDamageAfter);
        }

        public static UnequipResult nothingEquipped(int attackDamageAfter) {
            return new UnequipResult(UnequipStatus.NOTHING_EQUIPPED, null, attackDamageAfter);
        }

        public UnequipStatus getStatus() {
            return status;
        }

        public Item getItem() {
            return item;
        }

        public int getAttackDamageAfter() {
            return attackDamageAfter;
        }
    }

    public enum ConsumeStatus {
        CONSUMED,
        FULL_HEALTH,
        NOT_OWNED,
        NOT_CONSUMABLE
    }

    public static final class ConsumeResult {
        private final ConsumeStatus status;
        private final Item item;
        private final int restoredAmount;
        private final int healthAfter;
        private final int maxHealth;

        private ConsumeResult(ConsumeStatus status, Item item,
                int restoredAmount, int healthAfter, int maxHealth) {
            this.status = status;
            this.item = item;
            this.restoredAmount = restoredAmount;
            this.healthAfter = healthAfter;
            this.maxHealth = maxHealth;
        }

        public static ConsumeResult consumed(Item item, int restoredAmount, int healthAfter, int maxHealth) {
            return new ConsumeResult(ConsumeStatus.CONSUMED, item, restoredAmount, healthAfter, maxHealth);
        }

        public static ConsumeResult fullHealth(int healthAfter, int maxHealth) {
            return new ConsumeResult(ConsumeStatus.FULL_HEALTH, null, 0, healthAfter, maxHealth);
        }

        public static ConsumeResult notOwned(int healthAfter, int maxHealth) {
            return new ConsumeResult(ConsumeStatus.NOT_OWNED, null, 0, healthAfter, maxHealth);
        }

        public static ConsumeResult notConsumable(Item item, int healthAfter, int maxHealth) {
            return new ConsumeResult(ConsumeStatus.NOT_CONSUMABLE, item, 0, healthAfter, maxHealth);
        }

        public ConsumeStatus getStatus() {
            return status;
        }

        public Item getItem() {
            return item;
        }

        public int getRestoredAmount() {
            return restoredAmount;
        }

        public int getHealthAfter() {
            return healthAfter;
        }

        public int getMaxHealth() {
            return maxHealth;
        }

        public boolean isSuccess() {
            return status == ConsumeStatus.CONSUMED;
        }
    }

    // ========================
    // ACTIONS (NO UI STRINGS)
    // ========================

    public PickupResult pickupItem(Room room, String itemName) {
        if (room == null || itemName == null || itemName.trim().isEmpty()) {
            return PickupResult.itemNotFound();
        }

        Item item = room.findItem(itemName);
        if (item == null) {
            return PickupResult.itemNotFound();
        }

        addItem(item);
        item.moveToInventory();
        room.removeItem(item);
        return PickupResult.pickedUp(item);
    }

    public DropResult dropItem(Room room, String itemName) {
        if (room == null || itemName == null || itemName.trim().isEmpty()) {
            return DropResult.notOwned();
        }

        Item item = removeItemByName(itemName);
        if (item == null) {
            return DropResult.notOwned();
        }

        room.addItem(item);
        item.moveToRoom(room.getRoomNumber());
        return DropResult.dropped(item);
    }

    public EquipResult equipWeapon(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return EquipResult.notOwned();
        }

        Item item = getItemByName(itemName);
        if (item == null) {
            return EquipResult.notOwned();
        }

        if (!item.isWeapon()) {
            return EquipResult.notWeapon(item);
        }

        if (!equipItem(item)) {
            return EquipResult.notWeapon(item);
        }

        return EquipResult.equipped(item, getAttackDamage());
    }

    public UnequipResult unequipWeapon() {
        Item previous = unequipItem();
        int attackAfter = getAttackDamage();
        if (previous == null) {
            return UnequipResult.nothingEquipped(attackAfter);
        }
        return UnequipResult.unequipped(previous, attackAfter);
    }

    public ConsumeResult consumeHealingItem(String itemName) {
        if (currentHealth >= maxHealth) {
            return ConsumeResult.fullHealth(currentHealth, maxHealth);
        }

        if (itemName == null || itemName.trim().isEmpty()) {
            return ConsumeResult.notOwned(currentHealth, maxHealth);
        }

        Item item = getItemByName(itemName);
        if (item == null) {
            return ConsumeResult.notOwned(currentHealth, maxHealth);
        }

        if (!item.isConsumable()) {
            return ConsumeResult.notConsumable(item, currentHealth, maxHealth);
        }

        int before = currentHealth;
        heal(item.getHealAmount());
        removeItem(item);
        int restored = currentHealth - before;
        return ConsumeResult.consumed(item, restored, currentHealth, maxHealth);
    }
}