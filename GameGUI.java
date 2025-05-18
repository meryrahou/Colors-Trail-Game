import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GameGUI extends JFrame {
    
    private final Map<String, Point> playerGoals = new HashMap<>();

    private static final int CELL_SIZE = 100;
    private static final Color[] COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW
    };
    private static final Map<String, Color> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put("Red", Color.RED);
        COLOR_MAP.put("Blue", Color.BLUE);
        COLOR_MAP.put("Green", Color.GREEN);
        COLOR_MAP.put("Yellow", Color.YELLOW);
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
                panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                add(panel);
                cells[y][x] = panel;
            }
        }

        setVisible(true);
    }


    public void updatePlayerPosition(String playerName, int x, int y, int goalX, int goalY) {
        SwingUtilities.invokeLater(() -> {
            resetOverlay();

            // Track player's goal
            playerGoals.put(playerName, new Point(goalX, goalY));

            // Highlight and label all goal cells
            for (Map.Entry<String, Point> entry : playerGoals.entrySet()) {
                Point goal = entry.getValue();

                cells[goal.y][goal.x].setBorder(BorderFactory.createLineBorder(Color.MAGENTA, 3));

                JLabel goalLabel = new JLabel("Goal " + entry.getKey().substring(entry.getKey().length() - 1));
                goalLabel.setFont(new Font("Arial", Font.BOLD, 10));
                goalLabel.setForeground(Color.MAGENTA);
                goalLabel.setHorizontalAlignment(SwingConstants.CENTER);
                goalLabel.setVerticalAlignment(SwingConstants.BOTTOM);

                cells[goal.y][goal.x].add(goalLabel);
            }


            // Draw all players
            for (Map.Entry<String, Point> playerEntry : playerPositions.entrySet()) {
                String player = playerEntry.getKey();
                Point pos = playerEntry.getValue();

                JLabel playerLabel = new JLabel(player);
                playerLabel.setFont(new Font("Arial", Font.BOLD, 12));
                playerLabel.setForeground(Color.WHITE);

                cells[pos.y][pos.x].add(playerLabel);
                cells[pos.y][pos.x].setBackground(Color.BLACK); // Highlight player cell
            }

            repaint();
        });
    }
    

    private void resetOverlay() {
        for (JPanel[] row : cells) {
            for (JPanel panel : row) {
                panel.removeAll();
                panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }
        }
    }
}
