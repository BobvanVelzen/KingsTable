package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import shared.IPiece;
import shared.PieceTeam;
import shared.PieceType;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApplicationClient extends Application {

    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = 11; // TODO: Must be recieved from Game

    private HashMap<Integer, Shape> pieceShapes;
    private HashMap<Integer, IPiece> pieces;
    private IPiece selectedPiece;

    private GameClient websocketClient;

    private Pane boardRoot = new Pane();
    private Pane rangeRoot = new Pane();

    private Parent createContent() {
        Pane root = new Pane();

        Shape background = new Rectangle((BOARD_SIZE + 1) * TILE_SIZE, (BOARD_SIZE + 1) * TILE_SIZE);
        background.setFill(Color.BURLYWOOD);
        root.getChildren().add(background);

        root.getChildren().add(makeGrid());
        root.getChildren().add(boardRoot);
        root.getChildren().add(rangeRoot);
        root.getChildren().addAll(makeTiles());

        return root;
    }

    private Shape makeGrid() {
        Shape grid = new Rectangle((BOARD_SIZE + 1) * TILE_SIZE, (BOARD_SIZE + 1) * TILE_SIZE);

        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                Rectangle tile = new Rectangle(TILE_SIZE, TILE_SIZE);
                tile.setTranslateX(x * (TILE_SIZE + 5) + TILE_SIZE / 4);
                tile.setTranslateY(y * (TILE_SIZE + 5) + TILE_SIZE / 4);

                grid = Shape.subtract(grid, tile);
            }
        }

        Shape corner = new Rectangle(TILE_SIZE / 2, TILE_SIZE / 2);
        corner.setTranslateX(TILE_SIZE / 2);
        corner.setTranslateY(TILE_SIZE / 2);
        grid = Shape.union(grid, corner);
        corner.setTranslateY((BOARD_SIZE - 1) * (TILE_SIZE + 5) + TILE_SIZE / 2);
        grid = Shape.union(grid, corner);
        corner.setTranslateX((BOARD_SIZE - 1) * (TILE_SIZE + 5) + TILE_SIZE / 2);
        grid = Shape.union(grid, corner);
        corner.setTranslateY(TILE_SIZE / 2);
        grid = Shape.union(grid, corner);

        grid.setFill(Color.BLACK);

        return grid;
    }

    private List<Rectangle> makeTiles() {
        List<Rectangle> list = new ArrayList<>();

        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                Rectangle tile = new Rectangle(TILE_SIZE, TILE_SIZE);
                tile.setTranslateX(x * (TILE_SIZE + 5) + TILE_SIZE / 4);
                tile.setTranslateY(y * (TILE_SIZE + 5) + TILE_SIZE / 4);
                tile.setFill(Color.TRANSPARENT);

                tile.setOnMouseEntered(e -> tile.setFill(Color.rgb(255, 200, 200, 0.4)));
                tile.setOnMouseExited(e -> tile.setFill(Color.TRANSPARENT));

                final int column = x;
                final int row = y;
                tile.setOnMouseClicked(e -> Platform.runLater(() -> {
                    rangeRoot.getChildren().clear();
                    selectTile(column, row);
                }));

                list.add(tile);
            }
        }

        return list;
    }

    private void selectTile(int x, int y) {

        for (IPiece piece : pieces.values()) {
            if (x == piece.getX()
                    && y == piece.getY()) {
                selectedPiece = piece;
                websocketClient.sendPiece(piece);
                return;
            }
        }

        if (selectedPiece != null) {
            IPiece piece = selectedPiece;
            piece.setPosition(x, y);

            selectedPiece = null;
            websocketClient.sendPiece(piece);
        }
    }

    void updatePiece(IPiece piece) {
        pieces.put(piece.getId(), piece);
        drawPiece(piece);
    }

    void removePiece(int pieceId) {
        Shape s = pieceShapes.get(pieceId);
        Platform.runLater(() -> boardRoot.getChildren().remove(s));

        pieces.remove(pieceId);
        pieceShapes.remove(pieceId);
    }

    void syncBoard(List<IPiece> pieceList) {
        pieceShapes = new HashMap<>();
        pieces = new HashMap<>();
        clearBoard();

        for (IPiece p : pieceList) {
            updatePiece(p);
        }
    }

    private void drawPiece(IPiece piece) {
        Platform.runLater(() -> {
            Shape c = new Circle(TILE_SIZE / 2);
            ((Circle)c).setCenterX(TILE_SIZE / 2);
            ((Circle)c).setCenterY(TILE_SIZE / 2);
            c.setTranslateX(piece.getX() * (TILE_SIZE + 5) + TILE_SIZE / 4);
            c.setTranslateY(piece.getY() * (TILE_SIZE + 5) + TILE_SIZE / 4);
            if (piece.getType() == PieceType.KING) {
                Circle king = new Circle(TILE_SIZE / 5);
                king.setCenterX(TILE_SIZE / 2);
                king.setCenterY(TILE_SIZE / 2);
                king.setTranslateX(piece.getX() * (TILE_SIZE + 5) + TILE_SIZE / 4);
                king.setTranslateY(piece.getY() * (TILE_SIZE + 5) + TILE_SIZE / 4);
                c = Circle.subtract(c, king);
            }
            c.setFill(piece.getTeam() == PieceTeam.VIKINGS ? Color.DARKBLUE : Color.DARKRED);
            boardRoot.getChildren().add(c);

            int id = piece.getId();
            // removes piece if in pieceShapes
            if (pieceShapes.containsKey(id)) {
                boardRoot.getChildren().remove(pieceShapes.get(id));
                pieceShapes.remove(id);
            }
            // Places piece in list
            pieceShapes.put(piece.getId(), c);
        });
    }

    void highlightPieceRange(IPiece piece, List<Point> points) {
        selectedPiece = piece;
        drawRange(points);
    }

    private void drawRange(List<Point> points) {
        Platform.runLater(() -> {
            rangeRoot.getChildren().clear();

            for (Point p : points) {
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                rect.setTranslateX(p.getX() * (TILE_SIZE + 5) + (double)TILE_SIZE / 4);
                rect.setTranslateY(p.getY() * (TILE_SIZE + 5) + (double)TILE_SIZE / 4);
                rect.setFill(Color.rgb(50, 200, 50, 0.5));

                rangeRoot.getChildren().add(rect);
            }
        });
    }

    private void clearBoard() {
        Platform.runLater(() -> {
            rangeRoot.getChildren().clear();
            boardRoot.getChildren().clear();
        });
    }

    @Override
    public void start(Stage primaryStage) {
        this.websocketClient = new GameClient(this);
        this.websocketClient.requestSync();
        this.pieceShapes = new HashMap<>();
        this.pieces = new HashMap<>();

        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    public static void main(String[] args) {
        if (args.length >= 2) {
            GameClient.hostAddress = args[0];
            GameClient.port = args[1];
        }
        launch(args);
    }
}
