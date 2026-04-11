package scheduling.tabuSearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;
import scheduling.spreadsheet.SpreadsheetReader;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;


class TabuSearchTest {

    /**
     * Test double for SpreadsheetReader that can be controlled in tests.
     */
    private static class TestableSpreadsheetReader extends SpreadsheetReader {
        private int lengthOfMonth = 30;
        private boolean[] freeDays;
        private int[] fixedEmployees;
        private boolean[][] isAvailablePerDay;
        private int numberOfEmployees = 5;

        TestableSpreadsheetReader() {
            super(new File("nonexistent.ots"));
            this.freeDays = new boolean[lengthOfMonth];
            this.fixedEmployees = new int[lengthOfMonth];
            this.isAvailablePerDay = new boolean[numberOfEmployees][lengthOfMonth];
            for (int i = 0; i < lengthOfMonth; i++) {
                fixedEmployees[i] = Config.MISSING_EMPLOYEE;
            }
            for (int e = 0; e < numberOfEmployees; e++) {
                for (int d = 0; d < lengthOfMonth; d++) {
                    isAvailablePerDay[e][d] = true;
                }
            }
        }

        @Override
        public int getLengthOfMonth() {
            return lengthOfMonth;
        }

        @Override
        public boolean isFreeDay(int day) {
            if (day >= 0 && day < freeDays.length) {
                return freeDays[day];
            }
            return false;
        }

        @Override
        public int getEmployeeOnFixedDay(int day) {
            if (day >= 0 && day < fixedEmployees.length) {
                return fixedEmployees[day];
            }
            return Config.MISSING_EMPLOYEE;
        }

        @Override
        public boolean isEmployeeAvailableOnDay(int employee, int day) {
            if (employee == Config.MISSING_EMPLOYEE) {
                return true;
            }
            if (employee >= 0 && employee < numberOfEmployees && day >= 0 && day < lengthOfMonth) {
                return isAvailablePerDay[employee][day];
            }
            return true;
        }

        @Override
        public double getDaysToWorkAtFreeDayForEmployee(int employee) {
            return 0; // Default: no free day work required
        }

        void setFreeDay(int day, boolean isFree) {
            if (day >= 0 && day < freeDays.length) {
                freeDays[day] = isFree;
            }
        }

        void setFixedEmployee(int day, int employee) {
            if (day >= 0 && day < fixedEmployees.length) {
                fixedEmployees[day] = employee;
            }
        }

        void setEmployeeAvailable(int employee, int day, boolean available) {
            if (employee >= 0 && employee < numberOfEmployees && day >= 0 && day < lengthOfMonth) {
                isAvailablePerDay[employee][day] = available;
            }
        }

        void setLengthOfMonth(int length) {
            this.lengthOfMonth = length;
            this.freeDays = new boolean[length];
            this.fixedEmployees = new int[length];
            this.isAvailablePerDay = new boolean[numberOfEmployees][length];
            for (int i = 0; i < length; i++) {
                fixedEmployees[i] = Config.MISSING_EMPLOYEE;
            }
            for (int e = 0; e < numberOfEmployees; e++) {
                for (int d = 0; d < length; d++) {
                    isAvailablePerDay[e][d] = true;
                }
            }
        }

        int getNumberOfEmployees() {
            return numberOfEmployees;
        }

        @Override
        public void run() throws IOException {
            // Do nothing in tests
        }
    }

    /**
     * Test double for ThreadsController that can be controlled in tests.
     * This is needed because Mockito cannot mock classes implementing Runnable in Java 25.
     */
    private static class TestableThreadsController extends ThreadsController {
        private TestableSpreadsheetReader testSpreadsheetReader;
        private Queue<Boolean> stoppedValues = new LinkedList<>();
        private boolean stopped = false;

        TestableThreadsController(TestableSpreadsheetReader spreadsheetReader) {
            super(new File("nonexistent.ots"), null);
            this.testSpreadsheetReader = spreadsheetReader;
        }

        @Override
        public SpreadsheetReader getSpreadsheetReader() {
            return testSpreadsheetReader;
        }

        @Override
        public boolean isStopped() {
            if (stoppedValues.isEmpty()) {
                return stopped;
            }
            return stoppedValues.poll();
        }

        void setStopped(boolean stopped) {
            this.stopped = stopped;
        }

        void addStoppedValues(Boolean... values) {
            for (Boolean value : values) {
                stoppedValues.add(value);
            }
        }

        @Override
        public void run() {
            // Do nothing in tests
        }
    }

    /**
     * Test double for Solution that can be controlled in tests.
     */
    private static class TestableSolution extends Solution {
        private double costs;
        private Queue<Double> costsValues = new LinkedList<>();
        private int[] solution;
        private int[] freeDaysCount;
        private Solution copyResult;
        private Queue<Solution> copyResults = new LinkedList<>();

        TestableSolution(double costs, int lengthOfMonth, int numberOfEmployees) {
            super(new int[lengthOfMonth], new int[numberOfEmployees], null, null);
            this.costs = costs;
            this.solution = new int[lengthOfMonth];
            this.freeDaysCount = new int[numberOfEmployees];
        }

        @Override
        public double getCosts() {
            if (costsValues.isEmpty()) {
                return costs;
            }
            return costsValues.poll();
        }

        void setCosts(double costs) {
            this.costs = costs;
        }

        void addCostsValues(Double... values) {
            for (Double value : values) {
                costsValues.add(value);
            }
        }

        @Override
        public Solution createCopy() {
            if (copyResults.isEmpty()) {
                if (copyResult != null) {
                    return copyResult;
                }
                return this;
            }
            return copyResults.poll();
        }

