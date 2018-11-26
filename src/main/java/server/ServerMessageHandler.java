package server;

import shared.RecieveSendAction;
import shared.IPiece;
import org.json.JSONException;
import org.json.JSONObject;
import shared.IMessageHandler;
import shared.JsonConverter;

import javax.websocket.Session;
import java.util.HashMap;
import java.util.UUID;

public class ServerMessageHandler implements IMessageHandler {

    // Map<gameId, game>
    private static HashMap<String, IGame> gameList = new HashMap<>();
    // Map<playerId, Player>
    private static HashMap<String, Player> players = new HashMap<>();

    @Override
    public void handleMessage(String message, Session session) {
        JSONObject json = JsonConverter.stringToJson(message);

        Player player = null;
        if (players.containsKey(session.getId())) {
            player = players.get(session.getId());
        }

        try {
            if (player != null) {
                RecieveSendAction action = player.getSubscription(json.getString("function"));
                // Invoke action if player is subscribed
                if (action != null)
                    action.invoke(json, player);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Player getPlayer(Session session) {
        if (players.containsKey(session.getId()))
            return players.get(session.getId());
        return  null;
    }

    private void syncBoard(Player player) {
        IGame game = getGameByPlayer(player);

        if (game != null && player != null)
            game.requestSync(player);
    }

    private void handlePiece(JSONObject json, Player player) {
        IGame game = getGameByPlayer(player);
        IPiece piece = JsonConverter.getPieceFromJson(json);

        //TODO: pass IPiece as parameter and use that to decide what to do
        if (game != null && player != null && piece != null) {
//            Point point = new Point(piece.getX(), piece.getY());
            game.handlePiece(piece, player);
        }
    }

    void joinGame(Session session) {
        String joinedGameId = null;
        Player player = getPlayer(session);

        // Add player to players if not in
        if (player == null) {
            Player p = new Player(session);
            players.put(p.getId(), p);
            player = p;
        }

        // Return if player is in game
        if (player.getGame() != null)
            return;

        // Try to add player to a game
        for (IGame game : gameList.values()) {
            if (game.addPlayer(player)) {
                joinedGameId = game.getId();
                break;
            }
        }
        // Create new game if player wasn't able to join a game
        if (joinedGameId == null) {
            joinedGameId = initGame();
            gameList.get(joinedGameId).addPlayer(player);
        }

        player.setGame(joinedGameId);

        // Subscribes player to game functions
        player.subscribe("SYNC_BOARD", (json, p) ->{
            syncBoard(p);
        });
        player.subscribe("HANDLE_PIECE", (json, p) ->{
            handlePiece(json, p);
        });

        System.out.println("test_subject_" + session.getId() + " joined game " + joinedGameId);
        try {
            JSONObject json = new JSONObject();
            json.put("function", "INFO");
            json.put("info", "joined game " + joinedGameId);

            player.sendMessage(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void leaveGame(Session session) {
        Player player = getPlayer(session);

        if (player != null) {
            String gameId = player.getGame();

            // Remove player from game
            if (gameList.containsKey(gameId) && gameList.get(gameId).removeClient(player)); {
                gameList.remove(gameId);

                // Unsubscribes player from game functions
                player.unsubscribe("SYNC_BOARD");
                player.unsubscribe("HANDLE_PIECE");
            }
            players.remove(player.getId());
            System.out.println("test_subject_" + player.getId() + " left game " + gameId);
        }
    }

    private IGame getGameByPlayer(Player player) {
        // If player is in players
        if (player != null) {
            String gameId = player.getGame();

            // Gets game if player has gameId
            if (gameId != null && gameList.containsKey(gameId)) {
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

        IGame game = new Game(gameId);

        gameList.put(gameId, game);
        return gameId;
    }
}
