package scheduling.tabuSearch;

import java.util.Arrays;

public class TabuList {
	private int nextElement;
	private final Tuple[] tabuList;

	public TabuList(int length) {
		tabuList = new Tuple[length];
		nextElement = 0;
	}

	public void add(int day1, int day2) {
		tabuList[nextElement] = new Tuple(day1, day2);
		nextElement = (nextElement + 1) % tabuList.length;
	}

	public boolean contains(int day1, int day2) {
		Tuple tuple = new Tuple(day1, day2);
		return Arrays.stream(tabuList).anyMatch(t -> t != null && t.equals(tuple));
	}

	public void reset() {
		Arrays.fill(tabuList, null);
		nextElement = 0;
	}
}
