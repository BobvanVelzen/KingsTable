package game;

public class Piece implements IPiece {

    private PieceType type;
    private PieceTeam team;
    private int column;
    private int row;

    public Piece(PieceType type, PieceTeam team) {
        this.type = type;
        this.team = team;
    }

    public PieceType getType() { return type; }

    public PieceTeam getTeam() {
        return team;
    }

    @Override
    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public int getRow() {
        return row;
    }
}
