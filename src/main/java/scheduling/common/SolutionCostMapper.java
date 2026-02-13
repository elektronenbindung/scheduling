package scheduling.common;

import java.util.Arrays;
import scheduling.spreadsheet.SpreadsheetReader;

public class SolutionCostMapper {

	private final SpreadsheetReader spreadsheetReader;
	private final CalculationState state;

	public SolutionCostMapper(SpreadsheetReader reader) {
		this.spreadsheetReader = reader;
		this.state = new CalculationState();
	}

	public double calculateTotalCosts(Solution solution) {
		if (solution == null) {
			return Config.OPTIMAL_SOLUTION;
		}
		state.initialize();
		double totalCosts = Config.OPTIMAL_SOLUTION;

		for (int day = 0; day < spreadsheetReader.getLengthOfMonth(); day++) {
			int currentEmployee = solution.getEmployeeForDay(day);

			if (currentEmployee == Config.MISSING_EMPLOYEE) {
				continue;
			}

			if (day > 0 && currentEmployee != solution.getEmployeeForDay(day - 1)) {
				handleShiftTransition(solution.getEmployeeForDay(day - 1));
			}

			totalCosts += calculateDailyPenalties(solution, day);

			updateEmployeeState(currentEmployee, day);
		}

		return totalCosts;
	}

	private void handleShiftTransition(int previousEmployee) {
		if (previousEmployee != Config.MISSING_EMPLOYEE) {
			state.lengthOfLastBlockShift[previousEmployee] = state.currentConsecutiveShifts;
		}
		state.currentConsecutiveShifts = 1;
	}

	private double calculateDailyPenalties(Solution solution, int day) {
		double dailyCosts = 0.0;
		int employee = solution.getEmployeeForDay(day);

		dailyCosts += calculatePenaltyForMandatoryBlockShift(solution, day);

		if (state.lastOccurrenceOfEmployee[employee] == Config.MISSING_EMPLOYEE) {
			return dailyCosts;
		}

		int daysSinceLastShift = day - state.lastOccurrenceOfEmployee[employee];

		if (daysSinceLastShift == 1) {
			state.currentConsecutiveShifts++;
			dailyCosts += calculatePenaltyForTooLongBlockShift(employee, state.currentConsecutiveShifts);
		} else {
			dailyCosts += calculatePenaltyForForbiddenShiftInterval(employee, daysSinceLastShift);
		}

		dailyCosts += calculatePenaltyForWishedInterval(employee, daysSinceLastShift, state.currentConsecutiveShifts);

		return dailyCosts;
	}

	private void updateEmployeeState(int employee, int day) {
		state.lastOccurrenceOfEmployee[employee] = day;
	}

	private double calculatePenaltyForMandatoryBlockShift(Solution solution, int day) {
		boolean isViolation = spreadsheetReader.isSingleShiftForbiddenOnDay(day)
				&& day < spreadsheetReader.getLengthOfMonth() - 1
				&& solution.getEmployeeForDay(day) != solution.getEmployeeForDay(day + 1);
		return isViolation ? Config.PENALTY_FOR_MANDATORY_BLOCK_SHIFT : 0;
	}

	private double calculatePenaltyForTooLongBlockShift(int employee, int consecutiveDays) {
		return consecutiveDays > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee)
				? Config.PENALTY_FOR_FORBIDDEN_SHIFT
				: 0;
	}

	private double calculatePenaltyForForbiddenShiftInterval(int employee, int daysSinceLastShift) {
		return isForbiddenShortInterval(employee, daysSinceLastShift) ? Config.PENALTY_FOR_FORBIDDEN_SHIFT : 0;
	}

	private boolean isForbiddenShortInterval(int employee, int daysSinceLastShift) {
		boolean canWorkSingleDays = spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee) == 1;
		if (canWorkSingleDays) {
			return false;
		}

		boolean isTooSoonAfterLastBlock = daysSinceLastShift <= (state.lengthOfLastBlockShift[employee] +
				spreadsheetReader.getAdditionalFreeDaysBetweenShifts(employee));
		boolean isExactlyOneDayOff = daysSinceLastShift == Config.INTERVAL_FOR_ONE_DAY;

		return isTooSoonAfterLastBlock || isExactlyOneDayOff;
	}

	private double calculatePenaltyForWishedInterval(int employee, int daysSinceLastShift, int currentConsecutiveShifts) {
		if (spreadsheetReader.getWishedLengthOfShiftForEmployee(employee) <= 0) {
			return 0;
		}

		boolean isNewShiftBlock = daysSinceLastShift > 1;
		boolean isShiftBlockTooLong = currentConsecutiveShifts > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee);

		if (!isNewShiftBlock && !isShiftBlockTooLong) {
			return 0;
		}

		double expectedInterval = spreadsheetReader.getExpectedDaysBetweenShiftsForEmployee(employee);
		double additionalFreeDays = spreadsheetReader.getAdditionalFreeDaysBetweenShifts(employee);

		boolean shouldResetOffset = (employee == Config.MISSING_EMPLOYEE)
				|| (additionalFreeDays > 0)
				|| (additionalFreeDays == Config.MISSING_EMPLOYEE);

		double offset = shouldResetOffset ? 0 : additionalFreeDays;

		return Math.max(0, Math.abs(daysSinceLastShift - expectedInterval) + offset);
	}

	private static class CalculationState {
		final int[] lastOccurrenceOfEmployee;
		final int[] lengthOfLastBlockShift;
		int currentConsecutiveShifts;

		CalculationState() {
			this.lastOccurrenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
			this.lengthOfLastBlockShift = new int[Config.NUMBER_OF_EMPLOYEES];
		}

		void initialize() {
			Arrays.fill(this.lastOccurrenceOfEmployee, Config.MISSING_EMPLOYEE);
			Arrays.fill(this.lengthOfLastBlockShift, 1);
			this.currentConsecutiveShifts = 1;
		}
	}
}
