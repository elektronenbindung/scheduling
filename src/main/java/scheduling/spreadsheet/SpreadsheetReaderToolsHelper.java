package scheduling.spreadsheet;

import com.github.miachm.sods.Range;

import scheduling.common.Config;

import java.util.Objects;

public class SpreadsheetReaderToolsHelper {
	private static final String SCHEDULE_DATA_START_COLUMN = "B";
	private static final String SCHEDULE_DATA_END_COLUMN = "AF";
	private static final int SCHEDULE_DATA_START_ROW = 6;

	private final SpreadsheetReader reader;
	private Integer[] fixedEmployeeOnDay;

	public SpreadsheetReaderToolsHelper(SpreadsheetReader reader) {
		this.reader = Objects.requireNonNull(reader, "SpreadsheetReader must not be null");
		this.fixedEmployeeOnDay = null;
	}

	public Boolean[] calculateDayProperty(int row, String property) {
		Boolean[] dayProperty = new Boolean[reader.getLengthOfMonth()];
		String a1Notation = SCHEDULE_DATA_START_COLUMN + row + ":" + SCHEDULE_DATA_END_COLUMN + row;
		Range range = reader.getSheet().getRange(a1Notation);
		Object[] objects = range.getValues()[0];

		for (int index = 0; index < reader.getLengthOfMonth(); index++) {
			String str = String.valueOf(objects[index]);
			dayProperty[index] = str.equals(property);
		}
		return dayProperty;
	}

	public double[] calculateEmployeePreferencesOnSpreadsheet(String columnInA1Notation, boolean isNegativeValueAllowed) {
		String a1Notation = columnInA1Notation + SCHEDULE_DATA_START_ROW + ":" + columnInA1Notation
				+ Config.LAST_ROW_OF_SCHEDULE;
		double[] preferencesPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];
		Range range = reader.getSheet().getRange(a1Notation);
		Object[][] values = range.getValues();

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			Object[] value = values[employee];
			preferencesPerEmployee[employee] = value[0] != null
					? Double.parseDouble(String.valueOf(value[0]))
					: Config.MISSING_EMPLOYEE;

			if(!isNegativeValueAllowed) {
				preferencesPerEmployee[employee] = preferencesPerEmployee[employee] < 0
						? Config.MISSING_EMPLOYEE
						: preferencesPerEmployee[employee];
			}
		}
		return preferencesPerEmployee;
	}

	public  <T> void calculatePropertyForEmployeeOnDays(T[] result, int employee,
														TriFunction<Range, Integer, Integer, T> function) {
		String a1Notation = SCHEDULE_DATA_START_COLUMN + SCHEDULE_DATA_START_ROW + ":" + SCHEDULE_DATA_END_COLUMN
				+ Config.LAST_ROW_OF_SCHEDULE;
		Range range = reader.getSheet().getRange(a1Notation);

		for (int day = 0; day < reader.getLengthOfMonth(); day++) {
			result[day] = function.apply(range, employee, day);
		}
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
