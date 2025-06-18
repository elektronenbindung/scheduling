package scheduling.common;

import java.io.File;

import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.spreadsheet.SpreadsheetWriter;
import scheduling.ui.UiController;

public class ThreadsController implements Runnable {
	private final File inputFile;
	private SpreadsheetReader spreadsheetReader;
	private final UiController uiController;
	private final boolean inUIMode;
	private int numberOfFinishedSolutions;
	private boolean outputHasBeenWritten;
	private Solution bestSolution;
	private boolean stopped;
	private boolean informedAboutSolvableSchedule;

	public ThreadsController(File file, UiController uiController) {
		inputFile = file;
		this.uiController = uiController;
		this.inUIMode = uiController != null;
		numberOfFinishedSolutions = 0;
		outputHasBeenWritten = false;
		bestSolution = null;
		stopped = false;
		informedAboutSolvableSchedule = false;
	}

	public void run() {
		if (!inputFile.exists()) {
			println("Error: The provided input file does not exist");
			finished();
		} else {
			try {
				spreadsheetReader = new SpreadsheetReader(inputFile);
				spreadsheetReader.run();
				println("Input file has been read successfully, computing solutions...");
				for (int currentSolutionThread = 0; currentSolutionThread < Config.NUMBER_OF_PARALLEL_THREADS; currentSolutionThread++) {
					SolutionController currentSolutionController = new SolutionController(this);
					new Thread(currentSolutionController).start();
				}
			} catch (Exception exception) {
				println("Error: " + exception.getMessage());
				finished();
			}
		}
	}

	public synchronized void informAboutSolvabilityOfSchedule(boolean solvable) {
		if (!informedAboutSolvableSchedule) {
			informedAboutSolvableSchedule = true;

			if (solvable) {
				println("Success: This schedule is solvable");
			} else {
				println("Warning: This schedule is not solvable");
			}
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

	public synchronized void setSolution(Solution solution) {
		if (this.bestSolution == null || solution.getCosts() < bestSolution.getCosts()) {
			bestSolution = solution;
			println("Costs of solution: " + bestSolution.getCosts());

			if (solution.getCosts() == Config.OPTIMAL_SOLUTION) {
				stop();
				numberOfFinishedSolutions = Config.NUMBER_OF_PARALLEL_THREADS;
			}
		}
		numberOfFinishedSolutions++;

		if ((!outputHasBeenWritten) && numberOfFinishedSolutions >= Config.NUMBER_OF_PARALLEL_THREADS) {
			writeOutput();
			finished();
		}
	}

	private void writeOutput() {
		try {
			new SpreadsheetWriter(bestSolution, this).run();
		} catch (Exception exception) {
			println("Error: " + exception.getMessage());
		}
	}

	private synchronized void finished() {
		if (inUIMode) {
			outputHasBeenWritten = true;
			stop();
			uiController.finished();
		} else {
			System.exit(0);
		}
	}
}
