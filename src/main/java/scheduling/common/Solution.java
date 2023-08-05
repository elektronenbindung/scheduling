package scheduling.common;

import java.util.Arrays;

import scheduling.spreadsheet.SpreadsheetReader;

public class Solution {
    private int[] solution;
    private double costs;
    private SpreadsheetReader input;
    private int[] lastOccurenceOfEmployee;
    private int[] lengthOfLastBlockShiftForEmployee;
    private int numberOfDirectFollowingShifts;
    private int numberOfRetrys;

    public Solution(int[] solution, SpreadsheetReader input) {
        this.solution = solution;
        this.input = input;
        costs = -1;
        numberOfRetrys = 0;
        lastOccurenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
        lengthOfLastBlockShiftForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
        numberOfDirectFollowingShifts = 1;
    }

    public boolean canBeRetried() {
        if (numberOfRetrys < Config.MAX_RETRYS_OF_SOLUTION) {
            numberOfRetrys++;
            return true;
        }
        return false;
    }

    public int getEmployeeForDay(int day) {
        return solution[day];
    }

    public void exchangeEmployeesOnDays(int day1, int day2) {
        costs = -1;
        int changedEmployee = solution[day1];
        solution[day1] = solution[day2];
        solution[day2] = changedEmployee;
    }

    public Solution createCopy() {
        return new Solution(Arrays.copyOf(solution, solution.length), input);
    }

    public double getCosts() {
        if (costs != -1) {
            return costs;
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

            costs = costs + calculateCostsForMandatoryBlockShiftOnDay(day);

            if (lastOccurenceOfEmployee[employee] == -1) {
                lastOccurenceOfEmployee[employee] = day;
                continue;
            }

            int interval = day - lastOccurenceOfEmployee[employee];
            lastOccurenceOfEmployee[employee] = day;

            if (interval == 1) {
                costs = costs + calculatePenaltyForBlockShift(employee);
            } else {
                costs = costs + calculatePenaltyForForbiddenShift(employee, interval);
            }

            costs = costs + calculateIntervalCosts(interval, employee);
        }

        return costs;
    }

    private void initializeForCalculationOfCosts() {
        costs = 0;
        Arrays.fill(lastOccurenceOfEmployee, -1);

        Arrays.fill(lengthOfLastBlockShiftForEmployee, 1);

        numberOfDirectFollowingShifts = 1;
    }

    private void initializeNextBlockShift(int day) {
        int previousEmployee = solution[day - 1];
        if (previousEmployee != -1) {
            lengthOfLastBlockShiftForEmployee[previousEmployee] = numberOfDirectFollowingShifts;
        }
        numberOfDirectFollowingShifts = 1;
    }

    private double calculateCostsForMandatoryBlockShiftOnDay(int day) {
        if ((!input.isSingleShiftAllowedOnDay(day)) && day < solution.length - 1
                && solution[day] != solution[day + 1]) {
            return Config.PENALTY_FOR_UNWANTED_SHIFT;
        }
        return 0;
    }

    private double calculatePenaltyForBlockShift(int employee) {
        numberOfDirectFollowingShifts++;
        if (numberOfDirectFollowingShifts > input.getMaxLengthOfShiftPerEmployee(employee)) {
            return Config.PENALTY_FOR_FORBIDDEN_SHIFT;
        }
        return 0;
    }

    private double calculatePenaltyForForbiddenShift(int employee, int interval) {
        if (input.getMaxLengthOfShiftPerEmployee(employee) != 1
                && (interval <= lengthOfLastBlockShiftForEmployee[employee] || interval == 2)) {
            return Config.PENALTY_FOR_FORBIDDEN_SHIFT;
        }
        return 0;
    }

    private double calculateIntervalCosts(int interval, int employee) {
        boolean hasIntervalCosts = input.getWishedLengthOfShiftForEmployee(employee) > 0 && (interval != 1
                || numberOfDirectFollowingShifts > Math.ceil(input.getWishedLengthOfShiftForEmployee(employee)));

        if (hasIntervalCosts) {
            double intervalCosts = Math.abs(interval - input.getExpectedDaysBetweenShiftsForEmployee(employee));
            return intervalCosts;
        }
        return 0;
    }

}
