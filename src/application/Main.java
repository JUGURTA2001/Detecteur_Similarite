package application;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.Constants;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(Constants.FXML_MAIN));
            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(getClass().getResource(Constants.CSS_BASE).toExternalForm());
            primaryStage.setTitle("Détecteur de similarité");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}