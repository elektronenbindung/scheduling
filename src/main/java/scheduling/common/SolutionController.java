package scheduling.common;

import scheduling.matching.ScheduleMatching;
import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
    private SpreadsheetReader inputReader;
    private ThreadsController threadsController;
    private TabuSearch tabuSearch;

    public SolutionController(ThreadsController threadsController) {
        tabuSearch = new TabuSearch();
        this.threadsController = threadsController;
        inputReader = threadsController.getInputReader();
    }

    public void run() {
        Solution solution = new ScheduleMatching(inputReader, this).run();
        solution = tabuSearch.run(inputReader, solution);
        threadsController.setSolution(solution);
    }

    public void println(String message) {
        threadsController.println(message);
    }

    public void stop() {
        tabuSearch.stop();
    }
}
