package client;

import game.*;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ApplicationClient extends Application {
    private static final Logger LOGGER = Logger.getLogger(GameClient.class.getName());

    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = 11; // TODO: Must be recieved from Game

    private IPiece[][] grid; // moet later verwijderd worden
    private GameClient websocketClient; // moet later vervangen worden door server
    private IGame game;

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
                tile.setOnMouseClicked(e -> {
                    Platform.runLater(() -> {
                        rangeRoot.getChildren().clear();
                        game.selectTile(new Point(column, row));
                    });
                });

                list.add(tile);
            }
        }

        return list;
    }

    public void syncBoard() {
        clearBoard();

        grid = game.getGrid();

        for (int x = 0; x < game.getSize(); x++) {
            for (int y = 0; y < game.getSize(); y++) {
                IPiece piece = grid[x][y];

                if (piece != null) {
                    piece.setColumn(x);
                    piece.setRow(y);
                    drawPiece(piece);
                }
            }
        }
    }

    private void drawPiece(IPiece piece) {
        Platform.runLater(() -> {
            Shape c = new Circle(TILE_SIZE / 2);
            ((Circle)c).setCenterX(TILE_SIZE / 2);
            ((Circle)c).setCenterY(TILE_SIZE / 2);
            c.setTranslateX(piece.getColumn() * (TILE_SIZE + 5) + TILE_SIZE / 4);
            c.setTranslateY(piece.getRow() * (TILE_SIZE + 5) + TILE_SIZE / 4);
            if (piece.getType() == PieceType.KING) {
                Circle king = new Circle(TILE_SIZE / 5);
                king.setCenterX(TILE_SIZE / 2);
                king.setCenterY(TILE_SIZE / 2);
                king.setTranslateX(piece.getColumn() * (TILE_SIZE + 5) + TILE_SIZE / 4);
                king.setTranslateY(piece.getRow() * (TILE_SIZE + 5) + TILE_SIZE / 4);
                c = Circle.subtract(c, king);
            }
            c.setFill(piece.getTeam() == PieceTeam.VIKINGS ? Color.DARKBLUE : Color.DARKRED);
            boardRoot.getChildren().add(c);
        });
    }

    public void drawRange(List<Point> points) {
        for (Point p : points) {
            Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
            rect.setTranslateX(p.getX() * (TILE_SIZE + 5) + TILE_SIZE / 4);
            rect.setTranslateY(p.getY() * (TILE_SIZE + 5) + TILE_SIZE / 4);
            rect.setFill(Color.rgb(50, 200, 50, 0.5));

            rangeRoot.getChildren().add(rect);
        }
    }

    public void clearBoard() {
        Platform.runLater(() -> {
            rangeRoot.getChildren().clear();
            boardRoot.getChildren().clear();
        });
    }

    @Override
    public void start(Stage primaryStage) {
        this.websocketClient = new GameClient(this);
        this.game = new Game();
        syncBoard(); // moet later weg

        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
