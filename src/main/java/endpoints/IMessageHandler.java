package endpoints;

import javax.websocket.Session;

public interface IMessageHandler {

    void handleMessage(String message, Session session);
}
