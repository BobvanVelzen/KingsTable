package game;

import javax.websocket.Session;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public interface IGame {

    boolean addPlayer(Session session);

    boolean addSpectator(Session session);

    void removeClient(Session session);

    boolean hasClient(Session session);

    void requestSync(Session session);

    List<IPiece> getPieces();

    IPiece getPiece(int column, int row);

    List<Point> getAvailableSpaces(IPiece piece);

    void selectTile(Point point, Session session);
}
