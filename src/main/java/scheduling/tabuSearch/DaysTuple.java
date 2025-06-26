package scheduling.tabuSearch;

public record DaysTuple(int fromDay, int toDay) {

	public boolean equals(DaysTuple tuple) {
		return (fromDay == tuple.fromDay && toDay == tuple.toDay) || (fromDay == tuple.toDay && toDay == tuple.fromDay);
	}
}
