package shared;

public class Piece implements IPiece {

    private int id;
    private PieceType type;
    private PieceTeam team;
    private int column;
    private int row;

    public Piece(int id, PieceType type, PieceTeam team) {
        this.id = id;
        this.type = type;
        this.team = team;
    }

    public PieceType getType() { return type; }

    public PieceTeam getTeam() {
        return team;
    }

    @Override
    public int getId() { return id; }

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

    @Override
    public void setPosition(int column, int row) {
        this.column = column;
        this.row = row;
    }

    @Override
    public String toString() {
        return String.format("%d: %d, %d", id, column, row);
    }
}
