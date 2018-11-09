package client;

import endpoints.GameClientEndPoint;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import javax.websocket.MessageHandler;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GameClient {

    public GameClient(ApplicationClient application) {

        try {
            final GameClientEndPoint gcep = new GameClientEndPoint(new URI("ws://localhost:8025/kingstable/game"));
            gcep.addMessageHandler(new ClientMessageHandler(application));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
