package scheduling.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import scheduling.common.Config;

class UiControllerTest extends ApplicationTest {

	private UiController controller;

	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Config.FXML_FILE_NAME));
		Parent root = loader.load();
		controller = loader.getController();
		stage.setScene(new Scene(root));
		stage.show();
	}

	@Test
	void initialButtonStateIsReadyToStart() {
		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
		assertFalse(selectFileButton().isDisabled());
	}

	@Test
	void printlnAppendsMessageToOutputConsole() {
		controller.println("hello world");

		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(outputConsole().getText().contains("hello world"));
	}

	@Test
	void printlnAppendsMultipleLines() {
		controller.println("first");
		controller.println("second");

		WaitForAsyncUtils.waitForFxEvents();

		String text = outputConsole().getText();
		assertTrue(text.contains("first"));
		assertTrue(text.contains("second"));
	}

	@Test
	void finishedResetsUiToReadyState() {
		controller.println("running");
		WaitForAsyncUtils.waitForFxEvents();

		controller.finished();
		WaitForAsyncUtils.waitForFxEvents();

		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
		assertFalse(selectFileButton().isDisabled());
		assertTrue(outputConsole().getText().contains("running"));
	}

	@Test
	void handleOnKeyReleasedWithNonEnterKeyDoesNothing() {
		inputField().setText("some.ods");
		WaitForAsyncUtils.waitForFxEvents();

		clickOn("#inputField").type(KeyCode.A);
		WaitForAsyncUtils.waitForFxEvents();

		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
	}

	@Test
	void startClickedWithEmptyInputRunsAndFinishesOnError() {
		assertEquals("", inputField().getText());

		clickOn("#startButton");

		long deadline = System.currentTimeMillis() + 5000;
		while (!outputConsole().getText().contains("does not exist") && System.currentTimeMillis() < deadline) {
			WaitForAsyncUtils.waitForFxEvents();
			sleep(50);
		}
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(outputConsole().getText().contains("does not exist"));
		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
		assertFalse(selectFileButton().isDisabled());
	}

	@Test
	void stopClickedDisablesStopButton() {
		clickOn("#startButton");
		WaitForAsyncUtils.waitForFxEvents();

		clickOn("#stopButton");
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(stopButton().isDisabled());
	}

	private Button startButton() {
		return lookup("#startButton").queryButton();
	}

	private Button stopButton() {
		return lookup("#stopButton").queryButton();
	}

	private Button selectFileButton() {
		return lookup("#selectFileButton").queryButton();
	}

	private TextField inputField() {
		return lookup("#inputField").query();
	}

	private TextArea outputConsole() {
		return lookup("#outputConsole").query();
	}
}