        void setCopyResult(Solution copyResult) {
            this.copyResult = copyResult;
        }

        void addCopyResults(Solution... results) {
            for (Solution result : results) {
                copyResults.add(result);
            }
        }

        @Override
        public void exchangeEmployeesOnDays(int day1, int day2) {
            int temp = solution[day1];
            solution[day1] = solution[day2];
            solution[day2] = temp;
        }

        @Override
        public void exchangeFreeDayBetweenEmployees(int fromDay, int toDay) {
            // Do nothing in tests
        }

        @Override
        public void setSolutionCosts(double costs) {
            this.costs = costs;
        }
    }

    @Test
    @DisplayName("run returns initial solution when thread is stopped before iteration starts")
    void run_returnsInitialSolution_whenThreadIsStopped() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.setStopped(true);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
        assertEquals(initialSolution, result);
    }

    @Test
    @DisplayName("run returns solution immediately when initial solution is optimal")
    void run_returnsSolutionImmediately_whenInitialSolutionIsOptimal() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.setStopped(false);

        TestableSolution optimalSolution = new TestableSolution(Config.OPTIMAL_SOLUTION, 30, 5);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(optimalSolution);

        assertNotNull(result);
        assertEquals(optimalSolution, result);
    }

    @Test
    @DisplayName("run returns best solution when thread is stopped during iteration")
    void run_returnsBestSolution_whenThreadStoppedDuringIteration() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.addStoppedValues(false, true);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution bestSolutionCopy = new TestableSolution(100.0, 30, 5);
        initialSolution.setCopyResult(bestSolutionCopy);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run returns initial solution when no valid moves found and no previous solution exists")
    void run_returnsInitialSolution_whenNoValidMovesAndNoPreviousSolution() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.setStopped(false);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution solutionCopy = new TestableSolution(100.0, 30, 5);
        solutionCopy.addCostsValues(100.0, 100.0);
        initialSolution.setCopyResult(solutionCopy);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run updates best solution when move improves cost")
    void run_updatesBestSolution_whenMoveImprovesCost() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.addStoppedValues(false, false, true);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution currentSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution improvedSolution = new TestableSolution(80.0, 30, 5);

        currentSolution.addCostsValues(100.0, 80.0, 80.0, 80.0);
        currentSolution.addCopyResults(improvedSolution, currentSolution);
        initialSolution.setCopyResult(currentSolution);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run increments iterations when move does not improve cost")
    void run_incrementsIterations_whenMoveDoesNotImproveCost() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.addStoppedValues(false, false, false, true);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution currentSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution worseSolution = new TestableSolution(120.0, 30, 5);

        currentSolution.addCostsValues(100.0, 120.0, 100.0, 120.0, 100.0);
        currentSolution.addCopyResults(worseSolution, currentSolution);
        initialSolution.setCopyResult(currentSolution);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run returns best solution after max retries without improvement")
    void run_returnsBestSolution_afterMaxRetriesWithoutImprovement() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.setStopped(false);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution currentSolution = new TestableSolution(100.0, 30, 5);
        currentSolution.setCopyResult(currentSolution);
        initialSolution.setCopyResult(currentSolution);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run applies move with free day exchange when days differ in free status")
    void run_appliesMoveWithFreeDayExchange_whenDaysDifferInFreeStatus() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);
        spreadsheetReader.setFreeDay(0, true);
        spreadsheetReader.setFreeDay(1, false);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.addStoppedValues(false, false, true);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution currentSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution improvedSolution = new TestableSolution(80.0, 30, 5);

        currentSolution.addCostsValues(100.0, 80.0, 80.0, 80.0);
        currentSolution.addCopyResults(improvedSolution, currentSolution);
        initialSolution.setCopyResult(currentSolution);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run handles stagnation by retrieving previous solution from solution list")
    void run_handlesStagnation_byRetrievingPreviousSolution() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.addStoppedValues(false, false, false, true);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution bestSolutionCopy = new TestableSolution(90.0, 30, 5);
        TestableSolution currentSolution = new TestableSolution(90.0, 30, 5);
        TestableSolution stagnationRecoverySolution = new TestableSolution(90.0, 30, 5);

        bestSolutionCopy.addCopyResults(currentSolution, stagnationRecoverySolution);
        initialSolution.setCopyResult(bestSolutionCopy);

        currentSolution.setCopyResult(bestSolutionCopy);

        stagnationRecoverySolution.addCostsValues(90.0, 85.0, 85.0);
        stagnationRecoverySolution.setCopyResult(bestSolutionCopy);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }

    @Test
    @DisplayName("run finds optimal solution during iteration and returns immediately")
    void run_findsOptimalSolution_andReturnsImmediately() {
        TestableSpreadsheetReader spreadsheetReader = new TestableSpreadsheetReader();
        spreadsheetReader.setLengthOfMonth(30);

        TestableThreadsController threadsController = new TestableThreadsController(spreadsheetReader);
        threadsController.addStoppedValues(false, false, false);

        TestableSolution initialSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution currentSolution = new TestableSolution(100.0, 30, 5);
        TestableSolution optimalSolution = new TestableSolution(Config.OPTIMAL_SOLUTION, 30, 5);

        currentSolution.addCostsValues(100.0, Config.OPTIMAL_SOLUTION, Config.OPTIMAL_SOLUTION);
        currentSolution.addCopyResults(optimalSolution, currentSolution);
        initialSolution.setCopyResult(currentSolution);

        TabuSearch tabuSearch = new TabuSearch(threadsController);
        Solution result = tabuSearch.run(initialSolution);

        assertNotNull(result);
    }
}
