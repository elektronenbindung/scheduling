package scheduling.common;

import scheduling.matching.ScheduleMatching;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
    private ThreadsController threadsController;

    public SolutionController(ThreadsController threadsController) {
        this.threadsController = threadsController;
    }

    public void run() {
        Solution solution = new ScheduleMatching(threadsController).run();
        TabuSearch tabuSearch = new TabuSearch(threadsController);
        solution = tabuSearch.run(solution);
        threadsController.setSolution(solution);
    }

}
