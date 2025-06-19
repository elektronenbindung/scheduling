package scheduling.tabuSearch;

public class DaysTuple {
	private final int fromDay, toDay;

	public DaysTuple(int day1, int day2) {
		this.fromDay = day1;
		this.toDay = day2;
	}

	public int getFromDay() {
		return fromDay;
	}

	public int getToDay() {
		return toDay;
	}

	public boolean equals(DaysTuple tuple) {
		return (fromDay == tuple.fromDay && toDay == tuple.toDay) || (fromDay == tuple.toDay && toDay == tuple.fromDay);
	}
}
