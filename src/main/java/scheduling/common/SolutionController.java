package scheduling.common;

import scheduling.matching.ScheduleMatching;
import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
    private SpreadsheetReader spreadsheetReader;
    private ThreadsController threadsController;

    public SolutionController(ThreadsController threadsController) {
        this.threadsController = threadsController;
        spreadsheetReader = threadsController.getSpreadsheetReader();
    }

    public void run() {
        Solution solution = new ScheduleMatching(spreadsheetReader, threadsController).run();
        TabuSearch tabuSearch = new TabuSearch(threadsController, spreadsheetReader);
        solution = tabuSearch.run(solution);
        threadsController.setSolution(solution);
    }

}
