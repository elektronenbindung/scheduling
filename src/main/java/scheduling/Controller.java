package scheduling;

import java.io.File;

public class Controller implements Runnable {
    private File inputFile;
    private SpreadsheetReader inputReader;
    private UI ui;
    private boolean inUIMode;
    private TabuSearch tabuSearch;

    public Controller(File file, UI ui) {
        inputFile = file;
        this.ui = ui;
        tabuSearch = new TabuSearch(this);
        this.inUIMode = ui != null;
    }

    public void run() {
        if (!inputFile.exists()) {
            println("Error: The provided input file does not exist");
        } else {
            try {
                inputReader = new SpreadsheetReader(inputFile);
                inputReader.run();
                Solution solution = new Matching(inputReader).run();
                solution = tabuSearch.run(inputReader, solution);
                new SpreadsheetWriter(inputReader, solution, this).run();
            } catch (Exception exception) {
                println("Error: " + exception.getMessage());
            }
        }
        if (inUIMode) {
            ui.finished();
        }
    }

    public void println(String message) {
        if (inUIMode) {
            ui.println(message);
        } else {
            System.out.println(message);
        }
    }

    public void stop() {
        tabuSearch.stop();
    }
}
