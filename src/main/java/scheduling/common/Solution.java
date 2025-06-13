package scheduling.common;

import java.util.Arrays;

import scheduling.spreadsheet.SpreadsheetReader;

public class Solution {
	private static final int UNKNOWN_COSTS = -1;
	private final int[] solution;
	private double costs;
	private final SpreadsheetReader spreadsheetReader;
	private final int[] lastOccurrenceOfEmployee;
	private final int[] lengthOfLastBlockShiftForEmployee;
	private int numberOfDirectFollowingShifts;
	private int numberOfRetries;
	private final int[] numberOfFreeDaysForEmployee;

	public Solution(int[] solution, int[] numberOfFreeDaysForEmployee, SpreadsheetReader input) {
		this.solution = solution;
		this.numberOfFreeDaysForEmployee = numberOfFreeDaysForEmployee;
		this.spreadsheetReader = input;
		this.costs = UNKNOWN_COSTS;
		this.numberOfRetries = 0;
		this.lastOccurrenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
		this.lengthOfLastBlockShiftForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
		this.numberOfDirectFollowingShifts = 1;
	}

	public boolean canBeRetried() {
		return numberOfRetries++ < Config.MAX_RETRIES_OF_SOLUTION;
	}

	public int getEmployeeForDay(int day) {
		return solution[day];
	}

	public void exchangeEmployeesOnDays(int day1, int day2) {
		costs = UNKNOWN_COSTS;
		int temp = solution[day1];
		solution[day1] = solution[day2];
		solution[day2] = temp;
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
		return employee == Config.MISSING_EMPLOYEE ? Config.MISSING_EMPLOYEE : numberOfFreeDaysForEmployee[employee];
	}

	public Solution createCopy() {
		return new Solution(Arrays.copyOf(solution, solution.length),
				Arrays.copyOf(numberOfFreeDaysForEmployee, numberOfFreeDaysForEmployee.length), spreadsheetReader);
	}

	public double getCosts() {
		if (costs != UNKNOWN_COSTS) {
			return costs;
		}
		initializeForCalculationOfCosts();
		costs = calculateTotalCosts();
		return costs;
	}

	private void initializeForCalculationOfCosts() {
		costs = Config.OPTIMAL_SOLUTION;
		Arrays.fill(lastOccurrenceOfEmployee, Config.MISSING_EMPLOYEE);
		Arrays.fill(lengthOfLastBlockShiftForEmployee, 1);
		numberOfDirectFollowingShifts = 1;
	}

	private double calculateTotalCosts() {
		double totalCosts = 0;
		for (int day = 0; day < solution.length; day++) {
			int employee = solution[day];

			if (employee == Config.MISSING_EMPLOYEE) {
				continue;
			}
			if (day != 0 && employee != solution[day - 1]) {
				initializeNextBlockShift(day);
			}

			totalCosts += calculateCostsForMandatoryBlockShiftOnDay(day);

			if (lastOccurrenceOfEmployee[employee] == Config.MISSING_EMPLOYEE) {
				lastOccurrenceOfEmployee[employee] = day;
				continue;
			}

			int interval = day - lastOccurrenceOfEmployee[employee];
			lastOccurrenceOfEmployee[employee] = day;

			if (interval == 1) {
				totalCosts += calculatePenaltyForTooLongBlockShift(employee);
			} else {
				totalCosts += calculatePenaltyForForbiddenShift(employee, interval);
			}

			totalCosts += calculateIntervalCosts(interval, employee);
		}
		return totalCosts;
	}

	private void initializeNextBlockShift(int day) {
		int previousEmployee = solution[day - 1];
		if (previousEmployee != Config.MISSING_EMPLOYEE) {
			lengthOfLastBlockShiftForEmployee[previousEmployee] = numberOfDirectFollowingShifts;
		}
		numberOfDirectFollowingShifts = 1;
	}

	private double calculateCostsForMandatoryBlockShiftOnDay(int day) {
		return (!spreadsheetReader.isSingleShiftAllowedOnDay(day) && day < solution.length - 1
				&& solution[day] != solution[day + 1]) ? Config.PENALTY_FOR_MANDATORY_BLOCK_SHIFT : 0;
	}

	private double calculatePenaltyForTooLongBlockShift(int employee) {
		numberOfDirectFollowingShifts++;
		return numberOfDirectFollowingShifts > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee)
				? Config.PENALTY_FOR_FORBIDDEN_SHIFT
				: 0;
	}

	private double calculatePenaltyForForbiddenShift(int employee, int interval) {
		return (spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee) != 1
				&& (interval <= lengthOfLastBlockShiftForEmployee[employee] || interval == Config.INTERVAL_FOR_ONE_DAY))
						? Config.PENALTY_FOR_FORBIDDEN_SHIFT
						: 0;
	}

	private double calculateIntervalCosts(int interval, int employee) {
		boolean hasIntervalCosts = spreadsheetReader.getWishedLengthOfShiftForEmployee(employee) > 0 && (interval != 1
				|| numberOfDirectFollowingShifts > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee));
		return hasIntervalCosts
				? Math.abs(interval - spreadsheetReader.getExpectedDaysBetweenShiftsForEmployee(employee))
				: 0;
	}
}