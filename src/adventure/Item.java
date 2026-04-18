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
        this.name            = name;
        this.description     = description;
        this.type            = type != null ? type.toUpperCase() : CONSUMABLE;
        this.attackBonus     = attackBonus;
        this.healAmount      = healAmount;
        this.currentLocation = currentLocation;
    }

    public String getName()            { return name; }
    public String getDescription()     { return description; }
    public String getType()            { return type; }
    public int    getAttackBonus()     { return attackBonus; }
    public int    getHealAmount()      { return healAmount; }
    public int    getCurrentLocation() { return currentLocation; }

    public void setCurrentLocation(int roomNumber) {
        this.currentLocation = roomNumber;
    }

    public boolean isWeapon()     { return WEAPON.equals(type); }
    public boolean isConsumable() { return CONSUMABLE.equals(type); }
    public boolean isKey()        { return KEY.equals(type); }

    // Find an item by name in an array (case-insensitive)
    public static Item findByName(Item[] items, String name) {
        if (items == null || name == null) return null;
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(name)) return item;
        }
        return null;
    }
}