package scheduling.common;

import scheduling.matching.ScheduleMatching;
import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
    private SpreadsheetReader inputReader;
    private ThreadsController threadsController;

    public SolutionController(ThreadsController threadsController) {
        this.threadsController = threadsController;
        inputReader = threadsController.getInputReader();
    }

    public void run() {
        Solution solution = new ScheduleMatching(inputReader, threadsController).run();
        TabuSearch tabuSearch = new TabuSearch(threadsController);
        solution = tabuSearch.run(inputReader, solution);
        threadsController.setSolution(solution);
    }

}
