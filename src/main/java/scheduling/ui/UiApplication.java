package scheduling.ui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class UiApplication extends Application {

    public UiApplication() {
    }

    public void show(String version) {
        if (version == null) {
            version = "dev";
        }
        launch(version);
    }

    @Override
    public void start(Stage stage) throws Exception {
        String version = getParameters().getRaw().get(0);
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("UI.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setTitle("Scheduling " + version);
        stage.show();
    }

}
