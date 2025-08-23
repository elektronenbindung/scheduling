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
			if (isSearchFinished(bestSolution)) {
				return bestSolution;
			}

			Move bestMove = findBestNeighborMove(currentSolution, bestSolution);

			if (bestMove == null) {
				currentSolution = solutionList.getPreviousSolution();
				if (currentSolution == null) {
					return bestSolution;
				}
				currentSolution = currentSolution.createCopy();
				tabuList.reset();
				continue;
			}

			applyMove(currentSolution, bestMove);

			if (currentSolution.getCosts() < bestSolution.getCosts()) {
				bestSolution = currentSolution.createCopy();
				solutionList.add(bestSolution);
				retriesWithoutImprovement = 0;
			}
		}
		return bestSolution;
	}

	private Move findBestNeighborMove(Solution currentSolution, Solution bestSolution) {
		Move bestMove = null;
		double bestMoveCost = Double.MAX_VALUE;

		for (int i = 0; i < Config.TABU_SEARCH_NEIGHBORHOOD_SAMPLE_SIZE; i++) {
			Move potentialMove = generateRandomMove();

			if (isSwapOfShiftForbidden(currentSolution, potentialMove)) {
				continue;
			}

			Solution neighborSolution = currentSolution.createCopy();
			neighborSolution.exchangeEmployeesOnDays(potentialMove.fromDay(), potentialMove.toDay());

			double neighborCost = neighborSolution.getCosts();

			boolean isAspirationCriterionMet = tabuList.contains(potentialMove)
					&& neighborCost < bestSolution.getCosts();

			if (!isSwapOfShiftForbidden(currentSolution, potentialMove) || isAspirationCriterionMet) {
				if (neighborCost < bestMoveCost) {
					bestMoveCost = neighborCost;
					bestMove = potentialMove;
				}
			}
		}
		return bestMove;
	}

	private void applyMove(Solution solution, Move move) {
		tabuList.add(move);

		if (spreadsheetReader.isFreeDay(move.fromDay()) != spreadsheetReader.isFreeDay(move.toDay())) {
			solution.exchangeFreeDayBetweenEmployees(move.fromDay(), move.toDay());
		}

		solution.exchangeEmployeesOnDays(move.fromDay(), move.toDay());
	}

	private boolean isSearchFinished(Solution bestSolution) {
		return threadsController.isStopped() || bestSolution.getCosts() == Config.OPTIMAL_SOLUTION;
	}

	private Move generateRandomMove() {
		int lengthOfMonth = spreadsheetReader.getLengthOfMonth();
		int fromDay;
		int toDay;
		do {
			fromDay = random.nextInt(lengthOfMonth);
			toDay = random.nextInt(lengthOfMonth);
		} while (fromDay == toDay);

		return new Move(fromDay, toDay);
	}

	private boolean isSwapOfShiftForbidden(Solution currentSolution, Move move) {
		if (move.fromDay() == move.toDay()) {
			return true;
		}

		return areFreeDaysForbidden(currentSolution, move)
				|| isAtLeastOneEmployeeUnavailable(currentSolution, move)
				|| isAtLeastOneEmployeeFixed(currentSolution, move);
	}

	private boolean areFreeDaysForbidden(Solution currentSolution, Move move) {
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

	private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, Move move) {
		int employee1 = currentSolution.getEmployeeForDay(move.fromDay());
		int employee2 = currentSolution.getEmployeeForDay(move.toDay());

		boolean employee1AvailableAtToDay = spreadsheetReader.isEmployeeAvailableOnDay(employee1, move.toDay());
		boolean employee2AvailableAtFromDay = spreadsheetReader.isEmployeeAvailableOnDay(employee2, move.fromDay());

		return !(employee1AvailableAtToDay && employee2AvailableAtFromDay);
	}

	private boolean isAtLeastOneEmployeeFixed(Solution currentSolution, Move move) {
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
