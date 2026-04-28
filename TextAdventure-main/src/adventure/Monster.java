package adventure;

import java.util.Random;

public class Monster {

    private final String name;
    private final String description;
    private final int maxHealth;
    private int currentHealth;
    private final int attackDamage;
    private final double threshold; // random roll below this = double damage
    private final String dropItemName;// item dropped on defeat, empty if none
    private final int roomNumber;

    private boolean ignored; // true if player chose to ignore this monster

    public Monster(String name, String description, int health, int attackDamage,
            double threshold, String dropItemName, int roomNumber) {
        this.name = name;
        this.description = description;
        this.maxHealth = health;
        this.currentHealth = health;
        this.attackDamage = attackDamage;
        this.threshold = threshold;
        this.dropItemName = dropItemName != null ? dropItemName : "";
        this.roomNumber = roomNumber;
        this.ignored = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getAttackDamage() {
        return attackDamage;
    }

    public double getThreshold() {
        return threshold;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public String getDropItemName() {
        return dropItemName;
    }

    public boolean hasDropItem() {
        return !dropItemName.isEmpty();
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored() {
        this.ignored = true;
    }

    public void clearIgnored() {
        this.ignored = false;
    }

    public void takeDamage(int amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    // Returns double damage if random roll falls below threshold, normal damage
    // otherwise
    public int calculateAttackDamage(double randomRoll) {
        return randomRoll < threshold ? attackDamage * 2 : attackDamage;
    }

    // Find an active (alive, not ignored) monster in a given room
    public static Monster findActiveByRoomNumber(Monster[] monsters, int roomNumber) {
        if (monsters == null)
            return null;
        for (Monster m : monsters) {
            if (m.getRoomNumber() == roomNumber && !m.isDead() && !m.isIgnored())
                return m;
        }
        return null;
    }

    public int monsterAttack() {
        double roll = new Random().nextDouble();
        return calculateAttackDamage(roll);
    }

    public void reset() {
        this.currentHealth = maxHealth;
        this.ignored = false;
    }

    public static Monster findByRoomNumber(Monster[] monsters, int roomNumber) {
        if (monsters == null)
            return null;
        for (Monster m : monsters) {
            if (m.getRoomNumber() == roomNumber) {
                return m;
            }
        }
        return null;
    }
}