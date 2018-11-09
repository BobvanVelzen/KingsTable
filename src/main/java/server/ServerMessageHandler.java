package server;

import endpoints.IMessageHandler;
import game.Game;
import game.IGame;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class ServerMessageHandler implements IMessageHandler {

    // Map<gameId, game>
    private static HashMap<String, IGame> gameList = new HashMap<>();
    // Map<sessionId, gameId>
    private static HashMap<String, String> userSessions = new HashMap<>();

    @Override
    public void handleMessage(String message, Session session) {
        if (message == "JOIN_GAME")
            joinGame(session);
        if (message == "LEAVE_GAME")
            leaveGame(session);
    }

    private void joinGame(Session session) {
        String joinedGameId = null;

        for (HashMap.Entry<String, IGame> entry : gameList.entrySet()) {
            if (entry.getValue().addClient(session)) {
                joinedGameId = entry.getKey();
                break;
            }
        }
        if (joinedGameId == null) {
            joinedGameId = initGame();
            gameList.get(joinedGameId).addClient(session);
        }

        userSessions.put(session.getId(), joinedGameId);
        System.out.println("test_subject_" + session.getId() + " joined game " + joinedGameId);
        try {
            session.getBasicRemote().sendText("INFO" + "joined game " + joinedGameId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void leaveGame(Session session) {
        String userId = session.getId();

        if (userSessions.containsKey(userId)) {
            String gameId = userSessions.get(userId);
            userSessions.remove(userId);
            System.out.println("test_subject_" + session.getId() + " left game " + gameId);
        }
    }

    private String initGame() {
        String gameId = UUID.randomUUID().toString();
        while (gameList.containsKey(gameId)){
            gameId = UUID.randomUUID().toString();
        }

        IGame game = new Game();

        gameList.put(gameId, game);
        return gameId;
    }


}
