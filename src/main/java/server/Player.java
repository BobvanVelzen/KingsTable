package server;

import org.json.JSONObject;
import shared.RecieveSendAction;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Player {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    private Session session;
    private String gameId;

    private HashMap<String, RecieveSendAction> actions = new HashMap<>();

    String getGame() {
        return gameId;
    }

    String getId() {
        return session.getId();
    }

    Player(Session session) {
        this.session = session;
    }

    boolean sendMessage(JSONObject packet) {
        try {
            session.getBasicRemote().sendText(packet.toString());
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    void setGame(String gameId) {
        this.gameId = gameId;
    }

    void subscribe(String function, RecieveSendAction action) {
        if (!actions.containsKey(function)) {
            actions.put(function, action);
            System.out.println(getId() + " subscribed to " + function);
        }
    }

    void unsubscribe(String function) {
        if (actions.containsKey(function))
            actions.remove(function);
    }

    RecieveSendAction getSubscription(String function) {
        if (actions.containsKey(function))
            return actions.get(function);
        return null;
    }

    @Override
    public boolean equals(Object other) {
        Player player = (Player) other;
        if (player == null)
            return false;

        return session.getId() == player.getId();
    }
}
