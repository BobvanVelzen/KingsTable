package server;

import java.util.EnumMap;
import java.util.logging.Level;
import shared.IPiece;
import shared.Piece;
import shared.PieceTeam;
import shared.PieceType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shared.JsonConverter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Game implements IGame {
    private static final Logger LOGGER = Logger.getLogger(Game.class.getName());

    private String id;
    private EnumMap<PieceTeam, Player> players = new EnumMap<>(PieceTeam.class); // assign players to this instead of to sessions
    private List<Player> spectators = new ArrayList<>(); // use this for spectators only later

    private int boardSize = 11;

    private List<IPiece> pieces;
    private List<Point> flanks; // TODO: ADD FLANKS
    private IPiece selected = null;
    private int nextPieceId = 0;
    private int getNPId() {
        int npid = nextPieceId;
        nextPieceId++;
        return  npid;
    }

    private PieceTeam turn;
    private int turnCount = 0;

    @Override
    public String getId() {
        return id;
    }

    Game(String id) {
        this.id = id;
        startGame();
    }

    private void startGame() {
        this.pieces = new ArrayList<>();
        this.flanks = new ArrayList<>();
        this.turn = PieceTeam.SOLDIERS;
        readyBoard();
        requestSync();
    }

    @Override
    public boolean addPlayer(Player player) {
        boolean added = false;
        if (this.players.get(PieceTeam.SOLDIERS) == null) {
            this.players.put(PieceTeam.SOLDIERS, player);
            added = true;
        } else if (this.players.get(PieceTeam.VIKINGS) == null) {
            this.players.put(PieceTeam.VIKINGS, player);
            added = true;
        }
        if (added)
            requestSync(player);

        return added;
    }

    @Override
    public boolean addSpectator(Player player) {
        boolean canSpectate = true;

        for (Player p : spectators) {
            if (p.getId().equals(player.getId()))
                canSpectate = false;
        }

        if (canSpectate)
            spectators.add(player);

        return canSpectate;
    }

    @Override
    public boolean hasClient(Player player) {
        return players.containsValue(player)
                || spectators.contains(player);
    }

    @Override
    public boolean removeClient(Player player) {
        Player pToRemove = null;
        for (Player p : players.values()) {
            if (p.getId().equals(player.getId()))
                pToRemove = p;
        }
        this.players.values().remove(pToRemove);

        for (Player p : spectators) {
            if (p.getId().equals(player.getId()))
                pToRemove = p;
        }
        this.spectators.remove(pToRemove);

        if (players.size() == 0) {
            sendCloseGame("All players left");
            return true;
        }
        return false;
    }

    @Override
    public List<IPiece> getPieces() {
        return pieces;
    }

    @Override
    public IPiece getPiece(int column, int row) {
        for (IPiece p : pieces) {
            if (p.getX() == column && p.getY() == row)
                return p;
        }
        return null;
    }

    @Override
    public List<Point> getAvailableSpaces(IPiece piece) {
        // TODO: Methode korter/netter/efficienter
        List<Point> availableSpaces = new ArrayList<>();

        if (piece != null) {
            int x = piece.getX();
            int y = piece.getY();

            if (piece.getType() == PieceType.KING) {

                List<Point> surrounding = getSurroundings(x, y);
                List<Point> availableSurrounding = new ArrayList<>();

                for (Point p : surrounding) {
                    if (inBounds(p.x, p.y) && getPiece(p.x, p.y) == null)
                        availableSurrounding.add(p);
                }

                availableSpaces.addAll(checkRange(availableSurrounding));

            } else {

                final int leftEnd = piece.getX() - 1;

                List<Point> left = IntStream.rangeClosed(0, leftEnd)
                        .mapToObj(c -> new Point(leftEnd - c, y))
                        .collect(Collectors.toList());

                List<Point> right = IntStream.rangeClosed(piece.getX() + 1, boardSize - 1)
                        .mapToObj(c -> new Point(c, y))
                        .collect(Collectors.toList());

                final int rightEnd = piece.getY() - 1;

                List<Point> up = IntStream.rangeClosed(0, rightEnd)
                        .mapToObj(r -> new Point(x, rightEnd - r))
                        .collect(Collectors.toList());

                List<Point> down = IntStream.rangeClosed(piece.getY() + 1, boardSize - 1)
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
    public void handlePiece(IPiece piece, Player player) {
        Player turnPlayer = players.get(turn);
        // Return if there are not enough players
        if (players.values().size() < 2) {
            sendInfo("Not enough players!", player);
            return;
        }
        // Return if it isn't the players turn right now
        if (!turnPlayer.equals(player)){
            sendInfo("It's " + turn + "'s turn!", player);
            return;
        }

        // If the piece is on the same tile
        IPiece oldPiece = getPiece(piece.getX(), piece.getY());
        if (piece.equals(oldPiece)) {
            // Checks if the selected piece's team is the same as the current turn's team
            if (piece.getTeam() != turn) {
                sendInfo("You cannot move " + turn + "'s pieces!", player);
                return;
            }

            // Set piece as selected
            selected = getPiece(piece.getX(), piece.getY());
            // Send moveRange to player
            sendRange(selected, getAvailableSpaces(selected));
            return;
        }

        // Move selected piece to selected tile if able
        if (selected != null && selected.getTeam() == turn){
            Point newPos = new Point(piece.getX(), piece.getY());
            if (getAvailableSpaces(selected).contains(newPos)) {
                moveSelected(newPos);
            }
            selected = null;
        }
    }

    private void moveSelected(Point newLocation) {
        int x = newLocation.x;
        int y = newLocation.y;

        selected.setPosition(x, y);

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
        int x = movedPiece.getX();
        int y = movedPiece.getY();

        // TODO: Review this code

        for (Point p : getSurroundings(x, y)) {
            if (inBounds(p.x, p.y)) {
                IPiece opponentPiece = getPiece(p.x, p.y);
                if (opponentPiece != null && canTake(movedPiece, opponentPiece)){
                    pieces.remove(opponentPiece);
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

            int x = opponentPiece.getX() + (opponentPiece.getX() - movedPiece.getX());
            int y = opponentPiece.getY() + (opponentPiece.getY() - movedPiece.getY());

            if (inBounds(x, y)) {
                IPiece oppositePiece = getPiece(x, y);

                return oppositePiece != null && movedPiece.getTeam() == oppositePiece.getTeam();
            }
        }
        return false;
    }

    private boolean isKingSurrounded(IPiece king){
        int kingX = king.getX();
        int kingY = king.getY();

        // TODO: Review this code

        int soldiers = 0;
        List<Point> surrounding = getSurroundings(kingX, kingY);

        for (Point p : surrounding) {
            if (inBounds(p.x, p.y)) {
                IPiece sidingPiece = getPiece(p.x, p.y);
                if (sidingPiece != null && sidingPiece.getTeam() == PieceTeam.VIKINGS)
                    soldiers++;
            } else soldiers++;
        }

        return soldiers >= 4;
    }

    private boolean isKingInCorner(IPiece king) {
        int kingX = king.getX();
        int kingY = king.getY();

        if (kingX == 0 && kingY == 0)
            return true;
        if (kingX == 0 && kingY == boardSize - 1)
            return true;
        if (kingX == boardSize - 1 && kingY == 0)
            return true;
        if (kingX == boardSize - 1 && kingY == boardSize - 1)
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
        return (x >= 0 && x < boardSize && y >= 0 && y < boardSize);
    }

    public void requestSync(Player player) {
        JSONObject json = new JSONObject();
        JSONArray pieceList = JsonConverter.convertPieceListToJson(getPieces());

        if (pieceList != null) {
            try {
                json.put("function", "SYNC_BOARD");
                json.put("pieces", pieceList);

                player.sendMessage(json);

            } catch (JSONException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }
        }
    }

    private void requestSync() {
        for (Player player : players.values()) {
            requestSync(player);
        }
        for (Player spectator : spectators) {
            requestSync(spectator);
        }
    }

    private void sendCloseGame(String reason) {
        JSONObject json = new JSONObject();

        System.out.println("Closing game");
        try {
            json.put("function", "CLOSE_GAME");
            json.put("reason", reason);

            for (Player player : players.values()) {
                player.sendMessage(json);
            }
            for (Player spectator : spectators) {
                spectator.sendMessage(json);
            }

        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void sendInfo(String info, Player player) {
        JSONObject json = new JSONObject();

        try {
            json.put("function", "INFO");
            json.put("info", info);

            player.sendMessage(json);

        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void sendInfo(String info) {
        for (Player player : players.values()) {
            sendInfo(info, player);
        }
        for (Player spectator : spectators) {
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

                for (Player player : players.values()) {
                    player.sendMessage(json);
                }
                for (Player spectator : spectators) {
                    spectator.sendMessage(json);
                }

            } catch (JSONException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
    }

    private void sendRemovedPieceId(IPiece piece) {
        JSONObject json = new JSONObject();

        try {
            json.put("function", "REMOVE_PIECE");
            json.put("pieceId", piece.getId());

            for (Player player : players.values()) {
                player.sendMessage(json);
            }
            for (Player spectator : spectators) {
                spectator.sendMessage(json);
            }

        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
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

                Player playerSend = players.get(piece.getTeam());
                playerSend.sendMessage(json);

            } catch (JSONException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
    }

    private void readyBoard() {
        // TODO: moet VEEL simpeler/korter
        IPiece piece;

        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(3, 0);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(4, 0);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(5, 0);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(6, 0);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(7, 0);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(5, 1);
        pieces.add(piece);

        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(0, 3);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(0, 4);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(0, 5);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(0, 6);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(0, 7);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(1, 5);
        pieces.add(piece);

        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(3, 10);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(4, 10);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(5, 10);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(6, 10);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(7, 10);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(5, 9);
        pieces.add(piece);

        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(10, 3);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(10, 4);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(10, 5);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(10, 6);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(10, 7);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.VIKINGS);
        piece.setPosition(9, 5);
        pieces.add(piece);

        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(5, 3);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(4, 4);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(5, 4);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(6, 4);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(3, 5);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(4, 5);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(6, 5);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(7, 5);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(4, 6);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(5, 6);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(6, 6);
        pieces.add(piece);
        piece = new Piece(getNPId(), PieceType.PAWN, PieceTeam.SOLDIERS);
        piece.setPosition(5, 7);
        pieces.add(piece);

        piece = new Piece(getNPId(), PieceType.KING, PieceTeam.SOLDIERS);
        piece.setPosition(5, 5);
        pieces.add(piece);

        flanks.add(new Point(0,0));
        flanks.add(new Point(boardSize - 1, 0));
        flanks.add(new Point(0,boardSize - 1));
        flanks.add(new Point(boardSize - 1,boardSize - 1));
        flanks.add(new Point(5,5));
    }
}
