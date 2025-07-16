package scheduling.common;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.spreadsheet.SpreadsheetWriter;
import scheduling.ui.UiController;

public class ThreadsController implements Runnable {
	private final File inputFile;
	private final SpreadsheetReader spreadsheetReader;
	private final UiController uiController;
	private final boolean inUIMode;
	private int numberOfFinishedSolutions;
	private boolean outputHasBeenWritten;
	private Solution bestSolution;
	private volatile boolean stopped;
	private final AtomicBoolean informedAboutSolvableSchedule;
	private final ReentrantLock setSolutionLock;

	public ThreadsController(File file, UiController uiController) {
		inputFile = file;
		spreadsheetReader = new SpreadsheetReader(inputFile);
		this.uiController = uiController;
		this.inUIMode = uiController != null;
		numberOfFinishedSolutions = 0;
		outputHasBeenWritten = false;
		bestSolution = null;
		stopped = false;
		informedAboutSolvableSchedule = new AtomicBoolean(false);
		setSolutionLock = new ReentrantLock();
	}

	@Override
	public void run() {
		if (!inputFile.exists() || !inputFile.isFile()) {
			println("Error: The provided input file does not exist or is not a file");
			finished();
		} else {
			try {
				spreadsheetReader.run();
				println("Input file has been read successfully, computing solutions...");
				startSolutionThreads();
			} catch (Exception exception) {
				println("Error: " + exception.getMessage());
				finished();
			}
		}
	}

	public void informAboutSolvabilityOfSchedule(boolean solvable) {
		if (informedAboutSolvableSchedule.compareAndSet(false, true)) {
			println(solvable ? "Success: This schedule is solvable" : "Warning: This schedule is not solvable");
		}
	}

	public void println(String message) {
		if (inUIMode) {
			uiController.println(message);
		} else {
			System.out.println(message);
		}
	}

	public void stop() {
		stopped = true;
	}

	public boolean isStopped() {
		return stopped;
	}

	public SpreadsheetReader getSpreadsheetReader() {
		return spreadsheetReader;
	}

	public void setSolution(Solution solution) {
		setSolutionLock.lock();
		try {
			if (isBetterSolution(solution)) {
				bestSolution = solution;
				println("Costs of solution: " + bestSolution.getCosts());

				if (isOptimalSolution(solution)) {
					stop();
					numberOfFinishedSolutions = Config.NUMBER_OF_PARALLEL_THREADS;
				}
			}
			numberOfFinishedSolutions++;

			if (shouldWriteOutput()) {
				outputHasBeenWritten = true;
				writeOutput();
				finished();
			}
		} finally {
			setSolutionLock.unlock();
		}
	}

	private void startSolutionThreads() {
		for (int i = 0; i < Config.NUMBER_OF_PARALLEL_THREADS; i++) {
			SolutionController solutionController = new SolutionController(this);
			new Thread(solutionController).start();
		}
	}

	private boolean isBetterSolution(Solution solution) {
		return bestSolution == null || solution.getCosts() < bestSolution.getCosts();
	}

	private boolean isOptimalSolution(Solution solution) {
		return solution.getCosts() == Config.OPTIMAL_SOLUTION;
	}

	private boolean shouldWriteOutput() {
		return !outputHasBeenWritten && numberOfFinishedSolutions >= Config.NUMBER_OF_PARALLEL_THREADS;
	}

	private void writeOutput() {
		try {
			new SpreadsheetWriter(bestSolution, this).run();
		} catch (Exception exception) {
			println("Error: " + exception.getMessage());
		}
	}

	private void finished() {
		if (inUIMode) {
			stop();
			uiController.finished();
		} else {
			System.exit(0);
		}
	}
}