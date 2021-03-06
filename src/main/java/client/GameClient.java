package client;

import shared.IPiece;
import org.json.JSONException;
import org.json.JSONObject;
import shared.JsonConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

class GameClient {
    private static final Logger LOGGER = Logger.getLogger(GameClient.class.getName());

    private static String localhostAddress = "localhost";
    static String hostAddress = "kaasplankje.synology.me";
    static String port = "8025";
    private GameClientEndPoint gcep;

    GameClient(ApplicationClient application) {
        try {
            gcep = new GameClientEndPoint(new URI(String.format("ws://%s:%s/kingstable/game", localhostAddress, port)));
            gcep.addMessageHandler(new ClientMessageHandler(application));

        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    void sendPiece(IPiece piece) {
        JSONObject json = new JSONObject();
        JSONObject jsonPiece = JsonConverter.convertPieceToJson(piece);

        try {
            json.put("function", "HANDLE_PIECE");
            json.put("piece", jsonPiece);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

        gcep.sendMessage(json);
    }

    void requestSync() {
        JSONObject json = new JSONObject();

        try {
            json.put("function", "SYNC_BOARD");
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

        gcep.sendMessage(json);
    }
}
