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

                if (!isSwapOfShiftAllowed(currenSolution, randomDay1, randomDay2)) {
                    continue;
                }
                currenSolution.exchangeEmployeesOnDays(randomDay1, randomDay2);

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

    private boolean isSwapOfShiftAllowed(Solution currenSolution, int day1, int day2) {
        Tuple tuple = new Tuple(day1, day2);

        if (areDaysForbidden(day1, day2, tuple) || isAtLeastOneEmployeeUnavailable(currenSolution, day1, day2)
                || isAtLeastOnEmployeeFixed(currenSolution, day1, day2)) {
            return false;
        }

        tabuList.add(tuple);

        return true;
    }

    private boolean areDaysForbidden(int day1, int day2, Tuple tuple) {
        boolean areDaysForbidden = day1 == day2
                || tabuList.contains(tuple)
                || input.isFreeDay(day1) != input.isFreeDay(day2);
        return areDaysForbidden;
    }

    private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, int day1, int day2) {
        int employee1 = currentSolution.getEmployeeForDay(day1);
        int employee2 = currentSolution.getEmployeeForDay(day2);

        return !(input.getIsEmployeeAvailableOnDay(employee1, day2)
                && input.getIsEmployeeAvailableOnDay(employee2, day1));
    }

    private boolean isAtLeastOnEmployeeFixed(Solution currenSolution, int day1, int day2) {
        boolean isEmployeeFixedOnDay1 = input.getEmployeeOnFixedDay(day1) != -1
                && currenSolution.getEmployeeForDay(day1) == input.getEmployeeOnFixedDay(day1);

        boolean isEmployeeFixedOnDay2 = input.getEmployeeOnFixedDay(day2) != -1
                && currenSolution.getEmployeeForDay(day2) == input.getEmployeeOnFixedDay(day2);

        return isEmployeeFixedOnDay1 || isEmployeeFixedOnDay2;
    }

    private int getRandomDay(int lengthOfMonth) {
        return random.nextInt(lengthOfMonth);
    }

}
