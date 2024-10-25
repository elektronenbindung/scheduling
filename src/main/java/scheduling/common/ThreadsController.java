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
    private int numberOfFinishedSolutions;
    private Solution bestSolution;
    private boolean stopped;

    public ThreadsController(File file, UI ui) {
        inputFile = file;
        this.ui = ui;
        this.inUIMode = ui != null;
        numberOfFinishedSolutions = 0;
        bestSolution = null;
        stopped = false;
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
                    SolutionController currentSolutionController = new SolutionController(this);
                    new Thread(currentSolutionController).start();
                }

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

    public synchronized SpreadsheetReader getInputReader() {
        return inputReader;
    }

    public synchronized void stop() {
        stopped = true;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized void setSolution(Solution solution) {
        if (this.bestSolution == null || solution.getCosts() < bestSolution.getCosts()) {
            bestSolution = solution;
            println("Costs of solution: " + bestSolution.getCosts());
        }
        numberOfFinishedSolutions++;
    }
}
