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
        if (day1 < 0 || day1 > solution.length - 1 || day2 < 0 || day2 > solution.length - 1) {
            throw new IllegalArgumentException("Invalid operation on a solution");
        }
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
            boolean hasIntervalCosts = true;

            if (interval == 1) {
                hasIntervalCosts = calculateCostsForBlockShift(employee);
            } else {
                calculateCostOfSolutionForForbiddenShift(employee, interval);
            }

            costsOfSolution = hasIntervalCosts ? costsOfSolution + getPartOfCostsOfSolution(interval, employee)
                    : costsOfSolution;
        }
        calculateCostOfSolutionForNextMonth(lastOccurenceOfEmployee);

        return costsOfSolution;
    }

    private boolean calculateCostsForBlockShift(int employee) {
        boolean hasIntervalCosts = true;
        numberOfDirectFollowingShifts++;
        if (numberOfDirectFollowingShifts > input.getMaxLengthOfShiftPerEmployee(employee)) {
            costsOfSolution = costsOfSolution + Config.PENALTY_FOR_FORBIDDEN_SHIFT;
        }
        if (input.getWishedLengthOfShiftForEmployee(employee) <= 0
                || numberOfDirectFollowingShifts <= input.getWishedLengthOfShiftForEmployee(employee)) {
            hasIntervalCosts = false;
        }
        return hasIntervalCosts;
    }

    private void calculateCostOfSolutionForForbiddenShift(int employee, int interval) {
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
        if ((!input.isChangeOfShiftAllowedOnDay(day)) && day < solution.length - 1
                && solution[day] != solution[day + 1]) {
            costsOfSolution = costsOfSolution + Config.PENALTY_FOR_UNWANTED_SHIFT;
        }
    }

    private void calculateCostOfSolutionForNextMonth(int[] lastOccurenceOfEmployee) {
        for (int employee = 0; employee < lastOccurenceOfEmployee.length; employee++) {
            if (input.getWishedLengthOfShiftForEmployee(employee) > 0
                    && input.getDaysToWorkInTotalForEmployee(employee) > 0) {
                int interval = input.getLengthOfMonth() - lastOccurenceOfEmployee[employee];
                costsOfSolution = costsOfSolution + getPartOfCostsOfSolution(interval, employee);
            }
        }
    }

    private double getPartOfCostsOfSolution(int interval, int employee) {
        return (Math.abs(interval - input.getExpectedDaysBetweenShiftsForEmployee(employee)));
    }
}
