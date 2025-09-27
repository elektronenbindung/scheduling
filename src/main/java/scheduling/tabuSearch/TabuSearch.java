package scheduling.tabuSearch;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;
import scheduling.spreadsheet.SpreadsheetReader;

import java.util.concurrent.ThreadLocalRandom;

public class TabuSearch {

	private final TabuList tabuList;
	private final SolutionList solutionList;
	private final ThreadsController threadsController;
	private final SpreadsheetReader spreadsheetReader;
	private final MoveValidator moveValidator;

	public TabuSearch(ThreadsController threadsController) {
		this.tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
		this.solutionList = new SolutionList(Config.LENGTH_OF_SOLUTION_LIST);
		this.threadsController = threadsController;
		this.spreadsheetReader = threadsController.getSpreadsheetReader();
		this.moveValidator = new MoveValidator(spreadsheetReader);
	}

	public Solution run(Solution initialSolution) {
		Solution bestSolution = initialSolution;
		Solution currentSolution = bestSolution.createCopy();
		solutionList.add(bestSolution);

		for (int iterationsWithoutImprovement = 0; iterationsWithoutImprovement < Config.MAX_RETRIES_OF_TABU_SEARCH; iterationsWithoutImprovement++) {
			if (isSearchFinished(bestSolution)) {
				return bestSolution;
			}

			Move bestMove = findBestNeighborMove(currentSolution, bestSolution);

			if (bestMove == null) {
				currentSolution = handleSearchStagnation();
				if (currentSolution == null) {
					return bestSolution;
				}
				continue;
			}

			applyMove(currentSolution, bestMove);

			if (currentSolution.getCosts() < bestSolution.getCosts()) {
				bestSolution = currentSolution.createCopy();
				solutionList.add(bestSolution);
				iterationsWithoutImprovement = 0;
			}
		}
		return bestSolution;
	}

	private Move findBestNeighborMove(Solution currentSolution, Solution bestSolution) {
		Move bestMove = null;
		double bestMoveCost = Double.MAX_VALUE;
		Solution copiedSolution = currentSolution.createCopy();

		for (int i = 0; i < Config.TABU_SEARCH_NEIGHBORHOOD_SAMPLE_SIZE; i++) {
			Move potentialMove = generateRandomMove();

			if (moveValidator.isMoveForbidden(currentSolution, potentialMove)) {
				continue;
			}

			copiedSolution.exchangeEmployeesOnDays(potentialMove.fromDay(), potentialMove.toDay());
			double neighborCost = copiedSolution.getCosts();
			copiedSolution.exchangeEmployeesOnDays(potentialMove.fromDay(), potentialMove.toDay());

			boolean isTabu = tabuList.contains(potentialMove);
			if (isTabu && neighborCost >= bestSolution.getCosts()) {
				continue;
			}

			if (neighborCost < bestMoveCost) {
				bestMoveCost = neighborCost;
				bestMove = potentialMove;
				bestMove.setCosts(neighborCost);
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
		solution.setSolutionCosts(move.getCosts());
	}

	private Solution handleSearchStagnation() {
		Solution previousSolution = solutionList.getPreviousSolution();
		if (previousSolution == null) {
			return null;
		}
		tabuList.reset();
		return previousSolution.createCopy();
	}

	private boolean isSearchFinished(Solution bestSolution) {
		return threadsController.isStopped() || bestSolution.getCosts() == Config.OPTIMAL_SOLUTION;
	}

	private Move generateRandomMove() {
		int lengthOfMonth = spreadsheetReader.getLengthOfMonth();
		int fromDay;
		int toDay;
		do {
			fromDay = ThreadLocalRandom.current().nextInt(lengthOfMonth);
			toDay = ThreadLocalRandom.current().nextInt(lengthOfMonth);
		} while (fromDay == toDay);

		return new Move(fromDay, toDay);
	}
}
