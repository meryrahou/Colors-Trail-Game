    import jade.core.Agent;
    import jade.lang.acl.ACLMessage;
    import jade.core.behaviours.CyclicBehaviour;
    import jade.core.AID;

    import java.util.*;

    public class PlayerAgent extends Agent {

        private int x, y;
        private int goalX, goalY;
        private int blockedTurns = 0;
        private java.util.List<String> tokens = new ArrayList<>();
        private Map<String, Integer> betrayalCount = new HashMap<>();
        private java.util.List<String> otherPlayers = new ArrayList<>();

        private static final int MAX_BLOCKED_TURNS = 3;  // Or import from GameConfig

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

            /**
             * Initialization message format changed to:
             * "startX,startY;goalX,goalY;token1,token2,...;player1,player2,player3"
             */
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

                // Dynamic list of players received in init message (part 4)
                if (parts.length > 3) {
                    otherPlayers = new ArrayList<>(Arrays.asList(parts[3].split(",")));
                    otherPlayers.remove(getLocalName());
                }

                System.out.println(getLocalName() + " initialized at (" + x + "," + y + "), goal: (" + goalX + "," + goalY + ")");
                System.out.println(getLocalName() + " has tokens: " + tokens);
                System.out.println(getLocalName() + " other players: " + otherPlayers);
            }

            /**
             * Smarter movement: move in direction that reduces Manhattan distance most.
             */
            private void handleTurn(ACLMessage msg) {
                String requiredColor = msg.getContent();
                System.out.println(getLocalName() + ": My turn. Needs '" + requiredColor + "'.");

                int nextX = x;
                int nextY = y;

                int dx = goalX - x;
                int dy = goalY - y;

                if (Math.abs(dx) >= Math.abs(dy)) {
                    nextX += Integer.signum(dx);
                } else {
                    nextY += Integer.signum(dy);
                }

                boolean canMove = tokens.contains(requiredColor);

                if (canMove) {
                    x = nextX;
                    y = nextY;
                    tokens.remove(requiredColor);
                    blockedTurns = 0;
                    System.out.println(getLocalName() + " moved to (" + x + "," + y + ") using '" + requiredColor + "'.");
                } else {
                    blockedTurns++;

                    if (blockedTurns >= MAX_BLOCKED_TURNS) {
                        System.out.println(getLocalName() + " blocked " + MAX_BLOCKED_TURNS + " times. Ending game.");
                        sendResult(false);
                        return;
                    }

                    // Choose player to propose to (prefer those who betrayed less)
                    String other = selectPlayerToTrade();

                    System.out.println(getLocalName() + " blocked (" + blockedTurns + "). Needs '" + requiredColor + "'. Proposing trade to " + other);

                    String needed = requiredColor;
                    String offer = selectOfferToken();

                    ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
                    propose.addReceiver(new AID(other, AID.ISLOCALNAME));
                    propose.setConversationId("negotiation");
                    propose.setContent("Need:" + needed + ";Offer:" + offer);
                    send(propose);

                    ACLMessage response = blockingReceive();
                    if (response != null && response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        System.out.println(getLocalName() + " received accepted proposal from " + other + ".");

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

            /**
             * Pick player to propose trade to, preferring those with fewer betrayals.
             */
            private String selectPlayerToTrade() {
                otherPlayers.sort(Comparator.comparingInt(p -> betrayalCount.getOrDefault(p, 0)));
                // Pick the player with least betrayals so far
                return otherPlayers.get(0);
            }

            /**
             * Smarter token selection: 
             * Offer token that is not needed or least useful for the agent.
             * If no tokens, offer "NONE".
             */
            private String selectOfferToken() {
                if (tokens.isEmpty()) return "NONE";

                // Simple heuristic: offer token with max count or random for now
                Map<String, Integer> tokenCounts = new HashMap<>();
                for (String t : tokens) tokenCounts.put(t, tokenCounts.getOrDefault(t, 0) + 1);

                // Find token with highest count (if duplicates)
                String bestOffer = tokens.get(0);
                int maxCount = 1;
                for (Map.Entry<String, Integer> e : tokenCounts.entrySet()) {
                    if (e.getValue() > maxCount) {
                        maxCount = e.getValue();
                        bestOffer = e.getKey();
                    }
                }
                return bestOffer;
            }

            /**
             * Handle incoming trade proposal: accept only if you have the needed token.
             */
            private void handleProposal(ACLMessage msg) {
                String content = msg.getContent(); // e.g. "Need:Blue;Offer:Green"
                String[] parts = content.split(";");
                String need = parts[0].split(":")[1];
                String offer = parts[1].split(":")[1];

                // If this sender has betrayed too often, reject immediately
                String sender = msg.getSender().getLocalName();
                if (betrayalCount.getOrDefault(sender, 0) >= 2) {
                    ACLMessage rejectReply = msg.createReply();
                    rejectReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    rejectReply.setContent("NONE");
                    System.out.println(getLocalName() + " rejected proposal from " + sender + " due to repeated betrayal.");
                    send(rejectReply);
                    return;
                }

                boolean accept = tokens.contains(need);

                ACLMessage reply = msg.createReply();
                if (accept) {
                    tokens.remove(need);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    reply.setContent(need);
                    System.out.println(getLocalName() + " accepted proposal from " + sender + ", sent token: " + need);
                } else {
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    reply.setContent("NONE");
                    System.out.println(getLocalName() + " rejected proposal from " + sender + " (token needed not available).");
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
