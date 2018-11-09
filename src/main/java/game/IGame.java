package game;

import javax.websocket.Session;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public interface IGame {

    boolean addClient(Session session);

    void removeClient(Session session);

    IPiece[][] getGrid();

    int getSize();

    IPiece getPiece(int column, int row);

    List<Point> getAvailableSpaces(IPiece piece);

    void selectTile(Point point);
}
