package scheduling.tabuSearch;

import java.util.Arrays;
import java.util.Random;

import scheduling.common.Config;

public class TabuList {
    private int nextElement;
    private Tuple[] tabuList;
    private int currentLength;
    private Random random;
    private int boundOfLengthInterval;

    public TabuList() {
        tabuList = new Tuple[Config.MAX_LENGTH_OF_TABU_LIST];
        nextElement = 0;
        currentLength = -1;
        random = new Random();
        boundOfLengthInterval = Config.MAX_LENGTH_OF_TABU_LIST - Config.MIN_LENGTH_OF_TABU_LIST;
    }

    public boolean contains(Tuple tuple) {
        for (int numberOfTuple = 0; numberOfTuple < currentLength; numberOfTuple++) {
            Tuple currentTuple = tabuList[numberOfTuple];

            if (currentTuple == null) {
                return false;
            }
            if (currentTuple.equals(tuple)) {
                return true;
            }
        }
        return false;
    }

    public void add(Tuple tuple) {
        tabuList[nextElement] = tuple;
        nextElement = (nextElement + 1) % currentLength;
    }

    public void resize() {
        int newLength = Config.MIN_LENGTH_OF_TABU_LIST + random.nextInt(boundOfLengthInterval);

        if (newLength < currentLength) {
            for (int element = newLength - 1; element < currentLength; element++) {
                tabuList[element] = null;

                if (nextElement >= newLength) {
                    nextElement = 0;
                }
            }
        }
        currentLength = newLength;
    }

    public void reset() {
        Arrays.fill(tabuList, null);
        nextElement = 0;
    }
}
