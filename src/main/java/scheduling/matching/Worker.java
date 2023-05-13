package scheduling.matching;

public class Worker implements Vertex {

    private final int employeeNumber;

    public Worker(int employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public int getEmployeeNumber() {
        return employeeNumber;
    }

}
