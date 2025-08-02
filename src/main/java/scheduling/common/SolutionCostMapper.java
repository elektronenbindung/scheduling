package scheduling.common;

import java.util.Arrays;
import scheduling.spreadsheet.SpreadsheetReader;

public class SolutionCostMapper {

    private final int[] solution;
    private final SpreadsheetReader spreadsheetReader;

    public SolutionCostMapper(int[] solution, SpreadsheetReader input) {
        this.solution = solution;
        this.spreadsheetReader = input;
    }

    public double calculateTotalCosts() {
        if (solution == null || solution.length == 0) {
            return 0.0;
        }

        CalculationState state = new CalculationState();
        double totalCosts = Config.OPTIMAL_SOLUTION;

        for (int day = 0; day < solution.length; day++) {
            int currentEmployee = solution[day];

            if (currentEmployee == Config.MISSING_EMPLOYEE) {
                continue;
            }

            if (day > 0 && currentEmployee != solution[day - 1]) {
                handleShiftTransition(solution[day - 1], state);
            }

            totalCosts += calculateDailyPenalties(day, currentEmployee, state);

            updateEmployeeState(day, currentEmployee, state);
        }

        return totalCosts;
    }

    private void handleShiftTransition(int previousEmployee, CalculationState state) {
        if (previousEmployee != Config.MISSING_EMPLOYEE) {
            state.lengthOfLastBlockShift[previousEmployee] = state.currentConsecutiveShifts;
        }
        state.currentConsecutiveShifts = 1;
    }

    private double calculateDailyPenalties(int day, int employee, CalculationState state) {
        double dailyCosts = 0.0;

        dailyCosts += calculatePenaltyForMandatoryBlockShift(day);

        if (state.lastOccurrenceOfEmployee[employee] == Config.MISSING_EMPLOYEE) {
            return dailyCosts;
        }

        int daysSinceLastShift = day - state.lastOccurrenceOfEmployee[employee];

        if (daysSinceLastShift == 1) {
            state.currentConsecutiveShifts++;
            dailyCosts += calculatePenaltyForTooLongBlockShift(employee, state.currentConsecutiveShifts);
        } else {
            dailyCosts += calculatePenaltyForForbiddenShiftInterval(employee, daysSinceLastShift, state);
        }

        dailyCosts += calculatePenaltyForWishedInterval(employee, daysSinceLastShift, state.currentConsecutiveShifts);

        return dailyCosts;
    }

    private void updateEmployeeState(int day, int employee, CalculationState state) {
        state.lastOccurrenceOfEmployee[employee] = day;
    }

    private double calculatePenaltyForMandatoryBlockShift(int day) {
        boolean isViolation = !spreadsheetReader.isSingleShiftAllowedOnDay(day)
                && day < solution.length - 1
                && solution[day] != solution[day + 1];
        return isViolation ? Config.PENALTY_FOR_MANDATORY_BLOCK_SHIFT : 0;
    }

    private double calculatePenaltyForTooLongBlockShift(int employee, int consecutiveDays) {
        return consecutiveDays > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee)
                ? Config.PENALTY_FOR_FORBIDDEN_SHIFT
                : 0;
    }

    private double calculatePenaltyForForbiddenShiftInterval(int employee, int daysSinceLastShift,
            CalculationState state) {
        return isForbiddenShortInterval(employee, daysSinceLastShift, state)
                ? Config.PENALTY_FOR_FORBIDDEN_SHIFT
                : 0;
    }

    private boolean isForbiddenShortInterval(int employee, int daysSinceLastShift, CalculationState state) {
        boolean canWorkSingleDays = spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee) == 1;
        if (canWorkSingleDays) {
            return false;
        }

        boolean isTooSoonAfterLastBlock = daysSinceLastShift <= state.lengthOfLastBlockShift[employee];
        boolean isExactlyOneDayOff = daysSinceLastShift == Config.INTERVAL_FOR_ONE_DAY;

        return isTooSoonAfterLastBlock || isExactlyOneDayOff;
    }

    private double calculatePenaltyForWishedInterval(int employee, int daysSinceLastShift,
            int currentConsecutiveShifts) {
        boolean hasWishedInterval = spreadsheetReader.getWishedLengthOfShiftForEmployee(employee) > 0;
        boolean isNewShiftBlock = daysSinceLastShift > 1;
        boolean isShiftBlockTooLong = currentConsecutiveShifts > spreadsheetReader
                .getMaxLengthOfShiftPerEmployee(employee);

        if (hasWishedInterval && (isNewShiftBlock || isShiftBlockTooLong)) {
            double expectedInterval = spreadsheetReader.getExpectedDaysBetweenShiftsForEmployee(employee);
            return Math.abs(daysSinceLastShift - expectedInterval);
        }
        return 0;
    }

    private static class CalculationState {
        final int[] lastOccurrenceOfEmployee;
        final int[] lengthOfLastBlockShift;
        int currentConsecutiveShifts;

        CalculationState() {

            this.lastOccurrenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
            Arrays.fill(this.lastOccurrenceOfEmployee, Config.MISSING_EMPLOYEE);

            this.lengthOfLastBlockShift = new int[Config.NUMBER_OF_EMPLOYEES];
            Arrays.fill(this.lengthOfLastBlockShift, 1);

            this.currentConsecutiveShifts = 1;
        }
    }
}
