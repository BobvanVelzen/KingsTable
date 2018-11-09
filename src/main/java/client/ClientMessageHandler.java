package client;

import endpoints.IMessageHandler;

import javax.websocket.Session;
import java.awt.*;
import java.util.List;

public class ClientMessageHandler implements IMessageHandler {

    private ApplicationClient application;

    public ClientMessageHandler(ApplicationClient application) {
        this.application = application;
    }

    @Override
    public void handleMessage(String message, Session session) {
        if (message.startsWith("INFO"))
            info(message.substring(4));
    }

    public void info(String info) {
        System.out.println(info);
    }

    public void syncBoard() {
        application.syncBoard();
    }

    public void drawRange(List<Point> points) {
        application.drawRange(points);
    }
}
