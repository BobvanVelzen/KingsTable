package shared;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JsonConverter {

    public static JSONObject stringToJson(String string) {
        JSONObject json = null;

        try {
            json = new JSONObject(string);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static JSONObject convertPieceToJson(IPiece piece) {
        JSONObject json = null;

        try {
            json = new JSONObject();

            json.put("id", piece.getId());
            json.put("type", piece.getType());
            json.put("team", piece.getTeam());
            json.put("column", piece.getColumn());
            json.put("row", piece.getRow());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static IPiece getPieceFromJson(JSONObject json) {
        IPiece piece = null;

        try {
            JSONObject jsonPiece = json.getJSONObject("piece");

            PieceType type = PieceType.valueOf(jsonPiece.getString("type"));
            PieceTeam team = PieceTeam.valueOf(jsonPiece.getString("team"));
            piece = new Piece(jsonPiece.getInt("id"), type, team);
            piece.setColumn(jsonPiece.getInt("column"));
            piece.setRow(jsonPiece.getInt("row"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return piece;
    }

    public static List<IPiece> getPieceListFromJson(JSONObject json) {
        List<IPiece> pieces = null;

        try {
            pieces = new ArrayList<>();

            JSONArray array = json.getJSONArray("pieces");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                IPiece p = getPieceFromJson(obj);
                if (p != null)
                    pieces.add(p);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return pieces;
    }

    public static JSONArray convertPieceListToJson(List<IPiece> pieces) {
        JSONArray array = null;
        try {
            array = new JSONArray();

            for (IPiece p : pieces) {
                JSONObject piece = new JSONObject();
                piece.put("piece", convertPieceToJson(p));
                array.put(piece);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return array;
    }

    public static JSONObject convertPointToJson(Point point) {
        JSONObject json = null;

        try {
            json = new JSONObject();

            json.put("column", point.getX());
            json.put("row", point.getY());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static Point getPointFromJson(JSONObject json) {
        Point point = null;

        try {
            point = new Point(json.getInt("column"), json.getInt("row"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return point;
    }

    public static JSONArray convertPointListToJson(List<Point> points) {
        JSONArray array = new JSONArray();
        for (Point p : points) {
            array.put(convertPointToJson(p));
        }

        return array;
    }

    public static List<Point> getPointListFromJson(JSONObject json) {
        List<Point> points = null;

        try {
            points = new ArrayList<>();

            JSONArray array = json.getJSONArray("points");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                Point p = getPointFromJson(obj);
                if (p != null)
                    points.add(p);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return points;
    }
}
