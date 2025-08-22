package scheduling.spreadsheet;

import com.github.miachm.sods.Range;

import scheduling.common.Config;

import java.util.Objects;

public class SpreadsheetReaderToolsHelper {

	private final SpreadsheetReader reader;
	private Integer[] fixedEmployeeOnDay;

	public SpreadsheetReaderToolsHelper(SpreadsheetReader reader) {
		this.reader = Objects.requireNonNull(reader, "SpreadsheetReader must not be null");
		this.fixedEmployeeOnDay = null;
	}

	public TriFunction<Range, Integer, Integer, Integer> getFunctionForCalculationOfFixedEmployees() {
		if (this.fixedEmployeeOnDay == null) {
			throw new IllegalStateException(
					"fixedEmployeeOnDay must be set before calling getFunctionForCalculationOfFixedEmployees.");
		}

		return this::calculateFixedEmployeeInternal;
	}

	public TriFunction<Range, Integer, Integer, Boolean> getFunctionForCalculationOfAvailableEmployees() {
		return (range, employee, day) -> range.getStyles()[employee][day].getBackgroundColor() == null;
	}

	public void setFixedEmployeeOnDay(Integer[] fixedEmployeeOnDay) {
		this.fixedEmployeeOnDay = Objects.requireNonNull(fixedEmployeeOnDay,
				"fixedEmployeeOnDay array must not be null");
	}

	private int calculateFixedEmployeeInternal(Range range, int employee, int day) {
		int date = day + 1;
		Object[][] values = range.getValues();
		boolean isWorking = String.valueOf(values[employee][day]).equals(Config.WORKING);
		boolean canWork = reader.isEmployeeAvailableOnDay(employee, day);

		validateEmployeeAvailability(isWorking, canWork, date);
		validateSingleEmployeePerDay(isWorking, day);

		return isWorking ? employee : getFixedEmployeeOnDay(day);
	}

	private void validateEmployeeAvailability(boolean isWorking, boolean canWork, int date) {
		if (isWorking && !canWork) {
			throw new IllegalArgumentException(
					"Error on day " + date + ": An employee is scheduled to work but is marked as unavailable.");
		}
	}

	private void validateSingleEmployeePerDay(boolean isWorking, int day) {
		if (fixedEmployeeOnDay == null) {
			throw new IllegalStateException("Internal error: fixedEmployeeOnDay is null during validation.");
		}

		if (isWorking && day >= 0 && day < fixedEmployeeOnDay.length) {
			if (fixedEmployeeOnDay[day] != Config.MISSING_EMPLOYEE) {
				throw new IllegalArgumentException("Error on day " + (day + 1)
						+ ": Multiple employees are fixed for this day, but only one is allowed.");
			}
		}
	}

	private int getFixedEmployeeOnDay(int day) {
		if (fixedEmployeeOnDay == null) {
			throw new IllegalStateException(
					"Internal error: fixedEmployeeOnDay is null when retrieving fixed employee.");
		}

		if (day < 0 || day >= fixedEmployeeOnDay.length) {
			return Config.MISSING_EMPLOYEE;
		}
		return fixedEmployeeOnDay[day];
	}
}
