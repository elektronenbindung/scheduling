package scheduling.tabuSearch;

import java.util.Objects;

public record DaysTuple(int fromDay, int toDay) {

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DaysTuple daysTuple = (DaysTuple) o;
		return (fromDay == daysTuple.fromDay && toDay == daysTuple.toDay) ||
				(fromDay == daysTuple.toDay && toDay == daysTuple.fromDay);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Math.min(fromDay, toDay), Math.max(fromDay, toDay));
	}
}
