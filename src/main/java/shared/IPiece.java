package shared;

public interface IPiece {

    PieceType getType();

    PieceTeam getTeam();

    int getId();

    void setColumn(int column);

    int getColumn();

    void setRow(int row);

    int getRow();

    void setPosition(int column, int row);
}
