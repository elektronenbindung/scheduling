package scheduling;

public class TabuSearch {
    private TabuList tabuList;
    private boolean stopped;
    private Controller controller;

    public TabuSearch(Controller controller) {
        tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
        stopped = false;
        this.controller = controller;
    }

    public Solution run(SpreadsheetReader input, Solution initialSolution) {
        Solution currentlyBestSolution = initialSolution;
        controller.println("Initial costs of solution: " + currentlyBestSolution.getCosts());
        Solution currenSolution = currentlyBestSolution.createCopy();

        for (int numberOfUnsuccessfulRetry = 0; numberOfUnsuccessfulRetry < Config.MAX_UNSUCCESSFUL_RETRYS_OF_TABU_SEARCH; numberOfUnsuccessfulRetry++) {
            int numberOfRetry = 0;
            int randomDay1 = 0;
            int randomDay2 = 0;

            while (numberOfRetry < Config.RETRYS_OF_FAILED_SOLUTION) {
                if (stopped) {
                    return currentlyBestSolution;
                }
                numberOfRetry++;
                randomDay1 = getRandomDay(input.getLengthOfMonth());
                randomDay2 = getRandomDay(input.getLengthOfMonth());
                Tuple tuple = new Tuple(randomDay1, randomDay2);

                if (randomDay1 == randomDay2
                        || tabuList.contains(tuple)
                        || input.getEmployeeOnFixedDay(randomDay1) != -1
                        || input.getEmployeeOnFixedDay(randomDay2) != -1
                        || input.isFreeDay(randomDay1) != input.isFreeDay(randomDay2)) {
                    continue;
                }
                int employee1 = currenSolution.getEmployeeForDay(randomDay1);
                int employee2 = currenSolution.getEmployeeForDay(randomDay2);
                if ((!input.getIsEmployeeAvailableOnDay(employee1, randomDay2))
                        || !input.getIsEmployeeAvailableOnDay(employee2, randomDay1)) {
                    continue;
                }
                currenSolution.exchangeEmployeesOnDays(randomDay1, randomDay2);
                tabuList.add(tuple);

                if (currenSolution.getCosts() < currentlyBestSolution.getCosts()) {
                    controller.println("Costs of solution: " + currenSolution.getCosts());
                    currentlyBestSolution = currenSolution;
                    numberOfUnsuccessfulRetry = 0;
                    currenSolution = currentlyBestSolution.createCopy();
                }
                break;
            }
        }
        return currentlyBestSolution;
    }

    private int getRandomDay(int lengthOfMonth) {
        return (int) (Math.random() * lengthOfMonth);
    }

    public void stop() {
        stopped = true;
    }

}
