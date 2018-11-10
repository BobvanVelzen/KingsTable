package server;

import game.IPiece;
import org.json.JSONException;
import org.json.JSONObject;
import shared.IMessageHandler;
import game.Game;
import game.IGame;
import shared.JsonConverter;

import javax.websocket.Session;
import java.awt.*;
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
        JSONObject json = JsonConverter.stringToJson(message);

        try {
            switch (json.getString("function")) {
                default:
                    break;
                case "SYNC_BOARD":
                    syncBoard(session);
                    break;
                case "HANDLE_PIECE":
                    handlePiece(json, session);
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void syncBoard(Session session) {
        IGame game = getGameBySession(session);
        if (game != null)
            game.requestSync(session);
    }

    private void handlePiece(JSONObject json, Session session) {
        IGame game = getGameBySession(session);
        IPiece piece = JsonConverter.getPieceFromJson(json);

        //TODO: pass IPiece as parameter and use that to decide what to do
        if (game != null && piece != null) {
            Point point = new Point(piece.getColumn(), piece.getRow());
            game.selectTile(point, session);
        }
    }

    void joinGame(Session session) {
        String joinedGameId = null;

        for (HashMap.Entry<String, IGame> entry : gameList.entrySet()) {
            if (entry.getValue().addPlayer(session)) {
                joinedGameId = entry.getKey();
                break;
            }
        }
        if (joinedGameId == null) {
            joinedGameId = initGame();
            gameList.get(joinedGameId).addPlayer(session);
        }

        userSessions.put(session.getId(), joinedGameId);
        System.out.println("test_subject_" + session.getId() + " joined game " + joinedGameId);
        try {
            JSONObject json = new JSONObject();
            json.put("function", "INFO");
            json.put("info", "joined game " + joinedGameId);

            session.getBasicRemote().sendText(json.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    void leaveGame(Session session) {
        String userId = session.getId();

        if (userSessions.containsKey(userId)) {
            String gameId = userSessions.get(userId);
            gameList.get(gameId).removeClient(session);
            userSessions.remove(userId);
            System.out.println("test_subject_" + session.getId() + " left game " + gameId);
        }
    }

    private IGame getGameBySession(Session session) {
        String userId = session.getId();
        if (userSessions.containsKey(userId)) {
            String gameId = userSessions.get(userId);

            if (gameList.containsKey(gameId)) {
                return gameList.get(gameId);
            }
        }
        return null;
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
