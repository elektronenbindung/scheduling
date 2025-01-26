package scheduling.common;

import scheduling.matching.ScheduleMatching;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
    private ThreadsController threadsController;
    private boolean checkForPerfectMatching;

    public SolutionController(ThreadsController threadsController, boolean checkForPerfectMatching) {
        this.threadsController = threadsController;
        this.checkForPerfectMatching = checkForPerfectMatching;
    }

    public void run() {
        Solution solution = new ScheduleMatching(threadsController, checkForPerfectMatching).run();
        TabuSearch tabuSearch = new TabuSearch(threadsController);
        solution = tabuSearch.run(solution);
        threadsController.setSolution(solution);
    }

}
