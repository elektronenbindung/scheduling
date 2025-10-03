package scheduling.tabuSearch;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;
import scheduling.spreadsheet.SpreadsheetReader;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class TabuSearch {

	private record EvaluatedMove(Optional<Move> move, double solutionCosts) {}

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

	public Solution run(final Solution initialSolution) {
		Solution bestSolution = initialSolution;
		Solution currentSolution = bestSolution.createCopy();
		solutionList.add(bestSolution);
		int iterationsWithoutImprovement = 0;

		while (iterationsWithoutImprovement < Config.MAX_RETRIES_OF_TABU_SEARCH) {
			if (isSearchFinished(bestSolution)) {
				return bestSolution;
			}

			EvaluatedMove bestEvaluatedMove = findBestNeighborMove(currentSolution, bestSolution);

			if (bestEvaluatedMove.move().isEmpty()) {
				Optional<Solution> stagnationSolution = handleSearchStagnation();
				if (stagnationSolution.isEmpty()) return bestSolution;

				currentSolution = stagnationSolution.get();
				iterationsWithoutImprovement++;
				continue;
			}

			applyMove(currentSolution, bestEvaluatedMove);

			if (currentSolution.getCosts() < bestSolution.getCosts()) {
				bestSolution = currentSolution.createCopy();
				solutionList.add(bestSolution);
				iterationsWithoutImprovement = 0;
			} else {
				iterationsWithoutImprovement++;
			}
		}
		return bestSolution;
	}

	private EvaluatedMove findBestNeighborMove(Solution currentSolution, Solution bestSolution) {
		Move bestMove = null;
		final double originalCost = currentSolution.getCosts();
		double bestMoveCost = Double.MAX_VALUE;

		for (int i = 0; i < Config.TABU_SEARCH_NEIGHBORHOOD_SAMPLE_SIZE; i++) {
			Move potentialMove = generateRandomMove();

			if (moveValidator.isMoveForbidden(currentSolution, potentialMove)) continue;

			final double neighborCost = getNeighborCost(currentSolution, potentialMove);

			final boolean isTabu = tabuList.contains(potentialMove);
			if (isTabu && neighborCost >= bestSolution.getCosts()) continue;

			if (neighborCost < bestMoveCost) {
				bestMoveCost = neighborCost;
				bestMove = potentialMove;
			}
		}
		currentSolution.setSolutionCosts(originalCost);
		return new EvaluatedMove(Optional.ofNullable(bestMove), bestMoveCost);
	}

	private double getNeighborCost(Solution currentSolution, Move potentialMove) {
		currentSolution.exchangeEmployeesOnDays(potentialMove.fromDay(), potentialMove.toDay());
		final double neighborCost = currentSolution.getCosts();
		currentSolution.exchangeEmployeesOnDays(potentialMove.fromDay(), potentialMove.toDay());
		return neighborCost;
	}

	private void applyMove(Solution solution, EvaluatedMove evaluatedMove) {
		if(evaluatedMove.move().isPresent()) {
			Move move = evaluatedMove.move().get();
			tabuList.add(move);

			if (spreadsheetReader.isFreeDay(move.fromDay()) != spreadsheetReader.isFreeDay(move.toDay())) {
				solution.exchangeFreeDayBetweenEmployees(move.fromDay(), move.toDay());
			}

			solution.exchangeEmployeesOnDays(move.fromDay(), move.toDay());
			solution.setSolutionCosts(evaluatedMove.solutionCosts());
		}
	}

	private Optional<Solution> handleSearchStagnation() {
		return solutionList.getPreviousSolution()
				.map(previousSolution -> {
					tabuList.reset();
					return previousSolution.createCopy();
				});
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
