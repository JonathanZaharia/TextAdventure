package adventure;

import java.util.List;

public class Puzzle {

    private final String name;
    private final String description;
    private final String correctAnswer;
    private final String successMessage;
    private final String rewardItemName;
    private final int allowedAttempts;
    private final int roomNumber;

    private int remainingAttempts;
    private boolean solved;
    private boolean failedForThisSession;

    public Puzzle(String name, String description, String correctAnswer,
            String successMessage, String rewardItemName,
            int allowedAttempts, int roomNumber) {

        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.correctAnswer = correctAnswer != null ? correctAnswer.trim() : "";
        this.successMessage = successMessage != null ? successMessage.trim() : "";
        this.rewardItemName = rewardItemName != null ? rewardItemName.trim() : "";

        // Use configured attempts per solve session
        this.allowedAttempts = allowedAttempts;
        this.remainingAttempts = this.allowedAttempts;

        this.roomNumber = roomNumber;
        this.solved = false;
        this.failedForThisSession = false;
    }

    // ========================
    // GETTERS
    // ========================

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getRewardItemName() {
        return rewardItemName;
    }

    public boolean hasItemReward() {
        return !rewardItemName.isEmpty();
    }

    public int getAllowedAttempts() {
        return allowedAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public boolean isSolved() {
        return solved;
    }

    public boolean hasAttemptsRemaining() {
        return remainingAttempts > 0;
    }

    // ========================
    // SESSION / STATE
    // ========================

    public boolean isFailedForThisSession() {
        return failedForThisSession;
    }

    public void clearFailedForThisSession() {
        failedForThisSession = false;
    }

    // Compatibility
    public boolean isFailedForThisVisit() {
        return failedForThisSession;
    }

    public void setFailedForThisVisit() {
        failedForThisSession = true;
    }

    public void clearFailedForThisVisit() {
        failedForThisSession = false;
    }

    // ========================
    // INPUT CHECKS
    // ========================

    public boolean isBlankAnswer(String answer) {
        return answer == null || answer.trim().isEmpty();
    }

    public boolean matchesAnswer(String answer) {
        if (isBlankAnswer(answer))
            return false;
        return answer.trim().equalsIgnoreCase(correctAnswer);
    }

    // ========================
    // SOLVE SESSION LOGIC
    // ========================

    public void startSolveSession() {
        if (!solved && !failedForThisSession) {
            remainingAttempts = allowedAttempts;
            failedForThisSession = false;
        }
    }

    public boolean attemptSolve(String answer) {

        if (solved || !hasAttemptsRemaining()) {
            return false;
        }

        if (isBlankAnswer(answer)) {
            return false;
        }

        if (matchesAnswer(answer)) {
            solved = true;
            failedForThisSession = false;
            return true;
        }

        remainingAttempts = Math.max(0, remainingAttempts - 1);

        if (!hasAttemptsRemaining()) {
            failedForThisSession = true;
        }

        return false;
    }

    // ========================
    // REWARD LOGIC
    // ========================

    public Item grantReward(Player player, List<Item> allItems) {

        if (!solved || !hasItemReward() || player == null || allItems == null) {
            return null;
        }

        for (Item item : allItems) {
            if (item != null && item.getName().equalsIgnoreCase(rewardItemName)) {

                // ALWAYS give reward (no location check)
                item.setCurrentLocation(0);
                player.addItem(item);
                return item;
            }
        }

        return null;
    }

    // ========================
    // RESET
    // ========================

    public void resetAttemptsForVisit() {
        if (!solved) {
            remainingAttempts = allowedAttempts;
            failedForThisSession = false;
        }
    }

    public void reset() {
        solved = false;
        remainingAttempts = allowedAttempts;
        failedForThisSession = false;
    }

    // ========================
    // FINDER
    // ========================

    public static Puzzle findByRoomNumber(Puzzle[] puzzles, int roomNumber) {

        if (puzzles == null)
            return null;

        for (Puzzle puzzle : puzzles) {
            if (puzzle != null && puzzle.getRoomNumber() == roomNumber) {
                return puzzle;
            }
        }

        return null;
    }
}
