package scheduling.spreadsheet;

import com.github.miachm.sods.Range;

import scheduling.common.Config;

public class SpreadsheetReaderToolsHelper {

	private final SpreadsheetReader reader;
	private Integer[] fixedEmployeeOnDay;

	public SpreadsheetReaderToolsHelper(SpreadsheetReader reader) {
		this.reader = reader;
		this.fixedEmployeeOnDay = null;
	}

	public TriFunction<Range, Integer, Integer, Integer> getFunctionForCalculationOfFixedEmployees() {
		return (range, employee, day) -> {
			int date = day + 1;
			Object[][] values = range.getValues();
			boolean isWorking = String.valueOf(values[employee][day]).equals(Config.WORKING);
			boolean canWork = reader.getIsEmployeeAvailableOnDay(employee, day);

			validateEmployeeAvailability(isWorking, canWork, date);
			validateSingleEmployeePerDay(isWorking, day);

			return isWorking ? employee : getFixedEmployeeOnDay(day);
		};
	}

	public TriFunction<Range, Integer, Integer, Boolean> getFunctionForCalculationOfAvailableEmployees() {
		return (range, employee, day) -> range.getStyles()[employee][day].getBackgroundColor() == null;
	}

	public void setFixedEmployeeOnDay(Integer[] fixedEmployeeOnDay) {
		this.fixedEmployeeOnDay = fixedEmployeeOnDay;
	}

	private void validateEmployeeAvailability(boolean isWorking, boolean canWork, int date) {
		if (isWorking && !canWork) {
			throw new IllegalArgumentException("An employee is working on day " + date + " but is not available");
		}
	}

	private void validateSingleEmployeePerDay(boolean isWorking, int day) {
		if (isWorking && fixedEmployeeOnDay.length > day && fixedEmployeeOnDay[day] != Config.MISSING_EMPLOYEE) {
			throw new IllegalArgumentException("At least two employees are working on day " + (day + 1) + " but only one is allowed");
		}
	}

	private int getFixedEmployeeOnDay(int day) {
		return fixedEmployeeOnDay.length > day ? fixedEmployeeOnDay[day] : Config.MISSING_EMPLOYEE;
	}
}