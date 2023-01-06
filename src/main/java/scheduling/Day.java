package scheduling;

public class Day implements Vertex {

    private int dayNumber;

    public Day(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getDayNumber() {
        return this.dayNumber;
    }
}
