package scheduling.common;

import java.util.Arrays;

import scheduling.spreadsheet.SpreadsheetReader;

public class Solution {
    private static final int UNKNOWN_COSTS = -1;
    private int[] solution;
    private double costs;
    private SpreadsheetReader input;
    private int[] lastOccurrenceOfEmployee;
    private int[] lengthOfLastBlockShiftForEmployee;
    private int numberOfDirectFollowingShifts;
    private int numberOfRetries;
    private int[] numberOfFreeDaysForEmployee;

    public Solution(int[] solution, int[] numberOfFreeDaysForEmployee, SpreadsheetReader input) {
        this.solution = solution;
        this.numberOfFreeDaysForEmployee = numberOfFreeDaysForEmployee;
        this.input = input;
        costs = UNKNOWN_COSTS;
        numberOfRetries = 0;
        lastOccurrenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
        lengthOfLastBlockShiftForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
        numberOfDirectFollowingShifts = 1;
    }

    public boolean canBeRetried() {
        if (numberOfRetries < Config.MAX_RETRIES_OF_SOLUTION) {
            numberOfRetries++;
            return true;
        }
        return false;
    }

    public int getEmployeeForDay(int day) {
        return solution[day];
    }

    public void exchangeEmployeesOnDays(int day1, int day2) {
        costs = UNKNOWN_COSTS;
        int changedEmployee = solution[day1];
        solution[day1] = solution[day2];
        solution[day2] = changedEmployee;
    }

    public void exchangeFreeDayBetweenEmployees(int fromDay, int toDay) {
        int fromEmployee = getEmployeeForDay(fromDay);
        int toEmployee = getEmployeeForDay(toDay);

        if (fromEmployee != Config.MISSING_EMPLOYEE) {
            numberOfFreeDaysForEmployee[fromEmployee]--;
        }
        if (toEmployee != Config.MISSING_EMPLOYEE) {
            numberOfFreeDaysForEmployee[toEmployee]++;
        }
    }

    public int getNumberOfFreeDaysForEmployee(int employee) {
        return numberOfFreeDaysForEmployee[employee];
    }

    public Solution createCopy() {
        return new Solution(Arrays.copyOf(solution, solution.length),
                Arrays.copyOf(numberOfFreeDaysForEmployee, numberOfFreeDaysForEmployee.length), input);
    }

    public double getCosts() {
        if (costs != UNKNOWN_COSTS) {
            return costs;
        }
        initializeForCalculationOfCosts();

        for (int day = 0; day < solution.length; day++) {
            int employee = solution[day];

            if (employee == Config.MISSING_EMPLOYEE) {
                continue;
            }
            if (day != 0 && employee != solution[day - 1]) {
                initializeNextBlockShift(day);
            }

            costs = costs + calculateCostsForMandatoryBlockShiftOnDay(day);

            if (lastOccurrenceOfEmployee[employee] == Config.MISSING_EMPLOYEE) {
                lastOccurrenceOfEmployee[employee] = day;
                continue;
            }

            int interval = day - lastOccurrenceOfEmployee[employee];
            lastOccurrenceOfEmployee[employee] = day;

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
        Arrays.fill(lastOccurrenceOfEmployee, Config.MISSING_EMPLOYEE);

        Arrays.fill(lengthOfLastBlockShiftForEmployee, 1);

        numberOfDirectFollowingShifts = 1;
    }

    private void initializeNextBlockShift(int day) {
        int previousEmployee = solution[day - 1];
        if (previousEmployee != Config.MISSING_EMPLOYEE) {
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
