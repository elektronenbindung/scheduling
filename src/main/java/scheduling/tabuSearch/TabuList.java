package scheduling.tabuSearch;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TabuList {
	private final int capacity;
	private final Queue<DaysTuple> moveQueue;
	private final Set<DaysTuple> moveSet;

	public TabuList(int length) {
		this.capacity = length;
		this.moveQueue = new ArrayDeque<>(length);
		this.moveSet = new HashSet<>(length);
	}

	public void add(DaysTuple daysTuple) {
		if (capacity == 0) {
			return;
		}

		if (moveQueue.size() >= capacity) {
			DaysTuple oldestMove = moveQueue.poll();
			moveSet.remove(oldestMove);
		}

		moveQueue.add(daysTuple);
		moveSet.add(daysTuple);
	}

	public boolean contains(DaysTuple daysTuple) {
		return moveSet.contains(daysTuple);
	}

	public void reset() {
		moveQueue.clear();
		moveSet.clear();
	}
}
