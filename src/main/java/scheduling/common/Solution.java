package scheduling.common;

import java.util.Arrays;

import scheduling.spreadsheet.SpreadsheetReader;

public class Solution {
  private static final int UNKNOWN_COSTS = -1;
  private final int[] solution;
  private double costs;
  private final SpreadsheetReader spreadsheetReader;
  private final int[] lastOccurrenceOfEmployee;
  private final int[] lengthOfLastBlockShiftForEmployee;
  private int numberOfDirectFollowingShifts;
  private int numberOfRetries;
  private final int[] numberOfFreeDaysForEmployee;

  public Solution(int[] solution, int[] numberOfFreeDaysForEmployee, SpreadsheetReader input) {
    this.solution = solution;
    this.numberOfFreeDaysForEmployee = numberOfFreeDaysForEmployee;
    this.spreadsheetReader = input;
    costs = UNKNOWN_COSTS;
    numberOfRetries = 0;
    lastOccurrenceOfEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
    lengthOfLastBlockShiftForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];
    numberOfDirectFollowingShifts = 1;
  }

  public boolean canBeRetried() {
    if (numberOfRetries < Config.MAX_RETRIES_OF_SOLUTION) {
      numberOfRetries++;
      return true;
    }
    return false;
  }

  public int getEmployeeForDay(int day) {
    return solution[day];
  }

  public void exchangeEmployeesOnDays(int day1, int day2) {
    costs = UNKNOWN_COSTS;
    int changedEmployee = solution[day1];
    solution[day1] = solution[day2];
    solution[day2] = changedEmployee;
  }

  public void exchangeFreeDayBetweenEmployees(int fromDay, int toDay) {
    int fromEmployee = getEmployeeForDay(fromDay);
    int toEmployee = getEmployeeForDay(toDay);

    if (fromEmployee != Config.MISSING_EMPLOYEE) {
      numberOfFreeDaysForEmployee[fromEmployee]--;
    }
    if (toEmployee != Config.MISSING_EMPLOYEE) {
      numberOfFreeDaysForEmployee[toEmployee]++;
    }
  }

  public int getNumberOfFreeDaysForEmployee(int employee) {
    if (employee == Config.MISSING_EMPLOYEE) {
      return Config.MISSING_EMPLOYEE;
    }
    return numberOfFreeDaysForEmployee[employee];
  }

  public Solution createCopy() {
    return new Solution(
        Arrays.copyOf(solution, solution.length),
        Arrays.copyOf(numberOfFreeDaysForEmployee, numberOfFreeDaysForEmployee.length),
        spreadsheetReader);
  }

  public double getCosts() {
    if (costs != UNKNOWN_COSTS) {
      return costs;
    }
    initializeForCalculationOfCosts();

    for (int day = 0; day < solution.length; day++) {
      int employee = solution[day];

      if (employee == Config.MISSING_EMPLOYEE) {
        continue;
      }
      if (day != 0 && employee != solution[day - 1]) {
        initializeNextBlockShift(day);
      }

      costs = costs + calculateCostsForMandatoryBlockShiftOnDay(day);

      if (lastOccurrenceOfEmployee[employee] == Config.MISSING_EMPLOYEE) {
        lastOccurrenceOfEmployee[employee] = day;
        continue;
      }

      int interval = day - lastOccurrenceOfEmployee[employee];
      lastOccurrenceOfEmployee[employee] = day;

      if (interval == 1) {
        costs = costs + calculatePenaltyForTooLongBlockShift(employee);
      } else {
        costs = costs + calculatePenaltyForForbiddenShift(employee, interval);
      }

      costs = costs + calculateIntervalCosts(interval, employee);
    }

    return costs;
  }

  private void initializeForCalculationOfCosts() {
    costs = Config.OPTIMAL_SOLUTION;
    Arrays.fill(lastOccurrenceOfEmployee, Config.MISSING_EMPLOYEE);

    Arrays.fill(lengthOfLastBlockShiftForEmployee, 1);

    numberOfDirectFollowingShifts = 1;
  }

  private void initializeNextBlockShift(int day) {
    int previousEmployee = solution[day - 1];
    if (previousEmployee != Config.MISSING_EMPLOYEE) {
      lengthOfLastBlockShiftForEmployee[previousEmployee] = numberOfDirectFollowingShifts;
    }
    numberOfDirectFollowingShifts = 1;
  }

  private double calculateCostsForMandatoryBlockShiftOnDay(int day) {
    if ((!spreadsheetReader.isSingleShiftAllowedOnDay(day))
        && day < solution.length - 1
        && solution[day] != solution[day + 1]) {
      return Config.PENALTY_FOR_MANDATORY_BLOCK_SHIFT;
    }
    return 0;
  }

  private double calculatePenaltyForTooLongBlockShift(int employee) {
    numberOfDirectFollowingShifts++;
    if (numberOfDirectFollowingShifts
        > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee)) {
      return Config.PENALTY_FOR_FORBIDDEN_SHIFT;
    }
    return 0;
  }

  private double calculatePenaltyForForbiddenShift(int employee, int interval) {
    if (spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee) != 1
        && (interval <= lengthOfLastBlockShiftForEmployee[employee]
            || interval == Config.INTERVAL_FOR_ONE_DAY)) {
      return Config.PENALTY_FOR_FORBIDDEN_SHIFT;
    }
    return 0;
  }

  private double calculateIntervalCosts(int interval, int employee) {
    boolean hasIntervalCosts =
        spreadsheetReader.getWishedLengthOfShiftForEmployee(employee) > 0
            && (interval != 1
                || numberOfDirectFollowingShifts
                    > spreadsheetReader.getMaxLengthOfShiftPerEmployee(employee));

    if (hasIntervalCosts) {
		return Math.abs(interval - spreadsheetReader.getExpectedDaysBetweenShiftsForEmployee(employee));
    }
    return 0;
  }
}
