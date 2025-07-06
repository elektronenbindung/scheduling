package scheduling.matching;

public class Shift implements Vertex {

	private final int employee;

	public Shift(int employee) {
		this.employee = employee;
	}

	public int getEmployee() {
		return employee;
	}
}
