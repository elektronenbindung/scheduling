package scheduling.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
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

	@AfterEach
	void deleteGeneratedOutputFiles() {
		File inputDir = new File(java.nio.file.Paths.get("src", "test", "java", "scheduling").toString());
		File[] outputFiles = inputDir.listFiles((_, name) -> name.endsWith("_output.ods"));
		if (outputFiles != null) {
			for (File file : outputFiles) {
				file.delete();
			}
		}
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
	void handleOnKeyReleasedWithEnterStartsProcessingWhenNoControllerIsActive() {
		assertEquals("", inputField().getText());

		clickOn("#inputField").type(KeyCode.ENTER);

		long deadline = System.currentTimeMillis() + 5000;
		while (!outputConsole().getText().contains("does not exist") && System.currentTimeMillis() < deadline) {
			WaitForAsyncUtils.waitForFxEvents();
			sleep(50);
		}
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(outputConsole().getText().contains("does not exist"));
		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
	}

	@Test
	void handleOnKeyReleasedWithEnterIsIgnoredWhileControllerIsActive() throws Exception {
		controller.println("previous content");
		WaitForAsyncUtils.waitForFxEvents();

		setThreadsController(new scheduling.common.ThreadsController(new java.io.File("Test.ods"), controller));

		clickOn("#inputField").type(KeyCode.ENTER);
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(outputConsole().getText().contains("previous content"));
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
		inputField().setText(new java.io.File("src/test/java/scheduling/Test.ods").getAbsolutePath());
		WaitForAsyncUtils.waitForFxEvents();

		clickOn("#startButton");

		long deadline = System.currentTimeMillis() + 5000;
		while (!startButton().isDisabled() && System.currentTimeMillis() < deadline) {
			WaitForAsyncUtils.waitForFxEvents();
			sleep(50);
		}
		assertTrue(startButton().isDisabled(), "Controller should be running (start disabled)");
		assertFalse(stopButton().isDisabled(), "Stop button should be enabled while running");

		clickOn("#stopButton");
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(stopButton().isDisabled());
	}

	@Test
	void chooseFileWithNoSelectionClearsConsoleAndShowsHint() {
		controller.println("previous content");
		WaitForAsyncUtils.waitForFxEvents();
		assertTrue(outputConsole().getText().contains("previous content"));

		controller.setFileSelector(window -> null);

		clickOn("#selectFileButton");
		WaitForAsyncUtils.waitForFxEvents();

		assertFalse(outputConsole().getText().contains("previous content"));
		assertTrue(outputConsole().getText().contains("No file selected"));
		assertEquals("", inputField().getText());
		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
	}

	@Test
	void chooseFileWithSelectionSetsInputFieldAndStartsProcessing() {
		controller.setFileSelector(window -> new java.io.File("Test.ods"));

		clickOn("#selectFileButton");
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(inputField().getText().endsWith("Test.ods"));

		long deadline = System.currentTimeMillis() + 5000;
		while (startButton().isDisabled() && System.currentTimeMillis() < deadline) {
			WaitForAsyncUtils.waitForFxEvents();
			sleep(50);
		}
		WaitForAsyncUtils.waitForFxEvents();

		assertFalse(startButton().isDisabled());
		assertTrue(stopButton().isDisabled());
		assertTrue(outputConsole().getText().contains("does not exist"));
	}

	private void setThreadsController(scheduling.common.ThreadsController threadsController) throws Exception {
		java.lang.reflect.Field field = UiController.class.getDeclaredField("threadsController");
		field.setAccessible(true);
		field.set(controller, threadsController);
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
