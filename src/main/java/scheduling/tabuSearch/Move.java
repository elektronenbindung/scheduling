package scheduling.tabuSearch;

import java.util.Objects;

import static scheduling.common.Config.UNKNOWN_SOLUTION_COSTS;

public final class Move {
	private final int fromDay;
	private final int toDay;
	private double solutionCosts;

	public Move(int fromDay, int toDay) {
		this.fromDay = fromDay;
		this.toDay = toDay;
		this.solutionCosts = UNKNOWN_SOLUTION_COSTS;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Move move = (Move) o;
		return (fromDay == move.fromDay && toDay == move.toDay) || (fromDay == move.toDay && toDay == move.fromDay);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Math.min(fromDay, toDay), Math.max(fromDay, toDay));
	}

	public int fromDay() {
		return fromDay;
	}

	public int toDay() {
		return toDay;
	}

	public void setSolutionCosts(double solutionCosts) {
		this.solutionCosts = solutionCosts;
	}

	public double getSolutionCosts() {
		return solutionCosts;
	}

}
