package scheduling.common;

import java.util.Arrays;

import scheduling.spreadsheet.SpreadsheetReader;

public class Solution {
	static final int UNKNOWN_COSTS = -1;
	private final int[] solution;
	private final SpreadsheetReader spreadsheetReader;
	private int numberOfRetries;
	private final int[] numberOfFreeDaysForEmployee;
	private double costs;
	private final SolutionCostMapper solutionCostMapper;

	public Solution(int[] solution, int[] numberOfFreeDaysForEmployee, SpreadsheetReader input) {
		this.solution = solution;
		this.numberOfFreeDaysForEmployee = numberOfFreeDaysForEmployee;
		this.spreadsheetReader = input;
		this.costs = UNKNOWN_COSTS;
		this.numberOfRetries = 0;
		this.solutionCostMapper = new SolutionCostMapper(this, input);
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
		int direction = spreadsheetReader.isFreeDay(fromDay) ? 1 : -1;

		if (fromEmployee != Config.MISSING_EMPLOYEE) {
			numberOfFreeDaysForEmployee[fromEmployee] -= direction;
		}
		if (toEmployee != Config.MISSING_EMPLOYEE) {
			numberOfFreeDaysForEmployee[toEmployee] += direction;
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
		costs = solutionCostMapper.calculateTotalCosts();
		return costs;
	}

}