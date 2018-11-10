package server;

import shared.IPiece;
import shared.Piece;
import shared.PieceTeam;
import shared.PieceType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shared.JsonConverter;

import javax.websocket.Session;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Game implements IGame {
    private static final Logger LOGGER = Logger.getLogger(Game.class.getName());

    // TODO: add players to hashmap and spectators to the current sessionList
    private transient HashMap<PieceTeam, Session> players = new HashMap<>(); // assign players to this instead of to sessions
    private transient List<Session> spectators = new ArrayList<>(); // use this for spectators only later

    private int BOARD_SIZE = 11;

    private IPiece[][] grid;
    private IPiece selected = null;
    private int nextPieceId = 0;
    private int getNPId() {
        int npid = nextPieceId;
        nextPieceId++;
        return  npid;
    }

    private PieceTeam turn;
    private int turnCount = 0;

    Game() {
        startGame();
    }

    private void startGame() {
        this.grid = new IPiece[BOARD_SIZE][BOARD_SIZE];
        this.turn = PieceTeam.SOLDIERS;
        readyBoard();
        requestSync();
    }

    @Override
    public boolean addPlayer(Session session) {
        boolean added = false;
        if (this.players.get(PieceTeam.SOLDIERS) == null) {
            this.players.put(PieceTeam.SOLDIERS, session);
            added = true;
        } else if (this.players.get(PieceTeam.VIKINGS) == null) {
            this.players.put(PieceTeam.VIKINGS, session);
            added = true;
        }
        if (added)
            requestSync(session);

        return added;
    }

    @Override
    public boolean addSpectator(Session session) {
        boolean canSpectate = true;

        for (Session s : spectators) {
            if (s.getId().equals(session.getId()))
                canSpectate = false;
        }

        if (canSpectate)
            spectators.add(session);

        return canSpectate;
    }

    @Override
    public boolean hasClient(Session session) {
        return players.containsValue(session)
                || spectators.contains(session);
    }

    @Override
    public void removeClient(Session session) {
        Session sToRemove = null;
        for (Session s : players.values()) {
            if (s.getId().equals(session.getId()))
                sToRemove = s;
        }
        this.players.values().remove(sToRemove);

        for (Session s : spectators) {
            if (s.getId().equals(session.getId()))
                sToRemove = s;
        }
        this.spectators.remove(sToRemove);
    }

    @Override
    public List<IPiece> getPieces() {
        List<IPiece> pieces = new ArrayList<>();

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (grid[x][y] != null){
                    pieces.add(grid[x][y]);
                }
            }
        }

        return pieces;
    }

    @Override
    public IPiece getPiece(int column, int row) {
        return this.grid[column][row];
    }

    @Override
    public List<Point> getAvailableSpaces(IPiece piece) {
        // TODO: Methode korter/netter/efficienter
        List<Point> availableSpaces = new ArrayList<>();

        if (piece != null) {
            int x = piece.getColumn();
            int y = piece.getRow();

            if (piece.getType() == PieceType.KING) {

                List<Point> surrounding = getSurroundings(x, y);
                List<Point> availableSurrounding = new ArrayList<>();

                for (Point p : surrounding) {
                    if (inBounds(p.x, p.y)){
                        if (getPiece(p.x, p.y) == null)
                            availableSurrounding.add(p);
                    }
                }

                availableSpaces.addAll(checkRange(availableSurrounding));

            } else {

                final int leftEnd = piece.getColumn() - 1;

                List<Point> left = IntStream.rangeClosed(0, leftEnd)
                        .mapToObj(c -> new Point(leftEnd - c, y))
                        .collect(Collectors.toList());

                List<Point> right = IntStream.rangeClosed(piece.getColumn() + 1, BOARD_SIZE - 1)
                        .mapToObj(c -> new Point(c, y))
                        .collect(Collectors.toList());

                final int rightEnd = piece.getRow() - 1;

                List<Point> up = IntStream.rangeClosed(0, rightEnd)
                        .mapToObj(r -> new Point(x, rightEnd - r))
                        .collect(Collectors.toList());

                List<Point> down = IntStream.rangeClosed(piece.getRow() + 1, BOARD_SIZE - 1)
                        .mapToObj(r -> new Point(x, r))
                        .collect(Collectors.toList());

                availableSpaces.addAll(checkRange(left));
                availableSpaces.addAll(checkRange(right));
                availableSpaces.addAll(checkRange(up));
                availableSpaces.addAll(checkRange(down));
            }
        }

        return availableSpaces;
    }

    private List<Point> checkRange(List<Point> points) {
        List<Point> list = new ArrayList<>();

        for (Point p : points) {
            if (inBounds(p.x, p.y)){
                if (getPiece(p.x, p.y) != null) break;

                list.add(p);
            }
        }

        return list;
    }

    private void endTurn() {
        if (turn == PieceTeam.SOLDIERS)
            turn = PieceTeam.VIKINGS;
        else turn = PieceTeam.SOLDIERS;
        this.turnCount++;
    }

    @Override
    public void selectTile(Point point, Session session) {
        // TODO: pass IPiece instead of point
        Session turnPlayer = players.get(turn);
        if (turnPlayer == null) {
            sendInfo("No player assigned for " + turn + "'s turn!", session);
            return;
        }
        if (!turnPlayer.getId().equals(session.getId())){
            sendInfo("No player assigned for " + turn + "'s turn!", session);
            return;
        }

        // TODO: if passed position match return range. If not, check if can move to new passed position
        if (grid[point.x][point.y] != null) {
            // Checks if the selected piece's team is the same as the current turn's team
            if (grid[point.x][point.y].getTeam() != turn) {
                sendInfo("No player assigned for " + turn + "'s turn!", session);
                return;
            }

            selected = grid[point.x][point.y];
            sendRange(selected, getAvailableSpaces(selected));
            return;
        }

        if (selected != null && selected.getTeam() == turn){
            if (getAvailableSpaces(selected).contains(point)) {
                moveSelected(point);
            }
            selected = null;
        }
    }

    private void moveSelected(Point newLocation) {
        int x = newLocation.x;
        int y = newLocation.y;
        
        grid[x][y] = selected;
        grid[selected.getColumn()][selected.getRow()] = null;
        selected.setColumn(x);
        selected.setRow(y);

        if (selected.getType() != PieceType.KING) {
            checkTakes(selected);
        } else if (isKingInCorner(selected)) {
            // TODO: End game with Soldiers as winner
            sendInfo("Soldiers Wins!");
            startGame();
        }

        endTurn();
        sendMovedPiece(selected);
    }

    private void checkTakes(IPiece movedPiece) {
        int x = movedPiece.getColumn();
        int y = movedPiece.getRow();

        // TODO: Review this code

        IPiece opponentPiece;
        for (Point p : getSurroundings(x, y)) {
            if (inBounds(p.x, p.y)) {
                opponentPiece = grid[p.x][p.y];
                if (opponentPiece != null && canTake(movedPiece, opponentPiece)){
                    grid[p.x][p.y] = null;
                    sendRemovedPieceId(opponentPiece);
                }
            }
        }
    }

    private boolean canTake(IPiece movedPiece, IPiece opponentPiece) {
        if (opponentPiece.getTeam() != movedPiece.getTeam()){
            if (opponentPiece.getType() == PieceType.KING && movedPiece.getTeam() == PieceTeam.VIKINGS) {
                boolean blackWin = isKingSurrounded(opponentPiece);
                if (blackWin){
                    // TODO: End game with Vikings as winner
                    sendInfo("Vikings Wins!");
                    startGame();
                }
                return false;
            }

            int x = opponentPiece.getColumn() + (opponentPiece.getColumn() - movedPiece.getColumn());
            int y = opponentPiece.getRow() + (opponentPiece.getRow() - movedPiece.getRow());

            if (inBounds(x, y)) {
                IPiece oppositePiece = grid[x][y];

                return oppositePiece != null && movedPiece.getTeam() == oppositePiece.getTeam();
            }
        }
        return false;
    }

    private boolean isKingSurrounded(IPiece king){
        int kingX = king.getColumn();
        int kingY = king.getRow();

        // TODO: Review this code

        int soldiers = 0;
        List<Point> surrounding = getSurroundings(kingX, kingY);

        for (Point p : surrounding) {
            if (inBounds(p.x, p.y)) {
                IPiece sidingPiece = grid[p.x][p.y];
                if (sidingPiece != null && sidingPiece.getTeam() == PieceTeam.VIKINGS)
                    soldiers++;
            } else soldiers++;
        }

        return soldiers >= 4;
    }

    private boolean isKingInCorner(IPiece king) {
        int kingX = king.getColumn();
        int kingY = king.getRow();

        if (kingX == 0 && kingY == 0)
            return true;
        if (kingX == 0 && kingY == BOARD_SIZE - 1)
            return true;
        if (kingX == BOARD_SIZE - 1 && kingY == 0)
            return true;
        if (kingX == BOARD_SIZE - 1 && kingY == BOARD_SIZE - 1)
            return true;

        return false;
    }

    private List<Point> getSurroundings(int x, int y) {
        List<Point> surrounding = new ArrayList<>();

        surrounding.add(new Point(x + 1, y));
        surrounding.add(new Point(x - 1, y));
        surrounding.add(new Point(x, y + 1));
        surrounding.add(new Point(x, y - 1));

        return surrounding;
    }

    private boolean inBounds(int x, int y) {
        return (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE);
    }

    public void requestSync(Session session) {
        JSONObject json = new JSONObject();
        JSONArray pieceList = JsonConverter.convertPieceListToJson(getPieces());

        if (pieceList != null) {
            try {
                json.put("function", "SYNC_BOARD");
                json.put("pieces", pieceList);

                session.getBasicRemote().sendText(json.toString());

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestSync() {
        for (Session player : players.values()) {
            requestSync(player);
        }
        for (Session spectator : spectators) {
            requestSync(spectator);
        }
    }

    private void sendInfo(String info, Session session) {
        JSONObject json = new JSONObject();

        try {
            json.put("function", "INFO");
            json.put("info", info);

            session.getBasicRemote().sendText(json.toString());

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void sendInfo(String info) {
        for (Session player : players.values()) {
            sendInfo(info, player);
        }
        for (Session spectator : spectators) {
            sendInfo(info, spectator);
        }
    }

    private void sendMovedPiece(IPiece piece) {
        JSONObject json = new JSONObject();
        JSONObject jsonPiece = JsonConverter.convertPieceToJson(piece);

        if (jsonPiece != null) {
            try {
                json.put("function", "MOVE_PIECE");
                json.put("piece", jsonPiece);

                for (Session session : players.values()) {
                    session.getBasicRemote().sendText(json.toString());
                }
                for (Session session : spectators) {
                    session.getBasicRemote().sendText(json.toString());
                }

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendRemovedPieceId(IPiece piece) {
        JSONObject json = new JSONObject();

        try {
            json.put("function", "REMOVE_PIECE");
            json.put("pieceId", piece.getId());

            for (Session session : players.values()) {
                session.getBasicRemote().sendText(json.toString());
            }
            for (Session session : spectators) {
                session.getBasicRemote().sendText(json.toString());
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRange(IPiece piece, List<Point> points) {
        JSONObject json = new JSONObject();
        JSONObject jsonPiece = JsonConverter.convertPieceToJson(piece);
        JSONArray jsonPoints = JsonConverter.convertPointListToJson(points);

        if (jsonPiece != null) {
            try {
                json.put("function", "HIGHLIGHT_PIECE_RANGE");
                json.put("piece", jsonPiece);
                json.put("points", jsonPoints);

                Session playerSend = players.get(piece.getTeam());
                playerSend.getBasicRemote().sendText(json.toString());

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readyBoard() {
        // moet VEEL simpeler/korter

        grid[3][0] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[4][0] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][0] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[6][0] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[7][0] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][1] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);

        grid[0][3] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][4] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][6] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][7] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[1][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);

        grid[3][10] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[4][10] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][10] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[6][10] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[7][10] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][9] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);

        grid[10][3] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][4] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][6] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][7] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        grid[9][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);

        grid[5][3] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[4][4] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[5][4] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[6][4] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[3][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[4][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[6][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[7][5] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[4][6] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[5][6] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[6][6] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[5][7] = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);

        grid[5][5] = new Piece(getNPId(), PieceType.KING, PieceTeam.SOLDIERS);

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                IPiece p = grid[x][y];
                if (p != null){
                    p.setColumn(x);
                    p.setRow(y);
                }
            }
        }
    }
}
