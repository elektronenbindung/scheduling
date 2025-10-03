package scheduling.ui;

import java.io.File;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import scheduling.common.ThreadsController;

public class UiController {

    @FXML
    private TextField inputField;

    @FXML
    private Button selectFileButton;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private TextArea outputConsole;

    private ThreadsController threadsController;

    private final FileChooser fileChooser;

    private static final String ODS_EXTENSION_DESCRIPTION = "ODS Files";
    private static final String ODS_EXTENSION = "*.ods";
    private static final String OTS_EXTENSION_DESCRIPTION = "OTS Files";
    private static final String OTS_EXTENSION = "*.ots";

    public UiController() {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(ODS_EXTENSION_DESCRIPTION, ODS_EXTENSION),
                new FileChooser.ExtensionFilter(OTS_EXTENSION_DESCRIPTION, OTS_EXTENSION));
    }

    @FXML
    void selectFileClicked(MouseEvent event) {
        File selectedFile = fileChooser.showOpenDialog(inputField.getScene().getWindow());
        if (selectedFile != null) {
            String fileName = selectedFile.getAbsolutePath();
            inputField.setText(fileName);
            startClicked(null);
        } else {
            outputConsole.setText("");
            println("No file selected. Please choose a valid spreadsheet file.");
            inputField.requestFocus();
        }
    }

    @FXML
    void handleOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            startClicked(null);
        }
    }

    @FXML
    void startClicked(MouseEvent event) {
        if (threadsController == null) {
            outputConsole.setText("");
            setUiStateOnStart();
            File input = new File(inputField.getText());
            threadsController = new ThreadsController(input, this);
            new Thread(threadsController).start();
            inputField.requestFocus();
        }
    }

    @FXML
    void stopClicked(MouseEvent event) {
        if (threadsController != null) {
            stopButton.setDisable(true);
            threadsController.stop();
        }
    }

    public void finished() {
        Platform.runLater(this::setUiStateOnFinish);
    }

    public void println(String message) {
        Platform.runLater(() -> {
            outputConsole.appendText(message + "\n");
            outputConsole.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void setUiStateOnStart() {
        startButton.setDisable(true);
        stopButton.setDisable(false);
        selectFileButton.setDisable(true);
    }

    private void setUiStateOnFinish() {
        stopButton.setDisable(true);
        startButton.setDisable(false);
        selectFileButton.setDisable(false);
        threadsController = null;
        inputField.requestFocus();
    }
}
