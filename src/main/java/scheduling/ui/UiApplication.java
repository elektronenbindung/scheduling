package scheduling.ui;

import java.util.Objects;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import scheduling.AppVersion;

public class UiApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        String version = AppVersion.getVersion();
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("UI.fxml")));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setTitle("Scheduling " + version);
        stage.show();
    }

}
