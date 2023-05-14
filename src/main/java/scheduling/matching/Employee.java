package scheduling.matching;

public class Employee implements Vertex {

    private final int employeeNumber;

    public Employee(int employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public int getEmployeeNumber() {
        return employeeNumber;
    }

}
