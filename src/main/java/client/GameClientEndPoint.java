package client;

import org.json.JSONObject;
import shared.IMessageHandler;

import javax.websocket.*;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@ClientEndpoint
public class GameClientEndPoint {
    private static final Logger LOGGER = Logger.getLogger(GameClientEndPoint.class.getName());

    private Session userSession = null;

    private IMessageHandler messageHandler;

    GameClientEndPoint(URI endpointURI) {
        boolean failed = true;
        int triesLeft = 3;
        while (triesLeft > 0 && failed) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, endpointURI);
                failed = false;
            } catch (Exception e) {
                triesLeft--;
                if (triesLeft > 0)
                    LOGGER.log(Level.INFO, "Couldn't connect to " + GameClient.hostAddress + ":" + GameClient.port + ". Retrying... " + triesLeft + " tries left");
            }
        }
        if (failed) {
            LOGGER.log(Level.INFO, "Couldn't connect to host. Closing application...");
            System.exit(0);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        LOGGER.log(Level.INFO, "opening websocket");
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        LOGGER.log(Level.INFO, "closing websocket");
        this.userSession = null;
        if (messageHandler != null)
            ((ClientMessageHandler)this.messageHandler).unsubscribe();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message, session);
        }
    }

    void sendMessage(JSONObject json) {
        try {
            userSession.getBasicRemote().sendText(json.toString());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    void addMessageHandler(IMessageHandler messageHandler) { this.messageHandler = messageHandler; }
}
