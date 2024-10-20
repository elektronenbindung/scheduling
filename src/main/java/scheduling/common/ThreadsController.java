package scheduling.common;

import java.io.File;

import scheduling.UI;
import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.spreadsheet.SpreadsheetWriter;

public class ThreadsController implements Runnable {
    private File inputFile;
    private SpreadsheetReader inputReader;
    private UI ui;
    private boolean inUIMode;
    private SolutionController[] solutionControllers;
    private int numberOfFinishedSolutions;
    private Solution bestSolution;
    private boolean allThreadsStarted;

    public ThreadsController(File file, UI ui) {
        inputFile = file;
        this.ui = ui;
        this.solutionControllers = new SolutionController[Config.NUMBER_OF_PARALLEL_THREADS];
        this.inUIMode = ui != null;
        numberOfFinishedSolutions = 0;
        bestSolution = null;
        allThreadsStarted = false;
    }

    public void run() {
        if (!inputFile.exists()) {
            println("Error: The provided input file does not exist");
        } else {
            try {
                inputReader = new SpreadsheetReader(inputFile);
                inputReader.run();
                println("Input file has been read successfully, computing solutions...");
                for (int currentSolutionThread = 0; currentSolutionThread < Config.NUMBER_OF_PARALLEL_THREADS; currentSolutionThread++) {
                    solutionControllers[currentSolutionThread] = new SolutionController(this);
                    new Thread(solutionControllers[currentSolutionThread]).start();
                }
                allThreadsStarted = true;
                while (numberOfFinishedSolutions < Config.NUMBER_OF_PARALLEL_THREADS) {
                    Thread.sleep(100);
                }
                new SpreadsheetWriter(inputReader, bestSolution, this).run();
            } catch (Exception exception) {
                println("Error: " + exception.getMessage());
            }
        }
        if (inUIMode) {
            ui.finished();
        } else {
            System.exit(0);
        }
    }

    public synchronized void println(String message) {
        if (inUIMode) {
            ui.println(message);
        } else {
            System.out.println(message);
        }
    }

    public SpreadsheetReader getInputReader() {
        return inputReader;
    }

    public synchronized void stop() {
        while (!allThreadsStarted) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                println("Something went wrong during interruption of threads");
            }
        }
        for (int currentSolutionThread = 0; currentSolutionThread < Config.NUMBER_OF_PARALLEL_THREADS; currentSolutionThread++)
            solutionControllers[currentSolutionThread].stop();
    }

    public synchronized void setSolution(Solution solution) {
        if (this.bestSolution == null || solution.getCosts() < bestSolution.getCosts()) {
            bestSolution = solution;
            println("Costs of solution: " + bestSolution.getCosts());
        }
        numberOfFinishedSolutions++;
    }
}
