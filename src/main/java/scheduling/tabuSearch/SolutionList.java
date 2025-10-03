package scheduling.tabuSearch;

import scheduling.common.Solution;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

public class SolutionList {

	private final Deque<Solution> solutions;
	private final int capacity;

	public SolutionList(int length) {
		this.solutions = new ArrayDeque<>(length);
		this.capacity = length;
	}

	public void add(Solution solution) {
		if (capacity == 0) {
			return;
		}
		if (solutions.size() >= capacity) {
			solutions.removeFirst();
		}
		solutions.addLast(solution);
	}

	public Optional<Solution> getPreviousSolution() {
		Iterator<Solution> descendingIterator = solutions.descendingIterator();

		while (descendingIterator.hasNext()) {
			Solution solution = descendingIterator.next();
			if (solution != null && solution.canBeRetried()) {
				return Optional.of(solution);
			}
		}
		return Optional.empty();
	}
}