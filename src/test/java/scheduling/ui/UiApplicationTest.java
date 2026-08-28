package scheduling.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import scheduling.AppVersion;
import scheduling.common.Config;

class UiApplicationTest extends ApplicationTest {

	private Stage stage;

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		new UiApplication().start(stage);
	}

	@Test
	void showsWindowWithVersionPrefixedTitle() {
		assertEquals(Config.WINDOW_TITLE_PREFIX + AppVersion.getVersion(), stage.getTitle());
	}

	@Test
	void sceneIsSetAndRootLoaded() {
		Scene scene = stage.getScene();
		assertNotNull(scene);
		Parent root = scene.getRoot();
		assertNotNull(root);
		assertTrue(root.getStyleClass().contains("main-container"));
	}

	@Test
	void windowIsVisible() {
		assertTrue(stage.isShowing());
		WaitForAsyncUtils.waitForFxEvents();
	}
}
