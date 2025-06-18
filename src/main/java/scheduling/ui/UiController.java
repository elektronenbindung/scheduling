package scheduling.ui;

import java.io.File;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

    private FileChooser fileChooser;

    public UiController() {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ods Files", "*.ods"),
                new FileChooser.ExtensionFilter("ots Files", "*.ots"));
    }

    @FXML
    void selectFileClicked(MouseEvent event) {
        File selectedFile = fileChooser.showOpenDialog(inputField.getScene().getWindow());
        if (selectedFile != null) {
            String fileName = selectedFile.getAbsolutePath();
            inputField.setText(fileName);
            startClicked(event);
        }
    }

    @FXML
    void startClicked(MouseEvent event) {
        if (threadsController == null) {
            outputConsole.setText("");
            startButton.setDisable(true);
            stopButton.setDisable(false);
            selectFileButton.setDisable(true);
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
        stopButton.setDisable(true);
        startButton.setDisable(false);
        selectFileButton.setDisable(false);
        threadsController = null;
        Platform.runLater(() -> inputField.requestFocus());
    }

    public void println(String message) {
        Platform.runLater(() -> {
            String currentText = outputConsole.getText();
            outputConsole.setText(currentText + message + "\n");
        });

    }
}