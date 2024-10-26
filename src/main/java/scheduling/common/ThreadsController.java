package scheduling.common;

import java.io.File;

import scheduling.UI;
import scheduling.spreadsheet.SpreadsheetReader;
import scheduling.spreadsheet.SpreadsheetWriter;

public class ThreadsController implements Runnable {
    private File inputFile;
    private SpreadsheetReader spreadsheetReader;
    private UI ui;
    private boolean inUIMode;
    private int numberOfFinishedSolutions;
    private Solution bestSolution;
    private boolean stopped;
    private boolean warningForPerfectSolutionOccurred;

    public ThreadsController(File file, UI ui) {
        inputFile = file;
        this.ui = ui;
        this.inUIMode = ui != null;
        numberOfFinishedSolutions = 0;
        bestSolution = null;
        stopped = false;
        warningForPerfectSolutionOccurred = false;
    }

    public void run() {
        if (!inputFile.exists()) {
            println("Error: The provided input file does not exist");
        } else {
            try {
                spreadsheetReader = new SpreadsheetReader(inputFile);
                spreadsheetReader.run();
                println("Input file has been read successfully, computing solutions...");
                for (int currentSolutionThread = 0; currentSolutionThread < Config.NUMBER_OF_PARALLEL_THREADS; currentSolutionThread++) {
                    SolutionController currentSolutionController = new SolutionController(this);
                    new Thread(currentSolutionController).start();
                }

                while (numberOfFinishedSolutions < Config.NUMBER_OF_PARALLEL_THREADS) {
                    Thread.sleep(100);
                }
                new SpreadsheetWriter(spreadsheetReader, bestSolution, this).run();
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

    public synchronized void warnForNonPerfectSolution() {
        if (!warningForPerfectSolutionOccurred) {
            warningForPerfectSolutionOccurred = true;
            println("Warning: No perfect matching between shift and days has been found");
        }
    }

    public synchronized SpreadsheetReader getSpreadsheetReader() {
        return spreadsheetReader;
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

            if (solution.getCosts() == 0) {
                stop();
                numberOfFinishedSolutions = Config.NUMBER_OF_PARALLEL_THREADS;
            }
        }
        numberOfFinishedSolutions++;
    }
}
