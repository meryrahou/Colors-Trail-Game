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
        p.setParameter(Profile.GUI, "false"); // show JADE GUI

        // Create main container
        AgentContainer mainContainer = rt.createMainContainer(p);

        try {
            // Launch main agent (game manager)
            AgentController mainAgent = mainContainer.createNewAgent("MainAgent", "MainAgent", null);
            mainAgent.start();

            // Launch player agents
            for (int i = 1; i <= GameConfig.NUM_PLAYERS; i++) {
                AgentController player = mainContainer.createNewAgent("Player" + i, "PlayerAgent", new Object[]{String.valueOf(i)});
                player.start();
            }

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
