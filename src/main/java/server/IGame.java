package server;

import shared.IPiece;

import java.awt.*;
import java.util.List;

public interface IGame {

    String getId();

    boolean addPlayer(Player player);

    boolean addSpectator(Player player);

    boolean removeClient(Player player);

    boolean hasClient(Player player);

    void requestSync(Player player);

    List<IPiece> getPieces();

    IPiece getPiece(int column, int row);

    List<Point> getAvailableSpaces(IPiece piece);

    void handlePiece(IPiece piece, Player player);
}
