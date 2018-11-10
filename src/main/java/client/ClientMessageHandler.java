package client;

import shared.IPiece;
import org.json.JSONException;
import org.json.JSONObject;
import shared.IMessageHandler;
import shared.JsonConverter;

import javax.websocket.Session;
import java.awt.*;
import java.util.List;

public class ClientMessageHandler implements IMessageHandler {

    private ApplicationClient application;

    ClientMessageHandler(ApplicationClient application) {
        this.application = application;
    }

    @Override
    public void handleMessage(String message, Session session) {
        JSONObject json = JsonConverter.stringToJson(message);

        try {
            switch (json.getString("function")) {
                default:
                    break;
                case "SYNC_BOARD":
                    syncBoard(json);
                    break;
                case "MOVE_PIECE":
                    movePiece(json);
                    break;
                case "REMOVE_PIECE":
                    removePiece(json);
                    break;
                case "HIGHLIGHT_PIECE_RANGE":
                    highlightPieceRange(json);
                    break;
                case "INFO":
                    info(json);
                    break;
                    // TODO: RECIEVE_WINNER
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void info(JSONObject message) {
        try {
            System.out.println("INFO: " + message.get("info"));
        } catch (JSONException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void highlightPieceRange(JSONObject message)
    {
        List<Point> points = JsonConverter.getPointListFromJson(message);
        IPiece piece = JsonConverter.getPieceFromJson(message);

        if (points != null && piece != null)
            application.highlightPieceRange(piece, points);
    }
}
