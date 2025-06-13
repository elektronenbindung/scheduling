package scheduling.tabuSearch;

public class Tuple {
  private int x, y;

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
