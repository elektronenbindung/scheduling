package scheduling.ui;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class UiApplication extends Application {

    public void show(String version) {
        launch(version);
    }

    @Override
    public void start(Stage stage) throws Exception {
        List<String> paramList = getParameters().getRaw();
        String version = paramList.size() > 0 ? paramList.get(0) : "dev";
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("UI.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setTitle("Scheduling " + version);
        stage.show();
    }

}
