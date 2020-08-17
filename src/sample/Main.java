package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    public static final double WINHEIGHT = 800.0;
    public static final double WINWIDTH = 800.0;
    public static final int PIECESIZE = 30;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GamePanel.fxml"));
        Parent root = loader.load();
        GameController gameController = loader.getController();
        primaryStage.getIcons().add(new Image (getClass().getResourceAsStream ( "AppIcon.png" )));
        primaryStage.setTitle("Tetris");
        primaryStage.setScene(new Scene(root, WINWIDTH, WINHEIGHT));
        gameController.sendStage(primaryStage);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
