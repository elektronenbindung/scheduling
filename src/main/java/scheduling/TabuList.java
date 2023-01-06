package scheduling;

import java.util.Arrays;

public class TabuList {
    private int nextElement;
    private Tuple[] tabuList;

    public TabuList(int length) {
        tabuList = new Tuple[length];
        nextElement = 0;
    }

    public boolean contains(Tuple tuple) {
        return Arrays.stream(tabuList).anyMatch(t -> t != null && t.equals(tuple));
    }

    public void add(Tuple tuple) {
        tabuList[nextElement] = tuple;
        nextElement = (nextElement + 1) % tabuList.length;
    }
}
