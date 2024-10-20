package scheduling.tabuSearch;

import java.util.Random;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.spreadsheet.SpreadsheetReader;

public class TabuSearch {
    private TabuList tabuList;
    private SolutionList solutionList;
    private boolean stopped;
    private Random random;
    private SpreadsheetReader input;

    public TabuSearch() {
        tabuList = new TabuList(Config.LENGTH_OF_TABU_LIST);
        solutionList = new SolutionList(Config.LENGTH_OF_SOLUTION_LIST);
        stopped = false;
        random = new Random();
        input = null;
    }

    public Solution run(SpreadsheetReader reader, Solution initialSolution) {
        input = reader;
        Solution currentlyBestSolution = initialSolution;
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
                        currentSolution = solutionList.getPreviousSolution();
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

    private boolean isSwapOfShiftAllowed(Solution currentSolution, int day1, int day2) {
        Tuple tuple = new Tuple(day1, day2);

        if (areDaysForbidden(day1, day2, tuple, currentSolution)
                || isAtLeastOneEmployeeUnavailable(currentSolution, day1, day2)
                || isAtLeastOneEmployeeFixed(currentSolution, day1, day2)) {
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

        boolean canFreeDayBeMovedFrom = employeeOnFromDay == Config.MISSING_EMPLOYEE
                || numberOfFreeDaysForEmployeeOnFromDay > input
                        .getDaysToWorkAtFreeDayForEmployee(employeeOnFromDay);
        boolean canFreeDayBeMovedTo = employeeOnToDay == Config.MISSING_EMPLOYEE
                || numberOfFreeDaysForEmployeeOnToDay < input
                        .getDaysToWorkAtFreeDayForEmployee(employeeOnToDay);

        return (!canFreeDayBeMovedFrom) && (!canFreeDayBeMovedTo);
    }

    private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, int day1, int day2) {
        int employee1 = currentSolution.getEmployeeForDay(day1);
        int employee2 = currentSolution.getEmployeeForDay(day2);

        return !(input.getIsEmployeeAvailableOnDay(employee1, day2)
                && input.getIsEmployeeAvailableOnDay(employee2, day1));
    }

    private boolean isAtLeastOneEmployeeFixed(Solution currentSolution, int day1, int day2) {
        boolean isEmployeeFixedOnDay1 = input.getEmployeeOnFixedDay(day1) != Config.MISSING_EMPLOYEE
                && currentSolution.getEmployeeForDay(day1) == input.getEmployeeOnFixedDay(day1);

        boolean isEmployeeFixedOnDay2 = input.getEmployeeOnFixedDay(day2) != Config.MISSING_EMPLOYEE
                && currentSolution.getEmployeeForDay(day2) == input.getEmployeeOnFixedDay(day2);

        return isEmployeeFixedOnDay1 || isEmployeeFixedOnDay2;
    }

}
