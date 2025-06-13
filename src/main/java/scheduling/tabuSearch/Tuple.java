package scheduling.tabuSearch;

public class Tuple {
	private final int x, y;

	@SuppressWarnings("SuspiciousNameCombination")
	public Tuple(int x, int y) {
		if (y < x) {
			this.x = y;
			this.y = x;
		} else {
			this.x = x;
			this.y = y;
		}
	}

	public boolean equals(Tuple tuple) {
		return x == tuple.x && y == tuple.y;
	}
}
