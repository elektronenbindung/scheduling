package scheduling.tabuSearch;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;
import scheduling.spreadsheet.SpreadsheetReader;

import java.util.Random;

public class TabuSearch {

	private final TabuList tabuList;
	private final SolutionList solutionList;
	private final Random random;
	private final ThreadsController threadsController;
	private final SpreadsheetReader spreadsheetReader;

	public TabuSearch(ThreadsController threadsController) {
		this.tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
		this.solutionList = new SolutionList(Config.LENGTH_OF_SOLUTION_LIST);
		this.random = new Random();
		this.threadsController = threadsController;
		this.spreadsheetReader = threadsController.getSpreadsheetReader();
	}

	public Solution run(Solution initialSolution) {
		Solution bestSolution = initialSolution;
		Solution currentSolution = bestSolution.createCopy();
		solutionList.add(bestSolution);

		for (int retriesWithoutImprovement = 0; retriesWithoutImprovement < Config.MAX_RETRIES_OF_TABU_SEARCH; retriesWithoutImprovement++) {
			int invalidRetries = 0;

			while (invalidRetries < Config.RETRIES_OF_INVALID_SOLUTION) {
				if (isSearchFinished(bestSolution)) {
					return bestSolution;
				}
				invalidRetries++;
				DaysTuple move = generateRandomMove();

				if (isSwapOfShiftForbidden(currentSolution, move)) {
					if (invalidRetries == Config.RETRIES_OF_INVALID_SOLUTION) {
						currentSolution = solutionList.getPreviousSolution();
						if (currentSolution == null) {
							return bestSolution;
						}
						currentSolution = currentSolution.createCopy();
						tabuList.reset();
					}
					continue;
				}

				applyMove(currentSolution, move);

				if (currentSolution.getCosts() < bestSolution.getCosts()) {
					solutionList.add(currentSolution);
					bestSolution = currentSolution;
					retriesWithoutImprovement = 0;
					currentSolution = bestSolution.createCopy();
				}
				break;
			}
		}
		return bestSolution;
	}

	private void applyMove(Solution solution, DaysTuple move) {
		tabuList.add(move);

		if (spreadsheetReader.isFreeDay(move.fromDay()) != spreadsheetReader.isFreeDay(move.toDay())) {
			solution.exchangeFreeDayBetweenEmployees(move.fromDay(), move.toDay());
		}

		solution.exchangeEmployeesOnDays(move.fromDay(), move.toDay());
	}

	private boolean isSearchFinished(Solution bestSolution) {
		return threadsController.isStopped() || bestSolution.getCosts() == Config.OPTIMAL_SOLUTION;
	}

	private DaysTuple generateRandomMove() {
		int lengthOfMonth = spreadsheetReader.getLengthOfMonth();
		int fromDay = random.nextInt(lengthOfMonth);
		int toDay = random.nextInt(lengthOfMonth);
		return new DaysTuple(fromDay, toDay);
	}

	private boolean isSwapOfShiftForbidden(Solution currentSolution, DaysTuple move) {
		if (move.fromDay() == move.toDay() || tabuList.contains(move)) {
			return true;
		}

		return areFreeDaysForbidden(currentSolution, move)
				|| isAtLeastOneEmployeeUnavailable(currentSolution, move)
				|| isAtLeastOneEmployeeFixed(currentSolution, move);
	}

	private boolean areFreeDaysForbidden(Solution currentSolution, DaysTuple move) {
		boolean isFromDayFree = spreadsheetReader.isFreeDay(move.fromDay());
		boolean isToDayFree = spreadsheetReader.isFreeDay(move.toDay());

		if (isFromDayFree == isToDayFree) {
			return false;
		}

		if (isToDayFree) {
			return true;
		}

		int employeeOnFromDay = currentSolution.getEmployeeForDay(move.fromDay());
		int employeeOnToDay = currentSolution.getEmployeeForDay(move.toDay());

		int freeDaysForEmployeeOnFrom = currentSolution.getNumberOfFreeDaysForEmployee(employeeOnFromDay);
		int freeDaysForEmployeeOnTo = currentSolution.getNumberOfFreeDaysForEmployee(employeeOnToDay);

		boolean canMoveFrom = (employeeOnFromDay == Config.MISSING_EMPLOYEE)
				|| (freeDaysForEmployeeOnFrom > spreadsheetReader.getDaysToWorkAtFreeDayForEmployee(employeeOnFromDay));

		boolean canMoveTo = (employeeOnToDay == Config.MISSING_EMPLOYEE)
				|| (freeDaysForEmployeeOnTo < spreadsheetReader.getDaysToWorkAtFreeDayForEmployee(employeeOnToDay));

		return !canMoveFrom && !canMoveTo;
	}

	private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, DaysTuple move) {
		int employee1 = currentSolution.getEmployeeForDay(move.fromDay());
		int employee2 = currentSolution.getEmployeeForDay(move.toDay());

		boolean employee1AvailableAtToDay = spreadsheetReader.isEmployeeAvailableOnDay(employee1, move.toDay());
		boolean employee2AvailableAtFromDay = spreadsheetReader.isEmployeeAvailableOnDay(employee2, move.fromDay());

		return !(employee1AvailableAtToDay && employee2AvailableAtFromDay);
	}

	private boolean isAtLeastOneEmployeeFixed(Solution currentSolution, DaysTuple move) {
		return isEmployeeFixedOnDay(currentSolution, move.fromDay())
				|| isEmployeeFixedOnDay(currentSolution, move.toDay());
	}

	private boolean isEmployeeFixedOnDay(Solution currentSolution, int day) {
		int fixedEmployee = spreadsheetReader.getEmployeeOnFixedDay(day);
		if (fixedEmployee == Config.MISSING_EMPLOYEE) {
			return false;
		}
		return currentSolution.getEmployeeForDay(day) == fixedEmployee;
	}
}
