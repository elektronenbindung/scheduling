package scheduling.tabuSearch;

import scheduling.common.Solution;

public class SolutionList {
	private final Solution[] solutionList;
	private int nextElement;

	public SolutionList(int length) {
		solutionList = new Solution[length];
		nextElement = 0;
	}

	public void add(Solution solution) {
		solutionList[nextElement] = solution;
		nextElement = (nextElement + 1) % solutionList.length;
	}

	public Solution getPreviousSolution() {
		int currentSolution = getNextPointer(nextElement);

		while (currentSolution != nextElement && solutionList[currentSolution] != null) {
			if (solutionList[currentSolution].canBeRetried()) {
				return solutionList[currentSolution];
			}
			currentSolution = getNextPointer(currentSolution);
		}
		return null;
	}

	private int getNextPointer(int pointer) {
		return ((pointer - 1) % solutionList.length + solutionList.length) % solutionList.length;
	}
}
