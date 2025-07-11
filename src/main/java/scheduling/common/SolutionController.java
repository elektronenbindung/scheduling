package scheduling.common;

import scheduling.matching.ShiftMatching;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
	private final ThreadsController threadsController;

	public SolutionController(ThreadsController threadsController) {
		this.threadsController = threadsController;
	}

	public void run() {
		Solution solution = new ShiftMatching(threadsController).run();
		TabuSearch tabuSearch = new TabuSearch(threadsController);
		solution = tabuSearch.run(solution);
		threadsController.setSolution(solution);
	}
}
