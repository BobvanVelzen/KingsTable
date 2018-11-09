package game;

public interface IPiece {

    PieceType getType();

    PieceTeam getTeam();

    void setColumn(int column);

    int getColumn();

    void setRow(int row);

    int getRow();
}
