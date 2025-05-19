import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;                     // ✅ Needed for agent identifiers
import jade.lang.acl.ACLMessage;

import java.util.*;

public class PlayerAgent extends Agent {

    private int x, y;
    private int goalX, goalY;
    private int blockedTurns = 0;
    private List<String> tokens = new ArrayList<>();
    private Map<String, Integer> betrayalCount = new HashMap<>(); // ✅ TRACKS betrayals per agent

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Starting...");
        addBehaviour(new MessageHandler());
    }

    private class MessageHandler extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg == null) {
                block();
                return;
            }

            switch (msg.getConversationId()) {
                case "init":
                    handleInit(msg.getContent());
                    break;
                case "your-turn":
                    handleTurn(msg);
                    break;
                case "negotiation":
                    handleProposal(msg);
                    break;
                default:
                    break;
            }
        }

        private void handleInit(String content) {
            String[] parts = content.split(";");
            String[] startCoords = parts[0].split(",");
            String[] goalCoords = parts[1].split(",");
            String[] tokenArray = parts[2].split(",");

            x = Integer.parseInt(startCoords[0]);
            y = Integer.parseInt(startCoords[1]);
            goalX = Integer.parseInt(goalCoords[0]);
            goalY = Integer.parseInt(goalCoords[1]);
            tokens = new ArrayList<>(Arrays.asList(tokenArray));

            System.out.println(getLocalName() + " initialized at (" + x + "," + y + "), goal: (" + goalX + "," + goalY + ")");
            System.out.println(getLocalName() + " has tokens: " + tokens);
        }

        private void handleTurn(ACLMessage msg) {
            String requiredColor = msg.getContent();
            System.out.println(getLocalName() + ": My turn. Needs '" + requiredColor + "'.");

            int nextX = x, nextY = y;
            if (x < goalX) nextX++;
            else if (x > goalX) nextX--;
            else if (y < goalY) nextY++;
            else if (y > goalY) nextY--;

            boolean canMove = tokens.contains(requiredColor);

            if (canMove) {
                x = nextX;
                y = nextY;
                tokens.remove(requiredColor);
                blockedTurns = 0;
                System.out.println(getLocalName() + " moved to (" + x + "," + y + ") using '" + requiredColor + "'.");
            } else {
                blockedTurns++;

                List<String> otherPlayers = new ArrayList<>(Arrays.asList("Player1", "Player2", "Player3", "Player4"));
                otherPlayers.remove(getLocalName());
                String other = otherPlayers.get(new Random().nextInt(otherPlayers.size()));

                System.out.println(getLocalName() + " blocked (" + blockedTurns + "). Needs '" + requiredColor + "'.");
                System.out.println(getLocalName() + " proposes trade to " + other);

                if (blockedTurns >= GameConfig.MAX_BLOCKED_TURNS) {
                    System.out.println(getLocalName() + " blocked " + GameConfig.MAX_BLOCKED_TURNS + " times. Ending game.");
                    sendResult(false);
                    return;
                }

                String needed = requiredColor;
                String offer = tokens.isEmpty() ? "NONE" : tokens.get(0);

                ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
                propose.addReceiver(new AID(other, AID.ISLOCALNAME));
                propose.setConversationId("negotiation");
                propose.setContent("Need:" + needed + ";Offer:" + offer);
                send(propose);

                ACLMessage response = blockingReceive();
                if (response != null && response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println(getLocalName() + " received accepted proposal.");

                    boolean honest = new Random().nextBoolean();
                    if (honest && !offer.equals("NONE")) {
                        tokens.remove(offer);
                        System.out.println(getLocalName() + " sent token: '" + offer + "'");
                    } else {
                        System.out.println(getLocalName() + " betrayed and sent nothing!");
                        betrayalCount.put(other, betrayalCount.getOrDefault(other, 0) + 1);
                    }

                    String tokenGiven = response.getContent();
                    if (!tokenGiven.equals("NONE")) {
                        tokens.add(tokenGiven);
                        System.out.println(getLocalName() + " received token: '" + tokenGiven + "'");
                    }
                } else {
                    System.out.println(getLocalName() + " negotiation rejected or timed out.");
                }
            }

            sendResult(true);
            System.out.println(getLocalName() + " now holds: " + tokens);
        }

        private void handleProposal(ACLMessage msg) {
            String content = msg.getContent(); // e.g. "Need:Blue;Offer:Green"
            String[] parts = content.split(";");
            String need = parts[0].split(":")[1];
            String offer = parts[1].split(":")[1];

            boolean accept = tokens.contains(need);

            ACLMessage reply = msg.createReply();
            if (accept) {
                tokens.remove(need);
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(need);
                System.out.println(getLocalName() + " accepted proposal, sent token: " + need);
            } else {
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                reply.setContent("NONE");
                System.out.println(getLocalName() + " rejected proposal.");
            }

            send(reply);
        }

        private void sendResult(boolean stillPlaying) {
            ACLMessage result = new ACLMessage(ACLMessage.INFORM);
            result.addReceiver(new AID("MainAgent", AID.ISLOCALNAME));
            result.setConversationId("turn-result");
            result.setContent(x + ";" + y + ";" + String.join(",", tokens) + ";" + (stillPlaying ? "OK" : "BLOCKED"));
            send(result);
        }
    }
}






