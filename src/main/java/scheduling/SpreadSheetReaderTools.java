package scheduling;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Style;

public class SpreadSheetReaderTools {
    private SpreadsheetReader reader;
    private Integer[] fixedEmployeeOnDay;

    public SpreadSheetReaderTools(SpreadsheetReader reader) {
        this.reader = reader;
        fixedEmployeeOnDay = null;
    }

    public double[] calculateMaxLengthOfShiftPerEmployee() {
        double[] result = calculateEmployeePreferencesOnSpreadsheet("AK");
        IntToDoubleFunction function = employee -> {
            double maxLengthOfShift = result[employee] > 0
                    ? result[employee]
                    : Config.DEFAULT_MAX_LENGTH_OF_SHIFT;
            return Math.min(maxLengthOfShift, reader.getDaysToWorkInTotalForEmployee(employee));
        };

        return IntStream.range(0, result.length - 1).mapToDouble(function).toArray();

    }

    public double[] calculateExpectedDaysBetweenShifts() {
        double[] expectedDaysBetweenShiftsPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];

        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            if (reader.getDaysToWorkInTotalForEmployee(employee) <= 0
                    || reader.getWishedLengthOfShiftForEmployee(employee) <= 0) {
                expectedDaysBetweenShiftsPerEmployee[employee] = -1;
            } else {
                expectedDaysBetweenShiftsPerEmployee[employee] = (reader.getLengthOfMonth()
                        * reader.getWishedLengthOfShiftForEmployee(employee))
                        / reader.getDaysToWorkInTotalForEmployee(employee);
            }
        }

        return expectedDaysBetweenShiftsPerEmployee;
    }

    public int calculateDaysInMonth() {
        Range range = reader.getSheet().getRange("K1");
        LocalDate date = (LocalDate) range.getValue();
        return date.lengthOfMonth();
    }

    public boolean[] calculateIsFreeDay() {
        int row = 1 + Config.LAST_ROW_OF_SCHEDULE;
        return calculateDayProperty(row, Config.FREE_DAY);
    }

    public boolean[] calculateIsMandatoryBlockShiftOnDay() {
        int row = 2 + Config.LAST_ROW_OF_SCHEDULE;
        return calculateDayProperty(row, Config.SINGLE_SHIFT);
    }

    public Integer[] calculateFixedEmployees() {
        Integer[] result = new Integer[reader.getLengthOfMonth()];
        fixedEmployeeOnDay = result;
        Arrays.fill(result, -1);

        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            calculatePropertyForEmployeeOnDay(result, employee,
                    getFunctionForCalculationOfFixedEmployees());
        }
        return result;
    }

    public Boolean[][] calculateAvailability() {
        Boolean[][] result = new Boolean[Config.NUMBER_OF_EMPLOYEES][reader.getLengthOfMonth()];
        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            calculatePropertyForEmployeeOnDay(result[employee], employee,
                    getFunctionForCalculationOfAvailableEmployees());
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

        return IntStream.range(0, result.length - 1)
                .mapToDouble(employee -> Math.min(result[employee], reader.getMaxLengthOfShiftPerEmployee(employee)))
                .toArray();
    }

    public boolean[] calculateDayProperty(int row, String property) {
        boolean[] dayProperty = new boolean[reader.getLengthOfMonth()];
        String a1Notation = "B" + row + ":AF" + row;
        Range range = reader.getSheet().getRange(a1Notation);
        Object[] objects = range.getValues()[0];

        for (int index = 0; index < reader.getLengthOfMonth(); index++) {
            String str = String.valueOf(objects[index]);
            dayProperty[index] = str.equals(property);
        }

        return dayProperty;
    }

    public double[] calculateEmployeePreferencesOnSpreadsheet(String rowInA1Notation) {
        String a1Notation = rowInA1Notation + "6:" + rowInA1Notation + Config.LAST_ROW_OF_SCHEDULE;
        double[] preferencesPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];
        Range range = reader.getSheet().getRange(a1Notation);
        Object[][] values = range.getValues();

        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            Object[] value = values[employee];
            preferencesPerEmployee[employee] = value[0] != null
                    ? Double.parseDouble(String.valueOf(value[0]))
                    : -1;
            preferencesPerEmployee[employee] = preferencesPerEmployee[employee] < 0 ? -1
                    : preferencesPerEmployee[employee];

        }
        return preferencesPerEmployee;
    }

    private ThreeFunction<Range, Integer, Integer, Integer> getFunctionForCalculationOfFixedEmployees() {
        return (Range range, Integer employee, Integer day) -> {
            int date = day + 1;
            Object[][] values = range.getValues();
            boolean isWorking = String.valueOf(values[employee][day]).equals(Config.WORKING);
            boolean canWork = reader.getIsEmployeeAvailableOnDay(employee, day);
            if (isWorking && (!canWork)) {
                throw new IllegalArgumentException("An employee is working on day " + date + " but is not available");
            }

            if (isWorking && fixedEmployeeOnDay[day] != -1) {
                throw new IllegalArgumentException("Two employees are working on day " + date);
            }
            return isWorking ? employee : fixedEmployeeOnDay[day];
        };
    }

    private ThreeFunction<Range, Integer, Integer, Boolean> getFunctionForCalculationOfAvailableEmployees() {
        return (Range range, Integer employee, Integer day) -> {
            Style[][] styles = range.getStyles();
            boolean canWork = styles[employee][day].getBackgroundColor() == null;

            return canWork;
        };
    }

    private <T> void calculatePropertyForEmployeeOnDay(T[] result, int employee,
            ThreeFunction<Range, Integer, Integer, T> function) {
        String a1Notation = "B6:AF" + Config.LAST_ROW_OF_SCHEDULE;
        Range range = reader.getSheet().getRange(a1Notation);

        for (int day = 0; day < reader.getLengthOfMonth(); day++) {
            result[day] = function.apply(range, employee, day);
        }

    }
}