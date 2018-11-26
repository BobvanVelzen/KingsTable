package shared;

public class Piece implements IPiece {

    private int id;
    private PieceType type;
    private PieceTeam team;
    private int x;
    private int y;

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
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("%d: %d, %d", id, x, y);
    }

    @Override
    public boolean equals(Object other) {
        IPiece piece = (IPiece)other;
        if (piece == null)
            return false;

        return id == piece.getId()
                && x == piece.getX()
                && y == piece.getY();
    }
}
