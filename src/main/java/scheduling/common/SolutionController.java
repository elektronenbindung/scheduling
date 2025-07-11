package scheduling.common;

import scheduling.matching.ShiftMatching;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
	private final ThreadsController threadsController;

	public SolutionController(ThreadsController threadsController) {
		this.threadsController = threadsController;
	}

	@Override
	public void run() {
		Solution solution = executeShiftMatching();
		solution = executeTabuSearch(solution);
		threadsController.setSolution(solution);
	}

	private Solution executeShiftMatching() {
		return new ShiftMatching(threadsController).run();
	}

	private Solution executeTabuSearch(Solution solution) {
		TabuSearch tabuSearch = new TabuSearch(threadsController);
		return tabuSearch.run(solution);
	}
}