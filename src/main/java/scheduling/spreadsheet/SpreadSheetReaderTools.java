package scheduling.spreadsheet;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;
import com.github.miachm.sods.Range;

import scheduling.common.Config;

public class SpreadSheetReaderTools {
  private final SpreadsheetReader reader;
  private final SpreadsheetReaderToolsHelper helper;

  public SpreadSheetReaderTools(SpreadsheetReader reader) {
    this.reader = reader;
    this.helper = new SpreadsheetReaderToolsHelper(reader);
  }

  public double[] calculateMaxLengthOfShiftPerEmployee() {
    double[] result = calculateEmployeePreferencesOnSpreadsheet("AK");
    IntToDoubleFunction function =
        employee -> {
          double maxLengthOfShift =
              result[employee] > 0 ? result[employee] : Config.DEFAULT_MAX_LENGTH_OF_SHIFT;
          return Math.min(maxLengthOfShift, reader.getDaysToWorkInTotalForEmployee(employee));
        };

    return IntStream.range(0, result.length).mapToDouble(function).toArray();
  }

  public double[] calculateExpectedDaysBetweenShifts() {
    double[] expectedDaysBetweenShiftsPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];

    for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
      if (reader.getDaysToWorkInTotalForEmployee(employee) <= 0
          || reader.getWishedLengthOfShiftForEmployee(employee) <= 0) {
        expectedDaysBetweenShiftsPerEmployee[employee] = Config.MISSING_EMPLOYEE;
      } else {
        expectedDaysBetweenShiftsPerEmployee[employee] =
            (reader.getLengthOfMonth() * reader.getWishedLengthOfShiftForEmployee(employee))
                / reader.getDaysToWorkInTotalForEmployee(employee);
      }
    }

    return expectedDaysBetweenShiftsPerEmployee;
  }

  public int calculateLengthOfMonth() {
    Range range = reader.getSheet().getRange("L1");
    LocalDate date = (LocalDate) range.getValue();
    return date.lengthOfMonth();
  }

  public Boolean[] calculateIsFreeDay() {
    int row = 1 + Config.LAST_ROW_OF_SCHEDULE;

    return Arrays.stream(calculateDayProperty(row, Config.WORK_DAY))
			.map(x -> !x).toArray(Boolean[]::new);
  }

  public Boolean[] calculateIsSingleShiftAllowedOnDay() {
    int row = 3 + Config.LAST_ROW_OF_SCHEDULE;
    return calculateDayProperty(row, Config.SINGLE_SHIFT);
  }

  public Integer[] calculateFixedEmployees() {
    Integer[] result = new Integer[reader.getLengthOfMonth()];
    Arrays.fill(result, Config.MISSING_EMPLOYEE);
    helper.setFixedEmployeeOnDay(result);

    for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
      calculatePropertyForEmployeeOnDays(
          result, employee, helper.getFunctionForCalculationOfFixedEmployees());
    }
    return result;
  }

  public Boolean[][] calculateAvailability() {
    Boolean[][] result = new Boolean[Config.NUMBER_OF_EMPLOYEES][reader.getLengthOfMonth()];
    for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
      calculatePropertyForEmployeeOnDays(
          result[employee], employee, helper.getFunctionForCalculationOfAvailableEmployees());
    }
    return result;
  }

  public double[] calculateDaysToWorkInTotal() {
    return calculateEmployeePreferencesOnSpreadsheet("AH");
  }

  public double[] calculateDaysToWorkAtFreeDay() {
    return calculateEmployeePreferencesOnSpreadsheet("AI");
  }

  public double[] calculateWishedLengthOfShiftPerEmployee() {
    double[] result = calculateEmployeePreferencesOnSpreadsheet("AJ");

    return IntStream.range(0, result.length)
        .mapToDouble(
            employee -> Math.min(result[employee], reader.getMaxLengthOfShiftPerEmployee(employee)))
        .toArray();
  }

  private Boolean[] calculateDayProperty(int row, String property) {
    Boolean[] dayProperty = new Boolean[reader.getLengthOfMonth()];
    String a1Notation = "B" + row + ":AF" + row;
    Range range = reader.getSheet().getRange(a1Notation);
    Object[] objects = range.getValues()[0];

    for (int index = 0; index < reader.getLengthOfMonth(); index++) {
      String str = String.valueOf(objects[index]);
      dayProperty[index] = str.equals(property);
    }

    return dayProperty;
  }

  private double[] calculateEmployeePreferencesOnSpreadsheet(String rowInA1Notation) {
    String a1Notation = rowInA1Notation + "6:" + rowInA1Notation + Config.LAST_ROW_OF_SCHEDULE;
    double[] preferencesPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];
    Range range = reader.getSheet().getRange(a1Notation);
    Object[][] values = range.getValues();

    for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
      Object[] value = values[employee];
      preferencesPerEmployee[employee] =
          value[0] != null ? Double.parseDouble(String.valueOf(value[0])) : Config.MISSING_EMPLOYEE;
      preferencesPerEmployee[employee] =
          preferencesPerEmployee[employee] < 0
              ? Config.MISSING_EMPLOYEE
              : preferencesPerEmployee[employee];
    }
    return preferencesPerEmployee;
  }

  private <T> void calculatePropertyForEmployeeOnDays(
      T[] result, int employee, ThreeFunction<Range, Integer, Integer, T> function) {
    String a1Notation = "B6:AF" + Config.LAST_ROW_OF_SCHEDULE;
    Range range = reader.getSheet().getRange(a1Notation);

    for (int day = 0; day < reader.getLengthOfMonth(); day++) {
      result[day] = function.apply(range, employee, day);
    }
  }
}
