package game;

import client.GameClient;

import javax.websocket.Session;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Game implements IGame {
    private static final Logger LOGGER = Logger.getLogger(Game.class.getName());

    private transient List<Session> sessions = new ArrayList<>();

    private int BOARD_SIZE = 11;

    private IPiece[][] grid;
    private IPiece selected = null;

    private int turn = 0;

    public Game() {
        startGame();
    }


    private void startGame() {
        this.grid = new IPiece[BOARD_SIZE][BOARD_SIZE];
        readyBoard();
    }

    @Override
    public boolean addClient(Session session) {
        if (this.sessions.size() < 2) {
            this.sessions.add(session);

            return true;
        }
        return false;
    }

    @Override
    public void removeClient(Session session) {
        Session sToRemove = null;
        for (Session s : this.sessions) {
            if (s.getId() == session.getId())
                sToRemove = s;
        }
        this.sessions.remove(sToRemove);
    }

    @Override
    public IPiece[][] getGrid() {
        return this.grid;
    }

    @Override
    public int getSize() {
        return this.BOARD_SIZE;
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

                availableSpaces.addAll(checkRange(surrounding));

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

    @Override
    public void selectTile(Point point) {
        if (grid[point.x][point.y] != null) {
            if (grid[point.x][point.y].getTeam().ordinal() != turn%2) {
                for (Session session : sessions) {
                    try {
                        // TODO: Send syncBoard request
                        session.getBasicRemote().sendText("syncBoard");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            selected = grid[point.x][point.y];
            for (Session client : sessions) {
                // TODO: Send drawRange request
                //client.drawRange(getAvailableSpaces(selected));
            }
            return;
        }

        if (selected != null){
            if (getAvailableSpaces(selected).contains(point)) {
                moveSelected(point);
            }
            selected = null;
        }

        return;
    }

    private void moveSelected(Point newLocation) {
        int x = newLocation.x;
        int y = newLocation.y;
        
        grid[x][y] = selected;
        grid[selected.getColumn()][selected.getRow()] = null;
        selected.setColumn(x);
        selected.setRow(y);
        turn++;

        if (selected.getType() != PieceType.KING) {
            checkTakes(selected);
        } else if (isKingInCorner(selected)) {
            startGame();
            LOGGER.log(Level.INFO, "Soldiers Wins!");
        }

        for (Session session : sessions) {
            try {
                // TODO: Send syncBoard request
                session.getBasicRemote().sendText("syncBoard");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkTakes(IPiece movedPiece) {
        int x = movedPiece.getColumn();
        int y = movedPiece.getRow();

        // TODO: Review this code

        IPiece opponentPiece;
        for (Point p : getSurroundings(x, y)) {
            if (inBounds(p.x, p.y)) {
                opponentPiece = grid[p.x][p.y];
                if (opponentPiece != null && canTake(movedPiece, opponentPiece))
                    grid[p.x][p.y] = null;
            }
        }
    }

    private boolean canTake(IPiece movedPiece, IPiece opponentPiece) {
        if (opponentPiece.getTeam() != movedPiece.getTeam()){
            if (opponentPiece.getType() == PieceType.KING && movedPiece.getTeam() == PieceTeam.VIKINGS) {
                boolean blackWin = isKingSurrounded(opponentPiece);
                if (blackWin){
                    // TODO: End game with Black as winner
                    startGame();
                    LOGGER.log(Level.INFO, "Vikings Wins!");
                }
                return false;
            }

            int x = opponentPiece.getColumn() + (opponentPiece.getColumn() - movedPiece.getColumn());
            int y = opponentPiece.getRow() + (opponentPiece.getRow() - movedPiece.getRow());

            if (inBounds(x, y)) {
                IPiece oppositePiece = grid[x][y];

                if (oppositePiece != null && movedPiece.getTeam() == oppositePiece.getTeam())
                    return true;
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

    private void readyBoard() {
        // moet VEEL simpeler/korter
        grid[3][0] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[4][0] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][0] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[6][0] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[7][0] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][1] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);

        grid[0][3] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][4] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][5] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][6] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[0][7] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[1][5] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);

        grid[3][10] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[4][10] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][10] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[6][10] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[7][10] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[5][9] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);

        grid[10][3] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][4] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][5] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][6] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[10][7] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);
        grid[9][5] = new Piece(PieceType.PAWN, PieceTeam.VIKINGS);

        grid[5][3] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[4][4] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[5][4] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[6][4] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[3][5] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[4][5] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[6][5] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[7][5] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[4][6] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[5][6] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[6][6] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);
        grid[5][7] = new Piece(PieceType.PAWN, PieceTeam.SOLDIERS);

        grid[5][5] = new Piece(PieceType.KING, PieceTeam.SOLDIERS);
    }
}
