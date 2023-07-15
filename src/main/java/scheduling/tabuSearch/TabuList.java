package scheduling.tabuSearch;

import java.util.Arrays;

public class TabuList {
    private int nextElement;
    private Tuple[] tabuList;

    public TabuList(int length) {
        tabuList = new Tuple[length];
        nextElement = 0;
    }

    public void add(Tuple tuple) {
        tabuList[nextElement] = tuple;
        nextElement = (nextElement + 1) % tabuList.length;
    }

    public boolean contains(Tuple tuple) {
        return Arrays.stream(tabuList).anyMatch(t -> t != null && t.equals(tuple));
    }

    public void reset() {
        Arrays.fill(tabuList, null);
        nextElement = 0;
    }
}
