package diffiehellman;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class MainApp extends Application {
    @Override public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/diffiehellman/DiffieView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1520, 850);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Diffie-Hellman Key Exchange Studio");
        stage.setScene(scene);
        stage.setMinWidth(1120);
        stage.setMinHeight(700);
        stage.setMaximized(true);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
