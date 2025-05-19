import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.Behaviour;

import java.util.*;
import java.util.List;

import javax.swing.*;
import java.awt.*;
public class MainAgent extends Agent {

    private GameGUI gui; // GUI for visualizing the grid and players

    public static final int WIDTH = GameConfig.GRID_WIDTH;
    public static final int HEIGHT = GameConfig.GRID_HEIGHT;
    public static final String[] COLORS = GameConfig.COLOR_MAP.keySet().toArray(new String[0]);

    private static final int NUM_PLAYERS = GameConfig.NUM_PLAYERS;


    private Case[][] grid = new Case[HEIGHT][WIDTH];
    private Map<String, PlayerData> players = new HashMap<>();
    private int turnCount = 0;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Initializing the game...");

        initGrid();
        gui = new GameGUI(grid);

        assignPlayers();

        // Start the main game loop
        addBehaviour(new GameBehaviour());
    }

    private void initGrid() {
        Random rand = new Random();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                String color = COLORS[rand.nextInt(COLORS.length)];
                grid[y][x] = new Case(x, y, color);
            }
        }
    }

    private void assignPlayers() {
        Random rand = new Random();
        Set<String> usedPositions = new HashSet<>();

        for (int i = 1; i <= NUM_PLAYERS; i++) {
            String name = "Player" + i;

            int startX, startY, goalX, goalY;
            String startPos;

            // Ensure start position is unique
            do {
                startX = rand.nextInt(WIDTH);
                startY = rand.nextInt(HEIGHT);
                startPos = startX + "," + startY;
            } while (usedPositions.contains(startPos));

            usedPositions.add(startPos);

            do {
                goalX = rand.nextInt(WIDTH);
                goalY = rand.nextInt(HEIGHT);
            } while (goalX == startX && goalY == startY);

            // Assign 5 random tokens
            List<String> tokens = new ArrayList<>();
            for (int t = 0; t < GameConfig.TOKENS_PER_PLAYER; t++) {
                tokens.add(COLORS[rand.nextInt(COLORS.length)]);
            }

            PlayerData playerData = new PlayerData(name, startX, startY, goalX, goalY, tokens);
            players.put(name, playerData);
            gui.updatePlayerPosition(playerData.name, playerData.x, playerData.y, playerData.goalX, playerData.goalY);

            // Send setup info to agent
            ACLMessage setupMsg = new ACLMessage(ACLMessage.INFORM);
            setupMsg.addReceiver(new AID(name, AID.ISLOCALNAME));
            setupMsg.setConversationId("init");
            setupMsg.setContent(playerData.toMessage());
            send(setupMsg);
        }
    }


    private class GameBehaviour extends Behaviour {

        private boolean gameOver = false;
        private int currentPlayerIndex = 1; // start with Player1

        @Override
        public void action() {
            if (gameOver) return;

            turnCount++;

            String currentPlayer = "Player" + currentPlayerIndex;
            System.out.println(String.format("=== Turn %d: %s's move ===", turnCount, currentPlayer));
            


            PlayerData pdata = players.get(currentPlayer);
            // Calculate next position towards goal
            int nextX = pdata.x, nextY = pdata.y;
            if (pdata.x < pdata.goalX) nextX++;
            else if (pdata.x > pdata.goalX) nextX--;
            else if (pdata.y < pdata.goalY) nextY++;
            else if (pdata.y > pdata.goalY) nextY--;

            // Get color of next cell from grid
            String nextColor = grid[nextY][nextX].getColor();

            ACLMessage turnMsg = new ACLMessage(ACLMessage.REQUEST);
            turnMsg.addReceiver(new AID(currentPlayer, AID.ISLOCALNAME));
            turnMsg.setConversationId("your-turn");

            // Send required color as content
            turnMsg.setContent(nextColor);
            send(turnMsg);



            ACLMessage reply = blockingReceive();

            if (reply != null && reply.getConversationId().equals("turn-result")) {
                String[] data = reply.getContent().split(";");

                int x = Integer.parseInt(data[0]);
                int y = Integer.parseInt(data[1]);
                List<String> updatedTokens = data[2].isEmpty() ? new ArrayList<>() : Arrays.asList(data[2].split(","));
                boolean wasBlocked = data.length > 3 && data[3].equalsIgnoreCase("BLOCKED");

                pdata.setX(x);
                pdata.setY(y);
                pdata.setTokens(new ArrayList<>(updatedTokens));

                if (wasBlocked) {
                    pdata.incrementBlockCount();
                    System.out.println(String.format("[Blocked] %s is blocked %d time(s) consecutively.", currentPlayer, pdata.getBlockCount()));
                }

                gui.updatePlayerPosition(pdata.name, pdata.x, pdata.y, pdata.goalX, pdata.goalY);

                if (pdata.isAtGoal()) {
                    System.out.println("🏁 " + getLocalName() + ": Reached the goal at (" + x + "," + y + ")! Victory is mine! 🎉");

                    // Create a prettier centered label
                    JLabel label = new JLabel(pdata.name + " has reached the goal! 🎯 🎉", SwingConstants.CENTER);
                    label.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    label.setForeground(new Color(34, 139, 34)); // Forest green

                    JOptionPane.showMessageDialog(
                        null,
                        label,
                        "🎉 Game Ended",
                        JOptionPane.PLAIN_MESSAGE
                    );

                    gameOver = true;
                }

                boolean allBlocked = players.values().stream()
                    .allMatch(p -> p.getBlockCount() >= GameConfig.MAX_BLOCKED_TURNS);
                    if (allBlocked) {
                        System.out.println(">>> All players are blocked for"+ GameConfig.MAX_GAME_TURNS + " turns in a row. Game Over.");

                        JLabel blockedLabel = new JLabel("All players are blocked for "+ GameConfig.MAX_GAME_TURNS + " turns.\nIt's a draw.", SwingConstants.CENTER);
                        blockedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        blockedLabel.setForeground(Color.RED);

                        JOptionPane.showMessageDialog(
                            null,
                            blockedLabel,
                            "Game Over - Draw",
                            JOptionPane.WARNING_MESSAGE
                        );

                        gameOver = true;
                    }

                try {
                    Thread.sleep(GameConfig.TURN_DELAY_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Move to next player, looping back to 1
            currentPlayerIndex++;
            if (currentPlayerIndex > NUM_PLAYERS) {
                currentPlayerIndex = 1;
            }
        }

        @Override
        public boolean done() {
            return gameOver;
        }
    }

    // Helper class for player state
    private static class PlayerData {
        private final String name;
        private final int goalX, goalY;
        private int x, y;
        private List<String> tokens;
        private int blockCount = 0; // Track how many times this player was blocked

        public PlayerData(String name, int startX, int startY, int goalX, int goalY, List<String> tokens) {
            this.name = name;
            this.x = startX;
            this.y = startY;
            this.goalX = goalX;
            this.goalY = goalY;
            this.tokens = tokens;
        }

        public boolean isAtGoal() {
            return x == goalX && y == goalY;
        }

        public void incrementBlockCount() {
            blockCount++;
        }

        public int getBlockCount() {
            return blockCount;
        }

        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
        public void setTokens(List<String> tokens) { this.tokens = tokens; }

        public String toMessage() {
            return x + "," + y + ";" + goalX + "," + goalY + ";" + String.join(",", tokens);
        }
    }

}
