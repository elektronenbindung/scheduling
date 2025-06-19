package scheduling.tabuSearch;

public class DaysTuple {
	private final int fromDay, toDay;

	public DaysTuple(int fromDay, int toDay) {
		this.fromDay = fromDay;
		this.toDay = toDay;
	}

	public int fromDay() {
		return fromDay;
	}

	public int toDay() {
		return toDay;
	}

	public boolean equals(DaysTuple tuple) {
		return (fromDay == tuple.fromDay && toDay == tuple.toDay) || (fromDay == tuple.toDay && toDay == tuple.fromDay);
	}
}
