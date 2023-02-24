package scheduling;

import java.util.Arrays;

public class Solution {
    private int[] solution;
    private double costsOfSolution;
    private SpreadsheetReader input;
    private int[] lastOccurenceOfEmployee;
    private int[] lengthOfLastBlockShiftForEmployee;
    private int numberOfDirectFollowingShifts;

    public Solution(int[] solution, SpreadsheetReader input) {
        this.solution = solution;
        this.input = input;
        costsOfSolution = -1;
        lastOccurenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
        lengthOfLastBlockShiftForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
        numberOfDirectFollowingShifts = 1;
    }

    public int getEmployeeForDay(int day) {
        return solution[day];
    }

    public void exchangeEmployeesOnDays(int day1, int day2) {
        costsOfSolution = -1;
        int changedEmployee = solution[day1];
        solution[day1] = solution[day2];
        solution[day2] = changedEmployee;
    }

    public Solution createCopy() {
        return new Solution(Arrays.copyOf(solution, solution.length), input);
    }

    public double getCosts() {
        if (costsOfSolution != -1) {
            return costsOfSolution;
        }
        initializeForCalculationOfCosts();

        for (int day = 0; day < solution.length; day++) {
            int employee = solution[day];

            if (employee == -1) {
                continue;
            }
            if (day != 0 && employee != solution[day - 1]) {
                initializeNextBlockShift(day);
            }

            calculateCostOfSolutionForMandatoryBlockShiftOnDay(day);

            if (lastOccurenceOfEmployee[employee] == -1) {
                lastOccurenceOfEmployee[employee] = day;
                continue;
            }

            int interval = day - lastOccurenceOfEmployee[employee];
            lastOccurenceOfEmployee[employee] = day;

            if (interval == 1) {
                calculatePenaltyForBlockShift(employee);
            } else {
                calculatePenaltyForForbiddenShift(employee, interval);
            }

            calculateIntervalCosts(interval, employee);
        }

        return costsOfSolution;
    }

    private void calculatePenaltyForBlockShift(int employee) {
        numberOfDirectFollowingShifts++;
        if (numberOfDirectFollowingShifts > input.getMaxLengthOfShiftPerEmployee(employee)) {
            costsOfSolution = costsOfSolution + Config.PENALTY_FOR_FORBIDDEN_SHIFT;
        }
    }

    private void calculatePenaltyForForbiddenShift(int employee, int interval) {
        if (input.getMaxLengthOfShiftPerEmployee(employee) != 1
                && (interval <= lengthOfLastBlockShiftForEmployee[employee] || interval == 2)) {
            costsOfSolution = costsOfSolution + Config.PENALTY_FOR_FORBIDDEN_SHIFT;
        }
    }

    private void initializeNextBlockShift(int day) {
        int previousEmployee = solution[day - 1];
        if (previousEmployee != -1) {
            lengthOfLastBlockShiftForEmployee[previousEmployee] = numberOfDirectFollowingShifts;
        }
        numberOfDirectFollowingShifts = 1;
    }

    private void initializeForCalculationOfCosts() {
        costsOfSolution = 0;
        Arrays.fill(lastOccurenceOfEmployee, -1);

        Arrays.fill(lengthOfLastBlockShiftForEmployee, 1);

        numberOfDirectFollowingShifts = 1;
    }

    private void calculateCostOfSolutionForMandatoryBlockShiftOnDay(int day) {
        if ((!input.isMandatoryBlockShiftOnDay(day)) && day < solution.length - 1
                && solution[day] != solution[day + 1]) {
            costsOfSolution = costsOfSolution + Config.PENALTY_FOR_UNWANTED_SHIFT;
        }
    }

    private void calculateIntervalCosts(int interval, int employee) {
        boolean hasIntervalCosts = input.getWishedLengthOfShiftForEmployee(employee) > 0 && (interval != 1
                || numberOfDirectFollowingShifts > input.getWishedLengthOfShiftForEmployee(employee));

        if (hasIntervalCosts) {
            double intervalCosts = Math.abs(interval - input.getExpectedDaysBetweenShiftsForEmployee(employee));
            costsOfSolution = costsOfSolution + intervalCosts;
        }

    }
}
