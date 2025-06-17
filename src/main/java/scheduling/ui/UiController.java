package scheduling.ui;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class UiController {

    @FXML
    void selectFileClicked(MouseEvent event) {
        System.out.println("select file clicked");
    }

    @FXML
    void startClicked(MouseEvent event) {
        System.out.println("start clicked");

    }

    @FXML
    void stopClicked(MouseEvent event) {
        System.out.println("stop clicked");

    }

}
