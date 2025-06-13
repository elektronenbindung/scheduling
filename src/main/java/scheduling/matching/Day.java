package scheduling.matching;

public class Day implements Vertex {

  private final int dayNumber;

  public Day(int dayNumber) {
    this.dayNumber = dayNumber;
  }

  public int getDayNumber() {
    return this.dayNumber;
  }
}
