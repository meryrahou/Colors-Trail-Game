import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class GameGUI extends JFrame {

    private final Map<String, Point> playerGoals = new HashMap<>();
    private final Map<String, Point> playerPositions = new HashMap<>();
    private final Map<String, List<Point>> playerTrails = new HashMap<>();

    private static final int CELL_SIZE = 100;
    private static final Map<String, Color> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put("Red", new Color(255, 105, 97));    // bright pastel red (coral)
        COLOR_MAP.put("Blue", new Color(100, 149, 237));  // cornflower blue
        COLOR_MAP.put("Green", new Color(144, 238, 144)); // light green (lime-ish)
        COLOR_MAP.put("Yellow", new Color(255, 255, 153)); // soft sunny yellow
    }

    private JPanel[][] cells;

    public GameGUI(Case[][] grid) {
        setTitle("Colored Trails Game");
        setSize(CELL_SIZE * grid[0].length + 50, CELL_SIZE * grid.length + 50);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(grid.length, grid[0].length));

        cells = new JPanel[grid.length][grid[0].length];

        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                JPanel panel = new JPanel();
                panel.setBackground(COLOR_MAP.getOrDefault(grid[y][x].getColor(), Color.LIGHT_GRAY));
                panel.setBorder(BorderFactory.createLineBorder(Color.black));
                panel.setLayout(new OverlayLayout(panel));
                add(panel);
                cells[y][x] = panel;
            }
        }

        setVisible(true);
    }
       
    

    public void updatePlayerPosition(String playerName, int x, int y, int goalX, int goalY) {
        SwingUtilities.invokeLater(() -> {
            resetOverlay();

            // Update player goal and position
            playerGoals.put(playerName, new Point(goalX, goalY));
            playerPositions.put(playerName, new Point(x, y));

            // Update player's trail list
            playerTrails.putIfAbsent(playerName, new ArrayList<>());
            List<Point> trail = playerTrails.get(playerName);
            Point currentPos = new Point(x, y);
            if (trail.isEmpty() || !trail.get(trail.size() - 1).equals(currentPos)) {
                trail.add(currentPos);
            }

            // --- Draw goal overlays ---
            for (Map.Entry<String, Point> entry : playerGoals.entrySet()) {
                String player = entry.getKey();
                Point goal = entry.getValue();
                JPanel goalCell = cells[goal.y][goal.x];
                goalCell.setBorder(BorderFactory.createLineBorder(Color.black, 3));
                goalCell.setLayout(null); // Absolute layout

                // Goal emoji (left-center)
                String flag = switch (player) {
                    case "Player1" -> "🍯";
                    case "Player2" -> "🧀";
                    case "Player3" -> "🌸";
                    case "Player4" -> "🍌";
                    default -> "🏁";
                };
                JLabel flagLabel = new JLabel(flag);
                flagLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                flagLabel.setBounds(CELL_SIZE / 4 - 6, CELL_SIZE / 2 - 16, 32, 32);
                goalCell.add(flagLabel);

                // Goal label (bottom-center): "Goal PlayerX"
                String playerNumber = player.replaceAll("\\D", ""); // Remove non-digits, keep only numbers
                JLabel goalLabel = new JLabel("Goal " + playerNumber);
                goalLabel.setFont(new Font("Arial", Font.BOLD, 10));
                goalLabel.setForeground(Color.BLACK);
                goalLabel.setBounds(0, CELL_SIZE - 20, CELL_SIZE, 15);
                goalLabel.setHorizontalAlignment(SwingConstants.CENTER);
                goalCell.add(goalLabel);
            }

            // --- Draw trail dots ---
            for (Map.Entry<String, List<Point>> trailEntry : playerTrails.entrySet()) {
                Color trailColor = getTrailColor(trailEntry.getKey());
                for (Point pt : trailEntry.getValue()) {
                    JPanel cell = cells[pt.y][pt.x];
                    JLabel dotLabel = new JLabel("•");
                    dotLabel.setFont(new Font("Arial", Font.BOLD, 18));
                    dotLabel.setForeground(trailColor);
                    dotLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    dotLabel.setAlignmentX(0.5f);
                    dotLabel.setAlignmentY(0.5f);
                    cell.add(dotLabel);
                }
            }

            // --- Draw players with name label ---
            for (Map.Entry<String, Point> playerEntry : playerPositions.entrySet()) {
                String player = playerEntry.getKey();
                Point pos = playerEntry.getValue();
                JPanel playerCell = cells[pos.y][pos.x];
                playerCell.setLayout(null); // Absolute layout

                // Player emoji (center-right)
                String emoji = switch (player) {
                    case "Player1" -> "🐻";
                    case "Player2" -> "🐭";
                    case "Player3" -> "🐝";
                    case "Player4" -> "🐵";
                    default -> "❓";
                };
                JLabel playerLabel = new JLabel(emoji);
                playerLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
                playerLabel.setBounds((CELL_SIZE * 3 / 4) - 16, CELL_SIZE / 2 - 16, 32, 32);  // Approach center from right
                playerCell.add(playerLabel);

                // Player name (below emoji)
                JLabel nameLabel = new JLabel(player);
                nameLabel.setFont(new Font("Arial", Font.PLAIN, 10));
                nameLabel.setForeground(Color.DARK_GRAY);
                nameLabel.setBounds(CELL_SIZE - 50, CELL_SIZE / 2 + 18, 45, 15); // Below emoji
                nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                playerCell.add(nameLabel);
            }

            repaint();
        });
    }




    private void resetOverlay() {
        for (JPanel[] row : cells) {
            for (JPanel panel : row) {
                panel.removeAll();
                panel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 220), 1));
                panel.setLayout(new OverlayLayout(panel));

            }
        }
    }

    private Color getTrailColor(String playerName) {
        switch (playerName) {
            case "Player1":
                return new Color(0, 0, 0, 180);        // Semi-transparent black
            case "Player2":
                return new Color(255, 255, 255, 180);  // Semi-transparent white
            case "Player3":
                return new Color(255, 0, 0, 180);      // Semi-transparent red
            case "Player4":
                return new Color(0, 0, 255, 180);      // Semi-transparent blue
            default:
                return Color.GRAY;
        }
    }

}