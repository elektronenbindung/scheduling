package scheduling.common;

import java.util.Random;

import scheduling.matching.ShiftMatching;
import scheduling.tabuSearch.TabuSearch;

public class SolutionController implements Runnable {
	private final ThreadsController threadsController;
	private final Random random;

	public SolutionController(ThreadsController threadsController, Long randomSeed) {
		this.threadsController = threadsController;
		this.random = createRandomForThread(randomSeed);
	}

	@Override
	public void run() {
		Solution solution = executeShiftMatching();
		solution = executeTabuSearch(solution);
		threadsController.setSolution(solution);
	}

	private Random createRandomForThread(Long randomSeed) {
		if (randomSeed == null) {
			return new Random();
		}
		return new Random(randomSeed);
	}

	private Solution executeShiftMatching() {
		return new ShiftMatching(threadsController).run();
	}

	private Solution executeTabuSearch(Solution solution) {
		TabuSearch tabuSearch = new TabuSearch(threadsController, random);
		return tabuSearch.run(solution);
	}
}
