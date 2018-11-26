package client;

import shared.*;
import org.json.JSONException;
import org.json.JSONObject;

import javax.websocket.Session;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientMessageHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(ClientMessageHandler.class.getName());

    private ApplicationClient application;

    private HashMap<String, RecieveAction> actions = new HashMap<>();

    ClientMessageHandler(ApplicationClient application) {
        this.application = application;
        subscribe();
    }

    @Override
    public void handleMessage(String message, Session session) {
        JSONObject json = JsonConverter.stringToJson(message);

        try {
            RecieveAction action = actions.get(json.getString("function"));
            // Invoke action if player is subscribed
            if (action != null)
                action.invoke(json);

        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void info(JSONObject message) {
        try {
            LOGGER.log(Level.INFO, "INFO: " + message.get("info"));
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void syncBoard(JSONObject message) {
        List<IPiece> pieces = JsonConverter.getPieceListFromJson(message);
        if (pieces != null)
            application.syncBoard(pieces);
    }

    private void movePiece(JSONObject message){
        IPiece piece = JsonConverter.getPieceFromJson(message);

        if (piece != null)
            application.updatePiece(piece);
    }

    private void removePiece(JSONObject message){
        try {
            int pieceId = message.getInt("pieceId");

            application.removePiece(pieceId);

        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void highlightPieceRange(JSONObject message)
    {
        List<Point> points = JsonConverter.getPointListFromJson(message);
        IPiece piece = JsonConverter.getPieceFromJson(message);

        if (points != null && piece != null)
            application.highlightPieceRange(piece, points);
    }

    public void subscribe() {
        actions.put("SYNC_BOARD", json -> syncBoard(json));
        actions.put("MOVE_PIECE", json -> movePiece(json));
        actions.put("REMOVE_PIECE", json -> removePiece(json));
        actions.put("HIGHLIGHT_PIECE_RANGE", json -> highlightPieceRange(json));
        actions.put("INFO", json -> info(json));

        // TODO: RECIEVE_WINNER
    }

    public void unsubscribe() {
        actions.clear();
    }
}
