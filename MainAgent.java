import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.Behaviour;

import java.util.*;

public class MainAgent extends Agent {

    public static final int WIDTH = 7;
    public static final int HEIGHT = 5;
    public static final String[] COLORS = {"Red", "Blue", "Green", "Yellow"};

    private Case[][] grid = new Case[HEIGHT][WIDTH];
    private Map<String, PlayerData> players = new HashMap<>();
    private int turnCount = 0;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Initializing the game...");

        initGrid();
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

        // Define two players
        for (int i = 1; i <= 2; i++) {
            String name = "Player" + i;
            int startX = rand.nextInt(WIDTH);
            int startY = rand.nextInt(HEIGHT);
            int goalX = rand.nextInt(WIDTH);
            int goalY = rand.nextInt(HEIGHT);

            while (startX == goalX && startY == goalY) {
                goalX = rand.nextInt(WIDTH);
                goalY = rand.nextInt(HEIGHT);
            }

            // Assign 5 random tokens
            List<String> tokens = new ArrayList<>();
            for (int t = 0; t < 5; t++) {
                tokens.add(COLORS[rand.nextInt(COLORS.length)]);
            }

            PlayerData playerData = new PlayerData(name, startX, startY, goalX, goalY, tokens);
            players.put(name, playerData);

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

        @Override
        public void action() {
            if (gameOver) return;

            turnCount++;

            // Alternate turns: Player1 -> Player2
            String currentPlayer = (turnCount % 2 == 1) ? "Player1" : "Player2";
            System.out.println("Turn " + turnCount + ": Asking " + currentPlayer + " to play.");

            ACLMessage turnMsg = new ACLMessage(ACLMessage.REQUEST);
            turnMsg.addReceiver(new AID(currentPlayer, AID.ISLOCALNAME));
            turnMsg.setConversationId("your-turn");
            send(turnMsg);

            // Wait for response
            ACLMessage reply = blockingReceive();

            if (reply != null && reply.getConversationId().equals("turn-result")) {
                String[] data = reply.getContent().split(";");

                int x = Integer.parseInt(data[0]);
                int y = Integer.parseInt(data[1]);
                List<String> updatedTokens = Arrays.asList(data[2].split(","));

                PlayerData pdata = players.get(currentPlayer);
                pdata.setX(x);
                pdata.setY(y);
                pdata.setTokens(new ArrayList<>(updatedTokens));

                if (pdata.isAtGoal()) {
                    System.out.println(currentPlayer + " has reached the goal! Game Over.");
                    gameOver = true;
                }

                // TODO: Add scoring logic, block counter, token trading, etc.
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

        public void setX(int x) { this.x = x; }

        public void setY(int y) { this.y = y; }

        public void setTokens(List<String> tokens) { this.tokens = tokens; }

        public String toMessage() {
            return x + "," + y + ";" + goalX + "," + goalY + ";" + String.join(",", tokens);
        }
    }
}
