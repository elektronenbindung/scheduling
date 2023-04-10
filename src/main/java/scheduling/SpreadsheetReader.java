package scheduling;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import com.github.miachm.sods.Style;

public class SpreadsheetReader {
    private File input;
    private SpreadSheet spreadSheet;
    private Sheet sheet;
    private int lengthOfMonth;
    private double[] maxLengthOfShiftPerEmployee;
    private boolean[] isFreeDay;
    private boolean[] isMandatoryBlockShiftOnDay;
    private Integer[] fixedEmployeeOnDay;
    private Boolean[][] isAvailablePerDay;
    private double[] daysToWorkInTotalPerEmployee;
    private double[] daysToWorkAtFreeDayPerEmployee;
    private double[] wishedLengthOfShiftPerEmployee;
    private double[] expectedDaysBetweenShiftsPerEmployee;

    public SpreadsheetReader(File inpFile) {
        input = inpFile;
    }

    public SpreadSheet getSpreadSheet() {
        return spreadSheet;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public File getInputFile() {
        return input;
    }

    public double getMaxLengthOfShiftPerEmployee(int employee) {
        double maxLengthOfShift = maxLengthOfShiftPerEmployee[employee] > 0
                ? maxLengthOfShiftPerEmployee[employee]
                : Config.DEFAULT_MAX_LENGTH_OF_SHIFT;
        return Math.min(maxLengthOfShift, getDaysToWorkInTotalForEmployee(employee));
    }

    public double getWishedLengthOfShiftForEmployee(int employee) {
        return Math.min(wishedLengthOfShiftPerEmployee[employee], getMaxLengthOfShiftPerEmployee(employee));
    }

    public double getExpectedDaysBetweenShiftsForEmployee(int employee) {
        return expectedDaysBetweenShiftsPerEmployee[employee];
    }

    public int getLengthOfMonth() {
        return lengthOfMonth;
    }

    public boolean isFreeDay(int day) {
        return isFreeDay[day];
    }

    public int getEmployeeOnFixedDay(int day) {
        return fixedEmployeeOnDay[day];
    }

    public boolean getIsEmployeeAvailableOnDay(int employee, int day) {
        if (employee == -1) {
            return true;
        }
        return isAvailablePerDay[employee][day];
    }

    public double getDaysToWorkInTotalForEmployee(int employee) {
        return daysToWorkInTotalPerEmployee[employee];
    }

    public double getDaysToWorkAtFreeDayForEmployee(int employee) {
        return daysToWorkAtFreeDayPerEmployee[employee];
    }

    public boolean isMandatoryBlockShiftOnDay(int day) {
        return isMandatoryBlockShiftOnDay[day];
    }

    public void run() throws IOException {
        spreadSheet = new SpreadSheet(input);
        sheet = spreadSheet.getSheet(0);
        lengthOfMonth = calculateDaysInMonth();
        isFreeDay = calculateIsFreeDay();
        isMandatoryBlockShiftOnDay = calculateIsMandatoryBlockShiftOnDay();
        daysToWorkInTotalPerEmployee = calculateDaysToWorkInTotal();
        maxLengthOfShiftPerEmployee = calculateMaxLengthOfShiftPerEmployee();
        isAvailablePerDay = calculateAvailability();
        fixedEmployeeOnDay = calculateFixedEmployees();
        daysToWorkAtFreeDayPerEmployee = calculateDaysToWorkAtFreeDay();
        wishedLengthOfShiftPerEmployee = calculateWishedLengthOfShiftPerEmployee();
        expectedDaysBetweenShiftsPerEmployee = calculateExpectedDaysBetweenShifts();
    }

    private double[] calculateMaxLengthOfShiftPerEmployee() {
        return calculateEmployeePreferencesOnSpreadsheet("AK");

    }

    private double[] calculateExpectedDaysBetweenShifts() {
        double[] expectedDaysBetweenShiftsPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];

        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            if (getDaysToWorkInTotalForEmployee(employee) <= 0 || getWishedLengthOfShiftForEmployee(employee) <= 0) {
                expectedDaysBetweenShiftsPerEmployee[employee] = -1;
            } else {
                expectedDaysBetweenShiftsPerEmployee[employee] = (getLengthOfMonth()
                        * getWishedLengthOfShiftForEmployee(employee)) / getDaysToWorkInTotalForEmployee(employee);
            }
        }

        return expectedDaysBetweenShiftsPerEmployee;
    }

    private int calculateDaysInMonth() {
        Range range = sheet.getRange("K1");
        LocalDate date = (LocalDate) range.getValue();
        return date.lengthOfMonth();
    }

    private boolean[] calculateIsFreeDay() {
        int row = 1 + Config.LAST_ROW_OF_SCHEDULE;
        return calculateDayProperty(row, Config.FREE_DAY);
    }

    private boolean[] calculateIsMandatoryBlockShiftOnDay() {
        int row = 2 + Config.LAST_ROW_OF_SCHEDULE;
        return calculateDayProperty(row, Config.SINGLE_SHIFT);
    }

    private Integer[] calculateFixedEmployees() {
        Integer[] result = new Integer[lengthOfMonth];
        fixedEmployeeOnDay = result;
        Arrays.fill(result, -1);

        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            calculatePropertyForEmployeeOnDay(result, employee,
                    getFunctionForCalculationOfFixedEmployees());
        }
        return result;
    }

    private Boolean[][] calculateAvailability() {
        Boolean[][] result = new Boolean[Config.NUMBER_OF_EMPLOYEES][lengthOfMonth];
        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            calculatePropertyForEmployeeOnDay(result[employee], employee,
                    getFunctionForCalculationOfAvailableEmployees());
        }
        return result;
    }

    private ThreeFunction<Range, Integer, Integer, Integer> getFunctionForCalculationOfFixedEmployees() {
        return (Range range, Integer employee, Integer day) -> {
            int date = day + 1;
            Object[][] values = range.getValues();
            boolean isWorking = String.valueOf(values[employee][day]).equals(Config.WORKING);
            boolean canWork = getIsEmployeeAvailableOnDay(employee, day);
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
        Range range = sheet.getRange(a1Notation);

        for (int day = 0; day < lengthOfMonth; day++) {
            result[day] = function.apply(range, employee, day);
        }

    }

    private double[] calculateDaysToWorkInTotal() {
        return calculateEmployeePreferencesOnSpreadsheet("AH");
    }

    private double[] calculateDaysToWorkAtFreeDay() {
        return calculateEmployeePreferencesOnSpreadsheet("AI");
    }

    private double[] calculateWishedLengthOfShiftPerEmployee() {
        return calculateEmployeePreferencesOnSpreadsheet("AJ");
    }

    private boolean[] calculateDayProperty(int row, String property) {
        boolean[] dayProperty = new boolean[lengthOfMonth];
        String a1Notation = "B" + row + ":AF" + row;
        Range range = sheet.getRange(a1Notation);
        Object[] objects = range.getValues()[0];

        for (int index = 0; index < lengthOfMonth; index++) {
            String str = String.valueOf(objects[index]);
            dayProperty[index] = str.equals(property);
        }

        return dayProperty;
    }

    private double[] calculateEmployeePreferencesOnSpreadsheet(String rowInA1Notation) {
        String a1Notation = rowInA1Notation + "6:" + rowInA1Notation + Config.LAST_ROW_OF_SCHEDULE;
        double[] preferencesPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];
        Range range = sheet.getRange(a1Notation);
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

}
