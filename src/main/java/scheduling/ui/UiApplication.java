package scheduling.ui;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import scheduling.AppVersion;
import scheduling.common.Config;

public class UiApplication extends Application {

	@Override
	public void start(Stage stage) throws IOException {
		String version = AppVersion.getVersion();

		Scene scene = createScene();

		stage.setScene(scene);
		stage.centerOnScreen();
		stage.setTitle(Config.WINDOW_TITLE_PREFIX + version);

		stage.show();
	}

	private Scene createScene() throws IOException {
		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();

		double width = Math.min(
				bounds.getWidth() * Config.SCREEN_PERCENTAGE_FOR_INITIAL_WINDOW_WIDTH,
				Config.MAX_INITIAL_WINDOW_WIDTH
		);
		double height = Math.min(
				bounds.getHeight() * Config.SCREEN_PERCENTAGE_FOR_INITIAL_WINDOW_HEIGHT,
				Config.MAX_INITIAL_WINDOW_HEIGHT
		);

		URL fxmlUrl = getClass().getClassLoader().getResource(Config.FXML_FILE_NAME);
		if (fxmlUrl == null) {
			throw new IOException("FXML file '" + Config.FXML_FILE_NAME + "' not found in classpath.");
		}

		Parent root = FXMLLoader.load(fxmlUrl);
		return new Scene(root, width, height);
	}
}
