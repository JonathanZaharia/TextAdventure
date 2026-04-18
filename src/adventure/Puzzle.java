package adventure;

public class Puzzle {

    private final String name;
    private final String description;
    private final String correctAnswer;
    private final String successMessage;
    private final String rewardItemName;  // item granted on solve, empty if none
    private final int allowedAttempts;
    private final int roomNumber;

    private int remainingAttempts;
    private boolean solved;
    private boolean failedForThisVisit;

    public Puzzle(String name, String description, String correctAnswer,
                  String successMessage, String rewardItemName,
                  int allowedAttempts, int roomNumber) {
        this.name             = name;
        this.description      = description;
        this.correctAnswer    = correctAnswer;
        this.successMessage   = successMessage;
        this.rewardItemName   = rewardItemName != null ? rewardItemName : "";
        this.allowedAttempts  = allowedAttempts;
        this.remainingAttempts = allowedAttempts;
        this.roomNumber       = roomNumber;
        this.solved           = false;
        this.failedForThisVisit = false;
    }

    public String getName()           { return name; }
    public String getDescription()    { return description; }
    public String getSuccessMessage() { return successMessage; }
    public String getRewardItemName() { return rewardItemName; }
    public boolean hasItemReward()    { return !rewardItemName.isEmpty(); }
    public int  getRoomNumber()       { return roomNumber; }
    public boolean isSolved()         { return solved; }
    public int  getRemainingAttempts(){ return remainingAttempts; }
    public boolean hasAttemptsRemaining() { return remainingAttempts > 0; }
    public boolean isFailedForThisVisit() { return failedForThisVisit; }

    public void setFailedForThisVisit() { this.failedForThisVisit = true; }

    // Called when player leaves a room — resets visit flag and attempts if still unsolved
    public void clearFailedForThisVisit() {
        this.failedForThisVisit = false;
        if (!this.solved) this.remainingAttempts = allowedAttempts;
    }

    // Attempt an answer; updates solved/remainingAttempts automatically
    public boolean attemptAnswer(String userAnswer) {
        if (userAnswer == null) { remainingAttempts--; return false; }

        if (userAnswer.trim().equalsIgnoreCase(correctAnswer.trim())) {
            this.solved = true;
            return true;
        }

        remainingAttempts--;
        return false;
    }

    // Fully reset puzzle (used on game restart)
    public void reset() {
        this.solved             = false;
        this.remainingAttempts  = allowedAttempts;
        this.failedForThisVisit = false;
    }

    // Find a puzzle by room number
    public static Puzzle findByRoomNumber(Puzzle[] puzzles, int roomNumber) {
        if (puzzles == null) return null;
        for (Puzzle puzzle : puzzles) {
            if (puzzle.getRoomNumber() == roomNumber) return puzzle;
        }
        return null;
    }
}