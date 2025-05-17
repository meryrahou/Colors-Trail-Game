import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class StartJADE {

    public static void main(String[] args) {
        // Create JADE runtime
        Runtime rt = Runtime.instance();

        // Create main container profile
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true"); // show JADE GUI

        // Create main container
        AgentContainer mainContainer = rt.createMainContainer(p);

        try {
            // Launch main agent (game manager)
            AgentController mainAgent = mainContainer.createNewAgent("MainAgent", "MainAgent", null);
            mainAgent.start();

            // Launch player agents
            AgentController player1 = mainContainer.createNewAgent("Player1", "PlayerAgent", new Object[]{"1"});
            AgentController player2 = mainContainer.createNewAgent("Player2", "PlayerAgent", new Object[]{"2"});

            player1.start();
            player2.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
