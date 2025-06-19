package scheduling.tabuSearch;

import java.util.Arrays;

public class TabuList {
	private int nextElement;
	private final DaysTuple[] tabuList;

	public TabuList(int length) {
		tabuList = new DaysTuple[length];
		nextElement = 0;
	}

	public void add(DaysTuple daysTuple) {
		tabuList[nextElement] = daysTuple;
		nextElement = (nextElement + 1) % tabuList.length;
	}

	public boolean contains(DaysTuple daysTuple) {
		return Arrays.stream(tabuList).anyMatch(t -> t != null && t.equals(daysTuple));
	}

	public void reset() {
		Arrays.fill(tabuList, null);
		nextElement = 0;
	}
}
