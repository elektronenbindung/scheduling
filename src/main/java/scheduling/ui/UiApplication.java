package scheduling.ui;

import java.io.IOException;
import java.util.Objects;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import scheduling.AppVersion;

public class UiApplication extends Application {

    private static final String APP_TITLE_PREFIX = "Scheduling ";
    private static final String FXML_FILE_NAME = "UI.fxml";

    @Override
    public void start(Stage stage) throws IOException {
        String version = AppVersion.getVersion();

        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getClassLoader().getResource(FXML_FILE_NAME),
                "FXML file " + FXML_FILE_NAME + " not found. Ensure it's in the classpath."));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setTitle(APP_TITLE_PREFIX + version);

        stage.show();
    }
}
