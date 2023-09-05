package scheduling.tabuSearch;

import java.util.Random;

import scheduling.common.Config;
import scheduling.common.Controller;
import scheduling.common.Solution;
import scheduling.spreadsheet.SpreadsheetReader;

public class TabuSearch {
    private TabuList tabuList;
    private SolutionList solutionList;
    private boolean stopped;
    private Controller controller;
    private Random random;
    private SpreadsheetReader input;

    public TabuSearch(Controller controller) {
        tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
        solutionList = new SolutionList(Config.LENGTH_OF_SOLUTION_LIST);
        stopped = false;
        this.controller = controller;
        random = new Random();
        input = null;
    }

    public Solution run(SpreadsheetReader reader, Solution initialSolution) {
        input = reader;
        Solution currentlyBestSolution = initialSolution;
        controller.println("Initial costs of solution: " + currentlyBestSolution.getCosts());
        Solution currentSolution = currentlyBestSolution.createCopy();
        solutionList.add(currentlyBestSolution);

        for (int numberOfUnsuccessfulRetry = 0; numberOfUnsuccessfulRetry < Config.MAX_RETRIES_OF_TABU_SEARCH; numberOfUnsuccessfulRetry++) {
            int numberOfInvalidRetry = 0;
            int randomDay1 = 0;
            int randomDay2 = 0;

            while (numberOfInvalidRetry < Config.RETRIES_OF_INVALID_SOLUTION) {
                if (stopped || currentlyBestSolution.getCosts() == 0) {
                    return currentlyBestSolution;
                }
                numberOfInvalidRetry++;
                randomDay1 = getRandomDay(input.getLengthOfMonth());
                randomDay2 = getRandomDay(input.getLengthOfMonth());

                if (!isSwapOfShiftAllowed(currentSolution, randomDay1, randomDay2)) {
                    if (numberOfInvalidRetry == Config.RETRIES_OF_INVALID_SOLUTION) {
                        currentSolution = solutionList.getPreviouSolution();
                        if (currentSolution == null) {
                            return currentlyBestSolution;
                        } else {
                            currentSolution = currentSolution.createCopy();
                            tabuList.reset();
                        }
                    }
                    continue;
                }

                if (input.isFreeDay(randomDay1) != input.isFreeDay(randomDay2)) {
                    currentSolution.exchangeFreeDayBetweenEmployees(randomDay1, randomDay2);
                }

                currentSolution.exchangeEmployeesOnDays(randomDay1, randomDay2);

                if (currentSolution.getCosts() < currentlyBestSolution.getCosts()) {
                    controller.println("Costs of solution: " + currentSolution.getCosts());
                    solutionList.add(currentSolution);
                    currentlyBestSolution = currentSolution;
                    numberOfUnsuccessfulRetry = 0;
                    currentSolution = currentlyBestSolution.createCopy();
                }
                break;
            }
        }
        return currentlyBestSolution;
    }

    public void stop() {
        stopped = true;
    }

    private int getRandomDay(int lengthOfMonth) {
        return random.nextInt(lengthOfMonth);
    }

    private boolean isSwapOfShiftAllowed(Solution currenSolution, int day1, int day2) {
        Tuple tuple = new Tuple(day1, day2);

        if (areDaysForbidden(day1, day2, tuple, currenSolution)
                || isAtLeastOneEmployeeUnavailable(currenSolution, day1, day2)
                || isAtLeastOneEmployeeFixed(currenSolution, day1, day2)) {
            return false;
        }

        tabuList.add(tuple);

        return true;
    }

    private boolean areDaysForbidden(int day1, int day2, Tuple tuple, Solution currentSolution) {

        boolean areDaysForbidden = day1 == day2
                || tabuList.contains(tuple)
                || areFreeDaysForbidden(day1, day2, currentSolution);
        return areDaysForbidden;
    }

    private boolean areFreeDaysForbidden(int fromDay, int toDay, Solution currentSolution) {
        if (input.isFreeDay(fromDay) == input.isFreeDay(toDay)) {
            return false;
        }

        if (input.isFreeDay(toDay)) {
            return true;
        }

        int employeeOnFromDay = currentSolution.getEmployeeForDay(fromDay);
        int employeeOnToDay = currentSolution.getEmployeeForDay(toDay);

        int numberOfFreeDaysForEmployeeOnFromDay = currentSolution.getNumberOfFreeDaysForEmployee(employeeOnFromDay);
        int numberOfFreeDaysForEmployeeOnToDay = currentSolution.getNumberOfFreeDaysForEmployee(employeeOnToDay);

        boolean canFreeDayBeMovedFrom = numberOfFreeDaysForEmployeeOnFromDay > input
                .getDaysToWorkAtFreeDayForEmployee(employeeOnFromDay);
        boolean canFreeDayBeMovedTo = numberOfFreeDaysForEmployeeOnToDay < input
                .getDaysToWorkAtFreeDayForEmployee(employeeOnToDay);

        return (!canFreeDayBeMovedFrom) && (!canFreeDayBeMovedTo);
    }

    private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, int day1, int day2) {
        int employee1 = currentSolution.getEmployeeForDay(day1);
        int employee2 = currentSolution.getEmployeeForDay(day2);

        return !(input.getIsEmployeeAvailableOnDay(employee1, day2)
                && input.getIsEmployeeAvailableOnDay(employee2, day1));
    }

    private boolean isAtLeastOneEmployeeFixed(Solution currenSolution, int day1, int day2) {
        boolean isEmployeeFixedOnDay1 = input.getEmployeeOnFixedDay(day1) != -1
                && currenSolution.getEmployeeForDay(day1) == input.getEmployeeOnFixedDay(day1);

        boolean isEmployeeFixedOnDay2 = input.getEmployeeOnFixedDay(day2) != -1
                && currenSolution.getEmployeeForDay(day2) == input.getEmployeeOnFixedDay(day2);

        return isEmployeeFixedOnDay1 || isEmployeeFixedOnDay2;
    }

}
