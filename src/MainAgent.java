import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.Behaviour;

import java.util.*;
import java.util.List;

import javax.swing.*;
import java.awt.*;

public class MainAgent extends Agent {

    private GameGUI gui;

    public static final int WIDTH = GameConfig.GRID_WIDTH;
    public static final int HEIGHT = GameConfig.GRID_HEIGHT;
    public static final String[] COLORS = GameConfig.COLOR_MAP.keySet().toArray(new String[0]);

    // Remove NUM_PLAYERS static, get dynamically from playerNames list
    private java.util.List<String> playerNames = new ArrayList<>();

    private Case[][] grid = new Case[HEIGHT][WIDTH];
    private Map<String, PlayerData> players = new HashMap<>();
    private int turnCount = 0;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Initializing the game...");

        initGrid();
        gui = new GameGUI(grid);

        assignPlayers();

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

        // Generate player names dynamically
        for (int i = 1; i <= GameConfig.NUM_PLAYERS; i++) {
            playerNames.add("Player" + i);
        }

        for (String name : playerNames) {
            int startX, startY, goalX, goalY;
            String startPos;

            // Ensure unique start position
            do {
                startX = rand.nextInt(WIDTH);
                startY = rand.nextInt(HEIGHT);
                startPos = startX + "," + startY;
            } while (usedPositions.contains(startPos));

            usedPositions.add(startPos);

            // Ensure goal position different from start
            do {
                goalX = rand.nextInt(WIDTH);
                goalY = rand.nextInt(HEIGHT);
            } while (goalX == startX && goalY == startY);

            // Assign tokens
            java.util.List<String> tokens = new ArrayList<>();
            for (int t = 0; t < GameConfig.TOKENS_PER_PLAYER; t++) {
                tokens.add(COLORS[rand.nextInt(COLORS.length)]);
            }

            PlayerData playerData = new PlayerData(name, startX, startY, goalX, goalY, tokens);
            players.put(name, playerData);
            gui.updatePlayerPosition(playerData.name, playerData.x, playerData.y, playerData.goalX, playerData.goalY);

            // Send init message including all player names
            ACLMessage setupMsg = new ACLMessage(ACLMessage.INFORM);
            setupMsg.addReceiver(new AID(name, AID.ISLOCALNAME));
            setupMsg.setConversationId("init");

            // Compose content with start, goal, tokens, and player list
            // Format: "startX,startY;goalX,goalY;token1,token2,...;Player1,Player2,..."
            String content = playerData.toMessage() + ";" + String.join(",", playerNames);
            setupMsg.setContent(content);

            send(setupMsg);
        }
    }

    private class GameBehaviour extends Behaviour {

        private boolean gameOver = false;
        private int currentPlayerIndex = 0; // index in playerNames list, start from 0

        @Override
        public void action() {
            if (gameOver) return;

            turnCount++;

            String currentPlayer = playerNames.get(currentPlayerIndex);
            System.out.println(String.format("=== Turn %d: %s's move ===", turnCount, currentPlayer));

            PlayerData pdata = players.get(currentPlayer);

            // Calculate next position towards goal (same logic as before)
            int nextX = pdata.x, nextY = pdata.y;
            if (pdata.x < pdata.goalX) nextX++;
            else if (pdata.x > pdata.goalX) nextX--;
            else if (pdata.y < pdata.goalY) nextY++;
            else if (pdata.y > pdata.goalY) nextY--;

            // Get color of next cell
            String nextColor = grid[nextY][nextX].getColor();

            ACLMessage turnMsg = new ACLMessage(ACLMessage.REQUEST);
            turnMsg.addReceiver(new AID(currentPlayer, AID.ISLOCALNAME));
            turnMsg.setConversationId("your-turn");
            turnMsg.setContent(nextColor);
            send(turnMsg);

            ACLMessage reply = blockingReceive();

            if (reply != null && reply.getConversationId().equals("turn-result")) {
                String[] data = reply.getContent().split(";");

                int x = Integer.parseInt(data[0]);
                int y = Integer.parseInt(data[1]);
                java.util.List<String> updatedTokens = data[2].isEmpty() ? new ArrayList<>() : Arrays.asList(data[2].split(","));
                boolean wasBlocked = data.length > 3 && data[3].equalsIgnoreCase("BLOCKED");

                pdata.setX(x);
                pdata.setY(y);
                pdata.setTokens(new ArrayList<>(updatedTokens));

                if (wasBlocked) {
                    pdata.incrementBlockCount();
                    System.out.println("[Blocked] " + currentPlayer + " (" + pdata.getBlockCount() + ")");
                } else {
                    pdata.resetBlockCount();
                }

                gui.updatePlayerPosition(pdata.name, pdata.x, pdata.y, pdata.goalX, pdata.goalY);

                if (pdata.isAtGoal()) {
                    
                    System.out.println("🏁 " + currentPlayer + " reached the goal! 🎉");

                    JLabel label = new JLabel(currentPlayer + " has reached the goal! 🎯 🎉", SwingConstants.CENTER);
                    label.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    label.setForeground(new Color(34, 139, 34));

                    JOptionPane.showMessageDialog(
                        null,
                        label,
                        "🎉 Game Ended",
                        JOptionPane.PLAIN_MESSAGE
                    );

                    gameOver = true;
                    doDelete();
                    System.exit(0);
                    return;
                }

                boolean allBlocked = players.values().stream()
                        .allMatch(p -> p.getBlockCount() >= GameConfig.MAX_BLOCKED_TURNS);
                if (allBlocked) {
                    System.out.println(">>> All players are blocked for " + GameConfig.MAX_BLOCKED_TURNS + " turns in a row. Game Over.");

                    JLabel blockedLabel = new JLabel("All players are blocked for " + GameConfig.MAX_BLOCKED_TURNS + " turns.\nIt's a draw.", SwingConstants.CENTER);
                    blockedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    blockedLabel.setForeground(Color.RED);

                    JOptionPane.showMessageDialog(
                        null,
                        blockedLabel,
                        "Game Over - Draw",
                        JOptionPane.WARNING_MESSAGE
                    );

                    gameOver = true;
                    doDelete();
                    System.exit(0);
                    return;
                }

                try {
                    Thread.sleep(GameConfig.TURN_DELAY_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Move to next player index cyclically
            currentPlayerIndex = (currentPlayerIndex + 1) % playerNames.size();
        }

        @Override
        public boolean done() {
            return gameOver;
        }
    }

    // PlayerData with added resetBlockCount method for proper block tracking
    private static class PlayerData {
        private final String name;
        private final int goalX, goalY;
        private int x, y;
        private java.util.List<String> tokens;
        private int blockCount = 0;

        public PlayerData(String name, int startX, int startY, int goalX, int goalY, java.util.List<String> tokens) {
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

        public void resetBlockCount() {
            blockCount = 0;
        }

        public int getBlockCount() {
            return blockCount;
        }

        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
        public void setTokens(java.util.List<String> tokens) { this.tokens = tokens; }

        // Format for init message: startX,startY;goalX,goalY;token1,token2,...
        public String toMessage() {
            return x + "," + y + ";" + goalX + "," + goalY + ";" + String.join(",", tokens);
        }
    }
}
