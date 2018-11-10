package client;

import game.IPiece;
import org.json.JSONException;
import org.json.JSONObject;
import shared.JsonConverter;

import java.net.URI;
import java.net.URISyntaxException;

class GameClient {

    private final String hostAddress = "localhost";
    private GameClientEndPoint gcep;

    GameClient(ApplicationClient application) {
        try {
            gcep = new GameClientEndPoint(new URI(String.format("ws://%s:8025/kingstable/game", hostAddress)));
            gcep.addMessageHandler(new ClientMessageHandler(application));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    void sendPiece(IPiece piece) {
        JSONObject json = new JSONObject();
        JSONObject jsonPiece = JsonConverter.convertPieceToJson(piece);

        try {
            json.put("function", "HANDLE_PIECE");
            json.put("piece", jsonPiece);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        gcep.sendMessage(json);
    }

    void requestSync() {
        JSONObject json = new JSONObject();

        try {
            json.put("function", "SYNC_BOARD");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        gcep.sendMessage(json);
    }
}
