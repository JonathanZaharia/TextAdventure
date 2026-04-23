package adventure;

public class Item {

    // Item types used in Items.txt
    public static final String WEAPON     = "WEAPON";
    public static final String CONSUMABLE = "CONSUMABLE";
    public static final String KEY        = "KEY";

    private final String name;
    private final String description;
    private final String type;
    private final int attackBonus;   // bonus damage when equipped (weapons only)
    private final int healAmount;    // HP restored when consumed (consumables only)
    private int currentLocation;     // room number, or 0 if in player inventory

    public Item(String name, String description, String type, int attackBonus, int healAmount, int currentLocation) {
        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.type = normalizeType(type);
        this.attackBonus = Math.max(0, attackBonus);
        this.healAmount = Math.max(0, healAmount);
        this.currentLocation = Math.max(0, currentLocation);
    }

    private static String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return CONSUMABLE;
        }

        String normalized = type.trim().toUpperCase();

        switch (normalized) {
            case WEAPON:
            case CONSUMABLE:
            case KEY:
                return normalized;
            default:
                return CONSUMABLE;
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public int getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(int roomNumber) {
        this.currentLocation = Math.max(0, roomNumber);
    }

    public boolean isWeapon() {
        return WEAPON.equals(type);
    }

    public boolean isConsumable() {
        return CONSUMABLE.equals(type);
    }

    public boolean isKey() {
        return KEY.equals(type);
    }

    public boolean isEquippable() {
        return isWeapon();
    }

    public boolean canUseFromInventory() {
        return isConsumable() || isWeapon();
    }

    public boolean isInInventory() {
        return currentLocation == 0;
    }

    // Find an item by name in an array (case-insensitive)
    public static Item findByName(Item[] items, String name) {
        if (items == null || name == null) {
            return null;
        }

        String target = name.trim();
        if (target.isEmpty()) {
            return null;
        }

        for (Item item : items) {
            if (item != null && item.getName() != null && item.getName().equalsIgnoreCase(target)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
