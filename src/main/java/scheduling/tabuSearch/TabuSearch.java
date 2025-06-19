package scheduling.tabuSearch;

import java.util.Random;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;
import scheduling.spreadsheet.SpreadsheetReader;

public class TabuSearch {
	private final TabuList tabuList;
	private final SolutionList solutionList;
	private final Random random;
	private final ThreadsController threadsController;
	private final SpreadsheetReader spreadsheetReader;

	public TabuSearch(ThreadsController threadsController) {
		tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
		solutionList = new SolutionList(Config.LENGTH_OF_SOLUTION_LIST);
		random = new Random();
		this.threadsController = threadsController;
		spreadsheetReader = threadsController.getSpreadsheetReader();
	}

	public Solution run(Solution initialSolution) {
		Solution currentlyBestSolution = initialSolution;
		Solution currentSolution = currentlyBestSolution.createCopy();
		solutionList.add(currentlyBestSolution);

		for (int numberOfUnsuccessfulRetry = 0; numberOfUnsuccessfulRetry < Config.MAX_RETRIES_OF_TABU_SEARCH; numberOfUnsuccessfulRetry++) {
			int numberOfInvalidRetry = 0;

			while (numberOfInvalidRetry < Config.RETRIES_OF_INVALID_SOLUTION) {
				if (threadsController.isStopped() || currentlyBestSolution.getCosts() == Config.OPTIMAL_SOLUTION) {
					return currentlyBestSolution;
				}
				numberOfInvalidRetry++;
				DaysTuple daysTuple = new DaysTuple(getRandomDay(spreadsheetReader.getLengthOfMonth()),
						getRandomDay(spreadsheetReader.getLengthOfMonth()));

				if (isSwapOfShiftForbidden(currentSolution, daysTuple)) {
					if (numberOfInvalidRetry == Config.RETRIES_OF_INVALID_SOLUTION) {
						currentSolution = solutionList.getPreviousSolution();
						if (currentSolution == null) {
							return currentlyBestSolution;
						} else {
							currentSolution = currentSolution.createCopy();
							tabuList.reset();
						}
					}
					continue;
				}

				if (spreadsheetReader.isFreeDay(daysTuple.fromDay()) != spreadsheetReader
						.isFreeDay(daysTuple.toDay())) {
					currentSolution.exchangeFreeDayBetweenEmployees(daysTuple.fromDay(), daysTuple.toDay());
				}

				currentSolution.exchangeEmployeesOnDays(daysTuple.fromDay(), daysTuple.toDay());

				if (currentSolution.getCosts() < currentlyBestSolution.getCosts()) {
					solutionList.add(currentSolution);
					currentlyBestSolution = currentSolution;
					numberOfUnsuccessfulRetry = 0;
					currentSolution = currentlyBestSolution.createCopy();
				}
				break;
			}
		}
		return currentlyBestSolution;
	}

	private int getRandomDay(int lengthOfMonth) {
		return random.nextInt(lengthOfMonth);
	}

	private boolean isSwapOfShiftForbidden(Solution currentSolution, DaysTuple daysTuple) {
		return (areDaysForbidden(daysTuple, currentSolution)
				|| isAtLeastOneEmployeeUnavailable(currentSolution, daysTuple)
				|| isAtLeastOneEmployeeFixed(currentSolution, daysTuple));
	}

	private boolean areDaysForbidden(DaysTuple daysTuple, Solution currentSolution) {
		return daysTuple.fromDay() == daysTuple.toDay() || tabuList.contains(daysTuple)
				|| areFreeDaysForbidden(daysTuple, currentSolution);
	}

	private boolean areFreeDaysForbidden(DaysTuple daysTuple, Solution currentSolution) {
		if (spreadsheetReader.isFreeDay(daysTuple.fromDay()) == spreadsheetReader.isFreeDay(daysTuple.toDay())) {
			return false;
		}

		if (spreadsheetReader.isFreeDay(daysTuple.toDay())) {
			return true;
		}

		int employeeOnFromDay = currentSolution.getEmployeeForDay(daysTuple.fromDay());
		int employeeOnToDay = currentSolution.getEmployeeForDay(daysTuple.toDay());

		int numberOfFreeDaysForEmployeeOnFromDay = currentSolution.getNumberOfFreeDaysForEmployee(employeeOnFromDay);
		int numberOfFreeDaysForEmployeeOnToDay = currentSolution.getNumberOfFreeDaysForEmployee(employeeOnToDay);

		boolean canFreeDayBeMovedFrom = employeeOnFromDay == Config.MISSING_EMPLOYEE
				|| numberOfFreeDaysForEmployeeOnFromDay > spreadsheetReader
						.getDaysToWorkAtFreeDayForEmployee(employeeOnFromDay);

		boolean canFreeDayBeMovedTo = employeeOnToDay == Config.MISSING_EMPLOYEE
				|| numberOfFreeDaysForEmployeeOnToDay < spreadsheetReader
						.getDaysToWorkAtFreeDayForEmployee(employeeOnToDay);

		return (!canFreeDayBeMovedFrom) && (!canFreeDayBeMovedTo);
	}

	private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, DaysTuple daysTuple) {
		int employee1 = currentSolution.getEmployeeForDay(daysTuple.fromDay());
		int employee2 = currentSolution.getEmployeeForDay(daysTuple.toDay());

		return !(spreadsheetReader.getIsEmployeeAvailableOnDay(employee1, daysTuple.toDay())
				&& spreadsheetReader.getIsEmployeeAvailableOnDay(employee2, daysTuple.fromDay()));
	}

	private boolean isAtLeastOneEmployeeFixed(Solution currentSolution, DaysTuple daysTuple) {
		boolean isEmployeeFixedOnFromDay = spreadsheetReader
				.getEmployeeOnFixedDay(daysTuple.fromDay()) != Config.MISSING_EMPLOYEE
				&& currentSolution.getEmployeeForDay(daysTuple.fromDay()) == spreadsheetReader
						.getEmployeeOnFixedDay(daysTuple.fromDay());

		boolean isEmployeeFixedOnToDay = spreadsheetReader
				.getEmployeeOnFixedDay(daysTuple.toDay()) != Config.MISSING_EMPLOYEE
				&& currentSolution.getEmployeeForDay(daysTuple.toDay()) == spreadsheetReader
						.getEmployeeOnFixedDay(daysTuple.toDay());

		return isEmployeeFixedOnFromDay || isEmployeeFixedOnToDay;
	}
}
