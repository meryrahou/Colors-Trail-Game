import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class GameConfig {
    // Number of players in the game (up to 4)
    public static final int NUM_PLAYERS = 4; // can change to 4

    // Grid size (width and height)
    public static final int GRID_WIDTH = 7;
    public static final int GRID_HEIGHT = 5;

    public static final int CELL_SIZE = 100; 
    // Pixel size of each grid cell in the GUI
    
    // Background color for each token color on the grid
    public static final Map<String, Color> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put("Red", new Color(255, 105, 97));    // bright pastel red (coral)
        COLOR_MAP.put("Blue", new Color(100, 149, 237));  // cornflower blue
        COLOR_MAP.put("Green", new Color(144, 238, 144)); // light green (lime-ish)
        COLOR_MAP.put("Yellow", new Color(255, 255, 153)); // soft sunny yellow
    }

    // Delay (milliseconds) between agent turns for visualization
    public static final int TURN_DELAY_MS = 2000;
    
    // Tokens available in the game (4 tokens, one per player max)
    public static final String[] AVAILABLE_TOKENS = {"Red", "Blue", "Green", "Yellow"};
    // Number of tokens each player starts with
    public static final int TOKENS_PER_PLAYER = 5;
    
    // Probability that a player will betray in a token trade (0.0 to 1.0)
    public static final double BETRAYAL_PROBABILITY = 0.9;
    
    
    // Maximum allowed blocked turns before disqualification
    public static final int MAX_BLOCKED_TURNS = 3;
    // Max number of turns before the game ends automatically
    public static final int MAX_GAME_TURNS = 50;

    // Player flags (goal emoji, shown on left-center)
    public static String getFlag(String playerName) {
        return switch (playerName) {
            case "Player1" -> "🍯";
            case "Player2" -> "🧀";
            case "Player3" -> "🌸";
            case "Player4" -> "🍌";
            default -> "🏁";
        };
    }

    // Player emojis (shown center-right)
    public static String getPlayerEmoji(String playerName) {
        return switch (playerName) {
            case "Player1" -> "🐻";
            case "Player2" -> "🐭";
            case "Player3" -> "🐝";
            case "Player4" -> "🐵";
            default -> "❓";
        };
    }

    // Trail colors (semi-transparent colors for player trails)
    public static Color getTrailColor(String playerName) {
        return switch (playerName) {
            case "Player1" -> new Color(0, 0, 0, 180);       // Semi-transparent black
            case "Player2" -> new Color(255, 255, 255, 180); // Semi-transparent white
            case "Player3" -> new Color(255, 0, 0, 180);     // Semi-transparent red
            case "Player4" -> new Color(0, 0, 255, 180);     // Semi-transparent blue
            default -> Color.GRAY;
        };
    }


    // Check if all players are blocked for the max allowed turns
    // Use in game logic: pass the blocked turns count of each player to check
    public static boolean areAllPlayersBlocked(int[] blockedCounts) {
        for (int count : blockedCounts) {
            if (count < MAX_BLOCKED_TURNS) {
                return false;
            }
        }
        return true;
    }
}
