package scheduling.spreadsheet;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Style;

import scheduling.common.Config;

public class SpreadsheetReaderToolsHelper {

    private SpreadsheetReader reader;

    public SpreadsheetReaderToolsHelper(SpreadsheetReader reader) {
        this.reader = reader;
    }

    public ThreeFunction<Range, Integer, Integer, Integer> getFunctionForCalculationOfFixedEmployees(
            Integer[] fixedEmployeeOnDay) {
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

    public ThreeFunction<Range, Integer, Integer, Boolean> getFunctionForCalculationOfAvailableEmployees() {
        return (Range range, Integer employee, Integer day) -> {
            Style[][] styles = range.getStyles();
            boolean canWork = styles[employee][day].getBackgroundColor() == null;

            return canWork;
        };
    }
}