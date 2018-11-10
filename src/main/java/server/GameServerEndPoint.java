package server;

import org.json.JSONObject;
import shared.IMessageHandler;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/game")
public class GameServerEndPoint {

    private ServerMessageHandler messageHandler = new ServerMessageHandler();

    @OnOpen
    public void onOpen(Session session) {
        // Add user
        messageHandler.joinGame(session);
    }

    @OnClose
    public void onClose(Session session) {
        // Remove user
        messageHandler.leaveGame(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {

//        System.out.println(session.getId() + ":" + message);

        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message, session);
        }
    }
}
