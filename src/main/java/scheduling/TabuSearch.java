package scheduling;

import java.util.Random;

public class TabuSearch {
    private TabuList tabuList;
    private boolean stopped;
    private Controller controller;
    private Random random;
    private SpreadsheetReader input;

    public TabuSearch(Controller controller) {
        tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
        stopped = false;
        this.controller = controller;
        random = new Random();
        input = null;
    }

    public Solution run(SpreadsheetReader reader, Solution initialSolution) {
        input = reader;
        Solution currentlyBestSolution = initialSolution;
        controller.println("Initial costs of solution: " + currentlyBestSolution.getCosts());
        Solution currenSolution = currentlyBestSolution.createCopy();

        for (int numberOfUnsuccessfulRetry = 0; numberOfUnsuccessfulRetry < Config.MAX_UNSUCCESSFUL_RETRYS_OF_TABU_SEARCH; numberOfUnsuccessfulRetry++) {
            int numberOfRetry = 0;
            int randomDay1 = 0;
            int randomDay2 = 0;

            while (numberOfRetry < Config.RETRYS_OF_FAILED_SOLUTION) {
                if (stopped || currentlyBestSolution.getCosts() == 0) {
                    return currentlyBestSolution;
                }
                numberOfRetry++;
                randomDay1 = getRandomDay(input.getLengthOfMonth());
                randomDay2 = getRandomDay(input.getLengthOfMonth());
                Tuple tuple = new Tuple(randomDay1, randomDay2);

                if (isExchangeOFShiftAllowedOnDay(currenSolution, randomDay1, randomDay2, tuple)) {
                    continue;
                }
                int employee1 = currenSolution.getEmployeeForDay(randomDay1);
                int employee2 = currenSolution.getEmployeeForDay(randomDay2);

                boolean areEmployeesAvailable = input.getIsEmployeeAvailableOnDay(employee1, randomDay2)
                        && input.getIsEmployeeAvailableOnDay(employee2, randomDay1);

                if (!areEmployeesAvailable) {
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

    public void stop() {
        stopped = true;
    }

    private boolean isExchangeOFShiftAllowedOnDay(Solution currenSolution, int day1, int day2, Tuple tuple) {
        boolean isEmployeeFixedOnDay1 = input.getEmployeeOnFixedDay(day1) != -1
                && currenSolution.getEmployeeForDay(day1) == input.getEmployeeOnFixedDay(day1);

        boolean isEmployeeFixedOnDay2 = input.getEmployeeOnFixedDay(day2) != -1
                && currenSolution.getEmployeeForDay(day2) == input.getEmployeeOnFixedDay(day2);

        return day1 == day2
                || tabuList.contains(tuple)
                || isEmployeeFixedOnDay1
                || isEmployeeFixedOnDay2
                || input.isFreeDay(day1) != input.isFreeDay(day2);
    }

    private int getRandomDay(int lengthOfMonth) {
        return random.nextInt(lengthOfMonth);
    }

}
