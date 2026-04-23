package adventure;

import java.util.List;
import java.util.Scanner;

public class Puzzle {

    private final String name;
    private final String description;
    private final String correctAnswer;
    private final String successMessage;
    private final String rewardItemName; // item granted on solve, empty if none
    private final int allowedAttempts;
    private final int roomNumber;

    private int remainingAttempts;
    private boolean solved;
    private boolean failedForThisVisit;

    public Puzzle(String name, String description, String correctAnswer,
            String successMessage, String rewardItemName,
            int allowedAttempts, int roomNumber) {
        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.correctAnswer = correctAnswer != null ? correctAnswer.trim() : "";
        this.successMessage = successMessage != null ? successMessage.trim() : "";
        this.rewardItemName = rewardItemName != null ? rewardItemName.trim() : "";
        this.allowedAttempts = Math.max(1, allowedAttempts);
        this.remainingAttempts = this.allowedAttempts;
        this.roomNumber = roomNumber;
        this.solved = false;
        this.failedForThisVisit = false;
    }

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

    public boolean isFailedForThisVisit() {
        return failedForThisVisit;
    }

    public void setFailedForThisVisit() {
        this.failedForThisVisit = true;
    }

    public void clearFailedForThisVisit() {
        this.failedForThisVisit = false;
    }

    public boolean isBlankAnswer(String answer) {
        return answer == null || answer.trim().isEmpty();
    }

    public boolean matchesAnswer(String answer) {
        if (isBlankAnswer(answer)) {
            return false;
        }
        return answer.trim().equalsIgnoreCase(correctAnswer);
    }

    // Pure model logic: checks answer and updates puzzle state
    public boolean attemptSolve(String answer) {
        if (solved || !hasAttemptsRemaining()) {
            return false;
        }

        if (isBlankAnswer(answer)) {
            return false;
        }

        if (matchesAnswer(answer)) {
            solved = true;
            failedForThisVisit = false;
            return true;
        }

        remainingAttempts = Math.max(0, remainingAttempts - 1);

        if (!hasAttemptsRemaining()) {
            failedForThisVisit = true;
        }

        return false;
    }

    // Interactive puzzle flow used by the controller during room entry and SOLVE
    // command.
    public boolean attemptPuzzle(List<Item> allItems, Player player, Scanner input) {
        if (solved) {
            GameView.printLine("This puzzle has already been solved.");
            return true;
        }

        if (!hasAttemptsRemaining()) {
            failedForThisVisit = true;
            GameView.printLine("No attempts remaining for this puzzle.");
            return false;
        }

        GameView.printLine("\n--- Puzzle: " + name + " ---");
        GameView.printLine(description);
        GameView.printLine("\nAttempts remaining: " + remainingAttempts);
        GameView.print("\nAnswer: ");

        String answer = input.nextLine().trim();
        if (isBlankAnswer(answer)) {
            GameView.printLine("No answer entered.");
            return false;
        }

        boolean solvedNow = attemptSolve(answer);
        if (solvedNow) {
            GameView.printLine(successMessage.isEmpty() ? "Puzzle solved!" : successMessage);
            Item reward = grantReward(player, allItems);
            if (reward != null) {
                GameView.printLine("You received: " + reward.getName());
            }
            return true;
        }

        GameView.printLine("Incorrect answer.");
        if (hasAttemptsRemaining()) {
            GameView.printLine("Attempts remaining: " + remainingAttempts);
        } else {
            failedForThisVisit = true;
            GameView.printLine("You have no attempts remaining for this puzzle.");
        }

        return false;
    }

    // Applies reward if puzzle has one and it exists in the item list
    public Item grantReward(Player player, List<Item> allItems) {
        if (!hasItemReward() || player == null || allItems == null) {
            return null;
        }

        for (Item item : allItems) {
            if (item != null && item.getName().equalsIgnoreCase(rewardItemName)) {
                item.setCurrentLocation(0);
                player.addItem(item);
                return item;
            }
        }

        return null;
    }

    public void resetAttemptsForVisit() {
        if (!solved) {
            remainingAttempts = allowedAttempts;
            failedForThisVisit = false;
        }
    }

    public void reset() {
        solved = false;
        remainingAttempts = allowedAttempts;
        failedForThisVisit = false;
    }

    public static Puzzle findByRoomNumber(Puzzle[] puzzles, int roomNumber) {
        if (puzzles == null) {
            return null;
        }

        for (Puzzle puzzle : puzzles) {
            if (puzzle != null && puzzle.getRoomNumber() == roomNumber) {
                return puzzle;
            }
        }

        return null;
    }
}
