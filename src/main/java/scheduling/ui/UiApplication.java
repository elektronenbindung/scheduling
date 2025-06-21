package scheduling.ui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import scheduling.Main;
import javafx.fxml.FXMLLoader;

public class UiApplication extends Application {

    public void show() {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        String version = Main.getVersion();
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("UI.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setTitle("Scheduling " + version);
        stage.show();
    }

}
