package shared;

public interface IPiece {

    PieceType getType();

    PieceTeam getTeam();

    int getId();

    int getX();

    int getY();

    void setPosition(int x, int y);
}
