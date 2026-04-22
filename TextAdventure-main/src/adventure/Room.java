package adventure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {

    private final int roomNumber;
    private final String name;
    private final String description;
    private boolean visited;
    private boolean locked;
    private String requiredItemName;  // item needed to unlock, empty if not locked

    private final Map<String, Integer> exits;  // direction -> destination room number
    private final ArrayList<Item> items;       // original had one Item; expanded to list

    public Room(int roomNumber, String name, String description) {
        this.roomNumber      = roomNumber;
        this.name            = name;
        this.description     = description;
        this.visited         = false;
        this.locked          = false;
        this.requiredItemName = "";
        this.exits           = new HashMap<>();
        this.items           = new ArrayList<>();
    }

    public int    getRoomNumber() { return roomNumber; }
    public String getName()       { return name; }
    public String getDescription(){ return description; }
    public boolean isVisited()    { return visited; }
    public boolean isLocked()     { return locked; }
    public String getRequiredItemName() { return requiredItemName; }

    public void markVisited()              { this.visited = true; }
    public void setVisited(boolean visited){ this.visited = visited; }

    // Lock this room, requiring the named item to enter
    public void setLocked(String requiredItemName) {
        this.locked           = true;
        this.requiredItemName = requiredItemName != null ? requiredItemName : "";
    }

    public void unlock() {
        this.locked           = false;
        this.requiredItemName = "";
    }

    // Returns true if player has the required item (or room is not locked)
    public boolean canPlayerEnter(Player player) {
        if (!locked) return true;
        return player.getItemByName(requiredItemName) != null;
    }

    // --- Exits ---

    public void addExit(String direction, int destinationRoomNumber) {
        if (destinationRoomNumber > 0) exits.put(direction.toUpperCase(), destinationRoomNumber);
    }

    public int getExit(String direction) {
        if (direction == null) return 0;
        Integer dest = exits.get(direction.toUpperCase());
        return dest == null ? 0 : dest;
    }

    public Map<String, Integer> getExits() { return new HashMap<>(exits); }

    // --- Items ---

    public boolean addItem(Item item) {
        if (item != null && !items.contains(item)) {
            items.add(item);
            return true;
        }
        return false;
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public Item findItem(String itemName) {
        if (itemName == null) return null;
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(itemName)) return item;
        }
        return null;
    }

    public boolean hasItems()      { return !items.isEmpty(); }
    public List<Item> getItems()   { return items; }
}