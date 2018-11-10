package server;

import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GameServer {

    private static int port = 8025;

    public static void main(String[] args) {
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        runServer();
    }

    private static void runServer() {
        Server server = new Server("localhost", port, "/kingstable", null, GameServerEndPoint.class);

        try {
            server.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Running on port " + port + ". Please press enter to stop the server.");
            reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}