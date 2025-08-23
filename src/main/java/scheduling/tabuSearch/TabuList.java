package scheduling.tabuSearch;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TabuList {
	private final int capacity;
	private final Queue<Move> moveQueue;
	private final Set<Move> moveSet;

	private static final float LOAD_FACTOR = 0.5f;

	public TabuList(int length) {
		int capacityOfSet = (int) (Math.ceil(length / LOAD_FACTOR)) + 1;
		this.capacity = length;
		this.moveQueue = new ArrayDeque<>(length);
		this.moveSet = new HashSet<>(capacityOfSet, LOAD_FACTOR);
	}

	public void add(Move move) {
		if (capacity == 0) {
			return;
		}

		if (moveQueue.size() >= capacity) {
			Move oldestMove = moveQueue.poll();
			moveSet.remove(oldestMove);
		}

		moveQueue.add(move);
		moveSet.add(move);
	}

	public boolean contains(Move move) {
		return moveSet.contains(move);
	}

	public void reset() {
		moveQueue.clear();
		moveSet.clear();
	}
}
